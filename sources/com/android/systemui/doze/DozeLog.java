package com.android.systemui.doze;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.util.TimeUtils;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DozeLog {
    private static final boolean DEBUG = Log.isLoggable("DozeLog", 3);
    static final SimpleDateFormat FORMAT = new SimpleDateFormat("MM-dd HH:mm:ss.SSS");
    private static final int SIZE = (Build.IS_DEBUGGABLE ? 400 : 50);
    private static int sCount;
    private static SummaryStats sEmergencyCallStats;
    private static final KeyguardUpdateMonitorCallback sKeyguardCallback = new KeyguardUpdateMonitorCallback() {
        public void onEmergencyCallAction() {
            DozeLog.traceEmergencyCall();
        }

        public void onKeyguardBouncerChanged(boolean bouncer) {
            DozeLog.traceKeyguardBouncerChanged(bouncer);
        }

        public void onStartedWakingUp() {
            DozeLog.traceScreenOn();
        }

        public void onFinishedGoingToSleep(int why) {
            DozeLog.traceScreenOff(why);
        }

        public void onKeyguardVisibilityChanged(boolean showing) {
            DozeLog.traceKeyguard(showing);
        }
    };
    private static String[] sMessages;
    private static SummaryStats sNotificationPulseStats;
    private static SummaryStats sPickupPulseNearVibrationStats;
    private static SummaryStats sPickupPulseNotNearVibrationStats;
    private static int sPosition;
    private static SummaryStats[][] sProxStats;
    private static boolean sPulsing;
    private static boolean sRegisterKeyguardCallback = true;
    private static SummaryStats sScreenOnNotPulsingStats;
    private static SummaryStats sScreenOnPulsingStats;
    /* access modifiers changed from: private */
    public static long sSince;
    private static long[] sTimes;

    private static class SummaryStats {
        private int mCount;

        private SummaryStats() {
        }

        public void append() {
            this.mCount++;
        }

        public void dump(PrintWriter pw, String type) {
            if (this.mCount != 0) {
                pw.print("    ");
                pw.print(type);
                pw.print(": n=");
                pw.print(this.mCount);
                pw.print(" (");
                pw.print((((double) this.mCount) / ((double) (System.currentTimeMillis() - DozeLog.sSince))) * 1000.0d * 60.0d * 60.0d);
                pw.print("/hr)");
                pw.println();
            }
        }
    }

    public static void tracePulseStart(int reason) {
        sPulsing = true;
        log("pulseStart reason=" + pulseReasonToString(reason));
    }

    public static void tracePulseFinish() {
        sPulsing = false;
        log("pulseFinish");
    }

    private static void init(Context context) {
        synchronized (DozeLog.class) {
            if (sMessages == null) {
                sTimes = new long[SIZE];
                sMessages = new String[SIZE];
                sSince = System.currentTimeMillis();
                sPickupPulseNearVibrationStats = new SummaryStats();
                sPickupPulseNotNearVibrationStats = new SummaryStats();
                sNotificationPulseStats = new SummaryStats();
                sScreenOnPulsingStats = new SummaryStats();
                sScreenOnNotPulsingStats = new SummaryStats();
                sEmergencyCallStats = new SummaryStats();
                sProxStats = (SummaryStats[][]) Array.newInstance(SummaryStats.class, new int[]{5, 2});
                for (int i = 0; i < 5; i++) {
                    sProxStats[i][0] = new SummaryStats();
                    sProxStats[i][1] = new SummaryStats();
                }
                log("init");
                if (sRegisterKeyguardCallback) {
                    KeyguardUpdateMonitor.getInstance(context).registerCallback(sKeyguardCallback);
                }
            }
        }
    }

    public static void traceDozing(Context context, boolean dozing) {
        sPulsing = false;
        init(context);
        log("dozing " + dozing);
    }

    public static void traceFling(boolean expand, boolean aboveThreshold, boolean thresholdNeeded, boolean screenOnFromTouch) {
        log("fling expand=" + expand + " aboveThreshold=" + aboveThreshold + " thresholdNeeded=" + thresholdNeeded + " screenOnFromTouch=" + screenOnFromTouch);
    }

    public static void traceEmergencyCall() {
        log("emergencyCall");
        sEmergencyCallStats.append();
    }

    public static void traceKeyguardBouncerChanged(boolean showing) {
        log("bouncer " + showing);
    }

    public static void traceScreenOn() {
        log("screenOn pulsing=" + sPulsing);
        (sPulsing ? sScreenOnPulsingStats : sScreenOnNotPulsingStats).append();
        sPulsing = false;
    }

    public static void traceScreenOff(int why) {
        log("screenOff why=" + why);
    }

    public static void traceKeyguard(boolean showing) {
        log("keyguard " + showing);
        if (!showing) {
            sPulsing = false;
        }
    }

    public static String pulseReasonToString(int pulseReason) {
        switch (pulseReason) {
            case 0:
                return "intent";
            case 1:
                return "notification";
            case 2:
                return "sigmotion";
            case 3:
                return "pickup";
            case 4:
                return "doubletap";
            default:
                throw new IllegalArgumentException("bad reason: " + pulseReason);
        }
    }

    public static void dump(PrintWriter pw) {
        synchronized (DozeLog.class) {
            if (sMessages != null) {
                pw.println("  Doze log:");
                int start = ((sPosition - sCount) + SIZE) % SIZE;
                for (int i = 0; i < sCount; i++) {
                    int j = (start + i) % SIZE;
                    pw.print("    ");
                    pw.print(FORMAT.format(new Date(sTimes[j])));
                    pw.print(' ');
                    pw.println(sMessages[j]);
                }
                pw.print("  Doze summary stats (for ");
                TimeUtils.formatDuration(System.currentTimeMillis() - sSince, pw);
                pw.println("):");
                sPickupPulseNearVibrationStats.dump(pw, "Pickup pulse (near vibration)");
                sPickupPulseNotNearVibrationStats.dump(pw, "Pickup pulse (not near vibration)");
                sNotificationPulseStats.dump(pw, "Notification pulse");
                sScreenOnPulsingStats.dump(pw, "Screen on (pulsing)");
                sScreenOnNotPulsingStats.dump(pw, "Screen on (not pulsing)");
                sEmergencyCallStats.dump(pw, "Emergency call");
                for (int i2 = 0; i2 < 5; i2++) {
                    String reason = pulseReasonToString(i2);
                    sProxStats[i2][0].dump(pw, "Proximity near (" + reason + ")");
                    sProxStats[i2][1].dump(pw, "Proximity far (" + reason + ")");
                }
            }
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:10:0x0031, code lost:
        if (DEBUG == false) goto L_0x0038;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:11:0x0033, code lost:
        android.util.Log.d("DozeLog", r5);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:12:0x0038, code lost:
        return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static void log(java.lang.String r5) {
        /*
            java.lang.Class<com.android.systemui.doze.DozeLog> r0 = com.android.systemui.doze.DozeLog.class
            monitor-enter(r0)
            java.lang.String[] r1 = sMessages     // Catch:{ all -> 0x0039 }
            if (r1 != 0) goto L_0x0009
            monitor-exit(r0)     // Catch:{ all -> 0x0039 }
            return
        L_0x0009:
            long[] r1 = sTimes     // Catch:{ all -> 0x0039 }
            int r2 = sPosition     // Catch:{ all -> 0x0039 }
            long r3 = java.lang.System.currentTimeMillis()     // Catch:{ all -> 0x0039 }
            r1[r2] = r3     // Catch:{ all -> 0x0039 }
            java.lang.String[] r1 = sMessages     // Catch:{ all -> 0x0039 }
            int r2 = sPosition     // Catch:{ all -> 0x0039 }
            r1[r2] = r5     // Catch:{ all -> 0x0039 }
            int r1 = sPosition     // Catch:{ all -> 0x0039 }
            int r1 = r1 + 1
            int r2 = SIZE     // Catch:{ all -> 0x0039 }
            int r1 = r1 % r2
            sPosition = r1     // Catch:{ all -> 0x0039 }
            int r1 = sCount     // Catch:{ all -> 0x0039 }
            int r1 = r1 + 1
            int r2 = SIZE     // Catch:{ all -> 0x0039 }
            int r1 = java.lang.Math.min(r1, r2)     // Catch:{ all -> 0x0039 }
            sCount = r1     // Catch:{ all -> 0x0039 }
            monitor-exit(r0)     // Catch:{ all -> 0x0039 }
            boolean r0 = DEBUG
            if (r0 == 0) goto L_0x0038
            java.lang.String r0 = "DozeLog"
            android.util.Log.d(r0, r5)
        L_0x0038:
            return
        L_0x0039:
            r1 = move-exception
            monitor-exit(r0)     // Catch:{ all -> 0x0039 }
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.doze.DozeLog.log(java.lang.String):void");
    }
}
