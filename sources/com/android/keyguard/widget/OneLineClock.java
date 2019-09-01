package com.android.keyguard.widget;

import android.graphics.Typeface;
import android.view.View;
import android.widget.TextView;
import com.android.systemui.R;

public class OneLineClock extends HorizontalClock {
    public int getLayoutResource() {
        return R.layout.aod_content_oneline;
    }

    public void bindView(View clock) {
        super.bindView(clock);
        this.mClockHourView = (TextView) clock.findViewById(R.id.clock_hour);
        this.mClockMinuteView = (TextView) clock.findViewById(R.id.clock_minute);
        if (this.mClockHourView != null) {
            this.mClockHourView.setTypeface(null);
        }
        if (this.mClockMinuteView != null) {
            this.mClockMinuteView.setTypeface(null);
        }
        this.mClockHorizontal = (TextView) clock.findViewById(R.id.clock_horizontal);
        Typeface typeface = Typeface.createFromAsset(clock.getContext().getAssets(), "fonts/Mitype2018-clock.ttf");
        if (this.mClockHorizontal != null) {
            this.mClockHorizontal.setTypeface(typeface);
        }
    }

    public void setPaint(int color) {
    }
}
