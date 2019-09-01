package com.android.systemui.plugins;

import android.app.Notification;
import android.app.NotificationCompat;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.widget.Toast;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.Dependency;
import com.android.systemui.plugins.PluginInstanceManager;
import com.android.systemui.plugins.annotations.ProvidesInterface;
import dalvik.system.PathClassLoader;
import java.lang.Thread;
import java.util.Map;

public class PluginManagerImpl extends BroadcastReceiver implements PluginManager {
    public static final String TAG = "PluginManagerImpl";
    private final boolean isDebuggable;
    private final Map<String, ClassLoader> mClassLoaders;
    private final Context mContext;
    private final PluginInstanceManagerFactory mFactory;
    private boolean mHasOneShot;
    private boolean mListening;
    private Looper mLooper;
    private final ArraySet<String> mOneShotPackages;
    private ClassLoaderFilter mParentClassLoader;
    /* access modifiers changed from: private */
    public final ArrayMap<PluginListener<?>, PluginInstanceManager> mPluginMap;
    private final PluginPrefs mPluginPrefs;

    private static class ClassLoaderFilter extends ClassLoader {
        private final ClassLoader mBase;
        private final String mPackage;

        public ClassLoaderFilter(ClassLoader base, String pkg) {
            super(ClassLoader.getSystemClassLoader());
            this.mBase = base;
            this.mPackage = pkg;
        }

        /* access modifiers changed from: protected */
        public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (!name.startsWith(this.mPackage)) {
                super.loadClass(name, resolve);
            }
            return this.mBase.loadClass(name);
        }
    }

    private class PluginExceptionHandler implements Thread.UncaughtExceptionHandler {
        private final Thread.UncaughtExceptionHandler mHandler;

        private PluginExceptionHandler(Thread.UncaughtExceptionHandler handler) {
            this.mHandler = handler;
        }

        public void uncaughtException(Thread thread, Throwable throwable) {
            if (SystemProperties.getBoolean("plugin.debugging", false)) {
                this.mHandler.uncaughtException(thread, throwable);
                return;
            }
            if (!checkStack(throwable)) {
                for (PluginInstanceManager manager : PluginManagerImpl.this.mPluginMap.values()) {
                    manager.disableAll();
                }
            }
            this.mHandler.uncaughtException(thread, throwable);
        }

        private boolean checkStack(Throwable throwable) {
            if (throwable == null) {
                return false;
            }
            boolean disabledAny = false;
            for (StackTraceElement element : throwable.getStackTrace()) {
                for (PluginInstanceManager manager : PluginManagerImpl.this.mPluginMap.values()) {
                    disabledAny |= manager.checkAndDisable(element.getClassName());
                }
            }
            return checkStack(throwable.getCause()) | disabledAny;
        }
    }

    @VisibleForTesting
    public static class PluginInstanceManagerFactory {
        public <T extends Plugin> PluginInstanceManager createPluginInstanceManager(Context context, String action, PluginListener<T> listener, boolean allowMultiple, Looper looper, Class<?> cls, PluginManagerImpl manager) {
            PluginInstanceManager pluginInstanceManager = new PluginInstanceManager(context, action, listener, allowMultiple, looper, new VersionInfo().addClass(cls), manager);
            return pluginInstanceManager;
        }
    }

    public PluginManagerImpl(Context context) {
        this(context, new PluginInstanceManagerFactory(), Build.IS_DEBUGGABLE, Thread.getDefaultUncaughtExceptionHandler());
    }

    @VisibleForTesting
    PluginManagerImpl(Context context, PluginInstanceManagerFactory factory, boolean debuggable, Thread.UncaughtExceptionHandler defaultHandler) {
        this.mPluginMap = new ArrayMap<>();
        this.mClassLoaders = new ArrayMap();
        this.mOneShotPackages = new ArraySet<>();
        this.mContext = context;
        this.mFactory = factory;
        this.mLooper = (Looper) Dependency.get(Dependency.BG_LOOPER);
        this.isDebuggable = debuggable;
        this.mPluginPrefs = new PluginPrefs(this.mContext);
        Thread.setDefaultUncaughtExceptionHandler(new PluginExceptionHandler(defaultHandler));
        if (this.isDebuggable) {
            new Handler(this.mLooper).post(new Runnable() {
                public void run() {
                    ((PluginDependencyProvider) Dependency.get(PluginDependencyProvider.class)).allowPluginDependency(ActivityStarter.class);
                }
            });
        }
    }

    public <T extends Plugin> T getOneShotPlugin(Class<T> cls) {
        ProvidesInterface info = (ProvidesInterface) cls.getDeclaredAnnotation(ProvidesInterface.class);
        if (info == null) {
            Log.d(TAG, cls + " doesn't provide an interface");
            return null;
        } else if (!TextUtils.isEmpty(info.action())) {
            return getOneShotPlugin(info.action(), cls);
        } else {
            Log.d(TAG, cls + " doesn't provide an action");
            return null;
        }
    }

    public <T extends Plugin> T getOneShotPlugin(String action, Class<?> cls) {
        if (!this.isDebuggable) {
            return null;
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            PluginInstanceManager p = this.mFactory.createPluginInstanceManager(this.mContext, action, null, false, this.mLooper, cls, this);
            this.mPluginPrefs.addAction(action);
            PluginInstanceManager.PluginInfo<T> info = p.getPlugin();
            if (info == null) {
                return null;
            }
            this.mOneShotPackages.add(info.mPackage);
            this.mHasOneShot = true;
            startListening();
            return (Plugin) info.mPlugin;
        }
        throw new RuntimeException("Must be called from UI thread");
    }

    public <T extends Plugin> void addPluginListener(PluginListener<T> listener, Class<?> cls) {
        addPluginListener(listener, cls, false);
    }

    public <T extends Plugin> void addPluginListener(PluginListener<T> listener, Class<?> cls, boolean allowMultiple) {
        if (this.isDebuggable) {
            addPluginListener(PluginManagerHelper.getAction(cls), listener, cls, allowMultiple);
        }
    }

    public <T extends Plugin> void addPluginListener(String action, PluginListener<T> listener, Class<?> cls) {
        addPluginListener(action, listener, cls, false);
    }

    public <T extends Plugin> void addPluginListener(String action, PluginListener<T> listener, Class cls, boolean allowMultiple) {
        if (this.isDebuggable) {
            this.mPluginPrefs.addAction(action);
            PluginInstanceManager p = this.mFactory.createPluginInstanceManager(this.mContext, action, listener, allowMultiple, this.mLooper, cls, this);
            p.loadAll();
            this.mPluginMap.put(listener, p);
            startListening();
        }
    }

    public void removePluginListener(PluginListener<?> listener) {
        if (this.isDebuggable && this.mPluginMap.containsKey(listener)) {
            this.mPluginMap.remove(listener).destroy();
            if (this.mPluginMap.size() == 0) {
                stopListening();
            }
        }
    }

    private void startListening() {
        if (!this.mListening) {
            this.mListening = true;
            IntentFilter filter = new IntentFilter("android.intent.action.PACKAGE_ADDED");
            filter.addAction("android.intent.action.PACKAGE_CHANGED");
            filter.addAction("android.intent.action.PACKAGE_REMOVED");
            filter.addAction(PluginManager.PLUGIN_CHANGED);
            filter.addAction("com.android.systemui.action.DISABLE_PLUGIN");
            filter.addDataScheme("package");
            this.mContext.registerReceiver(this, filter);
            this.mContext.registerReceiver(this, new IntentFilter("android.intent.action.USER_UNLOCKED"));
        }
    }

    private void stopListening() {
        if (this.mListening && !this.mHasOneShot) {
            this.mListening = false;
            this.mContext.unregisterReceiver(this);
        }
    }

    public void onReceive(Context context, Intent intent) {
        if ("android.intent.action.USER_UNLOCKED".equals(intent.getAction())) {
            for (PluginInstanceManager manager : this.mPluginMap.values()) {
                manager.loadAll();
            }
        } else if ("com.android.systemui.action.DISABLE_PLUGIN".equals(intent.getAction())) {
            ComponentName component = ComponentName.unflattenFromString(intent.getData().toString().substring(10));
            this.mContext.getPackageManager().setComponentEnabledSetting(component, 2, 1);
            ((NotificationManager) this.mContext.getSystemService(NotificationManager.class)).cancel(component.getClassName(), 6);
        } else {
            String pkg = intent.getData().getEncodedSchemeSpecificPart();
            if (this.mOneShotPackages.contains(pkg)) {
                int icon = this.mContext.getResources().getIdentifier("tuner", "drawable", this.mContext.getPackageName());
                int color = Resources.getSystem().getIdentifier("system_notification_accent_color", "color", "android");
                String label = pkg;
                try {
                    PackageManager pm = this.mContext.getPackageManager();
                    label = pm.getApplicationInfo(pkg, 0).loadLabel(pm).toString();
                } catch (PackageManager.NameNotFoundException e) {
                }
                Notification.Builder color2 = NotificationCompat.newBuilder(this.mContext, PluginManager.NOTIFICATION_CHANNEL_ID).setSmallIcon(icon).setWhen(0).setShowWhen(false).setPriority(2).setVisibility(1).setColor(this.mContext.getColor(color));
                Notification.Builder nb = color2.setContentTitle("Plugin \"" + label + "\" has updated").setContentText("Restart SysUI for changes to take effect.");
                Intent intent2 = new Intent("com.android.systemui.action.RESTART");
                nb.addAction(new Notification.Action.Builder(null, "Restart SysUI", PendingIntent.getBroadcast(this.mContext, 0, intent2.setData(Uri.parse("package://" + pkg)), 0)).build());
                ((NotificationManager) this.mContext.getSystemService(NotificationManager.class)).notifyAsUser(pkg, 6, nb.build(), UserHandle.ALL);
            }
            if (clearClassLoader(pkg)) {
                Context context2 = this.mContext;
                Toast.makeText(context2, "Reloading " + pkg, 1).show();
            }
            if (!"android.intent.action.PACKAGE_REMOVED".equals(intent.getAction())) {
                for (PluginInstanceManager manager2 : this.mPluginMap.values()) {
                    manager2.onPackageChange(pkg);
                }
                return;
            }
            for (PluginInstanceManager manager3 : this.mPluginMap.values()) {
                manager3.onPackageRemoved(pkg);
            }
        }
    }

    public ClassLoader getClassLoader(String sourceDir, String pkg) {
        if (this.mClassLoaders.containsKey(pkg)) {
            return this.mClassLoaders.get(pkg);
        }
        ClassLoader classLoader = new PathClassLoader(sourceDir, getParentClassLoader());
        this.mClassLoaders.put(pkg, classLoader);
        return classLoader;
    }

    private boolean clearClassLoader(String pkg) {
        return this.mClassLoaders.remove(pkg) != null;
    }

    /* access modifiers changed from: package-private */
    public ClassLoader getParentClassLoader() {
        if (this.mParentClassLoader == null) {
            this.mParentClassLoader = new ClassLoaderFilter(getClass().getClassLoader(), "com.android.systemui.plugin");
        }
        return this.mParentClassLoader;
    }

    public Context getContext(ApplicationInfo info, String pkg) throws PackageManager.NameNotFoundException {
        return new PluginInstanceManager.PluginContextWrapper(this.mContext.createApplicationContext(info, 0), getClassLoader(info.sourceDir, pkg));
    }

    public <T> boolean dependsOn(Plugin p, Class<T> cls) {
        for (int i = 0; i < this.mPluginMap.size(); i++) {
            if (this.mPluginMap.valueAt(i).dependsOn(p, cls)) {
                return true;
            }
        }
        return false;
    }
}
