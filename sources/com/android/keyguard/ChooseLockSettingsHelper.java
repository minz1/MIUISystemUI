package com.android.keyguard;

import android.content.Context;
import android.security.MiuiLockPatternUtils;

public final class ChooseLockSettingsHelper {
    private Context mContext;
    private final MiuiLockPatternUtils mLockPatternUtils = new MiuiLockPatternUtils(this.mContext);
    private final LockPatternUtilsWrapper mLockPatternUtilsWrapper = new LockPatternUtilsWrapper(this.mLockPatternUtils);

    public ChooseLockSettingsHelper(Context context) {
        this.mContext = context;
    }
}
