package com.miui.internal.policy.impl.AwesomeLockScreenImp;

import android.graphics.drawable.BitmapDrawable;
import com.android.keyguard.wallpaper.KeyguardWallpaperUtils;
import miui.maml.ScreenElementRoot;
import miui.maml.elements.ImageScreenElement;
import org.w3c.dom.Element;

public class WallpaperScreenElement extends ImageScreenElement {
    public WallpaperScreenElement(Element node, ScreenElementRoot root) {
        super(node, root);
    }

    public void init() {
        WallpaperScreenElement.super.init();
        BitmapDrawable drawable = (BitmapDrawable) KeyguardWallpaperUtils.getLockWallpaperPreview(getContext().mContext);
        if (drawable != null) {
            setBitmap(drawable.getBitmap());
        }
    }
}
