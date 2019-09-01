package com.android.systemui.qs;

import android.content.ComponentName;
import android.content.Context;
import android.metrics.LogMaker;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsLoggerCompat;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.plugins.qs.DetailAdapter;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.plugins.qs.QSTileView;
import com.android.systemui.qs.QSDetail;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.customize.QSCustomizer;
import com.android.systemui.qs.external.CustomTile;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class QSPanel extends LinearLayout implements QSHost.Callback {
    protected final Context mContext;
    /* access modifiers changed from: private */
    public QSCustomizer.QSPanelCallback mCustomizerCallback;
    private QSDetail.QSPanelCallback mDetailCallback;
    /* access modifiers changed from: private */
    public Record mDetailRecord;
    private int mEditTopOffset;
    protected boolean mExpanded;
    protected QSSecurityFooter mFooter;
    /* access modifiers changed from: private */
    public final H mHandler;
    protected QSTileHost mHost;
    protected boolean mListening;
    private final MetricsLogger mMetricsLogger;
    private View mPageIndicator;
    protected final ArrayList<TileRecord> mRecords;
    protected QSTileLayout mTileLayout;

    private class H extends Handler {
        private H() {
        }

        public void handleMessage(Message msg) {
            boolean z = false;
            if (msg.what == 1) {
                QSPanel qSPanel = QSPanel.this;
                Record record = (Record) msg.obj;
                if (msg.arg1 != 0) {
                    z = true;
                }
                qSPanel.handleShowDetail(record, z);
            } else if (msg.what == 4) {
                QSPanel qSPanel2 = QSPanel.this;
                Record record2 = (Record) msg.obj;
                if (msg.arg1 != 0) {
                    z = true;
                }
                qSPanel2.handleShowEdit(record2, z);
            } else if (msg.what == 3) {
                QSPanel.this.announceForAccessibility((CharSequence) msg.obj);
            }
        }
    }

    public interface QSTileLayout {
        void addTile(TileRecord tileRecord);

        int getOffsetTop(TileRecord tileRecord);

        void removeTile(TileRecord tileRecord);

        void setListening(boolean z);

        boolean updateResources();
    }

    protected static class Record {
        DetailAdapter detailAdapter;
        int x;
        int y;

        protected Record() {
        }
    }

    public static final class TileRecord extends Record {
        public QSTile.Callback callback;
        public boolean scanState;
        public QSTile tile;
        public QSTileView tileView;
    }

    public QSPanel(Context context) {
        this(context, null);
    }

    public QSPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mRecords = new ArrayList<>();
        this.mHandler = new H();
        this.mMetricsLogger = (MetricsLogger) Dependency.get(MetricsLogger.class);
        this.mContext = context;
        this.mEditTopOffset = getResources().getDimensionPixelSize(R.dimen.qs_detail_margin_top);
        initViews();
    }

    /* access modifiers changed from: protected */
    public void initViews() {
        setOrientation(1);
        setupTileLayout();
        setupPageIndicator();
        setupFooter();
        updateResources(false);
    }

    public View getPageIndicator() {
        return this.mPageIndicator;
    }

    /* access modifiers changed from: protected */
    public void setupTileLayout() {
        this.mTileLayout = (QSTileLayout) LayoutInflater.from(this.mContext).inflate(R.layout.qs_paged_tile_layout, this, false);
        this.mTileLayout.setListening(this.mListening);
        addView((View) this.mTileLayout);
    }

    /* access modifiers changed from: protected */
    public void setupPageIndicator() {
        this.mPageIndicator = LayoutInflater.from(this.mContext).inflate(R.layout.qs_page_indicator, this, false);
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) this.mPageIndicator.getLayoutParams();
        layoutParams.bottomMargin = this.mContext.getResources().getDimensionPixelSize(R.dimen.qs_page_indicator_dot_bottom_margin);
        addView(this.mPageIndicator, layoutParams);
        if (this.mTileLayout instanceof PagedTileLayout) {
            ((PagedTileLayout) this.mTileLayout).setPageIndicator((PageIndicator) this.mPageIndicator);
        }
    }

    /* access modifiers changed from: protected */
    public void setupFooter() {
        this.mFooter = new QSSecurityFooter(this, this.mContext);
        addView(this.mFooter.getView());
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (this.mHost != null) {
            setTiles(this.mHost.getTiles());
        }
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        if (this.mHost != null) {
            this.mHost.removeCallback(this);
        }
        Iterator<TileRecord> it = this.mRecords.iterator();
        while (it.hasNext()) {
            it.next().tile.removeCallbacks();
        }
        super.onDetachedFromWindow();
    }

    public void onTilesChanged() {
        setTiles(this.mHost.getTiles());
    }

    public void openDetails(String subPanel) {
        showDetailAdapter(true, getTile(subPanel).getDetailAdapter(), new int[]{getWidth() / 2, 0});
    }

    private QSTile getTile(String subPanel) {
        for (int i = 0; i < this.mRecords.size(); i++) {
            if (subPanel.equals(this.mRecords.get(i).tile.getTileSpec())) {
                return this.mRecords.get(i).tile;
            }
        }
        return this.mHost.createTile(subPanel);
    }

    public void setQSDetailCallback(QSDetail.QSPanelCallback callback) {
        this.mDetailCallback = callback;
    }

    public void setQSCustomizerCallback(QSCustomizer.QSPanelCallback callback) {
        this.mCustomizerCallback = callback;
    }

    public void setHost(QSTileHost host) {
        this.mHost = host;
        this.mHost.addCallback(this);
        setTiles(this.mHost.getTiles());
        this.mFooter.setHostEnvironment(host);
    }

    public QSTileHost getHost() {
        return this.mHost;
    }

    public void updateResources(boolean isThemeChanged) {
        this.mFooter.onConfigurationChanged();
        this.mEditTopOffset = getResources().getDimensionPixelSize(R.dimen.qs_detail_margin_top);
        Iterator<TileRecord> it = this.mRecords.iterator();
        while (it.hasNext()) {
            TileRecord r = it.next();
            r.tileView.getIcon().updateResources();
            r.tile.clearState();
        }
        if (this.mListening) {
            refreshAllTiles();
        }
        if (this.mTileLayout != null) {
            this.mTileLayout.updateResources();
        }
    }

    public void setExpanded(boolean expanded) {
        if (this.mExpanded != expanded) {
            this.mExpanded = expanded;
            if (!this.mExpanded && (this.mTileLayout instanceof PagedTileLayout)) {
                ((PagedTileLayout) this.mTileLayout).setCurrentItem(0, false);
            }
            MetricsLogger.visibility(getContext(), 111, this.mExpanded);
            if (!this.mExpanded) {
                closeDetail(false);
            } else {
                logTiles();
            }
        }
    }

    public void setListening(boolean listening) {
        if (this.mListening != listening) {
            this.mListening = listening;
            if (this.mTileLayout != null) {
                this.mTileLayout.setListening(listening);
            }
            this.mFooter.setListening(this.mListening);
            if (this.mListening) {
                refreshAllTiles();
            }
        }
    }

    public void refreshAllTiles() {
        Iterator<TileRecord> it = this.mRecords.iterator();
        while (it.hasNext()) {
            it.next().tile.refreshState();
        }
        this.mFooter.refreshState();
    }

    public void showDetailAdapter(boolean show, DetailAdapter adapter, int[] locationInWindow) {
        int xInWindow = locationInWindow[0];
        int yInWindow = locationInWindow[1];
        ((View) getParent()).getLocationInWindow(locationInWindow);
        Record r = new Record();
        r.detailAdapter = adapter;
        r.x = xInWindow - locationInWindow[0];
        r.y = yInWindow - locationInWindow[1];
        locationInWindow[0] = xInWindow;
        locationInWindow[1] = yInWindow;
        showDetail(show, r);
    }

    /* access modifiers changed from: protected */
    public void showDetail(boolean show, Record r) {
        this.mHandler.obtainMessage(1, show, 0, r).sendToTarget();
    }

    /* access modifiers changed from: protected */
    public void showEdit(boolean show, Record r) {
        this.mHandler.obtainMessage(4, show, 0, r).sendToTarget();
    }

    public void setTiles(Collection<QSTile> tiles) {
        setTiles(tiles, false);
    }

    public void setTiles(Collection<QSTile> tiles, boolean collapsedView) {
        Iterator<TileRecord> it = this.mRecords.iterator();
        while (it.hasNext()) {
            TileRecord record = it.next();
            this.mTileLayout.removeTile(record);
            record.tile.removeCallback(record.callback);
        }
        this.mRecords.clear();
        for (QSTile tile : tiles) {
            addTile(tile, collapsedView);
        }
    }

    /* access modifiers changed from: protected */
    public void drawTile(TileRecord r, QSTile.State state) {
        r.tileView.onStateChanged(state);
    }

    /* access modifiers changed from: protected */
    public QSTileView createTileView(QSTile tile, boolean collapsedView) {
        return this.mHost.createTileView(tile, collapsedView);
    }

    /* access modifiers changed from: protected */
    public boolean shouldShowDetail() {
        return this.mExpanded;
    }

    /* access modifiers changed from: protected */
    public TileRecord addTile(QSTile tile, boolean collapsedView) {
        final TileRecord r = new TileRecord();
        r.tile = tile;
        r.tileView = createTileView(tile, collapsedView);
        QSTile.Callback callback = new QSTile.Callback() {
            public void onStateChanged(QSTile.State state) {
                QSPanel.this.drawTile(r, state);
            }

            public void onShowDetail(boolean show) {
                if (QSPanel.this.shouldShowDetail() || !show) {
                    QSPanel.this.showDetail(show, r);
                }
            }

            public void onShowEdit(boolean show) {
                QSPanel.this.showEdit(show, r);
            }

            public void onToggleStateChanged(boolean state) {
                if (QSPanel.this.mDetailRecord == r) {
                    QSPanel.this.fireToggleStateChanged(state);
                }
            }

            public void onScanStateChanged(boolean state) {
                r.scanState = state;
                if (QSPanel.this.mDetailRecord == r) {
                    QSPanel.this.fireScanStateChanged(r.scanState);
                }
            }

            public void onAnnouncementRequested(CharSequence announcement) {
                if (announcement != null) {
                    QSPanel.this.mHandler.obtainMessage(3, announcement).sendToTarget();
                }
            }
        };
        r.tile.addCallback(callback);
        r.callback = callback;
        r.tileView.init(r.tile);
        r.tile.refreshState();
        this.mRecords.add(r);
        if (this.mTileLayout != null) {
            this.mTileLayout.addTile(r);
        }
        return r;
    }

    public void showEdit(final View v) {
        v.post(new Runnable() {
            public void run() {
                if (QSPanel.this.mCustomizerCallback != null) {
                    int[] loc = new int[2];
                    v.getLocationInWindow(loc);
                    QSPanel.this.mCustomizerCallback.show(loc[0] + (v.getWidth() / 2), loc[1] + (v.getHeight() / 2));
                }
            }
        });
    }

    public void closeDetail(boolean animate) {
        if (this.mCustomizerCallback != null) {
            this.mCustomizerCallback.hide(0, 0, animate);
        }
        if (this.mDetailRecord == null || !(this.mDetailRecord instanceof TileRecord)) {
            showDetail(false, this.mDetailRecord);
            return;
        }
        QSTile tile = ((TileRecord) this.mDetailRecord).tile;
        if (tile instanceof QSTileImpl) {
            ((QSTileImpl) tile).showDetail(false);
        }
    }

    /* access modifiers changed from: protected */
    public void handleShowDetail(Record r, boolean show) {
        if (r instanceof TileRecord) {
            handleShowDetailTile((TileRecord) r, show);
            return;
        }
        int x = 0;
        int y = 0;
        if (r != null) {
            x = r.x;
            y = r.y;
        }
        handleShowDetailImpl(r, show, x, y);
    }

    /* access modifiers changed from: protected */
    public void handleShowEdit(Record r, boolean show) {
        if (r instanceof TileRecord) {
            handleShowEditTile((TileRecord) r);
            return;
        }
        int x = 0;
        int y = 0;
        if (r != null) {
            x = r.x;
            y = r.y;
        }
        fireShowingEdit(x, y);
    }

    private void handleShowDetailTile(TileRecord r, boolean show) {
        if ((this.mDetailRecord != null) != show || this.mDetailRecord != r) {
            if (show) {
                r.detailAdapter = r.tile.getDetailAdapter();
                if (r.detailAdapter == null) {
                    return;
                }
            }
            r.tile.setDetailListening(show);
            handleShowDetailImpl(r, show, r.tileView.getLeft() + (r.tileView.getWidth() / 2), r.tileView.getDetailY() + this.mTileLayout.getOffsetTop(r) + getTop() + this.mEditTopOffset);
        }
    }

    private void handleShowEditTile(TileRecord r) {
        fireShowingEdit(r.tileView.getLeft() + (r.tileView.getWidth() / 2), r.tileView.getDetailY() + this.mTileLayout.getOffsetTop(r) + getTop() + this.mEditTopOffset);
    }

    private void handleShowDetailImpl(Record r, boolean show, int x, int y) {
        DetailAdapter detailAdapter = null;
        setDetailRecord(show ? r : null);
        if (show) {
            detailAdapter = r.detailAdapter;
        }
        fireShowingDetail(detailAdapter, x, y);
    }

    /* access modifiers changed from: protected */
    public void setDetailRecord(Record r) {
        if (r != this.mDetailRecord) {
            this.mDetailRecord = r;
            fireScanStateChanged((this.mDetailRecord instanceof TileRecord) && ((TileRecord) this.mDetailRecord).scanState);
        }
    }

    private void logTiles() {
        for (int i = 0; i < this.mRecords.size(); i++) {
            MetricsLoggerCompat.write(this.mContext, this.mMetricsLogger, new LogMaker(this.mRecords.get(i).tile.getMetricsCategory()).setType(1));
        }
    }

    private void fireShowingDetail(DetailAdapter detail, int x, int y) {
        if (this.mDetailCallback != null) {
            this.mDetailCallback.onShowingDetail(detail, x, y);
        }
    }

    private void fireShowingEdit(int x, int y) {
        if (this.mCustomizerCallback != null) {
            this.mCustomizerCallback.show(x, y);
        }
    }

    /* access modifiers changed from: private */
    public void fireToggleStateChanged(boolean state) {
        if (this.mDetailCallback != null) {
            this.mDetailCallback.onToggleStateChanged(state);
        }
    }

    /* access modifiers changed from: private */
    public void fireScanStateChanged(boolean state) {
        if (this.mDetailCallback != null) {
            this.mDetailCallback.onScanStateChanged(state);
        }
    }

    public void clickTile(ComponentName tile) {
        String spec = CustomTile.toSpec(tile);
        int N = this.mRecords.size();
        for (int i = 0; i < N; i++) {
            if (this.mRecords.get(i).tile.getTileSpec().equals(spec)) {
                this.mRecords.get(i).tile.click();
                return;
            }
        }
    }

    /* access modifiers changed from: package-private */
    public QSTileLayout getTileLayout() {
        return this.mTileLayout;
    }

    /* access modifiers changed from: package-private */
    public QSTileView getTileView(QSTile tile) {
        Iterator<TileRecord> it = this.mRecords.iterator();
        while (it.hasNext()) {
            TileRecord r = it.next();
            if (r.tile == tile) {
                return r.tileView;
            }
        }
        return null;
    }

    public QSSecurityFooter getFooter() {
        return this.mFooter;
    }

    public void showDeviceMonitoringDialog() {
        this.mFooter.showDeviceMonitoringDialog();
    }
}
