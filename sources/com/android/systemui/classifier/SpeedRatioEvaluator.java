package com.android.systemui.classifier;

public class SpeedRatioEvaluator {
    public static float evaluate(float value) {
        float evaluation = 0.0f;
        if (value == 0.0f) {
            return 0.0f;
        }
        if (((double) value) <= 1.0d) {
            evaluation = 0.0f + 1.0f;
        }
        if (((double) value) <= 0.5d) {
            evaluation += 1.0f;
        }
        if (((double) value) > 9.0d) {
            evaluation += 1.0f;
        }
        if (((double) value) > 18.0d) {
            evaluation += 1.0f;
        }
        return evaluation;
    }
}
