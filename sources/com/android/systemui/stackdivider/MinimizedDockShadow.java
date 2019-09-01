package com.android.systemui.stackdivider;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import com.android.systemui.R;

public class MinimizedDockShadow extends View {
    private int mDockSide = -1;
    private final Paint mShadowPaint = new Paint();

    public MinimizedDockShadow(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setDockSide(int dockSide) {
        if (dockSide != this.mDockSide) {
            this.mDockSide = dockSide;
            updatePaint(getLeft(), getTop(), getRight(), getBottom());
            invalidate();
        }
    }

    private void updatePaint(int left, int top, int right, int bottom) {
        int startColor = this.mContext.getResources().getColor(R.color.minimize_dock_shadow_start, null);
        int endColor = this.mContext.getResources().getColor(R.color.minimize_dock_shadow_end, null);
        int middleColor = Color.argb((Color.alpha(startColor) + Color.alpha(endColor)) / 2, 0, 0, 0);
        int quarter = Color.argb((int) ((((float) Color.alpha(startColor)) * 0.25f) + (((float) Color.alpha(endColor)) * 0.75f)), 0, 0, 0);
        if (this.mDockSide == 2) {
            Paint paint = this.mShadowPaint;
            LinearGradient linearGradient = r11;
            LinearGradient linearGradient2 = new LinearGradient(0.0f, 0.0f, 0.0f, (float) (bottom - top), new int[]{startColor, middleColor, quarter, endColor}, new float[]{0.0f, 0.35f, 0.6f, 1.0f}, Shader.TileMode.CLAMP);
            paint.setShader(linearGradient);
        } else if (this.mDockSide == 1) {
            Paint paint2 = this.mShadowPaint;
            LinearGradient linearGradient3 = r11;
            LinearGradient linearGradient4 = new LinearGradient(0.0f, 0.0f, (float) (right - left), 0.0f, new int[]{startColor, middleColor, quarter, endColor}, new float[]{0.0f, 0.35f, 0.6f, 1.0f}, Shader.TileMode.CLAMP);
            paint2.setShader(linearGradient3);
        } else if (this.mDockSide == 3) {
            Paint paint3 = this.mShadowPaint;
            LinearGradient linearGradient5 = r11;
            LinearGradient linearGradient6 = new LinearGradient((float) (right - left), 0.0f, 0.0f, 0.0f, new int[]{startColor, middleColor, quarter, endColor}, new float[]{0.0f, 0.35f, 0.6f, 1.0f}, Shader.TileMode.CLAMP);
            paint3.setShader(linearGradient5);
        }
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            updatePaint(left, top, right, bottom);
            invalidate();
        }
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        canvas.drawRect(0.0f, 0.0f, (float) getWidth(), (float) getHeight(), this.mShadowPaint);
    }

    public boolean hasOverlappingRendering() {
        return false;
    }
}
