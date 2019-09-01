package com.android.keyguard.doze;

import android.app.AlarmManager;
import android.content.Context;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.Log;
import com.android.internal.hardware.AmbientDisplayConfiguration;
import com.android.keyguard.doze.DozeMachine;
import com.android.keyguard.phone.DozeParameters;
import com.android.keyguard.util.AsyncSensorManager;
import com.android.keyguard.util.wakelock.DelayedWakeLock;
import com.android.keyguard.util.wakelock.WakeLock;
import com.android.systemui.Dependency;
import com.android.systemui.doze.DozeHost;

public class DozeFactory {
    public DozeMachine assembleMachine(DozeService dozeService) {
        DozeService dozeService2 = dozeService;
        SensorManager sensorManager = new AsyncSensorManager((SensorManager) dozeService2.getSystemService(SensorManager.class));
        AlarmManager alarmManager = (AlarmManager) dozeService2.getSystemService(AlarmManager.class);
        DozeHost host = getHost(dozeService);
        AmbientDisplayConfiguration config = new AmbientDisplayConfiguration(dozeService2);
        DozeParameters params = new DozeParameters(dozeService2);
        Handler handler = new Handler();
        WakeLock wakeLock = new DelayedWakeLock(handler, WakeLock.createPartial(dozeService2, "Doze"));
        DozeMachine.Service wrappedService = new MiuiDozeBrightnessTimeoutAdapter(MiuiAnimDozeStatePreventingAdapter.wrapIfNeeded(MiuiGxzwDozeStatePreventingAdapter.wrapIfNeeded(DozeSuspendScreenStatePreventingAdapter.wrapIfNeeded(DozeScreenStatePreventingAdapter.wrapIfNeeded(new DozeBrightnessHostForwarder(dozeService, host), params), params), host, dozeService2), dozeService2), alarmManager, handler);
        DozeMachine machine = new DozeMachine(wrappedService, config, wakeLock, dozeService2);
        DozePauser dozePauser = new DozePauser(handler, machine, alarmManager, new AlwaysOnDisplayPolicy(dozeService2));
        DozeMachine machine2 = machine;
        DozeMachine.Service wrappedService2 = wrappedService;
        WakeLock wakeLock2 = wakeLock;
        Handler handler2 = handler;
        DozeTriggers dozeTriggers = createDozeTriggers(dozeService2, sensorManager, host, alarmManager, config, params, handler, wakeLock, machine2, wrappedService2);
        DozeMachine.Service wrappedService3 = wrappedService2;
        Handler handler3 = handler2;
        Handler handler4 = handler3;
        DozeParameters dozeParameters = params;
        MiuiDozeScreenBrightnessController miuiDozeScreenBrightnessController = new MiuiDozeScreenBrightnessController(handler3, machine2, alarmManager, wrappedService3, host, sensorManager, dozeTriggers, dozeService2);
        MiuiDozeTimeController miuiDozeTimeController = new MiuiDozeTimeController(dozeService2, handler4, machine2, alarmManager, dozeTriggers, wrappedService3, host);
        DozeMachine.Part[] partArr = {dozePauser, dozeTriggers, createDozeUi(dozeService2, host, wakeLock2, machine2, handler2, alarmManager), new DozeScreenState(wrappedService3, handler3), miuiDozeScreenBrightnessController, miuiDozeTimeController, new MiuiBgController(dozeService2, handler4, alarmManager, host)};
        DozeMachine machine3 = machine2;
        machine3.setParts(partArr);
        return machine3;
    }

    private DozeTriggers createDozeTriggers(Context context, SensorManager sensorManager, DozeHost host, AlarmManager alarmManager, AmbientDisplayConfiguration config, DozeParameters params, Handler handler, WakeLock wakeLock, DozeMachine machine, DozeMachine.Service service) {
        DozeTriggers dozeTriggers = new DozeTriggers(context, machine, host, alarmManager, config, params, sensorManager, handler, wakeLock, true, service);
        return dozeTriggers;
    }

    private DozeMachine.Part createDozeUi(Context context, DozeHost host, WakeLock wakeLock, DozeMachine machine, Handler handler, AlarmManager alarmManager) {
        DozeUi dozeUi = new DozeUi(context, alarmManager, machine, wakeLock, host, handler);
        return dozeUi;
    }

    public static DozeHost getHost(DozeService service) {
        Log.d("DOZE", "" + service.getApplication());
        return Dependency.getHost();
    }
}
