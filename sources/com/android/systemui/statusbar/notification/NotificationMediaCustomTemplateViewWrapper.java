package com.android.systemui.statusbar.notification;

import android.content.Context;
import android.view.View;
import com.android.systemui.R;
import com.android.systemui.statusbar.ExpandableNotificationRow;

public class NotificationMediaCustomTemplateViewWrapper extends NotificationMediaTemplateViewWrapper {
    private int mMediaCustomNotificationContentTopMargin;

    protected NotificationMediaCustomTemplateViewWrapper(Context ctx, View view, ExpandableNotificationRow row) {
        super(ctx, view, row);
    }

    /* access modifiers changed from: protected */
    public void initResources() {
        super.initResources();
        this.mMediaCustomNotificationContentTopMargin = this.mContext.getResources().getDimensionPixelSize(R.dimen.media_custom_notification_content_top_margin);
    }

    /* access modifiers changed from: protected */
    public void handleLargeIcon() {
        super.handleLargeIcon();
        if (this.mPicture != null && this.mRow.getStatusBarNotification().getNotification().getLargeIcon() == null) {
            this.mPicture.setVisibility(8);
        }
    }

    /* access modifiers changed from: protected */
    public void handleMainContainerMargin() {
        super.handleMainContainerMargin();
        if (this.mMainColumn != null) {
            setTopMargin(this.mMainColumn, this.mMediaCustomNotificationContentTopMargin);
        }
    }
}
