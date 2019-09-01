package com.android.keyguard.smartcover;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.UserHandle;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.widget.TextView;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import java.util.ArrayList;
import java.util.Calendar;
import miui.date.DateUtils;

public class Clock extends TextView {
    /* access modifiers changed from: private */
    public static Calendar sCalendar;
    private static final ThreadLocal<ReceiverInfo> sReceiverInfo = new ThreadLocal<>();
    private boolean mShowDate;
    private boolean mShowHour;
    private boolean mShowMinute;

    private static class ReceiverInfo {
        private final ArrayList<Clock> mAttachedViews;
        /* access modifiers changed from: private */
        public final Handler mHandler;
        private final BroadcastReceiver mReceiver;
        Runnable mUpdateRunnable;

        private ReceiverInfo() {
            this.mAttachedViews = new ArrayList<>();
            this.mHandler = new Handler();
            this.mReceiver = new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    if (intent.getAction().equals("android.intent.action.TIMEZONE_CHANGED")) {
                        Calendar unused = Clock.sCalendar = Calendar.getInstance();
                    }
                    ReceiverInfo.this.mHandler.post(ReceiverInfo.this.mUpdateRunnable);
                }
            };
            this.mUpdateRunnable = new Runnable() {
                public void run() {
                    ReceiverInfo.this.updateAll();
                }
            };
        }

        public void addView(Clock v) {
            boolean register = this.mAttachedViews.isEmpty();
            this.mAttachedViews.add(v);
            if (register) {
                register(v.getContext().getApplicationContext());
            }
            v.updateClock();
        }

        public void removeView(Clock v) {
            this.mAttachedViews.remove(v);
            if (this.mAttachedViews.isEmpty()) {
                unregister(v.getContext().getApplicationContext());
            }
        }

        /* access modifiers changed from: package-private */
        public void updateAll() {
            int count = this.mAttachedViews.size();
            for (int i = 0; i < count; i++) {
                this.mAttachedViews.get(i).updateClock();
            }
        }

        /* access modifiers changed from: package-private */
        public void register(Context context) {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.TIME_TICK");
            filter.addAction("android.intent.action.TIME_SET");
            filter.addAction("android.intent.action.TIMEZONE_CHANGED");
            filter.addAction("android.intent.action.CONFIGURATION_CHANGED");
            context.registerReceiverAsUser(this.mReceiver, UserHandle.ALL, filter, null, (Handler) Dependency.get(Dependency.TIME_TICK_HANDLER));
        }

        /* access modifiers changed from: package-private */
        public void unregister(Context context) {
            context.unregisterReceiver(this.mReceiver);
            this.mHandler.removeCallbacks(this.mUpdateRunnable);
        }
    }

    public Clock(Context context) {
        this(context, null);
    }

    public Clock(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Clock(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        if (sCalendar == null) {
            sCalendar = Calendar.getInstance();
        }
    }

    public void setShowDate(boolean showDate) {
        if (this.mShowDate != showDate) {
            this.mShowDate = showDate;
            updateClock();
        }
    }

    public void setShowHour(boolean showHour) {
        if (this.mShowHour != showHour) {
            this.mShowHour = showHour;
            updateClock();
        }
    }

    public void setShowMinute(boolean showMinute) {
        if (this.mShowMinute != showMinute) {
            this.mShowMinute = showMinute;
            updateClock();
        }
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        ReceiverInfo ri = sReceiverInfo.get();
        if (ri == null) {
            ri = new ReceiverInfo();
            sReceiverInfo.set(ri);
        }
        ri.addView(this);
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ReceiverInfo ri = sReceiverInfo.get();
        if (ri != null) {
            ri.removeView(this);
        }
    }

    /* access modifiers changed from: package-private */
    public final void updateClock() {
        int flags;
        sCalendar.setTimeInMillis(System.currentTimeMillis());
        boolean is24HourFormat = DateFormat.is24HourFormat(this.mContext, -2);
        if (this.mShowDate) {
            setText(DateFormat.format(this.mContext.getString(is24HourFormat ? R.string.lock_screen_date : R.string.lock_screen_date_12), sCalendar));
            return;
        }
        int hour = 12;
        if (this.mShowHour) {
            int hour2 = sCalendar.get(11);
            int hour3 = (is24HourFormat || hour2 <= 12) ? hour2 : hour2 - 12;
            if (is24HourFormat || hour3 != 0) {
                hour = hour3;
            }
            setText(String.valueOf(hour));
        } else if (this.mShowMinute) {
            setText(String.format("%02d", new Object[]{Integer.valueOf(sCalendar.get(12))}));
        } else {
            if (is24HourFormat) {
                flags = 32;
            } else {
                flags = 16;
            }
            setText(DateUtils.formatDateTime(System.currentTimeMillis(), flags | 12 | 64));
        }
    }
}
