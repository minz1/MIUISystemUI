package com.android.systemui.qs.customize;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.ArraySet;
import android.widget.Button;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSTileHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class TileQueryHelper {
    private final Handler mBgHandler;
    /* access modifiers changed from: private */
    public final Context mContext;
    /* access modifiers changed from: private */
    public boolean mFinished;
    /* access modifiers changed from: private */
    public final TileStateListener mListener;
    /* access modifiers changed from: private */
    public final HashMap<String, QSTile> mLiveTiles = new HashMap<>();
    /* access modifiers changed from: private */
    public final Handler mMainHandler;
    private final ArraySet<String> mSpecs = new ArraySet<>();
    /* access modifiers changed from: private */
    public final ArrayList<TileInfo> mTiles = new ArrayList<>();

    private class TileCallback implements QSTile.Callback {
        private QSTile mTile;

        TileCallback(QSTile tile) {
            this.mTile = tile;
        }

        public void onStateChanged(QSTile.State state) {
            QSTile.State stateCopy = this.mTile.getState().copy();
            TileQueryHelper.this.updateStateForCustomizer(stateCopy);
            stateCopy.label = this.mTile.getTileLabel();
            Iterator it = TileQueryHelper.this.mTiles.iterator();
            while (it.hasNext()) {
                final TileInfo tileInfo = (TileInfo) it.next();
                if (TextUtils.equals(this.mTile.getTileSpec(), tileInfo.spec)) {
                    tileInfo.state = stateCopy;
                    TileQueryHelper.this.mMainHandler.post(new Runnable() {
                        public void run() {
                            TileQueryHelper.this.mListener.onTileChanged(tileInfo);
                        }
                    });
                    return;
                }
            }
        }

        public void onShowDetail(boolean show) {
        }

        public void onShowEdit(boolean show) {
        }

        public void onToggleStateChanged(boolean state) {
        }

        public void onScanStateChanged(boolean state) {
        }

        public void onAnnouncementRequested(CharSequence announcement) {
        }
    }

    public static class TileInfo {
        public boolean isSystem;
        public String spec;
        public QSTile.State state;
    }

    public interface TileStateListener {
        void onTileChanged(TileInfo tileInfo);

        void onTilesChanged(List<TileInfo> list, Map<String, QSTile> map);
    }

    public TileQueryHelper(Context context, TileStateListener listener) {
        this.mContext = context;
        this.mListener = listener;
        this.mBgHandler = new Handler((Looper) Dependency.get(Dependency.BG_LOOPER));
        this.mMainHandler = (Handler) Dependency.get(Dependency.MAIN_HANDLER);
    }

    public void queryTiles(QSTileHost host) {
        this.mTiles.clear();
        this.mSpecs.clear();
        this.mFinished = false;
        addStockTiles(host);
        addPackageTiles(host);
    }

    public void releaseTiles() {
        for (QSTile tile : this.mLiveTiles.values()) {
            tile.removeCallbacks();
            tile.setListening(this, false);
            tile.destroy();
        }
        this.mLiveTiles.clear();
    }

    public boolean isFinished() {
        return this.mFinished;
    }

    private void addStockTiles(QSTileHost host) {
        String[] possibleTiles = this.mContext.getString(R.string.quick_settings_tiles_stock).split(",");
        final ArrayList<QSTile> tilesToAdd = new ArrayList<>();
        for (String spec : possibleTiles) {
            QSTile tile = host.createTile(spec);
            if (tile != null) {
                if (!tile.isAvailable()) {
                    tile.destroy();
                } else {
                    tile.setListening(this, true);
                    tile.clearState();
                    tile.refreshState();
                    tile.addCallback(new TileCallback(tile));
                    tile.setTileSpec(spec);
                    tilesToAdd.add(tile);
                    this.mLiveTiles.put(spec, tile);
                }
            }
        }
        this.mBgHandler.post(new Runnable() {
            public void run() {
                Iterator it = tilesToAdd.iterator();
                while (it.hasNext()) {
                    QSTile tile = (QSTile) it.next();
                    QSTile.State state = tile.getState().copy();
                    state.label = tile.getTileLabel();
                    TileQueryHelper.this.addTile(tile.getTileSpec(), (CharSequence) null, state, true);
                }
            }
        });
    }

    private void addPackageTiles(final QSTileHost host) {
        this.mBgHandler.post(new Runnable() {
            /* JADX WARNING: Removed duplicated region for block: B:38:0x00ef  */
            /* JADX WARNING: Removed duplicated region for block: B:40:0x00f8  */
            /* Code decompiled incorrectly, please refer to instructions dump. */
            public void run() {
                /*
                    r21 = this;
                    r1 = r21
                    int r0 = android.os.Build.VERSION.SDK_INT
                    r2 = 1
                    r3 = 23
                    if (r0 != r3) goto L_0x000f
                    com.android.systemui.qs.customize.TileQueryHelper r0 = com.android.systemui.qs.customize.TileQueryHelper.this
                    r0.notifyTilesChanged(r2)
                    return
                L_0x000f:
                    com.android.systemui.qs.QSTileHost r0 = r3
                    java.util.Collection r3 = r0.getTiles()
                    com.android.systemui.qs.customize.TileQueryHelper r0 = com.android.systemui.qs.customize.TileQueryHelper.this
                    android.content.Context r0 = r0.mContext
                    android.content.pm.PackageManager r4 = r0.getPackageManager()
                    android.content.Intent r0 = new android.content.Intent
                    java.lang.String r5 = "android.service.quicksettings.action.QS_TILE"
                    r0.<init>(r5)
                    int r5 = android.app.ActivityManager.getCurrentUser()
                    r6 = 0
                    java.util.List r5 = r4.queryIntentServicesAsUser(r0, r6, r5)
                    com.android.systemui.qs.customize.TileQueryHelper r0 = com.android.systemui.qs.customize.TileQueryHelper.this
                    android.content.Context r0 = r0.mContext
                    r7 = 2131821955(0x7f110583, float:1.9276668E38)
                    java.lang.String r7 = r0.getString(r7)
                    java.util.Iterator r8 = r5.iterator()
                L_0x0040:
                    boolean r0 = r8.hasNext()
                    if (r0 == 0) goto L_0x00ff
                    java.lang.Object r0 = r8.next()
                    r9 = r0
                    android.content.pm.ResolveInfo r9 = (android.content.pm.ResolveInfo) r9
                    android.content.pm.ServiceInfo r0 = r9.serviceInfo
                    java.lang.String r10 = r0.packageName
                    android.content.ComponentName r0 = new android.content.ComponentName
                    android.content.pm.ServiceInfo r11 = r9.serviceInfo
                    java.lang.String r11 = r11.name
                    r0.<init>(r10, r11)
                    r11 = r0
                    java.lang.String r0 = r11.flattenToString()
                    boolean r0 = r7.contains(r0)
                    if (r0 == 0) goto L_0x0066
                    goto L_0x0040
                L_0x0066:
                    android.content.pm.ServiceInfo r0 = r9.serviceInfo
                    android.content.pm.ApplicationInfo r0 = r0.applicationInfo
                    java.lang.CharSequence r12 = r0.loadLabel(r4)
                    java.lang.String r13 = com.android.systemui.qs.external.CustomTile.toSpec(r11)
                    com.android.systemui.qs.customize.TileQueryHelper r0 = com.android.systemui.qs.customize.TileQueryHelper.this
                    com.android.systemui.plugins.qs.QSTile$State r14 = r0.getState(r3, r13)
                    if (r14 == 0) goto L_0x0080
                    com.android.systemui.qs.customize.TileQueryHelper r0 = com.android.systemui.qs.customize.TileQueryHelper.this
                    r0.addTile((java.lang.String) r13, (java.lang.CharSequence) r12, (com.android.systemui.plugins.qs.QSTile.State) r14, (boolean) r6)
                    goto L_0x0040
                L_0x0080:
                    android.content.pm.ServiceInfo r0 = r9.serviceInfo
                    int r0 = r0.icon
                    if (r0 != 0) goto L_0x008f
                    android.content.pm.ServiceInfo r0 = r9.serviceInfo
                    android.content.pm.ApplicationInfo r0 = r0.applicationInfo
                    int r0 = r0.icon
                    if (r0 != 0) goto L_0x008f
                    goto L_0x0040
                L_0x008f:
                    android.content.pm.ServiceInfo r0 = r9.serviceInfo
                    int r0 = r0.icon
                    if (r0 == 0) goto L_0x009a
                    android.content.pm.ServiceInfo r0 = r9.serviceInfo
                    int r0 = r0.icon
                    goto L_0x00a0
                L_0x009a:
                    android.content.pm.ServiceInfo r0 = r9.serviceInfo
                    android.content.pm.ApplicationInfo r0 = r0.applicationInfo
                    int r0 = r0.icon
                L_0x00a0:
                    r15 = r0
                    if (r15 == 0) goto L_0x00a8
                    android.graphics.drawable.Icon r0 = android.graphics.drawable.Icon.createWithResource(r10, r15)
                    goto L_0x00a9
                L_0x00a8:
                    r0 = 0
                L_0x00a9:
                    r16 = r0
                    r17 = 0
                    r6 = r16
                    if (r6 == 0) goto L_0x00c8
                    com.android.systemui.qs.customize.TileQueryHelper r0 = com.android.systemui.qs.customize.TileQueryHelper.this     // Catch:{ Exception -> 0x00be }
                    android.content.Context r0 = r0.mContext     // Catch:{ Exception -> 0x00be }
                    android.graphics.drawable.Drawable r0 = r6.loadDrawable(r0)     // Catch:{ Exception -> 0x00be }
                    r17 = r0
                    goto L_0x00ca
                L_0x00be:
                    r0 = move-exception
                    java.lang.String r2 = "TileQueryHelper"
                    r18 = r0
                    java.lang.String r0 = "Invalid icon"
                    android.util.Log.w(r2, r0)
                L_0x00c8:
                    r0 = r17
                L_0x00ca:
                    java.lang.String r2 = "android.permission.BIND_QUICK_SETTINGS_TILE"
                    r19 = r3
                    android.content.pm.ServiceInfo r3 = r9.serviceInfo
                    java.lang.String r3 = r3.permission
                    boolean r2 = r2.equals(r3)
                    if (r2 != 0) goto L_0x00df
                L_0x00d9:
                    r3 = r19
                    r2 = 1
                    r6 = 0
                    goto L_0x0040
                L_0x00df:
                    if (r0 != 0) goto L_0x00e2
                    goto L_0x00d9
                L_0x00e2:
                    r0.mutate()
                    android.content.pm.ServiceInfo r2 = r9.serviceInfo
                    java.lang.CharSequence r2 = r2.loadLabel(r4)
                    com.android.systemui.qs.customize.TileQueryHelper r3 = com.android.systemui.qs.customize.TileQueryHelper.this
                    if (r2 == 0) goto L_0x00f8
                    java.lang.String r16 = r2.toString()
                L_0x00f3:
                    r20 = r2
                    r2 = r16
                    goto L_0x00fb
                L_0x00f8:
                    java.lang.String r16 = "null"
                    goto L_0x00f3
                L_0x00fb:
                    r3.addTile((java.lang.String) r13, (android.graphics.drawable.Drawable) r0, (java.lang.CharSequence) r2, (java.lang.CharSequence) r12)
                    goto L_0x00d9
                L_0x00ff:
                    r19 = r3
                    com.android.systemui.qs.customize.TileQueryHelper r0 = com.android.systemui.qs.customize.TileQueryHelper.this
                    r2 = 1
                    r0.notifyTilesChanged(r2)
                    return
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.qs.customize.TileQueryHelper.AnonymousClass2.run():void");
            }
        });
    }

    /* access modifiers changed from: private */
    public void notifyTilesChanged(final boolean finished) {
        final ArrayList<TileInfo> tilesToReturn = new ArrayList<>(this.mTiles);
        this.mMainHandler.post(new Runnable() {
            public void run() {
                TileQueryHelper.this.mListener.onTilesChanged(tilesToReturn, TileQueryHelper.this.mLiveTiles);
                boolean unused = TileQueryHelper.this.mFinished = finished;
            }
        });
    }

    /* access modifiers changed from: private */
    public void updateStateForCustomizer(QSTile.State state) {
        state.dualTarget = false;
        state.expandedAccessibilityClassName = Button.class.getName();
    }

    /* access modifiers changed from: private */
    public QSTile.State getState(Collection<QSTile> tiles, String spec) {
        for (QSTile tile : tiles) {
            if (spec.equals(tile.getTileSpec())) {
                return tile.getState().copy();
            }
        }
        return null;
    }

    /* access modifiers changed from: private */
    public void addTile(String spec, CharSequence appLabel, QSTile.State state, boolean isSystem) {
        if (!this.mSpecs.contains(spec) && !"edit".equals(spec)) {
            TileInfo info = new TileInfo();
            info.state = state;
            updateStateForCustomizer(info.state);
            info.spec = spec;
            info.state.secondaryLabel = (isSystem || TextUtils.equals(state.label, appLabel)) ? null : appLabel;
            info.isSystem = isSystem;
            this.mTiles.add(info);
            this.mSpecs.add(spec);
        }
    }

    /* access modifiers changed from: private */
    public void addTile(String spec, Drawable drawable, CharSequence label, CharSequence appLabel) {
        QSTile.State state = new QSTile.State();
        state.state = 1;
        state.label = label;
        state.contentDescription = label;
        state.icon = new QSTileImpl.DrawableIcon(drawable);
        addTile(spec, appLabel, state, false);
    }
}
