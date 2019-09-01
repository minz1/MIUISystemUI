package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.IconCompat;
import android.util.ArrayMap;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.internal.util.NotificationColorUtil;
import com.android.systemui.R;
import com.android.systemui.Util;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.Icons;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.NotificationShelf;
import com.android.systemui.statusbar.StatusBarIconView;
import com.android.systemui.statusbar.notification.NotificationUtils;
import com.android.systemui.statusbar.policy.DarkIconDispatcher;
import com.android.systemui.statusbar.policy.DarkIconDispatcherHelper;
import com.android.systemui.statusbar.stack.NotificationStackScrollLayout;
import com.android.systemui.util.function.Function;
import java.util.ArrayList;

public class NotificationIconAreaController implements DarkIconDispatcher.DarkReceiver {
    private static int sFilterColor = 0;
    private int mClearableNotificationsCount;
    private Context mContext;
    private boolean mForceHideMoreIcon;
    private int mIconHPadding;
    private int mIconSize;
    private int mIconTint = -1;
    private StatusBarIconView mMoreIcon;
    private final NotificationColorUtil mNotificationColorUtil;
    protected View mNotificationIconArea;
    private NotificationIconContainer mNotificationIcons;
    private NotificationStackScrollLayout mNotificationScrollLayout;
    private NotificationIconContainer mShelfIcons;
    private boolean mShowNotificationIcons;
    private StatusBar mStatusBar;
    private final Rect mTintArea = new Rect();

    public NotificationIconAreaController(Context context, StatusBar statusBar) {
        this.mStatusBar = statusBar;
        this.mNotificationColorUtil = NotificationColorUtil.getInstance(context);
        this.mContext = context;
        initializeNotificationAreaViews(context);
    }

    /* access modifiers changed from: protected */
    public View inflateIconArea(LayoutInflater inflater) {
        return inflater.inflate(R.layout.notification_icon_area, null);
    }

    /* access modifiers changed from: protected */
    public void initializeNotificationAreaViews(Context context) {
        reloadDimens(context);
        this.mNotificationIconArea = inflateIconArea(LayoutInflater.from(context));
        this.mNotificationIcons = (NotificationIconContainer) this.mNotificationIconArea.findViewById(R.id.notificationIcons);
        this.mNotificationScrollLayout = this.mStatusBar.getNotificationScrollLayout();
    }

    public void setupShelf(NotificationShelf shelf) {
        this.mShelfIcons = shelf.getShelfIcons();
        shelf.setCollapsedIcons(this.mNotificationIcons);
    }

    public void setupClockContainer(View clockContainer) {
    }

    public void setMoreIcon(StatusBarIconView moreIcon) {
        this.mMoreIcon = moreIcon;
    }

    public void setForceHideMoreIcon(boolean force) {
        this.mForceHideMoreIcon = force;
        setIconsVisibility();
    }

    public void onDensityOrFontScaleChanged(Context context) {
        reloadDimens(context);
        FrameLayout.LayoutParams params = generateIconLayoutParams();
        for (int i = 0; i < this.mNotificationIcons.getChildCount(); i++) {
            this.mNotificationIcons.getChildAt(i).setLayoutParams(params);
        }
        for (int i2 = 0; i2 < this.mShelfIcons.getChildCount(); i2++) {
            this.mShelfIcons.getChildAt(i2).setLayoutParams(params);
        }
    }

    private FrameLayout.LayoutParams generateIconLayoutParams() {
        return new FrameLayout.LayoutParams(this.mIconSize + (2 * this.mIconHPadding), this.mContext.getResources().getDimensionPixelSize(R.dimen.status_bar_icon_size));
    }

    private void reloadDimens(Context context) {
        Resources res = context.getResources();
        this.mIconSize = res.getDimensionPixelSize(R.dimen.status_bar_icon_size);
        this.mIconHPadding = res.getDimensionPixelSize(R.dimen.status_bar_notification_icon_padding);
    }

    public View getNotificationInnerAreaView() {
        return this.mNotificationIconArea;
    }

    public void onDarkChanged(Rect tintArea, float darkIntensity, int iconTint) {
        if (tintArea == null) {
            this.mTintArea.setEmpty();
        } else {
            this.mTintArea.set(tintArea);
        }
        this.mIconTint = iconTint;
        applyNotificationIconsTint();
        if (this.mMoreIcon != null) {
            boolean isDarkMode = DarkIconDispatcherHelper.inDarkMode(tintArea, this.mMoreIcon, darkIntensity);
            this.mMoreIcon.setImageResource(Icons.get(Integer.valueOf(R.drawable.stat_notify_more), isDarkMode));
            Drawable drawable = this.mMoreIcon.getDrawable();
            if (drawable == null) {
                return;
            }
            if (!isDarkMode || !Util.showCtsSpecifiedColor()) {
                drawable.setColorFilter(null);
                return;
            }
            if (sFilterColor == 0) {
                sFilterColor = this.mContext.getResources().getColor(R.color.status_bar_icon_text_color_dark_mode_cts);
            }
            drawable.setColorFilter(sFilterColor, PorterDuff.Mode.SRC_IN);
        }
    }

    /* access modifiers changed from: protected */
    public boolean shouldShowNotificationIcon(NotificationData.Entry entry, NotificationData notificationData, boolean showAmbient) {
        if ((!notificationData.isAmbient(entry.key) || showAmbient) && StatusBar.isTopLevelChild(entry) && entry.row.getVisibility() != 8) {
            return true;
        }
        return false;
    }

    public void updateNotificationIcons(NotificationData notificationData) {
        int i = 0;
        updateIconsForLayout(notificationData, new Function<NotificationData.Entry, StatusBarIconView>() {
            public StatusBarIconView apply(NotificationData.Entry entry) {
                return entry.icon;
            }
        }, this.mNotificationIcons, false);
        updateIconsForLayout(notificationData, new Function<NotificationData.Entry, StatusBarIconView>() {
            public StatusBarIconView apply(NotificationData.Entry entry) {
                return entry.expandedIcon;
            }
        }, this.mShelfIcons, true);
        applyNotificationIconsTint();
        NotificationIconContainer notificationIconContainer = this.mNotificationIcons;
        if (!this.mShowNotificationIcons) {
            i = 8;
        }
        notificationIconContainer.setVisibility(i);
        this.mClearableNotificationsCount = notificationData.getClearableNotificationsCount();
        setIconsVisibility();
    }

    private void setIconsVisibility() {
        if (this.mMoreIcon != null) {
            this.mMoreIcon.setVisibility((this.mForceHideMoreIcon || this.mShowNotificationIcons || this.mClearableNotificationsCount <= 0) ? 8 : 0);
        }
    }

    private void updateIconsForLayout(NotificationData notificationData, Function<NotificationData.Entry, StatusBarIconView> function, NotificationIconContainer hostLayout, boolean showAmbient) {
        NotificationIconContainer notificationIconContainer = hostLayout;
        ArrayList<StatusBarIconView> toShow = new ArrayList<>(this.mNotificationScrollLayout.getChildCount());
        for (int i = 0; i < this.mNotificationScrollLayout.getChildCount(); i++) {
            View view = this.mNotificationScrollLayout.getChildAt(i);
            if (view instanceof ExpandableNotificationRow) {
                NotificationData.Entry ent = ((ExpandableNotificationRow) view).getEntry();
                if (shouldShowNotificationIcon(ent, notificationData, showAmbient)) {
                    toShow.add(function.apply(ent));
                } else {
                    Function<NotificationData.Entry, StatusBarIconView> function2 = function;
                }
            } else {
                NotificationData notificationData2 = notificationData;
                Function<NotificationData.Entry, StatusBarIconView> function3 = function;
                boolean z = showAmbient;
            }
        }
        NotificationData notificationData3 = notificationData;
        Function<NotificationData.Entry, StatusBarIconView> function4 = function;
        boolean z2 = showAmbient;
        ArrayMap<String, ArrayList<StatusBarIcon>> replacingIcons = new ArrayMap<>();
        ArrayList<View> toRemove = new ArrayList<>();
        int i2 = 0;
        while (i2 < hostLayout.getChildCount()) {
            View child = notificationIconContainer.getChildAt(i2);
            if ((child instanceof StatusBarIconView) && !toShow.contains(child)) {
                StatusBarIconView removedIcon = (StatusBarIconView) child;
                String removedGroupKey = removedIcon.getNotification().getGroupKey();
                boolean iconWasReplaced = false;
                int j = 0;
                while (true) {
                    if (j >= toShow.size()) {
                        break;
                    }
                    StatusBarIconView candidate = toShow.get(j);
                    if (IconCompat.sameAs(candidate.getSourceIcon(), removedIcon.getSourceIcon()) && candidate.getNotification().getGroupKey().equals(removedGroupKey)) {
                        if (iconWasReplaced) {
                            iconWasReplaced = false;
                            break;
                        }
                        iconWasReplaced = true;
                    }
                    j++;
                    NotificationData notificationData4 = notificationData;
                }
                if (iconWasReplaced) {
                    ArrayList<StatusBarIcon> statusBarIcons = replacingIcons.get(removedGroupKey);
                    if (statusBarIcons == null) {
                        statusBarIcons = new ArrayList<>();
                        replacingIcons.put(removedGroupKey, statusBarIcons);
                    }
                    statusBarIcons.add(removedIcon.getStatusBarIcon());
                }
                toRemove.add(removedIcon);
            }
            i2++;
            NotificationData notificationData5 = notificationData;
        }
        ArrayList<String> duplicates = new ArrayList<>();
        for (String key : replacingIcons.keySet()) {
            if (replacingIcons.get(key).size() != 1) {
                duplicates.add(key);
            }
        }
        replacingIcons.removeAll(duplicates);
        notificationIconContainer.setReplacingIcons(replacingIcons);
        int toRemoveCount = toRemove.size();
        for (int i3 = 0; i3 < toRemoveCount; i3++) {
            notificationIconContainer.removeView(toRemove.get(i3));
        }
        FrameLayout.LayoutParams params = generateIconLayoutParams();
        for (int i4 = 0; i4 < toShow.size(); i4++) {
            StatusBarIconView v = toShow.get(i4);
            notificationIconContainer.removeTransientView(v);
            if (v.getParent() == null) {
                notificationIconContainer.addView(v, i4, params);
            }
        }
        notificationIconContainer.setChangingViewPositions(true);
        int childCount = hostLayout.getChildCount();
        for (int i5 = 0; i5 < childCount; i5++) {
            View actual = notificationIconContainer.getChildAt(i5);
            StatusBarIconView expected = toShow.get(i5);
            if (actual != expected) {
                notificationIconContainer.removeView(expected);
                notificationIconContainer.addView(expected, i5);
            }
        }
        notificationIconContainer.setChangingViewPositions(false);
        notificationIconContainer.setReplacingIcons(null);
    }

    private void applyNotificationIconsTint() {
        for (int i = 0; i < this.mNotificationIcons.getChildCount(); i++) {
            applyNotificationIconTint((StatusBarIconView) this.mNotificationIcons.getChildAt(i));
        }
    }

    private void applyNotificationIconTint(StatusBarIconView v) {
        int color = 0;
        if (NotificationUtils.isGrayscale(v, this.mNotificationColorUtil)) {
            color = DarkIconDispatcherHelper.getTint(this.mTintArea, v, this.mIconTint);
        }
        v.setStaticDrawableColor(color);
        v.setDecorColor(this.mIconTint);
    }

    /* access modifiers changed from: package-private */
    public void setShowNotificationIcon(boolean show) {
        this.mShowNotificationIcons = show;
    }
}
