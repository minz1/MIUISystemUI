package com.android.settingslib.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothMapClient;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothUuid;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public final class MapClientProfile implements LocalBluetoothProfile {
    static final ParcelUuid[] UUIDS = {BluetoothUuid.MAP, BluetoothUuid.MNS, BluetoothUuid.MAS};
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
    public BluetoothMapClient mService;

    private final class MapClientServiceListener implements BluetoothProfile.ServiceListener {
        private MapClientServiceListener() {
        }

        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (MapClientProfile.V) {
                Log.d("MapClientProfile", "Bluetooth service connected");
            }
            BluetoothMapClient unused = MapClientProfile.this.mService = (BluetoothMapClient) proxy;
            List<BluetoothDevice> deviceList = MapClientProfile.this.mService.getConnectedDevices();
            while (!deviceList.isEmpty()) {
                BluetoothDevice nextDevice = deviceList.remove(0);
                CachedBluetoothDevice device = MapClientProfile.this.mDeviceManager.findDevice(nextDevice);
                if (device == null) {
                    Log.w("MapClientProfile", "MapProfile found new device: " + nextDevice);
                    device = MapClientProfile.this.mDeviceManager.addDevice(MapClientProfile.this.mLocalAdapter, MapClientProfile.this.mProfileManager, nextDevice);
                }
                device.onProfileStateChanged(MapClientProfile.this, 2);
                device.refresh();
            }
            MapClientProfile.this.mProfileManager.callServiceConnectedListeners();
            boolean unused2 = MapClientProfile.this.mIsProfileReady = true;
        }

        public void onServiceDisconnected(int profile) {
            if (MapClientProfile.V) {
                Log.d("MapClientProfile", "Bluetooth service disconnected");
            }
            MapClientProfile.this.mProfileManager.callServiceDisconnectedListeners();
            boolean unused = MapClientProfile.this.mIsProfileReady = false;
        }
    }

    public boolean isProfileReady() {
        if (V) {
            Log.d("MapClientProfile", "isProfileReady(): " + this.mIsProfileReady);
        }
        return this.mIsProfileReady;
    }

    public int getProfileId() {
        return 18;
    }

    MapClientProfile(Context context, LocalBluetoothAdapter adapter, CachedBluetoothDeviceManager deviceManager, LocalBluetoothProfileManager profileManager) {
        this.mLocalAdapter = adapter;
        this.mDeviceManager = deviceManager;
        this.mProfileManager = profileManager;
        this.mLocalAdapter.getProfileProxy(context, new MapClientServiceListener(), 18);
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
        List<BluetoothDevice> connectedDevices = getConnectedDevices();
        if (connectedDevices == null || !connectedDevices.contains(device)) {
            return this.mService.connect(device);
        }
        Log.d("MapClientProfile", "Ignoring Connect");
        return true;
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

    public List<BluetoothDevice> getConnectedDevices() {
        if (this.mService == null) {
            return new ArrayList(0);
        }
        return this.mService.getDevicesMatchingConnectionStates(new int[]{2, 1, 3});
    }

    public String toString() {
        return "MAP Client";
    }

    /* access modifiers changed from: protected */
    public void finalize() {
        if (V) {
            Log.d("MapClientProfile", "finalize()");
        }
        if (this.mService != null) {
            try {
                BluetoothAdapter.getDefaultAdapter().closeProfileProxy(18, this.mService);
                this.mService = null;
            } catch (Throwable t) {
                Log.w("MapClientProfile", "Error cleaning up MAP Client proxy", t);
            }
        }
    }
}
