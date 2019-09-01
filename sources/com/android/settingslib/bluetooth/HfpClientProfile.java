package com.android.settingslib.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadsetClient;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothUuid;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

final class HfpClientProfile implements LocalBluetoothProfile {
    static final ParcelUuid[] SRC_UUIDS = {BluetoothUuid.HSP_AG, BluetoothUuid.Handsfree_AG};
    /* access modifiers changed from: private */
    public static boolean V = false;
    /* access modifiers changed from: private */
    public final CachedBluetoothDeviceManager mDeviceManager;
    /* access modifiers changed from: private */
    public boolean mIsProfileReady;
    /* access modifiers changed from: private */
    public final LocalBluetoothAdapter mLocalAdapter;
    /* access modifiers changed from: private */
    public final LocalBluetoothProfileManager mProfileManager;
    /* access modifiers changed from: private */
    public BluetoothHeadsetClient mService;

    private final class HfpClientServiceListener implements BluetoothProfile.ServiceListener {
        private HfpClientServiceListener() {
        }

        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (HfpClientProfile.V) {
                Log.d("HfpClientProfile", "Bluetooth service connected");
            }
            BluetoothHeadsetClient unused = HfpClientProfile.this.mService = (BluetoothHeadsetClient) proxy;
            List<BluetoothDevice> deviceList = HfpClientProfile.this.mService.getConnectedDevices();
            while (!deviceList.isEmpty()) {
                BluetoothDevice nextDevice = deviceList.remove(0);
                CachedBluetoothDevice device = HfpClientProfile.this.mDeviceManager.findDevice(nextDevice);
                if (device == null) {
                    Log.w("HfpClientProfile", "HfpClient profile found new device: " + nextDevice);
                    device = HfpClientProfile.this.mDeviceManager.addDevice(HfpClientProfile.this.mLocalAdapter, HfpClientProfile.this.mProfileManager, nextDevice);
                }
                device.onProfileStateChanged(HfpClientProfile.this, 2);
                device.refresh();
            }
            boolean unused2 = HfpClientProfile.this.mIsProfileReady = true;
        }

        public void onServiceDisconnected(int profile) {
            if (HfpClientProfile.V) {
                Log.d("HfpClientProfile", "Bluetooth service disconnected");
            }
            boolean unused = HfpClientProfile.this.mIsProfileReady = false;
        }
    }

    public boolean isProfileReady() {
        return this.mIsProfileReady;
    }

    public int getProfileId() {
        return 16;
    }

    HfpClientProfile(Context context, LocalBluetoothAdapter adapter, CachedBluetoothDeviceManager deviceManager, LocalBluetoothProfileManager profileManager) {
        this.mLocalAdapter = adapter;
        this.mDeviceManager = deviceManager;
        this.mProfileManager = profileManager;
        this.mLocalAdapter.getProfileProxy(context, new HfpClientServiceListener(), 16);
    }

    public boolean isConnectable() {
        return true;
    }

    public boolean isAutoConnectable() {
        return true;
    }

    public List<BluetoothDevice> getConnectedDevices() {
        if (this.mService == null) {
            return new ArrayList(0);
        }
        return this.mService.getDevicesMatchingConnectionStates(new int[]{2, 1, 3});
    }

    public boolean connect(BluetoothDevice device) {
        if (this.mService == null) {
            return false;
        }
        List<BluetoothDevice> srcs = getConnectedDevices();
        if (srcs != null) {
            for (BluetoothDevice src : srcs) {
                if (src.equals(device)) {
                    Log.d("HfpClientProfile", "Ignoring Connect");
                    return true;
                }
            }
        }
        return this.mService.connect(device);
    }

    public boolean disconnect(BluetoothDevice device) {
        if (this.mService == null) {
            return false;
        }
        if (this.mService.getPriority(device) > 100) {
            this.mService.setPriority(device, 100);
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
        return "HEADSET_CLIENT";
    }

    /* access modifiers changed from: protected */
    public void finalize() {
        if (V) {
            Log.d("HfpClientProfile", "finalize()");
        }
        if (this.mService != null) {
            try {
                BluetoothAdapter.getDefaultAdapter().closeProfileProxy(16, this.mService);
                this.mService = null;
            } catch (Throwable t) {
                Log.w("HfpClientProfile", "Error cleaning up HfpClient proxy", t);
            }
        }
    }
}
