package com.android.systemui;

import android.content.Context;
import android.util.ArrayMap;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.ViewMediatorCallback;
import com.android.systemui.Dependency;
import com.android.systemui.keyguard.DismissCallbackRegistry;
import com.android.systemui.miui.ActivityObserver;
import com.android.systemui.miui.ActivityObserverImpl;
import com.android.systemui.miui.AppIconsManager;
import com.android.systemui.miui.ToastOverlayManager;
import com.android.systemui.miui.policy.NotificationsMonitor;
import com.android.systemui.miui.policy.NotificationsMonitorImpl;
import com.android.systemui.qs.QSTileHost;
import com.android.systemui.statusbar.KeyguardIndicationController;
import com.android.systemui.statusbar.LockScreenMagazineController;
import com.android.systemui.statusbar.ScrimView;
import com.android.systemui.statusbar.phone.KeyguardBouncer;
import com.android.systemui.statusbar.phone.LightBarController;
import com.android.systemui.statusbar.phone.LockscreenWallpaper;
import com.android.systemui.statusbar.phone.NotificationIconAreaController;
import com.android.systemui.statusbar.phone.NotificationPanelView;
import com.android.systemui.statusbar.phone.NotificationPeekingIconAreaController;
import com.android.systemui.statusbar.phone.ScrimController;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.phone.StatusBarIconController;
import com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager;

public class SystemUIFactory {
    static SystemUIFactory mFactory;

    public static SystemUIFactory getInstance() {
        return mFactory;
    }

    public static void createFromConfig(Context context) {
        String clsName = context.getString(R.string.config_systemUIFactoryComponent);
        if (clsName == null || clsName.length() == 0) {
            throw new RuntimeException("No SystemUIFactory component configured");
        }
        try {
            mFactory = (SystemUIFactory) context.getClassLoader().loadClass(clsName).newInstance();
        } catch (Throwable t) {
            Log.w("SystemUIFactory", "Error creating SystemUIFactory component: " + clsName, t);
            throw new RuntimeException(t);
        }
    }

    public StatusBarKeyguardViewManager createStatusBarKeyguardViewManager(Context context, ViewMediatorCallback viewMediatorCallback, LockPatternUtils lockPatternUtils) {
        return new StatusBarKeyguardViewManager(context, viewMediatorCallback, lockPatternUtils);
    }

    public KeyguardBouncer createKeyguardBouncer(Context context, ViewMediatorCallback callback, LockPatternUtils lockPatternUtils, ViewGroup container, DismissCallbackRegistry dismissCallbackRegistry) {
        KeyguardBouncer keyguardBouncer = new KeyguardBouncer(context, callback, lockPatternUtils, container, dismissCallbackRegistry);
        return keyguardBouncer;
    }

    public ScrimController createScrimController(LightBarController lightBarController, ScrimView scrimBehind, ScrimView scrimInFront, View headsUpScrim, LockscreenWallpaper lockscreenWallpaper) {
        return new ScrimController(lightBarController, scrimBehind, scrimInFront, headsUpScrim);
    }

    public NotificationIconAreaController createNotificationIconAreaController(Context context, StatusBar statusBar) {
        if (context.getResources().getBoolean(R.bool.status_bar_notification_icons_peeking)) {
            return new NotificationPeekingIconAreaController(context, statusBar);
        }
        return new NotificationIconAreaController(context, statusBar);
    }

    public KeyguardIndicationController createKeyguardIndicationController(Context context, NotificationPanelView notificationPanelView) {
        return new KeyguardIndicationController(context, notificationPanelView);
    }

    public LockScreenMagazineController createKeyguardWallpaperCarouselController(Context context, ViewGroup notificationPanelView, StatusBar statusBar) {
        return new LockScreenMagazineController(context, notificationPanelView, statusBar);
    }

    public QSTileHost createQSTileHost(Context context, StatusBar statusBar, StatusBarIconController iconController) {
        return new QSTileHost(context, statusBar, iconController);
    }

    public <T> T createInstance(Class<T> cls) {
        return null;
    }

    public void injectDependencies(ArrayMap<Object, Dependency.DependencyProvider> providers, Context context) {
        providers.put(ToastOverlayManager.class, new Dependency.DependencyProvider() {
            public Object createDependency() {
                return new ToastOverlayManager();
            }
        });
        providers.put(AppIconsManager.class, new Dependency.DependencyProvider() {
            public Object createDependency() {
                return new AppIconsManager();
            }
        });
        providers.put(NotificationsMonitor.class, new Dependency.DependencyProvider() {
            public Object createDependency() {
                return new NotificationsMonitorImpl();
            }
        });
        providers.put(ActivityObserver.class, new Dependency.DependencyProvider() {
            public Object createDependency() {
                return new ActivityObserverImpl();
            }
        });
    }
}
