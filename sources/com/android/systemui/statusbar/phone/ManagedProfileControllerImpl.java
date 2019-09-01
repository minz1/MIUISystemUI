package com.android.systemui.statusbar.phone;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;
import com.android.systemui.statusbar.phone.ManagedProfileController;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ManagedProfileControllerImpl implements ManagedProfileController {
    /* access modifiers changed from: private */
    public final List<ManagedProfileController.Callback> mCallbacks = new ArrayList();
    private final Context mContext;
    private int mCurrentUser;
    private boolean mListening;
    private final LinkedList<UserInfo> mProfiles;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            ManagedProfileControllerImpl.this.reloadManagedProfiles();
            for (ManagedProfileController.Callback callback : ManagedProfileControllerImpl.this.mCallbacks) {
                callback.onManagedProfileChanged();
            }
        }
    };
    private final UserManager mUserManager;

    public ManagedProfileControllerImpl(Context context) {
        this.mContext = context;
        this.mUserManager = UserManager.get(this.mContext);
        this.mProfiles = new LinkedList<>();
    }

    public void addCallback(ManagedProfileController.Callback callback) {
        this.mCallbacks.add(callback);
        if (this.mCallbacks.size() == 1) {
            setListening(true);
        }
        callback.onManagedProfileChanged();
    }

    public void removeCallback(ManagedProfileController.Callback callback) {
        if (this.mCallbacks.remove(callback) && this.mCallbacks.size() == 0) {
            setListening(false);
        }
    }

    /* access modifiers changed from: private */
    public void reloadManagedProfiles() {
        synchronized (this.mProfiles) {
            boolean hadProfile = this.mProfiles.size() > 0;
            int user = ActivityManager.getCurrentUser();
            this.mProfiles.clear();
            for (UserInfo ui : this.mUserManager.getEnabledProfiles(user)) {
                if (ui.isManagedProfile()) {
                    this.mProfiles.add(ui);
                }
            }
            if (this.mProfiles.size() == 0 && hadProfile && user == this.mCurrentUser) {
                for (ManagedProfileController.Callback callback : this.mCallbacks) {
                    callback.onManagedProfileRemoved();
                }
            }
            this.mCurrentUser = user;
        }
    }

    private void setListening(boolean listening) {
        this.mListening = listening;
        if (listening) {
            reloadManagedProfiles();
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.USER_SWITCHED");
            filter.addAction("android.intent.action.MANAGED_PROFILE_ADDED");
            filter.addAction("android.intent.action.MANAGED_PROFILE_REMOVED");
            filter.addAction("android.intent.action.MANAGED_PROFILE_AVAILABLE");
            filter.addAction("android.intent.action.MANAGED_PROFILE_UNAVAILABLE");
            this.mContext.registerReceiverAsUser(this.mReceiver, UserHandle.ALL, filter, null, null);
            return;
        }
        this.mContext.unregisterReceiver(this.mReceiver);
    }
}
