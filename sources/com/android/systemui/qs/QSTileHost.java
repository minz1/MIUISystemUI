package com.android.systemui.qs;

import android.app.ActivityManager;
import android.app.MiuiStatusBarManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.quicksettings.TileCompat;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import com.android.systemui.AnalyticsHelper;
import com.android.systemui.Constants;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.Util;
import com.android.systemui.plugins.PluginListener;
import com.android.systemui.plugins.PluginManager;
import com.android.systemui.plugins.qs.QSFactory;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.plugins.qs.QSTileView;
import com.android.systemui.proxy.UserManager;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.external.CustomTile;
import com.android.systemui.qs.external.TileLifecycleManager;
import com.android.systemui.qs.external.TileServices;
import com.android.systemui.qs.tileimpl.QSFactoryImpl;
import com.android.systemui.qs.tiles.DriveModeTile;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.phone.StatusBarIconController;
import com.android.systemui.tuner.TunerService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class QSTileHost implements PluginListener<QSFactory>, QSHost, TunerService.Tunable {
    private static final boolean DEBUG = Constants.DEBUG;
    /* access modifiers changed from: private */
    public final List<QSHost.Callback> mCallbacks = new ArrayList();
    /* access modifiers changed from: private */
    public boolean mCollpaseAfterClick = false;
    /* access modifiers changed from: private */
    public final Context mContext;
    private int mCurrentUser;
    private Handler mHandler;
    /* access modifiers changed from: private */
    public volatile boolean mHasDriveApp = false;
    private final StatusBarIconController mIconController;
    private BroadcastReceiver mPackageChangeReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.PACKAGE_ADDED".equals(action)) {
                if (!QSTileHost.this.mHasDriveApp && "com.xiaomi.drivemode".equals(intent.getData().getSchemeSpecificPart())) {
                    boolean unused = QSTileHost.this.mHasDriveApp = true;
                }
            } else if ("android.intent.action.PACKAGE_REMOVED".equals(action) && QSTileHost.this.mHasDriveApp && intent.getData().getSchemeSpecificPart().equals("com.xiaomi.drivemode")) {
                boolean unused2 = QSTileHost.this.mHasDriveApp = false;
                DriveModeTile.leaveDriveMode(QSTileHost.this.getContext());
            }
        }
    };
    private final ArrayList<QSFactory> mQsFactories = new ArrayList<>();
    private final TileServices mServices;
    private final StatusBar mStatusBar;
    protected final ArrayList<String> mTileSpecs = new ArrayList<>();
    private final LinkedHashMap<String, QSTile> mTiles = new LinkedHashMap<>();

    public QSTileHost(final Context context, StatusBar statusBar, StatusBarIconController iconController) {
        this.mIconController = iconController;
        this.mContext = context;
        this.mStatusBar = statusBar;
        this.mServices = new TileServices(this, (Looper) Dependency.get(Dependency.BG_LOOPER));
        this.mQsFactories.add(new QSFactoryImpl(this));
        ((PluginManager) Dependency.get(PluginManager.class)).addPluginListener(this, (Class<?>) QSFactory.class, true);
        ((TunerService) Dependency.get(TunerService.class)).addTunable(this, "sysui_qs_tiles");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PACKAGE_ADDED");
        intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter.addDataScheme("package");
        this.mContext.registerReceiverAsUser(this.mPackageChangeReceiver, UserHandle.ALL, intentFilter, null, null);
        IntentFilter userChangeFilter = new IntentFilter();
        userChangeFilter.addAction("android.intent.action.USER_SWITCHED");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                boolean unused = QSTileHost.this.mCollpaseAfterClick = MiuiStatusBarManager.isCollapseAfterClickedForUser(context, -2);
            }
        }, userChangeFilter);
        new Thread() {
            public void run() {
                try {
                    QSTileHost qSTileHost = QSTileHost.this;
                    boolean z = false;
                    if (QSTileHost.this.mContext.getPackageManager().getApplicationInfo("com.xiaomi.drivemode", 0) != null) {
                        z = true;
                    }
                    boolean unused = qSTileHost.mHasDriveApp = z;
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }.start();
        this.mHandler = new Handler();
        ContentObserver contentObserver = new ContentObserver(this.mHandler) {
            public void onChange(boolean selfChange) {
                boolean unused = QSTileHost.this.mCollpaseAfterClick = MiuiStatusBarManager.isCollapseAfterClickedForUser(context, -2);
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("status_bar_collapse_after_clicked"), false, contentObserver, -1);
        contentObserver.onChange(false);
    }

    public boolean isDriveModeInstalled() {
        return this.mHasDriveApp;
    }

    public StatusBarIconController getIconController() {
        return this.mIconController;
    }

    public void onPluginConnected(QSFactory plugin, Context pluginContext) {
        this.mQsFactories.add(0, plugin);
        String value = ((TunerService) Dependency.get(TunerService.class)).getValue("sysui_qs_tiles");
        Log.d("QSTileHost", "onPluginConnected: force remove and recreate of all tiles.");
        onTuningChanged("sysui_qs_tiles", "");
        onTuningChanged("sysui_qs_tiles", value);
    }

    public void onPluginDisconnected(QSFactory plugin) {
        this.mQsFactories.remove(plugin);
        Log.d("QSTileHost", "onPluginDisconnected: force remove and recreate of all tiles.");
        String value = ((TunerService) Dependency.get(TunerService.class)).getValue("sysui_qs_tiles");
        onTuningChanged("sysui_qs_tiles", "");
        onTuningChanged("sysui_qs_tiles", value);
    }

    public void addCallback(QSHost.Callback callback) {
        this.mCallbacks.add(callback);
    }

    public void removeCallback(QSHost.Callback callback) {
        this.mCallbacks.remove(callback);
    }

    public Collection<QSTile> getTiles() {
        return this.mTiles.values();
    }

    public QSTile getTile(String tileSpec) {
        return this.mTiles.get(tileSpec);
    }

    public void warn(String message, Throwable t) {
    }

    public void collapsePanels() {
        if (this.mStatusBar.getBarState() == 1) {
            this.mStatusBar.closeQs();
        } else {
            this.mStatusBar.postAnimateCollapsePanels();
        }
    }

    public boolean isQSFullyCollapsed() {
        return this.mStatusBar.isQSFullyCollapsed();
    }

    public boolean collapseAfterClick() {
        return this.mCollpaseAfterClick;
    }

    public void forceCollapsePanels() {
        this.mStatusBar.postAnimateForceCollapsePanels();
    }

    public Context getContext() {
        return this.mContext;
    }

    public TileServices getTileServices() {
        return this.mServices;
    }

    public int indexOf(String spec) {
        return this.mTileSpecs.indexOf(spec);
    }

    public void onTuningChanged(String key, String newValue) {
        if (!"sysui_qs_tiles".equals(key)) {
            Slog.d("QSTileHost", "onTuningChanged: other key: " + key);
            return;
        }
        if (newValue == null && UserManager.isDeviceInDemoMode(this.mContext)) {
            newValue = this.mContext.getResources().getString(R.string.quick_settings_tiles_retail_mode);
        }
        List<String> tileSpecs = loadTileSpecs(this.mContext, newValue);
        Slog.d("QSTileHost", "onTuningChanged: recreating tiles: newValue: " + newValue + ", tileSpecs: " + tileSpecs);
        int currentUser = ActivityManager.getCurrentUser();
        if (!tileSpecs.equals(this.mTileSpecs) || currentUser != this.mCurrentUser) {
            for (Map.Entry<String, QSTile> entry : this.mTiles.entrySet()) {
                if (!tileSpecs.contains(entry.getKey())) {
                    if (DEBUG) {
                        Log.d("QSTileHost", "Destroying tile: " + entry.getKey());
                    }
                    entry.getValue().destroy();
                }
            }
            LinkedHashMap<String, QSTile> newTiles = new LinkedHashMap<>();
            for (String tileSpec : tileSpecs) {
                QSTile tile = this.mTiles.get(tileSpec);
                if (tile == null || ((tile instanceof CustomTile) && ((CustomTile) tile).getUser() != currentUser)) {
                    if (DEBUG) {
                        Log.d("QSTileHost", "Creating tile: " + tileSpec);
                    }
                    try {
                        QSTile tile2 = createTile(tileSpec);
                        if (tile2 != null) {
                            if (tile2.isAvailable()) {
                                tile2.setTileSpec(tileSpec);
                                newTiles.put(tileSpec, tile2);
                            } else {
                                Slog.d("QSTileHost", "onTuningChanged: unavailable custom tile: " + tile2);
                                tile2.destroy();
                            }
                        }
                    } catch (Throwable t) {
                        Slog.w("QSTileHost", "onTuningChanged: Error creating tile for spec: " + tileSpec, t);
                    }
                } else if (tile.isAvailable()) {
                    if (DEBUG) {
                        Log.d("QSTileHost", "Adding " + tile);
                    }
                    tile.removeCallbacks();
                    if (!(tile instanceof CustomTile) && this.mCurrentUser != currentUser) {
                        tile.userSwitch(currentUser);
                    }
                    newTiles.put(tileSpec, tile);
                } else {
                    Slog.d("QSTileHost", "onTuningChanged: unavailable tile: " + tile);
                    tile.destroy();
                }
            }
            this.mCurrentUser = currentUser;
            if (this.mTiles.size() == 0) {
                AnalyticsHelper.trackQSTilesCount(tileSpecs.size());
            }
            this.mTileSpecs.clear();
            this.mTileSpecs.addAll(tileSpecs);
            this.mTiles.clear();
            this.mTiles.putAll(newTiles);
            int delay = Util.isMiuiOptimizationDisabled() ? 200 : 0;
            if (this.mHandler == null) {
                this.mHandler = new Handler();
            }
            this.mHandler.postDelayed(new Runnable() {
                public void run() {
                    for (int i = 0; i < QSTileHost.this.mCallbacks.size(); i++) {
                        ((QSHost.Callback) QSTileHost.this.mCallbacks.get(i)).onTilesChanged();
                    }
                }
            }, (long) delay);
        }
    }

    public void addTile(ComponentName tile) {
        List<String> newSpecs = new ArrayList<>(this.mTileSpecs);
        newSpecs.add(0, CustomTile.toSpec(tile));
        changeTiles(this.mTileSpecs, newSpecs);
    }

    public void removeTile(ComponentName tile) {
        List<String> newSpecs = new ArrayList<>(this.mTileSpecs);
        newSpecs.remove(CustomTile.toSpec(tile));
        changeTiles(this.mTileSpecs, newSpecs);
    }

    public void changeTiles(List<String> previousTiles, List<String> newTiles) {
        List<String> list = newTiles;
        int NP = previousTiles.size();
        for (int i = 0; i < NP; i++) {
            String tileSpec = previousTiles.get(i);
            if (tileSpec.startsWith("custom(") && !list.contains(tileSpec)) {
                ComponentName component = CustomTile.getComponentFromSpec(tileSpec);
                TileLifecycleManager lifecycleManager = new TileLifecycleManager(new Handler(), this.mContext, this.mServices, TileCompat.newTile(component), new Intent().setComponent(component), new UserHandle(ActivityManager.getCurrentUser()));
                lifecycleManager.onStopListening();
                lifecycleManager.onTileRemoved();
                TileLifecycleManager.setTileAdded(this.mContext, component, false);
                lifecycleManager.flushMessagesAndUnbind();
            }
        }
        List<String> list2 = previousTiles;
        if (DEBUG) {
            Log.d("QSTileHost", "saveCurrentTiles " + list);
        }
        Settings.Secure.putStringForUser(getContext().getContentResolver(), "sysui_qs_tiles", TextUtils.join(",", list), ActivityManager.getCurrentUser());
    }

    public QSTile createTile(String tileSpec) {
        for (int i = 0; i < this.mQsFactories.size(); i++) {
            QSTile t = this.mQsFactories.get(i).createTile(tileSpec);
            if (t != null) {
                return t;
            }
        }
        return null;
    }

    public QSTileView createTileView(QSTile tile, boolean collapsedView) {
        for (int i = 0; i < this.mQsFactories.size(); i++) {
            QSTileView view = this.mQsFactories.get(i).createTileView(tile, collapsedView);
            if (view != null) {
                return view;
            }
        }
        throw new RuntimeException("Default factory didn't create view for " + tile.getTileSpec());
    }

    /* access modifiers changed from: protected */
    public List<String> loadTileSpecs(Context context, String tileList) {
        Resources res = context.getResources();
        String defaultTileList = res.getString(R.string.quick_settings_tiles_default);
        if (tileList == null) {
            tileList = res.getString(R.string.quick_settings_tiles);
            Slog.d("QSTileHost", "loadTileSpecs: Loaded tile specs from config: " + tileList);
        } else if (!tileList.contains("edit")) {
            tileList = res.getString(R.string.quick_settings_tiles);
            Slog.d("QSTileHost", "loadTileSpecs: missing edit, loaded tile specs from config: " + tileList);
        } else {
            Slog.d("QSTileHost", "loadTileSpecs: loaded tile specs from setting: " + tileList);
        }
        ArrayList<String> tiles = new ArrayList<>();
        boolean addedDefault = false;
        for (String tile : tileList.split(",")) {
            String tile2 = tile.trim();
            if (!tile2.isEmpty()) {
                if (!tile2.equals("default")) {
                    tiles.add(tile2);
                } else if (!addedDefault) {
                    tiles.addAll(Arrays.asList(defaultTileList.split(",")));
                    addedDefault = true;
                }
            }
        }
        filterQSTiles(tiles);
        return tiles;
    }

    private void filterQSTiles(List<String> tiles) {
        Iterator<String> iterator = tiles.iterator();
        while (iterator.hasNext()) {
            String tileSpec = iterator.next();
            if (!tileSpec.startsWith("custom(")) {
                QSTile qsTile = createTile(tileSpec);
                if (qsTile == null) {
                    iterator.remove();
                } else {
                    if (!qsTile.isAvailable()) {
                        iterator.remove();
                    }
                    qsTile.destroy();
                }
            }
        }
        if (tiles.size() < 12) {
            int index = tiles.size();
            int num = 12 - tiles.size();
            String[] possibleTiles = this.mContext.getString(R.string.quick_settings_tiles_stock).split(",");
            int j = 0;
            for (int i = 0; i < possibleTiles.length && j < num; i++) {
                String tileSpec2 = possibleTiles[i];
                if (!tiles.contains(tileSpec2)) {
                    QSTile tile = createTile(possibleTiles[i]);
                    if (tile != null) {
                        if (tile.isAvailable()) {
                            tiles.add((index + j) - 1, tileSpec2);
                            j++;
                        }
                        tile.destroy();
                    }
                }
            }
        }
    }
}
