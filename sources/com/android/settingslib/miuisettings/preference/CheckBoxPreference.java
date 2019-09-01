package com.android.settingslib.miuisettings.preference;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;

public class CheckBoxPreference extends android.preference.CheckBoxPreference implements PreferenceApiDiff {
    private PreferenceDelegate mDelegate;

    public CheckBoxPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        boolean showIcon = false;
        if (attrs != null) {
            showIcon = attrs.getAttributeBooleanValue("http://schemas.android.com/apk/miuisettings", "showIcon", false);
        }
        this.mDelegate = new PreferenceDelegate(this, this, showIcon);
    }

    /* access modifiers changed from: protected */
    public void onBindView(View view) {
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
