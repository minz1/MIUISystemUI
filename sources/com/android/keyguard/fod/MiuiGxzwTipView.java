package com.android.keyguard.fod;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.android.keyguard.KeyguardCompatibilityHelperForO;
import com.android.keyguard.KeyguardCompatibilityHelperForP;
import com.android.systemui.R;
import miui.maml.animation.interpolater.ElasticEaseOutInterpolater;

public class MiuiGxzwTipView extends FrameLayout {
    private float mFontScale;
    private int mGxzwAnimHeight;
    private int mGxzwAnimWidth;
    private int mGxzwIconHeight;
    private int mGxzwIconWidth;
    private int mGxzwIconX;
    private int mGxzwIconY;
    private boolean mKeyguardAuthen;
    private boolean mPortraitOrientation = true;
    private int mScreenHeight;
    private int mScreenWidth;
    private boolean mShowed = false;
    private TextView mTryAgain;

    public MiuiGxzwTipView(Context context) {
        super(context);
        initView();
    }

    private void initView() {
        LayoutInflater.from(getContext()).inflate(R.layout.miui_keyguard_gxzw_tip_view, this);
        this.mTryAgain = (TextView) findViewById(R.id.gxzw_anim_try_again);
        this.mGxzwIconX = MiuiGxzwUtils.GXZW_ICON_X;
        this.mGxzwIconY = MiuiGxzwUtils.GXZW_ICON_Y;
        this.mGxzwIconWidth = MiuiGxzwUtils.GXZW_ICON_WIDTH;
        this.mGxzwIconHeight = MiuiGxzwUtils.GXZW_ICON_HEIGHT;
        this.mGxzwAnimWidth = 1008;
        this.mGxzwAnimHeight = 1008;
        Display display = ((DisplayManager) getContext().getSystemService("display")).getDisplay(0);
        Point point = new Point();
        display.getSize(point);
        this.mScreenWidth = point.x;
        this.mScreenHeight = point.y;
        setSystemUiVisibility(4868);
    }

    public void show() {
        if (!this.mShowed) {
            boolean z = true;
            this.mShowed = true;
            WindowManager wm = (WindowManager) getContext().getSystemService("window");
            if (getContext().getResources().getConfiguration().orientation != 1 && !this.mKeyguardAuthen) {
                z = false;
            }
            this.mPortraitOrientation = z;
            this.mGxzwIconX = MiuiGxzwUtils.GXZW_ICON_X;
            this.mGxzwIconY = MiuiGxzwUtils.GXZW_ICON_Y;
            this.mGxzwIconWidth = MiuiGxzwUtils.GXZW_ICON_WIDTH;
            this.mGxzwIconHeight = MiuiGxzwUtils.GXZW_ICON_HEIGHT;
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(this.mGxzwAnimWidth, this.mGxzwAnimHeight, 2015, 16778776, -2);
            KeyguardCompatibilityHelperForP.setLayoutInDisplayCutoutMode(layoutParams);
            KeyguardCompatibilityHelperForO.setFlag(layoutParams);
            layoutParams.gravity = 51;
            updateLpByOrientation(layoutParams);
            layoutParams.privateFlags |= MiuiGxzwUtils.PRIVATE_FLAG_IS_HBM_OVERLAY;
            layoutParams.setTitle("gxzw_tip");
            if (isAttachedToWindow()) {
                wm.updateViewLayout(this, layoutParams);
            } else if (getParent() == null) {
                wm.addView(this, layoutParams);
            }
            setVisibility(0);
            updateFontScale();
            setAlpha(1.0f);
        }
    }

    public void dismiss() {
        if (this.mShowed) {
            this.mShowed = false;
            WindowManager wm = (WindowManager) getContext().getSystemService("window");
            if (isAttachedToWindow()) {
                wm.removeViewImmediate(this);
            }
            setVisibility(8);
        }
    }

    public void startTipAnim(boolean light, String tip, float translationY) {
        if (!MiuiGxzwManager.getInstance().isBouncer()) {
            this.mTryAgain.setText(tip);
            this.mTryAgain.setVisibility(0);
            this.mTryAgain.setTextColor(light ? -16777216 : -1);
            this.mTryAgain.setTranslationY(translationY);
            new ObjectAnimator();
            ObjectAnimator translationX = ObjectAnimator.ofFloat(this.mTryAgain, "translationX", new float[]{60.0f, 0.0f});
            translationX.setDuration(700);
            translationX.setInterpolator(new ElasticEaseOutInterpolater());
            new ObjectAnimator();
            ObjectAnimator alpha = ObjectAnimator.ofFloat(this.mTryAgain, "alpha", new float[]{0.0f, 1.0f});
            alpha.setDuration(150);
            alpha.setInterpolator(new DecelerateInterpolator());
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(new Animator[]{translationX, alpha});
            animatorSet.start();
        }
    }

    public void stopTipAnim() {
        this.mTryAgain.setVisibility(8);
    }

    public void onKeyguardAuthen(boolean keyguardAuthen) {
        this.mKeyguardAuthen = keyguardAuthen;
        boolean portraitOrientation = true;
        if (getContext().getResources().getConfiguration().orientation != 1 && !this.mKeyguardAuthen) {
            portraitOrientation = false;
        }
        updateOrientation(portraitOrientation);
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        boolean portraitOrientation = true;
        if (newConfig.orientation != 1 && !this.mKeyguardAuthen) {
            portraitOrientation = false;
        }
        updateOrientation(portraitOrientation);
        updateFontScale();
    }

    private void updateFontScale() {
        Configuration configuration = getResources().getConfiguration();
        if (this.mFontScale != configuration.fontScale) {
            this.mTryAgain.setTextSize(0, (float) getResources().getDimensionPixelSize(R.dimen.gxzw_tip_font_size));
            this.mFontScale = configuration.fontScale;
        }
    }

    private void updateOrientation(boolean portraitOrientation) {
        if (portraitOrientation != this.mPortraitOrientation && isAttachedToWindow()) {
            this.mPortraitOrientation = portraitOrientation;
            WindowManager.LayoutParams lp = (WindowManager.LayoutParams) getLayoutParams();
            updateLpByOrientation(lp);
            ((WindowManager) getContext().getSystemService("window")).updateViewLayout(this, lp);
        }
    }

    private void updateLpByOrientation(WindowManager.LayoutParams lp) {
        int height;
        int width;
        int y;
        int x;
        if (this.mPortraitOrientation) {
            x = this.mGxzwIconX - ((this.mGxzwAnimWidth - this.mGxzwIconWidth) / 2);
            y = this.mGxzwIconY - ((this.mGxzwAnimHeight - this.mGxzwIconHeight) / 2);
            width = this.mGxzwAnimWidth;
            height = this.mGxzwAnimHeight;
        } else {
            y = this.mGxzwIconX - ((this.mGxzwAnimWidth - this.mGxzwIconWidth) / 2);
            x = this.mGxzwIconY - ((this.mGxzwAnimHeight - this.mGxzwIconHeight) / 2);
            height = this.mGxzwAnimWidth;
            width = this.mGxzwAnimHeight;
        }
        int rotation = ((DisplayManager) getContext().getSystemService("display")).getDisplay(0).getRotation();
        if (!this.mKeyguardAuthen && (rotation == 2 || rotation == 3)) {
            x = ((this.mPortraitOrientation ? this.mScreenWidth : this.mScreenHeight) - x) - width;
            y = ((this.mPortraitOrientation ? this.mScreenHeight : this.mScreenWidth) - y) - height;
        }
        lp.width = width;
        lp.height = height;
        lp.x = x;
        lp.y = y;
    }
}
