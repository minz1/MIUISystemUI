package com.android.systemui.statusbar.notification;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.internal.widget.MessagingLinearLayout;
import com.android.internal.widget.MessagingLinearLayoutCompat;
import com.android.systemui.miui.statusbar.notification.NotificationUtil;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.notification.NotificationViewWrapper;
import com.android.systemui.util.Utils;
import java.util.ArrayList;

public class NotificationMessagingTemplateViewWrapper extends NotificationTemplateViewWrapper {
    private View mContractedMessage;
    private ArrayList<View> mHistoricMessages = new ArrayList<>();
    private TextView mInboxText0;

    protected NotificationMessagingTemplateViewWrapper(Context ctx, View view, ExpandableNotificationRow row) {
        super(ctx, view, row);
    }

    private void resolveViews() {
        this.mContractedMessage = null;
        MessagingLinearLayout findNotificationMessagingView = NotificationViewWrapperCompat.findNotificationMessagingView(this.mView);
        if ((findNotificationMessagingView instanceof MessagingLinearLayout) && findNotificationMessagingView.getChildCount() > 0) {
            MessagingLinearLayout messagingContainer = findNotificationMessagingView;
            int childCount = messagingContainer.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = messagingContainer.getChildAt(i);
                if (child.getVisibility() == 8 && (child instanceof TextView) && !TextUtils.isEmpty(((TextView) child).getText())) {
                    this.mHistoricMessages.add(child);
                }
                if (child.getId() == MessagingLinearLayoutCompat.getContractedChildId(messagingContainer)) {
                    this.mContractedMessage = child;
                } else if (child.getVisibility() == 0) {
                    break;
                }
            }
            Utils.makeSenderSpanBold((ViewGroup) messagingContainer);
            if (NotificationUtil.showMiuiStyle()) {
                MessagingLinearLayoutCompat.setNumIndentLines(messagingContainer, 0);
            }
        }
        this.mInboxText0 = NotificationViewWrapperCompat.findInboxText0View(this.mView);
        if (NotificationUtil.showMiuiStyle() && findNotificationMessagingView != null && this.mShowingType == NotificationViewWrapper.TYPE_SHOWING.TYPE_CONTRACTED) {
            findNotificationMessagingView.setVisibility(8);
        }
    }

    /* access modifiers changed from: protected */
    public boolean isOneLine() {
        return super.isOneLine() && (this.mInboxText0 == null || TextUtils.isEmpty(this.mInboxText0.getText()));
    }

    /* access modifiers changed from: protected */
    public void handleHeaderStyle() {
        super.handleHeaderStyle();
        if (this.mMainColumn != null) {
            this.mMainColumn.setPaddingRelative(this.mContentMarginStartInternational, 0, this.mContentMarginEndInternational, 0);
        }
        if (this.mNotificationMainContainer != null) {
            setTopMargin(this.mNotificationMainContainer, this.mContentMarginTopInternational);
        }
    }

    /* access modifiers changed from: protected */
    public void handleLine1() {
        super.handleLine1();
        if (NotificationUtil.showGoogleStyle() && this.mLine1Container != null) {
            this.mLine1Container.setVisibility(8);
        }
    }

    /* access modifiers changed from: protected */
    public void handleTitle() {
        super.handleTitle();
        handleContentView(this.mTitle, "android.title");
    }

    /* access modifiers changed from: protected */
    public void handleText() {
        super.handleText();
        handleContentView(this.mText, "android.text");
    }

    private void handleContentView(TextView tv, String extraKey) {
        if (tv != null) {
            CharSequence currentText = tv.getText();
            CharSequence extraText = this.mRow.getStatusBarNotification().getNotification().extras.getCharSequence(extraKey);
            if (TextUtils.isEmpty(currentText) && !TextUtils.isEmpty(extraText)) {
                tv.setText(extraText);
            }
            if (NotificationUtil.showGoogleStyle()) {
                if (this.mShowingType == NotificationViewWrapper.TYPE_SHOWING.TYPE_CONTRACTED || this.mShowingType == NotificationViewWrapper.TYPE_SHOWING.TYPE_EXPANDED) {
                    tv.setVisibility(8);
                }
            } else if (this.mShowingType == NotificationViewWrapper.TYPE_SHOWING.TYPE_CONTRACTED) {
                tv.setVisibility(0);
            }
        }
    }

    /* access modifiers changed from: protected */
    public void handleMainContainerMargin() {
        if (this.mNotificationMainContainer != null) {
            if (this.mShowingType != NotificationViewWrapper.TYPE_SHOWING.TYPE_CONTRACTED || !NotificationUtil.showMiuiStyle()) {
                super.handleMainContainerMargin();
            } else {
                setBottomMargin(this.mNotificationMainContainer, 0);
            }
        }
    }

    public void updateExpandability(boolean expandable, View.OnClickListener onClickListener) {
        super.updateExpandability(expandable, onClickListener);
        if (NotificationUtil.showMiuiStyle() && this.mShowingType == NotificationViewWrapper.TYPE_SHOWING.TYPE_CONTRACTED && this.mUpArrow != null) {
            this.mUpArrow.setVisibility(8);
        }
    }

    public void onContentUpdated(ExpandableNotificationRow row) {
        resolveViews();
        super.onContentUpdated(row);
    }

    /* access modifiers changed from: protected */
    public void updateTransformedTypes() {
        super.updateTransformedTypes();
        if (this.mContractedMessage != null) {
            this.mTransformationHelper.addTransformedView(2, this.mContractedMessage);
        }
    }

    public void setRemoteInputVisible(boolean visible) {
        super.setRemoteInputVisible(visible);
        for (int i = 0; i < this.mHistoricMessages.size(); i++) {
            this.mHistoricMessages.get(i).setVisibility(visible ? 0 : 8);
        }
    }
}
