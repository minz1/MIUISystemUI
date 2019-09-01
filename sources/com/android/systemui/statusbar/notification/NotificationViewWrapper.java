package com.android.systemui.statusbar.notification;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.ColorUtils;
import android.view.NotificationHeaderView;
import android.view.View;
import com.android.systemui.miui.statusbar.notification.NotificationUtil;
import com.android.systemui.statusbar.CrossFadeHelper;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.TransformableView;

public abstract class NotificationViewWrapper implements TransformableView {
    private int mBackgroundColor = 0;
    protected Context mContext;
    protected boolean mDark;
    protected boolean mDarkInitialized = false;
    private final NotificationDozeHelper mDozer;
    protected final ExpandableNotificationRow mRow;
    protected boolean mShouldInvertDark;
    protected TYPE_SHOWING mShowingType = TYPE_SHOWING.TYPE_UNKNOWN;
    protected final View mView;

    public enum TYPE_SHOWING {
        TYPE_UNKNOWN,
        TYPE_CONTRACTED,
        TYPE_EXPANDED,
        TYPE_HEADSUP,
        TYPE_AMBIENT
    }

    public static NotificationViewWrapper wrap(Context ctx, View v, ExpandableNotificationRow row, TYPE_SHOWING showingType) {
        NotificationViewWrapper resultWrapper = wrap(ctx, v, row);
        resultWrapper.mShowingType = showingType;
        resultWrapper.initHandle();
        return resultWrapper;
    }

    public static NotificationViewWrapper wrap(Context ctx, View v, ExpandableNotificationRow row) {
        if (v.getId() == 16909384) {
            if ("bigPicture".equals(v.getTag())) {
                return new NotificationBigPictureTemplateViewWrapper(ctx, v, row);
            }
            if ("bigText".equals(v.getTag())) {
                return new NotificationBigTextTemplateViewWrapper(ctx, v, row);
            }
            if ("media".equals(v.getTag()) || "bigMediaNarrow".equals(v.getTag())) {
                if (NotificationUtil.isCustomViewNotification(row.getStatusBarNotification())) {
                    return new NotificationMediaCustomTemplateViewWrapper(ctx, v, row);
                }
                return new NotificationMediaTemplateViewWrapper(ctx, v, row);
            } else if ("messaging".equals(v.getTag())) {
                return new NotificationMessagingTemplateViewWrapper(ctx, v, row);
            } else {
                if ("inbox".equals(v.getTag())) {
                    return new NotificationInboxTemplateViewWrapper(ctx, v, row);
                }
                return new NotificationTemplateViewWrapper(ctx, v, row);
            }
        } else if (v instanceof NotificationHeaderView) {
            return new NotificationHeaderViewWrapper(ctx, v, row);
        } else {
            if (v instanceof OptimizedHeadsUpNotificationView) {
                return new NotificationOptimizedHeadsUpViewWrapper(ctx, v, row);
            }
            if (v instanceof InCallNotificationView) {
                return new NotificationInCallViewWrapper(ctx, v, row);
            }
            return new NotificationCustomViewWrapper(ctx, v, row);
        }
    }

    protected NotificationViewWrapper(Context ctx, View view, ExpandableNotificationRow row) {
        this.mContext = ctx;
        this.mView = view;
        this.mRow = row;
        this.mDozer = createDozer(ctx);
        onReinflated();
    }

    /* access modifiers changed from: protected */
    public NotificationDozeHelper createDozer(Context ctx) {
        return new NotificationDozeHelper();
    }

    /* access modifiers changed from: protected */
    public NotificationDozeHelper getDozer() {
        return this.mDozer;
    }

    /* access modifiers changed from: protected */
    public void initHandle() {
    }

    public void setDark(boolean dark, boolean fade, long delay) {
        this.mDark = dark;
        this.mDarkInitialized = true;
    }

    public void onContentUpdated(ExpandableNotificationRow row) {
        this.mDarkInitialized = false;
    }

    public void onReinflated() {
        boolean z = false;
        if (shouldClearBackgroundOnReapply()) {
            this.mBackgroundColor = 0;
        }
        Drawable background = this.mView.getBackground();
        if (background instanceof ColorDrawable) {
            this.mBackgroundColor = ((ColorDrawable) background).getColor();
            this.mView.setBackground(null);
        }
        if (this.mBackgroundColor == 0 || isColorLight(this.mBackgroundColor)) {
            z = true;
        }
        this.mShouldInvertDark = z;
    }

    /* access modifiers changed from: protected */
    public boolean shouldClearBackgroundOnReapply() {
        return true;
    }

    private boolean isColorLight(int backgroundColor) {
        return Color.alpha(backgroundColor) == 0 || ColorUtils.calculateLuminance(backgroundColor) > 0.5d;
    }

    public void updateExpandability(boolean expandable, View.OnClickListener onClickListener) {
    }

    public void showPublic() {
    }

    public NotificationHeaderView getNotificationHeader() {
        return null;
    }

    public TransformState getCurrentState(int fadingView) {
        return null;
    }

    public void transformTo(TransformableView notification, Runnable endRunnable) {
        CrossFadeHelper.fadeOut(this.mView, endRunnable);
    }

    public void transformTo(TransformableView notification, float transformationAmount) {
        CrossFadeHelper.fadeOut(this.mView, transformationAmount);
    }

    public void transformFrom(TransformableView notification) {
        CrossFadeHelper.fadeIn(this.mView);
    }

    public void transformFrom(TransformableView notification, float transformationAmount) {
        CrossFadeHelper.fadeIn(this.mView, transformationAmount);
    }

    public void setVisible(boolean visible) {
        this.mView.animate().cancel();
        this.mView.setVisibility(visible ? 0 : 4);
    }

    public int getCustomBackgroundColor() {
        if (this.mRow.isSummaryWithChildren()) {
            return 0;
        }
        return this.mBackgroundColor;
    }

    public void setLegacy(boolean legacy) {
    }

    public void setContentHeight(int contentHeight, int minHeightHint) {
    }

    public void setRemoteInputVisible(boolean visible) {
    }

    public void setIsChildInGroup(boolean isChildInGroup) {
    }

    public boolean isDimmable() {
        return true;
    }
}
