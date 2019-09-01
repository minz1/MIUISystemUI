package com.android.keyguard.widget;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.widget.TextView;
import java.util.TimeZone;
import miui.date.Calendar;

public class SunClock implements IAodClock {
    private boolean m24HourFormat;
    protected Calendar mCal;
    private TextView mClockHorizontal;
    private SunClockView mClockView;
    protected Context mContext;
    private int mSize;

    public SunClock(int size) {
        this.mSize = size;
    }

    public int getLayoutResource() {
        return R.layout.aod_content_sun;
    }

    public void bindView(View clock) {
        this.mContext = clock.getContext();
        this.mClockView = (SunClockView) clock.findViewById(R.id.clock);
        this.mClockView.setSize(this.mSize);
        this.mClockHorizontal = (TextView) clock.findViewById(R.id.clock_horizontal);
        this.mClockHorizontal.setTypeface(Typeface.createFromAsset(clock.getContext().getAssets(), "fonts/Mitype2018-clock.ttf"));
    }

    public void updateTime(boolean is24HourFormat) {
        this.m24HourFormat = is24HourFormat;
        this.mCal = new Calendar(TimeZone.getDefault());
        int hour = this.mCal.get(18);
        int i = 12;
        int hour2 = (this.m24HourFormat || hour <= 12) ? hour : hour - 12;
        if (this.m24HourFormat || hour2 != 0) {
            i = hour2;
        }
        int hour3 = i;
        if (this.mClockHorizontal != null) {
            this.mClockHorizontal.setText(String.format("%02d:%02d", new Object[]{Integer.valueOf(hour3), Integer.valueOf(this.mCal.get(20))}));
        }
    }

    public void setTimeZone(TimeZone timeZone) {
    }

    public void setTimeZone2(TimeZone timeZone) {
    }

    public void setPaint(int color) {
    }
}
