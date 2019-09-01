package com.android.systemui.miui;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawableCompat;
import android.os.Build;
import android.util.Log;
import android.view.animation.Interpolator;
import com.android.systemui.Interpolators;

public class DrawableAnimators {
    private static Interpolator DECELERATE = Interpolators.DECELERATE_QUART;

    private static class CornerRadiiTypeEvaluator implements TypeEvaluator<float[]> {
        private float[] mFallbackStartValue;
        private float[] mResult;

        CornerRadiiTypeEvaluator(Drawable target) {
            if (target instanceof GradientDrawable) {
                if (Build.VERSION.SDK_INT < 24) {
                    this.mFallbackStartValue = GradientDrawableCompat.getCornerRadii((GradientDrawable) target);
                    if (this.mFallbackStartValue != null) {
                        return;
                    }
                }
                this.mFallbackStartValue = getFallBackArray(target);
            }
        }

        private float[] getFallBackArray(Drawable target) {
            float radius = GradientDrawableCompat.getCornerRadius((GradientDrawable) target);
            return new float[]{radius, radius, radius, radius, radius, radius, radius, radius};
        }

        public float[] evaluate(float fraction, float[] startValue, float[] endValue) {
            if (startValue == null) {
                startValue = this.mFallbackStartValue;
            }
            if (this.mResult == null) {
                this.mResult = new float[startValue.length];
            }
            for (int i = 0; i < this.mResult.length; i++) {
                this.mResult[i] = startValue[i] + ((endValue[i] - startValue[i]) * fraction);
            }
            return this.mResult;
        }
    }

    public static Animator fade(Drawable drawable, boolean in) {
        int[] iArr = new int[1];
        iArr[0] = in ? 255 : 0;
        ObjectAnimator fade = ObjectAnimator.ofInt(drawable, "alpha", iArr);
        fade.setDuration(300);
        fade.setInterpolator(DECELERATE);
        fade.setAutoCancel(true);
        fade.start();
        return fade;
    }

    public static Animator updateCornerRadii(Context context, Drawable drawable, int toArrayRes) {
        TypedArray a = context.getResources().obtainTypedArray(toArrayRes);
        float[] array = new float[a.length()];
        for (int i = 0; i < array.length; i++) {
            array[i] = a.getDimension(i, 0.0f);
        }
        a.recycle();
        return updateCornerRadii(drawable, array);
    }

    public static Animator updateCornerRadii(Drawable drawable, float[] to) {
        if (!(drawable instanceof GradientDrawable)) {
            Log.e("DrawableAnimatorHelper", "cornerRadii change cannot be applied to " + drawable);
            return null;
        }
        ObjectAnimator radii = ObjectAnimator.ofObject(drawable, "cornerRadii", new CornerRadiiTypeEvaluator(drawable), new Object[]{to});
        radii.setDuration(300);
        radii.setInterpolator(DECELERATE);
        radii.setAutoCancel(true);
        radii.start();
        return radii;
    }
}
