package com.android.keyguard;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import java.util.TimeZone;

public class KeyguardClockContainer extends FrameLayout {
    ContentObserver mClockPositionObserver;
    /* access modifiers changed from: private */
    public KeyguardClockView mClockView;
    /* access modifiers changed from: private */
    public String mCurrentTimezone;
    /* access modifiers changed from: private */
    public boolean mDualClockOpen;
    ContentObserver mDualClockOpenObserver;
    /* access modifiers changed from: private */
    public final Handler mHandler;
    private final BroadcastReceiver mIntentReceiver;
    private int mLastSelectedClockPosition;
    /* access modifiers changed from: private */
    public String mResidentTimezone;
    ContentObserver mResidentTimezoneObserver;
    /* access modifiers changed from: private */
    public int mSelectedClockPosition;
    private boolean mShowDualClock;
    /* access modifiers changed from: private */
    public boolean mShowVerticalClock;
    /* access modifiers changed from: private */
    public KeyguardUpdateMonitor mUpdateMonitor;
    /* access modifiers changed from: private */
    public Runnable mUpdateTimeRunnable;

    public interface KeyguardClockView {
        int getClockHeight();

        float getClockVisibleHeight();

        float getTopMargin();

        void setClockAlpha(float f);

        void setDarkMode(boolean z);

        void setSelectedClockPosition(int i);

        void updateClockView(boolean z, boolean z2);

        void updateLockScreenMagazineInfo();

        void updateResidentTimeZone(String str);

        void updateTime();

        void updateTimeAndBatteryInfo();

        void updateTimeZone(String str);
    }

    public KeyguardClockContainer(Context context) {
        this(context, null, 0, 0);
    }

    public KeyguardClockContainer(Context context, AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }

    public KeyguardClockContainer(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public KeyguardClockContainer(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mCurrentTimezone = TimeZone.getDefault().getID();
        boolean z = false;
        this.mDualClockOpen = false;
        this.mShowDualClock = false;
        this.mSelectedClockPosition = 0;
        this.mLastSelectedClockPosition = 0;
        this.mShowVerticalClock = false;
        this.mHandler = new Handler();
        this.mUpdateTimeRunnable = new Runnable() {
            public void run() {
                if (KeyguardClockContainer.this.mClockView != null) {
                    KeyguardClockContainer.this.mClockView.updateTime();
                }
            }
        };
        this.mIntentReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("android.intent.action.TIMEZONE_CHANGED")) {
                    String unused = KeyguardClockContainer.this.mCurrentTimezone = intent.getStringExtra("time-zone");
                    KeyguardClockContainer.this.mHandler.post(new Runnable() {
                        public void run() {
                            KeyguardClockContainer.this.updateKeyguardClock();
                        }
                    });
                    return;
                }
                KeyguardClockContainer.this.mHandler.post(KeyguardClockContainer.this.mUpdateTimeRunnable);
            }
        };
        this.mDualClockOpenObserver = new ContentObserver(new Handler()) {
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                KeyguardClockContainer keyguardClockContainer = KeyguardClockContainer.this;
                ContentResolver contentResolver = KeyguardClockContainer.this.mContext.getContentResolver();
                KeyguardUpdateMonitor unused = KeyguardClockContainer.this.mUpdateMonitor;
                boolean z = false;
                if (Settings.System.getIntForUser(contentResolver, "auto_dual_clock", 0, KeyguardUpdateMonitor.getCurrentUser()) != 0) {
                    z = true;
                }
                boolean unused2 = keyguardClockContainer.mDualClockOpen = z;
                KeyguardClockContainer.this.updateKeyguardClock();
            }
        };
        this.mResidentTimezoneObserver = new ContentObserver(new Handler()) {
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                KeyguardClockContainer keyguardClockContainer = KeyguardClockContainer.this;
                ContentResolver contentResolver = KeyguardClockContainer.this.mContext.getContentResolver();
                KeyguardUpdateMonitor unused = KeyguardClockContainer.this.mUpdateMonitor;
                String unused2 = keyguardClockContainer.mResidentTimezone = Settings.System.getStringForUser(contentResolver, "resident_timezone", KeyguardUpdateMonitor.getCurrentUser());
                KeyguardClockContainer.this.updateKeyguardClock();
            }
        };
        this.mClockPositionObserver = new ContentObserver(new Handler()) {
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                KeyguardClockContainer keyguardClockContainer = KeyguardClockContainer.this;
                ContentResolver contentResolver = KeyguardClockContainer.this.mContext.getContentResolver();
                KeyguardUpdateMonitor unused = KeyguardClockContainer.this.mUpdateMonitor;
                int unused2 = keyguardClockContainer.mSelectedClockPosition = Settings.System.getIntForUser(contentResolver, "selected_keyguard_clock_position", 0, KeyguardUpdateMonitor.getCurrentUser());
                boolean unused3 = KeyguardClockContainer.this.mShowVerticalClock = MiuiKeyguardUtils.isSupportVerticalClock(KeyguardClockContainer.this.mSelectedClockPosition, KeyguardClockContainer.this.mContext);
                KeyguardClockContainer.this.updateKeyguardClock();
            }
        };
        this.mUpdateMonitor = KeyguardUpdateMonitor.getInstance(context);
        ContentResolver contentResolver = this.mContext.getContentResolver();
        KeyguardUpdateMonitor keyguardUpdateMonitor = this.mUpdateMonitor;
        this.mSelectedClockPosition = Settings.System.getIntForUser(contentResolver, "selected_keyguard_clock_position", 0, KeyguardUpdateMonitor.getCurrentUser());
        ContentResolver contentResolver2 = this.mContext.getContentResolver();
        KeyguardUpdateMonitor keyguardUpdateMonitor2 = this.mUpdateMonitor;
        this.mDualClockOpen = Settings.System.getIntForUser(contentResolver2, "auto_dual_clock", 0, KeyguardUpdateMonitor.getCurrentUser()) != 0;
        ContentResolver contentResolver3 = this.mContext.getContentResolver();
        KeyguardUpdateMonitor keyguardUpdateMonitor3 = this.mUpdateMonitor;
        this.mResidentTimezone = Settings.System.getStringForUser(contentResolver3, "resident_timezone", KeyguardUpdateMonitor.getCurrentUser());
        if (this.mDualClockOpen && this.mResidentTimezone != null && !this.mResidentTimezone.equals(this.mCurrentTimezone)) {
            z = true;
        }
        this.mShowDualClock = z;
        this.mShowVerticalClock = MiuiKeyguardUtils.isSupportVerticalClock(this.mSelectedClockPosition, this.mContext);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        addClockView();
        updateKeyguardClock();
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.TIME_TICK");
        filter.addAction("android.intent.action.TIME_SET");
        filter.addAction("android.intent.action.TIMEZONE_CHANGED");
        this.mContext.registerReceiverAsUser(this.mIntentReceiver, UserHandle.ALL, filter, null, (Handler) Dependency.get(Dependency.TIME_TICK_HANDLER));
        registerDualClockObserver();
        registerClockPositionObserver();
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mContext.unregisterReceiver(this.mIntentReceiver);
        unregisterDualClockObserver();
        unregisterClockPositionObserver();
    }

    private void registerDualClockObserver() {
        if (MiuiKeyguardUtils.supportDualClock()) {
            this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("auto_dual_clock"), false, this.mDualClockOpenObserver, -1);
            this.mDualClockOpenObserver.onChange(false);
            this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("resident_timezone"), false, this.mResidentTimezoneObserver, -1);
            this.mResidentTimezoneObserver.onChange(false);
        }
    }

    private void unregisterDualClockObserver() {
        if (MiuiKeyguardUtils.supportDualClock()) {
            this.mContext.getContentResolver().unregisterContentObserver(this.mDualClockOpenObserver);
            this.mContext.getContentResolver().unregisterContentObserver(this.mResidentTimezoneObserver);
        }
    }

    private void registerClockPositionObserver() {
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("selected_keyguard_clock_position"), false, this.mClockPositionObserver, -1);
        this.mClockPositionObserver.onChange(false);
    }

    private void unregisterClockPositionObserver() {
        this.mContext.getContentResolver().unregisterContentObserver(this.mClockPositionObserver);
    }

    private void addClockView() {
        View view;
        LayoutInflater inflater = LayoutInflater.from(this.mContext);
        if (this.mShowDualClock) {
            view = inflater.inflate(R.layout.miui_keyguard_dual_clock, this, false);
        } else if (this.mSelectedClockPosition == 2) {
            view = inflater.inflate(R.layout.miui_keyguard_left_top_clock, this, false);
        } else {
            view = inflater.inflate(R.layout.miui_keyguard_vertical_clock, this, false);
        }
        addView(view);
        this.mClockView = (KeyguardClockView) view;
    }

    public void updateKeyguardClock() {
        boolean showDualClock = this.mDualClockOpen && this.mResidentTimezone != null && !this.mResidentTimezone.equals(this.mCurrentTimezone);
        if (!(this.mShowDualClock == showDualClock && this.mSelectedClockPosition == this.mLastSelectedClockPosition)) {
            this.mShowDualClock = showDualClock;
            this.mLastSelectedClockPosition = this.mSelectedClockPosition;
            removeAllViews();
            addClockView();
        }
        if (this.mClockView != null) {
            this.mClockView.updateResidentTimeZone(this.mResidentTimezone);
            this.mClockView.updateTimeZone(this.mCurrentTimezone);
            this.mClockView.setSelectedClockPosition(this.mSelectedClockPosition);
        }
    }

    public void setDarkMode(boolean isLightClock) {
        this.mClockView.setDarkMode(isLightClock);
    }

    public void updateTimeAndBatteryInfo() {
        this.mHandler.removeCallbacks(this.mUpdateTimeRunnable);
        this.mClockView.updateTimeAndBatteryInfo();
        this.mClockView.updateTime();
    }

    public void updateClockView(boolean hasNotifiction, boolean isUnderKeyguard) {
        this.mClockView.updateClockView(hasNotifiction, isUnderKeyguard);
    }

    public int getClockHeight() {
        return this.mClockView.getClockHeight();
    }

    public float getClockVisibleHeight() {
        return this.mClockView.getClockVisibleHeight();
    }

    public float getTopMargin() {
        return this.mClockView.getTopMargin();
    }

    public void onUserChanged() {
        if (MiuiKeyguardUtils.supportDualClock()) {
            this.mDualClockOpenObserver.onChange(false);
            this.mResidentTimezoneObserver.onChange(false);
        }
        this.mClockPositionObserver.onChange(false);
    }

    public void setClockAlpha(float alpha) {
        this.mClockView.setClockAlpha(alpha);
    }

    public void updateLockScreenMagazineInfo() {
        this.mClockView.updateLockScreenMagazineInfo();
    }
}
