package com.miui.browser.webapps;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import java.io.File;
import miui.content.res.IconCustomizer;

public class WebAppInfo {
    public Drawable mDrawable;
    public String mIconPath;
    public String mLabel;
    public String mTaskAffinity;

    public WebAppInfo(Cursor cursor) {
        this.mIconPath = getCursorString("icon_path", cursor);
        this.mLabel = getCursorString("label", cursor);
        this.mTaskAffinity = getCursorString("affinity", cursor);
        if (this.mIconPath != null) {
            byte[] bytes = getCursorBlob("icon_data", cursor);
            if (bytes != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bitmap != null) {
                    this.mDrawable = IconCustomizer.generateIconStyleDrawable(new BitmapDrawable(bitmap), true);
                }
            }
        }
    }

    private byte[] getCursorBlob(String name, Cursor cursor) {
        try {
            int index = cursor.getColumnIndexOrThrow(name);
            if (index >= 0) {
                return cursor.getBlob(index);
            }
        } catch (Exception e) {
        }
        return null;
    }

    private String getCursorString(String name, Cursor cursor) {
        return cursor.getString(cursor.getColumnIndexOrThrow(name));
    }

    public Drawable getIcon(Context context) {
        if (this.mDrawable == null) {
            if (this.mIconPath == null || context == null || !new File(this.mIconPath).exists()) {
                return null;
            }
            BitmapDrawable bitmapDrawable = new BitmapDrawable(context.getResources(), this.mIconPath);
            if (bitmapDrawable.getBitmap() == null) {
                return null;
            }
            this.mDrawable = IconCustomizer.generateIconStyleDrawable(bitmapDrawable, true);
        }
        return this.mDrawable;
    }
}
