package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.RemoteException;
import android.provider.Settings;
import android.view.IWindowManager;
import android.view.WindowManagerGlobal;
import com.android.internal.view.RotationPolicy;
import com.android.systemui.R;
import com.android.systemui.Util;
import com.android.systemui.statusbar.policy.RotationLockController;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
import miui.util.ObjectReference;
import miui.util.ReflectionUtils;

public final class RotationLockControllerImpl implements RotationLockController {
    private final CopyOnWriteArrayList<RotationLockController.RotationLockControllerCallback> mCallbacks = new CopyOnWriteArrayList<>();
    /* access modifiers changed from: private */
    public final Context mContext;
    /* access modifiers changed from: private */
    public final Handler mHandler = new Handler();
    private final RotationPolicy.RotationPolicyListener mRotationPolicyListener = new RotationPolicy.RotationPolicyListener() {
        public void onChange() {
            RotationLockControllerImpl.this.notifyChanged();
        }
    };

    public RotationLockControllerImpl(Context context) {
        this.mContext = context;
        setListening(true);
    }

    public void addCallback(RotationLockController.RotationLockControllerCallback callback) {
        this.mCallbacks.add(callback);
        notifyChanged(callback);
    }

    public void removeCallback(RotationLockController.RotationLockControllerCallback callback) {
        this.mCallbacks.remove(callback);
    }

    public int getRotationLockOrientation() {
        return 0;
    }

    public boolean isRotationLocked() {
        return RotationPolicy.isRotationLocked(this.mContext);
    }

    public void setRotationLocked(boolean locked) {
        setRotationLock(this.mContext, locked);
    }

    public void setListening(boolean listening) {
        if (listening) {
            RotationPolicy.registerRotationPolicyListener(this.mContext, this.mRotationPolicyListener, -1);
        } else {
            RotationPolicy.unregisterRotationPolicyListener(this.mContext, this.mRotationPolicyListener);
        }
    }

    /* access modifiers changed from: private */
    public void notifyChanged() {
        Iterator<RotationLockController.RotationLockControllerCallback> it = this.mCallbacks.iterator();
        while (it.hasNext()) {
            notifyChanged(it.next());
        }
    }

    private void notifyChanged(RotationLockController.RotationLockControllerCallback callback) {
        callback.onRotationLockStateChanged(RotationPolicy.isRotationLocked(this.mContext), RotationPolicy.isRotationLockToggleVisible(this.mContext));
    }

    public void setRotationLock(Context context, boolean enabled) {
        Settings.System.putIntForUser(context.getContentResolver(), "hide_rotation_lock_toggle_for_accessibility", 0, -2);
        setRotationLock(enabled, -1);
    }

    private void setRotationLock(final boolean enabled, final int rotation) {
        AsyncTask.execute(new Runnable() {
            public void run() {
                try {
                    IWindowManager wm = WindowManagerGlobal.getWindowManagerService();
                    if (enabled) {
                        wm.freezeRotation(rotation);
                        int curRotation = RotationLockControllerImpl.this.getRotation(wm);
                        if (!(curRotation == 0 || 2 == curRotation)) {
                            RotationLockControllerImpl.this.mHandler.post(new Runnable() {
                                public void run() {
                                    Util.showSystemOverlayToast(RotationLockControllerImpl.this.mContext, (int) R.string.miui_screen_rotation_freeze_message, 1);
                                }
                            });
                        }
                        return;
                    }
                    wm.thawRotation();
                } catch (RemoteException e) {
                }
            }
        });
    }

    /* access modifiers changed from: private */
    public int getRotation(IWindowManager wm) {
        ObjectReference<Integer> reference;
        if (Build.VERSION.SDK_INT < 26) {
            reference = ReflectionUtils.tryCallMethod(wm, "getRotation", Integer.class, new Object[0]);
        } else {
            reference = ReflectionUtils.tryCallMethod(wm, "getDefaultDisplayRotation", Integer.class, new Object[0]);
        }
        return ((Integer) reference.get()).intValue();
    }
}
