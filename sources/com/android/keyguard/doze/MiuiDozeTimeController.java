package com.android.keyguard.doze;

import android.app.AlarmManager;
import android.content.Context;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import com.android.keyguard.doze.DozeMachine;
import com.android.keyguard.util.AlarmTimeout;
import com.android.keyguard.widget.AODSettings;
import com.android.systemui.doze.DozeHost;
import java.util.Calendar;

public class MiuiDozeTimeController implements DozeMachine.Part {
    private static final boolean DEBUG = DozeService.DEBUG;
    public static final String TAG = MiuiDozeTimeController.class.getSimpleName();
    private Context mContext;
    private DozeTriggers mDozeTriggers;
    private final AlarmTimeout mHideDozeTimeout;
    private long mHideTime;
    private DozeHost mHost;
    private final DozeMachine mMachine;
    private DozeMachine.Service mService;
    private final AlarmTimeout mShowDozeTimeout;
    private long mShowTime;
    private boolean mTimeSet = false;

    public MiuiDozeTimeController(Context context, Handler handler, DozeMachine machine, AlarmManager alarmManager, DozeTriggers dozeTriggers, DozeMachine.Service service, DozeHost host) {
        boolean z = false;
        this.mContext = context;
        this.mMachine = machine;
        this.mDozeTriggers = dozeTriggers;
        this.mService = service;
        this.mHost = host;
        this.mShowDozeTimeout = new AlarmTimeout(alarmManager, new AlarmManager.OnAlarmListener() {
            public final void onAlarm() {
                MiuiDozeTimeController.this.showDoze();
            }
        }, "DarkenAlarmTimeout", handler);
        this.mHideDozeTimeout = new AlarmTimeout(alarmManager, new AlarmManager.OnAlarmListener() {
            public final void onAlarm() {
                MiuiDozeTimeController.this.hideDoze();
            }
        }, "OffAlarmTimeout", handler);
        if (Settings.Secure.getIntForUser(this.mContext.getContentResolver(), AODSettings.AOD_MODE, 0, -2) == 1 && Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "aod_mode_time", 0, -2) == 1) {
            z = true;
        }
        this.mTimeSet = z;
        if (this.mTimeSet) {
            checkTime();
        }
    }

    private void checkTime() {
        this.mShowTime = 0;
        this.mHideTime = 0;
        long startTime = ((long) Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "aod_start", 420, -2)) * 60000;
        long endTime = ((long) Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "aod_end", 1380, -2)) * 60000;
        Calendar c = Calendar.getInstance();
        long time = (((long) ((c.get(11) * 60) + c.get(12))) * 60000) + 1;
        if (startTime <= endTime) {
            if (time < startTime || time > endTime) {
                this.mShowTime = startTime > time ? startTime - time : (startTime - time) + 86400000;
            } else {
                this.mHideTime = endTime - time;
            }
        } else if (startTime <= endTime) {
        } else {
            if (time >= startTime || time <= endTime) {
                this.mHideTime = endTime > time ? endTime - time : (86400000 + endTime) - time;
            } else {
                this.mShowTime = startTime > time ? startTime - time : (startTime - time) + 86400000;
            }
        }
    }

    public void transitionTo(DozeMachine.State oldState, DozeMachine.State newState) {
        if (this.mTimeSet) {
            switch (newState) {
                case INITIALIZED:
                    if (this.mShowTime > 0) {
                        this.mShowDozeTimeout.schedule(this.mShowTime, 1);
                        return;
                    } else if (this.mHideTime > 0) {
                        this.mHideDozeTimeout.schedule(this.mHideTime, 1);
                        return;
                    } else {
                        return;
                    }
                case FINISH:
                    this.mShowDozeTimeout.cancel();
                    this.mHideDozeTimeout.cancel();
                    return;
                default:
                    return;
            }
        }
    }

    /* access modifiers changed from: private */
    public void showDoze() {
        onShowDoze(true);
        checkTime();
        this.mHideDozeTimeout.schedule(this.mHideTime, 1);
    }

    /* access modifiers changed from: private */
    public void hideDoze() {
        onShowDoze(false);
        checkTime();
        this.mShowDozeTimeout.schedule(this.mShowTime, 1);
    }

    private void onShowDoze(boolean show) {
        if (DEBUG) {
            Log.i(TAG, "onShowDoze:" + show);
        }
        DozeMachine.State state = this.mMachine.getState();
        boolean doze = false;
        boolean paused = state == DozeMachine.State.DOZE_AOD_PAUSED;
        boolean pausing = state == DozeMachine.State.DOZE_AOD_PAUSING;
        boolean dozeAod = state == DozeMachine.State.DOZE_AOD;
        if (state == DozeMachine.State.DOZE) {
            doze = true;
        }
        if (show) {
            if (doze) {
                if (DEBUG) {
                    Log.i(TAG, "Show, unpausing AOD");
                }
                this.mService.requestState(DozeMachine.State.DOZE_AOD);
            }
        } else if (dozeAod || paused || paused || pausing) {
            if (DEBUG) {
                Log.i(TAG, "Hide, pausing AOD");
            }
            this.mService.requestState(DozeMachine.State.DOZE);
        }
        this.mHost.setAodClockVisibility(show);
    }
}
