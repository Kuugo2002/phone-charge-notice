# 充满提醒

[简体中文](README.md) | [English](README_EN.md)

一个简洁的原生 Android 应用。手机接着充电器且电量首次达到 100% 时，会点亮屏幕并发送高优先级通知。

## 功能

- 前台服务持续监听系统电池状态
- 充满时亮屏、震动并显示锁屏通知
- 同一次充电过程只提醒一次，拔掉充电器后重置
- 开机自动恢复监控
- 可选 Root 增强：加入 Doze 白名单，并在充满时使用 Root 命令再次唤醒屏幕
- 内置“测试提醒”按钮

## 构建

使用 Android Studio 打开项目，等待 Gradle 同步完成后运行即可。项目需要 JDK 17、Android SDK 36 或更高版本。

首次打开应用时请允许通知权限。部分厂商系统还需要手动允许“自启动”，并允许通知在锁屏显示。

## 自动构建

GitHub Actions 工作流 [Android CI](.github/workflows/android.yml) 会在推送代码、创建或更新拉取请求时自动运行，也可以在仓库的 **Actions → Android CI → Run workflow** 中手动触发。

工作流使用 JDK 17 和 Android SDK 36，运行单元测试与 Android Lint 检查，通过后构建 Debug APK。进入对应运行记录的 **Artifacts** 区域，下载 `ChargeNotice-debug-运行编号` 即可获取测试安装包；测试与静态检查报告保存在 `android-reports-运行编号` 中，构建产物保留 14 天。无需配置额外的 Secrets。
