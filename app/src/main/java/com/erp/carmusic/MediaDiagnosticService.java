package com.erp.carmusic;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.AudioPlaybackConfiguration;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Passive audio and session observer, with a separately opt-in 10-second focus probe. */
public class MediaDiagnosticService extends Service {
    public static final String START = "com.erp.carmusic.diagnostic.START";
    public static final String STOP = "com.erp.carmusic.diagnostic.STOP";
    public static final String FOCUS = "com.erp.carmusic.diagnostic.FOCUS";
    public static final String RELEASE_FOCUS = "com.erp.carmusic.diagnostic.RELEASE_FOCUS";
    private static final String CHANNEL = "erp_media_diagnostic";
    private static final int NOTIFICATION_ID = 2102;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private AudioManager audio;
    private MediaSessionManager sessions;
    private AudioFocusRequest focusRequest;
    private boolean recording;
    private String lastSessions = "";
    private String lastAudio = "";
    private boolean permissionWarningSent;
    private final AudioManager.OnAudioFocusChangeListener focusListener =
            change -> MediaDiagnosticLog.record(this, "FOCUS_PROBE", "本应用焦点变化=" + change);
    private final Runnable releaseLater = this::releaseFocus;
    private final AudioManager.AudioPlaybackCallback playbackCallback =
            new AudioManager.AudioPlaybackCallback() {
                @Override public void onPlaybackConfigChanged(List<AudioPlaybackConfiguration> configs) {
                    if (!recording) return;
                    ArrayList<String> visible = new ArrayList<>();
                    for (AudioPlaybackConfiguration c : configs) {
                        try {
                            // Android may anonymize other applications' playback configurations.
                            AudioAttributes attributes = c.getAudioAttributes();
                            visible.add(attributes == null ? "attributes=unavailable"
                                    : "usage=" + attributes.getUsage()
                                    + " content=" + attributes.getContentType());
                        } catch (Exception ignored) {
                            visible.add("unavailable");
                        }
                    }
                    Collections.sort(visible);
                    String snapshot = "playersVisible=" + configs.size() + " " + visible;
                    if (!snapshot.equals(lastAudio)) {
                        lastAudio = snapshot;
                        MediaDiagnosticLog.record(MediaDiagnosticService.this, "PLAYBACK", snapshot
                                + " (播放器身份/逐播放器状态/厂家音源：权限或接口不可见；不是原车音源归属证明)");
                    }
                }
            };

    @Override public void onCreate() {
        super.onCreate();
        audio = getSystemService(AudioManager.class);
        sessions = getSystemService(MediaSessionManager.class);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? START : intent.getAction();
        if (STOP.equals(action)) {
            MediaDiagnosticLog.record(this, "SYSTEM", "用户停止后台记录");
            stopSelf();
            return START_NOT_STICKY;
        }
        if (RELEASE_FOCUS.equals(action)) {
            releaseFocus();
            if (!recording) stopSelf();
            return START_NOT_STICKY;
        }
        if (!recording) startObserving();
        if (!recording) return START_NOT_STICKY;
        if (FOCUS.equals(action)) testFocus();
        return START_STICKY;
    }

    private void startObserving() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm == null) {
            MediaDiagnosticLog.record(this, "ERROR", "前台通知权限或接口不可见，未启动记录或焦点测试");
            stopSelf();
            return;
        }
        nm.createNotificationChannel(new NotificationChannel(CHANNEL, "ERP媒体诊断", NotificationManager.IMPORTANCE_LOW));
        Intent open = new Intent(this, MediaDiagnosticActivity.class);
        PendingIntent click = PendingIntent.getActivity(this, 1, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle("ERP 媒体诊断正在记录")
                .setContentText("不录音、不改变媒体控制；点击查看")
                .setContentIntent(click)
                .setOngoing(true).build();
        startForeground(NOTIFICATION_ID, notification);
        recording = true;
        MediaDiagnosticLog.record(this, "SYSTEM", "开始后台记录；Android=" + android.os.Build.VERSION.SDK_INT
                + "，仅记录平台可见信息");
        try {
            audio.registerAudioPlaybackCallback(playbackCallback, handler);
        } catch (Exception e) {
            MediaDiagnosticLog.record(this, "ERROR", "播放活动监听不可用：" + e.getClass().getSimpleName());
        }
        handler.post(sessionPoll);
    }

    private final Runnable sessionPoll = new Runnable() {
        @Override public void run() {
            if (!recording) return;
            try {
                ComponentName listener = new ComponentName(MediaDiagnosticService.this, MediaAccessListener.class);
                List<MediaController> controllers = sessions.getActiveSessions(listener);
                permissionWarningSent = false;
                ArrayList<String> states = new ArrayList<>();
                for (MediaController controller : controllers) {
                    PlaybackState playback = controller.getPlaybackState();
                    states.add(controller.getPackageName() + "="
                            + (playback == null ? "unknown" : playback.getState()));
                }
                Collections.sort(states);
                String snapshot = "activeSessions=" + states.size() + " " + states;
                if (!snapshot.equals(lastSessions)) {
                    lastSessions = snapshot;
                    MediaDiagnosticLog.record(MediaDiagnosticService.this, "SESSIONS", snapshot
                            + " (原车MCU媒体可能不可见)");
                }
            } catch (SecurityException denied) {
                if (!permissionWarningSent) {
                    permissionWarningSent = true;
                    MediaDiagnosticLog.record(MediaDiagnosticService.this, "ACCESS",
                            "媒体会话权限或接口不可见：请在系统「通知使用权」授权本诊断应用；未授权不影响播放活动监听");
                }
            } catch (Exception error) {
                if (!permissionWarningSent) {
                    permissionWarningSent = true;
                    MediaDiagnosticLog.record(MediaDiagnosticService.this, "ACCESS",
                            "媒体会话接口不可用：" + error.getClass().getSimpleName());
                }
            }
            handler.postDelayed(this, 2500);
        }
    };

    private void testFocus() {
        if (focusRequest != null) releaseFocus();
        if (audio == null) {
            MediaDiagnosticLog.record(this, "FOCUS_PROBE", "音频服务不可用，未请求焦点");
            return;
        }
        focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                .setOnAudioFocusChangeListener(focusListener, handler).build();
        int result;
        try {
            result = audio.requestAudioFocus(focusRequest);
        } catch (RuntimeException unavailable) {
            MediaDiagnosticLog.record(this, "FOCUS_PROBE", "焦点请求不可用：" + unavailable.getClass().getSimpleName());
            releaseFocus();
            return;
        }
        MediaDiagnosticLog.record(this, "FOCUS_PROBE", "用户主动请求短暂MAY_DUCK焦点 result=" + result
                + "；可能降低其他音乐音量；10秒自动释放");
        handler.removeCallbacks(releaseLater);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            handler.postDelayed(releaseLater, 10000);
        } else {
            releaseFocus();
        }
    }

    private void releaseFocus() {
        handler.removeCallbacks(releaseLater);
        if (focusRequest != null) {
            AudioFocusRequest request = focusRequest;
            focusRequest = null;
            try {
                int result = audio.abandonAudioFocusRequest(request);
                MediaDiagnosticLog.record(this, "FOCUS_PROBE", "释放本应用音频焦点 result=" + result);
            } catch (RuntimeException unavailable) {
                MediaDiagnosticLog.record(this, "FOCUS_PROBE", "平台释放焦点接口异常：" + unavailable.getClass().getSimpleName());
            }
        }
    }

    @Override public void onDestroy() {
        recording = false;
        handler.removeCallbacks(sessionPoll);
        releaseFocus();
        try { audio.unregisterAudioPlaybackCallback(playbackCallback); } catch (Exception ignored) {}
        MediaDiagnosticLog.record(this, "SYSTEM", "后台记录结束");
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
