# 怪物增强模组：实施计划

## Goal
在当前 Minecraft 1.20.1 / Forge 47.4.23 模组骨架中，实现一套可扩展的服务器权威精英怪系统：自然生成的原版敌对生物有 5% 概率在符合环境时获得一个或多个精英词缀、战斗能力和可识别外观。

## Current context / assumptions

- 当前仓库是 Forge MDK 起始骨架：Minecraft `1.20.1`、Forge `47.4.23`、Java 17、Gradle Wrapper 8.8；mod id 是 `ori_improved_mobs`。入口是 `src/main/java/com/oriimprovedmobs/OriImprovedMobs.java`，尚无怪物逻辑。参见 `README.md:5-17,32-35`、`gradle.properties:10-16,45-55` 和入口类。
- 当前没有 `src/test` 测试、没有 `AGENTS.md` / `CLAUDE.md` / `.cursorrules`，也没有 `.git`。用户已授权把 `git init` 和基线提交列入实施前任务；`.gitignore` 已忽略 `build/`、`.gradle/`、`.idea/` 和 `run/`，`run-data/` 当前为空。
- 当前 Gradle 全局 Java 可能是 25；必须照 `README.md:12-17` 用已安装的 JDK 17 启动 Gradle 8.8。计划命令按 Windows PowerShell 编写。
- 已确认的玩法决策：只处理自然生成的原版敌对生物；排除 Boss；不增强命令、刷怪笼或刷怪蛋生成的怪；主世界夜晚为夜行者、Forge 雪地群系标签为霜裔、下界为狱火；主世界雪地夜晚允许两个词缀同时出现；每次 eligible spawn 只掷一次全局概率，默认 5%；全局启用、概率和三个词缀各自启用均由服务器配置控制；不加专属掉落；用名称标记和原版发光轮廓识别；支持单人及专用服务器，逻辑由服务器执行。
- 本计划为三种词缀提出首版能力：夜行者获得速度 I；霜裔命中时施加缓慢 I 40 ticks；狱火获得火焰抗性并在命中时点燃目标 2 秒。只使用原版效果，不加模型、音效、物品、实体或运行时模组依赖。双词缀同时生效。
- “可复用框架”按内部代码可扩展理解：使用类型枚举、环境解析、持久化和行为分层；首版不承诺对外 Java API 或 datapack 注册接口（YAGNI）。新增 JUnit 仅为 test-only 开发依赖，不打进模组运行时依赖。
- Forge 1.20.1 的 `MobSpawnEvent.FinalizeSpawn` 在 `Mob#finalizeSpawn` 前、只在逻辑服务端触发；Forge 源码描述的生成事件流在完成该事件后才触发 `EntityJoinLevelEvent`。因此 spawn 事件只负责判定并写词缀数据，不取消或改写原版生成初始化；实体入世界后再施加效果和外观。citeturn2view1turn2view2
- 使用 Forge `Tags.Biomes.IS_SNOWY` 做雪地判定，避免手工枚举生物群系；Boss 排除使用 Forge `Tags.EntityTypes.BOSSES`，并在验收中确认凋灵、末影龙不会被增强。citeturn2view4turn2view5

## Architecture / proposed approach
在纯 Java 规则层中解析环境、资格、开关和单次概率掷骰；Forge 事件层只读取世界/实体事实并调用规则层。词缀 ID 存入实体 Forge persistent NBT，加载实体时幂等地恢复名称、发光和原版状态效果，受击行为由服务端伤害事件处理。用 JUnit 5 覆盖确定性规则，再以开发客户端和专用服务器验证实际 Forge 事件接线；服务器配置用 `ForgeConfigSpec.Type.SERVER`，它会同步到客户端。citeturn2view6turn2view7

## Step-by-step tasks

> 每个代码任务严格按 RED → GREEN → COMMIT：先加测试，确认失败；最小实现后复跑同一命令；绿灯后提交。下列 PowerShell 命令都从项目根目录执行。

### 0. 初始化 Git 并提交干净基线（约 3 分钟；项目准备，不是功能代码）

**文件范围：**仅使用现有 `.gitignore`，不改模组代码。当前目录不是 Git 仓库，这是用户要求的提交工作流前置条件。

**执行：**
```powershell
git init
git add -A
git status --short
git commit -m "chore: baseline Forge mod scaffold"
git status --short --branch
```

**验证：**`git init` 报告创建仓库；提交前 `git status --short` 显示现有项目文件（不会包含 `.gradle/`、`build/`、`.idea/`、`run/`）；提交后 `git status --short --branch` 只有当前分支标题且工作区干净。若 Git 未配置 `user.name` / `user.email`，停止并请用户设置真实身份，不要编造身份或用占位值提交。

### 1. 接通 JUnit 5 测试任务（约 4 分钟；测试基础设施）

**测试先行：**新建 `src/test/java/com/oriimprovedmobs/BuildEnvironmentTest.java`：
```java
package com.oriimprovedmobs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BuildEnvironmentTest {
    @Test
    void testRuntimeUsesJava17() {
        assertEquals(17, Runtime.version().feature());
    }
}
```
先执行：
```powershell
.\gradlew.bat test --tests com.oriimprovedmobs.BuildEnvironmentTest
```
预期 RED：`compileTestJava` 因找不到 `org.junit.jupiter` 而失败。

**最小实现：**在 `build.gradle` 现有 `dependencies {}` 中追加：
```groovy
testImplementation 'org.junit.jupiter:junit-jupiter:5.10.2'
testRuntimeOnly 'org.junit.platform:junit-platform-launcher:1.10.2'
```
在 `build.gradle` 末尾追加：
```groovy
tasks.named('test') {
    useJUnitPlatform()
}
```
ForgeGradle 已自动提供 Maven Central，无需再加仓库声明（见 `build.gradle:114-117`）。

**验证并提交：**再次运行同一命令；预期 `BUILD SUCCESSFUL`，报告中 `BuildEnvironmentTest` 通过。然后：
```powershell
git add build.gradle src/test/java/com/oriimprovedmobs/BuildEnvironmentTest.java
git commit -m "test: configure JUnit 5"
```

### 2. 实现环境上下文和词缀解析器（约 5 分钟）

**测试先行：**新建 `src/test/java/com/oriimprovedmobs/elite/EliteTypeResolverTest.java`，先运行命令确认缺失实现/空实现导致 RED：
```powershell
.\gradlew.bat test --tests com.oriimprovedmobs.elite.EliteTypeResolverTest
```
测试必须覆盖夜晚主世界只匹配夜行者、雪地匹配霜裔、下界匹配狱火、雪地夜晚返回夜行者与霜裔两者、关闭单个词缀只移除该词缀、主世界白天普通群系不匹配。

测试核心代码：
```java
package com.oriimprovedmobs.elite;

import java.util.EnumSet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EliteTypeResolverTest {
    private static EnumSet<EliteType> all() {
        return EnumSet.allOf(EliteType.class);
    }

    @Test
    void resolvesNightStalkerOnlyAtOverworldNight() {
        var context = new EliteSpawnContext(true, false, false, true);
        assertEquals(EnumSet.of(EliteType.NIGHT_STALKER), EliteTypeResolver.resolve(context, all()));
    }

    @Test
    void resolvesFrostbornInSnowyBiome() {
        var context = new EliteSpawnContext(true, false, true, false);
        assertEquals(EnumSet.of(EliteType.FROSTBORN), EliteTypeResolver.resolve(context, all()));
    }

    @Test
    void resolvesInfernalInNether() {
        var context = new EliteSpawnContext(false, true, false, false);
        assertEquals(EnumSet.of(EliteType.INFERNAL), EliteTypeResolver.resolve(context, all()));
    }

    @Test
    void allowsNightAndSnowTraitsToStack() {
        var context = new EliteSpawnContext(true, false, true, true);
        assertEquals(EnumSet.of(EliteType.NIGHT_STALKER, EliteType.FROSTBORN),
                EliteTypeResolver.resolve(context, all()));
    }

    @Test
    void respectsPerTypeEnableFlags() {
        var context = new EliteSpawnContext(true, false, true, true);
        assertEquals(EnumSet.of(EliteType.FROSTBORN),
                EliteTypeResolver.resolve(context, EnumSet.of(EliteType.FROSTBORN)));
    }

    @Test
    void returnsEmptyWhenNoEnvironmentMatches() {
        var context = new EliteSpawnContext(true, false, false, false);
        assertEquals(EnumSet.noneOf(EliteType.class), EliteTypeResolver.resolve(context, all()));
    }
}
```

**最小实现：**新增以下文件。

`src/main/java/com/oriimprovedmobs/elite/EliteType.java`：
```java
package com.oriimprovedmobs.elite;

import java.util.Arrays;
import java.util.Optional;

public enum EliteType {
    NIGHT_STALKER("night_stalker"),
    FROSTBORN("frostborn"),
    INFERNAL("infernal");

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

`src/main/java/com/oriimprovedmobs/elite/EliteSpawnContext.java`：
```java
package com.oriimprovedmobs.elite;

public record EliteSpawnContext(boolean overworld, boolean nether, boolean snowyBiome, boolean night) {
    public static EliteSpawnContext from(boolean overworld, boolean nether, boolean snowyBiome, long dayTime) {
        long timeOfDay = Math.floorMod(dayTime, 24_000L);
        boolean night = timeOfDay >= 13_000L && timeOfDay < 23_000L;
        return new EliteSpawnContext(overworld, nether, snowyBiome, night);
    }
}
```

`src/main/java/com/oriimprovedmobs/elite/EliteTypeResolver.java`：
```java
package com.oriimprovedmobs.elite;

import java.util.EnumSet;
import java.util.Set;

public final class EliteTypeResolver {
    private EliteTypeResolver() {}

    public static EnumSet<EliteType> resolve(EliteSpawnContext context, Set<EliteType> enabled) {
        EnumSet<EliteType> result = EnumSet.noneOf(EliteType.class);
        if (context.overworld() && context.night() && enabled.contains(EliteType.NIGHT_STALKER)) {
            result.add(EliteType.NIGHT_STALKER);
        }
        if (context.snowyBiome() && enabled.contains(EliteType.FROSTBORN)) {
            result.add(EliteType.FROSTBORN);
        }
        if (context.nether() && enabled.contains(EliteType.INFERNAL)) {
            result.add(EliteType.INFERNAL);
        }
        return result;
    }
}
```

**验证并提交：**
```powershell
.\gradlew.bat test --tests com.oriimprovedmobs.elite.EliteTypeResolverTest
git add src/main/java/com/oriimprovedmobs/elite/EliteType.java src/main/java/com/oriimprovedmobs/elite/EliteSpawnContext.java src/main/java/com/oriimprovedmobs/elite/EliteTypeResolver.java src/test/java/com/oriimprovedmobs/elite/EliteTypeResolverTest.java
git commit -m "feat: add elite environment resolver"
```
RED 时测试不能通过；GREEN 时 6 项测试通过、`BUILD SUCCESSFUL`。另在同一测试类加边界测试：`EliteSpawnContext.from(..., 12_999)` 和 `(..., 23_000)` 是白天；`13_000`、`22_999` 是夜晚；重新运行测试后再提交。

### 3. 实现自然生成资格与单次概率策略（约 5 分钟）

**测试先行：**新建 `src/test/java/com/oriimprovedmobs/elite/EliteSpawnPolicyTest.java`。先以空实现运行，确认正向用例失败：
```powershell
.\gradlew.bat test --tests com.oriimprovedmobs.elite.EliteSpawnPolicyTest
```
要求测试涵盖：自然生成的非 Boss `MONSTER` 可通过；命令/刷怪笼/刷怪蛋、非 `MONSTER`、Boss、已有词缀、全局关闭均拒绝；没有环境匹配项时不调用随机源；`roll < 0.05` 成功而 `roll == 0.05` 失败；一次成功结果返回所有匹配且已启用的词缀。

**生产接口：**新增 `src/main/java/com/oriimprovedmobs/elite/EliteSpawnPolicy.java`。使用 `DoubleSupplier` 让不合格实体不消耗随机数：
```java
package com.oriimprovedmobs.elite;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.DoubleSupplier;

public final class EliteSpawnPolicy {
    private EliteSpawnPolicy() {}

    public static EnumSet<EliteType> select(
            boolean naturalSpawn,
            boolean monsterCategory,
            boolean boss,
            boolean alreadyElite,
            boolean globallyEnabled,
            EliteSpawnContext context,
            Set<EliteType> enabledTypes,
            DoubleSupplier roll,
            double chance) {
        if (!naturalSpawn || !monsterCategory || boss || alreadyElite || !globallyEnabled) {
            return EnumSet.noneOf(EliteType.class);
        }
        EnumSet<EliteType> candidates = EliteTypeResolver.resolve(context, enabledTypes);
        if (candidates.isEmpty() || chance <= 0.0D) {
            return EnumSet.noneOf(EliteType.class);
        }
        if (roll.getAsDouble() >= chance) {
            return EnumSet.noneOf(EliteType.class);
        }
        return candidates;
    }
}
```

**验证并提交：**
```powershell
.\gradlew.bat test --tests com.oriimprovedmobs.elite.EliteSpawnPolicyTest
git add src/main/java/com/oriimprovedmobs/elite/EliteSpawnPolicy.java src/test/java/com/oriimprovedmobs/elite/EliteSpawnPolicyTest.java
git commit -m "feat: add elite spawn policy"
```
RED 应显示断言失败；GREEN 应显示测试类全绿、`BUILD SUCCESSFUL`。

### 4. 添加服务器配置并注册（约 5 分钟）

**测试先行：**新建 `src/test/java/com/oriimprovedmobs/config/EliteConfigDefaultsTest.java`，先运行测试确认缺少默认值类时失败：
```powershell
.\gradlew.bat test --tests com.oriimprovedmobs.config.EliteConfigDefaultsTest
```

测试代码：
```java
package com.oriimprovedmobs.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EliteConfigDefaultsTest {
    @Test
    void defaultsAreEnabledAndRare() {
        assertTrue(EliteConfigDefaults.ENABLED);
        assertEquals(0.05D, EliteConfigDefaults.SPAWN_CHANCE, 0.0D);
        assertTrue(EliteConfigDefaults.NIGHT_STALKER_ENABLED);
        assertTrue(EliteConfigDefaults.FROSTBORN_ENABLED);
        assertTrue(EliteConfigDefaults.INFERNAL_ENABLED);
    }
}
```

**最小实现：**新增 `src/main/java/com/oriimprovedmobs/config/EliteConfigDefaults.java`，定义测试中的五个 `public static final` 常量；再新增 `src/main/java/com/oriimprovedmobs/config/EliteConfig.java`，用 `ForgeConfigSpec.Builder` 建立 `[elites]` 项：`enabled=true`、`spawnChance=0.05`（范围 `[0.0,1.0]`）、`nightStalkerEnabled=true`、`frostbornEnabled=true`、`infernalEnabled=true`。`enabledTypes()` 每次返回新 `EnumSet`，不要暴露可变静态集合。

在 `src/main/java/com/oriimprovedmobs/OriImprovedMobs.java` 构造函数中注册：
```java
ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, EliteConfig.SPEC);
```
并添加 `net.minecraftforge.fml.ModLoadingContext`、`net.minecraftforge.fml.config.ModConfig`、`com.oriimprovedmobs.config.EliteConfig` imports。

**验证并提交：**
```powershell
.\gradlew.bat test --tests com.oriimprovedmobs.config.EliteConfigDefaultsTest
.\gradlew.bat build
git add src/main/java/com/oriimprovedmobs/config src/main/java/com/oriimprovedmobs/OriImprovedMobs.java src/test/java/com/oriimprovedmobs/config/EliteConfigDefaultsTest.java
git commit -m "feat: add server-side elite config"
```
预期默认值测试全绿，`build` 为 `BUILD SUCCESSFUL`。Forge 要求在 mod 构造函数注册配置；`SERVER` 配置同步给客户端，服务端文件位于世界的 `serverconfig` 目录。citeturn2view7

### 5. 持久化多个词缀 ID（约 5 分钟）

**测试先行：**新增 `src/test/java/com/oriimprovedmobs/elite/EliteTypeCodecTest.java`，用空实现确认 RED：
```powershell
.\gradlew.bat test --tests com.oriimprovedmobs.elite.EliteTypeCodecTest
```
测试需覆盖空集合、多个类型往返、未知 ID 被忽略。测试主体：
```java
package com.oriimprovedmobs.elite;

import java.util.EnumSet;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EliteTypeCodecTest {
    @Test
    void roundTripsMultipleTypes() {
        var expected = EnumSet.of(EliteType.NIGHT_STALKER, EliteType.FROSTBORN);
        assertEquals(expected, EliteTypeCodec.read(EliteTypeCodec.write(expected)));
    }

    @Test
    void ignoresUnknownIds() {
        var tag = new ListTag();
        tag.add(StringTag.valueOf("removed_type"));
        tag.add(StringTag.valueOf("infernal"));
        assertEquals(EnumSet.of(EliteType.INFERNAL), EliteTypeCodec.read(tag));
    }
}
```

**最小实现：**新增 `src/main/java/com/oriimprovedmobs/elite/EliteTypeCodec.java`：`write(Set<EliteType>)` 写入按 enum 顺序排列的 `ListTag` 字符串 ID；`read(ListTag)` 逐项调用 `EliteType.fromId`，忽略未知值。新增 `src/main/java/com/oriimprovedmobs/elite/EliteData.java`，把列表存到 `mob.getPersistentData()` 的固定键 `ori_improved_mobs:elite_types`；提供 `read(Mob)`、`write(Mob, Set<EliteType>)`、`isElite(Mob)`，写空集合时移除 NBT 键。不要把随机数、能力数值或自定义名塞进类型 ID。

**验证并提交：**
```powershell
.\gradlew.bat test --tests com.oriimprovedmobs.elite.EliteTypeCodecTest
git add src/main/java/com/oriimprovedmobs/elite/EliteTypeCodec.java src/main/java/com/oriimprovedmobs/elite/EliteData.java src/test/java/com/oriimprovedmobs/elite/EliteTypeCodecTest.java
git commit -m "feat: persist elite type ids"
```
预期 codec 测试全绿、`BUILD SUCCESSFUL`。

### 6. 接上 Forge 生成事件与实体外观（约 5 分钟）

**测试先行：**新增 `src/test/java/com/oriimprovedmobs/elite/ElitePresentationTest.java`，先用空实现确认 RED。测试 `labelKeys(EnumSet)` 对双词缀按 `EliteType` 固定顺序返回两个 translation key，空集合返回空列表。命令：
```powershell
.\gradlew.bat test --tests com.oriimprovedmobs.elite.ElitePresentationTest
```

**最小实现：**新增 `src/main/java/com/oriimprovedmobs/elite/ElitePresentation.java`，集中提供 `labelKeys` 和名称构造。新增 `src/main/java/com/oriimprovedmobs/events/EliteSpawnHandler.java`，订阅 `MobSpawnEvent.FinalizeSpawn`，执行以下固定顺序：

1. 要求 `event.getSpawnType() == MobSpawnType.NATURAL` 且实体类别为 `MobCategory.MONSTER`；用 `mob.getType().is(Tags.EntityTypes.BOSSES)` 排除 Boss；对已含 `EliteData` 的实体立即返回。
2. 用 `mob.level().dimension()` 识别主世界/下界；用 `mob.level().getBiome(mob.blockPosition()).is(Tags.Biomes.IS_SNOWY)` 识别雪地；把 `mob.level().getDayTime()` 交给 `EliteSpawnContext.from(...)` 计算夜晚。
3. 调用已测试的 `EliteSpawnPolicy.select(...)`，传入服务器配置和由实体 UUID 派生的独立 `java.util.Random`。不要消耗 `ServerLevel` 或实体正在使用的随机源，以免改变原版后续生成随机行为。
4. 若返回集合非空，只调用 `EliteData.write(mob, selected)`；不要取消事件、改写 `SpawnGroupData` 或直接在 `finalizeSpawn` 之前安装 AI。

新增 `src/main/java/com/oriimprovedmobs/events/EliteJoinHandler.java`，订阅 `EntityJoinLevelEvent`：只在服务端处理 `Mob`；对带词缀的实体设置 `setGlowingTag(true)`，用 `Component.translatable` 设置由词缀标签和原实体显示名组成的名称，`setCustomNameVisible(false)` 以避免全场常驻名字；用持久化布尔标记防止加载时重复添加前缀。夜行者添加速度 I，狱火添加火焰抗性，均使用长时长 vanilla `MobEffectInstance` 且在已存在时不重复添加。配置关闭只影响新生成实体，不移除已存活精英的词缀。

**验证并提交：**
```powershell
.\gradlew.bat test
git add src/main/java/com/oriimprovedmobs/events/EliteSpawnHandler.java src/main/java/com/oriimprovedmobs/events/EliteJoinHandler.java src/main/java/com/oriimprovedmobs/elite/ElitePresentation.java src/test/java/com/oriimprovedmobs/elite/ElitePresentationTest.java
git commit -m "feat: apply elite spawn traits and visuals"
```
RED/GREEN 由 `ElitePresentationTest` 覆盖顺序/堆叠标签；事件桥接不能由无世界单元测试完整模拟，最终客户端和专用服务器 smoke test 是该任务的集成验证。预期测试全绿、`BUILD SUCCESSFUL`。Forge 明确规定 `FinalizeSpawn` 位于原版 `Mob#finalizeSpawn` 之前且只在逻辑服务端触发；生成事件流随后才触发 `EntityJoinLevelEvent`，所以效果/外观放到实体入世界事件应用。citeturn2view1turn2view2

### 7. 添加受击能力（约 5 分钟）

**测试先行：**新增 `src/test/java/com/oriimprovedmobs/elite/EliteCombatRulesTest.java`。先让 `EliteCombatRules` 返回空效果并确认测试失败；测试必须验证：无词缀不触发、霜裔只施加缓慢、狱火只点燃、两类词缀可同时触发。

**可测试规则的完整实现：**新增 `src/main/java/com/oriimprovedmobs/elite/EliteHitEffects.java`：
```java
package com.oriimprovedmobs.elite;

public record EliteHitEffects(boolean slowTarget, boolean igniteTarget) {}
```
新增 `src/main/java/com/oriimprovedmobs/elite/EliteCombatRules.java`：
```java
package com.oriimprovedmobs.elite;

import java.util.Set;

public final class EliteCombatRules {
    private EliteCombatRules() {}

    public static EliteHitEffects onHit(Set<EliteType> types) {
        return new EliteHitEffects(
                types.contains(EliteType.FROSTBORN),
                types.contains(EliteType.INFERNAL));
    }
}
```
新增 `src/main/java/com/oriimprovedmobs/events/EliteCombatHandler.java`，订阅 `LivingHurtEvent`：若伤害来源实体是带词缀的 `Mob`，调用 `EliteCombatRules.onHit(EliteData.read(attacker))`；霜裔对受害者添加 `MobEffects.MOVEMENT_SLOWDOWN`（40 ticks，amplifier 0）；狱火对非火焰免疫受害者调用 `setSecondsOnFire(2)`。空来源、非 Mob 攻击者、空词缀和零伤害都立即返回。此监听器只做服务端逻辑，不添加客户端专用 import。

**验证并提交：**
```powershell
.\gradlew.bat test --tests com.oriimprovedmobs.elite.EliteCombatRulesTest
git add src/main/java/com/oriimprovedmobs/elite/EliteHitEffects.java src/main/java/com/oriimprovedmobs/elite/EliteCombatRules.java src/main/java/com/oriimprovedmobs/events/EliteCombatHandler.java src/test/java/com/oriimprovedmobs/elite/EliteCombatRulesTest.java
git commit -m "feat: add elite hit abilities"
```
RED 时能力断言失败；GREEN 时规则测试全绿、`BUILD SUCCESSFUL`。

### 8. 添加中英文名称并更新 README（约 4 分钟；资源/文档）

在 `src/main/resources/assets/ori_improved_mobs/lang/en_us.json` 新建：
```json
{
  "entity.ori_improved_mobs.elite_name": "%s %s",
  "entity.ori_improved_mobs.elite.night_stalker": "Night Stalker",
  "entity.ori_improved_mobs.elite.frostborn": "Frostborn",
  "entity.ori_improved_mobs.elite.infernal": "Infernal"
}
```
在 `src/main/resources/assets/ori_improved_mobs/lang/zh_cn.json` 新建：
```json
{
  "entity.ori_improved_mobs.elite_name": "%s·%s",
  "entity.ori_improved_mobs.elite.night_stalker": "夜行者",
  "entity.ori_improved_mobs.elite.frostborn": "霜裔",
  "entity.ori_improved_mobs.elite.infernal": "狱火"
}
```
更新 `README.md`：移除“尚未实现具体生物行为”的陈述；写明三个环境条件、5% 单次全局抽样、可叠加规则、排除 Boss/非自然生成、服务器配置键、无额外掉落和测试命令。

**验证并提交：**
```powershell
.\gradlew.bat processResources test
git add README.md src/main/resources/assets/ori_improved_mobs/lang/en_us.json src/main/resources/assets/ori_improved_mobs/lang/zh_cn.json
git commit -m "docs: describe elite mob gameplay"
```
预期资源处理成功、JSON 无解析错误、`BUILD SUCCESSFUL`。Forge 将客户端本地化资源放在 mod 的 `assets` 资源目录下。citeturn1view2

### 9. 全量验证与游戏内验收（约 5 分钟自动验证，另加实际游玩时间）

**环境：**在 PowerShell 使用 `README.md:12-17` 中的 JDK 17 路径：
```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat --version
```
预期显示 Gradle 8.8 和 JVM 17。若该路径不存在，停下并指定机器上真实 JDK 17 路径，不要让 Gradle 8.8 在 JDK 25 下启动。

**自动验证：**
```powershell
.\gradlew.bat clean test build
```
预期所有单元测试通过并显示 `BUILD SUCCESSFUL`。之后检查产物：
```powershell
jar tf build\libs\ori_improved_mobs-0.1.0.jar | findstr /C:"com/oriimprovedmobs/OriImprovedMobs.class" /C:"META-INF/mods.toml" /C:"assets/ori_improved_mobs/lang/en_us.json" /C:"assets/ori_improved_mobs/lang/zh_cn.json"
```
预期列出入口类、`mods.toml` 和两份语言资源。

**开发客户端验收：**执行 ` .\gradlew.bat runClient`，在新测试世界先生成 server config，退出客户端后临时把 `spawnChance` 改为 `1.0` 并重启：
- 主世界夜晚自然生成的普通怪显示夜行者标签/发光并移动更快；雪地夜晚自然生成的怪同时显示夜行者和霜裔；雪地白天只出现霜裔；下界自然生成的怪显示狱火并免疫火焰。
- 精英受击效果符合 40-tick 缓慢 / 2 秒点燃；非火焰免疫目标才会被点燃。
- 用 `/summon minecraft:zombie` 生成的怪不变精英；凋灵和末影龙不变精英；没有额外掉落。
- 恢复 `spawnChance=0.05` 后保存并退出。测试结束关闭客户端；Gradle 任务应以 `BUILD SUCCESSFUL` 结束。

**专用服务器验收：**仅在用户已接受 Minecraft EULA 后执行 ` .\gradlew.bat runServer`；不要自动把 `eula.txt` 改成 `true`。预期服务端正常启动到 `Done (...)! For help, type "help"`，连接一个安装同版本 Forge 与该模组的测试客户端，确认精英名称/发光同步、受击效果由服务端结算且日志没有客户端类加载异常。手动 Ctrl+C 关闭服务端。仓库当前虽然有 `runGameTestServer` 配置，但还没有 GameTest；不要把没有测试类的该任务当作通过验证（`build.gradle:94-98` 注释也说明空 GameTestServer 会失败）。Forge 建议实际验证专用服务器，并说明开发服务端需先由用户接受 EULA。citeturn1view1

## Tests / validation

- 规则层：JUnit 5 对环境解析、夜间边界、资格过滤、概率边界、词缀堆叠、配置默认值、NBT 往返、未知 ID、标签顺序、受击能力组合做 RED/GREEN 测试。
- Forge 接线：`clean test build`、JAR 内容检查、开发客户端自然生成验收、专用服务器启动与客户端连接验收。
- 对每个代码任务，禁止跳过红灯：先跑对应 `--tests <完整测试类名>`，记录预期失败；只实现最小改动，再跑同命令到绿；绿后提交。资源/README 任务用 `processResources test` 验证，不虚报单元测试覆盖资源行为。

## Risks, tradeoffs, and open questions

- **版本控制前置条件：**当前没有 `.git`；实施前会按用户授权初始化仓库并提交基线。若 Git identity 未配置，需先请用户设置。
- **概率语义：**5% 是每只符合条件的自然生成怪物的一次总概率，不是每个词缀单独 5%；雪地夜晚成功时同时得到两个匹配且启用的词缀。
- **事件时机：**`FinalizeSpawn` 在原版 `finalizeSpawn` 前。只写 persistent NBT，不取消事件；效果和显示状态在 `EntityJoinLevelEvent` 应用，防止覆盖原版怪物初始化。citeturn2view1turn2view2
- **资格语义：**使用 `MobCategory.MONSTER` 加 Forge boss tag；未指定的 End 维度没有词缀。雪地资格由 Forge 提供的 `is_snowy` tag 决定；若实际内容与用户理解的“雪原/冰雪群系”不一致，再调整为明确白名单。citeturn2view4turn2view5
- **平衡：**速度 I、缓慢 I 2 秒、点燃 2 秒是首版可玩的起点；配置只调总概率和开关，不开放效果强度。若实际游玩过强，下一轮再增加数值配置。
- **持久化/配置变化：**词缀 ID随实体保存，重载不重新抽签；禁用某词缀只影响之后的新生成实体，现存精英不会在运行中被重写。
- **API 范围：**对外扩展 API、datapack 驱动规则、专属掉落、模型/音效、End 词缀和第三方模组兼容均不在首版范围；若“可复用框架”实际指公开给其他模组作者使用，应在实现前重新设计扩展契约。
- **待游玩确认：**技能数值由实现者先按上面的温和版本实现；若希望首版将夜行者做成爆发/隐身、霜裔做成范围控制、狱火做成远程能力，应在代码开始前调整这一组能力方案。
