package com.android.systemui.statusbar.phone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;
import com.android.systemui.Constants;
import com.android.systemui.Dependency;
import com.android.systemui.miui.statusbar.phone.MiuiStatusBarPromptController;
import com.android.systemui.statusbar.phone.OLEDScreenHelper;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class OLEDScreenHelper {
    private static final boolean DEBUG = Constants.DEBUG;
    /* access modifiers changed from: private */
    public static final int DEFAULT_INTERVAL = ((int) TimeUnit.MINUTES.toMillis(2));
    private Context mContext;
    /* access modifiers changed from: private */
    public int mDirection;
    /* access modifiers changed from: private */
    public Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 10001) {
                OLEDScreenHelper.this.update();
                int unused = OLEDScreenHelper.this.mDirection = OLEDScreenHelper.access$604(OLEDScreenHelper.this) % 4;
                OLEDScreenHelper.this.mHandler.sendEmptyMessageDelayed(10001, (long) OLEDScreenHelper.this.mInterval);
            }
        }
    };
    /* access modifiers changed from: private */
    public int mInterval;
    /* access modifiers changed from: private */
    public boolean mIsScreenOn;
    private MiuiStatusBarPromptController.OnPromptStateChangedListener mListener = new MiuiStatusBarPromptController.OnPromptStateChangedListener() {
        public void onPromptStateChanged(boolean isNormalMode, int topState) {
            if (Constants.IS_OLED_SCREEN) {
                if (isNormalMode) {
                    OLEDScreenHelper.this.restart();
                } else {
                    OLEDScreenHelper.this.stop(OLEDScreenHelper.this.mIsScreenOn);
                }
            }
        }
    };
    private NavigationBarView mNavigationBarView;
    /* access modifiers changed from: private */
    public int mPixels;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            OLEDScreenHelper.this.stop(OLEDScreenHelper.this.mIsScreenOn);
            int unused = OLEDScreenHelper.this.mInterval = intent.getIntExtra("interval", OLEDScreenHelper.DEFAULT_INTERVAL);
            int unused2 = OLEDScreenHelper.this.mPixels = intent.getIntExtra("pixels", 3);
            OLEDScreenHelper.this.start(OLEDScreenHelper.this.mIsScreenOn);
        }
    };
    private int mStatusBarMode;
    private View mStatusBarView;

    private static class FullTouchDelegate extends TouchDelegate {
        private View mDelegateView;

        public FullTouchDelegate(View delegateView) {
            super(new Rect(), delegateView);
            this.mDelegateView = delegateView;
        }

        public boolean onTouchEvent(MotionEvent event) {
            return this.mDelegateView.dispatchTouchEvent(event);
        }
    }

    static /* synthetic */ int access$604(OLEDScreenHelper x0) {
        int i = x0.mDirection + 1;
        x0.mDirection = i;
        return i;
    }

    public OLEDScreenHelper(Context context) {
        Log.d("OLEDScreenHelper", String.format("IS_OLED_SCREEN=%b", new Object[]{Boolean.valueOf(Constants.IS_OLED_SCREEN)}));
        this.mContext = context;
        this.mInterval = DEFAULT_INTERVAL;
        this.mPixels = 3;
        ((MiuiStatusBarPromptController) Dependency.get(MiuiStatusBarPromptController.class)).addPromptStateChangedListener("OLEDScreenHelper", this.mListener);
        if (DEBUG) {
            IntentFilter filter = new IntentFilter();
            filter.addAction("systemui.oled.strategy");
            this.mContext.registerReceiverAsUser(this.mReceiver, UserHandle.ALL, filter, null, null);
        }
    }

    public void setStatusBarView(View statusBarView) {
        if (Constants.IS_OLED_SCREEN) {
            this.mStatusBarView = statusBarView;
            this.mStatusBarView.post(new Runnable() {
                public final void run() {
                    ((View) OLEDScreenHelper.this.mStatusBarView.getParent()).setTouchDelegate(new OLEDScreenHelper.FullTouchDelegate(OLEDScreenHelper.this.mStatusBarView));
                }
            });
        }
    }

    public void setNavigationBarView(NavigationBarView navigationBarView) {
        if (Constants.IS_OLED_SCREEN) {
            this.mNavigationBarView = navigationBarView;
        }
    }

    public void onStatusBarModeChanged(int sbMode) {
        if (Constants.IS_OLED_SCREEN && this.mStatusBarMode != sbMode) {
            if (sbMode == 4 || sbMode == 2 || sbMode == 6) {
                restart();
            } else {
                stop(this.mIsScreenOn);
            }
            this.mStatusBarMode = sbMode;
        }
    }

    public void start(boolean isScreenOn) {
        this.mIsScreenOn = isScreenOn;
        if (Constants.IS_OLED_SCREEN) {
            if (DEBUG) {
                Log.d("OLEDScreenHelper", String.format("start isScreenOn=%b", new Object[]{Boolean.valueOf(isScreenOn)}));
            }
            if (isScreenOn && !this.mHandler.hasMessages(10001)) {
                this.mDirection = generateRandomDirection();
                this.mHandler.sendEmptyMessageDelayed(10001, (long) this.mInterval);
            }
        }
    }

    public void stop(boolean isScreenOn) {
        this.mIsScreenOn = isScreenOn;
        if (Constants.IS_OLED_SCREEN) {
            if (DEBUG) {
                Log.d("OLEDScreenHelper", "stop");
            }
            this.mHandler.removeMessages(10001);
            resetView(this.mStatusBarView);
            if (this.mNavigationBarView != null) {
                resetView(this.mNavigationBarView.getRecentsButton());
                resetView(this.mNavigationBarView.getHomeButton());
                resetView(this.mNavigationBarView.getBackButton());
            }
        }
    }

    private void resetView(View view) {
        if (view != null) {
            view.setTranslationX(0.0f);
            view.setTranslationY(0.0f);
        }
    }

    public void onConfigurationChanged() {
        restart();
    }

    /* access modifiers changed from: private */
    public void restart() {
        stop(this.mIsScreenOn);
        start(this.mIsScreenOn);
    }

    /* access modifiers changed from: private */
    public void update() {
        if (DEBUG) {
            Log.d("OLEDScreenHelper", String.format("update mDirection=%d mInterval=%d mPixels=%d", new Object[]{Integer.valueOf(this.mDirection), Integer.valueOf(this.mInterval), Integer.valueOf(this.mPixels)}));
        }
        updateView(this.mStatusBarView);
        if (this.mNavigationBarView != null) {
            updateView(this.mNavigationBarView.getRecentsButton());
            updateView(this.mNavigationBarView.getHomeButton());
            updateView(this.mNavigationBarView.getBackButton());
        }
    }

    private void updateView(View view) {
        if (view != null && view.isShown()) {
            float translationX = view.getTranslationX();
            float translationY = view.getTranslationY();
            switch (this.mDirection) {
                case 0:
                    view.setTranslationX(translationX - ((float) this.mPixels));
                    return;
                case 1:
                    view.setTranslationY(translationY - ((float) this.mPixels));
                    return;
                case 2:
                    view.setTranslationX(((float) this.mPixels) + translationX);
                    return;
                case 3:
                    view.setTranslationY(((float) this.mPixels) + translationY);
                    return;
                default:
                    return;
            }
        }
    }

    private int generateRandomDirection() {
        return new Random(SystemClock.uptimeMillis()).nextInt(4);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        Object[] objArr = new Object[6];
        objArr[0] = Constants.IS_OLED_SCREEN ? "T" : "f";
        objArr[1] = Integer.valueOf(this.mDirection);
        objArr[2] = Long.valueOf(TimeUnit.MILLISECONDS.toSeconds((long) this.mInterval));
        objArr[3] = Integer.valueOf(this.mPixels);
        objArr[4] = Integer.valueOf(this.mStatusBarMode);
        objArr[5] = this.mIsScreenOn ? "T" : "f";
        pw.println(String.format("  OLEDScreenHelper: [IS_OLED_SCREEN=%s mDirection=%d mInterval=%d mPixels=%d mStatusBarMode=%d mIsScreenOn=%s]", objArr));
    }
}
