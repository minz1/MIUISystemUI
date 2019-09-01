package com.android.keyguard.charge.rapid;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import com.android.systemui.Constants;
import com.android.systemui.plugins.R;

public class OutlineView extends View {
    private static final int OUTER_CIRCLE_END_COLOR = Color.parseColor("#3216a5");
    private static final int OUTER_CIRCLE_MIDDLE_COLOR = Color.parseColor("#0e5dff");
    private static final int OUTER_CIRCLE_START_COLOR = Color.parseColor("#d013ff");
    private float mArcAngleDegree;
    private float mArcCircleCenterY;
    private float mArcCircleRadius;
    private float mArcLeftCircleCenterX;
    private float mArcRightCircleCenterX;
    private Paint mOutCirclePaint;
    private Paint mOutSecCirclePaint;
    private Paint mOutThrCirclePaint;
    private int mOuterCircleCenterX;
    private int mOuterCircleCenterY;
    private int mOuterCircleRadius;
    private int mOuterCircleWidth;
    private int mOuterSecCircleRadius;
    private int mOuterSecCircleWidth;
    private int mOuterThrCircleRadius;
    private int mOuterThrCircleWidth;
    private Point mScreenSize;
    private float mSecArcAngleDegree;
    private float mSecArcCircleCenterY;
    private float mSecArcCircleRadius;
    private float mSecArcLeftCircleCenterX;
    private float mSecArcRightCircleCenterX;
    private int mSecTrackTopY;
    private float mThrArcAngleDegree;
    private float mThrArcCircleCenterY;
    private float mThrArcCircleRadius;
    private float mThrArcLeftCircleCenterX;
    private float mThrArcRightCircleCenterX;
    private int mThrTrackTopY;
    private int mTrackLeftX;
    private int mTrackRightX;
    private int mTrackTopY;
    private int mViewHeight;
    private int mViewWidth;
    private WindowManager mWindowManager;

    private void updateSizeForScreenSizeChange() {
        int screenWidth = Math.min(this.mScreenSize.x, this.mScreenSize.y);
        int screenHeight = this.mScreenSize.y;
        float rateWidth = (((float) screenWidth) * 1.0f) / 1080.0f;
        this.mOuterCircleWidth = (int) (6.0f * rateWidth);
        this.mOuterSecCircleWidth = (int) (4.0f * rateWidth);
        this.mOuterThrCircleWidth = (int) (4.0f * rateWidth);
        this.mOuterCircleRadius = (int) (378.0f * rateWidth);
        this.mOuterSecCircleRadius = (int) (358.0f * rateWidth);
        this.mOuterThrCircleRadius = (int) (338.0f * rateWidth);
        this.mOuterCircleCenterX = (this.mOuterCircleWidth / 2) + this.mOuterCircleRadius;
        this.mOuterCircleCenterY = this.mOuterCircleCenterX;
        this.mViewWidth = 2 * (this.mOuterCircleRadius + this.mOuterCircleWidth);
        this.mViewHeight = this.mOuterCircleCenterY + (screenHeight / 2);
        int anchorPointTrackTopY = (int) (475.0f * rateWidth);
        int anchorPointSecTrackTopY = (int) (455.0f * rateWidth);
        int anchorPointThrTrackTopY = (int) (435.0f * rateWidth);
        int trackWidth = (int) (122.0f * rateWidth);
        this.mTrackLeftX = this.mOuterCircleCenterX - (trackWidth / 2);
        this.mTrackRightX = this.mOuterCircleCenterX + (trackWidth / 2);
        this.mTrackTopY = this.mOuterCircleCenterY + anchorPointTrackTopY;
        this.mSecTrackTopY = this.mOuterCircleCenterY + anchorPointSecTrackTopY;
        this.mThrTrackTopY = this.mOuterCircleCenterY + anchorPointThrTrackTopY;
        float tempA = (float) anchorPointTrackTopY;
        float tempD = ((float) trackWidth) / 2.0f;
        float tempR = (float) this.mOuterCircleRadius;
        this.mArcCircleRadius = (((tempA * tempA) + (tempD * tempD)) - (tempR * tempR)) / ((tempR - tempD) * 2.0f);
        this.mArcAngleDegree = (float) (((double) (((float) Math.atan((double) ((this.mArcCircleRadius + tempD) / tempA))) * 180.0f)) / 3.141592653589793d);
        this.mArcLeftCircleCenterX = ((float) this.mTrackLeftX) - this.mArcCircleRadius;
        this.mArcRightCircleCenterX = ((float) this.mTrackRightX) + this.mArcCircleRadius;
        this.mArcCircleCenterY = (float) this.mTrackTopY;
        float tempA2 = (float) anchorPointSecTrackTopY;
        float tempR2 = (float) this.mOuterSecCircleRadius;
        this.mSecArcCircleRadius = (((tempA2 * tempA2) + (tempD * tempD)) - (tempR2 * tempR2)) / ((tempR2 - tempD) * 2.0f);
        float f = tempR2;
        this.mSecArcAngleDegree = (float) (((double) (((float) Math.atan((double) ((this.mSecArcCircleRadius + tempD) / tempA2))) * 180.0f)) / 3.141592653589793d);
        this.mSecArcLeftCircleCenterX = ((float) this.mTrackLeftX) - this.mSecArcCircleRadius;
        this.mSecArcRightCircleCenterX = ((float) this.mTrackRightX) + this.mSecArcCircleRadius;
        this.mSecArcCircleCenterY = (float) this.mSecTrackTopY;
        float tempA3 = (float) anchorPointThrTrackTopY;
        float tempR3 = (float) this.mOuterThrCircleRadius;
        this.mThrArcCircleRadius = (((tempA3 * tempA3) + (tempD * tempD)) - (tempR3 * tempR3)) / ((tempR3 - tempD) * 2.0f);
        float f2 = rateWidth;
        int i = anchorPointTrackTopY;
        this.mThrArcAngleDegree = (float) (((double) (180.0f * ((float) Math.atan((double) ((this.mThrArcCircleRadius + tempD) / tempA3))))) / 3.141592653589793d);
        this.mThrArcLeftCircleCenterX = ((float) this.mTrackLeftX) - this.mThrArcCircleRadius;
        this.mThrArcRightCircleCenterX = ((float) this.mTrackRightX) + this.mThrArcCircleRadius;
        this.mThrArcCircleCenterY = (float) this.mThrTrackTopY;
        Log.i("OutlineView", "updateSizeForScreenSizeChange:  screenWidth: " + screenWidth + " screenHeight: " + screenHeight + " IS_NOTCH " + Constants.IS_NOTCH);
    }

    public OutlineView(Context context) {
        this(context, null);
    }

    public OutlineView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OutlineView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        this.mWindowManager = (WindowManager) context.getSystemService("window");
        this.mScreenSize = new Point();
        this.mWindowManager.getDefaultDisplay().getRealSize(this.mScreenSize);
        updateSizeForScreenSizeChange();
        this.mOutCirclePaint = new Paint(1);
        this.mOutCirclePaint.setStyle(Paint.Style.STROKE);
        this.mOutCirclePaint.setStrokeWidth((float) this.mOuterCircleWidth);
        LinearGradient linearGradient = new LinearGradient(0.0f, 0.0f, 0.0f, (float) this.mViewHeight, new int[]{OUTER_CIRCLE_START_COLOR, OUTER_CIRCLE_MIDDLE_COLOR, OUTER_CIRCLE_END_COLOR}, new float[]{0.0f, 0.34f, 1.0f}, Shader.TileMode.CLAMP);
        this.mOutCirclePaint.setShader(linearGradient);
        this.mOutSecCirclePaint = new Paint(1);
        this.mOutSecCirclePaint.setStyle(Paint.Style.STROKE);
        this.mOutSecCirclePaint.setStrokeWidth((float) this.mOuterSecCircleWidth);
        this.mOutSecCirclePaint.setAlpha(178);
        this.mOutSecCirclePaint.setShader(linearGradient);
        this.mOutThrCirclePaint = new Paint(1);
        this.mOutThrCirclePaint.setStyle(Paint.Style.STROKE);
        this.mOutThrCirclePaint.setStrokeWidth((float) this.mOuterThrCircleWidth);
        this.mOutThrCirclePaint.setAlpha(R.styleable.AppCompatTheme_textAppearanceSearchResultTitle);
        this.mOutThrCirclePaint.setShader(linearGradient);
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Canvas canvas2 = canvas;
        canvas2.drawArc((float) (this.mOuterCircleCenterX - this.mOuterCircleRadius), (float) (this.mOuterCircleCenterY - this.mOuterCircleRadius), (float) (this.mOuterCircleCenterX + this.mOuterCircleRadius), (float) (this.mOuterCircleCenterY + this.mOuterCircleRadius), this.mArcAngleDegree - 270.0f, 360.0f - (this.mArcAngleDegree * 2.0f), false, this.mOutCirclePaint);
        int left = this.mOuterCircleCenterX - this.mOuterSecCircleRadius;
        int top = this.mOuterCircleCenterY - this.mOuterSecCircleRadius;
        int right = this.mOuterCircleCenterX + this.mOuterSecCircleRadius;
        Canvas canvas3 = canvas;
        canvas3.drawArc((float) left, (float) top, (float) right, (float) (this.mOuterCircleCenterY + this.mOuterSecCircleRadius), this.mSecArcAngleDegree - 270.0f, 360.0f - (this.mSecArcAngleDegree * 2.0f), false, this.mOutSecCirclePaint);
        int left2 = this.mOuterCircleCenterX - this.mOuterThrCircleRadius;
        int top2 = this.mOuterCircleCenterY - this.mOuterThrCircleRadius;
        int right2 = this.mOuterCircleCenterX + this.mOuterThrCircleRadius;
        canvas3.drawArc((float) left2, (float) top2, (float) right2, (float) (this.mOuterCircleCenterY + this.mOuterThrCircleRadius), this.mThrArcAngleDegree - 270.0f, 360.0f - (this.mThrArcAngleDegree * 2.0f), false, this.mOutThrCirclePaint);
        float tempLeft = this.mArcLeftCircleCenterX - this.mArcCircleRadius;
        float tempTop = this.mArcCircleCenterY - this.mArcCircleRadius;
        float tempRight = this.mArcLeftCircleCenterX + this.mArcCircleRadius;
        canvas.drawArc(tempLeft, tempTop, tempRight, this.mArcCircleCenterY + this.mArcCircleRadius, this.mArcAngleDegree - 90.0f, 90.0f - this.mArcAngleDegree, false, this.mOutCirclePaint);
        float tempLeft2 = this.mSecArcLeftCircleCenterX - this.mSecArcCircleRadius;
        float tempTop2 = this.mSecArcCircleCenterY - this.mSecArcCircleRadius;
        float tempRight2 = this.mSecArcLeftCircleCenterX + this.mSecArcCircleRadius;
        Canvas canvas4 = canvas;
        canvas4.drawArc(tempLeft2, tempTop2, tempRight2, this.mSecArcCircleCenterY + this.mSecArcCircleRadius, this.mSecArcAngleDegree - 90.0f, 90.0f - this.mSecArcAngleDegree, false, this.mOutSecCirclePaint);
        float tempLeft3 = this.mThrArcLeftCircleCenterX - this.mThrArcCircleRadius;
        float tempTop3 = this.mThrArcCircleCenterY - this.mThrArcCircleRadius;
        float tempRight3 = this.mThrArcLeftCircleCenterX + this.mThrArcCircleRadius;
        canvas4.drawArc(tempLeft3, tempTop3, tempRight3, this.mThrArcCircleCenterY + this.mThrArcCircleRadius, this.mThrArcAngleDegree - 90.0f, 90.0f - this.mThrArcAngleDegree, false, this.mOutThrCirclePaint);
        float tempLeft4 = this.mArcRightCircleCenterX - this.mArcCircleRadius;
        float tempTop4 = this.mArcCircleCenterY - this.mArcCircleRadius;
        float tempRight4 = this.mArcRightCircleCenterX + this.mArcCircleRadius;
        canvas4.drawArc(tempLeft4, tempTop4, tempRight4, this.mArcCircleCenterY + this.mArcCircleRadius, 180.0f, 90.0f - this.mArcAngleDegree, false, this.mOutCirclePaint);
        float tempLeft5 = this.mSecArcRightCircleCenterX - this.mSecArcCircleRadius;
        float tempTop5 = this.mSecArcCircleCenterY - this.mSecArcCircleRadius;
        float tempRight5 = this.mSecArcRightCircleCenterX + this.mSecArcCircleRadius;
        canvas4.drawArc(tempLeft5, tempTop5, tempRight5, this.mSecArcCircleCenterY + this.mSecArcCircleRadius, 180.0f, 90.0f - this.mSecArcAngleDegree, false, this.mOutSecCirclePaint);
        float tempLeft6 = this.mThrArcRightCircleCenterX - this.mThrArcCircleRadius;
        float tempTop6 = this.mThrArcCircleCenterY - this.mThrArcCircleRadius;
        float tempRight6 = this.mThrArcRightCircleCenterX + this.mThrArcCircleRadius;
        canvas4.drawArc(tempLeft6, tempTop6, tempRight6, this.mThrArcCircleCenterY + this.mThrArcCircleRadius, 180.0f, 90.0f - this.mThrArcAngleDegree, false, this.mOutThrCirclePaint);
        Canvas canvas5 = canvas;
        canvas5.drawLine((float) this.mTrackLeftX, (float) this.mTrackTopY, (float) this.mTrackLeftX, (float) this.mViewHeight, this.mOutCirclePaint);
        canvas5.drawLine((float) this.mTrackRightX, (float) this.mTrackTopY, (float) this.mTrackRightX, (float) this.mViewHeight, this.mOutCirclePaint);
        canvas5.drawLine((float) this.mTrackLeftX, (float) this.mSecTrackTopY, (float) this.mTrackLeftX, (float) this.mViewHeight, this.mOutSecCirclePaint);
        canvas5.drawLine((float) this.mTrackRightX, (float) this.mSecTrackTopY, (float) this.mTrackRightX, (float) this.mViewHeight, this.mOutSecCirclePaint);
        canvas5.drawLine((float) this.mTrackLeftX, (float) this.mThrTrackTopY, (float) this.mTrackLeftX, (float) this.mViewHeight, this.mOutThrCirclePaint);
        canvas5.drawLine((float) this.mTrackRightX, (float) this.mThrTrackTopY, (float) this.mTrackRightX, (float) this.mViewHeight, this.mOutThrCirclePaint);
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(this.mViewWidth, this.mViewHeight);
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        checkScreenSize();
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
            this.mOutCirclePaint.setStrokeWidth((float) this.mOuterCircleWidth);
            this.mOutSecCirclePaint.setStrokeWidth((float) this.mOuterSecCircleWidth);
            this.mOutThrCirclePaint.setStrokeWidth((float) this.mOuterThrCircleWidth);
            LinearGradient linearGradient = new LinearGradient(0.0f, 0.0f, 0.0f, (float) this.mViewHeight, new int[]{OUTER_CIRCLE_START_COLOR, OUTER_CIRCLE_MIDDLE_COLOR, OUTER_CIRCLE_END_COLOR}, new float[]{0.0f, 0.34f, 1.0f}, Shader.TileMode.CLAMP);
            this.mOutCirclePaint.setShader(linearGradient);
            this.mOutSecCirclePaint.setShader(linearGradient);
            this.mOutThrCirclePaint.setShader(linearGradient);
            requestLayout();
        }
    }
}
