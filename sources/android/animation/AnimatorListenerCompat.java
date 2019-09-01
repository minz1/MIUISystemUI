package android.animation;

import android.animation.Animator;

public class AnimatorListenerCompat {
    public static void onAnimationStart(Animator.AnimatorListener listener, Animator animation, boolean isReverse) {
        listener.onAnimationStart(animation, isReverse);
    }

    public static void onAnimationEnd(Animator.AnimatorListener listener, Animator animation, boolean isReverse) {
        listener.onAnimationEnd(animation, isReverse);
    }
}
