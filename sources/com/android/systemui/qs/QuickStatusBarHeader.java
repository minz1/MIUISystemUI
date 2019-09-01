package com.android.systemui.qs;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import com.android.systemui.CustomizedUtils;
import com.android.systemui.R;
import com.android.systemui.miui.widget.ClipEdgeLinearLayout;

public class QuickStatusBarHeader extends RelativeLayout {
    private boolean mExpanded;
    private int mLastOrientation;

    public QuickStatusBarHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        updateResources();
        ((ClipEdgeLinearLayout) findViewById(R.id.system_icons)).setClipEdge(true);
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateResources();
        if (newConfig.orientation != this.mLastOrientation) {
            this.mLastOrientation = newConfig.orientation;
        }
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);
        updateResources();
    }

    private void updateResources() {
        ViewGroup.LayoutParams lp = getLayoutParams();
        lp.height = CustomizedUtils.getNotchExpandedHeaderViewHeight(getContext(), getResources().getDimensionPixelSize(R.dimen.notch_expanded_header_height));
        setLayoutParams(lp);
    }

    public void setExpanded(boolean expanded) {
        if (this.mExpanded != expanded) {
            this.mExpanded = expanded;
            updateEverything();
        }
    }

    public void updateEverything() {
        post(new Runnable() {
            public void run() {
                QuickStatusBarHeader.this.setClickable(false);
            }
        });
    }
}
