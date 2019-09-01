package com.android.systemui.plugins;

import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.plugins.Plugin;
import com.android.systemui.plugins.VersionInfo;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PluginInstanceManager<T extends Plugin> {
    public static final String PLUGIN_PERMISSION = "com.android.systemui.permission.PLUGIN";
    /* access modifiers changed from: private */
    public final boolean isDebuggable;
    /* access modifiers changed from: private */
    public final String mAction;
    /* access modifiers changed from: private */
    public final boolean mAllowMultiple;
    /* access modifiers changed from: private */
    public final Context mContext;
    /* access modifiers changed from: private */
    public final PluginListener<T> mListener;
    @VisibleForTesting
    final PluginInstanceManager<T>.MainHandler mMainHandler;
    /* access modifiers changed from: private */
    public final PluginManagerImpl mManager;
    @VisibleForTesting
    final PluginInstanceManager<T>.PluginHandler mPluginHandler;
    /* access modifiers changed from: private */
    public final PackageManager mPm;
    /* access modifiers changed from: private */
    public final VersionInfo mVersion;

    private class MainHandler extends Handler {
        public MainHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    PluginPrefs.setHasPlugins(PluginInstanceManager.this.mContext);
                    PluginInfo<T> info = (PluginInfo) msg.obj;
                    if (!(msg.obj instanceof PluginFragment)) {
                        ((Plugin) info.mPlugin).onCreate(PluginInstanceManager.this.mContext, info.mPluginContext);
                    }
                    PluginInstanceManager.this.mListener.onPluginConnected((Plugin) info.mPlugin, info.mPluginContext);
                    return;
                case 2:
                    PluginInstanceManager.this.mListener.onPluginDisconnected((Plugin) msg.obj);
                    if (!(msg.obj instanceof PluginFragment)) {
                        ((Plugin) msg.obj).onDestroy();
                        return;
                    }
                    return;
                default:
                    super.handleMessage(msg);
                    return;
            }
        }
    }

    public static class PluginContextWrapper extends ContextWrapper {
        private final ClassLoader mClassLoader;
        private LayoutInflater mInflater;

        public PluginContextWrapper(Context base, ClassLoader classLoader) {
            super(base);
            this.mClassLoader = classLoader;
        }

        public ClassLoader getClassLoader() {
            return this.mClassLoader;
        }

        public Object getSystemService(String name) {
            if (!"layout_inflater".equals(name)) {
                return getBaseContext().getSystemService(name);
            }
            if (this.mInflater == null) {
                this.mInflater = LayoutInflater.from(getBaseContext()).cloneInContext(this);
            }
            return this.mInflater;
        }
    }

    private class PluginHandler extends Handler {
        /* access modifiers changed from: private */
        public final ArrayList<PluginInfo<T>> mPlugins = new ArrayList<>();

        public PluginHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    for (int i = this.mPlugins.size() - 1; i >= 0; i--) {
                        PluginInfo<T> plugin = this.mPlugins.get(i);
                        PluginInstanceManager.this.mListener.onPluginDisconnected((Plugin) plugin.mPlugin);
                        if (!(plugin.mPlugin instanceof PluginFragment)) {
                            ((Plugin) plugin.mPlugin).onDestroy();
                        }
                    }
                    this.mPlugins.clear();
                    handleQueryPlugins(null);
                    return;
                case 2:
                    String p = (String) msg.obj;
                    if (PluginInstanceManager.this.mAllowMultiple || this.mPlugins.size() == 0) {
                        handleQueryPlugins(p);
                        return;
                    }
                    return;
                case 3:
                    String pkg = (String) msg.obj;
                    for (int i2 = this.mPlugins.size() - 1; i2 >= 0; i2--) {
                        PluginInfo<T> plugin2 = this.mPlugins.get(i2);
                        if (plugin2.mPackage.equals(pkg)) {
                            PluginInstanceManager.this.mMainHandler.obtainMessage(2, plugin2.mPlugin).sendToTarget();
                            this.mPlugins.remove(i2);
                        }
                    }
                    return;
                default:
                    super.handleMessage(msg);
                    return;
            }
        }

        /* access modifiers changed from: private */
        public void handleQueryPlugins(String pkgName) {
            Intent intent = new Intent(PluginInstanceManager.this.mAction);
            if (pkgName != null) {
                intent.setPackage(pkgName);
            }
            List<ResolveInfo> result = PluginInstanceManager.this.mPm.queryIntentServices(intent, 0);
            if (result != null) {
                if (result.size() <= 1 || PluginInstanceManager.this.mAllowMultiple) {
                    for (ResolveInfo info : result) {
                        PluginInfo<T> t = handleLoadPlugin(new ComponentName(info.serviceInfo.packageName, info.serviceInfo.name));
                        if (t != null) {
                            PluginInstanceManager.this.mMainHandler.obtainMessage(1, t).sendToTarget();
                            this.mPlugins.add(t);
                        }
                    }
                    return;
                }
                Log.w("PluginInstanceManager", "Multiple plugins found for " + PluginInstanceManager.this.mAction);
            }
        }

        /* access modifiers changed from: protected */
        /* JADX WARNING: Removed duplicated region for block: B:46:0x0129 A[Catch:{ Throwable -> 0x011b }] */
        /* JADX WARNING: Removed duplicated region for block: B:47:0x015c A[Catch:{ Throwable -> 0x011b }] */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public com.android.systemui.plugins.PluginInstanceManager.PluginInfo<T> handleLoadPlugin(android.content.ComponentName r21) {
            /*
                r20 = this;
                r1 = r20
                com.android.systemui.plugins.PluginInstanceManager r0 = com.android.systemui.plugins.PluginInstanceManager.this
                boolean r0 = r0.isDebuggable
                r2 = 0
                if (r0 != 0) goto L_0x0013
                java.lang.String r0 = "PluginInstanceManager"
                java.lang.String r3 = "Somehow hit second debuggable check"
                android.util.Log.d(r0, r3)
                return r2
            L_0x0013:
                java.lang.String r3 = r21.getPackageName()
                java.lang.String r0 = r21.getClassName()
                r10 = r0
                com.android.systemui.plugins.PluginInstanceManager r0 = com.android.systemui.plugins.PluginInstanceManager.this     // Catch:{ Throwable -> 0x0217 }
                android.content.pm.PackageManager r0 = r0.mPm     // Catch:{ Throwable -> 0x0217 }
                r11 = 0
                android.content.pm.ApplicationInfo r0 = r0.getApplicationInfo(r3, r11)     // Catch:{ Throwable -> 0x0217 }
                r12 = r0
                com.android.systemui.plugins.PluginInstanceManager r0 = com.android.systemui.plugins.PluginInstanceManager.this     // Catch:{ Throwable -> 0x0217 }
                android.content.pm.PackageManager r0 = r0.mPm     // Catch:{ Throwable -> 0x0217 }
                java.lang.String r4 = "com.android.systemui.permission.PLUGIN"
                int r0 = r0.checkPermission(r4, r3)     // Catch:{ Throwable -> 0x0217 }
                if (r0 == 0) goto L_0x0054
                java.lang.String r0 = "PluginInstanceManager"
                java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch:{ Throwable -> 0x004d }
                r4.<init>()     // Catch:{ Throwable -> 0x004d }
                java.lang.String r5 = "Plugin doesn't have permission: "
                r4.append(r5)     // Catch:{ Throwable -> 0x004d }
                r4.append(r3)     // Catch:{ Throwable -> 0x004d }
                java.lang.String r4 = r4.toString()     // Catch:{ Throwable -> 0x004d }
                android.util.Log.d(r0, r4)     // Catch:{ Throwable -> 0x004d }
                return r2
            L_0x004d:
                r0 = move-exception
                r7 = r21
                r17 = r3
                goto L_0x021c
            L_0x0054:
                com.android.systemui.plugins.PluginInstanceManager r0 = com.android.systemui.plugins.PluginInstanceManager.this     // Catch:{ Throwable -> 0x0217 }
                com.android.systemui.plugins.PluginManagerImpl r0 = r0.mManager     // Catch:{ Throwable -> 0x0217 }
                java.lang.String r4 = r12.sourceDir     // Catch:{ Throwable -> 0x0217 }
                java.lang.String r5 = r12.packageName     // Catch:{ Throwable -> 0x0217 }
                java.lang.ClassLoader r0 = r0.getClassLoader(r4, r5)     // Catch:{ Throwable -> 0x0217 }
                r13 = r0
                com.android.systemui.plugins.PluginInstanceManager$PluginContextWrapper r8 = new com.android.systemui.plugins.PluginInstanceManager$PluginContextWrapper     // Catch:{ Throwable -> 0x0217 }
                com.android.systemui.plugins.PluginInstanceManager r0 = com.android.systemui.plugins.PluginInstanceManager.this     // Catch:{ Throwable -> 0x0217 }
                android.content.Context r0 = r0.mContext     // Catch:{ Throwable -> 0x0217 }
                android.content.Context r0 = r0.createApplicationContext(r12, r11)     // Catch:{ Throwable -> 0x0217 }
                r8.<init>(r0, r13)     // Catch:{ Throwable -> 0x0217 }
                r14 = 1
                java.lang.Class r0 = java.lang.Class.forName(r10, r14, r13)     // Catch:{ Throwable -> 0x0217 }
                r15 = r0
                java.lang.Object r0 = r15.newInstance()     // Catch:{ Throwable -> 0x0217 }
                com.android.systemui.plugins.Plugin r0 = (com.android.systemui.plugins.Plugin) r0     // Catch:{ Throwable -> 0x0217 }
                r7 = r0
                com.android.systemui.plugins.PluginInstanceManager r0 = com.android.systemui.plugins.PluginInstanceManager.this     // Catch:{ InvalidVersionException -> 0x0096 }
                com.android.systemui.plugins.VersionInfo r0 = r0.mVersion     // Catch:{ InvalidVersionException -> 0x0096 }
                com.android.systemui.plugins.VersionInfo r9 = r1.checkVersion(r15, r7, r0)     // Catch:{ InvalidVersionException -> 0x0096 }
                com.android.systemui.plugins.PluginInstanceManager$PluginInfo r0 = new com.android.systemui.plugins.PluginInstanceManager$PluginInfo     // Catch:{ InvalidVersionException -> 0x0096 }
                r4 = r0
                r5 = r3
                r6 = r10
                r16 = r7
                r4.<init>(r5, r6, r7, r8, r9)     // Catch:{ InvalidVersionException -> 0x0094 }
                return r0
            L_0x0094:
                r0 = move-exception
                goto L_0x0099
            L_0x0096:
                r0 = move-exception
                r16 = r7
            L_0x0099:
                r4 = r0
                com.android.systemui.plugins.PluginInstanceManager r0 = com.android.systemui.plugins.PluginInstanceManager.this     // Catch:{ Throwable -> 0x0217 }
                android.content.Context r0 = r0.mContext     // Catch:{ Throwable -> 0x0217 }
                android.content.res.Resources r0 = r0.getResources()     // Catch:{ Throwable -> 0x0217 }
                java.lang.String r5 = "tuner"
                java.lang.String r6 = "drawable"
                com.android.systemui.plugins.PluginInstanceManager r7 = com.android.systemui.plugins.PluginInstanceManager.this     // Catch:{ Throwable -> 0x0217 }
                android.content.Context r7 = r7.mContext     // Catch:{ Throwable -> 0x0217 }
                java.lang.String r7 = r7.getPackageName()     // Catch:{ Throwable -> 0x0217 }
                int r0 = r0.getIdentifier(r5, r6, r7)     // Catch:{ Throwable -> 0x0217 }
                r5 = r0
                android.content.res.Resources r0 = android.content.res.Resources.getSystem()     // Catch:{ Throwable -> 0x0217 }
                java.lang.String r6 = "system_notification_accent_color"
                java.lang.String r7 = "color"
                java.lang.String r9 = "android"
                int r0 = r0.getIdentifier(r6, r7, r9)     // Catch:{ Throwable -> 0x0217 }
                r6 = r0
                com.android.systemui.plugins.PluginInstanceManager r0 = com.android.systemui.plugins.PluginInstanceManager.this     // Catch:{ Throwable -> 0x0217 }
                android.content.Context r0 = r0.mContext     // Catch:{ Throwable -> 0x0217 }
                java.lang.String r7 = "ALR"
                android.app.Notification$Builder r0 = android.app.NotificationCompat.newBuilder(r0, r7)     // Catch:{ Throwable -> 0x0217 }
                android.app.Notification$BigTextStyle r7 = new android.app.Notification$BigTextStyle     // Catch:{ Throwable -> 0x0217 }
                r7.<init>()     // Catch:{ Throwable -> 0x0217 }
                android.app.Notification$Builder r0 = r0.setStyle(r7)     // Catch:{ Throwable -> 0x0217 }
                android.app.Notification$Builder r0 = r0.setSmallIcon(r5)     // Catch:{ Throwable -> 0x0217 }
                r17 = r3
                r2 = 0
                android.app.Notification$Builder r0 = r0.setWhen(r2)     // Catch:{ Throwable -> 0x0213 }
                android.app.Notification$Builder r0 = r0.setShowWhen(r11)     // Catch:{ Throwable -> 0x0213 }
                android.app.Notification$Builder r0 = r0.setVisibility(r14)     // Catch:{ Throwable -> 0x0213 }
                com.android.systemui.plugins.PluginInstanceManager r2 = com.android.systemui.plugins.PluginInstanceManager.this     // Catch:{ Throwable -> 0x0213 }
                android.content.Context r2 = r2.mContext     // Catch:{ Throwable -> 0x0213 }
                int r2 = r2.getColor(r6)     // Catch:{ Throwable -> 0x0213 }
                android.app.Notification$Builder r0 = r0.setColor(r2)     // Catch:{ Throwable -> 0x0213 }
                r2 = r0
                r3 = r10
                com.android.systemui.plugins.PluginInstanceManager r0 = com.android.systemui.plugins.PluginInstanceManager.this     // Catch:{ NameNotFoundException -> 0x0120 }
                android.content.pm.PackageManager r0 = r0.mPm     // Catch:{ NameNotFoundException -> 0x0120 }
                r7 = r21
                android.content.pm.ServiceInfo r0 = r0.getServiceInfo(r7, r11)     // Catch:{ NameNotFoundException -> 0x011e }
                com.android.systemui.plugins.PluginInstanceManager r9 = com.android.systemui.plugins.PluginInstanceManager.this     // Catch:{ NameNotFoundException -> 0x011e }
                android.content.pm.PackageManager r9 = r9.mPm     // Catch:{ NameNotFoundException -> 0x011e }
                java.lang.CharSequence r0 = r0.loadLabel(r9)     // Catch:{ NameNotFoundException -> 0x011e }
                java.lang.String r0 = r0.toString()     // Catch:{ NameNotFoundException -> 0x011e }
                r3 = r0
                goto L_0x0123
            L_0x011b:
                r0 = move-exception
                goto L_0x021c
            L_0x011e:
                r0 = move-exception
                goto L_0x0123
            L_0x0120:
                r0 = move-exception
                r7 = r21
            L_0x0123:
                boolean r0 = r4.isTooNew()     // Catch:{ Throwable -> 0x011b }
                if (r0 != 0) goto L_0x015c
                java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch:{ Throwable -> 0x011b }
                r0.<init>()     // Catch:{ Throwable -> 0x011b }
                java.lang.String r9 = "Plugin \""
                r0.append(r9)     // Catch:{ Throwable -> 0x011b }
                r0.append(r3)     // Catch:{ Throwable -> 0x011b }
                java.lang.String r9 = "\" is too old"
                r0.append(r9)     // Catch:{ Throwable -> 0x011b }
                java.lang.String r0 = r0.toString()     // Catch:{ Throwable -> 0x011b }
                android.app.Notification$Builder r0 = r2.setContentTitle(r0)     // Catch:{ Throwable -> 0x011b }
                java.lang.StringBuilder r9 = new java.lang.StringBuilder     // Catch:{ Throwable -> 0x011b }
                r9.<init>()     // Catch:{ Throwable -> 0x011b }
                java.lang.String r14 = "Contact plugin developer to get an updated version.\n"
                r9.append(r14)     // Catch:{ Throwable -> 0x011b }
                java.lang.String r14 = r4.getMessage()     // Catch:{ Throwable -> 0x011b }
                r9.append(r14)     // Catch:{ Throwable -> 0x011b }
                java.lang.String r9 = r9.toString()     // Catch:{ Throwable -> 0x011b }
                r0.setContentText(r9)     // Catch:{ Throwable -> 0x011b }
                goto L_0x018e
            L_0x015c:
                java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch:{ Throwable -> 0x011b }
                r0.<init>()     // Catch:{ Throwable -> 0x011b }
                java.lang.String r9 = "Plugin \""
                r0.append(r9)     // Catch:{ Throwable -> 0x011b }
                r0.append(r3)     // Catch:{ Throwable -> 0x011b }
                java.lang.String r9 = "\" is too new"
                r0.append(r9)     // Catch:{ Throwable -> 0x011b }
                java.lang.String r0 = r0.toString()     // Catch:{ Throwable -> 0x011b }
                android.app.Notification$Builder r0 = r2.setContentTitle(r0)     // Catch:{ Throwable -> 0x011b }
                java.lang.StringBuilder r9 = new java.lang.StringBuilder     // Catch:{ Throwable -> 0x011b }
                r9.<init>()     // Catch:{ Throwable -> 0x011b }
                java.lang.String r14 = "Check to see if an OTA is available.\n"
                r9.append(r14)     // Catch:{ Throwable -> 0x011b }
                java.lang.String r14 = r4.getMessage()     // Catch:{ Throwable -> 0x011b }
                r9.append(r14)     // Catch:{ Throwable -> 0x011b }
                java.lang.String r9 = r9.toString()     // Catch:{ Throwable -> 0x011b }
                r0.setContentText(r9)     // Catch:{ Throwable -> 0x011b }
            L_0x018e:
                android.content.Intent r0 = new android.content.Intent     // Catch:{ Throwable -> 0x011b }
                java.lang.String r9 = "com.android.systemui.action.DISABLE_PLUGIN"
                r0.<init>(r9)     // Catch:{ Throwable -> 0x011b }
                java.lang.StringBuilder r9 = new java.lang.StringBuilder     // Catch:{ Throwable -> 0x011b }
                r9.<init>()     // Catch:{ Throwable -> 0x011b }
                java.lang.String r14 = "package://"
                r9.append(r14)     // Catch:{ Throwable -> 0x011b }
                java.lang.String r14 = r21.flattenToString()     // Catch:{ Throwable -> 0x011b }
                r9.append(r14)     // Catch:{ Throwable -> 0x011b }
                java.lang.String r9 = r9.toString()     // Catch:{ Throwable -> 0x011b }
                android.net.Uri r9 = android.net.Uri.parse(r9)     // Catch:{ Throwable -> 0x011b }
                android.content.Intent r0 = r0.setData(r9)     // Catch:{ Throwable -> 0x011b }
                com.android.systemui.plugins.PluginInstanceManager r9 = com.android.systemui.plugins.PluginInstanceManager.this     // Catch:{ Throwable -> 0x011b }
                android.content.Context r9 = r9.mContext     // Catch:{ Throwable -> 0x011b }
                android.app.PendingIntent r9 = android.app.PendingIntent.getBroadcast(r9, r11, r0, r11)     // Catch:{ Throwable -> 0x011b }
                android.app.Notification$Action$Builder r11 = new android.app.Notification$Action$Builder     // Catch:{ Throwable -> 0x011b }
                java.lang.String r14 = "Disable plugin"
                r18 = r3
                r3 = 0
                r11.<init>(r3, r14, r9)     // Catch:{ Throwable -> 0x011b }
                android.app.Notification$Action r3 = r11.build()     // Catch:{ Throwable -> 0x011b }
                r2.addAction(r3)     // Catch:{ Throwable -> 0x011b }
                com.android.systemui.plugins.PluginInstanceManager r3 = com.android.systemui.plugins.PluginInstanceManager.this     // Catch:{ Throwable -> 0x011b }
                android.content.Context r3 = r3.mContext     // Catch:{ Throwable -> 0x011b }
                java.lang.Class<android.app.NotificationManager> r11 = android.app.NotificationManager.class
                java.lang.Object r3 = r3.getSystemService(r11)     // Catch:{ Throwable -> 0x011b }
                android.app.NotificationManager r3 = (android.app.NotificationManager) r3     // Catch:{ Throwable -> 0x011b }
                android.app.Notification r14 = r2.build()     // Catch:{ Throwable -> 0x011b }
                android.os.UserHandle r11 = android.os.UserHandle.ALL     // Catch:{ Throwable -> 0x011b }
                r19 = r0
                r0 = 6
                r3.notifyAsUser(r10, r0, r14, r11)     // Catch:{ Throwable -> 0x011b }
                java.lang.String r0 = "PluginInstanceManager"
                java.lang.StringBuilder r3 = new java.lang.StringBuilder     // Catch:{ Throwable -> 0x011b }
                r3.<init>()     // Catch:{ Throwable -> 0x011b }
                java.lang.String r11 = "Plugin has invalid interface version "
                r3.append(r11)     // Catch:{ Throwable -> 0x011b }
                r11 = r16
                int r14 = r11.getVersion()     // Catch:{ Throwable -> 0x011b }
                r3.append(r14)     // Catch:{ Throwable -> 0x011b }
                java.lang.String r14 = ", expected "
                r3.append(r14)     // Catch:{ Throwable -> 0x011b }
                com.android.systemui.plugins.PluginInstanceManager r14 = com.android.systemui.plugins.PluginInstanceManager.this     // Catch:{ Throwable -> 0x011b }
                com.android.systemui.plugins.VersionInfo r14 = r14.mVersion     // Catch:{ Throwable -> 0x011b }
                r3.append(r14)     // Catch:{ Throwable -> 0x011b }
                java.lang.String r3 = r3.toString()     // Catch:{ Throwable -> 0x011b }
                android.util.Log.w(r0, r3)     // Catch:{ Throwable -> 0x011b }
                r3 = 0
                return r3
            L_0x0213:
                r0 = move-exception
                r7 = r21
                goto L_0x021c
            L_0x0217:
                r0 = move-exception
                r7 = r21
                r17 = r3
            L_0x021c:
                java.lang.String r2 = "PluginInstanceManager"
                java.lang.StringBuilder r3 = new java.lang.StringBuilder
                r3.<init>()
                java.lang.String r4 = "Couldn't load plugin: "
                r3.append(r4)
                r4 = r17
                r3.append(r4)
                java.lang.String r3 = r3.toString()
                android.util.Log.w(r2, r3, r0)
                r2 = 0
                return r2
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.plugins.PluginInstanceManager.PluginHandler.handleLoadPlugin(android.content.ComponentName):com.android.systemui.plugins.PluginInstanceManager$PluginInfo");
        }

        private VersionInfo checkVersion(Class<?> pluginClass, T plugin, VersionInfo version) throws VersionInfo.InvalidVersionException {
            VersionInfo pv = new VersionInfo().addClass(pluginClass);
            if (pv.hasVersionInfo()) {
                version.checkVersion(pv);
                return pv;
            } else if (plugin.getVersion() == version.getDefaultVersion()) {
                return null;
            } else {
                throw new VersionInfo.InvalidVersionException("Invalid legacy version", false);
            }
        }
    }

    static class PluginInfo<T> {
        /* access modifiers changed from: private */
        public String mClass;
        String mPackage;
        T mPlugin;
        /* access modifiers changed from: private */
        public final Context mPluginContext;
        /* access modifiers changed from: private */
        public final VersionInfo mVersion;

        public PluginInfo(String pkg, String cls, T plugin, Context pluginContext, VersionInfo info) {
            this.mPlugin = plugin;
            this.mClass = cls;
            this.mPackage = pkg;
            this.mPluginContext = pluginContext;
            this.mVersion = info;
        }
    }

    PluginInstanceManager(Context context, String action, PluginListener<T> listener, boolean allowMultiple, Looper looper, VersionInfo version, PluginManagerImpl manager) {
        this(context, context.getPackageManager(), action, listener, allowMultiple, looper, version, manager, Build.IS_DEBUGGABLE);
    }

    @VisibleForTesting
    PluginInstanceManager(Context context, PackageManager pm, String action, PluginListener<T> listener, boolean allowMultiple, Looper looper, VersionInfo version, PluginManagerImpl manager, boolean debuggable) {
        this.mMainHandler = new MainHandler(Looper.getMainLooper());
        this.mPluginHandler = new PluginHandler(looper);
        this.mManager = manager;
        this.mContext = context;
        this.mPm = pm;
        this.mAction = action;
        this.mListener = listener;
        this.mAllowMultiple = allowMultiple;
        this.mVersion = version;
        this.isDebuggable = debuggable;
    }

    public PluginInfo<T> getPlugin() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            this.mPluginHandler.handleQueryPlugins(null);
            if (this.mPluginHandler.mPlugins.size() <= 0) {
                return null;
            }
            this.mMainHandler.removeMessages(1);
            PluginInfo<T> info = (PluginInfo) this.mPluginHandler.mPlugins.get(0);
            PluginPrefs.setHasPlugins(this.mContext);
            ((Plugin) info.mPlugin).onCreate(this.mContext, info.mPluginContext);
            return info;
        }
        throw new RuntimeException("Must be called from UI thread");
    }

    public void loadAll() {
        this.mPluginHandler.sendEmptyMessage(1);
    }

    public void destroy() {
        Iterator<PluginInfo<T>> it = new ArrayList<>(this.mPluginHandler.mPlugins).iterator();
        while (it.hasNext()) {
            this.mMainHandler.obtainMessage(2, it.next().mPlugin).sendToTarget();
        }
    }

    public void onPackageRemoved(String pkg) {
        this.mPluginHandler.obtainMessage(3, pkg).sendToTarget();
    }

    public void onPackageChange(String pkg) {
        this.mPluginHandler.obtainMessage(3, pkg).sendToTarget();
        this.mPluginHandler.obtainMessage(2, pkg).sendToTarget();
    }

    public boolean checkAndDisable(String className) {
        boolean disableAny = false;
        Iterator<PluginInfo<T>> it = new ArrayList<>(this.mPluginHandler.mPlugins).iterator();
        while (it.hasNext()) {
            PluginInfo info = it.next();
            if (className.startsWith(info.mPackage)) {
                disable(info);
                disableAny = true;
            }
        }
        return disableAny;
    }

    public void disableAll() {
        ArrayList<PluginInfo<T>> plugins = new ArrayList<>(this.mPluginHandler.mPlugins);
        for (int i = 0; i < plugins.size(); i++) {
            disable(plugins.get(i));
        }
    }

    private void disable(PluginInfo info) {
        Log.w("PluginInstanceManager", "Disabling plugin " + info.mPackage + "/" + info.mClass);
        this.mPm.setComponentEnabledSetting(new ComponentName(info.mPackage, info.mClass), 2, 1);
    }

    public boolean dependsOn(Plugin p, Class<T> cls) {
        boolean z;
        PluginInfo info;
        Iterator<PluginInfo<T>> it = new ArrayList<>(this.mPluginHandler.mPlugins).iterator();
        do {
            z = false;
            if (!it.hasNext()) {
                return false;
            }
            info = it.next();
        } while (!info.mPlugin.getClass().getName().equals(p.getClass().getName()));
        if (info.mVersion != null && info.mVersion.hasClass(cls)) {
            z = true;
        }
        return z;
    }
}
