package com.android.systemui.recents.misc;

import android.app.ActivityManager;
import android.app.ITaskStackListener;
import android.content.ComponentName;
import android.os.RemoteException;

public abstract class TaskStackListener extends ITaskStackListener.Stub {
    public void onTaskStackChanged() throws RemoteException {
    }

    public void onActivityUnpinned() throws RemoteException {
    }

    public void onPinnedActivityRestartAttempt(boolean clearedTask) throws RemoteException {
    }

    public void onPinnedStackAnimationStarted() throws RemoteException {
    }

    public void onPinnedStackAnimationEnded() throws RemoteException {
    }

    public void onActivityForcedResizable(String packageName, int taskId, int reason) throws RemoteException {
    }

    public void onActivityDismissingDockedStack() throws RemoteException {
    }

    public void onActivityLaunchOnSecondaryDisplayFailed() throws RemoteException {
    }

    public void onTaskCreated(int taskId, ComponentName componentName) throws RemoteException {
    }

    public void onTaskRemoved(int taskId) throws RemoteException {
    }

    public void onTaskMovedToFront(int taskId) throws RemoteException {
    }

    public void onTaskRemovalStarted(int taskId) {
    }

    public void onTaskDescriptionChanged(int taskId, ActivityManager.TaskDescription td) throws RemoteException {
    }

    public void onActivityRequestedOrientationChanged(int taskId, int requestedOrientation) throws RemoteException {
    }

    public void onTaskProfileLocked(int taskId, int userId) {
    }

    public void onTaskSnapshotChanged(int taskId, ActivityManager.TaskSnapshot snapshot) throws RemoteException {
    }
}
