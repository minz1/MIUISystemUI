package com.android.keyguard.fod;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.Vibrator;
import android.util.Log;
import android.util.Slog;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.widget.FrameLayout;
import com.android.keyguard.KeyguardCompatibilityHelperForP;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.keyguard.fod.MiuiGxzwQuickOpenView;
import com.android.systemui.Application;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.statusbar.phone.StatusBar;

public class MiuiGxzwIconView extends FrameLayout implements DisplayManager.DisplayListener, View.OnTouchListener, MiuiGxzwQuickOpenView.DismissListener {
    private static long AOD_DOZE_SUSPEND_DELAY = 50;
    private static int TYPE_PUT_UP_DETECT = 33171030;
    private final int FINGERPRINT_ERROR_LOCKOUT = 7;
    private final int FINGERPRINT_ERROR_LOCKOUT_PERMANENT_FOR_O = 9;
    private AlarmManager mAlarmManager;
    /* access modifiers changed from: private */
    public boolean mAuthFailedSignal = false;
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if ("action_set_icon_transparent".equals(intent.getAction()) && MiuiGxzwIconView.this.mDozing) {
                boolean unused = MiuiGxzwIconView.this.mDozeShowIconTimeout = true;
                if (MiuiGxzwIconView.this.mTouchDown) {
                    MiuiGxzwIconView.this.scheduleSetIconTransparen();
                } else if (!MiuiGxzwIconView.this.mDeviceMoving) {
                    MiuiGxzwIconView.this.dismissFingerpirntIcon();
                }
            }
        }
    };
    private CollectGxzwListener mCollectGxzwListener;
    /* access modifiers changed from: private */
    public boolean mDeviceMoving = false;
    private DisplayManager mDisplayManager;
    private int mDisplayState = 2;
    /* access modifiers changed from: private */
    public boolean mDozeShowIconTimeout = false;
    /* access modifiers changed from: private */
    public boolean mDozing = false;
    private boolean mEnrolling;
    /* access modifiers changed from: private */
    public boolean mFingerprintLockout = false;
    private Runnable mGotoUnlockRunnable = new Runnable() {
        public void run() {
            if (MiuiGxzwIconView.this.mDozing) {
                MiuiGxzwIconView.this.mPowerManager.wakeUp(SystemClock.uptimeMillis(), "com.android.systemui:GXZW_GOTO_UNLOCK");
            }
            MiuiGxzwIconView.this.showBouncer();
        }
    };
    private int mGxzwIconHeight;
    private boolean mGxzwIconTransparent = false;
    private int mGxzwIconWidth;
    private int mGxzwIconX;
    private int mGxzwIconY;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 1001 && MiuiGxzwIconView.this.mKeyguardAuthen && MiuiGxzwIconView.this.mShowed) {
                MiuiGxzwIconView.this.mMiuiGxzwAnimView.showMorePress();
            }
        }
    };
    /* access modifiers changed from: private */
    public boolean mHasPressureSensor;
    private View mHighlightView;
    private IntentFilter mIntentFilter;
    /* access modifiers changed from: private */
    public boolean mKeyguardAuthen;
    /* access modifiers changed from: private */
    public KeyguardUpdateMonitor mKeyguardUpdateMonitor;
    private KeyguardUpdateMonitorCallback mKeyguardUpdateMonitorCallback = new KeyguardUpdateMonitorCallback() {
        public void onFingerprintAcquired() {
            super.onFingerprintAcquired();
            Log.i("MiuiGxzwViewIcon", "onFingerprintAcquired");
        }

        public void onFingerprintAuthFailed() {
            super.onFingerprintAuthFailed();
            Log.i("MiuiGxzwViewIcon", "onFingerprintAuthFailed");
            if (MiuiGxzwIconView.this.mKeyguardAuthen && MiuiGxzwIconView.this.mTouchDown) {
                MiuiGxzwIconView.this.mMiuiGxzwAnimView.startFalseAnim();
            }
            boolean unused = MiuiGxzwIconView.this.mAuthFailedSignal = true;
            if (!MiuiGxzwIconView.this.mHasPressureSensor) {
                MiuiGxzwIconView.this.mVibrator.vibrate(12);
            }
        }

        public void onFingerprintAuthenticated(int userId) {
            super.onFingerprintAuthenticated(userId);
            Log.i("MiuiGxzwViewIcon", "onFingerprintAuthenticated: userId = " + userId);
            boolean unused = MiuiGxzwIconView.this.mAuthFailedSignal = false;
            if (!MiuiGxzwIconView.this.mKeyguardUpdateMonitor.getStrongAuthTracker().hasUserAuthenticatedSinceBoot(userId)) {
                if (MiuiGxzwIconView.this.mDozing) {
                    MiuiGxzwIconView.this.mPowerManager.wakeUp(SystemClock.uptimeMillis(), "com.android.systemui:GXZW_AUTHENTICATED");
                }
                MiuiGxzwIconView.this.showBouncer();
            }
        }

        public void onFingerprintHelp(int msgId, String helpString) {
            super.onFingerprintHelp(msgId, helpString);
            Log.i("MiuiGxzwViewIcon", "onFingerprintHelp: msgId = " + msgId + ", helpString = " + helpString);
        }

        public void onFingerprintError(int msgId, String errString) {
            super.onFingerprintError(msgId, errString);
            Log.i("MiuiGxzwViewIcon", "onFingerprintError: msgId = " + msgId + ", errString = " + errString);
            if (msgId == 7 || msgId == 9) {
                if (MiuiGxzwIconView.this.mDozing && MiuiGxzwIconView.this.mAuthFailedSignal) {
                    MiuiGxzwIconView.this.mPowerManager.wakeUp(SystemClock.uptimeMillis(), "com.android.systemui:GXZW_FP_ERROR");
                    MiuiGxzwIconView.this.showBouncer();
                }
                boolean unused = MiuiGxzwIconView.this.mFingerprintLockout = true;
                boolean unused2 = MiuiGxzwIconView.this.mAuthFailedSignal = false;
            }
        }

        public void onFingerprintRunningStateChanged(boolean running) {
            super.onFingerprintRunningStateChanged(running);
            Log.i("MiuiGxzwViewIcon", "onFingerprintRunningStateChanged: running = " + running);
            if (running) {
                boolean unused = MiuiGxzwIconView.this.mFingerprintLockout = false;
            }
        }
    };
    /* access modifiers changed from: private */
    public MiuiGxzwAnimView mMiuiGxzwAnimView;
    private MiuiGxzwQuickOpenView mMiuiGxzwQuickOpenView;
    private boolean mNeedVibrator = true;
    private PendingIntent mPendingIntent;
    private boolean mPendingShow;
    private boolean mPendingShowLightIcon;
    private boolean mPortraitOrientation = true;
    /* access modifiers changed from: private */
    public PowerManager mPowerManager;
    private int mPressureValue = 0;
    private SensorEventListener mPutUpSensorListener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent event) {
            Log.d("MiuiGxzwViewIcon", "onSensorChanged");
            if (event == null) {
                return;
            }
            if (event.values[0] == 1.0f) {
                Log.d("MiuiGxzwViewIcon", "detect device move");
                MiuiGxzwIconView.this.showFingerprintIcon();
                if (MiuiGxzwIconView.this.isSupportNewFodSensor()) {
                    boolean unused = MiuiGxzwIconView.this.mDeviceMoving = true;
                }
            } else if (event.values[0] == 2.0f) {
                Log.d("MiuiGxzwViewIcon", "detect device stable");
                if (MiuiGxzwIconView.this.isSupportNewFodSensor()) {
                    boolean unused2 = MiuiGxzwIconView.this.mDeviceMoving = false;
                    if (MiuiGxzwIconView.this.mDozeShowIconTimeout && MiuiGxzwIconView.this.mDozing) {
                        if (MiuiGxzwIconView.this.mTouchDown) {
                            MiuiGxzwIconView.this.scheduleSetIconTransparen();
                        } else {
                            MiuiGxzwIconView.this.dismissFingerpirntIcon();
                        }
                    }
                }
            } else {
                Log.e("MiuiGxzwViewIcon", "event.values[0] = " + event.values[0]);
            }
        }

        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    };
    private int mScreenHeight;
    private int mScreenWidth;
    private SensorManager mSensorManager;
    /* access modifiers changed from: private */
    public boolean mShowed = false;
    /* access modifiers changed from: private */
    public boolean mTouchDown = false;
    private boolean mUnlockLockout = false;
    private int mValidRegionCount;
    /* access modifiers changed from: private */
    public Vibrator mVibrator;

    interface CollectGxzwListener {
        void onCollectStateChange(boolean z);

        void onIconStateChange(boolean z);
    }

    public MiuiGxzwIconView(Context context) {
        super(context);
        initView();
    }

    private void initView() {
        LayoutInflater.from(getContext()).inflate(R.layout.miui_keyguard_gxzw_icon_view, this);
        this.mHighlightView = findViewById(R.id.gxzw_highlight);
        this.mHighlightView.setVisibility(0);
        this.mMiuiGxzwAnimView = new MiuiGxzwAnimView(getContext());
        this.mMiuiGxzwQuickOpenView = new MiuiGxzwQuickOpenView(getContext());
        this.mMiuiGxzwQuickOpenView.setDismissListener(this);
        setOnTouchListener(this);
        this.mSensorManager = (SensorManager) getContext().getSystemService("sensor");
        this.mPowerManager = (PowerManager) getContext().getSystemService("power");
        this.mVibrator = (Vibrator) getContext().getSystemService("vibrator");
        initSize();
        Display display = ((DisplayManager) getContext().getSystemService("display")).getDisplay(0);
        Point point = new Point();
        display.getSize(point);
        this.mScreenWidth = point.x;
        this.mScreenHeight = point.y;
        setSystemUiVisibility(4864);
        this.mKeyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(getContext());
        this.mKeyguardUpdateMonitor.registerCallback(this.mKeyguardUpdateMonitorCallback);
        this.mDisplayManager = (DisplayManager) getContext().getSystemService("display");
        this.mAlarmManager = (AlarmManager) getContext().getSystemService("alarm");
        this.mIntentFilter = new IntentFilter("action_set_icon_transparent");
        this.mPendingIntent = PendingIntent.getBroadcast(getContext(), 0, new Intent("action_set_icon_transparent"), 268435456);
        this.mHasPressureSensor = MiuiGxzwUtils.hasPressureSensor();
    }

    public void show(boolean lightIcon) {
        if (!this.mShowed) {
            boolean z = true;
            if (this.mMiuiGxzwQuickOpenView.isShow()) {
                this.mPendingShow = true;
                this.mPendingShowLightIcon = lightIcon;
                return;
            }
            Log.d("MiuiGxzwViewIcon", "show");
            this.mShowed = true;
            this.mNeedVibrator = true;
            this.mMiuiGxzwAnimView.show(lightIcon);
            WindowManager wm = (WindowManager) getContext().getSystemService("window");
            if (getContext().getResources().getConfiguration().orientation != 1 && !this.mKeyguardAuthen) {
                z = false;
            }
            this.mPortraitOrientation = z;
            initSize();
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(this.mGxzwIconWidth, this.mGxzwIconHeight, 2018, 25167368, -2);
            KeyguardCompatibilityHelperForP.setLayoutInDisplayCutoutMode(layoutParams);
            layoutParams.privateFlags |= MiuiGxzwUtils.PRIVATE_FLAG_IS_HBM_OVERLAY;
            layoutParams.gravity = 51;
            layoutParams.alpha = 0.0f;
            if (this.mKeyguardAuthen) {
                layoutParams.screenOrientation = 5;
            }
            updateLpByOrientation(layoutParams);
            layoutParams.setTitle("gxzw_icon");
            if (this.mDozing) {
                registerPutUpSensor();
                scheduleSetIconTransparen();
                getContext().registerReceiver(this.mBroadcastReceiver, this.mIntentFilter);
                layoutParams.flags &= -9;
                layoutParams.flags |= 131072;
                layoutParams.flags |= 32;
                if (!MiuiGxzwUtils.isFodAodShowEnable(getContext())) {
                    dismissFingerpirntIcon();
                }
            }
            if (isAttachedToWindow()) {
                wm.updateViewLayout(this, layoutParams);
            } else if (getParent() == null) {
                wm.addView(this, layoutParams);
            }
            setVisibility(0);
            this.mDeviceMoving = false;
            this.mDozeShowIconTimeout = false;
            this.mDisplayState = this.mDisplayManager.getDisplay(0).getState();
            this.mDisplayManager.registerDisplayListener(this, this.mHandler);
        }
    }

    private void initSize() {
        int detalX = (int) (((float) MiuiGxzwUtils.GXZW_ICON_WIDTH) * 0.2f);
        int detalY = (int) (((float) MiuiGxzwUtils.GXZW_ICON_HEIGHT) * 0.2f);
        this.mGxzwIconX = MiuiGxzwUtils.GXZW_ICON_X - detalX;
        this.mGxzwIconY = MiuiGxzwUtils.GXZW_ICON_Y - detalY;
        this.mGxzwIconWidth = MiuiGxzwUtils.GXZW_ICON_WIDTH + (detalX * 2);
        this.mGxzwIconHeight = MiuiGxzwUtils.GXZW_ICON_HEIGHT + (detalY * 2);
        setPadding(detalX, detalY, detalX, detalY);
    }

    public void dismiss() {
        this.mPendingShow = false;
        this.mPendingShowLightIcon = false;
        if (this.mShowed) {
            Log.d("MiuiGxzwViewIcon", "dismiss");
            if (this.mDozing) {
                setGxzwIconOpaque();
                unregisterPutUpSensor();
                unscheduleSetIconTransparen();
            }
            if (!this.mTouchDown || !MiuiGxzwManager.getInstance().isUnlockByGxzw() || !MiuiGxzwQuickOpenUtil.isQuickOpenEnable(getContext()) || AccessibilityManager.getInstance(getContext()).isTouchExplorationEnabled()) {
                this.mMiuiGxzwQuickOpenView.dismiss();
            } else {
                this.mMiuiGxzwQuickOpenView.show(MiuiGxzwManager.getInstance().getGxzwAuthFingerprintID());
            }
            onTouchUp(false);
            if (!this.mMiuiGxzwQuickOpenView.isShow()) {
                removeView();
            }
            this.mMiuiGxzwAnimView.dismiss();
            this.mShowed = false;
            this.mDeviceMoving = false;
            this.mDozeShowIconTimeout = false;
            this.mDisplayManager.unregisterDisplayListener(this);
        }
    }

    private void removeView() {
        WindowManager wm = (WindowManager) getContext().getSystemService("window");
        if (isAttachedToWindow()) {
            wm.removeViewImmediate(this);
        }
        setVisibility(8);
    }

    public void startDozing() {
        Log.d("MiuiGxzwViewIcon", "startDozing");
        this.mMiuiGxzwAnimView.startDozing();
        this.mDozing = true;
        if (this.mShowed) {
            registerPutUpSensor();
            scheduleSetIconTransparen();
            getContext().registerReceiver(this.mBroadcastReceiver, this.mIntentFilter);
            requestWindowFocus();
        }
        if (!MiuiGxzwUtils.isFodAodShowEnable(getContext())) {
            dismissFingerpirntIcon();
        }
        this.mDeviceMoving = false;
        this.mDozeShowIconTimeout = false;
    }

    public void stopDozing() {
        Log.d("MiuiGxzwViewIcon", "stopDozing");
        this.mMiuiGxzwAnimView.stopDozing();
        this.mDozing = false;
        if (this.mShowed) {
            setGxzwIconOpaque();
            unregisterPutUpSensor();
            unscheduleSetIconTransparen();
            getContext().unregisterReceiver(this.mBroadcastReceiver);
            WindowManager wm = (WindowManager) getContext().getSystemService("window");
            WindowManager.LayoutParams lp = (WindowManager.LayoutParams) getLayoutParams();
            lp.flags |= 8;
            lp.flags &= -131073;
            lp.flags &= -33;
            if (isAttachedToWindow()) {
                wm.updateViewLayout(this, lp);
            }
        }
        this.mDeviceMoving = false;
        this.mDozeShowIconTimeout = false;
    }

    public void setCollectGxzwListener(CollectGxzwListener l) {
        this.mCollectGxzwListener = l;
    }

    public void onScreenTurnedOn() {
        Log.d("MiuiGxzwViewIcon", "onScreenTurnedOn");
    }

    public void onStartedGoingToSleep() {
        Log.d("MiuiGxzwViewIcon", "onStartedGoingToSleep");
        this.mMiuiGxzwQuickOpenView.dismiss();
        this.mMiuiGxzwQuickOpenView.resetFingerID();
    }

    public void onKeyguardAuthen(boolean keyguardAuthen) {
        this.mKeyguardAuthen = keyguardAuthen;
        this.mMiuiGxzwAnimView.onKeyguardAuthen(keyguardAuthen);
        boolean portraitOrientation = true;
        if (getContext().getResources().getConfiguration().orientation != 1 && !this.mKeyguardAuthen) {
            portraitOrientation = false;
        }
        updateOrientation(portraitOrientation);
    }

    public void setEnrolling(boolean enrolling) {
        this.mEnrolling = enrolling;
        this.mMiuiGxzwAnimView.setEnrolling(enrolling);
    }

    public void setUnlockLockout(boolean lockout) {
        this.mUnlockLockout = lockout;
    }

    public void refreshIcon() {
        this.mMiuiGxzwAnimView.drawFingerprintIcon(this.mDozing);
    }

    private int findFodTouchEventIndex(MotionEvent event) {
        if (event.getPointerCount() == 1) {
            return 0;
        }
        float centerX = ((float) this.mGxzwIconWidth) / 2.0f;
        float centerY = ((float) this.mGxzwIconHeight) / 2.0f;
        int index = 0;
        float minDistance = Float.MAX_VALUE;
        int count = event.getPointerCount();
        for (int i = 0; i < count; i++) {
            float x = event.getX(i);
            float y = event.getY(i);
            float distance = (float) Math.pow((double) (((x - centerX) * (x - centerX)) + ((y - centerY) * (y - centerY))), 2.0d);
            if (distance < minDistance) {
                minDistance = distance;
                index = i;
            }
        }
        return index;
    }

    public boolean onTouch(View view, MotionEvent event) {
        int action;
        int index = findFodTouchEventIndex(event);
        float x = event.getX(index);
        float y = event.getY(index);
        float pressure = event.getPressure(index);
        float area = event.getToolMinor(index);
        int action2 = event.getAction();
        if (action2 != 6 && action2 != 262 && action2 != 518) {
            switch (action2) {
                case 0:
                    action = 0;
                    break;
                case 1:
                case 3:
                    action = 1;
                    break;
                case 2:
                    action = 2;
                    break;
                default:
                    action = 2;
                    break;
            }
        } else if (index == event.getActionIndex()) {
            action = 1;
        } else {
            action = 2;
        }
        String msg = String.format("onTouch: originalAction = %d, action = %d, x = %f, y = %f, pressure = %f, area = %f", new Object[]{Integer.valueOf(event.getAction()), Integer.valueOf(action), Float.valueOf(x), Float.valueOf(y), Float.valueOf(pressure), Float.valueOf(area)});
        if (event.getAction() == 2) {
            Log.i("MiuiGxzwViewIcon", msg);
        } else {
            Slog.i("MiuiGxzwViewIcon", msg);
        }
        if (this.mMiuiGxzwQuickOpenView.isShow()) {
            dispatchTouchEventForQuickOpenView(action, x, y);
        }
        if (!this.mShowed) {
            return false;
        }
        if (this.mDozing) {
            scheduleSetIconTransparen();
        } else {
            userActivity();
        }
        switch (action) {
            case 0:
                if (!this.mTouchDown && this.mShowed && (this.mHasPressureSensor || area > 0.0f)) {
                    onTouchDown();
                    break;
                }
            case 1:
            case 3:
                this.mPressureValue = 0;
                if (this.mTouchDown && this.mShowed) {
                    onTouchUp(true);
                    break;
                }
            case 2:
                this.mPressureValue = (int) (2048.0f * pressure);
                if (this.mPressureValue > 70 && this.mNeedVibrator && this.mTouchDown && this.mHasPressureSensor) {
                    Log.d("MiuiGxzwViewIcon", "pressure value is more than 70, vibrator!!!");
                    if (!this.mEnrolling) {
                        this.mVibrator.vibrate(12);
                    }
                    this.mNeedVibrator = false;
                    if (this.mKeyguardAuthen) {
                        this.mMiuiGxzwAnimView.startRecognizingAnim();
                    }
                    this.mHandler.removeMessages(1001);
                } else if (!this.mHasPressureSensor && !this.mTouchDown && area > 0.0f) {
                    onTouchDown();
                }
                if (!isInValidRegion(x, y) && this.mTouchDown && this.mShowed) {
                    onTouchUp(true);
                    break;
                }
                break;
        }
        return true;
    }

    private void dispatchTouchEventForQuickOpenView(int action, float x, float y) {
        switch (action) {
            case 0:
                this.mMiuiGxzwQuickOpenView.onTouchDown(((float) this.mGxzwIconX) + x, ((float) this.mGxzwIconY) + y);
                return;
            case 1:
            case 3:
                this.mMiuiGxzwQuickOpenView.onTouchUp(((float) this.mGxzwIconX) + x, ((float) this.mGxzwIconY) + y);
                return;
            case 2:
                this.mMiuiGxzwQuickOpenView.onTouchMove(((float) this.mGxzwIconX) + x, ((float) this.mGxzwIconY) + y);
                return;
            default:
                return;
        }
    }

    public boolean onHoverEvent(MotionEvent event) {
        if (!AccessibilityManager.getInstance(getContext()).isTouchExplorationEnabled()) {
            return super.onHoverEvent(event);
        }
        int action = event.getAction();
        if (action != 7) {
            switch (action) {
                case 9:
                    event.setAction(0);
                    setTalkbackDescription(getContext().getString(R.string.gxzw_area));
                    break;
                case 10:
                    event.setAction(1);
                    break;
            }
        } else {
            event.setAction(2);
        }
        onTouch(this, event);
        return true;
    }

    private void setTalkbackDescription(String str) {
        setContentDescription(str);
        announceForAccessibility(str);
    }

    private boolean isInValidRegion(float x, float y) {
        if (x >= -62.0f && y >= -62.0f && x <= ((float) (this.mGxzwIconWidth + 62)) && y <= ((float) (this.mGxzwIconHeight + 62))) {
            this.mValidRegionCount = 0;
        } else {
            this.mValidRegionCount++;
        }
        if (this.mValidRegionCount < 3) {
            return true;
        }
        return false;
    }

    public boolean isShowFingerprintIcon() {
        return !this.mGxzwIconTransparent;
    }

    private void onTouchDown() {
        if (!this.mTouchDown) {
            Log.i("MiuiGxzwViewIcon", "onTouchDown");
            turnOnAodIfScreenOff();
            setGxzwIconOpaque();
            this.mMiuiGxzwAnimView.setCollecting(true);
            if (Dependency.getHost() != null) {
                Dependency.getHost().fireFingerprintPressed(true);
            }
            if (this.mFingerprintLockout || this.mUnlockLockout) {
                this.mHandler.postDelayed(this.mGotoUnlockRunnable, 400);
            }
            this.mTouchDown = true;
            this.mNeedVibrator = true;
            this.mValidRegionCount = 0;
            WindowManager wm = (WindowManager) getContext().getSystemService("window");
            WindowManager.LayoutParams lp = (WindowManager.LayoutParams) getLayoutParams();
            lp.alpha = 1.0f;
            if (isAttachedToWindow()) {
                wm.updateViewLayout(this, lp);
            }
            if (this.mDozing) {
                MiuiGxzwManager.getInstance().requestDrawWackLock(AOD_DOZE_SUSPEND_DELAY);
            }
            if (this.mCollectGxzwListener != null) {
                this.mCollectGxzwListener.onCollectStateChange(true);
            }
            KeyguardCompatibilityHelperForP.saveShowTouchesState(getContext());
            if (this.mHasPressureSensor) {
                this.mHandler.removeMessages(1001);
                this.mHandler.sendEmptyMessageDelayed(1001, 500);
            } else if (this.mKeyguardAuthen) {
                this.mMiuiGxzwAnimView.startRecognizingAnim();
            }
        }
    }

    private void onTouchUp(boolean anim) {
        if (this.mTouchDown) {
            Log.i("MiuiGxzwViewIcon", "onTouchUp");
            this.mMiuiGxzwAnimView.setCollecting(false);
            this.mHandler.removeCallbacks(this.mGotoUnlockRunnable);
            if (this.mCollectGxzwListener != null) {
                this.mCollectGxzwListener.onCollectStateChange(false);
            }
            this.mTouchDown = false;
            this.mNeedVibrator = true;
            if (Dependency.getHost() != null) {
                Dependency.getHost().fireFingerprintPressed(false);
            }
            WindowManager wm = (WindowManager) getContext().getSystemService("window");
            WindowManager.LayoutParams lp = (WindowManager.LayoutParams) getLayoutParams();
            lp.alpha = 0.0f;
            if (isAttachedToWindow()) {
                wm.updateViewLayout(this, lp);
            }
            if (this.mDozing) {
                MiuiGxzwManager.getInstance().requestDrawWackLock(AOD_DOZE_SUSPEND_DELAY);
            }
            if (anim && this.mKeyguardAuthen) {
                this.mMiuiGxzwAnimView.startBackAnim();
            } else if (!this.mKeyguardAuthen || !MiuiGxzwManager.getInstance().isUnlockByGxzw()) {
                this.mMiuiGxzwAnimView.stopAnim();
            }
            this.mMiuiGxzwAnimView.stopTip();
            KeyguardCompatibilityHelperForP.restoreShowTouchesState(getContext());
            this.mHandler.removeMessages(1001);
        }
    }

    private void registerPutUpSensor() {
        if (this.mSensorManager == null) {
            Log.e("MiuiGxzwViewIcon", "sensor not supported");
            return;
        }
        if (MiuiGxzwUtils.isFodAodShowEnable(getContext())) {
            Sensor sensor = this.mSensorManager.getDefaultSensor(TYPE_PUT_UP_DETECT, true);
            if (sensor != null) {
                this.mSensorManager.registerListener(this.mPutUpSensorListener, sensor, 3);
            } else {
                Log.e("MiuiGxzwViewIcon", "no put up sensor");
            }
        }
    }

    private void unregisterPutUpSensor() {
        if (this.mSensorManager == null) {
            Log.e("MiuiGxzwViewIcon", "sensor not supported");
        } else {
            this.mSensorManager.unregisterListener(this.mPutUpSensorListener);
        }
    }

    /* access modifiers changed from: private */
    public void scheduleSetIconTransparen() {
        Log.i("MiuiGxzwViewIcon", "scheduleSetIconTransparen");
        this.mAlarmManager.cancel(this.mPendingIntent);
        this.mAlarmManager.setExact(2, SystemClock.elapsedRealtime() + 10000, this.mPendingIntent);
        this.mDozeShowIconTimeout = false;
    }

    private void unscheduleSetIconTransparen() {
        Log.i("MiuiGxzwViewIcon", "unscheduleSetIconTransparen");
        this.mAlarmManager.cancel(this.mPendingIntent);
    }

    /* access modifiers changed from: private */
    public void dismissFingerpirntIcon() {
        setGxzwIconTransparent();
        unscheduleSetIconTransparen();
    }

    /* access modifiers changed from: private */
    public void showFingerprintIcon() {
        if (this.mDozing) {
            setGxzwIconOpaque();
            scheduleSetIconTransparen();
            updateDozeScreenState();
        }
    }

    private void setGxzwIconTransparent() {
        Log.i("MiuiGxzwViewIcon", "setGxzwIconTransparent");
        updateGxzwIconAlpha(0.0f);
        if (this.mCollectGxzwListener != null && !this.mGxzwIconTransparent) {
            this.mCollectGxzwListener.onIconStateChange(true);
        }
        this.mGxzwIconTransparent = true;
        updateDozeScreenState();
    }

    private void setGxzwIconOpaque() {
        Log.i("MiuiGxzwViewIcon", "setGxzwIconOpaque");
        updateGxzwIconAlpha(1.0f);
        if (this.mCollectGxzwListener != null && this.mGxzwIconTransparent) {
            this.mCollectGxzwListener.onIconStateChange(false);
        }
        this.mGxzwIconTransparent = false;
    }

    private void turnOffScreenIfInAod() {
        Log.i("MiuiGxzwViewIcon", "turnOffScreenIfInAod");
        int state = this.mDisplayManager.getDisplay(0).getState();
        if (!this.mDozing) {
            return;
        }
        if (state == 3 || state == 4) {
            Dependency.getHost().fireAodState(false);
        }
    }

    private void turnOnAodIfScreenOff() {
        Log.i("MiuiGxzwViewIcon", "turnOnAodIfScreenOff");
        int state = this.mDisplayManager.getDisplay(0).getState();
        if (this.mDozing && state == 1) {
            Dependency.getHost().fireAodState(true);
        }
    }

    private void updateGxzwIconAlpha(float alpha) {
        this.mMiuiGxzwAnimView.setAlpha(alpha);
    }

    private void userActivity() {
        this.mPowerManager.userActivity(SystemClock.uptimeMillis(), false);
    }

    /* access modifiers changed from: private */
    public void showBouncer() {
        ((StatusBar) ((Application) getContext().getApplicationContext()).getSystemUIApplication().getComponent(StatusBar.class)).collapsePanels();
    }

    /* access modifiers changed from: private */
    public boolean isSupportNewFodSensor() {
        return Build.VERSION.SDK_INT >= 28;
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        Log.i("MiuiGxzwViewIcon", "dispatchKeyEvent: keycode = " + event.getKeyCode() + ", action = " + event.getAction());
        if (event.getKeyCode() != 354) {
            return super.dispatchKeyEvent(event);
        }
        if (event.getAction() == 0 && MiuiGxzwUtils.isFodAodShowEnable(getContext())) {
            showFingerprintIcon();
        }
        return true;
    }

    private void requestWindowFocus() {
        WindowManager wm = (WindowManager) getContext().getSystemService("window");
        WindowManager.LayoutParams lp = (WindowManager.LayoutParams) getLayoutParams();
        lp.flags &= -9;
        lp.flags |= 131072;
        lp.flags |= 32;
        if (isAttachedToWindow()) {
            wm.updateViewLayout(this, lp);
        }
    }

    public void onDisplayAdded(int displayId) {
    }

    public void onDisplayRemoved(int displayId) {
    }

    public void onDisplayChanged(int displayId) {
        if (displayId == 0) {
            int newState = this.mDisplayManager.getDisplay(displayId).getState();
            int oldState = this.mDisplayState;
            if (this.mKeyguardAuthen && this.mShowed && this.mDozing) {
                boolean z = false;
                boolean needUpdate = (newState == 3 || newState == 4) && oldState == 1 && this.mGxzwIconTransparent;
                if ((oldState == 3 || oldState == 4) && newState == 1 && !this.mGxzwIconTransparent) {
                    z = true;
                }
                if (z || needUpdate) {
                    updateDozeScreenState();
                }
            }
            this.mDisplayState = newState;
        }
    }

    private void updateDozeScreenState() {
        if (this.mGxzwIconTransparent) {
            turnOffScreenIfInAod();
        } else {
            turnOnAodIfScreenOff();
        }
    }

    public void onDismiss() {
        if (this.mPendingShow) {
            show(this.mPendingShowLightIcon);
            this.mPendingShow = false;
            this.mPendingShowLightIcon = false;
            return;
        }
        removeView();
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        boolean portraitOrientation = true;
        if (newConfig.orientation != 1 && !this.mKeyguardAuthen) {
            portraitOrientation = false;
        }
        updateOrientation(portraitOrientation);
    }

    private void updateOrientation(boolean portraitOrientation) {
        if (portraitOrientation != this.mPortraitOrientation && isAttachedToWindow()) {
            this.mPortraitOrientation = portraitOrientation;
            WindowManager.LayoutParams lp = (WindowManager.LayoutParams) getLayoutParams();
            updateLpByOrientation(lp);
            ((WindowManager) getContext().getSystemService("window")).updateViewLayout(this, lp);
        }
    }

    private void updateLpByOrientation(WindowManager.LayoutParams lp) {
        int height;
        int width;
        int y;
        int x;
        if (this.mPortraitOrientation) {
            x = this.mGxzwIconX;
            y = this.mGxzwIconY;
            width = this.mGxzwIconWidth;
            height = this.mGxzwIconHeight;
        } else {
            y = this.mGxzwIconX;
            x = this.mGxzwIconY;
            height = this.mGxzwIconWidth;
            width = this.mGxzwIconHeight;
        }
        int rotation = ((DisplayManager) getContext().getSystemService("display")).getDisplay(0).getRotation();
        if (!this.mKeyguardAuthen && (rotation == 2 || rotation == 3)) {
            x = ((this.mPortraitOrientation ? this.mScreenWidth : this.mScreenHeight) - x) - width;
            y = ((this.mPortraitOrientation ? this.mScreenHeight : this.mScreenWidth) - y) - height;
        }
        lp.width = width;
        lp.height = height;
        lp.x = x;
        lp.y = y;
    }
}
