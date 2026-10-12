package com.erp.carmusic;

import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class MediaDiagnosticLogTest {
    @Rule public TemporaryFolder tmp = new TemporaryFolder();
    @Test public void missingLogIsSafe() { assertTrue(MediaDiagnosticLog.tail(new File(tmp.getRoot(), "absent")).contains("还没有记录")); }
    @Test public void newlinesAndNullAreSafe() throws Exception {
        File log = new File(tmp.getRoot(), "sub/log.txt");
        MediaDiagnosticLog.record(log, "A\nB", "one\r\ntwo");
        MediaDiagnosticLog.record(log, null, null);
        String s = new String(Files.readAllBytes(log.toPath()), StandardCharsets.UTF_8);
        assertEquals(2, s.split("\n").length);
        assertTrue(s.contains("[A B]"));
        assertTrue(s.contains("unavailable"));
    }
    @Test public void storageAndUtf8TailRemainBounded() throws Exception {
        File log = new File(tmp.getRoot(), "log.txt");
        String text = String.join("", java.util.Collections.nCopies(2000, "中文测试"));
        for (int i = 0; i < 120; i++) MediaDiagnosticLog.record(log, "PLAYBACK", i + text);
        assertTrue(log.length() <= 1024 * 1024);
        String tail = MediaDiagnosticLog.tail(log);
        assertFalse(tail.contains("\ufffd"));
        assertTrue(tail.contains("119中文测试"));
        assertTrue(new String(Files.readAllBytes(log.toPath()), StandardCharsets.UTF_8).contains("rotated"));
    }
    @Test public void oversizedEventCannotExceedStorageCap() {
        File log = new File(tmp.getRoot(), "log.txt");
        MediaDiagnosticLog.record(log, "BIG", String.join("", java.util.Collections.nCopies(2000000, "中")));
        assertTrue(log.length() < 30000);
        assertTrue(MediaDiagnosticLog.tail(log).contains("[truncated]"));
    }
}
