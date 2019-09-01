package com.android.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.icu.text.DateFormat;
import android.icu.text.DisplayContext;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.TextView;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import java.util.Date;
import java.util.Locale;

public class DateView extends TextView {
    private final Date mCurrentTime = new Date();
    /* access modifiers changed from: private */
    public DateFormat mDateFormat;
    private String mDatePattern;
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.TIME_TICK".equals(action) || "android.intent.action.TIME_SET".equals(action) || "android.intent.action.TIMEZONE_CHANGED".equals(action) || "android.intent.action.LOCALE_CHANGED".equals(action)) {
                if ("android.intent.action.LOCALE_CHANGED".equals(action) || "android.intent.action.TIMEZONE_CHANGED".equals(action)) {
                    DateView.this.getHandler().post(new Runnable() {
                        public void run() {
                            DateFormat unused = DateView.this.mDateFormat = null;
                        }
                    });
                }
                DateView.this.getHandler().post(new Runnable() {
                    public void run() {
                        DateView.this.updateClock();
                    }
                });
            }
        }
    };
    private String mLastText;

    /* JADX INFO: finally extract failed */
    public DateView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.DateView, 0, 0);
        try {
            this.mDatePattern = a.getString(0);
            a.recycle();
            if (this.mDatePattern == null) {
                this.mDatePattern = getContext().getString(R.string.system_ui_date_pattern);
            }
        } catch (Throwable th) {
            a.recycle();
            throw th;
        }
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.TIME_TICK");
        filter.addAction("android.intent.action.TIME_SET");
        filter.addAction("android.intent.action.TIMEZONE_CHANGED");
        filter.addAction("android.intent.action.LOCALE_CHANGED");
        getContext().registerReceiver(this.mIntentReceiver, filter, null, (Handler) Dependency.get(Dependency.TIME_TICK_HANDLER));
        updateClock();
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mDateFormat = null;
        getContext().unregisterReceiver(this.mIntentReceiver);
    }

    /* access modifiers changed from: protected */
    public void updateClock() {
        if (this.mDateFormat == null) {
            DateFormat format = DateFormat.getInstanceForSkeleton(this.mDatePattern, Locale.getDefault());
            format.setContext(DisplayContext.CAPITALIZATION_FOR_STANDALONE);
            this.mDateFormat = format;
        }
        this.mCurrentTime.setTime(System.currentTimeMillis());
        String text = this.mDateFormat.format(this.mCurrentTime);
        if (!text.equals(this.mLastText)) {
            setText(text);
            this.mLastText = text;
        }
    }

    public void setDatePattern(String pattern) {
        if (!TextUtils.equals(pattern, this.mDatePattern)) {
            this.mDatePattern = pattern;
            this.mDateFormat = null;
            if (isAttachedToWindow()) {
                updateClock();
            }
        }
    }
}
