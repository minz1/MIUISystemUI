package com.android.systemui.miui.widget;

import android.view.MotionEvent;
import android.widget.SeekBar;

public class RelativeSeekBarInjector {
    private float mOffset;
    private SeekBar mSeekBar;
    private boolean mVertical;

    public RelativeSeekBarInjector(SeekBar seekBar, boolean vertical) {
        this.mSeekBar = seekBar;
        this.mVertical = vertical;
    }

    public void setVertical(boolean vertical) {
        this.mVertical = vertical;
    }

    public void transformTouchEvent(MotionEvent event) {
        if (event.getAction() == 0) {
            computeTouchOffset(event);
        }
        float f = 0.0f;
        float f2 = this.mVertical ? 0.0f : this.mOffset;
        if (this.mVertical) {
            f = this.mOffset;
        }
        event.offsetLocation(f2, f);
    }

    private void computeTouchOffset(MotionEvent down) {
        float currentX;
        int[] location = new int[2];
        this.mSeekBar.getLocationOnScreen(location);
        float progress = ((float) this.mSeekBar.getProgress()) / ((float) this.mSeekBar.getMax());
        if (this.mVertical) {
            this.mOffset = (((float) (location[1] + this.mSeekBar.getPaddingTop())) + ((1.0f - progress) * ((float) ((this.mSeekBar.getHeight() - this.mSeekBar.getPaddingTop()) - this.mSeekBar.getPaddingBottom())))) - down.getRawY();
            return;
        }
        int visualWidth = (this.mSeekBar.getWidth() - this.mSeekBar.getPaddingStart()) - this.mSeekBar.getPaddingEnd();
        if (this.mSeekBar.isLayoutRtl()) {
            currentX = ((float) ((location[0] + this.mSeekBar.getWidth()) - this.mSeekBar.getPaddingEnd())) - (((float) visualWidth) * progress);
        } else {
            currentX = ((float) (location[0] + this.mSeekBar.getPaddingStart())) + (((float) visualWidth) * progress);
        }
        this.mOffset = currentX - down.getRawX();
    }
}
