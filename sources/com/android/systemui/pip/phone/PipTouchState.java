package com.android.systemui.pip.phone;

import android.graphics.PointF;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import java.io.PrintWriter;

public class PipTouchState {
    private int mActivePointerId;
    private boolean mAllowDraggingOffscreen = false;
    private boolean mAllowTouches = true;
    private final PointF mDownDelta = new PointF();
    private final PointF mDownTouch = new PointF();
    private boolean mIsDragging = false;
    private boolean mIsUserInteracting = false;
    private final PointF mLastDelta = new PointF();
    private final PointF mLastTouch = new PointF();
    private boolean mStartedDragging = false;
    private final PointF mVelocity = new PointF();
    private VelocityTracker mVelocityTracker;
    private ViewConfiguration mViewConfig;

    public PipTouchState(ViewConfiguration viewConfig) {
        this.mViewConfig = viewConfig;
    }

    public void reset() {
        this.mAllowDraggingOffscreen = false;
        this.mIsDragging = false;
        this.mStartedDragging = false;
        this.mIsUserInteracting = false;
    }

    /* JADX WARNING: Can't fix incorrect switch cases order */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void onTouchEvent(android.view.MotionEvent r9) {
        /*
            r8 = this;
            int r0 = r9.getAction()
            r1 = 6
            r2 = 0
            r3 = 1
            if (r0 == r1) goto L_0x012b
            r1 = -1
            switch(r0) {
                case 0: goto L_0x00ec;
                case 1: goto L_0x008e;
                case 2: goto L_0x000f;
                case 3: goto L_0x00e7;
                default: goto L_0x000d;
            }
        L_0x000d:
            goto L_0x0171
        L_0x000f:
            boolean r0 = r8.mIsUserInteracting
            if (r0 != 0) goto L_0x0015
            goto L_0x0171
        L_0x0015:
            android.view.VelocityTracker r0 = r8.mVelocityTracker
            r0.addMovement(r9)
            int r0 = r8.mActivePointerId
            int r0 = r9.findPointerIndex(r0)
            if (r0 != r1) goto L_0x003c
            java.lang.String r1 = "PipTouchHandler"
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            java.lang.String r3 = "Invalid active pointer id on MOVE: "
            r2.append(r3)
            int r3 = r8.mActivePointerId
            r2.append(r3)
            java.lang.String r2 = r2.toString()
            android.util.Log.e(r1, r2)
            goto L_0x0171
        L_0x003c:
            float r1 = r9.getX(r0)
            float r4 = r9.getY(r0)
            android.graphics.PointF r5 = r8.mLastDelta
            android.graphics.PointF r6 = r8.mLastTouch
            float r6 = r6.x
            float r6 = r1 - r6
            android.graphics.PointF r7 = r8.mLastTouch
            float r7 = r7.y
            float r7 = r4 - r7
            r5.set(r6, r7)
            android.graphics.PointF r5 = r8.mDownDelta
            android.graphics.PointF r6 = r8.mDownTouch
            float r6 = r6.x
            float r6 = r1 - r6
            android.graphics.PointF r7 = r8.mDownTouch
            float r7 = r7.y
            float r7 = r4 - r7
            r5.set(r6, r7)
            android.graphics.PointF r5 = r8.mDownDelta
            float r5 = r5.length()
            android.view.ViewConfiguration r6 = r8.mViewConfig
            int r6 = r6.getScaledTouchSlop()
            float r6 = (float) r6
            int r5 = (r5 > r6 ? 1 : (r5 == r6 ? 0 : -1))
            if (r5 <= 0) goto L_0x0079
            r5 = r3
            goto L_0x007a
        L_0x0079:
            r5 = r2
        L_0x007a:
            boolean r6 = r8.mIsDragging
            if (r6 != 0) goto L_0x0085
            if (r5 == 0) goto L_0x0087
            r8.mIsDragging = r3
            r8.mStartedDragging = r3
            goto L_0x0087
        L_0x0085:
            r8.mStartedDragging = r2
        L_0x0087:
            android.graphics.PointF r2 = r8.mLastTouch
            r2.set(r1, r4)
            goto L_0x0171
        L_0x008e:
            boolean r0 = r8.mIsUserInteracting
            if (r0 != 0) goto L_0x0094
            goto L_0x0171
        L_0x0094:
            android.view.VelocityTracker r0 = r8.mVelocityTracker
            r0.addMovement(r9)
            android.view.VelocityTracker r0 = r8.mVelocityTracker
            r2 = 1000(0x3e8, float:1.401E-42)
            android.view.ViewConfiguration r3 = r8.mViewConfig
            int r3 = r3.getScaledMaximumFlingVelocity()
            float r3 = (float) r3
            r0.computeCurrentVelocity(r2, r3)
            android.graphics.PointF r0 = r8.mVelocity
            android.view.VelocityTracker r2 = r8.mVelocityTracker
            float r2 = r2.getXVelocity()
            android.view.VelocityTracker r3 = r8.mVelocityTracker
            float r3 = r3.getYVelocity()
            r0.set(r2, r3)
            int r0 = r8.mActivePointerId
            int r0 = r9.findPointerIndex(r0)
            if (r0 != r1) goto L_0x00da
            java.lang.String r1 = "PipTouchHandler"
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            java.lang.String r3 = "Invalid active pointer id on UP: "
            r2.append(r3)
            int r3 = r8.mActivePointerId
            r2.append(r3)
            java.lang.String r2 = r2.toString()
            android.util.Log.e(r1, r2)
            goto L_0x0171
        L_0x00da:
            android.graphics.PointF r1 = r8.mLastTouch
            float r2 = r9.getX(r0)
            float r3 = r9.getY(r0)
            r1.set(r2, r3)
        L_0x00e7:
            r8.recycleVelocityTracker()
            goto L_0x0171
        L_0x00ec:
            boolean r0 = r8.mAllowTouches
            if (r0 != 0) goto L_0x00f1
            return
        L_0x00f1:
            r8.initOrResetVelocityTracker()
            int r0 = r9.getPointerId(r2)
            r8.mActivePointerId = r0
            java.lang.String r0 = "PipTouchHandler"
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r1.<init>()
            java.lang.String r2 = "Setting active pointer id on DOWN: "
            r1.append(r2)
            int r2 = r8.mActivePointerId
            r1.append(r2)
            java.lang.String r1 = r1.toString()
            android.util.Log.e(r0, r1)
            android.graphics.PointF r0 = r8.mLastTouch
            float r1 = r9.getX()
            float r2 = r9.getY()
            r0.set(r1, r2)
            android.graphics.PointF r0 = r8.mDownTouch
            android.graphics.PointF r1 = r8.mLastTouch
            r0.set(r1)
            r8.mAllowDraggingOffscreen = r3
            r8.mIsUserInteracting = r3
            goto L_0x0171
        L_0x012b:
            boolean r0 = r8.mIsUserInteracting
            if (r0 != 0) goto L_0x0130
            goto L_0x0171
        L_0x0130:
            android.view.VelocityTracker r0 = r8.mVelocityTracker
            r0.addMovement(r9)
            int r0 = r9.getActionIndex()
            int r1 = r9.getPointerId(r0)
            int r4 = r8.mActivePointerId
            if (r1 != r4) goto L_0x0171
            if (r0 != 0) goto L_0x0145
            r2 = r3
        L_0x0145:
            int r3 = r9.getPointerId(r2)
            r8.mActivePointerId = r3
            java.lang.String r3 = "PipTouchHandler"
            java.lang.StringBuilder r4 = new java.lang.StringBuilder
            r4.<init>()
            java.lang.String r5 = "Relinquish active pointer id on POINTER_UP: "
            r4.append(r5)
            int r5 = r8.mActivePointerId
            r4.append(r5)
            java.lang.String r4 = r4.toString()
            android.util.Log.e(r3, r4)
            android.graphics.PointF r3 = r8.mLastTouch
            float r4 = r9.getX(r2)
            float r5 = r9.getY(r2)
            r3.set(r4, r5)
        L_0x0171:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.pip.phone.PipTouchState.onTouchEvent(android.view.MotionEvent):void");
    }

    public PointF getVelocity() {
        return this.mVelocity;
    }

    public PointF getLastTouchPosition() {
        return this.mLastTouch;
    }

    public PointF getLastTouchDelta() {
        return this.mLastDelta;
    }

    public PointF getDownTouchPosition() {
        return this.mDownTouch;
    }

    public boolean isDragging() {
        return this.mIsDragging;
    }

    public boolean isUserInteracting() {
        return this.mIsUserInteracting;
    }

    public boolean startedDragging() {
        return this.mStartedDragging;
    }

    public void setAllowTouches(boolean allowTouches) {
        this.mAllowTouches = allowTouches;
        if (this.mIsUserInteracting) {
            reset();
        }
    }

    public boolean allowDraggingOffscreen() {
        return this.mAllowDraggingOffscreen;
    }

    private void initOrResetVelocityTracker() {
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        } else {
            this.mVelocityTracker.clear();
        }
    }

    private void recycleVelocityTracker() {
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.recycle();
            this.mVelocityTracker = null;
        }
    }

    public void dump(PrintWriter pw, String prefix) {
        String innerPrefix = prefix + "  ";
        pw.println(prefix + "PipTouchHandler");
        pw.println(innerPrefix + "mAllowTouches=" + this.mAllowTouches);
        pw.println(innerPrefix + "mActivePointerId=" + this.mActivePointerId);
        pw.println(innerPrefix + "mDownTouch=" + this.mDownTouch);
        pw.println(innerPrefix + "mDownDelta=" + this.mDownDelta);
        pw.println(innerPrefix + "mLastTouch=" + this.mLastTouch);
        pw.println(innerPrefix + "mLastDelta=" + this.mLastDelta);
        pw.println(innerPrefix + "mVelocity=" + this.mVelocity);
        pw.println(innerPrefix + "mIsUserInteracting=" + this.mIsUserInteracting);
        pw.println(innerPrefix + "mIsDragging=" + this.mIsDragging);
        pw.println(innerPrefix + "mStartedDragging=" + this.mStartedDragging);
        pw.println(innerPrefix + "mAllowDraggingOffscreen=" + this.mAllowDraggingOffscreen);
    }
}
