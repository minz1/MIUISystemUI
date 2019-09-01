package com.android.systemui.statusbar;

import android.app.Notification;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.widget.DateTimeView;
import com.android.systemui.Dependency;
import com.android.systemui.miui.statusbar.notification.NotificationUtil;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.phone.NotificationGroupManager;
import com.android.systemui.statusbar.policy.KeyguardNotificationController;
import java.io.ByteArrayOutputStream;
import miui.maml.FancyDrawable;
import miui.provider.KeyguardNotification;

public class KeyguardNotificationHelper {
    private Handler mBgHandler = new Handler((Looper) Dependency.get(Dependency.BG_LOOPER)) {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 3000:
                    KeyguardNotificationHelper.this.handleInsertDB((ContentValues) msg.obj);
                    return;
                case 3001:
                    KeyguardNotificationHelper.this.handleUpdateDB((ContentValues) msg.obj);
                    return;
                case 3002:
                    KeyguardNotificationHelper.this.handleDeleteDB(msg.arg1, msg.obj != null ? (String) msg.obj : null);
                    return;
                case 3003:
                    KeyguardNotificationHelper.this.handleClearDB();
                    return;
                default:
                    return;
            }
        }
    };
    private Context mContext;
    private NotificationGroupManager mGroupManager;

    public KeyguardNotificationHelper(Context context, NotificationGroupManager groupManager) {
        this.mContext = context;
        this.mGroupManager = groupManager;
    }

    public void add(NotificationData.Entry entry) {
        if (!entry.notification.getNotification().isGroupSummary()) {
            ExpandableNotificationRow row = this.mGroupManager.getGroupSummary((StatusBarNotification) entry.notification);
            if (row != null) {
                remove(row.getEntry().key.hashCode(), row.getEntry().notification.getPackageName());
            }
        } else if (this.mGroupManager.isSummaryOfGroup(entry.notification)) {
            return;
        }
        ContentValues values = buildValues(entry);
        if (values != null) {
            this.mBgHandler.obtainMessage(3000, values).sendToTarget();
        }
    }

    public void update(NotificationData.Entry entry) {
        ContentValues values = buildValues(entry);
        if (values != null) {
            this.mBgHandler.obtainMessage(3001, values).sendToTarget();
        }
    }

    public void remove(int key, String pkg) {
        this.mBgHandler.obtainMessage(3002, key, 0, pkg).sendToTarget();
    }

    public void clear() {
        this.mBgHandler.obtainMessage(3003).sendToTarget();
    }

    /* access modifiers changed from: private */
    public void handleInsertDB(ContentValues values) {
        String pkg = values.getAsString("pkg");
        ContentResolver resolver = this.mContext.getContentResolver();
        resolver.insert(KeyguardNotification.URI, values);
        resolver.notifyChange(KeyguardNotification.URI, null);
        ((KeyguardNotificationController) Dependency.get(KeyguardNotificationController.class)).add(pkg);
    }

    /* access modifiers changed from: private */
    public void handleUpdateDB(ContentValues values) {
        int key = values.getAsInteger("key").intValue();
        String pkg = values.getAsString("pkg");
        ContentResolver resolver = this.mContext.getContentResolver();
        resolver.update(KeyguardNotification.URI, values, "key" + "=" + key, null);
        resolver.notifyChange(KeyguardNotification.URI, null);
        ((KeyguardNotificationController) Dependency.get(KeyguardNotificationController.class)).update(pkg);
    }

    /* access modifiers changed from: private */
    public void handleDeleteDB(int key, String pkg) {
        ContentResolver resolver = this.mContext.getContentResolver();
        if (resolver.delete(KeyguardNotification.URI, "key" + "=" + key, null) > 0) {
            resolver.notifyChange(KeyguardNotification.URI, null);
            if (!TextUtils.isEmpty(pkg)) {
                ((KeyguardNotificationController) Dependency.get(KeyguardNotificationController.class)).delete(pkg);
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleClearDB() {
        ContentResolver resolver = this.mContext.getContentResolver();
        resolver.delete(KeyguardNotification.URI, null, null);
        resolver.notifyChange(KeyguardNotification.URI, null);
        ((KeyguardNotificationController) Dependency.get(KeyguardNotificationController.class)).clearAll();
    }

    private ContentValues buildValues(NotificationData.Entry entry) {
        byte[] icon = getByteIcon(entry);
        if (icon == null) {
            return null;
        }
        boolean sensitive = entry.hideSensitive || entry.hideSensitiveByAppLock;
        Notification n = entry.notification.getNotification();
        CharSequence title = sensitive ? entry.notification.getAppName() : NotificationUtil.resolveTitle(n);
        CharSequence content = sensitive ? NotificationUtil.getHiddenText(this.mContext) : NotificationUtil.resolveText(n);
        CharSequence subtext = sensitive ? "" : NotificationUtil.resolveSubText(n);
        ContentValues values = new ContentValues();
        values.put("icon", icon);
        values.put("title", TextUtils.isEmpty(title) ? "" : title.toString());
        values.put("content", TextUtils.isEmpty(content) ? "" : content.toString());
        values.put("time", getTimeText(entry));
        values.put("info", "");
        values.put("subtext", TextUtils.isEmpty(subtext) ? "" : subtext.toString());
        values.put("key", Integer.valueOf(entry.key.hashCode()));
        values.put("pkg", entry.notification.getPackageName());
        values.put("user_id", Integer.valueOf(entry.notification.getUserId()));
        return values;
    }

    /* JADX WARNING: type inference failed for: r2v3, types: [android.view.View] */
    /* JADX WARNING: type inference failed for: r2v4, types: [android.view.View] */
    /* JADX WARNING: Multi-variable type inference failed */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private byte[] getByteIcon(com.android.systemui.statusbar.NotificationData.Entry r5) {
        /*
            r4 = this;
            r0 = 0
            boolean r1 = r5.hideSensitive
            r2 = 16909273(0x10203d9, float:2.387999E-38)
            if (r1 != 0) goto L_0x0023
            boolean r1 = r5.hideSensitiveByAppLock
            if (r1 == 0) goto L_0x000d
            goto L_0x0023
        L_0x000d:
            android.view.View r1 = r5.getPrivateView()
            com.android.systemui.statusbar.NotificationContentView r1 = (com.android.systemui.statusbar.NotificationContentView) r1
            if (r1 == 0) goto L_0x0031
            android.view.View r3 = r1.getContractedChild()
            if (r3 == 0) goto L_0x0031
            android.view.View r2 = r3.findViewById(r2)
            r0 = r2
            android.widget.ImageView r0 = (android.widget.ImageView) r0
            goto L_0x0031
        L_0x0023:
            android.view.View r1 = r5.getPublicContentView()
            if (r1 == 0) goto L_0x0030
            android.view.View r2 = r1.findViewById(r2)
            r0 = r2
            android.widget.ImageView r0 = (android.widget.ImageView) r0
        L_0x0030:
        L_0x0031:
            if (r0 == 0) goto L_0x0038
            android.graphics.drawable.Drawable r1 = r0.getDrawable()
            goto L_0x0039
        L_0x0038:
            r1 = 0
        L_0x0039:
            if (r1 != 0) goto L_0x0041
            com.android.systemui.miui.statusbar.ExpandedNotification r2 = r5.notification
            android.graphics.drawable.Drawable r1 = r2.getAppIcon()
        L_0x0041:
            byte[] r2 = r4.drawableToByte(r1)
            return r2
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.KeyguardNotificationHelper.getByteIcon(com.android.systemui.statusbar.NotificationData$Entry):byte[]");
    }

    private byte[] drawableToByte(Drawable drawable) {
        if (drawable == null) {
            return null;
        }
        if (drawable instanceof FancyDrawable) {
            Drawable quiet = ((FancyDrawable) drawable).getQuietDrawable();
            if (quiet == null) {
                ((FancyDrawable) drawable).getRoot().tick(SystemClock.elapsedRealtime());
            } else {
                drawable = quiet;
            }
        }
        int w = drawable.getIntrinsicWidth();
        int h = drawable.getIntrinsicHeight();
        if (w <= 0 || h <= 0) {
            return null;
        }
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, w, h);
        drawable.draw(canvas);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return baos.toByteArray();
    }

    private String getTimeText(NotificationData.Entry entry) {
        DateTimeView dateTime = new DateTimeView(this.mContext);
        if (entry.notification.getNotification().when != 0) {
            dateTime.setTime(entry.notification.getNotification().when);
        }
        return dateTime.getText().toString();
    }
}
