package com.android.systemui.qs;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Space;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.plugins.qs.QSTileView;
import com.android.systemui.qs.QSPanel;
import com.android.systemui.tuner.TunerService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import miui.os.Build;

public class QuickQSPanel extends QSPanel {
    protected QSPanel mFullPanel;
    private int mMaxTiles;
    private final TunerService.Tunable mNumTiles = new TunerService.Tunable() {
        public void onTuningChanged(String key, String newValue) {
            QuickQSPanel.this.setMaxTiles(QuickQSPanel.getNumQuickTiles(QuickQSPanel.this.mContext));
        }
    };

    private static class HeaderTileLayout extends LinearLayout implements QSPanel.QSTileLayout {
        private int mContentPaddingBottom;
        private int mContentPaddingHorizontal;
        private int mContentPaddingTop;
        private boolean mListening;
        protected final ArrayList<QSPanel.TileRecord> mRecords = new ArrayList<>();

        public HeaderTileLayout(Context context) {
            super(context);
            setClipChildren(false);
            setClipToPadding(false);
            setGravity(16);
            updateResources();
        }

        public void setListening(boolean listening) {
            if (this.mListening != listening) {
                this.mListening = listening;
                Iterator<QSPanel.TileRecord> it = this.mRecords.iterator();
                while (it.hasNext()) {
                    it.next().tile.setListening(this, this.mListening);
                }
            }
        }

        public void addTile(QSPanel.TileRecord tile) {
            if (getChildCount() != 0) {
                addView(new Space(this.mContext), getChildCount(), generateSpaceParams());
            }
            addView(tile.tileView, getChildCount(), generateLayoutParams());
            this.mRecords.add(tile);
            tile.tile.setListening(this, this.mListening);
        }

        private LinearLayout.LayoutParams generateSpaceParams() {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, this.mContext.getResources().getDimensionPixelSize(R.dimen.qs_quick_tile_icon_bg_size));
            lp.weight = 1.0f;
            lp.gravity = 17;
            return lp;
        }

        private LinearLayout.LayoutParams generateLayoutParams() {
            int size = this.mContext.getResources().getDimensionPixelSize(R.dimen.qs_quick_tile_icon_bg_size);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.gravity = 17;
            return lp;
        }

        public void removeTile(QSPanel.TileRecord tile) {
            int childIndex = getChildIndex(tile.tileView);
            removeViewAt(childIndex);
            if (getChildCount() != 0) {
                removeViewAt(childIndex);
            }
            this.mRecords.remove(tile);
            tile.tile.setListening(this, false);
        }

        private int getChildIndex(QSTileView tileView) {
            int N = getChildCount();
            for (int i = 0; i < N; i++) {
                if (getChildAt(i) == tileView) {
                    return i;
                }
            }
            return -1;
        }

        public int getOffsetTop(QSPanel.TileRecord tile) {
            return 0;
        }

        public boolean updateResources() {
            Resources res = getResources();
            this.mContentPaddingHorizontal = res.getDimensionPixelSize(R.dimen.qs_quick_panel_content_padding_horizontal);
            this.mContentPaddingTop = res.getDimensionPixelSize(R.dimen.qs_quick_panel_content_padding_top);
            this.mContentPaddingBottom = res.getDimensionPixelSize(R.dimen.qs_quick_panel_content_padding_bottom);
            setPadding(this.mContentPaddingHorizontal, this.mContentPaddingTop, this.mContentPaddingHorizontal, this.mContentPaddingBottom);
            return true;
        }

        public boolean hasOverlappingRendering() {
            return false;
        }

        /* access modifiers changed from: protected */
        public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            if (this.mRecords != null && this.mRecords.size() > 0) {
                View previousView = this;
                Iterator<QSPanel.TileRecord> it = this.mRecords.iterator();
                while (it.hasNext()) {
                    QSPanel.TileRecord record = it.next();
                    if (record.tileView.getVisibility() != 8) {
                        previousView = record.tileView.updateAccessibilityOrder(previousView);
                    }
                }
                this.mRecords.get(0).tileView.setAccessibilityTraversalAfter(R.id.alarm_status_collapsed);
                this.mRecords.get(this.mRecords.size() - 1).tileView.setAccessibilityTraversalBefore(R.id.expand_indicator);
            }
        }
    }

    public QuickQSPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (this.mFooter != null) {
            removeView(this.mFooter.getView());
        }
        if (this.mTileLayout != null) {
            for (int i = 0; i < this.mRecords.size(); i++) {
                this.mTileLayout.removeTile((QSPanel.TileRecord) this.mRecords.get(i));
            }
            removeView((View) this.mTileLayout);
        }
        this.mTileLayout = new HeaderTileLayout(context);
        addView((View) this.mTileLayout, 0);
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        HeaderTileLayout tileLayout = (HeaderTileLayout) this.mTileLayout;
        tileLayout.measure(widthMeasureSpec, heightMeasureSpec);
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) getLayoutParams();
        super.onMeasure(widthMeasureSpec, View.MeasureSpec.makeMeasureSpec(layoutParams.topMargin + layoutParams.bottomMargin + tileLayout.getMeasuredHeight(), 1073741824));
    }

    public void setQSPanelAndHeader(QSPanel fullPanel, View header) {
        this.mFullPanel = fullPanel;
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mTileLayout.setListening(this.mListening);
        ((TunerService) Dependency.get(TunerService.class)).addTunable(this.mNumTiles, "sysui_qqs_count");
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mTileLayout.setListening(false);
        ((TunerService) Dependency.get(TunerService.class)).removeTunable(this.mNumTiles);
    }

    /* access modifiers changed from: protected */
    public boolean shouldShowDetail() {
        return !this.mExpanded;
    }

    /* access modifiers changed from: protected */
    public void drawTile(QSPanel.TileRecord r, QSTile.State state) {
        if (state instanceof QSTile.SignalState) {
            QSTile.SignalState copy = new QSTile.SignalState();
            state.copyTo(copy);
            copy.activityIn = false;
            copy.activityOut = false;
            state = copy;
        }
        super.drawTile(r, state);
    }

    public void setHost(QSTileHost host) {
        super.setHost(host);
        setTiles(this.mHost.getTiles());
    }

    public void setMaxTiles(int maxTiles) {
        this.mMaxTiles = maxTiles;
        if (this.mHost != null) {
            setTiles(this.mHost.getTiles());
        }
    }

    public void setTiles(Collection<QSTile> tiles) {
        ArrayList<QSTile> quickTiles = new ArrayList<>();
        for (QSTile tile : tiles) {
            quickTiles.add(tile);
            if (quickTiles.size() == this.mMaxTiles) {
                break;
            }
        }
        super.setTiles(quickTiles, true);
    }

    public static int getNumQuickTiles(Context context) {
        return ((TunerService) Dependency.get(TunerService.class)).getValue("sysui_qqs_count", Build.IS_TABLET ? 6 : 5);
    }
}
