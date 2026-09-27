# Ori Improved Mobs — 怪物增强模组

## 环境版本

- Minecraft：1.20.1
- Minecraft Forge：47.4.23
- Java 编译目标：17
- Gradle：使用项目自带 Wrapper，无需单独安装 Gradle

首次构建时，Gradle 会下载 Gradle 发行版及 Forge/Minecraft 依赖。项目配置了 Java 17 toolchain；若本机未安装 JDK 17，Gradle 会尝试自动解析并获取。构建需要访问 Forge Maven、Gradle Plugin Portal 和 Minecraft 依赖仓库。

注意：本机全局 `JAVA_HOME` 当前指向 JDK 25，而 MDK 自带的 Gradle 8.8 在 JDK 25 下无法启动；请使用已安装的 JDK 17（IntelliJ 的 Gradle JVM 也设为 JDK 17）。PowerShell 当前窗口可这样切换：

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

## Windows 命令

在项目根目录打开 PowerShell 或命令提示符：

```powershell
.\gradlew.bat build
.\gradlew.bat runClient
.\gradlew.bat runServer
.\gradlew.bat genIntellijRuns
```

可将项目根目录作为 Gradle 项目导入 IntelliJ IDEA 或 Eclipse。Gradle 会在项目根目录下创建开发运行目录，构建产物位于 `build/libs/`。

- mod id：`ori_improved_mobs`
- 入口类：`src/main/java/com/oriimprovedmobs/OriImprovedMobs.java`

## 精英怪

模组为原版自然生成的敌对生物提供七种可叠加的精英词缀。每只符合条件的怪物只进行一次总概率判定，默认概率为 5%；凋灵、末影龙等 Boss、非自然生成实体和其他模组添加的怪物不参与。雪地夜晚可能同时获得夜行者与霜裔。

| 词缀 | 生成环境 | 能力 |
| --- | --- | --- |
| 夜行者 | 主世界夜晚 | 速度 I；目标生命值低于 50% 时近战伤害 +15% |
| 霜裔 | Forge `is_snowy` 群系标签 | 命中时使目标缓慢 I 40 ticks；被近战命中时对攻击者施加缓慢 I 20 ticks，每只霜裔有独立 100-tick 冷却 |
| 狱火 | 下界 | 火焰抗性；命中非火焰免疫目标时点燃 2 秒；攻击已着火目标时伤害 +15%（含投射物） |
| 破障者 | 所有维度（沿用现有自然生成资格） | 追击玩家且路径受阻时，每 10 ticks 最多立即清除一个硬度 0–3 的挡路方块，不掉落；不破坏方块实体/容器或不可破坏方块，并遵守 `mobGriefing` 与 Forge 方块保护 |
| 隐身 | 主世界夜晚 | 生命值 ≥ 50% 时隐身（无药水粒子），并隐藏发光与名称；低于 50% 完全现形 |
| 吸血 | 所有维度 | 造成实际伤害时回复伤害量 50% 的生命值（近战与投射物均生效，不越过最大生命值） |
| 开拓者 | 所有维度 | 追击玩家且寻路受阻时，16 格内放置梯子（贴墙）或脚手架（脚下，可向上叠柱）；在可攀爬方块中且目标在上方时主动向上爬；每 10 ticks 最多一次，遵守 `mobGriefing` 与 Forge 方块保护 |

条件伤害加成以基础伤害相加，同时满足夜行者与狱火条件时最多 +30%。每个词缀都有独立开关（`nightStalkerEnabled`…`pathfinderEnabled`）；破障者拆方块与开拓者建造方块仍受世界 `mobGriefing` 规则控制。

精英怪使用原版发光效果和名称前缀识别（隐身词缀在半血及以上时会隐藏这两者）。词缀会随实体保存；配置变更只影响之后生成的怪物。首版不添加专属掉落、新实体、模型或运行时依赖。支持单人游戏与专用服务器，能力由服务器结算。

## 服务器配置

配置位于各世界的 `serverconfig/ori_improved_mobs-server.toml`：

```toml
[elites]
enabled = true
spawnChance = 0.05
nightStalkerEnabled = true
frostbornEnabled = true
infernalEnabled = true
breacherEnabled = true
shroudedEnabled = true
vampiricEnabled = true
pathfinderEnabled = true
```

`spawnChance` 范围是 `0.0` 到 `1.0`。全局开关、总生成概率和七个词缀开关均由服务器配置控制。

## 测试

```powershell
.\gradlew.bat test
```
