package com.android.settingslib.bluetooth;

import android.content.Context;
import com.android.settingslib.R;

public class Utils {
    private static ErrorListener sErrorListener;

    public interface ErrorListener {
        void onShowError(Context context, String str, int i);
    }

    public static int getConnectionStateSummary(int connectionState) {
        switch (connectionState) {
            case 0:
                return R.string.bluetooth_disconnected;
            case 1:
                return R.string.bluetooth_connecting;
            case 2:
                return R.string.bluetooth_connected;
            case 3:
                return R.string.bluetooth_disconnecting;
            default:
                return 0;
        }
    }

    static void showError(Context context, String name, int messageResId) {
        if (sErrorListener != null) {
            sErrorListener.onShowError(context, name, messageResId);
        }
    }

    public static void setErrorListener(ErrorListener listener) {
        sErrorListener = listener;
    }
}
