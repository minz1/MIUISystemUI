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
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.security.MiuiLockPatternUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.EditText;
import com.android.keyguard.PasswordTextView;
import com.android.systemui.R;
import java.util.ArrayList;
import java.util.Iterator;

public class PasswordTextViewForPIN extends PasswordTextView {
    /* access modifiers changed from: private */
    public Interpolator mAppearInterpolator;
    /* access modifiers changed from: private */
    public int mCharPadding;
    /* access modifiers changed from: private */
    public Interpolator mDisappearInterpolator;
    /* access modifiers changed from: private */
    public int mDotSize;
    /* access modifiers changed from: private */
    public final Paint mDrawPaint;
    private Interpolator mFastOutSlowInInterpolator;
    private final int mGravity;
    private Handler mHandler;
    /* access modifiers changed from: private */
    public boolean mIsResetAnimating;
    private PowerManager mPM;
    private int mPasswordLength;
    Runnable mResetAnimRunnable;
    boolean mShowPassword;
    private String mText;
    /* access modifiers changed from: private */
    public ArrayList<CharState> mTextChars;
    private final int mTextHeightRaw;
    private PasswordTextView.UserActivityListener mUserActivityListener;
    private int mWidth;

    private class CharState {
        float currentDotSizeFactor;
        boolean dotAnimationIsGrowing;
        Animator dotAnimator;
        Animator.AnimatorListener dotFinishListener;
        private ValueAnimator.AnimatorUpdateListener dotSizeUpdater;
        boolean isVisible;
        Animator.AnimatorListener removeDotFinishListener;

        private CharState() {
            this.dotFinishListener = new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    CharState.this.dotAnimator = null;
                }
            };
            this.removeDotFinishListener = new AnimatorListenerAdapter() {
                private boolean mCancelled;

                public void onAnimationCancel(Animator animation) {
                    this.mCancelled = true;
                }

                public void onAnimationEnd(Animator animation) {
                    CharState.this.isVisible = false;
                    if (!this.mCancelled) {
                        CharState.this.reset();
                        CharState.this.dotAnimator = null;
                    }
                    if (PasswordTextViewForPIN.this.mIsResetAnimating && PasswordTextViewForPIN.this.getVisibleTextCharSize() == 0) {
                        boolean unused = PasswordTextViewForPIN.this.mIsResetAnimating = false;
                    }
                }
            };
            this.dotSizeUpdater = new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    CharState.this.currentDotSizeFactor = ((Float) animation.getAnimatedValue()).floatValue();
                    PasswordTextViewForPIN.this.invalidate();
                }
            };
        }

        /* access modifiers changed from: package-private */
        public void reset() {
            this.currentDotSizeFactor = 0.0f;
            cancelAnimator(this.dotAnimator);
            this.dotAnimator = null;
            this.isVisible = false;
        }

        /* access modifiers changed from: private */
        public void startRemoveAnimation(long startDelay) {
            if ((this.currentDotSizeFactor > 0.0f && this.dotAnimator == null) || (this.dotAnimator != null && this.dotAnimationIsGrowing)) {
                startDotDisappearAnimation(startDelay);
            }
        }

        /* access modifiers changed from: private */
        public void startAppearAnimation() {
            if (this.dotAnimator == null || !this.dotAnimationIsGrowing) {
                this.isVisible = true;
                startDotAppearAnimation(0);
            }
        }

        private void startDotDisappearAnimation(long startDelay) {
            cancelAnimator(this.dotAnimator);
            ValueAnimator animator = ValueAnimator.ofFloat(new float[]{this.currentDotSizeFactor, 0.0f});
            animator.addUpdateListener(this.dotSizeUpdater);
            animator.addListener(this.removeDotFinishListener);
            animator.setInterpolator(PasswordTextViewForPIN.this.mDisappearInterpolator);
            animator.setDuration((long) (160.0f * Math.min(this.currentDotSizeFactor, 1.0f)));
            animator.setStartDelay(startDelay);
            animator.start();
            this.dotAnimator = animator;
            this.dotAnimationIsGrowing = false;
        }

        private void startDotAppearAnimation(long delay) {
            cancelAnimator(this.dotAnimator);
            ValueAnimator overShootAnimator = ValueAnimator.ofFloat(new float[]{this.currentDotSizeFactor, 1.5f});
            overShootAnimator.addUpdateListener(this.dotSizeUpdater);
            overShootAnimator.setInterpolator(PasswordTextViewForPIN.this.mAppearInterpolator);
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
            this.dotAnimationIsGrowing = true;
        }

        private void cancelAnimator(Animator animator) {
            if (animator != null) {
                animator.cancel();
            }
        }

        public float draw(Canvas canvas, float currentDrawPosition, int charHeight, float yPosition, float charLength) {
            float charWidth = charLength;
            if (this.currentDotSizeFactor > 0.0f) {
                canvas.save();
                canvas.translate(currentDrawPosition, yPosition);
                canvas.drawCircle(0.0f, 0.0f, ((float) (PasswordTextViewForPIN.this.mDotSize / 2)) * this.currentDotSizeFactor, PasswordTextViewForPIN.this.mDrawPaint);
                canvas.restore();
            }
            return ((float) PasswordTextViewForPIN.this.mCharPadding) + charWidth;
        }
    }

    public PasswordTextViewForPIN(Context context) {
        this(context, null);
    }

    public PasswordTextViewForPIN(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PasswordTextViewForPIN(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    /* JADX INFO: finally extract failed */
    public PasswordTextViewForPIN(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mTextChars = new ArrayList<>();
        this.mText = "";
        this.mDrawPaint = new Paint();
        this.mHandler = new Handler(Looper.getMainLooper());
        this.mResetAnimRunnable = new Runnable() {
            public void run() {
                int length = PasswordTextViewForPIN.this.mTextChars.size();
                for (int i = 0; i < length; i++) {
                    ((CharState) PasswordTextViewForPIN.this.mTextChars.get(i)).startRemoveAnimation(((long) (length - i)) * 40);
                }
            }
        };
        boolean z = true;
        setFocusableInTouchMode(true);
        setFocusable(true);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PasswordTextView);
        int i = 0;
        try {
            this.mTextHeightRaw = a.getInt(3, 0);
            this.mGravity = a.getInt(0, 17);
            this.mDotSize = a.getDimensionPixelSize(2, getContext().getResources().getDimensionPixelSize(R.dimen.password_dot_size));
            this.mCharPadding = a.getDimensionPixelSize(1, getContext().getResources().getDimensionPixelSize(R.dimen.password_char_padding));
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
            this.mShowPassword = false;
            this.mPasswordLength = (int) new MiuiLockPatternUtils(context).getLockPasswordLength(KeyguardUpdateMonitor.getCurrentUser());
            if (this.mPasswordLength < 4) {
                this.mPasswordLength = 4;
                Log.e("PasswordTextViewForPIN", "get password length = " + this.mPasswordLength);
            }
            while (true) {
                int i2 = i;
                if (i2 < this.mPasswordLength) {
                    this.mTextChars.add(new CharState());
                    i = i2 + 1;
                } else {
                    this.mWidth = getResources().getDimensionPixelSize(R.dimen.keyguard_security_pin_entry_width);
                    initCharPadding();
                    return;
                }
            }
        } catch (Throwable lockPatternUtils) {
            a.recycle();
            throw lockPatternUtils;
        }
    }

    private void initCharPadding() {
        int maxSinglePadding = this.mCharPadding;
        int textLength = this.mPasswordLength;
        Rect bounds = getCharBounds();
        int singlePadding = (this.mWidth - (textLength * (bounds.right - bounds.left))) / (textLength - 1);
        this.mCharPadding = singlePadding > maxSinglePadding ? maxSinglePadding : singlePadding;
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        float currentDrawPosition;
        Canvas canvas2 = canvas;
        float totalDrawingWidth = getDrawingWidth();
        if ((this.mGravity & 7) != 3) {
            currentDrawPosition = ((float) (this.mWidth / 2)) - (totalDrawingWidth / 2.0f);
        } else if ((this.mGravity & 8388608) == 0 || getLayoutDirection() != 1) {
            currentDrawPosition = (float) getPaddingLeft();
        } else {
            currentDrawPosition = ((float) (this.mWidth - getPaddingRight())) - totalDrawingWidth;
        }
        float currentDrawPosition2 = currentDrawPosition;
        Rect bounds = getCharBounds();
        int charHeight = bounds.bottom - bounds.top;
        float yPosition = (float) ((((getHeight() - getPaddingBottom()) - getPaddingTop()) / 2) + getPaddingTop());
        canvas2.clipRect(getPaddingLeft(), getPaddingTop(), this.mWidth - getPaddingRight(), getHeight() - getPaddingBottom());
        float charLength = (float) (bounds.right - bounds.left);
        this.mDrawPaint.setColor(Integer.MAX_VALUE);
        int i = 0;
        float startDrawPosition = (charLength / 2.0f) + currentDrawPosition2;
        for (int i2 = 0; i2 < this.mPasswordLength; i2++) {
            startDrawPosition += initGrayDotDraw(canvas2, startDrawPosition, yPosition, charLength);
        }
        this.mDrawPaint.setColor(-1);
        float startDrawPosition2 = (charLength / 2.0f) + currentDrawPosition2;
        while (true) {
            int i3 = i;
            if (i3 < getVisibleTextCharSize()) {
                startDrawPosition2 += this.mTextChars.get(i3).draw(canvas2, startDrawPosition2, charHeight, yPosition, charLength);
                i = i3 + 1;
            } else {
                return;
            }
        }
    }

    /* access modifiers changed from: private */
    public int getVisibleTextCharSize() {
        int size = 0;
        Iterator<CharState> it = this.mTextChars.iterator();
        while (it.hasNext()) {
            if (it.next().isVisible) {
                size++;
            }
        }
        return size;
    }

    private float initGrayDotDraw(Canvas canvas, float currentDrawPosition, float yPosition, float charLength) {
        canvas.save();
        canvas.drawCircle(currentDrawPosition, yPosition, (float) (this.mDotSize / 2), this.mDrawPaint);
        canvas.restore();
        return ((float) this.mCharPadding) + charLength;
    }

    private Rect getCharBounds() {
        this.mDrawPaint.setTextSize(((float) this.mTextHeightRaw) * getResources().getDisplayMetrics().scaledDensity);
        Rect bounds = new Rect();
        this.mDrawPaint.getTextBounds("0", 0, 1, bounds);
        return bounds;
    }

    private float getDrawingWidth() {
        int width = 0;
        Rect bounds = getCharBounds();
        int charLength = bounds.right - bounds.left;
        for (int i = 0; i < this.mPasswordLength; i++) {
            if (i != 0) {
                width += this.mCharPadding;
            }
            width += charLength;
        }
        return (float) width;
    }

    public void append(char c) {
        String textbefore = this.mText;
        this.mText += c;
        int newLength = this.mText.length();
        if (newLength <= this.mPasswordLength) {
            if (this.mIsResetAnimating) {
                this.mHandler.removeCallbacks(this.mResetAnimRunnable);
                Iterator<CharState> it = this.mTextChars.iterator();
                while (it.hasNext()) {
                    it.next().reset();
                }
                this.mIsResetAnimating = false;
            }
            this.mTextChars.get(newLength - 1).startAppearAnimation();
            if (this.mTextChangeListener != null) {
                this.mTextChangeListener.onTextChanged(newLength);
            }
            userActivity();
            sendAccessibilityEventTypeViewTextChanged(textbefore, textbefore.length(), 0, 1);
        }
    }

    public void setUserActivityListener(PasswordTextView.UserActivityListener userActivitiListener) {
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
            this.mTextChars.get(length - 1).startRemoveAnimation(0);
            sendAccessibilityEventTypeViewTextChanged(textbefore, textbefore.length() - 1, 1, 0);
        }
        userActivity();
        if (this.mTextChangeListener != null) {
            this.mTextChangeListener.onTextChanged(this.mText.length());
        }
    }

    public String getText() {
        return this.mText;
    }

    public void reset(boolean animated, boolean announce) {
        String textbefore = this.mText;
        this.mText = "";
        this.mIsResetAnimating = true;
        if (animated) {
            this.mHandler.postDelayed(this.mResetAnimRunnable, 320);
        } else {
            Iterator<CharState> it = this.mTextChars.iterator();
            while (it.hasNext()) {
                it.next().reset();
            }
            this.mIsResetAnimating = false;
        }
        if (announce) {
            sendAccessibilityEventTypeViewTextChanged(textbefore, 0, textbefore.length(), 0);
        }
        if (this.mTextChangeListener != null) {
            this.mTextChangeListener.onTextChanged(0);
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
