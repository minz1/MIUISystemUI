package com.miui.internal.policy.impl.AwesomeLockScreenImp;

import android.text.TextUtils;
import miui.maml.ObjectFactory;
import miui.maml.ScreenElementRoot;
import miui.maml.elements.BitmapProvider;

public class LockscreenBitmapProviderFactory extends ObjectFactory.BitmapProviderFactory {
    /* access modifiers changed from: protected */
    public BitmapProvider doCreate(ScreenElementRoot root, String type) {
        if (TextUtils.equals(type, "Screenshot")) {
            return new ScreenshotProvider(root);
        }
        return null;
    }
}
