package com.android.systemui.classifier;

import android.os.SystemClock;
import java.util.ArrayList;

public class HistoryEvaluator {
    private final ArrayList<Data> mGestureWeights = new ArrayList<>();
    private long mLastUpdate = SystemClock.elapsedRealtime();
    private final ArrayList<Data> mStrokes = new ArrayList<>();

    private static class Data {
        public float evaluation;
        public float weight = 1.0f;

        public Data(float evaluation2) {
            this.evaluation = evaluation2;
        }
    }

    public void addStroke(float evaluation) {
        decayValue();
        this.mStrokes.add(new Data(evaluation));
    }

    public void addGesture(float evaluation) {
        decayValue();
        this.mGestureWeights.add(new Data(evaluation));
    }

    public float getEvaluation() {
        return weightedAverage(this.mStrokes) + weightedAverage(this.mGestureWeights);
    }

    private float weightedAverage(ArrayList<Data> list) {
        float sumValue = 0.0f;
        float sumWeight = 0.0f;
        int size = list.size();
        for (int i = 0; i < size; i++) {
            Data data = list.get(i);
            sumValue += data.evaluation * data.weight;
            sumWeight += data.weight;
        }
        if (sumWeight == 0.0f) {
            return 0.0f;
        }
        return sumValue / sumWeight;
    }

    private void decayValue() {
        long time = SystemClock.elapsedRealtime();
        if (time > this.mLastUpdate) {
            float factor = (float) Math.pow(0.8999999761581421d, (double) (((float) (time - this.mLastUpdate)) / 50.0f));
            decayValue(this.mStrokes, factor);
            decayValue(this.mGestureWeights, factor);
            this.mLastUpdate = time;
        }
    }

    private void decayValue(ArrayList<Data> list, float factor) {
        int size = list.size();
        for (int i = 0; i < size; i++) {
            list.get(i).weight *= factor;
        }
        while (list.isEmpty() == 0 && isZero(list.get(0).weight)) {
            list.remove(0);
        }
    }

    private boolean isZero(float x) {
        return x <= 1.0E-5f && x >= -1.0E-5f;
    }
}
