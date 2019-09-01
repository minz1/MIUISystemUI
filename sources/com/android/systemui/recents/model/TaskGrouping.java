package com.android.systemui.recents.model;

import android.util.ArrayMap;
import com.android.systemui.recents.model.Task;
import java.util.ArrayList;

public class TaskGrouping {
    int affiliation;
    long latestActiveTimeInGroup;
    Task.TaskKey mFrontMostTaskKey;
    ArrayMap<Task.TaskKey, Integer> mTaskKeyIndices = new ArrayMap<>();
    ArrayList<Task.TaskKey> mTaskKeys = new ArrayList<>();

    public TaskGrouping(int affiliation2) {
        this.affiliation = affiliation2;
    }

    /* access modifiers changed from: package-private */
    public void addTask(Task t) {
        this.mTaskKeys.add(t.key);
        if (t.key.lastActiveTime > this.latestActiveTimeInGroup) {
            this.latestActiveTimeInGroup = t.key.lastActiveTime;
        }
        t.setGroup(this);
        updateTaskIndices();
    }

    /* access modifiers changed from: package-private */
    public void removeTask(Task t) {
        this.mTaskKeys.remove(t.key);
        this.latestActiveTimeInGroup = 0;
        int taskCount = this.mTaskKeys.size();
        for (int i = 0; i < taskCount; i++) {
            long lastActiveTime = this.mTaskKeys.get(i).lastActiveTime;
            if (lastActiveTime > this.latestActiveTimeInGroup) {
                this.latestActiveTimeInGroup = lastActiveTime;
            }
        }
        t.setGroup(null);
        updateTaskIndices();
    }

    public boolean isFrontMostTask(Task t) {
        return t.key == this.mFrontMostTaskKey;
    }

    public boolean isTaskAboveTask(Task t, Task below) {
        return this.mTaskKeyIndices.containsKey(t.key) && this.mTaskKeyIndices.containsKey(below.key) && this.mTaskKeyIndices.get(t.key).intValue() > this.mTaskKeyIndices.get(below.key).intValue();
    }

    public int getTaskCount() {
        return this.mTaskKeys.size();
    }

    private void updateTaskIndices() {
        if (this.mTaskKeys.isEmpty()) {
            this.mFrontMostTaskKey = null;
            this.mTaskKeyIndices.clear();
            return;
        }
        int taskCount = this.mTaskKeys.size();
        this.mFrontMostTaskKey = this.mTaskKeys.get(this.mTaskKeys.size() - 1);
        this.mTaskKeyIndices.clear();
        for (int i = 0; i < taskCount; i++) {
            this.mTaskKeyIndices.put(this.mTaskKeys.get(i), Integer.valueOf(i));
        }
    }
}
