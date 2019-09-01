package com.android.systemui.statusbar.policy;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.widget.FrameLayout;
import com.android.systemui.Dependency;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.statusbar.ScrimView;
import com.android.systemui.statusbar.phone.StatusBarWindowManager;
import com.android.systemui.statusbar.phone.StatusBarWindowView;
import com.android.systemui.statusbar.stack.NotificationStackScrollLayout;

public class BrightnessMirrorController {
    public long TRANSITION_DURATION_IN = 200;
    public long TRANSITION_DURATION_OUT = 150;
    /* access modifiers changed from: private */
    public View mBrightnessMirror;
    private final int[] mInt2Cache = new int[2];
    private FrameLayout mMirrorContent;
    private final View mNotificationPanel;
    private final View mNotificationsQuickSettingsContainer;
    /* access modifiers changed from: private */
    public View mQSBrightness;
    private final ScrimView mScrimBehind;
    /* access modifiers changed from: private */
    public final NotificationStackScrollLayout mStackScroller;
    private final StatusBarWindowView mStatusBarWindow;

    public BrightnessMirrorController(StatusBarWindowView statusBarWindow) {
        this.mStatusBarWindow = statusBarWindow;
        this.mScrimBehind = (ScrimView) statusBarWindow.findViewById(R.id.scrim_behind);
        this.mBrightnessMirror = statusBarWindow.findViewById(R.id.brightness_mirror);
        this.mMirrorContent = (FrameLayout) this.mBrightnessMirror.findViewById(R.id.mirror_content);
        this.mNotificationPanel = statusBarWindow.findViewById(R.id.notification_panel);
        this.mNotificationsQuickSettingsContainer = this.mNotificationPanel.findViewById(R.id.notification_container_parent);
        this.mStackScroller = (NotificationStackScrollLayout) statusBarWindow.findViewById(R.id.notification_stack_scroller);
    }

    public void showMirror() {
        if (this.mQSBrightness == null) {
            this.mQSBrightness = this.mNotificationPanel.findViewById(R.id.qs_brightness);
        }
        this.mQSBrightness.setVisibility(4);
        this.mBrightnessMirror.setVisibility(0);
        this.mStackScroller.setFadingOut(true);
        this.mScrimBehind.animateViewAlpha(0.0f, this.TRANSITION_DURATION_OUT, Interpolators.ALPHA_OUT);
        outAnimation(this.mNotificationsQuickSettingsContainer.animate()).withLayer().withEndAction(new Runnable() {
            public void run() {
                ((StatusBarWindowManager) Dependency.get(StatusBarWindowManager.class)).toggleBlurBackgroundByBrightnessMirror(false);
            }
        });
    }

    public void hideMirror() {
        this.mScrimBehind.animateViewAlpha(1.0f, this.TRANSITION_DURATION_IN, Interpolators.ALPHA_IN);
        inAnimation(this.mNotificationsQuickSettingsContainer.animate()).withLayer().withEndAction(new Runnable() {
            public void run() {
                BrightnessMirrorController.this.mQSBrightness.setVisibility(0);
                BrightnessMirrorController.this.mBrightnessMirror.setVisibility(4);
                BrightnessMirrorController.this.mStackScroller.setFadingOut(false);
            }
        });
        ((StatusBarWindowManager) Dependency.get(StatusBarWindowManager.class)).toggleBlurBackgroundByBrightnessMirror(true);
    }

    private ViewPropertyAnimator outAnimation(ViewPropertyAnimator a) {
        return a.alpha(0.0f).setDuration(this.TRANSITION_DURATION_OUT).setInterpolator(Interpolators.MIUI_ALPHA_OUT).withEndAction(null);
    }

    private ViewPropertyAnimator inAnimation(ViewPropertyAnimator a) {
        return a.alpha(1.0f).setDuration(this.TRANSITION_DURATION_IN).setInterpolator(Interpolators.MIUI_ALPHA_IN);
    }

    public void setLocation(View original) {
        original.getLocationInWindow(this.mInt2Cache);
        int originalX = this.mInt2Cache[0] + (original.getWidth() / 2);
        int originalY = this.mInt2Cache[1] + (original.getHeight() / 2);
        this.mBrightnessMirror.setTranslationX(0.0f);
        this.mBrightnessMirror.setTranslationY(0.0f);
        this.mMirrorContent.getLocationInWindow(this.mInt2Cache);
        int mirrorX = this.mInt2Cache[0] + (this.mMirrorContent.getWidth() / 2);
        int mirrorY = this.mInt2Cache[1] + (this.mMirrorContent.getHeight() / 2);
        this.mBrightnessMirror.setTranslationX((float) (originalX - mirrorX));
        this.mBrightnessMirror.setTranslationY((float) (originalY - mirrorY));
    }

    public View getMirror() {
        return this.mBrightnessMirror;
    }

    public void updateResources() {
        Resources res = this.mBrightnessMirror.getResources();
        int gravity = res.getInteger(R.integer.notification_panel_layout_gravity);
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) this.mBrightnessMirror.getLayoutParams();
        if (lp.gravity != gravity) {
            lp.gravity = gravity;
            this.mBrightnessMirror.setLayoutParams(lp);
        }
        FrameLayout.LayoutParams contentLp = (FrameLayout.LayoutParams) this.mMirrorContent.getLayoutParams();
        int contentWidth = res.getDimensionPixelSize(R.dimen.notification_panel_width);
        if (contentLp.width != contentWidth) {
            contentLp.width = contentWidth;
            this.mMirrorContent.setLayoutParams(contentLp);
        }
        if (res.getConfiguration().orientation == 1) {
            this.mBrightnessMirror.setBackgroundResource(R.drawable.brightness_mirror_bg);
        } else {
            this.mBrightnessMirror.setBackgroundResource(R.drawable.brightness_mirror_bg_land);
        }
    }

    public void onDensityOrFontScaleChanged() {
        int index = this.mStatusBarWindow.indexOfChild(this.mBrightnessMirror);
        this.mStatusBarWindow.removeView(this.mBrightnessMirror);
        this.mBrightnessMirror = LayoutInflater.from(this.mBrightnessMirror.getContext()).inflate(R.layout.brightness_mirror, this.mStatusBarWindow, false);
        this.mStatusBarWindow.addView(this.mBrightnessMirror, index);
        this.mMirrorContent = (FrameLayout) this.mBrightnessMirror.findViewById(R.id.mirror_content);
    }
}
