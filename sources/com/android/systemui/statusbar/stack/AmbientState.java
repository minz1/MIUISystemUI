package com.android.systemui.statusbar.stack;

import android.content.Context;
import android.view.View;
import com.android.systemui.R;
import com.android.systemui.statusbar.ActivatableNotificationView;
import com.android.systemui.statusbar.ExpandableView;
import com.android.systemui.statusbar.NotificationShelf;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import java.util.ArrayList;

public class AmbientState {
    private ActivatableNotificationView mActivatedChild;
    private int mBaseZHeight;
    private float mCurrentScrollVelocity;
    private boolean mDark;
    private boolean mDimmed;
    private boolean mDismissAllInProgress;
    private ArrayList<View> mDraggedViews = new ArrayList<>();
    private float mExpandingVelocity;
    private boolean mExpansionChanging;
    private boolean mHasPulsingNotifications;
    private HeadsUpManager mHeadsUpManager;
    private boolean mHideSensitive;
    private ExpandableView mLastVisibleBackgroundChild;
    private int mLayoutHeight;
    private int mLayoutMinHeight;
    private float mMaxHeadsUpTranslation;
    private int mMaxLayoutHeight;
    private float mOverScrollBottomAmount;
    private float mOverScrollTopAmount;
    private boolean mPanelFullWidth;
    private boolean mPanelTracking;
    private int mScrollY;
    private boolean mShadeExpanded;
    private NotificationShelf mShelf;
    private int mSpeedBumpIndex = -1;
    private float mStackTranslation;
    private int mStatusBarState;
    private int mTopPadding;
    private boolean mUnlockHintRunning;
    private int mZDistanceBetweenElements;

    public AmbientState(Context context) {
        reload(context);
    }

    public void reload(Context context) {
        this.mZDistanceBetweenElements = Math.max(1, context.getResources().getDimensionPixelSize(R.dimen.z_distance_between_notifications));
        this.mBaseZHeight = context.getResources().getDimensionPixelSize(R.dimen.notification_heads_up_z_translation);
    }

    public int getBaseZHeight() {
        return this.mBaseZHeight;
    }

    public int getZDistanceBetweenElements() {
        return this.mZDistanceBetweenElements;
    }

    public int getScrollY() {
        return this.mScrollY;
    }

    public void setScrollY(int scrollY) {
        this.mScrollY = scrollY;
    }

    public void onBeginDrag(View view) {
        this.mDraggedViews.add(view);
    }

    public void onDragFinished(View view) {
        this.mDraggedViews.remove(view);
    }

    public ArrayList<View> getDraggedViews() {
        return this.mDraggedViews;
    }

    public void setDimmed(boolean dimmed) {
        this.mDimmed = dimmed;
    }

    public void setDark(boolean dark) {
        this.mDark = dark;
    }

    public void setHideSensitive(boolean hideSensitive) {
        this.mHideSensitive = hideSensitive;
    }

    public void setActivatedChild(ActivatableNotificationView activatedChild) {
        this.mActivatedChild = activatedChild;
    }

    public boolean isDimmed() {
        return this.mDimmed;
    }

    public boolean isDark() {
        return this.mDark;
    }

    public boolean isHideSensitive() {
        return this.mHideSensitive;
    }

    public ActivatableNotificationView getActivatedChild() {
        return this.mActivatedChild;
    }

    public void setOverScrollAmount(float amount, boolean onTop) {
        if (onTop) {
            this.mOverScrollTopAmount = amount;
        } else {
            this.mOverScrollBottomAmount = amount;
        }
    }

    public float getOverScrollAmount(boolean top) {
        return top ? this.mOverScrollTopAmount : this.mOverScrollBottomAmount;
    }

    public int getSpeedBumpIndex() {
        return this.mSpeedBumpIndex;
    }

    public void setSpeedBumpIndex(int shelfIndex) {
        this.mSpeedBumpIndex = shelfIndex;
    }

    public void setHeadsUpManager(HeadsUpManager headsUpManager) {
        this.mHeadsUpManager = headsUpManager;
    }

    public float getStackTranslation() {
        return this.mStackTranslation;
    }

    public void setStackTranslation(float stackTranslation) {
        this.mStackTranslation = stackTranslation;
    }

    public void setLayoutHeight(int layoutHeight) {
        this.mLayoutHeight = layoutHeight;
    }

    public float getTopPadding() {
        return (float) this.mTopPadding;
    }

    public void setTopPadding(int topPadding) {
        this.mTopPadding = topPadding;
    }

    public int getInnerHeight() {
        return Math.max(Math.min(this.mLayoutHeight, this.mMaxLayoutHeight) - this.mTopPadding, this.mLayoutMinHeight);
    }

    public boolean isShadeExpanded() {
        return this.mShadeExpanded;
    }

    public void setShadeExpanded(boolean shadeExpanded) {
        this.mShadeExpanded = shadeExpanded;
    }

    public void setMaxHeadsUpTranslation(float maxHeadsUpTranslation) {
        this.mMaxHeadsUpTranslation = maxHeadsUpTranslation;
    }

    public float getMaxHeadsUpTranslation() {
        return this.mMaxHeadsUpTranslation;
    }

    public void setDismissAllInProgress(boolean dismissAllInProgress) {
        this.mDismissAllInProgress = dismissAllInProgress;
    }

    public void setLayoutMinHeight(int layoutMinHeight) {
        this.mLayoutMinHeight = layoutMinHeight;
    }

    public void setShelf(NotificationShelf shelf) {
        this.mShelf = shelf;
    }

    public NotificationShelf getShelf() {
        return this.mShelf;
    }

    public void setLayoutMaxHeight(int maxLayoutHeight) {
        this.mMaxLayoutHeight = maxLayoutHeight;
    }

    public int getMaxLayoutHeight() {
        return this.mMaxLayoutHeight;
    }

    public void setLastVisibleBackgroundChild(ExpandableView lastVisibleBackgroundChild) {
        this.mLastVisibleBackgroundChild = lastVisibleBackgroundChild;
    }

    public ExpandableView getLastVisibleBackgroundChild() {
        return this.mLastVisibleBackgroundChild;
    }

    public void setCurrentScrollVelocity(float currentScrollVelocity) {
        this.mCurrentScrollVelocity = currentScrollVelocity;
    }

    public float getCurrentScrollVelocity() {
        return this.mCurrentScrollVelocity;
    }

    public boolean isOnKeyguard() {
        return this.mStatusBarState == 1;
    }

    public void setStatusBarState(int statusBarState) {
        this.mStatusBarState = statusBarState;
    }

    public void setExpandingVelocity(float expandingVelocity) {
        this.mExpandingVelocity = expandingVelocity;
    }

    public void setExpansionChanging(boolean expansionChanging) {
        this.mExpansionChanging = expansionChanging;
    }

    public boolean isExpansionChanging() {
        return this.mExpansionChanging;
    }

    public float getExpandingVelocity() {
        return this.mExpandingVelocity;
    }

    public void setPanelTracking(boolean panelTracking) {
        this.mPanelTracking = panelTracking;
    }

    public boolean hasPulsingNotifications() {
        return this.mHasPulsingNotifications;
    }

    public boolean isPanelTracking() {
        return this.mPanelTracking;
    }

    public boolean isPanelFullWidth() {
        return this.mPanelFullWidth;
    }

    public void setPanelFullWidth(boolean panelFullWidth) {
        this.mPanelFullWidth = panelFullWidth;
    }

    public boolean isUnlockHintRunning() {
        return this.mUnlockHintRunning;
    }
}
