package com.android.systemui.settings;

import android.animation.ValueAnimator;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.service.vr.IVrManager;
import android.service.vr.IVrStateCallbacks;
import android.util.Log;
import com.android.internal.logging.MetricsLogger;
import com.android.settingslib.display.BrightnessUtils;
import com.android.systemui.Dependency;
import com.android.systemui.settings.ToggleSlider;
import java.util.ArrayList;
import java.util.Iterator;
import miui.mqsas.sdk.MQSEventManagerDelegate;

public class BrightnessController implements ToggleSlider.Listener {
    /* access modifiers changed from: private */
    public volatile boolean mAutomatic;
    /* access modifiers changed from: private */
    public final boolean mAutomaticAvailable;
    /* access modifiers changed from: private */
    public final Handler mBackgroundHandler;
    /* access modifiers changed from: private */
    public final BrightnessObserver mBrightnessObserver;
    /* access modifiers changed from: private */
    public ArrayList<BrightnessStateChangeCallback> mChangeCallbacks = new ArrayList<>();
    /* access modifiers changed from: private */
    public final Context mContext;
    /* access modifiers changed from: private */
    public final ToggleSlider mControl;
    private boolean mControlValueInitialized;
    /* access modifiers changed from: private */
    public final int mDefaultBacklight;
    /* access modifiers changed from: private */
    public final int mDefaultBacklightForVr;
    private final DisplayManager mDisplayManager;
    /* access modifiers changed from: private */
    public boolean mExternalChange;
    /* access modifiers changed from: private */
    public final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            boolean z = true;
            boolean unused = BrightnessController.this.mExternalChange = true;
            try {
                switch (msg.what) {
                    case 0:
                        BrightnessController brightnessController = BrightnessController.this;
                        int i = msg.arg1;
                        if (msg.arg2 == 0) {
                            z = false;
                        }
                        brightnessController.updateSlider(i, z);
                        break;
                    case 1:
                        BrightnessController.this.mControl.setOnChangedListener(BrightnessController.this);
                        break;
                    case 2:
                        BrightnessController.this.mControl.setOnChangedListener(null);
                        break;
                    case 3:
                        BrightnessController brightnessController2 = BrightnessController.this;
                        if (msg.arg1 == 0) {
                            z = false;
                        }
                        brightnessController2.updateVrMode(z);
                        break;
                    default:
                        super.handleMessage(msg);
                        break;
                }
            } finally {
                boolean unused2 = BrightnessController.this.mExternalChange = false;
            }
        }
    };
    /* access modifiers changed from: private */
    public volatile boolean mIsVrModeEnabled;
    private boolean mListening;
    private final int mMaximumBacklight;
    private final int mMaximumBacklightForVr;
    private final int mMinimumBacklight;
    private final int mMinimumBacklightForVr;
    private ValueAnimator mSliderAnimator;
    private final Runnable mStartListeningRunnable = new Runnable() {
        public void run() {
            BrightnessController.this.mBrightnessObserver.startObserving();
            BrightnessController.this.mUserTracker.startTracking();
            BrightnessController.this.mUpdateModeRunnable.run();
            BrightnessController.this.mUpdateSliderRunnable.run();
            BrightnessController.this.mHandler.sendEmptyMessage(1);
        }
    };
    private final Runnable mStopListeningRunnable = new Runnable() {
        public void run() {
            BrightnessController.this.mBrightnessObserver.stopObserving();
            BrightnessController.this.mUserTracker.stopTracking();
            BrightnessController.this.mHandler.sendEmptyMessage(2);
        }
    };
    /* access modifiers changed from: private */
    public final Runnable mUpdateModeRunnable = new Runnable() {
        public void run() {
            if (BrightnessController.this.mAutomaticAvailable) {
                boolean z = false;
                int automatic = Settings.System.getIntForUser(BrightnessController.this.mContext.getContentResolver(), "screen_brightness_mode", 0, -2);
                BrightnessController brightnessController = BrightnessController.this;
                if (automatic != 0) {
                    z = true;
                }
                boolean unused = brightnessController.mAutomatic = z;
            }
        }
    };
    /* access modifiers changed from: private */
    public final Runnable mUpdateSliderRunnable = new Runnable() {
        public void run() {
            int val;
            boolean inVrMode = BrightnessController.this.mIsVrModeEnabled;
            if (inVrMode) {
                val = Settings.System.getIntForUser(BrightnessController.this.mContext.getContentResolver(), "screen_brightness_for_vr", BrightnessController.this.mDefaultBacklightForVr, -2);
            } else {
                val = Settings.System.getIntForUser(BrightnessController.this.mContext.getContentResolver(), "screen_brightness", BrightnessController.this.mDefaultBacklight, -2);
            }
            BrightnessController.this.mHandler.obtainMessage(0, val, inVrMode).sendToTarget();
        }
    };
    /* access modifiers changed from: private */
    public final CurrentUserTracker mUserTracker;
    private final IVrManager mVrManager;
    private final IVrStateCallbacks mVrStateCallbacks = new IVrStateCallbacks.Stub() {
        public void onVrStateChanged(boolean enabled) {
            BrightnessController.this.mHandler.obtainMessage(3, enabled, 0).sendToTarget();
        }
    };

    private class BrightnessObserver extends ContentObserver {
        private final Uri BRIGHTNESS_FOR_VR_URI = Settings.System.getUriFor("screen_brightness_for_vr");
        private final Uri BRIGHTNESS_MODE_URI = Settings.System.getUriFor("screen_brightness_mode");
        private final Uri BRIGHTNESS_URI = Settings.System.getUriFor("screen_brightness");

        public BrightnessObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }

        public void onChange(boolean selfChange, Uri uri) {
            if (!selfChange) {
                if (this.BRIGHTNESS_MODE_URI.equals(uri)) {
                    BrightnessController.this.mBackgroundHandler.post(BrightnessController.this.mUpdateModeRunnable);
                    BrightnessController.this.mBackgroundHandler.post(BrightnessController.this.mUpdateSliderRunnable);
                } else if (this.BRIGHTNESS_URI.equals(uri)) {
                    BrightnessController.this.mBackgroundHandler.post(BrightnessController.this.mUpdateSliderRunnable);
                } else if (this.BRIGHTNESS_FOR_VR_URI.equals(uri)) {
                    BrightnessController.this.mBackgroundHandler.post(BrightnessController.this.mUpdateSliderRunnable);
                } else {
                    BrightnessController.this.mBackgroundHandler.post(BrightnessController.this.mUpdateModeRunnable);
                    BrightnessController.this.mBackgroundHandler.post(BrightnessController.this.mUpdateSliderRunnable);
                }
                Iterator it = BrightnessController.this.mChangeCallbacks.iterator();
                while (it.hasNext()) {
                    ((BrightnessStateChangeCallback) it.next()).onBrightnessLevelChanged();
                }
            }
        }

        public void startObserving() {
            ContentResolver cr = BrightnessController.this.mContext.getContentResolver();
            cr.unregisterContentObserver(this);
            cr.registerContentObserver(this.BRIGHTNESS_MODE_URI, false, this, -1);
            cr.registerContentObserver(this.BRIGHTNESS_URI, false, this, -1);
            cr.registerContentObserver(this.BRIGHTNESS_FOR_VR_URI, false, this, -1);
        }

        public void stopObserving() {
            BrightnessController.this.mContext.getContentResolver().unregisterContentObserver(this);
        }
    }

    public interface BrightnessStateChangeCallback {
        void onBrightnessLevelChanged();
    }

    public BrightnessController(Context context, ToggleSlider control) {
        this.mContext = context;
        this.mControl = control;
        this.mControl.setMax(BrightnessUtils.GAMMA_SPACE_MAX);
        this.mBackgroundHandler = new Handler((Looper) Dependency.get(Dependency.BG_LOOPER));
        this.mUserTracker = new CurrentUserTracker(this.mContext) {
            public void onUserSwitched(int newUserId) {
                BrightnessController.this.mBackgroundHandler.post(BrightnessController.this.mUpdateModeRunnable);
                BrightnessController.this.mBackgroundHandler.post(BrightnessController.this.mUpdateSliderRunnable);
            }
        };
        this.mBrightnessObserver = new BrightnessObserver(this.mHandler);
        PowerManager pm = (PowerManager) context.getSystemService(PowerManager.class);
        this.mMinimumBacklight = pm.getMinimumScreenBrightnessSetting();
        this.mMaximumBacklight = pm.getMaximumScreenBrightnessSetting();
        this.mDefaultBacklight = pm.getDefaultScreenBrightnessSetting();
        this.mMinimumBacklightForVr = pm.getMinimumScreenBrightnessForVrSetting();
        this.mMaximumBacklightForVr = pm.getMaximumScreenBrightnessForVrSetting();
        this.mDefaultBacklightForVr = pm.getDefaultScreenBrightnessForVrSetting();
        this.mAutomaticAvailable = context.getResources().getBoolean(17956895);
        this.mDisplayManager = (DisplayManager) context.getSystemService(DisplayManager.class);
        this.mVrManager = IVrManager.Stub.asInterface(ServiceManager.getService("vrmanager"));
    }

    public void onInit(ToggleSlider control) {
    }

    public void registerCallbacks() {
        if (!this.mListening) {
            if (this.mVrManager != null) {
                try {
                    this.mVrManager.registerListener(this.mVrStateCallbacks);
                    this.mIsVrModeEnabled = this.mVrManager.getVrModeState();
                } catch (RemoteException e) {
                    Log.e("StatusBar.BrightnessController", "Failed to register VR mode state listener: ", e);
                }
            }
            this.mBackgroundHandler.post(this.mStartListeningRunnable);
            this.mListening = true;
        }
    }

    public void unregisterCallbacks() {
        if (this.mListening) {
            if (this.mVrManager != null) {
                try {
                    this.mVrManager.unregisterListener(this.mVrStateCallbacks);
                } catch (RemoteException e) {
                    Log.e("StatusBar.BrightnessController", "Failed to unregister VR mode state listener: ", e);
                }
            }
            this.mBackgroundHandler.post(this.mStopListeningRunnable);
            this.mListening = false;
        }
    }

    public void onStart(int value) {
        MQSEventManagerDelegate.getInstance().reportBrightnessEvent(0, value, this.mAutomatic ? 1 : 0, "");
    }

    public void onStop(int value) {
        MQSEventManagerDelegate.getInstance().reportBrightnessEvent(1, value, this.mAutomatic ? 1 : 0, "");
    }

    public void onChanged(ToggleSlider toggleSlider, final boolean tracking, int value, boolean stopTracking) {
        final String setting;
        int max;
        int min;
        int metric;
        if (!this.mExternalChange) {
            if (this.mSliderAnimator != null) {
                this.mSliderAnimator.cancel();
            }
            if (this.mIsVrModeEnabled) {
                metric = 498;
                min = this.mMinimumBacklightForVr;
                max = this.mMaximumBacklightForVr;
                setting = "screen_brightness_for_vr";
            } else {
                if (this.mAutomatic != 0) {
                    metric = 219;
                } else {
                    metric = 218;
                }
                min = this.mMinimumBacklight;
                max = this.mMaximumBacklight;
                setting = "screen_brightness";
            }
            final int val = BrightnessUtils.convertGammaToLinear(value, min, max);
            if (stopTracking) {
                MetricsLogger.action(this.mContext, metric, val);
            }
            AsyncTask.execute(new Runnable() {
                public void run() {
                    if (tracking) {
                        BrightnessController.this.setBrightness(val);
                    } else if (Settings.System.getIntForUser(BrightnessController.this.mContext.getContentResolver(), setting, -1, -2) == val) {
                        BrightnessController.this.setBrightness(-1);
                    } else {
                        Settings.System.putIntForUser(BrightnessController.this.mContext.getContentResolver(), setting, val, -2);
                    }
                }
            });
            Iterator<BrightnessStateChangeCallback> it = this.mChangeCallbacks.iterator();
            while (it.hasNext()) {
                it.next().onBrightnessLevelChanged();
            }
        }
    }

    /* access modifiers changed from: private */
    public void setBrightness(int brightness) {
        this.mDisplayManager.setTemporaryBrightness(brightness);
    }

    /* access modifiers changed from: private */
    public void updateVrMode(boolean isEnabled) {
        if (this.mIsVrModeEnabled != isEnabled) {
            this.mIsVrModeEnabled = isEnabled;
            this.mBackgroundHandler.post(this.mUpdateSliderRunnable);
        }
    }

    /* access modifiers changed from: private */
    public void updateSlider(int val, boolean inVrMode) {
        int max;
        int min;
        if (inVrMode) {
            min = this.mMinimumBacklightForVr;
            max = this.mMaximumBacklightForVr;
        } else {
            min = this.mMinimumBacklight;
            max = this.mMaximumBacklight;
        }
        if (val != BrightnessUtils.convertGammaToLinear(this.mControl.getValue(), min, max)) {
            animateSliderTo(BrightnessUtils.convertLinearToGamma(val, min, max));
        }
    }

    private void animateSliderTo(int target) {
        if (!this.mControlValueInitialized) {
            this.mControl.setValue(target);
            this.mControlValueInitialized = true;
        }
        if (this.mSliderAnimator != null && this.mSliderAnimator.isStarted()) {
            this.mSliderAnimator.cancel();
        }
        this.mSliderAnimator = ValueAnimator.ofInt(new int[]{this.mControl.getValue(), target});
        this.mSliderAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                BrightnessController.lambda$animateSliderTo$0(BrightnessController.this, valueAnimator);
            }
        });
        this.mSliderAnimator.setDuration(3000);
        this.mSliderAnimator.start();
    }

    public static /* synthetic */ void lambda$animateSliderTo$0(BrightnessController brightnessController, ValueAnimator animation) {
        brightnessController.mExternalChange = true;
        brightnessController.mControl.setValue(((Integer) animation.getAnimatedValue()).intValue());
        brightnessController.mExternalChange = false;
    }
}
