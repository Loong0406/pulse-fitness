# 硅基流动 AI 接入与使用

2026-10-26 已实连 `deepseek-ai/DeepSeek-V4.1-Flash`，使用项目现有 Java HTTP 网关，保留 MySQL 演示登录。

## 本机验证结果

- API Key 校验及当前账号模型列表检查通过，模型 ID 精确匹配。
- 上游直接生成演示档案的七天计划成功，通过领域校验。
- 已重启后台网关，`/health` 显示 modelConfigured=true、databaseLoginEnabled=true、模型名为用户指定的完整 ID。
- App 使用的 `/api/v1/plans` 实际返回 `source=AI`，7 天中有 3 个训练日，通过动作白名单、器械、限制、负荷和时间检查；该次请求约 4.2 秒，仅为一次观测，不保证每次相同时延。
- AI 启用后重新验证 MySQL 演示登录的写入、同号复用和重放拒绝，测试数据已清理。
- 33 项 JUnit 测试通过，Android Lint/打包与服务分发通过。本次 AI 改动在后端，不需要在 APK 中保存 Key。已扫描 APK 各解压条目，未发现实际 Key。
- 非敏感验证报告：`.runtime/siliconflow-live-verification.json`；后台进程与日志：`.runtime/mysql-gateway-process.json`。

## 配置文件

实际配置保存在已忽略的 `config/ai.local.properties`。文件由后端读取，不打入 APK。配置项如下，真实 Key 不写在本说明：

```properties
AI_BASE_URL=https://api.siliconflow.cn/v1
AI_MODEL=deepseek-ai/DeepSeek-V4.1-Flash
AI_API_KEY=在本机文件填写的真实Key
AI_RESPONSE_FORMAT=json_object
AI_ENABLE_THINKING=false
```

环境变量优先于文件。若你在启动终端设置过 AI_BASE_URL/AI_MODEL/AI_API_KEY 等变量，请核对是否在覆盖文件。`Start-Server.ps1 -LocalOnly` 显式关闭模型，即使本地 AI 配置存在也不调用模型。

模型 ID 经 [硅基流动官方更新公告](https://docs.siliconflow.cn/docs/release-notes/overview) 核实：2026-10-21 的后端模型版本升级不要求更改 `deepseek-ai/DeepSeek-V4.1-Flash` 这个调用 ID。地址与格式参考 [官方快速上手](https://docs.siliconflow.cn/docs/userguide/quickstart)、[JSON 模式](https://docs.siliconflow.cn/docs/userguide/guides/json-mode)、[Chat Completions 接口](https://docs.siliconflow.cn/docs/api/chat-completions-post)。

## 在 App 中使用

1. 保持电脑上的 MySQL 和 Java 后端运行。后台服务这次已启动。
2. App 的服务地址仍是你自己的 Java 网关：标准模拟器填写 `http://10.0.2.2:8080`，访问令牌使用 `.runtime/dev-token.txt`。不把 App 服务地址改成硅基流动地址。
3. 在“我的”保存个人档案；“保存连接并检测服务”可以检查网关和模型配置。
4. 到“计划”点击“生成 AI 专属计划”，阅读发送数据说明并确认。
5. 成功计划显示“大模型生成”。若显示“本地规则计划”，说明模型尚未配置、调用失败或结果未通过检查；不把回退当 AI 成功。

### Android 17 上连接超时（1.2.1 已修复）

面向 Android 17 / targetSdk 37 的应用连接电脑网关，需要额外声明并动态申请 `ACCESS_LOCAL_NETWORK`。旧版只有 `INTERNET`，即使电脑后端和 Key 正常，模拟器 App 仍会超时。依据见 [Android 官方本地网络权限说明](https://developer.android.com/privacy-and-security/local-network-permission)。

1. 使用当前 1.2.1 安装包，或在 Android Studio 重新运行当前项目；覆盖安装保留原资料。
2. 点击 AI 生成、检测连接或使用服务端登录时，若系统询问“附近的设备”权限，请允许。首次 AI 生成允许后会继续原请求。
3. 拒绝权限时 App 会说明原因，不发起本次后端请求；可以重试。若系统不再弹窗，到系统设置 → 应用 → 脉动 PULSE → 权限 → 附近的设备中允许。
4. Android 8–16 不申请此新增权限；本机验证码、游客入口和本地计划不触发权限申请。

2026-10-26 已在既有 Android 17 模拟器实际验证：拒绝与重试、允许后生成、AI 来源持久化和冷启动恢复。实际结果为 7 天安排、4 个训练日，原档案保留。[设备截图](screenshots/ai-plan-android17.png)。电脑上的 Java 后端仍需保持运行。

昵称和性别在网关调用上游前移除；其他训练所需档案按页面确认范围发送。Key 仅在网关向硅基流动发送认证请求时使用，App 中的服务访问令牌仍是另外一份凭据。

## 电脑重启后启动

在项目根目录运行：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\Start-AiServer.ps1 -Offline
```

该脚本读取现有 AI 和 MySQL 配置，复用网关访问令牌，不部署本地模型。手动终端运行时保持窗口打开；Ctrl+C 结束服务。如果 8080 端口已被当前后台实例占用，不重复启动。

更换 Key 或模型后，可以先测试：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\Test-Ai.ps1 -Offline
```

此测试会获取当前 Key 可见的对话模型列表，并向所选模型发送一次演示档案生成请求，按服务商规则计入用量。测试直接验证 AI 生成器，不经网关回退，因此不能把本地规则结果当成测试成功。

## 后端实现

- `AiSettings`：UTF-8 本机配置读取、环境覆盖、Key 占位检查、JSON 模式与推理开关校验。
- `CompatibleLlmAdapter`：可选 json_object/json_schema。硅基流动配置使用 json_object，不附加复杂 schema；通过 `enable_thinking=false` 控制本次请求，不再向该请求附加 `/no_think` 文本。
- JSON 模式仅约束有效 JSON，七天结构、档案签名、动作和负荷仍由 JsonCodec、PlanSafety 复检。输出截断或其他非正常结束会被拒绝。
- HTTP 错误只提供受控状态提示，不回传上游原始错误正文或密钥；不会因格式问题自动发出额外模型请求。
- 原有 JSON Schema 默认模式保留给其他兼容服务。本次只为选择的模型使用配置化 JSON 模式，不改变本地计划规则。

新增 8 项配置与假上游 HTTP 测试覆盖：空 Key/占位拒绝、离线覆盖、环境优先级、错误参数、精确模型 ID、JSON 模式和思考字段、去昵称性别、危险动作拒绝、输出截断拒绝和错误正文不外泄。它们不访问外部模型，真实调用验证另行执行。

数据库及 AI 代码分别以 DATABASE.md、本说明及当前源码为准；逐文件职责见 [当前项目文件作用说明](当前项目文件作用说明.md)。
