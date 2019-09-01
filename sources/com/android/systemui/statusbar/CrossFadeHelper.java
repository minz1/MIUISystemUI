package com.android.systemui.statusbar;

import android.view.View;
import com.android.systemui.Interpolators;

public class CrossFadeHelper {
    public static void fadeOut(final View view, final Runnable endRunnable) {
        view.animate().cancel();
        view.animate().alpha(0.0f).setDuration(210).setInterpolator(Interpolators.ALPHA_OUT).withEndAction(new Runnable() {
            public void run() {
                if (endRunnable != null) {
                    endRunnable.run();
                }
                view.setVisibility(4);
            }
        });
        if (view.hasOverlappingRendering()) {
            view.animate().withLayer();
        }
    }

    public static void fadeOut(View view, float fadeOutAmount) {
        fadeOut(view, fadeOutAmount, true);
    }

    public static void fadeOut(View view, float fadeOutAmount, boolean remap) {
        view.animate().cancel();
        if (fadeOutAmount == 1.0f) {
            view.setVisibility(4);
        } else if (view.getVisibility() == 4) {
            view.setVisibility(0);
        }
        if (remap) {
            fadeOutAmount = mapToFadeDuration(fadeOutAmount);
        }
        float alpha = Interpolators.ALPHA_OUT.getInterpolation(1.0f - fadeOutAmount);
        view.setAlpha(alpha);
        updateLayerType(view, alpha);
    }

    private static float mapToFadeDuration(float fadeOutAmount) {
        return Math.min(fadeOutAmount / 0.5833333f, 1.0f);
    }

    private static void updateLayerType(View view, float alpha) {
        if (view.hasOverlappingRendering() && alpha > 0.0f && alpha < 1.0f) {
            view.setLayerType(2, null);
        } else if (view.getLayerType() == 2) {
            view.setLayerType(0, null);
        }
    }

    public static void fadeIn(View view) {
        view.animate().cancel();
        if (view.getVisibility() == 4) {
            view.setAlpha(0.0f);
            view.setVisibility(0);
        }
        view.animate().alpha(1.0f).setDuration(210).setInterpolator(Interpolators.ALPHA_IN).withEndAction(null);
        if (view.hasOverlappingRendering()) {
            view.animate().withLayer();
        }
    }

    public static void fadeIn(View view, float fadeInAmount) {
        fadeIn(view, fadeInAmount, true);
    }

    public static void fadeIn(View view, float fadeInAmount, boolean remap) {
        view.animate().cancel();
        if (view.getVisibility() == 4) {
            view.setVisibility(0);
        }
        if (remap) {
            fadeInAmount = mapToFadeDuration(fadeInAmount);
        }
        float alpha = Interpolators.ALPHA_IN.getInterpolation(fadeInAmount);
        view.setAlpha(alpha);
        updateLayerType(view, alpha);
    }
}
