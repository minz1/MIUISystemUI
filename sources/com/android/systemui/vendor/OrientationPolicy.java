package com.android.systemui.vendor;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.AsyncTask;
import android.os.SystemProperties;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

public class OrientationPolicy {
    private Display mDisplay;
    private final DisplayManager mDisplayManager;
    private int mLastRotation = -1;
    private final CustomDisplayListener mOrientationDetector;

    private class CustomDisplayListener implements DisplayManager.DisplayListener {
        private CustomDisplayListener() {
        }

        public void onDisplayAdded(int displayId) {
        }

        public void onDisplayRemoved(int displayId) {
        }

        public void onDisplayChanged(int displayId) {
            OrientationPolicy.this.writeRotationForBsp();
        }
    }

    public OrientationPolicy(Context context) {
        DisplayMetrics dm = new DisplayMetrics();
        this.mDisplay = ((WindowManager) context.getSystemService("window")).getDefaultDisplay();
        this.mDisplay.getRealMetrics(dm);
        this.mOrientationDetector = new CustomDisplayListener();
        this.mDisplayManager = (DisplayManager) context.getSystemService("display");
        writeRotationForBsp();
        this.mDisplayManager.registerDisplayListener(this.mOrientationDetector, null);
    }

    /* access modifiers changed from: private */
    public void writeRotationForBsp() {
        int rotaion = -1;
        switch (this.mDisplay.getRotation()) {
            case 0:
                rotaion = 0;
                break;
            case 1:
                rotaion = 90;
                break;
            case 2:
                rotaion = 180;
                break;
            case 3:
                rotaion = 270;
                break;
        }
        if (this.mLastRotation != rotaion) {
            final int finalRotaion = rotaion;
            AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
                public void run() {
                    SystemProperties.set("sys.tp.grip_enable", Integer.toString(finalRotaion));
                }
            });
            this.mLastRotation = rotaion;
        }
    }
}
