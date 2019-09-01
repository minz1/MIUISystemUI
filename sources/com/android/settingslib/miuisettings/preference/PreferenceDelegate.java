package com.android.settingslib.miuisettings.preference;

import android.preference.Preference;
import android.preference.PreferenceManager;
import android.view.View;
import java.lang.reflect.Field;

public class PreferenceDelegate {
    private static Field Preference_mIconResId;
    private static String TAG = "Miui_Preference";
    private PreferenceApiDiff mApiDiff;
    private Preference mPreference;
    private boolean mShowIcon;
    private boolean mVisible;

    public PreferenceDelegate(Preference preference, PreferenceApiDiff apiDiff) {
        this(preference, apiDiff, false);
    }

    public PreferenceDelegate(Preference preference, PreferenceApiDiff apiDiff, boolean showIcon) {
        this.mVisible = true;
        this.mShowIcon = false;
        this.mPreference = preference;
        this.mApiDiff = apiDiff;
        this.mShowIcon = showIcon;
    }

    public void onBindViewStart(View view) {
        if ((!(this.mPreference instanceof PreferenceFeature) || !((PreferenceFeature) this.mPreference).hasIcon()) && !this.mShowIcon) {
            hideIcon();
        }
    }

    private void hideIcon() {
        this.mPreference.setIcon(null);
        if (Preference_mIconResId == null) {
            try {
                Preference_mIconResId = Preference.class.getDeclaredField("mIconResId");
                Preference_mIconResId.setAccessible(true);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        try {
            Preference_mIconResId.set(this.mPreference, 0);
        } catch (Exception e2) {
            throw new RuntimeException(e2);
        }
    }

    public void onBindViewEnd(View view) {
        this.mApiDiff.onBindViewHolder(new PreferenceViewHolder(view));
    }

    /* access modifiers changed from: protected */
    public void onAttachedToHierarchy(PreferenceManager preferenceManager) {
        this.mApiDiff.onAttached();
    }
}
