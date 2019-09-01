package com.android.systemui.qs.external;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.UserHandle;
import android.service.quicksettings.IQSTileService;
import android.util.Log;
import com.android.systemui.qs.external.TileLifecycleManager;
import java.util.Objects;

public class TileServiceManager {
    static final String PREFS_FILE = "CustomTileModes";
    private boolean mBindAllowed;
    /* access modifiers changed from: private */
    public boolean mBindRequested;
    /* access modifiers changed from: private */
    public boolean mBound;
    private final Handler mHandler;
    /* access modifiers changed from: private */
    public boolean mJustBound;
    final Runnable mJustBoundOver;
    private long mLastUpdate;
    private Object mLock;
    private boolean mPendingBind;
    private int mPriority;
    /* access modifiers changed from: private */
    public final TileServices mServices;
    private boolean mShowingDialog;
    /* access modifiers changed from: private */
    public final TileLifecycleManager mStateManager;
    private final Runnable mUnbind;
    private final BroadcastReceiver mUninstallReceiver;

    /* JADX WARNING: Illegal instructions before constructor call */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    TileServiceManager(com.android.systemui.qs.external.TileServices r9, android.os.Handler r10, android.content.ComponentName r11, android.service.quicksettings.Tile r12) {
        /*
            r8 = this;
            com.android.systemui.qs.external.TileLifecycleManager r7 = new com.android.systemui.qs.external.TileLifecycleManager
            android.content.Context r2 = r9.getContext()
            android.content.Intent r0 = new android.content.Intent
            r0.<init>()
            android.content.Intent r5 = r0.setComponent(r11)
            android.os.UserHandle r6 = new android.os.UserHandle
            int r0 = android.app.ActivityManager.getCurrentUser()
            r6.<init>(r0)
            r0 = r7
            r1 = r10
            r3 = r9
            r4 = r12
            r0.<init>(r1, r2, r3, r4, r5, r6)
            r8.<init>(r9, r10, r7)
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.qs.external.TileServiceManager.<init>(com.android.systemui.qs.external.TileServices, android.os.Handler, android.content.ComponentName, android.service.quicksettings.Tile):void");
    }

    TileServiceManager(TileServices tileServices, Handler handler, TileLifecycleManager tileLifecycleManager) {
        this.mPendingBind = true;
        this.mLock = new Object();
        this.mUnbind = new Runnable() {
            public void run() {
                if (TileServiceManager.this.mBound && !TileServiceManager.this.mBindRequested) {
                    TileServiceManager.this.unbindService();
                }
            }
        };
        this.mJustBoundOver = new Runnable() {
            public void run() {
                boolean unused = TileServiceManager.this.mJustBound = false;
                TileServiceManager.this.mServices.recalculateBindAllowance();
            }
        };
        this.mUninstallReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if ("android.intent.action.PACKAGE_REMOVED".equals(intent.getAction())) {
                    String pkgName = intent.getData().getEncodedSchemeSpecificPart();
                    ComponentName component = TileServiceManager.this.mStateManager.getComponent();
                    if (Objects.equals(pkgName, component.getPackageName())) {
                        if (intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                            Intent queryIntent = new Intent("android.service.quicksettings.action.QS_TILE");
                            queryIntent.setPackage(pkgName);
                            for (ResolveInfo info : context.getPackageManager().queryIntentServicesAsUser(queryIntent, 0, ActivityManager.getCurrentUser())) {
                                if (Objects.equals(info.serviceInfo.packageName, component.getPackageName()) && Objects.equals(info.serviceInfo.name, component.getClassName())) {
                                    return;
                                }
                            }
                        }
                        TileServiceManager.this.mServices.getHost().removeTile(component);
                    }
                }
            }
        };
        this.mServices = tileServices;
        this.mHandler = handler;
        this.mStateManager = tileLifecycleManager;
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.PACKAGE_REMOVED");
        filter.addDataScheme("package");
        Context context = this.mServices.getContext();
        context.registerReceiverAsUser(this.mUninstallReceiver, new UserHandle(ActivityManager.getCurrentUser()), filter, null, this.mHandler);
        ComponentName component = tileLifecycleManager.getComponent();
        if (!TileLifecycleManager.isTileAdded(context, component)) {
            TileLifecycleManager.setTileAdded(context, component, true);
            this.mStateManager.onTileAdded();
            this.mStateManager.flushMessagesAndUnbind();
        }
    }

    public void setTileChangeListener(TileLifecycleManager.TileChangeListener changeListener) {
        this.mStateManager.setTileChangeListener(changeListener);
    }

    public boolean isActiveTile() {
        return this.mStateManager.isActiveTile();
    }

    public void setShowingDialog(boolean dialog) {
        this.mShowingDialog = dialog;
    }

    public IQSTileService getTileService() {
        return this.mStateManager;
    }

    public IBinder getToken() {
        return this.mStateManager.getToken();
    }

    public void setBindRequested(boolean bindRequested) {
        if (this.mBindRequested != bindRequested) {
            this.mBindRequested = bindRequested;
            if (!this.mBindAllowed || !this.mBindRequested || this.mBound) {
                this.mServices.recalculateBindAllowance();
            } else {
                this.mHandler.removeCallbacks(this.mUnbind);
                bindService();
            }
            if (this.mBound && !this.mBindRequested) {
                this.mHandler.postDelayed(this.mUnbind, 30000);
            }
        }
    }

    public void setLastUpdate(long lastUpdate) {
        this.mLastUpdate = lastUpdate;
        if (this.mBound && isActiveTile()) {
            this.mStateManager.onStopListening();
            setBindRequested(false);
        }
        this.mServices.recalculateBindAllowance();
    }

    public void handleDestroy() {
        setBindAllowed(false);
        this.mServices.getContext().unregisterReceiver(this.mUninstallReceiver);
        this.mStateManager.handleDestroy();
    }

    public void setBindAllowed(boolean allowed) {
        if (this.mBindAllowed != allowed) {
            this.mBindAllowed = allowed;
            if (!this.mBindAllowed && this.mBound) {
                unbindService();
            } else if (this.mBindAllowed && this.mBindRequested && !this.mBound) {
                bindService();
            }
        }
    }

    public boolean hasPendingBind() {
        return this.mPendingBind;
    }

    public void clearPendingBind() {
        this.mPendingBind = false;
    }

    private void bindService() {
        synchronized (this.mLock) {
            if (this.mBound) {
                Log.e("TileServiceManager", "Service already bound");
                return;
            }
            this.mPendingBind = true;
            this.mBound = true;
            this.mJustBound = true;
            this.mHandler.postDelayed(this.mJustBoundOver, 5000);
            this.mStateManager.setBindService(true);
        }
    }

    /* access modifiers changed from: private */
    public void unbindService() {
        synchronized (this.mLock) {
            if (!this.mBound) {
                Log.e("TileServiceManager", "Service not bound");
                return;
            }
            this.mBound = false;
            this.mJustBound = false;
            this.mStateManager.setBindService(false);
        }
    }

    public void calculateBindPriority(long currentTime) {
        if (this.mStateManager.hasPendingClick()) {
            this.mPriority = Integer.MAX_VALUE;
        } else if (this.mShowingDialog) {
            this.mPriority = 2147483646;
        } else if (this.mJustBound) {
            this.mPriority = 2147483645;
        } else if (!this.mBindRequested) {
            this.mPriority = Integer.MIN_VALUE;
        } else {
            long timeSinceUpdate = currentTime - this.mLastUpdate;
            if (timeSinceUpdate > 2147483644) {
                this.mPriority = 2147483644;
            } else {
                this.mPriority = (int) timeSinceUpdate;
            }
        }
    }

    public int getBindPriority() {
        return this.mPriority;
    }
}
