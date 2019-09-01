package com.android.systemui.statusbar.notification;

import android.util.Pools;

public class ProgressTransformState extends TransformState {
    private static Pools.SimplePool<ProgressTransformState> sInstancePool = new Pools.SimplePool<>(40);

    /* access modifiers changed from: protected */
    public boolean sameAs(TransformState otherState) {
        if (otherState instanceof ProgressTransformState) {
            return true;
        }
        return super.sameAs(otherState);
    }

    public static ProgressTransformState obtain() {
        ProgressTransformState instance = (ProgressTransformState) sInstancePool.acquire();
        if (instance != null) {
            return instance;
        }
        return new ProgressTransformState();
    }

    public void recycle() {
        super.recycle();
        sInstancePool.release(this);
    }
}
