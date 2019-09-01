package com.android.systemui.screenshot;

import android.app.Notification;
import android.app.NotificationCompat;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import com.android.systemui.R;
import com.android.systemui.util.NotificationChannels;

public class TakeScreenshotService extends Service {
    /* access modifiers changed from: private */
    public static int sRunningCount;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    StatHelper.recordCountEvent(TakeScreenshotService.this.getApplicationContext(), "all");
                    GlobalScreenshot.beforeTakeScreenshot(TakeScreenshotService.this);
                    Message newMsg = Message.obtain(msg);
                    newMsg.what = 2;
                    sendMessageDelayed(newMsg, 150);
                    return;
                case 2:
                    TakeScreenshotService.access$008();
                    GlobalScreenshot screenshot = new GlobalScreenshot(TakeScreenshotService.this);
                    final Messenger callback = msg.replyTo;
                    Runnable animationFinisher = new Runnable() {
                        public void run() {
                            try {
                                callback.send(Message.obtain(null, 1));
                            } catch (RemoteException e) {
                            }
                        }
                    };
                    Runnable totalFinisher = new Runnable() {
                        public void run() {
                            TakeScreenshotService.access$010();
                            if (TakeScreenshotService.sRunningCount <= 0) {
                                TakeScreenshotService.this.stopSelf();
                            }
                        }
                    };
                    boolean z = true;
                    boolean z2 = (msg.arg1 & 1) > 0;
                    if (msg.arg2 <= 0) {
                        z = false;
                    }
                    screenshot.takeScreenshot(animationFinisher, totalFinisher, z2, z);
                    return;
                default:
                    return;
            }
        }
    };

    static /* synthetic */ int access$008() {
        int i = sRunningCount;
        sRunningCount = i + 1;
        return i;
    }

    static /* synthetic */ int access$010() {
        int i = sRunningCount;
        sRunningCount = i - 1;
        return i;
    }

    public IBinder onBind(Intent intent) {
        startService(new Intent(this, getClass()));
        getSystemService("notification");
        Notification.Builder builder = new Notification.Builder(this).setSmallIcon(R.drawable.fold_tips);
        NotificationCompat.setChannelId(builder, NotificationChannels.SCREENSHOTS);
        startForeground(1, builder.build());
        return new Messenger(this.mHandler).getBinder();
    }
}
