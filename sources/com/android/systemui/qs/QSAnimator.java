package com.android.systemui.qs;

import android.util.Log;
import android.view.View;
import com.android.systemui.Dependency;
import com.android.systemui.plugins.qs.QS;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.plugins.qs.QSTileView;
import com.android.systemui.qs.PagedTileLayout;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.QSPanel;
import com.android.systemui.qs.TouchAnimator;
import com.android.systemui.tuner.TunerService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class QSAnimator implements View.OnAttachStateChangeListener, View.OnLayoutChangeListener, PagedTileLayout.PageListener, QSHost.Callback, TouchAnimator.Listener, TunerService.Tunable {
    private final ArrayList<View> mAllViews = new ArrayList<>();
    private boolean mAllowFancy;
    private TouchAnimator mFirstPageAnimator;
    private TouchAnimator mFirstPageDelayedAnimator;
    private boolean mFullRows;
    private QSTileHost mHost;
    /* access modifiers changed from: private */
    public float mLastPosition;
    private final TouchAnimator.Listener mNonFirstPageListener = new TouchAnimator.ListenerAdapter() {
        public void onAnimationAtEnd() {
            QSAnimator.this.mQuickQsPanel.setVisibility(4);
        }

        public void onAnimationStarted() {
            QSAnimator.this.mQuickQsPanel.setVisibility(0);
        }
    };
    private TouchAnimator mNonfirstPageAnimator;
    private int mNumQuickTiles;
    private boolean mOnFirstPage = true;
    private boolean mOnKeyguard;
    private PagedTileLayout mPagedLayout;
    private final QS mQs;
    private final QSPanel mQsPanel;
    /* access modifiers changed from: private */
    public final QuickQSPanel mQuickQsPanel;
    private final ArrayList<View> mTopFiveQs = new ArrayList<>();
    private TouchAnimator mTranslationXAnimator;
    private TouchAnimator mTranslationYAnimator;
    private Runnable mUpdateAnimators = new Runnable() {
        public void run() {
            QSAnimator.this.updateAnimators();
            QSAnimator.this.setPosition(QSAnimator.this.mLastPosition);
        }
    };

    public QSAnimator(QS qs, QuickQSPanel quickPanel, QSPanel panel) {
        this.mQs = qs;
        this.mQuickQsPanel = quickPanel;
        this.mQsPanel = panel;
        this.mQsPanel.addOnAttachStateChangeListener(this);
        qs.getView().addOnLayoutChangeListener(this);
        if (this.mQsPanel.isAttachedToWindow()) {
            onViewAttachedToWindow(null);
        }
        QSPanel.QSTileLayout tileLayout = this.mQsPanel.getTileLayout();
        if (tileLayout instanceof PagedTileLayout) {
            this.mPagedLayout = (PagedTileLayout) tileLayout;
            this.mPagedLayout.setPageListener(this);
            return;
        }
        Log.w("QSAnimator", "QS Not using page layout");
    }

    public void onRtlChanged() {
        updateAnimators();
    }

    public void setOnKeyguard(boolean onKeyguard) {
        this.mOnKeyguard = onKeyguard;
        this.mQuickQsPanel.setVisibility(this.mOnKeyguard ? 4 : 0);
        if (this.mOnKeyguard) {
            clearAnimationState();
        }
    }

    public void setHost(QSTileHost qsh) {
        this.mHost = qsh;
        qsh.addCallback(this);
        updateAnimators();
    }

    public void onViewAttachedToWindow(View v) {
        ((TunerService) Dependency.get(TunerService.class)).addTunable(this, "sysui_qs_fancy_anim", "sysui_qs_move_whole_rows", "sysui_qqs_count");
    }

    public void onViewDetachedFromWindow(View v) {
        if (this.mHost != null) {
            this.mHost.removeCallback(this);
        }
        ((TunerService) Dependency.get(TunerService.class)).removeTunable(this);
    }

    public void onTuningChanged(String key, String newValue) {
        boolean z = true;
        if ("sysui_qs_fancy_anim".equals(key)) {
            if (newValue != null && Integer.parseInt(newValue) == 0) {
                z = false;
            }
            this.mAllowFancy = z;
            if (!this.mAllowFancy) {
                clearAnimationState();
            }
        } else if ("sysui_qs_move_whole_rows".equals(key)) {
            if (newValue != null && Integer.parseInt(newValue) == 0) {
                z = false;
            }
            this.mFullRows = z;
        } else if ("sysui_qqs_count".equals(key)) {
            QuickQSPanel quickQSPanel = this.mQuickQsPanel;
            this.mNumQuickTiles = QuickQSPanel.getNumQuickTiles(this.mQs.getContext());
            clearAnimationState();
        }
        updateAnimators();
    }

    public void onPageChanged(boolean isFirst) {
        if (this.mOnFirstPage != isFirst) {
            if (!isFirst) {
                clearAnimationState();
            }
            this.mOnFirstPage = isFirst;
        }
    }

    /* access modifiers changed from: private */
    public void updateAnimators() {
        Iterator<QSTile> it;
        int height;
        int lastXDiff;
        int height2;
        int[] loc2;
        int[] loc1;
        TouchAnimator.Builder firstPageBuilder = new TouchAnimator.Builder();
        TouchAnimator.Builder translationXBuilder = new TouchAnimator.Builder();
        TouchAnimator.Builder translationYBuilder = new TouchAnimator.Builder();
        if (this.mQsPanel.getHost() != null) {
            Collection<QSTile> tiles = this.mQsPanel.getHost().getTiles();
            int[] loc12 = new int[2];
            int[] loc22 = new int[2];
            int lastXDiff2 = false;
            clearAnimationState();
            this.mAllViews.clear();
            this.mTopFiveQs.clear();
            QSPanel.QSTileLayout tileLayout = this.mQsPanel.getTileLayout();
            this.mAllViews.add((View) tileLayout);
            int height3 = this.mQs.getView() != null ? this.mQs.getView().getMeasuredHeight() : 0;
            int heightDiff = (height3 - this.mQs.getHeader().getBottom()) + this.mQs.getHeader().getPaddingBottom();
            firstPageBuilder.addFloat(tileLayout, "translationY", (float) heightDiff, 0.0f);
            Iterator<QSTile> it2 = tiles.iterator();
            int lastFirstRowTileYDiff = 0;
            int count = 0;
            while (it2.hasNext()) {
                QSTile tile = it2.next();
                QSTileView tileView = this.mQsPanel.getTileView(tile);
                if (tileView == null) {
                    lastXDiff = lastXDiff2;
                    height = height3;
                    StringBuilder sb = new StringBuilder();
                    it = it2;
                    sb.append("tileView is null ");
                    sb.append(tile.getTileSpec());
                    Log.e("QSAnimator", sb.toString());
                } else {
                    lastXDiff = lastXDiff2;
                    height = height3;
                    it = it2;
                    View tileIcon = tileView.getIcon().getIconView();
                    View view = this.mQs.getView();
                    if (count >= this.mNumQuickTiles || !this.mAllowFancy) {
                        loc1 = loc12;
                        loc2 = loc22;
                        View view2 = tileIcon;
                        View view3 = view;
                    } else {
                        QSTileView quickTileView = this.mQuickQsPanel.getTileView(tile);
                        if (quickTileView != null) {
                            int lastX = loc12[0];
                            QSTile qSTile = tile;
                            getRelativePosition(loc12, quickTileView.getIcon().getIconView(), view);
                            getRelativePosition(loc22, tileIcon, view);
                            int xDiff = loc22[0] - loc12[0];
                            loc1 = loc12;
                            int yDiff = loc22[1] - loc12[1];
                            loc2 = loc22;
                            if (count < this.mPagedLayout.getColumnCount()) {
                                View view4 = tileIcon;
                                View view5 = view;
                                translationXBuilder.addFloat(quickTileView, "translationX", 0.0f, (float) xDiff);
                                translationYBuilder.addFloat(quickTileView, "translationY", 0.0f, (float) yDiff);
                                translationXBuilder.addFloat(tileView, "translationX", (float) (-xDiff), 0.0f);
                                lastFirstRowTileYDiff = yDiff;
                                this.mTopFiveQs.add(tileView.getIcon());
                            } else {
                                View view6 = view;
                                translationXBuilder.addFloat(quickTileView, "translationX", 0.0f, ((float) this.mQsPanel.getWidth()) - quickTileView.getX());
                                translationYBuilder.addFloat(quickTileView, "translationY", 0.0f, (float) lastFirstRowTileYDiff);
                                firstPageBuilder.addFloat(quickTileView, "alpha", 1.0f, 0.0f, 0.0f);
                            }
                            this.mAllViews.add(tileView.getIcon());
                            this.mAllViews.add(quickTileView);
                        }
                    }
                    this.mAllViews.add(tileView);
                    count++;
                    lastXDiff2 = lastXDiff;
                    height2 = height;
                    it2 = it;
                    loc12 = loc1;
                    loc22 = loc2;
                }
                lastXDiff2 = lastXDiff;
                height2 = height;
                it2 = it;
            }
            int[] iArr = loc22;
            int i = lastXDiff2;
            int i2 = height3;
            if (this.mAllowFancy) {
                this.mFirstPageAnimator = firstPageBuilder.setListener(this).build();
                this.mFirstPageDelayedAnimator = new TouchAnimator.Builder().setStartDelay(0.5f).addFloat(tileLayout, "alpha", 0.0f, 1.0f).addFloat(this.mQsPanel.getPageIndicator(), "alpha", 0.0f, 1.0f).addFloat(this.mQsPanel.getFooter().getView(), "alpha", 0.0f, 1.0f).build();
                this.mAllViews.add(this.mQsPanel.getPageIndicator());
                this.mAllViews.add(this.mQsPanel.getFooter().getView());
                float px = 0.0f;
                if (tiles.size() <= 3) {
                    px = 1.0f;
                } else if (tiles.size() <= 6) {
                    px = 0.4f;
                }
                PathInterpolatorBuilder interpolatorBuilder = new PathInterpolatorBuilder(0.0f, 0.0f, px, 1.0f);
                translationXBuilder.setInterpolator(interpolatorBuilder.getXInterpolator());
                translationYBuilder.setInterpolator(interpolatorBuilder.getYInterpolator());
                this.mTranslationXAnimator = translationXBuilder.build();
                this.mTranslationYAnimator = translationYBuilder.build();
            }
            this.mNonfirstPageAnimator = new TouchAnimator.Builder().addFloat(this.mQuickQsPanel, "alpha", 1.0f, 0.0f, 0.0f).addFloat(this.mQsPanel.getPageIndicator(), "alpha", 0.0f, 0.0f, 1.0f).addFloat(tileLayout, "translationY", (float) heightDiff, 0.0f).addFloat(tileLayout, "alpha", 0.0f, 0.0f, 1.0f).setListener(this.mNonFirstPageListener).build();
        }
    }

    private void getRelativePosition(int[] loc1, View view, View parent) {
        loc1[0] = (view.getWidth() / 2) + 0;
        loc1[1] = 0;
        getRelativePositionInt(loc1, view, parent);
    }

    private void getRelativePositionInt(int[] loc1, View view, View parent) {
        if (view != parent && view != null) {
            if (!(view instanceof PagedTileLayout.TilePage)) {
                loc1[0] = loc1[0] + view.getLeft();
                loc1[1] = loc1[1] + view.getTop();
            }
            getRelativePositionInt(loc1, (View) view.getParent(), parent);
        }
    }

    public void setPosition(float position) {
        if (this.mFirstPageAnimator != null && !this.mOnKeyguard) {
            this.mLastPosition = position;
            if (!this.mOnFirstPage || !this.mAllowFancy) {
                this.mNonfirstPageAnimator.setPosition(position);
            } else {
                this.mQuickQsPanel.setAlpha(1.0f);
                this.mFirstPageAnimator.setPosition(position);
                this.mFirstPageDelayedAnimator.setPosition(position);
                this.mTranslationXAnimator.setPosition(position);
                this.mTranslationYAnimator.setPosition(position);
            }
        }
    }

    public void onAnimationAtStart() {
        this.mQuickQsPanel.setVisibility(0);
    }

    public void onAnimationAtEnd() {
        this.mQuickQsPanel.setVisibility(4);
        int N = this.mTopFiveQs.size();
        for (int i = 0; i < N; i++) {
            this.mTopFiveQs.get(i).setVisibility(0);
        }
    }

    public void onAnimationStarted() {
        int i = 0;
        this.mQuickQsPanel.setVisibility(this.mOnKeyguard ? 4 : 0);
        if (this.mOnFirstPage) {
            int N = this.mTopFiveQs.size();
            while (true) {
                int i2 = i;
                if (i2 < N) {
                    this.mTopFiveQs.get(i2).setVisibility(4);
                    i = i2 + 1;
                } else {
                    return;
                }
            }
        }
    }

    private void clearAnimationState() {
        int N = this.mAllViews.size();
        this.mQuickQsPanel.setAlpha(0.0f);
        for (int i = 0; i < N; i++) {
            View v = this.mAllViews.get(i);
            v.setAlpha(1.0f);
            v.setTranslationX(0.0f);
            v.setTranslationY(0.0f);
        }
        int N2 = this.mTopFiveQs.size();
        for (int i2 = 0; i2 < N2; i2++) {
            this.mTopFiveQs.get(i2).setVisibility(0);
        }
    }

    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        this.mQsPanel.post(this.mUpdateAnimators);
    }

    public void onTilesChanged() {
        this.mQsPanel.post(this.mUpdateAnimators);
    }
}
