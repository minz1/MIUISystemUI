package com.android.settingslib.miuisettings.preference;

import android.content.Context;
import android.util.AttributeSet;

public class IconPreference extends Preference implements PreferenceFeature {
    public IconPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public boolean hasIcon() {
        return true;
    }
}
