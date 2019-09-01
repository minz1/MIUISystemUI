package com.android.systemui.qs.tiles;

import com.android.settingslib.wifi.AccessPoint;

public class WifiTileHelper {
    public static AccessPoint[] filterUnreachableAPs(AccessPoint[] accessPoints) {
        int numReachable = 0;
        for (AccessPoint ap : accessPoints) {
            if (ap.isReachable()) {
                numReachable++;
            }
        }
        if (numReachable != accessPoints.length) {
            AccessPoint[] unfiltered = accessPoints;
            accessPoints = new AccessPoint[numReachable];
            int i = 0;
            for (AccessPoint ap2 : unfiltered) {
                if (ap2.isReachable()) {
                    accessPoints[i] = ap2;
                    i++;
                }
            }
        }
        return accessPoints;
    }
}
