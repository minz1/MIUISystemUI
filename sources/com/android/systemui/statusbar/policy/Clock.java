package com.android.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.android.systemui.DemoMode;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.Util;
import com.android.systemui.statusbar.policy.DarkIconDispatcher;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TimeZone;
import miui.date.Calendar;
import miui.date.DateUtils;

public class Clock extends TextView implements DemoMode, DarkIconDispatcher.DarkReceiver {
    private static final ThreadLocal<ReceiverInfo> sReceiverInfo = new ThreadLocal<>();
    private Calendar mCalendar;
    private int mClockMode;
    private boolean mDemoMode;
    public boolean mForceHideAmPm;
    private boolean mShowAmPm;
    private LinkedList<ClockVisibilityListener> mVisibilityListeners;

    public interface ClockVisibilityListener {
        void onClockVisibilityChanged(boolean z);
    }

    private static class ReceiverInfo {
        private final ArrayList<Clock> mAttachedViews;
        private final BroadcastReceiver mReceiver;
        /* access modifiers changed from: private */
        public int mTimeFormat;

        private ReceiverInfo() {
            this.mAttachedViews = new ArrayList<>();
            this.mReceiver = new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    if ("android.intent.action.TIME_SET".equals(intent.getAction())) {
                        int unused = ReceiverInfo.this.mTimeFormat = DateFormat.is24HourFormat(context, -2) ? 32 : 16;
                    }
                    ReceiverInfo.this.updateAll();
                }
            };
            this.mTimeFormat = 16;
        }

        public int getTimeFormat() {
            return this.mTimeFormat;
        }

        public void setTimeFormat(int timeFormat) {
            this.mTimeFormat = timeFormat;
        }

        public void addView(Clock v) {
            synchronized (this.mAttachedViews) {
                boolean register = this.mAttachedViews.isEmpty();
                this.mAttachedViews.add(v);
                if (register) {
                    register(v.getContext().getApplicationContext());
                }
            }
            v.updateClock();
        }

        public void removeView(Clock v) {
            synchronized (this.mAttachedViews) {
                this.mAttachedViews.remove(v);
                if (this.mAttachedViews.isEmpty()) {
                    unregister(v.getContext().getApplicationContext());
                }
            }
        }

        /* access modifiers changed from: package-private */
        public void updateAll() {
            synchronized (this.mAttachedViews) {
                int count = this.mAttachedViews.size();
                for (int i = 0; i < count; i++) {
                    final Clock clock = this.mAttachedViews.get(i);
                    clock.post(new Runnable() {
                        public void run() {
                            clock.updateClock();
                        }
                    });
                }
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
        this.mVisibilityListeners = new LinkedList<>();
        this.mShowAmPm = true;
    }

    public void setClockMode(int clockMode) {
        if (this.mClockMode != clockMode) {
            this.mClockMode = clockMode;
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
        ri.setTimeFormat(DateFormat.is24HourFormat(this.mContext, -2) ? 32 : 16);
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

    public void setShowAmPm(boolean showAmPm) {
        this.mShowAmPm = showAmPm;
    }

    /* access modifiers changed from: package-private */
    public final void updateClock() {
        if (this.mDemoMode) {
            showDemoModeClock();
            return;
        }
        ReceiverInfo ri = sReceiverInfo.get();
        if (this.mClockMode == 2) {
            if (this.mCalendar == null) {
                this.mCalendar = new Calendar();
            }
            this.mCalendar.setTimeZone(TimeZone.getDefault());
            this.mCalendar.setTimeInMillis(System.currentTimeMillis());
            int resId = R.string.status_bar_clock_date_time_format;
            if (ri != null && ri.getTimeFormat() == 16) {
                resId = R.string.status_bar_clock_date_time_format_12;
            }
            setText(this.mCalendar.format(this.mContext.getString(resId)));
        } else if (this.mClockMode == 1) {
            if (this.mCalendar == null) {
                this.mCalendar = new Calendar();
            }
            this.mCalendar.setTimeZone(TimeZone.getDefault());
            this.mCalendar.setTimeInMillis(System.currentTimeMillis());
            int resId2 = R.string.status_bar_clock_date_format;
            if (ri != null && ri.getTimeFormat() == 16) {
                resId2 = R.string.status_bar_clock_date_format_12;
            }
            setText(this.mCalendar.format(this.mContext.getString(resId2)));
        } else if (ri != null) {
            int flags = ri.getTimeFormat();
            if (!this.mShowAmPm || this.mForceHideAmPm) {
                setText(DateUtils.formatDateTime(System.currentTimeMillis(), flags | 12 | 64));
            } else {
                setText(DateUtils.formatDateTime(System.currentTimeMillis(), flags | 12));
            }
        }
    }

    public void update() {
        updateClock();
    }

    public void onDarkChanged(Rect area, float darkIntensity, int tint) {
        boolean showCtsSpecifiedColor = Util.showCtsSpecifiedColor();
        int i = R.color.status_bar_textColor;
        if (showCtsSpecifiedColor) {
            Resources resources = getResources();
            if (DarkIconDispatcherHelper.inDarkMode(area, this, darkIntensity)) {
                i = R.color.status_bar_icon_text_color_dark_mode_cts;
            }
            setTextColor(resources.getColor(i));
            return;
        }
        Resources resources2 = getResources();
        if (DarkIconDispatcherHelper.inDarkMode(area, this, darkIntensity)) {
            i = R.color.status_bar_textColor_darkmode;
        }
        setTextColor(resources2.getColor(i));
    }

    public void dispatchDemoCommand(String command, Bundle args) {
        Log.d("demo_mode", "Clock mDemoMode = " + this.mDemoMode + ", command = " + command);
        if (!this.mDemoMode && command.equals("enter")) {
            this.mDemoMode = true;
            showDemoModeClock();
        } else if (this.mDemoMode && command.equals("exit")) {
            this.mDemoMode = false;
            updateClock();
        }
    }

    private void showDemoModeClock() {
        if (this.mCalendar == null) {
            this.mCalendar = new Calendar();
        }
        this.mCalendar.setTimeZone(TimeZone.getDefault());
        this.mCalendar.set(18, 2);
        this.mCalendar.set(20, 36);
        if (this.mClockMode == 2) {
            setText(this.mCalendar.format(this.mContext.getString(R.string.status_bar_clock_date_time_format)));
        } else if (this.mClockMode == 1) {
            setText(this.mCalendar.format(this.mContext.getString(R.string.status_bar_clock_date_format)));
        } else {
            ReceiverInfo ri = sReceiverInfo.get();
            if (ri != null) {
                setText(DateUtils.formatDateTime(this.mCalendar.getTimeInMillis(), ri.getTimeFormat() | 12));
            }
        }
    }

    /* access modifiers changed from: protected */
    public void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        Iterator it = this.mVisibilityListeners.iterator();
        while (it.hasNext()) {
            ((ClockVisibilityListener) it.next()).onClockVisibilityChanged(isShown());
        }
    }
}
