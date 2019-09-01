package com.android.keyguard.charge.rapid;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class WirelessRapidChargeView extends FrameLayout {
    public WirelessRapidChargeView(Context context) {
        this(context, null);
    }

    public WirelessRapidChargeView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WirelessRapidChargeView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setChargeState(boolean superRapid, boolean isCarMode) {
    }

    public void setScreenOn(boolean screenOn) {
    }

    public void setProgress(float progress) {
    }

    public void zoomLarge(boolean screenOn) {
    }

    public void addToWindow(String reason) {
    }

    public void removeFromWindow(String reason) {
    }

    public void startDismiss(String reason) {
    }

    public void setRapidAnimationListener(IRapidAnimationListener listener) {
    }
}
