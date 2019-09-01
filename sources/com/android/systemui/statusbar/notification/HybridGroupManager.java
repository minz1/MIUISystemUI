package com.android.systemui.statusbar.notification;

import android.app.Notification;
import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.systemui.R;
import com.android.systemui.miui.statusbar.notification.NotificationUtil;
import com.android.systemui.util.function.Consumer;

public class HybridGroupManager {
    private final Context mContext;
    /* access modifiers changed from: private */
    public float mDarkAmount = 0.0f;
    private final NotificationDozeHelper mDozer;
    private int mOverflowNumberColor;
    private int mOverflowNumberColorDark;
    private final int mOverflowNumberPadding;
    private final ViewGroup mParent;

    public HybridGroupManager(Context ctx, ViewGroup parent) {
        this.mContext = ctx;
        this.mParent = parent;
        this.mDozer = new NotificationDozeHelper();
        this.mOverflowNumberPadding = this.mContext.getResources().getDimensionPixelSize(R.dimen.group_overflow_number_padding);
    }

    private HybridNotificationView inflateHybridViewWithStyle(int style) {
        HybridNotificationView hybrid = (HybridNotificationView) ((LayoutInflater) new ContextThemeWrapper(this.mContext, style).getSystemService(LayoutInflater.class)).inflate(R.layout.hybrid_notification, this.mParent, false);
        if (NotificationUtil.showGoogleStyle()) {
            hybrid.setPaddingRelative(this.mContext.getResources().getDimensionPixelSize(R.dimen.notification_content_margin_start_for_international), 0, this.mContext.getResources().getDimensionPixelSize(R.dimen.notification_content_margin_end_for_international), 0);
        }
        this.mParent.addView(hybrid);
        return hybrid;
    }

    private TextView inflateOverflowNumber() {
        TextView numberView = (TextView) ((LayoutInflater) this.mContext.getSystemService(LayoutInflater.class)).inflate(R.layout.hybrid_overflow_number, this.mParent, false);
        this.mParent.addView(numberView);
        updateOverFlowNumberColor(numberView);
        return numberView;
    }

    /* access modifiers changed from: private */
    public void updateOverFlowNumberColor(TextView numberView) {
        numberView.setTextColor(NotificationUtils.interpolateColors(this.mOverflowNumberColor, this.mOverflowNumberColorDark, this.mDarkAmount));
    }

    public void setOverflowNumberColor(TextView numberView, int colorRegular, int colorDark) {
        this.mOverflowNumberColor = colorRegular;
        this.mOverflowNumberColorDark = colorDark;
        if (numberView != null) {
            updateOverFlowNumberColor(numberView);
        }
    }

    public HybridNotificationView bindFromNotification(HybridNotificationView reusableView, Notification notification) {
        return bindFromNotificationWithStyle(reusableView, notification, R.style.HybridNotification);
    }

    public HybridNotificationView bindAmbientFromNotification(HybridNotificationView reusableView, Notification notification) {
        return bindFromNotificationWithStyle(reusableView, notification, com.android.systemui.plugins.R.style.HybridNotification_Ambient);
    }

    private HybridNotificationView bindFromNotificationWithStyle(HybridNotificationView reusableView, Notification notification, int style) {
        if (reusableView == null) {
            reusableView = inflateHybridViewWithStyle(style);
        }
        reusableView.bind(resolveTitle(notification), resolveText(notification));
        return reusableView;
    }

    private CharSequence resolveText(Notification notification) {
        CharSequence contentText = notification.extras.getCharSequence("android.text");
        if (contentText == null) {
            return notification.extras.getCharSequence("android.bigText");
        }
        return contentText;
    }

    private CharSequence resolveTitle(Notification notification) {
        CharSequence titleText = notification.extras.getCharSequence("android.title");
        if (titleText == null) {
            return notification.extras.getCharSequence("android.title.big");
        }
        return titleText;
    }

    public TextView bindOverflowNumber(TextView reusableView, int number) {
        if (reusableView == null) {
            reusableView = inflateOverflowNumber();
        }
        String text = this.mContext.getResources().getString(R.string.notification_group_overflow_indicator, new Object[]{Integer.valueOf(number)});
        if (!text.equals(reusableView.getText())) {
            reusableView.setText(text);
        }
        reusableView.setContentDescription(String.format(this.mContext.getResources().getQuantityString(R.plurals.notification_group_overflow_description, number), new Object[]{Integer.valueOf(number)}));
        return reusableView;
    }

    public void setOverflowNumberDark(final TextView view, boolean dark, boolean fade, long delay) {
        this.mDozer.setIntensityDark(new Consumer<Float>() {
            public void accept(Float f) {
                float unused = HybridGroupManager.this.mDarkAmount = f.floatValue();
                HybridGroupManager.this.updateOverFlowNumberColor(view);
            }
        }, dark, fade, delay);
    }

    public int getOverflowNumberPadding() {
        return this.mOverflowNumberPadding;
    }
}
