package com.android.systemui.recents.views;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Outline;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.FloatProperty;
import android.util.Property;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewOutlineProvider;
import android.widget.TextView;
import android.widget.Toast;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.proxy.ActivityManager;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.RecentsConfiguration;
import com.android.systemui.recents.events.RecentsEventBus;
import com.android.systemui.recents.events.activity.ClickTaskViewToLaunchTaskEvent;
import com.android.systemui.recents.events.activity.LaunchTaskEvent;
import com.android.systemui.recents.events.activity.ShowTaskMenuEvent;
import com.android.systemui.recents.events.component.UpdateLockStateEvent;
import com.android.systemui.recents.events.ui.DismissTaskViewEvent;
import com.android.systemui.recents.events.ui.TaskViewDismissedEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragEndCancelledEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragEndEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragStartEvent;
import com.android.systemui.recents.misc.ReferenceCountedTrigger;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.model.TaskStack;
import java.util.ArrayList;

public class TaskView extends FixedSizeFrameLayout implements View.OnClickListener, View.OnLongClickListener, Task.TaskCallbacks {
    public static final Property<TaskView, Float> DIM_ALPHA = new FloatProperty<TaskView>("dimAlpha") {
        public void setValue(TaskView tv, float dimAlpha) {
            tv.setDimAlpha(dimAlpha);
        }

        public Float get(TaskView tv) {
            return Float.valueOf(tv.getDimAlpha());
        }
    };
    public static final Property<TaskView, Float> DIM_ALPHA_WITHOUT_HEADER = new FloatProperty<TaskView>("dimAlphaWithoutHeader") {
        public void setValue(TaskView tv, float dimAlpha) {
            tv.setDimAlphaWithoutHeader(dimAlpha);
        }

        public Float get(TaskView tv) {
            return Float.valueOf(tv.getDimAlpha());
        }
    };
    public static final Property<TaskView, Float> VIEW_OUTLINE_ALPHA = new FloatProperty<TaskView>("viewOutlineAlpha") {
        public void setValue(TaskView tv, float alpha) {
            tv.getViewBounds().setAlpha(alpha);
        }

        public Float get(TaskView tv) {
            return Float.valueOf(tv.getViewBounds().getAlpha());
        }
    };
    private float mActionButtonTranslationZ;
    /* access modifiers changed from: private */
    public View mActionButtonView;
    private TaskViewCallbacks mCb;
    @ViewDebug.ExportedProperty(category = "recents")
    private boolean mClipViewInStack;
    @ViewDebug.ExportedProperty(category = "recents")
    private float mDimAlpha;
    private ObjectAnimator mDimAnimator;
    private Toast mDisabledAppToast;
    @ViewDebug.ExportedProperty(category = "recents")
    private Point mDownTouchPos;
    @ViewDebug.ExportedProperty(deepExport = true, prefix = "header_")
    TaskViewHeader mHeaderView;
    private View mIncompatibleAppToastView;
    @ViewDebug.ExportedProperty(category = "recents")
    private boolean mIsDisabledInSafeMode;
    private boolean mIsDragging;
    public boolean mIsScollAnimating;
    private ObjectAnimator mOutlineAnimator;
    private final TaskViewTransform mTargetAnimationTransform;
    /* access modifiers changed from: private */
    @ViewDebug.ExportedProperty(deepExport = true, prefix = "task_")
    public Task mTask;
    @ViewDebug.ExportedProperty(deepExport = true, prefix = "thumbnail_")
    TaskViewThumbnail mThumbnailView;
    private ArrayList<Animator> mTmpAnimators;
    @ViewDebug.ExportedProperty(category = "recents")
    private boolean mTouchExplorationEnabled;
    private AnimatorSet mTransformAnimation;
    @ViewDebug.ExportedProperty(deepExport = true, prefix = "view_bounds_")
    private AnimateableViewBounds mViewBounds;

    interface TaskViewCallbacks {
        void onTaskViewClipStateChanged(TaskView taskView);
    }

    public TaskView(Context context) {
        this(context, null);
    }

    public TaskView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TaskView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public TaskView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mClipViewInStack = true;
        this.mTargetAnimationTransform = new TaskViewTransform();
        this.mTmpAnimators = new ArrayList<>();
        this.mDownTouchPos = new Point();
        this.mIsScollAnimating = false;
        RecentsConfiguration configuration = Recents.getConfiguration();
        this.mViewBounds = new AnimateableViewBounds(this, context.getResources().getDimensionPixelSize(R.dimen.recents_task_view_shadow_rounded_corners_radius));
        setOutlineProvider(this.mViewBounds);
        setOnLongClickListener(this);
    }

    /* access modifiers changed from: package-private */
    public void setCallbacks(TaskViewCallbacks cb) {
        this.mCb = cb;
    }

    /* access modifiers changed from: package-private */
    public void onReload(boolean isResumingFromVisible) {
        resetNoUserInteractionState();
        if (!isResumingFromVisible) {
            resetViewProperties();
        }
    }

    public Task getTask() {
        return this.mTask;
    }

    /* access modifiers changed from: package-private */
    public AnimateableViewBounds getViewBounds() {
        return this.mViewBounds;
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        this.mHeaderView = (TaskViewHeader) findViewById(R.id.task_view_bar);
        this.mThumbnailView = (TaskViewThumbnail) findViewById(R.id.task_view_thumbnail);
        this.mThumbnailView.updateClipToTaskBar(this.mHeaderView);
        this.mActionButtonView = findViewById(R.id.lock_to_app_fab);
        this.mActionButtonView.setOutlineProvider(new ViewOutlineProvider() {
            public void getOutline(View view, Outline outline) {
                outline.setOval(0, 0, TaskView.this.mActionButtonView.getWidth(), TaskView.this.mActionButtonView.getHeight());
                outline.setAlpha(0.35f);
            }
        });
        this.mActionButtonView.setOnClickListener(this);
        this.mActionButtonTranslationZ = this.mActionButtonView.getTranslationZ();
    }

    /* access modifiers changed from: package-private */
    public void onConfigurationChanged() {
        this.mHeaderView.onConfigurationChanged();
    }

    /* access modifiers changed from: protected */
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            this.mHeaderView.onTaskViewSizeChanged(w, h);
            this.mThumbnailView.onTaskViewSizeChanged(w, h);
            this.mActionButtonView.setTranslationX((float) (w - getMeasuredWidth()));
            this.mActionButtonView.setTranslationY((float) (h - getMeasuredHeight()));
        }
    }

    public boolean hasOverlappingRendering() {
        return false;
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == 0) {
            this.mDownTouchPos.set((int) (ev.getX() * getScaleX()), (int) (ev.getY() * getScaleY()));
        }
        return super.onInterceptTouchEvent(ev);
    }

    /* access modifiers changed from: protected */
    public void measureContents(int width, int height) {
        measureChildren(View.MeasureSpec.makeMeasureSpec((width - this.mPaddingLeft) - this.mPaddingRight, 1073741824), View.MeasureSpec.makeMeasureSpec((height - this.mPaddingTop) - this.mPaddingBottom, 1073741824));
        setMeasuredDimension(width, height);
    }

    /* access modifiers changed from: package-private */
    public void updateViewPropertiesToTaskTransform(TaskViewTransform toTransform, AnimationProps toAnimation, ValueAnimator.AnimatorUpdateListener updateCallback) {
        RecentsConfiguration configuration = Recents.getConfiguration();
        cancelTransformAnimation();
        this.mTmpAnimators.clear();
        toTransform.applyToTaskView(this, this.mTmpAnimators, toAnimation, false);
        if (toAnimation.isImmediate()) {
            if (Float.compare(getDimAlpha(), toTransform.dimAlpha) != 0) {
                setDimAlpha(toTransform.dimAlpha);
            }
            if (Float.compare(this.mViewBounds.getAlpha(), toTransform.viewOutlineAlpha) != 0) {
                this.mViewBounds.setAlpha(toTransform.viewOutlineAlpha);
            }
            if (toAnimation.getListener() != null) {
                toAnimation.getListener().onAnimationEnd(null);
            }
            if (updateCallback != null) {
                updateCallback.onAnimationUpdate(null);
                return;
            }
            return;
        }
        if (Float.compare(getDimAlpha(), toTransform.dimAlpha) != 0) {
            this.mDimAnimator = ObjectAnimator.ofFloat(this, DIM_ALPHA, new float[]{getDimAlpha(), toTransform.dimAlpha});
            this.mTmpAnimators.add(toAnimation.apply(6, this.mDimAnimator));
        }
        if (Float.compare(this.mViewBounds.getAlpha(), toTransform.viewOutlineAlpha) != 0) {
            this.mOutlineAnimator = ObjectAnimator.ofFloat(this, VIEW_OUTLINE_ALPHA, new float[]{this.mViewBounds.getAlpha(), toTransform.viewOutlineAlpha});
            this.mTmpAnimators.add(toAnimation.apply(6, this.mOutlineAnimator));
        }
        if (updateCallback != null) {
            ValueAnimator updateCallbackAnim = ValueAnimator.ofInt(new int[]{0, 1});
            updateCallbackAnim.addUpdateListener(updateCallback);
            this.mTmpAnimators.add(toAnimation.apply(6, updateCallbackAnim));
        }
        this.mTransformAnimation = toAnimation.createAnimator(this.mTmpAnimators);
        this.mTransformAnimation.start();
        this.mTargetAnimationTransform.copyFrom(toTransform);
    }

    /* access modifiers changed from: package-private */
    public void resetViewProperties() {
        cancelTransformAnimation();
        setDimAlpha(0.0f);
        setVisibility(0);
        getViewBounds().reset();
        getHeaderView().reset();
        getThumbnailView().reset();
        TaskViewTransform.reset(this);
        this.mActionButtonView.setScaleX(1.0f);
        this.mActionButtonView.setScaleY(1.0f);
        this.mActionButtonView.setAlpha(0.0f);
        this.mActionButtonView.setTranslationX(0.0f);
        this.mActionButtonView.setTranslationY(0.0f);
        this.mActionButtonView.setTranslationZ(this.mActionButtonTranslationZ);
        if (this.mIncompatibleAppToastView != null) {
            this.mIncompatibleAppToastView.setVisibility(4);
        }
    }

    /* access modifiers changed from: package-private */
    public boolean isAnimatingTo(TaskViewTransform transform) {
        return this.mTransformAnimation != null && this.mTransformAnimation.isStarted() && this.mTargetAnimationTransform.isSame(transform);
    }

    public void cancelTransformAnimation() {
        Utilities.cancelAnimationWithoutCallbacks(this.mTransformAnimation);
        Utilities.cancelAnimationWithoutCallbacks(this.mDimAnimator);
        Utilities.cancelAnimationWithoutCallbacks(this.mOutlineAnimator);
    }

    /* access modifiers changed from: package-private */
    public void setTouchEnabled(boolean enabled) {
        setOnClickListener(enabled ? this : null);
    }

    /* access modifiers changed from: package-private */
    public void startNoUserInteractionAnimation() {
        this.mHeaderView.startNoUserInteractionAnimation();
    }

    /* access modifiers changed from: package-private */
    public void setNoUserInteractionState() {
        this.mHeaderView.setNoUserInteractionState();
    }

    /* access modifiers changed from: package-private */
    public void resetNoUserInteractionState() {
        this.mHeaderView.resetNoUserInteractionState();
    }

    /* access modifiers changed from: package-private */
    public void dismissTask() {
        DismissTaskViewEvent dismissEvent = new DismissTaskViewEvent(this);
        dismissEvent.addPostAnimationCallback(new Runnable() {
            public void run() {
                RecentsEventBus.getDefault().send(new TaskViewDismissedEvent(TaskView.this.mTask, this, new AnimationProps(200, Interpolators.FAST_OUT_SLOW_IN)));
            }
        });
        RecentsEventBus.getDefault().send(dismissEvent);
    }

    /* access modifiers changed from: package-private */
    public void setClipViewInStack(boolean clip) {
        if (clip != this.mClipViewInStack) {
            this.mClipViewInStack = clip;
            if (this.mCb != null) {
                this.mCb.onTaskViewClipStateChanged(this);
            }
        }
    }

    public TaskViewHeader getHeaderView() {
        return this.mHeaderView;
    }

    public TaskViewThumbnail getThumbnailView() {
        return this.mThumbnailView;
    }

    public void setDimAlpha(float dimAlpha) {
        this.mDimAlpha = dimAlpha;
        this.mThumbnailView.setDimAlpha(dimAlpha);
        this.mHeaderView.setDimAlpha(dimAlpha);
    }

    public void setDimAlphaWithoutHeader(float dimAlpha) {
        this.mDimAlpha = dimAlpha;
        this.mThumbnailView.setDimAlpha(dimAlpha);
    }

    public float getDimAlpha() {
        return this.mDimAlpha;
    }

    public void setFocusedState(boolean isFocused, boolean requestViewFocus) {
        if (isFocused) {
            if (requestViewFocus && !isFocused()) {
                requestFocus();
            }
        } else if (isAccessibilityFocused() && this.mTouchExplorationEnabled) {
            clearAccessibilityFocus();
        }
    }

    public void showActionButton(boolean fadeIn, int fadeInDuration) {
        this.mActionButtonView.setVisibility(0);
        if (!fadeIn || this.mActionButtonView.getAlpha() >= 1.0f) {
            this.mActionButtonView.setScaleX(1.0f);
            this.mActionButtonView.setScaleY(1.0f);
            this.mActionButtonView.setAlpha(1.0f);
            this.mActionButtonView.setTranslationZ(this.mActionButtonTranslationZ);
            return;
        }
        this.mActionButtonView.animate().alpha(1.0f).scaleX(1.0f).scaleY(1.0f).setDuration((long) fadeInDuration).setInterpolator(Interpolators.ALPHA_IN).start();
    }

    public void hideActionButton(boolean fadeOut, int fadeOutDuration, boolean scaleDown, final Animator.AnimatorListener animListener) {
        if (!fadeOut || this.mActionButtonView.getAlpha() <= 0.0f) {
            this.mActionButtonView.setAlpha(0.0f);
            this.mActionButtonView.setVisibility(4);
            if (animListener != null) {
                animListener.onAnimationEnd(null);
                return;
            }
            return;
        }
        if (scaleDown) {
            this.mActionButtonView.animate().scaleX(0.9f).scaleY(0.9f);
        }
        this.mActionButtonView.animate().alpha(0.0f).setDuration((long) fadeOutDuration).setInterpolator(Interpolators.ALPHA_OUT).withEndAction(new Runnable() {
            public void run() {
                if (animListener != null) {
                    animListener.onAnimationEnd(null);
                }
                TaskView.this.mActionButtonView.setVisibility(4);
            }
        }).start();
    }

    public void updateLockedFlagVisible(boolean visible) {
        updateLockedFlagVisible(visible, false, 0);
    }

    public void updateLockedFlagVisible(boolean visible, boolean withAnim, long startDelay) {
        String isLocked;
        this.mHeaderView.updateLockedFlagVisible(visible, withAnim, startDelay);
        if (visible) {
            isLocked = getContext().getString(R.string.accessibility_recent_task_locked_state);
        } else {
            isLocked = getContext().getString(R.string.accessibility_recent_task_unlocked_state);
        }
        setContentDescription(this.mTask.titleDescription + "," + isLocked);
    }

    public void onPrepareLaunchTargetForEnterAnimation() {
        setDimAlphaWithoutHeader(0.0f);
        this.mActionButtonView.setAlpha(0.0f);
        if (this.mIncompatibleAppToastView != null && this.mIncompatibleAppToastView.getVisibility() == 0) {
            this.mIncompatibleAppToastView.setAlpha(0.0f);
        }
    }

    public void onStartLaunchTargetEnterAnimation(TaskViewTransform transform, int duration, boolean screenPinningEnabled, ReferenceCountedTrigger postAnimationTrigger) {
        Utilities.cancelAnimationWithoutCallbacks(this.mDimAnimator);
        postAnimationTrigger.increment();
        this.mDimAnimator = (ObjectAnimator) new AnimationProps(duration, Interpolators.ALPHA_OUT).apply(7, ObjectAnimator.ofFloat(this, DIM_ALPHA_WITHOUT_HEADER, new float[]{getDimAlpha(), transform.dimAlpha}));
        this.mDimAnimator.addListener(postAnimationTrigger.decrementOnAnimationEnd());
        this.mDimAnimator.start();
        if (screenPinningEnabled) {
            showActionButton(true, duration);
        }
        if (this.mIncompatibleAppToastView != null && this.mIncompatibleAppToastView.getVisibility() == 0) {
            this.mIncompatibleAppToastView.animate().alpha(1.0f).setDuration((long) duration).setInterpolator(Interpolators.ALPHA_IN).start();
        }
    }

    public void onLaunchNextTask() {
        if (this.mIncompatibleAppToastView != null && this.mIncompatibleAppToastView.getVisibility() == 0) {
            this.mIncompatibleAppToastView.setAlpha(1.0f);
        }
    }

    public void onStartLaunchTargetLaunchAnimation(int duration, boolean screenPinningRequested, ReferenceCountedTrigger postAnimationTrigger) {
        Utilities.cancelAnimationWithoutCallbacks(this.mDimAnimator);
        this.mDimAnimator = (ObjectAnimator) new AnimationProps(duration, Interpolators.ALPHA_OUT).apply(7, ObjectAnimator.ofFloat(this, DIM_ALPHA, new float[]{getDimAlpha(), 0.0f}));
        this.mDimAnimator.start();
        postAnimationTrigger.increment();
        hideActionButton(true, duration, !screenPinningRequested, postAnimationTrigger.decrementOnAnimationEnd());
    }

    public void onStartFrontTaskEnterAnimation(boolean screenPinningEnabled) {
        if (screenPinningEnabled) {
            showActionButton(false, 0);
        }
    }

    public void onTaskBound(Task t, boolean touchExplorationEnabled, int displayOrientation, Rect displayRect) {
        SystemServicesProxy ssp = Recents.getSystemServices();
        this.mTouchExplorationEnabled = touchExplorationEnabled;
        this.mTask = t;
        this.mTask.addCallback(this);
        this.mIsDisabledInSafeMode = !this.mTask.isSystemApp && ssp.isInSafeMode();
        this.mThumbnailView.bindToTask(this.mTask, this.mIsDisabledInSafeMode, displayOrientation, displayRect);
        this.mHeaderView.bindToTask(this.mTask, this.mTouchExplorationEnabled, this.mIsDisabledInSafeMode);
        if (!t.isDockable && ssp.hasDockedTask()) {
            if (this.mIncompatibleAppToastView == null) {
                this.mIncompatibleAppToastView = Utilities.findViewStubById((View) this, (int) R.id.incompatible_app_toast_stub).inflate();
                ((TextView) findViewById(16908299)).setText(R.string.recents_incompatible_app_message);
            }
            this.mIncompatibleAppToastView.setVisibility(0);
        } else if (this.mIncompatibleAppToastView != null) {
            this.mIncompatibleAppToastView.setVisibility(4);
        }
        updateLockedFlagVisible(this.mTask.isLocked);
    }

    public void onTaskDataLoaded(Task task, ActivityManager.TaskThumbnailInfo thumbnailInfo) {
        this.mThumbnailView.onTaskDataLoaded(thumbnailInfo);
        this.mHeaderView.onTaskDataLoaded();
    }

    public void onTaskDataUnloaded() {
        this.mTask.removeCallback(this);
        this.mThumbnailView.unbindFromTask();
        this.mHeaderView.unbindFromTask(this.mTouchExplorationEnabled);
    }

    public void onTaskStackIdChanged() {
        this.mHeaderView.bindToTask(this.mTask, this.mTouchExplorationEnabled, this.mIsDisabledInSafeMode);
        this.mHeaderView.onTaskDataLoaded();
    }

    public void onClick(View v) {
        if (this.mIsDisabledInSafeMode) {
            Context context = getContext();
            String msg = context.getString(R.string.recents_launch_disabled_message, new Object[]{this.mTask.title});
            if (this.mDisabledAppToast != null) {
                this.mDisabledAppToast.cancel();
            }
            this.mDisabledAppToast = Toast.makeText(context, msg, 0);
            this.mDisabledAppToast.show();
            return;
        }
        boolean screenPinningRequested = false;
        if (v == this.mActionButtonView) {
            this.mActionButtonView.setTranslationZ(0.0f);
            screenPinningRequested = true;
        }
        RecentsEventBus recentsEventBus = RecentsEventBus.getDefault();
        LaunchTaskEvent launchTaskEvent = new LaunchTaskEvent(this, this.mTask, null, -1, screenPinningRequested);
        recentsEventBus.send(launchTaskEvent);
        RecentsEventBus.getDefault().send(new ClickTaskViewToLaunchTaskEvent(this.mTask));
        MetricsLogger.action(v.getContext(), 277, this.mTask.key.getComponent().toString());
    }

    public boolean onLongClick(View v) {
        if (RecentsConfiguration.sCanMultiWindow) {
            return startDrag();
        }
        RecentsEventBus.getDefault().send(new ShowTaskMenuEvent(this));
        return true;
    }

    public boolean startDrag() {
        boolean inBounds;
        if (this.mIsDragging || !waitForDragToEnterMultiWindowMode()) {
            return false;
        }
        SystemServicesProxy ssp = Recents.getSystemServices();
        Rect clipBounds = new Rect(this.mViewBounds.mClipBounds);
        if (!clipBounds.isEmpty()) {
            clipBounds.scale(getScaleX());
            inBounds = clipBounds.contains(this.mDownTouchPos.x, this.mDownTouchPos.y);
        } else {
            inBounds = this.mDownTouchPos.x <= getWidth() && this.mDownTouchPos.y <= getHeight();
        }
        if (!inBounds || ssp.hasDockedTask()) {
            return false;
        }
        setTranslationZ(10.0f);
        setClipViewInStack(false);
        Point point = this.mDownTouchPos;
        point.x = (int) (((float) point.x) + (((1.0f - getScaleX()) * ((float) getWidth())) / 2.0f));
        Point point2 = this.mDownTouchPos;
        point2.y = (int) (((float) point2.y) + (((1.0f - getScaleY()) * ((float) getHeight())) / 2.0f));
        RecentsEventBus.getDefault().register(this, 3);
        RecentsEventBus.getDefault().send(new DragStartEvent(this.mTask, this, this.mDownTouchPos));
        return true;
    }

    public final void onBusEvent(DragStartEvent event) {
        this.mIsDragging = true;
    }

    public final void onBusEvent(DragEndEvent event) {
        this.mIsDragging = false;
        postDelayed(new Runnable() {
            public void run() {
                TaskView.this.setTranslationZ(0.0f);
            }
        }, 250);
        if (!(event.dropTarget instanceof TaskStack.DockState)) {
            event.addPostAnimationCallback(new Runnable() {
                public void run() {
                    TaskView.this.setClipViewInStack(true);
                }
            });
        }
    }

    public final void onBusEvent(DragEndCancelledEvent event) {
        event.addPostAnimationCallback(new Runnable() {
            public void run() {
                TaskView.this.setClipViewInStack(true);
            }
        });
    }

    public final void onBusEvent(UpdateLockStateEvent event) {
        updateLockedFlagVisible(this.mTask.isLocked);
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        RecentsEventBus.getDefault().register(this, 3);
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        RecentsEventBus.getDefault().unregister(this);
    }

    public static boolean waitForDragToEnterMultiWindowMode() {
        return RecentsConfiguration.sCanMultiWindow;
    }

    public void setIsScollAnimating(boolean isAnimating) {
        this.mIsScollAnimating = isAnimating;
    }
}
