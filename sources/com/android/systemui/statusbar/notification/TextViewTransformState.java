package com.android.systemui.statusbar.notification;

import android.text.Layout;
import android.text.TextUtils;
import android.util.Pools;
import android.view.View;
import android.widget.TextView;

public class TextViewTransformState extends TransformState {
    private static Pools.SimplePool<TextViewTransformState> sInstancePool = new Pools.SimplePool<>(40);
    private TextView mText;

    public void initFrom(View view) {
        super.initFrom(view);
        if (view instanceof TextView) {
            this.mText = (TextView) view;
        }
    }

    /* access modifiers changed from: protected */
    public boolean sameAs(TransformState otherState) {
        if (otherState instanceof TextViewTransformState) {
            TextViewTransformState otherTvs = (TextViewTransformState) otherState;
            if (TextUtils.equals(otherTvs.mText.getText(), this.mText.getText())) {
                return getEllipsisCount() == otherTvs.getEllipsisCount() && getInnerHeight(this.mText) == getInnerHeight(otherTvs.mText);
            }
        }
        return super.sameAs(otherState);
    }

    private int getInnerHeight(TextView text) {
        return (text.getHeight() - text.getPaddingTop()) - text.getPaddingBottom();
    }

    private int getEllipsisCount() {
        Layout l = this.mText.getLayout();
        if (l == null || l.getLineCount() <= 0) {
            return 0;
        }
        return l.getEllipsisCount(0);
    }

    public static TextViewTransformState obtain() {
        TextViewTransformState instance = (TextViewTransformState) sInstancePool.acquire();
        if (instance != null) {
            return instance;
        }
        return new TextViewTransformState();
    }

    public void recycle() {
        super.recycle();
        sInstancePool.release(this);
    }

    /* access modifiers changed from: protected */
    public void reset() {
        super.reset();
        this.mText = null;
    }
}
