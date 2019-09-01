package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.Configuration;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.TextView;
import com.android.systemui.R;

public class KeyguardIndicationTextView extends TextView {
    private int mDensityDpi;
    private float mFontScale;
    private boolean mIsBottomButtonAnimating;

    public KeyguardIndicationTextView(Context context) {
        super(context);
    }

    public KeyguardIndicationTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public KeyguardIndicationTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public KeyguardIndicationTextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void switchIndication(CharSequence text) {
        if (TextUtils.isEmpty(text) || this.mIsBottomButtonAnimating) {
            setVisibility(4);
        } else {
            setVisibility(0);
        }
        setText(text);
    }

    public void switchIndication(int textResId) {
        switchIndication(getResources().getText(textResId));
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        float fontScale = newConfig.fontScale;
        int densityDpi = newConfig.densityDpi;
        if (this.mFontScale != fontScale) {
            updateTextSize();
            this.mFontScale = fontScale;
        }
        if (this.mDensityDpi != densityDpi) {
            updateTextSize();
            this.mDensityDpi = densityDpi;
        }
    }

    private void updateTextSize() {
        setTextSize(0, (float) getResources().getDimensionPixelSize(R.dimen.miui_default_lock_screen_unlock_hint_text_size));
    }

    public void setBottomAreaButtonClicked(boolean isClickAnimating) {
        this.mIsBottomButtonAnimating = isClickAnimating;
    }
}
