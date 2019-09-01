package com.android.systemui.recents;

import android.content.Context;
import com.android.systemui.recents.misc.SystemServicesProxy;

public class RecentsDebugFlags {
    public RecentsDebugFlags(Context context) {
    }

    public boolean isFastToggleRecentsEnabled() {
        SystemServicesProxy ssp = Recents.getSystemServices();
        return (ssp.hasFreeformWorkspaceSupport() || ssp.isTouchExplorationEnabled()) ? false : false;
    }

    public boolean isPagingEnabled() {
        return false;
    }
}
