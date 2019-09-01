package com.android.systemui.statusbar.phone;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManagerCompat;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.util.Slog;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.Toast;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardHostView;
import com.android.keyguard.KeyguardSecurityModel;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.keyguard.MiuiBleUnlockHelper;
import com.android.keyguard.ViewMediatorCallback;
import com.android.keyguard.analytics.AnalyticsHelper;
import com.android.keyguard.magazine.LockScreenMagazineUtils;
import com.android.keyguard.wallpaper.KeyguardWallpaperUtils;
import com.android.systemui.DejankUtils;
import com.android.systemui.R;
import com.android.systemui.classifier.FalsingManager;
import com.android.systemui.keyguard.DismissCallbackRegistry;
import miui.util.CustomizeUtil;
import miui.util.ScreenshotUtils;

public class KeyguardBouncer {
    /* access modifiers changed from: private */
    public ImageView mBgImageView;
    /* access modifiers changed from: private */
    public int mBouncerPromptReason;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (KeyguardBouncer.this.isAllowUnlockForBle()) {
                AnalyticsHelper.recordUnlockWay("band");
                KeyguardBouncer.this.unlockByBle();
            }
        }
    };
    protected final ViewMediatorCallback mCallback;
    protected final ViewGroup mContainer;
    protected final Context mContext;
    private final DismissCallbackRegistry mDismissCallbackRegistry;
    private final FalsingManager mFalsingManager;
    /* access modifiers changed from: private */
    public boolean mForceBlack = false;
    private ContentObserver mForceBlackObserver;
    private final Handler mHandler;
    /* access modifiers changed from: private */
    public boolean mHasUnlockByBle = false;
    protected KeyguardHostView mKeyguardView;
    protected final LockPatternUtils mLockPatternUtils;
    /* access modifiers changed from: private */
    public View mNotchCorner;
    private final Runnable mRemoveViewRunnable = new Runnable() {
        public void run() {
            KeyguardBouncer.this.removeView();
        }
    };
    protected ViewGroup mRoot;
    private final Runnable mShowRunnable = new Runnable() {
        public void run() {
            KeyguardBouncer.this.mRoot.setVisibility(0);
            KeyguardBouncer.this.mKeyguardView.onResume();
            KeyguardBouncer.this.showPromptReason(KeyguardBouncer.this.mBouncerPromptReason);
            if (!KeyguardBouncer.this.isFullscreenBouncer() && KeyguardBouncer.this.mUpdateMonitor.isScreenOn()) {
                KeyguardBouncer.this.mKeyguardView.applyHintAnimation(500);
            }
            if (KeyguardBouncer.this.mKeyguardView.getHeight() != 0) {
                KeyguardBouncer.this.mKeyguardView.startAppearAnimation();
            } else {
                KeyguardBouncer.this.mKeyguardView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                    public boolean onPreDraw() {
                        KeyguardBouncer.this.mKeyguardView.getViewTreeObserver().removeOnPreDrawListener(this);
                        KeyguardBouncer.this.mKeyguardView.startAppearAnimation();
                        return true;
                    }
                });
                KeyguardBouncer.this.mKeyguardView.requestLayout();
            }
            boolean unused = KeyguardBouncer.this.mShowingSoon = false;
            KeyguardBouncer.this.mKeyguardView.sendAccessibilityEvent(32);
            if (KeyguardBouncer.this.isAllowUnlockForBle()) {
                KeyguardBouncer.this.unlockByBle();
                boolean unused2 = KeyguardBouncer.this.mHasUnlockByBle = true;
            }
        }
    };
    /* access modifiers changed from: private */
    public boolean mShowingSoon;
    /* access modifiers changed from: private */
    public KeyguardUpdateMonitor mUpdateMonitor;
    private final KeyguardUpdateMonitorCallback mUpdateMonitorCallback = new KeyguardUpdateMonitorCallback() {
        public void onStrongAuthStateChanged(int userId) {
            int unused = KeyguardBouncer.this.mBouncerPromptReason = KeyguardBouncer.this.mCallback.getBouncerPromptReason();
        }

        public void onKeyguardBouncerChanged(boolean bouncer) {
            if (!KeyguardBouncer.this.mUpdateMonitor.getStrongAuthTracker().hasOwnerUserAuthenticatedSinceBoot() && bouncer) {
                int unused = KeyguardBouncer.this.mBouncerPromptReason = KeyguardBouncer.this.mCallback.getBouncerPromptReason();
            }
        }
    };
    private final KeyguardUpdateMonitor.WallpaperChangeCallback mWallpaperChangeCallback = new KeyguardUpdateMonitor.WallpaperChangeCallback() {
        public void onWallpaperChange(boolean succeed) {
            if (succeed) {
                KeyguardBouncer.this.updateWallpaper();
            }
        }
    };

    public KeyguardBouncer(Context context, ViewMediatorCallback callback, LockPatternUtils lockPatternUtils, ViewGroup container, DismissCallbackRegistry dismissCallbackRegistry) {
        this.mContext = context;
        this.mCallback = callback;
        this.mLockPatternUtils = lockPatternUtils;
        this.mContainer = container;
        this.mUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        this.mUpdateMonitor.registerCallback(this.mUpdateMonitorCallback);
        this.mUpdateMonitor.registerWallpaperChangeCallback(this.mWallpaperChangeCallback);
        this.mFalsingManager = FalsingManager.getInstance(this.mContext);
        this.mDismissCallbackRegistry = dismissCallbackRegistry;
        this.mHandler = new Handler();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("miui_keyguard_ble_unlock_succeed");
        this.mContext.registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.CURRENT, intentFilter, null, null);
        if (CustomizeUtil.HAS_NOTCH) {
            this.mForceBlackObserver = new ContentObserver(this.mHandler) {
                public void onChange(boolean selfChange) {
                    boolean unused = KeyguardBouncer.this.mForceBlack = MiuiSettings.Global.getBoolean(KeyguardBouncer.this.mContext.getContentResolver(), "force_black");
                    if (KeyguardBouncer.this.mNotchCorner != null) {
                        KeyguardBouncer.this.mNotchCorner.setVisibility(KeyguardBouncer.this.mForceBlack ? 0 : 8);
                    }
                }
            };
            this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("force_black"), false, this.mForceBlackObserver, -1);
            this.mForceBlackObserver.onChange(false);
        }
    }

    /* access modifiers changed from: private */
    public boolean isAllowUnlockForBle() {
        return this.mUpdateMonitor.getBLEUnlockState() == MiuiBleUnlockHelper.BLEUnlockState.SUCCEED && this.mRoot != null && this.mRoot.getVisibility() == 0 && !this.mHasUnlockByBle && this.mUpdateMonitor.isUnlockingWithFingerprintAllowed(KeyguardUpdateMonitor.getCurrentUser()) && !SubscriptionManager.isValidSubscriptionId(this.mUpdateMonitor.getNextSubIdForState(IccCardConstants.State.PIN_REQUIRED)) && !SubscriptionManager.isValidSubscriptionId(this.mUpdateMonitor.getNextSubIdForState(IccCardConstants.State.PUK_REQUIRED));
    }

    /* access modifiers changed from: private */
    public void unlockByBle() {
        if (this.mKeyguardView != null) {
            this.mKeyguardView.finish(false, KeyguardUpdateMonitor.getCurrentUser());
            Toast.makeText(this.mContext, R.string.miui_keyguard_ble_unlock_succeed_msg, 0).show();
        }
    }

    /* access modifiers changed from: private */
    public void updateWallpaper() {
        if (this.mBgImageView != null) {
            new AsyncTask<Void, Void, Drawable>() {
                /* access modifiers changed from: protected */
                public Drawable doInBackground(Void... params) {
                    return KeyguardWallpaperUtils.getLockWallpaperPreview(KeyguardBouncer.this.mContext);
                }

                /* access modifiers changed from: protected */
                public void onPostExecute(Drawable background) {
                    if (KeyguardBouncer.this.mBgImageView != null) {
                        Bitmap backgroundOriginal = background == null ? null : ((BitmapDrawable) background).getBitmap();
                        int width = backgroundOriginal == null ? 0 : (int) (((float) backgroundOriginal.getWidth()) * 0.33333334f);
                        int height = backgroundOriginal == null ? 0 : (int) (((float) backgroundOriginal.getHeight()) * 0.33333334f);
                        if (width <= 0 || height <= 0) {
                            KeyguardBouncer.this.mBgImageView.setImageDrawable(null);
                            KeyguardBouncer.this.mBgImageView.setBackgroundColor(KeyguardBouncer.this.mContext.getResources().getColor(miui.system.R.color.blur_background_mask));
                            return;
                        }
                        Bitmap backgroundOriginal2 = Bitmap.createScaledBitmap(backgroundOriginal, width, height, true);
                        KeyguardBouncer.this.mBgImageView.setBackgroundColor(0);
                        KeyguardBouncer.this.mBgImageView.setImageDrawable(new BitmapDrawable(KeyguardBouncer.this.mContext.getResources(), ScreenshotUtils.getBlurBackground(backgroundOriginal2, null)));
                        backgroundOriginal2.recycle();
                    }
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
        }
    }

    public void show(boolean resetSecuritySelection) {
        int keyguardUserId = KeyguardUpdateMonitor.getCurrentUser();
        if (keyguardUserId != 0 || !UserManagerCompat.isSplitSystemUser()) {
            this.mFalsingManager.onBouncerShown();
            ensureView();
            if (resetSecuritySelection) {
                this.mKeyguardView.showPrimarySecurityScreen();
            }
            if (this.mRoot.getVisibility() != 0 && !this.mShowingSoon) {
                int activeUserId = ActivityManager.getCurrentUser();
                boolean allowDismissKeyguard = false;
                if (!(UserManagerCompat.isSplitSystemUser() && activeUserId == 0) && activeUserId == keyguardUserId) {
                    allowDismissKeyguard = true;
                }
                if (!allowDismissKeyguard || !this.mKeyguardView.dismiss(activeUserId)) {
                    if (!allowDismissKeyguard) {
                        Slog.w("KeyguardBouncer", "User can't dismiss keyguard: " + activeUserId + " != " + keyguardUserId);
                    }
                    this.mShowingSoon = true;
                    DejankUtils.postAfterTraversal(this.mShowRunnable);
                    this.mCallback.onBouncerVisiblityChanged(true);
                    LockScreenMagazineUtils.sendLockScreenMagazineEventBrodcast(this.mContext, "Wallpaper_Covered");
                }
            }
        }
    }

    public void showPromptReason(int reason) {
        if (!isFullscreenBouncer() && this.mKeyguardView != null) {
            this.mKeyguardView.showPromptReason(reason);
        }
    }

    public void showMessage(String message, int color) {
        if (!isFullscreenBouncer() && this.mKeyguardView != null) {
            this.mKeyguardView.showMessage(message, color);
        }
    }

    public void showMessage(String title, String message, int color) {
        if (!isFullscreenBouncer() && this.mKeyguardView != null) {
            this.mKeyguardView.showMessage(title, message, color);
        }
    }

    public void applyHintAnimation(long offset) {
        if (!isFullscreenBouncer() && this.mKeyguardView != null) {
            this.mKeyguardView.applyHintAnimation(offset);
        }
    }

    private void cancelShowRunnable() {
        DejankUtils.removeCallbacks(this.mShowRunnable);
        this.mShowingSoon = false;
    }

    public void showWithDismissAction(KeyguardHostView.OnDismissAction r, Runnable cancelAction) {
        ensureView();
        this.mKeyguardView.setOnDismissAction(r, cancelAction);
        show(false);
    }

    public void hide(boolean destroyView) {
        if (isShowing()) {
            this.mDismissCallbackRegistry.notifyDismissCancelled();
        }
        this.mFalsingManager.onBouncerHidden();
        this.mCallback.onBouncerVisiblityChanged(false);
        cancelShowRunnable();
        if (this.mKeyguardView != null) {
            this.mKeyguardView.cancelDismissAction();
            this.mKeyguardView.cleanUp();
        }
        if (this.mRoot != null) {
            this.mRoot.setVisibility(4);
            if (destroyView) {
                this.mHandler.postDelayed(this.mRemoveViewRunnable, 50);
            }
        }
    }

    public void startPreHideAnimation(Runnable runnable) {
        if (this.mKeyguardView != null) {
            this.mKeyguardView.startDisappearAnimation(runnable);
        } else if (runnable != null) {
            runnable.run();
        }
    }

    public void onFinishedGoingToSleep() {
        if (this.mKeyguardView != null && this.mRoot != null && this.mRoot.getVisibility() == 0) {
            this.mKeyguardView.onPause();
        }
    }

    public boolean isShowing() {
        return this.mShowingSoon || (this.mRoot != null && this.mRoot.getVisibility() == 0);
    }

    public void prepare() {
        boolean wasInitialized = this.mRoot != null;
        ensureView();
        if (wasInitialized) {
            this.mKeyguardView.showPrimarySecurityScreen();
        }
        this.mBouncerPromptReason = this.mCallback.getBouncerPromptReason();
    }

    /* access modifiers changed from: protected */
    public void ensureView() {
        this.mHandler.removeCallbacks(this.mRemoveViewRunnable);
        if (this.mRoot == null) {
            inflateView();
        }
    }

    /* access modifiers changed from: protected */
    public void inflateView() {
        removeView();
        this.mHandler.removeCallbacks(this.mRemoveViewRunnable);
        this.mRoot = (ViewGroup) LayoutInflater.from(this.mContext).inflate(R.layout.keyguard_bouncer, null);
        this.mBgImageView = (ImageView) this.mRoot.findViewById(R.id.keyguard_bouncer_bg);
        this.mNotchCorner = this.mRoot.findViewById(R.id.notch_corner_security);
        this.mNotchCorner.setVisibility(this.mForceBlack ? 0 : 8);
        this.mKeyguardView = (KeyguardHostView) this.mRoot.findViewById(R.id.keyguard_host_view);
        this.mKeyguardView.setLockPatternUtils(this.mLockPatternUtils);
        this.mKeyguardView.setViewMediatorCallback(this.mCallback);
        this.mContainer.addView(this.mRoot, this.mContainer.getChildCount());
        this.mRoot.setVisibility(4);
        this.mHasUnlockByBle = false;
        updateWallpaper();
    }

    /* access modifiers changed from: protected */
    public void removeView() {
        if (this.mRoot != null && this.mRoot.getParent() == this.mContainer) {
            this.mContainer.removeView(this.mRoot);
            this.mRoot = null;
        }
    }

    public boolean onBackPressed() {
        return this.mKeyguardView != null && this.mKeyguardView.handleBackKey();
    }

    public boolean needsFullscreenBouncer() {
        ensureView();
        boolean z = false;
        if (this.mKeyguardView == null) {
            return false;
        }
        KeyguardSecurityModel.SecurityMode mode = this.mKeyguardView.getSecurityMode();
        if (mode == KeyguardSecurityModel.SecurityMode.SimPin || mode == KeyguardSecurityModel.SecurityMode.SimPuk) {
            z = true;
        }
        return z;
    }

    public boolean isFullscreenBouncer() {
        boolean z = false;
        if (this.mKeyguardView == null) {
            return false;
        }
        KeyguardSecurityModel.SecurityMode mode = this.mKeyguardView.getCurrentSecurityMode();
        if (mode == KeyguardSecurityModel.SecurityMode.SimPin || mode == KeyguardSecurityModel.SecurityMode.SimPuk) {
            z = true;
        }
        return z;
    }

    public boolean isSecure() {
        return this.mKeyguardView == null || this.mKeyguardView.getSecurityMode() != KeyguardSecurityModel.SecurityMode.None;
    }

    public boolean shouldDismissOnMenuPressed() {
        return this.mKeyguardView.shouldEnableMenuKey();
    }

    public boolean interceptMediaKey(KeyEvent event) {
        ensureView();
        return this.mKeyguardView.interceptMediaKey(event);
    }

    public void notifyKeyguardAuthenticated(boolean strongAuth) {
        ensureView();
        this.mKeyguardView.finish(strongAuth, KeyguardUpdateMonitor.getCurrentUser());
    }
}
