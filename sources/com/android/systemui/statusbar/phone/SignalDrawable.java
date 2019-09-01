package com.android.systemui.statusbar.phone;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.os.Handler;

public class SignalDrawable extends Drawable {
    private static final float[] FIT = {2.26f, -3.02f, 1.76f};
    private static final float INV_TAN = (1.0f / ((float) Math.tan(0.39269908169872414d)));
    private static float[][] X_PATH = {new float[]{0.91249996f, 0.7083333f}, new float[]{-0.045833334f, -0.045833334f}, new float[]{-0.079166666f, 0.079166666f}, new float[]{-0.079166666f, -0.079166666f}, new float[]{-0.045833334f, 0.045833334f}, new float[]{0.079166666f, 0.079166666f}, new float[]{-0.079166666f, 0.079166666f}, new float[]{0.045833334f, 0.045833334f}, new float[]{0.079166666f, -0.079166666f}, new float[]{0.079166666f, 0.079166666f}, new float[]{0.045833334f, -0.045833334f}, new float[]{-0.079166666f, -0.079166666f}};
    private boolean mAnimating;
    private final Runnable mChangeDot;
    private int mCurrentDot;
    private final Path mCutPath;
    private final Paint mForegroundPaint;
    private final Path mForegroundPath;
    private final Path mFullPath;
    private final Handler mHandler;
    private int mIntrinsicSize;
    private int mLevel;
    private float mNumLevels;
    private final Paint mPaint;
    private final SlashArtist mSlash;
    private int mState;
    private boolean mVisible;
    private final Path mXPath;

    private final class SlashArtist {
        private final Path mPath;
        private final RectF mSlashRect;

        /* access modifiers changed from: package-private */
        public void draw(int height, int width, Canvas canvas, Paint paint) {
            Matrix m = new Matrix();
            updateRect(scale(0.40544835f, width), scale(0.20288496f, height), scale(0.4820516f, width), scale(1.1195517f, height));
            this.mPath.reset();
            this.mPath.addRect(this.mSlashRect, Path.Direction.CW);
            m.setRotate(-45.0f, (float) (width / 2), (float) (height / 2));
            this.mPath.transform(m);
            canvas.drawPath(this.mPath, paint);
            m.setRotate(45.0f, (float) (width / 2), (float) (height / 2));
            this.mPath.transform(m);
            m.setTranslate(this.mSlashRect.width(), 0.0f);
            this.mPath.transform(m);
            this.mPath.addRect(this.mSlashRect, Path.Direction.CW);
            m.setRotate(-45.0f, (float) (width / 2), (float) (height / 2));
            this.mPath.transform(m);
            canvas.clipPath(this.mPath, Region.Op.DIFFERENCE);
        }

        /* access modifiers changed from: package-private */
        public void updateRect(float left, float top, float right, float bottom) {
            this.mSlashRect.left = left;
            this.mSlashRect.top = top;
            this.mSlashRect.right = right;
            this.mSlashRect.bottom = bottom;
        }

        private float scale(float frac, int width) {
            return ((float) width) * frac;
        }
    }

    public int getIntrinsicWidth() {
        return this.mIntrinsicSize;
    }

    public int getIntrinsicHeight() {
        return this.mIntrinsicSize;
    }

    public void setNumLevels(int levels) {
        if (((float) levels) != this.mNumLevels) {
            this.mNumLevels = (float) levels;
            invalidateSelf();
        }
    }

    private void setSignalState(int state) {
        if (state != this.mState) {
            this.mState = state;
            updateAnimation();
            invalidateSelf();
        }
    }

    private void updateAnimation() {
        boolean shouldAnimate = this.mState == 3 && this.mVisible;
        if (shouldAnimate != this.mAnimating) {
            this.mAnimating = shouldAnimate;
            if (shouldAnimate) {
                this.mChangeDot.run();
            } else {
                this.mHandler.removeCallbacks(this.mChangeDot);
            }
        }
    }

    /* access modifiers changed from: protected */
    public boolean onLevelChange(int state) {
        setNumLevels(getNumLevels(state));
        setSignalState(getState(state));
        int level = getLevel(state);
        if (level != this.mLevel) {
            this.mLevel = level;
            invalidateSelf();
        }
        return true;
    }

    /* access modifiers changed from: protected */
    public void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        invalidateSelf();
    }

    public void draw(Canvas canvas) {
        float padding;
        int i;
        int i2;
        Canvas canvas2 = canvas;
        boolean isRtl = getLayoutDirection() == 1;
        if (isRtl) {
            canvas.save();
            canvas2.translate((float) canvas.getWidth(), 0.0f);
            canvas2.scale(-1.0f, 1.0f);
        }
        this.mFullPath.reset();
        this.mFullPath.setFillType(Path.FillType.WINDING);
        float width = (float) getBounds().width();
        float height = (float) getBounds().height();
        float padding2 = (float) Math.round(0.083333336f * width);
        this.mFullPath.moveTo(width - padding2, height - padding2);
        this.mFullPath.lineTo(width - padding2, padding2);
        this.mFullPath.lineTo(padding2, height - padding2);
        this.mFullPath.lineTo(width - padding2, height - padding2);
        if (this.mState == 3) {
            float cutWidth = 0.5833334f * width;
            float cutHeight = 0.16666667f * width;
            float dotSize = 0.125f * height;
            this.mFullPath.moveTo(width - padding2, height - padding2);
            this.mFullPath.rLineTo(-cutWidth, 0.0f);
            this.mFullPath.rLineTo(0.0f, -cutHeight);
            this.mFullPath.rLineTo(cutWidth, 0.0f);
            this.mFullPath.rLineTo(0.0f, cutHeight);
            float dotSpacing = (0.041666668f * height * 2.0f) + dotSize;
            float x = (width - padding2) - dotSize;
            this.mForegroundPath.reset();
            float f = cutHeight;
            float f2 = cutWidth;
            float f3 = (height - padding2) - dotSize;
            i2 = 3;
            i = 2;
            float f4 = dotSize;
            padding = padding2;
            drawDot(this.mFullPath, this.mForegroundPath, x, f3, f4, 2);
            drawDot(this.mFullPath, this.mForegroundPath, x - dotSpacing, f3, f4, 1);
            drawDot(this.mFullPath, this.mForegroundPath, x - (dotSpacing * 2.0f), f3, f4, 0);
        } else {
            i2 = 3;
            padding = padding2;
            i = 2;
            if (this.mState == 2) {
                float cut = 0.32916668f * width;
                this.mFullPath.moveTo(width - padding, height - padding);
                this.mFullPath.rLineTo(-cut, 0.0f);
                this.mFullPath.rLineTo(0.0f, -cut);
                this.mFullPath.rLineTo(cut, 0.0f);
                this.mFullPath.rLineTo(0.0f, cut);
            }
        }
        if (this.mState == 1) {
            float cutWidth2 = 0.083333336f * height;
            float cutDiagInset = INV_TAN * cutWidth2;
            this.mCutPath.reset();
            this.mCutPath.setFillType(Path.FillType.WINDING);
            this.mCutPath.moveTo((width - padding) - cutWidth2, (height - padding) - cutWidth2);
            this.mCutPath.lineTo((width - padding) - cutWidth2, padding + cutDiagInset);
            this.mCutPath.lineTo(padding + cutDiagInset, (height - padding) - cutWidth2);
            this.mCutPath.lineTo((width - padding) - cutWidth2, (height - padding) - cutWidth2);
            this.mForegroundPath.reset();
            this.mFullPath.op(this.mCutPath, Path.Op.DIFFERENCE);
        } else if (this.mState == 4) {
            this.mForegroundPath.set(this.mFullPath);
            this.mFullPath.reset();
            this.mSlash.draw((int) height, (int) width, canvas2, this.mForegroundPaint);
        } else if (this.mState != i2) {
            this.mForegroundPath.reset();
            this.mForegroundPath.addRect(padding, padding, padding + ((float) Math.round(calcFit(((float) this.mLevel) / (this.mNumLevels - 1.0f)) * (width - (2.0f * padding)))), height - padding, Path.Direction.CW);
            this.mForegroundPath.op(this.mFullPath, Path.Op.INTERSECT);
        }
        canvas2.drawPath(this.mFullPath, this.mPaint);
        canvas2.drawPath(this.mForegroundPath, this.mForegroundPaint);
        if (this.mState == i) {
            this.mXPath.reset();
            this.mXPath.moveTo(X_PATH[0][0] * width, X_PATH[0][1] * height);
            for (int i3 = 1; i3 < X_PATH.length; i3++) {
                this.mXPath.rLineTo(X_PATH[i3][0] * width, X_PATH[i3][1] * height);
            }
            canvas2.drawPath(this.mXPath, this.mForegroundPaint);
        }
        if (isRtl) {
            canvas.restore();
        }
    }

    private void drawDot(Path fullPath, Path foregroundPath, float x, float y, float dotSize, int i) {
        (i == this.mCurrentDot ? foregroundPath : fullPath).addRect(x, y, x + dotSize, y + dotSize, Path.Direction.CW);
    }

    private float calcFit(float v) {
        float ret = 0.0f;
        float t = v;
        for (float f : FIT) {
            ret += f * t;
            t *= v;
        }
        return ret;
    }

    public int getAlpha() {
        return this.mPaint.getAlpha();
    }

    public void setAlpha(int alpha) {
        this.mPaint.setAlpha(alpha);
        this.mForegroundPaint.setAlpha(alpha);
    }

    public void setColorFilter(ColorFilter colorFilter) {
        this.mPaint.setColorFilter(colorFilter);
        this.mForegroundPaint.setColorFilter(colorFilter);
    }

    public int getOpacity() {
        return 255;
    }

    public boolean setVisible(boolean visible, boolean restart) {
        this.mVisible = visible;
        updateAnimation();
        return super.setVisible(visible, restart);
    }

    public static int getLevel(int fullState) {
        return fullState & 255;
    }

    public static int getState(int fullState) {
        return (16711680 & fullState) >> 16;
    }

    public static int getNumLevels(int fullState) {
        return (65280 & fullState) >> 8;
    }

    public static int getState(int level, int numLevels, boolean cutOut) {
        return ((cutOut ? 2 : 0) << 16) | (numLevels << 8) | level;
    }

    public static int getCarrierChangeState(int numLevels) {
        return (numLevels << 8) | 196608;
    }

    public static int getEmptyState(int numLevels) {
        return (numLevels << 8) | 65536;
    }

    public static int getAirplaneModeState(int numLevels) {
        return (numLevels << 8) | 262144;
    }
}
