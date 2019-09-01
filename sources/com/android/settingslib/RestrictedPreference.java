package com.android.settingslib;

import android.content.Context;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v4.content.res.TypedArrayUtils;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.settingslib.miuisettings.preference.PreferenceViewHolder;
import miui.R;

public class RestrictedPreference extends TwoTargetPreference {
    RestrictedPreferenceHelper mHelper;
    private CharSequence mRightValue;
    private boolean mShowRightArrow;
    private boolean mSummary2Value;

    public RestrictedPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mShowRightArrow = true;
        this.mHelper = new RestrictedPreferenceHelper(context, this, attrs);
        setLayoutResource(R.layout.preference_value);
    }

    public RestrictedPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public RestrictedPreference(Context context, AttributeSet attrs) {
        this(context, attrs, TypedArrayUtils.getAttr(context, R.attr.preferenceStyle, 16842894));
    }

    /* access modifiers changed from: protected */
    public int getSecondTargetResId() {
        return R.layout.restricted_icon;
    }

    /* access modifiers changed from: protected */
    public boolean shouldHideSecondTarget() {
        return !isDisabledByAdmin();
    }

    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        this.mHelper.onBindViewHolder(holder);
        View restrictedIcon = holder.findViewById(R.id.restricted_icon);
        if (restrictedIcon != null) {
            restrictedIcon.setVisibility(isDisabledByAdmin() ? 0 : 8);
        }
        View view = holder.itemView;
        ImageView rightArrowView = (ImageView) view.findViewById(R.id.arrow_right);
        if (rightArrowView != null) {
            rightArrowView.setVisibility((isDisabledByAdmin() || !this.mShowRightArrow) ? 8 : 0);
        }
        TextView valueView = (TextView) view.findViewById(R.id.value_right);
        TextView summaryView = (TextView) view.findViewById(16908304);
        if (valueView != null && this.mSummary2Value) {
            CharSequence value = getSummary();
            if (!TextUtils.isEmpty(value)) {
                valueView.setText(value);
                valueView.setVisibility(0);
            } else {
                valueView.setVisibility(8);
            }
            if (summaryView != null) {
                summaryView.setVisibility(8);
            }
        } else if (valueView != null && !TextUtils.isEmpty(this.mRightValue)) {
            valueView.setText(this.mRightValue);
        }
    }

    public void performClick(PreferenceScreen preferenceScreen) {
        if (!this.mHelper.performClick()) {
            super.performClick(preferenceScreen);
        }
    }

    /* access modifiers changed from: protected */
    public void onAttachedToHierarchy(PreferenceManager preferenceManager) {
        this.mHelper.onAttachedToHierarchy();
        super.onAttachedToHierarchy(preferenceManager);
    }

    public void setEnabled(boolean enabled) {
        if (!enabled || !isDisabledByAdmin()) {
            super.setEnabled(enabled);
        } else {
            this.mHelper.setDisabledByAdmin(null);
        }
    }

    public boolean isDisabledByAdmin() {
        return this.mHelper.isDisabledByAdmin();
    }
}
