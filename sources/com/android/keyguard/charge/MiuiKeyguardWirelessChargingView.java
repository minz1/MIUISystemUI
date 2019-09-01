package com.android.keyguard.charge;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.Xfermode;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.PathInterpolator;
import com.android.systemui.R;
import miui.maml.animation.interpolater.ElasticEaseOutInterpolater;
import miui.maml.animation.interpolater.QuartEaseInOutInterpolater;

public class MiuiKeyguardWirelessChargingView extends View {
    private static final float[][] ENTER_BALL_CIRCLER_RADIUS_VALUE = {new float[]{146.0f, 193.0f, 205.0f}, new float[]{172.0f, 220.0f, 231.0f}, new float[]{198.0f, 245.0f, 257.0f}, new float[]{224.0f, 271.0f, 283.0f}, new float[]{250.0f, 297.0f, 309.0f}, new float[]{277.0f, 325.0f, 336.0f}, new float[]{303.0f, 351.0f, 362.0f}, new float[]{329.0f, 377.0f, 388.0f}};
    private static final float[][] ENTER_BALL_DOTS_RADIUS_VALUE = {new float[]{0.0f, 7.08f, 8.8f}, new float[]{0.0f, 6.715f, 8.35f}, new float[]{0.0f, 6.715f, 8.35f}, new float[]{0.0f, 6.715f, 8.35f}, new float[]{0.0f, 6.715f, 8.35f}, new float[]{0.0f, 4.75f, 6.0f}, new float[]{0.0f, 3.5f, 4.5f}, new float[]{0.0f, 2.5f, 3.0f}};
    private static final float[][] EXIT_BALL_CIRCLER_RADIUS_VALUE = {new float[]{205.0f, 146.0f}, new float[]{231.0f, 172.0f}, new float[]{257.0f, 198.0f}, new float[]{283.0f, 224.0f}, new float[]{309.0f, 250.0f}, new float[]{336.0f, 277.0f}, new float[]{362.0f, 303.0f}, new float[]{388.0f, 329.0f}};
    private static final float[][] EXIT_BALL_DOTS_RADIUS_VALUE = {new float[]{8.8f, 0.0f}, new float[]{8.35f, 0.0f}, new float[]{8.35f, 0.0f}, new float[]{8.35f, 0.0f}, new float[]{8.35f, 0.0f}, new float[]{6.0f, 0.0f}, new float[]{4.5f, 0.0f}, new float[]{3.0f, 0.0f}};
    private final int COUNT = 28;
    private Interpolator mBackInterpolator = new ElasticEaseOutInterpolater(0.5f, 0.3f);
    private Bitmap mBallBackground;
    private Paint mBallPaint;
    private Interpolator mDecelerateInterpolator = new DecelerateInterpolator();
    private Interpolator mExitInterpolator = new QuartEaseInOutInterpolater();
    private int mLevel;
    private Bitmap mLightingBitmap;
    private Paint mLightingPaint;
    private Interpolator mLinearInterpolator = new LinearInterpolator();
    private Interpolator mPathInterpolator = new PathInterpolator(0.1f, 0.8f, 0.5f, 1.0f);
    private boolean mScreenOnWhenStartAnim;
    private Paint mTextNumPaint;
    private int mTextSize;
    private int mTime;
    private Xfermode mXfermode = new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP);

    public MiuiKeyguardWirelessChargingView(Context context) {
        super(context);
    }

    public MiuiKeyguardWirelessChargingView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MiuiKeyguardWirelessChargingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setTime(int t) {
        this.mTime = t;
        invalidate();
    }

    public void setChargingProgress(int level) {
        this.mLevel = level;
        invalidate();
    }

    public void setScreenStateWhenStartAnim(boolean on) {
        this.mScreenOnWhenStartAnim = on;
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawBackgroundAnim(canvas);
        drawBallAnim(canvas);
        drawLightingAnim(canvas);
        drawTextAnim(canvas);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mBallPaint = new Paint();
        this.mBallPaint.setAntiAlias(true);
        this.mTextNumPaint = new Paint();
        this.mTextNumPaint.setAntiAlias(true);
        this.mTextNumPaint.setColor(3722239);
        this.mTextNumPaint.setTypeface(Typeface.createFromAsset(getContext().getAssets(), "fonts/Mitype2018-50.otf"));
        this.mLightingPaint = new Paint();
        this.mTextSize = getResources().getDimensionPixelSize(R.dimen.battery_charging_wireless_progress_view_text_size);
        this.mLightingBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.wireless_charge_lighting);
        this.mBallBackground = BitmapFactory.decodeResource(getResources(), R.drawable.wireless_charge_ball_bg);
    }

    private void drawBackgroundAnim(Canvas canvas) {
        float alpha = 0.9f;
        if (!this.mScreenOnWhenStartAnim) {
            alpha = 0.9f;
        } else if (this.mTime >= 0 && this.mTime <= 200) {
            alpha = getAnimtionValue((float) this.mTime, 0.0f, 200.0f, 0.0f, 0.9f, this.mLinearInterpolator);
        } else if (this.mTime >= 9040 && this.mTime <= 9240) {
            alpha = getAnimtionValue((float) this.mTime, 9040.0f, 9240.0f, 0.9f, 0.0f, this.mLinearInterpolator);
        }
        canvas.drawARGB((int) (255.0f * alpha), 0, 0, 0);
    }

    private void drawBallAnim(Canvas canvas) {
        Canvas canvas2 = canvas;
        int saveCount = canvas2.saveLayer(0.0f, 0.0f, (float) getMeasuredWidth(), (float) getMeasuredHeight(), null, 31);
        if (this.mTime >= 200 && this.mTime <= 8600) {
            drawBallEnterAnim(canvas);
        } else if (this.mTime > 8600) {
            drawBallExitAnim(canvas);
        }
        drawBallBackgroundAnim(canvas);
        canvas.restoreToCount(saveCount);
    }

    private void drawBallEnterAnim(Canvas canvas) {
        int i;
        int endTime;
        int time;
        int i2;
        int i3 = 200;
        int endTime2 = ((ENTER_BALL_CIRCLER_RADIUS_VALUE.length - 1) * 80) + 200 + 1480;
        int time2 = this.mTime;
        if (time2 >= 200 && time2 <= endTime2) {
            time2 = (int) getAnimtionValue((float) this.mTime, 200.0f, (float) endTime2, 200.0f, (float) endTime2, this.mPathInterpolator);
        }
        int i4 = 0;
        while (true) {
            int i5 = i4;
            if (i5 < ENTER_BALL_CIRCLER_RADIUS_VALUE.length) {
                int startTime = i3 + (80 * i5);
                if (time2 <= startTime + 280) {
                    i2 = i5;
                    time = time2;
                    endTime = endTime2;
                    i = i3;
                    drawOneCircleBallAnim(canvas, time2, startTime, 280, ENTER_BALL_CIRCLER_RADIUS_VALUE[i5][0] * 1.16f, ENTER_BALL_CIRCLER_RADIUS_VALUE[i5][1] * 1.16f, ENTER_BALL_DOTS_RADIUS_VALUE[i5][0] * 1.16f, ENTER_BALL_DOTS_RADIUS_VALUE[i5][1] * 1.16f, i5 % 2 == 0 ? 0.0f : 6.428571f, this.mDecelerateInterpolator);
                } else {
                    i2 = i5;
                    time = time2;
                    endTime = endTime2;
                    i = i3;
                    drawOneCircleBallAnim(canvas, time, startTime + 280, 1480 - 280, ENTER_BALL_CIRCLER_RADIUS_VALUE[i2][1] * 1.16f, ENTER_BALL_CIRCLER_RADIUS_VALUE[i2][2] * 1.16f, ENTER_BALL_DOTS_RADIUS_VALUE[i2][1] * 1.16f, ENTER_BALL_DOTS_RADIUS_VALUE[i2][2] * 1.16f, i2 % 2 == 0 ? 0.0f : 6.428571f, this.mDecelerateInterpolator);
                }
                i4 = i2 + 1;
                time2 = time;
                endTime2 = endTime;
                i3 = i;
            } else {
                int i6 = endTime2;
                return;
            }
        }
    }

    private void drawBallExitAnim(Canvas canvas) {
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 < EXIT_BALL_CIRCLER_RADIUS_VALUE.length) {
                drawOneCircleBallAnim(canvas, this.mTime, 8600 + (40 * i2), 280, EXIT_BALL_CIRCLER_RADIUS_VALUE[i2][0] * 1.16f, EXIT_BALL_CIRCLER_RADIUS_VALUE[i2][1] * 1.16f, EXIT_BALL_DOTS_RADIUS_VALUE[i2][0] * 1.16f, EXIT_BALL_DOTS_RADIUS_VALUE[i2][1] * 1.16f, i2 % 2 == 0 ? 0.0f : 6.428571f, this.mExitInterpolator);
                i = i2 + 1;
            } else {
                return;
            }
        }
    }

    private void drawOneCircleBallAnim(Canvas canvas, int currentTime, int startTime, int duration, float circleRadiusStart, float circleRadiusEnd, float dotsRadiusStart, float dotsRadiusEnd, float angle, Interpolator interpolator) {
        int i = currentTime;
        int i2 = startTime;
        int i3 = duration;
        int centerX = getCenterX();
        int centerY = getCenterY();
        Interpolator interpolator2 = interpolator;
        float circleRadius = getAnimtionValue((float) i, (float) i2, ((float) i3) + ((float) i2), circleRadiusStart, circleRadiusEnd, interpolator2);
        float dotsRadius = getAnimtionValue((float) i, (float) i2, ((float) i3) + ((float) i2), dotsRadiusStart, dotsRadiusEnd, interpolator2);
        float alpha = 1.0f;
        if (i < 200) {
            alpha = 0.0f;
        } else if (i <= 400) {
            alpha = getAnimtionValue((float) i, 200.0f, 400.0f, 0.0f, 1.0f, this.mLinearInterpolator);
        } else if (i >= 9040 && i <= 9240) {
            alpha = getAnimtionValue((float) i, 9040.0f, 9240.0f, 1.0f, 0.0f, this.mLinearInterpolator);
        }
        this.mBallPaint.setStyle(Paint.Style.FILL);
        this.mBallPaint.setColor(-1);
        this.mBallPaint.setAlpha((int) (255.0f * alpha));
        for (int i4 = 0; i4 < 28; i4++) {
            canvas.drawCircle(getCircleCoordinateX(centerX, circleRadius, ((((float) i4) * 360.0f) / 28.0f) + angle), getCircleCoordinateY(centerY, circleRadius, ((((float) i4) * 360.0f) / 28.0f) + angle), dotsRadius, this.mBallPaint);
        }
        Canvas canvas2 = canvas;
    }

    private void drawBallBackgroundAnim(Canvas canvas) {
        this.mBallPaint.setXfermode(this.mXfermode);
        float alpha = 1.0f;
        if (this.mTime < 200) {
            alpha = 0.0f;
        } else if (this.mTime <= 400) {
            alpha = getAnimtionValue((float) this.mTime, 200.0f, 400.0f, 0.0f, 1.0f, this.mLinearInterpolator);
        } else if (this.mTime >= 9040 && this.mTime <= 9240) {
            alpha = getAnimtionValue((float) this.mTime, 9040.0f, 9240.0f, 1.0f, 0.0f, this.mLinearInterpolator);
        }
        int centerX = getCenterX();
        int centerY = getCenterY();
        Rect src = new Rect(0, 0, this.mBallBackground.getWidth(), this.mBallBackground.getHeight());
        Rect dst = new Rect(centerX - (this.mBallBackground.getWidth() / 2), centerY - (this.mBallBackground.getHeight() / 2), (this.mBallBackground.getWidth() / 2) + centerX, (this.mBallBackground.getHeight() / 2) + centerY);
        this.mBallPaint.setAlpha((int) (255.0f * alpha));
        canvas.drawBitmap(this.mBallBackground, src, dst, this.mBallPaint);
        drawBallCircleAnim(canvas);
        this.mBallPaint.setXfermode(null);
    }

    private void drawBallCircleAnim(Canvas canvas) {
        int centerX = getCenterX();
        int centerY = getCenterY();
        float radius = 0.0f;
        if (this.mTime >= 1720 && this.mTime <= 2960) {
            radius = getAnimtionValue((float) this.mTime, 1720.0f, 2960.0f, 140.0f, 462.0f, this.mLinearInterpolator);
        } else if (this.mTime >= 4400 && this.mTime <= 5640) {
            radius = getAnimtionValue((float) this.mTime, 4400.0f, 5640.0f, 140.0f, 462.0f, this.mLinearInterpolator);
        } else if (this.mTime >= 6880 && this.mTime <= 8120) {
            radius = getAnimtionValue((float) this.mTime, 6880.0f, 8120.0f, 140.0f, 462.0f, this.mLinearInterpolator);
        }
        if (radius >= 140.0f) {
            float radius2 = radius + 57.0f;
            this.mBallPaint.setColor(-9508865);
            this.mBallPaint.setAlpha(255);
            this.mBallPaint.setStyle(Paint.Style.FILL);
            float f = radius2;
            RadialGradient rg = new RadialGradient((float) centerX, (float) centerY, f, new int[]{0, 0, -9508865, 0}, new float[]{0.0f, (radius2 - 114.0f) / radius2, (radius2 - 57.0f) / radius2, 1.0f}, Shader.TileMode.REPEAT);
            this.mBallPaint.setShader(rg);
            canvas.drawCircle((float) centerX, (float) centerY, radius2, this.mBallPaint);
            this.mBallPaint.setShader(null);
        }
    }

    private void drawLightingAnim(Canvas canvas) {
        int centerX = getCenterX();
        int centerY = getCenterY();
        float sizeRatio = 0.0f;
        float alphaRatio = 0.0f;
        if (this.mTime >= 200 && this.mTime <= 400) {
            alphaRatio = getAnimtionValue((float) this.mTime, 200.0f, 400.0f, 0.0f, 1.0f, this.mLinearInterpolator);
            sizeRatio = getAnimtionValue((float) this.mTime, 200.0f, 400.0f, 0.5f, 1.0f, this.mLinearInterpolator);
        } else if (this.mTime > 400 && this.mTime < 2400) {
            sizeRatio = 1.0f;
            alphaRatio = 1.0f;
        } else if (this.mTime >= 2400 && this.mTime <= 2640) {
            alphaRatio = getAnimtionValue((float) this.mTime, 2400.0f, 2640.0f, 1.0f, 0.0f, this.mLinearInterpolator);
            sizeRatio = getAnimtionValue((float) this.mTime, 2400.0f, 2640.0f, 1.0f, 0.5f, this.mLinearInterpolator);
        }
        this.mLightingPaint.setAlpha((int) (255.0f * alphaRatio));
        canvas.drawBitmap(this.mLightingBitmap, new Rect(0, 0, this.mLightingBitmap.getWidth(), this.mLightingBitmap.getHeight()), new Rect((int) (((float) centerX) - ((((float) this.mLightingBitmap.getWidth()) * sizeRatio) / 2.0f)), (int) (((float) centerY) - ((((float) this.mLightingBitmap.getHeight()) * sizeRatio) / 2.0f)), (int) (((float) centerX) + ((((float) this.mLightingBitmap.getWidth()) * sizeRatio) / 2.0f)), (int) (((float) centerY) + ((((float) this.mLightingBitmap.getHeight()) * sizeRatio) / 2.0f))), this.mLightingPaint);
    }

    private void drawTextAnim(Canvas canvas) {
        int centerX = getCenterX();
        int centerY = getCenterY();
        float sizeRatio = 0.0f;
        float alphaRatio = 0.0f;
        if (this.mTime >= 2480 && this.mTime <= 3680) {
            sizeRatio = getAnimtionValue((float) this.mTime, 2480.0f, 3680.0f, 0.5f, 1.0f, this.mBackInterpolator);
            alphaRatio = getAnimtionValue((float) this.mTime, 2480.0f, 2720.0f, 0.0f, 1.0f, this.mLinearInterpolator);
        } else if (this.mTime > 3680 && this.mTime < 8800) {
            sizeRatio = 1.0f;
            alphaRatio = 1.0f;
        } else if (this.mTime >= 8800 && this.mTime <= 9000) {
            sizeRatio = getAnimtionValue((float) this.mTime, 8800.0f, 9000.0f, 1.0f, 0.5f, this.mLinearInterpolator);
            alphaRatio = getAnimtionValue((float) this.mTime, 8800.0f, 9000.0f, 1.0f, 0.0f, this.mLinearInterpolator);
        }
        this.mTextNumPaint.setTextSize(((float) this.mTextSize) * sizeRatio);
        this.mTextNumPaint.setAlpha((int) (255.0f * alphaRatio));
        String text = String.valueOf(this.mLevel);
        canvas.drawText(text, ((float) centerX) - (this.mTextNumPaint.measureText(text) / 2.0f), (float) ((((int) (Math.ceil((double) (this.mTextNumPaint.getFontMetrics().descent - this.mTextNumPaint.getFontMetrics().ascent)) + 2.0d)) / 4) + centerY), this.mTextNumPaint);
    }

    private float getAnimtionValue(float x, float startX, float endX, float startY, float endY, Interpolator interpolator) {
        float radio = 1.0f;
        float radio2 = ((x - startX) * 1.0f) / (endX - startX);
        float f = 0.0f;
        if (radio2 >= 0.0f) {
            f = radio2;
        }
        float radio3 = f;
        if (radio3 <= 1.0f) {
            radio = radio3;
        }
        return ((endY - startY) * interpolator.getInterpolation(radio)) + startY;
    }

    private float getCircleCoordinateX(int x, float r, float a) {
        return ((float) x) + ((float) (((double) r) * Math.cos((((double) a) * 3.14d) / 180.0d)));
    }

    private float getCircleCoordinateY(int y, float r, float a) {
        return ((float) y) + ((float) (((double) r) * Math.sin((((double) a) * 3.14d) / 180.0d)));
    }

    private int getCenterX() {
        return getMeasuredWidth() / 2;
    }

    private int getCenterY() {
        return getMeasuredHeight() / 2;
    }
}
