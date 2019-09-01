package com.android.keyguard.widget;

import android.content.Context;
import android.view.View;
import android.widget.TextView;
import com.android.keyguard.Utils;
import com.android.systemui.R;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import miui.date.Calendar;

public class HorizontalClock implements IAodClock {
    protected boolean m24HourFormat;
    protected Calendar mCal;
    protected TextView mCity;
    protected TextView mClockHorizontal;
    protected TextView mClockHourView;
    protected TextView mClockMinuteView;
    protected Context mContext;
    protected TextView mDateView;
    protected GradientLinearLayout mGradientLayout;

    public int getLayoutResource() {
        return R.layout.aod_content_horizontal;
    }

    public void bindView(View clock) {
        this.mContext = clock.getContext();
        this.mClockHourView = (TextView) clock.findViewById(R.id.clock_hour);
        this.mClockMinuteView = (TextView) clock.findViewById(R.id.clock_minute);
        this.mDateView = (TextView) clock.findViewById(R.id.date);
        this.mClockHorizontal = (TextView) clock.findViewById(R.id.clock_horizontal);
        this.mCity = (TextView) clock.findViewById(R.id.city);
        this.mGradientLayout = (GradientLinearLayout) clock.findViewById(R.id.gradient_layout);
    }

    public void updateTime(boolean is24HourFormat) {
        this.m24HourFormat = is24HourFormat;
        TimeZone timeZone = TimeZone.getDefault();
        this.mCal = new Calendar(timeZone);
        new SimpleDateFormat(Utils.getHourMinformat(this.mContext)).setTimeZone(timeZone);
        int hour = this.mCal.get(18);
        int i = 12;
        int hour2 = (this.m24HourFormat || hour <= 12) ? hour : hour - 12;
        if (this.m24HourFormat || hour2 != 0) {
            i = hour2;
        }
        int hour3 = i;
        if (this.mClockHourView != null) {
            TextView textView = this.mClockHourView;
            textView.setText(hour3 + "");
        }
        if (this.mClockMinuteView != null) {
            this.mClockMinuteView.setText(String.format("%02d", new Object[]{Integer.valueOf(this.mCal.get(20))}));
        }
        if (this.mClockHorizontal != null) {
            this.mClockHorizontal.setText(String.format("%d:%02d", new Object[]{Integer.valueOf(hour3), Integer.valueOf(this.mCal.get(20))}));
        }
        String dateFormat = this.mContext.getResources().getString(this.m24HourFormat ? R.string.lock_screen_date : R.string.lock_screen_date_12);
        if (this.mDateView != null) {
            this.mDateView.setText(this.mCal.format(dateFormat));
        }
    }

    public void setTimeZone(TimeZone timeZone) {
    }

    public void setTimeZone2(TimeZone timeZone) {
    }

    public void setPaint(int color) {
        if (color == 1) {
            this.mGradientLayout.setGradientOverlayDrawable(this.mContext.getResources().getDrawable(R.drawable.aod_num_big_blue_x));
        } else if (color == 2) {
            this.mGradientLayout.setGradientOverlayDrawable(this.mContext.getResources().getDrawable(R.drawable.aod_num_big_pink_x));
        }
    }
}
