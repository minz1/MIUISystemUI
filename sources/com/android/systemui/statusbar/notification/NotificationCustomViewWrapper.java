package com.android.systemui.statusbar.notification;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.android.systemui.R;
import com.android.systemui.Util;
import com.android.systemui.ViewInvertHelper;
import com.android.systemui.statusbar.ExpandableNotificationRow;

public class NotificationCustomViewWrapper extends NotificationViewWrapper {
    private final int mBackgroundColor;
    private final int mCornerRadius;
    private final int mCustomViewMarginEnd;
    private final int mCustomViewMarginStart;
    /* access modifiers changed from: private */
    public final Paint mGreyPaint = new Paint();
    private final ViewInvertHelper mInvertHelper;
    private boolean mIsLegacy;

    protected NotificationCustomViewWrapper(Context ctx, View view, ExpandableNotificationRow row) {
        super(ctx, view, row);
        this.mInvertHelper = new ViewInvertHelper(view, 700);
        Resources resources = ctx.getResources();
        this.mCustomViewMarginStart = resources.getDimensionPixelSize(R.dimen.notification_custom_view_margin_start);
        this.mCustomViewMarginEnd = resources.getDimensionPixelSize(R.dimen.notification_custom_view_margin_end);
        this.mCornerRadius = resources.getDimensionPixelSize(R.dimen.notification_custom_view_corner_radius);
        this.mBackgroundColor = resources.getColor(R.color.notification_material_background_color);
        handleViewMargin();
        updateRoundCorner();
    }

    public void onReinflated() {
        this.mShouldInvertDark = true;
    }

    public void setDark(boolean dark, boolean fade, long delay) {
        if (dark != this.mDark || !this.mDarkInitialized) {
            super.setDark(dark, fade, delay);
            if (this.mIsLegacy || !this.mShouldInvertDark) {
                this.mView.setLayerType(dark ? 2 : 0, null);
                if (fade) {
                    fadeGrayscale(dark, delay);
                } else {
                    updateGrayscale(dark);
                }
            } else if (fade) {
                this.mInvertHelper.fade(dark, delay);
            } else {
                this.mInvertHelper.update(dark);
            }
        }
    }

    /* access modifiers changed from: protected */
    public void fadeGrayscale(final boolean dark, long delay) {
        getDozer().startIntensityAnimation(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                NotificationCustomViewWrapper.this.getDozer().updateGrayscaleMatrix(((Float) animation.getAnimatedValue()).floatValue());
                NotificationCustomViewWrapper.this.mGreyPaint.setColorFilter(new ColorMatrixColorFilter(NotificationCustomViewWrapper.this.getDozer().getGrayscaleColorMatrix()));
                NotificationCustomViewWrapper.this.mView.setLayerPaint(NotificationCustomViewWrapper.this.mGreyPaint);
            }
        }, dark, delay, new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                if (!dark) {
                    NotificationCustomViewWrapper.this.mView.setLayerType(0, null);
                }
            }
        });
    }

    /* access modifiers changed from: protected */
    public void updateGrayscale(boolean dark) {
        if (dark) {
            getDozer().updateGrayscaleMatrix(1.0f);
            this.mGreyPaint.setColorFilter(new ColorMatrixColorFilter(getDozer().getGrayscaleColorMatrix()));
            this.mView.setLayerPaint(this.mGreyPaint);
        }
    }

    public void setVisible(boolean visible) {
        super.setVisible(visible);
        this.mView.setAlpha(visible ? 1.0f : 0.0f);
    }

    /* access modifiers changed from: protected */
    public boolean shouldClearBackgroundOnReapply() {
        return false;
    }

    public int getCustomBackgroundColor() {
        return this.mBackgroundColor;
    }

    public void setLegacy(boolean legacy) {
        super.setLegacy(legacy);
        this.mIsLegacy = legacy;
    }

    private void handleViewMargin() {
        ViewGroup.LayoutParams layoutParams = this.mView.getLayoutParams();
        if (layoutParams instanceof FrameLayout.LayoutParams) {
            FrameLayout.LayoutParams frameLayoutParams = (FrameLayout.LayoutParams) layoutParams;
            frameLayoutParams.setMarginStart(this.mCustomViewMarginStart);
            frameLayoutParams.setMarginEnd(this.mCustomViewMarginEnd);
            this.mView.setLayoutParams(layoutParams);
            return;
        }
        Log.w("NotiCustomViewWrapper", "handleViewMargin(): view not attached to a FrameLayout");
    }

    private void updateRoundCorner() {
        Util.setViewRoundCorner(this.mView, (float) this.mCornerRadius);
    }
}
