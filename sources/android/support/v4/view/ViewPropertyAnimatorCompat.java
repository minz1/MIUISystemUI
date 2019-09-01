package android.support.v4.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.os.Build;
import android.view.View;
import android.view.animation.Interpolator;
import java.lang.ref.WeakReference;

public final class ViewPropertyAnimatorCompat {
    Runnable mEndAction = null;
    int mOldLayerType = -1;
    Runnable mStartAction = null;
    private WeakReference<View> mView;

    static class ViewPropertyAnimatorListenerApi14 implements ViewPropertyAnimatorListener {
        boolean mAnimEndCalled;
        ViewPropertyAnimatorCompat mVpa;

        ViewPropertyAnimatorListenerApi14(ViewPropertyAnimatorCompat vpa) {
            this.mVpa = vpa;
        }

        public void onAnimationStart(View view) {
            this.mAnimEndCalled = false;
            if (this.mVpa.mOldLayerType > -1) {
                view.setLayerType(2, null);
            }
            if (this.mVpa.mStartAction != null) {
                Runnable startAction = this.mVpa.mStartAction;
                this.mVpa.mStartAction = null;
                startAction.run();
            }
            Object listenerTag = view.getTag(2113929216);
            ViewPropertyAnimatorListener listener = null;
            if (listenerTag instanceof ViewPropertyAnimatorListener) {
                listener = (ViewPropertyAnimatorListener) listenerTag;
            }
            if (listener != null) {
                listener.onAnimationStart(view);
            }
        }

        public void onAnimationEnd(View view) {
            if (this.mVpa.mOldLayerType > -1) {
                view.setLayerType(this.mVpa.mOldLayerType, null);
                this.mVpa.mOldLayerType = -1;
            }
            if (Build.VERSION.SDK_INT >= 16 || !this.mAnimEndCalled) {
                if (this.mVpa.mEndAction != null) {
                    Runnable endAction = this.mVpa.mEndAction;
                    this.mVpa.mEndAction = null;
                    endAction.run();
                }
                Object listenerTag = view.getTag(2113929216);
                ViewPropertyAnimatorListener listener = null;
                if (listenerTag instanceof ViewPropertyAnimatorListener) {
                    listener = (ViewPropertyAnimatorListener) listenerTag;
                }
                if (listener != null) {
                    listener.onAnimationEnd(view);
                }
                this.mAnimEndCalled = true;
            }
        }

        public void onAnimationCancel(View view) {
            Object listenerTag = view.getTag(2113929216);
            ViewPropertyAnimatorListener listener = null;
            if (listenerTag instanceof ViewPropertyAnimatorListener) {
                listener = (ViewPropertyAnimatorListener) listenerTag;
            }
            if (listener != null) {
                listener.onAnimationCancel(view);
            }
        }
    }

    ViewPropertyAnimatorCompat(View view) {
        this.mView = new WeakReference<>(view);
    }

    public ViewPropertyAnimatorCompat setDuration(long value) {
        View view = (View) this.mView.get();
        View view2 = view;
        if (view != null) {
            view2.animate().setDuration(value);
        }
        return this;
    }

    public ViewPropertyAnimatorCompat alpha(float value) {
        View view = (View) this.mView.get();
        View view2 = view;
        if (view != null) {
            view2.animate().alpha(value);
        }
        return this;
    }

    public ViewPropertyAnimatorCompat translationY(float value) {
        View view = (View) this.mView.get();
        View view2 = view;
        if (view != null) {
            view2.animate().translationY(value);
        }
        return this;
    }

    public long getDuration() {
        View view = (View) this.mView.get();
        View view2 = view;
        if (view != null) {
            return view2.animate().getDuration();
        }
        return 0;
    }

    public ViewPropertyAnimatorCompat setInterpolator(Interpolator value) {
        View view = (View) this.mView.get();
        View view2 = view;
        if (view != null) {
            view2.animate().setInterpolator(value);
        }
        return this;
    }

    public ViewPropertyAnimatorCompat setStartDelay(long value) {
        View view = (View) this.mView.get();
        View view2 = view;
        if (view != null) {
            view2.animate().setStartDelay(value);
        }
        return this;
    }

    public void cancel() {
        View view = (View) this.mView.get();
        View view2 = view;
        if (view != null) {
            view2.animate().cancel();
        }
    }

    public void start() {
        View view = (View) this.mView.get();
        View view2 = view;
        if (view != null) {
            view2.animate().start();
        }
    }

    public ViewPropertyAnimatorCompat setListener(ViewPropertyAnimatorListener listener) {
        View view = (View) this.mView.get();
        View view2 = view;
        if (view != null) {
            if (Build.VERSION.SDK_INT >= 16) {
                setListenerInternal(view2, listener);
            } else {
                view2.setTag(2113929216, listener);
                setListenerInternal(view2, new ViewPropertyAnimatorListenerApi14(this));
            }
        }
        return this;
    }

    private void setListenerInternal(final View view, final ViewPropertyAnimatorListener listener) {
        if (listener != null) {
            view.animate().setListener(new AnimatorListenerAdapter() {
                public void onAnimationCancel(Animator animation) {
                    listener.onAnimationCancel(view);
                }

                public void onAnimationEnd(Animator animation) {
                    listener.onAnimationEnd(view);
                }

                public void onAnimationStart(Animator animation) {
                    listener.onAnimationStart(view);
                }
            });
        } else {
            view.animate().setListener(null);
        }
    }

    public ViewPropertyAnimatorCompat setUpdateListener(final ViewPropertyAnimatorUpdateListener listener) {
        View view = (View) this.mView.get();
        final View view2 = view;
        if (view != null && Build.VERSION.SDK_INT >= 19) {
            ValueAnimator.AnimatorUpdateListener wrapped = null;
            if (listener != null) {
                wrapped = new ValueAnimator.AnimatorUpdateListener() {
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        listener.onAnimationUpdate(view2);
                    }
                };
            }
            view2.animate().setUpdateListener(wrapped);
        }
        return this;
    }
}
