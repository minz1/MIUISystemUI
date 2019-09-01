package com.android.systemui.statusbar.phone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import com.android.systemui.Dependency;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.miui.anim.HideAfterAnimatorListener;
import com.android.systemui.miui.anim.ShowBeforeAnimatorListener;
import com.android.systemui.miui.policy.NotificationsMonitor;
import com.android.systemui.miui.statusbar.phone.MiuiStatusBarPromptController;
import com.android.systemui.statusbar.policy.KeyguardMonitor;

public class NotificationPeekingIconAreaController extends NotificationIconAreaController implements NotificationsMonitor.Callback, MiuiStatusBarPromptController.OnPromptStateChangedListener {
    private View mClockContainer;
    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 100) {
                NotificationPeekingIconAreaController.this.handlePeeking(true);
            } else if (msg.what == 101) {
                NotificationPeekingIconAreaController.this.handlePeeking(false);
            }
        }
    };
    /* access modifiers changed from: private */
    public ViewGroup mNotificationIcons;
    private int mNotificationIconsCount;
    private boolean mPeeking;
    private int mPeekingDuration;
    private int mPeekingSizeHint;
    private boolean mPeekingWithExtraPadding;
    private boolean mPendingPeeking;
    private boolean mShowNotifications;
    private boolean mShowingMiuiPrompts;

    private class UserPresentReceiver extends BroadcastReceiver {
        private UserPresentReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.USER_PRESENT".equals(intent.getAction())) {
                NotificationPeekingIconAreaController.this.firePendingPeeking();
            }
        }
    }

    public NotificationPeekingIconAreaController(Context context, StatusBar statusBar) {
        super(context, statusBar);
        this.mPeekingWithExtraPadding = context.getResources().getBoolean(R.bool.status_bar_notification_icons_peeking_extra_padding);
        this.mPeekingSizeHint = context.getResources().getInteger(R.integer.status_bar_notification_icons_size_hint);
        this.mPeekingDuration = context.getResources().getInteger(R.integer.status_bar_notification_icons_peeking_duration);
        context.registerReceiver(new UserPresentReceiver(), new IntentFilter("android.intent.action.USER_PRESENT"));
        ((MiuiStatusBarPromptController) Dependency.get(MiuiStatusBarPromptController.class)).addPromptStateChangedListener("NotificationPeekingIcon", this);
        ((NotificationsMonitor) Dependency.get(NotificationsMonitor.class)).addCallback(this);
    }

    /* access modifiers changed from: protected */
    public View inflateIconArea(LayoutInflater inflater) {
        View inflateIconArea = super.inflateIconArea(inflater);
        this.mNotificationIcons = (ViewGroup) inflateIconArea.findViewById(R.id.notificationIcons);
        this.mNotificationIcons.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
            public void onChildViewAdded(View parent, View child) {
                NotificationPeekingIconAreaController.this.onIconsChanged();
                NotificationPeekingIconAreaController.this.firePendingPeeking();
            }

            public void onChildViewRemoved(View parent, View child) {
                NotificationPeekingIconAreaController.this.onIconsChanged();
            }
        });
        this.mNotificationIcons.setAlpha(0.0f);
        return inflateIconArea;
    }

    public void setupClockContainer(View clockContainer) {
        super.setupClockContainer(clockContainer);
        this.mClockContainer = clockContainer;
    }

    /* access modifiers changed from: package-private */
    public void setShowNotificationIcon(boolean show) {
        super.setShowNotificationIcon(show);
        boolean oldShow = this.mShowNotifications;
        this.mShowNotifications = show;
        if (show && !oldShow) {
            dispatchPeeking();
        }
        if (oldShow && !show && this.mHandler.hasMessages(com.android.systemui.plugins.R.styleable.AppCompatTheme_textAppearanceSearchResultSubtitle)) {
            this.mHandler.removeMessages(com.android.systemui.plugins.R.styleable.AppCompatTheme_textAppearanceSearchResultSubtitle);
            this.mHandler.sendEmptyMessageDelayed(com.android.systemui.plugins.R.styleable.AppCompatTheme_textAppearanceSearchResultSubtitle, 0);
        }
    }

    private void dispatchPeeking() {
        if (this.mShowNotifications && this.mNotificationIconsCount > 0) {
            if (this.mShowingMiuiPrompts) {
                Log.i("NotificationPeekingIcon", "ignore peeking because of miui prompt showing");
            } else if (((KeyguardMonitor) Dependency.get(KeyguardMonitor.class)).isShowing()) {
                Log.v("NotificationPeekingIcon", "pending peeking because of keyguard showing");
                recordPendingPeeking();
            } else {
                this.mHandler.removeMessages(100);
                this.mHandler.sendEmptyMessageDelayed(100, 10);
                this.mHandler.removeMessages(com.android.systemui.plugins.R.styleable.AppCompatTheme_textAppearanceSearchResultSubtitle);
                this.mHandler.sendEmptyMessageDelayed(com.android.systemui.plugins.R.styleable.AppCompatTheme_textAppearanceSearchResultSubtitle, (long) this.mPeekingDuration);
            }
        }
    }

    /* access modifiers changed from: private */
    public void handlePeeking(boolean peeking) {
        if (peeking && !this.mPeeking) {
            this.mClockContainer.animate().alpha(0.0f).setDuration(160).setStartDelay(0).setListener(new HideAfterAnimatorListener(this.mClockContainer)).setInterpolator(Interpolators.MIUI_ALPHA_OUT).start();
            this.mNotificationIcons.animate().alpha(1.0f).setDuration(160).setStartDelay(160).setInterpolator(Interpolators.MIUI_ALPHA_IN).start();
        } else if (!peeking) {
            this.mNotificationIcons.animate().alpha(0.0f).setDuration(160).setStartDelay(0).setInterpolator(Interpolators.MIUI_ALPHA_OUT).start();
            this.mClockContainer.animate().alpha(1.0f).setDuration(160).setStartDelay(160).setListener(new ShowBeforeAnimatorListener(this.mClockContainer)).setInterpolator(Interpolators.MIUI_ALPHA_IN).start();
        }
        this.mPeeking = peeking;
    }

    /* access modifiers changed from: private */
    public void onIconsChanged() {
        this.mNotificationIconsCount = this.mNotificationIcons.getChildCount();
        if (this.mPeekingWithExtraPadding) {
            this.mNotificationIcons.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                public void onGlobalLayout() {
                    NotificationPeekingIconAreaController.this.mNotificationIcons.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    NotificationPeekingIconAreaController.this.handleExtraPadding();
                }
            });
        }
    }

    /* access modifiers changed from: private */
    public void handleExtraPadding() {
        float iconSize = 0.0f;
        if (this.mNotificationIconsCount > 0) {
            iconSize = (float) this.mNotificationIcons.getChildAt(0).getMeasuredWidth();
        }
        this.mNotificationIcons.setPaddingRelative((int) ((Math.max(0.0f, (float) (this.mPeekingSizeHint - this.mNotificationIconsCount)) * iconSize) / 2.0f), 0, 0, 0);
    }

    private void recordPendingPeeking() {
        this.mPendingPeeking = true;
    }

    /* access modifiers changed from: private */
    public void firePendingPeeking() {
        if (this.mPendingPeeking) {
            this.mPendingPeeking = false;
            dispatchPeeking();
        }
    }

    public void onPromptStateChanged(boolean isNormalMode, int topState) {
        this.mShowingMiuiPrompts = !isNormalMode;
        if (this.mShowingMiuiPrompts) {
            this.mHandler.removeMessages(100);
            if (this.mPeeking) {
                this.mHandler.removeMessages(com.android.systemui.plugins.R.styleable.AppCompatTheme_textAppearanceSearchResultSubtitle);
                this.mHandler.sendEmptyMessage(com.android.systemui.plugins.R.styleable.AppCompatTheme_textAppearanceSearchResultSubtitle);
                Log.d("NotificationPeekingIcon", "prompt showing, end peeking immediately");
            }
        }
    }

    public void onNotificationAdded(StatusBarNotification entry) {
    }

    public void onNotificationArrived(StatusBarNotification entry) {
        recordPendingPeeking();
    }

    public void onNotificationUpdated(StatusBarNotification entry) {
    }
}
