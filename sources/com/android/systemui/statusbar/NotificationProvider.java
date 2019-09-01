package com.android.systemui.statusbar;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;
import com.android.systemui.miui.statusbar.DatabaseHelper;
import miui.util.NotificationFilterHelper;

public class NotificationProvider extends ContentProvider {
    private static final UriMatcher sMatcher = new UriMatcher(-1);
    protected DatabaseHelper mOpenHelper;

    static {
        sMatcher.addURI("keyguard.notification", "notifications", 1);
        sMatcher.addURI("keyguard.notification", "notifications/#", 2);
        sMatcher.addURI("keyguard.notification", "app_corner", 3);
    }

    public boolean onCreate() {
        this.mOpenHelper = DatabaseHelper.getInstance(getContext());
        return true;
    }

    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        NotificationProvider notificationProvider;
        Uri uri2 = uri;
        String[] strArr = projection;
        SQLiteDatabase db = openDB();
        if (db == null) {
            return null;
        }
        Cursor cursor = null;
        switch (sMatcher.match(uri2)) {
            case 1:
                notificationProvider = this;
                cursor = db.query("notifications", strArr, selection, selectionArgs, null, null, sortOrder);
                break;
            case 2:
                StringBuilder sb = new StringBuilder();
                sb.append("_id=");
                sb.append(uri.getPathSegments().get(1));
                notificationProvider = this;
                sb.append(notificationProvider.parseSelection(selection));
                cursor = db.query("notifications", strArr, sb.toString(), selectionArgs, null, null, sortOrder);
                break;
            case 3:
                if (strArr != null && strArr.length >= 1) {
                    MatrixCursor matrixCursor = new MatrixCursor(new String[]{"show_corner"});
                    for (String isAllowed : strArr) {
                        matrixCursor.addRow(new String[]{String.valueOf((int) NotificationFilterHelper.isAllowed(getContext(), isAllowed, "_message"))});
                    }
                    cursor = matrixCursor;
                    break;
                } else {
                    return null;
                }
                break;
        }
        notificationProvider = this;
        String str = selection;
        if (cursor != null) {
            cursor.setNotificationUri(notificationProvider.getContext().getContentResolver(), uri2);
        }
        return cursor;
    }

    private String parseSelection(String selection) {
        if (TextUtils.isEmpty(selection)) {
            return "";
        }
        return " AND (" + selection + ')';
    }

    public String getType(Uri uri) {
        return null;
    }

    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = openDB();
        if (db == null) {
            return null;
        }
        long rowId = -1;
        if (sMatcher.match(uri) == 1) {
            rowId = db.insert("notifications", null, values);
        }
        return ContentUris.withAppendedId(uri, rowId);
    }

    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = openDB();
        if (db == null) {
            return 0;
        }
        int count = 0;
        if (sMatcher.match(uri) == 1) {
            count = db.delete("notifications", selection, selectionArgs);
        }
        return count;
    }

    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase db = openDB();
        if (db == null) {
            return 0;
        }
        int count = 0;
        if (sMatcher.match(uri) == 1) {
            count = db.update("notifications", values, selection, selectionArgs);
        }
        return count;
    }

    private SQLiteDatabase openDB() {
        try {
            return this.mOpenHelper.getWritableDatabase();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
