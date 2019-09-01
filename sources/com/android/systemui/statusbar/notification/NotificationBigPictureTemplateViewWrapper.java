package com.android.systemui.statusbar.notification;

import android.content.Context;
import android.graphics.drawable.Icon;
import android.service.notification.StatusBarNotification;
import android.view.View;
import android.widget.ImageView;
import com.android.systemui.R;
import com.android.systemui.Util;
import com.android.systemui.statusbar.ExpandableNotificationRow;

public class NotificationBigPictureTemplateViewWrapper extends NotificationTemplateViewWrapper {
    private ImageView mBigPictureView;
    private Context mContext;

    protected NotificationBigPictureTemplateViewWrapper(Context ctx, View view, ExpandableNotificationRow row) {
        super(ctx, view, row);
        this.mContext = ctx;
    }

    /* access modifiers changed from: protected */
    public void handleHeaderStyle() {
        super.handleHeaderStyle();
        if (this.mNotificationMainContainer != null) {
            setTopMargin(this.mNotificationMainContainer, this.mContentMarginTopInternational);
        }
        if (this.mMainColumn != null) {
            setStartMargin(this.mMainColumn, this.mContentMarginStartInternational);
            setEndMargin(this.mMainColumn, this.mContentMarginEndInternational);
        }
        if (this.mBigPictureView != null) {
            setStartMargin(this.mBigPictureView, this.mContentMarginStartInternational);
            setEndMargin(this.mBigPictureView, this.mContentMarginEndInternational);
        }
    }

    /* access modifiers changed from: protected */
    public void resolveViews(ExpandableNotificationRow row) {
        super.resolveViews(row);
        this.mBigPictureView = (ImageView) this.mView.findViewById(16908752);
    }

    public void onContentUpdated(ExpandableNotificationRow row) {
        super.onContentUpdated(row);
        updateImageTag(row.getStatusBarNotification());
        if (this.mBigPictureView != null) {
            Util.setViewRoundCorner(this.mBigPictureView, (float) this.mContext.getResources().getDimensionPixelSize(R.dimen.notification_big_picture_corner_radius));
        }
    }

    private void updateImageTag(StatusBarNotification notification) {
        Icon overRiddenIcon = (Icon) notification.getNotification().extras.getParcelable("android.largeIcon.big");
        if (overRiddenIcon != null) {
            this.mPicture.setTag(R.id.image_icon_tag, overRiddenIcon);
        }
    }
}
