package com.android.keyguard.widget;

import android.content.Context;
import android.icu.text.TimeZoneNames;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import com.android.keyguard.Utils;
import com.android.keyguard.doze.ClockPanel;
import com.android.systemui.R;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import miui.date.Calendar;

public class DualClockPanel implements IAodClock {
    private boolean m24HourFormat;
    private TextView mCity;
    private ClockPanel mClockPanel;
    private Context mContext;
    private TextView mDateView;
    private TimeZone mTimeZone;

    public int getLayoutResource() {
        return R.layout.aod_clock_panel_dual;
    }

    public void bindView(View clock) {
        this.mContext = clock.getContext();
        this.mClockPanel = (ClockPanel) clock.findViewById(R.id.clock_panel);
        this.mDateView = (TextView) clock.findViewById(R.id.date);
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
        if (this.mClockPanel != null) {
            this.mClockPanel.setHour(hour3);
            this.mClockPanel.setMinute(cal.get(20));
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
