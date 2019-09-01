package com.android.systemui.statusbar.stack;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import com.android.systemui.R;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.ExpandableView;
import java.util.List;
import java.util.WeakHashMap;

public class StackScrollState {
    private final ViewGroup mHostView;
    private WeakHashMap<ExpandableView, ExpandableViewState> mStateMap = new WeakHashMap<>();

    public StackScrollState(ViewGroup hostView) {
        this.mHostView = hostView;
    }

    public ViewGroup getHostView() {
        return this.mHostView;
    }

    public void resetViewStates() {
        int numChildren = this.mHostView.getChildCount();
        for (int i = 0; i < numChildren; i++) {
            ExpandableView child = (ExpandableView) this.mHostView.getChildAt(i);
            resetViewState(child);
            if (child instanceof ExpandableNotificationRow) {
                ExpandableNotificationRow row = (ExpandableNotificationRow) child;
                List<ExpandableNotificationRow> children = row.getNotificationChildren();
                if (row.isSummaryWithChildren() && children != null) {
                    for (ExpandableNotificationRow childRow : children) {
                        resetViewState(childRow);
                    }
                }
            }
        }
    }

    private void resetViewState(ExpandableView view) {
        ExpandableViewState viewState = this.mStateMap.get(view);
        if (viewState == null) {
            viewState = view.createNewViewState(this);
            this.mStateMap.put(view, viewState);
        }
        viewState.height = view.getIntrinsicHeight();
        int i = 0;
        viewState.gone = view.getVisibility() == 8;
        viewState.alpha = 1.0f;
        viewState.shadowAlpha = 1.0f;
        viewState.notGoneIndex = -1;
        viewState.xTranslation = view.getTranslationX();
        viewState.hidden = false;
        viewState.scaleX = view.getScaleX();
        viewState.scaleY = view.getScaleY();
        viewState.inShelf = false;
        if (view instanceof ExpandableNotificationRow) {
            ExpandableNotificationRow row = (ExpandableNotificationRow) view;
            int padding = row.getContext().getResources().getDimensionPixelSize(R.dimen.notification_row_extra_padding);
            this.mStateMap.get(row).paddingTop = (row.isOnKeyguard() || row.isIsShowHeadsUpBackground() || !row.hasExtraTopPadding()) ? 0 : padding;
            ExpandableViewState expandableViewState = this.mStateMap.get(row);
            if (!row.isOnKeyguard() && !row.isIsShowHeadsUpBackground() && row.hasExtraBottomPadding()) {
                i = padding;
            }
            expandableViewState.paddingBottom = i;
        }
    }

    public ExpandableViewState getViewStateForView(View requestedView) {
        return this.mStateMap.get(requestedView);
    }

    public void removeViewStateForView(View child) {
        this.mStateMap.remove(child);
    }

    public void apply() {
        int numChildren = this.mHostView.getChildCount();
        for (int i = 0; i < numChildren; i++) {
            ExpandableView child = (ExpandableView) this.mHostView.getChildAt(i);
            ExpandableViewState state = this.mStateMap.get(child);
            if (state == null) {
                Log.wtf("StackScrollStateNoSuchChild", "No child state was found when applying this state to the hostView");
            } else if (!state.gone) {
                state.applyToView(child);
            }
        }
    }
}
