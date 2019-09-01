package com.android.systemui.statusbar.policy;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import com.android.systemui.Dependency;
import com.android.systemui.settings.CurrentUserTracker;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import com.miui.systemui.annotation.Inject;
import java.util.ArrayList;
import java.util.Iterator;

public class DeviceProvisionedControllerImpl extends CurrentUserTracker implements DeviceProvisionedController {
    private final ContentResolver mContentResolver;
    private final Context mContext;
    private final Uri mDeviceProvisionedUri;
    private final ArrayList<DeviceProvisionedController.DeviceProvisionedListener> mListeners = new ArrayList<>();
    protected final ContentObserver mSettingsObserver = new ContentObserver((Handler) Dependency.get(Dependency.MAIN_HANDLER)) {
        public void onChange(boolean selfChange, Uri uri, int userId) {
            if (DeviceProvisionedControllerImpl.this.mUserSetupUri.equals(uri)) {
                DeviceProvisionedControllerImpl.this.notifySetupChanged();
            } else {
                DeviceProvisionedControllerImpl.this.notifyProvisionedChanged();
            }
        }
    };
    /* access modifiers changed from: private */
    public final Uri mUserSetupUri;

    public DeviceProvisionedControllerImpl(@Inject Context context) {
        super(context);
        this.mContext = context;
        this.mContentResolver = context.getContentResolver();
        this.mDeviceProvisionedUri = Settings.Global.getUriFor("device_provisioned");
        this.mUserSetupUri = Settings.Secure.getUriFor("user_setup_complete");
    }

    public boolean isDeviceProvisioned() {
        return Settings.Global.getInt(this.mContentResolver, "device_provisioned", 0) != 0;
    }

    public boolean isUserSetup(int currentUser) {
        return Settings.Secure.getIntForUser(this.mContentResolver, "user_setup_complete", 0, currentUser) != 0;
    }

    public int getCurrentUser() {
        return ActivityManager.getCurrentUser();
    }

    public boolean isCurrentUserSetup() {
        return isUserSetup(getCurrentUser());
    }

    public void addCallback(DeviceProvisionedController.DeviceProvisionedListener listener) {
        this.mListeners.add(listener);
        if (this.mListeners.size() == 1) {
            startListening(getCurrentUser());
        }
        listener.onUserSetupChanged();
        listener.onDeviceProvisionedChanged();
    }

    public void removeCallback(DeviceProvisionedController.DeviceProvisionedListener listener) {
        this.mListeners.remove(listener);
        if (this.mListeners.size() == 0) {
            stopListening();
        }
    }

    private void startListening(int user) {
        this.mContentResolver.registerContentObserver(this.mDeviceProvisionedUri, true, this.mSettingsObserver, 0);
        this.mContentResolver.registerContentObserver(this.mUserSetupUri, true, this.mSettingsObserver, user);
        startTracking();
    }

    private void stopListening() {
        stopTracking();
        this.mContentResolver.unregisterContentObserver(this.mSettingsObserver);
    }

    public void onUserSwitched(int newUserId) {
        this.mContentResolver.unregisterContentObserver(this.mSettingsObserver);
        this.mContentResolver.registerContentObserver(this.mDeviceProvisionedUri, true, this.mSettingsObserver, 0);
        this.mContentResolver.registerContentObserver(this.mUserSetupUri, true, this.mSettingsObserver, newUserId);
        notifyUserChanged();
    }

    private void notifyUserChanged() {
        Iterator<DeviceProvisionedController.DeviceProvisionedListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onUserSwitched();
        }
    }

    /* access modifiers changed from: private */
    public void notifySetupChanged() {
        Iterator<DeviceProvisionedController.DeviceProvisionedListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onUserSetupChanged();
        }
    }

    /* access modifiers changed from: private */
    public void notifyProvisionedChanged() {
        Iterator<DeviceProvisionedController.DeviceProvisionedListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onDeviceProvisionedChanged();
        }
    }
}
