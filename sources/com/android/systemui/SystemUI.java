package com.android.systemui;

import android.app.Notification;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import com.android.systemui.miui.PackageEventReceiver;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Map;

public abstract class SystemUI implements PackageEventReceiver {
    public Map<Class<?>, Object> mComponents;
    public Context mContext;

    public abstract void start();

    public SystemUI() {
        Dependency.inject(this);
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration newConfig) {
    }

    public void onPackageChanged(int uid, String packageName) {
    }

    public void onPackageAdded(int uid, String packageName, boolean replacing) {
    }

    public void onPackageRemoved(int uid, String packageName, boolean dataRemoved, boolean replacing) {
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
    }

    /* access modifiers changed from: protected */
    public void onBootCompleted() {
    }

    public <T> T getComponent(Class<T> interfaceType) {
        if (this.mComponents != null) {
            return this.mComponents.get(interfaceType);
        }
        return null;
    }

    public static <T> T getComponent(Context context, Class<T> interfaceType) {
        return ((Application) context.getApplicationContext()).getSystemUIApplication().getComponent(interfaceType);
    }

    public <T, C extends T> void putComponent(Class<T> interfaceType, C component) {
        if (this.mComponents != null) {
            this.mComponents.put(interfaceType, component);
        }
    }

    public static void overrideNotificationAppName(Context context, Notification.Builder n) {
        Bundle extras = new Bundle();
        extras.putString("android.substName", context.getString(17039487));
        n.addExtras(extras);
    }
}
