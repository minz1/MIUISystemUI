package com.android.systemui.classifier;

public class SpeedVarianceEvaluator {
    public static float evaluate(float value) {
        float evaluation = 0.0f;
        if (((double) value) > 0.06d) {
            evaluation = 0.0f + 1.0f;
        }
        if (((double) value) > 0.15d) {
            evaluation += 1.0f;
        }
        if (((double) value) > 0.3d) {
            evaluation += 1.0f;
        }
        if (((double) value) > 0.6d) {
            return evaluation + 1.0f;
        }
        return evaluation;
    }
}
