package com.android.settingslib.bluetooth;

import android.content.Context;

public class LocalBluetoothManager {
    private static LocalBluetoothManager sInstance;
    private final CachedBluetoothDeviceManager mCachedDeviceManager;
    private final Context mContext;
    private final BluetoothEventManager mEventManager;
    private final LocalBluetoothAdapter mLocalAdapter;
    private final LocalBluetoothProfileManager mProfileManager;

    public interface BluetoothManagerCallback {
        void onBluetoothManagerInitialized(Context context, LocalBluetoothManager localBluetoothManager);
    }

    public static synchronized LocalBluetoothManager getInstance(Context context, BluetoothManagerCallback onInitCallback) {
        synchronized (LocalBluetoothManager.class) {
            if (sInstance == null) {
                LocalBluetoothAdapter adapter = LocalBluetoothAdapter.getInstance();
                if (adapter == null) {
                    return null;
                }
                Context appContext = context.getApplicationContext();
                sInstance = new LocalBluetoothManager(adapter, appContext);
                if (onInitCallback != null) {
                    onInitCallback.onBluetoothManagerInitialized(appContext, sInstance);
                }
            }
            LocalBluetoothManager localBluetoothManager = sInstance;
            return localBluetoothManager;
        }
    }

    private LocalBluetoothManager(LocalBluetoothAdapter adapter, Context context) {
        this.mContext = context;
        this.mLocalAdapter = adapter;
        this.mCachedDeviceManager = new CachedBluetoothDeviceManager(context, this);
        this.mEventManager = new BluetoothEventManager(this.mLocalAdapter, this.mCachedDeviceManager, context);
        this.mProfileManager = new LocalBluetoothProfileManager(context, this.mLocalAdapter, this.mCachedDeviceManager, this.mEventManager);
        this.mEventManager.readPairedDevices();
    }

    public LocalBluetoothAdapter getBluetoothAdapter() {
        return this.mLocalAdapter;
    }

    public CachedBluetoothDeviceManager getCachedDeviceManager() {
        return this.mCachedDeviceManager;
    }

    public BluetoothEventManager getEventManager() {
        return this.mEventManager;
    }

    public LocalBluetoothProfileManager getProfileManager() {
        return this.mProfileManager;
    }
}
