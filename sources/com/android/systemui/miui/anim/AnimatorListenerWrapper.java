package com.android.systemui.miui.anim;

import android.animation.Animator;
import android.animation.AnimatorListenerCompat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AnimatorListenerWrapper implements Animator.AnimatorListener {
    private List<Animator.AnimatorListener> mListeners = new ArrayList();

    private AnimatorListenerWrapper(Animator.AnimatorListener... listeners) {
        Collections.addAll(this.mListeners, listeners);
    }

    public void onAnimationStart(Animator animation, boolean isReverse) {
        for (Animator.AnimatorListener listener : this.mListeners) {
            AnimatorListenerCompat.onAnimationStart(listener, animation, isReverse);
        }
    }

    public void onAnimationEnd(Animator animation, boolean isReverse) {
        for (Animator.AnimatorListener listener : this.mListeners) {
            AnimatorListenerCompat.onAnimationEnd(listener, animation, isReverse);
        }
    }

    public void onAnimationStart(Animator animation) {
        for (Animator.AnimatorListener listener : this.mListeners) {
            listener.onAnimationStart(animation);
        }
    }

    public void onAnimationEnd(Animator animation) {
        for (Animator.AnimatorListener listener : this.mListeners) {
            listener.onAnimationEnd(animation);
        }
    }

    public void onAnimationCancel(Animator animation) {
        for (Animator.AnimatorListener listener : this.mListeners) {
            listener.onAnimationCancel(animation);
        }
    }

    public void onAnimationRepeat(Animator animation) {
        for (Animator.AnimatorListener listener : this.mListeners) {
            listener.onAnimationRepeat(animation);
        }
    }

    public static Animator.AnimatorListener of(Animator.AnimatorListener... listeners) {
        return new AnimatorListenerWrapper(listeners);
    }
}
