package com.android.systemui.statusbar.notification;

import com.android.systemui.statusbar.CrossFadeHelper;
import com.android.systemui.statusbar.TransformableView;
import com.android.systemui.statusbar.ViewTransformationHelper;

public abstract class CustomInterpolatorTransformation extends ViewTransformationHelper.CustomTransformation {
    private final int mViewType;

    public CustomInterpolatorTransformation(int viewType) {
        this.mViewType = viewType;
    }

    public boolean transformTo(TransformState ownState, TransformableView notification, float transformationAmount) {
        if (!hasCustomTransformation()) {
            return false;
        }
        TransformState otherState = notification.getCurrentState(this.mViewType);
        if (otherState == null) {
            return false;
        }
        CrossFadeHelper.fadeOut(ownState.getTransformedView(), transformationAmount);
        ownState.transformViewFullyTo(otherState, this, transformationAmount);
        otherState.recycle();
        return true;
    }

    /* access modifiers changed from: protected */
    public boolean hasCustomTransformation() {
        return true;
    }

    public boolean transformFrom(TransformState ownState, TransformableView notification, float transformationAmount) {
        if (!hasCustomTransformation()) {
            return false;
        }
        TransformState otherState = notification.getCurrentState(this.mViewType);
        if (otherState == null) {
            return false;
        }
        CrossFadeHelper.fadeIn(ownState.getTransformedView(), transformationAmount);
        ownState.transformViewFullyFrom(otherState, this, transformationAmount);
        otherState.recycle();
        return true;
    }
}
