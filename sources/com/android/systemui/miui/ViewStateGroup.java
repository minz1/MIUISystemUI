package com.android.systemui.miui;

import android.content.Context;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

public class ViewStateGroup {
    /* access modifiers changed from: private */
    public SparseArray<ViewState> mStates;

    public static class Builder {
        private Context mContext;
        ViewStateGroup mResult = new ViewStateGroup();

        public Builder(Context context) {
            this.mContext = context;
        }

        public Builder addState(int viewId, int property, int value) {
            ViewState viewState = (ViewState) this.mResult.mStates.get(viewId);
            if (viewState == null) {
                viewState = new ViewState(viewId);
                this.mResult.mStates.put(viewId, viewState);
            }
            viewState.mIntStates.put(property, value);
            return this;
        }

        public Builder addStateWithIntDimen(int viewId, int property, int res) {
            return addState(viewId, property, this.mContext.getResources().getDimensionPixelSize(res));
        }

        public Builder addStateWithIntRes(int viewId, int property, int res) {
            return addState(viewId, property, this.mContext.getResources().getInteger(res));
        }

        public ViewStateGroup build() {
            return this.mResult;
        }
    }

    public static class ViewState {
        private SparseArray<Float> mFloatStates = new SparseArray<>();
        /* access modifiers changed from: private */
        public SparseIntArray mIntStates = new SparseIntArray();
        /* access modifiers changed from: private */
        public int mViewId;

        ViewState(int id) {
            this.mViewId = id;
        }

        /* access modifiers changed from: package-private */
        public void apply(View view) {
            if (view != null && this.mViewId == view.getId()) {
                for (int i = 0; i < this.mIntStates.size(); i++) {
                    int property = this.mIntStates.keyAt(i);
                    applyIntProperty(view, property, this.mIntStates.get(property));
                }
                for (int i2 = 0; i2 < this.mFloatStates.size(); i2++) {
                    int property2 = this.mFloatStates.keyAt(i2);
                    applyFloatProperty(view, property2, this.mFloatStates.get(property2).floatValue());
                }
            }
        }

        private static void applyIntProperty(View view, int property, int value) {
            ViewGroup.LayoutParams params = view.getLayoutParams();
            switch (property) {
                case 1:
                    setLayoutGravity(params, value);
                    return;
                case 2:
                    params.width = value;
                    return;
                case 3:
                    params.height = value;
                    return;
                case 5:
                    if (params instanceof ViewGroup.MarginLayoutParams) {
                        ((ViewGroup.MarginLayoutParams) params).leftMargin = value;
                        return;
                    }
                    return;
                case 6:
                    if (params instanceof ViewGroup.MarginLayoutParams) {
                        ((ViewGroup.MarginLayoutParams) params).topMargin = value;
                        return;
                    }
                    return;
                case 7:
                    if (params instanceof ViewGroup.MarginLayoutParams) {
                        ((ViewGroup.MarginLayoutParams) params).rightMargin = value;
                        return;
                    }
                    return;
                case 8:
                    if (params instanceof ViewGroup.MarginLayoutParams) {
                        ((ViewGroup.MarginLayoutParams) params).bottomMargin = value;
                        return;
                    }
                    return;
                case 9:
                    view.setPadding(value, value, value, value);
                    return;
                case 10:
                    view.setVisibility(value);
                    return;
                case 11:
                    if (view instanceof LinearLayout) {
                        ((LinearLayout) view).setOrientation(value);
                        return;
                    }
                    return;
                case 12:
                    if (view instanceof LinearLayout) {
                        ((LinearLayout) view).setGravity(value);
                        return;
                    }
                    return;
                default:
                    return;
            }
        }

        private static void applyFloatProperty(View view, int property, float value) {
            ViewGroup.LayoutParams params = view.getLayoutParams();
            if (property == 4 && (params instanceof LinearLayout.LayoutParams)) {
                ((LinearLayout.LayoutParams) params).weight = value;
            }
        }

        private static void setLayoutGravity(ViewGroup.LayoutParams params, int gravity) {
            if (params instanceof FrameLayout.LayoutParams) {
                ((FrameLayout.LayoutParams) params).gravity = gravity;
            } else if (params instanceof LinearLayout.LayoutParams) {
                ((LinearLayout.LayoutParams) params).gravity = gravity;
            }
        }
    }

    private ViewStateGroup() {
        this.mStates = new SparseArray<>();
    }

    public void apply(ViewGroup parent) {
        for (int i = 0; i < this.mStates.size(); i++) {
            ViewState state = this.mStates.get(this.mStates.keyAt(i), null);
            if (state != null) {
                state.apply(parent.findViewById(state.mViewId));
            }
        }
    }
}
