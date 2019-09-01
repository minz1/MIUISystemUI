package com.android.systemui.recents.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Handler;
import android.os.Vibrator;
import android.util.FloatProperty;
import android.util.Property;
import android.view.ViewDebug;
import android.widget.OverScroller;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.recents.events.RecentsEventBus;
import com.android.systemui.recents.events.activity.HideRecentsEvent;
import com.android.systemui.recents.events.activity.ScrollerFlingFinishEvent;
import com.android.systemui.recents.misc.Utilities;
import java.io.PrintWriter;

public class TaskStackViewScroller {
    private static final Property<TaskStackViewScroller, Float> STACK_SCROLL = new FloatProperty<TaskStackViewScroller>("stackScroll") {
        public void setValue(TaskStackViewScroller object, float value) {
            object.setStackScroll(value);
        }

        public Float get(TaskStackViewScroller object) {
            return Float.valueOf(object.getStackScroll());
        }
    };
    private final long VIBRATOR_DURATION = 10;
    TaskStackViewScrollerCallbacks mCb;
    Context mContext;
    float mExitRecentOverscrollThreshold = 1.0f;
    private int mExitRecentVelocityThreshold = 1200;
    float mFinalAnimatedScroll;
    float mFlingDownScrollP;
    int mFlingDownY;
    @ViewDebug.ExportedProperty(category = "recents")
    float mLastDeltaP = 0.0f;
    TaskStackLayoutAlgorithm mLayoutAlgorithm;
    ObjectAnimator mScrollAnimator;
    OverScroller mScroller;
    @ViewDebug.ExportedProperty(category = "recents")
    float mStackScrollP;
    private Vibrator mVibrator;

    public interface TaskStackViewScrollerCallbacks {
        void onStackScrollChanged(float f, float f2, AnimationProps animationProps);
    }

    public TaskStackViewScroller(Context context, TaskStackViewScrollerCallbacks cb, TaskStackLayoutAlgorithm layoutAlgorithm) {
        this.mContext = context;
        this.mCb = cb;
        this.mScroller = new OverScroller(context);
        this.mLayoutAlgorithm = layoutAlgorithm;
        this.mExitRecentOverscrollThreshold = this.mContext.getResources().getFloat(R.dimen.exit_recent_overscroll_threshold);
        this.mExitRecentVelocityThreshold = (int) (((float) this.mExitRecentVelocityThreshold) * this.mContext.getResources().getDisplayMetrics().density);
        this.mVibrator = (Vibrator) this.mContext.getSystemService("vibrator");
    }

    /* access modifiers changed from: package-private */
    public void reset() {
        this.mStackScrollP = 0.0f;
        this.mLastDeltaP = 0.0f;
    }

    /* access modifiers changed from: package-private */
    public void resetDeltaScroll() {
        this.mLastDeltaP = 0.0f;
    }

    public float getStackScroll() {
        return this.mStackScrollP;
    }

    public void setStackScroll(float s) {
        setStackScroll(s, AnimationProps.IMMEDIATE);
    }

    public float setDeltaStackScroll(float downP, float deltaP) {
        float targetScroll = downP + deltaP;
        float newScroll = this.mLayoutAlgorithm.updateFocusStateOnScroll(this.mLastDeltaP + downP, targetScroll, this.mStackScrollP);
        setStackScroll(newScroll, AnimationProps.IMMEDIATE);
        this.mLastDeltaP = deltaP;
        return newScroll - targetScroll;
    }

    public void setStackScroll(float newScroll, AnimationProps animation) {
        float prevScroll = this.mStackScrollP;
        this.mStackScrollP = newScroll;
        if (this.mCb != null) {
            this.mCb.onStackScrollChanged(prevScroll, this.mStackScrollP, animation);
        }
    }

    public boolean setStackScrollToInitialState() {
        float prevScroll = this.mStackScrollP;
        setStackScroll(this.mLayoutAlgorithm.mInitialScrollP);
        return Float.compare(prevScroll, this.mStackScrollP) != 0;
    }

    public void fling(float downScrollP, int downY, int y, int velY, int minY, int maxY, int overscroll) {
        this.mFlingDownScrollP = downScrollP;
        this.mFlingDownY = downY;
        this.mScroller.fling(0, y, 0, velY, 0, 0, minY, maxY, 0, overscroll);
        new Handler().postDelayed(new Runnable() {
            public void run() {
                RecentsEventBus.getDefault().send(new ScrollerFlingFinishEvent());
            }
        }, (long) (this.mScroller.getDuration() + 25));
    }

    public boolean boundScroll() {
        float curScroll = getStackScroll();
        float newScroll = getBoundedStackScroll(curScroll);
        if (Float.compare(newScroll, curScroll) == 0) {
            return false;
        }
        setStackScroll(newScroll);
        return true;
    }

    /* access modifiers changed from: package-private */
    public float getBoundedStackScroll(float scroll) {
        return Utilities.clamp(scroll, this.mLayoutAlgorithm.mMinScrollP, this.mLayoutAlgorithm.mMaxScrollP);
    }

    /* access modifiers changed from: package-private */
    public float getScrollAmountOutOfBounds(float scroll) {
        if (scroll < this.mLayoutAlgorithm.mMinScrollP) {
            return Math.abs(scroll - this.mLayoutAlgorithm.mMinScrollP);
        }
        if (scroll > this.mLayoutAlgorithm.mMaxScrollP) {
            return Math.abs(scroll - this.mLayoutAlgorithm.mMaxScrollP);
        }
        return 0.0f;
    }

    /* access modifiers changed from: package-private */
    public boolean isScrollOutOfBounds() {
        return Float.compare(getScrollAmountOutOfBounds(this.mStackScrollP), 0.0f) != 0;
    }

    /* access modifiers changed from: package-private */
    public ObjectAnimator animateBoundScroll(int velocity) {
        float curScroll = getStackScroll();
        float newScroll = getBoundedStackScroll(curScroll);
        if (Float.compare(newScroll, curScroll) != 0) {
            if (curScroll < (-this.mExitRecentOverscrollThreshold) || (velocity > this.mExitRecentVelocityThreshold && ((double) curScroll) < ((double) (-this.mExitRecentOverscrollThreshold)) * 0.3d)) {
                RecentsEventBus.getDefault().send(new HideRecentsEvent(false, false, false, true));
            } else {
                animateScroll(newScroll, null);
            }
        }
        return this.mScrollAnimator;
    }

    /* access modifiers changed from: package-private */
    public void animateScroll(float newScroll, Runnable postRunnable) {
        animateScroll(newScroll, this.mContext.getResources().getInteger(R.integer.recents_animate_task_stack_scroll_duration), postRunnable);
    }

    /* access modifiers changed from: package-private */
    public void animateScroll(float newScroll, int duration, final Runnable postRunnable) {
        if (this.mScrollAnimator != null && this.mScrollAnimator.isRunning()) {
            setStackScroll(this.mFinalAnimatedScroll);
            this.mScroller.forceFinished(true);
        }
        stopScroller();
        stopBoundScrollAnimation();
        if (Float.compare(this.mStackScrollP, newScroll) != 0) {
            this.mFinalAnimatedScroll = newScroll;
            this.mScrollAnimator = ObjectAnimator.ofFloat(this, STACK_SCROLL, new float[]{getStackScroll(), newScroll});
            this.mScrollAnimator.setDuration((long) duration);
            this.mScrollAnimator.setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN);
            this.mScrollAnimator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    if (postRunnable != null) {
                        postRunnable.run();
                    }
                    TaskStackViewScroller.this.mScrollAnimator.removeAllListeners();
                }
            });
            this.mScrollAnimator.start();
        } else if (postRunnable != null) {
            postRunnable.run();
        }
    }

    /* access modifiers changed from: package-private */
    public void stopBoundScrollAnimation() {
        Utilities.cancelAnimationWithoutCallbacks(this.mScrollAnimator);
    }

    /* access modifiers changed from: package-private */
    public boolean computeScroll() {
        if (!this.mScroller.computeScrollOffset()) {
            return false;
        }
        this.mFlingDownScrollP += setDeltaStackScroll(this.mFlingDownScrollP, this.mLayoutAlgorithm.getDeltaPForX(this.mFlingDownY, this.mScroller.getCurrY()));
        return true;
    }

    /* access modifiers changed from: package-private */
    public void stopScroller() {
        if (!this.mScroller.isFinished()) {
            this.mScroller.abortAnimation();
        }
    }

    public void dump(String prefix, PrintWriter writer) {
        writer.print(prefix);
        writer.print("TaskStackViewScroller");
        writer.print(" stackScroll:");
        writer.print(this.mStackScrollP);
        writer.println();
    }
}
