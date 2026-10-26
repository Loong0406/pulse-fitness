# 脉动 PULSE · Java 原生安卓健身 App

## 在 Android Studio 直接运行

1. 选择 **Open**，打开 `D:\项目\健身APP\fitness-sever`。
2. 等待 Gradle 同步，顶部运行配置选择 **PULSE**（项目附 `.run/PULSE.run.xml`，原有 **app** 配置也可使用），选择模拟器或 Android 8.0 及以上真机，点击 Run。
3. 首次启动会显示手机号登录页。可以选择“游客体验”，或输入手机号、勾选协议并获取**演示验证码**完成登录；此流程不发送短信。默认仅本机；连接 MySQL 并保存用户和登录记录，见下方数据库说明。
4. 到“我的”保存个人档案，在“计划”选择“使用本地计划”，即可训练。

本机 `local.properties` 已指向现有 SDK。换电脑时由 Android Studio 重新选择 SDK。项目附完整 Gradle Wrapper。

顶部显示 **Add Configuration** 或运行三角形灰色时，先从旁边下拉框选择 **PULSE**。如果没有此项，关闭当前项目，然后用 **File → Open** 打开上述 `fitness-sever` 文件夹（包含 `settings.gradle.kts` 和 `gradlew.bat`），等待同步完成；不要只打开上一级 `健身APP` 文件夹。设备未启动时，在 **Tools → Device Manager** 启动 Pixel 7，再选择该设备运行。

| 配置项 | 版本 |
| --- | --- |
| Android Gradle Plugin / Gradle | 9.3.0 / 9.5.0 |
| 编译和目标 SDK | Android 37.0 |
| 最低系统 | API 26 / Android 8.0 |
| Java 源码级别 | 17 |
| 建议 Gradle JDK | Android Studio 自带 JDK 或 JDK 21+ |
| 界面 | XML，没有使用 Compose |

本机系统 JAVA_HOME 指向旧版 Java，请让 Android Studio 使用内置 JDK。命令行脚本会选择可用的新 JDK：

若 Android Studio 已生成 `gradle/gradle-daemon-jvm.properties` 指定 JDK 版本，脚本会匹配该版本；当前本机配置为 JDK 25，可直接使用 Android Studio 自带运行时，不需再下载一份 JDK。

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\Build.ps1
# 已缓存依赖时可添加 -Offline
```

构建同时执行 JVM 测试、Android Lint、APK 打包与可选网关打包。安装包为 [deliverables/PulseFitness-debug.apk](deliverables/PulseFitness-debug.apk)，属于开发测试 Debug 包，商店发布需配置自己的正式签名。

当前安装包为 **1.2.2**：全 App 红黑配色，覆盖登录、导航、卡片、图表、弹窗与训练插画，颜色角色见 [DESIGN.md](DESIGN.md)。已保留 1.2.1 的 Android 17 本地网络权限修复；使用 AI / 服务端登录时，允许系统询问的“附近的设备”权限。硅基流动已在本机配置，并已在模拟器实际生成 AI 计划；步骤和排查见 [AI 接入说明](docs/SILICONFLOW.md)。

## 当前功能

- 登录：手机号＋演示验证码、60 秒重发倒计时、5 分钟有效期、错误提示、可阅读的协议、游客体验与退出登录；可选服务端 SQL 保存用户和登录记录。
- 今天：个人目标、今日训练入口、周节奏与实际训练统计。
- 计划：结合目标、经验、器械、年龄、天数、时长和动作限制生成七天安排，包含恢复日、组数、次数、休息与时间估算。
- 动作：22 个内置动作，部位筛选、关键词与条件表达式搜索、收藏和动作要领。
- 训练：热身、逐组确认、组间倒计时、暂停、离开后自动暂停、进度保存、放松及完成记录。
- 记录：累计次数、分钟、组数、周图表与最近 50 条训练；累计统计包含全部记录。
- 我的：档案编辑与撤销，服务连接设置、令牌加密保存和清除本机数据。
- 导出：通过系统文件选择器导出 JSON 或易读文本。

没有预置训练记录。记录来自用户在训练流程中的完成确认，目前没有传感器或视频动作校验。

两种演示登录都不会验证真实手机号所有权。服务端模式会在数据库创建演示用户和登录事件，游客与各演示身份仍共用当前设备的训练资料。退出登录保留训练记录，“清除本机所有个人数据”会清除本机身份，但不会删除服务端用户和登录记录。正式短信登录及训练资料的多账号隔离需要后续实现。

## 连接 SQL 数据库（1.2.0）

采用 **Android → Java HTTP 网关 → MySQL**，JDBC 驱动和数据库密码只在后端。新增 `pulse_users`、`pulse_login_events` 两张表。建表、填写本机连接文件、测试、启动和 App 操作见 [数据库接入说明](docs/DATABASE.md)。2026-10-26 已在本机创建 `pulse_fitness`，自动保存专用应用账号至已忽略的 `config/database.local.properties`，并通过真实 MySQL 登录写入验证。

隔离 SQL 测试使用 H2 MySQL 模式；本机 MySQL 已额外完成 HTTP 演示登录、事务提交、独立 SQL 读取、同号复用及重放拒绝验证，测试记录已清理。更换数据库配置后需重新执行 `scripts/Test-Database.ps1` 和实际登录验证。

## 大模型功能的当前状态

未部署本地模型。2026-10-26 已配置并实连硅基流动 `deepseek-ai/DeepSeek-V4.1-Flash`，通过 App 的网关计划接口实际取得通过校验的 `AI` 来源计划，MySQL 登录同时可用。Android 不依赖服务器即可运行本地规则计划。AI 按钮通过 Java 网关调用上游模型，模型 API Key 不进入 APK。

AI 使用、Key 配置、更换模型和电脑重启后的启动命令见 [硅基流动接入说明](docs/SILICONFLOW.md)。实际 Key 只保存在已忽略的 `config/ai.local.properties`；不要把服务商地址或 Key 填到 App 的网关连接位置。

候选计划需通过动作白名单、器械、动作限制、七天日程、训练天数、组数、次数和时长检查。模型服务失败或结果不合格时明确显示 **本地规则计划**，不会宣称 AI 成功。昵称和性别不发送给上游模型。

当前自动计划覆盖 18–69 岁、无医疗运动限制的成年人。未成年人、高龄、孕产期或有医疗运动限制的档案可以保存，但停止自动制定训练计划。膝腰选项用于避开动作，不提供诊断或康复处方。

## 文件组织

```text
app/           Android Java 源码、XML 布局和资源
core/          纯 Java 模型、规则、状态机、仓库契约、导出与测试
server/        可选 Java HTTP 网关、模型适配器、鉴权与测试
gradle/        Gradle Wrapper
scripts/       构建与可选网关启动脚本
docs/          模式映射、架构、接口、验证记录和实际截图
deliverables/  可安装 APK
.runtime/      临时验证文件及开发令牌，已忽略，不应分享
```

[24 种设计模式](docs/DESIGN_PATTERNS.md) · [架构](docs/ARCHITECTURE.md) · [接口与后续接入](docs/API.md) · [验证记录](docs/VERIFICATION.md)

## 代码学习说明

按当前 1.2.2 排版后的源码学习，可先阅读 [当前项目文件作用说明](docs/当前项目文件作用说明.md)：逐项说明 74 个 Java 文件、47 个 XML，以及构建、数据库/模型配置和开发脚本，共 155 个文件，并整理启动、AI 生成、SQL 登录和训练的调用链。

专题说明：[24 种设计模式](docs/DESIGN_PATTERNS.md)、[架构](docs/ARCHITECTURE.md)、[数据库接入](docs/DATABASE.md)、[AI 接入](docs/SILICONFLOW.md)。旧版逐行讲义已按要求删除，阅读以当前源码和上述说明为准。

配置依据：[Android 官方兼容表](https://developer.android.com/build/releases/agp-9-3-0-release-notes)。后续模型适配参考：[兼容接口](https://docs.ollama.com/api/openai-compatibility)、[结构化输出](https://docs.ollama.com/capabilities/structured-outputs)，当前没有启动本地部署。
