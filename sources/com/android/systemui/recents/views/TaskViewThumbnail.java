package com.android.systemui.recents.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Shader;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewDebug;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.proxy.ActivityManager;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.RecentsImpl;
import com.android.systemui.recents.events.RecentsEventBus;
import com.android.systemui.recents.events.activity.TaskSnapshotChangedEvent;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.recents.model.Task;
import com.android.systemui.statusbar.policy.ConfigurationController;

public class TaskViewThumbnail extends View {
    private static final ColorMatrix TMP_BRIGHTNESS_COLOR_MATRIX = new ColorMatrix();
    private static final ColorMatrix TMP_FILTER_COLOR_MATRIX = new ColorMatrix();
    private Paint mBgFillPaint;
    private BitmapShader mBitmapShader;
    private int mCornerRadius;
    @ViewDebug.ExportedProperty(category = "recents")
    private float mDimAlpha;
    @ViewDebug.ExportedProperty(category = "recents")
    private boolean mDisabledInSafeMode;
    private int mDisplayOrientation;
    private Rect mDisplayRect;
    private Paint mDrawPaint;
    private Paint mEdgePaint;
    private float mFullscreenThumbnailScale;
    @ViewDebug.ExportedProperty(category = "recents")
    private boolean mInvisible;
    private LightingColorFilter mLightingColorFilter;
    @ViewDebug.ExportedProperty(category = "recents")
    private float mRotateDegrees;
    private Matrix mScaleMatrix;
    private Task mTask;
    private View mTaskBar;
    @ViewDebug.ExportedProperty(category = "recents")
    private Rect mTaskViewRect;
    private ActivityManager.TaskThumbnailInfo mThumbnailInfo;
    @ViewDebug.ExportedProperty(category = "recents")
    private Rect mThumbnailRect;
    @ViewDebug.ExportedProperty(category = "recents")
    private float mThumbnailScale;

    public TaskViewThumbnail(Context context) {
        this(context, null);
    }

    public TaskViewThumbnail(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TaskViewThumbnail(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public TaskViewThumbnail(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mDisplayOrientation = 0;
        this.mDisplayRect = new Rect();
        this.mTaskViewRect = new Rect();
        this.mThumbnailRect = new Rect();
        this.mScaleMatrix = new Matrix();
        this.mDrawPaint = new Paint();
        this.mBgFillPaint = new Paint();
        this.mLightingColorFilter = new LightingColorFilter(-1, 0);
        this.mEdgePaint = new Paint();
        this.mDrawPaint.setColorFilter(this.mLightingColorFilter);
        this.mDrawPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        if (Build.VERSION.SDK_INT >= 28) {
            this.mDrawPaint.setFilterBitmap(true);
            this.mDrawPaint.setAntiAlias(true);
        }
        this.mCornerRadius = getResources().getDimensionPixelSize(R.dimen.recents_task_view_rounded_corners_radius);
        this.mBgFillPaint.setColor(getResources().getColor(R.color.recent_task_bg_color));
        this.mBgFillPaint.setAntiAlias(true);
        this.mFullscreenThumbnailScale = 0.6f;
        this.mEdgePaint.setColor(getResources().getColor(R.color.recent_task_edge_color));
        this.mEdgePaint.setAntiAlias(true);
        this.mEdgePaint.setStyle(Paint.Style.STROKE);
        this.mEdgePaint.setStrokeWidth(2.0f);
    }

    public void onTaskViewSizeChanged(int width, int height) {
        if (this.mTaskViewRect.width() != width || this.mTaskViewRect.bottom != height) {
            this.mDisplayOrientation = Utilities.getAppConfiguration(getContext()).orientation;
            this.mDisplayRect = Recents.getSystemServices().getDisplayRect();
            this.mTaskViewRect.set(0, RecentsImpl.mTaskBarHeight, width, height);
            setLeftTopRightBottom(0, 0, width, height);
            updateThumbnailScale();
        }
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        if (!this.mInvisible) {
            canvas.saveLayer(0.0f, 0.0f, (float) getWidth(), (float) getHeight(), null);
            Canvas canvas2 = canvas;
            canvas2.translate(0.0f, (float) RecentsImpl.mTaskBarHeight);
            int viewWidth = this.mTaskViewRect.width();
            int viewHeight = this.mTaskViewRect.height();
            int thumbnailWidth = Math.min(viewWidth, Math.round(((float) (this.mRotateDegrees == 0.0f ? this.mThumbnailRect.width() : this.mThumbnailRect.height())) * this.mThumbnailScale));
            int thumbnailHeight = Math.min(viewHeight, Math.round(((float) (this.mRotateDegrees == 0.0f ? this.mThumbnailRect.height() : this.mThumbnailRect.width())) * this.mThumbnailScale));
            if (this.mBitmapShader == null || thumbnailWidth <= 0 || thumbnailHeight <= 0) {
                canvas2.drawRoundRect(0.0f, 0.0f, (float) viewWidth, (float) viewHeight, (float) this.mCornerRadius, (float) this.mCornerRadius, this.mBgFillPaint);
            } else {
                int width = Math.abs(viewWidth - thumbnailWidth) > 2 ? viewWidth : thumbnailWidth;
                int height = Math.abs(viewHeight - thumbnailHeight) > 2 ? viewHeight : thumbnailHeight;
                Canvas canvas3 = canvas2;
                canvas3.drawRoundRect(0.0f, 0.0f, (float) width, (float) height, (float) this.mCornerRadius, (float) this.mCornerRadius, this.mBgFillPaint);
                canvas3.drawRect(0.0f, 0.0f, (float) thumbnailWidth, (float) thumbnailHeight, this.mDrawPaint);
                if (((ConfigurationController) Dependency.get(ConfigurationController.class)).isNightMode()) {
                    canvas2.drawRoundRect(1.0f, 1.0f, (float) (width - 1), (float) (height - 1), (float) this.mCornerRadius, (float) this.mCornerRadius, this.mEdgePaint);
                }
            }
            canvas.restore();
        }
    }

    /* access modifiers changed from: package-private */
    public void setThumbnail(Bitmap bm, ActivityManager.TaskThumbnailInfo thumbnailInfo) {
        if (bm != null) {
            this.mBitmapShader = new BitmapShader(bm, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            this.mDrawPaint.setShader(this.mBitmapShader);
            this.mThumbnailInfo = thumbnailInfo;
            if (this.mThumbnailInfo != null) {
                this.mFullscreenThumbnailScale = this.mThumbnailInfo.scale;
            }
            if (this.mThumbnailInfo == null || this.mThumbnailInfo.insets == null) {
                this.mThumbnailRect.set(0, 0, bm.getWidth(), bm.getHeight());
            } else {
                this.mThumbnailRect.set(0, 0, (int) (((float) bm.getWidth()) - (((float) (this.mThumbnailInfo.insets.left + this.mThumbnailInfo.insets.right)) * this.mFullscreenThumbnailScale)), (int) (((float) bm.getHeight()) - (((float) (this.mThumbnailInfo.insets.top + this.mThumbnailInfo.insets.bottom)) * this.mFullscreenThumbnailScale)));
            }
            updateThumbnailScale();
            return;
        }
        this.mBitmapShader = null;
        this.mDrawPaint.setShader(null);
        this.mThumbnailRect.setEmpty();
        this.mThumbnailInfo = null;
    }

    /* access modifiers changed from: package-private */
    public void updateThumbnailPaintFilter() {
        if (!this.mInvisible) {
            int mul = (int) ((1.0f - this.mDimAlpha) * 255.0f);
            if (this.mBitmapShader == null) {
                int grey = mul;
                this.mDrawPaint.setColorFilter(null);
                this.mDrawPaint.setColor(Color.argb(255, grey, grey, grey));
            } else if (this.mDisabledInSafeMode) {
                TMP_FILTER_COLOR_MATRIX.setSaturation(0.0f);
                float scale = 1.0f - this.mDimAlpha;
                float[] mat = TMP_BRIGHTNESS_COLOR_MATRIX.getArray();
                mat[0] = scale;
                mat[6] = scale;
                mat[12] = scale;
                mat[4] = this.mDimAlpha * 255.0f;
                mat[9] = this.mDimAlpha * 255.0f;
                mat[14] = this.mDimAlpha * 255.0f;
                TMP_FILTER_COLOR_MATRIX.preConcat(TMP_BRIGHTNESS_COLOR_MATRIX);
                ColorMatrixColorFilter filter = new ColorMatrixColorFilter(TMP_FILTER_COLOR_MATRIX);
                this.mDrawPaint.setColorFilter(filter);
                this.mBgFillPaint.setColorFilter(filter);
            } else {
                this.mLightingColorFilter.setColorMultiply(Color.argb(255, mul, mul, mul));
                this.mDrawPaint.setColorFilter(this.mLightingColorFilter);
                this.mDrawPaint.setColor(-1);
                this.mBgFillPaint.setColorFilter(this.mLightingColorFilter);
            }
            if (this.mInvisible == 0) {
                invalidate();
            }
        }
    }

    public void updateThumbnailScale() {
        float pivotX;
        float pivotY;
        this.mRotateDegrees = 0.0f;
        float pivotY2 = 0.0f;
        float pivotY3 = 0.0f;
        this.mThumbnailScale = 1.0f;
        if (this.mBitmapShader != null) {
            boolean isStackTask = !this.mTask.isFreeformTask() || this.mTask.bounds == null;
            if (this.mTaskViewRect.isEmpty() || this.mThumbnailInfo == null || this.mThumbnailInfo.taskWidth == 0 || this.mThumbnailInfo.taskHeight == 0) {
                this.mThumbnailScale = 0.0f;
            } else if (isStackTask) {
                if (this.mDisplayOrientation == 1) {
                    if (this.mThumbnailInfo.screenOrientation == 1) {
                        this.mThumbnailScale = ((float) this.mTaskViewRect.width()) / ((float) this.mThumbnailRect.width());
                    } else {
                        this.mThumbnailScale = Math.max((((float) this.mTaskViewRect.width()) * 1.0f) / ((float) this.mThumbnailRect.height()), (((float) this.mTaskViewRect.height()) * 1.0f) / ((float) this.mThumbnailRect.width()));
                        this.mRotateDegrees = 90.0f;
                        pivotX = (float) (this.mTaskViewRect.width() / 2);
                        pivotY = (float) (this.mTaskViewRect.width() / 2);
                    }
                } else if (this.mThumbnailInfo.screenOrientation == 2) {
                    this.mThumbnailScale = Math.max((((float) this.mTaskViewRect.width()) * 1.0f) / ((float) this.mThumbnailRect.width()), (((float) this.mTaskViewRect.height()) * 1.0f) / ((float) this.mThumbnailRect.height()));
                } else {
                    this.mThumbnailScale = Math.max((((float) this.mTaskViewRect.width()) * 1.0f) / ((float) this.mThumbnailRect.height()), (((float) this.mTaskViewRect.height()) * 1.0f) / ((float) this.mThumbnailRect.width()));
                    if (Recents.getSystemServices().getDisplayRotation() == 3) {
                        this.mRotateDegrees = 90.0f;
                        pivotX = (float) (this.mTaskViewRect.width() / 2);
                        pivotY = (float) (this.mTaskViewRect.width() / 2);
                    } else {
                        this.mRotateDegrees = -90.0f;
                        pivotX = (((float) this.mThumbnailRect.width()) * this.mThumbnailScale) / 2.0f;
                        pivotY = (((float) this.mThumbnailRect.width()) * this.mThumbnailScale) / 2.0f;
                    }
                }
                pivotY3 = pivotY;
                pivotY2 = pivotX;
            } else {
                this.mThumbnailScale = Math.min(((float) this.mTaskViewRect.width()) / ((float) this.mThumbnailRect.width()), ((float) this.mTaskViewRect.height()) / ((float) this.mThumbnailRect.height()));
            }
            if (!(this.mThumbnailInfo == null || this.mThumbnailInfo.insets == null)) {
                this.mScaleMatrix.setTranslate(((float) (-this.mThumbnailInfo.insets.left)) * this.mFullscreenThumbnailScale, ((float) (-this.mThumbnailInfo.insets.top)) * this.mFullscreenThumbnailScale);
            }
            this.mScaleMatrix.postScale(this.mThumbnailScale, this.mThumbnailScale);
            this.mScaleMatrix.postRotate(this.mRotateDegrees, pivotY2, pivotY3);
            this.mBitmapShader.setLocalMatrix(this.mScaleMatrix);
        }
        if (!this.mInvisible) {
            invalidate();
        }
    }

    /* access modifiers changed from: package-private */
    public void updateClipToTaskBar(View taskBar) {
        this.mTaskBar = taskBar;
        invalidate();
    }

    public void setDimAlpha(float dimAlpha) {
        this.mDimAlpha = dimAlpha;
        updateThumbnailPaintFilter();
    }

    /* access modifiers changed from: package-private */
    public void bindToTask(Task t, boolean disabledInSafeMode, int displayOrientation, Rect displayRect) {
        this.mTask = t;
        this.mDisabledInSafeMode = disabledInSafeMode;
        this.mDisplayOrientation = displayOrientation;
        this.mDisplayRect.set(displayRect);
        RecentsEventBus.getDefault().register(this);
    }

    /* access modifiers changed from: package-private */
    public void onTaskDataLoaded(ActivityManager.TaskThumbnailInfo thumbnailInfo) {
        if (this.mTask.thumbnail != null) {
            setThumbnail(this.mTask.thumbnail, thumbnailInfo);
        } else {
            setThumbnail(null, null);
        }
    }

    /* access modifiers changed from: package-private */
    public void unbindFromTask() {
        this.mTask = null;
        setThumbnail(null, null);
        RecentsEventBus.getDefault().unregister(this);
    }

    public final void onBusEvent(TaskSnapshotChangedEvent event) {
        if (this.mTask != null && event.taskId == this.mTask.key.id && event.snapshot != null && event.taskThumbnailInfo != null) {
            setThumbnail(event.snapshot, event.taskThumbnailInfo);
        }
    }

    public void reset() {
        setAlpha(1.0f);
    }
}
