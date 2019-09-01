package com.android.systemui.statusbar.notification;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.systemui.R;
import com.android.systemui.statusbar.ExpandableNotificationRow;

public class OptimizedHeadsUpNotificationView extends LinearLayout {
    private TextView mAction;
    private Context mContext;
    private ImageView mIcon;
    private TextView mText;
    private TextView mTitle;
    private ExpandableNotificationRow row;

    public OptimizedHeadsUpNotificationView(Context context) {
        this(context, null);
    }

    public OptimizedHeadsUpNotificationView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OptimizedHeadsUpNotificationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mIcon = (ImageView) findViewById(R.id.icon);
        this.mTitle = (TextView) findViewById(R.id.title);
        this.mText = (TextView) findViewById(R.id.text);
        this.mAction = (TextView) findViewById(R.id.action);
    }

    public void setRow(ExpandableNotificationRow row2) {
        this.row = row2;
    }

    public void wrapIconView(ImageView icon) {
        if (this.mIcon != null && icon != null && icon.getDrawable() != null && icon.getDrawable().getConstantState() != null) {
            this.mIcon.setImageDrawable(icon.getDrawable().getConstantState().newDrawable());
        }
    }

    public void wrapTitleView(TextView title, boolean isGameMode) {
        int i;
        if (this.mTitle != null && title != null) {
            TextView textView = this.mTitle;
            Context context = this.mContext;
            if (isGameMode) {
                i = R.color.optimized_game_heads_up_notification_text;
            } else {
                i = R.color.optimized_heads_up_notification_text;
            }
            textView.setTextColor(context.getColor(i));
            if (!TextUtils.isEmpty(title.getText())) {
                this.mTitle.setVisibility(0);
                this.mTitle.setText(title.getText());
                return;
            }
            this.mTitle.setVisibility(8);
        }
    }

    public void wrapTextView(TextView text, boolean isGameMode) {
        int i;
        if (this.mText != null && text != null) {
            TextView textView = this.mText;
            Context context = this.mContext;
            if (isGameMode) {
                i = R.color.optimized_game_heads_up_notification_text;
            } else {
                i = R.color.optimized_heads_up_notification_text;
            }
            textView.setTextColor(context.getColor(i));
            if (this.mTitle != null && !TextUtils.isEmpty(text.getText()) && text.getText().toString().contains(this.mTitle.getText())) {
                this.mTitle.setVisibility(8);
            }
            this.mText.setText(text.getText());
        }
    }

    public TextView getActionView() {
        return this.mAction;
    }
}
