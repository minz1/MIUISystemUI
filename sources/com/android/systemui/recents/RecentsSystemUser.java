package com.android.systemui.recents;

import android.content.Context;
import android.graphics.Rect;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.EventLog;
import android.util.Log;
import android.util.SparseArray;
import com.android.systemui.recents.IRecentsNonSystemUserCallbacks;
import com.android.systemui.recents.IRecentsSystemUserCallbacks;
import com.android.systemui.recents.events.RecentsEventBus;
import com.android.systemui.recents.events.activity.DockedTopTaskEvent;
import com.android.systemui.recents.events.activity.RecentsActivityStartingEvent;
import com.android.systemui.recents.events.ui.RecentsDrawnEvent;
import com.android.systemui.recents.misc.ForegroundThread;

public class RecentsSystemUser extends IRecentsSystemUserCallbacks.Stub {
    /* access modifiers changed from: private */
    public Context mContext;
    /* access modifiers changed from: private */
    public RecentsImpl mImpl;
    /* access modifiers changed from: private */
    public final SparseArray<IRecentsNonSystemUserCallbacks> mNonSystemUserRecents = new SparseArray<>();

    public RecentsSystemUser(Context context, RecentsImpl impl) {
        this.mContext = context;
        this.mImpl = impl;
    }

    public void registerNonSystemUserCallbacks(IBinder nonSystemUserCallbacks, final int userId) {
        try {
            final IRecentsNonSystemUserCallbacks callback = IRecentsNonSystemUserCallbacks.Stub.asInterface(nonSystemUserCallbacks);
            nonSystemUserCallbacks.linkToDeath(new IBinder.DeathRecipient() {
                public void binderDied() {
                    RecentsSystemUser.this.mNonSystemUserRecents.removeAt(RecentsSystemUser.this.mNonSystemUserRecents.indexOfValue(callback));
                    EventLog.writeEvent(36060, new Object[]{5, Integer.valueOf(userId)});
                }
            }, 0);
            this.mNonSystemUserRecents.put(userId, callback);
            EventLog.writeEvent(36060, new Object[]{4, Integer.valueOf(userId)});
        } catch (RemoteException e) {
            Log.e("RecentsSystemUser", "Failed to register NonSystemUserCallbacks", e);
        }
    }

    public IRecentsNonSystemUserCallbacks getNonSystemUserRecentsForUser(int userId) {
        return this.mNonSystemUserRecents.get(userId);
    }

    public void updateRecentsVisibility(final boolean visible) {
        ForegroundThread.getHandler().post(new Runnable() {
            public void run() {
                RecentsSystemUser.this.mImpl.onVisibilityChanged(RecentsSystemUser.this.mContext, visible);
            }
        });
    }

    public void startScreenPinning(final int taskId) {
        ForegroundThread.getHandler().post(new Runnable() {
            public void run() {
                RecentsSystemUser.this.mImpl.onStartScreenPinning(RecentsSystemUser.this.mContext, taskId);
            }
        });
    }

    public void sendRecentsDrawnEvent() {
        RecentsEventBus.getDefault().post(new RecentsDrawnEvent());
    }

    public void sendDockingTopTaskEvent(int dragMode, Rect initialRect) throws RemoteException {
        RecentsEventBus.getDefault().post(new DockedTopTaskEvent(dragMode, initialRect));
    }

    public void sendLaunchRecentsEvent() throws RemoteException {
        RecentsEventBus.getDefault().post(new RecentsActivityStartingEvent());
    }
}
