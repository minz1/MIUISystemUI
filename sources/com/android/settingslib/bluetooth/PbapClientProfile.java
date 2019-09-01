package com.android.settingslib.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothPbapClient;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothUuid;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public final class PbapClientProfile implements LocalBluetoothProfile {
    static final ParcelUuid[] SRC_UUIDS = {BluetoothUuid.PBAP_PSE};
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
    public BluetoothPbapClient mService;

    private final class PbapClientServiceListener implements BluetoothProfile.ServiceListener {
        private PbapClientServiceListener() {
        }

        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (PbapClientProfile.V) {
                Log.d("PbapClientProfile", "Bluetooth service connected");
            }
            BluetoothPbapClient unused = PbapClientProfile.this.mService = (BluetoothPbapClient) proxy;
            List<BluetoothDevice> deviceList = PbapClientProfile.this.mService.getConnectedDevices();
            while (!deviceList.isEmpty()) {
                BluetoothDevice nextDevice = deviceList.remove(0);
                CachedBluetoothDevice device = PbapClientProfile.this.mDeviceManager.findDevice(nextDevice);
                if (device == null) {
                    Log.w("PbapClientProfile", "PbapClientProfile found new device: " + nextDevice);
                    device = PbapClientProfile.this.mDeviceManager.addDevice(PbapClientProfile.this.mLocalAdapter, PbapClientProfile.this.mProfileManager, nextDevice);
                }
                device.onProfileStateChanged(PbapClientProfile.this, 2);
                device.refresh();
            }
            boolean unused2 = PbapClientProfile.this.mIsProfileReady = true;
        }

        public void onServiceDisconnected(int profile) {
            if (PbapClientProfile.V) {
                Log.d("PbapClientProfile", "Bluetooth service disconnected");
            }
            boolean unused = PbapClientProfile.this.mIsProfileReady = false;
        }
    }

    public boolean isProfileReady() {
        return this.mIsProfileReady;
    }

    public int getProfileId() {
        return 17;
    }

    PbapClientProfile(Context context, LocalBluetoothAdapter adapter, CachedBluetoothDeviceManager deviceManager, LocalBluetoothProfileManager profileManager) {
        this.mLocalAdapter = adapter;
        this.mDeviceManager = deviceManager;
        this.mProfileManager = profileManager;
        this.mLocalAdapter.getProfileProxy(context, new PbapClientServiceListener(), 17);
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
        if (V) {
            Log.d("PbapClientProfile", "PBAPClientProfile got connect request");
        }
        if (this.mService == null) {
            return false;
        }
        List<BluetoothDevice> srcs = getConnectedDevices();
        if (srcs != null) {
            for (BluetoothDevice src : srcs) {
                if (src.equals(device)) {
                    Log.d("PbapClientProfile", "Ignoring Connect");
                    return true;
                }
            }
        }
        Log.d("PbapClientProfile", "PBAPClientProfile attempting to connect to " + device.getAddress());
        return this.mService.connect(device);
    }

    public boolean disconnect(BluetoothDevice device) {
        if (V) {
            Log.d("PbapClientProfile", "PBAPClientProfile got disconnect request");
        }
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
        return "PbapClient";
    }

    /* access modifiers changed from: protected */
    public void finalize() {
        if (V) {
            Log.d("PbapClientProfile", "finalize()");
        }
        if (this.mService != null) {
            try {
                BluetoothAdapter.getDefaultAdapter().closeProfileProxy(17, this.mService);
                this.mService = null;
            } catch (Throwable t) {
                Log.w("PbapClientProfile", "Error cleaning up PBAP Client proxy", t);
            }
        }
    }
}
