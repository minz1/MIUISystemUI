package com.android.systemui.statusbar.phone;

import android.content.ComponentName;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.IBinder;
import android.os.UserHandleCompat;
import android.util.ArraySet;
import android.view.ViewGroup;
import com.android.internal.os.SomeArgs;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.systemui.Dependency;
import com.android.systemui.Dumpable;
import com.android.systemui.R;
import com.android.systemui.SystemUI;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.StatusBarIconView;
import com.android.systemui.statusbar.phone.StatusBarIconController;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.DarkIconDispatcher;
import com.android.systemui.tuner.TunerService;
import com.miui.systemui.annotation.Inject;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

public class StatusBarIconControllerImpl extends StatusBarIconList implements Dumpable, CommandQueue.Callbacks, StatusBarIconController, ConfigurationController.ConfigurationListener, TunerService.Tunable {
    private Context mContext;
    private final DarkIconDispatcher mDarkIconDispatcher;
    private boolean mDemoMode;
    private final ArraySet<String> mIconBlacklist = new ArraySet<>();
    private final ArrayList<StatusBarIconController.IconManager> mIconGroups = new ArrayList<>();

    public StatusBarIconControllerImpl(@Inject Context context) {
        super(context.getResources().getStringArray(17236042));
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).addCallback(this);
        this.mDarkIconDispatcher = (DarkIconDispatcher) Dependency.get(DarkIconDispatcher.class);
        this.mContext = context;
        loadDimens();
        ((CommandQueue) SystemUI.getComponent(context, CommandQueue.class)).addCallbacks(this);
        ((TunerService) Dependency.get(TunerService.class)).addTunable(this, "icon_blacklist");
    }

    public void addIconGroup(StatusBarIconController.IconManager group) {
        this.mIconGroups.add(group);
        for (int i = 0; i < this.mIcons.size(); i++) {
            StatusBarIcon icon = (StatusBarIcon) this.mIcons.get(i);
            if (icon != null) {
                String slot = (String) this.mSlots.get(i);
                group.onIconAdded(getViewIndex(getSlotIndex(slot)), slot, this.mIconBlacklist.contains(slot), icon);
            }
        }
    }

    public void removeIconGroup(StatusBarIconController.IconManager group) {
        group.destroy();
        this.mIconGroups.remove(group);
    }

    public void onTuningChanged(String key, String newValue) {
        if ("icon_blacklist".equals(key)) {
            this.mIconBlacklist.clear();
            this.mIconBlacklist.addAll(StatusBarIconControllerHelper.getIconBlacklist(newValue));
            ArrayList<StatusBarIcon> current = new ArrayList<>(this.mIcons);
            ArrayList<String> currentSlots = new ArrayList<>(this.mSlots);
            for (int i = current.size() - 1; i >= 0; i--) {
                removeIcon(currentSlots.get(i));
            }
            for (int i2 = 0; i2 < current.size(); i2++) {
                setIcon(currentSlots.get(i2), current.get(i2));
            }
        }
    }

    private void loadDimens() {
    }

    private void addSystemIcon(int index, StatusBarIcon icon) {
        String slot = getSlot(index);
        int viewIndex = getViewIndex(index);
        boolean blocked = this.mIconBlacklist.contains(slot);
        Iterator<StatusBarIconController.IconManager> it = this.mIconGroups.iterator();
        while (it.hasNext()) {
            it.next().onIconAdded(viewIndex, slot, blocked, icon);
        }
    }

    public void setIcon(String slot, int resourceId, CharSequence contentDescription) {
        if (this.mSlots.contains(slot)) {
            int index = getSlotIndex(slot);
            StatusBarIcon icon = getIcon(index);
            if (icon == null) {
                StatusBarIcon icon2 = new StatusBarIcon(UserHandleCompat.SYSTEM, this.mContext.getPackageName(), Icon.createWithResource(this.mContext, resourceId), 0, 0, contentDescription);
                setIcon(slot, icon2);
                return;
            }
            icon.icon = Icon.createWithResource(this.mContext, resourceId);
            icon.contentDescription = contentDescription;
            handleSet(index, icon);
        }
    }

    public void setExternalIcon(String slot) {
        int viewIndex = getViewIndex(getSlotIndex(slot));
        int height = this.mContext.getResources().getDimensionPixelSize(R.dimen.status_bar_icon_drawing_size);
        Iterator<StatusBarIconController.IconManager> it = this.mIconGroups.iterator();
        while (it.hasNext()) {
            it.next().onIconExternal(viewIndex, height);
        }
    }

    public void setIcon(String slot, StatusBarIcon icon) {
        if (this.mSlots.contains(slot)) {
            setIcon(getSlotIndex(slot), icon);
        }
    }

    public void removeIcon(String slot) {
        if (this.mSlots.contains(slot)) {
            removeIcon(getSlotIndex(slot));
        }
    }

    public void disable(int state1, int state2, boolean animate) {
    }

    public void animateExpandNotificationsPanel() {
    }

    public void animateCollapsePanels(int flags) {
    }

    public void animateExpandSettingsPanel(String obj) {
    }

    public void setSystemUiVisibility(int vis, int fullscreenStackVis, int dockedStackVis, int mask, Rect fullscreenStackBounds, Rect dockedStackBounds) {
    }

    public void topAppWindowChanged(boolean visible) {
    }

    public void setImeWindowStatus(IBinder token, int vis, int backDisposition, boolean showImeSwitcher) {
    }

    public void showRecentApps(boolean triggeredFromAltTab, boolean fromHome) {
    }

    public void hideRecentApps(boolean triggeredFromAltTab, boolean triggeredFromHomeKey) {
    }

    public void toggleRecentApps() {
    }

    public void toggleSplitScreen() {
    }

    public void preloadRecentApps() {
    }

    public void dismissKeyboardShortcutsMenu() {
    }

    public void toggleKeyboardShortcutsMenu(int deviceId) {
    }

    public void cancelPreloadRecentApps() {
    }

    public void setWindowState(int window, int state) {
    }

    public void showScreenPinningRequest(int taskId) {
    }

    public void appTransitionPending(boolean forced) {
    }

    public void appTransitionCancelled() {
    }

    public void appTransitionStarting(long startTime, long duration, boolean forced) {
    }

    public void appTransitionFinished() {
    }

    public void showAssistDisclosure() {
    }

    public void startAssist(Bundle args) {
    }

    public void showPictureInPictureMenu() {
    }

    public void addQsTile(ComponentName tile) {
    }

    public void remQsTile(ComponentName tile) {
    }

    public void clickTile(ComponentName tile) {
    }

    public void showFingerprintDialog(SomeArgs args) {
    }

    public void onFingerprintAuthenticated() {
    }

    public void onFingerprintHelp(String message) {
    }

    public void onFingerprintError(String error) {
    }

    public void hideFingerprintDialog() {
    }

    public void handleSystemNavigationKey(int arg1) {
    }

    public void handleShowGlobalActionsMenu() {
    }

    public void setStatus(int what, String action, Bundle ext) {
    }

    public void setIconVisibility(String slot, boolean visibility) {
        if (this.mSlots.contains(slot)) {
            int index = getSlotIndex(slot);
            StatusBarIcon icon = getIcon(index);
            if (icon != null && icon.visible != visibility) {
                icon.visible = visibility;
                handleSet(index, icon);
            }
        }
    }

    public void removeIcon(int index) {
        if (getIcon(index) != null) {
            super.removeIcon(index);
            int viewIndex = getViewIndex(index);
            Iterator<StatusBarIconController.IconManager> it = this.mIconGroups.iterator();
            while (it.hasNext()) {
                it.next().onRemoveIcon(viewIndex, getSlot(index));
            }
        }
    }

    public void setIcon(int index, StatusBarIcon icon) {
        if (icon == null) {
            removeIcon(index);
            return;
        }
        boolean isNew = getIcon(index) == null;
        super.setIcon(index, icon);
        if (isNew) {
            addSystemIcon(index, icon);
        } else {
            handleSet(index, icon);
        }
    }

    private void handleSet(int index, StatusBarIcon icon) {
        int viewIndex = getViewIndex(index);
        Iterator<StatusBarIconController.IconManager> it = this.mIconGroups.iterator();
        while (it.hasNext()) {
            it.next().onSetIcon(viewIndex, getSlot(index), icon);
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (this.mIconGroups.size() != 0) {
            ViewGroup statusIcons = this.mIconGroups.get(0).mGroup;
            int N = statusIcons.getChildCount();
            pw.println("  icon views: " + N);
            for (int i = 0; i < N; i++) {
                pw.println("    [" + i + "] icon=" + ((StatusBarIconView) statusIcons.getChildAt(i)));
            }
            super.dump(pw);
        }
    }

    public void dispatchDemoCommand(String command, Bundle args) {
        int i = 0;
        if (!this.mDemoMode && command.equals("enter")) {
            this.mDemoMode = true;
            while (true) {
                int i2 = i;
                if (i2 < this.mIconGroups.size()) {
                    this.mIconGroups.get(i2).mGroup.setVisibility(8);
                    i = i2 + 1;
                } else {
                    return;
                }
            }
        } else if (this.mDemoMode && command.equals("exit")) {
            this.mDemoMode = false;
            for (int i3 = 0; i3 < this.mIconGroups.size(); i3++) {
                this.mIconGroups.get(i3).mGroup.setVisibility(0);
            }
        }
    }

    public void onConfigChanged(Configuration newConfig) {
    }

    public void onDensityOrFontScaleChanged() {
        loadDimens();
    }
}
