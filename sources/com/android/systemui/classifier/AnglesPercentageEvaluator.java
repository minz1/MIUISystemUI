package com.android.systemui.classifier;

public class AnglesPercentageEvaluator {
    public static float evaluate(float value) {
        float evaluation = 0.0f;
        if (((double) value) < 1.0d) {
            evaluation = 0.0f + 1.0f;
        }
        if (((double) value) < 0.9d) {
            evaluation += 1.0f;
        }
        if (((double) value) < 0.7d) {
            return evaluation + 1.0f;
        }
        return evaluation;
    }
}
