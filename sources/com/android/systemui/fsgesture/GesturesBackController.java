package com.android.systemui.fsgesture;

import android.util.Log;
import android.view.MotionEvent;

public class GesturesBackController implements IPointerEventListener {
    private GesturesBackCallback mCallback;
    private long mContinuousBackFinishTime;
    private float mDownX;
    private float mDownY;
    private int mDragDirection = -1;
    int mGestureEdgeLeft;
    int mGestureEdgeRight;
    private volatile boolean mIsGestureAnimationEnabled = true;
    private int mSwipeStatus = 4;
    private float mWithoutAnimatingDownX;
    private int mWithoutAnimatingDragDirection = -1;

    public interface GesturesBackCallback {
        void onSwipeProcess(boolean z, float f);

        void onSwipeStart(boolean z, float f);

        void onSwipeStop(boolean z, float f);

        void onSwipeStopDirect();
    }

    public GesturesBackController(GesturesBackCallback callback, int gestureEdgeLeft, int gestureEdgeRight) {
        this.mCallback = callback;
        this.mGestureEdgeLeft = gestureEdgeLeft;
        this.mGestureEdgeRight = gestureEdgeRight;
    }

    public void onPointerEvent(MotionEvent motionEvent) {
        Log.d("GesturesBackController", "onPointerEvent swipeStatus:" + this.mSwipeStatus);
        if (this.mSwipeStatus != 16) {
            processPointerEvent(motionEvent);
            Log.d("GesturesBackController", "mSwipeStatus != SWIPE_STATUS_ANIMATING, processPointerEvent");
        } else if (motionEvent.getEventTime() - this.mContinuousBackFinishTime >= 300) {
            this.mSwipeStatus = 4;
            if (motionEvent.getActionMasked() == 0) {
                processPointerEvent(motionEvent);
                Log.d("GesturesBackController", "mSwipeStatus == SWIPE_STATUS_ANIMATING, processPointerEvent");
            }
        } else {
            processPointerEventWithoutAnimating(motionEvent);
            Log.d("GesturesBackController", "mSwipeStatus == SWIPE_STATUS_ANIMATING, processPointerEventWithoutAnimating");
        }
    }

    private void processPointerEventWithoutAnimating(MotionEvent motionEvent) {
        float currX = motionEvent.getRawX();
        float currY = motionEvent.getRawY();
        this.mContinuousBackFinishTime = motionEvent.getEventTime();
        Log.d("GesturesBackController", "processPointerEventWithoutAnimating currX:" + currX + " currY:" + currY + " mDragDirection:" + this.mDragDirection);
        int actionMasked = motionEvent.getActionMasked();
        if (actionMasked != 3) {
            switch (actionMasked) {
                case 0:
                    this.mWithoutAnimatingDownX = currX;
                    if (currX > ((float) this.mGestureEdgeLeft)) {
                        if (currX >= ((float) this.mGestureEdgeRight)) {
                            this.mWithoutAnimatingDragDirection = 2;
                            break;
                        }
                    } else {
                        this.mWithoutAnimatingDragDirection = 1;
                        break;
                    }
                    break;
                case 1:
                    break;
            }
        }
        if (this.mWithoutAnimatingDragDirection != -1) {
            float offsetX = this.mWithoutAnimatingDragDirection == 1 ? currX - this.mWithoutAnimatingDownX : this.mWithoutAnimatingDownX - currX;
            int diffTime = (int) (motionEvent.getEventTime() - motionEvent.getDownTime());
            int speed = (int) (offsetX / ((float) diffTime));
            if (offsetX / ((float) diffTime) > 2.0f) {
                this.mCallback.onSwipeStopDirect();
            }
            Log.d("GesturesBackController", "processPointerEventWithoutAnimating MotionEvent.ACTION_UP offsetX:" + offsetX + " diffTime:" + diffTime + " speed:" + speed);
            this.mWithoutAnimatingDragDirection = -1;
        }
    }

    private void processPointerEvent(MotionEvent motionEvent) {
        float currX = motionEvent.getRawX();
        float currY = motionEvent.getRawY();
        switch (motionEvent.getActionMasked()) {
            case 0:
                this.mDownX = currX;
                this.mDownY = currY;
                if (currX <= ((float) this.mGestureEdgeLeft)) {
                    this.mSwipeStatus = 8;
                    this.mDragDirection = 1;
                    return;
                } else if (currX >= ((float) this.mGestureEdgeRight)) {
                    this.mSwipeStatus = 8;
                    this.mDragDirection = 2;
                    return;
                } else {
                    this.mSwipeStatus = 1;
                    return;
                }
            case 1:
            case 3:
                if (this.mSwipeStatus == 2) {
                    int diffTime = (int) (motionEvent.getEventTime() - motionEvent.getDownTime());
                    float finalRawOffsetX = this.mDragDirection == 1 ? currX - this.mDownX : this.mDownX - currX;
                    int speed = (int) (finalRawOffsetX / ((float) diffTime));
                    boolean isFinish = isFinished(finalRawOffsetX, speed);
                    if (this.mIsGestureAnimationEnabled) {
                        this.mSwipeStatus = 16;
                        this.mCallback.onSwipeStop(isFinish, finalRawOffsetX);
                    } else if (isFinish) {
                        this.mCallback.onSwipeStopDirect();
                    }
                    this.mContinuousBackFinishTime = motionEvent.getEventTime();
                    Log.d("GesturesBackController", "onPointerEvent MotionEvent.ACTION_UP stopGestures isFinish:" + isFinish + " speed:" + speed);
                }
                this.mDragDirection = -1;
                return;
            case 2:
                if (this.mSwipeStatus != 1) {
                    float rawOffsetX = this.mDragDirection == 1 ? currX - this.mDownX : this.mDownX - currX;
                    float offsetY = Math.abs(currY - this.mDownY);
                    if (this.mSwipeStatus == 8 && rawOffsetX >= 20.0f && rawOffsetX >= offsetY / 2.0f) {
                        this.mSwipeStatus = 2;
                        this.mCallback.onSwipeStart(this.mIsGestureAnimationEnabled, currY);
                    }
                    if (this.mSwipeStatus == 2) {
                        Log.d("GesturesBackController", "onPointerEvent MotionEvent.ACTION_MOVE processMiuiGestures");
                        if (this.mIsGestureAnimationEnabled) {
                            this.mCallback.onSwipeProcess(isFinished(rawOffsetX, (int) (rawOffsetX / ((float) ((int) (motionEvent.getEventTime() - motionEvent.getDownTime()))))), rawOffsetX);
                            return;
                        }
                        return;
                    }
                    return;
                }
                return;
            default:
                return;
        }
    }

    /* access modifiers changed from: package-private */
    public void setGestureEdgeWidth(int gestureEdgeLeft, int gestureEdgeRight) {
        this.mGestureEdgeLeft = gestureEdgeLeft;
        this.mGestureEdgeRight = gestureEdgeRight;
    }

    /* access modifiers changed from: package-private */
    public void enableGestureBackAnimation(boolean enable) {
        this.mIsGestureAnimationEnabled = enable;
    }

    static float convertOffset(float rawOffsetX) {
        if (rawOffsetX < 0.0f) {
            return 0.0f;
        }
        return (float) (10.0d - (Math.sin((((double) (90.0f + (Math.min(rawOffsetX, 360.0f) / 2.0f))) * 3.141592653589793d) / 180.0d) * 10.0d));
    }

    static boolean isFinished(float rawOffsetX, int speed) {
        return rawOffsetX >= 0.0f && (90.0f + (Math.min(rawOffsetX, 360.0f) / 2.0f) > 180.0f || speed > 2);
    }

    /* access modifiers changed from: package-private */
    public void reset() {
        this.mSwipeStatus = 1;
    }
}
