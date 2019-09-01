package com.android.systemui.statusbar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.ActivityOptions;
import android.app.admin.DevicePolicyManager;
import android.app.admin.DevicePolicyManagerCompat;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserHandleCompat;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.IBatteryStats;
import com.android.keyguard.Ease;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.keyguard.MiuiKeyguardFingerprintUtils;
import com.android.keyguard.MiuiKeyguardUtils;
import com.android.keyguard.charge.ChargeUtils;
import com.android.keyguard.utils.PhoneUtils;
import com.android.keyguard.wallpaper.WallpaperAuthorityUtils;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.statusbar.phone.KeyguardBottomAreaView;
import com.android.systemui.statusbar.phone.KeyguardIndicationTextView;
import com.android.systemui.statusbar.phone.NotificationPanelView;
import com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager;
import com.android.systemui.statusbar.policy.UserInfoController;
import com.android.systemui.util.wakelock.SettableWakeLock;
import com.android.systemui.util.wakelock.WakeLock;
import com.android.systemui.util.wakelock.WakeLockHelper;
import miui.os.Build;
import miui.util.FeatureParser;

public class KeyguardIndicationController {
    private static final Intent INTENT_EMERGENCY_DIAL = new Intent().setAction("com.android.phone.EmergencyDialer.DIAL").setPackage("com.android.phone").setFlags(343932928);
    private final IBatteryStats mBatteryInfo;
    /* access modifiers changed from: private */
    public int mBatteryLevel;
    private ObjectAnimator mBottomButtonClickAnimator;
    /* access modifiers changed from: private */
    public boolean mBouncerShowing;
    /* access modifiers changed from: private */
    public AsyncTask<?, ?, ?> mChargeAsyncTask;
    /* access modifiers changed from: private */
    public int mChargingSpeed;
    /* access modifiers changed from: private */
    public int mChargingWattage;
    /* access modifiers changed from: private */
    public final Context mContext;
    private boolean mDarkMode;
    private final DevicePolicyManager mDevicePolicyManager;
    private final KeyguardIndicationTextView mDisclosure;
    /* access modifiers changed from: private */
    public boolean mDozing;
    /* access modifiers changed from: private */
    public boolean mFaceUnlockHasDetectFace;
    /* access modifiers changed from: private */
    public boolean mFaceUnlockHasFailed;
    /* access modifiers changed from: private */
    public final int mFastThreshold;
    /* access modifiers changed from: private */
    public final Handler mHandler;
    private final ViewGroup mIndicationArea;
    /* access modifiers changed from: private */
    public boolean mIsCMSingleClicking;
    private final KeyguardBottomAreaView mKeyguardBottomAreaView;
    /* access modifiers changed from: private */
    public String mMessageToShowOnScreenOn;
    private final NotificationPanelView mNotificationPanelView;
    /* access modifiers changed from: private */
    public boolean mPowerCharged;
    /* access modifiers changed from: private */
    public boolean mPowerPluggedIn;
    /* access modifiers changed from: private */
    public final Resources mResources;
    private String mRestingIndication;
    /* access modifiers changed from: private */
    public boolean mShouldUpdateBatteryIndication;
    /* access modifiers changed from: private */
    public boolean mSignalAvailable;
    /* access modifiers changed from: private */
    public final int mSlowThreshold;
    /* access modifiers changed from: private */
    public StatusBarKeyguardViewManager mStatusBarKeyguardViewManager;
    private KeyguardUpdateMonitor.StrongAuthTracker mStrongAuthTracker;
    /* access modifiers changed from: private */
    public final KeyguardIndicationTextView mTextView;
    private View.OnClickListener mTextViewOnClickListener;
    private final BroadcastReceiver mTickReceiver;
    /* access modifiers changed from: private */
    public String mTransientIndication;
    private int mTransientTextColor;
    /* access modifiers changed from: private */
    public final ImageView mUpArrow;
    /* access modifiers changed from: private */
    public String mUpArrowIndication;
    /* access modifiers changed from: private */
    public KeyguardUpdateMonitor mUpdateMonitor;
    private KeyguardUpdateMonitorCallback mUpdateMonitorCallback;
    private final UserManager mUserManager;
    private ViewConfiguration mViewConfiguration;
    /* access modifiers changed from: private */
    public boolean mVisible;
    private final SettableWakeLock mWakeLock;

    protected class BaseKeyguardCallback extends KeyguardUpdateMonitorCallback {
        private final int FINGERPRINT_ERROR_LOCKOUT_PERMANENT_FOR_O = 9;
        private int mFingerprintAuthUserId;
        private int mFingerprintErrorMsgId;
        private MiuiKeyguardFingerprintUtils.FingerprintIdentificationState mFpiState;
        private MiuiKeyguardFingerprintUtils.FingerprintIdentificationState mLastFpiState;

        protected BaseKeyguardCallback() {
        }

        public void onRefreshBatteryInfo(KeyguardUpdateMonitor.BatteryStatus status) {
            if (!FeatureParser.getBoolean("is_pad", false)) {
                boolean isChargingOrFull = status.status == 2 || status.status == 5;
                boolean wasPluggedIn = KeyguardIndicationController.this.mPowerPluggedIn;
                boolean unused = KeyguardIndicationController.this.mPowerPluggedIn = status.isPluggedIn() && isChargingOrFull;
                boolean unused2 = KeyguardIndicationController.this.mPowerCharged = status.isCharged();
                int unused3 = KeyguardIndicationController.this.mChargingWattage = status.maxChargingWattage;
                int unused4 = KeyguardIndicationController.this.mChargingSpeed = status.getChargingSpeed(KeyguardIndicationController.this.mSlowThreshold, KeyguardIndicationController.this.mFastThreshold);
                int unused5 = KeyguardIndicationController.this.mBatteryLevel = status.level;
                if (KeyguardIndicationController.this.mPowerPluggedIn && !wasPluggedIn) {
                    KeyguardIndicationController.this.clearUpArrowAnimation();
                    if (MiuiKeyguardUtils.canShowChargeCircle(KeyguardIndicationController.this.mContext)) {
                        KeyguardIndicationController.this.mTextView.switchIndication((CharSequence) KeyguardIndicationController.this.mResources.getString(R.string.keyguard_charging_info_click_to_detail));
                        KeyguardIndicationController.this.mTextView.setTextColor(KeyguardIndicationController.this.getTextColor());
                        KeyguardIndicationController.this.mHandler.sendEmptyMessageDelayed(4, 2000);
                    } else {
                        boolean unused6 = KeyguardIndicationController.this.mShouldUpdateBatteryIndication = true;
                    }
                } else if (!KeyguardIndicationController.this.mPowerPluggedIn && wasPluggedIn) {
                    boolean unused7 = KeyguardIndicationController.this.mShouldUpdateBatteryIndication = false;
                    KeyguardIndicationController.this.mHandler.removeMessages(4);
                }
                KeyguardIndicationController.this.updateIndication();
                if (KeyguardIndicationController.this.mDozing) {
                    if (!wasPluggedIn && KeyguardIndicationController.this.mPowerPluggedIn) {
                        showChargingTransientIndication();
                    } else if (wasPluggedIn && !KeyguardIndicationController.this.mPowerPluggedIn) {
                        KeyguardIndicationController.this.hideTransientIndication();
                    }
                }
            }
        }

        private void showChargingTransientIndication() {
            new AsyncTask<Void, Void, String>() {
                /* access modifiers changed from: protected */
                public String doInBackground(Void... params) {
                    return ChargeUtils.getChargingHintText(KeyguardIndicationController.this.mContext, KeyguardIndicationController.this.mPowerPluggedIn, KeyguardIndicationController.this.mBatteryLevel);
                }

                /* access modifiers changed from: protected */
                public void onPostExecute(String result) {
                    super.onPostExecute(result);
                    KeyguardIndicationController.this.showTransientIndication(result);
                    KeyguardIndicationController.this.hideTransientIndicationDelayed(5000);
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
        }

        public void onKeyguardVisibilityChanged(boolean showing) {
            if (showing) {
                KeyguardIndicationController.this.updateDisclosure();
            } else {
                KeyguardIndicationController.this.clearUpArrowAnimation();
            }
        }

        public void onKeyguardBouncerChanged(boolean bouncer) {
            boolean unused = KeyguardIndicationController.this.mBouncerShowing = bouncer;
            if (bouncer) {
                KeyguardIndicationController.this.clearUpArrowAnimation();
                if (MiuiKeyguardUtils.isSupportLiftingCamera(KeyguardIndicationController.this.mContext) && KeyguardIndicationController.this.mUpdateMonitor.shouldListenForFaceUnlock() && !KeyguardIndicationController.this.mUpdateMonitor.isFaceUnlockStarted() && KeyguardIndicationController.this.mUpdateMonitor.isPasswordStatusEnableFaceUnlock()) {
                    handleFaceUnlockBouncerMessage(KeyguardIndicationController.this.mContext.getResources().getString(R.string.face_unlock_fail_retry_global));
                }
            }
        }

        public void onFaceHelp(int helpStringId) {
            if (KeyguardIndicationController.this.mUpdateMonitor.isUnlockingWithFingerprintAllowed()) {
                handleFaceUnlockBouncerMessage(KeyguardIndicationController.this.mContext.getResources().getString(helpStringId));
                showFaceTransientIndication(helpStringId);
            }
        }

        public void onFaceAuthFailed(boolean hasFace) {
            super.onFaceAuthFailed(hasFace);
            boolean unused = KeyguardIndicationController.this.mFaceUnlockHasFailed = true;
            boolean unused2 = KeyguardIndicationController.this.mFaceUnlockHasDetectFace = hasFace;
            if (!MiuiKeyguardUtils.isSupportLiftingCamera(KeyguardIndicationController.this.mContext)) {
                handleFaceUnlockBouncerMessage(hasFace ? KeyguardIndicationController.this.mContext.getResources().getString(R.string.face_unlock_fail_retry) : " ");
            }
        }

        public void onFaceStop() {
            if (!KeyguardIndicationController.this.mFaceUnlockHasFailed || !KeyguardIndicationController.this.mFaceUnlockHasDetectFace) {
                handleFaceUnlockBouncerMessage(" ");
            }
            if (MiuiKeyguardUtils.isSupportLiftingCamera(KeyguardIndicationController.this.mContext) && KeyguardIndicationController.this.mUpdateMonitor.shouldListenForFaceUnlock() && !KeyguardIndicationController.this.mUpdateMonitor.isFaceUnlockStarted()) {
                handleFaceUnlockBouncerMessage(KeyguardIndicationController.this.mContext.getResources().getString(R.string.face_unlock_fail_retry_global));
            }
        }

        public void onFaceLocked() {
            super.onFaceLocked();
            handleFaceUnlockBouncerMessage(KeyguardIndicationController.this.mContext.getResources().getString(R.string.face_unlock_fail));
        }

        private void handleFaceUnlockBouncerMessage(String message) {
            String title;
            boolean unlockWithFingerprint = KeyguardIndicationController.this.mUpdateMonitor.isUnlockWithFingerprintPossible(KeyguardUpdateMonitor.getCurrentUser()) && KeyguardIndicationController.this.mUpdateMonitor.shouldListenForFingerprint() && !KeyguardIndicationController.this.mUpdateMonitor.isFingerprintTemporarilyLockout();
            if (MiuiKeyguardUtils.isGxzwSensor() || !unlockWithFingerprint) {
                title = KeyguardIndicationController.this.mResources.getString(R.string.input_password_hint_text);
            } else {
                title = KeyguardIndicationController.this.mResources.getString(R.string.face_unlock_passwork_and_fingerprint);
            }
            KeyguardIndicationController.this.mStatusBarKeyguardViewManager.showBouncerMessage(title, message, KeyguardIndicationController.this.mResources.getColor(R.color.secure_keyguard_bouncer_message_content_text_color));
        }

        public void onFaceStart() {
            boolean unused = KeyguardIndicationController.this.mFaceUnlockHasFailed = false;
            boolean unused2 = KeyguardIndicationController.this.mFaceUnlockHasDetectFace = false;
            handleFaceUnlockBouncerMessage(" ");
        }

        public void onFaceAuthenticated() {
            showFaceTransientIndication(R.string.face_unlock_success);
        }

        private void showFaceTransientIndication(int stringId) {
            if (!KeyguardIndicationController.this.shouldShowFaceTransientIndication()) {
                return;
            }
            if (KeyguardIndicationController.this.mUpdateMonitor.isDeviceInteractive() || (KeyguardIndicationController.this.mDozing && KeyguardIndicationController.this.mUpdateMonitor.isScreenOn())) {
                KeyguardIndicationController.this.showTransientIndication(KeyguardIndicationController.this.mContext.getResources().getString(stringId), KeyguardIndicationController.this.getTextColor());
                KeyguardIndicationController.this.mHandler.removeMessages(2);
                KeyguardIndicationController.this.mHandler.sendMessageDelayed(KeyguardIndicationController.this.mHandler.obtainMessage(2), 1300);
            }
        }

        public void onFingerprintHelp(int msgId, String helpString) {
            if (KeyguardIndicationController.this.mUpdateMonitor.isUnlockingWithFingerprintAllowed()) {
                int errorColor = KeyguardIndicationController.this.getTextColor();
                if (KeyguardIndicationController.this.mStatusBarKeyguardViewManager.isBouncerShowing()) {
                    if (!TextUtils.isEmpty(helpString) && !MiuiKeyguardUtils.isGxzwSensor()) {
                        KeyguardIndicationController.this.mStatusBarKeyguardViewManager.showBouncerMessage(helpString, KeyguardIndicationController.this.mResources.getColor(R.color.secure_keyguard_bouncer_message_content_text_color));
                    }
                } else if (KeyguardIndicationController.this.mUpdateMonitor.isDeviceInteractive() || (KeyguardIndicationController.this.mDozing && KeyguardIndicationController.this.mUpdateMonitor.isScreenOn())) {
                    KeyguardIndicationController.this.showTransientIndication(helpString, errorColor);
                    KeyguardIndicationController.this.mHandler.removeMessages(2);
                    KeyguardIndicationController.this.mHandler.sendMessageDelayed(KeyguardIndicationController.this.mHandler.obtainMessage(2), 1300);
                }
            }
        }

        public void onFingerprintError(int msgId, String errString) {
            if (KeyguardIndicationController.this.mUpdateMonitor.isUnlockingWithFingerprintAllowed() && msgId != 5) {
                this.mFingerprintErrorMsgId = msgId;
                this.mFpiState = MiuiKeyguardFingerprintUtils.FingerprintIdentificationState.ERROR;
                handleFingerprintStateChanged();
            }
        }

        public void onStartedWakingUp() {
            if (KeyguardIndicationController.this.mMessageToShowOnScreenOn != null) {
                KeyguardIndicationController.this.showTransientIndication(KeyguardIndicationController.this.mMessageToShowOnScreenOn, KeyguardIndicationController.this.getTextColor());
                KeyguardIndicationController.this.hideTransientIndicationDelayed(5000);
                String unused = KeyguardIndicationController.this.mMessageToShowOnScreenOn = null;
            }
            String unused2 = KeyguardIndicationController.this.mUpArrowIndication = KeyguardIndicationController.this.mResources.getString(R.string.default_lockscreen_unlock_hint_text);
            KeyguardIndicationController.this.updateIndication();
            if (KeyguardIndicationController.this.mVisible && TextUtils.isEmpty(KeyguardIndicationController.this.mTransientIndication) && !KeyguardIndicationController.this.mPowerPluggedIn) {
                if (Build.IS_CM_CUSTOMIZATION_TEST) {
                    KeyguardIndicationController.this.updateIndication();
                } else {
                    handleEnterArrowAnimation();
                }
            }
        }

        private void handleEnterArrowAnimation() {
            TranslateAnimation translateAnimation = new TranslateAnimation(1, 0.0f, 1, 0.0f, 1, 2.0f, 1, 0.0f);
            Animation fadeIn = AnimationUtils.loadAnimation(KeyguardIndicationController.this.mContext, 17432576);
            AnimationSet upArrowEnterAnimationSet = new AnimationSet(true);
            upArrowEnterAnimationSet.addAnimation(translateAnimation);
            upArrowEnterAnimationSet.addAnimation(fadeIn);
            upArrowEnterAnimationSet.setDuration(500);
            upArrowEnterAnimationSet.setStartOffset(30);
            KeyguardIndicationController.this.mUpArrow.setVisibility(0);
            KeyguardIndicationController.this.mUpArrow.startAnimation(upArrowEnterAnimationSet);
            KeyguardIndicationController.this.mHandler.sendEmptyMessageDelayed(5, 100);
        }

        public void onStartedGoingToSleep(int why) {
            super.onStartedGoingToSleep(why);
            KeyguardIndicationController.this.clearUpArrowAnimation();
            if (KeyguardIndicationController.this.mUpArrowIndication != null) {
                String unused = KeyguardIndicationController.this.mUpArrowIndication = null;
                KeyguardIndicationController.this.updateIndication();
            }
        }

        public void onFingerprintRunningStateChanged(boolean running) {
            if (running) {
                String unused = KeyguardIndicationController.this.mMessageToShowOnScreenOn = null;
            }
        }

        public void onFingerprintAuthenticated(int userId) {
            super.onFingerprintAuthenticated(userId);
            this.mFpiState = MiuiKeyguardFingerprintUtils.FingerprintIdentificationState.SUCCEEDED;
            this.mFingerprintAuthUserId = userId;
            handleFingerprintStateChanged();
        }

        public void onFingerprintAuthFailed() {
            super.onFingerprintAuthFailed();
            this.mFpiState = MiuiKeyguardFingerprintUtils.FingerprintIdentificationState.FAILED;
            handleFingerprintStateChanged();
        }

        public void onFingerprintLockoutReset() {
            super.onFingerprintLockoutReset();
            this.mFpiState = MiuiKeyguardFingerprintUtils.FingerprintIdentificationState.RESET;
            handleFingerprintStateChanged();
        }

        private void handleFingerprintStateChanged() {
            if (!KeyguardIndicationController.this.mUpdateMonitor.isUnlockingWithFingerprintAllowed(KeyguardUpdateMonitor.getCurrentUser())) {
                this.mLastFpiState = this.mFpiState;
                return;
            }
            String title = "";
            String message = "";
            if (this.mFpiState == MiuiKeyguardFingerprintUtils.FingerprintIdentificationState.FAILED) {
                title = KeyguardIndicationController.this.mResources.getString(R.string.fingerprint_try_again_text);
                message = KeyguardIndicationController.this.mResources.getString(R.string.fingerprint_try_again_msg);
            } else if (this.mFpiState == MiuiKeyguardFingerprintUtils.FingerprintIdentificationState.ERROR) {
                title = KeyguardIndicationController.this.mResources.getString(R.string.fingerprint_not_identified_title);
                message = KeyguardIndicationController.this.mResources.getString(R.string.fingerprint_not_identified_msg);
                if (this.mFingerprintErrorMsgId == 9) {
                    message = KeyguardIndicationController.this.mResources.getString(R.string.fingerprint_not_identified_msg_lock);
                }
            } else if (this.mFpiState == MiuiKeyguardFingerprintUtils.FingerprintIdentificationState.SUCCEEDED) {
                if (this.mFingerprintAuthUserId != KeyguardUpdateMonitor.getCurrentUser()) {
                    if (MiuiKeyguardUtils.isGreenKidActive(KeyguardIndicationController.this.mContext)) {
                        title = KeyguardIndicationController.this.mResources.getString(R.string.input_password_after_boot_msg_can_not_switch_when_greenkid_active);
                    } else if (PhoneUtils.isInCall(KeyguardIndicationController.this.mContext)) {
                        title = KeyguardIndicationController.this.mResources.getString(R.string.input_password_after_boot_msg_can_not_switch_when_calling);
                    } else if (!KeyguardIndicationController.this.mUpdateMonitor.getStrongAuthTracker().hasUserAuthenticatedSinceBoot(this.mFingerprintAuthUserId)) {
                        title = KeyguardIndicationController.this.mResources.getString(R.string.fingerprint_enter_second_psw_title);
                        message = KeyguardIndicationController.this.mResources.getString(R.string.fingerprint_enter_second_psw_msg);
                    }
                }
            } else if (this.mLastFpiState == MiuiKeyguardFingerprintUtils.FingerprintIdentificationState.ERROR && this.mFpiState == MiuiKeyguardFingerprintUtils.FingerprintIdentificationState.RESET) {
                title = KeyguardIndicationController.this.mResources.getString(R.string.fingerprint_not_identified_title);
                message = KeyguardIndicationController.this.mResources.getString(R.string.fingerprint_again_identified_msg);
            }
            int errorColor = KeyguardIndicationController.this.getTextColor();
            KeyguardIndicationController.this.mStatusBarKeyguardViewManager.showBouncerMessage(title, message, KeyguardIndicationController.this.mResources.getColor(R.color.secure_keyguard_bouncer_message_content_text_color));
            if (this.mFpiState == MiuiKeyguardFingerprintUtils.FingerprintIdentificationState.ERROR && this.mLastFpiState != MiuiKeyguardFingerprintUtils.FingerprintIdentificationState.ERROR && KeyguardIndicationController.this.mStatusBarKeyguardViewManager.isBouncerShowing()) {
                KeyguardIndicationController.this.mStatusBarKeyguardViewManager.applyHintAnimation(500);
            }
            if (this.mFpiState == MiuiKeyguardFingerprintUtils.FingerprintIdentificationState.FAILED && MiuiKeyguardUtils.isGxzwSensor() && KeyguardIndicationController.this.mStatusBarKeyguardViewManager.isBouncerShowing()) {
                KeyguardIndicationController.this.mStatusBarKeyguardViewManager.applyHintAnimation(500);
            }
            if (this.mFpiState == MiuiKeyguardFingerprintUtils.FingerprintIdentificationState.FAILED && KeyguardIndicationController.this.mUpdateMonitor.isDeviceInteractive()) {
                KeyguardIndicationController.this.mHandler.removeMessages(1);
                KeyguardIndicationController.this.showTransientIndication(KeyguardIndicationController.this.mContext.getString(R.string.fingerprint_try_again_text), errorColor);
                KeyguardIndicationController.this.hideTransientIndicationDelayed(5000);
            }
            String unused = KeyguardIndicationController.this.mMessageToShowOnScreenOn = title;
            this.mLastFpiState = this.mFpiState;
        }

        public void onUserUnlocked() {
            if (KeyguardIndicationController.this.mVisible) {
                KeyguardIndicationController.this.updateIndication();
            }
        }

        public void onBottomAreaButtonClicked(boolean isClickAnimating) {
            KeyguardIndicationController.this.handleBottomButtonClicked(isClickAnimating);
        }

        public void onPhoneSignalChanged(boolean isSignalAvailable) {
            boolean unused = KeyguardIndicationController.this.mSignalAvailable = isSignalAvailable;
            KeyguardIndicationController.this.updateIndication();
        }

        public void onLockScreenMagazinePreViewVisibilityChanged(boolean visible) {
            if (!visible) {
                if (KeyguardIndicationController.this.mPowerPluggedIn) {
                    boolean unused = KeyguardIndicationController.this.mShouldUpdateBatteryIndication = true;
                    KeyguardIndicationController.this.updateChargingInfoIndication();
                }
                KeyguardIndicationController.this.hideTransientIndication();
            }
        }
    }

    public KeyguardIndicationController(Context context, NotificationPanelView notificationPanelView) {
        this(context, WakeLockHelper.createPartial(context, "Doze:KeyguardIndication"), notificationPanelView);
        registerCallbacks(this.mUpdateMonitor);
    }

    @VisibleForTesting
    KeyguardIndicationController(Context context, WakeLock wakeLock, NotificationPanelView notificationPanelView) {
        this.mTextViewOnClickListener = new View.OnClickListener() {
            public void onClick(View v) {
                if (Build.IS_CM_CUSTOMIZATION_TEST && KeyguardIndicationController.this.mSignalAvailable && !KeyguardIndicationController.this.mIsCMSingleClicking) {
                    KeyguardIndicationController.this.takeEmergencyCallAction();
                }
            }
        };
        this.mTickReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                KeyguardIndicationController.this.mHandler.sendEmptyMessage(3);
            }
        };
        this.mHandler = new Handler() {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        KeyguardIndicationController.this.hideTransientIndication();
                        return;
                    case 2:
                        KeyguardIndicationController.this.hideTransientIndication();
                        return;
                    case 3:
                        KeyguardIndicationController.this.handleTickReceived();
                        return;
                    case 4:
                        KeyguardIndicationController.this.hideChargingClickToDetailInfo();
                        return;
                    case 5:
                        KeyguardIndicationController.this.handleExitArrowAndTextAnimation();
                        return;
                    case 6:
                        KeyguardIndicationController.this.updateChargingInfoIndication();
                        return;
                    case 7:
                        KeyguardIndicationController.this.handleShowCMEmergency();
                        return;
                    default:
                        return;
                }
            }
        };
        this.mContext = context;
        this.mResources = this.mContext.getResources();
        this.mWakeLock = new SettableWakeLock(wakeLock);
        this.mNotificationPanelView = notificationPanelView;
        this.mKeyguardBottomAreaView = (KeyguardBottomAreaView) notificationPanelView.findViewById(R.id.keyguard_bottom_area);
        this.mIndicationArea = (LinearLayout) this.mKeyguardBottomAreaView.findViewById(R.id.keyguard_indication_area);
        this.mTextView = (KeyguardIndicationTextView) this.mKeyguardBottomAreaView.findViewById(R.id.keyguard_indication_text);
        this.mTextView.setOnClickListener(this.mTextViewOnClickListener);
        this.mDisclosure = (KeyguardIndicationTextView) this.mKeyguardBottomAreaView.findViewById(R.id.keyguard_indication_enterprise_disclosure);
        this.mUpArrow = (ImageView) this.mKeyguardBottomAreaView.findViewById(R.id.keyguard_up_arrow);
        this.mSlowThreshold = this.mResources.getInteger(R.integer.config_chargingSlowlyThreshold);
        this.mFastThreshold = this.mResources.getInteger(R.integer.config_chargingFastThreshold);
        this.mUserManager = (UserManager) context.getSystemService(UserManager.class);
        this.mBatteryInfo = IBatteryStats.Stub.asInterface(ServiceManager.getService("batterystats"));
        this.mDevicePolicyManager = (DevicePolicyManager) context.getSystemService("device_policy");
        this.mUpdateMonitor = KeyguardUpdateMonitor.getInstance(context);
        this.mStrongAuthTracker = this.mUpdateMonitor.getStrongAuthTracker();
        this.mViewConfiguration = ViewConfiguration.get(context);
        updateDisclosure();
    }

    /* access modifiers changed from: private */
    public void takeEmergencyCallAction() {
        if (PhoneUtils.isInCall(this.mContext)) {
            PhoneUtils.resumeCall(this.mContext);
            return;
        }
        KeyguardUpdateMonitor.getInstance(this.mContext).reportEmergencyCallAction(true);
        this.mContext.startActivityAsUser(INTENT_EMERGENCY_DIAL, ActivityOptions.makeCustomAnimation(this.mContext, 0, 0).toBundle(), new UserHandle(KeyguardUpdateMonitor.getCurrentUser()));
    }

    private void registerCallbacks(KeyguardUpdateMonitor monitor) {
        monitor.registerCallback(getKeyguardCallback());
        this.mContext.registerReceiverAsUser(this.mTickReceiver, UserHandleCompat.SYSTEM, new IntentFilter("android.intent.action.TIME_TICK"), null, (Handler) Dependency.get(Dependency.TIME_TICK_HANDLER));
    }

    /* access modifiers changed from: protected */
    public KeyguardUpdateMonitorCallback getKeyguardCallback() {
        if (this.mUpdateMonitorCallback == null) {
            this.mUpdateMonitorCallback = new BaseKeyguardCallback();
        }
        return this.mUpdateMonitorCallback;
    }

    /* access modifiers changed from: private */
    public void updateDisclosure() {
        if (this.mDevicePolicyManager != null) {
            if (this.mDozing || !DevicePolicyManagerCompat.isDeviceManaged(this.mDevicePolicyManager)) {
                this.mDisclosure.setVisibility(8);
            } else {
                CharSequence organizationName = DevicePolicyManagerCompat.getDeviceOwnerOrganizationName(this.mDevicePolicyManager);
                if (organizationName != null) {
                    this.mDisclosure.switchIndication((CharSequence) this.mResources.getString(R.string.do_disclosure_with_name, new Object[]{organizationName}));
                } else {
                    this.mDisclosure.switchIndication((int) R.string.do_disclosure_generic);
                }
                this.mDisclosure.setVisibility(0);
            }
        }
    }

    public void setVisible(boolean visible) {
        this.mVisible = visible;
        this.mIndicationArea.setVisibility(visible ? 0 : 8);
        if (visible) {
            hideTransientIndication();
            updateIndication();
        }
    }

    public void setUserInfoController(UserInfoController userInfoController) {
    }

    public void hideTransientIndicationDelayed(long delayMs) {
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(1), delayMs);
    }

    public void showTransientIndication(int transientIndication) {
        showTransientIndication(this.mResources.getString(transientIndication));
    }

    /* access modifiers changed from: private */
    public void showTransientIndication(String transientIndication) {
        showTransientIndication(transientIndication, getTextColor());
    }

    /* access modifiers changed from: private */
    public void showTransientIndication(String transientIndication, int textColor) {
        this.mTransientIndication = transientIndication;
        this.mTransientTextColor = textColor;
        this.mHandler.removeMessages(1);
        if (this.mDozing && !TextUtils.isEmpty(this.mTransientIndication)) {
            this.mWakeLock.setAcquired(true);
            hideTransientIndicationDelayed(5000);
        }
        updateIndication();
    }

    public void hideTransientIndication() {
        if (this.mTransientIndication != null) {
            this.mTransientIndication = null;
            this.mHandler.removeMessages(1);
            updateIndication();
        }
    }

    /* access modifiers changed from: private */
    public void updateIndication() {
        int i;
        if (TextUtils.isEmpty(this.mTransientIndication)) {
            this.mWakeLock.setAcquired(false);
        }
        Log.i("KeyguardIndication", "updateIndication: mVisible " + this.mVisible + " mDozing " + this.mDozing + " mTransientIndication " + this.mTransientIndication + " mPowerPluggedIn " + this.mPowerPluggedIn + " mShouldUpdateBatteryIndication " + this.mShouldUpdateBatteryIndication + " mUpArrowIndication " + this.mUpArrowIndication);
        if (this.mVisible) {
            if (this.mDozing) {
                if (!TextUtils.isEmpty(this.mTransientIndication)) {
                    this.mTextView.switchIndication((CharSequence) this.mTransientIndication);
                    this.mTextView.setTextColor(this.mTransientTextColor);
                } else {
                    this.mTextView.switchIndication((CharSequence) null);
                }
            } else if (!TextUtils.isEmpty(this.mTransientIndication)) {
                this.mTextView.switchIndication((CharSequence) this.mTransientIndication);
                this.mTextView.setTextColor(this.mTransientTextColor);
            } else if (this.mPowerPluggedIn) {
                if (this.mShouldUpdateBatteryIndication) {
                    this.mHandler.removeMessages(6);
                    this.mHandler.sendEmptyMessageDelayed(6, 500);
                }
            } else if (Build.IS_CM_CUSTOMIZATION_TEST && this.mSignalAvailable && !this.mIsCMSingleClicking) {
                this.mTextView.switchIndication((int) R.string.emergency_call_string);
                this.mTextView.setTextColor(getTextColor());
            } else if (this.mUpdateMonitor.isFaceUnlock() && this.mUpdateMonitor.isStayScreenFaceUnlockSuccess() && shouldShowFaceTransientIndication()) {
                this.mTextView.switchIndication((int) R.string.face_unlock_success);
                this.mTextView.setTextColor(getTextColor());
            } else if (!TextUtils.isEmpty(this.mUpArrowIndication)) {
                this.mTextView.switchIndication((CharSequence) this.mUpArrowIndication);
                this.mTextView.setTextColor(getTextColor());
                ImageView imageView = this.mUpArrow;
                if (this.mDarkMode) {
                    i = R.drawable.miui_default_lock_screen_up_arrow_dark;
                } else {
                    i = R.drawable.miui_default_lock_screen_up_arrow;
                }
                imageView.setImageResource(i);
            } else {
                this.mTextView.switchIndication((CharSequence) this.mRestingIndication);
                this.mTextView.setTextColor(getTextColor());
            }
        }
    }

    /* access modifiers changed from: private */
    public void updateChargingInfoIndication() {
        if (this.mChargeAsyncTask == null) {
            this.mChargeAsyncTask = new AsyncTask<Void, Void, String>() {
                /* access modifiers changed from: protected */
                public String doInBackground(Void... params) {
                    return ChargeUtils.getChargingHintText(KeyguardIndicationController.this.mContext, KeyguardIndicationController.this.mPowerPluggedIn, KeyguardIndicationController.this.mBatteryLevel);
                }

                /* access modifiers changed from: protected */
                public void onPostExecute(String result) {
                    super.onPostExecute(result);
                    KeyguardIndicationController.this.mTextView.switchIndication((CharSequence) result);
                    KeyguardIndicationController.this.mTextView.setTextColor(KeyguardIndicationController.this.getTextColor());
                    AsyncTask unused = KeyguardIndicationController.this.mChargeAsyncTask = null;
                }

                /* access modifiers changed from: protected */
                public void onCancelled() {
                    super.onCancelled();
                    AsyncTask unused = KeyguardIndicationController.this.mChargeAsyncTask = null;
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
        }
    }

    public void setStatusBarKeyguardViewManager(StatusBarKeyguardViewManager statusBarKeyguardViewManager) {
        this.mStatusBarKeyguardViewManager = statusBarKeyguardViewManager;
    }

    public void onTouchEvent(MotionEvent event, int statusBarState, float initialTouchX, float initialTouchY) {
        if (event.getAction() == 0) {
            clearUpArrowAnimation();
        } else if (event.getAction() == 1 && statusBarState == 1 && this.mNotificationPanelView.isQSFullyCollapsed()) {
            int touchSlop = this.mViewConfiguration.getScaledTouchSlop();
            if (Math.abs(initialTouchX - event.getRawX()) < ((float) touchSlop) && Math.abs(initialTouchY - event.getRawY()) < ((float) touchSlop)) {
                handleSingleClickEvent();
            }
        }
    }

    private void handleSingleClickEvent() {
        if (Build.IS_CM_CUSTOMIZATION_TEST && !this.mPowerPluggedIn) {
            this.mIsCMSingleClicking = true;
            updateIndication();
            this.mHandler.removeMessages(7);
            this.mHandler.sendEmptyMessageDelayed(7, 2000);
        }
    }

    /* access modifiers changed from: private */
    public void handleShowCMEmergency() {
        this.mIsCMSingleClicking = false;
        updateIndication();
    }

    /* access modifiers changed from: private */
    public void clearUpArrowAnimation() {
        this.mHandler.removeMessages(5);
        this.mUpArrow.clearAnimation();
        this.mTextView.clearAnimation();
        this.mUpArrow.setVisibility(4);
        updateIndication();
    }

    /* access modifiers changed from: private */
    public void handleTickReceived() {
        if (this.mVisible) {
            updateIndication();
        }
    }

    /* access modifiers changed from: private */
    public void hideChargingClickToDetailInfo() {
        this.mShouldUpdateBatteryIndication = true;
        updateIndication();
    }

    /* access modifiers changed from: private */
    public void handleExitArrowAndTextAnimation() {
        Animation upArrowFadeOut = AnimationUtils.loadAnimation(this.mContext, 17432577);
        Animation textFadeIn = AnimationUtils.loadAnimation(this.mContext, 17432576);
        TranslateAnimation translateAnimation = new TranslateAnimation(1, 0.0f, 1, 0.0f, 1, 0.0f, 1, -2.0f);
        TranslateAnimation translateAnimation2 = new TranslateAnimation(1, 0.0f, 1, 0.0f, 1, 2.0f, 1, 0.0f);
        AnimationSet upArrowExitAnimationSet = new AnimationSet(true);
        AnimationSet textEnterAnimationSet = new AnimationSet(true);
        upArrowExitAnimationSet.addAnimation(upArrowFadeOut);
        upArrowExitAnimationSet.addAnimation(translateAnimation);
        upArrowExitAnimationSet.setDuration((long) 500);
        textEnterAnimationSet.addAnimation(textFadeIn);
        textEnterAnimationSet.addAnimation(translateAnimation2);
        textEnterAnimationSet.setDuration((long) 500);
        textEnterAnimationSet.setStartOffset(100);
        upArrowExitAnimationSet.setAnimationListener(new Animation.AnimationListener() {
            public void onAnimationStart(Animation animation) {
            }

            public void onAnimationEnd(Animation animation) {
                KeyguardIndicationController.this.mUpArrow.setVisibility(4);
            }

            public void onAnimationRepeat(Animation animation) {
            }
        });
        textEnterAnimationSet.setAnimationListener(new Animation.AnimationListener() {
            public void onAnimationStart(Animation animation) {
                KeyguardIndicationController.this.updateIndication();
            }

            public void onAnimationEnd(Animation animation) {
                KeyguardIndicationController.this.updateIndication();
                KeyguardIndicationController.this.mTextView.setVisibility(0);
            }

            public void onAnimationRepeat(Animation animation) {
            }
        });
        this.mUpArrow.startAnimation(upArrowExitAnimationSet);
        this.mTextView.startAnimation(textEnterAnimationSet);
    }

    public void setDozing(boolean dozing) {
        if (this.mDozing != dozing) {
            this.mDozing = dozing;
            updateIndication();
            updateDisclosure();
        }
    }

    /* access modifiers changed from: private */
    public boolean shouldShowFaceTransientIndication() {
        if (this.mBouncerShowing || !MiuiKeyguardUtils.isIndianRegion(this.mContext) || !WallpaperAuthorityUtils.isLockScreenMagazineOpenedWallpaper() || !this.mUpdateMonitor.isSupportLockScreenMagazineLeft()) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: private */
    public void handleBottomButtonClicked(boolean isClickAnimating) {
        this.mTextView.setBottomAreaButtonClicked(isClickAnimating);
        startBottomButtonClickAnim(isClickAnimating);
    }

    private void startBottomButtonClickAnim(boolean isClickAnimating) {
        if (this.mBottomButtonClickAnimator != null && this.mBottomButtonClickAnimator.isRunning()) {
            if (isClickAnimating) {
                this.mBottomButtonClickAnimator.cancel();
            } else {
                return;
            }
        }
        if (isClickAnimating) {
            this.mBottomButtonClickAnimator = ObjectAnimator.ofFloat(this.mTextView, View.ALPHA, new float[]{1.0f, 0.0f});
            this.mBottomButtonClickAnimator.setInterpolator(Ease.Quint.easeOut);
            this.mBottomButtonClickAnimator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    KeyguardIndicationController.this.mTextView.setVisibility(8);
                }
            });
        } else {
            this.mBottomButtonClickAnimator = ObjectAnimator.ofFloat(this.mTextView, View.ALPHA, new float[]{0.0f, 1.0f});
            this.mBottomButtonClickAnimator.setInterpolator(Ease.Cubic.easeInOut);
            this.mBottomButtonClickAnimator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationStart(Animator animation) {
                    KeyguardIndicationController.this.mTextView.setVisibility(0);
                }

                public void onAnimationCancel(Animator animation) {
                    KeyguardIndicationController.this.mTextView.setVisibility(0);
                    KeyguardIndicationController.this.mTextView.setAlpha(1.0f);
                }
            });
        }
        this.mBottomButtonClickAnimator.setDuration(200);
        this.mBottomButtonClickAnimator.start();
    }

    public void setDarkMode(boolean dark) {
        if (this.mDarkMode != dark) {
            this.mDarkMode = dark;
            updateIndication();
        }
    }

    public int getTextColor() {
        int i;
        Resources resources = this.mResources;
        if (this.mDarkMode) {
            i = R.color.miui_common_unlock_screen_common_dark_text_color;
        } else {
            i = R.color.miui_default_lock_screen_unlock_hint_text_color;
        }
        return resources.getColor(i);
    }
}
