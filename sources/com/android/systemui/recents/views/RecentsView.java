package com.android.systemui.recents.views;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.ActivityOptions;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.Log;
import android.view.AppTransitionAnimationSpec;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewPropertyAnimator;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.Constants;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.RecentsActivityLaunchState;
import com.android.systemui.recents.RecentsConfiguration;
import com.android.systemui.recents.events.RecentsEventBus;
import com.android.systemui.recents.events.activity.ConfigurationChangedEvent;
import com.android.systemui.recents.events.activity.DismissRecentsToHomeAnimationStarted;
import com.android.systemui.recents.events.activity.DockedFirstAnimationFrameEvent;
import com.android.systemui.recents.events.activity.EnterRecentsWindowAnimationCompletedEvent;
import com.android.systemui.recents.events.activity.HideRecentsEvent;
import com.android.systemui.recents.events.activity.HideStackActionButtonEvent;
import com.android.systemui.recents.events.activity.LaunchTaskEvent;
import com.android.systemui.recents.events.activity.MultiWindowStateChangedEvent;
import com.android.systemui.recents.events.activity.ShowStackActionButtonEvent;
import com.android.systemui.recents.events.ui.AllTaskViewsDismissedEvent;
import com.android.systemui.recents.events.ui.DismissAllTaskViewsEvent;
import com.android.systemui.recents.events.ui.DraggingInRecentsEndedEvent;
import com.android.systemui.recents.events.ui.DraggingInRecentsEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragDropTargetChangedEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragEndCancelledEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragEndEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragStartEvent;
import com.android.systemui.recents.misc.RecentsPushEventHelper;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.model.TaskStack;
import com.android.systemui.recents.views.RecentsTransitionHelper;
import com.android.systemui.stackdivider.WindowManagerProxy;
import com.android.systemui.statusbar.FlingAnimationUtils;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class RecentsView extends FrameLayout {
    private boolean mAwaitingFirstLayout;
    private Drawable mBackgroundScrim;
    private Animator mBackgroundScrimAnimator;
    private float mDefaultScrimAlpha;
    private int mDividerSize;
    private TextView mEmptyView;
    private final FlingAnimationUtils mFlingAnimationUtils;
    private boolean mLastTaskLaunchedWasFreeform;
    private Drawable mRecentBackground;
    private RecentMenuView mRecentMenuView;
    private TaskStack mStack;
    /* access modifiers changed from: private */
    public TextView mStackActionButton;
    @ViewDebug.ExportedProperty(category = "recents")
    private Rect mSystemInsets;
    /* access modifiers changed from: private */
    public TaskStackView mTaskStackView;
    @ViewDebug.ExportedProperty(deepExport = true, prefix = "touch_")
    private RecentsViewTouchHandler mTouchHandler;
    /* access modifiers changed from: private */
    public RecentsTransitionHelper mTransitionHelper;

    public RecentsView(Context context) {
        this(context, null);
    }

    public RecentsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecentsView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public RecentsView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mAwaitingFirstLayout = true;
        this.mSystemInsets = new Rect();
        setWillNotDraw(false);
        SystemServicesProxy ssp = Recents.getSystemServices();
        this.mTransitionHelper = new RecentsTransitionHelper(getContext());
        this.mDividerSize = ssp.getDockedDividerSize(context);
        this.mTouchHandler = new RecentsViewTouchHandler(this);
        this.mFlingAnimationUtils = new FlingAnimationUtils(context, 0.3f);
        LayoutInflater inflater = LayoutInflater.from(context);
        this.mEmptyView = (TextView) inflater.inflate(R.layout.recents_empty, this, false);
        this.mEmptyView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                RecentsEventBus.getDefault().send(new HideRecentsEvent(false, true, false));
            }
        });
        addView(this.mEmptyView);
        this.mRecentMenuView = (RecentMenuView) inflater.inflate(R.layout.recent_menu_view, this, false);
        addView(this.mRecentMenuView, -1, -1);
        this.mDefaultScrimAlpha = context.getResources().getFloat(R.dimen.recent_background_scrim_alpha);
        this.mBackgroundScrim = new ColorDrawable(Color.argb((int) (this.mDefaultScrimAlpha * 255.0f), 0, 0, 0)).mutate();
        this.mRecentBackground = context.getResources().getDrawable(R.drawable.recent_task_bg, context.getTheme());
    }

    public void onReload(boolean isResumingFromVisible, boolean isTaskStackEmpty) {
        RecentsActivityLaunchState launchState = Recents.getConfiguration().getLaunchState();
        if (this.mTaskStackView == null) {
            isResumingFromVisible = false;
            this.mTaskStackView = new TaskStackView(getContext());
            this.mTaskStackView.setSystemInsets(this.mSystemInsets);
            addView(this.mTaskStackView);
            this.mRecentMenuView.setTaskStackView(this.mTaskStackView);
            RecentsEventBus.getDefault().register(this.mTaskStackView, 3);
        }
        this.mAwaitingFirstLayout = !isResumingFromVisible;
        this.mLastTaskLaunchedWasFreeform = false;
        this.mTaskStackView.onReload(isResumingFromVisible);
        if (launchState.launchedViaFsGesture) {
            this.mBackgroundScrim.setAlpha(255);
        } else if (isResumingFromVisible) {
            animateBackgroundScrim(1.0f, 200);
        } else if (launchState.launchedViaDockGesture || launchState.launchedFromApp || isTaskStackEmpty) {
            this.mBackgroundScrim.setAlpha(255);
        } else {
            this.mBackgroundScrim.setAlpha(0);
        }
    }

    public void updateStack(TaskStack stack, boolean setStackViewTasks) {
        int i;
        this.mStack = stack;
        if (setStackViewTasks) {
            this.mTaskStackView.setTasks(stack, true);
        }
        if (stack.getTaskCount() > 0) {
            hideEmptyView();
            return;
        }
        if (Recents.getSystemServices().hasDockedTask()) {
            i = R.string.recents_empty_message_multi_window;
        } else {
            i = R.string.recents_empty_message;
        }
        showEmptyView(i);
    }

    public TaskStack getStack() {
        return this.mStack;
    }

    public Drawable getBackgroundScrim() {
        if (this.mRecentBackground instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable) this.mRecentBackground).getBitmap();
            if (bitmap.getWidth() == 1 && bitmap.getHeight() == 1 && Color.alpha(bitmap.getPixel(0, 0)) == 0) {
                return this.mBackgroundScrim;
            }
        }
        return this.mRecentBackground;
    }

    public boolean isLastTaskLaunchedFreeform() {
        return this.mLastTaskLaunchedWasFreeform;
    }

    public boolean launchTargetTask(int logEvent) {
        if (this.mTaskStackView != null) {
            Task task = this.mTaskStackView.getStack().getLaunchTarget();
            if (task != null) {
                TaskView taskView = this.mTaskStackView.getChildViewForTask(task);
                RecentsEventBus recentsEventBus = RecentsEventBus.getDefault();
                LaunchTaskEvent launchTaskEvent = new LaunchTaskEvent(taskView, task, null, -1, false);
                recentsEventBus.send(launchTaskEvent);
                if (logEvent != 0) {
                    MetricsLogger.action(getContext(), logEvent, task.key.getComponent().toString());
                }
                return true;
            }
        }
        return false;
    }

    public boolean launchPreviousTask() {
        if (this.mTaskStackView != null) {
            Task task = this.mTaskStackView.getStack().getLaunchTarget();
            if (task != null) {
                TaskView taskView = this.mTaskStackView.getChildViewForTask(task);
                RecentsEventBus recentsEventBus = RecentsEventBus.getDefault();
                LaunchTaskEvent launchTaskEvent = new LaunchTaskEvent(taskView, task, null, -1, false);
                recentsEventBus.send(launchTaskEvent);
                return true;
            }
        }
        return false;
    }

    public void showEmptyView(int msgResId) {
        this.mEmptyView.setText(msgResId);
        this.mEmptyView.setVisibility(0);
    }

    public void hideEmptyView() {
        this.mEmptyView.setVisibility(4);
        this.mTaskStackView.setVisibility(0);
        this.mTaskStackView.bringToFront();
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        RecentsEventBus.getDefault().register(this, 3);
        RecentsEventBus.getDefault().register(this.mTouchHandler, 4);
        super.onAttachedToWindow();
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        RecentsEventBus.getDefault().unregister(this);
        RecentsEventBus.getDefault().unregister(this.mTouchHandler);
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = View.MeasureSpec.getSize(widthMeasureSpec);
        int height = View.MeasureSpec.getSize(heightMeasureSpec);
        if (this.mTaskStackView.getVisibility() != 8) {
            this.mTaskStackView.measure(widthMeasureSpec, heightMeasureSpec);
        }
        if (this.mEmptyView.getVisibility() != 8) {
            measureChild(this.mEmptyView, View.MeasureSpec.makeMeasureSpec(width, 1073741824), View.MeasureSpec.makeMeasureSpec(height, 1073741824));
        }
        if (this.mRecentMenuView.getVisibility() != 8) {
            measureChild(this.mRecentMenuView, View.MeasureSpec.makeMeasureSpec(width, 1073741824), View.MeasureSpec.makeMeasureSpec(height, 1073741824));
        }
        setMeasuredDimension(width, height);
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int i = left;
        int i2 = top;
        if (this.mTaskStackView.getVisibility() != 8) {
            this.mTaskStackView.layout(i, i2, getMeasuredWidth() + i, getMeasuredHeight() + i2);
        }
        if (this.mEmptyView.getVisibility() != 8) {
            int leftRightInsets = this.mSystemInsets.left + this.mSystemInsets.right;
            int topBottomInsets = this.mSystemInsets.top + this.mSystemInsets.bottom;
            int childWidth = this.mEmptyView.getMeasuredWidth();
            int childHeight = this.mEmptyView.getMeasuredHeight();
            int childLeft = this.mSystemInsets.left + i + (Math.max(0, ((right - i) - leftRightInsets) - childWidth) / 2);
            int childTop = this.mSystemInsets.top + i2 + (Math.max(0, ((bottom - i2) - topBottomInsets) - childHeight) / 2);
            this.mEmptyView.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
        }
        if (this.mRecentMenuView.getVisibility() != 8) {
            this.mRecentMenuView.layout(0, 0, this.mRecentMenuView.getMeasuredWidth(), this.mRecentMenuView.getMeasuredHeight());
        }
        if (this.mAwaitingFirstLayout) {
            this.mAwaitingFirstLayout = false;
            if (Recents.getConfiguration().getLaunchState().launchedViaDragGesture) {
                setTranslationY((float) getMeasuredHeight());
            } else {
                setTranslationY(0.0f);
            }
        }
    }

    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        this.mSystemInsets.set(insets.getSystemWindowInsets());
        this.mTaskStackView.setSystemInsets(this.mSystemInsets);
        requestLayout();
        return insets;
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return this.mTouchHandler.onInterceptTouchEvent(ev);
    }

    public boolean onTouchEvent(MotionEvent ev) {
        return this.mTouchHandler.onTouchEvent(ev);
    }

    public void onDrawForeground(Canvas canvas) {
        super.onDrawForeground(canvas);
        ArrayList<TaskStack.DockState> visDockStates = this.mTouchHandler.getVisibleDockStates();
        for (int i = visDockStates.size() - 1; i >= 0; i--) {
            visDockStates.get(i).viewState.draw(canvas);
        }
    }

    /* access modifiers changed from: protected */
    public boolean verifyDrawable(Drawable who) {
        ArrayList<TaskStack.DockState> visDockStates = this.mTouchHandler.getVisibleDockStates();
        for (int i = visDockStates.size() - 1; i >= 0; i--) {
            if (visDockStates.get(i).viewState.dockAreaOverlay == who) {
                return true;
            }
        }
        return super.verifyDrawable(who);
    }

    public final void onBusEvent(LaunchTaskEvent event) {
        this.mLastTaskLaunchedWasFreeform = event.task.isFreeformTask();
        if (Recents.getConfiguration().getLaunchState().launchedViaFsGesture && Recents.getConfiguration().getLaunchState().launchedFromHome) {
            Recents.getSystemServices().changeAlphaScaleForFsGesture(Constants.HOME_LAUCNHER_PACKAGE_NAME, 0.0f, 1.0f);
        }
        this.mTransitionHelper.launchTaskFromRecents(this.mStack, event.task, this.mTaskStackView, event.taskView, event.screenPinningRequested, event.targetTaskBounds, event.targetTaskStack);
    }

    public final void onBusEvent(DismissRecentsToHomeAnimationStarted event) {
        animateBackgroundScrim(0.0f, 350);
    }

    public final void onBusEvent(DragStartEvent event) {
    }

    public final void onBusEvent(DragDropTargetChangedEvent event) {
        if (event.dropTarget == null || !(event.dropTarget instanceof TaskStack.DockState)) {
            updateVisibleDockRegions(this.mTouchHandler.getDockStatesForCurrentOrientation(), true, TaskStack.DockState.NONE.viewState.dockAreaAlpha, TaskStack.DockState.NONE.viewState.hintTextAlpha, true, true);
        } else {
            updateVisibleDockRegions(new TaskStack.DockState[]{(TaskStack.DockState) event.dropTarget}, false, -1, -1, true, true);
        }
        if (this.mStackActionButton != null) {
            event.addPostAnimationCallback(new Runnable() {
                public void run() {
                    Rect buttonBounds = RecentsView.this.getStackActionButtonBoundsFromStackLayout();
                    RecentsView.this.mStackActionButton.setLeftTopRightBottom(buttonBounds.left, buttonBounds.top, buttonBounds.right, buttonBounds.bottom);
                }
            });
        }
    }

    public final void onBusEvent(final DragEndEvent event) {
        if (event.dropTarget instanceof TaskStack.DockState) {
            updateVisibleDockRegions(null, false, -1, -1, false, false);
            RecentsConfiguration.sCanMultiWindow = false;
            Utilities.setViewFrameFromTranslation(event.taskView);
            SystemServicesProxy ssp = Recents.getSystemServices();
            if (ssp.startTaskInDockedMode(event.task, ((TaskStack.DockState) event.dropTarget).createMode, getContext())) {
                this.mTaskStackView.mIsMultiStateChanging = true;
                if (!Utilities.isAndroidNorNewer()) {
                    event.taskView.setVisibility(4);
                }
                ActivityOptions.OnAnimationStartedListener startedListener = new ActivityOptions.OnAnimationStartedListener() {
                    public void onAnimationStarted() {
                        RecentsEventBus.getDefault().send(new DockedFirstAnimationFrameEvent());
                        RecentsView.this.mTaskStackView.getStack().removeTask(event.task, null, true);
                        if (!Utilities.isAndroidNorNewer()) {
                            event.taskView.setVisibility(0);
                        }
                    }
                };
                final Rect taskRect = getTaskRect(event.taskView);
                ssp.overridePendingAppTransitionMultiThumbFuture(this.mTransitionHelper.getAppTransitionFuture(new RecentsTransitionHelper.AnimationSpecComposer() {
                    public List<AppTransitionAnimationSpec> composeSpecs() {
                        return RecentsView.this.mTransitionHelper.composeDockAnimationSpec(event.taskView, taskRect);
                    }
                }, getHandler()), this.mTransitionHelper.wrapStartedListener(startedListener), true);
                MetricsLogger.action(this.mContext, 270, event.task.getTopComponent().flattenToShortString());
                RecentsPushEventHelper.sendMultiWindowEvent("enterMultiWindow", "in recents");
            } else {
                RecentsEventBus.getDefault().send(new DragEndCancelledEvent(this.mStack, event.task, event.taskView));
            }
        }
        if (this.mStackActionButton != null) {
            this.mStackActionButton.animate().alpha(1.0f).setDuration(134).setInterpolator(Interpolators.ALPHA_IN).start();
        }
    }

    public final void onBusEvent(DragEndCancelledEvent event) {
    }

    private Rect getTaskRect(TaskView taskView) {
        int[] location = taskView.getLocationOnScreen();
        int viewX = location[0];
        int viewY = location[1];
        return new Rect(viewX, viewY, (int) (((float) viewX) + (((float) taskView.getWidth()) * taskView.getScaleX())), (int) (((float) viewY) + (((float) taskView.getHeight()) * taskView.getScaleY())));
    }

    public final void onBusEvent(DraggingInRecentsEvent event) {
        if (this.mTaskStackView.getTaskViews().size() > 0) {
            setTranslationY(event.distanceFromTop - this.mTaskStackView.getTaskViews().get(0).getY());
        }
    }

    public final void onBusEvent(DraggingInRecentsEndedEvent event) {
        ViewPropertyAnimator animator = animate();
        if (event.velocity > this.mFlingAnimationUtils.getMinVelocityPxPerSecond()) {
            animator.translationY((float) getHeight());
            animator.withEndAction(new Runnable() {
                public void run() {
                    WindowManagerProxy.getInstance().maximizeDockedStack();
                }
            });
            this.mFlingAnimationUtils.apply(animator, getTranslationY(), (float) getHeight(), event.velocity);
        } else {
            animator.translationY(0.0f);
            animator.setListener(null);
            this.mFlingAnimationUtils.apply(animator, getTranslationY(), 0.0f, event.velocity);
        }
        animator.start();
    }

    public final void onBusEvent(EnterRecentsWindowAnimationCompletedEvent event) {
        RecentsActivityLaunchState launchState = Recents.getConfiguration().getLaunchState();
        if (!launchState.launchedViaDockGesture && !launchState.launchedFromApp && !launchState.launchedViaFsGesture && this.mStack.getTaskCount() > 0) {
            animateBackgroundScrim(1.0f, 180);
        }
    }

    public final void onBusEvent(AllTaskViewsDismissedEvent event) {
        hideStackActionButton(100, true);
    }

    public final void onBusEvent(DismissAllTaskViewsEvent event) {
        Recents.getSystemServices().hasDockedTask();
    }

    public final void onBusEvent(ShowStackActionButtonEvent event) {
    }

    public final void onBusEvent(HideStackActionButtonEvent event) {
    }

    public final void onBusEvent(MultiWindowStateChangedEvent event) {
        updateStack(event.stack, false);
    }

    public final void onBusEvent(ConfigurationChangedEvent event) {
        if (event.fromDeviceOrientationChange) {
            hideDockRegionsAnim();
            if (RecentsConfiguration.sCanMultiWindow) {
                postDelayed(new Runnable() {
                    public void run() {
                        RecentsView.this.showDockRegionsAnim();
                    }
                }, 100);
            }
            Configuration newDeviceConfiguration = Utilities.getAppConfiguration(getContext());
            WallpaperInfo wallpaperInfo = WallpaperManager.getInstance(getContext()).getWallpaperInfo();
            if (newDeviceConfiguration.orientation != 2 || wallpaperInfo == null) {
                this.mBackgroundScrim.setColorFilter(null);
            } else {
                this.mBackgroundScrim.setColorFilter(-16777216, PorterDuff.Mode.SRC);
            }
            this.mRecentMenuView.removeMenu(false);
        }
    }

    private void hideStackActionButton(int duration, boolean translate) {
    }

    private void updateVisibleDockRegions(TaskStack.DockState[] newDockStates, boolean isDefaultDockState, int overrideAreaAlpha, int overrideHintAlpha, boolean animateAlpha, boolean animateBounds) {
        TaskStack.DockState.ViewState viewState;
        Rect bounds;
        TaskStack.DockState[] dockStateArr = newDockStates;
        ArraySet<TaskStack.DockState> newDockStatesSet = Utilities.arrayToSet(dockStateArr, new ArraySet());
        ArrayList<TaskStack.DockState> visDockStates = this.mTouchHandler.getVisibleDockStates();
        for (int i = visDockStates.size() - 1; i >= 0; i--) {
            TaskStack.DockState dockState = visDockStates.get(i);
            TaskStack.DockState.ViewState viewState2 = dockState.viewState;
            if (dockStateArr == null) {
                viewState = viewState2;
            } else if (!newDockStatesSet.contains(dockState)) {
                viewState = viewState2;
            } else {
                int i2 = overrideAreaAlpha;
                int areaAlpha = i2 != -1 ? i2 : viewState2.dockAreaAlpha;
                int i3 = overrideHintAlpha;
                int hintAlpha = i3 != -1 ? i3 : viewState2.hintTextAlpha;
                if (isDefaultDockState) {
                    bounds = dockState.getPreDockedBounds(getMeasuredWidth(), getMeasuredHeight());
                } else {
                    bounds = dockState.getDockedBounds(getMeasuredWidth(), getMeasuredHeight(), this.mDividerSize, this.mSystemInsets, getResources());
                }
                if (viewState2.dockAreaOverlay.getCallback() != this) {
                    viewState2.dockAreaOverlay.setCallback(this);
                    viewState2.dockAreaOverlay.setBounds(bounds);
                }
                TaskStack.DockState.ViewState viewState3 = viewState2;
                viewState2.startAnimation(bounds, areaAlpha, hintAlpha, 250, Interpolators.FAST_OUT_SLOW_IN, animateAlpha, animateBounds);
            }
            viewState.startAnimation(null, 0, 0, 250, Interpolators.FAST_OUT_SLOW_IN, animateAlpha, animateBounds);
        }
    }

    private void animateBackgroundScrim(float alpha, int duration) {
        Interpolator interpolator;
        Utilities.cancelAnimationWithoutCallbacks(this.mBackgroundScrimAnimator);
        int fromAlpha = (int) ((((float) this.mBackgroundScrim.getAlpha()) / (this.mDefaultScrimAlpha * 255.0f)) * 255.0f);
        int toAlpha = (int) (255.0f * alpha);
        this.mBackgroundScrimAnimator = ObjectAnimator.ofInt(this.mBackgroundScrim, Utilities.DRAWABLE_ALPHA, new int[]{fromAlpha, toAlpha});
        this.mBackgroundScrimAnimator.setDuration((long) duration);
        Animator animator = this.mBackgroundScrimAnimator;
        if (toAlpha > fromAlpha) {
            interpolator = Interpolators.MIUI_ALPHA_IN;
        } else {
            interpolator = Interpolators.MIUI_ALPHA_OUT;
        }
        animator.setInterpolator(interpolator);
        this.mBackgroundScrimAnimator.start();
    }

    /* access modifiers changed from: private */
    public Rect getStackActionButtonBoundsFromStackLayout() {
        int left;
        Rect actionButtonRect = new Rect(this.mTaskStackView.mLayoutAlgorithm.mStackActionButtonRect);
        if (isLayoutRtl()) {
            left = actionButtonRect.left - this.mStackActionButton.getPaddingLeft();
        } else {
            left = (actionButtonRect.right + this.mStackActionButton.getPaddingRight()) - this.mStackActionButton.getMeasuredWidth();
        }
        int top = actionButtonRect.top + ((actionButtonRect.height() - this.mStackActionButton.getMeasuredHeight()) / 2);
        actionButtonRect.set(left, top, this.mStackActionButton.getMeasuredWidth() + left, this.mStackActionButton.getMeasuredHeight() + top);
        return actionButtonRect;
    }

    public void showDockRegionsAnim() {
        this.mTouchHandler.setupVisibleDockStates();
        updateVisibleDockRegions(this.mTouchHandler.getDockStatesForCurrentOrientation(), true, TaskStack.DockState.NONE.viewState.dockAreaAlpha, TaskStack.DockState.NONE.viewState.hintTextAlpha, true, false);
    }

    public void hideDockRegionsAnim() {
        this.mTouchHandler.setupVisibleDockStates();
        updateVisibleDockRegions(null, true, -1, -1, true, false);
    }

    public void dump(String prefix, PrintWriter writer) {
        String innerPrefix = prefix + "  ";
        String id = Integer.toHexString(System.identityHashCode(this));
        writer.print(prefix);
        writer.print("RecentsView");
        writer.print(" awaitingFirstLayout=");
        writer.print(this.mAwaitingFirstLayout ? "Y" : "N");
        writer.print(" insets=");
        writer.print(Utilities.dumpRect(this.mSystemInsets));
        writer.print(" [0x");
        writer.print(id);
        writer.print("]");
        writer.println();
        if (this.mStack != null) {
            this.mStack.dump(innerPrefix, writer);
        }
        if (this.mTaskStackView != null) {
            this.mTaskStackView.dump(innerPrefix, writer);
        }
    }

    public RecentMenuView getMenuView() {
        return this.mRecentMenuView;
    }

    public void updateBlurRatio(float ratio) {
        try {
            View view = getRootView();
            if (view != null && (view.getLayoutParams() instanceof WindowManager.LayoutParams)) {
                WindowManager.LayoutParams lp = (WindowManager.LayoutParams) view.getLayoutParams();
                WindowManagerGlobal sGlobal = WindowManagerGlobal.getInstance();
                lp.flags |= 4;
                lp.blurRatio = ratio;
                sGlobal.updateViewLayout(view, lp);
            }
        } catch (Exception e) {
            Log.e("RecentsView", "updateBlurRatio error.", e);
        }
    }

    public int getTaskViewPaddingView() {
        return this.mTaskStackView.mLayoutAlgorithm.mPaddingTop + this.mTaskStackView.mLayoutAlgorithm.mVerticalGap;
    }

    public void requstLayoutTaskStackView() {
        if (this.mTaskStackView != null) {
            this.mTaskStackView.requestLayout();
        }
    }

    public void release() {
        if (this.mTaskStackView != null) {
            RecentsEventBus.getDefault().unregister(this.mTaskStackView);
        }
    }

    public void startFrontTaskViewHeadFadeInAnim(long duration) {
        TaskView frontTv = this.mTaskStackView.getFrontMostTaskView(true);
        if (frontTv != null) {
            if (duration > 0) {
                frontTv.getHeaderView().animate().alpha(1.0f).setDuration(duration).start();
            } else {
                frontTv.getHeaderView().setAlpha(1.0f);
            }
        }
    }
}
