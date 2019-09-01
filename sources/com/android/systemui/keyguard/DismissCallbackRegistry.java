package com.android.systemui.keyguard;

import com.android.internal.policy.IKeyguardDismissCallback;
import com.android.systemui.Dependency;
import com.android.systemui.UiOffloadThread;
import java.util.ArrayList;

public class DismissCallbackRegistry {
    private final ArrayList<DismissCallbackWrapper> mDismissCallbacks = new ArrayList<>();
    private final UiOffloadThread mUiOffloadThread = ((UiOffloadThread) Dependency.get(UiOffloadThread.class));

    public void addCallback(IKeyguardDismissCallback callback) {
        this.mDismissCallbacks.add(new DismissCallbackWrapper(callback));
    }

    public void notifyDismissCancelled() {
        for (int i = this.mDismissCallbacks.size() - 1; i >= 0; i--) {
            final DismissCallbackWrapper callback = this.mDismissCallbacks.get(i);
            this.mUiOffloadThread.submit(new Runnable() {
                public void run() {
                    callback.notifyDismissCancelled();
                }
            });
        }
        this.mDismissCallbacks.clear();
    }

    public void notifyDismissSucceeded() {
        for (int i = this.mDismissCallbacks.size() - 1; i >= 0; i--) {
            final DismissCallbackWrapper callback = this.mDismissCallbacks.get(i);
            this.mUiOffloadThread.submit(new Runnable() {
                public void run() {
                    callback.notifyDismissSucceeded();
                }
            });
        }
        this.mDismissCallbacks.clear();
    }
}
