package com.android.systemui.volume;

import android.content.Context;
import android.content.res.Configuration;
import android.media.VolumePolicy;
import android.os.Bundle;
import android.os.Handler;
import com.android.systemui.Dependency;
import com.android.systemui.Dumpable;
import com.android.systemui.SystemUI;
import com.android.systemui.keyguard.KeyguardViewMediator;
import com.android.systemui.miui.volume.MiuiVolumeDialogImpl;
import com.android.systemui.plugins.PluginDependencyProvider;
import com.android.systemui.plugins.VolumeDialog;
import com.android.systemui.plugins.VolumeDialogController;
import com.android.systemui.statusbar.policy.ExtensionController;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.util.function.Consumer;
import com.android.systemui.util.function.Supplier;
import com.android.systemui.volume.VolumeDialogControllerImpl;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class VolumeDialogComponent implements TunerService.Tunable, VolumeComponent, VolumeDialogControllerImpl.UserActivityListener {
    private final Context mContext;
    private final VolumeDialogControllerImpl mController;
    /* access modifiers changed from: private */
    public VolumeDialog mDialog;
    private final ExtensionController.Extension mExtension;
    private final SystemUI mSysui;
    /* access modifiers changed from: private */
    public final VolumeDialog.Callback mVolumeDialogCallback = new VolumeDialog.Callback() {
        public void onZenSettingsClicked() {
        }

        public void onZenPrioritySettingsClicked() {
        }
    };
    private VolumePolicy mVolumePolicy = new VolumePolicy(true, true, true, 400);

    public VolumeDialogComponent(SystemUI sysui, Context context, Handler handler) {
        this.mSysui = sysui;
        this.mContext = context;
        this.mController = (VolumeDialogControllerImpl) Dependency.get(VolumeDialogController.class);
        this.mController.setUserActivityListener(this);
        ((PluginDependencyProvider) Dependency.get(PluginDependencyProvider.class)).allowPluginDependency(VolumeDialogController.class);
        this.mExtension = ((ExtensionController) Dependency.get(ExtensionController.class)).newExtension(VolumeDialog.class).withPlugin(VolumeDialog.class).withDefault(new Supplier<VolumeDialog>() {
            public VolumeDialog get() {
                return VolumeDialogComponent.this.createDefault();
            }
        }).withCallback(new Consumer<VolumeDialog>() {
            public void accept(VolumeDialog dialog) {
                if (VolumeDialogComponent.this.mDialog != null) {
                    VolumeDialogComponent.this.mDialog.destroy();
                }
                VolumeDialog unused = VolumeDialogComponent.this.mDialog = dialog;
                VolumeDialogComponent.this.mDialog.init(2020, VolumeDialogComponent.this.mVolumeDialogCallback);
            }
        }).build();
        applyConfiguration();
        ((TunerService) Dependency.get(TunerService.class)).addTunable(this, "sysui_volume_down_silent", "sysui_volume_up_silent", "sysui_do_not_disturb");
    }

    /* access modifiers changed from: private */
    public VolumeDialog createDefault() {
        MiuiVolumeDialogImpl impl = new MiuiVolumeDialogImpl(this.mContext);
        impl.setStreamImportant(4, true);
        impl.setStreamImportant(1, false);
        impl.setAutomute(true);
        impl.setSilentMode(false);
        return impl;
    }

    public void onTuningChanged(String key, String newValue) {
        boolean volumeDownToEnterSilent = true;
        if ("sysui_volume_down_silent".equals(key)) {
            if (newValue != null && Integer.parseInt(newValue) == 0) {
                volumeDownToEnterSilent = false;
            }
            setVolumePolicy(volumeDownToEnterSilent, this.mVolumePolicy.volumeUpToExitSilent, this.mVolumePolicy.doNotDisturbWhenSilent, this.mVolumePolicy.vibrateToSilentDebounce);
        } else if ("sysui_volume_up_silent".equals(key)) {
            if (newValue != null && Integer.parseInt(newValue) == 0) {
                volumeDownToEnterSilent = false;
            }
            setVolumePolicy(this.mVolumePolicy.volumeDownToEnterSilent, volumeDownToEnterSilent, this.mVolumePolicy.doNotDisturbWhenSilent, this.mVolumePolicy.vibrateToSilentDebounce);
        } else if ("sysui_do_not_disturb".equals(key)) {
            if (newValue != null && Integer.parseInt(newValue) == 0) {
                volumeDownToEnterSilent = false;
            }
            setVolumePolicy(this.mVolumePolicy.volumeDownToEnterSilent, this.mVolumePolicy.volumeUpToExitSilent, volumeDownToEnterSilent, this.mVolumePolicy.vibrateToSilentDebounce);
        }
    }

    private void setVolumePolicy(boolean volumeDownToEnterSilent, boolean volumeUpToExitSilent, boolean doNotDisturbWhenSilent, int vibrateToSilentDebounce) {
        this.mVolumePolicy = new VolumePolicy(volumeDownToEnterSilent, volumeUpToExitSilent, doNotDisturbWhenSilent, vibrateToSilentDebounce);
        this.mController.setVolumePolicy(this.mVolumePolicy);
    }

    public void onUserActivity() {
        KeyguardViewMediator kvm = (KeyguardViewMediator) this.mSysui.getComponent(KeyguardViewMediator.class);
        if (kvm != null) {
            kvm.userActivity();
        }
    }

    private void applyConfiguration() {
        this.mController.setVolumePolicy(this.mVolumePolicy);
        this.mController.showDndTile(true);
    }

    public void onConfigurationChanged(Configuration newConfig) {
    }

    public void dismissNow() {
        this.mController.dismiss();
    }

    public void dispatchDemoCommand(String command, Bundle args) {
    }

    public void register() {
        this.mController.register();
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (this.mDialog instanceof Dumpable) {
            ((Dumpable) this.mDialog).dump(fd, pw, args);
        }
    }
}
