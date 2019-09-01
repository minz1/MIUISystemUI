package com.miui.internal.policy.impl.AwesomeLockScreenImp;

import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.view.IWindowManager;
import android.view.MotionEvent;
import com.android.keyguard.MiuiKeyguardUtils;
import com.android.systemui.statusbar.phone.NotificationPanelView;
import miui.maml.component.MamlView;
import miui.util.ProximitySensorWrapper;

public class AwesomeLockScreenView extends MamlView {
    private boolean mHasNavigationBar;
    private boolean mIsPsensorDisabled = false;
    private NotificationPanelView mPanelView;
    private boolean mPaused = false;
    private ProximitySensorWrapper mProximitySensorWrapper = null;
    private final boolean mWakeupForNotification = MiuiKeyguardUtils.isWakeupForNotification(this.mContext.getContentResolver());

    public AwesomeLockScreenView(Context context, LockScreenRoot root) {
        super(context, root);
        initInner();
        this.mIsPsensorDisabled = MiuiKeyguardUtils.isPsensorDisabled(this.mContext);
    }

    public void pause() {
        this.mPaused = true;
        onPause();
    }

    public void resume() {
        this.mPaused = false;
        onResume();
    }

    private void initInner() {
        try {
            this.mHasNavigationBar = IWindowManager.Stub.asInterface(ServiceManager.getService("window")).hasNavigationBar();
        } catch (RemoteException e) {
        }
    }

    public void setPanelView(NotificationPanelView panelView) {
        this.mPanelView = panelView;
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (this.mPanelView != null && this.mPanelView.isInSettings()) {
            return false;
        }
        if (event.getPointerCount() > 1) {
            event.setAction(3);
            Log.d("AwesomeLockScreenView", "touch point count > 1, set to ACTION_CANCEL");
        } else if (this.mHasNavigationBar) {
            int actionMasked = event.getActionMasked();
            if (actionMasked == 3 || actionMasked == 4) {
                event.setAction(1);
            }
        }
        return AwesomeLockScreenView.super.onTouchEvent(event);
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        AwesomeLockScreenView.super.onAttachedToWindow();
        if (this.mPaused) {
            onPause();
        }
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        AwesomeLockScreenView.super.onDetachedFromWindow();
    }

    public void rebindRoot() {
        init();
    }
}
