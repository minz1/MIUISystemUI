package com.android.systemui.classifier;

public class Point {
    public long timeOffsetNano;
    public float x;
    public float y;

    public Point(float x2, float y2) {
        this.x = x2;
        this.y = y2;
        this.timeOffsetNano = 0;
    }

    public Point(float x2, float y2, long timeOffsetNano2) {
        this.x = x2;
        this.y = y2;
        this.timeOffsetNano = timeOffsetNano2;
    }

    public boolean equals(Point p) {
        return this.x == p.x && this.y == p.y;
    }

    public float dist(Point a) {
        return (float) Math.hypot((double) (a.x - this.x), (double) (a.y - this.y));
    }

    public float crossProduct(Point a, Point b) {
        return ((a.x - this.x) * (b.y - this.y)) - ((a.y - this.y) * (b.x - this.x));
    }

    public float dotProduct(Point a, Point b) {
        return ((a.x - this.x) * (b.x - this.x)) + ((a.y - this.y) * (b.y - this.y));
    }

    public float getAngle(Point a, Point b) {
        float dist1 = dist(a);
        float dist2 = dist(b);
        if (dist1 == 0.0f || dist2 == 0.0f) {
            return 0.0f;
        }
        float crossProduct = crossProduct(a, b);
        float angle = (float) Math.acos((double) Math.min(1.0f, Math.max(-1.0f, (dotProduct(a, b) / dist1) / dist2)));
        if (((double) crossProduct) < 0.0d) {
            angle = 6.2831855f - angle;
        }
        return angle;
    }
}
