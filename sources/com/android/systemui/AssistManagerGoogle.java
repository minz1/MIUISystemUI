package com.android.systemui;

import android.content.ContentResolver;
import android.content.Context;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.systemui.assist.AssistManager;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;

public class AssistManagerGoogle extends AssistManager {
    private final ContentObserver mContentObserver = new AssistantSettingsObserver();
    private final ContentResolver mContentResolver;
    private final AssistantStateReceiver mEnableReceiver = new AssistantStateReceiver();
    private final OpaEnableDispatcher mOpaEnableDispatcher;
    private final KeyguardUpdateMonitorCallback mUserSwitchCallback = new KeyguardUpdateMonitorCallback() {
        public void onUserSwitching(int userId) {
            Log.v("AssistantStateReceiver", "onUserSwitching");
            AssistManagerGoogle.this.updateAssistantEnabledState();
            AssistManagerGoogle.this.unregisterSettingsObserver();
            AssistManagerGoogle.this.registerSettingsObserver();
            AssistManagerGoogle.this.unregisterEnableReceiver();
            AssistManagerGoogle.this.registerEnableReceiver(userId);
        }
    };

    private class AssistantSettingsObserver extends ContentObserver {
        public AssistantSettingsObserver() {
            super(new Handler());
        }

        public void onChange(boolean selfChange, Uri uri) {
            Log.v("AssistantStateReceiver", "AssistantSettingsObserver");
            AssistManagerGoogle.this.updateAssistantEnabledState();
        }
    }

    public AssistManagerGoogle(DeviceProvisionedController controller, Context context) {
        super(controller, context);
        this.mContentResolver = context.getContentResolver();
        this.mOpaEnableDispatcher = new OpaEnableDispatcher(context, this.mAssistUtils);
        KeyguardUpdateMonitor.getInstance(this.mContext).registerCallback(this.mUserSwitchCallback);
        registerSettingsObserver();
        registerEnableReceiver(-2);
    }

    public boolean shouldShowOrb() {
        return false;
    }

    /* access modifiers changed from: private */
    public void registerEnableReceiver(int userId) {
        Context context = this.mContext;
        AssistantStateReceiver assistantStateReceiver = this.mEnableReceiver;
        UserHandle userHandle = new UserHandle(userId);
        AssistantStateReceiver assistantStateReceiver2 = this.mEnableReceiver;
        context.registerReceiverAsUser(assistantStateReceiver, userHandle, new IntentFilter("com.google.android.systemui.OPA_ENABLED"), null, null);
    }

    /* access modifiers changed from: private */
    public void unregisterEnableReceiver() {
        this.mContext.unregisterReceiver(this.mEnableReceiver);
    }

    /* access modifiers changed from: private */
    public void updateAssistantEnabledState() {
        this.mOpaEnableDispatcher.dispatchOpaEnabled(UserSettingsUtils.load(this.mContentResolver));
    }

    /* access modifiers changed from: private */
    public void registerSettingsObserver() {
        this.mContentResolver.registerContentObserver(Settings.Secure.getUriFor("assistant"), false, this.mContentObserver, KeyguardUpdateMonitor.getCurrentUser());
    }

    /* access modifiers changed from: private */
    public void unregisterSettingsObserver() {
        this.mContentResolver.unregisterContentObserver(this.mContentObserver);
    }

    public void dispatchOpaEnabledState() {
        updateAssistantEnabledState();
    }
}
