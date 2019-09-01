package com.xiaomi.mistatistic.sdk.controller;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/* compiled from: MiStatDatabaseHelper */
public class i extends SQLiteOpenHelper {
    public static final Object a = new Object();

    public i(Context context) {
        super(context, "mistat.db", null, 2);
    }

    public void onCreate(SQLiteDatabase sQLiteDatabase) {
        synchronized (a) {
            sQLiteDatabase.execSQL(String.format("create table %s(_id integer primary key autoincrement, category text, ts integer, key text, value text, type text, extra text, anonymous integer)", new Object[]{"mistat_event"}));
        }
        h.b("MDH", String.format("db onCreate version %d", new Object[]{2}));
    }

    public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
        synchronized (a) {
            if (i == 1) {
                sQLiteDatabase.execSQL(String.format("alter table %s add column %s integer ", new Object[]{"mistat_event", "anonymous"}));
            }
        }
        h.b("MDH", String.format("db onUpgrade %d to %d", new Object[]{Integer.valueOf(i), Integer.valueOf(i2)}));
    }

    /* access modifiers changed from: protected */
    public void finalize() throws Throwable {
        close();
        super.finalize();
    }
}
