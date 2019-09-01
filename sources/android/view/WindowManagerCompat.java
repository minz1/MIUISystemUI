package android.view;

import android.view.WindowManager;
import java.util.List;

public class WindowManagerCompat {

    public interface IKeyboardShortcutsReceiver {
        void onKeyboardShortcutsReceived(List<KeyboardShortcutGroup> list);
    }

    public static void applySleepToken(boolean dozing, WindowManager.LayoutParams lpChanged) {
        if (dozing) {
            lpChanged.privateFlags |= 2097152;
        } else {
            lpChanged.privateFlags &= -2097153;
        }
    }

    public static void applyExpandedFlag(boolean expanded, WindowManager.LayoutParams lpChanged) {
        if (expanded) {
            lpChanged.privateFlags |= 8388608;
        } else {
            lpChanged.privateFlags &= -8388609;
        }
    }

    public static void requestAppKeyboardShortcuts(WindowManager wm, final IKeyboardShortcutsReceiver receiver, int deviceId) {
        wm.requestAppKeyboardShortcuts(new WindowManager.KeyboardShortcutsReceiver() {
            public void onKeyboardShortcutsReceived(List<KeyboardShortcutGroup> result) {
                IKeyboardShortcutsReceiver.this.onKeyboardShortcutsReceived(result);
            }
        }, deviceId);
    }

    public static void setLayoutInDisplayCutoutMode(WindowManager.LayoutParams lp, int layoutInDisplayCutoutMode) {
        lp.layoutInDisplayCutoutMode = layoutInDisplayCutoutMode;
    }
}
