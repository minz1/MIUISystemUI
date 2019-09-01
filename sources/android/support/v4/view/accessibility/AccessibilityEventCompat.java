package android.support.v4.view.accessibility;

import android.os.Build;
import android.view.accessibility.AccessibilityEvent;

public final class AccessibilityEventCompat {
    public static void setContentChangeTypes(AccessibilityEvent event, int changeTypes) {
        if (Build.VERSION.SDK_INT >= 19) {
            event.setContentChangeTypes(changeTypes);
        }
    }

    public static int getContentChangeTypes(AccessibilityEvent event) {
        if (Build.VERSION.SDK_INT >= 19) {
            return event.getContentChangeTypes();
        }
        return 0;
    }
}
