# AGENTS.md

面向在本仓库工作的 AI 编码代理（以及人类协作者）。用户向文档在 `README.md`（中文，含玩法说明与配置示例）；本文件关注"怎么改、怎么验证"。

## 项目概览

- Minecraft Forge 模组，为原版自然生成的敌对怪物添加可叠加的**精英词缀**（夜行者 / 霜裔 / 狱火 / 破障者 / 隐身 / 吸血 / 开拓者）。
- 版本：Minecraft 1.20.1、Forge 47.4.23、Java 17；Gradle 8.8 由项目自带 Wrapper 提供（`gradle/wrapper/`），不要单独安装或升级 Gradle。
- mod id：`ori_improved_mobs`；包名：`com.oriimprovedmobs`。两者与 `@Mod` 常量、`gradle.properties` 必须保持一致。
- 设计基线：不新增实体/物品/模型，无运行时依赖，只修改原版实体行为；支持单人 + 专用服务器，能力由服务器结算。
- 模组元数据（`mods.toml` / `pack.mcmeta`）由 `build.gradle` 的 `processResources` 从 `gradle.properties` 展开；改 mod 名称/版本请改 `gradle.properties`，不要直接改 `src/main/resources/META-INF/mods.toml`。

## 构建与测试

### 前置：Gradle 必须用 JDK 17

本机全局 `JAVA_HOME` 指向 JDK 25，Gradle 8.8 在 JDK 25 下无法启动。**每次新开 shell 先切换**：

```bash
# bash / git-bash（本仓库会话默认 shell）
export JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-17.0.20.101-hotspot"
```

```powershell
# PowerShell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot'
```

JDK 17 的实际目录名可在 `C:\Program Files\Eclipse Adoptium\`（bash 下为 `/c/Program Files/Eclipse Adoptium/`）中确认；IDE 的 Gradle JVM 同样设为 JDK 17。

### 命令

在项目根目录执行（bash 用 `./gradlew.bat`，PowerShell 用 `.\gradlew.bat`）：

| 目的 | 命令 | 说明 |
| --- | --- | --- |
| 跑单元测试 | `./gradlew.bat test` | 当前基线：12 个测试类 / 60 个测试全绿 |
| 提交前完整验证 | `./gradlew.bat clean test build` | 最终交付门禁 |
| 生成 IDE 运行配置 | `./gradlew.bat genIntellijRuns` | 按需 |
| 启动客户端 | `./gradlew.bat runClient` | 仅当需要运行时验证；会真正打开游戏进程 |

- `gradle.properties` 设置了 `org.gradle.daemon=false`，每次调用都会 fork 一次性守护进程，构建偏慢属正常，不要为了"提速"改动它。
- 产物：`build/libs/ori_improved_mobs-<mod_version>.jar`（当前 0.2.0；`jar` 任务自动接 `reobfJar` 重混淆）。
- 不要升级 Gradle / Forge / Java 目标版本，除非用户明确要求——ForgeGradle 对版本组合很敏感。
- 编译通过 ≠ 运行时正确。涉及 Forge 事件/世界交互的改动，单元测试不能证明事件边界行为；需要运行时验证时用 `runClient` / `runServer`，并在交付说明中如实写明是否真的做过运行时验证。

## 架构

两层结构：**纯规则层 + 薄事件适配层**。

```text
src/main/java/com/oriimprovedmobs/
├── OriImprovedMobs.java            @Mod 入口：注册 SERVER 配置与 4 个事件处理器
├── config/
│   ├── EliteConfig.java            ForgeConfigSpec（enabled、spawnChance、每词缀开关）+ enabledTypes()
│   └── EliteConfigDefaults.java    纯 Java 默认值常量（供单测断言）
├── elite/                          规则与数据层：静态纯函数、值对象、NBT 编解码
│   ├── EliteType.java              枚举 + id + 翻译键 + fromId()
│   ├── EliteSpawnContext.java      生成环境快照 record(overworld, nether, snowyBiome, night)
│   ├── EliteTypeResolver.java      按环境与开关解析应得词缀集合
│   ├── EliteSpawnPolicy.java       总判定：资格过滤 + 单次概率 roll
│   ├── EliteCombatRules.java       伤害加成 / 命中效果 / 霜裔反击条件
│   ├── EliteHitEffects.java        命中效果 record(slowTarget, igniteTarget)
│   ├── EliteCooldowns.java         霜裔反击冷却（NBT + gameTime）
│   ├── EliteBlockBreakingRules.java 破障者：硬度上限、冷却、Goal 安装条件
│   ├── EliteConcealmentRules.java  隐身：50% 血量阈值的显隐判定
│   ├── EliteClimbingRules.java     开拓者：攀爬/建造条件、16 格范围、10 tick 间隔
│   ├── EliteData.java              实体 NBT 读写（键 `ori_improved_mobs:elite_types`）
│   ├── EliteTypeCodec.java         EnumSet ↔ ListTag（字符串 id）
│   └── ElitePresentation.java      名称前缀展示
└── events/                         Forge 事件薄适配器
    ├── EliteSpawnHandler.java      MobSpawnEvent.FinalizeSpawn：精英化判定
    ├── EliteJoinHandler.java       EntityJoinLevelEvent：发光/名称/永久增益/破障者与开拓者 Goal 安装
    ├── EliteCombatHandler.java     LivingHurtEvent / LivingDamageEvent：战斗词缀效果（含吸血）
    ├── EliteConcealmentHandler.java LivingDamageEvent / LivingHealEvent：隐身显隐刷新
    ├── EliteBlockBreakingGoal.java 破障者拆方块 Goal（尊重 mobGriefing 与 ForgeHooks.canEntityDestroy）
    └── EliteClimbingGoal.java      开拓者建造（梯子/脚手架）+ 主动攀爬 Goal（尊重 mobGriefing 与 Forge 事件）
```

必须遵守的设计约束：

1. **决策逻辑放 `elite` 包的 static 纯函数**：先把世界状态解构成原始值（boolean/float/long/EnumSet）再传入规则函数；`events` 包只负责收集状态、调用规则、应用效果。这是为了让规则可单测——新逻辑不要直接写进 handler。
2. **实体状态存 entity persistent NBT**（`mob.getPersistentData()`，键带 `ori_improved_mobs:` 前缀），禁止用 static Map 保存实体数据。
3. **服务端结算**：处理器先检查 `isClientSide` / `ServerLevel`，客户端直接返回。
4. **精英化 = 每只怪一次总判定**：默认 5% 概率命中后，叠加该怪环境匹配的**所有**词缀；随机种子由 mob UUID 派生，同一只怪结果确定。
5. 精英资格：自然生成（NATURAL）+ 原版 `MONSTER` 类别 + 非 BOSS tag + 尚未精英化。
6. 配置在生成时读取；改配置只影响之后生成的怪。

具体玩法数值与用户可见行为见 `README.md`。

## 测试与 TDD 工作流

- JUnit 5（`org.junit.jupiter:junit-jupiter:5.10.2`；`tasks.named('test')` 已启用 `useJUnitPlatform()`）。
- 测试目录镜像主代码包结构：`src/test/java/com/oriimprovedmobs/...`。测试可以引用 Minecraft 的 NBT 类（在测试类路径上），但不要构造世界/实体。
- 命名约定：驼峰描述句（如 `rejectsRollAtTheSpawnChanceBoundary`），断言边界值（roll == chance 不算精英；恰好半血不加成伤害、隐身词缀仍隐身；冷却正好到期才可用）。
- `BuildEnvironmentTest` 守护测试运行时必须是 Java 17。
- 行为变更走 RED → GREEN：
  1. 先写会失败的测试（提交信息 `test:`）；
  2. 最小实现使其通过（提交信息 `feat:`）；
  3. 跑全量 `./gradlew.bat test`；交付前 `./gradlew.bat clean test build`。
- Git 提交信息用 Conventional Commits：`feat:` / `test:` / `docs:` / `chore:`，且测试提交先于实现提交。

## 新增精英词缀清单

按既有词缀（破障者 / 隐身 / 吸血 / 开拓者）的提交链：

1. `EliteType` 加枚举项 + id；
2. `EliteConfigDefaults` 加默认值（ForgeConfigSpec 的默认值在单测里通过 `getDefault()` 断言）；
3. `EliteConfig` 定义开关，并把参数加进 `enabledTypes(...)`（补测试）；
4. 环境条件加进 `EliteTypeResolver`（若该词缀有环境限制）；
5. 行为规则以纯函数加进对应 `*Rules` 类 + 边界测试；
6. 在 `events/` 对应处理器接线（生成 / 入世界 / 战斗 / Goal）；新增处理器要在 `OriImprovedMobs.java` 里**同时**加显式 import 和 `MinecraftForge.EVENT_BUS.register(...)`（漏 import 会在接线后直接编译失败）；
7. `assets/ori_improved_mobs/lang/en_us.json` 与 `zh_cn.json` 都加 `entity.ori_improved_mobs.elite.<id>`；
8. 更新 `README.md` 的词缀表与配置示例，并同步本文件的测试基线数字与架构树。

## 代码风格

- 4 空格缩进；显式 import（不用通配符）；UTF-8（`build.gradle` 已设）。
- 规则类：`public final class` + 私有构造器 + 全 static 方法；值对象用 `record`。
- 命名：时间常量以 `_TICKS` 结尾；配置键 camelCase（TOML 同名）；NBT 键带模组命名空间前缀。
- 用户可见变更三处同步：`README.md`、lang 文件、配置项。

## 仓库卫生

- `build/`、`run/`、`logs/`、`.gradle/` 是生成物，已在 `.gitignore`；不要提交 `run/` 下的存档与日志。
- 除非用户明确要求，不要 commit、push 或改写历史。
- 提交前先确认构建真的成功：不要把 `git commit` 和 gradle 串在同一条 shell 命令里（管道/串联会掩盖 `BUILD FAILED`），先单独跑并读到 `BUILD SUCCESSFUL` 再提交；若坏提交已产生，修好后用 `git reset --soft HEAD~1` 重做。
