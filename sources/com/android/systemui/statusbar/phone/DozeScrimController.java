package com.android.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.animation.Interpolator;
import com.android.systemui.Interpolators;
import com.android.systemui.doze.DozeHost;
import com.android.systemui.doze.DozeLog;

public class DozeScrimController {
    /* access modifiers changed from: private */
    public static final boolean DEBUG = Log.isLoggable("DozeScrimController", 3);
    private Animator mBehindAnimator;
    private float mBehindTarget;
    private final Context mContext;
    /* access modifiers changed from: private */
    public final DozeParameters mDozeParameters;
    /* access modifiers changed from: private */
    public boolean mDozing;
    private boolean mDozingAborted;
    /* access modifiers changed from: private */
    public final Handler mHandler = new Handler();
    private Animator mInFrontAnimator;
    private float mInFrontTarget;
    private DozeHost.PulseCallback mPulseCallback;
    private final Runnable mPulseIn = new Runnable() {
        public void run() {
            if (DozeScrimController.DEBUG) {
                Log.d("DozeScrimController", "Pulse in, mDozing=" + DozeScrimController.this.mDozing + " mPulseReason=" + DozeLog.pulseReasonToString(DozeScrimController.this.mPulseReason));
            }
            if (DozeScrimController.this.mDozing) {
                DozeLog.tracePulseStart(DozeScrimController.this.mPulseReason);
                DozeScrimController.this.pulseStarted();
                if (DozeScrimController.this.mDozeParameters.getAlwaysOn()) {
                    DozeScrimController.this.mHandler.post(new Runnable() {
                        public void run() {
                            DozeScrimController.this.onScreenTurnedOn();
                        }
                    });
                }
            }
        }
    };
    private final Runnable mPulseInFinished = new Runnable() {
        public void run() {
            if (DozeScrimController.DEBUG) {
                Log.d("DozeScrimController", "Pulse in finished, mDozing=" + DozeScrimController.this.mDozing);
            }
            if (DozeScrimController.this.mDozing) {
                DozeScrimController.this.mHandler.postDelayed(DozeScrimController.this.mPulseOut, (long) DozeScrimController.this.mDozeParameters.getPulseVisibleDuration());
                DozeScrimController.this.mHandler.postDelayed(DozeScrimController.this.mPulseOutExtended, (long) DozeScrimController.this.mDozeParameters.getPulseVisibleDurationExtended());
            }
        }
    };
    /* access modifiers changed from: private */
    public final Runnable mPulseOut = new Runnable() {
        public void run() {
            DozeScrimController.this.mHandler.removeCallbacks(DozeScrimController.this.mPulseOutExtended);
            if (DozeScrimController.DEBUG) {
                Log.d("DozeScrimController", "Pulse out, mDozing=" + DozeScrimController.this.mDozing);
            }
            if (DozeScrimController.this.mDozing) {
                DozeScrimController.this.startScrimAnimation(true, DozeScrimController.this.mDozeParameters.getAlwaysOn() ? 0.0f : 1.0f, (long) DozeScrimController.this.mDozeParameters.getPulseOutDuration(), Interpolators.ALPHA_IN, DozeScrimController.this.mPulseOutFinished);
            }
        }
    };
    /* access modifiers changed from: private */
    public final Runnable mPulseOutExtended = new Runnable() {
        public void run() {
            DozeScrimController.this.mHandler.removeCallbacks(DozeScrimController.this.mPulseOut);
            DozeScrimController.this.mPulseOut.run();
        }
    };
    /* access modifiers changed from: private */
    public final Runnable mPulseOutFinished = new Runnable() {
        public void run() {
            if (DozeScrimController.DEBUG) {
                Log.d("DozeScrimController", "Pulse out finished");
            }
            DozeLog.tracePulseFinish();
            DozeScrimController.this.pulseFinished();
        }
    };
    /* access modifiers changed from: private */
    public int mPulseReason;
    private final ScrimController mScrimController;
    private boolean mWakeAndUnlocking;

    public DozeScrimController(ScrimController scrimController, Context context) {
        this.mContext = context;
        this.mScrimController = scrimController;
        this.mDozeParameters = new DozeParameters(context);
    }

    public void setDozing(boolean dozing, boolean animate) {
        if (this.mDozing != dozing) {
            this.mDozing = dozing;
            this.mWakeAndUnlocking = false;
            if (this.mDozing) {
                this.mDozingAborted = false;
                abortAnimations();
                float f = 1.0f;
                this.mScrimController.setDozeBehindAlpha(1.0f);
                ScrimController scrimController = this.mScrimController;
                if (this.mDozeParameters.getAlwaysOn()) {
                    f = 0.0f;
                }
                scrimController.setDozeInFrontAlpha(f);
            } else {
                cancelPulsing();
                if (animate) {
                    startScrimAnimation(false, 0.0f, 700, Interpolators.LINEAR_OUT_SLOW_IN);
                    startScrimAnimation(true, 0.0f, 700, Interpolators.LINEAR_OUT_SLOW_IN);
                } else {
                    abortAnimations();
                    this.mScrimController.setDozeBehindAlpha(0.0f);
                    this.mScrimController.setDozeInFrontAlpha(0.0f);
                }
            }
        }
    }

    public void setWakeAndUnlocking() {
        if (!this.mWakeAndUnlocking) {
            this.mWakeAndUnlocking = true;
            this.mScrimController.setDozeBehindAlpha(0.0f);
            this.mScrimController.setDozeInFrontAlpha(0.0f);
        }
    }

    public void abortPulsing() {
        cancelPulsing();
        if (this.mDozing && !this.mWakeAndUnlocking) {
            float f = 1.0f;
            this.mScrimController.setDozeBehindAlpha(1.0f);
            ScrimController scrimController = this.mScrimController;
            if (this.mDozeParameters.getAlwaysOn() && !this.mDozingAborted) {
                f = 0.0f;
            }
            scrimController.setDozeInFrontAlpha(f);
        }
    }

    public void abortDoze() {
        this.mDozingAborted = true;
        abortPulsing();
    }

    public void onScreenTurnedOn() {
        if (isPulsing()) {
            boolean pickupOrDoubleTap = this.mPulseReason == 3 || this.mPulseReason == 4;
            startScrimAnimation(true, 0.0f, (long) this.mDozeParameters.getPulseInDuration(pickupOrDoubleTap), pickupOrDoubleTap ? Interpolators.LINEAR_OUT_SLOW_IN : Interpolators.ALPHA_OUT, this.mPulseInFinished);
        }
    }

    public boolean isPulsing() {
        return this.mPulseCallback != null;
    }

    public void extendPulse() {
        this.mHandler.removeCallbacks(this.mPulseOut);
    }

    private void cancelPulsing() {
        if (DEBUG) {
            Log.d("DozeScrimController", "Cancel pulsing");
        }
        if (this.mPulseCallback != null) {
            this.mHandler.removeCallbacks(this.mPulseIn);
            this.mHandler.removeCallbacks(this.mPulseOut);
            this.mHandler.removeCallbacks(this.mPulseOutExtended);
            pulseFinished();
        }
    }

    /* access modifiers changed from: private */
    public void pulseStarted() {
        if (this.mPulseCallback != null) {
            this.mPulseCallback.onPulseStarted();
        }
    }

    /* access modifiers changed from: private */
    public void pulseFinished() {
        if (this.mPulseCallback != null) {
            this.mPulseCallback.onPulseFinished();
            this.mPulseCallback = null;
        }
    }

    private void abortAnimations() {
        if (this.mInFrontAnimator != null) {
            this.mInFrontAnimator.cancel();
        }
        if (this.mBehindAnimator != null) {
            this.mBehindAnimator.cancel();
        }
    }

    private void startScrimAnimation(boolean inFront, float target, long duration, Interpolator interpolator) {
        startScrimAnimation(inFront, target, duration, interpolator, null);
    }

    /* access modifiers changed from: private */
    public void startScrimAnimation(final boolean inFront, float target, long duration, Interpolator interpolator, final Runnable endRunnable) {
        Animator current = getCurrentAnimator(inFront);
        if (current != null) {
            if (getCurrentTarget(inFront) != target) {
                current.cancel();
            } else {
                return;
            }
        }
        ValueAnimator anim = ValueAnimator.ofFloat(new float[]{getDozeAlpha(inFront), target});
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                DozeScrimController.this.setDozeAlpha(inFront, ((Float) animation.getAnimatedValue()).floatValue());
            }
        });
        anim.setInterpolator(interpolator);
        anim.setDuration(duration);
        anim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                DozeScrimController.this.setCurrentAnimator(inFront, null);
                if (endRunnable != null) {
                    endRunnable.run();
                }
            }
        });
        anim.start();
        setCurrentAnimator(inFront, anim);
        setCurrentTarget(inFront, target);
    }

    private float getCurrentTarget(boolean inFront) {
        return inFront ? this.mInFrontTarget : this.mBehindTarget;
    }

    private void setCurrentTarget(boolean inFront, float target) {
        if (inFront) {
            this.mInFrontTarget = target;
        } else {
            this.mBehindTarget = target;
        }
    }

    private Animator getCurrentAnimator(boolean inFront) {
        return inFront ? this.mInFrontAnimator : this.mBehindAnimator;
    }

    /* access modifiers changed from: private */
    public void setCurrentAnimator(boolean inFront, Animator animator) {
        if (inFront) {
            this.mInFrontAnimator = animator;
        } else {
            this.mBehindAnimator = animator;
        }
    }

    /* access modifiers changed from: private */
    public void setDozeAlpha(boolean inFront, float alpha) {
        if (!this.mWakeAndUnlocking) {
            if (inFront) {
                this.mScrimController.setDozeInFrontAlpha(alpha);
            } else {
                this.mScrimController.setDozeBehindAlpha(alpha);
            }
        }
    }

    private float getDozeAlpha(boolean inFront) {
        if (inFront) {
            return this.mScrimController.getDozeInFrontAlpha();
        }
        return this.mScrimController.getDozeBehindAlpha();
    }
}
