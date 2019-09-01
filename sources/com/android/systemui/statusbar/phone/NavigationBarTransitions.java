package com.android.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import com.android.internal.statusbar.IStatusBarService;
import com.android.systemui.R;

public final class NavigationBarTransitions extends BarTransitions {
    /* access modifiers changed from: private */
    public final IStatusBarService mBarService;
    private boolean mLightsOut;
    private final View.OnTouchListener mLightsOutListener = new View.OnTouchListener() {
        public boolean onTouch(View v, MotionEvent ev) {
            if (ev.getAction() == 0) {
                NavigationBarTransitions.this.applyLightsOut(false, false, false);
                try {
                    NavigationBarTransitions.this.mBarService.setSystemUiVisibility(0, 1, "LightsOutListener");
                } catch (RemoteException e) {
                }
            }
            return false;
        }
    };
    private final NavigationBarView mView;

    public NavigationBarTransitions(NavigationBarView view) {
        super(view, R.drawable.nav_background, R.color.system_nav_bar_background_opaque);
        this.mView = view;
        this.mBarService = IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"));
    }

    public void init() {
        applyModeBackground(-1, getMode(), false);
        applyMode(getMode(), false, true);
    }

    /* access modifiers changed from: protected */
    public void onTransition(int oldMode, int newMode, boolean animate) {
        super.onTransition(oldMode, newMode, animate);
        applyMode(newMode, animate, false);
    }

    private void applyMode(int mode, boolean animate, boolean force) {
        applyLightsOut(isLightsOut(mode), animate, force);
    }

    /* access modifiers changed from: private */
    public void applyLightsOut(boolean lightsOut, boolean animate, boolean force) {
        if (force || lightsOut != this.mLightsOut) {
            this.mLightsOut = lightsOut;
            View navButtons = this.mView.getCurrentView().findViewById(R.id.nav_buttons);
            final View lowLights = this.mView.getCurrentView().findViewById(R.id.lights_out);
            navButtons.animate().cancel();
            lowLights.animate().cancel();
            float lowLightsAlpha = 1.0f;
            float navButtonsAlpha = lightsOut ? 0.0f : 1.0f;
            if (!lightsOut) {
                lowLightsAlpha = 0.0f;
            }
            int i = 8;
            if (!animate) {
                navButtons.setAlpha(navButtonsAlpha);
                lowLights.setAlpha(lowLightsAlpha);
                if (lightsOut) {
                    i = 0;
                }
                lowLights.setVisibility(i);
            } else {
                int duration = lightsOut ? 750 : 250;
                navButtons.animate().alpha(navButtonsAlpha).setDuration((long) duration).start();
                lowLights.setOnTouchListener(this.mLightsOutListener);
                if (lowLights.getVisibility() == 8) {
                    lowLights.setAlpha(0.0f);
                    lowLights.setVisibility(0);
                }
                lowLights.animate().alpha(lowLightsAlpha).setDuration((long) duration).setInterpolator(new AccelerateInterpolator(2.0f)).setListener(lightsOut ? null : new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator _a) {
                        lowLights.setVisibility(8);
                    }
                }).start();
            }
        }
    }
}
