package com.android.settingslib.miuisettings.preference;

import android.content.Context;
import android.os.Parcelable;
import android.preference.Preference;
import android.util.AttributeSet;

public class MultiSelectListPreference extends android.preference.MultiSelectListPreference {
    public MultiSelectListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /* access modifiers changed from: protected */
    public void onRestoreInstanceState(Parcelable state) {
        if (state == null) {
            super.onRestoreInstanceState(state);
        } else {
            super.onRestoreInstanceState(((Preference.BaseSavedState) state).getSuperState());
        }
    }
}
