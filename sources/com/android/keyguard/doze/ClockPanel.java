package com.android.keyguard.doze;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import com.android.systemui.R;

public class ClockPanel extends View {
    private int mHour;
    private final int mHourLength;
    private final int mHourLengthTail;
    private int mMinute;
    private final int mMinuteLength;
    private final int mMinuteLengthTail;
    private final int mRound;

    public ClockPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mHourLength = context.getResources().getDimensionPixelOffset(R.dimen.clock_panel_hour_size);
        this.mHourLengthTail = context.getResources().getDimensionPixelOffset(R.dimen.clock_panel_hour_tail);
        this.mMinuteLength = context.getResources().getDimensionPixelOffset(R.dimen.clock_panel_minute_size);
        this.mMinuteLengthTail = context.getResources().getDimensionPixelOffset(R.dimen.clock_panel_minute_tail);
        this.mRound = context.getResources().getDimensionPixelOffset(R.dimen.clock_panel_round);
    }

    public void setHour(int hour) {
        this.mHour = hour;
    }

    public void setMinute(int minute) {
        this.mMinute = minute;
    }

    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Paint p = new Paint();
        p.setColor(-1);
        p.setStrokeWidth(3.0f);
        p.setAntiAlias(true);
        canvas.drawLine(((float) this.mRound) + ((float) (((double) (-this.mHourLengthTail)) * Math.sin((((double) (((this.mHour * 60) + this.mMinute) * 2)) * 3.141592653589793d) / 720.0d))), ((float) this.mRound) + ((float) (((double) this.mHourLengthTail) * Math.cos((((double) (((this.mHour * 60) + this.mMinute) * 2)) * 3.141592653589793d) / 720.0d))), ((float) this.mRound) + ((float) (((double) this.mHourLength) * Math.sin((((double) (((this.mHour * 60) + this.mMinute) * 2)) * 3.141592653589793d) / 720.0d))), ((float) this.mRound) + ((float) (((double) (-this.mHourLength)) * Math.cos((((double) (((this.mHour * 60) + this.mMinute) * 2)) * 3.141592653589793d) / 720.0d))), p);
        canvas.drawLine(((float) this.mRound) + ((float) (((double) (-this.mMinuteLengthTail)) * Math.sin((((double) (this.mMinute * 2)) * 3.141592653589793d) / 60.0d))), ((float) this.mRound) + ((float) (((double) this.mMinuteLengthTail) * Math.cos((((double) (this.mMinute * 2)) * 3.141592653589793d) / 60.0d))), ((float) this.mRound) + ((float) (((double) this.mMinuteLength) * Math.sin((((double) (this.mMinute * 2)) * 3.141592653589793d) / 60.0d))), ((float) this.mRound) + ((float) (((double) (-this.mMinuteLength)) * Math.cos((((double) (this.mMinute * 2)) * 3.141592653589793d) / 60.0d))), p);
    }
}
