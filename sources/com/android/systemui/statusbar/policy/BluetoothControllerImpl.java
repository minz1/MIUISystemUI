package com.android.systemui.statusbar.policy;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.os.UserHandleCompat;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.systemui.Dependency;
import com.android.systemui.SystemUICompat;
import com.android.systemui.statusbar.policy.BluetoothController;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class BluetoothControllerImpl implements BluetoothCallback, CachedBluetoothDevice.Callback, BluetoothController {
    private static final boolean DEBUG = Log.isLoggable("BluetoothController", 3);
    private final Handler mBgHandler;
    private Collection<CachedBluetoothDevice> mCachedDevices;
    private Map<String, CachedDeviceState> mCachedStates;
    private int mConnectionState = 0;
    private final Context mContext;
    private final int mCurrentUser;
    /* access modifiers changed from: private */
    public boolean mEnabled;
    /* access modifiers changed from: private */
    public final H mHandler = new H(Looper.getMainLooper());
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("com.android.bluetooth.opp.BLUETOOTH_OPP_INBOUND_START".equals(action) || "com.android.bluetooth.opp.BLUETOOTH_OPP_INBOUND_END".equals(action) || "com.android.bluetooth.opp.BLUETOOTH_OPP_OUTBOUND_START".equals(action) || "com.android.bluetooth.opp.BLUETOOTH_OPP_OUTBOUND_END".equals(action)) {
                Message message = BluetoothControllerImpl.this.mHandler.obtainMessage();
                message.what = 5;
                message.obj = action;
                BluetoothControllerImpl.this.mHandler.sendMessage(message);
            }
        }
    };
    private CachedBluetoothDevice mLastActiveDevice;
    /* access modifiers changed from: private */
    public final LocalBluetoothManager mLocalBluetoothManager;
    private int mState = 10;
    private final UserManager mUserManager;

    private final class BH extends Handler {
        public BH(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                BluetoothControllerImpl.this.handleDeviceAttributesChanged();
            }
        }
    }

    private static class CachedDeviceState {
        private int mBondState;
        /* access modifiers changed from: private */
        public int mMaxConnectionState;
        /* access modifiers changed from: private */
        public String mSummary;
        private final Handler mUiHandler;

        private CachedDeviceState(Handler uiHandler) {
            this.mBondState = 10;
            this.mMaxConnectionState = 0;
            this.mSummary = "";
            this.mUiHandler = uiHandler;
        }

        public void setBondState(int bondState) {
            if (this.mBondState != bondState) {
                this.mBondState = bondState;
                this.mUiHandler.removeMessages(1);
                this.mUiHandler.sendEmptyMessage(1);
            }
        }

        public void setConnectionState(int maxConnectionState) {
            if (this.mMaxConnectionState != maxConnectionState) {
                this.mMaxConnectionState = maxConnectionState;
                this.mUiHandler.removeMessages(2);
                this.mUiHandler.sendEmptyMessage(2);
            }
        }

        public void setSummary(String summary) {
            if (!TextUtils.equals(this.mSummary, summary)) {
                this.mSummary = summary;
                this.mUiHandler.removeMessages(2);
                this.mUiHandler.sendEmptyMessage(2);
            }
        }
    }

    private final class H extends Handler {
        /* access modifiers changed from: private */
        public final ArrayList<BluetoothController.Callback> mCallbacks = new ArrayList<>();

        public H(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    firePairedDevicesChanged();
                    return;
                case 2:
                    Log.d("BluetoothController", "fireStateChange");
                    fireStateChange();
                    return;
                case 3:
                    this.mCallbacks.add((BluetoothController.Callback) msg.obj);
                    return;
                case 4:
                    this.mCallbacks.remove((BluetoothController.Callback) msg.obj);
                    return;
                case 5:
                    fireInoutStateChange((String) msg.obj);
                    return;
                default:
                    return;
            }
        }

        private void firePairedDevicesChanged() {
            Iterator<BluetoothController.Callback> it = this.mCallbacks.iterator();
            while (it.hasNext()) {
                it.next().onBluetoothDevicesChanged();
            }
        }

        private void fireStateChange() {
            Iterator<BluetoothController.Callback> it = this.mCallbacks.iterator();
            while (it.hasNext()) {
                fireStateChange(it.next());
            }
        }

        private void fireInoutStateChange(String action) {
            Iterator<BluetoothController.Callback> it = this.mCallbacks.iterator();
            while (it.hasNext()) {
                fireInoutStateChange(it.next(), action);
            }
        }

        private void fireStateChange(BluetoothController.Callback cb) {
            cb.onBluetoothStateChange(BluetoothControllerImpl.this.mEnabled);
        }

        private void fireInoutStateChange(BluetoothController.Callback cb, String action) {
            cb.onBluetoothInoutStateChange(action);
        }
    }

    public BluetoothControllerImpl(Context context, Looper bgLooper) {
        this.mContext = context;
        this.mCachedDevices = new ArraySet();
        this.mCachedStates = new HashMap();
        this.mLocalBluetoothManager = (LocalBluetoothManager) Dependency.get(LocalBluetoothManager.class);
        this.mBgHandler = new BH(bgLooper);
        if (this.mLocalBluetoothManager != null) {
            this.mLocalBluetoothManager.getEventManager().setReceiverHandler(this.mBgHandler);
            this.mLocalBluetoothManager.getEventManager().registerCallback(this);
            this.mBgHandler.post(new Runnable() {
                public void run() {
                    BluetoothControllerImpl.this.onBluetoothStateChanged(BluetoothControllerImpl.this.mLocalBluetoothManager.getBluetoothAdapter().getBluetoothState());
                }
            });
        }
        this.mUserManager = (UserManager) context.getSystemService("user");
        this.mCurrentUser = ActivityManager.getCurrentUser();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.android.bluetooth.opp.BLUETOOTH_OPP_INBOUND_START");
        filter.addAction("com.android.bluetooth.opp.BLUETOOTH_OPP_INBOUND_END");
        filter.addAction("com.android.bluetooth.opp.BLUETOOTH_OPP_OUTBOUND_START");
        filter.addAction("com.android.bluetooth.opp.BLUETOOTH_OPP_OUTBOUND_END");
        context.registerReceiverAsUser(this.mIntentReceiver, UserHandle.ALL, filter, null, this.mBgHandler);
    }

    private void addPairedDevices() {
        Log.d("BluetoothController", "addPairedDevices");
        int state = this.mLocalBluetoothManager.getBluetoothAdapter().getConnectionState();
        Collection<CachedBluetoothDevice> cachedBluetoothDevices = this.mLocalBluetoothManager.getCachedDeviceManager().getCachedDevicesCopy();
        CachedBluetoothDevice lastConnectedDevice = null;
        this.mLastActiveDevice = null;
        for (CachedBluetoothDevice device : cachedBluetoothDevices) {
            int bondState = device.getBondState();
            if (bondState != 10) {
                addCachedDevice(device);
                CachedDeviceState deviceState = getCachedState(device);
                deviceState.setBondState(bondState);
                int maxConnectionState = device.getMaxConnectionState();
                if (maxConnectionState > state) {
                    state = maxConnectionState;
                }
                deviceState.setConnectionState(maxConnectionState);
                if (SystemUICompat.isDeviceActive(device)) {
                    Log.d("BluetoothController", "addPairedDevices: last active device: " + device.getName());
                    this.mLastActiveDevice = device;
                } else if (device.isConnected()) {
                    Log.d("BluetoothController", "addPairedDevices: last connected device: " + device.getName());
                    lastConnectedDevice = device;
                }
                deviceState.setSummary(SystemUICompat.getConnectionSummary(this.mContext, device));
            }
        }
        if (this.mLastActiveDevice == null && lastConnectedDevice != null) {
            Log.d("BluetoothController", "addPairedDevices: find last connected device: " + lastConnectedDevice.getName());
            this.mLastActiveDevice = lastConnectedDevice;
        }
        if (this.mLastActiveDevice == null && state == 2) {
            state = 0;
        }
        if (state != this.mConnectionState) {
            this.mConnectionState = state;
            this.mHandler.removeMessages(2);
            this.mHandler.sendEmptyMessage(2);
        }
    }

    private void removeAllDevices() {
        Log.d("BluetoothController", "removeAllDevices");
        for (CachedBluetoothDevice device : this.mCachedDevices) {
            device.unregisterCallback(this);
        }
        this.mCachedDevices.clear();
        this.mCachedStates.clear();
    }

    public void addCallback(BluetoothController.Callback cb) {
        this.mHandler.obtainMessage(3, cb).sendToTarget();
        this.mHandler.removeMessages(2);
        this.mHandler.sendEmptyMessage(2);
    }

    public void removeCallback(BluetoothController.Callback cb) {
        this.mHandler.obtainMessage(4, cb).sendToTarget();
    }

    private void addCachedDevice(CachedBluetoothDevice cachedDevice) {
        if (this.mCachedDevices.add(cachedDevice)) {
            cachedDevice.registerCallback(this);
        }
    }

    private void removeCachedDevice(CachedBluetoothDevice cachedDevice) {
        if (this.mCachedDevices.remove(cachedDevice)) {
            cachedDevice.unregisterCallback(this);
        }
    }

    public boolean canConfigBluetooth() {
        return !this.mUserManager.hasUserRestriction("no_config_bluetooth", UserHandleCompat.of(this.mCurrentUser));
    }

    public boolean isBluetoothEnabled() {
        return this.mEnabled;
    }

    public int getBluetoothState() {
        return this.mState;
    }

    public boolean isBluetoothConnected() {
        return this.mConnectionState == 2;
    }

    public boolean isBluetoothConnecting() {
        return this.mConnectionState == 1;
    }

    public boolean isBluetoothReady() {
        return this.mState == 12 || this.mState == 10 || this.mState == 15;
    }

    public void setBluetoothEnabled(boolean enabled) {
        if (this.mLocalBluetoothManager != null) {
            this.mLocalBluetoothManager.getBluetoothAdapter().setBluetoothEnabled(enabled);
        }
    }

    public boolean isBluetoothSupported() {
        return this.mLocalBluetoothManager != null;
    }

    public void connect(CachedBluetoothDevice device) {
        if (this.mLocalBluetoothManager != null && device != null) {
            device.connect(true);
        }
    }

    public void disconnect(CachedBluetoothDevice device) {
        if (this.mLocalBluetoothManager != null && device != null) {
            device.disconnect();
        }
    }

    public String getLastDeviceName() {
        if (this.mLastActiveDevice != null) {
            return this.mLastActiveDevice.getName();
        }
        return null;
    }

    public Collection<CachedBluetoothDevice> getCachedDevicesCopy() {
        if (this.mCachedDevices == null || this.mCachedDevices.isEmpty()) {
            return null;
        }
        return new ArrayList<>(this.mCachedDevices);
    }

    public void onBluetoothStateChanged(int bluetoothState) {
        Log.d("BluetoothController", "onBluetoothStateChanged: bluetoothState: " + bluetoothState);
        this.mState = bluetoothState;
        switch (bluetoothState) {
            case 10:
                this.mEnabled = false;
                break;
            case 11:
                removeAllDevices();
                this.mEnabled = true;
                break;
            case 12:
                this.mEnabled = true;
                addPairedDevices();
                break;
            case 13:
                this.mEnabled = false;
                removeAllDevices();
                this.mLastActiveDevice = null;
                this.mConnectionState = 0;
                break;
        }
        this.mHandler.removeMessages(2);
        this.mHandler.sendEmptyMessage(2);
    }

    public void onScanningStateChanged(boolean started) {
    }

    public void onDeviceAdded(CachedBluetoothDevice cachedDevice) {
    }

    public void onDeviceDeleted(CachedBluetoothDevice cachedDevice) {
    }

    public void onDeviceBondStateChanged(CachedBluetoothDevice cachedDevice, int bondState) {
        Log.d("BluetoothController", "onDeviceBondStateChanged");
        if (bondState == 10) {
            Log.d("BluetoothController", "onDeviceBondStateChanged: " + cachedDevice.getDevice() + ": bond none");
            removeCachedDevice(cachedDevice);
            this.mCachedStates.remove(cachedDevice.getDevice().getAddress());
            onDeviceAttributesChanged();
        } else if (bondState == 12) {
            Log.d("BluetoothController", "onDeviceBondStateChanged: " + cachedDevice.getDevice() + ": bonded");
            addCachedDevice(cachedDevice);
            getCachedState(cachedDevice).setBondState(bondState);
        }
    }

    public void onConnectionStateChanged(CachedBluetoothDevice cachedDevice, int state) {
    }

    public void onActiveDeviceChanged(CachedBluetoothDevice activeDevice, int bluetoothProfile) {
    }

    public void onAudioModeChanged() {
    }

    public void onDeviceAttributesChanged() {
        Log.d("BluetoothController", "onDeviceAttributesChanged");
        this.mBgHandler.removeMessages(1);
        this.mBgHandler.sendEmptyMessage(1);
    }

    public void handleDeviceAttributesChanged() {
        Log.d("BluetoothController", "handleDeviceAttributesChanged");
        updateConnectionState();
        updateSummary();
    }

    private void updateConnectionState() {
        Log.d("BluetoothController", "updateConnectionState");
        int state = 0;
        CachedBluetoothDevice lastConnectedDevice = null;
        this.mLastActiveDevice = null;
        for (CachedBluetoothDevice device : this.mCachedDevices) {
            int maxConnectionState = device.getMaxConnectionState();
            if (maxConnectionState > state) {
                state = maxConnectionState;
            }
            if (SystemUICompat.isDeviceActive(device)) {
                Log.d("BluetoothController", "updateConnectionState: last active device: " + device.getName());
                this.mLastActiveDevice = device;
            } else if (device.isConnected()) {
                Log.d("BluetoothController", "updateConnectionState: last connected device: " + device.getName());
                lastConnectedDevice = device;
            }
            getCachedState(device).setConnectionState(maxConnectionState);
        }
        if (this.mLastActiveDevice == null && lastConnectedDevice != null) {
            Log.d("BluetoothController", "updateConnectionState: find last connected device: " + lastConnectedDevice.getName());
            this.mLastActiveDevice = lastConnectedDevice;
        }
        if (this.mConnectionState != state) {
            this.mConnectionState = state;
            this.mHandler.removeMessages(2);
            this.mHandler.sendEmptyMessage(2);
            Log.d("BluetoothController", "updateConnectionState: " + this.mConnectionState);
        }
    }

    private void updateSummary() {
        for (CachedBluetoothDevice cachedDevice : this.mCachedDevices) {
            getCachedState(cachedDevice).setSummary(SystemUICompat.getConnectionSummary(this.mContext, cachedDevice));
        }
    }

    public CachedDeviceState getCachedState(CachedBluetoothDevice cachedDevice) {
        String address = cachedDevice.getDevice().getAddress();
        CachedDeviceState deviceState = this.mCachedStates.get(address);
        if (deviceState != null) {
            return deviceState;
        }
        CachedDeviceState deviceState2 = new CachedDeviceState(this.mHandler);
        this.mCachedStates.put(address, deviceState2);
        return deviceState2;
    }

    public int getMaxConnectionState(CachedBluetoothDevice device) {
        return getCachedState(device).mMaxConnectionState;
    }

    public String getSummary(CachedBluetoothDevice device) {
        return getCachedState(device).mSummary;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("BluetoothController state:");
        pw.print("  mLocalBluetoothManager=");
        pw.println(this.mLocalBluetoothManager);
        if (this.mLocalBluetoothManager != null) {
            pw.print("  mEnabled=");
            pw.println(this.mEnabled);
            pw.print("  mState=");
            pw.println(this.mState);
            pw.print("  mConnectionState=");
            pw.println(stateToString(this.mConnectionState));
            pw.print("  mLastActiveDevice=");
            pw.println(this.mLastActiveDevice);
            pw.print("  mCallbacks.size=");
            pw.println(this.mHandler.mCallbacks.size());
            pw.println("  Bluetooth Devices:");
            Iterator<CachedBluetoothDevice> it = this.mCachedDevices.iterator();
            while (it.hasNext()) {
                pw.println("    " + getDeviceString(it.next()));
            }
        }
    }

    private static String stateToString(int state) {
        switch (state) {
            case 0:
                return "DISCONNECTED";
            case 1:
                return "CONNECTING";
            case 2:
                return "CONNECTED";
            case 3:
                return "DISCONNECTING";
            default:
                return "UNKNOWN(" + state + ")";
        }
    }

    private String getDeviceString(CachedBluetoothDevice device) {
        return device.getName() + " " + device.getBondState() + " " + device.isConnected();
    }
}
