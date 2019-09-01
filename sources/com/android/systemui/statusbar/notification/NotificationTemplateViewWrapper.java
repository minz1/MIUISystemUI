package com.android.systemui.statusbar.notification;

import android.animation.ValueAnimator;
import android.app.Notification;
import android.app.NotificationCompat;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ChronometerCompat;
import android.widget.DateTimeView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.android.internal.widget.NotificationActionCompat;
import com.android.systemui.Constants;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.Util;
import com.android.systemui.miui.statusbar.ExpandedNotification;
import com.android.systemui.miui.statusbar.analytics.SystemUIStat;
import com.android.systemui.miui.statusbar.notification.HookViewHelper;
import com.android.systemui.miui.statusbar.notification.NotificationUtil;
import com.android.systemui.statusbar.CrossFadeHelper;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.TransformableView;
import com.android.systemui.statusbar.ViewTransformationHelper;
import com.android.systemui.statusbar.notification.NotificationViewWrapper;
import com.android.systemui.util.Utils;

public class NotificationTemplateViewWrapper extends NotificationHeaderViewWrapper {
    private ViewGroup mActionList;
    private View mActionsContainer;
    private ViewStub mChronometerStub;
    private int mContentHeight;
    protected LinearLayout mLine1Container;
    protected View mMainColumn;
    private int mMinHeightHint;
    private TextView mMiuiAction;
    protected View mNotificationMainContainer;
    protected ImageView mPicture;
    private ProgressBar mProgressBar;
    private int mProgressBarPaddingLine1;
    private int mProgressBarPaddingLine2;
    protected TextView mText;
    private TextView mTextLine1;
    private TextView mTextLine2;
    protected DateTimeView mTime;
    protected TextView mTitle;
    protected View mUpArrow;

    protected NotificationTemplateViewWrapper(Context ctx, View view, ExpandableNotificationRow row) {
        super(ctx, view, row);
        this.mTransformationHelper.setCustomTransformation(new ViewTransformationHelper.CustomTransformation() {
            public boolean transformTo(TransformState ownState, TransformableView notification, float transformationAmount) {
                if (!(notification instanceof HybridNotificationView)) {
                    return false;
                }
                TransformState otherState = notification.getCurrentState(1);
                CrossFadeHelper.fadeOut(ownState.getTransformedView(), transformationAmount);
                if (otherState != null) {
                    ownState.transformViewVerticalTo(otherState, this, transformationAmount);
                    otherState.recycle();
                }
                return true;
            }

            public boolean customTransformTarget(TransformState ownState, TransformState otherState) {
                ownState.setTransformationEndY(getTransformationY(ownState, otherState));
                return true;
            }

            public boolean transformFrom(TransformState ownState, TransformableView notification, float transformationAmount) {
                if (!(notification instanceof HybridNotificationView)) {
                    return false;
                }
                TransformState otherState = notification.getCurrentState(1);
                CrossFadeHelper.fadeIn(ownState.getTransformedView(), transformationAmount);
                if (otherState != null) {
                    ownState.transformViewVerticalFrom(otherState, this, transformationAmount);
                    otherState.recycle();
                }
                return true;
            }

            public boolean initTransformation(TransformState ownState, TransformState otherState) {
                ownState.setTransformationStartY(getTransformationY(ownState, otherState));
                return true;
            }

            private float getTransformationY(TransformState ownState, TransformState otherState) {
                return ((float) ((otherState.getLaidOutLocationOnScreen()[1] + otherState.getTransformedView().getHeight()) - ownState.getLaidOutLocationOnScreen()[1])) * 0.33f;
            }
        }, 2);
    }

    /* access modifiers changed from: protected */
    public void resolveViews(ExpandableNotificationRow row) {
        super.resolveViews(row);
        this.mPicture = (ImageView) this.mView.findViewById(16909273);
        if (this.mPicture != null) {
            this.mPicture.setTag(R.id.image_icon_tag, row.getStatusBarNotification().getNotification().getLargeIcon());
        }
        this.mLine1Container = (LinearLayout) this.mView.findViewById(16909040);
        this.mTitle = (TextView) this.mView.findViewById(16908310);
        this.mText = (TextView) this.mView.findViewById(16909408);
        this.mUpArrow = this.mView.findViewById(16909511);
        this.mTextLine1 = (TextView) this.mView.findViewById(16909436);
        this.mTextLine2 = (TextView) this.mView.findViewById(16909437);
        this.mProgressBarPaddingLine1 = this.mContext.getResources().getDimensionPixelSize(R.dimen.progress_bar_margin_top_1_line);
        this.mProgressBarPaddingLine2 = this.mContext.getResources().getDimensionPixelSize(R.dimen.progress_bar_margin_top_2_line);
        View progressView = this.mView.findViewById(16908301);
        if (progressView instanceof ProgressBar) {
            this.mProgressBar = (ProgressBar) progressView;
            if (Build.VERSION.SDK_INT == 23) {
                this.mProgressBar.setProgress(row.getEntry().notification.getNotification().extras.getInt("android.progress"));
            }
        } else {
            this.mProgressBar = null;
        }
        this.mActionsContainer = this.mView.findViewById(16908687);
        this.mActionList = (ViewGroup) this.mView.findViewById(16908686);
        this.mNotificationMainContainer = this.mView.findViewById(16909134);
        this.mMainColumn = this.mView.findViewById(16909138);
        this.mTime = this.mView.findViewById(16909150);
        this.mChronometerStub = (ViewStub) this.mView.findViewById(16909135);
        if (row.getEntry().notification.isShowMiuiAction()) {
            this.mMiuiAction = (TextView) this.mView.findViewById(16908663);
        }
        if (this.mTextLine2 != null && TextUtils.isEmpty(this.mTextLine2.getText())) {
            this.mTextLine2.setVisibility(8);
        }
        handleLine1();
        handleTitle();
        handleText();
        handleProgressBar();
        handleTextWithProgressBar();
        handleLargeIcon();
        handleMainContainerMargin();
        handleChronometerAndTime(row.getStatusBarNotification());
        handleActions();
        handleMiuiAction();
        handleTextSenderStyle();
        handleOneLineStyle();
        handleWorkProfileImage();
    }

    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            handleChronometerAndTime(this.mRow.getStatusBarNotification());
        }
    }

    /* access modifiers changed from: protected */
    public void handleLine1() {
        if (this.mLine1Container != null) {
            this.mLine1Container.setPadding(0, 0, 0, 0);
        }
    }

    /* access modifiers changed from: protected */
    public void handleTitle() {
    }

    /* access modifiers changed from: protected */
    public void handleText() {
        if (!isProgressBarVisible() && NotificationUtil.showMiuiStyle() && this.mText != null && TextUtils.isEmpty(this.mText.getText())) {
            Bundle extras = this.mRow.getEntry().notification.getNotification().extras;
            String text = extras.getString("android.text");
            if (TextUtils.isEmpty(text)) {
                text = extras.getString("android.subText");
            }
            if (!TextUtils.isEmpty(text)) {
                this.mText.setText(text);
                this.mText.setVisibility(0);
            }
        }
    }

    private void handleActions() {
        int i = 0;
        if (this.mActionList != null) {
            for (int i2 = 0; i2 < this.mActionList.getChildCount(); i2++) {
                View action = this.mActionList.getChildAt(i2).findViewById(16908663);
                if (NotificationUtil.showMiuiStyle() && (action instanceof Button)) {
                    ((Button) action).setTextColor(this.mContext.getColor(R.color.notification_actions_button_color));
                }
                NotificationActionCompat.setEmphasizedNotificationButtonBgIfNeed(action);
                NotificationActionCompat.removeCompoundDrawableIfNeed(action);
            }
        }
        if (this.mActionList != null) {
            while (true) {
                int i3 = i;
                if (i3 < this.mActionList.getChildCount()) {
                    final int actionIndex = i3;
                    HookViewHelper.hookView(this.mActionList.getChildAt(i3), new Runnable() {
                        public void run() {
                            ((SystemUIStat) Dependency.get(SystemUIStat.class)).onActionClick(NotificationTemplateViewWrapper.this.mRow.getStatusBarNotification(), actionIndex);
                        }
                    });
                    i = i3 + 1;
                } else {
                    return;
                }
            }
        }
    }

    private void handleMiuiAction() {
        if (this.mRow.getEntry().notification.isShowMiuiAction() && this.mMiuiAction != null) {
            Notification.Action[] actions = this.mRow.getStatusBarNotification().getNotification().actions;
            if (actions != null && actions.length > 0) {
                Notification.Action action = actions[0];
                if (!TextUtils.isEmpty(action.title)) {
                    this.mMiuiAction.setVisibility(0);
                    this.mMiuiAction.setText(action.title);
                    HookViewHelper.hookView(this.mMiuiAction, new Runnable() {
                        public void run() {
                            ((SystemUIStat) Dependency.get(SystemUIStat.class)).onActionClick(NotificationTemplateViewWrapper.this.mRow.getStatusBarNotification(), 0);
                        }
                    });
                }
            }
        }
    }

    private void handleProgressBar() {
        Bundle bundle = this.mRow.getEntry().notification.getNotification().extras;
        String subText = bundle.getString("android.subText");
        if (Constants.FORCE_SHOW_PROGRESS_BAR_WITH_SUBTEXT && !isProgressBarVisible() && !TextUtils.isEmpty(subText)) {
            int max = bundle.getInt("android.progressMax");
            int progress = bundle.getInt("android.progress");
            boolean indeterminate = bundle.getBoolean("android.progressIndeterminate");
            if (indeterminate || max != 0) {
                inflateProgressView();
                if (this.mProgressBar != null) {
                    this.mProgressBar.setVisibility(0);
                } else {
                    return;
                }
            }
            if (indeterminate) {
                this.mProgressBar.setIndeterminate(true);
            } else if (max != 0) {
                this.mProgressBar.setMax(max);
                this.mProgressBar.setProgress(progress);
            }
        }
        if (isProgressBarVisible() != 0 && !Constants.IS_INTERNATIONAL) {
            this.mProgressBar.setIndeterminateTintList(null);
            this.mProgressBar.setProgressTintList(null);
        }
    }

    private void inflateProgressView() {
        if (this.mProgressBar == null) {
            View progressView = this.mView.findViewById(16908301);
            if (progressView instanceof ViewStub) {
                this.mProgressBar = (ProgressBar) ((ViewStub) progressView).inflate();
            }
        }
    }

    private void handleTextWithProgressBar() {
        if (isProgressBarVisible() && NotificationUtil.showMiuiStyle()) {
            String contentText = this.mRow.getEntry().notification.getNotification().extras.getString("android.text");
            String subText = this.mRow.getEntry().notification.getNotification().extras.getString("android.subText");
            if (contentText != null) {
                if (this.mText != null) {
                    this.mText.setVisibility(0);
                    this.mText.setText(contentText);
                }
                if (this.mTextLine1 != null) {
                    this.mTextLine1.setVisibility(8);
                }
                if (this.mTextLine2 != null) {
                    this.mTextLine2.setText(subText);
                    this.mTextLine2.setVisibility(0);
                }
                setTopMargin(this.mProgressBar, this.mProgressBarPaddingLine2);
                return;
            }
            if (this.mText != null) {
                this.mText.setVisibility(8);
            }
            if (this.mTextLine1 != null) {
                this.mTextLine1.setVisibility(0);
                this.mTextLine1.setText(subText);
            }
            if (this.mTextLine2 != null) {
                this.mTextLine2.setVisibility(8);
            }
            setTopMargin(this.mProgressBar, this.mProgressBarPaddingLine1);
        }
    }

    /* access modifiers changed from: protected */
    public void handleLargeIcon() {
        if (this.mPicture != null) {
            ExpandedNotification sbn = this.mRow.getEntry().notification;
            if (this.mRow.isSummaryWithChildren() || this.mRow.isChildInGroup()) {
                this.mPicture.setVisibility(8);
            } else if (NotificationUtil.showMiuiStyle()) {
                NotificationUtil.applyRightIcon(this.mContext, sbn, this.mPicture);
                this.mPicture.setContentDescription(this.mRow.getAppName());
                this.mPicture.setVisibility(0);
            }
        }
        if (NotificationUtil.showMiuiStyle()) {
            if (this.mText != null) {
                setEndMargin(this.mText, 0);
            }
            if (this.mLine1Container != null) {
                setEndMargin(this.mLine1Container, 0);
            }
            if (this.mProgressBar != null) {
                setEndMargin(this.mProgressBar, 0);
            }
        }
    }

    /* access modifiers changed from: protected */
    public void handleMainContainerMargin() {
        int containerMarginBottom;
        if (this.mNotificationMainContainer != null) {
            if (this.mActionsContainer != null && this.mActionsContainer.getVisibility() != 8) {
                containerMarginBottom = this.mView.getResources().getDimensionPixelSize(17105204);
            } else if (NotificationUtil.showGoogleStyle() || this.mShowingType == NotificationViewWrapper.TYPE_SHOWING.TYPE_HEADSUP) {
                containerMarginBottom = this.mContext.getResources().getDimensionPixelSize(17105218);
            } else {
                containerMarginBottom = this.mView.getResources().getDimensionPixelSize(17105251);
            }
            setBottomMargin(this.mNotificationMainContainer, containerMarginBottom);
            setBottomMargin(this.mMainColumn, 0);
        }
    }

    private void handleChronometerAndTime(StatusBarNotification notification) {
        if (!NotificationUtil.showGoogleStyle() && !isProgressBarVisible() && !this.mRow.getEntry().notification.isShowMiuiAction()) {
            Notification n = notification.getNotification();
            if (NotificationCompat.showsChronometer(n) && this.mChronometerStub != null) {
                this.mChronometerStub.setVisibility(0);
                Chronometer chronometer = (Chronometer) this.mMainColumn.findViewById(16908802);
                chronometer.setBase(n.when + (SystemClock.elapsedRealtime() - System.currentTimeMillis()));
                chronometer.start();
                ChronometerCompat.setCountDown(chronometer, n.extras.getBoolean("android.chronometerCountDown"));
            } else if (NotificationCompat.showsTime(n) && this.mTime != null) {
                this.mTime.setVisibility(0);
                this.mTime.setTime(n.when);
            }
        }
    }

    /* access modifiers changed from: protected */
    public void handleHeaderStyle() {
        super.handleHeaderStyle();
        if (this.mPicture != null) {
            ViewGroup.LayoutParams layoutParams = this.mPicture.getLayoutParams();
            if (layoutParams instanceof FrameLayout.LayoutParams) {
                ((FrameLayout.LayoutParams) layoutParams).gravity = 8388661;
                ((FrameLayout.LayoutParams) layoutParams).topMargin = this.mContext.getResources().getDimensionPixelSize(R.dimen.notification_right_icon_margin_top_for_international);
                ((FrameLayout.LayoutParams) layoutParams).setMarginEnd(this.mContentMarginEndInternational);
                this.mPicture.setLayoutParams(layoutParams);
            }
            Util.setViewRoundCorner(this.mPicture, (float) this.mContext.getResources().getDimensionPixelSize(R.dimen.notification_large_icon_corner_radius_for_international));
        }
        updateUpArrowVisibility();
        View contentLayout = null;
        if (isNormalNotification() && this.mMainColumn != null) {
            ViewParent viewParent = this.mMainColumn.getParent();
            if (viewParent != null && (viewParent instanceof ViewGroup)) {
                contentLayout = (ViewGroup) viewParent;
            }
        }
        if (isBigNormalNotification() && this.mMainColumn != null) {
            contentLayout = this.mMainColumn;
        }
        if (contentLayout != null) {
            setTopMargin(contentLayout, this.mContentMarginTopInternational);
            setStartMargin(contentLayout, this.mContentMarginStartInternational);
        }
        if (this.mActionsContainer != null) {
            this.mActionsContainer.setPaddingRelative(this.mContentMarginStartInternational, 0, this.mContentMarginEndInternational, 0);
        }
    }

    private void handleTextSenderStyle() {
        if (this.mText != null) {
            Utils.makeSenderSpanBold(this.mText);
        }
    }

    private void handleOneLineStyle() {
        if (isOneLine()) {
            setTopMargin(this.mLine1Container, this.mContext.getResources().getDimensionPixelOffset(R.dimen.notification_one_line_title_margin_top));
        } else {
            setTopMargin(this.mLine1Container, 0);
        }
    }

    private void handleWorkProfileImage() {
        if (Build.VERSION.SDK_INT == 23 && this.mWorkProfileImage != null) {
            Drawable profileDrawable = this.mContext.getPackageManager().getUserBadgeForDensity(this.mRow.getEntry().notification.getUser(), 0);
            if (profileDrawable != null) {
                this.mWorkProfileImage.setImageDrawable(profileDrawable);
                this.mWorkProfileImage.setVisibility(0);
                return;
            }
            this.mWorkProfileImage.setVisibility(8);
        } else if (NotificationUtil.showMiuiStyle() && this.mWorkProfileImage != null && this.mWorkProfileImage.getVisibility() == 0 && this.mNotificationHeader != null && this.mNotificationHeader.indexOfChild(this.mWorkProfileImage) >= 0 && this.mTextLine2 != null && this.mTextLine2.getParent() != null) {
            this.mNotificationHeader.removeView(this.mWorkProfileImage);
            ((LinearLayout) this.mTextLine2.getParent()).addView(this.mWorkProfileImage);
        }
    }

    /* access modifiers changed from: protected */
    public boolean isOneLine() {
        return (this.mText == null || this.mText.getVisibility() == 8 || TextUtils.isEmpty(this.mText.getText())) && (this.mTextLine2 == null || TextUtils.isEmpty(this.mTextLine2.getText())) && this.mProgressBar == null;
    }

    private boolean isProgressBarVisible() {
        return this.mProgressBar != null && this.mProgressBar.getVisibility() == 0;
    }

    public void setIsChildInGroup(boolean isChildInGroup) {
        super.setIsChildInGroup(isChildInGroup);
        if (this.mPicture == null) {
            return;
        }
        if (this.mRow.isSummaryWithChildren() || this.mRow.isChildInGroup()) {
            this.mPicture.setVisibility(8);
        } else {
            this.mPicture.setVisibility(0);
        }
    }

    /* access modifiers changed from: protected */
    public void updateInvertHelper() {
        super.updateInvertHelper();
        if (this.mMainColumn != null) {
            this.mInvertHelper.addTarget(this.mMainColumn);
        }
    }

    /* access modifiers changed from: protected */
    public void updateTransformedTypes() {
        super.updateTransformedTypes();
        if (this.mTitle != null) {
            this.mTransformationHelper.addTransformedView(1, this.mTitle);
        }
        if (this.mText != null) {
            this.mTransformationHelper.addTransformedView(2, this.mText);
        }
        if (this.mPicture != null) {
            this.mTransformationHelper.addTransformedView(3, this.mPicture);
        }
        if (this.mProgressBar != null) {
            this.mTransformationHelper.addTransformedView(4, this.mProgressBar);
        }
    }

    /* access modifiers changed from: protected */
    public void removeTransformedTypes() {
        super.removeTransformedTypes();
        if (this.mRow.getStatusBarNotification().getNotification().getLargeIcon() == null) {
            this.mTransformationHelper.removeTransformedView(3);
        }
    }

    public void setDark(boolean dark, boolean fade, long delay) {
        if (dark != this.mDark || !this.mDarkInitialized) {
            super.setDark(dark, fade, delay);
            setPictureDark(dark, fade, delay);
            setProgressBarDark(dark, fade, delay);
        }
    }

    public void updateExpandability(boolean expandable, View.OnClickListener onClickListener) {
        super.updateExpandability(expandable, onClickListener);
        if (this.mUpArrow != null) {
            updateUpArrowVisibility();
            this.mUpArrow.setOnClickListener(expandable ? onClickListener : null);
        }
    }

    public void showPublic() {
        if (this.mText != null) {
            this.mText.setText("");
            handleOneLineStyle();
        }
    }

    private void updateUpArrowVisibility() {
        if (this.mUpArrow != null) {
            if (!this.mExpandable || this.mShowingType == NotificationViewWrapper.TYPE_SHOWING.TYPE_HEADSUP || !NotificationUtil.showMiuiStyle()) {
                this.mUpArrow.setVisibility(8);
            } else {
                this.mUpArrow.setVisibility(0);
            }
        }
    }

    public void initHandle() {
        if (this.mShowingType == NotificationViewWrapper.TYPE_SHOWING.TYPE_HEADSUP) {
            updateUpArrowVisibility();
            handleMainContainerMargin();
        }
    }

    private void setProgressBarDark(boolean dark, boolean fade, long delay) {
        if (this.mProgressBar == null) {
            return;
        }
        if (fade) {
            fadeProgressDark(this.mProgressBar, dark, delay);
        } else {
            updateProgressDark(this.mProgressBar, dark);
        }
    }

    private void fadeProgressDark(final ProgressBar target, boolean dark, long delay) {
        getDozer().startIntensityAnimation(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                NotificationTemplateViewWrapper.this.updateProgressDark(target, ((Float) animation.getAnimatedValue()).floatValue());
            }
        }, dark, delay, null);
    }

    /* access modifiers changed from: private */
    public void updateProgressDark(ProgressBar target, float intensity) {
        int interpolateColor = interpolateColor(this.mColor, -1, intensity);
    }

    private void updateProgressDark(ProgressBar target, boolean dark) {
        updateProgressDark(target, dark ? 1.0f : 0.0f);
    }

    private void setPictureDark(boolean dark, boolean fade, long delay) {
        if (this.mPicture != null) {
            getDozer().setImageDark(this.mPicture, dark, fade, delay, true);
        }
    }

    private static int interpolateColor(int source, int target, float t) {
        int aSource = Color.alpha(source);
        int rSource = Color.red(source);
        int gSource = Color.green(source);
        int bSource = Color.blue(source);
        return Color.argb((int) ((((float) aSource) * (1.0f - t)) + (((float) Color.alpha(target)) * t)), (int) ((((float) rSource) * (1.0f - t)) + (((float) Color.red(target)) * t)), (int) ((((float) gSource) * (1.0f - t)) + (((float) Color.green(target)) * t)), (int) ((((float) bSource) * (1.0f - t)) + (((float) Color.blue(target)) * t)));
    }

    public void setContentHeight(int contentHeight, int minHeightHint) {
        super.setContentHeight(contentHeight, minHeightHint);
        this.mContentHeight = contentHeight;
        this.mMinHeightHint = minHeightHint;
        if (this.mShowingType == NotificationViewWrapper.TYPE_SHOWING.TYPE_HEADSUP) {
            updateActionOffset();
        }
    }

    private void updateActionOffset() {
        if (this.mActionsContainer != null) {
            this.mActionsContainer.setTranslationY((float) (((Math.max(this.mContentHeight, this.mMinHeightHint) - this.mView.getHeight()) - this.mRow.getPaddingTop()) - this.mRow.getPaddingBottom()));
        }
    }

    private boolean isNormalNotification() {
        return this.mView.getId() == 16909384 && "base".equals(this.mView.getTag());
    }

    private boolean isBigNormalNotification() {
        return this.mView.getId() == 16909384 && "big".equals(this.mView.getTag());
    }

    public void setRemoteInputVisible(boolean visible) {
        if (this.mActionList != null) {
            this.mActionList.setVisibility(visible ? 4 : 0);
        }
    }
}
