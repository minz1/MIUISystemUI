package com.android.systemui.statusbar;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import com.android.systemui.R;
import com.android.systemui.statusbar.stack.ExpandableViewState;
import com.android.systemui.statusbar.stack.StackScrollState;

public class EmptyShadeView extends StackScrollerDecorView {
    /* access modifiers changed from: private */
    public TextView mEmptyText;

    public class EmptyShadeViewState extends ExpandableViewState {
        public EmptyShadeViewState() {
        }

        public void applyToView(View view) {
            super.applyToView(view);
            if (view instanceof EmptyShadeView) {
                EmptyShadeView emptyShadeView = (EmptyShadeView) view;
                boolean z = false;
                if ((((float) this.clipTopAmount) <= ((float) EmptyShadeView.this.mEmptyText.getPaddingTop()) * 0.6f) && !emptyShadeView.willBeGone()) {
                    z = true;
                }
                emptyShadeView.performVisibilityAnimation(z);
            }
        }
    }

    public EmptyShadeView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.mEmptyText.setText(R.string.empty_shade_text);
    }

    /* access modifiers changed from: protected */
    public View findContentView() {
        return findViewById(R.id.no_notifications);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mEmptyText = (TextView) findContentView();
    }

    public ExpandableViewState createNewViewState(StackScrollState stackScrollState) {
        return new EmptyShadeViewState();
    }
}
