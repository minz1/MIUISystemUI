package com.android.settingslib.bluetooth;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import com.android.settingslib.R;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class BluetoothEventManager {
    private final IntentFilter mAdapterIntentFilter;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
            Handler handler = (Handler) BluetoothEventManager.this.mHandlerMap.get(action);
            if (handler != null) {
                handler.onReceive(context, intent, device);
            }
        }
    };
    /* access modifiers changed from: private */
    public final Collection<BluetoothCallback> mCallbacks = new ArrayList();
    private Context mContext;
    /* access modifiers changed from: private */
    public final CachedBluetoothDeviceManager mDeviceManager;
    /* access modifiers changed from: private */
    public final Map<String, Handler> mHandlerMap;
    /* access modifiers changed from: private */
    public final LocalBluetoothAdapter mLocalAdapter;
    /* access modifiers changed from: private */
    public final BroadcastReceiver mProfileBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
            Handler handler = (Handler) BluetoothEventManager.this.mHandlerMap.get(action);
            if (handler != null) {
                handler.onReceive(context, intent, device);
            }
        }
    };
    private final IntentFilter mProfileIntentFilter;
    /* access modifiers changed from: private */
    public LocalBluetoothProfileManager mProfileManager;
    private android.os.Handler mReceiverHandler;

    private class ActiveDeviceChangedHandler implements Handler {
        private ActiveDeviceChangedHandler() {
        }

        public void onReceive(Context context, Intent intent, BluetoothDevice device) {
            int bluetoothProfile;
            String action = intent.getAction();
            if (action == null) {
                Log.w("BluetoothEventManager", "ActiveDeviceChangedHandler: action is null");
                return;
            }
            CachedBluetoothDevice activeDevice = BluetoothEventManager.this.mDeviceManager.findDevice(device);
            if (Objects.equals(action, "android.bluetooth.a2dp.profile.action.ACTIVE_DEVICE_CHANGED")) {
                bluetoothProfile = 2;
            } else if (Objects.equals(action, "android.bluetooth.headset.profile.action.ACTIVE_DEVICE_CHANGED")) {
                bluetoothProfile = 1;
            } else if (Objects.equals(action, "android.bluetooth.hearingaid.profile.action.ACTIVE_DEVICE_CHANGED")) {
                bluetoothProfile = 21;
            } else {
                Log.w("BluetoothEventManager", "ActiveDeviceChangedHandler: unknown action " + action);
                return;
            }
            BluetoothEventManager.this.dispatchActiveDeviceChanged(activeDevice, bluetoothProfile);
        }
    }

    private class AdapterStateChangedHandler implements Handler {
        private AdapterStateChangedHandler() {
        }

        public void onReceive(Context context, Intent intent, BluetoothDevice device) {
            int state = intent.getIntExtra("android.bluetooth.adapter.extra.STATE", Integer.MIN_VALUE);
            if (state == 10) {
                context.unregisterReceiver(BluetoothEventManager.this.mProfileBroadcastReceiver);
                BluetoothEventManager.this.registerProfileIntentReceiver();
            }
            BluetoothEventManager.this.mLocalAdapter.setBluetoothStateInt(state);
            synchronized (BluetoothEventManager.this.mCallbacks) {
                for (BluetoothCallback callback : BluetoothEventManager.this.mCallbacks) {
                    callback.onBluetoothStateChanged(state);
                }
            }
            BluetoothEventManager.this.mDeviceManager.onBluetoothStateChanged(state);
        }
    }

    private class AudioModeChangedHandler implements Handler {
        private AudioModeChangedHandler() {
        }

        public void onReceive(Context context, Intent intent, BluetoothDevice device) {
            if (intent.getAction() == null) {
                Log.w("BluetoothEventManager", "AudioModeChangedHandler() action is null");
            } else {
                BluetoothEventManager.this.dispatchAudioModeChanged();
            }
        }
    }

    private class BatteryLevelChangedHandler implements Handler {
        private BatteryLevelChangedHandler() {
        }

        public void onReceive(Context context, Intent intent, BluetoothDevice device) {
            CachedBluetoothDevice cachedDevice = BluetoothEventManager.this.mDeviceManager.findDevice(device);
            if (cachedDevice != null) {
                cachedDevice.refresh();
            }
        }
    }

    private class BondStateChangedHandler implements Handler {
        private BondStateChangedHandler() {
        }

        public void onReceive(Context context, Intent intent, BluetoothDevice device) {
            if (device == null) {
                Log.e("BluetoothEventManager", "ACTION_BOND_STATE_CHANGED with no EXTRA_DEVICE");
                return;
            }
            int bondState = intent.getIntExtra("android.bluetooth.device.extra.BOND_STATE", Integer.MIN_VALUE);
            CachedBluetoothDevice cachedDevice = BluetoothEventManager.this.mDeviceManager.findDevice(device);
            if (cachedDevice == null) {
                Log.w("BluetoothEventManager", "CachedBluetoothDevice for device " + device + " not found, calling readPairedDevices().");
                if (BluetoothEventManager.this.readPairedDevices()) {
                    cachedDevice = BluetoothEventManager.this.mDeviceManager.findDevice(device);
                }
                if (cachedDevice == null) {
                    Log.w("BluetoothEventManager", "Got bonding state changed for " + device + ", but we have no record of that device.");
                    cachedDevice = BluetoothEventManager.this.mDeviceManager.addDevice(BluetoothEventManager.this.mLocalAdapter, BluetoothEventManager.this.mProfileManager, device);
                    BluetoothEventManager.this.dispatchDeviceAdded(cachedDevice);
                }
            }
            synchronized (BluetoothEventManager.this.mCallbacks) {
                for (BluetoothCallback callback : BluetoothEventManager.this.mCallbacks) {
                    callback.onDeviceBondStateChanged(cachedDevice, bondState);
                }
            }
            cachedDevice.onBondingStateChanged(bondState);
            if (bondState == 10) {
                if (cachedDevice.getHiSyncId() != 0) {
                    BluetoothEventManager.this.mDeviceManager.onDeviceUnpaired(cachedDevice);
                }
                showUnbondMessage(context, cachedDevice.getName(), intent.getIntExtra("android.bluetooth.device.extra.REASON", Integer.MIN_VALUE));
            }
        }

        private void showUnbondMessage(Context context, String name, int reason) {
            int errorMsg;
            switch (reason) {
                case 1:
                    errorMsg = R.string.bluetooth_pairing_pin_error_message;
                    break;
                case 2:
                    errorMsg = R.string.bluetooth_pairing_rejected_error_message;
                    break;
                case 4:
                    errorMsg = R.string.bluetooth_pairing_device_down_error_message;
                    break;
                case 5:
                case 6:
                case 7:
                case 8:
                    errorMsg = R.string.bluetooth_pairing_error_message;
                    break;
                default:
                    Log.w("BluetoothEventManager", "showUnbondMessage: Not displaying any message for reason: " + reason);
                    return;
            }
            Utils.showError(context, name, errorMsg);
        }
    }

    private class ClassChangedHandler implements Handler {
        private ClassChangedHandler() {
        }

        public void onReceive(Context context, Intent intent, BluetoothDevice device) {
            BluetoothEventManager.this.mDeviceManager.onBtClassChanged(device);
            if (BluetoothDeviceFilter.BONDED_DEVICE_FILTER.matches(device)) {
                Log.d("BluetoothEventManager", "a bonded device: " + device + " bt class changed, do nothing.");
                return;
            }
            CachedBluetoothDevice cachedDevice = BluetoothEventManager.this.mDeviceManager.findDevice(device);
            if (cachedDevice != null) {
                synchronized (BluetoothEventManager.this.mCallbacks) {
                    for (BluetoothCallback callback : BluetoothEventManager.this.mCallbacks) {
                        callback.onDeviceDeleted(cachedDevice);
                        callback.onDeviceAdded(cachedDevice);
                    }
                }
            }
        }
    }

    private class ConnectionStateChangedHandler implements Handler {
        private ConnectionStateChangedHandler() {
        }

        public void onReceive(Context context, Intent intent, BluetoothDevice device) {
            BluetoothEventManager.this.dispatchConnectionStateChanged(BluetoothEventManager.this.mDeviceManager.findDevice(device), intent.getIntExtra("android.bluetooth.adapter.extra.CONNECTION_STATE", Integer.MIN_VALUE));
        }
    }

    private class DeviceDisappearedHandler implements Handler {
        private DeviceDisappearedHandler() {
        }

        public void onReceive(Context context, Intent intent, BluetoothDevice device) {
            CachedBluetoothDevice cachedDevice = BluetoothEventManager.this.mDeviceManager.findDevice(device);
            if (cachedDevice == null) {
                Log.w("BluetoothEventManager", "received ACTION_DISAPPEARED for an unknown device: " + device);
                return;
            }
            if (CachedBluetoothDeviceManager.onDeviceDisappeared(cachedDevice)) {
                synchronized (BluetoothEventManager.this.mCallbacks) {
                    for (BluetoothCallback callback : BluetoothEventManager.this.mCallbacks) {
                        callback.onDeviceDeleted(cachedDevice);
                    }
                }
            }
        }
    }

    private class DeviceFoundHandler implements Handler {
        private DeviceFoundHandler() {
        }

        public void onReceive(Context context, Intent intent, BluetoothDevice device) {
            short rssi = intent.getShortExtra("android.bluetooth.device.extra.RSSI", Short.MIN_VALUE);
            BluetoothClass btClass = (BluetoothClass) intent.getParcelableExtra("android.bluetooth.device.extra.CLASS");
            String name = intent.getStringExtra("android.bluetooth.device.extra.NAME");
            CachedBluetoothDevice cachedDevice = BluetoothEventManager.this.mDeviceManager.findDevice(device);
            if (cachedDevice == null) {
                cachedDevice = BluetoothEventManager.this.mDeviceManager.addDevice(BluetoothEventManager.this.mLocalAdapter, BluetoothEventManager.this.mProfileManager, device);
                Log.d("BluetoothEventManager", "DeviceFoundHandler created new CachedBluetoothDevice: " + cachedDevice);
                BluetoothEventManager.this.dispatchDeviceAdded(cachedDevice);
            }
            cachedDevice.setRssi(rssi);
            cachedDevice.setBtClass(btClass);
            cachedDevice.setNewName(name);
            cachedDevice.setJustDiscovered(true);
        }
    }

    private class DockEventHandler implements Handler {
        private DockEventHandler() {
        }

        public void onReceive(Context context, Intent intent, BluetoothDevice device) {
            if (intent.getIntExtra("android.intent.extra.DOCK_STATE", 1) == 0 && device != null && device.getBondState() == 10) {
                CachedBluetoothDevice cachedDevice = BluetoothEventManager.this.mDeviceManager.findDevice(device);
                if (cachedDevice != null) {
                    cachedDevice.setJustDiscovered(false);
                }
            }
        }
    }

    interface Handler {
        void onReceive(Context context, Intent intent, BluetoothDevice bluetoothDevice);
    }

    private class NameChangedHandler implements Handler {
        private NameChangedHandler() {
        }

        public void onReceive(Context context, Intent intent, BluetoothDevice device) {
            BluetoothEventManager.this.mDeviceManager.onDeviceNameUpdated(device);
        }
    }

    private class ScanningStateChangedHandler implements Handler {
        private final boolean mStarted;

        ScanningStateChangedHandler(boolean started) {
            this.mStarted = started;
        }

        public void onReceive(Context context, Intent intent, BluetoothDevice device) {
            synchronized (BluetoothEventManager.this.mCallbacks) {
                for (BluetoothCallback callback : BluetoothEventManager.this.mCallbacks) {
                    callback.onScanningStateChanged(this.mStarted);
                }
            }
            BluetoothEventManager.this.mDeviceManager.onScanningStateChanged(this.mStarted);
        }
    }

    private class UuidChangedHandler implements Handler {
        private UuidChangedHandler() {
        }

        public void onReceive(Context context, Intent intent, BluetoothDevice device) {
            BluetoothEventManager.this.mDeviceManager.onUuidChanged(device);
        }
    }

    private void addHandler(String action, Handler handler) {
        this.mHandlerMap.put(action, handler);
        this.mAdapterIntentFilter.addAction(action);
    }

    /* access modifiers changed from: package-private */
    public void addProfileHandler(String action, Handler handler) {
        this.mHandlerMap.put(action, handler);
        this.mProfileIntentFilter.addAction(action);
    }

    /* access modifiers changed from: package-private */
    public void setProfileManager(LocalBluetoothProfileManager manager) {
        this.mProfileManager = manager;
    }

    BluetoothEventManager(LocalBluetoothAdapter adapter, CachedBluetoothDeviceManager deviceManager, Context context) {
        this.mLocalAdapter = adapter;
        this.mDeviceManager = deviceManager;
        this.mAdapterIntentFilter = new IntentFilter();
        this.mProfileIntentFilter = new IntentFilter();
        this.mHandlerMap = new HashMap();
        this.mContext = context;
        addHandler("android.bluetooth.adapter.action.STATE_CHANGED", new AdapterStateChangedHandler());
        addHandler("android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED", new ConnectionStateChangedHandler());
        addHandler("android.bluetooth.adapter.action.DISCOVERY_STARTED", new ScanningStateChangedHandler(true));
        addHandler("android.bluetooth.adapter.action.DISCOVERY_FINISHED", new ScanningStateChangedHandler(false));
        addHandler("android.bluetooth.device.action.FOUND", new DeviceFoundHandler());
        addHandler("android.bluetooth.device.action.DISAPPEARED", new DeviceDisappearedHandler());
        addHandler("android.bluetooth.device.action.NAME_CHANGED", new NameChangedHandler());
        addHandler("android.bluetooth.device.action.ALIAS_CHANGED", new NameChangedHandler());
        addHandler("android.bluetooth.device.action.BOND_STATE_CHANGED", new BondStateChangedHandler());
        addHandler("android.bluetooth.device.action.CLASS_CHANGED", new ClassChangedHandler());
        addHandler("android.bluetooth.device.action.UUID", new UuidChangedHandler());
        addHandler("android.bluetooth.device.action.BATTERY_LEVEL_CHANGED", new BatteryLevelChangedHandler());
        addHandler("android.intent.action.DOCK_EVENT", new DockEventHandler());
        addHandler("android.bluetooth.a2dp.profile.action.ACTIVE_DEVICE_CHANGED", new ActiveDeviceChangedHandler());
        addHandler("android.bluetooth.headset.profile.action.ACTIVE_DEVICE_CHANGED", new ActiveDeviceChangedHandler());
        addHandler("android.bluetooth.hearingaid.profile.action.ACTIVE_DEVICE_CHANGED", new ActiveDeviceChangedHandler());
        addHandler("android.bluetooth.headset.profile.action.AUDIO_STATE_CHANGED", new AudioModeChangedHandler());
        addHandler("android.intent.action.PHONE_STATE", new AudioModeChangedHandler());
        this.mContext.registerReceiver(this.mBroadcastReceiver, this.mAdapterIntentFilter, null, this.mReceiverHandler);
        this.mContext.registerReceiver(this.mProfileBroadcastReceiver, this.mProfileIntentFilter, null, this.mReceiverHandler);
    }

    /* access modifiers changed from: package-private */
    public void registerProfileIntentReceiver() {
        this.mContext.registerReceiver(this.mProfileBroadcastReceiver, this.mProfileIntentFilter, null, this.mReceiverHandler);
    }

    public void setReceiverHandler(android.os.Handler handler) {
        this.mContext.unregisterReceiver(this.mBroadcastReceiver);
        this.mContext.unregisterReceiver(this.mProfileBroadcastReceiver);
        this.mReceiverHandler = handler;
        this.mContext.registerReceiver(this.mBroadcastReceiver, this.mAdapterIntentFilter, null, this.mReceiverHandler);
        registerProfileIntentReceiver();
    }

    public void registerCallback(BluetoothCallback callback) {
        synchronized (this.mCallbacks) {
            this.mCallbacks.add(callback);
        }
    }

    /* access modifiers changed from: private */
    public void dispatchConnectionStateChanged(CachedBluetoothDevice cachedDevice, int state) {
        synchronized (this.mCallbacks) {
            for (BluetoothCallback callback : this.mCallbacks) {
                callback.onConnectionStateChanged(cachedDevice, state);
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void dispatchDeviceAdded(CachedBluetoothDevice cachedDevice) {
        synchronized (this.mCallbacks) {
            for (BluetoothCallback callback : this.mCallbacks) {
                callback.onDeviceAdded(cachedDevice);
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void dispatchDeviceRemoved(CachedBluetoothDevice cachedDevice) {
        synchronized (this.mCallbacks) {
            for (BluetoothCallback callback : this.mCallbacks) {
                callback.onDeviceDeleted(cachedDevice);
            }
        }
    }

    /* access modifiers changed from: package-private */
    public boolean readPairedDevices() {
        Set<BluetoothDevice> bondedDevices = this.mLocalAdapter.getBondedDevices();
        if (bondedDevices == null) {
            return false;
        }
        boolean deviceAdded = false;
        for (BluetoothDevice device : bondedDevices) {
            if (this.mDeviceManager.findDevice(device) == null) {
                dispatchDeviceAdded(this.mDeviceManager.addDevice(this.mLocalAdapter, this.mProfileManager, device));
                deviceAdded = true;
            }
        }
        return deviceAdded;
    }

    /* access modifiers changed from: private */
    public void dispatchActiveDeviceChanged(CachedBluetoothDevice activeDevice, int bluetoothProfile) {
        this.mDeviceManager.onActiveDeviceChanged(activeDevice, bluetoothProfile);
        synchronized (this.mCallbacks) {
            for (BluetoothCallback callback : this.mCallbacks) {
                callback.onActiveDeviceChanged(activeDevice, bluetoothProfile);
            }
        }
    }

    /* access modifiers changed from: private */
    public void dispatchAudioModeChanged() {
        this.mDeviceManager.dispatchAudioModeChanged();
        synchronized (this.mCallbacks) {
            for (BluetoothCallback callback : this.mCallbacks) {
                callback.onAudioModeChanged();
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void dispatchProfileConnectionStateChanged(CachedBluetoothDevice device, int state, int bluetoothProfile) {
        synchronized (this.mCallbacks) {
            for (BluetoothCallback callback : this.mCallbacks) {
                callback.onProfileConnectionStateChanged(device, state, bluetoothProfile);
            }
        }
        this.mDeviceManager.onProfileConnectionStateChanged(device, state, bluetoothProfile);
    }
}
