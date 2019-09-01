package com.miui.browser.webapps;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.net.Uri;
import java.util.HashMap;
import java.util.Map;

public class WebAppDAO {
    private static WebAppDAO sInstance = null;
    private ContentResolver mContentResolver = null;
    private DataChangeBroadcastReceiver mDataChangeBroadcastReceiver = new DataChangeBroadcastReceiver();
    private boolean mIsReady = false;
    private Map<String, WebAppInfo> mMap = new HashMap(6);

    private class DataChangeBroadcastReceiver extends BroadcastReceiver {
        private DataChangeBroadcastReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if ("miui.browser.webapps.action.DATA_CHANGE".equals(intent.getAction())) {
                WebAppDAO.this.reset();
                WebAppDAO.this.query();
            }
        }
    }

    private WebAppDAO(Context context) {
        this.mContentResolver = context.getContentResolver();
        context.registerReceiver(this.mDataChangeBroadcastReceiver, new IntentFilter("miui.browser.webapps.action.DATA_CHANGE"));
    }

    public static WebAppDAO getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new WebAppDAO(context.getApplicationContext());
        }
        return sInstance;
    }

    /* access modifiers changed from: private */
    public final void query() {
        Cursor cursor = this.mContentResolver.query(Uri.parse("content://miui.browser.webapps/"), null, null, null, null);
        if (cursor != null) {
            for (boolean flag = cursor.moveToFirst(); flag; flag = cursor.moveToNext()) {
                WebAppInfo info = new WebAppInfo(cursor);
                this.mMap.put(info.mTaskAffinity, info);
            }
            closeCursor(cursor);
        }
        this.mIsReady = true;
    }

    public WebAppInfo get(ActivityInfo activityInfo) {
        if (!isNeedToCheck(activityInfo)) {
            return null;
        }
        if (!isReady()) {
            query();
        }
        return get(activityInfo.taskAffinity);
    }

    private WebAppInfo get(String affinity) {
        if (affinity == null) {
            return null;
        }
        return this.mMap.get(affinity);
    }

    private final boolean isNeedToCheck(ActivityInfo activityInfo) {
        if (activityInfo != null && "com.android.browser".equals(activityInfo.packageName) && activityInfo.taskAffinity != null && activityInfo.taskAffinity.startsWith("miui.browser.webapps.app")) {
            return true;
        }
        return false;
    }

    private final boolean isReady() {
        return this.mIsReady;
    }

    public final void reset() {
        this.mIsReady = false;
        this.mMap.clear();
    }

    private void closeCursor(Cursor cursor) {
        if (cursor != null) {
            try {
                if (!cursor.isClosed()) {
                    cursor.close();
                }
            } catch (Exception e) {
            }
        }
    }
}
