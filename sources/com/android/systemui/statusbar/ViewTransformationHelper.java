package com.android.systemui.statusbar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.statusbar.notification.TransformState;
import java.util.Stack;

public class ViewTransformationHelper implements TransformableView {
    private ArrayMap<Integer, CustomTransformation> mCustomTransformations = new ArrayMap<>();
    private ArrayMap<Integer, View> mTransformedViews = new ArrayMap<>();
    private ValueAnimator mViewTransformationAnimation;

    public static abstract class CustomTransformation {
        public abstract boolean transformFrom(TransformState transformState, TransformableView transformableView, float f);

        public abstract boolean transformTo(TransformState transformState, TransformableView transformableView, float f);

        public boolean initTransformation(TransformState ownState, TransformState otherState) {
            return false;
        }

        public boolean customTransformTarget(TransformState ownState, TransformState otherState) {
            return false;
        }

        public Interpolator getCustomInterpolator(int interpolationType, boolean isFrom) {
            return null;
        }
    }

    public void addTransformedView(int key, View transformedView) {
        this.mTransformedViews.put(Integer.valueOf(key), transformedView);
    }

    public void removeTransformedView(int key) {
        this.mTransformedViews.remove(Integer.valueOf(key));
    }

    public void reset() {
        this.mTransformedViews.clear();
    }

    public void setCustomTransformation(CustomTransformation transformation, int viewType) {
        this.mCustomTransformations.put(Integer.valueOf(viewType), transformation);
    }

    public TransformState getCurrentState(int fadingView) {
        View view = this.mTransformedViews.get(Integer.valueOf(fadingView));
        if (view == null || view.getVisibility() == 8) {
            return null;
        }
        return TransformState.createFrom(view);
    }

    public void transformTo(final TransformableView notification, final Runnable endRunnable) {
        if (this.mViewTransformationAnimation != null) {
            this.mViewTransformationAnimation.cancel();
        }
        this.mViewTransformationAnimation = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
        this.mViewTransformationAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                ViewTransformationHelper.this.transformTo(notification, animation.getAnimatedFraction());
            }
        });
        this.mViewTransformationAnimation.setInterpolator(Interpolators.LINEAR);
        this.mViewTransformationAnimation.setDuration(360);
        this.mViewTransformationAnimation.addListener(new AnimatorListenerAdapter() {
            public boolean mCancelled;

            public void onAnimationEnd(Animator animation) {
                if (!this.mCancelled) {
                    if (endRunnable != null) {
                        endRunnable.run();
                    }
                    ViewTransformationHelper.this.setVisible(false);
                    return;
                }
                ViewTransformationHelper.this.abortTransformations();
            }

            public void onAnimationCancel(Animator animation) {
                this.mCancelled = true;
            }
        });
        this.mViewTransformationAnimation.start();
    }

    public void transformTo(TransformableView notification, float transformationAmount) {
        for (Integer viewType : this.mTransformedViews.keySet()) {
            TransformState ownState = getCurrentState(viewType.intValue());
            if (ownState != null) {
                CustomTransformation customTransformation = this.mCustomTransformations.get(viewType);
                if (customTransformation == null || !customTransformation.transformTo(ownState, notification, transformationAmount)) {
                    TransformState otherState = notification.getCurrentState(viewType.intValue());
                    if (otherState != null) {
                        ownState.transformViewTo(otherState, transformationAmount);
                        otherState.recycle();
                    } else {
                        ownState.disappear(transformationAmount, notification);
                    }
                    ownState.recycle();
                } else {
                    ownState.recycle();
                }
            }
        }
    }

    public void transformFrom(final TransformableView notification) {
        if (this.mViewTransformationAnimation != null) {
            this.mViewTransformationAnimation.cancel();
        }
        this.mViewTransformationAnimation = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
        this.mViewTransformationAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                ViewTransformationHelper.this.transformFrom(notification, animation.getAnimatedFraction());
            }
        });
        this.mViewTransformationAnimation.addListener(new AnimatorListenerAdapter() {
            public boolean mCancelled;

            public void onAnimationEnd(Animator animation) {
                if (!this.mCancelled) {
                    ViewTransformationHelper.this.setVisible(true);
                } else {
                    ViewTransformationHelper.this.abortTransformations();
                }
            }

            public void onAnimationCancel(Animator animation) {
                this.mCancelled = true;
            }
        });
        this.mViewTransformationAnimation.setInterpolator(Interpolators.LINEAR);
        this.mViewTransformationAnimation.setDuration(360);
        this.mViewTransformationAnimation.start();
    }

    public void transformFrom(TransformableView notification, float transformationAmount) {
        for (Integer viewType : this.mTransformedViews.keySet()) {
            TransformState ownState = getCurrentState(viewType.intValue());
            if (ownState != null) {
                CustomTransformation customTransformation = this.mCustomTransformations.get(viewType);
                if (customTransformation == null || !customTransformation.transformFrom(ownState, notification, transformationAmount)) {
                    TransformState otherState = notification.getCurrentState(viewType.intValue());
                    if (otherState != null) {
                        ownState.transformViewFrom(otherState, transformationAmount);
                        otherState.recycle();
                    } else {
                        ownState.appear(transformationAmount, notification);
                    }
                    ownState.recycle();
                } else {
                    ownState.recycle();
                }
            }
        }
    }

    public void setVisible(boolean visible) {
        if (this.mViewTransformationAnimation != null) {
            this.mViewTransformationAnimation.cancel();
        }
        for (Integer viewType : this.mTransformedViews.keySet()) {
            TransformState ownState = getCurrentState(viewType.intValue());
            if (ownState != null) {
                ownState.setVisible(visible, false);
                ownState.recycle();
            }
        }
    }

    /* access modifiers changed from: private */
    public void abortTransformations() {
        for (Integer viewType : this.mTransformedViews.keySet()) {
            TransformState ownState = getCurrentState(viewType.intValue());
            if (ownState != null) {
                ownState.abortTransformation();
                ownState.recycle();
            }
        }
    }

    public void addRemainingTransformTypes(View viewRoot) {
        int numValues = this.mTransformedViews.size();
        for (int i = 0; i < numValues; i++) {
            Object valueAt = this.mTransformedViews.valueAt(i);
            while (true) {
                View view = (View) valueAt;
                if (view == viewRoot.getParent()) {
                    break;
                }
                view.setTag(R.id.contains_transformed_view, true);
                valueAt = view.getParent();
            }
        }
        Stack<View> stack = new Stack<>();
        stack.push(viewRoot);
        while (!stack.isEmpty()) {
            View child = stack.pop();
            if (((Boolean) child.getTag(R.id.contains_transformed_view)) == null) {
                int id = child.getId();
                if (id != -1) {
                    addTransformedView(id, child);
                }
            }
            child.setTag(R.id.contains_transformed_view, null);
            if ((child instanceof ViewGroup) && !this.mTransformedViews.containsValue(child)) {
                ViewGroup group = (ViewGroup) child;
                for (int i2 = 0; i2 < group.getChildCount(); i2++) {
                    stack.push(group.getChildAt(i2));
                }
            }
        }
    }

    public void resetTransformedView(View view) {
        TransformState state = TransformState.createFrom(view);
        state.setVisible(true, true);
        state.recycle();
    }

    public ArraySet<View> getAllTransformingViews() {
        return new ArraySet<>(this.mTransformedViews.values());
    }
}
