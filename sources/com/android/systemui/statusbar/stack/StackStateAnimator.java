package com.android.systemui.statusbar.stack;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.util.Property;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.miui.statusbar.notification.NotificationUtil;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.ExpandableView;
import com.android.systemui.statusbar.NotificationShelf;
import com.android.systemui.statusbar.stack.NotificationStackScrollLayout;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;

public class StackStateAnimator {
    /* access modifiers changed from: private */
    public AnimationFilter mAnimationFilter = new AnimationFilter();
    /* access modifiers changed from: private */
    public Stack<AnimatorListenerAdapter> mAnimationListenerPool = new Stack<>();
    private final AnimationProperties mAnimationProperties;
    /* access modifiers changed from: private */
    public HashSet<Animator> mAnimatorSet = new HashSet<>();
    /* access modifiers changed from: private */
    public ValueAnimator mBottomOverScrollAnimator;
    private ArrayList<View> mChildrenToClearFromOverlay = new ArrayList<>();
    private long mCurrentAdditionalDelay;
    private int mCurrentLastNotAddedIndex;
    private long mCurrentLength;
    /* access modifiers changed from: private */
    public HashSet<View> mDismissChildren = new HashSet<>();
    private final int mGoToFullShadeAppearingTranslation;
    /* access modifiers changed from: private */
    public HashSet<View> mHeadsUpAppearChildren = new HashSet<>();
    private int mHeadsUpAppearHeightBottom;
    /* access modifiers changed from: private */
    public HashSet<View> mHeadsUpDisappearChildren = new HashSet<>();
    public NotificationStackScrollLayout mHostLayout;
    /* access modifiers changed from: private */
    public ArrayList<View> mNewAddChildren = new ArrayList<>();
    private ArrayList<NotificationStackScrollLayout.AnimationEvent> mNewEvents = new ArrayList<>();
    /* access modifiers changed from: private */
    public HashSet<View> mPopupChildren = new HashSet<>();
    private boolean mShadeExpanded;
    private NotificationShelf mShelf;
    private final ExpandableViewState mTmpState = new ExpandableViewState();
    /* access modifiers changed from: private */
    public ValueAnimator mTopOverScrollAnimator;

    public StackStateAnimator(NotificationStackScrollLayout hostLayout) {
        this.mHostLayout = hostLayout;
        this.mGoToFullShadeAppearingTranslation = hostLayout.getContext().getResources().getDimensionPixelSize(R.dimen.go_to_full_shade_appearing_translation);
        this.mAnimationProperties = new AnimationProperties() {
            public AnimationFilter getAnimationFilter() {
                return StackStateAnimator.this.mAnimationFilter;
            }

            public AnimatorListenerAdapter getAnimationFinishListener() {
                return StackStateAnimator.this.getGlobalAnimationFinishedListener();
            }

            public boolean wasAdded(View view) {
                return StackStateAnimator.this.mNewAddChildren.contains(view);
            }

            public Interpolator getCustomInterpolator(View child, Property property) {
                if (StackStateAnimator.this.mHeadsUpAppearChildren.contains(child) && View.TRANSLATION_Y.equals(property)) {
                    return Interpolators.HEADS_UP_APPEAR;
                }
                if (StackStateAnimator.this.mHeadsUpDisappearChildren.contains(child) && View.TRANSLATION_Y.equals(property)) {
                    return Interpolators.HEADS_UP_DISAPPEAR;
                }
                if (StackStateAnimator.this.mPopupChildren.contains(child)) {
                    return Interpolators.BACK_EASE_OUT;
                }
                if (StackStateAnimator.this.mDismissChildren.contains(child)) {
                    return Interpolators.DECELERATE_QUART;
                }
                return null;
            }
        };
    }

    public boolean isRunning() {
        return !this.mAnimatorSet.isEmpty();
    }

    public void startAnimationForEvents(ArrayList<NotificationStackScrollLayout.AnimationEvent> mAnimationEvents, StackScrollState finalState, long additionalDelay) {
        processAnimationEvents(mAnimationEvents, finalState);
        int childCount = this.mHostLayout.getChildCount();
        this.mAnimationFilter.applyCombination(this.mNewEvents);
        this.mCurrentAdditionalDelay = additionalDelay;
        this.mCurrentLength = NotificationStackScrollLayout.AnimationEvent.combineLength(this.mNewEvents);
        this.mCurrentLastNotAddedIndex = findLastNotAddedIndex(finalState);
        for (int i = 0; i < childCount; i++) {
            ExpandableView child = (ExpandableView) this.mHostLayout.getChildAt(i);
            ExpandableViewState viewState = finalState.getViewStateForView(child);
            if (!(viewState == null || child.getVisibility() == 8 || applyWithoutAnimation(child, viewState, finalState))) {
                initAnimationProperties(finalState, child, viewState);
                viewState.animateTo(child, this.mAnimationProperties);
            }
        }
        if (isRunning() == 0) {
            onAnimationFinished();
        }
        this.mHeadsUpAppearChildren.clear();
        this.mHeadsUpDisappearChildren.clear();
        this.mPopupChildren.clear();
        this.mDismissChildren.clear();
        this.mNewEvents.clear();
        this.mNewAddChildren.clear();
    }

    private void initAnimationProperties(StackScrollState finalState, ExpandableView child, ExpandableViewState viewState) {
        boolean wasAdded = this.mAnimationProperties.wasAdded(child);
        this.mAnimationProperties.duration = this.mCurrentLength;
        adaptDurationWhenGoingToFullShade(child, viewState, wasAdded);
        this.mAnimationProperties.delay = 0;
        if (!wasAdded) {
            if (!this.mAnimationFilter.hasDelays) {
                return;
            }
            if (viewState.yTranslation == child.getTranslationY() && viewState.zTranslation == child.getTranslationZ() && viewState.alpha == child.getAlpha() && viewState.height == child.getActualHeight() && viewState.clipTopAmount == child.getClipTopAmount() && viewState.dark == child.isDark() && viewState.shadowAlpha == child.getShadowAlpha()) {
                return;
            }
        }
        this.mAnimationProperties.delay = this.mCurrentAdditionalDelay + calculateChildAnimationDelay(viewState, finalState);
    }

    private void adaptDurationWhenGoingToFullShade(ExpandableView child, ExpandableViewState viewState, boolean wasAdded) {
        if (wasAdded && this.mAnimationFilter.hasGoToFullShadeEvent) {
            child.setTranslationY(child.getTranslationY() + ((float) this.mGoToFullShadeAppearingTranslation));
            this.mAnimationProperties.duration = 514 + ((long) (100.0f * ((float) Math.pow((double) ((float) (viewState.notGoneIndex - this.mCurrentLastNotAddedIndex)), 0.699999988079071d))));
        }
    }

    private boolean applyWithoutAnimation(ExpandableView child, ExpandableViewState viewState, StackScrollState finalState) {
        if (this.mShadeExpanded || ViewState.isAnimatingY(child) || this.mHeadsUpDisappearChildren.contains(child) || this.mHeadsUpAppearChildren.contains(child) || NotificationStackScrollLayout.isPinnedHeadsUp(child)) {
            return false;
        }
        viewState.applyToView(child);
        return true;
    }

    private int findLastNotAddedIndex(StackScrollState finalState) {
        for (int i = this.mHostLayout.getChildCount() - 1; i >= 0; i--) {
            ExpandableView child = (ExpandableView) this.mHostLayout.getChildAt(i);
            ExpandableViewState viewState = finalState.getViewStateForView(child);
            if (viewState != null && child.getVisibility() != 8 && !this.mNewAddChildren.contains(child)) {
                return viewState.notGoneIndex;
            }
        }
        return -1;
    }

    private long calculateChildAnimationDelay(ExpandableViewState viewState, StackScrollState finalState) {
        if (this.mAnimationFilter.hasGoToFullShadeEvent) {
            return calculateDelayGoToFullShade(viewState);
        }
        if (this.mAnimationFilter.hasHeadsUpDisappearClickEvent) {
            return 120;
        }
        long minDelay = 0;
        Iterator<NotificationStackScrollLayout.AnimationEvent> it = this.mNewEvents.iterator();
        while (it.hasNext()) {
            NotificationStackScrollLayout.AnimationEvent event = it.next();
            switch (event.animationType) {
                case 0:
                    int ownIndex = viewState.notGoneIndex;
                    if (finalState.getViewStateForView(event.changingView) != null && !NotificationUtil.isFoldAnimating()) {
                        minDelay = Math.max(((long) (2 - Math.max(0, Math.min(2, Math.abs(ownIndex - finalState.getViewStateForView(event.changingView).notGoneIndex) - 1)))) * 80, minDelay);
                        break;
                    }
                case 1:
                    break;
                case 2:
                    break;
            }
            minDelay = 0;
        }
        return minDelay;
    }

    private long calculateDelayGoToFullShade(ExpandableViewState viewState) {
        int shelfIndex = this.mShelf.getNotGoneIndex();
        float index = (float) viewState.notGoneIndex;
        long result = 0;
        if (index > ((float) shelfIndex)) {
            result = 0 + ((long) (((double) (((float) Math.pow((double) (index - ((float) shelfIndex)), 0.699999988079071d)) * 48.0f)) * 0.25d));
            index = (float) shelfIndex;
        }
        return result + ((long) (48.0f * ((float) Math.pow((double) index, 0.699999988079071d))));
    }

    /* access modifiers changed from: private */
    public AnimatorListenerAdapter getGlobalAnimationFinishedListener() {
        if (!this.mAnimationListenerPool.empty()) {
            return this.mAnimationListenerPool.pop();
        }
        return new AnimatorListenerAdapter() {
            private boolean mWasCancelled;

            public void onAnimationEnd(Animator animation) {
                StackStateAnimator.this.mAnimatorSet.remove(animation);
                if (StackStateAnimator.this.mAnimatorSet.isEmpty() && !this.mWasCancelled) {
                    StackStateAnimator.this.onAnimationFinished();
                }
                StackStateAnimator.this.mAnimationListenerPool.push(this);
            }

            public void onAnimationCancel(Animator animation) {
                this.mWasCancelled = true;
            }

            public void onAnimationStart(Animator animation) {
                this.mWasCancelled = false;
                StackStateAnimator.this.mAnimatorSet.add(animation);
            }
        };
    }

    /* access modifiers changed from: private */
    public void onAnimationFinished() {
        this.mHostLayout.onChildAnimationFinished();
        Iterator<View> it = this.mChildrenToClearFromOverlay.iterator();
        while (it.hasNext()) {
            removeFromOverlay(it.next());
        }
        this.mChildrenToClearFromOverlay.clear();
    }

    private void processAnimationEvents(ArrayList<NotificationStackScrollLayout.AnimationEvent> animationEvents, StackScrollState finalState) {
        long j;
        Iterator<NotificationStackScrollLayout.AnimationEvent> it = animationEvents.iterator();
        while (it.hasNext()) {
            NotificationStackScrollLayout.AnimationEvent event = it.next();
            final ExpandableView changingView = (ExpandableView) event.changingView;
            if (event.animationType == 0) {
                ExpandableViewState viewState = finalState.getViewStateForView(changingView);
                if (viewState != null) {
                    viewState.applyToView(changingView);
                    this.mNewAddChildren.add(changingView);
                }
            } else if (event.animationType == 1) {
                if (changingView.getVisibility() != 0) {
                    removeFromOverlay(changingView);
                } else {
                    ExpandableViewState viewState2 = finalState.getViewStateForView(event.viewAfterChangingView);
                    int actualHeight = changingView.getActualHeight();
                    float foldTranslationDirection = NotificationUtil.getFoldTranslationDirection(false, -1.0f);
                    if (!NotificationUtil.isFoldAnimating() && viewState2 != null) {
                        float ownPosition = changingView.getTranslationY();
                        if ((changingView instanceof ExpandableNotificationRow) && (event.viewAfterChangingView instanceof ExpandableNotificationRow)) {
                            ExpandableNotificationRow changingRow = (ExpandableNotificationRow) changingView;
                            ExpandableNotificationRow nextRow = (ExpandableNotificationRow) event.viewAfterChangingView;
                            if (changingRow.isRemoved() && changingRow.wasChildInGroupWhenRemoved() && !nextRow.isChildInGroup()) {
                                ownPosition = changingRow.getTranslationWhenRemoved();
                            }
                        }
                        foldTranslationDirection = Math.max(Math.min(((viewState2.yTranslation - ((((float) actualHeight) / 2.0f) + ownPosition)) * 2.0f) / ((float) actualHeight), 1.0f), -1.0f);
                    }
                    float translationDirection = foldTranslationDirection;
                    changingView.performRemoveAnimation(464, translationDirection, this.mAnimationProperties.getAnimationFinishListener(), new Runnable() {
                        public void run() {
                            StackStateAnimator.removeFromOverlay(changingView);
                        }
                    });
                }
            } else if (event.animationType == 2) {
                this.mHostLayout.getOverlay().remove(changingView);
                if (Math.abs(changingView.getTranslation()) == ((float) changingView.getWidth()) && changingView.getTransientContainer() != null) {
                    changingView.getTransientContainer().removeTransientView(changingView);
                }
            } else if (event.animationType == 13) {
                ((ExpandableNotificationRow) event.changingView).prepareExpansionChanged(finalState);
            } else if (event.animationType == 14) {
                this.mTmpState.copyFrom(finalState.getViewStateForView(changingView));
                if (event.headsUpFromBottom) {
                    this.mTmpState.yTranslation = (float) this.mHeadsUpAppearHeightBottom;
                } else {
                    this.mTmpState.yTranslation = (float) (-this.mTmpState.height);
                }
                this.mHeadsUpAppearChildren.add(changingView);
                this.mTmpState.applyToView(changingView);
            } else if (event.animationType == 15 || event.animationType == 16) {
                this.mHeadsUpDisappearChildren.add(changingView);
                if (changingView.getParent() == null) {
                    this.mHostLayout.getOverlay().add(changingView);
                    this.mTmpState.initFrom(changingView);
                    this.mTmpState.yTranslation = (float) (-changingView.getActualHeight());
                    this.mAnimationFilter.animateY = true;
                    AnimationProperties animationProperties = this.mAnimationProperties;
                    if (event.animationType == 16) {
                        j = 120;
                    } else {
                        j = 0;
                    }
                    animationProperties.delay = j;
                    this.mAnimationProperties.duration = 150;
                    this.mTmpState.animateTo(changingView, this.mAnimationProperties);
                    this.mChildrenToClearFromOverlay.add(changingView);
                }
            } else if (event.animationType == 18) {
                this.mPopupChildren.add(changingView);
            } else if (event.animationType == 19) {
                this.mDismissChildren.add(changingView);
            }
            this.mNewEvents.add(event);
        }
    }

    public static void removeFromOverlay(View changingView) {
        ViewGroup parent = (ViewGroup) changingView.getParent();
        if (parent != null) {
            parent.removeView(changingView);
        }
    }

    public void animateOverScrollToAmount(float targetAmount, final boolean onTop, final boolean isRubberbanded) {
        float startOverScrollAmount = this.mHostLayout.getCurrentOverScrollAmount(onTop);
        if (targetAmount != startOverScrollAmount) {
            cancelOverScrollAnimators(onTop);
            ValueAnimator overScrollAnimator = ValueAnimator.ofFloat(new float[]{startOverScrollAmount, targetAmount});
            overScrollAnimator.setDuration(360);
            overScrollAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    StackStateAnimator.this.mHostLayout.setOverScrollAmount(((Float) animation.getAnimatedValue()).floatValue(), onTop, false, false, isRubberbanded);
                }
            });
            overScrollAnimator.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
            overScrollAnimator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    if (onTop) {
                        ValueAnimator unused = StackStateAnimator.this.mTopOverScrollAnimator = null;
                    } else {
                        ValueAnimator unused2 = StackStateAnimator.this.mBottomOverScrollAnimator = null;
                    }
                }
            });
            overScrollAnimator.start();
            if (onTop) {
                this.mTopOverScrollAnimator = overScrollAnimator;
            } else {
                this.mBottomOverScrollAnimator = overScrollAnimator;
            }
        }
    }

    public void cancelOverScrollAnimators(boolean onTop) {
        ValueAnimator currentAnimator = onTop ? this.mTopOverScrollAnimator : this.mBottomOverScrollAnimator;
        if (currentAnimator != null) {
            currentAnimator.cancel();
        }
    }

    public void setHeadsUpAppearHeightBottom(int headsUpAppearHeightBottom) {
        this.mHeadsUpAppearHeightBottom = headsUpAppearHeightBottom;
    }

    public void setShadeExpanded(boolean shadeExpanded) {
        this.mShadeExpanded = shadeExpanded;
    }

    public void setShelf(NotificationShelf shelf) {
        this.mShelf = shelf;
    }
}
