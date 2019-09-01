package com.android.systemui.statusbar;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

public class AlphaOptimizedTextView extends TextView {
    public AlphaOptimizedTextView(Context context) {
        this(context, null);
    }

    public AlphaOptimizedTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AlphaOptimizedTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public AlphaOptimizedTextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public boolean hasOverlappingRendering() {
        return false;
    }
}
