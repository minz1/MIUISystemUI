package com.android.systemui.statusbar;

import android.telephony.ServiceState;
import android.util.Log;

public class NetworkTypeUtils {
    public static int getDataNetTypeFromServiceState(int srcDataNetType, ServiceState sState) {
        int destDataNetType = srcDataNetType;
        int i = 19;
        if ((destDataNetType == 13 || destDataNetType == 19) && sState != null) {
            if (!sState.isUsingCarrierAggregation()) {
                i = 13;
            }
            destDataNetType = i;
        }
        Log.d("NetworkTypeUtils", "getDataNetTypeFromServiceState:srcDataNetType = " + srcDataNetType + ", destDataNetType " + destDataNetType);
        return destDataNetType;
    }
}
