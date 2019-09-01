package com.android.systemui.qs;

import android.animation.Animator;
import android.view.View;
import android.view.animation.Interpolator;
import com.android.systemui.Interpolators;
import com.android.systemui.miui.anim.HideBeforeAnimatorListener;
import com.android.systemui.miui.anim.ShowBeforeAnimatorListener;

public class QSAnimation {
    public static final Interpolator INTERPOLATOR = Interpolators.CUBIC_EASE_IN_OUT;

    public static class QsHideBeforeAnimatorListener extends HideBeforeAnimatorListener {
        public QsHideBeforeAnimatorListener(View... views) {
            super(views);
            animateAlpha(true);
            setAlphaDuration(400);
            setAlphaInterpolator(QSAnimation.INTERPOLATOR);
        }
    }

    public static class QsShowBeforeAnimatorListener extends ShowBeforeAnimatorListener {
        public QsShowBeforeAnimatorListener(View... views) {
            super(views);
            animateAlpha(true);
            setAlphaDuration(400);
            setAlphaInterpolator(QSAnimation.INTERPOLATOR);
        }

        public void onAnimationEnd(Animator animation) {
            super.onAnimationEnd(animation);
            for (View v : this.mViews) {
                v.animate().cancel();
                v.setAlpha(1.0f);
            }
        }
    }
}
