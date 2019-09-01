package com.android.keyguard;

import android.content.Context;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class HeiHeiGestureView extends FrameLayout {
    private static final float DENSITY = Resources.getSystem().getDisplayMetrics().density;
    private static final float MOVE_DOWN_DISTANCE_THREDHOLD = (DENSITY * 100.0f);
    private static final float MOVE_UP_DISTANCE_THREDHOLD = (100.0f * DENSITY);
    private static final float TWO_POINTS_DISTANCE_X_THREDHOLD = (150.0f * DENSITY);
    private static final float TWO_POINTS_DISTANCE_Y_THREDHOLD = (300.0f * DENSITY);
    private static final float TWO_POINTS_DISTANCE_Y_THREDHOLD_MIN = (50.0f * DENSITY);
    private float mBottomY;
    private double[] mChances = new double[0];
    private int mCurrentPicture;
    private String mCurrentSound;
    private float mFirstY;
    /* access modifiers changed from: private */
    public ImageView mImageView;
    private long mLastMatchTime;
    private long mLastTiggerTime;
    private OnTriggerListener mListener;
    private int[] mPictures;
    private MediaPlayer mPlayer;
    private String[] mSounds;
    private DetectingStage mStage;
    private float mTopY;

    enum DetectingStage {
        STOP,
        WAITING,
        MOVE_DOWN,
        MOVE_UP,
        MATCHED
    }

    public interface OnTriggerListener {
        void onTrigger();
    }

    public HeiHeiGestureView(Context context) {
        super(context);
    }

    public void setOnTriggerListener(OnTriggerListener listener) {
        this.mListener = listener;
    }

    private void trigger() {
        this.mLastTiggerTime = System.currentTimeMillis();
        if (this.mChances.length == 0) {
            if (this.mListener != null) {
                this.mListener.onTrigger();
            }
            if (1 == Settings.System.getIntForUser(this.mContext.getContentResolver(), "lockscreen_sounds_enabled", 1, KeyguardUpdateMonitor.getCurrentUser())) {
                playSound();
                return;
            }
            return;
        }
        playSound();
        this.mImageView.setVisibility(0);
        this.mImageView.setImageResource(this.mCurrentPicture);
        postDelayed(new Runnable() {
            public void run() {
                HeiHeiGestureView.this.mImageView.setVisibility(8);
            }
        }, 1500);
    }

    private void prepare() {
        String path = "/system/media/audio/ui/HeiHei.mp3";
        if (this.mChances.length > 0) {
            double rand = Math.random();
            int i = 0;
            while (true) {
                if (i < this.mChances.length) {
                    rand -= this.mChances[i];
                    if (rand <= 0.0d) {
                        path = this.mSounds[i];
                        this.mCurrentPicture = this.mPictures[i];
                        break;
                    }
                    i++;
                }
            }
        }
        try {
            if (this.mPlayer == null) {
                this.mPlayer = new MediaPlayer();
                this.mPlayer.setAudioStreamType(1);
            }
            if (!TextUtils.equals(this.mCurrentSound, path)) {
                this.mPlayer.reset();
                this.mPlayer.setDataSource(path);
            } else {
                this.mPlayer.stop();
                this.mPlayer.seekTo(0);
            }
            this.mPlayer.prepare();
        } catch (Exception e) {
            e.printStackTrace();
            this.mPlayer = null;
        }
    }

    private void playSound() {
        try {
            this.mPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
            this.mPlayer = null;
        }
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getActionMasked() == 0 && this.mLastTiggerTime + 1500 < System.currentTimeMillis()) {
            this.mStage = DetectingStage.WAITING;
        }
        if (exitWaiting(ev)) {
            return true;
        }
        return super.onInterceptTouchEvent(ev);
    }

    private boolean exitWaiting(MotionEvent ev) {
        if (DetectingStage.WAITING != this.mStage || 5 != ev.getActionMasked()) {
            return false;
        }
        if (getElapsedTime(ev) < 200) {
            this.mStage = DetectingStage.MOVE_UP;
            this.mFirstY = getTrackingY(ev);
            this.mBottomY = -1.0f;
            this.mTopY = 2.14748365E9f;
            prepare();
        } else {
            this.mStage = DetectingStage.STOP;
        }
        return true;
    }

    private boolean matchGesture(MotionEvent event) {
        if (this.mBottomY - this.mTopY < MOVE_DOWN_DISTANCE_THREDHOLD || getElapsedTime(event) > 1000) {
            this.mStage = DetectingStage.STOP;
            return false;
        }
        this.mStage = DetectingStage.MATCHED;
        this.mLastMatchTime = getElapsedTime(event);
        return true;
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (1 == event.getActionMasked()) {
            if (DetectingStage.MATCHED == this.mStage && getElapsedTime(event) - this.mLastMatchTime < 200) {
                trigger();
            }
            return true;
        } else if (exitWaiting(event)) {
            return true;
        } else {
            if (DetectingStage.MOVE_DOWN != this.mStage && DetectingStage.MOVE_UP != this.mStage) {
                return true;
            }
            if (6 == event.getActionMasked()) {
                matchGesture(event);
                return true;
            } else if (event.getPointerCount() != 2) {
                this.mStage = DetectingStage.STOP;
                return true;
            } else if (Math.abs(event.getX(0) - event.getX(1)) > TWO_POINTS_DISTANCE_X_THREDHOLD || Math.abs(event.getY(0) - event.getY(1)) > TWO_POINTS_DISTANCE_Y_THREDHOLD || Math.abs(event.getY(0) - event.getY(1)) < TWO_POINTS_DISTANCE_Y_THREDHOLD_MIN) {
                this.mStage = DetectingStage.STOP;
                return true;
            } else {
                float trackingY = getTrackingY(event);
                if (DetectingStage.MOVE_UP == this.mStage) {
                    if (this.mTopY >= trackingY) {
                        this.mTopY = trackingY;
                    } else if (this.mFirstY - this.mTopY < MOVE_UP_DISTANCE_THREDHOLD || getElapsedTime(event) > 2000) {
                        this.mStage = DetectingStage.STOP;
                        return true;
                    } else {
                        this.mStage = DetectingStage.MOVE_DOWN;
                    }
                } else if (this.mBottomY <= trackingY) {
                    this.mBottomY = trackingY;
                } else {
                    matchGesture(event);
                }
                return super.onTouchEvent(event);
            }
        }
    }

    private long getElapsedTime(MotionEvent event) {
        return event.getEventTime() - event.getDownTime();
    }

    private float getTrackingY(MotionEvent event) {
        return event.getY(0);
    }
}
