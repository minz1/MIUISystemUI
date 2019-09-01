package com.android.keyguard.widget;

import android.icu.text.TimeZoneNames;
import android.view.View;
import android.widget.TextView;
import com.android.keyguard.Utils;
import com.android.systemui.R;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import miui.date.Calendar;

public class DualClockTogother extends HorizontalClock {
    private boolean m24HourFormat;
    private Calendar mCal2;
    private TextView mCity;
    private TextView mCity2;
    private String mCityName;
    private String mCityName2;
    private TimeZone mTimeZone;
    private TimeZone mTimeZone2;

    public int getLayoutResource() {
        return R.layout.aod_content_horizontal_togother;
    }

    public void bindView(View clock) {
        super.bindView(clock);
        this.mCity = (TextView) clock.findViewById(R.id.city);
        this.mCity2 = (TextView) clock.findViewById(R.id.another_city);
        TimeZoneNames timeZoneNames = TimeZoneNames.getInstance(Locale.getDefault());
        this.mCityName = timeZoneNames.getExemplarLocationName(this.mTimeZone.getID());
        this.mCityName2 = timeZoneNames.getExemplarLocationName(this.mTimeZone2.getID());
    }

    public void updateTime(boolean is24HourFormat) {
        StringBuilder sb;
        this.m24HourFormat = is24HourFormat;
        TimeZone timeZone = TimeZone.getDefault();
        this.mCal = new Calendar(timeZone);
        this.mCal2 = new Calendar(this.mTimeZone2);
        String timepattern = Utils.getHourMinformat(this.mContext);
        new SimpleDateFormat(timepattern).setTimeZone(timeZone);
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
        TextView textView2 = this.mDateView;
        textView2.setText(this.mCal.format(dateFormat) + " " + this.mCityName);
        long offset = TimeUnit.HOURS.convert((long) this.mTimeZone2.getRawOffset(), TimeUnit.MILLISECONDS) - TimeUnit.HOURS.convert((long) this.mTimeZone.getRawOffset(), TimeUnit.MILLISECONDS);
        if (offset > 0) {
            sb = new StringBuilder();
            sb.append("+");
            sb.append(offset);
        } else {
            sb = new StringBuilder();
            sb.append(offset);
            sb.append("");
        }
        String off = sb.toString();
        String clockFomat = this.mContext.getResources().getString(this.m24HourFormat ? R.string.aod_dual_togother : R.string.aod_dual_togother_12);
        TextView textView3 = this.mCity2;
        StringBuilder sb2 = new StringBuilder();
        sb2.append(this.mCal2.format(clockFomat));
        TimeZone timeZone2 = timeZone;
        String str = timepattern;
        sb2.append(this.mContext.getResources().getQuantityString(R.plurals.aod_dual_togother_city, (int) offset, new Object[]{off, this.mCityName2}));
        textView3.setText(sb2.toString());
    }

    public void setTimeZone(TimeZone timeZone) {
        this.mTimeZone = timeZone;
    }

    public void setTimeZone2(TimeZone timeZone) {
        this.mTimeZone2 = timeZone;
    }
}
