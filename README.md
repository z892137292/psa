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

## 媒体诊断 v0.2.0（独立开发分支，未实车验收）

保持 ERP Music 播放器核心不变，新增首页「媒体诊断」入口。调试版 APK 使用独立的 `com.erp.carmusic.diag` 包名，与旧 ERP Music 并存，不覆盖原安装。

- 「开始后台记录」启动前台服务及常驻通知，后台记录 Android 可见的 AudioPlaybackConfiguration 状态和 MediaSession 包名/播放状态变化。
- 「媒体会话权限」打开系统通知访问设置；手动授权 ERP车机音乐后才能查询其他应用的 MediaSession。通知服务不读取通知文本。
- 「10秒音频焦点测试」是**唯一主动请求音频焦点的按钮**，会发起 `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK` 请求，可能让原车/其他音乐降音，10秒后自动释放；日常被动记录不请求焦点。
- 记录 ERP Music 自身 MediaSession 传输命令；诊断页面前台收到的媒体按键也记录。无法全局监听所有方向盘按键，OEM/MCU 原车音源可能完全不走 Android MediaSession。
- 不采集 PCM、语音录音、通知内容、账号、热点信息。日志仅保存在本地 `media-diagnostic.txt`，上限约 1 MiB（超限清空旧事件）；点击「导出日志」才通过 Android 分享界面发送。
- 不修改、安装或覆盖 `com.androidplay.app` PSA CarPlay。
- Android 9 / API28；本分支只供静态/CI 检查及之后人工停车实车测试，测试结果以真机为准。

### 实车流程（ADB 与 CarPlay 不并行）

1. 安装诊断版 ERP Music，打开媒体诊断，开始后台记录。
2. 可选打开「媒体会话权限」，在系统设置中开启本应用通知使用权，然后返回。**权限是可选的**；不授予时仍记录 Android 可见播放活动。
3. 返回原车音乐，开始播放，然后连接 CarPlay 并播放 iPhone 音乐。
4. 测试方向盘上/下一首、暂停；打开 ChatGPT 语音/Siri 观察原车音乐是否降低音量。普通诊断期间**不要点击**焦点测试，以免干扰被动对照。
5. 如果需要主动鉴别 Android Audio Focus 是否能影响原车音源，再单独点击10秒焦点测试，记录原车音乐是否降低。
6. 断开 CarPlay，回到诊断页停止记录并导出日志。

日志局限：其他 App 播放信息可能被 Android 匿名化；通知访问只可见系统提供的 MediaSession；未收到按键或未见媒体会话不等于原车没有发送或播放；焦点测试只验证本应用请求，不能证明 CarPlay 是否曾请求焦点。
