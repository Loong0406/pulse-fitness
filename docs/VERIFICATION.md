# 验证记录

日期：2026-10-20。环境：Windows，用户指定项目目录。

- Android Debug APK 编译、安装及 MainActivity 冷启动成功。
- API 37.2 模拟器实际检查首页、档案、档案保存和本地计划生成。
- 13 个 JVM 测试：core 9 个，server 4 个；包含 15,552 种档案组合的时间、器械和动作限制检查。
- 覆盖人群门槛、危险计划拒绝、深复制、JSON 往返、文本导出、撤销、记录去重、搜索表达式。
- 训练状态机覆盖热身、逐组训练、休息、暂停、不计暂停时间、快照恢复、放松和完成。
- HTTP 测试覆盖令牌、错误档案、请求大小、路径、失败回退，以及 mock 上游的真实 HTTP 报文适配与昵称 / 性别最小化。
- Android Lint 无阻断错误；部分原生样式与 Debug HTTP 提示属于已知开发配置。

本机已有模型曾返回不符合约束的日程，系统正确回退为 LOCAL，未宣称 AI 成功。按照后续要求，已经停止本次测试启动的模型与网关服务，不再继续本地部署。实际模型端到端验收留待选定服务商后完成。

截图：[首页](screenshots/home.png)、[档案](screenshots/profile.png)、[本地计划](screenshots/plan.png)。测试 HTML 位于 `core/build/reports/tests/test/`、`server/build/reports/tests/test/`，Lint 位于 `app/build/reports/lint-results-debug.html`。

尚未验证实体手机、所有 Android 版本及全部系统文件提供商；没有执行覆盖全部按钮的 Android 仪器自动化测试，训练关键转移由纯 Java 测试覆盖。容器及远程生产模型未部署验证。

临时 R: 文件夹映射已撤销，测试模拟器已停止。项目与脚本不需要额外盘符。

自动审批以策略限制为由拦截了临时测试目录的删除，`.runtime` 中的模拟器测试文件及 SDK 目录链接暂时保留；该链接指向原有 SDK，运行和构建不依赖此链接。未继续尝试绕过此限制。

## 2026-10-23：登录入口更新（1.1.0）

- 新增 Java＋XML 的手机号登录页、本机演示验证码、协议查看、游客入口、手机号隐藏显示及退出登录。
- 构建和 Android Lint 通过（0 个错误）；JVM 测试共 17 个通过，新增 4 个手机号验证测试，覆盖格式、号码绑定、验证码重放、重发限制、过期及连续错误。
- 在既有 Pixel 7 / Android 17.2 模拟器实际验证：未同意协议、错误手机号、错误验证码、验证码展示与填入、协议返回、登录成功、隐藏号码、登录和游客状态在重启后保留、游客转登录、退出后返回键行为。
- 验证码弹窗和输入在深色切换、1.3 倍系统字号及横竖屏切换后保留；完成上述切换后仍能使用原验证码登录。重新进入测试前的字体、深色和旋转设置已恢复。
- 检查了浅色、深色、大字号及横屏实际截图。原生表单可滚动，横屏与较宽窗口时表单内容最大宽度为 560 dp。
- 新增界面主要正文、辅助文字、按钮与字段颜色对比度均高于 4.5:1。
- 验证使用演示号码，没有发送短信、配置认证服务器、清除原有训练资料、创建额外盘符或启动模型服务。

截图：[登录页](screenshots/login.png)、[演示验证码](screenshots/login-demo-code.png)、[本机账号状态](screenshots/login-account.png)、[深色](screenshots/login-dark.png)、[大字号](screenshots/login-large-text-bottom.png)、[横屏](screenshots/login-landscape.png)。

当前仍是明确标注的本机体验，不能验证真实手机号所有权；正式短信、生产账号系统和多账号训练数据隔离不在本次范围。

## 2026-10-26：可选 MySQL 登录记录接入（1.2.0）

- 新增 MySQL JDBC 配置、连接检查工具、显式建表脚本，以及 `pulse_users` 和 `pulse_login_events` 事务存储。数据库凭据只在后端本机忽略文件/环境变量，不进入 APK。
- 新增网关令牌保护的演示码、演示登录接口；手机号仍为演示用途，数据库 `phone_verified` 为 FALSE。验证码不持久化；错误 SQL 事务回滚，连接恢复后有效期内可重试。
- Android Java＋XML 登录页可选择是否把号码和登录记录发给服务，独立提供服务连接配置。SQL 模式失败会报错，不静默降级为本机成功。离线演示及游客继续可用。
- `scripts/Build.ps1 -Offline` 通过：25 项 JUnit 测试、Android Lint（0 错误，18 警告）、APK 和服务分发构建。新包为 1.2.0，versionCode=3。
- 新增 8 项真实 JDBC/SQL 测试使用隔离 H2 的 MySQL 兼容模式：同号用户唯一、仓库重新打开、并发、事务回滚、挑战/手机号绑定、验证码重放拒绝、SQL 故障后原挑战重试、HTTP 鉴权/限长/路径/未配置拒绝，以及连接写入探测成功/失败都回滚、不遗留测试用户。
- **尚未验证本机 MySQL 实连**：检测到 MySQL80 服务及 3306 监听，但 `config/database.local.properties` 仍为密码占位值，等待用户填写有效账号和库信息。H2 成功不能代替实连与 MySQL 写入验证。
- 本次未启动 Android 模拟器，未宣称完成新版本模拟器 UI 验证；未替换现有本机训练资料，未修改数据库服务/密码、创建盘符或部署模型。
- 旧版逐行讲义曾保存为 2026-10-23 / 1.1.0 快照，现已按用户要求删除；当前 SQL 接入与边界见 [DATABASE.md](DATABASE.md)。
- 用户确认项目库尚未创建后，增加 `Initialize-MySql.ps1` 与 `BootstrapMysql`。初始化固定项目库/两张表及新专用账号，自动保存应用凭据，保留现有非占位连接配置。已验证缺管理员密码的预检会在连接前退出，应用配置内容不变。管理员配置仍为占位值，尚未实际执行 MySQL 建库/授权。

### 管理员配置填写后的本机 MySQL 实连结果

- 用户填写管理员配置后，实际运行 `Initialize-MySql.ps1 -Offline` 成功：创建 `pulse_fitness` 和两张表、新专用应用账号及两表 SELECT/INSERT/UPDATE 权限。根账号密码未被修改，管理员密码未复制到应用配置或输出。
- 专用账号已通过连接、表结构、INSERT/UPDATE 及回滚探测。应用凭据自动保存到本机忽略文件，不在 APK 中。
- 实际启动仅规则模式的 Java 网关，监听 `127.0.0.1:8080`；`databaseLoginEnabled=true`，未部署或启动模型。
- 实际 HTTP 联调本机 MySQL：获取演示码，错误码拒绝且不建用户，正确码登录提交后由独立 JDBC 连接读到同一用户 ID，重放拒绝，同号第二次写入复用 ID 并保留两条登录事件。
- 联调生成的临时用户及其两条事件按精确 ID/号码条件清理，验证前后用户/事件数量分别均为 0，没有预置演示数据。非敏感结果在 `.runtime/mysql-live-verification.json`。
- 后台网关进程与日志位置记录在 `.runtime/mysql-gateway-process.json`；本次没有 Android 模拟器在运行，因此不宣称已在设备上完成 SQL 模式 UI 交互。

## 2026-10-26：硅基流动 DeepSeek-V4.1-Flash 实连

- 用户在本机 AI 配置文件填写 Key 后，实际验证当前 Key 的模型访问列表和直接生成，所选完整 ID 为 `deepseek-ai/DeepSeek-V4.1-Flash`。
- 增加后端忽略配置文件、JSON 输出模式与可选推理字段、受控状态错误、截断拒绝及启动/测试脚本。运行的网关切换至 AI 模式，原 MySQL 配置、访问令牌与端口保留。
- 实际 `/api/v1/plans` 返回 source=AI、7 天、3 个训练日，通过 PlanSafety；调用约 4.2 秒。模型响应未被当作本地回退成功，实际报告位于 `.runtime/siliconflow-live-verification.json`。
- 切换后重新实跑 MySQL 演示登录、正确/错误码、重放拒绝、同号复用和数据库写入验证，测试条目已清理，两表恢复测试前数量。
- `Build.ps1 -Offline` 成功，33 项 JUnit 测试通过（新增 8 项 AI 配置及假上游 HTTP 测试），Android Lint/打包和服务分发通过；实际 API Key 未出现在 APK 解压条目中。
- 本次没有新启动 Android 模拟器，没有部署本地模型或创建盘符。实际健康检查 modelConfigured、databaseLoginEnabled 均为 true。使用和维护说明见 [SILICONFLOW.md](SILICONFLOW.md)。

## 2026-10-26：修复 Android 17 本地网关访问权限（1.2.1）

- 用户反馈 App 仍不能生成后，在用户已运行的 `emulator-5554` 复现：系统 shell 可访问 `10.0.2.2:8080`，App 请求超时。旧 APK targetSdk=37，但只声明 INTERNET，缺少 Android 17 强制的 ACCESS_LOCAL_NETWORK 声明与动态申请。前述主机侧 AI 接口成功不能代表设备端网络成功。
- 补齐 Manifest 声明，共用 `BackendActivity` 在 AI 生成、连接检测及 SQL 演示登录访问后端前检查并申请权限。屏幕通过 NavigationMediator 发送意图；ViewModel/仓库不保留 Activity。拒绝不继续请求，有明确说明；销毁时释放待执行回调，旋转重建后提示重试。旧系统、本机验证码、游客和离线计划无需新增权限。
- `Build.ps1 -Offline` 成功，现有 33 项 JUnit 报告为 0 失败/0 错误，Android Lint 0 错误/18 警告。APK 1.2.1（versionCode=4）覆盖安装，未清除应用数据；同步更新交付 APK 和 SHA-256。
- 用标准系统权限弹窗实际验证拒绝 → 说明 → 重试 → 允许，没有用 ADB grant 代替权限流程。允许后由 App 发出请求并成功显示“大模型生成 · 已通过规则校验”。设备持久化计划 source=AI，共 7 天、4 个训练日，档案与修复前一致。此前未保存的网关连接也已通过表单保存，访问令牌加密存储。
- 强制停止应用后重新打开，计划仍显示 AI 来源；未再发出模型生成请求。实际结果摘要保存在 `.runtime/ai-actual-user-verification.json`；[设备成功截图](screenshots/ai-plan-android17.png)。未重新进行 SQL 登录 UI 写入或宣称所有系统/实体手机都已验证。
- 本次复用既有模拟器与后台 AI/MySQL 网关，没有启动本地模型、创建盘符或修改系统代理。诊断时临时创建的单条 ADB reverse 8080 映射已经移除，实际成功使用 `10.0.2.2:8080` 直连。

## 2026-10-26：全 App 红黑配色（1.2.2）

- 黑色页面、深灰卡片、红色主要操作及浅红选中/链接，覆盖登录、五个主页面、训练、动作详情弹窗、图表、输入与品牌插画。旧绿色/青柠色及 Java 中的硬编码颜色均替换为统一颜色资源；系统浅/深模式保持红黑主题。说明见 [DESIGN.md](../DESIGN.md)。
- `Build.ps1 -Offline` 成功，既有 33 项 JVM 测试报告无失败，Android Lint 无错误；交付 APK 1.2.2（versionCode=5）。配色改动没有新增只重复实现的测试，采用真实设备显示和计算对比度验证。
- 使用既有 `emulator-5554` 覆盖安装，检查首页、计划、动作列表、动作说明、记录、档案、登录表单/未同意协议的提示，以及训练热身页面。登录额外切换系统深色与字号 1.3；完成后恢复原夜间模式 no、字号 1.0。未宣称验证所有设备尺寸或整个训练流程。
- 主按钮白字/红底 4.78:1；辅助文字/卡片 8.23:1；浅红强调/页面 7.36:1；输入框边界/输入背景 3.93:1。检查的主要文字与控件颜色组合均满足对应 4.5:1 或 3:1 阈值，计算摘要位于 `.runtime/red-black-contrast.json`。
- 本次不生成新计划、不完成训练、不进行 SQL 登录；使用前已保存本机训练数据快照，查看训练页后恢复临时训练状态，并核对档案、原计划、训练记录和原进度与快照一致。系统登录身份和后端配置保留。
- 截图：[计划](screenshots/red-black-plan.png)、[登录表单](screenshots/red-black-login-form.png)、[动作](screenshots/red-black-exercises.png)、[详情弹窗](screenshots/red-black-exercise-detail.png)、[记录](screenshots/red-black-progress.png)、[训练](screenshots/red-black-workout.png)。涉及个人资料的截图仅放本机 `.runtime`。
