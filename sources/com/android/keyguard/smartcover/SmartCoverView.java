package com.android.keyguard.smartcover;

import android.content.Context;
import android.net.Uri;
import android.provider.CallLog;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import com.android.keyguard.smartcover.ContentProviderBinder;
import java.util.ArrayList;
import java.util.Iterator;

public abstract class SmartCoverView extends RelativeLayout implements ContentProviderBinder.QueryCompleteListener {
    private Uri SMS_INBOX_URI = Uri.parse("content://sms");
    private ArrayList<ContentProviderBinder> mBinders = new ArrayList<>();
    protected boolean mCharging;
    protected boolean mFull;
    protected int mLevel;
    protected int mLowBatteryWarningLevel;
    protected int mMissCallNum;
    protected boolean mShowMissCall;
    protected boolean mShowSms;
    protected int mSmsNum;

    /* access modifiers changed from: protected */
    public abstract void refresh();

    public SmartCoverView(Context context) {
        super(context);
    }

    public SmartCoverView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mLowBatteryWarningLevel = this.mContext.getResources().getInteger(285736963);
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        fillMissedCall();
        fillUnreadSms();
        Iterator<ContentProviderBinder> it = this.mBinders.iterator();
        while (it.hasNext()) {
            it.next().init();
        }
    }

    private void fillMissedCall() {
        addContentProviderBinder(CallLog.Calls.CONTENT_URI).setColumns(new String[]{"number"}).setWhere("type=3 AND new=1").setCountName("call_missed_count");
    }

    private void fillUnreadSms() {
        addContentProviderBinder(this.SMS_INBOX_URI).setColumns(null).setWhere("seen=0 AND read=0").setCountName("sms_unread_count");
    }

    public ContentProviderBinder.Builder addContentProviderBinder(Uri uri) {
        ContentProviderBinder binder = new ContentProviderBinder(this.mContext);
        binder.setQueryCompleteListener(this);
        binder.setUri(uri);
        this.mBinders.add(binder);
        return new ContentProviderBinder.Builder(binder);
    }

    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Iterator<ContentProviderBinder> it = this.mBinders.iterator();
        while (it.hasNext()) {
            it.next().finish();
        }
    }

    public void onQueryCompleted(Uri uri, int num) {
        boolean z = false;
        if (CallLog.Calls.CONTENT_URI.equals(uri)) {
            if (num > 0) {
                z = true;
            }
            this.mShowMissCall = z;
            this.mMissCallNum = num;
        } else if (this.SMS_INBOX_URI.equals(uri)) {
            if (num > 0) {
                z = true;
            }
            this.mShowSms = z;
            this.mSmsNum = num;
        }
        refresh();
    }

    public void onBatteryInfoRefresh(boolean charging, boolean full, int level) {
        this.mCharging = charging;
        this.mFull = full;
        this.mLevel = level;
        refresh();
    }
}
