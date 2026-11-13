# Loom Build

把手机里的一个 Android 项目，交给 GitHub Actions 编译成 APK —— 全程不必开电脑。

界面沿用 [Loom](https://github.com/CelestialCatkins/Loom) 的视觉体系：稿纸 / 墨 / 单一铜色，衬线标题、等宽标识、发线分隔。

## 它做什么

1. **绑定 GitHub**（一次）：粘贴 Personal Access Token。
2. **选项目**：通过系统目录选择器挑一个 Android Studio 项目的根目录。
3. **上传并构建**：自动新建（或复用）仓库、写入全部文件、注入 `build-apk.yml`、触发 Actions。
4. **取回 APK**：轮询运行状态，成功后下载 artifact，直接调起系统安装器。

## 前置条件

- Android 8.0（API 26）及以上。
- 一个 GitHub 账号。
- 目标项目是 **Gradle 构建的 Android 项目**（根目录有 `settings.gradle(.kts)` 或 `build.gradle(.kts)`）。

## 创建令牌

打开 <https://github.com/settings/tokens/new?scopes=repo,workflow>，勾选：

| 权限 | 为什么需要 |
|---|---|
| `repo` | 创建私有仓库、写入文件 |
| `workflow` | 提交 `.github/workflows/*.yml` |

令牌形如 `ghp_…`。粘贴进 App 后只保存在应用私有目录。

## 它怎么构建

上传后会在目标仓库写入 `.github/workflows/build-apk.yml`，内容等价于：

- JDK 17 + Gradle 8.7（若项目自带 `gradlew` 则优先用 wrapper）
- 直接用 runner 预装的 Android SDK，安装 platform 34/35/36 与 build-tools
- `assembleDebug` + `assembleRelease`
- 收集所有 APK 并作为 artifact 上传

> 注意：这里刻意不用 `android-actions/setup-android`，因为它会执行 `sdkmanager tools`，而该包已从 SDK 源移除，会让步骤直接失败。

## 边界与已知限制

- 只接受 Gradle 项目；不会尝试 CMake/Flutter 等其他构建方式。
- 单文件上限 12 MB，文件总数上限 800（超出部分跳过）。
- 私有仓库的 Actions 分钟数计入账号额度。
- 令牌存于应用私有目录（非加密存储）；不再使用时请在设置里解绑，并在 GitHub 侧吊销。

## 构建

```bash
# 无需本地 gradlew
gradle :app:assembleDebug
```

或直接推到 `main`，由本仓库的 Actions 产出 APK。
