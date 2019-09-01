package com.android.systemui.qs;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.MiuiStatusBarManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import com.android.internal.os.SomeArgs;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.systemui.Dependency;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.SystemUI;
import com.android.systemui.plugins.qs.QS;
import com.android.systemui.qs.customize.QSCustomizer;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.HeaderView;
import com.android.systemui.statusbar.policy.BrightnessMirrorController;
import com.miui.systemui.support.v4.app.Fragment;

public class QSFragment extends Fragment implements QS, CommandQueue.Callbacks {
    /* access modifiers changed from: private */
    public final Animator.AnimatorListener mAnimateHeaderSlidingInListener = new AnimatorListenerAdapter() {
        public void onAnimationEnd(Animator animation) {
            boolean unused = QSFragment.this.mQuickQsAnimating = false;
            QSFragment.this.updateQsState();
        }
    };
    protected View mBackground;
    private Handler mBgHandler = new Handler((Looper) Dependency.get(Dependency.BG_LOOPER));
    private boolean mBrightnessListening;
    /* access modifiers changed from: private */
    public QSContainerImpl mContainer;
    protected View mContent;
    private int mContentMargin;
    protected View mContentWithoutHeader;
    /* access modifiers changed from: private */
    public long mDelay;
    private int mGutterHeight;
    protected QuickStatusBarHeader mHeader;
    protected HeaderView mHeaderView;
    private boolean mKeyguardShowing;
    private float mLastAppearFraction = -1.0f;
    private int mLayoutDirection;
    private boolean mListening;
    private QS.HeightListener mPanelView;
    private QSAnimator mQSAnimator;
    private QSCustomizer mQSCustomizer;
    /* access modifiers changed from: private */
    public boolean mQSDataUsageEnabled;
    private QSDetail mQSDetail;
    private View mQSFooterBundle;
    protected QSPanel mQSPanel;
    private boolean mQsDisabled;
    private boolean mQsExpanded;
    protected QuickQSPanel mQuickQSPanel;
    /* access modifiers changed from: private */
    public boolean mQuickQsAnimating;
    private ContentResolver mResolver;
    private ContentObserver mShowDataUsageObserver;
    private boolean mStackScrollerOverscrolling;
    private final ViewTreeObserver.OnPreDrawListener mStartHeaderSlidingIn = new ViewTreeObserver.OnPreDrawListener() {
        public boolean onPreDraw() {
            QSFragment.this.getView().getViewTreeObserver().removeOnPreDrawListener(this);
            QSFragment.this.getView().animate().translationY(0.0f).setStartDelay(QSFragment.this.mDelay).setDuration(448).setInterpolator(Interpolators.FAST_OUT_SLOW_IN).setListener(QSFragment.this.mAnimateHeaderSlidingInListener).start();
            QSFragment.this.getView().setY((float) (-QSFragment.this.getQsMinExpansionHeight()));
            return true;
        }
    };
    private int mStatusBarMinHeight;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.qs_panel, container, false);
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Resources res = getResources();
        this.mContainer = (QSContainerImpl) view.findViewById(R.id.quick_settings_container);
        this.mContentWithoutHeader = view.findViewById(R.id.qs_container);
        this.mContent = view.findViewById(R.id.qs_content);
        this.mBackground = view.findViewById(R.id.qs_background);
        this.mQuickQSPanel = (QuickQSPanel) view.findViewById(R.id.quick_qs_panel);
        this.mQSPanel = (QSPanel) view.findViewById(R.id.quick_settings_panel);
        this.mQSDetail = (QSDetail) view.findViewById(R.id.qs_detail);
        this.mQSDetail.setQsPanel(this.mQSPanel);
        this.mQSDetail.setQs(this);
        this.mQSCustomizer = (QSCustomizer) view.findViewById(R.id.qs_customize);
        this.mQSCustomizer.setQsPanel(this.mQSPanel);
        this.mQSCustomizer.setQs(this);
        this.mHeader = (QuickStatusBarHeader) view.findViewById(R.id.header);
        this.mHeaderView = (HeaderView) view.findViewById(R.id.header_content);
        this.mQSFooterBundle = view.findViewById(R.id.qs_footer_bundle);
        this.mGutterHeight = res.getDimensionPixelSize(R.dimen.qs_gutter_height);
        this.mContentMargin = res.getDimensionPixelSize(R.dimen.panel_content_margin);
        this.mStatusBarMinHeight = res.getDimensionPixelSize(17105351);
        if (res.getBoolean(R.bool.config_showQuickSettingsRow)) {
            this.mQSAnimator = new QSAnimator(this, this.mQuickQSPanel, this.mQSPanel);
        }
        if (savedInstanceState != null) {
            setExpanded(savedInstanceState.getBoolean("expanded"));
            setListening(savedInstanceState.getBoolean("listening"));
            setBrightnessListening(savedInstanceState.getBoolean("brightness_listening"));
        }
        this.mResolver = getContext().getContentResolver();
        this.mShowDataUsageObserver = new ContentObserver(this.mBgHandler) {
            public void onChange(boolean selfChange) {
                Context context = QSFragment.this.getContext();
                if (context != null) {
                    boolean unused = QSFragment.this.mQSDataUsageEnabled = MiuiStatusBarManager.isShowFlowInfoForUser(context, -2);
                    QSFragment.this.mContainer.post(new Runnable() {
                        public void run() {
                            QSFragment.this.mContainer.updateQSDataUsage(QSFragment.this.mQSDataUsageEnabled);
                        }
                    });
                }
            }
        };
        this.mResolver.registerContentObserver(Settings.System.getUriFor("status_bar_show_network_assistant"), false, this.mShowDataUsageObserver, -1);
        this.mShowDataUsageObserver.onChange(false);
        ((CommandQueue) SystemUI.getComponent(getContext(), CommandQueue.class)).addCallbacks(this);
    }

    public void onDestroyView() {
        ((CommandQueue) SystemUI.getComponent(getContext(), CommandQueue.class)).removeCallbacks(this);
        this.mResolver.unregisterContentObserver(this.mShowDataUsageObserver);
        super.onDestroyView();
    }

    public void onDestroy() {
        if (this.mListening) {
            setListening(false);
        }
        this.mQSDetail = null;
        super.onDestroy();
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("expanded", this.mQsExpanded);
        outState.putBoolean("listening", this.mListening);
        outState.putBoolean("brightness_listening", this.mBrightnessListening);
    }

    /* access modifiers changed from: package-private */
    public boolean isListening() {
        return this.mListening;
    }

    /* access modifiers changed from: package-private */
    public boolean isExpanded() {
        return this.mQsExpanded;
    }

    public boolean isQSFullyCollapsed() {
        return this.mContainer.isQSFullyCollapsed();
    }

    public View getHeader() {
        return this.mHeader;
    }

    public View getHeaderView() {
        return this.mHeaderView;
    }

    public void setHasNotifications(boolean hasNotifications) {
        this.mContainer.setGutterEnabled(hasNotifications);
    }

    public void setPanelView(QS.HeightListener panelView) {
        this.mPanelView = panelView;
    }

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.getLayoutDirection() != this.mLayoutDirection) {
            this.mLayoutDirection = newConfig.getLayoutDirection();
            if (this.mQSAnimator != null) {
                this.mQSAnimator.onRtlChanged();
            }
        }
    }

    public void setHost(QSTileHost qsh) {
        this.mQSPanel.setHost(qsh);
        this.mQSDetail.setHost(qsh);
        this.mQSCustomizer.setHost(qsh);
        this.mQuickQSPanel.setHost(qsh);
        this.mQuickQSPanel.setQSPanelAndHeader(this.mQSPanel, this.mHeader);
        if (this.mContainer.getQSFooter() != null) {
            this.mContainer.getQSFooter().setQSPanel(this.mQSPanel);
        }
        if (this.mQSAnimator != null) {
            this.mQSAnimator.setHost(qsh);
        }
    }

    public void disable(int state1, int state2, boolean animate) {
        boolean disabled = (state2 & 1) != 0;
        if (disabled != this.mQsDisabled) {
            this.mQsDisabled = disabled;
            updateQsState();
        }
    }

    /* access modifiers changed from: private */
    public void updateQsState() {
        boolean z = true;
        int i = 0;
        boolean expandVisually = this.mQsExpanded || this.mStackScrollerOverscrolling || this.mQuickQsAnimating;
        this.mQSPanel.setExpanded(this.mQsExpanded);
        this.mQuickQSPanel.setExpanded(this.mQsExpanded);
        if (this.mQSDetail != null) {
            this.mQSDetail.setExpanded(this.mQsExpanded);
        }
        int i2 = 4;
        this.mHeader.setVisibility((this.mQsExpanded || !this.mKeyguardShowing || this.mQuickQsAnimating) ? 0 : 4);
        this.mHeader.setExpanded((this.mKeyguardShowing && !this.mQuickQsAnimating) || (this.mQsExpanded && !this.mStackScrollerOverscrolling));
        if (this.mContainer.getQSFooter() != null) {
            QSFooter qSFooter = this.mContainer.getQSFooter();
            if ((!this.mKeyguardShowing || this.mQuickQsAnimating) && (!this.mQsExpanded || this.mStackScrollerOverscrolling)) {
                z = false;
            }
            qSFooter.setExpanded(z);
        }
        this.mQSPanel.setVisibility(expandVisually ? 0 : 4);
        this.mContainer.getBrightnessView().setVisibility((!this.mKeyguardShowing || expandVisually) ? 0 : 4);
        View expandIndicator = this.mContainer.getExpandIndicator();
        if (!this.mKeyguardShowing || expandVisually) {
            i2 = 0;
        }
        expandIndicator.setVisibility(i2);
        QSContainerImpl qSContainerImpl = this.mContainer;
        if (this.mQsDisabled) {
            i = 8;
        }
        qSContainerImpl.setVisibility(i);
    }

    public View getQsContent() {
        return this.mContentWithoutHeader;
    }

    public QSPanel getQsPanel() {
        return this.mQSPanel;
    }

    public void setHeaderClickable(boolean clickable) {
        if (this.mContainer.getQSFooter() != null) {
            this.mContainer.getQSFooter().getExpandView().setClickable(clickable);
        }
    }

    public boolean isCustomizing() {
        return this.mQSCustomizer.isCustomizing() || this.mQSCustomizer.isShown();
    }

    public void setExpanded(boolean expanded) {
        this.mQsExpanded = expanded;
        if (this.mKeyguardShowing) {
            setBrightnessListening(this.mQsExpanded);
        }
        updateQsState();
    }

    public void setKeyguardShowing(boolean keyguardShowing) {
        this.mKeyguardShowing = keyguardShowing;
        if (this.mQSAnimator != null) {
            this.mQSAnimator.setOnKeyguard(keyguardShowing);
        }
        if (this.mContainer.getQSFooter() != null) {
            this.mContainer.getQSFooter().setKeyguardShowing(keyguardShowing);
        }
        updateQsState();
    }

    public void setOverscrolling(boolean stackScrollerOverscrolling) {
        this.mStackScrollerOverscrolling = stackScrollerOverscrolling;
        updateQsState();
    }

    public void setListening(boolean listening) {
        this.mListening = listening;
        this.mContainer.setListening(listening);
        this.mQSPanel.setListening(this.mListening);
    }

    public boolean isShowingDetail() {
        return this.mQSDetail.isShowingDetail();
    }

    public void setHeaderListening(boolean listening) {
        this.mQuickQSPanel.setListening(listening);
        if (this.mContainer.getQSFooter() != null) {
            this.mContainer.getQSFooter().setListening(listening);
        }
        if (this.mContainer.isDataUsageAvailable()) {
            this.mContainer.updateDataUsageInfo();
        }
    }

    public void notifyCustomizeChanged() {
        this.mPanelView.onQsHeightChanged();
    }

    public void setContainer(ViewGroup container) {
        this.mQSCustomizer.setContainer(container);
    }

    public void setBrightnessListening(boolean listening) {
        this.mBrightnessListening = listening;
        this.mContainer.setBrightnessListening(listening);
    }

    public void setQsExpansion(float expansion, float headerTranslation, float appearFraction) {
        float f;
        this.mContainer.setExpansion(expansion);
        float translationScaleY = expansion - 1.0f;
        if (!this.mQuickQsAnimating) {
            int height = getQsMinExpansionHeight() + this.mGutterHeight;
            View view = getView();
            if (this.mKeyguardShowing) {
                f = ((float) height) * translationScaleY;
            } else {
                f = 0.0f;
            }
            view.setTranslationY(f);
        }
        if (this.mLastAppearFraction != appearFraction) {
            float headerFraction = getFraction(0.05f, 0.3f, appearFraction);
            this.mHeader.setAlpha(headerFraction);
            this.mHeader.setTranslationY((1.0f - headerFraction) * 0.25f * ((float) (-this.mHeader.getHeight())));
            float contentFraction = getFraction(0.1f, 1.0f, appearFraction);
            View qsContent = getQsContent();
            qsContent.setTransitionAlpha(contentFraction);
            qsContent.setScaleX((0.12f * appearFraction) + 0.88f);
            qsContent.setScaleY(0.88f + (0.12f * appearFraction));
            qsContent.setPivotX(0.5f * ((float) qsContent.getWidth()));
            qsContent.setPivotY(-0.3f * ((float) qsContent.getHeight()));
            this.mLastAppearFraction = appearFraction;
        }
        if (this.mContainer.getQSFooter() != null) {
            this.mContainer.getQSFooter().setExpansion(this.mKeyguardShowing ? 1.0f : expansion);
        }
        this.mQSPanel.setTranslationY(((float) ((this.mQSPanel.getBottom() - this.mQuickQSPanel.getBottom()) + this.mQuickQSPanel.getPaddingBottom())) * translationScaleY);
        if (this.mQSDetail != null) {
            this.mQSDetail.setFullyExpanded(expansion == 1.0f);
        }
        if (this.mQSAnimator != null) {
            this.mQSAnimator.setPosition(expansion);
        }
    }

    private float getFraction(float start, float end, float current) {
        if (current <= start) {
            return 0.0f;
        }
        if (current >= end) {
            return 1.0f;
        }
        return (current - start) / (end - start);
    }

    public void animateHeaderSlidingIn(long delay) {
        if (!this.mQsExpanded) {
            this.mQuickQsAnimating = true;
            this.mDelay = delay;
            getView().getViewTreeObserver().addOnPreDrawListener(this.mStartHeaderSlidingIn);
        }
    }

    public void animateHeaderSlidingOut() {
        this.mQuickQsAnimating = true;
        getView().animate().y((float) ((-getQsMinExpansionHeight()) - (this.mContentMargin * 2))).setStartDelay(0).setDuration(360).setInterpolator(Interpolators.FAST_OUT_SLOW_IN).setListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                QSFragment.this.getView().animate().setListener(null);
                boolean unused = QSFragment.this.mQuickQsAnimating = false;
                QSFragment.this.updateQsState();
            }
        }).start();
    }

    public void setExpandClickListener(View.OnClickListener onClickListener) {
        if (this.mContainer.getQSFooter() != null) {
            this.mContainer.getQSFooter().getExpandView().setOnClickListener(onClickListener);
        }
    }

    public void closeDetail() {
        this.mQSPanel.closeDetail(true);
    }

    public int getDesiredHeight() {
        if (this.mQSCustomizer.isShown()) {
            return this.mQSCustomizer.getVisualBottom() + this.mQSFooterBundle.getHeight();
        }
        if (this.mQSDetail.isShowingDetail()) {
            return this.mQSDetail.getVisualBottom() + this.mQSFooterBundle.getHeight();
        }
        return this.mQsDisabled ? this.mStatusBarMinHeight : this.mContent.getMeasuredHeight();
    }

    public void setHeightOverride(int desiredHeight) {
        this.mContainer.setHeightOverride(desiredHeight);
    }

    public void setBrightnessMirror(BrightnessMirrorController c) {
        this.mContainer.setBrightnessMirror(c);
    }

    public int getQsMinExpansionHeight() {
        return this.mQsDisabled ? this.mStatusBarMinHeight : this.mContainer.getQsMinExpansionHeight();
    }

    public int getQsHeaderHeight() {
        return this.mQsDisabled ? this.mStatusBarMinHeight : this.mHeader.getHeight();
    }

    public void hideImmediately() {
        getView().animate().cancel();
        getView().setY((float) (-getQsMinExpansionHeight()));
    }

    public void setIcon(String slot, StatusBarIcon icon) {
    }

    public void removeIcon(String slot) {
    }

    public void animateExpandNotificationsPanel() {
    }

    public void animateCollapsePanels(int flags) {
    }

    public void animateExpandSettingsPanel(String obj) {
    }

    public void setSystemUiVisibility(int vis, int fullscreenStackVis, int dockedStackVis, int mask, Rect fullscreenStackBounds, Rect dockedStackBounds) {
    }

    public void topAppWindowChanged(boolean visible) {
    }

    public void setImeWindowStatus(IBinder token, int vis, int backDisposition, boolean showImeSwitcher) {
    }

    public void showRecentApps(boolean triggeredFromAltTab, boolean fromHome) {
    }

    public void hideRecentApps(boolean triggeredFromAltTab, boolean triggeredFromHomeKey) {
    }

    public void toggleRecentApps() {
    }

    public void toggleSplitScreen() {
    }

    public void preloadRecentApps() {
    }

    public void dismissKeyboardShortcutsMenu() {
    }

    public void toggleKeyboardShortcutsMenu(int deviceId) {
    }

    public void cancelPreloadRecentApps() {
    }

    public void setWindowState(int window, int state) {
    }

    public void showScreenPinningRequest(int taskId) {
    }

    public void appTransitionPending(boolean forced) {
    }

    public void appTransitionCancelled() {
    }

    public void appTransitionStarting(long startTime, long duration, boolean forced) {
    }

    public void appTransitionFinished() {
    }

    public void showAssistDisclosure() {
    }

    public void startAssist(Bundle args) {
    }

    public void showPictureInPictureMenu() {
    }

    public void addQsTile(ComponentName tile) {
    }

    public void remQsTile(ComponentName tile) {
    }

    public void clickTile(ComponentName tile) {
    }

    public void handleSystemNavigationKey(int arg1) {
    }

    public void handleShowGlobalActionsMenu() {
    }

    public void setStatus(int what, String action, Bundle ext) {
    }

    public void showFingerprintDialog(SomeArgs args) {
    }

    public void onFingerprintAuthenticated() {
    }

    public void onFingerprintHelp(String message) {
    }

    public void onFingerprintError(String error) {
    }

    public void hideFingerprintDialog() {
    }
}
