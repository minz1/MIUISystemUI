package com.android.systemui.statusbar.notification;

import android.app.Notification;
import android.app.NotificationCompat;
import android.os.CancellationSignal;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.NotificationContentView;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.notification.InCallNotificationView;
import com.android.systemui.statusbar.notification.NotificationInflater;
import com.android.systemui.util.Assert;
import java.util.HashMap;

public class NotificationInflaterHelper {
    private static final NotificationInflater.InflationExecutor EXECUTOR = new NotificationInflater.InflationExecutor();

    /* access modifiers changed from: private */
    public static void onViewApplied(View v, NotificationInflater.InflationProgress result, int reInflateFlags, int inflationId, ExpandableNotificationRow row, boolean redactAmbient, boolean isNewView, NotificationInflater.InflationCallback callback, NotificationViewWrapper existingWrapper, HashMap<Integer, CancellationSignal> runningInflations, NotificationInflater.ApplyCallback applyCallback, InCallNotificationView.InCallCallback inCallCallback) {
        if (isNewView) {
            v.setIsRootNamespace(true);
            applyCallback.setResultView(v);
        } else if (existingWrapper != null) {
            existingWrapper.onReinflated();
        }
        runningInflations.remove(Integer.valueOf(inflationId));
    }

    /* access modifiers changed from: private */
    public static void applyRemoteViewSync(NotificationInflater.InflationProgress result, int reInflateFlags, int inflationId, ExpandableNotificationRow row, boolean redactAmbient, boolean isNewView, RemoteViews.OnClickHandler remoteViewClickHandler, NotificationInflater.InflationCallback callback, NotificationData.Entry entry, NotificationContentView parentLayout, View existingView, NotificationViewWrapper existingWrapper, HashMap<Integer, CancellationSignal> runningInflations, NotificationInflater.ApplyCallback applyCallback, InCallNotificationView.InCallCallback inCallCallback, Exception formerException) {
        NotificationInflater.InflationProgress inflationProgress = result;
        RemoteViews.OnClickHandler onClickHandler = remoteViewClickHandler;
        HashMap<Integer, CancellationSignal> hashMap = runningInflations;
        Exception exc = formerException;
        try {
            RemoteViews newContentView = applyCallback.getRemoteView();
            View newView = existingView;
            if (isNewView) {
                try {
                    newView = newContentView.apply(inflationProgress.packageContext, parentLayout, onClickHandler);
                    View view = existingView;
                } catch (Exception e) {
                    anotherException = e;
                    NotificationContentView notificationContentView = parentLayout;
                    Exception exc2 = exc;
                    hashMap.put(Integer.valueOf(inflationId), new CancellationSignal());
                    handleInflationError(hashMap, anotherException, entry.notification, callback);
                }
            } else {
                NotificationContentView notificationContentView2 = parentLayout;
                newContentView.reapply(inflationProgress.packageContext, existingView, onClickHandler);
            }
            NotificationInflater.InflationProgress inflationProgress2 = inflationProgress;
            Exception exc3 = exc;
            try {
                onViewApplied(newView, inflationProgress2, reInflateFlags, inflationId, row, redactAmbient, isNewView, callback, existingWrapper, hashMap, applyCallback, inCallCallback);
                if (exc3 != null) {
                    Log.wtf("NotificationInflater", "Async Inflation failed but normal inflation finished normally.", exc3);
                }
                NotificationInflater.InflationCallback inflationCallback = callback;
                NotificationData.Entry entry2 = entry;
            } catch (Exception e2) {
                anotherException = e2;
                hashMap.put(Integer.valueOf(inflationId), new CancellationSignal());
                handleInflationError(hashMap, anotherException, entry.notification, callback);
            }
        } catch (Exception e3) {
            anotherException = e3;
            Exception exc22 = exc;
            hashMap.put(Integer.valueOf(inflationId), new CancellationSignal());
            handleInflationError(hashMap, anotherException, entry.notification, callback);
        }
    }

    private static void applyRemoteViewAsync(NotificationInflater.InflationProgress result, int reInflateFlags, int inflationId, ExpandableNotificationRow row, boolean redactAmbient, boolean isNewView, RemoteViews.OnClickHandler remoteViewClickHandler, NotificationInflater.InflationCallback callback, NotificationData.Entry entry, NotificationContentView parentLayout, View existingView, NotificationViewWrapper existingWrapper, HashMap<Integer, CancellationSignal> runningInflations, NotificationInflater.ApplyCallback applyCallback, InCallNotificationView.InCallCallback inCallCallback) {
        CancellationSignal cancellationSignal;
        RemoteViews newContentView = applyCallback.getRemoteView();
        final NotificationInflater.InflationProgress inflationProgress = result;
        final int i = reInflateFlags;
        final int i2 = inflationId;
        final ExpandableNotificationRow expandableNotificationRow = row;
        final boolean z = redactAmbient;
        final boolean z2 = isNewView;
        final NotificationInflater.InflationCallback inflationCallback = callback;
        final NotificationViewWrapper notificationViewWrapper = existingWrapper;
        final HashMap<Integer, CancellationSignal> hashMap = runningInflations;
        final NotificationInflater.ApplyCallback applyCallback2 = applyCallback;
        final InCallNotificationView.InCallCallback inCallCallback2 = inCallCallback;
        final RemoteViews.OnClickHandler onClickHandler = remoteViewClickHandler;
        final NotificationData.Entry entry2 = entry;
        final NotificationContentView notificationContentView = parentLayout;
        final View view = existingView;
        AnonymousClass1 r0 = new RemoteViews.OnViewAppliedListener() {
            public void onViewApplied(View v) {
                NotificationInflaterHelper.onViewApplied(v, NotificationInflater.InflationProgress.this, i, i2, expandableNotificationRow, z, z2, inflationCallback, notificationViewWrapper, hashMap, applyCallback2, inCallCallback2);
                NotificationInflater.finishIfDone(NotificationInflater.InflationProgress.this, i, hashMap, inflationCallback, expandableNotificationRow, z, inCallCallback2);
            }

            public void onError(Exception e) {
                NotificationInflaterHelper.applyRemoteViewSync(NotificationInflater.InflationProgress.this, i, i2, expandableNotificationRow, z, z2, onClickHandler, inflationCallback, entry2, notificationContentView, view, notificationViewWrapper, hashMap, applyCallback2, inCallCallback2, e);
            }
        };
        AnonymousClass1 r4 = r0;
        if (isNewView) {
            cancellationSignal = newContentView.applyAsync(result.packageContext, parentLayout, EXECUTOR, r4, remoteViewClickHandler);
        } else {
            cancellationSignal = newContentView.reapplyAsync(result.packageContext, existingView, EXECUTOR, r4, remoteViewClickHandler);
        }
        runningInflations.put(Integer.valueOf(inflationId), cancellationSignal);
    }

    public static void applyRemoteView(NotificationInflater.InflationProgress result, int reInflateFlags, int inflationId, ExpandableNotificationRow row, boolean redactAmbient, boolean isNewView, RemoteViews.OnClickHandler remoteViewClickHandler, NotificationInflater.InflationCallback callback, NotificationData.Entry entry, NotificationContentView parentLayout, View existingView, NotificationViewWrapper existingWrapper, HashMap<Integer, CancellationSignal> runningInflations, NotificationInflater.ApplyCallback applyCallback, InCallNotificationView.InCallCallback inCallCallback) {
        if (NotificationInflater.isAsyncApplySupported()) {
            applyRemoteViewAsync(result, reInflateFlags, inflationId, row, redactAmbient, isNewView, remoteViewClickHandler, callback, entry, parentLayout, existingView, existingWrapper, runningInflations, applyCallback, inCallCallback);
        } else {
            applyRemoteViewSync(result, reInflateFlags, inflationId, row, redactAmbient, isNewView, remoteViewClickHandler, callback, entry, parentLayout, existingView, existingWrapper, runningInflations, applyCallback, inCallCallback, null);
        }
    }

    private static void handleInflationError(HashMap<Integer, CancellationSignal> runningInflations, Exception e, StatusBarNotification notification, NotificationInflater.InflationCallback callback) {
        Assert.isMainThread();
        for (CancellationSignal signal : runningInflations.values()) {
            signal.cancel();
        }
        if (callback != null) {
            callback.handleInflationException(notification, e);
        }
    }

    public static RemoteViews createContentView(Notification.Builder builder, boolean isLowPriority, boolean useLarge, ExpandableNotificationRow row) {
        RemoteViews view = NotificationCompat.createContentView(builder, isLowPriority, useLarge);
        applyMiuiAction(view, row);
        return view;
    }

    public static RemoteViews createExpandedView(Notification.Builder builder, boolean isLowPriority, ExpandableNotificationRow row) {
        if (row.getEntry().notification.isShowMiuiAction()) {
            RemoteViews view = builder.createContentView();
            applyMiuiAction(view, row);
            return view;
        }
        RemoteViews view2 = builder.createBigContentView();
        if (view2 != null) {
            return view2;
        }
        if (!isLowPriority) {
            return null;
        }
        RemoteViews contentView = builder.createContentView();
        NotificationCompat.makeHeaderExpanded(contentView);
        return contentView;
    }

    public static RemoteViews createHeadsUpView(Notification.Builder builder, boolean usesIncreasedHeadsUpHeight, ExpandableNotificationRow row) {
        if (!row.getEntry().notification.isShowMiuiAction()) {
            return NotificationCompat.createHeadsUpContentView(builder, usesIncreasedHeadsUpHeight);
        }
        RemoteViews view = builder.createContentView();
        applyMiuiAction(view, row);
        return view;
    }

    public static RemoteViews createPublicContentView(Notification.Builder builder, ExpandableNotificationRow row) {
        return builder.makePublicContentView();
    }

    public static RemoteViews createAmbientView(Notification.Builder builder, boolean redactAmbient) {
        return NotificationCompat.makeAmbientNotification(builder, redactAmbient);
    }

    private static void applyMiuiAction(RemoteViews remoteViews, ExpandableNotificationRow row) {
        if (remoteViews != null && row.getEntry().notification.isShowMiuiAction()) {
            Notification.Action[] actions = row.getEntry().notification.getNotification().actions;
            if (actions != null && actions.length > 0) {
                Notification.Action action = actions[0];
                if (action.actionIntent != null) {
                    remoteViews.setOnClickPendingIntent(16908663, action.actionIntent);
                }
                if (action.getRemoteInputs() != null) {
                    remoteViews.setRemoteInputs(16908663, action.getRemoteInputs());
                }
            }
        }
    }
}
