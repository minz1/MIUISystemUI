package com.android.systemui.classifier;

public class SpeedClassifier extends StrokeClassifier {
    private final float NANOS_TO_SECONDS = 1.0E9f;

    public SpeedClassifier(ClassifierData classifierData) {
    }

    public String getTag() {
        return "SPD";
    }

    public float getFalseTouchEvaluation(int type, Stroke stroke) {
        float duration = ((float) stroke.getDurationNanos()) / 1.0E9f;
        if (duration == 0.0f) {
            return SpeedEvaluator.evaluate(0.0f);
        }
        return SpeedEvaluator.evaluate(stroke.getTotalLength() / duration);
    }
}
