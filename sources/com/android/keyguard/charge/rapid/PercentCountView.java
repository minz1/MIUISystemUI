package com.android.keyguard.charge.rapid;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.WindowManager;
import android.widget.LinearLayout;

public class PercentCountView extends LinearLayout {
    private float mCurrentProgress;
    private NumberDrawView mFractionTv;
    private NumberDrawView mIntegerTv;
    private float mLargeTextSizePx;
    private Point mScreenSize;
    private float mSmallTextSizePx;
    private WindowManager mWindowManager;

    public PercentCountView(Context context) {
        this(context, null);
    }

    public PercentCountView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PercentCountView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        this.mWindowManager = (WindowManager) context.getSystemService("window");
        this.mScreenSize = new Point();
        this.mWindowManager.getDefaultDisplay().getRealSize(this.mScreenSize);
        updateSizeForScreenSizeChange();
        Typeface fontTypeFace = Typeface.createFromAsset(context.getAssets(), "fonts/Mitype2018-35.otf");
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(-2, -2);
        this.mCurrentProgress = 0.0f;
        setOrientation(0);
        setGravity(81);
        this.mIntegerTv = new NumberDrawView(context);
        this.mIntegerTv.setTextSize(0, this.mLargeTextSizePx);
        this.mIntegerTv.setTextColor(Color.parseColor("#FFFFFF"));
        if (fontTypeFace != null) {
            this.mIntegerTv.setTypeface(fontTypeFace);
        }
        addView(this.mIntegerTv, llp);
        this.mFractionTv = new NumberDrawView(context);
        this.mFractionTv.setTextSize(0, this.mSmallTextSizePx);
        this.mFractionTv.setTextColor(Color.parseColor("#FFFFFF"));
        Typeface fontTypeFace2 = Typeface.createFromAsset(context.getAssets(), "fonts/Mitype2018-60.otf");
        if (fontTypeFace2 != null) {
            this.mFractionTv.setTypeface(fontTypeFace2);
        }
        this.mFractionTv.setText("%");
        addView(this.mFractionTv, llp);
        setProgress(0.0f);
    }

    public void setProgress(float progress) {
        if (progress >= 0.0f && progress <= 100.0f) {
            this.mCurrentProgress = progress;
            this.mIntegerTv.setText(String.valueOf((int) this.mCurrentProgress));
        }
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
            this.mIntegerTv.setTextSize(0, this.mLargeTextSizePx);
            this.mFractionTv.setTextSize(0, this.mSmallTextSizePx);
            requestLayout();
        }
    }

    private void updateSizeForScreenSizeChange() {
        float rateWidth = (((float) Math.min(this.mScreenSize.x, this.mScreenSize.y)) * 1.0f) / 1080.0f;
        this.mLargeTextSizePx = (float) ((int) (188.0f * rateWidth));
        this.mSmallTextSizePx = (float) ((int) (60.0f * rateWidth));
    }
}
