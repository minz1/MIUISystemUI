package com.android.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.keyguard.MiuiKeyguardBaseClock;
import com.android.keyguard.MiuiKeyguardUtils;
import com.android.keyguard.analytics.AnalyticsHelper;
import com.android.keyguard.analytics.LockScreenMagazineAnalytics;
import com.android.keyguard.fod.MiuiGxzwManager;
import com.android.keyguard.magazine.LockScreenMagazinePreView;
import com.android.keyguard.magazine.LockScreenMagazineUtils;
import com.android.keyguard.wallpaper.WallpaperAuthorityUtils;
import com.android.systemui.R;
import com.android.systemui.classifier.FalsingManager;
import com.android.systemui.statusbar.FlingAnimationUtils;
import com.android.systemui.statusbar.KeyguardAffordanceView;
import java.util.ArrayList;
import java.util.List;
import miui.view.animation.CubicEaseOutInterpolator;

public class KeyguardMoveHelper {
    private Runnable mAnimationEndRunnable = new Runnable() {
        public void run() {
            KeyguardMoveHelper.this.mCallback.onAnimationToSideEnded();
        }
    };
    /* access modifiers changed from: private */
    public final Callback mCallback;
    private boolean mCanShowGxzw = true;
    private final Context mContext;
    /* access modifiers changed from: private */
    public int mCurrentScreen = 1;
    private FalsingManager mFalsingManager;
    private FlingAnimationUtils mFlingAnimationUtils;
    private AnimatorListenerAdapter mFlingEndListener = new AnimatorListenerAdapter() {
        public void onAnimationEnd(Animator animation) {
            Animator unused = KeyguardMoveHelper.this.mSwipeAnimator = null;
            boolean unused2 = KeyguardMoveHelper.this.mSwipingInProgress = false;
        }
    };
    private int mHintGrowAmount;
    private float mInitialTouchX;
    private float mInitialTouchY;
    /* access modifiers changed from: private */
    public boolean mIsLockScreenMagazinePreViewVisible;
    private int mKeyguardHorizontalGestureSlop;
    private KeyguardUpdateMonitor mKeyguardUpdateMonitor;
    private final KeyguardUpdateMonitorCallback mKeyguardUpdateMonitorCallback = new KeyguardUpdateMonitorCallback() {
        public void onLockScreenMagazinePreViewVisibilityChanged(boolean visible) {
            boolean unused = KeyguardMoveHelper.this.mIsLockScreenMagazinePreViewVisible = visible;
        }
    };
    private KeyguardAffordanceView mLeftIcon;
    private int mMinBackgroundRadius;
    private int mMinFlingVelocity;
    private int mMinTranslationAmount;
    private boolean mMotionCancelled;
    private KeyguardAffordanceView mRightIcon;
    /* access modifiers changed from: private */
    public Animator mSwipeAnimator;
    /* access modifiers changed from: private */
    public boolean mSwipingInProgress;
    private int mTouchSlop;
    private int mTouchTargetSize;
    /* access modifiers changed from: private */
    public float mTranslation;
    private float mTranslationOnDown;
    private VelocityTracker mVelocityTracker;

    public interface Callback {
        View getFaceUnlockView();

        KeyguardAffordanceView getLeftIcon();

        View getLeftView();

        View getLeftViewBg();

        List<View> getLockScreenView();

        float getMaxTranslationDistance();

        KeyguardAffordanceView getRightIcon();

        View getRightView();

        boolean isKeyguardWallpaperCarouselSwitchAnimating();

        boolean needsAntiFalsing();

        void onAnimationToSideEnded();

        void onAnimationToSideStarted(boolean z, float f, float f2);

        void onSwipingAborted();

        void onSwipingStarted();

        void startFaceUnlockByMove();

        void stopFaceUnlockByMove();

        void triggerAction(boolean z, float f, float f2);
    }

    KeyguardMoveHelper(Callback callback, Context context) {
        this.mContext = context;
        this.mCallback = callback;
        this.mKeyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        this.mKeyguardUpdateMonitor.registerCallback(this.mKeyguardUpdateMonitorCallback);
        this.mKeyguardHorizontalGestureSlop = context.getResources().getDimensionPixelSize(R.dimen.keyguard_horizontal_gesture_slop);
        initIcons();
        updateIcon(this.mLeftIcon, this.mLeftIcon.getRestingAlpha(), false, true);
        updateIcon(this.mRightIcon, this.mRightIcon.getRestingAlpha(), false, true);
        initDimens();
    }

    private void initDimens() {
        ViewConfiguration configuration = ViewConfiguration.get(this.mContext);
        this.mTouchSlop = configuration.getScaledPagingTouchSlop();
        this.mMinFlingVelocity = configuration.getScaledMinimumFlingVelocity();
        this.mMinTranslationAmount = this.mContext.getResources().getDimensionPixelSize(R.dimen.keyguard_min_swipe_amount);
        this.mMinBackgroundRadius = this.mContext.getResources().getDimensionPixelSize(R.dimen.keyguard_affordance_min_background_radius);
        this.mTouchTargetSize = this.mContext.getResources().getDimensionPixelSize(R.dimen.keyguard_affordance_touch_target_size);
        this.mHintGrowAmount = this.mContext.getResources().getDimensionPixelSize(R.dimen.hint_grow_amount_sideways);
        this.mFlingAnimationUtils = new FlingAnimationUtils(this.mContext, 0.4f);
        this.mFalsingManager = FalsingManager.getInstance(this.mContext);
    }

    private void initIcons() {
        this.mLeftIcon = this.mCallback.getLeftIcon();
        this.mRightIcon = this.mCallback.getRightIcon();
    }

    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        boolean z = false;
        if (this.mMotionCancelled && action != 0) {
            return false;
        }
        float y = event.getY();
        float x = event.getX();
        boolean isUp = false;
        if (action != 5) {
            switch (action) {
                case 0:
                    this.mInitialTouchX = x;
                    this.mInitialTouchY = y;
                    this.mTranslationOnDown = this.mTranslation;
                    initVelocityTracker();
                    trackMovement(event);
                    this.mMotionCancelled = false;
                    break;
                case 1:
                    isUp = true;
                    break;
                case 2:
                    trackMovement(event);
                    float xDist = x - this.mInitialTouchX;
                    float yDist = y - this.mInitialTouchY;
                    if (!this.mSwipingInProgress) {
                        if (Math.abs(xDist) > ((float) (this.mTouchSlop * 2)) || Math.abs(yDist) > ((float) (2 * this.mTouchSlop))) {
                            if (Math.abs(xDist) > Math.abs(yDist) && ((xDist > 0.0f && this.mLeftIcon.getVisibility() == 0) || (xDist < 0.0f && this.mRightIcon.getVisibility() == 0))) {
                                startSwiping();
                                setTranslation(xDist, false, false, false);
                                this.mCallback.stopFaceUnlockByMove();
                                break;
                            } else {
                                this.mMotionCancelled = true;
                                break;
                            }
                        }
                    } else {
                        setTranslation(xDist, false, false, false);
                        break;
                    }
                    break;
                case 3:
                    break;
            }
            trackMovement(event);
            if (!isUp) {
                z = true;
            }
            endMotion(z, x, y);
        } else {
            this.mMotionCancelled = true;
            endMotion(true, x, y);
        }
        return true;
    }

    private void startSwiping() {
        this.mCallback.onSwipingStarted();
        this.mSwipingInProgress = true;
    }

    public boolean isInLeftView() {
        return this.mCurrentScreen == 0;
    }

    public boolean isInCenterScreen() {
        return this.mCurrentScreen == 1;
    }

    public boolean isOnAffordanceIcon(float x, float y) {
        return isOnIcon(this.mLeftIcon, x, y) || isOnIcon(this.mRightIcon, x, y);
    }

    private boolean isOnIcon(View icon, float x, float y) {
        return Math.hypot((double) (x - (icon.getX() + (((float) icon.getWidth()) / 2.0f))), (double) (y - (icon.getY() + (((float) icon.getHeight()) / 2.0f)))) <= ((double) (this.mTouchTargetSize / 2));
    }

    private void endMotion(boolean forceSnapBack, float lastX, float lastY) {
        if (this.mSwipingInProgress) {
            flingWithCurrentVelocity(forceSnapBack, lastX, lastY);
        }
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.recycle();
            this.mVelocityTracker = null;
        }
    }

    private void cancelAnimation() {
        if (this.mSwipeAnimator != null) {
            this.mSwipeAnimator.cancel();
        }
    }

    private void flingWithCurrentVelocity(boolean forceSnapBack, float lastX, float lastY) {
        float vel = getCurrentVelocity(lastX, lastY);
        boolean snapBack = false;
        boolean z = true;
        if (this.mCallback.needsAntiFalsing() && this.mFalsingManager.isClassiferEnabled()) {
            snapBack = 0 != 0 || this.mFalsingManager.isFalseTouch();
        }
        boolean velIsInWrongDirection = this.mTranslation * vel < 0.0f;
        boolean snapBack2 = snapBack | ((Math.abs(vel) > ((float) this.mMinFlingVelocity) && velIsInWrongDirection) || Math.abs(this.mInitialTouchX - lastX) < ((float) this.mKeyguardHorizontalGestureSlop));
        float vel2 = snapBack2 ^ velIsInWrongDirection ? 0.0f : vel;
        boolean z2 = snapBack2 || forceSnapBack;
        if (this.mTranslation >= 0.0f) {
            z = false;
        }
        fling(vel2, z2, z);
    }

    private void fling(final float vel, final boolean snapBack, final boolean right) {
        ValueAnimator animator = ValueAnimator.ofFloat(new float[]{this.mTranslation, snapBack ? 0.0f : right ? -getScreenWidth() : getScreenWidth()});
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                float unused = KeyguardMoveHelper.this.mTranslation = ((Float) animation.getAnimatedValue()).floatValue();
                if (!snapBack) {
                    KeyguardMoveHelper.this.setTranslation(KeyguardMoveHelper.this.mTranslation, false, false, true);
                }
            }
        });
        animator.addListener(this.mFlingEndListener);
        animator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                if (snapBack) {
                    return;
                }
                if (KeyguardMoveHelper.this.mCurrentScreen == 2 || KeyguardMoveHelper.this.mCurrentScreen == 0) {
                    KeyguardMoveHelper.this.mCallback.triggerAction(right, KeyguardMoveHelper.this.mTranslation, vel);
                }
            }
        });
        if (snapBack) {
            reset(true);
        }
        animator.setDuration(100);
        animator.setInterpolator(new CubicEaseOutInterpolator());
        animator.start();
        this.mSwipeAnimator = animator;
        if (snapBack) {
            this.mCallback.onSwipingAborted();
        }
    }

    /* access modifiers changed from: private */
    public void setTranslation(float translation, boolean isReset, boolean animateReset, boolean force) {
        setTranslation(translation, isReset, animateReset, force, false);
    }

    private void setTranslation(float translation, boolean isReset, boolean animateReset, boolean force, boolean completeReset) {
        float f = translation;
        if (!this.mIsLockScreenMagazinePreViewVisible) {
            float realTranslation = f;
            float leftViewBgAlpha = 0.0f;
            View leftView = this.mCallback.getLeftView();
            View rightView = this.mCallback.getRightView();
            View leftViewBg = this.mCallback.getLeftViewBg();
            View faceUnlockView = this.mCallback.getFaceUnlockView();
            if (completeReset) {
                if (MiuiKeyguardUtils.isGxzwSensor() && this.mCurrentScreen != 1) {
                    setCanShowGxzw(true);
                }
                if (this.mCurrentScreen != 1) {
                    faceUnlockView.setTranslationX(0.0f);
                    faceUnlockView.setAlpha(1.0f);
                }
                this.mCurrentScreen = 1;
                leftView.setTranslationX(-getScreenWidth());
                rightView.setTranslationX(getScreenWidth());
                leftViewBg.setVisibility(4);
                animateShowLeftRightIcon();
                for (View view : this.mCallback.getLockScreenView()) {
                    view.setTranslationX(0.0f);
                    view.setAlpha(1.0f);
                }
                return;
            }
            if (this.mCurrentScreen == 1 && f > 0.0f) {
                leftViewBg.setVisibility(0);
                leftViewBgAlpha = f / getScreenWidth();
            } else if (this.mCurrentScreen == 0) {
                leftViewBgAlpha = 1.0f + (f / getScreenWidth());
            }
            float leftViewBgAlpha2 = leftViewBgAlpha < 0.0f ? 0.0f : leftViewBgAlpha > 1.0f ? 1.0f : leftViewBgAlpha;
            if (this.mCurrentScreen == 0) {
                if (f <= 0.0f) {
                    realTranslation += getScreenWidth();
                } else {
                    return;
                }
            }
            if (f != this.mTranslation || isReset || force) {
                if (!animateReset) {
                    leftView.setTranslationX(realTranslation - getScreenWidth());
                    rightView.setTranslationX(getScreenWidth() + realTranslation);
                    leftViewBg.setAlpha(leftViewBgAlpha2);
                    leftView.setAlpha(this.mKeyguardUpdateMonitor.isSupportLockScreenMagazineLeft() ? leftViewBgAlpha2 : 1.0f);
                    for (View view2 : this.mCallback.getLockScreenView()) {
                        view2.setTranslationX(realTranslation);
                        view2.setAlpha(1.0f - leftViewBgAlpha2);
                    }
                    faceUnlockView.setTranslationX(realTranslation);
                    faceUnlockView.setAlpha(1.0f - leftViewBgAlpha2);
                    View view3 = leftView;
                } else {
                    AnimatorSet animatorSet = new AnimatorSet();
                    List<Animator> animators = new ArrayList<>();
                    animators.add(ObjectAnimator.ofFloat(leftView, View.TRANSLATION_X, new float[]{leftView.getTranslationX(), realTranslation - getScreenWidth()}));
                    animators.add(ObjectAnimator.ofFloat(faceUnlockView, View.TRANSLATION_X, new float[]{faceUnlockView.getTranslationX(), realTranslation}));
                    animators.add(ObjectAnimator.ofFloat(rightView, View.TRANSLATION_X, new float[]{rightView.getTranslationX(), getScreenWidth() + realTranslation}));
                    animators.add(ObjectAnimator.ofFloat(leftViewBg, "alpha", new float[]{leftViewBg.getAlpha(), leftViewBgAlpha2}));
                    animators.add(ObjectAnimator.ofFloat(faceUnlockView, "alpha", new float[]{faceUnlockView.getAlpha(), 1.0f - leftViewBgAlpha2}));
                    if (this.mKeyguardUpdateMonitor.isSupportLockScreenMagazineLeft()) {
                        animators.add(ObjectAnimator.ofFloat(leftView, "alpha", new float[]{leftView.getAlpha(), leftViewBgAlpha2}));
                    }
                    for (View view4 : this.mCallback.getLockScreenView()) {
                        View leftView2 = leftView;
                        animators.add(ObjectAnimator.ofFloat(view4, View.TRANSLATION_X, new float[]{view4.getTranslationX(), realTranslation}));
                        if ((!(view4 instanceof LockScreenMagazinePreView) && !(view4 instanceof MiuiKeyguardBaseClock)) || !this.mCallback.isKeyguardWallpaperCarouselSwitchAnimating()) {
                            animators.add(ObjectAnimator.ofFloat(view4, "alpha", new float[]{view4.getAlpha(), 1.0f - leftViewBgAlpha2}));
                        }
                        leftView = leftView2;
                    }
                    animatorSet.playTogether(animators);
                    animatorSet.setDuration(300);
                    animatorSet.setInterpolator(new CubicEaseOutInterpolator());
                    animatorSet.start();
                }
                this.mTranslation = f;
            } else {
                View view5 = leftView;
            }
            if (this.mCurrentScreen == 0 && f == (-getScreenWidth())) {
                this.mCurrentScreen = 1;
                leftViewBg.setVisibility(4);
                if (MiuiKeyguardUtils.isGxzwSensor()) {
                    setCanShowGxzw(true);
                }
                this.mCallback.startFaceUnlockByMove();
                LockScreenMagazineUtils.sendLockScreenMagazineEventBrodcast(this.mContext, "Wallpaper_Uncovered");
            } else if (this.mCurrentScreen == 1 && f == (-getScreenWidth())) {
                this.mCurrentScreen = 2;
                if (MiuiKeyguardUtils.isGxzwSensor()) {
                    setCanShowGxzw(false);
                }
            } else if (this.mCurrentScreen == 1 && f == getScreenWidth()) {
                this.mCurrentScreen = 0;
                if (MiuiKeyguardUtils.isGxzwSensor()) {
                    setCanShowGxzw(false);
                }
                AnalyticsHelper.recordEnterLeftview(WallpaperAuthorityUtils.isLockScreenMagazineOpenedWallpaper());
                LockScreenMagazineAnalytics.recordNegativeStatus(this.mContext);
                this.mCallback.stopFaceUnlockByMove();
                if (!this.mKeyguardUpdateMonitor.isSupportLockScreenMagazineLeft()) {
                    LockScreenMagazineUtils.sendLockScreenMagazineEventBrodcast(this.mContext, "Wallpaper_Covered");
                }
            } else if (this.mCurrentScreen == 1 && f == 0.0f) {
                if (MiuiKeyguardUtils.isGxzwSensor()) {
                    setCanShowGxzw(true);
                }
            } else if (MiuiKeyguardUtils.isGxzwSensor()) {
                setCanShowGxzw(false);
            }
        }
    }

    private float getScreenWidth() {
        return (float) this.mContext.getResources().getDisplayMetrics().widthPixels;
    }

    public void animateHideLeftRightIcon() {
        cancelAnimation();
        updateIcon(this.mRightIcon, 0.0f, true, false);
        updateIcon(this.mLeftIcon, 0.0f, true, false);
    }

    public void animateShowLeftRightIcon() {
        cancelAnimation();
        updateIcon(this.mRightIcon, this.mRightIcon.getRestingAlpha(), true, false);
        updateIcon(this.mLeftIcon, this.mLeftIcon.getRestingAlpha(), true, false);
    }

    private void updateIcon(KeyguardAffordanceView view, float alpha, boolean animate, boolean force) {
        if (view.getVisibility() == 0 || force) {
            updateIconAlpha(view, alpha, animate);
        }
    }

    private void updateIconAlpha(KeyguardAffordanceView view, float alpha, boolean animate) {
        float scale = getScale(alpha, view);
        view.setImageAlpha(Math.min(1.0f, alpha), animate);
        view.setImageScale(scale, animate);
    }

    private float getScale(float alpha, KeyguardAffordanceView icon) {
        return Math.min(((alpha / icon.getRestingAlpha()) * 0.2f) + 0.8f, 1.5f);
    }

    private void trackMovement(MotionEvent event) {
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.addMovement(event);
        }
    }

    private void initVelocityTracker() {
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.recycle();
        }
        this.mVelocityTracker = VelocityTracker.obtain();
    }

    private float getCurrentVelocity(float lastX, float lastY) {
        if (this.mVelocityTracker == null) {
            return 0.0f;
        }
        this.mVelocityTracker.computeCurrentVelocity(1000);
        return this.mVelocityTracker.getXVelocity();
    }

    public void onConfigurationChanged() {
        initDimens();
        initIcons();
    }

    public void onRtlPropertiesChanged() {
        initIcons();
    }

    public void reset(boolean animate) {
        reset(animate, false);
    }

    private void reset(boolean animate, boolean force, boolean completeReset) {
        cancelAnimation();
        setTranslation(0.0f, true, animate, force, completeReset);
        this.mMotionCancelled = true;
        if (this.mSwipingInProgress) {
            this.mCallback.onSwipingAborted();
            this.mSwipingInProgress = false;
        }
    }

    public void updateKeyguardHorizontalSwpingSlop(boolean isInSuspectMode) {
        if (isInSuspectMode) {
            this.mKeyguardHorizontalGestureSlop = this.mContext.getResources().getDimensionPixelSize(R.dimen.keyguard_horizontal_gesture_slop_in_suspected_mode);
        } else {
            this.mKeyguardHorizontalGestureSlop = this.mContext.getResources().getDimensionPixelSize(R.dimen.keyguard_horizontal_gesture_slop);
        }
    }

    public void reset(boolean animate, boolean force) {
        reset(animate, force, false);
    }

    public void resetImmediately() {
        reset(false, true, true);
    }

    public boolean isSwipingInProgress() {
        return this.mSwipingInProgress;
    }

    public void launchAffordance(boolean animate, boolean left) {
        float f;
        if (!this.mSwipingInProgress) {
            KeyguardAffordanceView targetView = left ? this.mLeftIcon : this.mRightIcon;
            KeyguardAffordanceView otherView = left ? this.mRightIcon : this.mLeftIcon;
            if (animate) {
                fling(0.0f, false, !left);
                updateIcon(otherView, 0.0f, true, true);
            } else {
                this.mCallback.onAnimationToSideStarted(!left, this.mTranslation, 0.0f);
                if (left) {
                    f = this.mCallback.getMaxTranslationDistance();
                } else {
                    f = this.mCallback.getMaxTranslationDistance();
                }
                this.mTranslation = f;
                updateIcon(otherView, 0.0f, false, true);
                targetView.instantFinishAnimation();
                this.mFlingEndListener.onAnimationEnd(null);
                this.mAnimationEndRunnable.run();
            }
        }
    }

    private void setCanShowGxzw(boolean show) {
        this.mCanShowGxzw = show;
        if (MiuiKeyguardUtils.isGxzwSensor()) {
            MiuiGxzwManager.getInstance().updateGxzwState();
        }
    }

    public boolean canShowGxzw() {
        return this.mCanShowGxzw;
    }
}
