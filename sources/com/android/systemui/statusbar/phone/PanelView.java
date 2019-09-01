package com.android.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import com.android.keyguard.LatencyTracker;
import com.android.systemui.DejankUtils;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.Util;
import com.android.systemui.classifier.FalsingManager;
import com.android.systemui.doze.DozeLog;
import com.android.systemui.statusbar.FlingAnimationUtils;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import com.android.systemui.util.QcomBoostFramework;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public abstract class PanelView extends FrameLayout {
    public static final boolean DEBUG = PanelBar.DEBUG;
    public static final String TAG = PanelView.class.getSimpleName();
    /* access modifiers changed from: private */
    public boolean mAnimateAfterExpanding;
    private boolean mAnimatingOnDown;
    PanelBar mBar;
    private Interpolator mBounceInterpolator;
    private boolean mClosing;
    private boolean mCollapsedAndHeadsUpOnDown;
    private boolean mExpandLatencyTracking;
    private float mExpandedFraction = 0.0f;
    protected float mExpandedHeight = 0.0f;
    protected boolean mExpanding;
    private FalsingManager mFalsingManager;
    private FlingAnimationUtils mFlingAnimationUtils;
    private FlingAnimationUtils mFlingAnimationUtilsClosing;
    private FlingAnimationUtils mFlingAnimationUtilsDismissing;
    private final Runnable mFlingCollapseRunnable = new Runnable() {
        public void run() {
            PanelView.this.fling(0.0f, false, PanelView.this.mNextCollapseSpeedUpFactor, false);
        }
    };
    private boolean mGestureWaitForTouchSlop;
    private boolean mHasLayoutedSinceDown;
    protected HeadsUpManager mHeadsUpManager;
    private ValueAnimator mHeightAnimator;
    protected boolean mHintAnimationRunning;
    private float mHintDistance;
    private boolean mIgnoreXTouchSlop;
    private float mInitialOffsetOnTouch;
    private float mInitialTouchX;
    private float mInitialTouchY;
    /* access modifiers changed from: private */
    public boolean mInstantExpanding;
    protected boolean mIsDefaultTheme = true;
    private boolean mIsKeyguardShowingOnDown;
    private boolean mJustPeeked;
    protected KeyguardBottomAreaView mKeyguardBottomArea;
    protected KeyguardVerticalMoveHelper mKeyguardVerticalMoveHelper;
    private LockscreenGestureLogger mLockscreenGestureLogger = new LockscreenGestureLogger();
    private float mMinExpandHeight;
    private boolean mMotionAborted;
    /* access modifiers changed from: private */
    public float mNextCollapseSpeedUpFactor = 1.0f;
    private boolean mNotificationsDragEnabled;
    private boolean mOverExpandedBeforeFling;
    private boolean mPanelClosedOnDown;
    private boolean mPanelUpdateWhenAnimatorEnds;
    /* access modifiers changed from: private */
    public ObjectAnimator mPeekAnimator;
    private float mPeekHeight;
    private boolean mPeekTouching;
    /* access modifiers changed from: private */
    public QcomBoostFramework mPerf = null;
    protected final Runnable mPostCollapseRunnable = new Runnable() {
        public void run() {
            PanelView.this.collapse(false, 1.0f);
        }
    };
    protected StatusBar mStatusBar;
    private boolean mStopTrackingAndCollapsed;
    private boolean mTouchAboveFalsingThreshold;
    private boolean mTouchDisabled;
    protected int mTouchSlop;
    private boolean mTouchSlopExceeded;
    private boolean mTouchStartedInEmptyArea;
    protected boolean mTracking;
    private int mTrackingPointer;
    private int mUnlockFalsingThreshold;
    private boolean mUpdateFlingOnLayout;
    private float mUpdateFlingVelocity;
    private boolean mUpwardsWhenTresholdReached;
    private VelocityTrackerInterface mVelocityTracker;
    private String mViewName;

    /* access modifiers changed from: protected */
    public abstract int getMaxPanelHeight();

    /* access modifiers changed from: protected */
    public abstract float getOpeningHeight();

    /* access modifiers changed from: protected */
    public abstract float getOverExpansionAmount();

    /* access modifiers changed from: protected */
    public abstract float getOverExpansionPixels();

    /* access modifiers changed from: protected */
    public abstract float getPeekHeight();

    /* access modifiers changed from: protected */
    public abstract float getQsExpansionFraction();

    /* access modifiers changed from: protected */
    public abstract boolean hasConflictingGestures();

    /* access modifiers changed from: protected */
    public abstract boolean isInCenterScreen();

    /* access modifiers changed from: protected */
    public abstract boolean isInContentBounds(float f, float f2);

    /* access modifiers changed from: protected */
    public abstract boolean isInUnderlapBounds(float f, float f2);

    /* access modifiers changed from: protected */
    public abstract boolean isPanelVisibleBecauseOfHeadsUp();

    /* access modifiers changed from: protected */
    public abstract boolean isTrackingBlocked();

    /* access modifiers changed from: protected */
    public abstract void onHeightUpdated(float f);

    /* access modifiers changed from: protected */
    public abstract boolean onMiddleClicked();

    public abstract void resetViews();

    /* access modifiers changed from: protected */
    public abstract void setOverExpansion(float f, boolean z);

    /* access modifiers changed from: protected */
    public abstract boolean shouldGestureIgnoreXTouchSlop(float f, float f2);

    /* access modifiers changed from: protected */
    public abstract boolean shouldUseDismissingAnimation();

    /* access modifiers changed from: protected */
    public final void logf(String fmt, Object... args) {
        Log.v(TAG, String.format(fmt, args));
    }

    /* access modifiers changed from: protected */
    public void onExpandingFinished() {
        this.mBar.onExpandingFinished();
    }

    /* access modifiers changed from: protected */
    public void onExpandingStarted() {
    }

    /* access modifiers changed from: private */
    public void notifyExpandingStarted() {
        if (!this.mExpanding) {
            this.mExpanding = true;
            onExpandingStarted();
        }
    }

    /* access modifiers changed from: protected */
    public final void notifyExpandingFinished() {
        endClosing();
        if (this.mExpanding) {
            this.mExpanding = false;
            onExpandingFinished();
        }
    }

    private void runPeekAnimation(long duration, float peekHeight, final boolean collapseWhenFinished) {
        this.mPeekHeight = peekHeight;
        if (DEBUG) {
            logf("peek to height=%.1f", Float.valueOf(this.mPeekHeight));
        }
        if (this.mHeightAnimator == null) {
            if (this.mPeekAnimator != null) {
                this.mPeekAnimator.cancel();
            }
            this.mPeekAnimator = ObjectAnimator.ofFloat(this, "expandedHeight", new float[]{this.mPeekHeight}).setDuration(duration);
            this.mPeekAnimator.setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN);
            this.mPeekAnimator.addListener(new AnimatorListenerAdapter() {
                private boolean mCancelled;

                public void onAnimationCancel(Animator animation) {
                    this.mCancelled = true;
                }

                public void onAnimationEnd(Animator animation) {
                    ObjectAnimator unused = PanelView.this.mPeekAnimator = null;
                    if (!this.mCancelled && collapseWhenFinished) {
                        PanelView.this.postOnAnimation(PanelView.this.mPostCollapseRunnable);
                    }
                }
            });
            notifyExpandingStarted();
            this.mPeekAnimator.start();
            this.mJustPeeked = true;
        }
    }

    public PanelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mFlingAnimationUtils = new FlingAnimationUtils(context, 0.6f, 0.6f);
        this.mFlingAnimationUtilsClosing = new FlingAnimationUtils(context, 0.3f, 0.6f);
        FlingAnimationUtils flingAnimationUtils = new FlingAnimationUtils(context, 0.5f, 0.2f, 0.6f, 0.84f);
        this.mFlingAnimationUtilsDismissing = flingAnimationUtils;
        this.mBounceInterpolator = new BounceInterpolator();
        this.mFalsingManager = FalsingManager.getInstance(context);
        this.mNotificationsDragEnabled = getResources().getBoolean(R.bool.config_enableNotificationShadeDrag);
        this.mPerf = new QcomBoostFramework();
    }

    /* access modifiers changed from: protected */
    public void loadDimens(Resources res) {
        this.mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        this.mHintDistance = res.getDimension(R.dimen.hint_move_distance);
        this.mUnlockFalsingThreshold = res.getDimensionPixelSize(R.dimen.unlock_falsing_threshold);
    }

    private void trackMovement(MotionEvent event) {
        float deltaX = event.getRawX() - event.getX();
        float deltaY = event.getRawY() - event.getY();
        event.offsetLocation(deltaX, deltaY);
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.addMovement(event);
        }
        event.offsetLocation(-deltaX, -deltaY);
    }

    public void setTouchDisabled(boolean disabled) {
        this.mTouchDisabled = disabled;
        if (this.mTouchDisabled) {
            cancelHeightAnimator();
            if (this.mTracking) {
                onTrackingStopped(true);
            }
            notifyExpandingFinished();
        }
    }

    public void startExpandLatencyTracking() {
        if (LatencyTracker.isEnabled(this.mContext)) {
            LatencyTracker.getInstance(this.mContext).onActionStart(0);
            this.mExpandLatencyTracking = true;
        }
    }

    public boolean onTouchEvent(MotionEvent event) {
        boolean z = true;
        if (this.mStatusBar.getBarState() == 0 && event.getActionMasked() != 2) {
            Log.d(TAG, String.format("onTouchEvent action=%d x=%.1f y=%.1f", new Object[]{Integer.valueOf(event.getActionMasked()), Float.valueOf(event.getX()), Float.valueOf(event.getY())}));
        }
        if (this.mInstantExpanding || this.mTouchDisabled || (this.mMotionAborted && event.getActionMasked() != 0)) {
            return false;
        }
        if (!this.mNotificationsDragEnabled) {
            if (this.mTracking) {
                onTrackingStopped(true);
            }
            return false;
        } else if (!isFullyCollapsed() || !event.isFromSource(8194)) {
            int pointerIndex = event.findPointerIndex(this.mTrackingPointer);
            if (pointerIndex < 0) {
                pointerIndex = 0;
                this.mTrackingPointer = event.getPointerId(0);
            }
            float x = event.getX(pointerIndex);
            float y = event.getY(pointerIndex);
            if (event.getActionMasked() == 0) {
                this.mGestureWaitForTouchSlop = isFullyCollapsed() || hasConflictingGestures();
                this.mIgnoreXTouchSlop = isFullyCollapsed() || shouldGestureIgnoreXTouchSlop(x, y);
            }
            if (isKeyguardShowing() && ((event.getActionMasked() == 0 && isFullyExpanded() && !this.mTracking && !this.mClosing) || (event.getActionMasked() != 0 && !isFullyCollapsed()))) {
                this.mKeyguardVerticalMoveHelper.onTouchEvent(event);
            }
            switch (event.getActionMasked()) {
                case 0:
                    startExpandMotion(x, y, false, this.mExpandedHeight);
                    this.mJustPeeked = false;
                    this.mMinExpandHeight = 0.0f;
                    this.mPanelClosedOnDown = isFullyCollapsed();
                    this.mHasLayoutedSinceDown = false;
                    this.mUpdateFlingOnLayout = false;
                    this.mMotionAborted = false;
                    this.mPeekTouching = this.mPanelClosedOnDown;
                    this.mTouchAboveFalsingThreshold = false;
                    this.mIsKeyguardShowingOnDown = isKeyguardShowing();
                    this.mStopTrackingAndCollapsed = false;
                    this.mCollapsedAndHeadsUpOnDown = isFullyCollapsed() && this.mHeadsUpManager.hasPinnedHeadsUp();
                    if (this.mVelocityTracker == null) {
                        initVelocityTracker();
                    }
                    trackMovement(event);
                    if (!this.mGestureWaitForTouchSlop || ((this.mHeightAnimator != null && !this.mHintAnimationRunning) || this.mPeekAnimator != null)) {
                        this.mTouchSlopExceeded = (this.mHeightAnimator != null && !this.mHintAnimationRunning) || this.mPeekAnimator != null;
                        cancelHeightAnimator();
                        cancelPeek();
                        onTrackingStarted();
                    }
                    if (isFullyCollapsed() && !this.mHeadsUpManager.hasPinnedHeadsUp() && !isExpandForbiddenInKeyguard()) {
                        startOpening();
                        break;
                    }
                    break;
                case 1:
                case 3:
                    trackMovement(event);
                    endMotionEvent(event, x, y, false);
                    break;
                case 2:
                    trackMovement(event);
                    float h = y - this.mInitialTouchY;
                    if (Math.abs(h) > ((float) this.mTouchSlop) && (Math.abs(h) > Math.abs(x - this.mInitialTouchX) || this.mIgnoreXTouchSlop)) {
                        this.mTouchSlopExceeded = true;
                        if (this.mGestureWaitForTouchSlop && !this.mTracking && !this.mCollapsedAndHeadsUpOnDown) {
                            if (!this.mJustPeeked && this.mInitialOffsetOnTouch != 0.0f) {
                                startExpandMotion(x, y, false, this.mExpandedHeight);
                                h = 0.0f;
                            }
                            cancelHeightAnimator();
                            onTrackingStarted();
                        }
                    }
                    float newHeight = Math.max(0.0f, this.mInitialOffsetOnTouch + h);
                    if (newHeight > this.mPeekHeight) {
                        if (this.mPeekAnimator != null) {
                            this.mPeekAnimator.cancel();
                        }
                        this.mJustPeeked = false;
                    } else if (this.mPeekAnimator == null && this.mJustPeeked) {
                        this.mInitialOffsetOnTouch = this.mExpandedHeight;
                        this.mInitialTouchY = y;
                        this.mMinExpandHeight = this.mExpandedHeight;
                        this.mJustPeeked = false;
                    }
                    float newHeight2 = Math.max(newHeight, this.mMinExpandHeight);
                    if ((-h) >= ((float) getFalsingThreshold())) {
                        this.mTouchAboveFalsingThreshold = true;
                        this.mUpwardsWhenTresholdReached = isDirectionUpwards(x, y);
                    }
                    if (!this.mJustPeeked && ((!this.mGestureWaitForTouchSlop || this.mTracking) && !isTrackingBlocked() && !this.mIsKeyguardShowingOnDown && !isExpandForbiddenInKeyguard())) {
                        setExpandedHeightInternal(newHeight2);
                        break;
                    }
                case 5:
                    if (this.mStatusBar.getBarState() == 1) {
                        this.mMotionAborted = true;
                        endMotionEvent(event, x, y, true);
                        return false;
                    }
                    break;
                case 6:
                    int upPointer = event.getPointerId(event.getActionIndex());
                    if (this.mTrackingPointer == upPointer) {
                        int newIndex = event.getPointerId(0) != upPointer ? 0 : 1;
                        float newY = event.getY(newIndex);
                        float newX = event.getX(newIndex);
                        this.mTrackingPointer = event.getPointerId(newIndex);
                        startExpandMotion(newX, newY, true, this.mExpandedHeight);
                        break;
                    }
                    break;
            }
            if (this.mGestureWaitForTouchSlop && !this.mTracking) {
                z = false;
            }
            return z;
        } else {
            if (event.getAction() == 1) {
                expand(true);
            }
            return true;
        }
    }

    private void startOpening() {
        runPeekAnimation(200, getOpeningHeight(), false);
        notifyBarPanelExpansionChanged();
    }

    private boolean isDirectionUpwards(float x, float y) {
        float xDiff = x - this.mInitialTouchX;
        float yDiff = y - this.mInitialTouchY;
        boolean z = false;
        if (yDiff >= 0.0f) {
            return false;
        }
        if (Math.abs(yDiff) >= Math.abs(xDiff)) {
            z = true;
        }
        return z;
    }

    /* access modifiers changed from: protected */
    public void startExpandMotion(float newX, float newY, boolean startTracking, float expandedHeight) {
        this.mInitialOffsetOnTouch = expandedHeight;
        this.mInitialTouchY = newY;
        this.mInitialTouchX = newX;
        if (startTracking) {
            this.mTouchSlopExceeded = true;
            setExpandedHeight(this.mInitialOffsetOnTouch);
            onTrackingStarted();
        }
    }

    private void endMotionEvent(MotionEvent event, float x, float y, boolean forceCancel) {
        float f = x;
        float f2 = y;
        this.mTrackingPointer = -1;
        if ((this.mTracking && this.mTouchSlopExceeded) || Math.abs(f - this.mInitialTouchX) > ((float) this.mTouchSlop) || Math.abs(f2 - this.mInitialTouchY) > ((float) this.mTouchSlop) || event.getActionMasked() == 3 || forceCancel) {
            float vel = 0.0f;
            float vectorVel = 0.0f;
            if (this.mVelocityTracker != null) {
                this.mVelocityTracker.computeCurrentVelocity(1000);
                vel = this.mVelocityTracker.getYVelocity();
                vectorVel = (float) Math.hypot((double) this.mVelocityTracker.getXVelocity(), (double) this.mVelocityTracker.getYVelocity());
            }
            boolean z = true;
            boolean expand = !this.mStopTrackingAndCollapsed && (flingExpands(vel, vectorVel, f, f2) || forceCancel);
            boolean ignoreCollapseGesture = expand || this.mStopTrackingAndCollapsed;
            DozeLog.traceFling(expand, this.mTouchAboveFalsingThreshold, this.mStatusBar.isFalsingThresholdNeeded(), this.mStatusBar.isWakeUpComingFromTouch());
            if (!ignoreCollapseGesture && this.mStatusBar.getBarState() == 1) {
                float displayDensity = this.mStatusBar.getDisplayDensity();
                this.mLockscreenGestureLogger.write(getContext(), 186, (int) Math.abs((f2 - this.mInitialTouchY) / displayDensity), (int) Math.abs(vel / displayDensity));
            }
            if (expand || this.mStatusBar.getBarState() != 1) {
                fling(vel, expand, isFalseTouch(f, f2));
            } else {
                if (this.mExpanding) {
                    notifyExpandingFinished();
                }
                if (!ignoreCollapseGesture) {
                    this.mStatusBar.showBouncer();
                }
            }
            onTrackingStopped(expand);
            if (!expand || !this.mPanelClosedOnDown || this.mHasLayoutedSinceDown) {
                z = false;
            }
            this.mUpdateFlingOnLayout = z;
            if (this.mUpdateFlingOnLayout) {
                this.mUpdateFlingVelocity = vel;
            }
        } else if (!this.mPanelClosedOnDown || this.mHeadsUpManager.hasPinnedHeadsUp() || this.mTracking) {
            onTrackingStopped(onEmptySpaceClick(this.mInitialTouchX));
        } else {
            post(new Runnable() {
                public void run() {
                    PanelView.this.postOnAnimation(PanelView.this.mPostCollapseRunnable);
                }
            });
        }
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.recycle();
            this.mVelocityTracker = null;
        }
        this.mPeekTouching = false;
    }

    /* access modifiers changed from: protected */
    public float getCurrentExpandVelocity() {
        if (this.mVelocityTracker == null) {
            return 0.0f;
        }
        this.mVelocityTracker.computeCurrentVelocity(1000);
        return this.mVelocityTracker.getYVelocity();
    }

    private int getFalsingThreshold() {
        return (int) (((float) this.mUnlockFalsingThreshold) * (this.mStatusBar.isWakeUpComingFromTouch() ? 1.5f : 1.0f));
    }

    /* access modifiers changed from: protected */
    public void onTrackingStopped(boolean expand) {
        this.mTracking = false;
        this.mBar.onTrackingStopped(expand);
        notifyBarPanelExpansionChanged();
    }

    /* access modifiers changed from: protected */
    public void onTrackingStarted() {
        endClosing();
        this.mTracking = true;
        this.mBar.onTrackingStarted();
        notifyExpandingStarted();
        notifyBarPanelExpansionChanged();
    }

    public void stopTrackingAndCollapsed() {
        this.mStopTrackingAndCollapsed = true;
    }

    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (this.mInstantExpanding || !this.mNotificationsDragEnabled || this.mTouchDisabled || !isInCenterScreen() || ((!this.mIsDefaultTheme && isKeyguardShowing() && getQsExpansionFraction() == 0.0f) || (this.mMotionAborted && event.getActionMasked() != 0))) {
            Log.d(TAG, "PanelView not intercept");
            return false;
        }
        int pointerIndex = event.findPointerIndex(this.mTrackingPointer);
        if (pointerIndex < 0) {
            pointerIndex = 0;
            this.mTrackingPointer = event.getPointerId(0);
        }
        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);
        boolean scrolledToBottom = isScrolledToBottom();
        boolean z = true;
        switch (event.getActionMasked()) {
            case 0:
                this.mStatusBar.userActivity();
                this.mAnimatingOnDown = this.mHeightAnimator != null;
                this.mMinExpandHeight = 0.0f;
                if ((!this.mAnimatingOnDown || !this.mClosing || this.mHintAnimationRunning) && this.mPeekAnimator == null) {
                    this.mInitialTouchY = y;
                    this.mInitialTouchX = x;
                    if (isInContentBounds(x, y) && !isInUnderlapBounds(x, y)) {
                        z = false;
                    }
                    this.mTouchStartedInEmptyArea = z;
                    this.mTouchSlopExceeded = false;
                    this.mJustPeeked = false;
                    this.mMotionAborted = false;
                    this.mPanelClosedOnDown = isFullyCollapsed();
                    this.mCollapsedAndHeadsUpOnDown = false;
                    this.mHasLayoutedSinceDown = false;
                    this.mUpdateFlingOnLayout = false;
                    this.mTouchAboveFalsingThreshold = false;
                    initVelocityTracker();
                    trackMovement(event);
                    break;
                } else {
                    cancelHeightAnimator();
                    cancelPeek();
                    this.mTouchSlopExceeded = true;
                    return true;
                }
                break;
            case 1:
            case 3:
                if (this.mVelocityTracker != null) {
                    this.mVelocityTracker.recycle();
                    this.mVelocityTracker = null;
                    break;
                }
                break;
            case 2:
                float h = y - this.mInitialTouchY;
                trackMovement(event);
                if (scrolledToBottom || this.mTouchStartedInEmptyArea || this.mAnimatingOnDown) {
                    float hAbs = Math.abs(h);
                    if ((h < ((float) (-this.mTouchSlop)) || (this.mAnimatingOnDown && hAbs > ((float) this.mTouchSlop))) && hAbs > Math.abs(x - this.mInitialTouchX)) {
                        cancelHeightAnimator();
                        startExpandMotion(x, y, true, this.mExpandedHeight);
                        return true;
                    }
                }
            case 5:
                if (this.mStatusBar.getBarState() == 1) {
                    this.mMotionAborted = true;
                    if (this.mVelocityTracker != null) {
                        this.mVelocityTracker.recycle();
                        this.mVelocityTracker = null;
                        break;
                    }
                }
                break;
            case 6:
                int upPointer = event.getPointerId(event.getActionIndex());
                if (this.mTrackingPointer == upPointer) {
                    if (event.getPointerId(0) != upPointer) {
                        z = false;
                    }
                    int newIndex = z;
                    this.mTrackingPointer = event.getPointerId((int) newIndex);
                    this.mInitialTouchX = event.getX(newIndex);
                    this.mInitialTouchY = event.getY(newIndex);
                    break;
                }
                break;
        }
        return false;
    }

    /* access modifiers changed from: protected */
    public boolean isExpandForbiddenInKeyguard() {
        return false;
    }

    /* access modifiers changed from: protected */
    public void cancelHeightAnimator() {
        if (this.mHeightAnimator != null) {
            if (this.mHeightAnimator.isRunning()) {
                this.mPanelUpdateWhenAnimatorEnds = false;
            }
            this.mHeightAnimator.cancel();
        }
        endClosing();
    }

    private void endClosing() {
        if (this.mClosing) {
            this.mClosing = false;
            onClosingFinished();
        }
    }

    private void initVelocityTracker() {
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.recycle();
        }
        this.mVelocityTracker = VelocityTrackerFactory.obtain(getContext());
    }

    /* access modifiers changed from: protected */
    public boolean isScrolledToBottom() {
        return true;
    }

    /* access modifiers changed from: protected */
    public boolean flingExpands(float vel, float vectorVel, float x, float y) {
        boolean z = true;
        if (isFalseTouch(x, y)) {
            return true;
        }
        if (Math.abs(vectorVel) < this.mFlingAnimationUtils.getMinVelocityPxPerSecond()) {
            if (getExpandedFraction() <= 0.5f) {
                z = false;
            }
            return z;
        }
        if (vel <= 0.0f) {
            z = false;
        }
        return z;
    }

    private boolean isFalseTouch(float x, float y) {
        if (!this.mStatusBar.isFalsingThresholdNeeded()) {
            return false;
        }
        if (this.mFalsingManager.isClassiferEnabled()) {
            return this.mFalsingManager.isFalseTouch();
        }
        if (!this.mTouchAboveFalsingThreshold) {
            return true;
        }
        if (this.mUpwardsWhenTresholdReached) {
            return false;
        }
        return !isDirectionUpwards(x, y);
    }

    /* access modifiers changed from: protected */
    public void fling(float vel, boolean expand) {
        fling(vel, expand, 1.0f, false);
    }

    /* access modifiers changed from: protected */
    public void fling(float vel, boolean expand, boolean expandBecauseOfFalsing) {
        fling(vel, expand, 1.0f, expandBecauseOfFalsing);
    }

    /* access modifiers changed from: protected */
    public void fling(float vel, boolean expand, float collapseSpeedUpFactor, boolean expandBecauseOfFalsing) {
        cancelPeek();
        float f = 0.0f;
        if (vel < 1000.0f && expand && 0.0f < this.mExpandedHeight && this.mExpandedHeight < this.mPeekHeight) {
            expand = false;
            Log.d(TAG, "warning false touch.");
        }
        if (expand) {
            f = (float) getMaxPanelHeight();
        }
        float target = f;
        if (!expand) {
            this.mClosing = true;
        }
        flingToHeight(vel, expand, target, collapseSpeedUpFactor, expandBecauseOfFalsing);
    }

    /* access modifiers changed from: protected */
    public void flingToHeight(float vel, boolean expand, float target, float collapseSpeedUpFactor, boolean expandBecauseOfFalsing) {
        float f = target;
        boolean z = false;
        if (DEBUG) {
            Log.d(TAG, String.format("flingToHeight vel=%.1f expand=%b target=%.1f mExpandedHeight=%.1f", new Object[]{Float.valueOf(vel), Boolean.valueOf(expand), Float.valueOf(target), Float.valueOf(this.mExpandedHeight)}));
        }
        if (f == this.mExpandedHeight || (getOverExpansionAmount() > 0.0f && expand)) {
            notifyExpandingFinished();
            return;
        }
        if (getOverExpansionAmount() > 0.0f) {
            z = true;
        }
        this.mOverExpandedBeforeFling = z;
        ValueAnimator animator = createHeightAnimator(f);
        long j = 0;
        if (!expand) {
            if (!shouldUseDismissingAnimation()) {
                this.mFlingAnimationUtilsClosing.apply((Animator) animator, this.mExpandedHeight, f, vel, (float) getHeight());
            } else if (vel == 0.0f) {
                animator.setInterpolator(Interpolators.PANEL_CLOSE_ACCELERATED);
                animator.setDuration(Util.isMiuiOptimizationDisabled() ? 0 : (long) (200.0f + ((this.mExpandedHeight / ((float) getHeight())) * 100.0f)));
            } else {
                this.mFlingAnimationUtilsDismissing.apply((Animator) animator, this.mExpandedHeight, f, vel, (float) getHeight());
            }
            if (this.mExpandedHeight < this.mPeekHeight) {
                animator.setDuration(0);
            } else if (vel == 0.0f) {
                animator.setDuration((long) (((float) animator.getDuration()) / collapseSpeedUpFactor));
            }
            float f2 = vel;
        } else if (!isExpandForbiddenInKeyguard()) {
            float vel2 = (!expandBecauseOfFalsing || vel >= 0.0f) ? vel : 0.0f;
            this.mFlingAnimationUtils.apply((Animator) animator, this.mExpandedHeight, f, vel2, (float) getHeight());
            if (vel2 == 0.0f) {
                if (!Util.isMiuiOptimizationDisabled()) {
                    j = 350;
                }
                animator.setDuration(j);
            }
        } else {
            return;
        }
        if (this.mPerf != null) {
            this.mPerf.perfHint(4224, this.mContext.getPackageName(), -1, 1);
        }
        animator.addListener(new AnimatorListenerAdapter() {
            private boolean mCancelled;

            public void onAnimationCancel(Animator animation) {
                if (PanelView.this.mPerf != null) {
                    PanelView.this.mPerf.perfLockRelease();
                }
                this.mCancelled = true;
            }

            public void onAnimationEnd(Animator animation) {
                if (PanelView.this.mPerf != null) {
                    PanelView.this.mPerf.perfLockRelease();
                }
                PanelView.this.setAnimator(null);
                if (!this.mCancelled) {
                    PanelView.this.notifyExpandingFinished();
                }
                PanelView.this.mKeyguardVerticalMoveHelper.reset();
                PanelView.this.notifyBarPanelExpansionChanged();
            }
        });
        setAnimator(animator);
        animator.start();
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mViewName = getResources().getResourceName(getId());
    }

    public void setExpandedHeight(float height) {
        if (DEBUG) {
            logf("setExpandedHeight(%.1f)", Float.valueOf(height));
        }
        setExpandedHeightInternal(getOverExpansionPixels() + height);
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        this.mStatusBar.onPanelLaidOut();
        requestPanelHeightUpdate();
        this.mHasLayoutedSinceDown = true;
        if (this.mUpdateFlingOnLayout) {
            abortAnimations();
            fling(this.mUpdateFlingVelocity, true);
            this.mUpdateFlingOnLayout = false;
        }
    }

    /* access modifiers changed from: protected */
    public void requestPanelHeightUpdate() {
        float currentMaxPanelHeight = (float) getMaxPanelHeight();
        if (isFullyCollapsed() || currentMaxPanelHeight == this.mExpandedHeight || this.mPeekAnimator != null || this.mPeekTouching) {
            return;
        }
        if (this.mTracking && !isTrackingBlocked()) {
            return;
        }
        if (this.mHeightAnimator != null) {
            this.mPanelUpdateWhenAnimatorEnds = true;
        } else {
            setExpandedHeight(currentMaxPanelHeight);
        }
    }

    public void setExpandedHeightInternal(float h) {
        float f = 0.0f;
        if (this.mExpandLatencyTracking && h != 0.0f) {
            DejankUtils.postAfterTraversal(new Runnable() {
                public void run() {
                    LatencyTracker.getInstance(PanelView.this.mContext).onActionEnd(0);
                }
            });
            this.mExpandLatencyTracking = false;
        }
        float fhWithoutOverExpansion = ((float) getMaxPanelHeight()) - getOverExpansionAmount();
        if (this.mHeightAnimator == null) {
            float overExpansionPixels = Math.max(0.0f, h - fhWithoutOverExpansion);
            if (getOverExpansionPixels() != overExpansionPixels && this.mTracking) {
                setOverExpansion(overExpansionPixels, true);
            }
            this.mExpandedHeight = Math.min(h, fhWithoutOverExpansion) + getOverExpansionAmount();
        } else {
            this.mExpandedHeight = h;
            if (this.mOverExpandedBeforeFling) {
                setOverExpansion(Math.max(0.0f, h - fhWithoutOverExpansion), false);
            }
        }
        if (this.mExpandedHeight < 1.0f && this.mExpandedHeight != 0.0f && this.mClosing) {
            this.mExpandedHeight = 0.0f;
            if (this.mHeightAnimator != null) {
                this.mHeightAnimator.end();
            }
        }
        if (fhWithoutOverExpansion != 0.0f) {
            f = this.mExpandedHeight / fhWithoutOverExpansion;
        }
        this.mExpandedFraction = Math.min(1.0f, f);
        onHeightUpdated(this.mExpandedHeight);
        notifyBarPanelExpansionChanged();
    }

    public void setExpandedFraction(float frac) {
        setExpandedHeight(((float) getMaxPanelHeight()) * frac);
    }

    public float getExpandedHeight() {
        return this.mExpandedHeight;
    }

    public float getExpandedFraction() {
        return this.mExpandedFraction;
    }

    public boolean isFullyExpanded() {
        return this.mExpandedHeight >= ((float) getMaxPanelHeight());
    }

    public boolean isFullyCollapsed() {
        return this.mExpandedFraction <= 0.0f;
    }

    public boolean isCollapsing() {
        return this.mClosing;
    }

    public boolean isTracking() {
        return this.mTracking;
    }

    public boolean isKeyguardShowing() {
        return false;
    }

    public void setBar(PanelBar panelBar) {
        this.mBar = panelBar;
    }

    public void collapse(boolean delayed, float speedUpFactor) {
        if (DEBUG) {
            logf("collapse: " + this, new Object[0]);
        }
        if (canPanelBeCollapsed()) {
            cancelHeightAnimator();
            notifyExpandingStarted();
            this.mClosing = true;
            if (delayed) {
                this.mNextCollapseSpeedUpFactor = speedUpFactor;
                postDelayed(this.mFlingCollapseRunnable, 120);
                return;
            }
            fling(0.0f, false, speedUpFactor, false);
        }
    }

    public boolean canPanelBeCollapsed() {
        return !isFullyCollapsed() && !this.mTracking && !this.mClosing;
    }

    public void cancelPeek() {
        boolean cancelled = false;
        if (this.mPeekAnimator != null) {
            cancelled = true;
            this.mPeekAnimator.cancel();
        }
        if (cancelled) {
            notifyBarPanelExpansionChanged();
        }
    }

    public void expand(boolean animate) {
        if (isFullyCollapsed() || isCollapsing()) {
            this.mInstantExpanding = true;
            this.mAnimateAfterExpanding = animate;
            this.mUpdateFlingOnLayout = false;
            abortAnimations();
            cancelPeek();
            if (this.mTracking) {
                onTrackingStopped(true);
            }
            if (this.mExpanding) {
                notifyExpandingFinished();
            }
            notifyBarPanelExpansionChanged();
            getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                public void onGlobalLayout() {
                    if (!PanelView.this.mInstantExpanding) {
                        PanelView.this.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        return;
                    }
                    if (PanelView.this.mStatusBar.getStatusBarWindow().getHeight() != PanelView.this.mStatusBar.getStatusBarHeight()) {
                        PanelView.this.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        if (PanelView.this.mAnimateAfterExpanding) {
                            PanelView.this.notifyExpandingStarted();
                            PanelView.this.fling(0.0f, true);
                        } else {
                            PanelView.this.setExpandedFraction(1.0f);
                        }
                        boolean unused = PanelView.this.mInstantExpanding = false;
                    }
                }
            });
            requestLayout();
        }
    }

    public void instantCollapse() {
        abortAnimations();
        setExpandedFraction(0.0f);
        if (this.mExpanding) {
            notifyExpandingFinished();
        }
        if (this.mInstantExpanding) {
            this.mInstantExpanding = false;
            notifyBarPanelExpansionChanged();
        }
    }

    private void abortAnimations() {
        cancelPeek();
        cancelHeightAnimator();
        removeCallbacks(this.mPostCollapseRunnable);
        removeCallbacks(this.mFlingCollapseRunnable);
    }

    /* access modifiers changed from: protected */
    public void onClosingFinished() {
        this.mBar.onClosingFinished();
    }

    /* access modifiers changed from: private */
    public void setAnimator(ValueAnimator animator) {
        this.mHeightAnimator = animator;
        if (animator == null && this.mPanelUpdateWhenAnimatorEnds) {
            this.mPanelUpdateWhenAnimatorEnds = false;
            requestPanelHeightUpdate();
        }
    }

    private ValueAnimator createHeightAnimator(float targetHeight) {
        ValueAnimator animator = ValueAnimator.ofFloat(new float[]{this.mExpandedHeight, targetHeight});
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                PanelView.this.setExpandedHeightInternal(((Float) animation.getAnimatedValue()).floatValue());
            }
        });
        return animator;
    }

    /* access modifiers changed from: protected */
    public void notifyBarPanelExpansionChanged() {
        if (this.mBar != null) {
            this.mBar.panelExpansionChanged(this.mExpandedFraction, this.mExpandedFraction > 0.0f || this.mPeekAnimator != null || this.mInstantExpanding || isPanelVisibleBecauseOfHeadsUp() || this.mTracking || this.mHeightAnimator != null);
        }
    }

    /* access modifiers changed from: protected */
    public boolean onEmptySpaceClick(float x) {
        if (this.mHintAnimationRunning) {
            return true;
        }
        return onMiddleClicked();
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        Object[] objArr = new Object[11];
        objArr[0] = Float.valueOf(getExpandedHeight());
        objArr[1] = Integer.valueOf(getMaxPanelHeight());
        objArr[2] = this.mClosing ? "T" : "f";
        objArr[3] = this.mTracking ? "T" : "f";
        objArr[4] = this.mJustPeeked ? "T" : "f";
        objArr[5] = this.mPeekAnimator;
        objArr[6] = (this.mPeekAnimator == null || !this.mPeekAnimator.isStarted()) ? "" : " (started)";
        objArr[7] = this.mHeightAnimator;
        objArr[8] = (this.mHeightAnimator == null || !this.mHeightAnimator.isStarted()) ? "" : " (started)";
        objArr[9] = this.mTouchDisabled ? "T" : "f";
        objArr[10] = this.mIsDefaultTheme ? "T" : "f";
        pw.println(String.format("[PanelView: expandedHeight=%f maxPanelHeight=%d closing=%s tracking=%s justPeeked=%s peekAnim=%s%s timeAnim=%s%s touchDisabled=%s mIsDefaultTheme=%s]", objArr));
    }

    public void setHeadsUpManager(HeadsUpManager headsUpManager) {
        this.mHeadsUpManager = headsUpManager;
    }
}
