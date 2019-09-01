package com.android.systemui.stackdivider;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.graphics.Rect;
import android.os.RemoteException;
import android.util.Log;
import android.view.WindowManagerGlobal;
import com.android.internal.annotations.GuardedBy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WindowManagerProxy {
    private static final WindowManagerProxy sInstance = new WindowManagerProxy();
    /* access modifiers changed from: private */
    public float mDimLayerAlpha;
    private final Runnable mDimLayerRunnable = new Runnable() {
        public void run() {
            try {
                WindowManagerGlobal.getWindowManagerService().setResizeDimLayer(WindowManagerProxy.this.mDimLayerVisible, WindowManagerProxy.this.mDimLayerTargetWindowingMode, WindowManagerProxy.this.mDimLayerAlpha);
            } catch (RemoteException e) {
                Log.w("WindowManagerProxy", "Failed to resize stack: " + e);
            }
        }
    };
    /* access modifiers changed from: private */
    public int mDimLayerTargetWindowingMode;
    /* access modifiers changed from: private */
    public boolean mDimLayerVisible;
    private final Runnable mDismissRunnable = new Runnable() {
        public void run() {
            try {
                ActivityManager.getService().dismissSplitScreenMode(false);
                Log.i("WindowManagerProxy", "exit splitScreen mode ---- dismiss.");
            } catch (RemoteException e) {
                Log.w("WindowManagerProxy", "Failed to remove stack: " + e);
            }
        }
    };
    /* access modifiers changed from: private */
    @GuardedBy("mDockedRect")
    public final Rect mDockedRect = new Rect();
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final Runnable mMaximizeRunnable = new Runnable() {
        public void run() {
            try {
                ActivityManager.getService().dismissSplitScreenMode(true);
                Log.i("WindowManagerProxy", "exit splitScreen mode ---- maximize.");
            } catch (RemoteException e) {
                Log.w("WindowManagerProxy", "Failed to resize stack: " + e);
            }
        }
    };
    private final Runnable mResizeRunnable = new Runnable() {
        public void run() {
            Rect rect;
            Rect rect2;
            Rect rect3;
            Rect rect4;
            synchronized (WindowManagerProxy.this.mDockedRect) {
                WindowManagerProxy.this.mTmpRect1.set(WindowManagerProxy.this.mDockedRect);
                WindowManagerProxy.this.mTmpRect2.set(WindowManagerProxy.this.mTempDockedTaskRect);
                WindowManagerProxy.this.mTmpRect3.set(WindowManagerProxy.this.mTempDockedInsetRect);
                WindowManagerProxy.this.mTmpRect4.set(WindowManagerProxy.this.mTempOtherTaskRect);
                WindowManagerProxy.this.mTmpRect5.set(WindowManagerProxy.this.mTempOtherInsetRect);
            }
            try {
                IActivityManager iActivityManager = ActivityManagerNative.getDefault();
                Rect access$100 = WindowManagerProxy.this.mTmpRect1;
                if (WindowManagerProxy.this.mTmpRect2.isEmpty()) {
                    rect = null;
                } else {
                    rect = WindowManagerProxy.this.mTmpRect2;
                }
                if (WindowManagerProxy.this.mTmpRect3.isEmpty()) {
                    rect2 = null;
                } else {
                    rect2 = WindowManagerProxy.this.mTmpRect3;
                }
                if (WindowManagerProxy.this.mTmpRect4.isEmpty()) {
                    rect3 = null;
                } else {
                    rect3 = WindowManagerProxy.this.mTmpRect4;
                }
                if (WindowManagerProxy.this.mTmpRect5.isEmpty()) {
                    rect4 = null;
                } else {
                    rect4 = WindowManagerProxy.this.mTmpRect5;
                }
                iActivityManager.resizeDockedStack(access$100, rect, rect2, rect3, rect4);
            } catch (RemoteException e) {
                Log.w("WindowManagerProxy", "Failed to resize stack: " + e);
            }
        }
    };
    private final Runnable mSetTouchableRegionRunnable = new Runnable() {
        public void run() {
            try {
                synchronized (WindowManagerProxy.this.mDockedRect) {
                    WindowManagerProxy.this.mTmpRect1.set(WindowManagerProxy.this.mTouchableRegion);
                }
                WindowManagerGlobal.getWindowManagerService().setDockedStackDividerTouchRegion(WindowManagerProxy.this.mTmpRect1);
            } catch (RemoteException e) {
                Log.w("WindowManagerProxy", "Failed to set touchable region: " + e);
            }
        }
    };
    private final Runnable mSwapRunnable = new Runnable() {
        public void run() {
        }
    };
    /* access modifiers changed from: private */
    public final Rect mTempDockedInsetRect = new Rect();
    /* access modifiers changed from: private */
    public final Rect mTempDockedTaskRect = new Rect();
    /* access modifiers changed from: private */
    public final Rect mTempOtherInsetRect = new Rect();
    /* access modifiers changed from: private */
    public final Rect mTempOtherTaskRect = new Rect();
    /* access modifiers changed from: private */
    public final Rect mTmpRect1 = new Rect();
    /* access modifiers changed from: private */
    public final Rect mTmpRect2 = new Rect();
    /* access modifiers changed from: private */
    public final Rect mTmpRect3 = new Rect();
    /* access modifiers changed from: private */
    public final Rect mTmpRect4 = new Rect();
    /* access modifiers changed from: private */
    public final Rect mTmpRect5 = new Rect();
    /* access modifiers changed from: private */
    @GuardedBy("mDockedRect")
    public final Rect mTouchableRegion = new Rect();

    private WindowManagerProxy() {
    }

    public static WindowManagerProxy getInstance() {
        return sInstance;
    }

    public void resizeDockedStack(Rect docked, Rect tempDockedTaskRect, Rect tempDockedInsetRect, Rect tempOtherTaskRect, Rect tempOtherInsetRect, boolean resize) {
        synchronized (this.mDockedRect) {
            this.mDockedRect.set(docked);
            if (tempDockedTaskRect != null) {
                this.mTempDockedTaskRect.set(tempDockedTaskRect);
            } else {
                this.mTempDockedTaskRect.setEmpty();
            }
            if (tempDockedInsetRect != null) {
                this.mTempDockedInsetRect.set(tempDockedInsetRect);
            } else {
                this.mTempDockedInsetRect.setEmpty();
            }
            if (tempOtherTaskRect != null) {
                this.mTempOtherTaskRect.set(tempOtherTaskRect);
            } else {
                this.mTempOtherTaskRect.setEmpty();
            }
            if (tempOtherInsetRect != null) {
                this.mTempOtherInsetRect.set(tempOtherInsetRect);
            } else {
                this.mTempOtherInsetRect.setEmpty();
            }
        }
        if (resize) {
            this.mExecutor.execute(this.mResizeRunnable);
        }
    }

    public void dismissDockedStack() {
        this.mExecutor.execute(this.mDismissRunnable);
    }

    public void maximizeDockedStack() {
        this.mExecutor.execute(this.mMaximizeRunnable);
    }

    public void setResizing(final boolean resizing) {
        this.mExecutor.execute(new Runnable() {
            public void run() {
                try {
                    ActivityManager.getService().setSplitScreenResizing(resizing);
                } catch (RemoteException e) {
                    Log.w("WindowManagerProxy", "Error calling setDockedStackResizing: " + e);
                }
            }
        });
    }

    public int getDockSide() {
        try {
            return WindowManagerGlobal.getWindowManagerService().getDockedStackSide();
        } catch (RemoteException e) {
            Log.w("WindowManagerProxy", "Failed to get dock side: " + e);
            return -1;
        }
    }

    public void setResizeDimLayer(boolean visible, int targetStackId, int targetWindowingMode, float alpha) {
        this.mDimLayerVisible = visible;
        this.mDimLayerTargetWindowingMode = targetWindowingMode;
        this.mDimLayerAlpha = alpha;
        this.mExecutor.execute(this.mDimLayerRunnable);
    }

    public void swapTasks() {
        this.mExecutor.execute(this.mSwapRunnable);
    }

    public void setTouchRegion(Rect region) {
        synchronized (this.mDockedRect) {
            this.mTouchableRegion.set(region);
        }
        this.mExecutor.execute(this.mSetTouchableRegionRunnable);
    }
}
