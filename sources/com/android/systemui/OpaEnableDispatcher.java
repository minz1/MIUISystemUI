package com.android.systemui;

import android.content.ComponentName;
import android.content.Context;
import com.android.internal.app.AssistUtils;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.statusbar.phone.StatusBar;

public class OpaEnableDispatcher {
    private final AssistUtils mAssistUtils;
    private final Context mContext;

    public OpaEnableDispatcher(Context context, AssistUtils assistUtils) {
        this.mContext = context;
        this.mAssistUtils = assistUtils;
    }

    public void dispatchOpaEnabled(boolean enabled) {
        dispatchUnchecked(enabled && isGsaCurrentAssistant());
    }

    private void dispatchUnchecked(boolean enabled) {
        StatusBar bar = (StatusBar) ((Application) this.mContext.getApplicationContext()).getSystemUIApplication().getComponent(StatusBar.class);
        if (bar != null && bar.getNavigationBarView() != null) {
            bar.getNavigationBarView().setOpaEnabled(enabled);
        }
    }

    private boolean isGsaCurrentAssistant() {
        ComponentName assistant = this.mAssistUtils.getAssistComponentForUser(KeyguardUpdateMonitor.getCurrentUser());
        return assistant != null && "com.google.android.googlequicksearchbox/com.google.android.voiceinteraction.GsaVoiceInteractionService".equals(assistant.flattenToString());
    }
}
