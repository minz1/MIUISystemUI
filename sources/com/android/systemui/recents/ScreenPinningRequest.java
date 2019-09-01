package com.android.systemui.recents;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.ActivityManager;
import android.app.ActivityManagerCompat;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.RemoteException;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.systemui.R;
import java.util.ArrayList;

public class ScreenPinningRequest implements View.OnClickListener {
    /* access modifiers changed from: private */
    public final AccessibilityManager mAccessibilityService = ((AccessibilityManager) this.mContext.getSystemService("accessibility"));
    private final Context mContext;
    private RequestWindowView mRequestWindow;
    /* access modifiers changed from: private */
    public final WindowManager mWindowManager = ((WindowManager) this.mContext.getSystemService("window"));
    private int taskId;

    private class RequestWindowView extends FrameLayout {
        /* access modifiers changed from: private */
        public final ColorDrawable mColor = new ColorDrawable(0);
        private ValueAnimator mColorAnim;
        /* access modifiers changed from: private */
        public ViewGroup mLayout;
        private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("android.intent.action.CONFIGURATION_CHANGED")) {
                    RequestWindowView.this.post(RequestWindowView.this.mUpdateLayoutRunnable);
                } else if (intent.getAction().equals("android.intent.action.USER_SWITCHED") || intent.getAction().equals("android.intent.action.SCREEN_OFF")) {
                    ScreenPinningRequest.this.clearPrompt();
                }
            }
        };
        private boolean mShowCancel;
        /* access modifiers changed from: private */
        public final Runnable mUpdateLayoutRunnable = new Runnable() {
            public void run() {
                if (RequestWindowView.this.mLayout != null && RequestWindowView.this.mLayout.getParent() != null) {
                    RequestWindowView.this.mLayout.setLayoutParams(ScreenPinningRequest.this.getRequestLayoutParams(RequestWindowView.this.isLandscapePhone(RequestWindowView.this.mContext)));
                }
            }
        };

        public RequestWindowView(Context context, boolean showCancel) {
            super(context);
            setClickable(true);
            setOnClickListener(ScreenPinningRequest.this);
            setBackground(this.mColor);
            this.mShowCancel = showCancel;
        }

        public void onAttachedToWindow() {
            DisplayMetrics metrics = new DisplayMetrics();
            ScreenPinningRequest.this.mWindowManager.getDefaultDisplay().getMetrics(metrics);
            float density = metrics.density;
            boolean isLandscape = isLandscapePhone(this.mContext);
            inflateView(isLandscape);
            int bgColor = this.mContext.getColor(R.color.screen_pinning_request_window_bg);
            if (ActivityManager.isHighEndGfx()) {
                this.mLayout.setAlpha(0.0f);
                if (isLandscape) {
                    this.mLayout.setTranslationX(96.0f * density);
                } else {
                    this.mLayout.setTranslationY(96.0f * density);
                }
                this.mLayout.animate().alpha(1.0f).translationX(0.0f).translationY(0.0f).setDuration(300).setInterpolator(new DecelerateInterpolator()).start();
                this.mColorAnim = ValueAnimator.ofObject(new ArgbEvaluator(), new Object[]{0, Integer.valueOf(bgColor)});
                this.mColorAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    public void onAnimationUpdate(ValueAnimator animation) {
                        RequestWindowView.this.mColor.setColor(((Integer) animation.getAnimatedValue()).intValue());
                    }
                });
                this.mColorAnim.setDuration(1000);
                this.mColorAnim.start();
            } else {
                this.mColor.setColor(bgColor);
            }
            IntentFilter filter = new IntentFilter("android.intent.action.CONFIGURATION_CHANGED");
            filter.addAction("android.intent.action.USER_SWITCHED");
            filter.addAction("android.intent.action.SCREEN_OFF");
            this.mContext.registerReceiver(this.mReceiver, filter);
        }

        /* access modifiers changed from: private */
        public boolean isLandscapePhone(Context context) {
            Configuration config = this.mContext.getResources().getConfiguration();
            return config.orientation == 2 && config.smallestScreenWidthDp < 600;
        }

        private void inflateView(boolean isLandscape) {
            this.mLayout = (ViewGroup) View.inflate(getContext(), isLandscape ? R.layout.screen_pinning_request_land_phone : R.layout.screen_pinning_request, null);
            this.mLayout.setClickable(true);
            int backBgVisibility = 0;
            this.mLayout.setLayoutDirection(0);
            this.mLayout.findViewById(R.id.screen_pinning_text_area).setLayoutDirection(3);
            View buttons = this.mLayout.findViewById(R.id.screen_pinning_buttons);
            if (Recents.getSystemServices().hasSoftNavigationBar()) {
                buttons.setLayoutDirection(3);
                swapChildrenIfRtlAndVertical(buttons);
            } else {
                buttons.setVisibility(8);
            }
            ((Button) this.mLayout.findViewById(R.id.screen_pinning_ok_button)).setOnClickListener(ScreenPinningRequest.this);
            if (this.mShowCancel) {
                ((Button) this.mLayout.findViewById(R.id.screen_pinning_cancel_button)).setOnClickListener(ScreenPinningRequest.this);
            } else {
                ((Button) this.mLayout.findViewById(R.id.screen_pinning_cancel_button)).setVisibility(4);
            }
            ((TextView) this.mLayout.findViewById(R.id.screen_pinning_description)).setText(R.string.screen_pinning_description);
            if (ScreenPinningRequest.this.mAccessibilityService.isEnabled()) {
                backBgVisibility = 4;
            }
            this.mLayout.findViewById(R.id.screen_pinning_back_bg).setVisibility(backBgVisibility);
            this.mLayout.findViewById(R.id.screen_pinning_back_bg_light).setVisibility(backBgVisibility);
            addView(this.mLayout, ScreenPinningRequest.this.getRequestLayoutParams(isLandscape));
        }

        private void swapChildrenIfRtlAndVertical(View group) {
            if (this.mContext.getResources().getConfiguration().getLayoutDirection() == 1) {
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

        public void onDetachedFromWindow() {
            this.mContext.unregisterReceiver(this.mReceiver);
        }

        /* access modifiers changed from: protected */
        public void onConfigurationChanged() {
            removeAllViews();
            inflateView(isLandscapePhone(this.mContext));
        }
    }

    public ScreenPinningRequest(Context context) {
        this.mContext = context;
    }

    public void clearPrompt() {
        if (this.mRequestWindow != null) {
            this.mWindowManager.removeView(this.mRequestWindow);
            this.mRequestWindow = null;
        }
    }

    public void showPrompt(int taskId2, boolean allowCancel) {
        try {
            clearPrompt();
        } catch (IllegalArgumentException e) {
        }
        this.taskId = taskId2;
        this.mRequestWindow = new RequestWindowView(this.mContext, allowCancel);
        this.mRequestWindow.setSystemUiVisibility(256);
        this.mWindowManager.addView(this.mRequestWindow, getWindowLayoutParams());
    }

    public void onConfigurationChanged() {
        if (this.mRequestWindow != null) {
            this.mRequestWindow.onConfigurationChanged();
        }
    }

    private WindowManager.LayoutParams getWindowLayoutParams() {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(-1, -1, 2024, 16777480, -3);
        lp.privateFlags |= 16;
        lp.setTitle("ScreenPinningConfirmation");
        lp.gravity = 119;
        return lp;
    }

    public void onClick(View v) {
        if (v.getId() == R.id.screen_pinning_ok_button || this.mRequestWindow == v) {
            try {
                ActivityManagerCompat.startSystemLockTaskMode(this.taskId);
            } catch (RemoteException e) {
            }
        }
        clearPrompt();
    }

    public FrameLayout.LayoutParams getRequestLayoutParams(boolean isLandscape) {
        int i;
        if (isLandscape) {
            i = 21;
        } else {
            i = 81;
        }
        return new FrameLayout.LayoutParams(-2, -2, i);
    }
}
