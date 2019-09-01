package com.android.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.ActivityManager;
import android.app.ActivityManagerCompat;
import android.app.ActivityManagerNative;
import android.app.ActivityOptions;
import android.app.IActivityManager;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Xfermode;
import android.hardware.input.InputManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.MiuiMultiWindowUtils;
import android.view.IGestureStubListener;
import android.view.KeyEvent;
import android.view.MiuiWindowManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.Toast;
import com.android.systemui.Application;
import com.android.systemui.Constants;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.fsgesture.IFsGestureCallback;
import com.android.systemui.fsgesture.TransitionAnimationSpec;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.events.RecentsEventBus;
import com.android.systemui.recents.events.activity.AnimFirstTaskViewAlphaEvent;
import com.android.systemui.recents.events.activity.FsGestureEnterRecentsCompleteEvent;
import com.android.systemui.recents.events.activity.FsGestureEnterRecentsEvent;
import com.android.systemui.recents.events.activity.FsGestureLaunchTargetTaskViewRectEvent;
import com.android.systemui.recents.events.activity.FsGesturePreloadRecentsEvent;
import com.android.systemui.recents.events.activity.FsGestureRecentsViewWrapperEvent;
import com.android.systemui.recents.events.activity.FsGestureShowFirstCardEvent;
import com.android.systemui.recents.events.activity.FsGestureSlideInEvent;
import com.android.systemui.recents.events.activity.FsGestureSlideOutEvent;
import com.android.systemui.recents.events.activity.HideRecentsEvent;
import com.android.systemui.recents.events.activity.MultiWindowStateChangedEvent;
import com.android.systemui.recents.events.activity.NavStubViewAttachToWindowEvent;
import com.android.systemui.recents.events.component.RecentsVisibilityChangedEvent;
import com.android.systemui.recents.model.MutableBoolean;
import com.android.systemui.recents.views.RecentsView;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import miui.util.ScreenshotUtils;

public class NavStubView extends FrameLayout {
    /* access modifiers changed from: private */
    public static final boolean DEBUG = Log.isLoggable(TAG, 3);
    public static final int DEFAULT_ANIM_TIME = (IS_E10 ? 144 : 300);
    public static final boolean IS_E10 = "beryllium".equals(Build.PRODUCT);
    public static final String TAG = NavStubView.class.getSimpleName();
    public final int RADIUS_SIZE;
    /* access modifiers changed from: private */
    public AntiMistakeTouchView antiMistakeTouchView;
    ExecutorService jobExecutor;
    private ActivityManager mAm;
    /* access modifiers changed from: private */
    public ValueAnimator mAppEnterRecentsAnim;
    private Bitmap mAppIcon;
    private boolean mBitmapShown;
    /* access modifiers changed from: private */
    public boolean mCancelActionToStartApp;
    private Intent mCloseScreenshotIntent;
    private Interpolator mCubicEaseOutInterpolator;
    /* access modifiers changed from: private */
    public float mCurAlpha = 1.0f;
    /* access modifiers changed from: private */
    public float mCurScale;
    /* access modifiers changed from: private */
    public ActivityManager.RecentTaskInfo mCurTask;
    /* access modifiers changed from: private */
    public int mCurrAction = -1;
    /* access modifiers changed from: private */
    public float mCurrX;
    /* access modifiers changed from: private */
    public float mCurrY;
    /* access modifiers changed from: private */
    public float mCurrentY;
    private float mDelta;
    private Rect mDest = new Rect();
    /* access modifiers changed from: private */
    public RectF mDestRectF;
    /* access modifiers changed from: private */
    public float mDestRectHeightScale = 1.0f;
    /* access modifiers changed from: private */
    public float mDestTopOffset;
    private boolean mDisableTouch;
    private final int mDividerSize;
    /* access modifiers changed from: private */
    public MotionEvent mDownEvent;
    private int mDownNo = 0;
    private long mDownTime;
    private float mDownX;
    /* access modifiers changed from: private */
    public float mFollowTailX;
    /* access modifiers changed from: private */
    public float mFollowTailY;
    /* access modifiers changed from: private */
    public Handler mFrameHandler = new Handler();
    private BroadcastReceiver mFullScreenModeChangeReceiver;
    /* access modifiers changed from: private */
    public GestureStubListenerWrapper mGestureStubListenerWrapper;
    /* access modifiers changed from: private */
    public H mHandler;
    /* access modifiers changed from: private */
    public IFsGestureCallback mHomeCallback;
    /* access modifiers changed from: private */
    public ValueAnimator mHomeFadeInAnim;
    /* access modifiers changed from: private */
    public ValueAnimator mHomeFadeOutAnim;
    /* access modifiers changed from: private */
    public final Intent mHomeIntent;
    /* access modifiers changed from: private */
    public IActivityManager mIam;
    private float mInitX;
    private float mInitY;
    /* access modifiers changed from: private */
    public boolean mIsAlreadyCropStatusBar;
    private boolean mIsAppToHome;
    private boolean mIsAppToRecents;
    /* access modifiers changed from: private */
    public boolean mIsBgIconVisible;
    /* access modifiers changed from: private */
    public boolean mIsEnterRecents = false;
    /* access modifiers changed from: private */
    public boolean mIsFullScreenMode;
    /* access modifiers changed from: private */
    public boolean mIsFullScreenModeCurTime;
    /* access modifiers changed from: private */
    public boolean mIsGestureStarted;
    private boolean mIsInFsMode;
    /* access modifiers changed from: private */
    public boolean mIsMultiWindow;
    private boolean mKeepHidden;
    private Configuration mLastConfiguration = new Configuration();
    private int mLastDownNo = 0;
    private long mLastTouchTime;
    private int[] mLocation = new int[2];
    private Xfermode mModeOverlay = new PorterDuffXfermode(PorterDuff.Mode.OVERLAY);
    private Xfermode mModeSrcIn = new PorterDuffXfermode(PorterDuff.Mode.SRC_IN);
    /* access modifiers changed from: private */
    public int mMultiDelta;
    private boolean mNeedRender;
    boolean mOrientationChangedAfterDown = false;
    private Paint mPaint;
    /* access modifiers changed from: private */
    public boolean mPendingResetStatus;
    /* access modifiers changed from: private */
    public int mPivotLocX;
    /* access modifiers changed from: private */
    public int mPivotLocY;
    private QuartEaseOutInterpolator mQuartEaseOutInterpolator;
    private boolean mRecentVisible;
    private View mRecentsBg;
    private ViewGroup mRecentsContainer;
    /* access modifiers changed from: private */
    public RecentsView mRecentsView;
    private Bitmap mScreenBitmap;
    /* access modifiers changed from: private */
    public int mScreenBmpHeight;
    /* access modifiers changed from: private */
    public float mScreenBmpScale;
    /* access modifiers changed from: private */
    public int mScreenBmpWidth;
    /* access modifiers changed from: private */
    public int mScreenHeight;
    /* access modifiers changed from: private */
    public int mScreenWidth;
    private Rect mShowRect = new Rect();
    private Rect mSrc = new Rect();
    /* access modifiers changed from: private */
    public int mStateMode;
    private StatusBar mStatusBar;
    /* access modifiers changed from: private */
    public int mStatusBarDec;
    /* access modifiers changed from: private */
    public int mStatusBarHeight;
    private final StatusBarManager mStatusBarManager;
    private boolean mSupportAntiMistake;
    private Runnable mTailCatcherTask = new Runnable() {
        public void run() {
            NavStubView.access$016(NavStubView.this, (((float) NavStubView.this.mPivotLocX) - NavStubView.this.mFollowTailX) / 4.0f);
            NavStubView.access$216(NavStubView.this, (((float) NavStubView.this.mPivotLocY) - NavStubView.this.mFollowTailY) / 4.0f);
            float xDelta = Math.abs(((float) NavStubView.this.mPivotLocX) - NavStubView.this.mFollowTailX);
            float yDelta = Math.abs(((float) NavStubView.this.mPivotLocY) - NavStubView.this.mFollowTailY);
            double distance = Math.sqrt((double) ((xDelta * xDelta) + (yDelta * yDelta)));
            if (NavStubView.this.mWindowMode == 3 || NavStubView.this.mWindowMode == 4) {
                NavStubView.this.mFrameHandler.postDelayed(this, 16);
                return;
            }
            if (NavStubView.this.mStateMode == 65538) {
                if (NavStubView.this.mCurrentY < ((float) (NavStubView.this.mScreenHeight - 160)) && distance < 80.0d && !NavStubView.this.mIsEnterRecents) {
                    boolean unused = NavStubView.this.mIsEnterRecents = true;
                    if (NavStubView.this.mWindowMode == 1) {
                        NavStubView.this.updateRecentsBlurRatio(0.0f);
                    }
                    if (NavStubView.this.mWindowMode == 2) {
                        NavStubView.this.mAppEnterRecentsAnim.start();
                    }
                    RecentsEventBus.getDefault().send(new FsGestureEnterRecentsEvent());
                }
                if (NavStubView.this.mCurrentY < ((float) (NavStubView.this.mScreenHeight - 320)) && distance < 40.0d) {
                    int unused2 = NavStubView.this.mStateMode = 65539;
                    Log.d(NavStubView.TAG, "current state mode: StateMode.STATE_TASK_HOLD");
                    NavStubView.this.mFrameHandler.postDelayed(new Runnable() {
                        public void run() {
                            NavStubView.this.performHapticFeedback(1);
                        }
                    }, 100);
                    RecentsEventBus.getDefault().send(new FsGestureSlideInEvent());
                    if (NavStubView.this.mWindowMode == 1) {
                        NavStubView.this.mHomeFadeOutAnim.setFloatValues(new float[]{NavStubView.this.mCurScale, 0.8f});
                        NavStubView.this.mHomeFadeOutAnim.start();
                    }
                }
            } else if (NavStubView.this.mStateMode == 65539 && NavStubView.this.mCurrentY > ((float) (NavStubView.this.mScreenHeight - 240))) {
                int unused3 = NavStubView.this.mStateMode = 65538;
                Log.d(NavStubView.TAG, "current state mode: StateMode.STATE_ON_DRAG");
                RecentsEventBus.getDefault().send(new FsGestureSlideOutEvent());
                if (NavStubView.this.mWindowMode == 1) {
                    NavStubView.this.mHomeFadeInAnim.start();
                }
            }
            NavStubView.this.mFrameHandler.postDelayed(this, 16);
        }
    };
    private int mTouchSlop;
    private WindowManager mWindowManager;
    /* access modifiers changed from: private */
    public int mWindowMode;
    /* access modifiers changed from: private */
    public int mWindowSize;
    /* access modifiers changed from: private */
    public float mXScale;
    /* access modifiers changed from: private */
    public float mYScale;
    public int targetBgAlpha = 136;

    private static class BerylliumConfig {
        protected static final Interpolator FAST_OUT_SLOW_IN = new PathInterpolator(0.4f, 0.0f, 0.2f, 1.0f);
    }

    private static class CubicEaseOutInterpolator implements Interpolator {
        private CubicEaseOutInterpolator() {
        }

        public float getInterpolation(float t) {
            float f = t - 1.0f;
            float t2 = f;
            return (f * t2 * t2) + 1.0f;
        }
    }

    private class GestureStubListenerWrapper {
        private Method getGestureStubListenerMethod;
        IGestureStubListener mListener = getGestureStubListener();

        public GestureStubListenerWrapper() {
        }

        public void onGestureReady() {
            try {
                if (this.mListener != null) {
                    this.mListener.onGestureReady();
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        public void onGestureStart() {
            try {
                if (this.mListener != null) {
                    this.mListener.onGestureStart();
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        public void onGestureFinish(boolean immediate) {
            try {
                if (this.mListener != null) {
                    this.mListener.onGestureFinish(immediate);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        public void skipAppTransition() {
            try {
                if (this.mListener != null) {
                    this.mListener.skipAppTransition();
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        private IGestureStubListener getGestureStubListener() {
            try {
                if (this.getGestureStubListenerMethod == null) {
                    this.getGestureStubListenerMethod = WindowManagerGlobal.getWindowManagerService().getClass().getMethod("getGestureStubListener", new Class[0]);
                }
                if (this.getGestureStubListenerMethod != null) {
                    return (IGestureStubListener) this.getGestureStubListenerMethod.invoke(WindowManagerGlobal.getWindowManagerService(), new Object[0]);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private class H extends Handler {
        private H() {
        }

        public void handleMessage(Message msg) {
            MotionEvent downEvent = NavStubView.this.mDownEvent;
            switch (msg.what) {
                case 255:
                    if (!NavStubView.this.mIsGestureStarted) {
                        if (NavStubView.DEBUG) {
                            Log.d(NavStubView.TAG, "handleMessage MSG_SET_GESTURE_STUB_UNTOUCHABLE");
                        }
                        NavStubView.this.disableTouch(true);
                        sendMessageDelayed(obtainMessage(260), 20);
                        boolean unused = NavStubView.this.mPendingResetStatus = true;
                        sendMessageDelayed(obtainMessage(257), 500);
                        break;
                    } else {
                        return;
                    }
                case 256:
                    if (downEvent != null && !NavStubView.this.mIsGestureStarted) {
                        float diffX = NavStubView.this.mCurrX - downEvent.getRawX();
                        float diffY = NavStubView.this.mCurrY - downEvent.getRawY();
                        if (NavStubView.DEBUG) {
                            String str = NavStubView.TAG;
                            Log.d(str, "handleMessage MSG_CHECK_GESTURE_STUB_TOUCHABLE diffX: " + diffX + " diffY: " + diffY + " mDownX: " + downEvent.getRawX() + " mDownY: " + downEvent.getRawY());
                        }
                        if (Math.abs(diffX) <= 30.0f && Math.abs(diffY) <= 30.0f) {
                            NavStubView.this.mHandler.removeMessages(255);
                            NavStubView.this.mHandler.sendMessage(NavStubView.this.mHandler.obtainMessage(255));
                            break;
                        }
                    } else {
                        return;
                    }
                    break;
                case 257:
                    boolean unused2 = NavStubView.this.mPendingResetStatus = false;
                    NavStubView.this.disableTouch(false);
                    if (NavStubView.DEBUG) {
                        Log.d(NavStubView.TAG, "handleMessage MSG_RESET_GESTURE_STUB_TOUCHABLE");
                        break;
                    }
                    break;
                case 258:
                    MotionEvent event = (MotionEvent) msg.obj;
                    NavStubView.this.onPointerEvent(event);
                    event.recycle();
                    break;
                case 260:
                    if (NavStubView.this.mCurrAction == 2 || NavStubView.this.mCurrAction == 0) {
                        NavStubView.this.injectMotionEvent(0);
                    } else {
                        NavStubView.this.injectMotionEvent(0);
                        NavStubView.this.injectMotionEvent(1);
                    }
                    if (NavStubView.this.mDownEvent != null) {
                        NavStubView.this.mDownEvent.recycle();
                        MotionEvent unused3 = NavStubView.this.mDownEvent = null;
                        break;
                    }
                    break;
            }
        }
    }

    private static class QuartEaseOutInterpolator implements Interpolator {
        private QuartEaseOutInterpolator() {
        }

        public float getInterpolation(float t) {
            float f = t - 1.0f;
            float t2 = f;
            return -((((f * t2) * t2) * t2) - 1.0f);
        }
    }

    static /* synthetic */ float access$016(NavStubView x0, float x1) {
        float f = x0.mFollowTailX + x1;
        x0.mFollowTailX = f;
        return f;
    }

    static /* synthetic */ float access$216(NavStubView x0, float x1) {
        float f = x0.mFollowTailY + x1;
        x0.mFollowTailY = f;
        return f;
    }

    public NavStubView(Context context) {
        super(context);
        boolean z = true;
        this.jobExecutor = Executors.newFixedThreadPool(1);
        this.mFullScreenModeChangeReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if ("com.android.systemui.fullscreen.statechange".equals(intent.getAction())) {
                    int i = 0;
                    boolean unused = NavStubView.this.mIsFullScreenMode = intent.getBooleanExtra("isEnter", false);
                    if (NavStubView.this.antiMistakeTouchView != null) {
                        AntiMistakeTouchView access$2700 = NavStubView.this.antiMistakeTouchView;
                        if (!NavStubView.this.isMistakeTouch()) {
                            i = 8;
                        }
                        access$2700.updateVisibilityState(i);
                    }
                }
            }
        };
        this.mCubicEaseOutInterpolator = IS_E10 ? BerylliumConfig.FAST_OUT_SLOW_IN : new CubicEaseOutInterpolator();
        this.mQuartEaseOutInterpolator = new QuartEaseOutInterpolator();
        this.RADIUS_SIZE = context.getResources().getDimensionPixelSize(R.dimen.recents_task_view_rounded_corners_radius) * 2;
        this.mStateMode = 65537;
        this.mWindowManager = (WindowManager) this.mContext.getSystemService("window");
        this.mStatusBarManager = (StatusBarManager) this.mContext.getSystemService("statusbar");
        DisplayMetrics dm = new DisplayMetrics();
        this.mWindowManager.getDefaultDisplay().getRealMetrics(dm);
        this.mScreenWidth = dm.widthPixels;
        this.mScreenHeight = dm.heightPixels;
        this.mPaint = new Paint();
        this.mPaint.setAntiAlias(true);
        this.mWindowSize = "lithium".equals(Build.DEVICE) ? 14 : 13;
        this.mHomeIntent = new Intent("android.intent.action.MAIN", null);
        this.mHomeIntent.addCategory("android.intent.category.HOME");
        this.mHomeIntent.addFlags(270532608);
        this.mIam = ActivityManagerNative.getDefault();
        this.mAm = (ActivityManager) context.getSystemService("activity");
        this.mDividerSize = getResources().getDimensionPixelSize(R.dimen.docked_stack_divider_thickness) - (2 * getResources().getDimensionPixelSize(R.dimen.docked_stack_divider_insets));
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            this.mStatusBarHeight = getResources().getDimensionPixelSize(resourceId);
        }
        this.mCloseScreenshotIntent = new Intent("android.intent.action.CLOSE_SYSTEM_DIALOGS");
        this.mCloseScreenshotIntent.putExtra("reason", "fs_gesture");
        this.mGestureStubListenerWrapper = new GestureStubListenerWrapper();
        this.mHandler = new H();
        this.mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        initValueAnimator();
        if (!(this.mGestureStubListenerWrapper == null || this.mGestureStubListenerWrapper.mListener == null)) {
            z = false;
        }
        this.mSupportAntiMistake = z;
        if (this.mSupportAntiMistake) {
            this.antiMistakeTouchView = new AntiMistakeTouchView(context);
            addView(this.antiMistakeTouchView, this.antiMistakeTouchView.getFrameLayoutParams());
        }
        this.mRecentVisible = false;
    }

    private void initValueAnimator() {
        this.mHomeFadeOutAnim = new ValueAnimator();
        this.mHomeFadeOutAnim.setInterpolator(Interpolators.CUBIC_EASE_OUT);
        this.mHomeFadeOutAnim.setDuration(200);
        this.mHomeFadeOutAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                float scale = ((Float) animation.getAnimatedValue()).floatValue();
                Recents.getSystemServices().changeAlphaScaleForFsGesture(Constants.HOME_LAUCNHER_PACKAGE_NAME, 1.0f - animation.getAnimatedFraction(), scale);
                NavStubView.this.updateRecentsBlurRatio(1.0f * animation.getAnimatedFraction());
            }
        });
        this.mHomeFadeInAnim = new ValueAnimator();
        this.mHomeFadeInAnim.setFloatValues(new float[]{0.0f, 1.0f});
        this.mHomeFadeInAnim.setInterpolator(Interpolators.CUBIC_EASE_OUT);
        this.mHomeFadeInAnim.setDuration(150);
        this.mHomeFadeInAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                float fraction = animation.getAnimatedFraction();
                float scale = 0.8f + ((NavStubView.this.mCurScale - 0.8f) * fraction);
                Recents.getSystemServices().changeAlphaScaleForFsGesture(Constants.HOME_LAUCNHER_PACKAGE_NAME, fraction, scale);
                NavStubView.this.updateRecentsBlurRatio((1.0f - fraction) * 1.0f);
            }
        });
        this.mAppEnterRecentsAnim = new ValueAnimator();
        this.mAppEnterRecentsAnim.setInterpolator(new AccelerateInterpolator());
        this.mAppEnterRecentsAnim.setDuration(200);
        this.mAppEnterRecentsAnim.setFloatValues(new float[]{0.0f, 1.0f});
        this.mAppEnterRecentsAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                NavStubView.this.updateRecentsBlurRatio(1.0f * ((Float) animation.getAnimatedValue()).floatValue());
            }
        });
    }

    public void startAppEnterRecentsAnim() {
        this.mAppEnterRecentsAnim.start();
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration newConfig) {
        int changes = this.mLastConfiguration.updateFrom(newConfig);
        boolean densitySizeChange = true;
        int i = 0;
        this.mOrientationChangedAfterDown = this.mOrientationChangedAfterDown || ((changes & 128) != 0);
        boolean screenSizeChange = (changes & 1024) != 0;
        boolean smallestScreenSizeChange = (changes & 2048) != 0;
        if ((changes & 4096) == 0) {
            densitySizeChange = false;
        }
        if (densitySizeChange && smallestScreenSizeChange && screenSizeChange) {
            updateViewLayout((int) (((float) this.mWindowSize) * this.mContext.getResources().getDisplayMetrics().density));
        }
        if (this.mSupportAntiMistake != 0 && this.antiMistakeTouchView != null) {
            AntiMistakeTouchView antiMistakeTouchView2 = this.antiMistakeTouchView;
            if (!isMistakeTouch()) {
                i = 8;
            }
            antiMistakeTouchView2.updateVisibilityState(i);
        }
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        RecentsEventBus.getDefault().register(this);
        RecentsEventBus.getDefault().send(new NavStubViewAttachToWindowEvent());
        IntentFilter fsgFilter = new IntentFilter();
        fsgFilter.addAction("com.android.systemui.fullscreen.statechange");
        this.mContext.registerReceiverAsUser(this.mFullScreenModeChangeReceiver, UserHandle.ALL, fsgFilter, "miui.permission.USE_INTERNAL_GENERAL_API", null);
    }

    public final void onBusEvent(FsGestureLaunchTargetTaskViewRectEvent event) {
        this.mDestRectF = event.mRectF;
        if (this.mDestRectF != null) {
            this.mDestRectHeightScale = (((float) this.mScreenBmpWidth) * this.mDestRectF.height()) / (this.mDestRectF.width() * ((float) this.mScreenBmpHeight));
        }
    }

    public final void onBusEvent(MultiWindowStateChangedEvent event) {
        this.mIsMultiWindow = event.inMultiWindow;
    }

    public final void onBusEvent(FsGestureRecentsViewWrapperEvent event) {
        this.mRecentsView = event.mRecentsView;
        this.mRecentsBg = event.mBackGround;
        this.mRecentsContainer = event.mRecentsContainer;
    }

    public final void onBusEvent(RecentsVisibilityChangedEvent event) {
        this.mRecentVisible = event.visible;
        int i = 0;
        if (event.visible && this.mCancelActionToStartApp) {
            RecentsEventBus.getDefault().post(new HideRecentsEvent(false, false, true));
        }
        if (this.mSupportAntiMistake && this.antiMistakeTouchView != null && this.mIsFullScreenMode && getResources().getConfiguration().orientation == 2) {
            if (this.mRecentVisible) {
                this.antiMistakeTouchView.updateVisibilityState(8);
                return;
            }
            AntiMistakeTouchView antiMistakeTouchView2 = this.antiMistakeTouchView;
            if (!isMistakeTouch()) {
                i = 8;
            }
            antiMistakeTouchView2.updateVisibilityState(i);
        }
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        RecentsEventBus.getDefault().unregister(this);
        this.mContext.unregisterReceiver(this.mFullScreenModeChangeReceiver);
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        int bmpLeft;
        int bmpTop;
        Canvas canvas2 = canvas;
        super.onDraw(canvas);
        if (this.mScreenBitmap != null) {
            if (this.mIsAppToHome) {
                bmpLeft = this.mPivotLocX - (this.mScreenBmpWidth / 2);
                bmpTop = this.mPivotLocY - (this.mScreenBmpHeight / 2);
            } else {
                bmpLeft = this.mPivotLocX - (this.mScreenBmpWidth / 2);
                bmpTop = this.mPivotLocY - this.mScreenBmpHeight;
            }
            int bmpLeft2 = bmpLeft;
            int bmpTop2 = bmpTop;
            this.mShowRect.left = bmpLeft2;
            this.mShowRect.top = bmpTop2;
            this.mShowRect.right = this.mScreenBmpWidth + bmpLeft2;
            this.mShowRect.bottom = this.mScreenBmpHeight + bmpTop2;
            if (!this.mIsAppToHome) {
                canvas.save();
                canvas2.translate((float) this.mPivotLocX, (float) this.mPivotLocY);
                canvas2.scale(this.mCurScale, this.mCurScale);
                canvas2.translate((float) (-this.mPivotLocX), (float) (-this.mPivotLocY));
                if (this.mDestRectF == null || !this.mIsAppToRecents) {
                    int bottom = (int) (((float) this.mShowRect.top) + (((float) this.mScreenBmpHeight) * this.mCurScale));
                    this.mPaint.setAlpha(255);
                    this.mPaint.setXfermode(null);
                    this.mPaint.setStyle(Paint.Style.FILL);
                    int saveLayer = canvas2.saveLayer((float) this.mShowRect.left, (float) this.mShowRect.top, (float) this.mShowRect.right, (float) bottom, null);
                    canvas2.drawRoundRect((float) this.mShowRect.left, (float) this.mShowRect.top, (float) this.mShowRect.right, (float) bottom, (float) this.RADIUS_SIZE, (float) this.RADIUS_SIZE, this.mPaint);
                    this.mPaint.setAlpha((int) (this.mCurAlpha * 255.0f));
                    this.mPaint.setXfermode(this.mModeSrcIn);
                    canvas2.drawBitmap(this.mScreenBitmap, null, this.mShowRect, this.mPaint);
                    this.mPaint.setXfermode(null);
                    canvas2.restoreToCount(saveLayer);
                } else {
                    this.mSrc.left = 0;
                    this.mSrc.top = this.mStatusBarDec;
                    this.mSrc.right = this.mScreenBmpWidth;
                    this.mSrc.bottom = (int) ((this.mScreenBmpScale * ((float) this.mScreenBmpHeight)) + ((float) this.mStatusBarDec));
                    this.mDest.set(this.mSrc);
                    this.mDest.offset(this.mShowRect.left, (int) ((((float) this.mPivotLocY) + ((this.mDestTopOffset - 1.0f) * ((float) this.mScreenBmpHeight))) - ((float) this.mStatusBarDec)));
                    this.mPaint.setAlpha(255);
                    this.mPaint.setXfermode(null);
                    this.mPaint.setStyle(Paint.Style.FILL);
                    int saveLayer2 = canvas2.saveLayer((float) this.mShowRect.left, (float) this.mShowRect.top, (float) this.mShowRect.right, (float) this.mShowRect.bottom, null);
                    canvas2.drawRoundRect((float) this.mDest.left, (float) this.mDest.top, (float) this.mDest.right, (float) this.mDest.bottom, (float) this.RADIUS_SIZE, (float) this.RADIUS_SIZE, this.mPaint);
                    this.mPaint.setXfermode(this.mModeSrcIn);
                    canvas2.drawBitmap(this.mScreenBitmap, this.mSrc, this.mDest, this.mPaint);
                    this.mPaint.setXfermode(null);
                    canvas2.restoreToCount(saveLayer2);
                }
                canvas.restore();
            } else {
                canvas.save();
                canvas2.translate((float) this.mPivotLocX, (float) this.mPivotLocY);
                canvas2.scale(this.mXScale, this.mYScale);
                canvas2.translate((float) (-this.mPivotLocX), (float) (-this.mPivotLocY));
                this.mShowRect.bottom = (int) (((float) this.mShowRect.top) + (((float) this.mScreenBmpHeight) * this.mCurScale));
                this.mPaint.setAlpha((int) (255.0f * this.mCurAlpha));
                canvas2.drawBitmap(this.mScreenBitmap, null, this.mShowRect, this.mPaint);
                canvas.restore();
            }
            if (!this.mBitmapShown) {
                this.mBitmapShown = true;
                this.mGestureStubListenerWrapper.onGestureStart();
            }
        }
    }

    /* access modifiers changed from: private */
    public void disableTouch(boolean disableTouch) {
        Log.d(TAG, "distouch : " + disableTouch);
        this.mDisableTouch = disableTouch;
        WindowManager.LayoutParams windowLayoutParameters = (WindowManager.LayoutParams) getLayoutParams();
        if (disableTouch) {
            windowLayoutParameters.flags |= 16;
        } else {
            windowLayoutParameters.flags &= -17;
        }
        this.mWindowManager.updateViewLayout(this, windowLayoutParameters);
    }

    public void setVisibility(int visibility) {
        this.mKeepHidden = visibility != 0;
        if (this.mKeepHidden) {
            super.setVisibility(8);
            if (this.mDownEvent != null) {
                this.mDownEvent.recycle();
                this.mDownEvent = null;
                return;
            }
            return;
        }
        super.setVisibility(0);
    }

    /* access modifiers changed from: private */
    public void injectMotionEvent(int action) {
        int i;
        MotionEvent downEvent = this.mDownEvent;
        if (downEvent != null) {
            if (DEBUG) {
                String str = TAG;
                StringBuilder sb = new StringBuilder();
                sb.append("injectMotionEvent action :");
                i = action;
                sb.append(i);
                sb.append(" downX: ");
                sb.append(downEvent.getRawX());
                sb.append(" downY: ");
                sb.append(downEvent.getRawY());
                Log.d(str, sb.toString());
            } else {
                i = action;
            }
            MotionEvent.PointerProperties[] arrayOfPointerProperties = MotionEvent.PointerProperties.createArray(1);
            downEvent.getPointerProperties(0, arrayOfPointerProperties[0]);
            MotionEvent.PointerCoords[] arrayOfPointerCoords = MotionEvent.PointerCoords.createArray(1);
            downEvent.getPointerCoords(0, arrayOfPointerCoords[0]);
            arrayOfPointerCoords[0].x = downEvent.getRawX();
            arrayOfPointerCoords[0].y = downEvent.getRawY();
            InputManager.getInstance().injectInputEvent(MotionEvent.obtain(downEvent.getDownTime(), downEvent.getEventTime(), i, 1, arrayOfPointerProperties, arrayOfPointerCoords, downEvent.getMetaState(), downEvent.getButtonState(), downEvent.getXPrecision(), downEvent.getYPrecision(), downEvent.getDeviceId(), downEvent.getEdgeFlags(), downEvent.getSource(), downEvent.getFlags()), 0);
        }
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (DEBUG) {
            String str = TAG;
            Log.d(str, "onTouchEvent:" + event.getRawX() + " " + event.getRawY() + " " + event);
        }
        if (this.mDisableTouch) {
            return false;
        }
        this.mCurrAction = event.getAction();
        switch (this.mCurrAction) {
            case 0:
                this.mInitX = event.getRawX();
                this.mInitY = event.getRawY();
                this.mCurrX = event.getRawX();
                this.mCurrY = event.getRawY();
                if (this.mDownEvent != null) {
                    this.mDownEvent.recycle();
                    this.mDownEvent = null;
                }
                this.mDownEvent = event.copy();
                this.mHandler.removeMessages(256);
                this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(256), 300);
                if (DEBUG) {
                    Log.d(TAG, "onTouch ACTION_DOWN sendMessageDelayed MSG_CHECK_GESTURE_STUB_TOUCHABLE");
                    break;
                }
                break;
            case 1:
            case 3:
                MotionEvent downEvent = this.mDownEvent;
                if (downEvent != null) {
                    this.mCurrX = event.getRawX();
                    this.mCurrY = event.getRawY();
                    if (event.getEventTime() - downEvent.getEventTime() < 300 && !this.mIsGestureStarted) {
                        clearMessages();
                        float diffX = this.mCurrX - downEvent.getRawX();
                        float diffY = this.mCurrY - downEvent.getRawY();
                        if (!this.mIsGestureStarted && Math.abs(diffX) <= 30.0f && Math.abs(diffY) <= 30.0f) {
                            this.mHandler.sendMessage(this.mHandler.obtainMessage(255));
                            if (DEBUG) {
                                String str2 = TAG;
                                Log.d(str2, "currTime - mDownTime < MSG_CHECK_GESTURE_STUB_TOUCHABLE_TIMEOUT updateViewLayout UnTouchable, diffX:" + diffX + " diffY:" + diffY);
                            }
                        }
                    }
                    if (DEBUG) {
                        String str3 = TAG;
                        Log.d(str3, "ACTION_UP: mIsGestureStarted: " + this.mIsGestureStarted);
                    }
                    this.mIsGestureStarted = false;
                    break;
                } else {
                    return true;
                }
            case 2:
                this.mCurrX = event.getRawX();
                this.mCurrY = event.getRawY();
                if (2.0f * Math.abs(this.mCurrY - this.mInitY) >= Math.abs(this.mCurrX - this.mInitX) || Math.abs(this.mCurrX - this.mInitX) <= ((float) this.mTouchSlop) || this.mIsGestureStarted || this.mPendingResetStatus) {
                    if (!this.mPendingResetStatus && this.mInitY - this.mCurrY > ((float) this.mTouchSlop) && !this.mIsGestureStarted) {
                        this.mIsGestureStarted = true;
                        exitFreeFormWindowIfNeeded();
                        clearMessages();
                        if (this.mDownEvent != null && getResources().getConfiguration().orientation == 1) {
                            this.mHandler.sendMessage(this.mHandler.obtainMessage(258, this.mDownEvent.copy()));
                            break;
                        } else {
                            event.setAction(0);
                            break;
                        }
                    }
                } else {
                    this.mHandler.sendMessage(this.mHandler.obtainMessage(255));
                    if (DEBUG) {
                        Log.d(TAG, "h-slide detected, sendMessage MSG_SET_GESTURE_STUB_UNTOUCHABLE");
                        break;
                    }
                }
                break;
        }
        if (this.mPendingResetStatus || (!this.mIsGestureStarted && this.mCurrAction != 1 && this.mCurrAction != 3)) {
            return false;
        }
        this.mHandler.sendMessage(this.mHandler.obtainMessage(258, event.copy()));
        return true;
    }

    private void clearMessages() {
        this.mHandler.removeMessages(256);
        this.mHandler.removeMessages(255);
        this.mHandler.removeMessages(260);
    }

    public boolean onPointerEvent(MotionEvent event) {
        if (DEBUG) {
            Log.d(TAG, "onPointEvent:" + event.getRawX() + " " + event.getRawY() + " " + event);
        }
        if (this.mIsInFsMode) {
            return false;
        }
        if (event.getAction() == 0 && isMistakeTouch()) {
            if (!this.mSupportAntiMistake || (this.antiMistakeTouchView != null && this.antiMistakeTouchView.containsLocation(event.getRawX()))) {
                if (SystemClock.uptimeMillis() - this.mLastTouchTime > 2000) {
                    Toast toast = Toast.makeText(this.mContext, getResources().getString(R.string.please_slide_agian), 0);
                    toast.getWindowParams().privateFlags |= 16;
                    toast.show();
                    if (this.antiMistakeTouchView != null) {
                        this.antiMistakeTouchView.slideUp();
                    }
                    this.mLastTouchTime = SystemClock.uptimeMillis();
                    return false;
                }
            } else if (this.mSupportAntiMistake && this.antiMistakeTouchView != null) {
                this.antiMistakeTouchView.slideUp();
            }
        }
        if (event.getAction() == 0) {
            this.mDownNo++;
        }
        if (this.mDownNo == this.mLastDownNo) {
            return false;
        }
        if (1 == event.getAction()) {
            this.mLastDownNo = this.mDownNo;
        }
        if (event.getAction() == 0) {
            this.mIsFullScreenModeCurTime = this.mIsFullScreenMode;
            this.mDownTime = SystemClock.uptimeMillis();
            this.mOrientationChangedAfterDown = false;
            MutableBoolean isHomeStackVisible = new MutableBoolean(false);
            boolean recentsActivityVisible = Recents.getSystemServices().isRecentsActivityVisible(isHomeStackVisible);
            if (this.mStatusBar == null) {
                this.mStatusBar = (StatusBar) ((Application) this.mContext.getApplicationContext()).getSystemUIApplication().getComponent(StatusBar.class);
            }
            if (this.mStatusBar != null && this.mStatusBar.isKeyguardShowing()) {
                this.mWindowMode = 4;
            } else if (recentsActivityVisible) {
                this.mWindowMode = 3;
            } else if (isHomeStackVisible.value) {
                this.mWindowMode = 1;
            } else {
                this.mWindowMode = 2;
            }
            if (this.mStatusBar == null) {
                this.mStatusBarManager.collapsePanels();
            } else if (this.mStatusBar.mExpandedVisible) {
                this.mStatusBar.animateCollapsePanels(0);
            }
        }
        Log.d(TAG, "current window mode:" + this.mWindowMode + " (1:home, 2:app, 3:recent-task, 4:keyguard)");
        if (event.getAction() == 0) {
            int max = Math.max(this.mScreenHeight, this.mScreenWidth);
            int min = Math.min(this.mScreenHeight, this.mScreenWidth);
            if (getResources().getConfiguration().orientation == 2) {
                this.mScreenHeight = min;
                this.mScreenWidth = max;
            } else {
                this.mScreenHeight = max;
                this.mScreenWidth = min;
            }
        }
        if (1 == event.getAction()) {
            this.mIsInFsMode = true;
        }
        switch (this.mWindowMode) {
            case 1:
                homeTouchResolution(event);
                break;
            case 2:
                if (this.mGestureStubListenerWrapper != null && this.mGestureStubListenerWrapper.mListener != null) {
                    appTouchResolution(event);
                    break;
                } else {
                    appTouchResolutionForVersionTwo(event);
                    break;
                }
            case 3:
            case 4:
                simpleTouchResolution(event);
                break;
            default:
                this.mIsInFsMode = false;
                break;
        }
        return false;
    }

    private void appTouchResolutionForVersionTwo(MotionEvent event) {
        if (event.getAction() == 0) {
            this.mStateMode = 65537;
        }
        if (event.getAction() == 2 && this.mStateMode == 65537) {
            this.mStateMode = 65538;
            RecentsEventBus.getDefault().post(new FsGesturePreloadRecentsEvent());
        }
        this.mIsInFsMode = false;
    }

    /* access modifiers changed from: private */
    public boolean isMistakeTouch() {
        if (Settings.Global.getInt(this.mContext.getContentResolver(), "show_mistake_touch_toast", 1) == 0 || this.mRecentVisible) {
            return false;
        }
        if (Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "gb_notification", 0, -2) == 1) {
            return true;
        }
        return getResources().getConfiguration().orientation == 2 && this.mIsFullScreenMode;
    }

    private void appTouchResolution(MotionEvent event) {
        Bitmap cropBmp;
        this.mCurrentY = event.getRawY();
        switch (event.getAction()) {
            case 0:
                Log.d(TAG, "======>>>>down: " + SystemClock.uptimeMillis());
                initValue(event);
                try {
                    this.mCurTask = ActivityManagerCompat.getRecentTasksForUser(this.mAm, 1, 127, -2).get(0);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to get recent tasks", e);
                }
                this.jobExecutor.execute(new Runnable() {
                    public void run() {
                        NavStubView.this.mGestureStubListenerWrapper.onGestureReady();
                    }
                });
                try {
                    this.mScreenBitmap = ScreenshotUtils.getScreenshot(this.mContext, 1.0f, 0, MiuiWindowManager.getLayer(this.mContext, 2001) - 1, true);
                    if (this.mScreenBitmap != null) {
                        Bitmap original = this.mScreenBitmap;
                        this.mScreenBitmap = original.copy(Bitmap.Config.ARGB_8888, false);
                        original.recycle();
                    }
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
                if (this.mScreenBitmap == null) {
                    this.mScreenBitmap = Bitmap.createBitmap(this.mScreenWidth, this.mScreenHeight, Bitmap.Config.ARGB_8888);
                    this.mScreenBitmap.eraseColor(Color.parseColor("#00000000"));
                }
                if (this.mIsMultiWindow) {
                    try {
                        ActivityManager.StackInfo stackInfo = ActivityManagerCompat.getStackInfo(3, 3, 0);
                        if (this.mScreenHeight > this.mScreenWidth) {
                            cropBmp = Bitmap.createBitmap(this.mScreenBitmap, 0, stackInfo.bounds.bottom + this.mDividerSize, this.mScreenBitmap.getWidth(), (this.mScreenHeight - stackInfo.bounds.bottom) - this.mDividerSize);
                        } else {
                            cropBmp = Bitmap.createBitmap(this.mScreenBitmap, stackInfo.bounds.right + this.mDividerSize, 0, (this.mScreenWidth - stackInfo.bounds.right) - this.mDividerSize, this.mScreenHeight);
                        }
                        if (this.mScreenHeight < this.mScreenWidth) {
                            this.mMultiDelta = (stackInfo.bounds.right + this.mDividerSize) / 2;
                            this.mDelta += (float) this.mMultiDelta;
                        }
                        this.mScreenBitmap.recycle();
                        this.mScreenBitmap = cropBmp;
                        this.targetBgAlpha = 0;
                    } catch (Exception e3) {
                        e3.printStackTrace();
                    }
                }
                boolean isForceBlack = MiuiSettings.Global.getBoolean(getContext().getContentResolver(), "force_black");
                if (!this.mIsMultiWindow && Constants.IS_NOTCH && isForceBlack && this.mScreenHeight > this.mScreenWidth) {
                    try {
                        Bitmap cropBmp2 = Bitmap.createBitmap(this.mScreenBitmap, 0, this.mStatusBarHeight, this.mScreenWidth, this.mScreenHeight - this.mStatusBarHeight);
                        this.mScreenBitmap.recycle();
                        this.mScreenBitmap = cropBmp2;
                        this.mIsAlreadyCropStatusBar = true;
                    } catch (Exception e4) {
                        e4.printStackTrace();
                    }
                }
                this.mScreenBitmap = createRoundCornerBmp(this.mScreenBitmap);
                this.mScreenBmpWidth = this.mScreenBitmap.getWidth();
                this.mScreenBmpHeight = this.mScreenBitmap.getHeight();
                this.mScreenBitmap.setHasAlpha(false);
                this.mScreenBitmap.prepareToDraw();
                return;
            case 1:
                actionUpResolution();
                return;
            case 2:
                actionMoveResolution(event);
                this.mCurScale = 1.0f - (linearToCubic(this.mCurrentY, (float) this.mScreenHeight, 0.0f, 3.0f) * 0.385f);
                invalidate();
                Log.d(TAG, "=======>>>>>move: " + SystemClock.uptimeMillis());
                return;
            case 3:
                this.mGestureStubListenerWrapper.onGestureFinish(false);
                if (this.mOrientationChangedAfterDown) {
                    startAppAnimation(3);
                    return;
                } else {
                    finalization(false, true, true, "appTouchResolution");
                    return;
                }
            default:
                return;
        }
    }

    private void homeTouchResolution(MotionEvent event) {
        this.mCurrentY = event.getRawY();
        switch (event.getAction()) {
            case 0:
                initValue(event);
                return;
            case 1:
            case 3:
                actionUpResolution();
                return;
            case 2:
                actionMoveResolution(event);
                this.mCurScale = 1.0f - (linearToCubic(this.mCurrentY, (float) this.mScreenHeight, 0.0f, 3.0f) * 0.15f);
                if (this.mStateMode != 65539 && !this.mHomeFadeInAnim.isRunning()) {
                    Recents.getSystemServices().changeAlphaScaleForFsGesture(Constants.HOME_LAUCNHER_PACKAGE_NAME, 1.0f, this.mCurScale);
                    return;
                }
                return;
            default:
                return;
        }
    }

    private void simpleTouchResolution(MotionEvent event) {
        this.mCurrentY = event.getRawY();
        switch (event.getAction()) {
            case 0:
                initValue(event);
                return;
            case 1:
            case 3:
                if (this.mStateMode == 65538 && ((float) this.mPivotLocY) - this.mFollowTailY < -40.0f) {
                    this.mContext.sendBroadcast(this.mCloseScreenshotIntent);
                    if (this.mWindowMode == 4) {
                        sendEvent(0, 0, 3);
                        sendEvent(1, 0, 3);
                    } else {
                        RecentsEventBus.getDefault().send(new HideRecentsEvent(false, true, false));
                    }
                }
                finalization(false, false, false, "simpleTouchResolution");
                return;
            case 2:
                this.mPivotLocX = (int) (((event.getRawX() + this.mDownX) / 2.0f) + this.mDelta);
                this.mPivotLocY = (int) (((float) this.mScreenHeight) - (linearToCubic(this.mCurrentY, (float) this.mScreenHeight, 0.0f, 3.0f) * 444.0f));
                if (this.mStateMode == 65537) {
                    this.mStateMode = 65538;
                    Log.d(TAG, "current state mode: StateMode.STATE_ON_DRAG");
                    this.mFrameHandler.post(this.mTailCatcherTask);
                    updateViewLayout(-1);
                    return;
                }
                return;
            default:
                return;
        }
    }

    public void sendEvent(int action, int flags, int code) {
        sendEvent(action, flags, code, SystemClock.uptimeMillis());
    }

    /* access modifiers changed from: package-private */
    public void sendEvent(int action, int flags, int code, long when) {
        int i = flags;
        KeyEvent keyEvent = new KeyEvent(this.mDownTime, when, action, code, (i & 128) != 0 ? 1 : 0, 0, -1, 0, i | 8 | 64, 257);
        InputManager.getInstance().injectInputEvent(keyEvent, 0);
    }

    private void initValue(MotionEvent event) {
        this.mDownX = event.getRawX();
        this.mIsAppToHome = false;
        this.mDelta = ((float) (this.mScreenWidth / 2)) - this.mDownX;
        int i = this.mScreenWidth / 2;
        this.mPivotLocX = i;
        this.mFollowTailX = (float) i;
        int i2 = this.mScreenHeight;
        this.mPivotLocY = i2;
        this.mFollowTailY = (float) i2;
        this.mCurTask = null;
        this.mStateMode = 65537;
        Log.d(TAG, "current state mode: StateMode.STATE_INIT");
    }

    private Bitmap createRoundCornerBmp(Bitmap srcBmp) {
        Bitmap target = Bitmap.createBitmap(srcBmp.getWidth(), srcBmp.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(target);
        canvas.drawRoundRect(new RectF(0.0f, 0.0f, (float) srcBmp.getWidth(), (float) srcBmp.getHeight()), (float) this.RADIUS_SIZE, (float) this.RADIUS_SIZE, this.mPaint);
        this.mPaint.setXfermode(this.mModeSrcIn);
        canvas.drawBitmap(srcBmp, 0.0f, 0.0f, this.mPaint);
        this.mPaint.setXfermode(null);
        return target;
    }

    private void actionMoveResolution(MotionEvent event) {
        this.mPivotLocX = (int) (((event.getRawX() + this.mDownX) / 2.0f) + this.mDelta);
        this.mPivotLocY = (int) (((float) this.mScreenHeight) - (linearToCubic(this.mCurrentY, (float) this.mScreenHeight, 0.0f, 3.0f) * 444.0f));
        if (this.mStatusBar != null) {
        }
        if (this.mStateMode == 65537) {
            this.mStateMode = 65538;
            Log.d(TAG, "current state mode: StateMode.STATE_ON_DRAG");
            this.mFrameHandler.post(this.mTailCatcherTask);
            updateViewLayout(-1);
            if (this.mWindowMode == 2) {
                setBackgroundColor(Color.argb(this.targetBgAlpha, 0, 0, 0));
            }
            if (this.mRecentsBg != null) {
                this.mRecentsBg.setAlpha(0.0f);
            }
            if (this.mRecentsContainer != null) {
                this.mRecentsContainer.setAlpha(0.0f);
            }
            if (this.mWindowMode == 2) {
                this.mGestureStubListenerWrapper.skipAppTransition();
            }
            RecentsEventBus.getDefault().post(new FsGesturePreloadRecentsEvent());
        }
    }

    private void actionUpResolution() {
        this.mFrameHandler.removeCallbacksAndMessages(null);
        boolean isOnDrag = this.mStateMode == 65538;
        boolean isTaskHold = this.mStateMode == 65539;
        if (!isOnDrag && !isTaskHold) {
            finalization(false, true, true, "actionUpResolution-5");
        } else if (((float) this.mPivotLocY) - this.mFollowTailY > 40.0f) {
            if (isTaskHold) {
                RecentsEventBus.getDefault().send(new FsGestureSlideOutEvent());
            }
            if (this.mWindowMode == 2) {
                startAppAnimation(1);
            } else if (this.mWindowMode == 1) {
                startHomeAnimation(1);
            } else {
                finalization(false, true, true, "actionUpResolution-1");
            }
        } else if (((float) this.mPivotLocY) - this.mFollowTailY < -40.0f) {
            if (isTaskHold) {
                RecentsEventBus.getDefault().send(new FsGestureSlideOutEvent());
            }
            this.mContext.sendBroadcast(this.mCloseScreenshotIntent);
            if (this.mWindowMode == 2) {
                startAppAnimation(2);
            } else if (this.mWindowMode == 1) {
                startHomeAnimation(2);
            } else {
                finalization(false, true, true, "actionUpResolution-2");
            }
        } else if (!isOnDrag) {
            this.mContext.sendBroadcast(this.mCloseScreenshotIntent);
            if (this.mWindowMode == 2) {
                startAppAnimation(3);
            } else if (this.mWindowMode == 1) {
                startHomeAnimation(3);
            } else {
                finalization(false, true, true, "actionUpResolution-4");
            }
        } else if (this.mWindowMode == 2) {
            startAppAnimation(1);
        } else if (this.mWindowMode == 1) {
            startHomeAnimation(1);
        } else {
            finalization(false, true, true, "actionUpResolution-3");
        }
    }

    private void startHomeAnimation(final int type) {
        if (this.mStateMode == 65539 && (type == 1 || type == 2)) {
            this.mHomeFadeInAnim.start();
        }
        ValueAnimator animator = ValueAnimator.ofFloat(new float[]{this.mCurScale, 1.0f});
        animator.setInterpolator(new DecelerateInterpolator());
        float curRecentsViewTranslationY = 0.0f;
        float curRecentsViewScale = 0.0f;
        float curRecentsViewAlpha = 0.0f;
        if (this.mRecentsView != null) {
            curRecentsViewTranslationY = this.mRecentsView.getTranslationY();
            curRecentsViewScale = this.mRecentsView.getScaleX();
            curRecentsViewAlpha = this.mRecentsView.getAlpha();
        }
        final float finalCurTranslationY = curRecentsViewTranslationY;
        final float finalCurScale = curRecentsViewScale;
        final float finalCurAlpha = curRecentsViewAlpha;
        final int i = type;
        AnonymousClass9 r5 = new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                float unused = NavStubView.this.mCurScale = ((Float) animation.getAnimatedValue()).floatValue();
                float fraction = animation.getAnimatedFraction();
                float scale = NavStubView.this.mCurScale;
                switch (i) {
                    case 1:
                    case 2:
                        if (!NavStubView.this.mHomeFadeInAnim.isRunning()) {
                            Recents.getSystemServices().changeAlphaScaleForFsGesture(Constants.HOME_LAUCNHER_PACKAGE_NAME, 1.0f, scale);
                            return;
                        }
                        return;
                    case 3:
                        NavStubView.this.controlRecentTaskView(finalCurTranslationY, finalCurScale, finalCurAlpha, fraction);
                        return;
                    default:
                        return;
                }
            }
        };
        animator.addUpdateListener(r5);
        long duration = (long) DEFAULT_ANIM_TIME;
        if (type == 2) {
            duration = 200;
        }
        animator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animation) {
                if (type == 2) {
                    NavStubView.this.mHomeIntent.putExtra("ignore_bring_to_front", true);
                    NavStubView.this.mHomeIntent.putExtra("filter_flag", true);
                    NavStubView.this.mContext.startActivityAsUser(NavStubView.this.mHomeIntent, ActivityOptions.makeCustomAnimation(NavStubView.this.mContext, 0, 0).toBundle(), UserHandle.CURRENT);
                } else if (type == 3 && NavStubView.this.mRecentsView != null) {
                    NavStubView.this.mRecentsView.animate().cancel();
                }
            }

            public void onAnimationEnd(Animator animation) {
                long delayTime = 100;
                int i = type;
                if (i == 1) {
                    NavStubView.this.mContext.startActivityAsUser(NavStubView.this.mHomeIntent, ActivityOptions.makeCustomAnimation(NavStubView.this.mContext, 0, 0).toBundle(), UserHandle.CURRENT);
                } else if (i == 3) {
                    RecentsEventBus.getDefault().send(new FsGestureShowFirstCardEvent());
                    RecentsEventBus.getDefault().send(new AnimFirstTaskViewAlphaEvent(1.0f, false));
                    RecentsEventBus.getDefault().send(new FsGestureEnterRecentsCompleteEvent());
                    delayTime = 50;
                }
                NavStubView.this.mFrameHandler.postDelayed(new Runnable() {
                    public void run() {
                        NavStubView.this.finalization(false, false, true, "startHomeAnimation");
                    }
                }, delayTime);
            }
        });
        animator.setDuration(duration).start();
    }

    private void exitFreeFormWindowIfNeeded() {
        MiuiMultiWindowUtils.exitFreeFormWindowIfNeeded();
    }

    private void startAppAnimation(int type) {
        ValueAnimator animator;
        String tempAction;
        TransitionAnimationSpec transitionAnimationSpec;
        final int i = type;
        if (i == 2) {
            if (Constants.IS_TABLET || getResources().getConfiguration().orientation == 1) {
                this.mIsAppToHome = false;
                TransitionAnimationSpec animationSpec = null;
                this.mHomeCallback = Recents.getSystemServices().mIFsGestureCallbackMap.get(Constants.HOME_LAUCNHER_PACKAGE_NAME);
                try {
                    if (this.mHomeCallback != null) {
                        IFsGestureCallback iFsGestureCallback = this.mHomeCallback;
                        transitionAnimationSpec = iFsGestureCallback.getSpec(this.mCurTask.baseIntent.getComponent().getPackageName() + "/", this.mCurTask.userId);
                    } else {
                        transitionAnimationSpec = null;
                    }
                    animationSpec = transitionAnimationSpec;
                    this.mIsAppToHome = (animationSpec == null || animationSpec.mRect == null || animationSpec.mRect.top == 0 || animationSpec.mRect.right == 0 || animationSpec.mBitmap == null) ? false : true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (this.mIsAppToHome) {
                    this.mGestureStubListenerWrapper.skipAppTransition();
                    final int destPivotY = (animationSpec.mRect.bottom + animationSpec.mRect.top) / 2;
                    final int destPivotX = (animationSpec.mRect.right + animationSpec.mRect.left) / 2;
                    this.mAppIcon = animationSpec.mBitmap;
                    float curScale = this.mCurScale;
                    int curPivotX = this.mPivotLocX;
                    AnimatorSet animatorSet = new AnimatorSet();
                    animatorSet.addListener(new AnimatorListenerAdapter() {
                        public void onAnimationStart(Animator animation) {
                            Recents.getSystemServices().setIsFsGestureAnimating(true);
                            NavStubView.this.mContext.startActivityAsUser(NavStubView.this.mHomeIntent, ActivityOptions.makeCustomAnimation(NavStubView.this.mContext, 0, 0).toBundle(), UserHandle.CURRENT);
                            try {
                                NavStubView.this.mHomeCallback.notifyMiuiAnimationStart();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        public void onAnimationEnd(Animator animation) {
                            NavStubView.this.finalization(false, false, true, "startAppAnimation-1");
                            Recents.getSystemServices().setIsFsGestureAnimating(false);
                            NavStubJankyFrameReporter.recordJankyFrames("home");
                        }
                    });
                    PropertyValuesHolder homeScaleHolder = PropertyValuesHolder.ofFloat("homeScale", new float[]{0.8f, 1.0f});
                    PropertyValuesHolder homeAlphaHolder = PropertyValuesHolder.ofFloat("homeAlpha", new float[]{0.0f, 1.0f});
                    TransitionAnimationSpec transitionAnimationSpec2 = animationSpec;
                    PropertyValuesHolder xScaleHolder = PropertyValuesHolder.ofFloat("xScale", new float[]{this.mCurScale, 0.03f});
                    PropertyValuesHolder yScaleHolder = PropertyValuesHolder.ofFloat("yScale", new float[]{this.mCurScale, 0.03f});
                    PropertyValuesHolder xPivotHolder = PropertyValuesHolder.ofInt("xPivot", new int[]{curPivotX, destPivotX});
                    float f = curScale;
                    ValueAnimator quarterAnimator = ValueAnimator.ofPropertyValuesHolder(new PropertyValuesHolder[]{yScaleHolder, PropertyValuesHolder.ofInt("yPivot", new int[]{(int) (((float) this.mPivotLocY) - ((((float) this.mScreenBmpHeight) * curScale) / 2.0f)), destPivotY}), xScaleHolder, xPivotHolder, homeAlphaHolder, homeScaleHolder});
                    quarterAnimator.setInterpolator(this.mQuartEaseOutInterpolator);
                    quarterAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        public void onAnimationUpdate(ValueAnimator animation) {
                            NavStubJankyFrameReporter.caculateAnimationFrameInterval("home");
                            float unused = NavStubView.this.mYScale = ((Float) animation.getAnimatedValue("yScale")).floatValue();
                            float unused2 = NavStubView.this.mXScale = ((Float) animation.getAnimatedValue("xScale")).floatValue();
                            int unused3 = NavStubView.this.mPivotLocY = ((Integer) animation.getAnimatedValue("yPivot")).intValue();
                            int unused4 = NavStubView.this.mPivotLocX = ((Integer) animation.getAnimatedValue("xPivot")).intValue();
                            float scale = NavStubView.IS_E10 ? 1.0f : ((Float) animation.getAnimatedValue("homeScale")).floatValue();
                            if (animation.getAnimatedFraction() > 0.75f) {
                                boolean unused5 = NavStubView.this.mIsBgIconVisible = true;
                            }
                            Recents.getSystemServices().changeAlphaScaleForFsGesture(Constants.HOME_LAUCNHER_PACKAGE_NAME, ((Float) animation.getAnimatedValue("homeAlpha")).floatValue(), scale, destPivotX, destPivotY, NavStubView.this.mPivotLocX, NavStubView.this.mPivotLocY, NavStubView.this.mIsBgIconVisible);
                            NavStubView.this.invalidate();
                        }
                    });
                    int i2 = destPivotY;
                    PropertyValuesHolder propertyValuesHolder = xScaleHolder;
                    quarterAnimator.setDuration((long) DEFAULT_ANIM_TIME);
                    ValueAnimator alphaAnimator = ValueAnimator.ofFloat(new float[]{1.0f, 0.0f});
                    alphaAnimator.setInterpolator(this.mCubicEaseOutInterpolator);
                    alphaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        public void onAnimationUpdate(ValueAnimator animation) {
                            float unused = NavStubView.this.mCurAlpha = ((Float) animation.getAnimatedValue()).floatValue();
                        }
                    });
                    int i3 = destPivotX;
                    alphaAnimator.setDuration((long) (DEFAULT_ANIM_TIME / 4));
                    alphaAnimator.setStartDelay((long) (DEFAULT_ANIM_TIME / 3));
                    ValueAnimator bgAlphaAnimator = ValueAnimator.ofInt(new int[]{this.targetBgAlpha, 0});
                    bgAlphaAnimator.setInterpolator(this.mCubicEaseOutInterpolator);
                    bgAlphaAnimator.addListener(new AnimatorListenerAdapter() {
                        public void onAnimationEnd(Animator animation) {
                            try {
                                NavStubView.this.mHomeCallback.notifyMiuiAnimationEnd();
                                NavStubView.this.mGestureStubListenerWrapper.onGestureFinish(false);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    bgAlphaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        public void onAnimationUpdate(ValueAnimator animation) {
                            NavStubView.this.setBackgroundColor(Color.argb(((Integer) animation.getAnimatedValue()).intValue(), 0, 0, 0));
                        }
                    });
                    PropertyValuesHolder propertyValuesHolder2 = xPivotHolder;
                    bgAlphaAnimator.setDuration((long) DEFAULT_ANIM_TIME);
                    NavStubJankyFrameReporter.resetAnimationFrameIntervalParams("home");
                    animatorSet.playTogether(new Animator[]{quarterAnimator, bgAlphaAnimator, alphaAnimator});
                    animatorSet.start();
                    return;
                }
            } else {
                this.mGestureStubListenerWrapper.skipAppTransition();
                this.mContext.startActivityAsUser(this.mHomeIntent, ActivityOptions.makeCustomAnimation(this.mContext, 0, 0).toBundle(), UserHandle.CURRENT);
                finalization(false, true, true, "startAppAnimation-2");
                return;
            }
        }
        float curTranslationY = 0.0f;
        float curScale2 = 0.0f;
        float curAlpha = 0.0f;
        switch (i) {
            case 1:
                animator = ValueAnimator.ofFloat(new float[]{this.mCurScale, 1.0f});
                tempAction = "cancel";
                break;
            case 2:
                animator = ValueAnimator.ofFloat(new float[]{this.mCurScale, 0.5f});
                tempAction = "home";
                break;
            case 3:
                this.mIsAppToRecents = true;
                float destScale = 0.0f;
                if (this.mDestRectF != null) {
                    destScale = this.mDestRectF.width() / ((float) this.mScreenBmpWidth);
                }
                animator = ValueAnimator.ofFloat(new float[]{this.mCurScale, destScale});
                if (this.mRecentsView != null) {
                    curTranslationY = this.mRecentsView.getTranslationY();
                    curScale2 = this.mRecentsView.getScaleX();
                    curAlpha = this.mRecentsView.getAlpha();
                }
                tempAction = "recents";
                break;
            default:
                return;
        }
        float curTranslationY2 = curTranslationY;
        String tempAction2 = tempAction;
        ValueAnimator animator2 = animator;
        String action = tempAction2;
        animator2.setInterpolator(new DecelerateInterpolator());
        int initX = this.mPivotLocX;
        final float finalCurTranslationY = curTranslationY2;
        final float finalCurScale = curScale2;
        final float finalCurAlpha = curAlpha;
        String str = tempAction2;
        AnonymousClass16 r0 = r1;
        final int i4 = i;
        int initY = this.mPivotLocY;
        final int initY2 = initX;
        int i5 = initX;
        final int initX2 = initY;
        String action2 = action;
        final float f2 = this.mCurScale;
        float f3 = curTranslationY2;
        ValueAnimator animator3 = animator2;
        final String str2 = action2;
        AnonymousClass16 r1 = new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                float unused = NavStubView.this.mCurScale = ((Float) animation.getAnimatedValue()).floatValue();
                float fraction = animation.getAnimatedFraction();
                int i = 0;
                switch (i4) {
                    case 1:
                        int xDestLoc = NavStubView.this.mScreenWidth / 2;
                        if (NavStubView.this.mIsMultiWindow && NavStubView.this.mScreenHeight < NavStubView.this.mScreenWidth) {
                            xDestLoc += NavStubView.this.mMultiDelta;
                        }
                        int unused2 = NavStubView.this.mPivotLocX = (int) (((float) initY2) + (((float) (xDestLoc - initY2)) * fraction));
                        int unused3 = NavStubView.this.mPivotLocY = (int) (((float) initX2) + (((float) (NavStubView.this.mScreenHeight - initX2)) * fraction));
                        break;
                    case 2:
                        int unused4 = NavStubView.this.mPivotLocX = (int) (((float) initY2) + (((float) ((NavStubView.this.mScreenBmpWidth / 2) - initY2)) * fraction));
                        int unused5 = NavStubView.this.mPivotLocY = (int) (((float) initX2) + (((float) (((NavStubView.this.mScreenBmpHeight * 3) / 4) - initX2)) * fraction));
                        NavStubView.this.setBackgroundColor(Color.argb((int) (((float) NavStubView.this.targetBgAlpha) * (1.0f - fraction)), 0, 0, 0));
                        float unused6 = NavStubView.this.mCurAlpha = 1.0f - fraction;
                        break;
                    case 3:
                        NavStubView.this.controlRecentTaskView(finalCurTranslationY, finalCurScale, finalCurAlpha, fraction);
                        NavStubView.this.setBackgroundColor(Color.argb((int) (((float) NavStubView.this.targetBgAlpha) * (1.0f - fraction)), 0, 0, 0));
                        float destX = 0.0f;
                        float destY = 0.0f;
                        if (NavStubView.this.mDestRectF != null) {
                            destX = (NavStubView.this.mDestRectF.left + NavStubView.this.mDestRectF.right) / 2.0f;
                            destY = NavStubView.this.mDestRectF.bottom;
                        }
                        int unused7 = NavStubView.this.mPivotLocX = (int) (((float) initY2) + ((destX - ((float) initY2)) * fraction));
                        int unused8 = NavStubView.this.mPivotLocY = (int) (((float) initX2) + ((destY - ((float) initX2)) * fraction));
                        float unused9 = NavStubView.this.mScreenBmpScale = f2 + ((NavStubView.this.mDestRectHeightScale - f2) * fraction);
                        float unused10 = NavStubView.this.mDestTopOffset = (1.0f - NavStubView.this.mDestRectHeightScale) * fraction;
                        NavStubView navStubView = NavStubView.this;
                        if (!NavStubView.this.mIsAlreadyCropStatusBar && !NavStubView.this.mIsFullScreenModeCurTime) {
                            i = (int) (((float) NavStubView.this.mStatusBarHeight) * fraction);
                        }
                        int unused11 = navStubView.mStatusBarDec = i;
                        break;
                }
                NavStubJankyFrameReporter.caculateAnimationFrameInterval(str2);
                NavStubView.this.invalidate();
            }
        };
        animator3.addUpdateListener(r0);
        final String action3 = action2;
        animator3.addListener(new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animation) {
                if (i == 2) {
                    NavStubView.this.mGestureStubListenerWrapper.skipAppTransition();
                    NavStubView.this.mContext.startActivityAsUser(NavStubView.this.mHomeIntent, ActivityOptions.makeCustomAnimation(NavStubView.this.mContext, 0, 0).toBundle(), UserHandle.CURRENT);
                } else if (i == 3 && NavStubView.this.mRecentsView != null) {
                    NavStubView.this.mRecentsView.animate().cancel();
                }
            }

            public void onAnimationEnd(Animator animation) {
                long delayTime = 0;
                boolean isRecoveryHomeIcon = true;
                switch (i) {
                    case 1:
                        if (NavStubView.this.mCurTask != null) {
                            try {
                                NavStubView.this.mIam.startActivityFromRecents(NavStubView.this.mCurTask.persistentId, ActivityOptions.makeCustomAnimation(NavStubView.this.mContext, 0, 0).toBundle());
                            } catch (Exception e) {
                                Log.e(NavStubView.TAG, "Fail to start activity", e);
                                NavStubView.this.mContext.startActivityAsUser(NavStubView.this.mHomeIntent, ActivityOptions.makeCustomAnimation(NavStubView.this.mContext, 0, 0).toBundle(), UserHandle.CURRENT);
                            }
                        }
                        boolean unused = NavStubView.this.mCancelActionToStartApp = true;
                        if (NavStubView.this.getResources().getConfiguration().orientation == 2) {
                            delayTime = 400;
                        } else {
                            delayTime = 300;
                        }
                        NavStubView.this.mGestureStubListenerWrapper.onGestureFinish(true);
                        break;
                    case 2:
                        isRecoveryHomeIcon = false;
                        NavStubView.this.mGestureStubListenerWrapper.onGestureFinish(false);
                        break;
                    case 3:
                        RecentsEventBus.getDefault().send(new FsGestureShowFirstCardEvent());
                        RecentsEventBus.getDefault().send(new AnimFirstTaskViewAlphaEvent(1.0f, false));
                        RecentsEventBus.getDefault().send(new FsGestureEnterRecentsCompleteEvent());
                        NavStubView.this.mGestureStubListenerWrapper.onGestureFinish(false);
                        break;
                }
                final boolean finalIsRecoveryHomeIcon = isRecoveryHomeIcon;
                NavStubView.this.mFrameHandler.postDelayed(new Runnable() {
                    public void run() {
                        NavStubView navStubView = NavStubView.this;
                        boolean z = finalIsRecoveryHomeIcon;
                        navStubView.finalization(false, z, true, "startAppAnimation-3-" + i);
                    }
                }, delayTime);
                NavStubJankyFrameReporter.recordJankyFrames(action3);
            }
        });
        NavStubJankyFrameReporter.resetAnimationFrameIntervalParams(action3);
        if (i == 2) {
            AnimatorSet animatorSet2 = new AnimatorSet();
            ValueAnimator cubicAnimator = ValueAnimator.ofPropertyValuesHolder(new PropertyValuesHolder[]{PropertyValuesHolder.ofFloat("homeAlpha", new float[]{0.0f, 1.0f}), PropertyValuesHolder.ofFloat("homeScale", new float[]{2.0f, 1.0f})});
            cubicAnimator.setInterpolator(this.mCubicEaseOutInterpolator);
            cubicAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    Recents.getSystemServices().changeAlphaScaleForFsGesture(Constants.HOME_LAUCNHER_PACKAGE_NAME, ((Float) animation.getAnimatedValue("homeAlpha")).floatValue(), NavStubView.IS_E10 ? 1.0f : ((Float) animation.getAnimatedValue("homeScale")).floatValue());
                }
            });
            animatorSet2.playTogether(new Animator[]{animator3, cubicAnimator});
            animatorSet2.setDuration(200).start();
        } else {
            animator3.setDuration((long) DEFAULT_ANIM_TIME).start();
        }
    }

    /* access modifiers changed from: private */
    public void controlRecentTaskView(float translationY, float scale, float alpha, float fraction) {
        if (this.mRecentsView != null) {
            this.mRecentsView.setTranslationY((1.0f - fraction) * translationY);
            float viewScale = ((scale - 1.0f) * (1.0f - fraction)) + 1.0f;
            this.mRecentsView.setScaleX(viewScale);
            this.mRecentsView.setScaleY(viewScale);
            this.mRecentsView.setAlpha(1.0f + ((alpha - 1.0f) * (1.0f - fraction)));
            this.mRecentsContainer.setAlpha(fraction);
        }
    }

    /* access modifiers changed from: private */
    public void finalization(boolean isShowHomeAnimation, boolean isRecoverHomeIcon, boolean isRecoverTask, String fromTag) {
        String str = TAG;
        Log.d(str, "===>>>finalization executed from: " + fromTag);
        this.mIsFullScreenModeCurTime = false;
        this.mIsAlreadyCropStatusBar = false;
        this.mIsBgIconVisible = false;
        this.mIsEnterRecents = false;
        this.mIsAppToRecents = false;
        this.mIsAppToHome = false;
        this.mCancelActionToStartApp = false;
        this.mDestRectF = null;
        this.mCurAlpha = 1.0f;
        this.targetBgAlpha = 136;
        this.mPaint.setAlpha(255);
        this.mHomeIntent.removeExtra("ignore_bring_to_front");
        this.mHomeIntent.removeExtra("filter_flag");
        this.mStateMode = 65537;
        Log.d(TAG, "current state mode: StateMode.STATE_INIT");
        if (this.mScreenBitmap != null) {
            this.mScreenBitmap.recycle();
            this.mScreenBitmap = null;
            this.mBitmapShown = false;
        }
        setBackgroundColor(0);
        post(new Runnable() {
            public void run() {
                NavStubView.this.updateViewLayout((int) (((float) NavStubView.this.mWindowSize) * NavStubView.this.mContext.getResources().getDisplayMetrics().density));
            }
        });
        if (this.mFrameHandler != null) {
            this.mFrameHandler.removeCallbacksAndMessages(null);
        }
        if (isRecoverTask && this.mRecentsView != null) {
            this.mRecentsView.setAlpha(1.0f);
            this.mRecentsView.setScaleX(1.0f);
            this.mRecentsView.setScaleY(1.0f);
        }
        if (isRecoverHomeIcon) {
            if (isShowHomeAnimation) {
                ValueAnimator animator = ValueAnimator.ofFloat(new float[]{0.95f, 1.0f});
                animator.setInterpolator(new DecelerateInterpolator());
                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    public void onAnimationUpdate(ValueAnimator animation) {
                        Recents.getSystemServices().changeAlphaScaleForFsGesture(Constants.HOME_LAUCNHER_PACKAGE_NAME, 1.0f, ((Float) animation.getAnimatedValue()).floatValue());
                    }
                });
                animator.addListener(new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator animation) {
                        Recents.getSystemServices().setIsFsGestureAnimating(false);
                    }
                });
                animator.setDuration(200).start();
            } else {
                Recents.getSystemServices().changeAlphaScaleForFsGesture(Constants.HOME_LAUCNHER_PACKAGE_NAME, 1.0f, 1.0f);
            }
        }
        this.mIsInFsMode = false;
    }

    private float linearToCubic(float now, float orignal, float end, float pow) {
        if (pow == orignal) {
            return now;
        }
        float ease = 0.0f;
        float percent = (now - orignal) / (pow - orignal);
        if (pow != 0.0f) {
            ease = (float) (1.0d - Math.pow((double) (1.0f - percent), (double) pow));
        }
        return ease;
    }

    public boolean gatherTransparentRegion(Region region) {
        if (!this.mNeedRender && region != null && !isMistakeTouch()) {
            int w = getWidth();
            int h = getHeight();
            if (DEBUG) {
                String str = TAG;
                Log.d(str, "gatherTransparentRegion: need render w:" + w + "  h:" + h);
            }
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
    public void updateViewLayout(int stubSize) {
        this.mWindowManager.updateViewLayout(this, getWindowParam(stubSize));
        if (getParent() != null) {
            this.mNeedRender = stubSize == -1;
            if (DEBUG) {
                String str = TAG;
                Log.d(str, "need render:" + this.mNeedRender);
            }
            getParent().requestTransparentRegion(this);
        }
    }

    public WindowManager.LayoutParams getWindowParam(int stubSize) {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(-1, stubSize, 2027, 296, -3);
        lp.privateFlags |= 64;
        lp.gravity = 80;
        lp.setTitle("GestureStub");
        return lp;
    }

    /* access modifiers changed from: private */
    public void updateRecentsBlurRatio(float ratio) {
        if (this.mRecentsView != null) {
            this.mRecentsView.updateBlurRatio(Math.min(1.0f, ratio));
        }
    }
}
