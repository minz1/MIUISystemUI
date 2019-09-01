package com.android.settingslib.wifi;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Looper;
import android.preference.Preference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.settingslib.R;
import miui.preference.RadioButtonPreference;

public class AccessPointPreference extends RadioButtonPreference {
    private static final int[] FRICTION_ATTRS = {R.attr.wifi_friction};
    private static final int[] STATE_METERED = {R.attr.state_metered};
    private static final int[] STATE_SECURED = {R.attr.state_encrypted};
    private static final int[] STATE_SECURED_OWE = {R.attr.state_encrypted_owe};
    private static final int[] STATE_SECURED_SAE = {R.attr.state_encrypted_sae};
    public static final int[] WIFI_CONNECTION_STRENGTH = {R.string.accessibility_no_wifi, R.string.accessibility_wifi_one_bar, R.string.accessibility_wifi_two_bars, R.string.accessibility_wifi_three_bars, R.string.accessibility_wifi_signal_full};
    protected AccessPoint mAccessPoint;
    private final UserBadgeCache mBadgeCache;
    private final int mBadgePadding;
    private CharSequence mContentDescription;
    private int mDefaultIconResId;
    private boolean mForSavedNetworks;
    private final StateListDrawable mFrictionSld;
    private final IconInjector mIconInjector;
    private int mLevel;
    private final Runnable mNotifyChanged;
    private TextView mTitleView;
    private int mWifiSpeed;

    static class IconInjector {
        private final Context mContext;

        public IconInjector(Context context) {
            this.mContext = context;
        }
    }

    public static class UserBadgeCache {
    }

    public AccessPointPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mForSavedNetworks = false;
        this.mWifiSpeed = 0;
        this.mNotifyChanged = new Runnable() {
            public void run() {
                AccessPointPreference.this.notifyChanged();
            }
        };
        this.mFrictionSld = null;
        this.mBadgePadding = 0;
        this.mBadgeCache = null;
        this.mIconInjector = new IconInjector(context);
    }

    AccessPointPreference(AccessPoint accessPoint, Context context, UserBadgeCache cache, int iconResId, boolean forSavedNetworks, StateListDrawable frictionSld, int level, IconInjector iconInjector) {
        super(context);
        this.mForSavedNetworks = false;
        this.mWifiSpeed = 0;
        this.mNotifyChanged = new Runnable() {
            public void run() {
                AccessPointPreference.this.notifyChanged();
            }
        };
        setLayoutResource(R.layout.preference_access_point);
        setWidgetLayoutResource(getWidgetLayoutResourceId());
        this.mBadgeCache = cache;
        this.mAccessPoint = accessPoint;
        this.mForSavedNetworks = forSavedNetworks;
        this.mAccessPoint.setTag(this);
        this.mLevel = level;
        this.mDefaultIconResId = iconResId;
        this.mFrictionSld = frictionSld;
        this.mIconInjector = iconInjector;
        this.mBadgePadding = context.getResources().getDimensionPixelSize(R.dimen.wifi_preference_badge_padding);
    }

    /* access modifiers changed from: protected */
    public int getWidgetLayoutResourceId() {
        return R.layout.access_point_friction_widget;
    }

    public void onBindView(View view) {
        AccessPointPreference.super.onBindView(view);
        if (this.mAccessPoint != null) {
            Drawable drawable = getIcon();
            if (drawable != null) {
                drawable.setLevel(this.mLevel);
            }
            this.mTitleView = (TextView) view.findViewById(16908310);
            view.setContentDescription(this.mContentDescription);
            bindFrictionImage((ImageView) view.findViewById(R.id.friction_icon));
        }
    }

    private void bindFrictionImage(ImageView frictionImageView) {
        if (frictionImageView != null && this.mFrictionSld != null) {
            if (this.mAccessPoint.getSecurity() == 5) {
                this.mFrictionSld.setState(STATE_SECURED_SAE);
            } else if (this.mAccessPoint.getSecurity() == 6) {
                this.mFrictionSld.setState(STATE_SECURED_OWE);
            } else if (this.mAccessPoint.getSecurity() != 0) {
                this.mFrictionSld.setState(STATE_SECURED);
            } else if (this.mAccessPoint.isMetered()) {
                this.mFrictionSld.setState(STATE_METERED);
            }
            frictionImageView.setImageDrawable(this.mFrictionSld.getCurrent());
        }
    }

    /* access modifiers changed from: protected */
    public void notifyChanged() {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            postNotifyChanged();
        } else {
            AccessPointPreference.super.notifyChanged();
        }
    }

    static void setTitle(AccessPointPreference preference, AccessPoint ap, boolean savedNetworks) {
        if (savedNetworks) {
            preference.setTitle(ap.getConfigName());
        } else {
            preference.setTitle(ap.getSsidStr());
        }
    }

    static CharSequence buildContentDescription(Context context, Preference pref, AccessPoint ap) {
        String str;
        CharSequence contentDescription = pref.getTitle();
        CharSequence summary = pref.getSummary();
        if (!TextUtils.isEmpty(summary)) {
            contentDescription = TextUtils.concat(new CharSequence[]{contentDescription, ",", summary});
        }
        int level = ap.getLevel();
        if (level >= 0 && level < WIFI_CONNECTION_STRENGTH.length) {
            contentDescription = TextUtils.concat(new CharSequence[]{contentDescription, ",", context.getString(WIFI_CONNECTION_STRENGTH[level])});
        }
        CharSequence[] charSequenceArr = new CharSequence[3];
        charSequenceArr[0] = contentDescription;
        charSequenceArr[1] = ",";
        if (ap.getSecurity() == 0) {
            str = context.getString(R.string.accessibility_wifi_security_type_none);
        } else {
            str = context.getString(R.string.accessibility_wifi_security_type_secured);
        }
        charSequenceArr[2] = str;
        return TextUtils.concat(charSequenceArr);
    }

    private void postNotifyChanged() {
        if (this.mTitleView != null) {
            this.mTitleView.post(this.mNotifyChanged);
        }
    }
}
