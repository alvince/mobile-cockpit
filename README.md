# Cockpit

类似 Facebook/Flipper 的桌面端调试、演示和操作工具箱。

## 目标平台

- macOS (DMG / 本地运行)
- Windows (MSI / EXE)

## 技术栈

- Kotlin Multiplatform (KMP) - Desktop (JVM)
- Kotlin 2.2.x
- Compose Multiplatform 1.9.x
- Koin 4.x (依赖注入)
- kotlinx.coroutines

## 项目结构

```
cockpit/
├── composeApp/                 # 桌面应用主模块
│   ├── src/commonMain/         # 通用 UI / ViewModel / DI
│   └── src/desktopMain/        # 桌面入口、平台相关代码
├── gradle/
│   └── libs.versions.toml      # 版本目录
├── build.gradle.kts            # Root build script
├── settings.gradle.kts
└── gradle.properties
```

## 构建与运行

> 需要 JDK 21+（已验证 OpenJDK 21）。

```bash
# macOS 示例（使用 Homebrew OpenJDK 21）
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home

# 运行桌面应用
./gradlew :composeApp:run

# 打包 macOS DMG
./gradlew :composeApp:packageDmg

# 打包 Windows MSI / EXE（需 Windows 环境或 cross-compile 配置）
./gradlew :composeApp:packageMsi
./gradlew :composeApp:packageExe
```

## 终态规划

Cockpit 将集成以下能力：

1. **ADB 常用操作**
   - 设备发现、连接、shell 命令执行
   - 安装 / 卸载 / 日志抓取 / 截图 / 录屏
2. **Scrcpy 手机镜像演示与操作**
   - 桌面窗口实时镜像 Android 设备
   - 键鼠反向控制手机
3. **与手机 App 内 SDK 通信**
   - 网络请求拦截与回放
   - 日志实时查看与过滤
   - 数据库 / SharedPreferences / MMKV 浏览与编辑
   - Sandbox 文件浏览与管理

## 当前状态

项目框架已搭建完成，为空壳应用，可运行并显示主界面。后续按模块逐步填充业务实现。
