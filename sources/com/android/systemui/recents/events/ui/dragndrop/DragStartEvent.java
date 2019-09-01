package com.android.systemui.recents.events.ui.dragndrop;

import android.graphics.Point;
import com.android.systemui.recents.events.RecentsEventBus;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.views.TaskView;

public class DragStartEvent extends RecentsEventBus.Event {
    public final Task task;
    public final TaskView taskView;
    public final Point tlOffset;

    public DragStartEvent(Task task2, TaskView taskView2, Point tlOffset2) {
        this.task = task2;
        this.taskView = taskView2;
        this.tlOffset = tlOffset2;
    }
}
