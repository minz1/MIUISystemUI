package com.android.keyguard.widget;

import android.content.Context;
import android.graphics.Typeface;
import android.icu.text.TimeZoneNames;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import com.android.keyguard.Utils;
import com.android.systemui.R;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import miui.date.Calendar;

public class DualClock implements IAodClock {
    private boolean m24HourFormat;
    private TextView mCity;
    private TextView mClockHorizontal;
    private TextView mClockHourView;
    private TextView mClockMinuteView;
    private Context mContext;
    private TextView mDateView;
    private TimeZone mTimeZone;

    public int getLayoutResource() {
        return R.layout.aod_content_horizontal_dual;
    }

    public void bindView(View clock) {
        this.mContext = clock.getContext();
        Typeface typeface = Typeface.createFromAsset(this.mContext.getAssets(), "fonts/Mitype2018-clock.ttf");
        this.mClockHourView = (TextView) clock.findViewById(R.id.clock_hour);
        this.mClockMinuteView = (TextView) clock.findViewById(R.id.clock_minute);
        if (this.mClockHourView != null) {
            this.mClockHourView.setTypeface(typeface);
        }
        if (this.mClockMinuteView != null) {
            this.mClockMinuteView.setTypeface(typeface);
        }
        this.mDateView = (TextView) clock.findViewById(R.id.date);
        this.mClockHorizontal = (TextView) clock.findViewById(R.id.clock_horizontal);
        if (this.mClockHorizontal != null) {
            this.mClockHorizontal.setTypeface(typeface);
        }
        this.mCity = (TextView) clock.findViewById(R.id.city);
        if (this.mTimeZone != null) {
            String cityName = TimeZoneNames.getInstance(Locale.getDefault()).getExemplarLocationName(this.mTimeZone.getID());
            if (!TextUtils.isEmpty(cityName)) {
                this.mCity.setText(cityName);
            }
        }
    }

    public void updateTime(boolean is24HourFormat) {
        this.m24HourFormat = is24HourFormat;
        TimeZone timeZone = this.mTimeZone == null ? TimeZone.getDefault() : this.mTimeZone;
        Calendar cal = new Calendar(timeZone);
        new SimpleDateFormat(Utils.getHourMinformat(this.mContext)).setTimeZone(timeZone);
        int hour = cal.get(18);
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
            this.mClockMinuteView.setText(String.format("%02d", new Object[]{Integer.valueOf(cal.get(20))}));
        }
        if (this.mClockHorizontal != null) {
            this.mClockHorizontal.setText(String.format("%d:%02d", new Object[]{Integer.valueOf(hour3), Integer.valueOf(cal.get(20))}));
        }
        this.mDateView.setText(cal.format(this.mContext.getResources().getString(this.m24HourFormat ? R.string.lock_screen_date : R.string.lock_screen_date_12)));
    }

    public void setTimeZone(TimeZone timeZone) {
        this.mTimeZone = timeZone;
    }

    public void setTimeZone2(TimeZone timeZone) {
    }

    public void setPaint(int color) {
    }
}
