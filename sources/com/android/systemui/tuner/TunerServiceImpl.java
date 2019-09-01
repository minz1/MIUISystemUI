package com.android.systemui.tuner;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import com.android.systemui.Dependency;
import com.android.systemui.settings.CurrentUserTracker;
import com.android.systemui.statusbar.phone.StatusBarIconControllerHelper;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.util.leak.LeakDetector;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class TunerServiceImpl extends TunerService {
    private ContentResolver mContentResolver;
    private final Context mContext;
    /* access modifiers changed from: private */
    public int mCurrentUser;
    private final ArrayMap<Uri, String> mListeningUris = new ArrayMap<>();
    private final Observer mObserver = new Observer();
    private final HashMap<String, Set<TunerService.Tunable>> mTunableLookup = new HashMap<>();
    private final HashSet<TunerService.Tunable> mTunables;
    private CurrentUserTracker mUserTracker;

    private class Observer extends ContentObserver {
        public Observer() {
            super(new Handler(Looper.getMainLooper()));
        }

        public void onChange(boolean selfChange, Uri uri, int userId) {
            if (userId == ActivityManager.getCurrentUser()) {
                TunerServiceImpl.this.reloadSetting(uri);
            }
        }
    }

    public TunerServiceImpl(Context context) {
        this.mTunables = LeakDetector.ENABLED ? new HashSet<>() : null;
        this.mContext = context;
        this.mContentResolver = this.mContext.getContentResolver();
        for (UserInfo user : UserManager.get(this.mContext).getUsers()) {
            this.mCurrentUser = user.getUserHandle().getIdentifier();
            if (getValue("sysui_tuner_version", 0) != 1) {
                upgradeTuner(getValue("sysui_tuner_version", 0), 1);
            }
        }
        this.mCurrentUser = ActivityManager.getCurrentUser();
        this.mUserTracker = new CurrentUserTracker(this.mContext) {
            public void onUserSwitched(int newUserId) {
                int unused = TunerServiceImpl.this.mCurrentUser = newUserId;
                TunerServiceImpl.this.reloadAll();
                TunerServiceImpl.this.reregisterAll();
            }
        };
        this.mUserTracker.startTracking();
    }

    private void upgradeTuner(int oldVersion, int newVersion) {
        if (oldVersion < 1) {
            String blacklistStr = getValue("icon_blacklist");
            if (blacklistStr != null) {
                ArraySet<String> iconBlacklist = StatusBarIconControllerHelper.getIconBlacklist(blacklistStr);
                iconBlacklist.add("rotate");
                iconBlacklist.add("headset");
                Settings.Secure.putStringForUser(this.mContentResolver, "icon_blacklist", TextUtils.join(",", iconBlacklist), this.mCurrentUser);
            }
        }
        setValue("sysui_tuner_version", newVersion);
    }

    public String getValue(String setting) {
        return Settings.Secure.getStringForUser(this.mContentResolver, setting, this.mCurrentUser);
    }

    public int getValue(String setting, int def) {
        return Settings.Secure.getIntForUser(this.mContentResolver, setting, def, this.mCurrentUser);
    }

    public void setValue(String setting, int value) {
        Settings.Secure.putIntForUser(this.mContentResolver, setting, value, this.mCurrentUser);
    }

    public void addTunable(TunerService.Tunable tunable, String... keys) {
        for (String key : keys) {
            addTunable(tunable, key);
        }
    }

    private void addTunable(TunerService.Tunable tunable, String key) {
        if (!this.mTunableLookup.containsKey(key)) {
            this.mTunableLookup.put(key, new ArraySet());
        }
        this.mTunableLookup.get(key).add(tunable);
        if (LeakDetector.ENABLED) {
            this.mTunables.add(tunable);
            ((LeakDetector) Dependency.get(LeakDetector.class)).trackCollection(this.mTunables, "TunerService.mTunables");
        }
        Uri uri = Settings.Secure.getUriFor(key);
        if (!this.mListeningUris.containsKey(uri)) {
            this.mListeningUris.put(uri, key);
            this.mContentResolver.registerContentObserver(uri, false, this.mObserver, this.mCurrentUser);
        }
        tunable.onTuningChanged(key, Settings.Secure.getStringForUser(this.mContentResolver, key, this.mCurrentUser));
    }

    public void removeTunable(TunerService.Tunable tunable) {
        for (Set<TunerService.Tunable> list : this.mTunableLookup.values()) {
            list.remove(tunable);
        }
        if (LeakDetector.ENABLED) {
            this.mTunables.remove(tunable);
        }
    }

    /* access modifiers changed from: protected */
    public void reregisterAll() {
        if (this.mListeningUris.size() != 0) {
            this.mContentResolver.unregisterContentObserver(this.mObserver);
            for (Uri uri : this.mListeningUris.keySet()) {
                this.mContentResolver.registerContentObserver(uri, false, this.mObserver, this.mCurrentUser);
            }
        }
    }

    /* access modifiers changed from: private */
    public void reloadSetting(Uri uri) {
        String key = this.mListeningUris.get(uri);
        Set<TunerService.Tunable> tunables = this.mTunableLookup.get(key);
        if (tunables != null) {
            String value = Settings.Secure.getStringForUser(this.mContentResolver, key, this.mCurrentUser);
            for (TunerService.Tunable tunable : tunables) {
                tunable.onTuningChanged(key, value);
            }
        }
    }

    /* access modifiers changed from: private */
    public void reloadAll() {
        for (String key : this.mTunableLookup.keySet()) {
            String value = Settings.Secure.getStringForUser(this.mContentResolver, key, this.mCurrentUser);
            for (TunerService.Tunable tunable : this.mTunableLookup.get(key)) {
                tunable.onTuningChanged(key, value);
            }
        }
    }

    public void clearAll() {
        Settings.Global.putString(this.mContentResolver, "sysui_demo_allowed", null);
        Intent intent = new Intent("com.android.systemui.demo");
        intent.putExtra("command", "exit");
        this.mContext.sendBroadcast(intent);
        for (String key : this.mTunableLookup.keySet()) {
            Settings.Secure.putString(this.mContentResolver, key, null);
        }
    }
}
