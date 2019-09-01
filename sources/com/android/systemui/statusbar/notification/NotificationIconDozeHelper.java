package com.android.systemui.statusbar.notification;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import com.android.systemui.R;

public class NotificationIconDozeHelper extends NotificationDozeHelper {
    private int mColor = -16777216;
    private final PorterDuffColorFilter mImageColorFilter = new PorterDuffColorFilter(0, PorterDuff.Mode.SRC_ATOP);
    /* access modifiers changed from: private */
    public final int mImageDarkAlpha;
    private final int mImageDarkColor = -1;

    public NotificationIconDozeHelper(Context ctx) {
        this.mImageDarkAlpha = ctx.getResources().getInteger(R.integer.doze_small_icon_alpha);
    }

    public void setColor(int color) {
        this.mColor = color;
    }

    public void setImageDark(ImageView target, boolean dark, boolean fade, long delay, boolean useGrayscale) {
        if (fade) {
            if (!useGrayscale) {
                fadeImageColorFilter(target, dark, delay);
                fadeImageAlpha(target, dark, delay);
                return;
            }
            fadeGrayscale(target, dark, delay);
        } else if (!useGrayscale) {
            updateImageColorFilter(target, dark);
            updateImageAlpha(target, dark);
        } else {
            updateGrayscale(target, dark);
        }
    }

    private void fadeImageColorFilter(final ImageView target, boolean dark, long delay) {
        startIntensityAnimation(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                NotificationIconDozeHelper.this.updateImageColorFilter(target, ((Float) valueAnimator.getAnimatedValue()).floatValue());
            }
        }, dark, delay, null);
    }

    private void fadeImageAlpha(final ImageView target, boolean dark, long delay) {
        startIntensityAnimation(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float t = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                target.setImageAlpha((int) ((255.0f * (1.0f - t)) + (((float) NotificationIconDozeHelper.this.mImageDarkAlpha) * t)));
            }
        }, dark, delay, null);
    }

    private void updateImageColorFilter(ImageView target, boolean dark) {
        updateImageColorFilter(target, dark ? 1.0f : 0.0f);
    }

    /* access modifiers changed from: private */
    public void updateImageColorFilter(ImageView target, float intensity) {
        this.mImageColorFilter.setColor(NotificationUtils.interpolateColors(this.mColor, -1, intensity));
        Drawable imageDrawable = target.getDrawable();
        if (imageDrawable != null) {
            Drawable d = imageDrawable.mutate();
            d.setColorFilter(null);
            d.setColorFilter(this.mImageColorFilter);
        }
    }

    private void updateImageAlpha(ImageView target, boolean dark) {
        target.setImageAlpha(dark ? this.mImageDarkAlpha : 255);
    }
}
