package com.android.keyguard.smartcover;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.database.sqlite.SQLiteDiskIOException;
import android.database.sqlite.SQLiteFullException;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import miui.os.SystemProperties;

public class ContentProviderBinder {
    public ChangeObserver mChangeObserver = new ChangeObserver(null);
    protected String[] mColumns;
    private Context mContext;
    protected String mCountName;
    private QueryCompleteListener mQueryCompletedListener;
    private QueryHandler mQueryHandler;
    private boolean mSystemBootCompleted;
    public Uri mUri;
    protected String mWhere;

    public static class Builder {
        private ContentProviderBinder mBinder;

        protected Builder(ContentProviderBinder binder) {
            this.mBinder = binder;
        }

        public Builder setWhere(String where) {
            this.mBinder.mWhere = where;
            return this;
        }

        public Builder setColumns(String[] columns) {
            this.mBinder.mColumns = columns;
            return this;
        }

        public Builder setCountName(String countName) {
            this.mBinder.mCountName = countName;
            return this;
        }
    }

    private class ChangeObserver extends ContentObserver {
        public ChangeObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean selfChange) {
            ContentProviderBinder.this.startQuery();
        }
    }

    public interface QueryCompleteListener {
        void onQueryCompleted(Uri uri, int i);
    }

    private final class QueryHandler extends AsyncQueryHandler {

        protected class CatchingWorkerHandler extends AsyncQueryHandler.WorkerHandler {
            public CatchingWorkerHandler(Looper looper) {
                super(QueryHandler.this, looper);
            }

            public void handleMessage(Message msg) {
                try {
                    QueryHandler.super.handleMessage(msg);
                } catch (SQLiteDiskIOException e) {
                    Log.w("ContentProviderBinder", "Exception on background worker thread", e);
                } catch (SQLiteFullException e2) {
                    Log.w("ContentProviderBinder", "Exception on background worker thread", e2);
                } catch (SQLiteDatabaseCorruptException e3) {
                    Log.w("ContentProviderBinder", "Exception on background worker thread", e3);
                }
            }
        }

        /* JADX WARNING: type inference failed for: r0v0, types: [com.android.keyguard.smartcover.ContentProviderBinder$QueryHandler$CatchingWorkerHandler, android.os.Handler] */
        /* access modifiers changed from: protected */
        public Handler createHandler(Looper looper) {
            return new CatchingWorkerHandler(looper);
        }

        public QueryHandler(Context context) {
            super(context.getContentResolver());
        }

        /* access modifiers changed from: protected */
        public void onQueryComplete(int token, Object cookie, Cursor cursor) {
            ContentProviderBinder.this.onQueryComplete(cursor);
        }
    }

    public void setUri(Uri uri) {
        this.mUri = uri;
    }

    public ContentProviderBinder(Context context) {
        this.mContext = context;
        this.mQueryHandler = new QueryHandler(context);
    }

    public void init() {
        registerObserver(true);
        startQuery();
    }

    public void finish() {
        registerObserver(false);
    }

    private void registerObserver(boolean reg) {
        ContentResolver cr = this.mContext.getContentResolver();
        cr.unregisterContentObserver(this.mChangeObserver);
        if (reg && this.mUri != null) {
            try {
                cr.registerContentObserver(this.mUri, true, this.mChangeObserver);
            } catch (IllegalArgumentException e) {
                Log.e("ContentProviderBinder", e.toString() + "  uri:" + this.mUri);
            }
        }
    }

    public void startQuery() {
        if (this.mUri == null) {
            Log.d("ContentProviderBinder", "startQuery  uri == null");
            return;
        }
        if (!this.mSystemBootCompleted) {
            this.mSystemBootCompleted = "1".equals(SystemProperties.get("sys.boot_completed"));
            if (!this.mSystemBootCompleted) {
                return;
            }
        }
        this.mQueryHandler.cancelOperation(100);
        String where = this.mWhere;
        Log.d("ContentProviderBinder", "start query: " + this.mUri + "\n where:" + where);
        this.mQueryHandler.startQuery(100, null, this.mUri, this.mColumns, where, null, null);
    }

    /* access modifiers changed from: private */
    public void onQueryComplete(Cursor cursor) {
        int num = 0;
        if (cursor != null) {
            try {
                num = cursor.getCount();
                Log.d("ContentProviderBinder", "num=" + num + "; muri=" + this.mUri);
            } catch (Exception e) {
                e.printStackTrace();
                if (cursor != null) {
                    try {
                        cursor.close();
                    } catch (Exception e2) {
                        e2.printStackTrace();
                    }
                }
            } catch (Throwable th) {
                if (cursor != null) {
                    try {
                        cursor.close();
                    } catch (Exception e3) {
                        e3.printStackTrace();
                    }
                }
                throw th;
            }
        }
        if (cursor != null) {
            cursor.close();
        }
        if (this.mQueryCompletedListener != null && this.mUri != null) {
            this.mQueryCompletedListener.onQueryCompleted(this.mUri, num);
        }
    }

    public void setQueryCompleteListener(QueryCompleteListener queryCompleteListener) {
        this.mQueryCompletedListener = queryCompleteListener;
    }
}
