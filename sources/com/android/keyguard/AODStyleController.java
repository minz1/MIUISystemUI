package com.android.keyguard;

import android.content.Context;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.android.keyguard.widget.AODBg;
import com.android.keyguard.widget.AODSettings;
import com.android.keyguard.widget.DualClock;
import com.android.keyguard.widget.DualClockPanel;
import com.android.keyguard.widget.DualClockTogother;
import com.android.keyguard.widget.GradientLinearLayout;
import com.android.keyguard.widget.HorizontalClock;
import com.android.keyguard.widget.IAodClock;
import com.android.keyguard.widget.OneLineClock;
import com.android.keyguard.widget.SunClock;
import com.android.keyguard.widget.SunSelector;
import com.android.keyguard.widget.VerticalClock;
import com.android.systemui.R;
import java.util.TimeZone;

public class AODStyleController {
    IAodClock mAodClock = null;
    IAodClock mAodClock2 = null;

    public void inflateClockView(View aodView) {
        View view = aodView;
        Context context = aodView.getContext();
        boolean z = true;
        boolean dualClock = Settings.System.getIntForUser(context.getContentResolver(), "auto_dual_clock", 0, -2) == 1;
        String timeZoneId = Settings.System.getStringForUser(context.getContentResolver(), "resident_timezone", -2);
        ViewStub contentViewStub = (ViewStub) view.findViewById(R.id.content);
        ViewStub contentViewStub2 = (ViewStub) view.findViewById(R.id.content2);
        ViewStub iconsStub = (ViewStub) view.findViewById(R.id.aod_icons);
        iconsStub.setLayoutResource(R.layout.aod_icons);
        View icons = iconsStub.inflate();
        if (!dualClock || timeZoneId == null || TimeZone.getDefault().getID().equals(timeZoneId)) {
            z = false;
        }
        boolean dualClock2 = z;
        int aodStyleIndex = 0;
        if (AODSettings.supportColorImage()) {
            aodStyleIndex = Settings.Secure.getIntForUser(context.getContentResolver(), dualClock2 ? "aod_style_index_dual" : "aod_style_index", 0, -2);
        }
        int aodStyleIndex2 = aodStyleIndex;
        int clockStyle = AODSettings.getClockOrientation()[aodStyleIndex2];
        Context context2 = context;
        int clockStyle2 = clockStyle;
        inflateView(dualClock2, clockStyle, contentViewStub, contentViewStub2, timeZoneId, aodStyleIndex2);
        setBg(dualClock2, aodStyleIndex2, view);
        setupBatteryViews(dualClock2, clockStyle2, (ViewStub) view.findViewById(R.id.aod_battery));
        setIconMargin(icons, clockStyle2);
    }

    public void handleUpdateTime(boolean is24HourFormate) {
        if (this.mAodClock != null) {
            this.mAodClock.updateTime(is24HourFormate);
        }
        if (this.mAodClock2 != null) {
            this.mAodClock2.updateTime(is24HourFormate);
        }
    }

    private void inflateView(boolean dualClock, int clockStyle, ViewStub contentViewStub, ViewStub contentViewStub2, String timeZoneId, int aodStyleIndex) {
        View clock;
        View clock2 = null;
        if (!dualClock) {
            if (clockStyle == 0) {
                this.mAodClock = new HorizontalClock();
            } else if (clockStyle == 1) {
                this.mAodClock = new VerticalClock();
            } else if (clockStyle == 2) {
                this.mAodClock = new OneLineClock();
            } else if (clockStyle == 5) {
                this.mAodClock = new SunClock(3);
            }
            contentViewStub.setLayoutResource(this.mAodClock.getLayoutResource());
            clock = contentViewStub.inflate();
        } else {
            TimeZone timeZone = TimeZone.getDefault();
            TimeZone timeZone2 = TimeZone.getTimeZone(timeZoneId);
            if (aodStyleIndex == 0) {
                this.mAodClock = new DualClock();
                this.mAodClock2 = new DualClock();
            } else if (aodStyleIndex == 1) {
                this.mAodClock = new DualClockPanel();
                this.mAodClock2 = new DualClockPanel();
            } else {
                this.mAodClock = new DualClockTogother();
                this.mAodClock.setTimeZone2(timeZone2);
            }
            this.mAodClock.setTimeZone(timeZone);
            if (this.mAodClock2 != null) {
                this.mAodClock2.setTimeZone(timeZone2);
            }
            contentViewStub.setLayoutResource(this.mAodClock.getLayoutResource());
            clock = contentViewStub.inflate();
            if (this.mAodClock2 != null) {
                contentViewStub2.setLayoutResource(this.mAodClock2.getLayoutResource());
                clock2 = contentViewStub2.inflate();
            }
        }
        this.mAodClock.bindView(clock);
        if (this.mAodClock2 != null && clock2 != null) {
            this.mAodClock2.bindView(clock2);
        }
    }

    private void setBg(boolean dualClock, int aodStyleIndex, View aodView) {
        int[] clockColor = AODSettings.getClockColor();
        Integer[] clockBg = AODBg.getClockBg();
        if (!dualClock) {
            if (aodStyleIndex >= 0) {
                this.mAodClock.setPaint(clockColor[aodStyleIndex]);
            }
            ImageView aodBg = (ImageView) aodView.findViewById(R.id.aod_bg);
            if (aodStyleIndex >= 0 && clockBg[aodStyleIndex] != null) {
                aodBg.setImageResource(clockBg[aodStyleIndex].intValue());
            }
        }
        GradientLinearLayout gradientLinearLayout = (GradientLinearLayout) aodView.findViewById(R.id.icons);
        if (AODSettings.getIconMask(aodStyleIndex) != 0) {
            gradientLinearLayout.setGradientOverlayDrawable(gradientLinearLayout.getContext().getResources().getDrawable(AODSettings.getIconMask(aodStyleIndex)));
        }
    }

    private void setupBatteryViews(boolean dualClock, int clockStyle, ViewStub battery) {
        if ((dualClock && this.mAodClock2 != null) || clockStyle == 5) {
            battery.setLayoutResource(R.layout.aod_battery);
            AODBatteryMeterView aodBattery = (AODBatteryMeterView) battery.inflate();
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) aodBattery.getLayoutParams();
            layoutParams.topMargin = battery.getContext().getResources().getDimensionPixelOffset(R.dimen.battery_margin_top_vertical);
            layoutParams.gravity = 1;
            aodBattery.setLayoutParams(layoutParams);
        }
    }

    private void setIconMargin(View icons, int clockStyle) {
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) icons.getLayoutParams();
        if (clockStyle == 2) {
            layoutParams.topMargin = icons.getContext().getResources().getDimensionPixelOffset(R.dimen.icons_margin_top_oneline);
        } else if (clockStyle == 1) {
            layoutParams.topMargin = icons.getContext().getResources().getDimensionPixelOffset(R.dimen.icons_margin_top_vertical);
        } else if (clockStyle == 5) {
            layoutParams.topMargin = icons.getContext().getResources().getDimensionPixelOffset(R.dimen.icons_margin_top_sun);
        } else {
            layoutParams.topMargin = icons.getContext().getResources().getDimensionPixelOffset(R.dimen.icons_margin_top);
        }
        icons.setLayoutParams(layoutParams);
    }

    public void setSunImage(int index, View aodView) {
        ImageView aodBg = (ImageView) aodView.findViewById(R.id.aod_bg);
        ViewGroup.LayoutParams layoutParams = aodBg.getLayoutParams();
        layoutParams.width = aodView.getContext().getResources().getDimensionPixelOffset(R.dimen.sun_width);
        aodBg.setLayoutParams(layoutParams);
        if (index >= 0 && index < SunSelector.getDrawableLength()) {
            aodBg.setImageResource(SunSelector.getSunImage(index));
        }
    }
}
