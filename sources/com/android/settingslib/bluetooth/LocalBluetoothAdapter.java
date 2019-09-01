package com.android.settingslib.bluetooth;

import android.app.ActivityThread;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.BoostFramework;
import android.util.Log;
import java.util.List;
import java.util.Set;
import miui.util.FeatureParser;

public class LocalBluetoothAdapter {
    private static LocalBluetoothAdapter sInstance;
    private final BluetoothAdapter mAdapter;
    private LocalBluetoothProfileManager mProfileManager;
    private int mState = Integer.MIN_VALUE;

    private LocalBluetoothAdapter(BluetoothAdapter adapter) {
        this.mAdapter = adapter;
    }

    /* access modifiers changed from: package-private */
    public void setProfileManager(LocalBluetoothProfileManager manager) {
        this.mProfileManager = manager;
    }

    public static synchronized LocalBluetoothAdapter getInstance() {
        LocalBluetoothAdapter localBluetoothAdapter;
        synchronized (LocalBluetoothAdapter.class) {
            if (sInstance == null) {
                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                if (adapter != null) {
                    sInstance = new LocalBluetoothAdapter(adapter);
                }
            }
            localBluetoothAdapter = sInstance;
        }
        return localBluetoothAdapter;
    }

    public void cancelDiscovery() {
        this.mAdapter.cancelDiscovery();
    }

    public boolean enable() {
        return this.mAdapter.enable();
    }

    /* access modifiers changed from: package-private */
    public void getProfileProxy(Context context, BluetoothProfile.ServiceListener listener, int profile) {
        this.mAdapter.getProfileProxy(context, listener, profile);
    }

    public Set<BluetoothDevice> getBondedDevices() {
        return this.mAdapter.getBondedDevices();
    }

    public BluetoothLeScanner getBluetoothLeScanner() {
        return this.mAdapter.getBluetoothLeScanner();
    }

    public int getState() {
        return this.mAdapter.getState();
    }

    public ParcelUuid[] getUuids() {
        return this.mAdapter.getUuids();
    }

    public boolean isDiscovering() {
        return this.mAdapter.isDiscovering();
    }

    public int getConnectionState() {
        return this.mAdapter.getConnectionState();
    }

    public synchronized int getBluetoothState() {
        syncBluetoothState();
        return this.mState;
    }

    /* access modifiers changed from: package-private */
    /* JADX WARNING: Code restructure failed: missing block: B:11:0x0010, code lost:
        if (r1.mProfileManager == null) goto L_0x0017;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:12:0x0012, code lost:
        r1.mProfileManager.setBluetoothStateOn();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:13:0x0017, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:9:0x000c, code lost:
        if (r2 != 12) goto L_0x0017;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setBluetoothStateInt(int r2) {
        /*
            r1 = this;
            monitor-enter(r1)
            int r0 = r1.mState     // Catch:{ all -> 0x0018 }
            if (r0 != r2) goto L_0x0007
            monitor-exit(r1)     // Catch:{ all -> 0x0018 }
            return
        L_0x0007:
            r1.mState = r2     // Catch:{ all -> 0x0018 }
            monitor-exit(r1)     // Catch:{ all -> 0x0018 }
            r0 = 12
            if (r2 != r0) goto L_0x0017
            com.android.settingslib.bluetooth.LocalBluetoothProfileManager r0 = r1.mProfileManager
            if (r0 == 0) goto L_0x0017
            com.android.settingslib.bluetooth.LocalBluetoothProfileManager r0 = r1.mProfileManager
            r0.setBluetoothStateOn()
        L_0x0017:
            return
        L_0x0018:
            r0 = move-exception
            monitor-exit(r1)     // Catch:{ all -> 0x0018 }
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.settingslib.bluetooth.LocalBluetoothAdapter.setBluetoothStateInt(int):void");
    }

    /* access modifiers changed from: package-private */
    public boolean syncBluetoothState() {
        if (this.mAdapter.getState() == this.mState) {
            return false;
        }
        setBluetoothStateInt(this.mAdapter.getState());
        return true;
    }

    public boolean setBluetoothEnabled(boolean enabled) {
        boolean success;
        int i;
        if (FeatureParser.getBoolean("support_bluetooth_boost", false)) {
            String boostValue = FeatureParser.getString("bluetooth_boost_value");
            if (boostValue != null) {
                if (boostValue.length() < 1) {
                    Log.e("LocalBluetoothAdapter", "setBluetoothEnabled: boost value error");
                } else {
                    if (boostValue.length() > 2 && "0x".equalsIgnoreCase(boostValue.substring(0, 2))) {
                        boostValue = boostValue.substring(2);
                    }
                    try {
                        Log.v("LocalBluetoothAdapter", "setBluetoothEnabled : boostValue = " + boostValue);
                        int VENDOR_HINT_BLUETOOTH_BOOST = Integer.parseInt(boostValue, 16);
                        BoostFramework mBtEnablePerf = new BoostFramework();
                        int rec = mBtEnablePerf.perfHint(VENDOR_HINT_BLUETOOTH_BOOST, "BluetoothApp=" + ActivityThread.currentPackageName(), -1, 1);
                        if (rec != 0) {
                            Log.e("LocalBluetoothAdapter", "setBluetoothEnabled: set boost rec = " + rec);
                        }
                    } catch (NumberFormatException e) {
                        Log.e("LocalBluetoothAdapter", "setBluetoothEnabled: set boost number format exception");
                    }
                }
            }
        }
        if (enabled) {
            success = this.mAdapter.enable();
        } else {
            success = this.mAdapter.disable();
        }
        if (success) {
            if (enabled) {
                i = 11;
            } else {
                i = 13;
            }
            setBluetoothStateInt(i);
        } else {
            syncBluetoothState();
        }
        return success;
    }

    public List<Integer> getSupportedProfiles() {
        return this.mAdapter.getSupportedProfiles();
    }
}
