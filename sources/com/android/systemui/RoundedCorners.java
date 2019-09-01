package com.android.systemui;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManagerCompat;
import android.view.WindowManagerGlobal;
import android.widget.ImageView;
import com.android.internal.os.SomeArgs;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.systemui.OverlayManagerWrapper;
import com.android.systemui.qs.SecureSetting;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.policy.ConfigurationController;
import miui.util.CustomizeUtil;
import miui.util.FeatureParser;

public class RoundedCorners extends SystemUI implements CommandQueue.Callbacks, ConfigurationController.ConfigurationListener {
    private static boolean sIsRoundCorner;
    RoundCornerData[] mBottomCorner = {new RoundCornerData(80, -1, -2, R.drawable.screen_round_corner_bottom), new RoundCornerData(5, -2, -1, R.drawable.screen_round_corner_bottom_rot90), new RoundCornerData(48, -1, -2, R.drawable.screen_round_corner_bottom_rot180), new RoundCornerData(3, -2, -1, R.drawable.screen_round_corner_bottom_rot270)};
    /* access modifiers changed from: private */
    public Point mCurrentSize;
    protected int mCurrentUserId = 0;
    /* access modifiers changed from: private */
    public Display mDisplay;
    /* access modifiers changed from: private */
    public boolean mDriveMode;
    /* access modifiers changed from: private */
    public ContentObserver mDriveModeObserver;
    private boolean mEnableNotchConfig;
    /* access modifiers changed from: private */
    public boolean mForceBlack = false;
    private ContentObserver mForceBlackObserver;
    RoundCornerData[] mForceBlackTopCorner = {new RoundCornerData(48, -1, -2, R.drawable.screen_round_corner_bottom_rot180), new RoundCornerData(3, -2, -1, R.drawable.screen_round_corner_bottom_rot270), new RoundCornerData(80, -1, -2, R.drawable.screen_round_corner_bottom), new RoundCornerData(5, -2, -1, R.drawable.screen_round_corner_bottom_rot90)};
    /* access modifiers changed from: private */
    public boolean mForceBlackV2 = false;
    /* access modifiers changed from: private */
    public ContentObserver mForceBlackV2Observer;
    /* access modifiers changed from: private */
    public Handler mHandler;
    /* access modifiers changed from: private */
    public boolean mHandyMode;
    /* access modifiers changed from: private */
    public ImageView mHideNotchRoundCornerView;
    private Point mInitialSize;
    RoundCornerData[] mNotchCorner = {new RoundCornerData(48, -1, -2, R.drawable.screen_round_corner_notch), new RoundCornerData(3, -2, -1, R.drawable.screen_round_corner_notch_rot90), new RoundCornerData(80, -1, -2, R.drawable.screen_round_corner_notch_rot180), new RoundCornerData(5, -2, -1, R.drawable.screen_round_corner_notch_rot270)};
    /* access modifiers changed from: private */
    public ImageView mNotchRoundCornerView;
    /* access modifiers changed from: private */
    public OverlayManagerWrapper mOverlayManager;
    /* access modifiers changed from: private */
    public int mRoundCornerRotation;
    /* access modifiers changed from: private */
    public ImageView mRoundCornerViewBottom;
    /* access modifiers changed from: private */
    public ImageView mRoundCornerViewTop;
    /* access modifiers changed from: private */
    public SecureSetting mSettings;
    RoundCornerData[] mTopCorner = {new RoundCornerData(48, -1, -2, R.drawable.screen_round_corner_top), new RoundCornerData(3, -2, -1, R.drawable.screen_round_corner_top_rot90), new RoundCornerData(80, -1, -2, R.drawable.screen_round_corner_top_rot180), new RoundCornerData(5, -2, -1, R.drawable.screen_round_corner_top_rot270)};
    Runnable mUpdateRoundCornerRunnable = new Runnable() {
        public void run() {
            RoundCornerData roundCornerData;
            RoundedCorners.this.mDisplay.getRealSize(RoundedCorners.this.mCurrentSize);
            RoundedCorners roundedCorners = RoundedCorners.this;
            ImageView access$1400 = RoundedCorners.this.mRoundCornerViewTop;
            if (RoundedCorners.this.mForceBlackV2) {
                roundCornerData = RoundedCorners.this.mForceBlackTopCorner[RoundedCorners.this.mRoundCornerRotation];
            } else {
                roundCornerData = RoundedCorners.this.mTopCorner[RoundedCorners.this.mRoundCornerRotation];
            }
            roundedCorners.updateRoundCornerViewAt(access$1400, roundCornerData, false);
            RoundedCorners.this.updateRoundCornerViewAt(RoundedCorners.this.mRoundCornerViewBottom, RoundedCorners.this.mBottomCorner[RoundedCorners.this.mRoundCornerRotation], false);
            RoundedCorners.this.updateRoundCornerViewAt(RoundedCorners.this.mHideNotchRoundCornerView, RoundedCorners.this.mForceBlackTopCorner[RoundedCorners.this.mRoundCornerRotation], false);
            RoundedCorners.this.updateRoundCornerViewAt(RoundedCorners.this.mNotchRoundCornerView, RoundedCorners.this.mNotchCorner[RoundedCorners.this.mRoundCornerRotation], true);
            RoundedCorners.this.updateNotchRoundCornerVisibility();
            if (RoundedCorners.this.mSettings != null) {
                RoundedCorners.this.handleStateChange(RoundedCorners.this.mSettings.getValue());
            }
        }
    };

    static class RoundCornerData {
        int backgroundRes;
        int gravity;
        int height;
        int width;

        public RoundCornerData(int gravity2, int width2, int height2, int backgroundRes2) {
            this.gravity = gravity2;
            this.width = width2;
            this.height = height2;
            this.backgroundRes = backgroundRes2;
        }
    }

    public RoundedCorners() {
    }

    static {
        boolean z = true;
        if (!SystemProperties.getBoolean("sys.miui.show_round_corner", true) || !FeatureParser.getBoolean("support_round_corner", false)) {
            z = false;
        }
        sIsRoundCorner = z;
    }

    /* access modifiers changed from: private */
    public boolean isOverlay(int userId) {
        OverlayManagerWrapper.OverlayInfo info = null;
        try {
            info = this.mOverlayManager.getOverlayInfo("com.android.systemui.notch.overlay", userId);
        } catch (Exception e) {
            Log.w("RoundedCorners", "Can't get overlay info for user " + userId, e);
        }
        return info != null && info.isEnabled();
    }

    public void start() {
        if (Build.VERSION.SDK_INT >= 28) {
            MiuiSettings.Global.putBoolean(this.mContext.getContentResolver(), "force_black", false);
        }
        this.mCurrentUserId = ActivityManager.getCurrentUser();
        this.mDisplay = ((WindowManager) this.mContext.getSystemService("window")).getDefaultDisplay();
        this.mInitialSize = new Point();
        try {
            WindowManagerGlobal.getWindowManagerService().getInitialDisplaySize(0, this.mInitialSize);
        } catch (RemoteException e) {
            Log.w("RoundedCorners", "Unable to get the display size:" + e);
        }
        this.mCurrentSize = new Point();
        this.mDisplay.getRealSize(this.mCurrentSize);
        this.mHandler = (Handler) Dependency.get(Dependency.MAIN_HANDLER);
        initRoundCornerWindows();
        if (CustomizeUtil.HAS_NOTCH) {
            ((CommandQueue) SystemUI.getComponent(this.mContext, CommandQueue.class)).addCallbacks(this);
            this.mForceBlackObserver = new ContentObserver(this.mHandler) {
                public void onChange(boolean selfChange) {
                    boolean unused = RoundedCorners.this.mForceBlack = MiuiSettings.Global.getBoolean(RoundedCorners.this.mContext.getContentResolver(), "force_black");
                    RoundedCorners.this.updateNotchRoundCornerVisibility();
                }
            };
            this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("force_black"), false, this.mForceBlackObserver, -1);
            this.mForceBlackObserver.onChange(false);
            if (supportForceBlackV2()) {
                this.mOverlayManager = new OverlayManagerWrapper();
                try {
                    this.mOverlayManager.setEnabled("com.android.systemui.notch.overlay", false, this.mCurrentUserId);
                } catch (Exception e2) {
                    Log.w("RoundedCorners", "Can't apply overlay for user " + this.mCurrentUserId, e2);
                }
                if (this.mCurrentUserId != 0) {
                    try {
                        this.mOverlayManager.setEnabled("com.android.systemui.notch.overlay", false, 0);
                    } catch (Exception e3) {
                        Log.w("RoundedCorners", "Can't apply overlay for user owner ", e3);
                    }
                }
                this.mForceBlackV2Observer = new ContentObserver(this.mHandler) {
                    public void onChange(boolean selfChange) {
                        boolean unused = RoundedCorners.this.mForceBlackV2 = MiuiSettings.Global.getBoolean(RoundedCorners.this.mContext.getContentResolver(), "force_black_v2");
                        RoundedCorners.this.mHandler.removeCallbacks(RoundedCorners.this.mUpdateRoundCornerRunnable);
                        Message msg = Message.obtain(RoundedCorners.this.mHandler, RoundedCorners.this.mUpdateRoundCornerRunnable);
                        msg.setAsynchronous(true);
                        RoundedCorners.this.mHandler.sendMessageAtFrontOfQueue(msg);
                        if (CustomizeUtil.HAS_NOTCH && RoundedCorners.this.mOverlayManager != null) {
                            boolean isOverlay = RoundedCorners.this.isOverlay(RoundedCorners.this.mCurrentUserId);
                            boolean needOverlay = RoundedCorners.this.mForceBlackV2;
                            if (needOverlay != isOverlay) {
                                try {
                                    RoundedCorners.this.mOverlayManager.setEnabled("com.android.systemui.notch.overlay", needOverlay, RoundedCorners.this.mCurrentUserId);
                                } catch (Exception e) {
                                    Log.w("RoundedCorners", "Can't apply overlay for user " + RoundedCorners.this.mCurrentUserId, e);
                                }
                            }
                            if (RoundedCorners.this.mCurrentUserId != 0 && needOverlay != RoundedCorners.this.isOverlay(0)) {
                                try {
                                    RoundedCorners.this.mOverlayManager.setEnabled("com.android.systemui.notch.overlay", needOverlay, 0);
                                } catch (Exception e2) {
                                    Log.w("RoundedCorners", "Can't apply overlay for user owner", e2);
                                }
                            }
                        }
                    }
                };
                this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("force_black_v2"), false, this.mForceBlackV2Observer, -1);
                this.mForceBlackV2Observer.onChange(false);
            }
            this.mDriveModeObserver = new ContentObserver(this.mHandler) {
                public void onChange(boolean selfChange) {
                    RoundedCorners roundedCorners = RoundedCorners.this;
                    boolean z = true;
                    if (Settings.System.getIntForUser(RoundedCorners.this.mContext.getContentResolver(), "drive_mode_drive_mode", 0, -2) != 1) {
                        z = false;
                    }
                    boolean unused = roundedCorners.mDriveMode = z;
                    RoundedCorners.this.updateNotchRoundCornerVisibility();
                }
            };
            this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("drive_mode_drive_mode"), false, this.mDriveModeObserver, -1);
            this.mDriveModeObserver.onChange(false);
        }
        if (CustomizeUtil.HAS_NOTCH || miui.os.Build.DEVICE.equals("perseus")) {
            ((ConfigurationController) Dependency.get(ConfigurationController.class)).addCallback(this);
        }
    }

    private boolean supportForceBlackV2() {
        return Build.VERSION.SDK_INT >= 28;
    }

    private void initRoundCornerWindows() {
        if (sIsRoundCorner || CustomizeUtil.HAS_NOTCH) {
            if (sIsRoundCorner) {
                this.mRoundCornerViewTop = showRoundCornerViewAt(51, R.drawable.screen_round_corner_top);
                this.mRoundCornerViewBottom = showRoundCornerViewAt(83, R.drawable.screen_round_corner_bottom);
            }
            if (CustomizeUtil.HAS_NOTCH) {
                if (Build.VERSION.SDK_INT < 28) {
                    this.mNotchRoundCornerView = showRoundCornerViewAt(51, R.drawable.screen_round_corner_notch, true);
                    updateNotchRoundCornerVisibility();
                } else if (this.mRoundCornerViewTop == null) {
                    this.mHideNotchRoundCornerView = showRoundCornerViewAt(51, R.drawable.screen_round_corner_bottom_rot180);
                    updateNotchRoundCornerVisibility();
                }
                this.mRoundCornerRotation = this.mDisplay.getRotation();
                ((DisplayManager) this.mContext.getSystemService("display")).registerDisplayListener(new DisplayManager.DisplayListener() {
                    public void onDisplayRemoved(int displayId) {
                    }

                    public void onDisplayChanged(int displayId) {
                        int rotation = RoundedCorners.this.mDisplay.getRotation();
                        if (RoundedCorners.this.mRoundCornerRotation != rotation) {
                            int unused = RoundedCorners.this.mRoundCornerRotation = rotation;
                            Message msg = Message.obtain(RoundedCorners.this.mHandler, RoundedCorners.this.mUpdateRoundCornerRunnable);
                            msg.setAsynchronous(true);
                            RoundedCorners.this.mHandler.sendMessageAtFrontOfQueue(msg);
                        }
                    }

                    public void onDisplayAdded(int displayId) {
                    }
                }, this.mHandler);
            }
            this.mSettings = new SecureSetting(this.mContext, this.mHandler, "accessibility_display_inversion_enabled") {
                /* access modifiers changed from: protected */
                public void handleValueChanged(int value, boolean observedChange) {
                    RoundedCorners.this.handleStateChange(value);
                }
            };
            this.mSettings.setListening(true);
            this.mSettings.onChange(false);
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.USER_SWITCHED");
            filter.addAction("miui.action.handymode_change");
            this.mContext.registerReceiver(new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    int i = 0;
                    if ("android.intent.action.USER_SWITCHED".equals(action)) {
                        RoundedCorners.this.mCurrentUserId = intent.getIntExtra("android.intent.extra.user_handle", -1);
                        RoundedCorners.this.mSettings.setUserId(ActivityManager.getCurrentUser());
                        RoundedCorners.this.handleStateChange(RoundedCorners.this.mSettings.getValue());
                        if (RoundedCorners.this.mDriveModeObserver != null) {
                            RoundedCorners.this.mDriveModeObserver.onChange(false);
                        }
                        if (RoundedCorners.this.mForceBlackV2Observer != null) {
                            RoundedCorners.this.mForceBlackV2Observer.onChange(false);
                        }
                    } else if ("miui.action.handymode_change".equals(action)) {
                        boolean unused = RoundedCorners.this.mHandyMode = intent.getIntExtra("handymode", 0) != 0;
                        if (RoundedCorners.this.mRoundCornerViewTop != null) {
                            RoundedCorners.this.mRoundCornerViewTop.setVisibility(RoundedCorners.this.mHandyMode ? 8 : 0);
                        }
                        if (RoundedCorners.this.mRoundCornerViewBottom != null) {
                            ImageView access$1500 = RoundedCorners.this.mRoundCornerViewBottom;
                            if (RoundedCorners.this.mHandyMode) {
                                i = 8;
                            }
                            access$1500.setVisibility(i);
                        }
                    }
                }
            }, filter, null, this.mHandler);
        }
    }

    /* access modifiers changed from: private */
    public void handleStateChange(int value) {
        PorterDuffColorFilter porterDuffColorFilter;
        if (this.mRoundCornerViewTop != null && this.mRoundCornerViewBottom != null) {
            Drawable background = this.mRoundCornerViewTop.getBackground();
            PorterDuffColorFilter porterDuffColorFilter2 = null;
            if (value != 0) {
                porterDuffColorFilter = new PorterDuffColorFilter(-1, PorterDuff.Mode.SRC_ATOP);
            } else {
                porterDuffColorFilter = null;
            }
            background.setColorFilter(porterDuffColorFilter);
            Drawable background2 = this.mRoundCornerViewBottom.getBackground();
            if (value != 0) {
                porterDuffColorFilter2 = new PorterDuffColorFilter(-1, PorterDuff.Mode.SRC_ATOP);
            }
            background2.setColorFilter(porterDuffColorFilter2);
        }
    }

    /* access modifiers changed from: private */
    public void updateRoundCornerViewAt(View view, RoundCornerData data, boolean notch) {
        if (view != null) {
            WindowManager.LayoutParams lp = (WindowManager.LayoutParams) view.getLayoutParams();
            lp.gravity = data.gravity;
            lp.width = data.width;
            lp.height = data.height;
            setBackgroundResource(view, data.backgroundRes, notch);
            ((WindowManager) this.mContext.getSystemService("window")).updateViewLayout(view, lp);
        }
    }

    private ImageView showRoundCornerViewAt(int gravity, int roundCornerImgResId) {
        return showRoundCornerViewAt(gravity, roundCornerImgResId, false);
    }

    private ImageView showRoundCornerViewAt(int gravity, int roundCornerImgResId, boolean notch) {
        WindowManager wm = (WindowManager) this.mContext.getSystemService("window");
        ImageView view = new ImageView(this.mContext);
        setBackgroundResource(view, roundCornerImgResId, notch);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(-1, -2, notch ? 2017 : 2015, 1304, -3);
        lp.privateFlags = 1048592;
        lp.privateFlags |= 64;
        lp.gravity = gravity;
        lp.setTitle("RoundCorner");
        WindowManagerCompat.setLayoutInDisplayCutoutMode(lp, 1);
        wm.addView(view, lp);
        return view;
    }

    private void setBackgroundResource(View view, int res, boolean notch) {
        if (notch) {
            view.setBackgroundResource(res);
            return;
        }
        int initWidth = Math.min(this.mInitialSize.x, this.mInitialSize.y);
        int currentWidth = Math.min(this.mCurrentSize.x, this.mCurrentSize.y);
        TypedValue value = new TypedValue();
        this.mContext.getResources().getValue(res, value, true);
        view.setBackground(this.mContext.getResources().getDrawableForDensity(res, (value.density * currentWidth) / initWidth));
    }

    /* access modifiers changed from: private */
    public void updateNotchRoundCornerVisibility() {
        int i = 8;
        if (this.mNotchRoundCornerView != null) {
            this.mNotchRoundCornerView.setVisibility(((this.mRoundCornerRotation == 0 || !this.mEnableNotchConfig) && !this.mDriveMode && this.mForceBlack) ? 0 : 8);
        }
        if (this.mHideNotchRoundCornerView != null) {
            ImageView imageView = this.mHideNotchRoundCornerView;
            if (this.mForceBlackV2) {
                i = 0;
            }
            imageView.setVisibility(i);
        }
    }

    public void setStatus(int what, String action, Bundle ext) {
        if (CustomizeUtil.HAS_NOTCH && "upate_specail_mode".equals(action)) {
            this.mEnableNotchConfig = ext.getBoolean("enable_config");
            updateNotchRoundCornerVisibility();
        }
    }

    public void onConfigChanged(Configuration newConfig) {
    }

    public void onDensityOrFontScaleChanged() {
        if (this.mNotchRoundCornerView != null) {
            this.mNotchRoundCornerView.setBackgroundResource(0);
        }
        Message msg = Message.obtain(this.mHandler, this.mUpdateRoundCornerRunnable);
        msg.setAsynchronous(true);
        this.mHandler.sendMessageAtFrontOfQueue(msg);
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
}
