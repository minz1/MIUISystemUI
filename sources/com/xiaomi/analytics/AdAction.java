package com.xiaomi.analytics;

import android.text.TextUtils;
import java.util.List;

public class AdAction extends TrackAction {
    public AdAction(String actionType) {
        setCategory("ad");
        setAction(actionType);
    }

    public AdAction addAdMonitor(List<String> urlList) {
        if (urlList != null) {
            StringBuilder adMonitor = new StringBuilder();
            for (String url : urlList) {
                if (!TextUtils.isEmpty(url)) {
                    if (adMonitor.length() > 0) {
                        adMonitor.append("|");
                    }
                    adMonitor.append(url);
                }
            }
            if (adMonitor.length() > 0) {
                addExtra("_ad_monitor_", adMonitor.toString());
            }
        }
        return this;
    }
}
