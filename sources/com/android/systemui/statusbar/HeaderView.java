package com.android.systemui.statusbar;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import com.android.keyguard.CarrierText;
import com.android.systemui.Constants;
import com.android.systemui.CustomizedUtils;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.Util;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.statusbar.phone.StatusBarIconController;
import com.android.systemui.statusbar.policy.Clock;
import com.android.systemui.statusbar.policy.DarkIconDispatcher;
import com.android.systemui.statusbar.policy.DarkIconDispatcherHelper;
import com.android.systemui.tuner.TunerService;

public class HeaderView extends RelativeLayout implements TunerService.Tunable {
    private static int SDK_INT = Build.VERSION.SDK_INT;
    /* access modifiers changed from: private */
    public ActivityStarter mActStarter;
    private LinearLayout mCarrierLayout;
    private CarrierText mCarrierText;
    private CarrierText mCarrierTextLand;
    /* access modifiers changed from: private */
    public Clock mClock;
    private int mDarkModeIconColorSingleTone;
    /* access modifiers changed from: private */
    public Clock mDateView;
    private boolean mHasMobileDataFeature;
    private StatusBarIconController.DarkIconManager mIconManager;
    /* access modifiers changed from: private */
    public int mLastOrientation;
    private boolean mLayoutChangedForCarrierInLand;
    private boolean mLayoutChangedForCarrierInPortrait;
    private int mLightModeIconColorSingleTone;
    View.OnClickListener mOnClickListener = new View.OnClickListener() {
        /* JADX WARNING: Removed duplicated region for block: B:19:0x0054 A[Catch:{ Exception -> 0x0064 }] */
        /* JADX WARNING: Removed duplicated region for block: B:24:? A[RETURN, SYNTHETIC] */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onClick(android.view.View r4) {
            /*
                r3 = this;
                r0 = 0
                com.android.systemui.statusbar.HeaderView r1 = com.android.systemui.statusbar.HeaderView.this     // Catch:{ Exception -> 0x0064 }
                com.android.systemui.statusbar.policy.Clock r1 = r1.mClock     // Catch:{ Exception -> 0x0064 }
                if (r4 == r1) goto L_0x0045
                boolean r1 = com.android.systemui.Constants.IS_TABLET     // Catch:{ Exception -> 0x0064 }
                if (r1 != 0) goto L_0x001f
                com.android.systemui.statusbar.HeaderView r1 = com.android.systemui.statusbar.HeaderView.this     // Catch:{ Exception -> 0x0064 }
                int r1 = r1.mLastOrientation     // Catch:{ Exception -> 0x0064 }
                r2 = 2
                if (r1 != r2) goto L_0x001f
                com.android.systemui.statusbar.HeaderView r1 = com.android.systemui.statusbar.HeaderView.this     // Catch:{ Exception -> 0x0064 }
                com.android.systemui.statusbar.policy.Clock r1 = r1.mDateView     // Catch:{ Exception -> 0x0064 }
                if (r4 != r1) goto L_0x001f
                goto L_0x0045
            L_0x001f:
                com.android.systemui.statusbar.HeaderView r1 = com.android.systemui.statusbar.HeaderView.this     // Catch:{ Exception -> 0x0064 }
                com.android.systemui.statusbar.policy.Clock r1 = r1.mDateView     // Catch:{ Exception -> 0x0064 }
                if (r4 != r1) goto L_0x0035
                android.content.Intent r1 = new android.content.Intent     // Catch:{ Exception -> 0x0064 }
                java.lang.String r2 = "android.intent.action.MAIN"
                r1.<init>(r2)     // Catch:{ Exception -> 0x0064 }
                r0 = r1
                java.lang.String r1 = "com.android.calendar"
                r0.setPackage(r1)     // Catch:{ Exception -> 0x0064 }
                goto L_0x0052
            L_0x0035:
                com.android.systemui.statusbar.HeaderView r1 = com.android.systemui.statusbar.HeaderView.this     // Catch:{ Exception -> 0x0064 }
                android.widget.ImageView r1 = r1.mShortcut     // Catch:{ Exception -> 0x0064 }
                if (r4 != r1) goto L_0x0052
                com.android.systemui.statusbar.HeaderView r1 = com.android.systemui.statusbar.HeaderView.this     // Catch:{ Exception -> 0x0064 }
                android.content.Intent r1 = r1.buildShortcutClickIntent()     // Catch:{ Exception -> 0x0064 }
                r0 = r1
                goto L_0x0052
            L_0x0045:
                android.content.Intent r1 = new android.content.Intent     // Catch:{ Exception -> 0x0064 }
                java.lang.String r2 = "android.intent.action.MAIN"
                r1.<init>(r2)     // Catch:{ Exception -> 0x0064 }
                r0 = r1
                java.lang.String r1 = "com.android.deskclock"
                r0.setPackage(r1)     // Catch:{ Exception -> 0x0064 }
            L_0x0052:
                if (r0 == 0) goto L_0x0063
                r1 = 268435456(0x10000000, float:2.5243549E-29)
                r0.addFlags(r1)     // Catch:{ Exception -> 0x0064 }
                com.android.systemui.statusbar.HeaderView r1 = com.android.systemui.statusbar.HeaderView.this     // Catch:{ Exception -> 0x0064 }
                com.android.systemui.plugins.ActivityStarter r1 = r1.mActStarter     // Catch:{ Exception -> 0x0064 }
                r2 = 1
                r1.startActivity(r0, r2)     // Catch:{ Exception -> 0x0064 }
            L_0x0063:
                goto L_0x0068
            L_0x0064:
                r0 = move-exception
                r0.printStackTrace()
            L_0x0068:
                return
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.HeaderView.AnonymousClass1.onClick(android.view.View):void");
        }
    };
    /* access modifiers changed from: private */
    public ImageView mShortcut;
    private int mShortcutDestination;
    private LinearLayout mStatusIcons;
    private LinearLayout mSystemIcons;
    private LinearLayout mSystemIconsArea;

    public HeaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mHasMobileDataFeature = ((ConnectivityManager) context.getSystemService("connectivity")).isNetworkSupported(0);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mDateView = (Clock) findViewById(R.id.date_time);
        int i = 1;
        this.mDateView.setClockMode(1);
        this.mDateView.setOnClickListener(this.mOnClickListener);
        this.mClock = (Clock) findViewById(R.id.big_time);
        this.mClock.setShowAmPm(false);
        this.mClock.setOnClickListener(this.mOnClickListener);
        this.mSystemIcons = (LinearLayout) findViewById(R.id.system_icons);
        this.mSystemIconsArea = (LinearLayout) findViewById(R.id.system_icon_area);
        this.mCarrierText = (CarrierText) findViewById(R.id.carrier);
        this.mCarrierTextLand = (CarrierText) findViewById(R.id.carrier_land);
        this.mCarrierLayout = (LinearLayout) findViewById(R.id.carrier_layout);
        this.mCarrierText.setShowStyle(-1);
        this.mCarrierTextLand.setShowStyle(-1);
        if (!this.mHasMobileDataFeature) {
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) this.mDateView.getLayoutParams();
            layoutParams.removeRule(6);
            this.mDateView.setLayoutParams(layoutParams);
            LinearLayout systemIcons = (LinearLayout) this.mSystemIconsArea.findViewById(R.id.signal_cluster_view);
            LinearLayout.LayoutParams layoutParams1 = (LinearLayout.LayoutParams) systemIcons.getLayoutParams();
            layoutParams1.height = -1;
            systemIcons.setLayoutParams(layoutParams1);
        }
        this.mDarkModeIconColorSingleTone = this.mContext.getColor(R.color.dark_mode_icon_color_single_tone);
        this.mLightModeIconColorSingleTone = this.mContext.getColor(R.color.light_mode_icon_color_single_tone);
        this.mStatusIcons = (LinearLayout) findViewById(R.id.statusIcons);
        this.mShortcut = (ImageView) findViewById(R.id.notification_shade_shortcut);
        this.mShortcut.setOnClickListener(this.mOnClickListener);
        if (!Constants.IS_INTERNATIONAL) {
            i = 0;
        }
        this.mShortcutDestination = i;
        this.mShortcutDestination = getShortcut();
        updateShortcut();
        this.mActStarter = (ActivityStarter) Dependency.get(ActivityStarter.class);
        CustomizedUtils.checkRegion();
        updateCarrierText(getResources().getConfiguration().orientation);
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateResources(getResources().getConfiguration());
        ((TunerService) Dependency.get(TunerService.class)).addTunable(this, "status_bar_notification_shade_shortcut");
        this.mIconManager = new StatusBarIconController.DarkIconManager(this.mStatusIcons);
        this.mIconManager.setShieldDarkReceiver(true);
        ((StatusBarIconController) Dependency.get(StatusBarIconController.class)).addIconGroup(this.mIconManager);
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ((StatusBarIconController) Dependency.get(StatusBarIconController.class)).removeIconGroup(this.mIconManager);
        ((TunerService) Dependency.get(TunerService.class)).removeTunable(this);
        this.mIconManager.destroy();
        this.mIconManager = null;
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateResources(newConfig);
    }

    public void onTuningChanged(String key, String newValue) {
        if ("status_bar_notification_shade_shortcut".equals(key)) {
            if (!TextUtils.isEmpty(newValue)) {
                try {
                    this.mShortcutDestination = Integer.parseInt(newValue);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            } else if (Constants.IS_INTERNATIONAL) {
                this.mShortcutDestination = 1;
            } else {
                this.mShortcutDestination = 0;
            }
            updateShortcut();
        }
    }

    /* access modifiers changed from: protected */
    public int getShortcut() {
        return ((TunerService) Dependency.get(TunerService.class)).getValue("status_bar_notification_shade_shortcut", this.mShortcutDestination);
    }

    private void updateResources(Configuration newConfig) {
        int newOrient = newConfig.orientation;
        if (newOrient != this.mLastOrientation && !Constants.IS_TABLET) {
            int i = 1;
            int topVis = newOrient == 1 ? 0 : 8;
            this.mClock.setVisibility(topVis);
            this.mShortcut.setVisibility(topVis);
            updateCarrierText(newOrient);
            Clock clock = this.mDateView;
            if (newOrient != 1) {
                i = 2;
            }
            clock.setClockMode(i);
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) getLayoutParams();
            lp.bottomMargin = getResources().getDimensionPixelSize(R.dimen.expanded_notification_header_bottom);
            setLayoutParams(lp);
            this.mLastOrientation = newOrient;
        }
    }

    private void updateCarrierText(int newOrient) {
        if (CustomizedUtils.isCarrierInHeaderViewShown()) {
            if (newOrient == 1) {
                this.mLayoutChangedForCarrierInPortrait = true;
                this.mCarrierText.setShowStyle(1);
                this.mCarrierTextLand.setShowStyle(-1);
                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) this.mDateView.getLayoutParams();
                layoutParams.removeRule(12);
                layoutParams.removeRule(6);
                layoutParams.addRule(8, R.id.notification_shade_shortcut);
                this.mDateView.setLayoutParams(layoutParams);
                RelativeLayout.LayoutParams layoutParams2 = (RelativeLayout.LayoutParams) this.mCarrierLayout.getLayoutParams();
                layoutParams2.addRule(6, R.id.system_icon_area);
                this.mCarrierLayout.setLayoutParams(layoutParams2);
                RelativeLayout.LayoutParams layoutParams3 = (RelativeLayout.LayoutParams) this.mSystemIconsArea.getLayoutParams();
                layoutParams3.addRule(17, R.id.carrier_layout);
                this.mSystemIconsArea.setLayoutParams(layoutParams3);
                return;
            }
            this.mLayoutChangedForCarrierInLand = true;
            this.mCarrierText.setShowStyle(-1);
            this.mCarrierTextLand.setShowStyle(1);
            RelativeLayout.LayoutParams layoutParams4 = (RelativeLayout.LayoutParams) this.mCarrierLayout.getLayoutParams();
            layoutParams4.removeRule(6);
            this.mCarrierLayout.setLayoutParams(layoutParams4);
            RelativeLayout.LayoutParams layoutParams5 = (RelativeLayout.LayoutParams) this.mDateView.getLayoutParams();
            layoutParams5.removeRule(8);
            layoutParams5.addRule(12);
            layoutParams5.addRule(6, R.id.system_icon_area);
            this.mDateView.setLayoutParams(layoutParams5);
            RelativeLayout.LayoutParams layoutParams6 = (RelativeLayout.LayoutParams) this.mSystemIconsArea.getLayoutParams();
            layoutParams6.addRule(17, R.id.carrier_land_layout);
            this.mSystemIconsArea.setLayoutParams(layoutParams6);
        } else if (SDK_INT >= 24) {
        } else {
            if (this.mLayoutChangedForCarrierInPortrait || this.mLayoutChangedForCarrierInLand) {
                this.mCarrierText.setShowStyle(-1);
                this.mCarrierTextLand.setShowStyle(-1);
                if (this.mLayoutChangedForCarrierInPortrait && newOrient == 1) {
                    this.mLayoutChangedForCarrierInPortrait = false;
                    RelativeLayout.LayoutParams layoutParams7 = (RelativeLayout.LayoutParams) this.mDateView.getLayoutParams();
                    layoutParams7.addRule(12);
                    layoutParams7.addRule(6, R.id.system_icon_area);
                    layoutParams7.removeRule(8);
                    this.mDateView.setLayoutParams(layoutParams7);
                    RelativeLayout.LayoutParams layoutParams8 = (RelativeLayout.LayoutParams) this.mCarrierLayout.getLayoutParams();
                    layoutParams8.removeRule(6);
                    this.mCarrierLayout.setLayoutParams(layoutParams8);
                    RelativeLayout.LayoutParams layoutParams9 = (RelativeLayout.LayoutParams) this.mSystemIconsArea.getLayoutParams();
                    layoutParams9.removeRule(17);
                    this.mSystemIconsArea.setLayoutParams(layoutParams9);
                } else if (this.mLayoutChangedForCarrierInLand && newOrient == 2) {
                    this.mLayoutChangedForCarrierInLand = false;
                    RelativeLayout.LayoutParams layoutParams10 = (RelativeLayout.LayoutParams) this.mCarrierLayout.getLayoutParams();
                    layoutParams10.removeRule(6);
                    this.mCarrierLayout.setLayoutParams(layoutParams10);
                    RelativeLayout.LayoutParams layoutParams11 = (RelativeLayout.LayoutParams) this.mDateView.getLayoutParams();
                    layoutParams11.removeRule(12);
                    layoutParams11.removeRule(6);
                    layoutParams11.addRule(8);
                    this.mDateView.setLayoutParams(layoutParams11);
                    RelativeLayout.LayoutParams layoutParams12 = (RelativeLayout.LayoutParams) this.mSystemIconsArea.getLayoutParams();
                    layoutParams12.removeRule(17);
                    this.mSystemIconsArea.setLayoutParams(layoutParams12);
                }
            }
        }
    }

    public void updateShortcut() {
        switch (this.mShortcutDestination) {
            case 0:
                this.mShortcut.setImageResource(R.drawable.notch_quicksearch);
                this.mShortcut.setContentDescription(getResources().getString(R.string.accessibility_search_light));
                return;
            case 1:
                this.mShortcut.setImageResource(R.drawable.notch_settings);
                this.mShortcut.setContentDescription(getResources().getString(R.string.accessibility_settings));
                return;
            default:
                return;
        }
    }

    public void themeChanged() {
        boolean isDark = getContext().getResources().getBoolean(R.bool.expanded_status_bar_darkmode);
        float darkIntensity = isDark ? 1.0f : 0.0f;
        Rect area = new Rect(0, 0, 0, 0);
        int tint = isDark ? this.mDarkModeIconColorSingleTone : this.mLightModeIconColorSingleTone;
        for (int i = 0; i < this.mSystemIcons.getChildCount(); i++) {
            View view = this.mSystemIcons.getChildAt(i);
            if (view instanceof DarkIconDispatcher.DarkReceiver) {
                ((DarkIconDispatcher.DarkReceiver) view).onDarkChanged(area, darkIntensity, tint);
            }
        }
        if (this.mIconManager != null) {
            this.mIconManager.setDarkIntensity(area, darkIntensity, tint);
        }
        for (int i2 = 0; i2 < this.mStatusIcons.getChildCount(); i2++) {
            if (this.mStatusIcons.getChildAt(i2) instanceof StatusBarIconView) {
                StatusBarIconView iconView = (StatusBarIconView) this.mStatusIcons.getChildAt(i2);
                iconView.setImageResource(Icons.get(Integer.valueOf(iconView.getStatusBarIcon().icon.getResId()), DarkIconDispatcherHelper.inDarkMode(area, iconView, darkIntensity)));
            }
        }
    }

    public void regionChanged() {
        CustomizedUtils.checkRegion();
        updateCarrierText(this.mLastOrientation);
    }

    public boolean onInterceptTouchEvent(MotionEvent event) {
        return isEnabled() && super.onInterceptTouchEvent(event);
    }

    public boolean dispatchTouchEvent(MotionEvent event) {
        super.dispatchTouchEvent(event);
        return isEnabled();
    }

    public boolean hasOverlappingRendering() {
        return false;
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        this.mDateView.setEnabled(enabled);
    }

    /* access modifiers changed from: private */
    public Intent buildShortcutClickIntent() {
        switch (this.mShortcutDestination) {
            case 0:
                if (!Constants.IS_INTERNATIONAL) {
                    Intent intent = new Intent("android.intent.action.SEARCH");
                    intent.setPackage("com.android.quicksearchbox");
                    intent.setData(Uri.parse("qsb://query?close_web_page=true&ref=systemui10"));
                    return intent;
                } else if (!Util.isBrowserSearchExist(getContext())) {
                    return new Intent("android.intent.action.WEB_SEARCH");
                } else {
                    Intent intent2 = new Intent("com.android.browser.browser_search");
                    intent2.setPackage("com.android.browser");
                    return intent2;
                }
            case 1:
                return new Intent("android.settings.SETTINGS");
            default:
                return null;
        }
    }
}
