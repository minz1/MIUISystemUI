package com.android.systemui.qs.customize;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.AnalyticsHelper;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.miui.anim.AnimatorListenerWrapper;
import com.android.systemui.plugins.qs.QS;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSAnimation;
import com.android.systemui.qs.QSDetailClipper;
import com.android.systemui.qs.QSPanel;
import com.android.systemui.qs.QSTileHost;
import com.android.systemui.qs.customize.TileQueryHelper;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class QSCustomizer extends LinearLayout implements TileQueryHelper.TileStateListener {
    /* access modifiers changed from: private */
    public boolean isShown;
    private Animator.AnimatorListener mAnimInListener = this.mExpandAnimationListener;
    /* access modifiers changed from: private */
    public Animator.AnimatorListener mAnimOutListener = this.mCollapseAnimationListener;
    /* access modifiers changed from: private */
    public final QSDetailClipper mClipper = new QSDetailClipper(this);
    private final Animator.AnimatorListener mCollapseAnimationListener = new AnimatorListenerAdapter() {
        public void onAnimationEnd(Animator animation) {
            if (!QSCustomizer.this.isShown) {
                QSCustomizer.this.setVisibility(8);
            }
            QSCustomizer.this.setCustomizerAnimating(false);
            QSCustomizer.this.mRecyclerView.setAdapter(QSCustomizer.this.mTileAdapter);
        }

        public void onAnimationCancel(Animator animation) {
            if (!QSCustomizer.this.isShown) {
                QSCustomizer.this.setVisibility(8);
            }
            QSCustomizer.this.setCustomizerAnimating(false);
        }
    };
    private int mCount;
    private boolean mCustomizerAnimating;
    private boolean mCustomizing;
    protected TextView mDoneButton;
    private final Animator.AnimatorListener mExpandAnimationListener = new AnimatorListenerAdapter() {
        public void onAnimationEnd(Animator animation) {
            if (QSCustomizer.this.isShown) {
                QSCustomizer.this.setCustomizing(true);
            }
            boolean unused = QSCustomizer.this.mOpening = false;
            QSCustomizer.this.setCustomizerAnimating(false);
        }

        public void onAnimationCancel(Animator animation) {
            boolean unused = QSCustomizer.this.mOpening = false;
            QSCustomizer.this.setCustomizerAnimating(false);
        }
    };
    protected RelativeLayout mHeader;
    private QSTileHost mHost;
    /* access modifiers changed from: private */
    public final KeyguardMonitor.Callback mKeyguardCallback = new KeyguardMonitor.Callback() {
        public void onKeyguardShowingChanged() {
            if (QSCustomizer.this.isAttachedToWindow() && ((KeyguardMonitor) Dependency.get(KeyguardMonitor.class)).isShowing() && !QSCustomizer.this.mOpening) {
                QSCustomizer.this.mQsPanelCallback.hide(0, 0, false);
            }
        }
    };
    private ViewGroup mNotifQsContainer;
    /* access modifiers changed from: private */
    public boolean mOpening;
    private QS mQs;
    protected QSPanelCallback mQsPanelCallback = new QSPanelCallback() {
        public void show(int x, int y) {
            if (!QSCustomizer.this.isShown) {
                int unused = QSCustomizer.this.mX = x;
                int unused2 = QSCustomizer.this.mY = y;
                MetricsLogger.visible(QSCustomizer.this.getContext(), 358);
                boolean unused3 = QSCustomizer.this.isShown = true;
                boolean unused4 = QSCustomizer.this.mShownRequested = true;
                boolean unused5 = QSCustomizer.this.mOpening = true;
                QSCustomizer.this.setTileSpecs();
                QSCustomizer.this.queryTiles();
                QSCustomizer.this.setCustomizerAnimating(true);
                QSCustomizer.this.announceForAccessibility(QSCustomizer.this.mContext.getString(R.string.accessibility_desc_quick_settings_edit));
                ((KeyguardMonitor) Dependency.get(KeyguardMonitor.class)).addCallback(QSCustomizer.this.mKeyguardCallback);
            }
        }

        public void hide(int x, int y, boolean animate) {
            if (QSCustomizer.this.isShown) {
                MetricsLogger.hidden(QSCustomizer.this.getContext(), 358);
                boolean unused = QSCustomizer.this.isShown = false;
                boolean unused2 = QSCustomizer.this.mShownRequested = false;
                QSCustomizer.this.setCustomizing(false);
                QSCustomizer.this.save();
                if (animate) {
                    QSCustomizer.this.mClipper.animateCircularClip(QSCustomizer.this.mX, QSCustomizer.this.mY, false, QSCustomizer.this.mAnimOutListener);
                } else {
                    QSCustomizer.this.setAlpha(0.0f);
                    QSCustomizer.this.mAnimOutListener.onAnimationEnd(null);
                }
                QSCustomizer.this.releaseTiles();
                QSCustomizer.this.setCustomizerAnimating(true);
                QSCustomizer.this.announceForAccessibility(QSCustomizer.this.mContext.getString(R.string.accessibility_desc_quick_settings));
                ((KeyguardMonitor) Dependency.get(KeyguardMonitor.class)).removeCallback(QSCustomizer.this.mKeyguardCallback);
            }
        }
    };
    /* access modifiers changed from: private */
    public RecyclerView mRecyclerView;
    protected TextView mResetButton;
    /* access modifiers changed from: private */
    public boolean mShownRequested;
    private int mSpanCount = Math.max(1, this.mContext.getResources().getInteger(R.integer.quick_settings_num_columns));
    private List<String> mSpecs = new ArrayList();
    protected TextView mSubTitle;
    /* access modifiers changed from: private */
    public TileAdapter mTileAdapter;
    private final TileQueryHelper mTileQueryHelper;
    protected TextView mTitle;
    /* access modifiers changed from: private */
    public int mX;
    /* access modifiers changed from: private */
    public int mY;

    public interface QSPanelCallback {
        void hide(int i, int i2, boolean z);

        void show(int i, int i2);
    }

    public QSCustomizer(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(getContext()).inflate(R.layout.qs_customize_panel_content, this);
        this.mRecyclerView = (RecyclerView) findViewById(16908298);
        this.mTileAdapter = new TileAdapter(getContext(), this.mSpanCount, this.mRecyclerView);
        this.mTileQueryHelper = new TileQueryHelper(context, this);
        this.mRecyclerView.setAdapter(this.mTileAdapter);
        this.mTileAdapter.getItemTouchHelper().attachToRecyclerView(this.mRecyclerView);
        updateLayout();
        this.mRecyclerView.addItemDecoration(this.mTileAdapter.getItemDecoration());
        DefaultItemAnimator animator = new DefaultItemAnimator();
        animator.setMoveDuration(150);
        this.mRecyclerView.setItemAnimator(animator);
        this.mResetButton = (TextView) findViewById(16908314);
        this.mResetButton.setText(R.string.reset);
        this.mResetButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                MetricsLogger.action(QSCustomizer.this.getContext(), 359);
                QSCustomizer.this.reset();
            }
        });
        this.mDoneButton = (TextView) findViewById(16908313);
        this.mDoneButton.setText(R.string.quick_settings_done);
        this.mDoneButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                QSCustomizer.this.mQsPanelCallback.hide(((int) v.getX()) + (v.getWidth() / 2), ((int) v.getY()) + (v.getHeight() / 2), true);
            }
        });
        this.mHeader = (RelativeLayout) findViewById(R.id.header);
        this.mTitle = (TextView) findViewById(R.id.title);
        this.mSubTitle = (TextView) findViewById(R.id.sub_title);
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.mTitle.setText(R.string.qs_customize_title);
        this.mSubTitle.setText(R.string.drag_to_add_tiles);
        this.mResetButton.setText(17040862);
        this.mDoneButton.setText(R.string.quick_settings_done);
        Resources res = this.mContext.getResources();
        int count = Math.max(1, res.getInteger(R.integer.quick_settings_num_columns));
        ViewGroup.LayoutParams layoutParams = this.mHeader.getLayoutParams();
        layoutParams.height = this.mContext.getResources().getDimensionPixelOffset(R.dimen.notch_expanded_header_height);
        this.mHeader.setLayoutParams(layoutParams);
        if (this.mSpanCount != count) {
            if (res.getConfiguration().orientation == 1) {
                this.mHeader.setVisibility(0);
            } else {
                this.mHeader.setVisibility(8);
            }
            this.mSpanCount = count;
            this.mTileAdapter.setSpanCount(this.mSpanCount);
            updateLayout();
        }
    }

    private void updateLayout() {
        GridLayoutManager layout = new GridLayoutManager(getContext(), this.mSpanCount);
        layout.setSpanSizeLookup(this.mTileAdapter.getSizeLookup());
        this.mRecyclerView.setLayoutManager(layout);
        setPadding(getPaddingLeft(), getResources().getDimensionPixelSize(R.dimen.qs_customize_padding_top), getPaddingRight(), getResources().getDimensionPixelOffset(R.dimen.qs_customize_padding_bottom));
    }

    public void setQs(QS qs) {
        this.mQs = qs;
    }

    public void setHost(QSTileHost host) {
        this.mHost = host;
        this.mTileAdapter.setHost(host);
    }

    public void setContainer(ViewGroup notificationsQsContainer) {
        this.mNotifQsContainer = notificationsQsContainer;
    }

    /* access modifiers changed from: private */
    public void queryTiles() {
        this.mTileQueryHelper.queryTiles(this.mHost);
    }

    /* access modifiers changed from: private */
    public void releaseTiles() {
        this.mTileQueryHelper.releaseTiles();
    }

    public void onTilesChanged(List<TileQueryHelper.TileInfo> tiles, Map<String, QSTile> liveTiles) {
        this.mTileAdapter.onTilesChanged(tiles, liveTiles);
        post(new Runnable() {
            public void run() {
                QSCustomizer.this.handleShowAnimation();
            }
        });
    }

    public void onTileChanged(TileQueryHelper.TileInfo tileInfo) {
        this.mTileAdapter.onTileChanged(tileInfo);
    }

    /* access modifiers changed from: private */
    public void handleShowAnimation() {
        if (this.isShown && this.mShownRequested) {
            setAlpha(1.0f);
            setVisibility(0);
            this.mClipper.animateCircularClip(this.mX, this.mY, true, this.mAnimInListener);
            this.mShownRequested = false;
        }
    }

    public void setQsPanel(QSPanel panel) {
        panel.setQSCustomizerCallback(this.mQsPanelCallback);
    }

    public boolean isShown() {
        return this.isShown;
    }

    /* access modifiers changed from: private */
    public void setCustomizing(boolean customizing) {
        this.mCustomizing = customizing;
        this.mQs.notifyCustomizeChanged();
    }

    public boolean isCustomizing() {
        return this.mCustomizing;
    }

    /* access modifiers changed from: private */
    public void reset() {
        ArrayList<String> tiles = new ArrayList<>();
        for (String tile : this.mContext.getString(R.string.quick_settings_tiles_default).split(",")) {
            tiles.add(tile);
        }
        this.mTileAdapter.resetTileSpecs(this.mHost, tiles);
    }

    /* access modifiers changed from: private */
    public void setTileSpecs() {
        List<String> specs = new ArrayList<>();
        Collection<QSTile> tiles = this.mHost.getTiles();
        this.mCount = tiles == null ? 0 : tiles.size();
        for (QSTile tile : tiles) {
            if (!"edit".equals(tile.getTileSpec())) {
                specs.add(tile.getTileSpec());
            }
        }
        this.mSpecs.addAll(specs);
        this.mTileAdapter.setTileSpecs(specs);
        this.mRecyclerView.setAdapter(this.mTileAdapter);
    }

    /* access modifiers changed from: private */
    public void save() {
        if (this.mTileQueryHelper.isFinished()) {
            this.mTileAdapter.saveSpecs(this.mHost);
            List<String> specs = this.mTileAdapter.getCurrentSpecs();
            if (specs != null) {
                int count = specs.size();
                if (this.mCount != count) {
                    AnalyticsHelper.trackQSTilesCount(count);
                }
                if (!this.mSpecs.equals(specs)) {
                    AnalyticsHelper.trackQSTilesOrderChange(count);
                }
            }
        }
        this.mSpecs.clear();
    }

    public void setAnimatedViews(List<View> animViews) {
        if (animViews != null && animViews.size() != 0) {
            this.mAnimInListener = AnimatorListenerWrapper.of(this.mExpandAnimationListener, new QSAnimation.QsHideBeforeAnimatorListener((View[]) animViews.toArray(new View[0])));
            this.mAnimOutListener = AnimatorListenerWrapper.of(this.mCollapseAnimationListener, new QSAnimation.QsShowBeforeAnimatorListener((View[]) animViews.toArray(new View[0])));
        }
    }

    /* access modifiers changed from: private */
    public void setCustomizerAnimating(boolean animating) {
        if (this.mCustomizerAnimating != animating) {
            this.mCustomizerAnimating = animating;
            this.mNotifQsContainer.invalidate();
        }
    }

    public int getVisualBottom() {
        return getBottom() - getPaddingBottom();
    }
}
