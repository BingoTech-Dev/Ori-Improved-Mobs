# Ori Improved Mobs — Forge 开发环境

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

当前是干净的 Forge mod 起始骨架，尚未实现具体生物行为。
