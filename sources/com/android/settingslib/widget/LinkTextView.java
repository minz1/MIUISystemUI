package com.android.settingslib.widget;

import android.content.Context;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.AttributeSet;
import android.widget.TextView;

public class LinkTextView extends TextView {
    public LinkTextView(Context context) {
        this(context, null);
    }

    public LinkTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setText(CharSequence text, TextView.BufferType type) {
        super.setText(text, type);
        if ((text instanceof Spanned) && ((ClickableSpan[]) ((Spanned) text).getSpans(0, text.length(), ClickableSpan.class)).length > 0) {
            setMovementMethod(LinkMovementMethod.getInstance());
        }
    }
}
