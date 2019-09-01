package com.android.keyguard.util;

import android.content.AsyncQueryHandler;
import android.content.Context;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.database.sqlite.SQLiteDiskIOException;
import android.database.sqlite.SQLiteFullException;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public abstract class QueryHandler extends AsyncQueryHandler {

    protected class CatchingWorkerHandler extends AsyncQueryHandler.WorkerHandler {
        public CatchingWorkerHandler(Looper looper) {
            super(QueryHandler.this, looper);
        }

        public void handleMessage(Message msg) {
            try {
                QueryHandler.super.handleMessage(msg);
            } catch (SQLiteDiskIOException e) {
                Log.w("QueryHandler", "Exception on background worker thread", e);
            } catch (SQLiteFullException e2) {
                Log.w("QueryHandler", "Exception on background worker thread", e2);
            } catch (SQLiteDatabaseCorruptException e3) {
                Log.w("QueryHandler", "Exception on background worker thread", e3);
            }
        }
    }

    /* JADX WARNING: type inference failed for: r0v0, types: [com.android.keyguard.util.QueryHandler$CatchingWorkerHandler, android.os.Handler] */
    /* access modifiers changed from: protected */
    public Handler createHandler(Looper looper) {
        return new CatchingWorkerHandler(looper);
    }

    public QueryHandler(Context context) {
        super(context.getContentResolver());
    }
}
