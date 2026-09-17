# ERP车机音乐 v0.1.0（第一版测试）

目标：Android 9 车机的独立音乐播放器基础测试版。

已实现：
- Android 9 / API 28 最低兼容
- 横屏车机 UI
- 扫描 MediaStore 本地音乐
- ExoPlayer(Media3) 播放引擎
- MediaSessionCompat
- MediaBrowserServiceCompat
- 播放/暂停、上一首、下一首、进度接口基础
- LRC 解析器
- 原车 PSA_Media 适配层暂未接入

注意：
- 这不是对 QQ音乐/酷我代码的复制。
- 第一版先验证：APK能否安装、能否读取本地音乐、能否播放、车机能否识别 MediaSession。
- 歌词 UI 与 PSA_Media 私有歌词接口在后续版本接入。
