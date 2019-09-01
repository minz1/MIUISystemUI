package com.android.systemui.statusbar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.NotificationCompat;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.util.AttributeSet;
import android.util.FloatProperty;
import android.util.Log;
import android.util.Property;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.NotificationHeaderView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RemoteViews;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.Constants;
import com.android.systemui.Dependency;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.classifier.FalsingManager;
import com.android.systemui.miui.statusbar.ExpandedNotification;
import com.android.systemui.miui.statusbar.InCallUtils;
import com.android.systemui.miui.statusbar.analytics.SystemUIStat;
import com.android.systemui.miui.statusbar.notification.NotificationUtil;
import com.android.systemui.plugins.PluginListener;
import com.android.systemui.plugins.PluginManager;
import com.android.systemui.plugins.statusbar.NotificationMenuRowPlugin;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.NotificationGuts;
import com.android.systemui.statusbar.notification.HybridNotificationView;
import com.android.systemui.statusbar.notification.InCallNotificationView;
import com.android.systemui.statusbar.notification.NotificationInflater;
import com.android.systemui.statusbar.notification.VisualStabilityManager;
import com.android.systemui.statusbar.phone.NotificationGroupManager;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import com.android.systemui.statusbar.stack.AnimationProperties;
import com.android.systemui.statusbar.stack.ExpandableViewState;
import com.android.systemui.statusbar.stack.NotificationChildrenContainer;
import com.android.systemui.statusbar.stack.NotificationStackScrollLayout;
import com.android.systemui.statusbar.stack.StackScrollState;
import java.util.ArrayList;
import java.util.List;

public class ExpandableNotificationRow extends ActivatableNotificationView implements PluginListener<NotificationMenuRowPlugin> {
    private static final Property<ExpandableNotificationRow, Float> TRANSLATE_CONTENT = new FloatProperty<ExpandableNotificationRow>("translate") {
        public void setValue(ExpandableNotificationRow object, float value) {
            object.setTranslation(value);
        }

        public Float get(ExpandableNotificationRow object) {
            return Float.valueOf(object.getTranslation());
        }
    };
    private boolean mAboveShelf;
    private String mAppName;
    private View mChildAfterViewWhenDismissed;
    /* access modifiers changed from: private */
    public NotificationChildrenContainer mChildrenContainer;
    private ViewStub mChildrenContainerStub;
    private boolean mChildrenExpanded;
    private float mContentTransformationAmount;
    private boolean mDismissAllInProgress;
    private boolean mDismissed;
    /* access modifiers changed from: private */
    public NotificationData.Entry mEntry;
    private View.OnClickListener mExpandClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            boolean nowExpanded;
            if (ExpandableNotificationRow.this.mShowingPublic || ((ExpandableNotificationRow.this.mIsLowPriority && !ExpandableNotificationRow.this.isExpanded()) || !ExpandableNotificationRow.this.mGroupManager.isSummaryOfGroup(ExpandableNotificationRow.this.mStatusBarNotification))) {
                if (v.isAccessibilityFocused()) {
                    ExpandableNotificationRow.this.mPrivateLayout.setFocusOnVisibilityChange();
                }
                if (ExpandableNotificationRow.this.isPinned()) {
                    nowExpanded = !ExpandableNotificationRow.this.mExpandedWhenPinned;
                    boolean unused = ExpandableNotificationRow.this.mExpandedWhenPinned = nowExpanded;
                } else {
                    nowExpanded = !ExpandableNotificationRow.this.isExpanded();
                    ExpandableNotificationRow.this.setUserExpanded(nowExpanded);
                }
                ExpandableNotificationRow.this.notifyHeightChanged(true);
                ExpandableNotificationRow.this.mOnExpandClickListener.onExpandClicked(ExpandableNotificationRow.this.mEntry, nowExpanded);
                MetricsLogger.action(ExpandableNotificationRow.this.mContext, 407, nowExpanded);
                return;
            }
            boolean unused2 = ExpandableNotificationRow.this.mGroupExpansionChanging = true;
            boolean wasExpanded = ExpandableNotificationRow.this.mGroupManager.isGroupExpanded(ExpandableNotificationRow.this.mStatusBarNotification);
            boolean nowExpanded2 = ExpandableNotificationRow.this.mGroupManager.toggleGroupExpansion(ExpandableNotificationRow.this.mStatusBarNotification);
            ExpandableNotificationRow.this.mOnExpandClickListener.onExpandClicked(ExpandableNotificationRow.this.mEntry, nowExpanded2);
            MetricsLogger.action(ExpandableNotificationRow.this.mContext, 408, nowExpanded2);
            ExpandableNotificationRow.this.onExpansionChanged(true, wasExpanded);
        }
    };
    private boolean mExpandable;
    /* access modifiers changed from: private */
    public boolean mExpandedWhenPinned;
    private FalsingManager mFalsingManager;
    private boolean mForceUnlocked;
    /* access modifiers changed from: private */
    public boolean mGroupExpansionChanging;
    /* access modifiers changed from: private */
    public NotificationGroupManager mGroupManager;
    private View mGroupParentWhenDismissed;
    /* access modifiers changed from: private */
    public NotificationGuts mGuts;
    /* access modifiers changed from: private */
    public ViewStub mGutsStub;
    private boolean mHasExtraBottomPadding;
    private boolean mHasExtraTopPadding;
    private boolean mHasUserChangedExpansion;
    private int mHeadsUpHeight;
    private HeadsUpManager mHeadsUpManager;
    private boolean mHeadsupDisappearRunning;
    private boolean mHideSensitiveForIntrinsicHeight;
    private boolean mIconAnimationRunning;
    private int mIconTransformContentShift;
    private int mIconTransformContentShiftNoIcon;
    private boolean mIconsVisible = true;
    private int mIncreasedPaddingBetweenElements;
    private boolean mIsColorized;
    private boolean mIsFirstRow;
    private boolean mIsHeadsUp;
    private boolean mIsLastChild;
    /* access modifiers changed from: private */
    public boolean mIsLowPriority;
    private boolean mIsPinned;
    private boolean mIsShowHeadsUpBackground;
    private boolean mIsSummaryWithChildren;
    private boolean mIsSystemChildExpanded;
    private boolean mIsSystemExpanded;
    private boolean mJustClicked;
    private boolean mKeepInParent;
    private ImageView mLargeIcon;
    private boolean mLastChronometerRunning = true;
    private LayoutListener mLayoutListener;
    private NotificationContentView[] mLayouts;
    private ExpansionLogger mLogger;
    private String mLoggingKey;
    private boolean mLowPriorityStateUpdated;
    private int mMaxExpandHeight;
    private int mMaxHeadsUpHeight;
    private int mMaxHeadsUpHeightIncreased;
    private int mMaxHeadsUpHeightLegacy;
    /* access modifiers changed from: private */
    public NotificationMenuRowPlugin mMenuRow;
    private boolean mNeedDrawBgBottomDivider;
    private boolean mNeedDrawBgTopDivider;
    private int mNotificationAmbientHeight;
    private int mNotificationHeadsUpBgRadius;
    private final NotificationInflater mNotificationInflater;
    private int mNotificationMaxHeight;
    private int mNotificationMinHeight;
    private int mNotificationMinHeightLarge;
    private int mNotificationMinHeightLegacy;
    private ExpandableNotificationRow mNotificationParent;
    private View.OnClickListener mOnClickListener;
    private Runnable mOnDismissRunnable;
    /* access modifiers changed from: private */
    public OnExpandClickListener mOnExpandClickListener;
    private boolean mOnKeyguard;
    /* access modifiers changed from: private */
    public NotificationContentView mPrivateLayout;
    private NotificationContentView mPublicLayout;
    private boolean mRefocusOnDismiss;
    private boolean mRemoved;
    private boolean mSensitive;
    private boolean mSensitiveHiddenInGeneral;
    private boolean mShowAmbient;
    private boolean mShowNoBackground;
    /* access modifiers changed from: private */
    public boolean mShowingPublic;
    private boolean mShowingPublicInitialized;
    /* access modifiers changed from: private */
    public ExpandedNotification mStatusBarNotification;
    /* access modifiers changed from: private */
    public Animator mTranslateAnim;
    /* access modifiers changed from: private */
    public ArrayList<View> mTranslateableViews;
    private float mTranslationWhenRemoved;
    private boolean mUseIncreasedCollapsedHeight;
    private boolean mUseIncreasedHeadsUpHeight;
    private boolean mUserExpanded;
    private boolean mUserLocked;
    private boolean mWasChildInGroupWhenRemoved;

    public interface ExpansionLogger {
        void logNotificationExpansion(String str, boolean z, boolean z2);
    }

    public interface LayoutListener {
        void onLayout();
    }

    public static class NotificationViewState extends ExpandableViewState {
        private final StackScrollState mOverallState;

        private NotificationViewState(StackScrollState stackScrollState) {
            this.mOverallState = stackScrollState;
        }

        public void applyToView(View view) {
            super.applyToView(view);
            if (view instanceof ExpandableNotificationRow) {
                ((ExpandableNotificationRow) view).applyChildrenState(this.mOverallState);
            }
        }

        /* access modifiers changed from: protected */
        public void onYTranslationAnimationFinished(View view) {
            super.onYTranslationAnimationFinished(view);
            if (view instanceof ExpandableNotificationRow) {
                ExpandableNotificationRow row = (ExpandableNotificationRow) view;
                if (row.isHeadsUpAnimatingAway()) {
                    row.setHeadsUpAnimatingAway(false);
                }
            }
        }

        public void animateTo(View child, AnimationProperties properties) {
            super.animateTo(child, properties);
            if (child instanceof ExpandableNotificationRow) {
                ((ExpandableNotificationRow) child).startChildAnimation(this.mOverallState, properties);
            }
        }
    }

    public interface OnExpandClickListener {
        void onExpandClicked(NotificationData.Entry entry, boolean z);
    }

    public boolean isGroupExpansionChanging() {
        if (isChildInGroup()) {
            return this.mNotificationParent.isGroupExpansionChanging();
        }
        return this.mGroupExpansionChanging;
    }

    public void setGroupExpansionChanging(boolean changing) {
        this.mGroupExpansionChanging = changing;
    }

    public boolean isDismissAllInProgress() {
        return this.mDismissAllInProgress;
    }

    public void setDismissAllInProgress(boolean dismissAllInProgress) {
        if (this.mDismissAllInProgress != dismissAllInProgress) {
            this.mDismissAllInProgress = dismissAllInProgress;
        }
    }

    public void setActualHeightAnimating(boolean animating) {
        if (this.mPrivateLayout != null) {
            this.mPrivateLayout.setContentHeightAnimating(animating);
        }
    }

    public NotificationContentView getPrivateLayout() {
        return this.mPrivateLayout;
    }

    public NotificationContentView getPublicLayout() {
        return this.mPublicLayout;
    }

    public void setIconAnimationRunning(boolean running) {
        int i = 0;
        for (NotificationContentView l : this.mLayouts) {
            setIconAnimationRunning(running, l);
        }
        if (this.mIsSummaryWithChildren) {
            setIconAnimationRunningForChild(running, this.mChildrenContainer.getHeaderView());
            setIconAnimationRunningForChild(running, this.mChildrenContainer.getLowPriorityHeaderView());
            List<ExpandableNotificationRow> notificationChildren = this.mChildrenContainer.getNotificationChildren();
            while (true) {
                int i2 = i;
                if (i2 >= notificationChildren.size()) {
                    break;
                }
                notificationChildren.get(i2).setIconAnimationRunning(running);
                i = i2 + 1;
            }
        }
        this.mIconAnimationRunning = running;
    }

    private void setIconAnimationRunning(boolean running, NotificationContentView layout) {
        if (layout != null) {
            View contractedChild = layout.getContractedChild();
            View expandedChild = layout.getExpandedChild();
            View headsUpChild = layout.getHeadsUpChild();
            setIconAnimationRunningForChild(running, contractedChild);
            setIconAnimationRunningForChild(running, expandedChild);
            setIconAnimationRunningForChild(running, headsUpChild);
        }
    }

    private void setIconAnimationRunningForChild(boolean running, View child) {
        if (child != null) {
            setIconRunning((ImageView) child.findViewById(16908294), running);
            setIconRunning((ImageView) child.findViewById(16909273), running);
        }
    }

    private void setIconRunning(ImageView imageView, boolean running) {
        if (imageView != null) {
            Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AnimationDrawable) {
                AnimationDrawable animationDrawable = (AnimationDrawable) drawable;
                if (running) {
                    animationDrawable.start();
                } else {
                    animationDrawable.stop();
                }
            } else if (drawable instanceof AnimatedVectorDrawable) {
                AnimatedVectorDrawable animationDrawable2 = (AnimatedVectorDrawable) drawable;
                if (running) {
                    animationDrawable2.start();
                } else {
                    animationDrawable2.stop();
                }
            }
        }
    }

    public void updateNotification(NotificationData.Entry entry) {
        boolean isNew = this.mEntry == null;
        this.mEntry = entry;
        this.mStatusBarNotification = entry.notification;
        this.mNotificationInflater.inflateNotificationViews();
        if (isNew && isMediaNotification()) {
            resetUserExpansion();
        }
    }

    /* access modifiers changed from: private */
    public void addLargeIconIfNeeded() {
        if (!NotificationUtil.showGoogleStyle() && this.mChildrenContainer != null && this.mLargeIcon == null) {
            this.mLargeIcon = (ImageView) LayoutInflater.from(this.mContext).inflate(17367223, this, false);
            if (this.mLargeIcon != null) {
                addView(this.mLargeIcon);
                NotificationUtil.applyRightIcon(this.mContext, getEntry().notification, this.mLargeIcon);
                this.mLargeIcon.setContentDescription(this.mAppName);
                this.mLargeIcon.setVisibility(0);
                this.mTranslateableViews.add(this.mLargeIcon);
            }
        }
    }

    public void updateLargeIconVisibility(boolean visible) {
        if (this.mLargeIcon != null) {
            this.mLargeIcon.setVisibility(visible ? 0 : 8);
        }
    }

    public void onNotificationUpdated() {
        for (NotificationContentView l : this.mLayouts) {
            l.onNotificationUpdated(this.mEntry);
        }
        this.mIsColorized = NotificationCompat.isColorized(this.mStatusBarNotification.getNotification());
        this.mShowingPublicInitialized = false;
        if (this.mMenuRow != null) {
            this.mMenuRow.onNotificationUpdated();
        }
        if (this.mIsSummaryWithChildren) {
            this.mChildrenContainer.recreateNotificationHeader(this.mExpandClickListener);
            this.mChildrenContainer.onNotificationUpdated();
        }
        if (this.mIconAnimationRunning) {
            setIconAnimationRunning(true);
        }
        if (this.mNotificationParent != null) {
            this.mNotificationParent.updateChildrenHeaderAppearance();
        }
        onChildrenCountChanged();
        this.mPublicLayout.updateExpandButtons(true);
        updateLimits();
        updateIconVisibilities();
    }

    public boolean isDimmable() {
        if (!getShowingLayout().isDimmable()) {
            return false;
        }
        return super.isDimmable();
    }

    private void updateLimits() {
        for (NotificationContentView l : this.mLayouts) {
            updateLimitsForView(l);
        }
    }

    private void updateLimitsForView(NotificationContentView layout) {
        int minHeight;
        int headsUpheight;
        boolean headsUpCustom = false;
        boolean customView = (layout.getContractedChild() == null || layout.getContractedChild().getId() == 16909384) ? false : true;
        boolean beforeN = this.mEntry.targetSdk < 24;
        if (customView && beforeN && !this.mIsSummaryWithChildren) {
            minHeight = this.mNotificationMinHeightLegacy;
        } else if (this.mUseIncreasedCollapsedHeight == 0 || layout != this.mPrivateLayout) {
            minHeight = this.mNotificationMinHeight;
        } else {
            minHeight = this.mNotificationMinHeightLarge;
        }
        if (!(layout.getHeadsUpChild() == null || layout.getHeadsUpChild().getId() == 16909384)) {
            headsUpCustom = true;
        }
        if (headsUpCustom && beforeN) {
            headsUpheight = this.mMaxHeadsUpHeightLegacy;
        } else if (this.mUseIncreasedHeadsUpHeight == 0 || layout != this.mPrivateLayout) {
            headsUpheight = this.mMaxHeadsUpHeight;
        } else {
            headsUpheight = this.mMaxHeadsUpHeightIncreased;
        }
        layout.setHeights(minHeight, headsUpheight, this.mNotificationMaxHeight, this.mNotificationAmbientHeight);
    }

    public ExpandedNotification getStatusBarNotification() {
        return this.mStatusBarNotification;
    }

    public NotificationData.Entry getEntry() {
        return this.mEntry;
    }

    public boolean isHeadsUp() {
        return this.mIsHeadsUp;
    }

    public void setHeadsUp(boolean isHeadsUp) {
        int intrinsicBefore = getIntrinsicHeight();
        this.mIsHeadsUp = isHeadsUp;
        this.mPrivateLayout.setHeadsUp(isHeadsUp);
        if (this.mIsSummaryWithChildren) {
            this.mChildrenContainer.updateGroupOverflow();
        }
        if (intrinsicBefore != getIntrinsicHeight()) {
            notifyHeightChanged(false);
        }
        if (isHeadsUp) {
            setAboveShelf(true);
            resetTranslation();
        }
        updateOutline();
    }

    public void setGroupManager(NotificationGroupManager groupManager) {
        this.mGroupManager = groupManager;
        this.mPrivateLayout.setGroupManager(groupManager);
    }

    public void setRemoteInputController(RemoteInputController r) {
        this.mPrivateLayout.setRemoteInputController(r);
    }

    public void setAppName(String appName) {
        this.mAppName = appName;
        if (this.mMenuRow != null && this.mMenuRow.getMenuView() != null) {
            this.mMenuRow.setAppName(this.mAppName);
        }
    }

    public String getAppName() {
        return this.mAppName;
    }

    public void addChildNotification(ExpandableNotificationRow row, int childIndex) {
        if (this.mChildrenContainer == null) {
            this.mChildrenContainerStub.inflate();
        }
        this.mChildrenContainer.addNotification(row, childIndex);
        onChildrenCountChanged();
        row.setIsChildInGroup(true, this);
        this.mChildrenContainer.rebuildCollapseButton();
    }

    public void removeChildNotification(ExpandableNotificationRow row) {
        if (this.mChildrenContainer != null) {
            this.mChildrenContainer.removeNotification(row);
        }
        onChildrenCountChanged();
        row.setIsChildInGroup(false, null);
    }

    public boolean isChildInGroup() {
        return this.mNotificationParent != null;
    }

    public ExpandableNotificationRow getNotificationParent() {
        return this.mNotificationParent;
    }

    public void setIsChildInGroup(boolean isChildInGroup, ExpandableNotificationRow parent) {
        boolean childInGroup = StatusBar.ENABLE_CHILD_NOTIFICATIONS && isChildInGroup;
        this.mNotificationParent = childInGroup ? parent : null;
        this.mPrivateLayout.setIsChildInGroup(childInGroup);
        this.mNotificationInflater.setIsChildInGroup(childInGroup);
        resetBackgroundAlpha();
        updateBackgroundForGroupState();
        updateClickAndFocus();
        if (this.mNotificationParent != null) {
            setOverrideTintColor(0, 0.0f);
            this.mNotificationParent.updateBackgroundForGroupState();
        }
        updateIconVisibilities();
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() != 0 || !isChildInGroup() || isGroupExpanded()) {
            return super.onTouchEvent(event);
        }
        return false;
    }

    /* access modifiers changed from: protected */
    public boolean handleSlideBack() {
        if (this.mMenuRow == null || !this.mMenuRow.isMenuVisible()) {
            return false;
        }
        animateTranslateNotification(0.0f);
        return true;
    }

    /* access modifiers changed from: protected */
    public boolean shouldHideBackground() {
        return super.shouldHideBackground() || this.mShowNoBackground;
    }

    public boolean isSummaryWithChildren() {
        return this.mIsSummaryWithChildren;
    }

    public boolean areChildrenExpanded() {
        return this.mChildrenExpanded;
    }

    public List<ExpandableNotificationRow> getNotificationChildren() {
        if (this.mChildrenContainer == null) {
            return null;
        }
        return this.mChildrenContainer.getNotificationChildren();
    }

    public boolean applyChildOrder(List<ExpandableNotificationRow> childOrder, VisualStabilityManager visualStabilityManager, VisualStabilityManager.Callback callback) {
        return this.mChildrenContainer != null && this.mChildrenContainer.applyChildOrder(childOrder, visualStabilityManager, callback);
    }

    public void getChildrenStates(StackScrollState resultState) {
        if (this.mIsSummaryWithChildren) {
            this.mChildrenContainer.getState(resultState, resultState.getViewStateForView(this));
        }
    }

    public void applyChildrenState(StackScrollState state) {
        if (this.mIsSummaryWithChildren) {
            this.mChildrenContainer.applyState(state);
        }
    }

    public void prepareExpansionChanged(StackScrollState state) {
        if (this.mIsSummaryWithChildren) {
            this.mChildrenContainer.prepareExpansionChanged(state);
        }
    }

    public void startChildAnimation(StackScrollState finalState, AnimationProperties properties) {
        if (this.mIsSummaryWithChildren) {
            this.mChildrenContainer.startAnimationToState(finalState, properties);
        }
    }

    public ExpandableNotificationRow getViewAtPosition(float y) {
        if (!this.mIsSummaryWithChildren || !this.mChildrenExpanded) {
            return this;
        }
        ExpandableNotificationRow view = this.mChildrenContainer.getViewAtPosition(y);
        return view == null ? this : view;
    }

    public NotificationGuts getGuts() {
        return this.mGuts;
    }

    public void setPinned(boolean pinned) {
        int intrinsicHeight = getIntrinsicHeight();
        this.mIsPinned = pinned;
        if (intrinsicHeight != getIntrinsicHeight()) {
            notifyHeightChanged(false);
        }
        if (pinned) {
            setIconAnimationRunning(true);
            this.mExpandedWhenPinned = false;
        } else if (this.mExpandedWhenPinned) {
            setUserExpanded(true);
        }
        setChronometerRunning(this.mLastChronometerRunning);
    }

    public boolean isPinned() {
        return this.mIsPinned;
    }

    public int getPinnedHeadsUpHeight() {
        return getPinnedHeadsUpHeight(true);
    }

    private int getPinnedHeadsUpHeight(boolean atLeastMinHeight) {
        if (this.mIsSummaryWithChildren) {
            return this.mChildrenContainer.getIntrinsicHeight();
        }
        if (this.mExpandedWhenPinned) {
            return Math.max(getMaxExpandHeight(), this.mHeadsUpHeight);
        }
        if (atLeastMinHeight) {
            return Math.max(getCollapsedHeight(), this.mHeadsUpHeight);
        }
        return this.mHeadsUpHeight;
    }

    public void setJustClicked(boolean justClicked) {
        this.mJustClicked = justClicked;
    }

    public boolean wasJustClicked() {
        return this.mJustClicked;
    }

    public void setChronometerRunning(boolean running) {
        this.mLastChronometerRunning = running;
        setChronometerRunning(running, this.mPrivateLayout);
        setChronometerRunning(running, this.mPublicLayout);
        if (this.mChildrenContainer != null) {
            List<ExpandableNotificationRow> notificationChildren = this.mChildrenContainer.getNotificationChildren();
            for (int i = 0; i < notificationChildren.size(); i++) {
                notificationChildren.get(i).setChronometerRunning(running);
            }
        }
    }

    private void setChronometerRunning(boolean running, NotificationContentView layout) {
        if (layout != null) {
            boolean running2 = running || isPinned();
            View contractedChild = layout.getContractedChild();
            View expandedChild = layout.getExpandedChild();
            View headsUpChild = layout.getHeadsUpChild();
            setChronometerRunningForChild(running2, contractedChild);
            setChronometerRunningForChild(running2, expandedChild);
            setChronometerRunningForChild(running2, headsUpChild);
        }
    }

    private void setChronometerRunningForChild(boolean running, View child) {
        if (child != null) {
            View chronometer = child.findViewById(16908802);
            if (chronometer instanceof Chronometer) {
                ((Chronometer) chronometer).setStarted(running);
            }
        }
    }

    public NotificationHeaderView getNotificationHeader() {
        if (this.mIsSummaryWithChildren) {
            return this.mChildrenContainer.getHeaderView();
        }
        return this.mPrivateLayout.getNotificationHeader();
    }

    public NotificationHeaderView getVisibleNotificationHeader() {
        if (!this.mIsSummaryWithChildren || this.mShowingPublic) {
            return getShowingLayout().getVisibleNotificationHeader();
        }
        return this.mChildrenContainer.getVisibleHeader();
    }

    public void setOnExpandClickListener(OnExpandClickListener onExpandClickListener) {
        this.mOnExpandClickListener = onExpandClickListener;
    }

    public void setOnClickListener(View.OnClickListener l) {
        super.setOnClickListener(l);
        this.mOnClickListener = l;
        updateClickAndFocus();
    }

    private void updateClickAndFocus() {
        boolean clickable = true;
        boolean normalChild = !isChildInGroup() || isGroupExpanded();
        if (this.mOnClickListener == null || !normalChild) {
            clickable = false;
        }
        if (isFocusable() != normalChild) {
            setFocusable(normalChild);
        }
        if (isClickable() != clickable) {
            setClickable(clickable);
        }
    }

    public void setHeadsUpManager(HeadsUpManager headsUpManager) {
        this.mHeadsUpManager = headsUpManager;
    }

    public void setGutsView(NotificationMenuRowPlugin.MenuItem item) {
        if (this.mGuts != null && (item.getGutsView() instanceof NotificationGuts.GutsContent)) {
            ((NotificationGuts.GutsContent) item.getGutsView()).setGutsParent(this.mGuts);
            this.mGuts.setGutsContent((NotificationGuts.GutsContent) item.getGutsView());
        }
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        ((PluginManager) Dependency.get(PluginManager.class)).addPluginListener(this, (Class<?>) NotificationMenuRowPlugin.class, false);
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ((PluginManager) Dependency.get(PluginManager.class)).removePluginListener(this);
    }

    public void onPluginConnected(NotificationMenuRowPlugin plugin, Context pluginContext) {
        boolean existed = this.mMenuRow.getMenuView() != null;
        if (existed) {
            removeView(this.mMenuRow.getMenuView());
        }
        this.mMenuRow = plugin;
        if (this.mMenuRow.useDefaultMenuItems()) {
            ArrayList<NotificationMenuRowPlugin.MenuItem> items = new ArrayList<>();
            items.add(NotificationMenuRow.createInfoItem(this.mContext));
            if (Constants.IS_INTERNATIONAL) {
                items.add(NotificationMenuRow.createSnoozeItem(this.mContext));
            } else {
                items.add(NotificationMenuRow.createFilterItem(this.mContext, false));
            }
            this.mMenuRow.setMenuItems(items);
        }
        if (existed) {
            createMenu();
        }
    }

    public void onPluginDisconnected(NotificationMenuRowPlugin plugin) {
        boolean existed = this.mMenuRow.getMenuView() != null;
        this.mMenuRow = new NotificationMenuRow(this.mContext);
        if (existed) {
            createMenu();
        }
    }

    public NotificationMenuRowPlugin createMenu() {
        if (this.mMenuRow.getMenuView() == null) {
            this.mMenuRow.createMenu(this);
            this.mMenuRow.setAppName(this.mAppName);
            addView(this.mMenuRow.getMenuView(), 0, new FrameLayout.LayoutParams(-1, -1));
        }
        return this.mMenuRow;
    }

    public NotificationMenuRowPlugin getProvider() {
        return this.mMenuRow;
    }

    public void onDensityOrFontScaleChanged() {
        initDimens();
        if (this.mChildrenContainer != null) {
            this.mChildrenContainer.reInflateViews(this.mExpandClickListener, this.mEntry.notification);
        }
        if (this.mGuts != null) {
            View oldGuts = this.mGuts;
            int index = indexOfChild(oldGuts);
            removeView(oldGuts);
            this.mGuts = (NotificationGuts) LayoutInflater.from(this.mContext).inflate(R.layout.notification_guts, this, false);
            if (!Constants.IS_INTERNATIONAL) {
                updateLargeIconVisibility(oldGuts.getVisibility() == 0);
            }
            this.mGuts.setVisibility(oldGuts.getVisibility());
            addView(this.mGuts, index);
        }
        View oldMenu = this.mMenuRow.getMenuView();
        if (oldMenu != null) {
            int menuIndex = indexOfChild(oldMenu);
            removeView(oldMenu);
            this.mMenuRow.createMenu(this);
            this.mMenuRow.setAppName(this.mAppName);
            addView(this.mMenuRow.getMenuView(), menuIndex);
        }
        for (NotificationContentView l : this.mLayouts) {
            l.onDensityOrFontScaleChanged();
            l.reInflateViews();
        }
        if (this.mBackgroundNormal != null) {
            this.mBackgroundNormal.updateResource();
        }
        this.mNotificationInflater.onDensityOrFontScaleChanged();
        onNotificationUpdated();
        handleNotificationStyleChanged();
    }

    private void handleNotificationStyleChanged() {
        if (this.mChildrenContainer != null) {
            addLargeIconIfNeeded();
            updateLargeIconVisibility(NotificationUtil.showMiuiStyle());
        }
    }

    public void onConfigurationChanged(Configuration newConfig) {
        if (this.mMenuRow.getMenuView() != null) {
            this.mMenuRow.onConfigurationChanged();
        }
    }

    public void setContentBackground(int customBackgroundColor, boolean animate, NotificationContentView notificationContentView) {
        if (getShowingLayout() == notificationContentView) {
            setTintColor(customBackgroundColor, animate);
        }
    }

    public void closeRemoteInput() {
        for (NotificationContentView l : this.mLayouts) {
            l.closeRemoteInput();
        }
    }

    public void setSingleLineWidthIndention(int indention) {
        this.mPrivateLayout.setSingleLineWidthIndention(indention);
    }

    public HybridNotificationView getSingleLineView() {
        return this.mPrivateLayout.getSingleLineView();
    }

    public HybridNotificationView getAmbientSingleLineView() {
        return getShowingLayout().getAmbientSingleLineChild();
    }

    public boolean isOnKeyguard() {
        return this.mOnKeyguard;
    }

    public void setDismissed(boolean dismissed, boolean fromAccessibility) {
        this.mDismissed = dismissed;
        this.mGroupParentWhenDismissed = this.mNotificationParent;
        this.mRefocusOnDismiss = fromAccessibility;
        this.mChildAfterViewWhenDismissed = null;
        if (isChildInGroup()) {
            List<ExpandableNotificationRow> notificationChildren = this.mNotificationParent.getNotificationChildren();
            int i = notificationChildren.indexOf(this);
            if (i != -1 && i < notificationChildren.size() - 1) {
                this.mChildAfterViewWhenDismissed = notificationChildren.get(i + 1);
            }
        }
    }

    public boolean isDismissed() {
        return this.mDismissed;
    }

    public boolean keepInParent() {
        return this.mKeepInParent;
    }

    public void setKeepInParent(boolean keepInParent) {
        this.mKeepInParent = keepInParent;
    }

    public boolean isRemoved() {
        return this.mRemoved;
    }

    public void setRemoved() {
        this.mRemoved = true;
        this.mTranslationWhenRemoved = getTranslationY();
        this.mWasChildInGroupWhenRemoved = isChildInGroup();
        if (isChildInGroup()) {
            this.mTranslationWhenRemoved += getNotificationParent().getTranslationY();
        }
        this.mPrivateLayout.setRemoved();
    }

    public boolean wasChildInGroupWhenRemoved() {
        return this.mWasChildInGroupWhenRemoved;
    }

    public float getTranslationWhenRemoved() {
        return this.mTranslationWhenRemoved;
    }

    public NotificationChildrenContainer getChildrenContainer() {
        return this.mChildrenContainer;
    }

    public void setHeadsUpAnimatingAway(boolean headsUpAnimatingAway) {
        this.mHeadsupDisappearRunning = headsUpAnimatingAway;
        this.mPrivateLayout.setHeadsUpAnimatingAway(headsUpAnimatingAway);
        updateOutline();
    }

    public boolean isHeadsUpAnimatingAway() {
        return this.mHeadsupDisappearRunning;
    }

    public View getChildAfterViewWhenDismissed() {
        return this.mChildAfterViewWhenDismissed;
    }

    public View getGroupParentWhenDismissed() {
        return this.mGroupParentWhenDismissed;
    }

    public void performDismiss() {
        if (this.mOnDismissRunnable != null) {
            this.mOnDismissRunnable.run();
        }
    }

    public void setOnDismissRunnable(Runnable onDismissRunnable) {
        this.mOnDismissRunnable = onDismissRunnable;
    }

    public View getNotificationIcon() {
        NotificationHeaderView notificationHeader = getVisibleNotificationHeader();
        if (notificationHeader != null) {
            return getHeaderIcon(notificationHeader);
        }
        return null;
    }

    public boolean isShowingIcon() {
        boolean z = false;
        if (areGutsExposed()) {
            return false;
        }
        if (getVisibleNotificationHeader() != null) {
            z = true;
        }
        return z;
    }

    public void setContentTransformationAmount(float contentTransformationAmount, boolean isLastChild) {
        boolean z = false;
        boolean changeTransformation = isLastChild != this.mIsLastChild;
        if (this.mContentTransformationAmount != contentTransformationAmount) {
            z = true;
        }
        boolean changeTransformation2 = changeTransformation | z;
        this.mIsLastChild = isLastChild;
        this.mContentTransformationAmount = contentTransformationAmount;
        if (changeTransformation2) {
            updateContentTransformation();
        }
    }

    public void setIconsVisible(boolean iconsVisible) {
        if (iconsVisible != this.mIconsVisible) {
            this.mIconsVisible = iconsVisible;
            updateIconVisibilities();
        }
    }

    /* access modifiers changed from: protected */
    public void onBelowSpeedBumpChanged() {
        updateIconVisibilities();
    }

    private void updateContentTransformation() {
        float translationY = (-this.mContentTransformationAmount) * ((float) this.mIconTransformContentShift);
        float contentAlpha = 1.0f;
        if (this.mIsLastChild) {
            contentAlpha = Interpolators.ALPHA_OUT.getInterpolation(Math.min((1.0f - this.mContentTransformationAmount) / 0.5f, 1.0f));
            translationY *= 0.4f;
        }
        float contentAlpha2 = contentAlpha;
        for (NotificationContentView l : this.mLayouts) {
            l.setAlpha(contentAlpha2);
            l.setTranslationY(translationY);
        }
        if (this.mChildrenContainer != null) {
            this.mChildrenContainer.setAlpha(contentAlpha2);
            this.mChildrenContainer.setTranslationY(translationY);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:11:0x0023  */
    /* JADX WARNING: Removed duplicated region for block: B:14:? A[RETURN, SYNTHETIC] */
    /* JADX WARNING: Removed duplicated region for block: B:8:0x0017 A[LOOP:0: B:7:0x0015->B:8:0x0017, LOOP_END] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void updateIconVisibilities() {
        /*
            r5 = this;
            boolean r0 = r5.isChildInGroup()
            r1 = 0
            if (r0 != 0) goto L_0x0011
            r5.isBelowSpeedBump()
            boolean r0 = r5.mIconsVisible
            if (r0 == 0) goto L_0x000f
            goto L_0x0011
        L_0x000f:
            r0 = r1
            goto L_0x0012
        L_0x0011:
            r0 = 1
        L_0x0012:
            com.android.systemui.statusbar.NotificationContentView[] r2 = r5.mLayouts
            int r3 = r2.length
        L_0x0015:
            if (r1 >= r3) goto L_0x001f
            r4 = r2[r1]
            r4.setIconsVisible(r0)
            int r1 = r1 + 1
            goto L_0x0015
        L_0x001f:
            com.android.systemui.statusbar.stack.NotificationChildrenContainer r1 = r5.mChildrenContainer
            if (r1 == 0) goto L_0x0028
            com.android.systemui.statusbar.stack.NotificationChildrenContainer r1 = r5.mChildrenContainer
            r1.setIconsVisible(r0)
        L_0x0028:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.ExpandableNotificationRow.updateIconVisibilities():void");
    }

    /* JADX WARNING: type inference failed for: r1v3, types: [android.view.ViewParent] */
    /* JADX WARNING: Multi-variable type inference failed */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int getRelativeTopPadding(android.view.View r3) {
        /*
            r2 = this;
            r0 = 0
        L_0x0001:
            android.view.ViewParent r1 = r3.getParent()
            boolean r1 = r1 instanceof android.view.ViewGroup
            if (r1 == 0) goto L_0x001a
            int r1 = r3.getTop()
            int r0 = r0 + r1
            android.view.ViewParent r1 = r3.getParent()
            r3 = r1
            android.view.View r3 = (android.view.View) r3
            boolean r1 = r3 instanceof com.android.systemui.statusbar.ExpandableNotificationRow
            if (r1 == 0) goto L_0x0001
            return r0
        L_0x001a:
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.ExpandableNotificationRow.getRelativeTopPadding(android.view.View):int");
    }

    public float getContentTranslation() {
        return this.mPrivateLayout.getTranslationY();
    }

    public void setIsLowPriority(boolean isLowPriority) {
        this.mIsLowPriority = isLowPriority;
        this.mPrivateLayout.setIsLowPriority(isLowPriority);
        this.mNotificationInflater.setIsLowPriority(this.mIsLowPriority);
        if (this.mChildrenContainer != null) {
            this.mChildrenContainer.setIsLowPriority(isLowPriority);
        }
    }

    public void setLowPriorityStateUpdated(boolean lowPriorityStateUpdated) {
        this.mLowPriorityStateUpdated = lowPriorityStateUpdated;
    }

    public boolean hasLowPriorityStateUpdated() {
        return this.mLowPriorityStateUpdated;
    }

    public boolean isLowPriority() {
        return this.mIsLowPriority;
    }

    public void setUseIncreasedCollapsedHeight(boolean use) {
        this.mUseIncreasedCollapsedHeight = use;
        this.mNotificationInflater.setUsesIncreasedHeight(use);
    }

    public void setUseIncreasedHeadsUpHeight(boolean use) {
        this.mUseIncreasedHeadsUpHeight = use;
        this.mNotificationInflater.setUsesIncreasedHeadsUpHeight(use);
    }

    public void setRemoteViewClickHandler(RemoteViews.OnClickHandler remoteViewClickHandler) {
        this.mNotificationInflater.setRemoteViewClickHandler(remoteViewClickHandler);
    }

    public void setInflationCallback(NotificationInflater.InflationCallback callback) {
        this.mNotificationInflater.setInflationCallback(callback);
    }

    public void setInCallCallback(InCallNotificationView.InCallCallback inCallCallback) {
        this.mNotificationInflater.setInCallCallback(inCallCallback);
    }

    public void setNeedsRedaction(boolean needsRedaction) {
        this.mNotificationInflater.setRedactAmbient(needsRedaction);
    }

    @VisibleForTesting
    public NotificationInflater getNotificationInflater() {
        return this.mNotificationInflater;
    }

    public ExpandableNotificationRow(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mFalsingManager = FalsingManager.getInstance(context);
        this.mNotificationInflater = new NotificationInflater(this);
        this.mMenuRow = new NotificationMenuRow(this.mContext);
        initDimens();
    }

    private void initDimens() {
        this.mNotificationMinHeightLegacy = getFontScaledHeight(R.dimen.notification_min_height_legacy);
        this.mNotificationMinHeight = getFontScaledHeight(R.dimen.notification_min_height);
        this.mNotificationMinHeightLarge = getFontScaledHeight(R.dimen.notification_min_height_increased);
        this.mNotificationMaxHeight = getFontScaledHeight(R.dimen.notification_max_height);
        this.mNotificationAmbientHeight = getFontScaledHeight(R.dimen.notification_ambient_height);
        this.mMaxHeadsUpHeightLegacy = getFontScaledHeight(R.dimen.notification_max_heads_up_height_legacy);
        this.mMaxHeadsUpHeight = getFontScaledHeight(R.dimen.notification_max_heads_up_height);
        this.mMaxHeadsUpHeightIncreased = getFontScaledHeight(R.dimen.notification_max_heads_up_height_increased);
        this.mIncreasedPaddingBetweenElements = getResources().getDimensionPixelSize(R.dimen.notification_divider_height_increased);
        this.mIconTransformContentShiftNoIcon = getResources().getDimensionPixelSize(R.dimen.notification_icon_transform_content_shift);
        this.mNotificationHeadsUpBgRadius = getResources().getDimensionPixelSize(R.dimen.notification_heads_up_bg_radius);
    }

    private int getFontScaledHeight(int dimenId) {
        return (int) (((float) getResources().getDimensionPixelSize(dimenId)) * Math.max(1.0f, getResources().getDisplayMetrics().scaledDensity / getResources().getDisplayMetrics().density));
    }

    public void reset() {
        this.mShowingPublicInitialized = false;
        onHeightReset();
        requestLayout();
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mBackgroundNormal.setContainingRow(this);
        this.mBackgroundDimmed.setContainingRow(this);
        this.mPublicLayout = (NotificationContentView) findViewById(R.id.expandedPublic);
        this.mPrivateLayout = (NotificationContentView) findViewById(R.id.expanded);
        int i = 0;
        this.mLayouts = new NotificationContentView[]{this.mPrivateLayout, this.mPublicLayout};
        for (NotificationContentView l : this.mLayouts) {
            l.setExpandClickListener(this.mExpandClickListener);
            l.setContainingNotification(this);
        }
        this.mGutsStub = (ViewStub) findViewById(R.id.notification_guts_stub);
        this.mGutsStub.setOnInflateListener(new ViewStub.OnInflateListener() {
            public void onInflate(ViewStub stub, View inflated) {
                NotificationGuts unused = ExpandableNotificationRow.this.mGuts = (NotificationGuts) inflated;
                ExpandableNotificationRow.this.mGuts.setClipTopAmount(ExpandableNotificationRow.this.getClipTopAmount());
                ExpandableNotificationRow.this.mGuts.setActualHeight(ExpandableNotificationRow.this.getActualHeight());
                ViewStub unused2 = ExpandableNotificationRow.this.mGutsStub = null;
            }
        });
        this.mChildrenContainerStub = (ViewStub) findViewById(R.id.child_container_stub);
        this.mChildrenContainerStub.setOnInflateListener(new ViewStub.OnInflateListener() {
            public void onInflate(ViewStub stub, View inflated) {
                NotificationChildrenContainer unused = ExpandableNotificationRow.this.mChildrenContainer = (NotificationChildrenContainer) inflated;
                ExpandableNotificationRow.this.mChildrenContainer.setIsLowPriority(ExpandableNotificationRow.this.mIsLowPriority);
                ExpandableNotificationRow.this.mChildrenContainer.setContainingNotification(ExpandableNotificationRow.this);
                ExpandableNotificationRow.this.mChildrenContainer.onNotificationUpdated();
                ExpandableNotificationRow.this.mTranslateableViews.add(ExpandableNotificationRow.this.mChildrenContainer);
                ExpandableNotificationRow.this.addLargeIconIfNeeded();
            }
        });
        this.mTranslateableViews = new ArrayList<>();
        while (true) {
            int i2 = i;
            if (i2 < getChildCount()) {
                this.mTranslateableViews.add(getChildAt(i2));
                i = i2 + 1;
            } else {
                this.mTranslateableViews.remove(this.mChildrenContainerStub);
                this.mTranslateableViews.remove(this.mGutsStub);
                return;
            }
        }
    }

    public void resetTranslation() {
        if (this.mTranslateAnim != null) {
            this.mTranslateAnim.cancel();
        }
        if (this.mTranslateableViews != null) {
            for (int i = 0; i < this.mTranslateableViews.size(); i++) {
                this.mTranslateableViews.get(i).setTranslationX(0.0f);
            }
        }
        invalidateOutline();
        this.mMenuRow.resetMenu();
    }

    public void animateTranslateNotification(float leftTarget) {
        if (this.mTranslateAnim != null) {
            this.mTranslateAnim.cancel();
        }
        this.mTranslateAnim = getTranslateViewAnimator(leftTarget, null);
        if (this.mTranslateAnim != null) {
            this.mTranslateAnim.start();
        }
    }

    public void setTranslation(float translationX) {
        if (!areGutsExposed()) {
            for (int i = 0; i < this.mTranslateableViews.size(); i++) {
                if (this.mTranslateableViews.get(i) != null) {
                    this.mTranslateableViews.get(i).setTranslationX(translationX);
                }
            }
            invalidateOutline();
            if (this.mMenuRow.getMenuView() != null) {
                this.mMenuRow.onTranslationUpdate(translationX);
            }
            if (getParent() != null) {
                ((View) getParent()).invalidate();
            }
            if (isChildInGroup() && this.mBackgroundNormal != null) {
                this.mNotificationParent.getBackgroundNormal().invalidate();
            }
            updateClipping();
        }
    }

    public float getTranslation() {
        if (this.mTranslateableViews == null || this.mTranslateableViews.size() <= 0) {
            return 0.0f;
        }
        return this.mTranslateableViews.get(0).getTranslationX();
    }

    /* access modifiers changed from: protected */
    public int getExtraClipRightAmount() {
        if (this.mMenuRow == null || !this.mMenuRow.isMenuVisible()) {
            return 0;
        }
        return -((int) getTranslation());
    }

    public Animator getTranslateViewAnimator(final float leftTarget, ValueAnimator.AnimatorUpdateListener listener) {
        if (this.mTranslateAnim != null) {
            this.mTranslateAnim.cancel();
        }
        if (areGutsExposed()) {
            return null;
        }
        ObjectAnimator translateAnim = ObjectAnimator.ofFloat(this, TRANSLATE_CONTENT, new float[]{leftTarget});
        if (listener != null) {
            translateAnim.addUpdateListener(listener);
        }
        translateAnim.addListener(new AnimatorListenerAdapter() {
            boolean cancelled = false;

            public void onAnimationCancel(Animator anim) {
                this.cancelled = true;
            }

            public void onAnimationEnd(Animator anim) {
                if (!this.cancelled && leftTarget == 0.0f) {
                    ExpandableNotificationRow.this.mMenuRow.resetMenu();
                    Animator unused = ExpandableNotificationRow.this.mTranslateAnim = null;
                }
            }
        });
        this.mTranslateAnim = translateAnim;
        return translateAnim;
    }

    public void inflateGuts() {
        if (this.mGuts == null) {
            this.mGutsStub.inflate();
        }
    }

    private void updateChildrenVisibility() {
        int i = 4;
        this.mPrivateLayout.setVisibility((this.mShowingPublic || this.mIsSummaryWithChildren) ? 4 : 0);
        if (this.mChildrenContainer != null) {
            NotificationChildrenContainer notificationChildrenContainer = this.mChildrenContainer;
            if (!this.mShowingPublic && this.mIsSummaryWithChildren) {
                i = 0;
            }
            notificationChildrenContainer.setVisibility(i);
        }
        updateLimits();
    }

    public boolean onRequestSendAccessibilityEventInternal(View child, AccessibilityEvent event) {
        if (!super.onRequestSendAccessibilityEventInternal(child, event)) {
            return false;
        }
        AccessibilityEvent record = AccessibilityEvent.obtain();
        onInitializeAccessibilityEvent(record);
        dispatchPopulateAccessibilityEvent(record);
        event.appendRecord(record);
        return true;
    }

    public void setDark(boolean dark, boolean fade, long delay) {
        super.setDark(dark, fade, delay);
        if (!this.mIsHeadsUp) {
            fade = false;
        }
        NotificationContentView showing = getShowingLayout();
        if (showing != null) {
            showing.setDark(dark, fade, delay);
        }
        if (this.mIsSummaryWithChildren) {
            this.mChildrenContainer.setDark(dark, fade, delay);
        }
    }

    public boolean isExpandable() {
        if (!this.mIsSummaryWithChildren || this.mShowingPublic) {
            return this.mExpandable;
        }
        return !this.mChildrenExpanded;
    }

    public void setExpandable(boolean expandable) {
        this.mExpandable = expandable;
        this.mPrivateLayout.updateExpandButtons(isExpandable());
    }

    public void setClipToActualHeight(boolean clipToActualHeight) {
        boolean z = true;
        super.setClipToActualHeight(clipToActualHeight || isUserLocked());
        NotificationContentView showingLayout = getShowingLayout();
        if (!clipToActualHeight && !isUserLocked()) {
            z = false;
        }
        showingLayout.setClipToActualHeight(z);
    }

    public boolean hasUserChangedExpansion() {
        return this.mHasUserChangedExpansion;
    }

    public boolean isUserExpanded() {
        return this.mUserExpanded;
    }

    public void setUserExpanded(boolean userExpanded) {
        setUserExpanded(userExpanded, false);
    }

    public void setUserExpanded(boolean userExpanded, boolean allowChildExpansion) {
        this.mFalsingManager.setNotificationExpanded();
        if (this.mIsSummaryWithChildren && !this.mShowingPublic && allowChildExpansion && !this.mChildrenContainer.showingAsLowPriority()) {
            boolean wasExpanded = this.mGroupManager.isGroupExpanded(this.mStatusBarNotification);
            this.mGroupManager.setGroupExpanded((StatusBarNotification) this.mStatusBarNotification, userExpanded);
            onExpansionChanged(true, wasExpanded);
        } else if (!userExpanded || this.mExpandable) {
            boolean wasExpanded2 = isExpanded();
            this.mHasUserChangedExpansion = true;
            this.mUserExpanded = userExpanded;
            onExpansionChanged(true, wasExpanded2);
        }
    }

    public void resetUserExpansion() {
        boolean changed = this.mUserExpanded;
        if (!isMediaNotification()) {
            this.mUserExpanded = false;
            this.mHasUserChangedExpansion = false;
        } else if (!this.mHasUserChangedExpansion) {
            this.mUserExpanded = true;
        }
        if (changed && this.mIsSummaryWithChildren) {
            this.mChildrenContainer.onExpansionChanged();
        }
    }

    public boolean isUserLocked() {
        return this.mUserLocked && !this.mForceUnlocked;
    }

    public void setUserLocked(boolean userLocked) {
        this.mUserLocked = userLocked;
        this.mPrivateLayout.setUserExpanding(userLocked);
        if (this.mChildrenContainer != null) {
            this.mChildrenContainer.setUserLocked(userLocked);
            if (!this.mIsSummaryWithChildren) {
                return;
            }
            if (userLocked || !isGroupExpanded()) {
                updateBackgroundForGroupState();
            }
        }
    }

    public boolean isSystemExpanded() {
        return this.mIsSystemExpanded;
    }

    public void setSystemExpanded(boolean expand) {
        if (expand != this.mIsSystemExpanded) {
            boolean wasExpanded = isExpanded();
            this.mIsSystemExpanded = expand;
            notifyHeightChanged(false);
            onExpansionChanged(false, wasExpanded);
            if (this.mIsSummaryWithChildren) {
                this.mChildrenContainer.updateGroupOverflow();
            }
        }
    }

    public void setOnKeyguard(boolean onKeyguard) {
        if (onKeyguard != this.mOnKeyguard) {
            boolean wasExpanded = isExpanded();
            this.mOnKeyguard = onKeyguard;
            onExpansionChanged(false, wasExpanded);
            if (wasExpanded != isExpanded()) {
                if (this.mIsSummaryWithChildren) {
                    this.mChildrenContainer.updateGroupOverflow();
                }
                notifyHeightChanged(false);
            }
            updateOutline();
            updateBackground();
        }
    }

    public boolean isClearable() {
        if (this.mStatusBarNotification == null || !this.mStatusBarNotification.isClearable()) {
            return false;
        }
        if (this.mIsSummaryWithChildren) {
            List<ExpandableNotificationRow> notificationChildren = this.mChildrenContainer.getNotificationChildren();
            for (int i = 0; i < notificationChildren.size(); i++) {
                if (!notificationChildren.get(i).isClearable()) {
                    return false;
                }
            }
        }
        return true;
    }

    public int getIntrinsicHeight() {
        if (isUserLocked()) {
            return getActualHeight();
        }
        if (isShowingPublic() && this.mPublicLayout.getContractedChild() != null) {
            return this.mPublicLayout.getContractedChild().getHeight() + getExtraPadding();
        }
        if (this.mGuts != null && this.mGuts.isExposed()) {
            return this.mGuts.getIntrinsicHeight();
        }
        if (isChildInGroup() && !isGroupExpanded()) {
            return this.mPrivateLayout.getMinHeight();
        }
        if (this.mSensitive && this.mHideSensitiveForIntrinsicHeight) {
            return getMinHeight();
        }
        if (this.mIsSummaryWithChildren) {
            return Math.max(getShowingLayout().getMinHeight(), this.mChildrenContainer.getIntrinsicHeight()) + getExtraPadding();
        }
        if (!isHeadsUpAllowed() || (!this.mIsHeadsUp && !this.mHeadsupDisappearRunning)) {
            if (isExpanded()) {
                return getMaxExpandHeight() + getExtraPadding();
            }
            return getCollapsedHeight() + getExtraPadding();
        } else if (isPinned() || this.mHeadsupDisappearRunning) {
            return getPinnedHeadsUpHeight(true);
        } else {
            if (isExpanded()) {
                return Math.max(getMaxExpandHeight(), this.mHeadsUpHeight);
            }
            return Math.max(getCollapsedHeight(), this.mHeadsUpHeight);
        }
    }

    private boolean isHeadsUpAllowed() {
        return !this.mOnKeyguard && !this.mShowAmbient;
    }

    public boolean isGroupExpanded() {
        return this.mGroupManager != null && this.mGroupManager.isGroupExpanded(this.mStatusBarNotification);
    }

    private void onChildrenCountChanged() {
        this.mIsSummaryWithChildren = StatusBar.ENABLE_CHILD_NOTIFICATIONS && this.mChildrenContainer != null && this.mChildrenContainer.getNotificationChildCount() > 0;
        if (this.mIsSummaryWithChildren && this.mChildrenContainer.getHeaderView() == null) {
            this.mChildrenContainer.recreateNotificationHeader(this.mExpandClickListener);
        }
        getShowingLayout().updateBackgroundColor(false);
        this.mPrivateLayout.updateExpandButtons(isExpandable());
        updateChildrenHeaderAppearance();
        updateChildrenVisibility();
        if (this.mIsSummaryWithChildren && this.mStatusBarNotification != null) {
            this.mStatusBarNotification.setHasShownAfterUnlock(false);
            List<ExpandableNotificationRow> notificationChildren = this.mChildrenContainer.getNotificationChildren();
            for (int i = 0; i < notificationChildren.size(); i++) {
                ExpandedNotification notification = notificationChildren.get(i).getStatusBarNotification();
                if (notification != null) {
                    notification.setHasShownAfterUnlock(false);
                }
            }
        }
    }

    public void updateChildrenHeaderAppearance() {
        if (this.mIsSummaryWithChildren) {
            this.mChildrenContainer.updateChildrenHeaderAppearance();
        }
    }

    public boolean isExpanded() {
        return isExpanded(isMediaNotification());
    }

    public boolean isExpanded(boolean allowOnKeyguard) {
        return (!this.mOnKeyguard || allowOnKeyguard) && ((!hasUserChangedExpansion() && (isSystemExpanded() || isSystemChildExpanded())) || isUserExpanded()) && !isShowingPublic();
    }

    private boolean isSystemChildExpanded() {
        return false;
    }

    public void setSystemChildExpanded(boolean expanded) {
        this.mIsSystemChildExpanded = expanded;
    }

    public void setLayoutListener(LayoutListener listener) {
        this.mLayoutListener = listener;
    }

    public void removeListener() {
        this.mLayoutListener = null;
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        updateMaxHeights();
        if (this.mMenuRow.getMenuView() != null) {
            this.mMenuRow.onHeightUpdate();
        }
        updateContentShiftHeight();
        if (this.mLayoutListener != null) {
            this.mLayoutListener.onLayout();
        }
        this.mBackgroundNormal.setTop(0);
        if (this.mGuts != null) {
            this.mGuts.setTop(0);
        }
    }

    private void updateContentShiftHeight() {
        NotificationHeaderView notificationHeader = getVisibleNotificationHeader();
        if (notificationHeader != null) {
            View icon = getHeaderIcon(notificationHeader);
            this.mIconTransformContentShift = getRelativeTopPadding(icon) + icon.getHeight();
            return;
        }
        this.mIconTransformContentShift = this.mIconTransformContentShiftNoIcon;
    }

    private void updateMaxHeights() {
        int intrinsicBefore = getIntrinsicHeight();
        View expandedChild = this.mPrivateLayout.getExpandedChild();
        if (expandedChild == null) {
            expandedChild = this.mPrivateLayout.getContractedChild();
        }
        this.mMaxExpandHeight = expandedChild.getHeight();
        View headsUpChild = this.mPrivateLayout.getHeadsUpChild();
        if (headsUpChild == null) {
            headsUpChild = this.mPrivateLayout.getContractedChild();
        }
        this.mHeadsUpHeight = headsUpChild.getHeight();
        if (intrinsicBefore != getIntrinsicHeight()) {
            notifyHeightChanged(true);
        }
    }

    public void notifyHeightChanged(boolean needsAnimation) {
        super.notifyHeightChanged(needsAnimation);
        getShowingLayout().requestSelectLayout(needsAnimation || isUserLocked());
    }

    public void setSensitive(boolean sensitive, boolean hideSensitive) {
        this.mSensitive = sensitive;
        this.mSensitiveHiddenInGeneral = hideSensitive;
    }

    public void setHideSensitiveForIntrinsicHeight(boolean hideSensitive) {
        this.mHideSensitiveForIntrinsicHeight = hideSensitive;
        if (this.mIsSummaryWithChildren) {
            List<ExpandableNotificationRow> notificationChildren = this.mChildrenContainer.getNotificationChildren();
            for (int i = 0; i < notificationChildren.size(); i++) {
                notificationChildren.get(i).setHideSensitiveForIntrinsicHeight(hideSensitive);
            }
        }
    }

    public void setHideSensitive(boolean hideSensitive, boolean animated, long delay, long duration) {
        boolean oldShowingPublic = this.mShowingPublic;
        this.mShowingPublic = (this.mSensitive && hideSensitive && !isMediaNotification()) || this.mEntry.hideSensitiveByAppLock;
        if (this.mShowingPublic && !oldShowingPublic) {
            Log.d("ExpandableNotificationRow", "show public, hideSensitive=" + hideSensitive + ",mSensitive=" + this.mSensitive);
        }
        if ((!this.mShowingPublicInitialized || this.mShowingPublic != oldShowingPublic) && this.mPublicLayout.getChildCount() != 0) {
            if (!animated) {
                this.mPublicLayout.animate().cancel();
                this.mPrivateLayout.animate().cancel();
                if (this.mChildrenContainer != null) {
                    this.mChildrenContainer.animate().cancel();
                    this.mChildrenContainer.setAlpha(1.0f);
                }
                this.mPublicLayout.setAlpha(1.0f);
                this.mPrivateLayout.setAlpha(1.0f);
                this.mPublicLayout.setVisibility(this.mShowingPublic ? 0 : 4);
                updateChildrenVisibility();
            } else {
                animateShowingPublic(delay, duration);
            }
            NotificationContentView showingLayout = getShowingLayout();
            showingLayout.updateBackgroundColor(animated);
            this.mPrivateLayout.updateExpandButtons(isExpandable());
            showingLayout.setDark(isDark(), false, 0);
            if (this.mShowingPublic) {
                showingLayout.showPublic();
            }
            this.mShowingPublicInitialized = true;
        }
    }

    private void animateShowingPublic(long delay, long duration) {
        View[] privateViews = this.mIsSummaryWithChildren ? new View[]{this.mChildrenContainer} : new View[]{this.mPrivateLayout};
        View[] publicViews = {this.mPublicLayout};
        View[] hiddenChildren = this.mShowingPublic ? privateViews : publicViews;
        View[] shownChildren = this.mShowingPublic ? publicViews : privateViews;
        for (final View hiddenView : hiddenChildren) {
            hiddenView.setVisibility(0);
            hiddenView.animate().cancel();
            hiddenView.animate().alpha(0.0f).setStartDelay(delay).setDuration(duration).withEndAction(new Runnable() {
                public void run() {
                    hiddenView.setVisibility(4);
                }
            });
        }
        for (View showView : shownChildren) {
            showView.setVisibility(0);
            showView.setAlpha(0.0f);
            showView.animate().cancel();
            showView.animate().alpha(1.0f).setStartDelay(delay).setDuration(duration);
        }
    }

    public boolean isShowingPublic() {
        return this.mShowingPublic;
    }

    public boolean mustStayOnScreen() {
        return this.mIsHeadsUp;
    }

    public boolean canViewBeDismissed() {
        return isClearable() && (!this.mShowingPublic || !this.mSensitiveHiddenInGeneral);
    }

    public void makeActionsVisibile() {
        setUserExpanded(true, true);
        if (isChildInGroup()) {
            this.mGroupManager.setGroupExpanded((StatusBarNotification) this.mStatusBarNotification, true);
        }
        notifyHeightChanged(false);
    }

    public void setChildrenExpanded(boolean expanded, boolean animate) {
        this.mChildrenExpanded = expanded;
        if (isChildInGroup() && (isExpanded() || isUserLocked())) {
            setUserExpanded(false);
        }
        if (this.mChildrenContainer != null) {
            this.mChildrenContainer.setChildrenExpanded(expanded);
        }
        updateBackgroundForGroupState();
        updateClickAndFocus();
        ((SystemUIStat) Dependency.get(SystemUIStat.class)).onVisibilityChanged(this.mEntry, this.mEntry.isSeen);
    }

    public int getMaxExpandHeight() {
        return this.mMaxExpandHeight;
    }

    public boolean areGutsExposed() {
        return this.mGuts != null && this.mGuts.isExposed();
    }

    public boolean isContentExpandable() {
        if (!this.mIsSummaryWithChildren || this.mShowingPublic) {
            return getShowingLayout().isContentExpandable();
        }
        return true;
    }

    /* access modifiers changed from: protected */
    public View getContentView() {
        if (!this.mIsSummaryWithChildren || this.mShowingPublic) {
            return getShowingLayout();
        }
        return this.mChildrenContainer;
    }

    /* access modifiers changed from: protected */
    public void onAppearAnimationFinished(boolean wasAppearing) {
        super.onAppearAnimationFinished(wasAppearing);
        if (wasAppearing) {
            if (this.mChildrenContainer != null) {
                this.mChildrenContainer.setAlpha(1.0f);
                this.mChildrenContainer.setLayerType(0, null);
            }
            for (NotificationContentView l : this.mLayouts) {
                l.setAlpha(1.0f);
                l.setLayerType(0, null);
            }
        }
    }

    public int getExtraBottomPadding() {
        if (!this.mIsSummaryWithChildren || !isGroupExpanded()) {
            return 0;
        }
        return this.mIncreasedPaddingBetweenElements;
    }

    public void updateContentHeight() {
        int contentHeight = Math.max(getMinHeight(), getActualHeight());
        for (NotificationContentView l : this.mLayouts) {
            l.setContentHeight(contentHeight);
        }
    }

    public void setActualHeight(int height, boolean notifyListeners) {
        boolean changed = height != getActualHeight();
        super.setActualHeight(height, notifyListeners);
        if (changed && isRemoved()) {
            ViewGroup parent = (ViewGroup) getParent();
            if (parent != null) {
                parent.invalidate();
            }
        }
        if (this.mGuts == null || !this.mGuts.isExposed()) {
            int contentHeight = Math.max(getMinHeight(), height);
            for (NotificationContentView l : this.mLayouts) {
                l.setContentHeight(contentHeight);
            }
            if (this.mIsSummaryWithChildren) {
                this.mChildrenContainer.setActualHeight(height);
            }
            if (this.mGuts != null) {
                this.mGuts.setActualHeight(height);
            }
            if (this.mMenuRow.getMenuView() != null) {
                this.mMenuRow.onHeightUpdate();
            }
            return;
        }
        this.mGuts.setActualHeight(height);
    }

    public int getMaxContentHeight() {
        if (!this.mIsSummaryWithChildren || this.mShowingPublic) {
            return getShowingLayout().getMaxHeight() + getExtraPadding();
        }
        return this.mChildrenContainer.getMaxContentHeight() + getExtraPadding();
    }

    public int getMinHeight() {
        if (this.mGuts != null && this.mGuts.isExposed()) {
            return this.mGuts.getIntrinsicHeight();
        }
        if (isHeadsUpAllowed() && ((this.mIsHeadsUp || this.mHeadsupDisappearRunning) && this.mHeadsUpManager.isTrackingHeadsUp() && !this.mShowingPublic)) {
            return getPinnedHeadsUpHeight(false);
        }
        if (this.mIsSummaryWithChildren && !isGroupExpanded() && !this.mShowingPublic) {
            return this.mChildrenContainer.getMinHeight();
        }
        if (!isHeadsUpAllowed() || ((!this.mIsHeadsUp && !this.mHeadsupDisappearRunning) || this.mShowingPublic)) {
            return getShowingLayout().getMinHeight();
        }
        return this.mHeadsUpHeight;
    }

    public int getCollapsedHeight() {
        if (!this.mIsSummaryWithChildren || this.mShowingPublic) {
            return getMinHeight();
        }
        return this.mChildrenContainer.getCollapsedHeight();
    }

    public void setClipTopAmount(int clipTopAmount) {
        super.setClipTopAmount(clipTopAmount);
        for (NotificationContentView l : this.mLayouts) {
            l.setClipTopAmount(clipTopAmount);
        }
        if (this.mGuts != null) {
            this.mGuts.setClipTopAmount(clipTopAmount);
        }
    }

    public void setClipBottomAmount(int clipBottomAmount) {
        if (clipBottomAmount != this.mClipBottomAmount) {
            super.setClipBottomAmount(clipBottomAmount);
            for (NotificationContentView l : this.mLayouts) {
                l.setClipBottomAmount(clipBottomAmount);
            }
            if (this.mGuts != null) {
                this.mGuts.setClipBottomAmount(clipBottomAmount);
            }
        }
        if (this.mChildrenContainer != null) {
            this.mChildrenContainer.setClipBottomAmount(clipBottomAmount);
        }
    }

    public NotificationContentView getShowingLayout() {
        return this.mShowingPublic ? this.mPublicLayout : this.mPrivateLayout;
    }

    public void setLegacy(boolean legacy) {
        for (NotificationContentView l : this.mLayouts) {
            l.setLegacy(legacy);
        }
    }

    /* access modifiers changed from: protected */
    public void updateBackgroundTint() {
        super.updateBackgroundTint();
        updateBackgroundForGroupState();
        if (this.mIsSummaryWithChildren) {
            List<ExpandableNotificationRow> notificationChildren = this.mChildrenContainer.getNotificationChildren();
            for (int i = 0; i < notificationChildren.size(); i++) {
                notificationChildren.get(i).updateBackgroundForGroupState();
            }
        }
    }

    public void onFinishedExpansionChange() {
        this.mGroupExpansionChanging = false;
        updateBackgroundForGroupState();
    }

    public void updateBackgroundForGroupState() {
        boolean z = true;
        if (this.mIsSummaryWithChildren) {
            if (isGroupExpanded() || isGroupExpansionChanging() || isUserLocked()) {
                z = false;
            }
            this.mShowNoBackground = z;
            List<ExpandableNotificationRow> children = this.mChildrenContainer.getNotificationChildren();
            for (int i = 0; i < children.size(); i++) {
                children.get(i).updateBackgroundForGroupState();
            }
        } else if (isChildInGroup()) {
            this.mShowNoBackground = true;
        } else {
            this.mShowNoBackground = false;
        }
        updateOutline();
        updateBackground();
    }

    /* access modifiers changed from: protected */
    public void updateBackground() {
        super.updateBackground();
        this.mBackgroundDimmed.setVisibility(4);
        if (this.mIsShowHeadsUpBackground && !this.mOnKeyguard && !this.mGroupManager.isChildInGroupWithSummary(this.mStatusBarNotification)) {
            this.mBackgroundNormal.setVisibility(0);
            if (InCallUtils.isInCallNotification(this.mContext, getEntry().notification)) {
                this.mBackgroundNormal.setCustomBackground((int) R.drawable.notification_heads_up_bg);
            } else if (!StatusBar.sGameMode || NotificationUtil.isInCallUINotification(getEntry().notification)) {
                this.mBackgroundNormal.setCustomBackground((int) R.drawable.notification_heads_up_bg);
            } else {
                this.mBackgroundNormal.setCustomBackground((int) R.drawable.optimized_game_heads_up_notification_bg);
            }
        } else if (isMediaNotification()) {
            this.mBackgroundNormal.setVisibility(0);
            if (!isExpanded() || isMediaCustomNotification()) {
                this.mBackgroundNormal.setCustomBackground(R.drawable.notification_item_bg, this.mNeedDrawBgTopDivider, this.mNeedDrawBgBottomDivider);
            } else {
                this.mBackgroundNormal.setCustomBackground(R.drawable.notification_item_expanded_bg, this.mNeedDrawBgTopDivider, this.mNeedDrawBgBottomDivider);
            }
        } else if (isCustomViewNotification()) {
            this.mBackgroundNormal.setCustomBackground((int) R.drawable.notification_item_bg);
        } else if (isChildInGroup()) {
            if (isExpanded()) {
                this.mBackgroundNormal.setVisibility(0);
                this.mBackgroundNormal.setCustomBackground(R.drawable.notification_item_expanded_children_bg, this.mNeedDrawBgTopDivider, this.mNeedDrawBgBottomDivider);
            } else if (!getNotificationParent().isGroupExpanded()) {
                this.mBackgroundNormal.setVisibility(4);
            } else if (!isGroupExpansionChanging()) {
                this.mBackgroundNormal.setVisibility(0);
                this.mBackgroundNormal.setCustomBackground((int) R.drawable.notification_item_bg);
            }
        } else if (isSummaryWithChildren()) {
            if (isGroupExpanded()) {
                this.mBackgroundNormal.setVisibility(0);
                this.mBackgroundNormal.setCustomBackground(R.drawable.notification_item_expanded_bg, this.mNeedDrawBgTopDivider, this.mNeedDrawBgBottomDivider);
            } else {
                this.mBackgroundNormal.setVisibility(0);
                this.mBackgroundNormal.setCustomBackground((int) R.drawable.notification_item_bg);
            }
        } else if (!isExpanded() || !isContentExpandable() || isSystemExpanded() || this.mStatusBarNotification.isShowMiuiAction()) {
            this.mBackgroundNormal.setVisibility(0);
            this.mBackgroundNormal.setCustomBackground((int) R.drawable.notification_item_bg);
        } else {
            this.mBackgroundNormal.setVisibility(0);
            this.mBackgroundNormal.setCustomBackground(R.drawable.notification_item_expanded_bg, this.mNeedDrawBgTopDivider, this.mNeedDrawBgBottomDivider);
        }
        if (isOnKeyguard() && !isChildInGroup()) {
            this.mBackgroundNormal.setCustomBackground((int) R.drawable.notification_panel_bg_keyguard);
            this.mBackgroundNormal.setVisibility(0);
        }
    }

    public void showHeadsUpBackground() {
        this.mIsShowHeadsUpBackground = true;
        updateBackground();
    }

    public boolean isIsShowHeadsUpBackground() {
        return this.mIsShowHeadsUpBackground;
    }

    public void hideHeadsUpBackground() {
        this.mIsShowHeadsUpBackground = false;
        updateBackground();
        if (this.mChildrenContainer != null && this.mChildrenContainer.getNotificationChildCount() > 0) {
            for (ExpandableNotificationRow row : this.mChildrenContainer.getNotificationChildren()) {
                row.hideHeadsUpBackground();
            }
        }
    }

    public int getPositionOfChild(ExpandableNotificationRow childRow) {
        if (this.mIsSummaryWithChildren) {
            return this.mChildrenContainer.getPositionInLinearLayout(childRow);
        }
        return 0;
    }

    public void setExpansionLogger(ExpansionLogger logger, String key) {
        this.mLogger = logger;
        this.mLoggingKey = key;
    }

    public void onExpandedByGesture(boolean userExpanded) {
        int event = 409;
        if (this.mGroupManager.isSummaryOfGroup(getStatusBarNotification())) {
            event = 410;
        }
        MetricsLogger.action(this.mContext, event, userExpanded);
    }

    public float getIncreasedPaddingAmount() {
        return isOnKeyguard() ? 1.0f : 0.0f;
    }

    /* access modifiers changed from: protected */
    public boolean disallowSingleClick(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        NotificationHeaderView header = getVisibleNotificationHeader();
        if (header != null) {
            return header.isInTouchRect(x - getTranslation(), y);
        }
        return super.disallowSingleClick(event);
    }

    /* access modifiers changed from: private */
    public void onExpansionChanged(boolean userAction, boolean wasExpanded) {
        boolean nowExpanded = isExpanded();
        if (this.mIsSummaryWithChildren && (!this.mIsLowPriority || wasExpanded)) {
            nowExpanded = this.mGroupManager.isGroupExpanded(this.mStatusBarNotification);
        }
        if (this.mMenuRow != null) {
            this.mMenuRow.onExpansionChanged();
        }
        if (nowExpanded != wasExpanded) {
            if (this.mLogger != null && isContentExpandable()) {
                this.mLogger.logNotificationExpansion(this.mLoggingKey, userAction, nowExpanded);
            }
            if (this.mIsSummaryWithChildren) {
                this.mChildrenContainer.onExpansionChanged();
            }
            if (isLowPriority()) {
                NotificationHeaderView headerView = getVisibleNotificationHeader();
                if (headerView != null) {
                    headerView.setExpanded(nowExpanded);
                }
            }
            updateBackground();
        }
    }

    public void onInitializeAccessibilityNodeInfoInternal(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfoInternal(info);
        if (canViewBeDismissed()) {
            info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_DISMISS);
        }
        boolean expandable = this.mShowingPublic;
        boolean isExpanded = false;
        if (!expandable) {
            if (this.mIsSummaryWithChildren) {
                expandable = true;
                if (!this.mIsLowPriority || isExpanded()) {
                    isExpanded = isGroupExpanded();
                }
            } else {
                expandable = this.mPrivateLayout.isContentExpandable();
                isExpanded = isExpanded();
            }
        }
        if (!expandable) {
            return;
        }
        if (isExpanded) {
            info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_COLLAPSE);
        } else {
            info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_EXPAND);
        }
    }

    public boolean performAccessibilityActionInternal(int action, Bundle arguments) {
        if (super.performAccessibilityActionInternal(action, arguments)) {
            return true;
        }
        if (action == 262144 || action == 524288) {
            this.mExpandClickListener.onClick(this);
            return true;
        } else if (action != 1048576) {
            return false;
        } else {
            NotificationStackScrollLayout.performDismiss(this, this.mGroupManager, true);
            return true;
        }
    }

    public boolean shouldRefocusOnDismiss() {
        return this.mRefocusOnDismiss || isAccessibilityFocused();
    }

    public ExpandableViewState createNewViewState(StackScrollState stackScrollState) {
        return new NotificationViewState(stackScrollState);
    }

    public boolean isAboveShelf() {
        return !isOnKeyguard() && (this.mIsPinned || this.mHeadsupDisappearRunning || (this.mIsHeadsUp && this.mAboveShelf));
    }

    public void setShowAmbient(boolean showAmbient) {
        if (showAmbient != this.mShowAmbient) {
            this.mShowAmbient = showAmbient;
            if (this.mChildrenContainer != null) {
                this.mChildrenContainer.notifyShowAmbientChanged();
            }
            notifyHeightChanged(false);
        }
    }

    public boolean isShowingAmbient() {
        return this.mShowAmbient;
    }

    public void setAboveShelf(boolean aboveShelf) {
        this.mAboveShelf = aboveShelf;
    }

    /* access modifiers changed from: protected */
    @VisibleForTesting
    public void setChildrenContainer(NotificationChildrenContainer childrenContainer) {
        this.mChildrenContainer = childrenContainer;
    }

    public View.OnClickListener getExpandClickListener() {
        return this.mExpandClickListener;
    }

    /* access modifiers changed from: protected */
    public boolean needsOutline() {
        return isOnKeyguard() || ((isHeadsUp() || isHeadsUpAnimatingAway()) && !StatusBar.sGameMode);
    }

    /* access modifiers changed from: protected */
    public int getOutlineRadius() {
        if (isHeadsUp() || isHeadsUpAnimatingAway()) {
            return this.mNotificationHeadsUpBgRadius;
        }
        return super.getOutlineRadius();
    }

    /* access modifiers changed from: protected */
    public void updateOutlineAlpha() {
        if (isOnKeyguard()) {
            setOutlineAlpha(0.0f);
        } else {
            super.updateOutlineAlpha();
        }
    }

    public void setIsFirstRow(boolean isFirstRow) {
        if (this.mChildrenContainer != null) {
            for (ExpandableNotificationRow row : this.mChildrenContainer.getNotificationChildren()) {
                row.setIsFirstRow(false);
            }
        }
        this.mIsFirstRow = isFirstRow;
    }

    public boolean isFirstRow() {
        return this.mIsFirstRow;
    }

    public void setNeedDrawBgDivider(boolean needDrawBgTopDivider, boolean needDrawBgBottomDivider) {
        this.mNeedDrawBgTopDivider = needDrawBgTopDivider;
        this.mNeedDrawBgBottomDivider = needDrawBgBottomDivider;
        updateBackground();
    }

    public void setHasExtraTopPadding(boolean hasExtraTopPadding) {
        if (this.mChildrenContainer != null) {
            for (ExpandableNotificationRow row : this.mChildrenContainer.getNotificationChildren()) {
                row.setHasExtraTopPadding(false);
            }
        }
        this.mHasExtraTopPadding = hasExtraTopPadding;
    }

    public void setHasExtraBottomPadding(boolean hasExtraBottomPadding) {
        if (this.mChildrenContainer != null) {
            for (ExpandableNotificationRow row : this.mChildrenContainer.getNotificationChildren()) {
                row.setHasExtraBottomPadding(false);
            }
        }
        this.mHasExtraBottomPadding = hasExtraBottomPadding;
    }

    public int getExtraPadding() {
        return getPaddingTop() + getPaddingBottom();
    }

    public boolean hasExtraTopPadding() {
        return this.mHasExtraTopPadding;
    }

    public boolean hasExtraBottomPadding() {
        return this.mHasExtraBottomPadding;
    }

    public NotificationBackgroundView getBackgroundNormal() {
        return this.mBackgroundNormal;
    }

    public boolean isExpansionChanging() {
        return this.mPrivateLayout != null && this.mPrivateLayout.isExpansionChanging();
    }

    public boolean hasOverlappingRendering() {
        return false;
    }

    public boolean isMediaNotification() {
        if (getPrivateLayout() != null) {
            return getPrivateLayout().isMediaNotification(this.mStatusBarNotification);
        }
        return NotificationUtil.isMediaNotification(this.mStatusBarNotification);
    }

    public boolean isMediaCustomNotification() {
        return isMediaNotification() && NotificationUtil.isCustomViewNotification(this.mStatusBarNotification);
    }

    public boolean isCustomViewNotification() {
        if (getPrivateLayout() != null) {
            return getPrivateLayout().isCustomViewNotification(this.mStatusBarNotification);
        }
        return NotificationUtil.isCustomViewNotification(this.mStatusBarNotification);
    }

    private static View getHeaderIcon(NotificationHeaderView header) {
        return header.findViewById(16908294);
    }
}
