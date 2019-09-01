package android.content;

import android.os.Build;

public class IntentCompat {
    public static final String ACTION_DISMISS_KEYBOARD_SHORTCUTS;
    public static final String ACTION_SHOW_KEYBOARD_SHORTCUTS;

    static {
        String str;
        String str2;
        if (Build.VERSION.SDK_INT >= 26) {
            str = "com.android.intent.action.SHOW_KEYBOARD_SHORTCUTS";
        } else {
            str = "android.intent.action.SHOW_KEYBOARD_SHORTCUTS";
        }
        ACTION_SHOW_KEYBOARD_SHORTCUTS = str;
        if (Build.VERSION.SDK_INT >= 26) {
            str2 = "com.android.intent.action.DISMISS_KEYBOARD_SHORTCUTS";
        } else {
            str2 = "android.intent.action.DISMISS_KEYBOARD_SHORTCUTS";
        }
        ACTION_DISMISS_KEYBOARD_SHORTCUTS = str2;
    }
}
