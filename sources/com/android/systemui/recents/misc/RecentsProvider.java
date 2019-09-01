package com.android.systemui.recents.misc;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;
import java.util.List;

public class RecentsProvider extends ContentProvider {
    private static final UriMatcher sURIMatcher = new UriMatcher(-1);
    private MatrixCursor mForceMultiWindowPkgCursor;
    private MatrixCursor mForceNotMultiWindowPkgCursor;

    static {
        sURIMatcher.addURI("com.miui.systemui.recents", "MULTI_WINDOW_FORCE_RESIZE_PKGS", 1);
        sURIMatcher.addURI("com.miui.systemui.recents", "MULTI_WINDOW_FORCE_NOT_RESIZE_PKGS", 2);
    }

    public boolean onCreate() {
        return true;
    }

    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Log.d("RecentsProvider", "query uri=" + uri);
        switch (sURIMatcher.match(uri)) {
            case 1:
                if (this.mForceMultiWindowPkgCursor == null) {
                    List<String> multiWindowForceResizeList = SystemServicesProxy.getMultiWindowForceResizeList(getContext());
                    this.mForceMultiWindowPkgCursor = new MatrixCursor(new String[]{"pkgs"});
                    for (int i = 0; i < multiWindowForceResizeList.size(); i++) {
                        this.mForceMultiWindowPkgCursor.addRow(new String[]{multiWindowForceResizeList.get(i)});
                    }
                }
                return this.mForceMultiWindowPkgCursor;
            case 2:
                if (this.mForceNotMultiWindowPkgCursor == null) {
                    List<String> multiWindowForceNotResizeList = SystemServicesProxy.getMultiWindowForceNotResizeList(getContext());
                    this.mForceNotMultiWindowPkgCursor = new MatrixCursor(new String[]{"pkgs"});
                    for (int i2 = 0; i2 < multiWindowForceNotResizeList.size(); i2++) {
                        this.mForceNotMultiWindowPkgCursor.addRow(new String[]{multiWindowForceNotResizeList.get(i2)});
                    }
                }
                return this.mForceNotMultiWindowPkgCursor;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    public String getType(Uri uri) {
        return null;
    }

    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
