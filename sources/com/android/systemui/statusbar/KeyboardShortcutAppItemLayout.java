package com.android.systemui.statusbar;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.android.systemui.R;

public class KeyboardShortcutAppItemLayout extends RelativeLayout {
    public KeyboardShortcutAppItemLayout(Context context) {
        super(context);
    }

    public KeyboardShortcutAppItemLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (View.MeasureSpec.getMode(widthMeasureSpec) == 1073741824) {
            ImageView shortcutIcon = (ImageView) findViewById(R.id.keyboard_shortcuts_icon);
            TextView shortcutKeyword = (TextView) findViewById(R.id.keyboard_shortcuts_keyword);
            int availableWidth = View.MeasureSpec.getSize(widthMeasureSpec) - (getPaddingLeft() + getPaddingRight());
            if (shortcutIcon.getVisibility() == 0) {
                availableWidth -= shortcutIcon.getMeasuredWidth();
            }
            shortcutKeyword.setMaxWidth((int) Math.round(((double) availableWidth) * 0.7d));
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
