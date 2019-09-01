package com.android.systemui.statusbar.stack;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.view.View;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.ExpandableView;

public class ExpandableViewState extends ViewState {
    public boolean belowSpeedBump;
    public int clipTopAmount;
    public boolean dark;
    public boolean dimmed;
    public boolean fullyShown;
    public int height;
    public boolean hideSensitive;
    public boolean inShelf;
    public int location;
    public int notGoneIndex;
    public float shadowAlpha;

    public void copyFrom(ViewState viewState) {
        super.copyFrom(viewState);
        if (viewState instanceof ExpandableViewState) {
            ExpandableViewState svs = (ExpandableViewState) viewState;
            this.height = svs.height;
            this.dimmed = svs.dimmed;
            this.shadowAlpha = svs.shadowAlpha;
            this.dark = svs.dark;
            this.hideSensitive = svs.hideSensitive;
            this.belowSpeedBump = svs.belowSpeedBump;
            this.clipTopAmount = svs.clipTopAmount;
            this.notGoneIndex = svs.notGoneIndex;
            this.location = svs.location;
        }
    }

    public void applyToView(View view) {
        super.applyToView(view);
        if (view instanceof ExpandableView) {
            ExpandableView expandableView = (ExpandableView) view;
            int height2 = expandableView.getActualHeight();
            int newHeight = this.height;
            if (height2 != newHeight) {
                expandableView.setActualHeight(newHeight, false);
            }
            float shadowAlpha2 = expandableView.getShadowAlpha();
            float newShadowAlpha = this.shadowAlpha;
            if (shadowAlpha2 != newShadowAlpha) {
                expandableView.setShadowAlpha(newShadowAlpha);
            }
            expandableView.setDimmed(this.dimmed, false);
            expandableView.setHideSensitive(this.hideSensitive, false, 0, 0);
            expandableView.setBelowSpeedBump(this.belowSpeedBump);
            expandableView.setDark(this.dark, false, 0);
            if (((float) expandableView.getClipTopAmount()) != ((float) this.clipTopAmount)) {
                expandableView.setClipTopAmount(this.clipTopAmount);
            }
            expandableView.setTransformingInShelf(false);
            expandableView.setInShelf(this.inShelf);
        }
    }

    public void animateTo(View child, AnimationProperties properties) {
        super.animateTo(child, properties);
        if (child instanceof ExpandableView) {
            ExpandableView expandableView = (ExpandableView) child;
            AnimationFilter animationFilter = properties.getAnimationFilter();
            if (this.height != expandableView.getActualHeight()) {
                startHeightAnimation(expandableView, properties);
            } else {
                abortAnimation(child, R.id.height_animator_tag);
            }
            if (this.shadowAlpha != expandableView.getShadowAlpha()) {
                startShadowAlphaAnimation(expandableView, properties);
            } else {
                abortAnimation(child, R.id.shadow_alpha_animator_tag);
            }
            if (this.clipTopAmount != expandableView.getClipTopAmount()) {
                startInsetAnimation(expandableView, properties);
            } else {
                abortAnimation(child, R.id.top_inset_animator_tag);
            }
            expandableView.setDimmed(this.dimmed, animationFilter.animateDimmed);
            expandableView.setBelowSpeedBump(this.belowSpeedBump);
            expandableView.setHideSensitive(this.hideSensitive, animationFilter.animateHideSensitive, properties.delay, properties.duration);
            expandableView.setDark(this.dark, animationFilter.animateDark, properties.delay);
            if (properties.wasAdded(child) && !this.hidden) {
                expandableView.performAddAnimation(properties.delay, properties.duration, properties.getAnimationFinishListener());
            }
            if (!expandableView.isInShelf() && this.inShelf) {
                expandableView.setTransformingInShelf(true);
            }
            expandableView.setInShelf(this.inShelf);
        }
    }

    private void startHeightAnimation(ExpandableView child, AnimationProperties properties) {
        final ExpandableView expandableView = child;
        AnimationProperties animationProperties = properties;
        Integer previousStartValue = (Integer) getChildTag(expandableView, R.id.height_animator_start_value_tag);
        Integer previousEndValue = (Integer) getChildTag(expandableView, R.id.height_animator_end_value_tag);
        int newEndValue = this.height;
        if (previousEndValue == null || previousEndValue.intValue() != newEndValue) {
            ValueAnimator previousAnimator = (ValueAnimator) getChildTag(expandableView, R.id.height_animator_tag);
            if (properties.getAnimationFilter().animateHeight) {
                ValueAnimator animator = ValueAnimator.ofInt(new int[]{child.getActualHeight(), newEndValue});
                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    public void onAnimationUpdate(ValueAnimator animation) {
                        expandableView.setActualHeight(((Integer) animation.getAnimatedValue()).intValue(), false);
                    }
                });
                animator.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
                animator.setDuration(cancelAnimatorAndGetNewDuration(animationProperties.duration, previousAnimator));
                if (animationProperties.delay > 0 && (previousAnimator == null || previousAnimator.getAnimatedFraction() == 0.0f)) {
                    animator.setStartDelay(animationProperties.delay);
                }
                AnimatorListenerAdapter listener = properties.getAnimationFinishListener();
                if (listener != null) {
                    animator.addListener(listener);
                }
                animator.addListener(new AnimatorListenerAdapter() {
                    boolean mWasCancelled;

                    public void onAnimationEnd(Animator animation) {
                        expandableView.setTag(R.id.height_animator_tag, null);
                        expandableView.setTag(R.id.height_animator_start_value_tag, null);
                        expandableView.setTag(R.id.height_animator_end_value_tag, null);
                        expandableView.setActualHeightAnimating(false);
                        if (!this.mWasCancelled && (expandableView instanceof ExpandableNotificationRow)) {
                            ((ExpandableNotificationRow) expandableView).setGroupExpansionChanging(false);
                        }
                    }

                    public void onAnimationStart(Animator animation) {
                        this.mWasCancelled = false;
                    }

                    public void onAnimationCancel(Animator animation) {
                        this.mWasCancelled = true;
                    }
                });
                startAnimator(animator, listener);
                expandableView.setTag(R.id.height_animator_tag, animator);
                expandableView.setTag(R.id.height_animator_start_value_tag, Integer.valueOf(child.getActualHeight()));
                expandableView.setTag(R.id.height_animator_end_value_tag, Integer.valueOf(newEndValue));
                expandableView.setActualHeightAnimating(true);
            } else if (previousAnimator != null) {
                PropertyValuesHolder[] values = previousAnimator.getValues();
                int newStartValue = previousStartValue.intValue() + (newEndValue - previousEndValue.intValue());
                values[0].setIntValues(new int[]{newStartValue, newEndValue});
                expandableView.setTag(R.id.height_animator_start_value_tag, Integer.valueOf(newStartValue));
                expandableView.setTag(R.id.height_animator_end_value_tag, Integer.valueOf(newEndValue));
                previousAnimator.setCurrentPlayTime(previousAnimator.getCurrentPlayTime());
            } else {
                expandableView.setActualHeight(newEndValue, false);
            }
        }
    }

    private void startShadowAlphaAnimation(ExpandableView child, AnimationProperties properties) {
        final ExpandableView expandableView = child;
        AnimationProperties animationProperties = properties;
        Float previousStartValue = (Float) getChildTag(expandableView, R.id.shadow_alpha_animator_start_value_tag);
        Float previousEndValue = (Float) getChildTag(expandableView, R.id.shadow_alpha_animator_end_value_tag);
        float newEndValue = this.shadowAlpha;
        if (previousEndValue == null || previousEndValue.floatValue() != newEndValue) {
            ValueAnimator previousAnimator = (ValueAnimator) getChildTag(expandableView, R.id.shadow_alpha_animator_tag);
            if (properties.getAnimationFilter().animateShadowAlpha) {
                ValueAnimator animator = ValueAnimator.ofFloat(new float[]{child.getShadowAlpha(), newEndValue});
                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    public void onAnimationUpdate(ValueAnimator animation) {
                        expandableView.setShadowAlpha(((Float) animation.getAnimatedValue()).floatValue());
                    }
                });
                animator.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
                animator.setDuration(cancelAnimatorAndGetNewDuration(animationProperties.duration, previousAnimator));
                if (animationProperties.delay > 0 && (previousAnimator == null || previousAnimator.getAnimatedFraction() == 0.0f)) {
                    animator.setStartDelay(animationProperties.delay);
                }
                AnimatorListenerAdapter listener = properties.getAnimationFinishListener();
                if (listener != null) {
                    animator.addListener(listener);
                }
                animator.addListener(new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator animation) {
                        expandableView.setTag(R.id.shadow_alpha_animator_tag, null);
                        expandableView.setTag(R.id.shadow_alpha_animator_start_value_tag, null);
                        expandableView.setTag(R.id.shadow_alpha_animator_end_value_tag, null);
                    }
                });
                startAnimator(animator, listener);
                expandableView.setTag(R.id.shadow_alpha_animator_tag, animator);
                expandableView.setTag(R.id.shadow_alpha_animator_start_value_tag, Float.valueOf(child.getShadowAlpha()));
                expandableView.setTag(R.id.shadow_alpha_animator_end_value_tag, Float.valueOf(newEndValue));
            } else if (previousAnimator != null) {
                PropertyValuesHolder[] values = previousAnimator.getValues();
                float newStartValue = previousStartValue.floatValue() + (newEndValue - previousEndValue.floatValue());
                values[0].setFloatValues(new float[]{newStartValue, newEndValue});
                expandableView.setTag(R.id.shadow_alpha_animator_start_value_tag, Float.valueOf(newStartValue));
                expandableView.setTag(R.id.shadow_alpha_animator_end_value_tag, Float.valueOf(newEndValue));
                previousAnimator.setCurrentPlayTime(previousAnimator.getCurrentPlayTime());
            } else {
                expandableView.setShadowAlpha(newEndValue);
            }
        }
    }

    private void startInsetAnimation(ExpandableView child, AnimationProperties properties) {
        final ExpandableView expandableView = child;
        AnimationProperties animationProperties = properties;
        Integer previousStartValue = (Integer) getChildTag(expandableView, R.id.top_inset_animator_start_value_tag);
        Integer previousEndValue = (Integer) getChildTag(expandableView, R.id.top_inset_animator_end_value_tag);
        int newEndValue = this.clipTopAmount;
        if (previousEndValue == null || previousEndValue.intValue() != newEndValue) {
            ValueAnimator previousAnimator = (ValueAnimator) getChildTag(expandableView, R.id.top_inset_animator_tag);
            if (properties.getAnimationFilter().animateTopInset) {
                ValueAnimator animator = ValueAnimator.ofInt(new int[]{child.getClipTopAmount(), newEndValue});
                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    public void onAnimationUpdate(ValueAnimator animation) {
                        expandableView.setClipTopAmount(((Integer) animation.getAnimatedValue()).intValue());
                    }
                });
                animator.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
                animator.setDuration(cancelAnimatorAndGetNewDuration(animationProperties.duration, previousAnimator));
                if (animationProperties.delay > 0 && (previousAnimator == null || previousAnimator.getAnimatedFraction() == 0.0f)) {
                    animator.setStartDelay(animationProperties.delay);
                }
                AnimatorListenerAdapter listener = properties.getAnimationFinishListener();
                if (listener != null) {
                    animator.addListener(listener);
                }
                animator.addListener(new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator animation) {
                        expandableView.setTag(R.id.top_inset_animator_tag, null);
                        expandableView.setTag(R.id.top_inset_animator_start_value_tag, null);
                        expandableView.setTag(R.id.top_inset_animator_end_value_tag, null);
                    }
                });
                startAnimator(animator, listener);
                expandableView.setTag(R.id.top_inset_animator_tag, animator);
                expandableView.setTag(R.id.top_inset_animator_start_value_tag, Integer.valueOf(child.getClipTopAmount()));
                expandableView.setTag(R.id.top_inset_animator_end_value_tag, Integer.valueOf(newEndValue));
            } else if (previousAnimator != null) {
                PropertyValuesHolder[] values = previousAnimator.getValues();
                int newStartValue = previousStartValue.intValue() + (newEndValue - previousEndValue.intValue());
                values[0].setIntValues(new int[]{newStartValue, newEndValue});
                expandableView.setTag(R.id.top_inset_animator_start_value_tag, Integer.valueOf(newStartValue));
                expandableView.setTag(R.id.top_inset_animator_end_value_tag, Integer.valueOf(newEndValue));
                previousAnimator.setCurrentPlayTime(previousAnimator.getCurrentPlayTime());
            } else {
                expandableView.setClipTopAmount(newEndValue);
            }
        }
    }

    public static int getFinalActualHeight(ExpandableView view) {
        if (view == null) {
            return 0;
        }
        if (((ValueAnimator) getChildTag(view, R.id.height_animator_tag)) == null) {
            return view.getActualHeight();
        }
        return ((Integer) getChildTag(view, R.id.height_animator_end_value_tag)).intValue();
    }
}
