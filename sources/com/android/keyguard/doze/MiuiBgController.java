package com.android.keyguard.doze;

import android.app.AlarmManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import com.android.keyguard.analytics.AnalyticsHelper;
import com.android.keyguard.doze.DozeMachine;
import com.android.keyguard.util.AlarmTimeout;
import com.android.keyguard.widget.AODSettings;
import com.android.keyguard.widget.SunSelector;
import com.android.systemui.Dependency;
import com.android.systemui.doze.DozeHost;
import java.util.Calendar;

public class MiuiBgController implements DozeMachine.Part {
    private static final boolean DEBUG = DozeService.DEBUG;
    public static final String TAG = MiuiBgController.class.getSimpleName();
    private Handler mBgHandler;
    private final AlarmTimeout mChangeBgTimeout;
    /* access modifiers changed from: private */
    public Context mContext;
    private DozeHost mHost;
    private boolean mIsSunRiseOpen;
    private long mScheduleTime;

    public MiuiBgController(Context context, Handler handler, AlarmManager alarmManager, DozeHost host) {
        this.mHost = host;
        this.mChangeBgTimeout = new AlarmTimeout(alarmManager, new AlarmManager.OnAlarmListener() {
            public final void onAlarm() {
                MiuiBgController.this.changeBg();
            }
        }, "SunImageTimeout", handler);
        this.mContext = context;
        if (AODSettings.isHighPerformace()) {
            boolean z = false;
            if (!(Settings.System.getIntForUser(context.getContentResolver(), "auto_dual_clock", 0, -2) == 1) && Settings.Secure.getIntForUser(context.getContentResolver(), "aod_style_index", 0, -2) == 0) {
                z = true;
            }
            this.mIsSunRiseOpen = z;
            this.mBgHandler = new Handler((Looper) Dependency.get(Dependency.BG_LOOPER));
            this.mBgHandler.post(new Runnable() {
                public void run() {
                    SunSelector.updateSunRiseTime(MiuiBgController.this.mContext);
                }
            });
        }
    }

    public void transitionTo(DozeMachine.State oldState, DozeMachine.State newState) {
        switch (newState) {
            case INITIALIZED:
                changeBg();
                return;
            case FINISH:
                this.mChangeBgTimeout.cancel();
                return;
            default:
                return;
        }
    }

    /* access modifiers changed from: private */
    public void changeBg() {
        if (this.mIsSunRiseOpen) {
            Calendar c = Calendar.getInstance();
            int hour = c.get(11);
            int minute = c.get(12);
            int index = chooseBg((hour * 60) + minute);
            this.mHost.setSunImage(index);
            int nextChangePoint = SunSelector.getChangePoint((index + 1) % SunSelector.getChangePointLength());
            if (nextChangePoint > (hour * 60) + minute) {
                this.mScheduleTime = ((long) ((nextChangePoint - (hour * 60)) - minute)) * 60000;
            } else if (nextChangePoint >= 0) {
                this.mScheduleTime = ((long) (((nextChangePoint + 1440) - (hour * 60)) - minute)) * 60000;
            } else {
                AnalyticsHelper.recordWrongSunChangePoint(c.toString() + " get wrong sun change point " + nextChangePoint);
                return;
            }
            this.mChangeBgTimeout.schedule(this.mScheduleTime, 1);
        }
    }

    private int chooseBg(int time) {
        return SunSelector.getDrawableIndex(time);
    }
}
