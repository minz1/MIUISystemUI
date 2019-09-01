package com.android.settingslib.miuisettings.preference;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.Preference;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.android.settingslib.miuisettings.preference.DialogPreference;

public abstract class PreferenceDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {
    private BitmapDrawable mDialogIcon;
    private int mDialogLayoutRes;
    private CharSequence mDialogMessage;
    private CharSequence mDialogTitle;
    private CharSequence mNegativeButtonText;
    private CharSequence mPositiveButtonText;
    private DialogPreference mPreference;
    private int mWhichButtonClicked;

    public abstract void onDialogClosed(boolean z);

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fragment rawFragment = getTargetFragment();
        if (rawFragment instanceof DialogPreference.TargetFragment) {
            DialogPreference.TargetFragment fragment = (DialogPreference.TargetFragment) rawFragment;
            String key = getArguments().getString("key");
            if (savedInstanceState == null) {
                this.mPreference = (DialogPreference) fragment.findPreference(key);
                this.mDialogTitle = this.mPreference.getDialogTitle();
                this.mPositiveButtonText = this.mPreference.getPositiveButtonText();
                this.mNegativeButtonText = this.mPreference.getNegativeButtonText();
                this.mDialogMessage = this.mPreference.getDialogMessage();
                this.mDialogLayoutRes = this.mPreference.getDialogLayoutResource();
                Drawable icon = this.mPreference.getDialogIcon();
                if (icon == null || (icon instanceof BitmapDrawable)) {
                    this.mDialogIcon = (BitmapDrawable) icon;
                    return;
                }
                Bitmap bitmap = Bitmap.createBitmap(icon.getIntrinsicWidth(), icon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                icon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                icon.draw(canvas);
                this.mDialogIcon = new BitmapDrawable(getResources(), bitmap);
                return;
            }
            this.mDialogTitle = savedInstanceState.getCharSequence("PreferenceDialogFragment.title");
            this.mPositiveButtonText = savedInstanceState.getCharSequence("PreferenceDialogFragment.positiveText");
            this.mNegativeButtonText = savedInstanceState.getCharSequence("PreferenceDialogFragment.negativeText");
            this.mDialogMessage = savedInstanceState.getCharSequence("PreferenceDialogFragment.message");
            this.mDialogLayoutRes = savedInstanceState.getInt("PreferenceDialogFragment.layout", 0);
            Bitmap bitmap2 = (Bitmap) savedInstanceState.getParcelable("PreferenceDialogFragment.icon");
            if (bitmap2 != null) {
                this.mDialogIcon = new BitmapDrawable(getResources(), bitmap2);
                return;
            }
            return;
        }
        throw new IllegalStateException("Target fragment must implement TargetFragment interface");
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putCharSequence("PreferenceDialogFragment.title", this.mDialogTitle);
        outState.putCharSequence("PreferenceDialogFragment.positiveText", this.mPositiveButtonText);
        outState.putCharSequence("PreferenceDialogFragment.negativeText", this.mNegativeButtonText);
        outState.putCharSequence("PreferenceDialogFragment.message", this.mDialogMessage);
        outState.putInt("PreferenceDialogFragment.layout", this.mDialogLayoutRes);
        if (this.mDialogIcon != null) {
            outState.putParcelable("PreferenceDialogFragment.icon", this.mDialogIcon.getBitmap());
        }
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = getActivity();
        this.mWhichButtonClicked = -2;
        AlertDialog.Builder builder = new AlertDialog.Builder(context).setTitle(this.mDialogTitle).setIcon(this.mDialogIcon).setPositiveButton(this.mPositiveButtonText, this).setNegativeButton(this.mNegativeButtonText, this);
        View contentView = onCreateDialogView(context);
        if (contentView != null) {
            onBindDialogView(contentView);
            builder.setView(contentView);
        } else {
            builder.setMessage(this.mDialogMessage);
        }
        onPrepareDialogBuilder(builder);
        Dialog dialog = builder.create();
        if (needInputMethod()) {
            requestInputMethod(dialog);
        }
        return dialog;
    }

    public Preference getPreference() {
        if (this.mPreference == null) {
            this.mPreference = (DialogPreference) ((DialogPreference.TargetFragment) getTargetFragment()).findPreference(getArguments().getString("key"));
        }
        return this.mPreference;
    }

    /* access modifiers changed from: protected */
    public void onPrepareDialogBuilder(AlertDialog.Builder builder) {
    }

    /* access modifiers changed from: protected */
    public boolean needInputMethod() {
        return false;
    }

    private void requestInputMethod(Dialog dialog) {
        dialog.getWindow().setSoftInputMode(5);
    }

    /* access modifiers changed from: protected */
    public View onCreateDialogView(Context context) {
        int resId = this.mDialogLayoutRes;
        if (resId == 0) {
            return null;
        }
        return LayoutInflater.from(context).inflate(resId, null);
    }

    /* access modifiers changed from: protected */
    public void onBindDialogView(View view) {
        View dialogMessageView = view.findViewById(16908299);
        if (dialogMessageView != null) {
            CharSequence message = this.mDialogMessage;
            int newVisibility = 8;
            if (!TextUtils.isEmpty(message)) {
                if (dialogMessageView instanceof TextView) {
                    ((TextView) dialogMessageView).setText(message);
                }
                newVisibility = 0;
            }
            if (dialogMessageView.getVisibility() != newVisibility) {
                dialogMessageView.setVisibility(newVisibility);
            }
        }
    }

    public void onClick(DialogInterface dialog, int which) {
        this.mWhichButtonClicked = which;
    }

    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        onDialogClosed(this.mWhichButtonClicked == -1);
    }
}
