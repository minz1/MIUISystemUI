package com.android.systemui.proxy;

import android.content.ContentResolver;
import android.provider.Settings;

public class Settings {

    public static final class Global {
        public static int getInt(ContentResolver cr, String name, int def) {
            return Settings.Global.getInt(cr, name, def);
        }
    }
}
