package com.android.systemui.pip.phone;

import android.animation.AnimationHandler;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.RectEvaluator;
import android.animation.ValueAnimator;
import android.app.ActivityManager;
import android.app.IActivityManager;
import android.content.Context;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.animation.Interpolator;
import com.android.internal.graphics.SfVsyncFrameCallbackProvider;
import com.android.internal.os.SomeArgs;
import com.android.internal.policy.PipSnapAlgorithm;
import com.android.systemui.Interpolators;
import com.android.systemui.recents.misc.ForegroundThread;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.statusbar.FlingAnimationUtils;
import java.io.PrintWriter;

public class PipMotionHelper implements Handler.Callback {
    private static final RectEvaluator RECT_EVALUATOR = new RectEvaluator(new Rect());
    private IActivityManager mActivityManager;
    /* access modifiers changed from: private */
    public AnimationHandler mAnimationHandler;
    private final Rect mBounds = new Rect();
    private ValueAnimator mBoundsAnimator = null;
    private Context mContext;
    private FlingAnimationUtils mFlingAnimationUtils;
    private Handler mHandler;
    private PipMenuActivityController mMenuController;
    private PipSnapAlgorithm mSnapAlgorithm;
    private final Rect mStableInsets = new Rect();

    public PipMotionHelper(Context context, IActivityManager activityManager, PipMenuActivityController menuController, PipSnapAlgorithm snapAlgorithm, FlingAnimationUtils flingAnimationUtils) {
        this.mContext = context;
        this.mHandler = new Handler(ForegroundThread.get().getLooper(), this);
        this.mActivityManager = activityManager;
        this.mMenuController = menuController;
        this.mSnapAlgorithm = snapAlgorithm;
        this.mFlingAnimationUtils = flingAnimationUtils;
        this.mAnimationHandler = new AnimationHandler();
        this.mAnimationHandler.setProvider(new SfVsyncFrameCallbackProvider());
        onConfigurationChanged();
    }

    /* access modifiers changed from: package-private */
    public void onConfigurationChanged() {
        this.mSnapAlgorithm.onConfigurationChanged();
        SystemServicesProxy.getInstance(this.mContext).getStableInsets(this.mStableInsets);
    }

    /* access modifiers changed from: package-private */
    public void synchronizePinnedStackBounds() {
        cancelAnimations();
        try {
            ActivityManager.StackInfo stackInfo = this.mActivityManager.getStackInfo(2, 0);
            if (stackInfo != null) {
                this.mBounds.set(stackInfo.bounds);
            }
        } catch (Exception e) {
            Log.w("PipMotionHelper", "Failed to get pinned stack bounds", e);
        }
    }

    /* access modifiers changed from: package-private */
    public void movePip(Rect toBounds) {
        cancelAnimations();
        resizePipUnchecked(toBounds);
        this.mBounds.set(toBounds);
    }

    /* access modifiers changed from: package-private */
    public void expandPip() {
        expandPip(false);
    }

    /* access modifiers changed from: package-private */
    public void expandPip(boolean skipAnimation) {
        cancelAnimations();
        this.mMenuController.hideMenuWithoutResize();
        this.mHandler.post(new Runnable(skipAnimation) {
            private final /* synthetic */ boolean f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                PipMotionHelper.lambda$expandPip$0(PipMotionHelper.this, this.f$1);
            }
        });
    }

    public static /* synthetic */ void lambda$expandPip$0(PipMotionHelper pipMotionHelper, boolean skipAnimation) {
        try {
            pipMotionHelper.mActivityManager.dismissPip(!skipAnimation, 300);
        } catch (RemoteException e) {
            Log.e("PipMotionHelper", "Error expanding PiP activity", e);
        }
    }

    /* access modifiers changed from: package-private */
    public void dismissPip() {
        cancelAnimations();
        this.mMenuController.hideMenuWithoutResize();
        this.mHandler.post(new Runnable() {
            public final void run() {
                PipMotionHelper.lambda$dismissPip$1(PipMotionHelper.this);
            }
        });
    }

    public static /* synthetic */ void lambda$dismissPip$1(PipMotionHelper pipMotionHelper) {
        try {
            pipMotionHelper.mActivityManager.removeStacksInWindowingModes(new int[]{2});
        } catch (RemoteException e) {
            Log.e("PipMotionHelper", "Failed to remove PiP", e);
        }
    }

    /* access modifiers changed from: package-private */
    public Rect getBounds() {
        return this.mBounds;
    }

    /* access modifiers changed from: package-private */
    public Rect getClosestMinimizedBounds(Rect stackBounds, Rect movementBounds) {
        Point displaySize = new Point();
        this.mContext.getDisplay().getRealSize(displaySize);
        Rect toBounds = this.mSnapAlgorithm.findClosestSnapBounds(movementBounds, stackBounds);
        this.mSnapAlgorithm.applyMinimizedOffset(toBounds, movementBounds, displaySize, this.mStableInsets);
        return toBounds;
    }

    /* access modifiers changed from: package-private */
    public boolean shouldDismissPip() {
        Point displaySize = new Point();
        this.mContext.getDisplay().getSize(displaySize);
        boolean z = false;
        if (this.mBounds.bottom <= displaySize.y) {
            return false;
        }
        if (((float) (this.mBounds.bottom - displaySize.y)) / ((float) this.mBounds.height()) >= 0.3f) {
            z = true;
        }
        return z;
    }

    /* access modifiers changed from: package-private */
    public Rect animateToClosestMinimizedState(Rect movementBounds, ValueAnimator.AnimatorUpdateListener updateListener) {
        cancelAnimations();
        Rect toBounds = getClosestMinimizedBounds(this.mBounds, movementBounds);
        if (!this.mBounds.equals(toBounds)) {
            this.mBoundsAnimator = createAnimationToBounds(this.mBounds, toBounds, 200, Interpolators.LINEAR_OUT_SLOW_IN);
            if (updateListener != null) {
                this.mBoundsAnimator.addUpdateListener(updateListener);
            }
            this.mBoundsAnimator.start();
        }
        return toBounds;
    }

    /* access modifiers changed from: package-private */
    public Rect flingToSnapTarget(float velocity, float velocityX, float velocityY, Rect movementBounds, ValueAnimator.AnimatorUpdateListener updateListener, Animator.AnimatorListener listener) {
        cancelAnimations();
        Rect toBounds = this.mSnapAlgorithm.findClosestSnapBounds(movementBounds, this.mBounds);
        if (!this.mBounds.equals(toBounds)) {
            this.mBoundsAnimator = createAnimationToBounds(this.mBounds, toBounds, 0, Interpolators.FAST_OUT_SLOW_IN);
            this.mFlingAnimationUtils.apply((Animator) this.mBoundsAnimator, 0.0f, distanceBetweenRectOffsets(this.mBounds, toBounds), velocity);
            if (updateListener != null) {
                this.mBoundsAnimator.addUpdateListener(updateListener);
            }
            if (listener != null) {
                this.mBoundsAnimator.addListener(listener);
            }
            this.mBoundsAnimator.start();
        }
        return toBounds;
    }

    /* access modifiers changed from: package-private */
    public Rect animateToClosestSnapTarget(Rect movementBounds, ValueAnimator.AnimatorUpdateListener updateListener, Animator.AnimatorListener listener) {
        cancelAnimations();
        Rect toBounds = this.mSnapAlgorithm.findClosestSnapBounds(movementBounds, this.mBounds);
        if (!this.mBounds.equals(toBounds)) {
            this.mBoundsAnimator = createAnimationToBounds(this.mBounds, toBounds, 225, Interpolators.FAST_OUT_SLOW_IN);
            if (updateListener != null) {
                this.mBoundsAnimator.addUpdateListener(updateListener);
            }
            if (listener != null) {
                this.mBoundsAnimator.addListener(listener);
            }
            this.mBoundsAnimator.start();
        }
        return toBounds;
    }

    /* access modifiers changed from: package-private */
    public float animateToExpandedState(Rect expandedBounds, Rect movementBounds, Rect expandedMovementBounds) {
        float savedSnapFraction = this.mSnapAlgorithm.getSnapFraction(new Rect(this.mBounds), movementBounds);
        this.mSnapAlgorithm.applySnapFraction(expandedBounds, expandedMovementBounds, savedSnapFraction);
        resizeAndAnimatePipUnchecked(expandedBounds, 250);
        return savedSnapFraction;
    }

    /* access modifiers changed from: package-private */
    public void animateToUnexpandedState(Rect normalBounds, float savedSnapFraction, Rect normalMovementBounds, Rect currentMovementBounds, boolean minimized, boolean immediate) {
        if (savedSnapFraction < 0.0f) {
            savedSnapFraction = this.mSnapAlgorithm.getSnapFraction(new Rect(this.mBounds), currentMovementBounds);
        }
        this.mSnapAlgorithm.applySnapFraction(normalBounds, normalMovementBounds, savedSnapFraction);
        if (minimized) {
            normalBounds = getClosestMinimizedBounds(normalBounds, normalMovementBounds);
        }
        if (immediate) {
            movePip(normalBounds);
        } else {
            resizeAndAnimatePipUnchecked(normalBounds, 250);
        }
    }

    /* access modifiers changed from: package-private */
    public void animateToIMEOffset(Rect toBounds) {
        cancelAnimations();
        resizeAndAnimatePipUnchecked(toBounds, 300);
    }

    /* access modifiers changed from: package-private */
    public Rect animateDismiss(Rect pipBounds, float velocityX, float velocityY, ValueAnimator.AnimatorUpdateListener listener) {
        cancelAnimations();
        float velocity = PointF.length(velocityX, velocityY);
        boolean isFling = velocity > this.mFlingAnimationUtils.getMinVelocityPxPerSecond();
        Point p = getDismissEndPoint(pipBounds, velocityX, velocityY, isFling);
        Rect toBounds = new Rect(pipBounds);
        toBounds.offsetTo(p.x, p.y);
        this.mBoundsAnimator = createAnimationToBounds(this.mBounds, toBounds, 175, Interpolators.FAST_OUT_LINEAR_IN);
        this.mBoundsAnimator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                PipMotionHelper.this.dismissPip();
            }
        });
        if (isFling) {
            this.mFlingAnimationUtils.apply((Animator) this.mBoundsAnimator, 0.0f, distanceBetweenRectOffsets(this.mBounds, toBounds), velocity);
        }
        if (listener != null) {
            this.mBoundsAnimator.addUpdateListener(listener);
        }
        this.mBoundsAnimator.start();
        return toBounds;
    }

    /* access modifiers changed from: package-private */
    public void cancelAnimations() {
        if (this.mBoundsAnimator != null) {
            this.mBoundsAnimator.cancel();
            this.mBoundsAnimator = null;
        }
    }

    private ValueAnimator createAnimationToBounds(Rect fromBounds, Rect toBounds, int duration, Interpolator interpolator) {
        ValueAnimator anim = new ValueAnimator() {
            public AnimationHandler getAnimationHandler() {
                return PipMotionHelper.this.mAnimationHandler;
            }
        };
        anim.setObjectValues(new Object[]{fromBounds, toBounds});
        anim.setEvaluator(RECT_EVALUATOR);
        anim.setDuration((long) duration);
        anim.setInterpolator(interpolator);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                PipMotionHelper.this.resizePipUnchecked((Rect) valueAnimator.getAnimatedValue());
            }
        });
        return anim;
    }

    /* access modifiers changed from: private */
    public void resizePipUnchecked(Rect toBounds) {
        if (!toBounds.equals(this.mBounds)) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = toBounds;
            this.mHandler.sendMessage(this.mHandler.obtainMessage(1, args));
        }
    }

    private void resizeAndAnimatePipUnchecked(Rect toBounds, int duration) {
        if (!toBounds.equals(this.mBounds)) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = toBounds;
            args.argi1 = duration;
            this.mHandler.sendMessage(this.mHandler.obtainMessage(2, args));
        }
    }

    private Point getDismissEndPoint(Rect pipBounds, float velX, float velY, boolean isFling) {
        Point displaySize = new Point();
        this.mContext.getDisplay().getRealSize(displaySize);
        float bottomBound = ((float) displaySize.y) + (((float) pipBounds.height()) * 0.1f);
        if (!isFling || velX == 0.0f || velY == 0.0f) {
            return new Point(pipBounds.left, (int) bottomBound);
        }
        float slope = velY / velX;
        return new Point((int) ((bottomBound - (((float) pipBounds.top) - (((float) pipBounds.left) * slope))) / slope), (int) bottomBound);
    }

    private float distanceBetweenRectOffsets(Rect r1, Rect r2) {
        return PointF.length((float) (r1.left - r2.left), (float) (r1.top - r2.top));
    }

    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case 1:
                Rect toBounds = (Rect) ((SomeArgs) msg.obj).arg1;
                try {
                    this.mActivityManager.resizePinnedStack(toBounds, null);
                    this.mBounds.set(toBounds);
                } catch (RemoteException e) {
                    Log.e("PipMotionHelper", "Could not resize pinned stack to bounds: " + toBounds, e);
                }
                return true;
            case 2:
                SomeArgs args = (SomeArgs) msg.obj;
                Rect toBounds2 = (Rect) args.arg1;
                int duration = args.argi1;
                try {
                    ActivityManager.StackInfo stackInfo = this.mActivityManager.getStackInfo(2, 0);
                    if (stackInfo == null) {
                        return true;
                    }
                    this.mActivityManager.resizeStack(stackInfo.stackId, toBounds2, false, true, true, duration);
                    this.mBounds.set(toBounds2);
                    return true;
                } catch (Exception e2) {
                    Log.e("PipMotionHelper", "Could not animate resize pinned stack to bounds: " + toBounds2, e2);
                }
            default:
                return false;
        }
    }

    public void dump(PrintWriter pw, String prefix) {
        String innerPrefix = prefix + "  ";
        pw.println(prefix + "PipMotionHelper");
        pw.println(innerPrefix + "mBounds=" + this.mBounds);
        pw.println(innerPrefix + "mStableInsets=" + this.mStableInsets);
    }
}
