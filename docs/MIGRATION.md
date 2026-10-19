# 独立仓库拆分记录

2026-10-19，由用户授权将 Launcher 独立维护并发布 Pre-release。

- 原仓库：`Tito-XD/tito-dex`，分支 `codex/launcher-adaptive`，源码 `b532877b83e4e141beb9636d6a9ce2d691c57cab`。
- 提取目录：`launcher/` → 新仓库根目录；新仓库：`Tito-XD/tito-dex-launcher`，默认分支 `main`。
- 提取头：`f9cdb236b45f4def8ee2a8c3b48b0a0da4f8ce7b`。提取树与原目录 Git tree 均为 `a52154c5bc43efff9c64cbda871691680f0380e4`，85 个已跟踪文件逐项保持一致。
- 完整保留 12 次 Launcher 相关提交的作者、时间、消息与目录快照。子目录变为仓库根目录，提交 SHA 因树和父链改变而重新生成，映射如下。
- 提取后只添加独立 CI、当前 README、上下文、发布与设计文档，以及演示截图；应用源码、资源、构建版本、包名、偏好与签名机制保持不变。
- 原仓库、原工作区和其他任务代码未删除、改分支或重置。旧 release 继续留在原仓库，本次新 release 只发布到独立仓库。
- 未迁入私有设备备份、用户壁纸、密钥、构建缓存、TitoDex Flutter App 或数据服务。未新增项目级开源授权，第三方许可保持原状。

| 原提交 | 独立仓库提交 | 内容 |
| --- | --- | --- |
| `1c94b3087e1d9c91521885272ea995c3e842ac39` | `a0d95e9f82795a5780fedaf717fc1a379e47e975` | Add TitoDex Launcher preview and touchscreen redesign handoff |
| `50b10c5f90ff12f7a7f81793988807f782a35ad9` | `5a7556b0f93914f5c9f6be39d9ef4f4d96f68134` | Migrate launcher to native Compose with touch editing and bounded motion |
| `db779e51f30c89d5a5705904d013be547df6d04d` | `ac94fcc7c8aca7182b9c304088af798dd50db33d` | Keep launcher hit targets aligned through stage motion |
| `b2566d07900eac3af2ada8e8fb4f7a46eca5342a` | `1f06a5b681f75f88d07b51269bd5e959286607d3` | Record launcher prerelease and final RG acceptance |
| `9b85a56f58bf6481e78242111e4f1acec538e44f` | `49e689aa590200f894f57cb86acf904b4cc2ee76` | Adapt launcher layouts to system bars and screen sizes |
| `7121b4cbc67571131da9df4fc5241e6405bda811` | `a521730076ec0d7f3d8467889044c5da2e9cc59d` | Keep portrait weather legible and system bars transparent |
| `6803cd8925e30a6d0e23cec4e9d719e8b1df1dbd` | `c58ad59383b9620bb9a0b4ef0fa834252e108314` | Restore warm matte plastic surfaces and frameless app icons |
| `cbabb5e107b7d20a08189333fa8a320ed474b713` | `6c284d14b913d6fdaa5b4f8b4f0a4be8cbf6a11b` | Add continuous launcher motion and restore two-line clock layout |
| `d423ddd845249ea173a1a80d01ed85e8d7326c9c` | `f82acb92f58e0ca4430c1491b52045b9ca5dce1d` | Center clock typography and align configurable trainer avatars |
| `cc0e49524ea117a049d952ac050147c298f18192` | `0da55bc75423f55a1cfe7d05184d1e267aead1ac` | Align weather ink and unify fixed launcher corner radii |
| `3b83875b23262dda3501f08b7f34de7adc8b7607` | `17a5e9c4e50aa2fffc78aef977bdb9613f879d1d` | Illustrate weather forecasts and balance clock spacing |
| `b532877b83e4e141beb9636d6a9ce2d691c57cab` | `f9cdb236b45f4def8ee2a8c3b48b0a0da4f8ce7b` | Use weather icon controls and preserve settings on close |
