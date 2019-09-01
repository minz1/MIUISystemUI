package com.android.systemui;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Looper;
import android.util.ArrayMap;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.util.Preconditions;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.systemui.Dependency;
import com.android.systemui.fragments.FragmentService;
import com.android.systemui.miui.Dependencies;
import com.android.systemui.miui.PackageEventReceiver;
import com.android.systemui.miui.statusbar.analytics.SystemUIStat;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.PluginDependencyProvider;
import com.android.systemui.plugins.PluginManager;
import com.android.systemui.plugins.PluginManagerImpl;
import com.android.systemui.plugins.VolumeDialogController;
import com.android.systemui.statusbar.CallStateController;
import com.android.systemui.statusbar.CallStateControllerImpl;
import com.android.systemui.statusbar.phone.ConfigurationControllerImpl;
import com.android.systemui.statusbar.phone.ManagedProfileController;
import com.android.systemui.statusbar.phone.ManagedProfileControllerImpl;
import com.android.systemui.statusbar.phone.StatusBarWindowManager;
import com.android.systemui.statusbar.policy.AccessibilityController;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.BatteryControllerImpl;
import com.android.systemui.statusbar.policy.BluetoothController;
import com.android.systemui.statusbar.policy.BluetoothControllerImpl;
import com.android.systemui.statusbar.policy.CastController;
import com.android.systemui.statusbar.policy.CastControllerImpl;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.DemoModeController;
import com.android.systemui.statusbar.policy.DemoModeControllerImpl;
import com.android.systemui.statusbar.policy.ExtensionController;
import com.android.systemui.statusbar.policy.ExtensionControllerImpl;
import com.android.systemui.statusbar.policy.FlashlightController;
import com.android.systemui.statusbar.policy.FlashlightControllerImpl;
import com.android.systemui.statusbar.policy.HotspotController;
import com.android.systemui.statusbar.policy.HotspotControllerImpl;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.android.systemui.statusbar.policy.KeyguardMonitorImpl;
import com.android.systemui.statusbar.policy.KeyguardNotificationController;
import com.android.systemui.statusbar.policy.KeyguardNotificationControllerImpl;
import com.android.systemui.statusbar.policy.LocationController;
import com.android.systemui.statusbar.policy.LocationControllerImpl;
import com.android.systemui.statusbar.policy.NextAlarmController;
import com.android.systemui.statusbar.policy.NextAlarmControllerImpl;
import com.android.systemui.statusbar.policy.PaperModeController;
import com.android.systemui.statusbar.policy.PaperModeControllerImpl;
import com.android.systemui.statusbar.policy.RotationLockController;
import com.android.systemui.statusbar.policy.RotationLockControllerImpl;
import com.android.systemui.statusbar.policy.SecurityController;
import com.android.systemui.statusbar.policy.SecurityControllerImpl;
import com.android.systemui.statusbar.policy.SilentModeObserverController;
import com.android.systemui.statusbar.policy.SilentModeObserverControllerImpl;
import com.android.systemui.statusbar.policy.UserInfoController;
import com.android.systemui.statusbar.policy.UserInfoControllerImpl;
import com.android.systemui.statusbar.policy.UserSwitcherController;
import com.android.systemui.statusbar.policy.ZenModeController;
import com.android.systemui.statusbar.policy.ZenModeControllerImpl;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.tuner.TunerServiceImpl;
import com.android.systemui.util.leak.GarbageMonitor;
import com.android.systemui.util.leak.LeakDetector;
import com.android.systemui.util.leak.LeakReporter;
import com.android.systemui.volume.VolumeDialogControllerImpl;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.HashMap;

public class DependencyUI extends SystemUI {
    /* access modifiers changed from: private */
    public static DependencyUI sDependency;
    private final ArrayMap<Object, Object> mDependencies = new ArrayMap<>();
    private final ArrayMap<Object, Dependency.DependencyProvider> mProviders = new ArrayMap<>();

    public void start() {
        sDependency = this;
        Dependency.setDependencyResolver(new Dependency.DependencyResolver() {
            public <T> T get(Class<T> cls) {
                return DependencyUI.sDependency.getDependency(cls);
            }

            public <T> T get(Dependency.DependencyKey<T> cls) {
                return DependencyUI.sDependency.getDependency(cls);
            }
        });
        this.mProviders.put(Dependency.TIME_TICK_HANDLER, new Dependency.DependencyProvider() {
            public Object createDependency() {
                return Dependencies.getInstance().get(Handler.class, "TimeTick");
            }
        });
        this.mProviders.put(Dependency.SCREEN_OFF_HANDLER, new Dependency.DependencyProvider() {
            public Object createDependency() {
                return Dependencies.getInstance().get(Handler.class, "ScreenOff");
            }
        });
        this.mProviders.put(Dependency.BG_LOOPER, new Dependency.DependencyProvider() {
            public Object createDependency() {
                return Dependencies.getInstance().get(Looper.class, "SysUiBg");
            }
        });
        this.mProviders.put(Dependency.NET_BG_LOOPER, new Dependency.DependencyProvider() {
            public Object createDependency() {
                return Dependencies.getInstance().get(Looper.class, "SysUiNetBg");
            }
        });
        this.mProviders.put(Dependency.BT_BG_LOOPER, new Dependency.DependencyProvider() {
            public Object createDependency() {
                return Dependencies.getInstance().get(Looper.class, "SysUiBtBg");
            }
        });
        this.mProviders.put(Dependency.MAIN_HANDLER, new Dependency.DependencyProvider() {
            public Object createDependency() {
                return Dependencies.getInstance().get(Handler.class, "main_handler");
            }
        });
        this.mProviders.put(ActivityStarter.class, new Dependency.DependencyProvider() {
            public Object createDependency() {
                return new ActivityStarterDelegate();
            }
        });
        this.mProviders.put(ActivityStarterDelegate.class, new Dependency.DependencyProvider() {
            public Object createDependency() {
                return DependencyUI.this.getDependency(ActivityStarter.class);
            }
        });
        this.mProviders.put(BluetoothController.class, new Dependency.DependencyProvider() {
            public Object createDependency() {
                return new BluetoothControllerImpl(DependencyUI.this.mContext, (Looper) DependencyUI.this.getDependency(Dependency.BT_BG_LOOPER));
            }
        });
        this.mProviders.put(LocationController.class, new Dependency.DependencyProvider() {
            public Object createDependency() {
                return new LocationControllerImpl(DependencyUI.this.mContext, (Looper) DependencyUI.this.getDependency(Dependency.BG_LOOPER));
            }
        });
        this.mProviders.put(RotationLockController.class, new Dependency.DependencyProvider() {
            public Object createDependency() {
                return new RotationLockControllerImpl(DependencyUI.this.mContext);
            }
        });
        this.mProviders.put(ZenModeController.class, new Dependency.DependencyProvider() {
            public Object createDependency() {
                return new ZenModeControllerImpl(DependencyUI.this.mContext, (Handler) DependencyUI.this.getDependency(Dependency.MAIN_HANDLER));
            }
        });
        this.mProviders.put(HotspotController.class, new Dependency.DependencyProvider() {
            public Object createDependency() {
                return new HotspotControllerImpl(DependencyUI.this.mContext);
            }
        });
        this.mProviders.put(CastController.class, new Dependency.DependencyProvider() {
            public Object createDependency() {
                return new CastControllerImpl(DependencyUI.this.mContext);
            }
        });
        this.mProviders.put(PaperModeController.class, new Dependency.DependencyProvider() {
            public Object createDependency() {
                return new PaperModeControllerImpl(DependencyUI.this.mContext, (Looper) DependencyUI.this.getDependency(Dependency.NET_BG_LOOPER));
            }
        });
        this.mProviders.put(FlashlightController.class, new Dependency.DependencyProvider() {
            public Object createDependency() {
                return new FlashlightControllerImpl(DependencyUI.this.mContext);
            }
        });
        this.mProviders.put(KeyguardMonitor.class, new Dependency.DependencyProvider() {
            public Object createDependency() {
                return new KeyguardMonitorImpl(DependencyUI.this.mContext);
            }
        });
        this.mProviders.put(UserSwitcherController.class, new Dependency.DependencyProvider() {
            public Object createDependency() {
                return new UserSwitcherController(DependencyUI.this.mContext, (KeyguardMonitor) DependencyUI.this.getDependency(KeyguardMonitor.class), (Handler) DependencyUI.this.getDependency(Dependency.MAIN_HANDLER), (ActivityStarter) DependencyUI.this.getDependency(ActivityStarter.class));
            }
        });
        this.mProviders.put(UserInfoController.class, new Dependency.DependencyProvider() {
            public Object createDependency() {
                return new UserInfoControllerImpl(DependencyUI.this.mContext);
            }
        });
        this.mProviders.put(BatteryController.class, new Dependency.DependencyProvider() {
            public Object createDependency() {
                return new BatteryControllerImpl(DependencyUI.this.mContext);
            }
        });
        this.mProviders.put(ManagedProfileController.class, new Dependency.DependencyProvider() {
            public Object createDependency() {
                return new ManagedProfileControllerImpl(DependencyUI.this.mContext);
            }
        });
        this.mProviders.put(NextAlarmController.class, new Dependency.DependencyProvider() {
            public Object createDependency() {
                return new NextAlarmControllerImpl(DependencyUI.this.mContext);
            }
        });
        this.mProviders.put(AccessibilityController.class, new Dependency.DependencyProvider() {
            public Object createDependency() {
                return new AccessibilityController(DependencyUI.this.mContext);
            }
        });
        this.mProviders.put(PluginManager.class, new Dependency.DependencyProvider() {
            public Object createDependency() {
                return new PluginManagerImpl(DependencyUI.this.mContext);
            }
        });
        this.mProviders.put(SecurityController.class, new Dependency.DependencyProvider() {
            public Object createDependency() {
                return new SecurityControllerImpl(DependencyUI.this.mContext);
            }
        });
        this.mProviders.put(LeakDetector.class, new Dependency.DependencyProvider() {
            public Object createDependency() {
                return LeakDetector.create();
            }
        });
        this.mProviders.put(Dependency.LEAK_REPORT_EMAIL, new Dependency.DependencyProvider() {
            public Object createDependency() {
                return null;
            }
        });
        this.mProviders.put(LeakReporter.class, new Dependency.DependencyProvider() {
            public Object createDependency() {
                return new LeakReporter(DependencyUI.this.mContext, (LeakDetector) DependencyUI.this.getDependency(LeakDetector.class), (String) DependencyUI.this.getDependency(Dependency.LEAK_REPORT_EMAIL));
            }
        });
        this.mProviders.put(GarbageMonitor.class, new Dependency.DependencyProvider() {
            public Object createDependency() {
                return new GarbageMonitor((Looper) DependencyUI.this.getDependency(Dependency.BG_LOOPER), (LeakDetector) DependencyUI.this.getDependency(LeakDetector.class), (LeakReporter) DependencyUI.this.getDependency(LeakReporter.class));
            }
        });
        this.mProviders.put(TunerService.class, new Dependency.DependencyProvider() {
            public Object createDependency() {
                return new TunerServiceImpl(DependencyUI.this.mContext);
            }
        });
        this.mProviders.put(StatusBarWindowManager.class, new Dependency.DependencyProvider() {
            public Object createDependency() {
                return new StatusBarWindowManager(DependencyUI.this.mContext);
            }
        });
        this.mProviders.put(ConfigurationController.class, new Dependency.DependencyProvider() {
            public Object createDependency() {
                return new ConfigurationControllerImpl(DependencyUI.this.mContext);
            }
        });
        this.mProviders.put(FragmentService.class, new Dependency.DependencyProvider() {
            public Object createDependency() {
                return new FragmentService(DependencyUI.this.mContext);
            }
        });
        this.mProviders.put(ExtensionController.class, new Dependency.DependencyProvider() {
            public Object createDependency() {
                return new ExtensionControllerImpl();
            }
        });
        this.mProviders.put(PluginDependencyProvider.class, new Dependency.DependencyProvider() {
            public Object createDependency() {
                return new PluginDependencyProvider((PluginManager) Dependency.get(PluginManager.class));
            }
        });
        this.mProviders.put(LocalBluetoothManager.class, new Dependency.DependencyProvider() {
            public Object createDependency() {
                return LocalBluetoothManager.getInstance(DependencyUI.this.mContext, null);
            }
        });
        this.mProviders.put(VolumeDialogController.class, new Dependency.DependencyProvider() {
            public Object createDependency() {
                return new VolumeDialogControllerImpl(DependencyUI.this.mContext);
            }
        });
        this.mProviders.put(MetricsLogger.class, new Dependency.DependencyProvider() {
            public Object createDependency() {
                return new MetricsLogger();
            }
        });
        this.mProviders.put(ForegroundServiceController.class, new Dependency.DependencyProvider() {
            public Object createDependency() {
                return new ForegroundServiceControllerImpl(DependencyUI.this.mContext);
            }
        });
        this.mProviders.put(UiOffloadThread.class, new Dependency.DependencyProvider() {
            public Object createDependency() {
                return new UiOffloadThread();
            }
        });
        this.mProviders.put(DemoModeController.class, new Dependency.DependencyProvider() {
            public Object createDependency() {
                return new DemoModeControllerImpl(DependencyUI.this.mContext);
            }
        });
        this.mProviders.put(SilentModeObserverController.class, new Dependency.DependencyProvider() {
            public Object createDependency() {
                return new SilentModeObserverControllerImpl(DependencyUI.this.mContext);
            }
        });
        this.mProviders.put(KeyguardNotificationController.class, new Dependency.DependencyProvider() {
            public Object createDependency() {
                return new KeyguardNotificationControllerImpl();
            }
        });
        this.mProviders.put(CallStateController.class, new Dependency.DependencyProvider() {
            public Object createDependency() {
                return new CallStateControllerImpl();
            }
        });
        this.mProviders.put(SystemUIStat.class, new Dependency.DependencyProvider() {
            public Object createDependency() {
                return new SystemUIStat(DependencyUI.this.mContext);
            }
        });
        SystemUIFactory.getInstance().injectDependencies(this.mProviders, this.mContext);
    }

    public synchronized void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        super.dump(fd, pw, args);
        pw.println("Dumping existing controllers:");
        for (Object o : this.mDependencies.values()) {
            if (o instanceof Dumpable) {
                ((Dumpable) o).dump(fd, pw, args);
            }
        }
    }

    /* access modifiers changed from: protected */
    public synchronized void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        for (Object o : this.mDependencies.values()) {
            if (o instanceof ConfigurationChangedReceiver) {
                ((ConfigurationChangedReceiver) o).onConfigurationChanged(newConfig);
            }
        }
    }

    public void onPackageChanged(int uid, String packageName) {
        for (Object o : this.mDependencies.values()) {
            if (o instanceof PackageEventReceiver) {
                ((PackageEventReceiver) o).onPackageChanged(uid, packageName);
            }
        }
    }

    public void onPackageAdded(int uid, String packageName, boolean replacing) {
        for (Object o : this.mDependencies.values()) {
            if (o instanceof PackageEventReceiver) {
                ((PackageEventReceiver) o).onPackageAdded(uid, packageName, replacing);
            }
        }
    }

    public void onPackageRemoved(int uid, String packageName, boolean dataRemoved, boolean replacing) {
        for (Object o : this.mDependencies.values()) {
            if (o instanceof PackageEventReceiver) {
                ((PackageEventReceiver) o).onPackageRemoved(uid, packageName, dataRemoved, replacing);
            }
        }
    }

    /* access modifiers changed from: protected */
    public final <T> T getDependency(Class<T> cls) {
        return getDependencyInner(cls);
    }

    /* access modifiers changed from: protected */
    public final <T> T getDependency(Dependency.DependencyKey<T> key) {
        return getDependencyInner(key);
    }

    private synchronized <T> T getDependencyInner(Object key) {
        T obj;
        obj = this.mDependencies.get(key);
        if (obj == null) {
            obj = createDependency(key);
            this.mDependencies.put(key, obj);
        }
        return obj;
    }

    /* access modifiers changed from: protected */
    @VisibleForTesting
    public <T> T createDependency(Object cls) {
        Preconditions.checkArgument((cls instanceof Dependency.DependencyKey) || (cls instanceof Class));
        Dependency.DependencyProvider<T> provider = this.mProviders.get(cls);
        if (provider != null) {
            return provider.createDependency();
        }
        if (cls instanceof Class) {
            if (Constants.DEBUG) {
                Log.i("Dependency", "get dependency from injected : " + cls);
            }
            return Dependencies.getInstance().get((Class) cls, "");
        }
        throw new IllegalArgumentException("Unsupported dependency " + cls);
    }

    public static void initDependencies(Context context) {
        if (sDependency == null) {
            DependencyUI d = new DependencyUI();
            d.mContext = context;
            d.mComponents = new HashMap();
            d.start();
        }
    }
}
