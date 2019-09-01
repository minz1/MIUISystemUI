package com.android.systemui.keyguard;

import android.app.ActivityManager;
import android.app.ActivityManagerCompat;
import android.app.ActivityOptions;
import android.app.IActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.recents.misc.SystemServicesProxy;

public class WorkLockActivityController {
    private final Context mContext;
    private final IActivityManager mIam;
    private final SystemServicesProxy.TaskStackListener mLockListener;
    private final SystemServicesProxy mSsp;

    public WorkLockActivityController(Context context) {
        this(context, SystemServicesProxy.getInstance(context), ActivityManagerCompat.getService());
    }

    @VisibleForTesting
    WorkLockActivityController(Context context, SystemServicesProxy ssp, IActivityManager am) {
        this.mLockListener = new SystemServicesProxy.TaskStackListener() {
            public void onTaskProfileLocked(int taskId, int userId) {
                WorkLockActivityController.this.startWorkChallengeInTask(taskId, userId);
            }
        };
        this.mContext = context;
        this.mSsp = ssp;
        this.mIam = am;
        this.mSsp.registerTaskStackListener(this.mLockListener);
    }

    /* access modifiers changed from: private */
    public void startWorkChallengeInTask(int taskId, int userId) {
        Intent intent = new Intent("android.app.action.CONFIRM_DEVICE_CREDENTIAL_WITH_USER").setComponent(new ComponentName(this.mContext, WorkLockActivity.class)).putExtra("android.intent.extra.USER_ID", userId).addFlags(67239936);
        ActivityOptions options = ActivityOptions.makeBasic();
        options.setLaunchTaskId(taskId);
        options.setTaskOverlay(true, false);
        if (!ActivityManager.isStartResultSuccessful(startActivityAsUser(intent, options.toBundle(), -2))) {
            this.mSsp.removeTask(taskId, true);
        }
    }

    private int startActivityAsUser(Intent intent, Bundle options, int userId) {
        try {
            Intent intent2 = intent;
            try {
                return this.mIam.startActivityAsUser(this.mContext.getIApplicationThread(), this.mContext.getBasePackageName(), intent2, intent2.resolveTypeIfNeeded(this.mContext.getContentResolver()), null, null, 0, 268435456, null, options, userId);
            } catch (RemoteException e) {
                return -96;
            } catch (Exception e2) {
                return -96;
            }
        } catch (RemoteException e3) {
            Intent intent3 = intent;
            return -96;
        } catch (Exception e4) {
            Intent intent4 = intent;
            return -96;
        }
    }
}
