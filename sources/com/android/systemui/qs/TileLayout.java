package com.android.systemui.qs;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import com.android.systemui.R;
import com.android.systemui.qs.QSPanel;
import java.util.ArrayList;
import java.util.Iterator;

public class TileLayout extends ViewGroup implements QSPanel.QSTileLayout {
    protected int mCellHeight;
    protected int mCellWidth;
    protected int mColumns;
    protected int mContentHeight;
    protected int mContentMarginBottom;
    protected int mContentMarginHorizontal;
    protected int mContentMarginTop;
    private boolean mListening;
    protected final ArrayList<QSPanel.TileRecord> mRecords;
    protected int mRows;

    public TileLayout(Context context) {
        this(context, null);
    }

    public TileLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mRecords = new ArrayList<>();
        setFocusableInTouchMode(true);
        updateResources();
    }

    public int getOffsetTop(QSPanel.TileRecord tile) {
        return getTop();
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
        this.mRecords.add(tile);
        tile.tile.setListening(this, this.mListening);
        addView(tile.tileView);
    }

    public void removeTile(QSPanel.TileRecord tile) {
        this.mRecords.remove(tile);
        tile.tile.setListening(this, false);
        removeView(tile.tileView);
    }

    public void removeAllViews() {
        Iterator<QSPanel.TileRecord> it = this.mRecords.iterator();
        while (it.hasNext()) {
            it.next().tile.setListening(this, false);
        }
        this.mRecords.clear();
        super.removeAllViews();
    }

    public boolean updateResources() {
        Resources res = this.mContext.getResources();
        int columns = Math.max(1, res.getInteger(R.integer.quick_settings_num_columns));
        int rows = Math.max(1, res.getInteger(R.integer.quick_settings_num_rows));
        int height = res.getDimensionPixelSize(R.dimen.qs_tile_content_height);
        this.mContentMarginTop = res.getDimensionPixelSize(R.dimen.qs_tile_content_margin_top);
        this.mContentMarginHorizontal = res.getDimensionPixelSize(R.dimen.qs_tile_content_margin_horizontal);
        this.mContentMarginBottom = res.getDimensionPixelSize(R.dimen.qs_tile_content_margin_bottom);
        if (this.mColumns == columns && this.mRows == rows && this.mContentHeight == height) {
            return false;
        }
        this.mColumns = columns;
        this.mRows = rows;
        this.mContentHeight = height;
        requestLayout();
        return true;
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = View.MeasureSpec.getSize(widthMeasureSpec);
        this.mCellWidth = (width - (this.mContentMarginHorizontal * 2)) / this.mColumns;
        this.mCellHeight = ((this.mContentHeight - this.mContentMarginBottom) - this.mContentMarginTop) / this.mRows;
        View previousView = this;
        Iterator<QSPanel.TileRecord> it = this.mRecords.iterator();
        while (it.hasNext()) {
            QSPanel.TileRecord record = it.next();
            if (record.tileView.getVisibility() != 8) {
                record.tileView.measure(exactly(this.mCellWidth), exactly(this.mCellHeight));
                previousView = record.tileView.updateAccessibilityOrder(previousView);
            }
        }
        setMeasuredDimension(width, this.mContentHeight);
    }

    private static int exactly(int size) {
        return View.MeasureSpec.makeMeasureSpec(size, 1073741824);
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        int right;
        int w = getWidth();
        int i = 0;
        boolean z = true;
        if (getLayoutDirection() != 1) {
            z = false;
        }
        boolean isRtl = z;
        int row = 0;
        int column = 0;
        while (i < this.mRecords.size()) {
            if (column == this.mColumns) {
                row++;
                column -= this.mColumns;
            }
            QSPanel.TileRecord record = this.mRecords.get(i);
            int left = getColumnStart(column);
            int top = getRowTop(row);
            if (isRtl) {
                right = w - left;
                left = right - this.mCellWidth;
            } else {
                right = this.mCellWidth + left;
            }
            record.tileView.layout(left, top, right, record.tileView.getMeasuredHeight() + top);
            i++;
            column++;
        }
    }

    private int getRowTop(int row) {
        return (this.mCellHeight * row) + this.mContentMarginTop;
    }

    private int getColumnStart(int column) {
        return (this.mCellWidth * column) + this.mContentMarginHorizontal;
    }
}
