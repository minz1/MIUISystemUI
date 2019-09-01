package com.android.keyguard.fod;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;
import com.android.systemui.R;
import miui.os.Build;

public class MiuiGxzwHighlightView extends ImageView {
    private static final boolean GRADUAL_GREEN = (!"equuleus".equals(Build.DEVICE) && !"ursa".equals(Build.DEVICE) && !"cepheus".equals(Build.DEVICE) && !"grus".equals(Build.DEVICE));
    private static final int LIGHT_TIP_COLOR = MiuiGxzwUtils.getGxzwCircleColor();
    private Paint mPaint;

    public MiuiGxzwHighlightView(Context context) {
        super(context);
    }

    public MiuiGxzwHighlightView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MiuiGxzwHighlightView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setVisibility(int visibility) {
        Log.i("MiuiGxzwHighlightView", "setVisibility: visibility = " + visibility);
        super.setVisibility(visibility);
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!GRADUAL_GREEN) {
            int width = getWidth();
            int heigth = getHeight();
            canvas.drawCircle((float) (width / 2), (float) (heigth / 2), (float) (Math.min(width, heigth) / 2), this.mPaint);
        }
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        initView();
    }

    private void initView() {
        this.mPaint = new Paint();
        this.mPaint.setColor(LIGHT_TIP_COLOR);
        this.mPaint.setAntiAlias(true);
        if (GRADUAL_GREEN) {
            setImageResource(R.drawable.gxzw_green_light);
        }
    }
}
