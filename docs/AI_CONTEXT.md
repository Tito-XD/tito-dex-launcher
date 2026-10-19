# TitoDex Launcher — 开发上下文

唯一活跃实现是根目录 `app/` 的 Kotlin + Jetpack Compose Android 工程。独立仓库为 `Tito-XD/tito-dex-launcher`；TitoDex App 在原仓库继续使用 Flutter，二者没有构建依赖。

- 当前版本：`0.3.2-adaptive.8` / code `14`；包名 `com.tito.titodex.launcher`。
- 工具链：JDK 17、Android SDK 36、Gradle Wrapper 8.14；最低 API 24。
- 构建：`./gradlew --no-daemon --max-workers=2 :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease`。
- 串行构建，最多两个 worker，沿用 `gradle.properties` 内存限制。不要与 Flutter App 同时构建。
- 当前开发签名 SHA-256：`32b5a8a65a081518b592a49a1ce66cacbcdfe1982850b414721fa51465a65e0f`。仅为开发预览；CI 或另一台机器的默认 debug key 不可替代既有升级签名。

## 核心代码

- `LauncherState.kt`：模式、弹层、撤销、偏好和应用操作。
- `Model.kt` / `Grid.kt`：兼容设置 schema、组件身份和屏幕布局。
- `AndroidDevice.kt` / `WidgetHostBridge.kt`：系统应用、HOME 和第三方小组件。
- `ui/LauncherApp.kt` / `Stage.kt`：安全留边、桌面与触屏。
- `ui/Stickers.kt` / `OpticalText.kt`：时钟、名片与可见字形对齐。
- `ui/WeatherForecast.kt` / `WeatherControls.kt`：天气图形、城市入口、刷新和只关闭弹窗的叉号。
- `ui/Theme.kt` / `Motion.kt` / `Focus.kt`：三主题、连续动效和按键焦点。

以上 Kotlin 路径均位于 `app/src/main/kotlin/com/tito/titodex/launcher/`。检查位于 `app/src/test/kotlin/`；`design/prototype.html` 是历史交互原型，不是当前应用。

## 必须保留

HOME / 返回、应用库、个人名片问候、时钟日期天气、壁纸效果、恢复默认、触屏和手柄操作。主入口可替换或隐藏，旧用户默认保持 TitoDex；保留内部 `tito` ID。方屏/横屏/竖屏布局分别保存，不重置用户排布。偏好 schema、第三方小组件 host ID 和撤销语义不可无迁移变更。

时间一行、日期/镂空星期/天气一行。测量实际可见字形的对齐，不用行框中心代替。天气卡片叉号仅关闭弹窗；停用天气保留在设置中。三主题、间距、圆角和动效见 `docs/DESIGN_SYSTEM.md`、`ADAPTIVE_LAYOUT.md`。

天气需用户选择城市，无定位权限，不伪造实时天气。不要读取或上传设备私人偏好、壁纸、存档。默认 HOME 和系统壁纸由用户选择。

## 验证与发布

自动检查、组件截图、APK 签名、上传发布、真机验收分别报告。组件预览使用模拟数据，不当作设备效果。设备操作须遵循根目录 `AGENTS.md` 和 `tool/test_device.py` 的备份/覆盖安装/恢复/逐文件核对流程；不卸载、不清空数据。

经用户授权才发布，先核对 tag 源码、包名版本、签名、校验和，再按 `RELEASING.md` 发布 GitHub Pre-release。不要自动切换默认 HOME。拆分来源和历史映射见 `docs/MIGRATION.md`。
