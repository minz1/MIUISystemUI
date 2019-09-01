package com.android.keyguard.widget;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.TextView;

public class SunClockView extends FrameLayout {
    private TextView mClockHorizontal;
    private int mClockPaddingTop;
    private int mClockStyle;
    private int mSize;

    public SunClockView(Context context) {
        super(context);
    }

    public SunClockView(Context context, AttributeSet set) {
        super(context, set);
    }

    public void setSize(int size) {
        int paddingId;
        this.mSize = size;
        this.mClockHorizontal = (TextView) findViewById(R.id.clock_horizontal);
        if (this.mSize == 0) {
            this.mClockStyle = R.style.Aod_clock_sun_p;
            paddingId = AODSunStyle.SUN_PADDING_TOP_P;
        } else if (this.mSize == 2) {
            this.mClockStyle = R.style.Aod_clock_sun_s;
            paddingId = AODSunStyle.SUN_PADDING_TOP_S;
            this.mClockHorizontal.setLetterSpacing(1.0f);
        } else if (this.mSize == 1) {
            this.mClockStyle = R.style.Aod_clock_sun;
            paddingId = AODSunStyle.SUN_PADDING_TOP_E;
        } else {
            this.mClockStyle = R.style.Aod_clock_sun;
            paddingId = AODSunStyle.SUN_PADDING_TOP;
        }
        this.mClockPaddingTop = getResources().getDimensionPixelOffset(paddingId);
        Typeface typeface = Typeface.createFromAsset(getContext().getAssets(), "fonts/Mitype2018-60.otf");
        this.mClockHorizontal.setTextAppearance(this.mClockStyle);
        this.mClockHorizontal.setTypeface(typeface);
        this.mClockHorizontal.getPaint().setFakeBoldText(true);
        this.mClockHorizontal.setPadding(0, this.mClockPaddingTop, 0, 0);
    }
}
