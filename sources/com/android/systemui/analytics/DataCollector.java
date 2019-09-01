package com.android.systemui.analytics;

import android.content.Context;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.MotionEvent;
import android.widget.Toast;
import com.android.systemui.statusbar.phone.nano.TouchAnalyticsProto;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class DataCollector implements SensorEventListener {
    private static DataCollector sInstance = null;
    private boolean mAllowReportRejectedTouch = false;
    /* access modifiers changed from: private */
    public boolean mCollectBadTouches = false;
    /* access modifiers changed from: private */
    public final Context mContext;
    private boolean mCornerSwiping = false;
    private SensorLoggerSession mCurrentSession = null;
    private boolean mEnableCollector = false;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    protected final ContentObserver mSettingsObserver = new ContentObserver(this.mHandler) {
        public void onChange(boolean selfChange) {
            DataCollector.this.updateConfiguration();
        }
    };
    private boolean mTimeoutActive = false;
    private boolean mTrackingStarted = false;

    private DataCollector(Context context) {
        this.mContext = context;
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("data_collector_enable"), false, this.mSettingsObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("data_collector_collect_bad_touches"), false, this.mSettingsObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("data_collector_allow_rejected_touch_reports"), false, this.mSettingsObserver, -1);
        updateConfiguration();
    }

    public static DataCollector getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new DataCollector(context);
        }
        return sInstance;
    }

    /* access modifiers changed from: private */
    public void updateConfiguration() {
        boolean z = true;
        this.mEnableCollector = Build.IS_DEBUGGABLE && Settings.Secure.getInt(this.mContext.getContentResolver(), "data_collector_enable", 0) != 0;
        this.mCollectBadTouches = this.mEnableCollector && Settings.Secure.getInt(this.mContext.getContentResolver(), "data_collector_collect_bad_touches", 0) != 0;
        if (!Build.IS_DEBUGGABLE || Settings.Secure.getInt(this.mContext.getContentResolver(), "data_collector_allow_rejected_touch_reports", 0) == 0) {
            z = false;
        }
        this.mAllowReportRejectedTouch = z;
    }

    private boolean sessionEntrypoint() {
        if (!isEnabled() || this.mCurrentSession != null) {
            return false;
        }
        onSessionStart();
        return true;
    }

    private void sessionExitpoint(int result) {
        if (this.mCurrentSession != null) {
            onSessionEnd(result);
        }
    }

    private void onSessionStart() {
        this.mCornerSwiping = false;
        this.mTrackingStarted = false;
        this.mCurrentSession = new SensorLoggerSession(System.currentTimeMillis(), System.nanoTime());
    }

    private void onSessionEnd(int result) {
        SensorLoggerSession session = this.mCurrentSession;
        this.mCurrentSession = null;
        if (this.mEnableCollector) {
            session.end(System.currentTimeMillis(), result);
            queueSession(session);
        }
    }

    public Uri reportRejectedTouch() {
        if (this.mCurrentSession == null) {
            Toast.makeText(this.mContext, "Generating rejected touch report failed: session timed out.", 1).show();
            return null;
        }
        SensorLoggerSession currentSession = this.mCurrentSession;
        currentSession.setType(4);
        currentSession.end(System.currentTimeMillis(), 1);
        byte[] b = TouchAnalyticsProto.Session.toByteArray(currentSession.toProto());
        File dir = new File(this.mContext.getExternalCacheDir(), "rejected_touch_reports");
        dir.mkdir();
        File touch = new File(dir, "rejected_touch_report_" + System.currentTimeMillis());
        try {
            new FileOutputStream(touch).write(b);
            return Uri.fromFile(touch);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void queueSession(final SensorLoggerSession currentSession) {
        AsyncTask.execute(new Runnable() {
            public void run() {
                String dir;
                byte[] b = TouchAnalyticsProto.Session.toByteArray(currentSession.toProto());
                String dir2 = DataCollector.this.mContext.getFilesDir().getAbsolutePath();
                if (currentSession.getResult() == 1) {
                    dir = dir2 + "/good_touches";
                } else if (DataCollector.this.mCollectBadTouches) {
                    dir = dir2 + "/bad_touches";
                } else {
                    return;
                }
                File file = new File(dir);
                file.mkdir();
                try {
                    new FileOutputStream(new File(file, "trace_" + System.currentTimeMillis())).write(b);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public synchronized void onSensorChanged(SensorEvent event) {
        if (isEnabled() && this.mCurrentSession != null) {
            this.mCurrentSession.addSensorEvent(event, System.nanoTime());
            enforceTimeout();
        }
    }

    private void enforceTimeout() {
        if (this.mTimeoutActive && System.currentTimeMillis() - this.mCurrentSession.getStartTimestampMillis() > 11000) {
            onSessionEnd(2);
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public boolean isEnabled() {
        return this.mEnableCollector || this.mAllowReportRejectedTouch;
    }

    public boolean isEnabledFull() {
        return this.mEnableCollector;
    }

    public void onScreenTurningOn() {
        if (sessionEntrypoint()) {
            addEvent(0);
        }
    }

    public void onScreenOnFromTouch() {
        if (sessionEntrypoint()) {
            addEvent(1);
        }
    }

    public void onScreenOff() {
        addEvent(2);
        sessionExitpoint(0);
    }

    public void onSucccessfulUnlock() {
        addEvent(3);
        sessionExitpoint(1);
    }

    public void onBouncerShown() {
        addEvent(4);
    }

    public void onBouncerHidden() {
        addEvent(5);
    }

    public void onQsDown() {
        addEvent(6);
    }

    public void setQsExpanded(boolean expanded) {
        if (expanded) {
            addEvent(7);
        } else {
            addEvent(8);
        }
    }

    public void onTrackingStarted() {
        this.mTrackingStarted = true;
        addEvent(9);
    }

    public void onTrackingStopped() {
        if (this.mTrackingStarted) {
            this.mTrackingStarted = false;
            addEvent(10);
        }
    }

    public void onNotificationActive() {
        addEvent(11);
    }

    public void onNotificationDoubleTap() {
        addEvent(13);
    }

    public void setNotificationExpanded() {
        addEvent(14);
    }

    public void onNotificatonStartDraggingDown() {
        addEvent(16);
    }

    public void onNotificatonStopDraggingDown() {
        addEvent(17);
    }

    public void onNotificationDismissed() {
        addEvent(18);
    }

    public void onNotificatonStartDismissing() {
        addEvent(19);
    }

    public void onNotificatonStopDismissing() {
        addEvent(20);
    }

    public void onCameraOn() {
        addEvent(24);
    }

    public void onLeftAffordanceOn() {
        addEvent(25);
    }

    public void onAffordanceSwipingAborted() {
        if (this.mCornerSwiping) {
            this.mCornerSwiping = false;
            addEvent(23);
        }
    }

    public void onTouchEvent(MotionEvent event, int width, int height) {
        if (this.mCurrentSession != null) {
            this.mCurrentSession.addMotionEvent(event);
            this.mCurrentSession.setTouchArea(width, height);
            enforceTimeout();
        }
    }

    private void addEvent(int eventType) {
        if (isEnabled() && this.mCurrentSession != null) {
            this.mCurrentSession.addPhoneEvent(eventType, System.nanoTime());
        }
    }

    public boolean isReportingEnabled() {
        return this.mAllowReportRejectedTouch;
    }
}
