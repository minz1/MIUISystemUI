package com.android.systemui.keyboard;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.hardware.input.InputManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.telecom.Log;
import android.text.TextUtils;
import android.util.Pair;
import android.widget.Toast;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LocalBluetoothAdapter;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.bluetooth.Utils;
import com.android.systemui.R;
import com.android.systemui.SystemUI;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class KeyboardUI extends SystemUI implements InputManager.OnTabletModeChangedListener {
    private boolean mBootCompleted;
    private long mBootCompletedTime;
    private CachedBluetoothDeviceManager mCachedDeviceManager;
    protected volatile Context mContext;
    /* access modifiers changed from: private */
    public BluetoothDialog mDialog;
    private boolean mEnabled;
    /* access modifiers changed from: private */
    public volatile KeyboardHandler mHandler;
    private int mInTabletMode = -1;
    private String mKeyboardName;
    /* access modifiers changed from: private */
    public LocalBluetoothAdapter mLocalBluetoothAdapter;
    private LocalBluetoothProfileManager mProfileManager;
    private int mScanAttempt = 0;
    private ScanCallback mScanCallback;
    /* access modifiers changed from: private */
    public int mState;
    private volatile KeyboardUIHandler mUIHandler;

    private final class BluetoothCallbackHandler implements BluetoothCallback {
        private BluetoothCallbackHandler() {
        }

        public void onBluetoothStateChanged(int bluetoothState) {
            KeyboardUI.this.mHandler.obtainMessage(4, bluetoothState, 0).sendToTarget();
        }

        public void onDeviceBondStateChanged(CachedBluetoothDevice cachedDevice, int bondState) {
            KeyboardUI.this.mHandler.obtainMessage(5, bondState, 0, cachedDevice).sendToTarget();
        }

        public void onDeviceAdded(CachedBluetoothDevice cachedDevice) {
        }

        public void onDeviceDeleted(CachedBluetoothDevice cachedDevice) {
        }

        public void onScanningStateChanged(boolean started) {
        }

        public void onConnectionStateChanged(CachedBluetoothDevice cachedDevice, int state) {
        }

        public void onActiveDeviceChanged(CachedBluetoothDevice activeDevice, int bluetoothProfile) {
        }

        public void onAudioModeChanged() {
        }
    }

    private final class BluetoothDialogClickListener implements DialogInterface.OnClickListener {
        private BluetoothDialogClickListener() {
        }

        public void onClick(DialogInterface dialog, int which) {
            KeyboardUI.this.mHandler.obtainMessage(3, -1 == which ? 1 : 0, 0).sendToTarget();
            BluetoothDialog unused = KeyboardUI.this.mDialog = null;
        }
    }

    private final class BluetoothDialogDismissListener implements DialogInterface.OnDismissListener {
        private BluetoothDialogDismissListener() {
        }

        public void onDismiss(DialogInterface dialog) {
            BluetoothDialog unused = KeyboardUI.this.mDialog = null;
        }
    }

    private final class BluetoothErrorListener implements Utils.ErrorListener {
        private BluetoothErrorListener() {
        }

        public void onShowError(Context context, String name, int messageResId) {
            KeyboardUI.this.mHandler.obtainMessage(11, messageResId, 0, new Pair(context, name)).sendToTarget();
        }
    }

    private final class KeyboardHandler extends Handler {
        public KeyboardHandler(Looper looper) {
            super(looper, null, true);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    KeyboardUI.this.init();
                    return;
                case 1:
                    KeyboardUI.this.onBootCompletedInternal();
                    return;
                case 2:
                    KeyboardUI.this.processKeyboardState();
                    return;
                case 3:
                    boolean enable = true;
                    if (msg.arg1 != 1) {
                        enable = false;
                    }
                    if (enable) {
                        KeyboardUI.this.mLocalBluetoothAdapter.enable();
                        return;
                    } else {
                        int unused = KeyboardUI.this.mState = 8;
                        return;
                    }
                case 4:
                    KeyboardUI.this.onBluetoothStateChangedInternal(msg.arg1);
                    return;
                case 5:
                    int bondState = msg.arg1;
                    KeyboardUI.this.onDeviceBondStateChangedInternal((CachedBluetoothDevice) msg.obj, bondState);
                    return;
                case 6:
                    KeyboardUI.this.onDeviceAddedInternal(KeyboardUI.this.getCachedBluetoothDevice((BluetoothDevice) msg.obj));
                    return;
                case 7:
                    KeyboardUI.this.onBleScanFailedInternal();
                    return;
                case 10:
                    KeyboardUI.this.bleAbortScanInternal(msg.arg1);
                    return;
                case 11:
                    Pair<Context, String> p = (Pair) msg.obj;
                    KeyboardUI.this.onShowErrorInternal((Context) p.first, (String) p.second, msg.arg1);
                    return;
                default:
                    return;
            }
        }
    }

    private final class KeyboardScanCallback extends ScanCallback {
        private KeyboardScanCallback() {
        }

        private boolean isDeviceDiscoverable(ScanResult result) {
            return (result.getScanRecord().getAdvertiseFlags() & 3) != 0;
        }

        public void onBatchScanResults(List<ScanResult> results) {
            BluetoothDevice bestDevice = null;
            int bestRssi = Integer.MIN_VALUE;
            for (ScanResult result : results) {
                if (isDeviceDiscoverable(result) && result.getRssi() > bestRssi) {
                    bestDevice = result.getDevice();
                    bestRssi = result.getRssi();
                }
            }
            if (bestDevice != null) {
                KeyboardUI.this.mHandler.obtainMessage(6, bestDevice).sendToTarget();
            }
        }

        public void onScanFailed(int errorCode) {
            KeyboardUI.this.mHandler.obtainMessage(7).sendToTarget();
        }

        public void onScanResult(int callbackType, ScanResult result) {
            if (isDeviceDiscoverable(result)) {
                KeyboardUI.this.mHandler.obtainMessage(6, result.getDevice()).sendToTarget();
            }
        }
    }

    private final class KeyboardUIHandler extends Handler {
        public KeyboardUIHandler() {
            super(Looper.getMainLooper(), null, true);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 8:
                    if (KeyboardUI.this.mDialog == null) {
                        DialogInterface.OnClickListener clickListener = new BluetoothDialogClickListener();
                        DialogInterface.OnDismissListener dismissListener = new BluetoothDialogDismissListener();
                        BluetoothDialog unused = KeyboardUI.this.mDialog = new BluetoothDialog(KeyboardUI.this.mContext);
                        KeyboardUI.this.mDialog.setTitle(R.string.enable_bluetooth_title);
                        KeyboardUI.this.mDialog.setMessage(R.string.enable_bluetooth_message);
                        KeyboardUI.this.mDialog.setPositiveButton(R.string.enable_bluetooth_confirmation_ok, clickListener);
                        KeyboardUI.this.mDialog.setNegativeButton(17039360, clickListener);
                        KeyboardUI.this.mDialog.setOnDismissListener(dismissListener);
                        KeyboardUI.this.mDialog.show();
                        return;
                    }
                    return;
                case 9:
                    if (KeyboardUI.this.mDialog != null) {
                        KeyboardUI.this.mDialog.dismiss();
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    }

    public void start() {
        this.mContext = this.mContext;
        HandlerThread thread = new HandlerThread("Keyboard", 10);
        thread.start();
        this.mHandler = new KeyboardHandler(thread.getLooper());
        this.mHandler.sendEmptyMessage(0);
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration newConfig) {
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("KeyboardUI:");
        pw.println("  mEnabled=" + this.mEnabled);
        pw.println("  mBootCompleted=" + this.mEnabled);
        pw.println("  mBootCompletedTime=" + this.mBootCompletedTime);
        pw.println("  mKeyboardName=" + this.mKeyboardName);
        pw.println("  mInTabletMode=" + this.mInTabletMode);
        pw.println("  mState=" + stateToString(this.mState));
    }

    /* access modifiers changed from: protected */
    public void onBootCompleted() {
        this.mHandler.sendEmptyMessage(1);
    }

    public void onTabletModeChanged(long whenNanos, boolean inTabletMode) {
        int i = 1;
        if ((inTabletMode && this.mInTabletMode != 1) || (!inTabletMode && this.mInTabletMode != 0)) {
            if (!inTabletMode) {
                i = 0;
            }
            this.mInTabletMode = i;
            processKeyboardState();
        }
    }

    /* access modifiers changed from: private */
    public void init() {
        Context context = this.mContext;
        this.mKeyboardName = context.getString(17039733);
        if (!TextUtils.isEmpty(this.mKeyboardName)) {
            LocalBluetoothManager bluetoothManager = LocalBluetoothManager.getInstance(context, null);
            if (bluetoothManager != null) {
                this.mEnabled = true;
                this.mCachedDeviceManager = bluetoothManager.getCachedDeviceManager();
                this.mLocalBluetoothAdapter = bluetoothManager.getBluetoothAdapter();
                this.mProfileManager = bluetoothManager.getProfileManager();
                bluetoothManager.getEventManager().registerCallback(new BluetoothCallbackHandler());
                Utils.setErrorListener(new BluetoothErrorListener());
                InputManager im = (InputManager) context.getSystemService(InputManager.class);
                im.registerOnTabletModeChangedListener(this, this.mHandler);
                this.mInTabletMode = im.isInTabletMode();
                processKeyboardState();
                this.mUIHandler = new KeyboardUIHandler();
            }
        }
    }

    /* access modifiers changed from: private */
    public void processKeyboardState() {
        this.mHandler.removeMessages(2);
        if (!this.mEnabled) {
            this.mState = -1;
        } else if (!this.mBootCompleted) {
            this.mState = 1;
        } else if (this.mInTabletMode != 0) {
            if (this.mState == 3) {
                stopScanning();
            } else if (this.mState == 4) {
                this.mUIHandler.sendEmptyMessage(9);
            }
            this.mState = 2;
        } else {
            int btState = this.mLocalBluetoothAdapter.getState();
            if ((btState == 11 || btState == 12) && this.mState == 4) {
                this.mUIHandler.sendEmptyMessage(9);
            }
            if (btState == 11) {
                this.mState = 4;
            } else if (btState != 12) {
                this.mState = 4;
                showBluetoothDialog();
            } else {
                CachedBluetoothDevice device = getPairedKeyboard();
                if (this.mState == 2 || this.mState == 4) {
                    if (device != null) {
                        this.mState = 6;
                        device.connect(false);
                        return;
                    }
                    this.mCachedDeviceManager.clearNonBondedDevices();
                }
                CachedBluetoothDevice device2 = getDiscoveredKeyboard();
                if (device2 != null) {
                    this.mState = 5;
                    device2.startPairing();
                } else {
                    this.mState = 3;
                    startScanning();
                }
            }
        }
    }

    public void onBootCompletedInternal() {
        this.mBootCompleted = true;
        this.mBootCompletedTime = SystemClock.uptimeMillis();
        if (this.mState == 1) {
            processKeyboardState();
        }
    }

    private void showBluetoothDialog() {
        if (isUserSetupComplete()) {
            long now = SystemClock.uptimeMillis();
            long earliestDialogTime = this.mBootCompletedTime + 10000;
            if (earliestDialogTime < now) {
                this.mUIHandler.sendEmptyMessage(8);
            } else {
                this.mHandler.sendEmptyMessageAtTime(2, earliestDialogTime);
            }
        } else {
            this.mLocalBluetoothAdapter.enable();
        }
    }

    private boolean isUserSetupComplete() {
        return Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "user_setup_complete", 0, -2) != 0;
    }

    private CachedBluetoothDevice getPairedKeyboard() {
        for (BluetoothDevice d : this.mLocalBluetoothAdapter.getBondedDevices()) {
            if (this.mKeyboardName.equals(d.getName())) {
                return getCachedBluetoothDevice(d);
            }
        }
        return null;
    }

    private CachedBluetoothDevice getDiscoveredKeyboard() {
        for (CachedBluetoothDevice d : this.mCachedDeviceManager.getCachedDevicesCopy()) {
            if (d.getName().equals(this.mKeyboardName)) {
                return d;
            }
        }
        return null;
    }

    /* access modifiers changed from: private */
    public CachedBluetoothDevice getCachedBluetoothDevice(BluetoothDevice d) {
        CachedBluetoothDevice cachedDevice = this.mCachedDeviceManager.findDevice(d);
        if (cachedDevice == null) {
            return this.mCachedDeviceManager.addDevice(this.mLocalBluetoothAdapter, this.mProfileManager, d);
        }
        return cachedDevice;
    }

    private void startScanning() {
        BluetoothLeScanner scanner = getBluetoothLeScanner();
        if (scanner != null) {
            ScanFilter filter = new ScanFilter.Builder().setDeviceName(this.mKeyboardName).build();
            ScanSettings settings = new ScanSettings.Builder().setCallbackType(1).setNumOfMatches(1).setScanMode(2).setReportDelay(0).build();
            this.mScanCallback = new KeyboardScanCallback();
            scanner.startScan(Arrays.asList(new ScanFilter[]{filter}), settings, this.mScanCallback);
            KeyboardHandler keyboardHandler = this.mHandler;
            int i = this.mScanAttempt + 1;
            this.mScanAttempt = i;
            this.mHandler.sendMessageDelayed(keyboardHandler.obtainMessage(10, i, 0), 30000);
        }
    }

    private void stopScanning() {
        if (this.mScanCallback != null) {
            BluetoothLeScanner scanner = getBluetoothLeScanner();
            if (scanner != null) {
                scanner.stopScan(this.mScanCallback);
            }
            this.mScanCallback = null;
        }
    }

    private BluetoothLeScanner getBluetoothLeScanner() {
        try {
            return (BluetoothLeScanner) LocalBluetoothAdapter.class.getDeclaredMethod("getBluetoothLeScanner", new Class[0]).invoke(this.mLocalBluetoothAdapter, new Object[0]);
        } catch (Exception e) {
            Log.d("KeyboardUI", "getBluetoothLeScanner exception!", new Object[0]);
            return null;
        }
    }

    /* access modifiers changed from: private */
    public void bleAbortScanInternal(int scanAttempt) {
        if (this.mState == 3 && scanAttempt == this.mScanAttempt) {
            stopScanning();
            this.mState = 9;
        }
    }

    /* access modifiers changed from: private */
    public void onDeviceAddedInternal(CachedBluetoothDevice d) {
        if (this.mState == 3 && d.getName().equals(this.mKeyboardName)) {
            stopScanning();
            d.startPairing();
            this.mState = 5;
        }
    }

    /* access modifiers changed from: private */
    public void onBluetoothStateChangedInternal(int bluetoothState) {
        if (bluetoothState == 12 && this.mState == 4) {
            processKeyboardState();
        }
    }

    /* access modifiers changed from: private */
    public void onDeviceBondStateChangedInternal(CachedBluetoothDevice d, int bondState) {
        if (this.mState == 5 && d.getName().equals(this.mKeyboardName)) {
            if (bondState == 12) {
                this.mState = 6;
            } else if (bondState == 10) {
                this.mState = 7;
            }
        }
    }

    /* access modifiers changed from: private */
    public void onBleScanFailedInternal() {
        this.mScanCallback = null;
        if (this.mState == 3) {
            this.mState = 9;
        }
    }

    /* access modifiers changed from: private */
    public void onShowErrorInternal(Context context, String name, int messageResId) {
        if ((this.mState == 5 || this.mState == 7) && this.mKeyboardName.equals(name)) {
            Toast.makeText(context, context.getString(messageResId, new Object[]{name}), 0).show();
        }
    }

    private static String stateToString(int state) {
        if (state == -1) {
            return "STATE_NOT_ENABLED";
        }
        switch (state) {
            case 1:
                return "STATE_WAITING_FOR_BOOT_COMPLETED";
            case 2:
                return "STATE_WAITING_FOR_TABLET_MODE_EXIT";
            case 3:
                return "STATE_WAITING_FOR_DEVICE_DISCOVERY";
            case 4:
                return "STATE_WAITING_FOR_BLUETOOTH";
            case 5:
                return "STATE_PAIRING";
            case 6:
                return "STATE_PAIRED";
            case 7:
                return "STATE_PAIRING_FAILED";
            case 8:
                return "STATE_USER_CANCELLED";
            case 9:
                return "STATE_DEVICE_NOT_FOUND";
            default:
                return "STATE_UNKNOWN (" + state + ")";
        }
    }
}
