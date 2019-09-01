package com.android.systemui.statusbar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.keyguard.Ease;
import com.android.keyguard.KeyguardClockContainer;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.keyguard.MiuiKeyguardUtils;
import com.android.keyguard.analytics.AnalyticsHelper;
import com.android.keyguard.analytics.LockScreenMagazineAnalytics;
import com.android.keyguard.magazine.LockScreenMagazinePreView;
import com.android.keyguard.magazine.LockScreenMagazineUtils;
import com.android.keyguard.magazine.mode.LockScreenMagazineWallpaperInfo;
import com.android.keyguard.utils.PackageUtils;
import com.android.keyguard.wallpaper.WallpaperAuthorityUtils;
import com.android.systemui.R;
import com.android.systemui.statusbar.phone.KeyguardBottomAreaView;
import com.android.systemui.statusbar.phone.NotificationPanelView;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager;
import com.android.systemui.statusbar.stack.NotificationStackScrollLayout;
import com.google.gson.Gson;
import miui.os.Build;
import org.json.JSONObject;

public class LockScreenMagazineController {
    private final String TAG = "LockScreenMagazineController";
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("LockScreenMagazineController", "received broadcast " + action);
            if ("android.intent.action.PACKAGE_ADDED".equals(action) || "android.intent.action.PACKAGE_REPLACED".equals(action) || "android.intent.action.PACKAGE_REMOVED".equals(action) || "android.intent.action.PACKAGE_CHANGED".equals(action)) {
                String packageName = intent.getDataString();
                if (!TextUtils.isEmpty(packageName)) {
                    packageName = packageName.split(":")[1];
                }
                if (!TextUtils.isEmpty(packageName) && LockScreenMagazineUtils.LOCK_SCREEN_MAGAZINE_PACKAGE_NAME.equals(packageName)) {
                    Log.d("LockScreenMagazineController", "lock screen magazine package changed");
                    LockScreenMagazineController.this.mHandler.removeMessages(2);
                    LockScreenMagazineController.this.mHandler.sendEmptyMessageDelayed(2, 1000);
                }
            }
        }
    };
    private ValueAnimator mClockValueAnimator;
    /* access modifiers changed from: private */
    public Context mContext;
    private long mCurrentTouchDownTime;
    private ValueAnimator mDesValueAnimator;
    private View.OnClickListener mDomesticButtonOnClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            LockScreenMagazineAnalytics.recordLockScreenMagazinePreviewAction(LockScreenMagazineController.this.mContext, "click");
            if (LockScreenMagazineController.this.mIsLockScreenMagazinePkgExist) {
                LockScreenMagazineController.this.startSwitchAnimator(false);
                Log.d("miui_keyguard", "preview button goto lock screen wall paper");
                LockScreenMagazineUtils.gotoLockScreenMagazine(LockScreenMagazineController.this.mContext, "buttonLockScreen");
                return;
            }
            if (System.currentTimeMillis() - LockScreenMagazineController.this.mLastClickTime < 300) {
                AnalyticsHelper.recordDownloadLockScreenMagazine("buttonLockScreen");
                LockScreenMagazineController.this.startAppStoreToDownload();
            }
            long unused = LockScreenMagazineController.this.mLastClickTime = System.currentTimeMillis();
        }
    };
    private TextView mGlobalButton;
    private View.OnClickListener mGlobalButtonOnClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (LockScreenMagazineController.this.mIsLockScreenMagazinePkgExist) {
                Intent intent = LockScreenMagazineController.this.getPreLeftScreenIntent();
                if (PackageUtils.resolveIntent(LockScreenMagazineController.this.mContext, intent) != null) {
                    LockScreenMagazineController.this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
                    return;
                }
                return;
            }
            if (System.currentTimeMillis() - LockScreenMagazineController.this.mLastClickTime < 300) {
                AnalyticsHelper.recordDownloadLockScreenMagazine("buttonLockScreen");
                LockScreenMagazineController.this.startAppStoreToDownload();
            }
            long unused = LockScreenMagazineController.this.mLastClickTime = System.currentTimeMillis();
        }
    };
    /* access modifiers changed from: private */
    public final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    LockScreenMagazineController.this.handleLockScreenMagazineStatus();
                    return;
                case 2:
                    LockScreenMagazineController.this.initLockScreenMagazinePreRes();
                    return;
                default:
                    return;
            }
        }
    };
    private boolean mIsLockScreenMagazineOpenedWallpaper;
    /* access modifiers changed from: private */
    public boolean mIsLockScreenMagazinePkgExist;
    private boolean mIsLongPress;
    /* access modifiers changed from: private */
    public boolean mIsSwitchAnimating;
    /* access modifiers changed from: private */
    public KeyguardBottomAreaView mKeyguardBottomAreaView;
    /* access modifiers changed from: private */
    public KeyguardClockContainer mKeyguardClockView;
    /* access modifiers changed from: private */
    public boolean mKeyguardShowing;
    private final KeyguardUpdateMonitorCallback mKeyguardUpdateMonitorCallback = new KeyguardUpdateMonitorCallback() {
        public void onUserUnlocked() {
            LockScreenMagazineController.this.updateLockScreenMagazineAvailable();
            LockScreenMagazineController.this.queryLockScreenMagazineWallpaperInfo();
            LockScreenMagazineController.this.initLockScreenMagazinePreRes();
        }

        public void onLockScreenMagazineStatusChanged() {
            super.onLockScreenMagazineStatusChanged();
            Log.d("LockScreenMagazineController", "onLockScreenMagazineStatusChanged");
            LockScreenMagazineController.this.updateLockScreenMagazineAvailable();
            LockScreenMagazineController.this.mNotificationPanelView.inflateLeftView();
            LockScreenMagazineController.this.mLockScreenMagazinePre.initSettingButton();
            LockScreenMagazineController.this.mLockScreenMagazinePre.initLayoutVisibility();
            LockScreenMagazineController.this.mLockScreenMagazinePre.updateViews();
            LockScreenMagazineController.this.mKeyguardBottomAreaView.updateLeftAffordance();
            LockScreenMagazineController.this.mKeyguardBottomAreaView.initTipsView(true);
            LockScreenMagazineController.this.reset();
        }

        public void onRegionChanged() {
            LockScreenMagazineController.this.initLockScreenMagazinePreRes();
        }

        public void onKeyguardVisibilityChanged(boolean showing) {
            boolean unused = LockScreenMagazineController.this.mKeyguardShowing = showing;
            LockScreenMagazineController.this.reset();
        }

        public void onEmergencyCallAction() {
            LockScreenMagazineController.this.reset();
        }
    };
    /* access modifiers changed from: private */
    public long mLastClickTime = 0;
    private boolean mLockScreenMagazineAvailable;
    /* access modifiers changed from: private */
    public LockScreenMagazinePreView mLockScreenMagazinePre;
    private ContentObserver mLockScreenMagazineStatusObserver = new ContentObserver(this.mHandler) {
        public void onChange(boolean selfChange) {
            if (LockScreenMagazineUtils.getLockScreenMagazineStatus(LockScreenMagazineController.this.mContext) && !WallpaperAuthorityUtils.isLockScreenMagazineOpenedWallpaper()) {
                WallpaperAuthorityUtils.setWallpaperAuthoritySystemSetting(LockScreenMagazineController.this.mContext, WallpaperAuthorityUtils.APPLY_MAGAZINE_DEFAULT_AUTHORITY);
            }
        }
    };
    private ContentObserver mLockWallpaperProviderObserver = new ContentObserver(this.mHandler) {
        public void onChange(boolean selfChange) {
            LockScreenMagazineController.this.updateIsLockScreenMagazineWallpaper();
            LockScreenMagazineController.this.mHandler.removeMessages(1);
            LockScreenMagazineController.this.mHandler.sendEmptyMessageDelayed(1, 1000);
        }
    };
    /* access modifiers changed from: private */
    public NotificationPanelView mNotificationPanelView;
    /* access modifiers changed from: private */
    public NotificationStackScrollLayout mNotificationStackScrollLayout;
    /* access modifiers changed from: private */
    public String mPreLeftScreenActivityName;
    /* access modifiers changed from: private */
    public String mPreLeftScreenDrawableResName;
    private Drawable mPreMainEntryDarkIcon;
    private Drawable mPreMainEntryLightIcon;
    /* access modifiers changed from: private */
    public String mPreMainEntryResDarkIconName;
    /* access modifiers changed from: private */
    public String mPreMainEntryResLightIconName;
    /* access modifiers changed from: private */
    public String mPreTransToLeftScreenDrawableResName;
    Runnable mResetClockRunnable = new Runnable() {
        public void run() {
            if (LockScreenMagazineController.this.mNotificationPanelView.isQSFullyCollapsed()) {
                LockScreenMagazineController.this.startSwitchAnimator(false);
            } else {
                LockScreenMagazineController.this.reset();
            }
        }
    };
    private TextView mShowPreviewButton;
    protected StatusBar mStatusBar;
    private AnimatorSet mSwitchAnimator = new AnimatorSet();
    /* access modifiers changed from: private */
    public TextView mSwitchSystemUser;
    /* access modifiers changed from: private */
    public KeyguardUpdateMonitor mUpdateMonitor;
    private ViewConfiguration mViewConfiguration;
    private final KeyguardUpdateMonitor.WallpaperChangeCallback mWallpaperChangeCallback = new KeyguardUpdateMonitor.WallpaperChangeCallback() {
        public void onWallpaperChange(boolean succeed) {
            if (succeed) {
                boolean lightClock = LockScreenMagazineController.this.mUpdateMonitor.isLightClock();
                LockScreenMagazineController.this.mSwitchSystemUser.setTextColor(lightClock ? -1308622848 : -1);
                LockScreenMagazineController.this.mSwitchSystemUser.setCompoundDrawablesWithIntrinsicBounds(LockScreenMagazineController.this.mContext.getResources().getDrawable(lightClock ? R.drawable.logout_light : R.drawable.logout_dark), null, null, null);
                LockScreenMagazineController.this.queryLockScreenMagazineWallpaperInfo();
            }
        }
    };

    public LockScreenMagazineController(Context context, ViewGroup notificationPanelView, StatusBar statusBar) {
        this.mUpdateMonitor = KeyguardUpdateMonitor.getInstance(context);
        this.mContext = context;
        this.mStatusBar = statusBar;
        this.mNotificationPanelView = (NotificationPanelView) notificationPanelView;
        this.mLockScreenMagazinePre = (LockScreenMagazinePreView) this.mNotificationPanelView.findViewById(R.id.wallpaper_des);
        this.mShowPreviewButton = (TextView) this.mLockScreenMagazinePre.findViewById(R.id.preview_button);
        this.mShowPreviewButton.setOnClickListener(this.mDomesticButtonOnClickListener);
        this.mShowPreviewButton.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                return true;
            }
        });
        this.mGlobalButton = (TextView) this.mLockScreenMagazinePre.findViewById(R.id.global_button);
        this.mGlobalButton.setOnClickListener(this.mGlobalButtonOnClickListener);
        this.mGlobalButton.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                return true;
            }
        });
        this.mKeyguardClockView = (KeyguardClockContainer) this.mNotificationPanelView.findViewById(R.id.keyguard_clock_view);
        this.mKeyguardBottomAreaView = (KeyguardBottomAreaView) this.mNotificationPanelView.findViewById(R.id.keyguard_bottom_area);
        this.mKeyguardBottomAreaView.setLockScreenMagazineController(this);
        this.mSwitchSystemUser = (TextView) this.mNotificationPanelView.findViewById(R.id.switch_to_system_user);
        this.mNotificationStackScrollLayout = (NotificationStackScrollLayout) this.mNotificationPanelView.findViewById(R.id.notification_stack_scroller);
        this.mUpdateMonitor.registerWallpaperChangeCallback(this.mWallpaperChangeCallback);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("lock_wallpaper_provider_authority"), false, this.mLockWallpaperProviderObserver, -1);
        this.mLockWallpaperProviderObserver.onChange(false);
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(LockScreenMagazineUtils.SYSTEM_SETTINGS_KEY_LOCKSCREEN_MAGAZINE_STATUS), false, this.mLockScreenMagazineStatusObserver, -1);
        this.mUpdateMonitor.registerCallback(this.mKeyguardUpdateMonitorCallback);
        this.mViewConfiguration = ViewConfiguration.get(context);
        updateLockScreenMagazineAvailable();
        updateIsLockScreenMagazineWallpaper();
        Intent intent = new Intent("com.miui.keyguard.setwallpaper");
        intent.putExtra("set_lock_wallpaper_result", true);
        intent.setPackage("com.android.systemui");
        context.sendBroadcast(intent);
        IntentFilter packageChangeFilter = new IntentFilter();
        packageChangeFilter.addAction("android.intent.action.PACKAGE_ADDED");
        packageChangeFilter.addAction("android.intent.action.PACKAGE_REPLACED");
        packageChangeFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        packageChangeFilter.addAction("android.intent.action.PACKAGE_CHANGED");
        packageChangeFilter.addDataScheme("package");
        context.registerReceiver(this.mBroadcastReceiver, packageChangeFilter);
        if (this.mUpdateMonitor.isUserUnlocked()) {
            initLockScreenMagazinePreRes();
        }
    }

    public void setStatusBarKeyguardViewManager(StatusBarKeyguardViewManager statusBarKeyguardViewManager) {
        if (this.mLockScreenMagazinePre != null) {
            this.mLockScreenMagazinePre.setStatusBarKeyguardViewManager(statusBarKeyguardViewManager);
        }
    }

    public ViewGroup getWallPaperDes() {
        return this.mLockScreenMagazinePre;
    }

    /* access modifiers changed from: private */
    public void updateLockScreenMagazineWallpaperInfo() {
        if (this.mLockScreenMagazinePre != null) {
            this.mLockScreenMagazinePre.refreshWallpaperInfo();
        }
        if (this.mKeyguardClockView != null) {
            this.mKeyguardClockView.updateLockScreenMagazineInfo();
        }
    }

    public void reset() {
        this.mUpdateMonitor.handleLockScreenMagazinePreViewVisibilityChanged(false);
        cancelSwitchAnimate();
        removeResetClockCallbacks();
        resetViews();
    }

    private void resetViews() {
        if (this.mKeyguardShowing) {
            NotificationPanelView notificationPanelView = this.mNotificationPanelView;
            if (NotificationPanelView.isDefaultLockScreenTheme()) {
                if (!Build.IS_INTERNATIONAL_BUILD || !this.mUpdateMonitor.isSupportLockScreenMagazineLeft()) {
                    this.mLockScreenMagazinePre.setVisibility(4);
                    this.mLockScreenMagazinePre.setAlpha(0.0f);
                } else {
                    this.mLockScreenMagazinePre.setVisibility(0);
                    this.mLockScreenMagazinePre.setAlpha(1.0f);
                }
                this.mKeyguardClockView.setVisibility(0);
                this.mKeyguardBottomAreaView.setVisibility(0);
                this.mNotificationPanelView.refreshNotificationStackScrollerVisible();
                setViewsAlpha(1.0f);
            }
        }
        this.mLockScreenMagazinePre.setVisibility(4);
        this.mLockScreenMagazinePre.setAlpha(0.0f);
        this.mKeyguardClockView.setVisibility(4);
        this.mKeyguardBottomAreaView.setVisibility(4);
        this.mNotificationPanelView.refreshNotificationStackScrollerVisible();
        setViewsAlpha(1.0f);
    }

    public void setWallPaperViewsAlpha(float alpha) {
        if (this.mLockScreenMagazinePre.getVisibility() == 0 && !this.mIsSwitchAnimating) {
            this.mLockScreenMagazinePre.setAlpha(alpha);
        }
    }

    /* access modifiers changed from: private */
    public void updateLockScreenMagazineAvailable() {
        this.mLockScreenMagazineAvailable = LockScreenMagazineUtils.isLockScreenMagazineAvailable(this.mContext);
    }

    /* access modifiers changed from: private */
    public void updateIsLockScreenMagazineWallpaper() {
        String authority = WallpaperAuthorityUtils.getWallpaperAuthoritySystemSetting(this.mContext);
        if (!TextUtils.equals(authority, WallpaperAuthorityUtils.getWallpaperAuthority())) {
            WallpaperAuthorityUtils.setWallpaperAuthority(authority);
            boolean isLockScreenMagazineOpenedWallpaper = WallpaperAuthorityUtils.isLockScreenMagazineOpenedWallpaper();
            if (this.mIsLockScreenMagazineOpenedWallpaper != isLockScreenMagazineOpenedWallpaper) {
                this.mIsLockScreenMagazineOpenedWallpaper = isLockScreenMagazineOpenedWallpaper;
                if (!isLockScreenMagazineOpenedWallpaper) {
                    AnalyticsHelper.record("keyguard_close_lockscreen_magazine");
                }
                LockScreenMagazineAnalytics.recordLockScreenWallperProviderChanged(this.mContext, WallpaperAuthorityUtils.getWallpaperAuthority());
            }
            updateLockScreenMagazineWallpaperInfo();
        }
    }

    /* access modifiers changed from: private */
    public void startSwitchAnimator(final boolean toMagazine) {
        this.mUpdateMonitor.handleLockScreenMagazinePreViewVisibilityChanged(toMagazine);
        cancelSwitchAnimate();
        final boolean isOnlyForGlobal = Build.IS_INTERNATIONAL_BUILD && this.mUpdateMonitor.isSupportLockScreenMagazineLeft();
        float f = 1.0f;
        if (!isOnlyForGlobal) {
            float[] fArr = new float[2];
            fArr[0] = toMagazine ? 0.0f : 1.0f;
            fArr[1] = toMagazine ? 1.0f : 0.0f;
            this.mDesValueAnimator = ValueAnimator.ofFloat(fArr);
            this.mDesValueAnimator.setInterpolator(toMagazine ? Ease.Cubic.easeInOut : Ease.Quint.easeOut);
            this.mDesValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    LockScreenMagazineController.this.mLockScreenMagazinePre.setAlpha(((Float) animation.getAnimatedValue()).floatValue());
                }
            });
        }
        float[] fArr2 = new float[2];
        fArr2[0] = toMagazine ? 1.0f : 0.0f;
        if (toMagazine) {
            f = 0.0f;
        }
        fArr2[1] = f;
        this.mClockValueAnimator = ValueAnimator.ofFloat(fArr2);
        this.mClockValueAnimator.setInterpolator(toMagazine ? Ease.Quint.easeOut : Ease.Cubic.easeInOut);
        this.mClockValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                LockScreenMagazineController.this.setViewsAlpha(((Float) animation.getAnimatedValue()).floatValue());
            }
        });
        this.mSwitchAnimator.setDuration(500);
        if (isOnlyForGlobal) {
            this.mSwitchAnimator.play(this.mClockValueAnimator);
        } else {
            this.mSwitchAnimator.play(this.mDesValueAnimator).with(this.mClockValueAnimator);
        }
        this.mSwitchAnimator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationCancel(Animator animation) {
                super.onAnimationCancel(animation);
            }

            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                int i = 0;
                boolean unused = LockScreenMagazineController.this.mIsSwitchAnimating = false;
                LockScreenMagazineController.this.mKeyguardClockView.setVisibility(toMagazine ? 4 : 0);
                LockScreenMagazineController.this.mKeyguardBottomAreaView.setVisibility(toMagazine ? 4 : 0);
                LockScreenMagazineController.this.mNotificationStackScrollLayout.setVisibility(toMagazine ? 4 : 0);
                if (!isOnlyForGlobal) {
                    LockScreenMagazinePreView access$600 = LockScreenMagazineController.this.mLockScreenMagazinePre;
                    if (!toMagazine) {
                        i = 4;
                    }
                    access$600.setVisibility(i);
                }
            }

            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                boolean unused = LockScreenMagazineController.this.mIsSwitchAnimating = true;
                LockScreenMagazineController.this.mKeyguardClockView.setVisibility(0);
                LockScreenMagazineController.this.mKeyguardBottomAreaView.setVisibility(0);
                LockScreenMagazineController.this.mNotificationStackScrollLayout.setVisibility(0);
                if (!isOnlyForGlobal) {
                    LockScreenMagazineController.this.mLockScreenMagazinePre.setVisibility(0);
                }
            }
        });
        this.mSwitchAnimator.start();
        removeResetClockCallbacks();
        if (toMagazine) {
            postDelayedResetClock();
        }
    }

    /* access modifiers changed from: private */
    public void setViewsAlpha(float alpha) {
        this.mKeyguardClockView.setClockAlpha(alpha);
        this.mKeyguardBottomAreaView.setViewsAlpha(alpha);
        this.mSwitchSystemUser.setAlpha(alpha);
        this.mNotificationStackScrollLayout.setAlpha(alpha);
    }

    private void cancelSwitchAnimate() {
        if (this.mSwitchAnimator != null) {
            this.mSwitchAnimator.cancel();
            this.mSwitchAnimator.removeAllListeners();
        }
        if (this.mDesValueAnimator != null) {
            this.mDesValueAnimator.cancel();
            this.mDesValueAnimator.removeAllUpdateListeners();
            this.mDesValueAnimator = null;
        }
        if (this.mClockValueAnimator != null) {
            this.mClockValueAnimator.cancel();
            this.mClockValueAnimator.removeAllUpdateListeners();
            this.mClockValueAnimator = null;
        }
    }

    public boolean isSwitchAnimating() {
        return this.mIsSwitchAnimating;
    }

    private void postDelayedResetClock() {
        this.mNotificationPanelView.postDelayed(this.mResetClockRunnable, 5000);
    }

    private void removeResetClockCallbacks() {
        this.mNotificationPanelView.removeCallbacks(this.mResetClockRunnable);
    }

    public void onTouchEvent(MotionEvent event, int statusBarState, float initialTouchX, float initialTouchY) {
        if (event.getAction() == 0) {
            this.mCurrentTouchDownTime = System.currentTimeMillis();
            this.mIsLongPress = false;
        } else if (event.getAction() == 1 && System.currentTimeMillis() - this.mCurrentTouchDownTime > 500) {
            this.mIsLongPress = true;
        }
        int touchSlop = this.mViewConfiguration.getScaledTouchSlop();
        if (event.getAction() == 1 && statusBarState == 1 && this.mNotificationPanelView.isQSFullyCollapsed() && Math.abs(initialTouchX - event.getRawX()) < ((float) touchSlop) && Math.abs(initialTouchY - event.getRawY()) < ((float) touchSlop) && !this.mIsLongPress) {
            handleSingleClickEvent();
        }
    }

    public void handleSingleClickEvent() {
        if (this.mLockScreenMagazineAvailable) {
            if (shouldShowPreView()) {
                handleSwitchAnimator();
            } else if (!WallpaperAuthorityUtils.isLockScreenMagazineOpenedWallpaper() && this.mUpdateMonitor.isSupportLockScreenMagazineLeft()) {
                this.mKeyguardBottomAreaView.handleBottomButtonClicked(true);
                this.mKeyguardBottomAreaView.startButtonLayoutAnimate(true);
            }
        }
    }

    private boolean shouldShowPreView() {
        if (!Build.IS_INTERNATIONAL_BUILD || !this.mUpdateMonitor.isSupportLockScreenMagazineLeft()) {
            return WallpaperAuthorityUtils.isLockScreenMagazineWallpaper();
        }
        return WallpaperAuthorityUtils.isLockScreenMagazineOpenedWallpaper() && !TextUtils.isEmpty(this.mUpdateMonitor.getLockScreenMagazineWallpaperInfo().title);
    }

    private void handleSwitchAnimator() {
        if (!this.mSwitchAnimator.isRunning() && !this.mSwitchAnimator.isStarted() && !this.mIsSwitchAnimating) {
            if (this.mKeyguardClockView.getVisibility() == 0) {
                startSwitchAnimator(true);
                LockScreenMagazineAnalytics.recordLockScreenMagazinePreviewAction(this.mContext, "show");
            } else {
                startSwitchAnimator(false);
            }
        }
    }

    /* access modifiers changed from: private */
    public void startAppStoreToDownload() {
        try {
            this.mStatusBar.startActivity(PackageUtils.getMarketDownloadIntent(LockScreenMagazineUtils.LOCK_SCREEN_MAGAZINE_PACKAGE_NAME), true);
        } catch (Exception e) {
            Log.e("miui_keyguard", "start to download lockscreen wallpaper", e);
        }
    }

    public void updateResources(boolean isThemeChanged) {
        Log.d("LockScreenMagazineController", "updateResources isThemeChanged = " + isThemeChanged);
        if (isThemeChanged) {
            updateLockScreenMagazineAvailable();
            handleLockScreenMagazineStatus();
            updateLockScreenMagazineWallpaperInfo();
        }
    }

    /* access modifiers changed from: private */
    public void initLockScreenMagazinePreRes() {
        Log.d("LockScreenMagazineController", "initLockScreenMagazinePreRes");
        new AsyncTask<Void, Void, Void>() {
            /* access modifiers changed from: protected */
            public Void doInBackground(Void... params) {
                String resultJson;
                Bundle bundle = LockScreenMagazineUtils.getLockScreenMagazinePreContent(LockScreenMagazineController.this.mContext);
                if (bundle != null) {
                    resultJson = bundle.getString("result_json");
                } else {
                    resultJson = null;
                }
                if (!TextUtils.isEmpty(resultJson)) {
                    try {
                        Log.d("LockScreenMagazineController", "initLockScreenMagazinePreRes resultJson = " + resultJson);
                        JSONObject jsonObject = new JSONObject(resultJson);
                        String unused = LockScreenMagazineController.this.mPreLeftScreenActivityName = jsonObject.getString("leftscreen_activity");
                        String unused2 = LockScreenMagazineController.this.mPreMainEntryResDarkIconName = jsonObject.getString("main_entry_res_icon_dark");
                        String unused3 = LockScreenMagazineController.this.mPreMainEntryResLightIconName = jsonObject.getString("main_entry_res_icon_light");
                        String unused4 = LockScreenMagazineController.this.mPreTransToLeftScreenDrawableResName = jsonObject.getString("trans_to_leftscreen_res_drawable");
                        String unused5 = LockScreenMagazineController.this.mPreLeftScreenDrawableResName = jsonObject.getString("leftscreen_res_drawable_preview");
                    } catch (Exception e) {
                        Log.e("LockScreenMagazineController", "initLockScreenMagazinePreRes", e);
                    }
                    LockScreenMagazineController.this.initPreMainEntryIcon();
                } else {
                    String unused6 = LockScreenMagazineController.this.mPreLeftScreenActivityName = null;
                    String unused7 = LockScreenMagazineController.this.mPreMainEntryResDarkIconName = null;
                    String unused8 = LockScreenMagazineController.this.mPreMainEntryResLightIconName = null;
                    String unused9 = LockScreenMagazineController.this.mPreTransToLeftScreenDrawableResName = null;
                    String unused10 = LockScreenMagazineController.this.mPreLeftScreenDrawableResName = null;
                }
                LockScreenMagazineController.this.initLockScreenMagazinePkgExist();
                return null;
            }

            /* access modifiers changed from: protected */
            public void onPostExecute(Void result) {
                LockScreenMagazineController.this.handlePreLeftScreenActivityName();
                if (!TextUtils.isEmpty(LockScreenMagazineController.this.mPreLeftScreenActivityName)) {
                    KeyguardUpdateMonitor.getInstance(LockScreenMagazineController.this.mContext).setSupportLockScreenMagazineLeft(true);
                    return;
                }
                LockScreenMagazineUtils.setLockScreenMagazineStatus(LockScreenMagazineController.this.mContext, false);
                LockScreenMagazineUtils.notifySubscriptionChange(LockScreenMagazineController.this.mContext);
                KeyguardUpdateMonitor.getInstance(LockScreenMagazineController.this.mContext).setSupportLockScreenMagazineLeft(false);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
    }

    /* access modifiers changed from: private */
    public void handlePreLeftScreenActivityName() {
        if (!TextUtils.isEmpty(this.mPreLeftScreenActivityName)) {
            try {
                String[] arrayString = this.mPreLeftScreenActivityName.split("/");
                if (arrayString != null && arrayString.length > 1) {
                    this.mPreLeftScreenActivityName = arrayString[1];
                }
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(LockScreenMagazineUtils.LOCK_SCREEN_MAGAZINE_PACKAGE_NAME, this.mPreLeftScreenActivityName));
                if (PackageUtils.resolveIntent(this.mContext, intent) == null) {
                    this.mPreLeftScreenActivityName = null;
                }
            } catch (Exception e) {
                Log.e("LockScreenMagazineController", "handlePreLeftScreenActivityName failed", e);
                this.mPreLeftScreenActivityName = null;
            }
        }
    }

    public void initPreMainEntryIcon() {
        this.mPreMainEntryDarkIcon = PackageUtils.getDrawableFromPackage(this.mContext, LockScreenMagazineUtils.LOCK_SCREEN_MAGAZINE_PACKAGE_NAME, this.mPreMainEntryResDarkIconName);
        this.mPreMainEntryLightIcon = PackageUtils.getDrawableFromPackage(this.mContext, LockScreenMagazineUtils.LOCK_SCREEN_MAGAZINE_PACKAGE_NAME, this.mPreMainEntryResLightIconName);
    }

    /* access modifiers changed from: private */
    public void initLockScreenMagazinePkgExist() {
        this.mIsLockScreenMagazinePkgExist = PackageUtils.isAppInstalledForUser(this.mContext, LockScreenMagazineUtils.LOCK_SCREEN_MAGAZINE_PACKAGE_NAME, KeyguardUpdateMonitor.getCurrentUser());
        this.mUpdateMonitor.setLockScreenMagazinePkgExist(this.mIsLockScreenMagazinePkgExist);
    }

    private String getPreLeftScreenActivityName() {
        return this.mPreLeftScreenActivityName;
    }

    public String getPreTransToLeftScreenDrawableResName() {
        return this.mPreTransToLeftScreenDrawableResName;
    }

    public String getPreLeftScreenDrawableResName() {
        return this.mPreLeftScreenDrawableResName;
    }

    public Drawable getPreMainEntryResDarkIcon() {
        return this.mPreMainEntryDarkIcon;
    }

    public Drawable getPreMainEntryResLightIcon() {
        return this.mPreMainEntryLightIcon;
    }

    /* access modifiers changed from: private */
    public void handleLockScreenMagazineStatus() {
        boolean isCustomWallpaper = WallpaperAuthorityUtils.isCustomWallpaper();
        boolean isLockScreenMagazineOpend = LockScreenMagazineUtils.getLockScreenMagazineStatus(this.mContext);
        Log.d("LockScreenMagazineController", "handleLockScreenMagazineStatus wallpaperAuthority = " + WallpaperAuthorityUtils.getWallpaperAuthority() + " isLockScreenMagazineOpend = " + isLockScreenMagazineOpend);
        if (!isCustomWallpaper) {
            boolean isThemeLockWallpaper = WallpaperAuthorityUtils.isThemeLockWallpaper();
            boolean isDefaultLockScreenTheme = MiuiKeyguardUtils.isDefaultLockScreenTheme();
            if (!WallpaperAuthorityUtils.isLockScreenMagazineOpenedWallpaper() && isLockScreenMagazineOpend) {
                if (!isThemeLockWallpaper || isDefaultLockScreenTheme) {
                    WallpaperAuthorityUtils.setWallpaperAuthoritySystemSetting(this.mContext, WallpaperAuthorityUtils.APPLY_MAGAZINE_DEFAULT_AUTHORITY);
                }
            }
        } else if (isLockScreenMagazineOpend) {
            LockScreenMagazineUtils.setLockScreenMagazineStatus(this.mContext, false);
            LockScreenMagazineUtils.notifySubscriptionChange(this.mContext);
        }
    }

    /* access modifiers changed from: private */
    public void queryLockScreenMagazineWallpaperInfo() {
        if (this.mLockScreenMagazineAvailable && WallpaperAuthorityUtils.isLockScreenMagazineWallpaper()) {
            new AsyncTask<Void, Void, Void>() {
                /* access modifiers changed from: protected */
                public Void doInBackground(Void... params) {
                    LockScreenMagazineWallpaperInfo lockScreenMagazineWallpaperInfo = LockScreenMagazineUtils.getLockScreenMagazineWallpaperInfo(LockScreenMagazineController.this.mContext);
                    lockScreenMagazineWallpaperInfo.initExtra();
                    LockScreenMagazineController.this.mUpdateMonitor.setLockScreenMagazineWallpaperInfo(lockScreenMagazineWallpaperInfo);
                    return null;
                }

                /* access modifiers changed from: protected */
                public void onPostExecute(Void result) {
                    LockScreenMagazineController.this.updateLockScreenMagazineWallpaperInfo();
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
        }
    }

    public Intent getPreLeftScreenIntent() {
        if (!this.mUpdateMonitor.isSupportLockScreenMagazineLeft()) {
            return null;
        }
        Intent intent = null;
        try {
            String activityName = getPreLeftScreenActivityName();
            if (!TextUtils.isEmpty(activityName)) {
                intent = new Intent();
                intent.setComponent(new ComponentName(LockScreenMagazineUtils.LOCK_SCREEN_MAGAZINE_PACKAGE_NAME, activityName));
                intent.addFlags(268435456);
                if (Build.IS_INTERNATIONAL_BUILD) {
                    intent.putExtra("wc_enable_source", "systemui");
                    intent.putExtra("wallpaper_uri", this.mUpdateMonitor.getLockScreenMagazineWallpaperInfo().wallpaperUri);
                    intent.putExtra("wallpaper_details", new Gson().toJson(this.mUpdateMonitor.getLockScreenMagazineWallpaperInfo()));
                } else {
                    intent.putExtra("from", "keyguard");
                }
            }
        } catch (Exception e) {
            Log.e("LockScreenMagazineController", "getPreLeftScreenIntent", e);
            intent = null;
        }
        return intent;
    }
}
