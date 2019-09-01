package com.android.systemui.fsgesture;

import android.animation.Animator;
import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.KeyguardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Region;
import android.hardware.display.DisplayManager;
import android.hardware.input.InputManager;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.view.WindowManagerCompat;
import android.widget.FrameLayout;
import com.android.systemui.Constants;
import com.android.systemui.R;
import com.android.systemui.fsgesture.GestureBackArrowView;
import com.android.systemui.fsgesture.GesturesBackController;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.model.RecentsTaskLoadPlan;
import com.android.systemui.recents.model.RecentsTaskLoader;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.model.TaskStack;
import java.util.ArrayList;
import miui.util.ObjectReference;
import miui.util.ReflectionUtils;

public class GestureStubView extends FrameLayout {
    private static boolean isUserSetUp;
    /* access modifiers changed from: private */
    public boolean mAnimating;
    /* access modifiers changed from: private */
    public Animator.AnimatorListener mAnimatorListener;
    private float mAssistX1;
    private float mAssistX2;
    private int mBesideNotchArrowXStart;
    /* access modifiers changed from: private */
    public ContentResolver mContentResolver;
    /* access modifiers changed from: private */
    public Context mContext;
    /* access modifiers changed from: private */
    public int mCurrAction;
    /* access modifiers changed from: private */
    public float mCurrX;
    /* access modifiers changed from: private */
    public float mCurrY;
    private float mDensity;
    /* access modifiers changed from: private */
    public boolean mDisableQuickSwitch;
    private Display mDisplay;
    private StubViewDisplayListener mDisplayListener;
    private DisplayManager mDisplayManager;
    /* access modifiers changed from: private */
    public MotionEvent mDownEvent;
    /* access modifiers changed from: private */
    public float mDownX;
    private float mDownY;
    private int mEarWidth;
    /* access modifiers changed from: private */
    public GestureBackArrowView mGestureBackArrowView;
    private int mGestureStubDefaultSize;
    private WindowManager.LayoutParams mGestureStubParams;
    /* access modifiers changed from: private */
    public int mGestureStubPos;
    private int mGestureStubSize;
    private GesturesBackController.GesturesBackCallback mGesturesBackCallback;
    /* access modifiers changed from: private */
    public GesturesBackController mGesturesBackController;
    /* access modifiers changed from: private */
    public H mHandler;
    private boolean mIsGestureAnimationEnabled;
    /* access modifiers changed from: private */
    public boolean mIsGestureStarted;
    /* access modifiers changed from: private */
    public boolean mKeepHidden;
    /* access modifiers changed from: private */
    public KeyguardManager mKeyguardManager;
    private Configuration mLastConfiguration;
    private int[] mLocation;
    /* access modifiers changed from: private */
    public boolean mNeedAjustArrowPosition;
    private boolean mNeedRender;
    private int mNotchHeight;
    private int mNotchWidth;
    /* access modifiers changed from: private */
    public boolean mPendingResetStatus;
    private int mRotation;
    private int mScreenHeight;
    /* access modifiers changed from: private */
    public int mScreenWidth;
    private boolean mSwipeInRightDirection;
    /* access modifiers changed from: private */
    public Vibrator mVibrator;
    private WindowManager mWindowManager;

    /* renamed from: com.android.systemui.fsgesture.GestureStubView$3  reason: invalid class name */
    static /* synthetic */ class AnonymousClass3 {
        static final /* synthetic */ int[] $SwitchMap$com$android$systemui$fsgesture$GestureBackArrowView$ReadyState = new int[GestureBackArrowView.ReadyState.values().length];

        static {
            $SwitchMap$com$android$systemui$fsgesture$GestureStubView$EventPosition = new int[EventPosition.values().length];
            try {
                $SwitchMap$com$android$systemui$fsgesture$GestureStubView$EventPosition[EventPosition.UPON_NOTCH.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$systemui$fsgesture$GestureStubView$EventPosition[EventPosition.ALIGN_NOTCH.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$systemui$fsgesture$GestureStubView$EventPosition[EventPosition.BELOW_NOTCH.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$systemui$fsgesture$GestureBackArrowView$ReadyState[GestureBackArrowView.ReadyState.READY_STATE_BACK.ordinal()] = 1;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$android$systemui$fsgesture$GestureBackArrowView$ReadyState[GestureBackArrowView.ReadyState.READY_STATE_RECENT.ordinal()] = 2;
            } catch (NoSuchFieldError e5) {
            }
        }
    }

    private enum EventPosition {
        UPON_NOTCH,
        BELOW_NOTCH,
        ALIGN_NOTCH
    }

    private class H extends Handler {
        private H() {
        }

        public void handleMessage(Message msg) {
            MotionEvent downEvent = GestureStubView.this.mDownEvent;
            switch (msg.what) {
                case 255:
                    if (!GestureStubView.this.mIsGestureStarted) {
                        Log.d("GestureStubView", "handleMessage MSG_SET_GESTURE_STUB_UNTOUCHABLE");
                        GestureStubView.this.setVisibility(8);
                        if (GestureStubView.this.mGesturesBackController != null) {
                            GestureStubView.this.mGesturesBackController.reset();
                        }
                        sendMessageDelayed(obtainMessage(260), 20);
                        boolean unused = GestureStubView.this.mPendingResetStatus = true;
                        sendMessageDelayed(obtainMessage(257), 500);
                        break;
                    } else {
                        return;
                    }
                case 256:
                    if (downEvent != null && !GestureStubView.this.mIsGestureStarted) {
                        float diffX = GestureStubView.this.mCurrX - downEvent.getRawX();
                        float diffY = GestureStubView.this.mCurrY - downEvent.getRawY();
                        Log.d("GestureStubView", "handleMessage MSG_CHECK_GESTURE_STUB_TOUCHABLE diffX: " + diffX + " diffY: " + diffY + " mDownX: " + downEvent.getRawX() + " mDownY: " + downEvent.getRawY());
                        if (Math.abs(diffX) <= 30.0f && Math.abs(diffY) <= 30.0f) {
                            GestureStubView.this.mHandler.removeMessages(255);
                            GestureStubView.this.mHandler.sendMessage(GestureStubView.this.mHandler.obtainMessage(255));
                            break;
                        }
                    } else {
                        return;
                    }
                    break;
                case 257:
                    boolean unused2 = GestureStubView.this.mPendingResetStatus = false;
                    if (!GestureStubView.this.mKeepHidden) {
                        GestureStubView.this.setVisibility(0);
                    }
                    Log.d("GestureStubView", "handleMessage MSG_RESET_GESTURE_STUB_TOUCHABLE");
                    break;
                case 258:
                    boolean unused3 = GestureStubView.this.mAnimating = false;
                    GestureStubView.this.mGestureBackArrowView.setVisibility(8);
                    GestureStubView.this.resetRenderProperty("MSG_RESET_ANIMATING_STATUS");
                    Log.d("GestureStubView", "reset animating status");
                    break;
                case 259:
                    GestureStubView.this.hideGestureStub();
                    break;
                case 260:
                    if (GestureStubView.this.mCurrAction == 2 || GestureStubView.this.mCurrAction == 0) {
                        GestureStubView.this.injectMotionEvent(0);
                    } else {
                        GestureStubView.this.injectMotionEvent(0);
                        GestureStubView.this.injectMotionEvent(1);
                    }
                    if (GestureStubView.this.mDownEvent != null) {
                        GestureStubView.this.mDownEvent.recycle();
                        MotionEvent unused4 = GestureStubView.this.mDownEvent = null;
                        break;
                    }
                    break;
                case 261:
                    if (GestureStubView.this.mIsGestureStarted) {
                        if (!GestureStubView.this.mDisableQuickSwitch) {
                            GestureStubView.this.updateAssistXPosition();
                            if (!GestureStubView.this.isSwipeRightInDirection()) {
                                if (GestureStubView.this.isInSpeedLimit(20)) {
                                    if (Math.abs(GestureStubView.this.mCurrX - GestureStubView.this.mDownX) < ((float) GestureStubView.this.mScreenWidth) * 0.33f) {
                                        GestureStubView.this.mGestureBackArrowView.setReadyFinish(GestureBackArrowView.ReadyState.READY_STATE_BACK);
                                    } else {
                                        GestureStubView.this.mGestureBackArrowView.setReadyFinish(GestureBackArrowView.ReadyState.READY_STATE_RECENT);
                                    }
                                }
                            } else if (!GestureStubView.this.isInSpeedLimit(20)) {
                                if (GestureStubView.this.mGestureBackArrowView.getCurrentState() != GestureBackArrowView.ReadyState.READY_STATE_RECENT) {
                                    GestureStubView.this.mGestureBackArrowView.setReadyFinish(GestureBackArrowView.ReadyState.READY_STATE_BACK);
                                }
                            } else if (Math.abs(GestureStubView.this.mCurrX - GestureStubView.this.mDownX) > ((float) GestureStubView.this.mScreenWidth) * 0.33f) {
                                GestureStubView.this.mGestureBackArrowView.setReadyFinish(GestureBackArrowView.ReadyState.READY_STATE_RECENT);
                            } else {
                                GestureStubView.this.mGestureBackArrowView.setReadyFinish(GestureBackArrowView.ReadyState.READY_STATE_BACK);
                            }
                            GestureStubView.this.mHandler.sendEmptyMessageDelayed(261, 17);
                            break;
                        } else {
                            GestureStubView.this.mGestureBackArrowView.setReadyFinish(GestureBackArrowView.ReadyState.READY_STATE_BACK);
                            break;
                        }
                    } else {
                        return;
                    }
            }
        }
    }

    private class StubViewDisplayListener implements DisplayManager.DisplayListener {
        private StubViewDisplayListener() {
        }

        public void onDisplayAdded(int displayId) {
        }

        public void onDisplayRemoved(int displayId) {
        }

        public void onDisplayChanged(int displayId) {
            GestureStubView.this.adaptRotation(true);
        }
    }

    /* access modifiers changed from: private */
    public boolean isInSpeedLimit(int expectValue) {
        boolean z = false;
        if (this.mDownEvent == null) {
            return false;
        }
        if (Math.abs(this.mCurrX - this.mAssistX1) < ((float) expectValue) && Math.abs(this.mCurrX - this.mAssistX2) < ((float) expectValue)) {
            z = true;
        }
        return z;
    }

    /* access modifiers changed from: private */
    public void updateAssistXPosition() {
        this.mAssistX1 += (this.mCurrX - this.mAssistX1) / 4.0f;
        this.mAssistX2 += (this.mCurrX - this.mAssistX2) / 2.0f;
    }

    /* access modifiers changed from: private */
    public boolean isSwipeRightInDirection() {
        if ((this.mGestureStubPos != 0 || this.mCurrX >= this.mAssistX1) && (this.mGestureStubPos != 1 || this.mCurrX <= this.mAssistX1)) {
            this.mSwipeInRightDirection = true;
        } else {
            this.mSwipeInRightDirection = false;
        }
        return this.mSwipeInRightDirection;
    }

    public static Task getNextTask(Context context, boolean startActivity, int position) {
        SystemServicesProxy ssp = Recents.getSystemServices();
        RecentsTaskLoader loader = Recents.getTaskLoader();
        RecentsTaskLoadPlan plan = loader.createLoadPlan(context);
        loader.preloadTasks(plan, -1, false);
        TaskStack focusedStack = plan.getTaskStack();
        if (focusedStack == null || focusedStack.getTaskCount() == 0) {
            return null;
        }
        ActivityManager.RunningTaskInfo runningTask = ssp.getRunningTask();
        if (runningTask == null) {
            return null;
        }
        ArrayList<Task> tasks = focusedStack.getStackTasks();
        Task toTask = null;
        int taskCount = tasks.size();
        int i = 0;
        while (true) {
            if (i >= taskCount - 1) {
                break;
            } else if (tasks.get(i).key.id == runningTask.id) {
                toTask = tasks.get(i + 1);
                break;
            } else {
                i++;
            }
        }
        if (toTask == null && taskCount >= 1 && "com.miui.home".equals(runningTask.baseActivity.getPackageName())) {
            toTask = tasks.get(0);
        }
        if (toTask != null && toTask.icon == null) {
            toTask.icon = loader.getAndUpdateActivityIcon(toTask.key, toTask.taskDescription, context.getResources(), true);
        }
        if (startActivity && toTask != null) {
            ActivityOptions launchOpts = null;
            if (position == 0) {
                launchOpts = ActivityOptions.makeCustomAnimation(context, R.anim.recents_quick_switch_left_enter, R.anim.recents_quick_switch_left_exit);
            } else if (position == 1) {
                launchOpts = ActivityOptions.makeCustomAnimation(context, R.anim.recents_quick_switch_right_enter, R.anim.recents_quick_switch_right_exit);
            }
            ssp.startActivityFromRecents(context, toTask.key, toTask.title, launchOpts);
        }
        return toTask;
    }

    static boolean supportNextTask(KeyguardManager keyguardManager, ContentResolver contentResolver) {
        return !keyguardManager.isKeyguardLocked() && isUserSetUp(contentResolver);
    }

    private static boolean isUserSetUp(ContentResolver contentResolver) {
        if (!isUserSetUp) {
            boolean z = false;
            if (!(Settings.Global.getInt(contentResolver, "device_provisioned", 0) == 0 || Settings.Secure.getIntForUser(contentResolver, "user_setup_complete", 0, -2) == 0)) {
                z = true;
            }
            isUserSetUp = z;
        }
        return isUserSetUp;
    }

    public GestureStubView(Context context) {
        this(context, -1);
    }

    public GestureStubView(Context context, int gestureStubSize) {
        super(context);
        this.mLocation = new int[2];
        this.mCurrAction = -1;
        this.mScreenWidth = -1;
        this.mScreenHeight = -1;
        this.mGestureStubPos = -1;
        this.mGestureStubSize = -1;
        this.mGestureStubDefaultSize = -1;
        this.mNotchHeight = -1;
        this.mNotchWidth = -1;
        this.mEarWidth = -1;
        this.mRotation = 0;
        this.mDensity = -1.0f;
        this.mNeedAjustArrowPosition = false;
        this.mDisableQuickSwitch = false;
        this.mAnimatorListener = new Animator.AnimatorListener() {
            public void onAnimationStart(Animator animation) {
                boolean unused = GestureStubView.this.mAnimating = true;
            }

            public void onAnimationEnd(Animator animation) {
                boolean unused = GestureStubView.this.mAnimating = false;
                GestureStubView.this.mHandler.removeMessages(258);
                GestureStubView.this.mGestureBackArrowView.setVisibility(8);
                GestureStubView.this.resetRenderProperty("onAnimationEnd");
            }

            public void onAnimationCancel(Animator animation) {
                boolean unused = GestureStubView.this.mAnimating = false;
            }

            public void onAnimationRepeat(Animator animation) {
            }
        };
        this.mGesturesBackCallback = new GesturesBackController.GesturesBackCallback() {
            public void onSwipeStart(boolean needAnimation, float offsetY) {
                boolean unused = GestureStubView.this.mIsGestureStarted = true;
                GestureStubView.this.clearMessages();
                Log.d("GestureStubView", "onSwipeStart: needAnimation: " + needAnimation);
                if (needAnimation) {
                    GestureStubView.this.mGestureBackArrowView.setVisibility(0);
                    GestureStubView.this.renderView();
                    if (GestureStubView.this.mNeedAjustArrowPosition) {
                        int[] params = GestureStubView.this.getParams(offsetY);
                        GestureStubView.this.mGestureBackArrowView.onActionDown((float) params[0], (float) params[1], (float) params[2]);
                        return;
                    }
                    GestureStubView.this.mGestureBackArrowView.onActionDown(offsetY, 0.0f, -1.0f);
                }
            }

            public void onSwipeProcess(boolean readyFinish, float offsetX) {
                if (readyFinish) {
                    GestureStubView.this.mHandler.removeMessages(261);
                    GestureStubView.this.mHandler.sendEmptyMessage(261);
                } else {
                    GestureStubView.this.mGestureBackArrowView.setReadyFinish(GestureBackArrowView.ReadyState.READY_STATE_NONE);
                }
                GestureStubView.this.mGestureBackArrowView.onActionMove(offsetX);
            }

            public void onSwipeStop(boolean isFinish, float offsetX) {
                Log.d("GestureStubView", "onSwipeStop");
                boolean unused = GestureStubView.this.mIsGestureStarted = false;
                GestureStubView.this.mHandler.sendMessageDelayed(GestureStubView.this.mHandler.obtainMessage(258), 500);
                if (!GestureStubView.this.isInSpeedLimit(20)) {
                    if (GestureStubView.this.isSwipeRightInDirection()) {
                        GestureStubView.this.mGestureBackArrowView.setReadyFinish(GestureBackArrowView.ReadyState.READY_STATE_BACK);
                    } else {
                        GestureStubView.this.mGestureBackArrowView.setReadyFinish(GestureBackArrowView.ReadyState.READY_STATE_NONE);
                    }
                }
                if (isFinish) {
                    switch (AnonymousClass3.$SwitchMap$com$android$systemui$fsgesture$GestureBackArrowView$ReadyState[GestureStubView.this.mGestureBackArrowView.getCurrentState().ordinal()]) {
                        case 1:
                            GestureStubView.this.injectKeyEvent(4);
                            break;
                        case 2:
                            if (!GestureStubView.supportNextTask(GestureStubView.this.mKeyguardManager, GestureStubView.this.mContentResolver) || GestureStubView.getNextTask(GestureStubView.this.mContext, true, GestureStubView.this.mGestureStubPos) == null) {
                                GestureStubView.this.mVibrator.vibrate(100);
                                break;
                            }
                    }
                }
                GestureStubView.this.mHandler.removeMessages(261);
                GestureStubView.this.mGestureBackArrowView.onActionUp(GesturesBackController.convertOffset(offsetX), GestureStubView.this.mAnimatorListener);
            }

            public void onSwipeStopDirect() {
                Log.d("GestureStubView", "onSwipeStopDirect");
                boolean unused = GestureStubView.this.mIsGestureStarted = false;
                GestureStubView.this.injectKeyEvent(4);
            }
        };
        this.mLastConfiguration = new Configuration();
        this.mContext = context;
        this.mLastConfiguration.updateFrom(getResources().getConfiguration());
        this.mIsGestureStarted = false;
        this.mGestureStubPos = 2;
        this.mHandler = new H();
        this.mWindowManager = (WindowManager) context.getSystemService("window");
        initGestureStubSize(gestureStubSize);
        this.mWindowManager.addView(this, getGestureStubWindowParam());
        this.mDisplayListener = new StubViewDisplayListener();
        this.mDisplayManager = (DisplayManager) context.getSystemService("display");
        this.mKeyguardManager = (KeyguardManager) context.getSystemService("keyguard");
        this.mContentResolver = context.getContentResolver();
        isUserSetUp = isUserSetUp(this.mContentResolver);
        this.mVibrator = (Vibrator) this.mContext.getSystemService("vibrator");
        setVisibility(8);
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mDisplayManager.registerDisplayListener(this.mDisplayListener, null);
    }

    /* access modifiers changed from: private */
    public int[] getParams(float currY) {
        EventPosition position;
        if (currY < ((float) this.mEarWidth)) {
            position = EventPosition.UPON_NOTCH;
        } else if (currY > ((float) (this.mScreenWidth - this.mEarWidth))) {
            position = EventPosition.BELOW_NOTCH;
        } else {
            position = ((float) this.mNotchWidth) > 164.0f * this.mDensity ? EventPosition.ALIGN_NOTCH : EventPosition.UPON_NOTCH;
        }
        int currentWindowBesideNotchArrowXStart = adaptBesideNotchArrowXStart();
        int[] params = new int[3];
        switch (position) {
            case UPON_NOTCH:
                params[0] = (this.mEarWidth / 3) * 2;
                params[1] = currentWindowBesideNotchArrowXStart;
                params[2] = (int) (((float) this.mEarWidth) + (this.mDensity * 36.0f) + 0.5f);
                break;
            case ALIGN_NOTCH:
                params[0] = this.mScreenWidth / 2;
                params[1] = this.mNotchHeight - 1;
                params[2] = (int) ((((float) this.mNotchWidth) - (this.mDensity * 54.0f)) + 0.5f);
                break;
            case BELOW_NOTCH:
                params[0] = this.mScreenWidth - ((this.mEarWidth / 3) * 2);
                params[1] = currentWindowBesideNotchArrowXStart;
                params[2] = (int) (((float) this.mEarWidth) + (this.mDensity * 36.0f) + 0.5f);
                break;
        }
        return params;
    }

    private int adaptBesideNotchArrowXStart() {
        int currentWindowBesideNotchArrowXStart = this.mBesideNotchArrowXStart;
        if (currentWindowBesideNotchArrowXStart <= 0) {
            return currentWindowBesideNotchArrowXStart;
        }
        boolean z = false;
        ObjectReference reference = ReflectionUtils.tryCallMethod(getViewRootImpl(), "isFocusWindowAdaptNotch", Boolean.class, new Object[0]);
        if (reference != null) {
            z = ((Boolean) reference.get()).booleanValue();
        }
        if (Boolean.valueOf(z).booleanValue()) {
            return 0;
        }
        return currentWindowBesideNotchArrowXStart;
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mDisplayManager.unregisterDisplayListener(this.mDisplayListener);
    }

    public void showGestureStub() {
        if (!this.mAnimating) {
            this.mHandler.removeMessages(259);
            this.mKeepHidden = false;
            resetRenderProperty("showGestureStub");
            if (this.mGestureBackArrowView != null) {
                this.mGestureBackArrowView.reset();
            }
            setVisibility(0);
            Log.d("GestureStubView", "showGestureStub");
        }
    }

    public void hideGestureStubDelay() {
        this.mHandler.removeMessages(259);
        this.mHandler.sendEmptyMessageDelayed(259, 300);
    }

    /* access modifiers changed from: private */
    public void hideGestureStub() {
        this.mKeepHidden = true;
        if (this.mDownEvent != null) {
            this.mDownEvent.recycle();
            this.mDownEvent = null;
        }
        setVisibility(8);
        Log.d("GestureStubView", "hideGestureStub");
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        int changes = this.mLastConfiguration.updateFrom(newConfig);
        boolean densitySizeChange = true;
        boolean screenSizeChange = (changes & 1024) != 0;
        boolean smallestScreenSizeChange = (changes & 2048) != 0;
        if ((changes & 4096) == 0) {
            densitySizeChange = false;
        }
        if (!this.mKeepHidden && densitySizeChange && smallestScreenSizeChange && screenSizeChange) {
            initScreenSizeAndDensity(-1);
            if (this.mGestureBackArrowView != null) {
                removeView(this.mGestureBackArrowView);
            }
            setGestureStubPosition(this.mGestureStubPos);
            if (this.mGesturesBackController != null) {
                this.mGesturesBackController.setGestureEdgeWidth(this.mGestureStubSize, this.mScreenWidth - this.mGestureStubSize);
            }
        }
        adaptRotation(false);
    }

    public void clearGestureStub() {
        hideGestureStub();
        this.mWindowManager.removeView(this);
        Log.d("GestureStubView", "clearGestureStub");
    }

    private void initGestureStubSize(int gestureStubSize) {
        initScreenSizeAndDensity(gestureStubSize);
        boolean mIsNotch = true;
        if (SystemProperties.getInt("ro.miui.notch", 0) != 1) {
            mIsNotch = false;
        }
        if (mIsNotch) {
            this.mNotchHeight = this.mContext.getResources().getDimensionPixelSize(R.dimen.notch_height);
            this.mNotchWidth = this.mContext.getResources().getDimensionPixelSize(R.dimen.notch_width);
            this.mEarWidth = ((this.mScreenHeight < this.mScreenWidth ? this.mScreenHeight : this.mScreenWidth) - this.mNotchWidth) / 2;
        }
        this.mGesturesBackController = new GesturesBackController(this.mGesturesBackCallback, this.mGestureStubSize, this.mScreenWidth - this.mGestureStubSize);
        adaptRotation(false);
    }

    private void initScreenSizeAndDensity(int gestureStubSize) {
        Point realSize = new Point();
        this.mDisplay = this.mWindowManager.getDefaultDisplay();
        this.mDisplay.getRealSize(realSize);
        if (realSize.y > realSize.x) {
            this.mScreenWidth = realSize.x;
            this.mScreenHeight = realSize.y;
        } else {
            this.mScreenWidth = realSize.y;
            this.mScreenHeight = realSize.x;
        }
        if (gestureStubSize == -1) {
            int i = this.mScreenWidth;
            if (i == 720) {
                this.mGestureStubSize = 40;
                this.mGestureStubDefaultSize = 40;
            } else if (i != 1080) {
                this.mGestureStubSize = 54;
                this.mGestureStubDefaultSize = 54;
            } else {
                this.mGestureStubSize = 54;
                this.mGestureStubDefaultSize = 54;
            }
        } else {
            this.mGestureStubSize = gestureStubSize;
            this.mGestureStubDefaultSize = gestureStubSize;
        }
        this.mDensity = this.mContext.getResources().getDisplayMetrics().density;
    }

    private void adaptNotch() {
        if (Constants.IS_NOTCH && this.mNotchHeight > 0) {
            int rotation = this.mDisplay.getRotation();
            if (rotation == 1) {
                if (this.mGestureStubPos == 0) {
                    this.mGestureStubSize = this.mGestureStubDefaultSize + this.mNotchHeight;
                    this.mNeedAjustArrowPosition = true;
                } else if (this.mGestureStubPos == 1) {
                    this.mGestureStubSize = this.mGestureStubDefaultSize;
                    this.mNeedAjustArrowPosition = false;
                }
                if (((float) this.mNotchWidth) < 164.0f * this.mDensity) {
                    this.mBesideNotchArrowXStart = this.mNotchHeight - 1;
                }
            } else if (rotation != 3) {
                this.mGestureStubSize = this.mGestureStubDefaultSize;
                this.mNeedAjustArrowPosition = false;
                this.mBesideNotchArrowXStart = 0;
            } else {
                if (this.mGestureStubPos == 1) {
                    this.mGestureStubSize = this.mGestureStubDefaultSize + this.mNotchHeight;
                    this.mNeedAjustArrowPosition = true;
                } else if (this.mGestureStubPos == 0) {
                    this.mGestureStubSize = this.mGestureStubDefaultSize;
                    this.mNeedAjustArrowPosition = false;
                }
                if (((float) this.mNotchWidth) < 164.0f * this.mDensity) {
                    this.mBesideNotchArrowXStart = this.mNotchHeight - 1;
                }
            }
        }
    }

    public void setSize(int size) {
        this.mGestureStubDefaultSize = size;
        this.mGestureStubSize = size;
        adaptNotch();
        if (this.mGesturesBackController != null) {
            this.mGesturesBackController.mGestureEdgeLeft = this.mGestureStubSize;
            this.mGesturesBackController.mGestureEdgeRight = this.mScreenWidth - this.mGestureStubSize;
        }
        try {
            if (isAttachedToWindow()) {
                resetRenderProperty("setSize");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean gatherTransparentRegion(Region region) {
        if (!this.mNeedRender && region != null) {
            int w = getWidth();
            int h = getHeight();
            Log.d("GestureStubView", "gatherTransparentRegion: need render w:" + w + "  h:" + h);
            if (w > 0 && h > 0) {
                getLocationInWindow(this.mLocation);
                int l = this.mLocation[0];
                int t = this.mLocation[1];
                region.op(l, t, l + w, t + h, Region.Op.UNION);
                return false;
            }
        }
        return super.gatherTransparentRegion(region);
    }

    /* access modifiers changed from: private */
    public void resetRenderProperty(String caller) {
        if (!this.mKeepHidden) {
            this.mWindowManager.updateViewLayout(this, getGestureStubWindowParam());
            Log.d("GestureStubView", "resetRenderProperty: " + caller);
            if (getParent() != null) {
                this.mNeedRender = false;
                getParent().requestTransparentRegion(this);
            }
        }
    }

    /* access modifiers changed from: private */
    public void renderView() {
        if (!this.mKeepHidden) {
            this.mWindowManager.updateViewLayout(this, getAnimatingLayoutParam());
            Log.d("GestureStubView", "renderView");
            if (getParent() != null) {
                this.mNeedRender = true;
                getParent().requestTransparentRegion(this);
            }
        }
    }

    private WindowManager.LayoutParams getGestureStubWindowParam() {
        int i;
        int h;
        if (this.mGestureStubPos == 2) {
            h = this.mGestureStubSize;
            i = -1;
        } else {
            i = this.mGestureStubSize;
            if (this.mRotation == 0 || this.mRotation == 2) {
                h = (int) (((float) this.mScreenHeight) * 0.6f);
            } else {
                h = (int) (((float) this.mScreenWidth) * 0.6f);
            }
        }
        int w = i;
        boolean z = false;
        if (this.mGestureStubParams == null) {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(w, h, 2027, 296, 1);
            this.mGestureStubParams = layoutParams;
            WindowManagerCompat.setLayoutInDisplayCutoutMode(this.mGestureStubParams, 1);
            setBackgroundColor(0);
            this.mGestureStubParams.alpha = 1.0f;
        } else {
            this.mGestureStubParams.width = w;
            this.mGestureStubParams.height = h;
        }
        int gravityVertical = 80;
        if (this.mGestureStubPos == 2) {
            this.mGestureStubParams.gravity = 80;
            this.mGestureStubParams.setTitle("GestureStubBottom");
            return this.mGestureStubParams;
        }
        if (this.mGestureStubPos == 0) {
            z = true;
        }
        boolean isLeft = z;
        int gravityHorizontal = isLeft ? 3 : 5;
        if (!(this.mRotation == 0 || this.mRotation == 2)) {
            gravityVertical = 16;
        }
        this.mGestureStubParams.gravity = gravityHorizontal | gravityVertical;
        this.mGestureStubParams.setTitle(isLeft ? "GestureStubLeft" : "GestureStubRight");
        return this.mGestureStubParams;
    }

    private WindowManager.LayoutParams getAnimatingLayoutParam() {
        WindowManager.LayoutParams gestureStubParams = new WindowManager.LayoutParams(-1, -1, 2027, 296, 1);
        WindowManagerCompat.setLayoutInDisplayCutoutMode(gestureStubParams, 1);
        gestureStubParams.alpha = 1.0f;
        return gestureStubParams;
    }

    public boolean onTouchEvent(MotionEvent event) {
        this.mCurrAction = event.getAction();
        switch (this.mCurrAction) {
            case 0:
                float rawX = event.getRawX();
                this.mAssistX2 = rawX;
                this.mAssistX1 = rawX;
                this.mDownX = rawX;
                this.mCurrX = rawX;
                float rawY = event.getRawY();
                this.mDownY = rawY;
                this.mCurrY = rawY;
                if (this.mDownEvent != null) {
                    this.mDownEvent.recycle();
                }
                this.mDownEvent = event.copy();
                this.mHandler.removeMessages(256);
                this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(256), 150);
                Log.d("GestureStubView", "onTouch ACTION_DOWN sendMessageDelayed MSG_CHECK_GESTURE_STUB_TOUCHABLE");
                break;
            case 1:
            case 3:
                MotionEvent downEvent = this.mDownEvent;
                if (downEvent != null) {
                    this.mCurrX = event.getRawX();
                    this.mCurrY = event.getRawY();
                    if (event.getEventTime() - downEvent.getEventTime() < 150 && !this.mIsGestureStarted) {
                        clearMessages();
                        float diffX = this.mCurrX - downEvent.getRawX();
                        float diffY = this.mCurrY - downEvent.getRawY();
                        if (!this.mIsGestureStarted && Math.abs(diffX) <= 30.0f && Math.abs(diffY) <= 30.0f) {
                            this.mHandler.sendMessage(this.mHandler.obtainMessage(255));
                            Log.d("GestureStubView", "currTime - mDownTime < MSG_CHECK_GESTURE_STUB_TOUCHABLE_TIMEOUT updateViewLayout UnTouchable, diffX:" + diffX + " diffY:" + diffY);
                        }
                    }
                    Log.d("GestureStubView", "ACTION_UP: mIsGestureStarted: " + this.mIsGestureStarted + " mIsGestureAnimationEnabled: " + this.mIsGestureAnimationEnabled);
                    if (this.mIsGestureStarted && this.mIsGestureAnimationEnabled) {
                        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(258), 500);
                    }
                    this.mIsGestureStarted = false;
                    break;
                } else {
                    return true;
                }
            case 2:
                this.mCurrX = event.getRawX();
                this.mCurrY = event.getRawY();
                if (Math.abs(this.mCurrY - this.mDownY) > 2.0f * Math.abs(this.mCurrX - this.mDownX) && !this.mIsGestureStarted) {
                    this.mHandler.sendMessage(this.mHandler.obtainMessage(255));
                    Log.d("GestureStubView", "up-slide detected, sendMessage MSG_SET_GESTURE_STUB_UNTOUCHABLE");
                    break;
                }
        }
        if (this.mGesturesBackController == null || this.mGestureStubPos == 2 || this.mPendingResetStatus) {
            return false;
        }
        this.mGesturesBackController.onPointerEvent(event);
        return true;
    }

    public void enableGestureBackAnimation(boolean enable) {
        this.mIsGestureAnimationEnabled = enable;
        this.mGesturesBackController.enableGestureBackAnimation(enable);
        Log.d("GestureStubView", "enableGestureBackAnimation enable:" + enable);
    }

    public void disableQuickSwitch(boolean disableQuickSwitch) {
        this.mDisableQuickSwitch = disableQuickSwitch;
    }

    public void setGestureStubPosition(int pos) {
        this.mGestureStubPos = pos;
        this.mGestureBackArrowView = new GestureBackArrowView(this.mContext, this.mGestureStubPos);
        addView(this.mGestureBackArrowView);
        Point realSize = new Point();
        this.mDisplay = this.mWindowManager.getDefaultDisplay();
        this.mDisplay.getRealSize(realSize);
        this.mGestureBackArrowView.setDisplayWidth(realSize.x);
        resetRenderProperty("setGestureStubPosition");
    }

    /* access modifiers changed from: private */
    public void clearMessages() {
        this.mHandler.removeMessages(256);
        this.mHandler.removeMessages(255);
    }

    /* access modifiers changed from: private */
    public void injectMotionEvent(int action) {
        MotionEvent downEvent = this.mDownEvent;
        if (downEvent != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("injectMotionEvent action :");
            int i = action;
            sb.append(i);
            sb.append(" downX: ");
            sb.append(downEvent.getRawX());
            sb.append(" downY: ");
            sb.append(downEvent.getRawY());
            Log.d("GestureStubView", sb.toString());
            MotionEvent.PointerProperties[] arrayOfPointerProperties = MotionEvent.PointerProperties.createArray(1);
            downEvent.getPointerProperties(0, arrayOfPointerProperties[0]);
            MotionEvent.PointerCoords[] arrayOfPointerCoords = MotionEvent.PointerCoords.createArray(1);
            downEvent.getPointerCoords(0, arrayOfPointerCoords[0]);
            arrayOfPointerCoords[0].x = downEvent.getRawX();
            arrayOfPointerCoords[0].y = downEvent.getRawY();
            InputManager.getInstance().injectInputEvent(MotionEvent.obtain(downEvent.getDownTime(), downEvent.getEventTime(), i, 1, arrayOfPointerProperties, arrayOfPointerCoords, downEvent.getMetaState(), downEvent.getButtonState(), downEvent.getXPrecision(), downEvent.getYPrecision(), downEvent.getDeviceId(), downEvent.getEdgeFlags(), downEvent.getSource(), downEvent.getFlags()), 0);
        }
    }

    /* access modifiers changed from: private */
    public void injectKeyEvent(int keyCode) {
        StringBuilder sb = new StringBuilder();
        sb.append("injectKeyEvent keyCode:");
        int i = keyCode;
        sb.append(i);
        Log.d("GestureStubView", sb.toString());
        long now = SystemClock.uptimeMillis();
        long j = now;
        long j2 = now;
        int i2 = i;
        KeyEvent down = new KeyEvent(j, j2, 0, i2, 0, 0, -1, 0, 8, 257);
        long j3 = now;
        KeyEvent keyEvent = new KeyEvent(j, j2, 1, i2, 0, 0, -1, 0, 8, 257);
        KeyEvent up = keyEvent;
        InputManager.getInstance().injectInputEvent(down, 0);
        InputManager.getInstance().injectInputEvent(up, 0);
    }

    /* access modifiers changed from: private */
    public void adaptRotation(boolean bFromDisplayListener) {
        int currentRotation = this.mDisplay.getRotation();
        if ((!bFromDisplayListener && currentRotation != this.mRotation) || (bFromDisplayListener && Math.abs(this.mRotation - currentRotation) == 2)) {
            this.mRotation = currentRotation;
            setSize(this.mGestureStubDefaultSize);
            if (this.mGestureBackArrowView != null) {
                Point realSize = new Point();
                this.mDisplay = this.mWindowManager.getDefaultDisplay();
                this.mDisplay.getRealSize(realSize);
                this.mGestureBackArrowView.setDisplayWidth(realSize.x);
            }
        }
    }
}
