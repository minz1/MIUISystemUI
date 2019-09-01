package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.Pools;
import android.view.View;
import android.view.ViewTreeObserver;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.Constants;
import com.android.systemui.DisplayCutoutCompat;
import com.android.systemui.R;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.notification.VisualStabilityManager;
import com.android.systemui.statusbar.phone.NotificationGroupManager;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.policy.ConfigurationController;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

public class HeadsUpManager implements ViewTreeObserver.OnComputeInternalInsetsListener, VisualStabilityManager.Callback, ConfigurationController.ConfigurationListener {
    /* access modifiers changed from: private */
    public static final boolean DEBUG = Constants.DEBUG;
    private StatusBar mBar;
    /* access modifiers changed from: private */
    public Clock mClock;
    private final Context mContext;
    private final int mDefaultSnoozeLengthMs;
    private int mDisplayCutoutTouchableRegionSize;
    /* access modifiers changed from: private */
    public HashSet<NotificationData.Entry> mEntriesToRemoveAfterExpand = new HashSet<>();
    /* access modifiers changed from: private */
    public ArraySet<NotificationData.Entry> mEntriesToRemoveWhenReorderingAllowed = new ArraySet<>();
    private final Pools.Pool<HeadsUpEntry> mEntryPool = new Pools.Pool<HeadsUpEntry>() {
        private Stack<HeadsUpEntry> mPoolObjects = new Stack<>();

        public HeadsUpEntry acquire() {
            if (!this.mPoolObjects.isEmpty()) {
                return this.mPoolObjects.pop();
            }
            return new HeadsUpEntry();
        }

        public boolean release(HeadsUpEntry instance) {
            instance.reset();
            this.mPoolObjects.push(instance);
            return true;
        }
    };
    private final NotificationGroupManager mGroupManager;
    /* access modifiers changed from: private */
    public final Handler mHandler = new Handler();
    private boolean mHasPinnedNotification;
    private HashMap<String, HeadsUpEntry> mHeadsUpEntries = new HashMap<>();
    private boolean mHeadsUpGoingAway;
    /* access modifiers changed from: private */
    public final int mHeadsUpNotificationDecay;
    private boolean mIsExpanded;
    private boolean mIsObserving;
    private final HashSet<OnHeadsUpChangedListener> mListeners = new HashSet<>();
    /* access modifiers changed from: private */
    public final int mMinimumDisplayTime;
    private boolean mReleaseOnExpandFinish;
    private ContentObserver mSettingsObserver;
    /* access modifiers changed from: private */
    public int mSnoozeLengthMs;
    private final ArrayMap<String, Long> mSnoozedPackages;
    /* access modifiers changed from: private */
    public int mStatusBarHeight;
    private int mStatusBarState;
    /* access modifiers changed from: private */
    public final View mStatusBarWindowView;
    private HashSet<String> mSwipedOutKeys = new HashSet<>();
    private int[] mTmpTwoArray = new int[2];
    /* access modifiers changed from: private */
    public final int mTouchAcceptanceDelay;
    /* access modifiers changed from: private */
    public boolean mTrackingHeadsUp;
    private int mUser;
    /* access modifiers changed from: private */
    public VisualStabilityManager mVisualStabilityManager;
    /* access modifiers changed from: private */
    public boolean mWaitingOnCollapseWhenGoingAway;

    public static class Clock {
        public long currentTimeMillis() {
            return SystemClock.elapsedRealtime();
        }
    }

    public class HeadsUpEntry implements Comparable<HeadsUpEntry> {
        public long earliestRemovaltime;
        public NotificationData.Entry entry;
        public boolean expanded;
        private Runnable mRemoveHeadsUpRunnable;
        public long postTime;
        public boolean remoteInputActive;

        public HeadsUpEntry() {
        }

        public void setEntry(final NotificationData.Entry entry2) {
            this.entry = entry2;
            this.postTime = HeadsUpManager.this.mClock.currentTimeMillis() + ((long) HeadsUpManager.this.mTouchAcceptanceDelay);
            this.mRemoveHeadsUpRunnable = new Runnable() {
                public void run() {
                    if (!HeadsUpManager.this.mVisualStabilityManager.isReorderingAllowed()) {
                        HeadsUpManager.this.mEntriesToRemoveWhenReorderingAllowed.add(entry2);
                        HeadsUpManager.this.mVisualStabilityManager.addReorderingAllowedCallback(HeadsUpManager.this);
                    } else if (!HeadsUpManager.this.mTrackingHeadsUp) {
                        HeadsUpManager.this.removeHeadsUpEntry(entry2);
                    } else {
                        HeadsUpManager.this.mEntriesToRemoveAfterExpand.add(entry2);
                    }
                }
            };
            updateEntry();
        }

        public void updateEntry() {
            updateEntry(true);
        }

        public void updateEntry(boolean updatePostTime) {
            long currentTime = HeadsUpManager.this.mClock.currentTimeMillis();
            this.earliestRemovaltime = ((long) HeadsUpManager.this.mMinimumDisplayTime) + currentTime;
            if (updatePostTime) {
                this.postTime = Math.max(this.postTime, currentTime);
            }
            removeAutoRemovalCallbacks();
            if (HeadsUpManager.this.mEntriesToRemoveAfterExpand.contains(this.entry)) {
                HeadsUpManager.this.mEntriesToRemoveAfterExpand.remove(this.entry);
            }
            if (HeadsUpManager.this.mEntriesToRemoveWhenReorderingAllowed.contains(this.entry)) {
                HeadsUpManager.this.mEntriesToRemoveWhenReorderingAllowed.remove(this.entry);
            }
            if (!isSticky()) {
                int floatTime = this.entry.notification.getNotification().extraNotification.getFloatTime();
                HeadsUpManager.this.mHandler.postDelayed(this.mRemoveHeadsUpRunnable, Math.max((this.postTime + ((long) (floatTime > 0 ? floatTime : HeadsUpManager.this.mHeadsUpNotificationDecay))) - currentTime, (long) HeadsUpManager.this.mMinimumDisplayTime));
            }
        }

        private boolean isSticky() {
            return (this.entry.row.isPinned() && this.expanded) || this.remoteInputActive || HeadsUpManager.this.hasFullScreenIntent(this.entry);
        }

        public int compareTo(HeadsUpEntry o) {
            boolean isPinned = this.entry.row.isPinned();
            boolean otherPinned = o.entry.row.isPinned();
            int i = -1;
            if (isPinned && !otherPinned) {
                return -1;
            }
            if (!isPinned && otherPinned) {
                return 1;
            }
            boolean selfFullscreen = HeadsUpManager.this.hasFullScreenIntent(this.entry);
            boolean otherFullscreen = HeadsUpManager.this.hasFullScreenIntent(o.entry);
            if (selfFullscreen && !otherFullscreen) {
                return -1;
            }
            if (!selfFullscreen && otherFullscreen) {
                return 1;
            }
            if (this.remoteInputActive && !o.remoteInputActive) {
                return -1;
            }
            if (!this.remoteInputActive && o.remoteInputActive) {
                return 1;
            }
            if (this.postTime < o.postTime) {
                i = 1;
            } else if (this.postTime == o.postTime) {
                i = this.entry.key.compareTo(o.entry.key);
            }
            return i;
        }

        public void removeAutoRemovalCallbacks() {
            HeadsUpManager.this.mHandler.removeCallbacks(this.mRemoveHeadsUpRunnable);
        }

        public boolean wasShownLongEnough() {
            return this.earliestRemovaltime < HeadsUpManager.this.mClock.currentTimeMillis();
        }

        public void removeAsSoonAsPossible() {
            removeAutoRemovalCallbacks();
            HeadsUpManager.this.mHandler.postDelayed(this.mRemoveHeadsUpRunnable, this.earliestRemovaltime - HeadsUpManager.this.mClock.currentTimeMillis());
        }

        public void reset() {
            removeAutoRemovalCallbacks();
            this.entry = null;
            this.mRemoveHeadsUpRunnable = null;
            this.expanded = false;
            this.remoteInputActive = false;
        }
    }

    public HeadsUpManager(final Context context, View statusBarWindowView, NotificationGroupManager groupManager) {
        this.mContext = context;
        Resources resources = this.mContext.getResources();
        this.mTouchAcceptanceDelay = resources.getInteger(R.integer.touch_acceptance_delay);
        this.mSnoozedPackages = new ArrayMap<>();
        this.mDefaultSnoozeLengthMs = resources.getInteger(R.integer.heads_up_default_snooze_length_ms);
        this.mSnoozeLengthMs = this.mDefaultSnoozeLengthMs;
        this.mMinimumDisplayTime = resources.getInteger(R.integer.heads_up_notification_minimum_time);
        this.mHeadsUpNotificationDecay = resources.getInteger(R.integer.heads_up_notification_decay);
        this.mClock = new Clock();
        this.mSnoozeLengthMs = Settings.Global.getInt(context.getContentResolver(), "heads_up_snooze_length_ms", this.mDefaultSnoozeLengthMs);
        this.mSettingsObserver = new ContentObserver(this.mHandler) {
            public void onChange(boolean selfChange) {
                int packageSnoozeLengthMs = Settings.Global.getInt(context.getContentResolver(), "heads_up_snooze_length_ms", -1);
                if (packageSnoozeLengthMs > -1 && packageSnoozeLengthMs != HeadsUpManager.this.mSnoozeLengthMs) {
                    int unused = HeadsUpManager.this.mSnoozeLengthMs = packageSnoozeLengthMs;
                    if (HeadsUpManager.DEBUG) {
                        Log.v("HeadsUpManager", "mSnoozeLengthMs = " + HeadsUpManager.this.mSnoozeLengthMs);
                    }
                }
            }
        };
        context.getContentResolver().registerContentObserver(Settings.Global.getUriFor("heads_up_snooze_length_ms"), false, this.mSettingsObserver);
        this.mStatusBarWindowView = statusBarWindowView;
        this.mGroupManager = groupManager;
        initResources();
    }

    /* access modifiers changed from: private */
    public void updateTouchableRegionListener() {
        boolean shouldObserve = this.mHasPinnedNotification || this.mHeadsUpGoingAway || this.mWaitingOnCollapseWhenGoingAway || DisplayCutoutCompat.hasCutout(this.mStatusBarWindowView);
        if (shouldObserve != this.mIsObserving) {
            if (shouldObserve) {
                this.mStatusBarWindowView.getViewTreeObserver().addOnComputeInternalInsetsListener(this);
                this.mStatusBarWindowView.requestLayout();
            } else {
                this.mStatusBarWindowView.getViewTreeObserver().removeOnComputeInternalInsetsListener(this);
            }
            this.mIsObserving = shouldObserve;
        }
    }

    public void setBar(StatusBar bar) {
        this.mBar = bar;
    }

    public void addListener(OnHeadsUpChangedListener listener) {
        this.mListeners.add(listener);
    }

    public void removeListener(OnHeadsUpChangedListener listener) {
        this.mListeners.remove(listener);
    }

    public void showNotification(NotificationData.Entry headsUp) {
        if (DEBUG) {
            Log.v("HeadsUpManager", "showNotification");
        }
        addHeadsUpEntry(headsUp);
        updateNotification(headsUp, true);
        removeOldHeadsUpNotification();
        headsUp.setInterruption();
    }

    private void removeOldHeadsUpNotification() {
        if (!this.mHeadsUpEntries.isEmpty()) {
            HeadsUpEntry topEntry = getTopEntry();
            List<HeadsUpEntry> toRemove = new ArrayList<>();
            for (HeadsUpEntry entry : this.mHeadsUpEntries.values()) {
                if (entry != topEntry) {
                    toRemove.add(entry);
                }
            }
            for (HeadsUpEntry entry2 : toRemove) {
                removeNotification(entry2.entry.key, true);
            }
        }
    }

    public void updateNotification(NotificationData.Entry headsUp, boolean alert) {
        if (DEBUG) {
            Log.v("HeadsUpManager", "updateNotification");
        }
        headsUp.row.sendAccessibilityEvent(2048);
        if (alert) {
            HeadsUpEntry headsUpEntry = this.mHeadsUpEntries.get(headsUp.key);
            if (headsUpEntry != null) {
                headsUpEntry.updateEntry();
                setEntryPinned(headsUpEntry, shouldHeadsUpBecomePinned(headsUp));
            }
        }
    }

    private void addHeadsUpEntry(NotificationData.Entry entry) {
        HeadsUpEntry headsUpEntry = (HeadsUpEntry) this.mEntryPool.acquire();
        headsUpEntry.setEntry(entry);
        this.mHeadsUpEntries.put(entry.key, headsUpEntry);
        entry.row.setHeadsUp(true);
        setEntryPinned(headsUpEntry, shouldHeadsUpBecomePinned(entry));
        Iterator<OnHeadsUpChangedListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onHeadsUpStateChanged(entry, true);
        }
        entry.row.sendAccessibilityEvent(2048);
    }

    private boolean shouldHeadsUpBecomePinned(NotificationData.Entry entry) {
        if ((this.mStatusBarState == 1 || this.mIsExpanded) && !hasFullScreenIntent(entry)) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: private */
    public boolean hasFullScreenIntent(NotificationData.Entry entry) {
        return entry.notification.getNotification().fullScreenIntent != null;
    }

    private void setEntryPinned(HeadsUpEntry headsUpEntry, boolean isPinned) {
        ExpandableNotificationRow row = headsUpEntry.entry.row;
        if (row.isPinned() != isPinned) {
            row.setPinned(isPinned);
            updatePinnedMode();
            Iterator<OnHeadsUpChangedListener> it = this.mListeners.iterator();
            while (it.hasNext()) {
                OnHeadsUpChangedListener listener = it.next();
                if (isPinned) {
                    listener.onHeadsUpPinned(row);
                } else {
                    listener.onHeadsUpUnPinned(row);
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void removeHeadsUpEntry(NotificationData.Entry entry) {
        HeadsUpEntry remove = this.mHeadsUpEntries.remove(entry.key);
        entry.row.sendAccessibilityEvent(2048);
        entry.row.setHeadsUp(false);
        setEntryPinned(remove, false);
        Iterator<OnHeadsUpChangedListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onHeadsUpStateChanged(entry, false);
        }
        this.mEntryPool.release(remove);
    }

    private void updatePinnedMode() {
        boolean hasPinnedNotification = hasPinnedNotificationInternal();
        if (hasPinnedNotification != this.mHasPinnedNotification) {
            this.mHasPinnedNotification = hasPinnedNotification;
            if (this.mHasPinnedNotification) {
                MetricsLogger.count(this.mContext, "note_peek", 1);
            }
            updateTouchableRegionListener();
            Iterator<OnHeadsUpChangedListener> it = this.mListeners.iterator();
            while (it.hasNext()) {
                it.next().onHeadsUpPinnedModeChanged(hasPinnedNotification);
            }
        }
    }

    public void removeHeadsUpNotification() {
        HeadsUpEntry topEntry = getTopEntry();
        if (topEntry == null || !topEntry.entry.row.isHeadsUp() || !topEntry.entry.row.isPinned()) {
            Log.w("HeadsUpManager", "removeHeadsUpNotification() no heads up notification on show");
        } else {
            removeNotification(topEntry.entry.key, true);
        }
    }

    public boolean removeNotification(String key, boolean ignoreEarliestRemovalTime) {
        if (DEBUG) {
            Log.v("HeadsUpManager", "remove");
        }
        HeadsUpEntry headsUpEntry = getHeadsUpEntry(key);
        if (headsUpEntry == null) {
            return true;
        }
        if (wasShownLongEnough(key) || ignoreEarliestRemovalTime) {
            releaseImmediately(key);
            return true;
        }
        headsUpEntry.removeAsSoonAsPossible();
        return false;
    }

    private boolean wasShownLongEnough(String key) {
        HeadsUpEntry headsUpEntry = getHeadsUpEntry(key);
        HeadsUpEntry topEntry = getTopEntry();
        if (this.mSwipedOutKeys.contains(key)) {
            this.mSwipedOutKeys.remove(key);
            return true;
        } else if (headsUpEntry != topEntry) {
            return true;
        } else {
            return headsUpEntry.wasShownLongEnough();
        }
    }

    public boolean isHeadsUp(String key) {
        return this.mHeadsUpEntries.containsKey(key);
    }

    public void releaseAllImmediately() {
        if (DEBUG) {
            Log.v("HeadsUpManager", "releaseAllImmediately");
        }
        Iterator<String> it = new ArrayList<>(this.mHeadsUpEntries.keySet()).iterator();
        while (it.hasNext()) {
            releaseImmediately(it.next());
        }
    }

    public void releaseImmediately(String key) {
        HeadsUpEntry headsUpEntry = getHeadsUpEntry(key);
        if (headsUpEntry != null) {
            removeHeadsUpEntry(headsUpEntry.entry);
        }
    }

    public boolean isSnoozed(String packageName) {
        String key = snoozeKey(packageName, this.mUser);
        Long snoozedUntil = this.mSnoozedPackages.get(key);
        if (snoozedUntil != null) {
            if (snoozedUntil.longValue() > SystemClock.elapsedRealtime()) {
                if (DEBUG) {
                    Log.v("HeadsUpManager", key + " snoozed");
                }
                return true;
            }
            this.mSnoozedPackages.remove(packageName);
        }
        return false;
    }

    public void snooze() {
        for (String key : this.mHeadsUpEntries.keySet()) {
            this.mSnoozedPackages.put(snoozeKey(this.mHeadsUpEntries.get(key).entry.notification.getPackageName(), this.mUser), Long.valueOf(SystemClock.elapsedRealtime() + ((long) this.mSnoozeLengthMs)));
        }
        this.mReleaseOnExpandFinish = true;
    }

    private static String snoozeKey(String packageName, int user) {
        return user + "," + packageName;
    }

    private HeadsUpEntry getHeadsUpEntry(String key) {
        return this.mHeadsUpEntries.get(key);
    }

    public NotificationData.Entry getEntry(String key) {
        return this.mHeadsUpEntries.get(key).entry;
    }

    public Collection<HeadsUpEntry> getAllEntries() {
        return this.mHeadsUpEntries.values();
    }

    public HeadsUpEntry getTopEntry() {
        if (this.mHeadsUpEntries.isEmpty()) {
            return null;
        }
        HeadsUpEntry topEntry = null;
        for (HeadsUpEntry entry : this.mHeadsUpEntries.values()) {
            if (topEntry == null || entry.compareTo(topEntry) == -1) {
                topEntry = entry;
            }
        }
        return topEntry;
    }

    private void initResources() {
        Resources resources = this.mContext.getResources();
        this.mStatusBarHeight = resources.getDimensionPixelSize(17105351);
        this.mDisplayCutoutTouchableRegionSize = resources.getDimensionPixelSize(R.dimen.display_cutout_touchable_region_size);
    }

    public void onConfigChanged(Configuration newConfig) {
    }

    public void onDensityOrFontScaleChanged() {
        initResources();
    }

    public boolean shouldSwallowClick(String key) {
        HeadsUpEntry entry = this.mHeadsUpEntries.get(key);
        if (entry == null || this.mClock.currentTimeMillis() >= entry.postTime) {
            return false;
        }
        return true;
    }

    public void onComputeInternalInsets(ViewTreeObserver.InternalInsetsInfo info) {
        if (!this.mIsExpanded && !this.mBar.isBouncerShowing()) {
            if (this.mHasPinnedNotification) {
                ExpandableNotificationRow topEntry = getTopEntry().entry.row;
                if (topEntry.isChildInGroup()) {
                    ExpandableNotificationRow groupSummary = this.mGroupManager.getGroupSummary((StatusBarNotification) topEntry.getStatusBarNotification());
                    if (groupSummary != null) {
                        topEntry = groupSummary;
                    }
                }
                topEntry.getLocationOnScreen(this.mTmpTwoArray);
                int maxX = this.mTmpTwoArray[0] + topEntry.getWidth();
                int maxY = topEntry.getIntrinsicHeight() + ((int) topEntry.getTranslationY());
                info.setTouchableInsets(3);
                info.touchableRegion.set(this.mTmpTwoArray[0], (int) topEntry.getTranslationY(), maxX, maxY);
            } else {
                setCollapsedTouchableInsets(info);
            }
        }
    }

    private void setCollapsedTouchableInsets(ViewTreeObserver.InternalInsetsInfo info) {
        info.setTouchableInsets(3);
        info.touchableRegion.set(0, 0, this.mStatusBarWindowView.getWidth(), this.mStatusBarHeight);
        updateRegionForNotch(info.touchableRegion);
    }

    private void updateRegionForNotch(Region region) {
        Rect rect = new Rect();
        DisplayCutoutCompat.boundsFromDirection(this.mStatusBarWindowView, 48, rect);
        if (!rect.isEmpty()) {
            rect.offset(0, this.mDisplayCutoutTouchableRegionSize);
            region.op(rect, Region.Op.UNION);
        }
    }

    public void setUser(int user) {
        this.mUser = user;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("HeadsUpManager state:");
        pw.print("  mTouchAcceptanceDelay=");
        pw.println(this.mTouchAcceptanceDelay);
        pw.print("  mSnoozeLengthMs=");
        pw.println(this.mSnoozeLengthMs);
        pw.print("  now=");
        pw.println(SystemClock.elapsedRealtime());
        pw.print("  mUser=");
        pw.println(this.mUser);
        for (HeadsUpEntry entry : this.mHeadsUpEntries.values()) {
            pw.print("  HeadsUpEntry=");
            pw.println(entry.entry);
        }
        int N = this.mSnoozedPackages.size();
        pw.println("  snoozed packages: " + N);
        for (int i = 0; i < N; i++) {
            pw.print("    ");
            pw.print(this.mSnoozedPackages.valueAt(i));
            pw.print(", ");
            pw.println(this.mSnoozedPackages.keyAt(i));
        }
    }

    public boolean hasPinnedHeadsUp() {
        return this.mHasPinnedNotification;
    }

    private boolean hasPinnedNotificationInternal() {
        for (String key : this.mHeadsUpEntries.keySet()) {
            if (this.mHeadsUpEntries.get(key).entry.row.isPinned()) {
                return true;
            }
        }
        return false;
    }

    public void addSwipedOutNotification(String key) {
        this.mSwipedOutKeys.add(key);
    }

    public void unpinAll() {
        for (String key : this.mHeadsUpEntries.keySet()) {
            HeadsUpEntry entry = this.mHeadsUpEntries.get(key);
            setEntryPinned(entry, false);
            entry.updateEntry(false);
        }
    }

    public void onExpandingFinished() {
        if (this.mReleaseOnExpandFinish) {
            releaseAllImmediately();
            this.mReleaseOnExpandFinish = false;
        } else {
            Iterator<NotificationData.Entry> it = this.mEntriesToRemoveAfterExpand.iterator();
            while (it.hasNext()) {
                NotificationData.Entry entry = it.next();
                if (isHeadsUp(entry.key)) {
                    removeHeadsUpEntry(entry);
                }
            }
        }
        this.mEntriesToRemoveAfterExpand.clear();
    }

    public void setTrackingHeadsUp(boolean trackingHeadsUp) {
        this.mTrackingHeadsUp = trackingHeadsUp;
    }

    public boolean isTrackingHeadsUp() {
        return this.mTrackingHeadsUp;
    }

    public void setIsExpanded(boolean isExpanded) {
        if (isExpanded != this.mIsExpanded) {
            this.mIsExpanded = isExpanded;
            if (isExpanded) {
                this.mWaitingOnCollapseWhenGoingAway = false;
                this.mHeadsUpGoingAway = false;
                updateTouchableRegionListener();
            }
        }
    }

    public int getTopHeadsUpPinnedHeight() {
        HeadsUpEntry topEntry = getTopEntry();
        if (topEntry == null || topEntry.entry == null) {
            return 0;
        }
        ExpandableNotificationRow row = topEntry.entry.row;
        if (row.isChildInGroup()) {
            ExpandableNotificationRow groupSummary = this.mGroupManager.getGroupSummary((StatusBarNotification) row.getStatusBarNotification());
            if (groupSummary != null) {
                row = groupSummary;
            }
        }
        return row.getPinnedHeadsUpHeight();
    }

    public int compare(NotificationData.Entry a, NotificationData.Entry b) {
        HeadsUpEntry aEntry = getHeadsUpEntry(a.key);
        HeadsUpEntry bEntry = getHeadsUpEntry(b.key);
        if (aEntry != null && bEntry != null) {
            return aEntry.compareTo(bEntry);
        }
        return aEntry == null ? 1 : -1;
    }

    public void setHeadsUpGoingAway(boolean headsUpGoingAway) {
        if (headsUpGoingAway != this.mHeadsUpGoingAway) {
            this.mHeadsUpGoingAway = headsUpGoingAway;
            if (!headsUpGoingAway) {
                waitForStatusBarLayout();
            }
            updateTouchableRegionListener();
        }
    }

    public boolean isHeadsUpGoingAway() {
        return this.mHeadsUpGoingAway;
    }

    private void waitForStatusBarLayout() {
        this.mWaitingOnCollapseWhenGoingAway = true;
        this.mStatusBarWindowView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (HeadsUpManager.this.mStatusBarWindowView.getHeight() <= HeadsUpManager.this.mStatusBarHeight) {
                    HeadsUpManager.this.mStatusBarWindowView.removeOnLayoutChangeListener(this);
                    boolean unused = HeadsUpManager.this.mWaitingOnCollapseWhenGoingAway = false;
                    HeadsUpManager.this.updateTouchableRegionListener();
                }
            }
        });
    }

    public static void setIsClickedNotification(View child, boolean clicked) {
        child.setTag(R.id.is_clicked_heads_up_tag, clicked ? true : null);
    }

    public static boolean isClickedHeadsUpNotification(View child) {
        Boolean clicked = (Boolean) child.getTag(R.id.is_clicked_heads_up_tag);
        return clicked != null && clicked.booleanValue();
    }

    public void setRemoteInputActive(NotificationData.Entry entry, boolean remoteInputActive) {
        HeadsUpEntry headsUpEntry = this.mHeadsUpEntries.get(entry.key);
        if (headsUpEntry != null && headsUpEntry.remoteInputActive != remoteInputActive) {
            headsUpEntry.remoteInputActive = remoteInputActive;
            if (remoteInputActive) {
                headsUpEntry.removeAutoRemovalCallbacks();
            } else {
                headsUpEntry.updateEntry(false);
            }
        }
    }

    public void setExpanded(NotificationData.Entry entry, boolean expanded) {
        HeadsUpEntry headsUpEntry = this.mHeadsUpEntries.get(entry.key);
        if (headsUpEntry != null && headsUpEntry.expanded != expanded && entry.row.isPinned()) {
            headsUpEntry.expanded = expanded;
            if (expanded) {
                headsUpEntry.removeAutoRemovalCallbacks();
            } else {
                headsUpEntry.updateEntry(false);
            }
        }
    }

    public void onReorderingAllowed() {
        Iterator<NotificationData.Entry> it = this.mEntriesToRemoveWhenReorderingAllowed.iterator();
        while (it.hasNext()) {
            NotificationData.Entry entry = it.next();
            if (isHeadsUp(entry.key)) {
                removeHeadsUpEntry(entry);
            }
        }
        this.mEntriesToRemoveWhenReorderingAllowed.clear();
    }

    public void setVisualStabilityManager(VisualStabilityManager visualStabilityManager) {
        this.mVisualStabilityManager = visualStabilityManager;
    }

    public void setStatusBarState(int statusBarState) {
        this.mStatusBarState = statusBarState;
    }

    public static int getHeadsUpTopMargin(Context context) {
        int margin = context.getResources().getDimensionPixelSize(R.dimen.notification_heads_up_margin_top);
        boolean landscape = context.getResources().getConfiguration().orientation == 2;
        int resId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resId <= 0 || !Constants.IS_NOTCH || landscape) {
            return margin;
        }
        return margin + context.getResources().getDimensionPixelSize(resId);
    }
}
