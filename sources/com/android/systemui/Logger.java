package com.android.systemui;

import android.os.AsyncTask;
import miui.os.Build;
import miui.util.Log;

public final class Logger {
    private static final boolean IS_STABLE_VERSION = Build.IS_STABLE_VERSION;

    public static void i(String tag, String logMsg) {
        Log.i(tag, logMsg);
    }

    public static void w(String tag, String logMsg) {
        Log.w(tag, logMsg);
    }

    public static void e(String tag, String logMsg) {
        Log.e(tag, logMsg);
    }

    public static void fileI(final String tag, final String logMsg) {
        if (!IS_STABLE_VERSION) {
            AsyncTask.execute(new Runnable() {
                public void run() {
                    Log.getFileLogger().info(tag, logMsg);
                }
            });
        }
    }

    public static void fileW(final String tag, final String logMsg) {
        if (!IS_STABLE_VERSION) {
            AsyncTask.execute(new Runnable() {
                public void run() {
                    Log.getFileLogger().warn(tag, logMsg);
                }
            });
        }
    }

    public static void fileE(final String tag, final String logMsg) {
        if (!IS_STABLE_VERSION) {
            AsyncTask.execute(new Runnable() {
                public void run() {
                    Log.getFileLogger().error(tag, logMsg);
                }
            });
        }
    }

    public static void fullI(String tag, String logMsg) {
        i(tag, logMsg);
        fileI(tag, logMsg);
    }

    public static void fullW(String tag, String logMsg) {
        w(tag, logMsg);
        fileW(tag, logMsg);
    }

    public static void fullE(String tag, String logMsg) {
        e(tag, logMsg);
        fileE(tag, logMsg);
    }
}
