package com.android.systemui.qs.tileimpl;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.view.View;
import android.widget.ImageView;
import com.android.systemui.R;
import com.android.systemui.miui.DrawableUtils;
import com.android.systemui.plugins.qs.QSIconView;
import com.android.systemui.plugins.qs.QSTile;
import java.util.Objects;

public class QSIconViewImpl extends QSIconView {
    private boolean mAnimationEnabled = true;
    private ValueAnimator mAnimator;
    protected final View mIcon;
    protected final int mIconBgSizePx;
    protected int mIconColorDisabled;
    protected int mIconColorEnabled;
    protected final int mIconSizePx;
    protected final int mTilePaddingBelowIconPx;

    public QSIconViewImpl(Context context) {
        super(context);
        Resources res = context.getResources();
        this.mIconSizePx = res.getDimensionPixelSize(R.dimen.qs_tile_icon_size);
        this.mIconBgSizePx = res.getDimensionPixelSize(R.dimen.qs_tile_icon_bg_size);
        this.mTilePaddingBelowIconPx = res.getDimensionPixelSize(R.dimen.qs_tile_padding_below_icon);
        this.mIconColorEnabled = res.getColor(R.color.qs_tile_icon_enabled_color);
        this.mIconColorDisabled = res.getColor(R.color.qs_tile_icon_disabled_color);
        this.mIcon = createIcon();
        addView(this.mIcon);
    }

    public void setAnimationEnabled(boolean enabled) {
        this.mAnimationEnabled = enabled;
    }

    public View getIconView() {
        return this.mIcon;
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = View.MeasureSpec.getSize(widthMeasureSpec);
        this.mIcon.measure(View.MeasureSpec.makeMeasureSpec(w, getIconMeasureMode()), exactly(this.mIconBgSizePx));
        setMeasuredDimension(w, this.mIcon.getMeasuredHeight());
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        layout(this.mIcon, (getMeasuredWidth() - this.mIcon.getMeasuredWidth()) / 2, 0);
    }

    public void updateResources() {
        Resources res = getResources();
        this.mIconColorEnabled = res.getColor(R.color.qs_tile_icon_enabled_color);
        this.mIconColorDisabled = res.getColor(R.color.qs_tile_icon_disabled_color);
    }

    public void setIcon(QSTile.State state) {
        setIcon((ImageView) this.mIcon, state);
    }

    /* access modifiers changed from: protected */
    public void setIcon(ImageView iv, QSTile.State state) {
        updateIcon(iv, state);
    }

    /* access modifiers changed from: protected */
    public void updateIcon(ImageView iv, QSTile.State state) {
        int bgResId;
        int i;
        if (!Objects.equals(state.icon, iv.getTag(R.id.qs_icon_tag))) {
            iv.setTag(R.id.qs_icon_tag, state.icon);
            if (state.state == 2) {
                bgResId = R.drawable.ic_qs_bg_enabled;
            } else {
                bgResId = R.drawable.ic_qs_bg_disabled;
            }
            Drawable bgDrawable = getResources().getDrawable(bgResId);
            Drawable drawable = state.icon != null ? state.icon.getDrawable(this.mContext) : null;
            if (drawable != null) {
                boolean shouldAnimate = false;
                drawable.setAutoMirrored(false);
                if (state.state == 2) {
                    i = this.mIconColorEnabled;
                } else {
                    i = this.mIconColorDisabled;
                }
                drawable.setTint(i);
                LayerDrawable combined = DrawableUtils.combine(bgDrawable, drawable, 17);
                combined.setLayerSize(1, this.mIconSizePx, this.mIconSizePx);
                Drawable drawable2 = combined;
                boolean newIsActive = state.state == 2;
                boolean oldIsActive = newIsActive;
                if (iv.getTag(R.id.qs_icon_state_tag) != null) {
                    oldIsActive = ((Integer) iv.getTag(R.id.qs_icon_state_tag)).intValue() == 2;
                }
                iv.setTag(R.id.qs_icon_state_tag, Integer.valueOf(state.state));
                if (iv.isShown() && this.mAnimationEnabled && iv.getTag(R.id.qs_icon_tag) != null && oldIsActive != newIsActive) {
                    shouldAnimate = true;
                }
                if (shouldAnimate) {
                    startAnimatorIfNeed(iv, drawable2);
                } else {
                    iv.setImageDrawable(drawable2);
                }
            }
        }
    }

    private void startAnimatorIfNeed(final ImageView iv, final Drawable drawable) {
        if (this.mAnimator != null) {
            this.mAnimator.cancel();
            this.mAnimator = null;
        }
        if (this.mAnimator == null) {
            this.mAnimator = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
            this.mAnimator.setDuration(350);
        }
        this.mAnimator.removeAllListeners();
        this.mAnimator.removeAllUpdateListeners();
        this.mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            boolean imageChanged = false;

            public void onAnimationUpdate(ValueAnimator animation) {
                float value = ((Float) animation.getAnimatedValue()).floatValue();
                if (value < 0.5f) {
                    iv.setScaleX(1.0f - value);
                    iv.setScaleY(1.0f - value);
                    return;
                }
                if (!this.imageChanged) {
                    iv.setImageDrawable(drawable);
                    this.imageChanged = true;
                }
                iv.setScaleX(value);
                iv.setScaleY(value);
            }
        });
        this.mAnimator.start();
    }

    /* access modifiers changed from: protected */
    public int getIconMeasureMode() {
        return 1073741824;
    }

    /* access modifiers changed from: protected */
    public View createIcon() {
        ImageView icon = new ImageView(this.mContext);
        icon.setId(16908294);
        icon.setScaleType(ImageView.ScaleType.FIT_XY);
        return icon;
    }

    /* access modifiers changed from: protected */
    public final int exactly(int size) {
        return View.MeasureSpec.makeMeasureSpec(size, 1073741824);
    }

    /* access modifiers changed from: protected */
    public final void layout(View child, int left, int top) {
        child.layout(left, top, child.getMeasuredWidth() + left, child.getMeasuredHeight() + top);
    }
}
