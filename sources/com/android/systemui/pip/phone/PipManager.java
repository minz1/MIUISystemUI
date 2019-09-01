package com.android.systemui.pip.phone;

import android.app.ActivityManager;
import android.app.ActivityManagerCompat;
import android.app.IActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ParceledListSlice;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import android.view.IPinnedStackController;
import android.view.IPinnedStackListener;
import android.view.IWindowManager;
import android.view.WindowManagerGlobal;
import com.android.systemui.pip.BasePipManager;
import com.android.systemui.recents.events.RecentsEventBus;
import com.android.systemui.recents.events.component.ExpandPipEvent;
import com.android.systemui.recents.misc.SystemServicesProxy;
import java.io.PrintWriter;
import java.util.Objects;

public class PipManager implements BasePipManager {
    private static PipManager sPipController;
    /* access modifiers changed from: private */
    public IActivityManager mActivityManager;
    /* access modifiers changed from: private */
    public Context mContext;
    /* access modifiers changed from: private */
    public Handler mHandler = new Handler();
    private InputConsumerController mInputConsumerController;
    /* access modifiers changed from: private */
    public PipMediaController mMediaController;
    /* access modifiers changed from: private */
    public PipMenuActivityController mMenuController;
    /* access modifiers changed from: private */
    public PipNotificationController mNotificationController;
    private final PinnedStackListener mPinnedStackListener = new PinnedStackListener();
    SystemServicesProxy.TaskStackListener mTaskStackListener = new SystemServicesProxy.TaskStackListener() {
        public void onActivityPinned(String packageName, int userId, int taskId) {
            if (checkCurrentUserId(PipManager.this.mContext, false)) {
                PipManager.this.mTouchHandler.onActivityPinned();
                PipManager.this.mMediaController.onActivityPinned();
                PipManager.this.mMenuController.onActivityPinned();
                PipManager.this.mNotificationController.onActivityPinned(packageName, true);
                SystemServicesProxy.getInstance(PipManager.this.mContext).setPipVisibility(true);
            }
        }

        public void onActivityUnpinned() {
            boolean z = false;
            if (checkCurrentUserId(PipManager.this.mContext, false)) {
                ComponentName topPipActivity = PipUtils.getTopPinnedActivity(PipManager.this.mContext, PipManager.this.mActivityManager);
                PipManager.this.mMenuController.hideMenu();
                PipManager.this.mNotificationController.onActivityUnpinned(topPipActivity);
                SystemServicesProxy instance = SystemServicesProxy.getInstance(PipManager.this.mContext);
                if (topPipActivity != null) {
                    z = true;
                }
                instance.setPipVisibility(z);
            }
        }

        public void onPinnedStackAnimationStarted() {
            PipManager.this.mTouchHandler.setTouchEnabled(false);
        }

        public void onPinnedStackAnimationEnded() {
            PipManager.this.mTouchHandler.setTouchEnabled(true);
            PipManager.this.mTouchHandler.onPinnedStackAnimationEnded();
            PipManager.this.mMenuController.onPinnedStackAnimationEnded();
            PipManager.this.mNotificationController.onPinnedStackAnimationEnded();
        }

        public void onPinnedActivityRestartAttempt(boolean clearedTask) {
            if (checkCurrentUserId(PipManager.this.mContext, false)) {
                PipManager.this.mTouchHandler.getMotionHelper().expandPip(clearedTask);
            }
        }
    };
    /* access modifiers changed from: private */
    public PipTouchHandler mTouchHandler;
    private IWindowManager mWindowManager;

    private class PinnedStackListener extends IPinnedStackListener.Stub {
        private PinnedStackListener() {
        }

        public void onListenerRegistered(IPinnedStackController controller) {
            PipManager.this.mHandler.post(new Runnable(controller) {
                private final /* synthetic */ IPinnedStackController f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    PipManager.this.mTouchHandler.setPinnedStackController(this.f$1);
                }
            });
        }

        public void onImeVisibilityChanged(boolean imeVisible, int imeHeight) {
            PipManager.this.mHandler.post(new Runnable(imeVisible, imeHeight) {
                private final /* synthetic */ boolean f$1;
                private final /* synthetic */ int f$2;

                {
                    this.f$1 = r2;
                    this.f$2 = r3;
                }

                public final void run() {
                    PipManager.this.mTouchHandler.onImeVisibilityChanged(this.f$1, this.f$2);
                }
            });
        }

        public void onMinimizedStateChanged(boolean isMinimized) {
            PipManager.this.mHandler.post(new Runnable(isMinimized) {
                private final /* synthetic */ boolean f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    PipManager.this.mTouchHandler.setMinimizedState(this.f$1, true);
                }
            });
        }

        public void onMovementBoundsChanged(Rect insetBounds, Rect normalBounds, Rect animatingBounds, boolean fromImeAdjustement, int displayRotation) {
            Handler access$700 = PipManager.this.mHandler;
            $$Lambda$PipManager$PinnedStackListener$lz8LZOo4cNXF4zdTXMHL7dEArI r1 = new Runnable(insetBounds, normalBounds, animatingBounds, fromImeAdjustement, displayRotation) {
                private final /* synthetic */ Rect f$1;
                private final /* synthetic */ Rect f$2;
                private final /* synthetic */ Rect f$3;
                private final /* synthetic */ boolean f$4;
                private final /* synthetic */ int f$5;

                {
                    this.f$1 = r2;
                    this.f$2 = r3;
                    this.f$3 = r4;
                    this.f$4 = r5;
                    this.f$5 = r6;
                }

                public final void run() {
                    PipManager.this.mTouchHandler.onMovementBoundsChanged(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5);
                }
            };
            access$700.post(r1);
        }

        public void onMovementBoundsChanged(Rect insetBounds, Rect normalBounds, Rect animatingBounds, boolean fromImeAdjustment, boolean fromShelfAdjustment, int displayRotation) {
            onMovementBoundsChanged(insetBounds, normalBounds, animatingBounds, fromImeAdjustment, displayRotation);
        }

        public void onActionsChanged(ParceledListSlice actions) {
            PipManager.this.mHandler.post(new Runnable(actions) {
                private final /* synthetic */ ParceledListSlice f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    PipManager.this.mMenuController.setAppActions(this.f$1);
                }
            });
        }

        public void onShelfVisibilityChanged(boolean shelfVisible, int shelfHeight) {
        }
    }

    private PipManager() {
    }

    public void initialize(Context context) {
        this.mContext = context;
        this.mActivityManager = ActivityManagerCompat.getService();
        this.mWindowManager = WindowManagerGlobal.getWindowManagerService();
        try {
            this.mWindowManager.registerPinnedStackListener(0, this.mPinnedStackListener);
        } catch (RemoteException e) {
            Log.e("PipManager", "Failed to register pinned stack listener", e);
        }
        SystemServicesProxy.getInstance(this.mContext).registerTaskStackListener(this.mTaskStackListener);
        this.mInputConsumerController = new InputConsumerController(this.mWindowManager);
        this.mMediaController = new PipMediaController(context, this.mActivityManager);
        this.mMenuController = new PipMenuActivityController(context, this.mActivityManager, this.mMediaController, this.mInputConsumerController);
        this.mTouchHandler = new PipTouchHandler(context, this.mActivityManager, this.mMenuController, this.mInputConsumerController);
        this.mNotificationController = new PipNotificationController(context, this.mActivityManager, this.mTouchHandler.getMotionHelper());
        RecentsEventBus.getDefault().register(this);
    }

    public void onConfigurationChanged(Configuration newConfig) {
        this.mTouchHandler.onConfigurationChanged();
    }

    public final void onBusEvent(ExpandPipEvent event) {
        Objects.requireNonNull(event);
        try {
            ActivityManager.StackInfo stackInfo = ActivityManagerCompat.getStackInfo(4, 2, 0);
            if (!(stackInfo == null || stackInfo.taskIds == null)) {
                SystemServicesProxy ssp = SystemServicesProxy.getInstance(this.mContext);
                for (int taskId : stackInfo.taskIds) {
                    ssp.cancelThumbnailTransition(taskId);
                }
            }
        } catch (Exception e) {
        }
        this.mTouchHandler.getMotionHelper().expandPip(false);
    }

    public void showPictureInPictureMenu() {
        this.mTouchHandler.showPictureInPictureMenu();
    }

    public static PipManager getInstance() {
        if (sPipController == null) {
            sPipController = new PipManager();
        }
        return sPipController;
    }

    public void dump(PrintWriter pw) {
        pw.println("PipManager");
        this.mInputConsumerController.dump(pw, "  ");
        this.mMenuController.dump(pw, "  ");
        this.mTouchHandler.dump(pw, "  ");
    }
}
