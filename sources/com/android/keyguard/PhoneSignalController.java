package com.android.keyguard;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.util.Slog;
import com.android.keyguard.utils.PhoneUtils;
import com.google.android.collect.Lists;
import java.util.ArrayList;
import java.util.Iterator;
import miui.telephony.TelephonyManager;

public class PhoneSignalController {
    private Context mContext;
    private int mPhoneCount;
    private boolean[] mPhoneSignalAvailable;
    private int[] mPhoneSignalLevel;
    private ArrayList<PhoneStateListener> mPhoneStateListeners = new ArrayList<>(4);
    private ArrayList<PhoneSignalChangeCallback> mSignalChangeCallbacks = Lists.newArrayList();

    interface PhoneSignalChangeCallback {
        void onSignalChange(boolean z);
    }

    public PhoneSignalController(Context context) {
        this.mContext = context;
    }

    private PhoneStateListener getPhoneStateListener(final int slotId) {
        PhoneStateListener phoneStateListener = new PhoneStateListener(Integer.valueOf(slotId)) {
            public void onSignalStrengthsChanged(SignalStrength signal) {
                PhoneSignalController.this.handleSignalStrengthsChanged(signal, slotId);
            }

            public void onServiceStateChanged(ServiceState serviceState) {
                PhoneSignalController.this.handleServiceStateChanged(serviceState, slotId);
            }
        };
        this.mPhoneStateListeners.add(slotId, phoneStateListener);
        return phoneStateListener;
    }

    /* access modifiers changed from: private */
    public void handleSignalStrengthsChanged(SignalStrength signal, int slotId) {
        if (signal.getLevel() < 1 || signal.getLevel() > 5) {
            this.mPhoneSignalAvailable[slotId] = false;
        } else {
            this.mPhoneSignalAvailable[slotId] = true;
        }
        this.mPhoneSignalLevel[slotId] = signal.getLevel();
        Slog.d("PhoneSignalController", "level=" + signal.getLevel() + " " + this.mSignalChangeCallbacks.size());
        Iterator<PhoneSignalChangeCallback> it = this.mSignalChangeCallbacks.iterator();
        while (it.hasNext()) {
            notifyPhoneSignalChangeCallback(it.next());
        }
    }

    /* access modifiers changed from: private */
    public void handleServiceStateChanged(ServiceState serviceState, int slotId) {
        if (this.mPhoneSignalLevel[slotId] >= 1 && this.mPhoneSignalLevel[slotId] <= 5) {
            return;
        }
        if (serviceState == null) {
            this.mPhoneSignalAvailable[slotId] = false;
            return;
        }
        if (serviceState.getVoiceRegState() == 0 || serviceState.getVoiceRegState() == 2 || serviceState.isEmergencyOnly()) {
            this.mPhoneSignalAvailable[slotId] = true;
        } else {
            this.mPhoneSignalAvailable[slotId] = false;
        }
        Slog.d("PhoneSignalController", "level=" + this.mPhoneSignalLevel[slotId] + ";servicestate=" + serviceState + ";mPhoneSignalAvailable=" + this.mPhoneSignalAvailable[slotId]);
        Iterator<PhoneSignalChangeCallback> it = this.mSignalChangeCallbacks.iterator();
        while (it.hasNext()) {
            notifyPhoneSignalChangeCallback(it.next());
        }
    }

    private void addPhoneStateListener() {
        this.mPhoneCount = PhoneUtils.getPhoneCount();
        this.mPhoneSignalAvailable = new boolean[this.mPhoneCount];
        this.mPhoneSignalLevel = new int[this.mPhoneCount];
        this.mPhoneStateListeners = new ArrayList<>(this.mPhoneCount);
        for (int i = 0; i < this.mPhoneCount; i++) {
            this.mPhoneSignalAvailable[i] = false;
            this.mPhoneSignalLevel[i] = 0;
            TelephonyManager.getDefault().listenForSlot(i, getPhoneStateListener(i), 257);
        }
    }

    private void removePhoneStateListener() {
        for (int i = 0; i < this.mPhoneCount; i++) {
            this.mPhoneSignalAvailable[i] = false;
            this.mPhoneSignalLevel[i] = 0;
            TelephonyManager.getDefault().listenForSlot(i, this.mPhoneStateListeners.get(i), 0);
        }
    }

    public void registerPhoneSignalChangeCallback(PhoneSignalChangeCallback callback) {
        if (this.mSignalChangeCallbacks.isEmpty()) {
            addPhoneStateListener();
        }
        if (!this.mSignalChangeCallbacks.contains(callback)) {
            this.mSignalChangeCallbacks.add(callback);
            notifyPhoneSignalChangeCallback(callback);
        }
    }

    private void notifyPhoneSignalChangeCallback(PhoneSignalChangeCallback callback) {
        boolean signalAvailable = false;
        for (boolean z : this.mPhoneSignalAvailable) {
            signalAvailable |= z;
        }
        Slog.d("PhoneSignalController", "signalAvailable=" + signalAvailable);
        callback.onSignalChange(signalAvailable);
    }

    public void removePhoneSignalChangeCallback(PhoneSignalChangeCallback callback) {
        this.mSignalChangeCallbacks.remove(callback);
        if (this.mSignalChangeCallbacks.isEmpty()) {
            removePhoneStateListener();
        }
    }
}
