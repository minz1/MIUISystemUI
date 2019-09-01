package com.android.systemui.statusbar.policy;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.UserHandle;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextClock;
import com.android.systemui.R;

public class SplitClockView extends LinearLayout {
    private TextClock mAmPmView;
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.TIME_SET".equals(action) || "android.intent.action.TIMEZONE_CHANGED".equals(action) || "android.intent.action.LOCALE_CHANGED".equals(action) || "android.intent.action.CONFIGURATION_CHANGED".equals(action) || "android.intent.action.USER_SWITCHED".equals(action)) {
                SplitClockView.this.updatePatterns();
            }
        }
    };
    private TextClock mTimeView;

    public SplitClockView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mTimeView = (TextClock) findViewById(R.id.time_view);
        this.mAmPmView = (TextClock) findViewById(R.id.am_pm_view);
        this.mTimeView.setShowCurrentUserTime(true);
        this.mAmPmView.setShowCurrentUserTime(true);
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.TIME_SET");
        filter.addAction("android.intent.action.TIMEZONE_CHANGED");
        filter.addAction("android.intent.action.LOCALE_CHANGED");
        filter.addAction("android.intent.action.CONFIGURATION_CHANGED");
        filter.addAction("android.intent.action.USER_SWITCHED");
        getContext().registerReceiverAsUser(this.mIntentReceiver, UserHandle.ALL, filter, null, null);
        updatePatterns();
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getContext().unregisterReceiver(this.mIntentReceiver);
    }

    /* access modifiers changed from: private */
    public void updatePatterns() {
        String amPmString;
        String timeString;
        String formatString = DateFormat.getTimeFormatString(getContext(), ActivityManager.getCurrentUser());
        int index = getAmPmPartEndIndex(formatString);
        if (index == -1) {
            timeString = formatString;
            amPmString = "";
        } else {
            timeString = formatString.substring(0, index);
            amPmString = formatString.substring(index);
        }
        this.mTimeView.setFormat12Hour(timeString);
        this.mTimeView.setFormat24Hour(timeString);
        this.mTimeView.setContentDescriptionFormat12Hour(formatString);
        this.mTimeView.setContentDescriptionFormat24Hour(formatString);
        this.mAmPmView.setFormat12Hour(amPmString);
        this.mAmPmView.setFormat24Hour(amPmString);
    }

    private static int getAmPmPartEndIndex(String formatString) {
        boolean hasAmPm = false;
        int i = formatString.length() - 1;
        while (true) {
            boolean isAmPm = false;
            int i2 = -1;
            if (i >= 0) {
                char c = formatString.charAt(i);
                if (c == 'a') {
                    isAmPm = true;
                }
                boolean isWhitespace = Character.isWhitespace(c);
                if (isAmPm) {
                    hasAmPm = true;
                }
                if (isAmPm || isWhitespace) {
                    i--;
                } else if (i == length - 1) {
                    return -1;
                } else {
                    if (hasAmPm) {
                        i2 = i + 1;
                    }
                    return i2;
                }
            } else {
                if (!hasAmPm) {
                    isAmPm = true;
                }
                return isAmPm;
            }
        }
    }
}
