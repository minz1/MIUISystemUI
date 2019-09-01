package com.android.settingslib.deviceinfo;

import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

public abstract class AbstractConnectivityPreferenceController extends AbstractPreferenceController implements LifecycleObserver, OnStart, OnStop {
}
