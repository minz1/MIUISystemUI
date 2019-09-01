package com.android.systemui.miui.statusbar;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

public class DependenciesSetup {
    private Context mContext;

    public void setContext(Context context) {
        this.mContext = context;
    }

    public Context getContext() {
        return this.mContext;
    }

    public Handler getMainHandler() {
        return new Handler(Looper.getMainLooper());
    }

    public Handler getTimeTickHandler() {
        HandlerThread thread = new HandlerThread("TimeTick");
        thread.start();
        return new Handler(thread.getLooper());
    }

    public Handler getScreenOffHandler() {
        HandlerThread thread = new HandlerThread("ScreenOff");
        thread.start();
        return new Handler(thread.getLooper());
    }

    public Looper getSysUIBgLooper() {
        HandlerThread thread = new HandlerThread("SysUiBg");
        thread.start();
        return thread.getLooper();
    }

    public Looper getNetBgLooper() {
        HandlerThread thread = new HandlerThread("SysUiNetBg");
        thread.start();
        return thread.getLooper();
    }

    public Looper getBtBgLooper() {
        HandlerThread thread = new HandlerThread("SysUiBtBg");
        thread.start();
        return thread.getLooper();
    }
}
