package com.android.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.UserHandle;
import android.text.TextUtils;
import com.android.systemui.statusbar.policy.DemoModeController;
import java.util.ArrayList;
import java.util.List;

public class DemoModeControllerImpl implements DemoModeController {
    /* access modifiers changed from: private */
    public final List<DemoModeController.DemoModeCallback> mCallbacks = new ArrayList();
    private final Context mContext;
    private final BroadcastReceiver mDemoReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if ("com.android.systemui.demo".equals(intent.getAction())) {
                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    String command = bundle.getString("command", "").trim().toLowerCase();
                    if (command.length() > 0) {
                        Bundle unused = DemoModeControllerImpl.this.mLastArgs = bundle;
                        String unused2 = DemoModeControllerImpl.this.mLastCommand = command;
                        try {
                            for (DemoModeController.DemoModeCallback callback : DemoModeControllerImpl.this.mCallbacks) {
                                callback.onDemoModeChanged(command, bundle);
                            }
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    }
                }
            }
        }
    };
    /* access modifiers changed from: private */
    public Bundle mLastArgs;
    /* access modifiers changed from: private */
    public String mLastCommand;

    public DemoModeControllerImpl(Context context) {
        this.mContext = context;
        IntentFilter demoFilter = new IntentFilter();
        demoFilter.addAction("com.android.systemui.demo");
        this.mContext.registerReceiverAsUser(this.mDemoReceiver, UserHandle.ALL, demoFilter, "android.permission.DUMP", null);
    }

    public void addCallback(DemoModeController.DemoModeCallback listener) {
        if (!this.mCallbacks.contains(listener)) {
            this.mCallbacks.add(listener);
            if (!TextUtils.isEmpty(this.mLastCommand)) {
                listener.onDemoModeChanged(this.mLastCommand, this.mLastArgs);
            }
        }
    }

    public void removeCallback(DemoModeController.DemoModeCallback listener) {
        if (this.mCallbacks.contains(listener)) {
            this.mCallbacks.remove(listener);
        }
    }
}
