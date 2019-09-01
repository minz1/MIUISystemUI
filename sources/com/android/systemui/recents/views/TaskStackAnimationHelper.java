package com.android.systemui.recents.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.recents.misc.ReferenceCountedTrigger;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.model.TaskStack;
import java.util.ArrayList;
import java.util.List;
import miui.view.animation.CubicEaseOutInterpolator;
import miui.view.animation.QuinticEaseOutInterpolator;

public class TaskStackAnimationHelper {
    private static final Interpolator DISMISS_ALL_TRANSLATION_INTERPOLATOR = Interpolators.EASE_IN_OUT;
    private static final Interpolator ENTER_FROM_HOME_ALPHA_INTERPOLATOR = Interpolators.LINEAR;
    private static final Interpolator ENTER_FROM_HOME_TRANSLATION_INTERPOLATOR = new QuinticEaseOutInterpolator();
    private static final Interpolator ENTER_WHILE_DOCKING_INTERPOLATOR = Interpolators.LINEAR_OUT_SLOW_IN;
    private static final Interpolator EXIT_TO_HOME_TRANSLATION_INTERPOLATOR = new CubicEaseOutInterpolator();
    /* access modifiers changed from: private */
    public static final Interpolator FOCUS_BEHIND_NEXT_TASK_INTERPOLATOR = Interpolators.LINEAR_OUT_SLOW_IN;
    private static final Interpolator FOCUS_IN_FRONT_NEXT_TASK_INTERPOLATOR = new PathInterpolator(0.0f, 0.0f, 0.0f, 1.0f);
    private static final Interpolator FOCUS_NEXT_TASK_INTERPOLATOR = new PathInterpolator(0.4f, 0.0f, 0.0f, 1.0f);
    /* access modifiers changed from: private */
    public TaskStackView mStackView;
    private ArrayList<TaskViewTransform> mTmpCurrentTaskTransforms = new ArrayList<>();
    private ArrayList<TaskViewTransform> mTmpFinalTaskTransforms = new ArrayList<>();
    private TaskViewTransform mTmpTransform = new TaskViewTransform();

    public TaskStackAnimationHelper(Context context, TaskStackView stackView) {
        this.mStackView = stackView;
    }

    /* JADX WARNING: Removed duplicated region for block: B:28:0x00b9  */
    /* JADX WARNING: Removed duplicated region for block: B:29:0x00bf  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void prepareForEnterAnimation() {
        /*
            r24 = this;
            r0 = r24
            com.android.systemui.recents.RecentsConfiguration r1 = com.android.systemui.recents.Recents.getConfiguration()
            com.android.systemui.recents.RecentsActivityLaunchState r2 = r1.getLaunchState()
            com.android.systemui.recents.views.TaskStackView r3 = r0.mStackView
            android.content.res.Resources r3 = r3.getResources()
            com.android.systemui.recents.views.TaskStackView r4 = r0.mStackView
            android.content.Context r4 = r4.getContext()
            android.content.Context r4 = r4.getApplicationContext()
            android.content.res.Resources r4 = r4.getResources()
            com.android.systemui.recents.views.TaskStackView r5 = r0.mStackView
            com.android.systemui.recents.views.TaskStackLayoutAlgorithm r5 = r5.getStackAlgorithm()
            com.android.systemui.recents.views.TaskStackView r6 = r0.mStackView
            com.android.systemui.recents.views.TaskStackViewScroller r6 = r6.getScroller()
            com.android.systemui.recents.views.TaskStackView r7 = r0.mStackView
            com.android.systemui.recents.model.TaskStack r7 = r7.getStack()
            com.android.systemui.recents.model.Task r8 = r7.getLaunchTarget()
            int r9 = r7.getTaskCount()
            if (r9 != 0) goto L_0x003b
            return
        L_0x003b:
            android.graphics.Rect r9 = r5.mStackRect
            int r9 = r9.height()
            int r10 = r5.mPaddingTop
            int r9 = r9 - r10
            r10 = 2131166293(0x7f070455, float:1.7946827E38)
            int r10 = r3.getDimensionPixelSize(r10)
            r11 = 2131166294(0x7f070456, float:1.794683E38)
            int r11 = r3.getDimensionPixelSize(r11)
            android.content.res.Configuration r12 = r4.getConfiguration()
            int r12 = r12.orientation
            r13 = 2
            r15 = 1
            if (r12 != r13) goto L_0x005e
            r12 = r15
            goto L_0x005f
        L_0x005e:
            r12 = 0
        L_0x005f:
            com.android.systemui.recents.views.TaskStackView r13 = r0.mStackView
            java.util.List r13 = r13.getTaskViews()
            int r16 = r13.size()
            int r16 = r16 + -1
        L_0x006b:
            r17 = r16
            r15 = r17
            if (r15 < 0) goto L_0x014b
            java.lang.Object r16 = r13.get(r15)
            r14 = r16
            com.android.systemui.recents.views.TaskView r14 = (com.android.systemui.recents.views.TaskView) r14
            r18 = r1
            com.android.systemui.recents.model.Task r1 = r14.getTask()
            if (r8 == 0) goto L_0x0091
            r19 = r3
            com.android.systemui.recents.model.TaskGrouping r3 = r8.group
            if (r3 == 0) goto L_0x0093
            com.android.systemui.recents.model.TaskGrouping r3 = r8.group
            boolean r3 = r3.isTaskAboveTask(r1, r8)
            if (r3 == 0) goto L_0x0093
            r3 = 1
            goto L_0x0094
        L_0x0091:
            r19 = r3
        L_0x0093:
            r3 = 0
        L_0x0094:
            if (r8 == 0) goto L_0x00a5
            boolean r16 = r8.isFreeformTask()
            if (r16 == 0) goto L_0x00a5
            boolean r16 = r1.isFreeformTask()
            if (r16 == 0) goto L_0x00a5
            r16 = 1
            goto L_0x00a7
        L_0x00a5:
            r16 = 0
        L_0x00a7:
            r20 = r4
            float r4 = r6.getStackScroll()
            r21 = r6
            com.android.systemui.recents.views.TaskViewTransform r6 = r0.mTmpTransform
            r22 = r7
            r7 = 0
            r5.getStackTransform(r1, r4, r6, r7)
            if (r16 == 0) goto L_0x00bf
            r4 = 4
            r14.setVisibility(r4)
            goto L_0x013c
        L_0x00bf:
            boolean r4 = r2.launchedViaFsGesture
            if (r4 == 0) goto L_0x00ce
            com.android.systemui.recents.views.TaskStackView r4 = r0.mStackView
            com.android.systemui.recents.views.TaskViewTransform r6 = r0.mTmpTransform
            com.android.systemui.recents.views.AnimationProps r7 = com.android.systemui.recents.views.AnimationProps.IMMEDIATE
            r4.updateTaskViewToTransform(r14, r6, r7)
            goto L_0x013c
        L_0x00ce:
            boolean r4 = r2.launchedFromApp
            r6 = 0
            if (r4 == 0) goto L_0x00fb
            boolean r4 = r2.launchedViaDockGesture
            if (r4 != 0) goto L_0x00fb
            boolean r4 = r1.isLaunchTarget
            if (r4 == 0) goto L_0x00df
            r14.onPrepareLaunchTargetForEnterAnimation()
            goto L_0x013c
        L_0x00df:
            if (r3 == 0) goto L_0x013c
            com.android.systemui.recents.views.TaskViewTransform r4 = r0.mTmpTransform
            android.graphics.RectF r4 = r4.rect
            float r7 = (float) r10
            r4.offset(r6, r7)
            com.android.systemui.recents.views.TaskViewTransform r4 = r0.mTmpTransform
            r4.alpha = r6
            com.android.systemui.recents.views.TaskStackView r4 = r0.mStackView
            com.android.systemui.recents.views.TaskViewTransform r6 = r0.mTmpTransform
            com.android.systemui.recents.views.AnimationProps r7 = com.android.systemui.recents.views.AnimationProps.IMMEDIATE
            r4.updateTaskViewToTransform(r14, r6, r7)
            r4 = 0
            r14.setClipViewInStack(r4)
            goto L_0x013c
        L_0x00fb:
            r4 = 0
            boolean r7 = r2.launchedFromHome
            if (r7 == 0) goto L_0x0116
            com.android.systemui.recents.views.TaskViewTransform r7 = r0.mTmpTransform
            android.graphics.RectF r7 = r7.rect
            float r4 = (float) r9
            r7.offset(r6, r4)
            com.android.systemui.recents.views.TaskViewTransform r4 = r0.mTmpTransform
            r4.alpha = r6
            com.android.systemui.recents.views.TaskStackView r4 = r0.mStackView
            com.android.systemui.recents.views.TaskViewTransform r6 = r0.mTmpTransform
            com.android.systemui.recents.views.AnimationProps r7 = com.android.systemui.recents.views.AnimationProps.IMMEDIATE
            r4.updateTaskViewToTransform(r14, r6, r7)
            goto L_0x013c
        L_0x0116:
            boolean r4 = r2.launchedViaDockGesture
            if (r4 == 0) goto L_0x013c
            if (r12 == 0) goto L_0x011f
            r4 = r11
            goto L_0x0125
        L_0x011f:
            float r4 = (float) r9
            r7 = 1063675494(0x3f666666, float:0.9)
            float r4 = r4 * r7
            int r4 = (int) r4
        L_0x0125:
            com.android.systemui.recents.views.TaskViewTransform r7 = r0.mTmpTransform
            android.graphics.RectF r7 = r7.rect
            r23 = r1
            float r1 = (float) r4
            r7.offset(r6, r1)
            com.android.systemui.recents.views.TaskViewTransform r1 = r0.mTmpTransform
            r1.alpha = r6
            com.android.systemui.recents.views.TaskStackView r1 = r0.mStackView
            com.android.systemui.recents.views.TaskViewTransform r6 = r0.mTmpTransform
            com.android.systemui.recents.views.AnimationProps r7 = com.android.systemui.recents.views.AnimationProps.IMMEDIATE
            r1.updateTaskViewToTransform(r14, r6, r7)
        L_0x013c:
            int r16 = r15 + -1
            r1 = r18
            r3 = r19
            r4 = r20
            r6 = r21
            r7 = r22
            r15 = 1
            goto L_0x006b
        L_0x014b:
            r18 = r1
            r19 = r3
            r20 = r4
            r21 = r6
            r22 = r7
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.recents.views.TaskStackAnimationHelper.prepareForEnterAnimation():void");
    }

    /* JADX WARNING: Code restructure failed: missing block: B:10:0x0086, code lost:
        if (r9.group.isTaskAboveTask(r4, r9) != false) goto L_0x008d;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void startEnterAnimation(com.android.systemui.recents.misc.ReferenceCountedTrigger r28) {
        /*
            r27 = this;
            r0 = r27
            r1 = r28
            com.android.systemui.recents.RecentsConfiguration r2 = com.android.systemui.recents.Recents.getConfiguration()
            com.android.systemui.recents.RecentsActivityLaunchState r3 = r2.getLaunchState()
            com.android.systemui.recents.views.TaskStackView r4 = r0.mStackView
            android.content.res.Resources r4 = r4.getResources()
            com.android.systemui.recents.views.TaskStackView r5 = r0.mStackView
            android.content.Context r5 = r5.getContext()
            android.content.Context r5 = r5.getApplicationContext()
            android.content.res.Resources r5 = r5.getResources()
            com.android.systemui.recents.views.TaskStackView r6 = r0.mStackView
            com.android.systemui.recents.views.TaskStackLayoutAlgorithm r6 = r6.getStackAlgorithm()
            com.android.systemui.recents.views.TaskStackView r7 = r0.mStackView
            com.android.systemui.recents.views.TaskStackViewScroller r7 = r7.getScroller()
            com.android.systemui.recents.views.TaskStackView r8 = r0.mStackView
            com.android.systemui.recents.model.TaskStack r8 = r8.getStack()
            com.android.systemui.recents.model.Task r9 = r8.getLaunchTarget()
            int r10 = r8.getTaskCount()
            if (r10 != 0) goto L_0x003d
            return
        L_0x003d:
            r10 = 2131427403(0x7f0b004b, float:1.8476421E38)
            int r10 = r4.getInteger(r10)
            r11 = 2131427402(0x7f0b004a, float:1.847642E38)
            int r11 = r4.getInteger(r11)
            r12 = 2131427365(0x7f0b0025, float:1.8476344E38)
            int r12 = r5.getInteger(r12)
            com.android.systemui.recents.views.TaskStackView r13 = r0.mStackView
            java.util.List r13 = r13.getTaskViews()
            int r14 = r13.size()
            int r15 = r14 + -1
        L_0x005e:
            if (r15 < 0) goto L_0x0151
            int r16 = r14 - r15
            r17 = 1
            int r16 = r16 + -1
            r18 = r15
            java.lang.Object r19 = r13.get(r15)
            r20 = r2
            r2 = r19
            com.android.systemui.recents.views.TaskView r2 = (com.android.systemui.recents.views.TaskView) r2
            r21 = r4
            com.android.systemui.recents.model.Task r4 = r2.getTask()
            if (r9 == 0) goto L_0x0089
            r22 = r5
            com.android.systemui.recents.model.TaskGrouping r5 = r9.group
            if (r5 == 0) goto L_0x008b
            com.android.systemui.recents.model.TaskGrouping r5 = r9.group
            boolean r5 = r5.isTaskAboveTask(r4, r9)
            if (r5 == 0) goto L_0x008b
            goto L_0x008d
        L_0x0089:
            r22 = r5
        L_0x008b:
            r17 = 0
        L_0x008d:
            r5 = r17
            r23 = r8
            float r8 = r7.getStackScroll()
            r24 = r7
            com.android.systemui.recents.views.TaskViewTransform r7 = r0.mTmpTransform
            r25 = r9
            r9 = 0
            r6.getStackTransform(r4, r8, r7, r9)
            boolean r7 = r3.launchedFromApp
            if (r7 == 0) goto L_0x00cf
            boolean r7 = r3.launchedViaDockGesture
            if (r7 != 0) goto L_0x00cf
            boolean r7 = r4.isLaunchTarget
            if (r7 == 0) goto L_0x00b6
            com.android.systemui.recents.views.TaskViewTransform r7 = r0.mTmpTransform
            com.android.systemui.recents.views.TaskStackView r8 = r0.mStackView
            boolean r8 = r8.mScreenPinningEnabled
            r2.onStartLaunchTargetEnterAnimation(r7, r10, r8, r1)
            goto L_0x0141
        L_0x00b6:
            if (r5 == 0) goto L_0x0141
            com.android.systemui.recents.views.AnimationProps r7 = new com.android.systemui.recents.views.AnimationProps
            android.view.animation.Interpolator r8 = com.android.systemui.Interpolators.ALPHA_IN
            com.android.systemui.recents.views.TaskStackAnimationHelper$1 r9 = new com.android.systemui.recents.views.TaskStackAnimationHelper$1
            r9.<init>(r1, r2)
            r7.<init>((int) r11, (android.view.animation.Interpolator) r8, (android.animation.Animator.AnimatorListener) r9)
            r28.increment()
            com.android.systemui.recents.views.TaskStackView r8 = r0.mStackView
            com.android.systemui.recents.views.TaskViewTransform r9 = r0.mTmpTransform
            r8.updateTaskViewToTransform(r2, r9, r7)
            goto L_0x0141
        L_0x00cf:
            boolean r7 = r3.launchedFromHome
            r8 = 6
            if (r7 == 0) goto L_0x0112
            com.android.systemui.recents.views.AnimationProps r7 = new com.android.systemui.recents.views.AnimationProps
            r7.<init>()
            r9 = 180(0xb4, float:2.52E-43)
            com.android.systemui.recents.views.AnimationProps r7 = r7.setDuration(r8, r9)
            r9 = 70
            r8 = 4
            com.android.systemui.recents.views.AnimationProps r7 = r7.setDuration(r8, r9)
            android.view.animation.Interpolator r9 = ENTER_FROM_HOME_TRANSLATION_INTERPOLATOR
            r8 = 6
            com.android.systemui.recents.views.AnimationProps r7 = r7.setInterpolator(r8, r9)
            android.view.animation.Interpolator r8 = ENTER_FROM_HOME_ALPHA_INTERPOLATOR
            r9 = 4
            com.android.systemui.recents.views.AnimationProps r7 = r7.setInterpolator(r9, r8)
            android.animation.Animator$AnimatorListener r8 = r28.decrementOnAnimationEnd()
            com.android.systemui.recents.views.AnimationProps r7 = r7.setListener(r8)
            r28.increment()
            com.android.systemui.recents.views.TaskStackView r8 = r0.mStackView
            com.android.systemui.recents.views.TaskViewTransform r9 = r0.mTmpTransform
            r8.updateTaskViewToTransform(r2, r9, r7)
            int r8 = r14 + -1
            if (r15 != r8) goto L_0x0111
            com.android.systemui.recents.views.TaskStackView r8 = r0.mStackView
            boolean r8 = r8.mScreenPinningEnabled
            r2.onStartFrontTaskEnterAnimation(r8)
        L_0x0111:
            goto L_0x0141
        L_0x0112:
            boolean r7 = r3.launchedViaDockGesture
            if (r7 == 0) goto L_0x0141
            com.android.systemui.recents.views.AnimationProps r7 = new com.android.systemui.recents.views.AnimationProps
            r7.<init>()
            int r8 = r18 * 50
            int r8 = r8 + r12
            r9 = 6
            com.android.systemui.recents.views.AnimationProps r7 = r7.setDuration(r9, r8)
            android.view.animation.Interpolator r8 = ENTER_WHILE_DOCKING_INTERPOLATOR
            com.android.systemui.recents.views.AnimationProps r7 = r7.setInterpolator(r9, r8)
            r8 = 48
            com.android.systemui.recents.views.AnimationProps r7 = r7.setStartDelay(r9, r8)
            android.animation.Animator$AnimatorListener r8 = r28.decrementOnAnimationEnd()
            com.android.systemui.recents.views.AnimationProps r7 = r7.setListener(r8)
            r28.increment()
            com.android.systemui.recents.views.TaskStackView r8 = r0.mStackView
            com.android.systemui.recents.views.TaskViewTransform r9 = r0.mTmpTransform
            r8.updateTaskViewToTransform(r2, r9, r7)
        L_0x0141:
            int r15 = r15 + -1
            r2 = r20
            r4 = r21
            r5 = r22
            r8 = r23
            r7 = r24
            r9 = r25
            goto L_0x005e
        L_0x0151:
            r20 = r2
            r21 = r4
            r22 = r5
            r24 = r7
            r23 = r8
            r25 = r9
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.recents.views.TaskStackAnimationHelper.startEnterAnimation(com.android.systemui.recents.misc.ReferenceCountedTrigger):void");
    }

    public void startExitToHomeAnimation(boolean animated, ReferenceCountedTrigger postAnimationTrigger) {
        AnimationProps taskAnimation;
        TaskStackLayoutAlgorithm stackLayout = this.mStackView.getStackAlgorithm();
        if (this.mStackView.getStack().getTaskCount() != 0) {
            int height = stackLayout.mStackRect.height();
            List<TaskView> taskViews = this.mStackView.getTaskViews();
            int taskViewCount = taskViews.size();
            for (int i = 0; i < taskViewCount; i++) {
                int taskIndexFromFront = i;
                TaskView tv = taskViews.get(i);
                if (!this.mStackView.isIgnoredTask(tv.getTask())) {
                    if (animated) {
                        int min = Math.min(5, taskIndexFromFront) * 50;
                        taskAnimation = new AnimationProps().setDuration(6, 350).setInterpolator(6, EXIT_TO_HOME_TRANSLATION_INTERPOLATOR).setDuration(4, 350).setInterpolator(4, EXIT_TO_HOME_TRANSLATION_INTERPOLATOR).setListener(postAnimationTrigger.decrementOnAnimationEnd());
                        postAnimationTrigger.increment();
                    } else {
                        taskAnimation = AnimationProps.IMMEDIATE;
                    }
                    this.mTmpTransform.fillIn(tv);
                    this.mTmpTransform.alpha = 0.0f;
                    this.mStackView.updateTaskViewToTransform(tv, this.mTmpTransform, taskAnimation);
                }
            }
        }
    }

    public void startLaunchTaskAnimation(TaskView launchingTaskView, boolean screenPinningRequested, ReferenceCountedTrigger postAnimationTrigger) {
        Resources res;
        ReferenceCountedTrigger referenceCountedTrigger = postAnimationTrigger;
        Resources res2 = this.mStackView.getResources();
        int taskViewExitToAppDuration = res2.getInteger(R.integer.recents_task_exit_to_app_duration);
        int taskViewAffiliateGroupEnterOffset = res2.getDimensionPixelSize(R.dimen.recents_task_stack_animation_affiliate_enter_offset);
        Task launchingTask = launchingTaskView.getTask();
        List<TaskView> taskViews = this.mStackView.getTaskViews();
        int taskViewCount = taskViews.size();
        boolean z = false;
        int i = 0;
        while (i < taskViewCount) {
            final TaskView tv = taskViews.get(i);
            boolean currentTaskOccludesLaunchTarget = (launchingTask == null || launchingTask.group == null || !launchingTask.group.isTaskAboveTask(tv.getTask(), launchingTask)) ? z : true;
            if (tv == launchingTaskView) {
                tv.setClipViewInStack(z);
                referenceCountedTrigger.addLastDecrementRunnable(new Runnable() {
                    public void run() {
                        tv.setClipViewInStack(true);
                    }
                });
                tv.onStartLaunchTargetLaunchAnimation(taskViewExitToAppDuration, screenPinningRequested, referenceCountedTrigger);
                res = res2;
            } else {
                boolean z2 = screenPinningRequested;
                if (currentTaskOccludesLaunchTarget) {
                    res = res2;
                    postAnimationTrigger.increment();
                    this.mTmpTransform.fillIn(tv);
                    this.mTmpTransform.alpha = 0.0f;
                    this.mTmpTransform.rect.offset(0.0f, (float) taskViewAffiliateGroupEnterOffset);
                    this.mStackView.updateTaskViewToTransform(tv, this.mTmpTransform, new AnimationProps(taskViewExitToAppDuration, Interpolators.ALPHA_OUT, postAnimationTrigger.decrementOnAnimationEnd()));
                } else {
                    res = res2;
                }
            }
            i++;
            res2 = res;
            referenceCountedTrigger = postAnimationTrigger;
            z = false;
        }
        TaskView taskView = launchingTaskView;
        boolean z3 = screenPinningRequested;
        Resources resources = res2;
    }

    public void startDeleteTaskAnimation(final TaskView deleteTaskView, final ReferenceCountedTrigger postAnimationTrigger) {
        final TaskStackViewTouchHandler touchHandler = this.mStackView.getTouchHandler();
        touchHandler.onBeginManualDrag(deleteTaskView);
        postAnimationTrigger.increment();
        postAnimationTrigger.addLastDecrementRunnable(new Runnable() {
            public void run() {
                touchHandler.onChildDismissed(deleteTaskView);
            }
        });
        final float dismissSize = touchHandler.getScaledDismissSize();
        ValueAnimator animator = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
        animator.setDuration(400);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                float progress = ((Float) animation.getAnimatedValue()).floatValue();
                deleteTaskView.setTranslationX(dismissSize * progress);
                touchHandler.updateSwipeProgress(deleteTaskView, true, progress);
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                postAnimationTrigger.decrement();
            }
        });
        animator.start();
    }

    public void startDeleteAllTasksAnimation(List<TaskView> taskViews, final ReferenceCountedTrigger postAnimationTrigger) {
        for (int i = taskViews.size() - 1; i >= 0; i--) {
            final TaskView tv = taskViews.get(i);
            int startDelay = i * 50;
            tv.setClipViewInStack(false);
            if (!tv.getTask().isProtected()) {
                AnimationProps taskAnimation = new AnimationProps(startDelay, 150, DISMISS_ALL_TRANSLATION_INTERPOLATOR, new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator animation) {
                        postAnimationTrigger.decrement();
                        tv.setClipViewInStack(true);
                    }
                });
                postAnimationTrigger.increment();
                this.mTmpTransform.fillIn(tv);
                if (this.mTmpTransform.rect.centerX() <= ((float) this.mStackView.getWidth()) / 2.0f) {
                    this.mTmpTransform.rect.offset((float) (-tv.getRight()), 0.0f);
                } else {
                    this.mTmpTransform.rect.offset((float) (this.mStackView.getWidth() - tv.getLeft()), 0.0f);
                }
                this.mStackView.updateTaskViewToTransform(tv, this.mTmpTransform, taskAnimation);
            }
        }
    }

    public boolean startScrollToFocusedTaskAnimation(Task newFocusedTask, boolean requestViewFocus) {
        float curScroll;
        TaskStack stack;
        ArrayList<Task> stackTasks;
        TaskStackViewScroller stackScroller;
        Interpolator interpolator;
        int duration;
        Task task = newFocusedTask;
        TaskStackLayoutAlgorithm stackLayout = this.mStackView.getStackAlgorithm();
        TaskStackViewScroller stackScroller2 = this.mStackView.getScroller();
        TaskStack stack2 = this.mStackView.getStack();
        float curScroll2 = stackScroller2.getStackScroll();
        final float newScroll = stackScroller2.getBoundedStackScroll(stackLayout.getTrueStackScrollForTask(task));
        int i = 0;
        boolean willScrollToFront = newScroll > curScroll2;
        boolean willScroll = Float.compare(newScroll, curScroll2) != 0;
        int taskViewCount = this.mStackView.getTaskViews().size();
        ArrayList<Task> stackTasks2 = stack2.getStackTasks();
        this.mStackView.getCurrentTaskTransforms(stackTasks2, this.mTmpCurrentTaskTransforms);
        this.mStackView.bindVisibleTaskViews(newScroll);
        stackLayout.setFocusState(1);
        stackScroller2.setStackScroll(newScroll, null);
        this.mStackView.cancelDeferredTaskViewLayoutAnimation();
        ArrayList<Task> stackTasks3 = stackTasks2;
        this.mStackView.getLayoutTaskTransforms(newScroll, stackLayout.getFocusState(), stackTasks2, true, this.mTmpFinalTaskTransforms);
        TaskView newFocusedTaskView = this.mStackView.getChildViewForTask(task);
        if (newFocusedTaskView == null) {
            Log.e("TaskStackAnimationHelper", "b/27389156 null-task-view prebind:" + taskViewCount + " postbind:" + this.mStackView.getTaskViews().size() + " prescroll:" + curScroll2 + " postscroll: " + newScroll);
            return false;
        }
        newFocusedTaskView.setFocusedState(true, requestViewFocus);
        ReferenceCountedTrigger postAnimTrigger = new ReferenceCountedTrigger();
        postAnimTrigger.addLastDecrementRunnable(new Runnable() {
            public void run() {
                TaskStackAnimationHelper.this.mStackView.bindVisibleTaskViews(newScroll);
            }
        });
        List<TaskView> taskViews = this.mStackView.getTaskViews();
        int taskViewCount2 = taskViews.size();
        int newFocusTaskViewIndex = taskViews.indexOf(newFocusedTaskView);
        while (i < taskViewCount2) {
            TaskView tv = taskViews.get(i);
            Task task2 = tv.getTask();
            TaskStackLayoutAlgorithm stackLayout2 = stackLayout;
            if (this.mStackView.isIgnoredTask(task2)) {
                stackScroller = stackScroller2;
                stack = stack2;
                curScroll = curScroll2;
                stackTasks = stackTasks3;
            } else {
                stackScroller = stackScroller2;
                ArrayList<Task> stackTasks4 = stackTasks3;
                int taskIndex = stackTasks4.indexOf(task2);
                Task task3 = task2;
                TaskViewTransform fromTransform = this.mTmpCurrentTaskTransforms.get(taskIndex);
                stackTasks = stackTasks4;
                TaskViewTransform toTransform = this.mTmpFinalTaskTransforms.get(taskIndex);
                int i2 = taskIndex;
                stack = stack2;
                this.mStackView.updateTaskViewToTransform(tv, fromTransform, AnimationProps.IMMEDIATE);
                if (willScrollToFront) {
                    duration = calculateStaggeredAnimDuration(i);
                    interpolator = FOCUS_BEHIND_NEXT_TASK_INTERPOLATOR;
                    TaskViewTransform taskViewTransform = fromTransform;
                } else if (i < newFocusTaskViewIndex) {
                    duration = 150 + (((newFocusTaskViewIndex - i) - 1) * 50);
                    interpolator = FOCUS_BEHIND_NEXT_TASK_INTERPOLATOR;
                    TaskViewTransform taskViewTransform2 = fromTransform;
                } else if (i > newFocusTaskViewIndex) {
                    TaskViewTransform taskViewTransform3 = fromTransform;
                    duration = Math.max(100, 150 - (((i - newFocusTaskViewIndex) - 1) * 50));
                    interpolator = FOCUS_IN_FRONT_NEXT_TASK_INTERPOLATOR;
                } else {
                    duration = 200;
                    interpolator = FOCUS_NEXT_TASK_INTERPOLATOR;
                }
                curScroll = curScroll2;
                AnimationProps anim = new AnimationProps().setDuration(6, duration).setInterpolator(6, interpolator).setListener(postAnimTrigger.decrementOnAnimationEnd());
                postAnimTrigger.increment();
                this.mStackView.updateTaskViewToTransform(tv, toTransform, anim);
            }
            i++;
            stackLayout = stackLayout2;
            stackScroller2 = stackScroller;
            stackTasks3 = stackTasks;
            stack2 = stack;
            curScroll2 = curScroll;
            Task task4 = newFocusedTask;
        }
        TaskStackViewScroller taskStackViewScroller = stackScroller2;
        TaskStack taskStack = stack2;
        float f = curScroll2;
        ArrayList<Task> arrayList = stackTasks3;
        return willScroll;
    }

    public void startNewStackScrollAnimation(TaskStack newStack, ReferenceCountedTrigger animationTrigger) {
        TaskView frontMostTaskView;
        Task frontMostTask;
        TaskStackViewScroller stackScroller;
        TaskStackLayoutAlgorithm stackLayout;
        TaskStack taskStack = newStack;
        TaskStackLayoutAlgorithm stackLayout2 = this.mStackView.getStackAlgorithm();
        TaskStackViewScroller stackScroller2 = this.mStackView.getScroller();
        ArrayList<Task> stackTasks = newStack.getStackTasks();
        this.mStackView.getCurrentTaskTransforms(stackTasks, this.mTmpCurrentTaskTransforms);
        int i = 0;
        this.mStackView.setTasks(taskStack, false);
        this.mStackView.updateLayoutAlgorithm(false);
        final float newScroll = stackLayout2.mInitialScrollP;
        this.mStackView.bindVisibleTaskViews(newScroll);
        stackLayout2.setFocusState(0);
        stackLayout2.setTaskOverridesForInitialState(taskStack, true);
        stackScroller2.setStackScroll(newScroll);
        this.mStackView.cancelDeferredTaskViewLayoutAnimation();
        this.mStackView.getLayoutTaskTransforms(newScroll, stackLayout2.getFocusState(), stackTasks, false, this.mTmpFinalTaskTransforms);
        Task frontMostTask2 = taskStack.getStackFrontMostTask(false);
        final TaskView frontMostTaskView2 = this.mStackView.getChildViewForTask(frontMostTask2);
        final TaskViewTransform frontMostTransform = this.mTmpFinalTaskTransforms.get(stackTasks.indexOf(frontMostTask2));
        if (frontMostTaskView2 != null) {
            this.mStackView.updateTaskViewToTransform(frontMostTaskView2, stackLayout2.getFrontOfStackTransform(), AnimationProps.IMMEDIATE);
        }
        animationTrigger.addLastDecrementRunnable(new Runnable() {
            public void run() {
                TaskStackAnimationHelper.this.mStackView.bindVisibleTaskViews(newScroll);
                if (frontMostTaskView2 != null) {
                    TaskStackAnimationHelper.this.mStackView.updateTaskViewToTransform(frontMostTaskView2, frontMostTransform, new AnimationProps(75, 250, TaskStackAnimationHelper.FOCUS_BEHIND_NEXT_TASK_INTERPOLATOR));
                }
            }
        });
        List<TaskView> taskViews = this.mStackView.getTaskViews();
        int taskViewCount = taskViews.size();
        while (i < taskViewCount) {
            TaskView tv = taskViews.get(i);
            Task task = tv.getTask();
            if (!this.mStackView.isIgnoredTask(task) && (task != frontMostTask2 || frontMostTaskView2 == null)) {
                int taskIndex = stackTasks.indexOf(task);
                TaskViewTransform fromTransform = this.mTmpCurrentTaskTransforms.get(taskIndex);
                stackLayout = stackLayout2;
                stackScroller = stackScroller2;
                frontMostTask = frontMostTask2;
                this.mStackView.updateTaskViewToTransform(tv, fromTransform, AnimationProps.IMMEDIATE);
                int duration = calculateStaggeredAnimDuration(i);
                TaskViewTransform taskViewTransform = fromTransform;
                frontMostTaskView = frontMostTaskView2;
                AnimationProps anim = new AnimationProps().setDuration(6, duration).setInterpolator(6, FOCUS_BEHIND_NEXT_TASK_INTERPOLATOR).setListener(animationTrigger.decrementOnAnimationEnd());
                animationTrigger.increment();
                this.mStackView.updateTaskViewToTransform(tv, this.mTmpFinalTaskTransforms.get(taskIndex), anim);
            } else {
                stackLayout = stackLayout2;
                stackScroller = stackScroller2;
                frontMostTask = frontMostTask2;
                frontMostTaskView = frontMostTaskView2;
            }
            i++;
            stackLayout2 = stackLayout;
            stackScroller2 = stackScroller;
            frontMostTask2 = frontMostTask;
            frontMostTaskView2 = frontMostTaskView;
            TaskStack taskStack2 = newStack;
        }
        TaskStackViewScroller taskStackViewScroller = stackScroller2;
        Task task2 = frontMostTask2;
        TaskView taskView = frontMostTaskView2;
    }

    private int calculateStaggeredAnimDuration(int i) {
        return Math.max(100, ((i - 1) * 50) + 100);
    }
}
