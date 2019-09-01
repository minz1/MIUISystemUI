package com.android.systemui.recents.model;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.IntProperty;
import android.util.SparseArray;
import android.view.animation.Interpolator;
import com.android.internal.policy.DockedDividerUtils;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.RecentsActivity;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.views.AnimationProps;
import com.android.systemui.recents.views.DropTarget;
import com.android.systemui.recents.views.TaskStackLayoutAlgorithm;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TaskStack {
    private Comparator<Task> FREEFORM_COMPARATOR = new Comparator<Task>() {
        public int compare(Task o1, Task o2) {
            if (o1.isFreeformTask() && !o2.isFreeformTask()) {
                return 1;
            }
            if (!o2.isFreeformTask() || o1.isFreeformTask()) {
                return Long.compare((long) o1.temporarySortIndexInStack, (long) o2.temporarySortIndexInStack);
            }
            return -1;
        }
    };
    ArrayMap<Integer, TaskGrouping> mAffinitiesGroups = new ArrayMap<>();
    TaskStackCallbacks mCb;
    ArrayList<TaskGrouping> mGroups = new ArrayList<>();
    ArrayList<Task> mRawTaskList = new ArrayList<>();
    FilteredTaskList mStackTaskList = new FilteredTaskList();

    public static class DockState implements DropTarget {
        public static final DockState BOTTOM;
        public static final DockState LEFT;
        public static final DockState NONE;
        public static final DockState RIGHT;
        public static final DockState TOP;
        public static final DockState TOP_FORCE_BLACK;
        public final int createMode;
        private final RectF dockArea;
        public final int dockSide;
        private final RectF expandedTouchDockArea;
        private final RectF touchArea;
        public final ViewState viewState;

        public static class ViewState {
            private static final IntProperty<ViewState> HINT_ALPHA = new IntProperty<ViewState>("drawableAlpha") {
                public void setValue(ViewState object, int alpha) {
                    int unused = object.mHintTextAlpha = alpha;
                    object.dockAreaOverlay.invalidateSelf();
                }

                public Integer get(ViewState object) {
                    return Integer.valueOf(object.mHintTextAlpha);
                }
            };
            public final int dockAreaAlpha;
            public final ColorDrawable dockAreaOverlay;
            public final int hintTextAlpha;
            public final int hintTextOrientation;
            private AnimatorSet mDockAreaOverlayAnimator;
            private String mHintText;
            /* access modifiers changed from: private */
            public int mHintTextAlpha;
            private TextPaint mHintTextPaint;
            private final int mHintTextResId;
            private int mStatusBarHeight;
            private Rect mTmpRect;

            private ViewState(int areaAlpha, int hintAlpha, int hintOrientation, int hintTextResId) {
                this.mHintTextAlpha = 0;
                this.mTmpRect = new Rect();
                this.dockAreaAlpha = areaAlpha;
                this.dockAreaOverlay = new ColorDrawable(-1);
                this.dockAreaOverlay.setAlpha(0);
                this.hintTextAlpha = hintAlpha;
                this.hintTextOrientation = hintOrientation;
                this.mHintTextResId = hintTextResId;
                this.mHintTextPaint = new TextPaint(1);
                this.mHintTextPaint.setColor(-1);
                this.mHintTextPaint.setShadowLayer(3.0f, 2.0f, 2.0f, -16777216);
            }

            public void update(Context context) {
                Resources res = context.getResources();
                this.dockAreaOverlay.setColor(res.getColor(R.color.recents_dock_area_overlay));
                this.dockAreaOverlay.setAlpha(0);
                this.mHintTextPaint.setColor(res.getColor(R.color.recents_dock_area_text_color));
                this.mHintTextPaint.setShadowLayer(3.0f, 2.0f, 2.0f, res.getColor(R.color.recents_dock_area_text_shadow_color));
                this.mHintText = context.getString(this.mHintTextResId);
                this.mHintTextPaint.setTextSize((float) res.getDimensionPixelSize(R.dimen.recents_drag_hint_text_size));
                this.mHintTextPaint.getTextBounds(this.mHintText, 0, this.mHintText.length(), this.mTmpRect);
                this.mStatusBarHeight = res.getDimensionPixelSize(R.dimen.status_bar_height);
            }

            public void draw(Canvas canvas) {
                if (this.dockAreaOverlay.getAlpha() > 0) {
                    this.dockAreaOverlay.draw(canvas);
                }
                if (this.mHintTextAlpha > 0) {
                    Rect bounds = this.dockAreaOverlay.getBounds();
                    StaticLayout mSL = StaticLayout.Builder.obtain(this.mHintText, 0, this.mHintText.length(), this.mHintTextPaint, this.hintTextOrientation == 1 ? bounds.height() : bounds.width()).setAlignment(Layout.Alignment.ALIGN_CENTER).build();
                    int x = bounds.left + ((bounds.width() - mSL.getWidth()) / 2);
                    int y = bounds.top + ((bounds.height() - mSL.getHeight()) / 2);
                    this.mHintTextPaint.setAlpha(this.mHintTextAlpha);
                    canvas.save();
                    if (this.hintTextOrientation == 1) {
                        canvas.rotate(-90.0f, (float) bounds.centerX(), (float) bounds.centerY());
                    } else if (RecentsActivity.isForceBlack()) {
                        y += this.mStatusBarHeight / 2;
                    }
                    canvas.translate((float) x, (float) y);
                    mSL.draw(canvas);
                    canvas.restore();
                }
            }

            public void startAnimation(Rect bounds, int areaAlpha, int hintAlpha, int duration, Interpolator interpolator, boolean animateAlpha, boolean animateBounds) {
                Interpolator interpolator2;
                if (this.mDockAreaOverlayAnimator != null) {
                    this.mDockAreaOverlayAnimator.cancel();
                }
                ArrayList<Animator> animators = new ArrayList<>();
                if (this.dockAreaOverlay.getAlpha() != areaAlpha) {
                    if (animateAlpha) {
                        ObjectAnimator anim = ObjectAnimator.ofInt(this.dockAreaOverlay, Utilities.DRAWABLE_ALPHA, new int[]{this.dockAreaOverlay.getAlpha(), areaAlpha});
                        anim.setDuration((long) duration);
                        anim.setInterpolator(interpolator);
                        animators.add(anim);
                    } else {
                        this.dockAreaOverlay.setAlpha(areaAlpha);
                    }
                }
                if (this.mHintTextAlpha != hintAlpha) {
                    if (animateAlpha) {
                        ObjectAnimator anim2 = ObjectAnimator.ofInt(this, HINT_ALPHA, new int[]{this.mHintTextAlpha, hintAlpha});
                        anim2.setDuration(150);
                        if (hintAlpha > this.mHintTextAlpha) {
                            interpolator2 = Interpolators.ALPHA_IN;
                        } else {
                            interpolator2 = Interpolators.ALPHA_OUT;
                        }
                        anim2.setInterpolator(interpolator2);
                        animators.add(anim2);
                    } else {
                        this.mHintTextAlpha = hintAlpha;
                        this.dockAreaOverlay.invalidateSelf();
                    }
                }
                if (bounds != null && !this.dockAreaOverlay.getBounds().equals(bounds)) {
                    if (animateBounds) {
                        PropertyValuesHolder prop = PropertyValuesHolder.ofObject(Utilities.DRAWABLE_RECT, Utilities.RECT_EVALUATOR, new Rect[]{new Rect(this.dockAreaOverlay.getBounds()), bounds});
                        ObjectAnimator anim3 = ObjectAnimator.ofPropertyValuesHolder(this.dockAreaOverlay, new PropertyValuesHolder[]{prop});
                        anim3.setDuration((long) duration);
                        anim3.setInterpolator(interpolator);
                        animators.add(anim3);
                    } else {
                        this.dockAreaOverlay.setBounds(bounds);
                    }
                }
                if (!animators.isEmpty()) {
                    this.mDockAreaOverlayAnimator = new AnimatorSet();
                    this.mDockAreaOverlayAnimator.playTogether(animators);
                    this.mDockAreaOverlayAnimator.start();
                }
            }
        }

        static {
            DockState dockState = new DockState(-1, -1, 80, 255, 0, null, null, null);
            NONE = dockState;
            DockState dockState2 = new DockState(1, 0, 80, 0, 1, new RectF(0.0f, 0.0f, 0.125f, 1.0f), new RectF(0.0f, 0.0f, 0.125f, 1.0f), new RectF(0.0f, 0.0f, 0.5f, 1.0f));
            LEFT = dockState2;
            DockState dockState3 = new DockState(2, 0, 80, 0, 0, new RectF(0.0f, 0.0f, 1.0f, 0.125f), new RectF(0.0f, 0.0f, 1.0f, 0.125f), new RectF(0.0f, 0.0f, 1.0f, 0.5f));
            TOP = dockState3;
            DockState dockState4 = new DockState(2, 0, 80, 0, 0, new RectF(0.0f, 0.0f, 1.0f, 0.16f), new RectF(0.0f, 0.0f, 1.0f, 0.16f), new RectF(0.0f, 0.0f, 1.0f, 0.5f));
            TOP_FORCE_BLACK = dockState4;
            DockState dockState5 = new DockState(3, 1, 80, 0, 1, new RectF(0.875f, 0.0f, 1.0f, 1.0f), new RectF(0.875f, 0.0f, 1.0f, 1.0f), new RectF(0.5f, 0.0f, 1.0f, 1.0f));
            RIGHT = dockState5;
            DockState dockState6 = new DockState(4, 1, 80, 0, 0, new RectF(0.0f, 0.875f, 1.0f, 1.0f), new RectF(0.0f, 0.875f, 1.0f, 1.0f), new RectF(0.0f, 0.5f, 1.0f, 1.0f));
            BOTTOM = dockState6;
        }

        public boolean acceptsDrop(int x, int y, int width, int height, boolean isCurrentTarget) {
            if (isCurrentTarget) {
                return areaContainsPoint(this.expandedTouchDockArea, width, height, (float) x, (float) y);
            }
            return areaContainsPoint(this.touchArea, width, height, (float) x, (float) y);
        }

        DockState(int dockSide2, int createMode2, int dockAreaAlpha, int hintTextAlpha, int hintTextOrientation, RectF touchArea2, RectF dockArea2, RectF expandedTouchDockArea2) {
            this.dockSide = dockSide2;
            this.createMode = createMode2;
            ViewState viewState2 = new ViewState(dockAreaAlpha, hintTextAlpha, hintTextOrientation, R.string.recents_drag_hint_message);
            this.viewState = viewState2;
            this.dockArea = dockArea2;
            this.touchArea = touchArea2;
            this.expandedTouchDockArea = expandedTouchDockArea2;
        }

        public void update(Context context) {
            this.viewState.update(context);
        }

        public boolean areaContainsPoint(RectF area, int width, int height, float x, float y) {
            return x >= ((float) ((int) (area.left * ((float) width)))) && y >= ((float) ((int) (area.top * ((float) height)))) && x <= ((float) ((int) (area.right * ((float) width)))) && y <= ((float) ((int) (area.bottom * ((float) height))));
        }

        public Rect getPreDockedBounds(int width, int height) {
            return new Rect((int) (this.dockArea.left * ((float) width)), (int) (this.dockArea.top * ((float) height)), (int) (this.dockArea.right * ((float) width)), (int) (this.dockArea.bottom * ((float) height)));
        }

        public Rect getDockedBounds(int width, int height, int dividerSize, Rect insets, Resources res) {
            boolean isHorizontalDivision = true;
            if (res.getConfiguration().orientation != 1) {
                isHorizontalDivision = false;
            }
            int position = DockedDividerUtils.calculateMiddlePosition(isHorizontalDivision, insets, width, height, dividerSize);
            Rect newWindowBounds = new Rect();
            DockedDividerUtils.calculateBoundsForPosition(position, this.dockSide, newWindowBounds, width, height, dividerSize);
            return newWindowBounds;
        }

        public Rect getDockedTaskStackBounds(Rect displayRect, int width, int height, int dividerSize, Rect insets, TaskStackLayoutAlgorithm layoutAlgorithm, Resources res, Rect windowRectOut) {
            Rect rect = insets;
            int top = 0;
            boolean isHorizontalDivision = true;
            if (res.getConfiguration().orientation != 1) {
                isHorizontalDivision = false;
            }
            int i = width;
            int i2 = height;
            int i3 = dividerSize;
            DockedDividerUtils.calculateBoundsForPosition(DockedDividerUtils.calculateMiddlePosition(isHorizontalDivision, rect, i, i2, i3), DockedDividerUtils.invertDockSide(this.dockSide), windowRectOut, i, i2, i3);
            Rect taskStackBounds = new Rect();
            if (this.dockArea.bottom >= 1.0f) {
                top = rect.top;
            }
            layoutAlgorithm.getTaskStackBounds(displayRect, windowRectOut, top, rect.left, rect.right, taskStackBounds);
            return taskStackBounds;
        }
    }

    public interface TaskStackCallbacks {
        void onStackTaskAdded(TaskStack taskStack, Task task);

        void onStackTaskRemoved(TaskStack taskStack, Task task, Task task2, AnimationProps animationProps, boolean z);

        void onStackTasksRemoved(TaskStack taskStack);

        void onStackTasksUpdated(TaskStack taskStack);
    }

    public TaskStack() {
        this.mStackTaskList.setFilter(new TaskFilter() {
            public boolean acceptTask(SparseArray<Task> sparseArray, Task t, int index) {
                return t.isStackTask;
            }
        });
    }

    public void setCallbacks(TaskStackCallbacks cb) {
        this.mCb = cb;
    }

    public void moveTaskToStack(Task task, int newStackId) {
        ArrayList<Task> taskList = this.mStackTaskList.getTasks();
        int taskCount = taskList.size();
        if (!task.isFreeformTask() && newStackId == 2) {
            this.mStackTaskList.moveTaskToStack(task, taskCount, newStackId);
        } else if (task.isFreeformTask() && newStackId == 1) {
            int insertIndex = 0;
            int i = taskCount - 1;
            while (true) {
                if (i < 0) {
                    break;
                } else if (!taskList.get(i).isFreeformTask()) {
                    insertIndex = i + 1;
                    break;
                } else {
                    i--;
                }
            }
            this.mStackTaskList.moveTaskToStack(task, insertIndex, newStackId);
        }
    }

    /* access modifiers changed from: package-private */
    public void removeTaskImpl(FilteredTaskList taskList, Task t) {
        taskList.remove(t);
        TaskGrouping group = t.group;
        if (group != null) {
            group.removeTask(t);
            if (group.getTaskCount() == 0) {
                removeGroup(group);
            }
        }
    }

    public void removeTask(Task t, AnimationProps animation, boolean fromDockGesture) {
        if (this.mStackTaskList.contains(t)) {
            removeTaskImpl(this.mStackTaskList, t);
            Task newFrontMostTask = getStackFrontMostTask(false);
            if (this.mCb != null) {
                this.mCb.onStackTaskRemoved(this, t, newFrontMostTask, animation, fromDockGesture);
            }
        }
        this.mRawTaskList.remove(t);
    }

    public void removeAllTasks() {
        ArrayList<Task> tasks = this.mStackTaskList.getTasks();
        for (int i = tasks.size() - 1; i >= 0; i--) {
            Task t = tasks.get(i);
            if (!t.isProtected()) {
                removeTaskImpl(this.mStackTaskList, t);
                this.mRawTaskList.remove(t);
            }
        }
        if (this.mCb != null) {
            this.mCb.onStackTasksRemoved(this);
        }
    }

    public void setTasks(Context context, List<Task> tasks, boolean notifyStackChanges) {
        List<Task> list = tasks;
        ArrayMap<Task.TaskKey, Task> currentTasksMap = createTaskKeyMapFromList(this.mRawTaskList);
        ArrayMap<Task.TaskKey, Task> newTasksMap = createTaskKeyMapFromList(list);
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        ArrayList<Task> allTasks = new ArrayList<>();
        boolean notifyStackChanges2 = this.mCb == null ? false : notifyStackChanges;
        for (int i = this.mRawTaskList.size() - 1; i >= 0; i--) {
            Task task = this.mRawTaskList.get(i);
            if (!newTasksMap.containsKey(task.key) && notifyStackChanges2) {
                arrayList2.add(task);
            }
            task.setGroup(null);
        }
        int taskCount = tasks.size();
        int i2 = 0;
        for (int i3 = 0; i3 < taskCount; i3++) {
            Task newTask = list.get(i3);
            Task currentTask = currentTasksMap.get(newTask.key);
            if (currentTask == null && notifyStackChanges2) {
                arrayList.add(newTask);
            } else if (currentTask != null) {
                currentTask.copyFrom(newTask);
                newTask = currentTask;
            }
            allTasks.add(newTask);
        }
        for (int i4 = allTasks.size() - 1; i4 >= 0; i4--) {
            allTasks.get(i4).temporarySortIndexInStack = i4;
        }
        Collections.sort(allTasks, this.FREEFORM_COMPARATOR);
        this.mStackTaskList.set(allTasks);
        this.mRawTaskList = allTasks;
        createAffiliatedGroupings(context);
        int removedTaskCount = arrayList2.size();
        Task newFrontMostTask = getStackFrontMostTask(false);
        int i5 = 0;
        while (true) {
            int i6 = i5;
            if (i6 >= removedTaskCount) {
                break;
            }
            this.mCb.onStackTaskRemoved(this, (Task) arrayList2.get(i6), newFrontMostTask, AnimationProps.IMMEDIATE, false);
            i5 = i6 + 1;
            removedTaskCount = removedTaskCount;
        }
        int addedTaskCount = arrayList.size();
        while (true) {
            int i7 = i2;
            if (i7 >= addedTaskCount) {
                break;
            }
            this.mCb.onStackTaskAdded(this, (Task) arrayList.get(i7));
            i2 = i7 + 1;
        }
        if (notifyStackChanges2) {
            this.mCb.onStackTasksUpdated(this);
        }
    }

    public Task getStackFrontMostTask(boolean includeFreeformTasks) {
        return getStackFirstTask(includeFreeformTasks);
    }

    public Task getStackFirstTask(boolean includeFreeformTasks) {
        ArrayList<Task> stackTasks = this.mStackTaskList.getTasks();
        if (stackTasks.isEmpty()) {
            return null;
        }
        for (int i = 0; i <= stackTasks.size() - 1; i++) {
            Task task = stackTasks.get(i);
            if (!task.isFreeformTask() || includeFreeformTasks) {
                return task;
            }
        }
        return null;
    }

    public ArrayList<Task.TaskKey> getTaskKeys() {
        ArrayList<Task.TaskKey> taskKeys = new ArrayList<>();
        ArrayList<Task> tasks = computeAllTasksList();
        int taskCount = tasks.size();
        for (int i = 0; i < taskCount; i++) {
            taskKeys.add(tasks.get(i).key);
        }
        return taskKeys;
    }

    public ArrayList<Task> getStackTasks() {
        return this.mStackTaskList.getTasks();
    }

    public ArrayList<Task> getFreeformTasks() {
        ArrayList<Task> freeformTasks = new ArrayList<>();
        ArrayList<Task> tasks = this.mStackTaskList.getTasks();
        int taskCount = tasks.size();
        for (int i = 0; i < taskCount; i++) {
            Task task = tasks.get(i);
            if (task.isFreeformTask()) {
                freeformTasks.add(task);
            }
        }
        return freeformTasks;
    }

    public ArrayList<Task> computeAllTasksList() {
        ArrayList<Task> tasks = new ArrayList<>();
        tasks.addAll(this.mStackTaskList.getTasks());
        return tasks;
    }

    public int getTaskCount() {
        return this.mStackTaskList.size();
    }

    public int getStackTaskCount() {
        ArrayList<Task> tasks = this.mStackTaskList.getTasks();
        int stackCount = 0;
        int taskCount = tasks.size();
        for (int i = 0; i < taskCount; i++) {
            if (!tasks.get(i).isFreeformTask()) {
                stackCount++;
            }
        }
        return stackCount;
    }

    public int getFreeformTaskCount() {
        ArrayList<Task> tasks = this.mStackTaskList.getTasks();
        int freeformCount = 0;
        int taskCount = tasks.size();
        for (int i = 0; i < taskCount; i++) {
            if (tasks.get(i).isFreeformTask()) {
                freeformCount++;
            }
        }
        return freeformCount;
    }

    public Task getLaunchTarget() {
        ArrayList<Task> tasks = this.mStackTaskList.getTasks();
        int taskCount = tasks.size();
        for (int i = 0; i < taskCount; i++) {
            Task task = tasks.get(i);
            if (task.isLaunchTarget) {
                return task;
            }
        }
        return null;
    }

    public int indexOfStackTask(Task t) {
        return this.mStackTaskList.indexOf(t);
    }

    public void addGroup(TaskGrouping group) {
        this.mGroups.add(group);
        this.mAffinitiesGroups.put(Integer.valueOf(group.affiliation), group);
    }

    public void removeGroup(TaskGrouping group) {
        this.mGroups.remove(group);
        this.mAffinitiesGroups.remove(Integer.valueOf(group.affiliation));
    }

    /* access modifiers changed from: package-private */
    public void createAffiliatedGroupings(Context context) {
        this.mGroups.clear();
        this.mAffinitiesGroups.clear();
        ArrayMap<Task.TaskKey, Task> tasksMap = new ArrayMap<>();
        ArrayList<Task> tasks = this.mStackTaskList.getTasks();
        int taskCount = tasks.size();
        for (int i = 0; i < taskCount; i++) {
            Task t = tasks.get(i);
            TaskGrouping group = new TaskGrouping(t.key.id);
            addGroup(group);
            group.addTask(t);
            tasksMap.put(t.key, t);
        }
        float minAlpha = context.getResources().getFloat(R.dimen.recents_task_affiliation_color_min_alpha_percentage);
        int taskGroupCount = this.mGroups.size();
        int i2 = taskCount;
        for (int i3 = 0; i3 < taskGroupCount; i3++) {
            TaskGrouping group2 = this.mGroups.get(i3);
            int taskCount2 = group2.getTaskCount();
            if (taskCount2 > 1) {
                int affiliationColor = tasksMap.get(group2.mTaskKeys.get(0)).affiliationColor;
                float alphaStep = (1.0f - minAlpha) / ((float) taskCount2);
                float alpha = 1.0f;
                for (int j = 0; j < taskCount2; j++) {
                    tasksMap.get(group2.mTaskKeys.get(j)).colorPrimary = Utilities.getColorWithOverlay(affiliationColor, -1, alpha);
                    alpha -= alphaStep;
                }
            }
        }
    }

    public ArraySet<ComponentName> computeComponentsRemoved(String packageName, int userId) {
        SystemServicesProxy ssp = Recents.getSystemServices();
        ArraySet<ComponentName> existingComponents = new ArraySet<>();
        ArraySet<ComponentName> removedComponents = new ArraySet<>();
        ArrayList<Task.TaskKey> taskKeys = getTaskKeys();
        int taskKeyCount = taskKeys.size();
        for (int i = 0; i < taskKeyCount; i++) {
            Task.TaskKey t = taskKeys.get(i);
            if (t.userId == userId) {
                ComponentName cn = t.getComponent();
                if (cn.getPackageName().equals(packageName) && !existingComponents.contains(cn)) {
                    if (ssp.getActivityInfo(cn, userId) != null) {
                        existingComponents.add(cn);
                    } else {
                        removedComponents.add(cn);
                    }
                }
            }
        }
        return removedComponents;
    }

    public String toString() {
        String str = "Stack Tasks (" + this.mStackTaskList.size() + "):\n";
        for (int i = 0; i < this.mStackTaskList.getTasks().size(); i++) {
            str = str + "    " + tasks.get(i).toString() + "\n";
        }
        return str;
    }

    private ArrayMap<Task.TaskKey, Task> createTaskKeyMapFromList(List<Task> tasks) {
        ArrayMap<Task.TaskKey, Task> map = new ArrayMap<>(tasks.size());
        int taskCount = tasks.size();
        for (int i = 0; i < taskCount; i++) {
            Task task = tasks.get(i);
            map.put(task.key, task);
        }
        return map;
    }

    public void dump(String prefix, PrintWriter writer) {
        String innerPrefix = prefix + "  ";
        writer.print(prefix);
        writer.print("TaskStack");
        writer.print(" numStackTasks=");
        writer.print(this.mStackTaskList.size());
        writer.println();
        ArrayList<Task> tasks = this.mStackTaskList.getTasks();
        int taskCount = tasks.size();
        for (int i = 0; i < taskCount; i++) {
            tasks.get(i).dump(innerPrefix, writer);
        }
    }
}
