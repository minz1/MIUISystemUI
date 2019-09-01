package com.android.systemui.statusbar.notification;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import com.android.systemui.R;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.InflationTask;
import com.android.systemui.statusbar.NotificationData;
import com.miui.systemui.support.v4.view.AsyncLayoutInflater;

public class RowInflaterTask implements InflationTask, AsyncLayoutInflater.OnInflateFinishedListener {
    private boolean mCancelled;
    private NotificationData.Entry mEntry;
    private RowInflationFinishedListener mListener;

    public interface RowInflationFinishedListener {
        void onInflationFinished(ExpandableNotificationRow expandableNotificationRow);
    }

    public void inflate(Context context, ViewGroup parent, NotificationData.Entry entry, RowInflationFinishedListener listener) {
        this.mListener = listener;
        AsyncLayoutInflater inflater = new AsyncLayoutInflater(context);
        this.mEntry = entry;
        entry.setInflationTask(this);
        inflater.inflate(R.layout.status_bar_notification_row, parent, this);
    }

    public void abort() {
        this.mCancelled = true;
    }

    public void onInflateFinished(View view, int resid, ViewGroup parent) {
        if (!this.mCancelled) {
            this.mEntry.onInflationTaskFinished();
            this.mListener.onInflationFinished((ExpandableNotificationRow) view);
        }
    }

    public void supersedeTask(InflationTask task) {
    }
}
