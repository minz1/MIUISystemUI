package com.android.systemui.miui.statusbar.analytics;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import com.android.internal.os.SomeArgs;
import com.android.systemui.AdTracker;
import com.android.systemui.AnalyticsHelper;
import com.android.systemui.Constants;
import com.android.systemui.Util;
import com.android.systemui.miui.analytics.AnalyticsWrapper;
import com.android.systemui.miui.statusbar.ExpandedNotification;
import com.android.systemui.miui.statusbar.notification.FoldBucketHelper;
import com.android.systemui.miui.statusbar.notification.PushEvents;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.stack.NotificationStackScrollLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SystemUIStat {
    /* access modifiers changed from: private */
    public static boolean DEBUG = Constants.DEBUG;
    private final long NOTIFICATION_TIME_INTERVAL = TimeUnit.DAYS.toMillis(1);
    private Handler mBgHandler;
    /* access modifiers changed from: private */
    public Context mContext;
    private List<ExposeMessage> mExposeMessages;
    private long mFoldViewVisibleTime;
    /* access modifiers changed from: private */
    public long mLastNotificationTime;

    private final class WorkHandler extends Handler {
        public WorkHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1001:
                    TinyData tinyData = ((INotificationEvent) msg.obj).getTinyData();
                    if (SystemUIStat.DEBUG) {
                        Log.d("SystemUIStat", tinyData.toString());
                    }
                    Intent intent = new Intent("com.xiaomi.xmsf.push.XMSF_UPLOAD_ACTIVE");
                    intent.setPackage("com.xiaomi.xmsf");
                    intent.putExtra("pkgname", tinyData.getPkg());
                    intent.putExtra("category", tinyData.getCategory());
                    intent.putExtra("name", tinyData.getName());
                    intent.putExtra("data", tinyData.getData());
                    try {
                        SystemUIStat.this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT, "com.xiaomi.xmsf.permission.USE_XMSF_UPLOAD");
                        return;
                    } catch (Exception e) {
                        Log.d("SystemUIStat", "uploadTinyData failed {" + tinyData.toString() + "}");
                        return;
                    }
                case 1002:
                    ADBlock adBlock = (ADBlock) msg.obj;
                    if (SystemUIStat.DEBUG) {
                        Log.d("SystemUIStat", adBlock.adId);
                    }
                    Intent intent2 = new Intent("miui.intent.adblock");
                    intent2.setPackage("com.miui.systemAdSolution");
                    intent2.putExtra("adid", adBlock.adId);
                    SystemUIStat.this.mContext.sendBroadcastAsUser(intent2, UserHandle.CURRENT);
                    return;
                case 1003:
                    SomeArgs args = (SomeArgs) msg.obj;
                    AnalyticsWrapper.recordCountEventAnonymous((String) args.arg1, (String) args.arg2, (Map) args.arg3);
                    return;
                case 1004:
                    SomeArgs args2 = (SomeArgs) msg.obj;
                    AnalyticsWrapper.recordCalculateEventAnonymous((String) args2.arg1, (String) args2.arg2, ((Long) args2.arg3).longValue(), (Map) args2.arg4);
                    return;
                case 1005:
                    SystemUIStat.this.trackAppNotificationCount((String) msg.obj);
                    return;
                case 1006:
                    long unused = SystemUIStat.this.mLastNotificationTime = PreferenceManager.getDefaultSharedPreferences(SystemUIStat.this.mContext).getLong("pref_notification_time", 0);
                    return;
                case 1007:
                    SomeArgs args3 = (SomeArgs) msg.obj;
                    AnalyticsWrapper.recordStringPropertyEventAnonymous((String) args3.arg1, (String) args3.arg2, (String) args3.arg3);
                    return;
                default:
                    return;
            }
        }
    }

    public SystemUIStat(Context context) {
        this.mContext = context;
        this.mExposeMessages = new ArrayList();
        HandlerThread bgThread = new HandlerThread("SystemUIStat", 10);
        bgThread.start();
        this.mBgHandler = new WorkHandler(bgThread.getLooper());
        this.mBgHandler.sendEmptyMessage(1006);
    }

    public void onRemove(ExpandableNotificationRow row, String removeLocation) {
        onRemoveSingle(row.getEntry(), removeLocation);
        if (row.isSummaryWithChildren()) {
            List<ExpandableNotificationRow> notificationChildren = row.getNotificationChildren();
            if (notificationChildren != null) {
                for (ExpandableNotificationRow child : notificationChildren) {
                    onRemoveSingle(child.getEntry(), removeLocation);
                }
            }
        }
    }

    private void onRemoveSingle(NotificationData.Entry entry, String removeLocation) {
        ExpandedNotification notification = entry.notification;
        uploadTinyData(new NotificationRemoveEvent(notification.getNotificationEvent(), removeLocation));
        AnalyticsHelper.trackNotificationRemove(notification.getPackageName(), "right_swipe");
        AdTracker.trackRemove(this.mContext, entry);
        onVisibilityChanged(entry, false);
        postExposeEventIfNeed(entry);
    }

    public void onRemoveAll(ExpandedNotification notification) {
        uploadTinyData(new NotificationRemoveAllEvent(notification.getNotificationEvent(), "statusbar"));
        AnalyticsHelper.trackNotificationRemove(notification.getPackageName(), "click_clear_button");
    }

    public void onBlock(ExpandedNotification notification) {
        sendBlockNotificationEvent(notification.getNotificationEvent(), "statusbar", notification.getPackageName(), PushEvents.getADId(notification.getNotification()));
        AnalyticsHelper.trackNotificationBlock(notification.getPackageName(), notification);
    }

    public void onBlock(String pkg, String messageId) {
        String adId = null;
        if ("com.miui.systemAdSolution".equals(pkg) || "com.miui.msa.global".equals(pkg)) {
            adId = messageId;
        }
        sendBlockNotificationEvent(null, "settings", pkg, adId);
        AnalyticsHelper.trackNotificationBlock(pkg);
    }

    public void onArrive(ExpandedNotification notification) {
        uploadTinyData(new NotificationArriveEvent(notification.getNotificationEvent()));
        this.mBgHandler.obtainMessage(1005, notification.getPackageName()).sendToTarget();
    }

    /* access modifiers changed from: private */
    public void trackAppNotificationCount(String pkg) {
        SharedPreferences share = this.mContext.getSharedPreferences("notification_count", 0);
        share.edit().putLong(pkg, share.getLong(pkg, 0) + 1).commit();
        long current = System.currentTimeMillis();
        if (this.mLastNotificationTime == 0 || this.mLastNotificationTime > current) {
            this.mLastNotificationTime = current;
            PreferenceManager.getDefaultSharedPreferences(this.mContext).edit().putLong("pref_notification_time", current).commit();
        } else if (current - this.mLastNotificationTime > this.NOTIFICATION_TIME_INTERVAL) {
            this.mLastNotificationTime = current;
            PreferenceManager.getDefaultSharedPreferences(this.mContext).edit().putLong("pref_notification_time", current).commit();
            try {
                Map<String, ?> countMap = share.getAll();
                if (countMap != null) {
                    for (Map.Entry<String, ?> entry : countMap.entrySet()) {
                        Map<String, String> params = new HashMap<>();
                        params.put("pkg", entry.getKey());
                        params.put("alogrithm", String.valueOf(FoldBucketHelper.getFoldBucket()));
                        recordCalculateEventAnonymous("systemui_notifications", "systemui_notification_add_count", (Long) entry.getValue(), params);
                    }
                }
                share.edit().clear().commit();
            } catch (Exception e) {
            }
        }
    }

    public void onClick(ExpandedNotification notification, boolean floatNotification, boolean keyguardNotification, int index) {
        uploadTinyData(new NotificationClickEvent(notification.getNotificationEvent(), floatNotification, keyguardNotification));
        AnalyticsHelper.trackNotificationClick(notification.getPackageName(), index);
    }

    public void onActionClick(ExpandedNotification notification, int actionIndex) {
        uploadTinyData(new ActionClickEvent(notification.getNotificationEvent(), actionIndex));
        AnalyticsHelper.trackActionClick(notification.getPackageName(), actionIndex);
    }

    public void onSetImportance(ExpandedNotification notification, int importance) {
        uploadTinyData(new NotificationImportanceEvent(notification.getNotificationEvent(), importance));
        AnalyticsHelper.trackSetImportance(notification.getPackageName(), importance);
    }

    public void onVisibilityChanged(NotificationData.Entry entry, boolean visible) {
        if (visible != entry.isSeen) {
            if (visible) {
                entry.isSeen = true;
                if (entry.seeTime == 0) {
                    entry.seeTime = System.currentTimeMillis();
                }
                if (entry.row != null) {
                    entry.isGroupExpandedWhenExpose = entry.row.isGroupExpanded();
                    entry.isHeadsUpWhenExpose = entry.row.isHeadsUp();
                    entry.isKeyguardWhenExpose = entry.row.isOnKeyguard();
                }
                AdTracker.trackShow(this.mContext, entry);
                return;
            }
            entry.isSeen = false;
            List<ExposeMessage> list = entry.messageList;
            ExposeMessage exposeMessage = new ExposeMessage(entry.seeTime, entry.isGroupExpandedWhenExpose, entry.isHeadsUpWhenExpose, entry.isKeyguardWhenExpose);
            list.add(exposeMessage);
            entry.seeTime = 0;
        } else if (entry.row != null && visible && entry.isGroupExpandedWhenExpose != entry.row.isGroupExpanded()) {
            List<ExposeMessage> list2 = entry.messageList;
            ExposeMessage exposeMessage2 = new ExposeMessage(entry.seeTime, entry.isGroupExpandedWhenExpose, entry.isHeadsUpWhenExpose, entry.isKeyguardWhenExpose);
            list2.add(exposeMessage2);
            entry.isGroupExpandedWhenExpose = entry.row.isGroupExpanded();
            entry.seeTime = System.currentTimeMillis();
        }
    }

    public void onPanelCollapsed(NotificationStackScrollLayout stackScrollLayout) {
        markAllNotificationInVisibleAndExpose(stackScrollLayout);
    }

    private void markAllNotificationInVisibleAndExpose(NotificationStackScrollLayout stackScrollLayout) {
        int numChildren = stackScrollLayout.getChildCount();
        for (int i = 0; i < numChildren; i++) {
            View child = stackScrollLayout.getChildAt(i);
            if (child instanceof ExpandableNotificationRow) {
                ExpandableNotificationRow row = (ExpandableNotificationRow) child;
                NotificationData.Entry entry = row.getEntry();
                onVisibilityChanged(entry, false);
                postExposeEventIfNeed(entry);
                if (row.isSummaryWithChildren()) {
                    List<ExpandableNotificationRow> notificationChildren = row.getNotificationChildren();
                    if (notificationChildren != null) {
                        for (ExpandableNotificationRow childRow : notificationChildren) {
                            NotificationData.Entry entry2 = childRow.getEntry();
                            onVisibilityChanged(entry2, false);
                            postExposeEventIfNeed(entry2);
                        }
                    }
                }
            }
        }
        onFoldViewVisibilityChanged(false);
        if (!this.mExposeMessages.isEmpty()) {
            uploadTinyData(new FoldExposeEvent(this.mExposeMessages));
            this.mExposeMessages.clear();
        }
    }

    public void postExposeEventIfNeed(NotificationData.Entry entry) {
        if (entry.messageList != null && !entry.messageList.isEmpty()) {
            uploadTinyData(new NotificationExposeEvent(entry.notification.getNotificationEvent(), entry.messageList));
            entry.messageList.clear();
        }
    }

    public void onOpenFold(NotificationStackScrollLayout stackScrollLayout) {
        markAllNotificationInVisibleAndExpose(stackScrollLayout);
        uploadTinyData(new FoldClickEvent());
        AnalyticsHelper.trackFoldClick();
    }

    public void onCloseFold(NotificationStackScrollLayout stackScrollLayout) {
        markAllNotificationInVisibleAndExpose(stackScrollLayout);
    }

    public void onFoldViewVisibilityChanged(boolean visible) {
        if (visible) {
            this.mFoldViewVisibleTime = System.currentTimeMillis();
        } else if (this.mFoldViewVisibleTime != 0) {
            List<ExposeMessage> list = this.mExposeMessages;
            ExposeMessage exposeMessage = new ExposeMessage(this.mFoldViewVisibleTime, false, false, false);
            list.add(exposeMessage);
            this.mFoldViewVisibleTime = 0;
        }
    }

    private void sendBlockNotificationEvent(NotificationEvent notificationEvent, String blockLocation, String pkg, String adId) {
        uploadTinyData(new NotificationBlockEvent(notificationEvent, pkg, blockLocation));
        if ("com.miui.systemAdSolution".equals(pkg)) {
            ADBlock adBlock = new ADBlock();
            adBlock.adId = adId;
            sendADBlockEvent(adBlock);
        }
    }

    private void uploadTinyData(INotificationEvent event) {
        if (Util.isUserExperienceProgramEnable()) {
            this.mBgHandler.obtainMessage(1001, event).sendToTarget();
        }
    }

    private void sendADBlockEvent(ADBlock adBlock) {
        if (!TextUtils.isEmpty(adBlock.adId)) {
            this.mBgHandler.obtainMessage(1002, adBlock).sendToTarget();
        }
    }

    public void recordCountEventAnonymous(String category, String key, Map<String, String> params) {
        if (DEBUG) {
            Log.d("SystemUIStat", "track() category=" + category + ",key=" + key + ",params=" + params);
        }
        SomeArgs args = SomeArgs.obtain();
        args.arg1 = category;
        args.arg2 = key;
        args.arg3 = params;
        this.mBgHandler.obtainMessage(1003, args).sendToTarget();
    }

    public void recordStringPropertyEventAnonymous(String category, String event, String value) {
        if (DEBUG) {
            Log.d("SystemUIStat", "track() category=" + category + ",event=" + event + ",value=" + value);
        }
        SomeArgs args = SomeArgs.obtain();
        args.arg1 = category;
        args.arg2 = event;
        args.arg3 = value;
        this.mBgHandler.obtainMessage(1007, args).sendToTarget();
    }

    public void recordCalculateEventAnonymous(String category, String key, Long value, Map<String, String> params) {
        if (DEBUG) {
            Log.d("SystemUIStat", "track() category=" + category + ",key=" + key + ",value=" + value + ",params=" + params);
        }
        SomeArgs args = SomeArgs.obtain();
        args.arg1 = category;
        args.arg2 = key;
        args.arg3 = value;
        args.arg4 = params;
        this.mBgHandler.obtainMessage(1004, args).sendToTarget();
    }

    public void uploadLocalAlgoModel() {
        uploadTinyData(new UploadLocalAlgoModelEvent(PushEvents.getLocalModelStr(this.mContext)));
    }
}
