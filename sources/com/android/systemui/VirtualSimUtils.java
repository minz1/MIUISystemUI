package com.android.systemui;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MiuiSettings;
import android.util.Log;

public class VirtualSimUtils {
    public static boolean isVirtualSim(Context context, int slotId) {
        return MiuiSettings.VirtualSim.isVirtualSimEnabled(context) && slotId == MiuiSettings.VirtualSim.getVirtualSimSlotId(context);
    }

    public static String getVirtualSimCarrierName(Context context) {
        Bundle b = null;
        try {
            b = context.getContentResolver().call(Uri.parse("content://com.miui.virtualsim.provider.virtualsimInfo"), "getCarrierName", null, null);
        } catch (Exception e) {
            Log.d("VirtualSimUtils", "getVirtualSimCarrierName e" + e);
        }
        if (b == null) {
            return null;
        }
        return b.getString("carrierName");
    }
}
