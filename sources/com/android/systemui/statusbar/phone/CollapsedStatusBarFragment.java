package com.android.systemui.statusbar.phone;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.IBinder;
import android.util.ArraySet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.android.internal.os.SomeArgs;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.systemui.BatteryMeterView;
import com.android.systemui.Constants;
import com.android.systemui.Dependency;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.SystemUI;
import com.android.systemui.miui.statusbar.phone.MiuiStatusBarPromptController;
import com.android.systemui.miui.widget.ClipEdgeLinearLayout;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.NetworkSpeedView;
import com.android.systemui.statusbar.SignalClusterView;
import com.android.systemui.statusbar.StatusBarIconView;
import com.android.systemui.statusbar.phone.StatusBarIconController;
import com.android.systemui.statusbar.policy.Clock;
import com.android.systemui.statusbar.policy.DarkIconDispatcher;
import com.android.systemui.statusbar.policy.EncryptionHelper;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.android.systemui.statusbar.policy.LocationController;
import com.android.systemui.statusbar.policy.NetworkController;
import com.miui.systemui.support.v4.app.Fragment;
import java.util.List;
import miui.telephony.SubscriptionInfo;

public class CollapsedStatusBarFragment extends Fragment implements CommandQueue.Callbacks, LocationController.LocationChangeCallback {
    public ViewGroup mClockContainer;
    private boolean mClockVisible = true;
    private CollapsedStatusBarFragmentController mController;
    public StatusBarIconController.DarkIconManager mDarkIconManager;
    private int mDisabled1;
    private int mDisabled2;
    private ImageView mGpsDriveMode;
    private StatusBarIconController.IconManager mInCallIconManager;
    private boolean mIsStatusBarPromptNormalMode = true;
    private KeyguardMonitor mKeyguardMonitor;
    private LinearLayout mLeftSideLayout;
    private LocationController mLocationController = ((LocationController) Dependency.get(LocationController.class));
    private MiuiStatusBarPromptController mMiuiStatusBarPrompt;
    private NetworkController mNetworkController;
    private NetworkSpeedView mNetworkSpeedView;
    public ClipEdgeLinearLayout mNotchLeftEarIcons;
    ArraySet<String> mNotchleftearIconsList = new ArraySet<>();
    private NotificationIconAreaController mNotificationIconAreaController;
    private View mNotificationIconAreaInner;
    /* access modifiers changed from: private */
    public boolean mShowBluetooth;
    /* access modifiers changed from: private */
    public boolean mShowLocation;
    private NetworkController.SignalCallback mSignalCallback = new NetworkController.SignalCallback() {
        public void setIsAirplaneMode(NetworkController.IconState icon) {
            CollapsedStatusBarFragment.this.mStatusBarComponent.recomputeDisableFlags(true);
        }

        public void setWifiIndicators(boolean enabled, NetworkController.IconState statusIcon, NetworkController.IconState qsIcon, boolean activityIn, boolean activityOut, String description, boolean isTransient) {
        }

        public void setMobileDataIndicators(NetworkController.IconState statusIcon, NetworkController.IconState qsIcon, int statusType, int qsType, boolean activityIn, boolean activityOut, int dataActivityId, int stackedDataIcon, int stackedVoiceIcon, String typeContentDescription, String description, boolean isWide, int slot, boolean roaming) {
        }

        public void setSubs(List<SubscriptionInfo> list) {
        }

        public void setNoSims(boolean show) {
        }

        public void setEthernetIndicators(NetworkController.IconState icon) {
        }

        public void setMobileDataEnabled(boolean enabled) {
        }

        public void setIsImsRegisted(int slot, boolean imsRegisted) {
        }

        public void setVolteNoService(int slot, boolean show) {
        }

        public void setSpeechHd(int slot, boolean hd) {
        }

        public void setVowifi(int slot, boolean vowifi) {
        }

        public void setNetworkNameVoice(int slot, String networkNameVoice) {
        }

        public void setIsDefaultDataSim(int slot, boolean isDefaultDataSim) {
        }
    };
    private SignalClusterView mSignalClusterView;
    private PhoneStatusBarView mStatusBar;
    /* access modifiers changed from: private */
    public StatusBar mStatusBarComponent;
    /* access modifiers changed from: private */
    public Clock mStatusClock;
    public LinearLayout mStatusIcons;
    private LinearLayout mSystemIconArea;

    interface HideAnimateCallback {
        void callOnEnd();
    }

    public class LeftEarIconManager extends StatusBarIconController.DarkIconManager {
        public LeftEarIconManager(LinearLayout linearLayout) {
            super(linearLayout);
        }

        /* access modifiers changed from: protected */
        public void onIconAdded(int index, String slot, boolean blocked, StatusBarIcon icon) {
            super.onIconAdded(index, slot, blocked, icon);
            updateIcons(index, icon);
        }

        public void onSetIcon(int viewIndex, String slot, StatusBarIcon icon) {
            super.onSetIcon(viewIndex, slot, icon);
            updateIcons(viewIndex, icon);
        }

        /* access modifiers changed from: protected */
        public void onRemoveIcon(int viewIndex, String slot) {
            updateIcons(viewIndex, null);
            super.onRemoveIcon(viewIndex, slot);
        }

        private void updateIcons(int viewIndex, StatusBarIcon icon) {
            StatusBarIconView view = (StatusBarIconView) CollapsedStatusBarFragment.this.mNotchLeftEarIcons.getChildAt(viewIndex);
            if (view != null) {
                int iconId = icon == null ? 0 : icon.icon.getResId();
                String slot = view.getSlot();
                if (CollapsedStatusBarFragment.this.mNotchleftearIconsList.contains(slot)) {
                    if (slot.equals("bluetooth")) {
                        if (icon == null) {
                            boolean unused = CollapsedStatusBarFragment.this.mShowBluetooth = false;
                        } else if (!Constants.IS_NARROW_NOTCH) {
                            boolean show = iconId == R.drawable.stat_sys_data_bluetooth_in || iconId == R.drawable.stat_sys_data_bluetooth_out || iconId == R.drawable.stat_sys_data_bluetooth_inout;
                            view.setVisibility(show ? 0 : 8);
                            boolean unused2 = CollapsedStatusBarFragment.this.mShowBluetooth = show;
                        } else {
                            boolean unused3 = CollapsedStatusBarFragment.this.mShowBluetooth = view.getVisibility() == 0;
                        }
                    } else if (slot.equals("location")) {
                        boolean unused4 = CollapsedStatusBarFragment.this.mShowLocation = icon != null && view.getVisibility() == 0;
                    }
                    if ((CollapsedStatusBarFragment.this.mShowBluetooth || CollapsedStatusBarFragment.this.mShowLocation) && !Constants.IS_NARROW_NOTCH) {
                        if (!CollapsedStatusBarFragment.this.mStatusClock.mForceHideAmPm) {
                            CollapsedStatusBarFragment.this.mStatusClock.mForceHideAmPm = true;
                            CollapsedStatusBarFragment.this.mStatusClock.update();
                        }
                    } else if (CollapsedStatusBarFragment.this.mStatusClock.mForceHideAmPm) {
                        CollapsedStatusBarFragment.this.mStatusClock.mForceHideAmPm = false;
                        CollapsedStatusBarFragment.this.mStatusClock.update();
                    }
                }
            }
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mKeyguardMonitor = (KeyguardMonitor) Dependency.get(KeyguardMonitor.class);
        this.mNetworkController = (NetworkController) Dependency.get(NetworkController.class);
        this.mStatusBarComponent = (StatusBar) SystemUI.getComponent(getContext(), StatusBar.class);
        this.mMiuiStatusBarPrompt = (MiuiStatusBarPromptController) Dependency.get(MiuiStatusBarPromptController.class);
        this.mController = StatusBarFactory.getInstance().getCollapsedStatusBarFragmentController();
        this.mController.init(this);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.status_bar, container, false);
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.mStatusBar = (PhoneStatusBarView) view;
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey("panel_state")) {
                this.mStatusBar.go(savedInstanceState.getInt("panel_state"));
            }
            if (savedInstanceState.containsKey("clock_visible")) {
                this.mClockVisible = savedInstanceState.getBoolean("clock_visible");
            }
            if (savedInstanceState.containsKey("status_bar_prompt_normal")) {
                this.mIsStatusBarPromptNormalMode = savedInstanceState.getBoolean("status_bar_prompt_normal");
            }
            if (this.mMiuiStatusBarPrompt.getStatusBarModeState() == 4) {
                this.mMiuiStatusBarPrompt.forceRefreshRecorder();
            }
        }
        this.mClockContainer = (ViewGroup) this.mStatusBar.findViewById(R.id.clock_container);
        this.mLeftSideLayout = (LinearLayout) this.mStatusBar.findViewById(R.id.leftside);
        this.mStatusIcons = (LinearLayout) this.mStatusBar.findViewById(R.id.statusIcons);
        this.mSystemIconArea = (LinearLayout) this.mStatusBar.findViewById(R.id.system_icon_area);
        this.mSignalClusterView = (SignalClusterView) this.mStatusBar.findViewById(R.id.signal_cluster);
        this.mNetworkSpeedView = (NetworkSpeedView) this.mSystemIconArea.findViewById(R.id.network_speed_view);
        this.mStatusClock = (Clock) this.mStatusBar.findViewById(R.id.clock);
        this.mStatusClock.setVisibility(this.mClockVisible ? 0 : 8);
        this.mGpsDriveMode = (ImageView) this.mStatusBar.findViewById(R.id.gps_drivemode);
        if (!Constants.IS_NOTCH || Constants.IS_NARROW_NOTCH) {
            this.mDarkIconManager = new StatusBarIconController.DarkIconManager(this.mStatusIcons);
            if (Constants.IS_NARROW_NOTCH) {
                this.mDarkIconManager.mWhiteList = new ArraySet();
                this.mDarkIconManager.mWhiteList.add("volume");
                this.mDarkIconManager.mWhiteList.add("quiet");
                this.mDarkIconManager.mWhiteList.add("alarm_clock");
            }
            ((StatusBarIconController) Dependency.get(StatusBarIconController.class)).addIconGroup(this.mDarkIconManager);
        }
        if (!Constants.IS_NOTCH) {
            ArraySet<String> inCallList = new ArraySet<>();
            inCallList.add("call_record");
            inCallList.add("mute");
            inCallList.add("speakerphone");
            this.mInCallIconManager = new StatusBarIconController.IconManager((LinearLayout) this.mStatusBar.findViewById(R.id.call_icons));
            this.mInCallIconManager.mWhiteList = new ArraySet<>();
            this.mInCallIconManager.mWhiteList.addAll(inCallList);
            ((StatusBarIconController) Dependency.get(StatusBarIconController.class)).addIconGroup(this.mInCallIconManager);
        }
        if (Constants.IS_NOTCH) {
            updateNotchPromptViewLayout(this.mLeftSideLayout);
            this.mNotchLeftEarIcons = (ClipEdgeLinearLayout) this.mStatusBar.findViewById(R.id.notch_leftear_icons);
            this.mSignalClusterView.setNotchEar();
        }
        this.mController.start(this.mStatusBar);
        ((DarkIconDispatcher) Dependency.get(DarkIconDispatcher.class)).addDarkReceiver(this.mSignalClusterView);
        ((DarkIconDispatcher) Dependency.get(DarkIconDispatcher.class)).addDarkReceiver(this.mNetworkSpeedView);
        ((DarkIconDispatcher) Dependency.get(DarkIconDispatcher.class)).addDarkReceiver(this.mStatusClock);
        showSystemIconArea(false);
        initEmergencyCryptkeeperText();
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("panel_state", this.mStatusBar.getState());
        outState.putBoolean("clock_visible", this.mClockVisible);
        outState.putBoolean("status_bar_prompt_normal", this.mIsStatusBarPromptNormalMode);
    }

    public void onResume() {
        super.onResume();
        ((CommandQueue) SystemUI.getComponent(getContext(), CommandQueue.class)).addCallbacks(this);
    }

    public void onPause() {
        super.onPause();
        ((CommandQueue) SystemUI.getComponent(getContext(), CommandQueue.class)).removeCallbacks(this);
    }

    public void onDestroyView() {
        super.onDestroyView();
        ((DarkIconDispatcher) Dependency.get(DarkIconDispatcher.class)).removeDarkReceiver(this.mSignalClusterView);
        this.mController.stop();
        if (!Constants.IS_NOTCH) {
            ((StatusBarIconController) Dependency.get(StatusBarIconController.class)).removeIconGroup(this.mDarkIconManager);
            ((StatusBarIconController) Dependency.get(StatusBarIconController.class)).removeIconGroup(this.mInCallIconManager);
        }
        ((DarkIconDispatcher) Dependency.get(DarkIconDispatcher.class)).removeDarkReceiver(this.mStatusClock);
        ((DarkIconDispatcher) Dependency.get(DarkIconDispatcher.class)).removeDarkReceiver(this.mNetworkSpeedView);
        if (this.mNetworkController.hasEmergencyCryptKeeperText()) {
            this.mNetworkController.removeCallback(this.mSignalCallback);
        }
    }

    public void initNotificationIconArea(NotificationIconAreaController notificationIconAreaController) {
        ViewGroup notificationIconArea = (ViewGroup) this.mStatusBar.findViewById(R.id.notification_icon_area);
        this.mNotificationIconAreaInner = notificationIconAreaController.getNotificationInnerAreaView();
        this.mNotificationIconAreaController = notificationIconAreaController;
        if (this.mNotificationIconAreaInner.getParent() != null) {
            ((ViewGroup) this.mNotificationIconAreaInner.getParent()).removeView(this.mNotificationIconAreaInner);
        }
        notificationIconArea.addView(this.mNotificationIconAreaInner);
        showNotificationIconArea(false);
        if (!Constants.IS_NOTCH) {
            notificationIconAreaController.setMoreIcon((StatusBarIconView) this.mStatusBar.findViewById(R.id.moreIcon));
        }
        notificationIconAreaController.setupClockContainer(this.mClockContainer);
    }

    public void disable(int state1, int state2, boolean animate) {
        int state12 = adjustDisableFlags(state1);
        int diff1 = state12 ^ this.mDisabled1;
        this.mDisabled1 = state12;
        this.mDisabled2 = state2;
        boolean z = false;
        boolean hasDisableSystemInfo = (diff1 & 1048576) != 0;
        boolean hasDisableNotificationIcons = (diff1 & 131072) != 0;
        if (hasDisableSystemInfo) {
            if ((1048576 & state12) != 0) {
                hideSystemIconArea(animate);
            } else {
                showSystemIconArea(animate);
            }
        }
        if (hasDisableNotificationIcons) {
            if ((state12 & 131072) != 0) {
                hideNotificationIconArea(animate);
            } else {
                showNotificationIconArea(animate);
            }
        }
        boolean z2 = this.mIsStatusBarPromptNormalMode;
        if (!hasDisableSystemInfo && !hasDisableNotificationIcons) {
            z = true;
        }
        refreshClockVisibility(animate, z2, z);
    }

    /* access modifiers changed from: protected */
    public int adjustDisableFlags(int state) {
        if (this.mStatusBarComponent.shouldHideNotificationIcons()) {
            state |= 131072;
        }
        if (!this.mStatusBarComponent.isLaunchTransitionFadingAway() && !this.mKeyguardMonitor.isKeyguardFadingAway() && shouldHideNotificationIcons()) {
            state = state | 131072 | 1048576 | 8388608;
        }
        if (this.mNetworkController == null || !EncryptionHelper.IS_DATA_ENCRYPTED) {
            return state;
        }
        if (this.mNetworkController.hasEmergencyCryptKeeperText()) {
            state |= 131072;
        }
        if (!this.mNetworkController.isRadioOn()) {
            return state | 1048576;
        }
        return state;
    }

    private boolean shouldHideNotificationIcons() {
        return !this.mStatusBar.isClosed() && this.mStatusBarComponent.hideStatusBarIconsWhenExpanded();
    }

    public void hideSystemIconArea(boolean animate) {
        animateHide(this.mSystemIconArea, animate, true);
        this.mController.hideSystemIconArea(animate, true);
    }

    public void showSystemIconArea(boolean animate) {
        animateShow(this.mSystemIconArea, animate);
        this.mController.showSystemIconArea(animate);
    }

    public void hideNotificationIconArea(boolean animate) {
        animateHide(this.mNotificationIconAreaInner, animate, true);
    }

    public void showNotificationIconArea(boolean animate) {
        animateShow(this.mNotificationIconAreaInner, animate);
    }

    /* access modifiers changed from: protected */
    public void animateHide(View v, boolean animate, boolean isHoldPlace) {
        animateHideWithCallback(v, animate, isHoldPlace, null);
    }

    /* access modifiers changed from: package-private */
    public void animateHideWithCallback(final View v, boolean animate, boolean isHoldPlace, final HideAnimateCallback callback) {
        final int visible = isHoldPlace ? 4 : 8;
        v.animate().cancel();
        if (!animate) {
            v.setAlpha(0.0f);
            v.setVisibility(visible);
            return;
        }
        v.animate().alpha(0.0f).setDuration(160).setStartDelay(0).setInterpolator(Interpolators.ALPHA_OUT).withEndAction(new Runnable() {
            public void run() {
                v.setVisibility(visible);
                if (callback != null) {
                    callback.callOnEnd();
                }
            }
        });
    }

    /* access modifiers changed from: protected */
    public void animateShow(View v, boolean animate) {
        v.animate().cancel();
        v.setVisibility(0);
        if (!animate) {
            v.setAlpha(1.0f);
            return;
        }
        v.animate().alpha(1.0f).setDuration(320).setInterpolator(Interpolators.ALPHA_IN).setStartDelay(50).withEndAction(null);
        if (this.mKeyguardMonitor.isKeyguardFadingAway()) {
            v.animate().setDuration(this.mKeyguardMonitor.getKeyguardFadingAwayDuration()).setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN).setStartDelay(this.mKeyguardMonitor.getKeyguardFadingAwayDelay()).start();
        }
    }

    private void initEmergencyCryptkeeperText() {
        View emergencyViewStub = this.mStatusBar.findViewById(R.id.emergency_cryptkeeper_text);
        if (this.mNetworkController.hasEmergencyCryptKeeperText()) {
            this.mStatusClock.setVisibility(8);
            if (emergencyViewStub != null) {
                ((ViewStub) emergencyViewStub).inflate();
            }
            this.mNetworkController.addCallback(this.mSignalCallback);
        } else if (emergencyViewStub != null) {
            ((ViewGroup) emergencyViewStub.getParent()).removeView(emergencyViewStub);
        }
    }

    public void onLocationActiveChanged(boolean active) {
        if (this.mStatusIcons.getVisibility() != 8) {
            return;
        }
        if (this.mLocationController.isLocationActive()) {
            this.mGpsDriveMode.setVisibility(0);
        } else {
            this.mGpsDriveMode.setVisibility(8);
        }
    }

    public void onLocationSettingsChanged(boolean locationEnabled) {
    }

    public void onLocationStatusChanged(Intent intent) {
    }

    public void setIcon(String slot, StatusBarIcon icon) {
    }

    public void removeIcon(String slot) {
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

    public void preloadRecentApps() {
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

    public void appTransitionStarting(long startTime, long duration, boolean forced) {
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

    public void updateInDriveMode(boolean isInDriveMode) {
        if (!Constants.IS_NOTCH) {
            this.mNetworkSpeedView.setDriveMode(isInDriveMode);
        }
        int i = 0;
        if (!isInDriveMode || Constants.IS_NOTCH) {
            this.mLocationController.removeCallback(this);
            if (!this.mStatusBarComponent.mDemoMode) {
                boolean hideSystemIcon = (this.mDisabled2 & 2) != 0;
                LinearLayout linearLayout = this.mStatusIcons;
                if ((Constants.IS_NOTCH && this.mController.isStatusIconsVisible()) || hideSystemIcon) {
                    i = 8;
                }
                linearLayout.setVisibility(i);
            }
            this.mGpsDriveMode.setVisibility(8);
        } else {
            this.mLocationController.addCallback(this);
            this.mStatusIcons.setVisibility(8);
            ImageView imageView = this.mGpsDriveMode;
            if (!this.mDarkIconManager.hasView("location")) {
                i = 8;
            }
            imageView.setVisibility(i);
        }
        this.mNotificationIconAreaController.setForceHideMoreIcon(isInDriveMode);
    }

    public void refreshClockVisibility(boolean animate, boolean isNormalMode, boolean isOnlyClock) {
        this.mIsStatusBarPromptNormalMode = isNormalMode;
        boolean z = true;
        int i = 0;
        boolean visible = (this.mDisabled1 & 8388608) == 0 && (!Constants.IS_NOTCH || isNormalMode);
        if (this.mClockVisible != visible) {
            this.mClockVisible = visible;
            CollapsedStatusBarFragmentController collapsedStatusBarFragmentController = this.mController;
            if (!visible || this.mNetworkController.hasEmergencyCryptKeeperText()) {
                z = false;
            }
            collapsedStatusBarFragmentController.updateLeftPartVisibility(z, isOnlyClock);
        }
        if (Constants.IS_NOTCH) {
            ClipEdgeLinearLayout clipEdgeLinearLayout = this.mNotchLeftEarIcons;
            if (!isNormalMode) {
                i = 8;
            }
            clipEdgeLinearLayout.setVisibility(i);
        }
    }

    public void clockVisibleAnimate(boolean visible, boolean animate) {
        View v = this.mStatusClock;
        if (visible) {
            animateShow(v, animate);
        } else {
            animateHide(v, animate, false);
        }
    }

    public void setNotch() {
        ((BatteryMeterView) this.mStatusBar.findViewById(R.id.battery)).setNortchEar(true);
        this.mNetworkSpeedView.setNotch();
        if (this.mController.isStatusIconsVisible()) {
            this.mStatusIcons.setVisibility(8);
        }
    }

    private void updateNotchPromptViewLayout(View viewGroup) {
        if (viewGroup != null) {
            boolean center = false;
            if (this.mController.isStatusIconsVisible()) {
                center = true;
            }
            FrameLayout.LayoutParams mlp = (FrameLayout.LayoutParams) viewGroup.getLayoutParams();
            if ((mlp.gravity == 17) != center) {
                if (center) {
                    mlp.gravity = 17;
                } else {
                    mlp.gravity = 8388627;
                }
                viewGroup.setLayoutParams(mlp);
            }
        }
    }
}
