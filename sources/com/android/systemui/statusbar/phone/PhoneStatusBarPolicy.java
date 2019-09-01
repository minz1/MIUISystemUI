package com.android.systemui.statusbar.phone;

import android.app.ActivityManager;
import android.app.ActivityManagerCompat;
import android.app.AlarmManager;
import android.app.AppGlobals;
import android.app.NotificationManager;
import android.app.StatusBarManager;
import android.app.SynchronousUserSwitchObserverCompat;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.IPackageManager;
import android.content.pm.UserInfo;
import android.content.pm.UserInfoCompat;
import android.database.ContentObserver;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.UserManagerCompat;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.service.notification.Condition;
import android.service.notification.StatusBarNotification;
import android.service.notification.ZenModeConfig;
import android.util.ArraySet;
import android.util.Log;
import android.util.Pair;
import com.android.internal.os.SomeArgs;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.systemui.Constants;
import com.android.systemui.Dependency;
import com.android.systemui.DockedStackExistsListener;
import com.android.systemui.R;
import com.android.systemui.SystemUI;
import com.android.systemui.UiOffloadThread;
import com.android.systemui.qs.tiles.RotationLockTile;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.policy.BluetoothController;
import com.android.systemui.statusbar.policy.DataSaverController;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.android.systemui.statusbar.policy.LocationController;
import com.android.systemui.statusbar.policy.NextAlarmController;
import com.android.systemui.statusbar.policy.RotationLockController;
import com.android.systemui.statusbar.policy.UserInfoController;
import com.android.systemui.statusbar.policy.ZenModeController;
import com.android.systemui.util.ApplicationInfoHelper;
import com.android.systemui.util.function.Consumer;
import java.util.Iterator;
import miui.securityspace.XSpaceUserHandle;
import miui.util.AudioManagerHelper;

public class PhoneStatusBarPolicy implements CommandQueue.Callbacks, BluetoothController.Callback, DataSaverController.Listener, DeviceProvisionedController.DeviceProvisionedListener, KeyguardMonitor.Callback, LocationController.LocationChangeCallback, RotationLockController.RotationLockControllerCallback, ZenModeController.Callback {
    /* access modifiers changed from: private */
    public static final boolean DEBUG = Log.isLoggable("PhoneStatusBarPolicy", 3);
    public static final int LOCATION_STATUS_ACQUIRING_ICON_ID;
    public static final int LOCATION_STATUS_ON_ICON_ID;
    private final AlarmManager mAlarmManager;
    private BluetoothController mBluetooth;
    private byte mBluetoothFlowState;
    /* access modifiers changed from: private */
    public final Context mContext;
    private final ArraySet<Pair<String, Integer>> mCurrentNotifs = new ArraySet<>();
    /* access modifiers changed from: private */
    public int mCurrentProfileId;
    /* access modifiers changed from: private */
    public int mCurrentUserId;
    private boolean mCurrentUserSetup;
    private final DataSaverController mDataSaver;
    /* access modifiers changed from: private */
    public boolean mDockedStackExists;
    /* access modifiers changed from: private */
    public final Handler mHandler = new Handler();
    /* access modifiers changed from: private */
    public final StatusBarIconController mIconController;
    private BroadcastReceiver mIntentReceiver;
    /* access modifiers changed from: private */
    public final KeyguardMonitor mKeyguardMonitor;
    private final LocationController mLocationController;
    /* access modifiers changed from: private */
    public boolean mManagedProfileIconVisible;
    /* access modifiers changed from: private */
    public boolean mManagedProfileInQuietMode;
    private final NextAlarmController mNextAlarm;
    private final NextAlarmController.NextAlarmChangeCallback mNextAlarmCallback;
    private final DeviceProvisionedController mProvisionedController;
    private Runnable mRemoveCastIconRunnable;
    private final RotationLockController mRotationLockController;
    private final ContentObserver mSecondSpaceStatusIconObsever;
    /* access modifiers changed from: private */
    public boolean mSecondSpaceStatusIconVisible;
    private final StatusBarManager mService;
    private final String mSlotAlarmClock;
    private final String mSlotBluetooth;
    private final String mSlotBluetoothBattery;
    private final String mSlotCallrecord;
    /* access modifiers changed from: private */
    public final String mSlotCast;
    private final String mSlotDataSaver;
    private final String mSlotHeadset;
    private final String mSlotLocation;
    /* access modifiers changed from: private */
    public final String mSlotManagedProfile;
    private final String mSlotMute;
    private final String mSlotQuiet;
    private final String mSlotRotate;
    private final String mSlotSpeakerphone;
    private final String mSlotSyncActive;
    private final String mSlotTty;
    private final String mSlotVolume;
    private final String mSlotZen;
    private final SystemServicesProxy.TaskStackListener mTaskListener;
    private final UiOffloadThread mUiOffloadThread = ((UiOffloadThread) Dependency.get(UiOffloadThread.class));
    /* access modifiers changed from: private */
    public int mUserIdLegacy;
    /* access modifiers changed from: private */
    public final UserInfoController mUserInfoController;
    /* access modifiers changed from: private */
    public final UserManager mUserManager;
    private final SynchronousUserSwitchObserverCompat mUserSwitchListener;
    private boolean mVolumeVisible;
    private final ZenModeController mZenController;

    static {
        int i;
        int i2;
        if (Constants.SUPPORT_DUAL_GPS) {
            i = R.drawable.stat_sys_dual_gps_on;
        } else {
            i = R.drawable.stat_sys_gps_on;
        }
        LOCATION_STATUS_ON_ICON_ID = i;
        if (Constants.SUPPORT_DUAL_GPS) {
            i2 = R.drawable.stat_sys_dual_gps_acquiring;
        } else {
            i2 = R.drawable.stat_sys_gps_acquiring;
        }
        LOCATION_STATUS_ACQUIRING_ICON_ID = i2;
    }

    public PhoneStatusBarPolicy(Context context, StatusBarIconController iconController) {
        this.mManagedProfileIconVisible = false;
        this.mManagedProfileInQuietMode = false;
        this.mSecondSpaceStatusIconObsever = new ContentObserver(this.mHandler) {
            public void onChange(boolean selfChange) {
                boolean unused = PhoneStatusBarPolicy.this.mSecondSpaceStatusIconVisible = MiuiSettings.Global.isOpenSecondSpaceStatusIcon(PhoneStatusBarPolicy.this.mContext.getContentResolver());
                PhoneStatusBarPolicy.this.updateManagedProfile();
            }
        };
        this.mBluetoothFlowState = 0;
        this.mUserSwitchListener = new SynchronousUserSwitchObserverCompat() {
            public void onUserSwitching(int newUserId) throws RemoteException {
                PhoneStatusBarPolicy.this.mHandler.post(new Runnable() {
                    public void run() {
                        PhoneStatusBarPolicy.this.mUserInfoController.reloadUserInfo();
                    }
                });
            }

            public void onUserSwitchComplete(final int newUserId) throws RemoteException {
                PhoneStatusBarPolicy.this.mHandler.post(new Runnable() {
                    public void run() {
                        int unused = PhoneStatusBarPolicy.this.mUserIdLegacy = newUserId;
                        int unused2 = PhoneStatusBarPolicy.this.mCurrentUserId = ActivityManager.getCurrentUser();
                        PhoneStatusBarPolicy.this.updateQuietState();
                        PhoneStatusBarPolicy.this.updateManagedProfile();
                        PhoneStatusBarPolicy.this.updateForegroundInstantApps();
                    }
                });
            }

            public void onForegroundProfileSwitch(int newProfileId) throws RemoteException {
                int unused = PhoneStatusBarPolicy.this.mUserIdLegacy = newProfileId;
                PhoneStatusBarPolicy.this.profileChanged(newProfileId);
            }
        };
        this.mNextAlarmCallback = new NextAlarmController.NextAlarmChangeCallback() {
            public void onNextAlarmChanged(boolean hasAlarm) {
                PhoneStatusBarPolicy.this.updateAlarm(hasAlarm);
            }
        };
        this.mTaskListener = new SystemServicesProxy.TaskStackListener() {
            public void onTaskStackChanged() {
                PhoneStatusBarPolicy.this.updateForegroundInstantApps();
            }
        };
        this.mIntentReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals("android.media.RINGER_MODE_CHANGED") || action.equals("android.media.VIBRATE_SETTING_CHANGED")) {
                    if (!MiuiSettings.SilenceMode.isSupported) {
                        PhoneStatusBarPolicy.this.updateVolume();
                    }
                } else if (action.equals("android.telecom.action.CURRENT_TTY_MODE_CHANGED")) {
                    PhoneStatusBarPolicy.this.updateTTY(intent);
                } else if (action.equals("android.intent.action.MANAGED_PROFILE_AVAILABLE") || action.equals("android.intent.action.MANAGED_PROFILE_UNAVAILABLE") || action.equals("android.intent.action.MANAGED_PROFILE_REMOVED")) {
                    PhoneStatusBarPolicy.this.updateQuietState();
                    PhoneStatusBarPolicy.this.updateManagedProfile();
                } else if (action.equals("android.intent.action.HEADSET_PLUG")) {
                    PhoneStatusBarPolicy.this.updateHeadsetPlug(intent);
                } else if ("android.intent.action.BLUETOOTH_HANDSFREE_BATTERY_CHANGED".equals(action)) {
                    PhoneStatusBarPolicy.this.updateBluetoothHandsfreeBattery(intent);
                }
            }
        };
        this.mRemoveCastIconRunnable = new Runnable() {
            public void run() {
                if (PhoneStatusBarPolicy.DEBUG) {
                    Log.v("PhoneStatusBarPolicy", "updateCast: hiding icon NOW");
                }
                PhoneStatusBarPolicy.this.mIconController.setIconVisibility(PhoneStatusBarPolicy.this.mSlotCast, false);
            }
        };
        this.mContext = context;
        this.mCurrentUserId = ActivityManager.getCurrentUser();
        this.mIconController = iconController;
        this.mService = (StatusBarManager) context.getSystemService("statusbar");
        this.mBluetooth = (BluetoothController) Dependency.get(BluetoothController.class);
        this.mNextAlarm = (NextAlarmController) Dependency.get(NextAlarmController.class);
        this.mAlarmManager = (AlarmManager) context.getSystemService("alarm");
        this.mUserInfoController = (UserInfoController) Dependency.get(UserInfoController.class);
        this.mUserManager = (UserManager) this.mContext.getSystemService("user");
        this.mRotationLockController = (RotationLockController) Dependency.get(RotationLockController.class);
        this.mDataSaver = (DataSaverController) Dependency.get(DataSaverController.class);
        this.mZenController = (ZenModeController) Dependency.get(ZenModeController.class);
        this.mProvisionedController = (DeviceProvisionedController) Dependency.get(DeviceProvisionedController.class);
        this.mKeyguardMonitor = (KeyguardMonitor) Dependency.get(KeyguardMonitor.class);
        this.mLocationController = (LocationController) Dependency.get(LocationController.class);
        this.mSlotCast = "cast";
        this.mSlotBluetooth = "bluetooth";
        this.mSlotTty = "tty";
        this.mSlotZen = "zen";
        this.mSlotVolume = "volume";
        this.mSlotAlarmClock = "alarm_clock";
        this.mSlotManagedProfile = "managed_profile";
        this.mSlotRotate = "rotate";
        this.mSlotHeadset = "headset";
        this.mSlotDataSaver = "data_saver";
        this.mSlotLocation = "location";
        this.mSlotSyncActive = "sync_active";
        this.mSlotQuiet = "quiet";
        this.mSlotMute = "mute";
        this.mSlotSpeakerphone = "speakerphone";
        this.mSlotCallrecord = "call_record";
        this.mSlotBluetoothBattery = "bluetooth_handsfree_battery";
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.media.RINGER_MODE_CHANGED");
        filter.addAction("android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION");
        filter.addAction("android.intent.action.HEADSET_PLUG");
        filter.addAction("android.telecom.action.CURRENT_TTY_MODE_CHANGED");
        filter.addAction("android.intent.action.BLUETOOTH_HANDSFREE_BATTERY_CHANGED");
        filter.addAction("android.intent.action.MANAGED_PROFILE_AVAILABLE");
        filter.addAction("android.intent.action.MANAGED_PROFILE_UNAVAILABLE");
        filter.addAction("android.intent.action.MANAGED_PROFILE_REMOVED");
        this.mContext.registerReceiverAsUser(this.mIntentReceiver, UserHandle.ALL, filter, null, this.mHandler);
        try {
            ActivityManagerCompat.registerUserSwitchObserver(this.mUserSwitchListener, "PhoneStatusBarPolicy");
        } catch (RemoteException e) {
        }
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("open_second_space_status_icon"), false, this.mSecondSpaceStatusIconObsever);
        this.mSecondSpaceStatusIconObsever.onChange(false);
        this.mIconController.setIcon(this.mSlotTty, R.drawable.stat_sys_tty_mode, null);
        this.mIconController.setIconVisibility(this.mSlotTty, false);
        updateBluetooth(null);
        this.mIconController.setIcon(this.mSlotAlarmClock, R.drawable.stat_sys_alarm, null);
        this.mIconController.setIconVisibility(this.mSlotAlarmClock, false);
        this.mIconController.setIcon(this.mSlotVolume, R.drawable.stat_sys_ringer_vibrate, null);
        this.mIconController.setIconVisibility(this.mSlotVolume, false);
        updateVolume();
        this.mIconController.setIcon(this.mSlotCast, R.drawable.stat_sys_cast, null);
        this.mIconController.setIconVisibility(this.mSlotCast, false);
        this.mIconController.setIcon(this.mSlotManagedProfile, R.drawable.stat_sys_managed_profile_status, this.mContext.getString(R.string.accessibility_managed_profile));
        this.mIconController.setIconVisibility(this.mSlotManagedProfile, this.mManagedProfileIconVisible);
        this.mIconController.setIcon(this.mSlotDataSaver, R.drawable.stat_sys_data_saver, context.getString(R.string.accessibility_data_saver_on));
        this.mIconController.setIconVisibility(this.mSlotDataSaver, false);
        this.mIconController.setIcon(this.mSlotSyncActive, R.drawable.stat_sys_sync_active, null);
        this.mIconController.setIconVisibility(this.mSlotSyncActive, false);
        this.mService.setIcon(this.mSlotMute, R.drawable.stat_notify_call_mute, 0, null);
        this.mService.setIconVisibility(this.mSlotMute, false);
        this.mService.setIcon(this.mSlotSpeakerphone, R.drawable.stat_sys_speakerphone, 0, null);
        this.mService.setIconVisibility(this.mSlotSpeakerphone, false);
        this.mService.setIcon(this.mSlotCallrecord, R.drawable.stat_sys_call_record, 0, null);
        this.mService.setIconVisibility(this.mSlotCallrecord, false);
        this.mIconController.setIcon(this.mSlotQuiet, R.drawable.stat_sys_quiet_mode, null);
        if (MiuiSettings.SilenceMode.getZenMode(this.mContext) == 1) {
            setQuietMode(true);
            this.mIconController.setIconVisibility(this.mSlotVolume, false);
            this.mVolumeVisible = false;
        } else {
            setQuietMode(false);
        }
        this.mRotationLockController.addCallback(this);
        this.mBluetooth.addCallback(this);
        this.mProvisionedController.addCallback(this);
        this.mZenController.addCallback(this);
        this.mNextAlarm.addCallback(this.mNextAlarmCallback);
        this.mDataSaver.addCallback(this);
        this.mKeyguardMonitor.addCallback(this);
        this.mLocationController.addCallback(this);
        ((CommandQueue) SystemUI.getComponent(this.mContext, CommandQueue.class)).addCallbacks(this);
        SystemServicesProxy.getInstance(this.mContext).registerTaskStackListener(this.mTaskListener);
        NotificationManager noMan = (NotificationManager) this.mContext.getSystemService(NotificationManager.class);
        for (StatusBarNotification notification : noMan.getActiveNotifications()) {
            if (notification.getId() == 7) {
                noMan.cancel(notification.getTag(), notification.getId());
            }
        }
        DockedStackExistsListener.register(new Consumer<Boolean>() {
            public void accept(Boolean exists) {
                boolean unused = PhoneStatusBarPolicy.this.mDockedStackExists = exists.booleanValue();
                PhoneStatusBarPolicy.this.updateForegroundInstantApps();
            }
        });
    }

    public void onZenChanged(int zen) {
        updateVolumeZen();
    }

    public void onLocationActiveChanged(boolean active) {
    }

    public void onLocationStatusChanged(Intent intent) {
        updateLocation(intent);
    }

    private void updateLocation(Intent intent) {
        int iconId;
        boolean visible;
        String action = intent.getAction();
        boolean enabled = intent.getBooleanExtra("enabled", false);
        if (action.equals("android.location.GPS_FIX_CHANGE") && enabled) {
            iconId = LOCATION_STATUS_ON_ICON_ID;
            visible = true;
        } else if (!action.equals("android.location.GPS_ENABLED_CHANGE") || enabled) {
            iconId = LOCATION_STATUS_ACQUIRING_ICON_ID;
            visible = true;
        } else {
            iconId = 0;
            visible = false;
        }
        if (iconId != 0) {
            this.mIconController.setIcon(this.mSlotLocation, iconId, null);
        }
        this.mIconController.setIconVisibility(this.mSlotLocation, visible);
    }

    /* access modifiers changed from: private */
    public void updateAlarm(boolean hasAlarm) {
        int i;
        boolean z = false;
        boolean zenNone = this.mZenController.getZen() == 2;
        StatusBarIconController statusBarIconController = this.mIconController;
        String str = this.mSlotAlarmClock;
        if (zenNone) {
            i = R.drawable.stat_sys_alarm_dim;
        } else {
            i = R.drawable.stat_sys_alarm;
        }
        statusBarIconController.setIcon(str, i, this.mContext.getString(R.string.accessibility_quick_settings_alarm_on));
        StatusBarIconController statusBarIconController2 = this.mIconController;
        String str2 = this.mSlotAlarmClock;
        if (this.mCurrentUserSetup && hasAlarm) {
            z = true;
        }
        statusBarIconController2.setIconVisibility(str2, z);
    }

    /* access modifiers changed from: private */
    public final void updateVolume() {
        String contentDescription;
        int iconId;
        boolean visible = AudioManagerHelper.isSilentEnabled(this.mContext);
        if (AudioManagerHelper.isVibrateEnabled(this.mContext)) {
            iconId = R.drawable.stat_sys_ringer_vibrate;
            contentDescription = this.mContext.getString(R.string.accessibility_ringer_vibrate);
        } else {
            iconId = R.drawable.stat_sys_ringer_silent;
            contentDescription = this.mContext.getString(R.string.accessibility_ringer_silent);
        }
        if (visible) {
            this.mIconController.setIcon(this.mSlotVolume, iconId, contentDescription);
        }
        if (visible != this.mVolumeVisible) {
            this.mIconController.setIconVisibility(this.mSlotVolume, visible);
            this.mVolumeVisible = visible;
        }
    }

    public void setQuietMode(boolean quietMode) {
        this.mIconController.setIconVisibility(this.mSlotQuiet, quietMode);
    }

    public void updateSilentModeIcon() {
        if (MiuiSettings.SilenceMode.getZenMode(this.mContext) == 1) {
            setQuietMode(true);
            this.mIconController.setIconVisibility(this.mSlotVolume, false);
            this.mVolumeVisible = false;
            return;
        }
        setQuietMode(false);
        updateVolume();
    }

    private final void updateVolumeZen() {
    }

    public void onBluetoothDevicesChanged() {
        updateBluetooth(null);
    }

    public void onBluetoothStateChange(boolean enabled) {
        updateBluetooth(null);
    }

    public void onBluetoothInoutStateChange(String action) {
        updateBluetooth(action);
    }

    private final void updateBluetooth(String action) {
        int iconId = R.drawable.stat_sys_data_bluetooth;
        String contentDescription = this.mContext.getString(R.string.accessibility_quick_settings_bluetooth_on);
        boolean bluetoothEnabled = false;
        if (this.mBluetooth != null) {
            bluetoothEnabled = this.mBluetooth.isBluetoothEnabled();
            boolean bluetoothEnabledConnected = this.mBluetooth.isBluetoothConnected();
            if (!bluetoothEnabled) {
                this.mIconController.setIconVisibility(this.mSlotBluetoothBattery, false);
            }
            if (bluetoothEnabledConnected) {
                iconId = R.drawable.stat_sys_data_bluetooth_connected;
                contentDescription = this.mContext.getString(R.string.accessibility_bluetooth_connected);
            }
            if ("com.android.bluetooth.opp.BLUETOOTH_OPP_INBOUND_START".equals(action)) {
                this.mBluetoothFlowState = (byte) (this.mBluetoothFlowState | 1);
            } else if ("com.android.bluetooth.opp.BLUETOOTH_OPP_INBOUND_END".equals(action)) {
                this.mBluetoothFlowState = (byte) (this.mBluetoothFlowState & -2);
            } else if ("com.android.bluetooth.opp.BLUETOOTH_OPP_OUTBOUND_START".equals(action)) {
                this.mBluetoothFlowState = (byte) (this.mBluetoothFlowState | 2);
            } else if ("com.android.bluetooth.opp.BLUETOOTH_OPP_OUTBOUND_END".equals(action)) {
                this.mBluetoothFlowState = (byte) (this.mBluetoothFlowState & -3);
            }
            if (this.mBluetoothFlowState == 1) {
                iconId = R.drawable.stat_sys_data_bluetooth_in;
            } else if (this.mBluetoothFlowState == 2) {
                iconId = R.drawable.stat_sys_data_bluetooth_out;
            } else if (this.mBluetoothFlowState == 3) {
                iconId = R.drawable.stat_sys_data_bluetooth_inout;
            }
        }
        this.mIconController.setIcon(this.mSlotBluetooth, iconId, contentDescription);
        this.mIconController.setIconVisibility(this.mSlotBluetooth, bluetoothEnabled);
    }

    /* access modifiers changed from: private */
    public final void updateTTY(Intent intent) {
        boolean enabled = intent.getIntExtra("android.telecom.intent.extra.CURRENT_TTY_MODE", 0) != 0;
        if (DEBUG) {
            Log.v("PhoneStatusBarPolicy", "updateTTY: enabled: " + enabled);
        }
        if (enabled) {
            if (DEBUG) {
                Log.v("PhoneStatusBarPolicy", "updateTTY: set TTY on");
            }
            this.mIconController.setIcon(this.mSlotTty, R.drawable.stat_sys_tty_mode, this.mContext.getString(R.string.accessibility_tty_enabled));
            this.mIconController.setIconVisibility(this.mSlotTty, true);
            return;
        }
        if (DEBUG) {
            Log.v("PhoneStatusBarPolicy", "updateTTY: set TTY off");
        }
        this.mIconController.setIconVisibility(this.mSlotTty, false);
    }

    /* access modifiers changed from: private */
    public void updateQuietState() {
        this.mManagedProfileInQuietMode = false;
        for (UserInfo ui : this.mUserManager.getEnabledProfiles(ActivityManager.getCurrentUser())) {
            if (ui.isManagedProfile() && UserInfoCompat.isQuietModeEnabled(ui)) {
                this.mManagedProfileInQuietMode = true;
                return;
            }
        }
    }

    public void profileChanged(int profiledId) {
        this.mCurrentProfileId = profiledId;
        updateManagedProfile();
    }

    /* access modifiers changed from: private */
    public void updateManagedProfile() {
        this.mUiOffloadThread.submit(new Runnable() {
            public void run() {
                try {
                    final boolean isManagedProfile = UserManagerCompat.isManagedProfile(PhoneStatusBarPolicy.this.mUserManager, ActivityManagerCompat.getLastResumedActivityUserId(PhoneStatusBarPolicy.this.mUserIdLegacy));
                    final boolean xSpace = false;
                    final boolean secondSpace = PhoneStatusBarPolicy.this.mCurrentUserId != 0 && PhoneStatusBarPolicy.this.mSecondSpaceStatusIconVisible;
                    if (PhoneStatusBarPolicy.this.mCurrentUserId == 0 && XSpaceUserHandle.isXSpaceUserId(PhoneStatusBarPolicy.this.mCurrentProfileId)) {
                        xSpace = true;
                    }
                    PhoneStatusBarPolicy.this.mHandler.post(new Runnable() {
                        public void run() {
                            boolean showIcon;
                            if (xSpace) {
                                showIcon = !PhoneStatusBarPolicy.this.mKeyguardMonitor.isShowing();
                                PhoneStatusBarPolicy.this.mIconController.setIcon(PhoneStatusBarPolicy.this.mSlotManagedProfile, R.drawable.stat_sys_managed_profile_xspace_user, PhoneStatusBarPolicy.this.mContext.getString(R.string.accessibility_managed_profile));
                            } else if (isManagedProfile && !PhoneStatusBarPolicy.this.mKeyguardMonitor.isShowing()) {
                                showIcon = true;
                                PhoneStatusBarPolicy.this.mIconController.setIcon(PhoneStatusBarPolicy.this.mSlotManagedProfile, R.drawable.stat_sys_managed_profile_status, PhoneStatusBarPolicy.this.mContext.getString(R.string.accessibility_managed_profile));
                            } else if (PhoneStatusBarPolicy.this.mManagedProfileInQuietMode) {
                                showIcon = true;
                                PhoneStatusBarPolicy.this.mIconController.setIcon(PhoneStatusBarPolicy.this.mSlotManagedProfile, R.drawable.stat_sys_managed_profile_status_off, PhoneStatusBarPolicy.this.mContext.getString(R.string.accessibility_managed_profile));
                            } else if (secondSpace) {
                                showIcon = !PhoneStatusBarPolicy.this.mKeyguardMonitor.isShowing();
                                PhoneStatusBarPolicy.this.mIconController.setIcon(PhoneStatusBarPolicy.this.mSlotManagedProfile, R.drawable.stat_sys_managed_profile_not_owner_user, PhoneStatusBarPolicy.this.mContext.getString(R.string.accessibility_managed_profile));
                            } else {
                                showIcon = false;
                            }
                            if (PhoneStatusBarPolicy.this.mManagedProfileIconVisible != showIcon) {
                                PhoneStatusBarPolicy.this.mIconController.setIconVisibility(PhoneStatusBarPolicy.this.mSlotManagedProfile, showIcon);
                                boolean unused = PhoneStatusBarPolicy.this.mManagedProfileIconVisible = showIcon;
                            }
                        }
                    });
                } catch (RemoteException e) {
                    Log.w("PhoneStatusBarPolicy", "updateManagedProfile: ", e);
                }
            }
        });
    }

    /* access modifiers changed from: private */
    public void updateForegroundInstantApps() {
        final NotificationManager noMan = (NotificationManager) this.mContext.getSystemService(NotificationManager.class);
        final ArraySet<Pair<String, Integer>> notifs = new ArraySet<>(this.mCurrentNotifs);
        final IPackageManager pm = AppGlobals.getPackageManager();
        this.mCurrentNotifs.clear();
        this.mUiOffloadThread.submit(new Runnable() {
            public void run() {
                try {
                    if (ActivityManagerCompat.getFocusedStackId() == 1) {
                        PhoneStatusBarPolicy.this.checkStack(1, 1, 0, notifs, noMan, pm);
                    }
                    if (PhoneStatusBarPolicy.this.mDockedStackExists) {
                        PhoneStatusBarPolicy.this.checkStack(3, 3, 0, notifs, noMan, pm);
                    }
                } catch (Exception e) {
                }
                Iterator it = notifs.iterator();
                while (it.hasNext()) {
                    Pair<String, Integer> v = (Pair) it.next();
                    noMan.cancelAsUser((String) v.first, 7, new UserHandle(((Integer) v.second).intValue()));
                }
            }
        });
    }

    /* access modifiers changed from: private */
    public void checkStack(int stackId, int windowingMode, int activityType, ArraySet<Pair<String, Integer>> notifs, NotificationManager noMan, IPackageManager pm) {
        try {
            ActivityManager.StackInfo info = ActivityManagerCompat.getStackInfo(stackId, windowingMode, activityType);
            int userId = ActivityManagerCompat.getUserId(info);
            if (info != null) {
                if (info.topActivity != null) {
                    String pkg = info.topActivity.getPackageName();
                    try {
                        if (!hasNotif(notifs, pkg, userId)) {
                            try {
                                String str = pkg;
                                int i = userId;
                                ApplicationInfoHelper.postEphemeralNotificationIfNeeded(this.mContext, str, i, pm.getApplicationInfo(pkg, 8192, userId), noMan, info.taskIds[info.taskIds.length - 1], this.mCurrentNotifs);
                            } catch (Exception e) {
                            }
                        } else {
                            IPackageManager iPackageManager = pm;
                        }
                    } catch (Exception e2) {
                        IPackageManager iPackageManager2 = pm;
                    }
                }
            }
            ArraySet<Pair<String, Integer>> arraySet = notifs;
            IPackageManager iPackageManager3 = pm;
        } catch (Exception e3) {
            ArraySet<Pair<String, Integer>> arraySet2 = notifs;
            IPackageManager iPackageManager22 = pm;
        }
    }

    private boolean hasNotif(ArraySet<Pair<String, Integer>> notifs, String pkg, int userId) {
        Pair<String, Integer> key = new Pair<>(pkg, Integer.valueOf(userId));
        if (!notifs.remove(key)) {
            return false;
        }
        this.mCurrentNotifs.add(key);
        return true;
    }

    public void appTransitionStarting(long startTime, long duration, boolean forced) {
        updateManagedProfile();
        updateForegroundInstantApps();
    }

    public void onKeyguardShowingChanged() {
        updateManagedProfile();
        updateForegroundInstantApps();
    }

    public void onUserSetupChanged() {
        boolean userSetup = this.mProvisionedController.isUserSetup(this.mProvisionedController.getCurrentUser());
        if (this.mCurrentUserSetup != userSetup) {
            this.mCurrentUserSetup = userSetup;
            updateQuietState();
        }
    }

    public void preloadRecentApps() {
        updateForegroundInstantApps();
    }

    public void onRotationLockStateChanged(boolean rotationLocked, boolean affordanceVisible) {
        boolean portrait = RotationLockTile.isCurrentOrientationLockPortrait(this.mRotationLockController, this.mContext);
        if (rotationLocked) {
            if (portrait) {
                this.mIconController.setIcon(this.mSlotRotate, R.drawable.stat_sys_rotate_portrait, this.mContext.getString(R.string.accessibility_rotation_lock_on_portrait));
            } else {
                this.mIconController.setIcon(this.mSlotRotate, R.drawable.stat_sys_rotate_landscape, this.mContext.getString(R.string.accessibility_rotation_lock_on_landscape));
            }
            this.mIconController.setIconVisibility(this.mSlotRotate, true);
            return;
        }
        this.mIconController.setIconVisibility(this.mSlotRotate, false);
    }

    /* access modifiers changed from: private */
    public void updateHeadsetPlug(Intent intent) {
        int i;
        int i2;
        boolean connected = intent.getIntExtra("state", 0) != 0;
        boolean hasMic = intent.getIntExtra("microphone", 0) != 0;
        Log.d("PhoneStatusBarPolicy", "intent=" + intent + "  connected=" + connected + "  hasMic=" + hasMic);
        if (connected) {
            Context context = this.mContext;
            if (hasMic) {
                i = R.string.accessibility_status_bar_headset;
            } else {
                i = R.string.accessibility_status_bar_headphones;
            }
            String contentDescription = context.getString(i);
            StatusBarIconController statusBarIconController = this.mIconController;
            String str = this.mSlotHeadset;
            if (hasMic) {
                i2 = R.drawable.stat_sys_headset;
            } else {
                i2 = R.drawable.stat_sys_headset_without_mic;
            }
            statusBarIconController.setIcon(str, i2, contentDescription);
            this.mIconController.setIconVisibility(this.mSlotHeadset, true);
            return;
        }
        this.mIconController.setIconVisibility(this.mSlotHeadset, false);
    }

    public void onDataSaverChanged(boolean isDataSaving) {
        this.mIconController.setIconVisibility(this.mSlotDataSaver, isDataSaving);
    }

    public void onDeviceProvisionedChanged() {
    }

    public void onUserSwitched() {
    }

    public void onConditionsChanged(Condition[] conditions) {
    }

    public void onNextAlarmChanged() {
    }

    public void onLocationSettingsChanged(boolean locationEnabled) {
    }

    public void onZenAvailableChanged(boolean available) {
    }

    public void onEffectsSupressorChanged() {
    }

    public void onManualRuleChanged(ZenModeConfig.ZenRule rule) {
    }

    public void onConfigChanged(ZenModeConfig config) {
    }

    public void setIcon(String slot, StatusBarIcon icon) {
    }

    public void removeIcon(String slot) {
    }

    public void disable(int state1, int state2, boolean animate) {
    }

    public void animateExpandNotificationsPanel() {
    }

    public void animateCollapsePanels(int flags) {
    }

    public void animateExpandSettingsPanel(String obj) {
    }

    public void setSystemUiVisibility(int vis, int fullscreenStackVis, int dockedStackVis, int mask, Rect fullscreenStackBounds, Rect dockedStackBounds) {
    }

    public void topAppWindowChanged(boolean visible) {
    }

    public void setImeWindowStatus(IBinder token, int vis, int backDisposition, boolean showImeSwitcher) {
    }

    public void showRecentApps(boolean triggeredFromAltTab, boolean fromHome) {
    }

    public void hideRecentApps(boolean triggeredFromAltTab, boolean triggeredFromHomeKey) {
    }

    public void toggleRecentApps() {
    }

    public void toggleSplitScreen() {
    }

    public void dismissKeyboardShortcutsMenu() {
    }

    public void toggleKeyboardShortcutsMenu(int deviceId) {
    }

    public void cancelPreloadRecentApps() {
    }

    public void setWindowState(int window, int state) {
    }

    public void showScreenPinningRequest(int taskId) {
    }

    public void appTransitionPending(boolean forced) {
    }

    public void appTransitionCancelled() {
    }

    public void appTransitionFinished() {
    }

    public void showAssistDisclosure() {
    }

    public void startAssist(Bundle args) {
    }

    public void showPictureInPictureMenu() {
    }

    public void addQsTile(ComponentName tile) {
    }

    public void remQsTile(ComponentName tile) {
    }

    public void clickTile(ComponentName tile) {
    }

    public void showFingerprintDialog(SomeArgs args) {
    }

    public void onFingerprintAuthenticated() {
    }

    public void onFingerprintHelp(String message) {
    }

    public void onFingerprintError(String error) {
    }

    public void hideFingerprintDialog() {
    }

    public void handleSystemNavigationKey(int arg1) {
    }

    public void handleShowGlobalActionsMenu() {
    }

    public void setStatus(int what, String action, Bundle ext) {
    }

    /* access modifiers changed from: private */
    public final void updateBluetoothHandsfreeBattery(Intent intent) {
        if (!intent.getBooleanExtra("android.intent.extra.show_bluetooth_handsfree_battery", true)) {
            this.mService.setIconVisibility(this.mSlotBluetoothBattery, false);
            return;
        }
        int level = intent.getIntExtra("android.intent.extra.bluetooth_handsfree_battery_level", 0);
        this.mService.setIcon(this.mSlotBluetoothBattery, R.drawable.stat_sys_bluetooth_handsfree_battery, level, this.mContext.getString(R.string.accessibility_quick_settings_bluetooth_handsfree_battery_level, new Object[]{Integer.valueOf(level * 10)}));
        this.mService.setIconVisibility(this.mSlotBluetoothBattery, true);
    }
}
