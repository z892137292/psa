package com.erp.carmusic;

import android.Manifest;
import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    private static final int REQ = 10;
    private TextView song, artist, lyric;
    private Button play;
    private ArrayList<LrcParser.Line> lines = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        song = findViewById(R.id.song);
        artist = findViewById(R.id.artist);
        lyric = findViewById(R.id.lyric);
        play = findViewById(R.id.play);

        if (Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQ);
        }

        song.setText("ERP车机音乐");
        artist.setText("本地音乐播放器");
        lyric.setText("第一版\n等待选择歌曲");
        play.setOnClickListener(v -> {
            Intent i = new Intent(this, PlaybackService.class);
            i.setAction("android.intent.action.MEDIA_PLAY_FROM_SEARCH");
            startService(i);
            play.setText("暂停");
        });
        findViewById(R.id.prev).setOnClickListener(v -> lyric.setText("上一首"));
        findViewById(R.id.next).setOnClickListener(v -> lyric.setText("下一首"));
    }
}
