package android.view;

import android.view.accessibility.AccessibilityManager;

public class AccessibilityManagerCompat {
    public static boolean isAccessibilityVolumeStreamActive(AccessibilityManager accessibilityManager) {
        return accessibilityManager != null && accessibilityManager.isAccessibilityVolumeStreamActive();
    }
}
