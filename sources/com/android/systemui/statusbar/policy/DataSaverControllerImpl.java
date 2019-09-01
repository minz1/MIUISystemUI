package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.net.INetworkPolicyListener;
import android.net.NetworkPolicyManager;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import com.android.systemui.statusbar.policy.DataSaverController;
import java.util.ArrayList;

public class DataSaverControllerImpl implements DataSaverController {
    /* access modifiers changed from: private */
    public final Handler mHandler = new Handler(Looper.getMainLooper());
    private final ArrayList<DataSaverController.Listener> mListeners = new ArrayList<>();
    private final INetworkPolicyListener mPolicyListener = new INetworkPolicyListener.Stub() {
        public void onUidRulesChanged(int uid, int uidRules) throws RemoteException {
        }

        public void onMeteredIfacesChanged(String[] strings) throws RemoteException {
        }

        public void onRestrictBackgroundChanged(final boolean isDataSaving) throws RemoteException {
            DataSaverControllerImpl.this.mHandler.post(new Runnable() {
                public void run() {
                    DataSaverControllerImpl.this.handleRestrictBackgroundChanged(isDataSaving);
                }
            });
        }

        public void onUidPoliciesChanged(int uid, int uidPolicies) throws RemoteException {
        }

        public void onSubscriptionOverride(int subId, int overrideMask, int overrideValue) {
        }
    };
    private final NetworkPolicyManager mPolicyManager;

    public DataSaverControllerImpl(Context context) {
        this.mPolicyManager = NetworkPolicyManager.from(context);
    }

    /* access modifiers changed from: private */
    public void handleRestrictBackgroundChanged(boolean isDataSaving) {
        synchronized (this.mListeners) {
            for (int i = 0; i < this.mListeners.size(); i++) {
                this.mListeners.get(i).onDataSaverChanged(isDataSaving);
            }
        }
    }

    public void addCallback(DataSaverController.Listener listener) {
        synchronized (this.mListeners) {
            this.mListeners.add(listener);
            if (this.mListeners.size() == 1) {
                this.mPolicyManager.registerListener(this.mPolicyListener);
            }
        }
        listener.onDataSaverChanged(isDataSaverEnabled());
    }

    public void removeCallback(DataSaverController.Listener listener) {
        synchronized (this.mListeners) {
            this.mListeners.remove(listener);
            if (this.mListeners.size() == 0) {
                this.mPolicyManager.unregisterListener(this.mPolicyListener);
            }
        }
    }

    public boolean isDataSaverEnabled() {
        return this.mPolicyManager.getRestrictBackground();
    }

    public void setDataSaverEnabled(boolean enabled) {
        this.mPolicyManager.setRestrictBackground(enabled);
        try {
            this.mPolicyListener.onRestrictBackgroundChanged(enabled);
        } catch (RemoteException e) {
        }
    }
}
