package com.android.systemui.statusbar.stack;

import android.animation.AnimatorListenerAdapter;
import android.util.ArrayMap;
import android.util.Property;
import android.view.View;
import android.view.animation.Interpolator;

public class AnimationProperties {
    public long delay;
    public long duration;
    private ArrayMap<Property, Interpolator> mInterpolatorMap;

    public AnimationFilter getAnimationFilter() {
        return new AnimationFilter();
    }

    public AnimatorListenerAdapter getAnimationFinishListener() {
        return null;
    }

    public boolean wasAdded(View view) {
        return false;
    }

    public Interpolator getCustomInterpolator(View child, Property property) {
        if (this.mInterpolatorMap != null) {
            return this.mInterpolatorMap.get(property);
        }
        return null;
    }

    public void combineCustomInterpolators(AnimationProperties iconAnimationProperties) {
        ArrayMap<Property, Interpolator> map = iconAnimationProperties.mInterpolatorMap;
        if (map != null) {
            if (this.mInterpolatorMap == null) {
                this.mInterpolatorMap = new ArrayMap<>();
            }
            this.mInterpolatorMap.putAll(map);
        }
    }

    public AnimationProperties setCustomInterpolator(Property property, Interpolator interpolator) {
        if (this.mInterpolatorMap == null) {
            this.mInterpolatorMap = new ArrayMap<>();
        }
        this.mInterpolatorMap.put(property, interpolator);
        return this;
    }

    public AnimationProperties setDuration(long duration2) {
        this.duration = duration2;
        return this;
    }

    public AnimationProperties setDelay(long delay2) {
        this.delay = delay2;
        return this;
    }

    public AnimationProperties resetCustomInterpolators() {
        this.mInterpolatorMap = null;
        return this;
    }
}
