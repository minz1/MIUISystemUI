package com.android.settingslib.miuisettings.preference;

import android.preference.Preference;
import android.util.Log;
import java.lang.reflect.Method;

public class PreferenceUtils {
    private static Method Preference_callChangeListener;
    private static String TAG = "Miui_Preference";

    public static boolean call_Preference_callChangeListener(Preference preference, Object obj) {
        if (Preference_callChangeListener == null) {
            try {
                Preference_callChangeListener = preference.getClass().getDeclaredMethod("callChangeListener", new Class[]{Object.class});
                Preference_callChangeListener.setAccessible(true);
            } catch (Exception e) {
                Log.e(TAG, "", e);
                return false;
            }
        }
        try {
            return ((Boolean) Preference_callChangeListener.invoke(preference, new Object[]{obj})).booleanValue();
        } catch (Exception e2) {
            Log.e(TAG, "", e2);
            return false;
        }
    }
}
