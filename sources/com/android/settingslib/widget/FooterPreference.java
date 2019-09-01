package com.android.settingslib.widget;

import android.content.Context;
import android.support.v4.content.res.TypedArrayUtils;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.settingslib.R;
import com.android.settingslib.miuisettings.preference.Preference;
import com.android.settingslib.miuisettings.preference.PreferenceViewHolder;

public class FooterPreference extends Preference {
    public FooterPreference(Context context, AttributeSet attrs) {
        super(context, attrs, TypedArrayUtils.getAttr(context, R.attr.footerPreferenceStyle, 16842894));
        init();
    }

    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        TextView title = (TextView) holder.itemView.findViewById(16908310);
        title.setMovementMethod(new LinkMovementMethod());
        title.setClickable(false);
        title.setLongClickable(false);
        ((ImageView) holder.itemView.findViewById(miui.R.id.arrow_right)).setVisibility(8);
    }

    private void init() {
        setIcon(R.drawable.ic_info_outline_24dp);
        setKey("footer_preference");
        setOrder(2147483646);
        setSelectable(false);
        setSingleLineTitle(false);
    }
}
