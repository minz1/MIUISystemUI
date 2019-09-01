package com.android.keyguard.doze;

import android.app.ActivityManager;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.service.dreams.DreamService;
import android.util.Log;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.MiuiKeyguardUtils;
import com.android.keyguard.doze.DozeMachine;
import com.android.keyguard.widget.AODSettings;
import com.android.systemui.Constants;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class DozeService extends DreamService implements DozeMachine.Service {
    static final boolean DEBUG = Log.isLoggable("DozeService", 3);
    /* access modifiers changed from: private */
    public boolean mAcquire;
    private DozeMachine mDozeMachine;
    private boolean mDreamStart = false;
    private Handler mHander;
    private PowerManager mPowerManager;
    private Runnable mRunnable = new Runnable() {
        public void run() {
            DozeService.this.start();
        }
    };
    /* access modifiers changed from: private */
    public PowerManager.WakeLock mWakeLock;

    public DozeService() {
        setDebug(DEBUG);
    }

    public void onCreate() {
        super.onCreate();
        setWindowless(true);
        if (DozeFactory.getHost(this) == null) {
            finish();
        } else if (!Constants.SUPPORT_AOD) {
            finish();
        } else {
            if (Settings.Secure.getIntForUser(getContentResolver(), AODSettings.AOD_MODE, 0, -2) == 0) {
                boolean invertColors = MiuiKeyguardUtils.isInvertColorsEnable(this);
                KeyguardUpdateMonitor keyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(this);
                int userId = ActivityManager.getCurrentUser();
                if ((!MiuiKeyguardUtils.isGxzwSensor() || !keyguardUpdateMonitor.isUnlockWithFingerprintPossible(userId) || invertColors) && MiuiKeyguardUtils.getKeyguardNotificationStatus(getContentResolver()) != 2) {
                    finish();
                    return;
                }
            }
            this.mDozeMachine = new DozeFactory().assembleMachine(this);
            this.mHander = new Handler();
            this.mPowerManager = (PowerManager) getSystemService("power");
            this.mWakeLock = this.mPowerManager.newWakeLock(1, "DozeService");
        }
    }

    public void startDozing() {
        if (!this.mAcquire) {
            this.mWakeLock.acquire();
            this.mAcquire = true;
        }
        this.mHander.removeCallbacks(this.mRunnable);
        this.mHander.postDelayed(this.mRunnable, 300);
    }

    /* access modifiers changed from: private */
    public void start() {
        super.startDozing();
        if (this.mAcquire) {
            this.mHander.postDelayed(new Runnable() {
                public void run() {
                    DozeService.this.mWakeLock.release();
                    boolean unused = DozeService.this.mAcquire = false;
                }
            }, 1000);
        }
    }

    public void onDreamingStarted() {
        super.onDreamingStarted();
        this.mDreamStart = true;
        this.mDozeMachine.requestState(DozeMachine.State.INITIALIZED);
        startDozing();
    }

    public void setDozeScreenBrightness(int brightness) {
        if (this.mDreamStart) {
            super.setDozeScreenBrightness(brightness);
        }
    }

    public void fingerprintPressed(boolean pressed) {
    }

    public void onDreamingStopped() {
        super.onDreamingStopped();
        this.mDreamStart = false;
        this.mDozeMachine.requestState(DozeMachine.State.FINISH);
    }

    /* access modifiers changed from: protected */
    public void dumpOnHandler(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (this.mDozeMachine != null) {
            this.mDozeMachine.dump(pw);
        }
    }

    public void requestState(DozeMachine.State requestedState) {
        this.mDozeMachine.requestState(requestedState);
    }
}
