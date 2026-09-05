# 跃题 v0.11.0 第三方资源清单

本清单对应私人非商业 APK `Yueti-v0.11.0.apk`。完整许可文本同时随 APK 打包，并可在应用的开源许可页查看。

| 资源 | 固定版本/提交 | 用途 | 许可与限制 |
|---|---|---|---|
| Emotion Ball | `b406eeb20a1b1ae0084d4006e77cc74e28be009d` | 首页、AI、成绩与词卡中的 32 状态跃跃形象 | Emotion Ball Community License；球形角色视觉仅限个人学习研究，禁止商业使用 |
| OSS Document Scanner | `05f9611a35e7e2ff35de4f54e845c724d9168bc7` | OpenCV 文档轮廓、四角排序、透视与增强处理链 | MIT |
| ECDICT | `bc015ed2e24a7abef49fc6dbbb7fe32c1dadaf8b` | 4,794 个带 IELTS 标签的离线词条 | MIT；Copyright © 2025 Linwei |
| Wikimedia Commons | 运行时 API | 可选的词卡记忆配图 | 每张图片按 API 返回的独立许可使用；应用内保留作者、许可与原始页面链接 |
| Bing 图片网页 | 运行时网页 | 用户主动点选词卡记忆配图 | 不使用已停用的 Bing Search API，不静默抓取首图；保存用户确认的本地副本、来源域名与结果页 |
| netease-cloud-music | `09440b8737d847882ed961f4491fe65733685d7d` | 网易云未公开接口的登录校验、歌单、搜索、元数据与标准音质播放地址协议参考 | MIT；仅私人非商业使用，不含下载、自动化、解灰、VIP 或 DRM 绕过 |
| AndroidX Media3 | `1.11.0` | ExoPlayer、后台 MediaSession、系统媒体通知与音频焦点 | Apache License 2.0 |
| AndroidX / Jetpack Compose / Material 3 / Room / WorkManager | 见 Gradle 锁定版本 | Android UI、数据、后台任务 | Apache License 2.0 |
| Coil | `3.4.0` | 词卡网络图片加载与缓存 | Apache License 2.0 |

上游地址：

- Emotion Ball: https://github.com/sam70361/emotion-ball
- OSS Document Scanner: https://github.com/ossappscollective/OSS-DocumentScanner
- ECDICT: https://github.com/skywind3000/ECDICT
- Wikimedia Commons API: https://commons.wikimedia.org/w/api.php
- Bing 图片: https://www.bing.com/images
- netease-cloud-music: https://github.com/chaunsin/netease-cloud-music/tree/09440b8737d847882ed961f4491fe65733685d7d
