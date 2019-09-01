package com.miui.internal.policy.impl.AwesomeLockScreenImp;

import miui.maml.ScreenElementRoot;
import miui.maml.elements.ScreenElement;
import miui.maml.elements.ScreenElementFactory;
import org.w3c.dom.Element;

public class LockScreenElementFactory extends ScreenElementFactory {
    public ScreenElement createInstance(Element ele, ScreenElementRoot root) {
        String tag = ele.getTagName();
        if (tag.equalsIgnoreCase("Unlocker")) {
            return new UnlockerScreenElement(ele, (LockScreenRoot) root);
        }
        if (tag.equalsIgnoreCase("Wallpaper")) {
            return new WallpaperScreenElement(ele, root);
        }
        return LockScreenElementFactory.super.createInstance(ele, root);
    }
}
