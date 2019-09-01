package com.miui.internal.policy.impl.AwesomeLockScreenImp;

import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.SurfaceControl;
import com.android.keyguard.wallpaper.KeyguardWallpaperUtils;
import miui.maml.ScreenElementRoot;
import miui.maml.elements.BitmapProvider;

public class ScreenshotProvider extends BitmapProvider {
    public ScreenshotProvider(ScreenElementRoot root) {
        super(root);
    }

    public void reset() {
        ScreenshotProvider.super.reset();
        if (Boolean.parseBoolean(this.mRoot.getRawAttr("__is_secure"))) {
            Drawable draw = KeyguardWallpaperUtils.getLockWallpaperPreview(this.mRoot.getContext().mContext);
            if (draw instanceof BitmapDrawable) {
                this.mVersionedBitmap.setBitmap(((BitmapDrawable) draw).getBitmap());
                return;
            }
            return;
        }
        this.mVersionedBitmap.setBitmap(SurfaceControl.screenshot(new Rect(), this.mRoot.getScreenWidth(), this.mRoot.getScreenHeight(), 0, 109999, false, 0));
    }
}
