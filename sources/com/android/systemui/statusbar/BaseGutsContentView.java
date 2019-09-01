package com.android.systemui.statusbar;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import com.android.systemui.statusbar.NotificationGuts;

public abstract class BaseGutsContentView extends LinearLayout implements NotificationGuts.GutsContent {
    public BaseGutsContentView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public View getContentView() {
        return this;
    }

    public int getActualHeight() {
        return getHeight();
    }
}
