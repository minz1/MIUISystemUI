package com.android.systemui.miui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableWrapper;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RotateDrawable;
import android.graphics.drawable.ScaleDrawable;
import android.os.Build;

public class DrawableUtils {
    public static Drawable findDrawableById(Drawable parent, int id) {
        if (parent == null) {
            return null;
        }
        if (parent instanceof LayerDrawable) {
            LayerDrawable layer = (LayerDrawable) parent;
            for (int i = 0; i < layer.getNumberOfLayers(); i++) {
                if (layer.getId(i) == id) {
                    return layer.getDrawable(i);
                }
                Drawable result = findDrawableById(layer.getDrawable(i), id);
                if (result != null) {
                    return result;
                }
            }
        } else if (isWrapperDrawable(parent)) {
            return findDrawableById(getWrappedDrawable(parent, null), id);
        }
        return null;
    }

    private static boolean isWrapperDrawable(Drawable drawable) {
        return (Build.VERSION.SDK_INT >= 23 && (drawable instanceof DrawableWrapper)) || (drawable instanceof ScaleDrawable) || (drawable instanceof ClipDrawable) || (drawable instanceof InsetDrawable) || (drawable instanceof RotateDrawable);
    }

    private static Drawable getWrappedDrawable(Drawable parent, Drawable def) {
        if (Build.VERSION.SDK_INT >= 23 && (parent instanceof DrawableWrapper)) {
            return ((DrawableWrapper) parent).getDrawable();
        }
        if (parent instanceof ScaleDrawable) {
            return ((ScaleDrawable) parent).getDrawable();
        }
        if (parent instanceof ClipDrawable) {
            return ((ClipDrawable) parent).getDrawable();
        }
        if (parent instanceof InsetDrawable) {
            return ((InsetDrawable) parent).getDrawable();
        }
        if (parent instanceof RotateDrawable) {
            return ((RotateDrawable) parent).getDrawable();
        }
        return def;
    }

    public static LayerDrawable combine(Drawable background, Drawable foreground, int fgGravity) {
        LayerDrawable result = new LayerDrawable(new Drawable[]{background, foreground});
        result.setLayerGravity(1, fgGravity);
        return result;
    }

    public static Bitmap drawable2Bitmap(Drawable drawable) {
        Bitmap.Config config;
        if (drawable == null) {
            return null;
        }
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }
        int intrinsicWidth = drawable.getIntrinsicWidth();
        int intrinsicHeight = drawable.getIntrinsicHeight();
        if (drawable.getOpacity() != -1) {
            config = Bitmap.Config.ARGB_8888;
        } else {
            config = Bitmap.Config.RGB_565;
        }
        Bitmap bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, config);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}
