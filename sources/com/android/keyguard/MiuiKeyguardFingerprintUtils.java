package com.android.keyguard;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import com.android.keyguard.analytics.AnalyticsHelper;
import java.io.RandomAccessFile;
import miui.os.Build;

public class MiuiKeyguardFingerprintUtils {
    private static Handler sHandler;

    public enum FingerprintIdentificationState {
        FAILED,
        SUCCEEDED,
        ERROR,
        RESET
    }

    private static void initHandler() {
        if (sHandler == null) {
            HandlerThread handlerThread = new HandlerThread("MiuiKeyguardFingerprintUtils");
            handlerThread.start();
            sHandler = new Handler(handlerThread.getLooper());
        }
    }

    public static void processFingerprintResultAnalytics(final int result) {
        initHandler();
        sHandler.post(new Runnable() {
            public void run() {
                if ("capricorn".equals(Build.DEVICE) || "aqua".equals(Build.DEVICE)) {
                    MiuiKeyguardFingerprintUtils.processFingerprintResultAnalyticsForA7(result);
                } else if ("scorpio".equals(Build.DEVICE)) {
                    MiuiKeyguardFingerprintUtils.processFingerprintResultAnalyticsForA4(result);
                } else {
                    AnalyticsHelper.recordCalculateEvent("keyguard_fp_identify_result_" + Build.DEVICE, (long) result);
                }
            }
        });
    }

    public static void processFingerprintResultAnalyticsForA4(int result) {
        RandomAccessFile file = null;
        try {
            RandomAccessFile file2 = new RandomAccessFile("/sdcard/MIUI/debug_log/1.dat", "r");
            file2.seek(file2.length() - 8);
            int value = file2.readInt();
            String simpleName = KeyguardUpdateMonitor.class.getSimpleName();
            Log.d(simpleName, "value: " + value);
            String v = value == 18087936 ? "yinlan" : "default";
            AnalyticsHelper.recordCalculateEvent("keyguard_fp_identify_result_" + Build.DEVICE + "_" + v, (long) result);
            try {
                file2.close();
            } catch (Exception e) {
                Log.e(KeyguardUpdateMonitor.class.getSimpleName(), e.getMessage(), e);
            }
        } catch (Exception e2) {
            Log.e(KeyguardUpdateMonitor.class.getSimpleName(), e2.getMessage(), e2);
            AnalyticsHelper.recordCalculateEvent("keyguard_fp_identify_result_" + Build.DEVICE, (long) result);
            if (file != null) {
                file.close();
            }
        } catch (Throwable th) {
            if (file != null) {
                try {
                    file.close();
                } catch (Exception e3) {
                    Log.e(KeyguardUpdateMonitor.class.getSimpleName(), e3.getMessage(), e3);
                }
            }
            throw th;
        }
    }

    public static void processFingerprintResultAnalyticsForA7(int result) {
        RandomAccessFile file = null;
        try {
            file = new RandomAccessFile("/sdcard/MIUI/debug_log/1.dat", "r");
            file.seek(file.length() - 4);
            int temperature = file.readByte();
            String simpleName = KeyguardUpdateMonitor.class.getSimpleName();
            Log.d(simpleName, "temperature: " + temperature);
            AnalyticsHelper.recordCalculateEvent("keyguard_fp_identify_result_" + Build.DEVICE + "_" + (temperature / 10), (long) result);
            try {
                file.close();
            } catch (Exception e) {
                Log.e(KeyguardUpdateMonitor.class.getSimpleName(), e.getMessage(), e);
            }
        } catch (Exception e2) {
            Log.e(KeyguardUpdateMonitor.class.getSimpleName(), e2.getMessage(), e2);
            AnalyticsHelper.recordCalculateEvent("keyguard_fp_identify_result_" + Build.DEVICE, (long) result);
            if (file != null) {
                file.close();
            }
        } catch (Throwable th) {
            if (file != null) {
                try {
                    file.close();
                } catch (Exception e3) {
                    Log.e(KeyguardUpdateMonitor.class.getSimpleName(), e3.getMessage(), e3);
                }
            }
            throw th;
        }
    }
}
