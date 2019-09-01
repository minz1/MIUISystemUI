package com.android.settingslib.notification;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.content.Context;
import android.net.Uri;
import android.service.notification.Condition;
import android.service.notification.ZenModeConfig;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.Slog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.MetricsLogger;
import com.android.settingslib.R;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Objects;

public class EnableZenModeDialog {
    @VisibleForTesting
    protected static final int COUNTDOWN_ALARM_CONDITION_INDEX = 2;
    @VisibleForTesting
    protected static final int COUNTDOWN_CONDITION_INDEX = 1;
    /* access modifiers changed from: private */
    public static final boolean DEBUG = Log.isLoggable("EnableZenModeDialog", 3);
    private static final int DEFAULT_BUCKET_INDEX = Arrays.binarySearch(MINUTE_BUCKETS, 60);
    @VisibleForTesting
    protected static final int FOREVER_CONDITION_INDEX = 0;
    private static final int MAX_BUCKET_MINUTES = MINUTE_BUCKETS[MINUTE_BUCKETS.length - 1];
    private static final int[] MINUTE_BUCKETS = ZenModeConfig.MINUTE_BUCKETS;
    private static final int MIN_BUCKET_MINUTES = MINUTE_BUCKETS[0];
    private AlarmManager mAlarmManager;
    private boolean mAttached;
    private int mBucketIndex;
    @VisibleForTesting
    protected Context mContext;
    @VisibleForTesting
    protected Uri mForeverId;
    @VisibleForTesting
    protected LayoutInflater mLayoutInflater;
    @VisibleForTesting
    protected NotificationManager mNotificationManager;
    private int mUserId;
    @VisibleForTesting
    protected TextView mZenAlarmWarning;
    private RadioGroup mZenRadioGroup;
    @VisibleForTesting
    protected LinearLayout mZenRadioGroupContent;

    @VisibleForTesting
    protected static class ConditionTag {
        public Condition condition;
        public TextView line1;
        public TextView line2;
        public View lines;
        public RadioButton rb;

        protected ConditionTag() {
        }
    }

    /* access modifiers changed from: protected */
    @VisibleForTesting
    public void bind(Condition condition, View row, int rowId) {
        ConditionTag conditionTag;
        if (condition != null) {
            boolean first = true;
            boolean enabled = condition.state == 1;
            if (row.getTag() != null) {
                conditionTag = (ConditionTag) row.getTag();
            } else {
                conditionTag = new ConditionTag();
            }
            final ConditionTag tag = conditionTag;
            row.setTag(tag);
            if (tag.rb != null) {
                first = false;
            }
            if (tag.rb == null) {
                tag.rb = (RadioButton) this.mZenRadioGroup.getChildAt(rowId);
            }
            tag.condition = condition;
            final Uri conditionId = getConditionId(tag.condition);
            if (DEBUG) {
                Log.d("EnableZenModeDialog", "bind i=" + this.mZenRadioGroupContent.indexOfChild(row) + " first=" + first + " condition=" + conditionId);
            }
            tag.rb.setEnabled(enabled);
            tag.rb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        tag.rb.setChecked(true);
                        if (EnableZenModeDialog.DEBUG) {
                            Log.d("EnableZenModeDialog", "onCheckedChanged " + conditionId);
                        }
                        MetricsLogger.action(EnableZenModeDialog.this.mContext, 164);
                        EnableZenModeDialog.this.updateAlarmWarningText(tag.condition);
                    }
                }
            });
            updateUi(tag, row, condition, enabled, rowId, conditionId);
            row.setVisibility(0);
            return;
        }
        throw new IllegalArgumentException("condition must not be null");
    }

    /* access modifiers changed from: protected */
    @VisibleForTesting
    public ConditionTag getConditionTagAt(int index) {
        return (ConditionTag) this.mZenRadioGroupContent.getChildAt(index).getTag();
    }

    /* access modifiers changed from: protected */
    @VisibleForTesting
    public void bindConditions(Condition c) {
        bind(forever(), this.mZenRadioGroupContent.getChildAt(0), 0);
        if (c == null) {
            bindGenericCountdown();
            bindNextAlarm(getTimeUntilNextAlarmCondition());
        } else if (isForever(c)) {
            getConditionTagAt(0).rb.setChecked(true);
            bindGenericCountdown();
            bindNextAlarm(getTimeUntilNextAlarmCondition());
        } else if (isAlarm(c)) {
            bindGenericCountdown();
            bindNextAlarm(c);
            getConditionTagAt(2).rb.setChecked(true);
        } else if (isCountdown(c)) {
            bindNextAlarm(getTimeUntilNextAlarmCondition());
            bind(c, this.mZenRadioGroupContent.getChildAt(1), 1);
            getConditionTagAt(1).rb.setChecked(true);
        } else {
            Slog.d("EnableZenModeDialog", "Invalid manual condition: " + c);
        }
    }

    public static Uri getConditionId(Condition condition) {
        if (condition != null) {
            return condition.id;
        }
        return null;
    }

    public Condition forever() {
        Condition condition = new Condition(Condition.newId(this.mContext).appendPath("forever").build(), foreverSummary(this.mContext), "", "", 0, 1, 0);
        return condition;
    }

    public long getNextAlarm() {
        AlarmManager.AlarmClockInfo info = this.mAlarmManager.getNextAlarmClock(this.mUserId);
        if (info != null) {
            return info.getTriggerTime();
        }
        return 0;
    }

    /* access modifiers changed from: protected */
    @VisibleForTesting
    public boolean isAlarm(Condition c) {
        return c != null && ZenModeConfig.isValidCountdownToAlarmConditionId(c.id);
    }

    /* access modifiers changed from: protected */
    @VisibleForTesting
    public boolean isCountdown(Condition c) {
        return c != null && ZenModeConfig.isValidCountdownConditionId(c.id);
    }

    private boolean isForever(Condition c) {
        return c != null && this.mForeverId.equals(c.id);
    }

    private String foreverSummary(Context context) {
        return context.getString(17041263);
    }

    private static void setToMidnight(Calendar calendar) {
        calendar.set(11, 0);
        calendar.set(12, 0);
        calendar.set(13, 0);
        calendar.set(14, 0);
    }

    /* access modifiers changed from: protected */
    @VisibleForTesting
    public Condition getTimeUntilNextAlarmCondition() {
        GregorianCalendar weekRange = new GregorianCalendar();
        setToMidnight(weekRange);
        weekRange.add(5, 6);
        long nextAlarmMs = getNextAlarm();
        if (nextAlarmMs > 0) {
            GregorianCalendar nextAlarm = new GregorianCalendar();
            nextAlarm.setTimeInMillis(nextAlarmMs);
            setToMidnight(nextAlarm);
            if (weekRange.compareTo(nextAlarm) >= 0) {
                return ZenModeConfig.toNextAlarmCondition(this.mContext, nextAlarmMs, ActivityManager.getCurrentUser());
            }
        }
        return null;
    }

    /* access modifiers changed from: protected */
    @VisibleForTesting
    public void bindGenericCountdown() {
        this.mBucketIndex = DEFAULT_BUCKET_INDEX;
        Condition countdown = ZenModeConfig.toTimeCondition(this.mContext, MINUTE_BUCKETS[this.mBucketIndex], ActivityManager.getCurrentUser());
        if (!this.mAttached || getConditionTagAt(1).condition == null) {
            bind(countdown, this.mZenRadioGroupContent.getChildAt(1), 1);
        }
    }

    private void updateUi(ConditionTag tag, View row, Condition condition, boolean enabled, int rowId, Uri conditionId) {
        String line1;
        final ConditionTag conditionTag = tag;
        final View view = row;
        Condition condition2 = condition;
        boolean z = enabled;
        final int i = rowId;
        boolean z2 = true;
        if (conditionTag.lines == null) {
            conditionTag.lines = view.findViewById(16908290);
            conditionTag.lines.setAccessibilityLiveRegion(1);
        }
        if (conditionTag.line1 == null) {
            conditionTag.line1 = (TextView) view.findViewById(16908308);
        }
        if (conditionTag.line2 == null) {
            conditionTag.line2 = (TextView) view.findViewById(16908309);
        }
        if (!TextUtils.isEmpty(condition2.line1)) {
            line1 = condition2.line1;
        } else {
            line1 = condition2.summary;
        }
        String line2 = condition2.line2;
        conditionTag.line1.setText(line1);
        if (TextUtils.isEmpty(line2)) {
            conditionTag.line2.setVisibility(8);
        } else {
            conditionTag.line2.setVisibility(0);
            conditionTag.line2.setText(line2);
        }
        conditionTag.lines.setEnabled(z);
        conditionTag.lines.setAlpha(z ? 1.0f : 0.4f);
        conditionTag.lines.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                conditionTag.rb.setChecked(true);
            }
        });
        ImageView button1 = (ImageView) view.findViewById(16908313);
        button1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                EnableZenModeDialog.this.onClickTimeButton(view, conditionTag, false, i);
            }
        });
        ImageView button2 = (ImageView) view.findViewById(16908314);
        button2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                EnableZenModeDialog.this.onClickTimeButton(view, conditionTag, true, i);
            }
        });
        long time = ZenModeConfig.tryParseCountdownConditionId(conditionId);
        if (i != 1 || time <= 0) {
            button1.setVisibility(8);
            button2.setVisibility(8);
            return;
        }
        button1.setVisibility(0);
        button2.setVisibility(0);
        if (this.mBucketIndex > -1) {
            button1.setEnabled(this.mBucketIndex > 0);
            if (this.mBucketIndex >= MINUTE_BUCKETS.length - 1) {
                z2 = false;
            }
            button2.setEnabled(z2);
            String str = line2;
        } else {
            String str2 = line2;
            button1.setEnabled(time - System.currentTimeMillis() > ((long) (MIN_BUCKET_MINUTES * 60000)));
            button2.setEnabled(!Objects.equals(condition2.summary, ZenModeConfig.toTimeCondition(this.mContext, MAX_BUCKET_MINUTES, ActivityManager.getCurrentUser()).summary));
        }
        float f = 0.5f;
        button1.setAlpha(button1.isEnabled() ? 1.0f : 0.5f);
        if (button2.isEnabled()) {
            f = 1.0f;
        }
        button2.setAlpha(f);
    }

    /* access modifiers changed from: protected */
    @VisibleForTesting
    public void bindNextAlarm(Condition c) {
        View alarmContent = this.mZenRadioGroupContent.getChildAt(2);
        ConditionTag tag = (ConditionTag) alarmContent.getTag();
        if (c != null && (!this.mAttached || tag == null || tag.condition == null)) {
            bind(c, alarmContent, 2);
        }
        ConditionTag tag2 = (ConditionTag) alarmContent.getTag();
        int i = 0;
        boolean showAlarm = (tag2 == null || tag2.condition == null) ? false : true;
        this.mZenRadioGroup.getChildAt(2).setVisibility(showAlarm ? 0 : 8);
        if (!showAlarm) {
            i = 8;
        }
        alarmContent.setVisibility(i);
    }

    /* access modifiers changed from: private */
    public void onClickTimeButton(View row, ConditionTag tag, boolean up, int rowId) {
        int j;
        int bucketMinutes;
        long bucketTime;
        ConditionTag conditionTag = tag;
        boolean z = up;
        MetricsLogger.action(this.mContext, 163, z);
        Condition newCondition = null;
        int N = MINUTE_BUCKETS.length;
        int i = 0;
        int i2 = -1;
        if (this.mBucketIndex == -1) {
            long time = ZenModeConfig.tryParseCountdownConditionId(getConditionId(conditionTag.condition));
            long now = System.currentTimeMillis();
            while (true) {
                if (i >= N) {
                    break;
                }
                j = z ? i : (N - 1) - i;
                bucketMinutes = MINUTE_BUCKETS[j];
                bucketTime = now + ((long) (60000 * bucketMinutes));
                if ((!z || bucketTime <= time) && (z || bucketTime >= time)) {
                    i++;
                }
            }
            this.mBucketIndex = j;
            newCondition = ZenModeConfig.toTimeCondition(this.mContext, bucketTime, bucketMinutes, ActivityManager.getCurrentUser(), false);
            if (newCondition == null) {
                this.mBucketIndex = DEFAULT_BUCKET_INDEX;
                newCondition = ZenModeConfig.toTimeCondition(this.mContext, MINUTE_BUCKETS[this.mBucketIndex], ActivityManager.getCurrentUser());
            }
        } else {
            int i3 = N - 1;
            int i4 = this.mBucketIndex;
            if (z) {
                i2 = 1;
            }
            this.mBucketIndex = Math.max(0, Math.min(i3, i4 + i2));
            newCondition = ZenModeConfig.toTimeCondition(this.mContext, MINUTE_BUCKETS[this.mBucketIndex], ActivityManager.getCurrentUser());
        }
        bind(newCondition, row, rowId);
        updateAlarmWarningText(conditionTag.condition);
        conditionTag.rb.setChecked(true);
    }

    /* access modifiers changed from: private */
    public void updateAlarmWarningText(Condition condition) {
        String warningText = computeAlarmWarningText(condition);
        this.mZenAlarmWarning.setText(warningText);
        this.mZenAlarmWarning.setVisibility(warningText == null ? 8 : 0);
    }

    /* access modifiers changed from: protected */
    @VisibleForTesting
    public String computeAlarmWarningText(Condition condition) {
        if ((this.mNotificationManager.getNotificationPolicy().priorityCategories & 32) != 0) {
            return null;
        }
        long now = System.currentTimeMillis();
        long nextAlarm = getNextAlarm();
        if (nextAlarm < now) {
            return null;
        }
        int warningRes = 0;
        if (condition == null || isForever(condition)) {
            warningRes = R.string.zen_alarm_warning_indef;
        } else {
            long time = ZenModeConfig.tryParseCountdownConditionId(condition.id);
            if (time > now && nextAlarm < time) {
                warningRes = R.string.zen_alarm_warning;
            }
        }
        if (warningRes == 0) {
            return null;
        }
        return this.mContext.getResources().getString(warningRes, new Object[]{getTime(nextAlarm, now)});
    }

    /* access modifiers changed from: protected */
    @VisibleForTesting
    public String getTime(long nextAlarm, long now) {
        boolean soon = nextAlarm - now < 86400000;
        boolean is24 = DateFormat.is24HourFormat(this.mContext, ActivityManager.getCurrentUser());
        return this.mContext.getResources().getString(soon ? R.string.alarm_template : R.string.alarm_template_far, new Object[]{DateFormat.format(DateFormat.getBestDateTimePattern(Locale.getDefault(), soon ? is24 ? "Hm" : "hma" : is24 ? "EEEHm" : "EEEhma"), nextAlarm)});
    }
}
