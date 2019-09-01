package com.android.systemui.statusbar.notification;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import com.android.systemui.R;
import com.android.systemui.Util;
import com.android.systemui.statusbar.CrossFadeHelper;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.TransformableView;
import com.android.systemui.statusbar.ViewTransformationHelper;

public class NotificationMediaTemplateViewWrapper extends NotificationTemplateViewWrapper {
    private ViewGroup mActionsView;
    private TextView mAppNameView;
    private int mMediaNotificationActionSize;
    private int mMediaNotificationLargeIconRadius;
    private int mNotificationMediaActionColor;

    protected NotificationMediaTemplateViewWrapper(Context ctx, View view, ExpandableNotificationRow row) {
        super(ctx, view, row);
        this.mTransformationHelper.setCustomTransformation(new ViewTransformationHelper.CustomTransformation() {
            public boolean transformTo(TransformState ownState, TransformableView notification, float transformationAmount) {
                CrossFadeHelper.fadeOut(ownState.getTransformedView(), transformationAmount);
                return true;
            }

            public boolean transformFrom(TransformState ownState, TransformableView notification, float transformationAmount) {
                CrossFadeHelper.fadeIn(ownState.getTransformedView(), transformationAmount);
                return true;
            }
        }, 5);
    }

    /* access modifiers changed from: protected */
    public void initResources() {
        super.initResources();
        this.mMediaNotificationLargeIconRadius = this.mContext.getResources().getDimensionPixelSize(R.dimen.media_notification_large_icon_radius);
        this.mMediaNotificationActionSize = this.mContext.getResources().getDimensionPixelSize(R.dimen.media_notification_action_size);
        this.mNotificationMediaActionColor = this.mContext.getColor(R.color.notification_media_action_color);
    }

    /* access modifiers changed from: protected */
    public void resolveViews(ExpandableNotificationRow row) {
        super.resolveViews(row);
        this.mActionsView = (ViewGroup) this.mView.findViewById(16909067);
        this.mAppNameView = (TextView) this.mView.findViewById(16909068);
        if (this.mAppNameView != null) {
            this.mAppNameView.setText(row.getAppName());
        }
        updateViews();
    }

    /* access modifiers changed from: protected */
    public void handleLargeIcon() {
        super.handleLargeIcon();
        if (this.mPicture != null) {
            this.mPicture.setVisibility(0);
            Util.setViewRoundCorner(this.mPicture, (float) this.mMediaNotificationLargeIconRadius);
        }
    }

    private void updateViews() {
        if (this.mActionsView != null && this.mActionsView.getChildCount() > 0) {
            int childCount = this.mActionsView.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View action = this.mActionsView.getChildAt(i);
                if (action instanceof ImageButton) {
                    Drawable drawable = ((ImageButton) action).getDrawable();
                    if (drawable != null) {
                        drawable.setColorFilter(this.mNotificationMediaActionColor, PorterDuff.Mode.SRC_ATOP);
                    }
                }
                if (isNormalMediaView()) {
                    ViewGroup.LayoutParams layoutParams = action.getLayoutParams();
                    layoutParams.width = this.mMediaNotificationActionSize;
                    layoutParams.height = this.mMediaNotificationActionSize;
                }
            }
        }
        if (this.mTime != null) {
            this.mTime.setVisibility(8);
        }
        if (this.mMainColumn != null) {
            boolean shouldClipPadding = !isNormalMediaView();
            if (shouldClipPadding != ((ViewGroup) this.mMainColumn).getClipToPadding()) {
                ((ViewGroup) this.mMainColumn).setClipToPadding(shouldClipPadding);
            }
        }
    }

    /* access modifiers changed from: protected */
    public boolean isOneLine() {
        return false;
    }

    /* access modifiers changed from: protected */
    public void updateTransformedTypes() {
        super.updateTransformedTypes();
        if (this.mActionsView != null) {
            this.mTransformationHelper.addTransformedView(5, this.mActionsView);
        }
    }

    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (this.mTime != null) {
            this.mTime.setVisibility(8);
        }
    }

    public boolean isDimmable() {
        return false;
    }

    private boolean isNormalMediaView() {
        return this.mMainColumn != null && "media".equals(this.mMainColumn.getTag());
    }
}
