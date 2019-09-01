package com.android.systemui.fragments;

import android.content.Context;
import android.util.Log;
import android.view.View;
import com.android.systemui.Dependency;
import com.android.systemui.plugins.FragmentBase;
import com.android.systemui.plugins.Plugin;
import com.android.systemui.plugins.PluginListener;
import com.android.systemui.plugins.PluginManager;
import com.miui.systemui.support.v4.app.Fragment;

public class PluginFragmentListener implements PluginListener<Plugin> {
    private final Class<? extends Fragment> mDefaultClass;
    private final Class<? extends FragmentBase> mExpectedInterface;
    private final FragmentHostManager mFragmentHostManager;
    private final PluginManager mPluginManager = ((PluginManager) Dependency.get(PluginManager.class));
    private final String mTag;

    public PluginFragmentListener(View view, String tag, Class<? extends Fragment> defaultFragment, Class<? extends FragmentBase> expectedInterface) {
        this.mTag = tag;
        this.mFragmentHostManager = FragmentHostManager.get(view);
        this.mExpectedInterface = expectedInterface;
        this.mDefaultClass = defaultFragment;
    }

    public void startListening() {
        this.mPluginManager.addPluginListener(this, (Class<?>) this.mExpectedInterface, false);
    }

    public void onPluginConnected(Plugin plugin, Context pluginContext) {
        try {
            this.mExpectedInterface.cast(plugin);
            Fragment.class.cast(plugin);
            this.mFragmentHostManager.getPluginManager().setCurrentPlugin(this.mTag, plugin.getClass().getName(), pluginContext);
        } catch (ClassCastException e) {
            Log.e("PluginFragmentListener", plugin.getClass().getName() + " must be a Fragment and implement " + this.mExpectedInterface.getName(), e);
        }
    }

    public void onPluginDisconnected(Plugin plugin) {
        this.mFragmentHostManager.getPluginManager().removePlugin(this.mTag, plugin.getClass().getName(), this.mDefaultClass.getName());
    }
}
