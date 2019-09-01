package com.android.systemui;

import android.app.ActivityThread;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.os.Process;
import android.os.SystemProperties;
import android.os.UserHandleCompat;
import android.util.ArraySet;
import android.util.Log;
import com.android.systemui.keyguard.KeyguardViewMediator;
import com.android.systemui.media.RingtonePlayer;
import com.android.systemui.miui.Dependencies;
import com.android.systemui.miui.PackageEventController;
import com.android.systemui.miui.PackageEventReceiver;
import com.android.systemui.miui.analytics.AnalyticsWrapper;
import com.android.systemui.miui.statusbar.DependenciesSetup;
import com.android.systemui.plugins.OverlayPlugin;
import com.android.systemui.plugins.PluginListener;
import com.android.systemui.plugins.PluginManager;
import com.android.systemui.power.PowerUI;
import com.android.systemui.recents.Recents;
import com.android.systemui.stackdivider.Divider;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.phone.StatusBarWindowManager;
import com.android.systemui.statusbar.policy.EncryptionHelper;
import com.android.systemui.usb.StorageNotification;
import com.android.systemui.util.NotificationChannels;
import com.android.systemui.util.Utils;
import com.android.systemui.util.leak.GarbageMonitor;
import com.android.systemui.volume.VolumeUI;
import com.miui.systemui.gen.SystemUIDependencies;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import miui.external.ApplicationDelegate;

public class SystemUIApplication extends ApplicationDelegate implements PackageEventReceiver {
    private final Class<?>[] BASE_SERVICES = {DependencyUI.class, NotificationChannels.class, CommandQueue.CommandQueueStart.class, KeyguardViewMediator.class, Recents.class, VolumeUI.class, Divider.class, SystemBars.class, StorageNotification.class, PowerUI.class, RingtonePlayer.class, VendorServices.class, GarbageMonitor.Service.class, LatencyTester.class, RoundedCorners.class};
    private final Class<?>[] SERVICES = ((Class[]) Utils.arrayConcat(this.BASE_SERVICES, (Class[]) Dependencies.getInstance().getAllClassesFor(SystemUI.class).toArray(new Class[0])));
    private final Class<?>[] SERVICES_PER_USER = {DependencyUI.class, NotificationChannels.class, Recents.class};
    /* access modifiers changed from: private */
    public boolean mBootCompleted;
    private final Map<Class<?>, Object> mComponents = new HashMap();
    /* access modifiers changed from: private */
    public SystemUI[] mServices = new SystemUI[this.SERVICES.length];
    /* access modifiers changed from: private */
    public boolean mServicesStarted;

    static {
        Dependencies.initialize(new SystemUIDependencies());
    }

    public void onCreate() {
        super.onCreate();
        setTheme(R.style.Theme);
        ((DependenciesSetup) Dependencies.getInstance().get(DependenciesSetup.class, "")).setContext(this);
        SystemUIFactory.createFromConfig(this);
        AnalyticsWrapper.init(this);
        if (Process.myUserHandle().equals(UserHandleCompat.SYSTEM)) {
            IntentFilter bootCompletedFilter = new IntentFilter("android.intent.action.BOOT_COMPLETED");
            bootCompletedFilter.setPriority(1000);
            registerReceiver(new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    if (!SystemUIApplication.this.mBootCompleted) {
                        Log.v("SystemUIService", "BOOT_COMPLETED received");
                        SystemUIApplication.this.unregisterReceiver(this);
                        boolean unused = SystemUIApplication.this.mBootCompleted = true;
                        if (SystemUIApplication.this.mServicesStarted) {
                            for (SystemUI onBootCompleted : SystemUIApplication.this.mServices) {
                                onBootCompleted.onBootCompleted();
                            }
                        }
                    }
                }
            }, bootCompletedFilter);
            registerReceiver(new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    if ("android.intent.action.LOCALE_CHANGED".equals(intent.getAction()) && SystemUIApplication.this.mBootCompleted) {
                        NotificationChannels.createAll(context);
                    }
                }
            }, new IntentFilter("android.intent.action.LOCALE_CHANGED"));
        } else {
            String processName = ActivityThread.currentProcessName();
            ApplicationInfo info = getApplicationInfo();
            if (processName != null) {
                if (processName.startsWith(info.processName + ":")) {
                    return;
                }
            }
            startServicesIfNeeded(this.SERVICES_PER_USER);
        }
        new PackageEventController(this, this, null).start();
    }

    public void startServicesIfNeeded() {
        startServicesIfNeeded(this.SERVICES);
    }

    /* access modifiers changed from: package-private */
    public void startSecondaryUserServicesIfNeeded() {
        startServicesIfNeeded(this.SERVICES_PER_USER);
    }

    private void startServicesIfNeeded(Class<?>[] services) {
        if (EncryptionHelper.systemNotReady()) {
            Log.e("SystemUIService", "abort starting service, system not ready due to data encryption");
        } else if (!this.mServicesStarted) {
            if (!this.mBootCompleted && "1".equals(SystemProperties.get("sys.boot_completed"))) {
                this.mBootCompleted = true;
                Log.v("SystemUIService", "BOOT_COMPLETED was already sent");
            }
            Log.v("SystemUIService", "Starting SystemUI services for user " + Process.myUserHandle().getIdentifier() + ".");
            int N = services.length;
            int i = 0;
            while (i < N) {
                Class<?> cl = services[i];
                Log.d("SystemUIService", "loading: " + cl);
                try {
                    Object newService = SystemUIFactory.getInstance().createInstance(cl);
                    this.mServices[i] = (SystemUI) (newService == null ? cl.newInstance() : newService);
                    this.mServices[i].mContext = this;
                    this.mServices[i].mComponents = this.mComponents;
                    Log.d("SystemUIService", "running: " + this.mServices[i]);
                    this.mServices[i].start();
                    if (this.mBootCompleted) {
                        this.mServices[i].onBootCompleted();
                    }
                    i++;
                } catch (IllegalAccessException ex) {
                    throw new RuntimeException(ex);
                } catch (InstantiationException ex2) {
                    throw new RuntimeException(ex2);
                }
            }
            ((PluginManager) Dependency.get(PluginManager.class)).addPluginListener(new PluginListener<OverlayPlugin>() {
                /* access modifiers changed from: private */
                public ArraySet<OverlayPlugin> mOverlays;

                public void onPluginConnected(OverlayPlugin plugin, Context pluginContext) {
                    StatusBar statusBar = (StatusBar) SystemUIApplication.this.getComponent(StatusBar.class);
                    if (statusBar != null) {
                        plugin.setup(statusBar.getStatusBarWindow(), statusBar.getNavigationBarView());
                    }
                    if (this.mOverlays == null) {
                        this.mOverlays = new ArraySet<>();
                    }
                    if (plugin.holdStatusBarOpen()) {
                        this.mOverlays.add(plugin);
                        ((StatusBarWindowManager) Dependency.get(StatusBarWindowManager.class)).setStateListener(new StatusBarWindowManager.OtherwisedCollapsedListener() {
                            public void setWouldOtherwiseCollapse(boolean otherwiseCollapse) {
                                Iterator it = AnonymousClass3.this.mOverlays.iterator();
                                while (it.hasNext()) {
                                    ((OverlayPlugin) it.next()).setCollapseDesired(otherwiseCollapse);
                                }
                            }
                        });
                        ((StatusBarWindowManager) Dependency.get(StatusBarWindowManager.class)).setForcePluginOpen(this.mOverlays.size() != 0);
                    }
                }

                public void onPluginDisconnected(OverlayPlugin plugin) {
                    this.mOverlays.remove(plugin);
                    ((StatusBarWindowManager) Dependency.get(StatusBarWindowManager.class)).setForcePluginOpen(this.mOverlays.size() != 0);
                }
            }, (Class<?>) OverlayPlugin.class, true);
            this.mServicesStarted = true;
        }
    }

    public void onConfigurationChanged(Configuration newConfig) {
        if (this.mServicesStarted) {
            int len = this.mServices.length;
            for (int i = 0; i < len; i++) {
                if (this.mServices[i] != null) {
                    this.mServices[i].onConfigurationChanged(newConfig);
                }
            }
        }
    }

    public void onPackageChanged(int uid, String packageName) {
        if (this.mServicesStarted) {
            for (SystemUI service : this.mServices) {
                if (service != null) {
                    service.onPackageChanged(uid, packageName);
                }
            }
        }
    }

    public void onPackageAdded(int uid, String packageName, boolean replacing) {
        if (this.mServicesStarted) {
            for (SystemUI service : this.mServices) {
                if (service != null) {
                    service.onPackageAdded(uid, packageName, replacing);
                }
            }
        }
    }

    public void onPackageRemoved(int uid, String packageName, boolean dataRemoved, boolean replacing) {
        if (this.mServicesStarted) {
            for (SystemUI service : this.mServices) {
                if (service != null) {
                    service.onPackageRemoved(uid, packageName, dataRemoved, replacing);
                }
            }
        }
    }

    public <T> T getComponent(Class<T> interfaceType) {
        return this.mComponents.get(interfaceType);
    }

    public SystemUI[] getServices() {
        return this.mServices;
    }
}
