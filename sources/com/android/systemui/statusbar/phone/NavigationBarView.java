package com.android.systemui.statusbar.phone;

import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.app.ActivityManagerNative;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewRootImpl;
import android.view.ViewRootImplCompat;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.Util;
import com.android.systemui.events.AspectClickEvent;
import com.android.systemui.recents.events.RecentsEventBus;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.DeadZone;
import com.android.systemui.statusbar.policy.KeyButtonView;
import com.android.systemui.statusbar.policy.OpaLayout;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

public class NavigationBarView extends LinearLayout {
    private static int sFilterColor = 0;
    private static HashMap<Integer, Integer> sKeyIdMap = new HashMap<>();
    private final View.OnClickListener mAspectClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            RecentsEventBus.getDefault().send(new AspectClickEvent());
        }
    };
    private Drawable mBackAltIcon;
    private Drawable mBackAltLandIcon;
    private Drawable mBackIcon;
    private Drawable mBackLandIcon;
    /* access modifiers changed from: private */
    public StatusBar mBar;
    int mBarSize;
    private final NavigationBarTransitions mBarTransitions;
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if ("miui.intent.TAKE_SCREENSHOT".equals(intent.getAction())) {
                boolean screenshotFinished = intent.getBooleanExtra("IsFinished", true);
                Log.d("PhoneStatusBar/NavigationBarView", "ACTION_TAKE_SCREENSHOT:" + screenshotFinished);
                if (!screenshotFinished) {
                    NavigationBarView.this.getRecentsButton().setEnabled(false);
                    NavigationBarView.this.getRecentsButton().setPressed(false);
                    return;
                }
                NavigationBarView.this.getRecentsButton().setEnabled(true);
            }
        }
    };
    private Configuration mConfiguration;
    private ConfigurationController.ConfigurationListener mConfigurationListener;
    View mCurrentView = null;
    private DrawableSuit mDarkSuit;
    private DeadZone mDeadZone;
    int mDisabledFlags = 0;
    final Display mDisplay;
    private DisplayManager.DisplayListener mDisplayListener;
    private DisplayManager mDisplayManager;
    private boolean mForceHide;
    private H mHandler = new H();
    private Drawable mHomeIcon;
    private final View.OnClickListener mImeSwitcherClickListener = new View.OnClickListener() {
        public void onClick(View view) {
            ((InputMethodManager) NavigationBarView.this.mContext.getSystemService("input_method")).showInputMethodPicker(true);
        }
    };
    private boolean mIsLayoutRtl;
    private ArrayList<Integer> mKeyOrder = new ArrayList<>();
    private boolean mLayoutTransitionsEnabled = true;
    private DrawableSuit mLightSuit;
    int mNavigationIconHints = 0;
    private OnVerticalChangedListener mOnVerticalChangedListener;
    private boolean mOpaEnable;
    private Drawable mRecentIcon;
    private Drawable mRecentLandIcon;
    View[] mRotatedViews = new View[4];
    /* access modifiers changed from: private */
    public final ContentObserver mScreenKeyOrderObserver = new ContentObserver(this.mHandler) {
        public void onChange(boolean selfChange) {
            NavigationBarView.this.adjustKeyOrder();
        }
    };
    boolean mShowAspect;
    boolean mShowMenu;
    private NavigationBarViewTaskSwitchHelper mTaskSwitchHelper;
    private final NavTransitionListener mTransitionListener = new NavTransitionListener();
    private final KeyguardUpdateMonitorCallback mUserSwitchCallback = new KeyguardUpdateMonitorCallback() {
        public void onUserSwitching(int userId) {
            NavigationBarView.this.mScreenKeyOrderObserver.onChange(false);
        }
    };
    boolean mVertical;
    private boolean mWakeAndUnlocking;

    private static class DrawableSuit {
        Drawable mBack;
        Drawable mBackAlt;
        int mBgColor;
        Drawable mBgLand;
        Drawable mBgLandCTS;
        Drawable mBgPort;
        Drawable mBgPortCTS;
        Drawable mHome;
        Drawable mRecent;

        public static class Builder {
            private int mBack = R.drawable.ic_sysbar_back;
            private int mBackAlt = R.drawable.ic_sysbar_back_ime;
            private int mBgColorRes = R.color.nav_bar_background_color;
            private int mBgLand = R.drawable.ic_sysbar_bg_land;
            private int mBgLandCTS = 0;
            private int mBgPort = R.drawable.ic_sysbar_bg;
            private int mBgPortCTS = 0;
            private Context mContext;
            private int mHome = R.drawable.ic_sysbar_home;
            private int mRecent = R.drawable.ic_sysbar_recent;

            Builder(Context context) {
                this.mContext = context;
            }

            /* access modifiers changed from: package-private */
            public Builder setBack(int back) {
                this.mBack = back;
                return this;
            }

            /* access modifiers changed from: package-private */
            public Builder setBackAlt(int backAlt) {
                this.mBackAlt = backAlt;
                return this;
            }

            /* access modifiers changed from: package-private */
            public Builder setHome(int home) {
                this.mHome = home;
                return this;
            }

            /* access modifiers changed from: package-private */
            public Builder setRecent(int recent) {
                this.mRecent = recent;
                return this;
            }

            /* access modifiers changed from: package-private */
            public Builder setBgPort(int bgPort) {
                this.mBgPort = bgPort;
                return this;
            }

            /* access modifiers changed from: package-private */
            public Builder setBgLand(int bgLand) {
                this.mBgLand = bgLand;
                return this;
            }

            /* access modifiers changed from: package-private */
            public Builder setBgPortCTS(int bgPortCTS) {
                this.mBgPortCTS = bgPortCTS;
                return this;
            }

            /* access modifiers changed from: package-private */
            public Builder setBgLandCTS(int bgLandCTS) {
                this.mBgLandCTS = bgLandCTS;
                return this;
            }

            /* access modifiers changed from: package-private */
            public Builder setBgColorRes(int bgColorRes) {
                this.mBgColorRes = bgColorRes;
                return this;
            }

            public DrawableSuit build() {
                DrawableSuit suit = new DrawableSuit();
                Resources res = this.mContext.getResources();
                suit.mBack = res.getDrawable(this.mBack);
                suit.mBackAlt = res.getDrawable(this.mBackAlt);
                suit.mHome = res.getDrawable(this.mHome);
                suit.mRecent = res.getDrawable(this.mRecent);
                suit.mBgPort = res.getDrawable(this.mBgPort);
                suit.mBgLand = res.getDrawable(this.mBgLand);
                if (this.mBgPortCTS != 0) {
                    suit.mBgPortCTS = res.getDrawable(this.mBgPortCTS);
                }
                if (this.mBgLandCTS != 0) {
                    suit.mBgLandCTS = res.getDrawable(this.mBgLandCTS);
                }
                suit.mBgColor = res.getColor(this.mBgColorRes);
                return suit;
            }
        }

        private DrawableSuit() {
        }
    }

    private class H extends Handler {
        private H() {
        }

        public void handleMessage(Message m) {
            if (m.what == 8686) {
                String how = "" + m.obj;
                int w = NavigationBarView.this.getWidth();
                int h = NavigationBarView.this.getHeight();
                int vw = NavigationBarView.this.mCurrentView.getWidth();
                int vh = NavigationBarView.this.mCurrentView.getHeight();
                if (h != vh || w != vw) {
                    Log.w("PhoneStatusBar/NavigationBarView", String.format("*** Invalid layout in navigation bar (%s this=%dx%d cur=%dx%d)", new Object[]{how, Integer.valueOf(w), Integer.valueOf(h), Integer.valueOf(vw), Integer.valueOf(vh)}));
                    NavigationBarView.this.requestLayout();
                }
            }
        }
    }

    private class NavTransitionListener implements LayoutTransition.TransitionListener {
        private boolean mBackTransitioning;
        private long mDuration;
        private boolean mHomeAppearing;
        private TimeInterpolator mInterpolator;
        private long mStartDelay;

        private NavTransitionListener() {
        }

        public void startTransition(LayoutTransition transition, ViewGroup container, View view, int transitionType) {
            if (view.getId() == R.id.back) {
                this.mBackTransitioning = true;
            } else if (view.getId() == R.id.home && transitionType == 2) {
                this.mHomeAppearing = true;
                this.mStartDelay = transition.getStartDelay(transitionType);
                this.mDuration = transition.getDuration(transitionType);
                this.mInterpolator = transition.getInterpolator(transitionType);
            }
        }

        public void endTransition(LayoutTransition transition, ViewGroup container, View view, int transitionType) {
            if (view.getId() == R.id.back) {
                this.mBackTransitioning = false;
            } else if (view.getId() == R.id.home && transitionType == 2) {
                this.mHomeAppearing = false;
            }
        }

        public void onBackAltCleared() {
            if (!this.mBackTransitioning && NavigationBarView.this.getBackButton().getVisibility() == 0 && this.mHomeAppearing && NavigationBarView.this.getHomeButton().getAlpha() == 0.0f) {
                Log.d("PhoneStatusBar/NavigationBarView", "onBackAltCleared");
                NavigationBarView.this.getBackButton().setAlpha(0.0f);
                ValueAnimator a = ObjectAnimator.ofFloat(NavigationBarView.this.getBackButton(), "alpha", new float[]{0.0f, 1.0f});
                a.setStartDelay(this.mStartDelay);
                a.setDuration(this.mDuration);
                a.setInterpolator(this.mInterpolator);
                a.start();
            }
        }
    }

    public interface OnVerticalChangedListener {
        void onVerticalChanged(boolean z);
    }

    public NavigationBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (SystemProperties.get("ro.miui.build.region", "").equalsIgnoreCase("eea")) {
            this.mOpaEnable = true;
        }
        this.mDisplay = ((WindowManager) context.getSystemService("window")).getDefaultDisplay();
        Resources res = getContext().getResources();
        this.mBarSize = res.getDimensionPixelSize(R.dimen.navigation_bar_size);
        this.mVertical = false;
        this.mShowMenu = false;
        this.mTaskSwitchHelper = new NavigationBarViewTaskSwitchHelper(context);
        getIcons(res);
        this.mBarTransitions = new NavigationBarTransitions(this);
        this.mDisplayListener = new DisplayManager.DisplayListener() {
            public void onDisplayRemoved(int displayId) {
            }

            public void onDisplayChanged(int displayId) {
                if (NavigationBarView.this.mBar != null) {
                    NavigationBarView.this.mBar.updateStatusBarPading();
                }
            }

            public void onDisplayAdded(int displayId) {
            }
        };
        this.mDisplayManager = (DisplayManager) this.mContext.getSystemService("display");
        this.mConfiguration = new Configuration();
        this.mConfiguration.updateFrom(getResources().getConfiguration());
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mDisplayManager.registerDisplayListener(this.mDisplayListener, this.mHandler);
        ViewRootImpl root = getViewRootImpl();
        if (root != null) {
            ViewRootImplCompat.setDrawDuringWindowsAnimating(root);
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction("miui.intent.TAKE_SCREENSHOT");
        this.mContext.registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, filter, null, null);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("screen_key_order"), false, this.mScreenKeyOrderObserver, -1);
        this.mScreenKeyOrderObserver.onChange(false);
        postInvalidate();
        if (this.mBar != null) {
            this.mBar.updateStatusBarPading();
        }
        this.mConfigurationListener = new ConfigurationController.ConfigurationListener() {
            public void onConfigChanged(Configuration newConfig) {
            }

            public void onDensityOrFontScaleChanged() {
                NavigationBarView.this.getIcons(NavigationBarView.this.getResources());
                NavigationBarView.this.setNavigationIconHints(NavigationBarView.this.mNavigationIconHints, true);
            }
        };
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).addCallback(this.mConfigurationListener);
        KeyguardUpdateMonitor.getInstance(this.mContext).registerCallback(this.mUserSwitchCallback);
        processConfigurationChanged(getResources().getConfiguration());
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mDisplayManager.unregisterDisplayListener(this.mDisplayListener);
        this.mContext.unregisterReceiver(this.mBroadcastReceiver);
        this.mContext.getContentResolver().unregisterContentObserver(this.mScreenKeyOrderObserver);
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).removeCallback(this.mConfigurationListener);
        KeyguardUpdateMonitor.getInstance(this.mContext).removeCallback(this.mUserSwitchCallback);
    }

    static {
        sKeyIdMap.put(0, Integer.valueOf(R.id.menu));
        sKeyIdMap.put(1, Integer.valueOf(R.id.home));
        sKeyIdMap.put(2, Integer.valueOf(R.id.recent_apps));
        sKeyIdMap.put(3, Integer.valueOf(R.id.back));
    }

    /* access modifiers changed from: private */
    public void adjustKeyOrder() {
        this.mKeyOrder.clear();
        ArrayList<Integer> list = getScreenKeyOrder(this.mContext);
        for (int i = 0; i < list.size(); i++) {
            this.mKeyOrder.add(sKeyIdMap.get(list.get(i)));
        }
        for (int i2 = 0; i2 < this.mRotatedViews.length; i2++) {
            ViewGroup viewGroup = (ViewGroup) this.mRotatedViews[i2].findViewById(R.id.nav_buttons);
            boolean z = true;
            if (!(i2 == 1 || i2 == 3)) {
                z = false;
            }
            adjustKeyOrder(viewGroup, z);
        }
    }

    private void adjustKeyOrder(ViewGroup view, boolean revert) {
        int i;
        Log.d("PhoneStatusBar/NavigationBarView", "adjustKeyOrder");
        LinkedList<Integer> positions = new LinkedList<>();
        HashMap<Integer, View> keyViews = new HashMap<>();
        int i2 = view.getChildCount();
        while (true) {
            i2--;
            if (i2 < 0) {
                break;
            }
            View child = view.getChildAt(i2);
            if (this.mKeyOrder.contains(Integer.valueOf(child.getId()))) {
                positions.add(0, Integer.valueOf(i2));
                view.removeView(child);
                keyViews.put(Integer.valueOf(child.getId()), child);
            }
        }
        int count = this.mKeyOrder.size();
        for (i = 0; i < count; i++) {
            View key = keyViews.get(this.mKeyOrder.get(revert ? (count - 1) - i : i));
            if (key != null) {
                view.addView(key, positions.removeFirst().intValue());
            }
        }
    }

    public static ArrayList<Integer> getScreenKeyOrder(Context context) {
        ArrayList<Integer> result = new ArrayList<>();
        String keyList = Settings.System.getStringForUser(context.getContentResolver(), "screen_key_order", -2);
        if (!TextUtils.isEmpty(keyList)) {
            String[] keys = keyList.split(" ");
            int i = 0;
            while (i < keys.length) {
                try {
                    int id = Integer.valueOf(keys[i]).intValue();
                    if (MiuiSettings.System.screenKeys.contains(Integer.valueOf(id))) {
                        result.add(Integer.valueOf(id));
                    }
                    i++;
                } catch (Exception e) {
                    result.clear();
                }
            }
        }
        Iterator it = MiuiSettings.System.screenKeys.iterator();
        while (it.hasNext()) {
            Integer id2 = (Integer) it.next();
            if (!result.contains(id2)) {
                result.add(id2);
            }
        }
        return result;
    }

    public boolean isForceImmersive() {
        return false;
    }

    public BarTransitions getBarTransitions() {
        return this.mBarTransitions;
    }

    public void setBar(StatusBar statusBar) {
        this.mBar = statusBar;
        this.mTaskSwitchHelper.setBar(statusBar);
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (this.mTaskSwitchHelper.onTouchEvent(event)) {
            return true;
        }
        if (this.mDeadZone != null && event.getAction() == 4) {
            this.mDeadZone.poke(event);
        }
        return super.onTouchEvent(event);
    }

    public boolean onInterceptTouchEvent(MotionEvent event) {
        return this.mTaskSwitchHelper.onInterceptTouchEvent(event);
    }

    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == 1 || event.getAction() == 3) {
            this.mBar.resumeSuspendedNavBarAutohide();
        } else if (event.getAction() == 0) {
            this.mBar.suspendNavBarAutohide();
        }
        return super.dispatchTouchEvent(event);
    }

    public View getCurrentView() {
        return this.mCurrentView;
    }

    public View getRecentsButton() {
        return this.mCurrentView.findViewById(R.id.recent_apps);
    }

    public View getMenuButton() {
        return this.mCurrentView.findViewById(R.id.menu);
    }

    public View getBackButton() {
        return this.mCurrentView.findViewById(R.id.back);
    }

    public KeyButtonView getHomeButton() {
        return (KeyButtonView) this.mCurrentView.findViewById(R.id.home);
    }

    public View getImeSwitchButton() {
        return this.mCurrentView.findViewById(R.id.ime_switcher);
    }

    public View getAspectButton() {
        return this.mCurrentView.findViewById(R.id.aspect);
    }

    public boolean isOverviewEnabled() {
        return (this.mDisabledFlags & 16777216) == 0;
    }

    /* access modifiers changed from: private */
    public void getIcons(Resources res) {
        this.mBackIcon = res.getDrawable(R.drawable.ic_sysbar_back);
        this.mBackIcon.setAutoMirrored(true);
        this.mBackLandIcon = this.mBackIcon;
        this.mBackAltIcon = res.getDrawable(R.drawable.ic_sysbar_back_ime);
        this.mBackAltLandIcon = this.mBackAltIcon;
        this.mRecentIcon = res.getDrawable(R.drawable.ic_sysbar_recent);
        this.mRecentLandIcon = this.mRecentIcon;
        this.mHomeIcon = res.getDrawable(R.drawable.ic_sysbar_home);
        this.mDarkSuit = new DrawableSuit.Builder(this.mContext).setBack(R.drawable.ic_sysbar_back_darkmode).setBackAlt(R.drawable.ic_sysbar_back_ime_darkmode).setHome(R.drawable.ic_sysbar_home_darkmode).setRecent(R.drawable.ic_sysbar_recent_darkmode).setBgPort(R.drawable.ic_sysbar_bg_darkmode).setBgPortCTS(R.drawable.ic_sysbar_bg_darkmode_cts).setBgLand(R.drawable.ic_sysbar_bg_land_darkmode).setBgLandCTS(R.drawable.ic_sysbar_bg_land_darkmode_cts).setBgColorRes(R.color.nav_bar_bakcground_color_darkmode).build();
        this.mLightSuit = new DrawableSuit.Builder(this.mContext).build();
    }

    public void setLayoutDirection(int layoutDirection) {
        getIcons(getContext().getResources());
        super.setLayoutDirection(layoutDirection);
        if (getBackButton() != null) {
            getBackButton().invalidate();
        }
    }

    public void setNavigationIconHints(int hints, boolean force) {
        Drawable drawable;
        if (force || hints != this.mNavigationIconHints) {
            boolean backAlt = (hints & 1) != 0;
            if ((this.mNavigationIconHints & 1) != 0 && !backAlt) {
                this.mTransitionListener.onBackAltCleared();
            }
            this.mNavigationIconHints = hints;
            ImageView imageView = (ImageView) getBackButton();
            if (backAlt) {
                drawable = this.mVertical ? this.mBackAltLandIcon : this.mBackAltIcon;
            } else {
                drawable = this.mVertical ? this.mBackLandIcon : this.mBackIcon;
            }
            imageView.setImageDrawable(drawable);
            ((ImageView) getRecentsButton()).setImageDrawable(this.mVertical ? this.mRecentLandIcon : this.mRecentIcon);
            if (!this.mOpaEnable) {
                getHomeButton().setImageDrawable(this.mHomeIcon);
            }
            if ((hints & 2) != 0) {
            }
            getImeSwitchButton().setVisibility(4);
            setMenuVisibility(this.mShowMenu, true);
            setAspectVisibility(this.mShowAspect, true);
            setDisabledFlags(this.mDisabledFlags, true);
        }
    }

    public void disableChangeBg(boolean disable) {
        this.mBarTransitions.disableChangeBg(disable);
    }

    public void setDisabledFlags(int disabledFlags) {
        setDisabledFlags(disabledFlags, false);
    }

    public void setDisabledFlags(int disabledFlags, boolean force) {
        if (force || this.mDisabledFlags != disabledFlags) {
            this.mDisabledFlags = disabledFlags;
            int i = 0;
            boolean disableHome = (2097152 & disabledFlags) != 0;
            boolean disableRecent = !isOverviewEnabled();
            boolean disableBack = (4194304 & disabledFlags) != 0 && (this.mNavigationIconHints & 1) == 0;
            if ((33554432 & disabledFlags) != 0) {
            }
            boolean darkMode = (disabledFlags & 512) != 0;
            int mode = this.mBarTransitions.getMode();
            if ((darkMode && (mode == 4 || mode == 6)) || mode == 0 || mode == 3) {
                switchSuit(this.mDarkSuit, true);
            } else {
                switchSuit(this.mLightSuit, false);
            }
            setSlippery(disableHome && disableRecent && disableBack);
            setNotTouchable(disableHome && disableRecent && disableBack);
            ViewGroup navButtons = (ViewGroup) this.mCurrentView.findViewById(R.id.nav_buttons);
            if (navButtons != null) {
                LayoutTransition lt = navButtons.getLayoutTransition();
                if (lt != null && !lt.getTransitionListeners().contains(this.mTransitionListener)) {
                    lt.addTransitionListener(this.mTransitionListener);
                }
            }
            if (Build.VERSION.SDK_INT >= 28) {
                if (isScreenPinningActive()) {
                    disableRecent = false;
                }
            } else if (inLockTask() && disableRecent && !disableHome) {
                disableRecent = false;
            }
            Log.d("PhoneStatusBar/NavigationBarView", "setDisabledFlags back:" + disableBack + " home:" + disableHome + " recent:" + disableRecent);
            setLayoutTransitionsEnabled(false);
            getBackButton().setVisibility(disableBack ? 4 : 0);
            if (!this.mOpaEnable) {
                getHomeButton().setVisibility(disableHome ? 4 : 0);
            } else {
                ((OpaLayout) this.mCurrentView.findViewById(R.id.home_layout)).setVisibility(disableHome ? 4 : 0);
            }
            View recentsButton = getRecentsButton();
            if (disableRecent) {
                i = 4;
            }
            recentsButton.setVisibility(i);
            setAlpha((this.mForceHide || (disableBack && disableHome && disableRecent)) ? 0.0f : 1.0f);
            setLayoutTransitionsEnabled(true);
        }
    }

    public void switchSuit(DrawableSuit suit, boolean isDarkMode) {
        Drawable drawable;
        Drawable drawable2 = suit.mBack;
        this.mBackLandIcon = drawable2;
        this.mBackIcon = drawable2;
        this.mBackIcon.setAutoMirrored(true);
        Drawable drawable3 = suit.mBackAlt;
        this.mBackAltLandIcon = drawable3;
        this.mBackAltIcon = drawable3;
        Drawable drawable4 = suit.mRecent;
        this.mRecentLandIcon = drawable4;
        this.mRecentIcon = drawable4;
        this.mHomeIcon = suit.mHome;
        boolean backAlt = (this.mNavigationIconHints & 1) != 0;
        ImageView imageView = (ImageView) getBackButton();
        if (backAlt) {
            drawable = this.mVertical ? this.mBackAltLandIcon : this.mBackAltIcon;
        } else {
            drawable = this.mVertical ? this.mBackLandIcon : this.mBackIcon;
        }
        updateIcon(imageView, drawable, isDarkMode);
        updateIcon((ImageView) getRecentsButton(), this.mVertical ? this.mRecentLandIcon : this.mRecentIcon, isDarkMode);
        if (!this.mOpaEnable) {
            updateIcon(getHomeButton(), this.mHomeIcon, isDarkMode);
        } else {
            ((OpaLayout) this.mCurrentView.findViewById(R.id.home_layout)).setDarkIntensity(isDarkMode ? 1.0f : 0.0f);
        }
        boolean isCTSValid = isDarkMode && Util.showCtsSpecifiedColor();
        this.mRotatedViews[0].setBackground(isCTSValid ? suit.mBgPortCTS : suit.mBgPort);
        this.mRotatedViews[1].setBackground(isCTSValid ? suit.mBgLandCTS : suit.mBgLand);
        this.mBarTransitions.setForceBgColor(suit.mBgColor);
    }

    private void updateIcon(ImageView icon, Drawable drawable, boolean isDarkMode) {
        if (icon != null && drawable != null) {
            if (!isDarkMode || !Util.showCtsSpecifiedColor()) {
                drawable.setColorFilter(null);
            } else {
                if (sFilterColor == 0) {
                    sFilterColor = this.mContext.getResources().getColor(R.color.status_bar_icon_text_color_dark_mode_cts);
                }
                drawable.setColorFilter(sFilterColor, PorterDuff.Mode.SRC_IN);
            }
            icon.setImageDrawable(drawable);
        }
    }

    private boolean inLockTask() {
        try {
            return ActivityManagerNative.getDefault().isInLockTaskMode();
        } catch (RemoteException e) {
            return false;
        }
    }

    private boolean isScreenPinningActive() {
        boolean z = false;
        try {
            if (ActivityManagerNative.getDefault().getLockTaskModeState() == 2) {
                z = true;
            }
            return z;
        } catch (RemoteException e) {
            return false;
        }
    }

    public void setLayoutTransitionsEnabled(boolean enabled) {
        this.mLayoutTransitionsEnabled = enabled;
        updateLayoutTransitionsEnabled();
    }

    public void setWakeAndUnlocking(boolean wakeAndUnlocking) {
        setUseFadingAnimations(wakeAndUnlocking);
        this.mWakeAndUnlocking = wakeAndUnlocking;
        updateLayoutTransitionsEnabled();
    }

    private void updateLayoutTransitionsEnabled() {
        boolean enabled = !this.mWakeAndUnlocking && this.mLayoutTransitionsEnabled;
        LayoutTransition lt = ((ViewGroup) this.mCurrentView.findViewById(R.id.nav_buttons)).getLayoutTransition();
        if (lt == null) {
            return;
        }
        if (enabled) {
            lt.enableTransitionType(2);
            lt.enableTransitionType(3);
            lt.enableTransitionType(0);
            lt.enableTransitionType(1);
            return;
        }
        lt.disableTransitionType(2);
        lt.disableTransitionType(3);
        lt.disableTransitionType(0);
        lt.disableTransitionType(1);
    }

    private void setUseFadingAnimations(boolean useFadingAnimations) {
        WindowManager.LayoutParams lp = (WindowManager.LayoutParams) getLayoutParams();
        if (lp != null) {
            boolean old = lp.windowAnimations != 0;
            if (!old && useFadingAnimations) {
                lp.windowAnimations = com.android.systemui.plugins.R.style.Animation_NavigationBarFadeIn;
            } else if (old && !useFadingAnimations) {
                lp.windowAnimations = 0;
            } else {
                return;
            }
            WindowManager wm = (WindowManager) getContext().getSystemService("window");
            if (isAttachedToWindow()) {
                wm.updateViewLayout(this, lp);
            }
        }
    }

    public void setSlippery(boolean newSlippery) {
        WindowManager.LayoutParams lp = (WindowManager.LayoutParams) getLayoutParams();
        if (lp != null) {
            boolean oldSlippery = (lp.flags & 536870912) != 0;
            if (!oldSlippery && newSlippery) {
                lp.flags = 536870912 | lp.flags;
            } else if (oldSlippery && !newSlippery) {
                lp.flags &= -536870913;
            } else {
                return;
            }
            WindowManager wm = (WindowManager) getContext().getSystemService("window");
            if (isAttachedToWindow()) {
                wm.updateViewLayout(this, lp);
            }
        }
    }

    private void setNotTouchable(boolean newNotTouchable) {
        WindowManager.LayoutParams lp = (WindowManager.LayoutParams) getLayoutParams();
        if (lp != null && isAttachedToWindow()) {
            boolean oldNotTouchable = (lp.flags & 16) != 0;
            if (!oldNotTouchable && newNotTouchable) {
                lp.flags |= 16;
            } else if (oldNotTouchable && !newNotTouchable) {
                lp.flags &= -17;
            } else {
                return;
            }
            ((WindowManager) getContext().getSystemService("window")).updateViewLayout(this, lp);
        }
    }

    public void setMenuVisibility(boolean show, boolean force) {
        if (force || this.mShowMenu != show) {
            this.mShowMenu = show;
            if (!this.mShowMenu || (this.mNavigationIconHints & 2) != 0) {
            }
            getMenuButton().setVisibility(4);
            postInvalidate();
            if (this.mBar != null) {
                this.mBar.updateStatusBarPading();
            }
        }
    }

    public void setAspectVisibility(boolean show) {
        setAspectVisibility(show, false);
    }

    public void setAspectVisibility(boolean show, boolean force) {
        if (force || this.mShowAspect != show) {
            this.mShowAspect = show;
            getAspectButton().setVisibility(this.mShowAspect ? 0 : 4);
        }
    }

    public void onFinishInflate() {
        View[] viewArr = this.mRotatedViews;
        View[] viewArr2 = this.mRotatedViews;
        View findViewById = findViewById(R.id.rot0);
        viewArr2[2] = findViewById;
        viewArr[0] = findViewById;
        this.mRotatedViews[1] = findViewById(R.id.rot90);
        this.mRotatedViews[3] = this.mRotatedViews[1];
        this.mCurrentView = this.mRotatedViews[0];
        getImeSwitchButton().setOnClickListener(this.mImeSwitcherClickListener);
        getAspectButton().setOnClickListener(this.mAspectClickListener);
        updateRTLOrder();
    }

    public void reorient() {
        int rot = this.mDisplay.getRotation();
        boolean z = false;
        for (int i = 0; i < 4; i++) {
            this.mRotatedViews[i].setVisibility(8);
        }
        this.mCurrentView = this.mRotatedViews[rot];
        this.mCurrentView.setVisibility(0);
        updateLayoutTransitionsEnabled();
        getImeSwitchButton().setOnClickListener(this.mImeSwitcherClickListener);
        getAspectButton().setOnClickListener(this.mAspectClickListener);
        this.mDeadZone = (DeadZone) this.mCurrentView.findViewById(R.id.deadzone);
        this.mBarTransitions.init();
        setDisabledFlags(this.mDisabledFlags, true);
        setMenuVisibility(this.mShowMenu, true);
        setAspectVisibility(this.mShowAspect, true);
        updateTaskSwitchHelper();
        setNavigationIconHints(this.mNavigationIconHints, true);
        if (this.mOpaEnable && !miui.os.Build.IS_TABLET) {
            OpaLayout opaLayout = (OpaLayout) this.mCurrentView.findViewById(R.id.home_layout);
            if (this.mDisplay.getRotation() == 1 || this.mDisplay.getRotation() == 3) {
                z = true;
            }
            opaLayout.setVertical(z);
        }
    }

    private void updateTaskSwitchHelper() {
        boolean isRtl = true;
        if (getLayoutDirection() != 1) {
            isRtl = false;
        }
        this.mTaskSwitchHelper.setBarState(this.mVertical, isRtl);
    }

    /* access modifiers changed from: protected */
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        boolean newVertical = h > 0 && h > w;
        if (newVertical != this.mVertical) {
            this.mVertical = newVertical;
            reorient();
            notifyVerticalChangedListener(newVertical);
        }
        postCheckForInvalidLayout("sizeChanged");
        super.onSizeChanged(w, h, oldw, oldh);
    }

    private void notifyVerticalChangedListener(boolean newVertical) {
        if (this.mOnVerticalChangedListener != null) {
            this.mOnVerticalChangedListener.onVerticalChanged(newVertical);
        }
    }

    private void processConfigurationChanged(Configuration newConfig) {
        if ((this.mConfiguration.updateFrom(newConfig) & 512) == 512) {
            this.mBarTransitions.darkModeChanged();
        }
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateRTLOrder();
        updateTaskSwitchHelper();
        processConfigurationChanged(newConfig);
    }

    private void updateRTLOrder() {
        boolean isLayoutRtl = getResources().getConfiguration().getLayoutDirection() == 1;
        if (this.mIsLayoutRtl != isLayoutRtl) {
            View rotation90 = this.mRotatedViews[1];
            swapChildrenOrderIfVertical(rotation90.findViewById(R.id.nav_buttons));
            adjustExtraKeyGravity(rotation90, isLayoutRtl);
            View rotation270 = this.mRotatedViews[3];
            if (rotation90 != rotation270) {
                swapChildrenOrderIfVertical(rotation270.findViewById(R.id.nav_buttons));
                adjustExtraKeyGravity(rotation270, isLayoutRtl);
            }
            this.mIsLayoutRtl = isLayoutRtl;
        }
    }

    private void adjustExtraKeyGravity(View navBar, boolean isLayoutRtl) {
        View menu = navBar.findViewById(R.id.menu);
        View imeSwitcher = navBar.findViewById(R.id.ime_switcher);
        int i = 48;
        if (menu != null) {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) menu.getLayoutParams();
            lp.gravity = isLayoutRtl ? 80 : 48;
            menu.setLayoutParams(lp);
        }
        if (imeSwitcher != null) {
            FrameLayout.LayoutParams lp2 = (FrameLayout.LayoutParams) imeSwitcher.getLayoutParams();
            if (isLayoutRtl) {
                i = 80;
            }
            lp2.gravity = i;
            imeSwitcher.setLayoutParams(lp2);
        }
    }

    private void swapChildrenOrderIfVertical(View group) {
        if (group instanceof LinearLayout) {
            LinearLayout linearLayout = (LinearLayout) group;
            if (linearLayout.getOrientation() == 1) {
                int childCount = linearLayout.getChildCount();
                ArrayList<View> childList = new ArrayList<>(childCount);
                for (int i = 0; i < childCount; i++) {
                    childList.add(linearLayout.getChildAt(i));
                }
                linearLayout.removeAllViews();
                for (int i2 = childCount - 1; i2 >= 0; i2--) {
                    linearLayout.addView(childList.get(i2));
                }
            }
        }
    }

    private String getResourceName(int resId) {
        if (resId == 0) {
            return "(null)";
        }
        try {
            return getContext().getResources().getResourceName(resId);
        } catch (Resources.NotFoundException e) {
            return "(unknown)";
        }
    }

    private void postCheckForInvalidLayout(String how) {
        this.mHandler.obtainMessage(8686, 0, 0, how).sendToTarget();
    }

    private static String visibilityToString(int vis) {
        if (vis == 4) {
            return "INVISIBLE";
        }
        if (vis != 8) {
            return "VISIBLE";
        }
        return "GONE";
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("NavigationBarView {");
        Rect r = new Rect();
        Point size = new Point();
        this.mDisplay.getRealSize(size);
        pw.println(String.format("      this: " + StatusBar.viewInfo(this) + " " + visibilityToString(getVisibility()), new Object[0]));
        getWindowVisibleDisplayFrame(r);
        boolean offscreen = r.right > size.x || r.bottom > size.y;
        StringBuilder sb = new StringBuilder();
        sb.append("      window: ");
        sb.append(r.toShortString());
        sb.append(" ");
        sb.append(visibilityToString(getWindowVisibility()));
        sb.append(offscreen ? " OFFSCREEN!" : "");
        pw.println(sb.toString());
        pw.println(String.format("      mCurrentView: id=%s (%dx%d) %s", new Object[]{getResourceName(this.mCurrentView.getId()), Integer.valueOf(this.mCurrentView.getWidth()), Integer.valueOf(this.mCurrentView.getHeight()), visibilityToString(this.mCurrentView.getVisibility())}));
        Object[] objArr = new Object[3];
        objArr[0] = Integer.valueOf(this.mDisabledFlags);
        objArr[1] = this.mVertical ? "true" : "false";
        objArr[2] = this.mShowMenu ? "true" : "false";
        pw.println(String.format("      disabled=0x%08x vertical=%s menu=%s", objArr));
        dumpButton(pw, "back", getBackButton());
        dumpButton(pw, "home", getHomeButton());
        dumpButton(pw, "rcnt", getRecentsButton());
        dumpButton(pw, "menu", getMenuButton());
        dumpButton(pw, "aspect", getAspectButton());
        pw.println("    }");
    }

    private static void dumpButton(PrintWriter pw, String caption, View button) {
        pw.print("      " + caption + ": ");
        if (button == null) {
            pw.print("null");
        } else {
            pw.print(StatusBar.viewInfo(button) + " " + visibilityToString(button.getVisibility()) + " alpha=" + button.getAlpha());
        }
        pw.println();
    }

    public void setOpaEnabled(boolean enabled) {
        View opaLayout = this.mRotatedViews[0].findViewById(R.id.home_layout);
        if (opaLayout != null) {
            ((OpaLayout) opaLayout).setOpaEnabled(enabled);
        }
        View opaLayout90 = this.mRotatedViews[1].findViewById(R.id.home_layout);
        if (opaLayout90 != null) {
            ((OpaLayout) opaLayout90).setOpaEnabled(enabled);
        }
    }
}
