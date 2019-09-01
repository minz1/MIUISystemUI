package com.android.systemui.statistic;

import android.os.statistics.E2EScenario;
import android.os.statistics.E2EScenarioPayload;
import android.os.statistics.E2EScenarioPerfTracer;
import android.os.statistics.E2EScenarioSettings;
import android.util.Log;
import java.util.Map;

public class ScenarioTrackUtil {
    private static final String TAG = ScenarioTrackUtil.class.getSimpleName();
    private static E2EScenarioSettings sScenarioSettings = new E2EScenarioSettings();

    public static class SystemUIEventScenario {
        public volatile boolean isTrackStarted = false;
        E2EScenario mE2eScenario;
        String mEventName;

        SystemUIEventScenario(String eventName) {
            this.mE2eScenario = initE2EScenario(eventName);
            this.mEventName = eventName;
        }

        private E2EScenario initE2EScenario(String eventName) {
            return new E2EScenario("com.android.systemui", "Performance", eventName);
        }

        public String toString() {
            return this.mEventName;
        }
    }

    static {
        sScenarioSettings.setStatisticsMode(7);
        sScenarioSettings.setHistoryLimitPerDay(200);
    }

    private ScenarioTrackUtil() {
    }

    public static void beginScenario(SystemUIEventScenario scenario) {
        beginScenario(scenario, null);
    }

    public static void beginScenario(SystemUIEventScenario scenario, Map payLoadMap) {
        if (scenario.mE2eScenario == null) {
            String str = TAG;
            Log.w(str, scenario.toString() + " event start cancel due to scenario is null!");
            return;
        }
        if (scenario.isTrackStarted) {
            E2EScenarioPerfTracer.abortScenario(scenario.mE2eScenario);
        }
        if (payLoadMap != null) {
            E2EScenarioPayload payload = new E2EScenarioPayload();
            payload.putAll(payLoadMap);
            E2EScenarioPerfTracer.asyncBeginScenario(scenario.mE2eScenario, sScenarioSettings, payload);
        } else {
            E2EScenarioPerfTracer.asyncBeginScenario(scenario.mE2eScenario, sScenarioSettings);
        }
        scenario.isTrackStarted = true;
    }

    public static void finishScenario(SystemUIEventScenario scenario) {
        if (scenario.mE2eScenario == null) {
            String str = TAG;
            Log.w(str, scenario.toString() + " event end cancel, due to scenario is null!");
        } else if (!scenario.isTrackStarted) {
            String str2 = TAG;
            Log.w(str2, scenario.toString() + " event end cancel, due to scenario has not started!");
        } else {
            E2EScenarioPerfTracer.finishScenario(scenario.mE2eScenario);
            scenario.isTrackStarted = false;
        }
    }
}
