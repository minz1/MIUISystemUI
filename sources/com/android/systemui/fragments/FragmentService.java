package com.android.systemui.fragments;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Handler;
import android.util.ArrayMap;
import android.view.View;
import com.android.systemui.ConfigurationChangedReceiver;

public class FragmentService implements ConfigurationChangedReceiver {
    /* access modifiers changed from: private */
    public final Context mContext;
    /* access modifiers changed from: private */
    public final Handler mHandler = new Handler();
    private final ArrayMap<View, FragmentHostState> mHosts = new ArrayMap<>();

    private class FragmentHostState {
        private FragmentHostManager mFragmentHostManager;
        private final View mView;

        public FragmentHostState(View view) {
            this.mView = view;
            this.mFragmentHostManager = new FragmentHostManager(FragmentService.this.mContext, FragmentService.this, this.mView);
        }

        public void sendConfigurationChange(final Configuration newConfig) {
            FragmentService.this.mHandler.post(new Runnable() {
                public void run() {
                    FragmentHostState.this.handleSendConfigurationChange(newConfig);
                }
            });
        }

        public FragmentHostManager getFragmentHostManager() {
            return this.mFragmentHostManager;
        }

        /* access modifiers changed from: private */
        public void handleSendConfigurationChange(Configuration newConfig) {
            this.mFragmentHostManager.onConfigurationChanged(newConfig);
        }
    }

    public FragmentService(Context context) {
        this.mContext = context;
    }

    public FragmentHostManager getFragmentHostManager(View view) {
        View root = view.getRootView();
        FragmentHostState state = this.mHosts.get(root);
        if (state == null) {
            state = new FragmentHostState(root);
            this.mHosts.put(root, state);
        }
        return state.getFragmentHostManager();
    }

    public void onConfigurationChanged(Configuration newConfig) {
        for (FragmentHostState state : this.mHosts.values()) {
            state.sendConfigurationChange(newConfig);
        }
    }
}
