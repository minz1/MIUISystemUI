package com.android.keyguard.charge.rapid;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Choreographer;
import android.view.View;
import android.view.WindowManager;
import com.android.systemui.R;
import java.util.ArrayList;
import java.util.List;

public class FireworksView extends View {
    private static final int OUTER_TRACK_END_COLOR = Color.parseColor("#ff210672");
    private static final int OUTER_TRACK_MIDDLE_COLOR = Color.parseColor("#B42F3A81");
    private static final int OUTER_TRACK_START_COLOR = Color.parseColor("#002F3A81");
    private Drawable mFireDrawable;
    private int mFireHeight;
    /* access modifiers changed from: private */
    public List<PointF> mFireList;
    private Runnable mFireRunnable;
    private int mFireWidth;
    /* access modifiers changed from: private */
    public FireworksManager mFireworksManager;
    private Choreographer.FrameCallback mFrameCallback;
    /* access modifiers changed from: private */
    public long mLastTime;
    private Point mScreenSize;
    private float mSpeedMove;
    private Paint mTrackPaint;
    private int mTrackStokeWidth;
    private int mViewHeight;
    private int mViewWidth;
    private WindowManager mWindowManager;

    public FireworksView(Context context) {
        this(context, null);
    }

    public FireworksView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FireworksView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mFrameCallback = new Choreographer.FrameCallback() {
            public void doFrame(long frameTimeNanos) {
                long elapseTime = (frameTimeNanos - FireworksView.this.mLastTime) / 1000000;
                long unused = FireworksView.this.mLastTime = frameTimeNanos;
                if (FireworksView.this.mFireworksManager != null) {
                    FireworksView.this.mFireworksManager.freshPositions(FireworksView.this.mFireList, elapseTime);
                    FireworksView.this.invalidate();
                }
                Choreographer.getInstance().postFrameCallback(this);
            }
        };
        this.mFireRunnable = new Runnable() {
            public void run() {
                if (FireworksView.this.mFireworksManager != null) {
                    FireworksView.this.mFireworksManager.fire();
                }
                FireworksView.this.postDelayed(this, 120);
            }
        };
        init(context);
    }

    private void init(Context context) {
        this.mWindowManager = (WindowManager) context.getSystemService("window");
        this.mScreenSize = new Point();
        this.mWindowManager.getDefaultDisplay().getRealSize(this.mScreenSize);
        updateSizeForScreenSizeChange();
        this.mTrackPaint = new Paint(1);
        this.mTrackPaint.setStyle(Paint.Style.STROKE);
        this.mTrackPaint.setStrokeWidth((float) this.mTrackStokeWidth);
        LinearGradient linearGradient = new LinearGradient(0.0f, 0.0f, 0.0f, (float) this.mViewHeight, new int[]{OUTER_TRACK_START_COLOR, OUTER_TRACK_MIDDLE_COLOR, OUTER_TRACK_END_COLOR}, new float[]{0.0f, 0.12f, 1.0f}, Shader.TileMode.CLAMP);
        this.mTrackPaint.setShader(linearGradient);
        this.mFireworksManager = new FireworksManager(this.mViewHeight, this.mSpeedMove);
        this.mFireList = new ArrayList();
        this.mFireDrawable = context.getDrawable(R.drawable.charge_animation_fire_light_icon);
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawTrack(canvas);
        drawFireworks(canvas);
    }

    private void drawTrack(Canvas canvas) {
        float step = ((float) this.mViewWidth) / 6.0f;
        for (int index = 1; index <= 5; index++) {
            float startX = ((float) index) * step;
            canvas.drawLine(startX, 0.0f, startX, (float) this.mViewHeight, this.mTrackPaint);
        }
    }

    private void drawFireworks(Canvas canvas) {
        if (this.mFireList != null) {
            float step = ((float) this.mViewWidth) / 6.0f;
            for (PointF pointF : this.mFireList) {
                float startY = pointF.y;
                int left = (int) (((pointF.x + 1.0f) * step) - ((float) (this.mFireWidth / 2)));
                int top = (int) startY;
                this.mFireDrawable.setAlpha(evaluateAlpha(top, this.mViewHeight));
                this.mFireDrawable.setBounds(left, top, this.mFireWidth + left, this.mFireHeight + top);
                this.mFireDrawable.draw(canvas);
            }
        }
    }

    private int evaluateAlpha(int top, int height) {
        float dismissPoint = ((float) height) * 0.3f;
        if (((float) top) > dismissPoint) {
            return 255;
        }
        return (int) (((float) (top * 255)) / dismissPoint);
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(this.mViewWidth, this.mViewHeight);
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        checkScreenSize();
    }

    private void checkScreenSize() {
        Point point = new Point();
        this.mWindowManager.getDefaultDisplay().getRealSize(point);
        if (!this.mScreenSize.equals(point.x, point.y)) {
            this.mScreenSize.set(point.x, point.y);
            updateSizeForScreenSizeChange();
            this.mTrackPaint.setStrokeWidth((float) this.mTrackStokeWidth);
            LinearGradient linearGradient = new LinearGradient(0.0f, 0.0f, 0.0f, (float) this.mViewHeight, new int[]{OUTER_TRACK_START_COLOR, OUTER_TRACK_MIDDLE_COLOR, OUTER_TRACK_END_COLOR}, new float[]{0.0f, 0.12f, 1.0f}, Shader.TileMode.CLAMP);
            this.mTrackPaint.setShader(linearGradient);
            this.mFireworksManager.updateDistanceAndSpeed(this.mViewHeight, this.mSpeedMove);
            requestLayout();
        }
    }

    private void updateSizeForScreenSizeChange() {
        int screenWidth = Math.min(this.mScreenSize.x, this.mScreenSize.y);
        int screenHeight = this.mScreenSize.y;
        float rateWidth = (((float) screenWidth) * 1.0f) / 1080.0f;
        float rateHeight = (((float) screenHeight) * 1.0f) / 2340.0f;
        this.mViewWidth = (int) (122.0f * rateWidth);
        this.mViewHeight = (screenHeight / 2) - ((int) (292.0f * rateWidth));
        this.mFireWidth = (int) (15.0f * rateWidth);
        this.mFireHeight = (int) (345.0f * rateHeight);
        this.mTrackStokeWidth = (int) (4.0f * rateWidth);
        this.mSpeedMove = 1.4633334f * rateHeight;
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        checkScreenSize();
    }

    public void start() {
        Choreographer.getInstance().postFrameCallback(this.mFrameCallback);
        post(this.mFireRunnable);
    }

    public void stop() {
        removeCallbacks(this.mFireRunnable);
        Choreographer.getInstance().removeFrameCallback(this.mFrameCallback);
    }
}
