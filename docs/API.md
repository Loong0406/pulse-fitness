# 可选 Java 网关

当前不部署本地模型，网关不是 App 离线运行的前置条件。

## 构建和规则服务

```powershell
.\gradlew.bat -PserverOnly=true :server:installDist
powershell -ExecutionPolicy Bypass -File .\scripts\Start-Server.ps1 -LocalOnly
```

建议 JDK 21+。启动脚本默认只监听 `127.0.0.1:8080`，自动创建 `.runtime/dev-token.txt`。将该令牌填入 App“我的 → AI 教练服务”，不要填模型 API Key。

模拟器默认地址 `http://10.0.2.2:8080`。若 Windows 环境阻止连接，可使用 SDK 的 `adb reverse tcp:8080 tcp:8080`，再将 App 地址改为 `http://127.0.0.1:8080`；USB 真机也可采用此方式。

## 后续接入模型

2026-10-26 已实连硅基流动 `deepseek-ai/DeepSeek-V4.1-Flash`，本机使用 `config/ai.local.properties` 与 `scripts/Start-AiServer.ps1`。MySQL 配置继续生效。具体配置、真实验证和重启步骤见 [SILICONFLOW.md](SILICONFLOW.md)。

| 服务端变量 | 用途 |
| --- | --- |
| AI_BASE_URL | 上游根地址，通常以 `/v1` 结尾，适配器追加 `/chat/completions`。留空时仅生成规则计划。 |
| AI_MODEL | 服务商实际模型标识。 |
| AI_API_KEY | 上游密钥，只保留在服务端。 |
| APP_TOKEN | App 访问 Java 网关的令牌。 |
| HOST / PORT | 默认 127.0.0.1 / 8080，对外监听要求至少 24 字符的 APP_TOKEN。 |

选定服务商后，使用 `Start-Server.ps1 -AiBaseUrl <实际地址> -Model <实际标识>` 并设置服务端密钥。App 始终连接 Java 网关。

适配器默认使用项目所需的 JSON Schema 结构化输出，也可通过 AI_RESPONSE_FORMAT=json_object 使用 JSON 模式，随后执行相同的领域检查。硅基流动本机配置使用 JSON 模式；不同接口/模型对 Schema 与推理参数的支持仍需实测。容器模板只包含 Java 网关，不会下载或启动模型，当前没有 Docker，未实跑该模板。

Release App 只允许 HTTPS；Debug 允许 HTTP 联调。当前共享令牌用于开发验证与受控使用，对外运营需配置 HTTPS 与正式用户鉴权。

## 接口

`GET /health` 无需令牌，不触发模型。`modelConfigured=true` 仅代表有配置，不代表推理成功。

`POST /api/v1/plans` 接收 [档案示例](api-profile.example.json)，头部为 `Content-Type: application/json` 和 `Authorization: Bearer <APP_TOKEN>`。

响应：`{"plan": {...}, "message": "..."}`。`plan.source` 为 `LOCAL` 或 `AI`；候选结果不合格或模型不可用时明确返回 LOCAL。

错误码：400 档案无效或人群不支持；401 令牌错误；404 路径不存在；405 方法错误；413 超过 32 KiB；415 类型错误；429 推理并发已满；500 内部失败。

上游并发为 2，线程和队列有界，响应上限 256 KiB，调用设有时限，不向手机返回密钥或上游内部异常。目前没有生产用户认证或云端训练档案库。

1.2.0 增加可选的数据库演示账号接口 `/api/v1/auth/demo-code`、`/api/v1/auth/demo-login`，由网关令牌保护。开启前需要配置数据库及 AUTH_DEMO_ENABLED。接口、字段、数据范围及实际连接步骤见 [DATABASE.md](DATABASE.md)。健康检测新增 `databaseLoginEnabled`，不代表数据库此刻的写操作一定成功。
