package com.android.systemui.miui.widget;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.TextView;

public class LocaleSensitiveTextView extends TextView {
    private int mTextId;

    public LocaleSensitiveTextView(Context context) {
        this(context, null);
    }

    public LocaleSensitiveTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LocaleSensitiveTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.LocaleSensitiveTextView, defStyleAttr, 0);
        if (typedArray != null) {
            this.mTextId = typedArray.getResourceId(R.styleable.LocaleSensitiveTextView_android_text, 0);
            typedArray.recycle();
        }
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration newConfig) {
        if (!TextUtils.equals(getTextLocale().getLanguage(), newConfig.locale.getLanguage()) && this.mTextId != 0) {
            setText(this.mTextId);
        }
        super.onConfigurationChanged(newConfig);
    }
}
