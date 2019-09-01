package com.android.systemui.recents.views;

import android.graphics.Point;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.ViewDebug;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.RecentsActivity;
import com.android.systemui.recents.RecentsConfiguration;
import com.android.systemui.recents.RecentsImpl;
import com.android.systemui.recents.events.RecentsEventBus;
import com.android.systemui.recents.events.activity.ConfigurationChangedEvent;
import com.android.systemui.recents.events.component.RecentsVisibilityChangedEvent;
import com.android.systemui.recents.events.ui.HideIncompatibleAppOverlayEvent;
import com.android.systemui.recents.events.ui.ShowIncompatibleAppOverlayEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragDropTargetChangedEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragEndEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragStartEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragStartInitializeDropTargetsEvent;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.model.TaskStack;
import com.android.systemui.stackdivider.DividerSnapAlgorithm;
import java.util.ArrayList;
import java.util.Iterator;

public class RecentsViewTouchHandler {
    private DividerSnapAlgorithm mDividerSnapAlgorithm;
    @ViewDebug.ExportedProperty(category = "recents")
    private Point mDownPos = new Point();
    @ViewDebug.ExportedProperty(category = "recents")
    private boolean mDragRequested;
    private float mDragSlop;
    @ViewDebug.ExportedProperty(deepExport = true, prefix = "drag_task")
    private Task mDragTask;
    private ArrayList<DropTarget> mDropTargets = new ArrayList<>();
    @ViewDebug.ExportedProperty(category = "recents")
    private boolean mIsDragging;
    private boolean mIsRemovingMenu = false;
    private DropTarget mLastDropTarget;
    private RecentsView mRv;
    @ViewDebug.ExportedProperty(deepExport = true, prefix = "drag_task_view_")
    private TaskView mTaskView;
    @ViewDebug.ExportedProperty(category = "recents")
    private Point mTaskViewOffset = new Point();
    private ArrayList<TaskStack.DockState> mVisibleDockStates = new ArrayList<>();

    public RecentsViewTouchHandler(RecentsView rv) {
        this.mRv = rv;
        this.mDragSlop = (float) ViewConfiguration.get(rv.getContext()).getScaledTouchSlop();
        updateSnapAlgorithm();
    }

    private void updateSnapAlgorithm() {
        Rect insets = new Rect();
        SystemServicesProxy.getInstance(this.mRv.getContext()).getStableInsets(insets);
        this.mDividerSnapAlgorithm = DividerSnapAlgorithm.create(this.mRv.getContext(), insets);
    }

    public void registerDropTargetForCurrentDrag(DropTarget target) {
        this.mDropTargets.add(target);
    }

    public TaskStack.DockState[] getDockStatesForCurrentOrientation() {
        boolean isLandscape = this.mRv.getResources().getConfiguration().orientation == 2;
        RecentsConfiguration config = Recents.getConfiguration();
        if (isLandscape) {
            return config.isLargeScreen ? DockRegion.TABLET_LANDSCAPE : DockRegion.PHONE_LANDSCAPE;
        }
        if (config.isLargeScreen) {
            return DockRegion.TABLET_PORTRAIT;
        }
        return RecentsActivity.isForceBlack() ? DockRegion.PHONE_PORTRAIT_FORCE_BLACK : DockRegion.PHONE_PORTRAIT;
    }

    public ArrayList<TaskStack.DockState> getVisibleDockStates() {
        return this.mVisibleDockStates;
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        handleTouchEvent(ev);
        return this.mDragRequested || this.mIsRemovingMenu || RecentsImpl.sOneKeyCleaning;
    }

    public boolean onTouchEvent(MotionEvent ev) {
        handleTouchEvent(ev);
        return this.mDragRequested;
    }

    public final void onBusEvent(DragStartEvent event) {
        SystemServicesProxy ssp = Recents.getSystemServices();
        this.mRv.getParent().requestDisallowInterceptTouchEvent(true);
        this.mDragRequested = true;
        this.mIsDragging = false;
        this.mDragTask = event.task;
        this.mTaskView = event.taskView;
        this.mDropTargets.clear();
        int[] recentsViewLocation = new int[2];
        this.mRv.getLocationInWindow(recentsViewLocation);
        this.mTaskViewOffset.set((this.mTaskView.getLeft() - recentsViewLocation[0]) + event.tlOffset.x, (this.mTaskView.getTop() - recentsViewLocation[1]) + event.tlOffset.y);
        this.mTaskView.setTranslationX((float) (this.mDownPos.x - this.mTaskViewOffset.x));
        this.mTaskView.setTranslationY((float) (this.mDownPos.y - this.mTaskViewOffset.y));
        this.mVisibleDockStates.clear();
        if (!ssp.hasDockedTask() && this.mDividerSnapAlgorithm.isSplitScreenFeasible()) {
            Recents.logDockAttempt(this.mRv.getContext(), event.task.getTopComponent(), event.task.resizeMode);
            if (!event.task.isDockable) {
                RecentsEventBus.getDefault().send(new ShowIncompatibleAppOverlayEvent());
            } else {
                setupVisibleDockStates();
            }
        }
        RecentsEventBus.getDefault().send(new DragStartInitializeDropTargetsEvent(event.task, event.taskView, this));
    }

    public void setupVisibleDockStates() {
        this.mDropTargets.clear();
        this.mVisibleDockStates.clear();
        for (TaskStack.DockState dockState : getDockStatesForCurrentOrientation()) {
            registerDropTargetForCurrentDrag(dockState);
            dockState.update(this.mRv.getContext());
            this.mVisibleDockStates.add(dockState);
        }
    }

    public final void onBusEvent(DragEndEvent event) {
        if (this.mDragTask == null || !this.mDragTask.isDockable) {
            RecentsEventBus.getDefault().send(new HideIncompatibleAppOverlayEvent());
        }
        this.mDragRequested = false;
        this.mDragTask = null;
        this.mTaskView = null;
        this.mLastDropTarget = null;
    }

    public final void onBusEvent(ConfigurationChangedEvent event) {
        if (event.fromDisplayDensityChange || event.fromDeviceOrientationChange) {
            updateSnapAlgorithm();
        }
    }

    public final void onBusEvent(RecentsVisibilityChangedEvent event) {
        if (!event.visible && this.mDragRequested) {
            RecentsEventBus.getDefault().send(new DragEndEvent(this.mDragTask, this.mTaskView, null));
        }
    }

    private void handleTouchEvent(MotionEvent ev) {
        int action = ev.getActionMasked();
        boolean cancelled = false;
        switch (action) {
            case 0:
                this.mIsRemovingMenu = this.mRv.getMenuView().isShowOrHideAnimRunning();
                this.mDownPos.set((int) ev.getX(), (int) ev.getY());
                return;
            case 1:
            case 3:
                if (this.mDragRequested) {
                    if (action == 3) {
                        cancelled = true;
                    }
                    DropTarget dropTarget = null;
                    if (cancelled) {
                        RecentsEventBus.getDefault().send(new DragDropTargetChangedEvent(this.mDragTask, null));
                    }
                    RecentsEventBus recentsEventBus = RecentsEventBus.getDefault();
                    Task task = this.mDragTask;
                    TaskView taskView = this.mTaskView;
                    if (!cancelled) {
                        dropTarget = this.mLastDropTarget;
                    }
                    recentsEventBus.send(new DragEndEvent(task, taskView, dropTarget));
                    return;
                }
                return;
            case 2:
                float evX = ev.getX();
                float evY = ev.getY();
                float x = evX - ((float) this.mTaskViewOffset.x);
                float y = evY - ((float) this.mTaskViewOffset.y);
                if (this.mDragRequested) {
                    if (!this.mIsDragging) {
                        if (Math.hypot((double) (evX - ((float) this.mDownPos.x)), (double) (evY - ((float) this.mDownPos.y))) > ((double) this.mDragSlop)) {
                            cancelled = true;
                        }
                        this.mIsDragging = cancelled;
                    }
                    if (this.mIsDragging) {
                        int width = this.mRv.getMeasuredWidth();
                        int height = this.mRv.getMeasuredHeight();
                        DropTarget currentDropTarget = null;
                        if (this.mLastDropTarget != null && this.mLastDropTarget.acceptsDrop((int) evX, (int) evY, width, height, true)) {
                            currentDropTarget = this.mLastDropTarget;
                        }
                        if (currentDropTarget == null) {
                            Iterator<DropTarget> it = this.mDropTargets.iterator();
                            while (true) {
                                if (it.hasNext()) {
                                    DropTarget target = it.next();
                                    int[] taskViewLocation = new int[2];
                                    int[] recentsViewLocation = new int[2];
                                    if (this.mTaskView != null) {
                                        this.mTaskView.getLocationOnScreen(taskViewLocation);
                                        this.mRv.getLocationOnScreen(recentsViewLocation);
                                    }
                                    int[] iArr = recentsViewLocation;
                                    int[] iArr2 = taskViewLocation;
                                    Iterator<DropTarget> it2 = it;
                                    if (target.acceptsDrop((int) evX, this.mTaskView != null ? (taskViewLocation[1] - recentsViewLocation[1]) + RecentsImpl.mTaskBarHeight : (int) evY, width, height, false)) {
                                        currentDropTarget = target;
                                    } else {
                                        it = it2;
                                    }
                                }
                            }
                        }
                        if (this.mLastDropTarget != currentDropTarget) {
                            this.mLastDropTarget = currentDropTarget;
                            RecentsEventBus.getDefault().send(new DragDropTargetChangedEvent(this.mDragTask, currentDropTarget));
                        }
                    }
                    this.mTaskView.setTranslationX(x);
                    this.mTaskView.setTranslationY(y);
                    return;
                }
                return;
            default:
                return;
        }
    }
}
