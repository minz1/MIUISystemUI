package com.android.keyguard;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.android.systemui.R;
import miui.view.animation.SineEaseInInterpolator;
import miui.view.animation.SineEaseInOutInterpolator;
import miui.view.animation.SineEaseOutInterpolator;

public class KeyguardBouncerMessageView extends RelativeLayout {
    private final int MAX_SHAKE_TIMES;
    private TextView mContent;
    private Resources mResources;
    private int mShakeDistance;
    private int mShakeDuration;
    /* access modifiers changed from: private */
    public int mShakeTimes;
    private TextView mTitle;

    public KeyguardBouncerMessageView(Context context) {
        this(context, null);
    }

    public KeyguardBouncerMessageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mShakeDuration = 25;
        this.MAX_SHAKE_TIMES = 2;
        this.mResources = getResources();
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mTitle = (TextView) findViewById(R.id.secure_keyguard_bouncer_message_title);
        this.mContent = (TextView) findViewById(R.id.secure_keyguard_bouncer_message_content);
        resetAnimValue();
    }

    public void showMessage(int titleResId, int messageResId) {
        if (getVisibility() != 8) {
            this.mTitle.setText(titleResId == 0 ? "" : this.mResources.getString(titleResId));
            this.mContent.setText(messageResId == 0 ? "" : this.mResources.getString(messageResId));
        }
    }

    public void showMessage(String title, String message) {
        if (getVisibility() != 8 && (!TextUtils.isEmpty(title) || !TextUtils.isEmpty(message))) {
            this.mTitle.setText(title);
            this.mContent.setText(message);
        }
    }

    public void showMessage(String title, String message, int color) {
        if (getVisibility() != 8 && (!TextUtils.isEmpty(title) || !TextUtils.isEmpty(message))) {
            this.mTitle.setText(title);
            this.mContent.setText(message);
            this.mContent.setTextColor(color);
        }
    }

    public void applyHintAnimation(long offset) {
        if (getVisibility() != 8 && !TextUtils.isEmpty(this.mContent.getText())) {
            this.mShakeTimes++;
            this.mShakeDistance -= this.mShakeDistance / 2;
            float x = this.mContent.getX();
            ObjectAnimator anim1 = ObjectAnimator.ofFloat(this.mContent, "X", new float[]{x, ((float) this.mShakeDistance) + x});
            anim1.setInterpolator(new SineEaseOutInterpolator());
            anim1.setDuration((long) this.mShakeDuration);
            ObjectAnimator anim2 = ObjectAnimator.ofFloat(this.mContent, "X", new float[]{((float) this.mShakeDistance) + x, x - ((float) this.mShakeDistance)});
            anim2.setInterpolator(new SineEaseInOutInterpolator());
            anim2.setDuration((long) (this.mShakeDuration * 2));
            ObjectAnimator anim3 = ObjectAnimator.ofFloat(this.mContent, "X", new float[]{x - ((float) this.mShakeDistance), x});
            anim3.setInterpolator(this.mShakeTimes == 2 ? new SineEaseOutInterpolator() : new SineEaseInInterpolator());
            anim3.setDuration((long) this.mShakeDuration);
            AnimatorSet animatiorSet = new AnimatorSet();
            animatiorSet.playSequentially(new Animator[]{anim1, anim2, anim3});
            animatiorSet.addListener(new Animator.AnimatorListener() {
                public void onAnimationStart(Animator animation) {
                }

                public void onAnimationRepeat(Animator animation) {
                }

                public void onAnimationEnd(Animator animation) {
                    if (KeyguardBouncerMessageView.this.mShakeTimes > 2) {
                        KeyguardBouncerMessageView.this.resetAnimValue();
                    } else {
                        KeyguardBouncerMessageView.this.applyHintAnimation(0);
                    }
                }

                public void onAnimationCancel(Animator animation) {
                }
            });
            animatiorSet.setStartDelay(offset);
            animatiorSet.start();
        }
    }

    public void resetAnimValue() {
        this.mShakeTimes = 0;
        this.mShakeDistance = this.mContext.getResources().getDimensionPixelSize(R.dimen.miui_common_unlock_screen_tip_shake_distance);
    }
}
