package com.erp.carmusic;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.core.content.FileProvider;

/** Car-friendly standalone screen. No root, PCM capture, or media control hijack. */
public class MediaDiagnosticActivity extends Activity {
    private TextView logView;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            if (logView != null) logView.setText(MediaDiagnosticLog.tail(MediaDiagnosticActivity.this));
            handler.postDelayed(this, 1600);
        }
    };

    @Override public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        setTitle("ERP 媒体诊断");
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20, 12, 20, 12);
        root.setBackgroundColor(0xff101820);
        TextView title = new TextView(this);
        title.setText("PSA 媒体控制权诊断 · Android 9");
        title.setTextSize(22);
        title.setTextColor(0xffffffff);
        root.addView(title);
        TextView hint = new TextView(this);
        hint.setText("默认只观察：不抢占音频焦点，不控制原车音乐。\n音频焦点测试需主动点击，可能暂时降低音乐音量。");
        hint.setTextSize(14);
        hint.setTextColor(0xffbed5e1);
        root.addView(hint);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        add(actions, "开始后台记录", () -> action(MediaDiagnosticService.START));
        add(actions, "停止记录", () -> action(MediaDiagnosticService.STOP));
        add(actions, "媒体会话权限", () -> {
            try { startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)); }
            catch (Exception e) { MediaDiagnosticLog.record(this, "ERROR", "无法打开通知权限设置"); }
        });
        add(actions, "导出日志", this::share);
        root.addView(actions);

        LinearLayout probes = new LinearLayout(this);
        probes.setOrientation(LinearLayout.HORIZONTAL);
        add(probes, "10秒音频焦点测试", () -> action(MediaDiagnosticService.FOCUS));
        add(probes, "释放焦点", () -> action(MediaDiagnosticService.RELEASE_FOCUS));
        add(probes, "刷新日志", () -> logView.setText(MediaDiagnosticLog.tail(this)));
        root.addView(probes);

        TextView info = new TextView(this);
        info.setText("操作：开始记录 → 返回原车/CarPlay 播音乐 → 按方向盘按键 → ChatGPT/Siri → 回来导出。\n"
                + "只会记录系统可见会话；未收到按键不等于原车没有发送，MCU 或 OEM 音源可能不经 Android MediaSession。");
        info.setTextSize(13);
        info.setTextColor(0xffb8c4ca);
        root.addView(info);

        ScrollView scroll = new ScrollView(this);
        logView = new TextView(this);
        logView.setTextSize(12);
        logView.setTextColor(0xffe8ecef);
        logView.setTextIsSelectable(true);
        logView.setPadding(8, 8, 8, 8);
        scroll.addView(logView);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    private void add(LinearLayout row, String text, Runnable click) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(12);
        row.addView(b, new LinearLayout.LayoutParams(0, -2, 1));
        b.setOnClickListener(v -> click.run());
    }

    private void action(String action) {
        Intent intent = new Intent(this, MediaDiagnosticService.class).setAction(action);
        if (MediaDiagnosticService.STOP.equals(action)) {
            startService(intent);
        } else {
            startForegroundService(intent);
        }
    }

    private void share() {
        try {
            java.io.File file = MediaDiagnosticLog.file(this);
            if (!file.exists()) {
                MediaDiagnosticLog.record(this, "SYSTEM", "用户请求导出日志（当前暂无事件）");
            }
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".files", file);
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/plain");
            send.putExtra(Intent.EXTRA_STREAM, uri);
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(send, "导出 ERP 媒体诊断日志"));
        } catch (Exception e) {
            MediaDiagnosticLog.record(this, "ERROR", "导出失败 " + e.getClass().getSimpleName());
            logView.setText(MediaDiagnosticLog.tail(this));
        }
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                || keyCode == KeyEvent.KEYCODE_MEDIA_NEXT
                || keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY
                || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
            MediaDiagnosticLog.record(this, "KEY_IN_ACTIVITY",
                    "诊断界面获得按键=" + KeyEvent.keyCodeToString(keyCode)
                            + " (仅当诊断界面收到，非全系统监听)");
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override protected void onStart() { super.onStart(); handler.post(refresh); }
    @Override protected void onStop() { handler.removeCallbacks(refresh); super.onStop(); }
}
