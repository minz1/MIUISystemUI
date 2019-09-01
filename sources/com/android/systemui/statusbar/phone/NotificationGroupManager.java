package com.android.systemui.statusbar.phone;

import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.service.notification.StatusBarNotificationCompat;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import com.android.systemui.statusbar.policy.OnHeadsUpChangedListener;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

public class NotificationGroupManager implements OnHeadsUpChangedListener {
    private int mBarState = -1;
    private final HashMap<String, NotificationGroup> mGroupMap = new HashMap<>();
    private HeadsUpManager mHeadsUpManager;
    private boolean mIsUpdatingUnchangedGroup;
    private HashMap<String, StatusBarNotification> mIsolatedEntries = new HashMap<>();
    private OnGroupChangeListener mListener;

    public static class NotificationGroup {
        public final HashSet<NotificationData.Entry> children = new HashSet<>();
        public boolean expanded;
        public NotificationData.Entry summary;
        public boolean suppressed;

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("    summary:\n      ");
            sb.append(this.summary != null ? this.summary.notification : "null");
            String result = sb.toString();
            String result2 = result + "\n    children size: " + this.children.size();
            while (this.children.iterator().hasNext()) {
                result2 = result2 + "\n      " + r1.next().notification;
            }
            return result2;
        }
    }

    public interface OnGroupChangeListener {
        void onGroupCreatedFromChildren(NotificationGroup notificationGroup);

        void onGroupExpansionChanged(ExpandableNotificationRow expandableNotificationRow, boolean z);

        void onGroupsChanged();
    }

    public void setOnGroupChangeListener(OnGroupChangeListener listener) {
        this.mListener = listener;
    }

    public boolean isGroupExpanded(StatusBarNotification sbn) {
        NotificationGroup group = this.mGroupMap.get(getGroupKey(sbn));
        if (group == null) {
            return false;
        }
        return group.expanded;
    }

    public void setGroupExpanded(StatusBarNotification sbn, boolean expanded) {
        NotificationGroup group = this.mGroupMap.get(getGroupKey(sbn));
        if (group != null) {
            setGroupExpanded(group, expanded);
        }
    }

    private void setGroupExpanded(NotificationGroup group, boolean expanded) {
        group.expanded = expanded;
        if (group.summary != null) {
            this.mListener.onGroupExpansionChanged(group.summary.row, expanded);
        }
    }

    public void onEntryRemoved(NotificationData.Entry removed) {
        onEntryRemovedInternal(removed, removed.notification);
        this.mIsolatedEntries.remove(removed.key);
    }

    private void onEntryRemovedInternal(NotificationData.Entry removed, StatusBarNotification sbn) {
        String groupKey = getGroupKey(sbn);
        NotificationGroup group = this.mGroupMap.get(groupKey);
        if (group != null) {
            if (isGroupChild(sbn)) {
                group.children.remove(removed);
            } else {
                group.summary = null;
            }
            updateSuppression(group);
            if (group.children.isEmpty() && group.summary == null) {
                this.mGroupMap.remove(groupKey);
            }
        }
    }

    public boolean canRemove(NotificationGroup group) {
        if (group == null || group.summary == null || !group.children.isEmpty() || !group.summary.notification.getNotification().isGroupSummary() || hasIsolatedChildren(group) || !StatusBarNotificationCompat.isAutoGroupSummary(group.summary.notification)) {
            return false;
        }
        return true;
    }

    public void onEntryAdded(NotificationData.Entry added) {
        StatusBarNotification sbn = added.notification;
        boolean isGroupChild = isGroupChild(sbn);
        String groupKey = getGroupKey(sbn);
        NotificationGroup group = this.mGroupMap.get(groupKey);
        if (group == null) {
            group = new NotificationGroup();
            this.mGroupMap.put(groupKey, group);
        }
        if (isGroupChild) {
            group.children.add(added);
            updateSuppression(group);
            return;
        }
        group.summary = added;
        group.expanded = added.row.areChildrenExpanded();
        updateSuppression(group);
        if (!group.children.isEmpty()) {
            Iterator<NotificationData.Entry> it = ((HashSet) group.children.clone()).iterator();
            while (it.hasNext()) {
                onEntryBecomingChild(it.next());
            }
            this.mListener.onGroupCreatedFromChildren(group);
        }
    }

    private void onEntryBecomingChild(NotificationData.Entry entry) {
        if (entry.row.isHeadsUp()) {
            onHeadsUpStateChanged(entry, true);
        }
    }

    private void updateSuppression(NotificationGroup group) {
        if (group != null) {
            boolean prevSuppressed = group.suppressed;
            if (Build.VERSION.SDK_INT == 23) {
                group.suppressed = false;
            } else {
                boolean z = true;
                if (group.summary == null || group.expanded || (group.children.size() != 1 && ((!group.children.isEmpty() || !group.summary.notification.getNotification().isGroupSummary() || !hasIsolatedChildren(group)) && !hasMediaOrCustomChildren(group)))) {
                    z = false;
                }
                group.suppressed = z;
            }
            if (prevSuppressed != group.suppressed) {
                if (group.suppressed) {
                    handleSuppressedSummaryHeadsUpped(group.summary);
                }
                if (!this.mIsUpdatingUnchangedGroup) {
                    this.mListener.onGroupsChanged();
                }
            }
        }
    }

    private boolean hasMediaOrCustomChildren(NotificationGroup group) {
        if (group.children != null) {
            Iterator<NotificationData.Entry> iterator = group.children.iterator();
            while (iterator.hasNext()) {
                NotificationData.Entry entry = iterator.next();
                if (!entry.isMediaNotification()) {
                    if (entry.isCustomViewNotification()) {
                    }
                }
                return true;
            }
        }
        return false;
    }

    private boolean hasIsolatedChildren(NotificationGroup group) {
        return getNumberOfIsolatedChildren(group.summary.notification.getGroupKey()) != 0;
    }

    private int getNumberOfIsolatedChildren(String groupKey) {
        int count = 0;
        for (StatusBarNotification sbn : this.mIsolatedEntries.values()) {
            if (sbn.getGroupKey().equals(groupKey) && isIsolated(sbn)) {
                count++;
            }
        }
        return count;
    }

    private NotificationData.Entry getIsolatedChild(String groupKey) {
        for (StatusBarNotification sbn : this.mIsolatedEntries.values()) {
            if (sbn.getGroupKey().equals(groupKey) && isIsolated(sbn)) {
                return this.mGroupMap.get(sbn.getKey()).summary;
            }
        }
        return null;
    }

    public void onEntryUpdated(NotificationData.Entry entry, StatusBarNotification oldNotification) {
        String oldKey = oldNotification.getGroupKey();
        String newKey = entry.notification.getGroupKey();
        boolean z = true;
        boolean groupKeysChanged = !oldKey.equals(newKey);
        boolean wasGroupChild = isGroupChild(oldNotification);
        boolean isGroupChild = isGroupChild(entry.notification);
        if (groupKeysChanged || wasGroupChild != isGroupChild) {
            z = false;
        }
        this.mIsUpdatingUnchangedGroup = z;
        if (this.mGroupMap.get(getGroupKey(oldNotification)) != null) {
            onEntryRemovedInternal(entry, oldNotification);
        }
        onEntryAdded(entry);
        this.mIsUpdatingUnchangedGroup = false;
        if (isIsolated(entry.notification)) {
            this.mIsolatedEntries.put(entry.key, entry.notification);
            if (groupKeysChanged) {
                updateSuppression(this.mGroupMap.get(oldKey));
                updateSuppression(this.mGroupMap.get(newKey));
            }
        } else if (!wasGroupChild && isGroupChild) {
            onEntryBecomingChild(entry);
        }
    }

    public boolean isSummaryHasChildren(StatusBarNotification sbn) {
        return sbn.getNotification().isGroupSummary() && getTotalNumberOfChildren(sbn) > 0;
    }

    public boolean isSummaryOfSuppressedGroup(StatusBarNotification sbn) {
        return sbn.getNotification().isGroupSummary() && isGroupSuppressed(getGroupKey(sbn));
    }

    private boolean isOnlyChild(StatusBarNotification sbn) {
        if (sbn.getNotification().isGroupSummary() || getTotalNumberOfChildren(sbn) != 1) {
            return false;
        }
        return true;
    }

    public boolean isOnlyChildInGroup(StatusBarNotification sbn) {
        boolean z = false;
        if (!isOnlyChild(sbn)) {
            return false;
        }
        ExpandableNotificationRow logicalGroupSummary = getLogicalGroupSummary(sbn);
        if (logicalGroupSummary != null && !logicalGroupSummary.getStatusBarNotification().equals(sbn)) {
            z = true;
        }
        return z;
    }

    private int getTotalNumberOfChildren(StatusBarNotification sbn) {
        int isolatedChildren = getNumberOfIsolatedChildren(sbn.getGroupKey());
        NotificationGroup group = this.mGroupMap.get(sbn.getGroupKey());
        return isolatedChildren + (group != null ? group.children.size() : 0);
    }

    private boolean isGroupSuppressed(String groupKey) {
        NotificationGroup group = this.mGroupMap.get(groupKey);
        return group != null && group.suppressed;
    }

    public void setStatusBarState(int newState) {
        if (this.mBarState != newState) {
            this.mBarState = newState;
            if (this.mBarState == 1) {
                collapseAllGroups();
            }
        }
    }

    public void collapseAllGroups() {
        ArrayList<NotificationGroup> groupCopy = new ArrayList<>(this.mGroupMap.values());
        int size = groupCopy.size();
        for (int i = 0; i < size; i++) {
            NotificationGroup group = groupCopy.get(i);
            if (group.expanded) {
                setGroupExpanded(group, false);
            }
            updateSuppression(group);
        }
    }

    public boolean isChildInGroupWithSummary(StatusBarNotification sbn) {
        if (!isGroupChild(sbn)) {
            return false;
        }
        NotificationGroup group = this.mGroupMap.get(getGroupKey(sbn));
        if (group == null || group.summary == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT == 23) {
            return true;
        }
        if (!group.suppressed && !group.children.isEmpty()) {
            return true;
        }
        return false;
    }

    public boolean isSummaryOfGroup(StatusBarNotification sbn) {
        if (!isGroupSummary(sbn)) {
            return false;
        }
        NotificationGroup group = this.mGroupMap.get(getGroupKey(sbn));
        if (group == null) {
            return false;
        }
        return !group.children.isEmpty();
    }

    public ExpandableNotificationRow getGroupSummary(StatusBarNotification sbn) {
        return getGroupSummary(getGroupKey(sbn));
    }

    public ExpandableNotificationRow getLogicalGroupSummary(StatusBarNotification sbn) {
        return getGroupSummary(sbn.getGroupKey());
    }

    private ExpandableNotificationRow getGroupSummary(String groupKey) {
        NotificationGroup group = this.mGroupMap.get(groupKey);
        if (group == null || group.summary == null) {
            return null;
        }
        return group.summary.row;
    }

    public NotificationGroup getNotificationGroup(String groupKey) {
        return this.mGroupMap.get(groupKey);
    }

    public boolean toggleGroupExpansion(StatusBarNotification sbn) {
        NotificationGroup group = this.mGroupMap.get(getGroupKey(sbn));
        if (group == null) {
            return false;
        }
        setGroupExpanded(group, !group.expanded);
        return group.expanded;
    }

    private boolean isIsolated(StatusBarNotification sbn) {
        return this.mIsolatedEntries.containsKey(sbn.getKey());
    }

    private boolean isGroupSummary(StatusBarNotification sbn) {
        if (isIsolated(sbn)) {
            return true;
        }
        return sbn.getNotification().isGroupSummary();
    }

    private boolean isGroupChild(StatusBarNotification sbn) {
        boolean z = false;
        if (isIsolated(sbn)) {
            return false;
        }
        if (StatusBarNotificationCompat.isGroup(sbn) && !sbn.getNotification().isGroupSummary()) {
            z = true;
        }
        return z;
    }

    private String getGroupKey(StatusBarNotification sbn) {
        if (isIsolated(sbn)) {
            return sbn.getKey();
        }
        return sbn.getGroupKey();
    }

    public void onHeadsUpPinnedModeChanged(boolean inPinnedMode) {
    }

    public void onHeadsUpPinned(ExpandableNotificationRow headsUp) {
    }

    public void onHeadsUpUnPinned(ExpandableNotificationRow headsUp) {
    }

    public void onHeadsUpStateChanged(NotificationData.Entry entry, boolean isHeadsUp) {
        StatusBarNotification sbn = entry.notification;
        if (entry.row.isHeadsUp()) {
            if (shouldIsolate(sbn)) {
                onEntryRemovedInternal(entry, entry.notification);
                this.mIsolatedEntries.put(sbn.getKey(), sbn);
                onEntryAdded(entry);
                updateSuppression(this.mGroupMap.get(entry.notification.getGroupKey()));
                this.mListener.onGroupsChanged();
                return;
            }
            handleSuppressedSummaryHeadsUpped(entry);
        } else if (this.mIsolatedEntries.containsKey(sbn.getKey())) {
            onEntryRemovedInternal(entry, entry.notification);
            this.mIsolatedEntries.remove(sbn.getKey());
            onEntryAdded(entry);
            this.mListener.onGroupsChanged();
        }
    }

    private void handleSuppressedSummaryHeadsUpped(NotificationData.Entry entry) {
        StatusBarNotification sbn = entry.notification;
        if (isGroupSuppressed(sbn.getGroupKey()) && sbn.getNotification().isGroupSummary() && entry.row.isHeadsUp()) {
            NotificationGroup notificationGroup = this.mGroupMap.get(sbn.getGroupKey());
            if (notificationGroup != null) {
                Iterator<NotificationData.Entry> iterator = notificationGroup.children.iterator();
                NotificationData.Entry child = iterator.hasNext() ? iterator.next() : null;
                if (child == null) {
                    child = getIsolatedChild(sbn.getGroupKey());
                }
                if (child != null) {
                    if (!child.row.keepInParent() && !child.row.isRemoved() && !child.row.isDismissed()) {
                        if (this.mHeadsUpManager.isHeadsUp(child.key)) {
                            this.mHeadsUpManager.updateNotification(child, true);
                        } else {
                            this.mHeadsUpManager.showNotification(child);
                        }
                    } else {
                        return;
                    }
                }
            }
            this.mHeadsUpManager.releaseImmediately(entry.key);
        }
    }

    private boolean shouldIsolate(StatusBarNotification sbn) {
        NotificationGroup notificationGroup = this.mGroupMap.get(sbn.getGroupKey());
        return StatusBarNotificationCompat.isGroup(sbn) && !sbn.getNotification().isGroupSummary() && (sbn.getNotification().fullScreenIntent != null || notificationGroup == null || !notificationGroup.expanded || isGroupNotFullyVisible(notificationGroup));
    }

    private boolean isGroupNotFullyVisible(NotificationGroup notificationGroup) {
        return notificationGroup.summary == null || notificationGroup.summary.row.getClipTopAmount() > 0 || notificationGroup.summary.row.getTranslationY() < 0.0f;
    }

    public void setHeadsUpManager(HeadsUpManager headsUpManager) {
        this.mHeadsUpManager = headsUpManager;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("GroupManager state:");
        pw.println("  number of groups: " + this.mGroupMap.size());
        for (Map.Entry<String, NotificationGroup> entry : this.mGroupMap.entrySet()) {
            pw.println("\n    key: " + entry.getKey());
            pw.println(entry.getValue());
        }
        pw.println("\n    isolated entries: " + this.mIsolatedEntries.size());
        for (Map.Entry<String, StatusBarNotification> entry2 : this.mIsolatedEntries.entrySet()) {
            pw.print("      ");
            pw.print(entry2.getKey());
            pw.print(", ");
            pw.println(entry2.getValue());
        }
    }
}
