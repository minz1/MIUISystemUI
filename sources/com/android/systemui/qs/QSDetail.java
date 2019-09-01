package com.android.systemui.qs;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.CustomizedUtils;
import com.android.systemui.Dependency;
import com.android.systemui.FontUtils;
import com.android.systemui.R;
import com.android.systemui.SystemUI;
import com.android.systemui.miui.anim.AnimatorListenerWrapper;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.qs.DetailAdapter;
import com.android.systemui.plugins.qs.QS;
import com.android.systemui.qs.QSAnimation;
import com.android.systemui.statusbar.CommandQueue;
import java.util.List;
import miui.widget.SlidingButton;

public class QSDetail extends LinearLayout {
    private Animator.AnimatorListener mAnimInListener = this.mHideGridContentWhenDone;
    private Animator.AnimatorListener mAnimOutListener = this.mTeardownDetailWhenDone;
    /* access modifiers changed from: private */
    public boolean mAnimatingOpen;
    private QSDetailClipper mClipper;
    /* access modifiers changed from: private */
    public boolean mClosingDetail;
    /* access modifiers changed from: private */
    public DetailAdapter mDetailAdapter;
    /* access modifiers changed from: private */
    public ViewGroup mDetailContent;
    protected TextView mDetailDoneButton;
    protected TextView mDetailSettingsButton;
    private final SparseArray<View> mDetailViews = new SparseArray<>();
    private boolean mFullyExpanded;
    private final AnimatorListenerAdapter mHideGridContentWhenDone = new AnimatorListenerAdapter() {
        public void onAnimationCancel(Animator animation) {
            animation.removeListener(this);
            boolean unused = QSDetail.this.mAnimatingOpen = false;
            QSDetail.this.checkPendingAnimations();
        }

        public void onAnimationEnd(Animator animation) {
            boolean unused = QSDetail.this.mAnimatingOpen = false;
            QSDetail.this.checkPendingAnimations();
        }
    };
    protected QSTileHost mHost;
    private int mOpenX;
    private int mOpenY;
    private QS mQs;
    protected View mQsDetailHeader;
    protected SlidingButton mQsDetailHeaderSwitch;
    protected TextView mQsDetailHeaderTitle;
    /* access modifiers changed from: private */
    public QSPanel mQsPanel;
    protected QSPanelCallback mQsPanelCallback = new QSPanelCallback() {
        public void onToggleStateChanged(final boolean state) {
            QSDetail.this.post(new Runnable() {
                public void run() {
                    QSDetail.this.handleToggleStateChanged(state, QSDetail.this.mDetailAdapter != null && QSDetail.this.mDetailAdapter.getToggleEnabled());
                }
            });
        }

        public void onShowingDetail(final DetailAdapter detail, final int x, final int y) {
            QSDetail.this.post(new Runnable() {
                public void run() {
                    QSDetail.this.handleShowingDetail(detail, x, y, true);
                }
            });
        }

        public void onScanStateChanged(final boolean state) {
            QSDetail.this.post(new Runnable() {
                public void run() {
                    QSDetail.this.handleScanStateChanged(state);
                }
            });
        }
    };
    private boolean mScanState;
    private boolean mSwitchState;
    private final AnimatorListenerAdapter mTeardownDetailWhenDone = new AnimatorListenerAdapter() {
        public void onAnimationEnd(Animator animation) {
            QSDetail.this.mDetailContent.removeAllViews();
            QSDetail.this.setVisibility(4);
            boolean unused = QSDetail.this.mClosingDetail = false;
        }
    };
    protected View mTopDivider;
    private boolean mTriggeredExpand;

    public interface QSPanelCallback {
        void onScanStateChanged(boolean z);

        void onShowingDetail(DetailAdapter detailAdapter, int i, int i2);

        void onToggleStateChanged(boolean z);
    }

    public QSDetail(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        FontUtils.updateFontSize(this.mQsDetailHeaderTitle, R.dimen.qs_detail_header_text_size);
        FontUtils.updateFontSize(this.mDetailDoneButton, R.dimen.qs_detail_buttons_text_size);
        FontUtils.updateFontSize(this.mDetailSettingsButton, R.dimen.qs_detail_buttons_text_size);
        for (int i = 0; i < this.mDetailViews.size(); i++) {
            this.mDetailViews.valueAt(i).dispatchConfigurationChanged(newConfig);
        }
        updateDetailPadding();
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mDetailContent = (ViewGroup) findViewById(16908290);
        this.mDetailSettingsButton = (TextView) findViewById(16908314);
        this.mDetailDoneButton = (TextView) findViewById(16908313);
        this.mTopDivider = findViewById(R.id.top_divider);
        this.mQsDetailHeader = findViewById(R.id.qs_detail_header);
        this.mQsDetailHeaderTitle = (TextView) this.mQsDetailHeader.findViewById(16908310);
        this.mQsDetailHeaderSwitch = this.mQsDetailHeader.findViewById(16908311);
        updateDetailText();
        this.mClipper = new QSDetailClipper(this);
        this.mDetailDoneButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                QSDetail.this.announceForAccessibility(QSDetail.this.mContext.getString(R.string.accessibility_desc_quick_settings));
                QSDetail.this.mQsPanel.closeDetail(false);
            }
        });
    }

    public void setQsPanel(QSPanel panel) {
        this.mQsPanel = panel;
        panel.setQSDetailCallback(this.mQsPanelCallback);
    }

    public void setHost(QSTileHost host) {
        this.mHost = host;
    }

    public boolean isShowingDetail() {
        return this.mDetailAdapter != null;
    }

    public void setFullyExpanded(boolean fullyExpanded) {
        this.mFullyExpanded = fullyExpanded;
    }

    public void setExpanded(boolean qsExpanded) {
        if (!qsExpanded) {
            this.mTriggeredExpand = false;
        }
    }

    private void updateDetailText() {
        this.mDetailDoneButton.setText(R.string.quick_settings_done);
        this.mDetailSettingsButton.setText(R.string.quick_settings_more_settings);
    }

    private void updateDetailPadding() {
        setPadding(getPaddingLeft(), CustomizedUtils.getNotchExpandedHeaderViewHeight(getContext(), getResources().getDimensionPixelSize(R.dimen.notch_expanded_header_height)), getPaddingRight(), getResources().getDimensionPixelOffset(R.dimen.qs_detail_margin_bottom));
    }

    public void handleShowingDetail(DetailAdapter adapter, int x, int y, boolean toggleQs) {
        Animator.AnimatorListener listener;
        boolean showingDetail = adapter != null;
        setClickable(showingDetail);
        if (showingDetail) {
            setupDetailHeader(adapter);
            if (!toggleQs || this.mFullyExpanded) {
                this.mTriggeredExpand = false;
            } else {
                this.mTriggeredExpand = true;
                ((CommandQueue) SystemUI.getComponent(this.mContext, CommandQueue.class)).animateExpandSettingsPanel(null);
            }
            this.mOpenX = x;
            this.mOpenY = y;
        } else if (toggleQs && this.mTriggeredExpand) {
            ((CommandQueue) SystemUI.getComponent(this.mContext, CommandQueue.class)).animateCollapsePanels();
            this.mTriggeredExpand = false;
        }
        boolean visibleDiff = (this.mDetailAdapter != null) != (adapter != null);
        if (visibleDiff || this.mDetailAdapter != adapter) {
            if (adapter != null) {
                int viewCacheIndex = adapter.getMetricsCategory();
                View detailView = adapter.createDetailView(this.mContext, this.mDetailViews.get(viewCacheIndex), this.mDetailContent);
                if (detailView != null) {
                    setupDetailFooter(adapter);
                    this.mDetailContent.removeAllViews();
                    this.mDetailContent.addView(detailView);
                    this.mDetailViews.put(viewCacheIndex, detailView);
                    MetricsLogger.visible(this.mContext, adapter.getMetricsCategory());
                    announceForAccessibility(this.mContext.getString(R.string.accessibility_quick_settings_detail, new Object[]{adapter.getTitle()}));
                    this.mDetailAdapter = adapter;
                    listener = this.mAnimInListener;
                    setVisibility(0);
                } else {
                    throw new IllegalStateException("Must return detail view");
                }
            } else {
                if (this.mDetailAdapter != null) {
                    MetricsLogger.hidden(this.mContext, this.mDetailAdapter.getMetricsCategory());
                }
                this.mClosingDetail = true;
                this.mDetailAdapter = null;
                listener = this.mAnimOutListener;
                this.mQsPanelCallback.onScanStateChanged(false);
            }
            sendAccessibilityEvent(32);
            animateDetailVisibleDiff(x, y, visibleDiff, listener);
            this.mQs.notifyCustomizeChanged();
        }
    }

    /* access modifiers changed from: protected */
    public void animateDetailVisibleDiff(int x, int y, boolean visibleDiff, Animator.AnimatorListener listener) {
        if (visibleDiff) {
            boolean animate = false;
            this.mAnimatingOpen = this.mDetailAdapter != null;
            if (this.mFullyExpanded || this.mAnimatingOpen) {
                animate = true;
            }
            if (animate) {
                setAlpha(1.0f);
                this.mClipper.animateCircularClip(x, y, this.mAnimatingOpen, listener);
                return;
            }
            setAlpha(0.0f);
            listener.onAnimationEnd(null);
        }
    }

    /* access modifiers changed from: protected */
    public void setupDetailFooter(final DetailAdapter adapter) {
        final Intent settingsIntent = adapter.getSettingsIntent();
        this.mDetailSettingsButton.setVisibility(settingsIntent != null ? 0 : 8);
        this.mDetailSettingsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                MetricsLogger.action(QSDetail.this.mContext, 929, adapter.getMetricsCategory());
                ((ActivityStarter) Dependency.get(ActivityStarter.class)).postStartActivityDismissingKeyguard(settingsIntent, 0);
            }
        });
    }

    /* access modifiers changed from: protected */
    public void setupDetailHeader(final DetailAdapter adapter) {
        this.mQsDetailHeaderTitle.setText(adapter.getTitle());
        Boolean toggleState = adapter.getToggleState();
        if (toggleState == null) {
            this.mQsDetailHeaderSwitch.setVisibility(4);
            return;
        }
        this.mQsDetailHeaderSwitch.setVisibility(0);
        handleToggleStateChanged(toggleState.booleanValue(), adapter.getToggleEnabled());
        this.mQsDetailHeaderSwitch.setOnPerformCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                adapter.setToggleState(checked);
            }
        });
    }

    /* access modifiers changed from: private */
    public void handleToggleStateChanged(boolean state, boolean toggleEnabled) {
        this.mSwitchState = state;
        if (!this.mAnimatingOpen) {
            this.mQsDetailHeaderSwitch.setChecked(state);
            this.mQsDetailHeaderSwitch.setEnabled(toggleEnabled);
        }
    }

    /* access modifiers changed from: private */
    public void handleScanStateChanged(boolean state) {
        if (this.mScanState != state) {
            this.mScanState = state;
        }
    }

    /* access modifiers changed from: private */
    public void checkPendingAnimations() {
        handleToggleStateChanged(this.mSwitchState, this.mDetailAdapter != null && this.mDetailAdapter.getToggleEnabled());
    }

    public void setAnimatedViews(List<View> animViews) {
        if (animViews != null && animViews.size() != 0) {
            this.mAnimInListener = AnimatorListenerWrapper.of(this.mHideGridContentWhenDone, new QSAnimation.QsHideBeforeAnimatorListener((View[]) animViews.toArray(new View[0])));
            this.mAnimOutListener = AnimatorListenerWrapper.of(this.mTeardownDetailWhenDone, new QSAnimation.QsShowBeforeAnimatorListener((View[]) animViews.toArray(new View[0])));
        }
    }

    public void setQs(QS qs) {
        this.mQs = qs;
    }

    public int getVisualBottom() {
        return getBottom() - getPaddingBottom();
    }
}
