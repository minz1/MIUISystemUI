package com.android.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.ConnectivityManagerCompat;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Log;
import com.android.systemui.statusbar.policy.HotspotController;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

public class HotspotControllerImpl implements HotspotController {
    /* access modifiers changed from: private */
    public static final boolean DEBUG = Log.isLoggable("HotspotController", 3);
    private final ArrayList<HotspotController.Callback> mCallbacks = new ArrayList<>();
    private final ConnectivityManager mConnectivityManager;
    /* access modifiers changed from: private */
    public final Context mContext;
    /* access modifiers changed from: private */
    public int mHotspotState = 11;
    private final Receiver mReceiver = new Receiver();
    /* access modifiers changed from: private */
    public boolean mWaitingForCallback;
    private final WifiManager mWifiManager;

    private final class OnStartTetheringCallback extends ConnectivityManagerCompat.OnStartTetheringCallback {
        private OnStartTetheringCallback() {
        }

        public void onTetheringStarted() {
            if (HotspotControllerImpl.DEBUG) {
                Log.d("HotspotController", "onTetheringStarted");
            }
            boolean unused = HotspotControllerImpl.this.mWaitingForCallback = false;
        }

        public void onTetheringFailed() {
            if (HotspotControllerImpl.DEBUG) {
                Log.d("HotspotController", "onTetheringFailed");
            }
            boolean unused = HotspotControllerImpl.this.mWaitingForCallback = false;
            HotspotControllerImpl.this.fireCallback(HotspotControllerImpl.this.isHotspotEnabled());
        }
    }

    private final class Receiver extends BroadcastReceiver {
        private boolean mRegistered;

        private Receiver() {
        }

        public void setListening(boolean listening) {
            if (listening && !this.mRegistered) {
                if (HotspotControllerImpl.DEBUG) {
                    Log.d("HotspotController", "Registering receiver");
                }
                IntentFilter filter = new IntentFilter();
                filter.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");
                HotspotControllerImpl.this.mContext.registerReceiver(this, filter);
                this.mRegistered = true;
            } else if (!listening && this.mRegistered) {
                if (HotspotControllerImpl.DEBUG) {
                    Log.d("HotspotController", "Unregistering receiver");
                }
                HotspotControllerImpl.this.mContext.unregisterReceiver(this);
                this.mRegistered = false;
            }
        }

        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra("wifi_state", 14);
            if (HotspotControllerImpl.DEBUG) {
                Log.d("HotspotController", "onReceive " + state);
            }
            int unused = HotspotControllerImpl.this.mHotspotState = state;
            HotspotControllerImpl.this.fireCallback(HotspotControllerImpl.this.mHotspotState == 13);
        }
    }

    public HotspotControllerImpl(Context context) {
        this.mContext = context;
        this.mWifiManager = (WifiManager) context.getSystemService("wifi");
        this.mConnectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
    }

    public boolean isHotspotSupported() {
        return this.mConnectivityManager.isTetheringSupported() && this.mConnectivityManager.getTetherableWifiRegexs().length != 0 && UserManager.get(this.mContext).isAdminUser();
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("HotspotController state:");
        pw.print("  mHotspotEnabled=");
        pw.println(stateToString(this.mHotspotState));
    }

    private static String stateToString(int hotspotState) {
        switch (hotspotState) {
            case 10:
                return "DISABLING";
            case 11:
                return "DISABLED";
            case 12:
                return "ENABLING";
            case 13:
                return "ENABLED";
            case 14:
                return "FAILED";
            default:
                return null;
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:16:0x0046, code lost:
        return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void addCallback(com.android.systemui.statusbar.policy.HotspotController.Callback r5) {
        /*
            r4 = this;
            java.util.ArrayList<com.android.systemui.statusbar.policy.HotspotController$Callback> r0 = r4.mCallbacks
            monitor-enter(r0)
            if (r5 == 0) goto L_0x0045
            java.util.ArrayList<com.android.systemui.statusbar.policy.HotspotController$Callback> r1 = r4.mCallbacks     // Catch:{ all -> 0x0043 }
            boolean r1 = r1.contains(r5)     // Catch:{ all -> 0x0043 }
            if (r1 == 0) goto L_0x000e
            goto L_0x0045
        L_0x000e:
            boolean r1 = DEBUG     // Catch:{ all -> 0x0043 }
            if (r1 == 0) goto L_0x0028
            java.lang.String r1 = "HotspotController"
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch:{ all -> 0x0043 }
            r2.<init>()     // Catch:{ all -> 0x0043 }
            java.lang.String r3 = "addCallback "
            r2.append(r3)     // Catch:{ all -> 0x0043 }
            r2.append(r5)     // Catch:{ all -> 0x0043 }
            java.lang.String r2 = r2.toString()     // Catch:{ all -> 0x0043 }
            android.util.Log.d(r1, r2)     // Catch:{ all -> 0x0043 }
        L_0x0028:
            java.util.ArrayList<com.android.systemui.statusbar.policy.HotspotController$Callback> r1 = r4.mCallbacks     // Catch:{ all -> 0x0043 }
            r1.add(r5)     // Catch:{ all -> 0x0043 }
            com.android.systemui.statusbar.policy.HotspotControllerImpl$Receiver r1 = r4.mReceiver     // Catch:{ all -> 0x0043 }
            java.util.ArrayList<com.android.systemui.statusbar.policy.HotspotController$Callback> r2 = r4.mCallbacks     // Catch:{ all -> 0x0043 }
            boolean r2 = r2.isEmpty()     // Catch:{ all -> 0x0043 }
            r2 = r2 ^ 1
            r1.setListening(r2)     // Catch:{ all -> 0x0043 }
            boolean r1 = r4.isHotspotEnabled()     // Catch:{ all -> 0x0043 }
            r5.onHotspotChanged(r1)     // Catch:{ all -> 0x0043 }
            monitor-exit(r0)     // Catch:{ all -> 0x0043 }
            return
        L_0x0043:
            r1 = move-exception
            goto L_0x0047
        L_0x0045:
            monitor-exit(r0)     // Catch:{ all -> 0x0043 }
            return
        L_0x0047:
            monitor-exit(r0)     // Catch:{ all -> 0x0043 }
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.policy.HotspotControllerImpl.addCallback(com.android.systemui.statusbar.policy.HotspotController$Callback):void");
    }

    public void removeCallback(HotspotController.Callback callback) {
        if (callback != null) {
            if (DEBUG) {
                Log.d("HotspotController", "removeCallback " + callback);
            }
            synchronized (this.mCallbacks) {
                this.mCallbacks.remove(callback);
                this.mReceiver.setListening(!this.mCallbacks.isEmpty());
            }
        }
    }

    public boolean isHotspotEnabled() {
        return this.mHotspotState == 13;
    }

    public boolean isHotspotReady() {
        return this.mHotspotState == 13 || this.mHotspotState == 11 || this.mHotspotState == 14;
    }

    public boolean isHotspotTransient() {
        return this.mWaitingForCallback || this.mHotspotState == 12;
    }

    public void setHotspotEnabled(boolean enabled) {
        if (Build.VERSION.SDK_INT < 24) {
            setHotspotEnabledWithWifiManager(enabled);
        } else {
            setHotspotEnabledWithConnectivityManager(enabled);
        }
    }

    private void setHotspotEnabledWithConnectivityManager(boolean enabled) {
        Log.d("HotspotController", "setHotspotEnabledWithConnectivityManager: enabled=" + enabled);
        if (enabled) {
            OnStartTetheringCallback callback = new OnStartTetheringCallback();
            this.mWaitingForCallback = true;
            if (DEBUG) {
                Log.d("HotspotController", "Starting tethering");
            }
            ConnectivityManagerCompat.startTethering(this.mConnectivityManager, 0, false, callback);
            fireCallback(isHotspotEnabled());
            return;
        }
        ConnectivityManagerCompat.stopTethering(this.mConnectivityManager, 0);
    }

    private void setHotspotEnabledWithWifiManager(boolean enabled) {
        ContentResolver cr = this.mContext.getContentResolver();
        int wifiState = this.mWifiManager.getWifiState();
        if (Build.VERSION.SDK_INT < 23 && enabled && (wifiState == 2 || wifiState == 3)) {
            this.mWifiManager.setWifiEnabled(false);
            Settings.Global.putInt(cr, "wifi_saved_state", 1);
        }
        if (enabled) {
            ConnectivityManagerCompat.startTethering(this.mWifiManager);
        } else {
            ConnectivityManagerCompat.stopTethering(this.mWifiManager);
        }
        fireCallback(isHotspotEnabled());
        if (Build.VERSION.SDK_INT < 23 && !enabled && Settings.Global.getInt(cr, "wifi_saved_state", 0) == 1) {
            this.mWifiManager.setWifiEnabled(true);
            Settings.Global.putInt(cr, "wifi_saved_state", 0);
        }
    }

    /* access modifiers changed from: private */
    public void fireCallback(boolean isEnabled) {
        synchronized (this.mCallbacks) {
            Iterator<HotspotController.Callback> it = this.mCallbacks.iterator();
            while (it.hasNext()) {
                it.next().onHotspotChanged(isEnabled);
            }
        }
    }
}
