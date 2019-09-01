package com.android.systemui.recents.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.ArrayMap;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewDebug;
import android.view.ViewParent;
import android.view.animation.Interpolator;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.SwipeHelperForRecents;
import com.android.systemui.recents.events.RecentsEventBus;
import com.android.systemui.recents.events.activity.HideRecentsEvent;
import com.android.systemui.recents.events.component.ChangeTaskLockStateEvent;
import com.android.systemui.recents.events.ui.StackViewScrolledEvent;
import com.android.systemui.recents.events.ui.TaskViewDismissedEvent;
import com.android.systemui.recents.misc.FreePathInterpolator;
import com.android.systemui.recents.misc.RecentsPushEventHelper;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.recents.model.Task;
import com.android.systemui.statusbar.FlingAnimationUtils;
import java.util.ArrayList;
import java.util.List;

class TaskStackViewTouchHandler implements SwipeHelperForRecents.Callback {
    private static final Interpolator OVERSCROLL_INTERP;
    int mActivePointerId = -1;
    TaskView mActiveTaskView = null;
    private boolean mAllowHideRecentsFromBackgroundTap = true;
    Context mContext;
    private ArrayList<TaskViewTransform> mCurrentTaskTransforms = new ArrayList<>();
    private ArrayList<Task> mCurrentTasks = new ArrayList<>();
    float mDownScrollP;
    int mDownX;
    int mDownY;
    private ArrayList<TaskViewTransform> mFinalTaskTransforms = new ArrayList<>();
    FlingAnimationUtils mFlingAnimUtils;
    boolean mInterceptedBySwipeHelper;
    private boolean mIsCancelAnimations = false;
    @ViewDebug.ExportedProperty(category = "recents")
    boolean mIsScrolling;
    float mLastScrollP;
    int mLastY;
    int mMaximumVelocity;
    int mMinimumVelocity;
    private float mOldStackScroll;
    int mOverscrollSize;
    int mRecentsTaskLockDistance;
    ValueAnimator mScrollFlingAnimator;
    int mScrollTouchSlop;
    TaskStackViewScroller mScroller;
    private final StackViewScrolledEvent mStackViewScrolledEvent = new StackViewScrolledEvent();
    TaskStackView mSv;
    SwipeHelperForRecents mSwipeHelper;
    /* access modifiers changed from: private */
    public ArrayMap<View, Animator> mSwipeHelperAnimations = new ArrayMap<>();
    /* access modifiers changed from: private */
    public float mTargetStackScroll;
    private TaskViewTransform mTmpTransform = new TaskViewTransform();
    VelocityTracker mVelocityTracker;
    final int mWindowTouchSlop;

    static {
        Path OVERSCROLL_PATH = new Path();
        OVERSCROLL_PATH.moveTo(0.0f, 0.0f);
        OVERSCROLL_PATH.cubicTo(0.2f, 0.175f, 0.25f, 0.3f, 1.0f, 0.3f);
        OVERSCROLL_INTERP = new FreePathInterpolator(OVERSCROLL_PATH);
    }

    public TaskStackViewTouchHandler(Context context, TaskStackView sv, TaskStackViewScroller scroller) {
        Resources res = context.getResources();
        ViewConfiguration configuration = ViewConfiguration.get(context);
        this.mContext = context;
        this.mSv = sv;
        this.mScroller = scroller;
        this.mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        this.mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        this.mScrollTouchSlop = res.getDimensionPixelSize(R.dimen.recents_view_configuration_touch_slop);
        this.mWindowTouchSlop = configuration.getScaledWindowTouchSlop();
        this.mFlingAnimUtils = new FlingAnimationUtils(context, 0.2f);
        this.mOverscrollSize = res.getDimensionPixelSize(R.dimen.recents_fling_overscroll_distance);
        this.mRecentsTaskLockDistance = res.getDimensionPixelSize(R.dimen.recents_task_lock_distance);
        int dimensionPixelSize = res.getDimensionPixelSize(R.dimen.recents_lock_view_swipe_top_margin);
        int dimensionPixelSize2 = res.getDimensionPixelSize(R.dimen.recents_lock_view_swipe_height);
        this.mSwipeHelper = new SwipeHelperForRecents(0, this, context) {
            /* access modifiers changed from: protected */
            public float getSize(View v) {
                return TaskStackViewTouchHandler.this.getScaledDismissSize();
            }

            /* access modifiers changed from: protected */
            public void prepareDismissAnimation(View v, Animator anim) {
                TaskStackViewTouchHandler.this.mSwipeHelperAnimations.put(v, anim);
            }

            /* access modifiers changed from: protected */
            public void prepareSnapBackAnimation(View v, Animator anim) {
                anim.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
                TaskStackViewTouchHandler.this.mSwipeHelperAnimations.put(v, anim);
            }

            /* access modifiers changed from: protected */
            public float getUnscaledEscapeVelocity() {
                return 800.0f;
            }

            /* access modifiers changed from: protected */
            public void onMoveUpdate(View view, float totalTranslation, float delta) {
            }
        };
        this.mSwipeHelper.setDisableHardwareLayers(true);
    }

    /* access modifiers changed from: package-private */
    public void initOrResetVelocityTracker() {
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        } else {
            this.mVelocityTracker.clear();
        }
    }

    private void initVelocityTrackerIfNotExists() {
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        }
    }

    /* access modifiers changed from: package-private */
    public void recycleVelocityTracker() {
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.recycle();
            this.mVelocityTracker = null;
        }
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (this.mSv.isShowingMenu()) {
            return true;
        }
        this.mInterceptedBySwipeHelper = this.mSwipeHelper.onInterceptTouchEvent(ev);
        if (this.mInterceptedBySwipeHelper) {
            return true;
        }
        return handleTouchEvent(ev);
    }

    public boolean onTouchEvent(MotionEvent ev) {
        if (this.mInterceptedBySwipeHelper && this.mSwipeHelper.onTouchEvent(ev)) {
            return true;
        }
        handleTouchEvent(ev);
        return true;
    }

    public boolean cancelNonDismissTaskAnimations() {
        Utilities.cancelAnimationWithoutCallbacks(this.mScrollFlingAnimator);
        boolean isCancelAnimations = false;
        if (!this.mSwipeHelperAnimations.isEmpty()) {
            List<TaskView> taskViews = this.mSv.getTaskViews();
            for (int i = taskViews.size() - 1; i >= 0; i--) {
                TaskView tv = taskViews.get(i);
                if (!this.mSv.isIgnoredTask(tv.getTask())) {
                    tv.cancelTransformAnimation();
                    this.mSv.getStackAlgorithm().addUnfocusedTaskOverride(tv, this.mTargetStackScroll);
                }
            }
            this.mSv.getStackAlgorithm().setFocusState(0);
            this.mSv.getScroller().setStackScroll(this.mTargetStackScroll, null);
            this.mSwipeHelperAnimations.clear();
            isCancelAnimations = true;
        }
        this.mActiveTaskView = null;
        return isCancelAnimations;
    }

    private boolean handleTouchEvent(MotionEvent ev) {
        MotionEvent motionEvent = ev;
        int newPointerIndex = 1;
        if (this.mSv.isShowingMenu()) {
            return true;
        }
        initVelocityTrackerIfNotExists();
        TaskStackLayoutAlgorithm layoutAlgorithm = this.mSv.mLayoutAlgorithm;
        switch (ev.getAction() & 255) {
            case 0:
                this.mScroller.stopScroller();
                this.mScroller.stopBoundScrollAnimation();
                this.mScroller.resetDeltaScroll();
                if (cancelNonDismissTaskAnimations()) {
                    this.mIsCancelAnimations = true;
                }
                this.mSv.cancelDeferredTaskViewLayoutAnimation();
                this.mDownX = (int) ev.getX();
                this.mDownY = (int) ev.getY();
                this.mLastY = this.mDownY;
                this.mDownScrollP = this.mScroller.getStackScroll();
                this.mActivePointerId = motionEvent.getPointerId(0);
                this.mActiveTaskView = findViewAtPoint(this.mDownX, this.mDownY);
                startScaleAnim();
                initOrResetVelocityTracker();
                this.mVelocityTracker.addMovement(motionEvent);
                break;
            case 1:
                this.mVelocityTracker.addMovement(motionEvent);
                this.mVelocityTracker.computeCurrentVelocity(1000, (float) this.mMaximumVelocity);
                int activePointerIndex = motionEvent.findPointerIndex(this.mActivePointerId);
                if (activePointerIndex < 0 || activePointerIndex >= ev.getPointerCount()) {
                    activePointerIndex = 0;
                    this.mActivePointerId = motionEvent.getPointerId(0);
                }
                int y = (int) motionEvent.getY(activePointerIndex);
                int velocity = (int) this.mVelocityTracker.getYVelocity(this.mActivePointerId);
                if (this.mIsScrolling) {
                    if (this.mScroller.isScrollOutOfBounds()) {
                        this.mScroller.animateBoundScroll(velocity);
                        int i = velocity;
                    } else {
                        float minY = (float) (this.mDownY + layoutAlgorithm.getXForDeltaP(this.mDownScrollP, layoutAlgorithm.mMaxScrollP));
                        float maxY = (float) (this.mDownY + layoutAlgorithm.getXForDeltaP(this.mDownScrollP, layoutAlgorithm.mMinScrollP));
                        int i2 = (int) maxY;
                        float f = maxY;
                        int i3 = (int) minY;
                        float f2 = minY;
                        int i4 = i2;
                        int i5 = velocity;
                        this.mScroller.fling(this.mDownScrollP, this.mDownY, y, velocity, i3, i4, this.mOverscrollSize);
                        this.mSv.invalidate();
                    }
                    if (!this.mSv.mTouchExplorationEnabled) {
                        this.mSv.resetFocusedTask(this.mSv.getFocusedTask());
                    }
                } else {
                    if (this.mActiveTaskView == null) {
                        maybeHideRecentsFromBackgroundTap((int) ev.getX(), (int) ev.getY());
                    } else {
                        startResetAnim();
                    }
                }
                this.mIsCancelAnimations = false;
                this.mActivePointerId = -1;
                this.mIsScrolling = false;
                recycleVelocityTracker();
                break;
            case 2:
                int activePointerIndex2 = motionEvent.findPointerIndex(this.mActivePointerId);
                if (activePointerIndex2 < 0 || activePointerIndex2 >= ev.getPointerCount()) {
                    activePointerIndex2 = 0;
                    this.mActivePointerId = motionEvent.getPointerId(0);
                }
                int y2 = (int) motionEvent.getY(activePointerIndex2);
                int x = (int) motionEvent.getX(activePointerIndex2);
                if (!this.mIsScrolling) {
                    int yDiff = Math.abs(y2 - this.mDownY);
                    int xDiff = Math.abs(x - this.mDownX);
                    if (Math.abs(y2 - this.mDownY) > this.mScrollTouchSlop && yDiff > xDiff) {
                        this.mIsScrolling = true;
                        float stackScroll = this.mScroller.getStackScroll();
                        List<TaskView> taskViews = this.mSv.getTaskViews();
                        for (int i6 = taskViews.size() - 1; i6 >= 0; i6--) {
                            layoutAlgorithm.addUnfocusedTaskOverride(taskViews.get(i6).getTask(), stackScroll);
                        }
                        layoutAlgorithm.setFocusState(0);
                        ViewParent parent = this.mSv.getParent();
                        if (parent != null) {
                            parent.requestDisallowInterceptTouchEvent(true);
                        }
                        MetricsLogger.action(this.mSv.getContext(), 287);
                        startResetAnim();
                    }
                }
                if (this.mIsScrolling) {
                    float deltaP = layoutAlgorithm.getDeltaPForX(this.mDownY, y2);
                    float minScrollP = layoutAlgorithm.mMinScrollP;
                    float maxScrollP = layoutAlgorithm.mMaxScrollP;
                    float curScrollP = this.mDownScrollP + deltaP;
                    if (curScrollP < minScrollP || curScrollP > maxScrollP) {
                        float clampedScrollP = Utilities.clamp(curScrollP, minScrollP, maxScrollP);
                        float overscrollP = curScrollP - clampedScrollP;
                        curScrollP = clampedScrollP + (Math.signum(overscrollP) * 2.3333333f * OVERSCROLL_INTERP.getInterpolation(Math.abs(overscrollP) / 2.3333333f));
                    }
                    float threshold = this.mScroller.mExitRecentOverscrollThreshold;
                    if (this.mLastScrollP > (-threshold) && curScrollP < (-threshold) && y2 - this.mLastY > 0) {
                        this.mSv.performHapticFeedback(1);
                    }
                    this.mDownScrollP += this.mScroller.setDeltaStackScroll(this.mDownScrollP, curScrollP - this.mDownScrollP);
                    this.mStackViewScrolledEvent.updateY(y2 - this.mLastY);
                    RecentsEventBus.getDefault().send(this.mStackViewScrolledEvent);
                    this.mLastScrollP = curScrollP;
                }
                this.mLastY = y2;
                this.mVelocityTracker.addMovement(motionEvent);
                break;
            case 3:
                this.mIsCancelAnimations = false;
                this.mActivePointerId = -1;
                this.mIsScrolling = false;
                recycleVelocityTracker();
                break;
            case 5:
                int index = ev.getActionIndex();
                this.mActivePointerId = motionEvent.getPointerId(index);
                this.mDownX = (int) motionEvent.getX(index);
                this.mDownY = (int) motionEvent.getY(index);
                this.mLastY = this.mDownY;
                this.mDownScrollP = this.mScroller.getStackScroll();
                this.mScroller.resetDeltaScroll();
                this.mVelocityTracker.addMovement(motionEvent);
                break;
            case 6:
                int pointerIndex = ev.getActionIndex();
                if (motionEvent.getPointerId(pointerIndex) == this.mActivePointerId) {
                    if (pointerIndex != 0) {
                        newPointerIndex = 0;
                    }
                    this.mActivePointerId = motionEvent.getPointerId(newPointerIndex);
                    this.mDownX = (int) motionEvent.getX(pointerIndex);
                    this.mDownY = (int) motionEvent.getY(pointerIndex);
                    this.mLastY = this.mDownY;
                    this.mDownScrollP = this.mScroller.getStackScroll();
                }
                this.mVelocityTracker.addMovement(motionEvent);
                break;
        }
        return this.mIsScrolling;
    }

    private void startScaleAnim() {
        if (this.mActiveTaskView != null) {
            this.mActiveTaskView.animate().setDuration(100).scaleX(1.02f).scaleY(1.02f).setInterpolator(Interpolators.EASE_IN_OUT).start();
        }
    }

    private void startResetAnim() {
        if (this.mActiveTaskView != null) {
            this.mActiveTaskView.animate().setDuration(200).scaleX(1.0f).scaleY(1.0f).setListener(new AnimatorListenerAdapter() {
                public void onAnimationStart(Animator animation) {
                    if (TaskStackViewTouchHandler.this.mActiveTaskView != null) {
                        TaskStackViewTouchHandler.this.mActiveTaskView.setIsScollAnimating(true);
                    }
                }

                public void onAnimationEnd(Animator animation) {
                    if (TaskStackViewTouchHandler.this.mActiveTaskView != null) {
                        TaskStackViewTouchHandler.this.mActiveTaskView.setIsScollAnimating(false);
                    }
                }
            }).setInterpolator(Interpolators.QUAD_EASE_OUT).start();
        }
    }

    public void setAllowHideRecentsFromBackgroundTap(boolean isAllow) {
        this.mAllowHideRecentsFromBackgroundTap = isAllow;
    }

    /* access modifiers changed from: package-private */
    public void maybeHideRecentsFromBackgroundTap(int x, int y) {
        int dx = Math.abs(this.mDownX - x);
        int dy = Math.abs(this.mDownY - y);
        if (dx > this.mScrollTouchSlop || dy > this.mScrollTouchSlop) {
            if (this.mIsCancelAnimations) {
                this.mSv.requestLayout();
            }
        } else if (!this.mAllowHideRecentsFromBackgroundTap) {
            Log.w("TaskStackViewTouchHandler", "mAllowHideRecentsFromBackgroundTap == false");
        } else {
            if (Recents.getSystemServices().hasFreeformWorkspaceSupport()) {
                Rect freeformRect = this.mSv.mLayoutAlgorithm.mFreeformRect;
                if (freeformRect.top <= y && y <= freeformRect.bottom && this.mSv.launchFreeformTasks()) {
                    return;
                }
            }
            RecentsEventBus.getDefault().send(new HideRecentsEvent(false, true, false));
            RecentsPushEventHelper.sendRecentsEvent("hideRecents", "clickEmptySpace");
        }
    }

    public boolean onGenericMotionEvent(MotionEvent ev) {
        if ((ev.getSource() & 2) != 2 || (ev.getAction() & 255) != 8) {
            return false;
        }
        if (ev.getAxisValue(9) > 0.0f) {
            this.mSv.setRelativeFocusedTask(true, true, false);
        } else {
            this.mSv.setRelativeFocusedTask(false, true, false);
        }
        return true;
    }

    public View getChildAtPosition(MotionEvent ev) {
        TaskView tv = findViewAtPoint((int) ev.getX(), (int) ev.getY());
        if (tv != null) {
            return tv;
        }
        return null;
    }

    public boolean canChildBeDismissed(View v) {
        return !this.mSwipeHelperAnimations.containsKey(v) && this.mSv.getStack().indexOfStackTask(((TaskView) v).getTask()) != -1 && v.getTranslationY() <= 0.0f;
    }

    public void onBeginManualDrag(TaskView v) {
        this.mActiveTaskView = v;
        this.mSwipeHelperAnimations.put(v, null);
        onBeginDrag(v);
    }

    public void onBeginDrag(View v) {
        TaskView tv = (TaskView) v;
        tv.getViewBounds().reset();
        tv.setTranslationZ(10.0f);
        tv.getHeaderView().animate().setDuration(100).setStartDelay(0).alpha(0.0f).start();
        tv.setClipViewInStack(false);
        tv.setTouchEnabled(false);
        ViewParent parent = this.mSv.getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(true);
        }
    }

    public void onDragEnd(View v) {
        if (v instanceof TaskView) {
            this.mSv.addIgnoreTask(((TaskView) v).getTask());
            this.mCurrentTasks = new ArrayList<>(this.mSv.getStack().getStackTasks());
            TaskStackViewScroller stackScroller = this.mSv.getScroller();
            this.mSv.getCurrentTaskTransforms(this.mCurrentTasks, this.mCurrentTaskTransforms);
            this.mSv.updateLayoutAlgorithm(false);
            this.mOldStackScroll = stackScroller.getStackScroll();
            this.mSv.bindVisibleTaskViews(this.mOldStackScroll, true);
            this.mSv.getLayoutTaskTransforms(this.mOldStackScroll, 0, this.mCurrentTasks, true, this.mFinalTaskTransforms);
            this.mTargetStackScroll = Math.min(this.mOldStackScroll, this.mSv.getStackAlgorithm().mMaxScrollP);
        }
    }

    public float getOldStackScroll() {
        return this.mOldStackScroll;
    }

    public boolean updateSwipeProgress(View v, boolean dismissable, float swipeProgress) {
        if (this.mActiveTaskView == v || this.mSwipeHelperAnimations.containsKey(v)) {
            updateTaskViewTransforms(Interpolators.FAST_OUT_SLOW_IN.getInterpolation(Math.abs(swipeProgress)));
        }
        return true;
    }

    public void onChildDismissed(View v) {
        AnimationProps animationProps;
        TaskView tv = (TaskView) v;
        tv.setClipViewInStack(true);
        tv.setTouchEnabled(true);
        RecentsEventBus recentsEventBus = RecentsEventBus.getDefault();
        Task task = tv.getTask();
        if (this.mSwipeHelperAnimations.containsKey(v)) {
            animationProps = new AnimationProps(200, Interpolators.FAST_OUT_SLOW_IN);
        } else {
            animationProps = null;
        }
        recentsEventBus.send(new TaskViewDismissedEvent(task, tv, animationProps));
        if (this.mSwipeHelperAnimations.containsKey(v)) {
            this.mSv.postDelayed(new Runnable() {
                public void run() {
                    TaskStackViewTouchHandler.this.mSv.getScroller().animateScroll(TaskStackViewTouchHandler.this.mTargetStackScroll, null);
                }
            }, 200);
            this.mSv.getStackAlgorithm().setFocusState(0);
            this.mSv.getStackAlgorithm().clearUnfocusedTaskOverrides();
            this.mSwipeHelperAnimations.remove(v);
        }
        MetricsLogger.histogram(tv.getContext(), "overview_task_dismissed_source", 1);
        RecentsPushEventHelper.sendRecentsEvent("removeTask", tv.getTask().key.getComponent().getPackageName());
    }

    public void onChildSnappedBack(View v, float targetLeft) {
        TaskView tv = (TaskView) v;
        tv.setClipViewInStack(true);
        tv.setTouchEnabled(true);
        tv.setTranslationZ(0.0f);
        this.mSv.removeIgnoreTask(tv.getTask());
        this.mSv.updateLayoutAlgorithm(false);
        this.mSv.relayoutTaskViews(AnimationProps.IMMEDIATE);
        this.mSwipeHelperAnimations.remove(v);
    }

    public void onDragCancelled(View v) {
        TaskView tv = (TaskView) v;
        tv.animate().setDuration(200).scaleX(1.0f).scaleY(1.0f).start();
        tv.getHeaderView().animate().setDuration(150).setStartDelay(150).alpha(1.0f).start();
        Task task = tv.getTask();
        if (v.getTranslationY() > ((float) this.mRecentsTaskLockDistance)) {
            tv.updateLockedFlagVisible(!task.isLocked);
            RecentsEventBus.getDefault().send(new ChangeTaskLockStateEvent(task, !task.isLocked));
        }
    }

    public boolean checkToBeginDrag(View v) {
        return !((TaskView) v).startDrag();
    }

    public boolean isAntiFalsingNeeded() {
        return false;
    }

    public float getFalsingThresholdFactor() {
        return 0.0f;
    }

    private void updateTaskViewTransforms(float dismissFraction) {
        List<TaskView> taskViews = this.mSv.getTaskViews();
        int taskViewCount = taskViews.size();
        for (int i = 0; i < taskViewCount; i++) {
            TaskView tv = taskViews.get(i);
            Task task = tv.getTask();
            if (!this.mSv.isIgnoredTask(task)) {
                int taskIndex = this.mCurrentTasks.indexOf(task);
                if (taskIndex != -1) {
                    if (taskIndex < 0 || taskIndex >= this.mCurrentTaskTransforms.size() || taskIndex >= this.mFinalTaskTransforms.size()) {
                        Log.w("TaskStackViewTouchHandler", "updateTaskViewTransforms error, taskIndex = " + taskIndex + ",  mCurrentTaskTransforms.size() = " + this.mCurrentTaskTransforms.size() + ",  mCurrentTaskTransforms.size() = " + this.mFinalTaskTransforms.size());
                    } else {
                        TaskViewTransform fromTransform = this.mCurrentTaskTransforms.get(taskIndex);
                        TaskViewTransform toTransform = this.mFinalTaskTransforms.get(taskIndex);
                        this.mTmpTransform.copyFrom(fromTransform);
                        this.mTmpTransform.rect.set(Utilities.RECTF_EVALUATOR.evaluate(dismissFraction, fromTransform.rect, toTransform.rect));
                        this.mTmpTransform.dimAlpha = fromTransform.dimAlpha + ((toTransform.dimAlpha - fromTransform.dimAlpha) * dismissFraction);
                        this.mTmpTransform.viewOutlineAlpha = fromTransform.viewOutlineAlpha + ((toTransform.viewOutlineAlpha - fromTransform.viewOutlineAlpha) * dismissFraction);
                        this.mTmpTransform.translationZ = fromTransform.translationZ + ((toTransform.translationZ - fromTransform.translationZ) * dismissFraction);
                        this.mSv.updateTaskViewToTransform(tv, this.mTmpTransform, AnimationProps.IMMEDIATE);
                    }
                }
            }
        }
    }

    private TaskView findViewAtPoint(int x, int y) {
        List<Task> tasks = this.mSv.getStack().getStackTasks();
        for (int i = tasks.size() - 1; i >= 0; i--) {
            TaskView tv = this.mSv.getChildViewForTask(tasks.get(i));
            if (tv != null && tv.getVisibility() == 0 && this.mSv.isTouchPointInView((float) x, (float) y, tv)) {
                return tv;
            }
        }
        return null;
    }

    public float getScaledDismissSize() {
        return 1.0f * ((float) Math.max(this.mSv.getWidth(), this.mSv.getHeight()));
    }
}
