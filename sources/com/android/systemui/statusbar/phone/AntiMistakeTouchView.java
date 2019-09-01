package com.android.systemui.statusbar.phone;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import com.android.systemui.R;
import java.lang.ref.WeakReference;

public class AntiMistakeTouchView extends View implements ValueAnimator.AnimatorUpdateListener {
    private static final boolean DEBUG = Log.isLoggable("AntiMistakeTouchView", 3);
    private Drawable mDrawable;
    private int mDrawableHeight;
    private int mDrawableWidth;
    private H mHandler;
    private Rect mRect;
    private ValueAnimator mSlideAnimator;
    private int mTopMargin;

    private static class H extends Handler {
        private WeakReference<AntiMistakeTouchView> mRef;

        private H(AntiMistakeTouchView view) {
            this.mRef = new WeakReference<>(view);
        }

        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (!(this.mRef == null || this.mRef.get() == null || msg.what != 191)) {
                ((AntiMistakeTouchView) this.mRef.get()).slideDown();
            }
        }
    }

    public AntiMistakeTouchView(Context context) {
        this(context, null);
    }

    public AntiMistakeTouchView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AntiMistakeTouchView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mHandler = new H();
        this.mDrawable = getResources().getDrawable(R.drawable.anti_touch_bar);
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        this.mDrawableWidth = Math.min(displayMetrics.widthPixels, displayMetrics.heightPixels);
        this.mDrawableHeight = (int) (10.0f * displayMetrics.density);
        if (DEBUG) {
            Log.i("AntiMistakeTouchView", "AntiMistakeTouchView: " + this.mDrawableWidth + " " + this.mDrawableHeight);
        }
        this.mTopMargin = this.mDrawableHeight;
        this.mRect = new Rect(0, this.mTopMargin, this.mDrawableWidth, this.mTopMargin + this.mDrawableHeight);
        setVisibility(8);
        setEnabled(false);
        setClickable(false);
    }

    public boolean containsLocation(float x) {
        int[] location = new int[2];
        getLocationOnScreen(location);
        if (DEBUG) {
            Log.i("AntiMistakeTouchView", "contains: " + x + " " + location[0]);
        }
        float offsetX = x - ((float) location[0]);
        if (offsetX < 0.01f || offsetX > ((float) this.mDrawableWidth)) {
            return false;
        }
        return true;
    }

    private void initAnimator() {
        this.mSlideAnimator = new ValueAnimator();
        this.mSlideAnimator.setDuration(200);
        this.mSlideAnimator.addUpdateListener(this);
    }

    public void slideUp() {
        if (DEBUG) {
            Log.i("AntiMistakeTouchView", "slideUp: ");
        }
        this.mHandler.removeMessages(191);
        if (this.mSlideAnimator != null) {
            this.mSlideAnimator.cancel();
        } else {
            initAnimator();
        }
        this.mSlideAnimator.setIntValues(new int[]{this.mRect.top, 0});
        this.mSlideAnimator.start();
        this.mHandler.sendEmptyMessageDelayed(191, 2000);
    }

    /* access modifiers changed from: private */
    public void slideDown() {
        if (this.mSlideAnimator != null) {
            this.mSlideAnimator.cancel();
        } else {
            initAnimator();
        }
        this.mSlideAnimator.setIntValues(new int[]{this.mRect.top, this.mTopMargin});
        this.mSlideAnimator.start();
    }

    public void onAnimationUpdate(ValueAnimator animation) {
        int top = ((Integer) animation.getAnimatedValue()).intValue();
        this.mRect.set(0, top, this.mDrawableWidth, this.mDrawableHeight + top);
        this.mDrawable.setAlpha(0 + ((255 * (this.mTopMargin - top)) / this.mTopMargin));
        if (DEBUG) {
            Log.i("AntiMistakeTouchView", "onAnimationUpdate: " + top);
        }
        invalidate();
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        this.mDrawable.setBounds(this.mRect);
        this.mDrawable.draw(canvas);
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mHandler.removeCallbacksAndMessages(null);
    }

    public void updateVisibilityState(int visibility) {
        if (DEBUG) {
            Log.i("AntiMistakeTouchView", "updateVisibilityState: " + visibility);
        }
        if (visibility != getVisibility()) {
            if (this.mSlideAnimator != null) {
                this.mSlideAnimator.cancel();
            }
            if (visibility == 0) {
                this.mRect.top = this.mTopMargin;
                this.mDrawable.setAlpha(0);
            }
            setVisibility(visibility);
        }
    }

    public FrameLayout.LayoutParams getFrameLayoutParams() {
        FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(this.mDrawableWidth, this.mDrawableHeight);
        flp.gravity = 81;
        return flp;
    }
}
