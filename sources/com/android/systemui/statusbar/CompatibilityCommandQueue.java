package com.android.systemui.statusbar;

import android.hardware.biometrics.IBiometricPromptReceiver;
import android.os.Bundle;
import com.android.internal.os.SomeArgs;
import com.android.internal.statusbar.IStatusBar;

public abstract class CompatibilityCommandQueue extends IStatusBar.Stub {
    public void showFingerprintDialog(SomeArgs args) {
    }

    public void showFingerprintDialog(Bundle bundle, IBiometricPromptReceiver receiver) {
        SomeArgs args = SomeArgs.obtain();
        args.arg1 = bundle;
        args.arg2 = receiver;
        showFingerprintDialog(args);
    }
}
