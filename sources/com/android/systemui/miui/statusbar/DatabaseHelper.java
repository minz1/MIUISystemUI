package com.android.systemui.miui.statusbar;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.StrictMode;
import android.util.Log;
import com.android.systemui.Constants;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static volatile DatabaseHelper mDatabaseHelper;

    private DatabaseHelper(Context context) {
        super(context, "systemui.db", null, 14);
    }

    public static DatabaseHelper getInstance(Context context) {
        if (mDatabaseHelper == null) {
            synchronized (DatabaseHelper.class) {
                if (mDatabaseHelper == null) {
                    mDatabaseHelper = new DatabaseHelper(context);
                }
            }
        }
        return mDatabaseHelper;
    }

    public void onCreate(SQLiteDatabase db) {
        if (Constants.DEBUG) {
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectLeakedSqlLiteObjects().detectLeakedClosableObjects().penaltyLog().penaltyDeath().build());
        }
        db.execSQL("CREATE TABLE IF NOT EXISTS notification_sort(_id INTEGER PRIMARY KEY AUTOINCREMENT, package_name VARCHAR(40) NOT NULL, date INTEGER, click_count INTEGER, show_count INTEGER);");
        db.execSQL("CREATE TABLE  IF NOT EXISTS notifications (_id INTEGER PRIMARY KEY AUTOINCREMENT,icon BLOB,title TEXT,content TEXT,time TEXT,info TEXT,subtext TEXT,key INTEGER,pkg TEXT,user_id INTEGER);");
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int currentVersion) {
        Log.w("DatabaseHelper", "Upgrading settings database from version " + oldVersion + " to " + currentVersion);
        if (oldVersion != currentVersion) {
            Log.w("DatabaseHelper", "Got stuck trying to upgrade from version " + oldVersion + ", must wipe the settings provider");
            if (oldVersion < 13) {
                db.execSQL("DROP TABLE IF EXISTS notifications");
                db.execSQL("DROP TABLE IF EXISTS notification_sort");
                oldVersion = 13;
            }
            if (oldVersion == 13) {
                db.execSQL("DROP TABLE IF EXISTS notifications");
            }
            onCreate(db);
        }
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int currentVersion) {
        if (oldVersion != currentVersion) {
            Log.w("DatabaseHelper", "Got stuck trying to upgrade from version " + oldVersion + ", must wipe the settings provider");
            db.execSQL("DROP TABLE IF EXISTS notifications");
            db.execSQL("DROP TABLE IF EXISTS notification_sort");
            onCreate(db);
        }
    }
}
