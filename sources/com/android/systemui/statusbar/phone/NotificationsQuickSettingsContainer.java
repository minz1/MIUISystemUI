package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewStub;
import android.widget.FrameLayout;
import com.android.systemui.R;
import com.android.systemui.SystemUI;
import com.android.systemui.fragments.FragmentHostManager;
import com.android.systemui.plugins.qs.QS;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import com.android.systemui.statusbar.policy.OnHeadsUpChangedListener;
import com.miui.systemui.support.v4.app.Fragment;

public class NotificationsQuickSettingsContainer extends FrameLayout implements ViewStub.OnInflateListener, FragmentHostManager.FragmentListener, OnHeadsUpChangedListener {
    private boolean mHeadsUp;
    private HeadsUpManager mHeadsUpManager;
    private boolean mInflated;
    private View mKeyguardStatusBar;
    private QS mQS;
    private boolean mQsExpanded;
    private FrameLayout mQsFrame;
    private View mStackScroller;
    private View mUserSwitcher;

    public NotificationsQuickSettingsContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        setClipChildren(false);
        this.mQsFrame = (FrameLayout) findViewById(R.id.qs_frame);
        this.mStackScroller = findViewById(R.id.notification_stack_scroller);
        this.mKeyguardStatusBar = findViewById(R.id.keyguard_header);
        ViewStub userSwitcher = (ViewStub) findViewById(R.id.keyguard_user_switcher);
        userSwitcher.setOnInflateListener(this);
        this.mUserSwitcher = userSwitcher;
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        FragmentHostManager.get(this).addTagListener(QS.TAG, this);
        this.mHeadsUpManager = ((StatusBar) SystemUI.getComponent(getContext(), StatusBar.class)).mHeadsUpManager;
        this.mHeadsUpManager.addListener(this);
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        this.mHeadsUpManager.removeListener(this);
        FragmentHostManager.get(this).removeTagListener(QS.TAG, this);
        super.onDetachedFromWindow();
    }

    public void updateResources(boolean isThemeChanged) {
        reloadWidth(this.mQsFrame);
        reloadWidth(this.mStackScroller);
    }

    private void reloadWidth(View view) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
        params.width = getContext().getResources().getDimensionPixelSize(R.dimen.notification_panel_width);
        view.setLayoutParams(params);
    }

    /* access modifiers changed from: protected */
    public boolean drawChild(Canvas canvas, View child, long drawingTime) {
        View view;
        View view2;
        boolean statusBarVisible = false;
        boolean userSwitcherVisible = this.mInflated && this.mUserSwitcher.getVisibility() == 0;
        if (this.mKeyguardStatusBar.getVisibility() == 0) {
            statusBarVisible = true;
        }
        boolean qsBottom = this.mHeadsUp;
        View stackQsTop = qsBottom ? this.mStackScroller : this.mQsFrame;
        View stackQsBottom = !qsBottom ? this.mStackScroller : this.mQsFrame;
        if (child == this.mQsFrame) {
            if (userSwitcherVisible && statusBarVisible) {
                view2 = this.mUserSwitcher;
            } else if (statusBarVisible) {
                view2 = this.mKeyguardStatusBar;
            } else if (userSwitcherVisible) {
                view2 = this.mUserSwitcher;
            } else {
                view2 = stackQsBottom;
            }
            return super.drawChild(canvas, view2, drawingTime);
        } else if (child == this.mStackScroller) {
            if (!userSwitcherVisible || !statusBarVisible) {
                view = (statusBarVisible || userSwitcherVisible) ? stackQsBottom : stackQsTop;
            } else {
                view = this.mKeyguardStatusBar;
            }
            return super.drawChild(canvas, view, drawingTime);
        } else if (child == this.mUserSwitcher) {
            return super.drawChild(canvas, (!userSwitcherVisible || !statusBarVisible) ? stackQsTop : stackQsBottom, drawingTime);
        } else if (child == this.mKeyguardStatusBar) {
            return super.drawChild(canvas, stackQsTop, drawingTime);
        } else {
            return super.drawChild(canvas, child, drawingTime);
        }
    }

    public void onInflate(ViewStub stub, View inflated) {
        if (stub == this.mUserSwitcher) {
            this.mUserSwitcher = inflated;
            this.mInflated = true;
        }
    }

    public void onFragmentViewCreated(String tag, Fragment fragment) {
        this.mQS = (QS) fragment;
        this.mQS.setContainer(this);
    }

    public void setQsExpanded(boolean expanded) {
        if (this.mQsExpanded != expanded) {
            this.mQsExpanded = expanded;
            invalidate();
        }
    }

    public void onHeadsUpStateChanged(NotificationData.Entry entry, boolean isHeadsUp) {
        boolean hasHeadsUp = this.mHeadsUpManager.getAllEntries().size() != 0;
        if (this.mHeadsUp != hasHeadsUp) {
            this.mHeadsUp = hasHeadsUp;
            invalidate();
        }
    }

    public void onHeadsUpPinnedModeChanged(boolean inPinnedMode) {
    }

    public void onHeadsUpPinned(ExpandableNotificationRow headsUp) {
    }

    public void onHeadsUpUnPinned(ExpandableNotificationRow headsUp) {
    }

    public void onFragmentViewDestroyed(String tag, Fragment fragment) {
    }
}
