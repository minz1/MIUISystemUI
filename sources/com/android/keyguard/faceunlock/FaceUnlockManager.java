package com.android.keyguard.faceunlock;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.miuiface.IMiuiFaceManager;
import android.hardware.miuiface.MiuiFaceFactory;
import android.hardware.miuiface.Miuiface;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.provider.Settings;
import android.util.Log;
import android.util.Slog;
import android.view.Surface;
import android.view.TextureView;
import com.android.keyguard.BoostFrameworkHelper;
import com.android.keyguard.FingerprintHelper;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.MiuiKeyguardUtils;
import com.android.keyguard.analytics.AnalyticsHelper;
import com.android.systemui.R;
import miui.os.Build;

public class FaceUnlockManager {
    protected static Context mContext;
    private static FaceUnlockManager sFaceUnlockManager;
    private static boolean sInitFaceUlockUtil = false;
    public static long sStageInFaceUnlockTime;
    public static long sStartFaceUnlockTime;
    /* access modifiers changed from: private */
    public CancellationSignal mAuhtenCancelSignal = null;
    IMiuiFaceManager.AuthenticationCallback mAuthenCallback = new IMiuiFaceManager.AuthenticationCallback() {
        public void onAuthenticationSucceeded(Miuiface result) {
            FaceUnlockManager.super.onAuthenticationSucceeded(result);
            Log.i("face_unlock", " authenCallback, onAuthenticationSucceeded");
            Log.d("face_unlock", "receive verify passed time=" + (System.currentTimeMillis() - FaceUnlockManager.sStartFaceUnlockTime));
            FaceUnlockManager.sStageInFaceUnlockTime = System.currentTimeMillis();
            CancellationSignal unused = FaceUnlockManager.this.mAuhtenCancelSignal = null;
            FaceUnlockManager.this.mCompareSuccess = true;
            boolean unused2 = FaceUnlockManager.this.mFaceUnlockDetectRunning = false;
            FaceUnlockManager.this.mMainHandler.sendEmptyMessage(1002);
            FaceUnlockManager.this.mMainHandler.sendEmptyMessage(1009);
            FaceUnlockManager.this.mFingerprintHelper.resetFingerLockoutTime();
        }

        public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
            int string;
            FaceUnlockManager.super.onAuthenticationHelp(helpCode, helpString);
            Log.i("face_unlock", " authenCallback, onAuthenticationHelp helpCode:" + helpCode + " helpString:" + helpString);
            if (helpCode != 5) {
                FaceUnlockManager.this.mHasFace = true;
            }
            if (helpCode == 14) {
                FaceUnlockManager.this.mLiveAttackValue++;
                if (FaceUnlockManager.this.mLiveAttackValue >= 3) {
                    FaceUnlockManager.this.mLiveAttack = true;
                }
            } else {
                FaceUnlockManager.this.mLiveAttackValue = 0;
            }
            if (MiuiKeyguardUtils.isScreenTurnOnDelayed() && (helpCode == 5 || helpCode == 22 || helpCode == 14)) {
                FaceUnlockManager.this.mNoFaceDetectedValue++;
                if (FaceUnlockManager.this.mNoFaceDetectedValue >= 3) {
                    FaceUnlockManager.this.mFaceUnlockCallback.unblockScreenOn();
                    MiuiKeyguardUtils.setScreenTurnOnDelayed(false);
                }
            }
            switch (helpCode) {
                case 3:
                    string = R.string.unlock_failed;
                    break;
                case 4:
                    string = R.string.face_unlock_be_on_the_screen;
                    break;
                case 5:
                    string = R.string.face_unlock_not_found;
                    break;
                default:
                    switch (helpCode) {
                        case 8:
                        case 9:
                        case 10:
                        case 11:
                            break;
                        case 12:
                            string = R.string.face_unlock_check_failed;
                            break;
                        case 13:
                        case 14:
                            string = FaceUnlockManager.this.mLiveAttack ? R.string.face_unlock_check_failed : R.string.face_unlock_be_on_the_screen;
                            break;
                        default:
                            switch (helpCode) {
                                case 21:
                                    string = R.string.face_unlock_reveal_eye;
                                    break;
                                case 22:
                                    string = R.string.face_unlock_open_eye;
                                    break;
                                case 23:
                                    string = R.string.face_unlock_reveal_mouth;
                                    break;
                                default:
                                    string = R.string.face_unlock_check_failed;
                                    break;
                            }
                    }
                    string = R.string.face_unlock_be_on_the_screen;
                    break;
            }
            FaceUnlockManager.this.mHelpStringResId = string;
            FaceUnlockManager.this.mMainHandler.sendEmptyMessage(1003);
        }

        public void onAuthenticationFailed() {
            Log.i("face_unlock", "authenCallback, onAuthenticationFailed");
            AnalyticsHelper.trackFaceUnlockFailCount(FaceUnlockManager.this.mHasFace);
            FaceUnlockManager.this.mMainHandler.sendEmptyMessage(1005);
            FaceUnlockManager.this.stopFaceUnlock();
            CancellationSignal unused = FaceUnlockManager.this.mAuhtenCancelSignal = null;
        }

        public void onAuthenticationError(int errorCode, CharSequence errString) {
            Log.i("face_unlock", "authenCallback, onAuthenticationError code:" + errorCode + " msg:" + errString);
            CancellationSignal unused = FaceUnlockManager.this.mAuhtenCancelSignal = null;
            boolean unused2 = FaceUnlockManager.this.mFaceUnlockDetectRunning = false;
            FaceUnlockManager.this.mMainHandler.sendEmptyMessage(1009);
            if (!FaceUnlockManager.this.mStartFaceUnlockSuccess) {
                FaceUnlockManager.this.mMainHandler.sendEmptyMessage(1010);
            }
        }
    };
    protected boolean mCompareSuccess;
    private CancellationSignal mEnrollCancelSignal = null;
    private IMiuiFaceManager mFaceManager = null;
    /* access modifiers changed from: private */
    public FaceUnlockCallback mFaceUnlockCallback = null;
    /* access modifiers changed from: private */
    public boolean mFaceUnlockDetectRunning;
    protected int mFailedCount;
    protected int mFailedLiveCount;
    protected FingerprintHelper mFingerprintHelper;
    protected HandlerThread mHandlerThread = new HandlerThread("face_unlock");
    protected boolean mHasFace = false;
    protected int mHelpStringResId;
    protected boolean mLiveAttack = false;
    protected int mLiveAttackValue;
    protected Handler mMainHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1001:
                    FaceUnlockManager.this.mFaceUnlockCallback.onFaceStart();
                    return;
                case 1002:
                    FaceUnlockManager.this.mFaceUnlockCallback.onFaceAuthenticated();
                    return;
                case 1003:
                    FaceUnlockManager.this.mFaceUnlockCallback.onFaceHelp(FaceUnlockManager.this.mHelpStringResId);
                    return;
                case 1005:
                    if (FaceUnlockManager.this.mHasFace) {
                        FaceUnlockManager.this.mFaceUnlockCallback.onFaceHelp(R.string.face_unlock_check_failed);
                    }
                    FaceUnlockManager.this.mFaceUnlockCallback.onFaceAuthFailed(FaceUnlockManager.this.mHasFace);
                    return;
                case 1006:
                    FaceUnlockManager.this.mFaceUnlockCallback.onFaceLocked();
                    return;
                case 1009:
                    break;
                case 1010:
                    FaceUnlockManager.this.mFaceUnlockCallback.restartFaceUnlock();
                    break;
                default:
                    return;
            }
            FaceUnlockManager.this.mFaceUnlockCallback.onFaceStop();
            if (MiuiKeyguardUtils.isSupportLiftingCamera(FaceUnlockManager.mContext) && FaceUnlockManager.this.isFaceUnlockLocked()) {
                FaceUnlockManager.this.mFaceUnlockCallback.onFaceLocked();
            }
        }
    };
    protected int mNoFaceDetectedValue = 0;
    /* access modifiers changed from: private */
    public boolean mStartFaceUnlockSuccess = true;
    protected Handler mWorkerHandler;

    public static FaceUnlockManager getInstance(Context context) {
        if (sFaceUnlockManager == null) {
            sFaceUnlockManager = new FaceUnlockManager(context);
        } else if (!sInitFaceUlockUtil) {
            sFaceUnlockManager.initFaceUnlockUtil();
        }
        return sFaceUnlockManager;
    }

    private FaceUnlockManager(Context context) {
        mContext = context;
        initFaceUnlockUtil();
    }

    public void initFaceUnlockUtil() {
        this.mFaceManager = MiuiFaceFactory.getFaceManager(mContext, 0);
        if (MiuiKeyguardUtils.isSupportFaceUnlock(mContext)) {
            this.mHandlerThread.start();
            this.mWorkerHandler = new Handler(this.mHandlerThread.getLooper());
            BoostFrameworkHelper.initBoostFramework();
            sInitFaceUlockUtil = true;
        }
        this.mFingerprintHelper = new FingerprintHelper(mContext);
    }

    public void runOnFaceUnlockWorkerThread(Runnable r) {
        if (this.mHandlerThread != null && this.mWorkerHandler != null) {
            if (this.mHandlerThread.getThreadId() == Process.myTid()) {
                r.run();
            } else {
                this.mWorkerHandler.post(r);
            }
        }
    }

    public void initAll() {
        this.mFaceManager.preInitAuthen();
    }

    public boolean hasInit() {
        return true;
    }

    public boolean needInitFaceUnlock() {
        return true;
    }

    public boolean isValidFeature() {
        boolean z = true;
        if ("ursa".equals(Build.DEVICE)) {
            if (Settings.Secure.getIntForUser(mContext.getContentResolver(), "face_unlock_valid_feature", 0, KeyguardUpdateMonitor.getCurrentUser()) != 1) {
                z = false;
            }
            return z;
        }
        if (this.mFaceManager == null) {
            this.mFaceManager = MiuiFaceFactory.getFaceManager(mContext, 0);
        }
        if (this.mFaceManager.hasEnrolledFaces() == -1) {
            z = false;
        }
        return z;
    }

    public boolean hasEnrolledFaces() {
        boolean z = true;
        if ("ursa".equals(Build.DEVICE)) {
            if (Settings.Secure.getIntForUser(mContext.getContentResolver(), "face_unlock_has_feature", 0, KeyguardUpdateMonitor.getCurrentUser()) != 1) {
                z = false;
            }
            return z;
        }
        if (this.mFaceManager == null) {
            this.mFaceManager = MiuiFaceFactory.getFaceManager(mContext, 0);
        }
        if (this.mFaceManager.hasEnrolledFaces() <= 0) {
            z = false;
        }
        return z;
    }

    public void startFaceUnlock(TextureView textureView, FaceUnlockCallback callback) {
        if (isFaceUnlockLocked()) {
            this.mMainHandler.sendEmptyMessage(1006);
            return;
        }
        this.mFaceUnlockCallback = callback;
        if (this.mAuhtenCancelSignal != null) {
            Log.d("face_unlock", "start face unlock is runing");
            this.mStartFaceUnlockSuccess = false;
            return;
        }
        this.mLiveAttackValue = 0;
        this.mNoFaceDetectedValue = 0;
        this.mHasFace = false;
        this.mLiveAttack = false;
        this.mCompareSuccess = false;
        this.mStartFaceUnlockSuccess = true;
        this.mFaceUnlockDetectRunning = true;
        sStartFaceUnlockTime = System.currentTimeMillis();
        Slog.i("face_unlock", "start verify time=" + (System.currentTimeMillis() - KeyguardUpdateMonitor.sScreenTurnedOnTime));
        sStageInFaceUnlockTime = System.currentTimeMillis();
        this.mAuhtenCancelSignal = new CancellationSignal();
        this.mFaceManager.authenticate(this.mAuhtenCancelSignal, 0, this.mAuthenCallback, this.mWorkerHandler, 5000);
        sStageInFaceUnlockTime = System.currentTimeMillis();
        BoostFrameworkHelper.setBoost(3);
        this.mMainHandler.sendEmptyMessage(1001);
    }

    public void stopFaceUnlock() {
        if (this.mAuhtenCancelSignal != null && this.mFaceUnlockDetectRunning) {
            this.mMainHandler.sendEmptyMessage(1009);
            if (!this.mAuhtenCancelSignal.isCanceled()) {
                Log.i("face_unlock", "call stopFaceUnlock cancel()");
                this.mAuhtenCancelSignal.cancel();
            }
            this.mFaceUnlockDetectRunning = false;
            this.mEnrollCancelSignal = null;
            if (this.mHasFace && !this.mCompareSuccess) {
                this.mFailedCount++;
                if (this.mFailedCount == 5) {
                    AnalyticsHelper.trackFaceUnlockLocked();
                }
            }
            if (this.mLiveAttack) {
                this.mFailedLiveCount++;
                if (this.mFailedLiveCount == 3) {
                    AnalyticsHelper.trackFaceUnlockLocked();
                }
            }
        }
    }

    public boolean enrollFace(SurfaceTexture surfaceTexture, IMiuiFaceManager.EnrollmentCallback enrollCallback) {
        if (this.mEnrollCancelSignal == null || this.mEnrollCancelSignal.isCanceled()) {
            Slog.i("face_unlock", "start enrollFace");
            if (Build.DEVICE.equals("grus")) {
                surfaceTexture.setDefaultBufferSize(800, 600);
            } else {
                surfaceTexture.setDefaultBufferSize(640, 480);
            }
            Surface surface = new Surface(surfaceTexture);
            this.mEnrollCancelSignal = new CancellationSignal();
            this.mFaceManager.enroll(null, this.mEnrollCancelSignal, 0, enrollCallback, surface, new Rect(20, 20, 460, 600), 30000);
            return true;
        }
        Log.i("face_unlock", "call mEnrollCancelSignal.cancel(), return.");
        this.mEnrollCancelSignal.cancel();
        this.mEnrollCancelSignal = null;
        return false;
    }

    public void stopEnrollFace() {
        Slog.i("face_unlock", "stop enrollFace");
        if (this.mEnrollCancelSignal != null && !this.mEnrollCancelSignal.isCanceled()) {
            Log.i("face_unlock", "call mEnrollCancelSignal.cancel(), return.");
            this.mEnrollCancelSignal.cancel();
            this.mEnrollCancelSignal = null;
        }
    }

    public void release() {
    }

    public void release(boolean delay) {
    }

    public void deleteFeature() {
        for (int i = 0; i < this.mFaceManager.getEnrolledFaces().size(); i++) {
            this.mFaceManager.remove((Miuiface) this.mFaceManager.getEnrolledFaces().get(i), null);
        }
    }

    public void resetFailCount() {
        this.mFailedCount = 0;
        this.mFailedLiveCount = 0;
    }

    /* access modifiers changed from: protected */
    public boolean isFaceUnlockLocked() {
        return this.mFailedCount >= 5 || this.mFailedLiveCount >= 3;
    }
}
