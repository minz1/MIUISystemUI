package com.android.systemui.statusbar.stack;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.util.Property;
import android.view.View;
import android.view.animation.Interpolator;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.statusbar.ExpandableView;
import com.android.systemui.statusbar.notification.PropertyAnimator;
import com.android.systemui.statusbar.policy.HeadsUpManager;

public class ViewState {
    protected static final AnimationProperties NO_NEW_ANIMATIONS = new AnimationProperties() {
        AnimationFilter mAnimationFilter = new AnimationFilter();

        public AnimationFilter getAnimationFilter() {
            return this.mAnimationFilter;
        }
    };
    private static final PropertyAnimator.AnimatableProperty SCALE_X_PROPERTY = new PropertyAnimator.AnimatableProperty() {
        public int getAnimationStartTag() {
            return R.id.scale_x_animator_start_value_tag;
        }

        public int getAnimationEndTag() {
            return R.id.scale_x_animator_end_value_tag;
        }

        public int getAnimatorTag() {
            return R.id.scale_x_animator_tag;
        }

        public Property getProperty() {
            return View.SCALE_X;
        }
    };
    private static final PropertyAnimator.AnimatableProperty SCALE_Y_PROPERTY = new PropertyAnimator.AnimatableProperty() {
        public int getAnimationStartTag() {
            return R.id.scale_y_animator_start_value_tag;
        }

        public int getAnimationEndTag() {
            return R.id.scale_y_animator_end_value_tag;
        }

        public int getAnimatorTag() {
            return R.id.scale_y_animator_tag;
        }

        public Property getProperty() {
            return View.SCALE_Y;
        }
    };
    public float alpha;
    public boolean gone;
    public boolean hidden;
    public int paddingBottom;
    public int paddingTop;
    public float scaleX = 1.0f;
    public float scaleY = 1.0f;
    public float xTranslation;
    public float yTranslation;
    public float zTranslation;

    public void copyFrom(ViewState viewState) {
        this.alpha = viewState.alpha;
        this.xTranslation = viewState.xTranslation;
        this.yTranslation = viewState.yTranslation;
        this.zTranslation = viewState.zTranslation;
        this.gone = viewState.gone;
        this.hidden = viewState.hidden;
        this.scaleX = viewState.scaleX;
        this.scaleY = viewState.scaleY;
        this.paddingTop = viewState.paddingTop;
        this.paddingBottom = viewState.paddingBottom;
    }

    public void initFrom(View view) {
        this.alpha = view.getAlpha();
        this.xTranslation = view.getTranslationX();
        this.yTranslation = view.getTranslationY();
        this.zTranslation = view.getTranslationZ();
        boolean z = false;
        this.gone = view.getVisibility() == 8;
        if (view.getVisibility() == 4) {
            z = true;
        }
        this.hidden = z;
        this.scaleX = view.getScaleX();
        this.scaleY = view.getScaleY();
        this.paddingTop = view.getPaddingTop();
        this.paddingBottom = view.getPaddingBottom();
    }

    public void applyToView(View view) {
        int newLayerType;
        View view2 = view;
        if (!this.gone) {
            boolean animatingX = isAnimating(view2, (int) R.id.translation_x_animator_tag);
            if (animatingX) {
                updateAnimationX(view);
            } else if (view.getTranslationX() != this.xTranslation) {
                view2.setTranslationX(this.xTranslation);
            }
            if (isAnimating(view2, (int) R.id.translation_y_animator_tag)) {
                updateAnimationY(view);
            } else if (view.getTranslationY() != this.yTranslation) {
                view2.setTranslationY(this.yTranslation);
            }
            if (isAnimating(view2, (int) R.id.padding_top_animator_tag)) {
                startPaddingTopAnimation(view2, NO_NEW_ANIMATIONS);
            } else if (this.paddingTop != view.getPaddingTop()) {
                view2.setPadding(view.getPaddingLeft(), this.paddingTop, view.getPaddingRight(), view.getPaddingBottom());
            }
            if (isAnimating(view2, (int) R.id.padding_bottom_animator_tag)) {
                startPaddingBottomAnimation(view2, NO_NEW_ANIMATIONS);
            } else if (this.paddingBottom != view.getPaddingBottom()) {
                view2.setPadding(view.getPaddingLeft(), view.getPaddingTop(), view.getPaddingRight(), this.paddingBottom);
            }
            if (isAnimating(view2, (int) R.id.translation_z_animator_tag)) {
                updateAnimationZ(view);
            } else if (view.getTranslationZ() != this.zTranslation) {
                view2.setTranslationZ(this.zTranslation);
            }
            if (isAnimating(view2, SCALE_X_PROPERTY)) {
                updateAnimation(view2, SCALE_X_PROPERTY, this.scaleX);
            } else if (view.getScaleX() != this.scaleX) {
                view2.setScaleX(this.scaleX);
            }
            if (isAnimating(view2, SCALE_Y_PROPERTY)) {
                updateAnimation(view2, SCALE_Y_PROPERTY, this.scaleY);
            } else if (view.getScaleY() != this.scaleY) {
                view2.setScaleY(this.scaleY);
            }
            int oldVisibility = view.getVisibility();
            boolean newLayerTypeIsHardware = true;
            boolean becomesInvisible = this.alpha == 0.0f || (this.hidden && (!isAnimating(view) || oldVisibility != 0));
            if (isAnimating(view2, (int) R.id.alpha_animator_tag)) {
                updateAlphaAnimation(view);
                boolean z = animatingX;
            } else if (view.getAlpha() != this.alpha) {
                boolean becomesFullyVisible = this.alpha == 1.0f;
                if (becomesInvisible || becomesFullyVisible || !view.hasOverlappingRendering()) {
                    newLayerTypeIsHardware = false;
                }
                int layerType = view.getLayerType();
                if (newLayerTypeIsHardware) {
                    newLayerType = 2;
                } else {
                    newLayerType = 0;
                }
                int newLayerType2 = newLayerType;
                if (layerType != newLayerType2) {
                    boolean z2 = animatingX;
                    view2.setLayerType(newLayerType2, null);
                }
                view2.setAlpha(this.alpha);
            }
            int newVisibility = becomesInvisible ? 4 : 0;
            if (newVisibility != oldVisibility && (!(view2 instanceof ExpandableView) || !((ExpandableView) view2).willBeGone())) {
                view2.setVisibility(newVisibility);
            }
        }
    }

    public boolean isAnimating(View view) {
        if (!isAnimating(view, (int) R.id.translation_x_animator_tag) && !isAnimating(view, (int) R.id.translation_y_animator_tag) && !isAnimating(view, (int) R.id.translation_z_animator_tag) && !isAnimating(view, (int) R.id.alpha_animator_tag) && !isAnimating(view, SCALE_X_PROPERTY) && !isAnimating(view, SCALE_Y_PROPERTY)) {
            return false;
        }
        return true;
    }

    private static boolean isAnimating(View view, int tag) {
        return getChildTag(view, tag) != null;
    }

    public static boolean isAnimating(View view, PropertyAnimator.AnimatableProperty property) {
        return getChildTag(view, property.getAnimatorTag()) != null;
    }

    public void animateTo(View child, AnimationProperties animationProperties) {
        boolean alphaChanging = false;
        boolean wasVisible = child.getVisibility() == 0;
        float alpha2 = this.alpha;
        if (!wasVisible && (!(alpha2 == 0.0f && child.getAlpha() == 0.0f) && !this.gone && !this.hidden)) {
            child.setVisibility(0);
        }
        if (this.alpha != child.getAlpha()) {
            alphaChanging = true;
        }
        if (child instanceof ExpandableView) {
            alphaChanging &= true ^ ((ExpandableView) child).willBeGone();
        }
        if (child.getTranslationX() != this.xTranslation) {
            startXTranslationAnimation(child, animationProperties);
        } else {
            abortAnimation(child, R.id.translation_x_animator_tag);
        }
        if (child.getTranslationY() != this.yTranslation) {
            startYTranslationAnimation(child, animationProperties);
        } else {
            abortAnimation(child, R.id.translation_y_animator_tag);
        }
        if (child.getPaddingTop() != this.paddingTop) {
            startPaddingTopAnimation(child, animationProperties);
        } else {
            abortAnimation(child, R.id.padding_top_animator_tag);
        }
        if (child.getPaddingBottom() != this.paddingBottom) {
            startPaddingBottomAnimation(child, animationProperties);
        } else {
            abortAnimation(child, R.id.padding_bottom_animator_tag);
        }
        if (child.getTranslationZ() != this.zTranslation) {
            startZTranslationAnimation(child, animationProperties);
        } else {
            abortAnimation(child, R.id.translation_z_animator_tag);
        }
        if (child.getScaleX() != this.scaleX) {
            PropertyAnimator.startAnimation(child, SCALE_X_PROPERTY, this.scaleX, animationProperties);
        } else {
            abortAnimation(child, SCALE_X_PROPERTY.getAnimatorTag());
        }
        if (child.getScaleY() != this.scaleY) {
            PropertyAnimator.startAnimation(child, SCALE_Y_PROPERTY, this.scaleY, animationProperties);
        } else {
            abortAnimation(child, SCALE_Y_PROPERTY.getAnimatorTag());
        }
        if (alphaChanging) {
            startAlphaAnimation(child, animationProperties);
        } else {
            abortAnimation(child, R.id.alpha_animator_tag);
        }
    }

    private void updateAlphaAnimation(View view) {
        startAlphaAnimation(view, NO_NEW_ANIMATIONS);
    }

    private void startAlphaAnimation(View child, AnimationProperties properties) {
        final View view = child;
        AnimationProperties animationProperties = properties;
        Float previousStartValue = (Float) getChildTag(view, R.id.alpha_animator_start_value_tag);
        Float previousEndValue = (Float) getChildTag(view, R.id.alpha_animator_end_value_tag);
        final float newEndValue = this.alpha;
        if (previousEndValue == null || previousEndValue.floatValue() != newEndValue) {
            ObjectAnimator previousAnimator = (ObjectAnimator) getChildTag(view, R.id.alpha_animator_tag);
            if (!properties.getAnimationFilter().animateAlpha) {
                if (previousAnimator != null) {
                    PropertyValuesHolder[] values = previousAnimator.getValues();
                    float newStartValue = previousStartValue.floatValue() + (newEndValue - previousEndValue.floatValue());
                    values[0].setFloatValues(new float[]{newStartValue, newEndValue});
                    view.setTag(R.id.alpha_animator_start_value_tag, Float.valueOf(newStartValue));
                    view.setTag(R.id.alpha_animator_end_value_tag, Float.valueOf(newEndValue));
                    previousAnimator.setCurrentPlayTime(previousAnimator.getCurrentPlayTime());
                    return;
                }
                view.setAlpha(newEndValue);
                if (newEndValue == 0.0f) {
                    view.setVisibility(4);
                }
            }
            ObjectAnimator animator = ObjectAnimator.ofFloat(view, View.ALPHA, new float[]{child.getAlpha(), newEndValue});
            animator.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
            view.setLayerType(2, null);
            animator.addListener(new AnimatorListenerAdapter() {
                public boolean mWasCancelled;

                public void onAnimationEnd(Animator animation) {
                    view.setLayerType(0, null);
                    if (newEndValue == 0.0f && !this.mWasCancelled) {
                        view.setVisibility(4);
                    }
                    view.setTag(R.id.alpha_animator_tag, null);
                    view.setTag(R.id.alpha_animator_start_value_tag, null);
                    view.setTag(R.id.alpha_animator_end_value_tag, null);
                }

                public void onAnimationCancel(Animator animation) {
                    this.mWasCancelled = true;
                }

                public void onAnimationStart(Animator animation) {
                    this.mWasCancelled = false;
                }
            });
            animator.setDuration(cancelAnimatorAndGetNewDuration(animationProperties.duration, previousAnimator));
            Float f = previousStartValue;
            if (animationProperties.delay > 0 && (previousAnimator == null || previousAnimator.getAnimatedFraction() == 0.0f)) {
                animator.setStartDelay(animationProperties.delay);
            }
            AnimatorListenerAdapter listener = properties.getAnimationFinishListener();
            if (listener != null) {
                animator.addListener(listener);
            }
            startAnimator(animator, listener);
            view.setTag(R.id.alpha_animator_tag, animator);
            view.setTag(R.id.alpha_animator_start_value_tag, Float.valueOf(child.getAlpha()));
            view.setTag(R.id.alpha_animator_end_value_tag, Float.valueOf(newEndValue));
        }
    }

    private void updateAnimationZ(View view) {
        startZTranslationAnimation(view, NO_NEW_ANIMATIONS);
    }

    private void updateAnimation(View view, PropertyAnimator.AnimatableProperty property, float endValue) {
        PropertyAnimator.startAnimation(view, property, endValue, NO_NEW_ANIMATIONS);
    }

    private void startZTranslationAnimation(View child, AnimationProperties properties) {
        final View view = child;
        AnimationProperties animationProperties = properties;
        Float previousStartValue = (Float) getChildTag(view, R.id.translation_z_animator_start_value_tag);
        Float previousEndValue = (Float) getChildTag(view, R.id.translation_z_animator_end_value_tag);
        float newEndValue = this.zTranslation;
        if (previousEndValue == null || previousEndValue.floatValue() != newEndValue) {
            ObjectAnimator previousAnimator = (ObjectAnimator) getChildTag(view, R.id.translation_z_animator_tag);
            if (!properties.getAnimationFilter().animateZ) {
                if (previousAnimator != null) {
                    PropertyValuesHolder[] values = previousAnimator.getValues();
                    float newStartValue = previousStartValue.floatValue() + (newEndValue - previousEndValue.floatValue());
                    values[0].setFloatValues(new float[]{newStartValue, newEndValue});
                    view.setTag(R.id.translation_z_animator_start_value_tag, Float.valueOf(newStartValue));
                    view.setTag(R.id.translation_z_animator_end_value_tag, Float.valueOf(newEndValue));
                    previousAnimator.setCurrentPlayTime(previousAnimator.getCurrentPlayTime());
                    return;
                }
                view.setTranslationZ(newEndValue);
            }
            ObjectAnimator animator = ObjectAnimator.ofFloat(view, View.TRANSLATION_Z, new float[]{Float.isNaN(child.getTranslationZ()) ? 0.0f : child.getTranslationZ(), newEndValue});
            animator.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
            animator.setDuration(cancelAnimatorAndGetNewDuration(animationProperties.duration, previousAnimator));
            Float f = previousStartValue;
            if (animationProperties.delay > 0 && (previousAnimator == null || previousAnimator.getAnimatedFraction() == 0.0f)) {
                animator.setStartDelay(animationProperties.delay);
            }
            AnimatorListenerAdapter listener = properties.getAnimationFinishListener();
            if (listener != null) {
                animator.addListener(listener);
            }
            animator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    view.setTag(R.id.translation_z_animator_tag, null);
                    view.setTag(R.id.translation_z_animator_start_value_tag, null);
                    view.setTag(R.id.translation_z_animator_end_value_tag, null);
                }
            });
            startAnimator(animator, listener);
            view.setTag(R.id.translation_z_animator_tag, animator);
            view.setTag(R.id.translation_z_animator_start_value_tag, Float.valueOf(child.getTranslationZ()));
            view.setTag(R.id.translation_z_animator_end_value_tag, Float.valueOf(newEndValue));
        }
    }

    private void updateAnimationX(View view) {
        startXTranslationAnimation(view, NO_NEW_ANIMATIONS);
    }

    private void startXTranslationAnimation(View child, AnimationProperties properties) {
        final View view = child;
        AnimationProperties animationProperties = properties;
        Float previousStartValue = (Float) getChildTag(view, R.id.translation_x_animator_start_value_tag);
        Float previousEndValue = (Float) getChildTag(view, R.id.translation_x_animator_end_value_tag);
        float newEndValue = this.xTranslation;
        if (previousEndValue == null || previousEndValue.floatValue() != newEndValue) {
            ObjectAnimator previousAnimator = (ObjectAnimator) getChildTag(view, R.id.translation_x_animator_tag);
            if (properties.getAnimationFilter().animateX) {
                ObjectAnimator animator = ObjectAnimator.ofFloat(view, View.TRANSLATION_X, new float[]{child.getTranslationX(), newEndValue});
                Interpolator customInterpolator = animationProperties.getCustomInterpolator(view, View.TRANSLATION_X);
                animator.setInterpolator(customInterpolator != null ? customInterpolator : Interpolators.FAST_OUT_SLOW_IN);
                animator.setDuration(cancelAnimatorAndGetNewDuration(animationProperties.duration, previousAnimator));
                Float f = previousStartValue;
                if (animationProperties.delay > 0 && (previousAnimator == null || previousAnimator.getAnimatedFraction() == 0.0f)) {
                    animator.setStartDelay(animationProperties.delay);
                }
                AnimatorListenerAdapter listener = properties.getAnimationFinishListener();
                if (listener != null) {
                    animator.addListener(listener);
                }
                animator.addListener(new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator animation) {
                        view.setTag(R.id.translation_x_animator_tag, null);
                        view.setTag(R.id.translation_x_animator_start_value_tag, null);
                        view.setTag(R.id.translation_x_animator_end_value_tag, null);
                    }
                });
                startAnimator(animator, listener);
                view.setTag(R.id.translation_x_animator_tag, animator);
                view.setTag(R.id.translation_x_animator_start_value_tag, Float.valueOf(child.getTranslationX()));
                view.setTag(R.id.translation_x_animator_end_value_tag, Float.valueOf(newEndValue));
            } else if (previousAnimator != null) {
                PropertyValuesHolder[] values = previousAnimator.getValues();
                float newStartValue = previousStartValue.floatValue() + (newEndValue - previousEndValue.floatValue());
                values[0].setFloatValues(new float[]{newStartValue, newEndValue});
                view.setTag(R.id.translation_x_animator_start_value_tag, Float.valueOf(newStartValue));
                view.setTag(R.id.translation_x_animator_end_value_tag, Float.valueOf(newEndValue));
                previousAnimator.setCurrentPlayTime(previousAnimator.getCurrentPlayTime());
            } else {
                view.setTranslationX(newEndValue);
            }
        }
    }

    private void updateAnimationY(View view) {
        startYTranslationAnimation(view, NO_NEW_ANIMATIONS);
    }

    private void startPaddingTopAnimation(final View child, AnimationProperties properties) {
        getChildTag(child, R.id.padding_top_animator_start_value_tag);
        Integer previousEndValue = (Integer) getChildTag(child, R.id.padding_top_animator_end_value_tag);
        int newEndValue = this.paddingTop;
        if (previousEndValue == null || previousEndValue.intValue() != newEndValue) {
            ValueAnimator previousAnimator = (ValueAnimator) getChildTag(child, R.id.padding_top_animator_tag);
            ValueAnimator animator = ValueAnimator.ofInt(new int[]{child.getPaddingTop(), newEndValue});
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    child.setPadding(child.getPaddingLeft(), ((Integer) animation.getAnimatedValue()).intValue(), child.getPaddingRight(), child.getPaddingBottom());
                }
            });
            animator.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
            animator.setDuration(cancelAnimatorAndGetNewDuration(properties.duration, previousAnimator));
            if (properties.delay > 0 && (previousAnimator == null || previousAnimator.getAnimatedFraction() == 0.0f)) {
                animator.setStartDelay(properties.delay);
            }
            AnimatorListenerAdapter listener = properties.getAnimationFinishListener();
            if (listener != null) {
                animator.addListener(listener);
            }
            animator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    child.setTag(R.id.padding_top_animator_tag, null);
                    child.setTag(R.id.padding_top_animator_start_value_tag, null);
                    child.setTag(R.id.padding_top_animator_end_value_tag, null);
                }
            });
            startAnimator(animator, listener);
            child.setTag(R.id.padding_top_animator_tag, animator);
            child.setTag(R.id.padding_top_animator_start_value_tag, Integer.valueOf(child.getPaddingTop()));
            child.setTag(R.id.padding_top_animator_end_value_tag, Integer.valueOf(newEndValue));
        }
    }

    private void startPaddingBottomAnimation(final View child, AnimationProperties properties) {
        getChildTag(child, R.id.padding_bottom_animator_start_value_tag);
        Integer previousEndValue = (Integer) getChildTag(child, R.id.padding_bottom_animator_end_value_tag);
        int newEndValue = this.paddingBottom;
        if (previousEndValue == null || previousEndValue.intValue() != newEndValue) {
            ValueAnimator previousAnimator = (ValueAnimator) getChildTag(child, R.id.padding_bottom_animator_tag);
            ValueAnimator animator = ValueAnimator.ofInt(new int[]{child.getPaddingBottom(), newEndValue});
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    child.setPadding(child.getPaddingLeft(), child.getPaddingTop(), child.getPaddingRight(), ((Integer) animation.getAnimatedValue()).intValue());
                }
            });
            animator.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
            animator.setDuration(cancelAnimatorAndGetNewDuration(properties.duration, previousAnimator));
            if (properties.delay > 0 && (previousAnimator == null || previousAnimator.getAnimatedFraction() == 0.0f)) {
                animator.setStartDelay(properties.delay);
            }
            AnimatorListenerAdapter listener = properties.getAnimationFinishListener();
            if (listener != null) {
                animator.addListener(listener);
            }
            animator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    child.setTag(R.id.padding_bottom_animator_tag, null);
                    child.setTag(R.id.padding_bottom_animator_start_value_tag, null);
                    child.setTag(R.id.padding_bottom_animator_end_value_tag, null);
                }
            });
            startAnimator(animator, listener);
            child.setTag(R.id.padding_bottom_animator_tag, animator);
            child.setTag(R.id.padding_bottom_animator_start_value_tag, Integer.valueOf(child.getPaddingBottom()));
            child.setTag(R.id.padding_bottom_animator_end_value_tag, Integer.valueOf(newEndValue));
        }
    }

    private void startYTranslationAnimation(View child, AnimationProperties properties) {
        final View view = child;
        AnimationProperties animationProperties = properties;
        Float previousStartValue = (Float) getChildTag(view, R.id.translation_y_animator_start_value_tag);
        Float previousEndValue = (Float) getChildTag(view, R.id.translation_y_animator_end_value_tag);
        float newEndValue = this.yTranslation;
        if (previousEndValue == null || previousEndValue.floatValue() != newEndValue) {
            ObjectAnimator previousAnimator = (ObjectAnimator) getChildTag(view, R.id.translation_y_animator_tag);
            if (properties.getAnimationFilter().shouldAnimateY(view)) {
                ObjectAnimator animator = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, new float[]{child.getTranslationY(), newEndValue});
                Interpolator customInterpolator = animationProperties.getCustomInterpolator(view, View.TRANSLATION_Y);
                animator.setInterpolator(customInterpolator != null ? customInterpolator : Interpolators.FAST_OUT_SLOW_IN);
                animator.setDuration(cancelAnimatorAndGetNewDuration(animationProperties.duration, previousAnimator));
                Float f = previousStartValue;
                if (animationProperties.delay > 0 && (previousAnimator == null || previousAnimator.getAnimatedFraction() == 0.0f)) {
                    animator.setStartDelay(animationProperties.delay);
                }
                AnimatorListenerAdapter listener = properties.getAnimationFinishListener();
                if (listener != null) {
                    animator.addListener(listener);
                }
                animator.addListener(new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator animation) {
                        HeadsUpManager.setIsClickedNotification(view, false);
                        view.setTag(R.id.translation_y_animator_tag, null);
                        view.setTag(R.id.translation_y_animator_start_value_tag, null);
                        view.setTag(R.id.translation_y_animator_end_value_tag, null);
                        ViewState.this.onYTranslationAnimationFinished(view);
                    }
                });
                startAnimator(animator, listener);
                view.setTag(R.id.translation_y_animator_tag, animator);
                view.setTag(R.id.translation_y_animator_start_value_tag, Float.valueOf(child.getTranslationY()));
                view.setTag(R.id.translation_y_animator_end_value_tag, Float.valueOf(newEndValue));
            } else if (previousAnimator != null) {
                PropertyValuesHolder[] values = previousAnimator.getValues();
                float newStartValue = previousStartValue.floatValue() + (newEndValue - previousEndValue.floatValue());
                values[0].setFloatValues(new float[]{newStartValue, newEndValue});
                view.setTag(R.id.translation_y_animator_start_value_tag, Float.valueOf(newStartValue));
                view.setTag(R.id.translation_y_animator_end_value_tag, Float.valueOf(newEndValue));
                previousAnimator.setCurrentPlayTime(previousAnimator.getCurrentPlayTime());
            } else {
                view.setTranslationY(newEndValue);
            }
        }
    }

    /* access modifiers changed from: protected */
    public void onYTranslationAnimationFinished(View view) {
        if (this.hidden && !this.gone) {
            view.setVisibility(4);
        }
    }

    public static void startAnimator(Animator animator, AnimatorListenerAdapter listener) {
        if (listener != null) {
            listener.onAnimationStart(animator);
        }
        animator.start();
    }

    public static <T> T getChildTag(View child, int tag) {
        return child.getTag(tag);
    }

    /* access modifiers changed from: protected */
    public void abortAnimation(View child, int animatorTag) {
        Animator previousAnimator = (Animator) getChildTag(child, animatorTag);
        if (previousAnimator != null) {
            previousAnimator.cancel();
        }
    }

    public static long cancelAnimatorAndGetNewDuration(long duration, ValueAnimator previousAnimator) {
        long newDuration = duration;
        if (previousAnimator == null) {
            return newDuration;
        }
        long newDuration2 = Math.max(previousAnimator.getDuration() - previousAnimator.getCurrentPlayTime(), newDuration);
        previousAnimator.cancel();
        return newDuration2;
    }

    public static float getFinalTranslationY(View view) {
        if (view == null) {
            return 0.0f;
        }
        if (((ValueAnimator) getChildTag(view, R.id.translation_y_animator_tag)) == null) {
            return view.getTranslationY();
        }
        return ((Float) getChildTag(view, R.id.translation_y_animator_end_value_tag)).floatValue();
    }

    public static float getFinalTranslationZ(View view) {
        if (view == null) {
            return 0.0f;
        }
        if (((ValueAnimator) getChildTag(view, R.id.translation_z_animator_tag)) == null) {
            return view.getTranslationZ();
        }
        return ((Float) getChildTag(view, R.id.translation_z_animator_end_value_tag)).floatValue();
    }

    public static boolean isAnimatingY(View child) {
        return getChildTag(child, R.id.translation_y_animator_tag) != null;
    }

    public void cancelAnimations(View view) {
        Animator animator = (Animator) getChildTag(view, R.id.translation_x_animator_tag);
        if (animator != null) {
            animator.cancel();
        }
        Animator animator2 = (Animator) getChildTag(view, R.id.translation_y_animator_tag);
        if (animator2 != null) {
            animator2.cancel();
        }
        Animator animator3 = (Animator) getChildTag(view, R.id.translation_z_animator_tag);
        if (animator3 != null) {
            animator3.cancel();
        }
        Animator animator4 = (Animator) getChildTag(view, R.id.alpha_animator_tag);
        if (animator4 != null) {
            animator4.cancel();
        }
    }
}
