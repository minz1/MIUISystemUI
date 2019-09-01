package com.android.systemui.statusbar.notification;

import android.content.Context;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.util.ArraySet;
import android.view.NotificationHeaderView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.DateTimeView;
import android.widget.DateTimeViewCompat;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.ViewInvertHelper;
import com.android.systemui.miui.statusbar.ExpandedNotification;
import com.android.systemui.miui.statusbar.notification.NotificationUtil;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.TransformableView;
import com.android.systemui.statusbar.ViewTransformationHelper;
import com.android.systemui.statusbar.stack.NotificationChildrenContainer;
import java.util.Stack;
import miui.securityspace.XSpaceUserHandle;

public class NotificationHeaderViewWrapper extends NotificationViewWrapper {
    /* access modifiers changed from: private */
    public static final Interpolator LOW_PRIORITY_HEADER_CLOSE = new PathInterpolator(0.4f, 0.0f, 0.7f, 1.0f);
    protected TextView mAppNameText;
    protected int mColor;
    protected int mContentMarginEndInternational;
    protected int mContentMarginStartInternational;
    protected int mContentMarginTopInternational;
    private ImageView mExpandButton;
    protected boolean mExpandable;
    private TextView mHeaderText;
    private TextView mHeaderTextDivider;
    private View.OnAttachStateChangeListener mHeaderTimeAttachStateChangeListener;
    /* access modifiers changed from: private */
    public DateTimeView mHeaderTimeView;
    private ImageView mHeaderXSpaceIcon;
    private ImageView mIcon;
    protected final ViewInvertHelper mInvertHelper;
    /* access modifiers changed from: private */
    public boolean mIsLowPriority;
    private boolean mIsMiniView;
    protected NotificationHeaderView mNotificationHeader;
    /* access modifiers changed from: private */
    public boolean mTransformLowPriorityTitle;
    protected final ViewTransformationHelper mTransformationHelper = new ViewTransformationHelper();
    protected ImageView mWorkProfileImage;

    protected NotificationHeaderViewWrapper(Context ctx, View view, ExpandableNotificationRow row) {
        super(ctx, view, row);
        this.mInvertHelper = new ViewInvertHelper(ctx, 700);
        this.mTransformationHelper.setCustomTransformation(new CustomInterpolatorTransformation(1) {
            public Interpolator getCustomInterpolator(int interpolationType, boolean isFrom) {
                boolean isLowPriority = NotificationHeaderViewWrapper.this.mView instanceof NotificationHeaderView;
                if (interpolationType != 16) {
                    return null;
                }
                if ((!isLowPriority || isFrom) && (isLowPriority || !isFrom)) {
                    return NotificationHeaderViewWrapper.LOW_PRIORITY_HEADER_CLOSE;
                }
                return Interpolators.LINEAR_OUT_SLOW_IN;
            }

            /* access modifiers changed from: protected */
            public boolean hasCustomTransformation() {
                return NotificationHeaderViewWrapper.this.mIsLowPriority && NotificationHeaderViewWrapper.this.mTransformLowPriorityTitle;
            }
        }, 1);
        resolveViews(row);
        updateInvertHelper();
    }

    /* access modifiers changed from: protected */
    public NotificationDozeHelper createDozer(Context ctx) {
        return new NotificationIconDozeHelper(ctx);
    }

    /* access modifiers changed from: protected */
    public NotificationIconDozeHelper getDozer() {
        return (NotificationIconDozeHelper) super.getDozer();
    }

    public void onReinflated() {
        super.onReinflated();
        resolveViews(this.mRow);
    }

    /* access modifiers changed from: protected */
    public void resolveViews(ExpandableNotificationRow row) {
        initResources();
        this.mIcon = (ImageView) this.mView.findViewById(16908294);
        this.mAppNameText = NotificationViewWrapperCompat.findAppNameTextView(this.mView);
        if (this.mAppNameText != null) {
            this.mAppNameText.setText(row.getAppName());
        }
        this.mHeaderText = NotificationViewWrapperCompat.findHeaderTextView(this.mView);
        this.mHeaderTextDivider = NotificationViewWrapperCompat.findHeaderTextDividerView(this.mView);
        this.mExpandButton = NotificationViewWrapperCompat.findExpandButtonView(this.mView);
        ImageView imageView = this.mExpandButton;
        this.mHeaderXSpaceIcon = (ImageView) this.mView.findViewById(16909548);
        this.mWorkProfileImage = (ImageView) this.mView.findViewById(16909233);
        this.mHeaderTimeView = this.mView.findViewById(16909440);
        this.mNotificationHeader = NotificationViewWrapperCompat.findNotificationHeaderView(this.mView);
        this.mIsMiniView = this.mRow.getChildrenContainer() == null && this.mRow.isLowPriority() && this.mView == this.mNotificationHeader;
        if (NotificationUtil.showGoogleStyle() && this.mNotificationHeader != null) {
            this.mColor = this.mNotificationHeader.getOriginalNotificationColor();
        } else if (this.mIsMiniView) {
            this.mColor = this.mContext.getResources().getColor(R.color.notification_mini_view_icon_tint_color);
        } else {
            this.mColor = 0;
        }
        getDozer().setColor(this.mColor);
        handleHeaderVisibility();
        handleMiniView();
        handleXSpaceIcon();
        if (NotificationUtil.showGoogleStyle()) {
            handleHeaderStyle();
        }
    }

    private boolean isNotificationContainerHeader() {
        return this.mView.getParent() instanceof NotificationChildrenContainer;
    }

    private boolean isShowingPublicHeader() {
        return this.mRow.isShowingPublic() && this.mView == this.mNotificationHeader;
    }

    /* access modifiers changed from: protected */
    public void initResources() {
        this.mContentMarginTopInternational = this.mContext.getResources().getDimensionPixelSize(R.dimen.notification_content_margin_top_for_international);
        this.mContentMarginStartInternational = this.mContext.getResources().getDimensionPixelSize(R.dimen.notification_content_margin_start_for_international);
        this.mContentMarginEndInternational = this.mContext.getResources().getDimensionPixelSize(R.dimen.notification_content_margin_end_for_international);
    }

    private void handleHeaderVisibility() {
        if (this.mNotificationHeader != null) {
            this.mNotificationHeader.setVisibility(((this.mIsMiniView || NotificationUtil.showGoogleStyle() || (isShowingPublicHeader() && !isNotificationContainerHeader())) && !this.mRow.isMediaNotification()) ? 0 : 8);
        }
    }

    /* access modifiers changed from: protected */
    public void handleHeaderStyle() {
        if (this.mNotificationHeader != null) {
            this.mNotificationHeader.setPaddingRelative(this.mContentMarginStartInternational, 0, this.mContentMarginEndInternational, 0);
        }
        if (this.mIcon != null) {
            setStartMargin(this.mIcon, 0);
            setEndMargin(this.mIcon, this.mContext.getResources().getDimensionPixelSize(R.dimen.notification_header_icon_margin_end_for_international));
            Object tag = this.mIcon.getTag(R.id.custom_view_icon_applied);
            if ((tag instanceof Boolean) && ((Boolean) tag).booleanValue()) {
                int iconSize = this.mContext.getResources().getDimensionPixelSize(17105236);
                ViewGroup.LayoutParams lp = this.mIcon.getLayoutParams();
                lp.width = iconSize;
                lp.height = iconSize;
                this.mIcon.setLayoutParams(lp);
                this.mIcon.setTag(R.id.custom_view_icon_applied, false);
            }
            ExpandedNotification sbn = this.mRow.getEntry().notification;
            if (NotificationUtil.shouldSubstituteSmallIcon(sbn)) {
                this.mIcon.setImageDrawable(sbn.getAppIcon());
            }
        }
        if (this.mAppNameText != null) {
            setStartMargin(this.mAppNameText, this.mContext.getResources().getDimensionPixelSize(R.dimen.notification_header_app_name_margin_start_for_international));
        }
        if (this.mHeaderText != null) {
            setStartMargin(this.mHeaderText, this.mContext.getResources().getDimensionPixelSize(R.dimen.notification_header_separating_margin_for_international));
        }
        if (this.mHeaderTimeView != null && this.mHeaderTimeAttachStateChangeListener == null) {
            this.mHeaderTimeAttachStateChangeListener = new View.OnAttachStateChangeListener() {
                public void onViewAttachedToWindow(View v) {
                    DateTimeViewCompat.setShowRelativeTime(NotificationHeaderViewWrapper.this.mHeaderTimeView, true);
                }

                public void onViewDetachedFromWindow(View v) {
                }
            };
            this.mHeaderTimeView.addOnAttachStateChangeListener(this.mHeaderTimeAttachStateChangeListener);
        }
    }

    public void onContentUpdated(ExpandableNotificationRow row) {
        super.onContentUpdated(row);
        this.mIsLowPriority = row.isLowPriority();
        this.mTransformLowPriorityTitle = !row.isChildInGroup() && !row.isSummaryWithChildren();
        ArraySet<View> previousViews = this.mTransformationHelper.getAllTransformingViews();
        resolveViews(row);
        updateInvertHelper();
        updateTransformedTypes();
        addRemainingTransformTypes();
        removeTransformedTypes();
        updateCropToPaddingForImageViews();
        Icon smallIcon = NotificationUtil.getSmallIcon(this.mContext, row.getStatusBarNotification());
        this.mIcon.setTag(R.id.image_icon_tag, smallIcon);
        if (this.mWorkProfileImage != null) {
            this.mWorkProfileImage.setTag(R.id.image_icon_tag, smallIcon);
        }
        if (this.mHeaderXSpaceIcon != null) {
            this.mHeaderXSpaceIcon.setTag(R.id.image_icon_tag, smallIcon);
        }
        ArraySet<View> currentViews = this.mTransformationHelper.getAllTransformingViews();
        for (int i = 0; i < previousViews.size(); i++) {
            View view = previousViews.valueAt(i);
            if (!currentViews.contains(view)) {
                this.mTransformationHelper.resetTransformedView(view);
            }
        }
    }

    private void handleMiniView() {
        if (this.mIsMiniView) {
            this.mAppNameText.setTextColor(this.mContext.getColor(17170691));
            this.mHeaderTextDivider.setVisibility(8);
        }
    }

    private void handleXSpaceIcon() {
        if (this.mHeaderXSpaceIcon == null) {
            return;
        }
        if (XSpaceUserHandle.isXSpaceUser(this.mRow.getStatusBarNotification().getUser())) {
            this.mHeaderXSpaceIcon.setVisibility(0);
            this.mHeaderXSpaceIcon.setColorFilter(this.mColor);
            return;
        }
        this.mHeaderXSpaceIcon.setVisibility(8);
    }

    private void addRemainingTransformTypes() {
        this.mTransformationHelper.addRemainingTransformTypes(this.mView);
    }

    private void updateCropToPaddingForImageViews() {
        Stack<View> stack = new Stack<>();
        stack.push(this.mView);
        while (!stack.isEmpty()) {
            View child = stack.pop();
            if (child instanceof ImageView) {
                ((ImageView) child).setCropToPadding(true);
            } else if (child instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) child;
                for (int i = 0; i < group.getChildCount(); i++) {
                    stack.push(group.getChildAt(i));
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    public void updateInvertHelper() {
        this.mInvertHelper.clearTargets();
        if (this.mNotificationHeader != null) {
            for (int i = 0; i < this.mNotificationHeader.getChildCount(); i++) {
                View child = this.mNotificationHeader.getChildAt(i);
                if (child != this.mIcon) {
                    this.mInvertHelper.addTarget(child);
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    public void updateTransformedTypes() {
        this.mTransformationHelper.reset();
        this.mTransformationHelper.addTransformedView(0, this.mIcon);
        if (this.mIsLowPriority) {
            this.mTransformationHelper.addTransformedView(1, this.mHeaderText);
        }
    }

    /* access modifiers changed from: protected */
    public void removeTransformedTypes() {
    }

    public void setDark(boolean dark, boolean fade, long delay) {
        if ((dark != this.mDark || !this.mDarkInitialized) && Build.VERSION.SDK_INT <= 27) {
            super.setDark(dark, fade, delay);
            if (fade) {
                this.mInvertHelper.fade(dark, delay);
            } else {
                this.mInvertHelper.update(dark);
            }
            if (!(NotificationUtil.showGoogleStyle() && NotificationUtil.shouldSubstituteSmallIcon(this.mRow.getEntry().notification)) && this.mIcon != null && this.mNotificationHeader != null && !this.mRow.isChildInGroup() && !this.mRow.isCustomViewNotification()) {
                getDozer().setImageDark(this.mIcon, dark, fade, delay, !(this.mNotificationHeader.getOriginalIconColor() != 1));
            }
        }
    }

    public void updateExpandability(boolean expandable, View.OnClickListener onClickListener) {
        this.mExpandable = expandable;
        if (this.mExpandButton != null) {
            if (!NotificationUtil.showGoogleStyle() || !expandable || ((!this.mRow.isLowPriority() && this.mRow.getStatusBarNotification().isShowMiuiAction()) || this.mRow.isShowingPublic())) {
                this.mExpandButton.setVisibility(8);
            } else {
                this.mExpandButton.setVisibility(0);
            }
            this.mExpandButton.setAlpha(1.0f);
        }
        if (this.mNotificationHeader != null) {
            this.mNotificationHeader.setOnClickListener(expandable ? onClickListener : null);
        }
    }

    public NotificationHeaderView getNotificationHeader() {
        return this.mNotificationHeader;
    }

    public TransformState getCurrentState(int fadingView) {
        return this.mTransformationHelper.getCurrentState(fadingView);
    }

    public void transformTo(TransformableView notification, Runnable endRunnable) {
        this.mTransformationHelper.transformTo(notification, endRunnable);
    }

    public void transformTo(TransformableView notification, float transformationAmount) {
        this.mTransformationHelper.transformTo(notification, transformationAmount);
    }

    public void transformFrom(TransformableView notification) {
        this.mTransformationHelper.transformFrom(notification);
    }

    public void transformFrom(TransformableView notification, float transformationAmount) {
        this.mTransformationHelper.transformFrom(notification, transformationAmount);
    }

    public void setIsChildInGroup(boolean isChildInGroup) {
        super.setIsChildInGroup(isChildInGroup);
        this.mTransformLowPriorityTitle = !isChildInGroup;
    }

    public void setVisible(boolean visible) {
        super.setVisible(visible);
        this.mTransformationHelper.setVisible(visible);
    }

    /* access modifiers changed from: protected */
    public void setStartMargin(View v, int startMargin) {
        if (v != null) {
            ViewGroup.LayoutParams layoutParams = v.getLayoutParams();
            if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
                ((ViewGroup.MarginLayoutParams) layoutParams).setMarginStart(startMargin);
                v.setLayoutParams(layoutParams);
            }
        }
    }

    /* access modifiers changed from: protected */
    public void setEndMargin(View v, int endMargin) {
        if (v != null) {
            ViewGroup.LayoutParams layoutParams = v.getLayoutParams();
            if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
                ((ViewGroup.MarginLayoutParams) layoutParams).setMarginEnd(endMargin);
                v.setLayoutParams(layoutParams);
            }
        }
    }

    /* access modifiers changed from: protected */
    public void setTopMargin(View v, int topMargin) {
        if (v != null) {
            ViewGroup.LayoutParams layoutParams = v.getLayoutParams();
            if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
                ((ViewGroup.MarginLayoutParams) layoutParams).topMargin = topMargin;
                v.setLayoutParams(layoutParams);
            }
        }
    }

    /* access modifiers changed from: protected */
    public void setBottomMargin(View v, int bottomMargin) {
        if (v != null) {
            ViewGroup.LayoutParams layoutParams = v.getLayoutParams();
            if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
                ((ViewGroup.MarginLayoutParams) layoutParams).bottomMargin = bottomMargin;
                v.setLayoutParams(layoutParams);
            }
        }
    }
}
