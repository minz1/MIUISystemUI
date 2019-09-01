package com.android.systemui.classifier;

import android.content.Context;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityManager;
import com.android.systemui.Dependency;
import com.android.systemui.UiOffloadThread;
import com.android.systemui.analytics.DataCollector;
import com.android.systemui.statusbar.StatusBarState;
import java.io.PrintWriter;

public class FalsingManager implements SensorEventListener {
    private static final int[] CLASSIFIER_SENSORS = {8};
    private static final int[] COLLECTOR_SENSORS = {1, 4, 8, 5, 11};
    private static FalsingManager sInstance = null;
    private final AccessibilityManager mAccessibilityManager;
    private boolean mBouncerOn = false;
    private final Context mContext;
    private final DataCollector mDataCollector;
    private boolean mEnforceBouncer = false;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final HumanInteractionClassifier mHumanInteractionClassifier;
    private Runnable mPendingWtf;
    /* access modifiers changed from: private */
    public boolean mScreenOn;
    /* access modifiers changed from: private */
    public final SensorManager mSensorManager;
    private boolean mSessionActive = false;
    protected final ContentObserver mSettingsObserver = new ContentObserver(this.mHandler) {
        public void onChange(boolean selfChange) {
            FalsingManager.this.updateConfiguration();
        }
    };
    /* access modifiers changed from: private */
    public int mState = 0;
    private final UiOffloadThread mUiOffloadThread;

    private FalsingManager(Context context) {
        this.mContext = context;
        this.mSensorManager = (SensorManager) this.mContext.getSystemService(SensorManager.class);
        this.mAccessibilityManager = (AccessibilityManager) context.getSystemService(AccessibilityManager.class);
        this.mDataCollector = DataCollector.getInstance(this.mContext);
        this.mHumanInteractionClassifier = HumanInteractionClassifier.getInstance(this.mContext);
        this.mUiOffloadThread = (UiOffloadThread) Dependency.get(UiOffloadThread.class);
        this.mScreenOn = ((PowerManager) context.getSystemService(PowerManager.class)).isInteractive();
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("falsing_manager_enforce_bouncer"), false, this.mSettingsObserver, -1);
        updateConfiguration();
    }

    public static FalsingManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new FalsingManager(context);
        }
        return sInstance;
    }

    /* access modifiers changed from: private */
    public void updateConfiguration() {
        boolean z = false;
        if (Settings.Secure.getInt(this.mContext.getContentResolver(), "falsing_manager_enforce_bouncer", 0) != 0) {
            z = true;
        }
        this.mEnforceBouncer = z;
    }

    private boolean shouldSessionBeActive() {
        boolean z = FalsingLog.ENABLED;
        return isEnabled() && this.mScreenOn && this.mState == 1;
    }

    private boolean sessionEntrypoint() {
        if (this.mSessionActive || !shouldSessionBeActive()) {
            return false;
        }
        onSessionStart();
        return true;
    }

    private void sessionExitpoint(boolean force) {
        if (!this.mSessionActive) {
            return;
        }
        if (force || !shouldSessionBeActive()) {
            this.mSessionActive = false;
            this.mUiOffloadThread.submit(new Runnable() {
                public void run() {
                    FalsingManager.this.mSensorManager.unregisterListener(FalsingManager.this);
                }
            });
        }
    }

    private void onSessionStart() {
        if (FalsingLog.ENABLED) {
            FalsingLog.i("onSessionStart", "classifierEnabled=" + isClassiferEnabled());
            clearPendingWtf();
        }
        this.mBouncerOn = false;
        this.mSessionActive = true;
        if (this.mHumanInteractionClassifier.isEnabled()) {
            registerSensors(CLASSIFIER_SENSORS);
        }
        if (this.mDataCollector.isEnabledFull()) {
            registerSensors(COLLECTOR_SENSORS);
        }
    }

    private void registerSensors(int[] sensors) {
        for (int sensorType : sensors) {
            final Sensor s = this.mSensorManager.getDefaultSensor(sensorType);
            if (s != null) {
                this.mUiOffloadThread.submit(new Runnable() {
                    public void run() {
                        FalsingManager.this.mSensorManager.registerListener(FalsingManager.this, s, 1);
                    }
                });
            }
        }
    }

    public boolean isClassiferEnabled() {
        return this.mHumanInteractionClassifier.isEnabled();
    }

    /* access modifiers changed from: private */
    public boolean isEnabled() {
        return this.mHumanInteractionClassifier.isEnabled() || this.mDataCollector.isEnabled();
    }

    public boolean isFalseTouch() {
        if (FalsingLog.ENABLED && !this.mSessionActive && ((PowerManager) this.mContext.getSystemService(PowerManager.class)).isInteractive() && this.mPendingWtf == null) {
            int enabled = isEnabled();
            int screenOn = this.mScreenOn;
            String state = StatusBarState.toShortString(this.mState);
            final Throwable here = new Throwable("here");
            FalsingLog.wLogcat("isFalseTouch", "Session is not active, yet there's a query for a false touch." + " enabled=" + ((int) enabled) + " mScreenOn=" + ((int) screenOn) + " mState=" + state + ". Escalating to WTF if screen does not turn on soon.");
            final String str = state;
            final int i = (int) enabled;
            final int i2 = (int) screenOn;
            AnonymousClass4 r1 = new Runnable() {
                public void run() {
                    FalsingLog.wtf("isFalseTouch", "Session did not become active after query for a false touch." + " enabled=" + i + '/' + (FalsingManager.this.isEnabled() ? 1 : 0) + " mScreenOn=" + i2 + '/' + (FalsingManager.this.mScreenOn ? 1 : 0) + " mState=" + str + '/' + StatusBarState.toShortString(FalsingManager.this.mState) + ". Look for warnings ~1000ms earlier to see root cause.", here);
                }
            };
            this.mPendingWtf = r1;
            this.mHandler.postDelayed(this.mPendingWtf, 1000);
        }
        if (this.mAccessibilityManager.isTouchExplorationEnabled()) {
            return false;
        }
        return this.mHumanInteractionClassifier.isFalseTouch();
    }

    private void clearPendingWtf() {
        if (this.mPendingWtf != null) {
            this.mHandler.removeCallbacks(this.mPendingWtf);
            this.mPendingWtf = null;
        }
    }

    public synchronized void onSensorChanged(SensorEvent event) {
        this.mDataCollector.onSensorChanged(event);
        this.mHumanInteractionClassifier.onSensorChanged(event);
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        this.mDataCollector.onAccuracyChanged(sensor, accuracy);
    }

    public boolean shouldEnforceBouncer() {
        return this.mEnforceBouncer;
    }

    public void setStatusBarState(int state) {
        if (FalsingLog.ENABLED) {
            FalsingLog.i("setStatusBarState", "from=" + StatusBarState.toShortString(this.mState) + " to=" + StatusBarState.toShortString(state));
        }
        this.mState = state;
        if (shouldSessionBeActive()) {
            sessionEntrypoint();
        } else {
            sessionExitpoint(false);
        }
    }

    public void onScreenTurningOn() {
        if (FalsingLog.ENABLED) {
            FalsingLog.i("onScreenTurningOn", "from=" + (this.mScreenOn ? 1 : 0));
            clearPendingWtf();
        }
        this.mScreenOn = true;
        if (sessionEntrypoint()) {
            this.mDataCollector.onScreenTurningOn();
        }
    }

    public void onScreenOnFromTouch() {
        if (FalsingLog.ENABLED) {
            FalsingLog.i("onScreenOnFromTouch", "from=" + (this.mScreenOn ? 1 : 0));
        }
        this.mScreenOn = true;
        if (sessionEntrypoint()) {
            this.mDataCollector.onScreenOnFromTouch();
        }
    }

    public void onScreenOff() {
        if (FalsingLog.ENABLED) {
            FalsingLog.i("onScreenOff", "from=" + (this.mScreenOn ? 1 : 0));
        }
        this.mDataCollector.onScreenOff();
        this.mScreenOn = false;
        sessionExitpoint(false);
    }

    public void onSucccessfulUnlock() {
        if (FalsingLog.ENABLED) {
            FalsingLog.i("onSucccessfulUnlock", "");
        }
        this.mDataCollector.onSucccessfulUnlock();
    }

    public void onBouncerShown() {
        if (FalsingLog.ENABLED) {
            FalsingLog.i("onBouncerShown", "from=" + (this.mBouncerOn ? 1 : 0));
        }
        if (!this.mBouncerOn) {
            this.mBouncerOn = true;
            this.mDataCollector.onBouncerShown();
        }
    }

    public void onBouncerHidden() {
        if (FalsingLog.ENABLED) {
            FalsingLog.i("onBouncerHidden", "from=" + (this.mBouncerOn ? 1 : 0));
        }
        if (this.mBouncerOn) {
            this.mBouncerOn = false;
            this.mDataCollector.onBouncerHidden();
        }
    }

    public void onQsDown() {
        if (FalsingLog.ENABLED) {
            FalsingLog.i("onQsDown", "");
        }
        this.mHumanInteractionClassifier.setType(0);
        this.mDataCollector.onQsDown();
    }

    public void setQsExpanded(boolean expanded) {
        this.mDataCollector.setQsExpanded(expanded);
    }

    public void onTrackingStarted() {
        if (FalsingLog.ENABLED) {
            FalsingLog.i("onTrackingStarted", "");
        }
        this.mHumanInteractionClassifier.setType(4);
        this.mDataCollector.onTrackingStarted();
    }

    public void onTrackingStopped() {
        this.mDataCollector.onTrackingStopped();
    }

    public void onNotificationActive() {
        this.mDataCollector.onNotificationActive();
    }

    public void onNotificationDoubleTap(boolean accepted, float dx, float dy) {
        if (FalsingLog.ENABLED) {
            FalsingLog.i("onNotificationDoubleTap", "accepted=" + accepted + " dx=" + dx + " dy=" + dy + " (px)");
        }
        this.mDataCollector.onNotificationDoubleTap();
    }

    public void setNotificationExpanded() {
        this.mDataCollector.setNotificationExpanded();
    }

    public void onNotificatonStartDraggingDown() {
        if (FalsingLog.ENABLED) {
            FalsingLog.i("onNotificatonStartDraggingDown", "");
        }
        this.mHumanInteractionClassifier.setType(2);
        this.mDataCollector.onNotificatonStartDraggingDown();
    }

    public void onNotificatonStopDraggingDown() {
        this.mDataCollector.onNotificatonStopDraggingDown();
    }

    public void onNotificationDismissed() {
        this.mDataCollector.onNotificationDismissed();
    }

    public void onNotificatonStartDismissing() {
        if (FalsingLog.ENABLED) {
            FalsingLog.i("onNotificatonStartDismissing", "");
        }
        this.mHumanInteractionClassifier.setType(1);
        this.mDataCollector.onNotificatonStartDismissing();
    }

    public void onNotificatonStopDismissing() {
        this.mDataCollector.onNotificatonStopDismissing();
    }

    public void onCameraOn() {
        this.mDataCollector.onCameraOn();
    }

    public void onLeftAffordanceOn() {
        this.mDataCollector.onLeftAffordanceOn();
    }

    public void onAffordanceSwipingAborted() {
        this.mDataCollector.onAffordanceSwipingAborted();
    }

    public void onTouchEvent(MotionEvent event, int width, int height) {
        if (this.mSessionActive && !this.mBouncerOn) {
            this.mDataCollector.onTouchEvent(event, width, height);
            this.mHumanInteractionClassifier.onTouchEvent(event);
        }
    }

    public void dump(PrintWriter pw) {
        pw.println("FALSING MANAGER");
        pw.print("classifierEnabled=");
        pw.println(isClassiferEnabled() ? 1 : 0);
        pw.print("mSessionActive=");
        pw.println(this.mSessionActive ? 1 : 0);
        pw.print("mBouncerOn=");
        pw.println(this.mSessionActive ? 1 : 0);
        pw.print("mState=");
        pw.println(StatusBarState.toShortString(this.mState));
        pw.print("mScreenOn=");
        pw.println(this.mScreenOn ? 1 : 0);
        pw.println();
    }

    public Uri reportRejectedTouch() {
        if (this.mDataCollector.isEnabled()) {
            return this.mDataCollector.reportRejectedTouch();
        }
        return null;
    }

    public boolean isReportingEnabled() {
        return this.mDataCollector.isReportingEnabled();
    }
}
