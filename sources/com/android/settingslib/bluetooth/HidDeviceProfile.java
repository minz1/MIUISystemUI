package com.android.settingslib.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHidDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;
import java.util.List;

public class HidDeviceProfile implements LocalBluetoothProfile {
    /* access modifiers changed from: private */
    public final CachedBluetoothDeviceManager mDeviceManager;
    /* access modifiers changed from: private */
    public boolean mIsProfileReady;
    /* access modifiers changed from: private */
    public final LocalBluetoothAdapter mLocalAdapter;
    /* access modifiers changed from: private */
    public final LocalBluetoothProfileManager mProfileManager;
    /* access modifiers changed from: private */
    public BluetoothHidDevice mService;

    private final class HidDeviceServiceListener implements BluetoothProfile.ServiceListener {
        private HidDeviceServiceListener() {
        }

        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            Log.d("HidDeviceProfile", "Bluetooth service connected :-)");
            BluetoothHidDevice unused = HidDeviceProfile.this.mService = (BluetoothHidDevice) proxy;
            for (BluetoothDevice nextDevice : HidDeviceProfile.this.mService.getConnectedDevices()) {
                CachedBluetoothDevice device = HidDeviceProfile.this.mDeviceManager.findDevice(nextDevice);
                if (device == null) {
                    Log.w("HidDeviceProfile", "HidProfile found new device: " + nextDevice);
                    device = HidDeviceProfile.this.mDeviceManager.addDevice(HidDeviceProfile.this.mLocalAdapter, HidDeviceProfile.this.mProfileManager, nextDevice);
                }
                Log.d("HidDeviceProfile", "Connection status changed: " + device);
                device.onProfileStateChanged(HidDeviceProfile.this, 2);
                device.refresh();
            }
            boolean unused2 = HidDeviceProfile.this.mIsProfileReady = true;
        }

        public void onServiceDisconnected(int profile) {
            Log.d("HidDeviceProfile", "Bluetooth service disconnected");
            boolean unused = HidDeviceProfile.this.mIsProfileReady = false;
        }
    }

    HidDeviceProfile(Context context, LocalBluetoothAdapter adapter, CachedBluetoothDeviceManager deviceManager, LocalBluetoothProfileManager profileManager) {
        this.mLocalAdapter = adapter;
        this.mDeviceManager = deviceManager;
        this.mProfileManager = profileManager;
        adapter.getProfileProxy(context, new HidDeviceServiceListener(), 19);
    }

    public boolean isProfileReady() {
        return this.mIsProfileReady;
    }

    public int getProfileId() {
        return 19;
    }

    public boolean isConnectable() {
        return true;
    }

    public boolean isAutoConnectable() {
        return false;
    }

    public boolean connect(BluetoothDevice device) {
        return false;
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
        return getConnectionStatus(device) != 0;
    }

    public void setPreferred(BluetoothDevice device, boolean preferred) {
        if (!preferred) {
            this.mService.disconnect(device);
        }
    }

    public String toString() {
        return "HID DEVICE";
    }

    /* access modifiers changed from: protected */
    public void finalize() {
        Log.d("HidDeviceProfile", "finalize()");
        if (this.mService != null) {
            try {
                BluetoothAdapter.getDefaultAdapter().closeProfileProxy(19, this.mService);
                this.mService = null;
            } catch (Throwable t) {
                Log.w("HidDeviceProfile", "Error cleaning up HID proxy", t);
            }
        }
    }
}
