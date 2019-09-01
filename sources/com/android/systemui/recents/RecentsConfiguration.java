package com.android.systemui.recents;

import android.content.Context;
import android.content.res.Resources;
import com.android.systemui.R;
import com.android.systemui.recents.misc.SystemServicesProxy;

public class RecentsConfiguration {
    public static boolean sCanMultiWindow = false;
    public boolean fakeShadows;
    public final boolean isLargeScreen;
    public final boolean isXLargeScreen;
    public RecentsActivityLaunchState mLaunchState = new RecentsActivityLaunchState();
    public final int smallestWidth;
    public int svelteLevel;

    public RecentsConfiguration(Context context) {
        SystemServicesProxy ssp = Recents.getSystemServices();
        Resources res = context.getApplicationContext().getResources();
        this.fakeShadows = res.getBoolean(R.bool.config_recents_fake_shadows);
        this.svelteLevel = res.getInteger(R.integer.recents_svelte_level);
        float screenDensity = context.getResources().getDisplayMetrics().density;
        this.smallestWidth = ssp.getDeviceSmallestWidth();
        boolean z = false;
        this.isLargeScreen = this.smallestWidth >= ((int) (600.0f * screenDensity));
        this.isXLargeScreen = this.smallestWidth >= ((int) (720.0f * screenDensity)) ? true : z;
    }

    public RecentsActivityLaunchState getLaunchState() {
        return this.mLaunchState;
    }
}
