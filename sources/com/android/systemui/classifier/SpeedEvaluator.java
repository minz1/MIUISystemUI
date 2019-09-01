package com.android.systemui.classifier;

public class SpeedEvaluator {
    public static float evaluate(float value) {
        float evaluation = 0.0f;
        if (((double) value) < 4.0d) {
            evaluation = 0.0f + 1.0f;
        }
        if (((double) value) < 2.2d) {
            evaluation += 1.0f;
        }
        if (((double) value) > 35.0d) {
            evaluation += 1.0f;
        }
        if (((double) value) > 50.0d) {
            return evaluation + 1.0f;
        }
        return evaluation;
    }
}
