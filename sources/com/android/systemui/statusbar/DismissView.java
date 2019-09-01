package com.android.systemui.statusbar;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import com.android.keyguard.AlphaOptimizedImageButton;
import com.android.systemui.R;

public class DismissView extends AlphaOptimizedImageButton {
    private int mExtraMarginBottom;

    public DismissView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setImageResource(R.drawable.notifications_clear_all);
        setContentDescription(getContext().getString(R.string.accessibility_clear_all));
        updateLayoutParam();
    }

    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        WindowInsets insets2 = super.onApplyWindowInsets(insets);
        this.mExtraMarginBottom = insets2.getStableInsetBottom();
        updateLayoutParam();
        return insets2;
    }

    private void updateLayoutParam() {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
        lp.bottomMargin = getResources().getDimensionPixelSize(R.dimen.notification_clear_all_bottom_margin) + this.mExtraMarginBottom;
        lp.rightMargin = getResources().getDimensionPixelSize(R.dimen.notification_clear_all_end_margin);
        lp.gravity = getResources().getInteger(R.integer.dismiss_button_layout_gravity);
        setLayoutParams(lp);
    }
}
