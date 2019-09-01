package com.android.systemui.statusbar.notification;

import android.graphics.drawable.Icon;
import android.graphics.drawable.IconCompat;
import android.util.Pools;
import android.view.View;
import android.widget.ImageView;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.statusbar.CrossFadeHelper;
import com.android.systemui.statusbar.TransformableView;

public class ImageTransformState extends TransformState {
    private static Pools.SimplePool<ImageTransformState> sInstancePool = new Pools.SimplePool<>(40);
    private Icon mIcon;

    public void initFrom(View view) {
        super.initFrom(view);
        if (view instanceof ImageView) {
            this.mIcon = (Icon) view.getTag(R.id.image_icon_tag);
        }
    }

    /* access modifiers changed from: protected */
    public boolean sameAs(TransformState otherState) {
        if (!(otherState instanceof ImageTransformState)) {
            return super.sameAs(otherState);
        }
        if (this.mIcon == null) {
            return false;
        }
        Icon otherIcon = ((ImageTransformState) otherState).getIcon();
        if (otherIcon == null) {
            return false;
        }
        return IconCompat.sameAs(this.mIcon, otherIcon);
    }

    public void appear(float transformationAmount, TransformableView otherView) {
        if (otherView instanceof HybridNotificationView) {
            float f = 0.0f;
            if (transformationAmount == 0.0f) {
                this.mTransformedView.setPivotY(0.0f);
                this.mTransformedView.setPivotX((float) (this.mTransformedView.getWidth() / 2));
                prepareFadeIn();
            }
            float transformationAmount2 = mapToDuration(transformationAmount);
            CrossFadeHelper.fadeIn(this.mTransformedView, transformationAmount2, false);
            float transformationAmount3 = Interpolators.LINEAR_OUT_SLOW_IN.getInterpolation(transformationAmount2);
            if (!Float.isNaN(transformationAmount3)) {
                f = transformationAmount3;
            }
            float transformationAmount4 = f;
            this.mTransformedView.setScaleX(transformationAmount4);
            this.mTransformedView.setScaleY(transformationAmount4);
            return;
        }
        super.appear(transformationAmount, otherView);
    }

    public void disappear(float transformationAmount, TransformableView otherView) {
        if (otherView instanceof HybridNotificationView) {
            float f = 0.0f;
            if (transformationAmount == 0.0f) {
                this.mTransformedView.setPivotY(0.0f);
                this.mTransformedView.setPivotX((float) (this.mTransformedView.getWidth() / 2));
            }
            float transformationAmount2 = mapToDuration(1.0f - transformationAmount);
            CrossFadeHelper.fadeOut(this.mTransformedView, 1.0f - transformationAmount2, false);
            float transformationAmount3 = Interpolators.LINEAR_OUT_SLOW_IN.getInterpolation(transformationAmount2);
            if (!Float.isNaN(transformationAmount3)) {
                f = transformationAmount3;
            }
            float transformationAmount4 = f;
            this.mTransformedView.setScaleX(transformationAmount4);
            this.mTransformedView.setScaleY(transformationAmount4);
            return;
        }
        super.disappear(transformationAmount, otherView);
    }

    private static float mapToDuration(float scaleAmount) {
        return Math.max(Math.min(((360.0f * scaleAmount) - 150.0f) / 210.0f, 1.0f), 0.0f);
    }

    public Icon getIcon() {
        return this.mIcon;
    }

    public static ImageTransformState obtain() {
        ImageTransformState instance = (ImageTransformState) sInstancePool.acquire();
        if (instance != null) {
            return instance;
        }
        return new ImageTransformState();
    }

    /* access modifiers changed from: protected */
    public boolean transformScale() {
        return true;
    }

    public void recycle() {
        super.recycle();
        sInstancePool.release(this);
    }

    /* access modifiers changed from: protected */
    public void reset() {
        super.reset();
        this.mIcon = null;
    }
}
