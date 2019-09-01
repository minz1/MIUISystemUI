package com.android.systemui.plugins;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.ArraySet;
import java.util.Set;

public class PluginPrefs {
    private final Set<String> mPluginActions = new ArraySet(this.mSharedPrefs.getStringSet("actions", null));
    private final SharedPreferences mSharedPrefs;

    public PluginPrefs(Context context) {
        this.mSharedPrefs = context.getSharedPreferences("plugin_prefs", 0);
    }

    public Set<String> getPluginList() {
        return this.mPluginActions;
    }

    public synchronized void addAction(String action) {
        if (this.mPluginActions.add(action)) {
            this.mSharedPrefs.edit().putStringSet("actions", this.mPluginActions).commit();
        }
    }

    public static boolean hasPlugins(Context context) {
        return context.getSharedPreferences("plugin_prefs", 0).getBoolean("plugins", false);
    }

    public static void setHasPlugins(Context context) {
        context.getSharedPreferences("plugin_prefs", 0).edit().putBoolean("plugins", true).commit();
    }
}
