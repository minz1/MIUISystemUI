package com.android.systemui.miui;

import android.app.ActivityManagerCompat;
import android.app.IActivityManager;
import android.app.IMiuiActivityObserver;
import android.content.ComponentName;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import com.android.systemui.miui.ActivityObserver;
import java.util.ArrayList;
import java.util.List;

public class ActivityObserverImpl implements ActivityObserver {
    /* access modifiers changed from: private */
    public final List<ActivityObserver.ActivityObserverCallback> mCallbacks = new ArrayList();
    private final IMiuiActivityObserver mMiuiActivityObserver = new IMiuiActivityObserver.Stub() {
        public void activityIdle(Intent intent) throws RemoteException {
            synchronized (ActivityObserverImpl.this.mCallbacks) {
                for (ActivityObserver.ActivityObserverCallback callback : ActivityObserverImpl.this.mCallbacks) {
                    callback.activityIdle(intent);
                }
            }
        }

        public void activityResumed(Intent intent) throws RemoteException {
            if (!(intent == null || intent.getComponent() == null)) {
                ComponentName unused = ActivityObserverImpl.this.mTopActivity = intent.getComponent().clone();
            }
            synchronized (ActivityObserverImpl.this.mCallbacks) {
                for (ActivityObserver.ActivityObserverCallback callback : ActivityObserverImpl.this.mCallbacks) {
                    callback.activityResumed(intent);
                }
            }
        }

        public void activityPaused(Intent intent) throws RemoteException {
            synchronized (ActivityObserverImpl.this.mCallbacks) {
                for (ActivityObserver.ActivityObserverCallback callback : ActivityObserverImpl.this.mCallbacks) {
                    callback.activityPaused(intent);
                }
            }
        }

        public void activityStopped(Intent intent) throws RemoteException {
            synchronized (ActivityObserverImpl.this.mCallbacks) {
                for (ActivityObserver.ActivityObserverCallback callback : ActivityObserverImpl.this.mCallbacks) {
                    callback.activityStopped(intent);
                }
            }
        }

        public void activityDestroyed(Intent intent) throws RemoteException {
            synchronized (ActivityObserverImpl.this.mCallbacks) {
                for (ActivityObserver.ActivityObserverCallback callback : ActivityObserverImpl.this.mCallbacks) {
                    callback.activityDestroyed(intent);
                }
            }
        }

        /* JADX WARNING: type inference failed for: r0v0, types: [com.android.systemui.miui.ActivityObserverImpl$1, android.os.IBinder] */
        public IBinder asBinder() {
            return this;
        }
    };
    /* access modifiers changed from: private */
    public ComponentName mTopActivity;

    public ActivityObserverImpl() {
        IActivityManager am = ActivityManagerCompat.getService();
        if (am != null) {
            try {
                am.registerActivityObserver(this.mMiuiActivityObserver, new Intent());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public ComponentName getTopActivity() {
        return this.mTopActivity;
    }

    public void addCallback(ActivityObserver.ActivityObserverCallback listener) {
        synchronized (this.mCallbacks) {
            this.mCallbacks.add(listener);
        }
    }

    public void removeCallback(ActivityObserver.ActivityObserverCallback listener) {
        synchronized (this.mCallbacks) {
            this.mCallbacks.remove(listener);
        }
    }
}
