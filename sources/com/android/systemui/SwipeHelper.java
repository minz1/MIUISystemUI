package com.android.systemui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.RectF;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import com.android.systemui.classifier.FalsingManager;
import com.android.systemui.plugins.statusbar.NotificationMenuRowPlugin;
import com.android.systemui.statistic.ScenarioConstants;
import com.android.systemui.statistic.ScenarioTrackUtil;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.FlingAnimationUtils;
import com.android.systemui.statusbar.stack.NotificationStackScrollLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SwipeHelper {
    private static float sMenuShownSize;
    private int mBlockWidth;
    /* access modifiers changed from: private */
    public Callback mCallback;
    private boolean mCanCurrViewBeDimissed;
    /* access modifiers changed from: private */
    public Context mContext;
    /* access modifiers changed from: private */
    public View mCurrView;
    private float mDensityScale;
    /* access modifiers changed from: private */
    public boolean mDisableHwLayers;
    private AnimatorSet mDismissAllAnimatorSet;
    /* access modifiers changed from: private */
    public boolean mDismissAllRunning;
    /* access modifiers changed from: private */
    public HashMap<View, Animator> mDismissPendingMap = new HashMap<>();
    private boolean mDragging;
    private FalsingManager mFalsingManager;
    private int mFalsingThreshold;
    private FlingAnimationUtils mFlingAnimationUtils;
    private Handler mHandler;
    private float mInitialTouchPos;
    /* access modifiers changed from: private */
    public LongPressListener mLongPressListener;
    /* access modifiers changed from: private */
    public boolean mLongPressSent;
    private long mLongPressTimeout;
    private float mPagingTouchSlop;
    private float mPerpendicularInitialTouchPos;
    /* access modifiers changed from: private */
    public boolean mSnappingChild;
    private int mSwipeDirection;
    /* access modifiers changed from: private */
    public final int[] mTmpPos = new int[2];
    private boolean mTouchAboveFalsingThreshold;
    private float mTranslation = 0.0f;
    private VelocityTracker mVelocityTracker;
    private Runnable mWatchLongPress;

    public interface Callback {
        boolean canChildBeDismissed(View view);

        View getChildAtPosition(MotionEvent motionEvent);

        float getFalsingThresholdFactor();

        boolean isAntiFalsingNeeded();

        void onBeginDrag(View view);

        void onChildDismissed(View view);

        void onChildSnappedBack(View view, float f);

        void onDragCancelled(View view);
    }

    public interface LongPressListener {
        boolean onLongPress(View view, int i, int i2, NotificationMenuRowPlugin.MenuItem menuItem);
    }

    public interface MenuPressListener {
        boolean onMenuPress(View view, int i, int i2, NotificationMenuRowPlugin.MenuItem menuItem);
    }

    public SwipeHelper(int swipeDirection, Callback callback, Context context) {
        this.mContext = context;
        this.mCallback = callback;
        this.mHandler = new Handler();
        this.mSwipeDirection = swipeDirection;
        this.mVelocityTracker = VelocityTracker.obtain();
        this.mDensityScale = context.getResources().getDisplayMetrics().density;
        this.mPagingTouchSlop = (float) ViewConfiguration.get(context).getScaledPagingTouchSlop();
        this.mLongPressTimeout = (long) (((float) ViewConfiguration.getLongPressTimeout()) * 1.5f);
        this.mFalsingThreshold = context.getResources().getDimensionPixelSize(R.dimen.swipe_helper_falsing_threshold);
        this.mFalsingManager = FalsingManager.getInstance(context);
        this.mBlockWidth = context.getResources().getDimensionPixelOffset(R.dimen.notification_block_width);
        this.mFlingAnimationUtils = new FlingAnimationUtils(context, ((float) getMaxEscapeAnimDuration()) / 1000.0f);
        sMenuShownSize = (float) context.getResources().getDimensionPixelSize(R.dimen.notification_menu_space);
        initDismissAllAnimation();
    }

    private void initDismissAllAnimation() {
        this.mDismissAllAnimatorSet = new AnimatorSet();
        this.mDismissAllAnimatorSet.setDuration(160);
    }

    public void setLongPressListener(LongPressListener listener) {
        this.mLongPressListener = listener;
    }

    public void setDensityScale(float densityScale) {
        this.mDensityScale = densityScale;
    }

    public void setPagingTouchSlop(float pagingTouchSlop) {
        this.mPagingTouchSlop = pagingTouchSlop;
    }

    private float getPos(MotionEvent ev) {
        return this.mSwipeDirection == 0 ? ev.getX() : ev.getY();
    }

    private float getPerpendicularPos(MotionEvent ev) {
        return this.mSwipeDirection == 0 ? ev.getY() : ev.getX();
    }

    /* access modifiers changed from: protected */
    public float getTranslation(View v) {
        return this.mSwipeDirection == 0 ? v.getTranslationX() : v.getTranslationY();
    }

    private float getVelocity(VelocityTracker vt) {
        if (this.mSwipeDirection == 0) {
            return vt.getXVelocity();
        }
        return vt.getYVelocity();
    }

    /* access modifiers changed from: protected */
    public ObjectAnimator createTranslationAnimation(View v, float newPos) {
        return ObjectAnimator.ofFloat(v, this.mSwipeDirection == 0 ? View.TRANSLATION_X : View.TRANSLATION_Y, new float[]{newPos});
    }

    /* access modifiers changed from: protected */
    public Animator getViewTranslationAnimator(View v, float target, ValueAnimator.AnimatorUpdateListener listener) {
        ObjectAnimator anim = createTranslationAnimation(v, target);
        if (listener != null) {
            anim.addUpdateListener(listener);
        }
        return anim;
    }

    /* access modifiers changed from: protected */
    public void setTranslation(View v, float translate) {
        if (v != null) {
            if (this.mSwipeDirection == 0) {
                v.setTranslationX(translate);
            } else {
                v.setTranslationY(translate);
            }
        }
    }

    /* access modifiers changed from: protected */
    public float getSize(View v) {
        if (this.mSwipeDirection == 0) {
            return (float) v.getMeasuredWidth();
        }
        return (float) v.getMeasuredHeight();
    }

    public static float getAlphaForOffset(float translation) {
        if (sMenuShownSize > 0.0f) {
            return Math.max(1.0f - ((Math.abs(translation) / sMenuShownSize) * 0.35000002f), 0.0f);
        }
        return 1.0f;
    }

    /* access modifiers changed from: private */
    public void updateSwipeProgressFromOffset(View animView, boolean dismissable) {
        updateSwipeProgressFromOffset(animView, dismissable, getTranslation(animView));
    }

    private void updateSwipeProgressFromOffset(View animView, boolean dismissable, float translation) {
        if ((translation > 0.0f && dismissable) || translation < 0.0f) {
            float alpha = getAlphaForOffset(translation);
            if (translation > 0.0f && (animView instanceof ExpandableNotificationRow)) {
                if (!this.mDisableHwLayers) {
                    if (alpha == 0.0f || alpha == 1.0f) {
                        animView.setLayerType(0, null);
                    } else {
                        animView.setLayerType(2, null);
                    }
                }
                animView.setAlpha(alpha);
            }
        }
        invalidateGlobalRegion(animView);
    }

    public static void invalidateGlobalRegion(View view) {
        invalidateGlobalRegion(view, new RectF((float) view.getLeft(), (float) view.getTop(), (float) view.getRight(), (float) view.getBottom()));
    }

    /* JADX WARNING: type inference failed for: r0v3, types: [android.view.ViewParent] */
    /* JADX WARNING: Multi-variable type inference failed */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static void invalidateGlobalRegion(android.view.View r5, android.graphics.RectF r6) {
        /*
        L_0x0000:
            android.view.ViewParent r0 = r5.getParent()
            if (r0 == 0) goto L_0x0040
            android.view.ViewParent r0 = r5.getParent()
            boolean r0 = r0 instanceof android.view.View
            if (r0 == 0) goto L_0x0040
            android.view.ViewParent r0 = r5.getParent()
            r5 = r0
            android.view.View r5 = (android.view.View) r5
            android.graphics.Matrix r0 = r5.getMatrix()
            r0.mapRect(r6)
            float r0 = r6.left
            double r0 = (double) r0
            double r0 = java.lang.Math.floor(r0)
            int r0 = (int) r0
            float r1 = r6.top
            double r1 = (double) r1
            double r1 = java.lang.Math.floor(r1)
            int r1 = (int) r1
            float r2 = r6.right
            double r2 = (double) r2
            double r2 = java.lang.Math.ceil(r2)
            int r2 = (int) r2
            float r3 = r6.bottom
            double r3 = (double) r3
            double r3 = java.lang.Math.ceil(r3)
            int r3 = (int) r3
            r5.invalidate(r0, r1, r2, r3)
            goto L_0x0000
        L_0x0040:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.SwipeHelper.invalidateGlobalRegion(android.view.View, android.graphics.RectF):void");
    }

    public void removeLongPressCallback() {
        if (this.mWatchLongPress != null) {
            this.mHandler.removeCallbacks(this.mWatchLongPress);
            this.mWatchLongPress = null;
        }
    }

    public boolean onInterceptTouchEvent(final MotionEvent ev) {
        boolean z = true;
        switch (ev.getAction()) {
            case 0:
                this.mTouchAboveFalsingThreshold = false;
                this.mDragging = false;
                this.mSnappingChild = false;
                this.mLongPressSent = false;
                this.mVelocityTracker.clear();
                this.mCurrView = this.mCallback.getChildAtPosition(ev);
                if (this.mCurrView != null) {
                    onDownUpdate(this.mCurrView, ev);
                    this.mCanCurrViewBeDimissed = this.mCallback.canChildBeDismissed(this.mCurrView);
                    this.mVelocityTracker.addMovement(ev);
                    this.mInitialTouchPos = getPos(ev);
                    this.mPerpendicularInitialTouchPos = getPerpendicularPos(ev);
                    this.mTranslation = getTranslation(this.mCurrView);
                    if (this.mLongPressListener != null) {
                        if (this.mWatchLongPress == null) {
                            this.mWatchLongPress = new Runnable() {
                                public void run() {
                                    if (SwipeHelper.this.mCurrView != null && !SwipeHelper.this.mLongPressSent) {
                                        boolean unused = SwipeHelper.this.mLongPressSent = true;
                                        SwipeHelper.this.mCurrView.sendAccessibilityEvent(2);
                                        SwipeHelper.this.mCurrView.getLocationOnScreen(SwipeHelper.this.mTmpPos);
                                        int x = ((int) ev.getRawX()) - SwipeHelper.this.mTmpPos[0];
                                        int y = ((int) ev.getRawY()) - SwipeHelper.this.mTmpPos[1];
                                        NotificationMenuRowPlugin.MenuItem menuItem = null;
                                        if (SwipeHelper.this.mCurrView instanceof ExpandableNotificationRow) {
                                            menuItem = ((ExpandableNotificationRow) SwipeHelper.this.mCurrView).getProvider().getLongpressMenuItem(SwipeHelper.this.mContext);
                                        }
                                        SwipeHelper.this.mLongPressListener.onLongPress(SwipeHelper.this.mCurrView, x, y, menuItem);
                                    }
                                }
                            };
                        }
                        this.mHandler.postDelayed(this.mWatchLongPress, this.mLongPressTimeout);
                        break;
                    }
                }
                break;
            case 1:
            case 3:
                boolean captured = this.mDragging || this.mLongPressSent;
                this.mDragging = false;
                this.mCurrView = null;
                this.mLongPressSent = false;
                removeLongPressCallback();
                if (captured) {
                    return true;
                }
                break;
            case 2:
                if (this.mCurrView != null && !this.mLongPressSent) {
                    this.mVelocityTracker.addMovement(ev);
                    float pos = getPos(ev);
                    float perpendicularPos = getPerpendicularPos(ev);
                    float delta = pos - this.mInitialTouchPos;
                    float deltaPerpendicular = perpendicularPos - this.mPerpendicularInitialTouchPos;
                    if (Math.abs(delta) > this.mPagingTouchSlop && Math.abs(delta) > Math.abs(deltaPerpendicular)) {
                        this.mCallback.onBeginDrag(this.mCurrView);
                        this.mDragging = true;
                        this.mInitialTouchPos = getPos(ev);
                        this.mTranslation = getTranslation(this.mCurrView);
                        removeLongPressCallback();
                        break;
                    }
                }
        }
        if (!this.mDragging && !this.mLongPressSent) {
            z = false;
        }
        return z;
    }

    public void resetAnimatingValue() {
        this.mDismissAllRunning = false;
    }

    public void dispatchDismissAllToChild(final List<View> realHideAnimatedList, final Runnable finishAction) {
        if (!realHideAnimatedList.isEmpty()) {
            if (this.mDismissAllRunning) {
                Log.i("com.android.systemui.SwipeHelper", "dispatchDismissAllToChild mDismissAllAnimatorSet is Running");
                return;
            }
            Log.d("com.android.systemui.SwipeHelper", "dispatchDismissAllToChild do dismiss All.");
            this.mDismissAllRunning = true;
            this.mDismissAllAnimatorSet.cancel();
            this.mDismissAllAnimatorSet.removeAllListeners();
            Set<View> alphaViews = new HashSet<>();
            for (View view : realHideAnimatedList) {
                if (view instanceof ExpandableNotificationRow) {
                    ExpandableNotificationRow expandableRow = (ExpandableNotificationRow) view;
                    expandableRow.setDismissAllInProgress(true);
                    if (expandableRow.isClearable()) {
                        if (expandableRow.isGroupExpanded()) {
                            if (!expandableRow.isChildInGroup() && expandableRow.getChildrenContainer() != null) {
                                alphaViews.add(expandableRow.getChildrenContainer().getCollapsedButton());
                            }
                            alphaViews.add(expandableRow.getBackgroundNormal());
                        } else if (expandableRow.isExpanded()) {
                            alphaViews.add(expandableRow.getBackgroundNormal());
                        }
                    }
                }
            }
            if (Build.VERSION.SDK_INT < 24) {
                initDismissAllAnimation();
            }
            this.mDismissAllAnimatorSet.addListener(new AnimatorListenerAdapter() {
                public void onAnimationCancel(Animator animation) {
                    boolean unused = SwipeHelper.this.mDismissAllRunning = false;
                    Log.d("com.android.systemui.SwipeHelper", "dispatchDismissAllToChild onAnimationCancel.");
                }

                public void onAnimationEnd(Animator animation) {
                    SwipeHelper.this.doRowAnimations(realHideAnimatedList, finishAction);
                    boolean unused = SwipeHelper.this.mDismissAllRunning = false;
                    Log.d("com.android.systemui.SwipeHelper", "dispatchDismissAllToChild onAnimationEnd.");
                }
            });
            this.mDismissAllAnimatorSet.playTogether(createAlphaAnimators(alphaViews, 0.0f));
            try {
                this.mDismissAllAnimatorSet.start();
            } catch (Exception e) {
                Log.e("com.android.systemui.SwipeHelper", e.getMessage());
                this.mDismissAllRunning = false;
            }
            Log.d("com.android.systemui.SwipeHelper", "dispatchDismissAllToChild mDismissAllAnimatorSet started.");
        }
    }

    private List<Animator> createAlphaAnimators(Collection<View> views, float toValue) {
        List<Animator> animatorList = new ArrayList<>();
        for (View view : views) {
            ObjectAnimator animator = ObjectAnimator.ofFloat(view, "alpha", new float[]{toValue});
            animator.setAutoCancel(true);
            animatorList.add(animator);
        }
        return animatorList;
    }

    /* access modifiers changed from: private */
    public void doRowAnimations(List<View> realHideAnimatedList, Runnable animationFinishAction) {
        int currentDelay = 100;
        int totalDelay = 0;
        for (int i = realHideAnimatedList.size() - 1; i >= 0; i--) {
            View view = realHideAnimatedList.get(i);
            Runnable endRunnable = null;
            if (i == 0) {
                endRunnable = animationFinishAction;
            }
            dismissChild(view, 0.0f, endRunnable, (long) totalDelay, true, 200, true);
            currentDelay = Math.max(30, currentDelay - 10);
            totalDelay += currentDelay;
        }
        List<View> list = realHideAnimatedList;
    }

    public void dismissChild(View view, float velocity, boolean useAccelerateInterpolator) {
        dismissChild(view, velocity, null, 0, useAccelerateInterpolator, 0, false);
    }

    public void dismissChild(View animView, float velocity, Runnable endAction, long delay, boolean useAccelerateInterpolator, long fixedDuration, boolean isDismissAll) {
        float newPos;
        long duration;
        Animator anim;
        Animator anim2;
        final View view = animView;
        long j = delay;
        final boolean canBeDismissed = this.mCallback.canChildBeDismissed(view);
        boolean animateLeft = false;
        boolean isLayoutRtl = animView.getLayoutDirection() == 1;
        boolean animateUpForMenu = velocity == 0.0f && (getTranslation(animView) == 0.0f || isDismissAll) && this.mSwipeDirection == 1;
        boolean animateLeftForRtl = velocity == 0.0f && (getTranslation(animView) == 0.0f || isDismissAll) && isLayoutRtl;
        if ((Math.abs(velocity) > getEscapeVelocity() && velocity < 0.0f) || (getTranslation(animView) < 0.0f && !isDismissAll)) {
            animateLeft = true;
        }
        if (animateLeft || animateLeftForRtl || animateUpForMenu) {
            newPos = -getSize(animView);
        } else {
            newPos = getSize(animView);
        }
        float newPos2 = newPos;
        if (fixedDuration != 0) {
            duration = fixedDuration;
        } else if (velocity != 0.0f) {
            duration = Math.min(400, (long) ((int) ((Math.abs(newPos2 - getTranslation(animView)) * 1000.0f) / Math.abs(velocity))));
        } else {
            duration = 200;
        }
        long duration2 = duration;
        if (!this.mDisableHwLayers) {
            view.setLayerType(2, null);
        }
        AnonymousClass3 r2 = new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                SwipeHelper.this.onTranslationUpdate(view, ((Float) animation.getAnimatedValue()).floatValue(), canBeDismissed);
            }
        };
        Animator anim3 = getViewTranslationAnimator(view, newPos2, r2);
        if (anim3 != null) {
            if (useAccelerateInterpolator) {
                anim3.setInterpolator(Interpolators.FAST_OUT_LINEAR_IN);
                anim3.setDuration(duration2);
                anim = anim3;
                AnonymousClass3 r21 = r2;
                long j2 = duration2;
                float f = newPos2;
            } else {
                anim = anim3;
                AnonymousClass3 r212 = r2;
                long j3 = duration2;
                float f2 = newPos2;
                this.mFlingAnimationUtils.applyDismissing(anim3, getTranslation(animView), newPos2, velocity, getSize(animView));
            }
            if (j > 0) {
                anim2 = anim;
                anim2.setStartDelay(j);
            } else {
                anim2 = anim;
            }
            final View view2 = view;
            final boolean z = canBeDismissed;
            AnonymousClass4 r8 = r0;
            final Runnable runnable = endAction;
            Animator anim4 = anim2;
            final boolean z2 = isDismissAll;
            AnonymousClass4 r0 = new AnimatorListenerAdapter() {
                private boolean mCancelled;

                public void onAnimationCancel(Animator animation) {
                    this.mCancelled = true;
                }

                public void onAnimationEnd(Animator animation) {
                    ScenarioTrackUtil.SystemUIEventScenario systemUIEventScenario;
                    SwipeHelper.this.updateSwipeProgressFromOffset(view2, z);
                    SwipeHelper.this.mDismissPendingMap.remove(view2);
                    if (!this.mCancelled) {
                        SwipeHelper.this.mCallback.onChildDismissed(view2);
                    }
                    if (runnable != null) {
                        runnable.run();
                    }
                    if (!SwipeHelper.this.mDisableHwLayers) {
                        view2.setLayerType(0, null);
                    }
                    if (z2) {
                        systemUIEventScenario = ScenarioConstants.SCENARIO_CLEAR_ALL_NOTI;
                    } else {
                        systemUIEventScenario = ScenarioConstants.SCENARIO_CLEAR_NOTI;
                    }
                    ScenarioTrackUtil.finishScenario(systemUIEventScenario);
                }
            };
            anim4.addListener(r8);
            prepareDismissAnimation(view, anim4);
            this.mDismissPendingMap.put(view, anim4);
            anim4.start();
        }
    }

    /* access modifiers changed from: protected */
    public void prepareDismissAnimation(View view, Animator anim) {
    }

    public void snapChild(final View animView, final float targetLeft, float velocity) {
        final boolean canBeDismissed = this.mCallback.canChildBeDismissed(animView);
        Animator anim = getViewTranslationAnimator(animView, targetLeft, new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                SwipeHelper.this.onTranslationUpdate(animView, ((Float) animation.getAnimatedValue()).floatValue(), canBeDismissed);
            }
        });
        if (anim != null) {
            anim.setDuration((long) 150);
            anim.addListener(new AnimatorListenerAdapter() {
                boolean wasCancelled = false;

                public void onAnimationCancel(Animator animator) {
                    this.wasCancelled = true;
                }

                public void onAnimationEnd(Animator animator) {
                    boolean unused = SwipeHelper.this.mSnappingChild = false;
                    if (!this.wasCancelled) {
                        SwipeHelper.this.updateSwipeProgressFromOffset(animView, canBeDismissed);
                        SwipeHelper.this.mCallback.onChildSnappedBack(animView, targetLeft);
                    }
                }
            });
            prepareSnapBackAnimation(animView, anim);
            this.mSnappingChild = true;
            anim.start();
        }
    }

    /* access modifiers changed from: protected */
    public void prepareSnapBackAnimation(View view, Animator anim) {
    }

    public void onDownUpdate(View currView, MotionEvent ev) {
    }

    /* access modifiers changed from: protected */
    public void onMoveUpdate(View view, MotionEvent ev, float totalTranslation, float delta) {
    }

    public void onTranslationUpdate(View animView, float value, boolean canBeDismissed) {
        updateSwipeProgressFromOffset(animView, canBeDismissed, value);
    }

    private void snapChildInstantly(View view) {
        boolean canAnimViewBeDismissed = this.mCallback.canChildBeDismissed(view);
        setTranslation(view, 0.0f);
        updateSwipeProgressFromOffset(view, canAnimViewBeDismissed);
    }

    public void snapChildIfNeeded(View view, boolean animate, float targetLeft) {
        if ((!this.mDragging || this.mCurrView != view) && !this.mSnappingChild) {
            boolean needToSnap = false;
            Animator dismissPendingAnim = this.mDismissPendingMap.get(view);
            if (dismissPendingAnim != null) {
                needToSnap = true;
                dismissPendingAnim.cancel();
            } else if (getTranslation(view) != 0.0f) {
                needToSnap = true;
            }
            if (needToSnap) {
                if (animate) {
                    snapChild(view, targetLeft, 0.0f);
                } else {
                    snapChildInstantly(view);
                }
            }
        }
    }

    public boolean onTouchEvent(MotionEvent ev) {
        if (this.mLongPressSent) {
            return true;
        }
        if (this.mDragging) {
            this.mVelocityTracker.addMovement(ev);
            switch (ev.getAction()) {
                case 1:
                case 3:
                    if (this.mCurrView != null) {
                        this.mVelocityTracker.computeCurrentVelocity(1000, getMaxVelocity());
                        float velocity = getVelocity(this.mVelocityTracker);
                        if (!handleUpEvent(ev, this.mCurrView, velocity, getTranslation(this.mCurrView))) {
                            if (isDismissGesture(ev)) {
                                ScenarioTrackUtil.beginScenario(ScenarioConstants.SCENARIO_CLEAR_NOTI);
                                dismissChild(this.mCurrView, velocity, !swipedFastEnough());
                            } else {
                                this.mCallback.onDragCancelled(this.mCurrView);
                                snapChild(this.mCurrView, 0.0f, velocity);
                            }
                            this.mCurrView = null;
                        }
                        this.mDragging = false;
                        break;
                    }
                    break;
                case 2:
                case 4:
                    if (this.mCurrView != null) {
                        float delta = getPos(ev) - this.mInitialTouchPos;
                        float absDelta = Math.abs(delta);
                        if (absDelta >= ((float) getFalsingThreshold())) {
                            this.mTouchAboveFalsingThreshold = true;
                        }
                        if (!this.mCallback.canChildBeDismissed(this.mCurrView)) {
                            float size = getSize(this.mCurrView);
                            float maxScrollDistance = 0.3f * size;
                            if (absDelta >= size) {
                                delta = delta > 0.0f ? maxScrollDistance : -maxScrollDistance;
                            } else {
                                delta = maxScrollDistance * ((float) Math.sin(((double) (delta / size)) * 1.5707963267948966d));
                            }
                        }
                        float translation = this.mTranslation + delta;
                        if (translation < 0.0f && !NotificationStackScrollLayout.isPinnedHeadsUp(this.mCurrView)) {
                            translation = Math.max(translation, (float) (0 - this.mCurrView.getResources().getDimensionPixelSize(R.dimen.notification_block_width)));
                        }
                        setTranslation(this.mCurrView, translation);
                        updateSwipeProgressFromOffset(this.mCurrView, this.mCanCurrViewBeDimissed);
                        onMoveUpdate(this.mCurrView, ev, translation, delta);
                        break;
                    }
                    break;
            }
            return true;
        } else if (this.mCallback.getChildAtPosition(ev) != null) {
            onInterceptTouchEvent(ev);
            return true;
        } else {
            removeLongPressCallback();
            return false;
        }
    }

    private int getFalsingThreshold() {
        return (int) (((float) this.mFalsingThreshold) * this.mCallback.getFalsingThresholdFactor());
    }

    private float getMaxVelocity() {
        return 4000.0f * this.mDensityScale;
    }

    /* access modifiers changed from: protected */
    public float getEscapeVelocity() {
        return getUnscaledEscapeVelocity() * this.mDensityScale;
    }

    /* access modifiers changed from: protected */
    public float getUnscaledEscapeVelocity() {
        return 500.0f;
    }

    /* access modifiers changed from: protected */
    public long getMaxEscapeAnimDuration() {
        return 400;
    }

    /* access modifiers changed from: protected */
    public boolean swipedFarEnough() {
        return Math.abs(getTranslation(this.mCurrView)) > 0.6f * getSize(this.mCurrView);
    }

    public boolean isDismissGesture(MotionEvent ev) {
        if (ev.getActionMasked() != 1 || isFalseGesture(ev) || ((!swipedFastEnough() && !swipedFarEnough()) || !this.mCallback.canChildBeDismissed(this.mCurrView))) {
            return false;
        }
        return true;
    }

    public boolean isFalseGesture(MotionEvent ev) {
        boolean falsingDetected = this.mCallback.isAntiFalsingNeeded();
        boolean falsingDetected2 = false;
        if (this.mFalsingManager.isClassiferEnabled()) {
            if (falsingDetected && this.mFalsingManager.isFalseTouch()) {
                falsingDetected2 = true;
            }
            return falsingDetected2;
        }
        if (falsingDetected && !this.mTouchAboveFalsingThreshold) {
            falsingDetected2 = true;
        }
        return falsingDetected2;
    }

    /* access modifiers changed from: protected */
    public boolean swipedFastEnough() {
        float velocity = getVelocity(this.mVelocityTracker);
        float translation = getTranslation(this.mCurrView);
        boolean ret = false;
        if (Math.abs(velocity) > getEscapeVelocity()) {
            if ((velocity > 0.0f) == (translation > 0.0f)) {
                ret = true;
            }
        }
        return ret;
    }

    /* access modifiers changed from: protected */
    public boolean handleUpEvent(MotionEvent ev, View animView, float velocity, float translation) {
        return false;
    }
}
