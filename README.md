# ANPilotClient

ANPilotClient 是一个基于 Fabric 与 Kotlin 开发的 Minecraft 客户端 Mod，包含战斗、移动、渲染、HUD、玩家辅助和自动建造等模块。

本项目主要用于个人学习、开发测试与客户端功能实验。使用时请遵守目标服务器规则以及相关平台条款。

## 环境要求

- Minecraft `26.1.2`
- Java `25` 或更高版本
- Fabric Loader `0.19.2` 或更高版本
- Fabric API
- Fabric Language Kotlin

## 构建

Windows:

```powershell
.\gradlew.bat build
```

Linux / macOS:

```bash
./gradlew build
```

构建产物位于 `build/libs/`。

## 开发

推荐使用 IntelliJ IDEA 打开项目，并等待 Gradle 同步完成。

常用命令:

```powershell
.\gradlew.bat compileKotlin
.\gradlew.bat runClient
```

## 依赖

项目通过 Gradle 拉取主要依赖:

- Minecraft
- Fabric Loader
- Fabric API
- Fabric Language Kotlin
- Sodium
- Baritone API

请不要将第三方依赖 Jar 直接提交到仓库。

## 许可证

本项目使用 GNU General Public License v3.0 only 授权，详见 `LICENSE`。

如果你分发本项目的修改版本或基于本项目的衍生作品，需要按照 GPL-3.0-only 的要求一并提供对应源码。
