package com.android.systemui.plugins;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;

public abstract class PluginFragment extends Fragment implements Plugin {
    private Context mPluginContext;

    public void onCreate(Context sysuiContext, Context pluginContext) {
        this.mPluginContext = pluginContext;
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    public Context getContext() {
        return this.mPluginContext;
    }
}
