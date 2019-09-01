package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.view.accessibility.AccessibilityManager;
import java.util.ArrayList;

public class AccessibilityController implements AccessibilityManager.AccessibilityStateChangeListener, AccessibilityManager.TouchExplorationStateChangeListener {
    private boolean mAccessibilityEnabled;
    private final ArrayList<AccessibilityStateChangedCallback> mChangeCallbacks = new ArrayList<>();
    private boolean mTouchExplorationEnabled;

    public interface AccessibilityStateChangedCallback {
        void onStateChanged(boolean z, boolean z2);
    }

    public AccessibilityController(Context context) {
        AccessibilityManager am = (AccessibilityManager) context.getSystemService("accessibility");
        am.addTouchExplorationStateChangeListener(this);
        am.addAccessibilityStateChangeListener(this);
        this.mAccessibilityEnabled = am.isEnabled();
        this.mTouchExplorationEnabled = am.isTouchExplorationEnabled();
    }

    public void addStateChangedCallback(AccessibilityStateChangedCallback cb) {
        this.mChangeCallbacks.add(cb);
        cb.onStateChanged(this.mAccessibilityEnabled, this.mTouchExplorationEnabled);
    }

    public void removeStateChangedCallback(AccessibilityStateChangedCallback cb) {
        this.mChangeCallbacks.remove(cb);
    }

    private void fireChanged() {
        int N = this.mChangeCallbacks.size();
        for (int i = 0; i < N; i++) {
            this.mChangeCallbacks.get(i).onStateChanged(this.mAccessibilityEnabled, this.mTouchExplorationEnabled);
        }
    }

    public void onAccessibilityStateChanged(boolean enabled) {
        this.mAccessibilityEnabled = enabled;
        fireChanged();
    }

    public void onTouchExplorationStateChanged(boolean enabled) {
        this.mTouchExplorationEnabled = enabled;
        fireChanged();
    }
}
