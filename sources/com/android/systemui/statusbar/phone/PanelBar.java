package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import com.android.systemui.Constants;

public abstract class PanelBar extends FrameLayout {
    public static final boolean DEBUG = Constants.DEBUG;
    public static final String TAG = PanelBar.class.getSimpleName();
    PanelView mPanel;
    private int mState = 0;
    private boolean mTracking;

    public abstract void panelScrimMinFractionChanged(float f);

    public static final void LOG(String fmt, Object... args) {
        if (DEBUG) {
            Log.v(TAG, String.format(fmt, args));
        }
    }

    public static final void LOG(Class t, String fmt) {
        Log.v(TAG + " " + t.getSimpleName(), fmt);
    }

    public void go(int state) {
        if (DEBUG) {
            LOG("go state: %d -> %d", Integer.valueOf(this.mState), Integer.valueOf(state));
        }
        this.mState = state;
    }

    public int getState() {
        return this.mState;
    }

    public PanelBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
    }

    public void setPanel(PanelView pv) {
        this.mPanel = pv;
        pv.setBar(this);
    }

    public void setBouncerShowing(boolean showing) {
        int important;
        if (showing) {
            important = 4;
        } else {
            important = 0;
        }
        setImportantForAccessibility(important);
        if (this.mPanel != null) {
            this.mPanel.setImportantForAccessibility(important);
        }
    }

    public boolean panelEnabled() {
        return true;
    }

    public boolean onTouchEvent(MotionEvent event) {
        boolean z = false;
        if (!panelEnabled()) {
            if (event.getAction() == 0) {
                Log.v(TAG, String.format("onTouch: all panels disabled, ignoring touch at (%d,%d)", new Object[]{Integer.valueOf((int) event.getX()), Integer.valueOf((int) event.getY())}));
            }
            return false;
        }
        if (event.getAction() == 0) {
            PanelView panel = this.mPanel;
            if (panel == null) {
                Log.v(TAG, String.format("onTouch: no panel for touch at (%d,%d)", new Object[]{Integer.valueOf((int) event.getX()), Integer.valueOf((int) event.getY())}));
                return true;
            }
            boolean enabled = panel.isEnabled();
            if (DEBUG) {
                Object[] objArr = new Object[3];
                objArr[0] = Integer.valueOf(this.mState);
                objArr[1] = panel;
                objArr[2] = enabled ? "" : " (disabled)";
                LOG("PanelBar.onTouch: state=%d ACTION_DOWN: panel %s %s", objArr);
            }
            if (!enabled) {
                Log.v(TAG, String.format("onTouch: panel (%s) is disabled, ignoring touch at (%d,%d)", new Object[]{panel, Integer.valueOf((int) event.getX()), Integer.valueOf((int) event.getY())}));
                return true;
            }
        }
        if (this.mPanel == null || this.mPanel.onTouchEvent(event)) {
            z = true;
        }
        return z;
    }

    public void panelExpansionChanged(float frac, boolean expanded) {
        boolean fullyClosed = true;
        boolean fullyOpened = false;
        PanelView pv = this.mPanel;
        pv.setVisibility(expanded ? 0 : 4);
        if (expanded) {
            boolean z = true;
            if (this.mState == 0) {
                go(1);
                onPanelPeeked();
            }
            fullyClosed = false;
            if (pv.getExpandedFraction() < 1.0f) {
                z = false;
            }
            fullyOpened = z;
        }
        if (fullyOpened && !this.mTracking) {
            go(2);
            onPanelFullyOpened();
        } else if (fullyClosed && !this.mTracking && this.mState != 0) {
            go(0);
            onPanelCollapsed();
        }
    }

    public void collapsePanel(boolean animate, boolean delayed, float speedUpFactor) {
        boolean waiting = false;
        PanelView pv = this.mPanel;
        if (!animate || pv.isFullyCollapsed()) {
            pv.resetViews();
            pv.setExpandedFraction(0.0f);
            pv.cancelPeek();
        } else {
            pv.collapse(delayed, speedUpFactor);
            waiting = true;
        }
        if (DEBUG) {
            LOG("collapsePanel: animate=%s waiting=%s", Boolean.valueOf(animate), Boolean.valueOf(waiting));
        }
        if (!waiting && this.mState != 0) {
            go(0);
            onPanelCollapsed();
        }
    }

    public void onPanelPeeked() {
        if (DEBUG) {
            LOG("onPanelPeeked", new Object[0]);
        }
    }

    public boolean isClosed() {
        return this.mState == 0;
    }

    public void onPanelCollapsed() {
        if (DEBUG) {
            LOG("onPanelCollapsed", new Object[0]);
        }
    }

    public void onPanelFullyOpened() {
        if (DEBUG) {
            LOG("onPanelFullyOpened", new Object[0]);
        }
    }

    public void onTrackingStarted() {
        this.mTracking = true;
    }

    public void onTrackingStopped(boolean expand) {
        this.mTracking = false;
    }

    public void onExpandingFinished() {
        if (DEBUG) {
            LOG("onExpandingFinished", new Object[0]);
        }
    }

    public void onClosingFinished() {
    }
}
