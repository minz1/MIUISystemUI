package com.android.systemui.statusbar.stack;

import android.content.Context;
import android.view.NotificationHeaderView;
import android.view.View;
import android.view.ViewGroup;
import com.android.systemui.R;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.ExpandableView;
import com.android.systemui.statusbar.notification.NotificationUtils;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class StackScrollAlgorithm {
    private int mCollapsedSize;
    private int mHeadsUpMarginTop;
    private int mIncreasedPaddingBetweenElements;
    private boolean mIsExpanded;
    private boolean mIsExpandedBecauseOfHeadsUp;
    private OnAnimationRequestedListener mOnAnimationRequestedListener;
    /* access modifiers changed from: private */
    public int mPaddingBetweenElements;
    private int mStatusBarHeight;
    private StackScrollAlgorithmState mTempAlgorithmState = new StackScrollAlgorithmState();

    public interface OnAnimationRequestedListener {
        void onDismissAnimationRequested(ExpandableView expandableView);

        void onPopupAnimationRequested(ExpandableView expandableView);
    }

    public class StackScrollAlgorithmState {
        public final HashMap<ExpandableView, Float> paddingMap = new HashMap<>();
        public int scrollY;
        public final ArrayList<ExpandableView> visibleChildren = new ArrayList<>();

        public StackScrollAlgorithmState() {
        }

        public int getPaddingAfterChild(ExpandableView child) {
            Float padding = this.paddingMap.get(child);
            if (padding == null) {
                return StackScrollAlgorithm.this.mPaddingBetweenElements;
            }
            return (int) padding.floatValue();
        }
    }

    public StackScrollAlgorithm(Context context) {
        initView(context);
    }

    public void initView(Context context) {
        initConstants(context);
    }

    private void initConstants(Context context) {
        this.mPaddingBetweenElements = context.getResources().getDimensionPixelSize(R.dimen.notification_divider_height);
        this.mIncreasedPaddingBetweenElements = context.getResources().getDimensionPixelSize(R.dimen.notification_divider_height_increased);
        this.mCollapsedSize = context.getResources().getDimensionPixelSize(R.dimen.notification_min_height);
        this.mStatusBarHeight = context.getResources().getDimensionPixelSize(R.dimen.status_bar_height);
        this.mHeadsUpMarginTop = HeadsUpManager.getHeadsUpTopMargin(context);
    }

    public void setOnAnimationRequestedListener(OnAnimationRequestedListener listener) {
        this.mOnAnimationRequestedListener = listener;
    }

    public void getStackScrollState(AmbientState ambientState, StackScrollState resultState) {
        StackScrollAlgorithmState algorithmState = this.mTempAlgorithmState;
        resultState.resetViewStates();
        initAlgorithmState(resultState, algorithmState, ambientState);
        updatePositionsForState(resultState, algorithmState, ambientState);
        updateZValuesForState(resultState, algorithmState, ambientState);
        updateHeadsUpStates(resultState, algorithmState);
        handleDraggedViews(ambientState, resultState, algorithmState);
        updateDimmedActivatedHideSensitive(ambientState, resultState, algorithmState);
        updateClipping(resultState, algorithmState, ambientState);
        updateSpeedBumpState(resultState, algorithmState, ambientState);
        updateShelfState(resultState, ambientState);
        getNotificationChildrenStates(resultState, algorithmState, ambientState);
    }

    private void getNotificationChildrenStates(StackScrollState resultState, StackScrollAlgorithmState algorithmState, AmbientState ambientState) {
        StackScrollState stackScrollState = resultState;
        StackScrollAlgorithmState stackScrollAlgorithmState = algorithmState;
        int childCount = stackScrollAlgorithmState.visibleChildren.size();
        for (int i = 0; i < childCount; i++) {
            ExpandableView v = stackScrollAlgorithmState.visibleChildren.get(i);
            if (v instanceof ExpandableNotificationRow) {
                ExpandableNotificationRow row = (ExpandableNotificationRow) v;
                row.getChildrenStates(stackScrollState);
                if (!ambientState.isOnKeyguard()) {
                    ExpandableViewState parentState = stackScrollState.getViewStateForView(row);
                    if (row.areChildrenExpanded() && !row.isGroupExpansionChanging() && row.getChildrenContainer() != null) {
                        for (int j = 0; j < row.getChildrenContainer().getNotificationChildCount(); j++) {
                            View child = row.getChildrenContainer().getNotificationChildren().get(j);
                            updateNotificationFullyShown((ExpandableView) child, i + j, stackScrollState.getViewStateForView(child), ambientState, (parentState.yTranslation - ambientState.getTopPadding()) - ambientState.getStackTranslation());
                        }
                    }
                }
            }
        }
    }

    private void updateSpeedBumpState(StackScrollState resultState, StackScrollAlgorithmState algorithmState, AmbientState ambientState) {
        int childCount = algorithmState.visibleChildren.size();
        int belowSpeedBump = ambientState.getSpeedBumpIndex();
        int i = 0;
        while (i < childCount) {
            resultState.getViewStateForView(algorithmState.visibleChildren.get(i)).belowSpeedBump = i >= belowSpeedBump;
            i++;
        }
    }

    private void updateShelfState(StackScrollState resultState, AmbientState ambientState) {
        ambientState.getShelf().updateState(resultState, ambientState);
    }

    private void updateClipping(StackScrollState resultState, StackScrollAlgorithmState algorithmState, AmbientState ambientState) {
        StackScrollAlgorithmState stackScrollAlgorithmState = algorithmState;
        float drawStart = !ambientState.isOnKeyguard() ? ambientState.getTopPadding() + ambientState.getStackTranslation() : 0.0f;
        int childCount = stackScrollAlgorithmState.visibleChildren.size();
        float previousNotificationStart = 0.0f;
        float previousNotificationEnd = 0.0f;
        for (int i = 0; i < childCount; i++) {
            ExpandableView child = stackScrollAlgorithmState.visibleChildren.get(i);
            ExpandableViewState state = resultState.getViewStateForView(child);
            if (!child.mustStayOnScreen()) {
                previousNotificationEnd = Math.max(drawStart, previousNotificationEnd);
                previousNotificationStart = Math.max(drawStart, previousNotificationStart);
            }
            float newYTranslation = state.yTranslation;
            float newNotificationEnd = newYTranslation + ((float) state.height);
            boolean isHeadsUpAnimatingAway = true;
            boolean isHeadsUp = (child instanceof ExpandableNotificationRow) && ((ExpandableNotificationRow) child).isPinned();
            if (!(child instanceof ExpandableNotificationRow) || !((ExpandableNotificationRow) child).isHeadsUpAnimatingAway()) {
                isHeadsUpAnimatingAway = false;
            }
            if (state.inShelf || newYTranslation >= previousNotificationEnd || ((isHeadsUp || isHeadsUpAnimatingAway) && !ambientState.isShadeExpanded())) {
                state.clipTopAmount = 0;
            } else {
                state.clipTopAmount = (int) (previousNotificationEnd - newYTranslation);
            }
            if (!child.isTransparent()) {
                previousNotificationEnd = newNotificationEnd;
                previousNotificationStart = newYTranslation;
            }
        }
        StackScrollState stackScrollState = resultState;
    }

    public static boolean canChildBeDismissed(View v) {
        if (!(v instanceof ExpandableNotificationRow)) {
            return false;
        }
        ExpandableNotificationRow row = (ExpandableNotificationRow) v;
        if (row.areGutsExposed()) {
            return false;
        }
        return row.canViewBeDismissed();
    }

    private void updateDimmedActivatedHideSensitive(AmbientState ambientState, StackScrollState resultState, StackScrollAlgorithmState algorithmState) {
        StackScrollAlgorithmState stackScrollAlgorithmState = algorithmState;
        boolean dimmed = ambientState.isDimmed();
        boolean dark = ambientState.isDark();
        boolean hideSensitive = ambientState.isHideSensitive();
        View activatedChild = ambientState.getActivatedChild();
        int childCount = stackScrollAlgorithmState.visibleChildren.size();
        boolean hideSensitiveByAppLock = false;
        for (int i = 0; i < childCount; i++) {
            View child = stackScrollAlgorithmState.visibleChildren.get(i);
            if (child instanceof ExpandableNotificationRow) {
                hideSensitiveByAppLock = ((ExpandableNotificationRow) child).getEntry().hideSensitiveByAppLock;
            }
            ExpandableViewState childViewState = resultState.getViewStateForView(child);
            childViewState.dimmed = dimmed;
            childViewState.dark = dark;
            boolean isActivatedChild = true;
            childViewState.hideSensitive = hideSensitive || hideSensitiveByAppLock;
            if (activatedChild != child) {
                isActivatedChild = false;
            }
            if (dimmed && isActivatedChild) {
                childViewState.zTranslation += 2.0f * ((float) ambientState.getZDistanceBetweenElements());
            }
        }
        StackScrollState stackScrollState = resultState;
    }

    private void handleDraggedViews(AmbientState ambientState, StackScrollState resultState, StackScrollAlgorithmState algorithmState) {
        ArrayList<View> draggedViews = ambientState.getDraggedViews();
        Iterator<View> it = draggedViews.iterator();
        while (it.hasNext()) {
            View draggedView = it.next();
            int childIndex = algorithmState.visibleChildren.indexOf(draggedView);
            if (childIndex >= 0 && childIndex < algorithmState.visibleChildren.size() - 1) {
                View nextChild = algorithmState.visibleChildren.get(childIndex + 1);
                if (!draggedViews.contains(nextChild)) {
                    ExpandableViewState viewState = resultState.getViewStateForView(nextChild);
                    if (ambientState.isShadeExpanded()) {
                        viewState.shadowAlpha = 1.0f;
                        viewState.hidden = false;
                    }
                }
                resultState.getViewStateForView(draggedView).alpha = draggedView.getAlpha();
            }
        }
    }

    private void initAlgorithmState(StackScrollState resultState, StackScrollAlgorithmState state, AmbientState ambientState) {
        StackScrollAlgorithm stackScrollAlgorithm = this;
        StackScrollState stackScrollState = resultState;
        StackScrollAlgorithmState stackScrollAlgorithmState = state;
        int i = 0;
        stackScrollAlgorithmState.scrollY = (int) (((float) Math.max(0, ambientState.getScrollY())) + ambientState.getOverScrollAmount(false));
        ViewGroup hostView = resultState.getHostView();
        int childCount = hostView.getChildCount();
        stackScrollAlgorithmState.visibleChildren.clear();
        stackScrollAlgorithmState.visibleChildren.ensureCapacity(childCount);
        stackScrollAlgorithmState.paddingMap.clear();
        int notGoneIndex = 0;
        ExpandableView lastView = null;
        while (i < childCount) {
            ExpandableView v = (ExpandableView) hostView.getChildAt(i);
            if (!(v.getVisibility() == 8 || v == ambientState.getShelf())) {
                notGoneIndex = stackScrollAlgorithm.updateNotGoneIndex(stackScrollState, stackScrollAlgorithmState, notGoneIndex, v);
                float increasedPadding = v.getIncreasedPaddingAmount();
                if (increasedPadding != 0.0f) {
                    stackScrollAlgorithmState.paddingMap.put(v, Float.valueOf(increasedPadding));
                    if (lastView != null) {
                        Float prevValue = stackScrollAlgorithmState.paddingMap.get(lastView);
                        float newValue = stackScrollAlgorithm.getPaddingForValue(Float.valueOf(increasedPadding));
                        if (prevValue != null) {
                            float prevPadding = stackScrollAlgorithm.getPaddingForValue(prevValue);
                            if (increasedPadding > 0.0f) {
                                newValue = NotificationUtils.interpolate(prevPadding, newValue, increasedPadding);
                            } else if (prevValue.floatValue() > 0.0f) {
                                newValue = NotificationUtils.interpolate(newValue, prevPadding, prevValue.floatValue());
                            }
                        }
                        stackScrollAlgorithmState.paddingMap.put(lastView, Float.valueOf(newValue));
                    }
                } else if (lastView != null) {
                    stackScrollAlgorithmState.paddingMap.put(lastView, Float.valueOf(stackScrollAlgorithm.getPaddingForValue(stackScrollAlgorithmState.paddingMap.get(lastView))));
                }
                if (v instanceof ExpandableNotificationRow) {
                    ExpandableNotificationRow row = (ExpandableNotificationRow) v;
                    List<ExpandableNotificationRow> children = row.getNotificationChildren();
                    if (row.isSummaryWithChildren() && children != null) {
                        for (ExpandableNotificationRow childRow : children) {
                            if (childRow.getVisibility() != 8) {
                                stackScrollState.getViewStateForView(childRow).notGoneIndex = notGoneIndex;
                                notGoneIndex++;
                            }
                            StackScrollAlgorithmState stackScrollAlgorithmState2 = state;
                        }
                    }
                }
                lastView = v;
            }
            i++;
            stackScrollAlgorithm = this;
            stackScrollAlgorithmState = state;
            AmbientState ambientState2 = ambientState;
        }
    }

    private float getPaddingForValue(Float increasedPadding) {
        if (increasedPadding == null) {
            return (float) this.mPaddingBetweenElements;
        }
        if (increasedPadding.floatValue() >= 0.0f) {
            return NotificationUtils.interpolate((float) this.mPaddingBetweenElements, (float) this.mIncreasedPaddingBetweenElements, increasedPadding.floatValue());
        }
        return NotificationUtils.interpolate(0.0f, (float) this.mPaddingBetweenElements, 1.0f + increasedPadding.floatValue());
    }

    private int updateNotGoneIndex(StackScrollState resultState, StackScrollAlgorithmState state, int notGoneIndex, ExpandableView v) {
        resultState.getViewStateForView(v).notGoneIndex = notGoneIndex;
        state.visibleChildren.add(v);
        return notGoneIndex + 1;
    }

    private void updatePositionsForState(StackScrollState resultState, StackScrollAlgorithmState algorithmState, AmbientState ambientState) {
        float currentYPosition = (float) (-algorithmState.scrollY);
        int childCount = algorithmState.visibleChildren.size();
        for (int i = 0; i < childCount; i++) {
            currentYPosition = updateChild(i, resultState, algorithmState, ambientState, currentYPosition);
        }
    }

    /* access modifiers changed from: protected */
    /* JADX WARNING: Removed duplicated region for block: B:19:0x0076  */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x007c  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public float updateChild(int r19, com.android.systemui.statusbar.stack.StackScrollState r20, com.android.systemui.statusbar.stack.StackScrollAlgorithm.StackScrollAlgorithmState r21, com.android.systemui.statusbar.stack.AmbientState r22, float r23) {
        /*
            r18 = this;
            r6 = r18
            r7 = r19
            r8 = r21
            java.util.ArrayList<com.android.systemui.statusbar.ExpandableView> r0 = r8.visibleChildren
            java.lang.Object r0 = r0.get(r7)
            r9 = r0
            com.android.systemui.statusbar.ExpandableView r9 = (com.android.systemui.statusbar.ExpandableView) r9
            r10 = r20
            com.android.systemui.statusbar.stack.ExpandableViewState r11 = r10.getViewStateForView(r9)
            r0 = 0
            r11.location = r0
            int r12 = r6.getPaddingAfterChild(r8, r9)
            int r13 = r6.getMaxAllowedChildHeight(r9)
            r14 = r23
            r11.yTranslation = r14
            r0 = 4
            r11.location = r0
            int r15 = r9.getViewType()
            r5 = 2
            if (r15 != r5) goto L_0x0041
            int r0 = r22.getInnerHeight()
            int r0 = r0 - r13
            float r0 = (float) r0
            float r1 = r22.getStackTranslation()
            r2 = 1048576000(0x3e800000, float:0.25)
            float r1 = r1 * r2
            float r0 = r0 + r1
            r11.yTranslation = r0
        L_0x003e:
            r4 = r22
            goto L_0x004b
        L_0x0041:
            if (r15 == 0) goto L_0x0046
            r0 = 1
            if (r15 != r0) goto L_0x003e
        L_0x0046:
            r4 = r22
            r6.clampPositionToShelf(r11, r4)
        L_0x004b:
            boolean r0 = r6.mIsExpandedBecauseOfHeadsUp
            if (r0 != 0) goto L_0x006a
            if (r15 == 0) goto L_0x0055
            r0 = 13
            if (r15 != r0) goto L_0x006a
        L_0x0055:
            boolean r0 = r22.isOnKeyguard()
            if (r0 != 0) goto L_0x006a
            r16 = 0
            r0 = r6
            r1 = r9
            r2 = r7
            r3 = r11
            r4 = r22
            r6 = r5
            r5 = r16
            r0.updateNotificationFullyShown(r1, r2, r3, r4, r5)
            goto L_0x006b
        L_0x006a:
            r6 = r5
        L_0x006b:
            float r0 = r11.yTranslation
            float r1 = (float) r13
            float r0 = r0 + r1
            float r1 = (float) r12
            float r0 = r0 + r1
            r1 = 0
            int r1 = (r0 > r1 ? 1 : (r0 == r1 ? 0 : -1))
            if (r1 > 0) goto L_0x0078
            r11.location = r6
        L_0x0078:
            int r1 = r11.location
            if (r1 != 0) goto L_0x0092
            java.lang.String r1 = "StackScrollAlgorithm"
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            java.lang.String r3 = "Failed to assign location for child "
            r2.append(r3)
            r2.append(r7)
            java.lang.String r2 = r2.toString()
            android.util.Log.wtf(r1, r2)
        L_0x0092:
            float r1 = r11.yTranslation
            float r2 = r22.getTopPadding()
            float r3 = r22.getStackTranslation()
            float r2 = r2 + r3
            float r1 = r1 + r2
            r11.yTranslation = r1
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.stack.StackScrollAlgorithm.updateChild(int, com.android.systemui.statusbar.stack.StackScrollState, com.android.systemui.statusbar.stack.StackScrollAlgorithm$StackScrollAlgorithmState, com.android.systemui.statusbar.stack.AmbientState, float):float");
    }

    /* access modifiers changed from: protected */
    public int getPaddingAfterChild(StackScrollAlgorithmState algorithmState, ExpandableView child) {
        return algorithmState.getPaddingAfterChild(child);
    }

    private void updateHeadsUpStates(StackScrollState resultState, StackScrollAlgorithmState algorithmState) {
        int childCount = algorithmState.visibleChildren.size();
        ExpandableNotificationRow topHeadsUpEntry = null;
        for (int i = 0; i < childCount; i++) {
            View child = algorithmState.visibleChildren.get(i);
            if (child instanceof ExpandableNotificationRow) {
                ExpandableNotificationRow row = (ExpandableNotificationRow) child;
                if (row.isHeadsUp() || row.isHeadsUpAnimatingAway()) {
                    ExpandableViewState childState = resultState.getViewStateForView(row);
                    if (topHeadsUpEntry == null) {
                        topHeadsUpEntry = row;
                        childState.location = 1;
                    }
                    if (row.isHeadsUpAnimatingAway()) {
                        applyCommonHeadsUpChildState(childState, row);
                        if (row == topHeadsUpEntry) {
                            childState.yTranslation = (float) (-childState.height);
                            childState.alpha = 1.0f;
                        } else {
                            childState.alpha = 0.0f;
                        }
                    } else if (row.isPinned()) {
                        applyCommonHeadsUpChildState(childState, row);
                        childState.yTranslation = (float) this.mHeadsUpMarginTop;
                        childState.alpha = 1.0f;
                    }
                }
            }
        }
    }

    private void applyCommonHeadsUpChildState(ExpandableViewState childState, ExpandableNotificationRow row) {
        childState.height = Math.max(row.getIntrinsicHeight(), childState.height);
        childState.hidden = false;
        childState.shadowAlpha = 0.8f;
    }

    private void clampPositionToShelf(ExpandableViewState childViewState, AmbientState ambientState) {
        int shelfStart = (int) (((float) ambientState.getInnerHeight()) + ambientState.getTopPadding());
        childViewState.yTranslation = Math.min(childViewState.yTranslation, (float) shelfStart);
        if (childViewState.yTranslation >= ((float) shelfStart)) {
            childViewState.hidden = true;
            childViewState.inShelf = true;
        }
        if (!ambientState.isShadeExpanded()) {
            childViewState.height = 0;
        }
    }

    private void updateNotificationFullyShown(ExpandableView child, int index, ExpandableViewState childViewState, AmbientState ambientState, float parentY) {
        float contentHeight;
        ExpandableView expandableView = child;
        ExpandableViewState expandableViewState = childViewState;
        boolean animateScale = true;
        boolean firstRowNotification = (expandableView instanceof ExpandableNotificationRow) && ((ExpandableNotificationRow) expandableView).isFirstRow();
        boolean isExpandedGroupRow = (expandableView instanceof ExpandableNotificationRow) && child.areChildrenExpanded() && !child.isChildInGroup() && ((ExpandableNotificationRow) expandableView).getChildrenContainer() != null && ((ExpandableNotificationRow) expandableView).getChildrenContainer().getNotificationChildren().size() > 0 && !((ExpandableNotificationRow) expandableView).areGutsExposed();
        float height = (float) expandableViewState.height;
        float y = parentY + expandableViewState.yTranslation;
        if (ambientState.isExpansionChanging()) {
            contentHeight = (float) ambientState.getInnerHeight();
        } else {
            contentHeight = ((float) ambientState.getMaxLayoutHeight()) - ambientState.getTopPadding();
        }
        if (isExpandedGroupRow) {
            float height2 = (float) ((ExpandableNotificationRow) expandableView).getChildrenContainer().getNotificationChildren().get(0).getActualHeight();
            NotificationHeaderView header = ((ExpandableNotificationRow) expandableView).getNotificationHeader();
            if (header != null && header.getVisibility() == 0) {
                height2 += (float) header.getHeight();
            }
            height = Math.min(height2, (float) expandableViewState.height);
        }
        boolean considerState = index != 0 && !firstRowNotification;
        boolean considerAnimation = considerState && y <= ((float) ambientState.getInnerHeight());
        boolean fullyShown = y + height <= (this.mIsExpandedBecauseOfHeadsUp ? parentY : 0.0f) + contentHeight;
        boolean hideInTracking = considerState && !fullyShown;
        if (child.getViewType() != 0 || isExpandedGroupRow) {
            animateScale = false;
        }
        expandableViewState.alpha = hideInTracking ? 0.0f : 1.0f;
        expandableViewState.scaleX = (!animateScale || !hideInTracking) ? 1.0f : 0.92f;
        expandableViewState.scaleY = (!animateScale || !hideInTracking) ? 1.0f : 0.92f;
        if (!(!considerAnimation || expandableViewState.fullyShown == fullyShown || this.mOnAnimationRequestedListener == null)) {
            if (fullyShown) {
                this.mOnAnimationRequestedListener.onPopupAnimationRequested(expandableView);
            } else {
                this.mOnAnimationRequestedListener.onDismissAnimationRequested(expandableView);
            }
        }
        expandableViewState.fullyShown = fullyShown;
    }

    /* access modifiers changed from: protected */
    public int getMaxAllowedChildHeight(View child) {
        if (child instanceof ExpandableView) {
            return ((ExpandableView) child).getIntrinsicHeight();
        }
        return child == null ? this.mCollapsedSize : child.getHeight();
    }

    private void updateZValuesForState(StackScrollState resultState, StackScrollAlgorithmState algorithmState, AmbientState ambientState) {
        float childrenOnTop = 0.0f;
        for (int i = algorithmState.visibleChildren.size() - 1; i >= 0; i--) {
            childrenOnTop = updateChildZValue(i, childrenOnTop, resultState, algorithmState, ambientState);
        }
    }

    /* access modifiers changed from: protected */
    public float updateChildZValue(int i, float childrenOnTop, StackScrollState resultState, StackScrollAlgorithmState algorithmState, AmbientState ambientState) {
        float childrenOnTop2;
        int i2 = i;
        ExpandableView child = algorithmState.visibleChildren.get(i2);
        ExpandableViewState childViewState = resultState.getViewStateForView(child);
        int zDistanceBetweenElements = ambientState.getZDistanceBetweenElements();
        float baseZ = (float) ambientState.getBaseZHeight();
        if (!child.mustStayOnScreen() || childViewState.yTranslation >= ambientState.getTopPadding() + ambientState.getStackTranslation()) {
            if (i2 != 0 || !child.isAboveShelf()) {
                childViewState.zTranslation = baseZ;
            } else {
                int shelfHeight = ambientState.getShelf().getIntrinsicHeight();
                float shelfStart = ((float) (ambientState.getInnerHeight() - shelfHeight)) + ambientState.getTopPadding() + ambientState.getStackTranslation();
                float notificationEnd = childViewState.yTranslation + ((float) child.getPinnedHeadsUpHeight()) + ((float) this.mPaddingBetweenElements);
                if (shelfStart > notificationEnd) {
                    childViewState.zTranslation = baseZ;
                } else {
                    float factor = 1.0f;
                    if (shelfHeight != 0) {
                        factor = Math.min((notificationEnd - shelfStart) / ((float) shelfHeight), 1.0f);
                    }
                    childViewState.zTranslation = (((float) zDistanceBetweenElements) * factor) + baseZ;
                }
            }
            return childrenOnTop;
        }
        if (childrenOnTop != 0.0f) {
            childrenOnTop2 = childrenOnTop + 1.0f;
        } else {
            childrenOnTop2 = childrenOnTop + Math.min(1.0f, ((ambientState.getTopPadding() + ambientState.getStackTranslation()) - childViewState.yTranslation) / ((float) childViewState.height));
        }
        childViewState.zTranslation = (((float) zDistanceBetweenElements) * childrenOnTop2) + baseZ;
        return childrenOnTop2;
    }

    public void setIsExpanded(boolean isExpanded) {
        this.mIsExpanded = isExpanded;
    }

    public void setExpandedBecauseOfHeadsUp(boolean isExpandedBecauseOfHeadsUp) {
        this.mIsExpandedBecauseOfHeadsUp = isExpandedBecauseOfHeadsUp;
    }
}
