package com.android.systemui.statusbar.notification;

import android.app.Notification;
import android.app.NotificationCompat;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.CancellationSignal;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.Util;
import com.android.systemui.miui.AppIconsManager;
import com.android.systemui.miui.statusbar.ExpandedNotification;
import com.android.systemui.miui.statusbar.InCallUtils;
import com.android.systemui.miui.statusbar.notification.NotificationUtil;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.InflationTask;
import com.android.systemui.statusbar.NotificationContentView;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.notification.InCallNotificationView;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.util.Assert;
import java.util.HashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class NotificationInflater {
    @VisibleForTesting
    static final int FLAG_REINFLATE_ALL = -1;
    @VisibleForTesting
    static final int FLAG_REINFLATE_EXPANDED_VIEW = 2;
    private InflationCallback mCallback;
    private InCallNotificationView.InCallCallback mInCallCallback;
    private boolean mIsChildInGroup;
    private boolean mIsLowPriority;
    private boolean mRedactAmbient;
    private RemoteViews.OnClickHandler mRemoteViewClickHandler;
    private final ExpandableNotificationRow mRow;
    private boolean mUsesIncreasedHeadsUpHeight;
    private boolean mUsesIncreasedHeight;

    @VisibleForTesting
    static abstract class ApplyCallback {
        public abstract RemoteViews getRemoteView();

        public abstract void setResultView(View view);

        ApplyCallback() {
        }
    }

    public static class AsyncInflationTask extends AsyncTask<Void, Void, InflationProgress> implements InflationTask, InflationCallback {
        private final InflationCallback mCallback;
        private CancellationSignal mCancellationSignal;
        private final Context mContext;
        private Exception mError;
        private InCallNotificationView.InCallCallback mInCallCallback;
        private final boolean mIsChildInGroup;
        private final boolean mIsLowPriority;
        private int mReInflateFlags;
        private final boolean mRedactAmbient;
        private RemoteViews.OnClickHandler mRemoteViewClickHandler;
        private ExpandableNotificationRow mRow;
        private final ExpandedNotification mSbn;
        private final boolean mUsesIncreasedHeadsUpHeight;
        private final boolean mUsesIncreasedHeight;

        private AsyncInflationTask(ExpandedNotification notification, int reInflateFlags, ExpandableNotificationRow row, boolean isLowPriority, boolean isChildInGroup, boolean usesIncreasedHeight, boolean usesIncreasedHeadsUpHeight, boolean redactAmbient, InflationCallback callback, RemoteViews.OnClickHandler remoteViewClickHandler, InCallNotificationView.InCallCallback inCallCallback) {
            this.mRow = row;
            this.mSbn = notification;
            this.mReInflateFlags = reInflateFlags;
            this.mContext = this.mRow.getContext();
            this.mIsLowPriority = isLowPriority;
            this.mIsChildInGroup = isChildInGroup;
            this.mUsesIncreasedHeight = usesIncreasedHeight;
            this.mUsesIncreasedHeadsUpHeight = usesIncreasedHeadsUpHeight;
            this.mRedactAmbient = redactAmbient;
            this.mRemoteViewClickHandler = remoteViewClickHandler;
            this.mCallback = callback;
            this.mInCallCallback = inCallCallback;
            row.getEntry().setInflationTask(this);
        }

        @VisibleForTesting
        public int getReInflateFlags() {
            return this.mReInflateFlags;
        }

        /* access modifiers changed from: protected */
        public InflationProgress doInBackground(Void... params) {
            initAppInfo();
            try {
                Notification.Builder recoveredBuilder = NotificationCompat.recoverBuilder(this.mContext, this.mSbn.getNotification());
                Context packageContext = NotificationUtil.getPackageContext(this.mContext, this.mSbn);
                Notification notification = this.mSbn.getNotification();
                if (this.mIsLowPriority) {
                    NotificationCompat.setBackgroundColorHint(recoveredBuilder, this.mContext.getColor(R.color.notification_material_background_low_priority_color));
                }
                if (NotificationCompat.isMediaNotification(notification) != 0) {
                    MediaNotificationProcessor.processNotification(notification, recoveredBuilder);
                }
                return NotificationInflater.createRemoteViews(this.mReInflateFlags, recoveredBuilder, this.mIsLowPriority, this.mIsChildInGroup, this.mUsesIncreasedHeight, this.mUsesIncreasedHeadsUpHeight, this.mRedactAmbient, packageContext, this.mRow);
            } catch (Exception e) {
                this.mError = e;
                return null;
            }
        }

        private void initAppInfo() {
            int userId = this.mSbn.getUser().getIdentifier();
            PackageManager pmUser = Util.getPackageManagerForUser(this.mContext, userId);
            try {
                ApplicationInfo info = pmUser.getApplicationInfo(this.mSbn.getPackageName(), 795136);
                if (info != null) {
                    this.mSbn.setAppUid(info.uid);
                    this.mSbn.setTargetSdk(info.targetSdkVersion);
                    this.mSbn.setAppName(String.valueOf(pmUser.getApplicationLabel(info)));
                    this.mSbn.setAppIcon(((AppIconsManager) Dependency.get(AppIconsManager.class)).getAppIcon(this.mContext, info, pmUser, userId));
                    this.mSbn.setRowIcon(NotificationUtil.getRowIcon(this.mContext, this.mSbn));
                    if (!TextUtils.equals(this.mRow.getAppName(), this.mSbn.getAppName())) {
                        this.mRow.setAppName(this.mSbn.getAppName());
                    }
                }
            } catch (PackageManager.NameNotFoundException e) {
                this.mSbn.setAppIcon(pmUser.getDefaultActivityIcon());
                this.mSbn.setRowIcon(this.mSbn.getAppIcon());
            }
        }

        /* access modifiers changed from: protected */
        public void onPostExecute(InflationProgress result) {
            if (this.mError == null) {
                this.mCancellationSignal = NotificationInflater.apply(result, this.mReInflateFlags, this.mRow, this.mRedactAmbient, this.mRemoteViewClickHandler, this, this.mInCallCallback);
                return;
            }
            handleError(this.mError);
        }

        private void handleError(Exception e) {
            StatusBarNotification sbn;
            this.mRow.getEntry().onInflationTaskFinished();
            String ident = sbn.getPackageName() + "/0x" + Integer.toHexString(this.mRow.getStatusBarNotification().getId());
            Log.e("StatusBar", "couldn't inflate view for notification " + ident, e);
            this.mCallback.handleInflationException(sbn, new InflationException("Couldn't inflate contentViews" + e));
        }

        public void abort() {
            cancel(true);
            if (this.mCancellationSignal != null) {
                this.mCancellationSignal.cancel();
            }
        }

        public void supersedeTask(InflationTask task) {
            if (task instanceof AsyncInflationTask) {
                this.mReInflateFlags |= ((AsyncInflationTask) task).mReInflateFlags;
            }
        }

        public void handleInflationException(StatusBarNotification notification, Exception e) {
            handleError(e);
        }

        public void onAsyncInflationFinished(NotificationData.Entry entry) {
            this.mRow.getEntry().onInflationTaskFinished();
            this.mRow.onNotificationUpdated();
            this.mCallback.onAsyncInflationFinished(this.mRow.getEntry());
        }
    }

    public interface InflationCallback {
        void handleInflationException(StatusBarNotification statusBarNotification, Exception exc);

        void onAsyncInflationFinished(NotificationData.Entry entry);
    }

    public static class InflationExecutor implements Executor {
        private static final int CORE_POOL_SIZE = Math.max(2, Math.min(CPU_COUNT + NotificationInflater.FLAG_REINFLATE_ALL, 4));
        private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
        private static final int MAXIMUM_POOL_SIZE = ((CPU_COUNT * 2) + 1);
        private static final ThreadFactory sThreadFactory = new ThreadFactory() {
            private final AtomicInteger mCount = new AtomicInteger(1);

            public Thread newThread(Runnable r) {
                return new Thread(r, "InflaterThread #" + this.mCount.getAndIncrement());
            }
        };
        private final ThreadPoolExecutor mExecutor;

        public InflationExecutor() {
            ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, 30, TimeUnit.SECONDS, new LinkedBlockingQueue(), sThreadFactory);
            this.mExecutor = threadPoolExecutor;
            this.mExecutor.allowCoreThreadTimeOut(true);
        }

        public void execute(Runnable runnable) {
            this.mExecutor.execute(runnable);
        }
    }

    @VisibleForTesting
    static class InflationProgress {
        /* access modifiers changed from: private */
        public View inflatedAmbientView;
        /* access modifiers changed from: private */
        public View inflatedContentView;
        /* access modifiers changed from: private */
        public View inflatedExpandedView;
        /* access modifiers changed from: private */
        public View inflatedHeadsUpView;
        /* access modifiers changed from: private */
        public View inflatedPublicView;
        /* access modifiers changed from: private */
        public RemoteViews newAmbientView;
        /* access modifiers changed from: private */
        public RemoteViews newContentView;
        /* access modifiers changed from: private */
        public RemoteViews newExpandedView;
        /* access modifiers changed from: private */
        public RemoteViews newHeadsUpView;
        /* access modifiers changed from: private */
        public RemoteViews newPublicView;
        @VisibleForTesting
        Context packageContext;

        InflationProgress() {
        }
    }

    public NotificationInflater(ExpandableNotificationRow row) {
        this.mRow = row;
    }

    public void setIsLowPriority(boolean isLowPriority) {
        this.mIsLowPriority = isLowPriority;
    }

    public void setIsChildInGroup(boolean childInGroup) {
        if (childInGroup != this.mIsChildInGroup) {
            this.mIsChildInGroup = childInGroup;
            if (this.mIsLowPriority) {
                inflateNotificationViews(3);
            }
        }
    }

    public void setUsesIncreasedHeight(boolean usesIncreasedHeight) {
        this.mUsesIncreasedHeight = usesIncreasedHeight;
    }

    public void setUsesIncreasedHeadsUpHeight(boolean usesIncreasedHeight) {
        this.mUsesIncreasedHeadsUpHeight = usesIncreasedHeight;
    }

    public void setRemoteViewClickHandler(RemoteViews.OnClickHandler remoteViewClickHandler) {
        this.mRemoteViewClickHandler = remoteViewClickHandler;
    }

    public void setRedactAmbient(boolean redactAmbient) {
        if (this.mRedactAmbient != redactAmbient) {
            this.mRedactAmbient = redactAmbient;
            if (this.mRow.getEntry() != null) {
                inflateNotificationViews(16);
            }
        }
    }

    public void inflateNotificationViews() {
        inflateNotificationViews(FLAG_REINFLATE_ALL);
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public void inflateNotificationViews(int reInflateFlags) {
        if (!this.mRow.isRemoved()) {
            AsyncInflationTask asyncInflationTask = new AsyncInflationTask(this.mRow.getEntry().notification, reInflateFlags, this.mRow, this.mIsLowPriority, this.mIsChildInGroup, this.mUsesIncreasedHeight, this.mUsesIncreasedHeadsUpHeight, this.mRedactAmbient, this.mCallback, this.mRemoteViewClickHandler, this.mInCallCallback);
            asyncInflationTask.execute(new Void[0]);
        }
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public InflationProgress inflateNotificationViews(int reInflateFlags, Notification.Builder builder, Context packageContext) {
        InflationProgress result = createRemoteViews(reInflateFlags, builder, this.mIsLowPriority, this.mIsChildInGroup, this.mUsesIncreasedHeight, this.mUsesIncreasedHeadsUpHeight, this.mRedactAmbient, packageContext, this.mRow);
        apply(result, reInflateFlags, this.mRow, this.mRedactAmbient, this.mRemoteViewClickHandler, null, null);
        return result;
    }

    /* access modifiers changed from: private */
    public static InflationProgress createRemoteViews(int reInflateFlags, Notification.Builder builder, boolean isLowPriority, boolean isChildInGroup, boolean usesIncreasedHeight, boolean usesIncreasedHeadsUpHeight, boolean redactAmbient, Context packageContext, ExpandableNotificationRow row) {
        InflationProgress result = new InflationProgress();
        boolean isLowPriority2 = isLowPriority && !isChildInGroup;
        if ((reInflateFlags & 1) != 0) {
            RemoteViews unused = result.newContentView = NotificationInflaterHelper.createContentView(builder, isLowPriority2, usesIncreasedHeight, row);
        }
        if ((reInflateFlags & 2) != 0) {
            RemoteViews unused2 = result.newExpandedView = NotificationInflaterHelper.createExpandedView(builder, isLowPriority2, row);
        }
        if ((reInflateFlags & 4) != 0) {
            RemoteViews unused3 = result.newHeadsUpView = NotificationInflaterHelper.createHeadsUpView(builder, usesIncreasedHeadsUpHeight, row);
        }
        if ((reInflateFlags & 8) != 0) {
            RemoteViews unused4 = result.newPublicView = NotificationInflaterHelper.createPublicContentView(builder, row);
        }
        if ((reInflateFlags & 16) != 0) {
            RemoteViews unused5 = result.newAmbientView = NotificationInflaterHelper.createAmbientView(builder, redactAmbient);
        }
        result.packageContext = packageContext;
        return result;
    }

    public static CancellationSignal apply(InflationProgress result, int reInflateFlags, ExpandableNotificationRow row, boolean redactAmbient, RemoteViews.OnClickHandler remoteViewClickHandler, InflationCallback callback, InCallNotificationView.InCallCallback inCallCallback) {
        NotificationData.Entry entry;
        NotificationContentView privateLayout;
        NotificationContentView publicLayout;
        HashMap<Integer, CancellationSignal> runningInflations;
        NotificationData.Entry entry2;
        NotificationContentView privateLayout2;
        boolean z;
        NotificationData.Entry entry3;
        NotificationContentView privateLayout3;
        NotificationData.Entry entry4;
        boolean z2;
        NotificationContentView publicLayout2;
        NotificationData.Entry entry5;
        boolean isNewView;
        final InflationProgress inflationProgress = result;
        NotificationData.Entry entry6 = row.getEntry();
        NotificationContentView privateLayout4 = row.getPrivateLayout();
        NotificationContentView publicLayout3 = row.getPublicLayout();
        HashMap<Integer, CancellationSignal> runningInflations2 = new HashMap<>();
        if ((reInflateFlags & 1) != 0) {
            runningInflations = runningInflations2;
            publicLayout = publicLayout3;
            privateLayout = privateLayout4;
            entry = entry6;
            NotificationInflaterHelper.applyRemoteView(inflationProgress, reInflateFlags, 1, row, redactAmbient, !compareRemoteViews(result.newContentView, entry6.cachedContentView), remoteViewClickHandler, callback, entry6, privateLayout4, privateLayout4.getContractedChild(), privateLayout4.getVisibleWrapper(0), runningInflations, new ApplyCallback() {
                public void setResultView(View v) {
                    View unused = InflationProgress.this.inflatedContentView = v;
                }

                public RemoteViews getRemoteView() {
                    return InflationProgress.this.newContentView;
                }
            }, inCallCallback);
        } else {
            runningInflations = runningInflations2;
            publicLayout = publicLayout3;
            privateLayout = privateLayout4;
            entry = entry6;
        }
        if ((reInflateFlags & 2) == 0 || result.newExpandedView == null) {
            privateLayout2 = privateLayout;
            entry2 = entry;
            z = true;
        } else {
            NotificationData.Entry entry7 = entry;
            NotificationContentView privateLayout5 = privateLayout;
            privateLayout2 = privateLayout5;
            z = true;
            entry2 = entry7;
            NotificationInflaterHelper.applyRemoteView(inflationProgress, reInflateFlags, 2, row, redactAmbient, !compareRemoteViews(result.newExpandedView, entry7.cachedBigContentView), remoteViewClickHandler, callback, entry7, privateLayout5, privateLayout5.getExpandedChild(), privateLayout5.getVisibleWrapper(1), runningInflations, new ApplyCallback() {
                public void setResultView(View v) {
                    View unused = InflationProgress.this.inflatedExpandedView = v;
                }

                public RemoteViews getRemoteView() {
                    return InflationProgress.this.newExpandedView;
                }
            }, inCallCallback);
        }
        if ((reInflateFlags & 4) == 0 || result.newHeadsUpView == null) {
            privateLayout3 = privateLayout2;
            entry3 = entry2;
        } else {
            NotificationData.Entry entry8 = entry2;
            NotificationContentView privateLayout6 = privateLayout2;
            privateLayout3 = privateLayout6;
            entry3 = entry8;
            NotificationInflaterHelper.applyRemoteView(inflationProgress, reInflateFlags, 4, row, redactAmbient, (!compareRemoteViews(result.newHeadsUpView, entry8.cachedHeadsUpContentView) || entry8.isGameModeWhenHeadsUp != StatusBar.sGameMode) ? z : false, remoteViewClickHandler, callback, entry8, privateLayout6, privateLayout6.getHeadsUpChild(), privateLayout6.getVisibleWrapper(2), runningInflations, new ApplyCallback() {
                public void setResultView(View v) {
                    View unused = InflationProgress.this.inflatedHeadsUpView = v;
                }

                public RemoteViews getRemoteView() {
                    return InflationProgress.this.newHeadsUpView;
                }
            }, inCallCallback);
        }
        if ((reInflateFlags & 8) != 0) {
            NotificationData.Entry entry9 = entry3;
            NotificationContentView publicLayout4 = publicLayout;
            z2 = false;
            publicLayout2 = publicLayout4;
            entry4 = entry9;
            NotificationInflaterHelper.applyRemoteView(inflationProgress, reInflateFlags, 8, row, redactAmbient, !compareRemoteViews(result.newPublicView, entry9.cachedPublicContentView), remoteViewClickHandler, callback, entry9, publicLayout4, publicLayout4.getContractedChild(), publicLayout4.getVisibleWrapper(0), runningInflations, new ApplyCallback() {
                public void setResultView(View v) {
                    View unused = InflationProgress.this.inflatedPublicView = v;
                }

                public RemoteViews getRemoteView() {
                    return InflationProgress.this.newPublicView;
                }
            }, inCallCallback);
        } else {
            publicLayout2 = publicLayout;
            entry4 = entry3;
            z2 = false;
        }
        if (Build.VERSION.SDK_INT < 26 || (reInflateFlags & 16) == 0) {
        } else {
            NotificationContentView newParent = redactAmbient ? publicLayout2 : privateLayout3;
            if (canReapplyAmbient(row, redactAmbient)) {
                entry5 = entry4;
                if (compareRemoteViews(result.newAmbientView, entry5.cachedAmbientContentView)) {
                    isNewView = z2;
                    NotificationData.Entry entry10 = entry5;
                    NotificationContentView notificationContentView = newParent;
                    NotificationInflaterHelper.applyRemoteView(inflationProgress, reInflateFlags, 16, row, redactAmbient, isNewView, remoteViewClickHandler, callback, entry5, newParent, newParent.getAmbientChild(), newParent.getVisibleWrapper(4), runningInflations, new ApplyCallback() {
                        public void setResultView(View v) {
                            View unused = InflationProgress.this.inflatedAmbientView = v;
                        }

                        public RemoteViews getRemoteView() {
                            return InflationProgress.this.newAmbientView;
                        }
                    }, inCallCallback);
                }
            } else {
                entry5 = entry4;
            }
            isNewView = z;
            NotificationData.Entry entry102 = entry5;
            NotificationContentView notificationContentView2 = newParent;
            NotificationInflaterHelper.applyRemoteView(inflationProgress, reInflateFlags, 16, row, redactAmbient, isNewView, remoteViewClickHandler, callback, entry5, newParent, newParent.getAmbientChild(), newParent.getVisibleWrapper(4), runningInflations, new ApplyCallback() {
                public void setResultView(View v) {
                    View unused = InflationProgress.this.inflatedAmbientView = v;
                }

                public RemoteViews getRemoteView() {
                    return InflationProgress.this.newAmbientView;
                }
            }, inCallCallback);
        }
        final HashMap<Integer, CancellationSignal> hashMap = runningInflations;
        finishIfDone(inflationProgress, reInflateFlags, hashMap, callback, row, redactAmbient, inCallCallback);
        CancellationSignal cancellationSignal = new CancellationSignal();
        cancellationSignal.setOnCancelListener(new CancellationSignal.OnCancelListener() {
            public void onCancel() {
                for (CancellationSignal signal : hashMap.values()) {
                    signal.cancel();
                }
            }
        });
        return cancellationSignal;
    }

    public static boolean finishIfDone(InflationProgress result, int reInflateFlags, HashMap<Integer, CancellationSignal> runningInflations, InflationCallback endListener, ExpandableNotificationRow row, boolean redactAmbient, InCallNotificationView.InCallCallback inCallCallback) {
        ExpandableNotificationRow expandableNotificationRow;
        InflationCallback inflationCallback = endListener;
        Assert.isMainThread();
        NotificationData.Entry entry = row.getEntry();
        ExpandedNotification notification = entry.notification;
        NotificationContentView privateLayout = row.getPrivateLayout();
        NotificationContentView publicLayout = row.getPublicLayout();
        Context context = privateLayout.getContext();
        boolean z = false;
        if (!runningInflations.isEmpty()) {
            return false;
        }
        if ((reInflateFlags & 1) != 0) {
            if (result.inflatedContentView != null) {
                processChildView(context, notification, result.inflatedContentView);
                privateLayout.setContractedChild(result.inflatedContentView);
            }
            entry.cachedContentView = result.newContentView;
        }
        if ((reInflateFlags & 2) != 0) {
            if (result.inflatedExpandedView != null) {
                processChildView(context, notification, result.inflatedExpandedView);
                privateLayout.setExpandedChild(result.inflatedExpandedView);
            } else if (result.newExpandedView == null) {
                privateLayout.setExpandedChild(null);
            }
            entry.cachedBigContentView = result.newExpandedView;
            expandableNotificationRow = row;
            expandableNotificationRow.setExpandable(result.newExpandedView != null);
        } else {
            expandableNotificationRow = row;
        }
        if ((reInflateFlags & 4) != 0) {
            if (result.inflatedHeadsUpView != null) {
                processChildView(context, notification, result.inflatedHeadsUpView);
                privateLayout.setHeadsUpChild(result.inflatedHeadsUpView);
                entry.isGameModeWhenHeadsUp = StatusBar.sGameMode;
            } else if (result.newHeadsUpView == null) {
                privateLayout.setHeadsUpChild(null);
            }
            entry.cachedHeadsUpContentView = result.newHeadsUpView;
        }
        if ((reInflateFlags & 8) != 0) {
            if (result.inflatedPublicView != null) {
                processChildView(context, notification, result.inflatedPublicView);
                publicLayout.setContractedChild(result.inflatedPublicView);
            }
            entry.cachedPublicContentView = result.newPublicView;
        }
        if ((reInflateFlags & 16) != 0) {
            if (result.inflatedAmbientView != null) {
                processChildView(context, notification, result.inflatedAmbientView);
                NotificationContentView newParent = redactAmbient ? publicLayout : privateLayout;
                NotificationContentView otherParent = !redactAmbient ? publicLayout : privateLayout;
                newParent.setAmbientChild(result.inflatedAmbientView);
                otherParent.setAmbientChild(null);
            }
            entry.cachedAmbientContentView = result.newAmbientView;
        }
        boolean z2 = StatusBar.sGameMode;
        if (row.getResources().getConfiguration().orientation == 2) {
            z = true;
        }
        optimizeHeadsUpViewIfNeed(result, expandableNotificationRow, z2, z, StatusBar.sIsStatusBarHidden, inCallCallback);
        if (inflationCallback != null) {
            inflationCallback.onAsyncInflationFinished(row.getEntry());
        }
        return true;
    }

    private static void processChildView(Context context, ExpandedNotification notification, View childView) {
        NotificationUtil.applyLegacyRowIcon(context, notification, childView);
        if (childView.getId() != 16909384) {
            ((FrameLayout.LayoutParams) childView.getLayoutParams()).gravity = 0;
        }
    }

    private static void optimizeHeadsUpViewIfNeed(InflationProgress result, ExpandableNotificationRow row, boolean isGameMode, boolean isFullScreen, boolean isLandscape, InCallNotificationView.InCallCallback inCallCallback) {
        int oldHeadsUpViewType;
        NotificationData.Entry entry = row.getEntry();
        NotificationContentView privateLayout = row.getPrivateLayout();
        Context context = privateLayout.getContext();
        if (StatusBar.sGameMode || ((isFullScreen && isLandscape) || InCallUtils.isInCallNotification(context, row.getEntry().notification))) {
            if (result.inflatedHeadsUpView != null) {
                oldHeadsUpViewType = 2;
            } else {
                oldHeadsUpViewType = result.inflatedExpandedView != null ? 1 : 0;
            }
            View oldHeadsUpView = privateLayout.getViewForVisibleType(oldHeadsUpViewType);
            privateLayout.getVisibleWrapper(oldHeadsUpViewType).onContentUpdated(row);
            if (InCallUtils.isInCallNotification(context, entry.notification)) {
                View unused = result.inflatedHeadsUpView = inflateInCallHeadsUpNotification(context, oldHeadsUpView, entry, inCallCallback);
            } else {
                View unused2 = result.inflatedHeadsUpView = inflateOptimizedHeadsUpNotification(context, oldHeadsUpView, entry, isGameMode);
            }
            if (result.inflatedHeadsUpView != null) {
                privateLayout.setHeadsUpChild(result.inflatedHeadsUpView);
            } else {
                Log.w("NotificationInflater", "optimizeHeadsUpViewIfNeed() can not inflate optimized heads up child");
            }
        }
    }

    private static InCallNotificationView inflateInCallHeadsUpNotification(Context context, View oldHeadsUpView, NotificationData.Entry entry, InCallNotificationView.InCallCallback inCallCallback) {
        InCallNotificationView inCallNotificationView = (InCallNotificationView) LayoutInflater.from(context).inflate(R.layout.in_call_heads_up_notification, null);
        inCallNotificationView.updateInfo(oldHeadsUpView, entry.notification.getNotification().extras);
        inCallNotificationView.setInCallCallback(inCallCallback);
        return inCallNotificationView;
    }

    private static OptimizedHeadsUpNotificationView inflateOptimizedHeadsUpNotification(Context context, View oldHeadsUpView, NotificationData.Entry entry, boolean isGameMode) {
        if (oldHeadsUpView == null) {
            Log.d("NotificationInflater", "inflateOptimizedHeadsUpNotification() oldHeadsUpView is null");
            return null;
        }
        ImageView icon = (ImageView) oldHeadsUpView.findViewById(NotificationUtil.showGoogleStyle() ? 16908294 : 16909273);
        TextView title = (TextView) oldHeadsUpView.findViewById(16908310);
        TextView text = (TextView) oldHeadsUpView.findViewById(16909408);
        TextView textLine1 = (TextView) oldHeadsUpView.findViewById(16909436);
        if (icon == null || (title == null && text == null && textLine1 == null)) {
            Log.d("NotificationInflater", "inflateOptimizedHeadsUpNotification() invalid content");
            return null;
        }
        OptimizedHeadsUpNotificationView newRowUi = (OptimizedHeadsUpNotificationView) ((LayoutInflater) context.getSystemService("layout_inflater")).inflate(R.layout.optimized_heads_up_notification, null);
        newRowUi.wrapIconView(icon);
        newRowUi.wrapTitleView(title, isGameMode);
        if (text != null) {
            newRowUi.wrapTextView(text, isGameMode);
        } else {
            newRowUi.wrapTextView(textLine1, isGameMode);
        }
        final View content = oldHeadsUpView.findViewById(R.id.content);
        if (content != null && content.hasOnClickListeners()) {
            newRowUi.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    content.callOnClick();
                }
            });
        }
        if (entry.notification.getNotification().extras.getBoolean("miui.showAction") && entry.getExpandedContentView() != null) {
            final TextView largeAction = (TextView) entry.getExpandedContentView().findViewById(16908663);
            TextView action = newRowUi.getActionView();
            if (!(largeAction == null || action == null)) {
                action.setVisibility(0);
                action.setText(largeAction.getText());
                if (isGameMode) {
                    action.setTextColor(context.getColor(R.color.optimized_game_heads_up_notification_action_text));
                } else {
                    action.setTextColor(context.getColor(R.color.optimized_heads_up_notification_action_text));
                }
                action.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        largeAction.callOnClick();
                    }
                });
            }
        }
        newRowUi.setRow(entry.row);
        return newRowUi;
    }

    private static boolean compareRemoteViews(RemoteViews a, RemoteViews b) {
        return (a == null && b == null) || !(a == null || b == null || b.getPackage() == null || a.getPackage() == null || !a.getPackage().equals(b.getPackage()) || a.getLayoutId() != b.getLayoutId());
    }

    public static boolean isAsyncApplySupported() {
        return Build.VERSION.SDK_INT > 25;
    }

    public void setInflationCallback(InflationCallback callback) {
        this.mCallback = callback;
    }

    public void setInCallCallback(InCallNotificationView.InCallCallback callCallback) {
        this.mInCallCallback = callCallback;
    }

    public void onDensityOrFontScaleChanged() {
        NotificationData.Entry entry = this.mRow.getEntry();
        entry.cachedAmbientContentView = null;
        entry.cachedBigContentView = null;
        entry.cachedContentView = null;
        entry.cachedHeadsUpContentView = null;
        entry.cachedPublicContentView = null;
        inflateNotificationViews();
    }

    private static boolean canReapplyAmbient(ExpandableNotificationRow row, boolean redactAmbient) {
        NotificationContentView ambientView;
        if (redactAmbient) {
            ambientView = row.getPublicLayout();
        } else {
            ambientView = row.getPrivateLayout();
        }
        return ambientView.getAmbientChild() != null;
    }
}
