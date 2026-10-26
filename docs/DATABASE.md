# MySQL 登录数据接入（2026-10-26，1.2.0）

这次接入范围是手机号演示登录：服务端生成/校验演示码，成功后通过 JDBC 把用户和每次登录记录写入 MySQL。手机不直接连接数据库。训练档案、计划、收藏和训练历史保持现有本机存储，尚未进行账号隔离或云同步。

本机已检测到 MySQL80 服务，3306 端口。实际连接需要你提供可用数据库账号与密码；仅检测服务运行不能证明账号可用。默认离线演示和游客体验仍可使用。当前没有部署本地大模型。

## 本机实际接入结果（2026-10-26）

已使用用户在本机填写的管理员配置完成初始化，`pulse_fitness`、`pulse_users`、`pulse_login_events` 已创建。应用连接使用新生成的专用账户，账号密码自动保存在已忽略的 `config/database.local.properties`；该文件不包含管理员密码。

已对本机 MySQL 完成真实 HTTP 演示码→登录→提交事务→独立 SQL 查询验证，并检查错误码拒绝、验证码重放拒绝及同号复用用户。测试条目已按生成的用户 ID 精确清理，验证前后两表记录均为 0。测试记录见本机 `.runtime/mysql-live-verification.json`，不含密码、验证码或号码。

Java 网关本次已在后台启动，地址 `http://127.0.0.1:8080`。标准 Android 模拟器使用 `http://10.0.2.2:8080`，服务访问令牌从本机 `.runtime/dev-token.txt` 填入登录页连接设置。重启电脑或服务关闭后，按下面“检测并启动”重新启动网关。

## 配置数据库

### 数据库还没创建时

已经准备 `config/database-admin.local.properties`。只需在该本机文件填写 `MYSQL_ADMIN_USER` 与 `MYSQL_ADMIN_PASSWORD`，通常为安装 MySQL 时的 root 账号密码。不要将密码发到聊天，也不要把这个文件放进 APK。

填写后，在项目根目录运行：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\Initialize-MySql.ps1 -Offline
```

该命令只允许本机 MySQL，只创建固定项目库 `pulse_fitness`，并创建两张表。随后生成全新专用应用账号，仅授予这两张表的 SELECT、INSERT、UPDATE 权限，验证连接和读写，再把专用账号自动写入 `config/database.local.properties`。应用配置中不保存管理员密码。已有非占位应用配置不会被覆盖；已有账户密码不会被修改。验证写入使用回滚事务，不留模拟用户或登录事件。

MySQL 建库/建用户/授权是分别生效的管理语句，若中途权限或环境错误，可能已完成前面部分；命令会报告失败，不会自动删除项目库、原有表或账户。修正配置后根据实际状态继续处理。管理员需要 CREATE DATABASE、CREATE USER 及可授予对应权限的权限，参见 [MySQL 官方账户与授权说明](https://dev.mysql.com/doc/refman/8.0/en/creating-accounts.html)。

如果不记得管理员密码，先说明情况；本脚本不会重置密码、停掉 MySQL 或改动现有服务。

### 已有项目库或希望手工配置时

1. 在数据库管理工具中，为项目创建独立库 `pulse_fitness`，执行 [mysql-schema.sql](../database/mysql-schema.sql)。脚本只包含创建库/表，不删除已有数据。若已存在同名业务表，请先核对结构，不要直接用于其他项目的库。
2. 给专用账号分配该库中 `pulse_users`、`pulse_login_events` 的 SELECT、INSERT、UPDATE 权限。初始化脚本若由该账号运行，还需要相应建表权限；也可以管理员先建表，再给应用账号只分配数据权限。
3. 填写本机文件 `config/database.local.properties` 的 `DB_USER`、`DB_PASSWORD`，核对 `DB_URL`。文件已忽略，不进入 Git。密码只填本机文件，不放 Java 或 XML，更不要作为 URL 的一部分。
4. 保留 `AUTH_DEMO_ENABLED=true` 才会开放返回演示码的接口；默认不配置数据库时，服务端演示登录接口返回 503。生产短信认证不应直接启用这种返回验证码的接口。

配置例子见 [database.example.properties](../config/database.example.properties)。Java Properties 使用 UTF-8 Reader，密码中的反斜杠需按 properties 规则转义成双反斜杠；`DB_PASSWORD=` 后保留所需原始内容。也可通过进程环境变量设置 DB_URL、DB_USER、DB_PASSWORD、AUTH_DEMO_ENABLED，环境变量优先于文件。不要把密码放在命令行参数中。

MySQL 驱动仅在 `server` 使用，当前固定 `com.mysql:mysql-connector-j:26.10.0`。JDBC URL 和凭据分开传给 DriverManager，写操作采用 PreparedStatement。参考：[MySQL 官方 JDBC URL 文档](https://dev.mysql.com/doc/connector-j/en/connector-j-reference-jdbc-url-format.html)、[官方 Connector/J 项目](https://github.com/mysql/mysql-connector-j)。

## 检测并启动

在项目根目录运行：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\Test-Database.ps1
```

只有在你选定的项目库内需要创建两张表时，使用 `-InitializeTables`。它不会创建数据库本身，也不会删除/修改已有表结构。若测试输出 `Database connection and pulse account tables: OK`，说明当前账号能连接并读取所需字段；写入能力还需要实际登录事务检查。

启动 Java 网关，不启动模型：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\Start-Server.ps1 -LocalOnly -DatabaseConfig .\config\database.local.properties
```

此脚本会先检查数据库和表，再启动服务。不要关闭这个终端，Ctrl+C 结束服务。脚本会创建/复用 `.runtime/dev-token.txt` 中的网关访问令牌。这个令牌是手机访问服务用的，和 DB_PASSWORD 不是同一个东西。

## 安卓端操作

1. 重新运行 app；若已有本机登录，在“我的”退出登录，或从游客页面点手机号登录。
2. 登录页勾选“将手机号与登录记录保存到服务端”。不勾选就是原来的离线演示，不会写 SQL。
3. 点“设置登录服务连接”：标准模拟器填写 `http://10.0.2.2:8080`；令牌填写电脑 `.runtime/dev-token.txt` 的内容。不要填数据库密码。真机需要可访问的服务地址；默认网关只监听电脑回环地址，不能直接从另一台设备连接。
4. 阅读并勾选协议，获取演示码并登录。没有真实短信。
5. 成功后“我的”显示“服务端演示账号”，号码与登录记录在数据库，训练数据仍在此设备。

如果数据库未配置、表缺失、服务没启动或网关令牌错误，服务端模式会明确报错，不会悄悄改成本机登录。数据库暂时故障发生在验证码校验之后，挑战会保留至原有效期，修复连接后可重试登录。

## 数据表与查看

| 表 | 字段 | 含义 |
| --- | --- | --- |
| pulse_users | id | 服务器生成的 UUID，主键 |
| pulse_users | phone | 完整手机号，唯一约束，相同手机号复用用户 |
| pulse_users | phone_verified | 当前演示始终 FALSE，不冒充真实短信验证 |
| pulse_users | created_at / last_login_at | Unix 毫秒时刻 |
| pulse_login_events | id / user_id | 登录事件 ID、对应用户外键 |
| pulse_login_events | logged_in_at | Unix 毫秒登录时刻 |
| pulse_login_events | login_method | 当前为 PHONE_CODE_DEMO |

```sql
SELECT id, phone, phone_verified, created_at, last_login_at
FROM pulse_users ORDER BY last_login_at DESC;

SELECT e.id, u.phone, e.login_method, e.logged_in_at
FROM pulse_login_events e JOIN pulse_users u ON u.id = e.user_id
ORDER BY e.logged_in_at DESC;
```

一次成功登录在同一 SQL 事务内创建/更新用户并插入一条事件，任一步失败整体回滚。多个网关实例同时首次注册同一号码时，唯一约束防止重复用户；冲突请求可重试，不会绕开约束。演示码只在网关内存，不存表、不打印到服务日志；网关重启后需要重新获取。

手机保存的是 `SQL_DEMO` 标记、号码和返回的 `user_id`，不是可用来授权敏感服务器数据的登录令牌。退出登录只清本机状态，清本机个人数据也不会删除服务端记录。尚未实现生产用户会话、服务端删号或训练资料隔离。

## 代码入口

| 文件 | 职责 |
| --- | --- |
| core/auth/PhoneLoginGateway | 界面依赖的本机/远程可替换接口 |
| app/data/LocalPhoneLoginGateway、RemotePhoneLoginGateway | 离线验证与 HTTP 适配 |
| app/presentation/LoginViewModel | 后台登录工作与主线程状态发布，旋转时保留流程 |
| app/ui/LoginActivity | 选择模式、服务连接、显示演示码与错误 |
| server/DatabaseSettings | 从本机文件/环境读取 SQL 连接信息 |
| server/DatabaseTool | 独立检测连接及显式初始化表 |
| server/JdbcAccountRepository | JDBC、预编译参数、用户与事件事务 |
| server/DemoAccountService | 有界内存验证码挑战、冷却、重试和一次消费 |
| server/AccountHttpHandler | 请求格式、大小、错误码和 JSON 响应 |

## 接口

两个接口都要求 `Authorization: Bearer <APP_TOKEN>` 和 JSON 类型：

- `POST /api/v1/auth/demo-code`：请求 `{"phone":"13800138000"}`，响应含 challengeId、phone、code、demo=true、phoneVerified=false。
- `POST /api/v1/auth/demo-login`：请求 phone、challengeId、code，成功响应 userId、phone 及演示标记。用户数据写 SQL 成功后才返回成功。
- `GET /health` 新增 databaseLoginEnabled，仅表示网关已启用此模式；数据库故障仍以实际登录写入结果为准。

## 验证范围

新增 `AccountDatabaseTest` 使用隔离的 H2 数据库 MySQL 模式执行真实 JDBC/SQL，验证重复手机号、重新打开仓库、并发、事务回滚、验证码与手机号绑定、重放拒绝、SQL 故障重试、HTTP 鉴权和无配置拒绝。

H2 测试不能代替本机 MySQL 的实连验证。本机实连及提交写入已经另外验证，详见本页“本机实际接入结果”和 docs/VERIFICATION.md 的记录。

阅读当前数据库接入以本文件及带注释的源码为准；逐文件职责见 [当前项目文件作用说明](当前项目文件作用说明.md)。
