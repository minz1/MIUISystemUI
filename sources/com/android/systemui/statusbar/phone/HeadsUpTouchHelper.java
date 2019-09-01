package com.android.systemui.statusbar.phone;

import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.ExpandableView;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import com.android.systemui.statusbar.stack.NotificationStackScrollLayout;

public class HeadsUpTouchHelper {
    private boolean mCollapseSnoozes;
    private HeadsUpManager mHeadsUpManager;
    private float mInitialTouchX;
    private float mInitialTouchY;
    private NotificationPanelView mPanel;
    private ExpandableNotificationRow mPickedChild;
    private NotificationStackScrollLayout mStackScroller;
    private StatusBar mStatusBar;
    private float mTouchSlop;
    private boolean mTouchingHeadsUpView;
    private boolean mTrackingHeadsUp;
    private int mTrackingPointer;

    public HeadsUpTouchHelper(HeadsUpManager headsUpManager, NotificationStackScrollLayout stackScroller, NotificationPanelView notificationPanelView, StatusBar statusBar) {
        this.mHeadsUpManager = headsUpManager;
        this.mStackScroller = stackScroller;
        this.mPanel = notificationPanelView;
        this.mStatusBar = statusBar;
        this.mTouchSlop = (float) ViewConfiguration.get(stackScroller.getContext()).getScaledTouchSlop();
    }

    public boolean isTrackingHeadsUp() {
        return this.mTrackingHeadsUp;
    }

    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (!this.mTouchingHeadsUpView && event.getActionMasked() != 0) {
            return false;
        }
        int pointerIndex = event.findPointerIndex(this.mTrackingPointer);
        if (pointerIndex < 0) {
            pointerIndex = 0;
            this.mTrackingPointer = event.getPointerId(0);
        }
        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);
        int actionMasked = event.getActionMasked();
        boolean z = true;
        if (actionMasked != 6) {
            switch (actionMasked) {
                case 0:
                    this.mInitialTouchY = y;
                    this.mInitialTouchX = x;
                    setTrackingHeadsUp(false);
                    ExpandableView child = this.mStackScroller.getChildAtRawPosition(x, y);
                    this.mTouchingHeadsUpView = false;
                    if (child instanceof ExpandableNotificationRow) {
                        this.mPickedChild = (ExpandableNotificationRow) child;
                        if (this.mStackScroller.isExpanded() || !this.mPickedChild.isHeadsUp() || !this.mPickedChild.isPinned()) {
                            z = false;
                        }
                        this.mTouchingHeadsUpView = z;
                        break;
                    }
                    break;
                case 1:
                case 3:
                    if (this.mPickedChild == null || !this.mTouchingHeadsUpView || !this.mHeadsUpManager.shouldSwallowClick(this.mPickedChild.getStatusBarNotification().getKey())) {
                        endMotion();
                        break;
                    } else {
                        endMotion();
                        return true;
                    }
                case 2:
                    float h = y - this.mInitialTouchY;
                    if (this.mTouchingHeadsUpView && Math.abs(h) > this.mTouchSlop && Math.abs(h) > Math.abs(x - this.mInitialTouchX) && h < 0.0f) {
                        this.mTrackingHeadsUp = true;
                        this.mStatusBar.showReturnToInCallScreenButtonIfNeed();
                        this.mHeadsUpManager.removeNotification(this.mPickedChild.getEntry().key, true);
                        this.mTouchingHeadsUpView = false;
                        this.mHeadsUpManager.unpinAll();
                        return true;
                    }
            }
        } else {
            int upPointer = event.getPointerId(event.getActionIndex());
            if (this.mTrackingPointer == upPointer) {
                if (event.getPointerId(0) != upPointer) {
                    z = false;
                }
                int newIndex = z;
                this.mTrackingPointer = event.getPointerId((int) newIndex);
                this.mInitialTouchX = event.getX(newIndex);
                this.mInitialTouchY = event.getY(newIndex);
            }
        }
        return false;
    }

    private void setTrackingHeadsUp(boolean tracking) {
        if (this.mTrackingHeadsUp != tracking) {
            Log.d("HeadsUpTouchHelper", "setTrackingHeadsUp tracking=" + tracking);
        }
        this.mTrackingHeadsUp = tracking;
        this.mHeadsUpManager.setTrackingHeadsUp(tracking);
        this.mPanel.setTrackingHeadsUp(tracking);
    }

    public void notifyFling(boolean collapse) {
        if (collapse && this.mCollapseSnoozes) {
            this.mHeadsUpManager.snooze();
        }
        this.mCollapseSnoozes = false;
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (!this.mTrackingHeadsUp) {
            return false;
        }
        int actionMasked = event.getActionMasked();
        if (actionMasked == 1 || actionMasked == 3) {
            endMotion();
            setTrackingHeadsUp(false);
        }
        return true;
    }

    private void endMotion() {
        this.mTrackingPointer = -1;
        this.mPickedChild = null;
        this.mTouchingHeadsUpView = false;
    }
}
