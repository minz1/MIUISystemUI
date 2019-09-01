package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import com.android.systemui.Constants;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.miui.statusbar.phone.MiuiStatusBarPromptController;
import com.android.systemui.statusbar.policy.DarkIconDispatcher;

public class PhoneStatusBarView extends PanelBar implements MiuiStatusBarPromptController.OnPromptStateChangedListener {
    private static final boolean DEBUG = StatusBar.DEBUG;
    StatusBar mBar;
    private final PhoneStatusBarTransitions mBarTransitions = new PhoneStatusBarTransitions(this);
    private DarkIconDispatcher.DarkReceiver mBattery;
    private boolean mBlockClickActionToStatusBar;
    private long mDownTime;
    private float mDownX;
    private float mDownY;
    private Runnable mHideExpandedRunnable = new Runnable() {
        public void run() {
            if (PhoneStatusBarView.this.mPanel.isFullyCollapsed()) {
                PhoneStatusBarView.this.mBar.makeExpandedInvisible();
            }
        }
    };
    boolean mIsFullyOpenedPanel = false;
    private float mMinFraction;
    private boolean mPanelClosedOnDown;
    private float mPanelFraction;
    private ScrimController mScrimController;

    public PhoneStatusBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BarTransitions getBarTransitions() {
        return this.mBarTransitions;
    }

    public void setBar(StatusBar bar) {
        this.mBar = bar;
        ((MiuiStatusBarPromptController) Dependency.get(MiuiStatusBarPromptController.class)).addStatusBarPrompt("PhoneStatusBarView", this.mBar, this, 0, this);
    }

    public void setScrimController(ScrimController scrimController) {
        this.mScrimController = scrimController;
    }

    public void onFinishInflate() {
        super.onFinishInflate();
        this.mBarTransitions.init();
        this.mBattery = (DarkIconDispatcher.DarkReceiver) findViewById(R.id.battery);
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        ((DarkIconDispatcher) Dependency.get(DarkIconDispatcher.class)).addDarkReceiver(this.mBattery);
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ((DarkIconDispatcher) Dependency.get(DarkIconDispatcher.class)).removeDarkReceiver(this.mBattery);
    }

    public boolean panelEnabled() {
        return this.mBar.panelsEnabled();
    }

    public boolean onRequestSendAccessibilityEventInternal(View child, AccessibilityEvent event) {
        if (!super.onRequestSendAccessibilityEventInternal(child, event)) {
            return false;
        }
        AccessibilityEvent record = AccessibilityEvent.obtain();
        onInitializeAccessibilityEvent(record);
        dispatchPopulateAccessibilityEvent(record);
        event.appendRecord(record);
        return true;
    }

    public void onPanelPeeked() {
        super.onPanelPeeked();
        this.mBar.makeExpandedVisible(false);
    }

    public void onPanelCollapsed() {
        super.onPanelCollapsed();
        post(this.mHideExpandedRunnable);
        this.mIsFullyOpenedPanel = false;
    }

    public void removePendingHideExpandedRunnables() {
        removeCallbacks(this.mHideExpandedRunnable);
    }

    public void onPanelFullyOpened() {
        super.onPanelFullyOpened();
        if (!this.mIsFullyOpenedPanel) {
            this.mPanel.sendAccessibilityEvent(32);
        }
        this.mIsFullyOpenedPanel = true;
    }

    public boolean onTouchEvent(MotionEvent event) {
        processMiuiPromptClick(event);
        return this.mBar.interceptTouchEvent(event) || super.onTouchEvent(event) || this.mBlockClickActionToStatusBar;
    }

    public void onTrackingStarted() {
        super.onTrackingStarted();
        this.mBar.onTrackingStarted();
        this.mScrimController.onTrackingStarted();
        removePendingHideExpandedRunnables();
    }

    public void onClosingFinished() {
        super.onClosingFinished();
        this.mBar.onClosingFinished();
    }

    public void onTrackingStopped(boolean expand) {
        super.onTrackingStopped(expand);
        this.mBar.onTrackingStopped(expand);
    }

    public void onExpandingFinished() {
        super.onExpandingFinished();
        this.mScrimController.onExpandingFinished();
    }

    public boolean onInterceptTouchEvent(MotionEvent event) {
        return this.mBar.interceptTouchEvent(event) || super.onInterceptTouchEvent(event);
    }

    private void processMiuiPromptClick(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        boolean z = true;
        if (event.getActionMasked() == 0) {
            if (this.mPanel.isKeyguardShowing() || !((MiuiStatusBarPromptController) Dependency.get(MiuiStatusBarPromptController.class)).blockClickAction()) {
                z = false;
            }
            this.mBlockClickActionToStatusBar = z;
            this.mPanelClosedOnDown = this.mPanel.isFullyCollapsed();
            this.mDownTime = SystemClock.uptimeMillis();
            this.mDownX = x;
            this.mDownY = y;
        } else if (event.getActionMasked() == 1 && this.mPanelClosedOnDown && !this.mPanel.isPanelVisibleBecauseOfHeadsUp() && !this.mPanel.isTracking()) {
            float touchSlop = (float) ViewConfiguration.get(getContext()).getScaledTouchSlop();
            if (SystemClock.uptimeMillis() - this.mDownTime < ((long) ViewConfiguration.getLongPressTimeout()) && Math.abs(x - this.mDownX) < touchSlop && Math.abs(y - this.mDownY) < touchSlop && this.mBlockClickActionToStatusBar) {
                this.mPanel.cancelPeek();
                ((MiuiStatusBarPromptController) Dependency.get(MiuiStatusBarPromptController.class)).handleClickAction();
            }
        }
    }

    public void panelScrimMinFractionChanged(float minFraction) {
        if (this.mMinFraction != minFraction) {
            this.mMinFraction = minFraction;
            updateScrimFraction();
        }
    }

    public void panelExpansionChanged(float frac, boolean expanded) {
        super.panelExpansionChanged(frac, expanded);
        this.mPanelFraction = frac;
        updateScrimFraction();
    }

    private void updateScrimFraction() {
        float scrimFraction = this.mPanelFraction;
        if (this.mMinFraction < 1.0f) {
            scrimFraction = Math.max((this.mPanelFraction - this.mMinFraction) / (1.0f - this.mMinFraction), 0.0f);
        }
        this.mScrimController.setPanelExpansion(scrimFraction);
    }

    public void onDensityOrFontScaleChanged() {
        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        layoutParams.height = getResources().getDimensionPixelSize(R.dimen.status_bar_height);
        setLayoutParams(layoutParams);
    }

    public void onPromptStateChanged(boolean isNormalMode, int topState) {
        if (Constants.IS_NOTCH) {
            this.mBar.refreshClockVisibility(isNormalMode);
        }
    }
}
