package com.android.systemui.qs;

import android.util.FloatProperty;
import android.util.MathUtils;
import android.util.Property;
import android.view.View;
import android.view.animation.Interpolator;
import java.util.ArrayList;
import java.util.List;

public class TouchAnimator {
    /* access modifiers changed from: private */
    public static final FloatProperty<TouchAnimator> POSITION = new FloatProperty<TouchAnimator>("position") {
        public void setValue(TouchAnimator touchAnimator, float value) {
            touchAnimator.setPosition(value);
        }

        public Float get(TouchAnimator touchAnimator) {
            return Float.valueOf(touchAnimator.mLastT);
        }
    };
    private final float mEndDelay;
    private final Interpolator mInterpolator;
    private final KeyframeSet[] mKeyframeSets;
    /* access modifiers changed from: private */
    public float mLastT;
    private final Listener mListener;
    private final float mSpan;
    private final float mStartDelay;
    private final Object[] mTargets;

    public static class Builder {
        private float mEndDelay;
        private Interpolator mInterpolator;
        private Listener mListener;
        private float mStartDelay;
        private List<Object> mTargets = new ArrayList();
        private List<KeyframeSet> mValues = new ArrayList();

        public Builder addFloat(Object target, String property, float... values) {
            add(target, KeyframeSet.ofFloat(getProperty(target, property, Float.TYPE), values));
            return this;
        }

        private void add(Object target, KeyframeSet keyframeSet) {
            this.mTargets.add(target);
            this.mValues.add(keyframeSet);
        }

        private static Property getProperty(Object target, String property, Class<?> cls) {
            if (target instanceof View) {
                char c = 65535;
                switch (property.hashCode()) {
                    case -1225497657:
                        if (property.equals("translationX")) {
                            c = 0;
                            break;
                        }
                        break;
                    case -1225497656:
                        if (property.equals("translationY")) {
                            c = 1;
                            break;
                        }
                        break;
                    case -1225497655:
                        if (property.equals("translationZ")) {
                            c = 2;
                            break;
                        }
                        break;
                    case -908189618:
                        if (property.equals("scaleX")) {
                            c = 7;
                            break;
                        }
                        break;
                    case -908189617:
                        if (property.equals("scaleY")) {
                            c = 8;
                            break;
                        }
                        break;
                    case -40300674:
                        if (property.equals("rotation")) {
                            c = 4;
                            break;
                        }
                        break;
                    case 120:
                        if (property.equals("x")) {
                            c = 5;
                            break;
                        }
                        break;
                    case 121:
                        if (property.equals("y")) {
                            c = 6;
                            break;
                        }
                        break;
                    case 92909918:
                        if (property.equals("alpha")) {
                            c = 3;
                            break;
                        }
                        break;
                }
                switch (c) {
                    case 0:
                        return View.TRANSLATION_X;
                    case 1:
                        return View.TRANSLATION_Y;
                    case 2:
                        return View.TRANSLATION_Z;
                    case 3:
                        return View.ALPHA;
                    case 4:
                        return View.ROTATION;
                    case 5:
                        return View.X;
                    case 6:
                        return View.Y;
                    case 7:
                        return View.SCALE_X;
                    case 8:
                        return View.SCALE_Y;
                }
            }
            if (!(target instanceof TouchAnimator) || !"position".equals(property)) {
                return Property.of(target.getClass(), cls, property);
            }
            return TouchAnimator.POSITION;
        }

        public Builder setStartDelay(float startDelay) {
            this.mStartDelay = startDelay;
            return this;
        }

        public Builder setInterpolator(Interpolator intepolator) {
            this.mInterpolator = intepolator;
            return this;
        }

        public Builder setListener(Listener listener) {
            this.mListener = listener;
            return this;
        }

        public TouchAnimator build() {
            TouchAnimator touchAnimator = new TouchAnimator(this.mTargets.toArray(new Object[this.mTargets.size()]), (KeyframeSet[]) this.mValues.toArray(new KeyframeSet[this.mValues.size()]), this.mStartDelay, this.mEndDelay, this.mInterpolator, this.mListener);
            return touchAnimator;
        }
    }

    private static class FloatKeyframeSet<T> extends KeyframeSet {
        private final Property<T, Float> mProperty;
        private final float[] mValues;

        public FloatKeyframeSet(Property<T, Float> property, float[] values) {
            super(values.length);
            this.mProperty = property;
            this.mValues = values;
        }

        /* access modifiers changed from: protected */
        public void interpolate(int index, float amount, Object target) {
            float firstFloat = this.mValues[index - 1];
            this.mProperty.set(target, Float.valueOf(((this.mValues[index] - firstFloat) * amount) + firstFloat));
        }
    }

    private static abstract class KeyframeSet {
        private final float mFrameWidth;
        private final int mSize;

        /* access modifiers changed from: protected */
        public abstract void interpolate(int i, float f, Object obj);

        public KeyframeSet(int size) {
            this.mSize = size;
            this.mFrameWidth = 1.0f / ((float) (size - 1));
        }

        /* access modifiers changed from: package-private */
        public void setValue(float fraction, Object target) {
            int i = 1;
            while (i < this.mSize - 1 && fraction > this.mFrameWidth) {
                i++;
            }
            interpolate(i, (fraction / this.mFrameWidth) - ((float) (i - 1)), target);
        }

        public static KeyframeSet ofFloat(Property property, float... values) {
            return new FloatKeyframeSet(property, values);
        }
    }

    public interface Listener {
        void onAnimationAtEnd();

        void onAnimationAtStart();

        void onAnimationStarted();
    }

    public static class ListenerAdapter implements Listener {
        public void onAnimationAtStart() {
        }

        public void onAnimationAtEnd() {
        }

        public void onAnimationStarted() {
        }
    }

    private TouchAnimator(Object[] targets, KeyframeSet[] keyframeSets, float startDelay, float endDelay, Interpolator interpolator, Listener listener) {
        this.mLastT = -1.0f;
        this.mTargets = targets;
        this.mKeyframeSets = keyframeSets;
        this.mStartDelay = startDelay;
        this.mEndDelay = endDelay;
        this.mSpan = (1.0f - this.mEndDelay) - this.mStartDelay;
        this.mInterpolator = interpolator;
        this.mListener = listener;
    }

    public void setPosition(float fraction) {
        float t = MathUtils.constrain((fraction - this.mStartDelay) / this.mSpan, 0.0f, 1.0f);
        if (this.mInterpolator != null) {
            t = this.mInterpolator.getInterpolation(t);
        }
        if (t != this.mLastT) {
            if (this.mListener != null) {
                if (t == 1.0f) {
                    this.mListener.onAnimationAtEnd();
                } else if (t == 0.0f) {
                    this.mListener.onAnimationAtStart();
                } else if (this.mLastT <= 0.0f || this.mLastT == 1.0f) {
                    this.mListener.onAnimationStarted();
                }
                this.mLastT = t;
            }
            for (int i = 0; i < this.mTargets.length; i++) {
                this.mKeyframeSets[i].setValue(t, this.mTargets[i]);
            }
        }
    }
}
