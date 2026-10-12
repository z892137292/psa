package com.erp.carmusic;

import static org.junit.Assert.*;
import android.content.Intent;
import android.media.AudioManager;
import android.media.AudioAttributes;
import static org.robolectric.Shadows.shadowOf;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;
import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
@LooperMode(LooperMode.Mode.PAUSED)
public class MediaDiagnosticServiceTest {
    private ServiceController<MediaDiagnosticService> controller;
    private MediaDiagnosticService service;
    @Before public void setup() {
        controller = Robolectric.buildService(MediaDiagnosticService.class).create();
        service = controller.get();
    }
    @After public void cleanup() { controller.destroy(); }
    private Object field(String name) throws Exception {
        Field f = MediaDiagnosticService.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(service);
    }
    private void send(String action) {
        service.onStartCommand(new Intent(service, MediaDiagnosticService.class).setAction(action), 0, 1);
    }
    @Test public void passiveStartAndRepeatNeverRequestFocus() throws Exception {
        send(MediaDiagnosticService.START);
        send(MediaDiagnosticService.START);
        assertEquals(true, field("recording"));
        assertNull(field("focusRequest"));
        assertFalse(MediaDiagnosticLog.tail(service).contains("FOCUS_PROBE"));
    }
    @Test public void releaseWhileStoppedDoesNotStartObserver() throws Exception {
        send(MediaDiagnosticService.RELEASE_FOCUS);
        assertEquals(false, field("recording"));
        assertNull(field("focusRequest"));
    }
    @Test public void explicitFocusAutomaticallyReleases() throws Exception {
        send(MediaDiagnosticService.FOCUS);
        assertNotNull(field("focusRequest"));
        ShadowLooper.shadowMainLooper().idleFor(11, TimeUnit.SECONDS);
        assertNull(field("focusRequest"));
    }
    @Test public void repeatedProbeRestartsDeadlineAndManualReleaseWorks() throws Exception {
        send(MediaDiagnosticService.FOCUS);
        Object old = field("focusRequest");
        ShadowLooper.shadowMainLooper().idleFor(5, TimeUnit.SECONDS);
        send(MediaDiagnosticService.FOCUS);
        assertNotSame(old, field("focusRequest"));
        ShadowLooper.shadowMainLooper().idleFor(6, TimeUnit.SECONDS);
        assertNotNull(field("focusRequest"));
        send(MediaDiagnosticService.RELEASE_FOCUS);
        assertNull(field("focusRequest"));
    }
    @Test public void destructionCancelsPollAndFocusDeadline() throws Exception {
        send(MediaDiagnosticService.FOCUS);
        service.onDestroy();
        assertEquals(false, field("recording"));
        assertNull(field("focusRequest"));
        String stopped = MediaDiagnosticLog.tail(service);
        ShadowLooper.shadowMainLooper().idleFor(30, TimeUnit.SECONDS);
        assertEquals(stopped, MediaDiagnosticLog.tail(service));
    }
    @Test public void unavailableAudioServiceDoesNotRequestFocusOrCrash() throws Exception {
        Field audio = MediaDiagnosticService.class.getDeclaredField("audio");
        audio.setAccessible(true);
        audio.set(service, null);
        send(MediaDiagnosticService.FOCUS);
        assertNull(field("focusRequest"));
        assertTrue(MediaDiagnosticLog.tail(service).contains("音频服务不可用"));
    }
    @Test public void unavailableSessionInterfaceDegradesToVisibleWarning() throws Exception {
        Field sessions = MediaDiagnosticService.class.getDeclaredField("sessions");
        sessions.setAccessible(true);
        sessions.set(service, null);
        send(MediaDiagnosticService.START);
        ShadowLooper.shadowMainLooper().idle();
        assertTrue(MediaDiagnosticLog.tail(service).contains("媒体会话接口不可用"));
        assertNull(field("focusRequest"));
    }
    @Test public void deniedFocusDoesNotLeaveRequestOrDeadline() throws Exception {
        shadowOf((AudioManager)field("audio")).setNextFocusRequestResponse(AudioManager.AUDIOFOCUS_REQUEST_FAILED);
        send(MediaDiagnosticService.FOCUS);
        assertNull(field("focusRequest"));
        ShadowLooper.shadowMainLooper().idleFor(11, TimeUnit.SECONDS);
        assertNull(field("focusRequest"));
    }
    @Test public void playbackCallbackUsesVisibleAttributesAndLabelsLimits() throws Exception {
        send(MediaDiagnosticService.START);
        shadowOf((AudioManager)field("audio")).setActivePlaybackConfigurationsFor(
                java.util.Collections.singletonList(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA).build()), true);
        ShadowLooper.shadowMainLooper().idle();
        String log = MediaDiagnosticLog.tail(service);
        assertTrue(log.contains("playersVisible=1"));
        assertTrue(log.contains("usage=1"));
        assertTrue(log.contains("权限或接口不可见"));
        assertFalse(log.contains("active="));
        assertNull(field("focusRequest"));
    }
}
