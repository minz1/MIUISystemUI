package com.android.systemui.util.leak;

import android.app.NotificationChannelCompat;
import android.app.NotificationCompat;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Debug;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.support.v4.content.FileProvider;
import android.util.Log;
import com.google.android.collect.Lists;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

public class LeakReporter {
    private final Context mContext;
    private final LeakDetector mLeakDetector;
    private final String mLeakReportEmail;

    public LeakReporter(Context context, LeakDetector leakDetector, String leakReportEmail) {
        this.mContext = context;
        this.mLeakDetector = leakDetector;
        this.mLeakReportEmail = leakReportEmail;
    }

    public void dumpLeak(int garbageCount) {
        FileOutputStream fos;
        Throwable th;
        Throwable th2;
        Throwable th3;
        try {
            File leakDir = new File(this.mContext.getCacheDir(), "leak");
            leakDir.mkdir();
            File hprofFile = new File(leakDir, "leak.hprof");
            Debug.dumpHprofData(hprofFile.getAbsolutePath());
            File dumpFile = new File(leakDir, "leak.dump");
            fos = new FileOutputStream(dumpFile);
            PrintWriter w = new PrintWriter(fos);
            w.print("Build: ");
            w.println(SystemProperties.get("ro.build.description"));
            w.println();
            w.flush();
            this.mLeakDetector.dump(fos.getFD(), w, new String[0]);
            w.close();
            fos.close();
            NotificationManager notiMan = (NotificationManager) this.mContext.getSystemService(NotificationManager.class);
            NotificationChannelCompat channel = new NotificationChannelCompat("leak", "Leak Alerts", 4);
            channel.enableVibration(true);
            NotificationChannelCompat.createNotificationChannel(notiMan, channel);
            notiMan.notify("LeakReporter", 0, NotificationCompat.newBuilder(this.mContext, "leak").setAutoCancel(true).setShowWhen(true).setContentTitle("Memory Leak Detected").setContentText(String.format("SystemUI has detected %d leaked objects. Tap to send", new Object[]{Integer.valueOf(garbageCount)})).setSmallIcon(17303485).setContentIntent(PendingIntent.getActivityAsUser(this.mContext, 0, getIntent(hprofFile, dumpFile), 134217728, null, UserHandle.CURRENT)).build());
            return;
        } catch (IOException e) {
            Log.e("LeakReporter", "Couldn't dump heap for leak", e);
            return;
        } catch (Throwable th4) {
            th2.addSuppressed(th4);
        }
        throw th3;
    }

    private Intent getIntent(File hprofFile, File dumpFile) {
        Uri dumpUri = FileProvider.getUriForFile(this.mContext, "com.android.systemui.fileprovider", dumpFile);
        Uri hprofUri = FileProvider.getUriForFile(this.mContext, "com.android.systemui.fileprovider", hprofFile);
        Intent intent = new Intent("android.intent.action.SEND_MULTIPLE");
        intent.addFlags(1);
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setType("application/vnd.android.leakreport");
        intent.putExtra("android.intent.extra.SUBJECT", "SystemUI leak report");
        intent.putExtra("android.intent.extra.TEXT", "Build info: " + SystemProperties.get("ro.build.description"));
        ClipData clipData = new ClipData(null, new String[]{"application/vnd.android.leakreport"}, new ClipData.Item(null, null, null, dumpUri));
        ArrayList<Uri> attachments = Lists.newArrayList(new Uri[]{dumpUri});
        clipData.addItem(new ClipData.Item(null, null, null, hprofUri));
        attachments.add(hprofUri);
        intent.setClipData(clipData);
        intent.putParcelableArrayListExtra("android.intent.extra.STREAM", attachments);
        String leakReportEmail = this.mLeakReportEmail;
        if (leakReportEmail != null) {
            intent.putExtra("android.intent.extra.EMAIL", new String[]{leakReportEmail});
        }
        return intent;
    }
}
