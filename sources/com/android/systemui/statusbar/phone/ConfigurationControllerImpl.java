package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.Configuration;
import com.android.systemui.ConfigurationChangedReceiver;
import com.android.systemui.Util;
import com.android.systemui.statusbar.policy.ConfigurationController;
import java.util.ArrayList;
import java.util.Iterator;

public class ConfigurationControllerImpl implements ConfigurationChangedReceiver, ConfigurationController {
    private int mDensity;
    private float mFontScale;
    private boolean mIsNightMode = false;
    private final ArrayList<ConfigurationController.ConfigurationListener> mListeners = new ArrayList<>();
    private Configuration mPreviousConfig;

    public ConfigurationControllerImpl(Context context) {
        Configuration currentConfig = context.getResources().getConfiguration();
        this.mFontScale = currentConfig.fontScale;
        this.mDensity = currentConfig.densityDpi;
        this.mPreviousConfig = new Configuration();
        this.mPreviousConfig.updateFrom(currentConfig);
        this.mIsNightMode = isNightMode(currentConfig);
    }

    public void onConfigurationChanged(Configuration newConfig) {
        Iterator<ConfigurationController.ConfigurationListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onConfigChanged(newConfig);
        }
        float fontScale = newConfig.fontScale;
        int density = newConfig.densityDpi;
        boolean themeChange = Util.isThemeResourcesChanged(this.mPreviousConfig.updateFrom(newConfig), newConfig.extraConfig.themeChangedFlags);
        this.mIsNightMode = isNightMode(newConfig);
        if (density != this.mDensity || this.mFontScale != fontScale || themeChange) {
            Iterator<ConfigurationController.ConfigurationListener> it2 = this.mListeners.iterator();
            while (it2.hasNext()) {
                it2.next().onDensityOrFontScaleChanged();
            }
            this.mDensity = density;
            this.mFontScale = fontScale;
        }
    }

    public void addCallback(ConfigurationController.ConfigurationListener listener) {
        this.mListeners.add(listener);
        listener.onDensityOrFontScaleChanged();
    }

    public void removeCallback(ConfigurationController.ConfigurationListener listener) {
        this.mListeners.remove(listener);
    }

    public boolean isNightMode() {
        return this.mIsNightMode;
    }

    private static boolean isNightMode(Configuration configuration) {
        return (configuration.uiMode & 48) == 32;
    }
}
