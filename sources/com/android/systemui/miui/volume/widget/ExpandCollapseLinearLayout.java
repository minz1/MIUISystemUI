package com.android.systemui.miui.volume.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import com.android.systemui.miui.volume.widget.ExpandCollapseStateHelper;

public class ExpandCollapseLinearLayout extends LinearLayout implements ExpandCollapseStateHelper.OnExpandStateUpdatedListener {
    private ExpandCollapseStateHelper mStateHelper;

    public ExpandCollapseLinearLayout(Context context) {
        this(context, null);
    }

    public ExpandCollapseLinearLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ExpandCollapseLinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mStateHelper = new ExpandCollapseStateHelper(this, this, attrs, defStyleAttr);
    }

    public void updateExpanded(boolean expand, boolean withTransition) {
        this.mStateHelper.updateExpanded(expand, withTransition);
    }

    public boolean isExpanded() {
        return this.mStateHelper.isExpanded();
    }

    public void onExpandStateUpdated(boolean expand) {
    }
}
