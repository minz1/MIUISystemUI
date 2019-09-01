package com.android.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import com.android.keyguard.MiuiKeyguardFaceUnlockView;
import com.android.systemui.R;

public class KeyguardVerticalMoveHelper {
    private ValueAnimator mAnimator;
    private final Context mContext;
    /* access modifiers changed from: private */
    public View mFaceUnlockView;
    private long mInitialDownTime;
    private float mInitialTouchX;
    /* access modifiers changed from: private */
    public float mInitialTouchY;
    /* access modifiers changed from: private */
    public View mKeyguardClockView;
    private int mKeyguardVerticalGestureSlop;
    /* access modifiers changed from: private */
    public View mNotificationStackScroller;
    private NotificationPanelView mPanelView;
    private int mTouchSlop;
    private boolean mTracking;
    private VelocityTracker mVelocityTracker;

    KeyguardVerticalMoveHelper(Context context, NotificationPanelView panelView, View clock, View noti, MiuiKeyguardFaceUnlockView faceView) {
        this.mContext = context;
        this.mKeyguardClockView = clock;
        this.mNotificationStackScroller = noti;
        this.mPanelView = panelView;
        this.mFaceUnlockView = faceView;
        initDimens();
    }

    private void initDimens() {
        this.mTouchSlop = ViewConfiguration.get(this.mContext).getScaledPagingTouchSlop();
        this.mKeyguardVerticalGestureSlop = this.mContext.getResources().getDimensionPixelSize(R.dimen.keyguard_vertical_gesture_slop);
    }

    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        float y = event.getY();
        float x = event.getX();
        if (action != 0 && !this.mTracking) {
            return false;
        }
        switch (action) {
            case 0:
                this.mInitialTouchX = x;
                this.mInitialTouchY = y;
                this.mTracking = true;
                this.mInitialDownTime = SystemClock.uptimeMillis();
                initVelocityTracker();
                trackMovement(event);
                break;
            case 1:
            case 3:
                trackMovement(event);
                this.mTracking = false;
                endMotionEvent(event, x, y, false);
                break;
            case 2:
                trackMovement(event);
                float translation = event.getY() - this.mInitialTouchY;
                float f = 0.0f;
                if (translation <= 0.0f) {
                    f = translation;
                }
                float translation2 = f;
                this.mKeyguardClockView.setTranslationY(translation2);
                this.mNotificationStackScroller.setTranslationY(translation2);
                this.mFaceUnlockView.setTranslationY(translation2);
                break;
            case 5:
                endMotionEvent(event, x, y, true);
                return false;
        }
        return true;
    }

    private void endMotionEvent(MotionEvent event, float x, float y, boolean forceCancel) {
        float vel = 0.0f;
        float vectorVel = 0.0f;
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.computeCurrentVelocity(1000);
            vel = this.mVelocityTracker.getYVelocity();
            vectorVel = (float) Math.hypot((double) this.mVelocityTracker.getXVelocity(), (double) this.mVelocityTracker.getYVelocity());
        }
        if (this.mPanelView.flingExpands(vel, vectorVel, x, y) || event.getActionMasked() == 3 || forceCancel) {
            handleMoveDownEvent(event);
        } else {
            this.mAnimator = ValueAnimator.ofFloat(new float[]{y, (float) (-this.mPanelView.getHeight())});
            this.mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    float translation = ((Float) animation.getAnimatedValue()).floatValue() - KeyguardVerticalMoveHelper.this.mInitialTouchY;
                    float f = 0.0f;
                    if (translation <= 0.0f) {
                        f = translation;
                    }
                    float translation2 = f;
                    KeyguardVerticalMoveHelper.this.mKeyguardClockView.setTranslationY(translation2);
                    KeyguardVerticalMoveHelper.this.mNotificationStackScroller.setTranslationY(translation2);
                    KeyguardVerticalMoveHelper.this.mFaceUnlockView.setTranslationY(translation2);
                }
            });
            this.mAnimator.setDuration(200);
            this.mAnimator.start();
        }
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.recycle();
            this.mVelocityTracker = null;
        }
    }

    public void reset() {
        if (this.mAnimator != null) {
            this.mAnimator.cancel();
        }
        this.mKeyguardClockView.setTranslationY(0.0f);
        this.mNotificationStackScroller.setTranslationY(0.0f);
        this.mFaceUnlockView.setTranslationY(0.0f);
    }

    private void handleMoveDownEvent(MotionEvent ev) {
        float translationYOfConcernedView = this.mKeyguardClockView.getTranslationY();
        startViewBounceDownAnimation(this.mKeyguardClockView, (int) (-translationYOfConcernedView));
        startViewBounceDownAnimation(this.mNotificationStackScroller, (int) (-translationYOfConcernedView));
        startViewBounceDownAnimation(this.mFaceUnlockView, (int) (-translationYOfConcernedView));
    }

    private void startViewBounceDownAnimation(View view, int offset) {
        Animator downAnimator = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, new float[]{(float) (-offset), 0.0f});
        downAnimator.setDuration(400);
        downAnimator.setInterpolator(new BounceInterpolator());
        downAnimator.start();
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

    public void updateKeyguardVerticalSwpingSlop(boolean isInSuspectMode) {
        if (isInSuspectMode) {
            this.mKeyguardVerticalGestureSlop = this.mContext.getResources().getDimensionPixelSize(R.dimen.keyguard_vertical_gesture_slop_in_suspected_mode);
        } else {
            this.mKeyguardVerticalGestureSlop = this.mContext.getResources().getDimensionPixelSize(R.dimen.keyguard_vertical_gesture_slop);
        }
    }
}
