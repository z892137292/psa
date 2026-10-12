package com.erp.carmusic;

import android.content.Context;
import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Local, bounded, metadata-only diagnostic log. Never records sound or notifications. */
public final class MediaDiagnosticLog {
    private static final Object LOCK = new Object();
    private static final long MAX_SIZE = 1024 * 1024;
    private MediaDiagnosticLog() {}

    public static File file(Context context) {
        return new File(context.getFilesDir(), "media-diagnostic.txt");
    }

    public static void record(Context context, String category, String detail) {
        synchronized (LOCK) {
            try {
                File target = file(context);
                if (target.length() >= MAX_SIZE) {
                    // Bound storage use; events in this new segment remain exportable.
                    try (FileOutputStream reset = new FileOutputStream(target, false)) {
                        reset.write("# Log rotated at 1 MiB; oldest events discarded.\n".getBytes(StandardCharsets.UTF_8));
                    }
                }
                String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(new Date());
                String line = timestamp + " [" + category + "] " + detail.replace('\n', ' ').replace('\r', ' ') + "\n";
                try (FileOutputStream out = new FileOutputStream(target, true)) {
                    out.write(line.getBytes(StandardCharsets.UTF_8));
                }
            } catch (Exception ignored) {
                // Diagnostics cannot interfere with car audio.
            }
        }
    }

    public static String tail(Context context) {
        synchronized (LOCK) {
            File f = file(context);
            if (!f.exists()) return "还没有记录。先点击「开始后台记录」。";
            try (RandomAccessFile in = new RandomAccessFile(f, "r")) {
                long offset = Math.max(0, in.length() - 18000);
                in.seek(offset);
                byte[] bytes = new byte[(int)(in.length() - offset)];
                in.readFully(bytes);
                return new String(bytes, StandardCharsets.UTF_8);
            } catch (Exception e) {
                return "读取日志失败：" + e.getClass().getSimpleName();
            }
        }
    }
}
