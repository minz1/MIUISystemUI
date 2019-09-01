package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.AnimatedVectorDrawableCompat;
import android.util.AttributeSet;
import android.widget.ImageView;
import com.android.systemui.R;

public class ExpandableIndicator extends ImageView {
    private boolean mExpanded;
    private boolean mIsDefaultDirection = true;

    public ExpandableIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        updateIndicatorDrawable();
        setContentDescription(getContentDescription(this.mExpanded));
    }

    public void setExpanded(boolean expanded) {
        if (expanded != this.mExpanded) {
            this.mExpanded = expanded;
            AnimatedVectorDrawable avd = (AnimatedVectorDrawable) getContext().getDrawable(getDrawableResourceId(!this.mExpanded)).getConstantState().newDrawable();
            setImageDrawable(avd);
            AnimatedVectorDrawableCompat.forceAnimationOnUI(avd);
            avd.start();
            setContentDescription(getContentDescription(expanded));
        }
    }

    private int getDrawableResourceId(boolean expanded) {
        boolean z = this.mIsDefaultDirection;
        int i = R.drawable.ic_volume_expand_animation;
        if (z) {
            if (expanded) {
                i = R.drawable.ic_volume_collapse_animation;
            }
            return i;
        }
        if (!expanded) {
            i = R.drawable.ic_volume_collapse_animation;
        }
        return i;
    }

    private String getContentDescription(boolean expanded) {
        if (expanded) {
            return this.mContext.getString(R.string.accessibility_quick_settings_collapse);
        }
        return this.mContext.getString(R.string.accessibility_quick_settings_expand);
    }

    private void updateIndicatorDrawable() {
        setImageResource(getDrawableResourceId(this.mExpanded));
    }
}
