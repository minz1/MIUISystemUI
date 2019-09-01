package com.android.keyguard.doze;

import android.app.AlarmManager;
import android.os.Handler;
import com.android.keyguard.doze.DozeMachine;
import com.android.keyguard.util.AlarmTimeout;

public class DozePauser implements DozeMachine.Part {
    public static final String TAG = DozePauser.class.getSimpleName();
    private final DozeMachine mMachine;
    private final AlarmTimeout mPauseTimeout;
    private final AlwaysOnDisplayPolicy mPolicy;

    /* renamed from: com.android.keyguard.doze.DozePauser$1  reason: invalid class name */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$com$android$keyguard$doze$DozeMachine$State = new int[DozeMachine.State.values().length];

        static {
            try {
                $SwitchMap$com$android$keyguard$doze$DozeMachine$State[DozeMachine.State.DOZE_AOD_PAUSING.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
        }
    }

    public DozePauser(Handler handler, DozeMachine machine, AlarmManager alarmManager, AlwaysOnDisplayPolicy policy) {
        this.mMachine = machine;
        this.mPauseTimeout = new AlarmTimeout(alarmManager, new AlarmManager.OnAlarmListener() {
            public final void onAlarm() {
                DozePauser.this.onTimeout();
            }
        }, TAG, handler);
        this.mPolicy = policy;
    }

    public void transitionTo(DozeMachine.State oldState, DozeMachine.State newState) {
        if (AnonymousClass1.$SwitchMap$com$android$keyguard$doze$DozeMachine$State[newState.ordinal()] != 1) {
            this.mPauseTimeout.cancel();
        } else {
            this.mPauseTimeout.schedule(this.mPolicy.proxScreenOffDelayMs, 1);
        }
    }

    /* access modifiers changed from: private */
    public void onTimeout() {
        this.mMachine.requestState(DozeMachine.State.DOZE_AOD_PAUSED);
    }
}
