package com.android.keyguard;

import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import java.util.ArrayList;
import java.util.List;

public class FingerprintHelper {
    private static List<Object> sFingerprintIdentifyCallbackList = new ArrayList();
    private Context mContext;
    private FingerprintManager mFingerprintMgr = null;

    public FingerprintHelper(Context context) {
        this.mContext = context;
    }

    public boolean isHardwareDetected() {
        initFingerprintManager();
        if (this.mFingerprintMgr == null) {
            return false;
        }
        return this.mFingerprintMgr.isHardwareDetected();
    }

    private void initFingerprintManager() {
        if (this.mFingerprintMgr == null) {
            this.mFingerprintMgr = (FingerprintManager) this.mContext.getSystemService("fingerprint");
        }
    }

    public void resetFingerLockoutTime() {
        initFingerprintManager();
        if (this.mFingerprintMgr != null) {
            this.mFingerprintMgr.resetTimeout(null);
        }
    }
}
