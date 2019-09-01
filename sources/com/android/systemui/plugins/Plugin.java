package com.android.systemui.plugins;

import android.content.Context;

public interface Plugin {
    int getVersion();

    void onCreate(Context context, Context context2);

    void onDestroy();
}
