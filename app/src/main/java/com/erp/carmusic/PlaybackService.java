package com.erp.carmusic;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import androidx.annotation.Nullable;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.session.MediaButtonReceiver;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import androidx.media3.common.*;
import androidx.media3.exoplayer.ExoPlayer;
import java.util.*;

public class PlaybackService extends MediaBrowserServiceCompat {
    public static final String ROOT = "ROOT";
    private static final String CHANNEL = "erp_car_music";
    private MediaSessionCompat session;
    private ExoPlayer player;
    private final ArrayList<MediaItem> songs = new ArrayList<>();

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
        player = new ExoPlayer.Builder(this).build();

        session = new MediaSessionCompat(this, "ERP_CarMusic");
        session.setCallback(new MediaSessionCompat.Callback() {
            @Override public void onPlay() { player.play(); updateState(); }
            @Override public void onPause() { player.pause(); updateState(); }
            @Override public void onSkipToNext() { player.seekToNext(); updateState(); }
            @Override public void onSkipToPrevious() { player.seekToPrevious(); updateState(); }
            @Override public void onSeekTo(long pos) { player.seekTo(pos); updateState(); }
        });
        session.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        setSessionToken(session.getSessionToken());

        scanSongs();
    }

    private void scanSongs() {
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA
        };
        try (Cursor c = getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection,
                MediaStore.Audio.Media.IS_MUSIC + "!=0", null,
                MediaStore.Audio.Media.TITLE + " COLLATE NOCASE")) {
            if (c == null) return;
            while (c.moveToNext()) {
                long id = c.getLong(0);
                String title = c.getString(1);
                String artist = c.getString(2);
                long duration = c.getLong(3);
                String path = c.getString(4);
                MediaItem item = new MediaItem.Builder()
                        .setMediaId(String.valueOf(id))
                        .setUri(Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, String.valueOf(id)))
                        .setMediaMetadata(new MediaMetadata.Builder()
                                .setTitle(title == null ? "未知歌曲" : title)
                                .setArtist(artist == null ? "未知歌手" : artist)
                                .setDurationMs(duration)
                                .setIsPlayable(true)
                                .build())
                        .build();
                songs.add(item);
            }
        } catch (Exception ignored) {}
        if (!songs.isEmpty()) {
            player.setMediaItems(songs);
            player.prepare();
            publishMetadata(0);
        }
    }

    private void publishMetadata(int index) {
        if (songs.isEmpty() || index < 0 || index >= songs.size()) return;
        MediaItem item = songs.get(index);
        String title = String.valueOf(item.mediaMetadata.title);
        String artist = String.valueOf(item.mediaMetadata.artist);
        MediaMetadataCompat md = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                .build();
        session.setMetadata(md);
    }

    private void updateState() {
        int state = player.isPlaying() ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED;
        PlaybackStateCompat ps = new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE |
                        PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_SEEK_TO)
                .setState(state, player.getCurrentPosition(), 1f)
                .build();
        session.setPlaybackState(ps);
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "ERP车机音乐", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
    }

    @Override public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
    }

    @Override public BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints) {
        return new BrowserRoot(ROOT, null);
    }

    @Override public void onLoadChildren(String parentId, Result<List<MediaBrowserCompat.MediaItem>> result) {
        List<MediaBrowserCompat.MediaItem> out = new ArrayList<>();
        if (ROOT.equals(parentId)) {
            for (MediaItem x : songs) {
                MediaDescriptionCompat d = new MediaDescriptionCompat.Builder()
                        .setMediaId(x.mediaId)
                        .setTitle(x.mediaMetadata.title)
                        .setSubtitle(x.mediaMetadata.artist)
                        .build();
                out.add(new MediaBrowserCompat.MediaItem(d, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE));
            }
        }
        result.sendResult(out);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        MediaButtonReceiver.handleIntent(session, intent);
        return START_STICKY;
    }

    @Override public void onDestroy() {
        if (player != null) player.release();
        if (session != null) session.release();
        super.onDestroy();
    }

    @Nullable @Override public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }
}
