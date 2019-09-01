package com.android.keyguard;

import android.content.Context;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.widget.TextView;
import com.android.systemui.R;

public class MiuiKeyguardBatteryInfoView extends TextView {
    private int mChargingProgress;
    private Paint mChargingProgressPaint;

    public MiuiKeyguardBatteryInfoView(Context context) {
        super(context);
        init();
    }

    public MiuiKeyguardBatteryInfoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MiuiKeyguardBatteryInfoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        BitmapShader chargingProgressBitmapShader = new BitmapShader(((BitmapDrawable) this.mContext.getResources().getDrawable(R.drawable.keyguard_battery_charging_progress)).getBitmap(), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        this.mChargingProgressPaint = new Paint(1);
        this.mChargingProgressPaint.setShader(chargingProgressBitmapShader);
        setTypeface(Typeface.create("miuiex", 0));
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawArc(new RectF(0.0f, 0.0f, (float) getWidth(), (float) getHeight()), -90.0f, (float) ((this.mChargingProgress * 360) / 100), true, this.mChargingProgressPaint);
    }
}
