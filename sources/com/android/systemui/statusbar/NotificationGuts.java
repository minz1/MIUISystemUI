package com.android.systemui.statusbar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.android.internal.R;
import com.android.systemui.Interpolators;

public class NotificationGuts extends FrameLayout {
    private int mActualHeight;
    private Drawable mBackground;
    private int mClipBottomAmount;
    private int mClipTopAmount;
    /* access modifiers changed from: private */
    public OnGutsClosedListener mClosedListener;
    /* access modifiers changed from: private */
    public boolean mExposed;
    private Runnable mFalsingCheck;
    private GutsContent mGutsContent;
    private Handler mHandler;
    private OnHeightChangedListener mHeightListener;
    /* access modifiers changed from: private */
    public boolean mIsAnimating;
    /* access modifiers changed from: private */
    public boolean mNeedsFalsingProtection;

    public interface GutsContent {
        int getActualHeight();

        View getContentView();

        boolean handleCloseControls(boolean z, boolean z2);

        boolean isLeavebehind();

        void setGutsParent(NotificationGuts notificationGuts);

        boolean willBeRemoved();
    }

    public interface OnGutsClosedListener {
        void onGutsCloseAnimationEnd();

        void onGutsClosed(NotificationGuts notificationGuts);
    }

    public interface OnHeightChangedListener {
        void onHeightChanged(NotificationGuts notificationGuts);
    }

    public NotificationGuts(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        this.mHandler = new Handler();
        this.mFalsingCheck = new Runnable() {
            public void run() {
                if (NotificationGuts.this.mNeedsFalsingProtection && NotificationGuts.this.mExposed) {
                    NotificationGuts.this.closeControls(-1, -1, false, false);
                }
            }
        };
        context.obtainStyledAttributes(attrs, R.styleable.Theme, 0, 0).recycle();
    }

    public NotificationGuts(Context context) {
        this(context, null);
    }

    public void setGutsContent(GutsContent content) {
        this.mGutsContent = content;
        removeAllViews();
        View contentView = this.mGutsContent.getContentView();
        if (contentView.getParent() != null) {
            ((ViewGroup) contentView.getParent()).removeView(contentView);
        }
        addView(contentView);
    }

    public GutsContent getGutsContent() {
        return this.mGutsContent;
    }

    public void resetFalsingCheck() {
        this.mHandler.removeCallbacks(this.mFalsingCheck);
        if (this.mNeedsFalsingProtection && this.mExposed) {
            this.mHandler.postDelayed(this.mFalsingCheck, 8000);
        }
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        draw(canvas, this.mBackground);
    }

    private void draw(Canvas canvas, Drawable drawable) {
        int top = this.mClipTopAmount;
        int bottom = this.mActualHeight - this.mClipBottomAmount;
        if (drawable != null && top < bottom) {
            drawable.setBounds(0, top, getWidth(), bottom);
            drawable.draw(canvas);
        }
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mBackground = this.mContext.getDrawable(com.android.systemui.R.drawable.notification_guts_bg);
        if (this.mBackground != null) {
            this.mBackground.setCallback(this);
        }
    }

    /* access modifiers changed from: protected */
    public boolean verifyDrawable(Drawable who) {
        return super.verifyDrawable(who) || who == this.mBackground;
    }

    /* access modifiers changed from: protected */
    public void drawableStateChanged() {
        drawableStateChanged(this.mBackground);
    }

    private void drawableStateChanged(Drawable d) {
        if (d != null && d.isStateful()) {
            d.setState(getDrawableState());
        }
    }

    public void drawableHotspotChanged(float x, float y) {
        if (this.mBackground != null) {
            this.mBackground.setHotspot(x, y);
        }
    }

    public void closeControls(boolean leavebehinds, boolean controls, int x, int y, boolean force) {
        if (this.mGutsContent == null) {
            return;
        }
        if (this.mGutsContent.isLeavebehind() && leavebehinds) {
            closeControls(x, y, true, force);
        } else if (!this.mGutsContent.isLeavebehind() && controls) {
            closeControls(x, y, true, force);
        }
    }

    public void closeControls(int x, int y, boolean save, boolean force) {
        if (getWindowToken() == null) {
            if (this.mClosedListener != null) {
                this.mClosedListener.onGutsClosed(this);
                this.mClosedListener.onGutsCloseAnimationEnd();
            }
            return;
        }
        if (this.mGutsContent == null || !this.mGutsContent.handleCloseControls(save, force)) {
            animateClose(x, y);
            setExposed(false, this.mNeedsFalsingProtection);
            if (this.mClosedListener != null) {
                this.mClosedListener.onGutsClosed(this);
            }
        }
    }

    public void setIsAnimating(boolean isAnimating) {
        this.mIsAnimating = isAnimating;
    }

    public boolean isAnimating() {
        return this.mIsAnimating;
    }

    private void animateClose(int x, int y) {
        if (x == -1 || y == -1) {
            x = (getLeft() + getRight()) / 2;
            y = getTop() + (getHeight() / 2);
        }
        Animator a = ViewAnimationUtils.createCircularReveal(this, x, y, (float) Math.hypot((double) Math.max(getWidth() - x, x), (double) Math.max(getHeight() - y, y)), 0.0f);
        a.setDuration(360);
        a.setInterpolator(Interpolators.FAST_OUT_LINEAR_IN);
        a.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                NotificationGuts.this.setVisibility(8);
                boolean unused = NotificationGuts.this.mIsAnimating = false;
                if (NotificationGuts.this.mClosedListener != null) {
                    NotificationGuts.this.mClosedListener.onGutsCloseAnimationEnd();
                }
            }
        });
        a.start();
        this.mIsAnimating = true;
    }

    public void setActualHeight(int actualHeight) {
        this.mActualHeight = actualHeight;
        invalidate();
    }

    public int getIntrinsicHeight() {
        return (this.mGutsContent == null || !this.mExposed) ? getHeight() : this.mGutsContent.getActualHeight();
    }

    public void setClipTopAmount(int clipTopAmount) {
        this.mClipTopAmount = clipTopAmount;
        invalidate();
    }

    public void setClipBottomAmount(int clipBottomAmount) {
        this.mClipBottomAmount = clipBottomAmount;
        invalidate();
    }

    public boolean hasOverlappingRendering() {
        return false;
    }

    public void setClosedListener(OnGutsClosedListener listener) {
        this.mClosedListener = listener;
    }

    public void setHeightChangedListener(OnHeightChangedListener listener) {
        this.mHeightListener = listener;
    }

    /* access modifiers changed from: protected */
    public void onHeightChanged() {
        if (this.mHeightListener != null) {
            this.mHeightListener.onHeightChanged(this);
        }
    }

    public void setExposed(boolean exposed, boolean needsFalsingProtection) {
        boolean wasExposed = this.mExposed;
        this.mExposed = exposed;
        this.mNeedsFalsingProtection = needsFalsingProtection;
        if (!this.mExposed || !this.mNeedsFalsingProtection) {
            this.mHandler.removeCallbacks(this.mFalsingCheck);
        } else {
            resetFalsingCheck();
        }
        if (wasExposed != this.mExposed && this.mGutsContent != null) {
            View contentView = this.mGutsContent.getContentView();
            contentView.sendAccessibilityEvent(32);
            if (this.mExposed) {
                contentView.requestAccessibilityFocus();
            }
        }
    }

    public boolean willBeRemoved() {
        if (this.mGutsContent != null) {
            return this.mGutsContent.willBeRemoved();
        }
        return false;
    }

    public boolean isExposed() {
        return this.mExposed;
    }
}
