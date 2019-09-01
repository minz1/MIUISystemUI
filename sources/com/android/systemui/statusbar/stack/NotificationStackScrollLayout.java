package com.android.systemui.statusbar.stack;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.TimeAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.service.notification.StatusBarNotification;
import android.util.AttributeSet;
import android.util.FloatProperty;
import android.util.Log;
import android.util.Pair;
import android.util.Property;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.AnimationUtils;
import android.widget.OverScroller;
import android.widget.ScrollView;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.Constants;
import com.android.systemui.ExpandHelper;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.SwipeHelper;
import com.android.systemui.Util;
import com.android.systemui.classifier.FalsingManager;
import com.android.systemui.miui.statusbar.notification.FoldFooterView;
import com.android.systemui.miui.statusbar.notification.FoldHeaderView;
import com.android.systemui.miui.statusbar.notification.NotificationUtil;
import com.android.systemui.plugins.qs.QS;
import com.android.systemui.plugins.statusbar.NotificationMenuRowPlugin;
import com.android.systemui.plugins.statusbar.NotificationSwipeActionHelper;
import com.android.systemui.statistic.ScenarioConstants;
import com.android.systemui.statistic.ScenarioTrackUtil;
import com.android.systemui.statusbar.ActivatableNotificationView;
import com.android.systemui.statusbar.EmptyShadeView;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.ExpandableView;
import com.android.systemui.statusbar.FlingAnimationUtils;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.NotificationGuts;
import com.android.systemui.statusbar.NotificationLogger;
import com.android.systemui.statusbar.NotificationShelf;
import com.android.systemui.statusbar.NotificationSnooze;
import com.android.systemui.statusbar.StackScrollerDecorView;
import com.android.systemui.statusbar.notification.NotificationUtils;
import com.android.systemui.statusbar.notification.VisibilityLocationProvider;
import com.android.systemui.statusbar.phone.NotificationGroupManager;
import com.android.systemui.statusbar.phone.ScrimController;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import com.android.systemui.statusbar.policy.ScrollAdapter;
import com.android.systemui.statusbar.stack.StackScrollAlgorithm;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class NotificationStackScrollLayout extends ViewGroup implements ExpandHelper.Callback, SwipeHelper.Callback, NotificationMenuRowPlugin.OnMenuEventListener, ExpandableView.OnHeightChangedListener, VisibilityLocationProvider, NotificationGroupManager.OnGroupChangeListener, ScrollAdapter, ScrollContainer, StackScrollAlgorithm.OnAnimationRequestedListener {
    private static final Property<NotificationStackScrollLayout, Float> BACKGROUND_FADE = new FloatProperty<NotificationStackScrollLayout>("backgroundFade") {
        public void setValue(NotificationStackScrollLayout object, float value) {
            object.setBackgroundFadeAmount(value);
        }

        public Float get(NotificationStackScrollLayout object) {
            return Float.valueOf(object.getBackgroundFadeAmount());
        }
    };
    private static final boolean DEBUG = Constants.DEBUG;
    private static final Object PRESENT = new Object();
    private boolean mActivateNeedsAnimation;
    private int mActivePointerId;
    private ArrayList<View> mAddedHeadsUpChildren;
    private final AmbientState mAmbientState;
    private boolean mAnimateNextBackgroundBottom;
    private boolean mAnimateNextBackgroundTop;
    private Runnable mAnimateScroll;
    private ArrayList<AnimationEvent> mAnimationEvents;
    private ConcurrentHashMap<Runnable, Object> mAnimationFinishedRunnables;
    private boolean mAnimationRunning;
    private boolean mAnimationsEnabled;
    private Rect mBackgroundBounds;
    private Drawable mBackgroundDrawable;
    private float mBackgroundFadeAmount;
    private final Paint mBackgroundPaint;
    /* access modifiers changed from: private */
    public int mBackgroundRadius;
    private boolean mBackwardScrollable;
    private int mBgColor;
    /* access modifiers changed from: private */
    public ObjectAnimator mBottomAnimator;
    private int mBottomInset;
    private int mCachedBackgroundColor;
    private boolean mChangePositionInProgress;
    boolean mCheckForLeavebehind;
    private boolean mChildRemoveAnimationRunning;
    private boolean mChildTransferInProgress;
    private ArrayList<View> mChildrenChangingPositions;
    private HashSet<View> mChildrenToAddAnimated;
    private ArrayList<View> mChildrenToRemoveAnimated;
    /* access modifiers changed from: private */
    public boolean mChildrenUpdateRequested;
    private ViewTreeObserver.OnPreDrawListener mChildrenUpdater;
    private HashSet<View> mClearOverlayViewsWhenFinished;
    private final Rect mClipRect;
    private boolean mClipToOutline;
    private int mCollapsedSize;
    private int mContentHeight;
    private boolean mContinuousShadowUpdate;
    private final Paint mCoverPaint;
    /* access modifiers changed from: private */
    public NotificationMenuRowPlugin mCurrMenuRow;
    /* access modifiers changed from: private */
    public Rect mCurrentBounds;
    private int mCurrentStackHeight;
    private StackScrollState mCurrentStackScrollState;
    private int mDarkAnimationOriginIndex;
    private boolean mDarkNeedsAnimation;
    private Paint mDebugPaint;
    private float mDimAmount;
    /* access modifiers changed from: private */
    public ValueAnimator mDimAnimator;
    private Animator.AnimatorListener mDimEndListener;
    private ValueAnimator.AnimatorUpdateListener mDimUpdateListener;
    private boolean mDimmedNeedsAnimation;
    private boolean mDisallowDismissInThisMotion;
    private boolean mDisallowScrollingInThisMotion;
    private boolean mDismissAllInProgress;
    /* access modifiers changed from: private */
    public boolean mDontClampNextScroll;
    /* access modifiers changed from: private */
    public boolean mDontReportNextOverScroll;
    private int mDownX;
    private ArrayList<View> mDragAnimPendingChildren;
    private boolean mDrawBackgroundAsSrc;
    protected EmptyShadeView mEmptyShadeView;
    /* access modifiers changed from: private */
    public Rect mEndAnimationRect;
    private boolean mEverythingNeedsAnimation;
    private ExpandHelper mExpandHelper;
    private View mExpandedGroupView;
    private float mExpandedHeight;
    private boolean mExpandedInThisMotion;
    private boolean mExpandingNotification;
    private int mExtraBottomRange;
    private int mExtraBottomRangeQsCovered;
    private boolean mFadingOut;
    private FalsingManager mFalsingManager;
    private Runnable mFinishScrollingCallback;
    private ExpandableView mFirstVisibleBackgroundChild;
    private FlingAnimationUtils mFlingAnimationUtils;
    private View mFoldFooterView;
    private View mFoldHeaderView;
    private boolean mForceNoOverlappingRendering;
    private View mForcedScroll;
    private boolean mForwardScrollable;
    private HashSet<View> mFromMoreCardAdditions;
    private boolean mGenerateChildOrderChangedEvent;
    private long mGoToFullShadeDelay;
    private boolean mGoToFullShadeNeedsAnimation;
    private boolean mGroupExpandedForMeasure;
    private NotificationGroupManager mGroupManager;
    private boolean mHeadsUpAnimatingAway;
    private HashSet<Pair<ExpandableNotificationRow, Boolean>> mHeadsUpChangeAnimations;
    private HeadsUpManager mHeadsUpManager;
    private boolean mHeadsUpPinned;
    private boolean mHideSensitiveNeedsAnimation;
    private boolean mInHeadsUpPinnedMode;
    private int mIncreasedPaddingBetweenElements;
    private float mInitialTouchX;
    private float mInitialTouchY;
    private int mIntrinsicPadding;
    private boolean mIsBeingDragged;
    private boolean mIsClipped;
    /* access modifiers changed from: private */
    public boolean mIsExpanded;
    private boolean mIsExpansionChanging;
    /* access modifiers changed from: private */
    public boolean mIsQsBeingCovered;
    private boolean mIsQsCovered;
    private int mLastDrawBoundsBottom;
    private int mLastDrawBoundsTop;
    private int mLastMotionY;
    private int mLastNavigationBarMode;
    private ExpandableView mLastVisibleBackgroundChild;
    private NotificationLogger.OnChildLocationsChangedListener mListener;
    private SwipeHelper.LongPressListener mLongPressListener;
    private int mMaxDisplayedNotifications;
    private int mMaxLayoutHeight;
    private float mMaxOverScroll;
    private int mMaxScrollAfterExpand;
    private int mMaximumVelocity;
    /* access modifiers changed from: private */
    public View mMenuExposedView;
    private SwipeHelper.MenuPressListener mMenuPressListener;
    private float mMinTopOverScrollToEscape;
    private int mMinimumVelocity;
    private boolean mNavBarDarkMode;
    private boolean mNeedViewResizeAnimation;
    private boolean mNeedsAnimation;
    private boolean mNoAmbient;
    private OnEmptySpaceClickListener mOnEmptySpaceClickListener;
    private ExpandableView.OnHeightChangedListener mOnHeightChangedListener;
    private OnTopPaddingUpdateListener mOnTopPaddingUpdateListener;
    private boolean mOnlyScrollingInThisMotion;
    private int mOrientation;
    private float mOverScrolledBottomPixels;
    private float mOverScrolledTopPixels;
    private int mOverflingDistance;
    private OnOverscrollTopChangedListener mOverscrollTopChangedListener;
    /* access modifiers changed from: private */
    public int mOwnScrollY;
    private int mPaddingBetweenElements;
    private boolean mPanelTracking;
    private boolean mParentNotFullyVisible;
    private ArrayList<View> mPendingDismissChildren;
    private ArrayList<View> mPendingPopupChildren;
    private Collection<HeadsUpManager.HeadsUpEntry> mPulsing;
    /* access modifiers changed from: private */
    public QS mQs;
    /* access modifiers changed from: private */
    public ValueAnimator mQsBeingCoveredAnimator;
    private boolean mQsExpanded;
    private Runnable mReclamp;
    private Rect mRequestedClipBounds;
    private int mRowExtraPadding;
    private ViewTreeObserver.OnPreDrawListener mRunningAnimationUpdater;
    private ScrimController mScrimController;
    private boolean mScrollable;
    private boolean mScrolledToTopOnFirstDown;
    /* access modifiers changed from: private */
    public OverScroller mScroller;
    protected boolean mScrollingEnabled;
    private ViewTreeObserver.OnPreDrawListener mShadowUpdater;
    private NotificationShelf mShelf;
    private final boolean mShouldDrawNotificationBackground;
    private ArrayList<View> mSnappedBackChildren;
    private PorterDuffXfermode mSrcMode;
    protected final StackScrollAlgorithm mStackScrollAlgorithm;
    private float mStackTranslation;
    /* access modifiers changed from: private */
    public Rect mStartAnimationRect;
    private final StackStateAnimator mStateAnimator;
    /* access modifiers changed from: private */
    public StatusBar mStatusBar;
    private int mStatusBarHeight;
    private int mStatusBarState;
    private NotificationSwipeHelper mSwipeHelper;
    private ArrayList<View> mSwipedOutViews;
    private boolean mSwipingInProgress;
    private int[] mTempInt2;
    private final ArrayList<Pair<ExpandableNotificationRow, Boolean>> mTmpList;
    private ArrayList<ExpandableView> mTmpSortedChildren;
    /* access modifiers changed from: private */
    public ObjectAnimator mTopAnimator;
    /* access modifiers changed from: private */
    public int mTopPadding;
    private boolean mTopPaddingNeedsAnimation;
    private float mTopPaddingOverflow;
    private boolean mTouchIsClick;
    private int mTouchSlop;
    private boolean mTrackingHeadsUp;
    /* access modifiers changed from: private */
    public View mTranslatingParentView;
    private final Paint mTransparentPaint;
    private VelocityTracker mVelocityTracker;
    private Comparator<ExpandableView> mViewPositionComparator;
    private List<ExpandableView> mVisibleRows;

    static class AnimationEvent {
        static AnimationFilter[] FILTERS = {new AnimationFilter().animateShadowAlpha().animateHeight().animateTopInset().animateY().animateZ().hasDelays(), new AnimationFilter().animateShadowAlpha().animateHeight().animateTopInset().animateY().animateZ().hasDelays(), new AnimationFilter().animateShadowAlpha().animateHeight().animateTopInset().animateY().animateZ().hasDelays(), new AnimationFilter().animateShadowAlpha().animateHeight().animateTopInset().animateY().animateDimmed().animateZ(), new AnimationFilter().animateShadowAlpha(), new AnimationFilter().animateShadowAlpha().animateHeight(), new AnimationFilter().animateZ(), new AnimationFilter().animateDimmed(), new AnimationFilter().animateAlpha().animateShadowAlpha().animateHeight().animateTopInset().animateY().animateZ(), null, new AnimationFilter().animateShadowAlpha().animateHeight().animateTopInset().animateY().animateDimmed().animateZ().hasDelays(), new AnimationFilter().animateHideSensitive(), new AnimationFilter().animateShadowAlpha().animateHeight().animateTopInset().animateY().animateZ(), new AnimationFilter().animateAlpha().animateShadowAlpha().animateHeight().animateTopInset().animateY().animateZ(), new AnimationFilter().animateShadowAlpha().animateHeight().animateTopInset().animateY().animateZ(), new AnimationFilter().animateShadowAlpha().animateHeight().animateTopInset().animateY().animateZ(), new AnimationFilter().animateShadowAlpha().animateHeight().animateTopInset().animateY().animateZ().hasDelays(), new AnimationFilter().animateShadowAlpha().animateHeight().animateTopInset().animateY().animateZ(), new AnimationFilter().animateAlpha().animateScale(), new AnimationFilter().animateAlpha().animateScale(), new AnimationFilter().animateAlpha().animateShadowAlpha().animateDark().animateDimmed().animateHideSensitive().animateHeight().animateTopInset().animateY().animateZ()};
        static int[] LENGTHS = {464, 464, 360, 360, 360, 360, 220, 220, 360, 200, 448, 360, 360, 360, 500, 150, 150, 360, 300, 150, 360};
        final int animationType;
        final View changingView;
        int darkAnimationOriginIndex;
        final long eventStartTime;
        final AnimationFilter filter;
        boolean headsUpFromBottom;
        final long length;
        View viewAfterChangingView;

        AnimationEvent(View view, int type) {
            this(view, type, (long) LENGTHS[type]);
        }

        AnimationEvent(View view, int type, AnimationFilter filter2) {
            this(view, type, (long) LENGTHS[type], filter2);
        }

        AnimationEvent(View view, int type, long length2) {
            this(view, type, length2, FILTERS[type]);
        }

        AnimationEvent(View view, int type, long length2, AnimationFilter filter2) {
            this.eventStartTime = AnimationUtils.currentAnimationTimeMillis();
            this.changingView = view;
            this.animationType = type;
            this.length = length2;
            this.filter = filter2;
        }

        static long combineLength(ArrayList<AnimationEvent> events) {
            long length2 = 0;
            int size = events.size();
            for (int i = 0; i < size; i++) {
                AnimationEvent event = events.get(i);
                length2 = Math.max(length2, event.length);
                if (event.animationType == 10) {
                    return event.length;
                }
            }
            return length2;
        }
    }

    private class NotificationSwipeHelper extends SwipeHelper implements NotificationSwipeActionHelper {
        private Runnable mFalsingCheck;
        private Handler mHandler = new Handler();

        public NotificationSwipeHelper(int swipeDirection, SwipeHelper.Callback callback, Context context) {
            super(swipeDirection, callback, context);
            this.mFalsingCheck = new Runnable(NotificationStackScrollLayout.this) {
                public void run() {
                    NotificationSwipeHelper.this.resetExposedMenuView(true, true);
                }
            };
        }

        public void onDownUpdate(View currView, MotionEvent ev) {
            View unused = NotificationStackScrollLayout.this.mTranslatingParentView = currView;
            NotificationMenuRowPlugin unused2 = NotificationStackScrollLayout.this.mCurrMenuRow = null;
            if (NotificationStackScrollLayout.this.mCurrMenuRow != null) {
                NotificationStackScrollLayout.this.mCurrMenuRow.onTouchEvent(currView, ev, 0.0f);
            }
            this.mHandler.removeCallbacks(this.mFalsingCheck);
            resetExposedMenuView(true, false);
            if (currView instanceof ExpandableNotificationRow) {
                NotificationMenuRowPlugin unused3 = NotificationStackScrollLayout.this.mCurrMenuRow = ((ExpandableNotificationRow) currView).createMenu();
                NotificationStackScrollLayout.this.mCurrMenuRow.setSwipeActionHelper(this);
                NotificationStackScrollLayout.this.mCurrMenuRow.setMenuClickListener(NotificationStackScrollLayout.this);
            }
        }

        public void onMoveUpdate(View view, MotionEvent ev, float translation, float delta) {
            this.mHandler.removeCallbacks(this.mFalsingCheck);
            if (NotificationStackScrollLayout.this.mCurrMenuRow != null) {
                NotificationStackScrollLayout.this.mCurrMenuRow.onTouchEvent(view, ev, 0.0f);
            }
        }

        public boolean handleUpEvent(MotionEvent ev, View animView, float velocity, float translation) {
            if (NotificationStackScrollLayout.this.mCurrMenuRow != null) {
                return NotificationStackScrollLayout.this.mCurrMenuRow.onTouchEvent(animView, ev, velocity);
            }
            return false;
        }

        public void dismissChild(View view, float velocity, boolean useAccelerateInterpolator) {
            super.dismissChild(view, velocity, useAccelerateInterpolator);
            if (NotificationStackScrollLayout.this.mIsExpanded) {
                NotificationStackScrollLayout.this.handleChildDismissed(view);
            }
            NotificationStackScrollLayout.this.mStatusBar.closeAndSaveGuts(true, false, false, -1, -1, false);
            handleMenuCoveredOrDismissed();
        }

        public void snapChild(View animView, float targetLeft, float velocity) {
            super.snapChild(animView, targetLeft, velocity);
            NotificationStackScrollLayout.this.onDragCancelled(animView);
            if (targetLeft == 0.0f) {
                handleMenuCoveredOrDismissed();
            }
        }

        public void snooze(StatusBarNotification sbn, NotificationSwipeActionHelper.SnoozeOption snoozeOption) {
            NotificationStackScrollLayout.this.mStatusBar.setNotificationSnoozed(sbn, snoozeOption);
        }

        public boolean isFalseGesture(MotionEvent ev) {
            return super.isFalseGesture(ev);
        }

        private void handleMenuCoveredOrDismissed() {
            if (NotificationStackScrollLayout.this.mMenuExposedView != null && NotificationStackScrollLayout.this.mMenuExposedView == NotificationStackScrollLayout.this.mTranslatingParentView) {
                View unused = NotificationStackScrollLayout.this.mMenuExposedView = null;
            }
        }

        public Animator getViewTranslationAnimator(View v, float target, ValueAnimator.AnimatorUpdateListener listener) {
            if (v instanceof ExpandableNotificationRow) {
                return ((ExpandableNotificationRow) v).getTranslateViewAnimator(target, listener);
            }
            return super.getViewTranslationAnimator(v, target, listener);
        }

        public void setTranslation(View v, float translate) {
            if (canViewSliding(v)) {
                ((ExpandableView) v).setTranslation(translate);
            }
        }

        public float getTranslation(View v) {
            return ((ExpandableView) v).getTranslation();
        }

        public void dismiss(View animView, float velocity) {
            dismissChild(animView, velocity, !swipedFastEnough(0.0f, 0.0f));
        }

        public void snap(View animView, float targetLeft, float velocity) {
            snapChild(animView, targetLeft, velocity);
        }

        public boolean swipedFarEnough(float translation, float viewSize) {
            return swipedFarEnough();
        }

        public boolean swipedFastEnough(float translation, float velocity) {
            return swipedFastEnough();
        }

        public float getMinDismissVelocity() {
            return getEscapeVelocity();
        }

        private boolean canViewSliding(View view) {
            return !(view instanceof FoldHeaderView) && !(view instanceof FoldFooterView);
        }

        public void onMenuShown(View animView) {
            NotificationStackScrollLayout.this.onDragCancelled(animView);
            if (NotificationStackScrollLayout.this.isAntiFalsingNeeded()) {
                this.mHandler.removeCallbacks(this.mFalsingCheck);
                this.mHandler.postDelayed(this.mFalsingCheck, 4000);
            }
        }

        public void closeControlsIfOutsideTouch(MotionEvent ev) {
            NotificationGuts guts = NotificationStackScrollLayout.this.mStatusBar.getExposedGuts();
            View view = null;
            if (guts != null && !guts.getGutsContent().isLeavebehind()) {
                view = guts;
            } else if (!(NotificationStackScrollLayout.this.mCurrMenuRow == null || !NotificationStackScrollLayout.this.mCurrMenuRow.isMenuVisible() || NotificationStackScrollLayout.this.mTranslatingParentView == null)) {
                view = NotificationStackScrollLayout.this.mTranslatingParentView;
            }
            if (view != null && !NotificationStackScrollLayout.this.isTouchInView(ev, view)) {
                NotificationStackScrollLayout.this.mStatusBar.closeAndSaveGuts(false, false, true, -1, -1, false);
                resetExposedMenuView(true, true);
            }
        }

        public void resetExposedMenuView(boolean animate, boolean force) {
            if (NotificationStackScrollLayout.this.mMenuExposedView != null && (force || NotificationStackScrollLayout.this.mMenuExposedView != NotificationStackScrollLayout.this.mTranslatingParentView)) {
                View prevMenuExposedView = NotificationStackScrollLayout.this.mMenuExposedView;
                if (animate) {
                    Animator anim = getViewTranslationAnimator(prevMenuExposedView, 0.0f, null);
                    if (anim != null) {
                        anim.start();
                    }
                } else if (NotificationStackScrollLayout.this.mMenuExposedView instanceof ExpandableNotificationRow) {
                    ((ExpandableNotificationRow) NotificationStackScrollLayout.this.mMenuExposedView).resetTranslation();
                }
                View unused = NotificationStackScrollLayout.this.mMenuExposedView = null;
            }
        }
    }

    public interface OnChildLocationsChangedListener {
    }

    public interface OnEmptySpaceClickListener {
        void onEmptySpaceClicked(float f, float f2);
    }

    public interface OnOverscrollTopChangedListener {
        void flingTopOverscroll(float f, boolean z);

        void onOverscrollTopChanged(float f, boolean z);
    }

    public interface OnTopPaddingUpdateListener {
        void onScrollerTopPaddingUpdate(int i);
    }

    public NotificationStackScrollLayout(Context context) {
        this(context, null);
    }

    public NotificationStackScrollLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NotificationStackScrollLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public NotificationStackScrollLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mCurrentStackHeight = Integer.MAX_VALUE;
        this.mBackgroundPaint = new Paint();
        this.mTransparentPaint = new Paint();
        this.mCoverPaint = new Paint();
        this.mActivePointerId = -1;
        this.mBottomInset = 0;
        this.mExtraBottomRange = 0;
        this.mExtraBottomRangeQsCovered = 0;
        this.mCurrentStackScrollState = new StackScrollState(this);
        this.mChildrenToAddAnimated = new HashSet<>();
        this.mAddedHeadsUpChildren = new ArrayList<>();
        this.mChildrenToRemoveAnimated = new ArrayList<>();
        this.mSnappedBackChildren = new ArrayList<>();
        this.mDragAnimPendingChildren = new ArrayList<>();
        this.mChildrenChangingPositions = new ArrayList<>();
        this.mFromMoreCardAdditions = new HashSet<>();
        this.mAnimationEvents = new ArrayList<>();
        this.mSwipedOutViews = new ArrayList<>();
        this.mPendingPopupChildren = new ArrayList<>();
        this.mPendingDismissChildren = new ArrayList<>();
        this.mStateAnimator = new StackStateAnimator(this);
        this.mIsExpanded = true;
        this.mOrientation = 1;
        this.mLastNavigationBarMode = -1;
        this.mChildrenUpdater = new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                NotificationStackScrollLayout.this.updateForcedScroll();
                NotificationStackScrollLayout.this.updateChildren();
                boolean unused = NotificationStackScrollLayout.this.mChildrenUpdateRequested = false;
                NotificationStackScrollLayout.this.getViewTreeObserver().removeOnPreDrawListener(this);
                return true;
            }
        };
        this.mTempInt2 = new int[2];
        this.mAnimationFinishedRunnables = new ConcurrentHashMap<>();
        this.mClearOverlayViewsWhenFinished = new HashSet<>();
        this.mHeadsUpChangeAnimations = new HashSet<>();
        this.mTmpList = new ArrayList<>();
        this.mRunningAnimationUpdater = new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                NotificationStackScrollLayout.this.onPreDrawDuringAnimation();
                return true;
            }
        };
        this.mBackgroundBounds = new Rect();
        this.mStartAnimationRect = new Rect();
        this.mEndAnimationRect = new Rect();
        this.mCurrentBounds = new Rect(-1, -1, -1, -1);
        this.mBottomAnimator = null;
        this.mTopAnimator = null;
        this.mFirstVisibleBackgroundChild = null;
        this.mLastVisibleBackgroundChild = null;
        this.mTmpSortedChildren = new ArrayList<>();
        this.mDimEndListener = new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                ValueAnimator unused = NotificationStackScrollLayout.this.mDimAnimator = null;
            }
        };
        this.mDimUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                NotificationStackScrollLayout.this.setDimAmount(((Float) animation.getAnimatedValue()).floatValue());
            }
        };
        this.mShadowUpdater = new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                NotificationStackScrollLayout.this.updateViewShadows();
                return true;
            }
        };
        this.mViewPositionComparator = new Comparator<ExpandableView>() {
            public int compare(ExpandableView view, ExpandableView otherView) {
                float endY = view.getTranslationY() + ((float) view.getActualHeight());
                float otherEndY = otherView.getTranslationY() + ((float) otherView.getActualHeight());
                if (endY < otherEndY) {
                    return -1;
                }
                if (endY > otherEndY) {
                    return 1;
                }
                return 0;
            }
        };
        this.mSrcMode = new PorterDuffXfermode(PorterDuff.Mode.SRC);
        this.mBackgroundFadeAmount = 1.0f;
        this.mMaxDisplayedNotifications = -1;
        this.mClipRect = new Rect();
        this.mAnimateScroll = new Runnable() {
            public void run() {
                NotificationStackScrollLayout.this.animateScroll();
            }
        };
        this.mLastDrawBoundsTop = -1;
        this.mLastDrawBoundsBottom = -1;
        this.mVisibleRows = new ArrayList();
        this.mReclamp = new Runnable() {
            public void run() {
                NotificationStackScrollLayout.this.mScroller.startScroll(NotificationStackScrollLayout.this.mScrollX, NotificationStackScrollLayout.this.mOwnScrollY, 0, NotificationStackScrollLayout.this.getScrollRange() - NotificationStackScrollLayout.this.mOwnScrollY);
                boolean unused = NotificationStackScrollLayout.this.mDontReportNextOverScroll = true;
                boolean unused2 = NotificationStackScrollLayout.this.mDontClampNextScroll = true;
                NotificationStackScrollLayout.this.animateScroll();
            }
        };
        Resources res = getResources();
        this.mRowExtraPadding = res.getDimensionPixelSize(R.dimen.notification_row_extra_padding);
        this.mAmbientState = new AmbientState(context);
        this.mBgColor = context.getColor(R.color.notification_shade_background_color);
        this.mExpandHelper = new ExpandHelper(getContext(), this, res.getDimensionPixelSize(R.dimen.notification_min_height), res.getDimensionPixelSize(R.dimen.notification_max_height));
        this.mExpandHelper.setEventSource(this);
        this.mExpandHelper.setScrollAdapter(this);
        this.mSwipeHelper = new NotificationSwipeHelper(0, this, getContext());
        this.mSwipeHelper.setLongPressListener(this.mLongPressListener);
        this.mStackScrollAlgorithm = createStackScrollAlgorithm(context);
        this.mStackScrollAlgorithm.setOnAnimationRequestedListener(this);
        initView(context);
        this.mFalsingManager = FalsingManager.getInstance(context);
        this.mShouldDrawNotificationBackground = res.getBoolean(R.bool.config_drawNotificationBackground);
        updateWillNotDraw();
        setOutlineProvider(new ViewOutlineProvider() {
            public void getOutline(View view, Outline outline) {
                if (!NotificationStackScrollLayout.this.onKeyguard()) {
                    outline.setRoundRect(0, NotificationStackScrollLayout.this.mCurrentBounds.top, NotificationStackScrollLayout.this.getWidth(), NotificationStackScrollLayout.this.mCurrentBounds.bottom, (float) NotificationStackScrollLayout.this.mBackgroundRadius);
                    outline.setAlpha(0.0f);
                    NotificationStackScrollLayout.this.switchNavigationBarModeIfNeed();
                }
            }
        });
        updateClipToOutline();
        if (DEBUG) {
            this.mDebugPaint = new Paint();
            this.mDebugPaint.setColor(-65536);
            this.mDebugPaint.setStrokeWidth(2.0f);
            this.mDebugPaint.setStyle(Paint.Style.STROKE);
        }
    }

    public void setLastNavigationBarMode(int mode) {
        if (this.mLastNavigationBarMode != -1) {
            this.mLastNavigationBarMode = mode;
        }
    }

    /* access modifiers changed from: private */
    public void switchNavigationBarModeIfNeed() {
        if (!this.mStatusBar.isFullScreenGestureMode()) {
            boolean z = false;
            int i = -1;
            if (this.mOrientation == 2) {
                if (this.mLastNavigationBarMode != -1) {
                    updateNavigationBarMode(false);
                    this.mLastNavigationBarMode = -1;
                    this.mNavBarDarkMode = false;
                }
                return;
            }
            if (this.mCurrentBounds.bottom > this.mStatusBar.getNavigationBarYPosition() && this.mStatusBar.getNavigationBarYPosition() > 0) {
                z = true;
            }
            boolean switchToDark = z;
            if (switchToDark != this.mNavBarDarkMode) {
                this.mNavBarDarkMode = switchToDark;
                updateNavigationBarMode(switchToDark);
                if (switchToDark) {
                    i = this.mStatusBar.getNavigationBarMode();
                }
                this.mLastNavigationBarMode = i;
            }
        }
    }

    private void updateNavigationBarMode(boolean beDark) {
        this.mStatusBar.getNavigationBarView().getBarTransitions().transitionTo(beDark ? 1 : this.mLastNavigationBarMode, true);
        this.mStatusBar.getNavigationBarView().setDisabledFlags(this.mStatusBar.getFlagDisable1(), true);
    }

    public NotificationSwipeActionHelper getSwipeActionHelper() {
        return this.mSwipeHelper;
    }

    public void setFlingAnimationUtils(FlingAnimationUtils flingAnimationUtils) {
        this.mFlingAnimationUtils = flingAnimationUtils;
    }

    public void onMenuClicked(View view, int x, int y, NotificationMenuRowPlugin.MenuItem item) {
        if (this.mMenuPressListener != null) {
            if (view instanceof ExpandableNotificationRow) {
                MetricsLogger.action(this.mContext, 333, ((ExpandableNotificationRow) view).getStatusBarNotification().getPackageName());
            }
            this.mMenuPressListener.onMenuPress(view, x, y, item);
        }
    }

    public void onMenuReset(View row) {
        if (this.mTranslatingParentView != null && row == this.mTranslatingParentView) {
            this.mMenuExposedView = null;
            this.mTranslatingParentView = null;
        }
    }

    public void onMenuShown(View row) {
        this.mMenuExposedView = this.mTranslatingParentView;
        if (row instanceof ExpandableNotificationRow) {
            MetricsLogger.action(this.mContext, 332, ((ExpandableNotificationRow) row).getStatusBarNotification().getPackageName());
        }
        this.mSwipeHelper.onMenuShown(row);
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        if (!onKeyguard() && this.mShouldDrawNotificationBackground && !this.mAmbientState.isDark() && this.mCurrentBounds.top < this.mCurrentBounds.bottom && (this.mStatusBar.hasActiveNotifications() || isFoldViewVisible())) {
            this.mBackgroundDrawable.setBounds(0, this.mCurrentBounds.top, getWidth(), this.mCurrentBounds.bottom);
            this.mBackgroundDrawable.draw(canvas);
            drawTransparentArea(canvas);
        }
        if (!(this.mLastDrawBoundsBottom == this.mCurrentBounds.bottom && this.mLastDrawBoundsTop == this.mCurrentBounds.top)) {
            invalidateOutline();
            this.mLastDrawBoundsTop = this.mCurrentBounds.top;
            this.mLastDrawBoundsBottom = this.mCurrentBounds.bottom;
        }
        if (DEBUG) {
            int y = this.mTopPadding;
            canvas.drawLine(0.0f, (float) y, (float) getWidth(), (float) y, this.mDebugPaint);
            int y2 = getLayoutHeight();
            canvas.drawLine(0.0f, (float) y2, (float) getWidth(), (float) y2, this.mDebugPaint);
            int y3 = getHeight() - getEmptyBottomMargin();
            canvas.drawLine(0.0f, (float) y3, (float) getWidth(), (float) y3, this.mDebugPaint);
        }
    }

    private void drawTransparentArea(Canvas canvas) {
        boolean isExpansionChange;
        boolean isGutsAnimating;
        boolean isGroupExpansionChanging;
        if (Util.isDefaultTheme() && !this.mDismissAllInProgress && !NotificationUtil.isFoldAnimating()) {
            this.mVisibleRows.clear();
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (((child instanceof ExpandableNotificationRow) || (child instanceof FoldHeaderView) || (child instanceof FoldFooterView)) && child.getVisibility() != 8) {
                    this.mVisibleRows.add((ExpandableView) child);
                }
            }
            int i2 = 0;
            while (true) {
                int i3 = i2;
                if (i3 < this.mVisibleRows.size()) {
                    ExpandableView view = this.mVisibleRows.get(i3);
                    ExpandableView nextView = i3 + 1 < this.mVisibleRows.size() ? this.mVisibleRows.get(i3 + 1) : null;
                    if (view instanceof ExpandableNotificationRow) {
                        ExpandableNotificationRow row = (ExpandableNotificationRow) view;
                        isGroupExpansionChanging = row.isGroupExpansionChanging();
                        isGutsAnimating = row.getGuts() != null && row.getGuts().isAnimating();
                        isExpansionChange = row.isExpansionChanging();
                        if (row.getTranslation() != 0.0f) {
                            float top = row.getTranslationY();
                            float bottom = top + ((float) (isGutsAnimating ? row.getActualHeight() : row.getIntrinsicHeight()));
                            if (row.getTranslation() <= 0.0f) {
                                drawCoverAndBackground(canvas, row, 0.0f, top, ((float) getWidth()) + row.getTranslation(), bottom);
                            } else if (StackScrollAlgorithm.canChildBeDismissed(row)) {
                                drawCoverAndBackground(canvas, row, row.getTranslation(), top, (float) getWidth(), bottom);
                            } else {
                                drawCover(canvas, 0.0f, top, row.getTranslation(), bottom);
                            }
                        }
                    } else {
                        isGroupExpansionChanging = false;
                        isGutsAnimating = false;
                        isExpansionChange = false;
                    }
                    if (nextView != null) {
                        float top2 = view.getTranslationY() + ((float) view.getIntrinsicHeight());
                        float bottom2 = nextView.getTranslationY();
                        if (!isGroupExpansionChanging && !isGutsAnimating && !isExpansionChange && top2 < bottom2) {
                            drawCover(canvas, 0.0f, top2, (float) getWidth(), bottom2);
                        }
                    }
                    if (i3 == 0) {
                        float top3 = (float) this.mCurrentBounds.top;
                        float bottom3 = view.getTranslationY();
                        if (top3 < bottom3) {
                            drawCover(canvas, 0.0f, top3, (float) getWidth(), bottom3);
                        }
                    }
                    if (i3 == this.mVisibleRows.size() - 1) {
                        float top4 = view.getTranslationY() + ((float) view.getIntrinsicHeight());
                        float bottom4 = (float) this.mCurrentBounds.bottom;
                        if (!(view instanceof FoldFooterView) && !isGroupExpansionChanging && !isGutsAnimating && !isExpansionChange && top4 < bottom4) {
                            drawCover(canvas, 0.0f, top4, (float) getWidth(), bottom4);
                        }
                    }
                    i2 = i3 + 1;
                } else {
                    return;
                }
            }
        }
    }

    private void drawCoverAndBackground(Canvas canvas, ExpandableNotificationRow row, float left, float top, float right, float bottom) {
        drawCover(canvas, 0.0f, top, (float) getWidth(), bottom);
        int alpha = 255;
        if (getResources().getBoolean(R.bool.notification_swipe_background_alpha_enabled)) {
            alpha = (int) (((float) Color.alpha(getResources().getColor(R.color.notification_panel_background_without_base))) * SwipeHelper.getAlphaForOffset(row.getTranslation()));
        }
        this.mBackgroundPaint.setAlpha(alpha);
        canvas.drawRect(left, top, right, bottom, this.mBackgroundPaint);
    }

    private void drawCover(Canvas canvas, float left, float top, float right, float bottom) {
        canvas.drawRect(left, top, right, bottom, this.mTransparentPaint);
        canvas.drawRect(left, top, right, bottom, this.mCoverPaint);
    }

    /* access modifiers changed from: private */
    public void updateBackgroundDimming() {
        if (this.mShouldDrawNotificationBackground) {
            float alpha = (0.7f + (0.3f * (1.0f - this.mDimAmount))) * this.mBackgroundFadeAmount;
            int scrimColor = this.mScrimController.getScrimBehindColor();
            float alphaInv = 1.0f - alpha;
            int color = Color.argb((int) ((255.0f * alpha) + (((float) Color.alpha(scrimColor)) * alphaInv)), (int) ((this.mBackgroundFadeAmount * ((float) Color.red(this.mBgColor))) + (((float) Color.red(scrimColor)) * alphaInv)), (int) ((this.mBackgroundFadeAmount * ((float) Color.green(this.mBgColor))) + (((float) Color.green(scrimColor)) * alphaInv)), (int) ((this.mBackgroundFadeAmount * ((float) Color.blue(this.mBgColor))) + (((float) Color.blue(scrimColor)) * alphaInv)));
            if (this.mCachedBackgroundColor != color) {
                this.mCachedBackgroundColor = color;
                invalidate();
            }
        }
    }

    private void initView(Context context) {
        this.mScroller = new OverScroller(getContext());
        setDescendantFocusability(262144);
        setClipChildren(false);
        ViewConfiguration configuration = ViewConfiguration.get(context);
        this.mTouchSlop = configuration.getScaledTouchSlop();
        this.mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        this.mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        this.mOverflingDistance = configuration.getScaledOverflingDistance();
        this.mCollapsedSize = context.getResources().getDimensionPixelSize(R.dimen.notification_min_height);
        this.mStackScrollAlgorithm.initView(context);
        this.mAmbientState.reload(context);
        this.mPaddingBetweenElements = Math.max(1, context.getResources().getDimensionPixelSize(R.dimen.notification_divider_height));
        this.mIncreasedPaddingBetweenElements = context.getResources().getDimensionPixelSize(R.dimen.notification_divider_height_increased);
        this.mMinTopOverScrollToEscape = (float) getResources().getDimensionPixelSize(R.dimen.min_top_overscroll_to_qs);
        this.mStatusBarHeight = getResources().getDimensionPixelOffset(R.dimen.status_bar_height);
        this.mBackgroundRadius = getResources().getDimensionPixelSize(R.dimen.notification_stack_scroller_bg_radius);
        this.mBackgroundDrawable = getResources().getDrawable(R.drawable.notification_panel_bg);
        this.mTransparentPaint.setColor(0);
        this.mTransparentPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        this.mTransparentPaint.setAntiAlias(true);
        this.mCoverPaint.setColor(context.getResources().getColor(R.color.notification_panel_transparent_cover));
        this.mBackgroundPaint.setColor(context.getResources().getColor(R.color.notification_panel_background_without_base));
    }

    public void setDrawBackgroundAsSrc(boolean asSrc) {
        this.mDrawBackgroundAsSrc = asSrc;
        updateSrcDrawing();
    }

    private void updateSrcDrawing() {
        if (this.mShouldDrawNotificationBackground) {
            invalidate();
        }
    }

    /* access modifiers changed from: private */
    public void notifyHeightChangeListener(ExpandableView view) {
        if (this.mOnHeightChangedListener != null) {
            this.mOnHeightChangedListener.onHeightChanged(view, false);
        }
    }

    /* access modifiers changed from: private */
    public void notifyTopPaddingUpdateListener(int topPadding) {
        int topPadding2 = Math.min(Math.max(topPadding, this.mQs.getQsHeaderHeight()), this.mQs.getQsMinExpansionHeight());
        this.mTopPaddingOverflow = 0.0f;
        if (this.mOnTopPaddingUpdateListener != null) {
            this.mOnTopPaddingUpdateListener.onScrollerTopPaddingUpdate(topPadding2);
        }
    }

    public void doExpandCollapseAnimation(boolean expand) {
        doExpandCollapseAnimation(expand, 2500);
    }

    public void doExpandCollapseAnimation(boolean expand, int vel) {
        int i = 1;
        if (expand) {
            this.mIsQsBeingCovered = true;
        } else {
            this.mIsQsCovered = false;
        }
        if (expand) {
            i = -1;
        }
        endQsBeingCoveredMotion(i * vel);
        this.mActivePointerId = -1;
        endDrag();
    }

    private void endQsBeingCoveredMotion(int vel) {
        int currValue = this.mTopPadding;
        int endValue = vel >= 0 ? this.mQs.getQsMinExpansionHeight() : this.mQs.getQsHeaderHeight();
        this.mQsBeingCoveredAnimator = ValueAnimator.ofInt(new int[]{currValue, endValue});
        this.mFlingAnimationUtils.apply((Animator) this.mQsBeingCoveredAnimator, (float) currValue, (float) endValue, (float) vel);
        this.mQsBeingCoveredAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                NotificationStackScrollLayout.this.notifyTopPaddingUpdateListener(((Integer) animation.getAnimatedValue()).intValue());
            }
        });
        this.mQsBeingCoveredAnimator.addListener(new Animator.AnimatorListener() {
            public void onAnimationStart(Animator animator) {
            }

            public void onAnimationEnd(Animator animator) {
                boolean z = false;
                boolean unused = NotificationStackScrollLayout.this.mIsQsBeingCovered = false;
                NotificationStackScrollLayout notificationStackScrollLayout = NotificationStackScrollLayout.this;
                if (NotificationStackScrollLayout.this.mTopPadding == NotificationStackScrollLayout.this.mQs.getQsHeaderHeight()) {
                    z = true;
                }
                notificationStackScrollLayout.resetIsQsCovered(z);
                ValueAnimator unused2 = NotificationStackScrollLayout.this.mQsBeingCoveredAnimator = null;
            }

            public void onAnimationCancel(Animator animator) {
            }

            public void onAnimationRepeat(Animator animator) {
            }
        });
        this.mQsBeingCoveredAnimator.start();
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int size = getChildCount();
        for (int i = 0; i < size; i++) {
            measureChild(getChildAt(i), widthMeasureSpec, heightMeasureSpec);
        }
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        float centerX = ((float) getWidth()) / 2.0f;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            float width = (float) child.getMeasuredWidth();
            child.layout((int) (centerX - (width / 2.0f)), 0, (int) ((width / 2.0f) + centerX), (int) ((float) child.getMeasuredHeight()));
        }
        setMaxLayoutHeight(getHeight());
        updateContentHeight();
        clampScrollPosition();
        requestChildrenUpdate();
        updateFirstAndLastBackgroundViews();
        updateAlgorithmLayoutMinHeight();
    }

    private void requestAnimationOnViewResize(ExpandableNotificationRow row) {
        if (!this.mAnimationsEnabled) {
            return;
        }
        if (this.mIsExpanded || (row != null && row.isPinned())) {
            this.mNeedViewResizeAnimation = true;
            this.mNeedsAnimation = true;
        }
    }

    public void updateSpeedBumpIndex(int newIndex, boolean noAmbient) {
        this.mAmbientState.setSpeedBumpIndex(newIndex);
        this.mNoAmbient = noAmbient;
    }

    public void setChildLocationsChangedListener(NotificationLogger.OnChildLocationsChangedListener listener) {
        this.mListener = listener;
    }

    public boolean isInVisibleLocation(ExpandableNotificationRow row) {
        ExpandableViewState childViewState = this.mCurrentStackScrollState.getViewStateForView(row);
        boolean z = false;
        if (childViewState == null) {
            return false;
        }
        int i = childViewState.location & 5;
        childViewState.location = i;
        if (i == 0 || row.getVisibility() != 0) {
            return false;
        }
        if (this.mHeadsUpPinned || this.mHeadsUpAnimatingAway) {
            return this.mHeadsUpManager.isHeadsUp(row.getEntry().key);
        }
        row.getLocationOnScreen(this.mTempInt2);
        int top = this.mTempInt2[1];
        if (childViewState.height + top >= Math.max(0, Math.max(getTop(), this.mBackgroundBounds.top)) && top <= Math.min(getBottom(), this.mBackgroundBounds.bottom)) {
            z = true;
        }
        return z;
    }

    public boolean isFoldFooterViewInVisibleLocation() {
        if (this.mFoldFooterView == null || this.mFoldFooterView.getVisibility() != 0) {
            return false;
        }
        this.mFoldFooterView.getLocationOnScreen(this.mTempInt2);
        int midLine = this.mTempInt2[1] + (this.mFoldFooterView.getMeasuredHeight() / 2);
        if (midLine < Math.max(0, Math.max(getTop(), this.mBackgroundBounds.top)) || midLine > Math.min(getBottom(), this.mBackgroundBounds.bottom)) {
            return false;
        }
        return true;
    }

    private void setMaxLayoutHeight(int maxLayoutHeight) {
        this.mMaxLayoutHeight = maxLayoutHeight;
        this.mShelf.setMaxLayoutHeight(maxLayoutHeight);
        updateAlgorithmHeightAndPadding();
    }

    private void updateAlgorithmHeightAndPadding() {
        this.mAmbientState.setLayoutHeight(getLayoutHeight());
        updateAlgorithmLayoutMinHeight();
        this.mAmbientState.setTopPadding(this.mTopPadding);
    }

    private void updateAlgorithmLayoutMinHeight() {
        this.mAmbientState.setLayoutMinHeight((!this.mQsExpanded || onKeyguard()) ? 0 : getLayoutMinHeight());
    }

    /* access modifiers changed from: private */
    public void updateChildren() {
        float f;
        updateScrollStateForAddedChildren();
        AmbientState ambientState = this.mAmbientState;
        if (this.mScroller.isFinished()) {
            f = 0.0f;
        } else {
            f = this.mScroller.getCurrVelocity();
        }
        ambientState.setCurrentScrollVelocity(f);
        this.mAmbientState.setScrollY(this.mOwnScrollY);
        this.mStackScrollAlgorithm.getStackScrollState(this.mAmbientState, this.mCurrentStackScrollState);
        if (isCurrentlyAnimating() || this.mNeedsAnimation) {
            startAnimationToState();
        } else {
            applyCurrentState();
        }
    }

    public void onPopupAnimationRequested(ExpandableView view) {
        this.mPendingPopupChildren.add(view);
        this.mNeedsAnimation = true;
        Log.v("StackScroller", "on pop animation requested");
    }

    public void onDismissAnimationRequested(ExpandableView view) {
        this.mPendingDismissChildren.add(view);
        this.mNeedsAnimation = true;
        Log.v("StackScroller", "on dismiss animation requested");
    }

    /* access modifiers changed from: private */
    public void onPreDrawDuringAnimation() {
        this.mShelf.updateAppearance();
        if (!this.mNeedsAnimation && !this.mChildrenUpdateRequested) {
            updateBackground();
        }
        if (this.mChildRemoveAnimationRunning) {
            postInvalidateOnAnimation();
        }
    }

    private void updateScrollStateForAddedChildren() {
        int padding;
        if (!this.mChildrenToAddAnimated.isEmpty()) {
            for (int i = 0; i < getChildCount(); i++) {
                ExpandableView child = (ExpandableView) getChildAt(i);
                if (this.mChildrenToAddAnimated.contains(child)) {
                    int startingPosition = getPositionInLinearLayout(child);
                    float increasedPaddingAmount = child.getIncreasedPaddingAmount();
                    if (increasedPaddingAmount == 1.0f) {
                        padding = this.mIncreasedPaddingBetweenElements;
                    } else {
                        padding = increasedPaddingAmount == -1.0f ? 0 : this.mPaddingBetweenElements;
                    }
                    int childHeight = getIntrinsicHeight(child) + padding;
                    if (startingPosition < this.mOwnScrollY) {
                        setOwnScrollY(this.mOwnScrollY + childHeight);
                    }
                }
            }
            clampScrollPosition();
        }
    }

    /* access modifiers changed from: private */
    public void updateForcedScroll() {
        if (this.mForcedScroll != null && (!this.mForcedScroll.hasFocus() || !this.mForcedScroll.isAttachedToWindow())) {
            this.mForcedScroll = null;
        }
        if (this.mForcedScroll != null) {
            ExpandableView expandableView = (ExpandableView) this.mForcedScroll;
            int positionInLinearLayout = getPositionInLinearLayout(expandableView);
            int targetScroll = targetScrollForView(expandableView, positionInLinearLayout);
            int outOfViewScroll = expandableView.getIntrinsicHeight() + positionInLinearLayout;
            int targetScroll2 = Math.max(0, Math.min(targetScroll, getScrollRange()));
            if (this.mOwnScrollY < targetScroll2 || outOfViewScroll < this.mOwnScrollY) {
                setOwnScrollY(targetScroll2);
            }
        }
    }

    private void requestChildrenUpdate() {
        if (!this.mChildrenUpdateRequested) {
            getViewTreeObserver().addOnPreDrawListener(this.mChildrenUpdater);
            this.mChildrenUpdateRequested = true;
            invalidate();
        }
    }

    private boolean isCurrentlyAnimating() {
        return this.mStateAnimator.isRunning();
    }

    private void clampScrollPosition() {
        int scrollRange = getScrollRange();
        if (scrollRange < this.mOwnScrollY) {
            setOwnScrollY(scrollRange);
        }
    }

    public int getTopPadding() {
        return this.mTopPadding;
    }

    private void setTopPadding(int topPadding, boolean animate) {
        if (DEBUG) {
            Log.d("StackScroller", "setTopPadding topPadding=" + topPadding);
        }
        if (this.mTopPadding != topPadding) {
            this.mTopPadding = topPadding;
            updateContentHeight();
            updateAlgorithmHeightAndPadding();
            if (animate && this.mAnimationsEnabled && this.mIsExpanded) {
                this.mTopPaddingNeedsAnimation = true;
                this.mNeedsAnimation = true;
            }
            requestChildrenUpdate();
            notifyHeightChangeListener(null);
        }
    }

    public void setExpandedHeight(float height) {
        int stackHeight;
        float translationY;
        float translationY2;
        if (DEBUG) {
            Log.d("StackScroller", "setExpandedHeight height=" + height);
        }
        this.mExpandedHeight = height;
        float f = 0.0f;
        setIsExpanded(height > 0.0f);
        int minExpansionHeight = getMinExpansionHeight();
        if (height < ((float) minExpansionHeight)) {
            this.mClipRect.left = 0;
            this.mClipRect.right = getWidth();
            this.mClipRect.top = 0;
            this.mClipRect.bottom = (int) height;
            height = (float) minExpansionHeight;
            setRequestedClipBounds(this.mClipRect);
        } else {
            setRequestedClipBounds(null);
        }
        float appearEndPosition = getAppearEndPosition();
        float appearStartPosition = getAppearStartPosition();
        float appearFraction = 1.0f;
        if (height >= appearEndPosition) {
            translationY = 0.0f;
            if (!this.mIsExpansionChanging || onKeyguard()) {
                stackHeight = (int) height;
                appearFraction = 1.0f;
            } else {
                float stackMinHeight = (float) getStackAppearMinHeight();
                stackHeight = (int) Math.max(height, appearEndPosition + stackMinHeight);
                if (stackMinHeight > 0.0f) {
                    appearFraction = Math.min((height - appearEndPosition) / stackMinHeight, 1.0f);
                }
            }
            updateStackAppearState(appearFraction, getWidth() / 2, (int) appearEndPosition);
        } else {
            float appearFraction2 = getAppearFraction(height);
            if (appearFraction2 >= 0.0f) {
                translationY2 = NotificationUtils.interpolate(getExpandTranslationStart(), 0.0f, appearFraction2);
            } else {
                translationY2 = (height - appearStartPosition) + getExpandTranslationStart();
            }
            stackHeight = (int) (height - translationY2);
            if (this.mHeadsUpPinned || this.mHeadsUpAnimatingAway) {
                f = 1.0f;
            }
            setTransitionAlpha(f);
            translationY = translationY2;
        }
        if (stackHeight != this.mCurrentStackHeight) {
            this.mCurrentStackHeight = stackHeight;
            updateAlgorithmHeightAndPadding();
            requestChildrenUpdate();
        }
        setStackTranslation(translationY);
    }

    private void updateStackAppearState(float appearFraction, int pivotX, int pivotY) {
        float scale = 0.92f + (0.07999998f * appearFraction);
        setPivotX((float) pivotX);
        setPivotY((float) pivotY);
        setScaleX(scale);
        setScaleY(scale);
        setTransitionAlpha(appearFraction);
    }

    private int getStackAppearMinHeight() {
        int topBottomPadding = 0;
        int height = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            if (v.getVisibility() == 0 && (v instanceof ExpandableView)) {
                if (!(v instanceof ExpandableNotificationRow) || ((ExpandableView) v).getViewType() != 0) {
                    height += ((ExpandableView) v).getActualHeight();
                } else {
                    ExpandableNotificationRow row = (ExpandableNotificationRow) v;
                    if ((row.areChildrenExpanded() || row.isGroupExpansionChanging()) && row.getNotificationChildren() != null && row.getNotificationChildren().size() > 0) {
                        row = row.getNotificationChildren().get(0);
                    }
                    if (!row.isMediaNotification()) {
                        topBottomPadding = this.mRowExtraPadding * 2;
                    }
                    return ((row.getIntrinsicHeight() + height) - row.getExtraPadding()) + topBottomPadding;
                }
            }
        }
        return height;
    }

    private void setRequestedClipBounds(Rect clipRect) {
        this.mRequestedClipBounds = clipRect;
        updateClipping();
    }

    private void updateClipToOutline() {
        boolean clipToOutline = !onKeyguard() && !this.mHeadsUpAnimatingAway && !this.mHeadsUpPinned;
        if (this.mClipToOutline != clipToOutline) {
            setClipToOutline(clipToOutline);
            this.mClipToOutline = clipToOutline;
            if (DEBUG) {
                Log.d("StackScroller", "updateClipToOutline " + this.mClipToOutline);
            }
        }
    }

    public void updateClipping() {
        boolean clipped = this.mRequestedClipBounds != null && !this.mInHeadsUpPinnedMode && !this.mHeadsUpAnimatingAway;
        if (this.mIsClipped != clipped) {
            this.mIsClipped = clipped;
            updateFadingState();
        }
        if (clipped) {
            setClipBounds(this.mRequestedClipBounds);
        } else {
            setClipBounds(null);
        }
    }

    private float getExpandTranslationStart() {
        if (this.mHeadsUpPinned || this.mHeadsUpAnimatingAway) {
            return (float) (-this.mTopPadding);
        }
        return 0.0f;
    }

    private float getAppearStartPosition() {
        if (!this.mTrackingHeadsUp || this.mFirstVisibleBackgroundChild == null || !this.mFirstVisibleBackgroundChild.isAboveShelf()) {
            return (float) getMinExpansionHeight();
        }
        return (float) this.mFirstVisibleBackgroundChild.getPinnedHeadsUpHeight();
    }

    private float getAppearEndPosition() {
        int appearPosition;
        int notGoneChildCount = getNotGoneChildCount();
        if (this.mEmptyShadeView.getVisibility() != 8 || notGoneChildCount == 0) {
            appearPosition = this.mEmptyShadeView.getHeight();
        } else {
            int minNotificationsForShelf = 1;
            if (this.mTrackingHeadsUp || this.mHeadsUpManager.hasPinnedHeadsUp()) {
                appearPosition = this.mHeadsUpManager.getTopHeadsUpPinnedHeight();
                minNotificationsForShelf = 2;
            } else {
                appearPosition = 0;
            }
            if (notGoneChildCount >= minNotificationsForShelf) {
                appearPosition += this.mShelf.getIntrinsicHeight();
            }
        }
        return (float) ((onKeyguard() != 0 ? this.mTopPadding : this.mIntrinsicPadding) + appearPosition);
    }

    public float getAppearFraction(float height) {
        float appearEndPosition = getAppearEndPosition();
        float appearStartPosition = getAppearStartPosition();
        if (appearStartPosition == appearEndPosition) {
            return 0.0f;
        }
        return (height - appearStartPosition) / (appearEndPosition - appearStartPosition);
    }

    public float getStackTranslation() {
        return this.mStackTranslation;
    }

    private void setStackTranslation(float stackTranslation) {
        if (stackTranslation != this.mStackTranslation) {
            this.mStackTranslation = stackTranslation;
            this.mAmbientState.setStackTranslation(stackTranslation);
            requestChildrenUpdate();
        }
    }

    private int getLayoutHeight() {
        if (this.mCurrentStackHeight < this.mMaxLayoutHeight) {
            return this.mCurrentStackHeight;
        }
        return this.mMaxLayoutHeight + this.mBackgroundRadius;
    }

    public int getFirstItemMinHeight() {
        ExpandableView firstChild = getFirstChildNotGone();
        return firstChild != null ? firstChild.getMinHeight() : this.mCollapsedSize;
    }

    public void setLongPressListener(SwipeHelper.LongPressListener listener) {
        this.mSwipeHelper.setLongPressListener(listener);
        this.mLongPressListener = listener;
    }

    public void setMenuPressListener(SwipeHelper.MenuPressListener listener) {
        this.mMenuPressListener = listener;
    }

    public void setQs(QS qs) {
        this.mQs = qs;
    }

    public void onChildDismissed(View v) {
        if (v instanceof ExpandableNotificationRow) {
            ExpandableNotificationRow row = (ExpandableNotificationRow) v;
            if (!row.isDismissed()) {
                handleChildDismissed(v);
            }
            ViewGroup transientContainer = row.getTransientContainer();
            if (transientContainer != null) {
                transientContainer.removeTransientView(v);
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleChildDismissed(View v) {
        if (!this.mDismissAllInProgress) {
            setSwipingInProgress(false);
            if (this.mDragAnimPendingChildren.contains(v)) {
                this.mDragAnimPendingChildren.remove(v);
            }
            this.mSwipedOutViews.add(v);
            this.mAmbientState.onDragFinished(v);
            updateContinuousShadowDrawing();
            if (v instanceof ExpandableNotificationRow) {
                ExpandableNotificationRow row = (ExpandableNotificationRow) v;
                if (row.isHeadsUp()) {
                    this.mHeadsUpManager.addSwipedOutNotification(row.getStatusBarNotification().getKey());
                }
            }
            performDismiss(v, this.mGroupManager, false);
            this.mFalsingManager.onNotificationDismissed();
            if (this.mFalsingManager.shouldEnforceBouncer()) {
                this.mStatusBar.executeRunnableDismissingKeyguard(null, null, false, true, false);
            }
        }
    }

    public static void performDismiss(View v, NotificationGroupManager groupManager, boolean fromAccessibility) {
        if (v instanceof ExpandableNotificationRow) {
            ExpandableNotificationRow row = (ExpandableNotificationRow) v;
            if (groupManager.isOnlyChildInGroup(row.getStatusBarNotification())) {
                ExpandableNotificationRow groupSummary = groupManager.getLogicalGroupSummary(row.getStatusBarNotification());
                if (groupSummary.isClearable()) {
                    performDismiss(groupSummary, groupManager, fromAccessibility);
                }
            }
            row.setDismissed(true, fromAccessibility);
            if (row.isClearable()) {
                row.performDismiss();
            }
            if (DEBUG) {
                Log.v("StackScroller", "onChildDismissed: " + v);
            }
        }
    }

    public void onChildSnappedBack(View animView, float targetLeft) {
        this.mAmbientState.onDragFinished(animView);
        updateContinuousShadowDrawing();
        if (!this.mDragAnimPendingChildren.contains(animView)) {
            if (this.mAnimationsEnabled) {
                this.mSnappedBackChildren.add(animView);
                this.mNeedsAnimation = true;
            }
            requestChildrenUpdate();
        } else {
            this.mDragAnimPendingChildren.remove(animView);
        }
        if (this.mCurrMenuRow != null && targetLeft == 0.0f) {
            this.mCurrMenuRow.resetMenu();
            this.mCurrMenuRow = null;
        }
    }

    public void onBeginDrag(View v) {
        this.mFalsingManager.onNotificatonStartDismissing();
        setSwipingInProgress(true);
        this.mAmbientState.onBeginDrag(v);
        updateContinuousShadowDrawing();
        if (this.mAnimationsEnabled && (this.mIsExpanded || !isPinnedHeadsUp(v))) {
            this.mDragAnimPendingChildren.add(v);
            this.mNeedsAnimation = true;
        }
        requestChildrenUpdate();
    }

    public static boolean isPinnedHeadsUp(View v) {
        boolean z = false;
        if (!(v instanceof ExpandableNotificationRow)) {
            return false;
        }
        ExpandableNotificationRow row = (ExpandableNotificationRow) v;
        if (row.isHeadsUp() && row.isPinned()) {
            z = true;
        }
        return z;
    }

    private boolean isHeadsUp(View v) {
        if (v instanceof ExpandableNotificationRow) {
            return ((ExpandableNotificationRow) v).isHeadsUp();
        }
        return false;
    }

    public void onDragCancelled(View v) {
        this.mFalsingManager.onNotificatonStopDismissing();
        setSwipingInProgress(false);
    }

    public float getFalsingThresholdFactor() {
        return this.mStatusBar.isWakeUpComingFromTouch() ? 1.5f : 1.0f;
    }

    public View getChildAtPosition(MotionEvent ev) {
        ExpandableView childAtPosition = getChildAtPosition(ev.getX(), ev.getY());
        if (!(childAtPosition instanceof ExpandableNotificationRow)) {
            return childAtPosition;
        }
        ExpandableNotificationRow parent = ((ExpandableNotificationRow) childAtPosition).getNotificationParent();
        if (parent == null || !parent.areChildrenExpanded()) {
            return childAtPosition;
        }
        if (parent.areGutsExposed() || this.mMenuExposedView == parent || (parent.getNotificationChildren().size() == 1 && parent.isClearable())) {
            return parent;
        }
        return childAtPosition;
    }

    public ExpandableView getClosestChildAtRawPosition(float touchX, float touchY) {
        getLocationOnScreen(this.mTempInt2);
        float localTouchY = touchY - ((float) this.mTempInt2[1]);
        ExpandableView closestChild = null;
        float minDist = Float.MAX_VALUE;
        int count = getChildCount();
        for (int childIdx = 0; childIdx < count; childIdx++) {
            ExpandableView slidingChild = (ExpandableView) getChildAt(childIdx);
            if (slidingChild.getVisibility() != 8 && !(slidingChild instanceof StackScrollerDecorView)) {
                float childTop = slidingChild.getTranslationY();
                float dist = Math.min(Math.abs((((float) slidingChild.getClipTopAmount()) + childTop) - localTouchY), Math.abs(((((float) slidingChild.getActualHeight()) + childTop) - ((float) slidingChild.getClipBottomAmount())) - localTouchY));
                if (dist < minDist) {
                    closestChild = slidingChild;
                    minDist = dist;
                }
            }
        }
        return closestChild;
    }

    public ExpandableView getChildAtRawPosition(float touchX, float touchY) {
        getLocationOnScreen(this.mTempInt2);
        return getChildAtPosition(touchX - ((float) this.mTempInt2[0]), touchY - ((float) this.mTempInt2[1]));
    }

    public ExpandableView getChildAtPosition(float touchX, float touchY) {
        int count = getChildCount();
        for (int childIdx = 0; childIdx < count; childIdx++) {
            ExpandableView slidingChild = (ExpandableView) getChildAt(childIdx);
            if (slidingChild.getVisibility() != 8 && !(slidingChild instanceof StackScrollerDecorView)) {
                float childTop = slidingChild.getTranslationY();
                float bottom = (((float) slidingChild.getActualHeight()) + childTop) - ((float) slidingChild.getClipBottomAmount());
                int right = getWidth();
                if (touchY >= ((float) slidingChild.getClipTopAmount()) + childTop && touchY <= bottom && touchX >= ((float) 0) && touchX <= ((float) right)) {
                    if (!(slidingChild instanceof ExpandableNotificationRow)) {
                        return slidingChild;
                    }
                    ExpandableNotificationRow row = (ExpandableNotificationRow) slidingChild;
                    if (this.mIsExpanded || !row.isHeadsUp() || !row.isPinned() || this.mHeadsUpManager.getTopEntry().entry.row == row || this.mGroupManager.getGroupSummary((StatusBarNotification) this.mHeadsUpManager.getTopEntry().entry.row.getStatusBarNotification()) == row) {
                        return row.getViewAtPosition(touchY - childTop);
                    }
                }
            }
        }
        return null;
    }

    public boolean canChildBeExpanded(View v) {
        return (v instanceof ExpandableNotificationRow) && ((ExpandableNotificationRow) v).isExpandable() && !((ExpandableNotificationRow) v).areGutsExposed() && (this.mIsExpanded || !((ExpandableNotificationRow) v).isPinned());
    }

    public void setUserExpandedChild(View v, boolean userExpanded) {
        if (v instanceof ExpandableNotificationRow) {
            ExpandableNotificationRow row = (ExpandableNotificationRow) v;
            if (!userExpanded || !onKeyguard()) {
                row.setUserExpanded(userExpanded, true);
                row.onExpandedByGesture(userExpanded);
            } else {
                row.setUserLocked(false);
                updateContentHeight();
                notifyHeightChangeListener(row);
            }
        }
    }

    public void setExpansionCancelled(View v) {
        if (v instanceof ExpandableNotificationRow) {
            ((ExpandableNotificationRow) v).setGroupExpansionChanging(false);
        }
    }

    public void setUserLockedChild(View v, boolean userLocked) {
        if (v instanceof ExpandableNotificationRow) {
            ((ExpandableNotificationRow) v).setUserLocked(userLocked);
        }
        removeLongPressCallback();
        requestDisallowInterceptTouchEvent(true);
    }

    public void expansionStateChanged(boolean isExpanding) {
        this.mExpandingNotification = isExpanding;
        if (!this.mExpandedInThisMotion) {
            this.mMaxScrollAfterExpand = this.mOwnScrollY;
            this.mExpandedInThisMotion = true;
        }
    }

    public int getMaxExpandHeight(ExpandableView view) {
        return view.getMaxContentHeight();
    }

    public void setScrollingEnabled(boolean enable) {
        this.mScrollingEnabled = enable;
    }

    public void lockScrollTo(View v) {
        if (this.mForcedScroll != v) {
            this.mForcedScroll = v;
            scrollTo(v);
        }
    }

    public boolean scrollTo(View v) {
        ExpandableView expandableView = (ExpandableView) v;
        int positionInLinearLayout = getPositionInLinearLayout(v);
        int targetScroll = targetScrollForView(expandableView, positionInLinearLayout);
        int outOfViewScroll = expandableView.getIntrinsicHeight() + positionInLinearLayout;
        if (this.mOwnScrollY >= targetScroll && outOfViewScroll >= this.mOwnScrollY) {
            return false;
        }
        this.mScroller.startScroll(this.mScrollX, this.mOwnScrollY, 0, targetScroll - this.mOwnScrollY);
        this.mDontReportNextOverScroll = true;
        animateScroll();
        return true;
    }

    private int targetScrollForView(ExpandableView v, int positionInLinearLayout) {
        return (((v.getIntrinsicHeight() + positionInLinearLayout) + getImeInset()) - getHeight()) + getTopPadding();
    }

    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        this.mBottomInset = insets.getSystemWindowInsetBottom();
        if (this.mOwnScrollY > getScrollRange()) {
            removeCallbacks(this.mReclamp);
            postDelayed(this.mReclamp, 50);
        } else if (this.mForcedScroll != null) {
            scrollTo(this.mForcedScroll);
        }
        return insets;
    }

    public void setExpandingEnabled(boolean enable) {
        this.mExpandHelper.setEnabled(enable);
    }

    private boolean isScrollingEnabled() {
        return this.mScrollingEnabled;
    }

    public boolean canChildBeDismissed(View v) {
        return StackScrollAlgorithm.canChildBeDismissed(v);
    }

    public boolean isAntiFalsingNeeded() {
        return onKeyguard();
    }

    /* access modifiers changed from: private */
    public boolean onKeyguard() {
        return this.mStatusBarState == 1;
    }

    private void setSwipingInProgress(boolean isSwiped) {
        this.mSwipingInProgress = isSwiped;
        if (isSwiped) {
            requestDisallowInterceptTouchEvent(true);
        }
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.mSwipeHelper.setDensityScale(getResources().getDisplayMetrics().density);
        this.mSwipeHelper.setPagingTouchSlop((float) ViewConfiguration.get(getContext()).getScaledPagingTouchSlop());
        initView(getContext());
        if (this.mOrientation != getResources().getConfiguration().orientation) {
            this.mOrientation = getResources().getConfiguration().orientation;
            switchNavigationBarModeIfNeed();
        }
    }

    public void snapViewIfNeeded(ExpandableNotificationRow child) {
        this.mSwipeHelper.snapChildIfNeeded(child, this.mIsExpanded || isPinnedHeadsUp(child), child.getProvider().isMenuVisible() ? child.getTranslation() : 0.0f);
    }

    public boolean onTouchEvent(MotionEvent ev) {
        boolean isCancelOrUp = ev.getActionMasked() == 3 || ev.getActionMasked() == 1;
        handleEmptySpaceClick(ev);
        boolean expandWantsIt = false;
        if (this.mIsExpanded && !this.mSwipingInProgress && !this.mOnlyScrollingInThisMotion && !this.mIsQsBeingCovered) {
            if (isCancelOrUp) {
                this.mExpandHelper.onlyObserveMovements(false);
            }
            boolean wasExpandingBefore = this.mExpandingNotification;
            expandWantsIt = this.mExpandHelper.onTouchEvent(ev);
            if (this.mExpandedInThisMotion && !this.mExpandingNotification && wasExpandingBefore && !this.mDisallowScrollingInThisMotion) {
                dispatchDownEventToScroller(ev);
            }
        }
        boolean scrollerWantsIt = false;
        if (this.mIsExpanded && !this.mSwipingInProgress && !this.mExpandingNotification && !this.mDisallowScrollingInThisMotion) {
            scrollerWantsIt = onScrollTouch(ev);
        }
        boolean horizontalSwipeWantsIt = false;
        if (!this.mIsBeingDragged && !this.mExpandingNotification && !this.mExpandedInThisMotion && !this.mOnlyScrollingInThisMotion && !this.mDisallowDismissInThisMotion) {
            horizontalSwipeWantsIt = this.mSwipeHelper.onTouchEvent(ev);
        }
        NotificationGuts guts = this.mStatusBar.getExposedGuts();
        if (guts != null && !isTouchInView(ev, guts) && (guts.getGutsContent() instanceof NotificationSnooze) && ((((NotificationSnooze) guts.getGutsContent()).isExpanded() && isCancelOrUp) || (!horizontalSwipeWantsIt && scrollerWantsIt))) {
            checkSnoozeLeavebehind();
        }
        if (ev.getActionMasked() == 1) {
            this.mCheckForLeavebehind = true;
        }
        if (horizontalSwipeWantsIt || scrollerWantsIt || expandWantsIt || super.onTouchEvent(ev)) {
            return true;
        }
        return false;
    }

    private void dispatchDownEventToScroller(MotionEvent ev) {
        MotionEvent downEvent = MotionEvent.obtain(ev);
        downEvent.setAction(0);
        onScrollTouch(downEvent);
        downEvent.recycle();
    }

    public boolean onGenericMotionEvent(MotionEvent event) {
        if (!isScrollingEnabled() || !this.mIsExpanded || this.mSwipingInProgress || this.mExpandingNotification || this.mDisallowScrollingInThisMotion) {
            return false;
        }
        if ((event.getSource() & 2) != 0 && event.getAction() == 8 && !this.mIsBeingDragged) {
            float vscroll = event.getAxisValue(9);
            if (vscroll != 0.0f) {
                int range = getScrollRange();
                int oldScrollY = this.mOwnScrollY;
                int newScrollY = oldScrollY - ((int) (getVerticalScrollFactor() * vscroll));
                if (newScrollY < 0) {
                    newScrollY = 0;
                } else if (newScrollY > range) {
                    newScrollY = range;
                }
                if (newScrollY != oldScrollY) {
                    setOwnScrollY(newScrollY);
                    return true;
                }
            }
        }
        return super.onGenericMotionEvent(event);
    }

    private boolean onScrollTouch(MotionEvent ev) {
        float scrollAmount;
        MotionEvent motionEvent = ev;
        if (!isScrollingEnabled()) {
            return false;
        }
        if (isInsideQsContainer(ev) && !this.mIsBeingDragged) {
            return false;
        }
        if (NotificationUtil.isFoldAnimating()) {
            return true;
        }
        this.mForcedScroll = null;
        initVelocityTrackerIfNotExists();
        this.mVelocityTracker.addMovement(motionEvent);
        switch (ev.getAction() & 255) {
            case 0:
                if (getChildCount() != 0 && (isInContentBounds(ev) || this.mIsQsCovered)) {
                    setIsBeingDragged(!this.mScroller.isFinished());
                    if (!this.mScroller.isFinished()) {
                        this.mScroller.forceFinished(true);
                    }
                    this.mLastMotionY = (int) ev.getY();
                    this.mDownX = (int) ev.getX();
                    this.mActivePointerId = motionEvent.getPointerId(0);
                    break;
                } else {
                    return false;
                }
            case 1:
                VelocityTracker velocityTracker = this.mVelocityTracker;
                velocityTracker.computeCurrentVelocity(1000, (float) this.mMaximumVelocity);
                int initialVelocity = (int) velocityTracker.getYVelocity(this.mActivePointerId);
                if (!this.mIsQsBeingCovered) {
                    if (this.mIsBeingDragged) {
                        if (shouldOverScrollFling(initialVelocity)) {
                            onOverScrollFling(true, initialVelocity);
                        } else if (getChildCount() > 0) {
                            if (Math.abs(initialVelocity) > this.mMinimumVelocity) {
                                if (getCurrentOverScrollAmount(true) == 0.0f || initialVelocity > 0) {
                                    fling(-initialVelocity);
                                } else {
                                    onOverScrollFling(false, initialVelocity);
                                }
                            } else if (this.mScroller.springBack(this.mScrollX, this.mOwnScrollY, 0, 0, 0, getScrollRange())) {
                                animateScroll();
                            }
                        }
                        this.mActivePointerId = -1;
                        endDrag();
                        break;
                    }
                } else {
                    endQsBeingCoveredMotion(initialVelocity);
                    this.mActivePointerId = -1;
                    endDrag();
                    break;
                }
                break;
            case 2:
                int activePointerIndex = motionEvent.findPointerIndex(this.mActivePointerId);
                if (activePointerIndex != -1) {
                    int y = (int) motionEvent.getY(activePointerIndex);
                    int deltaY = this.mLastMotionY - y;
                    int xDiff = Math.abs(((int) motionEvent.getX(activePointerIndex)) - this.mDownX);
                    int yDiff = Math.abs(deltaY);
                    if (!this.mIsBeingDragged && yDiff > this.mTouchSlop && yDiff > xDiff) {
                        setIsBeingDragged(true);
                        if ((isInContentBounds(ev) || this.mIsQsCovered) && isScrolledToTop() && ((deltaY > 0 && canScrollUp() && !isQsCovered()) || (deltaY < 0 && isQsCovered()))) {
                            this.mIsQsBeingCovered = true;
                            this.mIsQsCovered = false;
                        }
                        deltaY = deltaY > 0 ? deltaY - this.mTouchSlop : deltaY + this.mTouchSlop;
                    }
                    if (!this.mIsQsBeingCovered) {
                        if (this.mIsBeingDragged) {
                            this.mLastMotionY = y;
                            int range = getScrollRange();
                            if (this.mExpandedInThisMotion) {
                                range = Math.min(range, this.mMaxScrollAfterExpand);
                            }
                            if (deltaY < 0) {
                                scrollAmount = overScrollDown(deltaY);
                            } else {
                                scrollAmount = overScrollUp(deltaY, range);
                            }
                            if (scrollAmount != 0.0f) {
                                customOverScrollBy((int) scrollAmount, this.mOwnScrollY, range, getHeight() / 2);
                                checkSnoozeLeavebehind();
                                break;
                            }
                        }
                    } else {
                        if (this.mQsBeingCoveredAnimator != null) {
                            this.mQsBeingCoveredAnimator.cancel();
                            this.mQsBeingCoveredAnimator = null;
                            this.mIsQsBeingCovered = true;
                        }
                        this.mLastMotionY = y;
                        notifyTopPaddingUpdateListener(this.mTopPadding - deltaY);
                        break;
                    }
                } else {
                    Log.e("StackScroller", "Invalid pointerId=" + this.mActivePointerId + " in onTouchEvent");
                    break;
                }
                break;
            case 3:
                if (this.mIsQsBeingCovered == 0) {
                    if (this.mIsBeingDragged && getChildCount() > 0) {
                        if (this.mScroller.springBack(this.mScrollX, this.mOwnScrollY, 0, 0, 0, getScrollRange())) {
                            animateScroll();
                        }
                        this.mActivePointerId = -1;
                        endDrag();
                        break;
                    }
                } else {
                    endQsBeingCoveredMotion(0);
                    this.mActivePointerId = -1;
                    endDrag();
                    break;
                }
            case 5:
                int index = ev.getActionIndex();
                this.mLastMotionY = (int) motionEvent.getY(index);
                this.mDownX = (int) motionEvent.getX(index);
                this.mActivePointerId = motionEvent.getPointerId(index);
                break;
            case 6:
                onSecondaryPointerUp(ev);
                int index2 = motionEvent.findPointerIndex(this.mActivePointerId);
                if (index2 == -1) {
                    Log.e("StackScroller", "Invalid pointerId= " + this.mActivePointerId + " in onTouchScroll:ACTION_POINTER_UP");
                    break;
                } else {
                    this.mLastMotionY = (int) motionEvent.getY(index2);
                    this.mDownX = (int) motionEvent.getX(index2);
                    break;
                }
        }
        return true;
    }

    /* access modifiers changed from: protected */
    public boolean isInsideQsContainer(MotionEvent ev) {
        return ev.getY() < ((float) this.mQs.getView().getBottom());
    }

    private void onOverScrollFling(boolean open, int initialVelocity) {
        if (this.mOverscrollTopChangedListener != null) {
            this.mOverscrollTopChangedListener.flingTopOverscroll((float) initialVelocity, open);
        }
        this.mDontReportNextOverScroll = true;
        setOverScrollAmount(0.0f, true, false);
    }

    private float overScrollUp(int deltaY, int range) {
        int deltaY2 = Math.max(deltaY, 0);
        float currentTopAmount = getCurrentOverScrollAmount(true);
        float newTopAmount = currentTopAmount - ((float) deltaY2);
        float f = 0.0f;
        if (currentTopAmount > 0.0f) {
            setOverScrollAmount(newTopAmount, true, false);
        }
        if (newTopAmount < 0.0f) {
            f = -newTopAmount;
        }
        float scrollAmount = f;
        float newScrollY = ((float) this.mOwnScrollY) + scrollAmount;
        if (newScrollY <= ((float) range)) {
            return scrollAmount;
        }
        if (!this.mExpandedInThisMotion) {
            setOverScrolledPixels((getCurrentOverScrolledPixels(false) + newScrollY) - ((float) range), false, false);
        }
        setOwnScrollY(range);
        return 0.0f;
    }

    private float overScrollDown(int deltaY) {
        int deltaY2 = Math.min(deltaY, 0);
        float currentBottomAmount = getCurrentOverScrollAmount(false);
        float newBottomAmount = ((float) deltaY2) + currentBottomAmount;
        if (currentBottomAmount > 0.0f) {
            setOverScrollAmount(newBottomAmount, false, false);
        }
        float scrollAmount = newBottomAmount < 0.0f ? newBottomAmount : 0.0f;
        float newScrollY = ((float) this.mOwnScrollY) + scrollAmount;
        if (newScrollY >= 0.0f) {
            return scrollAmount;
        }
        setOverScrolledPixels(getCurrentOverScrolledPixels(true) - newScrollY, true, false);
        setOwnScrollY(0);
        return 0.0f;
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        int pointerIndex = (ev.getAction() & 65280) >> 8;
        if (ev.getPointerId(pointerIndex) == this.mActivePointerId) {
            int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            this.mLastMotionY = (int) ev.getY(newPointerIndex);
            this.mActivePointerId = ev.getPointerId(newPointerIndex);
            if (this.mVelocityTracker != null) {
                this.mVelocityTracker.clear();
            }
        }
    }

    private void initVelocityTrackerIfNotExists() {
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        }
    }

    private void recycleVelocityTracker() {
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.recycle();
            this.mVelocityTracker = null;
        }
    }

    private void initOrResetVelocityTracker() {
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        } else {
            this.mVelocityTracker.clear();
        }
    }

    public void setFinishScrollingCallback(Runnable runnable) {
        this.mFinishScrollingCallback = runnable;
    }

    /* access modifiers changed from: private */
    public void animateScroll() {
        if (this.mScroller.computeScrollOffset()) {
            int oldY = this.mOwnScrollY;
            int y = this.mScroller.getCurrY();
            if (oldY != y) {
                int range = getScrollRange();
                if ((y < 0 && oldY >= 0) || (y > range && oldY <= range)) {
                    float currVelocity = this.mScroller.getCurrVelocity();
                    if (currVelocity >= ((float) this.mMinimumVelocity)) {
                        this.mMaxOverScroll = (Math.abs(currVelocity) / 1000.0f) * ((float) this.mOverflingDistance);
                    }
                }
                if (this.mDontClampNextScroll) {
                    range = Math.max(range, oldY);
                }
                customOverScrollBy(y - oldY, oldY, range, (int) this.mMaxOverScroll);
            }
            postOnAnimation(this.mAnimateScroll);
            return;
        }
        this.mDontClampNextScroll = false;
        if (this.mFinishScrollingCallback != null) {
            this.mFinishScrollingCallback.run();
        }
    }

    private boolean customOverScrollBy(int deltaY, int scrollY, int scrollRangeY, int maxOverScrollY) {
        int newScrollY = scrollY + deltaY;
        int top = -maxOverScrollY;
        int bottom = maxOverScrollY + scrollRangeY;
        boolean clampedY = false;
        if (newScrollY > bottom) {
            newScrollY = bottom;
            clampedY = true;
        } else if (newScrollY < top) {
            newScrollY = top;
            clampedY = true;
        }
        onCustomOverScrolled(newScrollY, clampedY);
        return clampedY;
    }

    public void setOverScrolledPixels(float numPixels, boolean onTop, boolean animate) {
        setOverScrollAmount(getRubberBandFactor(onTop) * numPixels, onTop, animate, true);
    }

    public void setOverScrollAmount(float amount, boolean onTop, boolean animate) {
        setOverScrollAmount(amount, onTop, animate, true);
    }

    public void setOverScrollAmount(float amount, boolean onTop, boolean animate, boolean cancelAnimators) {
        setOverScrollAmount(amount, onTop, animate, cancelAnimators, isRubberbanded(onTop));
    }

    public void setOverScrollAmount(float amount, boolean onTop, boolean animate, boolean cancelAnimators, boolean isRubberbanded) {
        if (cancelAnimators) {
            this.mStateAnimator.cancelOverScrollAnimators(onTop);
        }
        setOverScrollAmountInternal(amount, onTop, animate, isRubberbanded);
    }

    private void setOverScrollAmountInternal(float amount, boolean onTop, boolean animate, boolean isRubberbanded) {
        float amount2 = Math.max(0.0f, amount);
        if (animate) {
            this.mStateAnimator.animateOverScrollToAmount(amount2, onTop, isRubberbanded);
            return;
        }
        setOverScrolledPixels(amount2 / getRubberBandFactor(onTop), onTop);
        this.mAmbientState.setOverScrollAmount(amount2, onTop);
        if (onTop) {
            notifyOverscrollTopListener(amount2, isRubberbanded);
        }
        requestChildrenUpdate();
    }

    private void notifyOverscrollTopListener(float amount, boolean isRubberbanded) {
        this.mExpandHelper.onlyObserveMovements(amount > 1.0f);
        if (this.mDontReportNextOverScroll) {
            this.mDontReportNextOverScroll = false;
            return;
        }
        if (this.mOverscrollTopChangedListener != null) {
            this.mOverscrollTopChangedListener.onOverscrollTopChanged(amount, isRubberbanded);
        }
    }

    public void setOverscrollTopChangedListener(OnOverscrollTopChangedListener overscrollTopChangedListener) {
        this.mOverscrollTopChangedListener = overscrollTopChangedListener;
    }

    public float getCurrentOverScrollAmount(boolean top) {
        return this.mAmbientState.getOverScrollAmount(top);
    }

    public float getCurrentOverScrolledPixels(boolean top) {
        return top ? this.mOverScrolledTopPixels : this.mOverScrolledBottomPixels;
    }

    private void setOverScrolledPixels(float amount, boolean onTop) {
        if (onTop) {
            this.mOverScrolledTopPixels = amount;
        } else {
            this.mOverScrolledBottomPixels = amount;
        }
    }

    private void onCustomOverScrolled(int scrollY, boolean clampedY) {
        if (!this.mScroller.isFinished()) {
            setOwnScrollY(scrollY);
            if (clampedY) {
                springBack();
                return;
            }
            float overScrollTop = getCurrentOverScrollAmount(true);
            if (this.mOwnScrollY < 0) {
                notifyOverscrollTopListener((float) (-this.mOwnScrollY), isRubberbanded(true));
            } else {
                notifyOverscrollTopListener(overScrollTop, isRubberbanded(true));
            }
        } else {
            setOwnScrollY(scrollY);
        }
    }

    private void springBack() {
        float newAmount;
        boolean onTop;
        int scrollRange = getScrollRange();
        boolean overScrolledTop = this.mOwnScrollY <= 0;
        boolean overScrolledBottom = this.mOwnScrollY >= scrollRange;
        if (overScrolledTop || overScrolledBottom) {
            if (overScrolledTop) {
                onTop = true;
                newAmount = (float) (-this.mOwnScrollY);
                setOwnScrollY(0);
                this.mDontReportNextOverScroll = true;
            } else {
                onTop = false;
                newAmount = (float) (this.mOwnScrollY - scrollRange);
                setOwnScrollY(scrollRange);
            }
            setOverScrollAmount(newAmount, onTop, false);
            setOverScrollAmount(0.0f, onTop, true);
            this.mScroller.forceFinished(true);
        }
    }

    /* access modifiers changed from: private */
    public int getScrollRange() {
        int scrollRange = Math.max(0, (this.mContentHeight - this.mMaxLayoutHeight) + (isQsCovered() ? this.mExtraBottomRangeQsCovered : this.mExtraBottomRange));
        int imeInset = getImeInset();
        return scrollRange + Math.min(imeInset, Math.max(0, this.mContentHeight - (getHeight() - imeInset)));
    }

    private int getImeInset() {
        return Math.max(0, this.mBottomInset - (getRootView().getHeight() - getHeight()));
    }

    public ExpandableView getFirstChildNotGone() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != 8 && child != this.mShelf) {
                return (ExpandableView) child;
            }
        }
        return null;
    }

    private View getFirstChildBelowTranlsationY(float translationY, boolean ignoreChildren) {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != 8) {
                float rowTranslation = child.getTranslationY();
                if (rowTranslation >= translationY) {
                    return child;
                }
                if (!ignoreChildren && (child instanceof ExpandableNotificationRow)) {
                    ExpandableNotificationRow row = (ExpandableNotificationRow) child;
                    if (row.isSummaryWithChildren() && row.areChildrenExpanded()) {
                        List<ExpandableNotificationRow> notificationChildren = row.getNotificationChildren();
                        for (int childIndex = 0; childIndex < notificationChildren.size(); childIndex++) {
                            ExpandableNotificationRow rowChild = notificationChildren.get(childIndex);
                            if (rowChild.getTranslationY() + rowTranslation >= translationY) {
                                return rowChild;
                            }
                        }
                        continue;
                    }
                }
            }
        }
        return null;
    }

    public int getNotGoneChildCount() {
        int childCount = getChildCount();
        int count = 0;
        for (int i = 0; i < childCount; i++) {
            ExpandableView child = (ExpandableView) getChildAt(i);
            if (!(child.getVisibility() == 8 || child.willBeGone() || child == this.mShelf)) {
                count++;
            }
        }
        return count;
    }

    public int getContentHeight() {
        return this.mContentHeight;
    }

    /* access modifiers changed from: private */
    public void updateContentHeight() {
        float padding;
        float previousPaddingRequest = (float) this.mPaddingBetweenElements;
        float previousPaddingAmount = 0.0f;
        int numShownItems = 0;
        boolean finish = false;
        int maxDisplayedNotifications = this.mAmbientState.isDark() ? hasPulsingNotifications() ? 1 : 0 : this.mMaxDisplayedNotifications;
        int height = 0;
        for (int i = 0; i < getChildCount(); i++) {
            ExpandableView expandableView = (ExpandableView) getChildAt(i);
            if (expandableView.getVisibility() != 8 && !expandableView.hasNoContentHeight()) {
                boolean limitReached = maxDisplayedNotifications != -1 && numShownItems >= maxDisplayedNotifications;
                boolean notificationOnAmbientThatIsNotPulsing = this.mAmbientState.isDark() && hasPulsingNotifications() && (expandableView instanceof ExpandableNotificationRow) && !isPulsing(((ExpandableNotificationRow) expandableView).getEntry());
                if (limitReached || notificationOnAmbientThatIsNotPulsing) {
                    expandableView = this.mShelf;
                    finish = true;
                }
                float increasedPaddingAmount = expandableView.getIncreasedPaddingAmount();
                if (increasedPaddingAmount >= 0.0f) {
                    padding = (float) ((int) NotificationUtils.interpolate(previousPaddingRequest, (float) this.mIncreasedPaddingBetweenElements, increasedPaddingAmount));
                    previousPaddingRequest = (float) ((int) NotificationUtils.interpolate((float) this.mPaddingBetweenElements, (float) this.mIncreasedPaddingBetweenElements, increasedPaddingAmount));
                } else {
                    int ownPadding = (int) NotificationUtils.interpolate(0.0f, (float) this.mPaddingBetweenElements, 1.0f + increasedPaddingAmount);
                    if (previousPaddingAmount > 0.0f) {
                        padding = (float) ((int) NotificationUtils.interpolate((float) ownPadding, (float) this.mIncreasedPaddingBetweenElements, previousPaddingAmount));
                    } else {
                        padding = (float) ownPadding;
                    }
                    previousPaddingRequest = (float) ownPadding;
                }
                if (height != 0) {
                    height = (int) (((float) height) + padding);
                }
                previousPaddingAmount = increasedPaddingAmount;
                height += expandableView.getIntrinsicHeight();
                numShownItems++;
                if (finish) {
                    break;
                }
            }
        }
        this.mContentHeight = this.mTopPadding + height;
        updateScrollability();
        this.mAmbientState.setLayoutMaxHeight(this.mContentHeight);
    }

    private boolean isPulsing(NotificationData.Entry entry) {
        for (HeadsUpManager.HeadsUpEntry e : this.mPulsing) {
            if (e.entry == entry) {
                return true;
            }
        }
        return false;
    }

    public boolean hasPulsingNotifications() {
        return this.mPulsing != null;
    }

    private void updateScrollability() {
        boolean scrollable = getScrollRange() > 0;
        if (scrollable != this.mScrollable) {
            this.mScrollable = scrollable;
            setFocusable(scrollable);
            updateForwardAndBackwardScrollability();
        }
    }

    private void updateForwardAndBackwardScrollability() {
        boolean changed = false;
        boolean forwardScrollable = this.mScrollable && this.mOwnScrollY < getScrollRange();
        boolean backwardsScrollable = this.mScrollable && this.mOwnScrollY > 0;
        if (!(forwardScrollable == this.mForwardScrollable && backwardsScrollable == this.mBackwardScrollable)) {
            changed = true;
        }
        this.mForwardScrollable = forwardScrollable;
        this.mBackwardScrollable = backwardsScrollable;
        if (changed) {
            sendAccessibilityEvent(2048);
        }
    }

    private void updateBackground() {
        if (this.mShouldDrawNotificationBackground && !this.mAmbientState.isDark()) {
            updateBackgroundBounds();
            if (!this.mCurrentBounds.equals(this.mBackgroundBounds)) {
                boolean animate = this.mAnimateNextBackgroundTop || this.mAnimateNextBackgroundBottom || areBoundsAnimating();
                if (!isExpanded()) {
                    abortBackgroundAnimators();
                    animate = false;
                }
                if (animate) {
                    startBackgroundAnimation();
                } else {
                    this.mCurrentBounds.set(this.mBackgroundBounds);
                    applyCurrentBackgroundBounds();
                }
            } else {
                abortBackgroundAnimators();
            }
            this.mAnimateNextBackgroundBottom = false;
            this.mAnimateNextBackgroundTop = false;
        }
    }

    private void abortBackgroundAnimators() {
        if (this.mBottomAnimator != null) {
            this.mBottomAnimator.cancel();
        }
        if (this.mTopAnimator != null) {
            this.mTopAnimator.cancel();
        }
    }

    private boolean areBoundsAnimating() {
        return (this.mBottomAnimator == null && this.mTopAnimator == null) ? false : true;
    }

    private void startBackgroundAnimation() {
        this.mCurrentBounds.left = this.mBackgroundBounds.left;
        this.mCurrentBounds.right = this.mBackgroundBounds.right;
        startBottomAnimation();
        startTopAnimation();
    }

    private void startTopAnimation() {
        int previousEndValue = this.mEndAnimationRect.top;
        int newEndValue = this.mBackgroundBounds.top;
        ObjectAnimator previousAnimator = this.mTopAnimator;
        if (previousAnimator != null && previousEndValue == newEndValue) {
            return;
        }
        if (this.mAnimateNextBackgroundTop) {
            if (previousAnimator != null) {
                previousAnimator.cancel();
            }
            ObjectAnimator animator = ObjectAnimator.ofInt(this, "backgroundTop", new int[]{this.mCurrentBounds.top, newEndValue});
            animator.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
            animator.setDuration(360);
            animator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    NotificationStackScrollLayout.this.mStartAnimationRect.top = -1;
                    NotificationStackScrollLayout.this.mEndAnimationRect.top = -1;
                    ObjectAnimator unused = NotificationStackScrollLayout.this.mTopAnimator = null;
                }
            });
            animator.start();
            this.mStartAnimationRect.top = this.mCurrentBounds.top;
            this.mEndAnimationRect.top = newEndValue;
            this.mTopAnimator = animator;
        } else if (previousAnimator != null) {
            int previousStartValue = this.mStartAnimationRect.top;
            previousAnimator.getValues()[0].setIntValues(new int[]{previousStartValue, newEndValue});
            this.mStartAnimationRect.top = previousStartValue;
            this.mEndAnimationRect.top = newEndValue;
            previousAnimator.setCurrentPlayTime(previousAnimator.getCurrentPlayTime());
        } else {
            setBackgroundTop(newEndValue);
        }
    }

    private void startBottomAnimation() {
        int previousStartValue = this.mStartAnimationRect.bottom;
        int previousEndValue = this.mEndAnimationRect.bottom;
        int newEndValue = this.mBackgroundBounds.bottom;
        ObjectAnimator previousAnimator = this.mBottomAnimator;
        if (previousAnimator != null && previousEndValue == newEndValue) {
            return;
        }
        if (this.mAnimateNextBackgroundBottom) {
            if (previousAnimator != null) {
                previousAnimator.cancel();
            }
            ObjectAnimator animator = ObjectAnimator.ofInt(this, "backgroundBottom", new int[]{this.mCurrentBounds.bottom, newEndValue});
            animator.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
            animator.setDuration(360);
            animator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    NotificationStackScrollLayout.this.mStartAnimationRect.bottom = -1;
                    NotificationStackScrollLayout.this.mEndAnimationRect.bottom = -1;
                    ObjectAnimator unused = NotificationStackScrollLayout.this.mBottomAnimator = null;
                }
            });
            animator.start();
            this.mStartAnimationRect.bottom = this.mCurrentBounds.bottom;
            this.mEndAnimationRect.bottom = newEndValue;
            this.mBottomAnimator = animator;
        } else if (previousAnimator != null) {
            previousAnimator.getValues()[0].setIntValues(new int[]{previousStartValue, newEndValue});
            this.mStartAnimationRect.bottom = previousStartValue;
            this.mEndAnimationRect.bottom = newEndValue;
            previousAnimator.setCurrentPlayTime(previousAnimator.getCurrentPlayTime());
        } else {
            setBackgroundBottom(newEndValue);
        }
    }

    private void setBackgroundTop(int top) {
        this.mCurrentBounds.top = top;
        applyCurrentBackgroundBounds();
    }

    public void setBackgroundBottom(int bottom) {
        this.mCurrentBounds.bottom = bottom;
        applyCurrentBackgroundBounds();
    }

    private void applyCurrentBackgroundBounds() {
        Rect rect;
        if (this.mShouldDrawNotificationBackground) {
            ScrimController scrimController = this.mScrimController;
            if (this.mFadingOut || this.mParentNotFullyVisible || this.mAmbientState.isDark() || this.mIsClipped) {
                rect = null;
            } else {
                rect = this.mCurrentBounds;
            }
            scrimController.setExcludedBackgroundArea(rect);
            invalidate();
        }
    }

    private void updateBackgroundBounds() {
        ExpandableView lastView;
        int bottom;
        int top;
        int finalTranslationY;
        if (this.mAmbientState.isPanelFullWidth()) {
            this.mBackgroundBounds.left = 0;
            this.mBackgroundBounds.right = getWidth();
        } else {
            getLocationInWindow(this.mTempInt2);
            this.mBackgroundBounds.left = this.mTempInt2[0];
            this.mBackgroundBounds.right = this.mTempInt2[0] + getWidth();
        }
        if (!this.mIsExpanded) {
            this.mBackgroundBounds.top = this.mTopPadding;
            this.mBackgroundBounds.bottom = this.mTopPadding;
            return;
        }
        ExpandableView firstView = this.mFirstVisibleBackgroundChild;
        int top2 = 0;
        if (firstView != null) {
            int finalTranslationY2 = (int) Math.ceil((double) ViewState.getFinalTranslationY(firstView));
            if (this.mAnimateNextBackgroundTop || ((this.mTopAnimator == null && this.mCurrentBounds.top == finalTranslationY2) || (this.mTopAnimator != null && this.mEndAnimationRect.top == finalTranslationY2))) {
                top2 = finalTranslationY2;
            } else {
                top2 = (int) Math.ceil((double) firstView.getTranslationY());
            }
        }
        if (this.mShelf.hasItemsInStableShelf()) {
            lastView = this.mShelf;
        } else {
            lastView = this.mLastVisibleBackgroundChild;
        }
        if (lastView != null) {
            if (lastView == this.mShelf) {
                finalTranslationY = (int) this.mShelf.getTranslationY();
            } else {
                finalTranslationY = (int) ViewState.getFinalTranslationY(lastView);
            }
            int finalBottom = (finalTranslationY + ExpandableViewState.getFinalActualHeight(lastView)) - lastView.getClipBottomAmount();
            if (this.mAnimateNextBackgroundBottom || ((this.mBottomAnimator == null && this.mCurrentBounds.bottom == finalBottom) || (this.mBottomAnimator != null && this.mEndAnimationRect.bottom == finalBottom))) {
                bottom = finalBottom;
            } else {
                bottom = (int) ((lastView.getTranslationY() + ((float) lastView.getActualHeight())) - ((float) lastView.getClipBottomAmount()));
            }
        } else {
            top2 = this.mTopPadding;
            bottom = top2;
        }
        if (this.mStatusBarState != 1) {
            top = (int) Math.max(((float) this.mTopPadding) + this.mStackTranslation, (float) top2);
        } else {
            top = Math.max(0, top2);
        }
        this.mBackgroundBounds.top = top;
        this.mBackgroundBounds.bottom = Math.max(bottom, top);
    }

    private ExpandableView getLastChildWithBackground() {
        for (int i = getChildCount() - 1; i >= 0; i--) {
            if (getChildAt(i) instanceof ExpandableView) {
                ExpandableView child = (ExpandableView) getChildAt(i);
                if (!(child.getVisibility() == 8 || child.getViewType() == 1 || child.getViewType() == 2)) {
                    return child;
                }
            }
        }
        return null;
    }

    private ExpandableView getFirstChildWithBackground() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            if (getChildAt(i) instanceof ExpandableView) {
                ExpandableView child = (ExpandableView) getChildAt(i);
                if (!(child.getVisibility() == 8 || child.getViewType() == 1 || child.getViewType() == 2)) {
                    return child;
                }
            }
        }
        return null;
    }

    /* access modifiers changed from: protected */
    public void fling(int velocityY) {
        if (getChildCount() > 0) {
            int scrollRange = getScrollRange();
            float topAmount = getCurrentOverScrollAmount(true);
            int i = 0;
            float bottomAmount = getCurrentOverScrollAmount(false);
            if (velocityY < 0 && topAmount > 0.0f) {
                setOwnScrollY(this.mOwnScrollY - ((int) topAmount));
                this.mDontReportNextOverScroll = true;
                setOverScrollAmount(0.0f, true, false);
                this.mMaxOverScroll = ((((float) Math.abs(velocityY)) / 1000.0f) * getRubberBandFactor(true) * ((float) this.mOverflingDistance)) + topAmount;
            } else if (velocityY <= 0 || bottomAmount <= 0.0f) {
                this.mMaxOverScroll = 0.0f;
            } else {
                setOwnScrollY((int) (((float) this.mOwnScrollY) + bottomAmount));
                setOverScrollAmount(0.0f, false, false);
                this.mMaxOverScroll = ((((float) Math.abs(velocityY)) / 1000.0f) * getRubberBandFactor(false) * ((float) this.mOverflingDistance)) + bottomAmount;
            }
            int minScrollY = Math.max(0, scrollRange);
            if (this.mExpandedInThisMotion) {
                minScrollY = Math.min(minScrollY, this.mMaxScrollAfterExpand);
            }
            int minScrollY2 = minScrollY;
            OverScroller overScroller = this.mScroller;
            int i2 = this.mScrollX;
            int i3 = this.mOwnScrollY;
            if (!this.mExpandedInThisMotion || this.mOwnScrollY < 0) {
                i = 1073741823;
            }
            overScroller.fling(i2, i3, 1, velocityY, 0, 0, 0, minScrollY2, 0, i);
            animateScroll();
        }
    }

    private boolean shouldOverScrollFling(int initialVelocity) {
        float topOverScroll = getCurrentOverScrollAmount(true);
        if (!this.mScrolledToTopOnFirstDown || this.mExpandedInThisMotion || topOverScroll <= this.mMinTopOverScrollToEscape || initialVelocity <= 0) {
            return false;
        }
        return true;
    }

    public void updateTopPadding(float qsHeight, boolean animate, boolean ignoreIntrinsicPadding) {
        int topPadding = (int) qsHeight;
        int minStackHeight = getLayoutMinHeight();
        if (topPadding + minStackHeight > getHeight()) {
            this.mTopPaddingOverflow = (float) ((topPadding + minStackHeight) - getHeight());
        } else {
            this.mTopPaddingOverflow = 0.0f;
        }
        setTopPadding(ignoreIntrinsicPadding ? topPadding : clampPadding(topPadding), animate);
        setExpandedHeight(this.mExpandedHeight);
    }

    public int getLayoutMinHeight() {
        return this.mShelf.getIntrinsicHeight();
    }

    public float getTopPaddingOverflow() {
        return this.mTopPaddingOverflow;
    }

    public int getPeekHeight() {
        int firstChildMinHeight;
        ExpandableView firstChild = getFirstChildNotGone();
        if (firstChild != null) {
            firstChildMinHeight = firstChild.getCollapsedHeight();
        } else {
            firstChildMinHeight = this.mCollapsedSize;
        }
        int shelfHeight = 0;
        if (this.mLastVisibleBackgroundChild != null) {
            shelfHeight = this.mShelf.getIntrinsicHeight();
        }
        return this.mIntrinsicPadding + firstChildMinHeight + shelfHeight;
    }

    private int clampPadding(int desiredPadding) {
        return Math.max(desiredPadding, this.mIntrinsicPadding);
    }

    private float getRubberBandFactor(boolean onTop) {
        if (!onTop) {
            return 0.35f;
        }
        if (this.mExpandedInThisMotion) {
            return 0.15f;
        }
        if (this.mIsExpansionChanging || this.mPanelTracking) {
            return 0.21f;
        }
        if (this.mScrolledToTopOnFirstDown) {
            return 1.0f;
        }
        return 0.35f;
    }

    private boolean isRubberbanded(boolean onTop) {
        return !onTop || this.mExpandedInThisMotion || this.mIsExpansionChanging || this.mPanelTracking || !this.mScrolledToTopOnFirstDown;
    }

    private void endDrag() {
        setIsBeingDragged(false);
        recycleVelocityTracker();
        if (getCurrentOverScrollAmount(true) > 0.0f) {
            setOverScrollAmount(0.0f, true, true);
        }
        if (getCurrentOverScrollAmount(false) > 0.0f) {
            setOverScrollAmount(0.0f, false, true);
        }
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        MotionEvent motionEvent = ev;
        initDownStates(ev);
        handleEmptySpaceClick(ev);
        boolean expandWantsIt = false;
        if (!this.mSwipingInProgress && !this.mOnlyScrollingInThisMotion) {
            expandWantsIt = this.mExpandHelper.onInterceptTouchEvent(motionEvent);
        }
        boolean scrollWantsIt = false;
        if (!this.mSwipingInProgress && !this.mExpandingNotification) {
            scrollWantsIt = onInterceptTouchEventScroll(ev);
        }
        boolean swipeWantsIt = false;
        if (!this.mIsBeingDragged && !this.mExpandingNotification && !this.mExpandedInThisMotion && !this.mOnlyScrollingInThisMotion && !this.mDisallowDismissInThisMotion) {
            swipeWantsIt = this.mSwipeHelper.onInterceptTouchEvent(motionEvent);
        }
        boolean isUp = ev.getActionMasked() == 1;
        if (!isTouchInView(motionEvent, this.mStatusBar.getExposedGuts()) && isUp && !swipeWantsIt && !expandWantsIt && !scrollWantsIt) {
            this.mCheckForLeavebehind = false;
            this.mStatusBar.closeAndSaveGuts(true, false, false, -1, -1, false);
        }
        if (ev.getActionMasked() == 1) {
            this.mCheckForLeavebehind = true;
        }
        if (DEBUG) {
            Log.d("StackScroller", String.format("onInterceptTouchEvent swipeWantsIt=%b scrollWantsIt=%b expandWantsIt=%b", new Object[]{Boolean.valueOf(swipeWantsIt), Boolean.valueOf(scrollWantsIt), Boolean.valueOf(expandWantsIt)}));
        }
        if (swipeWantsIt || scrollWantsIt || expandWantsIt || super.onInterceptTouchEvent(ev)) {
            return true;
        }
        return false;
    }

    private void handleEmptySpaceClick(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case 1:
                if (this.mStatusBarState != 1 && this.mTouchIsClick && isBelowLastNotification(this.mInitialTouchX, this.mInitialTouchY)) {
                    this.mOnEmptySpaceClickListener.onEmptySpaceClicked(this.mInitialTouchX, this.mInitialTouchY);
                    return;
                }
                return;
            case 2:
                if (!this.mTouchIsClick) {
                    return;
                }
                if (Math.abs(ev.getY() - this.mInitialTouchY) > ((float) this.mTouchSlop) || Math.abs(ev.getX() - this.mInitialTouchX) > ((float) this.mTouchSlop)) {
                    this.mTouchIsClick = false;
                    return;
                }
                return;
            default:
                return;
        }
    }

    private void initDownStates(MotionEvent ev) {
        if (ev.getAction() == 0) {
            this.mExpandedInThisMotion = false;
            this.mOnlyScrollingInThisMotion = !this.mScroller.isFinished();
            this.mDisallowScrollingInThisMotion = false;
            this.mDisallowDismissInThisMotion = false;
            this.mTouchIsClick = true;
            this.mInitialTouchX = ev.getX();
            this.mInitialTouchY = ev.getY();
        }
    }

    public void setChildTransferInProgress(boolean childTransferInProgress) {
        this.mChildTransferInProgress = childTransferInProgress;
    }

    public void onViewRemoved(View child) {
        super.onViewRemoved(child);
        if (!this.mChildTransferInProgress) {
            onViewRemovedInternal(child, this);
        }
    }

    public void cleanUpViewState(View child) {
        if (child == this.mTranslatingParentView) {
            this.mTranslatingParentView = null;
        }
        this.mCurrentStackScrollState.removeViewStateForView(child);
    }

    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        super.requestDisallowInterceptTouchEvent(disallowIntercept);
        if (disallowIntercept) {
            this.mSwipeHelper.removeLongPressCallback();
        }
    }

    private void onViewRemovedInternal(View child, ViewGroup container) {
        if (!this.mChangePositionInProgress) {
            ExpandableView expandableView = (ExpandableView) child;
            expandableView.setOnHeightChangedListener(null);
            this.mCurrentStackScrollState.removeViewStateForView(child);
            updateScrollStateForRemovedChild(expandableView);
            if (!generateRemoveAnimation(child)) {
                this.mSwipedOutViews.remove(child);
            } else if (!this.mSwipedOutViews.contains(child)) {
                container.getOverlay().add(child);
            } else if (Math.abs(expandableView.getTranslation()) != ((float) expandableView.getWidth())) {
                container.addTransientView(child, 0);
                expandableView.setTransientContainer(container);
            }
            updateAnimationState(false, child);
            expandableView.setClipTopAmount(0);
            focusNextViewIfFocused(child);
        }
    }

    private void focusNextViewIfFocused(View view) {
        float f;
        if (view instanceof ExpandableNotificationRow) {
            ExpandableNotificationRow row = (ExpandableNotificationRow) view;
            if (row.shouldRefocusOnDismiss()) {
                View nextView = row.getChildAfterViewWhenDismissed();
                if (nextView == null) {
                    View groupParentWhenDismissed = row.getGroupParentWhenDismissed();
                    if (groupParentWhenDismissed != null) {
                        f = groupParentWhenDismissed.getTranslationY();
                    } else {
                        f = view.getTranslationY();
                    }
                    nextView = getFirstChildBelowTranlsationY(f, true);
                }
                if (nextView != null) {
                    nextView.requestAccessibilityFocus();
                }
            }
        }
    }

    private boolean isChildInGroup(View child) {
        return (child instanceof ExpandableNotificationRow) && this.mGroupManager.isChildInGroupWithSummary(((ExpandableNotificationRow) child).getStatusBarNotification());
    }

    private boolean generateRemoveAnimation(View child) {
        if (child == this.mFoldFooterView || child == this.mFoldHeaderView) {
            return false;
        }
        if (removeRemovedChildFromHeadsUpChangeAnimations(child)) {
            this.mAddedHeadsUpChildren.remove(child);
            return false;
        } else if (isClickedHeadsUp(child)) {
            this.mClearOverlayViewsWhenFinished.add(child);
            return true;
        } else if (!this.mIsExpanded || !this.mAnimationsEnabled || isChildInInvisibleGroup(child)) {
            return false;
        } else {
            if (!this.mChildrenToAddAnimated.contains(child)) {
                this.mChildrenToRemoveAnimated.add(child);
                this.mNeedsAnimation = true;
                return true;
            }
            this.mChildrenToAddAnimated.remove(child);
            this.mFromMoreCardAdditions.remove(child);
            return false;
        }
    }

    private boolean isClickedHeadsUp(View child) {
        return HeadsUpManager.isClickedHeadsUpNotification(child);
    }

    private boolean removeRemovedChildFromHeadsUpChangeAnimations(View child) {
        boolean hasAddEvent = false;
        Iterator<Pair<ExpandableNotificationRow, Boolean>> it = this.mHeadsUpChangeAnimations.iterator();
        while (it.hasNext()) {
            Pair<ExpandableNotificationRow, Boolean> eventPair = it.next();
            ExpandableNotificationRow row = (ExpandableNotificationRow) eventPair.first;
            boolean isHeadsUp = ((Boolean) eventPair.second).booleanValue();
            if (child == row) {
                this.mTmpList.add(eventPair);
                hasAddEvent |= isHeadsUp;
            }
        }
        if (hasAddEvent) {
            this.mHeadsUpChangeAnimations.removeAll(this.mTmpList);
            ((ExpandableNotificationRow) child).setHeadsUpAnimatingAway(false);
        }
        this.mTmpList.clear();
        return hasAddEvent;
    }

    private boolean isChildInInvisibleGroup(View child) {
        boolean z = false;
        if (child instanceof ExpandableNotificationRow) {
            ExpandableNotificationRow row = (ExpandableNotificationRow) child;
            ExpandableNotificationRow groupSummary = this.mGroupManager.getGroupSummary((StatusBarNotification) row.getStatusBarNotification());
            if (!(groupSummary == null || groupSummary == row)) {
                if (row.getVisibility() == 4) {
                    z = true;
                }
                return z;
            }
        }
        return false;
    }

    private void updateScrollStateForRemovedChild(ExpandableView removedChild) {
        int padding;
        int startingPosition = getPositionInLinearLayout(removedChild);
        float increasedPaddingAmount = removedChild.getIncreasedPaddingAmount();
        if (increasedPaddingAmount >= 0.0f) {
            padding = (int) NotificationUtils.interpolate((float) this.mPaddingBetweenElements, (float) this.mIncreasedPaddingBetweenElements, increasedPaddingAmount);
        } else {
            padding = (int) NotificationUtils.interpolate(0.0f, (float) this.mPaddingBetweenElements, 1.0f + increasedPaddingAmount);
        }
        int childHeight = getIntrinsicHeight(removedChild) + padding;
        if (startingPosition + childHeight <= this.mOwnScrollY) {
            setOwnScrollY(this.mOwnScrollY - childHeight);
        } else if (startingPosition < this.mOwnScrollY) {
            setOwnScrollY(startingPosition);
        }
    }

    private int getIntrinsicHeight(View view) {
        if (view instanceof ExpandableView) {
            return ((ExpandableView) view).getIntrinsicHeight();
        }
        return view.getHeight();
    }

    private int getPositionInLinearLayout(View requestedView) {
        float padding;
        ExpandableNotificationRow childInGroup = null;
        ExpandableNotificationRow requestedRow = null;
        if (isChildInGroup(requestedView)) {
            childInGroup = (ExpandableNotificationRow) requestedView;
            ExpandableNotificationRow notificationParent = childInGroup.getNotificationParent();
            requestedRow = notificationParent;
            requestedView = notificationParent;
        }
        float previousPaddingAmount = 0.0f;
        float previousPaddingRequest = (float) this.mPaddingBetweenElements;
        int position = 0;
        for (int i = 0; i < getChildCount(); i++) {
            ExpandableView child = (ExpandableView) getChildAt(i);
            boolean notGone = child.getVisibility() != 8;
            if (notGone && !child.hasNoContentHeight()) {
                float increasedPaddingAmount = child.getIncreasedPaddingAmount();
                if (increasedPaddingAmount >= 0.0f) {
                    padding = (float) ((int) NotificationUtils.interpolate(previousPaddingRequest, (float) this.mIncreasedPaddingBetweenElements, increasedPaddingAmount));
                    previousPaddingRequest = (float) ((int) NotificationUtils.interpolate((float) this.mPaddingBetweenElements, (float) this.mIncreasedPaddingBetweenElements, increasedPaddingAmount));
                } else {
                    int ownPadding = (int) NotificationUtils.interpolate(0.0f, (float) this.mPaddingBetweenElements, 1.0f + increasedPaddingAmount);
                    if (previousPaddingAmount > 0.0f) {
                        padding = (float) ((int) NotificationUtils.interpolate((float) ownPadding, (float) this.mIncreasedPaddingBetweenElements, previousPaddingAmount));
                    } else {
                        padding = (float) ownPadding;
                    }
                    previousPaddingRequest = (float) ownPadding;
                }
                if (position != 0) {
                    position = (int) (((float) position) + padding);
                }
                previousPaddingAmount = increasedPaddingAmount;
            }
            if (child == requestedView) {
                if (requestedRow != null) {
                    position += requestedRow.getPositionOfChild(childInGroup);
                }
                return position;
            }
            if (notGone) {
                position += getIntrinsicHeight(child);
            }
        }
        return 0;
    }

    public void onViewAdded(View child) {
        super.onViewAdded(child);
        onViewAddedInternal(child);
    }

    private void updateFirstAndLastBackgroundViews() {
        ExpandableView firstChild = getFirstChildWithBackground();
        ExpandableView lastChild = getLastChildWithBackground();
        boolean z = false;
        if (!this.mAnimationsEnabled || !this.mIsExpanded) {
            this.mAnimateNextBackgroundTop = false;
            this.mAnimateNextBackgroundBottom = false;
        } else {
            this.mAnimateNextBackgroundTop = firstChild != this.mFirstVisibleBackgroundChild;
            if (lastChild != this.mLastVisibleBackgroundChild) {
                z = true;
            }
            this.mAnimateNextBackgroundBottom = z;
        }
        this.mFirstVisibleBackgroundChild = firstChild;
        this.mLastVisibleBackgroundChild = lastChild;
        this.mAmbientState.setLastVisibleBackgroundChild(lastChild);
    }

    private void onViewAddedInternal(View child) {
        updateHideSensitiveForChild(child);
        ((ExpandableView) child).setOnHeightChangedListener(this);
        generateAddAnimation(child, false);
        updateAnimationState(child);
        updateChronometerForChild(child);
    }

    private void updateHideSensitiveForChild(View child) {
        if (child instanceof ExpandableView) {
            ((ExpandableView) child).setHideSensitiveForIntrinsicHeight(this.mAmbientState.isHideSensitive());
        }
    }

    public void notifyGroupChildRemoved(View row, ViewGroup childrenContainer) {
        onViewRemovedInternal(row, childrenContainer);
    }

    public void notifyGroupChildAdded(View row) {
        onViewAddedInternal(row);
    }

    public void setAnimationsEnabled(boolean animationsEnabled) {
        this.mAnimationsEnabled = animationsEnabled;
        updateNotificationAnimationStates();
    }

    private void updateNotificationAnimationStates() {
        boolean running = this.mAnimationsEnabled || hasPulsingNotifications();
        this.mShelf.setAnimationsEnabled(running);
        int childCount = getChildCount();
        boolean running2 = running;
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            running2 &= this.mIsExpanded || isPinnedHeadsUp(child);
            updateAnimationState(running2, child);
        }
    }

    private void updateAnimationState(View child) {
        updateAnimationState((this.mAnimationsEnabled || hasPulsingNotifications()) && (this.mIsExpanded || isPinnedHeadsUp(child)), child);
    }

    private void updateAnimationState(boolean running, View child) {
        if (child instanceof ExpandableNotificationRow) {
            ((ExpandableNotificationRow) child).setIconAnimationRunning(running);
        }
    }

    public boolean isAddOrRemoveAnimationPending() {
        return this.mNeedsAnimation && (!this.mChildrenToAddAnimated.isEmpty() || !this.mChildrenToRemoveAnimated.isEmpty());
    }

    public void generateAddAnimation(View child, boolean fromMoreCard) {
        if (this.mIsExpanded && this.mAnimationsEnabled && !this.mChangePositionInProgress) {
            this.mChildrenToAddAnimated.add(child);
            if (fromMoreCard) {
                this.mFromMoreCardAdditions.add(child);
            }
            this.mNeedsAnimation = true;
        }
        if (isHeadsUp(child) && this.mAnimationsEnabled && !this.mChangePositionInProgress) {
            this.mAddedHeadsUpChildren.add(child);
            this.mChildrenToAddAnimated.remove(child);
        }
    }

    public void changeViewPosition(View child, int newIndex) {
        int currentIndex = indexOfChild(child);
        if (child != null && child.getParent() == this && currentIndex != newIndex) {
            this.mChangePositionInProgress = true;
            ((ExpandableView) child).setChangingPosition(true);
            removeView(child);
            addView(child, newIndex);
            ((ExpandableView) child).setChangingPosition(false);
            this.mChangePositionInProgress = false;
            if (this.mIsExpanded && this.mAnimationsEnabled && child.getVisibility() != 8) {
                this.mChildrenChangingPositions.add(child);
                this.mNeedsAnimation = true;
            }
        }
    }

    private void startAnimationToState() {
        if (this.mNeedsAnimation) {
            generateChildHierarchyEvents();
            this.mNeedsAnimation = false;
        }
        if (!this.mAnimationEvents.isEmpty() || isCurrentlyAnimating()) {
            setAnimationRunning(true);
            this.mStateAnimator.startAnimationForEvents(this.mAnimationEvents, this.mCurrentStackScrollState, this.mGoToFullShadeDelay);
            this.mAnimationEvents.clear();
            updateBackground();
            updateViewShadows();
        } else {
            applyCurrentState();
        }
        this.mGoToFullShadeDelay = 0;
    }

    private void generateChildHierarchyEvents() {
        generateHeadsUpAnimationEvents();
        generateChildRemovalEvents();
        generateChildAdditionEvents();
        generatePositionChangeEvents();
        generateSnapBackEvents();
        generateDragEvents();
        generateTopPaddingEvent();
        generateActivateEvent();
        generateDimmedEvent();
        generateHideSensitiveEvent();
        generateDarkEvent();
        generateGoToFullShadeEvent();
        generateViewResizeEvent();
        generateGroupExpansionEvent();
        generateChildPopDismissEvent();
        generateAnimateEverythingEvent();
    }

    private void generateChildPopDismissEvent() {
        Iterator<View> it = this.mPendingPopupChildren.iterator();
        while (it.hasNext()) {
            this.mAnimationEvents.add(new AnimationEvent(it.next(), 18));
        }
        Iterator<View> it2 = this.mPendingDismissChildren.iterator();
        while (it2.hasNext()) {
            this.mAnimationEvents.add(new AnimationEvent(it2.next(), 19));
        }
        this.mPendingPopupChildren.clear();
        this.mPendingDismissChildren.clear();
    }

    private void generateHeadsUpAnimationEvents() {
        int i;
        Iterator<Pair<ExpandableNotificationRow, Boolean>> it = this.mHeadsUpChangeAnimations.iterator();
        while (it.hasNext()) {
            Pair<ExpandableNotificationRow, Boolean> eventPair = it.next();
            ExpandableNotificationRow row = (ExpandableNotificationRow) eventPair.first;
            boolean isHeadsUp = ((Boolean) eventPair.second).booleanValue();
            int type = 17;
            boolean onBottom = false;
            boolean z = true;
            boolean pinnedAndClosed = row.isPinned() && !this.mIsExpanded;
            if (this.mIsExpanded || isHeadsUp) {
                ExpandableViewState viewState = this.mCurrentStackScrollState.getViewStateForView(row);
                if (viewState != null) {
                    if (isHeadsUp && (this.mAddedHeadsUpChildren.contains(row) || pinnedAndClosed)) {
                        if (pinnedAndClosed || shouldHunAppearFromBottom(viewState)) {
                            type = 14;
                        } else {
                            type = 0;
                        }
                        if (pinnedAndClosed) {
                            z = false;
                        }
                        onBottom = z;
                    }
                }
            } else {
                if (row.wasJustClicked()) {
                    i = 16;
                } else {
                    i = 15;
                }
                type = i;
                if (row.isChildInGroup()) {
                    row.setHeadsUpAnimatingAway(false);
                }
            }
            AnimationEvent event = new AnimationEvent(row, type);
            event.headsUpFromBottom = onBottom;
            this.mAnimationEvents.add(event);
        }
        this.mHeadsUpChangeAnimations.clear();
        this.mAddedHeadsUpChildren.clear();
    }

    private boolean shouldHunAppearFromBottom(ExpandableViewState viewState) {
        if (viewState.yTranslation + ((float) viewState.height) < this.mAmbientState.getMaxHeadsUpTranslation()) {
            return false;
        }
        return true;
    }

    private void generateGroupExpansionEvent() {
        if (this.mExpandedGroupView != null) {
            this.mAnimationEvents.add(new AnimationEvent(this.mExpandedGroupView, 13));
            this.mExpandedGroupView = null;
        }
    }

    private void generateViewResizeEvent() {
        if (this.mNeedViewResizeAnimation) {
            this.mAnimationEvents.add(new AnimationEvent(null, 12));
        }
        this.mNeedViewResizeAnimation = false;
    }

    private void generateSnapBackEvents() {
        Iterator<View> it = this.mSnappedBackChildren.iterator();
        while (it.hasNext()) {
            this.mAnimationEvents.add(new AnimationEvent(it.next(), 5));
        }
        this.mSnappedBackChildren.clear();
    }

    private void generateDragEvents() {
        Iterator<View> it = this.mDragAnimPendingChildren.iterator();
        while (it.hasNext()) {
            this.mAnimationEvents.add(new AnimationEvent(it.next(), 4));
        }
        this.mDragAnimPendingChildren.clear();
    }

    private void generateChildRemovalEvents() {
        int animationType;
        AnimationEvent event;
        Iterator<View> it = this.mChildrenToRemoveAnimated.iterator();
        while (it.hasNext()) {
            View child = it.next();
            if (this.mSwipedOutViews.contains(child)) {
                animationType = 2;
            } else {
                animationType = 1;
            }
            if (NotificationUtil.isFoldAnimating()) {
                event = new AnimationEvent(child, animationType, 150);
            } else {
                event = new AnimationEvent(child, animationType);
            }
            float removedTranslation = child.getTranslationY();
            boolean ignoreChildren = true;
            if (child instanceof ExpandableNotificationRow) {
                ExpandableNotificationRow row = (ExpandableNotificationRow) child;
                if (row.isRemoved() && row.wasChildInGroupWhenRemoved()) {
                    removedTranslation = row.getTranslationWhenRemoved();
                    ignoreChildren = false;
                }
            }
            event.viewAfterChangingView = getFirstChildBelowTranlsationY(removedTranslation, ignoreChildren);
            this.mAnimationEvents.add(event);
            this.mSwipedOutViews.remove(child);
            this.mChildRemoveAnimationRunning = true;
        }
        this.mChildrenToRemoveAnimated.clear();
    }

    private void generatePositionChangeEvents() {
        Iterator<View> it = this.mChildrenChangingPositions.iterator();
        while (it.hasNext()) {
            this.mAnimationEvents.add(new AnimationEvent(it.next(), 8));
        }
        this.mChildrenChangingPositions.clear();
        if (this.mGenerateChildOrderChangedEvent) {
            this.mAnimationEvents.add(new AnimationEvent(null, 8));
            this.mGenerateChildOrderChangedEvent = false;
        }
    }

    private void generateChildAdditionEvents() {
        Iterator<View> it = this.mChildrenToAddAnimated.iterator();
        while (it.hasNext()) {
            View child = it.next();
            if (this.mFromMoreCardAdditions.contains(child)) {
                this.mAnimationEvents.add(new AnimationEvent(child, 0, 360));
            } else if (NotificationUtil.isFoldAnimating()) {
                this.mAnimationEvents.add(new AnimationEvent(child, 0, 150));
            } else {
                this.mAnimationEvents.add(new AnimationEvent(child, 0));
            }
        }
        this.mChildrenToAddAnimated.clear();
        this.mFromMoreCardAdditions.clear();
    }

    private void generateTopPaddingEvent() {
        if (this.mTopPaddingNeedsAnimation) {
            this.mAnimationEvents.add(new AnimationEvent(null, 3));
        }
        this.mTopPaddingNeedsAnimation = false;
    }

    private void generateActivateEvent() {
        if (this.mActivateNeedsAnimation) {
            this.mAnimationEvents.add(new AnimationEvent(null, 6));
        }
        this.mActivateNeedsAnimation = false;
    }

    private void generateAnimateEverythingEvent() {
        if (this.mEverythingNeedsAnimation) {
            this.mAnimationEvents.add(new AnimationEvent(null, 20));
        }
        this.mEverythingNeedsAnimation = false;
    }

    private void generateDimmedEvent() {
        if (this.mDimmedNeedsAnimation) {
            this.mAnimationEvents.add(new AnimationEvent(null, 7));
        }
        this.mDimmedNeedsAnimation = false;
    }

    private void generateHideSensitiveEvent() {
        if (this.mHideSensitiveNeedsAnimation) {
            this.mAnimationEvents.add(new AnimationEvent(null, 11));
        }
        this.mHideSensitiveNeedsAnimation = false;
    }

    private void generateDarkEvent() {
        if (this.mDarkNeedsAnimation) {
            AnimationEvent ev = new AnimationEvent((View) null, 9, new AnimationFilter().animateDark().animateY(this.mShelf));
            ev.darkAnimationOriginIndex = this.mDarkAnimationOriginIndex;
            this.mAnimationEvents.add(ev);
            startBackgroundFadeIn();
        }
        this.mDarkNeedsAnimation = false;
    }

    private void generateGoToFullShadeEvent() {
        if (this.mGoToFullShadeNeedsAnimation) {
            this.mAnimationEvents.add(new AnimationEvent(null, 10));
        }
        this.mGoToFullShadeNeedsAnimation = false;
    }

    private boolean onInterceptTouchEventScroll(MotionEvent ev) {
        MotionEvent motionEvent = ev;
        boolean z = false;
        if (!isScrollingEnabled()) {
            return false;
        }
        int action = ev.getAction();
        if (action == 2 && this.mIsBeingDragged) {
            return true;
        }
        int i = action & 255;
        if (i != 6) {
            switch (i) {
                case 0:
                    int y = (int) ev.getY();
                    this.mScrolledToTopOnFirstDown = isScrolledToTop();
                    if (getChildAtPosition(ev.getX(), (float) y) != null) {
                        this.mLastMotionY = y;
                        this.mDownX = (int) ev.getX();
                        this.mActivePointerId = motionEvent.getPointerId(0);
                        initOrResetVelocityTracker();
                        this.mVelocityTracker.addMovement(motionEvent);
                        setIsBeingDragged(!this.mScroller.isFinished());
                        break;
                    } else {
                        setIsBeingDragged(false);
                        recycleVelocityTracker();
                        break;
                    }
                case 1:
                case 3:
                    setIsBeingDragged(false);
                    this.mActivePointerId = -1;
                    recycleVelocityTracker();
                    if (this.mScroller.springBack(this.mScrollX, this.mOwnScrollY, 0, 0, 0, getScrollRange())) {
                        animateScroll();
                        break;
                    }
                    break;
                case 2:
                    int activePointerId = this.mActivePointerId;
                    if (activePointerId != -1) {
                        int pointerIndex = motionEvent.findPointerIndex(activePointerId);
                        if (pointerIndex != -1) {
                            int y2 = (int) motionEvent.getY(pointerIndex);
                            int x = (int) motionEvent.getX(pointerIndex);
                            int deltaY = this.mLastMotionY - y2;
                            int xDiff = Math.abs(x - this.mDownX);
                            int yDiff = Math.abs(deltaY);
                            if (yDiff > this.mTouchSlop && yDiff > xDiff) {
                                setIsBeingDragged(true);
                                if ((isInContentBounds(ev) || this.mIsQsCovered) && isScrolledToTop() && ((deltaY > 0 && canScrollUp() && !isQsCovered()) || (deltaY < 0 && isQsCovered()))) {
                                    this.mIsQsBeingCovered = true;
                                    this.mIsQsCovered = false;
                                }
                                this.mLastMotionY = y2;
                                this.mDownX = x;
                                initVelocityTrackerIfNotExists();
                                this.mVelocityTracker.addMovement(motionEvent);
                                break;
                            }
                        } else {
                            Log.e("StackScroller", "Invalid pointerId=" + activePointerId + " in onInterceptTouchEvent");
                            break;
                        }
                    }
                    break;
            }
        } else {
            onSecondaryPointerUp(ev);
        }
        if (this.mIsBeingDragged || this.mIsQsBeingCovered) {
            z = true;
        }
        return z;
    }

    /* access modifiers changed from: protected */
    public StackScrollAlgorithm createStackScrollAlgorithm(Context context) {
        return new StackScrollAlgorithm(context);
    }

    private boolean isInContentBounds(MotionEvent event) {
        return isInContentBounds(event.getY());
    }

    public boolean isInContentBounds(float y) {
        return y < ((float) (getHeight() - getEmptyBottomMargin()));
    }

    private void setIsBeingDragged(boolean isDragged) {
        this.mIsBeingDragged = isDragged;
        if (isDragged) {
            requestDisallowInterceptTouchEvent(true);
            removeLongPressCallback();
        }
    }

    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (!hasWindowFocus) {
            removeLongPressCallback();
        }
    }

    public void clearChildFocus(View child) {
        super.clearChildFocus(child);
        if (this.mForcedScroll == child) {
            this.mForcedScroll = null;
        }
    }

    public void requestDisallowLongPress() {
        removeLongPressCallback();
    }

    public void requestDisallowDismiss() {
        this.mDisallowDismissInThisMotion = true;
    }

    public void removeLongPressCallback() {
        this.mSwipeHelper.removeLongPressCallback();
    }

    public boolean isQsBeingCovered() {
        return this.mIsQsBeingCovered;
    }

    public boolean isQsCovered() {
        return this.mIsQsCovered;
    }

    public void resetIsQsCovered(boolean isQsCovered) {
        this.mIsQsCovered = isQsCovered;
        if (!this.mIsQsCovered && this.mQs != null && this.mQs.getQsContent() != null) {
            this.mQs.getQsContent().setScaleY(1.0f);
            this.mQs.getQsContent().setScaleX(1.0f);
            this.mQs.getQsContent().setAlpha(1.0f);
        }
    }

    public boolean canScrollUp() {
        return this.mOwnScrollY < getScrollRange();
    }

    public boolean isScrolledToTop() {
        return this.mOwnScrollY == 0;
    }

    public boolean isScrolledToBottom() {
        return this.mOwnScrollY >= getScrollRange();
    }

    public View getHostView() {
        return this;
    }

    public int getEmptyBottomMargin() {
        return Math.max(this.mMaxLayoutHeight - this.mContentHeight, 0);
    }

    public void checkSnoozeLeavebehind() {
        if (this.mCheckForLeavebehind) {
            this.mStatusBar.closeAndSaveGuts(true, false, false, -1, -1, false);
            this.mCheckForLeavebehind = false;
        }
    }

    public void resetCheckSnoozeLeavebehind() {
        this.mCheckForLeavebehind = true;
    }

    public void onExpansionStarted() {
        this.mIsExpansionChanging = true;
        this.mAmbientState.setExpansionChanging(true);
        checkSnoozeLeavebehind();
    }

    public void onExpansionStopped() {
        this.mIsExpansionChanging = false;
        resetCheckSnoozeLeavebehind();
        this.mAmbientState.setExpansionChanging(false);
        if (!this.mIsExpanded) {
            setOwnScrollY(0);
            this.mStatusBar.resetUserExpandedStates();
            clearTemporaryViews(this);
            for (int i = 0; i < getChildCount(); i++) {
                ExpandableView child = (ExpandableView) getChildAt(i);
                if (child instanceof ExpandableNotificationRow) {
                    clearTemporaryViews(((ExpandableNotificationRow) child).getChildrenContainer());
                }
            }
        }
    }

    private void clearTemporaryViews(ViewGroup viewGroup) {
        while (viewGroup != null && viewGroup.getTransientViewCount() != 0) {
            viewGroup.removeTransientView(viewGroup.getTransientView(0));
        }
        if (viewGroup != null) {
            viewGroup.getOverlay().clear();
        }
    }

    public void onPanelTrackingStarted() {
        this.mPanelTracking = true;
        this.mAmbientState.setPanelTracking(true);
    }

    public void onPanelTrackingStopped() {
        this.mPanelTracking = false;
        this.mAmbientState.setPanelTracking(false);
    }

    public void resetScrollPosition() {
        this.mScroller.abortAnimation();
        setOwnScrollY(0);
    }

    private void setIsExpanded(boolean isExpanded) {
        boolean changed = isExpanded != this.mIsExpanded;
        this.mIsExpanded = isExpanded;
        if (!this.mIsExpanded) {
            resetIsQsCovered(false);
        }
        this.mStackScrollAlgorithm.setIsExpanded(isExpanded);
        if (changed) {
            if (!this.mIsExpanded) {
                this.mSwipeHelper.resetAnimatingValue();
                this.mGroupManager.collapseAllGroups();
            } else {
                this.mStatusBar.showReturnToInCallScreenButtonIfNeed();
                this.mHeadsUpManager.removeHeadsUpNotification();
                hideHeadsUpBackground();
                this.mStatusBar.updateNotifications();
                updateClipToOutline();
            }
            updateNotificationAnimationStates();
            updateChronometers();
            requestChildrenUpdate();
        }
    }

    private void hideHeadsUpBackground() {
        int i = 0;
        setPadding(0, getPaddingTop(), 0, getPaddingBottom());
        int childCount = getChildCount();
        while (true) {
            int i2 = i;
            if (i2 < childCount) {
                hideHeadsUpBackgroundForChild(getChildAt(i2));
                i = i2 + 1;
            } else {
                return;
            }
        }
    }

    private void hideHeadsUpBackgroundForChild(View child) {
        if (child instanceof ExpandableNotificationRow) {
            ((ExpandableNotificationRow) child).hideHeadsUpBackground();
        }
    }

    public void onOpenFold(boolean expandFold) {
        if (!isQsCovered() && expandFold) {
            doExpandCollapseAnimation(true);
        }
        updateChildContentHeight();
        hideHeadsUpBackground();
    }

    public void onCloseFold() {
        if (NotificationUtil.isLastQsCovered() != this.mIsQsCovered) {
            doExpandCollapseAnimation(NotificationUtil.isLastQsCovered());
        }
    }

    private void updateChildContentHeight() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child instanceof ExpandableNotificationRow) {
                ((ExpandableNotificationRow) child).updateContentHeight();
            }
        }
    }

    private void updateChronometers() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            updateChronometerForChild(getChildAt(i));
        }
    }

    private void updateChronometerForChild(View child) {
        if (child instanceof ExpandableNotificationRow) {
            ((ExpandableNotificationRow) child).setChronometerRunning(this.mIsExpanded);
        }
    }

    public void onHeightChanged(ExpandableView view, boolean needsAnimation) {
        ExpandableNotificationRow row;
        updateContentHeight();
        updateScrollPositionOnExpandInBottom(view);
        clampScrollPosition();
        notifyHeightChangeListener(view);
        if (view instanceof ExpandableNotificationRow) {
            row = (ExpandableNotificationRow) view;
        } else {
            row = null;
        }
        if (row != null && (row == this.mFirstVisibleBackgroundChild || row.getNotificationParent() == this.mFirstVisibleBackgroundChild)) {
            updateAlgorithmLayoutMinHeight();
        }
        if (needsAnimation) {
            requestAnimationOnViewResize(row);
        }
        requestChildrenUpdate();
    }

    public void onReset(ExpandableView view) {
        updateAnimationState(view);
        updateChronometerForChild(view);
    }

    private void updateScrollPositionOnExpandInBottom(ExpandableView view) {
        if (view instanceof ExpandableNotificationRow) {
            ExpandableNotificationRow row = (ExpandableNotificationRow) view;
            if (row.isUserLocked() && row != getFirstChildNotGone() && !row.isSummaryWithChildren()) {
                float endPosition = row.getTranslationY() + ((float) row.getActualHeight());
                if (row.isChildInGroup()) {
                    endPosition += row.getNotificationParent().getTranslationY();
                }
                int layoutEnd = this.mMaxLayoutHeight + ((int) this.mStackTranslation);
                if (row != this.mLastVisibleBackgroundChild) {
                    layoutEnd -= this.mShelf.getIntrinsicHeight() + this.mPaddingBetweenElements;
                }
                if (endPosition > ((float) layoutEnd)) {
                    setOwnScrollY((int) ((((float) this.mOwnScrollY) + endPosition) - ((float) layoutEnd)));
                    this.mDisallowScrollingInThisMotion = true;
                }
            }
        }
    }

    public void setOnHeightChangedListener(ExpandableView.OnHeightChangedListener mOnHeightChangedListener2) {
        this.mOnHeightChangedListener = mOnHeightChangedListener2;
    }

    public void setOnEmptySpaceClickListener(OnEmptySpaceClickListener listener) {
        this.mOnEmptySpaceClickListener = listener;
    }

    public void setOnTopPaddingUpdateListener(OnTopPaddingUpdateListener onTopPaddingUpdateListener) {
        this.mOnTopPaddingUpdateListener = onTopPaddingUpdateListener;
    }

    public void onChildAnimationFinished() {
        setAnimationRunning(false);
        requestChildrenUpdate();
        runAnimationFinishedRunnables();
        clearViewOverlays();
        clearHeadsUpDisappearRunning();
    }

    private void clearHeadsUpDisappearRunning() {
        for (int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            if (view instanceof ExpandableNotificationRow) {
                ExpandableNotificationRow row = (ExpandableNotificationRow) view;
                row.setHeadsUpAnimatingAway(false);
                if (row.isSummaryWithChildren()) {
                    for (ExpandableNotificationRow child : row.getNotificationChildren()) {
                        child.setHeadsUpAnimatingAway(false);
                    }
                }
            }
        }
    }

    private void clearViewOverlays() {
        Iterator<View> it = this.mClearOverlayViewsWhenFinished.iterator();
        while (it.hasNext()) {
            StackStateAnimator.removeFromOverlay(it.next());
        }
        this.mClearOverlayViewsWhenFinished.clear();
    }

    private void runAnimationFinishedRunnables() {
        for (Runnable runnable : this.mAnimationFinishedRunnables.keySet()) {
            runnable.run();
        }
        this.mAnimationFinishedRunnables.clear();
    }

    public void setDimmed(boolean dimmed, boolean animate) {
        this.mAmbientState.setDimmed(dimmed);
        if (!animate || !this.mAnimationsEnabled) {
            setDimAmount(dimmed ? 1.0f : 0.0f);
        } else {
            this.mDimmedNeedsAnimation = true;
            this.mNeedsAnimation = true;
            animateDimmed(dimmed);
        }
        requestChildrenUpdate();
    }

    /* access modifiers changed from: private */
    public void setDimAmount(float dimAmount) {
        this.mDimAmount = dimAmount;
        updateBackgroundDimming();
    }

    private void animateDimmed(boolean dimmed) {
        if (this.mDimAnimator != null) {
            this.mDimAnimator.cancel();
        }
        float target = dimmed ? 1.0f : 0.0f;
        if (target != this.mDimAmount) {
            this.mDimAnimator = TimeAnimator.ofFloat(new float[]{this.mDimAmount, target});
            this.mDimAnimator.setDuration(220);
            this.mDimAnimator.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
            this.mDimAnimator.addListener(this.mDimEndListener);
            this.mDimAnimator.addUpdateListener(this.mDimUpdateListener);
            this.mDimAnimator.start();
        }
    }

    public void setHideSensitive(boolean hideSensitive, boolean animate) {
        if (hideSensitive != this.mAmbientState.isHideSensitive()) {
            int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                ((ExpandableView) getChildAt(i)).setHideSensitiveForIntrinsicHeight(hideSensitive);
            }
            this.mAmbientState.setHideSensitive(hideSensitive);
            if (animate && this.mAnimationsEnabled) {
                this.mHideSensitiveNeedsAnimation = true;
                this.mNeedsAnimation = true;
            }
            requestChildrenUpdate();
        }
    }

    public void setActivatedChild(ActivatableNotificationView activatedChild) {
        this.mAmbientState.setActivatedChild(activatedChild);
        if (this.mAnimationsEnabled) {
            this.mActivateNeedsAnimation = true;
            this.mNeedsAnimation = true;
        }
        requestChildrenUpdate();
    }

    public ActivatableNotificationView getActivatedChild() {
        return this.mAmbientState.getActivatedChild();
    }

    private void applyCurrentState() {
        this.mCurrentStackScrollState.apply();
        if (this.mListener != null) {
            this.mListener.onChildLocationsChanged();
        }
        runAnimationFinishedRunnables();
        setAnimationRunning(false);
        updateBackground();
        updateViewShadows();
    }

    /* access modifiers changed from: private */
    public void updateViewShadows() {
        for (int i = 0; i < getChildCount(); i++) {
            ExpandableView child = (ExpandableView) getChildAt(i);
            if (child.getVisibility() != 8) {
                this.mTmpSortedChildren.add(child);
            }
        }
        Collections.sort(this.mTmpSortedChildren, this.mViewPositionComparator);
        ExpandableView previous = null;
        for (int i2 = 0; i2 < this.mTmpSortedChildren.size(); i2++) {
            ExpandableView expandableView = this.mTmpSortedChildren.get(i2);
            float translationZ = expandableView.getTranslationZ();
            float diff = (previous == null ? translationZ : previous.getTranslationZ()) - translationZ;
            if (previous == null || diff <= 0.0f || diff >= 0.1f) {
                expandableView.setFakeShadowIntensity(0.0f, 0.0f, 0, 0);
            } else {
                expandableView.setFakeShadowIntensity(diff / 0.1f, previous.getOutlineAlpha(), (int) (((previous.getTranslationY() + ((float) previous.getActualHeight())) - expandableView.getTranslationY()) - ((float) previous.getExtraBottomPadding())), previous.getOutlineTranslation());
            }
            previous = expandableView;
        }
        this.mTmpSortedChildren.clear();
    }

    public void goToFullShade(long delay) {
        this.mEmptyShadeView.setInvisible();
        this.mGoToFullShadeNeedsAnimation = true;
        this.mGoToFullShadeDelay = delay;
        this.mNeedsAnimation = true;
        requestChildrenUpdate();
    }

    public void cancelExpandHelper() {
        this.mExpandHelper.cancel();
    }

    public void setIntrinsicPadding(int intrinsicPadding) {
        this.mIntrinsicPadding = intrinsicPadding;
    }

    public int getIntrinsicPadding() {
        return this.mIntrinsicPadding;
    }

    public float getNotificationsTopY() {
        return ((float) this.mTopPadding) + getStackTranslation();
    }

    public boolean shouldDelayChildPressedState() {
        return true;
    }

    public void setDark(boolean dark, boolean animate, PointF touchWakeUpScreenLocation) {
        this.mAmbientState.setDark(dark);
        if (animate && this.mAnimationsEnabled) {
            this.mDarkNeedsAnimation = true;
            this.mDarkAnimationOriginIndex = findDarkAnimationOriginIndex(touchWakeUpScreenLocation);
            this.mNeedsAnimation = true;
            setBackgroundFadeAmount(0.0f);
        } else if (!dark) {
            setBackgroundFadeAmount(1.0f);
        }
        requestChildrenUpdate();
        if (dark) {
            this.mScrimController.setExcludedBackgroundArea(null);
        } else {
            updateBackground();
        }
        updateWillNotDraw();
        updateContentHeight();
        notifyHeightChangeListener(this.mShelf);
    }

    private void updateWillNotDraw() {
        boolean z = false;
        if (!((!this.mAmbientState.isDark() && this.mShouldDrawNotificationBackground) || DEBUG)) {
            z = true;
        }
        setWillNotDraw(z);
    }

    /* access modifiers changed from: private */
    public void setBackgroundFadeAmount(float fadeAmount) {
        this.mBackgroundFadeAmount = fadeAmount;
        updateBackgroundDimming();
    }

    public float getBackgroundFadeAmount() {
        return this.mBackgroundFadeAmount;
    }

    private void startBackgroundFadeIn() {
        ObjectAnimator fadeAnimator = ObjectAnimator.ofFloat(this, BACKGROUND_FADE, new float[]{0.0f, 1.0f});
        fadeAnimator.setDuration(200);
        fadeAnimator.setInterpolator(Interpolators.ALPHA_IN);
        fadeAnimator.start();
    }

    private int findDarkAnimationOriginIndex(PointF screenLocation) {
        if (screenLocation == null || screenLocation.y < ((float) this.mTopPadding)) {
            return -1;
        }
        if (screenLocation.y > getBottomMostNotificationBottom()) {
            return -2;
        }
        View child = getClosestChildAtRawPosition(screenLocation.x, screenLocation.y);
        if (child != null) {
            return getNotGoneIndex(child);
        }
        return -1;
    }

    private int getNotGoneIndex(View child) {
        int count = getChildCount();
        int notGoneIndex = 0;
        for (int i = 0; i < count; i++) {
            View v = getChildAt(i);
            if (child == v) {
                return notGoneIndex;
            }
            if (v.getVisibility() != 8) {
                notGoneIndex++;
            }
        }
        return -1;
    }

    public int getVisibleNotificationIndex(View child) {
        int count = getChildCount();
        int index = 0;
        for (int i = 0; i < count; i++) {
            View v = getChildAt(i);
            if (child == v) {
                return index;
            }
            if ((v instanceof ExpandableNotificationRow) && v.getVisibility() == 0) {
                index++;
            }
        }
        return -1;
    }

    public void setFoldView(View header, View footer) {
        this.mFoldHeaderView = header;
        this.mFoldFooterView = footer;
        addView(this.mFoldHeaderView, -1);
        addView(this.mFoldFooterView, -1);
    }

    public void setFoldViewVisibility(boolean showFoldHeader, boolean showFoldFooter) {
        int i = 8;
        this.mFoldHeaderView.setVisibility(showFoldHeader ? 0 : 8);
        View view = this.mFoldFooterView;
        if (showFoldFooter) {
            i = 0;
        }
        view.setVisibility(i);
    }

    private boolean isFoldViewVisible() {
        boolean z = false;
        if (!NotificationUtil.isUserFold()) {
            return false;
        }
        if (this.mFoldHeaderView.getVisibility() == 0 || this.mFoldFooterView.getVisibility() == 0) {
            z = true;
        }
        return z;
    }

    public void removeFoldView() {
        removeTargetView(this.mFoldHeaderView);
        removeTargetView(this.mFoldFooterView);
    }

    public void setEmptyShadeView(EmptyShadeView emptyShadeView) {
        int index = removeTargetView(this.mEmptyShadeView);
        this.mEmptyShadeView = emptyShadeView;
        addView(this.mEmptyShadeView, index);
    }

    private int removeTargetView(View view) {
        if (view == null) {
            return -1;
        }
        int index = indexOfChild(view);
        removeView(view);
        return index;
    }

    public void updateEmptyShadeView(boolean visible) {
        int oldVisibility = this.mEmptyShadeView.willBeGone() ? 8 : this.mEmptyShadeView.getVisibility();
        int newVisibility = visible ? 0 : 8;
        if (oldVisibility == newVisibility) {
            return;
        }
        if (newVisibility != 8) {
            if (this.mEmptyShadeView.willBeGone()) {
                this.mEmptyShadeView.cancelAnimation();
            } else {
                this.mEmptyShadeView.setInvisible();
            }
            this.mEmptyShadeView.setVisibility(newVisibility);
            this.mEmptyShadeView.setWillBeGone(false);
            updateContentHeight();
            notifyHeightChangeListener(this.mEmptyShadeView);
            return;
        }
        Runnable onFinishedRunnable = new Runnable() {
            public void run() {
                NotificationStackScrollLayout.this.mEmptyShadeView.setVisibility(8);
                NotificationStackScrollLayout.this.mEmptyShadeView.setWillBeGone(false);
                NotificationStackScrollLayout.this.updateContentHeight();
                NotificationStackScrollLayout.this.notifyHeightChangeListener(NotificationStackScrollLayout.this.mEmptyShadeView);
            }
        };
        if (!this.mAnimationsEnabled || !this.mIsExpanded) {
            this.mEmptyShadeView.setInvisible();
            onFinishedRunnable.run();
            return;
        }
        this.mEmptyShadeView.setWillBeGone(true);
        this.mEmptyShadeView.performVisibilityAnimation(false, onFinishedRunnable);
    }

    public void setDismissAllInProgress(boolean dismissAllInProgress) {
        if (this.mDismissAllInProgress != dismissAllInProgress) {
            this.mDismissAllInProgress = dismissAllInProgress;
            this.mAmbientState.setDismissAllInProgress(dismissAllInProgress);
            handleDismissAllClipping();
        }
    }

    private void handleDismissAllClipping() {
        int count = getChildCount();
        boolean previousChildWillBeDismissed = false;
        for (int i = 0; i < count; i++) {
            ExpandableView child = (ExpandableView) getChildAt(i);
            if (child.getVisibility() != 8) {
                if (!this.mDismissAllInProgress || !previousChildWillBeDismissed) {
                    child.setMinClipTopAmount(0);
                } else {
                    child.setMinClipTopAmount(child.getClipTopAmount());
                }
                previousChildWillBeDismissed = canChildBeDismissed(child);
            }
        }
    }

    public void dispatchDismissAllToChild(List<View> realHideAnimatedList, Runnable finishAction) {
        this.mSwipeHelper.dispatchDismissAllToChild(realHideAnimatedList, finishAction);
    }

    public int getEmptyShadeViewHeight() {
        return this.mEmptyShadeView.getHeight();
    }

    public float getBottomMostNotificationBottom() {
        int count = getChildCount();
        float max = 0.0f;
        for (int childIdx = 0; childIdx < count; childIdx++) {
            ExpandableView child = (ExpandableView) getChildAt(childIdx);
            if (child.getVisibility() != 8) {
                float bottom = (child.getTranslationY() + ((float) child.getActualHeight())) - ((float) child.getClipBottomAmount());
                if (bottom > max) {
                    max = bottom;
                }
            }
        }
        return getStackTranslation() + max;
    }

    public void setStatusBar(StatusBar statusBar) {
        this.mStatusBar = statusBar;
    }

    public void setGroupManager(NotificationGroupManager groupManager) {
        this.mGroupManager = groupManager;
    }

    public void onGoToKeyguard() {
        requestAnimateEverything();
    }

    private void requestAnimateEverything() {
        if (this.mIsExpanded && this.mAnimationsEnabled) {
            this.mEverythingNeedsAnimation = true;
            this.mNeedsAnimation = true;
            requestChildrenUpdate();
        }
    }

    public boolean isBelowLastNotification(float touchX, float touchY) {
        int i = getChildCount() - 1;
        while (true) {
            boolean z = false;
            if (i >= 0) {
                ExpandableView child = (ExpandableView) getChildAt(i);
                if (child.getVisibility() != 8) {
                    float childTop = child.getY();
                    if (childTop > touchY) {
                        return false;
                    }
                    boolean belowChild = touchY > (((float) child.getActualHeight()) + childTop) - ((float) child.getClipBottomAmount());
                    if (child == this.mEmptyShadeView) {
                        return true;
                    }
                    if (!belowChild) {
                        return false;
                    }
                }
                i--;
            } else {
                if (touchY > ((float) this.mTopPadding) + this.mStackTranslation) {
                    z = true;
                }
                return z;
            }
        }
    }

    public void onGroupExpansionChanged(final ExpandableNotificationRow changedRow, boolean expanded) {
        boolean animated = !this.mGroupExpandedForMeasure && this.mAnimationsEnabled && (this.mIsExpanded || changedRow.isPinned());
        if (animated) {
            this.mExpandedGroupView = changedRow;
            this.mNeedsAnimation = true;
        }
        changedRow.setChildrenExpanded(expanded, animated);
        if (!this.mGroupExpandedForMeasure) {
            onHeightChanged(changedRow, false);
        }
        runAfterAnimationFinished(new Runnable() {
            public void run() {
                changedRow.onFinishedExpansionChange();
            }
        });
    }

    public void onGroupCreatedFromChildren(NotificationGroupManager.NotificationGroup group) {
        this.mStatusBar.requestNotificationUpdate();
    }

    public void onInitializeAccessibilityEventInternal(AccessibilityEvent event) {
        super.onInitializeAccessibilityEventInternal(event);
        event.setScrollable(this.mScrollable);
        event.setScrollX(this.mScrollX);
        event.setScrollY(this.mOwnScrollY);
        event.setMaxScrollX(this.mScrollX);
        event.setMaxScrollY(getScrollRange());
    }

    public void onInitializeAccessibilityNodeInfoInternal(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfoInternal(info);
        if (this.mScrollable) {
            info.setScrollable(true);
            if (this.mBackwardScrollable) {
                info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);
                info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_UP);
            }
            if (this.mForwardScrollable) {
                info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);
                info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_DOWN);
            }
        }
        info.setClassName(ScrollView.class.getName());
    }

    /* JADX WARNING: Code restructure failed: missing block: B:13:0x0021, code lost:
        if (r10 != 16908346) goto L_0x005f;
     */
    /* JADX WARNING: Removed duplicated region for block: B:17:0x004e  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean performAccessibilityActionInternal(int r10, android.os.Bundle r11) {
        /*
            r9 = this;
            boolean r0 = super.performAccessibilityActionInternal(r10, r11)
            r1 = 1
            if (r0 == 0) goto L_0x0008
            return r1
        L_0x0008:
            boolean r0 = r9.isEnabled()
            r2 = 0
            if (r0 != 0) goto L_0x0010
            return r2
        L_0x0010:
            r0 = -1
            r3 = 4096(0x1000, float:5.74E-42)
            if (r10 == r3) goto L_0x0024
            r3 = 8192(0x2000, float:1.14794E-41)
            if (r10 == r3) goto L_0x0025
            r3 = 16908344(0x1020038, float:2.3877386E-38)
            if (r10 == r3) goto L_0x0025
            r3 = 16908346(0x102003a, float:2.3877392E-38)
            if (r10 == r3) goto L_0x0024
            goto L_0x005f
        L_0x0024:
            r0 = 1
        L_0x0025:
            int r3 = r9.getHeight()
            int r4 = r9.mPaddingBottom
            int r3 = r3 - r4
            int r4 = r9.mTopPadding
            int r3 = r3 - r4
            int r4 = r9.mPaddingTop
            int r3 = r3 - r4
            com.android.systemui.statusbar.NotificationShelf r4 = r9.mShelf
            int r4 = r4.getIntrinsicHeight()
            int r3 = r3 - r4
            int r4 = r9.mOwnScrollY
            int r5 = r0 * r3
            int r4 = r4 + r5
            int r5 = r9.getScrollRange()
            int r4 = java.lang.Math.min(r4, r5)
            int r4 = java.lang.Math.max(r2, r4)
            int r5 = r9.mOwnScrollY
            if (r4 == r5) goto L_0x005f
            android.widget.OverScroller r5 = r9.mScroller
            int r6 = r9.mScrollX
            int r7 = r9.mOwnScrollY
            int r8 = r9.mOwnScrollY
            int r8 = r4 - r8
            r5.startScroll(r6, r7, r2, r8)
            r9.animateScroll()
            return r1
        L_0x005f:
            return r2
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.stack.NotificationStackScrollLayout.performAccessibilityActionInternal(int, android.os.Bundle):boolean");
    }

    public void onGroupsChanged() {
        this.mStatusBar.requestNotificationUpdate();
    }

    public void generateChildOrderChangedEvent() {
        if (this.mIsExpanded && this.mAnimationsEnabled) {
            this.mGenerateChildOrderChangedEvent = true;
            this.mNeedsAnimation = true;
            requestChildrenUpdate();
        }
    }

    public void runAfterAnimationFinished(Runnable runnable) {
        this.mAnimationFinishedRunnables.put(runnable, PRESENT);
    }

    public void setHeadsUpManager(HeadsUpManager headsUpManager) {
        this.mHeadsUpManager = headsUpManager;
        this.mAmbientState.setHeadsUpManager(headsUpManager);
    }

    public void onHeadsUpPinned(ExpandableNotificationRow headsUp) {
        updateHeadsUpState();
        updateClipToOutline();
        generateHeadsUpAnimation(headsUp, true);
    }

    public void onHeadsUpUnPinned(ExpandableNotificationRow headsUp) {
        ExpandableViewState viewState = this.mCurrentStackScrollState.getViewStateForView(headsUp);
        if (viewState != null) {
            viewState.cancelAnimations(headsUp);
        }
        updateHeadsUpState();
        updateClipToOutline();
    }

    public void onHeadsUpStateChanged(NotificationData.Entry entry, boolean isHeadsUp) {
        updateHeadsUpState();
        generateHeadsUpAnimation(entry.row, isHeadsUp);
    }

    private void updateHeadsUpState() {
        this.mHeadsUpPinned = this.mHeadsUpManager.hasPinnedHeadsUp();
        this.mStackScrollAlgorithm.setExpandedBecauseOfHeadsUp(this.mHeadsUpPinned || this.mHeadsUpAnimatingAway);
    }

    private void generateHeadsUpAnimation(ExpandableNotificationRow row, boolean isHeadsUp) {
        if (isHeadsUp && !this.mIsExpanded) {
            int padding = getResources().getDimensionPixelOffset(R.dimen.notification_heads_up_margin_horizontal) - getResources().getDimensionPixelOffset(R.dimen.panel_content_margin_horizontal);
            setPadding(padding, getPaddingTop(), padding, getPaddingBottom());
            row.showHeadsUpBackground();
        }
        if (this.mAnimationsEnabled != 0) {
            this.mHeadsUpChangeAnimations.add(new Pair(row, Boolean.valueOf(isHeadsUp)));
            this.mNeedsAnimation = true;
            if (!this.mIsExpanded && !isHeadsUp) {
                row.setHeadsUpAnimatingAway(true);
            }
            requestChildrenUpdate();
        }
    }

    public void setShadeExpanded(boolean shadeExpanded) {
        this.mAmbientState.setShadeExpanded(shadeExpanded);
        this.mStateAnimator.setShadeExpanded(shadeExpanded);
    }

    public void setHeadsUpBoundaries(int height, int bottomBarHeight) {
        this.mAmbientState.setMaxHeadsUpTranslation((float) (height - bottomBarHeight));
        this.mStateAnimator.setHeadsUpAppearHeightBottom(height);
        requestChildrenUpdate();
    }

    public void setTrackingHeadsUp(boolean trackingHeadsUp) {
        this.mTrackingHeadsUp = trackingHeadsUp;
    }

    public void setScrimController(ScrimController scrimController) {
        this.mScrimController = scrimController;
        this.mScrimController.setScrimBehindChangeRunnable(new Runnable() {
            public void run() {
                NotificationStackScrollLayout.this.updateBackgroundDimming();
            }
        });
    }

    public void forceNoOverlappingRendering(boolean force) {
        this.mForceNoOverlappingRendering = force;
    }

    public boolean hasOverlappingRendering() {
        return !this.mForceNoOverlappingRendering && super.hasOverlappingRendering();
    }

    public void setAnimationRunning(boolean animationRunning) {
        ScenarioTrackUtil.SystemUIEventScenario systemUIEventScenario;
        if (animationRunning != this.mAnimationRunning) {
            if (animationRunning) {
                getViewTreeObserver().addOnPreDrawListener(this.mRunningAnimationUpdater);
            } else {
                getViewTreeObserver().removeOnPreDrawListener(this.mRunningAnimationUpdater);
            }
            this.mAnimationRunning = animationRunning;
            updateContinuousShadowDrawing();
        }
        if (!animationRunning) {
            this.mChildRemoveAnimationRunning = false;
            if (NotificationUtil.isFoldAnimating()) {
                if (NotificationUtil.isFold()) {
                    systemUIEventScenario = ScenarioConstants.SCENARIO_OPEN_FOLD;
                } else {
                    systemUIEventScenario = ScenarioConstants.SCENARIO_CLOSE_FOLD;
                }
                ScenarioTrackUtil.finishScenario(systemUIEventScenario);
                NotificationUtil.setFoldAnimating(false);
            }
        }
    }

    public boolean isExpanded() {
        return this.mIsExpanded;
    }

    public void setFadingOut(boolean fadingOut) {
        if (fadingOut != this.mFadingOut) {
            this.mFadingOut = fadingOut;
            updateFadingState();
        }
    }

    public void setParentNotFullyVisible(boolean parentNotFullyVisible) {
        if (!(this.mScrimController == null || parentNotFullyVisible == this.mParentNotFullyVisible)) {
            this.mParentNotFullyVisible = parentNotFullyVisible;
            updateFadingState();
        }
    }

    private void updateFadingState() {
        applyCurrentBackgroundBounds();
        updateSrcDrawing();
    }

    public void setAlpha(float alpha) {
        super.setAlpha(alpha);
        setFadingOut(alpha != 1.0f);
    }

    public void setQsExpanded(boolean qsExpanded) {
        this.mQsExpanded = qsExpanded;
        updateAlgorithmLayoutMinHeight();
    }

    public void setOwnScrollY(int ownScrollY) {
        if (ownScrollY != this.mOwnScrollY) {
            onScrollChanged(this.mScrollX, ownScrollY, this.mScrollX, this.mOwnScrollY);
            this.mOwnScrollY = ownScrollY;
            updateForwardAndBackwardScrollability();
            requestChildrenUpdate();
        }
    }

    public void setShelf(NotificationShelf shelf) {
        int index = -1;
        if (this.mShelf != null) {
            index = indexOfChild(this.mShelf);
            removeView(this.mShelf);
        }
        this.mShelf = shelf;
        addView(this.mShelf, index);
        this.mAmbientState.setShelf(shelf);
        this.mStateAnimator.setShelf(shelf);
        shelf.bind(this.mAmbientState, this);
    }

    public void setMaxDisplayedNotifications(int maxDisplayedNotifications) {
        if (this.mMaxDisplayedNotifications != maxDisplayedNotifications) {
            this.mMaxDisplayedNotifications = maxDisplayedNotifications;
            updateContentHeight();
            notifyHeightChangeListener(this.mShelf);
        }
    }

    public int getMinExpansionHeight() {
        return this.mShelf.getIntrinsicHeight() - ((this.mShelf.getIntrinsicHeight() - this.mStatusBarHeight) / 2);
    }

    public void setInHeadsUpPinnedMode(boolean inHeadsUpPinnedMode) {
        this.mInHeadsUpPinnedMode = inHeadsUpPinnedMode;
        updateClipping();
    }

    public void setHeadsUpAnimatingAway(boolean headsUpAnimatingAway) {
        this.mHeadsUpAnimatingAway = headsUpAnimatingAway;
        updateHeadsUpState();
        updateClipToOutline();
        updateClipping();
    }

    public void setStatusBarState(int statusBarState) {
        this.mStatusBarState = statusBarState;
        this.mAmbientState.setStatusBarState(statusBarState);
        updateClipToOutline();
        invalidateOutline();
    }

    public void setExpandingVelocity(float expandingVelocity) {
        this.mAmbientState.setExpandingVelocity(expandingVelocity);
    }

    public float getOpeningHeight() {
        return (float) getMinExpansionHeight();
    }

    public void setIsFullWidth(boolean isFullWidth) {
        this.mAmbientState.setPanelFullWidth(isFullWidth);
    }

    public boolean isInUserVisibleArea(ExpandableNotificationRow row) {
        boolean z = false;
        if (!isInVisibleLocation(row)) {
            return false;
        }
        ExpandableViewState state = this.mCurrentStackScrollState.getViewStateForView(row);
        float parentY = 0.0f;
        if (row.isChildInGroup() && row.getNotificationParent() != null) {
            parentY = this.mCurrentStackScrollState.getViewStateForView(row.getNotificationParent()).yTranslation;
        }
        float top = state.yTranslation + parentY;
        if (((float) row.getActualHeight()) + top > 0.0f && top < this.mExpandedHeight) {
            z = true;
        }
        return z;
    }

    public void setExtraBottomRange(int bottom, int bottomQsCovered) {
        this.mExtraBottomRange = bottom;
        this.mExtraBottomRangeQsCovered = bottomQsCovered;
    }

    public void resetViews() {
        setOverScrollAmount(0.0f, true, false, true);
        resetScrollPosition();
        this.mIsBeingDragged = false;
        this.mIsQsBeingCovered = false;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        Object[] objArr = new Object[12];
        objArr[0] = Float.valueOf(this.mExpandedHeight);
        objArr[1] = Integer.valueOf(this.mCurrentStackHeight);
        objArr[2] = Integer.valueOf(this.mContentHeight);
        objArr[3] = Integer.valueOf(this.mMaxLayoutHeight);
        objArr[4] = Integer.valueOf(this.mOwnScrollY);
        objArr[5] = Integer.valueOf(getScrollRange());
        objArr[6] = Integer.valueOf(this.mTopPadding);
        objArr[7] = Integer.valueOf(this.mIntrinsicPadding);
        objArr[8] = this.mIsBeingDragged ? "T" : "f";
        objArr[9] = this.mIsQsBeingCovered ? "T" : "f";
        objArr[10] = this.mIsQsCovered ? "T" : "f";
        objArr[11] = this.mQsExpanded ? "T" : "f";
        pw.println(String.format("      [NotificationStackScrollLayout: mExpandedHeight=%f mCurrentStackHeight=%d mContentHeight=%d mMaxLayoutHeight=%d mOwnScrollY=%d scrollRange=%d mTopPadding=%d mIntrinsicPadding=%d mIsBeingDragged=%s mIsQsBeingCovered=%s mIsQsCovered=%s mQsExpanded=%s]", objArr));
    }

    /* access modifiers changed from: private */
    public boolean isTouchInView(MotionEvent ev, View view) {
        int height;
        if (view == null) {
            return false;
        }
        if (view instanceof ExpandableView) {
            height = ((ExpandableView) view).getActualHeight();
        } else {
            height = view.getHeight();
        }
        view.getLocationOnScreen(this.mTempInt2);
        int x = this.mTempInt2[0];
        int y = this.mTempInt2[1];
        return new Rect(x, y, view.getWidth() + x, y + height).contains((int) ev.getRawX(), (int) ev.getRawY());
    }

    private void updateContinuousShadowDrawing() {
        boolean continuousShadowUpdate = this.mAnimationRunning || !this.mAmbientState.getDraggedViews().isEmpty();
        if (continuousShadowUpdate != this.mContinuousShadowUpdate) {
            if (continuousShadowUpdate) {
                getViewTreeObserver().addOnPreDrawListener(this.mShadowUpdater);
            } else {
                getViewTreeObserver().removeOnPreDrawListener(this.mShadowUpdater);
            }
            this.mContinuousShadowUpdate = continuousShadowUpdate;
        }
    }

    public void resetExposedMenuView(boolean animate, boolean force) {
        this.mSwipeHelper.resetExposedMenuView(animate, force);
    }

    public void closeControlsIfOutsideTouch(MotionEvent ev) {
        this.mSwipeHelper.closeControlsIfOutsideTouch(ev);
    }
}
