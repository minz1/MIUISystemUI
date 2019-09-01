package com.android.systemui.statusbar.notification;

import android.util.ArraySet;
import android.view.View;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.policy.OnHeadsUpChangedListener;
import java.util.ArrayList;

public class VisualStabilityManager implements OnHeadsUpChangedListener {
    private ArraySet<View> mAddedChildren = new ArraySet<>();
    private ArraySet<View> mAllowedReorderViews = new ArraySet<>();
    private final ArrayList<Callback> mCallbacks = new ArrayList<>();
    private ArraySet<View> mLowPriorityReorderingViews = new ArraySet<>();
    private boolean mPanelExpanded;
    private boolean mPulsing;
    private boolean mReorderingAllowed;
    private boolean mScreenOn;
    private VisibilityLocationProvider mVisibilityLocationProvider;

    public interface Callback {
        void onReorderingAllowed();
    }

    public void addReorderingAllowedCallback(Callback callback) {
        if (!this.mCallbacks.contains(callback)) {
            this.mCallbacks.add(callback);
        }
    }

    public void setPanelExpanded(boolean expanded) {
        this.mPanelExpanded = expanded;
        updateReorderingAllowed();
    }

    public void setScreenOn(boolean screenOn) {
        this.mScreenOn = screenOn;
        updateReorderingAllowed();
    }

    private void updateReorderingAllowed() {
        boolean changed = false;
        boolean reorderingAllowed = (!this.mScreenOn || !this.mPanelExpanded) && !this.mPulsing;
        if (reorderingAllowed && !this.mReorderingAllowed) {
            changed = true;
        }
        this.mReorderingAllowed = reorderingAllowed;
        if (changed) {
            notifyCallbacks();
        }
    }

    private void notifyCallbacks() {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            this.mCallbacks.get(i).onReorderingAllowed();
        }
        this.mCallbacks.clear();
    }

    public boolean isReorderingAllowed() {
        return this.mReorderingAllowed;
    }

    public boolean canReorderNotification(ExpandableNotificationRow row) {
        if (this.mReorderingAllowed || this.mAddedChildren.contains(row) || this.mLowPriorityReorderingViews.contains(row)) {
            return true;
        }
        if (!this.mAllowedReorderViews.contains(row) || this.mVisibilityLocationProvider.isInVisibleLocation(row)) {
            return false;
        }
        return true;
    }

    public void setVisibilityLocationProvider(VisibilityLocationProvider visibilityLocationProvider) {
        this.mVisibilityLocationProvider = visibilityLocationProvider;
    }

    public void onReorderingFinished() {
        this.mAllowedReorderViews.clear();
        this.mAddedChildren.clear();
        this.mLowPriorityReorderingViews.clear();
    }

    public void onHeadsUpStateChanged(NotificationData.Entry entry, boolean isHeadsUp) {
        if (isHeadsUp) {
            this.mAllowedReorderViews.add(entry.row);
        }
    }

    public void onHeadsUpPinnedModeChanged(boolean inPinnedMode) {
    }

    public void onHeadsUpPinned(ExpandableNotificationRow headsUp) {
    }

    public void onHeadsUpUnPinned(ExpandableNotificationRow headsUp) {
    }

    public void onLowPriorityUpdated(NotificationData.Entry entry) {
        this.mLowPriorityReorderingViews.add(entry.row);
    }

    public void notifyViewAddition(View view) {
        this.mAddedChildren.add(view);
    }
}
