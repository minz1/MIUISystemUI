package com.android.systemui.statusbar.policy;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import com.android.systemui.AssistManagerGoogle;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.assist.AssistManager;
import com.android.systemui.plugins.statusbar.phone.NavBarButtonProvider;
import java.util.ArrayList;

public class OpaLayout extends FrameLayout implements NavBarButtonProvider.ButtonInterface {
    private final ArrayList<View> mAnimatedViews = new ArrayList<>();
    /* access modifiers changed from: private */
    public int mAnimationState = 0;
    private View mBlue;
    private View mBottom;
    private final Runnable mCheckLongPress = new Runnable() {
        public void run() {
            if (OpaLayout.this.mIsPressed) {
                boolean unused = OpaLayout.this.mLongClicked = true;
            }
        }
    };
    private final Interpolator mCollapseInterpolator = Interpolators.FAST_OUT_LINEAR_IN;
    /* access modifiers changed from: private */
    public final ArraySet<Animator> mCurrentAnimators = new ArraySet<>();
    private final Interpolator mDiamondInterpolator = new PathInterpolator(0.2f, 0.0f, 0.2f, 1.0f);
    private final Interpolator mDotsFullSizeInterpolator = new PathInterpolator(0.4f, 0.0f, 0.0f, 1.0f);
    private final Interpolator mFastOutSlowInInterpolator = Interpolators.FAST_OUT_SLOW_IN;
    private View mGreen;
    private HomeButtonView mHalo;
    private KeyButtonView mHome;
    private final Interpolator mHomeDisappearInterpolator = new PathInterpolator(0.8f, 0.0f, 1.0f, 1.0f);
    /* access modifiers changed from: private */
    public boolean mIsPressed;
    private boolean mIsVertical;
    private View mLeft;
    /* access modifiers changed from: private */
    public boolean mLongClicked;
    private boolean mOpaEnabled;
    private View mRed;
    private final Runnable mRetract = new Runnable() {
        public void run() {
            OpaLayout.this.cancelCurrentAnimation();
            OpaLayout.this.startRetractAnimation();
        }
    };
    private final Interpolator mRetractInterpolator = new PathInterpolator(0.4f, 0.0f, 0.0f, 1.0f);
    private View mRight;
    private long mStartTime;
    private View mTop;
    private HomeButtonView mWhite;
    private View mYellow;

    public static class Interpolators {
        public static final Interpolator ACCELERATE = new AccelerateInterpolator();
        public static final Interpolator ACCELERATE_DECELERATE = new AccelerateDecelerateInterpolator();
        public static final Interpolator ALPHA_IN = new PathInterpolator(0.4f, 0.0f, 1.0f, 1.0f);
        public static final Interpolator ALPHA_OUT = new PathInterpolator(0.0f, 0.0f, 0.8f, 1.0f);
        public static final Interpolator CUSTOM_40_40 = new PathInterpolator(0.4f, 0.0f, 0.6f, 1.0f);
        public static final Interpolator DECELERATE_QUINT = new DecelerateInterpolator(2.5f);
        public static final Interpolator FAST_OUT_LINEAR_IN = new PathInterpolator(0.4f, 0.0f, 1.0f, 1.0f);
        public static final Interpolator FAST_OUT_SLOW_IN = new PathInterpolator(0.4f, 0.0f, 0.2f, 1.0f);
        public static final Interpolator ICON_OVERSHOT = new PathInterpolator(0.4f, 0.0f, 0.2f, 1.4f);
        public static final Interpolator LINEAR = new LinearInterpolator();
        public static final Interpolator LINEAR_OUT_SLOW_IN = new PathInterpolator(0.0f, 0.0f, 0.2f, 1.0f);
        public static final Interpolator TOUCH_RESPONSE = new PathInterpolator(0.3f, 0.0f, 0.1f, 1.0f);
    }

    public OpaLayout(Context context) {
        super(context);
    }

    public OpaLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public OpaLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public OpaLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mBlue = findViewById(R.id.blue);
        this.mRed = findViewById(R.id.red);
        this.mYellow = findViewById(R.id.yellow);
        this.mGreen = findViewById(R.id.green);
        this.mWhite = (HomeButtonView) findViewById(R.id.white);
        this.mHalo = (HomeButtonView) findViewById(R.id.halo);
        this.mHome = (KeyButtonView) findViewById(R.id.home);
        this.mHalo.setImageDrawable(getDrawable(getContext(), R.drawable.ic_sysbar_home_1, R.drawable.ic_sysbar_home_darkmode_1));
        this.mWhite.setImageDrawable(getDrawable(getContext(), R.drawable.ic_sysbar_home_2, R.drawable.ic_sysbar_home_darkmode_2));
        setVertical(false);
        this.mAnimatedViews.add(this.mBlue);
        this.mAnimatedViews.add(this.mRed);
        this.mAnimatedViews.add(this.mYellow);
        this.mAnimatedViews.add(this.mGreen);
        this.mAnimatedViews.add(this.mWhite);
        this.mAnimatedViews.add(this.mHalo);
        ((AssistManagerGoogle) Dependency.get(AssistManager.class)).dispatchOpaEnabledState();
    }

    public void setOnLongClickListener(View.OnLongClickListener l) {
        this.mHome.setOnLongClickListener(new View.OnLongClickListener(l) {
            private final /* synthetic */ View.OnLongClickListener f$1;

            {
                this.f$1 = r2;
            }

            public final boolean onLongClick(View view) {
                return this.f$1.onLongClick(OpaLayout.this.mHome);
            }
        });
    }

    public void setOnTouchListener(View.OnTouchListener l) {
        this.mHome.setOnTouchListener(l);
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!this.mOpaEnabled) {
            return false;
        }
        int action = ev.getAction();
        boolean z = true;
        if (action != 3) {
            switch (action) {
                case 0:
                    if (!this.mCurrentAnimators.isEmpty()) {
                        if (this.mAnimationState != 2) {
                            return false;
                        }
                        endCurrentAnimation();
                    }
                    this.mStartTime = SystemClock.elapsedRealtime();
                    this.mLongClicked = false;
                    this.mIsPressed = true;
                    startDiamondAnimation();
                    removeCallbacks(this.mCheckLongPress);
                    postDelayed(this.mCheckLongPress, (long) ViewConfiguration.getLongPressTimeout());
                    break;
                case 1:
                    break;
            }
        }
        if (this.mAnimationState == 1) {
            long targetTime = 100 - (SystemClock.elapsedRealtime() - this.mStartTime);
            removeCallbacks(this.mRetract);
            postDelayed(this.mRetract, targetTime);
            removeCallbacks(this.mCheckLongPress);
            return false;
        }
        if (!this.mIsPressed || this.mLongClicked) {
            z = false;
        }
        boolean doRetract = z;
        this.mIsPressed = false;
        if (doRetract) {
            this.mRetract.run();
        }
        return false;
    }

    private void startDiamondAnimation() {
        this.mCurrentAnimators.clear();
        this.mCurrentAnimators.addAll(getDiamondAnimatorSet());
        this.mAnimationState = 1;
        startAll(this.mCurrentAnimators);
    }

    /* access modifiers changed from: private */
    public void startRetractAnimation() {
        this.mCurrentAnimators.clear();
        this.mCurrentAnimators.addAll(getRetractAnimatorSet());
        this.mAnimationState = 2;
        startAll(this.mCurrentAnimators);
    }

    /* access modifiers changed from: private */
    public void startLineAnimation() {
        this.mCurrentAnimators.clear();
        this.mCurrentAnimators.addAll(getLineAnimatorSet());
        this.mAnimationState = 3;
        startAll(this.mCurrentAnimators);
    }

    /* access modifiers changed from: private */
    public void startCollapseAnimation() {
        this.mCurrentAnimators.clear();
        this.mCurrentAnimators.addAll(getCollapseAnimatorSet());
        this.mAnimationState = 3;
        startAll(this.mCurrentAnimators);
    }

    private Animator getScaleAnimatorX(View v, float factor, int duration, Interpolator interpolator) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(v, View.SCALE_X, new float[]{factor});
        animator.setInterpolator(interpolator);
        animator.setDuration((long) duration);
        return animator;
    }

    private Animator getScaleAnimatorY(View v, float factor, int duration, Interpolator interpolator) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(v, View.SCALE_Y, new float[]{factor});
        animator.setInterpolator(interpolator);
        animator.setDuration((long) duration);
        return animator;
    }

    private Animator getDeltaAnimatorX(View v, Interpolator interpolator, float deltaX, int duration) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(v, View.X, new float[]{v.getX() + deltaX});
        animator.setInterpolator(interpolator);
        animator.setDuration((long) duration);
        return animator;
    }

    private Animator getDeltaAnimatorY(View v, Interpolator interpolator, float deltaY, int duration) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(v, View.Y, new float[]{v.getY() + deltaY});
        animator.setInterpolator(interpolator);
        animator.setDuration((long) duration);
        return animator;
    }

    private Animator getTranslationAnimatorX(View v, Interpolator interpolator, int duration) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(v, View.TRANSLATION_X, new float[]{0.0f});
        animator.setInterpolator(interpolator);
        animator.setDuration((long) duration);
        return animator;
    }

    private Animator getTranslationAnimatorY(View v, Interpolator interpolator, int duration) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(v, View.TRANSLATION_Y, new float[]{0.0f});
        animator.setInterpolator(interpolator);
        animator.setDuration((long) duration);
        return animator;
    }

    private Animator getAlphaAnimator(View v, float alpha, int duration, Interpolator interpolator) {
        return getAlphaAnimator(v, alpha, duration, 0, interpolator);
    }

    private Animator getAlphaAnimator(View v, float alpha, int duration, int startDelay, Interpolator interpolator) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(v, View.ALPHA, new float[]{alpha});
        animator.setInterpolator(interpolator);
        animator.setDuration((long) duration);
        animator.setStartDelay((long) startDelay);
        return animator;
    }

    private void startAll(ArraySet<Animator> animators) {
        for (int i = animators.size() - 1; i >= 0; i--) {
            animators.valueAt(i).start();
        }
    }

    private float getPxVal(int id) {
        return (float) getResources().getDimensionPixelOffset(id);
    }

    private ArraySet<Animator> getDiamondAnimatorSet() {
        ArraySet<Animator> animators = new ArraySet<>();
        animators.add(getDeltaAnimatorY(this.mTop, this.mDiamondInterpolator, -getPxVal(R.dimen.opa_diamond_translation), 200));
        animators.add(getScaleAnimatorX(this.mTop, 0.8f, 200, this.mFastOutSlowInInterpolator));
        animators.add(getScaleAnimatorY(this.mTop, 0.8f, 200, this.mFastOutSlowInInterpolator));
        animators.add(getAlphaAnimator(this.mTop, 1.0f, 50, Interpolators.LINEAR));
        animators.add(getDeltaAnimatorY(this.mBottom, this.mDiamondInterpolator, getPxVal(R.dimen.opa_diamond_translation), 200));
        animators.add(getScaleAnimatorX(this.mBottom, 0.8f, 200, this.mFastOutSlowInInterpolator));
        animators.add(getScaleAnimatorY(this.mBottom, 0.8f, 200, this.mFastOutSlowInInterpolator));
        animators.add(getAlphaAnimator(this.mBottom, 1.0f, 50, Interpolators.LINEAR));
        animators.add(getDeltaAnimatorX(this.mLeft, this.mDiamondInterpolator, -getPxVal(R.dimen.opa_diamond_translation), 200));
        animators.add(getScaleAnimatorX(this.mLeft, 0.8f, 200, this.mFastOutSlowInInterpolator));
        animators.add(getScaleAnimatorY(this.mLeft, 0.8f, 200, this.mFastOutSlowInInterpolator));
        animators.add(getAlphaAnimator(this.mLeft, 1.0f, 50, Interpolators.LINEAR));
        animators.add(getDeltaAnimatorX(this.mRight, this.mDiamondInterpolator, getPxVal(R.dimen.opa_diamond_translation), 200));
        animators.add(getScaleAnimatorX(this.mRight, 0.8f, 200, this.mFastOutSlowInInterpolator));
        animators.add(getScaleAnimatorY(this.mRight, 0.8f, 200, this.mFastOutSlowInInterpolator));
        animators.add(getAlphaAnimator(this.mRight, 1.0f, 50, Interpolators.LINEAR));
        animators.add(getScaleAnimatorX(this.mWhite, 0.625f, 200, this.mFastOutSlowInInterpolator));
        animators.add(getScaleAnimatorY(this.mWhite, 0.625f, 200, this.mFastOutSlowInInterpolator));
        animators.add(getScaleAnimatorX(this.mHalo, 0.47619048f, 100, this.mFastOutSlowInInterpolator));
        animators.add(getScaleAnimatorY(this.mHalo, 0.47619048f, 100, this.mFastOutSlowInInterpolator));
        animators.add(getAlphaAnimator(this.mHalo, 0.0f, 100, this.mFastOutSlowInInterpolator));
        getLongestAnim(animators).addListener(new AnimatorListenerAdapter() {
            public void onAnimationCancel(Animator animation) {
                OpaLayout.this.mCurrentAnimators.clear();
            }

            public void onAnimationEnd(Animator animation) {
                OpaLayout.this.startLineAnimation();
            }
        });
        return animators;
    }

    private ArraySet<Animator> getRetractAnimatorSet() {
        ArraySet<Animator> animators = new ArraySet<>();
        animators.add(getTranslationAnimatorX(this.mRed, this.mRetractInterpolator, 300));
        animators.add(getTranslationAnimatorY(this.mRed, this.mRetractInterpolator, 300));
        animators.add(getScaleAnimatorX(this.mRed, 1.0f, 300, this.mRetractInterpolator));
        animators.add(getScaleAnimatorY(this.mRed, 1.0f, 300, this.mRetractInterpolator));
        animators.add(getAlphaAnimator(this.mRed, 0.0f, 50, 50, Interpolators.LINEAR));
        animators.add(getTranslationAnimatorX(this.mBlue, this.mRetractInterpolator, 300));
        animators.add(getTranslationAnimatorY(this.mBlue, this.mRetractInterpolator, 300));
        animators.add(getScaleAnimatorX(this.mBlue, 1.0f, 300, this.mRetractInterpolator));
        animators.add(getScaleAnimatorY(this.mBlue, 1.0f, 300, this.mRetractInterpolator));
        animators.add(getAlphaAnimator(this.mBlue, 0.0f, 50, 50, Interpolators.LINEAR));
        animators.add(getTranslationAnimatorX(this.mGreen, this.mRetractInterpolator, 300));
        animators.add(getTranslationAnimatorY(this.mGreen, this.mRetractInterpolator, 300));
        animators.add(getScaleAnimatorX(this.mGreen, 1.0f, 300, this.mRetractInterpolator));
        animators.add(getScaleAnimatorY(this.mGreen, 1.0f, 300, this.mRetractInterpolator));
        animators.add(getAlphaAnimator(this.mGreen, 0.0f, 50, 50, Interpolators.LINEAR));
        animators.add(getTranslationAnimatorX(this.mYellow, this.mRetractInterpolator, 300));
        animators.add(getTranslationAnimatorY(this.mYellow, this.mRetractInterpolator, 300));
        animators.add(getScaleAnimatorX(this.mYellow, 1.0f, 300, this.mRetractInterpolator));
        animators.add(getScaleAnimatorY(this.mYellow, 1.0f, 300, this.mRetractInterpolator));
        animators.add(getAlphaAnimator(this.mYellow, 0.0f, 50, 50, Interpolators.LINEAR));
        animators.add(getScaleAnimatorX(this.mWhite, 1.0f, 300, this.mRetractInterpolator));
        animators.add(getScaleAnimatorY(this.mWhite, 1.0f, 300, this.mRetractInterpolator));
        animators.add(getScaleAnimatorX(this.mHalo, 1.0f, 300, this.mFastOutSlowInInterpolator));
        animators.add(getScaleAnimatorY(this.mHalo, 1.0f, 300, this.mFastOutSlowInInterpolator));
        animators.add(getAlphaAnimator(this.mHalo, 1.0f, 300, this.mFastOutSlowInInterpolator));
        getLongestAnim(animators).addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                OpaLayout.this.mCurrentAnimators.clear();
                int unused = OpaLayout.this.mAnimationState = 0;
                OpaLayout.this.skipToStartingValue();
            }
        });
        return animators;
    }

    private ArraySet<Animator> getCollapseAnimatorSet() {
        Animator animator;
        Animator animator2;
        Animator animator3;
        Animator animator4;
        ArraySet<Animator> animators = new ArraySet<>();
        if (this.mIsVertical) {
            animator = getDeltaAnimatorY(this.mRed, this.mCollapseInterpolator, -getPxVal(R.dimen.opa_line_x_collapse_ry), 83);
        } else {
            animator = getDeltaAnimatorX(this.mRed, this.mCollapseInterpolator, getPxVal(R.dimen.opa_line_x_collapse_ry), 83);
        }
        animators.add(animator);
        animators.add(getScaleAnimatorX(this.mRed, 1.0f, 200, this.mDotsFullSizeInterpolator));
        animators.add(getScaleAnimatorY(this.mRed, 1.0f, 200, this.mDotsFullSizeInterpolator));
        animators.add(getAlphaAnimator(this.mRed, 0.0f, 50, 33, Interpolators.LINEAR));
        if (this.mIsVertical) {
            animator2 = getDeltaAnimatorY(this.mBlue, this.mCollapseInterpolator, -getPxVal(R.dimen.opa_line_x_collapse_bg), 100);
        } else {
            animator2 = getDeltaAnimatorX(this.mBlue, this.mCollapseInterpolator, getPxVal(R.dimen.opa_line_x_collapse_bg), 100);
        }
        animators.add(animator2);
        animators.add(getScaleAnimatorX(this.mBlue, 1.0f, 200, this.mDotsFullSizeInterpolator));
        animators.add(getScaleAnimatorY(this.mBlue, 1.0f, 200, this.mDotsFullSizeInterpolator));
        animators.add(getAlphaAnimator(this.mBlue, 0.0f, 50, 33, Interpolators.LINEAR));
        if (this.mIsVertical) {
            animator3 = getDeltaAnimatorY(this.mYellow, this.mCollapseInterpolator, getPxVal(R.dimen.opa_line_x_collapse_ry), 83);
        } else {
            animator3 = getDeltaAnimatorX(this.mYellow, this.mCollapseInterpolator, -getPxVal(R.dimen.opa_line_x_collapse_ry), 83);
        }
        animators.add(animator3);
        animators.add(getScaleAnimatorX(this.mYellow, 1.0f, 200, this.mDotsFullSizeInterpolator));
        animators.add(getScaleAnimatorY(this.mYellow, 1.0f, 200, this.mDotsFullSizeInterpolator));
        animators.add(getAlphaAnimator(this.mYellow, 0.0f, 50, 33, Interpolators.LINEAR));
        if (this.mIsVertical) {
            animator4 = getDeltaAnimatorY(this.mGreen, this.mCollapseInterpolator, getPxVal(R.dimen.opa_line_x_collapse_bg), 100);
        } else {
            animator4 = getDeltaAnimatorX(this.mGreen, this.mCollapseInterpolator, -getPxVal(R.dimen.opa_line_x_collapse_bg), 100);
        }
        animators.add(animator4);
        animators.add(getScaleAnimatorX(this.mGreen, 1.0f, 200, this.mDotsFullSizeInterpolator));
        animators.add(getScaleAnimatorY(this.mGreen, 1.0f, 200, this.mDotsFullSizeInterpolator));
        animators.add(getAlphaAnimator(this.mGreen, 0.0f, 50, 33, Interpolators.LINEAR));
        Animator homeScaleX = getScaleAnimatorX(this.mWhite, 1.0f, 150, this.mFastOutSlowInInterpolator);
        Animator homeScaleY = getScaleAnimatorY(this.mWhite, 1.0f, 150, this.mFastOutSlowInInterpolator);
        Animator haloScaleX = getScaleAnimatorX(this.mHalo, 1.0f, 150, this.mFastOutSlowInInterpolator);
        Animator haloScaleY = getScaleAnimatorY(this.mHalo, 1.0f, 150, this.mFastOutSlowInInterpolator);
        Animator haloAlpha = getAlphaAnimator(this.mHalo, 1.0f, 150, this.mFastOutSlowInInterpolator);
        homeScaleX.setStartDelay(33);
        homeScaleY.setStartDelay(33);
        haloScaleX.setStartDelay(33);
        haloScaleY.setStartDelay(33);
        haloAlpha.setStartDelay(33);
        animators.add(homeScaleX);
        animators.add(homeScaleY);
        animators.add(haloScaleX);
        animators.add(haloScaleY);
        animators.add(haloAlpha);
        getLongestAnim(animators).addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                OpaLayout.this.mCurrentAnimators.clear();
                int unused = OpaLayout.this.mAnimationState = 0;
            }
        });
        return animators;
    }

    private ArraySet<Animator> getLineAnimatorSet() {
        ArraySet<Animator> animators = new ArraySet<>();
        if (this.mIsVertical) {
            animators.add(getDeltaAnimatorY(this.mRed, this.mFastOutSlowInInterpolator, getPxVal(R.dimen.opa_line_x_trans_ry), 275));
            animators.add(getDeltaAnimatorX(this.mRed, this.mFastOutSlowInInterpolator, getPxVal(R.dimen.opa_line_y_translation), 133));
            animators.add(getDeltaAnimatorY(this.mBlue, this.mFastOutSlowInInterpolator, getPxVal(R.dimen.opa_line_x_trans_bg), 275));
            animators.add(getDeltaAnimatorY(this.mYellow, this.mFastOutSlowInInterpolator, -getPxVal(R.dimen.opa_line_x_trans_ry), 275));
            animators.add(getDeltaAnimatorX(this.mYellow, this.mFastOutSlowInInterpolator, -getPxVal(R.dimen.opa_line_y_translation), 133));
            animators.add(getDeltaAnimatorY(this.mGreen, this.mFastOutSlowInInterpolator, -getPxVal(R.dimen.opa_line_x_trans_bg), 275));
        } else {
            animators.add(getDeltaAnimatorX(this.mRed, this.mFastOutSlowInInterpolator, -getPxVal(R.dimen.opa_line_x_trans_ry), 275));
            animators.add(getDeltaAnimatorY(this.mRed, this.mFastOutSlowInInterpolator, getPxVal(R.dimen.opa_line_y_translation), 133));
            animators.add(getDeltaAnimatorX(this.mBlue, this.mFastOutSlowInInterpolator, -getPxVal(R.dimen.opa_line_x_trans_bg), 275));
            animators.add(getDeltaAnimatorX(this.mYellow, this.mFastOutSlowInInterpolator, getPxVal(R.dimen.opa_line_x_trans_ry), 275));
            animators.add(getDeltaAnimatorY(this.mYellow, this.mFastOutSlowInInterpolator, -getPxVal(R.dimen.opa_line_y_translation), 133));
            animators.add(getDeltaAnimatorX(this.mGreen, this.mFastOutSlowInInterpolator, getPxVal(R.dimen.opa_line_x_trans_bg), 275));
        }
        animators.add(getScaleAnimatorX(this.mWhite, 0.0f, 83, this.mHomeDisappearInterpolator));
        animators.add(getScaleAnimatorY(this.mWhite, 0.0f, 83, this.mHomeDisappearInterpolator));
        animators.add(getScaleAnimatorX(this.mHalo, 0.0f, 83, this.mHomeDisappearInterpolator));
        animators.add(getScaleAnimatorY(this.mHalo, 0.0f, 83, this.mHomeDisappearInterpolator));
        getLongestAnim(animators).addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                OpaLayout.this.startCollapseAnimation();
            }

            public void onAnimationCancel(Animator animation) {
                OpaLayout.this.mCurrentAnimators.clear();
            }
        });
        return animators;
    }

    public void setOpaEnabled(boolean enabled) {
        Log.i("OpaLayout", "Setting opa enabled to " + enabled);
        this.mOpaEnabled = enabled;
        int visibility = enabled ? 0 : 4;
        this.mBlue.setVisibility(visibility);
        this.mRed.setVisibility(visibility);
        this.mYellow.setVisibility(visibility);
        this.mGreen.setVisibility(visibility);
    }

    /* access modifiers changed from: private */
    public void cancelCurrentAnimation() {
        if (!this.mCurrentAnimators.isEmpty()) {
            for (int i = this.mCurrentAnimators.size() - 1; i >= 0; i--) {
                Animator a = this.mCurrentAnimators.valueAt(i);
                a.removeAllListeners();
                a.cancel();
            }
            this.mCurrentAnimators.clear();
            this.mAnimationState = 0;
        }
    }

    private void endCurrentAnimation() {
        if (!this.mCurrentAnimators.isEmpty()) {
            for (int i = this.mCurrentAnimators.size() - 1; i >= 0; i--) {
                Animator a = this.mCurrentAnimators.valueAt(i);
                a.removeAllListeners();
                a.end();
            }
            this.mCurrentAnimators.clear();
            this.mAnimationState = 0;
        }
    }

    private Animator getLongestAnim(ArraySet<Animator> animators) {
        long longestDuration = Long.MIN_VALUE;
        Animator longestAnim = null;
        for (int i = animators.size() - 1; i >= 0; i--) {
            Animator a = animators.valueAt(i);
            if (a.getTotalDuration() > longestDuration) {
                longestAnim = a;
                longestDuration = a.getTotalDuration();
            }
        }
        return longestAnim;
    }

    /* access modifiers changed from: private */
    public void skipToStartingValue() {
        int size = this.mAnimatedViews.size();
        for (int i = 0; i < size; i++) {
            View v = this.mAnimatedViews.get(i);
            v.setScaleY(1.0f);
            v.setScaleX(1.0f);
            v.setTranslationY(0.0f);
            v.setTranslationX(0.0f);
            v.setAlpha(0.0f);
        }
        this.mHalo.setAlpha(1.0f);
        this.mWhite.setAlpha(1.0f);
    }

    public void setVertical(boolean vertical) {
        this.mIsVertical = vertical;
        if (this.mIsVertical) {
            this.mTop = this.mGreen;
            this.mBottom = this.mBlue;
            this.mRight = this.mYellow;
            this.mLeft = this.mRed;
            return;
        }
        this.mTop = this.mRed;
        this.mBottom = this.mYellow;
        this.mLeft = this.mBlue;
        this.mRight = this.mGreen;
    }

    public void setImageDrawable(Drawable drawable) {
    }

    public void abortCurrentGesture() {
        this.mHome.abortCurrentGesture();
    }

    public void setCarMode(boolean carMode) {
    }

    public void setDarkIntensity(float intensity) {
        this.mHalo.setDarkIntensity(intensity);
        this.mWhite.setDarkIntensity(intensity);
    }

    private KeyButtonDrawable getDrawable(Context ctx, int lightIcon, int darkIcon) {
        return getDrawable(ctx, ctx, lightIcon, darkIcon);
    }

    private KeyButtonDrawable getDrawable(Context darkContext, Context lightContext, int lightIcon, int darkIcon) {
        return KeyButtonDrawable.create(lightContext.getDrawable(lightIcon), darkContext.getDrawable(darkIcon));
    }
}
