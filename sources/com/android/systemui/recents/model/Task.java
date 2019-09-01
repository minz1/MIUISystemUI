package com.android.systemui.recents.model;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.ViewDebug;
import com.android.systemui.proxy.ActivityManager;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.misc.Utilities;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Objects;

public class Task {
    @ViewDebug.ExportedProperty(category = "recents")
    public int affiliationColor;
    @ViewDebug.ExportedProperty(category = "recents")
    public int affiliationTaskId;
    @ViewDebug.ExportedProperty(category = "recents")
    public String appInfoDescription;
    @ViewDebug.ExportedProperty(category = "recents")
    public Rect bounds;
    @ViewDebug.ExportedProperty(category = "recents")
    public int colorBackground;
    @ViewDebug.ExportedProperty(category = "recents")
    public int colorPrimary;
    @ViewDebug.ExportedProperty(category = "recents")
    public String dismissDescription;
    @ViewDebug.ExportedProperty(deepExport = true, prefix = "group_")
    public TaskGrouping group;
    public Drawable icon;
    @ViewDebug.ExportedProperty(category = "recents")
    public boolean isAccessLocked;
    @ViewDebug.ExportedProperty(category = "recents")
    public boolean isDockable;
    @ViewDebug.ExportedProperty(category = "recents")
    public boolean isLaunchTarget;
    @ViewDebug.ExportedProperty(category = "recents")
    public boolean isLocked;
    @ViewDebug.ExportedProperty(category = "recents")
    public boolean isStackTask;
    @ViewDebug.ExportedProperty(category = "recents")
    public boolean isSystemApp;
    @ViewDebug.ExportedProperty(deepExport = true, prefix = "key_")
    public TaskKey key;
    private ArrayList<TaskCallbacks> mCallbacks = new ArrayList<>();
    @ViewDebug.ExportedProperty(category = "recents")
    public int resizeMode;
    public ActivityManager.TaskDescription taskDescription;
    public int temporarySortIndexInStack;
    public Bitmap thumbnail;
    @ViewDebug.ExportedProperty(category = "recents")
    public String title;
    @ViewDebug.ExportedProperty(category = "recents")
    public String titleDescription;
    @ViewDebug.ExportedProperty(category = "recents")
    public ComponentName topActivity;
    @ViewDebug.ExportedProperty(category = "recents")
    public boolean useLightOnPrimaryColor;

    public interface TaskCallbacks {
        void onTaskDataLoaded(Task task, ActivityManager.TaskThumbnailInfo taskThumbnailInfo);

        void onTaskDataUnloaded();

        void onTaskStackIdChanged();
    }

    public static class TaskKey {
        @ViewDebug.ExportedProperty(category = "recents")
        public final Intent baseIntent;
        @ViewDebug.ExportedProperty(category = "recents")
        public long firstActiveTime;
        @ViewDebug.ExportedProperty(category = "recents")
        public final int id;
        @ViewDebug.ExportedProperty(category = "recents")
        public long lastActiveTime;
        private int mHashCode;
        @ViewDebug.ExportedProperty(category = "recents")
        public int stackId;
        @ViewDebug.ExportedProperty(category = "recents")
        public final int userId;
        public int windowingMode;

        public TaskKey(int id2, int stackId2, int windowingMode2, Intent intent, int userId2, long firstActiveTime2, long lastActiveTime2) {
            this.id = id2;
            this.stackId = stackId2;
            this.windowingMode = windowingMode2;
            this.baseIntent = intent;
            this.userId = userId2;
            this.firstActiveTime = firstActiveTime2;
            this.lastActiveTime = lastActiveTime2;
            updateHashCode();
        }

        public void setStackId(int stackId2) {
            this.stackId = stackId2;
            updateHashCode();
        }

        public ComponentName getComponent() {
            return this.baseIntent.getComponent();
        }

        public boolean equals(Object o) {
            boolean z = false;
            if (!(o instanceof TaskKey)) {
                return false;
            }
            TaskKey otherKey = (TaskKey) o;
            if (this.id == otherKey.id && this.stackId == otherKey.stackId && this.userId == otherKey.userId && this.windowingMode == otherKey.windowingMode) {
                z = true;
            }
            return z;
        }

        public int hashCode() {
            return this.mHashCode;
        }

        public String toString() {
            return "id=" + this.id + " stackId=" + this.stackId + " windowingMode=" + this.windowingMode + " user=" + this.userId + " lastActiveTime=" + this.lastActiveTime;
        }

        private void updateHashCode() {
            this.mHashCode = Objects.hash(new Object[]{Integer.valueOf(this.id), Integer.valueOf(this.stackId), Integer.valueOf(this.windowingMode), Integer.valueOf(this.userId)});
        }
    }

    public Task() {
    }

    public Task(TaskKey key2, int affiliationTaskId2, int affiliationColor2, Drawable icon2, Bitmap thumbnail2, String title2, String titleDescription2, String dismissDescription2, String appInfoDescription2, int colorPrimary2, int colorBackground2, boolean isLaunchTarget2, boolean isStackTask2, boolean isSystemApp2, boolean isDockable2, Rect bounds2, ActivityManager.TaskDescription taskDescription2, int resizeMode2, ComponentName topActivity2, boolean isLocked2, boolean isAccessLocked2) {
        TaskKey taskKey = key2;
        int i = affiliationTaskId2;
        int i2 = affiliationColor2;
        boolean z = true;
        boolean hasAffiliationGroupColor = (i != taskKey.id) && i2 != 0;
        this.key = taskKey;
        this.affiliationTaskId = i;
        this.affiliationColor = i2;
        this.icon = icon2;
        this.thumbnail = thumbnail2;
        this.title = title2;
        this.titleDescription = titleDescription2;
        this.dismissDescription = dismissDescription2;
        this.appInfoDescription = appInfoDescription2;
        this.colorPrimary = hasAffiliationGroupColor ? i2 : colorPrimary2;
        this.colorBackground = colorBackground2;
        this.useLightOnPrimaryColor = Utilities.computeContrastBetweenColors(this.colorPrimary, -1) <= 3.0f ? false : z;
        this.bounds = bounds2;
        this.taskDescription = taskDescription2;
        this.isLaunchTarget = isLaunchTarget2;
        this.isStackTask = isStackTask2;
        this.isSystemApp = isSystemApp2;
        this.isDockable = isDockable2;
        this.resizeMode = resizeMode2;
        this.topActivity = topActivity2;
        this.isLocked = isLocked2;
        this.isAccessLocked = isAccessLocked2;
    }

    public void copyFrom(Task o) {
        this.key = o.key;
        this.group = o.group;
        this.affiliationTaskId = o.affiliationTaskId;
        this.affiliationColor = o.affiliationColor;
        this.icon = o.icon;
        this.thumbnail = o.thumbnail;
        this.title = o.title;
        this.titleDescription = o.titleDescription;
        this.dismissDescription = o.dismissDescription;
        this.appInfoDescription = o.appInfoDescription;
        this.colorPrimary = o.colorPrimary;
        this.colorBackground = o.colorBackground;
        this.useLightOnPrimaryColor = o.useLightOnPrimaryColor;
        this.bounds = o.bounds;
        this.taskDescription = o.taskDescription;
        this.isLaunchTarget = o.isLaunchTarget;
        this.isStackTask = o.isStackTask;
        this.isSystemApp = o.isSystemApp;
        this.isDockable = o.isDockable;
        this.resizeMode = o.resizeMode;
        this.topActivity = o.topActivity;
        this.isLocked = o.isLocked;
        this.isAccessLocked = o.isAccessLocked;
    }

    public void addCallback(TaskCallbacks cb) {
        if (!this.mCallbacks.contains(cb)) {
            this.mCallbacks.add(cb);
        }
    }

    public void removeCallback(TaskCallbacks cb) {
        this.mCallbacks.remove(cb);
    }

    public void setGroup(TaskGrouping group2) {
        this.group = group2;
    }

    public void setStackId(int stackId) {
        this.key.setStackId(stackId);
        int callbackCount = this.mCallbacks.size();
        for (int i = 0; i < callbackCount; i++) {
            this.mCallbacks.get(i).onTaskStackIdChanged();
        }
    }

    public boolean isFreeformTask() {
        return Recents.getSystemServices().hasFreeformWorkspaceSupport() && SystemServicesProxy.isFreeformStack(this.key.stackId);
    }

    public void notifyTaskDataLoaded(Bitmap thumbnail2, Drawable applicationIcon, ActivityManager.TaskThumbnailInfo thumbnailInfo) {
        this.icon = applicationIcon;
        this.thumbnail = thumbnail2;
        int callbackCount = this.mCallbacks.size();
        for (int i = 0; i < callbackCount; i++) {
            this.mCallbacks.get(i).onTaskDataLoaded(this, thumbnailInfo);
        }
    }

    public void notifyTaskDataUnloaded(Bitmap defaultThumbnail, Drawable defaultApplicationIcon) {
        this.icon = defaultApplicationIcon;
        this.thumbnail = defaultThumbnail;
        for (int i = this.mCallbacks.size() - 1; i >= 0; i--) {
            this.mCallbacks.get(i).onTaskDataUnloaded();
        }
    }

    public boolean isAffiliatedTask() {
        return this.key.id != this.affiliationTaskId;
    }

    public ComponentName getTopComponent() {
        if (this.topActivity != null) {
            return this.topActivity;
        }
        return this.key.baseIntent.getComponent();
    }

    public boolean equals(Object o) {
        return this.key.equals(((Task) o).key);
    }

    public String toString() {
        return "[" + this.key.toString() + "] " + this.title;
    }

    public void dump(String prefix, PrintWriter writer) {
        writer.print(prefix);
        writer.print(this.key);
        if (isAffiliatedTask()) {
            writer.print(" ");
            writer.print("affTaskId=" + this.affiliationTaskId);
        }
        if (!this.isDockable) {
            writer.print(" dockable=N");
        }
        if (this.isLaunchTarget) {
            writer.print(" launchTarget=Y");
        }
        if (isFreeformTask()) {
            writer.print(" freeform=Y");
        }
        writer.print(" ");
        writer.print(this.title);
        writer.println();
    }

    public boolean isProtected() {
        return this.isLocked || this.isLaunchTarget;
    }
}
