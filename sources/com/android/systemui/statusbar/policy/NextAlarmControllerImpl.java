package com.android.systemui.statusbar.policy;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.UserHandle;
import com.android.systemui.statusbar.policy.NextAlarmController;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;

public class NextAlarmControllerImpl extends BroadcastReceiver implements NextAlarmController {
    private AlarmManager mAlarmManager;
    private final ArrayList<NextAlarmController.NextAlarmChangeCallback> mChangeCallbacks = new ArrayList<>();
    private boolean mHasSystemAlarm;
    private boolean mHasThirdPartyAlarm;

    public NextAlarmControllerImpl(Context context) {
        this.mAlarmManager = (AlarmManager) context.getSystemService("alarm");
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.ALARM_CHANGED");
        context.registerReceiverAsUser(this, UserHandle.ALL, filter, null, null);
        fireAlarmChanged();
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("NextAlarmController state:");
        pw.print("  mHasNextAlarm=");
        pw.println(this.mHasSystemAlarm || this.mHasThirdPartyAlarm);
    }

    public void addCallback(NextAlarmController.NextAlarmChangeCallback cb) {
        this.mChangeCallbacks.add(cb);
        cb.onNextAlarmChanged(this.mHasSystemAlarm || this.mHasThirdPartyAlarm);
    }

    public void removeCallback(NextAlarmController.NextAlarmChangeCallback cb) {
        this.mChangeCallbacks.remove(cb);
    }

    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.ALARM_CHANGED")) {
            boolean alarmSet = intent.getBooleanExtra("alarmSet", false);
            if (intent.getBooleanExtra("alarmSystem", false)) {
                this.mHasSystemAlarm = alarmSet;
            } else {
                this.mHasThirdPartyAlarm = alarmSet;
            }
            fireAlarmChanged();
        }
    }

    private void fireAlarmChanged() {
        int n = this.mChangeCallbacks.size();
        for (int i = 0; i < n; i++) {
            this.mChangeCallbacks.get(i).onNextAlarmChanged(this.mHasSystemAlarm || this.mHasThirdPartyAlarm);
        }
    }
}
