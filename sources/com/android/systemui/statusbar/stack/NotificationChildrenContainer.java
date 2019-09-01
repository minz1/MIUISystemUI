package com.android.systemui.statusbar.stack;

import android.app.Notification;
import android.app.NotificationCompat;
import android.content.Context;
import android.content.res.Configuration;
import android.service.notification.StatusBarNotification;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.NotificationHeaderView;
import android.view.NotificationHeaderViewCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;
import android.widget.TextView;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.R;
import com.android.systemui.miui.statusbar.notification.NotificationUtil;
import com.android.systemui.statusbar.CrossFadeHelper;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.NotificationHeaderUtil;
import com.android.systemui.statusbar.TransformableView;
import com.android.systemui.statusbar.notification.HybridGroupManager;
import com.android.systemui.statusbar.notification.HybridNotificationView;
import com.android.systemui.statusbar.notification.NotificationUtils;
import com.android.systemui.statusbar.notification.NotificationViewWrapper;
import com.android.systemui.statusbar.notification.NotificationViewWrapperCompat;
import com.android.systemui.statusbar.notification.VisualStabilityManager;
import java.util.ArrayList;
import java.util.List;

public class NotificationChildrenContainer extends ViewGroup {
    private static final AnimationProperties ALPHA_FADE_IN = new AnimationProperties() {
        private AnimationFilter mAnimationFilter = new AnimationFilter().animateAlpha();

        public AnimationFilter getAnimationFilter() {
            return this.mAnimationFilter;
        }
    }.setDuration(200);
    private int mActualHeight;
    private int mChildTopMargin;
    private final List<ExpandableNotificationRow> mChildren;
    private boolean mChildrenExpanded;
    private int mClipBottomAmount;
    private ViewState mCollapseButtonViewState;
    private float mCollapsedBottomMargin;
    private TextView mCollapsedButton;
    private int mCollapsedButtonPadding;
    private ExpandableNotificationRow mContainingNotification;
    private int mContentMarginStart;
    private ViewGroup mCurrentHeader;
    private int mDividerHeight;
    private int mExpandedBottomMargin;
    private ViewState mGroupOverFlowState;
    private View.OnClickListener mHeaderClickListener;
    private int mHeaderHeight;
    private NotificationHeaderUtil mHeaderUtil;
    private ViewState mHeaderViewState;
    private final HybridGroupManager mHybridGroupManager;
    private boolean mIsLowPriority;
    private int mMaxNotificationHeight;
    private boolean mNeverAppliedGroupState;
    private NotificationHeaderView mNotificationHeader;
    private ViewGroup mNotificationHeaderAmbient;
    private NotificationHeaderView mNotificationHeaderLowPriority;
    private int mNotificationHeaderMargin;
    private NotificationViewWrapper mNotificationHeaderWrapper;
    private NotificationViewWrapper mNotificationHeaderWrapperAmbient;
    private NotificationViewWrapper mNotificationHeaderWrapperLowPriority;
    private int mNotificationTopPadding;
    private TextView mOverflowNumber;
    private int mOverflowNumberBottomPadding;
    private int mOverflowNumberTopMargin;
    private int mOverflowNumberTopPadding;
    private int mRealHeight;
    private boolean mUserLocked;

    public NotificationChildrenContainer(Context context) {
        this(context, null);
    }

    public NotificationChildrenContainer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NotificationChildrenContainer(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public NotificationChildrenContainer(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mChildren = new ArrayList();
        initDimens();
        this.mHybridGroupManager = new HybridGroupManager(getContext(), this);
    }

    private void initDimens() {
        this.mChildTopMargin = getResources().getDimensionPixelSize(R.dimen.notification_children_top_margin);
        this.mDividerHeight = 0;
        this.mHeaderHeight = getResources().getDimensionPixelSize(R.dimen.notification_header_height);
        this.mMaxNotificationHeight = getResources().getDimensionPixelSize(R.dimen.notification_max_height);
        if (NotificationUtil.showGoogleStyle()) {
            this.mNotificationHeaderMargin = getResources().getDimensionPixelSize(R.dimen.notification_content_margin_top_for_international);
            this.mNotificationTopPadding = 0;
            this.mExpandedBottomMargin = 0;
        } else {
            this.mNotificationHeaderMargin = 0;
            this.mNotificationTopPadding = getResources().getDimensionPixelSize(17105222);
            this.mExpandedBottomMargin = getResources().getDimensionPixelSize(R.dimen.notification_group_expanded_bottom_margin);
        }
        this.mCollapsedBottomMargin = (float) getResources().getDimensionPixelSize(17105218);
        this.mCollapsedButtonPadding = getResources().getDimensionPixelSize(R.dimen.notification_collapsed_button_padding);
        this.mContentMarginStart = getResources().getDimensionPixelSize(R.dimen.notification_title_margin_start);
        this.mOverflowNumberTopPadding = getResources().getDimensionPixelSize(R.dimen.notification_group_overflow_padding_top);
        this.mOverflowNumberBottomPadding = getResources().getDimensionPixelSize(R.dimen.notification_group_overflow_padding_bottom);
        this.mOverflowNumberTopMargin = getResources().getDimensionPixelSize(R.dimen.notification_group_overflow_margin_top);
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        int childCount = Math.min(this.mChildren.size(), 8);
        for (int i = 0; i < childCount; i++) {
            View child = this.mChildren.get(i);
            child.layout(0, 0, child.getMeasuredWidth(), child.getMeasuredHeight());
        }
        if (this.mOverflowNumber != null) {
            int marginEnd = this.mHybridGroupManager.getOverflowNumberPadding();
            boolean isRtl = true;
            if (getLayoutDirection() != 1) {
                isRtl = false;
            }
            int left = isRtl ? marginEnd : (getWidth() - this.mOverflowNumber.getMeasuredWidth()) - marginEnd;
            this.mOverflowNumber.layout(left, 0, this.mOverflowNumber.getMeasuredWidth() + left, this.mOverflowNumber.getMeasuredHeight());
        }
        if (this.mNotificationHeader != null) {
            this.mNotificationHeader.layout(0, 0, this.mNotificationHeader.getMeasuredWidth(), this.mNotificationHeader.getMeasuredHeight());
        }
        if (this.mNotificationHeaderLowPriority != null) {
            this.mNotificationHeaderLowPriority.layout(0, 0, this.mNotificationHeaderLowPriority.getMeasuredWidth(), this.mNotificationHeaderLowPriority.getMeasuredHeight());
        }
        if (this.mNotificationHeaderAmbient != null) {
            this.mNotificationHeaderAmbient.layout(0, 0, this.mNotificationHeaderAmbient.getMeasuredWidth(), this.mNotificationHeaderAmbient.getMeasuredHeight());
        }
        if (this.mCollapsedButton != null) {
            this.mCollapsedButton.layout(this.mContentMarginStart - this.mCollapsedButtonPadding, 0, (this.mCollapsedButton.getMeasuredWidth() + this.mContentMarginStart) - this.mCollapsedButtonPadding, this.mCollapsedButton.getMeasuredHeight());
        }
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int i;
        int i2;
        int i3 = widthMeasureSpec;
        int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);
        boolean hasFixedHeight = heightMode == 1073741824;
        boolean isHeightLimited = heightMode == Integer.MIN_VALUE;
        int size = View.MeasureSpec.getSize(heightMeasureSpec);
        int newHeightSpec = heightMeasureSpec;
        if (hasFixedHeight || isHeightLimited) {
            newHeightSpec = View.MeasureSpec.makeMeasureSpec(size, Integer.MIN_VALUE);
        }
        int width = View.MeasureSpec.getSize(widthMeasureSpec);
        if (this.mOverflowNumber != null) {
            this.mOverflowNumber.measure(View.MeasureSpec.makeMeasureSpec(width, Integer.MIN_VALUE), newHeightSpec);
        }
        int height = this.mNotificationHeaderMargin;
        int childCount = Math.min(this.mChildren.size(), 8);
        int collapsedChildren = getMaxAllowedVisibleChildren(true);
        int overflowIndex = childCount > collapsedChildren ? collapsedChildren - 1 : -1;
        int height2 = height;
        int i4 = 0;
        while (i4 < childCount) {
            ExpandableNotificationRow child = this.mChildren.get(i4);
            int overflowIndex2 = overflowIndex;
            if (!(i4 == overflowIndex2) || this.mOverflowNumber == null) {
                i2 = 0;
            } else {
                i2 = this.mOverflowNumber.getMeasuredWidth() + this.mHybridGroupManager.getOverflowNumberPadding();
            }
            child.setSingleLineWidthIndention(i2);
            child.measure(i3, newHeightSpec);
            if (child.getVisibility() != 8) {
                height2 += child.getIntrinsicHeight();
            }
            i4++;
            overflowIndex = overflowIndex2;
        }
        if (heightMode != 0) {
            height2 = Math.min(height2, size);
        }
        int headerHeightSpec = View.MeasureSpec.makeMeasureSpec(this.mHeaderHeight, 1073741824);
        if (this.mNotificationHeader != null) {
            this.mNotificationHeader.measure(i3, headerHeightSpec);
        }
        if (this.mNotificationHeaderLowPriority != null) {
            i = 1073741824;
            this.mNotificationHeaderLowPriority.measure(i3, View.MeasureSpec.makeMeasureSpec(this.mHeaderHeight, 1073741824));
        } else {
            i = 1073741824;
        }
        if (this.mNotificationHeaderAmbient != null) {
            this.mNotificationHeaderAmbient.measure(i3, View.MeasureSpec.makeMeasureSpec(this.mHeaderHeight, i));
        }
        if (this.mCollapsedButton != null) {
            this.mCollapsedButton.measure(View.MeasureSpec.makeMeasureSpec(width, Integer.MIN_VALUE), newHeightSpec);
            height2 += this.mCollapsedButton.getMeasuredHeight();
        }
        this.mRealHeight = height2;
        setMeasuredDimension(width, height2);
    }

    public boolean hasOverlappingRendering() {
        return false;
    }

    public void addNotification(ExpandableNotificationRow row, int childIndex) {
        this.mChildren.add(childIndex < 0 ? this.mChildren.size() : childIndex, row);
        addView(row);
        row.setUserLocked(this.mUserLocked);
        updateGroupOverflow();
        row.setContentTransformationAmount(0.0f, false);
    }

    public void removeNotification(ExpandableNotificationRow row) {
        int indexOf = this.mChildren.indexOf(row);
        this.mChildren.remove(row);
        removeView(row);
        row.setSystemChildExpanded(false);
        row.setUserLocked(false);
        updateGroupOverflow();
        if (!row.isRemoved()) {
            this.mHeaderUtil.restoreNotificationHeader(row);
        }
    }

    public void rebuildCollapseButton() {
        if (this.mCollapsedButton == null) {
            this.mCollapsedButton = (TextView) ((LayoutInflater) getContext().getSystemService("layout_inflater")).inflate(R.layout.notification_group_collapsed_button, this, false);
            addView(this.mCollapsedButton);
            if (this.mContainingNotification != null) {
                this.mCollapsedButton.setOnClickListener(this.mContainingNotification.getExpandClickListener());
            }
        }
    }

    public int getNotificationChildCount() {
        return this.mChildren.size();
    }

    public void recreateNotificationHeader(View.OnClickListener listener) {
        this.mHeaderClickListener = listener;
        Notification.Builder builder = NotificationCompat.recoverBuilder(getContext(), this.mContainingNotification.getStatusBarNotification().getNotification());
        RemoteViews header = NotificationCompat.makeNotificationHeader(builder, false);
        if (this.mNotificationHeader == null) {
            this.mNotificationHeader = header.apply(getContext(), this);
            NotificationViewWrapperCompat.findExpandButtonView(this.mNotificationHeader).setVisibility(0);
            this.mNotificationHeader.setOnClickListener(this.mHeaderClickListener);
            this.mNotificationHeaderWrapper = NotificationViewWrapper.wrap(getContext(), this.mNotificationHeader, this.mContainingNotification);
            addView(this.mNotificationHeader, 0);
            invalidate();
        } else {
            header.reapply(getContext(), this.mNotificationHeader);
        }
        this.mNotificationHeaderWrapper.onContentUpdated(this.mContainingNotification);
        recreateLowPriorityHeader(builder);
        recreateAmbientHeader(builder);
        updateHeaderVisibility(false);
        updateChildrenHeaderAppearance();
    }

    private void recreateAmbientHeader(Notification.Builder builder) {
        StatusBarNotification notification = this.mContainingNotification.getStatusBarNotification();
        if (builder == null) {
            builder = NotificationCompat.recoverBuilder(getContext(), notification.getNotification());
        }
        RemoteViews header = NotificationCompat.makeNotificationHeader(builder, true);
        if (this.mNotificationHeaderAmbient == null) {
            this.mNotificationHeaderAmbient = (ViewGroup) header.apply(getContext(), this);
            this.mNotificationHeaderWrapperAmbient = NotificationViewWrapper.wrap(getContext(), this.mNotificationHeaderAmbient, this.mContainingNotification);
            this.mNotificationHeaderWrapperAmbient.onContentUpdated(this.mContainingNotification);
            addView(this.mNotificationHeaderAmbient, 0);
            invalidate();
        } else {
            header.reapply(getContext(), this.mNotificationHeaderAmbient);
        }
        resetHeaderVisibilityIfNeeded(this.mNotificationHeaderAmbient, calculateDesiredHeader());
        this.mNotificationHeaderWrapperAmbient.onContentUpdated(this.mContainingNotification);
    }

    private void recreateLowPriorityHeader(Notification.Builder builder) {
        StatusBarNotification notification = this.mContainingNotification.getStatusBarNotification();
        if (this.mIsLowPriority) {
            if (builder == null) {
                builder = NotificationCompat.recoverBuilder(getContext(), notification.getNotification());
            }
            RemoteViews header = NotificationCompat.makeLowPriorityContentView(builder, true);
            if (this.mNotificationHeaderLowPriority == null) {
                this.mNotificationHeaderLowPriority = header.apply(getContext(), this);
                NotificationViewWrapperCompat.findExpandButtonView(this.mNotificationHeaderLowPriority).setVisibility(0);
                this.mNotificationHeaderLowPriority.setOnClickListener(this.mHeaderClickListener);
                this.mNotificationHeaderWrapperLowPriority = NotificationViewWrapper.wrap(getContext(), this.mNotificationHeaderLowPriority, this.mContainingNotification);
                addView(this.mNotificationHeaderLowPriority, 0);
                invalidate();
            } else {
                header.reapply(getContext(), this.mNotificationHeaderLowPriority);
            }
            this.mNotificationHeaderWrapperLowPriority.onContentUpdated(this.mContainingNotification);
            resetHeaderVisibilityIfNeeded(this.mNotificationHeaderLowPriority, calculateDesiredHeader());
            return;
        }
        removeView(this.mNotificationHeaderLowPriority);
        this.mNotificationHeaderLowPriority = null;
        this.mNotificationHeaderWrapperLowPriority = null;
    }

    public void updateChildrenHeaderAppearance() {
        this.mHeaderUtil.updateChildrenHeaderAppearance();
    }

    public void updateGroupOverflow() {
        int childCount = this.mChildren.size();
        int maxAllowedVisibleChildren = getMaxAllowedVisibleChildren(true);
        if (childCount > maxAllowedVisibleChildren) {
            this.mOverflowNumber = this.mHybridGroupManager.bindOverflowNumber(this.mOverflowNumber, childCount - maxAllowedVisibleChildren);
            if (this.mContainingNotification != null) {
                this.mOverflowNumber.setOnClickListener(this.mContainingNotification.getExpandClickListener());
            }
            if (this.mGroupOverFlowState == null) {
                this.mGroupOverFlowState = new ViewState();
                this.mNeverAppliedGroupState = true;
            }
        } else if (this.mOverflowNumber != null) {
            removeView(this.mOverflowNumber);
            if (isShown()) {
                final View removedOverflowNumber = this.mOverflowNumber;
                addTransientView(removedOverflowNumber, getTransientViewCount());
                CrossFadeHelper.fadeOut(removedOverflowNumber, (Runnable) new Runnable() {
                    public void run() {
                        NotificationChildrenContainer.this.removeTransientView(removedOverflowNumber);
                    }
                });
            }
            this.mOverflowNumber = null;
            this.mGroupOverFlowState = null;
        }
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateGroupOverflow();
    }

    public List<ExpandableNotificationRow> getNotificationChildren() {
        return this.mChildren;
    }

    public boolean applyChildOrder(List<ExpandableNotificationRow> childOrder, VisualStabilityManager visualStabilityManager, VisualStabilityManager.Callback callback) {
        int i = 0;
        if (childOrder == null) {
            return false;
        }
        boolean result = false;
        while (i < this.mChildren.size() && i < childOrder.size()) {
            ExpandableNotificationRow child = this.mChildren.get(i);
            ExpandableNotificationRow desiredChild = childOrder.get(i);
            if (child != desiredChild) {
                if (visualStabilityManager.canReorderNotification(desiredChild)) {
                    this.mChildren.remove(desiredChild);
                    this.mChildren.add(i, desiredChild);
                    result = true;
                } else {
                    visualStabilityManager.addReorderingAllowedCallback(callback);
                }
            }
            i++;
        }
        updateExpansionStates();
        return result;
    }

    private void updateExpansionStates() {
        if (!this.mChildrenExpanded && !this.mUserLocked) {
            int size = this.mChildren.size();
            for (int i = 0; i < size; i++) {
                ExpandableNotificationRow child = this.mChildren.get(i);
                boolean z = true;
                if (i != 0 || size != 1) {
                    z = false;
                }
                child.setSystemChildExpanded(z);
            }
        }
    }

    public int getIntrinsicHeight() {
        return getIntrinsicHeight((float) getMaxAllowedVisibleChildren());
    }

    private int getIntrinsicHeight(float maxAllowedVisibleChildren) {
        int intrinsicHeight;
        int intrinsicHeight2;
        if (showingAsLowPriority()) {
            return this.mNotificationHeaderLowPriority.getHeight();
        }
        int intrinsicHeight3 = this.mNotificationHeaderMargin;
        int visibleChildren = 0;
        int childCount = this.mChildren.size();
        boolean firstChild = true;
        float expandFactor = 0.0f;
        if (this.mUserLocked) {
            expandFactor = getGroupExpandFraction();
        }
        boolean childrenExpanded = this.mChildrenExpanded || this.mContainingNotification.isShowingAmbient();
        if (this.mUserLocked) {
            intrinsicHeight3 = (int) (((float) intrinsicHeight3) + NotificationUtils.interpolate((float) this.mNotificationTopPadding, 0.0f, expandFactor));
        } else if (!childrenExpanded) {
            intrinsicHeight3 += this.mNotificationTopPadding;
        }
        int intrinsicHeight4 = intrinsicHeight3;
        for (int i = 0; i < childCount && ((float) visibleChildren) < maxAllowedVisibleChildren; i++) {
            if (firstChild) {
                if (this.mUserLocked) {
                    intrinsicHeight2 = (int) (((float) intrinsicHeight4) + NotificationUtils.interpolate(0.0f, (float) this.mDividerHeight, expandFactor));
                } else {
                    intrinsicHeight2 = intrinsicHeight4 + (childrenExpanded ? this.mDividerHeight : 0);
                }
                firstChild = false;
            } else if (this.mUserLocked) {
                intrinsicHeight2 = (int) (((float) intrinsicHeight4) + NotificationUtils.interpolate((float) this.mChildTopMargin, (float) this.mDividerHeight, expandFactor));
            } else {
                intrinsicHeight2 = intrinsicHeight4 + (childrenExpanded ? this.mDividerHeight : this.mChildTopMargin);
            }
            intrinsicHeight4 = intrinsicHeight2 + this.mChildren.get(i).getIntrinsicHeight();
            visibleChildren++;
        }
        if (this.mUserLocked != 0) {
            intrinsicHeight = (int) (((float) intrinsicHeight4) + NotificationUtils.interpolate(this.mCollapsedBottomMargin, (float) this.mExpandedBottomMargin, expandFactor));
        } else if (childrenExpanded) {
            intrinsicHeight = this.mExpandedBottomMargin + intrinsicHeight4;
        } else {
            intrinsicHeight = (int) (((float) intrinsicHeight4) + this.mCollapsedBottomMargin);
        }
        if (this.mUserLocked) {
            intrinsicHeight = (int) (((float) intrinsicHeight) + NotificationUtils.interpolate(0.0f, (float) getCollapsedButtonHeight(), expandFactor));
        } else if (childrenExpanded) {
            intrinsicHeight += getCollapsedButtonHeight();
        }
        return intrinsicHeight;
    }

    public void getState(StackScrollState resultState, ExpandableViewState parentState) {
        float f;
        int maxAllowedVisibleChildren;
        boolean firstChild;
        int yPosition;
        float f2;
        int lastVisibleIndex;
        int yPosition2;
        boolean firstChild2;
        StackScrollState stackScrollState = resultState;
        ExpandableViewState expandableViewState = parentState;
        int childCount = this.mChildren.size();
        int yPosition3 = this.mNotificationHeaderMargin;
        int firstOverflowChildYPosition = yPosition3;
        boolean firstChild3 = true;
        int maxAllowedVisibleChildren2 = getMaxAllowedVisibleChildren();
        int lastVisibleIndex2 = maxAllowedVisibleChildren2 - 1;
        int firstOverflowIndex = lastVisibleIndex2 + 1;
        float expandFactor = 0.0f;
        boolean expandingToExpandedGroup = this.mUserLocked && !showingAsLowPriority();
        if (this.mUserLocked) {
            expandFactor = getGroupExpandFraction();
            firstOverflowIndex = getMaxAllowedVisibleChildren(true);
        }
        boolean childrenExpandedAndNotAnimating = this.mChildrenExpanded && !this.mContainingNotification.isGroupExpansionChanging();
        int firstOverflowChildYPosition2 = firstOverflowChildYPosition;
        int yPosition4 = yPosition3;
        int i = 0;
        while (i < childCount) {
            ExpandableNotificationRow child = this.mChildren.get(i);
            if (!firstChild3) {
                if (expandingToExpandedGroup) {
                    firstChild2 = firstChild3;
                    maxAllowedVisibleChildren = maxAllowedVisibleChildren2;
                    yPosition = (int) (((float) yPosition4) + NotificationUtils.interpolate((float) this.mChildTopMargin, (float) this.mDividerHeight, expandFactor));
                } else {
                    firstChild2 = firstChild3;
                    maxAllowedVisibleChildren = maxAllowedVisibleChildren2;
                    yPosition = yPosition4 + (this.mChildrenExpanded ? this.mDividerHeight : this.mChildTopMargin);
                }
                firstChild = firstChild2;
            } else {
                maxAllowedVisibleChildren = maxAllowedVisibleChildren2;
                if (expandingToExpandedGroup) {
                    yPosition2 = (int) (((float) yPosition4) + NotificationUtils.interpolate((float) this.mNotificationTopPadding, (float) this.mDividerHeight, expandFactor));
                } else {
                    yPosition2 = yPosition4 + (this.mChildrenExpanded ? this.mDividerHeight : this.mNotificationTopPadding);
                }
                firstChild = false;
            }
            ExpandableViewState childState = stackScrollState.getViewStateForView(child);
            int intrinsicHeight = child.getIntrinsicHeight();
            childState.height = intrinsicHeight;
            boolean firstChild4 = firstChild;
            childState.yTranslation = (float) yPosition;
            childState.hidden = false;
            if (childrenExpandedAndNotAnimating) {
                f2 = this.mContainingNotification.getTranslationZ();
            } else {
                f2 = 0.0f;
            }
            childState.zTranslation = f2;
            childState.dimmed = expandableViewState.dimmed;
            childState.dark = expandableViewState.dark;
            childState.hideSensitive = expandableViewState.hideSensitive;
            childState.belowSpeedBump = expandableViewState.belowSpeedBump;
            childState.clipTopAmount = 0;
            childState.alpha = 0.0f;
            if (i < firstOverflowIndex) {
                childState.alpha = showingAsLowPriority() ? expandFactor : 1.0f;
                lastVisibleIndex = lastVisibleIndex2;
            } else if (expandFactor != 1.0f || i > lastVisibleIndex2) {
                lastVisibleIndex = lastVisibleIndex2;
                childState.hidden = true;
                childState.yTranslation = (float) getIntrinsicHeight();
            } else {
                lastVisibleIndex = lastVisibleIndex2;
                childState.alpha = (((float) this.mActualHeight) - childState.yTranslation) / ((float) childState.height);
                childState.alpha = Math.max(0.0f, Math.min(1.0f, childState.alpha));
            }
            childState.location = expandableViewState.location;
            childState.inShelf = expandableViewState.inShelf;
            yPosition4 = yPosition + intrinsicHeight;
            if (i < firstOverflowIndex) {
                firstOverflowChildYPosition2 = yPosition4;
            }
            i++;
            maxAllowedVisibleChildren2 = maxAllowedVisibleChildren;
            firstChild3 = firstChild4;
            lastVisibleIndex2 = lastVisibleIndex;
        }
        boolean z = firstChild3;
        int i2 = maxAllowedVisibleChildren2;
        int i3 = lastVisibleIndex2;
        if (this.mOverflowNumber != null) {
            ExpandableNotificationRow overflowView = this.mChildren.get(Math.min(getMaxAllowedVisibleChildren(true), childCount) - 1);
            this.mGroupOverFlowState.copyFrom(stackScrollState.getViewStateForView(overflowView));
            this.mGroupOverFlowState.paddingTop = this.mOverflowNumberTopPadding;
            this.mGroupOverFlowState.paddingBottom = this.mOverflowNumberBottomPadding;
            int overflowNumberHeight = this.mOverflowNumber.getMeasuredHeight();
            int alignViewHeight = overflowView.getSingleLineView().getMeasuredHeight();
            this.mGroupOverFlowState.yTranslation += (float) (((alignViewHeight - overflowNumberHeight) / 2) + this.mOverflowNumberTopMargin);
            if (this.mContainingNotification.isShowingAmbient() || !this.mChildrenExpanded) {
                HybridNotificationView alignView = null;
                if (this.mContainingNotification.isShowingAmbient()) {
                    alignView = overflowView.getAmbientSingleLineView();
                } else if (this.mUserLocked) {
                    alignView = overflowView.getSingleLineView();
                }
                if (alignView != null) {
                    View mirrorView = alignView.getTextView();
                    if (mirrorView.getVisibility() == 8) {
                        mirrorView = alignView.getTitleView();
                    }
                    if (mirrorView.getVisibility() == 8) {
                        mirrorView = alignView;
                    }
                    this.mGroupOverFlowState.yTranslation += NotificationUtils.getRelativeYOffset(mirrorView, overflowView);
                    this.mGroupOverFlowState.alpha = mirrorView.getAlpha();
                }
            } else {
                this.mGroupOverFlowState.yTranslation += (float) this.mNotificationHeaderMargin;
                this.mGroupOverFlowState.alpha = 0.0f;
            }
        }
        if (this.mNotificationHeader != null) {
            if (this.mHeaderViewState == null) {
                this.mHeaderViewState = new ViewState();
            }
            this.mHeaderViewState.initFrom(this.mNotificationHeader);
            ViewState viewState = this.mHeaderViewState;
            if (childrenExpandedAndNotAnimating) {
                f = this.mContainingNotification.getTranslationZ();
            } else {
                f = 0.0f;
            }
            viewState.zTranslation = f;
        }
        if (this.mCollapsedButton != null) {
            if (this.mCollapseButtonViewState == null) {
                this.mCollapseButtonViewState = new ViewState();
            }
            this.mCollapseButtonViewState.initFrom(this.mCollapsedButton);
            this.mCollapseButtonViewState.hidden = NotificationUtil.showGoogleStyle();
            this.mCollapseButtonViewState.yTranslation = (float) firstOverflowChildYPosition2;
            this.mCollapseButtonViewState.alpha = this.mChildrenExpanded ? 1.0f : 0.0f;
        }
    }

    private int getMaxAllowedVisibleChildren() {
        return getMaxAllowedVisibleChildren(false);
    }

    private int getMaxAllowedVisibleChildren(boolean likeCollapsed) {
        if (this.mContainingNotification.isShowingAmbient()) {
            return 3;
        }
        if (!likeCollapsed && (this.mChildrenExpanded || this.mContainingNotification.isUserLocked())) {
            return 8;
        }
        if (this.mIsLowPriority || (!this.mContainingNotification.isOnKeyguard() && (this.mContainingNotification.isExpanded() || this.mContainingNotification.isHeadsUp()))) {
            return 5;
        }
        return 3;
    }

    public void applyState(StackScrollState state) {
        int childCount = this.mChildren.size();
        new ViewState();
        for (int i = 0; i < childCount; i++) {
            ExpandableNotificationRow child = this.mChildren.get(i);
            state.getViewStateForView(child).applyToView(child);
            child.setFakeShadowIntensity(0.0f, 0.0f, 0, 0);
        }
        if (this.mGroupOverFlowState != null) {
            this.mGroupOverFlowState.applyToView(this.mOverflowNumber);
            this.mNeverAppliedGroupState = false;
        }
        if (this.mHeaderViewState != null) {
            this.mHeaderViewState.applyToView(this.mNotificationHeader);
        }
        if (this.mCollapseButtonViewState != null) {
            this.mCollapseButtonViewState.applyToView(this.mCollapsedButton);
        }
        updateChildrenClipping();
    }

    private void updateChildrenClipping() {
        int childCount = this.mChildren.size();
        int layoutEnd = this.mContainingNotification.getActualHeight() - this.mClipBottomAmount;
        for (int i = 0; i < childCount; i++) {
            ExpandableNotificationRow child = this.mChildren.get(i);
            if (child.getVisibility() != 8) {
                float childTop = child.getTranslationY();
                float childBottom = ((float) child.getActualHeight()) + childTop;
                boolean visible = true;
                int clipBottomAmount = 0;
                if (childTop > ((float) layoutEnd)) {
                    visible = false;
                } else if (childBottom > ((float) layoutEnd)) {
                    clipBottomAmount = (int) (childBottom - ((float) layoutEnd));
                }
                boolean z = true;
                boolean isVisible = child.getVisibility() == 0;
                if (i >= getMaxAllowedVisibleChildren()) {
                    z = false;
                }
                boolean visible2 = visible & z;
                if (visible2 != isVisible) {
                    child.setVisibility(visible2 ? 0 : 4);
                }
                child.setClipBottomAmount(clipBottomAmount);
            }
        }
    }

    public void prepareExpansionChanged(StackScrollState state) {
    }

    public void startAnimationToState(StackScrollState state, AnimationProperties properties) {
        int childCount = this.mChildren.size();
        new ViewState();
        float groupExpandFraction = getGroupExpandFraction();
        if ((!this.mUserLocked || showingAsLowPriority()) && !this.mContainingNotification.isGroupExpansionChanging()) {
        }
        for (int i = childCount - 1; i >= 0; i--) {
            ExpandableNotificationRow child = this.mChildren.get(i);
            state.getViewStateForView(child).animateTo(child, properties);
            child.setFakeShadowIntensity(0.0f, 0.0f, 0, 0);
        }
        if (this.mOverflowNumber != null) {
            if (this.mNeverAppliedGroupState) {
                float alpha = this.mGroupOverFlowState.alpha;
                this.mGroupOverFlowState.alpha = 0.0f;
                this.mGroupOverFlowState.applyToView(this.mOverflowNumber);
                this.mGroupOverFlowState.alpha = alpha;
                this.mNeverAppliedGroupState = false;
            }
            this.mGroupOverFlowState.animateTo(this.mOverflowNumber, properties);
        }
        if (this.mNotificationHeader != null) {
            this.mHeaderViewState.applyToView(this.mNotificationHeader);
        }
        if (this.mCollapsedButton != null) {
            this.mCollapseButtonViewState.animateTo(this.mCollapsedButton, properties);
        }
        updateChildrenClipping();
    }

    public ExpandableNotificationRow getViewAtPosition(float y) {
        int count = this.mChildren.size();
        for (int childIdx = 0; childIdx < count; childIdx++) {
            ExpandableNotificationRow slidingChild = this.mChildren.get(childIdx);
            float childTop = slidingChild.getTranslationY();
            float bottom = ((float) slidingChild.getActualHeight()) + childTop;
            if (y >= ((float) slidingChild.getClipTopAmount()) + childTop && y <= bottom) {
                return slidingChild;
            }
        }
        return null;
    }

    public void setChildrenExpanded(boolean childrenExpanded) {
        this.mChildrenExpanded = childrenExpanded;
        updateExpansionStates();
        if (this.mNotificationHeader != null) {
            this.mNotificationHeader.setExpanded(childrenExpanded);
        }
        int count = this.mChildren.size();
        for (int childIdx = 0; childIdx < count; childIdx++) {
            this.mChildren.get(childIdx).setChildrenExpanded(childrenExpanded, false);
        }
    }

    public void setContainingNotification(ExpandableNotificationRow parent) {
        this.mContainingNotification = parent;
        this.mHeaderUtil = new NotificationHeaderUtil(this.mContainingNotification);
        if (!(this.mCollapsedButton == null || this.mContainingNotification == null)) {
            this.mCollapsedButton.setOnClickListener(this.mContainingNotification.getExpandClickListener());
        }
        if (this.mOverflowNumber != null && this.mContainingNotification != null) {
            this.mOverflowNumber.setOnClickListener(this.mContainingNotification.getExpandClickListener());
        }
    }

    public NotificationHeaderView getHeaderView() {
        return this.mNotificationHeader;
    }

    public NotificationHeaderView getLowPriorityHeaderView() {
        return this.mNotificationHeaderLowPriority;
    }

    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return super.onInterceptTouchEvent(ev);
    }

    @VisibleForTesting
    public ViewGroup getCurrentHeaderView() {
        return this.mCurrentHeader;
    }

    public void notifyShowAmbientChanged() {
        updateHeaderVisibility(false);
    }

    /* access modifiers changed from: private */
    public void updateHeaderVisibility(boolean animate) {
        if (!NotificationUtil.showMiuiStyle()) {
            ViewGroup currentHeader = this.mCurrentHeader;
            ViewGroup desiredHeader = calculateDesiredHeader();
            if (currentHeader != desiredHeader) {
                if (desiredHeader == this.mNotificationHeaderAmbient || currentHeader == this.mNotificationHeaderAmbient) {
                    animate = false;
                }
                if (animate) {
                    if (desiredHeader == null || currentHeader == null) {
                        animate = false;
                    } else {
                        currentHeader.setVisibility(0);
                        desiredHeader.setVisibility(0);
                        NotificationViewWrapper visibleWrapper = getWrapperForView(desiredHeader);
                        NotificationViewWrapper hiddenWrapper = getWrapperForView(currentHeader);
                        visibleWrapper.transformFrom(hiddenWrapper);
                        hiddenWrapper.transformTo((TransformableView) visibleWrapper, (Runnable) new Runnable() {
                            public void run() {
                                NotificationChildrenContainer.this.updateHeaderVisibility(false);
                            }
                        });
                        startChildAlphaAnimations(desiredHeader == this.mNotificationHeader);
                    }
                }
                if (!animate) {
                    if (desiredHeader != null) {
                        getWrapperForView(desiredHeader).setVisible(true);
                        desiredHeader.setVisibility(0);
                    }
                    if (currentHeader != null) {
                        NotificationViewWrapper wrapper = getWrapperForView(currentHeader);
                        if (wrapper != null) {
                            wrapper.setVisible(false);
                        }
                        currentHeader.setVisibility(4);
                    }
                }
                resetHeaderVisibilityIfNeeded(this.mNotificationHeader, desiredHeader);
                resetHeaderVisibilityIfNeeded(this.mNotificationHeaderAmbient, desiredHeader);
                resetHeaderVisibilityIfNeeded(this.mNotificationHeaderLowPriority, desiredHeader);
                this.mCurrentHeader = desiredHeader;
            }
        }
    }

    private void resetHeaderVisibilityIfNeeded(View header, View desiredHeader) {
        if (header != null) {
            if (NotificationUtil.showMiuiStyle()) {
                getWrapperForView(header).setVisible(false);
                header.setVisibility(4);
                return;
            }
            if (!(header == this.mCurrentHeader || header == desiredHeader)) {
                getWrapperForView(header).setVisible(false);
                header.setVisibility(4);
            }
            if (header == desiredHeader && header.getVisibility() != 0) {
                getWrapperForView(header).setVisible(true);
                header.setVisibility(0);
            }
        }
    }

    private ViewGroup calculateDesiredHeader() {
        if (this.mContainingNotification.isShowingAmbient()) {
            return this.mNotificationHeaderAmbient;
        }
        if (showingAsLowPriority()) {
            return this.mNotificationHeaderLowPriority;
        }
        return this.mNotificationHeader;
    }

    private void startChildAlphaAnimations(boolean toVisible) {
        float target = toVisible ? 1.0f : 0.0f;
        float start = 1.0f - target;
        int childCount = this.mChildren.size();
        int i = 0;
        while (i < childCount && i < 5) {
            ExpandableNotificationRow child = this.mChildren.get(i);
            child.setAlpha(start);
            ViewState viewState = new ViewState();
            viewState.initFrom(child);
            viewState.alpha = target;
            ALPHA_FADE_IN.setDelay((long) (i * 50));
            viewState.animateTo(child, ALPHA_FADE_IN);
            i++;
        }
    }

    private void updateHeaderTransformation() {
        if (this.mUserLocked && showingAsLowPriority()) {
            float fraction = getGroupExpandFraction();
            this.mNotificationHeaderWrapper.transformFrom(this.mNotificationHeaderWrapperLowPriority, fraction);
            this.mNotificationHeader.setVisibility(0);
            this.mNotificationHeaderWrapperLowPriority.transformTo((TransformableView) this.mNotificationHeaderWrapper, fraction);
        }
    }

    private NotificationViewWrapper getWrapperForView(View visibleHeader) {
        if (visibleHeader == this.mNotificationHeader) {
            return this.mNotificationHeaderWrapper;
        }
        if (visibleHeader == this.mNotificationHeaderAmbient) {
            return this.mNotificationHeaderWrapperAmbient;
        }
        return this.mNotificationHeaderWrapperLowPriority;
    }

    public int getMaxContentHeight() {
        int i;
        if (showingAsLowPriority()) {
            return getMinHeight(5, true);
        }
        int maxContentHeight = this.mNotificationHeaderMargin + this.mNotificationTopPadding;
        int visibleChildren = 0;
        int childCount = this.mChildren.size();
        for (int i2 = 0; i2 < childCount && visibleChildren < 8; i2++) {
            ExpandableNotificationRow child = this.mChildren.get(i2);
            if (child.isExpanded(true)) {
                i = child.getMaxExpandHeight();
            } else {
                i = child.getShowingLayout().getMinHeight(true);
            }
            maxContentHeight = (int) (((float) maxContentHeight) + ((float) i));
            visibleChildren++;
        }
        if (visibleChildren > 0) {
            maxContentHeight += this.mDividerHeight * visibleChildren;
        }
        return maxContentHeight;
    }

    public void setActualHeight(int actualHeight) {
        float childHeight;
        if (this.mUserLocked) {
            this.mActualHeight = actualHeight;
            float fraction = getGroupExpandFraction();
            boolean showingLowPriority = showingAsLowPriority();
            updateHeaderTransformation();
            int maxAllowedVisibleChildren = getMaxAllowedVisibleChildren(true);
            int childCount = this.mChildren.size();
            for (int i = 0; i < childCount; i++) {
                ExpandableNotificationRow child = this.mChildren.get(i);
                if (showingLowPriority) {
                    childHeight = (float) child.getShowingLayout().getMinHeight(false);
                } else if (child.isExpanded(true)) {
                    childHeight = (float) child.getMaxExpandHeight();
                } else {
                    childHeight = (float) child.getShowingLayout().getMinHeight(true);
                }
                if (i < maxAllowedVisibleChildren) {
                    child.setActualHeight((int) NotificationUtils.interpolate((float) child.getShowingLayout().getMinHeight(false), childHeight, fraction), false);
                } else {
                    child.setActualHeight((int) childHeight, false);
                }
            }
        }
    }

    public float getGroupExpandFraction() {
        int visibleChildrenExpandedHeight;
        if (showingAsLowPriority()) {
            visibleChildrenExpandedHeight = getMaxContentHeight();
        } else {
            visibleChildrenExpandedHeight = getVisibleChildrenExpandHeight();
        }
        int minExpandHeight = getCollapsedHeight();
        return Math.max(0.0f, Math.min(1.0f, ((float) (this.mActualHeight - minExpandHeight)) / ((float) (visibleChildrenExpandedHeight - minExpandHeight))));
    }

    private int getVisibleChildrenExpandHeight() {
        int i;
        int intrinsicHeight = this.mNotificationHeaderMargin + this.mNotificationTopPadding + this.mDividerHeight;
        int visibleChildren = 0;
        int childCount = this.mChildren.size();
        int maxAllowedVisibleChildren = getMaxAllowedVisibleChildren(true);
        for (int i2 = 0; i2 < childCount && visibleChildren < maxAllowedVisibleChildren; i2++) {
            ExpandableNotificationRow child = this.mChildren.get(i2);
            if (child.isExpanded(true)) {
                i = child.getMaxExpandHeight();
            } else {
                i = child.getShowingLayout().getMinHeight(true);
            }
            intrinsicHeight = (int) (((float) intrinsicHeight) + ((float) i));
            visibleChildren++;
        }
        return intrinsicHeight;
    }

    public int getMinHeight() {
        if (this.mContainingNotification.isShowingAmbient()) {
        }
        return getMinHeight(3, false);
    }

    public int getCollapsedHeight() {
        return getMinHeight(getMaxAllowedVisibleChildren(true), false);
    }

    private int getMinHeight(int maxAllowedVisibleChildren, boolean likeHighPriority) {
        if (!likeHighPriority && showingAsLowPriority()) {
            return this.mNotificationHeaderLowPriority.getHeight();
        }
        int minExpandHeight = this.mNotificationHeaderMargin;
        int visibleChildren = 0;
        boolean firstChild = true;
        int childCount = this.mChildren.size();
        for (int i = 0; i < childCount && visibleChildren < maxAllowedVisibleChildren; i++) {
            if (!firstChild) {
                minExpandHeight += this.mChildTopMargin;
            } else {
                firstChild = false;
            }
            minExpandHeight += this.mChildren.get(i).getSingleLineView().getHeight();
            visibleChildren++;
        }
        return (int) (((float) minExpandHeight) + this.mCollapsedBottomMargin);
    }

    public boolean showingAsLowPriority() {
        return this.mIsLowPriority && !this.mContainingNotification.isExpanded();
    }

    public void setDark(boolean dark, boolean fade, long delay) {
        if (this.mOverflowNumber != null) {
            this.mHybridGroupManager.setOverflowNumberDark(this.mOverflowNumber, dark, fade, delay);
        }
        this.mNotificationHeaderWrapper.setDark(dark, fade, delay);
    }

    public void reInflateViews(View.OnClickListener listener, StatusBarNotification notification) {
        if (this.mNotificationHeader != null) {
            removeView(this.mNotificationHeader);
            this.mNotificationHeader = null;
        }
        if (this.mNotificationHeaderLowPriority != null) {
            removeView(this.mNotificationHeaderLowPriority);
            this.mNotificationHeaderLowPriority = null;
        }
        if (this.mNotificationHeaderAmbient != null) {
            removeView(this.mNotificationHeaderAmbient);
            this.mNotificationHeaderAmbient = null;
        }
        recreateNotificationHeader(listener);
        initDimens();
        removeView(this.mOverflowNumber);
        this.mOverflowNumber = null;
        this.mGroupOverFlowState = null;
        updateGroupOverflow();
    }

    public void setUserLocked(boolean userLocked) {
        this.mUserLocked = userLocked;
        if (!this.mUserLocked) {
            updateHeaderVisibility(false);
        }
        int childCount = this.mChildren.size();
        for (int i = 0; i < childCount; i++) {
            this.mChildren.get(i).setUserLocked(userLocked && !showingAsLowPriority());
        }
    }

    public void onNotificationUpdated() {
        this.mHybridGroupManager.setOverflowNumberColor(this.mOverflowNumber, getContext().getColor(R.color.notification_overflow_number_color), getContext().getColor(R.color.notification_overflow_number_color));
    }

    public int getPositionInLinearLayout(View childInGroup) {
        int position = this.mNotificationHeaderMargin + this.mNotificationTopPadding;
        for (int i = 0; i < this.mChildren.size(); i++) {
            ExpandableNotificationRow child = this.mChildren.get(i);
            boolean notGone = child.getVisibility() != 8;
            if (notGone) {
                position += this.mDividerHeight;
            }
            if (child == childInGroup) {
                return position;
            }
            if (notGone) {
                position += child.getIntrinsicHeight();
            }
        }
        return 0;
    }

    public void setIconsVisible(boolean iconsVisible) {
        if (this.mNotificationHeaderWrapper != null) {
            NotificationHeaderViewCompat.setIconForceHidden(this.mNotificationHeaderWrapper.getNotificationHeader(), !iconsVisible);
        }
        if (this.mNotificationHeaderWrapperLowPriority != null) {
            NotificationHeaderViewCompat.setIconForceHidden(this.mNotificationHeaderWrapperLowPriority.getNotificationHeader(), !iconsVisible);
        }
    }

    public void setClipBottomAmount(int clipBottomAmount) {
        this.mClipBottomAmount = clipBottomAmount;
        updateChildrenClipping();
    }

    public void setIsLowPriority(boolean isLowPriority) {
        this.mIsLowPriority = isLowPriority;
        if (this.mContainingNotification != null) {
            recreateLowPriorityHeader(null);
            updateHeaderVisibility(false);
        }
        if (this.mUserLocked) {
            setUserLocked(this.mUserLocked);
        }
    }

    public NotificationHeaderView getVisibleHeader() {
        NotificationHeaderView header = this.mNotificationHeader;
        if (showingAsLowPriority()) {
            return this.mNotificationHeaderLowPriority;
        }
        return header;
    }

    public void onExpansionChanged() {
        if (this.mIsLowPriority) {
            if (this.mUserLocked) {
                setUserLocked(this.mUserLocked);
            }
            updateHeaderVisibility(true);
        }
    }

    public int getCollapsedButtonHeight() {
        if (NotificationUtil.showGoogleStyle()) {
            return 0;
        }
        return this.mCollapsedButton.getMeasuredHeight();
    }

    public TextView getCollapsedButton() {
        return this.mCollapsedButton;
    }

    public int getExpandedBottomMargin() {
        return this.mExpandedBottomMargin;
    }

    @VisibleForTesting
    public boolean isUserLocked() {
        return this.mUserLocked;
    }
}
