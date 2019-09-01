package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.os.Trace;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import java.util.ArrayList;
import java.util.Iterator;

public class UnlockMethodCache {
    private static UnlockMethodCache sInstance;
    private final KeyguardUpdateMonitorCallback mCallback = new KeyguardUpdateMonitorCallback() {
        public void onUserSwitchComplete(int userId) {
            UnlockMethodCache.this.update(false);
        }

        public void onStartedWakingUp() {
            UnlockMethodCache.this.update(false);
        }

        public void onFingerprintAuthenticated(int userId) {
            Trace.beginSection("KeyguardUpdateMonitorCallback#onFingerprintAuthenticated");
            if (!UnlockMethodCache.this.mKeyguardUpdateMonitor.isUnlockingWithFingerprintAllowed()) {
                Trace.endSection();
                return;
            }
            UnlockMethodCache.this.update(false);
            Trace.endSection();
        }

        public void onFaceUnlockStateChanged(boolean running, int userId) {
            UnlockMethodCache.this.update(false);
        }

        public void onStrongAuthStateChanged(int userId) {
            UnlockMethodCache.this.update(false);
        }

        public void onFaceAuthenticated() {
            Trace.beginSection("KeyguardUpdateMonitorCallback#onFaceAuthenticated");
            if (!UnlockMethodCache.this.mKeyguardUpdateMonitor.isUnlockingWithFingerprintAllowed()) {
                Trace.endSection();
                return;
            }
            UnlockMethodCache.this.update(false);
            Trace.endSection();
        }
    };
    private boolean mCanSkipBouncer;
    private boolean mFaceUnlockRunning;
    /* access modifiers changed from: private */
    public final KeyguardUpdateMonitor mKeyguardUpdateMonitor;
    private final ArrayList<OnUnlockMethodChangedListener> mListeners = new ArrayList<>();
    private final LockPatternUtils mLockPatternUtils;
    private boolean mSecure;
    private boolean mTrustManaged;
    private boolean mTrusted;

    public interface OnUnlockMethodChangedListener {
        void onUnlockMethodStateChanged();
    }

    private UnlockMethodCache(Context ctx) {
        this.mLockPatternUtils = new LockPatternUtils(ctx);
        this.mKeyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(ctx);
        KeyguardUpdateMonitor.getInstance(ctx).registerCallback(this.mCallback);
        update(true);
    }

    public static UnlockMethodCache getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new UnlockMethodCache(context);
        }
        return sInstance;
    }

    public boolean isMethodSecure() {
        return this.mSecure;
    }

    public boolean canSkipBouncer() {
        return this.mCanSkipBouncer;
    }

    public void addListener(OnUnlockMethodChangedListener listener) {
        this.mListeners.add(listener);
    }

    /* access modifiers changed from: private */
    public void update(boolean updateAlways) {
        Trace.beginSection("UnlockMethodCache#update");
        int user = KeyguardUpdateMonitor.getCurrentUser();
        boolean secure = this.mLockPatternUtils.isSecure(user);
        boolean changed = true;
        boolean canSkipBouncer = !secure || this.mKeyguardUpdateMonitor.getUserCanSkipBouncer(user);
        boolean trustManaged = this.mKeyguardUpdateMonitor.getUserTrustIsManaged(user);
        boolean trusted = this.mKeyguardUpdateMonitor.getUserHasTrust(user);
        boolean faceUnlockRunning = this.mKeyguardUpdateMonitor.isFaceUnlockRunning(user) && trustManaged;
        if (secure == this.mSecure && canSkipBouncer == this.mCanSkipBouncer && trustManaged == this.mTrustManaged && faceUnlockRunning == this.mFaceUnlockRunning) {
            changed = false;
        }
        if (changed || updateAlways) {
            this.mSecure = secure;
            this.mCanSkipBouncer = canSkipBouncer;
            this.mTrusted = trusted;
            this.mTrustManaged = trustManaged;
            this.mFaceUnlockRunning = faceUnlockRunning;
            notifyListeners();
        }
        Trace.endSection();
    }

    private void notifyListeners() {
        Iterator<OnUnlockMethodChangedListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onUnlockMethodStateChanged();
        }
    }
}
