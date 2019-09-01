package com.android.systemui.statusbar.notification;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import com.android.systemui.miui.statusbar.notification.NotificationUtil;
import com.android.systemui.statusbar.ExpandableNotificationRow;

public class NotificationBigTextTemplateViewWrapper extends NotificationTemplateViewWrapper {
    private TextView mBigText;

    protected NotificationBigTextTemplateViewWrapper(Context ctx, View view, ExpandableNotificationRow row) {
        super(ctx, view, row);
    }

    /* access modifiers changed from: protected */
    public void handleHeaderStyle() {
        super.handleHeaderStyle();
        if (this.mNotificationMainContainer != null) {
            setTopMargin(this.mNotificationMainContainer, this.mContentMarginTopInternational);
        }
        if (this.mMainColumn != null) {
            this.mMainColumn.setPaddingRelative(this.mContentMarginStartInternational, 0, this.mContentMarginEndInternational, 0);
        }
    }

    /* access modifiers changed from: protected */
    public void resolveViews(ExpandableNotificationRow row) {
        super.resolveViews(row);
        this.mBigText = (TextView) this.mView.findViewById(16908755);
        if (this.mBigText != null && NotificationUtil.showMiuiStyle()) {
            NotificationViewWrapperCompat.setHasImage(this.mBigText, false);
        }
    }

    /* access modifiers changed from: protected */
    public boolean isOneLine() {
        return super.isOneLine() && (this.mBigText == null || TextUtils.isEmpty(this.mBigText.getText()));
    }

    /* access modifiers changed from: protected */
    public void updateTransformedTypes() {
        super.updateTransformedTypes();
        if (this.mBigText != null) {
            this.mTransformationHelper.addTransformedView(2, this.mBigText);
        }
    }
}
