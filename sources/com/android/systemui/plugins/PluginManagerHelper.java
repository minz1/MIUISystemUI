package com.android.systemui.plugins;

import android.text.TextUtils;
import android.util.Log;
import com.android.systemui.plugins.annotations.ProvidesInterface;

public abstract class PluginManagerHelper {
    private PluginManagerHelper() {
    }

    public static <P> String getAction(Class<P> cls) {
        ProvidesInterface info = (ProvidesInterface) cls.getDeclaredAnnotation(ProvidesInterface.class);
        if (info == null) {
            Log.d("PluginManagerHelper", cls + " doesn't provide an interface");
            return null;
        } else if (!TextUtils.isEmpty(info.action())) {
            return info.action();
        } else {
            Log.d("PluginManagerHelper", cls + " doesn't provide an action");
            return null;
        }
    }
}
