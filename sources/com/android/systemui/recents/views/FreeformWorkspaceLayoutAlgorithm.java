package com.android.systemui.recents.views;

import android.content.Context;
import android.graphics.RectF;
import android.util.ArrayMap;
import com.android.systemui.R;
import com.android.systemui.recents.model.Task;
import java.util.Collections;
import java.util.List;

public class FreeformWorkspaceLayoutAlgorithm {
    private int mTaskPadding;
    private ArrayMap<Task.TaskKey, RectF> mTaskRectMap = new ArrayMap<>();

    public FreeformWorkspaceLayoutAlgorithm(Context context) {
        reloadOnConfigurationChange(context);
    }

    public void reloadOnConfigurationChange(Context context) {
        this.mTaskPadding = context.getResources().getDimensionPixelSize(R.dimen.recents_freeform_layout_task_padding) / 2;
    }

    public void update(List<Task> freeformTasks, TaskStackLayoutAlgorithm stackLayout) {
        float rowTaskWidth;
        List<Task> list = freeformTasks;
        TaskStackLayoutAlgorithm taskStackLayoutAlgorithm = stackLayout;
        Collections.reverse(freeformTasks);
        this.mTaskRectMap.clear();
        int numFreeformTasks = taskStackLayoutAlgorithm.mNumFreeformTasks;
        if (!freeformTasks.isEmpty()) {
            int workspaceWidth = taskStackLayoutAlgorithm.mFreeformRect.width();
            int i = taskStackLayoutAlgorithm.mFreeformRect.height();
            float normalizedWorkspaceWidth = ((float) workspaceWidth) / ((float) i);
            float[] normalizedTaskWidths = new float[numFreeformTasks];
            for (int i2 = 0; i2 < numFreeformTasks; i2++) {
                Task task = list.get(i2);
                if (task.bounds != null) {
                    rowTaskWidth = ((float) task.bounds.width()) / ((float) task.bounds.height());
                } else {
                    rowTaskWidth = normalizedWorkspaceWidth;
                }
                normalizedTaskWidths[i2] = Math.min(rowTaskWidth, normalizedWorkspaceWidth);
            }
            int rowCount = 1;
            float maxRowWidth = 0.0f;
            float rowWidth = 0.0f;
            float rowWidth2 = 0.85f;
            int i3 = 0;
            while (i3 < numFreeformTasks) {
                float width = normalizedTaskWidths[i3] * rowWidth2;
                if (rowWidth + width <= normalizedWorkspaceWidth) {
                    rowWidth += width;
                    i3++;
                } else if (((float) (rowCount + 1)) * rowWidth2 > 1.0f) {
                    i3 = 0;
                    rowCount = 1;
                    rowWidth = 0.0f;
                    rowWidth2 = Math.min(normalizedWorkspaceWidth / (rowWidth + width), 1.0f / ((float) (rowCount + 1)));
                } else {
                    rowCount++;
                    i3++;
                    rowWidth = width;
                }
                maxRowWidth = Math.max(rowWidth, maxRowWidth);
                TaskStackLayoutAlgorithm taskStackLayoutAlgorithm2 = stackLayout;
            }
            float defaultRowLeft = ((1.0f - (maxRowWidth / normalizedWorkspaceWidth)) * ((float) workspaceWidth)) / 2.0f;
            float rowLeft = defaultRowLeft;
            float rowTop = ((1.0f - (((float) rowCount) * rowWidth2)) * ((float) i)) / 2.0f;
            float rowHeight = ((float) i) * rowWidth2;
            int i4 = 0;
            while (true) {
                int workspaceHeight = i;
                int workspaceHeight2 = i4;
                if (workspaceHeight2 >= numFreeformTasks) {
                    break;
                }
                Task task2 = list.get(workspaceHeight2);
                float width2 = normalizedTaskWidths[workspaceHeight2] * rowHeight;
                int numFreeformTasks2 = numFreeformTasks;
                if (rowLeft + width2 > ((float) workspaceWidth)) {
                    rowTop += rowHeight;
                    rowLeft = defaultRowLeft;
                }
                float defaultRowLeft2 = defaultRowLeft;
                RectF rect = new RectF(rowLeft, rowTop, rowLeft + width2, rowTop + rowHeight);
                rect.inset((float) this.mTaskPadding, (float) this.mTaskPadding);
                rowLeft += width2;
                this.mTaskRectMap.put(task2.key, rect);
                i4 = workspaceHeight2 + 1;
                i = workspaceHeight;
                numFreeformTasks = numFreeformTasks2;
                defaultRowLeft = defaultRowLeft2;
                workspaceWidth = workspaceWidth;
                list = freeformTasks;
            }
        }
    }

    public boolean isTransformAvailable(Task task, TaskStackLayoutAlgorithm stackLayout) {
        if (stackLayout.mNumFreeformTasks == 0 || task == null) {
            return false;
        }
        return this.mTaskRectMap.containsKey(task.key);
    }

    public TaskViewTransform getTransform(Task task, TaskViewTransform transformOut, TaskStackLayoutAlgorithm stackLayout) {
        if (!this.mTaskRectMap.containsKey(task.key)) {
            return null;
        }
        transformOut.scale = 1.0f;
        transformOut.alpha = 1.0f;
        transformOut.translationZ = (float) stackLayout.mMaxTranslationZ;
        transformOut.dimAlpha = 0.0f;
        transformOut.viewOutlineAlpha = 2.0f;
        transformOut.rect.set(this.mTaskRectMap.get(task.key));
        transformOut.rect.offset((float) stackLayout.mFreeformRect.left, (float) stackLayout.mFreeformRect.top);
        transformOut.visible = true;
        return transformOut;
    }
}
