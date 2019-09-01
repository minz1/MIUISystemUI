package com.android.settingslib.miuisettings.preference;

import android.view.View;

public class PreferenceViewHolder {
    public View itemView;

    public PreferenceViewHolder(View itemView2) {
        this.itemView = itemView2;
    }

    public View findViewById(int id) {
        return this.itemView.findViewById(id);
    }
}
