package com.android.systemui.statusbar.notification;

import android.util.ArraySet;
import android.util.Pools;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.statusbar.CrossFadeHelper;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.TransformableView;
import com.android.systemui.statusbar.ViewTransformationHelper;

public class TransformState {
    private static Pools.SimplePool<TransformState> sInstancePool = new Pools.SimplePool<>(40);
    private int[] mOwnPosition = new int[2];
    private float mTransformationEndX = -1.0f;
    private float mTransformationEndY = -1.0f;
    protected View mTransformedView;

    public void initFrom(View view) {
        this.mTransformedView = view;
    }

    public void transformViewFrom(TransformState otherState, float transformationAmount) {
        this.mTransformedView.animate().cancel();
        if (!sameAs(otherState)) {
            CrossFadeHelper.fadeIn(this.mTransformedView, transformationAmount);
        } else if (this.mTransformedView.getVisibility() == 4 || this.mTransformedView.getAlpha() != 1.0f) {
            this.mTransformedView.setAlpha(1.0f);
            this.mTransformedView.setVisibility(0);
        }
        transformViewFullyFrom(otherState, transformationAmount);
    }

    public void transformViewFullyFrom(TransformState otherState, float transformationAmount) {
        transformViewFrom(otherState, 17, null, transformationAmount);
    }

    public void transformViewFullyFrom(TransformState otherState, ViewTransformationHelper.CustomTransformation customTransformation, float transformationAmount) {
        transformViewFrom(otherState, 17, customTransformation, transformationAmount);
    }

    public void transformViewVerticalFrom(TransformState otherState, ViewTransformationHelper.CustomTransformation customTransformation, float transformationAmount) {
        transformViewFrom(otherState, 16, customTransformation, transformationAmount);
    }

    public void transformViewVerticalFrom(TransformState otherState, float transformationAmount) {
        transformViewFrom(otherState, 16, null, transformationAmount);
    }

    /* JADX WARNING: Code restructure failed: missing block: B:30:0x0069, code lost:
        if (r1.initTransformation(r0, r17) == false) goto L_0x006e;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void transformViewFrom(com.android.systemui.statusbar.notification.TransformState r17, int r18, com.android.systemui.statusbar.ViewTransformationHelper.CustomTransformation r19, float r20) {
        /*
            r16 = this;
            r0 = r16
            r1 = r19
            r2 = r20
            android.view.View r3 = r0.mTransformedView
            r4 = r18 & 1
            r5 = 0
            r6 = 1
            if (r4 == 0) goto L_0x0010
            r4 = r6
            goto L_0x0011
        L_0x0010:
            r4 = r5
        L_0x0011:
            r7 = r18 & 16
            if (r7 == 0) goto L_0x0017
            r7 = r6
            goto L_0x0018
        L_0x0017:
            r7 = r5
        L_0x0018:
            boolean r8 = r16.transformScale()
            r9 = 0
            int r10 = (r2 > r9 ? 1 : (r2 == r9 ? 0 : -1))
            r11 = -1082130432(0xffffffffbf800000, float:-1.0)
            if (r10 == 0) goto L_0x0050
            if (r4 == 0) goto L_0x002d
            float r10 = r16.getTransformationStartX()
            int r10 = (r10 > r11 ? 1 : (r10 == r11 ? 0 : -1))
            if (r10 == 0) goto L_0x0050
        L_0x002d:
            if (r7 == 0) goto L_0x0037
            float r10 = r16.getTransformationStartY()
            int r10 = (r10 > r11 ? 1 : (r10 == r11 ? 0 : -1))
            if (r10 == 0) goto L_0x0050
        L_0x0037:
            if (r8 == 0) goto L_0x0041
            float r10 = r16.getTransformationStartScaleX()
            int r10 = (r10 > r11 ? 1 : (r10 == r11 ? 0 : -1))
            if (r10 == 0) goto L_0x0050
        L_0x0041:
            if (r8 == 0) goto L_0x004c
            float r10 = r16.getTransformationStartScaleY()
            int r10 = (r10 > r11 ? 1 : (r10 == r11 ? 0 : -1))
            if (r10 != 0) goto L_0x004c
            goto L_0x0050
        L_0x004c:
            r13 = r17
            goto L_0x00f6
        L_0x0050:
            int r10 = (r2 > r9 ? 1 : (r2 == r9 ? 0 : -1))
            if (r10 == 0) goto L_0x0059
            int[] r10 = r17.getLaidOutLocationOnScreen()
            goto L_0x005d
        L_0x0059:
            int[] r10 = r17.getLocationOnScreen()
        L_0x005d:
            int[] r12 = r16.getLaidOutLocationOnScreen()
            if (r1 == 0) goto L_0x006c
            r13 = r17
            boolean r14 = r1.initTransformation(r0, r13)
            if (r14 != 0) goto L_0x00e0
            goto L_0x006e
        L_0x006c:
            r13 = r17
        L_0x006e:
            if (r4 == 0) goto L_0x0079
            r14 = r10[r5]
            r5 = r12[r5]
            int r14 = r14 - r5
            float r5 = (float) r14
            r0.setTransformationStartX(r5)
        L_0x0079:
            if (r7 == 0) goto L_0x0084
            r5 = r10[r6]
            r14 = r12[r6]
            int r5 = r5 - r14
            float r5 = (float) r5
            r0.setTransformationStartY(r5)
        L_0x0084:
            android.view.View r5 = r17.getTransformedView()
            if (r8 == 0) goto L_0x00b1
            int r14 = r5.getWidth()
            int r6 = r3.getWidth()
            if (r14 == r6) goto L_0x00b1
            int r6 = r3.getWidth()
            if (r6 == 0) goto L_0x00b1
            int r6 = r5.getWidth()
            float r6 = (float) r6
            float r14 = r5.getScaleX()
            float r6 = r6 * r14
            int r14 = r3.getWidth()
            float r14 = (float) r14
            float r6 = r6 / r14
            r0.setTransformationStartScaleX(r6)
            r3.setPivotX(r9)
            goto L_0x00b4
        L_0x00b1:
            r0.setTransformationStartScaleX(r11)
        L_0x00b4:
            if (r8 == 0) goto L_0x00dd
            int r6 = r5.getHeight()
            int r14 = r3.getHeight()
            if (r6 == r14) goto L_0x00dd
            int r6 = r3.getHeight()
            if (r6 == 0) goto L_0x00dd
            int r6 = r5.getHeight()
            float r6 = (float) r6
            float r14 = r5.getScaleY()
            float r6 = r6 * r14
            int r14 = r3.getHeight()
            float r14 = (float) r14
            float r6 = r6 / r14
            r0.setTransformationStartScaleY(r6)
            r3.setPivotY(r9)
            goto L_0x00e0
        L_0x00dd:
            r0.setTransformationStartScaleY(r11)
        L_0x00e0:
            if (r4 != 0) goto L_0x00e5
            r0.setTransformationStartX(r11)
        L_0x00e5:
            if (r7 != 0) goto L_0x00ea
            r0.setTransformationStartY(r11)
        L_0x00ea:
            if (r8 != 0) goto L_0x00f2
            r0.setTransformationStartScaleX(r11)
            r0.setTransformationStartScaleY(r11)
        L_0x00f2:
            r5 = 1
            setClippingDeactivated(r3, r5)
        L_0x00f6:
            android.view.animation.Interpolator r5 = com.android.systemui.Interpolators.FAST_OUT_SLOW_IN
            float r5 = r5.getInterpolation(r2)
            if (r4 == 0) goto L_0x0118
            r6 = r5
            if (r1 == 0) goto L_0x010d
            r10 = 1
            android.view.animation.Interpolator r12 = r1.getCustomInterpolator(r10, r10)
            if (r12 == 0) goto L_0x010d
            float r6 = r12.getInterpolation(r2)
        L_0x010d:
            float r10 = r16.getTransformationStartX()
            float r10 = com.android.systemui.statusbar.notification.NotificationUtils.interpolate(r10, r9, r6)
            r3.setTranslationX(r10)
        L_0x0118:
            if (r7 == 0) goto L_0x0136
            r6 = r5
            if (r1 == 0) goto L_0x012b
            r10 = 16
            r12 = 1
            android.view.animation.Interpolator r10 = r1.getCustomInterpolator(r10, r12)
            if (r10 == 0) goto L_0x012b
            float r6 = r10.getInterpolation(r2)
        L_0x012b:
            float r10 = r16.getTransformationStartY()
            float r10 = com.android.systemui.statusbar.notification.NotificationUtils.interpolate(r10, r9, r6)
            r3.setTranslationY(r10)
        L_0x0136:
            if (r8 == 0) goto L_0x0169
            float r6 = r16.getTransformationStartScaleX()
            int r10 = (r6 > r11 ? 1 : (r6 == r11 ? 0 : -1))
            r12 = 1065353216(0x3f800000, float:1.0)
            if (r10 == 0) goto L_0x0152
            float r10 = com.android.systemui.statusbar.notification.NotificationUtils.interpolate(r6, r12, r5)
            boolean r14 = java.lang.Float.isNaN(r10)
            if (r14 == 0) goto L_0x014e
            r14 = r9
            goto L_0x014f
        L_0x014e:
            r14 = r10
        L_0x014f:
            r3.setScaleX(r14)
        L_0x0152:
            float r10 = r16.getTransformationStartScaleY()
            int r11 = (r10 > r11 ? 1 : (r10 == r11 ? 0 : -1))
            if (r11 == 0) goto L_0x0169
            float r11 = com.android.systemui.statusbar.notification.NotificationUtils.interpolate(r10, r12, r5)
            boolean r12 = java.lang.Float.isNaN(r11)
            if (r12 == 0) goto L_0x0165
            goto L_0x0166
        L_0x0165:
            r9 = r11
        L_0x0166:
            r3.setScaleY(r9)
        L_0x0169:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.notification.TransformState.transformViewFrom(com.android.systemui.statusbar.notification.TransformState, int, com.android.systemui.statusbar.ViewTransformationHelper$CustomTransformation, float):void");
    }

    /* access modifiers changed from: protected */
    public boolean transformScale() {
        return false;
    }

    public boolean transformViewTo(TransformState otherState, float transformationAmount) {
        this.mTransformedView.animate().cancel();
        if (sameAs(otherState)) {
            if (this.mTransformedView.getVisibility() == 0) {
                this.mTransformedView.setAlpha(0.0f);
                this.mTransformedView.setVisibility(4);
            }
            return false;
        }
        CrossFadeHelper.fadeOut(this.mTransformedView, transformationAmount);
        transformViewFullyTo(otherState, transformationAmount);
        return true;
    }

    public void transformViewFullyTo(TransformState otherState, float transformationAmount) {
        transformViewTo(otherState, 17, null, transformationAmount);
    }

    public void transformViewFullyTo(TransformState otherState, ViewTransformationHelper.CustomTransformation customTransformation, float transformationAmount) {
        transformViewTo(otherState, 17, customTransformation, transformationAmount);
    }

    public void transformViewVerticalTo(TransformState otherState, ViewTransformationHelper.CustomTransformation customTransformation, float transformationAmount) {
        transformViewTo(otherState, 16, customTransformation, transformationAmount);
    }

    public void transformViewVerticalTo(TransformState otherState, float transformationAmount) {
        transformViewTo(otherState, 16, null, transformationAmount);
    }

    private void transformViewTo(TransformState otherState, int transformationFlags, ViewTransformationHelper.CustomTransformation customTransformation, float transformationAmount) {
        TransformState transformState = otherState;
        ViewTransformationHelper.CustomTransformation customTransformation2 = customTransformation;
        float f = transformationAmount;
        View transformedView = this.mTransformedView;
        boolean transformX = (transformationFlags & 1) != 0;
        boolean transformY = (transformationFlags & 16) != 0;
        boolean transformScale = transformScale();
        if (f == 0.0f) {
            if (transformX) {
                float transformationStartX = getTransformationStartX();
                setTransformationStartX(transformationStartX != -1.0f ? transformationStartX : transformedView.getTranslationX());
            }
            if (transformY) {
                float transformationStartY = getTransformationStartY();
                setTransformationStartY(transformationStartY != -1.0f ? transformationStartY : transformedView.getTranslationY());
            }
            View otherView = otherState.getTransformedView();
            if (!transformScale || otherView.getWidth() == transformedView.getWidth()) {
                setTransformationStartScaleX(-1.0f);
            } else {
                setTransformationStartScaleX(transformedView.getScaleX());
                transformedView.setPivotX(0.0f);
            }
            if (!transformScale || otherView.getHeight() == transformedView.getHeight()) {
                setTransformationStartScaleY(-1.0f);
            } else {
                setTransformationStartScaleY(transformedView.getScaleY());
                transformedView.setPivotY(0.0f);
            }
            setClippingDeactivated(transformedView, true);
        }
        float interpolatedValue = Interpolators.FAST_OUT_SLOW_IN.getInterpolation(f);
        int[] otherStablePosition = otherState.getLaidOutLocationOnScreen();
        int[] ownPosition = getLaidOutLocationOnScreen();
        if (transformX) {
            float endX = (float) (otherStablePosition[0] - ownPosition[0]);
            float interpolation = interpolatedValue;
            if (customTransformation2 != null) {
                if (customTransformation2.customTransformTarget(this, transformState)) {
                    endX = this.mTransformationEndX;
                }
                Interpolator customInterpolator = customTransformation2.getCustomInterpolator(1, false);
                if (customInterpolator != null) {
                    interpolation = customInterpolator.getInterpolation(f);
                }
            }
            transformedView.setTranslationX(NotificationUtils.interpolate(getTransformationStartX(), endX, interpolation));
        }
        if (transformY) {
            float endY = (float) (otherStablePosition[1] - ownPosition[1]);
            float interpolation2 = interpolatedValue;
            if (customTransformation2 != null) {
                if (customTransformation2.customTransformTarget(this, transformState)) {
                    endY = this.mTransformationEndY;
                }
                Interpolator customInterpolator2 = customTransformation2.getCustomInterpolator(16, false);
                if (customInterpolator2 != null) {
                    interpolation2 = customInterpolator2.getInterpolation(f);
                }
            }
            transformedView.setTranslationY(NotificationUtils.interpolate(getTransformationStartY(), endY, interpolation2));
        }
        if (transformScale) {
            View otherView2 = otherState.getTransformedView();
            float transformationStartScaleX = getTransformationStartScaleX();
            if (transformationStartScaleX != -1.0f) {
                float scaleX = NotificationUtils.interpolate(transformationStartScaleX, ((float) otherView2.getWidth()) / ((float) transformedView.getWidth()), interpolatedValue);
                transformedView.setScaleX(Float.isNaN(scaleX) ? 0.0f : scaleX);
            }
            float scaleX2 = getTransformationStartScaleY();
            if (scaleX2 != -1.0f) {
                float scaleY = NotificationUtils.interpolate(scaleX2, ((float) otherView2.getHeight()) / ((float) transformedView.getHeight()), interpolatedValue);
                transformedView.setScaleY(Float.isNaN(scaleY) ? 0.0f : scaleY);
            }
        }
    }

    public static void setClippingDeactivated(View transformedView, boolean deactivated) {
        ExpandableNotificationRow row;
        if (transformedView.getParent() instanceof ViewGroup) {
            ViewParent parent = transformedView.getParent();
            while (true) {
                ViewGroup view = (ViewGroup) parent;
                ArraySet<View> clipSet = (ArraySet) view.getTag(R.id.clip_children_set_tag);
                if (clipSet == null) {
                    clipSet = new ArraySet<>();
                    view.setTag(R.id.clip_children_set_tag, clipSet);
                }
                Boolean clipChildren = (Boolean) view.getTag(R.id.clip_children_tag);
                if (clipChildren == null) {
                    clipChildren = Boolean.valueOf(view.getClipChildren());
                    view.setTag(R.id.clip_children_tag, clipChildren);
                }
                Boolean clipToPadding = (Boolean) view.getTag(R.id.clip_to_padding_tag);
                if (clipToPadding == null) {
                    clipToPadding = Boolean.valueOf(view.getClipToPadding());
                    view.setTag(R.id.clip_to_padding_tag, clipToPadding);
                }
                if (view instanceof ExpandableNotificationRow) {
                    row = (ExpandableNotificationRow) view;
                } else {
                    row = null;
                }
                if (!deactivated) {
                    clipSet.remove(transformedView);
                    if (clipSet.isEmpty()) {
                        view.setClipChildren(clipChildren.booleanValue());
                        view.setClipToPadding(clipToPadding.booleanValue());
                        view.setTag(R.id.clip_children_set_tag, null);
                        if (row != null) {
                            row.setClipToActualHeight(true);
                        }
                    }
                } else {
                    clipSet.add(transformedView);
                    view.setClipChildren(false);
                    view.setClipToPadding(false);
                    if (row != null && row.isChildInGroup()) {
                        row.setClipToActualHeight(false);
                    }
                }
                if (row == null || row.isChildInGroup()) {
                    ViewParent parent2 = view.getParent();
                    if (parent2 instanceof ViewGroup) {
                        parent = parent2;
                    } else {
                        return;
                    }
                } else {
                    return;
                }
            }
        }
    }

    public int[] getLaidOutLocationOnScreen() {
        int[] location = getLocationOnScreen();
        location[0] = (int) (((float) location[0]) - this.mTransformedView.getTranslationX());
        location[1] = (int) (((float) location[1]) - this.mTransformedView.getTranslationY());
        return location;
    }

    public int[] getLocationOnScreen() {
        this.mTransformedView.getLocationOnScreen(this.mOwnPosition);
        int[] iArr = this.mOwnPosition;
        iArr[0] = (int) (((float) iArr[0]) - ((1.0f - this.mTransformedView.getScaleX()) * this.mTransformedView.getPivotX()));
        int[] iArr2 = this.mOwnPosition;
        iArr2[1] = (int) (((float) iArr2[1]) - ((1.0f - this.mTransformedView.getScaleY()) * this.mTransformedView.getPivotY()));
        return this.mOwnPosition;
    }

    /* access modifiers changed from: protected */
    public boolean sameAs(TransformState otherState) {
        return false;
    }

    public void appear(float transformationAmount, TransformableView otherView) {
        if (transformationAmount == 0.0f) {
            prepareFadeIn();
        }
        CrossFadeHelper.fadeIn(this.mTransformedView, transformationAmount);
    }

    public void disappear(float transformationAmount, TransformableView otherView) {
        CrossFadeHelper.fadeOut(this.mTransformedView, transformationAmount);
    }

    public static TransformState createFrom(View view) {
        if (view instanceof TextView) {
            TextViewTransformState result = TextViewTransformState.obtain();
            result.initFrom(view);
            return result;
        } else if (view.getId() == 16908687) {
            ActionListTransformState result2 = ActionListTransformState.obtain();
            result2.initFrom(view);
            return result2;
        } else if (view instanceof ImageView) {
            ImageTransformState result3 = ImageTransformState.obtain();
            result3.initFrom(view);
            return result3;
        } else if (view instanceof ProgressBar) {
            ProgressTransformState result4 = ProgressTransformState.obtain();
            result4.initFrom(view);
            return result4;
        } else {
            TransformState result5 = obtain();
            result5.initFrom(view);
            return result5;
        }
    }

    public void recycle() {
        reset();
        if (getClass() == TransformState.class) {
            sInstancePool.release(this);
        }
    }

    public void setTransformationEndY(float transformationEndY) {
        this.mTransformationEndY = transformationEndY;
    }

    public float getTransformationStartX() {
        Object tag = this.mTransformedView.getTag(R.id.transformation_start_x_tag);
        if (tag == null) {
            return -1.0f;
        }
        return ((Float) tag).floatValue();
    }

    public float getTransformationStartY() {
        Object tag = this.mTransformedView.getTag(R.id.transformation_start_y_tag);
        if (tag == null) {
            return -1.0f;
        }
        return ((Float) tag).floatValue();
    }

    public float getTransformationStartScaleX() {
        Object tag = this.mTransformedView.getTag(R.id.transformation_start_scale_x_tag);
        if (tag == null) {
            return -1.0f;
        }
        return ((Float) tag).floatValue();
    }

    public float getTransformationStartScaleY() {
        Object tag = this.mTransformedView.getTag(R.id.transformation_start_scale_y_tag);
        if (tag == null) {
            return -1.0f;
        }
        return ((Float) tag).floatValue();
    }

    public void setTransformationStartX(float transformationStartX) {
        this.mTransformedView.setTag(R.id.transformation_start_x_tag, Float.valueOf(transformationStartX));
    }

    public void setTransformationStartY(float transformationStartY) {
        this.mTransformedView.setTag(R.id.transformation_start_y_tag, Float.valueOf(transformationStartY));
    }

    private void setTransformationStartScaleX(float startScaleX) {
        this.mTransformedView.setTag(R.id.transformation_start_scale_x_tag, Float.valueOf(startScaleX));
    }

    private void setTransformationStartScaleY(float startScaleY) {
        this.mTransformedView.setTag(R.id.transformation_start_scale_y_tag, Float.valueOf(startScaleY));
    }

    /* access modifiers changed from: protected */
    public void reset() {
        this.mTransformedView = null;
        this.mTransformationEndX = -1.0f;
        this.mTransformationEndY = -1.0f;
    }

    public void setVisible(boolean visible, boolean force) {
        if (force || this.mTransformedView.getVisibility() != 8) {
            if (this.mTransformedView.getVisibility() != 8) {
                this.mTransformedView.setVisibility(visible ? 0 : 4);
            }
            this.mTransformedView.animate().cancel();
            this.mTransformedView.setAlpha(visible ? 1.0f : 0.0f);
            resetTransformedView();
        }
    }

    public void prepareFadeIn() {
        resetTransformedView();
    }

    /* access modifiers changed from: protected */
    public void resetTransformedView() {
        this.mTransformedView.setTranslationX(0.0f);
        this.mTransformedView.setTranslationY(0.0f);
        this.mTransformedView.setScaleX(1.0f);
        this.mTransformedView.setScaleY(1.0f);
        setClippingDeactivated(this.mTransformedView, false);
        abortTransformation();
    }

    public void abortTransformation() {
        this.mTransformedView.setTag(R.id.transformation_start_x_tag, Float.valueOf(-1.0f));
        this.mTransformedView.setTag(R.id.transformation_start_y_tag, Float.valueOf(-1.0f));
        this.mTransformedView.setTag(R.id.transformation_start_scale_x_tag, Float.valueOf(-1.0f));
        this.mTransformedView.setTag(R.id.transformation_start_scale_y_tag, Float.valueOf(-1.0f));
    }

    public static TransformState obtain() {
        TransformState instance = (TransformState) sInstancePool.acquire();
        if (instance != null) {
            return instance;
        }
        return new TransformState();
    }

    public View getTransformedView() {
        return this.mTransformedView;
    }
}
