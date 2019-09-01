package com.android.keyguard;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.provider.Settings;
import android.security.MiuiLockPatternUtils;
import android.util.Log;
import android.util.Slog;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.HeiHeiGestureView;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.statusbar.phone.NotificationPanelView;
import com.android.systemui.statusbar.phone.PanelBar;
import com.android.systemui.statusbar.phone.StatusBar;
import com.miui.internal.policy.impl.AwesomeLockScreenImp.AwesomeLockScreenView;
import com.miui.internal.policy.impl.AwesomeLockScreenImp.LockScreenRoot;
import miui.maml.util.Utils;

public class AwesomeLockScreen extends FrameLayout implements LockScreenRoot.LockscreenCallback {
    /* access modifiers changed from: private */
    public static RootHolder mRootHolder = new RootHolder();
    private static int mThemeChanged;
    private static long sStartTime;
    static boolean sSuppressNextLockSound;
    private static long sTotalWakenTime;
    private boolean isPaused;
    private AudioManager mAudioManager;
    private PanelBar mBar;
    /* access modifiers changed from: private */
    public boolean mInitSuccessful;
    private boolean mIsFocus;
    private LockPatternUtils mLockPatternUtils;
    private LockPatternUtilsWrapper mLockPatternUtilsWrapper;
    private AwesomeLockScreenView mLockscreenView;
    private NotificationPanelView mPanelView;
    private int mPasswordMode;
    private StatusBar mStatusBar;
    private KeyguardUpdateMonitor mUpdateMonitor;
    KeyguardUpdateMonitorCallback mUpdateMonitorCallback;
    private long mWakeStartTime;

    public AwesomeLockScreen(Context context, StatusBar statusBar, NotificationPanelView panelView, PanelBar panelBar) {
        this(context);
        this.mStatusBar = statusBar;
        this.mPanelView = panelView;
        this.mBar = panelBar;
        if (this.mLockscreenView != null) {
            this.mLockscreenView.setPanelView(panelView);
        }
        this.mUpdateMonitor = KeyguardUpdateMonitor.getInstance(context);
    }

    /* JADX WARNING: type inference failed for: r9v1, types: [com.miui.internal.policy.impl.AwesomeLockScreenImp.AwesomeLockScreenView, android.view.View] */
    AwesomeLockScreen(Context context) {
        super(context);
        int i = 0;
        this.isPaused = false;
        this.mIsFocus = true;
        this.mUpdateMonitorCallback = new KeyguardUpdateMonitorCallback() {
            public void onRefreshBatteryInfo(KeyguardUpdateMonitor.BatteryStatus status) {
                super.onRefreshBatteryInfo(status);
                Log.d("AwesomeLockScreen", "onRefreshBatteryInfo: isBatteryLow = " + status.isBatteryLow() + " isPluggedIn = " + status.isPluggedIn() + " level = " + status.level);
                if (AwesomeLockScreen.this.mInitSuccessful) {
                    AwesomeLockScreen.mRootHolder.getRoot().onRefreshBatteryInfo(status);
                }
            }
        };
        this.mLockPatternUtils = new LockPatternUtils(context);
        this.mLockPatternUtilsWrapper = new LockPatternUtilsWrapper(this.mLockPatternUtils);
        setFocusable(true);
        setFocusableInTouchMode(true);
        this.mAudioManager = (AudioManager) context.getSystemService("audio");
        int version = context.getResources().getConfiguration().extraConfig.themeChanged;
        if (version > mThemeChanged) {
            clearCache();
            mThemeChanged = version;
        }
        if (!mRootHolder.init(this.mContext, this)) {
            Slog.e("AwesomeLockScreen", "fail to init RootHolder");
            return;
        }
        Utils.putVariableString("owner_info", mRootHolder.getContext().mVariables, this.mLockPatternUtilsWrapper.isOwnerInfoEnabled() ? new MiuiLockPatternUtils(context).getOwnerInfo() : null);
        HeiHeiGestureView gestureView = new HeiHeiGestureView(this.mContext);
        gestureView.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        gestureView.setOnTriggerListener(new HeiHeiGestureView.OnTriggerListener() {
            public void onTrigger() {
                AwesomeLockScreen.sSuppressNextLockSound = true;
                AwesomeLockScreen.this.collapsePanel();
            }
        });
        addView(gestureView);
        this.mPasswordMode = getPasswordMode();
        mRootHolder.getContext().mVariables.put("__password_mode", (double) this.mPasswordMode);
        mRootHolder.getRoot().setLockscreenCallback(this);
        this.mLockscreenView = mRootHolder.createView(this.mContext);
        if (this.mLockscreenView != null) {
            gestureView.addView(this.mLockscreenView, new FrameLayout.LayoutParams(-1, -1));
            this.mInitSuccessful = true;
        }
        if (sStartTime == 0) {
            sStartTime = System.currentTimeMillis() / 1000;
        }
        this.mWakeStartTime = System.currentTimeMillis() / 1000;
        onPause();
        mRootHolder.getRoot().setBgColor(this.mPasswordMode != 0 ? -16777216 : i);
    }

    public void rebindView() {
        if (this.mInitSuccessful) {
            mRootHolder.getRoot().setLockscreenCallback(this);
            this.mLockscreenView.rebindRoot();
        }
    }

    public static void clearCache() {
        mRootHolder.clear();
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mUpdateMonitor.registerCallback(this.mUpdateMonitorCallback);
        updateStatusBarColormode();
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        this.mUpdateMonitor.removeCallback(this.mUpdateMonitorCallback);
        cleanUp();
        super.onDetachedFromWindow();
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public void onPause() {
        if (!this.isPaused) {
            Log.d("AwesomeLockScreen", "onPause");
            this.isPaused = true;
            pause();
        }
    }

    private void pause() {
        if (this.mInitSuccessful) {
            this.mLockscreenView.pause();
            mRootHolder.getRoot().onCommand("pause");
            sTotalWakenTime += (System.currentTimeMillis() / 1000) - this.mWakeStartTime;
        }
    }

    public void onResume(boolean resumeAnimation) {
        if (this.isPaused) {
            Log.d("AwesomeLockScreen", "onResume");
            this.isPaused = false;
            if (this.mIsFocus) {
                resume();
            }
            updateStatusBarColormode();
        }
    }

    private void resume() {
        if (this.mInitSuccessful) {
            this.mLockscreenView.resume();
            mRootHolder.getRoot().onCommand("resume");
            this.mWakeStartTime = System.currentTimeMillis() / 1000;
        }
    }

    public void cleanUp() {
        mRootHolder.cleanUp(this);
    }

    public void unlocked(Intent intent, int delay) {
        sendLockscreenIntentTypeAnalytics(intent);
        postDelayed(new Runnable() {
            public void run() {
                try {
                    AwesomeLockScreen.this.collapsePanel();
                } catch (ActivityNotFoundException e) {
                    Log.e("AwesomeLockScreen", e.toString());
                    e.printStackTrace();
                }
            }
        }, (long) delay);
        if (MiuiKeyguardUtils.isSupportLiftingCamera(this.mContext)) {
            this.mUpdateMonitor.startFaceUnlock(true);
        }
        Log.d("AwesomeLockScreen", String.format("lockscreen awake time: [%d sec] in time range: [%d sec]", new Object[]{Long.valueOf(sTotalWakenTime), Long.valueOf((System.currentTimeMillis() / 1000) - sStartTime)}));
    }

    public boolean unlockVerify(String password, int delay) {
        this.mPasswordMode = getPasswordMode();
        mRootHolder.getRoot().getVariables().put("__password_mode", (double) this.mPasswordMode);
        if (this.mPasswordMode != 0 && this.mPasswordMode != -1) {
            return false;
        }
        collapsePanel();
        return true;
    }

    /* access modifiers changed from: private */
    public void collapsePanel() {
        this.mPanelView.collapse(false, 1.0f);
    }

    private void sendLockscreenIntentTypeAnalytics(Intent intent) {
        Intent analyticsIntent = new Intent("miui.intent.action.TRACK_EVENT");
        analyticsIntent.putExtra("eventId", "lockscreen_intent_type");
        analyticsIntent.putExtra("eventObj", intent == null ? "" : intent.toString());
    }

    public int getPasswordMode() {
        int pass = this.mLockPatternUtilsWrapper.getKeyguardStoredPasswordQuality();
        if (pass == 0) {
            return 0;
        }
        if (pass == 131072 || pass == 196608) {
            return 1;
        }
        return 10;
    }

    public void pokeWakelock() {
        this.mStatusBar.userActivity();
    }

    public void haptic(int effectId) {
        performHapticFeedback(1);
    }

    public boolean isSoundEnable() {
        boolean lockSoundsEnabled = Settings.System.getIntForUser(this.mContext.getContentResolver(), "lockscreen_sounds_enabled", 1, KeyguardUpdateMonitor.getCurrentUser()) != 0;
        if (this.mAudioManager.getRingerMode() != 2 || !lockSoundsEnabled) {
            return false;
        }
        return true;
    }

    private void updateStatusBarColormode() {
    }

    public boolean isSecure() {
        return this.mLockPatternUtilsWrapper.isSecure();
    }

    public void cleanUpView() {
        if (this.mInitSuccessful) {
            this.mLockscreenView.getRoot().finish();
            this.mLockscreenView.cleanUp(true);
        }
    }

    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        this.mIsFocus = hasWindowFocus;
        if (!hasWindowFocus) {
            pause();
        } else if (!this.isPaused) {
            resume();
        }
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (this.mInitSuccessful) {
            return super.onTouchEvent(event);
        }
        collapsePanel();
        return true;
    }
}
