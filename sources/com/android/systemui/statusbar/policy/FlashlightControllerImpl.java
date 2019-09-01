package com.android.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.miui.Shell;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import com.android.systemui.Constants;
import com.android.systemui.R;
import com.android.systemui.Util;
import com.android.systemui.statusbar.policy.FlashlightController;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import miui.os.Build;

public class FlashlightControllerImpl implements FlashlightController {
    /* access modifiers changed from: private */
    public static final boolean DEBUG = Log.isLoggable("FlashlightController", 3);
    public static final String[] FLASH_DEVICES = {"/sys/class/leds/flashlight/brightness", "/sys/class/leds/spotlight/brightness"};
    /* access modifiers changed from: private */
    public Handler mBgHandler;
    /* access modifiers changed from: private */
    public String mCameraId;
    private final CameraManager mCameraManager;
    /* access modifiers changed from: private */
    public final Context mContext;
    /* access modifiers changed from: private */
    public String mFlashDevice;
    /* access modifiers changed from: private */
    public boolean mFlashlightEnabled;
    /* access modifiers changed from: private */
    public boolean mForceOff;
    private Handler mHandler;
    private final ArrayList<WeakReference<FlashlightController.FlashlightListener>> mListeners = new ArrayList<>(1);
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            boolean newState;
            String action = intent.getAction();
            if ("miui.intent.action.TOGGLE_TORCH".equals(action)) {
                boolean isToggle = intent.getBooleanExtra("miui.intent.extra.IS_TOGGLE", false);
                if (isToggle) {
                    newState = !FlashlightControllerImpl.this.mFlashlightEnabled;
                } else {
                    newState = intent.getBooleanExtra("miui.intent.extra.IS_ENABLE", false);
                }
                Slog.d("FlashlightController", String.format("onReceive: isToggle=%b, newState=%b, from=%s", new Object[]{Boolean.valueOf(isToggle), Boolean.valueOf(newState), intent.getSender()}));
                FlashlightControllerImpl.this.setFlashlight(newState);
            } else if ("action_temp_state_change".equals(action)) {
                boolean forceOff = intent.getIntExtra("temp_state", 0) == 1;
                if (forceOff && FlashlightControllerImpl.this.mFlashlightEnabled) {
                    Slog.d("FlashlightController", String.format("onReceive: forceOff=%b, state=%b, from=%s", new Object[]{Boolean.valueOf(forceOff), false, intent.getSender()}));
                    FlashlightControllerImpl.this.setFlashlight(false);
                    FlashlightControllerImpl.this.postShowToast();
                }
                if (FlashlightControllerImpl.this.mForceOff != forceOff) {
                    boolean unused = FlashlightControllerImpl.this.mForceOff = forceOff;
                    FlashlightControllerImpl.this.dispatchAvailabilityChanged(FlashlightControllerImpl.this.isAvailable());
                }
            }
        }
    };
    /* access modifiers changed from: private */
    public Runnable mStatusDetecting;
    /* access modifiers changed from: private */
    public boolean mTorchAvailable;
    private final CameraManager.TorchCallback mTorchCallback = new CameraManager.TorchCallback() {
        public void onTorchModeUnavailable(String cameraId) {
            if (TextUtils.equals(cameraId, FlashlightControllerImpl.this.mCameraId)) {
                Slog.d("FlashlightController", "TorchCallback: onTorchModeUnavailable");
                setCameraAvailable(false);
            }
        }

        public void onTorchModeChanged(String cameraId, boolean enabled) {
            if (TextUtils.equals(cameraId, FlashlightControllerImpl.this.mCameraId)) {
                Slog.d("FlashlightController", "TorchCallback: onTorchModeChanged: enabled: " + enabled);
                setCameraAvailable(true);
                setTorchMode(enabled);
            }
        }

        private void setCameraAvailable(boolean available) {
            boolean changed;
            synchronized (FlashlightControllerImpl.this) {
                changed = FlashlightControllerImpl.this.mTorchAvailable != available;
                boolean unused = FlashlightControllerImpl.this.mTorchAvailable = available;
            }
            if (changed) {
                if (FlashlightControllerImpl.DEBUG) {
                    Log.d("FlashlightController", "setCameraAvailable: dispatchAvailabilityChanged(" + available + ")");
                }
                FlashlightControllerImpl.this.dispatchAvailabilityChanged(available);
            }
        }

        private void setTorchMode(boolean enabled) {
            boolean changed;
            synchronized (FlashlightControllerImpl.this) {
                changed = FlashlightControllerImpl.this.mFlashlightEnabled != enabled;
                boolean unused = FlashlightControllerImpl.this.mFlashlightEnabled = enabled;
            }
            if (changed) {
                if (FlashlightControllerImpl.DEBUG) {
                    Log.d("FlashlightController", "setCameraAvailable: dispatchModeChanged(" + enabled + ")");
                }
                FlashlightControllerImpl.this.dispatchModeChanged(enabled);
            }
        }
    };
    /* access modifiers changed from: private */
    public int mValueOn;
    /* access modifiers changed from: private */
    public String mWaringToastString;

    public FlashlightControllerImpl(Context context) {
        this.mContext = context;
        this.mCameraManager = (CameraManager) this.mContext.getSystemService("camera");
        this.mWaringToastString = this.mContext.getResources().getString(R.string.torch_high_temperature_warning);
        ensureHandler();
        this.mBgHandler.post(new Runnable() {
            public void run() {
                FlashlightControllerImpl.this.initFlash();
            }
        });
    }

    /* access modifiers changed from: private */
    public void initFlash() {
        if (Constants.SUPPORT_ANDROID_FLASHLIGHT) {
            initCameraFlash();
        } else {
            initMiuiFlash();
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction("miui.intent.action.TOGGLE_TORCH");
        filter.addAction("action_temp_state_change");
        filter.setPriority(-1000);
        this.mContext.registerReceiverAsUser(this.mReceiver, UserHandle.ALL, filter, null, this.mBgHandler);
    }

    private void initCameraFlash() {
        try {
            this.mCameraId = getCameraId();
            if (this.mCameraId != null) {
                Slog.d("FlashlightController", "initCameraFlash: register torch callback");
                this.mCameraManager.registerTorchCallback(this.mTorchCallback, this.mBgHandler);
            }
        } catch (Throwable e) {
            Log.e("FlashlightController", "Couldn't initialize.", e);
        }
    }

    private void initMiuiFlash() {
        Resources res = this.mContext.getResources();
        this.mValueOn = res.getInteger(R.integer.flash_on_value);
        this.mFlashDevice = res.getString(R.string.flash_device);
        int i = 0;
        while (!new File(this.mFlashDevice).exists()) {
            if (i == FLASH_DEVICES.length) {
                this.mFlashDevice = null;
                return;
            }
            this.mFlashDevice = FLASH_DEVICES[i];
            i++;
        }
    }

    public void setFlashlight(final boolean enabled) {
        if (this.mForceOff) {
            Slog.d("FlashlightController", "setFlashlight: force off state");
            postShowToast();
            return;
        }
        if (Constants.SUPPORT_ANDROID_FLASHLIGHT) {
            int failedCount = 0;
            while (this.mCameraId == null && failedCount < 2) {
                initCameraFlash();
                failedCount++;
            }
            if (this.mCameraId != null) {
                this.mBgHandler.post(new Runnable() {
                    public void run() {
                        FlashlightControllerImpl.this.setNormalFlashlight(enabled);
                    }
                });
            } else {
                Slog.d("FlashlightController", "setFlashlight: enabled: " + enabled + ", could not initialize cameraId");
            }
        } else {
            setMiuiFlashlight(enabled);
        }
    }

    /* access modifiers changed from: private */
    public void setNormalFlashlight(boolean enabled) {
        boolean pendingError = false;
        synchronized (this) {
            if (!this.mTorchAvailable) {
                Slog.d("FlashlightController", "setNormalFlashlight: enabled: " + enabled + ", torchAvailable: " + this.mTorchAvailable);
                return;
            } else if (this.mFlashlightEnabled != enabled) {
                this.mFlashlightEnabled = enabled;
                try {
                    this.mCameraManager.setTorchMode(this.mCameraId, enabled);
                } catch (CameraAccessException e) {
                    Log.e("FlashlightController", "Couldn't set torch mode", e);
                    this.mFlashlightEnabled = false;
                    pendingError = true;
                }
            }
        }
        if (pendingError) {
            dispatchError();
        } else {
            dispatchModeChanged(enabled);
        }
    }

    /* access modifiers changed from: private */
    public void setMiuiFlashlight(boolean enabled) {
        if (setMiuiFlashModeInternal(enabled)) {
            this.mFlashlightEnabled = enabled;
            dispatchModeChanged(enabled);
        }
    }

    private synchronized boolean setMiuiFlashModeInternal(final boolean enabled) {
        if (!hasFlashlight()) {
            Slog.d("FlashlightController", "setFlashModeInternal: no flashlight");
            return false;
        } else if (TextUtils.isEmpty(this.mFlashDevice)) {
            Slog.d("FlashlightController", "setFlashModeInternal: no device node");
            return false;
        } else {
            try {
                if (this.mStatusDetecting == null) {
                    this.mStatusDetecting = new Runnable() {
                        public void run() {
                            boolean changed = true;
                            FileReader reader = null;
                            try {
                                FileReader reader2 = new FileReader(FlashlightControllerImpl.this.mFlashDevice);
                                changed = reader2.read() == 48;
                                try {
                                    reader2.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            } catch (Exception e2) {
                                e2.printStackTrace();
                                if (reader != null) {
                                    reader.close();
                                }
                            } catch (Throwable th) {
                                if (reader != null) {
                                    try {
                                        reader.close();
                                    } catch (IOException e3) {
                                        e3.printStackTrace();
                                    }
                                }
                                throw th;
                            }
                            if (changed) {
                                Slog.d("FlashlightController", "setFlashModeInternal: StatusDetectingRunnable: state change");
                                FlashlightControllerImpl.this.setMiuiFlashlight(false);
                                return;
                            }
                            Slog.d("FlashlightController", "setFlashModeInternal: in runnable, post delay StatusDetectingRunnable");
                            FlashlightControllerImpl.this.mBgHandler.postDelayed(FlashlightControllerImpl.this.mStatusDetecting, 1000);
                        }
                    };
                }
                if (enabled) {
                    Slog.d("FlashlightController", "setFlashModeInternal: post delay StatusDetectingRunnable");
                    this.mBgHandler.postDelayed(this.mStatusDetecting, 1000);
                } else {
                    Slog.d("FlashlightController", "setFlashModeInternal: remove StatusDetectingRunnable");
                    this.mBgHandler.removeCallbacks(this.mStatusDetecting);
                }
                this.mBgHandler.post(new Runnable() {
                    public void run() {
                        int i = 0;
                        if (!Shell.write(FlashlightControllerImpl.this.mFlashDevice, String.valueOf(enabled ? FlashlightControllerImpl.this.mValueOn : 0))) {
                            FileWriter writer = null;
                            try {
                                FileWriter writer2 = new FileWriter(FlashlightControllerImpl.this.mFlashDevice);
                                Slog.d("FlashlightController", "setFlashModeInternal: file writer write: " + enabled);
                                if (enabled) {
                                    i = FlashlightControllerImpl.this.mValueOn;
                                }
                                writer2.write(String.valueOf(i));
                                try {
                                    writer2.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            } catch (Exception e2) {
                                Log.w("FlashlightController", "FileWriter write failed!" + e2.getMessage());
                                if (writer != null) {
                                    writer.close();
                                }
                            } catch (Throwable th) {
                                if (writer != null) {
                                    try {
                                        writer.close();
                                    } catch (IOException e3) {
                                        e3.printStackTrace();
                                    }
                                }
                                throw th;
                            }
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }
    }

    public boolean hasFlashlight() {
        return Build.hasCameraFlash(this.mContext);
    }

    public synchronized boolean isEnabled() {
        return this.mFlashlightEnabled;
    }

    public synchronized boolean isAvailable() {
        boolean z;
        z = true;
        boolean deviceAvailable = Constants.SUPPORT_ANDROID_FLASHLIGHT ? this.mTorchAvailable : true;
        if (this.mForceOff || !deviceAvailable) {
            z = false;
        }
        return z;
    }

    public void addCallback(FlashlightController.FlashlightListener l) {
        synchronized (this.mListeners) {
            if (Constants.SUPPORT_ANDROID_FLASHLIGHT && this.mCameraId == null) {
                initCameraFlash();
            }
            cleanUpListenersLocked(l);
            this.mListeners.add(new WeakReference(l));
            l.onFlashlightAvailabilityChanged(isAvailable());
            l.onFlashlightChanged(this.mFlashlightEnabled);
        }
    }

    public void removeCallback(FlashlightController.FlashlightListener l) {
        synchronized (this.mListeners) {
            cleanUpListenersLocked(l);
        }
    }

    private synchronized void ensureHandler() {
        if (this.mHandler == null) {
            this.mHandler = new Handler();
        }
        if (this.mBgHandler == null) {
            HandlerThread thread = new HandlerThread("FlashlightController", 10);
            thread.start();
            this.mBgHandler = new Handler(thread.getLooper());
        }
        if (this.mHandler == null) {
            this.mHandler = new Handler(Looper.getMainLooper());
        }
    }

    private String getCameraId() throws CameraAccessException {
        for (String id : this.mCameraManager.getCameraIdList()) {
            CameraCharacteristics c = this.mCameraManager.getCameraCharacteristics(id);
            Boolean flashAvailable = (Boolean) c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            Integer lensFacing = (Integer) c.get(CameraCharacteristics.LENS_FACING);
            if (flashAvailable != null && flashAvailable.booleanValue() && lensFacing != null && lensFacing.intValue() == 1) {
                return id;
            }
        }
        return null;
    }

    private void setTorchState(boolean enabled) {
        Slog.d("FlashlightController", "setTorchState: enabled: " + enabled);
        Settings.Global.putInt(this.mContext.getContentResolver(), "torch_state", enabled);
    }

    /* access modifiers changed from: private */
    public void dispatchModeChanged(boolean enabled) {
        setTorchState(enabled);
        dispatchListeners(1, enabled);
    }

    private void dispatchError() {
        dispatchListeners(1, false);
    }

    /* access modifiers changed from: private */
    public void dispatchAvailabilityChanged(boolean available) {
        dispatchListeners(2, available);
    }

    private void dispatchListeners(int message, boolean argument) {
        synchronized (this.mListeners) {
            int N = this.mListeners.size();
            boolean cleanup = false;
            for (int i = 0; i < N; i++) {
                FlashlightController.FlashlightListener l = (FlashlightController.FlashlightListener) this.mListeners.get(i).get();
                if (l == null) {
                    cleanup = true;
                } else if (message == 0) {
                    l.onFlashlightError();
                } else if (message == 1) {
                    l.onFlashlightChanged(argument);
                } else if (message == 2) {
                    l.onFlashlightAvailabilityChanged(argument);
                }
            }
            if (cleanup) {
                cleanUpListenersLocked(null);
            }
        }
    }

    private void cleanUpListenersLocked(FlashlightController.FlashlightListener listener) {
        for (int i = this.mListeners.size() - 1; i >= 0; i--) {
            FlashlightController.FlashlightListener found = (FlashlightController.FlashlightListener) this.mListeners.get(i).get();
            if (found == null || found == listener) {
                this.mListeners.remove(i);
            }
        }
    }

    /* access modifiers changed from: private */
    public void postShowToast() {
        this.mHandler.post(new Runnable() {
            public void run() {
                Util.showSystemOverlayToast(FlashlightControllerImpl.this.mContext, FlashlightControllerImpl.this.mWaringToastString, 1);
            }
        });
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("FlashlightController state:");
        pw.print("  mCameraId=");
        pw.println(this.mCameraId);
        pw.print("  mFlashlightEnabled=");
        pw.println(this.mFlashlightEnabled);
        pw.print("  isSupportAndroidFlashlight=");
        pw.println(Constants.SUPPORT_ANDROID_FLASHLIGHT);
        pw.print("  isAvailable=");
        pw.println(isAvailable());
    }
}
