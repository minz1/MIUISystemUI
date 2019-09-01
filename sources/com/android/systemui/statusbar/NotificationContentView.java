package com.android.systemui.statusbar;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.NotificationHeaderView;
import android.view.NotificationHeaderViewCompat;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbstractFrameLayout;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.R;
import com.android.systemui.miui.statusbar.ExpandedNotification;
import com.android.systemui.miui.statusbar.notification.NotificationUtil;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.notification.HybridGroupManager;
import com.android.systemui.statusbar.notification.HybridNotificationView;
import com.android.systemui.statusbar.notification.InCallNotificationView;
import com.android.systemui.statusbar.notification.NotificationCustomViewWrapper;
import com.android.systemui.statusbar.notification.NotificationMediaTemplateViewWrapper;
import com.android.systemui.statusbar.notification.NotificationMessagingTemplateViewWrapper;
import com.android.systemui.statusbar.notification.NotificationUtils;
import com.android.systemui.statusbar.notification.NotificationViewWrapper;
import com.android.systemui.statusbar.phone.NotificationGroupManager;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.policy.RemoteInputView;

public class NotificationContentView extends AbstractFrameLayout {
    private View mAmbientChild;
    private HybridNotificationView mAmbientSingleLineChild;
    private NotificationViewWrapper mAmbientWrapper;
    /* access modifiers changed from: private */
    public boolean mAnimate;
    /* access modifiers changed from: private */
    public int mAnimationStartVisibleType = -1;
    private boolean mBeforeN;
    private RemoteInputView mCachedExpandedRemoteInput;
    private RemoteInputView mCachedHeadsUpRemoteInput;
    private int mClipBottomAmount;
    private final Rect mClipBounds = new Rect();
    private boolean mClipToActualHeight = true;
    private int mClipTopAmount;
    /* access modifiers changed from: private */
    public ExpandableNotificationRow mContainingNotification;
    private int mContentHeight;
    private int mContentHeightAtAnimationStart = -1;
    private View mContractedChild;
    private NotificationViewWrapper mContractedWrapper;
    private boolean mDark;
    private final ViewTreeObserver.OnPreDrawListener mEnableAnimationPredrawListener = new ViewTreeObserver.OnPreDrawListener() {
        public boolean onPreDraw() {
            NotificationContentView.this.post(new Runnable() {
                public void run() {
                    boolean unused = NotificationContentView.this.mAnimate = true;
                }
            });
            NotificationContentView.this.getViewTreeObserver().removeOnPreDrawListener(this);
            return true;
        }
    };
    private View.OnClickListener mExpandClickListener;
    private boolean mExpandable;
    private View mExpandedChild;
    private RemoteInputView mExpandedRemoteInput;
    private Runnable mExpandedVisibleListener;
    private NotificationViewWrapper mExpandedWrapper;
    private boolean mFocusOnVisibilityChange;
    private boolean mForceSelectNextLayout = true;
    private NotificationGroupManager mGroupManager;
    private boolean mHeadsUpAnimatingAway;
    private View mHeadsUpChild;
    private int mHeadsUpHeight;
    private RemoteInputView mHeadsUpRemoteInput;
    private NotificationViewWrapper mHeadsUpWrapper;
    private HybridGroupManager mHybridGroupManager = new HybridGroupManager(getContext(), this);
    private boolean mIconsVisible;
    private boolean mIsChildInGroup;
    private boolean mIsContentExpandable;
    private boolean mIsHeadsUp;
    private boolean mIsLowPriority;
    private boolean mLegacy;
    private int mLowPriorityNotificationHeight;
    private int mMinContractedHeight;
    private int mNotificationAmbientHeight;
    private int mNotificationContentMarginEnd;
    private int mNotificationMaxHeight;
    private PendingIntent mPreviousExpandedRemoteInputIntent;
    private PendingIntent mPreviousHeadsUpRemoteInputIntent;
    private RemoteInputController mRemoteInputController;
    private HybridNotificationView mSingleLineView;
    private int mSingleLineWidthIndention;
    private int mSmallHeight;
    private ExpandedNotification mStatusBarNotification;
    private int mTransformationStartVisibleType;
    private boolean mUserExpanding;
    /* access modifiers changed from: private */
    public int mVisibleType = 0;

    public NotificationContentView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initDimens();
    }

    private void initDimens() {
        this.mMinContractedHeight = getResources().getDimensionPixelSize(R.dimen.min_notification_layout_height);
        this.mNotificationContentMarginEnd = getResources().getDimensionPixelSize(17105219);
        this.mLowPriorityNotificationHeight = getResources().getDimensionPixelSize(R.dimen.low_priority_notification_layout_height);
    }

    public void setHeights(int smallHeight, int headsUpMaxHeight, int maxHeight, int ambientHeight) {
        this.mSmallHeight = smallHeight;
        this.mHeadsUpHeight = headsUpMaxHeight;
        this.mNotificationMaxHeight = maxHeight;
        this.mNotificationAmbientHeight = ambientHeight;
    }

    public void onDensityOrFontScaleChanged() {
        initDimens();
    }

    private boolean isMessagingStyle(NotificationViewWrapper wrapper) {
        return wrapper instanceof NotificationMessagingTemplateViewWrapper;
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int i;
        int i2;
        int heightSpec;
        int spec;
        int i3 = widthMeasureSpec;
        int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);
        boolean isHeightLimited = true;
        boolean hasFixedHeight = heightMode == 1073741824;
        if (heightMode != Integer.MIN_VALUE) {
            isHeightLimited = false;
        }
        int maxSize = Integer.MAX_VALUE;
        int width = View.MeasureSpec.getSize(widthMeasureSpec);
        if ((hasFixedHeight || isHeightLimited) && !this.mIsHeadsUp) {
            maxSize = View.MeasureSpec.getSize(heightMeasureSpec);
        }
        int maxChildHeight = 0;
        if (this.mExpandedChild != null) {
            int size = isMessagingStyle(this.mExpandedWrapper) ? this.mNotificationMaxHeight : Math.min(maxSize, this.mNotificationMaxHeight);
            ViewGroup.LayoutParams layoutParams = this.mExpandedChild.getLayoutParams();
            boolean useExactly = false;
            if (layoutParams.height >= 0) {
                size = Math.min(maxSize, layoutParams.height);
                useExactly = true;
            }
            if (size == Integer.MAX_VALUE) {
                spec = View.MeasureSpec.makeMeasureSpec(0, 0);
            } else {
                spec = View.MeasureSpec.makeMeasureSpec(size, useExactly ? 1073741824 : Integer.MIN_VALUE);
            }
            this.mExpandedChild.measure(getChildWidthSpec(i3, (ViewGroup.MarginLayoutParams) layoutParams), spec);
            maxChildHeight = Math.max(0, this.mExpandedChild.getMeasuredHeight());
        }
        if (this.mContractedChild != null) {
            int size2 = isMessagingStyle(this.mContractedWrapper) ? this.mSmallHeight : Math.min(maxSize, this.mSmallHeight);
            ViewGroup.LayoutParams layoutParams2 = this.mContractedChild.getLayoutParams();
            boolean useExactly2 = false;
            if (layoutParams2.height >= 0) {
                size2 = Math.min(size2, layoutParams2.height);
                useExactly2 = true;
            }
            if (isMediaNotification(this.mStatusBarNotification)) {
                heightSpec = View.MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE, Integer.MIN_VALUE);
            } else if (shouldContractedBeFixedSize() != 0 || useExactly2) {
                heightSpec = View.MeasureSpec.makeMeasureSpec(size2, 1073741824);
            } else {
                heightSpec = View.MeasureSpec.makeMeasureSpec(size2, Integer.MIN_VALUE);
            }
            int widthSpec = getChildWidthSpec(i3, (ViewGroup.MarginLayoutParams) layoutParams2);
            this.mContractedChild.measure(widthSpec, heightSpec);
            int measuredHeight = this.mContractedChild.getMeasuredHeight();
            if (measuredHeight < this.mMinContractedHeight) {
                heightSpec = View.MeasureSpec.makeMeasureSpec(this.mIsLowPriority ? this.mLowPriorityNotificationHeight : this.mMinContractedHeight, 1073741824);
                this.mContractedChild.measure(widthSpec, heightSpec);
            }
            maxChildHeight = Math.max(maxChildHeight, measuredHeight);
            if (updateContractedHeaderWidth()) {
                this.mContractedChild.measure(widthSpec, heightSpec);
            }
            if (this.mExpandedChild != null && this.mContractedChild.getMeasuredHeight() > this.mExpandedChild.getMeasuredHeight()) {
                this.mExpandedChild.measure(widthSpec, View.MeasureSpec.makeMeasureSpec(this.mContractedChild.getMeasuredHeight(), 1073741824));
            }
        }
        if (this.mHeadsUpChild != null) {
            int size3 = Math.min(maxSize, this.mHeadsUpHeight);
            ViewGroup.LayoutParams layoutParams3 = this.mHeadsUpChild.getLayoutParams();
            boolean useExactly3 = false;
            if (layoutParams3.height >= 0) {
                size3 = Math.min(size3, layoutParams3.height);
                useExactly3 = true;
            }
            this.mHeadsUpChild.measure(getChildWidthSpec(i3, (ViewGroup.MarginLayoutParams) layoutParams3), View.MeasureSpec.makeMeasureSpec(size3, useExactly3 ? 1073741824 : Integer.MIN_VALUE));
            maxChildHeight = Math.max(maxChildHeight, this.mHeadsUpChild.getMeasuredHeight());
        }
        if (this.mSingleLineView != null) {
            int singleLineWidthSpec = i3;
            if (!(this.mSingleLineWidthIndention == 0 || View.MeasureSpec.getMode(widthMeasureSpec) == 0)) {
                singleLineWidthSpec = View.MeasureSpec.makeMeasureSpec((width - this.mSingleLineWidthIndention) + this.mSingleLineView.getPaddingEnd(), 1073741824);
            }
            i = Integer.MIN_VALUE;
            this.mSingleLineView.measure(singleLineWidthSpec, View.MeasureSpec.makeMeasureSpec(maxSize, Integer.MIN_VALUE));
            maxChildHeight = Math.max(maxChildHeight, this.mSingleLineView.getMeasuredHeight());
        } else {
            i = Integer.MIN_VALUE;
        }
        if (this.mAmbientChild != null) {
            int size4 = Math.min(maxSize, this.mNotificationAmbientHeight);
            ViewGroup.LayoutParams layoutParams4 = this.mAmbientChild.getLayoutParams();
            boolean useExactly4 = false;
            if (layoutParams4.height >= 0) {
                size4 = Math.min(size4, layoutParams4.height);
                useExactly4 = true;
            }
            this.mAmbientChild.measure(getChildWidthSpec(i3, (ViewGroup.MarginLayoutParams) layoutParams4), View.MeasureSpec.makeMeasureSpec(size4, useExactly4 ? 1073741824 : i));
            maxChildHeight = Math.max(maxChildHeight, this.mAmbientChild.getMeasuredHeight());
        }
        if (this.mAmbientSingleLineChild != null) {
            int size5 = Math.min(maxSize, this.mNotificationAmbientHeight);
            ViewGroup.LayoutParams layoutParams5 = this.mAmbientSingleLineChild.getLayoutParams();
            boolean useExactly5 = false;
            if (layoutParams5.height >= 0) {
                size5 = Math.min(size5, layoutParams5.height);
                useExactly5 = true;
            }
            int ambientSingleLineWidthSpec = i3;
            if (this.mSingleLineWidthIndention == 0 || View.MeasureSpec.getMode(widthMeasureSpec) == 0) {
                i2 = 1073741824;
            } else {
                i2 = 1073741824;
                ambientSingleLineWidthSpec = View.MeasureSpec.makeMeasureSpec((width - this.mSingleLineWidthIndention) + this.mAmbientSingleLineChild.getPaddingEnd(), 1073741824);
            }
            HybridNotificationView hybridNotificationView = this.mAmbientSingleLineChild;
            if (!useExactly5) {
                i2 = i;
            }
            hybridNotificationView.measure(ambientSingleLineWidthSpec, View.MeasureSpec.makeMeasureSpec(size5, i2));
            maxChildHeight = Math.max(maxChildHeight, this.mAmbientSingleLineChild.getMeasuredHeight());
        }
        setMeasuredDimension(width, Math.min(maxChildHeight, maxSize));
    }

    private int getChildWidthSpec(int parentWidthSpec, ViewGroup.MarginLayoutParams childLayoutParams) {
        if (childLayoutParams.width == -1) {
            return View.MeasureSpec.makeMeasureSpec(Math.max(0, (View.MeasureSpec.getSize(parentWidthSpec) - childLayoutParams.getMarginEnd()) - childLayoutParams.getMarginStart()), View.MeasureSpec.getMode(parentWidthSpec));
        }
        return parentWidthSpec;
    }

    private boolean updateContractedHeaderWidth() {
        int i;
        int i2;
        int i3;
        int i4;
        NotificationHeaderView contractedHeader = this.mContractedWrapper.getNotificationHeader();
        if (contractedHeader != null) {
            if (this.mExpandedChild == null || this.mExpandedWrapper.getNotificationHeader() == null || this.mExpandedWrapper.getNotificationHeader().getVisibility() != 0) {
                int paddingEnd = this.mNotificationContentMarginEnd;
                if (contractedHeader.getPaddingEnd() != paddingEnd) {
                    if (contractedHeader.isLayoutRtl()) {
                        i = paddingEnd;
                    } else {
                        i = contractedHeader.getPaddingLeft();
                    }
                    int paddingTop = contractedHeader.getPaddingTop();
                    if (contractedHeader.isLayoutRtl()) {
                        i2 = contractedHeader.getPaddingLeft();
                    } else {
                        i2 = paddingEnd;
                    }
                    contractedHeader.setPadding(i, paddingTop, i2, contractedHeader.getPaddingBottom());
                    contractedHeader.setShowWorkBadgeAtEnd(false);
                    return true;
                }
            } else {
                NotificationHeaderView expandedHeader = this.mExpandedWrapper.getNotificationHeader();
                int expandedSize = expandedHeader.getMeasuredWidth() - expandedHeader.getPaddingEnd();
                if (expandedSize != contractedHeader.getMeasuredWidth() - expandedHeader.getPaddingEnd()) {
                    int paddingEnd2 = contractedHeader.getMeasuredWidth() - expandedSize;
                    if (contractedHeader.isLayoutRtl()) {
                        i3 = paddingEnd2;
                    } else {
                        i3 = contractedHeader.getPaddingLeft();
                    }
                    int paddingTop2 = contractedHeader.getPaddingTop();
                    if (contractedHeader.isLayoutRtl()) {
                        i4 = contractedHeader.getPaddingLeft();
                    } else {
                        i4 = paddingEnd2;
                    }
                    contractedHeader.setPadding(i3, paddingTop2, i4, contractedHeader.getPaddingBottom());
                    contractedHeader.setShowWorkBadgeAtEnd(true);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean shouldContractedBeFixedSize() {
        return this.mBeforeN && (this.mContractedWrapper instanceof NotificationCustomViewWrapper);
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int previousHeight = 0;
        if (this.mExpandedChild != null) {
            previousHeight = this.mExpandedChild.getHeight();
        }
        super.onLayout(changed, left, top, right, bottom);
        if (!(previousHeight == 0 || this.mExpandedChild.getHeight() == previousHeight)) {
            this.mContentHeightAtAnimationStart = previousHeight;
        }
        updateClipping();
        invalidateOutline();
        selectLayout(false, this.mForceSelectNextLayout);
        this.mForceSelectNextLayout = false;
        updateExpandButtons(this.mExpandable);
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateVisibility();
    }

    public View getContractedChild() {
        return this.mContractedChild;
    }

    public View getExpandedChild() {
        return this.mExpandedChild;
    }

    public View getHeadsUpChild() {
        return this.mHeadsUpChild;
    }

    public View getAmbientChild() {
        return this.mAmbientChild;
    }

    public HybridNotificationView getAmbientSingleLineChild() {
        return this.mAmbientSingleLineChild;
    }

    public void setContractedChild(View child) {
        if (this.mContractedChild != null) {
            this.mContractedChild.animate().cancel();
            removeView(this.mContractedChild);
        }
        addView(child);
        this.mContractedChild = child;
        this.mContractedWrapper = NotificationViewWrapper.wrap(getContext(), child, this.mContainingNotification, NotificationViewWrapper.TYPE_SHOWING.TYPE_CONTRACTED);
        this.mContractedWrapper.setDark(this.mDark, false, 0);
    }

    public void setExpandedChild(View child) {
        if (this.mExpandedChild != null) {
            this.mPreviousExpandedRemoteInputIntent = null;
            if (this.mExpandedRemoteInput != null) {
                this.mExpandedRemoteInput.onNotificationUpdateOrReset();
                if (this.mExpandedRemoteInput.isActive()) {
                    this.mPreviousExpandedRemoteInputIntent = this.mExpandedRemoteInput.getPendingIntent();
                    this.mCachedExpandedRemoteInput = this.mExpandedRemoteInput;
                    this.mExpandedRemoteInput.dispatchStartTemporaryDetach();
                    ((ViewGroup) this.mExpandedRemoteInput.getParent()).removeView(this.mExpandedRemoteInput);
                }
            }
            this.mExpandedChild.animate().cancel();
            removeView(this.mExpandedChild);
            this.mExpandedRemoteInput = null;
        }
        if (child == null) {
            this.mExpandedChild = null;
            this.mExpandedWrapper = null;
            if (this.mVisibleType == 1) {
                this.mVisibleType = 0;
            }
            if (this.mTransformationStartVisibleType == 1) {
                this.mTransformationStartVisibleType = -1;
            }
            return;
        }
        addView(child);
        this.mExpandedChild = child;
        this.mExpandedWrapper = NotificationViewWrapper.wrap(getContext(), child, this.mContainingNotification, NotificationViewWrapper.TYPE_SHOWING.TYPE_EXPANDED);
    }

    public void setHeadsUpChild(View child) {
        if (this.mHeadsUpChild != null) {
            this.mPreviousHeadsUpRemoteInputIntent = null;
            if (this.mHeadsUpRemoteInput != null) {
                this.mHeadsUpRemoteInput.onNotificationUpdateOrReset();
                if (this.mHeadsUpRemoteInput.isActive()) {
                    this.mPreviousHeadsUpRemoteInputIntent = this.mHeadsUpRemoteInput.getPendingIntent();
                    this.mCachedHeadsUpRemoteInput = this.mHeadsUpRemoteInput;
                    this.mHeadsUpRemoteInput.dispatchStartTemporaryDetach();
                    ((ViewGroup) this.mHeadsUpRemoteInput.getParent()).removeView(this.mHeadsUpRemoteInput);
                }
            }
            this.mHeadsUpChild.animate().cancel();
            removeView(this.mHeadsUpChild);
            this.mHeadsUpRemoteInput = null;
        }
        if (child == null) {
            this.mHeadsUpChild = null;
            this.mHeadsUpWrapper = null;
            if (this.mVisibleType == 2) {
                this.mVisibleType = 0;
            }
            if (this.mTransformationStartVisibleType == 2) {
                this.mTransformationStartVisibleType = -1;
            }
            return;
        }
        addView(child);
        this.mHeadsUpChild = child;
        this.mHeadsUpWrapper = NotificationViewWrapper.wrap(getContext(), child, this.mContainingNotification, NotificationViewWrapper.TYPE_SHOWING.TYPE_HEADSUP);
    }

    public void setAmbientChild(View child) {
        if (this.mAmbientChild != null) {
            this.mAmbientChild.animate().cancel();
            removeView(this.mAmbientChild);
        }
        if (child != null) {
            addView(child);
            this.mAmbientChild = child;
            this.mAmbientWrapper = NotificationViewWrapper.wrap(getContext(), child, this.mContainingNotification, NotificationViewWrapper.TYPE_SHOWING.TYPE_AMBIENT);
        }
    }

    /* access modifiers changed from: protected */
    public void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        updateVisibility();
    }

    private void updateVisibility() {
        setVisible(isShown());
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getViewTreeObserver().removeOnPreDrawListener(this.mEnableAnimationPredrawListener);
    }

    private void setVisible(boolean isVisible) {
        if (isVisible) {
            getViewTreeObserver().removeOnPreDrawListener(this.mEnableAnimationPredrawListener);
            getViewTreeObserver().addOnPreDrawListener(this.mEnableAnimationPredrawListener);
            return;
        }
        getViewTreeObserver().removeOnPreDrawListener(this.mEnableAnimationPredrawListener);
        this.mAnimate = false;
    }

    private void focusExpandButtonIfNecessary() {
        if (this.mFocusOnVisibilityChange) {
            NotificationHeaderView header = getVisibleNotificationHeader();
            if (header != null) {
                ImageView expandButton = header.getExpandButton();
                if (expandButton != null) {
                    expandButton.requestAccessibilityFocus();
                }
            }
            this.mFocusOnVisibilityChange = false;
        }
    }

    public void setContentHeight(int contentHeight) {
        this.mContentHeight = Math.max(Math.min(contentHeight, getHeight()), getMinHeight());
        selectLayout(this.mAnimate, false);
        int minHeightHint = getMinContentHeightHint();
        NotificationViewWrapper wrapper = getVisibleWrapper(this.mVisibleType);
        if (wrapper != null) {
            wrapper.setContentHeight(this.mContentHeight, minHeightHint);
        }
        NotificationViewWrapper wrapper2 = getVisibleWrapper(this.mTransformationStartVisibleType);
        if (wrapper2 != null) {
            wrapper2.setContentHeight(this.mContentHeight, minHeightHint);
        }
        updateClipping();
        invalidateOutline();
    }

    private int getMinContentHeightHint() {
        int hint;
        if (this.mIsChildInGroup && isVisibleOrTransitioning(3)) {
            return this.mContext.getResources().getDimensionPixelSize(17105204);
        }
        if (!(this.mHeadsUpChild == null || this.mExpandedChild == null)) {
            boolean pinned = false;
            boolean transitioningBetweenHunAndExpanded = isTransitioningFromTo(2, 1) || isTransitioningFromTo(1, 2);
            if (!isVisibleOrTransitioning(0) && ((this.mIsHeadsUp || this.mHeadsUpAnimatingAway) && !this.mContainingNotification.isOnKeyguard())) {
                pinned = true;
            }
            if (transitioningBetweenHunAndExpanded || pinned) {
                return Math.min(this.mHeadsUpChild.getHeight(), this.mExpandedChild.getHeight());
            }
        }
        if (this.mVisibleType == 1 && this.mContentHeightAtAnimationStart >= 0 && this.mExpandedChild != null) {
            return Math.min(this.mContentHeightAtAnimationStart, this.mExpandedChild.getHeight());
        }
        if (this.mAmbientChild != null && isVisibleOrTransitioning(4)) {
            hint = this.mAmbientChild.getHeight();
        } else if (this.mAmbientSingleLineChild != null && isVisibleOrTransitioning(5)) {
            hint = this.mAmbientSingleLineChild.getHeight();
        } else if (this.mHeadsUpChild != null && isVisibleOrTransitioning(2)) {
            hint = this.mHeadsUpChild.getHeight();
        } else if (this.mExpandedChild != null) {
            hint = this.mExpandedChild.getHeight();
        } else {
            hint = this.mContractedChild.getHeight() + this.mContext.getResources().getDimensionPixelSize(17105204);
        }
        if (this.mExpandedChild != null && isVisibleOrTransitioning(1)) {
            hint = Math.min(hint, this.mExpandedChild.getHeight());
        }
        return hint;
    }

    private boolean isTransitioningFromTo(int from, int to) {
        return (this.mTransformationStartVisibleType == from || this.mAnimationStartVisibleType == from) && this.mVisibleType == to;
    }

    private boolean isVisibleOrTransitioning(int type) {
        return this.mVisibleType == type || this.mTransformationStartVisibleType == type || this.mAnimationStartVisibleType == type;
    }

    private void updateContentTransformation() {
        int visibleType = calculateVisibleType();
        if (visibleType != this.mVisibleType) {
            this.mTransformationStartVisibleType = this.mVisibleType;
            TransformableView shownView = getTransformableViewForVisibleType(visibleType);
            TransformableView hiddenView = getTransformableViewForVisibleType(this.mTransformationStartVisibleType);
            shownView.transformFrom(hiddenView, 0.0f);
            getViewForVisibleType(visibleType).setVisibility(0);
            hiddenView.transformTo(shownView, 0.0f);
            this.mVisibleType = visibleType;
            updateBackgroundColor(true);
        }
        if (this.mForceSelectNextLayout) {
            forceUpdateVisibilities();
        }
        if (this.mTransformationStartVisibleType == -1 || this.mVisibleType == this.mTransformationStartVisibleType || getViewForVisibleType(this.mTransformationStartVisibleType) == null) {
            updateViewVisibilities(visibleType);
            updateBackgroundColor(false);
            return;
        }
        TransformableView shownView2 = getTransformableViewForVisibleType(this.mVisibleType);
        TransformableView hiddenView2 = getTransformableViewForVisibleType(this.mTransformationStartVisibleType);
        float transformationAmount = calculateTransformationAmount();
        shownView2.transformFrom(hiddenView2, transformationAmount);
        hiddenView2.transformTo(shownView2, transformationAmount);
        updateBackgroundTransformation(transformationAmount);
    }

    private void updateBackgroundTransformation(float transformationAmount) {
        int endColor = getBackgroundColor(this.mVisibleType);
        int startColor = getBackgroundColor(this.mTransformationStartVisibleType);
        if (endColor != startColor) {
            if (startColor == 0) {
                startColor = this.mContainingNotification.getBackgroundColorWithoutTint();
            }
            if (endColor == 0) {
                endColor = this.mContainingNotification.getBackgroundColorWithoutTint();
            }
            endColor = NotificationUtils.interpolateColors(startColor, endColor, transformationAmount);
        }
        this.mContainingNotification.updateBackgroundAlpha(transformationAmount);
        this.mContainingNotification.setContentBackground(endColor, false, this);
    }

    private float calculateTransformationAmount() {
        int startHeight = getViewForVisibleType(this.mTransformationStartVisibleType).getHeight();
        int endHeight = getViewForVisibleType(this.mVisibleType).getHeight();
        return Math.min(1.0f, ((float) Math.abs(this.mContentHeight - startHeight)) / ((float) Math.abs(endHeight - startHeight)));
    }

    public int getMaxHeight() {
        if (this.mContainingNotification.isShowingAmbient()) {
            return getShowingAmbientView().getHeight();
        }
        if (this.mExpandedChild != null) {
            return this.mExpandedChild.getHeight();
        }
        if (!this.mIsHeadsUp || this.mHeadsUpChild == null || this.mContainingNotification.isOnKeyguard()) {
            return this.mContractedChild.getHeight();
        }
        return this.mHeadsUpChild.getHeight();
    }

    public int getMinHeight() {
        return getMinHeight(false);
    }

    public int getMinHeight(boolean likeGroupExpanded) {
        if (this.mContainingNotification.isShowingAmbient()) {
            return getShowingAmbientView().getHeight();
        }
        if (likeGroupExpanded || !this.mIsChildInGroup || isGroupExpanded()) {
            return this.mContractedChild.getHeight();
        }
        return this.mSingleLineView.getHeight();
    }

    public View getShowingAmbientView() {
        View v = this.mIsChildInGroup ? this.mAmbientSingleLineChild : this.mAmbientChild;
        if (v != null) {
            return v;
        }
        return this.mContractedChild;
    }

    private boolean isGroupExpanded() {
        return this.mGroupManager.isGroupExpanded(this.mStatusBarNotification);
    }

    public void setClipTopAmount(int clipTopAmount) {
        this.mClipTopAmount = clipTopAmount;
        updateClipping();
    }

    public void setClipBottomAmount(int clipBottomAmount) {
        this.mClipBottomAmount = clipBottomAmount;
        updateClipping();
    }

    public void setTranslationY(float translationY) {
        super.setTranslationY(translationY);
        updateClipping();
    }

    private void updateClipping() {
        if (this.mClipToActualHeight) {
            int top = (int) (((float) this.mClipTopAmount) - getTranslationY());
            this.mClipBounds.set(0, top, getWidth(), Math.max(top, (int) (((float) (this.mContentHeight - this.mClipBottomAmount)) - getTranslationY())));
            setClipBounds(this.mClipBounds);
            return;
        }
        setClipBounds(null);
    }

    public void setClipToActualHeight(boolean clipToActualHeight) {
        this.mClipToActualHeight = clipToActualHeight;
        updateClipping();
    }

    private void selectLayout(boolean animate, boolean force) {
        if (this.mContractedChild != null) {
            if (this.mUserExpanding) {
                updateContentTransformation();
            } else {
                int visibleType = calculateVisibleType();
                boolean changedType = visibleType != this.mVisibleType;
                if (changedType || force) {
                    View visibleView = getViewForVisibleType(visibleType);
                    if (visibleView != null) {
                        visibleView.setVisibility(0);
                        transferRemoteInputFocus(visibleType);
                    }
                    if (!animate || ((visibleType != 1 || this.mExpandedChild == null) && ((visibleType != 2 || this.mHeadsUpChild == null) && ((visibleType != 3 || this.mSingleLineView == null) && visibleType != 0)))) {
                        updateViewVisibilities(visibleType);
                    } else {
                        animateToVisibleType(visibleType);
                    }
                    this.mVisibleType = visibleType;
                    if (changedType) {
                        focusExpandButtonIfNecessary();
                    }
                    NotificationViewWrapper visibleWrapper = getVisibleWrapper(visibleType);
                    if (visibleWrapper != null) {
                        visibleWrapper.setContentHeight(this.mContentHeight, getMinContentHeightHint());
                    }
                    updateBackgroundColor(animate);
                }
            }
        }
    }

    private void forceUpdateVisibilities() {
        forceUpdateVisibility(0, this.mContractedChild, this.mContractedWrapper);
        forceUpdateVisibility(1, this.mExpandedChild, this.mExpandedWrapper);
        forceUpdateVisibility(2, this.mHeadsUpChild, this.mHeadsUpWrapper);
        forceUpdateVisibility(3, this.mSingleLineView, this.mSingleLineView);
        forceUpdateVisibility(4, this.mAmbientChild, this.mAmbientWrapper);
        forceUpdateVisibility(5, this.mAmbientSingleLineChild, this.mAmbientSingleLineChild);
        fireExpandedVisibleListenerIfVisible();
        this.mAnimationStartVisibleType = -1;
    }

    private void fireExpandedVisibleListenerIfVisible() {
        if (this.mExpandedVisibleListener != null && this.mExpandedChild != null && isShown() && this.mExpandedChild.getVisibility() == 0) {
            Runnable listener = this.mExpandedVisibleListener;
            this.mExpandedVisibleListener = null;
            listener.run();
        }
    }

    private void forceUpdateVisibility(int type, View view, TransformableView wrapper) {
        if (view != null) {
            if (!(this.mVisibleType == type || this.mTransformationStartVisibleType == type)) {
                view.setVisibility(4);
            } else {
                wrapper.setVisible(true);
            }
        }
    }

    public void updateBackgroundColor(boolean animate) {
        int customBackgroundColor = getBackgroundColor(this.mVisibleType);
        this.mContainingNotification.resetBackgroundAlpha();
        this.mContainingNotification.setContentBackground(customBackgroundColor, animate, this);
    }

    public int getBackgroundColor(int visibleType) {
        NotificationViewWrapper currentVisibleWrapper = getVisibleWrapper(visibleType);
        if (currentVisibleWrapper != null) {
            return currentVisibleWrapper.getCustomBackgroundColor();
        }
        return 0;
    }

    private void updateViewVisibilities(int visibleType) {
        updateViewVisibility(visibleType, 0, this.mContractedChild, this.mContractedWrapper);
        updateViewVisibility(visibleType, 1, this.mExpandedChild, this.mExpandedWrapper);
        updateViewVisibility(visibleType, 2, this.mHeadsUpChild, this.mHeadsUpWrapper);
        updateViewVisibility(visibleType, 3, this.mSingleLineView, this.mSingleLineView);
        updateViewVisibility(visibleType, 4, this.mAmbientChild, this.mAmbientWrapper);
        updateViewVisibility(visibleType, 5, this.mAmbientSingleLineChild, this.mAmbientSingleLineChild);
        fireExpandedVisibleListenerIfVisible();
        this.mAnimationStartVisibleType = -1;
        if (this.mContainingNotification.isChildInGroup()) {
            this.mContainingNotification.getNotificationParent().getChildrenContainer().requestLayout();
        }
    }

    private void updateViewVisibility(int visibleType, int type, View view, TransformableView wrapper) {
        if (view != null) {
            wrapper.setVisible(visibleType == type);
        }
    }

    public boolean isExpansionChanging() {
        return this.mAnimationStartVisibleType != -1;
    }

    private void animateToVisibleType(int visibleType) {
        TransformableView shownView = getTransformableViewForVisibleType(visibleType);
        final TransformableView hiddenView = getTransformableViewForVisibleType(this.mVisibleType);
        if (shownView == hiddenView || hiddenView == null) {
            shownView.setVisible(true);
            return;
        }
        this.mAnimationStartVisibleType = this.mVisibleType;
        shownView.transformFrom(hiddenView);
        getViewForVisibleType(visibleType).setVisibility(0);
        hiddenView.transformTo(shownView, (Runnable) new Runnable() {
            public void run() {
                if (hiddenView != NotificationContentView.this.getTransformableViewForVisibleType(NotificationContentView.this.mVisibleType)) {
                    hiddenView.setVisible(false);
                }
                int unused = NotificationContentView.this.mAnimationStartVisibleType = -1;
                if (NotificationContentView.this.mContainingNotification.isChildInGroup()) {
                    NotificationContentView.this.mContainingNotification.getNotificationParent().getChildrenContainer().requestLayout();
                }
            }
        });
        fireExpandedVisibleListenerIfVisible();
    }

    private void transferRemoteInputFocus(int visibleType) {
        if (visibleType == 2 && this.mHeadsUpRemoteInput != null && this.mExpandedRemoteInput != null && this.mExpandedRemoteInput.isActive()) {
            this.mHeadsUpRemoteInput.stealFocusFrom(this.mExpandedRemoteInput);
        }
        if (visibleType == 1 && this.mExpandedRemoteInput != null && this.mHeadsUpRemoteInput != null && this.mHeadsUpRemoteInput.isActive()) {
            this.mExpandedRemoteInput.stealFocusFrom(this.mHeadsUpRemoteInput);
        }
    }

    /* access modifiers changed from: private */
    public TransformableView getTransformableViewForVisibleType(int visibleType) {
        TransformableView resultView;
        switch (visibleType) {
            case 1:
                resultView = this.mExpandedWrapper;
                break;
            case 2:
                resultView = this.mHeadsUpWrapper;
                break;
            case 3:
                resultView = this.mSingleLineView;
                break;
            case 4:
                resultView = this.mAmbientWrapper;
                break;
            case 5:
                resultView = this.mAmbientSingleLineChild;
                break;
            default:
                resultView = this.mContractedWrapper;
                break;
        }
        return resultView == null ? this.mContractedWrapper : resultView;
    }

    public View getViewForVisibleType(int visibleType) {
        switch (visibleType) {
            case 1:
                return this.mExpandedChild;
            case 2:
                return this.mHeadsUpChild;
            case 3:
                return this.mSingleLineView;
            case 4:
                return this.mAmbientChild;
            case 5:
                return this.mAmbientSingleLineChild;
            default:
                return this.mContractedChild;
        }
    }

    public NotificationViewWrapper getVisibleWrapper(int visibleType) {
        if (visibleType == 4) {
            return this.mAmbientWrapper;
        }
        switch (visibleType) {
            case 0:
                return this.mContractedWrapper;
            case 1:
                return this.mExpandedWrapper;
            case 2:
                return this.mHeadsUpWrapper;
            default:
                return null;
        }
    }

    private boolean isForceShowHeadUpChild() {
        if ((StatusBar.sGameMode || (StatusBar.sIsStatusBarHidden && isLandscape(this.mContext))) && ((this.mIsHeadsUp || this.mHeadsUpAnimatingAway) && this.mHeadsUpChild != null && !this.mContainingNotification.isOnKeyguard())) {
            return true;
        }
        return false;
    }

    private boolean isLandscape(Context context) {
        return context.getResources().getConfiguration().orientation == 2;
    }

    public int calculateVisibleType() {
        int height;
        int collapsedVisualType;
        if (this.mContainingNotification.isShowingAmbient()) {
            if (this.mIsChildInGroup && this.mAmbientSingleLineChild != null) {
                return 5;
            }
            if (this.mAmbientChild != null) {
                return 4;
            }
            return 0;
        } else if (this.mUserExpanding) {
            if (!this.mIsChildInGroup || isGroupExpanded() || this.mContainingNotification.isExpanded(true)) {
                height = this.mContainingNotification.getMaxContentHeight();
            } else {
                height = this.mContainingNotification.getShowingLayout().getMinHeight();
            }
            if (height == 0) {
                height = this.mContentHeight;
            }
            int expandedVisualType = getVisualTypeForHeight((float) height);
            if (!this.mIsChildInGroup || isGroupExpanded()) {
                collapsedVisualType = getVisualTypeForHeight((float) this.mContainingNotification.getCollapsedHeight());
            } else {
                collapsedVisualType = 3;
            }
            return this.mTransformationStartVisibleType == collapsedVisualType ? expandedVisualType : collapsedVisualType;
        } else {
            int intrinsicHeight = this.mContainingNotification.getIntrinsicHeight() - this.mContainingNotification.getExtraPadding();
            int viewHeight = this.mContentHeight;
            if (intrinsicHeight != 0) {
                viewHeight = Math.min(this.mContentHeight, intrinsicHeight);
            }
            return getVisualTypeForHeight((float) viewHeight);
        }
    }

    private int getVisualTypeForHeight(float viewHeight) {
        if (isForceShowHeadUpChild()) {
            return 2;
        }
        boolean noExpandedChild = this.mExpandedChild == null;
        if (!noExpandedChild && viewHeight == ((float) this.mExpandedChild.getHeight())) {
            return 1;
        }
        if (!this.mUserExpanding && this.mIsChildInGroup && !isGroupExpanded()) {
            return 3;
        }
        if ((!this.mIsHeadsUp && !this.mHeadsUpAnimatingAway) || this.mHeadsUpChild == null || this.mContainingNotification.isOnKeyguard()) {
            return (noExpandedChild || (viewHeight <= ((float) this.mContractedChild.getHeight()) && (!this.mIsChildInGroup || isGroupExpanded() || !this.mContainingNotification.isExpanded(true)))) ? 0 : 1;
        }
        if (viewHeight <= ((float) this.mHeadsUpChild.getHeight()) || noExpandedChild) {
            return 2;
        }
        return 1;
    }

    public boolean isContentExpandable() {
        return this.mIsContentExpandable;
    }

    public void setDark(boolean dark, boolean fade, long delay) {
        if (this.mContractedChild != null) {
            this.mDark = dark;
            if (this.mVisibleType == 0 || !dark) {
                this.mContractedWrapper.setDark(dark, fade, delay);
            }
            boolean z = true;
            if (this.mVisibleType == 1 || (this.mExpandedChild != null && !dark)) {
                this.mExpandedWrapper.setDark(dark, fade, delay);
            }
            if (this.mVisibleType == 2 || (this.mHeadsUpChild != null && !dark)) {
                this.mHeadsUpWrapper.setDark(dark, fade, delay);
            }
            if (this.mSingleLineView != null && (this.mVisibleType == 3 || !dark)) {
                this.mSingleLineView.setDark(dark, fade, delay);
            }
            if (dark || !fade) {
                z = false;
            }
            selectLayout(z, false);
        }
    }

    public void setHeadsUp(boolean headsUp) {
        this.mIsHeadsUp = headsUp;
        if (this.mHeadsUpChild != null && (this.mHeadsUpChild instanceof InCallNotificationView)) {
            if (this.mIsHeadsUp) {
                ((InCallNotificationView) this.mHeadsUpChild).show();
            } else {
                ((InCallNotificationView) this.mHeadsUpChild).hide();
            }
        }
        selectLayout(false, true);
        updateExpandButtons(this.mExpandable);
    }

    public boolean hasOverlappingRendering() {
        return false;
    }

    public void setLegacy(boolean legacy) {
        this.mLegacy = legacy;
        updateLegacy();
    }

    private void updateLegacy() {
        if (this.mContractedChild != null) {
            this.mContractedWrapper.setLegacy(this.mLegacy);
        }
        if (this.mExpandedChild != null) {
            this.mExpandedWrapper.setLegacy(this.mLegacy);
        }
        if (this.mHeadsUpChild != null) {
            this.mHeadsUpWrapper.setLegacy(this.mLegacy);
        }
    }

    public void setIsChildInGroup(boolean isChildInGroup) {
        this.mIsChildInGroup = isChildInGroup;
        if (this.mContractedChild != null) {
            this.mContractedWrapper.setIsChildInGroup(this.mIsChildInGroup);
        }
        if (this.mExpandedChild != null) {
            this.mExpandedWrapper.setIsChildInGroup(this.mIsChildInGroup);
        }
        if (this.mHeadsUpChild != null) {
            this.mHeadsUpWrapper.setIsChildInGroup(this.mIsChildInGroup);
        }
        if (this.mAmbientChild != null) {
            this.mAmbientWrapper.setIsChildInGroup(this.mIsChildInGroup);
        }
        updateAllSingleLineViews();
    }

    public void onNotificationUpdated(NotificationData.Entry entry) {
        this.mStatusBarNotification = entry.notification;
        this.mBeforeN = entry.targetSdk < 24;
        updateAllSingleLineViews();
        if (this.mContractedChild != null) {
            this.mContractedWrapper.onContentUpdated(entry.row);
        }
        if (this.mExpandedChild != null) {
            this.mExpandedWrapper.onContentUpdated(entry.row);
        }
        if (this.mHeadsUpChild != null) {
            this.mHeadsUpWrapper.onContentUpdated(entry.row);
        }
        if (this.mAmbientChild != null) {
            this.mAmbientWrapper.onContentUpdated(entry.row);
        }
        applyRemoteInput(entry);
        updateLegacy();
        this.mForceSelectNextLayout = true;
        setDark(this.mDark, false, 0);
        this.mPreviousExpandedRemoteInputIntent = null;
        this.mPreviousHeadsUpRemoteInputIntent = null;
    }

    private void updateAllSingleLineViews() {
        updateSingleLineView();
        updateAmbientSingleLineView();
    }

    private void updateSingleLineView() {
        if (this.mIsChildInGroup) {
            this.mSingleLineView = this.mHybridGroupManager.bindFromNotification(this.mSingleLineView, this.mStatusBarNotification.getNotification());
        } else if (this.mSingleLineView != null) {
            removeView(this.mSingleLineView);
            this.mSingleLineView = null;
        }
    }

    private void updateAmbientSingleLineView() {
        if (this.mIsChildInGroup) {
            this.mAmbientSingleLineChild = this.mHybridGroupManager.bindAmbientFromNotification(this.mAmbientSingleLineChild, this.mStatusBarNotification.getNotification());
        } else if (this.mAmbientSingleLineChild != null) {
            removeView(this.mAmbientSingleLineChild);
            this.mAmbientSingleLineChild = null;
        }
    }

    private void applyRemoteInput(NotificationData.Entry entry) {
        if (this.mRemoteInputController != null) {
            boolean hasRemoteInput = false;
            Notification.Action[] actions = entry.notification.getNotification().actions;
            if (actions != null) {
                boolean hasRemoteInput2 = false;
                for (Notification.Action a : actions) {
                    if (a.getRemoteInputs() != null) {
                        RemoteInput[] remoteInputs = a.getRemoteInputs();
                        int length = remoteInputs.length;
                        int i = 0;
                        while (true) {
                            if (i >= length) {
                                break;
                            } else if (remoteInputs[i].getAllowFreeFormInput()) {
                                hasRemoteInput2 = true;
                                break;
                            } else {
                                i++;
                            }
                        }
                    }
                }
                hasRemoteInput = hasRemoteInput2;
            }
            View bigContentView = this.mExpandedChild;
            if (bigContentView != null) {
                this.mExpandedRemoteInput = applyRemoteInput(bigContentView, entry, hasRemoteInput, this.mPreviousExpandedRemoteInputIntent, this.mCachedExpandedRemoteInput, this.mExpandedWrapper);
            } else {
                this.mExpandedRemoteInput = null;
            }
            if (!(this.mCachedExpandedRemoteInput == null || this.mCachedExpandedRemoteInput == this.mExpandedRemoteInput)) {
                this.mCachedExpandedRemoteInput.dispatchFinishTemporaryDetach();
            }
            this.mCachedExpandedRemoteInput = null;
            View headsUpContentView = this.mHeadsUpChild;
            if (headsUpContentView != null) {
                this.mHeadsUpRemoteInput = applyRemoteInput(headsUpContentView, entry, hasRemoteInput, this.mPreviousHeadsUpRemoteInputIntent, this.mCachedHeadsUpRemoteInput, this.mHeadsUpWrapper);
            } else {
                this.mHeadsUpRemoteInput = null;
            }
            if (!(this.mCachedHeadsUpRemoteInput == null || this.mCachedHeadsUpRemoteInput == this.mHeadsUpRemoteInput)) {
                this.mCachedHeadsUpRemoteInput.dispatchFinishTemporaryDetach();
            }
            this.mCachedHeadsUpRemoteInput = null;
        }
    }

    private RemoteInputView applyRemoteInput(View view, NotificationData.Entry entry, boolean hasRemoteInput, PendingIntent existingPendingIntent, RemoteInputView cachedView, NotificationViewWrapper wrapper) {
        View actionContainerCandidate = view.findViewById(16908687);
        View actionsView = view.findViewById(16908686);
        if (actionsView != null) {
            actionsView.setBackgroundResource(0);
        }
        if (!(actionContainerCandidate instanceof FrameLayout)) {
            return null;
        }
        RemoteInputView existing = (RemoteInputView) view.findViewWithTag(RemoteInputView.VIEW_TAG);
        if (existing != null) {
            existing.onNotificationUpdateOrReset();
        }
        if (existing == null && hasRemoteInput) {
            ViewGroup actionContainer = (FrameLayout) actionContainerCandidate;
            if (cachedView == null) {
                RemoteInputView riv = RemoteInputView.inflate(this.mContext, actionContainer, entry, this.mRemoteInputController);
                riv.setVisibility(4);
                actionContainer.addView(riv, new FrameLayout.LayoutParams(-1, -1));
                existing = riv;
            } else {
                actionContainer.addView(cachedView);
                cachedView.dispatchFinishTemporaryDetach();
                cachedView.requestFocus();
                existing = cachedView;
            }
        }
        if (hasRemoteInput) {
            existing.setWrapper(wrapper);
            if (existingPendingIntent != null || existing.isActive()) {
                Notification.Action[] actions = entry.notification.getNotification().actions;
                if (existingPendingIntent != null) {
                    existing.setPendingIntent(existingPendingIntent);
                }
                if (existing.updatePendingIntentFromActions(actions)) {
                    if (!existing.isActive()) {
                        existing.focus();
                    }
                } else if (existing.isActive()) {
                    existing.close();
                }
            }
        }
        return existing;
    }

    public void closeRemoteInput() {
        if (this.mHeadsUpRemoteInput != null) {
            this.mHeadsUpRemoteInput.close();
        }
        if (this.mExpandedRemoteInput != null) {
            this.mExpandedRemoteInput.close();
        }
    }

    public void setGroupManager(NotificationGroupManager groupManager) {
        this.mGroupManager = groupManager;
    }

    public void setRemoteInputController(RemoteInputController r) {
        this.mRemoteInputController = r;
    }

    public void setExpandClickListener(View.OnClickListener expandClickListener) {
        this.mExpandClickListener = expandClickListener;
    }

    public void updateExpandButtons(boolean expandable) {
        this.mExpandable = expandable;
        if (!(this.mExpandedChild == null || this.mExpandedChild.getHeight() == 0)) {
            if ((this.mIsHeadsUp || this.mHeadsUpAnimatingAway) && this.mHeadsUpChild != null && !this.mContainingNotification.isOnKeyguard()) {
                if (this.mExpandedChild.getHeight() <= this.mHeadsUpChild.getHeight()) {
                    expandable = false;
                }
            } else if (this.mExpandedChild.getHeight() <= this.mContractedChild.getHeight()) {
                expandable = false;
            }
        }
        if (this.mExpandedChild != null) {
            this.mExpandedWrapper.updateExpandability(expandable, this.mExpandClickListener);
        }
        if (this.mContractedChild != null) {
            this.mContractedWrapper.updateExpandability(expandable, this.mExpandClickListener);
        }
        if (this.mHeadsUpChild != null) {
            this.mHeadsUpWrapper.updateExpandability(expandable, this.mExpandClickListener);
        }
        this.mIsContentExpandable = expandable;
    }

    public void showPublic() {
        if (this.mExpandedChild != null) {
            this.mExpandedWrapper.showPublic();
        }
        if (this.mContractedChild != null) {
            this.mContractedWrapper.showPublic();
        }
        if (this.mHeadsUpChild != null) {
            this.mHeadsUpWrapper.showPublic();
        }
    }

    public NotificationHeaderView getNotificationHeader() {
        NotificationHeaderView header = null;
        if (this.mContractedChild != null) {
            header = this.mContractedWrapper.getNotificationHeader();
        }
        if (header == null && this.mExpandedChild != null) {
            header = this.mExpandedWrapper.getNotificationHeader();
        }
        if (header == null && this.mHeadsUpChild != null) {
            header = this.mHeadsUpWrapper.getNotificationHeader();
        }
        if (header != null || this.mAmbientChild == null) {
            return header;
        }
        return this.mAmbientWrapper.getNotificationHeader();
    }

    public NotificationHeaderView getVisibleNotificationHeader() {
        NotificationViewWrapper wrapper = getVisibleWrapper(this.mVisibleType);
        if (wrapper == null) {
            return null;
        }
        return wrapper.getNotificationHeader();
    }

    public void setContainingNotification(ExpandableNotificationRow containingNotification) {
        this.mContainingNotification = containingNotification;
    }

    public void requestSelectLayout(boolean needsAnimation) {
        selectLayout(needsAnimation, false);
    }

    public void reInflateViews() {
        if (this.mIsChildInGroup && this.mSingleLineView != null) {
            removeView(this.mSingleLineView);
            this.mSingleLineView = null;
            updateAllSingleLineViews();
        }
    }

    public void setUserExpanding(boolean userExpanding) {
        this.mUserExpanding = userExpanding;
        if (userExpanding) {
            this.mTransformationStartVisibleType = this.mVisibleType;
            return;
        }
        this.mTransformationStartVisibleType = -1;
        this.mVisibleType = calculateVisibleType();
        updateViewVisibilities(this.mVisibleType);
        updateBackgroundColor(false);
    }

    public void setSingleLineWidthIndention(int singleLineWidthIndention) {
        if (singleLineWidthIndention != this.mSingleLineWidthIndention) {
            this.mSingleLineWidthIndention = singleLineWidthIndention;
            this.mContainingNotification.forceLayout();
            forceLayout();
        }
    }

    public HybridNotificationView getSingleLineView() {
        return this.mSingleLineView;
    }

    public void setRemoved() {
        if (this.mExpandedRemoteInput != null) {
            this.mExpandedRemoteInput.setRemoved();
        }
        if (this.mHeadsUpRemoteInput != null) {
            this.mHeadsUpRemoteInput.setRemoved();
        }
    }

    public void setContentHeightAnimating(boolean animating) {
        if (!animating) {
            this.mContentHeightAtAnimationStart = -1;
        }
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public boolean isAnimatingVisibleType() {
        return this.mAnimationStartVisibleType != -1;
    }

    public void setHeadsUpAnimatingAway(boolean headsUpAnimatingAway) {
        this.mHeadsUpAnimatingAway = headsUpAnimatingAway;
        selectLayout(false, true);
    }

    public void setFocusOnVisibilityChange() {
        this.mFocusOnVisibilityChange = true;
    }

    public void setIconsVisible(boolean iconsVisible) {
        this.mIconsVisible = iconsVisible;
        updateIconVisibilities();
    }

    private void updateIconVisibilities() {
        if (this.mContractedWrapper != null) {
            NotificationHeaderViewCompat.setIconForceHidden(this.mContractedWrapper.getNotificationHeader(), !this.mIconsVisible);
        }
        if (this.mHeadsUpWrapper != null) {
            NotificationHeaderViewCompat.setIconForceHidden(this.mHeadsUpWrapper.getNotificationHeader(), !this.mIconsVisible);
        }
        if (this.mExpandedWrapper != null) {
            NotificationHeaderViewCompat.setIconForceHidden(this.mExpandedWrapper.getNotificationHeader(), !this.mIconsVisible);
        }
    }

    public void onVisibilityAggregated(boolean isVisible) {
        super.onVisibilityAggregated(isVisible);
        if (isVisible) {
            fireExpandedVisibleListenerIfVisible();
        }
    }

    public void setOnExpandedVisibleListener(Runnable r) {
        this.mExpandedVisibleListener = r;
        fireExpandedVisibleListenerIfVisible();
    }

    public void setIsLowPriority(boolean isLowPriority) {
        this.mIsLowPriority = isLowPriority;
    }

    public boolean isDimmable() {
        if (!this.mContractedWrapper.isDimmable()) {
            return false;
        }
        return true;
    }

    public boolean isMediaNotification(ExpandedNotification notification) {
        if (this.mContractedWrapper != null) {
            return this.mContractedWrapper instanceof NotificationMediaTemplateViewWrapper;
        }
        return NotificationUtil.isMediaNotification(notification);
    }

    public boolean isCustomViewNotification(ExpandedNotification notification) {
        if (this.mContractedWrapper != null) {
            return this.mContractedWrapper instanceof NotificationCustomViewWrapper;
        }
        return NotificationUtil.isCustomViewNotification(notification);
    }
}
