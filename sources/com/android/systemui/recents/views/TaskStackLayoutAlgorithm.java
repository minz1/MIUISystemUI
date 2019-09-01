package com.android.systemui.recents.views;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.ArraySet;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.ViewDebug;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import com.android.systemui.Application;
import com.android.systemui.Constants;
import com.android.systemui.R;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.RecentsActivityLaunchState;
import com.android.systemui.recents.RecentsConfiguration;
import com.android.systemui.recents.RecentsImpl;
import com.android.systemui.recents.misc.FreePathInterpolator;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.model.TaskStack;
import com.android.systemui.stackdivider.Divider;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class TaskStackLayoutAlgorithm {
    private AccelerateInterpolator mAccelerateInterpolator = new AccelerateInterpolator();
    TaskViewTransform mBackOfStackTransform = new TaskViewTransform();
    @ViewDebug.ExportedProperty(category = "recents")
    private int mBaseBottomMargin;
    private int mBaseInitialBottomOffset;
    private int mBaseInitialTopOffset;
    @ViewDebug.ExportedProperty(category = "recents")
    private int mBaseSideMargin;
    @ViewDebug.ExportedProperty(category = "recents")
    private int mBaseTopMargin;
    private TaskStackLayoutAlgorithmCallbacks mCb;
    Context mContext;
    private Display mDisplay;
    public boolean mDropToDockState = false;
    @ViewDebug.ExportedProperty(category = "recents")
    public Rect mFirstTaskRect = new Rect();
    @ViewDebug.ExportedProperty(category = "recents")
    private int mFocusState;
    @ViewDebug.ExportedProperty(category = "recents")
    private int mFocusedBottomPeekHeight;
    private Path mFocusedCurve = linearCurve();
    private FreePathInterpolator mFocusedCurveInterpolator = new FreePathInterpolator(this.mFocusedCurve);
    private Path mFocusedDimCurve = linearCurve();
    private FreePathInterpolator mFocusedDimCurveInterpolator = new FreePathInterpolator(this.mFocusedDimCurve);
    private Range mFocusedRange;
    @ViewDebug.ExportedProperty(category = "recents")
    private int mFocusedTopPeekHeight;
    FreeformWorkspaceLayoutAlgorithm mFreeformLayoutAlgorithm;
    @ViewDebug.ExportedProperty(category = "recents")
    public Rect mFreeformRect = new Rect();
    @ViewDebug.ExportedProperty(category = "recents")
    private int mFreeformStackGap;
    @ViewDebug.ExportedProperty(category = "recents")
    float mFrontMostTaskP;
    TaskViewTransform mFrontOfStackTransform = new TaskViewTransform();
    int mHorizontalGap;
    @ViewDebug.ExportedProperty(category = "recents")
    private int mInitialBottomOffset;
    @ViewDebug.ExportedProperty(category = "recents")
    float mInitialScrollP;
    @ViewDebug.ExportedProperty(category = "recents")
    private int mInitialTopOffset;
    private boolean mIsRtlLayout;
    @ViewDebug.ExportedProperty(category = "recents")
    float mMaxScrollP;
    @ViewDebug.ExportedProperty(category = "recents")
    int mMaxTranslationZ;
    private int mMinMargin;
    @ViewDebug.ExportedProperty(category = "recents")
    float mMinScrollP;
    @ViewDebug.ExportedProperty(category = "recents")
    int mMinTranslationZ;
    @ViewDebug.ExportedProperty(category = "recents")
    int mNumFreeformTasks;
    @ViewDebug.ExportedProperty(category = "recents")
    int mNumStackTasks;
    int mPaddingBottom;
    int mPaddingLeft = 0;
    int mPaddingRight = 0;
    int mPaddingTop;
    @ViewDebug.ExportedProperty(category = "recents")
    public Rect mStackActionButtonRect = new Rect();
    @ViewDebug.ExportedProperty(category = "recents")
    private int mStackBottomOffset;
    @ViewDebug.ExportedProperty(category = "recents")
    public Rect mStackRect = new Rect();
    private StackState mState = StackState.SPLIT;
    private int mStatusbarHeight;
    @ViewDebug.ExportedProperty(category = "recents")
    public Rect mSystemInsets = new Rect();
    private SparseIntArray mTaskIndexMap = new SparseIntArray();
    private SparseArray<Float> mTaskIndexOverrideMap = new SparseArray<>();
    @ViewDebug.ExportedProperty(category = "recents")
    public Rect mTaskRect = new Rect();
    private int mTaskViewTop;
    private Path mUnfocusedCurve = linearCurve();
    private FreePathInterpolator mUnfocusedCurveInterpolator = new FreePathInterpolator(this.mUnfocusedCurve);
    private Path mUnfocusedDimCurve = linearCurve();
    private FreePathInterpolator mUnfocusedDimCurveInterpolator = new FreePathInterpolator(this.mUnfocusedDimCurve);
    private Range mUnfocusedRange;
    int mVerticalGap;
    @ViewDebug.ExportedProperty(category = "recents")
    public Rect mWindowRect = new Rect();

    public static class StackState {
        public static final StackState FREEFORM_ONLY = new StackState(1.0f, 255);
        public static final StackState SPLIT = new StackState(0.5f, 255);
        public static final StackState STACK_ONLY = new StackState(0.0f, 0);
        public final int freeformBackgroundAlpha;
        public final float freeformHeightPct;

        private StackState(float freeformHeightPct2, int freeformBackgroundAlpha2) {
            this.freeformHeightPct = freeformHeightPct2;
            this.freeformBackgroundAlpha = freeformBackgroundAlpha2;
        }

        public static StackState getStackStateForStack(TaskStack stack) {
            boolean hasFreeformWorkspaces = Recents.getSystemServices().hasFreeformWorkspaceSupport();
            int freeformCount = stack.getFreeformTaskCount();
            int stackCount = stack.getStackTaskCount();
            if (hasFreeformWorkspaces && stackCount > 0 && freeformCount > 0) {
                return SPLIT;
            }
            if (!hasFreeformWorkspaces || freeformCount <= 0) {
                return STACK_ONLY;
            }
            return FREEFORM_ONLY;
        }

        public void computeRects(Rect freeformRectOut, Rect stackRectOut, Rect taskStackBounds, int topMargin, int freeformGap, int stackBottomOffset) {
            int ffPaddedHeight = (int) (((float) ((taskStackBounds.height() - topMargin) - stackBottomOffset)) * this.freeformHeightPct);
            freeformRectOut.set(taskStackBounds.left, taskStackBounds.top + topMargin, taskStackBounds.right, taskStackBounds.top + topMargin + Math.max(0, ffPaddedHeight - freeformGap));
            stackRectOut.set(taskStackBounds.left, taskStackBounds.top, taskStackBounds.right, taskStackBounds.bottom);
            if (ffPaddedHeight > 0) {
                stackRectOut.top += ffPaddedHeight;
            } else {
                stackRectOut.top += topMargin;
            }
        }
    }

    public interface TaskStackLayoutAlgorithmCallbacks {
        void onFocusStateChanged(int i, int i2);
    }

    public class VisibilityReport {
        public int numVisibleTasks;
        public int numVisibleThumbnails;

        VisibilityReport(int tasks, int thumbnails) {
            this.numVisibleTasks = tasks;
            this.numVisibleThumbnails = thumbnails;
        }
    }

    public TaskStackLayoutAlgorithm(Context context, TaskStackLayoutAlgorithmCallbacks cb) {
        Resources res = context.getResources();
        this.mContext = context;
        this.mCb = cb;
        this.mFreeformLayoutAlgorithm = new FreeformWorkspaceLayoutAlgorithm(context);
        this.mMinMargin = res.getDimensionPixelSize(R.dimen.recents_layout_min_margin);
        this.mBaseTopMargin = getDimensionForDevice(context, R.dimen.recents_layout_top_margin_phone, R.dimen.recents_layout_top_margin_tablet, R.dimen.recents_layout_top_margin_tablet_xlarge);
        this.mBaseSideMargin = getDimensionForDevice(context, R.dimen.recents_layout_side_margin_phone, R.dimen.recents_layout_side_margin_tablet, R.dimen.recents_layout_side_margin_tablet_xlarge);
        this.mBaseBottomMargin = res.getDimensionPixelSize(R.dimen.recents_layout_bottom_margin);
        this.mFreeformStackGap = res.getDimensionPixelSize(R.dimen.recents_freeform_layout_bottom_margin);
        this.mDisplay = ((WindowManager) this.mContext.getSystemService("window")).getDefaultDisplay();
        reloadOnConfigurationChange(context);
    }

    public void reloadOnConfigurationChange(Context context) {
        Resources res = context.getResources();
        this.mFocusedRange = new Range(res.getFloat(R.integer.recents_layout_focused_range_min), res.getFloat(R.integer.recents_layout_focused_range_max));
        this.mUnfocusedRange = new Range(res.getFloat(R.integer.recents_layout_unfocused_range_min), res.getFloat(R.integer.recents_layout_unfocused_range_max));
        this.mFocusState = getInitialFocusState();
        this.mFocusedTopPeekHeight = res.getDimensionPixelSize(R.dimen.recents_layout_top_peek_size);
        this.mFocusedBottomPeekHeight = res.getDimensionPixelSize(R.dimen.recents_layout_bottom_peek_size);
        this.mMinTranslationZ = res.getDimensionPixelSize(R.dimen.recents_layout_z_min);
        this.mMaxTranslationZ = res.getDimensionPixelSize(R.dimen.recents_layout_z_max);
        Context context2 = context;
        this.mBaseInitialTopOffset = getDimensionForDevice(context2, R.dimen.recents_layout_initial_top_offset_phone_port, R.dimen.recents_layout_initial_top_offset_phone_land, R.dimen.recents_layout_initial_top_offset_tablet, R.dimen.recents_layout_initial_top_offset_tablet, R.dimen.recents_layout_initial_top_offset_tablet, R.dimen.recents_layout_initial_top_offset_tablet);
        this.mBaseInitialBottomOffset = getDimensionForDevice(context2, R.dimen.recents_layout_initial_bottom_offset_phone_port, R.dimen.recents_layout_initial_bottom_offset_phone_land, R.dimen.recents_layout_initial_bottom_offset_tablet, R.dimen.recents_layout_initial_bottom_offset_tablet, R.dimen.recents_layout_initial_bottom_offset_tablet, R.dimen.recents_layout_initial_bottom_offset_tablet);
        this.mTaskViewTop = getDimensionForDevice(context2, R.dimen.recents_task_view_top_port, R.dimen.recents_task_view_top_land, R.dimen.recents_task_view_top_tablet_port, R.dimen.recents_task_view_top_tablet_land, R.dimen.recents_task_view_top_tablet_port, R.dimen.recents_task_view_top_tablet_land);
        this.mFreeformLayoutAlgorithm.reloadOnConfigurationChange(context);
        boolean z = true;
        if (res.getConfiguration().getLayoutDirection() != 1) {
            z = false;
        }
        this.mIsRtlLayout = z;
        this.mStatusbarHeight = this.mContext.getResources().getDimensionPixelSize(R.dimen.status_bar_height);
    }

    public void reset() {
        this.mTaskIndexOverrideMap.clear();
        setFocusState(getInitialFocusState());
    }

    public boolean setSystemInsets(Rect systemInsets) {
        boolean changed = !this.mSystemInsets.equals(systemInsets);
        if (changed) {
            this.mSystemInsets.set(systemInsets);
        }
        return changed;
    }

    public void setFocusState(int focusState) {
        int prevFocusState = this.mFocusState;
        updateFrontBackTransforms();
        if (this.mCb != null) {
            this.mCb.onFocusStateChanged(prevFocusState, focusState);
        }
    }

    public int getFocusState() {
        return this.mFocusState;
    }

    public void initialize(Rect displayRect, Rect windowRect, Rect taskStackBounds, StackState state) {
        Rect rect = windowRect;
        this.mWindowRect = rect;
        Rect lastStackRect = new Rect(this.mStackRect);
        Rect rect2 = rect;
        Rect rect3 = displayRect;
        int topMargin = getScaleForExtent(rect2, rect3, this.mBaseTopMargin, this.mMinMargin, 1);
        int bottomMargin = getScaleForExtent(rect2, rect3, this.mBaseBottomMargin, this.mMinMargin, 1);
        this.mInitialTopOffset = 0;
        this.mInitialBottomOffset = 0;
        StackState stackState = state;
        this.mState = stackState;
        this.mStackBottomOffset = this.mSystemInsets.bottom + bottomMargin;
        stackState.computeRects(this.mFreeformRect, this.mStackRect, taskStackBounds, topMargin, this.mFreeformStackGap, this.mStackBottomOffset);
        this.mStackActionButtonRect.set(this.mStackRect.left, this.mStackRect.top - topMargin, this.mStackRect.right, this.mStackRect.top + this.mFocusedTopPeekHeight);
        computeTaskRect(taskStackBounds, rect, displayRect);
        if (!lastStackRect.equals(this.mStackRect)) {
            updateFrontBackTransforms();
        }
    }

    private void computeTaskRect(Rect taskStackBounds, Rect windowRect, Rect displayRect) {
        float taskRectScale = this.mContext.getResources().getFloat(R.dimen.recents_task_rect_scale);
        RectF taskRectF = new RectF();
        taskRectF.set(taskStackBounds);
        taskRectF.bottom -= (float) this.mSystemInsets.bottom;
        Utilities.scaleRectAboutCenter(taskRectF, taskRectScale);
        Utilities.scaleRectAboutCenter(taskRectF, 1.0f - ((((float) this.mContext.getResources().getDimensionPixelSize(R.dimen.recents_task_view_padding)) * 1.0f) / taskRectF.width()));
        taskRectF.top -= (float) RecentsImpl.mTaskBarHeight;
        taskRectF.offsetTo(taskRectF.left, (float) (taskStackBounds.top + getScaleForExtent(taskStackBounds, displayRect, this.mTaskViewTop, 0, 1)));
        taskRectF.round(this.mTaskRect);
        this.mFirstTaskRect.set(this.mTaskRect);
        if (isDockedMode()) {
            this.mHorizontalGap = ((this.mStackRect.width() - (2 * this.mTaskRect.width())) - this.mPaddingRight) / 3;
        } else {
            this.mHorizontalGap = (((this.mStackRect.width() - (2 * this.mTaskRect.width())) - this.mPaddingLeft) - this.mPaddingRight) / 3;
        }
        this.mVerticalGap = RecentsImpl.mTaskBarHeight;
        if (isLandscapeMode(this.mContext) || isDockedMode()) {
            this.mVerticalGap = (int) (0.8d * ((double) this.mVerticalGap));
            return;
        }
        this.mFirstTaskRect.bottom = this.mFirstTaskRect.top + ((int) (((float) RecentsImpl.mTaskBarHeight) + (((float) (this.mTaskRect.height() - RecentsImpl.mTaskBarHeight)) * 0.73f)));
    }

    private void computeTaskViewPadding(int taskViewCount) {
        float bottomRatio;
        float topRatio;
        if (taskViewCount > 0) {
            if (isDockedMode()) {
                topRatio = 0.2f;
                bottomRatio = 0.15f;
            } else if (isLandscapeMode(this.mContext)) {
                topRatio = 0.32f;
                bottomRatio = 0.5f;
            } else {
                topRatio = taskViewCount <= 2 ? 0.62f : 0.55f;
                bottomRatio = 0.4f;
            }
            this.mPaddingTop = (int) (((float) this.mTaskRect.height()) * topRatio);
            this.mPaddingBottom = taskViewCount <= 2 ? 0 : (int) (((float) this.mTaskRect.height()) * bottomRatio);
        }
    }

    /* access modifiers changed from: package-private */
    public void update(TaskStack stack, ArraySet<Task.TaskKey> ignoreTasksSet) {
        SystemServicesProxy systemServices = Recents.getSystemServices();
        RecentsActivityLaunchState launchState = Recents.getConfiguration().getLaunchState();
        this.mTaskIndexMap.clear();
        ArrayList<Task> tasks = stack.getStackTasks();
        if (tasks.isEmpty()) {
            this.mFrontMostTaskP = 0.0f;
            this.mInitialScrollP = 0.0f;
            this.mMaxScrollP = 0.0f;
            this.mMinScrollP = 0.0f;
            this.mNumFreeformTasks = 0;
            this.mNumStackTasks = 0;
            return;
        }
        ArrayList<Task> freeformTasks = new ArrayList<>();
        ArrayList<Task> stackTasks = new ArrayList<>();
        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);
            if (!ignoreTasksSet.contains(task.key)) {
                if (task.isFreeformTask()) {
                    freeformTasks.add(task);
                } else {
                    stackTasks.add(task);
                }
            }
        }
        this.mNumStackTasks = stackTasks.size();
        this.mNumFreeformTasks = freeformTasks.size();
        int taskCount = stackTasks.size();
        for (int i2 = 0; i2 < taskCount; i2++) {
            this.mTaskIndexMap.put(stackTasks.get(i2).key.id, i2);
        }
        if (freeformTasks.isEmpty() == 0) {
            this.mFreeformLayoutAlgorithm.update(freeformTasks, this);
        }
        computeTaskViewPadding(taskCount);
        this.mMinScrollP = 0.0f;
        this.mInitialScrollP = 0.0f;
        if (this.mNumStackTasks > 0) {
            this.mMaxScrollP = Math.max(0.0f, ((((((float) calculateTaskViewXandY(this.mNumStackTasks - 1, this.mTaskRect)[1]) + ((float) this.mTaskRect.top)) + ((float) this.mTaskRect.height())) - ((float) this.mWindowRect.bottom)) + ((float) this.mPaddingBottom)) / ((float) this.mTaskRect.height()));
        } else {
            this.mMaxScrollP = 0.0f;
        }
    }

    public void setTaskOverridesForInitialState(TaskStack stack, boolean ignoreScrollToFront) {
        RecentsActivityLaunchState launchState = Recents.getConfiguration().getLaunchState();
        this.mTaskIndexOverrideMap.clear();
        boolean scrollToFront = launchState.launchedFromHome || launchState.launchedViaDockGesture;
        if (getInitialFocusState() == 0 && this.mNumStackTasks > 1) {
            if (ignoreScrollToFront || (!launchState.launchedWithAltTab && !scrollToFront)) {
                float minBottomTaskNormX = getNormalizedXFromUnfocusedY((float) (this.mSystemInsets.right + this.mInitialBottomOffset), 1);
                float[] initialNormX = this.mNumStackTasks <= 2 ? new float[]{Math.min(getNormalizedXFromUnfocusedY((float) ((this.mFocusedTopPeekHeight + this.mTaskRect.width()) - this.mMinMargin), 0), minBottomTaskNormX), getNormalizedXFromUnfocusedY((float) this.mFocusedTopPeekHeight, 0)} : new float[]{minBottomTaskNormX, getNormalizedXFromUnfocusedY((float) this.mInitialTopOffset, 0)};
                this.mUnfocusedRange.offset(0.0f);
                List<Task> tasks = stack.getStackTasks();
                int taskCount = tasks.size();
                int i = taskCount - 1;
                while (i >= 0) {
                    int indexFromFront = (taskCount - i) - 1;
                    if (indexFromFront < initialNormX.length) {
                        this.mTaskIndexOverrideMap.put(tasks.get(i).key.id, Float.valueOf(this.mInitialScrollP + this.mUnfocusedRange.getAbsoluteX(initialNormX[indexFromFront])));
                        i--;
                    } else {
                        return;
                    }
                }
            }
        }
    }

    public void addUnfocusedTaskOverride(Task task, float stackScroll) {
        if (this.mFocusState != 0) {
            this.mFocusedRange.offset(stackScroll);
            this.mUnfocusedRange.offset(stackScroll);
            float focusedRangeX = this.mFocusedRange.getNormalizedX((float) this.mTaskIndexMap.get(task.key.id));
            float unfocusedRangeX = this.mUnfocusedCurveInterpolator.getX(this.mFocusedCurveInterpolator.getInterpolation(focusedRangeX));
            float unfocusedTaskProgress = this.mUnfocusedRange.getAbsoluteX(unfocusedRangeX) + stackScroll;
            if (Float.compare(focusedRangeX, unfocusedRangeX) != 0) {
                this.mTaskIndexOverrideMap.put(task.key.id, Float.valueOf(unfocusedTaskProgress));
            }
        }
    }

    public void addUnfocusedTaskOverride(TaskView taskView, float stackScroll) {
        this.mFocusedRange.offset(stackScroll);
        this.mUnfocusedRange.offset(stackScroll);
        Task task = taskView.getTask();
        int top = taskView.getLeft() - this.mTaskRect.left;
        float focusedRangeX = getNormalizedXFromFocusedY((float) top, 0);
        float unfocusedRangeX = getNormalizedXFromUnfocusedY((float) top, 0);
        float unfocusedTaskProgress = this.mUnfocusedRange.getAbsoluteX(unfocusedRangeX) + stackScroll;
        if (Float.compare(focusedRangeX, unfocusedRangeX) != 0) {
            this.mTaskIndexOverrideMap.put(task.key.id, Float.valueOf(unfocusedTaskProgress));
        }
    }

    public void clearUnfocusedTaskOverrides() {
        this.mTaskIndexOverrideMap.clear();
    }

    public float updateFocusStateOnScroll(float lastTargetStackScroll, float targetStackScroll, float lastStackScroll) {
        if (targetStackScroll == lastStackScroll) {
            return targetStackScroll;
        }
        float f = targetStackScroll - lastStackScroll;
        float f2 = targetStackScroll - lastTargetStackScroll;
        float newScroll = targetStackScroll;
        this.mUnfocusedRange.offset(targetStackScroll);
        int size = this.mTaskIndexOverrideMap.size() - 1;
        return newScroll;
    }

    public int getInitialFocusState() {
        RecentsActivityLaunchState launchState = Recents.getConfiguration().getLaunchState();
        if (Recents.getDebugFlags().isPagingEnabled() || launchState.launchedWithAltTab) {
            return 1;
        }
        return 0;
    }

    public TaskViewTransform getFrontOfStackTransform() {
        return this.mFrontOfStackTransform;
    }

    public StackState getStackState() {
        return this.mState;
    }

    public boolean isInitialized() {
        return !this.mStackRect.isEmpty();
    }

    public VisibilityReport computeStackVisibilityReport(ArrayList<Task> tasks) {
        int numVisibleThumbnails;
        int numVisibleTasks;
        int i;
        int numVisibleThumbnails2;
        ArrayList<Task> arrayList = tasks;
        boolean z = true;
        if (tasks.size() <= 1) {
            return new VisibilityReport(1, 1);
        }
        if (this.mNumStackTasks == 0) {
            return new VisibilityReport(Math.max(this.mNumFreeformTasks, 1), Math.max(this.mNumFreeformTasks, 1));
        }
        TaskViewTransform tmpTransform = new TaskViewTransform();
        Range currentRange = ((float) getInitialFocusState()) > 0.0f ? this.mFocusedRange : this.mUnfocusedRange;
        currentRange.offset(this.mInitialScrollP);
        int numVisibleTasks2 = Math.max(this.mNumFreeformTasks, 1);
        int numVisibleThumbnails3 = Math.max(this.mNumFreeformTasks, 1);
        float prevScreenX = 2.14748365E9f;
        int numVisibleTasks3 = 0;
        while (true) {
            int i2 = numVisibleTasks3;
            if (i2 > tasks.size() - z) {
                numVisibleThumbnails = numVisibleThumbnails3;
                numVisibleTasks = numVisibleTasks2;
                break;
            }
            Task task = arrayList.get(i2);
            if (!task.isFreeformTask()) {
                float taskProgress = getStackScrollForTask(task);
                if (currentRange.isInRange(taskProgress)) {
                    boolean isFrontMostTaskInGroup = (task.group == null || task.group.isFrontMostTask(task)) ? z : false;
                    if (isFrontMostTaskInGroup) {
                        float f = taskProgress;
                        Task task2 = task;
                        i = i2;
                        numVisibleThumbnails = numVisibleThumbnails3;
                        getStackTransform(taskProgress, taskProgress, this.mInitialScrollP, this.mFocusState, tmpTransform, null, false, false);
                        float screenX = tmpTransform.rect.left;
                        if (screenX - prevScreenX > ((float) this.mTaskRect.width())) {
                            numVisibleThumbnails3 = numVisibleThumbnails + 1;
                            numVisibleTasks2++;
                            prevScreenX = screenX;
                        } else {
                            int j = i;
                            while (j >= 0) {
                                numVisibleTasks2++;
                                float taskProgress2 = getStackScrollForTask(arrayList.get(j));
                                currentRange.isInRange(taskProgress2);
                                j--;
                                float f2 = taskProgress2;
                            }
                            numVisibleTasks = numVisibleTasks2;
                        }
                    } else {
                        Task task3 = task;
                        i = i2;
                        numVisibleThumbnails2 = numVisibleThumbnails3;
                        if (!isFrontMostTaskInGroup) {
                            numVisibleTasks2++;
                            numVisibleThumbnails3 = numVisibleThumbnails2;
                        }
                        numVisibleThumbnails3 = numVisibleThumbnails2;
                    }
                    numVisibleTasks3 = i + 1;
                    z = true;
                }
            }
            i = i2;
            numVisibleThumbnails2 = numVisibleThumbnails3;
            numVisibleThumbnails3 = numVisibleThumbnails2;
            numVisibleTasks3 = i + 1;
            z = true;
        }
        return new VisibilityReport(numVisibleTasks, numVisibleThumbnails);
    }

    public TaskViewTransform getStackTransform(Task task, float stackScroll, TaskViewTransform transformOut, TaskViewTransform frontTransform) {
        return getStackTransform(task, stackScroll, this.mFocusState, transformOut, frontTransform, false, false);
    }

    public TaskViewTransform getStackTransform(Task task, float stackScroll, TaskViewTransform transformOut, TaskViewTransform frontTransform, boolean ignoreTaskOverrides) {
        return getStackTransform(task, stackScroll, this.mFocusState, transformOut, frontTransform, false, ignoreTaskOverrides);
    }

    public TaskViewTransform getStackTransform(Task task, float stackScroll, int focusState, TaskViewTransform transformOut, TaskViewTransform frontTransform, boolean forceUpdate, boolean ignoreTaskOverrides) {
        float stackScrollForTask;
        Task task2 = task;
        TaskViewTransform taskViewTransform = transformOut;
        if (this.mFreeformLayoutAlgorithm.isTransformAvailable(task2, this)) {
            this.mFreeformLayoutAlgorithm.getTransform(task2, taskViewTransform, this);
            return taskViewTransform;
        }
        int nonOverrideTaskProgress = this.mTaskIndexMap.get(task2.key.id, -1);
        if (task2 == null || nonOverrideTaskProgress == -1) {
            transformOut.reset();
            return taskViewTransform;
        }
        if (ignoreTaskOverrides) {
            stackScrollForTask = (float) nonOverrideTaskProgress;
        } else {
            stackScrollForTask = getStackScrollForTask(task2);
        }
        float taskProgress = stackScrollForTask;
        getStackTransform(taskProgress, (float) nonOverrideTaskProgress, stackScroll, focusState, taskViewTransform, frontTransform, false, forceUpdate);
        return taskViewTransform;
    }

    public TaskViewTransform getStackTransformScreenCoordinates(Task task, float stackScroll, TaskViewTransform transformOut, TaskViewTransform frontTransform, Rect windowOverrideRect) {
        return transformToScreenCoordinates(getStackTransform(task, stackScroll, this.mFocusState, transformOut, frontTransform, true, false), windowOverrideRect);
    }

    public TaskViewTransform transformToScreenCoordinates(TaskViewTransform transformOut, Rect windowOverrideRect) {
        Rect windowRect = windowOverrideRect != null ? windowOverrideRect : Recents.getSystemServices().getWindowRect();
        transformOut.rect.offset((float) windowRect.left, (float) windowRect.top);
        return transformOut;
    }

    public void getStackTransform(float taskProgress, float nonOverrideTaskProgress, float stackScroll, int focusState, TaskViewTransform transformOut, TaskViewTransform frontTransform, boolean ignoreSingleTaskCase, boolean forceUpdate) {
        float f = taskProgress;
        TaskViewTransform taskViewTransform = transformOut;
        int[] positon = calculateTaskViewXandY((int) f, this.mTaskRect);
        boolean z = false;
        int x = positon[0];
        int y = positon[1];
        if (this.mNumStackTasks == 1) {
            x = 0;
        }
        int y2 = (int) (((float) y) - (((float) this.mTaskRect.height()) * stackScroll));
        float f2 = 1.0f;
        taskViewTransform.scale = 1.0f;
        if (stackScroll < 0.0f) {
            f2 = 1.0f - (this.mAccelerateInterpolator.getInterpolation(Math.abs(stackScroll)) / 3.0f);
        }
        taskViewTransform.alpha = f2;
        taskViewTransform.translationZ = 0.0f;
        taskViewTransform.dimAlpha = 0.0f;
        taskViewTransform.viewOutlineAlpha = 0.0f;
        taskViewTransform.rect.set((f != 0.0f || this.mNumStackTasks <= 1) ? this.mTaskRect : this.mFirstTaskRect);
        taskViewTransform.rect.offset((float) x, (float) y2);
        Utilities.scaleRectAboutCenter(taskViewTransform.rect, taskViewTransform.scale);
        if (taskViewTransform.rect.bottom > ((float) this.mWindowRect.top) && taskViewTransform.rect.top < ((float) this.mWindowRect.bottom)) {
            z = true;
        }
        taskViewTransform.visible = z;
    }

    /* access modifiers changed from: package-private */
    public int[] calculateTaskViewXandY(int index, Rect rect) {
        int rightOffsetX;
        int leftOffsetX;
        int leftHeight;
        int rightHeight;
        int[] result = new int[2];
        int leftHeight2 = this.mPaddingTop;
        int rightHeight2 = this.mPaddingTop;
        if (isDockedMode()) {
            leftOffsetX = (this.mStackRect.left + this.mHorizontalGap) - this.mTaskRect.left;
            rightOffsetX = ((this.mStackRect.right - this.mHorizontalGap) - this.mTaskRect.right) - this.mPaddingRight;
        } else {
            leftOffsetX = ((this.mStackRect.left + this.mHorizontalGap) - this.mTaskRect.left) + this.mPaddingLeft;
            rightOffsetX = ((this.mStackRect.right - this.mHorizontalGap) - this.mTaskRect.right) - this.mPaddingRight;
        }
        int rightHeight3 = rightHeight2;
        int leftHeight3 = leftHeight2;
        for (int i = 0; i < index; i++) {
            if (i == 0) {
                if (!this.mIsRtlLayout) {
                    leftHeight = this.mVerticalGap + leftHeight3 + this.mFirstTaskRect.height();
                } else {
                    rightHeight = this.mVerticalGap + rightHeight3 + this.mFirstTaskRect.height();
                    rightHeight3 = rightHeight;
                }
            } else if (leftHeight3 > rightHeight3) {
                rightHeight = this.mVerticalGap + rightHeight3 + rect.height();
                rightHeight3 = rightHeight;
            } else {
                leftHeight = this.mVerticalGap + leftHeight3 + rect.height();
            }
            leftHeight3 = leftHeight;
        }
        if (leftHeight3 > rightHeight3 || (this.mIsRtlLayout && index == 0)) {
            result[0] = rightOffsetX;
            result[1] = ((this.mVerticalGap + rightHeight3) + this.mWindowRect.top) - rect.top;
        } else {
            result[0] = leftOffsetX;
            result[1] = ((this.mVerticalGap + leftHeight3) + this.mWindowRect.top) - rect.top;
        }
        return result;
    }

    public Rect getUntransformedTaskViewBounds() {
        return new Rect(this.mTaskRect);
    }

    /* access modifiers changed from: package-private */
    public float getStackScrollForTask(Task t) {
        this.mTaskIndexOverrideMap.get(t.key.id, null);
        return (float) this.mTaskIndexMap.get(t.key.id, 0);
    }

    /* access modifiers changed from: package-private */
    public float getTrueStackScrollForTask(Task t) {
        int index = this.mTaskIndexMap.get(t.key.id, 0);
        if (index == 0 || index == 1) {
            return 0.0f;
        }
        return (((float) calculateTaskViewXandY(index, this.mTaskRect)[1]) + ((float) this.mTaskRect.top)) / ((float) this.mTaskRect.height());
    }

    public float getDeltaPForX(int downY, int y) {
        return -((((float) (y - downY)) / ((float) this.mStackRect.height())) * this.mUnfocusedCurveInterpolator.getArcLength());
    }

    public int getXForDeltaP(float downScrollP, float p) {
        return -((int) ((p - downScrollP) * ((float) this.mStackRect.height()) * (1.0f / this.mUnfocusedCurveInterpolator.getArcLength())));
    }

    public void getTaskStackBounds(Rect displayRect, Rect windowRect, int topInset, int leftInset, int rightInset, Rect taskStackBounds) {
        Rect rect = windowRect;
        Rect rect2 = taskStackBounds;
        rect2.set(rect.left + leftInset, rect.top + topInset, rect.right - rightInset, rect.bottom);
        rect2.inset((taskStackBounds.width() - (taskStackBounds.width() - (2 * getScaleForExtent(rect, displayRect, this.mBaseSideMargin, this.mMinMargin, 0)))) / 2, 0);
    }

    public static int getDimensionForDevice(Context ctx, int phoneResId, int tabletResId, int xlargeTabletResId) {
        return getDimensionForDevice(ctx, phoneResId, phoneResId, tabletResId, tabletResId, xlargeTabletResId, xlargeTabletResId);
    }

    public static int getDimensionForDevice(Context ctx, int phonePortResId, int phoneLandResId, int tabletPortResId, int tabletLandResId, int xlargeTabletPortResId, int xlargeTabletLandResId) {
        RecentsConfiguration config = Recents.getConfiguration();
        Resources res = ctx.getResources();
        boolean isLandscape = Utilities.getAppConfiguration(ctx).orientation == 2;
        if (config.isXLargeScreen) {
            return res.getDimensionPixelSize(isLandscape ? xlargeTabletLandResId : xlargeTabletPortResId);
        } else if (config.isLargeScreen) {
            return res.getDimensionPixelSize(isLandscape ? tabletLandResId : tabletPortResId);
        } else {
            return res.getDimensionPixelSize(isLandscape ? phoneLandResId : phonePortResId);
        }
    }

    private float getNormalizedXFromUnfocusedY(float y, int fromSide) {
        float offset;
        if (fromSide == 0) {
            offset = ((float) this.mStackRect.width()) - y;
        } else {
            offset = y;
        }
        return this.mUnfocusedCurveInterpolator.getX(offset / ((float) this.mStackRect.width()));
    }

    private float getNormalizedXFromFocusedY(float y, int fromSide) {
        float offset;
        if (fromSide == 0) {
            offset = ((float) this.mStackRect.width()) - y;
        } else {
            offset = y;
        }
        return this.mFocusedCurveInterpolator.getX(offset / ((float) this.mStackRect.width()));
    }

    private Path linearCurve() {
        Path p = new Path();
        p.moveTo(0.0f, -1.5f);
        p.lineTo(3.5f, 2.0f);
        return p;
    }

    private int getScaleForExtent(Rect instance, Rect other, int value, int minValue, int extent) {
        if (extent == 0) {
            return Math.max(minValue, (int) (((float) value) * Utilities.clamp01(((float) instance.width()) / ((float) other.width()))));
        } else if (extent != 1) {
            return value;
        } else {
            return Math.max(minValue, (int) (((float) value) * Utilities.clamp01(((float) instance.height()) / ((float) other.height()))));
        }
    }

    private void updateFrontBackTransforms() {
        if (!this.mStackRect.isEmpty()) {
            float min = Utilities.mapRange((float) this.mFocusState, this.mUnfocusedRange.relativeMin, this.mFocusedRange.relativeMin);
            float max = Utilities.mapRange((float) this.mFocusState, this.mUnfocusedRange.relativeMax, this.mFocusedRange.relativeMax);
            getStackTransform(min, min, 1.0f, this.mFocusState, this.mFrontOfStackTransform, null, true, true);
            getStackTransform(max, max, -1.0f, this.mFocusState, this.mBackOfStackTransform, null, true, true);
            this.mBackOfStackTransform.visible = true;
            this.mFrontOfStackTransform.visible = true;
        }
    }

    public void dump(String prefix, PrintWriter writer) {
        String innerPrefix = prefix + "  ";
        writer.print(prefix);
        writer.print("TaskStackLayoutAlgorithm");
        writer.write(" numStackTasks=");
        writer.write(this.mNumStackTasks);
        writer.println();
        writer.print(innerPrefix);
        writer.print("insets=");
        writer.print(Utilities.dumpRect(this.mSystemInsets));
        writer.print(" stack=");
        writer.print(Utilities.dumpRect(this.mStackRect));
        writer.print(" task=");
        writer.print(Utilities.dumpRect(this.mTaskRect));
        writer.print(" freeform=");
        writer.print(Utilities.dumpRect(this.mFreeformRect));
        writer.print(" actionButton=");
        writer.print(Utilities.dumpRect(this.mStackActionButtonRect));
        writer.println();
        writer.print(innerPrefix);
        writer.print("minScroll=");
        writer.print(this.mMinScrollP);
        writer.print(" maxScroll=");
        writer.print(this.mMaxScrollP);
        writer.print(" initialScroll=");
        writer.print(this.mInitialScrollP);
        writer.println();
        writer.print(innerPrefix);
        writer.print("focusState=");
        writer.print(this.mFocusState);
        writer.println();
        if (this.mTaskIndexOverrideMap.size() > 0) {
            for (int i = this.mTaskIndexOverrideMap.size() - 1; i >= 0; i--) {
                int taskId = this.mTaskIndexOverrideMap.keyAt(i);
                float overrideX = this.mTaskIndexOverrideMap.get(taskId, Float.valueOf(0.0f)).floatValue();
                writer.print(innerPrefix);
                writer.print("taskId= ");
                writer.print(taskId);
                writer.print(" x= ");
                writer.print((float) this.mTaskIndexMap.get(taskId));
                writer.print(" overrideX= ");
                writer.print(overrideX);
                writer.println();
            }
        }
    }

    public boolean isLandscapeMode(Context context) {
        return Utilities.getAppConfiguration(context).orientation == 2 && !isDockedMode();
    }

    public boolean isDockedMode() {
        boolean hasDockedTask;
        if (Utilities.isAndroidNorNewer()) {
            Divider divider = (Divider) ((Application) this.mContext.getApplicationContext()).getSystemUIApplication().getComponent(Divider.class);
            hasDockedTask = divider != null ? divider.isExists() : false;
        } else {
            hasDockedTask = Recents.getSystemServices().hasDockedTask();
        }
        if (hasDockedTask || this.mDropToDockState) {
            return true;
        }
        return false;
    }

    public void updatePaddingOfNotch(int rotation) {
        if (Constants.IS_NOTCH && !Utilities.isAndroidPorNewer()) {
            int i = 0;
            this.mPaddingLeft = rotation == 1 ? this.mStatusbarHeight : 0;
            if (rotation == 3) {
                i = this.mStatusbarHeight;
            }
            this.mPaddingRight = i;
        }
    }
}
