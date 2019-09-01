package com.android.systemui.statusbar.policy;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;

public class KeyButtonDrawable extends LayerDrawable {
    private final boolean mHasDarkDrawable;

    public static KeyButtonDrawable create(Drawable lightDrawable, Drawable darkDrawable) {
        if (darkDrawable != null) {
            return new KeyButtonDrawable(new Drawable[]{lightDrawable.mutate(), darkDrawable.mutate()});
        }
        return new KeyButtonDrawable(new Drawable[]{lightDrawable.mutate()});
    }

    private KeyButtonDrawable(Drawable[] drawables) {
        super(drawables);
        boolean z = false;
        for (int i = 0; i < drawables.length; i++) {
            setLayerGravity(i, 17);
        }
        mutate();
        this.mHasDarkDrawable = drawables.length > 1 ? true : z;
        setDarkIntensity(0.0f);
    }

    public void setDarkIntensity(float intensity) {
        if (this.mHasDarkDrawable) {
            getDrawable(0).setAlpha((int) ((1.0f - intensity) * 255.0f));
            getDrawable(1).setAlpha((int) (255.0f * intensity));
            invalidateSelf();
        }
    }
}
