package com.android.systemui.qs;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.systemui.R;
import com.android.systemui.qs.QSPanel;
import java.util.ArrayList;

public class PagedTileLayout extends ViewPager implements QSPanel.QSTileLayout {
    private final PagerAdapter mAdapter = new PagerAdapter() {
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        public Object instantiateItem(ViewGroup container, int position) {
            if (PagedTileLayout.this.isLayoutRtl()) {
                position = (PagedTileLayout.this.mPages.size() - 1) - position;
            }
            ViewGroup view = (ViewGroup) PagedTileLayout.this.mPages.get(position);
            container.addView(view);
            return view;
        }

        public int getCount() {
            return PagedTileLayout.this.mNumPages;
        }

        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }
    };
    private final Runnable mDistribute = new Runnable() {
        public void run() {
            PagedTileLayout.this.distributeTiles();
        }
    };
    private boolean mListening;
    /* access modifiers changed from: private */
    public int mNumPages;
    private boolean mOffPage;
    /* access modifiers changed from: private */
    public PageIndicator mPageIndicator;
    /* access modifiers changed from: private */
    public PageListener mPageListener;
    /* access modifiers changed from: private */
    public final ArrayList<TilePage> mPages = new ArrayList<>();
    private int mPosition;
    private final ArrayList<QSPanel.TileRecord> mTiles = new ArrayList<>();

    public interface PageListener {
        void onPageChanged(boolean z);
    }

    public static class TilePage extends TileLayout {
        public TilePage(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public boolean isFull() {
            return this.mRecords.size() >= this.mColumns * this.mRows;
        }
    }

    public PagedTileLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setAdapter(this.mAdapter);
        setOverScrollMode(2);
        setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            public void onPageSelected(int position) {
                if (!(PagedTileLayout.this.mPageIndicator == null || PagedTileLayout.this.mPageListener == null)) {
                    PageListener access$100 = PagedTileLayout.this.mPageListener;
                    boolean z = false;
                    if (!PagedTileLayout.this.isLayoutRtl() ? position == 0 : position == PagedTileLayout.this.mPages.size() - 1) {
                        z = true;
                    }
                    access$100.onPageChanged(z);
                }
            }

            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if (PagedTileLayout.this.mPageIndicator != null) {
                    boolean z = false;
                    PagedTileLayout.this.setCurrentPage(position, positionOffset != 0.0f);
                    PagedTileLayout.this.mPageIndicator.setLocation(((float) position) + positionOffset);
                    if (PagedTileLayout.this.mPageListener != null) {
                        PageListener access$100 = PagedTileLayout.this.mPageListener;
                        if (positionOffsetPixels == 0 && (!PagedTileLayout.this.isLayoutRtl() ? position == 0 : position == PagedTileLayout.this.mPages.size() - 1)) {
                            z = true;
                        }
                        access$100.onPageChanged(z);
                    }
                }
            }

            public void onPageScrollStateChanged(int state) {
            }
        });
        setCurrentItem(0);
    }

    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);
        setAdapter(this.mAdapter);
        setCurrentItem(0, false);
    }

    public void setCurrentItem(int item, boolean smoothScroll) {
        if (isLayoutRtl()) {
            item = (this.mPages.size() - 1) - item;
        }
        super.setCurrentItem(item, smoothScroll);
    }

    public void setListening(boolean listening) {
        if (this.mListening != listening) {
            this.mListening = listening;
            if (this.mListening) {
                setPageListening(this.mPosition, true);
                if (this.mOffPage) {
                    setPageListening(this.mPosition + 1, true);
                }
            } else {
                for (int i = 0; i < this.mPages.size(); i++) {
                    this.mPages.get(i).setListening(false);
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void setCurrentPage(int position, boolean offPage) {
        if (this.mPosition != position || this.mOffPage != offPage) {
            if (this.mListening) {
                if (this.mPosition != position) {
                    setPageListening(this.mPosition, false);
                    if (this.mOffPage) {
                        setPageListening(this.mPosition + 1, false);
                    }
                    setPageListening(position, true);
                    if (offPage) {
                        setPageListening(position + 1, true);
                    }
                } else if (this.mOffPage != offPage) {
                    setPageListening(this.mPosition + 1, offPage);
                }
            }
            this.mPosition = position;
            this.mOffPage = offPage;
        }
    }

    private void setPageListening(int position, boolean listening) {
        if (position < this.mPages.size()) {
            if (isLayoutRtl()) {
                position = (this.mPages.size() - 1) - position;
            }
            this.mPages.get(position).setListening(listening);
        }
    }

    public boolean hasOverlappingRendering() {
        return false;
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mPages.add((TilePage) LayoutInflater.from(getContext()).inflate(R.layout.qs_paged_page, this, false));
    }

    public void setPageIndicator(PageIndicator indicator) {
        this.mPageIndicator = indicator;
    }

    public int getOffsetTop(QSPanel.TileRecord tile) {
        ViewGroup parent = (ViewGroup) tile.tileView.getParent();
        if (parent == null) {
            return 0;
        }
        return parent.getTop() + getTop();
    }

    public void addTile(QSPanel.TileRecord tile) {
        this.mTiles.add(tile);
        postDistributeTiles();
    }

    public void removeTile(QSPanel.TileRecord tile) {
        if (this.mTiles.remove(tile)) {
            postDistributeTiles();
        }
    }

    public void setPageListener(PageListener listener) {
        this.mPageListener = listener;
    }

    private void postDistributeTiles() {
        removeCallbacks(this.mDistribute);
        post(this.mDistribute);
    }

    /* access modifiers changed from: private */
    public void distributeTiles() {
        int NP = this.mPages.size();
        for (int i = 0; i < NP; i++) {
            this.mPages.get(i).removeAllViews();
        }
        int NT = this.mTiles.size();
        int index = 0;
        for (int i2 = 0; i2 < NT; i2++) {
            QSPanel.TileRecord tile = this.mTiles.get(i2);
            if (this.mPages.get(index).isFull()) {
                index++;
                if (index == this.mPages.size()) {
                    this.mPages.add((TilePage) LayoutInflater.from(getContext()).inflate(R.layout.qs_paged_page, this, false));
                }
            }
            this.mPages.get(index).addTile(tile);
        }
        if (this.mNumPages != index + 1) {
            this.mNumPages = index + 1;
            while (this.mPages.size() > this.mNumPages) {
                this.mPages.remove(this.mPages.size() - 1);
            }
            this.mPageIndicator.setNumPages(this.mNumPages);
            this.mPageIndicator.setVisibility(this.mNumPages > 1 ? 0 : 8);
            setAdapter(this.mAdapter);
            this.mAdapter.notifyDataSetChanged();
            setCurrentItem(0, false);
        }
    }

    public boolean updateResources() {
        boolean changed = false;
        for (int i = 0; i < this.mPages.size(); i++) {
            changed |= this.mPages.get(i).updateResources();
        }
        if (changed) {
            distributeTiles();
        }
        return changed;
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int maxHeight = 0;
        int N = getChildCount();
        for (int i = 0; i < N; i++) {
            int height = getChildAt(i).getMeasuredHeight();
            if (height > maxHeight) {
                maxHeight = height;
            }
        }
        setMeasuredDimension(getMeasuredWidth(), getPaddingBottom() + maxHeight);
    }

    public int getColumnCount() {
        if (this.mPages.size() == 0) {
            return 0;
        }
        return this.mPages.get(0).mColumns;
    }
}
