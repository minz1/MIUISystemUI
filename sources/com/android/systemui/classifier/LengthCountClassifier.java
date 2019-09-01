package com.android.systemui.classifier;

public class LengthCountClassifier extends StrokeClassifier {
    public LengthCountClassifier(ClassifierData classifierData) {
    }

    public String getTag() {
        return "LEN_CNT";
    }

    public float getFalseTouchEvaluation(int type, Stroke stroke) {
        return LengthCountEvaluator.evaluate(stroke.getTotalLength() / Math.max(1.0f, (float) (stroke.getCount() - 2)));
    }
}
