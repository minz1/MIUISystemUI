package com.android.systemui.miui.statusbar.phone.rank;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.LruCache;
import com.android.systemui.Constants;
import com.android.systemui.DateUtils;
import com.android.systemui.miui.statusbar.DatabaseHelper;
import com.miui.systemui.annotation.Inject;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PackageScoreCache {
    private static final long DAYS_TO_MILLIS = TimeUnit.DAYS.toMillis(1);
    private static final boolean DEBUG = Constants.DEBUG;
    /* access modifiers changed from: private */
    public Handler mBgHandler;
    private int mCurrentDays;
    private DatabaseHelper mOpenHelper;
    private RankLruCache<String, PackageEntity> mPkgEntities = new RankLruCache<>(64);
    private int mTotalClickCount;
    private int mTotalShowCount;

    private class RankLruCache<K, V> extends LruCache<K, V> {
        public RankLruCache(int maxSize) {
            super(maxSize);
        }

        /* access modifiers changed from: protected */
        public void entryRemoved(boolean evicted, K k, V oldValue, V v) {
            if (evicted && (oldValue instanceof PackageEntity)) {
                final PackageEntity entity = (PackageEntity) oldValue;
                PackageScoreCache.this.mBgHandler.post(new Runnable() {
                    public void run() {
                        PackageScoreCache.this.updateEntity(entity);
                    }
                });
            }
        }
    }

    public PackageScoreCache(@Inject Context context, @Inject(tag = "SysUiBg") Looper bgLooper) {
        this.mOpenHelper = DatabaseHelper.getInstance(context);
        this.mBgHandler = new Handler(bgLooper);
    }

    public boolean containsPkg(String pkgName) {
        return getPkgEntity(pkgName) != null;
    }

    public PackageEntity getPkgEntity(String pkgName) {
        return (PackageEntity) this.mPkgEntities.get(pkgName);
    }

    public int getTotalShowCount() {
        return this.mTotalShowCount;
    }

    public int getTotalClickCount() {
        return this.mTotalClickCount;
    }

    public void asyncUpdate() {
        this.mBgHandler.post(new Runnable() {
            public void run() {
                PackageScoreCache.this.updateAll();
            }
        });
    }

    /* access modifiers changed from: private */
    public void updateEntity(PackageEntity entity) {
        if (entity.isDataChanged()) {
            SQLiteDatabase db = openDB();
            if (db != null) {
                insertOrUpdate(db, entity);
                closeDB(db);
            }
        }
    }

    /* access modifiers changed from: private */
    public void updateAll() {
        SQLiteDatabase db = openDB();
        if (db != null) {
            Map<String, PackageEntity> entities = this.mPkgEntities.snapshot();
            try {
                writeToDatabase(db, entities);
            } catch (Exception e) {
                Log.d("packageScoreCache", "updateAll Exception " + e);
            }
            if (isDateChanged()) {
                removeExpiredData(db);
                updateEntryData(db, entities);
                updateLocalData(db);
            }
            closeDB(db);
        }
    }

    private void writeToDatabase(SQLiteDatabase db, Map<String, PackageEntity> entityMap) {
        db.beginTransaction();
        try {
            for (Map.Entry<String, PackageEntity> packageEntityEntry : entityMap.entrySet()) {
                PackageEntity packageEntity = packageEntityEntry.getValue();
                if (packageEntity.isDataChanged()) {
                    insertOrUpdate(db, packageEntity);
                    packageEntity.setDataChanged(false);
                }
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e("packageScoreCache", "writeToDatabase Exception " + e);
        } catch (Throwable th) {
            db.endTransaction();
            throw th;
        }
        db.endTransaction();
    }

    private void insertOrUpdate(SQLiteDatabase db, PackageEntity pkgEntity) {
        int today = DateUtils.getDigitalFormatDateToday();
        String pkgName = pkgEntity.getPackageName();
        if (DEBUG) {
            Log.d("packageScoreCache", "insertOrUpdate " + sql.toString());
        }
        try {
            db.execSQL(" INSERT OR REPLACE INTO notification_sort " + " (_id, package_name, date, click_count, show_count) " + " VALUES((SELECT _id FROM notification_sort " + " WHERE package_name = '" + pkgName + "' " + " AND date = " + today + ") " + " , '" + pkgName + "' " + " , " + today + " , " + pkgEntity.getDailyClick() + " , " + pkgEntity.getDailyShow() + ") ");
        } catch (Exception e) {
            Log.d("packageScoreCache", "insertOrUpdate Exception " + e);
        }
    }

    private void removeExpiredData(SQLiteDatabase db) {
        int oneMonthAgo = DateUtils.getDigitalPreviousMonthDate();
        if (DEBUG) {
            Log.d("packageScoreCache", "removeExpiredData " + sql.toString());
        }
        try {
            db.execSQL(" DELETE FROM notification_sort " + " WHERE date < " + oneMonthAgo);
        } catch (Exception e) {
            Log.d("packageScoreCache", "removeExpiredData Exception " + e);
        }
    }

    private void updateEntryData(SQLiteDatabase db, Map<String, PackageEntity> entityMap) {
        int today = DateUtils.getDigitalFormatDateToday();
        if (DEBUG) {
            Log.d("packageScoreCache", "updateEntryData " + sql.toString());
        }
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(" SELECT package_name, SUM(click_count), SUM(show_count) FROM notification_sort " + " WHERE date < " + today + " GROUP BY package_name ", null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String pkgName = cursor.getString(0);
                    int clickCount = cursor.getInt(1);
                    int showCount = cursor.getInt(2);
                    PackageEntity entity = entityMap.get(pkgName);
                    if (entity != null) {
                        entity.onDateChanged(clickCount, showCount);
                    }
                }
            }
        } catch (Exception e) {
            Log.d("packageScoreCache", "updateEntryData exception " + e);
        } catch (Throwable th) {
            closeCursor(null);
            throw th;
        }
        closeCursor(cursor);
    }

    private void updateLocalData(SQLiteDatabase db) {
        int today = DateUtils.getDigitalFormatDateToday();
        if (DEBUG) {
            Log.d("packageScoreCache", "updateLocalData " + sql.toString());
        }
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(" SELECT SUM(click_count), SUM(show_count) FROM notification_sort " + " WHERE date < " + today, null);
            if (cursor != null && cursor.moveToFirst()) {
                this.mTotalClickCount = cursor.getInt(0);
                this.mTotalShowCount = cursor.getInt(1);
            }
        } catch (Exception e) {
            Log.d("packageScoreCache", "updateLocalData exception " + e);
        } catch (Throwable th) {
            closeCursor(null);
            throw th;
        }
        closeCursor(cursor);
        if (DEBUG) {
            Log.d("packageScoreCache", "updateLocalData click=" + this.mTotalClickCount + ", show=" + this.mTotalShowCount);
        }
    }

    public PackageEntity addShow(String packageName) {
        if (this.mPkgEntities.get(packageName) == null) {
            retrievePackage(packageName);
        }
        PackageEntity packageEntity = (PackageEntity) this.mPkgEntities.get(packageName);
        packageEntity.addShowCount();
        return packageEntity;
    }

    public PackageEntity addClick(String packageName) {
        if (this.mPkgEntities.get(packageName) == null) {
            retrievePackage(packageName);
        }
        PackageEntity packageEntity = (PackageEntity) this.mPkgEntities.get(packageName);
        packageEntity.addClickCount();
        return packageEntity;
    }

    private void retrievePackage(String packageName) {
        final PackageEntity pkgEntity = new PackageEntity(packageName);
        this.mPkgEntities.put(packageName, pkgEntity);
        this.mBgHandler.post(new Runnable() {
            public void run() {
                PackageScoreCache.this.updateEntityData(pkgEntity);
            }
        });
    }

    /* access modifiers changed from: private */
    public void updateEntityData(PackageEntity pkgEntity) {
        SQLiteDatabase db = openDB();
        if (db != null) {
            updateDailyData(db, pkgEntity);
            updateHistoryData(db, pkgEntity);
            closeDB(db);
            if (DEBUG) {
                Log.d("packageScoreCache", "updateEntityData " + pkgEntity.toString());
            }
        }
    }

    private void updateDailyData(SQLiteDatabase db, PackageEntity pkgEntity) {
        int today = DateUtils.getDigitalFormatDateToday();
        String pkgName = pkgEntity.getPackageName();
        if (DEBUG) {
            Log.d("packageScoreCache", "updateDailyData " + sql.toString());
        }
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(" SELECT click_count, show_count FROM notification_sort " + " WHERE package_name = '" + pkgName + "' " + " AND date = " + today, null);
            if (cursor != null && cursor.moveToFirst()) {
                pkgEntity.setDailyData(cursor.getInt(0), cursor.getInt(1));
            }
        } catch (Exception e) {
            Log.d("packageScoreCache", "updateDailyData exception " + e);
        } catch (Throwable th) {
            closeCursor(null);
            throw th;
        }
        closeCursor(cursor);
    }

    private void updateHistoryData(SQLiteDatabase db, PackageEntity pkgEntity) {
        int today = DateUtils.getDigitalFormatDateToday();
        String pkgName = pkgEntity.getPackageName();
        if (DEBUG) {
            Log.d("packageScoreCache", "updateHistoryData " + sql.toString());
        }
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(" SELECT SUM(click_count), SUM(show_count) FROM notification_sort " + " WHERE package_name = '" + pkgName + "' " + " AND date < " + today, null);
            if (cursor != null && cursor.moveToFirst()) {
                pkgEntity.setHistoryData(cursor.getInt(0), cursor.getInt(1));
            }
        } catch (Exception e) {
            Log.d("packageScoreCache", "updateHistoryData exception " + e);
        } catch (Throwable th) {
            closeCursor(null);
            throw th;
        }
        closeCursor(cursor);
    }

    private SQLiteDatabase openDB() {
        try {
            return this.mOpenHelper.getWritableDatabase();
        } catch (Exception e) {
            Log.d("packageScoreCache", "openDB failed " + e);
            return null;
        }
    }

    private void closeDB(SQLiteDatabase db) {
        if (db != null && db.isOpen()) {
            db.close();
        }
    }

    private void closeCursor(Cursor cursor) {
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
    }

    private boolean isDateChanged() {
        int days = (int) (System.currentTimeMillis() / DAYS_TO_MILLIS);
        if (days == this.mCurrentDays) {
            return false;
        }
        this.mCurrentDays = days;
        return true;
    }

    public int getTotalClickCount(String packageName) {
        PackageEntity entity = getPkgEntity(packageName);
        if (entity != null) {
            return entity.getTotalClick();
        }
        return 0;
    }

    public int getTotalShowCount(String packageName) {
        PackageEntity entity = getPkgEntity(packageName);
        if (entity != null) {
            return entity.getTotalShow();
        }
        return 0;
    }
}
