package com.android.keyguard;

import android.animation.TimeInterpolator;

public class Ease {

    public static class Cubic {
        public static final TimeInterpolator easeIn = new TimeInterpolator() {
            public float getInterpolation(float input) {
                float f = input / 1.0f;
                float input2 = f;
                return (1.0f * f * input2 * input2) + 0.0f;
            }
        };
        public static final TimeInterpolator easeInOut = new TimeInterpolator() {
            public float getInterpolation(float input) {
                float f = input / 0.5f;
                float input2 = f;
                if (f < 1.0f) {
                    return (0.5f * input2 * input2 * input2) + 0.0f;
                }
                float f2 = input2 - 2.0f;
                float input3 = f2;
                return (0.5f * ((f2 * input3 * input3) + 2.0f)) + 0.0f;
            }
        };
        public static final TimeInterpolator easeOut = new TimeInterpolator() {
            public float getInterpolation(float input) {
                float f = (input / 1.0f) - 1.0f;
                float input2 = f;
                return (1.0f * ((f * input2 * input2) + 1.0f)) + 0.0f;
            }
        };
    }

    public static class Quad {
        public static final TimeInterpolator easeIn = new TimeInterpolator() {
            public float getInterpolation(float input) {
                float input2 = input / 1.0f;
                return (1.0f * input2 * input2) + 0.0f;
            }
        };
        public static final TimeInterpolator easeInOut = new TimeInterpolator() {
            public float getInterpolation(float input) {
                float f = input / 0.5f;
                float input2 = f;
                if (f < 1.0f) {
                    return (0.5f * input2 * input2) + 0.0f;
                }
                float input3 = input2 - 1.0f;
                return (-0.5f * ((input3 * (input3 - 2.0f)) - 1.0f)) + 0.0f;
            }
        };
        public static final TimeInterpolator easeOut = new TimeInterpolator() {
            public float getInterpolation(float input) {
                float input2 = input / 1.0f;
                return (-1.0f * input2 * (input2 - 2.0f)) + 0.0f;
            }
        };
    }

    public static class Quint {
        public static final TimeInterpolator easeIn = new TimeInterpolator() {
            public float getInterpolation(float input) {
                float f = input / 1.0f;
                float input2 = f;
                return (1.0f * f * input2 * input2 * input2 * input2) + 0.0f;
            }
        };
        public static final TimeInterpolator easeInOut = new TimeInterpolator() {
            public float getInterpolation(float input) {
                float f = input / 0.5f;
                float input2 = f;
                if (f < 1.0f) {
                    return (0.5f * input2 * input2 * input2 * input2 * input2) + 0.0f;
                }
                float f2 = input2 - 2.0f;
                float input3 = f2;
                return (0.5f * ((f2 * input3 * input3 * input3 * input3) + 2.0f)) + 0.0f;
            }
        };
        public static final TimeInterpolator easeOut = new TimeInterpolator() {
            public float getInterpolation(float input) {
                float f = (input / 1.0f) - 1.0f;
                float input2 = f;
                return (1.0f * ((f * input2 * input2 * input2 * input2) + 1.0f)) + 0.0f;
            }
        };
    }

    public static class Sine {
        public static final TimeInterpolator easeIn = new TimeInterpolator() {
            public float getInterpolation(float input) {
                return (-1.0f * ((float) Math.cos(((double) (input / 1.0f)) * 1.5707963267948966d))) + 1.0f + 0.0f;
            }
        };
        public static final TimeInterpolator easeInOut = new TimeInterpolator() {
            public float getInterpolation(float input) {
                return (-0.5f * (((float) Math.cos((3.141592653589793d * ((double) input)) / 1.0d)) - 1.0f)) + 0.0f;
            }
        };
        public static final TimeInterpolator easeOut = new TimeInterpolator() {
            public float getInterpolation(float input) {
                return (1.0f * ((float) Math.sin(((double) (input / 1.0f)) * 1.5707963267948966d))) + 0.0f;
            }
        };
    }
}
