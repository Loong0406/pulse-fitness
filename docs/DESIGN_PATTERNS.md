# 24 种设计模式与代码对应

每一项对应实际功能。源代码根目录：`core/src/main/java/com/pulse/core`、`app/src/main/java/com/pulse/fitness`、`server/src/main/java/com/pulse/server`。

| # | 指定模式 | 代码入口 | 实际用途 |
| --- | --- | --- | --- |
| 1 | 简单工厂 Simple Factory | [GoalStrategyFactory](../core/src/main/java/com/pulse/core/plan/GoalStrategyFactory.java) | 根据减脂、力量、体能目标创建策略。 |
| 2 | 工厂方法 Factory Method | [PlanGeneratorFactory](../core/src/main/java/com/pulse/core/plan/PlanGeneratorFactory.java)、[RemotePlanGenerator.Factory](../app/src/main/java/com/pulse/fitness/data/RemotePlanGenerator.java) | 本地与远程 Creator 分别创建生成器，公共方法附加安全校验装饰。 |
| 3 | 抽象工厂 Abstract Factory | [Repositories.Factory](../core/src/main/java/com/pulse/core/data/Repositories.java)、[PreferencesRepositoryFactory](../app/src/main/java/com/pulse/fitness/data/PreferencesRepositoryFactory.java)、[MemoryRepositoryFactory](../core/src/main/java/com/pulse/core/data/MemoryRepositoryFactory.java) | 分别创建持久化和测试内存存储的档案、计划、历史仓库产品族。 |
| 4 | 建造者 Builder | [UserProfile.Builder](../core/src/main/java/com/pulse/core/model/UserProfile.java) | 分步构建身体信息、目标、时间和动作限制。 |
| 5 | 单例 Singleton | [AppContainer](../app/src/main/java/com/pulse/fitness/AppContainer.java) | 双重校验锁初始化唯一依赖容器，仅持有 Application 级对象。 |
| 6 | 原型 Prototype | [WeekPlan.copyForNextWeek](../core/src/main/java/com/pulse/core/model/WeekPlan.java) | 复制周期时深复制日程及动作处方，分配新计划 ID。 |
| 7 | 适配器 Adapter | [CompatibleLlmAdapter](../server/src/main/java/com/pulse/server/CompatibleLlmAdapter.java) | 上游聊天报文与领域 PlanGenerator / WeekPlan 接口互相转换。 |
| 8 | 桥接 Bridge | [PlanExporter](../core/src/main/java/com/pulse/core/export/PlanExporter.java)、[MainActivity.AndroidDocumentSink](../app/src/main/java/com/pulse/fitness/ui/MainActivity.java) | JSON / 文本导出格式，与内存 / 系统文档输出目标独立变化。 |
| 9 | 组合 Composite | [PlanNode](../core/src/main/java/com/pulse/core/model/PlanNode.java)、WeekPlan、DayPlan、Prescription | 周、天、动作组成树，支持统一聚合操作。 |
| 10 | 装饰器 Decorator | [SafePlanGeneratorDecorator](../core/src/main/java/com/pulse/core/plan/SafePlanGeneratorDecorator.java) | 为任意生成器动态增加档案与结果检查。 |
| 11 | 外观 Facade | [TrainingFacade](../core/src/main/java/com/pulse/core/data/TrainingFacade.java) | 对上层统一提供生成、提交、读取和失效计划入口。 |
| 12 | 享元 Flyweight | [ExerciseCatalog](../core/src/main/java/com/pulse/core/plan/ExerciseCatalog.java)、[Exercise](../core/src/main/java/com/pulse/core/model/Exercise.java) | 不可变动作定义按 ID 共享，用户负荷保存在独立 Prescription 中。 |
| 13 | 代理 Proxy | [AuthorizedHandlerProxy](../server/src/main/java/com/pulse/server/AuthorizedHandlerProxy.java) | 鉴权通过后才调用真正的计划处理器。 |
| 14 | 责任链 Chain of Responsibility | [ProfileValidator.Handler](../core/src/main/java/com/pulse/core/validation/ProfileValidator.java) | 基本信息、日程、人群门槛分别处理；存档只采用前两项。 |
| 15 | 命令 Command | [ProfileCommand](../core/src/main/java/com/pulse/core/data/ProfileCommand.java) | 保存档案与撤销逻辑独立于按钮和具体存储实现。 |
| 16 | 解释器 Interpreter | [ExerciseQuery](../core/src/main/java/com/pulse/core/plan/ExerciseQuery.java) | 解释 `group:上肢 & equipment:徒手 | group:核心`；AND 优先于 OR。 |
| 17 | 迭代器 Iterator | [WeekPlan implements Iterable](../core/src/main/java/com/pulse/core/model/WeekPlan.java) | 页面和导出统一遍历只读日程，不依赖内部集合表示。 |
| 18 | 中介者 Mediator | [NavigationMediator](../app/src/main/java/com/pulse/fitness/ui/NavigationMediator.java)、[MainActivity](../app/src/main/java/com/pulse/fitness/ui/MainActivity.java) | 五个页面通过中介请求切页、开练和导出，不互相引用。 |
| 19 | 备忘录 Memento | [WorkoutSession.Snapshot](../core/src/main/java/com/pulse/core/session/WorkoutSession.java)、[WorkoutActivity](../app/src/main/java/com/pulse/fitness/ui/WorkoutActivity.java) | 保存动作位置、组数、休息剩余、暂停前状态及累计时间，支持恢复。 |
| 20 | 观察者 Observer | [ObservableState](../core/src/main/java/com/pulse/core/util/ObservableState.java)、[FitnessViewModel](../app/src/main/java/com/pulse/fitness/presentation/FitnessViewModel.java) | 发布 UI 状态变更，Activity 在生命周期边界订阅和取消。 |
| 21 | 状态 State | [WorkoutSession](../core/src/main/java/com/pulse/core/session/WorkoutSession.java) | Warmup、Running、Rest、Paused、Cooldown、Complete 分别处理操作与时钟。 |
| 22 | 策略 Strategy | [GoalStrategy](../core/src/main/java/com/pulse/core/plan/GoalStrategy.java)、GoalStrategyFactory 中的三个策略 | 不同目标替换动作优先级、次数和名称算法。 |
| 23 | 模板方法 Template Method | [PlanGenerationTemplate](../core/src/main/java/com/pulse/core/plan/PlanGenerationTemplate.java) | 固定“校验档案 → 创建候选 → 校验计划”，子类只替换候选生成。 |
| 24 | 访问者 Visitor | [PlanVisitor](../core/src/main/java/com/pulse/core/plan/PlanVisitor.java)、[PlanStatisticsVisitor](../core/src/main/java/com/pulse/core/plan/PlanStatisticsVisitor.java) | 不改周、天、动作类型即可增加训练天数、组数和时间聚合。 |

## 低耦合设计

领域层不依赖 Android，仓库通过接口注入；仅组合根负责装配。模型提示词和服务商格式集中在服务端适配器，XML 页面只发出用户意图。导出格式、输出目标、持久化方式和计划算法分别替换。

24 项模式是项目结构约束，不要求每个属性或页面都额外套用模式。简单数据和布局保持直接。
