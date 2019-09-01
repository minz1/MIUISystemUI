package com.android.systemui.miui.volume;

import android.view.MotionEvent;
import android.widget.SeekBar;
import com.android.systemui.miui.widget.RelativeSeekBarInjector;

class BoundsSeekBarInjector extends RelativeSeekBarInjector {
    private float mBoundsEnd;
    private float mBoundsStart;
    private SeekBar mSeekBar;
    private float mTouchBoundsEnd;
    private float mTouchBoundsStart;
    private boolean mVertical;

    public BoundsSeekBarInjector(SeekBar seekBar, boolean vertical) {
        super(seekBar, vertical);
        this.mSeekBar = seekBar;
        this.mVertical = vertical;
    }

    public void setBounds(float boundsStart, float boundsEnd) {
        this.mBoundsStart = boundsStart;
        this.mBoundsEnd = boundsEnd;
    }

    public void setVertical(boolean vertical) {
        super.setVertical(vertical);
        this.mVertical = vertical;
    }

    public void transformTouchEvent(MotionEvent event) {
        super.transformTouchEvent(event);
        if (event.getAction() == 0) {
            computeTouchOffset();
        }
        if (this.mVertical) {
            event.offsetLocation(0.0f, Util.constrain(event.getY(), Math.min(this.mTouchBoundsStart, this.mTouchBoundsEnd), Math.max(this.mTouchBoundsStart, this.mTouchBoundsEnd)) - event.getY());
        } else {
            event.offsetLocation(Util.constrain(event.getX(), Math.min(this.mTouchBoundsStart, this.mTouchBoundsEnd), Math.max(this.mTouchBoundsStart, this.mTouchBoundsEnd)) - event.getX(), 0.0f);
        }
    }

    private void computeTouchOffset() {
        if (this.mVertical) {
            float progressHeight = (float) ((this.mSeekBar.getHeight() - this.mSeekBar.getPaddingTop()) - this.mSeekBar.getPaddingBottom());
            this.mTouchBoundsStart = ((float) this.mSeekBar.getPaddingTop()) + ((1.0f - (this.mBoundsStart / ((float) this.mSeekBar.getMax()))) * progressHeight);
            this.mTouchBoundsEnd = ((float) this.mSeekBar.getPaddingTop()) + ((1.0f - (this.mBoundsEnd / ((float) this.mSeekBar.getMax()))) * progressHeight);
            return;
        }
        float progressWidth = (float) ((this.mSeekBar.getWidth() - this.mSeekBar.getPaddingLeft()) - this.mSeekBar.getPaddingRight());
        this.mTouchBoundsStart = ((float) this.mSeekBar.getPaddingLeft()) + ((this.mBoundsStart / ((float) this.mSeekBar.getMax())) * progressWidth);
        this.mTouchBoundsEnd = ((float) this.mSeekBar.getPaddingLeft()) + ((this.mBoundsEnd / ((float) this.mSeekBar.getMax())) * progressWidth);
    }
}
