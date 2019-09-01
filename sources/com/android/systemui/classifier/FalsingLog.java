package com.android.systemui.classifier;

import android.app.ActivityThread;
import android.app.Application;
import android.os.Build;
import android.os.SystemProperties;
import android.util.Log;
import com.miui.systemui.support.v4.content.ContextCompat;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

public class FalsingLog {
    public static final boolean ENABLED = SystemProperties.getBoolean("debug.falsing_log", Build.IS_DEBUGGABLE);
    private static final boolean LOGCAT = SystemProperties.getBoolean("debug.falsing_logcat", false);
    private static final int MAX_SIZE = SystemProperties.getInt("debug.falsing_log_size", 100);
    private static FalsingLog sInstance;
    private final SimpleDateFormat mFormat = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.US);
    private final ArrayDeque<String> mLog = new ArrayDeque<>(MAX_SIZE);

    private FalsingLog() {
    }

    public static void i(String tag, String s) {
        if (LOGCAT) {
            Log.i("FalsingLog", tag + "\t" + s);
        }
        log("I", tag, s);
    }

    public static void wLogcat(String tag, String s) {
        Log.w("FalsingLog", tag + "\t" + s);
        log("W", tag, s);
    }

    public static void e(String tag, String s) {
        if (LOGCAT) {
            Log.e("FalsingLog", tag + "\t" + s);
        }
        log("E", tag, s);
    }

    public static synchronized void log(String level, String tag, String s) {
        synchronized (FalsingLog.class) {
            if (ENABLED) {
                if (sInstance == null) {
                    sInstance = new FalsingLog();
                }
                if (sInstance.mLog.size() >= MAX_SIZE) {
                    sInstance.mLog.removeFirst();
                }
                sInstance.mLog.add(sInstance.mFormat.format(new Date()) + " " + level + " " + tag + " " + s);
            }
        }
    }

    public static synchronized void dump(PrintWriter pw) {
        synchronized (FalsingLog.class) {
            pw.println("FALSING LOG:");
            if (!ENABLED) {
                pw.println("Disabled, to enable: setprop debug.falsing_log 1");
                pw.println();
                return;
            }
            if (sInstance != null) {
                if (!sInstance.mLog.isEmpty()) {
                    Iterator<String> it = sInstance.mLog.iterator();
                    while (it.hasNext()) {
                        pw.println(it.next());
                    }
                    pw.println();
                    return;
                }
            }
            pw.println("<empty>");
            pw.println();
        }
    }

    public static synchronized void wtf(String tag, String s, Throwable here) {
        synchronized (FalsingLog.class) {
            if (ENABLED) {
                e(tag, s);
                Application application = ActivityThread.currentApplication();
                String fileMessage = "";
                if (!Build.IS_DEBUGGABLE || application == null) {
                    Log.e("FalsingLog", "Unable to write log, build must be debuggable.");
                } else {
                    PrintWriter pw = null;
                    try {
                        pw = new PrintWriter(new File(ContextCompat.getDataDir(application), "falsing-" + new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date()) + ".txt"));
                        dump(pw);
                        pw.close();
                        fileMessage = "Log written to " + f.getAbsolutePath();
                    } catch (IOException e) {
                        try {
                            Log.e("FalsingLog", "Unable to write falsing log", e);
                        } finally {
                            if (pw != null) {
                                pw.close();
                            }
                        }
                    }
                }
                Log.wtf("FalsingLog", tag + " " + s + "; " + fileMessage, here);
            }
        }
    }
}
