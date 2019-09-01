package com.android.systemui.statusbar.policy;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.CanvasProperty;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.DisplayListCanvas;
import android.view.RenderNodeAnimator;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import com.android.systemui.R;
import java.util.ArrayList;
import java.util.HashSet;

public class KeyButtonRipple extends Drawable {
    private final Interpolator mAlphaExitInterpolator = new PathInterpolator(0.0f, 0.0f, 0.8f, 1.0f);
    private final AnimatorListenerAdapter mAnimatorListener = new AnimatorListenerAdapter() {
        public void onAnimationEnd(Animator animation) {
            KeyButtonRipple.this.mRunningAnimations.remove(animation);
            if (KeyButtonRipple.this.mRunningAnimations.isEmpty() && !KeyButtonRipple.this.mPressed) {
                boolean unused = KeyButtonRipple.this.mDrawingHardwareGlow = false;
                KeyButtonRipple.this.invalidateSelf();
            }
        }
    };
    private CanvasProperty<Float> mBottomProp;
    /* access modifiers changed from: private */
    public boolean mDrawingHardwareGlow;
    private Rect mEndRect;
    private float mFirstLeftEnd;
    private float mFirstLeftStart;
    private float mFirstRightEnd;
    private float mFirstRightStart;
    private float mGlowAlpha = 0.0f;
    private float mGlowScale = 1.0f;
    private final Interpolator mInterpolator = new LogInterpolator();
    private CanvasProperty<Float> mLeftProp;
    private int mMaxWidth;
    private CanvasProperty<Paint> mPaintProp;
    /* access modifiers changed from: private */
    public boolean mPressed;
    private CanvasProperty<Float> mRightProp;
    private Paint mRipplePaint;
    /* access modifiers changed from: private */
    public final HashSet<Animator> mRunningAnimations = new HashSet<>();
    private CanvasProperty<Float> mRxProp;
    private CanvasProperty<Float> mRyProp;
    private float mSecondLeftEnd;
    private float mSecondLeftStart;
    private float mSecondRightEnd;
    private float mSecondRightStart;
    private final Interpolator mSineInterpolator = new SineInterpolator();
    private final AnimatorListenerAdapter mSlideAnimatorListener = new AnimatorListenerAdapter() {
        public void onAnimationEnd(Animator animation) {
            KeyButtonRipple.this.mRunningAnimations.remove(animation);
            KeyButtonRipple.this.sildeSecondPart();
            if (KeyButtonRipple.this.mRunningAnimations.isEmpty() && !KeyButtonRipple.this.mPressed) {
                boolean unused = KeyButtonRipple.this.mDrawingHardwareGlow = false;
                KeyButtonRipple.this.invalidateSelf();
            }
        }
    };
    private boolean mSlideToRight = false;
    private Rect mStartRect;
    private boolean mSupportHardware;
    private final View mTargetView;
    private final ArrayList<Animator> mTmpArray = new ArrayList<>();
    private CanvasProperty<Float> mTopProp;

    private static final class LogInterpolator implements Interpolator {
        private LogInterpolator() {
        }

        public float getInterpolation(float input) {
            return 1.0f - ((float) Math.pow(400.0d, ((double) (-input)) * 1.4d));
        }
    }

    private static final class SineInterpolator implements Interpolator {
        private SineInterpolator() {
        }

        public float getInterpolation(float input) {
            return ((float) (1.0d - Math.cos(3.141592653589793d * ((double) input)))) / 2.0f;
        }
    }

    public KeyButtonRipple(Context ctx, View targetView) {
        this.mMaxWidth = ctx.getResources().getDimensionPixelSize(R.dimen.key_button_ripple_max_width);
        this.mTargetView = targetView;
    }

    private Paint getRipplePaint() {
        if (this.mRipplePaint == null) {
            this.mRipplePaint = new Paint();
            this.mRipplePaint.setAntiAlias(true);
            this.mRipplePaint.setColor(-3355444);
        }
        return this.mRipplePaint;
    }

    private void drawSoftware(Canvas canvas) {
        if (this.mGlowAlpha > 0.0f) {
            Paint p = getRipplePaint();
            p.setAlpha((int) (this.mGlowAlpha * 255.0f));
            float w = (float) getBounds().width();
            float h = (float) getBounds().height();
            boolean horizontal = w > h;
            float radius = ((float) getRippleSize()) * this.mGlowScale * 0.5f;
            float cx = w * 0.5f;
            float cy = h * 0.5f;
            float rx = horizontal ? radius : cx;
            float ry = horizontal ? cy : radius;
            float corner = horizontal ? cy : cx;
            canvas.drawRoundRect(cx - rx, cy - ry, cx + rx, cy + ry, corner, corner, p);
        }
    }

    public void draw(Canvas canvas) {
        this.mSupportHardware = canvas.isHardwareAccelerated();
        if (this.mSupportHardware) {
            drawHardware((DisplayListCanvas) canvas);
        } else {
            drawSoftware(canvas);
        }
    }

    public void setAlpha(int alpha) {
    }

    public void setColorFilter(ColorFilter colorFilter) {
    }

    public int getOpacity() {
        return -3;
    }

    private boolean isHorizontal() {
        return getBounds().width() > getBounds().height();
    }

    private void drawHardware(DisplayListCanvas c) {
        if (this.mDrawingHardwareGlow) {
            c.drawRoundRect(this.mLeftProp, this.mTopProp, this.mRightProp, this.mBottomProp, this.mRxProp, this.mRyProp, this.mPaintProp);
        }
    }

    public float getGlowAlpha() {
        return this.mGlowAlpha;
    }

    public void setGlowAlpha(float x) {
        this.mGlowAlpha = x;
        invalidateSelf();
    }

    public float getGlowScale() {
        return this.mGlowScale;
    }

    public void setGlowScale(float x) {
        this.mGlowScale = x;
        invalidateSelf();
    }

    /* access modifiers changed from: protected */
    public boolean onStateChange(int[] state) {
        boolean pressed = false;
        int i = 0;
        while (true) {
            if (i >= state.length) {
                break;
            } else if (state[i] == 16842919) {
                pressed = true;
                break;
            } else {
                i++;
            }
        }
        if (pressed == this.mPressed) {
            return false;
        }
        setPressed(pressed);
        this.mPressed = pressed;
        return true;
    }

    public void jumpToCurrentState() {
        cancelAnimations();
    }

    public boolean isStateful() {
        return true;
    }

    public void setPressed(boolean pressed) {
        if (this.mSupportHardware) {
            setPressedHardware(pressed);
        } else {
            setPressedSoftware(pressed);
        }
    }

    private void cancelAnimations() {
        this.mTmpArray.addAll(this.mRunningAnimations);
        int size = this.mTmpArray.size();
        for (int i = 0; i < size; i++) {
            this.mTmpArray.get(i).cancel();
        }
        this.mTmpArray.clear();
        this.mRunningAnimations.clear();
    }

    private void setPressedSoftware(boolean pressed) {
        if (pressed) {
            enterSoftware();
        } else {
            exitSoftware();
        }
    }

    private void enterSoftware() {
        cancelAnimations();
        this.mGlowAlpha = 0.25f;
        ObjectAnimator scaleAnimator = ObjectAnimator.ofFloat(this, "glowScale", new float[]{0.0f, 1.35f});
        scaleAnimator.setInterpolator(this.mInterpolator);
        scaleAnimator.setDuration(350);
        scaleAnimator.addListener(this.mAnimatorListener);
        scaleAnimator.start();
        this.mRunningAnimations.add(scaleAnimator);
    }

    private void exitSoftware() {
        ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(this, "glowAlpha", new float[]{this.mGlowAlpha, 0.0f});
        alphaAnimator.setInterpolator(this.mAlphaExitInterpolator);
        alphaAnimator.setDuration(450);
        alphaAnimator.addListener(this.mAnimatorListener);
        alphaAnimator.start();
        this.mRunningAnimations.add(alphaAnimator);
    }

    private void setPressedHardware(boolean pressed) {
        if (pressed) {
            enterHardware();
        } else {
            exitHardware();
        }
    }

    private void setExtendStart(CanvasProperty<Float> prop) {
        if (isHorizontal()) {
            this.mLeftProp = prop;
        } else {
            this.mTopProp = prop;
        }
    }

    private CanvasProperty<Float> getExtendStart() {
        return isHorizontal() ? this.mLeftProp : this.mTopProp;
    }

    private void setExtendEnd(CanvasProperty<Float> prop) {
        if (isHorizontal()) {
            this.mRightProp = prop;
        } else {
            this.mBottomProp = prop;
        }
    }

    private CanvasProperty<Float> getExtendEnd() {
        return isHorizontal() ? this.mRightProp : this.mBottomProp;
    }

    private int getExtendSize() {
        return isHorizontal() ? getBounds().width() : getBounds().height();
    }

    private int getRippleSize() {
        return Math.min(isHorizontal() ? getBounds().width() : getBounds().height(), this.mMaxWidth);
    }

    private void enterHardware() {
        cancelAnimations();
        this.mDrawingHardwareGlow = true;
        setExtendStart(CanvasProperty.createFloat((float) (getExtendSize() / 2)));
        RenderNodeAnimator startAnim = new RenderNodeAnimator(getExtendStart(), ((float) (getExtendSize() / 2)) - ((((float) getRippleSize()) * 1.35f) / 2.0f));
        startAnim.setDuration(350);
        startAnim.setInterpolator(this.mInterpolator);
        startAnim.addListener(this.mAnimatorListener);
        startAnim.setTarget(this.mTargetView);
        setExtendEnd(CanvasProperty.createFloat((float) (getExtendSize() / 2)));
        RenderNodeAnimator endAnim = new RenderNodeAnimator(getExtendEnd(), ((float) (getExtendSize() / 2)) + ((((float) getRippleSize()) * 1.35f) / 2.0f));
        endAnim.setDuration(350);
        endAnim.setInterpolator(this.mInterpolator);
        endAnim.addListener(this.mAnimatorListener);
        endAnim.setTarget(this.mTargetView);
        if (isHorizontal()) {
            this.mTopProp = CanvasProperty.createFloat(0.0f);
            this.mBottomProp = CanvasProperty.createFloat((float) getBounds().height());
            this.mRxProp = CanvasProperty.createFloat((float) (getBounds().height() / 2));
            this.mRyProp = CanvasProperty.createFloat((float) (getBounds().height() / 2));
        } else {
            this.mLeftProp = CanvasProperty.createFloat(0.0f);
            this.mRightProp = CanvasProperty.createFloat((float) getBounds().width());
            this.mRxProp = CanvasProperty.createFloat((float) (getBounds().width() / 2));
            this.mRyProp = CanvasProperty.createFloat((float) (getBounds().width() / 2));
        }
        this.mGlowScale = 1.35f;
        this.mGlowAlpha = 0.25f;
        this.mRipplePaint = getRipplePaint();
        this.mRipplePaint.setAlpha((int) (this.mGlowAlpha * 255.0f));
        this.mPaintProp = CanvasProperty.createPaint(this.mRipplePaint);
        startAnim.start();
        endAnim.start();
        this.mRunningAnimations.add(startAnim);
        this.mRunningAnimations.add(endAnim);
        invalidateSelf();
    }

    private void exitHardware() {
        this.mPaintProp = CanvasProperty.createPaint(getRipplePaint());
        RenderNodeAnimator opacityAnim = new RenderNodeAnimator(this.mPaintProp, 1, 0.0f);
        opacityAnim.setDuration(450);
        opacityAnim.setInterpolator(this.mAlphaExitInterpolator);
        opacityAnim.addListener(this.mAnimatorListener);
        opacityAnim.setTarget(this.mTargetView);
        opacityAnim.start();
        this.mRunningAnimations.add(opacityAnim);
        invalidateSelf();
    }

    public void gestureSlideEffect(Rect startRect, Rect endRect) {
        this.mStartRect = startRect;
        this.mEndRect = endRect;
        if (this.mStartRect.left < this.mEndRect.left) {
            this.mSlideToRight = true;
            this.mFirstLeftStart = 0.0f;
            this.mFirstLeftEnd = (((float) getRippleSize()) * -0.35f) / 2.0f;
            this.mFirstRightStart = (float) (this.mStartRect.right - this.mStartRect.left);
            this.mFirstRightEnd = ((float) (this.mEndRect.right - this.mStartRect.left)) + ((((float) getRippleSize()) * 0.35f) / 2.0f);
            this.mSecondLeftStart = (-0.35f * ((float) getRippleSize())) / 2.0f;
            this.mSecondLeftEnd = ((float) (this.mEndRect.left - this.mStartRect.left)) - ((((float) getRippleSize()) * 0.35f) / 2.0f);
            this.mSecondRightStart = ((float) (this.mEndRect.right - this.mStartRect.left)) + ((((float) getRippleSize()) * 0.35f) / 2.0f);
            this.mSecondRightEnd = ((float) (this.mEndRect.right - this.mStartRect.left)) + ((0.35f * ((float) getRippleSize())) / 2.0f);
        } else {
            this.mSlideToRight = false;
            this.mFirstLeftStart = 0.0f;
            this.mFirstLeftEnd = ((float) (this.mEndRect.left - this.mStartRect.left)) - ((((float) getRippleSize()) * 0.35f) / 2.0f);
            this.mFirstRightStart = (float) (this.mStartRect.right - this.mStartRect.left);
            this.mFirstRightEnd = ((float) (this.mStartRect.right - this.mStartRect.left)) + ((((float) getRippleSize()) * 0.35f) / 2.0f);
            this.mSecondLeftStart = ((float) (this.mEndRect.left - this.mStartRect.left)) - ((((float) getRippleSize()) * 0.35f) / 2.0f);
            this.mSecondLeftEnd = ((float) (this.mEndRect.left - this.mStartRect.left)) - ((((float) getRippleSize()) * 0.35f) / 2.0f);
            this.mSecondRightStart = ((float) (this.mStartRect.right - this.mStartRect.left)) + ((((float) getRippleSize()) * 0.35f) / 2.0f);
            this.mSecondRightEnd = ((float) (this.mEndRect.right - this.mStartRect.left)) + ((0.35f * ((float) getRippleSize())) / 2.0f);
        }
        sildeFirstPart();
    }

    private void sildeFirstPart() {
        cancelAnimations();
        this.mDrawingHardwareGlow = true;
        setExtendStart(CanvasProperty.createFloat(this.mFirstLeftStart));
        RenderNodeAnimator startAnim = new RenderNodeAnimator(getExtendStart(), this.mFirstLeftEnd);
        startAnim.setDuration(250);
        startAnim.setInterpolator(this.mSineInterpolator);
        startAnim.addListener(this.mSlideToRight ? this.mAnimatorListener : this.mSlideAnimatorListener);
        startAnim.setTarget(this.mTargetView);
        setExtendEnd(CanvasProperty.createFloat(this.mFirstRightStart));
        RenderNodeAnimator endAnim = new RenderNodeAnimator(getExtendEnd(), this.mFirstRightEnd);
        endAnim.setDuration(250);
        endAnim.setInterpolator(this.mSineInterpolator);
        endAnim.addListener(this.mSlideToRight ? this.mSlideAnimatorListener : this.mAnimatorListener);
        endAnim.setTarget(this.mTargetView);
        if (isHorizontal()) {
            this.mTopProp = CanvasProperty.createFloat(0.0f);
            this.mBottomProp = CanvasProperty.createFloat((float) getBounds().height());
            this.mRxProp = CanvasProperty.createFloat((float) (getBounds().height() / 2));
            this.mRyProp = CanvasProperty.createFloat((float) (getBounds().height() / 2));
        } else {
            this.mLeftProp = CanvasProperty.createFloat(0.0f);
            this.mRightProp = CanvasProperty.createFloat((float) getBounds().width());
            this.mRxProp = CanvasProperty.createFloat((float) (getBounds().width() / 2));
            this.mRyProp = CanvasProperty.createFloat((float) (getBounds().width() / 2));
        }
        this.mGlowScale = 1.35f;
        this.mRipplePaint = getRipplePaint();
        this.mRipplePaint.setAlpha((int) (this.mGlowAlpha * 255.0f));
        this.mPaintProp = CanvasProperty.createPaint(this.mRipplePaint);
        startAnim.start();
        endAnim.start();
        this.mRunningAnimations.add(startAnim);
        this.mRunningAnimations.add(endAnim);
        invalidateSelf();
    }

    /* access modifiers changed from: private */
    public void sildeSecondPart() {
        cancelAnimations();
        this.mDrawingHardwareGlow = true;
        setExtendStart(CanvasProperty.createFloat(this.mSecondLeftStart));
        RenderNodeAnimator startAnim = new RenderNodeAnimator(getExtendStart(), this.mSecondLeftEnd);
        startAnim.setDuration(250);
        startAnim.setInterpolator(this.mSineInterpolator);
        startAnim.addListener(this.mAnimatorListener);
        startAnim.setTarget(this.mTargetView);
        setExtendEnd(CanvasProperty.createFloat(this.mSecondRightStart));
        RenderNodeAnimator endAnim = new RenderNodeAnimator(getExtendEnd(), this.mSecondRightEnd);
        endAnim.setDuration(250);
        endAnim.setInterpolator(this.mSineInterpolator);
        endAnim.addListener(this.mAnimatorListener);
        endAnim.setTarget(this.mTargetView);
        if (isHorizontal()) {
            this.mTopProp = CanvasProperty.createFloat(0.0f);
            this.mBottomProp = CanvasProperty.createFloat((float) getBounds().height());
            this.mRxProp = CanvasProperty.createFloat((float) (getBounds().height() / 2));
            this.mRyProp = CanvasProperty.createFloat((float) (getBounds().height() / 2));
        } else {
            this.mLeftProp = CanvasProperty.createFloat(0.0f);
            this.mRightProp = CanvasProperty.createFloat((float) getBounds().width());
            this.mRxProp = CanvasProperty.createFloat((float) (getBounds().width() / 2));
            this.mRyProp = CanvasProperty.createFloat((float) (getBounds().width() / 2));
        }
        this.mGlowScale = 1.35f;
        this.mRipplePaint = getRipplePaint();
        this.mRipplePaint.setAlpha((int) (this.mGlowAlpha * 255.0f));
        this.mPaintProp = CanvasProperty.createPaint(this.mRipplePaint);
        startAnim.start();
        endAnim.start();
        this.mRunningAnimations.add(startAnim);
        this.mRunningAnimations.add(endAnim);
        invalidateSelf();
    }
}
