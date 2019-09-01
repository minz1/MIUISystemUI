package com.android.systemui.statusbar.phone;

import android.animation.ArgbEvaluator;
import android.content.Context;
import android.graphics.Rect;
import android.util.ArrayMap;
import com.android.systemui.R;
import com.android.systemui.statusbar.phone.LightBarTransitionsController;
import com.android.systemui.statusbar.policy.DarkIconDispatcher;
import com.miui.systemui.annotation.Inject;

public class DarkIconDispatcherImpl implements DarkIconDispatcher {
    private float mDarkIntensity;
    private int mDarkModeIconColorSingleTone;
    private int mIconTint = -1;
    private int mLightModeIconColorSingleTone;
    private final ArrayMap<Object, DarkIconDispatcher.DarkReceiver> mReceivers = new ArrayMap<>();
    private final Rect mTintArea = new Rect();
    private final LightBarTransitionsController mTransitionsController;

    public DarkIconDispatcherImpl(@Inject Context context) {
        this.mDarkModeIconColorSingleTone = context.getColor(R.color.dark_mode_icon_color_single_tone);
        this.mLightModeIconColorSingleTone = context.getColor(R.color.light_mode_icon_color_single_tone);
        this.mTransitionsController = new LightBarTransitionsController(context, new LightBarTransitionsController.DarkIntensityApplier() {
            public void applyDarkIntensity(float darkIntensity) {
                DarkIconDispatcherImpl.this.setIconTintInternal(darkIntensity);
            }
        });
    }

    public LightBarTransitionsController getTransitionsController() {
        return this.mTransitionsController;
    }

    public void addDarkReceiver(DarkIconDispatcher.DarkReceiver receiver) {
        this.mReceivers.put(receiver, receiver);
        receiver.onDarkChanged(this.mTintArea, this.mDarkIntensity, this.mIconTint);
    }

    public void removeDarkReceiver(DarkIconDispatcher.DarkReceiver object) {
        this.mReceivers.remove(object);
    }

    public void setIconsDarkArea(Rect darkArea) {
        if (darkArea != null || !this.mTintArea.isEmpty()) {
            if (darkArea == null) {
                this.mTintArea.setEmpty();
            } else {
                this.mTintArea.set(darkArea);
            }
            applyIconTint();
        }
    }

    /* access modifiers changed from: private */
    public void setIconTintInternal(float darkIntensity) {
        this.mDarkIntensity = darkIntensity;
        this.mIconTint = ((Integer) ArgbEvaluator.getInstance().evaluate(darkIntensity, Integer.valueOf(this.mLightModeIconColorSingleTone), Integer.valueOf(this.mDarkModeIconColorSingleTone))).intValue();
        applyIconTint();
    }

    private void applyIconTint() {
        for (int i = 0; i < this.mReceivers.size(); i++) {
            this.mReceivers.valueAt(i).onDarkChanged(this.mTintArea, this.mDarkIntensity, this.mIconTint);
        }
    }
}
