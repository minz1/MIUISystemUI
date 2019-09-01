package com.android.settingslib;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import com.android.settingslib.miuisettings.preference.DialogPreference;
import com.android.settingslib.miuisettings.preference.PreferenceDialogFragment;

public class CustomDialogPreference extends DialogPreference {
    private CustomPreferenceDialogFragment mFragment;
    private DialogInterface.OnShowListener mOnShowListener;

    public static class CustomPreferenceDialogFragment extends PreferenceDialogFragment {
        private CustomDialogPreference getCustomizablePreference() {
            return (CustomDialogPreference) getPreference();
        }

        /* access modifiers changed from: protected */
        public void onPrepareDialogBuilder(AlertDialog.Builder builder) {
            super.onPrepareDialogBuilder(builder);
            getCustomizablePreference().setFragment(this);
            getCustomizablePreference().onPrepareDialogBuilder(builder, this);
        }

        public void onDialogClosed(boolean positiveResult) {
            getCustomizablePreference().onDialogClosed(positiveResult);
        }

        /* access modifiers changed from: protected */
        public void onBindDialogView(View view) {
            super.onBindDialogView(view);
            getCustomizablePreference().onBindDialogView(view);
        }

        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Dialog dialog = super.onCreateDialog(savedInstanceState);
            dialog.setOnShowListener(getCustomizablePreference().getOnShowListener());
            return dialog;
        }

        public void onClick(DialogInterface dialog, int which) {
            super.onClick(dialog, which);
            getCustomizablePreference().onClick(dialog, which);
        }
    }

    public CustomDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Dialog getDialog() {
        if (this.mFragment != null) {
            return this.mFragment.getDialog();
        }
        return null;
    }

    /* access modifiers changed from: protected */
    public void onPrepareDialogBuilder(AlertDialog.Builder builder, DialogInterface.OnClickListener listener) {
    }

    /* access modifiers changed from: protected */
    public void onDialogClosed(boolean positiveResult) {
    }

    public void onClick(DialogInterface dialog, int which) {
    }

    /* access modifiers changed from: protected */
    public void onBindDialogView(View view) {
    }

    /* access modifiers changed from: private */
    public void setFragment(CustomPreferenceDialogFragment fragment) {
        this.mFragment = fragment;
    }

    /* access modifiers changed from: private */
    public DialogInterface.OnShowListener getOnShowListener() {
        return this.mOnShowListener;
    }
}
