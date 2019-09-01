package com.android.systemui.qs.tileimpl;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.metrics.LogMaker;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.ArraySet;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsLoggerCompat;
import com.android.settingslib.RestrictedLockUtils;
import com.android.systemui.AnalyticsHelper;
import com.android.systemui.Dependency;
import com.android.systemui.Util;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.qs.DetailAdapter;
import com.android.systemui.plugins.qs.QSIconView;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.plugins.qs.QSTile.State;
import com.android.systemui.qs.PagedTileLayout;
import com.android.systemui.qs.QSHost;
import java.util.ArrayList;
import java.util.Iterator;

public abstract class QSTileImpl<TState extends QSTile.State> implements QSTile {
    protected static final Object ARG_SHOW_TRANSIENT_ENABLING = new Object();
    /* access modifiers changed from: protected */
    public static final boolean DEBUG = Log.isLoggable("Tile", 3);
    /* access modifiers changed from: protected */
    public final String TAG = ("QSTile." + getClass().getSimpleName());
    private boolean mAnnounceNextStateChange;
    private final ArrayList<QSTile.Callback> mCallbacks = new ArrayList<>();
    /* access modifiers changed from: protected */
    public final Context mContext;
    /* access modifiers changed from: private */
    public RestrictedLockUtils.EnforcedAdmin mEnforcedAdmin;
    /* access modifiers changed from: protected */
    public QSTileImpl<TState>.H mHandler = new H((Looper) Dependency.get(Dependency.BG_LOOPER));
    /* access modifiers changed from: protected */
    public final QSHost mHost;
    private int mIsFullQs;
    private final ArraySet<Object> mListeners = new ArraySet<>();
    private final MetricsLogger mMetricsLogger = ((MetricsLogger) Dependency.get(MetricsLogger.class));
    private boolean mShowingDetail;
    private final Object mStaleListener = new Object();
    /* access modifiers changed from: protected */
    public TState mState = newTileState();
    private String mTileSpec;
    private TState mTmpState = newTileState();
    protected final Handler mUiHandler = new Handler(Looper.getMainLooper());

    public static class DrawableIcon extends QSTile.Icon {
        protected final Drawable mDrawable;

        public DrawableIcon(Drawable drawable) {
            this.mDrawable = drawable;
        }

        public Drawable getDrawable(Context context) {
            return this.mDrawable;
        }
    }

    protected final class H extends Handler {
        @VisibleForTesting
        protected H(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            try {
                boolean z = true;
                if (msg.what == 1) {
                    String name = "handleAddCallback";
                    QSTileImpl.this.handleAddCallback((QSTile.Callback) msg.obj);
                } else if (msg.what == 12) {
                    String name2 = "handleRemoveCallbacks";
                    QSTileImpl.this.handleRemoveCallbacks();
                } else if (msg.what == 13) {
                    String name3 = "handleRemoveCallback";
                    QSTileImpl.this.handleRemoveCallback((QSTile.Callback) msg.obj);
                } else if (msg.what == 2) {
                    String name4 = "handleClick";
                    if (!(msg.obj instanceof Boolean) || !((Boolean) msg.obj).booleanValue()) {
                        z = false;
                    }
                    boolean inCustomizer = z;
                    if (QSTileImpl.this.mState.disabledByPolicy) {
                        ((ActivityStarter) Dependency.get(ActivityStarter.class)).postStartActivityDismissingKeyguard(RestrictedLockUtils.getShowAdminSupportDetailsIntent(QSTileImpl.this.mContext, QSTileImpl.this.mEnforcedAdmin), 0);
                    } else if (QSTileImpl.this.getState().state != 0) {
                        QSTileImpl.this.handleClick();
                        if (!inCustomizer && QSTileImpl.this.mHost.collapseAfterClick() && !"edit".equals(QSTileImpl.this.getTileSpec()) && !"autobrightness".equals(QSTileImpl.this.getTileSpec())) {
                            QSTileImpl.this.mHost.collapsePanels();
                        }
                    }
                } else if (msg.what == 3) {
                    String name5 = "handleSecondaryClick";
                    QSTileImpl.this.handleSecondaryClick();
                } else if (msg.what == 4) {
                    String name6 = "handleLongClick";
                    QSTileImpl.this.handleLongClick();
                } else if (msg.what == 5) {
                    String name7 = "handleRefreshState";
                    QSTileImpl.this.handleRefreshState(msg.obj);
                } else if (msg.what == 6) {
                    String name8 = "handleShowDetail";
                    QSTileImpl qSTileImpl = QSTileImpl.this;
                    if (msg.arg1 == 0) {
                        z = false;
                    }
                    qSTileImpl.handleShowDetail(z);
                } else if (msg.what == 15) {
                    String name9 = "handleShowEdit";
                    QSTileImpl qSTileImpl2 = QSTileImpl.this;
                    if (msg.arg1 == 0) {
                        z = false;
                    }
                    qSTileImpl2.handleShowEdit(z);
                } else if (msg.what == 7) {
                    String name10 = "handleUserSwitch";
                    QSTileImpl.this.handleUserSwitch(msg.arg1);
                } else if (msg.what == 8) {
                    String name11 = "handleToggleStateChanged";
                    QSTileImpl qSTileImpl3 = QSTileImpl.this;
                    if (msg.arg1 == 0) {
                        z = false;
                    }
                    qSTileImpl3.handleToggleStateChanged(z);
                } else if (msg.what == 9) {
                    String name12 = "handleScanStateChanged";
                    QSTileImpl qSTileImpl4 = QSTileImpl.this;
                    if (msg.arg1 == 0) {
                        z = false;
                    }
                    qSTileImpl4.handleScanStateChanged(z);
                } else if (msg.what == 10) {
                    String name13 = "handleDestroy";
                    QSTileImpl.this.handleDestroy();
                } else if (msg.what == 11) {
                    String name14 = "handleClearState";
                    QSTileImpl.this.handleClearState();
                } else if (msg.what == 14) {
                    String name15 = "handleSetListeningInternal";
                    QSTileImpl qSTileImpl5 = QSTileImpl.this;
                    Object obj = msg.obj;
                    if (msg.arg1 == 0) {
                        z = false;
                    }
                    qSTileImpl5.handleSetListeningInternal(obj, z);
                } else {
                    throw new IllegalArgumentException("Unknown msg: " + msg.what);
                }
            } catch (Throwable t) {
                String error = "Error in " + null;
                Log.w(QSTileImpl.this.TAG, error, t);
                QSTileImpl.this.mHost.warn(error, t);
            }
        }
    }

    public static class ResourceIcon extends QSTile.Icon {
        private static final SparseArray<QSTile.Icon> ICONS = new SparseArray<>();
        protected final int mResId;

        private ResourceIcon(int resId) {
            this.mResId = resId;
        }

        public static QSTile.Icon get(int resId) {
            QSTile.Icon icon = ICONS.get(resId);
            if (icon != null) {
                return icon;
            }
            QSTile.Icon icon2 = new ResourceIcon(resId);
            ICONS.put(resId, icon2);
            return icon2;
        }

        public Drawable getDrawable(Context context) {
            return context.getDrawable(this.mResId);
        }

        public Drawable getInvisibleDrawable(Context context) {
            return context.getDrawable(this.mResId);
        }

        public boolean equals(Object o) {
            return (o instanceof ResourceIcon) && ((ResourceIcon) o).mResId == this.mResId;
        }

        public String toString() {
            return String.format("ResourceIcon[resId=0x%08x]", new Object[]{Integer.valueOf(this.mResId)});
        }
    }

    public abstract Intent getLongClickIntent();

    public abstract int getMetricsCategory();

    /* access modifiers changed from: protected */
    public abstract void handleClick();

    /* access modifiers changed from: protected */
    public abstract void handleSetListening(boolean z);

    /* access modifiers changed from: protected */
    public abstract void handleUpdateState(TState tstate, Object obj);

    public abstract TState newTileState();

    protected QSTileImpl(QSHost host) {
        this.mHost = host;
        this.mContext = host.getContext();
    }

    public void setListening(Object listener, boolean listening) {
        this.mHandler.obtainMessage(14, listening, 0, listener).sendToTarget();
    }

    public String getTileSpec() {
        return this.mTileSpec;
    }

    public void setTileSpec(String tileSpec) {
        this.mTileSpec = tileSpec;
    }

    public QSIconView createTileView(Context context) {
        return new QSIconViewImpl(context);
    }

    public DetailAdapter getDetailAdapter() {
        return null;
    }

    /* access modifiers changed from: protected */
    public DetailAdapter createDetailAdapter() {
        throw new UnsupportedOperationException();
    }

    public boolean isAvailable() {
        return true;
    }

    public void addCallback(QSTile.Callback callback) {
        this.mHandler.obtainMessage(1, callback).sendToTarget();
    }

    public void removeCallback(QSTile.Callback callback) {
        this.mHandler.obtainMessage(13, callback).sendToTarget();
    }

    public void removeCallbacks() {
        this.mHandler.sendEmptyMessage(12);
    }

    public void click() {
        click(false);
    }

    public void click(boolean inCustomizer) {
        MetricsLoggerCompat.write(this.mContext, this.mMetricsLogger, populate(new LogMaker(925).setType(4)));
        AnalyticsHelper.trackQSTilesClick(getTileSpec(), inCustomizer);
        Log.d(this.TAG, "send click msg");
        Message.obtain(this.mHandler, 2, Boolean.valueOf(inCustomizer)).sendToTarget();
    }

    public void secondaryClick() {
        MetricsLoggerCompat.write(this.mContext, this.mMetricsLogger, populate(new LogMaker(926).setType(4)));
        AnalyticsHelper.trackQSTilesSecondaryClick(getTileSpec());
        Log.d(this.TAG, "send secondary click msg");
        this.mHandler.sendEmptyMessage(3);
    }

    public void longClick() {
        MetricsLoggerCompat.write(this.mContext, this.mMetricsLogger, populate(new LogMaker(366).setType(4)));
        AnalyticsHelper.trackQSTilesLongClick(getTileSpec());
        Log.d(this.TAG, "send long click msg");
        this.mHandler.sendEmptyMessage(4);
    }

    public LogMaker populate(LogMaker logMaker) {
        if (this.mState instanceof QSTile.BooleanState) {
            logMaker.addTaggedData(928, Integer.valueOf(((QSTile.BooleanState) this.mState).value ? 1 : 0));
        }
        return logMaker.setSubtype(getMetricsCategory()).addTaggedData(833, Integer.valueOf(this.mIsFullQs)).addTaggedData(927, Integer.valueOf(this.mHost.indexOf(this.mTileSpec)));
    }

    public void showDetail(boolean show) {
        this.mHandler.obtainMessage(6, show, 0).sendToTarget();
    }

    public void showEdit(boolean show) {
        this.mHandler.obtainMessage(15, show, 0).sendToTarget();
    }

    public void refreshState() {
        refreshState(null);
    }

    /* access modifiers changed from: protected */
    public final void refreshState(Object arg) {
        this.mHandler.obtainMessage(5, arg).sendToTarget();
    }

    public void clearState() {
        this.mHandler.sendEmptyMessage(11);
    }

    public void userSwitch(int newUserId) {
        this.mHandler.obtainMessage(7, newUserId, 0).sendToTarget();
    }

    public void fireToggleStateChanged(boolean state) {
        this.mHandler.obtainMessage(8, state, 0).sendToTarget();
    }

    public void fireScanStateChanged(boolean state) {
        this.mHandler.obtainMessage(9, state, 0).sendToTarget();
    }

    public void destroy() {
        this.mHandler.sendEmptyMessage(10);
    }

    public TState getState() {
        return this.mState;
    }

    public void setDetailListening(boolean listening) {
    }

    /* access modifiers changed from: private */
    public void handleAddCallback(QSTile.Callback callback) {
        this.mCallbacks.add(callback);
        callback.onStateChanged(this.mState);
    }

    /* access modifiers changed from: private */
    public void handleRemoveCallback(QSTile.Callback callback) {
        this.mCallbacks.remove(callback);
    }

    /* access modifiers changed from: private */
    public void handleRemoveCallbacks() {
        this.mCallbacks.clear();
    }

    /* access modifiers changed from: protected */
    public void handleSecondaryClick() {
        handleClick();
    }

    /* access modifiers changed from: protected */
    public void handleLongClick() {
        Intent intent = getLongClickIntent();
        if (intent != null) {
            ((ActivityStarter) Dependency.get(ActivityStarter.class)).postStartActivityDismissingKeyguard(intent, 0);
        }
    }

    /* access modifiers changed from: protected */
    public void handleClearState() {
        this.mTmpState = newTileState();
        this.mState = newTileState();
    }

    /* access modifiers changed from: protected */
    public void handleRefreshState(Object arg) {
        handleUpdateState(this.mTmpState, arg);
        if (this.mTmpState.copyTo(this.mState)) {
            handleStateChanged();
        }
    }

    private void handleStateChanged() {
        boolean delayAnnouncement = shouldAnnouncementBeDelayed();
        boolean z = false;
        if (this.mCallbacks.size() != 0) {
            TState state = newTileState();
            this.mState.copyTo(state);
            for (int i = 0; i < this.mCallbacks.size(); i++) {
                this.mCallbacks.get(i).onStateChanged(state);
            }
            if (this.mAnnounceNextStateChange != 0 && !delayAnnouncement) {
                String announcement = composeChangeAnnouncement();
                if (announcement != null) {
                    this.mCallbacks.get(0).onAnnouncementRequested(announcement);
                }
            }
        }
        if (this.mAnnounceNextStateChange && delayAnnouncement) {
            z = true;
        }
        this.mAnnounceNextStateChange = z;
    }

    /* access modifiers changed from: protected */
    public boolean shouldAnnouncementBeDelayed() {
        return false;
    }

    /* access modifiers changed from: protected */
    public String composeChangeAnnouncement() {
        return null;
    }

    /* access modifiers changed from: private */
    public void handleShowDetail(boolean show) {
        this.mShowingDetail = show;
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            this.mCallbacks.get(i).onShowDetail(show);
        }
    }

    /* access modifiers changed from: private */
    public void handleShowEdit(boolean show) {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            this.mCallbacks.get(i).onShowEdit(show);
        }
    }

    /* access modifiers changed from: protected */
    public boolean isShowingDetail() {
        return this.mShowingDetail;
    }

    /* access modifiers changed from: private */
    public void handleToggleStateChanged(boolean state) {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            this.mCallbacks.get(i).onToggleStateChanged(state);
        }
    }

    /* access modifiers changed from: private */
    public void handleScanStateChanged(boolean state) {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            this.mCallbacks.get(i).onScanStateChanged(state);
        }
    }

    /* access modifiers changed from: protected */
    public void handleUserSwitch(int newUserId) {
        handleRefreshState(null);
    }

    /* access modifiers changed from: private */
    public void handleSetListeningInternal(Object listener, boolean listening) {
        if (listening) {
            if (this.mListeners.add(listener) && (Util.isMiuiOptimizationDisabled() || this.mListeners.size() == 1)) {
                if (DEBUG) {
                    Log.d(this.TAG, "handleSetListening true");
                }
                handleSetListening(listening);
                refreshState();
            }
        } else if (this.mListeners.remove(listener) && (Util.isMiuiOptimizationDisabled() || this.mListeners.size() == 0)) {
            if (DEBUG) {
                Log.d(this.TAG, "handleSetListening false");
            }
            handleSetListening(listening);
        }
        updateIsFullQs();
    }

    private void updateIsFullQs() {
        Iterator<Object> it = this.mListeners.iterator();
        while (it.hasNext()) {
            if (PagedTileLayout.TilePage.class.equals(it.next().getClass())) {
                this.mIsFullQs = 1;
                return;
            }
        }
        this.mIsFullQs = 0;
    }

    /* access modifiers changed from: protected */
    public void handleDestroy() {
        if (this.mListeners.size() != 0) {
            handleSetListening(false);
        }
        this.mCallbacks.clear();
    }

    /* access modifiers changed from: protected */
    public void checkIfRestrictionEnforcedByAdminOnly(QSTile.State state, String userRestriction) {
        RestrictedLockUtils.EnforcedAdmin admin = RestrictedLockUtils.checkIfRestrictionEnforced(this.mContext, userRestriction, ActivityManager.getCurrentUser());
        if (admin == null || RestrictedLockUtils.hasBaseUserRestriction(this.mContext, userRestriction, ActivityManager.getCurrentUser())) {
            state.disabledByPolicy = false;
            this.mEnforcedAdmin = null;
            return;
        }
        state.disabledByPolicy = true;
        this.mEnforcedAdmin = admin;
    }
}
