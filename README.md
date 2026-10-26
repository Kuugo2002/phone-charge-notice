# 充满提醒

[简体中文](README.md) | [English](README_EN.md)

[![Flutter Android](https://github.com/Kuugo2002/phone-charge-notice/actions/workflows/flutter-android.yml/badge.svg)](https://github.com/Kuugo2002/phone-charge-notice/actions/workflows/flutter-android.yml)

使用 Flutter 和 Material 3 开发的 Android 充满提醒应用。连接充电器后，电量首次达到 100% 时亮屏并发送通知。

## 功能

- 实时查看电量和充电状态。
- Android 前台服务持续监听电池状态，关闭界面后仍可监控。
- 充满时亮屏、震动并显示锁屏通知；同一次充电只提醒一次，拔下充电器后重置。
- 开机和应用更新后自动恢复监控。
- 内置测试提醒和系统通知设置入口。
- 可选 Root 增强：加入 Doze 白名单，并通过 Root 命令辅助唤醒屏幕。
- 四种主题色：清新绿、晴空蓝、鸢尾紫、暖阳橙。
- 跟随系统、浅色、深色三种外观模式，即时生效并保存。

## 安装与使用

支持 Android 8.0（API 26）及以上版本。

1. 安装 APK，打开应用并允许通知权限。
2. 点击「测试提醒」，检查亮屏与通知效果。
3. 在「设置」中选择主题颜色和暗黑模式；已 Root 的设备可按需开启 Root 增强。

部分厂商系统还需要允许自启动、后台运行和锁屏通知。Root 增强不是使用基本提醒功能的必要条件。

## 从 GitHub Actions 获取 APK

打开 [Flutter Android 工作流](https://github.com/Kuugo2002/phone-charge-notice/actions/workflows/flutter-android.yml)，选择一次成功运行，在 **Artifacts** 中下载 `charge-notice-debug-运行编号`，解压得到 `app-debug.apk`。下载构建产物需要登录 GitHub，产物保留 14 天。

每次 push、pull request 和手动运行都会依次执行：安装锁定依赖、格式检查、静态分析、测试、Android 调试包构建，以及上传 APK。工作流使用 Flutter **3.47.8 stable** 和 JDK **17**，与本地已验证环境一致。

自动构建提供调试签名 APK，用于安装测试。正式发布需配置自己的稳定签名；不同签名的安装包无法直接覆盖安装。

## 本地开发

准备 Flutter **3.47.8 stable**、JDK **17** 和 Android SDK **36**。使用 Android Studio（安装 Flutter 插件）或 VS Code 打开项目根目录。

```sh
flutter doctor
flutter pub get --enforce-lockfile
flutter run
```

执行与 CI 一致的检查并构建：

```sh
dart format --output=none --set-exit-if-changed lib test
flutter analyze --no-pub
flutter test --no-pub
flutter build apk --debug --no-pub
```

安装包输出路径为 `build/app/outputs/flutter-apk/app-debug.apk`。`pubspec.lock` 已纳入版本控制；升级依赖后请同步更新锁文件。应用版本在 `pubspec.yaml` 中维护。

正式发布前，在 `android/app/build.gradle` 中配置签名，然后执行 `flutter build apk --release`。签名密钥和本地配置不应提交到仓库。

## 项目结构

```text
.github/workflows/flutter-android.yml  自动检查和 APK 构建
lib/main.dart                         Flutter 界面和主题
lib/charge_controller.dart            状态管理与平台调用
android/                              Android 宿主和后台服务
test/                                 Flutter 测试
pubspec.yaml                          版本和依赖
pubspec.lock                          锁定依赖
```

Flutter 通过 MethodChannel 调用 Android 功能，通过 EventChannel 接收电量变化。后台服务独立于 Flutter 界面运行。原应用包名 `com.kuugo.chargenotice` 和偏好设置存储保持一致，同签名覆盖安装可保留已有设置。当前仅支持 Android。

构建产物、缓存、SDK 路径和签名文件均由 `.gitignore` 排除；APK 通过 Actions 分发，不存入源码仓库。

## 常见构建问题

- **Java 版本不兼容**：执行 `flutter config --jdk-dir="你的 JDK 17 路径"`，再重新构建。
- **Windows 中文路径下分析器报告 LSP JSON 解析错误**：使用 `subst Q: "项目绝对路径"` 映射目录，进入 `Q:\` 执行检查，完成后在其他目录执行 `subst Q: /D`。
- **首次构建下载较慢**：Gradle 会按需安装 NDK 和 CMake，请确认 Android SDK 许可证已接受，可执行 `flutter doctor --android-licenses`。
