package com.android.settingslib.miuisettings.preference;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;

public class ListPreference extends android.preference.ListPreference implements PreferenceApiDiff {
    private PreferenceDelegate mDelegate;

    private static class SavedState extends Preference.BaseSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        CharSequence[] mEntries;
        CharSequence[] mEntryValues;

        public SavedState(Parcel source) {
            super(source);
            this.mEntries = source.readCharSequenceArray();
            this.mEntryValues = source.readCharSequenceArray();
        }

        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeCharSequenceArray(this.mEntries);
            dest.writeCharSequenceArray(this.mEntryValues);
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }
    }

    public ListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public ListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        this.mDelegate = new PreferenceDelegate(this, this);
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
    }

    /* access modifiers changed from: protected */
    public void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        onPrepareDialogBuilder(builder, this);
    }

    /* access modifiers changed from: protected */
    public void onPrepareDialogBuilder(AlertDialog.Builder builder, DialogInterface.OnClickListener listener) {
    }

    /* access modifiers changed from: protected */
    public Parcelable onSaveInstanceState() {
        SavedState myState = new SavedState(super.onSaveInstanceState());
        myState.mEntries = getEntries();
        myState.mEntryValues = getEntryValues();
        return myState;
    }

    /* access modifiers changed from: protected */
    public void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState myState = (SavedState) state;
        if (getEntries() == null || getEntryValues() == null) {
            setEntries(myState.mEntries);
            setEntryValues(myState.mEntryValues);
        }
        super.onRestoreInstanceState(myState.getSuperState());
    }
}
