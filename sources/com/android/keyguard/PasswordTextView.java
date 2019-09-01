package com.android.keyguard;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.EditText;
import com.android.systemui.R;
import java.util.ArrayList;
import java.util.Stack;

public class PasswordTextView extends View {
    /* access modifiers changed from: private */
    public Interpolator mAppearInterpolator;
    /* access modifiers changed from: private */
    public int mCharPadding;
    /* access modifiers changed from: private */
    public Stack<CharState> mCharPool;
    /* access modifiers changed from: private */
    public Interpolator mDisappearInterpolator;
    /* access modifiers changed from: private */
    public int mDotSize;
    /* access modifiers changed from: private */
    public final Paint mDrawPaint;
    private Interpolator mFastOutSlowInInterpolator;
    private final int mGravity;
    private PowerManager mPM;
    boolean mShowPassword;
    private String mText;
    public TextChangeListener mTextChangeListener;
    /* access modifiers changed from: private */
    public ArrayList<CharState> mTextChars;
    private final int mTextHeightRaw;
    private UserActivityListener mUserActivityListener;

    private class CharState {
        float currentDotSizeFactor;
        float currentTextSizeFactor;
        float currentTextTranslationY;
        float currentWidthFactor;
        boolean dotAnimationIsGrowing;
        Animator dotAnimator;
        Animator.AnimatorListener dotFinishListener;
        private ValueAnimator.AnimatorUpdateListener dotSizeUpdater;
        private Runnable dotSwapperRunnable;
        boolean isDotSwapPending;
        Animator.AnimatorListener removeEndListener;
        boolean textAnimationIsGrowing;
        ValueAnimator textAnimator;
        Animator.AnimatorListener textFinishListener;
        private ValueAnimator.AnimatorUpdateListener textSizeUpdater;
        ValueAnimator textTranslateAnimator;
        Animator.AnimatorListener textTranslateFinishListener;
        private ValueAnimator.AnimatorUpdateListener textTranslationUpdater;
        char whichChar;
        boolean widthAnimationIsGrowing;
        ValueAnimator widthAnimator;
        Animator.AnimatorListener widthFinishListener;
        private ValueAnimator.AnimatorUpdateListener widthUpdater;

        private CharState() {
            this.currentTextTranslationY = 1.0f;
            this.removeEndListener = new AnimatorListenerAdapter() {
                private boolean mCancelled;

                public void onAnimationCancel(Animator animation) {
                    this.mCancelled = true;
                }

                public void onAnimationEnd(Animator animation) {
                    if (!this.mCancelled) {
                        PasswordTextView.this.mTextChars.remove(CharState.this);
                        PasswordTextView.this.mCharPool.push(CharState.this);
                        CharState.this.reset();
                        CharState.this.cancelAnimator(CharState.this.textTranslateAnimator);
                        CharState.this.textTranslateAnimator = null;
                    }
                }

                public void onAnimationStart(Animator animation) {
                    this.mCancelled = false;
                }
            };
            this.dotFinishListener = new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    CharState.this.dotAnimator = null;
                }
            };
            this.textFinishListener = new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    CharState.this.textAnimator = null;
                }
            };
            this.textTranslateFinishListener = new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    CharState.this.textTranslateAnimator = null;
                }
            };
            this.widthFinishListener = new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    CharState.this.widthAnimator = null;
                }
            };
            this.dotSizeUpdater = new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    CharState.this.currentDotSizeFactor = ((Float) animation.getAnimatedValue()).floatValue();
                    PasswordTextView.this.invalidate();
                }
            };
            this.textSizeUpdater = new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    CharState.this.currentTextSizeFactor = ((Float) animation.getAnimatedValue()).floatValue();
                    PasswordTextView.this.invalidate();
                }
            };
            this.textTranslationUpdater = new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    CharState.this.currentTextTranslationY = ((Float) animation.getAnimatedValue()).floatValue();
                    PasswordTextView.this.invalidate();
                }
            };
            this.widthUpdater = new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    CharState.this.currentWidthFactor = ((Float) animation.getAnimatedValue()).floatValue();
                    PasswordTextView.this.invalidate();
                }
            };
            this.dotSwapperRunnable = new Runnable() {
                public void run() {
                    CharState.this.performSwap();
                    CharState.this.isDotSwapPending = false;
                }
            };
        }

        /* access modifiers changed from: package-private */
        public void reset() {
            this.whichChar = 0;
            this.currentTextSizeFactor = 0.0f;
            this.currentDotSizeFactor = 0.0f;
            this.currentWidthFactor = 0.0f;
            cancelAnimator(this.textAnimator);
            this.textAnimator = null;
            cancelAnimator(this.dotAnimator);
            this.dotAnimator = null;
            cancelAnimator(this.widthAnimator);
            this.widthAnimator = null;
            this.currentTextTranslationY = 1.0f;
            removeDotSwapCallbacks();
        }

        /* access modifiers changed from: private */
        public void startRemoveAnimation(long startDelay, long widthDelay) {
            boolean z = false;
            boolean dotNeedsAnimation = (this.currentDotSizeFactor > 0.0f && this.dotAnimator == null) || (this.dotAnimator != null && this.dotAnimationIsGrowing);
            boolean textNeedsAnimation = (this.currentTextSizeFactor > 0.0f && this.textAnimator == null) || (this.textAnimator != null && this.textAnimationIsGrowing);
            if ((this.currentWidthFactor > 0.0f && this.widthAnimator == null) || (this.widthAnimator != null && this.widthAnimationIsGrowing)) {
                z = true;
            }
            boolean widthNeedsAnimation = z;
            if (dotNeedsAnimation) {
                startDotDisappearAnimation(startDelay);
            }
            if (textNeedsAnimation) {
                startTextDisappearAnimation(startDelay);
            }
            if (widthNeedsAnimation) {
                startWidthDisappearAnimation(widthDelay);
            }
        }

        /* access modifiers changed from: private */
        public void startAppearAnimation() {
            boolean widthNeedsAnimation = false;
            boolean dotNeedsAnimation = !PasswordTextView.this.mShowPassword && (this.dotAnimator == null || !this.dotAnimationIsGrowing);
            boolean textNeedsAnimation = PasswordTextView.this.mShowPassword && (this.textAnimator == null || !this.textAnimationIsGrowing);
            if (this.widthAnimator == null || !this.widthAnimationIsGrowing) {
                widthNeedsAnimation = true;
            }
            if (dotNeedsAnimation) {
                startDotAppearAnimation(0);
            }
            if (textNeedsAnimation) {
                startTextAppearAnimation();
            }
            if (widthNeedsAnimation) {
                startWidthAppearAnimation();
            }
            if (PasswordTextView.this.mShowPassword) {
                postDotSwap(1300);
            }
        }

        private void postDotSwap(long delay) {
            removeDotSwapCallbacks();
            PasswordTextView.this.postDelayed(this.dotSwapperRunnable, delay);
            this.isDotSwapPending = true;
        }

        /* access modifiers changed from: private */
        public void removeDotSwapCallbacks() {
            PasswordTextView.this.removeCallbacks(this.dotSwapperRunnable);
            this.isDotSwapPending = false;
        }

        /* access modifiers changed from: package-private */
        public void swapToDotWhenAppearFinished() {
            removeDotSwapCallbacks();
            if (this.textAnimator != null) {
                postDotSwap(100 + (this.textAnimator.getDuration() - this.textAnimator.getCurrentPlayTime()));
            } else {
                performSwap();
            }
        }

        /* access modifiers changed from: private */
        public void performSwap() {
            startTextDisappearAnimation(0);
            startDotAppearAnimation(30);
        }

        private void startWidthDisappearAnimation(long widthDelay) {
            cancelAnimator(this.widthAnimator);
            this.widthAnimator = ValueAnimator.ofFloat(new float[]{this.currentWidthFactor, 0.0f});
            this.widthAnimator.addUpdateListener(this.widthUpdater);
            this.widthAnimator.addListener(this.widthFinishListener);
            this.widthAnimator.addListener(this.removeEndListener);
            this.widthAnimator.setDuration((long) (160.0f * this.currentWidthFactor));
            this.widthAnimator.setStartDelay(widthDelay);
            this.widthAnimator.start();
            this.widthAnimationIsGrowing = false;
        }

        private void startTextDisappearAnimation(long startDelay) {
            cancelAnimator(this.textAnimator);
            this.textAnimator = ValueAnimator.ofFloat(new float[]{this.currentTextSizeFactor, 0.0f});
            this.textAnimator.addUpdateListener(this.textSizeUpdater);
            this.textAnimator.addListener(this.textFinishListener);
            this.textAnimator.setInterpolator(PasswordTextView.this.mDisappearInterpolator);
            this.textAnimator.setDuration((long) (160.0f * this.currentTextSizeFactor));
            this.textAnimator.setStartDelay(startDelay);
            this.textAnimator.start();
            this.textAnimationIsGrowing = false;
        }

        private void startDotDisappearAnimation(long startDelay) {
            cancelAnimator(this.dotAnimator);
            ValueAnimator animator = ValueAnimator.ofFloat(new float[]{this.currentDotSizeFactor, 0.0f});
            animator.addUpdateListener(this.dotSizeUpdater);
            animator.addListener(this.dotFinishListener);
            animator.setInterpolator(PasswordTextView.this.mDisappearInterpolator);
            animator.setDuration((long) (160.0f * Math.min(this.currentDotSizeFactor, 1.0f)));
            animator.setStartDelay(startDelay);
            animator.start();
            this.dotAnimator = animator;
            this.dotAnimationIsGrowing = false;
        }

        private void startWidthAppearAnimation() {
            cancelAnimator(this.widthAnimator);
            this.widthAnimator = ValueAnimator.ofFloat(new float[]{this.currentWidthFactor, 1.0f});
            this.widthAnimator.addUpdateListener(this.widthUpdater);
            this.widthAnimator.addListener(this.widthFinishListener);
            this.widthAnimator.setDuration((long) (160.0f * (1.0f - this.currentWidthFactor)));
            this.widthAnimator.start();
            this.widthAnimationIsGrowing = true;
        }

        private void startTextAppearAnimation() {
            cancelAnimator(this.textAnimator);
            this.textAnimator = ValueAnimator.ofFloat(new float[]{this.currentTextSizeFactor, 1.0f});
            this.textAnimator.addUpdateListener(this.textSizeUpdater);
            this.textAnimator.addListener(this.textFinishListener);
            this.textAnimator.setInterpolator(PasswordTextView.this.mAppearInterpolator);
            this.textAnimator.setDuration((long) (160.0f * (1.0f - this.currentTextSizeFactor)));
            this.textAnimator.start();
            this.textAnimationIsGrowing = true;
            if (this.textTranslateAnimator == null) {
                this.textTranslateAnimator = ValueAnimator.ofFloat(new float[]{1.0f, 0.0f});
                this.textTranslateAnimator.addUpdateListener(this.textTranslationUpdater);
                this.textTranslateAnimator.addListener(this.textTranslateFinishListener);
                this.textTranslateAnimator.setInterpolator(PasswordTextView.this.mAppearInterpolator);
                this.textTranslateAnimator.setDuration(160);
                this.textTranslateAnimator.start();
            }
        }

        private void startDotAppearAnimation(long delay) {
            cancelAnimator(this.dotAnimator);
            if (!PasswordTextView.this.mShowPassword) {
                ValueAnimator overShootAnimator = ValueAnimator.ofFloat(new float[]{this.currentDotSizeFactor, 1.5f});
                overShootAnimator.addUpdateListener(this.dotSizeUpdater);
                overShootAnimator.setInterpolator(PasswordTextView.this.mAppearInterpolator);
                overShootAnimator.setDuration(160);
                ValueAnimator settleBackAnimator = ValueAnimator.ofFloat(new float[]{1.5f, 1.0f});
                settleBackAnimator.addUpdateListener(this.dotSizeUpdater);
                settleBackAnimator.setDuration(320 - 160);
                settleBackAnimator.addListener(this.dotFinishListener);
                AnimatorSet animatorSet = new AnimatorSet();
                animatorSet.playSequentially(new Animator[]{overShootAnimator, settleBackAnimator});
                animatorSet.setStartDelay(delay);
                animatorSet.start();
                this.dotAnimator = animatorSet;
            } else {
                ValueAnimator growAnimator = ValueAnimator.ofFloat(new float[]{this.currentDotSizeFactor, 1.0f});
                growAnimator.addUpdateListener(this.dotSizeUpdater);
                growAnimator.setDuration((long) (160.0f * (1.0f - this.currentDotSizeFactor)));
                growAnimator.addListener(this.dotFinishListener);
                growAnimator.setStartDelay(delay);
                growAnimator.start();
                this.dotAnimator = growAnimator;
            }
            this.dotAnimationIsGrowing = true;
        }

        /* access modifiers changed from: private */
        public void cancelAnimator(Animator animator) {
            if (animator != null) {
                animator.cancel();
            }
        }

        public float draw(Canvas canvas, float currentDrawPosition, int charHeight, float yPosition, float charLength) {
            boolean dotVisible = false;
            boolean textVisible = this.currentTextSizeFactor > 0.0f;
            if (this.currentDotSizeFactor > 0.0f) {
                dotVisible = true;
            }
            float charWidth = this.currentWidthFactor * charLength;
            if (textVisible) {
                float currYPosition = ((((float) charHeight) / 2.0f) * this.currentTextSizeFactor) + yPosition + (((float) charHeight) * this.currentTextTranslationY * 0.8f);
                canvas.save();
                canvas.translate((charWidth / 2.0f) + currentDrawPosition, currYPosition);
                canvas.scale(this.currentTextSizeFactor, this.currentTextSizeFactor);
                canvas.drawText(Character.toString(this.whichChar), 0.0f, 0.0f, PasswordTextView.this.mDrawPaint);
                canvas.restore();
            }
            if (dotVisible) {
                canvas.save();
                canvas.translate((charWidth / 2.0f) + currentDrawPosition, yPosition);
                canvas.drawCircle(0.0f, 0.0f, ((float) (PasswordTextView.this.mDotSize / 2)) * this.currentDotSizeFactor, PasswordTextView.this.mDrawPaint);
                canvas.restore();
            }
            return (((float) PasswordTextView.this.mCharPadding) * this.currentWidthFactor) + charWidth;
        }
    }

    public interface TextChangeListener {
        void onTextChanged(int i);
    }

    public interface UserActivityListener {
        void onUserActivity();
    }

    public PasswordTextView(Context context) {
        this(context, null);
    }

    public PasswordTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PasswordTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    /* JADX INFO: finally extract failed */
    public PasswordTextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mTextChars = new ArrayList<>();
        this.mText = "";
        this.mCharPool = new Stack<>();
        this.mDrawPaint = new Paint();
        boolean z = true;
        setFocusableInTouchMode(true);
        setFocusable(true);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PasswordTextView);
        try {
            this.mTextHeightRaw = a.getInt(3, 0);
            this.mGravity = a.getInt(0, 17);
            this.mDotSize = a.getDimensionPixelSize(2, getContext().getResources().getDimensionPixelSize(R.dimen.password_dot_size));
            this.mCharPadding = a.getDimensionPixelSize(1, getContext().getResources().getDimensionPixelSize(R.dimen.pin_puk_password_char_padding));
            a.recycle();
            this.mDrawPaint.setFlags(129);
            this.mDrawPaint.setTextAlign(Paint.Align.CENTER);
            this.mDrawPaint.setColor(-1);
            this.mDrawPaint.setTypeface(Typeface.create("sans-serif-light", 0));
            this.mShowPassword = Settings.System.getInt(this.mContext.getContentResolver(), "show_password", 1) != 1 ? false : z;
            this.mAppearInterpolator = AnimationUtils.loadInterpolator(this.mContext, 17563662);
            this.mDisappearInterpolator = AnimationUtils.loadInterpolator(this.mContext, 17563663);
            this.mFastOutSlowInInterpolator = AnimationUtils.loadInterpolator(this.mContext, 17563661);
            this.mPM = (PowerManager) this.mContext.getSystemService("power");
        } catch (Throwable th) {
            a.recycle();
            throw th;
        }
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        float currentDrawPosition;
        float totalDrawingWidth = getDrawingWidth();
        if ((this.mGravity & 7) != 3) {
            currentDrawPosition = ((float) (getWidth() / 2)) - (totalDrawingWidth / 2.0f);
        } else if ((this.mGravity & 8388608) == 0 || getLayoutDirection() != 1) {
            currentDrawPosition = (float) getPaddingLeft();
        } else {
            currentDrawPosition = ((float) (getWidth() - getPaddingRight())) - totalDrawingWidth;
        }
        int length = this.mTextChars.size();
        Rect bounds = getCharBounds();
        int charHeight = bounds.bottom - bounds.top;
        float yPosition = (float) ((((getHeight() - getPaddingBottom()) - getPaddingTop()) / 2) + getPaddingTop());
        Canvas canvas2 = canvas;
        canvas2.clipRect(getPaddingLeft(), getPaddingTop(), getWidth() - getPaddingRight(), getHeight() - getPaddingBottom());
        float charLength = (float) (bounds.right - bounds.left);
        if (totalDrawingWidth > ((float) getWidth())) {
            currentDrawPosition -= (totalDrawingWidth - ((float) getWidth())) / 2.0f;
        }
        for (int i = 0; i < length; i++) {
            currentDrawPosition += this.mTextChars.get(i).draw(canvas2, currentDrawPosition, charHeight, yPosition, charLength);
        }
    }

    private Rect getCharBounds() {
        this.mDrawPaint.setTextSize(((float) this.mTextHeightRaw) * getResources().getDisplayMetrics().scaledDensity);
        Rect bounds = new Rect();
        this.mDrawPaint.getTextBounds("0", 0, 1, bounds);
        return bounds;
    }

    private float getDrawingWidth() {
        int width = 0;
        int length = this.mTextChars.size();
        Rect bounds = getCharBounds();
        int charLength = bounds.right - bounds.left;
        for (int i = 0; i < length; i++) {
            CharState charState = this.mTextChars.get(i);
            if (i != 0) {
                width = (int) (((float) width) + (((float) this.mCharPadding) * charState.currentWidthFactor));
            }
            width = (int) (((float) width) + (((float) charLength) * charState.currentWidthFactor));
        }
        return (float) width;
    }

    public void addTextChangedListener(TextChangeListener listener) {
        this.mTextChangeListener = listener;
    }

    public void removeTextChangedListener() {
        this.mTextChangeListener = null;
    }

    public boolean hasOverlappingRendering() {
        return false;
    }

    public void append(char c) {
        CharState charState;
        int visibleChars = this.mTextChars.size();
        String textbefore = this.mText;
        this.mText += c;
        int newLength = this.mText.length();
        if (this.mTextChangeListener != null) {
            this.mTextChangeListener.onTextChanged(newLength);
        }
        if (newLength > visibleChars) {
            charState = obtainCharState(c);
            this.mTextChars.add(charState);
        } else {
            charState = this.mTextChars.get(newLength - 1);
            charState.whichChar = c;
        }
        charState.startAppearAnimation();
        if (newLength > 1) {
            CharState previousState = this.mTextChars.get(newLength - 2);
            if (previousState.isDotSwapPending) {
                previousState.swapToDotWhenAppearFinished();
            }
        }
        userActivity();
        sendAccessibilityEventTypeViewTextChanged(textbefore, textbefore.length(), 0, 1);
    }

    public void setUserActivityListener(UserActivityListener userActivitiListener) {
        this.mUserActivityListener = userActivitiListener;
    }

    private void userActivity() {
        this.mPM.userActivity(SystemClock.uptimeMillis(), false);
        if (this.mUserActivityListener != null) {
            this.mUserActivityListener.onUserActivity();
        }
    }

    public void deleteLastChar() {
        int length = this.mText.length();
        String textbefore = this.mText;
        if (length > 0) {
            this.mText = this.mText.substring(0, length - 1);
            this.mTextChars.get(length - 1).startRemoveAnimation(0, 0);
            sendAccessibilityEventTypeViewTextChanged(textbefore, textbefore.length() - 1, 1, 0);
        }
        userActivity();
    }

    public String getText() {
        return this.mText;
    }

    private CharState obtainCharState(char c) {
        CharState charState;
        if (this.mCharPool.isEmpty()) {
            charState = new CharState();
        } else {
            charState = this.mCharPool.pop();
            charState.reset();
        }
        charState.whichChar = c;
        return charState;
    }

    public void reset(boolean animated, boolean announce) {
        int i;
        int distToMiddle;
        String textbefore = this.mText;
        this.mText = "";
        int length = this.mTextChars.size();
        int middleIndex = (length - 1) / 2;
        int i2 = 0;
        while (i2 < length) {
            CharState charState = this.mTextChars.get(i2);
            if (animated) {
                if (i2 <= middleIndex) {
                    distToMiddle = i2 * 2;
                } else {
                    distToMiddle = (length - 1) - (((i2 - middleIndex) - 1) * 2);
                }
                i = i2;
                charState.startRemoveAnimation(Math.min(((long) distToMiddle) * 40, 200), Math.min(((long) (length - 1)) * 40, 200) + 160);
                charState.removeDotSwapCallbacks();
            } else {
                i = i2;
                this.mCharPool.push(charState);
            }
            i2 = i + 1;
        }
        if (!animated) {
            this.mTextChars.clear();
        }
        if (announce) {
            sendAccessibilityEventTypeViewTextChanged(textbefore, 0, textbefore.length(), 0);
        }
    }

    /* access modifiers changed from: package-private */
    public void sendAccessibilityEventTypeViewTextChanged(String beforeText, int fromIndex, int removedCount, int addedCount) {
        if (!AccessibilityManager.getInstance(this.mContext).isEnabled()) {
            return;
        }
        if (isFocused() || (isSelected() && isShown())) {
            AccessibilityEvent event = AccessibilityEvent.obtain(16);
            event.setFromIndex(fromIndex);
            event.setRemovedCount(removedCount);
            event.setAddedCount(addedCount);
            event.setBeforeText(beforeText);
            event.setPassword(true);
            sendAccessibilityEventUnchecked(event);
        }
    }

    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(EditText.class.getName());
        event.setPassword(true);
    }

    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(PasswordTextView.class.getName());
        info.setPassword(true);
        info.setEditable(true);
        info.setInputType(16);
    }
}
