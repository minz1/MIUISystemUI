package com.android.systemui.statusbar.policy;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.service.notification.Condition;
import android.service.notification.IConditionListener;
import android.service.notification.ZenModeConfig;
import android.util.Log;
import android.util.Slog;
import com.android.systemui.qs.GlobalSetting;
import com.android.systemui.settings.CurrentUserTracker;
import com.android.systemui.statusbar.policy.ZenModeController;
import com.android.systemui.util.Utils;
import com.android.systemui.util.function.Consumer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Objects;

public class ZenModeControllerImpl extends CurrentUserTracker implements ZenModeController {
    /* access modifiers changed from: private */
    public static final boolean DEBUG = Log.isLoggable("ZenModeController", 3);
    private final AlarmManager mAlarmManager;
    private final ArrayList<ZenModeController.Callback> mCallbacks = new ArrayList<>();
    private final LinkedHashMap<Uri, Condition> mConditions = new LinkedHashMap<>();
    private ZenModeConfig mConfig;
    private final GlobalSetting mConfigSetting;
    /* access modifiers changed from: private */
    public final Context mContext;
    private final IConditionListener mListener = new IConditionListener.Stub() {
        public void onConditionsReceived(Condition[] conditions) {
            if (ZenModeControllerImpl.DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("onConditionsReceived ");
                sb.append(conditions == null ? 0 : conditions.length);
                sb.append(" mRequesting=");
                sb.append(ZenModeControllerImpl.this.mRequesting);
                Slog.d("ZenModeController", sb.toString());
            }
            if (ZenModeControllerImpl.this.mRequesting) {
                ZenModeControllerImpl.this.updateConditions(conditions);
            }
        }
    };
    private final GlobalSetting mModeSetting;
    private final NotificationManager mNoMan;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if ("android.app.action.NEXT_ALARM_CLOCK_CHANGED".equals(intent.getAction())) {
                ZenModeControllerImpl.this.fireNextAlarmChanged();
            }
            if ("android.os.action.ACTION_EFFECTS_SUPPRESSOR_CHANGED".equals(intent.getAction())) {
                ZenModeControllerImpl.this.fireEffectsSuppressorChanged();
            }
        }
    };
    private boolean mRegistered;
    /* access modifiers changed from: private */
    public boolean mRequesting;
    private final SetupObserver mSetupObserver;
    /* access modifiers changed from: private */
    public int mUserId;
    private final UserManager mUserManager;

    private final class SetupObserver extends ContentObserver {
        private boolean mRegistered;
        private final ContentResolver mResolver;

        public SetupObserver(Handler handler) {
            super(handler);
            this.mResolver = ZenModeControllerImpl.this.mContext.getContentResolver();
        }

        public boolean isUserSetup() {
            return Settings.Secure.getIntForUser(this.mResolver, "user_setup_complete", 0, ZenModeControllerImpl.this.mUserId) != 0;
        }

        public boolean isDeviceProvisioned() {
            return Settings.Global.getInt(this.mResolver, "device_provisioned", 0) != 0;
        }

        public void register() {
            if (this.mRegistered) {
                this.mResolver.unregisterContentObserver(this);
            }
            this.mResolver.registerContentObserver(Settings.Global.getUriFor("device_provisioned"), false, this);
            this.mResolver.registerContentObserver(Settings.Secure.getUriFor("user_setup_complete"), false, this, ZenModeControllerImpl.this.mUserId);
            ZenModeControllerImpl.this.fireZenAvailableChanged(ZenModeControllerImpl.this.isZenAvailable());
        }

        public void onChange(boolean selfChange, Uri uri) {
            if (Settings.Global.getUriFor("device_provisioned").equals(uri) || Settings.Secure.getUriFor("user_setup_complete").equals(uri)) {
                ZenModeControllerImpl.this.fireZenAvailableChanged(ZenModeControllerImpl.this.isZenAvailable());
            }
        }
    }

    public ZenModeControllerImpl(Context context, Handler handler) {
        super(context);
        this.mContext = context;
        this.mModeSetting = new GlobalSetting(this.mContext, handler, "zen_mode") {
            /* access modifiers changed from: protected */
            public void handleValueChanged(int value) {
                ZenModeControllerImpl.this.fireZenChanged(value);
            }
        };
        this.mConfigSetting = new GlobalSetting(this.mContext, handler, "zen_mode_config_etag") {
            /* access modifiers changed from: protected */
            public void handleValueChanged(int value) {
                ZenModeControllerImpl.this.updateZenModeConfig();
            }
        };
        this.mNoMan = (NotificationManager) context.getSystemService("notification");
        this.mConfig = this.mNoMan.getZenModeConfig();
        this.mModeSetting.setListening(true);
        this.mConfigSetting.setListening(true);
        this.mAlarmManager = (AlarmManager) context.getSystemService("alarm");
        this.mSetupObserver = new SetupObserver(handler);
        this.mSetupObserver.register();
        this.mUserManager = (UserManager) context.getSystemService(UserManager.class);
        startTracking();
    }

    public void addCallback(ZenModeController.Callback callback) {
        this.mCallbacks.add(callback);
    }

    public void removeCallback(ZenModeController.Callback callback) {
        this.mCallbacks.remove(callback);
    }

    public int getZen() {
        return this.mModeSetting.getValue();
    }

    public boolean isZenAvailable() {
        return this.mSetupObserver.isDeviceProvisioned() && this.mSetupObserver.isUserSetup();
    }

    public void onUserSwitched(int userId) {
        this.mUserId = userId;
        if (this.mRegistered) {
            this.mContext.unregisterReceiver(this.mReceiver);
        }
        IntentFilter filter = new IntentFilter("android.app.action.NEXT_ALARM_CLOCK_CHANGED");
        filter.addAction("android.os.action.ACTION_EFFECTS_SUPPRESSOR_CHANGED");
        this.mContext.registerReceiverAsUser(this.mReceiver, new UserHandle(this.mUserId), filter, null, null);
        this.mRegistered = true;
        this.mSetupObserver.register();
    }

    public int getCurrentUser() {
        return ActivityManager.getCurrentUser();
    }

    /* access modifiers changed from: private */
    public void fireNextAlarmChanged() {
        Utils.safeForeach(this.mCallbacks, new Consumer<ZenModeController.Callback>() {
            public void accept(ZenModeController.Callback c) {
                c.onNextAlarmChanged();
            }
        });
    }

    /* access modifiers changed from: private */
    public void fireEffectsSuppressorChanged() {
        Utils.safeForeach(this.mCallbacks, new Consumer<ZenModeController.Callback>() {
            public void accept(ZenModeController.Callback c) {
                c.onEffectsSupressorChanged();
            }
        });
    }

    /* access modifiers changed from: private */
    public void fireZenChanged(final int zen) {
        Utils.safeForeach(this.mCallbacks, new Consumer<ZenModeController.Callback>() {
            public void accept(ZenModeController.Callback c) {
                c.onZenChanged(zen);
            }
        });
    }

    /* access modifiers changed from: private */
    public void fireZenAvailableChanged(final boolean available) {
        Utils.safeForeach(this.mCallbacks, new Consumer<ZenModeController.Callback>() {
            public void accept(ZenModeController.Callback c) {
                c.onZenAvailableChanged(available);
            }
        });
    }

    private void fireConditionsChanged(final Condition[] conditions) {
        Utils.safeForeach(this.mCallbacks, new Consumer<ZenModeController.Callback>() {
            public void accept(ZenModeController.Callback c) {
                c.onConditionsChanged(conditions);
            }
        });
    }

    private void fireManualRuleChanged(final ZenModeConfig.ZenRule rule) {
        Utils.safeForeach(this.mCallbacks, new Consumer<ZenModeController.Callback>() {
            public void accept(ZenModeController.Callback c) {
                c.onManualRuleChanged(rule);
            }
        });
    }

    /* access modifiers changed from: protected */
    public void fireConfigChanged(final ZenModeConfig config) {
        Utils.safeForeach(this.mCallbacks, new Consumer<ZenModeController.Callback>() {
            public void accept(ZenModeController.Callback c) {
                c.onConfigChanged(config);
            }
        });
    }

    /* access modifiers changed from: private */
    public void updateConditions(Condition[] conditions) {
        if (conditions != null && conditions.length != 0) {
            for (Condition c : conditions) {
                if ((c.flags & 1) != 0) {
                    this.mConditions.put(c.id, c);
                }
            }
            fireConditionsChanged((Condition[]) this.mConditions.values().toArray(new Condition[this.mConditions.values().size()]));
        }
    }

    /* access modifiers changed from: private */
    public void updateZenModeConfig() {
        ZenModeConfig config = this.mNoMan.getZenModeConfig();
        if (!Objects.equals(config, this.mConfig)) {
            ZenModeConfig.ZenRule newRule = null;
            ZenModeConfig.ZenRule oldRule = this.mConfig != null ? this.mConfig.manualRule : null;
            this.mConfig = config;
            fireConfigChanged(config);
            if (config != null) {
                newRule = config.manualRule;
            }
            if (!Objects.equals(oldRule, newRule)) {
                fireManualRuleChanged(newRule);
            }
        }
    }
}
