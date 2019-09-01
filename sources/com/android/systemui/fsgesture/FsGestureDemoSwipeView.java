package com.android.systemui.fsgesture;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import com.android.systemui.R;

public class FsGestureDemoSwipeView extends FrameLayout {
    AnimatorSet finalAnimatorSet;
    ObjectAnimator hidingAnimator;
    /* access modifiers changed from: private */
    public int mDisplayHeight;
    /* access modifiers changed from: private */
    public int mDisplayWidth;
    private float mFinalTranslate;
    ObjectAnimator movingAnimator;
    ObjectAnimator scaleAnimator;
    ObjectAnimator showingAnimator;

    public FsGestureDemoSwipeView(Context context) {
        this(context, null);
    }

    public FsGestureDemoSwipeView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FsGestureDemoSwipeView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public FsGestureDemoSwipeView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.fs_gesture_swipe_view, this);
        setAlpha(0.0f);
        DisplayMetrics dm = new DisplayMetrics();
        ((WindowManager) getContext().getSystemService("window")).getDefaultDisplay().getRealMetrics(dm);
        this.mDisplayWidth = dm.widthPixels;
        this.mDisplayHeight = dm.heightPixels;
        this.mFinalTranslate = getResources().getDimension(R.dimen.fsgesture_swipe_final_translateX);
    }

    /* access modifiers changed from: package-private */
    public void prepare(int status) {
        setAlpha(0.0f);
        setVisibility(0);
        switch (status) {
            case 0:
                setTranslationY(getResources().getDimension(R.dimen.fsgesture_swipe_translateY));
                setTranslationX((float) ((-getWidth()) / 2));
                return;
            case 1:
                setTranslationY(getResources().getDimension(R.dimen.fsgesture_swipe_translateY));
                setTranslationX((float) (this.mDisplayWidth - (getWidth() / 2)));
                return;
            case 2:
            case 4:
                setTranslationX((float) ((this.mDisplayWidth / 2) - (getLeft() + (getWidth() / 2))));
                setTranslationY((float) (this.mDisplayHeight - (getHeight() / 2)));
                return;
            case 3:
                setTranslationY(getResources().getDimension(R.dimen.fsgesture_swipe_drawer_translateY));
                setTranslationX((float) ((-getWidth()) / 2));
                return;
            default:
                return;
        }
    }

    /* access modifiers changed from: package-private */
    public void startAnimation(int status) {
        createShowingAnimator(status);
        createMovingAnimator(status);
        createScaleAnimator(status);
        createHidingAnimator(status);
        createFinalAnimSet(status);
        this.finalAnimatorSet.start();
    }

    private void createFinalAnimSet(final int status) {
        if (this.finalAnimatorSet == null) {
            this.finalAnimatorSet = new AnimatorSet();
            if (status != 4) {
                this.finalAnimatorSet.playSequentially(new Animator[]{this.showingAnimator, this.movingAnimator, this.hidingAnimator});
            } else {
                this.finalAnimatorSet.playSequentially(new Animator[]{this.showingAnimator, this.movingAnimator, this.scaleAnimator, this.hidingAnimator});
            }
            this.finalAnimatorSet.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    int i = status;
                    if (i != 3) {
                        switch (i) {
                            case 0:
                                break;
                            case 1:
                                FsGestureDemoSwipeView.this.setTranslationX((float) (FsGestureDemoSwipeView.this.mDisplayWidth - (FsGestureDemoSwipeView.this.getWidth() / 2)));
                                break;
                            default:
                                FsGestureDemoSwipeView.this.setTranslationY((float) (FsGestureDemoSwipeView.this.mDisplayHeight - (FsGestureDemoSwipeView.this.getHeight() / 2)));
                                break;
                        }
                    }
                    FsGestureDemoSwipeView.this.setTranslationX((float) ((-FsGestureDemoSwipeView.this.getWidth()) / 2));
                    FsGestureDemoSwipeView.this.finalAnimatorSet.setStartDelay(1500);
                    FsGestureDemoSwipeView.this.finalAnimatorSet.start();
                }
            });
        }
    }

    private void createScaleAnimator(int status) {
        if (this.scaleAnimator == null) {
            this.scaleAnimator = ObjectAnimator.ofPropertyValuesHolder(this, new PropertyValuesHolder[]{PropertyValuesHolder.ofFloat("scaleX", new float[]{1.0f, 1.2f}), PropertyValuesHolder.ofFloat("scaleY", new float[]{1.0f, 1.2f})});
            this.scaleAnimator.setDuration(1000);
        }
    }

    private void createShowingAnimator(int status) {
        if (this.showingAnimator == null) {
            this.showingAnimator = ObjectAnimator.ofPropertyValuesHolder(this, new PropertyValuesHolder[]{PropertyValuesHolder.ofFloat("scaleX", new float[]{1.2f, 1.0f}), PropertyValuesHolder.ofFloat("scaleY", new float[]{1.2f, 1.0f}), PropertyValuesHolder.ofFloat("alpha", new float[]{0.0f, 1.0f})});
            this.showingAnimator.setDuration(200);
            this.showingAnimator.setStartDelay(300);
        }
    }

    private void createHidingAnimator(int status) {
        if (this.hidingAnimator == null) {
            if (status != 4) {
                this.hidingAnimator = ObjectAnimator.ofFloat(this, "alpha", new float[]{1.0f, 0.0f});
                this.hidingAnimator.setDuration(300);
            } else {
                this.hidingAnimator = ObjectAnimator.ofPropertyValuesHolder(this, new PropertyValuesHolder[]{PropertyValuesHolder.ofFloat("scaleX", new float[]{1.2f, 1.5f}), PropertyValuesHolder.ofFloat("scaleY", new float[]{1.2f, 1.5f}), PropertyValuesHolder.ofFloat("alpha", new float[]{1.0f, 0.0f})});
                this.hidingAnimator.setDuration(100);
            }
        }
    }

    private void createMovingAnimator(int status) {
        if (this.movingAnimator == null) {
            if (status != 3) {
                switch (status) {
                    case 0:
                        break;
                    case 1:
                        this.movingAnimator = ObjectAnimator.ofFloat(this, "translationX", new float[]{(float) (this.mDisplayWidth - (getWidth() / 2)), ((float) this.mDisplayWidth) - this.mFinalTranslate});
                        break;
                    default:
                        this.movingAnimator = ObjectAnimator.ofFloat(this, "translationY", new float[]{(float) (this.mDisplayHeight - (getHeight() / 2)), (float) (this.mDisplayHeight - 1000)});
                        break;
                }
            }
            this.movingAnimator = ObjectAnimator.ofFloat(this, "translationX", new float[]{(float) ((-getWidth()) / 2), this.mFinalTranslate - ((float) (getWidth() / 2))});
            this.movingAnimator.setInterpolator(new DecelerateInterpolator(1.5f));
            this.movingAnimator.setStartDelay(1000);
            this.movingAnimator.setDuration(500);
        }
    }

    /* access modifiers changed from: package-private */
    public void cancelAnimation() {
        setVisibility(8);
        if (this.finalAnimatorSet != null) {
            this.finalAnimatorSet.cancel();
            this.finalAnimatorSet.removeAllListeners();
            this.finalAnimatorSet = null;
        }
    }
}
