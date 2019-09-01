package com.android.systemui.aspect;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.DisplayInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManagerCompat;
import android.view.WindowManagerGlobal;
import com.android.internal.os.SomeArgs;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.systemui.Application;
import com.android.systemui.Dependency;
import com.android.systemui.DisplayCutoutCompat;
import com.android.systemui.R;
import com.android.systemui.SystemUI;
import com.android.systemui.events.AspectClickEvent;
import com.android.systemui.miui.ActivityObserver;
import com.android.systemui.recents.events.RecentsEventBus;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.policy.ConfigurationController;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import miui.os.Build;
import miui.os.MiuiInit;

public class AspectUI extends SystemUI implements CommandQueue.Callbacks, ConfigurationController.ConfigurationListener {
    private final ActivityObserver.ActivityObserverCallback mActivityStateObserver = new ActivityObserver.ActivityObserverCallback() {
        public void activityResumed(Intent intent) {
            AspectUI.this.mHandler.removeMessages(1);
            AspectUI.this.mHandler.obtainMessage(1, 0, 0, intent).sendToTarget();
        }
    };
    /* access modifiers changed from: private */
    public AlertDialog mAspectDialog;
    /* access modifiers changed from: private */
    public String mAspectPkg;
    private View mAspectView;
    /* access modifiers changed from: private */
    public Point mCurrentSize;
    /* access modifiers changed from: private */
    public Display mDisplay;
    private ContentObserver mFullScreenGestureListener = new ContentObserver(this.mHandler) {
        public void onChange(boolean selfChange) {
            boolean unused = AspectUI.this.mIsFsgMode = MiuiSettings.Global.getBoolean(AspectUI.this.mContext.getContentResolver(), "force_fsg_nav_bar");
            AspectUI.this.updateAspectVisibility();
        }
    };
    /* access modifiers changed from: private */
    public H mHandler = new H();
    /* access modifiers changed from: private */
    public final DisplayInfo mInfo = new DisplayInfo();
    /* access modifiers changed from: private */
    public boolean mIsFsgMode;
    private Configuration mLastConfiguration;
    private int mMinWindowHeight;
    /* access modifiers changed from: private */
    public int mRotation;
    private boolean mShowAspect;
    private boolean mSoftInputVisible;
    Runnable mUpdateAspectRunnable = new Runnable() {
        public void run() {
            AspectUI.this.updateAspectVisibility();
        }
    };
    private int mWindowHeight;
    private WindowManager mWindowManager;

    private class H extends Handler {
        private H() {
        }

        public void handleMessage(Message m) {
            if (m.what == 1) {
                Intent intent = (Intent) m.obj;
                String pkg = intent.getComponent().getPackageName();
                Rect appBounds = (Rect) intent.getParcelableExtra("appBounds");
                boolean show = false;
                if (appBounds != null) {
                    int width = Math.min(appBounds.width(), appBounds.height());
                    int height = Math.max(appBounds.width(), appBounds.height());
                    if (Math.min(AspectUI.this.mCurrentSize.x, AspectUI.this.mCurrentSize.y) == width && ((int) (((double) (((float) width) * 1.86f)) + 0.5d)) == height) {
                        show = true;
                    }
                }
                AspectUI.this.setAspectVisibility(show, pkg);
            }
        }
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
        this.mSoftInputVisible = (vis & 2) != 0;
        updateAspectVisibility();
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

    public void start() {
        Log.d("AspectUI", "start AspectUI");
        boolean hasNavigationBar = false;
        try {
            hasNavigationBar = WindowManagerGlobal.getWindowManagerService().hasNavigationBar();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        if (hasNavigationBar && !Build.IS_TABLET && !"lithium".equals(android.os.Build.DEVICE)) {
            updateResource();
            this.mWindowManager = (WindowManager) this.mContext.getSystemService("window");
            this.mDisplay = ((WindowManager) this.mContext.getSystemService("window")).getDefaultDisplay();
            this.mDisplay.getDisplayInfo(this.mInfo);
            this.mRotation = this.mDisplay.getRotation();
            this.mCurrentSize = new Point();
            this.mDisplay.getRealSize(this.mCurrentSize);
            addAspectWindow();
            ((ActivityObserver) Dependency.get(ActivityObserver.class)).addCallback(this.mActivityStateObserver);
            RecentsEventBus.getDefault().register(this);
            this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("force_fsg_nav_bar"), false, this.mFullScreenGestureListener);
            this.mFullScreenGestureListener.onChange(false);
            ((DisplayManager) this.mContext.getSystemService("display")).registerDisplayListener(new DisplayManager.DisplayListener() {
                public void onDisplayRemoved(int displayId) {
                }

                public void onDisplayChanged(int displayId) {
                    int rotation = AspectUI.this.mDisplay.getRotation();
                    if (AspectUI.this.mRotation != rotation) {
                        int unused = AspectUI.this.mRotation = rotation;
                        AspectUI.this.mDisplay.getDisplayInfo(AspectUI.this.mInfo);
                        Message msg = Message.obtain(AspectUI.this.mHandler, AspectUI.this.mUpdateAspectRunnable);
                        msg.setAsynchronous(true);
                        AspectUI.this.mHandler.sendMessageAtFrontOfQueue(msg);
                    }
                }

                public void onDisplayAdded(int displayId) {
                }
            }, this.mHandler);
            this.mLastConfiguration = new Configuration();
            this.mLastConfiguration.updateFrom(this.mContext.getResources().getConfiguration());
            ((ConfigurationController) Dependency.get(ConfigurationController.class)).addCallback(this);
            ((CommandQueue) SystemUI.getComponent(this.mContext, CommandQueue.class)).addCallbacks(this);
        }
    }

    private View addAspectWindow() {
        View frame = LayoutInflater.from(this.mContext).inflate(R.layout.aspect_window, null);
        this.mWindowHeight = getWindowHeight();
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(-2, this.mWindowHeight, 2003, 296, -2);
        WindowManagerCompat.setLayoutInDisplayCutoutMode(layoutParams, 1);
        layoutParams.setTitle("Aspect");
        layoutParams.gravity = 80;
        layoutParams.privateFlags |= 16;
        this.mWindowManager.addView(frame, layoutParams);
        frame.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                AspectUI.this.showAspectDialog();
            }
        });
        this.mAspectView = frame;
        updateAspectVisibility();
        return frame;
    }

    private int getWindowHeight() {
        Point point = new Point();
        this.mContext.getDisplay().getRealSize(point);
        return (Math.max(point.x, point.y) - DisplayCutoutCompat.getHeight(this.mInfo)) - ((int) (((double) (((float) Math.min(point.x, point.y)) * 1.86f)) + 0.5d));
    }

    /* access modifiers changed from: private */
    public void setAspectVisibility(boolean show, String pkg) {
        if (this.mShowAspect != show || !TextUtils.equals(pkg, this.mAspectPkg)) {
            this.mShowAspect = show;
            this.mAspectPkg = pkg;
            StatusBar statusBar = (StatusBar) ((Application) this.mContext.getApplicationContext()).getSystemUIApplication().getComponent(StatusBar.class);
            if (!(statusBar == null || statusBar.getNavigationBarView() == null)) {
                statusBar.getNavigationBarView().setAspectVisibility(this.mShowAspect);
            }
            updateAspectVisibility();
            if (!this.mShowAspect && this.mAspectDialog != null) {
                this.mAspectDialog.dismiss();
                this.mAspectDialog = null;
            }
        }
    }

    /* access modifiers changed from: private */
    public void updateAspectVisibility() {
        boolean wasVisible = true;
        int i = 0;
        boolean visible = this.mRotation == 0 && this.mIsFsgMode && this.mShowAspect && this.mWindowHeight > this.mMinWindowHeight && !this.mSoftInputVisible;
        if (this.mAspectView.getVisibility() != 0) {
            wasVisible = false;
        }
        if (wasVisible != visible) {
            View view = this.mAspectView;
            if (!visible) {
                i = 8;
            }
            view.setVisibility(i);
        }
    }

    /* access modifiers changed from: package-private */
    public void showAspectDialog() {
        if (this.mAspectDialog == null) {
            AlertDialog.Builder b = new AlertDialog.Builder(this.mContext, miui.R.style.Theme_Light_Dialog_Alert);
            b.setCancelable(true);
            b.setTitle(R.string.aspect_title);
            b.setMessage(R.string.aspect_message);
            b.setIconAttribute(16843605);
            b.setPositiveButton(R.string.aspect_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    MiuiInit.setRestrictAspect(AspectUI.this.mAspectPkg, false);
                    Intent intent = AspectUI.this.mContext.getPackageManager().getLaunchIntentForPackage(AspectUI.this.mAspectPkg);
                    if (intent != null) {
                        AspectUI.this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
                    }
                }
            });
            b.setNegativeButton(R.string.aspect_cancel, null);
            AlertDialog d = b.create();
            d.setOnDismissListener(new DialogInterface.OnDismissListener() {
                public void onDismiss(DialogInterface dialog) {
                    AlertDialog unused = AspectUI.this.mAspectDialog = null;
                }
            });
            d.getWindow().setType(2003);
            d.getWindow().addPrivateFlags(16);
            d.show();
            this.mAspectDialog = d;
        }
    }

    private void updateResource() {
        this.mMinWindowHeight = this.mContext.getResources().getDimensionPixelSize(R.dimen.min_aspect_window_height);
    }

    private void reCreateAspectWindow() {
        if (this.mAspectView != null) {
            this.mWindowManager.removeView(this.mAspectView);
            addAspectWindow();
        }
    }

    public void onConfigChanged(Configuration newConfig) {
        updateResource();
        if ((Integer.MIN_VALUE & this.mLastConfiguration.updateFrom(newConfig)) != 0) {
            Log.d("AspectUI", "recreate when assets change");
            this.mDisplay.getDisplayInfo(this.mInfo);
            reCreateAspectWindow();
        }
    }

    public void onDensityOrFontScaleChanged() {
        Log.d("AspectUI", "recreate when density change");
        this.mDisplay.getRealSize(this.mCurrentSize);
        reCreateAspectWindow();
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        super.dump(fd, pw, args);
        pw.print("  mAspectPkg=");
        pw.println(this.mAspectPkg);
        pw.print("  mShowAspect=");
        pw.println(this.mShowAspect);
        pw.print("  mMinWindowHeight=");
        pw.println(this.mMinWindowHeight);
        pw.print("  mWindowHeight=");
        pw.println(this.mWindowHeight);
        pw.print("  mRotation=");
        pw.println(this.mRotation);
        pw.print("  mIsFsgMode=");
        pw.println(this.mIsFsgMode);
        pw.print("  mSoftInputVisible=");
        pw.println(this.mSoftInputVisible);
    }

    public final void onBusEvent(AspectClickEvent event) {
        showAspectDialog();
        StatusBar statusBar = (StatusBar) ((Application) this.mContext.getApplicationContext()).getSystemUIApplication().getComponent(StatusBar.class);
        if (statusBar != null) {
            statusBar.animateCollapsePanels();
        }
    }
}
