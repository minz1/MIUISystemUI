package com.android.keyguard.phone;

import android.content.Context;
import android.os.SystemProperties;
import com.android.internal.hardware.AmbientDisplayConfiguration;
import com.android.systemui.R;

public class DozeParameters {
    private final AmbientDisplayConfiguration mAmbientDisplayConfiguration = new AmbientDisplayConfiguration(this.mContext);
    private final Context mContext;

    public DozeParameters(Context context) {
        this.mContext = context;
    }

    public boolean getDisplayStateSupported() {
        return getBoolean("doze.display.supported", R.bool.doze_display_state_supported);
    }

    private boolean getBoolean(String propName, int resId) {
        return SystemProperties.getBoolean(propName, this.mContext.getResources().getBoolean(resId));
    }

    public boolean getDozeSuspendDisplayStateSupported() {
        return this.mContext.getResources().getBoolean(R.bool.doze_suspend_display_state_supported);
    }
}
