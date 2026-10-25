package com.cleanyourtv.launcher;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;

/**
 * Fire OS keeps Amazon's launcher as the system home. When that launcher
 * comes to the front, this service sends our home screen on top instead.
 * Turn it off to get Amazon's home back (see README).
 */
public class HomeRedirectService extends AccessibilityService {

    private static final String AMAZON_LAUNCHER = "com.amazon.tv.launcher";
    private long lastRedirect = 0;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return;
        }
        CharSequence pkg = event.getPackageName();
        if (pkg == null || !AMAZON_LAUNCHER.contentEquals(pkg)) {
            return;
        }
        long now = SystemClock.uptimeMillis();
        if (now - lastRedirect < 1000) {
            return;
        }
        lastRedirect = now;
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
    }

    @Override
    public void onInterrupt() {
    }
}
