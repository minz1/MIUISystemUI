package com.android.systemui.statusbar.notification;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.systemui.miui.statusbar.notification.NotificationUtil;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.util.Utils;

public class NotificationInboxTemplateViewWrapper extends NotificationTemplateViewWrapper {
    private TextView mInboxText0;

    protected NotificationInboxTemplateViewWrapper(Context ctx, View view, ExpandableNotificationRow row) {
        super(ctx, view, row);
    }

    private void resolveViews() {
        View container = this.mView.findViewById(16909138);
        if (container instanceof ViewGroup) {
            Utils.makeSenderSpanBold((ViewGroup) container);
        }
        this.mInboxText0 = (TextView) this.mView.findViewById(16908980);
    }

    /* access modifiers changed from: protected */
    public boolean isOneLine() {
        return super.isOneLine() && (this.mInboxText0 == null || TextUtils.isEmpty(this.mInboxText0.getText()));
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

    public void onContentUpdated(ExpandableNotificationRow row) {
        resolveViews();
        super.onContentUpdated(row);
        if (this.mInboxText0 != null && NotificationUtil.showMiuiStyle()) {
            setEndMargin(this.mInboxText0, 0);
        }
    }
}
