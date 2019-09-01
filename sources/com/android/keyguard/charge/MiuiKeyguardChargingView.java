package com.android.keyguard.charge;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import com.android.systemui.R;

public class MiuiKeyguardChargingView extends View {
    private Paint mBackCirclePaint;
    private int mBatteryAnimaLevel = 0;
    private int mBatteryLevel = 0;
    private TextView mChargingHint;
    private float mCircleBorderWidth;
    private float mCirclePadding = 2.0f;
    private MiuiKeyguardChargingContainer mContainer;
    private boolean mDarkMode;
    private int mDensityDpi;
    int mDuar = 2;
    private Paint mGradientCirclePaint;
    private int mInitHeight;
    private Paint mTextNumPaint;
    private Paint mTextPaint;
    private float mTextSizeRatio = 0.0f;

    public MiuiKeyguardChargingView(Context context) {
        super(context);
        init();
    }

    public MiuiKeyguardChargingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MiuiKeyguardChargingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        this.mCircleBorderWidth = (float) getResources().getDimensionPixelSize(R.dimen.keyguard_charging_view_line_width);
        this.mBackCirclePaint = new Paint();
        this.mBackCirclePaint.setStyle(Paint.Style.STROKE);
        this.mBackCirclePaint.setAntiAlias(true);
        this.mBackCirclePaint.setColor(getResources().getColor(R.color.keyguard_charging_view_bg_color));
        this.mBackCirclePaint.setStrokeWidth(this.mCircleBorderWidth);
        this.mGradientCirclePaint = new Paint();
        this.mGradientCirclePaint.setStyle(Paint.Style.STROKE);
        this.mGradientCirclePaint.setAntiAlias(true);
        this.mGradientCirclePaint.setColor(getResources().getColor(R.color.keyguard_charging_view_color));
        this.mGradientCirclePaint.setStrokeWidth(this.mCircleBorderWidth);
        this.mTextNumPaint = new Paint();
        this.mTextNumPaint.setAntiAlias(true);
        this.mTextNumPaint.setTextSize((float) getResources().getDimensionPixelSize(R.dimen.battery_charging_progress_view_text_size));
        this.mTextNumPaint.setColor(getResources().getColor(R.color.battery_charging_progress_view_text_color));
        this.mTextPaint = new Paint();
        this.mTextPaint.setAntiAlias(true);
        this.mTextPaint.setTextSize((float) getResources().getDimensionPixelSize(R.dimen.battery_charging_progress_view_msg_size));
        this.mTextPaint.setColor(getResources().getColor(R.color.battery_charging_progress_view_msg_text_color));
        this.mInitHeight = getResources().getDimensionPixelOffset(R.dimen.keyguard_charging_view_height);
        this.mTextSizeRatio = (((float) this.mInitHeight) * 1.0f) / ((float) getResources().getDimensionPixelSize(R.dimen.battery_charging_progress_view_text_size));
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        int densityDpi = newConfig.densityDpi;
        if (this.mDensityDpi != densityDpi) {
            init();
            this.mDensityDpi = densityDpi;
        }
    }

    public void setDarkMode(boolean darkMode) {
        int i;
        int i2;
        this.mDarkMode = darkMode;
        if (this.mChargingHint != null) {
            this.mChargingHint.setTextColor(getResources().getColor(darkMode ? R.color.miui_common_unlock_screen_common_dark_text_color : R.color.battery_charging_progress_view_text_color));
        }
        if (this.mTextNumPaint != null && this.mBackCirclePaint != null) {
            Paint paint = this.mTextNumPaint;
            if (darkMode) {
                i = getResources().getColor(R.color.miui_common_unlock_screen_common_dark_text_color);
            } else {
                i = getResources().getColor(R.color.battery_charging_progress_view_text_color);
            }
            paint.setColor(i);
            Paint paint2 = this.mBackCirclePaint;
            if (darkMode) {
                i2 = getResources().getColor(R.color.keyguard_charging_view_bg_color_dark);
            } else {
                i2 = getResources().getColor(R.color.keyguard_charging_view_bg_color);
            }
            paint2.setColor(i2);
            invalidate();
        }
    }

    public void setNeedRepositionDevice(boolean needRepositionDevice) {
        if (needRepositionDevice) {
            this.mGradientCirclePaint.setColor(getResources().getColor(R.color.keyguard_charging_view_need_reset_color));
        } else {
            this.mGradientCirclePaint.setColor(getResources().getColor(R.color.keyguard_charging_view_color));
        }
        postInvalidate();
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        int i;
        int i2;
        super.onDraw(canvas);
        int start = ((int) (((((double) this.mBatteryAnimaLevel) / 100.0d) * ((double) (360 - this.mDuar))) + ((double) (this.mDuar * 2)))) - 90;
        canvas.drawArc(new RectF(this.mCirclePadding * 2.0f, this.mCirclePadding * 2.0f, ((float) getMeasuredWidth()) - (this.mCirclePadding * 2.0f), ((float) getMeasuredHeight()) - (this.mCirclePadding * 2.0f)), (float) start, (float) (270 - start), false, this.mBackCirclePaint);
        if (this.mBatteryLevel == 100) {
            canvas.drawArc(new RectF(this.mCirclePadding * 2.0f, this.mCirclePadding * 2.0f, ((float) getMeasuredWidth()) - (this.mCirclePadding * 2.0f), ((float) getMeasuredHeight()) - (this.mCirclePadding * 2.0f)), -90.0f, ((float) (((double) this.mBatteryAnimaLevel) / 100.0d)) * 360.0f, false, this.mGradientCirclePaint);
        } else {
            canvas.drawArc(new RectF(this.mCirclePadding * 2.0f, this.mCirclePadding * 2.0f, ((float) getMeasuredWidth()) - (this.mCirclePadding * 2.0f), ((float) getMeasuredHeight()) - (this.mCirclePadding * 2.0f)), (float) (-90 + this.mDuar), ((float) (((double) this.mBatteryAnimaLevel) / 100.0d)) * ((float) (360 - this.mDuar)), false, this.mGradientCirclePaint);
        }
        int centerX = getMeasuredWidth() / 2;
        int centerY = getMeasuredHeight() / 2;
        boolean isOnTop = isShowChargingHint();
        if (this.mTextSizeRatio != 0.0f) {
            if (isOnTop) {
                this.mTextNumPaint.setTypeface(Typeface.create("miui-light", 0));
                this.mTextNumPaint.setTextSize((float) getResources().getDimensionPixelSize(R.dimen.battery_charging_progress_view_large_text_size));
            } else {
                this.mTextNumPaint.setTypeface(null);
                this.mTextNumPaint.setTextSize(((float) getMeasuredHeight()) / this.mTextSizeRatio);
            }
        }
        Paint paint = this.mTextNumPaint;
        int i3 = R.color.battery_charging_progress_view_text_color;
        if (isOnTop) {
            i = getResources().getColor(R.color.battery_charging_progress_view_text_color);
        } else if (this.mDarkMode) {
            i = getResources().getColor(R.color.miui_common_unlock_screen_common_dark_text_color);
        } else {
            i = getResources().getColor(R.color.battery_charging_progress_view_text_color);
        }
        paint.setColor(i);
        Paint paint2 = this.mBackCirclePaint;
        if (isOnTop) {
            i2 = getResources().getColor(R.color.keyguard_charging_view_bg_color);
        } else if (this.mDarkMode) {
            i2 = getResources().getColor(R.color.keyguard_charging_view_bg_color_dark);
        } else {
            i2 = getResources().getColor(R.color.keyguard_charging_view_bg_color);
        }
        paint2.setColor(i2);
        if (this.mChargingHint != null) {
            TextView textView = this.mChargingHint;
            Resources resources = getResources();
            if (!isOnTop && this.mDarkMode) {
                i3 = R.color.miui_common_unlock_screen_common_dark_text_color;
            }
            textView.setTextColor(resources.getColor(i3));
        }
        Canvas canvas2 = canvas;
        canvas2.drawText(this.mBatteryLevel + "", ((float) centerX) - (this.mTextNumPaint.measureText(this.mBatteryLevel + "") / 2.0f), (float) ((((int) (Math.ceil((double) (this.mTextNumPaint.getFontMetrics().descent - this.mTextNumPaint.getFontMetrics().ascent)) + 2.0d)) / 4) + centerY), this.mTextNumPaint);
    }

    public void setChargingLevelForAnima(int level) {
        this.mBatteryAnimaLevel = level;
        invalidate();
    }

    public void setChargingLevel(int level) {
        this.mBatteryLevel = level;
        this.mBatteryAnimaLevel = level;
        invalidate();
    }

    public void setChargingHint(TextView tx) {
        this.mChargingHint = tx;
    }

    public void setChargingContainer(MiuiKeyguardChargingContainer container) {
        this.mContainer = container;
    }

    private boolean isShowChargingHint() {
        boolean z = false;
        if (this.mContainer == null) {
            return false;
        }
        if (this.mContainer.isFullScreen() && getY() < ((float) ((this.mContainer.getScreenHeight() * 2) / 3))) {
            z = true;
        }
        return z;
    }
}
