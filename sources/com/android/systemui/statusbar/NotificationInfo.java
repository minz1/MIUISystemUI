package com.android.systemui.statusbar;

import android.app.INotificationManager;
import android.app.NotificationChannelCompat;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.Util;
import com.android.systemui.miui.statusbar.ExpandedNotification;
import com.android.systemui.miui.statusbar.analytics.SystemUIStat;
import com.android.systemui.miui.statusbar.notification.NotificationUtil;
import com.android.systemui.statusbar.NotificationGuts;
import java.util.List;
import miui.util.NotificationFilterHelper;
import miui.widget.SlidingButton;

public class NotificationInfo extends BaseGutsContentView implements NotificationGuts.GutsContent {
    private SlidingButton mChannelEnabledSwitch;
    private ClickListener mClickListener;
    /* access modifiers changed from: private */
    public NotificationGuts mGutsContainer;
    private INotificationManager mINotificationManager;
    private boolean mIsDefaultChannel;
    private List<NotificationChannelCompat> mNotificationChannels;
    private ExpandedNotification mSbn;
    private TextView mSecondaryTextView;
    /* access modifiers changed from: private */
    public NotificationChannelCompat mSingleNotificationChannel;
    private int mStartingUserImportance;

    public interface ClickListener {
        void onClickCheckSave(Runnable runnable);

        void onClickDone(View view);

        void onClickSettings(View view, NotificationChannelCompat notificationChannelCompat, int i);
    }

    public NotificationInfo(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void bindNotification(INotificationManager iNotificationManager, List<NotificationChannelCompat> notificationChannels, int startingUserImportance, ExpandedNotification sbn, ClickListener listener) {
        String str;
        INotificationManager iNotificationManager2 = iNotificationManager;
        ExpandedNotification expandedNotification = sbn;
        final ClickListener clickListener = listener;
        this.mINotificationManager = iNotificationManager2;
        this.mNotificationChannels = notificationChannels;
        this.mStartingUserImportance = startingUserImportance;
        this.mSbn = expandedNotification;
        this.mClickListener = clickListener;
        String appName = sbn.getAppName();
        String appPkg = sbn.getPackageName();
        final int appUid = sbn.getAppUid();
        NotificationUtil.applyAppIcon(getContext(), expandedNotification, (ImageView) findViewById(R.id.pkgicon));
        int numTotalChannels = 1;
        try {
            numTotalChannels = NotificationChannelCompat.getNumNotificationChannelsForPackage(iNotificationManager2, appPkg, appUid, false);
        } catch (RemoteException e) {
        }
        if (!this.mNotificationChannels.isEmpty()) {
            if (this.mNotificationChannels.size() == 1) {
                this.mSingleNotificationChannel = this.mNotificationChannels.get(0);
                this.mIsDefaultChannel = this.mSingleNotificationChannel.getId().equals("miscellaneous");
            } else {
                this.mSingleNotificationChannel = null;
                this.mIsDefaultChannel = false;
            }
            CharSequence groupName = null;
            if (this.mSingleNotificationChannel != null) {
                try {
                    groupName = NotificationChannelCompat.getGroupName(this.mSingleNotificationChannel, iNotificationManager2, appPkg, appUid);
                } catch (RemoteException e2) {
                }
            }
            TextView titleView = (TextView) findViewById(R.id.title);
            if (TextUtils.isEmpty(groupName)) {
                str = appName;
            } else {
                str = getContext().getString(R.string.notification_group_divider_symbol, new Object[]{appName, groupName});
            }
            titleView.setText(str);
            initSlidingButton();
            this.mSecondaryTextView = (TextView) findViewById(R.id.secondary_text);
            updateSecondaryText();
            TextView settingsButton = (TextView) findViewById(R.id.button1);
            if (appUid >= 0) {
                settingsButton.setVisibility(0);
                settingsButton.setText(R.string.notification_more_settings);
                settingsButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        clickListener.onClickSettings(v, NotificationInfo.this.mSingleNotificationChannel, appUid);
                    }
                });
                if (numTotalChannels > 1) {
                    settingsButton.setText(R.string.notification_all_categories);
                } else {
                    settingsButton.setText(R.string.notification_more_settings);
                }
            } else {
                settingsButton.setVisibility(8);
            }
            TextView doneButton = (TextView) findViewById(R.id.button2);
            doneButton.setText(R.string.notification_done);
            doneButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    clickListener.onClickDone(v);
                }
            });
            return;
        }
        throw new IllegalArgumentException("bindNotification requires at least one channel");
    }

    private boolean hasImportanceChanged() {
        return (this.mSingleNotificationChannel == null || this.mChannelEnabledSwitch == null || this.mStartingUserImportance == getSelectedImportance()) ? false : true;
    }

    /* access modifiers changed from: private */
    public void saveImportance() {
        int appUid = this.mSbn.getAppUid();
        String appPkg = this.mSbn.getPackageName();
        if (this.mSingleNotificationChannel == null || this.mIsDefaultChannel) {
            NotificationFilterHelper.enableNotifications(this.mContext, appPkg, this.mChannelEnabledSwitch.isChecked());
        } else if (hasImportanceChanged()) {
            int selectedImportance = getSelectedImportance();
            MetricsLogger.action(this.mContext, 291, selectedImportance - this.mStartingUserImportance);
            if (this.mSbn.isSubstituteNotification()) {
                appPkg = this.mSbn.getBasePkg();
                ApplicationInfo info = Util.getApplicationInfo(this.mContext, appPkg, this.mSbn.getUser().getIdentifier());
                appUid = info != null ? info.uid : -1;
            }
            NotificationFilterHelper.saveBackupChannelImportance(this.mContext, appPkg, this.mSingleNotificationChannel.getId(), this.mSingleNotificationChannel.getImportance());
            NotificationChannelCompat.saveImportance(this.mSingleNotificationChannel, selectedImportance, this.mINotificationManager, appPkg, appUid);
        } else {
            return;
        }
        if (!this.mChannelEnabledSwitch.isChecked()) {
            Intent intent = new Intent("com.miui.app.ExtraStatusBarManager.action_refresh_notification");
            intent.setPackage("com.android.systemui");
            intent.putExtra("com.miui.app.ExtraStatusBarManager.extra_forbid_notification", !this.mChannelEnabledSwitch.isChecked());
            intent.putExtra("app_packageName", appPkg);
            String messageId = NotificationUtil.getMessageId(this.mSbn);
            if (!TextUtils.isEmpty(messageId)) {
                intent.putExtra("messageId", messageId);
            }
            this.mContext.sendBroadcast(intent);
            ((SystemUIStat) Dependency.get(SystemUIStat.class)).onBlock(this.mSbn);
        }
    }

    private int getSelectedImportance() {
        if (!this.mChannelEnabledSwitch.isChecked()) {
            return 0;
        }
        return this.mStartingUserImportance;
    }

    private void initSlidingButton() {
        this.mChannelEnabledSwitch = findViewById(R.id.channel_enabled_switch);
        boolean z = true;
        int i = 0;
        this.mChannelEnabledSwitch.setChecked(this.mStartingUserImportance != 0);
        if (NotificationFilterHelper.isNotificationForcedFor(this.mContext, this.mSbn.getPackageName()) || this.mSingleNotificationChannel == null) {
            z = false;
        }
        boolean visible = z;
        SlidingButton slidingButton = this.mChannelEnabledSwitch;
        if (!visible) {
            i = 8;
        }
        slidingButton.setVisibility(i);
        this.mChannelEnabledSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (NotificationInfo.this.mGutsContainer != null) {
                    NotificationInfo.this.mGutsContainer.resetFalsingCheck();
                }
                NotificationInfo.this.updateSecondaryText();
            }
        });
    }

    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        int i;
        super.onInitializeAccessibilityEvent(event);
        if (this.mGutsContainer != null && event.getEventType() == 32) {
            List text = event.getText();
            Context context = this.mContext;
            if (this.mGutsContainer.isExposed()) {
                i = R.string.notification_channel_controls_opened_accessibility;
            } else {
                i = R.string.notification_channel_controls_closed_accessibility;
            }
            text.add(context.getString(i, new Object[]{this.mSbn.getAppName()}));
        }
    }

    /* access modifiers changed from: private */
    public void updateSecondaryText() {
        int i;
        int i2;
        boolean disabled = this.mSingleNotificationChannel != null && getSelectedImportance() == 0;
        CharSequence channelNameText = getChannelNameText();
        TextView textView = this.mSecondaryTextView;
        Context context = this.mContext;
        if (disabled) {
            i = R.string.notification_info_channel_disabled_text;
        } else {
            i = R.string.notification_info_channel_enabled_text;
        }
        textView.setText(context.getString(i, new Object[]{this.mSbn.getAppName(), channelNameText}));
        TextView textView2 = this.mSecondaryTextView;
        Context context2 = this.mContext;
        if (disabled) {
            i2 = com.android.systemui.plugins.R.style.TextAppearance_NotificationInfo_Secondary_Warning;
        } else {
            i2 = 16974334;
        }
        textView2.setTextAppearance(context2, i2);
    }

    private CharSequence getChannelNameText() {
        CharSequence channelNameText = "";
        if (this.mSbn.isSubstituteNotification()) {
            return channelNameText;
        }
        if (this.mSingleNotificationChannel == null) {
            channelNameText = String.format(getContext().getResources().getQuantityString(R.plurals.notification_num_channels, this.mNotificationChannels.size()), new Object[]{Integer.valueOf(this.mNotificationChannels.size())});
        } else if (!this.mIsDefaultChannel) {
            channelNameText = this.mSingleNotificationChannel.getName();
        }
        return channelNameText;
    }

    public void setGutsParent(NotificationGuts guts) {
        this.mGutsContainer = guts;
    }

    public boolean willBeRemoved() {
        return this.mChannelEnabledSwitch != null && !this.mChannelEnabledSwitch.isChecked();
    }

    public boolean isLeavebehind() {
        return false;
    }

    public boolean handleCloseControls(boolean save, boolean force) {
        if (save && hasImportanceChanged()) {
            if (this.mClickListener != null) {
                this.mClickListener.onClickCheckSave(new Runnable() {
                    public void run() {
                        NotificationInfo.this.saveImportance();
                    }
                });
            } else {
                saveImportance();
            }
        }
        return false;
    }
}
