package com.android.systemui.statusbar.phone;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import com.android.systemui.R;

public class DoubleTapHelper {
    private boolean mActivated;
    private final ActivationListener mActivationListener;
    private float mActivationX;
    private float mActivationY;
    private final DoubleTapListener mDoubleTapListener;
    private final DoubleTapLogListener mDoubleTapLogListener;
    private float mDoubleTapSlop;
    private float mDownX;
    private float mDownY;
    private final SlideBackListener mSlideBackListener;
    private Runnable mTapTimeoutRunnable = new Runnable() {
        public void run() {
            DoubleTapHelper.this.makeInactive();
        }
    };
    private float mTouchSlop;
    private boolean mTrackTouch;
    private final View mView;

    public interface ActivationListener {
        void onActiveChanged(boolean z);
    }

    public interface DoubleTapListener {
        boolean onDoubleTap();
    }

    public interface DoubleTapLogListener {
        void onDoubleTapLog(boolean z, float f, float f2);
    }

    public interface SlideBackListener {
        boolean onSlideBack();
    }

    public DoubleTapHelper(View view, ActivationListener activationListener, DoubleTapListener doubleTapListener, SlideBackListener slideBackListener, DoubleTapLogListener doubleTapLogListener) {
        this.mTouchSlop = (float) ViewConfiguration.get(view.getContext()).getScaledTouchSlop();
        this.mDoubleTapSlop = view.getResources().getDimension(R.dimen.double_tap_slop);
        this.mView = view;
        this.mActivationListener = activationListener;
        this.mDoubleTapListener = doubleTapListener;
        this.mSlideBackListener = slideBackListener;
        this.mDoubleTapLogListener = doubleTapLogListener;
    }

    public boolean onTouchEvent(MotionEvent event) {
        return onTouchEvent(event, Integer.MAX_VALUE);
    }

    public boolean onTouchEvent(MotionEvent event, int maxTouchableHeight) {
        switch (event.getActionMasked()) {
            case 0:
                this.mDownX = event.getX();
                this.mDownY = event.getY();
                this.mTrackTouch = true;
                if (this.mDownY > ((float) maxTouchableHeight)) {
                    this.mTrackTouch = false;
                    break;
                }
                break;
            case 1:
                if (!isWithinTouchSlop(event)) {
                    makeInactive();
                    this.mTrackTouch = false;
                    break;
                } else if (this.mSlideBackListener == null || !this.mSlideBackListener.onSlideBack()) {
                    if (this.mActivated) {
                        boolean withinDoubleTapSlop = isWithinDoubleTapSlop(event);
                        if (this.mDoubleTapLogListener != null) {
                            this.mDoubleTapLogListener.onDoubleTapLog(withinDoubleTapSlop, event.getX() - this.mActivationX, event.getY() - this.mActivationY);
                        }
                        if (!withinDoubleTapSlop) {
                            makeInactive();
                            this.mTrackTouch = false;
                            break;
                        } else if (!this.mDoubleTapListener.onDoubleTap()) {
                            return false;
                        }
                    } else {
                        makeActive();
                        this.mView.postDelayed(this.mTapTimeoutRunnable, 1200);
                        this.mActivationX = event.getX();
                        this.mActivationY = event.getY();
                        break;
                    }
                } else {
                    return true;
                }
                break;
            case 2:
                if (!isWithinTouchSlop(event)) {
                    makeInactive();
                    this.mTrackTouch = false;
                    break;
                }
                break;
            case 3:
                makeInactive();
                this.mTrackTouch = false;
                break;
        }
        return this.mTrackTouch;
    }

    private void makeActive() {
        if (!this.mActivated) {
            this.mActivated = true;
            this.mActivationListener.onActiveChanged(true);
        }
    }

    /* access modifiers changed from: private */
    public void makeInactive() {
        if (this.mActivated) {
            this.mActivated = false;
            this.mActivationListener.onActiveChanged(false);
        }
    }

    private boolean isWithinTouchSlop(MotionEvent event) {
        return Math.abs(event.getX() - this.mDownX) < this.mTouchSlop && Math.abs(event.getY() - this.mDownY) < this.mTouchSlop;
    }

    private boolean isWithinDoubleTapSlop(MotionEvent event) {
        boolean z = true;
        if (!this.mActivated) {
            return true;
        }
        if (Math.abs(event.getX() - this.mActivationX) >= this.mDoubleTapSlop || Math.abs(event.getY() - this.mActivationY) >= this.mDoubleTapSlop) {
            z = false;
        }
        return z;
    }
}
