package com.android.systemui.classifier;

import android.view.MotionEvent;
import java.util.HashMap;

public class AccelerationClassifier extends StrokeClassifier {
    private final HashMap<Stroke, Data> mStrokeMap = new HashMap<>();

    private static class Data {
        float maxSpeedRatio = 0.0f;
        Point previousPoint;
        float previousSpeed = 0.0f;

        public Data(Point point) {
            this.previousPoint = point;
        }

        public void addPoint(Point point) {
            float distance = this.previousPoint.dist(point);
            float duration = (float) ((point.timeOffsetNano - this.previousPoint.timeOffsetNano) + 1);
            float speed = distance / duration;
            if (duration > 2.0E7f || duration < 5000000.0f) {
                this.previousSpeed = 0.0f;
                this.previousPoint = point;
                return;
            }
            if (this.previousSpeed != 0.0f) {
                this.maxSpeedRatio = Math.max(this.maxSpeedRatio, speed / this.previousSpeed);
            }
            this.previousSpeed = speed;
            this.previousPoint = point;
        }
    }

    public AccelerationClassifier(ClassifierData classifierData) {
        this.mClassifierData = classifierData;
    }

    public String getTag() {
        return "ACC";
    }

    public void onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == 0) {
            this.mStrokeMap.clear();
        }
        for (int i = 0; i < event.getPointerCount(); i++) {
            Stroke stroke = this.mClassifierData.getStroke(event.getPointerId(i));
            Point point = stroke.getPoints().get(stroke.getPoints().size() - 1);
            if (this.mStrokeMap.get(stroke) == null) {
                this.mStrokeMap.put(stroke, new Data(point));
            } else {
                this.mStrokeMap.get(stroke).addPoint(point);
            }
        }
    }

    public float getFalseTouchEvaluation(int type, Stroke stroke) {
        return 2.0f * SpeedRatioEvaluator.evaluate(this.mStrokeMap.get(stroke).maxSpeedRatio);
    }
}
