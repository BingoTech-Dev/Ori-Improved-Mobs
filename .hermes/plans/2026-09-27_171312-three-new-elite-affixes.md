# 新增三个精英词缀（隐身 / 吸血 / 开拓者）实施计划

## Goal
在现有精英怪框架里新增三个可叠加词缀——隐身（SHROUDED，半血以下现身）、吸血（VAMPIRIC，按实际伤害 50% 回血）、开拓者（PATHFINDER，造梯子/脚手架并主动攀爬追击）——并保持单测全绿、`clean test build` 通过。

## Current context / assumptions

- 仓库：Minecraft 1.20.1 / Forge 47.4.23 / Java 17 / Gradle Wrapper 8.8；mod id `ori_improved_mobs`，包 `com.oriimprovedmobs`。当前 4 个词缀（NIGHT_STALKER / FROSTBORN / INFERNAL / BREACHER）已完成，基线为 **10 个测试类 / 41 个测试全绿**（`AGENTS.md`）。
- 架构约定（`AGENTS.md`）：决策逻辑放 `elite/` 的 static 纯函数（可单测），`events/` 只做世界状态收集 + 调用规则 + 应用效果；实体状态存 entity persistent NBT（键带 `ori_improved_mobs:` 前缀）；服务端结算；行为变更走 RED → GREEN，`test:` 提交先于 `feat:` 提交。
- 构建前置：本机全局 `JAVA_HOME` 指向 JDK 25，Gradle 8.8 起不来。每个新 shell 先执行：
  `export JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-17.0.20.101-hotspot"`
  （`gradle.properties` 设了 `org.gradle.daemon=false`，每次调用都会 fork，慢属正常。）
- 已核实的运行时事实（来自开发存档 `run/saves/新的世界` 的实体 NBT 解码）：2 只骷髅 + 1 只爬行者确实带 `night_stalker` 词缀与命名存进了存档 → 「自然生成 → 精英化 → 入世界命名」链路真实跑通过；但该次游玩只有 2 分钟/34 个刷怪蛋/0 击杀，战斗与破障者拆方块**从未**在游戏内验证。
- **用户已决策本轮不做运行时验证**：验收 = 单测全绿 + `./gradlew.bat clean test build`，交付说明必须如实注明「未做运行时验证」。
- 已核实的游戏机制事实（决定开拓者实现方式）：
  - 1.20.1 Java 版怪物 AI **不会**主动用梯子/脚手架寻路——游戏 jar 里 `WalkNodeEvaluator` / `NodeEvaluator` / `BlockPathTypes` 没有任何 `LADDER` / `#climbable` 引用；梯子只有"被移动输入推着贴上时"才物理上爬。
  - 因此开拓者必须自带攀爬 Goal（否则功能形同虚设），用户已确认：**两样都建 + 攀爬 Goal**。
  - 脚手架竖直柱**无高度上限**（`ScaffoldingBlock.getDistance`：下方是脚手架时距离不变；水平伸出才 +1，`getDistance >= 7` 才塌），脚手架顶面可站、内部无碰撞（可穿过）。
- 已核实的 API 事实（防止计划代码编不过）：
  - `ForgeEventFactory.onBlockPlace(Entity, BlockSnapshot, Direction)` 返回 **true = 事件被取消（禁止放置）**，false = 允许（来源：Forge sources jar `ForgeEventFactory.java:165-170`）。
  - `ScaffoldingBlock.STABILITY_MAX_DISTANCE == 7`，`canSurvive` 要求 `getDistance(...) < 7`。
  - `LadderBlock.FACING = 点击面方向`；支撑面在 `pos.relative(FACING.getOpposite())`。
  - `LivingDamageEvent`（有 `getAmount()`）与 `LivingHealEvent` 在 Forge 1.20.1 存在；`Mob#getMoveControl().setWantedPosition(double,double,double,double)`、`JumpControl#jump()`、`Direction.getNearest(double,double,double)`、`BlockTags.CLIMBABLE` 均存在。
- 本轮设计约束（用户授权"按实际情况决定"，即沿用首版约束）：**不加新实体/物品/模型/运行时依赖**——新词缀只用原版效果与 `Blocks.LADDER` / `Blocks.SCAFFOLDING`。
- 新增常量一律做**代码常量**（不新增配置项），与既有的 `EliteBlockBreakingRules.MAX_HARDNESS = 3.0F` 风格一致（YAGNI）；三个词缀各加一个 `xxxEnabled` 布尔开关（沿用破障者模式）。
- 命名（可在开工前否决）：`SHROUDED`（隐身 / Shrouded）、`VAMPIRIC`（吸血 / Vampiric）、`PATHFINDER`（开拓者 / Pathfinder）；配置键 `shroudedEnabled` / `vampiricEnabled` / `pathfinderEnabled`。
- `mod_version` 保持 `0.1.0`（无发布流程；见"开放问题"）。
- 关于 commit：`AGENTS.md` 说"除非用户明确要求，不要 commit"；本计划按用户要求的 TDD 工作流列出提交步骤，执行时若用户不希望由代理提交，跳过 commit 步骤即可（其余步骤不变）。

## Architecture / proposed approach
完全沿用现有两层结构：`elite/` 新增两个纯规则类（`EliteConcealmentRules`、`EliteClimbingRules`）并扩展 `EliteCombatRules`（吸血比例）；`events/` 新增两个适配器（`EliteConcealmentHandler` 监听受伤/回血事件管理显隐状态、`EliteClimbingGoal` 负责放置方块与攀爬），并小改 `EliteJoinHandler`（入世界时用统一的 `refresh()` 恢复显隐状态、安装攀爬 Goal）与 `EliteCombatHandler`（监听 `LivingDamageEvent` 实现吸血）。显隐外观（发光 / 名字可见性 / 隐身效果）由单一幂等函数 `EliteConcealmentHandler.refresh(...)` 管理，避免状态分叉。

## Step-by-step tasks

> 每个代码任务严格按 RED → GREEN → COMMIT：先写/改测试 → 跑命令确认失败 → 最小实现 → 跑命令确认通过 → 提交。命令都在项目根目录的 git-bash 里执行。

### Task 0 — 基线自检（约 2 分钟；无代码改动）

```bash
export JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-17.0.20.101-hotspot"
./gradlew.bat test
```

**预期**：`BUILD SUCCESSFUL`；测试报告 `build/reports/tests/test/index.html` 显示 10 个测试类 / 41 个测试全绿。若不是这个状态，停止并报告实际输出，不要继续。

（可选、一次性）把本计划文件提交进仓库（仓库有先例）：
```bash
git add .hermes/plans/2026-09-27_171312-three-new-elite-affixes.md
git commit -m "chore: add three-new-affixes development plan"
```

### Task 1 — 三个新词缀的枚举 / 配置 / 环境解析（TDD 一对提交）

#### 1a. 测试先行（commit `test:`）

改动 4 个测试文件：

**`src/test/java/com/oriimprovedmobs/config/EliteConfigDefaultsTest.java`** — 全量替换为：
```java
package com.oriimprovedmobs.config;

import org.junit.jupiter.api.Test;

import com.oriimprovedmobs.elite.EliteType;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EliteConfigDefaultsTest {
    @Test
    void breacherConfigDefaultsToEnabledAndHasAnIndependentSwitch() {
        assertTrue(EliteConfigDefaults.BREACHER_ENABLED);
        assertTrue(EliteConfig.BREACHER_ENABLED.getDefault());
        assertEquals(EnumSet.allOf(EliteType.class),
                EliteConfig.enabledTypes(true, true, true, true, true, true, true));
        assertEquals(EnumSet.of(EliteType.NIGHT_STALKER, EliteType.FROSTBORN, EliteType.INFERNAL,
                        EliteType.SHROUDED, EliteType.VAMPIRIC, EliteType.PATHFINDER),
                EliteConfig.enabledTypes(true, true, true, false, true, true, true));
    }

    @Test
    void newAffixConfigDefaultsToEnabledAndHasIndependentSwitches() {
        assertTrue(EliteConfigDefaults.SHROUDED_ENABLED);
        assertTrue(EliteConfigDefaults.VAMPIRIC_ENABLED);
        assertTrue(EliteConfigDefaults.PATHFINDER_ENABLED);
        assertTrue(EliteConfig.SHROUDED_ENABLED.getDefault());
        assertTrue(EliteConfig.VAMPIRIC_ENABLED.getDefault());
        assertTrue(EliteConfig.PATHFINDER_ENABLED.getDefault());
        assertEquals(EnumSet.of(EliteType.SHROUDED),
                EliteConfig.enabledTypes(false, false, false, false, true, false, false));
        assertEquals(EnumSet.of(EliteType.VAMPIRIC),
                EliteConfig.enabledTypes(false, false, false, false, false, true, false));
        assertEquals(EnumSet.of(EliteType.PATHFINDER),
                EliteConfig.enabledTypes(false, false, false, false, false, false, true));
    }

    @Test
    void defaultsEnableEachEliteAtFivePercent() {
        assertTrue(EliteConfigDefaults.ENABLED);
        assertEquals(0.05D, EliteConfigDefaults.SPAWN_CHANCE, 0.0D);
        assertTrue(EliteConfigDefaults.NIGHT_STALKER_ENABLED);
        assertTrue(EliteConfigDefaults.FROSTBORN_ENABLED);
        assertTrue(EliteConfigDefaults.INFERNAL_ENABLED);
        assertTrue(EliteConfigDefaults.BREACHER_ENABLED);
        assertTrue(EliteConfigDefaults.SHROUDED_ENABLED);
        assertTrue(EliteConfigDefaults.VAMPIRIC_ENABLED);
        assertTrue(EliteConfigDefaults.PATHFINDER_ENABLED);
    }

    @Test
    void configSpecUsesTheDocumentedDefaults() {
        assertTrue(EliteConfig.ENABLED.getDefault());
        assertEquals(0.05D, EliteConfig.SPAWN_CHANCE.getDefault(), 0.0D);
        assertEquals(EnumSet.allOf(EliteType.class),
                EliteConfig.enabledTypes(true, true, true, true, true, true, true));
    }

    @Test
    void enabledTypesReturnsAnIndependentSet() {
        var first = EliteConfig.enabledTypes(true, true, true, true, true, true, true);
        first.clear();

        assertEquals(EnumSet.allOf(EliteType.class),
                EliteConfig.enabledTypes(true, true, true, true, true, true, true));
    }

    @Test
    void enabledTypesHonorEachPerTypeSwitch() {
        assertEquals(EnumSet.of(EliteType.FROSTBORN),
                EliteConfig.enabledTypes(false, true, false, false, false, false, false));
        assertEquals(EnumSet.noneOf(EliteType.class),
                EliteConfig.enabledTypes(false, false, false, false, false, false, false));
    }
}
```

**`src/test/java/com/oriimprovedmobs/elite/EliteTypeResolverTest.java`** — 在 `returnsEmptyWhenNoEnvironmentMatches` 之后、`legacyTypes()` 之前**新增**两个测试（文件其余部分不动）：
```java
    @Test
    void resolvesShroudedOnlyAtOverworldNight() {
        var enabled = EnumSet.of(EliteType.SHROUDED);

        assertEquals(EnumSet.of(EliteType.SHROUDED), EliteTypeResolver.resolve(
                new EliteSpawnContext(true, false, false, true), enabled));
        assertEquals(EnumSet.noneOf(EliteType.class), EliteTypeResolver.resolve(
                new EliteSpawnContext(true, false, false, false), enabled));
        assertEquals(EnumSet.noneOf(EliteType.class), EliteTypeResolver.resolve(
                new EliteSpawnContext(false, true, false, true), enabled));
    }

    @Test
    void resolvesVampiricAndPathfinderInEveryDimension() {
        var enabled = EnumSet.of(EliteType.VAMPIRIC, EliteType.PATHFINDER);
        var expected = EnumSet.of(EliteType.VAMPIRIC, EliteType.PATHFINDER);

        assertEquals(expected, EliteTypeResolver.resolve(
                new EliteSpawnContext(true, false, false, false), enabled));
        assertEquals(expected, EliteTypeResolver.resolve(
                new EliteSpawnContext(false, true, false, false), enabled));
        assertEquals(expected, EliteTypeResolver.resolve(
                new EliteSpawnContext(false, false, false, false), enabled));
    }
```

**`src/test/java/com/oriimprovedmobs/elite/EliteSpawnPolicyTest.java`** — 两处断言改成包含新词缀（`legacyTypes()` 辅助方法保持不动）：

`assignsMatchingTypeWhenNaturalSpawnRollSucceeds` 的断言替换为：
```java
        assertEquals(EnumSet.of(EliteType.NIGHT_STALKER, EliteType.SHROUDED, EliteType.VAMPIRIC,
                EliteType.PATHFINDER, EliteType.BREACHER), result);
```
`oneSuccessfulRollAssignsEveryMatchingType` 的断言替换为：
```java
        assertEquals(EnumSet.of(EliteType.NIGHT_STALKER, EliteType.FROSTBORN, EliteType.SHROUDED,
                EliteType.VAMPIRIC, EliteType.PATHFINDER, EliteType.BREACHER), result);
```

**`src/test/java/com/oriimprovedmobs/elite/EliteTypeCodecTest.java`** — 末尾新增：
```java
    @Test
    void storesNewAffixIdsUsingStableIds() {
        assertEquals("shrouded", EliteType.SHROUDED.id());
        assertEquals("vampiric", EliteType.VAMPIRIC.id());
        assertEquals("pathfinder", EliteType.PATHFINDER.id());
    }
```

**`src/test/java/com/oriimprovedmobs/elite/ElitePresentationTest.java`** — 末尾新增：
```java
    @Test
    void exposesNewAffixLabelKeys() {
        assertEquals(List.of(
                "entity.ori_improved_mobs.elite.shrouded",
                "entity.ori_improved_mobs.elite.vampiric",
                "entity.ori_improved_mobs.elite.pathfinder"),
                ElitePresentation.labelKeys(Set.of(
                        EliteType.SHROUDED, EliteType.VAMPIRIC, EliteType.PATHFINDER)));
    }
```

运行：
```bash
./gradlew.bat test
```
**预期 RED**：`compileTestJava` 失败，报 `找不到符号`/`cannot find symbol` 之类错误（`SHROUDED`、`VAMPIRIC`、`PATHFINDER`、以及 `enabledTypes(...)` 参数数量不匹配；实际文案以输出为准）。

提交：
```bash
git add src/test/java/com/oriimprovedmobs/config/EliteConfigDefaultsTest.java \
        src/test/java/com/oriimprovedmobs/elite/EliteTypeResolverTest.java \
        src/test/java/com/oriimprovedmobs/elite/EliteSpawnPolicyTest.java \
        src/test/java/com/oriimprovedmobs/elite/EliteTypeCodecTest.java \
        src/test/java/com/oriimprovedmobs/elite/ElitePresentationTest.java
git commit -m "test: define shrouded, vampiric and pathfinder affixes"
```

#### 1b. 最小实现（commit `feat:`）

**`src/main/java/com/oriimprovedmobs/elite/EliteType.java`** — 全量替换为：
```java
package com.oriimprovedmobs.elite;

import java.util.Arrays;
import java.util.Optional;

public enum EliteType {
    NIGHT_STALKER("night_stalker"),
    FROSTBORN("frostborn"),
    INFERNAL("infernal"),
    BREACHER("breacher"),
    SHROUDED("shrouded"),
    VAMPIRIC("vampiric"),
    PATHFINDER("pathfinder");

    private final String id;

    EliteType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public String translationKey() {
        return "entity.ori_improved_mobs.elite." + id;
    }

    public static Optional<EliteType> fromId(String id) {
        return Arrays.stream(values()).filter(type -> type.id.equals(id)).findFirst();
    }
}
```

**`src/main/java/com/oriimprovedmobs/config/EliteConfigDefaults.java`** — 全量替换为：
```java
package com.oriimprovedmobs.config;

public final class EliteConfigDefaults {
    public static final boolean ENABLED = true;
    public static final double SPAWN_CHANCE = 0.05D;
    public static final boolean NIGHT_STALKER_ENABLED = true;
    public static final boolean FROSTBORN_ENABLED = true;
    public static final boolean INFERNAL_ENABLED = true;
    public static final boolean BREACHER_ENABLED = true;
    public static final boolean SHROUDED_ENABLED = true;
    public static final boolean VAMPIRIC_ENABLED = true;
    public static final boolean PATHFINDER_ENABLED = true;

    private EliteConfigDefaults() {}
}
```

**`src/main/java/com/oriimprovedmobs/config/EliteConfig.java`** — 全量替换为：
```java
package com.oriimprovedmobs.config;

import com.oriimprovedmobs.elite.EliteType;
import java.util.EnumSet;
import net.minecraftforge.common.ForgeConfigSpec;

public final class EliteConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.DoubleValue SPAWN_CHANCE;
    public static final ForgeConfigSpec.BooleanValue NIGHT_STALKER_ENABLED;
    public static final ForgeConfigSpec.BooleanValue FROSTBORN_ENABLED;
    public static final ForgeConfigSpec.BooleanValue INFERNAL_ENABLED;
    public static final ForgeConfigSpec.BooleanValue BREACHER_ENABLED;
    public static final ForgeConfigSpec.BooleanValue SHROUDED_ENABLED;
    public static final ForgeConfigSpec.BooleanValue VAMPIRIC_ENABLED;
    public static final ForgeConfigSpec.BooleanValue PATHFINDER_ENABLED;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("elites");
        ENABLED = builder.define("enabled", EliteConfigDefaults.ENABLED);
        SPAWN_CHANCE = builder.defineInRange(
                "spawnChance", EliteConfigDefaults.SPAWN_CHANCE, 0.0D, 1.0D);
        NIGHT_STALKER_ENABLED = builder.define("nightStalkerEnabled", EliteConfigDefaults.NIGHT_STALKER_ENABLED);
        FROSTBORN_ENABLED = builder.define("frostbornEnabled", EliteConfigDefaults.FROSTBORN_ENABLED);
        INFERNAL_ENABLED = builder.define("infernalEnabled", EliteConfigDefaults.INFERNAL_ENABLED);
        BREACHER_ENABLED = builder.define("breacherEnabled", EliteConfigDefaults.BREACHER_ENABLED);
        SHROUDED_ENABLED = builder.define("shroudedEnabled", EliteConfigDefaults.SHROUDED_ENABLED);
        VAMPIRIC_ENABLED = builder.define("vampiricEnabled", EliteConfigDefaults.VAMPIRIC_ENABLED);
        PATHFINDER_ENABLED = builder.define("pathfinderEnabled", EliteConfigDefaults.PATHFINDER_ENABLED);
        builder.pop();
        SPEC = builder.build();
    }

    private EliteConfig() {}

    public static EnumSet<EliteType> enabledTypes() {
        return enabledTypes(
                NIGHT_STALKER_ENABLED.get(),
                FROSTBORN_ENABLED.get(),
                INFERNAL_ENABLED.get(),
                BREACHER_ENABLED.get(),
                SHROUDED_ENABLED.get(),
                VAMPIRIC_ENABLED.get(),
                PATHFINDER_ENABLED.get());
    }

    static EnumSet<EliteType> enabledTypes(
            boolean nightStalkerEnabled,
            boolean frostbornEnabled,
            boolean infernalEnabled,
            boolean breacherEnabled,
            boolean shroudedEnabled,
            boolean vampiricEnabled,
            boolean pathfinderEnabled) {
        EnumSet<EliteType> enabled = EnumSet.noneOf(EliteType.class);
        if (nightStalkerEnabled) {
            enabled.add(EliteType.NIGHT_STALKER);
        }
        if (frostbornEnabled) {
            enabled.add(EliteType.FROSTBORN);
        }
        if (infernalEnabled) {
            enabled.add(EliteType.INFERNAL);
        }
        if (breacherEnabled) {
            enabled.add(EliteType.BREACHER);
        }
        if (shroudedEnabled) {
            enabled.add(EliteType.SHROUDED);
        }
        if (vampiricEnabled) {
            enabled.add(EliteType.VAMPIRIC);
        }
        if (pathfinderEnabled) {
            enabled.add(EliteType.PATHFINDER);
        }
        return enabled;
    }
}
```

**`src/main/java/com/oriimprovedmobs/elite/EliteTypeResolver.java`** — 在 `NIGHT_STALKER` 判定块之后插入 SHROUDED 块，在 `BREACHER` 块之后追加两个全维度块（其余不动）：
```java
        if (context.overworld() && context.night() && enabled.contains(EliteType.SHROUDED)) {
            result.add(EliteType.SHROUDED);
        }
        ...
        if (enabled.contains(EliteType.VAMPIRIC)) {
            result.add(EliteType.VAMPIRIC);
        }
        if (enabled.contains(EliteType.PATHFINDER)) {
            result.add(EliteType.PATHFINDER);
        }
```

运行：
```bash
./gradlew.bat test
```
**预期 GREEN**：`BUILD SUCCESSFUL`，全部测试通过（本任务之后测试类数量不变）。

提交：
```bash
git add src/main/java/com/oriimprovedmobs/elite/EliteType.java \
        src/main/java/com/oriimprovedmobs/elite/EliteTypeResolver.java \
        src/main/java/com/oriimprovedmobs/config/EliteConfig.java \
        src/main/java/com/oriimprovedmobs/config/EliteConfigDefaults.java
git commit -m "feat: add shrouded, vampiric and pathfinder affix types"
```

### Task 2 — 隐身（SHROUDED）显隐机制

语义（已确认）：HP ≥ 50% 时完全隐身（隐身效果无粒子 + 无发光 + 无名字）；HP < 50% 时完全现形（移除隐身效果 + 发光 + 名字浮动显示）。状态在 入世界 / 受伤 / 回血 三个时机幂等刷新。

#### 2a. 测试先行（commit `test:`）

**新建 `src/test/java/com/oriimprovedmobs/elite/EliteConcealmentRulesTest.java`**：
```java
package com.oriimprovedmobs.elite;

import java.util.EnumSet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EliteConcealmentRulesTest {
    @Test
    void concealsShroudedMobAtAndAboveHalfHealth() {
        var types = EnumSet.of(EliteType.SHROUDED);

        assertTrue(EliteConcealmentRules.shouldConceal(types, 20.0F, 20.0F));
        assertTrue(EliteConcealmentRules.shouldConceal(types, 10.0F, 20.0F));
        assertFalse(EliteConcealmentRules.shouldConceal(types, 9.9F, 20.0F));
    }

    @Test
    void neverConcealsMobsWithoutTheShroudedAffix() {
        assertFalse(EliteConcealmentRules.shouldConceal(
                EnumSet.of(EliteType.NIGHT_STALKER), 20.0F, 20.0F));
    }

    @Test
    void concealedShroudedMobHidesGlowAndName() {
        var types = EnumSet.of(EliteType.SHROUDED);

        assertFalse(EliteConcealmentRules.shouldGlow(types, true));
        assertFalse(EliteConcealmentRules.shouldShowName(types, true));
    }

    @Test
    void revealedShroudedMobShowsGlowAndName() {
        var types = EnumSet.of(EliteType.SHROUDED);

        assertTrue(EliteConcealmentRules.shouldGlow(types, false));
        assertTrue(EliteConcealmentRules.shouldShowName(types, false));
    }

    @Test
    void otherElitesKeepGlowingWithHoverOnlyNames() {
        var types = EnumSet.of(EliteType.INFERNAL);

        assertTrue(EliteConcealmentRules.shouldGlow(types, false));
        assertFalse(EliteConcealmentRules.shouldShowName(types, false));
    }
}
```

运行：
```bash
./gradlew.bat test --tests com.oriimprovedmobs.elite.EliteConcealmentRulesTest
```
**预期 RED**：编译失败，`找不到符号: 类 EliteConcealmentRules`。

提交：
```bash
git add src/test/java/com/oriimprovedmobs/elite/EliteConcealmentRulesTest.java
git commit -m "test: define shrouded concealment rules"
```

#### 2b. 最小实现（commit `feat:`）

**新建 `src/main/java/com/oriimprovedmobs/elite/EliteConcealmentRules.java`**：
```java
package com.oriimprovedmobs.elite;

import java.util.Set;

public final class EliteConcealmentRules {
    public static final float REVEAL_HEALTH_FRACTION = 0.5F;

    private EliteConcealmentRules() {}

    public static boolean shouldConceal(Set<EliteType> types, float effectiveHealth, float maxHealth) {
        return types.contains(EliteType.SHROUDED) && effectiveHealth >= maxHealth * REVEAL_HEALTH_FRACTION;
    }

    public static boolean shouldGlow(Set<EliteType> types, boolean concealed) {
        return !types.contains(EliteType.SHROUDED) || !concealed;
    }

    public static boolean shouldShowName(Set<EliteType> types, boolean concealed) {
        return types.contains(EliteType.SHROUDED) && !concealed;
    }
}
```

**新建 `src/main/java/com/oriimprovedmobs/events/EliteConcealmentHandler.java`**：
```java
package com.oriimprovedmobs.events;

import com.oriimprovedmobs.elite.EliteConcealmentRules;
import com.oriimprovedmobs.elite.EliteData;
import com.oriimprovedmobs.elite.EliteType;
import java.util.EnumSet;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class EliteConcealmentHandler {
    private static final int CONCEALED_EFFECT_DURATION = Integer.MAX_VALUE;

    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide) {
            return;
        }
        EnumSet<EliteType> types = EliteData.read(mob);
        if (!types.contains(EliteType.SHROUDED)) {
            return;
        }
        refresh(mob, types, Math.max(0.0F, mob.getHealth() - event.getAmount()));
    }

    @SubscribeEvent
    public void onLivingHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide) {
            return;
        }
        EnumSet<EliteType> types = EliteData.read(mob);
        if (!types.contains(EliteType.SHROUDED)) {
            return;
        }
        refresh(mob, types, Math.min(mob.getMaxHealth(), mob.getHealth() + event.getAmount()));
    }

    static void refresh(Mob mob, EnumSet<EliteType> types, float effectiveHealth) {
        boolean concealed = EliteConcealmentRules.shouldConceal(types, effectiveHealth, mob.getMaxHealth());
        mob.setGlowingTag(EliteConcealmentRules.shouldGlow(types, concealed));
        if (!types.contains(EliteType.SHROUDED)) {
            return;
        }
        mob.setCustomNameVisible(EliteConcealmentRules.shouldShowName(types, concealed));
        if (concealed) {
            if (!mob.hasEffect(MobEffects.INVISIBILITY)) {
                mob.addEffect(new MobEffectInstance(
                        MobEffects.INVISIBILITY, CONCEALED_EFFECT_DURATION, 0, false, false));
            }
        } else {
            mob.removeEffect(MobEffects.INVISIBILITY);
        }
    }
}
```

**`src/main/java/com/oriimprovedmobs/events/EliteJoinHandler.java`** — 全量替换为（注意删掉原来的无条件 `setGlowingTag(true)`，改为调用 `refresh`；本任务先不加攀爬 Goal 安装，Task 4 再补）：
```java
package com.oriimprovedmobs.events;

import com.oriimprovedmobs.elite.EliteBlockBreakingRules;
import com.oriimprovedmobs.elite.EliteData;
import com.oriimprovedmobs.elite.ElitePresentation;
import com.oriimprovedmobs.elite.EliteType;
import java.util.EnumSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class EliteJoinHandler {
    private static final String NAME_APPLIED_KEY = "ori_improved_mobs:elite_name_applied";
    private static final int ELITE_EFFECT_DURATION = Integer.MAX_VALUE;

    @SubscribeEvent
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof Mob mob)) {
            return;
        }

        EnumSet<EliteType> types = EliteData.read(mob);
        if (types.isEmpty()) {
            return;
        }

        mob.setCustomNameVisible(false);

        CompoundTag persistentData = mob.getPersistentData();
        if (!persistentData.getBoolean(NAME_APPLIED_KEY)) {
            mob.setCustomName(ElitePresentation.name(mob.getDisplayName(), types));
            persistentData.putBoolean(NAME_APPLIED_KEY, true);
        }

        EliteConcealmentHandler.refresh(mob, types, mob.getHealth());

        if (types.contains(EliteType.NIGHT_STALKER)) {
            addEffectIfMissing(mob, MobEffects.MOVEMENT_SPEED);
        }
        if (types.contains(EliteType.INFERNAL)) {
            addEffectIfMissing(mob, MobEffects.FIRE_RESISTANCE);
        }

        boolean blockBreakingGoalInstalled = mob.goalSelector.getAvailableGoals().stream()
                .anyMatch(wrappedGoal -> wrappedGoal.getGoal() instanceof EliteBlockBreakingGoal);
        if (EliteBlockBreakingRules.shouldInstallGoal(
                types.contains(EliteType.BREACHER), blockBreakingGoalInstalled)) {
            mob.goalSelector.addGoal(1, new EliteBlockBreakingGoal(mob));
        }
    }

    private static void addEffectIfMissing(Mob mob, MobEffect effect) {
        if (!mob.hasEffect(effect)) {
            mob.addEffect(new MobEffectInstance(effect, ELITE_EFFECT_DURATION, 0, false, true));
        }
    }
}
```

**`src/main/java/com/oriimprovedmobs/OriImprovedMobs.java`** — 构造函数加一行注册：
```java
        MinecraftForge.EVENT_BUS.register(new EliteConcealmentHandler());
```

运行：
```bash
./gradlew.bat test
```
**预期 GREEN**：`BUILD SUCCESSFUL`，测试全绿（含新增 5 个）。

提交：
```bash
git add src/main/java/com/oriimprovedmobs/elite/EliteConcealmentRules.java \
        src/main/java/com/oriimprovedmobs/events/EliteConcealmentHandler.java \
        src/main/java/com/oriimprovedmobs/events/EliteJoinHandler.java \
        src/main/java/com/oriimprovedmobs/OriImprovedMobs.java
git commit -m "feat: hide shrouded elites above half health"
```

> 说明：事件接线本身无法单测（需要世界/实体），本任务验证 = 规则单测 + 编译通过。真实行为属"未做运行时验证"。

### Task 3 — 吸血（VAMPIRIC）

语义（已确认）：精英造成实际伤害时，按 `实际伤害 × 50%` 回复自身生命；近战与远程（箭）都触发。

#### 3a. 测试先行（commit `test:`）

**`src/test/java/com/oriimprovedmobs/elite/EliteCombatRulesTest.java`** — 末尾新增两个测试（文件其余不动）：
```java
    @Test
    void vampiricElitesHealHalfOfTheDamageTheyDeal() {
        assertEquals(2.0F,
                EliteCombatRules.lifestealHeal(EnumSet.of(EliteType.VAMPIRIC), 4.0F), 0.0001F);
    }

    @Test
    void lifestealIsZeroWithoutVampiricOrWithoutDamage() {
        assertEquals(0.0F,
                EliteCombatRules.lifestealHeal(EnumSet.of(EliteType.FROSTBORN), 4.0F), 0.0001F);
        assertEquals(0.0F,
                EliteCombatRules.lifestealHeal(EnumSet.of(EliteType.VAMPIRIC), 0.0F), 0.0001F);
    }
```

运行：
```bash
./gradlew.bat test --tests com.oriimprovedmobs.elite.EliteCombatRulesTest
```
**预期 RED**：编译失败，`找不到符号: 方法 lifestealHeal(...)`。

提交：
```bash
git add src/test/java/com/oriimprovedmobs/elite/EliteCombatRulesTest.java
git commit -m "test: define vampiric lifesteal rule"
```

#### 3b. 最小实现（commit `feat:`）

**`src/main/java/com/oriimprovedmobs/elite/EliteCombatRules.java`** — 类里新增常量与函数（放在 `onHit` 之前即可）：
```java
    public static final float LIFESTEAL_RATIO = 0.5F;

    public static float lifestealHeal(Set<EliteType> types, float actualDamage) {
        if (!types.contains(EliteType.VAMPIRIC) || actualDamage <= 0.0F) {
            return 0.0F;
        }
        return actualDamage * LIFESTEAL_RATIO;
    }
```

**`src/main/java/com/oriimprovedmobs/events/EliteCombatHandler.java`** — 新增 `LivingDamageEvent` 监听（其余方法不动）；注意补 import `net.minecraftforge.event.entity.living.LivingDamageEvent`：
```java
    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent event) {
        if (event.getAmount() <= 0.0F || event.getEntity().level().isClientSide) {
            return;
        }
        Entity causingEntity = event.getSource().getEntity();
        if (!(causingEntity instanceof Mob attacker) || attacker == event.getEntity()) {
            return;
        }
        EnumSet<EliteType> types = EliteData.read(attacker);
        float healAmount = EliteCombatRules.lifestealHeal(types, event.getAmount());
        if (healAmount > 0.0F) {
            attacker.heal(healAmount);
        }
    }
```

运行：
```bash
./gradlew.bat test
```
**预期 GREEN**：`BUILD SUCCESSFUL`，测试全绿（含新增 2 个）。

提交：
```bash
git add src/main/java/com/oriimprovedmobs/elite/EliteCombatRules.java \
        src/main/java/com/oriimprovedmobs/events/EliteCombatHandler.java
git commit -m "feat: heal vampiric elites from damage dealt"
```

> 说明：用 `LivingDamageEvent`（护甲/吸收之后的实际伤害）而非 `LivingHurtEvent`，保证"按实际伤害回血"。吸血触发自身回血会再触发 `LivingHealEvent` → 隐身怪回满后自动再次隐身（Task 2 的机制天然覆盖此联动）。

### Task 4 — 开拓者（PATHFINDER）：放置 + 攀爬

语义（已确认）：追击玩家、路径不通、玩家至少高 1 格时——有墙则贴墙放梯子；开阔地则在脚下盖脚手架柱；只要身处可攀爬方块内且玩家在上方就向上爬。每 10 ticks 最多放置 1 格；遵守 `mobGriefing` 与 Forge 方块保护事件。

#### 4a. 测试先行（commit `test:`）

**新建 `src/test/java/com/oriimprovedmobs/elite/EliteClimbingRulesTest.java`**：
```java
package com.oriimprovedmobs.elite;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EliteClimbingRulesTest {
    @Test
    void installsOnlyOnceAndOnlyForPathfinders() {
        assertTrue(EliteClimbingRules.shouldInstallGoal(true, false));
        assertFalse(EliteClimbingRules.shouldInstallGoal(true, true));
        assertFalse(EliteClimbingRules.shouldInstallGoal(false, false));
    }

    @Test
    void requiresTheTargetToBeAWholeBlockHigher() {
        assertTrue(EliteClimbingRules.targetIsHigher(64.0D, 65.0D));
        assertFalse(EliteClimbingRules.targetIsHigher(64.0D, 64.99D));
    }

    @Test
    void acceptsTargetsWithinSixteenBlocksAndRejectsFartherOnes() {
        assertTrue(EliteClimbingRules.withinBuildRange(16.0D, 0.0D));
        assertTrue(EliteClimbingRules.withinBuildRange(12.0D, 10.0D));
        assertFalse(EliteClimbingRules.withinBuildRange(16.01D, 0.0D));
    }

    @Test
    void climbsOnlyWhileInsideAClimbableBlockWithAHigherTarget() {
        assertTrue(EliteClimbingRules.shouldClimb(true, true, true, true, true));
        assertFalse(EliteClimbingRules.shouldClimb(true, true, true, false, true));
        assertFalse(EliteClimbingRules.shouldClimb(true, true, true, true, false));
        assertFalse(EliteClimbingRules.shouldClimb(false, true, true, true, true));
    }

    @Test
    void pursuesThePathOnlyWithAnIncompletePathAndAHigherTarget() {
        assertTrue(EliteClimbingRules.shouldPursuePath(true, true, true, true, true));
        assertFalse(EliteClimbingRules.shouldPursuePath(true, true, true, false, true));
        assertFalse(EliteClimbingRules.shouldPursuePath(true, true, true, true, false));
        assertFalse(EliteClimbingRules.shouldPursuePath(false, true, true, true, true));
    }

    @Test
    void placesBlocksOnlyForAGrieffingApprovedPursuerOnAValidSpot() {
        assertTrue(EliteClimbingRules.canPlaceBlock(true, true, true, true, true));
        assertFalse(EliteClimbingRules.canPlaceBlock(false, true, true, true, true));
        assertFalse(EliteClimbingRules.canPlaceBlock(true, false, true, true, true));
        assertFalse(EliteClimbingRules.canPlaceBlock(true, true, false, true, true));
        assertFalse(EliteClimbingRules.canPlaceBlock(true, true, true, false, true));
        assertFalse(EliteClimbingRules.canPlaceBlock(true, true, true, true, false));
    }

    @Test
    void allowsAtMostOnePlacementEveryTenTicks() {
        assertFalse(EliteClimbingRules.cooldownReady(9L, 10L));
        assertTrue(EliteClimbingRules.cooldownReady(10L, 10L));
    }
}
```

运行：
```bash
./gradlew.bat test --tests com.oriimprovedmobs.elite.EliteClimbingRulesTest
```
**预期 RED**：编译失败，`找不到符号: 类 EliteClimbingRules`。

提交：
```bash
git add src/test/java/com/oriimprovedmobs/elite/EliteClimbingRulesTest.java
git commit -m "test: define pathfinder building and climbing rules"
```

#### 4b. 最小实现（commit `feat:`）

**新建 `src/main/java/com/oriimprovedmobs/elite/EliteClimbingRules.java`**：
```java
package com.oriimprovedmobs.elite;

public final class EliteClimbingRules {
    public static final double CLIMB_SPEED = 0.2D;
    public static final double MIN_HEIGHT_ADVANTAGE = 1.0D;
    public static final double BUILD_RANGE = 16.0D;
    public static final long BUILD_INTERVAL_TICKS = 10L;

    private EliteClimbingRules() {}

    public static boolean shouldInstallGoal(boolean hasPathfinder, boolean alreadyInstalled) {
        return hasPathfinder && !alreadyInstalled;
    }

    public static boolean targetIsHigher(double mobY, double targetY) {
        return targetY - mobY >= MIN_HEIGHT_ADVANTAGE;
    }

    public static boolean withinBuildRange(double dx, double dz) {
        return dx * dx + dz * dz <= BUILD_RANGE * BUILD_RANGE;
    }

    public static boolean shouldClimb(
            boolean hasPathfinder,
            boolean hasPlayerTarget,
            boolean targetAlive,
            boolean onClimbable,
            boolean targetHigher) {
        return hasPathfinder && hasPlayerTarget && targetAlive && onClimbable && targetHigher;
    }

    public static boolean shouldPursuePath(
            boolean hasPathfinder,
            boolean hasPlayerTarget,
            boolean targetAlive,
            boolean pathIncomplete,
            boolean targetHigher) {
        return hasPathfinder && hasPlayerTarget && targetAlive && pathIncomplete && targetHigher;
    }

    public static boolean canPlaceBlock(
            boolean hasPathfinder,
            boolean chasingPlayer,
            boolean mobGriefingAllowed,
            boolean positionReplaceable,
            boolean supportOk) {
        return hasPathfinder && chasingPlayer && mobGriefingAllowed && positionReplaceable && supportOk;
    }

    public static boolean cooldownReady(long gameTime, long nextBuildGameTime) {
        return gameTime >= nextBuildGameTime;
    }
}
```

**新建 `src/main/java/com/oriimprovedmobs/events/EliteClimbingGoal.java`**：
```java
package com.oriimprovedmobs.events;

import com.oriimprovedmobs.elite.EliteClimbingRules;
import com.oriimprovedmobs.elite.EliteData;
import com.oriimprovedmobs.elite.EliteType;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.ScaffoldingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;

public final class EliteClimbingGoal extends Goal {
    private static final String NEXT_BUILD_GAME_TIME_KEY = "ori_improved_mobs:pathfinder_next_build_game_time";
    private static final double MOVE_SPEED = 1.0D;

    private final Mob mob;

    public EliteClimbingGoal(Mob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return plan() != null;
    }

    @Override
    public boolean canContinueToUse() {
        return plan() != null;
    }

    @Override
    public void tick() {
        if (!(mob.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Plan plan = plan();
        if (plan == null) {
            return;
        }
        switch (plan.action()) {
            case CLIMB -> climbUp();
            case PLACE_LADDER -> placeLadder(serverLevel, plan.pos());
            case PLACE_SCAFFOLD -> placeScaffolding(serverLevel, plan.pos());
            case APPROACH -> approachTarget();
        }
    }

    private enum Action {
        CLIMB, PLACE_LADDER, PLACE_SCAFFOLD, APPROACH
    }

    private record Plan(Action action, BlockPos pos) {}

    private Plan plan() {
        if (!(mob.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        EnumSet<EliteType> types = EliteData.read(mob);
        boolean hasPathfinder = types.contains(EliteType.PATHFINDER);
        LivingEntity target = mob.getTarget();
        boolean hasPlayerTarget = target instanceof Player;
        boolean targetAlive = target != null && target.isAlive();
        if (!hasPathfinder || !hasPlayerTarget || !targetAlive) {
            return null;
        }

        boolean targetHigher = EliteClimbingRules.targetIsHigher(mob.getY(), target.getY());
        boolean onClimbable = serverLevel.getBlockState(mob.blockPosition()).is(BlockTags.CLIMBABLE);
        if (EliteClimbingRules.shouldClimb(
                hasPathfinder, hasPlayerTarget, targetAlive, onClimbable, targetHigher)) {
            return new Plan(Action.CLIMB, mob.blockPosition());
        }

        Path path = mob.getNavigation().getPath();
        boolean pathIncomplete = path != null && !path.canReach();
        if (!EliteClimbingRules.shouldPursuePath(
                hasPathfinder, hasPlayerTarget, targetAlive, pathIncomplete, targetHigher)) {
            return null;
        }
        if (!EliteClimbingRules.withinBuildRange(target.getX() - mob.getX(), target.getZ() - mob.getZ())) {
            return new Plan(Action.APPROACH, mob.blockPosition());
        }

        boolean mobGriefingAllowed = serverLevel.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        long gameTime = serverLevel.getGameTime();
        long nextBuildGameTime = mob.getPersistentData().getLong(NEXT_BUILD_GAME_TIME_KEY);
        if (!mobGriefingAllowed || !EliteClimbingRules.cooldownReady(gameTime, nextBuildGameTime)) {
            return new Plan(Action.APPROACH, mob.blockPosition());
        }

        boolean chasingPlayer = hasPlayerTarget && targetAlive;
        Direction towardsTarget = Direction.getNearest(
                target.getX() - mob.getX(), 0.0D, target.getZ() - mob.getZ());
        BlockPos ladderPos = findLadderPosition(
                serverLevel, towardsTarget, hasPathfinder, chasingPlayer, mobGriefingAllowed);
        if (ladderPos != null) {
            return new Plan(Action.PLACE_LADDER, ladderPos);
        }
        BlockPos scaffoldPos = findScaffoldingPosition(
                serverLevel, hasPathfinder, chasingPlayer, mobGriefingAllowed);
        if (scaffoldPos != null) {
            return new Plan(Action.PLACE_SCAFFOLD, scaffoldPos);
        }
        return new Plan(Action.APPROACH, mob.blockPosition());
    }

    private void climbUp() {
        LivingEntity target = mob.getTarget();
        mob.getJumpControl().jump();
        mob.setDeltaMovement(0.0D, EliteClimbingRules.CLIMB_SPEED, 0.0D);
        if (target != null) {
            mob.getMoveControl().setWantedPosition(target.getX(), mob.getY(), target.getZ(), MOVE_SPEED);
        }
    }

    private void approachTarget() {
        LivingEntity target = mob.getTarget();
        if (target == null) {
            return;
        }
        mob.getMoveControl().setWantedPosition(target.getX(), mob.getY(), target.getZ(), MOVE_SPEED);
    }

    private BlockPos findLadderPosition(
            ServerLevel level,
            Direction towardsTarget,
            boolean hasPathfinder,
            boolean chasingPlayer,
            boolean mobGriefingAllowed) {
        BlockPos mobPos = mob.blockPosition();
        for (BlockPos candidate : List.of(mobPos.relative(towardsTarget), mobPos, mobPos.above())) {
            if (isLadderPlaceable(
                    level, candidate, towardsTarget, hasPathfinder, chasingPlayer, mobGriefingAllowed)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean isLadderPlaceable(
            ServerLevel level,
            BlockPos ladderPos,
            Direction towardsTarget,
            boolean hasPathfinder,
            boolean chasingPlayer,
            boolean mobGriefingAllowed) {
        BlockState ladderSpace = level.getBlockState(ladderPos);
        boolean positionReplaceable = ladderSpace.canBeReplaced() && level.getFluidState(ladderPos).isEmpty();
        BlockPos supportPos = ladderPos.relative(towardsTarget);
        BlockState support = level.getBlockState(supportPos);
        boolean supportOk = support.isFaceSturdy(level, supportPos, towardsTarget.getOpposite());
        return EliteClimbingRules.canPlaceBlock(
                hasPathfinder, chasingPlayer, mobGriefingAllowed, positionReplaceable, supportOk);
    }

    private BlockPos findScaffoldingPosition(
            ServerLevel level,
            boolean hasPathfinder,
            boolean chasingPlayer,
            boolean mobGriefingAllowed) {
        if (!mob.onGround()) {
            return null;
        }
        BlockPos scaffoldPos = mob.blockPosition();
        BlockState scaffoldSpace = level.getBlockState(scaffoldPos);
        boolean positionReplaceable = scaffoldSpace.canBeReplaced() && level.getFluidState(scaffoldPos).isEmpty();
        BlockPos supportPos = scaffoldPos.below();
        BlockState support = level.getBlockState(supportPos);
        boolean supportOk = support.isFaceSturdy(level, supportPos, Direction.UP)
                || support.is(Blocks.SCAFFOLDING);
        boolean columnSupported =
                ScaffoldingBlock.getDistance(level, scaffoldPos) < ScaffoldingBlock.STABILITY_MAX_DISTANCE;
        if (EliteClimbingRules.canPlaceBlock(
                hasPathfinder, chasingPlayer, mobGriefingAllowed, positionReplaceable, supportOk)
                && columnSupported) {
            return scaffoldPos;
        }
        return null;
    }

    private void placeLadder(ServerLevel level, BlockPos ladderPos) {
        LivingEntity target = mob.getTarget();
        if (target == null) {
            return;
        }
        Direction towardsTarget = Direction.getNearest(
                target.getX() - mob.getX(), 0.0D, target.getZ() - mob.getZ());
        Direction ladderFacing = towardsTarget.getOpposite();
        BlockState ladderState = Blocks.LADDER.defaultBlockState()
                .setValue(LadderBlock.FACING, ladderFacing)
                .setValue(LadderBlock.WATERLOGGED, false);
        placeBlock(level, ladderPos, ladderState, ladderFacing);
    }

    private void placeScaffolding(ServerLevel level, BlockPos scaffoldPos) {
        int distance = ScaffoldingBlock.getDistance(level, scaffoldPos);
        BlockState support = level.getBlockState(scaffoldPos.below());
        BlockState scaffoldState = Blocks.SCAFFOLDING.defaultBlockState()
                .setValue(ScaffoldingBlock.DISTANCE, distance)
                .setValue(ScaffoldingBlock.WATERLOGGED, false)
                .setValue(ScaffoldingBlock.BOTTOM, distance > 0 && !support.is(Blocks.SCAFFOLDING));
        placeBlock(level, scaffoldPos, scaffoldState, Direction.UP);
    }

    private void placeBlock(ServerLevel level, BlockPos pos, BlockState state, Direction placedAgainst) {
        BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, pos);
        if (ForgeEventFactory.onBlockPlace(mob, snapshot, placedAgainst)) {
            return;
        }
        level.setBlockAndUpdate(pos, state);
        mob.getPersistentData().putLong(
                NEXT_BUILD_GAME_TIME_KEY,
                level.getGameTime() + EliteClimbingRules.BUILD_INTERVAL_TICKS);
    }
}
```

**`src/main/java/com/oriimprovedmobs/events/EliteJoinHandler.java`** — 在破障者 Goal 安装块之后追加（并补 import `com.oriimprovedmobs.elite.EliteClimbingRules`）：
```java
        boolean climbingGoalInstalled = mob.goalSelector.getAvailableGoals().stream()
                .anyMatch(wrappedGoal -> wrappedGoal.getGoal() instanceof EliteClimbingGoal);
        if (EliteClimbingRules.shouldInstallGoal(
                types.contains(EliteType.PATHFINDER), climbingGoalInstalled)) {
            mob.goalSelector.addGoal(2, new EliteClimbingGoal(mob));
        }
```

运行：
```bash
./gradlew.bat test
```
**预期 GREEN**：`BUILD SUCCESSFUL`，测试全绿。

提交：
```bash
git add src/main/java/com/oriimprovedmobs/elite/EliteClimbingRules.java \
        src/main/java/com/oriimprovedmobs/events/EliteClimbingGoal.java \
        src/main/java/com/oriimprovedmobs/events/EliteJoinHandler.java
git commit -m "feat: let pathfinders build and climb towards players"
```

### Task 5 — 语言文件与文档（commit `docs:`）

**`src/main/resources/assets/ori_improved_mobs/lang/zh_cn.json`** — 全量替换为：
```json
{
  "entity.ori_improved_mobs.elite_name": "%s·%s",
  "entity.ori_improved_mobs.elite.night_stalker": "夜行者",
  "entity.ori_improved_mobs.elite.frostborn": "霜裔",
  "entity.ori_improved_mobs.elite.infernal": "狱火",
  "entity.ori_improved_mobs.elite.breacher": "破障者",
  "entity.ori_improved_mobs.elite.shrouded": "隐身",
  "entity.ori_improved_mobs.elite.vampiric": "吸血",
  "entity.ori_improved_mobs.elite.pathfinder": "开拓者"
}
```

**`src/main/resources/assets/ori_improved_mobs/lang/en_us.json`** — 全量替换为：
```json
{
  "entity.ori_improved_mobs.elite_name": "%s %s",
  "entity.ori_improved_mobs.elite.night_stalker": "Night Stalker",
  "entity.ori_improved_mobs.elite.frostborn": "Frostborn",
  "entity.ori_improved_mobs.elite.infernal": "Infernal",
  "entity.ori_improved_mobs.elite.breacher": "Breacher",
  "entity.ori_improved_mobs.elite.shrouded": "Shrouded",
  "entity.ori_improved_mobs.elite.vampiric": "Vampiric",
  "entity.ori_improved_mobs.elite.pathfinder": "Pathfinder"
}
```

**`README.md`** — 词缀表追加三行（在破障者行后）：

```markdown
| 隐身 | 主世界夜晚 | 生命值低于 50% 前完全隐身：隐身效果（无粒子）、无发光轮廓、无名称；低于 50% 后完全现形 |
| 吸血 | 所有维度 | 造成伤害时按实际伤害的 50% 回复自身生命（近战与远程均触发） |
| 开拓者 | 所有维度 | 追击玩家且路径不通时：贴墙建造梯子、开阔地建造脚手架柱，并主动沿可攀爬方块向上爬；每 10 ticks 最多放置 1 格，遵守 `mobGriefing` 与 Forge 方块保护事件 |
```

配置示例代码块追加三行：
```toml
shroudedEnabled = true
vampiricEnabled = true
pathfinderEnabled = true
```

并在说明段落后补一段：
```markdown
隐身怪与吸血存在联动：回血到 50% 以上会再次隐身。开拓者放置的梯子与脚手架是原版方块，被玩家破坏时正常掉落。三组新开关互不影响；配置变更只影响之后生成的怪物。
```

**`AGENTS.md`** — 两处更新：
1. 文件树中补上新文件：
   - `elite/` 下加 `EliteConcealmentRules.java      隐身：半血显隐判定`、`EliteClimbingRules.java        开拓者：建房/攀爬判定与常量`
   - `events/` 下加 `EliteClimbingGoal.java        开拓者：放置梯子/脚手架 + 攀爬 Goal`、`EliteConcealmentHandler.java  隐身显隐状态刷新（受击/回血/入世界）`
2. `当前基线` 数字改成实测值（跑完 Task 4 后从 `build/reports/tests/test/index.html` 读取；预期 12 个测试类 / 60 个测试左右）。

运行：
```bash
./gradlew.bat test
```
**预期**：`BUILD SUCCESSFUL`（文档改动不影响测试，仅确认未破坏构建）。

提交：
```bash
git add README.md AGENTS.md src/main/resources/assets/ori_improved_mobs/lang/zh_cn.json \
        src/main/resources/assets/ori_improved_mobs/lang/en_us.json
git commit -m "docs: describe shrouded, vampiric and pathfinder affixes"
```

### Task 6 — 交付门禁与产物检查（无提交）

```bash
./gradlew.bat clean test build
```
**预期**：`BUILD SUCCESSFUL`；产物 `build/libs/ori_improved_mobs-0.1.0.jar` 重新生成。

```bash
unzip -l build/libs/ori_improved_mobs-0.1.0.jar | grep -E "OriImprovedMobs|mods.toml|EliteClimbingGoal|EliteConcealmentHandler"
unzip -p build/libs/ori_improved_mobs-0.1.0.jar META-INF/mods.toml | head -20
```
**预期**：能看到入口类、4 个新事件/规则类以及 `mods.toml`；`mods.toml` 中 modId `ori_improved_mobs`、版本 `0.1.0`、无未展开的模板占位符。

**交付说明必须包含**：改了什么、测试全绿的确切数字、`clean test build` 结果、以及一句明确的「未做运行时验证（用户选择）——开拓者的攀爬行为、隐身的发光/隐身视觉、保护类模组兼容性均未在真实游戏中观察」。

## Tests / validation

**能单测覆盖的（本计划全部覆盖）**：
- 解析器环境门槛：隐身=主世界夜晚；吸血、开拓者=全维度（`EliteTypeResolverTest`）。
- 配置默认值 / 7 个独立开关 / `enabledTypes` 独立性（`EliteConfigDefaultsTest`）。
- 生成资格与边界（roll == chance 不算精英）（`EliteSpawnPolicyTest`，仅改期望值）。
- 显隐边界：HP == 50% 仍隐身、9.9/20 现身（`EliteConcealmentRulesTest`）。
- 吸血：50% 比例、0 伤害/无词缀不触发（`EliteCombatRulesTest`）。
- 开拓者：安装条件、高度差 1 格边界、16 格距离边界、攀爬/追击/放置布尔组合、10 ticks 冷却边界（`EliteClimbingRulesTest`）。
- NBT/展示：新 id 稳定、标签键（`EliteTypeCodecTest`、`ElitePresentationTest`）。

**不能单测（本计划如实标注为未验证）**：Forge 事件接线行为（隐身受击显形时机的真实顺序、吸血 `LivingDamageEvent` 数值语义、Goal 在真实世界中的放置与攀爬、`EntityPlaceEvent` 与保护模组的交互）。这符合 `AGENTS.md` 的"编译通过 ≠ 运行时正确"。

**最终验证命令**：`./gradlew.bat clean test build`（预期 `BUILD SUCCESSFUL`，测试报告 `build/reports/tests/test/index.html`）。

### 附录：可选手动验证清单（不属于本轮验收；将来想跑 runClient 时用）

1. 创建新世界（创造模式），`/time set midnight`；用刷怪蛋随便放怪只是热身——精英只会从**自然生成**来，建议开 `/gamerule doMobSpawning true` 并在夜里挂机观察发光怪。
2. 把世界目录里的 `serverconfig/ori_improved_mobs-server.toml` 的 `spawnChance` 临时调成 `1.0`，让每只自然怪都是精英，方便观察。
3. 隐身：夜里找发光精英，`/data get entity @e[type=zombie,limit=1,sort=nearest]` 看 `ori_improved_mobs:elite_types`；打它到半血以下，确认隐身效果消失、发光轮廓与名字出现。
4. 吸血：打一只吸血精英（僵尸），观察它挨打后是否回血（可用多次轻击观察血量）。
5. 开拓者：造 6 格高柱站上去，让开拓者精英在柱下；观察它贴柱盖脚手架并往上爬（若打转/爬不上去，见"风险"第 1、2 条）。
6. 破障者回归：确认原有拆方块行为未被破坏。

## Risks, tradeoffs, and open questions

1. **【最高风险】攀爬推力未经真实游戏确认**：原版爬梯依赖"移动输入压向梯子"，本实现给了双保险（`JumpControl.jump()` + 每 tick `setDeltaMovement(0, 0.2, 0)` + `MoveControl` 朝目标压），但仍可能需要运行时微调 `CLIMB_SPEED`、放置节奏或触发条件。用户已选择不做运行时验证——接受此风险。
2. **脚手架 + 怪物寻路是原版已知雷区**（MC-150293 等：怪在脚手架上会打转）。我们的用法是"柱内攀爬/站在柱顶"，不是"在脚手架顶上寻路"，但仍未实测；若实际表现为原地打转，退路 = 只保留梯子分支（删掉脚手架分支是局部改动）。
3. **玩家可"白嫖"建材**：怪放的梯子/脚手架被玩家拆掉会正常掉落（原版行为）。接受并写入 README；如要禁止需要额外的方块归属跟踪（本轮 YAGNI）。
4. **保护类模组**：放置走 `ForgeEventFactory.onBlockPlace`（`EntityPlaceEvent`，返回 true = 被取消则跳过），破坏类保护不受影响；`mobGriefing=false` 时完全不放置。均与破障者行为对称。
5. **性能**：`EliteClimbingGoal` 与其他 AI Goal 不同，每 tick 会查询若干方块状态（与破障者同级），只在有玩家目标时生效；隐身刷新只在 入世界/受击/回血 时计算，无每 tick 开销。
6. **吸血数值**：50% 实际伤害无上限，多个吸血鬼刷在一起会互相"奶"（回血会再触发回血事件，但我们的规则不放大，只刷新显隐）。若实际过强，调 `EliteCombatRules.LIFESTEAL_RATIO` 一个常量即可。
7. **数值均不可配置**（50%/0.5/10 ticks/1 格/16 格）：与 `MAX_HARDNESS` 先例一致；如果之后想要配置，加 3 个 `ForgeConfigSpec` 项即可（届时同步 README 与 `EliteConfigDefaultsTest`）。
8. **旧世界兼容**：新配置键会在下次加载时自动补默认值（Forge 行为，已有先例验证）；老怪缺 `ori_improved_mobs:elite_types` 不受影响。
9. **开放问题**：① `mod_version` 是否升到 `0.2.0`（7 个词缀是一个里程碑）？② 三个新 id/中英名（`shrouded`/`vampiric`/`pathfinder`、隐身/吸血/开拓者）是否照此定稿？③ 将来是否要为自己的运行验证补一个 `runClient` 冒烟（本计划仅提供可选清单）。
