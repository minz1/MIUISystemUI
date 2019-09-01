package com.miui.systemui.support.v4.app;

import android.app.Activity;
import com.miui.systemui.support.v4.util.SimpleArrayMap;

public class SupportActivity extends Activity {
    private SimpleArrayMap<Class<? extends Object>, Object> mExtraDataMap = new SimpleArrayMap<>();
}
