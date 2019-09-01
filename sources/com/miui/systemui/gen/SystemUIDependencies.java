package com.miui.systemui.gen;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.android.systemui.aspect.AspectUI;
import com.android.systemui.assist.AssistManager;
import com.android.systemui.fingerprint.FingerprintDialogImpl;
import com.android.systemui.globalactions.GlobalActionsComponent;
import com.android.systemui.keyboard.KeyboardUI;
import com.android.systemui.miui.statusbar.DependenciesSetup;
import com.android.systemui.miui.statusbar.phone.MiuiStatusBarPromptController;
import com.android.systemui.miui.statusbar.phone.rank.PackageScoreCache;
import com.android.systemui.miui.statusbar.policy.UsbNotificationController;
import com.android.systemui.pip.PipUI;
import com.android.systemui.shortcut.ShortcutKeyDispatcher;
import com.android.systemui.statusbar.NotificationLogger;
import com.android.systemui.statusbar.phone.DarkIconDispatcherImpl;
import com.android.systemui.statusbar.phone.StatusBarIconController;
import com.android.systemui.statusbar.phone.StatusBarIconControllerImpl;
import com.android.systemui.statusbar.policy.DarkIconDispatcher;
import com.android.systemui.statusbar.policy.DataSaverController;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import com.android.systemui.statusbar.policy.DeviceProvisionedControllerImpl;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.NetworkControllerImpl;
import com.miui.systemui.dependencies.BaseResolver;
import com.miui.systemui.dependencies.Provider;
import com.miui.systemui.dependencies.Resolver;

public final class SystemUIDependencies extends BaseResolver {
    public SystemUIDependencies() {
        put(Context.class, "", new Provider<Context>() {
            public final Context create(Resolver resolver) {
                return ((DependenciesSetup) resolver.get(DependenciesSetup.class, "")).getContext();
            }
        });
        put(Handler.class, "ScreenOff", new Provider<Handler>() {
            public final Handler create(Resolver resolver) {
                return ((DependenciesSetup) resolver.get(DependenciesSetup.class, "")).getScreenOffHandler();
            }
        });
        put(Handler.class, "TimeTick", new Provider<Handler>() {
            public final Handler create(Resolver resolver) {
                return ((DependenciesSetup) resolver.get(DependenciesSetup.class, "")).getTimeTickHandler();
            }
        });
        put(Handler.class, "main_handler", new Provider<Handler>() {
            public final Handler create(Resolver resolver) {
                return ((DependenciesSetup) resolver.get(DependenciesSetup.class, "")).getMainHandler();
            }
        });
        put(Looper.class, "SysUiBg", new Provider<Looper>() {
            public final Looper create(Resolver resolver) {
                return ((DependenciesSetup) resolver.get(DependenciesSetup.class, "")).getSysUIBgLooper();
            }
        });
        put(Looper.class, "SysUiBtBg", new Provider<Looper>() {
            public final Looper create(Resolver resolver) {
                return ((DependenciesSetup) resolver.get(DependenciesSetup.class, "")).getBtBgLooper();
            }
        });
        put(Looper.class, "SysUiNetBg", new Provider<Looper>() {
            public final Looper create(Resolver resolver) {
                return ((DependenciesSetup) resolver.get(DependenciesSetup.class, "")).getNetBgLooper();
            }
        });
        put(AssistManager.class, "", new Provider<AssistManager>() {
            public final AssistManager create(Resolver resolver) {
                return new AssistManager((DeviceProvisionedController) resolver.get(DeviceProvisionedController.class, ""), (Context) resolver.get(Context.class, ""));
            }
        });
        put(DependenciesSetup.class, "", new Provider<DependenciesSetup>() {
            public final DependenciesSetup create(Resolver resolver) {
                return new DependenciesSetup();
            }
        });
        put(MiuiStatusBarPromptController.class, "", new Provider<MiuiStatusBarPromptController>() {
            public final MiuiStatusBarPromptController create(Resolver resolver) {
                return new MiuiStatusBarPromptController();
            }
        });
        put(PackageScoreCache.class, "", new Provider<PackageScoreCache>() {
            public final PackageScoreCache create(Resolver resolver) {
                return new PackageScoreCache((Context) resolver.get(Context.class, ""), (Looper) resolver.get(Looper.class, "SysUiBg"));
            }
        });
        put(UsbNotificationController.class, "", new Provider<UsbNotificationController>() {
            public final UsbNotificationController create(Resolver resolver) {
                return new UsbNotificationController((Context) resolver.get(Context.class, ""));
            }
        });
        put(NotificationLogger.class, "", new Provider<NotificationLogger>() {
            public final NotificationLogger create(Resolver resolver) {
                return new NotificationLogger();
            }
        });
        put(StatusBarIconController.class, "", new Provider<StatusBarIconController>() {
            public final StatusBarIconController create(Resolver resolver) {
                return new StatusBarIconControllerImpl((Context) resolver.get(Context.class, ""));
            }
        });
        put(DarkIconDispatcher.class, "", new Provider<DarkIconDispatcher>() {
            public final DarkIconDispatcher create(Resolver resolver) {
                return new DarkIconDispatcherImpl((Context) resolver.get(Context.class, ""));
            }
        });
        put(DataSaverController.class, "", new Provider<DataSaverController>() {
            public final DataSaverController create(Resolver resolver) {
                return ((NetworkController) resolver.get(NetworkController.class, "")).getDataSaverController();
            }
        });
        put(DeviceProvisionedController.class, "", new Provider<DeviceProvisionedController>() {
            public final DeviceProvisionedController create(Resolver resolver) {
                return new DeviceProvisionedControllerImpl((Context) resolver.get(Context.class, ""));
            }
        });
        put(NetworkController.class, "", new Provider<NetworkController>() {
            public final NetworkController create(Resolver resolver) {
                return new NetworkControllerImpl((Context) resolver.get(Context.class, ""), (Looper) resolver.get(Looper.class, "SysUiNetBg"), (DeviceProvisionedController) resolver.get(DeviceProvisionedController.class, ""));
            }
        });
        put(Class.class, "com.android.systemui.SystemUI_aspect", new Provider<Class>() {
            public final Class create(Resolver resolver) {
                return AspectUI.class;
            }
        });
        put(Class.class, "com.android.systemui.SystemUI_fingerprint_dialog", new Provider<Class>() {
            public final Class create(Resolver resolver) {
                return FingerprintDialogImpl.class;
            }
        });
        put(Class.class, "com.android.systemui.SystemUI_global_action", new Provider<Class>() {
            public final Class create(Resolver resolver) {
                return GlobalActionsComponent.class;
            }
        });
        put(Class.class, "com.android.systemui.SystemUI_keyboard_ui", new Provider<Class>() {
            public final Class create(Resolver resolver) {
                return KeyboardUI.class;
            }
        });
        put(Class.class, "com.android.systemui.SystemUI_pip", new Provider<Class>() {
            public final Class create(Resolver resolver) {
                return PipUI.class;
            }
        });
        put(Class.class, "com.android.systemui.SystemUI_shortcut_key", new Provider<Class>() {
            public final Class create(Resolver resolver) {
                return ShortcutKeyDispatcher.class;
            }
        });
    }
}
