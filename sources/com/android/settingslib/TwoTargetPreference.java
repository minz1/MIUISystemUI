package com.android.settingslib;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.android.settingslib.miuisettings.preference.Preference;
import com.android.settingslib.miuisettings.preference.PreferenceFeature;
import com.android.settingslib.miuisettings.preference.PreferenceViewHolder;

public class TwoTargetPreference extends Preference implements PreferenceFeature {
    private boolean mHasIcon = false;
    private int mIconSize;
    private int mMediumIconSize;
    private int mSmallIconSize;

    public boolean hasIcon() {
        return this.mHasIcon;
    }

    public TwoTargetPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    public TwoTargetPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setLayoutResource(R.layout.preference_two_target);
        this.mSmallIconSize = context.getResources().getDimensionPixelSize(R.dimen.two_target_pref_small_icon_size);
        this.mMediumIconSize = context.getResources().getDimensionPixelSize(R.dimen.two_target_pref_medium_icon_size);
        int secondTargetResId = getSecondTargetResId();
        if (secondTargetResId != 0) {
            setWidgetLayoutResource(secondTargetResId);
        }
    }

    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        ImageView icon = (ImageView) holder.itemView.findViewById(16908294);
        if (icon != null) {
            switch (this.mIconSize) {
                case 1:
                    icon.setLayoutParams(new LinearLayout.LayoutParams(this.mMediumIconSize, this.mMediumIconSize));
                    break;
                case 2:
                    icon.setLayoutParams(new LinearLayout.LayoutParams(this.mSmallIconSize, this.mSmallIconSize));
                    break;
            }
        }
        View divider = holder.findViewById(R.id.two_target_divider);
        View widgetFrame = holder.findViewById(16908312);
        boolean shouldHideSecondTarget = shouldHideSecondTarget();
        int i = 0;
        if (divider != null) {
            divider.setVisibility(shouldHideSecondTarget ? 8 : 0);
        }
        if (widgetFrame != null) {
            if (shouldHideSecondTarget) {
                i = 8;
            }
            widgetFrame.setVisibility(i);
        }
    }

    /* access modifiers changed from: protected */
    public boolean shouldHideSecondTarget() {
        return getSecondTargetResId() == 0;
    }

    /* access modifiers changed from: protected */
    public int getSecondTargetResId() {
        return 0;
    }
}
