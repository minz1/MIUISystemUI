package com.android.settingslib.miuisettings.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.TwoStatePreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import com.android.internal.R;
import java.lang.reflect.Method;
import miui.widget.SlidingButton;

public class SwitchPreference extends android.preference.SwitchPreference implements PreferenceApiDiff {
    private static Method PreferenceManager_getPreferenceScreen;
    private static Method TwoStatePreference_syncSummaryView;
    private PreferenceDelegate mDelegate;

    private class Listener implements CompoundButton.OnCheckedChangeListener {
        private View rootView;

        Listener(View rootView2) {
            this.rootView = rootView2;
        }

        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (this.rootView.getWindowVisibility() != 8) {
                clickPreferenceScreen(SwitchPreference.this.getPreferenceScreen(), SwitchPreference.this, this.rootView);
                if (isChecked != SwitchPreference.this.isChecked()) {
                    buttonView.setChecked(!isChecked);
                }
            }
        }

        private boolean clickPreferenceScreen(PreferenceScreen preferenceScreen, Preference preference, View view) {
            if (preferenceScreen == null) {
                return false;
            }
            ListAdapter adapter = preferenceScreen.getRootAdapter();
            for (int idx = 0; idx < adapter.getCount(); idx++) {
                Preference item = (Preference) adapter.getItem(idx);
                if (item == preference) {
                    ListView parent = (ListView) view.getParent();
                    if (parent != null) {
                        preferenceScreen.onItemClick(parent, view, idx + parent.getHeaderViewsCount(), adapter.getItemId(idx));
                        return true;
                    }
                }
                if ((item instanceof PreferenceScreen) && clickPreferenceScreen((PreferenceScreen) item, preference, view)) {
                    return true;
                }
            }
            return false;
        }
    }

    public SwitchPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 16842895);
    }

    public SwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SwitchPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CheckBoxPreference, defStyleAttr, defStyleRes);
        setSummaryOn(a.getString(0));
        setSummaryOff(a.getString(1));
        setDisableDependentsState(a.getBoolean(2, false));
        a.recycle();
        boolean showIcon = false;
        if (attrs != null) {
            showIcon = attrs.getAttributeBooleanValue("http://schemas.android.com/apk/miuisettings", "showIcon", false);
        }
        this.mDelegate = new PreferenceDelegate(this, this, showIcon);
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

    /* access modifiers changed from: private */
    public PreferenceScreen getPreferenceScreen() {
        if (PreferenceManager_getPreferenceScreen == null) {
            try {
                PreferenceManager_getPreferenceScreen = PreferenceManager.class.getDeclaredMethod("getPreferenceScreen", new Class[0]);
                PreferenceManager_getPreferenceScreen.setAccessible(true);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        try {
            return (PreferenceScreen) PreferenceManager_getPreferenceScreen.invoke(getPreferenceManager(), new Object[0]);
        } catch (Exception e2) {
            throw new RuntimeException(e2);
        }
    }

    /* access modifiers changed from: protected */
    public void onBindView(View view) {
        this.mDelegate.onBindViewStart(view);
        super.onBindView(view);
        View checkboxView = view.findViewById(16908289);
        if (checkboxView != null && (checkboxView instanceof Checkable)) {
            ((Checkable) checkboxView).setChecked(isChecked());
        }
        syncSummaryView(view);
        if (checkboxView != null && (checkboxView instanceof SlidingButton)) {
            ((SlidingButton) checkboxView).setOnPerformCheckedChangeListener(new Listener(view));
        }
        this.mDelegate.onBindViewEnd(view);
    }

    private void syncSummaryView(View view) {
        if (TwoStatePreference_syncSummaryView == null) {
            try {
                TwoStatePreference_syncSummaryView = TwoStatePreference.class.getDeclaredMethod("syncSummaryView", new Class[]{View.class});
                TwoStatePreference_syncSummaryView.setAccessible(true);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        try {
            TwoStatePreference_syncSummaryView.invoke(this, new Object[]{view});
        } catch (Exception e2) {
            throw new RuntimeException(e2);
        }
    }

    public void setSwitchTextOn(CharSequence onText) {
    }

    public void setSwitchTextOff(CharSequence offText) {
    }

    public void setSwitchTextOn(int resId) {
    }

    public void setSwitchTextOff(int resId) {
    }

    public CharSequence getSwitchTextOn() {
        return "";
    }

    public CharSequence getSwitchTextOff() {
        return "";
    }
}
