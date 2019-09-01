package com.android.systemui.statusbar;

import android.app.AppGlobals;
import android.app.Notification;
import android.app.NotificationChannelCompat;
import android.content.Context;
import android.content.pm.IPackageManager;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.SystemClock;
import android.service.notification.NotificationListenerService;
import android.service.notification.NotificationListenerServiceCompat;
import android.service.notification.RankingCompat;
import android.service.notification.SnoozeCriterion;
import android.service.notification.StatusBarNotification;
import android.service.notification.StatusBarNotificationCompat;
import android.util.ArrayMap;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RemoteViews;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.statusbar.NotificationVisibility;
import com.android.internal.statusbar.NotificationVisibilityCompat;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.internal.statusbar.StatusBarServiceCompat;
import com.android.systemui.Constants;
import com.android.systemui.Dependency;
import com.android.systemui.ForegroundServiceController;
import com.android.systemui.miui.statusbar.ExpandedNotification;
import com.android.systemui.miui.statusbar.LocalAlgoModel;
import com.android.systemui.miui.statusbar.analytics.ExposeMessage;
import com.android.systemui.miui.statusbar.notification.NotificationUtil;
import com.android.systemui.miui.statusbar.phone.rank.PackageScoreCache;
import com.android.systemui.miui.statusbar.phone.rank.RankUtil;
import com.android.systemui.statusbar.StatusBarIconView;
import com.android.systemui.statusbar.notification.InflationException;
import com.android.systemui.statusbar.phone.NotificationGroupManager;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class NotificationData {
    private IStatusBarService mBarService;
    private int mClearableCount;
    private final ArrayMap<String, Entry> mEntries = new ArrayMap<>();
    /* access modifiers changed from: private */
    public final Environment mEnvironment;
    private int mFoldCount;
    private final ArrayList<Entry> mFoldEntries = new ArrayList<>();
    /* access modifiers changed from: private */
    public NotificationGroupManager mGroupManager;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    /* access modifiers changed from: private */
    public HeadsUpManager mHeadsUpManager;
    private long mLastRankingMapUpdatedTime;
    private final Comparator<Entry> mRankingComparator = new Comparator<Entry>() {
        private final NotificationListenerService.Ranking mRankingA = new NotificationListenerService.Ranking();
        private final NotificationListenerService.Ranking mRankingB = new NotificationListenerService.Ranking();

        public int compare(Entry a, Entry b) {
            StatusBarNotification na = a.notification;
            StatusBarNotification nb = b.notification;
            int aImportance = 3;
            int bImportance = 3;
            if (NotificationData.this.mRankingMap != null) {
                NotificationData.this.mRankingMap.getRanking(a.key, this.mRankingA);
                NotificationData.this.mRankingMap.getRanking(b.key, this.mRankingB);
                aImportance = NotificationData.this.getImportance(a, this.mRankingA);
                bImportance = NotificationData.this.getImportance(b, this.mRankingB);
            }
            int compareResult = RankUtil.compareHeadsUp(a, b, NotificationData.this.mHeadsUpManager);
            if (compareResult != 0) {
                return compareResult;
            }
            int compareResult2 = RankUtil.compareHighPriority(a, b);
            if (compareResult2 != 0) {
                return compareResult2;
            }
            int compareResult3 = RankUtil.compareMedia(a, b, aImportance, bImportance, NotificationData.this.mEnvironment.getCurrentMediaNotificationKey());
            if (compareResult3 != 0) {
                return compareResult3;
            }
            int compareResult4 = RankUtil.compareNew(a, b);
            if (compareResult4 != 0) {
                return compareResult4;
            }
            if (!Constants.IS_INTERNATIONAL) {
                int compareResult5 = RankUtil.compareIM(a, b);
                if (compareResult5 != 0) {
                    return compareResult5;
                }
            }
            int compareResult6 = RankUtil.compareSystemMax(a, b, aImportance, bImportance);
            if (compareResult6 != 0) {
                return compareResult6;
            }
            int compareResult7 = RankUtil.compareImportance(aImportance, bImportance);
            if (compareResult7 != 0) {
                return compareResult7;
            }
            int compareResult8 = RankUtil.comparePriority(a, b);
            if (compareResult8 != 0) {
                return compareResult8;
            }
            return Long.compare(nb.getNotification().when, na.getNotification().when);
        }
    };
    /* access modifiers changed from: private */
    public NotificationListenerService.RankingMap mRankingMap;
    private final ArrayList<Entry> mSortedAndFiltered = new ArrayList<>();
    private final NotificationListenerService.Ranking mTmpRanking = new NotificationListenerService.Ranking();

    public static final class Entry {
        public boolean autoRedacted;
        public RemoteViews cachedAmbientContentView;
        public RemoteViews cachedBigContentView;
        public RemoteViews cachedContentView;
        public RemoteViews cachedHeadsUpContentView;
        public RemoteViews cachedPublicContentView;
        public NotificationChannelCompat channel;
        public StatusBarIconView expandedIcon;
        public long firstWhen;
        public boolean hideSensitive;
        public boolean hideSensitiveByAppLock;
        public StatusBarIconView icon;
        private boolean interruption;
        public boolean isGameModeWhenHeadsUp;
        public boolean isGroupExpandedWhenExpose;
        public boolean isHeadsUpWhenExpose;
        public boolean isKeyguardWhenExpose;
        public boolean isSeen;
        public String key;
        private long lastFullScreenIntentLaunchTime = -2000;
        private int mCachedContrastColor = 1;
        private int mCachedContrastColorIsFor = 1;
        private InflationTask mRunningTask = null;
        public List<ExposeMessage> messageList = new ArrayList();
        public boolean needUpdateBadgeNum;
        public ExpandedNotification notification;
        public CharSequence remoteInputText;
        public ExpandableNotificationRow row;
        public long seeTime;
        public List<SnoozeCriterion> snoozeCriteria;
        public int targetSdk;

        public Entry(ExpandedNotification n) {
            this.key = n.getKey();
            this.notification = n;
            this.firstWhen = n.getNotification().when;
        }

        public void setInterruption() {
            this.interruption = true;
        }

        public boolean hasInterrupted() {
            return this.interruption;
        }

        public void reset() {
            this.lastFullScreenIntentLaunchTime = -2000;
            if (this.row != null) {
                this.row.reset();
            }
        }

        public View getPrivateView() {
            return this.row.getPrivateLayout();
        }

        public View getExpandedContentView() {
            return this.row.getPrivateLayout().getExpandedChild();
        }

        public View getPublicContentView() {
            return this.row.getPublicLayout().getContractedChild();
        }

        public void notifyFullScreenIntentLaunched() {
            this.lastFullScreenIntentLaunchTime = SystemClock.elapsedRealtime();
        }

        public boolean hasJustLaunchedFullScreenIntent() {
            return SystemClock.elapsedRealtime() < this.lastFullScreenIntentLaunchTime + 2000;
        }

        public void createIcons(Context context, ExpandedNotification sbn) throws InflationException {
            Notification n = sbn.getNotification();
            Icon smallIcon = NotificationUtil.getSmallIcon(context, sbn);
            if (smallIcon != null) {
                this.icon = new StatusBarIconView(context, sbn.getPackageName() + "/0x" + Integer.toHexString(sbn.getId()), sbn);
                this.icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                this.expandedIcon = new StatusBarIconView(context, sbn.getPackageName() + "/0x" + Integer.toHexString(sbn.getId()), sbn);
                this.expandedIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                StatusBarIcon ic = new StatusBarIcon(sbn.getUser(), sbn.getPackageName(), smallIcon, n.iconLevel, n.number, StatusBarIconView.contentDescForNotification(context, n));
                if (!this.icon.set(ic) || !this.expandedIcon.set(ic)) {
                    this.icon = null;
                    this.expandedIcon = null;
                    throw new InflationException("Couldn't create icon: " + ic);
                }
                this.expandedIcon.setVisibility(4);
                this.expandedIcon.setOnVisibilityChangedListener(new StatusBarIconView.OnVisibilityChangedListener() {
                    public void onVisibilityChanged(int newVisibility) {
                        if (Entry.this.row != null) {
                            Entry.this.row.setIconsVisible(newVisibility != 0);
                        }
                    }
                });
                return;
            }
            throw new InflationException("No small icon in notification from " + sbn.getPackageName());
        }

        public void updateIcons(Context context, ExpandedNotification sbn) throws InflationException {
            if (this.icon != null) {
                Notification n = sbn.getNotification();
                StatusBarIcon ic = new StatusBarIcon(this.notification.getUser(), this.notification.getPackageName(), NotificationUtil.getSmallIcon(context, sbn), n.iconLevel, n.number, StatusBarIconView.contentDescForNotification(context, n));
                this.icon.setNotification(sbn);
                this.expandedIcon.setNotification(sbn);
                if (!this.icon.set(ic) || !this.expandedIcon.set(ic)) {
                    throw new InflationException("Couldn't update icon: " + ic);
                }
            }
        }

        public void abortTask() {
            if (this.mRunningTask != null) {
                this.mRunningTask.abort();
                this.mRunningTask = null;
            }
        }

        public void setInflationTask(InflationTask abortableTask) {
            InflationTask existing = this.mRunningTask;
            abortTask();
            this.mRunningTask = abortableTask;
            if (existing != null && this.mRunningTask != null) {
                this.mRunningTask.supersedeTask(existing);
            }
        }

        public void onInflationTaskFinished() {
            this.mRunningTask = null;
        }

        @VisibleForTesting
        public InflationTask getRunningTask() {
            return this.mRunningTask;
        }

        public boolean isMediaNotification() {
            if (this.row != null) {
                return this.row.isMediaNotification();
            }
            return NotificationUtil.isMediaNotification(this.notification);
        }

        public boolean isCustomViewNotification() {
            if (this.row != null) {
                return this.row.isCustomViewNotification();
            }
            return NotificationUtil.isCustomViewNotification(this.notification);
        }
    }

    public interface Environment {
        IStatusBarService getBarService();

        String getCurrentMediaNotificationKey();

        NotificationGroupManager getGroupManager();

        boolean isDeviceProvisioned();

        boolean isNotificationForCurrentProfiles(StatusBarNotification statusBarNotification);

        boolean isSecurelyLocked(int i);

        boolean shouldHideNotifications(int i);

        boolean shouldHideNotifications(String str);
    }

    public void setHeadsUpManager(HeadsUpManager headsUpManager) {
        this.mHeadsUpManager = headsUpManager;
    }

    public NotificationData(Environment environment) {
        this.mEnvironment = environment;
        this.mGroupManager = environment.getGroupManager();
        this.mBarService = environment.getBarService();
    }

    public ArrayList<Entry> getActiveNotifications() {
        return this.mSortedAndFiltered;
    }

    public int getClearableNotificationsCount() {
        return this.mClearableCount;
    }

    public ArrayList<Entry> getFoldEntries() {
        return this.mFoldEntries;
    }

    public ArrayList<Entry> getPkgNotifications(String pkg) {
        ArrayList<Entry> entries = new ArrayList<>();
        synchronized (this.mEntries) {
            int N = this.mEntries.size();
            for (int i = 0; i < N; i++) {
                Entry entry = this.mEntries.valueAt(i);
                if (pkg.equals(entry.notification.getPackageName())) {
                    entries.add(entry);
                }
            }
        }
        return entries;
    }

    public ArrayList<Entry> getNeedsUpdateBadgeNumNotifications() {
        Entry entry;
        HashSet<String> pkgAndUserIdSet = new HashSet<>();
        ArrayList<Entry> entries = new ArrayList<>();
        synchronized (this.mEntries) {
            int N = this.mEntries.size();
            for (int i = 0; i < N; i++) {
                String key = entry.notification.getPackageName() + "_" + this.mEntries.valueAt(i).notification.getUser().getIdentifier();
                if (!pkgAndUserIdSet.contains(key)) {
                    entries.add(entry);
                    pkgAndUserIdSet.add(key);
                }
            }
        }
        return entries;
    }

    public Entry get(String key) {
        return this.mEntries.get(key);
    }

    public void add(Entry entry) {
        checkPkgBelowFoldLimit(entry.notification);
        checkNotificationLimit(entry.notification.getPackageName());
        RankUtil.updateLastNotificationAddedTime();
        synchronized (this.mEntries) {
            this.mEntries.put(entry.notification.getKey(), entry);
        }
        this.mGroupManager.onEntryAdded(entry);
        updateRankingAndSort(this.mRankingMap);
    }

    public Entry remove(String key, NotificationListenerService.RankingMap ranking) {
        Entry removed;
        synchronized (this.mEntries) {
            removed = this.mEntries.remove(key);
        }
        if (removed == null) {
            return null;
        }
        final NotificationGroupManager.NotificationGroup group = this.mGroupManager.getNotificationGroup(removed.notification.getGroupKey());
        this.mGroupManager.onEntryRemoved(removed);
        if (this.mGroupManager.canRemove(group)) {
            this.mHandler.postDelayed(new Runnable() {
                public void run() {
                    if (NotificationData.this.mGroupManager.canRemove(group)) {
                        NotificationData.this.performRemoveNotification(group.summary.notification);
                    }
                }
            }, 200);
        }
        updateRankingAndSort(ranking);
        return removed;
    }

    public void updateRanking(NotificationListenerService.RankingMap ranking) {
        updateRankingAndSort(ranking);
    }

    public boolean updateRankingDelayed(NotificationListenerService.RankingMap rankingMap, long messageReceiveTime) {
        if (messageReceiveTime < this.mLastRankingMapUpdatedTime) {
            Log.d("NotificationData", "drop deprecated ranking update message, messageReceiveTime=" + messageReceiveTime + ",mLastRankingMapUpdatedTime=" + this.mLastRankingMapUpdatedTime);
            return false;
        }
        updateRanking(rankingMap);
        return true;
    }

    public void changeImportance(String pkg, int importance) {
        synchronized (this.mEntries) {
            int N = this.mEntries.size();
            for (int i = 0; i < N; i++) {
                Entry entry = this.mEntries.valueAt(i);
                if (pkg.equals(entry.notification.getFoldPackageName())) {
                    entry.notification.setImportance(importance);
                }
            }
        }
    }

    public boolean isAmbient(String key) {
        if (this.mRankingMap == null) {
            return false;
        }
        this.mRankingMap.getRanking(key, this.mTmpRanking);
        return this.mTmpRanking.isAmbient();
    }

    public int getVisibilityOverride(String key) {
        if (this.mRankingMap == null) {
            return -1000;
        }
        this.mRankingMap.getRanking(key, this.mTmpRanking);
        return this.mTmpRanking.getVisibilityOverride();
    }

    public boolean shouldSuppressScreenOff(String key) {
        if (this.mRankingMap == null) {
            return false;
        }
        this.mRankingMap.getRanking(key, this.mTmpRanking);
        return NotificationListenerServiceCompat.shouldSuppressScreenOff(this.mTmpRanking);
    }

    public boolean shouldSuppressScreenOn(String key) {
        if (this.mRankingMap == null) {
            return false;
        }
        this.mRankingMap.getRanking(key, this.mTmpRanking);
        return NotificationListenerServiceCompat.shouldSuppressScreenOn(this.mTmpRanking);
    }

    public int getImportance(Entry entry) {
        return getImportance(entry, (NotificationListenerService.Ranking) null);
    }

    public int getImportance(Entry entry, NotificationListenerService.Ranking ranking) {
        if (Build.VERSION.SDK_INT > 23) {
            return getImportance(entry.notification.getKey(), ranking);
        }
        return NotificationListenerServiceCompat.getImportance(entry.notification.getNotification());
    }

    private int getImportance(String key, NotificationListenerService.Ranking ranking) {
        if (ranking != null) {
            return NotificationListenerServiceCompat.getImportance(ranking);
        }
        if (this.mRankingMap == null) {
            return -1000;
        }
        this.mRankingMap.getRanking(key, this.mTmpRanking);
        return NotificationListenerServiceCompat.getImportance(this.mTmpRanking);
    }

    public String getOverrideGroupKey(String key) {
        if (this.mRankingMap == null) {
            return null;
        }
        this.mRankingMap.getRanking(key, this.mTmpRanking);
        return NotificationListenerServiceCompat.getOverrideGroupKey(this.mTmpRanking);
    }

    public List<SnoozeCriterion> getSnoozeCriteria(String key) {
        if (this.mRankingMap == null) {
            return null;
        }
        this.mRankingMap.getRanking(key, this.mTmpRanking);
        return RankingCompat.getSnoozeCriteria(this.mTmpRanking);
    }

    public NotificationChannelCompat getChannel(String key) {
        if (this.mRankingMap == null) {
            return new NotificationChannelCompat("miscellaneous", "Default", -1000);
        }
        this.mRankingMap.getRanking(key, this.mTmpRanking);
        return NotificationChannelCompat.getChannel(this.mTmpRanking);
    }

    public int getRank(String key) {
        if (this.mRankingMap == null) {
            return 0;
        }
        this.mRankingMap.getRanking(key, this.mTmpRanking);
        return this.mTmpRanking.getRank();
    }

    private void updateRankingAndSort(NotificationListenerService.RankingMap ranking) {
        if (ranking != null) {
            this.mRankingMap = ranking;
            this.mLastRankingMapUpdatedTime = SystemClock.uptimeMillis();
            synchronized (this.mEntries) {
                int N = this.mEntries.size();
                for (int i = 0; i < N; i++) {
                    Entry entry = this.mEntries.valueAt(i);
                    StatusBarNotification oldSbn = entry.notification.cloneLight();
                    String overrideGroupKey = getOverrideGroupKey(entry.key);
                    if (!Objects.equals(StatusBarNotificationCompat.getOverrideGroupKey(oldSbn), overrideGroupKey)) {
                        StatusBarNotificationCompat.setOverrideGroupKey(entry.notification, overrideGroupKey);
                        this.mGroupManager.onEntryUpdated(entry, oldSbn);
                    }
                    entry.channel = getChannel(entry.key);
                    entry.snoozeCriteria = getSnoozeCriteria(entry.key);
                }
            }
        }
        filterAndSort();
    }

    public void filterAndSort() {
        this.mSortedAndFiltered.clear();
        this.mFoldEntries.clear();
        this.mFoldCount = 0;
        this.mClearableCount = 0;
        synchronized (this.mEntries) {
            int N = this.mEntries.size();
            for (int i = 0; i < N; i++) {
                Entry entry = this.mEntries.valueAt(i);
                ExpandedNotification sbn = entry.notification;
                if (!shouldFilterOut(sbn)) {
                    if (!filterFold(entry)) {
                        if (sbn.isClearable()) {
                            this.mClearableCount++;
                        }
                        this.mSortedAndFiltered.add(entry);
                    }
                }
            }
        }
        Collections.sort(this.mSortedAndFiltered, this.mRankingComparator);
    }

    public int getFoldCount() {
        return this.mFoldCount;
    }

    public boolean shouldFilterOut(ExpandedNotification sbn) {
        if ((!this.mEnvironment.isDeviceProvisioned() && !showNotificationEvenIfUnprovisioned(sbn)) || !this.mEnvironment.isNotificationForCurrentProfiles(sbn)) {
            return true;
        }
        if (this.mEnvironment.isSecurelyLocked(sbn.getUserId()) && (sbn.getNotification().visibility == -1 || this.mEnvironment.shouldHideNotifications(sbn.getUserId()) || this.mEnvironment.shouldHideNotifications(sbn.getKey()))) {
            return true;
        }
        if (!StatusBar.ENABLE_CHILD_NOTIFICATIONS && this.mGroupManager.isChildInGroupWithSummary(sbn)) {
            return true;
        }
        ForegroundServiceController fsc = (ForegroundServiceController) Dependency.get(ForegroundServiceController.class);
        if (!fsc.isDungeonNotification(sbn) || fsc.isDungeonNeededForUser(sbn.getUserId())) {
            return false;
        }
        return true;
    }

    private boolean filterFold(Entry entry) {
        ExpandedNotification sbn = entry.notification;
        boolean z = false;
        if (!NotificationUtil.isUserFold()) {
            return false;
        }
        if (sbn.isFold()) {
            this.mFoldCount++;
            this.mFoldEntries.add(entry);
            this.mHeadsUpManager.removeNotification(entry.key, true);
        }
        if (NotificationUtil.isFold() != sbn.isFold()) {
            z = true;
        }
        return z;
    }

    public static boolean showNotificationEvenIfUnprovisioned(StatusBarNotification sbn) {
        return showNotificationEvenIfUnprovisioned(AppGlobals.getPackageManager(), sbn);
    }

    @VisibleForTesting
    static boolean showNotificationEvenIfUnprovisioned(IPackageManager packageManager, StatusBarNotification sbn) {
        return ("android".equals(sbn.getPackageName()) || checkUidPermission(packageManager, "android.permission.NOTIFICATION_DURING_SETUP", sbn.getUid()) == 0) && sbn.getNotification().extras.getBoolean("android.allowDuringSetup");
    }

    private static int checkUidPermission(IPackageManager packageManager, String permission, int uid) {
        try {
            return packageManager.checkUidPermission(permission, uid);
        } catch (RemoteException e) {
            return -1;
        }
    }

    public void dump(PrintWriter pw, String indent) {
        int N = this.mSortedAndFiltered.size();
        pw.print(indent);
        pw.println("active notifications: " + N + " threshold: " + LocalAlgoModel.getThreshold());
        for (int active = 0; active < N; active++) {
            dumpEntry(pw, indent, active, this.mSortedAndFiltered.get(active));
        }
        synchronized (this.mEntries) {
            int M = this.mEntries.size();
            pw.print(indent);
            pw.println("inactive notifications: " + (M - active));
            int inactiveCount = 0;
            for (int i = 0; i < M; i++) {
                Entry entry = this.mEntries.valueAt(i);
                if (!this.mSortedAndFiltered.contains(entry)) {
                    dumpEntry(pw, indent, inactiveCount, entry);
                    inactiveCount++;
                }
            }
        }
    }

    private void dumpEntry(PrintWriter pw, String indent, int i, Entry e) {
        this.mRankingMap.getRanking(e.key, this.mTmpRanking);
        pw.print(indent);
        pw.println("  [" + i + "] key=" + e.key + " icon=" + e.icon);
        ExpandedNotification n = e.notification;
        pw.print(indent);
        pw.println("      pkg=" + n.getPackageName() + " id=" + n.getId() + " score=" + n.getScore() + " pushScore=" + n.getPushScore() + " localscore=" + n.getLocalScore() + " importance=" + n.getImportance() + " fold=" + n.isFold() + " showtimes=" + n.getShowSum() + " newly=" + n.isNewlyNotification() + " hasShown=" + n.hasShownAfterUnlock() + " isEnableFloat=" + n.getNotification().extraNotification.isEnableFloat() + " isEnableKeyguard=" + n.getNotification().extraNotification.isEnableKeyguard() + " messageCount=" + n.getNotification().extraNotification.getMessageCount() + " targetPkg=" + n.getNotification().extraNotification.getTargetPkg());
    }

    private void checkPkgBelowFoldLimit(ExpandedNotification notification) {
        String pkgName = notification.getFoldPackageName();
        PackageScoreCache packageScoreCache = (PackageScoreCache) Dependency.get(PackageScoreCache.class);
        notification.setNewlyNotification(!packageScoreCache.containsPkg(pkgName));
        notification.setShowSum(packageScoreCache.addShow(pkgName).getTotalShow());
    }

    private void checkNotificationLimit(String pkg) {
        int count = 0;
        Entry removeEntry = null;
        synchronized (this.mEntries) {
            int M = this.mEntries.size();
            for (int i = 0; i < M; i++) {
                Entry entry = this.mEntries.valueAt(i);
                if (entry.notification.getPackageName().equals(pkg)) {
                    count++;
                    removeEntry = shouldRemove(removeEntry, entry);
                }
            }
        }
        if (count >= 10 && removeEntry != null) {
            performRemoveNotification(removeEntry.notification);
        }
    }

    public void performRemoveNotification(ExpandedNotification n) {
        NotificationVisibility nv = NotificationVisibilityCompat.obtain(n.getKey(), getRank(n.getKey()), getActiveNotifications().size(), true);
        int dismissalSurface = 3;
        try {
            if (this.mHeadsUpManager.isHeadsUp(n.getKey())) {
                dismissalSurface = 1;
            }
            StatusBarServiceCompat.onNotificationClear(this.mBarService, n.getBasePkg(), n.getTag(), n.getId(), n.getUserId(), n.getKey(), dismissalSurface, nv);
        } catch (Exception e) {
        }
    }

    private Entry shouldRemove(Entry entry1, Entry entry2) {
        if (entry1 == null) {
            return entry2;
        }
        if (entry2 == null) {
            return entry1;
        }
        boolean isFold1 = entry1.notification.isFold();
        boolean isFold2 = entry2.notification.isFold();
        if (isFold1 != isFold2) {
            if (isFold1) {
                return entry1;
            }
            if (isFold2) {
                return entry2;
            }
        }
        boolean isGroupSummary1 = entry1.notification.getNotification().isGroupSummary();
        boolean isGroupSummary2 = entry2.notification.getNotification().isGroupSummary();
        if (isGroupSummary1 != isGroupSummary2) {
            if (isGroupSummary1) {
                return entry2;
            }
            if (isGroupSummary2) {
                return entry1;
            }
        }
        return entry1.firstWhen < entry2.firstWhen ? entry1 : entry2;
    }
}
