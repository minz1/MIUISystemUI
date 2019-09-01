package com.android.systemui.classifier;

public class LengthCountEvaluator {
    public static float evaluate(float value) {
        float evaluation = 0.0f;
        if (((double) value) < 0.09d) {
            evaluation = 0.0f + 1.0f;
        }
        if (((double) value) < 0.05d) {
            evaluation += 1.0f;
        }
        if (((double) value) < 0.02d) {
            evaluation += 1.0f;
        }
        if (((double) value) > 0.6d) {
            evaluation += 1.0f;
        }
        if (((double) value) > 0.9d) {
            evaluation += 1.0f;
        }
        if (((double) value) > 1.2d) {
            return evaluation + 1.0f;
        }
        return evaluation;
    }
}
