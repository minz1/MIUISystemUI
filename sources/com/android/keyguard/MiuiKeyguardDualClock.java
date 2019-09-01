package com.android.keyguard;

import android.content.Context;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.icu.text.TimeZoneNames;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.android.keyguard.KeyguardClockContainer;
import com.android.systemui.R;
import java.util.Locale;
import java.util.TimeZone;
import miui.date.Calendar;
import miui.date.DateUtils;
import miui.util.Log;

public class MiuiKeyguardDualClock extends RelativeLayout implements KeyguardClockContainer.KeyguardClockView {
    private boolean m24HourFormat;
    private boolean mAttached;
    /* access modifiers changed from: private */
    public boolean mAutoTimeZone;
    ContentObserver mAutoTimeZoneObserver;
    private Calendar mCalendar;
    private boolean mDarkMode;
    private KeyguardUpdateMonitorCallback mInfoCallback;
    private String mLanguage;
    /* access modifiers changed from: private */
    public TextView mLocalCity;
    ContentObserver mLocalCityObserver;
    private TextView mLocalDate;
    private TextView mLocalTime;
    private String mLocalTimeZone;
    private Calendar mResidentCalendar;
    private TextView mResidentCity;
    private TextView mResidentDate;
    private TextView mResidentTime;
    private String mResidentTimeZone;

    public MiuiKeyguardDualClock(Context context) {
        this(context, null);
    }

    public MiuiKeyguardDualClock(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mLanguage = "";
        this.mDarkMode = false;
        this.mAutoTimeZone = true;
        this.mInfoCallback = new KeyguardUpdateMonitorCallback() {
            public void onKeyguardVisibilityChanged(boolean showing) {
                if (showing) {
                    MiuiKeyguardDualClock.this.updateHourFormat();
                }
            }
        };
        this.mAutoTimeZoneObserver = new ContentObserver(new Handler()) {
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                MiuiKeyguardDualClock miuiKeyguardDualClock = MiuiKeyguardDualClock.this;
                boolean z = false;
                if (Settings.Global.getInt(MiuiKeyguardDualClock.this.mContext.getContentResolver(), "auto_time_zone", 0) > 0) {
                    z = true;
                }
                boolean unused = miuiKeyguardDualClock.mAutoTimeZone = z;
                MiuiKeyguardDualClock.this.updateLocalCity();
            }
        };
        this.mLocalCityObserver = new ContentObserver(new Handler()) {
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                MiuiKeyguardDualClock.this.updateLocalCity();
            }
        };
    }

    public boolean hasOverlappingRendering() {
        return false;
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mLocalCity = (TextView) findViewById(R.id.local_city_name);
        this.mLocalTime = (TextView) findViewById(R.id.local_time);
        this.mLocalTime.setAccessibilityDelegate(new KeyguardClockAccessibilityDelegate(this.mContext));
        this.mLocalDate = (TextView) findViewById(R.id.local_date);
        this.mResidentCity = (TextView) findViewById(R.id.resident_city_name);
        this.mResidentTime = (TextView) findViewById(R.id.resident_time);
        this.mResidentTime.setAccessibilityDelegate(new KeyguardClockAccessibilityDelegate(this.mContext));
        this.mResidentDate = (TextView) findViewById(R.id.resident_date);
        this.mLanguage = this.mContext.getResources().getConfiguration().locale.getLanguage();
        TimeZone tz = TimeZone.getDefault();
        this.mLocalTimeZone = tz.getID();
        updateLocalCity();
        this.mCalendar = new Calendar();
        if (TextUtils.isEmpty(this.mResidentTimeZone)) {
            this.mResidentTimeZone = tz.getID();
        }
        updateResidentCityName();
        this.mResidentCalendar = new Calendar(TimeZone.getTimeZone(this.mResidentTimeZone));
        updateHourFormat();
        updateTime();
    }

    public void updateHourFormat() {
        this.m24HourFormat = DateFormat.is24HourFormat(this.mContext, KeyguardUpdateMonitor.getCurrentUser());
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!this.mAttached) {
            this.mAttached = true;
            if (MiuiKeyguardUtils.supportDualClock()) {
                try {
                    this.mContext.getContentResolver().registerContentObserver(Uri.parse("content://weather/actualWeatherData/1/1"), false, this.mLocalCityObserver, -1);
                    this.mLocalCityObserver.onChange(false);
                } catch (Exception e) {
                    Log.e("MiuiKeyguardUtils", "register weather observer:", e);
                }
            }
            this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("auto_time_zone"), false, this.mAutoTimeZoneObserver, -1);
            this.mAutoTimeZoneObserver.onChange(false);
            setDarkMode(this.mDarkMode);
            KeyguardUpdateMonitor.getInstance(this.mContext).registerCallback(this.mInfoCallback);
        }
    }

    public void setDarkMode(boolean darkMode) {
        this.mDarkMode = darkMode;
        int color = darkMode ? getContext().getResources().getColor(R.color.miui_common_unlock_screen_common_time_dark_text_color) : -1;
        this.mLocalCity.setTextColor(color);
        this.mLocalTime.setTextColor(color);
        this.mLocalDate.setTextColor(color);
        this.mResidentCity.setTextColor(color);
        this.mResidentTime.setTextColor(color);
        this.mResidentDate.setTextColor(color);
        updateTime();
    }

    public int getClockHeight() {
        return getHeight();
    }

    public float getClockVisibleHeight() {
        return (float) getHeight();
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (this.mAttached) {
            this.mAttached = false;
            this.mContext.getContentResolver().unregisterContentObserver(this.mLocalCityObserver);
            this.mContext.getContentResolver().unregisterContentObserver(this.mAutoTimeZoneObserver);
            KeyguardUpdateMonitor.getInstance(this.mContext).removeCallback(this.mInfoCallback);
        }
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        String language = newConfig.locale.getLanguage();
        if (language != null && !language.equals(this.mLanguage)) {
            updateResidentCityName();
            updateLocalCity();
            this.mLanguage = language;
        }
    }

    public void updateTime() {
        updateTime(this.mCalendar, this.mLocalTime, this.mLocalDate);
        updateTime(this.mResidentCalendar, this.mResidentTime, this.mResidentDate);
    }

    public void updateTimeAndBatteryInfo() {
        updateTime();
    }

    public void updateTime(Calendar calendar, TextView timeView, TextView dateView) {
        calendar.setTimeInMillis(System.currentTimeMillis());
        timeView.setText(DateUtils.formatDateTime(System.currentTimeMillis(), (this.m24HourFormat ? 32 : 16) | 12 | 64, calendar.getTimeZone()));
        dateView.setText(calendar.format(this.mContext.getString(this.m24HourFormat ? R.string.lock_screen_date : R.string.lock_screen_date_12)));
    }

    public void updateTimeZone(String timeZone) {
        if (!TextUtils.isEmpty(timeZone)) {
            this.mLocalTimeZone = timeZone;
            this.mCalendar = new Calendar(TimeZone.getTimeZone(this.mLocalTimeZone));
            updateTime();
            updateLocalCity();
        }
    }

    public void updateResidentTimeZone(String residentTimeZone) {
        if (!TextUtils.isEmpty(residentTimeZone)) {
            this.mResidentTimeZone = residentTimeZone;
            this.mResidentCalendar = new Calendar(TimeZone.getTimeZone(this.mResidentTimeZone));
            updateTime();
            updateResidentCityName();
        }
    }

    public void updateClockView(boolean hasNotifiction, boolean isUnderKeyguard) {
    }

    public void setClockAlpha(float alpha) {
        setAlpha(alpha);
    }

    private void updateResidentCityName() {
        this.mResidentCity.setText(TimeZoneNames.getInstance(Locale.getDefault()).getExemplarLocationName(this.mResidentTimeZone));
    }

    /* access modifiers changed from: private */
    public void updateLocalCity() {
        if (this.mAutoTimeZone) {
            new AsyncTask<Void, Void, String>() {
                /* access modifiers changed from: protected */
                /* JADX WARNING: Code restructure failed: missing block: B:15:0x0040, code lost:
                    if (r1 == null) goto L_0x0043;
                 */
                /* JADX WARNING: Code restructure failed: missing block: B:16:0x0043, code lost:
                    return r0;
                 */
                /* JADX WARNING: Code restructure failed: missing block: B:8:0x0030, code lost:
                    if (r1 != null) goto L_0x0032;
                 */
                /* JADX WARNING: Code restructure failed: missing block: B:9:0x0032, code lost:
                    r1.close();
                 */
                /* Code decompiled incorrectly, please refer to instructions dump. */
                public java.lang.String doInBackground(java.lang.Void... r10) {
                    /*
                        r9 = this;
                        java.lang.String r0 = ""
                        r1 = 0
                        com.android.keyguard.MiuiKeyguardDualClock r2 = com.android.keyguard.MiuiKeyguardDualClock.this     // Catch:{ Exception -> 0x0038 }
                        android.content.Context r2 = r2.mContext     // Catch:{ Exception -> 0x0038 }
                        android.content.ContentResolver r3 = r2.getContentResolver()     // Catch:{ Exception -> 0x0038 }
                        java.lang.String r2 = "content://weather/actualWeatherData/1/1"
                        android.net.Uri r4 = android.net.Uri.parse(r2)     // Catch:{ Exception -> 0x0038 }
                        r5 = 0
                        r6 = 0
                        r7 = 0
                        r8 = 0
                        android.database.Cursor r2 = r3.query(r4, r5, r6, r7, r8)     // Catch:{ Exception -> 0x0038 }
                        r1 = r2
                        if (r1 == 0) goto L_0x0030
                    L_0x001e:
                        boolean r2 = r1.moveToNext()     // Catch:{ Exception -> 0x0038 }
                        if (r2 == 0) goto L_0x0030
                        java.lang.String r2 = "city_name"
                        int r2 = r1.getColumnIndex(r2)     // Catch:{ Exception -> 0x0038 }
                        java.lang.String r2 = r1.getString(r2)     // Catch:{ Exception -> 0x0038 }
                        r0 = r2
                        goto L_0x001e
                    L_0x0030:
                        if (r1 == 0) goto L_0x0043
                    L_0x0032:
                        r1.close()
                        goto L_0x0043
                    L_0x0036:
                        r2 = move-exception
                        goto L_0x0044
                    L_0x0038:
                        r2 = move-exception
                        java.lang.String r3 = "miui_keyguard"
                        java.lang.String r4 = "get city exception"
                        miui.util.Log.e(r3, r4, r2)     // Catch:{ all -> 0x0036 }
                        if (r1 == 0) goto L_0x0043
                        goto L_0x0032
                    L_0x0043:
                        return r0
                    L_0x0044:
                        if (r1 == 0) goto L_0x0049
                        r1.close()
                    L_0x0049:
                        throw r2
                    */
                    throw new UnsupportedOperationException("Method not decompiled: com.android.keyguard.MiuiKeyguardDualClock.AnonymousClass4.doInBackground(java.lang.Void[]):java.lang.String");
                }

                /* access modifiers changed from: protected */
                public void onPostExecute(String city) {
                    if (!TextUtils.isEmpty(city)) {
                        MiuiKeyguardDualClock.this.mLocalCity.setText(city);
                    } else {
                        MiuiKeyguardDualClock.this.mLocalCity.setText(MiuiKeyguardDualClock.this.mContext.getString(R.string.clock_city_name_local));
                    }
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
        } else {
            this.mLocalCity.setText(this.mContext.getString(R.string.clock_city_name_local));
        }
    }

    public void updateLockScreenMagazineInfo() {
    }

    public void setSelectedClockPosition(int clockPosition) {
    }

    public float getTopMargin() {
        return (float) this.mContext.getResources().getDimensionPixelSize(R.dimen.dual_clock_margin_top);
    }
}
