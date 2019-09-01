package com.android.systemui.qs.external;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.Icon;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.UserHandle;
import android.service.quicksettings.IQSService;
import android.service.quicksettings.Tile;
import android.util.ArrayMap;
import android.util.Log;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.systemui.Dependency;
import com.android.systemui.qs.QSTileHost;
import com.android.systemui.statusbar.phone.StatusBarIconController;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class TileServices extends IQSService.Stub {
    private static final Comparator<TileServiceManager> SERVICE_SORT = new Comparator<TileServiceManager>() {
        public int compare(TileServiceManager left, TileServiceManager right) {
            return -Integer.compare(left.getBindPriority(), right.getBindPriority());
        }
    };
    private final Context mContext;
    private final Handler mHandler;
    /* access modifiers changed from: private */
    public final QSTileHost mHost;
    private final Handler mMainHandler;
    private int mMaxBound = 3;
    private final BroadcastReceiver mRequestListeningReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if ("android.service.quicksettings.action.REQUEST_LISTENING".equals(intent.getAction())) {
                TileServices.this.requestListening((ComponentName) intent.getParcelableExtra("android.intent.extra.COMPONENT_NAME"));
            }
        }
    };
    private final ArrayMap<CustomTile, TileServiceManager> mServices = new ArrayMap<>();
    private final ArrayMap<ComponentName, CustomTile> mTiles = new ArrayMap<>();
    private final ArrayMap<IBinder, CustomTile> mTokenMap = new ArrayMap<>();

    public TileServices(QSTileHost host, Looper looper) {
        this.mHost = host;
        this.mContext = this.mHost.getContext();
        this.mContext.registerReceiver(this.mRequestListeningReceiver, new IntentFilter("android.service.quicksettings.action.REQUEST_LISTENING"));
        this.mHandler = new Handler(looper);
        this.mMainHandler = new Handler(Looper.getMainLooper());
    }

    public Context getContext() {
        return this.mContext;
    }

    public QSTileHost getHost() {
        return this.mHost;
    }

    public TileServiceManager getTileWrapper(CustomTile tile) {
        ComponentName component = tile.getComponent();
        TileServiceManager service = onCreateTileService(component, tile.getQsTile());
        synchronized (this.mServices) {
            this.mServices.put(tile, service);
            this.mTiles.put(component, tile);
            this.mTokenMap.put(service.getToken(), tile);
        }
        return service;
    }

    /* access modifiers changed from: protected */
    public TileServiceManager onCreateTileService(ComponentName component, Tile tile) {
        return new TileServiceManager(this, this.mHandler, component, tile);
    }

    public void freeService(CustomTile tile, TileServiceManager service) {
        synchronized (this.mServices) {
            service.setBindAllowed(false);
            service.handleDestroy();
            this.mServices.remove(tile);
            this.mTokenMap.remove(service.getToken());
            this.mTiles.remove(tile.getComponent());
            this.mMainHandler.post(new Runnable(tile.getComponent().getClassName()) {
                private final /* synthetic */ String f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    TileServices.this.mHost.getIconController().removeIcon(this.f$1);
                }
            });
        }
    }

    public void recalculateBindAllowance() {
        ArrayList<TileServiceManager> services;
        synchronized (this.mServices) {
            services = new ArrayList<>(this.mServices.values());
        }
        int N = services.size();
        if (N > this.mMaxBound) {
            long currentTime = System.currentTimeMillis();
            for (int i = 0; i < N; i++) {
                services.get(i).calculateBindPriority(currentTime);
            }
            Collections.sort(services, SERVICE_SORT);
        }
        int i2 = 0;
        while (i2 < this.mMaxBound && i2 < N) {
            services.get(i2).setBindAllowed(true);
            i2++;
        }
        while (i2 < N) {
            services.get(i2).setBindAllowed(false);
            i2++;
        }
    }

    private void verifyCaller(CustomTile tile) {
        try {
            if (Binder.getCallingUid() != this.mContext.getPackageManager().getPackageUidAsUser(tile.getComponent().getPackageName(), Binder.getCallingUserHandle().getIdentifier())) {
                throw new SecurityException("Component outside caller's uid");
            }
        } catch (PackageManager.NameNotFoundException e) {
            throw new SecurityException(e);
        }
    }

    /* access modifiers changed from: private */
    public void requestListening(ComponentName component) {
        synchronized (this.mServices) {
            CustomTile customTile = getTileForComponent(component);
            if (customTile == null) {
                Log.d("TileServices", "Couldn't find tile for " + component);
                return;
            }
            TileServiceManager service = this.mServices.get(customTile);
            if (service.isActiveTile()) {
                service.setBindRequested(true);
                try {
                    service.getTileService().onStartListening();
                } catch (RemoteException e) {
                }
            }
        }
    }

    public void updateQsTile(Tile tile, IBinder token) {
        CustomTile customTile = getTileForToken(token);
        if (customTile != null) {
            verifyCaller(customTile);
            synchronized (this.mServices) {
                TileServiceManager tileServiceManager = this.mServices.get(customTile);
                tileServiceManager.clearPendingBind();
                tileServiceManager.setLastUpdate(System.currentTimeMillis());
            }
            customTile.updateState(tile);
            customTile.refreshState();
        }
    }

    public void onStartSuccessful(IBinder token) {
        CustomTile customTile = getTileForToken(token);
        if (customTile != null) {
            verifyCaller(customTile);
            synchronized (this.mServices) {
                this.mServices.get(customTile).clearPendingBind();
            }
            customTile.refreshState();
        }
    }

    public void onShowDialog(IBinder token) {
        CustomTile customTile = getTileForToken(token);
        if (customTile != null) {
            verifyCaller(customTile);
            customTile.onDialogShown();
            this.mHost.forceCollapsePanels();
            this.mServices.get(customTile).setShowingDialog(true);
        }
    }

    public void onDialogHidden(IBinder token) {
        CustomTile customTile = getTileForToken(token);
        if (customTile != null) {
            verifyCaller(customTile);
            this.mServices.get(customTile).setShowingDialog(false);
            customTile.onDialogHidden();
        }
    }

    public void onStartActivity(IBinder token) {
        CustomTile customTile = getTileForToken(token);
        if (customTile != null) {
            verifyCaller(customTile);
            this.mHost.forceCollapsePanels();
        }
    }

    public void updateStatusIcon(IBinder token, Icon icon, String contentDescription) {
        StatusBarIcon statusBarIcon;
        CustomTile customTile = getTileForToken(token);
        if (customTile != null) {
            verifyCaller(customTile);
            try {
                final ComponentName componentName = customTile.getComponent();
                String packageName = componentName.getPackageName();
                UserHandle userHandle = getCallingUserHandle();
                if (this.mContext.getPackageManager().getPackageInfoAsUser(packageName, 0, userHandle.getIdentifier()).applicationInfo.isSystemApp()) {
                    if (icon != null) {
                        statusBarIcon = new StatusBarIcon(userHandle, packageName, icon, 0, 0, contentDescription);
                    } else {
                        statusBarIcon = null;
                    }
                    final StatusBarIcon statusIcon = statusBarIcon;
                    this.mMainHandler.post(new Runnable() {
                        public void run() {
                            StatusBarIconController iconController = TileServices.this.mHost.getIconController();
                            iconController.setIcon(componentName.getClassName(), statusIcon);
                            iconController.setExternalIcon(componentName.getClassName());
                        }
                    });
                }
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
    }

    public Tile getTile(IBinder token) {
        CustomTile customTile = getTileForToken(token);
        if (customTile == null) {
            return null;
        }
        verifyCaller(customTile);
        return customTile.getQsTile();
    }

    public void startUnlockAndRun(IBinder token) {
        CustomTile customTile = getTileForToken(token);
        if (customTile != null) {
            verifyCaller(customTile);
            customTile.startUnlockAndRun();
        }
    }

    public boolean isLocked() {
        return ((KeyguardMonitor) Dependency.get(KeyguardMonitor.class)).isShowing();
    }

    public boolean isSecure() {
        KeyguardMonitor keyguardMonitor = (KeyguardMonitor) Dependency.get(KeyguardMonitor.class);
        return keyguardMonitor.isSecure() && keyguardMonitor.isShowing();
    }

    private CustomTile getTileForToken(IBinder token) {
        CustomTile customTile;
        synchronized (this.mServices) {
            customTile = this.mTokenMap.get(token);
        }
        return customTile;
    }

    private CustomTile getTileForComponent(ComponentName component) {
        CustomTile customTile;
        synchronized (this.mServices) {
            customTile = this.mTiles.get(component);
        }
        return customTile;
    }
}
