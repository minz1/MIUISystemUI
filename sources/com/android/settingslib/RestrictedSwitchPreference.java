package com.android.settingslib;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v4.content.res.TypedArrayUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;
import com.android.settingslib.miuisettings.preference.PreferenceViewHolder;
import com.android.settingslib.miuisettings.preference.SwitchPreference;

public class RestrictedSwitchPreference extends SwitchPreference {
    RestrictedPreferenceHelper mHelper;
    CharSequence mRestrictedSwitchSummary;
    boolean mUseAdditionalSummary;

    public RestrictedSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs);
        this.mUseAdditionalSummary = false;
        setWidgetLayoutResource(R.layout.restricted_switch_widget);
        this.mHelper = new RestrictedPreferenceHelper(context, this, attrs);
        if (attrs != null) {
            TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.RestrictedSwitchPreference);
            TypedValue useAdditionalSummary = attributes.peekValue(R.styleable.RestrictedSwitchPreference_useAdditionalSummary);
            if (useAdditionalSummary != null) {
                this.mUseAdditionalSummary = useAdditionalSummary.type == 18 && useAdditionalSummary.data != 0;
            }
            TypedValue restrictedSwitchSummary = attributes.peekValue(R.styleable.RestrictedSwitchPreference_restrictedSwitchSummary);
            if (restrictedSwitchSummary != null && restrictedSwitchSummary.type == 3) {
                if (restrictedSwitchSummary.resourceId != 0) {
                    this.mRestrictedSwitchSummary = context.getText(restrictedSwitchSummary.resourceId);
                } else {
                    this.mRestrictedSwitchSummary = restrictedSwitchSummary.string;
                }
            }
        }
        this.mUseAdditionalSummary = false;
        if (this.mUseAdditionalSummary) {
            setLayoutResource(R.layout.restricted_switch_preference);
            useAdminDisabledSummary(false);
        }
    }

    public RestrictedSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public RestrictedSwitchPreference(Context context, AttributeSet attrs) {
        this(context, attrs, TypedArrayUtils.getAttr(context, R.attr.switchPreferenceStyle, 16843629));
    }

    public void onBindViewHolder(PreferenceViewHolder holder) {
        CharSequence switchSummary;
        super.onBindViewHolder(holder);
        this.mHelper.onBindViewHolder(holder);
        if (this.mRestrictedSwitchSummary == null) {
            switchSummary = getContext().getText(isChecked() ? R.string.enabled_by_admin : R.string.disabled_by_admin);
        } else {
            switchSummary = this.mRestrictedSwitchSummary;
        }
        View restrictedIcon = holder.findViewById(R.id.restricted_icon);
        View switchWidget = holder.findViewById(16908289);
        if (restrictedIcon != null) {
            restrictedIcon.setVisibility(isDisabledByAdmin() ? 0 : 8);
        }
        if (switchWidget != null) {
            switchWidget.setVisibility(isDisabledByAdmin() ? 8 : 0);
        }
        if (this.mUseAdditionalSummary) {
            TextView additionalSummaryView = (TextView) holder.findViewById(R.id.additional_summary);
            if (additionalSummaryView == null) {
                return;
            }
            if (isDisabledByAdmin()) {
                additionalSummaryView.setText(switchSummary);
                additionalSummaryView.setVisibility(0);
                return;
            }
            additionalSummaryView.setVisibility(8);
            return;
        }
        TextView summaryView = (TextView) holder.findViewById(16908304);
        if (summaryView != null && isDisabledByAdmin()) {
            summaryView.setText(switchSummary);
            summaryView.setVisibility(0);
        }
    }

    public void performClick(PreferenceScreen preferenceScreen) {
        if (!this.mHelper.performClick()) {
            super.performClick(preferenceScreen);
        }
    }

    public void useAdminDisabledSummary(boolean useSummary) {
        this.mHelper.useAdminDisabledSummary(useSummary);
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
