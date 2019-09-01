package com.android.systemui.statusbar;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.systemui.R;
import com.android.systemui.miui.statusbar.ExpandedNotification;
import com.android.systemui.miui.statusbar.notification.NotificationUtil;

public class NotificationFilter extends BaseGutsContentView {

    public interface ClickListener {
        void onClickCancel(View view);

        void onClickConfirm(View view);
    }

    public NotificationFilter(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void bindNotification(ExpandedNotification notification, final ClickListener listener) {
        int i;
        boolean isFold = notification.isFold();
        ((TextView) findViewById(R.id.title)).setText(isFold ? R.string.notification_menu_remove_from_filter_title : R.string.notification_menu_filter_title);
        findViewById(R.id.channel_enabled_switch).setVisibility(8);
        TextView secondaryTextView = (TextView) findViewById(R.id.secondary_text);
        Context context = getContext();
        if (isFold) {
            i = R.string.notification_menu_remove_from_filter_secondary_text;
        } else {
            i = R.string.notification_menu_filter_secondary_text;
        }
        secondaryTextView.setText(context.getString(i, new Object[]{notification.getAppName()}));
        NotificationUtil.applyAppIcon(getContext(), notification, (ImageView) findViewById(R.id.pkgicon));
        TextView cancelButton = (TextView) findViewById(R.id.button1);
        cancelButton.setText(17039360);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                listener.onClickCancel(v);
            }
        });
        TextView confirmButton = (TextView) findViewById(R.id.button2);
        confirmButton.setText(R.string.confirm);
        confirmButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                listener.onClickConfirm(v);
            }
        });
    }

    public void setGutsParent(NotificationGuts listener) {
    }

    public boolean handleCloseControls(boolean save, boolean force) {
        return false;
    }

    public boolean willBeRemoved() {
        return false;
    }

    public boolean isLeavebehind() {
        return false;
    }
}
