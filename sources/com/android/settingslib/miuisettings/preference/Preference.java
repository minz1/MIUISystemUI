package com.android.settingslib.miuisettings.preference;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import miui.R;

public class Preference extends android.preference.Preference implements PreferenceApiDiff {
    private PreferenceDelegate mDelegate;
    private boolean mForceRightArrow = false;
    private boolean mShowRightArrow = true;

    public Preference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public Preference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    public Preference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
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
        View rightArrow = holder.itemView.findViewById(R.id.arrow_right);
        if (rightArrow != null && this.mForceRightArrow) {
            rightArrow.setVisibility(this.mShowRightArrow ? 0 : 8);
        }
    }
}
