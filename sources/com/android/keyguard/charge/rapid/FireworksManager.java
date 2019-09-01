package com.android.keyguard.charge.rapid;

import android.graphics.PointF;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;

class FireworksManager {
    private int mDistance;
    private List<PointF> mFireList = new LinkedList();
    private int mLastIndex;
    private Random mRandom;
    private float mSpeed;

    FireworksManager(int distance, float speed) {
        this.mDistance = distance;
        this.mSpeed = speed;
        this.mRandom = new Random(System.currentTimeMillis());
    }

    /* access modifiers changed from: package-private */
    public void updateDistanceAndSpeed(int distance, float speed) {
        this.mDistance = distance;
        this.mSpeed = speed;
        this.mFireList.clear();
    }

    /* access modifiers changed from: package-private */
    public void freshPositions(List<PointF> newPositions, long elapseTime) {
        if (newPositions != null) {
            float distanceStep = ((float) elapseTime) * this.mSpeed;
            ListIterator<PointF> iterator = this.mFireList.listIterator();
            while (iterator.hasNext()) {
                PointF pointF = iterator.next();
                pointF.y -= distanceStep;
                if (pointF.y <= 0.0f) {
                    iterator.remove();
                }
            }
            newPositions.clear();
            newPositions.addAll(this.mFireList);
        }
    }

    /* access modifiers changed from: package-private */
    public void fire() {
        int index = this.mRandom.nextInt(5);
        int tryCount = 1;
        while (Math.abs(index - this.mLastIndex) <= 1 && tryCount < 6) {
            index = this.mRandom.nextInt(5);
            tryCount++;
        }
        if (index >= 0 && index < 5) {
            PointF pointF = new PointF();
            pointF.x = (float) index;
            pointF.y = (float) this.mDistance;
            this.mLastIndex = index;
            this.mFireList.add(pointF);
        }
    }
}
