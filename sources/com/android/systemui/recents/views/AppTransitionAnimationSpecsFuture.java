package com.android.systemui.recents.views;

import android.os.Handler;
import android.os.RemoteException;
import android.view.AppTransitionAnimationSpec;
import android.view.IAppTransitionAnimationSpecsFuture;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public abstract class AppTransitionAnimationSpecsFuture {
    /* access modifiers changed from: private */
    public FutureTask<List<AppTransitionAnimationSpec>> mComposeTask = new FutureTask<>(new Callable<List<AppTransitionAnimationSpec>>() {
        public List<AppTransitionAnimationSpec> call() throws Exception {
            return AppTransitionAnimationSpecsFuture.this.composeSpecs();
        }
    });
    private final IAppTransitionAnimationSpecsFuture mFuture = new IAppTransitionAnimationSpecsFuture.Stub() {
        public AppTransitionAnimationSpec[] get() throws RemoteException {
            try {
                if (!AppTransitionAnimationSpecsFuture.this.mComposeTask.isDone()) {
                    AppTransitionAnimationSpecsFuture.this.mHandler.post(AppTransitionAnimationSpecsFuture.this.mComposeTask);
                }
                List<AppTransitionAnimationSpec> specs = (List) AppTransitionAnimationSpecsFuture.this.mComposeTask.get();
                FutureTask unused = AppTransitionAnimationSpecsFuture.this.mComposeTask = null;
                if (specs == null) {
                    return null;
                }
                AppTransitionAnimationSpec[] arr = new AppTransitionAnimationSpec[specs.size()];
                for (int i = 0; i < specs.size(); i++) {
                    arr[i] = specs.get(i);
                }
                return arr;
            } catch (Exception e) {
                return null;
            }
        }
    };
    /* access modifiers changed from: private */
    public final Handler mHandler;

    public abstract List<AppTransitionAnimationSpec> composeSpecs();

    public AppTransitionAnimationSpecsFuture(Handler handler) {
        this.mHandler = handler;
    }

    public final IAppTransitionAnimationSpecsFuture getFuture() {
        return this.mFuture;
    }
}
