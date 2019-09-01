package com.android.keyguard.charge.rapid;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.android.systemui.R;
import miui.maml.animation.interpolater.CubicEaseOutInterpolater;
import miui.maml.animation.interpolater.QuartEaseOutInterpolater;

public class RapidChargeView extends FrameLayout implements Animator.AnimatorListener, ValueAnimator.AnimatorUpdateListener {
    /* access modifiers changed from: private */
    public IRapidAnimationListener animationListener;
    private Drawable mBottomLightDrawable;
    private int mBottomLightHeight;
    private ImageView mBottomLightImage;
    private int mBottomLightWidth;
    private View mCenterAnchorView;
    private int mChargeNumberTranslateInit;
    private int mChargeNumberTranslateSmall;
    private int mChargeState;
    private int mChargeTipTranslateSmall;
    private ViewGroup mContentContainer;
    private AnimatorSet mContentSwitchAnimator;
    private Interpolator mCubicInterpolator;
    private AnimatorSet mDismissAnimatorSet;
    /* access modifiers changed from: private */
    public final Runnable mDismissRunnable;
    private AnimatorSet mEnterAnimatorSet;
    /* access modifiers changed from: private */
    public FireworksView mFireworksView;
    /* access modifiers changed from: private */
    public GTChargeAniView mGtChargeAniView;
    /* access modifiers changed from: private */
    public Handler mHandler;
    private int mIconPaddingTop;
    /* access modifiers changed from: private */
    public ObjectAnimator mInnerCircleAnimator;
    private Drawable mInnerCircleDrawable;
    private int mInnerCircleSize;
    private ImageView mInnerCircleView;
    private int mInnerParticleCircleSize;
    private Drawable mInnerParticleDrawable;
    /* access modifiers changed from: private */
    public boolean mIsScreenOn;
    private OutlineView mOutlineView;
    /* access modifiers changed from: private */
    public ObjectAnimator mParticleCircleAnimator;
    private ImageView mParticleCircleView;
    private PercentCountView mPercentCountView;
    private int mPivotX;
    private Interpolator mQuartOutInterpolator;
    private ImageView mRapidIcon;
    private Drawable mRapidIconDrawable;
    private int mRapidIconHeight;
    private int mRapidIconWidth;
    private Point mScreenSize;
    private int mSpaceHeight;
    private int mSpeedTipTextSizePx;
    /* access modifiers changed from: private */
    public boolean mStartingDismissWirelessAlphaAnim;
    private TextView mStateTip;
    private ImageView mSuperRapidIcon;
    private Drawable mSuperRapidIconDrawable;
    private int mSuperRapidIconHeight;
    private int mSuperRapidIconWidth;
    private int mTipTopMargin;
    private WindowManager mWindowManager;
    private boolean mWindowShouldAdd;
    private Runnable timeoutDismissJob;

    public RapidChargeView(Context context) {
        this(context, null);
    }

    public RapidChargeView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RapidChargeView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mHandler = new Handler();
        this.mCubicInterpolator = new CubicEaseOutInterpolater();
        this.mQuartOutInterpolator = new QuartEaseOutInterpolater();
        this.mDismissRunnable = new Runnable() {
            public void run() {
                RapidChargeView.this.mFireworksView.stop();
                RapidChargeView.this.mInnerCircleAnimator.cancel();
                RapidChargeView.this.mParticleCircleAnimator.cancel();
                RapidChargeView.this.setComponentTransparent(true);
                RapidChargeView.this.disableTouch(true);
                if (RapidChargeView.this.mIsScreenOn) {
                    RapidChargeView.this.removeFromWindow("dismiss");
                }
                if (RapidChargeView.this.animationListener != null) {
                    RapidChargeView.this.animationListener.onRapidAnimationDismiss(11);
                }
            }
        };
        this.timeoutDismissJob = new Runnable() {
            public void run() {
                RapidChargeView.this.startDismiss("dismiss_for_timeout");
            }
        };
        init(context);
    }

    private void init(Context context) {
        this.mRapidIconDrawable = context.getDrawable(R.drawable.charge_animation_rapid_charge_icon);
        this.mSuperRapidIconDrawable = context.getDrawable(R.drawable.charge_animation_super_rapid_icon);
        this.mInnerCircleDrawable = context.getDrawable(R.drawable.charge_animation_wired_rotate_circle_icon);
        this.mInnerParticleDrawable = context.getDrawable(R.drawable.charge_animation_particle_circle_icon);
        this.mBottomLightDrawable = context.getDrawable(R.drawable.charge_animation_bottom_light_icon);
        this.mWindowManager = (WindowManager) context.getSystemService("window");
        this.mScreenSize = new Point();
        this.mWindowManager.getDefaultDisplay().getRealSize(this.mScreenSize);
        updateSizeForScreenSizeChange();
        this.mChargeState = 0;
        setBackgroundColor(Color.argb(242, 0, 0, 0));
        hideSystemUI();
        FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(-2, -2);
        flp.gravity = 81;
        this.mOutlineView = new OutlineView(context);
        addView(this.mOutlineView, flp);
        FrameLayout.LayoutParams flp2 = new FrameLayout.LayoutParams(-2, -2);
        flp2.gravity = 81;
        this.mFireworksView = new FireworksView(context);
        addView(this.mFireworksView, flp2);
        this.mContentContainer = new RelativeLayout(context);
        this.mCenterAnchorView = new TextView(context);
        RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(-2, this.mSpaceHeight);
        rlp.addRule(13);
        this.mCenterAnchorView.setId(View.generateViewId());
        this.mContentContainer.addView(this.mCenterAnchorView, rlp);
        RelativeLayout.LayoutParams rlp2 = new RelativeLayout.LayoutParams(-2, -2);
        rlp2.addRule(13);
        this.mPercentCountView = new PercentCountView(context);
        this.mPercentCountView.setTranslationY((float) this.mChargeNumberTranslateInit);
        this.mContentContainer.addView(this.mPercentCountView, rlp2);
        this.mStateTip = new TextView(context);
        this.mStateTip.setTextSize(0, (float) this.mSpeedTipTextSizePx);
        this.mStateTip.setIncludeFontPadding(false);
        this.mStateTip.setTextColor(Color.parseColor("#8CFFFFFF"));
        this.mStateTip.setGravity(17);
        this.mStateTip.setText(getResources().getString(R.string.rapid_charge_mode_tip));
        RelativeLayout.LayoutParams rlp3 = new RelativeLayout.LayoutParams(-2, -2);
        rlp3.addRule(14);
        rlp3.addRule(3, this.mCenterAnchorView.getId());
        rlp3.topMargin = this.mTipTopMargin;
        this.mContentContainer.addView(this.mStateTip, rlp3);
        this.mGtChargeAniView = new GTChargeAniView(context);
        RelativeLayout.LayoutParams rlp4 = new RelativeLayout.LayoutParams(-2, -2);
        rlp4.addRule(14);
        rlp4.addRule(3, this.mCenterAnchorView.getId());
        rlp4.topMargin = this.mTipTopMargin;
        this.mGtChargeAniView.setVisibility(8);
        this.mGtChargeAniView.setViewInitState();
        this.mContentContainer.addView(this.mGtChargeAniView, rlp4);
        this.mRapidIcon = new ImageView(context);
        this.mRapidIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        this.mRapidIcon.setImageDrawable(this.mRapidIconDrawable);
        RelativeLayout.LayoutParams rlp5 = new RelativeLayout.LayoutParams(this.mRapidIconWidth, this.mRapidIconHeight + this.mIconPaddingTop);
        rlp5.addRule(13);
        this.mRapidIcon.setPadding(0, this.mIconPaddingTop, 0, 0);
        this.mRapidIcon.setPivotX((float) this.mPivotX);
        this.mContentContainer.addView(this.mRapidIcon, rlp5);
        this.mSuperRapidIcon = new ImageView(context);
        this.mSuperRapidIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        this.mSuperRapidIcon.setImageDrawable(this.mSuperRapidIconDrawable);
        RelativeLayout.LayoutParams rlp6 = new RelativeLayout.LayoutParams(this.mSuperRapidIconWidth, this.mSuperRapidIconHeight + this.mIconPaddingTop);
        rlp6.addRule(13);
        this.mSuperRapidIcon.setPadding(0, this.mIconPaddingTop, 0, 0);
        this.mSuperRapidIcon.setPivotX((float) this.mPivotX);
        this.mContentContainer.addView(this.mSuperRapidIcon, rlp6);
        addView(this.mContentContainer, new FrameLayout.LayoutParams(-1, -1));
        this.mInnerCircleView = new ImageView(context);
        this.mInnerCircleView.setScaleType(ImageView.ScaleType.FIT_XY);
        this.mInnerCircleView.setImageDrawable(this.mInnerCircleDrawable);
        FrameLayout.LayoutParams flp3 = new FrameLayout.LayoutParams(this.mInnerCircleSize, this.mInnerCircleSize);
        flp3.gravity = 17;
        this.mInnerCircleView.setLayoutParams(flp3);
        addView(this.mInnerCircleView);
        this.mInnerCircleAnimator = ObjectAnimator.ofFloat(this.mInnerCircleView, View.ROTATION, new float[]{0.0f, 360.0f});
        this.mInnerCircleAnimator.setInterpolator(new LinearInterpolator());
        this.mInnerCircleAnimator.setRepeatCount(-1);
        this.mInnerCircleAnimator.setDuration(6000);
        this.mParticleCircleView = new ImageView(context);
        this.mParticleCircleView.setScaleType(ImageView.ScaleType.FIT_XY);
        this.mParticleCircleView.setImageDrawable(this.mInnerParticleDrawable);
        FrameLayout.LayoutParams flp4 = new FrameLayout.LayoutParams(this.mInnerParticleCircleSize, this.mInnerParticleCircleSize);
        flp4.gravity = 17;
        this.mParticleCircleView.setLayoutParams(flp4);
        addView(this.mParticleCircleView);
        this.mParticleCircleAnimator = ObjectAnimator.ofFloat(this.mParticleCircleView, View.ROTATION, new float[]{0.0f, 360.0f});
        this.mParticleCircleAnimator.setInterpolator(new LinearInterpolator());
        this.mParticleCircleAnimator.setRepeatCount(-1);
        this.mParticleCircleAnimator.setDuration(1000);
        FrameLayout.LayoutParams flp5 = new FrameLayout.LayoutParams(this.mBottomLightWidth, this.mBottomLightHeight);
        flp5.gravity = 81;
        this.mBottomLightImage = new ImageView(context);
        this.mBottomLightImage.setImageDrawable(this.mBottomLightDrawable);
        addView(this.mBottomLightImage, flp5);
        setComponentTransparent(true);
    }

    private void setChargeState(int state) {
        if (state != this.mChargeState) {
            Log.i("RapidChargeView", "setChargeState: " + state);
            this.mChargeState = state;
            post(new Runnable() {
                public void run() {
                    RapidChargeView.this.startContentSwitchAnimation();
                }
            });
        }
    }

    public void setChargeState(boolean rapid, boolean superRapid) {
        int chargeState;
        if (superRapid) {
            chargeState = 2;
        } else if (rapid) {
            chargeState = 1;
        } else {
            chargeState = 0;
        }
        setChargeState(chargeState);
    }

    /* access modifiers changed from: private */
    public void startContentSwitchAnimation() {
        switch (this.mChargeState) {
            case 0:
                switchToNormal();
                return;
            case 1:
                switchToRapid();
                return;
            case 2:
                switchToSuperRapid();
                return;
            default:
                return;
        }
    }

    private void switchToNormal() {
        animateToHideIcon();
    }

    private void switchToRapid() {
        animateToShowRapidIcon();
    }

    private void switchToSuperRapid() {
        animateToShowSuperRapidIcon();
    }

    private void animateToHideIcon() {
        Log.i("RapidChargeView", "animateToHideIcon: ");
        if (this.mContentSwitchAnimator != null) {
            this.mContentSwitchAnimator.cancel();
        }
        PropertyValuesHolder scaleXProperty = PropertyValuesHolder.ofFloat(SCALE_X, new float[]{this.mPercentCountView.getScaleX(), 1.0f});
        PropertyValuesHolder scaleYProperty = PropertyValuesHolder.ofFloat(SCALE_Y, new float[]{this.mPercentCountView.getScaleY(), 1.0f});
        PropertyValuesHolder translationYProperty = PropertyValuesHolder.ofFloat(TRANSLATION_Y, new float[]{this.mPercentCountView.getTranslationY(), (float) this.mChargeNumberTranslateInit});
        ObjectAnimator numberAnimator = ObjectAnimator.ofPropertyValuesHolder(this.mPercentCountView, new PropertyValuesHolder[]{scaleXProperty, scaleYProperty, translationYProperty}).setDuration(500);
        PropertyValuesHolder translationYProperty2 = PropertyValuesHolder.ofFloat(TRANSLATION_Y, new float[]{this.mStateTip.getTranslationY(), 0.0f});
        PropertyValuesHolder alphaProperty = PropertyValuesHolder.ofFloat(ALPHA, new float[]{this.mStateTip.getAlpha(), 0.0f});
        ObjectAnimator tipAnimator = ObjectAnimator.ofPropertyValuesHolder(this.mStateTip, new PropertyValuesHolder[]{alphaProperty, translationYProperty2}).setDuration(500);
        PropertyValuesHolder translationYProperty3 = PropertyValuesHolder.ofFloat(TRANSLATION_Y, new float[]{this.mGtChargeAniView.getTranslationY(), 0.0f});
        PropertyValuesHolder alphaProperty2 = PropertyValuesHolder.ofFloat(ALPHA, new float[]{this.mGtChargeAniView.getAlpha(), 0.0f});
        ObjectAnimator gtTipRapidAnimator = ObjectAnimator.ofPropertyValuesHolder(this.mGtChargeAniView, new PropertyValuesHolder[]{alphaProperty2, translationYProperty3}).setDuration(500);
        PropertyValuesHolder scaleXCarProperty = PropertyValuesHolder.ofFloat(SCALE_X, new float[]{this.mRapidIcon.getScaleX(), 0.0f});
        PropertyValuesHolder scaleYCarProperty = PropertyValuesHolder.ofFloat(SCALE_Y, new float[]{this.mRapidIcon.getScaleY(), 0.0f});
        PropertyValuesHolder alphaProperty3 = PropertyValuesHolder.ofFloat(ALPHA, new float[]{this.mRapidIcon.getAlpha(), 0.0f});
        ObjectAnimator numberAnimator2 = numberAnimator;
        ObjectAnimator rapidIconAnimator = ObjectAnimator.ofPropertyValuesHolder(this.mRapidIcon, new PropertyValuesHolder[]{scaleXCarProperty, scaleYCarProperty, alphaProperty3}).setDuration(500);
        PropertyValuesHolder scaleXProperty2 = PropertyValuesHolder.ofFloat(SCALE_X, new float[]{this.mSuperRapidIcon.getScaleX(), 0.0f});
        PropertyValuesHolder scaleYProperty2 = PropertyValuesHolder.ofFloat(SCALE_Y, new float[]{this.mSuperRapidIcon.getScaleY(), 0.0f});
        PropertyValuesHolder alphaProperty4 = PropertyValuesHolder.ofFloat(ALPHA, new float[]{this.mSuperRapidIcon.getAlpha(), 0.0f});
        ObjectAnimator superRapidIconAnimator = ObjectAnimator.ofPropertyValuesHolder(this.mSuperRapidIcon, new PropertyValuesHolder[]{scaleXProperty2, scaleYProperty2, alphaProperty4}).setDuration(500);
        this.mContentSwitchAnimator = new AnimatorSet();
        this.mContentSwitchAnimator.setInterpolator(this.mCubicInterpolator);
        this.mContentSwitchAnimator.playTogether(new Animator[]{numberAnimator2, tipAnimator, gtTipRapidAnimator, rapidIconAnimator, superRapidIconAnimator});
        this.mContentSwitchAnimator.start();
    }

    private void animateToShowRapidIcon() {
        Log.i("RapidChargeView", "animateToShowRapidIcon: ");
        if (this.mContentSwitchAnimator != null) {
            this.mContentSwitchAnimator.cancel();
        }
        PropertyValuesHolder scaleXProperty = PropertyValuesHolder.ofFloat(SCALE_X, new float[]{this.mPercentCountView.getScaleX(), 0.85f});
        PropertyValuesHolder scaleYProperty = PropertyValuesHolder.ofFloat(SCALE_Y, new float[]{this.mPercentCountView.getScaleY(), 0.85f});
        PropertyValuesHolder translationYProperty = PropertyValuesHolder.ofFloat(TRANSLATION_Y, new float[]{this.mPercentCountView.getTranslationY(), (float) this.mChargeNumberTranslateSmall});
        ObjectAnimator numberAnimator = ObjectAnimator.ofPropertyValuesHolder(this.mPercentCountView, new PropertyValuesHolder[]{scaleXProperty, scaleYProperty, translationYProperty}).setDuration(500);
        numberAnimator.setInterpolator(this.mCubicInterpolator);
        PropertyValuesHolder translationYProperty2 = PropertyValuesHolder.ofFloat(TRANSLATION_Y, new float[]{this.mStateTip.getTranslationY(), (float) this.mChargeTipTranslateSmall});
        PropertyValuesHolder alphaProperty = PropertyValuesHolder.ofFloat(ALPHA, new float[]{this.mStateTip.getAlpha(), 1.0f});
        ObjectAnimator tipAnimator = ObjectAnimator.ofPropertyValuesHolder(this.mStateTip, new PropertyValuesHolder[]{alphaProperty, translationYProperty2}).setDuration(500);
        tipAnimator.setInterpolator(this.mCubicInterpolator);
        PropertyValuesHolder translationYProperty3 = PropertyValuesHolder.ofFloat(TRANSLATION_Y, new float[]{this.mGtChargeAniView.getTranslationY(), (float) this.mChargeTipTranslateSmall});
        PropertyValuesHolder alphaProperty2 = PropertyValuesHolder.ofFloat(ALPHA, new float[]{this.mGtChargeAniView.getAlpha(), 0.0f});
        ObjectAnimator gtTipAnimator = ObjectAnimator.ofPropertyValuesHolder(this.mGtChargeAniView, new PropertyValuesHolder[]{alphaProperty2, translationYProperty3}).setDuration(250);
        gtTipAnimator.setInterpolator(this.mCubicInterpolator);
        PropertyValuesHolder scaleXProperty2 = PropertyValuesHolder.ofFloat(SCALE_X, new float[]{this.mRapidIcon.getScaleX(), 1.0f});
        PropertyValuesHolder scaleYProperty2 = PropertyValuesHolder.ofFloat(SCALE_Y, new float[]{this.mRapidIcon.getScaleY(), 1.0f});
        PropertyValuesHolder alphaProperty3 = PropertyValuesHolder.ofFloat(ALPHA, new float[]{this.mRapidIcon.getAlpha(), 1.0f});
        ObjectAnimator rapidIconAnimator = ObjectAnimator.ofPropertyValuesHolder(this.mRapidIcon, new PropertyValuesHolder[]{scaleXProperty2, scaleYProperty2, alphaProperty3}).setDuration(500);
        rapidIconAnimator.setInterpolator(this.mCubicInterpolator);
        PropertyValuesHolder scaleXCarProperty = PropertyValuesHolder.ofFloat(SCALE_X, new float[]{this.mSuperRapidIcon.getScaleX(), 0.0f});
        PropertyValuesHolder scaleYCarProperty = PropertyValuesHolder.ofFloat(SCALE_Y, new float[]{this.mSuperRapidIcon.getScaleY(), 0.0f});
        PropertyValuesHolder alphaProperty4 = PropertyValuesHolder.ofFloat(ALPHA, new float[]{this.mSuperRapidIcon.getAlpha(), 0.0f});
        ObjectAnimator superRapidIconAnimator = ObjectAnimator.ofPropertyValuesHolder(this.mSuperRapidIcon, new PropertyValuesHolder[]{scaleXCarProperty, scaleYCarProperty, alphaProperty4}).setDuration(500);
        superRapidIconAnimator.setInterpolator(this.mCubicInterpolator);
        rapidIconAnimator.setInterpolator(new OvershootInterpolator(3.0f));
        this.mContentSwitchAnimator = new AnimatorSet();
        this.mContentSwitchAnimator.playTogether(new Animator[]{numberAnimator, tipAnimator, gtTipAnimator, rapidIconAnimator, superRapidIconAnimator});
        this.mContentSwitchAnimator.start();
    }

    private void animateToShowSuperRapidIcon() {
        Log.i("RapidChargeView", "animateToShowSuperRapidIcon: ");
        if (this.mContentSwitchAnimator != null) {
            this.mContentSwitchAnimator.cancel();
        }
        PropertyValuesHolder scaleXProperty = PropertyValuesHolder.ofFloat(SCALE_X, new float[]{this.mPercentCountView.getScaleX(), 0.85f});
        PropertyValuesHolder scaleYProperty = PropertyValuesHolder.ofFloat(SCALE_Y, new float[]{this.mPercentCountView.getScaleY(), 0.85f});
        PropertyValuesHolder translationYProperty = PropertyValuesHolder.ofFloat(TRANSLATION_Y, new float[]{this.mPercentCountView.getTranslationY(), (float) this.mChargeNumberTranslateSmall});
        ObjectAnimator numberAnimator = ObjectAnimator.ofPropertyValuesHolder(this.mPercentCountView, new PropertyValuesHolder[]{scaleXProperty, scaleYProperty, translationYProperty}).setDuration(500);
        numberAnimator.setInterpolator(this.mCubicInterpolator);
        PropertyValuesHolder translationYProperty2 = PropertyValuesHolder.ofFloat(TRANSLATION_Y, new float[]{this.mStateTip.getTranslationY(), (float) this.mChargeTipTranslateSmall});
        PropertyValuesHolder alphaProperty = PropertyValuesHolder.ofFloat(ALPHA, new float[]{this.mStateTip.getAlpha(), 0.0f});
        ObjectAnimator tipAnimator = ObjectAnimator.ofPropertyValuesHolder(this.mStateTip, new PropertyValuesHolder[]{alphaProperty, translationYProperty2}).setDuration(500);
        tipAnimator.setInterpolator(this.mCubicInterpolator);
        PropertyValuesHolder translationYProperty3 = PropertyValuesHolder.ofFloat(TRANSLATION_Y, new float[]{this.mGtChargeAniView.getTranslationY(), (float) this.mChargeTipTranslateSmall});
        PropertyValuesHolder alphaProperty2 = PropertyValuesHolder.ofFloat(ALPHA, new float[]{this.mGtChargeAniView.getAlpha(), 1.0f});
        ObjectAnimator gtTipAnimator = ObjectAnimator.ofPropertyValuesHolder(this.mGtChargeAniView, new PropertyValuesHolder[]{alphaProperty2, translationYProperty3}).setDuration(250);
        gtTipAnimator.setInterpolator(this.mCubicInterpolator);
        gtTipAnimator.addListener(new Animator.AnimatorListener() {
            public void onAnimationStart(Animator animator) {
                RapidChargeView.this.mGtChargeAniView.setVisibility(8);
            }

            public void onAnimationEnd(Animator animator) {
                RapidChargeView.this.mGtChargeAniView.setViewInitState();
                RapidChargeView.this.mGtChargeAniView.setVisibility(0);
                RapidChargeView.this.mGtChargeAniView.animationToShow();
            }

            public void onAnimationCancel(Animator animator) {
                RapidChargeView.this.mGtChargeAniView.setVisibility(8);
            }

            public void onAnimationRepeat(Animator animator) {
            }
        });
        PropertyValuesHolder scaleXProperty2 = PropertyValuesHolder.ofFloat(SCALE_X, new float[]{this.mRapidIcon.getScaleX(), 0.0f});
        PropertyValuesHolder scaleYProperty2 = PropertyValuesHolder.ofFloat(SCALE_Y, new float[]{this.mRapidIcon.getScaleY(), 0.0f});
        PropertyValuesHolder alphaProperty3 = PropertyValuesHolder.ofFloat(ALPHA, new float[]{this.mRapidIcon.getAlpha(), 0.0f});
        ObjectAnimator rapidIconAnimator = ObjectAnimator.ofPropertyValuesHolder(this.mRapidIcon, new PropertyValuesHolder[]{scaleXProperty2, scaleYProperty2, alphaProperty3}).setDuration(500);
        rapidIconAnimator.setInterpolator(this.mCubicInterpolator);
        PropertyValuesHolder scaleXCarProperty = PropertyValuesHolder.ofFloat(SCALE_X, new float[]{this.mSuperRapidIcon.getScaleX(), 1.0f});
        PropertyValuesHolder scaleYCarProperty = PropertyValuesHolder.ofFloat(SCALE_Y, new float[]{this.mSuperRapidIcon.getScaleY(), 1.0f});
        PropertyValuesHolder alphaProperty4 = PropertyValuesHolder.ofFloat(ALPHA, new float[]{this.mSuperRapidIcon.getAlpha(), 1.0f});
        ObjectAnimator superRapidIconAnimator = ObjectAnimator.ofPropertyValuesHolder(this.mSuperRapidIcon, new PropertyValuesHolder[]{scaleXCarProperty, scaleYCarProperty, alphaProperty4}).setDuration(500);
        superRapidIconAnimator.setInterpolator(this.mCubicInterpolator);
        superRapidIconAnimator.setInterpolator(new OvershootInterpolator(3.0f));
        this.mContentSwitchAnimator = new AnimatorSet();
        this.mContentSwitchAnimator.playTogether(new Animator[]{numberAnimator, tipAnimator, gtTipAnimator, rapidIconAnimator, superRapidIconAnimator});
        this.mContentSwitchAnimator.start();
    }

    public void setScreenOn(boolean screenOn) {
        this.mIsScreenOn = screenOn;
    }

    public void setProgress(float progress) {
        this.mPercentCountView.setProgress(progress);
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!this.mWindowShouldAdd) {
            removeFromWindow("!mWindowShouldAdd");
        } else {
            checkScreenSize();
        }
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (this.mWindowShouldAdd) {
            addToWindow("mWindowShouldAdd");
        }
        this.mHandler.removeCallbacksAndMessages(null);
    }

    public void zoomLarge() {
        Log.i("RapidChargeView", "zoomLarge: ");
        this.mHandler.removeCallbacks(this.mDismissRunnable);
        if (this.mDismissAnimatorSet != null && this.mStartingDismissWirelessAlphaAnim) {
            this.mDismissAnimatorSet.cancel();
        }
        this.mStartingDismissWirelessAlphaAnim = false;
        addToWindow("zoomLarge: ");
        hideSystemUI();
        setComponentTransparent(false);
        setViewState();
        setVisibility(0);
        requestFocus();
        if (this.mEnterAnimatorSet == null) {
            initAnimator();
        }
        if (this.mEnterAnimatorSet.isStarted()) {
            this.mEnterAnimatorSet.cancel();
        }
        this.mEnterAnimatorSet.start();
        this.mFireworksView.start();
        this.mInnerCircleAnimator.start();
        this.mParticleCircleAnimator.start();
        post(new Runnable() {
            public void run() {
                RapidChargeView.this.disableTouch(false);
            }
        });
    }

    private void initAnimator() {
        ValueAnimator zoomAnimator = ValueAnimator.ofInt(new int[]{0, 1});
        zoomAnimator.setInterpolator(this.mQuartOutInterpolator);
        zoomAnimator.setDuration(800);
        zoomAnimator.addListener(this);
        zoomAnimator.addUpdateListener(this);
        PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat(ALPHA, new float[]{1.0f, 0.0f});
        ObjectAnimator lightFadeOutAnimator = ObjectAnimator.ofPropertyValuesHolder(this.mBottomLightImage, new PropertyValuesHolder[]{alpha}).setDuration(1000);
        this.mEnterAnimatorSet = new AnimatorSet();
        this.mEnterAnimatorSet.play(lightFadeOutAnimator).after(zoomAnimator);
    }

    private void setViewState() {
        this.mInnerCircleView.setAlpha(0.0f);
        this.mInnerCircleView.setScaleX(0.0f);
        this.mInnerCircleView.setScaleY(0.0f);
        this.mParticleCircleView.setAlpha(0.0f);
        this.mParticleCircleView.setScaleX(0.0f);
        this.mParticleCircleView.setScaleY(0.0f);
        this.mFireworksView.setAlpha(0.0f);
        this.mOutlineView.setAlpha(0.0f);
        this.mBottomLightImage.setAlpha(0.0f);
        switch (this.mChargeState) {
            case 0:
                this.mPercentCountView.setScaleX(1.0f);
                this.mPercentCountView.setScaleY(1.0f);
                this.mPercentCountView.setTranslationY((float) this.mChargeNumberTranslateInit);
                this.mStateTip.setAlpha(0.0f);
                this.mStateTip.setTranslationY(0.0f);
                this.mGtChargeAniView.setViewInitState();
                this.mGtChargeAniView.setVisibility(8);
                this.mRapidIcon.setScaleY(0.0f);
                this.mRapidIcon.setScaleX(0.0f);
                this.mRapidIcon.setAlpha(0.0f);
                this.mSuperRapidIcon.setScaleY(0.0f);
                this.mSuperRapidIcon.setScaleX(0.0f);
                this.mSuperRapidIcon.setAlpha(0.0f);
                return;
            case 1:
                this.mPercentCountView.setScaleX(0.85f);
                this.mPercentCountView.setScaleY(0.85f);
                this.mPercentCountView.setTranslationY((float) this.mChargeNumberTranslateSmall);
                this.mStateTip.setAlpha(1.0f);
                this.mStateTip.setTranslationY((float) this.mChargeTipTranslateSmall);
                this.mGtChargeAniView.setViewInitState();
                this.mGtChargeAniView.setVisibility(8);
                this.mRapidIcon.setScaleY(1.0f);
                this.mRapidIcon.setScaleX(1.0f);
                this.mRapidIcon.setAlpha(1.0f);
                this.mSuperRapidIcon.setScaleY(0.0f);
                this.mSuperRapidIcon.setScaleX(0.0f);
                this.mSuperRapidIcon.setAlpha(0.0f);
                return;
            case 2:
                this.mPercentCountView.setScaleX(0.85f);
                this.mPercentCountView.setScaleY(0.85f);
                this.mPercentCountView.setTranslationY((float) this.mChargeNumberTranslateSmall);
                this.mStateTip.setAlpha(0.0f);
                this.mStateTip.setTranslationY((float) this.mChargeTipTranslateSmall);
                this.mGtChargeAniView.setViewInitState();
                this.mGtChargeAniView.setVisibility(0);
                this.mGtChargeAniView.animationToShow();
                this.mRapidIcon.setScaleY(0.0f);
                this.mRapidIcon.setScaleX(0.0f);
                this.mRapidIcon.setAlpha(0.0f);
                this.mSuperRapidIcon.setScaleY(1.0f);
                this.mSuperRapidIcon.setScaleX(1.0f);
                this.mSuperRapidIcon.setAlpha(1.0f);
                return;
            default:
                return;
        }
    }

    public void onAnimationUpdate(ValueAnimator animation) {
        float fraction = animation.getAnimatedFraction();
        this.mContentContainer.setScaleX(fraction);
        this.mContentContainer.setScaleY(fraction);
        this.mContentContainer.setAlpha(fraction);
        this.mInnerCircleView.setScaleX(fraction);
        this.mInnerCircleView.setScaleY(fraction);
        this.mInnerCircleView.setAlpha(fraction);
        this.mParticleCircleView.setScaleX(fraction);
        this.mParticleCircleView.setScaleY(fraction);
        this.mParticleCircleView.setAlpha(fraction);
        this.mFireworksView.setAlpha(fraction);
        this.mOutlineView.setAlpha(fraction);
        this.mBottomLightImage.setAlpha(fraction);
    }

    public void onAnimationStart(Animator animation) {
        if (this.animationListener != null) {
            this.animationListener.onRapidAnimationStart(11);
        }
        this.mHandler.removeCallbacks(this.timeoutDismissJob);
        this.mHandler.postDelayed(this.timeoutDismissJob, 19800);
    }

    public void onAnimationEnd(Animator animation) {
    }

    public void onAnimationCancel(Animator animation) {
    }

    public void onAnimationRepeat(Animator animation) {
    }

    public void addToWindow(String reason) {
        this.mWindowShouldAdd = true;
        if (!isAttachedToWindow() && getParent() == null) {
            try {
                Log.i("RapidChargeView", "addToWindow: " + reason);
                setAlpha(0.0f);
                this.mWindowManager.addView(this, getWindowParam());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void removeFromWindow(String reason) {
        this.mWindowShouldAdd = false;
        if (isAttachedToWindow()) {
            try {
                Log.i("RapidChargeView", "removeFromWindow: " + reason);
                this.mWindowManager.removeView(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void startDismiss(String reason) {
        String str = reason;
        disableTouch(true);
        if (!this.mStartingDismissWirelessAlphaAnim) {
            if (this.mEnterAnimatorSet != null) {
                this.mEnterAnimatorSet.cancel();
            }
            Log.i("RapidChargeView", "startDismiss: reason: " + str);
            this.mHandler.removeCallbacks(this.timeoutDismissJob);
            this.mHandler.removeCallbacks(this.mDismissRunnable);
            PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat(ALPHA, new float[]{getAlpha(), 0.0f});
            ObjectAnimator bgAnimator = ObjectAnimator.ofPropertyValuesHolder(this, new PropertyValuesHolder[]{alpha}).setDuration(200);
            PropertyValuesHolder alphaContentContainer = PropertyValuesHolder.ofFloat(ALPHA, new float[]{this.mContentContainer.getAlpha(), 0.0f});
            PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat(SCALE_X, new float[]{this.mContentContainer.getScaleX(), 0.0f});
            PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat(SCALE_Y, new float[]{this.mContentContainer.getScaleY(), 0.0f});
            ObjectAnimator contentContainerAnimator = ObjectAnimator.ofPropertyValuesHolder(this.mContentContainer, new PropertyValuesHolder[]{alphaContentContainer, scaleX, scaleY}).setDuration(200);
            PropertyValuesHolder alphaImage = PropertyValuesHolder.ofFloat(ALPHA, new float[]{this.mOutlineView.getAlpha(), 0.0f});
            ObjectAnimator imageAnimator = ObjectAnimator.ofPropertyValuesHolder(this.mOutlineView, new PropertyValuesHolder[]{alphaImage}).setDuration(200);
            PropertyValuesHolder alphaCircle = PropertyValuesHolder.ofFloat(ALPHA, new float[]{this.mInnerCircleView.getAlpha(), 0.0f});
            PropertyValuesHolder scaleX2 = PropertyValuesHolder.ofFloat(SCALE_X, new float[]{this.mInnerCircleView.getScaleX(), 0.0f});
            PropertyValuesHolder scaleY2 = PropertyValuesHolder.ofFloat(SCALE_Y, new float[]{this.mInnerCircleView.getScaleY(), 0.0f});
            PropertyValuesHolder propertyValuesHolder = alphaCircle;
            ObjectAnimator circleAnimator = ObjectAnimator.ofPropertyValuesHolder(this.mInnerCircleView, new PropertyValuesHolder[]{alphaCircle, scaleX2, scaleY2}).setDuration(200);
            PropertyValuesHolder alphaCircle2 = PropertyValuesHolder.ofFloat(ALPHA, new float[]{this.mParticleCircleView.getAlpha(), 0.0f});
            PropertyValuesHolder scaleX3 = PropertyValuesHolder.ofFloat(SCALE_X, new float[]{this.mParticleCircleView.getScaleX(), 0.0f});
            PropertyValuesHolder scaleY3 = PropertyValuesHolder.ofFloat(SCALE_Y, new float[]{this.mParticleCircleView.getScaleY(), 0.0f});
            PropertyValuesHolder propertyValuesHolder2 = alpha;
            ObjectAnimator circleParticleAnimator = ObjectAnimator.ofPropertyValuesHolder(this.mParticleCircleView, new PropertyValuesHolder[]{alphaCircle2, scaleX3, scaleY3}).setDuration(200);
            PropertyValuesHolder alphaDot = PropertyValuesHolder.ofFloat(ALPHA, new float[]{this.mFireworksView.getAlpha(), 0.0f});
            PropertyValuesHolder propertyValuesHolder3 = scaleX3;
            ObjectAnimator dotAnimator = ObjectAnimator.ofPropertyValuesHolder(this.mFireworksView, new PropertyValuesHolder[]{alphaDot}).setDuration(200);
            PropertyValuesHolder alphaBottomLight = PropertyValuesHolder.ofFloat(ALPHA, new float[]{this.mBottomLightImage.getAlpha(), 0.0f});
            PropertyValuesHolder propertyValuesHolder4 = alphaImage;
            PropertyValuesHolder propertyValuesHolder5 = scaleY3;
            PropertyValuesHolder propertyValuesHolder6 = alphaCircle2;
            ObjectAnimator bottomLightAnimator = ObjectAnimator.ofPropertyValuesHolder(this.mBottomLightImage, new PropertyValuesHolder[]{alphaBottomLight}).setDuration(200);
            this.mDismissAnimatorSet = new AnimatorSet();
            this.mDismissAnimatorSet.setInterpolator(this.mQuartOutInterpolator);
            this.mDismissAnimatorSet.playTogether(new Animator[]{contentContainerAnimator, imageAnimator, circleAnimator, circleParticleAnimator, dotAnimator, bottomLightAnimator});
            if (!"dismiss_for_timeout".equals(str)) {
                this.mDismissAnimatorSet.play(bgAnimator).with(contentContainerAnimator);
            }
            this.mDismissAnimatorSet.addListener(new Animator.AnimatorListener() {
                public void onAnimationStart(Animator animation) {
                }

                public void onAnimationRepeat(Animator animation) {
                }

                public void onAnimationEnd(Animator animation) {
                    boolean unused = RapidChargeView.this.mStartingDismissWirelessAlphaAnim = false;
                    if (RapidChargeView.this.animationListener != null) {
                        RapidChargeView.this.animationListener.onRapidAnimationEnd(11);
                    }
                    RapidChargeView.this.mHandler.post(RapidChargeView.this.mDismissRunnable);
                }

                public void onAnimationCancel(Animator animation) {
                    boolean unused = RapidChargeView.this.mStartingDismissWirelessAlphaAnim = false;
                    if (RapidChargeView.this.animationListener != null) {
                        RapidChargeView.this.animationListener.onRapidAnimationEnd(11);
                    }
                    RapidChargeView.this.mHandler.removeCallbacks(RapidChargeView.this.mDismissRunnable);
                }
            });
            this.mStartingDismissWirelessAlphaAnim = true;
            this.mDismissAnimatorSet.start();
        }
    }

    public static WindowManager.LayoutParams getWindowParam() {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(-1, -1, 2009, 84083984, -3);
        lp.windowAnimations = 0;
        lp.screenOrientation = 1;
        lp.setTitle("rapid_charge");
        return lp;
    }

    /* access modifiers changed from: private */
    public void setComponentTransparent(boolean transparent) {
        if (transparent) {
            setAlpha(0.0f);
            this.mInnerCircleView.setAlpha(0.0f);
            this.mParticleCircleView.setAlpha(0.0f);
            this.mFireworksView.setAlpha(0.0f);
            this.mOutlineView.setAlpha(0.0f);
            this.mContentContainer.setAlpha(0.0f);
            this.mBottomLightImage.setAlpha(0.0f);
            return;
        }
        setAlpha(1.0f);
        this.mInnerCircleView.setAlpha(1.0f);
        this.mParticleCircleView.setAlpha(1.0f);
        this.mFireworksView.setAlpha(1.0f);
        this.mOutlineView.setAlpha(1.0f);
        this.mContentContainer.setAlpha(1.0f);
        this.mBottomLightImage.setAlpha(1.0f);
    }

    public void setRapidAnimationListener(IRapidAnimationListener listener) {
        this.animationListener = listener;
    }

    private void hideSystemUI() {
        setSystemUiVisibility(4864);
    }

    /* access modifiers changed from: private */
    public void disableTouch(boolean disableTouch) {
        if (isAttachedToWindow()) {
            WindowManager.LayoutParams windowLayoutParameters = (WindowManager.LayoutParams) getLayoutParams();
            if (disableTouch) {
                windowLayoutParameters.flags |= 16;
            } else {
                windowLayoutParameters.flags &= -17;
            }
            this.mWindowManager.updateViewLayout(this, windowLayoutParameters);
        }
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
            post(new Runnable() {
                public void run() {
                    RapidChargeView.this.startContentSwitchAnimation();
                }
            });
        }
    }

    private void updateSizeForScreenSizeChange() {
        float rateWidth = (((float) Math.min(this.mScreenSize.x, this.mScreenSize.y)) * 1.0f) / 1080.0f;
        this.mPivotX = (int) (100.0f * rateWidth);
        this.mChargeNumberTranslateSmall = (int) (-70.0f * rateWidth);
        this.mChargeNumberTranslateInit = (int) (-10.0f * rateWidth);
        this.mChargeTipTranslateSmall = (int) (-50.0f * rateWidth);
        this.mInnerCircleSize = (int) (662.0f * rateWidth);
        this.mInnerParticleCircleSize = (int) (612.0f * rateWidth);
        this.mSpeedTipTextSizePx = (int) (34.485f * rateWidth);
        this.mSpaceHeight = (int) (16.0f * rateWidth);
        this.mTipTopMargin = (int) (70.0f * rateWidth);
        this.mIconPaddingTop = (int) (275.0f * rateWidth);
        if (this.mRapidIconDrawable != null) {
            this.mRapidIconWidth = (int) (((float) this.mRapidIconDrawable.getIntrinsicWidth()) * rateWidth);
            this.mRapidIconHeight = (int) (((float) this.mRapidIconDrawable.getIntrinsicHeight()) * rateWidth);
        }
        if (this.mSuperRapidIconDrawable != null) {
            this.mSuperRapidIconWidth = (int) (((float) this.mSuperRapidIconDrawable.getIntrinsicWidth()) * rateWidth);
            this.mSuperRapidIconHeight = (int) (((float) this.mSuperRapidIconDrawable.getIntrinsicHeight()) * rateWidth);
        }
        if (this.mBottomLightDrawable != null) {
            this.mBottomLightWidth = (int) (((float) this.mBottomLightDrawable.getIntrinsicWidth()) * rateWidth);
            this.mBottomLightHeight = (int) (((float) this.mBottomLightDrawable.getIntrinsicHeight()) * rateWidth);
        }
    }

    private void updateLayoutParamForScreenSizeChange() {
        this.mCenterAnchorView.getLayoutParams().height = this.mSpaceHeight;
        this.mStateTip.setTextSize(0, (float) this.mSpeedTipTextSizePx);
        ((RelativeLayout.LayoutParams) this.mStateTip.getLayoutParams()).topMargin = this.mTipTopMargin;
        ((RelativeLayout.LayoutParams) this.mGtChargeAniView.getLayoutParams()).topMargin = this.mTipTopMargin;
        RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) this.mRapidIcon.getLayoutParams();
        rlp.width = this.mRapidIconWidth;
        rlp.height = this.mRapidIconHeight + this.mIconPaddingTop;
        this.mRapidIcon.setPadding(0, this.mIconPaddingTop, 0, 0);
        this.mRapidIcon.setPivotX((float) this.mPivotX);
        RelativeLayout.LayoutParams rlp2 = (RelativeLayout.LayoutParams) this.mSuperRapidIcon.getLayoutParams();
        rlp2.width = this.mSuperRapidIconWidth;
        rlp2.height = this.mSuperRapidIconHeight + this.mIconPaddingTop;
        this.mSuperRapidIcon.setPadding(0, this.mIconPaddingTop, 0, 0);
        this.mSuperRapidIcon.setPivotX((float) this.mPivotX);
        FrameLayout.LayoutParams flp = (FrameLayout.LayoutParams) this.mInnerCircleView.getLayoutParams();
        flp.width = this.mInnerCircleSize;
        flp.height = this.mInnerCircleSize;
        FrameLayout.LayoutParams flp2 = (FrameLayout.LayoutParams) this.mParticleCircleView.getLayoutParams();
        flp2.width = this.mInnerParticleCircleSize;
        flp2.height = this.mInnerParticleCircleSize;
        FrameLayout.LayoutParams flp3 = (FrameLayout.LayoutParams) this.mBottomLightImage.getLayoutParams();
        flp3.width = this.mBottomLightWidth;
        flp3.height = this.mBottomLightHeight;
    }
}
