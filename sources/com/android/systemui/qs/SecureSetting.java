package com.android.systemui.qs;

import android.app.ActivityManager;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;

public abstract class SecureSetting extends ContentObserver {
    private final Context mContext;
    private boolean mListening;
    private int mObservedValue = 0;
    private final String mSettingName;
    private int mUserId;

    /* access modifiers changed from: protected */
    public abstract void handleValueChanged(int i, boolean z);

    public SecureSetting(Context context, Handler handler, String settingName) {
        super(handler);
        this.mContext = context;
        this.mSettingName = settingName;
        this.mUserId = ActivityManager.getCurrentUser();
    }

    public int getValue() {
        return Settings.Secure.getIntForUser(this.mContext.getContentResolver(), this.mSettingName, 0, this.mUserId);
    }

    public void setListening(boolean listening) {
        if (listening != this.mListening) {
            this.mListening = listening;
            if (listening) {
                this.mObservedValue = getValue();
                this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(this.mSettingName), false, this, this.mUserId);
            } else {
                this.mContext.getContentResolver().unregisterContentObserver(this);
                this.mObservedValue = 0;
            }
        }
    }

    public void onChange(boolean selfChange) {
        int value = getValue();
        handleValueChanged(value, value != this.mObservedValue);
        this.mObservedValue = value;
    }

    public void setUserId(int userId) {
        this.mUserId = userId;
        if (this.mListening) {
            setListening(false);
            setListening(true);
        }
    }
}
