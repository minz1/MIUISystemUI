package com.android.settingslib.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothUuid;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;
import java.util.Iterator;
import java.util.List;

public class HeadsetProfile implements LocalBluetoothProfile {
    static final ParcelUuid[] UUIDS = {BluetoothUuid.HSP, BluetoothUuid.Handsfree};
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
    public BluetoothHeadset mService;

    private final class HeadsetServiceListener implements BluetoothProfile.ServiceListener {
        private HeadsetServiceListener() {
        }

        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (HeadsetProfile.V) {
                Log.d("HeadsetProfile", "Bluetooth service connected");
            }
            BluetoothHeadset unused = HeadsetProfile.this.mService = (BluetoothHeadset) proxy;
            List<BluetoothDevice> deviceList = HeadsetProfile.this.mService.getConnectedDevices();
            while (!deviceList.isEmpty()) {
                BluetoothDevice nextDevice = deviceList.remove(0);
                CachedBluetoothDevice device = HeadsetProfile.this.mDeviceManager.findDevice(nextDevice);
                if (device == null) {
                    Log.w("HeadsetProfile", "HeadsetProfile found new device: " + nextDevice);
                    device = HeadsetProfile.this.mDeviceManager.addDevice(HeadsetProfile.this.mLocalAdapter, HeadsetProfile.this.mProfileManager, nextDevice);
                }
                device.onProfileStateChanged(HeadsetProfile.this, 2);
                device.refresh();
            }
            HeadsetProfile.this.mProfileManager.callServiceConnectedListeners();
            boolean unused2 = HeadsetProfile.this.mIsProfileReady = true;
        }

        public void onServiceDisconnected(int profile) {
            if (HeadsetProfile.V) {
                Log.d("HeadsetProfile", "Bluetooth service disconnected");
            }
            HeadsetProfile.this.mProfileManager.callServiceDisconnectedListeners();
            boolean unused = HeadsetProfile.this.mIsProfileReady = false;
        }
    }

    public boolean isProfileReady() {
        return this.mIsProfileReady;
    }

    public int getProfileId() {
        return 1;
    }

    HeadsetProfile(Context context, LocalBluetoothAdapter adapter, CachedBluetoothDeviceManager deviceManager, LocalBluetoothProfileManager profileManager) {
        this.mLocalAdapter = adapter;
        this.mDeviceManager = deviceManager;
        this.mProfileManager = profileManager;
        this.mLocalAdapter.getProfileProxy(context, new HeadsetServiceListener(), 1);
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
        List<BluetoothDevice> sinks = this.mService.getConnectedDevices();
        if (sinks != null) {
            Iterator<BluetoothDevice> it = sinks.iterator();
            while (it.hasNext()) {
                Log.d("HeadsetProfile", "Not disconnecting device = " + it.next());
            }
        }
        return this.mService.connect(device);
    }

    public boolean disconnect(BluetoothDevice device) {
        if (this.mService == null) {
            return false;
        }
        List<BluetoothDevice> deviceList = this.mService.getConnectedDevices();
        if (!deviceList.isEmpty()) {
            for (BluetoothDevice dev : deviceList) {
                if (dev.equals(device)) {
                    if (V) {
                        Log.d("HeadsetProfile", "Downgrade priority as useris disconnecting the headset");
                    }
                    if (this.mService.getPriority(device) > 100) {
                        this.mService.setPriority(device, 100);
                    }
                    return this.mService.disconnect(device);
                }
            }
        }
        return false;
    }

    public int getConnectionStatus(BluetoothDevice device) {
        if (this.mService == null) {
            return 0;
        }
        List<BluetoothDevice> deviceList = this.mService.getConnectedDevices();
        if (!deviceList.isEmpty()) {
            for (BluetoothDevice dev : deviceList) {
                if (dev.equals(device)) {
                    return this.mService.getConnectionState(device);
                }
            }
        }
        return 0;
    }

    public boolean setActiveDevice(BluetoothDevice device) {
        if (this.mService == null) {
            return false;
        }
        return this.mService.setActiveDevice(device);
    }

    public BluetoothDevice getActiveDevice() {
        if (this.mService == null) {
            return null;
        }
        return this.mService.getActiveDevice();
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
        return "HEADSET";
    }

    /* access modifiers changed from: protected */
    public void finalize() {
        if (V) {
            Log.d("HeadsetProfile", "finalize()");
        }
        if (this.mService != null) {
            try {
                BluetoothAdapter.getDefaultAdapter().closeProfileProxy(1, this.mService);
                this.mService = null;
            } catch (Throwable t) {
                Log.w("HeadsetProfile", "Error cleaning up HID proxy", t);
            }
        }
    }
}
