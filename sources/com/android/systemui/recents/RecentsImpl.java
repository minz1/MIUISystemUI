package com.android.systemui.recents;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.GraphicBuffer;
import android.graphics.Rect;
import android.graphics.RectF;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.views.RecentsTransitionHelper;
import com.android.systemui.recents.views.TaskStackView;
import com.android.systemui.recents.views.TaskViewTransform;

public class RecentsImpl extends BaseRecentsImpl {
    public RecentsImpl(Context context) {
        super(context);
    }

    /* access modifiers changed from: package-private */
    public ActivityOptions getThumbnailTransitionActivityOptions(ActivityManager.RunningTaskInfo runningTask, TaskStackView stackView, Rect windowOverrideRect) {
        Task toTask = new Task();
        TaskViewTransform toTransform = getThumbnailTransitionTransform(stackView, toTask, windowOverrideRect);
        GraphicBuffer thumbnail = drawThumbnailTransitionBitmap(toTask, toTransform);
        RectF toTaskRect = toTransform.rect;
        toTaskRect.top += (float) mTaskBarHeight;
        return ActivityOptions.makeThumbnailAspectScaleDownAnimation(this.mDummyStackView, Bitmap.createHardwareBitmap(thumbnail), (int) toTaskRect.left, (int) toTaskRect.top, (int) toTaskRect.width(), (int) toTaskRect.height(), this.mHandler, null);
    }

    private GraphicBuffer drawThumbnailTransitionBitmap(Task toTask, TaskViewTransform toTransform) {
        GraphicBuffer drawViewIntoGraphicBuffer;
        SystemServicesProxy ssp = Recents.getSystemServices();
        if (toTransform == null || toTask.key == null) {
            return null;
        }
        synchronized (this.mHeaderBarLock) {
            if (toTask.isSystemApp || !ssp.isInSafeMode()) {
            }
            int width = (int) toTransform.rect.width();
            this.mHeaderBar.onTaskViewSizeChanged(width, (int) toTransform.rect.height());
            drawViewIntoGraphicBuffer = RecentsTransitionHelper.drawViewIntoGraphicBuffer(width, mTaskBarHeight, null, 1.0f, 0);
        }
        return drawViewIntoGraphicBuffer;
    }
}
