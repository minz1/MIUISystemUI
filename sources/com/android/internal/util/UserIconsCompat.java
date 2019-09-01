package com.android.internal.util;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;

public class UserIconsCompat {
    public static Drawable getDefaultUserIcon(Resources resources, int userId, boolean light) {
        return UserIcons.getDefaultUserIcon(resources, userId, light);
    }
}
