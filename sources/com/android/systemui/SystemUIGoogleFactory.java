package com.android.systemui;

import android.content.Context;
import android.util.ArrayMap;
import com.android.systemui.Dependency;
import com.android.systemui.assist.AssistManager;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;

public class SystemUIGoogleFactory extends SystemUIFactory {
    public void injectDependencies(ArrayMap<Object, Dependency.DependencyProvider> providers, final Context context) {
        super.injectDependencies(providers, context);
        providers.put(AssistManager.class, new Dependency.DependencyProvider() {
            public Object createDependency() {
                return new AssistManagerGoogle((DeviceProvisionedController) Dependency.get(DeviceProvisionedController.class), context);
            }
        });
    }
}
