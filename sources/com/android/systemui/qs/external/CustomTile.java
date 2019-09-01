package com.android.systemui.qs.external;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.metrics.LogMaker;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.service.quicksettings.IQSTileService;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileCompat;
import android.util.Log;
import android.view.IWindowManager;
import android.view.IWindowManagerCompat;
import android.view.WindowManagerGlobal;
import com.android.systemui.Dependency;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSTileHost;
import com.android.systemui.qs.external.TileLifecycleManager;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import java.util.Objects;

public class CustomTile extends QSTileImpl<QSTile.State> implements TileLifecycleManager.TileChangeListener {
    private final ComponentName mComponent;
    private Icon mDefaultIcon;
    private boolean mIsShowingDialog;
    private boolean mIsTokenGranted;
    private boolean mListening;
    /* access modifiers changed from: private */
    public final IQSTileService mService;
    private final TileServiceManager mServiceManager;
    private final Tile mTile;
    private final IBinder mToken = new Binder();
    private final int mUser;
    private final IWindowManager mWindowManager = WindowManagerGlobal.getWindowManagerService();

    private CustomTile(QSTileHost host, String action) {
        super(host);
        this.mComponent = ComponentName.unflattenFromString(action);
        this.mTile = TileCompat.newTile(this.mComponent);
        setTileIcon();
        this.mServiceManager = host.getTileServices().getTileWrapper(this);
        this.mService = this.mServiceManager.getTileService();
        this.mServiceManager.setTileChangeListener(this);
        this.mUser = ActivityManager.getCurrentUser();
    }

    /* JADX WARNING: Removed duplicated region for block: B:17:0x003e A[Catch:{ Exception -> 0x0067 }] */
    /* JADX WARNING: Removed duplicated region for block: B:18:0x0049 A[Catch:{ Exception -> 0x0067 }] */
    /* JADX WARNING: Removed duplicated region for block: B:21:0x004e A[Catch:{ Exception -> 0x0067 }] */
    /* JADX WARNING: Removed duplicated region for block: B:24:0x005d A[Catch:{ Exception -> 0x0067 }] */
    /* JADX WARNING: Removed duplicated region for block: B:29:? A[RETURN, SYNTHETIC] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void setTileIcon() {
        /*
            r8 = this;
            r0 = 0
            android.content.Context r1 = r8.mContext     // Catch:{ Exception -> 0x0067 }
            android.content.pm.PackageManager r1 = r1.getPackageManager()     // Catch:{ Exception -> 0x0067 }
            r2 = 786432(0xc0000, float:1.102026E-39)
            boolean r3 = r8.isSystemApp(r1)     // Catch:{ Exception -> 0x0067 }
            if (r3 == 0) goto L_0x0011
            r2 = r2 | 512(0x200, float:7.175E-43)
        L_0x0011:
            android.content.ComponentName r3 = r8.mComponent     // Catch:{ Exception -> 0x0067 }
            android.content.pm.ServiceInfo r3 = r1.getServiceInfo(r3, r2)     // Catch:{ Exception -> 0x0067 }
            int r4 = r3.icon     // Catch:{ Exception -> 0x0067 }
            if (r4 == 0) goto L_0x001e
            int r4 = r3.icon     // Catch:{ Exception -> 0x0067 }
            goto L_0x0022
        L_0x001e:
            android.content.pm.ApplicationInfo r4 = r3.applicationInfo     // Catch:{ Exception -> 0x0067 }
            int r4 = r4.icon     // Catch:{ Exception -> 0x0067 }
        L_0x0022:
            android.service.quicksettings.Tile r5 = r8.mTile     // Catch:{ Exception -> 0x0067 }
            android.graphics.drawable.Icon r5 = r5.getIcon()     // Catch:{ Exception -> 0x0067 }
            if (r5 == 0) goto L_0x003b
            android.service.quicksettings.Tile r5 = r8.mTile     // Catch:{ Exception -> 0x0067 }
            android.graphics.drawable.Icon r5 = r5.getIcon()     // Catch:{ Exception -> 0x0067 }
            android.graphics.drawable.Icon r6 = r8.mDefaultIcon     // Catch:{ Exception -> 0x0067 }
            boolean r5 = r8.iconEquals(r5, r6)     // Catch:{ Exception -> 0x0067 }
            if (r5 == 0) goto L_0x0039
            goto L_0x003b
        L_0x0039:
            r5 = 0
            goto L_0x003c
        L_0x003b:
            r5 = 1
        L_0x003c:
            if (r4 == 0) goto L_0x0049
            android.content.ComponentName r6 = r8.mComponent     // Catch:{ Exception -> 0x0067 }
            java.lang.String r6 = r6.getPackageName()     // Catch:{ Exception -> 0x0067 }
            android.graphics.drawable.Icon r6 = android.graphics.drawable.Icon.createWithResource(r6, r4)     // Catch:{ Exception -> 0x0067 }
            goto L_0x004a
        L_0x0049:
            r6 = r0
        L_0x004a:
            r8.mDefaultIcon = r6     // Catch:{ Exception -> 0x0067 }
            if (r5 == 0) goto L_0x0055
            android.service.quicksettings.Tile r6 = r8.mTile     // Catch:{ Exception -> 0x0067 }
            android.graphics.drawable.Icon r7 = r8.mDefaultIcon     // Catch:{ Exception -> 0x0067 }
            r6.setIcon(r7)     // Catch:{ Exception -> 0x0067 }
        L_0x0055:
            android.service.quicksettings.Tile r6 = r8.mTile     // Catch:{ Exception -> 0x0067 }
            java.lang.CharSequence r6 = r6.getLabel()     // Catch:{ Exception -> 0x0067 }
            if (r6 != 0) goto L_0x0066
            android.service.quicksettings.Tile r6 = r8.mTile     // Catch:{ Exception -> 0x0067 }
            java.lang.CharSequence r7 = r3.loadLabel(r1)     // Catch:{ Exception -> 0x0067 }
            r6.setLabel(r7)     // Catch:{ Exception -> 0x0067 }
        L_0x0066:
            goto L_0x006a
        L_0x0067:
            r1 = move-exception
            r8.mDefaultIcon = r0
        L_0x006a:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.qs.external.CustomTile.setTileIcon():void");
    }

    private boolean isSystemApp(PackageManager pm) throws PackageManager.NameNotFoundException {
        return pm.getApplicationInfo(this.mComponent.getPackageName(), 0).isSystemApp();
    }

    private boolean iconEquals(Icon icon1, Icon icon2) {
        if (icon1 == icon2) {
            return true;
        }
        if (icon1 == null || icon2 == null || icon1.getType() != 2 || icon2.getType() != 2 || icon1.getResId() != icon2.getResId() || !Objects.equals(icon1.getResPackage(), icon2.getResPackage())) {
            return false;
        }
        return true;
    }

    public void onTileChanged(ComponentName tile) {
        setTileIcon();
    }

    public boolean isAvailable() {
        return this.mDefaultIcon != null;
    }

    public int getUser() {
        return this.mUser;
    }

    public ComponentName getComponent() {
        return this.mComponent;
    }

    public LogMaker populate(LogMaker logMaker) {
        return super.populate(logMaker).setComponentName(this.mComponent);
    }

    public Tile getQsTile() {
        return this.mTile;
    }

    public void updateState(Tile tile) {
        this.mTile.setIcon(tile.getIcon());
        this.mTile.setLabel(tile.getLabel());
        this.mTile.setContentDescription(tile.getContentDescription());
        this.mTile.setState(tile.getState());
    }

    public void onDialogShown() {
        this.mIsShowingDialog = true;
    }

    public void onDialogHidden() {
        this.mIsShowingDialog = false;
        try {
            IWindowManagerCompat.removeWindowToken(this.mWindowManager, this.mToken, 0);
        } catch (RemoteException e) {
        }
    }

    public void handleSetListening(boolean listening) {
        if (this.mListening != listening) {
            this.mListening = listening;
            if (listening) {
                try {
                    setTileIcon();
                    refreshState();
                    if (!this.mServiceManager.isActiveTile()) {
                        this.mServiceManager.setBindRequested(true);
                        this.mService.onStartListening();
                    }
                } catch (RemoteException e) {
                }
            } else {
                this.mService.onStopListening();
                if (this.mIsTokenGranted && !this.mIsShowingDialog) {
                    try {
                        IWindowManagerCompat.removeWindowToken(this.mWindowManager, this.mToken, 0);
                    } catch (RemoteException e2) {
                    }
                    this.mIsTokenGranted = false;
                }
                this.mIsShowingDialog = false;
                this.mServiceManager.setBindRequested(false);
            }
        }
    }

    /* access modifiers changed from: protected */
    public void handleDestroy() {
        super.handleDestroy();
        if (this.mIsTokenGranted) {
            try {
                IWindowManagerCompat.removeWindowToken(this.mWindowManager, this.mToken, 0);
            } catch (RemoteException e) {
            }
        }
        this.mHost.getTileServices().freeService(this, this.mServiceManager);
    }

    public QSTile.State newTileState() {
        return new QSTile.State();
    }

    public Intent getLongClickIntent() {
        Intent i = new Intent("android.service.quicksettings.action.QS_TILE_PREFERENCES");
        i.setPackage(this.mComponent.getPackageName());
        Intent i2 = resolveIntent(i);
        if (i2 == null) {
            return new Intent("android.settings.APPLICATION_DETAILS_SETTINGS").setData(Uri.fromParts("package", this.mComponent.getPackageName(), null));
        }
        i2.putExtra("android.intent.extra.COMPONENT_NAME", this.mComponent);
        i2.putExtra("state", this.mTile.getState());
        return i2;
    }

    private Intent resolveIntent(Intent i) {
        ResolveInfo result = this.mContext.getPackageManager().resolveActivityAsUser(i, 0, ActivityManager.getCurrentUser());
        if (result != null) {
            return new Intent("android.service.quicksettings.action.QS_TILE_PREFERENCES").setClassName(result.activityInfo.packageName, result.activityInfo.name);
        }
        return null;
    }

    /* access modifiers changed from: protected */
    public void handleClick() {
        if (this.mTile.getState() != 0) {
            try {
                IWindowManagerCompat.addWindowToken(this.mWindowManager, this.mToken, 2035, 0);
                this.mIsTokenGranted = true;
            } catch (RemoteException e) {
            }
            try {
                if (this.mServiceManager.isActiveTile()) {
                    this.mServiceManager.setBindRequested(true);
                    this.mService.onStartListening();
                }
                this.mService.onClick(this.mToken);
            } catch (RemoteException e2) {
            }
        }
    }

    public CharSequence getTileLabel() {
        return getState().label;
    }

    /* access modifiers changed from: protected */
    public void handleUpdateState(QSTile.State state, Object arg) {
        Drawable drawable;
        int tileState = this.mTile.getState();
        if (this.mServiceManager.hasPendingBind()) {
            tileState = 0;
        }
        state.state = tileState;
        try {
            drawable = this.mTile.getIcon().loadDrawable(this.mContext);
        } catch (Exception e) {
            Log.w(this.TAG, "Invalid icon, forcing into unavailable state");
            state.state = 0;
            drawable = this.mDefaultIcon.loadDrawable(this.mContext);
        }
        state.icon = new QSTileImpl.DrawableIcon(drawable);
        state.label = this.mTile.getLabel();
        if (this.mTile.getContentDescription() != null) {
            state.contentDescription = this.mTile.getContentDescription();
        } else {
            state.contentDescription = state.label;
        }
    }

    public int getMetricsCategory() {
        return 268;
    }

    public void startUnlockAndRun() {
        ((ActivityStarter) Dependency.get(ActivityStarter.class)).postQSRunnableDismissingKeyguard(new Runnable() {
            public void run() {
                try {
                    CustomTile.this.mService.onUnlockComplete();
                } catch (RemoteException e) {
                }
            }
        });
    }

    public static String toSpec(ComponentName name) {
        return "custom(" + name.flattenToShortString() + ")";
    }

    public static ComponentName getComponentFromSpec(String spec) {
        String action = spec.substring("custom(".length(), spec.length() - 1);
        if (!action.isEmpty()) {
            return ComponentName.unflattenFromString(action);
        }
        throw new IllegalArgumentException("Empty custom tile spec action");
    }

    public static CustomTile create(QSTileHost host, String spec) {
        if (spec == null || !spec.startsWith("custom(") || !spec.endsWith(")")) {
            throw new IllegalArgumentException("Bad custom tile spec: " + spec);
        }
        String action = spec.substring("custom(".length(), spec.length() - 1);
        if (!action.isEmpty()) {
            return new CustomTile(host, action);
        }
        throw new IllegalArgumentException("Empty custom tile spec action");
    }
}
