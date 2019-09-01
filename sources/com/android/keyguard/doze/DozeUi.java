package com.android.keyguard.doze;

import android.app.AlarmManager;
import android.content.Context;
import android.os.Handler;
import android.os.SystemClock;
import android.text.format.Formatter;
import android.util.Log;
import com.android.keyguard.doze.DozeMachine;
import com.android.keyguard.util.AlarmTimeout;
import com.android.keyguard.util.wakelock.WakeLock;
import com.android.systemui.doze.DozeHost;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class DozeUi implements DozeMachine.Part {
    private final Context mContext;
    private final Handler mHandler;
    private final DozeHost mHost;
    private long mLastTimeTickElapsed = 0;
    /* access modifiers changed from: private */
    public final DozeMachine mMachine;
    private final AlarmTimeout mTimeTicker;
    private final WakeLock mWakeLock;

    public DozeUi(Context context, AlarmManager alarmManager, DozeMachine machine, WakeLock wakeLock, DozeHost host, Handler handler) {
        this.mContext = context;
        this.mMachine = machine;
        this.mWakeLock = wakeLock;
        this.mHost = host;
        this.mHandler = handler;
        this.mTimeTicker = new AlarmTimeout(alarmManager, new AlarmManager.OnAlarmListener() {
            public final void onAlarm() {
                DozeUi.this.onTimeTick();
            }
        }, "doze_time_tick", handler);
    }

    private void pulseWhileDozing(int reason) {
        this.mHost.pulseWhileDozing(new DozeHost.PulseCallback() {
            public void onPulseStarted() {
                DozeUi.this.mMachine.requestState(DozeMachine.State.DOZE_PULSING);
            }

            public void onPulseFinished() {
                DozeUi.this.mMachine.requestState(DozeMachine.State.DOZE_PULSE_DONE);
            }
        }, reason);
    }

    public void transitionTo(DozeMachine.State oldState, DozeMachine.State newState) {
        switch (newState) {
            case DOZE_AOD:
                if (oldState != DozeMachine.State.DOZE_AOD_PAUSING) {
                    this.mHost.dozeTimeTick();
                    break;
                }
                break;
            case DOZE_AOD_PAUSING:
                break;
            case DOZE:
            case DOZE_AOD_PAUSED:
                unscheduleTimeTick();
                break;
            case DOZE_REQUEST_PULSE:
                pulseWhileDozing(this.mMachine.getPulseReason());
                break;
            case INITIALIZED:
                this.mHost.startDozing();
                break;
            case FINISH:
                this.mHost.stopDozing();
                unscheduleTimeTick();
                break;
        }
        scheduleTimeTick();
        updateAnimateWakeup(newState);
    }

    private void updateAnimateWakeup(DozeMachine.State state) {
        int i = AnonymousClass2.$SwitchMap$com$android$keyguard$doze$DozeMachine$State[state.ordinal()];
        if (i != 5) {
            switch (i) {
                case 7:
                    return;
                case 8:
                case 9:
                    break;
                default:
                    this.mHost.setAnimateWakeup(false);
                    return;
            }
        }
        this.mHost.setAnimateWakeup(true);
    }

    private void scheduleTimeTick() {
        if (!this.mTimeTicker.isScheduled()) {
            this.mTimeTicker.schedule(roundToNextMinute(System.currentTimeMillis()) - System.currentTimeMillis(), 1);
            this.mLastTimeTickElapsed = SystemClock.elapsedRealtime();
        }
    }

    private void unscheduleTimeTick() {
        if (this.mTimeTicker.isScheduled()) {
            verifyLastTimeTick();
            this.mTimeTicker.cancel();
        }
    }

    private void verifyLastTimeTick() {
        long millisSinceLastTick = SystemClock.elapsedRealtime() - this.mLastTimeTickElapsed;
        if (millisSinceLastTick > 90000) {
            String delay = Formatter.formatShortElapsedTime(this.mContext, millisSinceLastTick);
            DozeLog.traceMissedTick(delay);
            Log.e("DozeMachine", "Missed AOD time tick by " + delay);
        }
    }

    private long roundToNextMinute(long timeInMillis) {
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTimeInMillis(timeInMillis);
        calendar.set(14, 0);
        calendar.set(13, 0);
        calendar.add(12, 1);
        return calendar.getTimeInMillis();
    }

    /* access modifiers changed from: private */
    public void onTimeTick() {
        verifyLastTimeTick();
        this.mHost.dozeTimeTick();
        this.mHandler.post(this.mWakeLock.wrap((Runnable) $$Lambda$DozeUi$MuB8A_YeFaloBCPLd59gFWKOpfA.INSTANCE));
        scheduleTimeTick();
    }

    static /* synthetic */ void lambda$onTimeTick$0() {
    }
}
