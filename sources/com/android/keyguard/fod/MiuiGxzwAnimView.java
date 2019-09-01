package com.android.keyguard.fod;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.FrameLayout;
import com.android.keyguard.KeyguardCompatibilityHelperForO;
import com.android.keyguard.KeyguardCompatibilityHelperForP;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.fod.MiuiGxzwAnimManager;
import com.android.keyguard.fod.MiuiGxzwFrameAnimation;
import com.android.systemui.R;
import miui.view.animation.QuarticEaseOutInterpolator;

public class MiuiGxzwAnimView extends FrameLayout implements DisplayManager.DisplayListener, SurfaceHolder.Callback, KeyguardUpdateMonitor.WallpaperChangeCallback {
    /* access modifiers changed from: private */
    public static long AOD_DOZE_SUSPEND_DELAY = 100;
    private float mAlpha = 1.0f;
    /* access modifiers changed from: private */
    public ValueAnimator mAlphaAnimator;
    private boolean mCollecting = false;
    private DisplayManager mDisplayManager;
    private int mDisplayState = 2;
    private boolean mDozing = false;
    private boolean mDozingIconAnimDone = false;
    private boolean mEnrolling;
    private int mGxzwAnimHeight;
    private int mGxzwAnimWidth;
    private int mGxzwIconHeight;
    private int mGxzwIconWidth;
    private int mGxzwIconX;
    private int mGxzwIconY;
    /* access modifiers changed from: private */
    public Handler mHandler = new Handler();
    private boolean mKeyguardAuthen;
    private KeyguardUpdateMonitor mKeyguardUpdateMonitor;
    private WindowManager.LayoutParams mLayoutParams;
    private boolean mLightIcon = false;
    private boolean mLightWallpaperGxzw;
    private MiuiGxzwAnimManager mMiuiGxzwAnimManager;
    private MiuiGxzwFrameAnimation mMiuiGxzwFrameAnimation;
    private MiuiGxzwTipView mMiuiGxzwTipView;
    private boolean mPortraitOrientation = true;
    private int mScreenHeight;
    private int mScreenWidth;
    private boolean mShouldShowBackAnim = false;
    private boolean mShowing = false;
    private SurfaceView mSurfaceView;
    private WindowManager mWindowManager;

    public MiuiGxzwAnimView(Context context) {
        super(context);
        initView();
    }

    private void initView() {
        LayoutInflater.from(getContext()).inflate(R.layout.miui_keyguard_gxzw_anim_view, this);
        this.mSurfaceView = (SurfaceView) findViewById(R.id.gxzw_anim_surface);
        this.mMiuiGxzwTipView = new MiuiGxzwTipView(getContext());
        this.mGxzwIconX = MiuiGxzwUtils.GXZW_ICON_X;
        this.mGxzwIconY = MiuiGxzwUtils.GXZW_ICON_Y;
        this.mGxzwIconWidth = MiuiGxzwUtils.GXZW_ICON_WIDTH;
        this.mGxzwIconHeight = MiuiGxzwUtils.GXZW_ICON_HEIGHT;
        this.mGxzwAnimWidth = 1008;
        this.mGxzwAnimHeight = 1008;
        setSystemUiVisibility(4868);
        this.mMiuiGxzwFrameAnimation = new MiuiGxzwFrameAnimation(this.mSurfaceView, this);
        this.mMiuiGxzwFrameAnimation.setMode(1);
        this.mWindowManager = (WindowManager) getContext().getSystemService("window");
        this.mDisplayManager = (DisplayManager) getContext().getSystemService("display");
        this.mDisplayManager.registerDisplayListener(this, this.mHandler);
        this.mMiuiGxzwAnimManager = new MiuiGxzwAnimManager(getContext(), this.mMiuiGxzwFrameAnimation);
        int x = this.mGxzwIconX - ((this.mGxzwAnimWidth - this.mGxzwIconWidth) / 2);
        int y = this.mGxzwIconY - ((this.mGxzwAnimHeight - this.mGxzwIconHeight) / 2);
        int width = this.mGxzwAnimWidth;
        int height = this.mGxzwAnimHeight;
        Display display = ((DisplayManager) getContext().getSystemService("display")).getDisplay(0);
        Point point = new Point();
        display.getSize(point);
        this.mScreenWidth = point.x;
        this.mScreenHeight = point.y;
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(width, height, 2015, 16778776, -2);
        this.mLayoutParams = layoutParams;
        KeyguardCompatibilityHelperForP.setLayoutInDisplayCutoutMode(this.mLayoutParams);
        KeyguardCompatibilityHelperForO.setFlag(this.mLayoutParams);
        this.mLayoutParams.privateFlags |= MiuiGxzwUtils.PRIVATE_FLAG_IS_HBM_OVERLAY;
        this.mLayoutParams.gravity = 51;
        this.mLayoutParams.x = x;
        this.mLayoutParams.y = y;
        this.mLayoutParams.setTitle("gxzw_anim");
        this.mKeyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(getContext());
    }

    public void show(boolean lightIcon) {
        if (!this.mShowing) {
            if (this.mAlphaAnimator != null && this.mAlphaAnimator.isRunning()) {
                this.mAlphaAnimator.cancel();
            }
            boolean z = true;
            this.mShowing = true;
            this.mLightIcon = lightIcon;
            this.mMiuiGxzwAnimManager.setLightIcon(this.mLightIcon);
            this.mKeyguardUpdateMonitor.registerWallpaperChangeCallback(this);
            if (getContext().getResources().getConfiguration().orientation != 1 && !this.mKeyguardAuthen) {
                z = false;
            }
            this.mPortraitOrientation = z;
            this.mGxzwIconX = MiuiGxzwUtils.GXZW_ICON_X;
            this.mGxzwIconY = MiuiGxzwUtils.GXZW_ICON_Y;
            this.mGxzwIconWidth = MiuiGxzwUtils.GXZW_ICON_WIDTH;
            this.mGxzwIconHeight = MiuiGxzwUtils.GXZW_ICON_HEIGHT;
            updateLpByOrientation();
            setAlpha(1.0f);
            if (isAttachedToWindow()) {
                this.mWindowManager.updateViewLayout(this, this.mLayoutParams);
                drawFingerprintIcon(this.mDozing);
            } else if (getParent() == null) {
                this.mWindowManager.addView(this, this.mLayoutParams);
            }
            setVisibility(0);
            this.mMiuiGxzwTipView.show();
        }
    }

    public void dismiss() {
        if (this.mShowing) {
            this.mShowing = false;
            this.mKeyguardUpdateMonitor.unregisterWallpaperChangeCallback(this);
            this.mMiuiGxzwTipView.dismiss();
            if (!this.mKeyguardAuthen || MiuiGxzwManager.getInstance().getGxzwUnlockMode() != 2) {
                removeViewFromWindow();
            } else {
                startFadeAniamtion();
            }
        }
    }

    /* access modifiers changed from: private */
    public void removeViewFromWindow() {
        this.mMiuiGxzwFrameAnimation.stopAnimation();
        this.mMiuiGxzwFrameAnimation.clean();
        if (isAttachedToWindow()) {
            this.mWindowManager.removeViewImmediate(this);
        }
        setVisibility(8);
    }

    public void startDozing() {
        this.mDozing = true;
        this.mMiuiGxzwAnimManager.startDozing();
    }

    public void stopDozing() {
        this.mDozingIconAnimDone = false;
        this.mDozing = false;
        this.mMiuiGxzwAnimManager.stopDozing();
        this.mShouldShowBackAnim = false;
        this.mMiuiGxzwTipView.stopTipAnim();
    }

    public void onKeyguardAuthen(boolean keyguardAuthen) {
        this.mKeyguardAuthen = keyguardAuthen;
        this.mMiuiGxzwTipView.onKeyguardAuthen(keyguardAuthen);
        if (this.mShowing && this.mKeyguardAuthen) {
            drawFingerprintIcon(this.mDozing);
        }
        boolean portraitOrientation = true;
        if (getContext().getResources().getConfiguration().orientation != 1 && !this.mKeyguardAuthen) {
            portraitOrientation = false;
        }
        updateOrientation(portraitOrientation);
        this.mMiuiGxzwAnimManager.onKeyguardAuthen(keyguardAuthen);
    }

    public void startIconAnim(boolean aod) {
        Log.i("MiuiGxzwAnimView", "startIconAnim");
        this.mShouldShowBackAnim = false;
        startAnim(aod, this.mMiuiGxzwAnimManager.getIconAnimArgs(aod));
        this.mMiuiGxzwTipView.stopTipAnim();
    }

    public void startRecognizingAnim() {
        Log.i("MiuiGxzwAnimView", "startRecognizingAnim");
        this.mShouldShowBackAnim = true;
        startAnim(this.mDozing, this.mMiuiGxzwAnimManager.getRecognizingAnimArgs(this.mDozing));
        this.mMiuiGxzwTipView.stopTipAnim();
    }

    public void startFalseAnim() {
        Log.i("MiuiGxzwAnimView", "startFalseAnim");
        boolean z = true;
        this.mShouldShowBackAnim = true;
        startAnim(this.mDozing, this.mMiuiGxzwAnimManager.getFalseAnimArgs(this.mDozing));
        MiuiGxzwTipView miuiGxzwTipView = this.mMiuiGxzwTipView;
        if (this.mDozing || !this.mLightWallpaperGxzw) {
            z = false;
        }
        miuiGxzwTipView.startTipAnim(z, getContext().getString(R.string.gxzw_try_again), (float) this.mMiuiGxzwAnimManager.getFalseTipTranslationY());
    }

    public void startBackAnim() {
        Log.i("MiuiGxzwAnimView", "startBackAnim: mShouldShowBackAnim = " + this.mShouldShowBackAnim);
        if (this.mShouldShowBackAnim) {
            this.mShouldShowBackAnim = false;
            startAnim(this.mDozing, this.mMiuiGxzwAnimManager.getBackAnimArgs(this.mDozing));
            this.mMiuiGxzwTipView.stopTipAnim();
            return;
        }
        this.mMiuiGxzwTipView.stopTipAnim();
        drawFingerprintIcon(this.mDozing);
    }

    public void startAnim(boolean aod, MiuiGxzwAnimManager.MiuiGxzwAnimArgs args) {
        int[] res = args.res;
        if (res != null && res.length > 0) {
            this.mMiuiGxzwFrameAnimation.setMode(args.repeat ? 2 : 1);
            this.mMiuiGxzwFrameAnimation.setFrameInterval(args.frameInterval);
            MiuiGxzwFrameAnimation.FrameAnimationListener listener = null;
            if (aod) {
                listener = new MiuiGxzwFrameAnimation.FrameAnimationListener() {
                    public void onStart() {
                        MiuiGxzwManager.getInstance().requestDrawWackLock();
                    }

                    public void onInterrupt() {
                        MiuiGxzwAnimView.this.mHandler.postDelayed(new Runnable() {
                            public void run() {
                                MiuiGxzwManager.getInstance().releaseDrawWackLock();
                            }
                        }, MiuiGxzwAnimView.AOD_DOZE_SUSPEND_DELAY);
                    }

                    public void onFinish() {
                        MiuiGxzwAnimView.this.mHandler.postDelayed(new Runnable() {
                            public void run() {
                                MiuiGxzwManager.getInstance().releaseDrawWackLock();
                            }
                        }, MiuiGxzwAnimView.AOD_DOZE_SUSPEND_DELAY);
                    }
                };
            }
            this.mMiuiGxzwFrameAnimation.startAnimation(res, args.startPosition, args.backgroundRes, args.backgroundFrame, listener, args.customerDrawBitmap);
        }
    }

    public void stopAnim() {
        this.mShouldShowBackAnim = false;
        this.mMiuiGxzwFrameAnimation.stopAnimation();
        this.mMiuiGxzwTipView.stopTipAnim();
    }

    public void stopTip() {
        this.mMiuiGxzwTipView.stopTipAnim();
    }

    public void showMorePress() {
        this.mMiuiGxzwTipView.startTipAnim(!this.mDozing && this.mLightWallpaperGxzw, getContext().getString(R.string.gxzw_press_harder), 260.0f);
    }

    public void setEnrolling(boolean enrolling) {
        this.mEnrolling = enrolling;
        if (this.mShowing) {
            drawFingerprintIcon(this.mDozing);
        }
        this.mMiuiGxzwAnimManager.setEnrolling(enrolling);
    }

    public void drawFingerprintIcon(boolean aod) {
        this.mMiuiGxzwFrameAnimation.draw(this.mMiuiGxzwAnimManager.getFingerIconResource(aod), false, 1.0f);
        if (aod) {
            MiuiGxzwManager.getInstance().requestDrawWackLock(AOD_DOZE_SUSPEND_DELAY);
        }
    }

    public void setCollecting(boolean collecting) {
        this.mCollecting = collecting;
    }

    public void setAlpha(float alpha) {
        this.mAlpha = alpha;
        if (isAttachedToWindow()) {
            this.mLayoutParams.alpha = this.mAlpha;
            this.mWindowManager.updateViewLayout(this, this.mLayoutParams);
            this.mMiuiGxzwTipView.setAlpha(alpha);
        }
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (this.mShowing && Float.compare(this.mLayoutParams.alpha, this.mAlpha) != 0) {
            setAlpha(this.mAlpha);
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        Log.d("MiuiGxzwAnimView", "surfaceCreated");
        if (!this.mKeyguardAuthen || !this.mShowing || !(this.mDisplayState == 3 || this.mDisplayState == 4)) {
            this.mShouldShowBackAnim = false;
            this.mMiuiGxzwTipView.stopTipAnim();
            drawFingerprintIcon(this.mDozing);
        } else if (!this.mDozingIconAnimDone) {
            this.mDozingIconAnimDone = true;
            startIconAnim(true);
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d("MiuiGxzwAnimView", "surfaceChanged");
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d("MiuiGxzwAnimView", "surfaceDestroyed");
    }

    public void onDisplayAdded(int displayId) {
    }

    public void onDisplayRemoved(int displayId) {
    }

    public void onDisplayChanged(int displayId) {
        if (displayId == 0) {
            int newState = this.mDisplayManager.getDisplay(displayId).getState();
            int oldState = this.mDisplayState;
            Log.d("MiuiGxzwAnimView", "onDisplayChanged: oldState = " + oldState + ", newState = " + newState);
            if (this.mKeyguardAuthen && this.mShowing) {
                if (oldState == 1 && newState == 2 && !this.mCollecting) {
                    startIconAnim(false);
                } else if (oldState == 1 && (newState == 3 || newState == 4)) {
                    if (!this.mDozingIconAnimDone && MiuiGxzwUtils.isFodAodShowEnable(getContext()) && !this.mCollecting) {
                        this.mDozingIconAnimDone = true;
                        startIconAnim(true);
                    }
                } else if (!(oldState == 2 && (newState == 3 || newState == 4)) && ((oldState == 3 || oldState == 4) && newState == 2 && !this.mCollecting)) {
                    this.mShouldShowBackAnim = false;
                    this.mMiuiGxzwTipView.stopTipAnim();
                    drawFingerprintIcon(false);
                }
            }
            this.mDisplayState = newState;
        }
    }

    public void onWallpaperChange(boolean succeed) {
        boolean newLightWallpaperGxzw = this.mKeyguardUpdateMonitor.isLightWallpaperGxzw();
        boolean oldLightWallpaperGxzw = this.mLightWallpaperGxzw;
        this.mLightWallpaperGxzw = newLightWallpaperGxzw;
        this.mMiuiGxzwAnimManager.setLightWallpaperGxzw(this.mLightWallpaperGxzw);
        if (oldLightWallpaperGxzw != newLightWallpaperGxzw && !this.mDozing && this.mShowing) {
            this.mShouldShowBackAnim = false;
            this.mMiuiGxzwTipView.stopTipAnim();
            drawFingerprintIcon(this.mDozing);
        }
    }

    private void startFadeAniamtion() {
        if (this.mAlphaAnimator != null && this.mAlphaAnimator.isRunning()) {
            this.mAlphaAnimator.cancel();
        }
        this.mAlphaAnimator = ValueAnimator.ofFloat(new float[]{1.0f, 0.0f});
        this.mAlphaAnimator.setDuration(300);
        this.mAlphaAnimator.setInterpolator(new QuarticEaseOutInterpolator());
        this.mAlphaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                MiuiGxzwAnimView.this.setAlpha(((Float) animation.getAnimatedValue()).floatValue());
            }
        });
        this.mAlphaAnimator.addListener(new Animator.AnimatorListener() {
            private boolean cancel = false;

            public void onAnimationStart(Animator animation) {
            }

            public void onAnimationEnd(Animator animation) {
                ValueAnimator unused = MiuiGxzwAnimView.this.mAlphaAnimator = null;
                if (!this.cancel) {
                    MiuiGxzwAnimView.this.removeViewFromWindow();
                }
            }

            public void onAnimationCancel(Animator animation) {
                ValueAnimator unused = MiuiGxzwAnimView.this.mAlphaAnimator = null;
                this.cancel = true;
            }

            public void onAnimationRepeat(Animator animation) {
            }
        });
        this.mAlphaAnimator.start();
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        boolean portraitOrientation = true;
        if (newConfig.orientation != 1 && !this.mKeyguardAuthen) {
            portraitOrientation = false;
        }
        updateOrientation(portraitOrientation);
    }

    private void updateOrientation(boolean portraitOrientation) {
        if (portraitOrientation != this.mPortraitOrientation && isAttachedToWindow()) {
            this.mPortraitOrientation = portraitOrientation;
            updateLpByOrientation();
            this.mWindowManager.updateViewLayout(this, this.mLayoutParams);
        }
    }

    private void updateLpByOrientation() {
        int height;
        int width;
        int y;
        int x;
        if (this.mPortraitOrientation) {
            x = this.mGxzwIconX - ((this.mGxzwAnimWidth - this.mGxzwIconWidth) / 2);
            y = this.mGxzwIconY - ((this.mGxzwAnimHeight - this.mGxzwIconHeight) / 2);
            width = this.mGxzwAnimWidth;
            height = this.mGxzwAnimHeight;
        } else {
            y = this.mGxzwIconX - ((this.mGxzwAnimWidth - this.mGxzwIconWidth) / 2);
            x = this.mGxzwIconY - ((this.mGxzwAnimHeight - this.mGxzwIconHeight) / 2);
            height = this.mGxzwAnimWidth;
            width = this.mGxzwAnimHeight;
        }
        int rotation = ((DisplayManager) getContext().getSystemService("display")).getDisplay(0).getRotation();
        if (!this.mKeyguardAuthen && (rotation == 2 || rotation == 3)) {
            x = ((this.mPortraitOrientation ? this.mScreenWidth : this.mScreenHeight) - x) - width;
            y = ((this.mPortraitOrientation ? this.mScreenHeight : this.mScreenWidth) - y) - height;
        }
        this.mLayoutParams.width = width;
        this.mLayoutParams.height = height;
        this.mLayoutParams.x = x;
        this.mLayoutParams.y = y;
    }
}
