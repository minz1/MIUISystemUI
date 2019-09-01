package com.android.settingslib.miuisettings.preference;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;

public class VolumePreference extends android.preference.VolumePreference implements PreferenceApiDiff {
    private PreferenceDelegate mDelegate;

    public VolumePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /* JADX WARNING: type inference failed for: r1v0, types: [android.preference.Preference, com.android.settingslib.miuisettings.preference.PreferenceApiDiff, com.android.settingslib.miuisettings.preference.VolumePreference] */
    private void init() {
        this.mDelegate = new PreferenceDelegate(this, this);
    }

    /* access modifiers changed from: protected */
    public void onBindView(View view) {
        this.mDelegate.onBindViewStart(view);
        VolumePreference.super.onBindView(view);
        this.mDelegate.onBindViewEnd(view);
    }

    /* access modifiers changed from: protected */
    public void onAttachedToHierarchy(PreferenceManager preferenceManager) {
        VolumePreference.super.onAttachedToHierarchy(preferenceManager);
        this.mDelegate.onAttachedToHierarchy(preferenceManager);
    }

    public void onAttached() {
    }

    public void onBindViewHolder(PreferenceViewHolder holder) {
    }
}
