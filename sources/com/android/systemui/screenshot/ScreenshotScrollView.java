package com.android.systemui.screenshot;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Scroller;
import com.android.systemui.Constants;
import com.android.systemui.R;
import java.util.List;
import miui.util.LongScreenshotUtils;

public class ScreenshotScrollView extends View {
    /* access modifiers changed from: private */
    public float mAnimatableOffsetY;
    private AnimatingCallback mAnimatingCallback;
    Runnable mAnimatingStepRunnable;
    /* access modifiers changed from: private */
    public int mAnimatorStep;
    private Bitmap mBottomPart;
    private long mFirstClickTime;
    private ValueAnimator mGotoSingleAnimator;
    /* access modifiers changed from: private */
    public boolean mIsAnimatingStoped;
    private boolean mIsBuildingLongScreenshot;
    private boolean mIsManuTaking;
    private boolean mIsTakingLongScreenshot;
    private float mLastTouchY;
    /* access modifiers changed from: private */
    public LongScreenshotUtils.LongBitmapDrawable mLongBitmapDrawable;
    private float mMaxOffsetY;
    private float mMinOffsetY;
    private int mMinTotalHeight;
    /* access modifiers changed from: private */
    public float mOffsetY;
    private Scroller mScroller;
    /* access modifiers changed from: private */
    public float mShowBig;
    private ValueAnimator mShowBigAnimator;
    private int mShowedPageCount;
    private Bitmap mSingleBitmap;
    private int mTotalHeight;
    private int mUiState;
    private VelocityTracker mVelocityTracker;

    public interface AnimatingCallback {
        void doubleClickEventReaction(boolean z);

        void onShowedPageCountChanged(int i);
    }

    public ScreenshotScrollView(Context context) {
        this(context, null);
    }

    public ScreenshotScrollView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScreenshotScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mVelocityTracker = VelocityTracker.obtain();
        this.mUiState = 0;
        this.mAnimatingStepRunnable = new Runnable() {
            public void run() {
                if (ScreenshotScrollView.this.mIsAnimatingStoped) {
                    Log.d("ScreenshotScrollView", "mIsAnimatingStoped, but also get here.");
                } else if (ScreenshotScrollView.this.mLongBitmapDrawable == null) {
                    Log.d("ScreenshotScrollView", "bitmap is null.");
                } else {
                    ScreenshotScrollView.this.doAnimatingStep(ScreenshotScrollView.this.mAnimatorStep);
                    ScreenshotScrollView.this.post(ScreenshotScrollView.this.mAnimatingStepRunnable);
                }
            }
        };
        this.mScroller = new Scroller(context);
        this.mAnimatorStep = (int) ((2.0f * getResources().getDisplayMetrics().density) + 0.5f);
        this.mShowBigAnimator = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
        this.mShowBigAnimator.setDuration(200);
        this.mShowBigAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                float unused = ScreenshotScrollView.this.mShowBig = ((Float) animation.getAnimatedValue()).floatValue();
                float unused2 = ScreenshotScrollView.this.mAnimatableOffsetY = ScreenshotScrollView.this.mOffsetY * animation.getAnimatedFraction();
                ScreenshotScrollView.this.invalidate();
            }
        });
    }

    public boolean performClick(MotionEvent event) {
        if (System.currentTimeMillis() - this.mFirstClickTime <= ((long) ViewConfiguration.getDoubleTapTimeout())) {
            onDoubleClick(event);
        } else {
            this.mFirstClickTime = System.currentTimeMillis();
        }
        return true;
    }

    private void onDoubleClick(MotionEvent event) {
        if (this.mShowBigAnimator.isRunning() || ((Float) this.mShowBigAnimator.getAnimatedValue()).floatValue() != 0.0f) {
            this.mShowBigAnimator.reverse();
            if (this.mAnimatingCallback != null) {
                this.mAnimatingCallback.doubleClickEventReaction(false);
            }
        } else {
            this.mOffsetY = ((this.mMaxOffsetY - this.mMinOffsetY) * Math.max(Math.min(1.0f - ((event.getY() - ((float) getPaddingTop())) / ((float) getHeightInner())), 1.0f), 0.0f)) + this.mMinOffsetY;
            this.mShowBigAnimator.start();
            if (this.mAnimatingCallback != null) {
                this.mAnimatingCallback.doubleClickEventReaction(true);
            }
        }
        StatHelper.recordCountEvent(this.mContext, "double_click", this.mLongBitmapDrawable == null ? "normal" : "longscreenshot");
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (this.mIsBuildingLongScreenshot) {
            return true;
        }
        this.mVelocityTracker.addMovement(event);
        switch (event.getAction()) {
            case 0:
                this.mLastTouchY = event.getY();
                if (this.mUiState == 2) {
                    this.mScroller.forceFinished(true);
                }
                if (!this.mIsTakingLongScreenshot) {
                    this.mUiState = 1;
                    break;
                } else {
                    if (!this.mIsManuTaking) {
                        StatHelper.recordCountEvent(this.mContext, "longscreenshot_manual");
                    }
                    stopAnimating(false);
                    this.mIsManuTaking = true;
                    this.mUiState = 2;
                    break;
                }
            case 1:
            case 3:
                if (this.mUiState == 2) {
                    this.mVelocityTracker.computeCurrentVelocity(1000);
                    if (this.mIsTakingLongScreenshot) {
                        this.mScroller.setFriction(ViewConfiguration.getScrollFriction() * 2.0f);
                        this.mScroller.fling(0, this.mTotalHeight, 0, -getVelocityY(7000), 0, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
                        this.mUiState = 3;
                        invalidate();
                    } else if (this.mShowBig == 1.0f) {
                        this.mScroller.setFriction(ViewConfiguration.getScrollFriction());
                        this.mScroller.fling(0, (int) this.mOffsetY, 0, getVelocityY(10000), 0, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
                        this.mUiState = 3;
                        invalidate();
                    }
                } else {
                    performClick(event);
                    this.mUiState = 0;
                }
                this.mVelocityTracker.clear();
                break;
            case 2:
                float thisOffsetY = event.getY() - this.mLastTouchY;
                if (!this.mIsTakingLongScreenshot) {
                    if (this.mShowBig == 1.0f) {
                        if (this.mUiState != 2 && Math.abs(thisOffsetY) >= ((float) ViewConfiguration.get(this.mContext).getScaledDoubleTapTouchSlop())) {
                            this.mUiState = 2;
                        }
                        if (this.mUiState == 2) {
                            setOffsetY(this.mOffsetY + thisOffsetY);
                            this.mLastTouchY = event.getY();
                            break;
                        }
                    }
                } else {
                    doAnimatingStep(-((int) (0.5f + thisOffsetY)));
                    this.mLastTouchY = event.getY();
                    break;
                }
                break;
        }
        return true;
    }

    private void setOffsetY(float offsetY) {
        float offsetY2 = Math.max(Math.min(offsetY, this.mMaxOffsetY), this.mMinOffsetY);
        this.mOffsetY = offsetY2;
        this.mAnimatableOffsetY = offsetY2;
        invalidate();
    }

    private int getVelocityY(int maxVelocity) {
        return Math.min(maxVelocity, Math.max(-maxVelocity, (int) this.mVelocityTracker.getYVelocity()));
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        int topY;
        boolean z = false;
        if (this.mUiState == 3) {
            if (!this.mScroller.computeScrollOffset()) {
                this.mUiState = 0;
            } else if (this.mIsTakingLongScreenshot) {
                doAnimatingStep(this.mScroller.getCurrY() - this.mTotalHeight);
            } else if (this.mShowBig == 1.0f) {
                setOffsetY((float) this.mScroller.getCurrY());
            }
        }
        if (this.mSingleBitmap != null && (this.mLongBitmapDrawable == null || this.mLongBitmapDrawable.getBitmaps().length == 0 || this.mShowBig < 1.0f)) {
            z = true;
        }
        boolean useSingleBitmap = z;
        float scale = calcScale(useSingleBitmap);
        if (useSingleBitmap) {
            canvas.translate(0.0f, (float) ((int) (this.mAnimatableOffsetY + (((float) getPaddingTop()) * (1.0f - this.mShowBig)))));
            canvas.scale(scale, scale, ((float) getWidth()) / 2.0f, 0.0f);
            drawSingleBitmap(canvas, scale);
            return;
        }
        int x = getPaddingLeft() + ((getWidthInner() - ((int) (((float) this.mLongBitmapDrawable.getIntrinsicWidth()) * scale))) / 2);
        if (this.mIsTakingLongScreenshot) {
            topY = Math.min(getHeight() - ((int) (((float) this.mTotalHeight) * scale)), getPaddingTop());
        } else {
            topY = (int) ((((float) getPaddingTop()) * (1.0f - this.mShowBig)) + ((float) ((int) this.mAnimatableOffsetY)));
        }
        canvas.translate((float) x, (float) topY);
        canvas.scale(scale, scale);
        drawLongScreenshot(canvas);
    }

    private float calcScale(boolean useSingleBitmap) {
        return calcScale(useSingleBitmap, this.mShowBig);
    }

    private float calcScale(boolean useSingleBitmap, float showBig) {
        if (useSingleBitmap) {
            int bitmapWidth = this.mSingleBitmap.getWidth();
            return ((1.0f - showBig) * Math.min(((float) getWidthInner()) / ((float) bitmapWidth), ((float) getHeightInner()) / ((float) this.mSingleBitmap.getHeight()))) + (showBig * (((float) getWidth()) / ((float) bitmapWidth)));
        }
        return ((1.0f - showBig) * (((float) getWidthInner()) / ((float) this.mLongBitmapDrawable.getIntrinsicWidth()))) + showBig;
    }

    private void drawSingleBitmap(Canvas canvas, float scale) {
        int x = (getWidth() - this.mSingleBitmap.getWidth()) / 2;
        int bitmapStrokeWidth = getResources().getDimensionPixelSize(R.dimen.screenshot_stroke_width);
        int bitmapMarginTop = getResources().getDimensionPixelSize(R.dimen.screenshot_scrollview_bitmap_margintop);
        Paint rectPaint = new Paint();
        rectPaint.setStyle(Paint.Style.STROKE);
        rectPaint.setAntiAlias(false);
        rectPaint.setStrokeWidth(((float) bitmapStrokeWidth) / scale);
        rectPaint.setColor(getContext().getResources().getColor(R.color.screenshot_part_divider_color));
        int left = x;
        int top = bitmapMarginTop + bitmapStrokeWidth;
        int right = x + this.mSingleBitmap.getWidth();
        int bottom = top + this.mSingleBitmap.getHeight();
        Rect rect = new Rect(left, top, right, bottom);
        Canvas canvas2 = canvas;
        int i = x;
        canvas2.drawRect((float) (left - bitmapStrokeWidth), (float) top, (float) (right + bitmapStrokeWidth), (float) (bottom + bitmapStrokeWidth), rectPaint);
        Canvas canvas3 = canvas;
        canvas3.drawBitmap(this.mSingleBitmap, null, rect, null);
    }

    private void drawLongScreenshot(Canvas canvas) {
        canvas.clipRect(0, 0, getWidth(), this.mTotalHeight);
        this.mLongBitmapDrawable.draw(canvas);
        if (this.mBottomPart != null) {
            canvas.drawBitmap(this.mBottomPart, 0.0f, (float) (this.mTotalHeight - this.mBottomPart.getHeight()), null);
        }
    }

    public boolean hasOverlappingRendering() {
        return false;
    }

    public void resetToShortMode(boolean isAnimator) {
        if (this.mShowBigAnimator.isRunning()) {
            this.mShowBigAnimator.end();
        }
        if (this.mShowBig == 1.0f) {
            this.mShowBigAnimator.reverse();
            if (!isAnimator) {
                this.mShowBigAnimator.end();
            }
        }
    }

    public void setIsTakingLongScreenshot(boolean value) {
        this.mIsTakingLongScreenshot = value;
        this.mIsBuildingLongScreenshot = false;
    }

    public void setSingleBitmap(Bitmap bmp) {
        this.mSingleBitmap = bmp;
        resetState();
        invalidate();
    }

    public void setBitmaps(List<Bitmap> bitmaps, boolean resetState) {
        if (bitmaps == null) {
            this.mLongBitmapDrawable = null;
        } else {
            this.mLongBitmapDrawable = new LongScreenshotUtils.LongBitmapDrawable((Bitmap[]) bitmaps.toArray(new Bitmap[bitmaps.size()]));
        }
        if (resetState) {
            resetState();
        }
        invalidate();
    }

    public void gotoSingleBitmap() {
        float bigScale = calcScale(true, 1.0f);
        float minScale = calcScale(true, 0.0f);
        this.mGotoSingleAnimator = ValueAnimator.ofFloat(new float[]{((bigScale * calcScale(false)) - minScale) / (bigScale - minScale), 0.0f});
        this.mGotoSingleAnimator.setDuration(200);
        this.mGotoSingleAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                float unused = ScreenshotScrollView.this.mShowBig = ((Float) animation.getAnimatedValue()).floatValue();
                float unused2 = ScreenshotScrollView.this.mAnimatableOffsetY = (ScreenshotScrollView.this.mOffsetY * ScreenshotScrollView.this.mShowBig) + (((float) ScreenshotScrollView.this.getPaddingTop()) * ScreenshotScrollView.this.mShowBig * (1.0f - animation.getAnimatedFraction()));
                ScreenshotScrollView.this.invalidate();
            }
        });
        this.mGotoSingleAnimator.start();
    }

    private void resetState() {
        resetToShortMode(false);
        this.mIsManuTaking = false;
        this.mOffsetY = this.mMinOffsetY;
        this.mAnimatableOffsetY = 0.0f;
    }

    public void setBottomPart(Bitmap bottomBmp) {
        this.mBottomPart = bottomBmp;
        postInvalidate();
    }

    public boolean getIsManuTaking() {
        return this.mIsManuTaking;
    }

    public void setAnimatingCallback(AnimatingCallback callback) {
        this.mAnimatingCallback = callback;
    }

    public int getShowedPageCount() {
        return this.mShowedPageCount;
    }

    public void startAnimating() {
        startAnimating(true);
    }

    public void startAnimating(boolean reset) {
        this.mIsAnimatingStoped = false;
        if (reset) {
            this.mIsManuTaking = false;
            int i = getResources().getDisplayMetrics().heightPixels;
            this.mTotalHeight = i;
            this.mMinTotalHeight = i;
        }
        post(this.mAnimatingStepRunnable);
    }

    public void stopAnimating() {
        stopAnimating(true);
    }

    public void stopAnimating(boolean isFinal) {
        this.mIsAnimatingStoped = true;
        removeCallbacks(this.mAnimatingStepRunnable);
        if (isFinal) {
            this.mIsBuildingLongScreenshot = true;
            this.mMinOffsetY = (float) (getHeight() - this.mTotalHeight);
            this.mOffsetY = this.mMinOffsetY;
        }
    }

    public void autoCalcPadding() {
        Bitmap bmp = this.mSingleBitmap;
        int titleHeight = getResources().getDimensionPixelSize(R.dimen.screenshot_actionbar_back_height);
        Configuration config = this.mContext.getResources().getConfiguration();
        if (Constants.IS_NOTCH && config.orientation == 1) {
            titleHeight += this.mContext.getResources().getDimensionPixelSize(R.dimen.status_bar_height);
        }
        int paddingTop = getResources().getDimensionPixelSize(R.dimen.screenshot_bmp_paddingtop) + titleHeight;
        int paddingBottom = getResources().getDimensionPixelSize(R.dimen.screenshot_bmp_paddingbottom);
        if (config.orientation == 2) {
            paddingTop = getResources().getDimensionPixelSize(R.dimen.screenshot_bmp_paddingtop_landscape);
            paddingBottom = getResources().getDimensionPixelSize(R.dimen.screenshot_bmp_paddingbottom_landscape);
        }
        int paddingLeft = (getWidth() - ((((getHeight() - paddingTop) - paddingBottom) * bmp.getWidth()) / bmp.getHeight())) / 2;
        setPadding(paddingLeft, paddingTop, paddingLeft, paddingBottom);
        this.mOffsetY = 0.0f;
        this.mAnimatableOffsetY = 0.0f;
        this.mMinOffsetY = (float) (getHeight() - getResources().getDisplayMetrics().heightPixels);
        this.mMaxOffsetY = (float) getPaddingTop();
    }

    public Bitmap buildLongScreenshot() {
        try {
            Bitmap longScreenshot = Bitmap.createBitmap(this.mLongBitmapDrawable.getIntrinsicWidth(), this.mTotalHeight, Bitmap.Config.ARGB_8888);
            drawLongScreenshot(new Canvas(longScreenshot));
            return longScreenshot;
        } catch (OutOfMemoryError ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public int getWidthInner() {
        return (getWidth() - getPaddingLeft()) - getPaddingRight();
    }

    public int getHeightInner() {
        return (getHeight() - getPaddingTop()) - getPaddingBottom();
    }

    private int getMaxTotalHeight() {
        return this.mLongBitmapDrawable.getIntrinsicHeight();
    }

    /* access modifiers changed from: private */
    public void doAnimatingStep(int step) {
        this.mTotalHeight += step;
        this.mTotalHeight = Math.min(this.mTotalHeight, getMaxTotalHeight());
        this.mTotalHeight = Math.max(this.mTotalHeight, this.mMinTotalHeight);
        int newShowedPageCount = 0;
        int bmpsHeight = 0;
        for (Bitmap bmp : this.mLongBitmapDrawable.getBitmaps()) {
            bmpsHeight += bmp.getHeight();
            if (bmpsHeight > this.mTotalHeight) {
                break;
            }
            newShowedPageCount++;
        }
        if (newShowedPageCount != this.mShowedPageCount) {
            this.mShowedPageCount = newShowedPageCount;
            if (this.mAnimatingCallback != null) {
                this.mAnimatingCallback.onShowedPageCountChanged(this.mShowedPageCount);
            }
        }
        invalidate();
    }
}
