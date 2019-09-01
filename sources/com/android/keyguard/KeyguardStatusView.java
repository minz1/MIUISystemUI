package com.android.keyguard;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Slog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.TextClock;
import android.widget.TextView;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternUtilsCompat;
import com.android.systemui.ChargingView;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.DateView;
import java.util.Locale;

public class KeyguardStatusView extends GridLayout {
    private final AlarmManager mAlarmManager;
    private TextView mAlarmStatusView;
    private ChargingView mBatteryDoze;
    private ViewGroup mClockContainer;
    private TextClock mClockView;
    private DateView mDateView;
    private KeyguardUpdateMonitorCallback mInfoCallback;
    private final LockPatternUtils mLockPatternUtils;
    private TextView mOwnerInfo;
    private View[] mVisibleInDoze;

    private static final class Patterns {
        static String cacheKey;
        static String clockView12;
        static String clockView24;
        static String dateViewSkel;

        static void update(Context context, boolean hasAlarm) {
            int i;
            String clockView24Skel;
            Locale locale = Locale.getDefault();
            Resources res = context.getResources();
            if (hasAlarm) {
                i = R.string.abbrev_wday_month_day_no_year_alarm;
            } else {
                i = R.string.abbrev_wday_month_day_no_year;
            }
            dateViewSkel = res.getString(i);
            String clockView12Skel = res.getString(R.string.clock_12hr_format);
            String key = locale.toString() + dateViewSkel + clockView12Skel + clockView24Skel;
            if (!key.equals(cacheKey)) {
                clockView12 = DateFormat.getBestDateTimePattern(locale, clockView12Skel);
                if (!context.getResources().getBoolean(R.bool.config_showAmpm) && !clockView12Skel.contains("a")) {
                    clockView12 = clockView12.replaceAll("a", "").trim();
                }
                clockView24 = DateFormat.getBestDateTimePattern(locale, clockView24Skel);
                clockView24 = clockView24.replace(':', 60929);
                clockView12 = clockView12.replace(':', 60929);
                cacheKey = key;
            }
        }
    }

    public KeyguardStatusView(Context context) {
        this(context, null, 0);
    }

    public KeyguardStatusView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KeyguardStatusView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mInfoCallback = new KeyguardUpdateMonitorCallback() {
            public void onTimeChanged() {
                KeyguardStatusView.this.refresh();
            }

            public void onKeyguardVisibilityChanged(boolean showing) {
                if (showing) {
                    Slog.v("KeyguardStatusView", "refresh statusview showing:" + showing);
                    KeyguardStatusView.this.refresh();
                    KeyguardStatusView.this.updateOwnerInfo();
                }
            }

            public void onStartedWakingUp() {
                KeyguardStatusView.this.setEnableMarquee(true);
            }

            public void onFinishedGoingToSleep(int why) {
                KeyguardStatusView.this.setEnableMarquee(false);
            }

            public void onUserSwitchComplete(int userId) {
                KeyguardStatusView.this.refresh();
                KeyguardStatusView.this.updateOwnerInfo();
            }
        };
        this.mAlarmManager = (AlarmManager) context.getSystemService("alarm");
        this.mLockPatternUtils = new LockPatternUtils(getContext());
    }

    /* access modifiers changed from: private */
    public void setEnableMarquee(boolean enabled) {
        StringBuilder sb = new StringBuilder();
        sb.append(enabled ? "Enable" : "Disable");
        sb.append(" transport text marquee");
        Log.v("KeyguardStatusView", sb.toString());
        if (this.mAlarmStatusView != null) {
            this.mAlarmStatusView.setSelected(enabled);
        }
        if (this.mOwnerInfo != null) {
            this.mOwnerInfo.setSelected(enabled);
        }
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mClockContainer = (ViewGroup) findViewById(R.id.keyguard_clock_container);
        this.mAlarmStatusView = (TextView) findViewById(R.id.alarm_status);
        this.mDateView = (DateView) findViewById(R.id.date_view);
        this.mClockView = (TextClock) findViewById(R.id.clock_view);
        this.mClockView.setShowCurrentUserTime(true);
        this.mClockView.setAccessibilityDelegate(new KeyguardClockAccessibilityDelegate(this.mContext));
        this.mOwnerInfo = (TextView) findViewById(R.id.owner_info);
        this.mBatteryDoze = (ChargingView) findViewById(R.id.battery_doze);
        this.mVisibleInDoze = new View[]{this.mBatteryDoze, this.mClockView};
        setEnableMarquee(KeyguardUpdateMonitor.getInstance(this.mContext).isDeviceInteractive());
        refresh();
        updateOwnerInfo();
        this.mClockView.setElegantTextHeight(false);
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.mClockView.setTextSize(0, (float) getResources().getDimensionPixelSize(R.dimen.widget_big_font_size));
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) this.mClockView.getLayoutParams();
        layoutParams.bottomMargin = getResources().getDimensionPixelSize(R.dimen.bottom_text_spacing_digital);
        this.mClockView.setLayoutParams(layoutParams);
        this.mDateView.setTextSize(0, (float) getResources().getDimensionPixelSize(R.dimen.widget_label_font_size));
        if (this.mOwnerInfo != null) {
            this.mOwnerInfo.setTextSize(0, (float) getResources().getDimensionPixelSize(R.dimen.widget_label_font_size));
        }
    }

    public void refreshTime() {
        this.mDateView.setDatePattern(Patterns.dateViewSkel);
        this.mClockView.setFormat12Hour(Patterns.clockView12);
        this.mClockView.setFormat24Hour(Patterns.clockView24);
    }

    /* access modifiers changed from: private */
    public void refresh() {
        AlarmManager.AlarmClockInfo nextAlarm = this.mAlarmManager.getNextAlarmClock(-2);
        Patterns.update(this.mContext, nextAlarm != null);
        refreshTime();
        refreshAlarmStatus(nextAlarm);
    }

    /* access modifiers changed from: package-private */
    public void refreshAlarmStatus(AlarmManager.AlarmClockInfo nextAlarm) {
        if (nextAlarm != null) {
            String alarm = formatNextAlarm(this.mContext, nextAlarm);
            this.mAlarmStatusView.setText(alarm);
            this.mAlarmStatusView.setContentDescription(getResources().getString(R.string.keyguard_accessibility_next_alarm, new Object[]{alarm}));
            this.mAlarmStatusView.setVisibility(0);
            return;
        }
        this.mAlarmStatusView.setVisibility(8);
    }

    public static String formatNextAlarm(Context context, AlarmManager.AlarmClockInfo info) {
        String skeleton;
        if (info == null) {
            return "";
        }
        if (DateFormat.is24HourFormat(context, ActivityManager.getCurrentUser())) {
            skeleton = "EHm";
        } else {
            skeleton = "Ehma";
        }
        return DateFormat.format(DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton), info.getTriggerTime()).toString();
    }

    /* access modifiers changed from: private */
    public void updateOwnerInfo() {
        if (this.mOwnerInfo != null) {
            String ownerInfo = getOwnerInfo();
            if (!TextUtils.isEmpty(ownerInfo)) {
                this.mOwnerInfo.setVisibility(0);
                this.mOwnerInfo.setText(ownerInfo);
            } else {
                this.mOwnerInfo.setVisibility(8);
            }
        }
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        KeyguardUpdateMonitor.getInstance(this.mContext).registerCallback(this.mInfoCallback);
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        KeyguardUpdateMonitor.getInstance(this.mContext).removeCallback(this.mInfoCallback);
    }

    private String getOwnerInfo() {
        if (LockPatternUtilsCompat.isDeviceOwnerInfoEnabled(this.mLockPatternUtils)) {
            return LockPatternUtilsCompat.getDeviceOwnerInfo(this.mLockPatternUtils);
        }
        return null;
    }

    public boolean hasOverlappingRendering() {
        return false;
    }
}
