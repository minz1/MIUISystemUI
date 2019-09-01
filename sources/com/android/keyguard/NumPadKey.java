package com.android.keyguard;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.Ease;
import com.android.systemui.R;

public class NumPadKey extends ViewGroup {
    static String[] sKlondike;
    private AnimatorSet mBackgroundAnimatorSet;
    /* access modifiers changed from: private */
    public boolean mBackgroundAppearAnimatorRunning;
    private float mBackgroundCircleCenterX;
    private float mBackgroundCircleCenterY;
    /* access modifiers changed from: private */
    public int mBackgroundCircleOriginalRadius;
    /* access modifiers changed from: private */
    public Paint mBackgroundCirclePaint;
    /* access modifiers changed from: private */
    public int mBackgroundCircleRadius;
    /* access modifiers changed from: private */
    public int mDigit;
    private TextView mDigitText;
    private boolean mEnableHaptics;
    private TextView mKlondikeText;
    private View.OnClickListener mListener;
    private PowerManager mPM;
    /* access modifiers changed from: private */
    public boolean mPendingBackgroundDisappearAnimate;
    /* access modifiers changed from: private */
    public PasswordTextView mTextView;
    /* access modifiers changed from: private */
    public int mTextViewResId;

    public void userActivity() {
        this.mPM.userActivity(SystemClock.uptimeMillis(), false);
    }

    public NumPadKey(Context context) {
        this(context, null);
    }

    public NumPadKey(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NumPadKey(Context context, AttributeSet attrs, int defStyle) {
        this(context, attrs, defStyle, R.layout.keyguard_num_pad_key);
    }

    /* JADX INFO: finally extract failed */
    protected NumPadKey(Context context, AttributeSet attrs, int defStyle, int contentResource) {
        super(context, attrs, defStyle);
        this.mDigit = -1;
        this.mListener = new View.OnClickListener() {
            public void onClick(View thisView) {
                if (NumPadKey.this.mTextView == null && NumPadKey.this.mTextViewResId > 0) {
                    View v = NumPadKey.this.getRootView().findViewById(NumPadKey.this.mTextViewResId);
                    if (v != null && (v instanceof PasswordTextView)) {
                        PasswordTextView unused = NumPadKey.this.mTextView = (PasswordTextView) v;
                    }
                }
                if (NumPadKey.this.mTextView != null && NumPadKey.this.mTextView.isEnabled()) {
                    NumPadKey.this.mTextView.append(Character.forDigit(NumPadKey.this.mDigit, 10));
                }
                NumPadKey.this.userActivity();
            }
        };
        setFocusable(true);
        setWillNotDraw(false);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.NumPadKey);
        try {
            this.mDigit = a.getInt(0, this.mDigit);
            this.mTextViewResId = a.getResourceId(1, 0);
            a.recycle();
            setOnClickListener(this.mListener);
            setOnHoverListener(new LiftToActivateListener(context));
            this.mEnableHaptics = new LockPatternUtils(context).isTactileFeedbackEnabled();
            this.mPM = (PowerManager) this.mContext.getSystemService("power");
            ((LayoutInflater) getContext().getSystemService("layout_inflater")).inflate(contentResource, this, true);
            this.mDigitText = (TextView) findViewById(R.id.digit_text);
            this.mDigitText.setText(Integer.toString(this.mDigit));
            this.mDigitText.setTextSize(0, getResources().getDimension(R.dimen.lock_screen_numeric_keyboard_number_text_size));
            this.mDigitText.setTextColor(getResources().getColor(R.color.lock_screen_numeric_keyboard_number_text_color));
            this.mDigitText.setTypeface(Typeface.create("miui-light", 0));
            this.mDigitText.setLineSpacing(0.0f, 1.0f);
            this.mDigitText.setIncludeFontPadding(false);
            this.mKlondikeText = (TextView) findViewById(R.id.klondike_text);
            this.mKlondikeText.setTextSize(0, getResources().getDimension(R.dimen.lock_screen_numeric_keyboard_alphabet_text_size));
            this.mKlondikeText.setTextColor(getResources().getColor(R.color.lock_screen_numeric_keyboard_alphabet_text_color));
            this.mKlondikeText.setTypeface(Typeface.create("miui-regular", 0));
            this.mKlondikeText.setLineSpacing(0.0f, 1.0f);
            this.mKlondikeText.setIncludeFontPadding(false);
            if (this.mDigit >= 0) {
                if (sKlondike == null) {
                    sKlondike = getResources().getStringArray(R.array.lockscreen_num_pad_klondike);
                }
                if (sKlondike != null && sKlondike.length > this.mDigit) {
                    String klondike = sKlondike[this.mDigit];
                    if (klondike.length() > 0) {
                        this.mKlondikeText.setText(klondike);
                    } else {
                        this.mKlondikeText.setVisibility(4);
                    }
                }
            }
            setContentDescription(this.mDigitText.getText().toString());
            this.mBackgroundCirclePaint = new Paint();
            this.mBackgroundCirclePaint.setColor(this.mContext.getResources().getColor(R.color.miui_keyguard_pin_num_pad_key_bg_color));
            this.mBackgroundCirclePaint.setAntiAlias(true);
            this.mBackgroundCircleOriginalRadius = this.mContext.getResources().getDimensionPixelOffset(R.dimen.miui_keyguard_pin_view_num_pad_width) / 2;
        } catch (Throwable th) {
            a.recycle();
            throw th;
        }
    }

    /* access modifiers changed from: protected */
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.mBackgroundCircleCenterX = (float) (w / 2);
        this.mBackgroundCircleCenterY = (float) (h / 2);
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.mBackgroundCircleRadius != 0) {
            canvas.drawCircle(this.mBackgroundCircleCenterX, this.mBackgroundCircleCenterY, (float) this.mBackgroundCircleRadius, this.mBackgroundCirclePaint);
        }
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == 0) {
            doHapticKeyClick();
            startAppearBackgroundAnimate();
        } else if (event.getActionMasked() == 1 || event.getActionMasked() == 3) {
            startDisappearBackgroundAnimate();
        }
        return super.onTouchEvent(event);
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        measureChildren(widthMeasureSpec, heightMeasureSpec);
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        int digitHeight = this.mDigitText.getMeasuredHeight();
        int klondikeHeight = this.mKlondikeText.getMeasuredHeight();
        int top = (getHeight() / 2) - ((digitHeight + klondikeHeight) / 2);
        int centerX = getWidth() / 2;
        int left = centerX - (this.mDigitText.getMeasuredWidth() / 2);
        int bottom = top + digitHeight;
        this.mDigitText.layout(left, top, this.mDigitText.getMeasuredWidth() + left, bottom);
        int top2 = (int) (((float) bottom) - (((float) klondikeHeight) * 0.35f));
        int left2 = centerX - (this.mKlondikeText.getMeasuredWidth() / 2);
        this.mKlondikeText.layout(left2, top2, this.mKlondikeText.getMeasuredWidth() + left2, top2 + klondikeHeight);
    }

    public boolean hasOverlappingRendering() {
        return false;
    }

    public void doHapticKeyClick() {
        if (this.mEnableHaptics) {
            performHapticFeedback(1, 3);
        }
    }

    private void startAppearBackgroundAnimate() {
        this.mPendingBackgroundDisappearAnimate = false;
        cancelBackgroundAnimatorSet();
        this.mBackgroundAnimatorSet = createBackgroundAnimatorSet(true, 200, new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animation) {
                boolean unused = NumPadKey.this.mBackgroundAppearAnimatorRunning = true;
            }

            public void onAnimationEnd(Animator animation) {
                boolean unused = NumPadKey.this.mBackgroundAppearAnimatorRunning = false;
                if (NumPadKey.this.mPendingBackgroundDisappearAnimate) {
                    boolean unused2 = NumPadKey.this.mPendingBackgroundDisappearAnimate = false;
                    NumPadKey.this.startDisappearBackgroundAnimate();
                }
            }
        });
        this.mBackgroundAnimatorSet.start();
    }

    /* access modifiers changed from: private */
    public void startDisappearBackgroundAnimate() {
        if (this.mBackgroundAppearAnimatorRunning) {
            this.mPendingBackgroundDisappearAnimate = true;
            return;
        }
        cancelBackgroundAnimatorSet();
        this.mBackgroundAnimatorSet = createBackgroundAnimatorSet(false, 300, new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                int unused = NumPadKey.this.mBackgroundCircleRadius = 0;
            }
        });
        this.mBackgroundAnimatorSet.start();
    }

    private AnimatorSet createBackgroundAnimatorSet(boolean isAppear, long duration, AnimatorListenerAdapter listenerAdapter) {
        AnimatorSet animatorSet = new AnimatorSet();
        float[] fArr = new float[2];
        float f = 0.1f;
        fArr[0] = isAppear ? 0.0f : 0.1f;
        if (!isAppear) {
            f = 0.0f;
        }
        fArr[1] = f;
        ValueAnimator alphaAnimator = ValueAnimator.ofFloat(fArr);
        alphaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                NumPadKey.this.mBackgroundCirclePaint.setAlpha((int) (255.0f * ((Float) animation.getAnimatedValue()).floatValue()));
                NumPadKey.this.invalidate();
            }
        });
        float[] fArr2 = new float[2];
        float f2 = 1.35f;
        fArr2[0] = isAppear ? 1.0f : 1.35f;
        if (!isAppear) {
            f2 = 1.0f;
        }
        fArr2[1] = f2;
        ValueAnimator scaleAnimator = ValueAnimator.ofFloat(fArr2);
        scaleAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                int unused = NumPadKey.this.mBackgroundCircleRadius = (int) (((float) NumPadKey.this.mBackgroundCircleOriginalRadius) * ((Float) animation.getAnimatedValue()).floatValue());
            }
        });
        animatorSet.playTogether(new Animator[]{alphaAnimator, scaleAnimator});
        animatorSet.setDuration(duration);
        animatorSet.setInterpolator(Ease.Quad.easeInOut);
        animatorSet.addListener(listenerAdapter);
        return animatorSet;
    }

    private void cancelBackgroundAnimatorSet() {
        if (this.mBackgroundAnimatorSet != null) {
            this.mBackgroundAnimatorSet.cancel();
            this.mBackgroundAnimatorSet.removeAllListeners();
        }
    }
}
