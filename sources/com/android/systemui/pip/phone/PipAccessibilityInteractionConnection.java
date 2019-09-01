package com.android.systemui.pip.phone;

import android.graphics.Rect;
import android.graphics.Region;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.view.MagnificationSpec;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.IAccessibilityInteractionConnection;
import android.view.accessibility.IAccessibilityInteractionConnectionCallback;
import java.util.ArrayList;
import java.util.List;

public class PipAccessibilityInteractionConnection extends IAccessibilityInteractionConnection.Stub {
    private List<AccessibilityNodeInfo> mAccessibilityNodeInfoList;
    private AccessibilityCallbacks mCallbacks;
    private Handler mHandler;
    private PipMotionHelper mMotionHelper;
    private Rect mTmpBounds = new Rect();

    public interface AccessibilityCallbacks {
        void onAccessibilityShowMenu();
    }

    public PipAccessibilityInteractionConnection(PipMotionHelper motionHelper, AccessibilityCallbacks callbacks, Handler handler) {
        this.mHandler = handler;
        this.mMotionHelper = motionHelper;
        this.mCallbacks = callbacks;
    }

    public void findAccessibilityNodeInfoByAccessibilityId(long accessibilityNodeId, Region interactiveRegion, int interactionId, IAccessibilityInteractionConnectionCallback callback, int flags, int interrogatingPid, long interrogatingTid, MagnificationSpec spec, Bundle args) {
        try {
            callback.setFindAccessibilityNodeInfosResult(accessibilityNodeId == AccessibilityNodeInfo.ROOT_NODE_ID ? getNodeList() : null, interactionId);
        } catch (RemoteException e) {
        }
    }

    public void performAccessibilityAction(long accessibilityNodeId, int action, Bundle arguments, int interactionId, IAccessibilityInteractionConnectionCallback callback, int flags, int interrogatingPid, long interrogatingTid) {
        int i = action;
        Bundle bundle = arguments;
        boolean result = false;
        if (accessibilityNodeId == AccessibilityNodeInfo.ROOT_NODE_ID) {
            if (i == 16) {
                this.mHandler.post(new Runnable() {
                    public final void run() {
                        PipAccessibilityInteractionConnection.this.mCallbacks.onAccessibilityShowMenu();
                    }
                });
                result = true;
            } else if (i == 262144) {
                this.mMotionHelper.expandPip();
                result = true;
            } else if (i == 1048576) {
                this.mMotionHelper.dismissPip();
                result = true;
            } else if (i == 16908354) {
                int newX = bundle.getInt("ACTION_ARGUMENT_MOVE_WINDOW_X");
                int newY = bundle.getInt("ACTION_ARGUMENT_MOVE_WINDOW_Y");
                new Rect().set(this.mMotionHelper.getBounds());
                this.mTmpBounds.offsetTo(newX, newY);
                this.mMotionHelper.movePip(this.mTmpBounds);
                result = true;
            }
        }
        try {
            callback.setPerformAccessibilityActionResult(result, interactionId);
        } catch (RemoteException e) {
        }
    }

    public void findAccessibilityNodeInfosByViewId(long accessibilityNodeId, String viewId, Region interactiveRegion, int interactionId, IAccessibilityInteractionConnectionCallback callback, int flags, int interrogatingPid, long interrogatingTid, MagnificationSpec spec) {
        try {
            callback.setFindAccessibilityNodeInfoResult(null, interactionId);
        } catch (RemoteException e) {
        }
    }

    public void findAccessibilityNodeInfosByText(long accessibilityNodeId, String text, Region interactiveRegion, int interactionId, IAccessibilityInteractionConnectionCallback callback, int flags, int interrogatingPid, long interrogatingTid, MagnificationSpec spec) {
        try {
            callback.setFindAccessibilityNodeInfoResult(null, interactionId);
        } catch (RemoteException e) {
        }
    }

    public void findFocus(long accessibilityNodeId, int focusType, Region interactiveRegion, int interactionId, IAccessibilityInteractionConnectionCallback callback, int flags, int interrogatingPid, long interrogatingTid, MagnificationSpec spec) {
        try {
            callback.setFindAccessibilityNodeInfoResult(null, interactionId);
        } catch (RemoteException e) {
        }
    }

    public void focusSearch(long accessibilityNodeId, int direction, Region interactiveRegion, int interactionId, IAccessibilityInteractionConnectionCallback callback, int flags, int interrogatingPid, long interrogatingTid, MagnificationSpec spec) {
        try {
            callback.setFindAccessibilityNodeInfoResult(null, interactionId);
        } catch (RemoteException e) {
        }
    }

    public static AccessibilityNodeInfo obtainRootAccessibilityNodeInfo() {
        AccessibilityNodeInfo info = AccessibilityNodeInfo.obtain();
        info.setSourceNodeId(AccessibilityNodeInfo.ROOT_NODE_ID, -3);
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK);
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_DISMISS);
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_MOVE_WINDOW);
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_EXPAND);
        info.setImportantForAccessibility(true);
        info.setClickable(true);
        info.setVisibleToUser(true);
        return info;
    }

    private List<AccessibilityNodeInfo> getNodeList() {
        if (this.mAccessibilityNodeInfoList == null) {
            this.mAccessibilityNodeInfoList = new ArrayList(1);
        }
        AccessibilityNodeInfo info = obtainRootAccessibilityNodeInfo();
        this.mAccessibilityNodeInfoList.clear();
        this.mAccessibilityNodeInfoList.add(info);
        return this.mAccessibilityNodeInfoList;
    }
}
