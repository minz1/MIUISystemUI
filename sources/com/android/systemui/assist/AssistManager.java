package com.android.systemui.assist;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.ImageView;
import com.android.internal.app.AssistUtils;
import com.android.internal.app.IVoiceInteractionSessionShowCallback;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.ConfigurationChangedReceiver;
import com.android.systemui.R;
import com.android.systemui.SystemUI;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import com.android.systemui.util.InterestingConfigChanges;
import com.miui.systemui.annotation.Inject;

public class AssistManager implements ConfigurationChangedReceiver {
    private final AssistDisclosure mAssistDisclosure;
    protected final AssistUtils mAssistUtils;
    protected final Context mContext;
    private final DeviceProvisionedController mDeviceProvisionedController;
    /* access modifiers changed from: private */
    public Runnable mHideRunnable = new Runnable() {
        public void run() {
            AssistManager.this.mView.removeCallbacks(this);
            AssistManager.this.mView.show(false, true);
        }
    };
    private final InterestingConfigChanges mInterestingConfigChanges;
    private IVoiceInteractionSessionShowCallback mShowCallback = new IVoiceInteractionSessionShowCallback.Stub() {
        public void onFailed() throws RemoteException {
            AssistManager.this.mView.post(AssistManager.this.mHideRunnable);
        }

        public void onShown() throws RemoteException {
            AssistManager.this.mView.post(AssistManager.this.mHideRunnable);
        }
    };
    /* access modifiers changed from: private */
    public AssistOrbContainer mView;
    private final WindowManager mWindowManager;

    public AssistManager(@Inject DeviceProvisionedController controller, @Inject Context context) {
        this.mContext = context;
        this.mDeviceProvisionedController = controller;
        this.mWindowManager = (WindowManager) this.mContext.getSystemService("window");
        this.mAssistUtils = new AssistUtils(context);
        this.mAssistDisclosure = new AssistDisclosure(context, new Handler());
        this.mInterestingConfigChanges = new InterestingConfigChanges(-2147482748);
        onConfigurationChanged(context.getResources().getConfiguration());
    }

    public void onConfigurationChanged(Configuration newConfiguration) {
        if (this.mInterestingConfigChanges.applyNewConfig(this.mContext.getResources())) {
            boolean visible = false;
            if (this.mView != null) {
                visible = this.mView.isShowing();
                this.mWindowManager.removeView(this.mView);
            }
            this.mView = (AssistOrbContainer) LayoutInflater.from(this.mContext).inflate(R.layout.assist_orb, null);
            this.mView.setVisibility(8);
            this.mView.setSystemUiVisibility(1792);
            this.mWindowManager.addView(this.mView, getLayoutParams());
            if (visible) {
                this.mView.show(true, false);
            }
        }
    }

    /* access modifiers changed from: protected */
    public boolean shouldShowOrb() {
        return true;
    }

    public void startAssist(Bundle args) {
        long j;
        ComponentName assistComponent = getAssistInfo();
        if (assistComponent != null) {
            boolean isService = assistComponent.equals(getVoiceInteractorComponentName());
            if (!isService || (!isVoiceSessionRunning() && shouldShowOrb())) {
                showOrb(assistComponent, isService);
                AssistOrbContainer assistOrbContainer = this.mView;
                Runnable runnable = this.mHideRunnable;
                if (isService) {
                    j = 2500;
                } else {
                    j = 1000;
                }
                assistOrbContainer.postDelayed(runnable, j);
            }
            startAssistInternal(args, assistComponent, isService);
        }
    }

    public void hideAssist() {
        this.mAssistUtils.hideCurrentSession();
    }

    private WindowManager.LayoutParams getLayoutParams() {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(-1, this.mContext.getResources().getDimensionPixelSize(R.dimen.assist_orb_scrim_height), 2033, 280, -3);
        lp.token = new Binder();
        if (ActivityManager.isHighEndGfx()) {
            lp.flags |= 16777216;
        }
        lp.gravity = 8388691;
        lp.setTitle("AssistPreviewPanel");
        lp.softInputMode = 49;
        return lp;
    }

    private void showOrb(ComponentName assistComponent, boolean isService) {
        maybeSwapSearchIcon(assistComponent, isService);
        this.mView.show(true, true);
    }

    private void startAssistInternal(Bundle args, ComponentName assistComponent, boolean isService) {
        if (isService) {
            startVoiceInteractor(args);
        } else {
            startAssistActivity(args, assistComponent);
        }
    }

    private void startAssistActivity(Bundle args, ComponentName assistComponent) {
        if (this.mDeviceProvisionedController.isDeviceProvisioned()) {
            ((CommandQueue) SystemUI.getComponent(this.mContext, CommandQueue.class)).animateCollapsePanels(3);
            boolean z = true;
            if (Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "assist_structure_enabled", 1, -2) == 0) {
                z = false;
            }
            boolean structureEnabled = z;
            final Intent intent = ((SearchManager) this.mContext.getSystemService("search")).getAssistIntent(structureEnabled);
            if (intent != null) {
                intent.setComponent(assistComponent);
                intent.putExtras(args);
                if (structureEnabled) {
                    showDisclosure();
                }
                try {
                    final ActivityOptions opts = ActivityOptions.makeCustomAnimation(this.mContext, R.anim.search_launch_enter, R.anim.search_launch_exit);
                    intent.addFlags(268435456);
                    AsyncTask.execute(new Runnable() {
                        public void run() {
                            AssistManager.this.mContext.startActivityAsUser(intent, opts.toBundle(), new UserHandle(-2));
                        }
                    });
                } catch (ActivityNotFoundException e) {
                    Log.w("AssistManager", "Activity not found for " + intent.getAction());
                }
            }
        }
    }

    private void startVoiceInteractor(Bundle args) {
        try {
            this.mAssistUtils.showSessionForActiveService(args, 4, this.mShowCallback, null);
        } catch (Exception e) {
            Log.e("AssistManager", "Failed to startVoiceInteractor", e);
        }
    }

    public ComponentName getVoiceInteractorComponentName() {
        return this.mAssistUtils.getActiveServiceComponentName();
    }

    private boolean isVoiceSessionRunning() {
        return this.mAssistUtils.isSessionRunning();
    }

    private void maybeSwapSearchIcon(ComponentName assistComponent, boolean isService) {
        replaceDrawable(this.mView.getOrb().getLogo(), assistComponent, "com.android.systemui.action_assist_icon", isService);
    }

    public void replaceDrawable(ImageView v, ComponentName component, String name, boolean isService) {
        Bundle metaData;
        if (component != null) {
            try {
                PackageManager packageManager = this.mContext.getPackageManager();
                if (isService) {
                    metaData = packageManager.getServiceInfo(component, 128).metaData;
                } else {
                    metaData = packageManager.getActivityInfo(component, 128).metaData;
                }
                if (metaData != null) {
                    int iconResId = metaData.getInt(name);
                    if (iconResId != 0) {
                        v.setImageDrawable(packageManager.getResourcesForApplication(component.getPackageName()).getDrawable(iconResId));
                        return;
                    }
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.v("AssistManager", "Assistant component " + component.flattenToShortString() + " not found");
            } catch (Resources.NotFoundException nfe) {
                Log.w("AssistManager", "Failed to swap drawable from " + component.flattenToShortString(), nfe);
            }
        }
        v.setImageDrawable(null);
    }

    private ComponentName getAssistInfo() {
        return this.mAssistUtils.getAssistComponentForUser(KeyguardUpdateMonitor.getCurrentUser());
    }

    public void showDisclosure() {
        this.mAssistDisclosure.postShow();
    }

    public void onLockscreenShown() {
        this.mAssistUtils.onLockscreenShown();
    }
}
