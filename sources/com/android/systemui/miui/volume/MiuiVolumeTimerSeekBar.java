package com.android.systemui.miui.volume;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.SeekBar;
import android.widget.TextView;
import com.android.systemui.miui.widget.TimerSeekBar;

public class MiuiVolumeTimerSeekBar extends TimerSeekBar {
    private int mBoundsStart;
    private int mCurrentSegment;
    protected BoundsSeekBarInjector mInjector;
    private TimerSeekBarMotions mMotions;
    private int mTimeRemain;

    interface TimerSeekBarMotions {
        void addCountDownStateReceiver(TextView textView);

        void addTickingTimeReceiver(TextView textView);

        void onSegmentChange(int i, int i2);

        void onTimeUpdate(int i);

        void onTouchDown();

        void onTouchRelease();
    }

    public MiuiVolumeTimerSeekBar(Context context) {
        this(context, null);
    }

    public MiuiVolumeTimerSeekBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /* JADX WARNING: type inference failed for: r5v0, types: [android.widget.SeekBar, com.android.systemui.miui.volume.MiuiVolumeTimerSeekBar] */
    public MiuiVolumeTimerSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MiuiVolumeTimerSeekBar, defStyleAttr, 0);
        this.mBoundsStart = a.getInt(R.styleable.MiuiVolumeTimerSeekBar_progressBoundsStart, 0);
        boolean drawTickingTime = a.getBoolean(R.styleable.MiuiVolumeTimerSeekBar_drawTickingTime, true);
        a.recycle();
        this.mMotions = new MiuiVolumeTimerDrawableHelper(this, drawTickingTime);
        this.mInjector = new BoundsSeekBarInjector(this, false);
        this.mInjector.setBounds((float) this.mBoundsStart, (float) getMax());
    }

    /* access modifiers changed from: protected */
    public void transformTouchEvent(MotionEvent event) {
        this.mInjector.transformTouchEvent(event);
    }

    public boolean onTouchEvent(MotionEvent event) {
        transformTouchEvent(event);
        return super.onTouchEvent(event);
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
        super.onStartTrackingTouch(seekBar);
        this.mMotions.onTouchDown();
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
        super.onStopTrackingTouch(seekBar);
        this.mMotions.onTouchRelease();
    }

    public void onTimeSet(int time) {
        super.onTimeSet(time);
        this.mTimeRemain = time;
        this.mMotions.onTimeUpdate(time);
    }

    public void onSegmentChange(int currentSegment, int determinedSegment) {
        super.onSegmentChange(currentSegment, determinedSegment);
        this.mCurrentSegment = currentSegment;
        this.mMotions.onSegmentChange(currentSegment, determinedSegment);
    }

    public void onTimeUpdate(int timeRemain) {
        super.onTimeUpdate(timeRemain);
        this.mTimeRemain = timeRemain;
        this.mMotions.onTimeUpdate(timeRemain);
    }

    private int constrainProgress(int progress) {
        return Util.constrain(progress, this.mBoundsStart, getMax());
    }

    public synchronized void setProgress(int progress) {
        super.setProgress(constrainProgress(progress));
    }

    public synchronized void setMax(int max) {
        super.setMax(max);
        if (this.mInjector != null) {
            this.mInjector.setBounds((float) this.mBoundsStart, (float) max);
        }
    }

    public int getRemainTime() {
        return this.mTimeRemain;
    }

    public void addTickingTimeReceiver(TextView view) {
        this.mMotions.addTickingTimeReceiver(view);
    }

    public void addCountDownStateReceiver(TextView view) {
        this.mMotions.addCountDownStateReceiver(view);
    }
}
