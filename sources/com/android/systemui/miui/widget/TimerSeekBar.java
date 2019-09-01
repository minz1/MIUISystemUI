package com.android.systemui.miui.widget;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.SeekBar;
import com.android.systemui.Interpolators;
import miui.widget.SeekBar;

public class TimerSeekBar extends SeekBar implements SeekBar.OnSeekBarChangeListener {
    private int mCurrentSegmentPoint;
    private int mDeterminedSegmentPoint;
    private boolean mDragging;
    private SeekBar.OnSeekBarChangeListener mOnSeekBarChangeListener;
    private OnTimeUpdateListener mOnTimeUpdateListener;
    private int[] mTimeSegments;

    public interface OnTimeUpdateListener {
        void onSegmentChange(int i, int i2);

        void onTimeSet(int i);

        void onTimeUpdate(int i);
    }

    public TimerSeekBar(Context context) {
        this(context, null);
    }

    public TimerSeekBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TimerSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TimerSeekBar, defStyleAttr, 0);
        int arrayId = a.getResourceId(R.styleable.TimerSeekBar_timeSegments, 0);
        if (arrayId > 0) {
            this.mTimeSegments = getResources().getIntArray(arrayId);
        }
        a.recycle();
        superSetOnSeekBarChangeListener(this);
    }

    public void setOnTimeUpdateListener(OnTimeUpdateListener onTimeUpdateListener) {
        this.mOnTimeUpdateListener = onTimeUpdateListener;
    }

    public void updateRemainTime(int remain) {
        if (!this.mDragging) {
            setProgress(timeToProgress(remain));
            onTimeUpdate(remain);
        }
    }

    private int timeToProgress(int time) {
        int length;
        int maxTime = this.mTimeSegments[this.mTimeSegments.length - 1];
        int time2 = time < 0 ? 0 : time > maxTime ? maxTime : time;
        if (time2 == maxTime) {
            return getMax();
        }
        for (int i = this.mTimeSegments.length - 1; i >= 0; i--) {
            if (this.mTimeSegments[i] < time2) {
                int timeLeft = this.mTimeSegments[i];
                return (int) (((float) (getMax() / length)) * (((float) (i + 1)) + (((float) (time2 - timeLeft)) / ((float) (this.mTimeSegments[i + 1] - timeLeft)))));
            }
        }
        return 0;
    }

    private int determineProgressToSegment(int progress) {
        int progressPerSeg = getMax() / this.mTimeSegments.length;
        int left = progress / progressPerSeg;
        return progress < ((int) ((((double) left) + 0.5d) * ((double) progressPerSeg))) ? left : left + 1;
    }

    private void setCurrentSegment(int segment, int determinedSegmentPoint) {
        if (segment != this.mCurrentSegmentPoint || determinedSegmentPoint != this.mDeterminedSegmentPoint) {
            this.mCurrentSegmentPoint = segment;
            this.mDeterminedSegmentPoint = determinedSegmentPoint;
            onSegmentChange(this.mCurrentSegmentPoint, this.mDeterminedSegmentPoint);
        }
    }

    public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
        setCurrentSegment(progress / (getMax() / this.mTimeSegments.length), determineProgressToSegment(progress));
        if (this.mOnSeekBarChangeListener != null) {
            this.mOnSeekBarChangeListener.onProgressChanged(seekBar, progress, fromUser);
        }
    }

    /* JADX WARNING: type inference failed for: r1v0, types: [com.android.systemui.miui.widget.TimerSeekBar, android.widget.SeekBar] */
    public void onStartTrackingTouch(android.widget.SeekBar seekBar) {
        this.mDragging = true;
        if (this.mOnSeekBarChangeListener != null) {
            this.mOnSeekBarChangeListener.onStartTrackingTouch(this);
        }
    }

    /* JADX WARNING: type inference failed for: r4v0, types: [com.android.systemui.miui.widget.TimerSeekBar, android.widget.SeekBar] */
    public void onStopTrackingTouch(android.widget.SeekBar seekBar) {
        int time = 0;
        this.mDragging = false;
        setCurrentSegment(this.mDeterminedSegmentPoint, this.mDeterminedSegmentPoint);
        animateToProgress((this.mCurrentSegmentPoint * (getMax() / this.mTimeSegments.length)) - 1);
        if (this.mCurrentSegmentPoint != 0) {
            time = this.mTimeSegments[this.mCurrentSegmentPoint - 1];
        }
        onTimeSet(time);
        if (this.mOnSeekBarChangeListener != null) {
            this.mOnSeekBarChangeListener.onStopTrackingTouch(this);
        }
    }

    private void animateToProgress(int progress) {
        ObjectAnimator animator = ObjectAnimator.ofInt(this, "progress", new int[]{progress});
        animator.setDuration(300);
        animator.setAutoCancel(true);
        animator.setInterpolator(Interpolators.DECELERATE_QUART);
        animator.start();
    }

    /* access modifiers changed from: protected */
    public void onTimeSet(int time) {
        if (this.mOnTimeUpdateListener != null) {
            this.mOnTimeUpdateListener.onTimeSet(time);
        }
    }

    /* access modifiers changed from: protected */
    public void onSegmentChange(int currentSegment, int determinedSegment) {
        if (this.mOnTimeUpdateListener != null) {
            this.mOnTimeUpdateListener.onSegmentChange(currentSegment, determinedSegment);
        }
    }

    /* access modifiers changed from: protected */
    public void onTimeUpdate(int timeRemain) {
        if (this.mOnTimeUpdateListener != null) {
            this.mOnTimeUpdateListener.onTimeUpdate(timeRemain);
        }
    }

    private void superSetOnSeekBarChangeListener(SeekBar.OnSeekBarChangeListener l) {
        TimerSeekBar.super.setOnSeekBarChangeListener(l);
    }

    public void setOnSeekBarChangeListener(SeekBar.OnSeekBarChangeListener l) {
        this.mOnSeekBarChangeListener = l;
    }
}
