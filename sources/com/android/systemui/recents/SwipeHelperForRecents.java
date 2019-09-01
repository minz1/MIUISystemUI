package com.android.systemui.recents;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.RectF;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import java.util.HashMap;

public class SwipeHelperForRecents {
    private int DEFAULT_ESCAPE_ANIMATION_DURATION = 300;
    private int MAX_DISMISS_VELOCITY = 4000;
    private int MAX_ESCAPE_ANIMATION_DURATION = 300;
    private float SWIPE_ESCAPE_VELOCITY = 100.0f;
    /* access modifiers changed from: private */
    public Callback mCallback;
    private boolean mCanCurrViewBeDimissed;
    /* access modifiers changed from: private */
    public View mCurrView;
    /* access modifiers changed from: private */
    public Animator mCurrentAnim;
    private float mDensityScale;
    /* access modifiers changed from: private */
    public boolean mDisableHwLayers;
    /* access modifiers changed from: private */
    public HashMap<View, Animator> mDismissPendingMap = new HashMap<>();
    private boolean mDragging;
    private int mFalsingThreshold;
    private Handler mHandler;
    private float mInitialTouchPos;
    /* access modifiers changed from: private */
    public LongPressListener mLongPressListener;
    /* access modifiers changed from: private */
    public boolean mLongPressSent;
    private long mLongPressTimeout;
    private float mMaxSwipeProgress = 1.0f;
    private float mMinSwipeProgress = 0.0f;
    private float mPagingTouchSlop;
    private float mPerpendicularInitialTouchPos;
    /* access modifiers changed from: private */
    public boolean mSnappingChild;
    private int mSwipeDirection;
    /* access modifiers changed from: private */
    public final int[] mTmpPos = new int[2];
    private boolean mTouchAboveFalsingThreshold;
    private float mTranslation = 0.0f;
    private VelocityTracker mVelocityTracker;
    private Runnable mWatchLongPress;

    public interface Callback {
        boolean canChildBeDismissed(View view);

        boolean checkToBeginDrag(View view);

        View getChildAtPosition(MotionEvent motionEvent);

        float getFalsingThresholdFactor();

        boolean isAntiFalsingNeeded();

        void onBeginDrag(View view);

        void onChildDismissed(View view);

        void onChildSnappedBack(View view, float f);

        void onDragCancelled(View view);

        void onDragEnd(View view);

        boolean updateSwipeProgress(View view, boolean z, float f);
    }

    public interface LongPressListener {
        boolean onLongPress(View view, int i, int i2);
    }

    public SwipeHelperForRecents(int swipeDirection, Callback callback, Context context) {
        this.mCallback = callback;
        this.mHandler = new Handler();
        this.mSwipeDirection = swipeDirection;
        this.mVelocityTracker = VelocityTracker.obtain();
        this.mDensityScale = context.getResources().getDisplayMetrics().density;
        this.mPagingTouchSlop = (float) ViewConfiguration.get(context).getScaledPagingTouchSlop();
        this.mLongPressTimeout = (long) (((float) ViewConfiguration.getLongPressTimeout()) * 1.5f);
        this.mFalsingThreshold = context.getResources().getDimensionPixelSize(R.dimen.swipe_helper_falsing_threshold);
    }

    public void setDisableHardwareLayers(boolean disableHwLayers) {
        this.mDisableHwLayers = disableHwLayers;
    }

    private float getPos(MotionEvent ev) {
        return this.mSwipeDirection == 0 ? ev.getX() : ev.getY();
    }

    private float getPerpendicularPos(MotionEvent ev) {
        return this.mSwipeDirection == 0 ? ev.getY() : ev.getX();
    }

    /* access modifiers changed from: protected */
    public float getTranslation(View v) {
        return this.mSwipeDirection == 0 ? v.getTranslationX() : v.getTranslationY();
    }

    private float getVelocity(VelocityTracker vt) {
        if (this.mSwipeDirection == 0) {
            return vt.getXVelocity();
        }
        return vt.getYVelocity();
    }

    /* access modifiers changed from: protected */
    public ObjectAnimator createTranslationAnimation(View v, float newPos) {
        return ObjectAnimator.ofFloat(v, this.mSwipeDirection == 0 ? View.TRANSLATION_X : View.TRANSLATION_Y, new float[]{newPos});
    }

    /* access modifiers changed from: protected */
    public Animator getViewTranslationAnimator(View v, float target, ValueAnimator.AnimatorUpdateListener listener) {
        ObjectAnimator anim = createTranslationAnimation(v, target);
        if (listener != null) {
            anim.addUpdateListener(listener);
        }
        return anim;
    }

    /* access modifiers changed from: protected */
    public void setTranslation(View v, float translate) {
        if (v != null) {
            if (this.mSwipeDirection == 0) {
                v.setTranslationX(translate);
            } else {
                v.setTranslationY(translate);
            }
        }
    }

    /* access modifiers changed from: protected */
    public float getSize(View v) {
        if (this.mSwipeDirection == 0) {
            return (float) v.getMeasuredWidth();
        }
        return (float) v.getMeasuredHeight();
    }

    private float getViewSize(View v) {
        if (this.mSwipeDirection == 0) {
            return (float) v.getMeasuredWidth();
        }
        return (float) v.getMeasuredHeight();
    }

    private float getSwipeProgressForOffset(View view, float translation) {
        return Math.min(Math.max(this.mMinSwipeProgress, Math.abs(translation / getSize(view))), this.mMaxSwipeProgress);
    }

    private float getSwipeAlpha(float progress) {
        return 1.0f - Math.max(0.0f, Math.min(1.0f, progress / 0.5f));
    }

    /* access modifiers changed from: private */
    public void updateSwipeProgressFromOffset(View animView, boolean dismissable) {
        updateSwipeProgressFromOffset(animView, dismissable, getTranslation(animView));
    }

    private void updateSwipeProgressFromOffset(View animView, boolean dismissable, float translation) {
        float swipeProgress = getSwipeProgressForOffset(animView, translation);
        if (!this.mCallback.updateSwipeProgress(animView, dismissable, translation > 0.0f ? swipeProgress : -swipeProgress) && dismissable) {
            float alpha = swipeProgress;
            if (!this.mDisableHwLayers) {
                if (alpha == 0.0f || alpha == 1.0f) {
                    animView.setLayerType(0, null);
                } else {
                    animView.setLayerType(2, null);
                }
            }
            animView.setAlpha(getSwipeAlpha(swipeProgress));
        }
        invalidateGlobalRegion(animView);
    }

    public static void invalidateGlobalRegion(View view) {
        invalidateGlobalRegion(view, new RectF((float) view.getLeft(), (float) view.getTop(), (float) view.getRight(), (float) view.getBottom()));
    }

    /* JADX WARNING: type inference failed for: r0v3, types: [android.view.ViewParent] */
    /* JADX WARNING: Multi-variable type inference failed */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static void invalidateGlobalRegion(android.view.View r5, android.graphics.RectF r6) {
        /*
        L_0x0000:
            android.view.ViewParent r0 = r5.getParent()
            if (r0 == 0) goto L_0x0040
            android.view.ViewParent r0 = r5.getParent()
            boolean r0 = r0 instanceof android.view.View
            if (r0 == 0) goto L_0x0040
            android.view.ViewParent r0 = r5.getParent()
            r5 = r0
            android.view.View r5 = (android.view.View) r5
            android.graphics.Matrix r0 = r5.getMatrix()
            r0.mapRect(r6)
            float r0 = r6.left
            double r0 = (double) r0
            double r0 = java.lang.Math.floor(r0)
            int r0 = (int) r0
            float r1 = r6.top
            double r1 = (double) r1
            double r1 = java.lang.Math.floor(r1)
            int r1 = (int) r1
            float r2 = r6.right
            double r2 = (double) r2
            double r2 = java.lang.Math.ceil(r2)
            int r2 = (int) r2
            float r3 = r6.bottom
            double r3 = (double) r3
            double r3 = java.lang.Math.ceil(r3)
            int r3 = (int) r3
            r5.invalidate(r0, r1, r2, r3)
            goto L_0x0000
        L_0x0040:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.recents.SwipeHelperForRecents.invalidateGlobalRegion(android.view.View, android.graphics.RectF):void");
    }

    public void removeLongPressCallback() {
        if (this.mWatchLongPress != null) {
            this.mHandler.removeCallbacks(this.mWatchLongPress);
            this.mWatchLongPress = null;
        }
    }

    public boolean onInterceptTouchEvent(final MotionEvent ev) {
        boolean z = true;
        switch (ev.getAction()) {
            case 0:
                this.mTouchAboveFalsingThreshold = false;
                this.mDragging = false;
                if (this.mSnappingChild && this.mCurrentAnim != null) {
                    this.mCurrentAnim.end();
                }
                this.mLongPressSent = false;
                this.mVelocityTracker.clear();
                this.mCurrView = this.mCallback.getChildAtPosition(ev);
                if (this.mCurrView != null) {
                    onDownUpdate(this.mCurrView);
                    this.mCanCurrViewBeDimissed = this.mCallback.canChildBeDismissed(this.mCurrView);
                    this.mVelocityTracker.addMovement(ev);
                    this.mInitialTouchPos = getPos(ev);
                    this.mPerpendicularInitialTouchPos = getPerpendicularPos(ev);
                    this.mTranslation = getTranslation(this.mCurrView);
                    if (this.mLongPressListener != null) {
                        if (this.mWatchLongPress == null) {
                            this.mWatchLongPress = new Runnable() {
                                public void run() {
                                    if (SwipeHelperForRecents.this.mCurrView != null && !SwipeHelperForRecents.this.mLongPressSent) {
                                        boolean unused = SwipeHelperForRecents.this.mLongPressSent = true;
                                        SwipeHelperForRecents.this.mCurrView.sendAccessibilityEvent(2);
                                        SwipeHelperForRecents.this.mCurrView.getLocationOnScreen(SwipeHelperForRecents.this.mTmpPos);
                                        SwipeHelperForRecents.this.mLongPressListener.onLongPress(SwipeHelperForRecents.this.mCurrView, ((int) ev.getRawX()) - SwipeHelperForRecents.this.mTmpPos[0], ((int) ev.getRawY()) - SwipeHelperForRecents.this.mTmpPos[1]);
                                    }
                                }
                            };
                        }
                        this.mHandler.postDelayed(this.mWatchLongPress, this.mLongPressTimeout);
                        break;
                    }
                }
                break;
            case 1:
            case 3:
                boolean captured = this.mDragging || this.mLongPressSent;
                this.mDragging = false;
                this.mCurrView = null;
                this.mLongPressSent = false;
                removeLongPressCallback();
                if (captured) {
                    return true;
                }
                break;
            case 2:
                if (this.mCurrView != null && !this.mLongPressSent) {
                    this.mVelocityTracker.addMovement(ev);
                    float pos = getPos(ev);
                    float perpendicularPos = getPerpendicularPos(ev);
                    float delta = pos - this.mInitialTouchPos;
                    float deltaPerpendicular = perpendicularPos - this.mPerpendicularInitialTouchPos;
                    if (Math.abs(delta) > this.mPagingTouchSlop && Math.abs(delta) > Math.abs(deltaPerpendicular) && this.mCallback.checkToBeginDrag(this.mCurrView)) {
                        this.mCallback.onBeginDrag(this.mCurrView);
                        this.mDragging = true;
                        this.mInitialTouchPos = getPos(ev);
                        this.mTranslation = getTranslation(this.mCurrView);
                        removeLongPressCallback();
                        break;
                    }
                }
        }
        if (!this.mDragging && !this.mLongPressSent) {
            z = false;
        }
        return z;
    }

    public void dismissChild(View view, float velocity) {
        dismissChild(view, velocity, null, 0, false);
    }

    public void dismissChild(View animView, float velocity, Runnable endAction, long delay, boolean isDismissAll) {
        float newPos;
        final View view = animView;
        long j = delay;
        final boolean canBeDismissed = this.mCallback.canChildBeDismissed(view);
        boolean animateLeft = false;
        boolean isLayoutRtl = view.getLayoutDirection() == 1;
        boolean animateUpForMenu = velocity == 0.0f && (getTranslation(view) == 0.0f || isDismissAll) && this.mSwipeDirection == 1;
        boolean animateLeftForRtl = velocity == 0.0f && (getTranslation(view) == 0.0f || isDismissAll) && isLayoutRtl;
        if (Math.abs(velocity) < 500.0f) {
            if (getTranslation(view) < 0.0f) {
                animateLeft = true;
            }
        } else if (velocity < 0.0f) {
            animateLeft = true;
        }
        if (animateLeft || animateLeftForRtl || animateUpForMenu) {
            newPos = -getSize(view);
        } else {
            newPos = getSize(view);
        }
        if (!this.mDisableHwLayers) {
            view.setLayerType(2, null);
        }
        Animator anim = getViewTranslationAnimator(view, newPos, new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                SwipeHelperForRecents.this.onTranslationUpdate(view, Float.valueOf(animation.getAnimatedFraction()).floatValue() * SwipeHelperForRecents.this.getSize(view), canBeDismissed);
            }
        });
        if (anim != null) {
            anim.setInterpolator(Interpolators.EASE_IN_OUT);
            anim.setDuration((long) this.DEFAULT_ESCAPE_ANIMATION_DURATION);
            if (j > 0) {
                anim.setStartDelay(j);
            }
            final Runnable runnable = endAction;
            anim.addListener(new AnimatorListenerAdapter() {
                private boolean mCancelled;

                public void onAnimationCancel(Animator animation) {
                    this.mCancelled = true;
                }

                public void onAnimationEnd(Animator animation) {
                    SwipeHelperForRecents.this.updateSwipeProgressFromOffset(view, canBeDismissed);
                    SwipeHelperForRecents.this.mDismissPendingMap.remove(view);
                    if (!this.mCancelled) {
                        SwipeHelperForRecents.this.mCallback.onChildDismissed(view);
                    }
                    if (runnable != null) {
                        runnable.run();
                    }
                    if (!SwipeHelperForRecents.this.mDisableHwLayers) {
                        view.setLayerType(0, null);
                    }
                }
            });
            prepareDismissAnimation(view, anim);
            this.mDismissPendingMap.put(view, anim);
            anim.start();
        }
    }

    /* access modifiers changed from: protected */
    public void prepareDismissAnimation(View view, Animator anim) {
    }

    public void snapChild(final View animView, final float targetLeft, float velocity) {
        final boolean canBeDismissed = this.mCallback.canChildBeDismissed(animView);
        Animator anim = getViewTranslationAnimator(animView, targetLeft, null);
        if (anim != null) {
            anim.setDuration((long) 150);
            anim.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animator) {
                    boolean unused = SwipeHelperForRecents.this.mSnappingChild = false;
                    SwipeHelperForRecents.this.updateSwipeProgressFromOffset(animView, canBeDismissed);
                    SwipeHelperForRecents.this.mCallback.onChildSnappedBack(animView, targetLeft);
                    Animator unused2 = SwipeHelperForRecents.this.mCurrentAnim = null;
                }
            });
            prepareSnapBackAnimation(animView, anim);
            this.mSnappingChild = true;
            anim.start();
            this.mCurrentAnim = anim;
        }
    }

    /* access modifiers changed from: protected */
    public void prepareSnapBackAnimation(View view, Animator anim) {
    }

    public void onDownUpdate(View currView) {
    }

    /* access modifiers changed from: protected */
    public void onMoveUpdate(View view, float totalTranslation, float delta) {
    }

    public void onTranslationUpdate(View animView, float value, boolean canBeDismissed) {
        updateSwipeProgressFromOffset(animView, canBeDismissed, value);
    }

    public boolean onTouchEvent(MotionEvent ev) {
        if (this.mLongPressSent) {
            return true;
        }
        if (this.mDragging) {
            this.mVelocityTracker.addMovement(ev);
            switch (ev.getAction()) {
                case 1:
                case 3:
                    if (this.mCurrView != null) {
                        this.mVelocityTracker.computeCurrentVelocity(1000, getMaxVelocity());
                        float velocity = getVelocity(this.mVelocityTracker);
                        this.mCallback.onDragEnd(this.mCurrView);
                        if (!handleUpEvent(ev, this.mCurrView, velocity, getTranslation(this.mCurrView))) {
                            if (isDismissGesture(ev)) {
                                dismissChild(this.mCurrView, velocity);
                            } else {
                                this.mCallback.onDragCancelled(this.mCurrView);
                                snapChild(this.mCurrView, 0.0f, velocity);
                            }
                            this.mCurrView = null;
                        }
                        this.mDragging = false;
                        break;
                    }
                    break;
                case 2:
                case 4:
                    if (this.mCurrView != null) {
                        float delta = getPos(ev) - this.mInitialTouchPos;
                        float absDelta = Math.abs(delta);
                        if (absDelta >= ((float) getFalsingThreshold())) {
                            this.mTouchAboveFalsingThreshold = true;
                        }
                        if (!this.mCallback.canChildBeDismissed(this.mCurrView)) {
                            float size = getSize(this.mCurrView);
                            float maxScrollDistance = 0.25f * size;
                            if (absDelta >= size) {
                                delta = delta > 0.0f ? maxScrollDistance : -maxScrollDistance;
                            } else {
                                delta = maxScrollDistance * ((float) Math.sin(((double) (delta / size)) * 1.5707963267948966d));
                            }
                        }
                        setTranslation(this.mCurrView, this.mTranslation + delta);
                        onMoveUpdate(this.mCurrView, this.mTranslation + delta, delta);
                        break;
                    }
                    break;
            }
            return true;
        } else if (this.mCallback.getChildAtPosition(ev) != null) {
            onInterceptTouchEvent(ev);
            return true;
        } else {
            removeLongPressCallback();
            return false;
        }
    }

    private int getFalsingThreshold() {
        return (int) (((float) this.mFalsingThreshold) * this.mCallback.getFalsingThresholdFactor());
    }

    private float getMaxVelocity() {
        return ((float) this.MAX_DISMISS_VELOCITY) * this.mDensityScale;
    }

    /* access modifiers changed from: protected */
    public float getEscapeVelocity() {
        return getUnscaledEscapeVelocity() * this.mDensityScale;
    }

    /* access modifiers changed from: protected */
    public float getUnscaledEscapeVelocity() {
        return this.SWIPE_ESCAPE_VELOCITY;
    }

    /* access modifiers changed from: protected */
    public boolean swipedFarEnough() {
        return ((double) Math.abs(getTranslation(this.mCurrView))) > 0.4d * ((double) getViewSize(this.mCurrView));
    }

    /* access modifiers changed from: protected */
    public boolean isDismissGesture(MotionEvent ev) {
        if (this.mCallback.isAntiFalsingNeeded() && !this.mTouchAboveFalsingThreshold) {
            return false;
        }
        if ((swipedFastEnough() || swipedFarEnough()) && ev.getActionMasked() == 1 && this.mCallback.canChildBeDismissed(this.mCurrView)) {
            return true;
        }
        return false;
    }

    /* access modifiers changed from: protected */
    public boolean swipedFastEnough() {
        float velocity = getVelocity(this.mVelocityTracker);
        float translation = getTranslation(this.mCurrView);
        boolean ret = false;
        if (Math.abs(velocity) > getEscapeVelocity()) {
            if ((velocity > 0.0f) == (translation > 0.0f)) {
                ret = true;
            }
        }
        return ret;
    }

    /* access modifiers changed from: protected */
    public boolean handleUpEvent(MotionEvent ev, View animView, float velocity, float translation) {
        return false;
    }
}
