package com.android.systemui.statusbar;

import android.content.Context;
import android.content.res.Configuration;
import android.os.SystemProperties;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.ViewInvertHelper;
import com.android.systemui.statusbar.notification.NotificationUtils;
import com.android.systemui.statusbar.phone.NotificationIconContainer;
import com.android.systemui.statusbar.stack.AmbientState;
import com.android.systemui.statusbar.stack.AnimationProperties;
import com.android.systemui.statusbar.stack.ExpandableViewState;
import com.android.systemui.statusbar.stack.NotificationStackScrollLayout;
import com.android.systemui.statusbar.stack.StackScrollState;
import com.android.systemui.statusbar.stack.ViewState;

public class NotificationShelf extends ActivatableNotificationView implements View.OnLayoutChangeListener {
    private static final boolean ICON_ANMATIONS_WHILE_SCROLLING = SystemProperties.getBoolean("debug.icon_scroll_animations", true);
    private static final boolean USE_ANIMATIONS_WHEN_OPENING = SystemProperties.getBoolean("debug.icon_opening_animations", true);
    private AmbientState mAmbientState;
    /* access modifiers changed from: private */
    public boolean mAnimationsEnabled = true;
    private NotificationIconContainer mCollapsedIcons;
    private boolean mDark;
    private boolean mHasItemsInStableShelf;
    private boolean mHideBackground;
    private NotificationStackScrollLayout mHostLayout;
    private int mIconAppearTopPadding;
    private boolean mInteractive;
    private int mMaxLayoutHeight;
    private float mMaxShelfEnd;
    private boolean mNoAnimationsInThisFrame;
    private int mNotGoneIndex;
    private float mOpenedAmount;
    private int mPaddingBetweenElements;
    private int mRelativeOffset;
    private int mScrollFastThreshold;
    /* access modifiers changed from: private */
    public NotificationIconContainer mShelfIcons;
    private ShelfState mShelfState;
    private int mStatusBarHeight;
    private int mStatusBarPaddingStart;
    private int mStatusBarState;
    private int[] mTmp = new int[2];
    private ViewInvertHelper mViewInvertHelper;

    private class ShelfState extends ExpandableViewState {
        /* access modifiers changed from: private */
        public boolean hasItemsInStableShelf;
        /* access modifiers changed from: private */
        public float maxShelfEnd;
        /* access modifiers changed from: private */
        public float openedAmount;

        private ShelfState() {
        }

        public void applyToView(View view) {
            super.applyToView(view);
            NotificationShelf.this.setMaxShelfEnd(this.maxShelfEnd);
            NotificationShelf.this.setOpenedAmount(this.openedAmount);
            NotificationShelf.this.updateAppearance();
            NotificationShelf.this.setHasItemsInStableShelf(this.hasItemsInStableShelf);
            NotificationShelf.this.mShelfIcons.setAnimationsEnabled(NotificationShelf.this.mAnimationsEnabled);
        }

        public void animateTo(View child, AnimationProperties properties) {
            super.animateTo(child, properties);
            NotificationShelf.this.setMaxShelfEnd(this.maxShelfEnd);
            NotificationShelf.this.setOpenedAmount(this.openedAmount);
            NotificationShelf.this.updateAppearance();
            NotificationShelf.this.setHasItemsInStableShelf(this.hasItemsInStableShelf);
            NotificationShelf.this.mShelfIcons.setAnimationsEnabled(NotificationShelf.this.mAnimationsEnabled);
        }
    }

    public NotificationShelf(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mShelfIcons = (NotificationIconContainer) findViewById(R.id.content);
        this.mShelfIcons.setClipChildren(false);
        this.mShelfIcons.setClipToPadding(false);
        setClipToActualHeight(false);
        setClipChildren(false);
        setClipToPadding(false);
        this.mShelfIcons.setShowAllIcons(false);
        this.mViewInvertHelper = new ViewInvertHelper((View) this.mShelfIcons, 700);
        this.mShelfState = new ShelfState();
        initDimens();
    }

    public void bind(AmbientState ambientState, NotificationStackScrollLayout hostLayout) {
        this.mAmbientState = ambientState;
        this.mHostLayout = hostLayout;
    }

    private void initDimens() {
        this.mIconAppearTopPadding = getResources().getDimensionPixelSize(R.dimen.notification_icon_appear_padding);
        this.mStatusBarHeight = getResources().getDimensionPixelOffset(R.dimen.status_bar_height);
        this.mStatusBarPaddingStart = getResources().getDimensionPixelOffset(R.dimen.status_bar_padding_start);
        this.mPaddingBetweenElements = getResources().getDimensionPixelSize(R.dimen.notification_divider_height);
        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        layoutParams.height = getResources().getDimensionPixelOffset(R.dimen.notification_shelf_height);
        setLayoutParams(layoutParams);
        int padding = getResources().getDimensionPixelOffset(R.dimen.shelf_icon_container_padding);
        this.mShelfIcons.setPadding(padding, 0, padding, 0);
        this.mScrollFastThreshold = getResources().getDimensionPixelOffset(R.dimen.scroll_fast_threshold);
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        initDimens();
    }

    public void setDark(boolean dark, boolean fade, long delay) {
        super.setDark(dark, fade, delay);
        if (this.mDark != dark) {
            this.mDark = dark;
            this.mShelfIcons.setDark(dark, fade, delay);
            updateInteractiveness();
        }
    }

    /* access modifiers changed from: protected */
    public View getContentView() {
        return this.mShelfIcons;
    }

    public NotificationIconContainer getShelfIcons() {
        return this.mShelfIcons;
    }

    public ExpandableViewState createNewViewState(StackScrollState stackScrollState) {
        return this.mShelfState;
    }

    public void updateState(StackScrollState resultState, AmbientState ambientState) {
        View lastView = ambientState.getLastVisibleBackgroundChild();
        if (lastView != null) {
            float maxShelfEnd = ((float) ambientState.getInnerHeight()) + ambientState.getTopPadding() + ambientState.getStackTranslation();
            ExpandableViewState lastViewState = resultState.getViewStateForView(lastView);
            float viewEnd = lastViewState.yTranslation + ((float) lastViewState.height);
            this.mShelfState.copyFrom(lastViewState);
            this.mShelfState.height = getIntrinsicHeight();
            this.mShelfState.yTranslation = Math.max(Math.min(viewEnd, maxShelfEnd) - ((float) this.mShelfState.height), getFullyClosedTranslation());
            this.mShelfState.zTranslation = (float) ambientState.getBaseZHeight();
            float unused = this.mShelfState.openedAmount = Math.min(1.0f, (this.mShelfState.yTranslation - getFullyClosedTranslation()) / ((float) (getIntrinsicHeight() * 2)));
            this.mShelfState.clipTopAmount = 0;
            this.mShelfState.alpha = this.mAmbientState.hasPulsingNotifications() ? 0.0f : 1.0f;
            this.mShelfState.belowSpeedBump = this.mAmbientState.getSpeedBumpIndex() == 0;
            this.mShelfState.shadowAlpha = 1.0f;
            this.mShelfState.hideSensitive = false;
            this.mShelfState.xTranslation = getTranslationX();
            if (this.mNotGoneIndex != -1) {
                this.mShelfState.notGoneIndex = Math.min(this.mShelfState.notGoneIndex, this.mNotGoneIndex);
            }
            boolean unused2 = this.mShelfState.hasItemsInStableShelf = lastViewState.inShelf;
            this.mShelfState.hidden = !this.mAmbientState.isShadeExpanded();
            float unused3 = this.mShelfState.maxShelfEnd = maxShelfEnd;
        } else {
            this.mShelfState.hidden = true;
            this.mShelfState.location = 64;
            boolean unused4 = this.mShelfState.hasItemsInStableShelf = false;
        }
        this.mShelfState.hidden = true;
    }

    /* JADX WARNING: type inference failed for: r1v7, types: [android.view.View] */
    /* JADX WARNING: Multi-variable type inference failed */
    /* JADX WARNING: Removed duplicated region for block: B:66:0x017f  */
    /* JADX WARNING: Removed duplicated region for block: B:70:0x0196  */
    /* JADX WARNING: Removed duplicated region for block: B:73:0x01a1  */
    /* JADX WARNING: Removed duplicated region for block: B:76:0x01ab  */
    /* JADX WARNING: Removed duplicated region for block: B:79:0x01bb  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void updateAppearance() {
        /*
            r37 = this;
            r7 = r37
            com.android.systemui.statusbar.phone.NotificationIconContainer r0 = r7.mShelfIcons
            r0.resetViewStates()
            float r8 = r37.getTranslationY()
            r0 = 0
            com.android.systemui.statusbar.stack.AmbientState r1 = r7.mAmbientState
            com.android.systemui.statusbar.ExpandableView r9 = r1.getLastVisibleBackgroundChild()
            r10 = -1
            r7.mNotGoneIndex = r10
            int r1 = r7.mMaxLayoutHeight
            int r2 = r37.getIntrinsicHeight()
            int r2 = r2 * 2
            int r1 = r1 - r2
            float r11 = (float) r1
            r1 = 0
            int r2 = (r8 > r11 ? 1 : (r8 == r11 ? 0 : -1))
            r12 = 1065353216(0x3f800000, float:1.0)
            if (r2 < 0) goto L_0x0032
            float r2 = r8 - r11
            int r3 = r37.getIntrinsicHeight()
            float r3 = (float) r3
            float r2 = r2 / r3
            float r1 = java.lang.Math.min(r12, r2)
        L_0x0032:
            r13 = r1
            r1 = 0
            r2 = 0
            r6 = 0
            r3 = 0
            boolean r4 = r7.mHideBackground
            if (r4 == 0) goto L_0x0044
            com.android.systemui.statusbar.NotificationShelf$ShelfState r4 = r7.mShelfState
            boolean r4 = r4.hasItemsInStableShelf
            if (r4 != 0) goto L_0x0044
            r3 = 1
        L_0x0044:
            r14 = r3
            r15 = 0
            r16 = 0
            r17 = 0
            com.android.systemui.statusbar.stack.AmbientState r3 = r7.mAmbientState
            float r18 = r3.getCurrentScrollVelocity()
            int r3 = r7.mScrollFastThreshold
            float r3 = (float) r3
            int r3 = (r18 > r3 ? 1 : (r18 == r3 ? 0 : -1))
            r19 = 1
            if (r3 > 0) goto L_0x0075
            com.android.systemui.statusbar.stack.AmbientState r3 = r7.mAmbientState
            boolean r3 = r3.isExpansionChanging()
            if (r3 == 0) goto L_0x0073
            com.android.systemui.statusbar.stack.AmbientState r3 = r7.mAmbientState
            float r3 = r3.getExpandingVelocity()
            float r3 = java.lang.Math.abs(r3)
            int r4 = r7.mScrollFastThreshold
            float r4 = (float) r4
            int r3 = (r3 > r4 ? 1 : (r3 == r4 ? 0 : -1))
            if (r3 <= 0) goto L_0x0073
            goto L_0x0075
        L_0x0073:
            r4 = 0
            goto L_0x0077
        L_0x0075:
            r4 = r19
        L_0x0077:
            r3 = 0
            int r20 = (r18 > r3 ? 1 : (r18 == r3 ? 0 : -1))
            if (r20 <= 0) goto L_0x007f
            r20 = r19
            goto L_0x0081
        L_0x007f:
            r20 = 0
        L_0x0081:
            r12 = r3
            r3 = r20
            com.android.systemui.statusbar.stack.AmbientState r5 = r7.mAmbientState
            boolean r5 = r5.isExpansionChanging()
            if (r5 == 0) goto L_0x0097
            com.android.systemui.statusbar.stack.AmbientState r5 = r7.mAmbientState
            boolean r5 = r5.isPanelTracking()
            if (r5 != 0) goto L_0x0097
            r5 = r19
            goto L_0x0098
        L_0x0097:
            r5 = 0
        L_0x0098:
            r12 = 0
            com.android.systemui.statusbar.stack.AmbientState r12 = r7.mAmbientState
            int r12 = r12.getBaseZHeight()
            r22 = r17
            r36 = r15
            r15 = r0
            r0 = r16
            r16 = r6
            r6 = r2
            r2 = r36
        L_0x00ab:
            com.android.systemui.statusbar.stack.NotificationStackScrollLayout r10 = r7.mHostLayout
            int r10 = r10.getChildCount()
            if (r1 >= r10) goto L_0x020c
            com.android.systemui.statusbar.stack.NotificationStackScrollLayout r10 = r7.mHostLayout
            int r17 = r1 + 1
            android.view.View r1 = r10.getChildAt(r1)
            r10 = r1
            com.android.systemui.statusbar.ExpandableView r10 = (com.android.systemui.statusbar.ExpandableView) r10
            int r1 = r10.getVisibility()
            r23 = r0
            r0 = 8
            if (r1 != r0) goto L_0x00c9
            goto L_0x00f0
        L_0x00c9:
            int r1 = r10.getViewType()
            r0 = 11
            if (r1 == r0) goto L_0x01de
            r0 = 12
            if (r1 == r0) goto L_0x01de
            r0 = 13
            if (r1 != r0) goto L_0x00ed
            r33 = r3
            r29 = r9
            r31 = r11
            r32 = r12
            r34 = r22
            r9 = r23
            r0 = 0
            r23 = r1
            r11 = r2
            r12 = r6
            r2 = 0
            goto L_0x01f0
        L_0x00ed:
            if (r1 == 0) goto L_0x00f6
        L_0x00f0:
            r1 = r17
            r0 = r23
        L_0x00f4:
            r10 = -1
            goto L_0x00ab
        L_0x00f6:
            r0 = r10
            com.android.systemui.statusbar.ExpandableNotificationRow r0 = (com.android.systemui.statusbar.ExpandableNotificationRow) r0
            float r20 = com.android.systemui.statusbar.stack.ViewState.getFinalTranslationZ(r0)
            r24 = r1
            float r1 = (float) r12
            int r1 = (r20 > r1 ? 1 : (r20 == r1 ? 0 : -1))
            if (r1 <= 0) goto L_0x0107
            r1 = r19
            goto L_0x0108
        L_0x0107:
            r1 = 0
        L_0x0108:
            r20 = r1
            if (r10 != r9) goto L_0x010f
            r1 = r19
            goto L_0x0110
        L_0x010f:
            r1 = 0
        L_0x0110:
            r21 = r1
            float r25 = r0.getTranslationY()
            if (r21 != 0) goto L_0x0150
            if (r20 != 0) goto L_0x0150
            if (r14 == 0) goto L_0x011f
            r26 = r2
            goto L_0x0152
        L_0x011f:
            int r1 = r37.getIntrinsicHeight()
            float r1 = (float) r1
            float r1 = r1 + r8
            r26 = r2
            int r2 = r7.mPaddingBetweenElements
            float r2 = (float) r2
            float r1 = r1 - r2
            float r2 = r1 - r25
            boolean r27 = r0.isBelowSpeedBump()
            if (r27 != 0) goto L_0x014b
            r28 = r1
            int r1 = r37.getNotificationMergeSize()
            float r1 = (float) r1
            int r1 = (r2 > r1 ? 1 : (r2 == r1 ? 0 : -1))
            if (r1 > 0) goto L_0x014d
            int r1 = r37.getNotificationMergeSize()
            float r1 = (float) r1
            float r1 = r25 + r1
            float r1 = java.lang.Math.min(r8, r1)
            goto L_0x0158
        L_0x014b:
            r28 = r1
        L_0x014d:
            r2 = r28
            goto L_0x0159
        L_0x0150:
            r26 = r2
        L_0x0152:
            int r1 = r37.getIntrinsicHeight()
            float r1 = (float) r1
            float r1 = r1 + r8
        L_0x0158:
            r2 = r1
        L_0x0159:
            r7.updateNotificationClipHeight(r0, r2)
            r1 = r0
            r29 = r9
            r9 = r23
            r0 = r7
            r30 = r1
            r23 = r24
            r28 = r2
            r31 = r11
            r11 = r26
            r2 = r13
            r32 = r12
            r12 = r6
            r6 = r21
            float r0 = r0.updateIconAppearance(r1, r2, r3, r4, r5, r6)
            float r15 = r15 + r0
            int r2 = r1.getBackgroundColorWithoutTint()
            int r6 = (r25 > r8 ? 1 : (r25 == r8 ? 0 : -1))
            if (r6 < 0) goto L_0x0196
            int r6 = r7.mNotGoneIndex
            r33 = r3
            r3 = -1
            if (r6 != r3) goto L_0x0193
            r7.mNotGoneIndex = r12
            r7.setTintColor(r9)
            r3 = r22
            r7.setOverrideTintColor(r11, r3)
            r34 = r3
            goto L_0x01a6
        L_0x0193:
            r3 = r22
            goto L_0x019a
        L_0x0196:
            r33 = r3
            r3 = r22
        L_0x019a:
            int r6 = r7.mNotGoneIndex
            r34 = r3
            r3 = -1
            if (r6 != r3) goto L_0x01a6
            r3 = r9
            r6 = r0
            r22 = r6
            goto L_0x01a9
        L_0x01a6:
            r3 = r11
            r22 = r34
        L_0x01a9:
            if (r21 == 0) goto L_0x01bb
            if (r16 != 0) goto L_0x01af
            r16 = r2
        L_0x01af:
            r6 = r16
            r1.setOverrideTintColor(r6, r0)
            r35 = r0
            r16 = r6
            r0 = 0
            r11 = 0
            goto L_0x01c5
        L_0x01bb:
            r6 = r2
            r35 = r0
            r0 = 0
            r11 = 0
            r1.setOverrideTintColor(r11, r0)
            r16 = r6
        L_0x01c5:
            if (r12 != 0) goto L_0x01c9
            if (r20 != 0) goto L_0x01cc
        L_0x01c9:
            r1.setAboveShelf(r11)
        L_0x01cc:
            int r6 = r12 + 1
            r1 = r2
            r0 = r1
            r2 = r3
            r1 = r17
            r9 = r29
            r11 = r31
            r12 = r32
            r3 = r33
            goto L_0x00f4
        L_0x01de:
            r33 = r3
            r29 = r9
            r31 = r11
            r32 = r12
            r34 = r22
            r9 = r23
            r0 = 0
            r23 = r1
            r11 = r2
            r12 = r6
            r2 = 0
        L_0x01f0:
            int r1 = r37.getIntrinsicHeight()
            float r1 = (float) r1
            float r1 = r1 + r8
            r7.updateExpandableViewClipHeight(r10, r1)
            int r6 = r12 + 1
            r0 = r9
            r2 = r11
            r1 = r17
            r9 = r29
            r11 = r31
            r12 = r32
            r3 = r33
            r22 = r34
            goto L_0x00f4
        L_0x020c:
            r33 = r3
            r29 = r9
            r31 = r11
            r32 = r12
            r34 = r22
            r9 = r0
            r11 = r2
            r12 = r6
            r2 = 0
            com.android.systemui.statusbar.phone.NotificationIconContainer r0 = r7.mShelfIcons
            com.android.systemui.statusbar.stack.AmbientState r3 = r7.mAmbientState
            int r3 = r3.getSpeedBumpIndex()
            r0.setSpeedBumpIndex(r3)
            com.android.systemui.statusbar.phone.NotificationIconContainer r0 = r7.mShelfIcons
            r0.calculateIconTranslations()
            com.android.systemui.statusbar.phone.NotificationIconContainer r0 = r7.mShelfIcons
            r0.applyIconStates()
            r0 = 1065353216(0x3f800000, float:1.0)
            int r0 = (r15 > r0 ? 1 : (r15 == r0 ? 0 : -1))
            if (r0 >= 0) goto L_0x0238
            r0 = r19
            goto L_0x0239
        L_0x0238:
            r0 = r2
        L_0x0239:
            if (r0 != 0) goto L_0x023f
            if (r14 == 0) goto L_0x023e
            goto L_0x023f
        L_0x023e:
            goto L_0x0241
        L_0x023f:
            r2 = r19
        L_0x0241:
            r7.setHideBackground(r2)
            int r2 = r7.mNotGoneIndex
            r3 = -1
            if (r2 != r3) goto L_0x024b
            r7.mNotGoneIndex = r12
        L_0x024b:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.NotificationShelf.updateAppearance():void");
    }

    private void updateNotificationClipHeight(ExpandableNotificationRow row, float notificationClipEnd) {
        float viewEnd = row.getTranslationY() + ((float) row.getActualHeight());
        boolean isPinned = row.isPinned() || row.isHeadsUpAnimatingAway();
        if (viewEnd <= notificationClipEnd || (!this.mAmbientState.isShadeExpanded() && isPinned)) {
            row.setClipBottomAmount(0);
            return;
        }
        int clipBottomAmount = (int) (viewEnd - notificationClipEnd);
        if (isPinned) {
            clipBottomAmount = Math.min(row.getIntrinsicHeight() - row.getCollapsedHeight(), clipBottomAmount);
        }
        row.setClipBottomAmount(clipBottomAmount);
    }

    private void updateExpandableViewClipHeight(ExpandableView view, float expandableViewClipEnd) {
        float viewEnd = view.getTranslationY() + ((float) view.getActualHeight());
        if (viewEnd <= expandableViewClipEnd || !this.mAmbientState.isShadeExpanded()) {
            view.setClipBottomAmount(0);
        } else {
            view.setClipBottomAmount((int) (viewEnd - expandableViewClipEnd));
        }
    }

    private float updateIconAppearance(ExpandableNotificationRow row, float expandAmount, boolean scrolling, boolean scrollingFast, boolean expandingAnimated, boolean isLastChild) {
        float fullTransitionAmount;
        float fullTransitionAmount2;
        float f;
        float fullTransitionAmount3;
        float f2 = expandAmount;
        float viewStart = row.getTranslationY();
        int fullHeight = row.getActualHeight() + this.mPaddingBetweenElements;
        float iconTransformDistance = ((float) getIntrinsicHeight()) * 1.5f * NotificationUtils.interpolate(1.0f, 1.5f, f2);
        if (isLastChild) {
            fullHeight = Math.min(fullHeight, row.getMinHeight() - getIntrinsicHeight());
            iconTransformDistance = Math.min(iconTransformDistance, (float) (row.getMinHeight() - getIntrinsicHeight()));
        }
        int fullHeight2 = fullHeight;
        float iconTransformDistance2 = iconTransformDistance;
        float shelfStart = getTranslationY();
        if (viewStart + ((float) fullHeight2) < shelfStart || ((this.mAmbientState.isUnlockHintRunning() && !row.isInShelf()) || (!this.mAmbientState.isShadeExpanded() && (row.isPinned() || row.isHeadsUpAnimatingAway())))) {
            fullTransitionAmount3 = 0.0f;
            f = 0.0f;
        } else if (viewStart < shelfStart) {
            float fullAmount = (shelfStart - viewStart) / ((float) fullHeight2);
            fullTransitionAmount = 1.0f - NotificationUtils.interpolate(Interpolators.ACCELERATE_DECELERATE.getInterpolation(fullAmount), fullAmount, f2);
            fullTransitionAmount2 = 1.0f - Math.min(1.0f, (shelfStart - viewStart) / iconTransformDistance2);
            updateIconPositioning(row, fullTransitionAmount2, fullTransitionAmount, iconTransformDistance2, scrolling, scrollingFast, expandingAnimated, isLastChild);
            return fullTransitionAmount;
        } else {
            fullTransitionAmount3 = 1.0f;
            f = 1.0f;
        }
        fullTransitionAmount = fullTransitionAmount3;
        fullTransitionAmount2 = f;
        updateIconPositioning(row, fullTransitionAmount2, fullTransitionAmount, iconTransformDistance2, scrolling, scrollingFast, expandingAnimated, isLastChild);
        return fullTransitionAmount;
    }

    private void updateIconPositioning(ExpandableNotificationRow row, float iconTransitionAmount, float fullTransitionAmount, float iconTransformDistance, boolean scrolling, boolean scrollingFast, boolean expandingAnimated, boolean isLastChild) {
        float transitionAmount;
        boolean z = isLastChild;
        StatusBarIconView icon = row.getEntry().expandedIcon;
        NotificationIconContainer.IconState iconState = getIconState(icon);
        if (iconState != null) {
            float f = 0.0f;
            float clampedAmount = iconTransitionAmount > 0.5f ? 1.0f : 0.0f;
            if (clampedAmount == fullTransitionAmount) {
                iconState.noAnimations = scrollingFast || expandingAnimated;
                iconState.useFullTransitionAmount = iconState.noAnimations || (!ICON_ANMATIONS_WHILE_SCROLLING && fullTransitionAmount == 0.0f && scrolling);
                iconState.useLinearTransitionAmount = !ICON_ANMATIONS_WHILE_SCROLLING && fullTransitionAmount == 0.0f && !this.mAmbientState.isExpansionChanging();
                iconState.translateContent = (((float) this.mMaxLayoutHeight) - getTranslationY()) - ((float) getIntrinsicHeight()) > 0.0f;
            }
            if (scrollingFast || (expandingAnimated && iconState.useFullTransitionAmount && !ViewState.isAnimatingY(icon))) {
                iconState.cancelAnimations(icon);
                iconState.useFullTransitionAmount = true;
                iconState.noAnimations = true;
            }
            if (z || !USE_ANIMATIONS_WHEN_OPENING || iconState.useFullTransitionAmount || iconState.useLinearTransitionAmount) {
                transitionAmount = iconTransitionAmount;
            } else {
                transitionAmount = clampedAmount;
                iconState.needsCannedAnimation = iconState.clampedAppearAmount != clampedAmount && !this.mNoAnimationsInThisFrame;
            }
            float transitionAmount2 = transitionAmount;
            iconState.iconAppearAmount = (!USE_ANIMATIONS_WHEN_OPENING || iconState.useFullTransitionAmount) ? fullTransitionAmount : transitionAmount2;
            iconState.clampedAppearAmount = clampedAmount;
            if (!row.isAboveShelf() && (z || iconState.translateContent)) {
                f = iconTransitionAmount;
            }
            float contentTransformationAmount = f;
            ExpandableNotificationRow expandableNotificationRow = row;
            expandableNotificationRow.setContentTransformationAmount(contentTransformationAmount, z);
            float f2 = contentTransformationAmount;
            setIconTransformationAmount(expandableNotificationRow, transitionAmount2, iconTransformDistance, clampedAmount != transitionAmount2, z);
        }
    }

    private void setIconTransformationAmount(ExpandableNotificationRow row, float transitionAmount, float iconTransformDistance, boolean usingLinearInterpolation, boolean isLastChild) {
        int iconTopPadding;
        StatusBarIconView icon = row.getEntry().expandedIcon;
        NotificationIconContainer.IconState iconState = getIconState(icon);
        View rowIcon = row.getNotificationIcon();
        float notificationIconPosition = row.getTranslationY() + row.getContentTranslation();
        boolean stayingInShelf = row.isInShelf() && !row.isTransformingIntoShelf();
        if (usingLinearInterpolation && !stayingInShelf) {
            notificationIconPosition = getTranslationY() - iconTransformDistance;
        }
        if (rowIcon != null) {
            iconTopPadding = row.getRelativeTopPadding(rowIcon);
        } else {
            ExpandableNotificationRow expandableNotificationRow = row;
            iconTopPadding = this.mIconAppearTopPadding;
        }
        float f = transitionAmount;
        float iconYTranslation = NotificationUtils.interpolate((notificationIconPosition + ((float) iconTopPadding)) - ((getTranslationY() + ((float) icon.getTop())) + (((1.0f - icon.getIconScale()) * ((float) icon.getHeight())) / 2.0f)), 0.0f, f);
        float alpha = 1.0f;
        if (!row.isShowingIcon()) {
            alpha = f;
        }
        if (iconState != null) {
            iconState.scaleX = 1.0f;
            iconState.scaleY = iconState.scaleX;
            iconState.hidden = true;
            iconState.alpha = alpha;
            iconState.yTranslation = iconYTranslation;
            if (stayingInShelf) {
                iconState.iconAppearAmount = 1.0f;
                iconState.alpha = 1.0f;
                iconState.scaleX = 1.0f;
                iconState.scaleY = 1.0f;
                iconState.hidden = false;
            }
            if (row.isAboveShelf() || (!row.isInShelf() && ((isLastChild && row.areGutsExposed()) || row.getTranslationZ() > ((float) this.mAmbientState.getBaseZHeight())))) {
                iconState.hidden = true;
            }
            iconState.iconColor = 0;
        }
    }

    private NotificationIconContainer.IconState getIconState(StatusBarIconView icon) {
        return this.mShelfIcons.getIconState(icon);
    }

    private float getFullyClosedTranslation() {
        return (float) ((-(getIntrinsicHeight() - this.mStatusBarHeight)) / 2);
    }

    public int getNotificationMergeSize() {
        return getIntrinsicHeight();
    }

    public boolean hasNoContentHeight() {
        return true;
    }

    private void setHideBackground(boolean hideBackground) {
        if (this.mHideBackground != hideBackground) {
            this.mHideBackground = hideBackground;
            updateBackground();
            updateOutline();
        }
    }

    /* access modifiers changed from: protected */
    public boolean needsOutline() {
        return !this.mHideBackground && super.needsOutline();
    }

    /* access modifiers changed from: protected */
    public boolean shouldHideBackground() {
        return super.shouldHideBackground() || this.mHideBackground;
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        updateRelativeOffset();
    }

    private void updateRelativeOffset() {
        this.mCollapsedIcons.getLocationOnScreen(this.mTmp);
        this.mRelativeOffset = this.mTmp[0];
        getLocationOnScreen(this.mTmp);
        this.mRelativeOffset -= this.mTmp[0];
    }

    /* access modifiers changed from: private */
    public void setOpenedAmount(float openedAmount) {
        this.mNoAnimationsInThisFrame = openedAmount == 1.0f && this.mOpenedAmount == 0.0f;
        this.mOpenedAmount = openedAmount;
        if (!this.mAmbientState.isPanelFullWidth()) {
            openedAmount = 1.0f;
        }
        int start = this.mRelativeOffset;
        if (isLayoutRtl()) {
            start = (getWidth() - start) - this.mCollapsedIcons.getWidth();
        }
        this.mShelfIcons.setActualLayoutWidth((int) NotificationUtils.interpolate((float) (this.mCollapsedIcons.getWidth() + start), (float) this.mShelfIcons.getWidth(), openedAmount));
        boolean hasOverflow = this.mCollapsedIcons.hasOverflow();
        int collapsedPadding = this.mCollapsedIcons.getPaddingEnd();
        if (!hasOverflow) {
            collapsedPadding = (int) (((float) collapsedPadding) - (1.0f * ((float) this.mCollapsedIcons.getIconSize())));
        }
        this.mShelfIcons.setActualPaddingEnd(NotificationUtils.interpolate((float) collapsedPadding, (float) this.mShelfIcons.getPaddingEnd(), openedAmount));
        this.mShelfIcons.setActualPaddingStart(NotificationUtils.interpolate((float) start, (float) this.mShelfIcons.getPaddingStart(), openedAmount));
        this.mShelfIcons.setOpenedAmount(openedAmount);
        this.mShelfIcons.setVisualOverflowAdaption(this.mCollapsedIcons.getVisualOverflowAdaption());
    }

    public void setMaxLayoutHeight(int maxLayoutHeight) {
        this.mMaxLayoutHeight = maxLayoutHeight;
    }

    public int getNotGoneIndex() {
        return this.mNotGoneIndex;
    }

    /* access modifiers changed from: private */
    public void setHasItemsInStableShelf(boolean hasItemsInStableShelf) {
        if (this.mHasItemsInStableShelf != hasItemsInStableShelf) {
            this.mHasItemsInStableShelf = hasItemsInStableShelf;
            updateInteractiveness();
        }
    }

    public boolean hasItemsInStableShelf() {
        return this.mHasItemsInStableShelf;
    }

    public void setCollapsedIcons(NotificationIconContainer collapsedIcons) {
        this.mCollapsedIcons = collapsedIcons;
        this.mCollapsedIcons.addOnLayoutChangeListener(this);
    }

    public void setStatusBarState(int statusBarState) {
        if (this.mStatusBarState != statusBarState) {
            this.mStatusBarState = statusBarState;
            updateInteractiveness();
        }
    }

    private void updateInteractiveness() {
        int i = 1;
        this.mInteractive = this.mStatusBarState == 1 && this.mHasItemsInStableShelf && !this.mDark;
        setClickable(this.mInteractive);
        setFocusable(this.mInteractive);
        if (!this.mInteractive) {
            i = 4;
        }
        setImportantForAccessibility(i);
    }

    /* access modifiers changed from: protected */
    public boolean isInteractive() {
        return this.mInteractive;
    }

    public void setMaxShelfEnd(float maxShelfEnd) {
        this.mMaxShelfEnd = maxShelfEnd;
    }

    public void setAnimationsEnabled(boolean enabled) {
        this.mAnimationsEnabled = enabled;
        this.mCollapsedIcons.setAnimationsEnabled(enabled);
        if (!enabled) {
            this.mShelfIcons.setAnimationsEnabled(false);
        }
    }

    public boolean hasOverlappingRendering() {
        return false;
    }

    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        if (this.mInteractive) {
            info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_EXPAND);
            info.addAction(new AccessibilityNodeInfo.AccessibilityAction(16, getContext().getString(R.string.accessibility_overflow_action)));
        }
    }

    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        updateRelativeOffset();
    }
}
