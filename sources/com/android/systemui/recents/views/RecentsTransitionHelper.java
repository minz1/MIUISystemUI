package com.android.systemui.recents.views;

import android.app.ActivityOptions;
import android.app.ActivityOptionsCompat;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.GraphicBuffer;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.IRemoteCallback;
import android.os.RemoteException;
import android.util.Log;
import android.view.AppTransitionAnimationSpec;
import android.view.DisplayListCanvas;
import android.view.IAppTransitionAnimationSpecsFuture;
import android.view.RenderNode;
import android.view.ThreadedRenderer;
import android.view.View;
import com.android.internal.annotations.GuardedBy;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.RecentsImpl;
import com.android.systemui.recents.events.RecentsEventBus;
import com.android.systemui.recents.events.activity.CancelEnterRecentsWindowAnimationEvent;
import com.android.systemui.recents.events.activity.ExitRecentsWindowFirstAnimationFrameEvent;
import com.android.systemui.recents.events.activity.LaunchTaskFailedEvent;
import com.android.systemui.recents.events.activity.LaunchTaskStartedEvent;
import com.android.systemui.recents.events.activity.LaunchTaskSucceededEvent;
import com.android.systemui.recents.events.component.ScreenPinningRequestEvent;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.model.TaskStack;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecentsTransitionHelper {
    private static final List<AppTransitionAnimationSpec> SPECS_WAITING = new ArrayList();
    @GuardedBy("this")
    private List<AppTransitionAnimationSpec> mAppTransitionAnimationSpecs = SPECS_WAITING;
    /* access modifiers changed from: private */
    public Context mContext;
    /* access modifiers changed from: private */
    public Handler mHandler;
    /* access modifiers changed from: private */
    public StartScreenPinningRunnableRunnable mStartScreenPinningRunnable = new StartScreenPinningRunnableRunnable();
    private TaskViewTransform mTmpTransform = new TaskViewTransform();

    public interface AnimationSpecComposer {
        List<AppTransitionAnimationSpec> composeSpecs();
    }

    private class StartScreenPinningRunnableRunnable implements Runnable {
        /* access modifiers changed from: private */
        public int taskId;

        private StartScreenPinningRunnableRunnable() {
            this.taskId = -1;
        }

        public void run() {
            RecentsEventBus.getDefault().send(new ScreenPinningRequestEvent(RecentsTransitionHelper.this.mContext, this.taskId));
        }
    }

    public RecentsTransitionHelper(Context context) {
        this.mContext = context.getApplicationContext();
        this.mHandler = new Handler();
    }

    public void launchTaskFromRecents(TaskStack stack, Task task, TaskStackView stackView, TaskView taskView, boolean screenPinningRequested, Rect bounds, int destinationStack) {
        IAppTransitionAnimationSpecsFuture transitionFuture;
        ActivityOptions.OnAnimationStartedListener animStartedListener;
        final Task task2 = task;
        final TaskStackView taskStackView = stackView;
        TaskView taskView2 = taskView;
        final boolean z = screenPinningRequested;
        ActivityOptions opts = ActivityOptions.makeBasic();
        if (bounds != null) {
            ActivityOptionsCompat.setOptionsLaunchBounds(opts, bounds.isEmpty() ? null : bounds);
        }
        if (taskView2 != null) {
            final Rect windowRect = Recents.getSystemServices().getWindowRect();
            IAppTransitionAnimationSpecsFuture transitionFuture2 = getAppTransitionFuture(new AnimationSpecComposer() {
                public List<AppTransitionAnimationSpec> composeSpecs() {
                    return RecentsTransitionHelper.this.composeAnimationSpecs(task2, taskStackView, 0, 0, windowRect);
                }
            }, stackView.getHandler());
            animStartedListener = new ActivityOptions.OnAnimationStartedListener() {
                public void onAnimationStarted() {
                    RecentsEventBus.getDefault().send(new CancelEnterRecentsWindowAnimationEvent(task2));
                    RecentsEventBus.getDefault().send(new ExitRecentsWindowFirstAnimationFrameEvent());
                    taskStackView.cancelAllTaskViewAnimations();
                    if (z) {
                        int unused = RecentsTransitionHelper.this.mStartScreenPinningRunnable.taskId = task2.key.id;
                        RecentsTransitionHelper.this.mHandler.postDelayed(RecentsTransitionHelper.this.mStartScreenPinningRunnable, 350);
                    }
                }
            };
            transitionFuture = transitionFuture2;
        } else {
            transitionFuture = null;
            animStartedListener = new ActivityOptions.OnAnimationStartedListener() {
                public void onAnimationStarted() {
                    RecentsEventBus.getDefault().send(new CancelEnterRecentsWindowAnimationEvent(task2));
                    RecentsEventBus.getDefault().send(new ExitRecentsWindowFirstAnimationFrameEvent());
                    taskStackView.cancelAllTaskViewAnimations();
                }
            };
        }
        if (taskView2 == null) {
            startTaskActivity(stack, task2, taskView2, opts, transitionFuture, animStartedListener);
            ActivityOptions activityOptions = opts;
            return;
        }
        LaunchTaskStartedEvent launchStartedEvent = new LaunchTaskStartedEvent(taskView2, z);
        if (task2.group == null || task2.group.isFrontMostTask(task2)) {
            RecentsEventBus.getDefault().send(launchStartedEvent);
            startTaskActivity(stack, task, taskView, opts, transitionFuture, animStartedListener);
            return;
        }
        final TaskStack taskStack = stack;
        ActivityOptions opts2 = opts;
        final Task task3 = task2;
        final TaskView taskView3 = taskView2;
        final ActivityOptions activityOptions2 = opts2;
        final IAppTransitionAnimationSpecsFuture iAppTransitionAnimationSpecsFuture = transitionFuture;
        final ActivityOptions.OnAnimationStartedListener onAnimationStartedListener = animStartedListener;
        AnonymousClass4 r7 = new Runnable() {
            public void run() {
                RecentsTransitionHelper.this.startTaskActivity(taskStack, task3, taskView3, activityOptions2, iAppTransitionAnimationSpecsFuture, onAnimationStartedListener);
            }
        };
        launchStartedEvent.addPostAnimationCallback(r7);
        RecentsEventBus.getDefault().send(launchStartedEvent);
    }

    public IRemoteCallback wrapStartedListener(final ActivityOptions.OnAnimationStartedListener listener) {
        if (listener == null) {
            return null;
        }
        return new IRemoteCallback.Stub() {
            public void sendResult(Bundle data) throws RemoteException {
                RecentsTransitionHelper.this.mHandler.post(new Runnable() {
                    public void run() {
                        listener.onAnimationStarted();
                    }
                });
            }
        };
    }

    /* access modifiers changed from: private */
    public void startTaskActivity(TaskStack stack, Task task, TaskView taskView, ActivityOptions opts, IAppTransitionAnimationSpecsFuture transitionFuture, ActivityOptions.OnAnimationStartedListener animStartedListener) {
        SystemServicesProxy ssp = Recents.getSystemServices();
        if (ssp.startActivityFromRecents(this.mContext, task.key, task.title, opts)) {
            int taskIndexFromFront = 0;
            int taskIndex = stack.indexOfStackTask(task);
            if (taskIndex > -1) {
                taskIndexFromFront = (stack.getTaskCount() - taskIndex) - 1;
            }
            RecentsEventBus.getDefault().send(new LaunchTaskSucceededEvent(taskIndexFromFront));
        } else {
            if (taskView != null) {
                taskView.dismissTask();
            }
            RecentsEventBus.getDefault().send(new LaunchTaskFailedEvent());
        }
        if (transitionFuture != null) {
            ssp.overridePendingAppTransitionMultiThumbFuture(transitionFuture, wrapStartedListener(animStartedListener), true);
        }
    }

    public IAppTransitionAnimationSpecsFuture getAppTransitionFuture(final AnimationSpecComposer composer, Handler handler) {
        if (composer == null || handler == null) {
            return null;
        }
        return new AppTransitionAnimationSpecsFuture(handler) {
            public List<AppTransitionAnimationSpec> composeSpecs() {
                return composer.composeSpecs();
            }
        }.getFuture();
    }

    public List<AppTransitionAnimationSpec> composeDockAnimationSpec(TaskView taskView, Rect bounds) {
        this.mTmpTransform.fillIn(taskView);
        Task task = taskView.getTask();
        return Collections.singletonList(new AppTransitionAnimationSpec(task.key.id, composeTaskBitmap(taskView, this.mTmpTransform), bounds));
    }

    public List<AppTransitionAnimationSpec> composeAnimationSpecs(Task task, TaskStackView stackView, int windowingMode, int activityType, Rect windowRect) {
        TaskView taskView = stackView.getChildViewForTask(task);
        TaskStackLayoutAlgorithm stackLayout = stackView.getStackAlgorithm();
        Rect offscreenTaskRect = new Rect();
        stackLayout.getFrontOfStackTransform().rect.round(offscreenTaskRect);
        if (windowingMode != 1 && windowingMode != 3 && windowingMode != 4 && activityType != 4 && windowingMode != 0) {
            return Collections.emptyList();
        }
        List<AppTransitionAnimationSpec> specs = new ArrayList<>();
        if (taskView == null) {
            specs.add(composeOffscreenAnimationSpec(task, offscreenTaskRect));
        } else {
            this.mTmpTransform.fillIn(taskView);
            stackLayout.transformToScreenCoordinates(this.mTmpTransform, windowRect);
            AppTransitionAnimationSpec spec = composeAnimationSpec(stackView, taskView, this.mTmpTransform, true);
            if (spec != null) {
                specs.add(spec);
            }
        }
        return specs;
    }

    private static AppTransitionAnimationSpec composeOffscreenAnimationSpec(Task task, Rect taskRect) {
        return new AppTransitionAnimationSpec(task.key.id, null, taskRect);
    }

    public static GraphicBuffer composeTaskBitmap(TaskView taskView, TaskViewTransform transform) {
        float scale = transform.scale;
        int fromWidth = (int) (transform.rect.width() * scale);
        int fromHeight = (int) (transform.rect.height() * scale);
        if (fromWidth != 0 && fromHeight != 0) {
            return drawViewIntoGraphicBuffer(fromWidth, fromHeight, null, 1.0f, 0);
        }
        Log.e("RecentsTransitionHelper", "Could not compose thumbnail for task: " + taskView.getTask() + " at transform: " + transform);
        return drawViewIntoGraphicBuffer(1, 1, null, 1.0f, 16777215);
    }

    private static GraphicBuffer composeHeaderBitmap(TaskView taskView, TaskViewTransform transform) {
        float scale = transform.scale;
        int headerWidth = (int) transform.rect.width();
        int headerHeight = (int) (((float) taskView.mHeaderView.getMeasuredHeight()) * scale);
        if (headerWidth == 0 || headerHeight == 0) {
            return null;
        }
        return drawViewIntoGraphicBuffer(headerWidth, headerHeight, null, 1.0f, 0);
    }

    public static GraphicBuffer drawViewIntoGraphicBuffer(int bufferWidth, int bufferHeight, View view, float scale, int eraseColor) {
        RenderNode node = RenderNode.create("RecentsTransition", null);
        node.setLeftTopRightBottom(0, 0, bufferWidth, bufferHeight);
        node.setClipToBounds(false);
        DisplayListCanvas c = node.start(bufferWidth, bufferHeight);
        c.scale(scale, scale);
        if (eraseColor != 0) {
            c.drawColor(eraseColor);
        }
        if (view != null) {
            view.draw(c);
        }
        node.end(c);
        Bitmap hardwareBitmap = ThreadedRenderer.createHardwareBitmap(node, bufferWidth, bufferHeight);
        if (hardwareBitmap == null) {
            return null;
        }
        return hardwareBitmap.createGraphicBufferHandle();
    }

    private static AppTransitionAnimationSpec composeAnimationSpec(TaskStackView stackView, TaskView taskView, TaskViewTransform transform, boolean addHeaderBitmap) {
        GraphicBuffer b = null;
        if (addHeaderBitmap) {
            b = composeHeaderBitmap(taskView, transform);
            if (b == null) {
                return null;
            }
        }
        Rect taskRect = new Rect();
        transform.rect.round(taskRect);
        taskRect.top += RecentsImpl.mTaskBarHeight;
        return new AppTransitionAnimationSpec(taskView.getTask().key.id, b, taskRect);
    }
}
