package com.android.settingslib.miuisettings.preference;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;

public class RadioButtonPreference extends miui.preference.RadioButtonPreference implements PreferenceApiDiff {
    private PreferenceDelegate mDelegate;

    public RadioButtonPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /* JADX WARNING: type inference failed for: r1v0, types: [com.android.settingslib.miuisettings.preference.RadioButtonPreference, android.preference.Preference, com.android.settingslib.miuisettings.preference.PreferenceApiDiff] */
    private void init() {
        this.mDelegate = new PreferenceDelegate(this, this);
    }

    /* access modifiers changed from: protected */
    public void onBindView(View view) {
        this.mDelegate.onBindViewStart(view);
        RadioButtonPreference.super.onBindView(view);
        this.mDelegate.onBindViewEnd(view);
    }

    /* access modifiers changed from: protected */
    public void onAttachedToHierarchy(PreferenceManager preferenceManager) {
        RadioButtonPreference.super.onAttachedToHierarchy(preferenceManager);
        this.mDelegate.onAttachedToHierarchy(preferenceManager);
    }

    public void onAttached() {
    }

    public void onBindViewHolder(PreferenceViewHolder holder) {
    }
}
