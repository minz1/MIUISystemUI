package com.android.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.support.v4.graphics.ColorUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.R;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.ScrimView;
import com.android.systemui.statusbar.policy.OnHeadsUpChangedListener;
import com.android.systemui.statusbar.stack.ViewState;

public class ScrimController implements ViewTreeObserver.OnPreDrawListener, OnHeadsUpChangedListener {
    public static final Interpolator KEYGUARD_FADE_OUT_INTERPOLATOR = new PathInterpolator(0.0f, 0.0f, 0.7f, 1.0f);
    public static final Interpolator KEYGUARD_FADE_OUT_INTERPOLATOR_LOCKED = new PathInterpolator(0.3f, 0.0f, 0.8f, 1.0f);
    protected boolean mAnimateChange;
    private boolean mAnimateKeyguardFadingOut;
    private long mAnimationDelay;
    protected boolean mBouncerIsKeyguard = false;
    protected boolean mBouncerShowing;
    private float mCurrentBehindAlpha = -1.0f;
    private float mCurrentHeadsUpAlpha = -1.0f;
    private float mCurrentInFrontAlpha = -1.0f;
    private boolean mDarkenWhileDragging;
    private boolean mDontAnimateBouncerChanges;
    private float mDozeBehindAlpha;
    private float mDozeInFrontAlpha;
    private boolean mDozing;
    private View mDraggedHeadsUpView;
    protected long mDurationOverride = -1;
    private boolean mForceHideScrims;
    private float mFraction;
    private final View mHeadsUpScrim;
    private final Interpolator mInterpolator = new DecelerateInterpolator();
    /* access modifiers changed from: private */
    public ValueAnimator mKeyguardFadeoutAnimation;
    /* access modifiers changed from: private */
    public boolean mKeyguardFadingOutInProgress;
    protected boolean mKeyguardShowing;
    private final KeyguardUpdateMonitor mKeyguardUpdateMonitor;
    private final LightBarController mLightBarController;
    /* access modifiers changed from: private */
    public Runnable mOnAnimationFinished;
    private int mPinnedHeadsUpCount;
    protected final ScrimView mScrimBehind;
    protected float mScrimBehindAlpha;
    protected float mScrimBehindAlphaKeyguard = 0.45f;
    protected float mScrimBehindAlphaUnlocking = 0.2f;
    private final ScrimView mScrimInFront;
    private boolean mSkipFirstFrame;
    private float mTopHeadsUpDragAmount;
    private boolean mTracking;
    private final UnlockMethodCache mUnlockMethodCache;
    private boolean mUpdatePending;
    private boolean mWakeAndUnlocking;

    public ScrimController(LightBarController lightBarController, ScrimView scrimBehind, ScrimView scrimInFront, View headsUpScrim) {
        this.mScrimBehind = scrimBehind;
        this.mScrimInFront = scrimInFront;
        this.mHeadsUpScrim = headsUpScrim;
        Context context = scrimBehind.getContext();
        this.mUnlockMethodCache = UnlockMethodCache.getInstance(context);
        this.mKeyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(context);
        this.mLightBarController = lightBarController;
        this.mScrimBehindAlpha = context.getResources().getFloat(R.dimen.scrim_behind_alpha);
        updateHeadsUpScrim(false);
        updateScrims();
    }

    public void setKeyguardShowing(boolean showing) {
        this.mKeyguardShowing = showing;
        scheduleUpdate();
    }

    public void onTrackingStarted() {
        this.mTracking = true;
        this.mDarkenWhileDragging = true ^ this.mUnlockMethodCache.canSkipBouncer();
    }

    public void onExpandingFinished() {
        this.mTracking = false;
    }

    public void setPanelExpansion(float fraction) {
        if (this.mFraction != fraction) {
            this.mFraction = fraction;
            scheduleUpdate();
            if (this.mPinnedHeadsUpCount != 0) {
                updateHeadsUpScrim(false);
            }
            if (this.mKeyguardFadeoutAnimation != null && this.mTracking) {
                this.mKeyguardFadeoutAnimation.cancel();
            }
        }
    }

    public void setBouncerShowing(boolean showing) {
        this.mBouncerShowing = showing;
        this.mAnimateChange = !this.mTracking && !this.mDontAnimateBouncerChanges;
        scheduleUpdate();
    }

    public void setWakeAndUnlocking() {
        this.mWakeAndUnlocking = true;
        scheduleUpdate();
    }

    public void animateKeyguardFadingOut(long delay, long duration, Runnable onAnimationFinished, boolean skipFirstFrame) {
        this.mWakeAndUnlocking = false;
        this.mAnimateKeyguardFadingOut = true;
        this.mDurationOverride = duration;
        this.mAnimationDelay = delay;
        this.mAnimateChange = true;
        this.mSkipFirstFrame = skipFirstFrame;
        this.mOnAnimationFinished = onAnimationFinished;
        if (!this.mKeyguardUpdateMonitor.needsSlowUnlockTransition()) {
            scheduleUpdate();
            onPreDraw();
            return;
        }
        this.mScrimInFront.postOnAnimationDelayed(new Runnable() {
            public void run() {
                ScrimController.this.scheduleUpdate();
            }
        }, 16);
    }

    public void abortKeyguardFadingOut() {
        if (this.mAnimateKeyguardFadingOut) {
            endAnimateKeyguardFadingOut(true);
        }
    }

    public void animateGoingToFullShade(long delay, long duration) {
        this.mDurationOverride = duration;
        this.mAnimationDelay = delay;
        this.mAnimateChange = true;
        scheduleUpdate();
    }

    public void setDozing(boolean dozing) {
        if (this.mDozing != dozing) {
            this.mDozing = dozing;
            scheduleUpdate();
        }
    }

    public void setDozeInFrontAlpha(float alpha) {
        this.mDozeInFrontAlpha = alpha;
        updateScrimColor(this.mScrimInFront);
    }

    public void setDozeBehindAlpha(float alpha) {
        this.mDozeBehindAlpha = alpha;
        updateScrimColor(this.mScrimBehind);
    }

    public float getDozeBehindAlpha() {
        return this.mDozeBehindAlpha;
    }

    public float getDozeInFrontAlpha() {
        return this.mDozeInFrontAlpha;
    }

    private float getScrimInFrontAlpha() {
        if (this.mKeyguardUpdateMonitor.needsSlowUnlockTransition()) {
            return 0.85f;
        }
        return 0.75f;
    }

    /* access modifiers changed from: protected */
    public void scheduleUpdate() {
        if (!this.mUpdatePending) {
            this.mScrimBehind.invalidate();
            this.mScrimBehind.getViewTreeObserver().addOnPreDrawListener(this);
            this.mUpdatePending = true;
        }
    }

    /* access modifiers changed from: protected */
    public void updateScrims() {
        if (this.mAnimateKeyguardFadingOut || this.mForceHideScrims) {
            setScrimInFrontColor(0.0f);
            setScrimBehindColor(0.0f);
        } else if (this.mWakeAndUnlocking) {
            if (this.mDozing) {
                setScrimInFrontColor(0.0f);
                setScrimBehindColor(1.0f);
            } else {
                setScrimInFrontColor(1.0f);
                setScrimBehindColor(0.0f);
            }
        } else if (this.mKeyguardShowing || this.mBouncerShowing) {
            updateScrimKeyguard();
        } else {
            updateScrimNormal();
            setScrimInFrontColor(0.0f);
        }
        this.mAnimateChange = false;
    }

    private void updateScrimKeyguard() {
        if (this.mTracking && this.mDarkenWhileDragging) {
            float behindFraction = Math.max(0.0f, Math.min(this.mFraction, 1.0f));
            float fraction = (float) Math.pow((double) (1.0f - behindFraction), 0.800000011920929d);
            float behindFraction2 = (float) Math.pow((double) behindFraction, 0.800000011920929d);
            setScrimInFrontColor(getScrimInFrontAlpha() * fraction);
            setScrimBehindColor(this.mScrimBehindAlphaKeyguard * behindFraction2);
        } else if (this.mBouncerShowing && !this.mBouncerIsKeyguard) {
            setScrimInFrontColor(getScrimInFrontAlpha());
            updateScrimNormal();
        } else if (this.mBouncerShowing) {
            setScrimInFrontColor(0.0f);
            setScrimBehindColor(this.mScrimBehindAlpha);
        } else {
            float fraction2 = Math.max(0.0f, Math.min(this.mFraction, 1.0f));
            setScrimInFrontColor(0.0f);
            setScrimBehindColor(((this.mScrimBehindAlphaKeyguard - this.mScrimBehindAlphaUnlocking) * fraction2) + this.mScrimBehindAlphaUnlocking);
        }
    }

    private void updateScrimNormal() {
        float frac = (1.2f * this.mFraction) - 0.2f;
        if (frac <= 0.0f) {
            setScrimBehindColor(0.0f);
            return;
        }
        setScrimBehindColor(this.mScrimBehindAlpha * ((float) (1.0d - (0.5d * (1.0d - Math.cos(3.141590118408203d * Math.pow((double) (1.0f - frac), 2.0d)))))));
    }

    private void setScrimBehindColor(float alpha) {
        setScrimColor(this.mScrimBehind, alpha);
    }

    private void setScrimInFrontColor(float alpha) {
        setScrimColor(this.mScrimInFront, alpha);
        if (alpha == 0.0f) {
            this.mScrimInFront.setClickable(false);
        } else {
            this.mScrimInFront.setClickable(!this.mDozing);
        }
    }

    private void setScrimColor(View scrim, float alpha) {
        updateScrim(this.mAnimateChange, scrim, alpha, getCurrentScrimAlpha(scrim));
    }

    /* access modifiers changed from: protected */
    public float getDozeAlpha(View scrim) {
        return scrim == this.mScrimBehind ? this.mDozeBehindAlpha : this.mDozeInFrontAlpha;
    }

    /* access modifiers changed from: protected */
    public float getCurrentScrimAlpha(View scrim) {
        if (scrim == this.mScrimBehind) {
            return this.mCurrentBehindAlpha;
        }
        if (scrim == this.mScrimInFront) {
            return this.mCurrentInFrontAlpha;
        }
        return this.mCurrentHeadsUpAlpha;
    }

    /* access modifiers changed from: private */
    public void setCurrentScrimAlpha(View scrim, float alpha) {
        if (scrim == this.mScrimBehind) {
            this.mCurrentBehindAlpha = alpha;
            this.mLightBarController.setScrimAlpha(this.mCurrentBehindAlpha);
        } else if (scrim == this.mScrimInFront) {
            this.mCurrentInFrontAlpha = alpha;
        } else {
            this.mCurrentHeadsUpAlpha = Math.max(0.0f, Math.min(1.0f, alpha));
        }
    }

    /* access modifiers changed from: protected */
    public void updateScrimColor(View scrim) {
        float alpha1 = getCurrentScrimAlpha(scrim);
        if (scrim instanceof ScrimView) {
            ((ScrimView) scrim).setScrimColor(ColorUtils.setAlphaComponent(((ScrimView) scrim).getScrimColor(), (int) (255.0f * Math.max(0.0f, Math.min(1.0f, 1.0f - ((1.0f - alpha1) * (1.0f - getDozeAlpha(scrim))))))));
            return;
        }
        scrim.setAlpha(alpha1);
    }

    private void startScrimAnimation(final View scrim, float target) {
        ValueAnimator anim = ValueAnimator.ofFloat(new float[]{getCurrentScrimAlpha(scrim), target});
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                ScrimController.this.setCurrentScrimAlpha(scrim, ((Float) animation.getAnimatedValue()).floatValue());
                ScrimController.this.updateScrimColor(scrim);
            }
        });
        anim.setInterpolator(getInterpolator());
        anim.setStartDelay(this.mAnimationDelay);
        anim.setDuration(this.mDurationOverride != -1 ? this.mDurationOverride : 220);
        anim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                if (ScrimController.this.mOnAnimationFinished != null) {
                    ScrimController.this.mOnAnimationFinished.run();
                    Runnable unused = ScrimController.this.mOnAnimationFinished = null;
                }
                if (ScrimController.this.mKeyguardFadingOutInProgress) {
                    ValueAnimator unused2 = ScrimController.this.mKeyguardFadeoutAnimation = null;
                    boolean unused3 = ScrimController.this.mKeyguardFadingOutInProgress = false;
                }
                scrim.setTag(R.id.scrim, null);
                scrim.setTag(R.id.scrim_target, null);
            }
        });
        anim.start();
        if (this.mAnimateKeyguardFadingOut) {
            this.mKeyguardFadingOutInProgress = true;
            this.mKeyguardFadeoutAnimation = anim;
        }
        if (this.mSkipFirstFrame) {
            anim.setCurrentPlayTime(16);
        }
        scrim.setTag(R.id.scrim, anim);
        scrim.setTag(R.id.scrim_target, Float.valueOf(target));
    }

    /* access modifiers changed from: protected */
    public Interpolator getInterpolator() {
        if (this.mAnimateKeyguardFadingOut && this.mKeyguardUpdateMonitor.needsSlowUnlockTransition()) {
            return KEYGUARD_FADE_OUT_INTERPOLATOR_LOCKED;
        }
        if (this.mAnimateKeyguardFadingOut) {
            return KEYGUARD_FADE_OUT_INTERPOLATOR;
        }
        return this.mInterpolator;
    }

    public boolean onPreDraw() {
        this.mScrimBehind.getViewTreeObserver().removeOnPreDrawListener(this);
        this.mUpdatePending = false;
        if (this.mDontAnimateBouncerChanges) {
            this.mDontAnimateBouncerChanges = false;
        }
        updateScrims();
        this.mDurationOverride = -1;
        this.mAnimationDelay = 0;
        this.mSkipFirstFrame = false;
        endAnimateKeyguardFadingOut(false);
        return true;
    }

    private void endAnimateKeyguardFadingOut(boolean force) {
        this.mAnimateKeyguardFadingOut = false;
        if (force || (!isAnimating(this.mScrimInFront) && !isAnimating(this.mScrimBehind))) {
            if (this.mOnAnimationFinished != null) {
                this.mOnAnimationFinished.run();
                this.mOnAnimationFinished = null;
            }
            this.mKeyguardFadingOutInProgress = false;
        }
    }

    private boolean isAnimating(View scrim) {
        return scrim.getTag(R.id.scrim) != null;
    }

    public void setDrawBehindAsSrc(boolean asSrc) {
        this.mScrimBehind.setDrawAsSrc(asSrc);
    }

    public void onHeadsUpPinnedModeChanged(boolean inPinnedMode) {
    }

    public void onHeadsUpPinned(ExpandableNotificationRow headsUp) {
        this.mPinnedHeadsUpCount++;
        updateHeadsUpScrim(true);
    }

    public void onHeadsUpUnPinned(ExpandableNotificationRow headsUp) {
        this.mPinnedHeadsUpCount--;
        if (headsUp == this.mDraggedHeadsUpView) {
            this.mDraggedHeadsUpView = null;
            this.mTopHeadsUpDragAmount = 0.0f;
        }
        updateHeadsUpScrim(true);
    }

    public void onHeadsUpStateChanged(NotificationData.Entry entry, boolean isHeadsUp) {
    }

    private void updateHeadsUpScrim(boolean animate) {
        updateScrim(animate, this.mHeadsUpScrim, calculateHeadsUpAlpha(), this.mCurrentHeadsUpAlpha);
    }

    private void updateScrim(boolean animate, View scrim, float alpha, float currentAlpha) {
        View view = scrim;
        float f = alpha;
        if (!this.mKeyguardFadingOutInProgress || this.mKeyguardFadeoutAnimation.getCurrentPlayTime() == 0) {
            ValueAnimator previousAnimator = (ValueAnimator) ViewState.getChildTag(view, R.id.scrim);
            float animEndValue = -1.0f;
            if (previousAnimator != null) {
                if (animate || f == currentAlpha) {
                    previousAnimator.cancel();
                } else {
                    animEndValue = ((Float) ViewState.getChildTag(view, R.id.scrim_alpha_end)).floatValue();
                }
            }
            if (!(f == currentAlpha || f == animEndValue)) {
                if (animate) {
                    startScrimAnimation(view, f);
                    view.setTag(R.id.scrim_alpha_start, Float.valueOf(currentAlpha));
                    view.setTag(R.id.scrim_alpha_end, Float.valueOf(alpha));
                } else if (previousAnimator != null) {
                    float previousStartValue = ((Float) ViewState.getChildTag(view, R.id.scrim_alpha_start)).floatValue();
                    float previousEndValue = ((Float) ViewState.getChildTag(view, R.id.scrim_alpha_end)).floatValue();
                    PropertyValuesHolder[] values = previousAnimator.getValues();
                    float newStartValue = Math.max(0.0f, Math.min(1.0f, previousStartValue + (f - previousEndValue)));
                    values[0].setFloatValues(new float[]{newStartValue, f});
                    view.setTag(R.id.scrim_alpha_start, Float.valueOf(newStartValue));
                    view.setTag(R.id.scrim_alpha_end, Float.valueOf(alpha));
                    previousAnimator.setCurrentPlayTime(previousAnimator.getCurrentPlayTime());
                } else {
                    setCurrentScrimAlpha(view, f);
                    updateScrimColor(view);
                }
            }
        }
    }

    private float calculateHeadsUpAlpha() {
        float alpha;
        if (this.mPinnedHeadsUpCount >= 2) {
            alpha = 1.0f;
        } else if (this.mPinnedHeadsUpCount == 0) {
            alpha = 0.0f;
        } else {
            alpha = 1.0f - this.mTopHeadsUpDragAmount;
        }
        return alpha * Math.max(1.0f - this.mFraction, 0.0f);
    }

    public void forceHideScrims(boolean hide) {
        this.mForceHideScrims = hide;
        this.mAnimateChange = false;
        scheduleUpdate();
    }

    public void dontAnimateBouncerChangesUntilNextFrame() {
        this.mDontAnimateBouncerChanges = true;
    }

    public void setExcludedBackgroundArea(Rect area) {
        this.mScrimBehind.setExcludedArea(area);
    }

    public int getScrimBehindColor() {
        return this.mScrimBehind.getScrimColorWithAlpha();
    }

    public void setScrimBehindChangeRunnable(Runnable changeRunnable) {
        this.mScrimBehind.setChangeRunnable(changeRunnable);
    }

    public void onDensityOrFontScaleChanged() {
        ViewGroup.LayoutParams layoutParams = this.mHeadsUpScrim.getLayoutParams();
        layoutParams.height = this.mHeadsUpScrim.getResources().getDimensionPixelSize(R.dimen.heads_up_scrim_height);
        this.mHeadsUpScrim.setLayoutParams(layoutParams);
    }

    public void setCurrentUser(int currentUser) {
    }
}
