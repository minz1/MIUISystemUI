package com.android.settingslib.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHidHost;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;
import java.util.List;

public class HidProfile implements LocalBluetoothProfile {
    /* access modifiers changed from: private */
    public static boolean V = true;
    /* access modifiers changed from: private */
    public final CachedBluetoothDeviceManager mDeviceManager;
    /* access modifiers changed from: private */
    public boolean mIsProfileReady;
    /* access modifiers changed from: private */
    public final LocalBluetoothAdapter mLocalAdapter;
    /* access modifiers changed from: private */
    public final LocalBluetoothProfileManager mProfileManager;
    /* access modifiers changed from: private */
    public BluetoothHidHost mService;

    private final class HidHostServiceListener implements BluetoothProfile.ServiceListener {
        private HidHostServiceListener() {
        }

        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (HidProfile.V) {
                Log.d("HidProfile", "Bluetooth service connected");
            }
            BluetoothHidHost unused = HidProfile.this.mService = (BluetoothHidHost) proxy;
            List<BluetoothDevice> deviceList = HidProfile.this.mService.getConnectedDevices();
            while (!deviceList.isEmpty()) {
                BluetoothDevice nextDevice = deviceList.remove(0);
                CachedBluetoothDevice device = HidProfile.this.mDeviceManager.findDevice(nextDevice);
                if (device == null) {
                    Log.w("HidProfile", "HidProfile found new device: " + nextDevice);
                    device = HidProfile.this.mDeviceManager.addDevice(HidProfile.this.mLocalAdapter, HidProfile.this.mProfileManager, nextDevice);
                }
                device.onProfileStateChanged(HidProfile.this, 2);
                device.refresh();
            }
            boolean unused2 = HidProfile.this.mIsProfileReady = true;
        }

        public void onServiceDisconnected(int profile) {
            if (HidProfile.V) {
                Log.d("HidProfile", "Bluetooth service disconnected");
            }
            boolean unused = HidProfile.this.mIsProfileReady = false;
        }
    }

    public boolean isProfileReady() {
        return this.mIsProfileReady;
    }

    public int getProfileId() {
        return 4;
    }

    HidProfile(Context context, LocalBluetoothAdapter adapter, CachedBluetoothDeviceManager deviceManager, LocalBluetoothProfileManager profileManager) {
        this.mLocalAdapter = adapter;
        this.mDeviceManager = deviceManager;
        this.mProfileManager = profileManager;
        adapter.getProfileProxy(context, new HidHostServiceListener(), 4);
    }

    public boolean isConnectable() {
        return true;
    }

    public boolean isAutoConnectable() {
        return true;
    }

    public boolean connect(BluetoothDevice device) {
        if (this.mService == null) {
            return false;
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
        int i = 0;
        if (this.mService == null) {
            return 0;
        }
        List<BluetoothDevice> deviceList = this.mService.getConnectedDevices();
        if (!deviceList.isEmpty() && deviceList.contains(device)) {
            i = this.mService.getConnectionState(device);
        }
        return i;
    }

    public boolean isPreferred(BluetoothDevice device) {
        boolean z = false;
        if (this.mService == null) {
            return false;
        }
        if (this.mService.getPriority(device) > 0) {
            z = true;
        }
        return z;
    }

    public void setPreferred(BluetoothDevice device, boolean preferred) {
        if (this.mService != null) {
            if (!preferred) {
                this.mService.setPriority(device, 0);
            } else if (this.mService.getPriority(device) < 100) {
                this.mService.setPriority(device, 100);
            }
        }
    }

    public String toString() {
        return "HID";
    }

    /* access modifiers changed from: protected */
    public void finalize() {
        if (V) {
            Log.d("HidProfile", "finalize()");
        }
        if (this.mService != null) {
            try {
                BluetoothAdapter.getDefaultAdapter().closeProfileProxy(4, this.mService);
                this.mService = null;
            } catch (Throwable t) {
                Log.w("HidProfile", "Error cleaning up HID proxy", t);
            }
        }
    }
}
