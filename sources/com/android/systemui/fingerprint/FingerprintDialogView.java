package com.android.systemui.fingerprint;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Interpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;
import com.android.keyguard.MiuiKeyguardUtils;
import com.android.keyguard.fod.MiuiGxzwUtils;
import com.android.systemui.Interpolators;
import com.android.systemui.R;

public class FingerprintDialogView extends LinearLayout {
    /* access modifiers changed from: private */
    public boolean mAnimatingAway;
    /* access modifiers changed from: private */
    public final float mAnimationTranslationOffset;
    private Bundle mBundle;
    /* access modifiers changed from: private */
    public final LinearLayout mDialog;
    private final float mDisplayHeight;
    private final float mDisplayWidth;
    private final int mErrorColor;
    private final TextView mErrorText;
    private final int mFingerprintColor;
    /* access modifiers changed from: private */
    public Handler mHandler;
    private int mLastState;
    /* access modifiers changed from: private */
    public ViewGroup mLayout;
    /* access modifiers changed from: private */
    public final Interpolator mLinearOutSlowIn;
    private final Runnable mShowAnimationRunnable = new Runnable() {
        public void run() {
            FingerprintDialogView.this.mLayout.animate().alpha(1.0f).setDuration(250).setInterpolator(FingerprintDialogView.this.mLinearOutSlowIn).withLayer().start();
            FingerprintDialogView.this.mDialog.animate().translationY(0.0f).setDuration(250).setInterpolator(FingerprintDialogView.this.mLinearOutSlowIn).withLayer().start();
        }
    };
    private final int mTextColor;
    private boolean mWasForceRemoved;
    /* access modifiers changed from: private */
    public final WindowManager mWindowManager;
    private final IBinder mWindowToken = new Binder();

    public FingerprintDialogView(Context context, Handler handler) {
        super(context);
        this.mHandler = handler;
        this.mLinearOutSlowIn = Interpolators.LINEAR_OUT_SLOW_IN;
        this.mWindowManager = (WindowManager) this.mContext.getSystemService("window");
        this.mAnimationTranslationOffset = getResources().getDimension(R.dimen.fingerprint_dialog_animation_translation_offset);
        this.mErrorColor = Color.parseColor(getResources().getString(R.color.fingerprint_dialog_error_color));
        this.mTextColor = Color.parseColor(getResources().getString(R.color.fingerprint_dialog_text_light_color));
        this.mFingerprintColor = Color.parseColor(getResources().getString(R.color.fingerprint_dialog_fingerprint_color));
        DisplayMetrics metrics = new DisplayMetrics();
        this.mWindowManager.getDefaultDisplay().getMetrics(metrics);
        this.mDisplayWidth = (float) metrics.widthPixels;
        this.mDisplayHeight = (float) metrics.heightPixels;
        this.mLayout = (ViewGroup) LayoutInflater.from(getContext()).inflate(R.layout.fingerprint_dialog, this, false);
        addView(this.mLayout);
        this.mDialog = (LinearLayout) this.mLayout.findViewById(R.id.dialog);
        this.mErrorText = (TextView) this.mLayout.findViewById(R.id.error);
        this.mLayout.setOnKeyListener(new View.OnKeyListener() {
            boolean downPressed = false;

            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode != 4) {
                    return false;
                }
                if (event.getAction() == 0 && !this.downPressed) {
                    this.downPressed = true;
                } else if (event.getAction() == 0) {
                    this.downPressed = false;
                } else if (event.getAction() == 1 && this.downPressed) {
                    this.downPressed = false;
                    FingerprintDialogView.this.mHandler.obtainMessage(7).sendToTarget();
                }
                return true;
            }
        });
        View space = this.mLayout.findViewById(R.id.space);
        View leftSpace = this.mLayout.findViewById(R.id.left_space);
        View rightSpace = this.mLayout.findViewById(R.id.right_space);
        setDismissesDialog(space);
        setDismissesDialog(leftSpace);
        setDismissesDialog(rightSpace);
        ((Button) this.mLayout.findViewById(R.id.button2)).setOnClickListener(new View.OnClickListener() {
            public final void onClick(View view) {
                FingerprintDialogView.this.mHandler.obtainMessage(6).sendToTarget();
            }
        });
        ((Button) this.mLayout.findViewById(R.id.button1)).setOnClickListener(new View.OnClickListener() {
            public final void onClick(View view) {
                FingerprintDialogView.this.mHandler.obtainMessage(8).sendToTarget();
            }
        });
        this.mLayout.setFocusableInTouchMode(true);
        this.mLayout.requestFocus();
    }

    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        TextView title = (TextView) this.mLayout.findViewById(R.id.title);
        TextView subtitle = (TextView) this.mLayout.findViewById(R.id.subtitle);
        TextView description = (TextView) this.mLayout.findViewById(R.id.description);
        Button negative = (Button) this.mLayout.findViewById(R.id.button2);
        Button positive = (Button) this.mLayout.findViewById(R.id.button1);
        Space fodIconSpace = (Space) this.mLayout.findViewById(R.id.fod_icon_space);
        LinearLayout.LayoutParams fodIconSpaceLayoutParams = (LinearLayout.LayoutParams) fodIconSpace.getLayoutParams();
        ImageView fingerprintIcon = (ImageView) this.mLayout.findViewById(R.id.fingerprint_icon);
        if (MiuiKeyguardUtils.isGxzwSensor()) {
            int spaceHeight = ((((int) this.mDisplayHeight) - MiuiGxzwUtils.GXZW_ICON_Y) + (MiuiGxzwUtils.GXZW_ICON_HEIGHT / 2)) - getResources().getDimensionPixelOffset(R.dimen.fingerprint_dialog_button_container_height);
            fodIconSpaceLayoutParams.height = spaceHeight > 0 ? spaceHeight : 0;
            fingerprintIcon.setVisibility(8);
        } else {
            fodIconSpaceLayoutParams.height = 0;
            fingerprintIcon.setVisibility(0);
        }
        fodIconSpace.setLayoutParams(fodIconSpaceLayoutParams);
        this.mDialog.getLayoutParams().width = (int) this.mDisplayWidth;
        this.mLastState = 0;
        updateFingerprintIcon(1);
        title.setText(this.mBundle.getCharSequence("title"));
        title.setSelected(true);
        CharSequence subtitleText = this.mBundle.getCharSequence("subtitle");
        if (TextUtils.isEmpty(subtitleText)) {
            subtitle.setVisibility(8);
        } else {
            subtitle.setVisibility(0);
            subtitle.setText(subtitleText);
        }
        CharSequence descriptionText = this.mBundle.getCharSequence("description");
        if (TextUtils.isEmpty(descriptionText)) {
            description.setVisibility(8);
        } else {
            description.setVisibility(0);
            description.setText(descriptionText);
        }
        negative.setText(this.mBundle.getCharSequence("negative_text"));
        CharSequence positiveText = this.mBundle.getCharSequence("positive_text");
        positive.setText(positiveText);
        if (positiveText != null) {
            positive.setVisibility(0);
        } else {
            positive.setVisibility(8);
        }
        if (!this.mWasForceRemoved) {
            this.mDialog.setTranslationY(this.mAnimationTranslationOffset);
            this.mLayout.setAlpha(0.0f);
            postOnAnimation(this.mShowAnimationRunnable);
        } else {
            this.mLayout.animate().cancel();
            this.mDialog.animate().cancel();
            this.mDialog.setAlpha(1.0f);
            this.mDialog.setTranslationY(0.0f);
            this.mLayout.setAlpha(1.0f);
        }
        this.mWasForceRemoved = false;
        resetMessage();
    }

    private void setDismissesDialog(View v) {
        v.setClickable(true);
        v.setOnTouchListener(new View.OnTouchListener() {
            public final boolean onTouch(View view, MotionEvent motionEvent) {
                return FingerprintDialogView.this.mHandler.obtainMessage(5, true).sendToTarget();
            }
        });
    }

    public void startDismiss() {
        this.mAnimatingAway = true;
        final Runnable endActionRunnable = new Runnable() {
            public void run() {
                FingerprintDialogView.this.mWindowManager.removeView(FingerprintDialogView.this);
                boolean unused = FingerprintDialogView.this.mAnimatingAway = false;
            }
        };
        postOnAnimation(new Runnable() {
            public void run() {
                FingerprintDialogView.this.mLayout.animate().alpha(0.0f).setDuration(350).setInterpolator(FingerprintDialogView.this.mLinearOutSlowIn).withLayer().start();
                FingerprintDialogView.this.mDialog.animate().translationY(FingerprintDialogView.this.mAnimationTranslationOffset).setDuration(350).setInterpolator(FingerprintDialogView.this.mLinearOutSlowIn).withLayer().withEndAction(endActionRunnable).start();
            }
        });
    }

    public void forceRemove() {
        this.mLayout.animate().cancel();
        this.mDialog.animate().cancel();
        this.mWindowManager.removeView(this);
        this.mAnimatingAway = false;
        this.mWasForceRemoved = true;
    }

    public boolean isAnimatingAway() {
        return this.mAnimatingAway;
    }

    public void setBundle(Bundle bundle) {
        this.mBundle = bundle;
    }

    /* access modifiers changed from: protected */
    public void resetMessage() {
        updateFingerprintIcon(1);
        this.mErrorText.setText(R.string.fingerprint_dialog_touch_sensor);
        this.mErrorText.setTextColor(this.mTextColor);
    }

    private void showTemporaryMessage(String message) {
        this.mHandler.removeMessages(9);
        updateFingerprintIcon(2);
        this.mErrorText.setText(message);
        this.mErrorText.setTextColor(this.mErrorColor);
        this.mErrorText.setContentDescription(message);
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(9), 2000);
    }

    public void showHelpMessage(String message) {
        showTemporaryMessage(message);
    }

    public void showErrorMessage(String error) {
        showTemporaryMessage(error);
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(5, false), 2000);
    }

    private void updateFingerprintIcon(int newState) {
        AnimatedVectorDrawable animation;
        if (!MiuiKeyguardUtils.isGxzwSensor()) {
            Drawable icon = getAnimationForTransition(this.mLastState, newState);
            if (icon == null) {
                Log.e("FingerprintDialogView", "Animation not found");
                return;
            }
            if (icon instanceof AnimatedVectorDrawable) {
                animation = (AnimatedVectorDrawable) icon;
            } else {
                animation = null;
            }
            ((ImageView) this.mLayout.findViewById(R.id.fingerprint_icon)).setImageDrawable(icon);
            if (animation != null && shouldAnimateForTransition(this.mLastState, newState)) {
                animation.forceAnimationOnUI();
                animation.start();
            }
            this.mLastState = newState;
        }
    }

    private boolean shouldAnimateForTransition(int oldState, int newState) {
        if (oldState == 0 && newState == 1) {
            return false;
        }
        if (oldState == 1 && newState == 2) {
            return true;
        }
        if (oldState == 2 && newState == 1) {
            return true;
        }
        return (oldState != 1 || newState == 3) ? false : false;
    }

    private Drawable getAnimationForTransition(int oldState, int newState) {
        int iconRes;
        if (oldState == 0 && newState == 1) {
            iconRes = R.drawable.fingerprint_dialog_fp_to_error;
        } else if (oldState == 1 && newState == 2) {
            iconRes = R.drawable.fingerprint_dialog_fp_to_error;
        } else if (oldState == 2 && newState == 1) {
            iconRes = R.drawable.fingerprint_dialog_error_to_fp;
        } else if (oldState != 1 || newState != 3) {
            return null;
        } else {
            iconRes = R.drawable.fingerprint_dialog_error_to_fp;
        }
        return this.mContext.getDrawable(iconRes);
    }

    public WindowManager.LayoutParams getLayoutParams() {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(-1, -1, 2014, 16777216, -3);
        lp.privateFlags |= 16;
        lp.setTitle("FingerprintDialogView");
        lp.token = this.mWindowToken;
        return lp;
    }
}
