package com.android.settingslib.utils;

import android.content.AsyncTaskLoader;
import android.content.Context;

public abstract class AsyncLoader<T> extends AsyncTaskLoader<T> {
    private T mResult;

    /* access modifiers changed from: protected */
    public abstract void onDiscardResult(T t);

    public AsyncLoader(Context context) {
        super(context);
    }

    /* access modifiers changed from: protected */
    public void onStartLoading() {
        if (this.mResult != null) {
            deliverResult(this.mResult);
        }
        if (takeContentChanged() || this.mResult == null) {
            forceLoad();
        }
    }

    /* access modifiers changed from: protected */
    public void onStopLoading() {
        cancelLoad();
    }

    public void deliverResult(T data) {
        if (isReset()) {
            if (data != null) {
                onDiscardResult(data);
            }
            return;
        }
        T oldResult = this.mResult;
        this.mResult = data;
        if (isStarted()) {
            super.deliverResult(data);
        }
        if (!(oldResult == null || oldResult == this.mResult)) {
            onDiscardResult(oldResult);
        }
    }

    /* access modifiers changed from: protected */
    public void onReset() {
        super.onReset();
        onStopLoading();
        if (this.mResult != null) {
            onDiscardResult(this.mResult);
        }
        this.mResult = null;
    }

    public void onCanceled(T data) {
        super.onCanceled(data);
        if (data != null) {
            onDiscardResult(data);
        }
    }
}
