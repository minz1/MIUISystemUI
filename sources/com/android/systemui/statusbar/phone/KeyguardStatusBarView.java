package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import com.android.keyguard.CarrierText;
import com.android.systemui.BatteryMeterView;
import com.android.systemui.Constants;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.miui.statusbar.phone.MiuiStatusBarPromptController;
import com.android.systemui.statusbar.Icons;
import com.android.systemui.statusbar.NetworkSpeedView;
import com.android.systemui.statusbar.SignalClusterView;
import com.android.systemui.statusbar.StatusBarIconView;
import com.android.systemui.statusbar.policy.DarkIconDispatcher;
import com.android.systemui.statusbar.policy.DarkIconDispatcherHelper;

public class KeyguardStatusBarView extends RelativeLayout implements MiuiStatusBarPromptController.OnPromptStateChangedListener {
    private boolean mBlockClickActionToStatusBar;
    private LinearLayout mCarrierContainer;
    private CarrierText mCarrierLabel;
    private KeyguardStatusBarViewController mController = StatusBarFactory.getInstance().getKeyguardStatusBarViewController();
    private int mDarkModeIconColorSingleTone;
    private int mLightModeIconColorSingleTone;
    private MiuiStatusBarPromptController mStatusBarPrompt;
    public LinearLayout mStatusIcons;
    private LinearLayout mSystemIcons;
    private View mSystemIconsSuperContainer;

    public KeyguardStatusBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mController.init(this);
        this.mSystemIconsSuperContainer = findViewById(R.id.system_icons_super_container);
        this.mCarrierContainer = (LinearLayout) findViewById(R.id.keyguard_carrier_container);
        this.mCarrierLabel = (CarrierText) findViewById(R.id.keyguard_carrier_text);
        this.mSystemIcons = (LinearLayout) findViewById(R.id.system_icons);
        this.mStatusIcons = (LinearLayout) findViewById(R.id.statusIcons);
        if (Constants.IS_NOTCH) {
            ((SignalClusterView) findViewById(R.id.signal_cluster)).setNotchEar();
            ((NetworkSpeedView) this.mSystemIcons.findViewById(R.id.network_speed_view)).setNotch();
            ((BatteryMeterView) findViewById(R.id.battery)).setNortchEar(true);
            updateNotchPromptViewLayout(this.mCarrierContainer);
        }
        this.mController.updateNotchVisible();
        this.mDarkModeIconColorSingleTone = this.mContext.getColor(R.color.dark_mode_icon_color_single_tone);
        this.mLightModeIconColorSingleTone = this.mContext.getColor(R.color.light_mode_icon_color_single_tone);
        this.mStatusBarPrompt = (MiuiStatusBarPromptController) Dependency.get(MiuiStatusBarPromptController.class);
        this.mStatusBarPrompt.addStatusBarPrompt("KeyguardStatusBarView", null, this, 7, this);
        this.mStatusBarPrompt.setPromptSosTypeImage("KeyguardStatusBarView");
    }

    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (visibility != 0) {
            this.mSystemIconsSuperContainer.animate().cancel();
            this.mSystemIconsSuperContainer.setTranslationX(0.0f);
        }
    }

    public boolean hasOverlappingRendering() {
        return false;
    }

    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case 0:
                this.mBlockClickActionToStatusBar = this.mStatusBarPrompt.blockClickAction();
                if (this.mBlockClickActionToStatusBar) {
                    return true;
                }
                break;
            case 1:
                if (this.mBlockClickActionToStatusBar) {
                    this.mStatusBarPrompt.handleClickAction();
                    this.mBlockClickActionToStatusBar = false;
                    return true;
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mController.showStatusIcons();
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mController.hideStatusIcons();
    }

    public void setDarkMode(boolean isDark) {
        int i;
        CarrierText carrierText = this.mCarrierLabel;
        Resources resources = this.mContext.getResources();
        if (isDark) {
            i = R.color.status_bar_textColor_darkmode;
        } else {
            i = R.color.status_bar_textColor;
        }
        carrierText.setTextColor(resources.getColor(i));
        Rect area = new Rect(0, 0, 0, 0);
        float darkIntensity = isDark ? 1.0f : 0.0f;
        int tint = isDark ? this.mDarkModeIconColorSingleTone : this.mLightModeIconColorSingleTone;
        this.mStatusBarPrompt.updateSosImageDark(isDark, area, darkIntensity);
        for (int i2 = 0; i2 < this.mSystemIcons.getChildCount(); i2++) {
            View view = this.mSystemIcons.getChildAt(i2);
            if (view instanceof DarkIconDispatcher.DarkReceiver) {
                ((DarkIconDispatcher.DarkReceiver) view).onDarkChanged(area, darkIntensity, tint);
            }
        }
        setDarkMode(this.mStatusIcons, area, darkIntensity);
        this.mController.setDarkMode(area, darkIntensity, tint);
    }

    public void setDarkMode(ViewGroup viewGroup, Rect area, float darkIntensity) {
        if (viewGroup != null) {
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                if (viewGroup.getChildAt(i) instanceof StatusBarIconView) {
                    StatusBarIconView iconView = (StatusBarIconView) viewGroup.getChildAt(i);
                    iconView.setImageResource(Icons.get(Integer.valueOf(iconView.getStatusBarIcon().icon.getResId()), DarkIconDispatcherHelper.inDarkMode(area, iconView, darkIntensity)));
                }
            }
        }
    }

    private void updateNotchPromptViewLayout(View viewGroup) {
        if (viewGroup != null) {
            boolean center = this.mController.isPromptCenter();
            FrameLayout.LayoutParams mlp = (FrameLayout.LayoutParams) viewGroup.getLayoutParams();
            if ((mlp.gravity == 17) != center) {
                if (center) {
                    mlp.gravity = 17;
                } else {
                    mlp.gravity = 8388627;
                }
                viewGroup.setLayoutParams(mlp);
            }
        }
    }

    public void onPromptStateChanged(boolean isNormalMode, int topState) {
        this.mCarrierLabel.forceHide(!isNormalMode);
    }
}
