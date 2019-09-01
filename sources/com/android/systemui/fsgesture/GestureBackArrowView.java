package com.android.systemui.fsgesture;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.KeyguardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import com.android.systemui.R;
import com.android.systemui.recents.model.Task;

public class GestureBackArrowView extends View {
    private static final Interpolator ACCELERATE_DECELERATE_INTERPOLATOR = new AccelerateDecelerateInterpolator();
    private static final Interpolator CUBIC_EASE_OUT_INTERPOLATOR = new DecelerateInterpolator(1.5f);
    private static final Interpolator QUAD_EASE_OUT_INTERPOLATOR = new DecelerateInterpolator();
    private Bitmap mArrow;
    private ValueAnimator mArrowAnimator;
    private Rect mArrowDstRect;
    private int mArrowHeight;
    /* access modifiers changed from: private */
    public Paint mArrowPaint;
    /* access modifiers changed from: private */
    public boolean mArrowShown;
    private int mArrowWidth;
    private Rect mBackDstRect;
    private int mBackHeight;
    private int mBackWidth;
    private Paint mBgPaint;
    private ContentResolver mContentResolver;
    /* access modifiers changed from: private */
    public int mCurArrowAlpha;
    private float mCurrentY;
    private int mDisplayWidth;
    private float mExpectBackHeight;
    private int mIconHeight;
    /* access modifiers changed from: private */
    public boolean mIconNeedDraw;
    /* access modifiers changed from: private */
    public float mIconScale;
    private int mIconWidth;
    private KeyguardManager mKeyguardManager;
    /* access modifiers changed from: private */
    public ValueAnimator mLastIconAnimator;
    private Bitmap mLeftBackground;
    private Drawable mNoneTaskIcon;
    /* access modifiers changed from: private */
    public float mOffsetX;
    private int mPosition;
    /* access modifiers changed from: private */
    public ReadyState mReadyState;
    private Drawable mRecentTaskIcon;
    private Bitmap mRightBackground;
    /* access modifiers changed from: private */
    public float mScale;
    private float mStartX;
    private Vibrator mVibrator;
    private ValueAnimator mWaveChangeAnimator;

    enum ReadyState {
        READY_STATE_NONE,
        READY_STATE_BACK,
        READY_STATE_RECENT
    }

    public GestureBackArrowView(Context context, int position) {
        this(context, null, position);
    }

    public GestureBackArrowView(Context context, AttributeSet attrs, int position) {
        this(context, attrs, 0, position);
    }

    public GestureBackArrowView(Context context, AttributeSet attrs, int defStyleAttr, int position) {
        this(context, attrs, defStyleAttr, 0, position);
    }

    public GestureBackArrowView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes, int position) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mScale = 0.0f;
        this.mIconScale = 1.0f;
        this.mReadyState = ReadyState.READY_STATE_NONE;
        this.mKeyguardManager = (KeyguardManager) context.getSystemService("keyguard");
        this.mContentResolver = context.getContentResolver();
        this.mPosition = position;
        this.mBgPaint = new Paint(1);
        this.mBgPaint.setFilterBitmap(true);
        this.mBgPaint.setDither(true);
        this.mArrowPaint = new Paint(1);
        this.mArrowPaint.setFilterBitmap(true);
        this.mArrowPaint.setDither(true);
        this.mArrowPaint.setAlpha(0);
        this.mLeftBackground = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.gesture_back_background);
        Matrix matrix = new Matrix();
        matrix.postScale(1.0f, 1.0f);
        matrix.postRotate(180.0f);
        this.mRightBackground = Bitmap.createBitmap(this.mLeftBackground, 0, 0, this.mLeftBackground.getWidth(), this.mLeftBackground.getHeight(), matrix, true);
        this.mNoneTaskIcon = context.getDrawable(R.drawable.ic_quick_switch_empty);
        this.mIconWidth = this.mNoneTaskIcon.getIntrinsicWidth();
        this.mIconHeight = this.mNoneTaskIcon.getIntrinsicHeight();
        switch (this.mPosition) {
            case 0:
                this.mBackHeight = this.mLeftBackground.getHeight();
                this.mBackWidth = this.mLeftBackground.getWidth();
                break;
            case 1:
                this.mBackHeight = this.mRightBackground.getHeight();
                this.mBackWidth = this.mRightBackground.getWidth();
                break;
        }
        this.mArrow = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.gesture_back_arrow);
        this.mArrowHeight = this.mArrow.getHeight();
        this.mArrowWidth = this.mArrow.getWidth();
        this.mBackDstRect = new Rect();
        this.mArrowDstRect = new Rect();
        this.mVibrator = (Vibrator) getContext().getSystemService("vibrator");
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        int i;
        int i2;
        Canvas canvas2 = canvas;
        super.onDraw(canvas);
        int bgLeft = 0;
        int bgRight = 0;
        int arrowLeft = 0;
        int arrowRight = 0;
        int iconLeft = 0;
        int iconRight = 0;
        Bitmap curBitmap = this.mLeftBackground;
        float currentWidth = ((float) this.mBackWidth) * this.mScale;
        switch (this.mPosition) {
            case 0:
                bgLeft = (int) this.mStartX;
                bgRight = (int) (this.mStartX + currentWidth);
                arrowLeft = (int) (this.mStartX + ((currentWidth - (((float) this.mArrowWidth) * this.mIconScale)) / 2.0f));
                arrowRight = (int) (this.mStartX + (((((float) this.mArrowWidth) * this.mIconScale) + currentWidth) / 2.0f));
                if (currentWidth < ((float) this.mIconWidth) * this.mIconScale) {
                    i = (int) (this.mStartX + currentWidth);
                } else {
                    i = (int) (this.mStartX + (((((float) this.mIconWidth) * this.mIconScale) + currentWidth) / 2.0f));
                }
                iconRight = i;
                iconLeft = (int) (((float) iconRight) - (((float) this.mIconWidth) * this.mIconScale));
                break;
            case 1:
                curBitmap = this.mRightBackground;
                bgLeft = this.mDisplayWidth - ((int) ((((float) this.mBackWidth) * this.mScale) + this.mStartX));
                bgRight = this.mDisplayWidth - ((int) this.mStartX);
                arrowLeft = this.mDisplayWidth - ((int) (this.mStartX + (((((float) this.mArrowWidth) * this.mIconScale) + currentWidth) / 2.0f)));
                arrowRight = this.mDisplayWidth - ((int) (this.mStartX + ((currentWidth - (((float) this.mArrowWidth) * this.mIconScale)) / 2.0f)));
                int i3 = this.mDisplayWidth;
                if (currentWidth < ((float) this.mIconWidth) * this.mIconScale) {
                    i2 = (int) (this.mStartX + currentWidth);
                } else {
                    i2 = (int) (this.mStartX + (((((float) this.mIconWidth) * this.mIconScale) + currentWidth) / 2.0f));
                }
                iconLeft = i3 - i2;
                iconRight = (int) (((float) iconLeft) + (((float) this.mIconWidth) * this.mIconScale));
                break;
        }
        this.mBackDstRect.set(bgLeft, (int) (this.mCurrentY - (this.mExpectBackHeight / 2.0f)), bgRight, (int) (this.mCurrentY + (this.mExpectBackHeight / 2.0f)));
        canvas2.drawBitmap(curBitmap, null, this.mBackDstRect, this.mBgPaint);
        if (this.mReadyState == ReadyState.READY_STATE_BACK || this.mReadyState == ReadyState.READY_STATE_RECENT) {
            if (!this.mArrowShown) {
                this.mIconNeedDraw = true;
                startArrowAnimating(true, 100);
                this.mArrowShown = true;
            }
        } else if (this.mArrowShown) {
            startArrowAnimating(false, 50);
            this.mArrowShown = false;
        }
        if (this.mIconNeedDraw && ((double) this.mScale) > 0.1d) {
            if (this.mReadyState == ReadyState.READY_STATE_BACK) {
                this.mArrowDstRect.set(arrowLeft, (int) (this.mCurrentY - ((((float) this.mArrowHeight) * this.mIconScale) / 2.0f)), arrowRight, (int) (this.mCurrentY + ((((float) this.mArrowHeight) * this.mIconScale) / 2.0f)));
                canvas2.drawBitmap(this.mArrow, null, this.mArrowDstRect, this.mArrowPaint);
            } else if (this.mRecentTaskIcon != null && this.mScale != 0.0f) {
                this.mRecentTaskIcon.setBounds(iconLeft, (int) (this.mCurrentY - ((((float) this.mIconHeight) * this.mIconScale) / 2.0f)), iconRight, (int) (this.mCurrentY + ((((float) this.mIconHeight) * this.mIconScale) / 2.0f)));
                this.mRecentTaskIcon.draw(canvas2);
            }
        }
    }

    private void startArrowAnimating(final boolean show, int duration) {
        if (this.mArrowAnimator != null) {
            this.mArrowAnimator.cancel();
        }
        int[] iArr = new int[2];
        int i = 0;
        iArr[0] = this.mCurArrowAlpha;
        if (show) {
            i = 255;
        }
        iArr[1] = i;
        this.mArrowAnimator = ValueAnimator.ofInt(iArr);
        this.mArrowAnimator.setDuration((long) duration);
        this.mArrowAnimator.setInterpolator(CUBIC_EASE_OUT_INTERPOLATOR);
        this.mArrowAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                int alpha = ((Integer) animation.getAnimatedValue()).intValue();
                GestureBackArrowView.this.mArrowPaint.setAlpha(alpha);
                GestureBackArrowView.this.invalidate();
                if (alpha == 0 && !show) {
                    boolean unused = GestureBackArrowView.this.mIconNeedDraw = false;
                }
                int unused2 = GestureBackArrowView.this.mCurArrowAlpha = alpha;
            }
        });
        this.mArrowAnimator.start();
    }

    /* access modifiers changed from: package-private */
    public void setReadyFinish(ReadyState nextState) {
        if (nextState != ReadyState.READY_STATE_RECENT) {
            this.mRecentTaskIcon = null;
        } else if (this.mRecentTaskIcon == null || this.mRecentTaskIcon == this.mNoneTaskIcon) {
            this.mRecentTaskIcon = loadRecentTaskIcon();
        }
        if (nextState != this.mReadyState) {
            if (this.mReadyState == ReadyState.READY_STATE_BACK && nextState == ReadyState.READY_STATE_RECENT) {
                changeScale(this.mScale, 1.17f, 200, false);
                this.mVibrator.vibrate(20);
            } else if (this.mReadyState == ReadyState.READY_STATE_RECENT) {
                changeScale(this.mScale, 1.0f, 200, true);
            }
            this.mReadyState = nextState;
        }
    }

    /* access modifiers changed from: package-private */
    public ReadyState getCurrentState() {
        return this.mReadyState;
    }

    private void changeScale(final float start, float end, int time, final boolean isAdjustWithScale) {
        if (this.mWaveChangeAnimator != null) {
            this.mWaveChangeAnimator.cancel();
        }
        this.mWaveChangeAnimator = ValueAnimator.ofFloat(new float[]{start, end});
        this.mWaveChangeAnimator.setDuration((long) time);
        this.mWaveChangeAnimator.setInterpolator(CUBIC_EASE_OUT_INTERPOLATOR);
        this.mWaveChangeAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                if (isAdjustWithScale) {
                    float unused = GestureBackArrowView.this.mScale = start + (((GesturesBackController.convertOffset(GestureBackArrowView.this.mOffsetX) / 20.0f) - start) * animation.getAnimatedFraction());
                } else {
                    float unused2 = GestureBackArrowView.this.mScale = ((Float) animation.getAnimatedValue()).floatValue();
                }
                GestureBackArrowView.this.invalidate();
            }
        });
        this.mWaveChangeAnimator.start();
        if (this.mLastIconAnimator != null) {
            this.mLastIconAnimator.cancel();
        }
        this.mLastIconAnimator = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
        this.mLastIconAnimator.setDuration(100);
        this.mLastIconAnimator.setInterpolator(QUAD_EASE_OUT_INTERPOLATOR);
        this.mLastIconAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                if (GestureBackArrowView.this.mReadyState == ReadyState.READY_STATE_NONE) {
                    GestureBackArrowView.this.mLastIconAnimator.cancel();
                }
                float unused = GestureBackArrowView.this.mIconScale = ((Float) animation.getAnimatedValue()).floatValue();
            }
        });
        this.mLastIconAnimator.start();
    }

    private Drawable loadRecentTaskIcon() {
        if (!GestureStubView.supportNextTask(this.mKeyguardManager, this.mContentResolver)) {
            return this.mNoneTaskIcon;
        }
        Task task = GestureStubView.getNextTask(getContext(), false, -1);
        return (task == null || task.icon == null) ? this.mNoneTaskIcon : task.icon;
    }

    /* access modifiers changed from: package-private */
    public void setDisplayWidth(int width) {
        this.mDisplayWidth = width;
    }

    /* access modifiers changed from: package-private */
    public void onActionDown(float y, float startX, float expectBackHeight) {
        if (expectBackHeight > 0.0f) {
            this.mExpectBackHeight = expectBackHeight;
            this.mCurrentY = y;
        } else {
            this.mExpectBackHeight = (float) this.mBackHeight;
            this.mCurrentY = y - 20.0f;
        }
        this.mStartX = startX;
        this.mArrowPaint.setAlpha(0);
        this.mArrowShown = false;
        this.mIconNeedDraw = false;
    }

    /* access modifiers changed from: package-private */
    public void onActionMove(float x) {
        this.mOffsetX = x;
        if (!skipChangeScaleOnAcitonMove()) {
            this.mScale = GesturesBackController.convertOffset(x) / 20.0f;
            invalidate();
        }
    }

    private boolean skipChangeScaleOnAcitonMove() {
        return this.mReadyState == ReadyState.READY_STATE_RECENT || (this.mWaveChangeAnimator != null && this.mWaveChangeAnimator.isRunning());
    }

    /* access modifiers changed from: package-private */
    public void onActionUp(float x, Animator.AnimatorListener listener) {
        if (this.mArrowAnimator != null) {
            this.mArrowAnimator.cancel();
        }
        if (this.mWaveChangeAnimator != null) {
            this.mWaveChangeAnimator.cancel();
        }
        if (this.mLastIconAnimator != null) {
            this.mLastIconAnimator.cancel();
        }
        this.mIconScale = 1.0f;
        this.mScale = x / 20.0f;
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(new float[]{this.mScale, 0.0f});
        valueAnimator.setDuration(100);
        valueAnimator.setInterpolator(QUAD_EASE_OUT_INTERPOLATOR);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                float unused = GestureBackArrowView.this.mScale = ((Float) animation.getAnimatedValue()).floatValue();
                long currentPlayTime = animation.getCurrentPlayTime();
                if (currentPlayTime > 0 && currentPlayTime < 50) {
                    boolean unused2 = GestureBackArrowView.this.mIconNeedDraw = GestureBackArrowView.this.mArrowShown = false;
                }
                GestureBackArrowView.this.invalidate();
            }
        });
        if (listener != null) {
            valueAnimator.addListener(listener);
        }
        valueAnimator.start();
        this.mReadyState = ReadyState.READY_STATE_NONE;
    }

    /* access modifiers changed from: package-private */
    public void reset() {
        this.mScale = 0.0f;
        onActionDown(-1000.0f, 0.0f, -1.0f);
        this.mReadyState = ReadyState.READY_STATE_NONE;
        invalidate();
    }
}
