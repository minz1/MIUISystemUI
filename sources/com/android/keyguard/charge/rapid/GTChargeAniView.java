package com.android.keyguard.charge.rapid;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import com.android.systemui.R;
import miui.maml.animation.interpolater.CubicEaseOutInterpolater;

public class GTChargeAniView extends RelativeLayout {
    private AnimatorSet animatorSet;
    private Interpolator cubicEaseOutInterpolator;
    private ImageView mChargeIcon;
    private Drawable mChargeIconDrawable;
    private int mChargeIconHeight;
    private int mChargeIconWidth;
    private Point mScreenSize;
    private ImageView mTailIcon;
    private int mTailIconHeight;
    private int mTailIconWidth;
    private int mTranslation;
    private ImageView mTurboIcon;
    private Drawable mTurboIconDrawable;
    private int mTurboIconHeight;
    private int mTurboIconWidth;
    private Drawable mTurboTailIconDrawable;
    private WindowManager mWindowManager;

    public GTChargeAniView(Context context) {
        this(context, null);
    }

    public GTChargeAniView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GTChargeAniView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.cubicEaseOutInterpolator = new CubicEaseOutInterpolater();
        init(context);
    }

    private void init(Context context) {
        this.mChargeIconDrawable = context.getDrawable(R.drawable.charge_animation_charge_icon);
        this.mTurboIconDrawable = context.getDrawable(R.drawable.charge_animation_turbo_icon);
        this.mTurboTailIconDrawable = context.getDrawable(R.drawable.charge_animation_turbo_tail_icon);
        this.mWindowManager = (WindowManager) context.getSystemService("window");
        this.mScreenSize = new Point();
        this.mWindowManager.getDefaultDisplay().getRealSize(this.mScreenSize);
        updateSizeForScreenSizeChange();
        this.mChargeIcon = new ImageView(context);
        this.mChargeIcon.setImageDrawable(this.mChargeIconDrawable);
        this.mChargeIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(this.mChargeIconWidth, this.mChargeIconHeight);
        rlp.addRule(9);
        addView(this.mChargeIcon, rlp);
        this.mTailIcon = new ImageView(context);
        this.mTailIcon.setId(View.generateViewId());
        this.mTailIcon.setImageDrawable(this.mTurboTailIconDrawable);
        this.mTailIcon.setPivotX((float) this.mTailIconWidth);
        this.mTailIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        RelativeLayout.LayoutParams rlp2 = new RelativeLayout.LayoutParams(this.mTailIconWidth, this.mTailIconHeight);
        rlp2.addRule(9);
        addView(this.mTailIcon, rlp2);
        this.mTurboIcon = new ImageView(context);
        this.mTurboIcon.setImageDrawable(this.mTurboIconDrawable);
        this.mTurboIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        RelativeLayout.LayoutParams rlp3 = new RelativeLayout.LayoutParams(this.mTurboIconWidth, this.mTurboIconHeight);
        rlp3.addRule(1, this.mTailIcon.getId());
        rlp3.leftMargin = (-this.mTurboIconWidth) / 15;
        addView(this.mTurboIcon, rlp3);
        this.mTranslation = this.mTailIconWidth;
    }

    public void setViewInitState() {
        this.mChargeIcon.setAlpha(0.0f);
        this.mTailIcon.setAlpha(1.0f);
        this.mTurboIcon.setAlpha(1.0f);
        this.mTailIcon.setScaleX(1.0f);
        this.mTailIcon.setTranslationX((float) (-this.mTranslation));
        this.mTurboIcon.setTranslationX((float) (-this.mTranslation));
    }

    public void animationToShow() {
        if (this.animatorSet != null) {
            this.animatorSet.cancel();
        }
        setViewInitState();
        PropertyValuesHolder alphaProperty = PropertyValuesHolder.ofFloat(ALPHA, new float[]{0.0f, 1.0f});
        PropertyValuesHolder alphaReverseProperty = PropertyValuesHolder.ofFloat(ALPHA, new float[]{1.0f, 0.0f});
        PropertyValuesHolder scaleReverseProperty = PropertyValuesHolder.ofFloat(SCALE_X, new float[]{1.0f, 0.0f});
        ObjectAnimator chargeIconAnimator = ObjectAnimator.ofPropertyValuesHolder(this.mChargeIcon, new PropertyValuesHolder[]{alphaProperty}).setDuration(300);
        chargeIconAnimator.setInterpolator(this.cubicEaseOutInterpolator);
        PropertyValuesHolder translationProperty = PropertyValuesHolder.ofFloat(TRANSLATION_X, new float[]{(float) (-this.mTranslation), 0.0f});
        ObjectAnimator GTIconMoveInAnimator = ObjectAnimator.ofPropertyValuesHolder(this.mTurboIcon, new PropertyValuesHolder[]{translationProperty}).setDuration(300);
        GTIconMoveInAnimator.setInterpolator(this.cubicEaseOutInterpolator);
        ObjectAnimator GTTailIconMoveInAnimator = ObjectAnimator.ofPropertyValuesHolder(this.mTailIcon, new PropertyValuesHolder[]{translationProperty}).setDuration(300);
        GTTailIconMoveInAnimator.setInterpolator(this.cubicEaseOutInterpolator);
        ObjectAnimator GTTailIconFadeOutAnimator = ObjectAnimator.ofPropertyValuesHolder(this.mTailIcon, new PropertyValuesHolder[]{alphaReverseProperty, scaleReverseProperty}).setDuration(100);
        GTTailIconFadeOutAnimator.setInterpolator(this.cubicEaseOutInterpolator);
        this.animatorSet = new AnimatorSet();
        this.animatorSet.playTogether(new Animator[]{chargeIconAnimator, GTIconMoveInAnimator, GTTailIconMoveInAnimator});
        this.animatorSet.play(GTTailIconFadeOutAnimator).after(GTTailIconMoveInAnimator);
        this.animatorSet.start();
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        checkScreenSize();
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        checkScreenSize();
    }

    private void checkScreenSize() {
        Point point = new Point();
        this.mWindowManager.getDefaultDisplay().getRealSize(point);
        if (!this.mScreenSize.equals(point.x, point.y)) {
            this.mScreenSize.set(point.x, point.y);
            updateSizeForScreenSizeChange();
            updateLayoutParamForScreenSizeChange();
            requestLayout();
        }
    }

    private void updateSizeForScreenSizeChange() {
        float rateWidth = (((float) Math.min(this.mScreenSize.x, this.mScreenSize.y)) * 1.0f) / 1080.0f;
        if (this.mChargeIconDrawable != null) {
            this.mChargeIconWidth = (int) (((float) this.mChargeIconDrawable.getIntrinsicWidth()) * rateWidth);
            this.mChargeIconHeight = (int) (((float) this.mChargeIconDrawable.getIntrinsicHeight()) * rateWidth);
        }
        if (this.mTurboIconDrawable != null) {
            this.mTurboIconWidth = (int) (((float) this.mTurboIconDrawable.getIntrinsicWidth()) * rateWidth);
            this.mTurboIconHeight = (int) (((float) this.mTurboIconDrawable.getIntrinsicHeight()) * rateWidth);
        }
        if (this.mTurboTailIconDrawable != null) {
            this.mTailIconWidth = (int) (((float) this.mTurboTailIconDrawable.getIntrinsicWidth()) * rateWidth);
            this.mTailIconHeight = (int) (((float) this.mTurboTailIconDrawable.getIntrinsicHeight()) * rateWidth);
        }
        this.mTranslation = this.mTailIconWidth;
    }

    private void updateLayoutParamForScreenSizeChange() {
        RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) this.mChargeIcon.getLayoutParams();
        rlp.width = this.mChargeIconWidth;
        rlp.height = this.mChargeIconHeight;
        RelativeLayout.LayoutParams rlp2 = (RelativeLayout.LayoutParams) this.mTailIcon.getLayoutParams();
        rlp2.width = this.mTailIconWidth;
        rlp2.height = this.mTailIconHeight;
        this.mTailIcon.setPivotX((float) this.mTailIconWidth);
        RelativeLayout.LayoutParams rlp3 = (RelativeLayout.LayoutParams) this.mTurboIcon.getLayoutParams();
        rlp3.width = this.mTurboIconWidth;
        rlp3.height = this.mTurboIconHeight;
        rlp3.leftMargin = (-this.mTurboIconWidth) / 15;
    }
}
