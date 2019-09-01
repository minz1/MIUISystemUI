package com.android.keyguard.widget;

import android.graphics.Typeface;
import android.view.View;
import android.widget.TextView;
import com.android.systemui.R;

public class VerticalClock extends HorizontalClock {
    public int getLayoutResource() {
        return R.layout.aod_content_vertical;
    }

    public void bindView(View clock) {
        this.mContext = clock.getContext();
        Typeface typeface = Typeface.createFromAsset(this.mContext.getAssets(), "fonts/Mitype2018-clock2.ttf");
        this.mClockHourView = (TextView) clock.findViewById(R.id.clock_hour);
        this.mClockMinuteView = (TextView) clock.findViewById(R.id.clock_minute);
        if (this.mClockHourView != null) {
            this.mClockHourView.setTypeface(typeface);
        }
        if (this.mClockMinuteView != null) {
            this.mClockMinuteView.setTypeface(typeface);
        }
        this.mDateView = (TextView) clock.findViewById(R.id.date);
        this.mCity = (TextView) clock.findViewById(R.id.city);
        this.mGradientLayout = (GradientLinearLayout) clock.findViewById(R.id.gradient_layout);
    }

    public void updateTime(boolean is24HourFormat) {
        super.updateTime(is24HourFormat);
        if (this.mClockHourView != null) {
            int hour = this.mCal.get(18);
            int hour2 = 12;
            int hour3 = (is24HourFormat || hour <= 12) ? hour : hour - 12;
            if (is24HourFormat || hour3 != 0) {
                hour2 = hour3;
            }
            this.mClockHourView.setText(String.format("%02d", new Object[]{Integer.valueOf(hour2)}));
        }
    }

    public void setPaint(int color) {
        if (color == 1) {
            this.mGradientLayout.setGradientOverlayDrawable(this.mContext.getResources().getDrawable(R.drawable.aod_num_big_blue_y));
        } else if (color == 2) {
            this.mGradientLayout.setGradientOverlayDrawable(this.mContext.getResources().getDrawable(R.drawable.aod_num_big_pink_y));
        }
    }
}
