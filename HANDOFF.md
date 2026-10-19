# TitoDex Launcher — 独立仓库交接

## 当前入口

- 仓库：`Tito-XD/tito-dex-launcher`，默认分支 `main`；新工作使用 `codex/` 分支。
- 原生工程位于根目录，源码在 `app/`，不再有外层 `launcher/` 或 Flutter 依赖。
- 先读 `docs/AI_CONTEXT.md`、`docs/DESIGN_SYSTEM.md`、`AGENTS.md` 和 `ADAPTIVE_LAYOUT.md`。
- 从原仓库 `b532877b83e4e141beb9636d6a9ce2d691c57cab` 的 `launcher/` 提取，保留 12 次相关提交；详见 `docs/MIGRATION.md`。

## 本次预发布

已发布 [v0.3.2-adaptive.8](https://github.com/Tito-XD/tito-dex-launcher/releases/tag/v0.3.2-adaptive.8)，保持 Pre-release，不设为 Latest。发布源码为 `19774c51b223c2c1036428ef0da9851ea7e7920c`；[独立 CI](https://github.com/Tito-XD/tito-dex-launcher/actions/runs/39199492841) 已通过。三个公开附件已下载回验，APK SHA-256：`44b14acc338f1717a73c5ce4d7c991c483b5d51e23dcc37fceb16b2aa3741e01`。

`0.3.2-adaptive.8` / code 14。包含安全留边、独立屏幕布局、可替换主入口、三主题回调、连续动效、时钟与头像可见对齐、图形天气和图标操作。

天气顶部是楼房图标与城市名，左下刷新，右下叉号只关闭弹窗，保留城市、预报与桌面天气。三主题一致；无城市、加载中和错误状态也能关闭。原有天气停用入口仍在设置里。

拆分只改变仓库组织与开发文档，包名、签名、versionCode、偏好 schema、组件 host ID 均连续。不要要求用户卸载或清空数据。

## 验证与设备

拆分后已从新仓库根目录重新完成 80 项检查及 Debug/Release 构建，签名、版本与对齐检查通过，并核对三主题原生截图；独立仓库构建/发布证据见 `VALIDATION.md` 与发布附件。当前 adaptive.8 尚未在 RG 上安装验收。此前 motion.1 的设备通过不能代表此版通过。

真机验证必须执行 `tool/test_device.py <serial> --apk <release.apk> --debug-apk <same-version-debug.apk>`，配置 JDK 17 和 Android SDK；先备份偏好与照片，`install -r` 更新，恢复并逐文件核对。禁止卸载、清空数据、自动改默认桌面或更换系统壁纸。备份、设备照片及用户截图不可公开上传。

## 历史

旧仓库和现有本地工作区未删除、重置或改分支；TitoDex App 与其他任务继续在原仓库。原始 Launcher 交接归档在 `docs/history/HANDOFF-monorepo.md`，0.2 Flutter 说明在 `docs/history/README-0.2.md`。历史命令与路径只反映当时状态，当前以本文件和 `docs/AI_CONTEXT.md` 为准。
