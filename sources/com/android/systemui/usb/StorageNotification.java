package com.android.systemui.usb;

import android.app.Notification;
import android.app.NotificationCompat;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.os.storage.DiskInfo;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.os.storage.VolumeRecord;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.SparseArray;
import com.android.systemui.SystemUI;
import com.android.systemui.Util;
import com.android.systemui.util.NotificationChannels;
import java.io.File;

public class StorageNotification extends SystemUI {
    private final BroadcastReceiver mFinishReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            StorageNotification.this.mNotificationManager.cancelAsUser(null, 1397575510, UserHandle.ALL);
        }
    };
    private final StorageEventListener mListener = new StorageEventListener() {
        public void onVolumeStateChanged(VolumeInfo vol, int oldState, int newState) {
            StorageNotification.this.onVolumeStateChangedInternal(vol);
        }

        public void onVolumeRecordChanged(VolumeRecord rec) {
            VolumeInfo vol = StorageNotification.this.mStorageManager.findVolumeByUuid(rec.getFsUuid());
            if (vol != null && vol.isMountedReadable()) {
                StorageNotification.this.onVolumeStateChangedInternal(vol);
            }
        }

        public void onVolumeForgotten(String fsUuid) {
            StorageNotification.this.mNotificationManager.cancelAsUser(fsUuid, 1397772886, UserHandle.ALL);
        }

        public void onDiskScanned(DiskInfo disk, int volumeCount) {
            StorageNotification.this.onDiskScannedInternal(disk, volumeCount);
        }

        public void onDiskDestroyed(DiskInfo disk) {
            StorageNotification.this.onDiskDestroyedInternal(disk);
        }
    };
    private final PackageManager.MoveCallback mMoveCallback = new PackageManager.MoveCallback() {
        public void onCreated(int moveId, Bundle extras) {
            MoveInfo move = new MoveInfo();
            move.moveId = moveId;
            move.extras = extras;
            if (extras != null) {
                move.packageName = extras.getString("android.intent.extra.PACKAGE_NAME");
                move.label = extras.getString("android.intent.extra.TITLE");
                move.volumeUuid = extras.getString("android.os.storage.extra.FS_UUID");
            }
            StorageNotification.this.mMoves.put(moveId, move);
        }

        public void onStatusChanged(int moveId, int status, long estMillis) {
            MoveInfo move = (MoveInfo) StorageNotification.this.mMoves.get(moveId);
            if (move == null) {
                Log.w("StorageNotification", "Ignoring unknown move " + moveId);
                return;
            }
            if (PackageManager.isMoveStatusFinished(status)) {
                StorageNotification.this.onMoveFinished(move, status);
            } else {
                StorageNotification.this.onMoveProgress(move, status, estMillis);
            }
        }
    };
    /* access modifiers changed from: private */
    public final SparseArray<MoveInfo> mMoves = new SparseArray<>();
    /* access modifiers changed from: private */
    public NotificationManager mNotificationManager;
    private final BroadcastReceiver mSnoozeReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            StorageNotification.this.mStorageManager.setVolumeSnoozed(intent.getStringExtra("android.os.storage.extra.FS_UUID"), true);
        }
    };
    /* access modifiers changed from: private */
    public StorageManager mStorageManager;

    private static class MoveInfo {
        public Bundle extras;
        public String label;
        public int moveId;
        public String packageName;
        public String volumeUuid;

        private MoveInfo() {
        }
    }

    public void start() {
        this.mNotificationManager = (NotificationManager) this.mContext.getSystemService(NotificationManager.class);
        this.mStorageManager = (StorageManager) this.mContext.getSystemService(StorageManager.class);
        this.mStorageManager.registerListener(this.mListener);
        this.mContext.registerReceiver(this.mSnoozeReceiver, new IntentFilter("com.android.systemui.action.SNOOZE_VOLUME"), "android.permission.MOUNT_UNMOUNT_FILESYSTEMS", null);
        this.mContext.registerReceiver(this.mFinishReceiver, new IntentFilter("com.android.systemui.action.FINISH_WIZARD"), "android.permission.MOUNT_UNMOUNT_FILESYSTEMS", null);
        for (DiskInfo disk : this.mStorageManager.getDisks()) {
            onDiskScannedInternal(disk, disk.volumeCount);
        }
        for (VolumeInfo vol : this.mStorageManager.getVolumes()) {
            onVolumeStateChangedInternal(vol);
        }
        this.mContext.getPackageManager().registerMoveCallback(this.mMoveCallback, new Handler());
        updateMissingPrivateVolumes();
    }

    private void updateMissingPrivateVolumes() {
        if (!isTv()) {
            for (VolumeRecord rec : this.mStorageManager.getVolumeRecords()) {
                if (rec.getType() == 1) {
                    String fsUuid = rec.getFsUuid();
                    VolumeInfo info = this.mStorageManager.findVolumeByUuid(fsUuid);
                    if ((info == null || !info.isMountedWritable()) && !rec.isSnoozed()) {
                        CharSequence title = this.mContext.getString(17039905, new Object[]{rec.getNickname()});
                        CharSequence text = this.mContext.getString(17039904);
                        Notification.Builder builder = NotificationCompat.newBuilder(this.mContext, NotificationChannels.STORAGE).setSmallIcon(17302746).setColor(this.mContext.getColor(17170799)).setContentTitle(title).setContentText(text).setContentIntent(buildForgetPendingIntent(rec)).setStyle(new Notification.BigTextStyle().bigText(text)).setVisibility(1).setLocalOnly(true).setCategory("sys").setDeleteIntent(buildSnoozeIntent(fsUuid));
                        SystemUI.overrideNotificationAppName(this.mContext, builder);
                        this.mNotificationManager.notifyAsUser(fsUuid, 1397772886, builder.build(), UserHandle.ALL);
                    } else {
                        this.mNotificationManager.cancelAsUser(fsUuid, 1397772886, UserHandle.ALL);
                    }
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void onDiskScannedInternal(DiskInfo disk, int volumeCount) {
        if (volumeCount != 0 || disk.size <= 0) {
            this.mNotificationManager.cancelAsUser(disk.getId(), 1396986699, UserHandle.ALL);
            return;
        }
        CharSequence title = this.mContext.getString(17039934, new Object[]{disk.getDescription()});
        CharSequence text = this.mContext.getString(17039933, new Object[]{disk.getDescription()});
        Notification.Builder builder = NotificationCompat.newBuilder(this.mContext, NotificationChannels.STORAGE).setSmallIcon(getSmallIcon(disk, 6)).setColor(this.mContext.getColor(17170799)).setContentTitle(title).setContentText(text).setContentIntent(buildInitPendingIntent(disk)).setStyle(new Notification.BigTextStyle().bigText(text)).setVisibility(1).setLocalOnly(true).setCategory("err");
        SystemUI.overrideNotificationAppName(this.mContext, builder);
        this.mNotificationManager.notifyAsUser(disk.getId(), 1396986699, builder.build(), UserHandle.ALL);
    }

    /* access modifiers changed from: private */
    public void onDiskDestroyedInternal(DiskInfo disk) {
        this.mNotificationManager.cancelAsUser(disk.getId(), 1396986699, UserHandle.ALL);
    }

    /* access modifiers changed from: private */
    public void onVolumeStateChangedInternal(VolumeInfo vol) {
        switch (vol.getType()) {
            case 0:
                onPublicVolumeStateChangedInternal(vol);
                return;
            case 1:
                onPrivateVolumeStateChangedInternal(vol);
                return;
            default:
                return;
        }
    }

    private void onPrivateVolumeStateChangedInternal(VolumeInfo vol) {
        Log.d("StorageNotification", "Notifying about private volume: " + vol.toString());
        updateMissingPrivateVolumes();
    }

    private void onPublicVolumeStateChangedInternal(VolumeInfo vol) {
        Notification notif;
        Log.d("StorageNotification", "Notifying about public volume: " + vol.toString());
        switch (vol.getState()) {
            case 0:
                notif = onVolumeUnmounted(vol);
                break;
            case 1:
                notif = onVolumeChecking(vol);
                break;
            case 2:
            case 3:
                notif = onVolumeMounted(vol);
                break;
            case 4:
                notif = onVolumeFormatting(vol);
                break;
            case 5:
                notif = onVolumeEjecting(vol);
                break;
            case 6:
                notif = onVolumeUnmountable(vol);
                break;
            case 7:
                notif = onVolumeRemoved(vol);
                break;
            case 8:
                notif = onVolumeBadRemoval(vol);
                break;
            default:
                notif = null;
                break;
        }
        if (notif != null) {
            this.mNotificationManager.notifyAsUser(vol.getId(), 1397773634, notif, UserHandle.ALL);
        } else {
            this.mNotificationManager.cancelAsUser(vol.getId(), 1397773634, UserHandle.ALL);
        }
    }

    private Notification onVolumeUnmounted(VolumeInfo vol) {
        return null;
    }

    private Notification onVolumeChecking(VolumeInfo vol) {
        DiskInfo disk = vol.getDisk();
        return buildNotificationBuilder(vol, this.mContext.getString(17039902, new Object[]{disk.getDescription()}), this.mContext.getString(17039901, new Object[]{disk.getDescription()})).setCategory("progress").setOngoing(true).build();
    }

    private Notification onVolumeMounted(VolumeInfo vol) {
        VolumeRecord rec = this.mStorageManager.findRecordByUuid(vol.getFsUuid());
        DiskInfo disk = vol.getDisk();
        if (rec.isSnoozed() && disk.isAdoptable()) {
            return null;
        }
        CharSequence title = disk.getDescription();
        CharSequence text = this.mContext.getString(17039916, new Object[]{disk.getDescription()});
        Notification.Builder builder = buildNotificationBuilder(vol, title, text).addAction(new Notification.Action(17302374, this.mContext.getString(17039928), buildUnmountPendingIntent(vol))).setContentIntent(buildBrowsePendingIntent(vol)).setCategory("sys").setPriority(-1);
        Bundle bundle = new Bundle();
        bundle.putBoolean("miui.showAction", true);
        builder.setExtras(bundle);
        if (disk.isAdoptable()) {
            builder.setDeleteIntent(buildSnoozeIntent(vol.getFsUuid()));
        }
        if (disk.isSd()) {
            builder.setAutoCancel(true);
        } else {
            builder.setOngoing(true);
        }
        NotificationCompat.setChannelId(builder, NotificationChannels.STORAGE);
        return builder.build();
    }

    private Notification onVolumeFormatting(VolumeInfo vol) {
        return null;
    }

    private Notification onVolumeEjecting(VolumeInfo vol) {
        DiskInfo disk = vol.getDisk();
        return buildNotificationBuilder(vol, this.mContext.getString(17039932, new Object[]{disk.getDescription()}), this.mContext.getString(17039931, new Object[]{disk.getDescription()})).setCategory("progress").setOngoing(true).build();
    }

    private Notification onVolumeUnmountable(VolumeInfo vol) {
        DiskInfo disk = vol.getDisk();
        return buildNotificationBuilder(vol, this.mContext.getString(17039930, new Object[]{disk.getDescription()}), this.mContext.getString(17039929, new Object[]{disk.getDescription()})).setContentIntent(buildInitPendingIntent(vol)).setCategory("err").build();
    }

    private Notification onVolumeRemoved(VolumeInfo vol) {
        if (!vol.isPrimary()) {
            return null;
        }
        DiskInfo disk = vol.getDisk();
        return buildNotificationBuilder(vol, this.mContext.getString(17039915, new Object[]{disk.getDescription()}), this.mContext.getString(17039914, new Object[]{disk.getDescription()})).setCategory("err").build();
    }

    private Notification onVolumeBadRemoval(VolumeInfo vol) {
        if (!vol.isPrimary()) {
            return null;
        }
        DiskInfo disk = vol.getDisk();
        return buildNotificationBuilder(vol, this.mContext.getString(17039899, new Object[]{disk.getDescription()}), this.mContext.getString(17039898, new Object[]{disk.getDescription()})).setCategory("err").build();
    }

    /* access modifiers changed from: private */
    public void onMoveProgress(MoveInfo move, int status, long estMillis) {
        CharSequence title;
        CharSequence text;
        PendingIntent intent;
        if (!TextUtils.isEmpty(move.label)) {
            title = this.mContext.getString(17039908, new Object[]{move.label});
        } else {
            title = this.mContext.getString(17039911);
        }
        if (estMillis < 0) {
            text = null;
        } else {
            text = DateUtils.formatDuration(estMillis);
        }
        if (move.packageName != null) {
            intent = buildWizardMovePendingIntent(move);
        } else {
            intent = buildWizardMigratePendingIntent(move);
        }
        Notification.Builder builder = NotificationCompat.newBuilder(this.mContext, NotificationChannels.STORAGE).setSmallIcon(17302746).setColor(this.mContext.getColor(17170799)).setContentTitle(title).setContentText(text).setContentIntent(intent).setStyle(new Notification.BigTextStyle().bigText(text)).setVisibility(1).setLocalOnly(true).setCategory("progress").setProgress(100, status, false).setOngoing(true);
        SystemUI.overrideNotificationAppName(this.mContext, builder);
        this.mNotificationManager.notifyAsUser(move.packageName, 1397575510, builder.build(), UserHandle.ALL);
    }

    /* access modifiers changed from: private */
    public void onMoveFinished(MoveInfo move, int status) {
        CharSequence text;
        CharSequence title;
        PendingIntent intent;
        if (move.packageName != null) {
            this.mNotificationManager.cancelAsUser(move.packageName, 1397575510, UserHandle.ALL);
            return;
        }
        VolumeInfo privateVol = this.mContext.getPackageManager().getPrimaryStorageCurrentVolume();
        String descrip = this.mStorageManager.getBestVolumeDescription(privateVol);
        if (status == -100) {
            title = this.mContext.getString(17039910);
            text = this.mContext.getString(17039909, new Object[]{descrip});
        } else {
            title = this.mContext.getString(17039907);
            text = this.mContext.getString(17039906);
        }
        if (privateVol != null && privateVol.getDisk() != null) {
            intent = buildWizardReadyPendingIntent(privateVol.getDisk());
        } else if (privateVol != null) {
            intent = buildVolumeSettingsPendingIntent(privateVol);
        } else {
            intent = null;
        }
        Notification.Builder builder = NotificationCompat.newBuilder(this.mContext, NotificationChannels.STORAGE).setSmallIcon(17302746).setColor(this.mContext.getColor(17170799)).setContentTitle(title).setContentText(text).setContentIntent(intent).setStyle(new Notification.BigTextStyle().bigText(text)).setVisibility(1).setLocalOnly(true).setCategory("sys").setAutoCancel(true);
        SystemUI.overrideNotificationAppName(this.mContext, builder);
        this.mNotificationManager.notifyAsUser(move.packageName, 1397575510, builder.build(), UserHandle.ALL);
    }

    private int getSmallIcon(DiskInfo disk, int state) {
        if (disk.isSd()) {
            return (state == 1 || state == 5) ? 17302746 : 17302746;
        }
        if (disk.isUsb()) {
            return 17302779;
        }
        return 17302746;
    }

    private Notification.Builder buildNotificationBuilder(VolumeInfo vol, CharSequence title, CharSequence text) {
        Notification.Builder builder = NotificationCompat.newBuilder(this.mContext, NotificationChannels.STORAGE).setSmallIcon(getSmallIcon(vol.getDisk(), vol.getState())).setColor(this.mContext.getColor(17170799)).setContentTitle(title).setContentText(text).setStyle(new Notification.BigTextStyle().bigText(text)).setVisibility(1).setLocalOnly(true);
        overrideNotificationAppName(this.mContext, builder);
        return builder;
    }

    private PendingIntent buildInitPendingIntent(DiskInfo disk) {
        Intent intent = new Intent();
        if (isTv()) {
            intent.setPackage("com.android.tv.settings");
            intent.setAction("com.android.tv.settings.action.NEW_STORAGE");
        } else {
            intent.setClassName("com.android.settings", "com.android.settings.deviceinfo.StorageWizardInit");
        }
        intent.putExtra("android.os.storage.extra.DISK_ID", disk.getId());
        return PendingIntent.getActivityAsUser(this.mContext, disk.getId().hashCode(), intent, 268435456, null, UserHandle.CURRENT);
    }

    private PendingIntent buildInitPendingIntent(VolumeInfo vol) {
        Intent intent = new Intent();
        if (isTv()) {
            intent.setPackage("com.android.tv.settings");
            intent.setAction("com.android.tv.settings.action.NEW_STORAGE");
        } else {
            intent.setClassName("com.android.settings", "com.android.settings.deviceinfo.StorageWizardInit");
        }
        intent.putExtra("android.os.storage.extra.VOLUME_ID", vol.getId());
        return PendingIntent.getActivityAsUser(this.mContext, vol.getId().hashCode(), intent, 268435456, null, UserHandle.CURRENT);
    }

    private PendingIntent buildUnmountPendingIntent(VolumeInfo vol) {
        Intent intent = new Intent();
        if (isTv()) {
            intent.setPackage("com.android.tv.settings");
            intent.setAction("com.android.tv.settings.action.UNMOUNT_STORAGE");
            intent.putExtra("android.os.storage.extra.VOLUME_ID", vol.getId());
            return PendingIntent.getActivityAsUser(this.mContext, vol.getId().hashCode(), intent, 268435456, null, UserHandle.CURRENT);
        }
        intent.setClassName("com.android.settings", "com.android.settings.deviceinfo.StorageUnmountReceiver");
        intent.putExtra("android.os.storage.extra.VOLUME_ID", vol.getId());
        return PendingIntent.getBroadcastAsUser(this.mContext, vol.getId().hashCode(), intent, 268435456, UserHandle.CURRENT);
    }

    private PendingIntent buildBrowsePendingIntent(VolumeInfo vol) {
        File file = vol.getPathForUser(UserHandle.myUserId());
        String path = null;
        if (file != null) {
            path = file.getPath();
        }
        Intent intent = new Intent();
        if (Util.isGlobalFileExplorerExist(this.mContext)) {
            intent.setClassName("com.mi.android.globalFileexplorer", "com.android.fileexplorer.FileExplorerTabActivity");
        } else if (Util.isCNFileExplorerExist(this.mContext)) {
            intent.setClassName("com.android.fileexplorer", "com.android.fileexplorer.FileExplorerTabActivity");
        }
        intent.setFlags(268435456);
        intent.putExtra("current_directory", path);
        return PendingIntent.getActivityAsUser(this.mContext, vol.getId().hashCode(), intent, 268435456, null, UserHandle.CURRENT);
    }

    private PendingIntent buildVolumeSettingsPendingIntent(VolumeInfo vol) {
        Intent intent = new Intent();
        if (isTv()) {
            intent.setPackage("com.android.tv.settings");
            intent.setAction("android.settings.INTERNAL_STORAGE_SETTINGS");
        } else {
            switch (vol.getType()) {
                case 0:
                    intent.setClassName("com.android.settings", "com.android.settings.Settings$PublicVolumeSettingsActivity");
                    break;
                case 1:
                    intent.setClassName("com.android.settings", "com.android.settings.Settings$PrivateVolumeSettingsActivity");
                    break;
                default:
                    return null;
            }
        }
        intent.putExtra("android.os.storage.extra.VOLUME_ID", vol.getId());
        return PendingIntent.getActivityAsUser(this.mContext, vol.getId().hashCode(), intent, 268435456, null, UserHandle.CURRENT);
    }

    private PendingIntent buildSnoozeIntent(String fsUuid) {
        Intent intent = new Intent("com.android.systemui.action.SNOOZE_VOLUME");
        intent.putExtra("android.os.storage.extra.FS_UUID", fsUuid);
        return PendingIntent.getBroadcastAsUser(this.mContext, fsUuid.hashCode(), intent, 268435456, UserHandle.CURRENT);
    }

    private PendingIntent buildForgetPendingIntent(VolumeRecord rec) {
        Intent intent = new Intent();
        intent.setClassName("com.android.settings", "com.android.settings.Settings$PrivateVolumeForgetActivity");
        intent.putExtra("android.os.storage.extra.FS_UUID", rec.getFsUuid());
        return PendingIntent.getActivityAsUser(this.mContext, rec.getFsUuid().hashCode(), intent, 268435456, null, UserHandle.CURRENT);
    }

    private PendingIntent buildWizardMigratePendingIntent(MoveInfo move) {
        Intent intent = new Intent();
        if (isTv()) {
            intent.setPackage("com.android.tv.settings");
            intent.setAction("com.android.tv.settings.action.MIGRATE_STORAGE");
        } else {
            intent.setClassName("com.android.settings", "com.android.settings.deviceinfo.StorageWizardMigrateProgress");
        }
        intent.putExtra("android.content.pm.extra.MOVE_ID", move.moveId);
        VolumeInfo vol = this.mStorageManager.findVolumeByQualifiedUuid(move.volumeUuid);
        if (vol != null) {
            intent.putExtra("android.os.storage.extra.VOLUME_ID", vol.getId());
        }
        return PendingIntent.getActivityAsUser(this.mContext, move.moveId, intent, 268435456, null, UserHandle.CURRENT);
    }

    private PendingIntent buildWizardMovePendingIntent(MoveInfo move) {
        Intent intent = new Intent();
        if (isTv()) {
            intent.setPackage("com.android.tv.settings");
            intent.setAction("com.android.tv.settings.action.MOVE_APP");
        } else {
            intent.setClassName("com.android.settings", "com.android.settings.deviceinfo.StorageWizardMoveProgress");
        }
        intent.putExtra("android.content.pm.extra.MOVE_ID", move.moveId);
        return PendingIntent.getActivityAsUser(this.mContext, move.moveId, intent, 268435456, null, UserHandle.CURRENT);
    }

    private PendingIntent buildWizardReadyPendingIntent(DiskInfo disk) {
        Intent intent = new Intent();
        if (isTv()) {
            intent.setPackage("com.android.tv.settings");
            intent.setAction("android.settings.INTERNAL_STORAGE_SETTINGS");
        } else {
            intent.setClassName("com.android.settings", "com.android.settings.deviceinfo.StorageWizardReady");
        }
        intent.putExtra("android.os.storage.extra.DISK_ID", disk.getId());
        return PendingIntent.getActivityAsUser(this.mContext, disk.getId().hashCode(), intent, 268435456, null, UserHandle.CURRENT);
    }

    private boolean isTv() {
        return this.mContext.getPackageManager().hasSystemFeature("android.software.leanback");
    }
}
