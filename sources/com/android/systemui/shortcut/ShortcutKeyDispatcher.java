package com.android.systemui.shortcut;

import android.app.ActivityManager;
import android.app.ActivityManagerCompat;
import android.app.IActivityManager;
import android.os.RemoteException;
import android.util.ArraySet;
import android.util.Log;
import android.view.IWindowManager;
import android.view.WindowManagerGlobal;
import com.android.systemui.SystemUI;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.shortcut.ShortcutKeyServiceProxy;
import com.android.systemui.stackdivider.Divider;
import com.android.systemui.stackdivider.DividerSnapAlgorithm;
import com.android.systemui.stackdivider.DividerView;
import java.util.List;

public class ShortcutKeyDispatcher extends SystemUI implements ShortcutKeyServiceProxy.Callbacks {
    protected final long ALT_MASK = 8589934592L;
    protected final long CTRL_MASK = 17592186044416L;
    protected final long META_MASK = 281474976710656L;
    protected final long SC_DOCK_LEFT = 281474976710727L;
    protected final long SC_DOCK_RIGHT = 281474976710728L;
    protected final long SHIFT_MASK = 4294967296L;
    private IActivityManager mActivityManager = ActivityManagerCompat.getService();
    private ShortcutKeyServiceProxy mShortcutKeyServiceProxy = new ShortcutKeyServiceProxy(this);
    private IWindowManager mWindowManagerService = WindowManagerGlobal.getWindowManagerService();

    public void registerShortcutKey(long shortcutCode) {
        try {
            this.mWindowManagerService.registerShortcutKey(shortcutCode, this.mShortcutKeyServiceProxy);
        } catch (RemoteException e) {
        }
    }

    public void onShortcutKeyPressed(long shortcutCode) {
        int orientation = this.mContext.getResources().getConfiguration().orientation;
        if ((shortcutCode == 281474976710727L || shortcutCode == 281474976710728L) && orientation == 2) {
            handleDockKey(shortcutCode);
        }
    }

    public void start() {
        registerShortcutKey(281474976710727L);
        registerShortcutKey(281474976710728L);
    }

    private void handleDockKey(long shortcutCode) {
        DividerSnapAlgorithm.SnapTarget target;
        try {
            if (this.mWindowManagerService.getDockedStackSide() == -1) {
                Recents recents = (Recents) getComponent(Recents.class);
                int dockMode = shortcutCode == 281474976710727L ? 0 : 1;
                List<ActivityManager.RecentTaskInfo> taskList = SystemServicesProxy.getInstance(this.mContext).getRecentTasks(1, -2, false, new ArraySet());
                recents.showRecentApps(false, false);
                if (!taskList.isEmpty()) {
                    SystemServicesProxy.getInstance(this.mContext).startTaskInDockedMode(taskList.get(0).id, dockMode);
                }
                return;
            }
            DividerView dividerView = ((Divider) getComponent(Divider.class)).getView();
            DividerSnapAlgorithm snapAlgorithm = dividerView.getSnapAlgorithm();
            DividerSnapAlgorithm.SnapTarget currentTarget = snapAlgorithm.calculateNonDismissingSnapTarget(dividerView.getCurrentPosition());
            if (shortcutCode == 281474976710727L) {
                target = snapAlgorithm.getPreviousTarget(currentTarget);
            } else {
                target = snapAlgorithm.getNextTarget(currentTarget);
            }
            dividerView.startDragging(true, false);
            dividerView.stopDragging(target.position, 0.0f, false, true);
        } catch (RemoteException e) {
            Log.e("ShortcutKeyDispatcher", "handleDockKey() failed.");
        }
    }
}
