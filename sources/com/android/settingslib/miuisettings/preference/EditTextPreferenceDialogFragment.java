package com.android.settingslib.miuisettings.preference;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class EditTextPreferenceDialogFragment extends PreferenceDialogFragment {
    private EditText mEditText;
    private CharSequence mText;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            this.mText = getEditTextPreference().getText();
        } else {
            this.mText = savedInstanceState.getCharSequence("EditTextPreferenceDialogFragment.text");
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putCharSequence("EditTextPreferenceDialogFragment.text", this.mText);
    }

    /* access modifiers changed from: protected */
    public void onBindDialogView(View view) {
        super.onBindDialogView(view);
        this.mEditText = (EditText) view.findViewById(16908291);
        if (this.mEditText != null) {
            this.mEditText.setText(this.mText);
            return;
        }
        throw new IllegalStateException("Dialog view must contain an EditText with id @android:id/edit");
    }

    private EditTextPreference getEditTextPreference() {
        return (EditTextPreference) getPreference();
    }

    /* access modifiers changed from: protected */
    public boolean needInputMethod() {
        return true;
    }

    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            String value = this.mEditText.getText().toString();
            if (PreferenceUtils.call_Preference_callChangeListener(getEditTextPreference(), value)) {
                getEditTextPreference().setText(value);
            }
        }
    }
}
