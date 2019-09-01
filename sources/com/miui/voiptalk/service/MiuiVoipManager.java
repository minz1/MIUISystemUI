package com.miui.voiptalk.service;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.MiuiSettings;
import android.util.Slog;
import com.miui.voiptalk.service.IMiuiVoipService;

public class MiuiVoipManager {
    private static MiuiVoipManager INSTANCE = null;
    private Context mContext;
    private boolean mHasInit;
    /* access modifiers changed from: private */
    public IMiuiVoipService mMiuiVoipService;
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder binder) {
            IMiuiVoipService unused = MiuiVoipManager.this.mMiuiVoipService = IMiuiVoipService.Stub.asInterface(binder);
            Slog.d("MiuiVoipManager", " onServiceConnected");
        }

        public void onServiceDisconnected(ComponentName name) {
            IMiuiVoipService unused = MiuiVoipManager.this.mMiuiVoipService = null;
            Slog.d("MiuiVoipManager", " onServiceDisconnected");
        }
    };
    private VoIPStatusReceiver mVoIPStatusReceiver;

    private class VoIPStatusReceiver extends BroadcastReceiver {
        private VoIPStatusReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                boolean isVoipEnabled = intent.getBooleanExtra("extra_activated", false);
                Slog.d("MiuiVoipManager", " onReceive isVoipEnabled: " + isVoipEnabled);
                if (isVoipEnabled) {
                    MiuiVoipManager.this.init();
                }
            }
        }
    }

    private MiuiVoipManager(Context context) {
        this.mContext = context;
        this.mHasInit = false;
    }

    public static synchronized MiuiVoipManager getInstance(Context context) {
        MiuiVoipManager miuiVoipManager;
        synchronized (MiuiVoipManager.class) {
            if (INSTANCE == null) {
                INSTANCE = new MiuiVoipManager(context);
            }
            miuiVoipManager = INSTANCE;
        }
        return miuiVoipManager;
    }

    public void init() {
        if (this.mHasInit || !MiuiSettings.MiuiVoip.isVoipEnabled(this.mContext)) {
            registerVoIPStatusReceiver();
            return;
        }
        Intent intent = new Intent("com.miui.voip.REMOTE_SERVICE");
        intent.setPackage("com.miui.voip");
        this.mContext.bindService(intent, this.mServiceConnection, 1);
        this.mHasInit = true;
        Slog.e("MiuiVoipManager", "init success");
    }

    public synchronized void endCall() {
        try {
            if (this.mMiuiVoipService != null) {
                this.mMiuiVoipService.endCall();
            } else {
                Slog.e("MiuiVoipManager", "MiuiVoipService is Null !");
            }
        } catch (RemoteException e) {
            Slog.e("MiuiVoipManager", "endCall", e);
        }
        return;
    }

    public synchronized void answerRingingCall() {
        try {
            if (this.mMiuiVoipService != null) {
                this.mMiuiVoipService.answerRingingCall();
            } else {
                Slog.e("MiuiVoipManager", "MiuiVoipService is Null !");
            }
        } catch (RemoteException e) {
            Slog.e("MiuiVoipManager", "answerRingingCall", e);
        }
        return;
    }

    public synchronized int getCallState() {
        try {
            if (this.mMiuiVoipService != null) {
                return this.mMiuiVoipService.getCallState();
            }
            Slog.e("MiuiVoipManager", "MiuiVoipService is Null !");
            return -1;
        } catch (RemoteException e) {
            Slog.e("MiuiVoipManager", "getCallState", e);
        }
    }

    public synchronized long getCallBaseTime() {
        try {
            if (this.mMiuiVoipService != null) {
                return this.mMiuiVoipService.getCallBaseTime();
            }
        } catch (RemoteException e) {
            Slog.e("MiuiVoipManager", "getCallBaseTime", e);
        }
        return 0;
    }

    public synchronized String getExtraCallState() {
        try {
            if (this.mMiuiVoipService != null) {
                return this.mMiuiVoipService.getExtraCallState();
            }
        } catch (RemoteException e) {
            Slog.e("MiuiVoipManager", "getExtraCallState", e);
        }
        return "";
    }

    public synchronized boolean isVoipCallUiOnBack() {
        try {
            if (this.mMiuiVoipService != null) {
                return this.mMiuiVoipService.isVoipCallUiOnBack();
            }
        } catch (RemoteException e) {
            Slog.e("MiuiVoipManager", "isVoipCallUiOnBack", e);
        }
        return false;
    }

    public synchronized boolean isVideoCall() {
        try {
            if (this.mMiuiVoipService != null) {
                return this.mMiuiVoipService.isVideoCall();
            }
        } catch (RemoteException e) {
            Slog.e("MiuiVoipManager", "isVideoCall", e);
        }
        return false;
    }

    private void registerVoIPStatusReceiver() {
        if (this.mVoIPStatusReceiver == null) {
            this.mVoIPStatusReceiver = new VoIPStatusReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction("com.miui.voip.action.ACTIVATE_STATUS_CHANGE");
            this.mContext.registerReceiver(this.mVoIPStatusReceiver, filter);
        }
    }
}
