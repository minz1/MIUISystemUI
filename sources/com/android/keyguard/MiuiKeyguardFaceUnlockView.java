package com.android.keyguard;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.AnimationDrawable;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.android.keyguard.Ease;
import com.android.keyguard.wallpaper.WallpaperAuthorityUtils;
import com.android.systemui.Constants;
import com.android.systemui.R;

public class MiuiKeyguardFaceUnlockView extends LinearLayout {
    private Handler mAnimationHandler;
    private Context mContext;
    private final Runnable mDelayedHide;
    /* access modifiers changed from: private */
    public boolean mFaceUnlockAnimationRuning;
    private View.OnClickListener mFaceUnlockClickListener;
    /* access modifiers changed from: private */
    public boolean mFaceUnlockDetectRuning;
    /* access modifiers changed from: private */
    public boolean mFaceUnlockHasDetectFace;
    /* access modifiers changed from: private */
    public boolean mFaceUnlockHasFailed;
    private KeyguardUpdateMonitorCallback mFaceUnlockInfoCallback;
    private boolean mIsKeyguardFaceUnlockView;
    /* access modifiers changed from: private */
    public boolean mKeyguardVisibility;
    /* access modifiers changed from: private */
    public PowerManager mPowerManager;
    /* access modifiers changed from: private */
    public boolean mShowBouncer;
    /* access modifiers changed from: private */
    public KeyguardUpdateMonitor mUpdateMonitor;

    public MiuiKeyguardFaceUnlockView(Context context) {
        this(context, null);
    }

    public MiuiKeyguardFaceUnlockView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mAnimationHandler = new Handler();
        this.mFaceUnlockInfoCallback = new KeyguardUpdateMonitorCallback() {
            public void onFaceStart() {
                boolean unused = MiuiKeyguardFaceUnlockView.this.mFaceUnlockHasFailed = false;
                boolean unused2 = MiuiKeyguardFaceUnlockView.this.mFaceUnlockDetectRuning = true;
                boolean unused3 = MiuiKeyguardFaceUnlockView.this.mFaceUnlockHasDetectFace = false;
                MiuiKeyguardFaceUnlockView.this.setVisibility(4);
            }

            public void onFaceHelp(int helpStringId) {
                if (MiuiKeyguardFaceUnlockView.this.mFaceUnlockDetectRuning) {
                    int i = 4;
                    if (MiuiKeyguardFaceUnlockView.this.shouldFaceUnlockViewExecuteAnimation()) {
                        MiuiKeyguardFaceUnlockView miuiKeyguardFaceUnlockView = MiuiKeyguardFaceUnlockView.this;
                        if (MiuiKeyguardFaceUnlockView.this.mKeyguardVisibility) {
                            i = 0;
                        }
                        miuiKeyguardFaceUnlockView.setVisibility(i);
                        MiuiKeyguardFaceUnlockView.this.updateFaceUnlockImgVisibleInLiftCamera();
                        if (helpStringId != R.string.face_unlock_not_found) {
                            MiuiKeyguardFaceUnlockView.this.startFaceUnlockAnimation();
                        } else {
                            MiuiKeyguardFaceUnlockView.this.updateFaceUnlockViewImage();
                        }
                    } else {
                        MiuiKeyguardFaceUnlockView.this.setVisibility(4);
                    }
                }
            }

            public void onFaceAuthenticated() {
                MiuiKeyguardFaceUnlockView.this.stopShakeHeadAnimation();
                if (MiuiKeyguardFaceUnlockView.this.shouldFaceUnlockViewExecuteAnimation() && MiuiKeyguardFaceUnlockView.this.mUpdateMonitor.isStayScreenFaceUnlockSuccess() && !MiuiKeyguardFaceUnlockView.this.mShowBouncer) {
                    MiuiKeyguardFaceUnlockView.this.startFaceUnlockSuccessAnimation();
                }
            }

            public void onFaceAuthFailed(boolean hasFace) {
                boolean unused = MiuiKeyguardFaceUnlockView.this.mFaceUnlockHasDetectFace = hasFace;
                boolean unused2 = MiuiKeyguardFaceUnlockView.this.mFaceUnlockHasFailed = true;
                int i = 4;
                if (MiuiKeyguardFaceUnlockView.this.shouldFaceUnlockViewExecuteAnimation()) {
                    MiuiKeyguardFaceUnlockView miuiKeyguardFaceUnlockView = MiuiKeyguardFaceUnlockView.this;
                    if (MiuiKeyguardFaceUnlockView.this.mShowBouncer && hasFace && MiuiKeyguardFaceUnlockView.this.mKeyguardVisibility) {
                        i = 0;
                    }
                    miuiKeyguardFaceUnlockView.setVisibility(i);
                    MiuiKeyguardFaceUnlockView.this.updateFaceUnlockImgVisibleInLiftCamera();
                    return;
                }
                MiuiKeyguardFaceUnlockView.this.setVisibility(4);
            }

            public void onFaceStop() {
                MiuiKeyguardFaceUnlockView.this.stopShakeHeadAnimation();
                int i = 4;
                if (!MiuiKeyguardFaceUnlockView.this.shouldFaceUnlockViewExecuteAnimation()) {
                    MiuiKeyguardFaceUnlockView.this.setVisibility(4);
                } else if (MiuiKeyguardFaceUnlockView.this.mUpdateMonitor.isFaceUnlock() || (MiuiKeyguardFaceUnlockView.this.mShowBouncer && MiuiKeyguardFaceUnlockView.this.isShowTryFaceDetectImgInBouncer())) {
                    MiuiKeyguardFaceUnlockView miuiKeyguardFaceUnlockView = MiuiKeyguardFaceUnlockView.this;
                    if (MiuiKeyguardFaceUnlockView.this.mKeyguardVisibility) {
                        i = 0;
                    }
                    miuiKeyguardFaceUnlockView.setVisibility(i);
                    MiuiKeyguardFaceUnlockView.this.updateFaceUnlockImgVisibleInLiftCamera();
                } else {
                    MiuiKeyguardFaceUnlockView.this.setVisibility(4);
                }
            }

            public void onFaceLocked() {
                boolean unused = MiuiKeyguardFaceUnlockView.this.mFaceUnlockHasFailed = false;
                MiuiKeyguardFaceUnlockView.this.setVisibility(4);
            }

            public void onKeyguardBouncerChanged(boolean bouncer) {
                boolean unused = MiuiKeyguardFaceUnlockView.this.mShowBouncer = bouncer;
                int i = 4;
                if (MiuiKeyguardFaceUnlockView.this.shouldFaceUnlockViewExecuteAnimation()) {
                    MiuiKeyguardFaceUnlockView.this.updateFaceUnlockViewImage();
                    if (MiuiKeyguardFaceUnlockView.this.mFaceUnlockDetectRuning || ((bouncer && MiuiKeyguardFaceUnlockView.this.isShowTryFaceDetectImgInBouncer()) || (!bouncer && MiuiKeyguardFaceUnlockView.this.mUpdateMonitor.isFaceUnlock()))) {
                        MiuiKeyguardFaceUnlockView miuiKeyguardFaceUnlockView = MiuiKeyguardFaceUnlockView.this;
                        if (MiuiKeyguardFaceUnlockView.this.mKeyguardVisibility) {
                            i = 0;
                        }
                        miuiKeyguardFaceUnlockView.setVisibility(i);
                        MiuiKeyguardFaceUnlockView.this.updateFaceUnlockImgVisibleInLiftCamera();
                    } else if (!MiuiKeyguardFaceUnlockView.this.mUpdateMonitor.isPasswordStatusEnableFaceUnlock()) {
                        MiuiKeyguardFaceUnlockView.this.setVisibility(4);
                    }
                } else {
                    MiuiKeyguardFaceUnlockView.this.setVisibility(4);
                }
            }

            public void onLockScreenMagazinePreViewVisibilityChanged(boolean visible) {
                if (!MiuiKeyguardFaceUnlockView.this.shouldFaceUnlockViewExecuteAnimation()) {
                    MiuiKeyguardFaceUnlockView.this.setVisibility(4);
                } else if (visible) {
                    MiuiKeyguardFaceUnlockView.this.mUpdateMonitor.stopFaceUnlock();
                    MiuiKeyguardFaceUnlockView.this.setVisibility(4);
                    MiuiKeyguardFaceUnlockView.this.stopShakeHeadAnimation();
                } else if (MiuiKeyguardFaceUnlockView.this.mKeyguardVisibility && !MiuiKeyguardFaceUnlockView.this.mShowBouncer && MiuiKeyguardFaceUnlockView.this.mUpdateMonitor.isFaceUnlock()) {
                    MiuiKeyguardFaceUnlockView.this.setVisibility(0);
                }
            }

            public void onKeyguardVisibilityChanged(boolean showing) {
                boolean unused = MiuiKeyguardFaceUnlockView.this.mKeyguardVisibility = showing;
                if (!showing) {
                    MiuiKeyguardFaceUnlockView.this.setVisibility(4);
                }
            }

            public void onFinishedGoingToSleep(int why) {
                MiuiKeyguardFaceUnlockView.this.setVisibility(4);
            }
        };
        this.mFaceUnlockClickListener = new View.OnClickListener() {
            public void onClick(View v) {
                if (!MiuiKeyguardFaceUnlockView.this.mShowBouncer) {
                    return;
                }
                if (MiuiKeyguardFaceUnlockView.this.mFaceUnlockHasFailed || MiuiKeyguardFaceUnlockView.this.isShowTryFaceDetectImgInBouncer()) {
                    ObjectAnimator objectXAnimator = ObjectAnimator.ofFloat(MiuiKeyguardFaceUnlockView.this, "scaleX", new float[]{1.0f, 1.2f, 0.9f, 1.0f});
                    ObjectAnimator objectYAnimator = ObjectAnimator.ofFloat(MiuiKeyguardFaceUnlockView.this, "scaleY", new float[]{1.0f, 1.2f, 0.9f, 1.0f});
                    AnimatorSet animatorSet = new AnimatorSet();
                    animatorSet.setInterpolator(Ease.Sine.easeInOut);
                    animatorSet.setDuration(400);
                    animatorSet.playTogether(new Animator[]{objectXAnimator, objectYAnimator});
                    animatorSet.addListener(new AnimatorListenerAdapter() {
                        public void onAnimationEnd(Animator animation) {
                            MiuiKeyguardFaceUnlockView.this.mUpdateMonitor.startFaceUnlock(MiuiKeyguardFaceUnlockView.this.mShowBouncer);
                            MiuiKeyguardFaceUnlockView.this.mPowerManager.userActivity(SystemClock.uptimeMillis(), false);
                        }
                    });
                    animatorSet.start();
                }
            }
        };
        this.mDelayedHide = new Runnable() {
            public void run() {
                boolean unused = MiuiKeyguardFaceUnlockView.this.mFaceUnlockAnimationRuning = false;
            }
        };
        this.mContext = context;
        this.mUpdateMonitor = KeyguardUpdateMonitor.getInstance(context);
        this.mUpdateMonitor.registerCallback(this.mFaceUnlockInfoCallback);
        this.mPowerManager = (PowerManager) context.getSystemService(PowerManager.class);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        setOnClickListener(this.mFaceUnlockClickListener);
        updateFaceUnlockViewForNotch();
    }

    public void setVisibility(int visibility) {
        if (this.mShowBouncer || !MiuiKeyguardUtils.isIndianRegion(this.mContext) || !WallpaperAuthorityUtils.isLockScreenMagazineOpenedWallpaper() || !this.mUpdateMonitor.isSupportLockScreenMagazineLeft()) {
            super.setVisibility(visibility);
        } else {
            super.setVisibility(4);
        }
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mUpdateMonitor.removeCallback(this.mFaceUnlockInfoCallback);
    }

    private void updateFaceUnlockViewForNotch() {
        int i;
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) getLayoutParams();
        Resources resources = this.mContext.getResources();
        if (Constants.IS_NOTCH) {
            i = R.dimen.miui_face_unlock_view_notch_top;
        } else {
            i = R.dimen.miui_face_unlock_view_top;
        }
        layoutParams.topMargin = resources.getDimensionPixelSize(i);
        setLayoutParams(layoutParams);
    }

    public void setKeyguardFaceUnlockView(boolean keyguard) {
        this.mIsKeyguardFaceUnlockView = keyguard;
    }

    /* access modifiers changed from: private */
    public boolean shouldFaceUnlockViewExecuteAnimation() {
        return ((!this.mShowBouncer && this.mIsKeyguardFaceUnlockView) || (this.mShowBouncer && !this.mIsKeyguardFaceUnlockView)) && MiuiKeyguardUtils.isSupportFaceUnlock(this.mContext);
    }

    /* access modifiers changed from: private */
    public void startFaceUnlockAnimation() {
        if (!this.mFaceUnlockAnimationRuning) {
            this.mFaceUnlockAnimationRuning = true;
            AnimationDrawable mFaceDetectAnimationDrawable = new AnimationDrawable();
            for (int i = 1; i <= 30; i++) {
                String faceViewColor = (this.mShowBouncer || !this.mUpdateMonitor.isLightClock()) ? "face_unlock_error" : "face_unlock_black_error";
                mFaceDetectAnimationDrawable.addFrame(getResources().getDrawable(this.mContext.getResources().getIdentifier(faceViewColor + i, "drawable", this.mContext.getPackageName())), 16);
            }
            setBackground(mFaceDetectAnimationDrawable);
            mFaceDetectAnimationDrawable.setOneShot(true);
            mFaceDetectAnimationDrawable.start();
            this.mAnimationHandler.postDelayed(this.mDelayedHide, 1480);
        }
    }

    /* access modifiers changed from: private */
    public void stopShakeHeadAnimation() {
        this.mAnimationHandler.removeCallbacks(this.mDelayedHide);
        this.mFaceUnlockAnimationRuning = false;
        this.mFaceUnlockDetectRuning = false;
    }

    /* access modifiers changed from: private */
    public void startFaceUnlockSuccessAnimation() {
        AnimationDrawable animationDrawable = new AnimationDrawable();
        for (int i = 1; i <= 20; i++) {
            String faceviewcolor = (this.mShowBouncer || !this.mUpdateMonitor.isLightClock()) ? "face_unlock_success" : "face_unlock_black_success";
            animationDrawable.addFrame(getResources().getDrawable(this.mContext.getResources().getIdentifier(faceviewcolor + i, "drawable", this.mContext.getPackageName())), 16);
        }
        animationDrawable.setOneShot(true);
        setVisibility(this.mKeyguardVisibility ? 0 : 4);
        setBackground(animationDrawable);
        animationDrawable.start();
    }

    /* access modifiers changed from: private */
    public void updateFaceUnlockViewImage() {
        if (this.mShowBouncer || !this.mUpdateMonitor.isLightClock()) {
            setBackground(getResources().getDrawable(this.mUpdateMonitor.isFaceUnlock() ? R.drawable.face_unlock_success20 : R.drawable.face_unlock_error1));
        } else {
            setBackground(getResources().getDrawable(this.mUpdateMonitor.isFaceUnlock() ? R.drawable.face_unlock_black_success20 : R.drawable.face_unlock_black_error1));
        }
    }

    /* access modifiers changed from: private */
    public boolean isShowTryFaceDetectImgInBouncer() {
        if (!(MiuiKeyguardUtils.isSupportLiftingCamera(this.mContext) ? this.mUpdateMonitor.shouldListenForFaceUnlock() : this.mFaceUnlockHasDetectFace && this.mFaceUnlockHasFailed) || !this.mUpdateMonitor.isPasswordStatusEnableFaceUnlock()) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: private */
    public void updateFaceUnlockImgVisibleInLiftCamera() {
        if (MiuiKeyguardUtils.isSupportLiftingCamera(this.mContext) && this.mShowBouncer && isShowTryFaceDetectImgInBouncer()) {
            setVisibility(this.mUpdateMonitor.isKeyguardShowing() ? 0 : 4);
        }
    }
}
