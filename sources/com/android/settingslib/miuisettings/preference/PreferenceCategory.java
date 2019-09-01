package com.android.settingslib.miuisettings.preference;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;

public class PreferenceCategory extends android.preference.PreferenceCategory implements PreferenceApiDiff {
    private PreferenceDelegate mDelegate;

    public PreferenceCategory(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        this.mDelegate = new PreferenceDelegate(this, this);
    }

    /* access modifiers changed from: protected */
    public final void onBindView(View view) {
        this.mDelegate.onBindViewStart(view);
        super.onBindView(view);
        this.mDelegate.onBindViewEnd(view);
    }

    /* access modifiers changed from: protected */
    public void onAttachedToHierarchy(PreferenceManager preferenceManager) {
        super.onAttachedToHierarchy(preferenceManager);
        this.mDelegate.onAttachedToHierarchy(preferenceManager);
    }

    public void onAttached() {
    }

    public void onBindViewHolder(PreferenceViewHolder holder) {
    }
}
