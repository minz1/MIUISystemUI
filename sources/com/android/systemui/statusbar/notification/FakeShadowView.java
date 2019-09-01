package com.android.systemui.statusbar.notification;

import android.content.Context;
import android.graphics.Outline;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.LinearLayout;
import com.android.systemui.R;
import com.android.systemui.statusbar.AlphaOptimizedFrameLayout;

public class FakeShadowView extends AlphaOptimizedFrameLayout {
    /* access modifiers changed from: private */
    public View mFakeShadow;
    /* access modifiers changed from: private */
    public float mOutlineAlpha;
    private final int mShadowMinHeight;

    public FakeShadowView(Context context) {
        this(context, null);
    }

    public FakeShadowView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FakeShadowView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public FakeShadowView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mFakeShadow = new View(context);
        this.mFakeShadow.setVisibility(4);
        this.mFakeShadow.setLayoutParams(new LinearLayout.LayoutParams(-1, (int) (48.0f * getResources().getDisplayMetrics().density)));
        this.mFakeShadow.setOutlineProvider(new ViewOutlineProvider() {
            public void getOutline(View view, Outline outline) {
                outline.setRect(0, 0, FakeShadowView.this.getWidth(), FakeShadowView.this.mFakeShadow.getHeight());
                outline.setAlpha(FakeShadowView.this.mOutlineAlpha);
            }
        });
        addView(this.mFakeShadow);
        this.mShadowMinHeight = Math.max(1, context.getResources().getDimensionPixelSize(R.dimen.notification_divider_height));
    }

    public void setFakeShadowTranslationZ(float fakeShadowTranslationZ, float outlineAlpha, int shadowYEnd, int outlineTranslation) {
        if (fakeShadowTranslationZ == 0.0f) {
            this.mFakeShadow.setVisibility(4);
            return;
        }
        this.mFakeShadow.setVisibility(0);
        this.mFakeShadow.setTranslationZ(Math.max((float) this.mShadowMinHeight, fakeShadowTranslationZ));
        this.mFakeShadow.setTranslationX((float) outlineTranslation);
        this.mFakeShadow.setTranslationY((float) (shadowYEnd - this.mFakeShadow.getHeight()));
        if (outlineAlpha != this.mOutlineAlpha) {
            this.mOutlineAlpha = outlineAlpha;
            this.mFakeShadow.invalidateOutline();
        }
    }
}
