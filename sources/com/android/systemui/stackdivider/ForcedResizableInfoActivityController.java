package com.android.systemui.stackdivider;

import android.app.ActivityManager;
import android.app.ActivityManagerCompat;
import android.app.ActivityOptions;
import android.app.ActivityOptionsCompat;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.ArraySet;
import android.util.Log;
import android.widget.Toast;
import com.android.internal.os.BackgroundThread;
import com.android.systemui.R;
import com.android.systemui.recents.events.RecentsEventBus;
import com.android.systemui.recents.events.activity.AppTransitionFinishedEvent;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.stackdivider.events.StartedDragingEvent;
import com.android.systemui.stackdivider.events.StoppedDragingEvent;

public class ForcedResizableInfoActivityController {
    /* access modifiers changed from: private */
    public final Context mContext;
    private boolean mDividerDraging;
    /* access modifiers changed from: private */
    public String mFullscreenTopPackageName;
    /* access modifiers changed from: private */
    public final Handler mHandler = new Handler();
    private final ArraySet<String> mPackagesShownInSession = new ArraySet<>();
    private final ArraySet<Integer> mPendingTaskIds = new ArraySet<>();
    private final Runnable mTimeoutRunnable = new Runnable() {
        public void run() {
            ForcedResizableInfoActivityController.this.showPending();
        }
    };

    public ForcedResizableInfoActivityController(Context context) {
        this.mContext = context;
        RecentsEventBus.getDefault().register(this);
        SystemServicesProxy.getInstance(context).registerTaskStackListener(new SystemServicesProxy.TaskStackListener() {
            public void onActivityForcedResizable(String packageName, int taskId, int reason) {
                ForcedResizableInfoActivityController.this.activityForcedResizable(packageName, taskId);
            }

            public void onActivityDismissingDockedStack() {
                ForcedResizableInfoActivityController.this.activityDismissingDockedStack();
            }
        });
    }

    public void notifyDockedStackExistsChanged(boolean exists) {
        if (!exists) {
            this.mPackagesShownInSession.clear();
        }
    }

    public final void onBusEvent(AppTransitionFinishedEvent event) {
        if (!this.mDividerDraging) {
            showPending();
        }
    }

    public final void onBusEvent(StartedDragingEvent event) {
        this.mDividerDraging = true;
        this.mHandler.removeCallbacks(this.mTimeoutRunnable);
    }

    public final void onBusEvent(StoppedDragingEvent event) {
        this.mDividerDraging = false;
        showPending();
    }

    /* access modifiers changed from: private */
    public void activityForcedResizable(String packageName, int taskId) {
        if (!debounce(packageName)) {
            this.mPendingTaskIds.add(Integer.valueOf(taskId));
            postTimeout();
        }
    }

    /* access modifiers changed from: private */
    public void activityDismissingDockedStack() {
        BackgroundThread.getHandler().post(new Runnable() {
            public void run() {
                if (((KeyguardManager) ForcedResizableInfoActivityController.this.mContext.getSystemService("keyguard")).isKeyguardLocked()) {
                    ForcedResizableInfoActivityController.this.mHandler.post(new Runnable() {
                        public void run() {
                            ForcedResizableInfoActivityController.this.showToast(R.string.dock_keyguard_locked_failed_to_dock_text);
                        }
                    });
                    return;
                }
                try {
                    ActivityManager.StackInfo fullscreenStackinfo = ActivityManagerCompat.getStackInfo(1, 1, 0);
                    if (!(fullscreenStackinfo == null || fullscreenStackinfo.topActivity == null)) {
                        String curFullscreenTopPackageName = fullscreenStackinfo.topActivity.getPackageName();
                        if (curFullscreenTopPackageName != null && !curFullscreenTopPackageName.equals(ForcedResizableInfoActivityController.this.mFullscreenTopPackageName)) {
                            String unused = ForcedResizableInfoActivityController.this.mFullscreenTopPackageName = curFullscreenTopPackageName;
                            ForcedResizableInfoActivityController.this.mHandler.post(new Runnable() {
                                public void run() {
                                    ForcedResizableInfoActivityController.this.showToast(R.string.dock_non_resizeble_failed_to_dock_text);
                                }
                            });
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /* access modifiers changed from: private */
    public void showToast(int resId) {
        Toast.makeText(this.mContext, resId, 0).show();
    }

    /* access modifiers changed from: private */
    public void showPending() {
        this.mHandler.removeCallbacks(this.mTimeoutRunnable);
        for (int i = this.mPendingTaskIds.size() - 1; i >= 0; i--) {
            Intent intent = new Intent(this.mContext, ForcedResizableInfoActivity.class);
            intent.addFlags(268435456);
            ActivityOptions options = ActivityOptions.makeBasic();
            if (Utilities.isAndroidNorNewer()) {
                ActivityOptionsCompat.setLaunchTaskId(options, this.mPendingTaskIds.valueAt(i).intValue());
                ActivityOptionsCompat.setTaskOverlay(options, true, true);
            } else if (!Utilities.isAndroidNorNewer()) {
                ActivityOptionsCompat.setLaunchStackId(options, this.mPendingTaskIds.valueAt(i).intValue(), -1, -1);
            }
            try {
                this.mContext.startActivity(intent, options.toBundle());
            } catch (Exception e) {
                Log.e("ForcedResizableInfoActivityController", "Start ForcedResizableInfoActivity error.", e);
            }
        }
        this.mPendingTaskIds.clear();
    }

    private void postTimeout() {
        this.mHandler.removeCallbacks(this.mTimeoutRunnable);
        this.mHandler.postDelayed(this.mTimeoutRunnable, 1000);
    }

    private boolean debounce(String packageName) {
        if (packageName == null) {
            return false;
        }
        if ("com.android.systemui".equals(packageName)) {
            return true;
        }
        boolean debounce = this.mPackagesShownInSession.contains(packageName);
        this.mPackagesShownInSession.add(packageName);
        return debounce;
    }
}
