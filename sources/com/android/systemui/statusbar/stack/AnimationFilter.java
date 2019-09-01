package com.android.systemui.statusbar.stack;

import android.util.ArraySet;
import android.util.Property;
import android.view.View;
import com.android.systemui.statusbar.stack.NotificationStackScrollLayout;
import java.util.ArrayList;

public class AnimationFilter {
    boolean animateAlpha;
    boolean animateDark;
    boolean animateDimmed;
    boolean animateHeight;
    boolean animateHideSensitive;
    public boolean animateShadowAlpha;
    boolean animateTopInset;
    boolean animateX;
    boolean animateY;
    ArraySet<View> animateYViews = new ArraySet<>();
    boolean animateZ;
    boolean hasDelays;
    boolean hasGoToFullShadeEvent;
    boolean hasHeadsUpDisappearClickEvent;
    private ArraySet<Property> mAnimatedProperties = new ArraySet<>();

    public AnimationFilter animateAlpha() {
        this.animateAlpha = true;
        return this;
    }

    public AnimationFilter animateScale() {
        animate(View.SCALE_X);
        animate(View.SCALE_Y);
        return this;
    }

    public AnimationFilter animateX() {
        this.animateX = true;
        return this;
    }

    public AnimationFilter animateY() {
        this.animateY = true;
        return this;
    }

    public AnimationFilter hasDelays() {
        this.hasDelays = true;
        return this;
    }

    public AnimationFilter animateZ() {
        this.animateZ = true;
        return this;
    }

    public AnimationFilter animateHeight() {
        this.animateHeight = true;
        return this;
    }

    public AnimationFilter animateTopInset() {
        this.animateTopInset = true;
        return this;
    }

    public AnimationFilter animateDimmed() {
        this.animateDimmed = true;
        return this;
    }

    public AnimationFilter animateDark() {
        this.animateDark = true;
        return this;
    }

    public AnimationFilter animateHideSensitive() {
        this.animateHideSensitive = true;
        return this;
    }

    public AnimationFilter animateShadowAlpha() {
        this.animateShadowAlpha = true;
        return this;
    }

    public AnimationFilter animateY(View view) {
        this.animateYViews.add(view);
        return this;
    }

    public boolean shouldAnimateY(View view) {
        return this.animateY || this.animateYViews.contains(view);
    }

    public void applyCombination(ArrayList<NotificationStackScrollLayout.AnimationEvent> events) {
        reset();
        int size = events.size();
        for (int i = 0; i < size; i++) {
            NotificationStackScrollLayout.AnimationEvent ev = events.get(i);
            combineFilter(events.get(i).filter);
            if (ev.animationType == 10) {
                this.hasGoToFullShadeEvent = true;
            }
            if (ev.animationType == 16) {
                this.hasHeadsUpDisappearClickEvent = true;
            }
        }
    }

    public void combineFilter(AnimationFilter filter) {
        this.animateAlpha |= filter.animateAlpha;
        this.animateX |= filter.animateX;
        this.animateY |= filter.animateY;
        this.animateYViews.addAll(filter.animateYViews);
        this.animateZ |= filter.animateZ;
        this.animateHeight |= filter.animateHeight;
        this.animateTopInset |= filter.animateTopInset;
        this.animateDimmed |= filter.animateDimmed;
        this.animateDark |= filter.animateDark;
        this.animateHideSensitive |= filter.animateHideSensitive;
        this.animateShadowAlpha |= filter.animateShadowAlpha;
        this.hasDelays |= filter.hasDelays;
        this.mAnimatedProperties.addAll(filter.mAnimatedProperties);
    }

    public void reset() {
        this.animateAlpha = false;
        this.animateX = false;
        this.animateY = false;
        this.animateYViews.clear();
        this.animateZ = false;
        this.animateHeight = false;
        this.animateShadowAlpha = false;
        this.animateTopInset = false;
        this.animateDimmed = false;
        this.animateDark = false;
        this.animateHideSensitive = false;
        this.hasDelays = false;
        this.hasGoToFullShadeEvent = false;
        this.hasHeadsUpDisappearClickEvent = false;
        this.mAnimatedProperties.clear();
    }

    public AnimationFilter animate(Property property) {
        this.mAnimatedProperties.add(property);
        return this;
    }

    public boolean shouldAnimateProperty(Property property) {
        return this.mAnimatedProperties.contains(property);
    }
}
