package com.erp.carmusic;
import static org.junit.Assert.*;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
@LooperMode(LooperMode.Mode.PAUSED)
public class MediaDiagnosticExportTest {
    Button button(View view, String label) {
        if (view instanceof Button && label.contentEquals(((Button)view).getText())) return (Button)view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup)view;
            for (int i = 0; i < group.getChildCount(); i++) {
                Button found = button(group.getChildAt(i), label);
                if (found != null) return found;
            }
        }
        return null;
    }
    @Test public void sharingCreatesContentUriAndScopedReadGrant() {
        ActivityController<MediaDiagnosticActivity> c = Robolectric.buildActivity(MediaDiagnosticActivity.class).setup();
        try {
            MediaDiagnosticActivity a = c.get();
            button(a.getWindow().getDecorView(), "导出日志").performClick();
            Intent chooser = shadowOf(a).getNextStartedActivity();
            assertNotNull(chooser);
            Intent send = chooser.getParcelableExtra(Intent.EXTRA_INTENT);
            Uri uri = send.getParcelableExtra(Intent.EXTRA_STREAM);
            assertEquals("content", uri.getScheme());
            assertEquals(a.getPackageName() + ".files", uri.getAuthority());
            assertNotEquals(0, send.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION);
            assertEquals(uri, send.getClipData().getItemAt(0).getUri());
        } finally { c.pause().stop().destroy(); }
    }
}
