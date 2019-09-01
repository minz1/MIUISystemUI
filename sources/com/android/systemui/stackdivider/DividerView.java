package com.android.systemui.stackdivider;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.graphics.Region;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.Display;
import android.view.DisplayInfo;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewPropertyAnimator;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.events.RecentsEventBus;
import com.android.systemui.recents.events.activity.DockedTopTaskEvent;
import com.android.systemui.recents.events.activity.MultiWindowStateChangedEvent;
import com.android.systemui.recents.events.activity.RecentsActivityStartingEvent;
import com.android.systemui.recents.events.activity.UndockingTaskEvent;
import com.android.systemui.recents.events.component.ExitMultiModeEvent;
import com.android.systemui.recents.events.ui.RecentsDrawnEvent;
import com.android.systemui.recents.events.ui.RecentsGrowingEvent;
import com.android.systemui.recents.misc.RecentsPushEventHelper;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.stackdivider.DividerSnapAlgorithm;
import com.android.systemui.stackdivider.events.StartedDragingEvent;
import com.android.systemui.stackdivider.events.StoppedDragingEvent;
import com.android.systemui.statusbar.FlingAnimationUtils;

public class DividerView extends FrameLayout implements View.OnTouchListener, ViewTreeObserver.OnComputeInternalInsetsListener {
    private static final PathInterpolator DIM_INTERPOLATOR = new PathInterpolator(0.23f, 0.87f, 0.52f, -0.11f);
    private static final Interpolator IME_ADJUST_INTERPOLATOR = new PathInterpolator(0.2f, 0.0f, 0.1f, 1.0f);
    private static final PathInterpolator SLOWDOWN_INTERPOLATOR = new PathInterpolator(0.5f, 1.0f, 0.5f, 1.0f);
    private boolean mAdjustedForIme;
    private View mBackground;
    private boolean mBackgroundLifted;
    /* access modifiers changed from: private */
    public ValueAnimator mCurrentAnimator;
    private int mCurrentTouchAction;
    private int mDisplayHeight;
    private final Rect mDisplayRect = new Rect();
    private int mDisplayWidth;
    private int mDividerInsets;
    private int mDividerSize;
    private int mDividerWindowWidth;
    /* access modifiers changed from: private */
    public int mDockSide;
    private final Rect mDockedInsetRect = new Rect();
    private final Rect mDockedRect = new Rect();
    private boolean mDockedStackMinimized;
    private final Rect mDockedTaskRect = new Rect();
    /* access modifiers changed from: private */
    public boolean mEntranceAnimationRunning;
    /* access modifiers changed from: private */
    public boolean mExitAnimationRunning;
    private int mExitStartPosition;
    private FlingAnimationUtils mFlingAnimationUtils;
    private GestureDetector mGestureDetector;
    private boolean mGrowRecents;
    private DividerHandleView mHandle;
    private final View.AccessibilityDelegate mHandleDelegate = new View.AccessibilityDelegate() {
        public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(host, info);
            if (DividerView.this.isHorizontalDivision()) {
                info.addAction(new AccessibilityNodeInfo.AccessibilityAction(R.id.action_move_tl_full, DividerView.this.mContext.getString(R.string.accessibility_action_divider_top_full)));
                if (DividerView.this.mSnapAlgorithm.isFirstSplitTargetAvailable()) {
                    info.addAction(new AccessibilityNodeInfo.AccessibilityAction(R.id.action_move_tl_70, DividerView.this.mContext.getString(R.string.accessibility_action_divider_top_70)));
                }
                info.addAction(new AccessibilityNodeInfo.AccessibilityAction(R.id.action_move_tl_50, DividerView.this.mContext.getString(R.string.accessibility_action_divider_top_50)));
                if (DividerView.this.mSnapAlgorithm.isLastSplitTargetAvailable()) {
                    info.addAction(new AccessibilityNodeInfo.AccessibilityAction(R.id.action_move_tl_30, DividerView.this.mContext.getString(R.string.accessibility_action_divider_top_30)));
                }
                info.addAction(new AccessibilityNodeInfo.AccessibilityAction(R.id.action_move_rb_full, DividerView.this.mContext.getString(R.string.accessibility_action_divider_bottom_full)));
                return;
            }
            info.addAction(new AccessibilityNodeInfo.AccessibilityAction(R.id.action_move_tl_full, DividerView.this.mContext.getString(R.string.accessibility_action_divider_left_full)));
            if (DividerView.this.mSnapAlgorithm.isFirstSplitTargetAvailable()) {
                info.addAction(new AccessibilityNodeInfo.AccessibilityAction(R.id.action_move_tl_70, DividerView.this.mContext.getString(R.string.accessibility_action_divider_left_70)));
            }
            info.addAction(new AccessibilityNodeInfo.AccessibilityAction(R.id.action_move_tl_50, DividerView.this.mContext.getString(R.string.accessibility_action_divider_left_50)));
            if (DividerView.this.mSnapAlgorithm.isLastSplitTargetAvailable()) {
                info.addAction(new AccessibilityNodeInfo.AccessibilityAction(R.id.action_move_tl_30, DividerView.this.mContext.getString(R.string.accessibility_action_divider_left_30)));
            }
            info.addAction(new AccessibilityNodeInfo.AccessibilityAction(R.id.action_move_rb_full, DividerView.this.mContext.getString(R.string.accessibility_action_divider_right_full)));
        }

        public boolean performAccessibilityAction(View host, int action, Bundle args) {
            int currentPosition = DividerView.this.getCurrentPosition();
            DividerSnapAlgorithm.SnapTarget nextTarget = null;
            switch (action) {
                case R.id.action_move_rb_full:
                    nextTarget = DividerView.this.mSnapAlgorithm.getDismissStartTarget();
                    break;
                case R.id.action_move_tl_30:
                    nextTarget = DividerView.this.mSnapAlgorithm.getFirstSplitTarget();
                    break;
                case R.id.action_move_tl_50:
                    nextTarget = DividerView.this.mSnapAlgorithm.getMiddleTarget();
                    break;
                case R.id.action_move_tl_70:
                    nextTarget = DividerView.this.mSnapAlgorithm.getLastSplitTarget();
                    break;
                case R.id.action_move_tl_full:
                    nextTarget = DividerView.this.mSnapAlgorithm.getDismissEndTarget();
                    break;
            }
            DividerSnapAlgorithm.SnapTarget nextTarget2 = nextTarget;
            if (nextTarget2 == null) {
                return super.performAccessibilityAction(host, action, args);
            }
            DividerView.this.startDragging(true, false);
            DividerView.this.stopDragging(currentPosition, nextTarget2, 250, Interpolators.FAST_OUT_SLOW_IN);
            return true;
        }
    };
    /* access modifiers changed from: private */
    public final Handler mHandler = new Handler();
    private final Rect mLastResizeRect = new Rect();
    /* access modifiers changed from: private */
    public int mLongPressEntraceAnimDuration;
    private MinimizedDockShadow mMinimizedShadow;
    private boolean mMoving;
    private final Rect mOtherInsetRect = new Rect();
    private final Rect mOtherRect = new Rect();
    private final Rect mOtherTaskRect = new Rect();
    private final Runnable mResetBackgroundRunnable = new Runnable() {
        public void run() {
            DividerView.this.resetBackground();
        }
    };
    /* access modifiers changed from: private */
    public DividerSnapAlgorithm mSnapAlgorithm;
    private final Rect mStableInsets = new Rect();
    private int mStartPosition;
    private int mStartX;
    private int mStartY;
    private DividerState mState;
    private final int[] mTempInt2 = new int[2];
    private int mTouchElevation;
    private int mTouchSlop;
    private boolean mUnDockByUndockingTaskEvent = false;
    private VelocityTracker mVelocityTracker;
    private DividerWindowManager mWindowManager;
    /* access modifiers changed from: private */
    public final WindowManagerProxy mWindowManagerProxy = WindowManagerProxy.getInstance();

    public DividerView(Context context) {
        super(context);
    }

    public DividerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DividerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public DividerView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mHandle = (DividerHandleView) findViewById(R.id.docked_divider_handle);
        this.mBackground = findViewById(R.id.docked_divider_background);
        this.mMinimizedShadow = (MinimizedDockShadow) findViewById(R.id.minimized_dock_shadow);
        this.mHandle.setOnTouchListener(this);
        this.mDividerWindowWidth = getResources().getDimensionPixelSize(R.dimen.docked_stack_divider_thickness);
        this.mDividerInsets = getResources().getDimensionPixelSize(R.dimen.docked_stack_divider_insets);
        this.mDividerSize = this.mDividerWindowWidth - (this.mDividerInsets * 2);
        this.mTouchElevation = getResources().getDimensionPixelSize(R.dimen.docked_stack_divider_lift_elevation);
        this.mLongPressEntraceAnimDuration = getResources().getInteger(R.integer.long_press_dock_anim_duration);
        this.mGrowRecents = getResources().getBoolean(R.bool.recents_grow_in_multiwindow);
        this.mTouchSlop = ViewConfiguration.get(this.mContext).getScaledTouchSlop();
        this.mFlingAnimationUtils = new FlingAnimationUtils(getContext(), 0.3f);
        updateDisplayInfo();
        if (getResources().getConfiguration().orientation == 2) {
        }
        getViewTreeObserver().addOnComputeInternalInsetsListener(this);
        this.mHandle.setAccessibilityDelegate(this.mHandleDelegate);
        this.mGestureDetector = new GestureDetector(this.mContext, new GestureDetector.SimpleOnGestureListener() {
            public boolean onDoubleTap(MotionEvent e) {
                DividerView.this.updateDockSide();
                SystemServicesProxy ssp = Recents.getSystemServices();
                if (DividerView.this.mDockSide == -1 || ssp.isRecentsActivityVisible()) {
                    return false;
                }
                DividerView.this.mWindowManagerProxy.swapTasks();
                RecentsPushEventHelper.sendMultiWindowEvent("swapTasks", null);
                return true;
            }
        });
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        RecentsEventBus.getDefault().register(this);
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        RecentsEventBus.getDefault().unregister(this);
    }

    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        if (!(this.mStableInsets.left == insets.getStableInsetLeft() && this.mStableInsets.top == insets.getStableInsetTop() && this.mStableInsets.right == insets.getStableInsetRight() && this.mStableInsets.bottom == insets.getStableInsetBottom())) {
            this.mStableInsets.set(insets.getStableInsetLeft(), insets.getStableInsetTop(), insets.getStableInsetRight(), insets.getStableInsetBottom());
            if (this.mSnapAlgorithm != null) {
                this.mSnapAlgorithm = null;
                initializeSnapAlgorithm();
            }
        }
        return super.onApplyWindowInsets(insets);
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        int minimizeLeft = 0;
        int minimizeTop = 0;
        if (this.mDockSide == 2) {
            minimizeTop = this.mBackground.getTop();
        } else if (this.mDockSide == 1) {
            minimizeLeft = this.mBackground.getLeft();
        } else if (this.mDockSide == 3) {
            minimizeLeft = this.mBackground.getRight() - this.mMinimizedShadow.getWidth();
        }
        this.mMinimizedShadow.layout(minimizeLeft, minimizeTop, this.mMinimizedShadow.getMeasuredWidth() + minimizeLeft, this.mMinimizedShadow.getMeasuredHeight() + minimizeTop);
        if (changed) {
            this.mWindowManagerProxy.setTouchRegion(new Rect(this.mHandle.getLeft(), this.mHandle.getTop(), this.mHandle.getRight(), this.mHandle.getBottom()));
        }
    }

    public void injectDependencies(DividerWindowManager windowManager, DividerState dividerState) {
        this.mWindowManager = windowManager;
        this.mState = dividerState;
    }

    public WindowManagerProxy getWindowManagerProxy() {
        return this.mWindowManagerProxy;
    }

    public boolean startDragging(boolean animate, boolean touching) {
        cancelFlingAnimation();
        if (touching) {
            this.mHandle.setTouching(true, animate);
        }
        this.mDockSide = this.mWindowManagerProxy.getDockSide();
        initializeSnapAlgorithm();
        this.mWindowManagerProxy.setResizing(true);
        if (touching) {
            this.mWindowManager.setSlippery(false);
            liftBackground();
        }
        RecentsEventBus.getDefault().send(new StartedDragingEvent());
        if (this.mDockSide != -1) {
            return true;
        }
        return false;
    }

    public void stopDragging(int position, float velocity, boolean avoidDismissStart, boolean logMetrics) {
        this.mHandle.setTouching(false, true);
        fling(position, velocity, avoidDismissStart, logMetrics);
        this.mWindowManager.setSlippery(true);
        releaseBackground();
    }

    public void stopDragging(int position, DividerSnapAlgorithm.SnapTarget target, long duration, Interpolator interpolator) {
        stopDragging(position, target, duration, 0, 0, interpolator);
    }

    public void stopDragging(int position, DividerSnapAlgorithm.SnapTarget target, long duration, Interpolator interpolator, long endDelay) {
        stopDragging(position, target, duration, 0, endDelay, interpolator);
    }

    public void stopDragging(int position, DividerSnapAlgorithm.SnapTarget target, long duration, long startDelay, long endDelay, Interpolator interpolator) {
        this.mHandle.setTouching(false, true);
        flingTo(position, target, duration, startDelay, endDelay, interpolator);
        this.mWindowManager.setSlippery(true);
        releaseBackground();
    }

    private void stopDragging() {
        this.mHandle.setTouching(false, true);
        this.mWindowManager.setSlippery(true);
        releaseBackground();
    }

    /* access modifiers changed from: private */
    public void updateDockSide() {
        this.mDockSide = this.mWindowManagerProxy.getDockSide();
        this.mMinimizedShadow.setDockSide(this.mDockSide);
    }

    private void initializeSnapAlgorithm() {
        if (this.mSnapAlgorithm == null) {
            DividerSnapAlgorithm dividerSnapAlgorithm = new DividerSnapAlgorithm(getContext().getResources(), this.mDisplayWidth, this.mDisplayHeight, this.mDividerSize, isHorizontalDivision(), this.mStableInsets);
            this.mSnapAlgorithm = dividerSnapAlgorithm;
        }
    }

    public DividerSnapAlgorithm getSnapAlgorithm() {
        initializeSnapAlgorithm();
        return this.mSnapAlgorithm;
    }

    public int getCurrentPosition() {
        getLocationOnScreen(this.mTempInt2);
        if (isHorizontalDivision()) {
            return this.mTempInt2[1] + this.mDividerInsets;
        }
        return this.mTempInt2[0] + this.mDividerInsets;
    }

    public boolean onTouch(View v, MotionEvent event) {
        float f;
        convertToScreenCoordinates(event);
        this.mGestureDetector.onTouchEvent(event);
        this.mCurrentTouchAction = event.getAction() & 255;
        switch (this.mCurrentTouchAction) {
            case 0:
                this.mVelocityTracker = VelocityTracker.obtain();
                this.mVelocityTracker.addMovement(event);
                this.mStartX = (int) event.getX();
                this.mStartY = (int) event.getY();
                boolean result = startDragging(true, true);
                if (!result) {
                    stopDragging();
                }
                this.mStartPosition = getCurrentPosition();
                this.mMoving = false;
                return result;
            case 1:
            case 3:
                this.mVelocityTracker.addMovement(event);
                this.mVelocityTracker.computeCurrentVelocity(1000);
                int position = calculatePosition((int) event.getRawX(), (int) event.getRawY());
                if (isHorizontalDivision()) {
                    f = this.mVelocityTracker.getYVelocity();
                } else {
                    f = this.mVelocityTracker.getXVelocity();
                }
                stopDragging(position, f, false, true);
                this.mMoving = false;
                break;
            case 2:
                this.mVelocityTracker.addMovement(event);
                int x = (int) event.getX();
                int y = (int) event.getY();
                boolean exceededTouchSlop = (isHorizontalDivision() && Math.abs(y - this.mStartY) > this.mTouchSlop) || (!isHorizontalDivision() && Math.abs(x - this.mStartX) > this.mTouchSlop);
                if (!this.mMoving && exceededTouchSlop) {
                    this.mStartX = x;
                    this.mStartY = y;
                    this.mMoving = true;
                }
                if (this.mMoving && this.mDockSide != -1) {
                    resizeStack(calculatePosition(x, y), this.mStartPosition, this.mSnapAlgorithm.calculateSnapTarget(this.mStartPosition, 0.0f, false), Utilities.isAndroidNorNewer());
                    break;
                }
                break;
        }
        return true;
    }

    private void logResizeEvent(DividerSnapAlgorithm.SnapTarget snapTarget) {
        int i = 0;
        if (snapTarget == this.mSnapAlgorithm.getDismissStartTarget()) {
            Context context = this.mContext;
            if (dockSideTopLeft(this.mDockSide)) {
                i = 1;
            }
            MetricsLogger.action(context, 390, i);
        } else if (snapTarget == this.mSnapAlgorithm.getDismissEndTarget()) {
            Context context2 = this.mContext;
            if (dockSideBottomRight(this.mDockSide)) {
                i = 1;
            }
            MetricsLogger.action(context2, 390, i);
        } else if (snapTarget == this.mSnapAlgorithm.getMiddleTarget()) {
            MetricsLogger.action(this.mContext, 389, 0);
        } else {
            int i2 = 2;
            if (snapTarget == this.mSnapAlgorithm.getFirstSplitTarget()) {
                Context context3 = this.mContext;
                if (dockSideTopLeft(this.mDockSide)) {
                    i2 = 1;
                }
                MetricsLogger.action(context3, 389, i2);
            } else if (snapTarget == this.mSnapAlgorithm.getLastSplitTarget()) {
                Context context4 = this.mContext;
                if (!dockSideTopLeft(this.mDockSide)) {
                    i2 = 1;
                }
                MetricsLogger.action(context4, 389, i2);
            }
        }
    }

    private void convertToScreenCoordinates(MotionEvent event) {
        event.setLocation(event.getRawX(), event.getRawY());
    }

    private void fling(int position, float velocity, boolean avoidDismissStart, boolean logMetrics) {
        DividerSnapAlgorithm.SnapTarget snapTarget = this.mSnapAlgorithm.calculateSnapTarget(position, velocity);
        if (avoidDismissStart && snapTarget == this.mSnapAlgorithm.getDismissStartTarget()) {
            snapTarget = this.mSnapAlgorithm.getFirstSplitTarget();
        }
        if (logMetrics) {
            logResizeEvent(snapTarget);
        }
        ValueAnimator anim = getFlingAnimator(position, snapTarget, 0);
        this.mFlingAnimationUtils.apply((Animator) anim, (float) position, (float) snapTarget.position, velocity);
        anim.start();
    }

    private void flingTo(int position, DividerSnapAlgorithm.SnapTarget target, long duration, long startDelay, long endDelay, Interpolator interpolator) {
        ValueAnimator anim = getFlingAnimator(position, target, endDelay);
        anim.setDuration(duration);
        anim.setStartDelay(startDelay);
        anim.setInterpolator(interpolator);
        anim.start();
    }

    private ValueAnimator getFlingAnimator(int position, final DividerSnapAlgorithm.SnapTarget snapTarget, long endDelay) {
        final boolean taskPositionSameAtEnd = snapTarget.flag == 0;
        ValueAnimator anim = ValueAnimator.ofInt(new int[]{position, snapTarget.position});
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                int i;
                DividerView dividerView = DividerView.this;
                int intValue = ((Integer) animation.getAnimatedValue()).intValue();
                if (!taskPositionSameAtEnd || animation.getAnimatedFraction() != 1.0f) {
                    i = snapTarget.taskPosition;
                } else {
                    i = Integer.MAX_VALUE;
                }
                dividerView.resizeStack(intValue, i, snapTarget, Utilities.isAndroidNorNewer());
            }
        });
        final Runnable endAction = new Runnable() {
            public void run() {
                DividerView.this.commitSnapFlags(snapTarget);
                DividerView.this.mWindowManagerProxy.setResizing(false);
                int unused = DividerView.this.mDockSide = -1;
                ValueAnimator unused2 = DividerView.this.mCurrentAnimator = null;
                boolean unused3 = DividerView.this.mEntranceAnimationRunning = false;
                boolean unused4 = DividerView.this.mExitAnimationRunning = false;
                RecentsEventBus.getDefault().send(new StoppedDragingEvent());
            }
        };
        final DividerSnapAlgorithm.SnapTarget snapTarget2 = snapTarget;
        final boolean z = taskPositionSameAtEnd;
        final long j = endDelay;
        AnonymousClass6 r3 = new AnimatorListenerAdapter() {
            private boolean mCancelled;

            public void onAnimationCancel(Animator animation) {
                this.mCancelled = true;
            }

            public void onAnimationEnd(Animator animation) {
                int i;
                DividerView dividerView = DividerView.this;
                int i2 = snapTarget2.position;
                if (z) {
                    i = Integer.MAX_VALUE;
                } else {
                    i = snapTarget2.taskPosition;
                }
                dividerView.resizeStack(i2, i, snapTarget2, true);
                if (j == 0 || this.mCancelled) {
                    endAction.run();
                } else {
                    DividerView.this.mHandler.postDelayed(endAction, j);
                }
            }
        };
        anim.addListener(r3);
        this.mCurrentAnimator = anim;
        return anim;
    }

    private void cancelFlingAnimation() {
        if (this.mCurrentAnimator != null) {
            this.mCurrentAnimator.cancel();
        }
    }

    /* access modifiers changed from: private */
    public void commitSnapFlags(DividerSnapAlgorithm.SnapTarget target) {
        boolean dismissOrMaximize;
        String targetName;
        if (target.flag == 0) {
            if (this.mSnapAlgorithm.getFirstSplitTarget() == target) {
                targetName = "firstSplitTarget";
            } else if (this.mSnapAlgorithm.getMiddleTarget() == target) {
                targetName = "middleTarget";
            } else if (this.mSnapAlgorithm.getLastSplitTarget() == target) {
                targetName = "lastSplitTarget";
            } else {
                targetName = "otherTarget";
            }
            RecentsPushEventHelper.sendMultiWindowEvent("resizeStack", targetName);
            return;
        }
        boolean z = true;
        if (target.flag == 1) {
            if (!(this.mDockSide == 1 || this.mDockSide == 2)) {
                z = false;
            }
            dismissOrMaximize = z;
        } else {
            dismissOrMaximize = this.mDockSide == 3 || this.mDockSide == 4;
        }
        if (dismissOrMaximize) {
            this.mWindowManagerProxy.dismissDockedStack();
        } else {
            this.mWindowManagerProxy.maximizeDockedStack();
        }
        RecentsEventBus.getDefault().send(new ExitMultiModeEvent());
        RecentsPushEventHelper.sendMultiWindowEvent("exitMultiWindow", this.mUnDockByUndockingTaskEvent ? "exitMultiWindowButton" : "Slippery");
        this.mUnDockByUndockingTaskEvent = false;
        this.mWindowManagerProxy.setResizeDimLayer(false, -1, 0, 0.0f);
    }

    private void liftBackground() {
        if (!this.mBackgroundLifted) {
            if (isHorizontalDivision()) {
                this.mBackground.animate().scaleY(1.4f);
            } else {
                this.mBackground.animate().scaleX(1.4f);
            }
            this.mBackground.animate().setInterpolator(Interpolators.TOUCH_RESPONSE).setDuration(150).translationZ((float) this.mTouchElevation).start();
            this.mHandle.animate().setInterpolator(Interpolators.TOUCH_RESPONSE).setDuration(150).translationZ((float) this.mTouchElevation).start();
            this.mBackgroundLifted = true;
        }
    }

    private void releaseBackground() {
        if (this.mBackgroundLifted) {
            this.mBackground.animate().setInterpolator(Interpolators.FAST_OUT_SLOW_IN).setDuration(200).translationZ(0.0f).scaleX(1.0f).scaleY(1.0f).start();
            this.mHandle.animate().setInterpolator(Interpolators.FAST_OUT_SLOW_IN).setDuration(200).translationZ(0.0f).start();
            this.mBackgroundLifted = false;
        }
    }

    public void setMinimizedDockStack(boolean minimized) {
        float f;
        if (Utilities.isAndroidNorNewer()) {
            updateDockSide();
            float f2 = 1.0f;
            this.mHandle.setAlpha(minimized ? 0.0f : 1.0f);
            if (!minimized) {
                resetBackground();
            } else if (this.mDockSide == 2) {
                this.mBackground.setPivotY(0.0f);
                this.mBackground.setScaleY(0.0f);
            } else if (this.mDockSide == 1 || this.mDockSide == 3) {
                View view = this.mBackground;
                if (this.mDockSide == 1) {
                    f = 0.0f;
                } else {
                    f = (float) this.mBackground.getWidth();
                }
                view.setPivotX(f);
                this.mBackground.setScaleX(0.0f);
            }
            MinimizedDockShadow minimizedDockShadow = this.mMinimizedShadow;
            if (!minimized) {
                f2 = 0.0f;
            }
            minimizedDockShadow.setAlpha(f2);
            this.mDockedStackMinimized = minimized;
        }
    }

    public void setMinimizedDockStack(boolean minimized, long animDuration) {
        float f;
        if (Utilities.isAndroidNorNewer()) {
            updateDockSide();
            float f2 = 1.0f;
            this.mHandle.animate().setInterpolator(Interpolators.FAST_OUT_SLOW_IN).setDuration(animDuration).alpha(minimized ? 0.0f : 1.0f).start();
            if (this.mDockSide == 2) {
                this.mBackground.setPivotY(0.0f);
                this.mBackground.animate().scaleY(minimized ? 0.0f : 1.0f);
            } else if (this.mDockSide == 1 || this.mDockSide == 3) {
                View view = this.mBackground;
                if (this.mDockSide == 1) {
                    f = 0.0f;
                } else {
                    f = (float) this.mBackground.getWidth();
                }
                view.setPivotX(f);
                this.mBackground.animate().scaleX(minimized ? 0.0f : 1.0f);
            }
            if (!minimized) {
                this.mBackground.animate().withEndAction(this.mResetBackgroundRunnable);
            }
            ViewPropertyAnimator animate = this.mMinimizedShadow.animate();
            if (!minimized) {
                f2 = 0.0f;
            }
            animate.alpha(f2).setInterpolator(Interpolators.ALPHA_IN).setDuration(animDuration).start();
            this.mBackground.animate().setInterpolator(Interpolators.FAST_OUT_SLOW_IN).setDuration(animDuration).start();
            this.mDockedStackMinimized = minimized;
        }
    }

    public void setAdjustedForIme(boolean adjustedForIme) {
        updateDockSide();
        this.mHandle.setAlpha(adjustedForIme ? 0.0f : 1.0f);
        if (!adjustedForIme) {
            resetBackground();
        } else if (this.mDockSide == 2) {
            this.mBackground.setPivotY(0.0f);
            this.mBackground.setScaleY(0.5f);
        }
        this.mAdjustedForIme = adjustedForIme;
    }

    public void setAdjustedForIme(boolean adjustedForIme, long animDuration) {
        updateDockSide();
        float f = 1.0f;
        this.mHandle.animate().setInterpolator(IME_ADJUST_INTERPOLATOR).setDuration(animDuration).alpha(adjustedForIme ? 0.0f : 1.0f).start();
        if (this.mDockSide == 2) {
            this.mBackground.setPivotY(0.0f);
            ViewPropertyAnimator animate = this.mBackground.animate();
            if (adjustedForIme) {
                f = 0.5f;
            }
            animate.scaleY(f);
        }
        if (!adjustedForIme) {
            this.mBackground.animate().withEndAction(this.mResetBackgroundRunnable);
        }
        this.mBackground.animate().setInterpolator(IME_ADJUST_INTERPOLATOR).setDuration(animDuration).start();
        this.mAdjustedForIme = adjustedForIme;
    }

    /* access modifiers changed from: private */
    public void resetBackground() {
        this.mBackground.setPivotX((float) (this.mBackground.getWidth() / 2));
        this.mBackground.setPivotY((float) (this.mBackground.getHeight() / 2));
        this.mBackground.setScaleX(1.0f);
        this.mBackground.setScaleY(1.0f);
        this.mMinimizedShadow.setAlpha(0.0f);
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateDisplayInfo();
    }

    public void notifyDockSideChanged(int newDockSide) {
        this.mDockSide = newDockSide;
        this.mMinimizedShadow.setDockSide(this.mDockSide);
        requestLayout();
    }

    private void updateDisplayInfo() {
        Display display = ((DisplayManager) this.mContext.getSystemService("display")).getDisplay(0);
        DisplayInfo info = new DisplayInfo();
        display.getDisplayInfo(info);
        this.mDisplayWidth = info.logicalWidth;
        this.mDisplayHeight = info.logicalHeight;
        this.mSnapAlgorithm = null;
        initializeSnapAlgorithm();
    }

    private int calculatePosition(int touchX, int touchY) {
        return isHorizontalDivision() ? calculateYPosition(touchY) : calculateXPosition(touchX);
    }

    public boolean isHorizontalDivision() {
        return getResources().getConfiguration().orientation == 1;
    }

    private int calculateXPosition(int touchX) {
        return (this.mStartPosition + touchX) - this.mStartX;
    }

    private int calculateYPosition(int touchY) {
        return (this.mStartPosition + touchY) - this.mStartY;
    }

    private void alignTopLeft(Rect containingRect, Rect rect) {
        rect.set(containingRect.left, containingRect.top, containingRect.left + rect.width(), containingRect.top + rect.height());
    }

    private void alignBottomRight(Rect containingRect, Rect rect) {
        rect.set(containingRect.right - rect.width(), containingRect.bottom - rect.height(), containingRect.right, containingRect.bottom);
    }

    public void calculateBoundsForPosition(int position, int dockSide, Rect outRect) {
        DockedDividerUtils.calculateBoundsForPosition(position, dockSide, outRect, this.mDisplayWidth, this.mDisplayHeight, this.mDividerSize);
    }

    public void resizeStack(int position, int taskPosition, DividerSnapAlgorithm.SnapTarget taskSnapTarget) {
        resizeStack(position, taskPosition, taskSnapTarget, true);
    }

    public void resizeStack(int position, int taskPosition, DividerSnapAlgorithm.SnapTarget taskSnapTarget, boolean resize) {
        int i = position;
        int i2 = taskPosition;
        DividerSnapAlgorithm.SnapTarget snapTarget = taskSnapTarget;
        boolean z = true;
        boolean resize2 = resize;
        if (!Utilities.isAndroidNorNewer()) {
            this.mWindowManager.update(i);
        }
        calculateBoundsForPosition(i, this.mDockSide, this.mDockedRect);
        if (!this.mDockedRect.equals(this.mLastResizeRect) || this.mEntranceAnimationRunning) {
            if (this.mBackground.getZ() > 0.0f) {
                this.mBackground.invalidate();
            }
            if (resize2) {
                this.mLastResizeRect.set(this.mDockedRect);
            }
            if (this.mEntranceAnimationRunning && i2 != Integer.MAX_VALUE) {
                if (this.mCurrentAnimator != null) {
                    calculateBoundsForPosition(i2, this.mDockSide, this.mDockedTaskRect);
                } else {
                    calculateBoundsForPosition(isHorizontalDivision() ? this.mDisplayHeight : this.mDisplayWidth, this.mDockSide, this.mDockedTaskRect);
                }
                calculateBoundsForPosition(i2, DockedDividerUtils.invertDockSide(this.mDockSide), this.mOtherTaskRect);
                this.mWindowManagerProxy.resizeDockedStack(this.mDockedRect, this.mDockedTaskRect, null, this.mOtherTaskRect, null, resize2);
            } else if (this.mExitAnimationRunning && i2 != Integer.MAX_VALUE) {
                calculateBoundsForPosition(i2, this.mDockSide, this.mDockedTaskRect);
                calculateBoundsForPosition(this.mExitStartPosition, DockedDividerUtils.invertDockSide(this.mDockSide), this.mOtherTaskRect);
                this.mOtherInsetRect.set(this.mOtherTaskRect);
                applyExitAnimationParallax(this.mOtherTaskRect, i);
                this.mWindowManagerProxy.resizeDockedStack(this.mDockedRect, this.mDockedTaskRect, null, this.mOtherTaskRect, this.mOtherInsetRect, resize2);
            } else if (i2 != Integer.MAX_VALUE) {
                calculateBoundsForPosition(i, DockedDividerUtils.invertDockSide(this.mDockSide), this.mOtherRect);
                int dockSideInverted = DockedDividerUtils.invertDockSide(this.mDockSide);
                int taskPositionDocked = restrictDismissingTaskPosition(i2, this.mDockSide, snapTarget);
                int taskPositionOther = restrictDismissingTaskPosition(i2, dockSideInverted, snapTarget);
                calculateBoundsForPosition(taskPositionDocked, this.mDockSide, this.mDockedTaskRect);
                calculateBoundsForPosition(taskPositionOther, dockSideInverted, this.mOtherTaskRect);
                this.mDisplayRect.set(0, 0, this.mDisplayWidth, this.mDisplayHeight);
                alignTopLeft(this.mDockedRect, this.mDockedTaskRect);
                alignTopLeft(this.mOtherRect, this.mOtherTaskRect);
                this.mDockedInsetRect.set(this.mDockedTaskRect);
                this.mOtherInsetRect.set(this.mOtherTaskRect);
                if (dockSideTopLeft(this.mDockSide)) {
                    alignTopLeft(this.mDisplayRect, this.mDockedInsetRect);
                    alignBottomRight(this.mDisplayRect, this.mOtherInsetRect);
                } else {
                    alignBottomRight(this.mDisplayRect, this.mDockedInsetRect);
                    alignTopLeft(this.mDisplayRect, this.mOtherInsetRect);
                }
                DividerSnapAlgorithm.SnapTarget snapTarget2 = snapTarget;
                int i3 = i;
                applyDismissingParallax(this.mDockedTaskRect, this.mDockSide, snapTarget2, i3, taskPositionDocked);
                applyDismissingParallax(this.mOtherTaskRect, dockSideInverted, snapTarget2, i3, taskPositionOther);
                int i4 = taskPositionDocked;
                int i5 = dockSideInverted;
                this.mWindowManagerProxy.resizeDockedStack(this.mDockedRect, this.mDockedTaskRect, this.mDockedInsetRect, this.mOtherTaskRect, this.mOtherInsetRect, resize2);
            } else {
                this.mWindowManagerProxy.resizeDockedStack(this.mDockedRect, null, null, null, null, resize2);
            }
            DividerSnapAlgorithm.SnapTarget closestDismissTarget = this.mSnapAlgorithm.getClosestDismissTarget(i);
            float dimFraction = getDimFraction(i, closestDismissTarget);
            WindowManagerProxy windowManagerProxy = this.mWindowManagerProxy;
            if (dimFraction == 0.0f) {
                z = false;
            }
            windowManagerProxy.setResizeDimLayer(z, getStackIdForDismissTarget(closestDismissTarget), getWindowingModeForDismissTarget(closestDismissTarget), dimFraction);
        }
    }

    private void applyExitAnimationParallax(Rect taskRect, int position) {
        if (this.mDockSide == 2) {
            taskRect.offset(0, (int) (((float) (position - this.mExitStartPosition)) * 0.25f));
        } else if (this.mDockSide == 1) {
            taskRect.offset((int) (((float) (position - this.mExitStartPosition)) * 0.25f), 0);
        } else if (this.mDockSide == 3) {
            taskRect.offset((int) (((float) (this.mExitStartPosition - position)) * 0.25f), 0);
        }
    }

    private float getDimFraction(int position, DividerSnapAlgorithm.SnapTarget dismissTarget) {
        if (this.mEntranceAnimationRunning) {
            return 0.0f;
        }
        float fraction = DIM_INTERPOLATOR.getInterpolation(Math.max(0.0f, Math.min(this.mSnapAlgorithm.calculateDismissingFraction(position), 1.0f)));
        if (hasInsetsAtDismissTarget(dismissTarget)) {
            fraction *= 0.8f;
        }
        return fraction;
    }

    private boolean hasInsetsAtDismissTarget(DividerSnapAlgorithm.SnapTarget dismissTarget) {
        boolean z = false;
        if (isHorizontalDivision()) {
            if (dismissTarget == this.mSnapAlgorithm.getDismissStartTarget()) {
                if (this.mStableInsets.top != 0) {
                    z = true;
                }
                return z;
            }
            if (this.mStableInsets.bottom != 0) {
                z = true;
            }
            return z;
        } else if (dismissTarget == this.mSnapAlgorithm.getDismissStartTarget()) {
            if (this.mStableInsets.left != 0) {
                z = true;
            }
            return z;
        } else {
            if (this.mStableInsets.right != 0) {
                z = true;
            }
            return z;
        }
    }

    private int restrictDismissingTaskPosition(int taskPosition, int dockSide, DividerSnapAlgorithm.SnapTarget snapTarget) {
        if (snapTarget.flag == 1 && dockSideTopLeft(dockSide)) {
            return Math.max(this.mSnapAlgorithm.getFirstSplitTarget().position, this.mStartPosition);
        }
        if (snapTarget.flag != 2 || !dockSideBottomRight(dockSide)) {
            return taskPosition;
        }
        return Math.min(this.mSnapAlgorithm.getLastSplitTarget().position, this.mStartPosition);
    }

    private void applyDismissingParallax(Rect taskRect, int dockSide, DividerSnapAlgorithm.SnapTarget snapTarget, int position, int taskPosition) {
        float fraction = Math.min(1.0f, Math.max(0.0f, this.mSnapAlgorithm.calculateDismissingFraction(position)));
        DividerSnapAlgorithm.SnapTarget dismissTarget = null;
        DividerSnapAlgorithm.SnapTarget splitTarget = null;
        int start = 0;
        if (position <= this.mSnapAlgorithm.getLastSplitTarget().position && dockSideTopLeft(dockSide)) {
            dismissTarget = this.mSnapAlgorithm.getDismissStartTarget();
            splitTarget = this.mSnapAlgorithm.getFirstSplitTarget();
            start = taskPosition;
        } else if (position >= this.mSnapAlgorithm.getLastSplitTarget().position && dockSideBottomRight(dockSide)) {
            dismissTarget = this.mSnapAlgorithm.getDismissEndTarget();
            splitTarget = this.mSnapAlgorithm.getLastSplitTarget();
            start = splitTarget.position;
        }
        if (dismissTarget != null && fraction > 0.0f && isDismissing(splitTarget, position, dockSide)) {
            int offsetPosition = (int) (((float) start) + (((float) (dismissTarget.position - splitTarget.position)) * calculateParallaxDismissingFraction(fraction, dockSide)));
            int width = taskRect.width();
            int height = taskRect.height();
            switch (dockSide) {
                case 1:
                    taskRect.left = offsetPosition - width;
                    taskRect.right = offsetPosition;
                    return;
                case 2:
                    taskRect.top = offsetPosition - height;
                    taskRect.bottom = offsetPosition;
                    return;
                case 3:
                    taskRect.left = this.mDividerSize + offsetPosition;
                    taskRect.right = offsetPosition + width + this.mDividerSize;
                    return;
                case 4:
                    taskRect.top = this.mDividerSize + offsetPosition;
                    taskRect.bottom = offsetPosition + height + this.mDividerSize;
                    return;
                default:
                    return;
            }
        }
    }

    private static float calculateParallaxDismissingFraction(float fraction, int dockSide) {
        float result = SLOWDOWN_INTERPOLATOR.getInterpolation(fraction) / 3.5f;
        if (dockSide == 2) {
            return result / 2.0f;
        }
        return result;
    }

    private static boolean isDismissing(DividerSnapAlgorithm.SnapTarget snapTarget, int position, int dockSide) {
        boolean z = false;
        if (dockSide == 2 || dockSide == 1) {
            if (position < snapTarget.position) {
                z = true;
            }
            return z;
        }
        if (position > snapTarget.position) {
            z = true;
        }
        return z;
    }

    private int getStackIdForDismissTarget(DividerSnapAlgorithm.SnapTarget dismissTarget) {
        if ((dismissTarget.flag == 1 && dockSideTopLeft(this.mDockSide)) || (dismissTarget.flag == 2 && dockSideBottomRight(this.mDockSide))) {
            return 3;
        }
        if (Build.VERSION.SDK_INT >= 26) {
            return 5;
        }
        return 0;
    }

    private int getWindowingModeForDismissTarget(DividerSnapAlgorithm.SnapTarget dismissTarget) {
        if ((dismissTarget.flag != 1 || !dockSideTopLeft(this.mDockSide)) && (dismissTarget.flag != 2 || !dockSideBottomRight(this.mDockSide))) {
            return 4;
        }
        return 3;
    }

    private static boolean dockSideTopLeft(int dockSide) {
        return dockSide == 2 || dockSide == 1;
    }

    private static boolean dockSideBottomRight(int dockSide) {
        return dockSide == 4 || dockSide == 3;
    }

    public void onComputeInternalInsets(ViewTreeObserver.InternalInsetsInfo inoutInfo) {
        inoutInfo.setTouchableInsets(3);
        inoutInfo.touchableRegion.set(this.mHandle.getLeft(), this.mHandle.getTop(), this.mHandle.getRight(), this.mHandle.getBottom());
        inoutInfo.touchableRegion.op(this.mBackground.getLeft(), this.mBackground.getTop(), this.mBackground.getRight(), this.mBackground.getBottom(), Region.Op.UNION);
    }

    public int growsRecents() {
        if (this.mGrowRecents && this.mWindowManagerProxy.getDockSide() == 2 && getCurrentPosition() == getSnapAlgorithm().getLastSplitTarget().position) {
            return getSnapAlgorithm().getMiddleTarget().position;
        }
        return -1;
    }

    public final void onBusEvent(RecentsActivityStartingEvent recentsActivityStartingEvent) {
        if (this.mGrowRecents && getWindowManagerProxy().getDockSide() == 2 && getCurrentPosition() == getSnapAlgorithm().getLastSplitTarget().position) {
            this.mState.growAfterRecentsDrawn = true;
            startDragging(false, false);
        }
    }

    public final void onBusEvent(DockedTopTaskEvent event) {
        if (event.dragMode == -1) {
            this.mState.growAfterRecentsDrawn = false;
            this.mState.animateAfterRecentsDrawn = true;
            startDragging(false, false);
        }
        updateDockSide();
        int position = DockedDividerUtils.calculatePositionForBounds(event.initialRect, this.mDockSide, this.mDividerSize);
        this.mEntranceAnimationRunning = true;
        if (this.mStableInsets.isEmpty()) {
            SystemServicesProxy.getInstance(this.mContext).getStableInsets(this.mStableInsets);
            this.mSnapAlgorithm = null;
            initializeSnapAlgorithm();
        }
        resizeStack(position, this.mSnapAlgorithm.getMiddleTarget().position, this.mSnapAlgorithm.getMiddleTarget());
    }

    public final void onBusEvent(RecentsDrawnEvent drawnEvent) {
        if (this.mState.animateAfterRecentsDrawn) {
            this.mState.animateAfterRecentsDrawn = false;
            updateDockSide();
            this.mHandler.post(new Runnable() {
                public void run() {
                    DividerView.this.stopDragging(DividerView.this.getCurrentPosition(), DividerView.this.mSnapAlgorithm.getMiddleTarget(), (long) DividerView.this.mLongPressEntraceAnimDuration, Interpolators.FAST_OUT_SLOW_IN, 200);
                }
            });
        }
        if (this.mState.growAfterRecentsDrawn) {
            this.mState.growAfterRecentsDrawn = false;
            updateDockSide();
            RecentsEventBus.getDefault().send(new RecentsGrowingEvent());
            stopDragging(getCurrentPosition(), this.mSnapAlgorithm.getMiddleTarget(), 336, Interpolators.FAST_OUT_SLOW_IN);
        }
    }

    public final void onBusEvent(UndockingTaskEvent undockingTaskEvent) {
        DividerSnapAlgorithm.SnapTarget dismissStartTarget;
        int dockSide = this.mWindowManagerProxy.getDockSide();
        if (dockSide != -1 && !this.mDockedStackMinimized) {
            startDragging(false, false);
            if (dockSideTopLeft(dockSide)) {
                dismissStartTarget = this.mSnapAlgorithm.getDismissEndTarget();
            } else {
                dismissStartTarget = this.mSnapAlgorithm.getDismissStartTarget();
            }
            DividerSnapAlgorithm.SnapTarget target = dismissStartTarget;
            this.mExitAnimationRunning = true;
            this.mExitStartPosition = getCurrentPosition();
            stopDragging(this.mExitStartPosition, target, 336, 100, 0, Interpolators.FAST_OUT_SLOW_IN);
            this.mUnDockByUndockingTaskEvent = true;
        }
    }

    public final void onBusEvent(MultiWindowStateChangedEvent event) {
        if (event.inMultiWindow && !Utilities.isAndroidNorNewer()) {
            updateDockSide();
            resizeStack(this.mSnapAlgorithm.getMiddleTarget().position, Integer.MAX_VALUE, this.mSnapAlgorithm.getMiddleTarget());
        }
    }
}
