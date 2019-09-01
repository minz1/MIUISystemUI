package com.miui.systemui.support.v4.content;

import android.content.Context;
import android.os.Build;
import java.io.File;
import java.lang.reflect.Method;

public class ContextCompat {
    private static final Object sLock = new Object();

    public static File getDataDir(Context context) {
        File file = null;
        if (Build.VERSION.SDK_INT >= 24) {
            try {
                Method method = Context.class.getDeclaredMethod("getDataDir", new Class[0]);
                method.setAccessible(true);
                return (File) method.invoke(context, new Object[0]);
            } catch (Exception e) {
                return null;
            }
        } else {
            String dataDir = context.getApplicationInfo().dataDir;
            if (dataDir != null) {
                file = new File(dataDir);
            }
            return file;
        }
    }

    public static Context createDeviceProtectedStorageContext(Context context) {
        if (Build.VERSION.SDK_INT < 24) {
            return null;
        }
        try {
            Method method = Context.class.getDeclaredMethod("createDeviceProtectedStorageContext", new Class[0]);
            method.setAccessible(true);
            return (Context) method.invoke(context, new Object[0]);
        } catch (Exception e) {
            return null;
        }
    }
}
