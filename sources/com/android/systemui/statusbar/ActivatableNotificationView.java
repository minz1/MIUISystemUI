package com.android.systemui.statusbar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.classifier.FalsingManager;
import com.android.systemui.miui.statusbar.notification.NotificationUtil;
import com.android.systemui.statusbar.notification.FakeShadowView;
import com.android.systemui.statusbar.notification.NotificationUtils;
import com.android.systemui.statusbar.phone.DoubleTapHelper;

public abstract class ActivatableNotificationView extends ExpandableOutlineView {
    private static final Interpolator ACTIVATE_INVERSE_ALPHA_INTERPOLATOR = new PathInterpolator(0.0f, 0.0f, 0.5f, 1.0f);
    private static final Interpolator ACTIVATE_INVERSE_INTERPOLATOR = new PathInterpolator(0.6f, 0.0f, 0.5f, 1.0f);
    private final AccessibilityManager mAccessibilityManager;
    private boolean mActivated;
    private float mAnimationTranslationY;
    /* access modifiers changed from: private */
    public float mAppearAnimationFraction = -1.0f;
    private RectF mAppearAnimationRect = new RectF();
    private float mAppearAnimationTranslation;
    private ValueAnimator mAppearAnimator;
    private ObjectAnimator mBackgroundAnimator;
    /* access modifiers changed from: private */
    public ValueAnimator mBackgroundColorAnimator;
    protected NotificationBackgroundView mBackgroundDimmed;
    protected NotificationBackgroundView mBackgroundNormal;
    private ValueAnimator.AnimatorUpdateListener mBackgroundVisibilityUpdater = new ValueAnimator.AnimatorUpdateListener() {
        public void onAnimationUpdate(ValueAnimator animation) {
            ActivatableNotificationView.this.setNormalBackgroundVisibilityAmount(ActivatableNotificationView.this.mBackgroundNormal.getAlpha());
            float unused = ActivatableNotificationView.this.mDimmedBackgroundFadeInAmount = ActivatableNotificationView.this.mBackgroundDimmed.getAlpha();
        }
    };
    private float mBgAlpha = 1.0f;
    protected int mBgTint = 0;
    private Interpolator mCurrentAlphaInterpolator;
    private Interpolator mCurrentAppearInterpolator;
    private int mCurrentBackgroundTint;
    private boolean mDark;
    private boolean mDimmed;
    private int mDimmedAlpha;
    /* access modifiers changed from: private */
    public float mDimmedBackgroundFadeInAmount = -1.0f;
    private final DoubleTapHelper mDoubleTapHelper;
    private boolean mDrawingAppearAnimation;
    private AnimatorListenerAdapter mFadeInEndListener = new AnimatorListenerAdapter() {
        public void onAnimationEnd(Animator animation) {
            super.onAnimationEnd(animation);
            ValueAnimator unused = ActivatableNotificationView.this.mFadeInFromDarkAnimator = null;
            float unused2 = ActivatableNotificationView.this.mDimmedBackgroundFadeInAmount = -1.0f;
            ActivatableNotificationView.this.updateBackground();
        }
    };
    /* access modifiers changed from: private */
    public ValueAnimator mFadeInFromDarkAnimator;
    private FakeShadowView mFakeShadow;
    /* access modifiers changed from: private */
    public FalsingManager mFalsingManager;
    private boolean mIsBelowSpeedBump;
    private final int mLowPriorityColor;
    private final int mLowPriorityRippleColor;
    private boolean mNeedsDimming;
    private float mNormalBackgroundVisibilityAmount;
    private final int mNormalColor;
    protected final int mNormalRippleColor;
    private OnActivatedListener mOnActivatedListener;
    private float mOverrideAmount;
    private int mOverrideTint;
    private float mShadowAlpha = 1.0f;
    private boolean mShadowHidden;
    private final Interpolator mSlowOutFastInInterpolator = new PathInterpolator(0.8f, 0.0f, 0.6f, 1.0f);
    private final Interpolator mSlowOutLinearInInterpolator = new PathInterpolator(0.8f, 0.0f, 1.0f, 1.0f);
    /* access modifiers changed from: private */
    public int mStartTint;
    private final Runnable mTapTimeoutRunnable = new Runnable() {
        public void run() {
            ActivatableNotificationView.this.makeInactive(true);
        }
    };
    /* access modifiers changed from: private */
    public int mTargetTint;
    private final int mTintedRippleColor;
    private ValueAnimator.AnimatorUpdateListener mUpdateOutlineListener = new ValueAnimator.AnimatorUpdateListener() {
        public void onAnimationUpdate(ValueAnimator animation) {
            ActivatableNotificationView.this.updateOutlineAlpha();
        }
    };
    private boolean mWasActivatedOnDown;

    public interface OnActivatedListener {
        void onActivated(ActivatableNotificationView activatableNotificationView);

        void onActivationReset(ActivatableNotificationView activatableNotificationView);
    }

    /* access modifiers changed from: protected */
    public abstract View getContentView();

    public ActivatableNotificationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setClipChildren(false);
        setClipToPadding(false);
        this.mNormalColor = context.getColor(R.color.notification_material_background_color);
        this.mLowPriorityColor = this.mNormalColor;
        this.mTintedRippleColor = context.getColor(R.color.notification_ripple_tinted_color);
        this.mLowPriorityRippleColor = context.getColor(R.color.notification_ripple_color_low_priority);
        this.mNormalRippleColor = context.getColor(R.color.notification_ripple_untinted_color);
        this.mFalsingManager = FalsingManager.getInstance(context);
        this.mAccessibilityManager = AccessibilityManager.getInstance(this.mContext);
        DoubleTapHelper doubleTapHelper = new DoubleTapHelper(this, new DoubleTapHelper.ActivationListener() {
            public void onActiveChanged(boolean active) {
                if (active) {
                    ActivatableNotificationView.this.makeActive();
                } else {
                    ActivatableNotificationView.this.makeInactive(true);
                }
            }
        }, new DoubleTapHelper.DoubleTapListener() {
            public boolean onDoubleTap() {
                return ActivatableNotificationView.this.performClick();
            }
        }, new DoubleTapHelper.SlideBackListener() {
            public boolean onSlideBack() {
                return ActivatableNotificationView.this.handleSlideBack();
            }
        }, new DoubleTapHelper.DoubleTapLogListener() {
            public void onDoubleTapLog(boolean accepted, float dx, float dy) {
                ActivatableNotificationView.this.mFalsingManager.onNotificationDoubleTap(accepted, dx, dy);
            }
        });
        this.mDoubleTapHelper = doubleTapHelper;
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mBackgroundNormal = (NotificationBackgroundView) findViewById(R.id.backgroundNormal);
        this.mFakeShadow = (FakeShadowView) findViewById(R.id.fake_shadow);
        this.mShadowHidden = this.mFakeShadow.getVisibility() != 0;
        this.mBackgroundDimmed = (NotificationBackgroundView) findViewById(R.id.backgroundDimmed);
        this.mBackgroundNormal.setCustomBackground((int) R.drawable.notification_material_bg);
        this.mBackgroundDimmed.setCustomBackground((int) R.drawable.notification_material_bg_dim);
        this.mDimmedAlpha = Color.alpha(this.mContext.getColor(R.color.notification_material_background_dimmed_color));
        updateBackground();
        updateBackgroundTint();
        updateOutlineAlpha();
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!this.mNeedsDimming || this.mActivated || ev.getActionMasked() != 0 || !disallowSingleClick(ev) || isTouchExplorationEnabled()) {
            return super.onInterceptTouchEvent(ev);
        }
        return true;
    }

    private boolean isTouchExplorationEnabled() {
        return this.mAccessibilityManager.isTouchExplorationEnabled();
    }

    /* access modifiers changed from: protected */
    public boolean disallowSingleClick(MotionEvent ev) {
        return false;
    }

    /* access modifiers changed from: protected */
    public boolean handleSlideBack() {
        return false;
    }

    public boolean onTouchEvent(MotionEvent event) {
        boolean result;
        if (event.getAction() == 0) {
            this.mWasActivatedOnDown = this.mActivated;
        }
        if (!this.mNeedsDimming || this.mActivated || isTouchExplorationEnabled() || !isInteractive()) {
            result = (!isSummaryWithChildren() || !isGroupExpanded()) ? super.onTouchEvent(event) : true;
        } else {
            boolean wasActivated = this.mActivated;
            result = handleTouchEventDimmed(event);
            if (wasActivated && result && event.getAction() == 1) {
                removeCallbacks(this.mTapTimeoutRunnable);
            }
        }
        return result;
    }

    /* access modifiers changed from: protected */
    public boolean isInteractive() {
        return true;
    }

    public void drawableHotspotChanged(float x, float y) {
        if (!this.mDimmed) {
            this.mBackgroundNormal.drawableHotspotChanged(x, y);
        }
    }

    /* access modifiers changed from: protected */
    public void drawableStateChanged() {
        super.drawableStateChanged();
        if (this.mDimmed) {
            this.mBackgroundDimmed.setState(getDrawableState());
        } else {
            this.mBackgroundNormal.setState(getDrawableState());
        }
    }

    private boolean handleTouchEventDimmed(MotionEvent event) {
        if (this.mNeedsDimming && !this.mDimmed) {
            super.onTouchEvent(event);
        }
        return this.mDoubleTapHelper.onTouchEvent(event, getActualHeight());
    }

    public boolean performClick() {
        if (this.mWasActivatedOnDown || !this.mNeedsDimming || isTouchExplorationEnabled()) {
            return super.performClick();
        }
        return false;
    }

    /* access modifiers changed from: private */
    public void makeActive() {
        this.mFalsingManager.onNotificationActive();
        startActivateAnimation(false);
        this.mActivated = true;
        if (this.mOnActivatedListener != null) {
            this.mOnActivatedListener.onActivated(this);
        }
    }

    private void startActivateAnimation(boolean reverse) {
        AnimatorSet animator;
        Interpolator interpolator;
        if (isAttachedToWindow() && isDimmable()) {
            if (reverse) {
                ObjectAnimator scaleX = ObjectAnimator.ofFloat(this, "scaleX", new float[]{1.0f});
                ObjectAnimator scaleY = ObjectAnimator.ofFloat(this, "scaleY", new float[]{1.0f});
                animator = new AnimatorSet();
                animator.play(scaleX).with(scaleY);
            } else {
                ObjectAnimator scaleX2 = ObjectAnimator.ofFloat(this, "scaleX", new float[]{1.05f});
                ObjectAnimator scaleY2 = ObjectAnimator.ofFloat(this, "scaleY", new float[]{1.05f});
                animator = new AnimatorSet();
                animator.play(scaleX2).with(scaleY2);
            }
            if (!reverse) {
                interpolator = Interpolators.MIUI_ALPHA_IN;
            } else {
                interpolator = Interpolators.MIUI_ALPHA_OUT;
            }
            animator.setInterpolator(interpolator);
            animator.setDuration(180);
            if (reverse) {
                animator.addListener(new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator animation) {
                        ActivatableNotificationView.this.updateBackground();
                    }
                });
                animator.start();
            } else {
                animator.start();
            }
        }
    }

    public void makeInactive(boolean animate) {
        if (this.mActivated) {
            this.mActivated = false;
            if (this.mDimmed) {
                if (animate) {
                    startActivateAnimation(true);
                } else {
                    updateBackground();
                }
            }
        }
        if (this.mOnActivatedListener != null) {
            this.mOnActivatedListener.onActivationReset(this);
        }
        removeCallbacks(this.mTapTimeoutRunnable);
    }

    public void setDimmed(boolean dimmed, boolean fade) {
        this.mNeedsDimming = dimmed;
        boolean dimmed2 = dimmed & isDimmable();
        if (this.mDimmed != dimmed2) {
            this.mDimmed = dimmed2;
            resetBackgroundAlpha();
            if (fade) {
                fadeDimmedBackground();
            } else {
                updateBackground();
            }
        }
    }

    public boolean isDimmable() {
        return true;
    }

    public void setDark(boolean dark, boolean fade, long delay) {
        super.setDark(dark, fade, delay);
        if (this.mDark != dark) {
            this.mDark = dark;
            updateBackground();
            updateBackgroundTint(false);
            if (!dark && fade && !shouldHideBackground()) {
                fadeInFromDark(delay);
            }
            updateOutlineAlpha();
        }
    }

    /* access modifiers changed from: protected */
    public void updateOutlineAlpha() {
        if (this.mDark) {
            setOutlineAlpha(0.0f);
            return;
        }
        float alpha = (0.7f + ((1.0f - 0.7f) * this.mNormalBackgroundVisibilityAmount)) * this.mShadowAlpha;
        if (this.mFadeInFromDarkAnimator != null) {
            alpha *= this.mFadeInFromDarkAnimator.getAnimatedFraction();
        }
        setOutlineAlpha(alpha);
    }

    public void setNormalBackgroundVisibilityAmount(float normalBackgroundVisibilityAmount) {
        this.mNormalBackgroundVisibilityAmount = normalBackgroundVisibilityAmount;
        updateOutlineAlpha();
    }

    public void setBelowSpeedBump(boolean below) {
        super.setBelowSpeedBump(below);
        if (below != this.mIsBelowSpeedBump) {
            this.mIsBelowSpeedBump = below;
            updateBackgroundTint();
            onBelowSpeedBumpChanged();
        }
    }

    /* access modifiers changed from: protected */
    public void onBelowSpeedBumpChanged() {
    }

    public boolean isBelowSpeedBump() {
        return this.mIsBelowSpeedBump;
    }

    public void setTintColor(int color) {
        setTintColor(color, false);
    }

    public void setTintColor(int color, boolean animated) {
        if (color != this.mBgTint) {
            this.mBgTint = color;
            updateBackgroundTint(animated);
        }
    }

    public void setOverrideTintColor(int color, float overrideAmount) {
        if (this.mDark) {
            color = 0;
            overrideAmount = 0.0f;
        }
        this.mOverrideTint = color;
        this.mOverrideAmount = overrideAmount;
        setBackgroundTintColor(calculateBgColor());
        if (isDimmable() || !this.mNeedsDimming) {
            this.mBackgroundNormal.setDrawableAlpha(255);
        } else {
            this.mBackgroundNormal.setDrawableAlpha((int) NotificationUtils.interpolate(255.0f, (float) this.mDimmedAlpha, overrideAmount));
        }
    }

    /* access modifiers changed from: protected */
    public void updateBackgroundTint() {
        updateBackgroundTint(false);
    }

    private void updateBackgroundTint(boolean animated) {
        if (this.mBackgroundColorAnimator != null) {
            this.mBackgroundColorAnimator.cancel();
        }
        int rippleColor = getRippleColor();
        this.mBackgroundDimmed.setRippleColor(rippleColor);
        this.mBackgroundNormal.setRippleColor(rippleColor);
        int color = calculateBgColor();
        if (!animated) {
            setBackgroundTintColor(color);
        } else if (color != this.mCurrentBackgroundTint) {
            this.mStartTint = this.mCurrentBackgroundTint;
            this.mTargetTint = color;
            this.mBackgroundColorAnimator = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
            this.mBackgroundColorAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    ActivatableNotificationView.this.setBackgroundTintColor(NotificationUtils.interpolateColors(ActivatableNotificationView.this.mStartTint, ActivatableNotificationView.this.mTargetTint, animation.getAnimatedFraction()));
                }
            });
            this.mBackgroundColorAnimator.setDuration(360);
            this.mBackgroundColorAnimator.setInterpolator(Interpolators.LINEAR);
            this.mBackgroundColorAnimator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    ValueAnimator unused = ActivatableNotificationView.this.mBackgroundColorAnimator = null;
                }
            });
            this.mBackgroundColorAnimator.start();
        }
    }

    /* access modifiers changed from: private */
    public void setBackgroundTintColor(int color) {
        if (color != this.mCurrentBackgroundTint) {
            this.mCurrentBackgroundTint = color;
            if (color == this.mNormalColor) {
                color = 0;
            }
            this.mBackgroundDimmed.setTint(color);
            this.mBackgroundNormal.setTint(color);
        }
    }

    private void fadeInFromDark(long delay) {
        final View background = this.mDimmed ? this.mBackgroundDimmed : this.mBackgroundNormal;
        background.setAlpha(0.0f);
        this.mBackgroundVisibilityUpdater.onAnimationUpdate(null);
        background.animate().alpha(1.0f).setDuration(200).setStartDelay(delay).setInterpolator(Interpolators.ALPHA_IN).setListener(new AnimatorListenerAdapter() {
            public void onAnimationCancel(Animator animation) {
                background.setAlpha(1.0f);
            }
        }).setUpdateListener(this.mBackgroundVisibilityUpdater).start();
        this.mFadeInFromDarkAnimator = TimeAnimator.ofFloat(new float[]{0.0f, 1.0f});
        this.mFadeInFromDarkAnimator.setDuration(200);
        this.mFadeInFromDarkAnimator.setStartDelay(delay);
        this.mFadeInFromDarkAnimator.setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN);
        this.mFadeInFromDarkAnimator.addListener(this.mFadeInEndListener);
        this.mFadeInFromDarkAnimator.addUpdateListener(this.mUpdateOutlineListener);
        this.mFadeInFromDarkAnimator.start();
    }

    private void fadeDimmedBackground() {
        this.mBackgroundDimmed.animate().cancel();
        this.mBackgroundNormal.animate().cancel();
        if (this.mActivated) {
            updateBackground();
            return;
        }
        if (!shouldHideBackground()) {
            if (this.mDimmed) {
                this.mBackgroundDimmed.setVisibility(0);
            } else {
                this.mBackgroundNormal.setVisibility(0);
            }
        }
    }

    /* access modifiers changed from: protected */
    public void updateBackgroundAlpha(float transformationAmount) {
        this.mBgAlpha = (!isChildInGroup() || !this.mDimmed) ? 1.0f : transformationAmount;
        if (this.mDimmedBackgroundFadeInAmount != -1.0f) {
            this.mBgAlpha *= this.mDimmedBackgroundFadeInAmount;
        }
        this.mBackgroundDimmed.setAlpha(this.mBgAlpha);
    }

    /* access modifiers changed from: protected */
    public void resetBackgroundAlpha() {
        updateBackgroundAlpha(0.0f);
    }

    /* access modifiers changed from: protected */
    public void updateBackground() {
        cancelFadeAnimations();
        float f = 1.0f;
        int i = 4;
        if (shouldHideBackground()) {
            this.mBackgroundDimmed.setVisibility(4);
            NotificationBackgroundView notificationBackgroundView = this.mBackgroundNormal;
            if (this.mActivated) {
                i = 0;
            }
            notificationBackgroundView.setVisibility(i);
        } else if (this.mDimmed) {
            boolean dontShowDimmed = isGroupExpansionChanging() && isChildInGroup();
            this.mBackgroundDimmed.setVisibility(dontShowDimmed ? 4 : 0);
            NotificationBackgroundView notificationBackgroundView2 = this.mBackgroundNormal;
            if (this.mActivated || dontShowDimmed) {
                i = 0;
            }
            notificationBackgroundView2.setVisibility(i);
        } else {
            this.mBackgroundDimmed.setVisibility(4);
            this.mBackgroundNormal.setVisibility(0);
            this.mBackgroundNormal.setAlpha(1.0f);
            removeCallbacks(this.mTapTimeoutRunnable);
            makeInactive(false);
        }
        if (this.mBackgroundNormal.getVisibility() != 0) {
            f = 0.0f;
        }
        setNormalBackgroundVisibilityAmount(f);
    }

    /* access modifiers changed from: protected */
    public boolean shouldHideBackground() {
        return this.mDark;
    }

    private void cancelFadeAnimations() {
        if (this.mBackgroundAnimator != null) {
            this.mBackgroundAnimator.cancel();
        }
        this.mBackgroundDimmed.animate().cancel();
        this.mBackgroundNormal.animate().cancel();
        animate().cancel();
        setScaleX(1.0f);
        setScaleY(1.0f);
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        setPivotX((float) (getWidth() / 2));
    }

    public void setActualHeight(int actualHeight, boolean notifyListeners) {
        super.setActualHeight(actualHeight, notifyListeners);
        setPivotY((float) (actualHeight / 2));
        this.mBackgroundNormal.setActualHeight(actualHeight);
        this.mBackgroundDimmed.setActualHeight(actualHeight);
    }

    public void setClipTopAmount(int clipTopAmount) {
        super.setClipTopAmount(clipTopAmount);
        this.mBackgroundNormal.setClipTopAmount(clipTopAmount);
        this.mBackgroundDimmed.setClipTopAmount(clipTopAmount);
    }

    public void setClipBottomAmount(int clipBottomAmount) {
        super.setClipBottomAmount(clipBottomAmount);
        this.mBackgroundNormal.setClipBottomAmount(clipBottomAmount);
        this.mBackgroundDimmed.setClipBottomAmount(clipBottomAmount);
    }

    public void performRemoveAnimation(long duration, float translationDirection, AnimatorListenerAdapter globalListener, Runnable onFinishedRunnable) {
        enableAppearDrawing(true);
        if (this.mDrawingAppearAnimation) {
            startAppearAnimation(false, translationDirection, 0, duration, globalListener, onFinishedRunnable);
        } else if (onFinishedRunnable != null) {
            onFinishedRunnable.run();
        }
    }

    public void performAddAnimation(long delay, long duration, AnimatorListenerAdapter globalListener) {
        enableAppearDrawing(true);
        if (this.mDrawingAppearAnimation) {
            startAppearAnimation(true, NotificationUtil.getFoldTranslationDirection(true, -1.0f), delay, duration, globalListener, null);
        }
    }

    private void startAppearAnimation(final boolean isAppearing, float translationDirection, long delay, long duration, AnimatorListenerAdapter globalListener, final Runnable onFinishedRunnable) {
        cancelAppearAnimation();
        this.mAnimationTranslationY = ((float) getActualHeight()) * translationDirection;
        float targetValue = 0.0f;
        if (this.mAppearAnimationFraction == -1.0f) {
            if (isAppearing) {
                this.mAppearAnimationFraction = 0.0f;
                this.mAppearAnimationTranslation = this.mAnimationTranslationY;
            } else {
                this.mAppearAnimationFraction = 1.0f;
                this.mAppearAnimationTranslation = 0.0f;
            }
        }
        if (isAppearing) {
            this.mCurrentAppearInterpolator = this.mSlowOutFastInInterpolator;
            this.mCurrentAlphaInterpolator = Interpolators.LINEAR_OUT_SLOW_IN;
            targetValue = 1.0f;
        } else {
            this.mCurrentAppearInterpolator = Interpolators.FAST_OUT_SLOW_IN;
            this.mCurrentAlphaInterpolator = this.mSlowOutLinearInInterpolator;
        }
        float targetValue2 = targetValue;
        this.mAppearAnimator = ValueAnimator.ofFloat(new float[]{this.mAppearAnimationFraction, targetValue2});
        this.mAppearAnimator.setInterpolator(Interpolators.LINEAR);
        this.mAppearAnimator.setDuration((long) (((float) duration) * Math.abs(this.mAppearAnimationFraction - targetValue2)));
        this.mAppearAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                float unused = ActivatableNotificationView.this.mAppearAnimationFraction = ((Float) animation.getAnimatedValue()).floatValue();
                ActivatableNotificationView.this.updateAppearAnimationAlpha();
                ActivatableNotificationView.this.updateAppearRect();
                ActivatableNotificationView.this.invalidate();
            }
        });
        if (delay > 0) {
            updateAppearAnimationAlpha();
            updateAppearRect();
            this.mAppearAnimator.setStartDelay(delay);
        }
        if (globalListener != null) {
            this.mAppearAnimator.addListener(globalListener);
        }
        this.mAppearAnimator.addListener(new AnimatorListenerAdapter() {
            private boolean mWasCancelled;

            public void onAnimationEnd(Animator animation) {
                if (onFinishedRunnable != null) {
                    onFinishedRunnable.run();
                }
                if (!this.mWasCancelled) {
                    ActivatableNotificationView.this.enableAppearDrawing(false);
                    ActivatableNotificationView.this.onAppearAnimationFinished(isAppearing);
                }
            }

            public void onAnimationStart(Animator animation) {
                this.mWasCancelled = false;
            }

            public void onAnimationCancel(Animator animation) {
                this.mWasCancelled = true;
            }
        });
        this.mAppearAnimator.start();
    }

    /* access modifiers changed from: protected */
    public void onAppearAnimationFinished(boolean wasAppearing) {
    }

    private void cancelAppearAnimation() {
        if (this.mAppearAnimator != null) {
            this.mAppearAnimator.cancel();
            this.mAppearAnimator = null;
        }
    }

    /* access modifiers changed from: private */
    public void updateAppearRect() {
        float top;
        float bottom;
        float inverseFraction = 1.0f - this.mAppearAnimationFraction;
        float translateYTotalAmount = this.mAnimationTranslationY * this.mCurrentAppearInterpolator.getInterpolation(inverseFraction);
        this.mAppearAnimationTranslation = translateYTotalAmount;
        float left = ((float) getWidth()) * 0.475f * this.mCurrentAppearInterpolator.getInterpolation(Math.min(1.0f, Math.max(0.0f, (inverseFraction - 0.0f) / 0.8f)));
        float right = ((float) getWidth()) - left;
        float heightFraction = this.mCurrentAppearInterpolator.getInterpolation(Math.max(0.0f, (inverseFraction - 0.0f) / 1.0f));
        int actualHeight = getActualHeight();
        if (this.mAnimationTranslationY > 0.0f) {
            bottom = (((float) actualHeight) - ((this.mAnimationTranslationY * heightFraction) * 0.1f)) - translateYTotalAmount;
            top = bottom * heightFraction;
        } else {
            top = (((((float) actualHeight) + this.mAnimationTranslationY) * heightFraction) * 0.1f) - translateYTotalAmount;
            bottom = (top * heightFraction) + (((float) actualHeight) * (1.0f - heightFraction));
        }
        this.mAppearAnimationRect.set(left, top, right, bottom);
        setOutlineRect(left, this.mAppearAnimationTranslation + top, right, this.mAppearAnimationTranslation + bottom);
    }

    /* access modifiers changed from: private */
    public void updateAppearAnimationAlpha() {
        float contentAlphaProgress = this.mCurrentAlphaInterpolator.getInterpolation(Math.min(1.0f, this.mAppearAnimationFraction / 1.0f));
        setAlphaWithLayer(getContentView(), contentAlphaProgress);
        setAlphaWithLayer(this.mBackgroundNormal, contentAlphaProgress);
    }

    private void setAlphaWithLayer(View view, float contentAlpha) {
        int layerType;
        if (view.hasOverlappingRendering()) {
            if (contentAlpha == 0.0f || contentAlpha == 1.0f) {
                layerType = 0;
            } else {
                layerType = 2;
            }
            if (view.getLayerType() != layerType) {
                view.setLayerType(layerType, null);
            }
        }
        view.setAlpha(contentAlpha);
    }

    public int calculateBgColor() {
        return calculateBgColor(true, true);
    }

    private int calculateBgColor(boolean withTint, boolean withOverRide) {
        if (withTint && this.mDark) {
            return getContext().getColor(R.color.notification_material_background_dark_color);
        }
        if (withOverRide && this.mOverrideTint != 0) {
            return NotificationUtils.interpolateColors(calculateBgColor(withTint, false), this.mOverrideTint, this.mOverrideAmount);
        }
        if (withTint && this.mBgTint != 0) {
            return this.mBgTint;
        }
        if (this.mIsBelowSpeedBump) {
            return this.mLowPriorityColor;
        }
        return this.mNormalColor;
    }

    /* access modifiers changed from: protected */
    public int getRippleColor() {
        if (this.mBgTint != 0) {
            return this.mTintedRippleColor;
        }
        if (this.mIsBelowSpeedBump) {
            return this.mLowPriorityRippleColor;
        }
        return this.mNormalRippleColor;
    }

    /* access modifiers changed from: private */
    public void enableAppearDrawing(boolean enable) {
        if (enable != this.mDrawingAppearAnimation) {
            this.mDrawingAppearAnimation = enable;
            if (!enable) {
                setAlphaWithLayer(getContentView(), 1.0f);
                this.mAppearAnimationFraction = -1.0f;
                setOutlineRect(null);
            }
            invalidate();
        }
    }

    /* access modifiers changed from: protected */
    public void dispatchDraw(Canvas canvas) {
        if (this.mDrawingAppearAnimation) {
            canvas.save();
            canvas.translate(0.0f, this.mAppearAnimationTranslation);
        }
        super.dispatchDraw(canvas);
        if (this.mDrawingAppearAnimation) {
            canvas.restore();
        }
    }

    public void setOnActivatedListener(OnActivatedListener onActivatedListener) {
        this.mOnActivatedListener = onActivatedListener;
    }

    public float getShadowAlpha() {
        return this.mShadowAlpha;
    }

    public void setShadowAlpha(float shadowAlpha) {
        if (shadowAlpha != this.mShadowAlpha) {
            this.mShadowAlpha = shadowAlpha;
            updateOutlineAlpha();
        }
    }

    public void setFakeShadowIntensity(float shadowIntensity, float outlineAlpha, int shadowYEnd, int outlineTranslation) {
        boolean hiddenBefore = this.mShadowHidden;
        this.mShadowHidden = shadowIntensity == 0.0f;
        if (!this.mShadowHidden || !hiddenBefore) {
            this.mFakeShadow.setFakeShadowTranslationZ((getTranslationZ() + 0.1f) * shadowIntensity, outlineAlpha, shadowYEnd, outlineTranslation);
        }
    }

    public int getBackgroundColorWithoutTint() {
        return calculateBgColor(false, false);
    }
}
