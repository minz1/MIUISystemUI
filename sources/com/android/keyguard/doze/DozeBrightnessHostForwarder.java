package com.android.keyguard.doze;

import com.android.keyguard.doze.DozeMachine;
import com.android.systemui.doze.DozeHost;

public class DozeBrightnessHostForwarder extends DozeMachine.Service.Delegate {
    private final DozeHost mHost;

    public DozeBrightnessHostForwarder(DozeMachine.Service wrappedService, DozeHost host) {
        super(wrappedService);
        this.mHost = host;
    }

    public void setDozeScreenBrightness(int brightness) {
        super.setDozeScreenBrightness(brightness);
        this.mHost.setDozeScreenBrightness(brightness);
    }
}
