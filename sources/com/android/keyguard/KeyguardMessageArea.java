package com.android.keyguard;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import com.android.systemui.R;
import java.lang.ref.WeakReference;

class KeyguardMessageArea extends TextView implements SecurityMessageDisplay {
    private static final Object ANNOUNCE_TOKEN = new Object();
    private final int mDefaultColor;
    private final Handler mHandler;
    private KeyguardUpdateMonitorCallback mInfoCallback;
    private CharSequence mMessage;
    private int mNextMessageColor;

    private static class AnnounceRunnable implements Runnable {
        private final WeakReference<View> mHost;
        private final CharSequence mTextToAnnounce;

        AnnounceRunnable(View host, CharSequence textToAnnounce) {
            this.mHost = new WeakReference<>(host);
            this.mTextToAnnounce = textToAnnounce;
        }

        public void run() {
            View host = (View) this.mHost.get();
            if (host != null) {
                host.announceForAccessibility(this.mTextToAnnounce);
            }
        }
    }

    public KeyguardMessageArea(Context context) {
        this(context, null);
    }

    public KeyguardMessageArea(Context context, AttributeSet attrs) {
        this(context, attrs, KeyguardUpdateMonitor.getInstance(context));
    }

    public KeyguardMessageArea(Context context, AttributeSet attrs, KeyguardUpdateMonitor monitor) {
        super(context, attrs);
        this.mNextMessageColor = -1;
        this.mInfoCallback = new KeyguardUpdateMonitorCallback() {
            public void onFinishedGoingToSleep(int why) {
                KeyguardMessageArea.this.setSelected(false);
            }

            public void onStartedWakingUp() {
                KeyguardMessageArea.this.setSelected(true);
            }
        };
        setLayerType(2, null);
        monitor.registerCallback(this.mInfoCallback);
        this.mHandler = new Handler(Looper.myLooper());
        this.mDefaultColor = getCurrentTextColor();
        update();
    }

    public void setMessage(CharSequence msg) {
        if (!TextUtils.isEmpty(msg)) {
            securityMessageChanged(msg);
        } else {
            clearMessage();
        }
    }

    public void setMessage(int resId) {
        CharSequence message = null;
        if (resId != 0) {
            message = getContext().getResources().getText(resId);
        }
        setMessage(message);
    }

    public static SecurityMessageDisplay findSecurityMessageDisplay(View v) {
        return (KeyguardMessageArea) v.findViewById(R.id.keyguard_message_area);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        setSelected(KeyguardUpdateMonitor.getInstance(this.mContext).isDeviceInteractive());
    }

    private void securityMessageChanged(CharSequence message) {
        this.mMessage = message;
        update();
        this.mHandler.removeCallbacksAndMessages(ANNOUNCE_TOKEN);
        this.mHandler.postAtTime(new AnnounceRunnable(this, getText()), ANNOUNCE_TOKEN, SystemClock.uptimeMillis() + 250);
    }

    private void clearMessage() {
        this.mMessage = null;
        update();
    }

    private void update() {
        CharSequence status = this.mMessage;
        setVisibility(TextUtils.isEmpty(status) ? 4 : 0);
        setText(status);
        int color = this.mDefaultColor;
        if (this.mNextMessageColor != -1) {
            color = this.mNextMessageColor;
            this.mNextMessageColor = -1;
        }
        setTextColor(color);
    }
}
