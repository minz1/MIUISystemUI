package com.android.systemui.recents.views;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.events.RecentsEventBus;
import com.android.systemui.recents.events.activity.AnimFirstTaskViewAlphaEvent;
import com.android.systemui.recents.events.activity.CancelEnterRecentsWindowAnimationEvent;
import com.android.systemui.recents.events.activity.ClickTaskViewToLaunchTaskEvent;
import com.android.systemui.recents.events.activity.ConfigurationChangedEvent;
import com.android.systemui.recents.events.activity.DismissRecentsToHomeAnimationStarted;
import com.android.systemui.recents.events.activity.EnterRecentsTaskStackAnimationCompletedEvent;
import com.android.systemui.recents.events.activity.EnterRecentsWindowAnimationCompletedEvent;
import com.android.systemui.recents.events.activity.HideRecentsEvent;
import com.android.systemui.recents.events.activity.HideStackActionButtonEvent;
import com.android.systemui.recents.events.activity.IterateRecentsEvent;
import com.android.systemui.recents.events.activity.LaunchNextTaskRequestEvent;
import com.android.systemui.recents.events.activity.LaunchTaskEvent;
import com.android.systemui.recents.events.activity.LaunchTaskStartedEvent;
import com.android.systemui.recents.events.activity.MultiWindowStateChangedEvent;
import com.android.systemui.recents.events.activity.PackagesChangedEvent;
import com.android.systemui.recents.events.activity.RecentsActivityStartingEvent;
import com.android.systemui.recents.events.activity.RotationChangedEvent;
import com.android.systemui.recents.events.activity.ShowStackActionButtonEvent;
import com.android.systemui.recents.events.activity.StackScrollChangedEvent;
import com.android.systemui.recents.events.component.RecentsVisibilityChangedEvent;
import com.android.systemui.recents.events.ui.AllTaskViewsDismissedEvent;
import com.android.systemui.recents.events.ui.DeleteTaskDataEvent;
import com.android.systemui.recents.events.ui.DismissAllTaskViewsEvent;
import com.android.systemui.recents.events.ui.DismissTaskViewEvent;
import com.android.systemui.recents.events.ui.RecentsGrowingEvent;
import com.android.systemui.recents.events.ui.TaskViewDismissedEvent;
import com.android.systemui.recents.events.ui.UpdateFreeformTaskViewVisibilityEvent;
import com.android.systemui.recents.events.ui.UserInteractionEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragDropTargetChangedEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragEndCancelledEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragEndEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragStartEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragStartInitializeDropTargetsEvent;
import com.android.systemui.recents.events.ui.focus.DismissFocusedTaskViewEvent;
import com.android.systemui.recents.events.ui.focus.FocusNextTaskViewEvent;
import com.android.systemui.recents.events.ui.focus.FocusPreviousTaskViewEvent;
import com.android.systemui.recents.misc.DozeTrigger;
import com.android.systemui.recents.misc.RecentsPushEventHelper;
import com.android.systemui.recents.misc.ReferenceCountedTrigger;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.model.TaskStack;
import com.android.systemui.recents.views.TaskStackLayoutAlgorithm;
import com.android.systemui.recents.views.TaskStackViewScroller;
import com.android.systemui.recents.views.TaskView;
import com.android.systemui.recents.views.ViewPool;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class TaskStackView extends FrameLayout implements TaskStack.TaskStackCallbacks, TaskStackLayoutAlgorithm.TaskStackLayoutAlgorithmCallbacks, TaskStackViewScroller.TaskStackViewScrollerCallbacks, TaskView.TaskViewCallbacks, ViewPool.ViewPoolConsumer<TaskView, Task> {
    private static boolean sIsChangingConfigurations = false;
    /* access modifiers changed from: private */
    public TaskStackAnimationHelper mAnimationHelper;
    @ViewDebug.ExportedProperty(category = "recents")
    private boolean mAwaitingFirstLayout = true;
    private ArrayList<TaskViewTransform> mCurrentTaskTransforms = new ArrayList<>();
    private AnimationProps mDeferredTaskViewLayoutAnimation = null;
    @ViewDebug.ExportedProperty(category = "recents")
    private int mDisplayOrientation = 0;
    @ViewDebug.ExportedProperty(category = "recents")
    private Rect mDisplayRect = new Rect();
    private int mDividerSize;
    @ViewDebug.ExportedProperty(category = "recents")
    private boolean mEnterAnimationComplete = false;
    /* access modifiers changed from: private */
    @ViewDebug.ExportedProperty(deepExport = true, prefix = "focused_task_")
    public Task mFocusedTask;
    private GradientDrawable mFreeformWorkspaceBackground;
    private ObjectAnimator mFreeformWorkspaceBackgroundAnimator;
    private DropTarget mFreeformWorkspaceDropTarget = new DropTarget() {
        public boolean acceptsDrop(int x, int y, int width, int height, boolean isCurrentTarget) {
            if (!isCurrentTarget) {
                return TaskStackView.this.mLayoutAlgorithm.mFreeformRect.contains(x, y);
            }
            return false;
        }
    };
    private ArraySet<Task.TaskKey> mIgnoreTasks = new ArraySet<>();
    @ViewDebug.ExportedProperty(category = "recents")
    private boolean mInMeasureLayout = false;
    private LayoutInflater mInflater;
    @ViewDebug.ExportedProperty(category = "recents")
    private int mInitialState = 1;
    public boolean mIsMultiStateChanging = false;
    private boolean mIsShowingMenu = false;
    private boolean mKeepAlphaWhenRelayout = false;
    private int mLastHeight;
    private int mLastWidth;
    @ViewDebug.ExportedProperty(deepExport = true, prefix = "layout_")
    TaskStackLayoutAlgorithm mLayoutAlgorithm;
    private FrameLayout mMaskWithMenu;
    private ValueAnimator.AnimatorUpdateListener mRequestUpdateClippingListener = new ValueAnimator.AnimatorUpdateListener() {
        public void onAnimationUpdate(ValueAnimator animation) {
            if (!TaskStackView.this.mTaskViewsClipDirty) {
                boolean unused = TaskStackView.this.mTaskViewsClipDirty = true;
                TaskStackView.this.invalidate();
            }
        }
    };
    private boolean mResetToInitialStateWhenResized;
    @ViewDebug.ExportedProperty(category = "recents")
    boolean mScreenPinningEnabled;
    private TaskStackLayoutAlgorithm mStableLayoutAlgorithm;
    @ViewDebug.ExportedProperty(category = "recents")
    private Rect mStableStackBounds = new Rect();
    @ViewDebug.ExportedProperty(category = "recents")
    private Rect mStableWindowRect = new Rect();
    /* access modifiers changed from: private */
    public TaskStack mStack = new TaskStack();
    @ViewDebug.ExportedProperty(category = "recents")
    private Rect mStackBounds = new Rect();
    private DropTarget mStackDropTarget = new DropTarget() {
        public boolean acceptsDrop(int x, int y, int width, int height, boolean isCurrentTarget) {
            if (!isCurrentTarget) {
                return TaskStackView.this.mLayoutAlgorithm.mStackRect.contains(x, y);
            }
            return false;
        }
    };
    @ViewDebug.ExportedProperty(category = "recents")
    private boolean mStackReloaded = false;
    @ViewDebug.ExportedProperty(deepExport = true, prefix = "scroller_")
    private TaskStackViewScroller mStackScroller;
    private int mStartTimerIndicatorDuration;
    private int mTaskCornerRadiusPx;
    private ArrayList<TaskView> mTaskViews = new ArrayList<>();
    /* access modifiers changed from: private */
    @ViewDebug.ExportedProperty(category = "recents")
    public boolean mTaskViewsClipDirty = true;
    private int[] mTmpIntPair = new int[2];
    private Rect mTmpRect = new Rect();
    private ArrayMap<Task.TaskKey, TaskView> mTmpTaskViewMap = new ArrayMap<>();
    private List<TaskView> mTmpTaskViews = new ArrayList();
    private TaskViewTransform mTmpTransform = new TaskViewTransform();
    @ViewDebug.ExportedProperty(category = "recents")
    boolean mTouchExplorationEnabled;
    @ViewDebug.ExportedProperty(deepExport = true, prefix = "touch_")
    private TaskStackViewTouchHandler mTouchHandler;
    /* access modifiers changed from: private */
    @ViewDebug.ExportedProperty(deepExport = true, prefix = "doze_")
    public DozeTrigger mUIDozeTrigger;
    private ViewPool<TaskView, Task> mViewPool;
    @ViewDebug.ExportedProperty(category = "recents")
    private Rect mWindowRect = new Rect();

    public static void setIsChangingConfigurations(boolean isChangingConfigurations) {
        sIsChangingConfigurations = isChangingConfigurations;
    }

    public TaskStackView(Context context) {
        super(context);
        SystemServicesProxy ssp = Recents.getSystemServices();
        Resources res = context.getResources();
        this.mStack.setCallbacks(this);
        this.mViewPool = new ViewPool<>(context, this);
        this.mInflater = LayoutInflater.from(context);
        this.mLayoutAlgorithm = new TaskStackLayoutAlgorithm(context, this);
        this.mStableLayoutAlgorithm = new TaskStackLayoutAlgorithm(context, null);
        this.mStackScroller = new TaskStackViewScroller(context, this, this.mLayoutAlgorithm);
        this.mTouchHandler = new TaskStackViewTouchHandler(context, this, this.mStackScroller);
        this.mAnimationHelper = new TaskStackAnimationHelper(context, this);
        this.mTaskCornerRadiusPx = res.getDimensionPixelSize(R.dimen.recents_task_view_rounded_corners_radius);
        this.mDividerSize = ssp.getDockedDividerSize(context);
        this.mDisplayOrientation = Utilities.getAppConfiguration(this.mContext).orientation;
        this.mDisplayRect = ssp.getDisplayRect();
        this.mUIDozeTrigger = new DozeTrigger(getResources().getInteger(R.integer.recents_task_bar_dismiss_delay_seconds), new Runnable() {
            public void run() {
                List<TaskView> taskViews = TaskStackView.this.getTaskViews();
                int taskViewCount = taskViews.size();
                for (int i = 0; i < taskViewCount; i++) {
                    taskViews.get(i).startNoUserInteractionAnimation();
                }
            }
        });
        setImportantForAccessibility(1);
        this.mFreeformWorkspaceBackground = (GradientDrawable) getContext().getDrawable(R.drawable.recents_freeform_workspace_bg);
        this.mFreeformWorkspaceBackground.setCallback(this);
        if (ssp.hasFreeformWorkspaceSupport()) {
            this.mFreeformWorkspaceBackground.setColor(getContext().getColor(R.color.recents_freeform_workspace_bg_color));
        }
        this.mMaskWithMenu = new FrameLayout(context);
        addView(this.mMaskWithMenu, -1, -1);
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        readSystemFlags();
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    /* access modifiers changed from: package-private */
    public void onReload(boolean isResumingFromVisible) {
        if (!isResumingFromVisible) {
            resetFocusedTask(getFocusedTask());
        }
        List<TaskView> taskViews = new ArrayList<>();
        taskViews.addAll(getTaskViews());
        taskViews.addAll(this.mViewPool.getViews());
        for (int i = taskViews.size() - 1; i >= 0; i--) {
            taskViews.get(i).onReload(isResumingFromVisible);
        }
        readSystemFlags();
        this.mTaskViewsClipDirty = true;
        this.mEnterAnimationComplete = false;
        this.mUIDozeTrigger.stopDozing();
        if (isResumingFromVisible) {
            animateFreeformWorkspaceBackgroundAlpha(this.mLayoutAlgorithm.getStackState().freeformBackgroundAlpha, new AnimationProps(150, Interpolators.FAST_OUT_SLOW_IN));
        } else {
            this.mStackScroller.reset();
            this.mStableLayoutAlgorithm.reset();
            this.mLayoutAlgorithm.reset();
        }
        this.mStackReloaded = true;
        this.mAwaitingFirstLayout = true;
        this.mInitialState = 1;
        requestLayout();
    }

    public void setTasks(TaskStack stack, boolean allowNotifyStackChanges) {
        this.mStack.setTasks(getContext(), stack.computeAllTasksList(), allowNotifyStackChanges && this.mLayoutAlgorithm.isInitialized());
    }

    public TaskStack getStack() {
        return this.mStack;
    }

    public void updateToInitialState() {
        this.mStackScroller.setStackScrollToInitialState();
        this.mLayoutAlgorithm.setTaskOverridesForInitialState(this.mStack, false);
    }

    /* access modifiers changed from: package-private */
    public void updateTaskViewsList() {
        this.mTaskViews.clear();
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View v = getChildAt(i);
            if (v instanceof TaskView) {
                this.mTaskViews.add((TaskView) v);
            }
        }
    }

    /* access modifiers changed from: package-private */
    public List<TaskView> getTaskViews() {
        return this.mTaskViews;
    }

    public TaskView getFrontMostTaskView(boolean stackTasksOnly) {
        List<TaskView> taskViews = getTaskViews();
        int taskViewCount = taskViews.size();
        for (int i = 0; i < taskViewCount; i++) {
            TaskView tv = taskViews.get(i);
            Task task = tv.getTask();
            if (!stackTasksOnly || !task.isFreeformTask()) {
                return tv;
            }
        }
        return null;
    }

    public TaskView getChildViewForTask(Task t) {
        List<TaskView> taskViews = getTaskViews();
        int taskViewCount = taskViews.size();
        for (int i = 0; i < taskViewCount; i++) {
            TaskView tv = taskViews.get(i);
            if (tv.getTask() == t) {
                return tv;
            }
        }
        return null;
    }

    public TaskStackLayoutAlgorithm getStackAlgorithm() {
        return this.mLayoutAlgorithm;
    }

    public TaskStackViewTouchHandler getTouchHandler() {
        return this.mTouchHandler;
    }

    /* access modifiers changed from: package-private */
    public void addIgnoreTask(Task task) {
        this.mIgnoreTasks.add(task.key);
    }

    /* access modifiers changed from: package-private */
    public void removeIgnoreTask(Task task) {
        this.mIgnoreTasks.remove(task.key);
    }

    /* access modifiers changed from: package-private */
    public boolean isIgnoredTask(Task task) {
        return this.mIgnoreTasks.contains(task.key);
    }

    /* access modifiers changed from: package-private */
    public int[] computeVisibleTaskTransforms(ArrayList<TaskViewTransform> taskTransforms, ArrayList<Task> tasks, float curStackScroll, float targetStackScroll, ArraySet<Task.TaskKey> ignoreTasksSet, boolean ignoreTaskOverrides) {
        boolean z;
        ArrayList<TaskViewTransform> arrayList = taskTransforms;
        ArrayList<Task> arrayList2 = tasks;
        int taskCount = tasks.size();
        int[] visibleTaskRange = this.mTmpIntPair;
        visibleTaskRange[0] = -1;
        visibleTaskRange[1] = -1;
        boolean useTargetStackScroll = Float.compare(curStackScroll, targetStackScroll) != 0;
        Utilities.matchTaskListSize(arrayList2, arrayList);
        TaskViewTransform frontTransform = null;
        TaskViewTransform frontTransformAtTarget = null;
        TaskViewTransform transformAtTarget = null;
        int i = taskCount - 1;
        while (i >= 0) {
            Task task = arrayList2.get(i);
            Task task2 = task;
            TaskViewTransform transform = this.mLayoutAlgorithm.getStackTransform(task, curStackScroll, arrayList.get(i), frontTransform, ignoreTaskOverrides);
            if (!useTargetStackScroll || transform.visible) {
                float f = targetStackScroll;
            } else {
                transformAtTarget = this.mLayoutAlgorithm.getStackTransform(task2, targetStackScroll, new TaskViewTransform(), frontTransformAtTarget);
                if (transformAtTarget.visible) {
                    transform.copyFrom(transformAtTarget);
                }
            }
            if (!ignoreTasksSet.contains(task2.key) && !task2.isFreeformTask()) {
                frontTransform = transform;
                frontTransformAtTarget = transformAtTarget;
                if (transform.visible) {
                    if (visibleTaskRange[0] < 0) {
                        visibleTaskRange[0] = i;
                    }
                    z = true;
                    visibleTaskRange[1] = i;
                    i--;
                    boolean z2 = z;
                }
            }
            z = true;
            i--;
            boolean z22 = z;
        }
        float f2 = targetStackScroll;
        ArraySet<Task.TaskKey> arraySet = ignoreTasksSet;
        return visibleTaskRange;
    }

    /* access modifiers changed from: package-private */
    public void bindVisibleTaskViews(float targetStackScroll) {
        bindVisibleTaskViews(targetStackScroll, false);
    }

    /* access modifiers changed from: package-private */
    public void bindVisibleTaskViews(float targetStackScroll, boolean ignoreTaskOverrides) {
        int newFocusedTaskIndex;
        ArrayList<Task> tasks = this.mStack.getStackTasks();
        int[] visibleTaskRange = computeVisibleTaskTransforms(this.mCurrentTaskTransforms, tasks, this.mStackScroller.getStackScroll(), targetStackScroll, this.mIgnoreTasks, ignoreTaskOverrides);
        this.mTmpTaskViewMap.clear();
        List<TaskView> taskViews = getTaskViews();
        int lastFocusedTaskIndex = -1;
        for (int i = taskViews.size() - 1; i >= 0; i--) {
            TaskView tv = taskViews.get(i);
            Task task = tv.getTask();
            if (!this.mIgnoreTasks.contains(task.key)) {
                int taskIndex = this.mStack.indexOfStackTask(task);
                TaskViewTransform transform = null;
                if (taskIndex != -1) {
                    transform = this.mCurrentTaskTransforms.size() > 0 ? this.mCurrentTaskTransforms.get(taskIndex) : null;
                }
                if (task.isFreeformTask() || (transform != null && transform.visible)) {
                    this.mTmpTaskViewMap.put(task.key, tv);
                } else {
                    if (this.mTouchExplorationEnabled && Utilities.isDescendentAccessibilityFocused(tv)) {
                        lastFocusedTaskIndex = taskIndex;
                        resetFocusedTask(task);
                    }
                    this.mViewPool.returnViewToPool(tv);
                }
            }
        }
        for (int i2 = tasks.size() - 1; i2 >= 0; i2--) {
            Task task2 = tasks.get(i2);
            TaskViewTransform transform2 = this.mCurrentTaskTransforms.size() > 0 ? this.mCurrentTaskTransforms.get(i2) : null;
            if (!this.mIgnoreTasks.contains(task2.key) && (task2.isFreeformTask() || transform2.visible)) {
                TaskView tv2 = this.mTmpTaskViewMap.get(task2.key);
                if (tv2 == null) {
                    TaskView tv3 = this.mViewPool.pickUpViewFromPool(task2, task2);
                    if (task2.isFreeformTask()) {
                        updateTaskViewToTransform(tv3, transform2, AnimationProps.IMMEDIATE);
                    } else {
                        TaskViewTransform preloadTransform = new TaskViewTransform();
                        this.mLayoutAlgorithm.getStackTransform((float) i2, (float) i2, this.mTouchHandler.getOldStackScroll(), 0, preloadTransform, null, true, true);
                        preloadTransform.visible = true;
                        updateTaskViewToTransform(tv3, preloadTransform, AnimationProps.IMMEDIATE);
                    }
                } else {
                    int insertIndex = findTaskViewInsertIndex(task2, this.mStack.indexOfStackTask(task2));
                    if (insertIndex != getTaskViews().indexOf(tv2)) {
                        if (tv2 == findFocus()) {
                            clearChildFocus(tv2);
                        }
                        detachViewFromParent(tv2);
                        attachViewToParent(tv2, insertIndex, tv2.getLayoutParams());
                        updateTaskViewsList();
                    }
                }
            }
        }
        if (lastFocusedTaskIndex != -1) {
            if (lastFocusedTaskIndex < visibleTaskRange[1]) {
                newFocusedTaskIndex = visibleTaskRange[1];
            } else {
                newFocusedTaskIndex = visibleTaskRange[0];
            }
            setFocusedTask(newFocusedTaskIndex, false, true);
            TaskView focusedTaskView = getChildViewForTask(this.mFocusedTask);
            if (focusedTaskView != null) {
                focusedTaskView.requestAccessibilityFocus();
            }
        }
    }

    public void relayoutTaskViews(AnimationProps animation) {
        relayoutTaskViews(animation, null, false);
    }

    private void relayoutTaskViews(AnimationProps animation, ArrayMap<Task, AnimationProps> animationOverrides, boolean ignoreTaskOverrides) {
        cancelDeferredTaskViewLayoutAnimation();
        bindVisibleTaskViews(this.mStackScroller.getStackScroll(), ignoreTaskOverrides);
        List<TaskView> taskViews = getTaskViews();
        int taskViewCount = taskViews.size();
        for (int i = 0; i < taskViewCount; i++) {
            TaskView tv = taskViews.get(i);
            Task task = tv.getTask();
            int taskIndex = this.mStack.indexOfStackTask(task);
            if (taskIndex == -1 || taskIndex >= this.mCurrentTaskTransforms.size()) {
                String fcLog = "current task :" + task.toString() + " topActivity: " + task.getTopComponent().toString() + "\n stack :" + this.mStack.toString() + "\n " + Log.getStackTraceString(new Throwable("relayoutTaskViews_ArrayIndexOutOfBoundsException"));
                Slog.e("TaskStackView", fcLog);
                RecentsPushEventHelper.sendRecentsEvent("relayoutTaskViews_ArrayIndexOutOfBoundsException", fcLog);
            } else {
                TaskViewTransform transform = this.mCurrentTaskTransforms.get(taskIndex);
                if (!this.mIgnoreTasks.contains(task.key)) {
                    if (task.isLaunchTarget && Recents.getConfiguration().getLaunchState().launchedViaFsGesture && this.mKeepAlphaWhenRelayout) {
                        transform.alpha = tv.getAlpha();
                    }
                    if (animationOverrides != null && animationOverrides.containsKey(task)) {
                        animation = animationOverrides.get(task);
                    }
                    updateTaskViewToTransform(tv, transform, animation);
                }
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void relayoutTaskViewsOnNextFrame(AnimationProps animation) {
        this.mDeferredTaskViewLayoutAnimation = animation;
        invalidate();
    }

    public void updateTaskViewToTransform(TaskView taskView, TaskViewTransform transform, AnimationProps animation) {
        if (!taskView.isAnimatingTo(transform)) {
            taskView.cancelTransformAnimation();
            taskView.updateViewPropertiesToTaskTransform(transform, animation, this.mRequestUpdateClippingListener);
        }
    }

    public void getCurrentTaskTransforms(ArrayList<Task> tasks, ArrayList<TaskViewTransform> transformsOut) {
        Utilities.matchTaskListSize(tasks, transformsOut);
        int focusState = this.mLayoutAlgorithm.getFocusState();
        int i = tasks.size() - 1;
        while (true) {
            int i2 = i;
            if (i2 >= 0) {
                Task task = tasks.get(i2);
                TaskViewTransform transform = transformsOut.get(i2);
                TaskView tv = getChildViewForTask(task);
                if (tv != null) {
                    transform.fillIn(tv);
                    TaskView taskView = tv;
                } else {
                    TaskView taskView2 = tv;
                    this.mLayoutAlgorithm.getStackTransform(task, this.mStackScroller.getStackScroll(), focusState, transform, null, true, false);
                }
                transform.visible = true;
                i = i2 - 1;
            } else {
                ArrayList<Task> arrayList = tasks;
                ArrayList<TaskViewTransform> arrayList2 = transformsOut;
                return;
            }
        }
    }

    public void getLayoutTaskTransforms(float stackScroll, int focusState, ArrayList<Task> tasks, boolean ignoreTaskOverrides, ArrayList<TaskViewTransform> transformsOut) {
        ArrayList<Task> arrayList = tasks;
        ArrayList<TaskViewTransform> arrayList2 = transformsOut;
        Utilities.matchTaskListSize(arrayList, arrayList2);
        for (int i = tasks.size() - 1; i >= 0; i--) {
            TaskViewTransform transform = arrayList2.get(i);
            this.mLayoutAlgorithm.getStackTransform(arrayList.get(i), stackScroll, focusState, transform, null, true, ignoreTaskOverrides);
            transform.visible = true;
        }
    }

    /* access modifiers changed from: package-private */
    public void cancelDeferredTaskViewLayoutAnimation() {
        this.mDeferredTaskViewLayoutAnimation = null;
    }

    /* access modifiers changed from: package-private */
    public void cancelAllTaskViewAnimations() {
        List<TaskView> taskViews = getTaskViews();
        for (int i = taskViews.size() - 1; i >= 0; i--) {
            TaskView tv = taskViews.get(i);
            if (!this.mIgnoreTasks.contains(tv.getTask().key)) {
                tv.cancelTransformAnimation();
            }
        }
    }

    private void clipTaskViews() {
    }

    public void updateLayoutAlgorithm(boolean boundScrollToNewMinMax) {
        this.mLayoutAlgorithm.update(this.mStack, this.mIgnoreTasks);
        if (Recents.getSystemServices().hasFreeformWorkspaceSupport()) {
            this.mTmpRect.set(this.mLayoutAlgorithm.mFreeformRect);
            this.mFreeformWorkspaceBackground.setBounds(this.mTmpRect);
        }
        if (boundScrollToNewMinMax) {
            this.mStackScroller.boundScroll();
        }
    }

    private void updateLayoutToStableBounds() {
        if (this.mLayoutAlgorithm.setSystemInsets(this.mStableLayoutAlgorithm.mSystemInsets) || !this.mWindowRect.equals(this.mStableWindowRect) || !this.mStackBounds.equals(this.mStableStackBounds)) {
            this.mWindowRect.set(this.mStableWindowRect);
            this.mStackBounds.set(this.mStableStackBounds);
            this.mLayoutAlgorithm.initialize(this.mDisplayRect, this.mWindowRect, this.mStackBounds, TaskStackLayoutAlgorithm.StackState.getStackStateForStack(this.mStack));
            updateLayoutAlgorithm(true);
        }
    }

    public TaskStackViewScroller getScroller() {
        return this.mStackScroller;
    }

    /* access modifiers changed from: private */
    public boolean setFocusedTask(int taskIndex, boolean scrollToTask, boolean requestViewFocus) {
        return setFocusedTask(taskIndex, scrollToTask, requestViewFocus, 0);
    }

    private boolean setFocusedTask(int focusTaskIndex, boolean scrollToTask, boolean requestViewFocus, int timerIndicatorDuration) {
        int newFocusedTaskIndex = this.mStack.getTaskCount() > 0 ? Utilities.clamp(focusTaskIndex, 0, this.mStack.getTaskCount() - 1) : -1;
        Task newFocusedTask = newFocusedTaskIndex != -1 ? this.mStack.getStackTasks().get(newFocusedTaskIndex) : null;
        if (this.mFocusedTask != null) {
            if (timerIndicatorDuration > 0) {
                TaskView tv = getChildViewForTask(this.mFocusedTask);
                if (tv != null) {
                    tv.getHeaderView().cancelFocusTimerIndicator();
                }
            }
            resetFocusedTask(this.mFocusedTask);
        }
        this.mFocusedTask = newFocusedTask;
        if (newFocusedTask == null) {
            return false;
        }
        if (timerIndicatorDuration > 0) {
            TaskView tv2 = getChildViewForTask(this.mFocusedTask);
            if (tv2 != null) {
                tv2.getHeaderView().startFocusTimerIndicator(timerIndicatorDuration);
            } else {
                this.mStartTimerIndicatorDuration = timerIndicatorDuration;
            }
        }
        if (scrollToTask) {
            if (!this.mEnterAnimationComplete) {
                cancelAllTaskViewAnimations();
            }
            this.mLayoutAlgorithm.clearUnfocusedTaskOverrides();
            return this.mAnimationHelper.startScrollToFocusedTaskAnimation(newFocusedTask, requestViewFocus);
        }
        TaskView newFocusedTaskView = getChildViewForTask(newFocusedTask);
        if (newFocusedTaskView == null) {
            return false;
        }
        newFocusedTaskView.setFocusedState(true, requestViewFocus);
        return false;
    }

    public void setRelativeFocusedTask(boolean forward, boolean stackTasksOnly, boolean animated) {
        setRelativeFocusedTask(forward, stackTasksOnly, animated, false, 0);
    }

    public void setRelativeFocusedTask(boolean forward, boolean stackTasksOnly, boolean animated, boolean cancelWindowAnimations, int timerIndicatorDuration) {
        Task focusedTask = getFocusedTask();
        int newIndex = this.mStack.indexOfStackTask(focusedTask);
        if (focusedTask == null) {
            float stackScroll = this.mStackScroller.getStackScroll();
            ArrayList<Task> tasks = this.mStack.getStackTasks();
            int taskCount = tasks.size();
            if (forward) {
                newIndex = taskCount - 1;
                while (newIndex >= 0 && Float.compare(this.mLayoutAlgorithm.getStackScrollForTask(tasks.get(newIndex)), stackScroll) > 0) {
                    newIndex--;
                }
            } else {
                int newIndex2 = 0;
                while (newIndex < taskCount && Float.compare(this.mLayoutAlgorithm.getStackScrollForTask(tasks.get(newIndex)), stackScroll) < 0) {
                    newIndex2 = newIndex + 1;
                }
            }
        } else if (stackTasksOnly) {
            List<Task> tasks2 = this.mStack.getStackTasks();
            if (focusedTask.isFreeformTask()) {
                TaskView tv = getFrontMostTaskView(stackTasksOnly);
                if (tv != null) {
                    newIndex = this.mStack.indexOfStackTask(tv.getTask());
                }
            } else {
                int tmpNewIndex = (forward ? -1 : 1) + newIndex;
                if (tmpNewIndex >= 0 && tmpNewIndex < tasks2.size() && !tasks2.get(tmpNewIndex).isFreeformTask()) {
                    newIndex = tmpNewIndex;
                }
            }
        } else {
            int taskCount2 = this.mStack.getTaskCount();
            if (taskCount2 > 0) {
                newIndex = (((forward ? -1 : 1) + newIndex) + taskCount2) % taskCount2;
            }
        }
        if (newIndex != -1 && setFocusedTask(newIndex, true, true, timerIndicatorDuration) && cancelWindowAnimations) {
            RecentsEventBus.getDefault().send(new CancelEnterRecentsWindowAnimationEvent(null));
        }
    }

    /* access modifiers changed from: package-private */
    public void resetFocusedTask(Task task) {
        if (task != null) {
            TaskView tv = getChildViewForTask(task);
            if (tv != null) {
                tv.setFocusedState(false, false);
            }
        }
        this.mFocusedTask = null;
    }

    /* access modifiers changed from: package-private */
    public Task getFocusedTask() {
        return this.mFocusedTask;
    }

    /* access modifiers changed from: package-private */
    public Task getAccessibilityFocusedTask() {
        List<TaskView> taskViews = getTaskViews();
        int taskViewCount = taskViews.size();
        for (int i = 0; i < taskViewCount; i++) {
            TaskView tv = taskViews.get(i);
            if (Utilities.isDescendentAccessibilityFocused(tv)) {
                return tv.getTask();
            }
        }
        TaskView frontTv = getFrontMostTaskView(true);
        if (frontTv != null) {
            return frontTv.getTask();
        }
        return null;
    }

    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        List<TaskView> taskViews = getTaskViews();
        int taskViewCount = taskViews.size();
        if (taskViewCount > 0) {
            TaskView frontMostTask = taskViews.get(taskViewCount - 1);
            event.setFromIndex(this.mStack.indexOfStackTask(taskViews.get(0).getTask()));
            event.setToIndex(this.mStack.indexOfStackTask(frontMostTask.getTask()));
            event.setContentDescription(frontMostTask.getTask().title);
        }
        event.setItemCount(this.mStack.getTaskCount());
        int stackHeight = this.mLayoutAlgorithm.mStackRect.height();
        event.setScrollY((int) (this.mStackScroller.getStackScroll() * ((float) stackHeight)));
        event.setMaxScrollY((int) (this.mLayoutAlgorithm.mMaxScrollP * ((float) stackHeight)));
    }

    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        if (getTaskViews().size() > 1) {
            Task focusedTask = getAccessibilityFocusedTask();
            info.setScrollable(true);
            int focusedTaskIndex = this.mStack.indexOfStackTask(focusedTask);
            if (focusedTaskIndex > 0) {
                info.addAction(8192);
            }
            if (focusedTaskIndex >= 0 && focusedTaskIndex < this.mStack.getTaskCount() - 1) {
                info.addAction(4096);
            }
        }
    }

    public CharSequence getAccessibilityClassName() {
        return ScrollView.class.getName();
    }

    public boolean performAccessibilityAction(int action, Bundle arguments) {
        if (super.performAccessibilityAction(action, arguments)) {
            return true;
        }
        int taskIndex = this.mStack.indexOfStackTask(getAccessibilityFocusedTask());
        if (taskIndex >= 0 && taskIndex < this.mStack.getTaskCount()) {
            if (action == 4096) {
                setFocusedTask(taskIndex + 1, true, true, 0);
                return true;
            } else if (action == 8192) {
                setFocusedTask(taskIndex - 1, true, true, 0);
                return true;
            }
        }
        return false;
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return this.mTouchHandler.onInterceptTouchEvent(ev);
    }

    public boolean onTouchEvent(MotionEvent ev) {
        return this.mTouchHandler.onTouchEvent(ev);
    }

    public boolean onGenericMotionEvent(MotionEvent ev) {
        return this.mTouchHandler.onGenericMotionEvent(ev);
    }

    public void computeScroll() {
        if (this.mStackScroller.computeScroll()) {
            sendAccessibilityEvent(4096);
        }
        if (this.mDeferredTaskViewLayoutAnimation != null) {
            relayoutTaskViews(this.mDeferredTaskViewLayoutAnimation);
            this.mTaskViewsClipDirty = true;
            this.mDeferredTaskViewLayoutAnimation = null;
        }
        RecentsEventBus.getDefault().send(new StackScrollChangedEvent((int) ((-this.mStackScroller.getStackScroll()) * ((float) this.mLayoutAlgorithm.mTaskRect.height()))));
        if (this.mTaskViewsClipDirty) {
            clipTaskViews();
        }
    }

    public TaskStackLayoutAlgorithm.VisibilityReport computeStackVisibilityReport() {
        return this.mLayoutAlgorithm.computeStackVisibilityReport(this.mStack.getStackTasks());
    }

    public void setSystemInsets(Rect systemInsets) {
        if ((false | this.mStableLayoutAlgorithm.setSystemInsets(systemInsets)) || this.mLayoutAlgorithm.setSystemInsets(systemInsets)) {
            requestLayout();
        }
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        boolean resetToInitialState = true;
        this.mInMeasureLayout = true;
        int width = View.MeasureSpec.getSize(widthMeasureSpec);
        int height = View.MeasureSpec.getSize(heightMeasureSpec);
        this.mLayoutAlgorithm.getTaskStackBounds(this.mDisplayRect, new Rect(0, 0, width, height), this.mLayoutAlgorithm.mSystemInsets.top, this.mLayoutAlgorithm.mSystemInsets.left, this.mLayoutAlgorithm.mSystemInsets.right, this.mTmpRect);
        if (!this.mTmpRect.equals(this.mStableStackBounds) && !this.mIsMultiStateChanging) {
            this.mStableStackBounds.set(this.mTmpRect);
            this.mStackBounds.set(this.mTmpRect);
            this.mStableWindowRect.set(0, 0, width, height);
            this.mWindowRect.set(0, 0, width, height);
        }
        this.mStableLayoutAlgorithm.initialize(this.mDisplayRect, this.mStableWindowRect, this.mStableStackBounds, TaskStackLayoutAlgorithm.StackState.getStackStateForStack(this.mStack));
        this.mLayoutAlgorithm.initialize(this.mDisplayRect, this.mWindowRect, this.mStackBounds, TaskStackLayoutAlgorithm.StackState.getStackStateForStack(this.mStack));
        updateLayoutAlgorithm(false);
        if ((width == this.mLastWidth && height == this.mLastHeight) || !this.mResetToInitialStateWhenResized) {
            resetToInitialState = false;
        }
        if (this.mAwaitingFirstLayout || this.mInitialState != 0 || resetToInitialState) {
            if (this.mInitialState != 2 || resetToInitialState) {
                updateToInitialState();
                this.mResetToInitialStateWhenResized = false;
            }
            if (!this.mAwaitingFirstLayout) {
                this.mInitialState = 0;
            }
        }
        bindVisibleTaskViews(this.mStackScroller.getStackScroll(), false);
        this.mTmpTaskViews.clear();
        this.mTmpTaskViews.addAll(getTaskViews());
        this.mTmpTaskViews.addAll(this.mViewPool.getViews());
        int taskViewCount = this.mTmpTaskViews.size();
        for (int i = 0; i < taskViewCount; i++) {
            measureTaskView(this.mTmpTaskViews.get(i));
        }
        measureMaskView(width, height);
        setMeasuredDimension(width, height);
        this.mLastWidth = width;
        this.mLastHeight = height;
        this.mInMeasureLayout = false;
    }

    private void measureTaskView(TaskView tv) {
        Rect padding = new Rect();
        if (tv.getBackground() != null) {
            tv.getBackground().getPadding(padding);
        }
        this.mTmpRect.set(this.mStableLayoutAlgorithm.mTaskRect);
        this.mTmpRect.union(this.mLayoutAlgorithm.mTaskRect);
        tv.measure(View.MeasureSpec.makeMeasureSpec(this.mTmpRect.width() + padding.left + padding.right, 1073741824), View.MeasureSpec.makeMeasureSpec(this.mTmpRect.height() + padding.top + padding.bottom, 1073741824));
    }

    private void measureMaskView(int width, int height) {
        if (this.mMaskWithMenu.getVisibility() != 8) {
            measureChild(this.mMaskWithMenu, View.MeasureSpec.makeMeasureSpec(width, 1073741824), View.MeasureSpec.makeMeasureSpec(height, 1073741824));
        }
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        this.mTmpTaskViews.clear();
        this.mTmpTaskViews.addAll(getTaskViews());
        this.mTmpTaskViews.addAll(this.mViewPool.getViews());
        int taskViewCount = this.mTmpTaskViews.size();
        for (int i = 0; i < taskViewCount; i++) {
            layoutTaskView(changed, this.mTmpTaskViews.get(i));
            this.mTmpTaskViews.get(i).getViewBounds().reset();
        }
        layoutMaskView();
        if (changed && this.mStackScroller.isScrollOutOfBounds()) {
            this.mStackScroller.boundScroll();
        }
        relayoutTaskViews(AnimationProps.IMMEDIATE);
        clipTaskViews();
        if (this.mAwaitingFirstLayout) {
            this.mInitialState = 0;
            onFirstLayout();
            if (this.mStackReloaded) {
                this.mAwaitingFirstLayout = false;
                tryStartEnterAnimation();
            }
        }
    }

    private void layoutTaskView(boolean changed, TaskView tv) {
        Task task = tv.getTask();
        if (task == null || !this.mIgnoreTasks.contains(task.key)) {
            if (changed) {
                Rect padding = new Rect();
                if (tv.getBackground() != null) {
                    tv.getBackground().getPadding(padding);
                }
                this.mTmpRect.set(this.mStableLayoutAlgorithm.mTaskRect);
                this.mTmpRect.union(this.mLayoutAlgorithm.mTaskRect);
                tv.cancelTransformAnimation();
                tv.layout(this.mTmpRect.left - padding.left, this.mTmpRect.top - padding.top, this.mTmpRect.right + padding.right, this.mTmpRect.bottom + padding.bottom);
            } else {
                tv.layout(tv.getLeft(), tv.getTop(), tv.getRight(), tv.getBottom());
            }
        }
    }

    private void layoutMaskView() {
        if (this.mMaskWithMenu.getVisibility() != 8) {
            this.mMaskWithMenu.layout(0, 0, getMeasuredWidth(), getMeasuredHeight());
        }
    }

    /* access modifiers changed from: package-private */
    public void onFirstLayout() {
        this.mAnimationHelper.prepareForEnterAnimation();
        animateFreeformWorkspaceBackgroundAlpha(this.mLayoutAlgorithm.getStackState().freeformBackgroundAlpha, new AnimationProps(150, Interpolators.FAST_OUT_SLOW_IN));
        int focusedTaskIndex = Recents.getConfiguration().getLaunchState().getInitialFocusTaskIndex(this.mStack.getTaskCount());
        if (focusedTaskIndex != -1) {
            setFocusedTask(focusedTaskIndex, false, false);
        }
        if (this.mStackScroller.getStackScroll() >= 0.3f || this.mStack.getTaskCount() <= 0) {
            RecentsEventBus.getDefault().send(new HideStackActionButtonEvent());
        } else {
            RecentsEventBus.getDefault().send(new ShowStackActionButtonEvent(false));
        }
    }

    public boolean isTouchPointInView(float x, float y, TaskView tv) {
        this.mTmpRect.set(tv.getLeft(), tv.getTop(), tv.getRight(), tv.getBottom());
        this.mTmpRect.offset((int) tv.getTranslationX(), (int) tv.getTranslationY());
        return this.mTmpRect.contains((int) x, (int) y);
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (Recents.getSystemServices().hasFreeformWorkspaceSupport() && this.mFreeformWorkspaceBackground.getAlpha() > 0) {
            this.mFreeformWorkspaceBackground.draw(canvas);
        }
    }

    /* access modifiers changed from: protected */
    public boolean verifyDrawable(Drawable who) {
        if (who == this.mFreeformWorkspaceBackground) {
            return true;
        }
        return super.verifyDrawable(who);
    }

    public boolean launchFreeformTasks() {
        ArrayList<Task> tasks = this.mStack.getFreeformTasks();
        if (!tasks.isEmpty()) {
            Task frontTask = tasks.get(tasks.size() - 1);
            if (frontTask != null && frontTask.isFreeformTask()) {
                RecentsEventBus recentsEventBus = RecentsEventBus.getDefault();
                LaunchTaskEvent launchTaskEvent = new LaunchTaskEvent(getChildViewForTask(frontTask), frontTask, null, -1, false);
                recentsEventBus.send(launchTaskEvent);
                return true;
            }
        }
        return false;
    }

    public void onStackTaskAdded(TaskStack stack, Task newTask) {
        AnimationProps animationProps;
        updateLayoutAlgorithm(true);
        if (this.mAwaitingFirstLayout) {
            animationProps = AnimationProps.IMMEDIATE;
        } else {
            animationProps = new AnimationProps(200, Interpolators.FAST_OUT_SLOW_IN);
        }
        relayoutTaskViews(animationProps);
    }

    public void onStackTaskRemoved(TaskStack stack, Task removedTask, Task newFrontMostTask, AnimationProps animation, boolean fromDockGesture) {
        int i;
        if (this.mFocusedTask == removedTask) {
            resetFocusedTask(removedTask);
        }
        TaskView tv = getChildViewForTask(removedTask);
        if (tv != null) {
            this.mViewPool.returnViewToPool(tv);
        }
        removeIgnoreTask(removedTask);
        if (animation != null) {
            updateLayoutAlgorithm(true);
            relayoutTaskViews(animation);
        }
        if (this.mScreenPinningEnabled && newFrontMostTask != null) {
            TaskView frontTv = getChildViewForTask(newFrontMostTask);
            if (frontTv != null) {
                frontTv.showActionButton(true, 200);
            }
        }
        if (this.mStack.getTaskCount() == 0) {
            RecentsEventBus recentsEventBus = RecentsEventBus.getDefault();
            if (Recents.getSystemServices().hasDockedTask()) {
                i = R.string.recents_empty_message_multi_window;
            } else {
                i = R.string.recents_empty_message;
            }
            recentsEventBus.send(new AllTaskViewsDismissedEvent(i, true, fromDockGesture));
        }
    }

    public void onStackTasksRemoved(TaskStack stack) {
        int i;
        if (stack.getTaskCount() == 0) {
            resetFocusedTask(getFocusedTask());
        }
        List<TaskView> taskViews = new ArrayList<>();
        for (TaskView tv : getTaskViews()) {
            if (tv.getTask() == null || !tv.getTask().isProtected()) {
                taskViews.add(tv);
            }
        }
        boolean z = true;
        for (int i2 = taskViews.size() - 1; i2 >= 0; i2--) {
            this.mViewPool.returnViewToPool(taskViews.get(i2));
        }
        this.mIgnoreTasks.clear();
        RecentsEventBus recentsEventBus = RecentsEventBus.getDefault();
        if (Recents.getSystemServices().hasDockedTask()) {
            i = R.string.recents_empty_message_multi_window;
        } else {
            i = R.string.recents_empty_message_dismissed_all;
        }
        if (stack.getTaskCount() != 0) {
            z = false;
        }
        recentsEventBus.send(new AllTaskViewsDismissedEvent(i, z));
    }

    public void onStackTasksUpdated(TaskStack stack) {
        updateLayoutAlgorithm(false);
        relayoutTaskViews(AnimationProps.IMMEDIATE);
        List<TaskView> taskViews = getTaskViews();
        int taskViewCount = taskViews.size();
        for (int i = 0; i < taskViewCount; i++) {
            TaskView tv = taskViews.get(i);
            bindTaskView(tv, tv.getTask());
        }
    }

    public TaskView createView(Context context) {
        return (TaskView) this.mInflater.inflate(R.layout.recents_task_view, this, false);
    }

    public void onReturnViewToPool(TaskView tv) {
        unbindTaskView(tv, tv.getTask());
        tv.clearAccessibilityFocus();
        tv.resetViewProperties();
        tv.setFocusedState(false, false);
        tv.setClipViewInStack(false);
        if (this.mScreenPinningEnabled) {
            tv.hideActionButton(false, 0, false, null);
        }
        if (tv == findFocus()) {
            clearChildFocus(tv);
        }
        detachViewFromParent(tv);
        updateTaskViewsList();
    }

    public void onPickUpViewFromPool(TaskView tv, Task task, boolean isNewView) {
        int insertIndex = findTaskViewInsertIndex(task, this.mStack.indexOfStackTask(task));
        if (!isNewView) {
            attachViewToParent(tv, insertIndex, tv.getLayoutParams());
        } else if (this.mInMeasureLayout) {
            addView(tv, insertIndex);
        } else {
            ViewGroup.LayoutParams params = tv.getLayoutParams();
            if (params == null) {
                params = generateDefaultLayoutParams();
            }
            addViewInLayout(tv, insertIndex, params, true);
            measureTaskView(tv);
            layoutTaskView(true, tv);
        }
        updateTaskViewsList();
        bindTaskView(tv, task);
        if (this.mUIDozeTrigger.isAsleep()) {
            tv.setNoUserInteractionState();
        }
        tv.setCallbacks(this);
        tv.setTouchEnabled(true);
        tv.setClipViewInStack(true);
        tv.setImportantForAccessibility(0);
        if (this.mFocusedTask == task) {
            tv.setFocusedState(true, false);
            if (this.mStartTimerIndicatorDuration > 0) {
                tv.getHeaderView().startFocusTimerIndicator(this.mStartTimerIndicatorDuration);
                this.mStartTimerIndicatorDuration = 0;
            }
        }
        if (this.mScreenPinningEnabled && tv.getTask() == this.mStack.getStackFrontMostTask(false)) {
            tv.showActionButton(false, 0);
        }
    }

    public boolean hasPreferredData(TaskView tv, Task preferredData) {
        return tv.getTask() == preferredData;
    }

    private void bindTaskView(TaskView tv, Task task) {
        tv.onTaskBound(task, this.mTouchExplorationEnabled, this.mDisplayOrientation, this.mDisplayRect);
        Recents.getTaskLoader().loadTaskData(task);
    }

    private void unbindTaskView(TaskView tv, Task task) {
        Recents.getTaskLoader().unloadTaskData(task);
    }

    public void onTaskViewClipStateChanged(TaskView tv) {
        if (!this.mTaskViewsClipDirty) {
            this.mTaskViewsClipDirty = true;
            invalidate();
        }
    }

    public void onFocusStateChanged(int prevFocusState, int curFocusState) {
        if (this.mDeferredTaskViewLayoutAnimation == null) {
            this.mUIDozeTrigger.poke();
            relayoutTaskViewsOnNextFrame(AnimationProps.IMMEDIATE);
        }
    }

    public void onStackScrollChanged(float prevScroll, float curScroll, AnimationProps animation) {
        this.mUIDozeTrigger.poke();
        if (animation != null) {
            relayoutTaskViewsOnNextFrame(animation);
        }
        if (!this.mEnterAnimationComplete) {
            return;
        }
        if (prevScroll > 0.3f && curScroll <= 0.3f && this.mStack.getTaskCount() > 0) {
            RecentsEventBus.getDefault().send(new ShowStackActionButtonEvent(true));
        } else if (prevScroll < 0.3f && curScroll >= 0.3f) {
            RecentsEventBus.getDefault().send(new HideStackActionButtonEvent());
        }
    }

    public final void onBusEvent(PackagesChangedEvent event) {
        ArraySet<ComponentName> removedComponents = this.mStack.computeComponentsRemoved(event.packageName, event.userId);
        ArrayList<Task> tasks = this.mStack.getStackTasks();
        for (int i = tasks.size() - 1; i >= 0; i--) {
            Task t = tasks.get(i);
            if (removedComponents.contains(t.key.getComponent())) {
                TaskView tv = getChildViewForTask(t);
                if (tv != null) {
                    tv.dismissTask();
                } else {
                    this.mStack.removeTask(t, AnimationProps.IMMEDIATE, false);
                }
            }
        }
    }

    public final void onBusEvent(LaunchTaskEvent event) {
        this.mUIDozeTrigger.stopDozing();
    }

    public final void onBusEvent(LaunchNextTaskRequestEvent event) {
        int launchTaskIndex;
        TaskView tv = getChildViewForTask(this.mStack.getLaunchTarget());
        if (tv != null) {
            tv.onLaunchNextTask();
        }
        int launchTaskIndex2 = this.mStack.indexOfStackTask(this.mStack.getLaunchTarget());
        if (launchTaskIndex2 != -1) {
            launchTaskIndex = Utilities.clamp(launchTaskIndex2 + 1, 0, this.mStack.getTaskCount() - 1);
        } else {
            launchTaskIndex = Math.min(this.mStack.getTaskCount() - 1, 0);
        }
        if (this.mStack.getTaskCount() == 0) {
            RecentsEventBus.getDefault().send(new HideRecentsEvent(false, true, false));
        } else if (launchTaskIndex != -1) {
            cancelAllTaskViewAnimations();
            final Task launchTask = this.mStack.getStackTasks().get(launchTaskIndex);
            float absScrollDiff = Math.abs(0.0f - this.mStackScroller.getStackScroll());
            if (getChildViewForTask(launchTask) == null || absScrollDiff > 0.35f) {
                this.mStackScroller.animateScroll(0.0f, (int) (216.0f + (32.0f * absScrollDiff)), new Runnable() {
                    public void run() {
                        RecentsEventBus recentsEventBus = RecentsEventBus.getDefault();
                        LaunchTaskEvent launchTaskEvent = new LaunchTaskEvent(TaskStackView.this.getChildViewForTask(launchTask), launchTask, null, -1, false);
                        recentsEventBus.send(launchTaskEvent);
                    }
                });
            } else {
                RecentsEventBus recentsEventBus = RecentsEventBus.getDefault();
                LaunchTaskEvent launchTaskEvent = new LaunchTaskEvent(getChildViewForTask(launchTask), launchTask, null, -1, false);
                recentsEventBus.send(launchTaskEvent);
            }
            MetricsLogger.action(getContext(), 318, launchTask.key.getComponent().toString());
        }
    }

    public final void onBusEvent(LaunchTaskStartedEvent event) {
        this.mAnimationHelper.startLaunchTaskAnimation(event.taskView, event.screenPinningRequested, event.getAnimationTrigger());
    }

    public final void onBusEvent(DismissRecentsToHomeAnimationStarted event) {
        this.mTouchHandler.cancelNonDismissTaskAnimations();
        this.mStackScroller.stopScroller();
        this.mStackScroller.stopBoundScrollAnimation();
        cancelDeferredTaskViewLayoutAnimation();
        this.mAnimationHelper.startExitToHomeAnimation(event.animated, event.getAnimationTrigger());
        animateFreeformWorkspaceBackgroundAlpha(0, new AnimationProps(350, Interpolators.FAST_OUT_SLOW_IN));
    }

    public final void onBusEvent(DismissFocusedTaskViewEvent event) {
        if (this.mFocusedTask != null) {
            TaskView tv = getChildViewForTask(this.mFocusedTask);
            if (tv != null) {
                tv.dismissTask();
            }
            resetFocusedTask(this.mFocusedTask);
        }
    }

    public final void onBusEvent(DismissTaskViewEvent event) {
        this.mAnimationHelper.startDeleteTaskAnimation(event.taskView, event.getAnimationTrigger());
    }

    public final void onBusEvent(DismissAllTaskViewsEvent event) {
        this.mStackScroller.stopScroller();
        ArrayList<Task> tasks = new ArrayList<>(this.mStack.getStackTasks());
        this.mAnimationHelper.startDeleteAllTasksAnimation(getTaskViews(), event.getAnimationTrigger());
        for (int i = tasks.size() - 1; i >= 0; i--) {
            Task task = tasks.get(i);
            if (!task.isProtected()) {
                RecentsEventBus.getDefault().send(new DeleteTaskDataEvent(task, true));
            }
        }
        Slog.d("TaskStackView", "removeAllTask, cleanByRecents=true");
        event.addPostAnimationCallback(new Runnable() {
            public void run() {
                TaskStackView.this.announceForAccessibility(TaskStackView.this.getContext().getString(R.string.accessibility_recents_all_items_dismissed));
                TaskStackView.this.mStack.removeAllTasks();
                MetricsLogger.action(TaskStackView.this.getContext(), 357);
            }
        });
    }

    public final void onBusEvent(TaskViewDismissedEvent event) {
        announceForAccessibility(getContext().getString(R.string.accessibility_recents_item_dismissed, new Object[]{event.task.title}));
        this.mStack.removeTask(event.task, event.animation, false);
        RecentsEventBus.getDefault().send(new DeleteTaskDataEvent(event.task));
        MetricsLogger.action(getContext(), 289, event.task.key.getComponent().toString());
    }

    public final void onBusEvent(FocusNextTaskViewEvent event) {
        this.mStackScroller.stopScroller();
        this.mStackScroller.stopBoundScrollAnimation();
        setRelativeFocusedTask(true, false, true, false, event.timerIndicatorDuration);
    }

    public final void onBusEvent(FocusPreviousTaskViewEvent event) {
        this.mStackScroller.stopScroller();
        this.mStackScroller.stopBoundScrollAnimation();
        setRelativeFocusedTask(false, false, true);
    }

    public final void onBusEvent(UserInteractionEvent event) {
        this.mUIDozeTrigger.poke();
        if (Recents.getDebugFlags().isFastToggleRecentsEnabled() && this.mFocusedTask != null) {
            TaskView tv = getChildViewForTask(this.mFocusedTask);
            if (tv != null) {
                tv.getHeaderView().cancelFocusTimerIndicator();
            }
        }
    }

    public final void onBusEvent(DragStartEvent event) {
        addIgnoreTask(event.task);
        if (event.task.isFreeformTask()) {
            this.mStackScroller.animateScroll(this.mLayoutAlgorithm.mInitialScrollP, null);
        }
        this.mLayoutAlgorithm.getStackTransform(event.task, getScroller().getStackScroll(), this.mTmpTransform, null);
        this.mTmpTransform.scale = 1.06f;
        this.mTmpTransform.translationZ = (float) (this.mLayoutAlgorithm.mMaxTranslationZ + 1);
        this.mTmpTransform.dimAlpha = 0.0f;
        event.taskView.animate().cancel();
        updateTaskViewToTransform(event.taskView, this.mTmpTransform, new AnimationProps(175, Interpolators.FAST_OUT_SLOW_IN));
    }

    public final void onBusEvent(DragStartInitializeDropTargetsEvent event) {
        if (Recents.getSystemServices().hasFreeformWorkspaceSupport()) {
            event.handler.registerDropTargetForCurrentDrag(this.mStackDropTarget);
            event.handler.registerDropTargetForCurrentDrag(this.mFreeformWorkspaceDropTarget);
        }
    }

    public final void onBusEvent(DragDropTargetChangedEvent event) {
        DragDropTargetChangedEvent dragDropTargetChangedEvent = event;
        AnimationProps animation = new AnimationProps(250, Interpolators.FAST_OUT_SLOW_IN);
        boolean ignoreTaskOverrides = false;
        if (dragDropTargetChangedEvent.dropTarget instanceof TaskStack.DockState) {
            Rect systemInsets = new Rect(this.mStableLayoutAlgorithm.mSystemInsets);
            int height = getMeasuredHeight() - systemInsets.bottom;
            systemInsets.set(systemInsets.left, systemInsets.top, systemInsets.right, 0);
            this.mStackBounds.set(((TaskStack.DockState) dragDropTargetChangedEvent.dropTarget).getDockedTaskStackBounds(this.mDisplayRect, getMeasuredWidth(), height, this.mDividerSize, systemInsets, this.mLayoutAlgorithm, getResources(), this.mWindowRect));
            this.mLayoutAlgorithm.mDropToDockState = true;
            this.mLayoutAlgorithm.setSystemInsets(systemInsets);
            this.mLayoutAlgorithm.initialize(this.mDisplayRect, this.mWindowRect, this.mStackBounds, TaskStackLayoutAlgorithm.StackState.getStackStateForStack(this.mStack));
            updateLayoutAlgorithm(true);
            ignoreTaskOverrides = true;
        } else {
            removeIgnoreTask(dragDropTargetChangedEvent.task);
            updateLayoutToStableBounds();
            addIgnoreTask(dragDropTargetChangedEvent.task);
        }
        relayoutTaskViews(animation, null, ignoreTaskOverrides);
        this.mLayoutAlgorithm.mDropToDockState = false;
    }

    public final void onBusEvent(final DragEndEvent event) {
        if (event.dropTarget instanceof TaskStack.DockState) {
            this.mLayoutAlgorithm.clearUnfocusedTaskOverrides();
            return;
        }
        boolean isFreeformTask = event.task.isFreeformTask();
        if ((!isFreeformTask && event.dropTarget == this.mFreeformWorkspaceDropTarget) || (isFreeformTask && event.dropTarget == this.mStackDropTarget)) {
            if (event.dropTarget == this.mFreeformWorkspaceDropTarget) {
                this.mStack.moveTaskToStack(event.task, 2);
            } else if (event.dropTarget == this.mStackDropTarget) {
                this.mStack.moveTaskToStack(event.task, 1);
            }
            updateLayoutAlgorithm(true);
            event.addPostAnimationCallback(new Runnable() {
                public void run() {
                    Recents.getSystemServices().moveTaskToStack(event.task.key.id, event.task.key.stackId);
                }
            });
        }
        removeIgnoreTask(event.task);
        Utilities.setViewFrameFromTranslation(event.taskView);
        new ArrayMap<>().put(event.task, new AnimationProps(250, Interpolators.FAST_OUT_SLOW_IN, event.getAnimationTrigger().decrementOnAnimationEnd()));
        relayoutTaskViews(new AnimationProps(250, Interpolators.FAST_OUT_SLOW_IN));
        event.getAnimationTrigger().increment();
    }

    public final void onBusEvent(DragEndCancelledEvent event) {
        removeIgnoreTask(event.task);
        updateLayoutToStableBounds();
        Utilities.setViewFrameFromTranslation(event.taskView);
        new ArrayMap<>().put(event.task, new AnimationProps(250, Interpolators.FAST_OUT_SLOW_IN, event.getAnimationTrigger().decrementOnAnimationEnd()));
        relayoutTaskViews(new AnimationProps(250, Interpolators.FAST_OUT_SLOW_IN));
        event.getAnimationTrigger().increment();
    }

    public final void onBusEvent(IterateRecentsEvent event) {
        if (!this.mEnterAnimationComplete) {
            RecentsEventBus.getDefault().send(new CancelEnterRecentsWindowAnimationEvent(null));
        }
    }

    public final void onBusEvent(EnterRecentsWindowAnimationCompletedEvent event) {
        this.mEnterAnimationComplete = true;
        tryStartEnterAnimation();
    }

    private void tryStartEnterAnimation() {
        if (this.mStackReloaded && !this.mAwaitingFirstLayout && (this.mEnterAnimationComplete || sIsChangingConfigurations)) {
            if (this.mStack.getTaskCount() > 0) {
                ReferenceCountedTrigger trigger = new ReferenceCountedTrigger();
                this.mAnimationHelper.startEnterAnimation(trigger);
                trigger.addLastDecrementRunnable(new Runnable() {
                    public void run() {
                        TaskStackView.this.mUIDozeTrigger.startDozing();
                        if (TaskStackView.this.mFocusedTask != null) {
                            boolean unused = TaskStackView.this.setFocusedTask(TaskStackView.this.mStack.indexOfStackTask(TaskStackView.this.mFocusedTask), false, Recents.getConfiguration().getLaunchState().launchedWithAltTab);
                            TaskView focusedTaskView = TaskStackView.this.getChildViewForTask(TaskStackView.this.mFocusedTask);
                            if (TaskStackView.this.mTouchExplorationEnabled && focusedTaskView != null) {
                                focusedTaskView.requestAccessibilityFocus();
                            }
                        }
                        RecentsEventBus.getDefault().send(new EnterRecentsTaskStackAnimationCompletedEvent());
                    }
                });
            }
            this.mStackReloaded = false;
        }
    }

    public final void onBusEvent(UpdateFreeformTaskViewVisibilityEvent event) {
        List<TaskView> taskViews = getTaskViews();
        int taskViewCount = taskViews.size();
        for (int i = 0; i < taskViewCount; i++) {
            TaskView tv = taskViews.get(i);
            if (tv.getTask().isFreeformTask()) {
                tv.setVisibility(event.visible ? 0 : 4);
            }
        }
    }

    public final void onBusEvent(final MultiWindowStateChangedEvent event) {
        this.mIsMultiStateChanging = false;
        if (event.inMultiWindow || !event.showDeferredAnimation) {
            setTasks(event.stack, true);
            return;
        }
        Recents.getConfiguration().getLaunchState().reset();
        event.getAnimationTrigger().increment();
        post(new Runnable() {
            public void run() {
                TaskStackView.this.mAnimationHelper.startNewStackScrollAnimation(event.stack, event.getAnimationTrigger());
                event.getAnimationTrigger().decrement();
            }
        });
    }

    public final void onBusEvent(ConfigurationChangedEvent event) {
        if (event.fromDeviceOrientationChange) {
            this.mDisplayOrientation = Utilities.getAppConfiguration(this.mContext).orientation;
            this.mDisplayRect = Recents.getSystemServices().getDisplayRect();
            this.mStackScroller.stopScroller();
        }
        reloadOnConfigurationChange();
        if (!event.fromMultiWindow) {
            this.mTmpTaskViews.clear();
            this.mTmpTaskViews.addAll(getTaskViews());
            this.mTmpTaskViews.addAll(this.mViewPool.getViews());
            int taskViewCount = this.mTmpTaskViews.size();
            for (int i = 0; i < taskViewCount; i++) {
                this.mTmpTaskViews.get(i).onConfigurationChanged();
            }
        }
        if (event.fromMultiWindow != 0) {
            this.mInitialState = 2;
            requestLayout();
        } else if (event.fromDeviceOrientationChange) {
            this.mInitialState = 1;
            requestLayout();
        }
    }

    public final void onBusEvent(RecentsGrowingEvent event) {
        this.mResetToInitialStateWhenResized = true;
    }

    public final void onBusEvent(RecentsVisibilityChangedEvent event) {
        if (event.visible) {
            updateLayoutToStableBounds();
            return;
        }
        List<TaskView> taskViews = new ArrayList<>(getTaskViews());
        for (int i = 0; i < taskViews.size(); i++) {
            this.mViewPool.returnViewToPool(taskViews.get(i));
        }
    }

    public final void onBusEvent(ClickTaskViewToLaunchTaskEvent event) {
        RecentsPushEventHelper.sendSwitchAppEvent("clickToSwitch", Integer.toString(this.mStack.indexOfStackTask(event.task)));
        this.mTouchHandler.setAllowHideRecentsFromBackgroundTap(false);
    }

    public final void onBusEvent(AnimFirstTaskViewAlphaEvent event) {
        TaskView frontTv = getFrontMostTaskView(true);
        if (frontTv != null && frontTv.getHeaderView() != null && frontTv.getThumbnailView() != null) {
            if (event.mWithAnim) {
                frontTv.getHeaderView().animate().alpha(event.mAlpha).start();
                frontTv.getThumbnailView().animate().alpha(event.mAlpha).start();
            } else {
                frontTv.getHeaderView().animate().cancel();
                frontTv.getThumbnailView().animate().cancel();
                frontTv.getHeaderView().setAlpha(event.mAlpha);
                frontTv.getThumbnailView().setAlpha(event.mAlpha);
            }
            this.mKeepAlphaWhenRelayout = event.mKeepAlphaWhenRelayout;
        }
    }

    public final void onBusEvent(RotationChangedEvent event) {
        this.mLayoutAlgorithm.updatePaddingOfNotch(event.rotation);
        this.mStableLayoutAlgorithm.updatePaddingOfNotch(event.rotation);
    }

    public final void onBusEvent(RecentsActivityStartingEvent event) {
        this.mTouchHandler.setAllowHideRecentsFromBackgroundTap(true);
    }

    public void reloadOnConfigurationChange() {
        this.mStableLayoutAlgorithm.reloadOnConfigurationChange(getContext());
        this.mLayoutAlgorithm.reloadOnConfigurationChange(getContext());
    }

    private void animateFreeformWorkspaceBackgroundAlpha(int targetAlpha, AnimationProps animation) {
        if (this.mFreeformWorkspaceBackground.getAlpha() != targetAlpha) {
            Utilities.cancelAnimationWithoutCallbacks(this.mFreeformWorkspaceBackgroundAnimator);
            this.mFreeformWorkspaceBackgroundAnimator = ObjectAnimator.ofInt(this.mFreeformWorkspaceBackground, Utilities.DRAWABLE_ALPHA, new int[]{this.mFreeformWorkspaceBackground.getAlpha(), targetAlpha});
            this.mFreeformWorkspaceBackgroundAnimator.setStartDelay(animation.getDuration(4));
            this.mFreeformWorkspaceBackgroundAnimator.setDuration(animation.getDuration(4));
            this.mFreeformWorkspaceBackgroundAnimator.setInterpolator(animation.getInterpolator(4));
            this.mFreeformWorkspaceBackgroundAnimator.start();
        }
    }

    private int findTaskViewInsertIndex(Task task, int taskIndex) {
        if (taskIndex != -1) {
            List<TaskView> taskViews = getTaskViews();
            boolean foundTaskView = false;
            int taskViewCount = taskViews.size();
            for (int i = 0; i < taskViewCount; i++) {
                Task tvTask = taskViews.get(i).getTask();
                if (tvTask == task) {
                    foundTaskView = true;
                } else if (taskIndex < this.mStack.indexOfStackTask(tvTask)) {
                    if (foundTaskView) {
                        return i - 1;
                    }
                    return i;
                }
            }
        }
        return -1;
    }

    private void readSystemFlags() {
        SystemServicesProxy ssp = Recents.getSystemServices();
        this.mTouchExplorationEnabled = ssp.isTouchExplorationEnabled();
        this.mScreenPinningEnabled = ssp.getSystemSetting(getContext(), "lock_to_app_enabled") != 0;
    }

    public void dump(String prefix, PrintWriter writer) {
        String innerPrefix = prefix + "  ";
        String id = Integer.toHexString(System.identityHashCode(this));
        writer.print(prefix);
        writer.print("TaskStackView");
        writer.print(" hasDefRelayout=");
        writer.print(this.mDeferredTaskViewLayoutAnimation != null ? "Y" : "N");
        writer.print(" clipDirty=");
        writer.print(this.mTaskViewsClipDirty ? "Y" : "N");
        writer.print(" awaitingFirstLayout=");
        writer.print(this.mAwaitingFirstLayout ? "Y" : "N");
        writer.print(" initialState=");
        writer.print(this.mInitialState);
        writer.print(" inMeasureLayout=");
        writer.print(this.mInMeasureLayout ? "Y" : "N");
        writer.print(" enterAnimCompleted=");
        writer.print(this.mEnterAnimationComplete ? "Y" : "N");
        writer.print(" touchExplorationOn=");
        writer.print(this.mTouchExplorationEnabled ? "Y" : "N");
        writer.print(" screenPinningOn=");
        writer.print(this.mScreenPinningEnabled ? "Y" : "N");
        writer.print(" numIgnoreTasks=");
        writer.print(this.mIgnoreTasks.size());
        writer.print(" numViewPool=");
        writer.print(this.mViewPool.getViews().size());
        writer.print(" stableStackBounds=");
        writer.print(Utilities.dumpRect(this.mStableStackBounds));
        writer.print(" stackBounds=");
        writer.print(Utilities.dumpRect(this.mStackBounds));
        writer.print(" stableWindow=");
        writer.print(Utilities.dumpRect(this.mStableWindowRect));
        writer.print(" window=");
        writer.print(Utilities.dumpRect(this.mWindowRect));
        writer.print(" display=");
        writer.print(Utilities.dumpRect(this.mDisplayRect));
        writer.print(" orientation=");
        writer.print(this.mDisplayOrientation);
        writer.print(" [0x");
        writer.print(id);
        writer.print("]");
        writer.println();
        if (this.mFocusedTask != null) {
            writer.print(innerPrefix);
            writer.print("Focused task: ");
            this.mFocusedTask.dump("", writer);
        }
        this.mLayoutAlgorithm.dump(innerPrefix, writer);
        this.mStackScroller.dump(innerPrefix, writer);
    }

    public void setIsShowingMenu(boolean isShowing) {
        this.mIsShowingMenu = isShowing;
    }

    public boolean isShowingMenu() {
        return this.mIsShowingMenu;
    }

    public FrameLayout getMask() {
        return this.mMaskWithMenu;
    }
}
