package com.android.settingslib.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHearingAid;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class HearingAidProfile implements LocalBluetoothProfile {
    /* access modifiers changed from: private */
    public static boolean V = true;
    private Context mContext;
    /* access modifiers changed from: private */
    public final CachedBluetoothDeviceManager mDeviceManager;
    /* access modifiers changed from: private */
    public boolean mIsProfileReady;
    /* access modifiers changed from: private */
    public final LocalBluetoothAdapter mLocalAdapter;
    /* access modifiers changed from: private */
    public final LocalBluetoothProfileManager mProfileManager;
    /* access modifiers changed from: private */
    public BluetoothHearingAid mService;

    private final class HearingAidServiceListener implements BluetoothProfile.ServiceListener {
        private HearingAidServiceListener() {
        }

        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (HearingAidProfile.V) {
                Log.d("HearingAidProfile", "Bluetooth service connected");
            }
            BluetoothHearingAid unused = HearingAidProfile.this.mService = (BluetoothHearingAid) proxy;
            List<BluetoothDevice> deviceList = HearingAidProfile.this.mService.getConnectedDevices();
            while (!deviceList.isEmpty()) {
                BluetoothDevice nextDevice = deviceList.remove(0);
                CachedBluetoothDevice device = HearingAidProfile.this.mDeviceManager.findDevice(nextDevice);
                if (device == null) {
                    if (HearingAidProfile.V) {
                        Log.d("HearingAidProfile", "HearingAidProfile found new device: " + nextDevice);
                    }
                    device = HearingAidProfile.this.mDeviceManager.addDevice(HearingAidProfile.this.mLocalAdapter, HearingAidProfile.this.mProfileManager, nextDevice);
                }
                device.onProfileStateChanged(HearingAidProfile.this, 2);
                device.refresh();
            }
            HearingAidProfile.this.mDeviceManager.updateHearingAidsDevices(HearingAidProfile.this.mProfileManager);
            boolean unused2 = HearingAidProfile.this.mIsProfileReady = true;
        }

        public void onServiceDisconnected(int profile) {
            if (HearingAidProfile.V) {
                Log.d("HearingAidProfile", "Bluetooth service disconnected");
            }
            boolean unused = HearingAidProfile.this.mIsProfileReady = false;
        }
    }

    public boolean isProfileReady() {
        return this.mIsProfileReady;
    }

    public int getProfileId() {
        return 21;
    }

    HearingAidProfile(Context context, LocalBluetoothAdapter adapter, CachedBluetoothDeviceManager deviceManager, LocalBluetoothProfileManager profileManager) {
        this.mContext = context;
        this.mLocalAdapter = adapter;
        this.mDeviceManager = deviceManager;
        this.mProfileManager = profileManager;
        this.mLocalAdapter.getProfileProxy(context, new HearingAidServiceListener(), 21);
    }

    public boolean isConnectable() {
        return false;
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

    public boolean setActiveDevice(BluetoothDevice device) {
        if (this.mService == null) {
            return false;
        }
        return this.mService.setActiveDevice(device);
    }

    public List<BluetoothDevice> getActiveDevices() {
        if (this.mService == null) {
            return new ArrayList();
        }
        return this.mService.getActiveDevices();
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

    public long getHiSyncId(BluetoothDevice device) {
        if (this.mService == null) {
            return 0;
        }
        return this.mService.getHiSyncId(device);
    }

    public String toString() {
        return "HearingAid";
    }

    /* access modifiers changed from: protected */
    public void finalize() {
        if (V) {
            Log.d("HearingAidProfile", "finalize()");
        }
        if (this.mService != null) {
            try {
                BluetoothAdapter.getDefaultAdapter().closeProfileProxy(21, this.mService);
                this.mService = null;
            } catch (Throwable t) {
                Log.w("HearingAidProfile", "Error cleaning up Hearing Aid proxy", t);
            }
        }
    }
}
