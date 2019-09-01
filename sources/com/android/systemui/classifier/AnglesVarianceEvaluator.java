package com.android.systemui.classifier;

public class AnglesVarianceEvaluator {
    public static float evaluate(float value) {
        float evaluation = 0.0f;
        if (((double) value) > 0.05d) {
            evaluation = 0.0f + 1.0f;
        }
        if (((double) value) > 0.1d) {
            evaluation += 1.0f;
        }
        if (((double) value) > 0.2d) {
            evaluation += 1.0f;
        }
        if (((double) value) > 0.4d) {
            evaluation += 1.0f;
        }
        if (((double) value) > 0.8d) {
            evaluation += 1.0f;
        }
        if (((double) value) > 1.5d) {
            return evaluation + 1.0f;
        }
        return evaluation;
    }
}
