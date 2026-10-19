# 预发布流程

当前预览使用开发签名，不自动升级为正式生产发行。仓库拆分不改变包名 `com.tito.titodex.launcher` 或原签名。

1. 确认用户已授权发布；检查工作区，仅发布已提交源码。`app/build.gradle.kts` 的版本名和 versionCode 必须与 APK、tag 一致，新功能版本的 code 递增。
2. 使用 JDK 17 和 SDK 36，串行运行 `./gradlew --no-daemon --max-workers=2 :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease`。不能与 Flutter 构建并行。
3. 用 Android Build Tools 的 `apksigner verify --verbose --print-certs`、`aapt dump badging` 与 `zipalign -c -P 16 -v 4` 核对两包。可覆盖既有预览的证书 SHA-256 必须是 `32b5a8a65a081518b592a49a1ce66cacbcdfe1982850b414721fa51465a65e0f`。CI 生成的临时 debug 证书不能作为替代，不提交私钥。
4. 生成验证 JSON 与 SHA256SUMS，记录仓库、源码 SHA、版本、包名、签名、测试数量以及设备验证状态。只发布 release APK、校验和、验证记录；debug 包留作受控设备备份流程的同版本同签名配套。
5. 在 `Tito-XD/tito-dex-launcher` 创建 `v<versionName>` tag，指向验证源码；先创建 draft pre-release，上传完整附件，下载并比对 SHA-256，再设置 `draft=false`、`prerelease=true`、`make_latest=false`。说明中写明开发预览签名和真机覆盖边界。
6. 读回 tag、发布类型、附件列表和下载文件校验和，向用户提供独立仓库与预发布链接。原 TitoDex App 仓库的版本和 Latest 不受影响。

工作区的 `build/`、`app/build/`、私有设备备份、个人壁纸、密钥不可进 Git 或公开附件。演示数据的组件截图可以放入 `docs/images/`，须明确不是设备截图。

CI 仅执行检查和 Debug 构建，没有写仓库、发布、访问设备或使用签名秘密的权限。
