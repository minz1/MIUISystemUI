package com.android.settingslib.animation;

import android.content.Context;
import android.view.animation.Interpolator;
import com.android.settingslib.animation.AppearAnimationUtils;

public class DisappearAnimationUtils extends AppearAnimationUtils {
    private static final AppearAnimationUtils.RowTranslationScaler ROW_TRANSLATION_SCALER = new AppearAnimationUtils.RowTranslationScaler() {
        public float getRowTranslationScale(int row, int numRows) {
            return (float) (Math.pow((double) (numRows - row), 2.0d) / ((double) numRows));
        }
    };

    public DisappearAnimationUtils(Context ctx, long duration, float translationScaleFactor, float delayScaleFactor, Interpolator interpolator) {
        this(ctx, duration, translationScaleFactor, delayScaleFactor, interpolator, ROW_TRANSLATION_SCALER);
    }

    public DisappearAnimationUtils(Context ctx, long duration, float translationScaleFactor, float delayScaleFactor, Interpolator interpolator, AppearAnimationUtils.RowTranslationScaler rowScaler) {
        super(ctx, duration, translationScaleFactor, delayScaleFactor, interpolator);
        this.mRowTranslationScaler = rowScaler;
        this.mAppearing = false;
    }

    /* access modifiers changed from: protected */
    public long calculateDelay(int row, int col) {
        return (long) ((((double) (row * 60)) + (((double) col) * (Math.pow((double) row, 0.4d) + 0.4d) * 10.0d)) * ((double) this.mDelayScale));
    }
}
