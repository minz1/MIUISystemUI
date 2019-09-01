package com.android.systemui.statusbar;

import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.util.ArraySet;
import android.util.Log;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.statusbar.NotificationVisibility;
import com.android.internal.statusbar.NotificationVisibilityCompat;
import com.android.systemui.Dependency;
import com.android.systemui.UiOffloadThread;
import com.android.systemui.miui.statusbar.analytics.SystemUIStat;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.stack.NotificationStackScrollLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class NotificationLogger {
    protected IStatusBarService mBarService = IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"));
    /* access modifiers changed from: private */
    public final ArraySet<NotificationVisibility> mCurrentlyVisibleNotifications = new ArraySet<>();
    protected Handler mHandler = new Handler();
    /* access modifiers changed from: private */
    public long mLastVisibilityReportUptimeMs;
    protected NotificationData mNotificationData;
    protected final OnChildLocationsChangedListener mNotificationLocationsChangedListener = new OnChildLocationsChangedListener() {
        public void onChildLocationsChanged() {
            if (!NotificationLogger.this.mHandler.hasCallbacks(NotificationLogger.this.mVisibilityReporter)) {
                NotificationLogger.this.mHandler.postAtTime(NotificationLogger.this.mVisibilityReporter, NotificationLogger.this.mLastVisibilityReportUptimeMs + 500);
            }
        }
    };
    /* access modifiers changed from: private */
    public NotificationStackScrollLayout mStackScroller;
    /* access modifiers changed from: private */
    public StatusBar mStatusBar;
    private final UiOffloadThread mUiOffloadThread = ((UiOffloadThread) Dependency.get(UiOffloadThread.class));
    protected final Runnable mVisibilityReporter = new Runnable() {
        private boolean mFoldViewVisible;
        private final ArraySet<NotificationVisibility> mTmpCurrentlyVisibleNotifications = new ArraySet<>();
        private final ArraySet<NotificationVisibility> mTmpNewlyVisibleNotifications = new ArraySet<>();
        private final ArraySet<NotificationVisibility> mTmpNoLongerVisibleNotifications = new ArraySet<>();

        public void run() {
            long unused = NotificationLogger.this.mLastVisibilityReportUptimeMs = SystemClock.uptimeMillis();
            ArrayList<NotificationData.Entry> activeNotifications = NotificationLogger.this.mNotificationData.getActiveNotifications();
            int N = activeNotifications.size();
            for (int i = 0; i < N; i++) {
                NotificationData.Entry entry = activeNotifications.get(i);
                String key = entry.notification.getKey();
                boolean isVisible = NotificationLogger.this.mStackScroller.isInVisibleLocation(entry.row);
                NotificationVisibility visObj = NotificationVisibilityCompat.obtain(key, i, N, isVisible);
                boolean previouslyVisible = NotificationLogger.this.mCurrentlyVisibleNotifications.contains(visObj);
                if (isVisible) {
                    this.mTmpCurrentlyVisibleNotifications.add(visObj);
                    if (!previouslyVisible) {
                        this.mTmpNewlyVisibleNotifications.add(visObj);
                    }
                } else {
                    visObj.recycle();
                }
            }
            this.mTmpNoLongerVisibleNotifications.addAll(NotificationLogger.this.mCurrentlyVisibleNotifications);
            this.mTmpNoLongerVisibleNotifications.removeAll(this.mTmpCurrentlyVisibleNotifications);
            NotificationLogger.this.logNotificationVisibilityChanges(this.mTmpNewlyVisibleNotifications, this.mTmpNoLongerVisibleNotifications);
            NotificationLogger.this.recycleAllVisibilityObjects((ArraySet<NotificationVisibility>) NotificationLogger.this.mCurrentlyVisibleNotifications);
            NotificationLogger.this.mCurrentlyVisibleNotifications.addAll(this.mTmpCurrentlyVisibleNotifications);
            NotificationLogger.this.recycleAllVisibilityObjects(this.mTmpNoLongerVisibleNotifications);
            this.mTmpCurrentlyVisibleNotifications.clear();
            this.mTmpNewlyVisibleNotifications.clear();
            this.mTmpNoLongerVisibleNotifications.clear();
            boolean foldViewVisible = NotificationLogger.this.mStackScroller.isFoldFooterViewInVisibleLocation();
            if (foldViewVisible != this.mFoldViewVisible) {
                this.mFoldViewVisible = foldViewVisible;
                ((SystemUIStat) Dependency.get(SystemUIStat.class)).onFoldViewVisibilityChanged(this.mFoldViewVisible);
            }
        }
    };

    public interface OnChildLocationsChangedListener {
        void onChildLocationsChanged();
    }

    public void setUp(StatusBar statusBar, NotificationData notificationData, NotificationStackScrollLayout stackScroller) {
        this.mStatusBar = statusBar;
        this.mNotificationData = notificationData;
        this.mStackScroller = stackScroller;
    }

    public void stopNotificationLogging() {
        if (!this.mCurrentlyVisibleNotifications.isEmpty()) {
            logNotificationVisibilityChanges(Collections.emptyList(), this.mCurrentlyVisibleNotifications);
            recycleAllVisibilityObjects(this.mCurrentlyVisibleNotifications);
        }
        this.mHandler.removeCallbacks(this.mVisibilityReporter);
        this.mStackScroller.setChildLocationsChangedListener(null);
    }

    public void startNotificationLogging() {
        this.mStackScroller.setChildLocationsChangedListener(this.mNotificationLocationsChangedListener);
        this.mNotificationLocationsChangedListener.onChildLocationsChanged();
    }

    /* access modifiers changed from: private */
    public void logNotificationVisibilityChanges(Collection<NotificationVisibility> newlyVisible, Collection<NotificationVisibility> noLongerVisible) {
        if (!newlyVisible.isEmpty() || !noLongerVisible.isEmpty()) {
            NotificationVisibility[] newlyVisibleAr = cloneVisibilitiesAsArr(newlyVisible);
            NotificationVisibility[] noLongerVisibleAr = cloneVisibilitiesAsArr(noLongerVisible);
            UiOffloadThread uiOffloadThread = this.mUiOffloadThread;
            final NotificationVisibility[] notificationVisibilityArr = newlyVisibleAr;
            final NotificationVisibility[] notificationVisibilityArr2 = noLongerVisibleAr;
            final Collection<NotificationVisibility> collection = newlyVisible;
            final Collection<NotificationVisibility> collection2 = noLongerVisible;
            AnonymousClass3 r1 = new Runnable() {
                public void run() {
                    try {
                        NotificationLogger.this.mBarService.onNotificationVisibilityChanged(notificationVisibilityArr, notificationVisibilityArr2);
                    } catch (RemoteException e) {
                    }
                    int N = collection.size();
                    if (N > 0) {
                        String[] newlyVisibleKeyAr = new String[N];
                        for (int i = 0; i < N; i++) {
                            newlyVisibleKeyAr[i] = notificationVisibilityArr[i].key;
                        }
                        try {
                            NotificationLogger.this.mStatusBar.setNotificationsShown(newlyVisibleKeyAr);
                        } catch (RuntimeException e2) {
                            Log.d("NotificationLogger", "failed setNotificationsShown: ", e2);
                        }
                    }
                    for (NotificationVisibility notificationVisibility : notificationVisibilityArr) {
                        NotificationLogger.this.onNotificationVisibilityChanged(notificationVisibility.key, true);
                    }
                    for (NotificationVisibility notificationVisibility2 : collection2) {
                        NotificationLogger.this.onNotificationVisibilityChanged(notificationVisibility2.key, false);
                    }
                    NotificationLogger.this.recycleAllVisibilityObjects(notificationVisibilityArr);
                    NotificationLogger.this.recycleAllVisibilityObjects(notificationVisibilityArr2);
                }
            };
            uiOffloadThread.submit(r1);
        }
    }

    /* access modifiers changed from: private */
    public void onNotificationVisibilityChanged(String key, boolean visible) {
        NotificationData.Entry entry = this.mNotificationData.get(key);
        if (entry == null) {
            Log.w("NotificationLogger", "onNotificationVisibilityChanged() no entry:" + key);
            return;
        }
        ((SystemUIStat) Dependency.get(SystemUIStat.class)).onVisibilityChanged(entry, visible);
    }

    /* access modifiers changed from: private */
    public void recycleAllVisibilityObjects(ArraySet<NotificationVisibility> array) {
        int N = array.size();
        for (int i = 0; i < N; i++) {
            array.valueAt(i).recycle();
        }
        array.clear();
    }

    /* access modifiers changed from: private */
    public void recycleAllVisibilityObjects(NotificationVisibility[] array) {
        int N = array.length;
        for (int i = 0; i < N; i++) {
            if (array[i] != null) {
                array[i].recycle();
            }
        }
    }

    private NotificationVisibility[] cloneVisibilitiesAsArr(Collection<NotificationVisibility> c) {
        NotificationVisibility[] array = new NotificationVisibility[c.size()];
        int i = 0;
        for (NotificationVisibility nv : c) {
            if (nv != null) {
                array[i] = nv.clone();
            }
            i++;
        }
        return array;
    }
}
