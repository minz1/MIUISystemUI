package com.android.systemui;

import android.os.Handler;
import android.os.Looper;
import com.android.systemui.doze.DozeHost;
import com.android.systemui.miui.Dependencies;

public class Dependency {
    public static final DependencyKey<Looper> BG_LOOPER = new DependencyKey<>("background_looper");
    public static final DependencyKey<Looper> BT_BG_LOOPER = new DependencyKey<>("bluetooth_background_looper");
    public static final DependencyKey<String> LEAK_REPORT_EMAIL = new DependencyKey<>("leak_report_email");
    public static final DependencyKey<Handler> MAIN_HANDLER = new DependencyKey<>("main_handler");
    public static final DependencyKey<Looper> NET_BG_LOOPER = new DependencyKey<>("network_background_looper");
    public static final DependencyKey<Handler> SCREEN_OFF_HANDLER = new DependencyKey<>("screen_off_handler");
    public static final DependencyKey<Handler> TIME_TICK_HANDLER = new DependencyKey<>("time_tick_handler");
    private static DependencyResolver sDependency;
    private static DozeHost sHost;

    public static final class DependencyKey<V> {
        private final String mDisplayName;

        public DependencyKey(String displayName) {
            this.mDisplayName = displayName;
        }

        public String toString() {
            return this.mDisplayName;
        }
    }

    public interface DependencyProvider<T> {
        T createDependency();
    }

    public interface DependencyResolver {
        <T> T get(DependencyKey<T> dependencyKey);

        <T> T get(Class<T> cls);
    }

    public static void setDependencyResolver(DependencyResolver dependencyResolver) {
        sDependency = dependencyResolver;
    }

    public static <T> T get(Class<T> cls) {
        return sDependency.get(cls);
    }

    public static <T> T get(DependencyKey<T> cls) {
        return sDependency.get(cls);
    }

    public static void setHost(DozeHost host) {
        sHost = host;
    }

    public static DozeHost getHost() {
        return sHost;
    }

    public static void inject(Object object) {
        Dependencies.getInstance().injectDependencies(object);
    }
}
