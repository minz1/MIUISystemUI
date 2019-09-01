package com.android.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.UserManagerCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.EmergencyButton;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.keyguard.MiuiKeyguardUtils;
import com.android.keyguard.analytics.AnalyticsHelper;
import com.android.keyguard.charge.ChargeUtils;
import com.android.keyguard.charge.MiuiChargeController;
import com.android.keyguard.charge.MiuiKeyguardChargingContainer;
import com.android.keyguard.charge.MiuiKeyguardChargingView;
import com.android.keyguard.utils.PackageUtils;
import com.android.systemui.Dependency;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.assist.AssistManager;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.IntentButtonProvider;
import com.android.systemui.statusbar.KeyguardAffordanceView;
import com.android.systemui.statusbar.KeyguardIndicationController;
import com.android.systemui.statusbar.LockScreenMagazineController;
import com.android.systemui.statusbar.phone.UnlockMethodCache;
import com.android.systemui.statusbar.policy.AccessibilityController;
import com.android.systemui.statusbar.policy.ExtensionController;
import com.android.systemui.statusbar.policy.FlashlightController;
import com.android.systemui.statusbar.policy.PreviewInflater;
import com.android.systemui.util.function.Consumer;
import com.android.systemui.util.function.Supplier;
import miui.maml.animation.interpolater.SineEaseInOutInterpolater;
import miui.util.FeatureParser;

public class KeyguardBottomAreaView extends FrameLayout implements View.OnClickListener, View.OnLongClickListener, UnlockMethodCache.OnUnlockMethodChangedListener, AccessibilityController.AccessibilityStateChangedCallback {
    public static final Intent INSECURE_CAMERA_INTENT = new Intent("android.media.action.STILL_IMAGE_CAMERA");
    private static final Intent PHONE_INTENT = new Intent("android.intent.action.DIAL");
    private static final Intent SECURE_CAMERA_INTENT = new Intent("android.media.action.STILL_IMAGE_CAMERA_SECURE").addFlags(8388608);
    private AccessibilityController mAccessibilityController;
    private View.AccessibilityDelegate mAccessibilityDelegate;
    private ActivityStarter mActivityStarter;
    private AssistManager mAssistManager;
    /* access modifiers changed from: private */
    public int mBatteryLevel;
    /* access modifiers changed from: private */
    public AsyncTask<?, ?, ?> mChargeAsyncTask;
    private View.OnClickListener mChargingBackArrowClickListener;
    private View.OnTouchListener mChargingBackArrowTouchListener;
    private Runnable mChargingEnterAnimRunnable;
    /* access modifiers changed from: private */
    public String mChargingHintText;
    /* access modifiers changed from: private */
    public ImageView mChargingInfoBackArrow;
    /* access modifiers changed from: private */
    public MiuiKeyguardChargingContainer mChargingInfoContainer;
    private LinearLayout mChargingListAndBackArrow;
    /* access modifiers changed from: private */
    public MiuiKeyguardChargingView mChargingView;
    private View.OnClickListener mChargingViewClickListener;
    /* access modifiers changed from: private */
    public boolean mDarkMode;
    private int mDensityDpi;
    private final BroadcastReceiver mDevicePolicyReceiver;
    private boolean mDozing;
    private EmergencyButton mEmergencyButton;
    private TextView mEnterpriseDisclosure;
    private FlashlightController mFlashlightController;
    private float mFontScale;
    private ViewGroup mIndicationArea;
    private KeyguardIndicationController mIndicationController;
    /* access modifiers changed from: private */
    public KeyguardUpdateMonitor mKeyguardUpdateMonitor;
    private String mLanguage;
    /* access modifiers changed from: private */
    public KeyguardAffordanceView mLeftAffordanceView;
    private LinearLayout mLeftAffordanceViewLayout;
    private TextView mLeftAffordanceViewTips;
    private IntentButtonProvider.IntentButton mLeftButton;
    AnimatorSet mLeftButtonLayoutAnimatorSet;
    private ExtensionController.Extension<IntentButtonProvider.IntentButton> mLeftExtension;
    private boolean mLeftIsVoiceAssist;
    private LockPatternUtils mLockPatternUtils;
    /* access modifiers changed from: private */
    public LockScreenMagazineController mLockScreenMagazineController;
    private LockscreenGestureLogger mLockscreenGestureLogger;
    /* access modifiers changed from: private */
    public boolean mNeedRepositionDevice;
    /* access modifiers changed from: private */
    public NotificationPanelView mNotificationPanelView;
    /* access modifiers changed from: private */
    public boolean mPluggedIn;
    private PowerManager mPowerManager;
    private PreviewInflater mPreviewInflater;
    private final ServiceConnection mPrewarmConnection;
    /* access modifiers changed from: private */
    public Messenger mPrewarmMessenger;
    private Runnable mRefreshChargingInfoRunnable;
    /* access modifiers changed from: private */
    public KeyguardAffordanceView mRightAffordanceView;
    private LinearLayout mRightAffordanceViewLayout;
    private TextView mRightAffordanceViewTips;
    private IntentButtonProvider.IntentButton mRightButton;
    AnimatorSet mRightButtonLayoutAnimatorSet;
    private ExtensionController.Extension<IntentButtonProvider.IntentButton> mRightExtension;
    private StatusBar mStatusBar;
    /* access modifiers changed from: private */
    public int mTemperature;
    private UnlockMethodCache mUnlockMethodCache;
    private final KeyguardUpdateMonitorCallback mUpdateMonitorCallback;
    /* access modifiers changed from: private */
    public boolean mUserSetupComplete;
    /* access modifiers changed from: private */
    public boolean mUserUnlocked;
    private MiuiChargeController.WirelessChargeCallback mWirelessChargeCallback;

    private class DefaultLeftButton implements IntentButtonProvider.IntentButton {
        private IntentButtonProvider.IntentButton.IconState mIconState;

        private DefaultLeftButton() {
            this.mIconState = new IntentButtonProvider.IntentButton.IconState();
        }

        public IntentButtonProvider.IntentButton.IconState getIcon() {
            Drawable drawable;
            boolean z = true;
            if (KeyguardBottomAreaView.this.mKeyguardUpdateMonitor.isSupportLockScreenMagazineLeft()) {
                ResolveInfo resolveInfo = PackageUtils.resolveIntent(KeyguardBottomAreaView.this.mContext, KeyguardBottomAreaView.this.getLockScreenMagazineIntent());
                IntentButtonProvider.IntentButton.IconState iconState = this.mIconState;
                if (!KeyguardBottomAreaView.this.mUserSetupComplete || !KeyguardBottomAreaView.this.mUserUnlocked || !KeyguardBottomAreaView.this.mNotificationPanelView.getLeftView().isSupportRightMove() || FeatureParser.getBoolean("is_pad", false) || resolveInfo == null) {
                    z = false;
                }
                iconState.isVisible = z;
                this.mIconState.drawable = KeyguardBottomAreaView.this.getLockScreenMagazineMainEntryIcon();
                this.mIconState.contentDescription = KeyguardBottomAreaView.this.mContext.getString(R.string.accessibility_left_lock_screen_magazine_button);
            } else {
                IntentButtonProvider.IntentButton.IconState iconState2 = this.mIconState;
                if (!KeyguardBottomAreaView.this.mUserSetupComplete || !KeyguardBottomAreaView.this.mUserUnlocked || !KeyguardBottomAreaView.this.mNotificationPanelView.getLeftView().isSupportRightMove() || FeatureParser.getBoolean("is_pad", false)) {
                    z = false;
                }
                iconState2.isVisible = z;
                IntentButtonProvider.IntentButton.IconState iconState3 = this.mIconState;
                if (KeyguardBottomAreaView.this.mDarkMode) {
                    drawable = KeyguardBottomAreaView.this.mContext.getDrawable(R.drawable.keyguard_bottom_remote_center_img_dark);
                } else {
                    drawable = KeyguardBottomAreaView.this.mContext.getDrawable(R.drawable.keyguard_bottom_remote_center_img);
                }
                iconState3.drawable = drawable;
                this.mIconState.contentDescription = KeyguardBottomAreaView.this.mContext.getString(R.string.accessibility_left_control_center_button);
            }
            this.mIconState.tint = false;
            return this.mIconState;
        }

        public Intent getIntent() {
            if (KeyguardBottomAreaView.this.mKeyguardUpdateMonitor.isSupportLockScreenMagazineLeft()) {
                return KeyguardBottomAreaView.this.mLockScreenMagazineController.getPreLeftScreenIntent();
            }
            return null;
        }
    }

    private class DefaultRightButton implements IntentButtonProvider.IntentButton {
        private IntentButtonProvider.IntentButton.IconState mIconState;

        private DefaultRightButton() {
            this.mIconState = new IntentButtonProvider.IntentButton.IconState();
        }

        public IntentButtonProvider.IntentButton.IconState getIcon() {
            Drawable drawable;
            ResolveInfo resolved = KeyguardBottomAreaView.this.resolveCameraIntent();
            this.mIconState.isVisible = !KeyguardBottomAreaView.this.isCameraDisabledByDpm() && resolved != null && KeyguardBottomAreaView.this.getResources().getBoolean(R.bool.config_keyguardShowCameraAffordance) && KeyguardBottomAreaView.this.mUserSetupComplete && KeyguardBottomAreaView.this.mUserUnlocked;
            IntentButtonProvider.IntentButton.IconState iconState = this.mIconState;
            if (KeyguardBottomAreaView.this.mDarkMode) {
                drawable = KeyguardBottomAreaView.this.mContext.getDrawable(R.drawable.keyguard_bottom_camera_img_dark);
            } else {
                drawable = KeyguardBottomAreaView.this.mContext.getDrawable(R.drawable.keyguard_bottom_camera_img);
            }
            iconState.drawable = drawable;
            this.mIconState.contentDescription = KeyguardBottomAreaView.this.mContext.getString(R.string.accessibility_camera_button);
            this.mIconState.tint = false;
            return this.mIconState;
        }

        public Intent getIntent() {
            Intent intent = new Intent();
            intent.setFlags(276856832);
            intent.putExtra("ShowCameraWhenLocked", true);
            intent.putExtra("StartActivityWhenLocked", true);
            intent.setAction("android.media.action.STILL_IMAGE_CAMERA");
            intent.setComponent(new ComponentName("com.android.camera", "com.android.camera.Camera"));
            return intent;
        }
    }

    public KeyguardBottomAreaView(Context context) {
        this(context, null);
    }

    public KeyguardBottomAreaView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KeyguardBottomAreaView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public KeyguardBottomAreaView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mPrewarmConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder service) {
                Messenger unused = KeyguardBottomAreaView.this.mPrewarmMessenger = new Messenger(service);
            }

            public void onServiceDisconnected(ComponentName name) {
                Messenger unused = KeyguardBottomAreaView.this.mPrewarmMessenger = null;
            }
        };
        this.mRightButton = new DefaultRightButton();
        this.mLeftButton = new DefaultLeftButton();
        this.mLockscreenGestureLogger = new LockscreenGestureLogger();
        this.mAccessibilityDelegate = new View.AccessibilityDelegate() {
            public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                String label = null;
                if (host == KeyguardBottomAreaView.this.mRightAffordanceView) {
                    label = KeyguardBottomAreaView.this.getResources().getString(R.string.camera_label);
                } else {
                    KeyguardAffordanceView unused = KeyguardBottomAreaView.this.mLeftAffordanceView;
                }
                info.addAction(new AccessibilityNodeInfo.AccessibilityAction(16, label));
            }

            public boolean performAccessibilityAction(View host, int action, Bundle args) {
                if (action == 16) {
                    if (host == KeyguardBottomAreaView.this.mRightAffordanceView) {
                        KeyguardBottomAreaView.this.launchCamera("lockscreen_affordance");
                        return true;
                    } else if (host == KeyguardBottomAreaView.this.mLeftAffordanceView) {
                        return true;
                    }
                }
                return super.performAccessibilityAction(host, action, args);
            }
        };
        this.mNeedRepositionDevice = false;
        this.mWirelessChargeCallback = new MiuiChargeController.WirelessChargeCallback() {
            public void onNeedRepositionDevice(boolean needRepositionDevice) {
                boolean unused = KeyguardBottomAreaView.this.mNeedRepositionDevice = needRepositionDevice;
                KeyguardBottomAreaView.this.mChargingInfoContainer.setNeedRepositionDevice(KeyguardBottomAreaView.this.mNeedRepositionDevice);
                KeyguardBottomAreaView.this.mChargingView.setNeedRepositionDevice(KeyguardBottomAreaView.this.mNeedRepositionDevice);
                KeyguardBottomAreaView.this.refreshChargingInfo();
            }
        };
        this.mRightButtonLayoutAnimatorSet = new AnimatorSet();
        this.mLeftButtonLayoutAnimatorSet = new AnimatorSet();
        this.mDevicePolicyReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                KeyguardBottomAreaView.this.post(new Runnable() {
                    public void run() {
                        KeyguardBottomAreaView.this.updateCameraVisibility();
                    }
                });
            }
        };
        this.mUpdateMonitorCallback = new KeyguardUpdateMonitorCallback() {
            public void onUserSwitchComplete(int userId) {
                KeyguardBottomAreaView.this.updateCameraVisibility();
            }

            public void onStartedGoingToSleep(int why) {
                KeyguardBottomAreaView.this.chargingInfoDown();
            }

            public void onUserUnlocked() {
                KeyguardBottomAreaView.this.mNotificationPanelView.getLeftView().initLeftView();
                KeyguardBottomAreaView.this.mNotificationPanelView.getLeftView().uploadData();
                boolean unused = KeyguardBottomAreaView.this.mUserUnlocked = true;
                KeyguardBottomAreaView.this.updateCameraVisibility();
                KeyguardBottomAreaView.this.updateLeftAffordance();
            }

            public void onRegionChanged() {
                KeyguardBottomAreaView.this.mNotificationPanelView.getLeftView().initLeftView();
            }

            public void onRefreshBatteryInfo(KeyguardUpdateMonitor.BatteryStatus status) {
                boolean pluggedIn = false;
                if (!FeatureParser.getBoolean("is_pad", false)) {
                    boolean isChargingOrFull = status.status == 2 || status.status == 5;
                    if (status.isPluggedIn() && isChargingOrFull) {
                        pluggedIn = true;
                    }
                    int batteryLevel = status.level;
                    int temperature = status.temperature;
                    if (!(pluggedIn == KeyguardBottomAreaView.this.mPluggedIn && batteryLevel == KeyguardBottomAreaView.this.mBatteryLevel && temperature == KeyguardBottomAreaView.this.mTemperature)) {
                        KeyguardBottomAreaView.this.handleRefreshBatteryInfo(pluggedIn, batteryLevel, temperature);
                    }
                    int unused = KeyguardBottomAreaView.this.mTemperature = temperature;
                    boolean unused2 = KeyguardBottomAreaView.this.mPluggedIn = pluggedIn;
                    int unused3 = KeyguardBottomAreaView.this.mBatteryLevel = batteryLevel;
                }
            }
        };
        this.mDarkMode = false;
        this.mChargingBackArrowTouchListener = new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                KeyguardBottomAreaView.this.mChargingInfoBackArrow.getParent().requestDisallowInterceptTouchEvent(true);
                if (event.getAction() == 0) {
                    KeyguardBottomAreaView.this.mChargingInfoBackArrow.performClick();
                }
                return true;
            }
        };
        this.mChargingBackArrowClickListener = new View.OnClickListener() {
            public void onClick(View v) {
                KeyguardBottomAreaView.this.chargingInfoDown();
            }
        };
        this.mChargingViewClickListener = new View.OnClickListener() {
            public void onClick(View v) {
                AnalyticsHelper.record("keyguard_charging_click");
                KeyguardBottomAreaView.this.chargingInfoUp();
            }
        };
        this.mChargingEnterAnimRunnable = new Runnable() {
            public void run() {
                if (KeyguardBottomAreaView.this.mPluggedIn) {
                    AnalyticsHelper.record("keyguard_charging_show");
                    KeyguardBottomAreaView.this.mChargingInfoContainer.startEnterAnim();
                }
            }
        };
        this.mRefreshChargingInfoRunnable = new Runnable() {
            public void run() {
                KeyguardBottomAreaView.this.refreshChargingInfo();
            }
        };
        this.mKeyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mLockPatternUtils = new LockPatternUtils(this.mContext);
        this.mEmergencyButton = (EmergencyButton) findViewById(R.id.emergency_call_button);
        this.mRightAffordanceViewLayout = (LinearLayout) findViewById(R.id.right_button_layout);
        this.mLeftAffordanceViewLayout = (LinearLayout) findViewById(R.id.left_button_layout);
        this.mRightAffordanceView = (KeyguardAffordanceView) findViewById(R.id.right_button);
        this.mLeftAffordanceView = (KeyguardAffordanceView) findViewById(R.id.left_button);
        this.mRightAffordanceViewTips = (TextView) findViewById(R.id.right_button_tips);
        this.mLeftAffordanceViewTips = (TextView) findViewById(R.id.left_button_tips);
        initTipsView(true);
        initTipsView(false);
        this.mIndicationArea = (ViewGroup) findViewById(R.id.keyguard_indication_area);
        this.mEnterpriseDisclosure = (TextView) findViewById(R.id.keyguard_indication_enterprise_disclosure);
        watchForCameraPolicyChanges();
        updateCameraVisibility();
        this.mUnlockMethodCache = UnlockMethodCache.getInstance(getContext());
        this.mUnlockMethodCache.addListener(this);
        updateEmergencyButton();
        setClipChildren(false);
        setClipToPadding(false);
        this.mPreviewInflater = new PreviewInflater(this.mContext, new LockPatternUtils(this.mContext));
        this.mRightAffordanceView.setOnClickListener(this);
        this.mLeftAffordanceView.setOnClickListener(this);
        initAccessibility();
        this.mActivityStarter = (ActivityStarter) Dependency.get(ActivityStarter.class);
        this.mFlashlightController = (FlashlightController) Dependency.get(FlashlightController.class);
        this.mAccessibilityController = (AccessibilityController) Dependency.get(AccessibilityController.class);
        this.mAssistManager = (AssistManager) Dependency.get(AssistManager.class);
        updateLeftAffordance();
        this.mChargingInfoContainer = (MiuiKeyguardChargingContainer) findViewById(R.id.miui_keyguard_charging_info_id);
        this.mChargingInfoContainer.setNeedRepositionDevice(this.mNeedRepositionDevice);
        this.mChargingListAndBackArrow = (LinearLayout) findViewById(R.id.charging_list_and_back_arrow_layout_id);
        this.mChargingInfoBackArrow = (ImageView) findViewById(R.id.keyguard_charging_info_back_arrow_id);
        this.mChargingInfoBackArrow.setOnTouchListener(this.mChargingBackArrowTouchListener);
        this.mChargingInfoBackArrow.setOnClickListener(this.mChargingBackArrowClickListener);
        this.mChargingView = (MiuiKeyguardChargingView) findViewById(R.id.battery_charging_view);
        this.mChargingView.setOnClickListener(this.mChargingViewClickListener);
        this.mChargingView.setNeedRepositionDevice(this.mNeedRepositionDevice);
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        this.mKeyguardUpdateMonitor.registerCallback(this.mUpdateMonitorCallback);
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == 0 && event.getKeyCode() == 4 && this.mChargingInfoContainer != null && this.mChargingInfoContainer.isFullScreen() && !this.mChargingInfoContainer.isChargingAnimationInDeclining()) {
            chargingInfoDown();
        }
        return super.dispatchKeyEvent(event);
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mAccessibilityController.addStateChangedCallback(this);
        this.mRightExtension = ((ExtensionController) Dependency.get(ExtensionController.class)).newExtension(IntentButtonProvider.IntentButton.class).withPlugin(IntentButtonProvider.class, "com.android.systemui.action.PLUGIN_LOCKSCREEN_RIGHT_BUTTON", new ExtensionController.PluginConverter<IntentButtonProvider.IntentButton, IntentButtonProvider>() {
            public IntentButtonProvider.IntentButton getInterfaceFromPlugin(IntentButtonProvider plugin) {
                return plugin.getIntentButton();
            }
        }).withDefault(new Supplier<IntentButtonProvider.IntentButton>() {
            public IntentButtonProvider.IntentButton get() {
                return new DefaultRightButton();
            }
        }).withCallback(new Consumer<IntentButtonProvider.IntentButton>() {
            public void accept(IntentButtonProvider.IntentButton button) {
                KeyguardBottomAreaView.this.setRightButton(button);
            }
        }).build();
        this.mLeftExtension = ((ExtensionController) Dependency.get(ExtensionController.class)).newExtension(IntentButtonProvider.IntentButton.class).withPlugin(IntentButtonProvider.class, "com.android.systemui.action.PLUGIN_LOCKSCREEN_LEFT_BUTTON", new ExtensionController.PluginConverter<IntentButtonProvider.IntentButton, IntentButtonProvider>() {
            public IntentButtonProvider.IntentButton getInterfaceFromPlugin(IntentButtonProvider plugin) {
                return plugin.getIntentButton();
            }
        }).withDefault(new Supplier<IntentButtonProvider.IntentButton>() {
            public IntentButtonProvider.IntentButton get() {
                return new DefaultLeftButton();
            }
        }).withCallback(new Consumer<IntentButtonProvider.IntentButton>() {
            public void accept(IntentButtonProvider.IntentButton button) {
                KeyguardBottomAreaView.this.setLeftButton(button);
            }
        }).build();
        if (MiuiKeyguardUtils.supportWirelessCharge()) {
            this.mKeyguardUpdateMonitor.registerWirelessChargeCallback(this.mWirelessChargeCallback);
        }
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mAccessibilityController.removeStateChangedCallback(this);
        this.mRightExtension.destroy();
        this.mLeftExtension.destroy();
        if (MiuiKeyguardUtils.supportWirelessCharge()) {
            this.mKeyguardUpdateMonitor.unregisterWirelessChargeCallback(this.mWirelessChargeCallback);
        }
    }

    private void initAccessibility() {
        this.mLeftAffordanceView.setAccessibilityDelegate(this.mAccessibilityDelegate);
        this.mRightAffordanceView.setAccessibilityDelegate(this.mAccessibilityDelegate);
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateEmergencyButton();
        float fontScale = newConfig.fontScale;
        if (this.mFontScale != fontScale) {
            updateViewsTextSize();
            this.mFontScale = fontScale;
        }
        int densityDpi = newConfig.densityDpi;
        if (densityDpi != this.mDensityDpi) {
            updateViewsLayoutParams();
            updateViewsTextSize();
            updateDrawableResource();
            this.mDensityDpi = densityDpi;
        }
        String language = newConfig.locale.getLanguage();
        if (!TextUtils.isEmpty(language) && !language.equals(this.mLanguage)) {
            initTipsView(false);
            this.mLanguage = language;
        }
    }

    private void updateViewsTextSize() {
        int tipsTextSize = getResources().getDimensionPixelSize(R.dimen.keyguard_bottom_button_tips_text_size);
        this.mLeftAffordanceViewTips.setTextSize(0, (float) tipsTextSize);
        this.mRightAffordanceViewTips.setTextSize(0, (float) tipsTextSize);
    }

    private void updateDrawableResource() {
        initTipsView(true);
        initTipsView(false);
    }

    private void updateViewsLayoutParams() {
        int width = getResources().getDimensionPixelOffset(R.dimen.keyguard_affordance_width);
        int height = getResources().getDimensionPixelOffset(R.dimen.keyguard_affordance_height);
        if (this.mLockScreenMagazineController != null) {
            this.mLockScreenMagazineController.initPreMainEntryIcon();
        }
        this.mLeftAffordanceViewLayout.setPaddingRelative(0, 0, width, 0);
        this.mRightAffordanceViewLayout.setPaddingRelative(width, 0, 0, 0);
        IntentButtonProvider.IntentButton.IconState leftState = this.mLeftButton.getIcon();
        this.mLeftAffordanceView.setImageDrawable(leftState.drawable, leftState.tint);
        LinearLayout.LayoutParams leftViewLayoutParams = (LinearLayout.LayoutParams) this.mLeftAffordanceView.getLayoutParams();
        leftViewLayoutParams.height = height;
        leftViewLayoutParams.width = width;
        this.mLeftAffordanceView.setLayoutParams(leftViewLayoutParams);
        LinearLayout.LayoutParams leftTipsLayoutParams = (LinearLayout.LayoutParams) this.mLeftAffordanceViewTips.getLayoutParams();
        leftTipsLayoutParams.setMarginStart(getResources().getDimensionPixelOffset(R.dimen.keyguard_bottom_left_button_tips_margin_start));
        this.mLeftAffordanceViewTips.setLayoutParams(leftTipsLayoutParams);
        IntentButtonProvider.IntentButton.IconState rightState = this.mRightButton.getIcon();
        this.mRightAffordanceView.setImageDrawable(rightState.drawable, rightState.tint);
        LinearLayout.LayoutParams rightViewLayoutParams = (LinearLayout.LayoutParams) this.mRightAffordanceView.getLayoutParams();
        rightViewLayoutParams.height = height;
        rightViewLayoutParams.width = width;
        this.mRightAffordanceView.setLayoutParams(rightViewLayoutParams);
        LinearLayout.LayoutParams rightTipsLayoutParams = (LinearLayout.LayoutParams) this.mRightAffordanceViewTips.getLayoutParams();
        rightTipsLayoutParams.setMarginEnd(getResources().getDimensionPixelOffset(R.dimen.keyguard_bottom_right_button_tips_margin_end));
        this.mRightAffordanceViewTips.setLayoutParams(rightTipsLayoutParams);
        FrameLayout.LayoutParams indicationLayoutParams = (FrameLayout.LayoutParams) this.mIndicationArea.getLayoutParams();
        indicationLayoutParams.height = getResources().getDimensionPixelOffset(R.dimen.keyguard_affordance_height);
        indicationLayoutParams.setMarginsRelative(width, 0, width, 0);
        this.mIndicationArea.setLayoutParams(indicationLayoutParams);
        FrameLayout.LayoutParams chargingInfoLayoutParams = (FrameLayout.LayoutParams) this.mChargingInfoContainer.getLayoutParams();
        chargingInfoLayoutParams.height = getResources().getDimensionPixelOffset(R.dimen.keyguard_bottom_charging_info_height);
        this.mChargingInfoContainer.setLayoutParams(chargingInfoLayoutParams);
    }

    private void updateRightAffordanceIcon() {
        IntentButtonProvider.IntentButton.IconState state = this.mRightButton.getIcon();
        this.mRightAffordanceView.setVisibility((this.mDozing || !state.isVisible) ? 8 : 0);
        this.mRightAffordanceView.setImageDrawable(state.drawable, state.tint);
        this.mRightAffordanceView.setContentDescription(state.contentDescription);
        initTipsView(false);
    }

    public void setStatusBar(StatusBar statusBar) {
        this.mStatusBar = statusBar;
        updateCameraVisibility();
    }

    public void setUserSetupComplete(boolean userSetupComplete) {
        this.mUserSetupComplete = userSetupComplete;
        updateCameraVisibility();
        updateLeftAffordanceIcon();
    }

    private Intent getCameraIntent() {
        return this.mRightButton.getIntent();
    }

    /* access modifiers changed from: private */
    public Intent getLockScreenMagazineIntent() {
        return this.mLeftButton.getIntent();
    }

    public ResolveInfo resolveCameraIntent() {
        return PackageUtils.resolveIntent(this.mContext, getCameraIntent(), 65536);
    }

    /* access modifiers changed from: private */
    public void updateCameraVisibility() {
        if (this.mRightAffordanceView != null) {
            this.mRightAffordanceView.setVisibility((this.mDozing || !this.mRightButton.getIcon().isVisible) ? 8 : 0);
        }
    }

    private void updateLeftAffordanceIcon() {
        IntentButtonProvider.IntentButton.IconState state = this.mLeftButton.getIcon();
        this.mLeftAffordanceView.setVisibility((this.mDozing || !state.isVisible) ? 8 : 0);
        this.mLeftAffordanceView.setImageDrawable(state.drawable, state.tint);
        this.mLeftAffordanceView.setContentDescription(state.contentDescription);
        initTipsView(true);
    }

    /* access modifiers changed from: private */
    public boolean isCameraDisabledByDpm() {
        DevicePolicyManager dpm = (DevicePolicyManager) getContext().getSystemService("device_policy");
        boolean z = false;
        if (dpm == null || this.mStatusBar == null) {
            return false;
        }
        boolean disabledBecauseKeyguardSecure = (dpm.getKeyguardDisabledFeatures(null, KeyguardUpdateMonitor.getCurrentUser()) & 2) != 0 && this.mStatusBar.isKeyguardSecure();
        if (dpm.getCameraDisabled(null) || disabledBecauseKeyguardSecure) {
            z = true;
        }
        return z;
    }

    private void watchForCameraPolicyChanges() {
        this.mUserUnlocked = UserManagerCompat.isUserUnlocked(UserManager.get(this.mContext), KeyguardUpdateMonitor.getCurrentUser());
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED");
        getContext().registerReceiverAsUser(this.mDevicePolicyReceiver, UserHandle.ALL, filter, null, null);
    }

    public void onStateChanged(boolean accessibilityEnabled, boolean touchExplorationEnabled) {
    }

    public void onClick(View v) {
        if (v == this.mRightAffordanceView) {
            handleBottomButtonClicked(true);
            startButtonLayoutAnimate(false);
        } else if (v == this.mLeftAffordanceView) {
            handleBottomButtonClicked(true);
            startButtonLayoutAnimate(true);
        }
    }

    public void startButtonLayoutAnimate(boolean isLeftButton) {
        if (!this.mLeftButtonLayoutAnimatorSet.isRunning() && !this.mRightButtonLayoutAnimatorSet.isRunning()) {
            if (isLeftButton) {
                startButtonLayoutAnimate(this.mLeftButtonLayoutAnimatorSet, this.mLeftAffordanceViewLayout, this.mLeftAffordanceViewTips, true);
            } else {
                startButtonLayoutAnimate(this.mRightButtonLayoutAnimatorSet, this.mRightAffordanceViewLayout, this.mRightAffordanceViewTips, false);
            }
        }
    }

    private void startButtonLayoutAnimate(AnimatorSet animatorSet, View layoutView, TextView tipsView, boolean isLeftButton) {
        AnimatorSet animatorSet2 = animatorSet;
        View view = layoutView;
        final TextView textView = tipsView;
        float direction = isLeftButton ? -1.0f : 1.0f;
        ObjectAnimator translationXAnimator1 = ObjectAnimator.ofFloat(view, TRANSLATION_X, new float[]{0.0f, -50.0f * direction});
        translationXAnimator1.setDuration(150);
        ObjectAnimator translationXAnimator2 = ObjectAnimator.ofFloat(view, TRANSLATION_X, new float[]{-50.0f * direction, 10.0f * direction});
        translationXAnimator2.setDuration(150);
        ObjectAnimator translationXAnimator3 = ObjectAnimator.ofFloat(view, TRANSLATION_X, new float[]{10.0f * direction, -8.0f * direction});
        translationXAnimator3.setDuration(100);
        ObjectAnimator translationXAnimator4 = ObjectAnimator.ofFloat(view, TRANSLATION_X, new float[]{-8.0f * direction, 5.0f * direction});
        translationXAnimator4.setDuration(100);
        ObjectAnimator translationXAnimator5 = ObjectAnimator.ofFloat(view, TRANSLATION_X, new float[]{5.0f * direction, 0.0f});
        translationXAnimator5.setDuration(100);
        ObjectAnimator tipsAlphaAnimator = ObjectAnimator.ofFloat(textView, ALPHA, new float[]{1.0f, 0.0f});
        tipsAlphaAnimator.setDuration(500);
        tipsAlphaAnimator.setStartDelay(1000);
        animatorSet2.play(translationXAnimator1).before(translationXAnimator2);
        animatorSet2.play(translationXAnimator2).before(translationXAnimator3);
        animatorSet2.play(translationXAnimator3).before(translationXAnimator4);
        animatorSet2.play(translationXAnimator4).before(translationXAnimator5);
        animatorSet2.play(translationXAnimator5).before(tipsAlphaAnimator);
        animatorSet2.setInterpolator(new SineEaseInOutInterpolater());
        animatorSet2.addListener(new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animation) {
                textView.setVisibility(0);
                textView.setTextColor(KeyguardBottomAreaView.this.getTextColor());
                textView.setAlpha(1.0f);
            }

            public void onAnimationCancel(Animator animation) {
                KeyguardBottomAreaView.this.handleBottomButtonClicked(false);
            }

            public void onAnimationEnd(Animator animation) {
                textView.setVisibility(8);
                KeyguardBottomAreaView.this.handleBottomButtonClicked(false);
            }
        });
        animatorSet.start();
    }

    public void initTipsView(boolean isLeft) {
        int i;
        String str;
        int i2;
        if (isLeft) {
            boolean isSupportLockScreenMagazineLeft = this.mKeyguardUpdateMonitor.isSupportLockScreenMagazineLeft();
            TextView textView = this.mLeftAffordanceViewTips;
            if (isSupportLockScreenMagazineLeft) {
                str = this.mContext.getString(R.string.open_lock_screen_magazine_hint_text);
            } else {
                str = this.mContext.getString(R.string.open_remote_center_hint_text);
            }
            textView.setText(str);
            Context context = this.mContext;
            if (this.mDarkMode) {
                i2 = R.drawable.keyguard_bottom_guide_right_arrow_dark;
            } else {
                i2 = R.drawable.keyguard_bottom_guide_right_arrow;
            }
            this.mLeftAffordanceViewTips.setCompoundDrawablesWithIntrinsicBounds(null, null, context.getDrawable(i2), null);
            return;
        }
        this.mRightAffordanceViewTips.setText(this.mContext.getString(R.string.open_camera_hint_text));
        Context context2 = this.mContext;
        if (this.mDarkMode) {
            i = R.drawable.keyguard_bottom_guide_left_arrow_dark;
        } else {
            i = R.drawable.keyguard_bottom_guide_left_arrow;
        }
        this.mRightAffordanceViewTips.setCompoundDrawablesWithIntrinsicBounds(context2.getDrawable(i), null, null, null);
    }

    public void handleBottomButtonClicked(boolean isClickAnimating) {
        this.mKeyguardUpdateMonitor.handleBottomAreaButtonClicked(isClickAnimating);
    }

    /* access modifiers changed from: private */
    public int getTextColor() {
        int i;
        Resources resources = this.mContext.getResources();
        if (this.mDarkMode) {
            i = R.color.miui_common_unlock_screen_common_dark_text_color;
        } else {
            i = R.color.miui_default_lock_screen_unlock_bottom_tips_text_color;
        }
        return resources.getColor(i);
    }

    public boolean onLongClick(View v) {
        handleTrustCircleClick();
        return true;
    }

    private void handleTrustCircleClick() {
        this.mLockscreenGestureLogger.write(getContext(), 191, 0, 0);
        this.mIndicationController.showTransientIndication((int) R.string.keyguard_indication_trust_disabled);
        this.mLockPatternUtils.requireCredentialEntry(KeyguardUpdateMonitor.getCurrentUser());
    }

    public void launchCamera(String source) {
        this.mContext.startActivityAsUser(getCameraIntent(), UserHandle.CURRENT);
        AnalyticsHelper.record("camera_from_keyguard");
    }

    public void launchLockScreenMagazine(String source) {
        Intent intent = getLockScreenMagazineIntent();
        if (intent != null) {
            this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
        }
    }

    /* access modifiers changed from: protected */
    public void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (changedView == this && visibility == 0) {
            updateCameraVisibility();
        }
    }

    public KeyguardAffordanceView getLeftView() {
        return this.mLeftAffordanceView;
    }

    public KeyguardAffordanceView getRightView() {
        return this.mRightAffordanceView;
    }

    public boolean hasOverlappingRendering() {
        return false;
    }

    public void onUnlockMethodStateChanged() {
        updateCameraVisibility();
    }

    public void startFinishDozeAnimation() {
        long delay = 0;
        if (this.mLeftAffordanceView.getVisibility() == 0) {
            startFinishDozeAnimationElement(this.mLeftAffordanceView, 0);
            delay = 0 + 48;
        }
        long delay2 = delay + 48;
        if (this.mRightAffordanceView.getVisibility() == 0) {
            startFinishDozeAnimationElement(this.mRightAffordanceView, delay2);
        }
        this.mIndicationArea.setAlpha(0.0f);
        this.mIndicationArea.animate().alpha(1.0f).setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN).setDuration(700);
    }

    private void startFinishDozeAnimationElement(View element, long delay) {
        element.setAlpha(0.0f);
        element.setTranslationY((float) (element.getHeight() / 2));
        element.animate().alpha(1.0f).translationY(0.0f).setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN).setStartDelay(delay).setDuration(250);
    }

    public void setKeyguardIndicationController(KeyguardIndicationController keyguardIndicationController) {
        this.mIndicationController = keyguardIndicationController;
        this.mIndicationController.setDarkMode(this.mDarkMode);
    }

    public void setNotificationPanelView(NotificationPanelView panelView) {
        this.mNotificationPanelView = panelView;
    }

    public void updateLeftAffordance() {
        updateLeftAffordanceIcon();
    }

    public void onKeyguardShowingChanged() {
        updateLeftAffordance();
        updateRightAffordanceIcon();
    }

    /* access modifiers changed from: private */
    public void setRightButton(IntentButtonProvider.IntentButton button) {
        this.mRightButton = button;
        updateRightAffordanceIcon();
        updateCameraVisibility();
    }

    /* access modifiers changed from: private */
    public void setLeftButton(IntentButtonProvider.IntentButton button) {
        this.mLeftButton = button;
        if (!(this.mLeftButton instanceof DefaultLeftButton)) {
            this.mLeftIsVoiceAssist = false;
        }
        updateLeftAffordance();
    }

    public void setDozing(boolean dozing, boolean animate) {
        this.mDozing = dozing;
        updateCameraVisibility();
        updateLeftAffordanceIcon();
        if (!dozing && animate) {
            startFinishDozeAnimation();
        }
    }

    /* access modifiers changed from: private */
    public Drawable getLockScreenMagazineMainEntryIcon() {
        if (this.mDarkMode) {
            Drawable drawable = this.mLockScreenMagazineController.getPreMainEntryResDarkIcon();
            if (drawable != null) {
                return drawable;
            }
            return this.mContext.getDrawable(R.drawable.keyguard_bottom_lock_screen_magazine_img_dark);
        }
        Drawable drawable2 = this.mLockScreenMagazineController.getPreMainEntryResLightIcon();
        if (drawable2 != null) {
            return drawable2;
        }
        return this.mContext.getDrawable(R.drawable.keyguard_bottom_lock_screen_magazine_img);
    }

    private void updateEmergencyButton() {
        if (this.mEmergencyButton != null) {
            this.mEmergencyButton.updateEmergencyCallButton();
        }
    }

    public void setDarkMode(boolean dark) {
        if (this.mDarkMode != dark) {
            this.mDarkMode = dark;
            this.mChargingInfoContainer.setDarkMode(this.mDarkMode);
            updateLeftAffordanceIcon();
            updateRightAffordanceIcon();
            if (this.mIndicationController != null) {
                this.mIndicationController.setDarkMode(this.mDarkMode);
            }
        }
    }

    public void chargingInfoUp() {
        this.mChargingInfoContainer.onChargeViewClick();
        removeCallbacks(this.mRefreshChargingInfoRunnable);
        post(this.mRefreshChargingInfoRunnable);
    }

    /* access modifiers changed from: private */
    public void handleRefreshBatteryInfo(boolean pluggedIn, int batteryLevel, int temp) {
        boolean canShowChargeCircle = MiuiKeyguardUtils.canShowChargeCircle(getContext());
        Log.i("StatusBar/KeyguardBottomAreaView", "handleRefreshBatteryInfo: pluggedIn: " + pluggedIn + " batteryLevel: " + batteryLevel + " temp: " + temp + " canShowChargeCircle: " + canShowChargeCircle);
        this.mChargingView.setChargingLevel(batteryLevel);
        this.mChargingInfoContainer.setChargingInfo(this.mChargingHintText, temp, batteryLevel);
        boolean anim = false;
        if (pluggedIn && !this.mPluggedIn) {
            removeCallbacks(this.mChargingEnterAnimRunnable);
            postDelayed(this.mChargingEnterAnimRunnable, this.mPowerManager.isScreenOn() ? 0 : 300);
            this.mChargingInfoContainer.clearAnimation();
            anim = true;
        }
        if (!pluggedIn || !canShowChargeCircle) {
            removeCallbacks(this.mChargingEnterAnimRunnable);
            this.mChargingInfoContainer.updateVisibility(false);
            this.mChargingView.setVisibility(4);
            chargingInfoDown();
        } else {
            this.mChargingInfoContainer.updateVisibility(true);
            if (!anim) {
                this.mChargingView.setVisibility(0);
            }
        }
        this.mTemperature = temp;
        this.mPluggedIn = pluggedIn;
        this.mBatteryLevel = batteryLevel;
        removeCallbacks(this.mRefreshChargingInfoRunnable);
        postDelayed(this.mRefreshChargingInfoRunnable, 500);
    }

    /* access modifiers changed from: private */
    public void refreshChargingInfo() {
        if (this.mChargeAsyncTask == null) {
            this.mChargeAsyncTask = new AsyncTask<Void, Void, String>() {
                /* access modifiers changed from: protected */
                public String doInBackground(Void... params) {
                    return ChargeUtils.getChargingHintText(KeyguardBottomAreaView.this.mContext, KeyguardBottomAreaView.this.mPluggedIn, KeyguardBottomAreaView.this.mBatteryLevel);
                }

                /* access modifiers changed from: protected */
                public void onPostExecute(String result) {
                    super.onPostExecute(result);
                    String unused = KeyguardBottomAreaView.this.mChargingHintText = result;
                    if (KeyguardBottomAreaView.this.getWindowToken() != null) {
                        KeyguardBottomAreaView.this.mChargingInfoContainer.setChargingInfo(KeyguardBottomAreaView.this.mChargingHintText, KeyguardBottomAreaView.this.mTemperature, KeyguardBottomAreaView.this.mBatteryLevel);
                    }
                    AsyncTask unused2 = KeyguardBottomAreaView.this.mChargeAsyncTask = null;
                }

                /* access modifiers changed from: protected */
                public void onCancelled() {
                    super.onCancelled();
                    AsyncTask unused = KeyguardBottomAreaView.this.mChargeAsyncTask = null;
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
        }
    }

    /* access modifiers changed from: private */
    public void chargingInfoDown() {
        if (this.mChargingInfoContainer.isFullScreen()) {
            post(new Runnable() {
                public void run() {
                    KeyguardBottomAreaView.this.mChargingInfoContainer.startDownAnim();
                }
            });
        }
    }

    public boolean canShowGxzw() {
        return this.mChargingInfoContainer.canShowGxzw();
    }

    public void reset() {
        chargingInfoDown();
        if (this.mPluggedIn && this.mChargingInfoContainer.getVisibility() != 0 && MiuiKeyguardUtils.canShowChargeCircle(getContext())) {
            this.mChargingInfoContainer.updateVisibility(true);
            this.mChargingView.setVisibility(0);
        } else if (this.mPluggedIn && this.mChargingInfoContainer.getVisibility() == 0 && !MiuiKeyguardUtils.canShowChargeCircle(getContext())) {
            this.mChargingInfoContainer.updateVisibility(false);
            this.mChargingView.setVisibility(4);
        }
        if (!this.mPluggedIn || !MiuiKeyguardUtils.canShowChargeCircle(getContext())) {
            this.mChargingView.setVisibility(4);
        }
    }

    public void setViewsAlpha(float alpha) {
        this.mLeftAffordanceViewLayout.setAlpha(alpha);
        this.mRightAffordanceViewLayout.setAlpha(alpha);
        this.mChargingInfoContainer.setAlpha(alpha);
        this.mIndicationArea.setAlpha(alpha);
    }

    public void setLockScreenMagazineController(LockScreenMagazineController lockScreenMagazineController) {
        this.mLockScreenMagazineController = lockScreenMagazineController;
    }
}
