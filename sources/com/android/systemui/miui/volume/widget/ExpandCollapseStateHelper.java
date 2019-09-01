package com.android.systemui.miui.volume.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.transition.AutoTransition;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.transition.TransitionManager;
import android.util.AttributeSet;
import android.view.ViewGroup;
import com.android.systemui.miui.volume.R;

class ExpandCollapseStateHelper {
    private boolean mExpanded = false;
    private OnExpandStateUpdatedListener mListener;
    private Transition mTransitionCollapse;
    private Transition mTransitionExpand;
    private ViewGroup mTransitionRoot;

    public interface OnExpandStateUpdatedListener {
        void onExpandStateUpdated(boolean z);
    }

    public ExpandCollapseStateHelper(ViewGroup parent, OnExpandStateUpdatedListener listener, AttributeSet attrs, int defStyleAttr) {
        this.mTransitionRoot = parent;
        Context context = parent.getContext();
        this.mListener = listener;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ExpandCollapseLayout, defStyleAttr, 0);
        this.mTransitionExpand = getTransition(context, a, R.styleable.ExpandCollapseLayout_expandingTransition, new AutoTransition());
        this.mTransitionCollapse = getTransition(context, a, R.styleable.ExpandCollapseLayout_collapsingTransition, new AutoTransition());
        a.recycle();
    }

    private static Transition getTransition(Context context, TypedArray a, int index, Transition def) {
        int transitionId = a.getResourceId(index, -1);
        if (transitionId > 0) {
            return TransitionInflater.from(context).inflateTransition(transitionId);
        }
        return def;
    }

    public final void updateExpanded(boolean expand, boolean withTransition) {
        this.mExpanded = expand;
        if (withTransition) {
            beginDelayedTransition();
        }
        if (this.mListener != null) {
            this.mListener.onExpandStateUpdated(expand);
        }
    }

    public void beginDelayedTransition() {
        TransitionManager.endTransitions(this.mTransitionRoot);
        TransitionManager.beginDelayedTransition(this.mTransitionRoot, this.mExpanded ? this.mTransitionExpand : this.mTransitionCollapse);
    }

    public boolean isExpanded() {
        return this.mExpanded;
    }
}
