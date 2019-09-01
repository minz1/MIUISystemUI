package com.android.systemui.statusbar.notification;

import android.util.Pools;

public class ActionListTransformState extends TransformState {
    private static Pools.SimplePool<ActionListTransformState> sInstancePool = new Pools.SimplePool<>(40);

    /* access modifiers changed from: protected */
    public boolean sameAs(TransformState otherState) {
        return otherState instanceof ActionListTransformState;
    }

    public static ActionListTransformState obtain() {
        ActionListTransformState instance = (ActionListTransformState) sInstancePool.acquire();
        if (instance != null) {
            return instance;
        }
        return new ActionListTransformState();
    }

    public void transformViewFullyFrom(TransformState otherState, float transformationAmount) {
    }

    public void transformViewFullyTo(TransformState otherState, float transformationAmount) {
    }

    /* access modifiers changed from: protected */
    public void resetTransformedView() {
        float y = getTransformedView().getTranslationY();
        super.resetTransformedView();
        getTransformedView().setTranslationY(y);
    }

    public void recycle() {
        super.recycle();
        sInstancePool.release(this);
    }
}
