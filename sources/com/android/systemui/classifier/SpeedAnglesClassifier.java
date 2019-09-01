package com.android.systemui.classifier;

import android.view.MotionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SpeedAnglesClassifier extends StrokeClassifier {
    private HashMap<Stroke, Data> mStrokeMap = new HashMap<>();

    private static class Data {
        private final float ANGLE_DEVIATION = 0.31415927f;
        private final float DURATION_SCALE = 1.0E8f;
        private final float LENGTH_SCALE = 1.0f;
        private float mAcceleratingAngles = 0.0f;
        private float mAnglesCount = 0.0f;
        private float mCount = 1.0f;
        private float mDist = 0.0f;
        private List<Point> mLastThreePoints = new ArrayList();
        private float mPreviousAngle = 3.1415927f;
        private Point mPreviousPoint = null;
        private float mSum = 0.0f;
        private float mSumSquares = 0.0f;

        public void addPoint(Point point) {
            if (this.mPreviousPoint != null) {
                this.mDist += this.mPreviousPoint.dist(point);
            }
            this.mPreviousPoint = point;
            Point speedPoint = new Point(((float) point.timeOffsetNano) / 1.0E8f, this.mDist / 1.0f);
            if (this.mLastThreePoints.isEmpty() || !this.mLastThreePoints.get(this.mLastThreePoints.size() - 1).equals(speedPoint)) {
                this.mLastThreePoints.add(speedPoint);
                if (this.mLastThreePoints.size() == 4) {
                    this.mLastThreePoints.remove(0);
                    float angle = this.mLastThreePoints.get(1).getAngle(this.mLastThreePoints.get(0), this.mLastThreePoints.get(2));
                    this.mAnglesCount += 1.0f;
                    if (angle >= 2.8274336f) {
                        this.mAcceleratingAngles += 1.0f;
                    }
                    float difference = angle - this.mPreviousAngle;
                    this.mSum += difference;
                    this.mSumSquares += difference * difference;
                    this.mCount = (float) (((double) this.mCount) + 1.0d);
                    this.mPreviousAngle = angle;
                }
            }
        }

        public float getAnglesVariance() {
            return (this.mSumSquares / this.mCount) - ((this.mSum / this.mCount) * (this.mSum / this.mCount));
        }

        public float getAnglesPercentage() {
            if (this.mAnglesCount == 0.0f) {
                return 1.0f;
            }
            return this.mAcceleratingAngles / this.mAnglesCount;
        }
    }

    public SpeedAnglesClassifier(ClassifierData classifierData) {
        this.mClassifierData = classifierData;
    }

    public String getTag() {
        return "SPD_ANG";
    }

    public void onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == 0) {
            this.mStrokeMap.clear();
        }
        for (int i = 0; i < event.getPointerCount(); i++) {
            Stroke stroke = this.mClassifierData.getStroke(event.getPointerId(i));
            if (this.mStrokeMap.get(stroke) == null) {
                this.mStrokeMap.put(stroke, new Data());
            }
            if (!(action == 1 || action == 3 || (action == 6 && i == event.getActionIndex()))) {
                this.mStrokeMap.get(stroke).addPoint(stroke.getPoints().get(stroke.getPoints().size() - 1));
            }
        }
    }

    public float getFalseTouchEvaluation(int type, Stroke stroke) {
        Data data = this.mStrokeMap.get(stroke);
        return SpeedVarianceEvaluator.evaluate(data.getAnglesVariance()) + SpeedAnglesPercentageEvaluator.evaluate(data.getAnglesPercentage());
    }
}
