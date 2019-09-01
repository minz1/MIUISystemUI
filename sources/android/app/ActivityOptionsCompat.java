package android.app;

import android.graphics.Rect;

public class ActivityOptionsCompat {
    public static void setRotationAnimationHint(ActivityOptions options, int hint) {
        options.setRotationAnimationHint(hint);
    }

    public static void setLaunchTaskId(ActivityOptions options, int taskId) {
        options.setLaunchTaskId(taskId);
    }

    public static void setLaunchStackId(ActivityOptions options, int stackId, int windowingMode, int activityType) {
        if (windowingMode != -1) {
            options.setLaunchWindowingMode(windowingMode);
        }
        if (activityType != -1) {
            options.setLaunchActivityType(activityType);
        }
    }

    public static void setOptionsLaunchBounds(ActivityOptions opts, Rect rect) {
        opts.setLaunchBounds(rect);
    }

    public static void setTaskOverlay(ActivityOptions options, boolean taskOverlay, boolean canResume) {
        options.setTaskOverlay(taskOverlay, canResume);
    }
}
