package com.android.keyguard.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import com.android.keyguard.Utils;

public class ContentProviderBinder {
    public ChangeObserver mChangeObserver = new ChangeObserver(null);
    protected String[] mColumns;
    private Context mContext;
    private QueryCompleteListener mQueryCompletedListener;
    private QueryHandler mQueryHandler;
    public Uri mUri;
    protected String mWhere;

    public static class Builder {
        private ContentProviderBinder mBinder;

        public Builder(ContentProviderBinder binder) {
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

    public void setUri(Uri uri) {
        this.mUri = uri;
    }

    public ContentProviderBinder(Context context) {
        this.mContext = context;
        this.mQueryHandler = new QueryHandler(context) {
            /* access modifiers changed from: protected */
            public void onQueryComplete(int token, Object cookie, Cursor cursor) {
                ContentProviderBinder.this.onQueryComplete(cursor);
            }
        };
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
        } else if (Utils.isBootCompleted()) {
            this.mQueryHandler.cancelOperation(100);
            String where = this.mWhere;
            Log.d("ContentProviderBinder", "start query: " + this.mUri + "\n where:" + where);
            this.mQueryHandler.startQuery(100, null, this.mUri, this.mColumns, where, null, null);
        }
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
