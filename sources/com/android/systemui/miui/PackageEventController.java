package com.android.systemui.miui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import com.android.systemui.Constants;

public class PackageEventController extends BroadcastReceiver {
    private Context mContext;
    /* access modifiers changed from: private */
    public PackageEventReceiver mPackageChangedReceiver;
    private Handler mScheduler;

    public PackageEventController(Context context, PackageEventReceiver receiver, Handler scheduler) {
        this.mContext = context;
        this.mScheduler = scheduler;
        this.mPackageChangedReceiver = receiver;
        if (this.mScheduler == null) {
            this.mScheduler = new Handler(Looper.getMainLooper());
        }
    }

    public void start() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.PACKAGE_ADDED");
        filter.addAction("android.intent.action.PACKAGE_CHANGED");
        filter.addAction("android.intent.action.PACKAGE_REMOVED");
        filter.addDataScheme("package");
        this.mContext.registerReceiverAsUser(this, UserHandle.ALL, filter, null, null);
    }

    public void onReceive(Context context, final Intent intent) {
        final String packageName = getPackageName(intent);
        final int uid = intent.getIntExtra("android.intent.extra.UID", -1);
        if (!TextUtils.isEmpty(packageName) && uid >= 0 && !TextUtils.isEmpty(intent.getAction())) {
            if ("android.intent.action.PACKAGE_CHANGED".equals(intent.getAction())) {
                this.mScheduler.post(new Runnable() {
                    public void run() {
                        PackageEventController.this.mPackageChangedReceiver.onPackageChanged(uid, packageName);
                    }
                });
            } else if ("android.intent.action.PACKAGE_ADDED".equals(intent.getAction())) {
                this.mScheduler.post(new Runnable() {
                    public void run() {
                        PackageEventController.this.mPackageChangedReceiver.onPackageAdded(uid, packageName, intent.getBooleanExtra("android.intent.extra.REPLACING", false));
                    }
                });
            } else if ("android.intent.action.PACKAGE_REMOVED".equals(intent.getAction())) {
                this.mScheduler.post(new Runnable() {
                    public void run() {
                        PackageEventController.this.mPackageChangedReceiver.onPackageRemoved(uid, packageName, intent.getBooleanExtra("android.intent.extra.DATA_REMOVED", false), intent.getBooleanExtra("android.intent.extra.REPLACING", false));
                    }
                });
            }
            if (Constants.DEBUG) {
                Log.i("PackageEventController", "broadcast received: " + intent.getAction() + " " + packageName);
            }
        }
    }

    private static String getPackageName(Intent intent) {
        Uri uri = intent.getData();
        if (uri != null) {
            return uri.getSchemeSpecificPart();
        }
        return null;
    }
}
