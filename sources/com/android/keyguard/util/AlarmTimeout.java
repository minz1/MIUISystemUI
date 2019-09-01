package com.android.keyguard.util;

import android.app.AlarmManager;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

public class AlarmTimeout implements AlarmManager.OnAlarmListener {
    private final AlarmManager mAlarmManager;
    private final Handler mHandler;
    private final AlarmManager.OnAlarmListener mListener;
    private boolean mScheduled;
    private final String mTag;

    public AlarmTimeout(AlarmManager alarmManager, AlarmManager.OnAlarmListener listener, String tag, Handler handler) {
        this.mAlarmManager = alarmManager;
        this.mListener = listener;
        this.mTag = tag;
        this.mHandler = handler;
    }

    public void schedule(long timeout, int mode) {
        switch (mode) {
            case 0:
                if (this.mScheduled) {
                    throw new IllegalStateException(this.mTag + " timeout is already scheduled");
                }
                break;
            case 1:
                if (this.mScheduled) {
                    return;
                }
                break;
            case 2:
                if (this.mScheduled) {
                    cancel();
                    break;
                }
                break;
            default:
                throw new IllegalArgumentException("Illegal mode: " + mode);
        }
        this.mAlarmManager.setExact(2, SystemClock.elapsedRealtime() + timeout, this.mTag, this, this.mHandler);
        Log.d("AlarmTimeout", "AlarmTimeout schedule " + this.mTag + " in " + timeout);
        this.mScheduled = true;
    }

    public boolean isScheduled() {
        return this.mScheduled;
    }

    public void cancel() {
        if (this.mScheduled) {
            this.mAlarmManager.cancel(this);
            Log.d("AlarmTimeout", "AlarmTimeout cancel " + this.mTag);
            this.mScheduled = false;
        }
    }

    public void onAlarm() {
        if (this.mScheduled) {
            this.mScheduled = false;
            this.mListener.onAlarm();
        }
    }
}
