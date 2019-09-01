package com.android.settingslib;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import com.android.settingslib.miuisettings.preference.EditTextPreference;
import com.android.settingslib.miuisettings.preference.EditTextPreferenceDialogFragment;

public class CustomEditTextPreference extends EditTextPreference {
    private CustomPreferenceDialogFragment mFragment;

    public static class CustomPreferenceDialogFragment extends EditTextPreferenceDialogFragment {
        private CustomEditTextPreference getCustomizablePreference() {
            return (CustomEditTextPreference) getPreference();
        }

        /* access modifiers changed from: protected */
        public void onBindDialogView(View view) {
            super.onBindDialogView(view);
            getCustomizablePreference().onBindDialogView(view);
        }

        /* access modifiers changed from: protected */
        public void onPrepareDialogBuilder(AlertDialog.Builder builder) {
            super.onPrepareDialogBuilder(builder);
            getCustomizablePreference().setFragment(this);
            getCustomizablePreference().onPrepareDialogBuilder(builder, this);
        }

        public void onDialogClosed(boolean positiveResult) {
            super.onDialogClosed(positiveResult);
            getCustomizablePreference().onDialogClosed(positiveResult);
        }

        public void onClick(DialogInterface dialog, int which) {
            super.onClick(dialog, which);
            getCustomizablePreference().onClick(dialog, which);
        }
    }

    public CustomEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EditText getEditText() {
        if (this.mFragment != null) {
            Dialog dialog = this.mFragment.getDialog();
            if (dialog != null) {
                return (EditText) dialog.findViewById(16908291);
            }
        }
        return null;
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
        EditText editText = (EditText) view.findViewById(16908291);
        if (editText != null) {
            editText.setInputType(16385);
            editText.requestFocus();
        }
    }

    /* access modifiers changed from: private */
    public void setFragment(CustomPreferenceDialogFragment fragment) {
        this.mFragment = fragment;
    }
}
