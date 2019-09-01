package com.android.settingslib.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothPan;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;
import java.util.HashMap;
import java.util.List;

public class PanProfile implements LocalBluetoothProfile {
    /* access modifiers changed from: private */
    public static boolean V = true;
    private final HashMap<BluetoothDevice, Integer> mDeviceRoleMap = new HashMap<>();
    /* access modifiers changed from: private */
    public boolean mIsProfileReady;
    private final LocalBluetoothAdapter mLocalAdapter;
    /* access modifiers changed from: private */
    public BluetoothPan mService;

    private final class PanServiceListener implements BluetoothProfile.ServiceListener {
        private PanServiceListener() {
        }

        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (PanProfile.V) {
                Log.d("PanProfile", "Bluetooth service connected");
            }
            BluetoothPan unused = PanProfile.this.mService = (BluetoothPan) proxy;
            boolean unused2 = PanProfile.this.mIsProfileReady = true;
        }

        public void onServiceDisconnected(int profile) {
            if (PanProfile.V) {
                Log.d("PanProfile", "Bluetooth service disconnected");
            }
            boolean unused = PanProfile.this.mIsProfileReady = false;
        }
    }

    public boolean isProfileReady() {
        return this.mIsProfileReady;
    }

    public int getProfileId() {
        return 5;
    }

    PanProfile(Context context, LocalBluetoothAdapter adapter) {
        this.mLocalAdapter = adapter;
        this.mLocalAdapter.getProfileProxy(context, new PanServiceListener(), 5);
    }

    public boolean isConnectable() {
        return true;
    }

    public boolean isAutoConnectable() {
        return false;
    }

    public boolean connect(BluetoothDevice device) {
        if (this.mService == null) {
            return false;
        }
        List<BluetoothDevice> sinks = this.mService.getConnectedDevices();
        if (sinks != null) {
            for (BluetoothDevice sink : sinks) {
                this.mService.disconnect(sink);
            }
        }
        return this.mService.connect(device);
    }

    public boolean disconnect(BluetoothDevice device) {
        if (this.mService == null) {
            return false;
        }
        return this.mService.disconnect(device);
    }

    public int getConnectionStatus(BluetoothDevice device) {
        if (this.mService == null) {
            return 0;
        }
        return this.mService.getConnectionState(device);
    }

    public boolean isPreferred(BluetoothDevice device) {
        return true;
    }

    public void setPreferred(BluetoothDevice device, boolean preferred) {
    }

    public String toString() {
        return "PAN";
    }

    /* access modifiers changed from: package-private */
    public void setLocalRole(BluetoothDevice device, int role) {
        this.mDeviceRoleMap.put(device, Integer.valueOf(role));
    }

    /* access modifiers changed from: package-private */
    public boolean isLocalRoleNap(BluetoothDevice device) {
        boolean z = false;
        if (!this.mDeviceRoleMap.containsKey(device)) {
            return false;
        }
        if (this.mDeviceRoleMap.get(device).intValue() == 1) {
            z = true;
        }
        return z;
    }

    /* access modifiers changed from: protected */
    public void finalize() {
        if (V) {
            Log.d("PanProfile", "finalize()");
        }
        if (this.mService != null) {
            try {
                BluetoothAdapter.getDefaultAdapter().closeProfileProxy(5, this.mService);
                this.mService = null;
            } catch (Throwable t) {
                Log.w("PanProfile", "Error cleaning up PAN proxy", t);
            }
        }
    }
}
