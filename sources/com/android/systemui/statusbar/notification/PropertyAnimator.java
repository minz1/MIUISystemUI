package com.android.systemui.statusbar.notification;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.util.Property;
import android.view.View;
import android.view.animation.Interpolator;
import com.android.systemui.Interpolators;
import com.android.systemui.statusbar.stack.AnimationProperties;
import com.android.systemui.statusbar.stack.ViewState;

public class PropertyAnimator {

    public interface AnimatableProperty {
        int getAnimationEndTag();

        int getAnimationStartTag();

        int getAnimatorTag();

        Property getProperty();
    }

    public static <T extends View> void startAnimation(T view, AnimatableProperty animatableProperty, float newEndValue, AnimationProperties properties) {
        final T t = view;
        AnimationProperties animationProperties = properties;
        final Property<T, Float> property = animatableProperty.getProperty();
        final int animationStartTag = animatableProperty.getAnimationStartTag();
        final int animationEndTag = animatableProperty.getAnimationEndTag();
        Float previousStartValue = (Float) ViewState.getChildTag(t, animationStartTag);
        Float previousEndValue = (Float) ViewState.getChildTag(t, animationEndTag);
        if (previousEndValue == null || previousEndValue.floatValue() != newEndValue) {
            final int animatorTag = animatableProperty.getAnimatorTag();
            ValueAnimator previousAnimator = (ValueAnimator) ViewState.getChildTag(t, animatorTag);
            if (properties.getAnimationFilter().shouldAnimateProperty(property)) {
                Float currentValue = property.get(t);
                ValueAnimator animator = ValueAnimator.ofFloat(new float[]{currentValue.floatValue(), newEndValue});
                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        property.set(t, (Float) valueAnimator.getAnimatedValue());
                    }
                });
                Interpolator customInterpolator = animationProperties.getCustomInterpolator(t, property);
                Interpolator interpolator = customInterpolator != null ? customInterpolator : Interpolators.FAST_OUT_SLOW_IN;
                animator.setInterpolator(interpolator);
                animator.setDuration(ViewState.cancelAnimatorAndGetNewDuration(animationProperties.duration, previousAnimator));
                Interpolator interpolator2 = customInterpolator;
                Interpolator interpolator3 = interpolator;
                if (animationProperties.delay > 0 && (previousAnimator == null || previousAnimator.getAnimatedFraction() == 0.0f)) {
                    animator.setStartDelay(animationProperties.delay);
                }
                AnimatorListenerAdapter listener = properties.getAnimationFinishListener();
                if (listener != null) {
                    animator.addListener(listener);
                }
                animator.addListener(new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator animation) {
                        t.setTag(animatorTag, null);
                        t.setTag(animationStartTag, null);
                        t.setTag(animationEndTag, null);
                    }
                });
                ViewState.startAnimator(animator, listener);
                t.setTag(animatorTag, animator);
                t.setTag(animationStartTag, currentValue);
                t.setTag(animationEndTag, Float.valueOf(newEndValue));
            } else if (previousAnimator != null) {
                PropertyValuesHolder[] values = previousAnimator.getValues();
                float newStartValue = previousStartValue.floatValue() + (newEndValue - previousEndValue.floatValue());
                Float f = previousStartValue;
                values[0].setFloatValues(new float[]{newStartValue, newEndValue});
                t.setTag(animationStartTag, Float.valueOf(newStartValue));
                t.setTag(animationEndTag, Float.valueOf(newEndValue));
                previousAnimator.setCurrentPlayTime(previousAnimator.getCurrentPlayTime());
            } else {
                property.set(t, Float.valueOf(newEndValue));
            }
        }
    }
}
