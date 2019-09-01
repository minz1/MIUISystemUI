package com.android.keyguard.charge;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Slog;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.android.keyguard.Ease;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.keyguard.MiuiKeyguardUtils;
import com.android.keyguard.fod.MiuiGxzwManager;
import com.android.keyguard.wallpaper.KeyguardWallpaperUtils;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;
import miui.date.DateUtils;
import miui.maml.animation.interpolater.CubicEaseOutInterpolater;
import miui.util.FeatureParser;
import miui.util.ScreenshotUtils;

public class MiuiKeyguardChargingContainer extends RelativeLayout {
    /* access modifiers changed from: private */
    public int mBatteryLevel;
    private Handler mBgHandler;
    /* access modifiers changed from: private */
    public ImageView mBgView;
    private ObjectAnimator mBottomButtonClickAnimator;
    private boolean mCanShowGxzw;
    /* access modifiers changed from: private */
    public Handler mChargeHandler;
    private Runnable mChargeInfoRunnable;
    /* access modifiers changed from: private */
    public boolean mChargingAnimationInDeclining;
    /* access modifiers changed from: private */
    public View mChargingCircleView;
    /* access modifiers changed from: private */
    public TextView mChargingHintView;
    private ImageView mChargingInfoBackArrow;
    /* access modifiers changed from: private */
    public View mChargingListAndBackArrow;
    private int mChargingListTopMargin;
    /* access modifiers changed from: private */
    public TextView mChargingTip;
    /* access modifiers changed from: private */
    public int[] mChargingTips;
    /* access modifiers changed from: private */
    public MiuiKeyguardChargingView mChargingView;
    private int mChargingViewBottomMarginDown;
    private int mChargingViewBottomMarginUp;
    private int mChargingViewHeight;
    private int mChargingViewHeightAfterScale;
    private int mChargingViewTopAfterScale;
    /* access modifiers changed from: private */
    public Context mContext;
    private String mCountry;
    private int mDensityDpi;
    private AnimatorSet mDownAnimator;
    /* access modifiers changed from: private */
    public TextView mDrainedPowerPercent;
    private AnimatorSet mEnterAnimator;
    private float mFontScale;
    /* access modifiers changed from: private */
    public int mHeight;
    private int mInitY;
    /* access modifiers changed from: private */
    public boolean mIsBottomButtonAnimating;
    boolean mIsTempHigh;
    private KeyguardUpdateMonitorCallback mKeyguardUpdateMonitorCallback;
    private String mLanguage;
    /* access modifiers changed from: private */
    public KeyguardUpdateMonitor.BatteryStatus mLastBatteryStatus;
    /* access modifiers changed from: private */
    public TextView mLastChargeTime;
    private int mLayoutDirection;
    /* access modifiers changed from: private */
    public boolean mNeedRepositionDevice;
    /* access modifiers changed from: private */
    public Resources mResources;
    private int mScreenHeight;
    private AnimatorSet mUpAnimator;
    /* access modifiers changed from: private */
    public TextView mUsedTime;

    public MiuiKeyguardChargingContainer(Context context) {
        this(context, null);
    }

    public MiuiKeyguardChargingContainer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MiuiKeyguardChargingContainer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mChargingAnimationInDeclining = false;
        this.mCanShowGxzw = true;
        this.mLanguage = Locale.getDefault().getLanguage();
        this.mCountry = Locale.getDefault().getCountry();
        this.mKeyguardUpdateMonitorCallback = new KeyguardUpdateMonitorCallback() {
            public void onBottomAreaButtonClicked(boolean isClickAnimating) {
                if (MiuiKeyguardUtils.canShowChargeCircle(MiuiKeyguardChargingContainer.this.mContext)) {
                    boolean unused = MiuiKeyguardChargingContainer.this.mIsBottomButtonAnimating = isClickAnimating;
                    MiuiKeyguardChargingContainer.this.handleBottomButtonClicked(isClickAnimating);
                }
            }

            public void onStartedWakingUp() {
                super.onStartedWakingUp();
                if (!FeatureParser.getBoolean("is_pad", false)) {
                    MiuiKeyguardChargingContainer.this.resetViewsCollapseState();
                }
            }

            public void onRefreshBatteryInfo(KeyguardUpdateMonitor.BatteryStatus status) {
                super.onRefreshBatteryInfo(status);
                KeyguardUpdateMonitor.BatteryStatus unused = MiuiKeyguardChargingContainer.this.mLastBatteryStatus = status;
            }
        };
        this.mChargeInfoRunnable = new Runnable() {
            public void run() {
                Bundle bundle = null;
                Uri uri = null;
                try {
                    uri = Uri.parse("content://com.miui.powercenter.provider");
                    bundle = MiuiKeyguardChargingContainer.this.mContext.getContentResolver().call(uri, "getBatteryCurrent", null, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (bundle != null) {
                    try {
                        bundle = MiuiKeyguardChargingContainer.this.mContext.getContentResolver().call(uri, "getBatteryInfo", null, null);
                    } catch (Exception e2) {
                        e2.printStackTrace();
                    }
                    if (bundle != null) {
                        long lastChargedTime = bundle.getLong("last_charged_time");
                        long drainedTime = bundle.getLong("last_drained_time");
                        int drainedPercent = bundle.getInt("last_drained_percent");
                        Bundle mBundle = new Bundle();
                        mBundle.putLong("lastChargedTime", lastChargedTime);
                        mBundle.putLong("drainedTime", drainedTime);
                        mBundle.putInt("drainedPercent", drainedPercent);
                        Message msg = new Message();
                        msg.setData(mBundle);
                        msg.what = 1;
                        MiuiKeyguardChargingContainer.this.mChargeHandler.sendMessage(msg);
                    }
                }
            }
        };
        this.mChargeHandler = new Handler() {
            public void handleMessage(Message msg) {
                Bundle mChargebundle = msg.getData();
                long lastChargedTime = mChargebundle.getLong("lastChargedTime");
                if (lastChargedTime <= 0) {
                    MiuiKeyguardChargingContainer.this.mLastChargeTime.setText("-");
                } else {
                    MiuiKeyguardChargingContainer.this.mLastChargeTime.setText(MiuiKeyguardChargingContainer.this.getLastChargeFormat(lastChargedTime));
                }
                long drainedTime = mChargebundle.getLong("drainedTime");
                if (drainedTime <= 0) {
                    MiuiKeyguardChargingContainer.this.mUsedTime.setText("-");
                } else {
                    long minutes = ChargeUtils.getMins(drainedTime);
                    long hours = ChargeUtils.getHours(drainedTime);
                    String drainedTimeStr = "-";
                    if (hours > 0 && minutes > 0) {
                        drainedTimeStr = MiuiKeyguardChargingContainer.this.mResources.getQuantityString(R.plurals.keyguard_charging_info_drained_time_format, (int) hours, new Object[]{Long.valueOf(hours), Long.valueOf(minutes)});
                    } else if (hours > 0) {
                        drainedTimeStr = MiuiKeyguardChargingContainer.this.mResources.getQuantityString(R.plurals.keyguard_charging_info_drained_hour_time_format, (int) hours, new Object[]{Long.valueOf(hours)});
                    } else if (minutes > 0) {
                        drainedTimeStr = MiuiKeyguardChargingContainer.this.mResources.getQuantityString(R.plurals.keyguard_charging_info_drained_min_time_format, (int) minutes, new Object[]{Long.valueOf(minutes)});
                    }
                    MiuiKeyguardChargingContainer.this.mUsedTime.setText(drainedTimeStr);
                }
                int drainedPercent = mChargebundle.getInt("drainedPercent");
                MiuiKeyguardChargingContainer.this.mDrainedPowerPercent.setText(MiuiKeyguardChargingContainer.this.mResources.getString(R.string.keyguard_charging_info_data_percent, new Object[]{String.valueOf(drainedPercent)}));
                MiuiKeyguardChargingContainer.this.updateContentDescription(R.id.keyguard_charging_last_time);
                MiuiKeyguardChargingContainer.this.updateContentDescription(R.id.keyguard_charging_used_time);
                MiuiKeyguardChargingContainer.this.updateContentDescription(R.id.keyguard_charging_drained_power);
            }
        };
        init(context);
    }

    private void init(Context context) {
        this.mContext = context;
        this.mResources = this.mContext.getResources();
        KeyguardUpdateMonitor.getInstance(this.mContext).registerCallback(this.mKeyguardUpdateMonitorCallback);
        this.mLayoutDirection = this.mContext.getResources().getConfiguration().getLayoutDirection();
        refreshScreenSize();
        this.mBgHandler = new Handler((Looper) Dependency.get(Dependency.BG_LOOPER));
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        refreshScreenSize();
        float fontScale = newConfig.fontScale;
        int densityDpi = newConfig.densityDpi;
        if (this.mFontScale != fontScale) {
            updateCharginInfosTextSize();
            this.mFontScale = fontScale;
        }
        if (this.mDensityDpi != densityDpi) {
            initDimension();
            initChargingInfosDes();
            updateCharginInfosTextSize();
            updateViewsLayoutParams();
            this.mDensityDpi = densityDpi;
        }
        if ((this.mLanguage != null && !this.mLanguage.equals(newConfig.locale.getLanguage())) || (this.mCountry != null && !this.mCountry.equalsIgnoreCase(newConfig.locale.getCountry()))) {
            this.mLayoutDirection = newConfig.getLayoutDirection();
            initChargingInfosDes();
            this.mLanguage = newConfig.locale.getLanguage();
            this.mCountry = newConfig.locale.getCountry();
        }
    }

    private void updateChargingInfoViewsLayout() {
        updateChargingInfoViewLayout(R.id.keyguard_charging_tip);
        updateChargingInfoViewLayout(R.id.keyguard_charging_last_time);
        updateChargingInfoViewLayout(R.id.keyguard_charging_used_time);
        updateChargingInfoViewLayout(R.id.keyguard_charging_drained_power);
    }

    private void updateCharginInfosTextSize() {
        this.mChargingHintView.setTextSize(0, (float) this.mResources.getDimensionPixelSize(R.dimen.battery_charging_progress_view_text_size));
        updateChargeInfoTextSize(R.id.keyguard_charging_last_time);
        updateChargeInfoTextSize(R.id.keyguard_charging_used_time);
        updateChargeInfoTextSize(R.id.keyguard_charging_drained_power);
        updateChargeInfoTextSize(R.id.keyguard_charging_tip);
    }

    private void updateViewsLayoutParams() {
        RelativeLayout.LayoutParams chargingViewlayoutParams = (RelativeLayout.LayoutParams) this.mChargingView.getLayoutParams();
        chargingViewlayoutParams.bottomMargin = this.mResources.getDimensionPixelOffset(R.dimen.keyguard_slide_icon_view_size);
        chargingViewlayoutParams.width = this.mResources.getDimensionPixelOffset(R.dimen.keyguard_charging_view_width);
        chargingViewlayoutParams.height = this.mResources.getDimensionPixelOffset(R.dimen.keyguard_charging_view_height);
        this.mChargingView.setLayoutParams(chargingViewlayoutParams);
        updateChargingInfoViewsLayout();
        this.mChargingListAndBackArrow.setPaddingRelative(this.mResources.getDimensionPixelOffset(R.dimen.keyguard_charging_info_layout_margin_start), 0, this.mResources.getDimensionPixelOffset(R.dimen.keyguard_charging_info_layout_margin_end), 0);
        LinearLayout.LayoutParams chargingInfoBackArrowlayoutParams = (LinearLayout.LayoutParams) this.mChargingInfoBackArrow.getLayoutParams();
        chargingInfoBackArrowlayoutParams.topMargin = this.mResources.getDimensionPixelSize(R.dimen.keyguard_charging_info_back_arrow_top_margin);
        chargingInfoBackArrowlayoutParams.bottomMargin = this.mResources.getDimensionPixelSize(R.dimen.keyguard_charging_info_back_arrow_bottom_margin);
        this.mChargingInfoBackArrow.setLayoutParams(chargingInfoBackArrowlayoutParams);
        this.mChargingInfoBackArrow.setImageDrawable(this.mResources.getDrawable(R.drawable.keyguard_charging_info_back_arrow_bg));
    }

    private void refreshScreenSize() {
        Point screenSize = new Point();
        ((WindowManager) this.mContext.getSystemService("window")).getDefaultDisplay().getRealSize(screenSize);
        if (!FeatureParser.getBoolean("is_pad", false)) {
            this.mScreenHeight = Math.max(screenSize.x, screenSize.y);
        } else if (this.mResources.getConfiguration().orientation == 2) {
            this.mScreenHeight = Math.min(screenSize.x, screenSize.y);
        } else {
            this.mScreenHeight = Math.max(screenSize.x, screenSize.y);
        }
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mChargingView = (MiuiKeyguardChargingView) findViewById(R.id.battery_charging_view);
        this.mChargingView.setChargingContainer(this);
        this.mChargingView.setNeedRepositionDevice(this.mNeedRepositionDevice);
        this.mChargingListAndBackArrow = findViewById(R.id.charging_list_and_back_arrow_layout_id);
        this.mChargingInfoBackArrow = (ImageView) findViewById(R.id.keyguard_charging_info_back_arrow_id);
        initDimension();
        this.mBgView = (ImageView) findViewById(R.id.keyguard_charging_container_bg);
        this.mChargingCircleView = findViewById(R.id.keyguard_charging_info_anim_start_cicle_id);
        this.mChargingHintView = (TextView) findViewById(R.id.keyguard_charging_hint);
        this.mChargingView.setChargingHint(this.mChargingHintView);
        initChargingInfosDes();
        this.mChargingTips = new int[]{R.string.keyguard_charging_info_tip_text1, R.string.keyguard_charging_info_tip_text2, R.string.keyguard_charging_info_tip_text3, R.string.keyguard_charging_info_tip_text4, R.string.keyguard_charging_info_tip_text5, R.string.keyguard_charging_info_tip_text6};
    }

    private void initChargingInfosDes() {
        initChargingInfoDes(R.id.keyguard_charging_last_time, R.drawable.keyguard_charging_info_last_charged_time_icon, R.string.keyguard_charging_info_last_charged_time_text, R.drawable.keyguard_charging_info_middle_list_first_item_bg);
        initChargingInfoDes(R.id.keyguard_charging_used_time, R.drawable.keyguard_charging_info_used_time_icon, R.string.keyguard_charging_info_battery_used_time_text, R.drawable.keyguard_charging_info_middle_list_second_item_bg);
        initChargingInfoDes(R.id.keyguard_charging_drained_power, R.drawable.keyguard_charging_info_drained_power_percent_icon, R.string.keyguard_charging_info_drained_power_percent_text, R.drawable.keyguard_charging_info_middle_list_third_item_bg);
        initChargingInfoDes(R.id.keyguard_charging_tip, R.drawable.keyguard_charging_info_tip_icon, R.string.keyguard_charging_info_tip_text1, R.drawable.keyguard_charging_info_last_list_bg);
    }

    private void initDimension() {
        this.mChargingViewHeight = this.mResources.getDimensionPixelOffset(R.dimen.keyguard_charging_view_height);
        this.mChargingViewHeightAfterScale = this.mResources.getDimensionPixelSize(R.dimen.keyguard_charging_view_height_after_enlarge);
        this.mChargingViewTopAfterScale = this.mResources.getDimensionPixelSize(R.dimen.keyguard_charging_view_after_enlarge_top_margin);
        this.mChargingViewBottomMarginUp = this.mResources.getDimensionPixelSize(R.dimen.keyguard_charging_view_bottom_margin);
        this.mChargingViewBottomMarginDown = this.mResources.getDimensionPixelSize(R.dimen.keyguard_slide_icon_view_size);
        this.mChargingListTopMargin = this.mResources.getDimensionPixelSize(R.dimen.keyguard_charging_info_list_top_margin);
        this.mHeight = this.mResources.getDimensionPixelSize(R.dimen.keyguard_bottom_charging_info_height);
    }

    private void initChargingInfoDes(int itemId, int imageId, int desId, int bgId) {
        View item = findViewById(itemId);
        item.setBackgroundResource(bgId);
        item.setLayoutDirection(this.mLayoutDirection);
        ((ImageView) item.findViewById(R.id.keyguard_charging_info_icon)).setImageDrawable(this.mResources.getDrawable(imageId));
        TextView des = (TextView) item.findViewById(R.id.keyguard_charging_info_des);
        des.setText(desId);
        TextView view = (TextView) item.findViewById(R.id.keyguard_charging_info_data);
        if (itemId != R.id.keyguard_charging_drained_power) {
            switch (itemId) {
                case R.id.keyguard_charging_last_time /*2131362184*/:
                    this.mLastChargeTime = view;
                    return;
                case R.id.keyguard_charging_tip /*2131362185*/:
                    this.mChargingTip = des;
                    this.mChargingTip.setTextSize(0, (float) this.mResources.getDimensionPixelSize(R.dimen.keyguard_charging_info_des_size));
                    this.mChargingTip.setTextColor(this.mResources.getColor(R.color.keyguard_charging_info_text_middle_color));
                    return;
                case R.id.keyguard_charging_used_time /*2131362186*/:
                    this.mUsedTime = view;
                    return;
                default:
                    return;
            }
        } else {
            this.mDrainedPowerPercent = view;
        }
    }

    /* access modifiers changed from: private */
    public void updateContentDescription(int itemId) {
        View item = findViewById(itemId);
        item.setContentDescription(((TextView) item.findViewById(R.id.keyguard_charging_info_des)).getText().toString() + ((TextView) item.findViewById(R.id.keyguard_charging_info_data)).getText().toString());
    }

    private void updateChargingInfoViewLayout(int itemId) {
        View item = findViewById(itemId);
        ImageView icon = (ImageView) item.findViewById(R.id.keyguard_charging_info_icon);
        LinearLayout.LayoutParams iconLayoutParams = (LinearLayout.LayoutParams) icon.getLayoutParams();
        iconLayoutParams.setMarginEnd(this.mResources.getDimensionPixelSize(R.dimen.keyguard_charging_info_middle_list_icon_margin_end));
        icon.setLayoutParams(iconLayoutParams);
        LinearLayout.LayoutParams itemLayoutParams = (LinearLayout.LayoutParams) item.getLayoutParams();
        if (itemId != R.id.keyguard_charging_drained_power) {
            switch (itemId) {
                case R.id.keyguard_charging_last_time /*2131362184*/:
                case R.id.keyguard_charging_used_time /*2131362186*/:
                    break;
                case R.id.keyguard_charging_tip /*2131362185*/:
                    itemLayoutParams.height = this.mResources.getDimensionPixelOffset(R.dimen.keyguard_charging_info_last_list_layout_height);
                    itemLayoutParams.topMargin = this.mResources.getDimensionPixelSize(R.dimen.keyguard_charging_info_horizontal_separator_top_margin);
                    break;
            }
        }
        itemLayoutParams.height = this.mResources.getDimensionPixelOffset(R.dimen.keyguard_charging_info_charging_middle_list_item_layout_height);
        item.setLayoutParams(itemLayoutParams);
    }

    private void updateChargeInfoTextSize(int itemId) {
        View item = findViewById(itemId);
        ((TextView) item.findViewById(R.id.keyguard_charging_info_des)).setTextSize(0, (float) this.mResources.getDimensionPixelSize(R.dimen.keyguard_charging_info_title_text_size));
        ((TextView) item.findViewById(R.id.keyguard_charging_info_data)).setTextSize(0, (float) this.mResources.getDimensionPixelSize(R.dimen.keyguard_charging_info_content_text_size));
    }

    public void onChargeViewClick() {
        if (!isFullScreen()) {
            ViewGroup.LayoutParams lp = getLayoutParams();
            lp.height = -1;
            lp.width = -1;
            setLayoutParams(lp);
            tryLoadChargingInfo();
            post(new Runnable() {
                public void run() {
                    MiuiKeyguardChargingContainer.this.startUpAnim();
                }
            });
        }
    }

    public void setDarkMode(boolean dark) {
        this.mChargingView.setChargingHint(this.mChargingHintView);
        this.mChargingView.setDarkMode(dark);
    }

    public void setChargingInfo(String hint, int temperature, int level) {
        this.mChargingHintView.setText(hint);
        this.mIsTempHigh = ((double) Math.round((((float) temperature) / 10.0f) * 10.0f)) / 10.0d > ((double) this.mResources.getInteger(R.integer.config_charging_temp_warning_level));
        if (this.mNeedRepositionDevice) {
            this.mChargingTip.setText(R.string.wireless_charge_reset_device_detail);
        } else if (this.mIsTempHigh) {
            this.mChargingTip.setText(this.mContext.getString(this.mChargingTips[this.mChargingTips.length - 1]));
        }
        this.mBatteryLevel = level;
    }

    public void setNeedRepositionDevice(boolean needRepositionDevice) {
        this.mNeedRepositionDevice = needRepositionDevice;
        if (this.mChargingView != null) {
            this.mChargingView.setNeedRepositionDevice(needRepositionDevice);
        }
    }

    public void startUpAnim() {
        cancelAnimation();
        logPositionInfo("startUpAnim");
        int backArrowUpY = this.mChargingViewTopAfterScale + this.mChargingViewHeightAfterScale + this.mChargingViewBottomMarginUp + this.mChargingHintView.getHeight() + this.mChargingListTopMargin;
        this.mInitY = (this.mScreenHeight - this.mChargingViewHeight) - this.mChargingViewBottomMarginDown;
        Drawable drawable = KeyguardWallpaperUtils.getLockWallpaperPreview(this.mContext);
        Bitmap backgroundOriginal = drawable == null ? null : ((BitmapDrawable) drawable).getBitmap();
        int width = backgroundOriginal == null ? 0 : (int) (((float) backgroundOriginal.getWidth()) * 0.33333334f);
        int height = backgroundOriginal == null ? 0 : (int) (((float) backgroundOriginal.getHeight()) * 0.33333334f);
        if (width > 0 && height > 0) {
            Bitmap backgroundOriginal2 = Bitmap.createScaledBitmap(backgroundOriginal, width, height, true);
            this.mBgView.setImageDrawable(new BitmapDrawable(ScreenshotUtils.getBlurBackground(backgroundOriginal2, null)));
            backgroundOriginal2.recycle();
        }
        this.mBgView.setVisibility(0);
        ObjectAnimator bgAnim = ObjectAnimator.ofFloat(this.mBgView, View.ALPHA, new float[]{0.0f, 1.0f});
        bgAnim.setInterpolator(new CubicEaseOutInterpolater());
        bgAnim.setDuration(200);
        this.mChargingHintView.setY(this.mChargingView.getY() + ((float) this.mChargingViewHeight) + ((float) this.mChargingViewBottomMarginUp));
        ObjectAnimator chargingHintYAnim = ObjectAnimator.ofFloat(this.mChargingHintView, Y, new float[]{this.mChargingHintView.getY(), (float) (this.mChargingViewTopAfterScale + this.mChargingViewHeightAfterScale + this.mChargingViewBottomMarginUp)});
        ObjectAnimator chargingViewYAnim = ObjectAnimator.ofFloat(this.mChargingView, Y, new float[]{this.mChargingView.getY(), (float) this.mChargingViewTopAfterScale});
        ObjectAnimator chargingListAndBackArrowYAnim = ObjectAnimator.ofFloat(this.mChargingListAndBackArrow, Y, new float[]{(float) this.mScreenHeight, (float) backArrowUpY});
        ObjectAnimator chargingListAlphaAnim = ObjectAnimator.ofFloat(this.mChargingListAndBackArrow, ALPHA, new float[]{0.0f, 1.0f});
        ValueAnimator chargingViewValueAnimator = ValueAnimator.ofInt(new int[]{this.mChargingViewHeight, this.mChargingViewHeightAfterScale});
        chargingViewValueAnimator.setDuration(300);
        chargingViewValueAnimator.setInterpolator(new CubicEaseOutInterpolater());
        chargingViewValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                int height = Integer.valueOf(animation.getAnimatedValue().toString()).intValue();
                ViewGroup.LayoutParams lp = MiuiKeyguardChargingContainer.this.mChargingView.getLayoutParams();
                lp.width = height;
                lp.height = height;
                MiuiKeyguardChargingContainer.this.mChargingView.setLayoutParams(lp);
            }
        });
        this.mUpAnimator = new AnimatorSet();
        this.mUpAnimator.setDuration(300);
        this.mUpAnimator.setInterpolator(new CubicEaseOutInterpolater());
        this.mUpAnimator.playTogether(new Animator[]{chargingListAndBackArrowYAnim, chargingListAlphaAnim, chargingViewYAnim, chargingHintYAnim, chargingViewValueAnimator, bgAnim});
        this.mUpAnimator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animation) {
                MiuiKeyguardChargingContainer.this.mBgView.setVisibility(0);
                MiuiKeyguardChargingContainer.this.mChargingHintView.setVisibility(0);
                MiuiKeyguardChargingContainer.this.mChargingListAndBackArrow.setVisibility(0);
            }

            public void onAnimationCancel(Animator animation) {
                super.onAnimationCancel(animation);
                MiuiKeyguardChargingContainer.this.mBgView.setVisibility(4);
                MiuiKeyguardChargingContainer.this.mChargingHintView.setVisibility(4);
                MiuiKeyguardChargingContainer.this.mChargingListAndBackArrow.setVisibility(4);
            }
        });
        this.mUpAnimator.start();
        setCanShowGxzw(false);
    }

    public boolean isChargingAnimationInDeclining() {
        return this.mChargingAnimationInDeclining;
    }

    public void startDownAnim() {
        if (!this.mChargingAnimationInDeclining) {
            this.mChargingAnimationInDeclining = true;
            cancelAnimation();
            logPositionInfo("startDownAnim:");
            ObjectAnimator chargingViewYAnim = ObjectAnimator.ofFloat(this.mChargingView, Y, new float[]{this.mChargingView.getY(), (float) this.mInitY});
            ObjectAnimator chargingHintYAnim = ObjectAnimator.ofFloat(this.mChargingHintView, Y, new float[]{this.mChargingHintView.getY(), (float) (this.mInitY + this.mChargingViewHeight)});
            ObjectAnimator chargingHintAlphaAnim = ObjectAnimator.ofFloat(this.mChargingHintView, ALPHA, new float[]{1.0f, 0.0f});
            chargingHintAlphaAnim.setDuration(100);
            ObjectAnimator chargingListAndBackArrowUpAnim = ObjectAnimator.ofFloat(this.mChargingListAndBackArrow, Y, new float[]{this.mChargingListAndBackArrow.getY(), (float) this.mScreenHeight});
            ObjectAnimator chargingListAlphaAnim = ObjectAnimator.ofFloat(this.mChargingListAndBackArrow, ALPHA, new float[]{1.0f, 0.0f});
            ObjectAnimator bgAnim = ObjectAnimator.ofFloat(this.mBgView, ALPHA, new float[]{1.0f, 0.0f});
            bgAnim.setInterpolator(new CubicEaseOutInterpolater());
            bgAnim.setDuration(300);
            ValueAnimator chargingViewValueAnimator = ValueAnimator.ofInt(new int[]{this.mChargingViewHeightAfterScale, this.mChargingViewHeight});
            chargingViewValueAnimator.setDuration(300);
            chargingViewValueAnimator.setInterpolator(new CubicEaseOutInterpolater());
            chargingViewValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    int height = Integer.valueOf(animation.getAnimatedValue().toString()).intValue();
                    ViewGroup.LayoutParams lp = MiuiKeyguardChargingContainer.this.mChargingView.getLayoutParams();
                    lp.width = height;
                    lp.height = height;
                    MiuiKeyguardChargingContainer.this.mChargingView.setLayoutParams(lp);
                }
            });
            this.mDownAnimator = new AnimatorSet();
            this.mDownAnimator.setDuration(300);
            this.mDownAnimator.setInterpolator(new CubicEaseOutInterpolater());
            this.mDownAnimator.playTogether(new Animator[]{chargingListAndBackArrowUpAnim, chargingListAlphaAnim, chargingViewYAnim, chargingHintYAnim, chargingHintAlphaAnim, chargingViewValueAnimator, bgAnim});
            this.mDownAnimator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    MiuiKeyguardChargingContainer.this.mBgView.setVisibility(4);
                    boolean unused = MiuiKeyguardChargingContainer.this.mChargingAnimationInDeclining = false;
                    MiuiKeyguardChargingContainer.this.mChargingView.setTranslationY(0.0f);
                    MiuiKeyguardChargingContainer.this.mChargingHintView.setAlpha(1.0f);
                    MiuiKeyguardChargingContainer.this.mChargingHintView.setVisibility(4);
                    MiuiKeyguardChargingContainer.this.mChargingListAndBackArrow.setVisibility(4);
                    ViewGroup.LayoutParams lp = MiuiKeyguardChargingContainer.this.getLayoutParams();
                    lp.width = -2;
                    lp.height = MiuiKeyguardChargingContainer.this.mHeight;
                    MiuiKeyguardChargingContainer.this.setLayoutParams(lp);
                }
            });
            this.mDownAnimator.start();
            setCanShowGxzw(true);
        }
    }

    public void startEnterAnim() {
        if (!this.mIsBottomButtonAnimating) {
            cancelAnimation();
            logPositionInfo("startEnterAnim:");
            this.mChargingCircleView.setVisibility(0);
            ObjectAnimator circleYAnimator = ObjectAnimator.ofFloat(this.mChargingCircleView, Y, new float[]{(float) getHeight(), (this.mChargingView.getY() + ((float) (this.mChargingViewHeight / 2))) - ((float) (this.mChargingCircleView.getHeight() / 2))});
            circleYAnimator.setDuration(200);
            ObjectAnimator circleScaleXAnimator = ObjectAnimator.ofFloat(this.mChargingCircleView, SCALE_X, new float[]{1.0f, 4.0f});
            circleScaleXAnimator.setDuration(300);
            ObjectAnimator circleScaleYAnimator = ObjectAnimator.ofFloat(this.mChargingCircleView, SCALE_Y, new float[]{1.0f, 4.0f});
            circleScaleYAnimator.setDuration(300);
            ObjectAnimator circleAlphaAnimator = ObjectAnimator.ofFloat(this.mChargingCircleView, ALPHA, new float[]{1.0f, 0.0f});
            circleAlphaAnimator.setDuration(200);
            this.mChargingView.setAlpha(0.0f);
            this.mChargingView.setScaleX(0.0f);
            this.mChargingView.setScaleY(0.0f);
            this.mChargingView.setVisibility(0);
            this.mChargingView.setChargingLevelForAnima(0);
            ObjectAnimator chargingViewScaleXAnimator = ObjectAnimator.ofFloat(this.mChargingView, SCALE_X, new float[]{0.0f, 1.0f});
            chargingViewScaleXAnimator.setDuration(300);
            ObjectAnimator chargingViewScaleYAnimator = ObjectAnimator.ofFloat(this.mChargingView, SCALE_Y, new float[]{0.0f, 1.0f});
            chargingViewScaleYAnimator.setDuration(300);
            ObjectAnimator chargingViewAlphaAnimator = ObjectAnimator.ofFloat(this.mChargingView, ALPHA, new float[]{0.0f, 1.0f});
            chargingViewScaleYAnimator.setDuration(300);
            this.mEnterAnimator = new AnimatorSet();
            this.mEnterAnimator.setInterpolator(new CubicEaseOutInterpolater());
            this.mEnterAnimator.playTogether(new Animator[]{circleScaleXAnimator, circleScaleYAnimator, chargingViewScaleXAnimator, chargingViewScaleYAnimator, chargingViewAlphaAnimator});
            this.mEnterAnimator.play(circleScaleXAnimator).after(circleYAnimator);
            this.mEnterAnimator.play(circleAlphaAnimator).after(200);
            this.mEnterAnimator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    ValueAnimator levelAnimator = ValueAnimator.ofInt(new int[]{0, MiuiKeyguardChargingContainer.this.mBatteryLevel});
                    levelAnimator.setDuration(500);
                    levelAnimator.setInterpolator(new CubicEaseOutInterpolater());
                    levelAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        public void onAnimationUpdate(ValueAnimator animation) {
                            MiuiKeyguardChargingContainer.this.mChargingView.setChargingLevelForAnima(Integer.valueOf(animation.getAnimatedValue().toString()).intValue());
                        }
                    });
                    levelAnimator.start();
                    MiuiKeyguardChargingContainer.this.mChargingCircleView.setVisibility(4);
                    MiuiKeyguardChargingContainer.this.mChargingCircleView.setScaleX(0.5f);
                    MiuiKeyguardChargingContainer.this.mChargingCircleView.setScaleY(0.5f);
                    MiuiKeyguardChargingContainer.this.mChargingCircleView.setY((float) MiuiKeyguardChargingContainer.this.getHeight());
                    MiuiKeyguardChargingContainer.this.mChargingCircleView.setAlpha(1.0f);
                    if (MiuiKeyguardChargingContainer.this.mNeedRepositionDevice) {
                        MiuiKeyguardChargingContainer.this.mChargingTip.setText(R.string.wireless_charge_reset_device_detail);
                    } else if (MiuiKeyguardChargingContainer.this.mIsTempHigh) {
                        MiuiKeyguardChargingContainer.this.mChargingTip.setText(MiuiKeyguardChargingContainer.this.mContext.getString(MiuiKeyguardChargingContainer.this.mChargingTips[MiuiKeyguardChargingContainer.this.mChargingTips.length - 1]));
                    } else {
                        MiuiKeyguardChargingContainer.this.mChargingTip.setText(MiuiKeyguardChargingContainer.this.mContext.getString(MiuiKeyguardChargingContainer.this.mChargingTips[new Random().nextInt(MiuiKeyguardChargingContainer.this.mChargingTips.length - 1)]));
                    }
                }
            });
            this.mEnterAnimator.start();
        }
    }

    private void cancelAnimation() {
        if (this.mEnterAnimator != null) {
            this.mEnterAnimator.cancel();
        }
        if (this.mDownAnimator != null) {
            this.mDownAnimator.cancel();
        }
        if (this.mUpAnimator != null) {
            this.mUpAnimator.cancel();
        }
    }

    private void tryLoadChargingInfo() {
        this.mBgHandler.removeCallbacks(this.mChargeInfoRunnable);
        this.mBgHandler.post(this.mChargeInfoRunnable);
    }

    /* access modifiers changed from: private */
    public String getLastChargeFormat(long time) {
        Calendar current = Calendar.getInstance();
        Calendar today = Calendar.getInstance();
        today.set(1, current.get(1));
        today.set(2, current.get(2));
        today.set(5, current.get(5));
        today.set(11, 0);
        today.set(12, 0);
        today.set(13, 0);
        Calendar yesterday = Calendar.getInstance();
        yesterday.set(1, current.get(1));
        yesterday.set(2, current.get(2));
        yesterday.set(5, current.get(5) - 1);
        yesterday.set(11, 0);
        yesterday.set(12, 0);
        yesterday.set(13, 0);
        String chargeTime = DateUtils.formatDateTime(time, (DateFormat.is24HourFormat(this.mContext, -2) ? 32 : 16) | 12);
        if (time > today.getTimeInMillis()) {
            return this.mResources.getString(R.string.keyguard_charging_info_last_charge_today_time, new Object[]{chargeTime});
        } else if (time <= yesterday.getTimeInMillis()) {
            return new SimpleDateFormat(this.mResources.getString(R.string.keyguard_charging_info_last_charge_time)).format(Long.valueOf(time));
        } else {
            return this.mResources.getString(R.string.keyguard_charging_info_last_charge_yesterday_time, new Object[]{chargeTime});
        }
    }

    public boolean onTouchEvent(MotionEvent ev) {
        if (getLayoutParams().height != -1) {
            return false;
        }
        this.mParent.requestDisallowInterceptTouchEvent(true);
        return true;
    }

    public boolean onHoverEvent(MotionEvent event) {
        return getLayoutParams().height == -1;
    }

    public void updateVisibility(boolean isVisible) {
        int i = 8;
        if (!isVisible || !this.mIsBottomButtonAnimating) {
            if (isVisible) {
                i = 0;
            }
            setVisibility(i);
            return;
        }
        setVisibility(8);
    }

    public int getScreenHeight() {
        return this.mScreenHeight;
    }

    public boolean isFullScreen() {
        return getLayoutParams().height == -1;
    }

    private void setCanShowGxzw(boolean show) {
        this.mCanShowGxzw = show;
        if (MiuiKeyguardUtils.isGxzwSensor()) {
            MiuiGxzwManager.getInstance().updateGxzwState();
        }
    }

    public boolean canShowGxzw() {
        return this.mCanShowGxzw;
    }

    /* access modifiers changed from: private */
    public void handleBottomButtonClicked(boolean isClickAnimating) {
        startBottomButtonClickAnim(isClickAnimating);
    }

    private void startBottomButtonClickAnim(boolean isClickAnimating) {
        if (this.mBottomButtonClickAnimator != null && this.mBottomButtonClickAnimator.isRunning()) {
            if (isClickAnimating) {
                this.mBottomButtonClickAnimator.cancel();
            } else {
                return;
            }
        }
        if (isClickAnimating) {
            this.mBottomButtonClickAnimator = ObjectAnimator.ofFloat(this, ALPHA, new float[]{1.0f, 0.0f});
            this.mBottomButtonClickAnimator.setInterpolator(Ease.Quint.easeOut);
            this.mBottomButtonClickAnimator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    MiuiKeyguardChargingContainer.this.setVisibility(8);
                }
            });
        } else {
            this.mBottomButtonClickAnimator = ObjectAnimator.ofFloat(this, ALPHA, new float[]{0.0f, 1.0f});
            this.mBottomButtonClickAnimator.setInterpolator(Ease.Cubic.easeInOut);
            this.mBottomButtonClickAnimator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationStart(Animator animation) {
                    MiuiKeyguardChargingContainer.this.setVisibility(0);
                }

                public void onAnimationCancel(Animator animation) {
                    MiuiKeyguardChargingContainer.this.setVisibility(0);
                    MiuiKeyguardChargingContainer.this.setAlpha(1.0f);
                }
            });
        }
        this.mBottomButtonClickAnimator.setDuration(200);
        this.mBottomButtonClickAnimator.start();
    }

    /* access modifiers changed from: private */
    public void resetViewsCollapseState() {
        Log.d("MiuiKeyguardChargingContainer", "resetViewsCollapseState: ");
        ViewGroup.LayoutParams lp = getLayoutParams();
        lp.width = -2;
        lp.height = this.mHeight;
        setLayoutParams(lp);
        this.mChargingView.setTranslationY(0.0f);
        ViewGroup.LayoutParams lp2 = this.mChargingView.getLayoutParams();
        lp2.width = this.mChargingViewHeight;
        lp2.height = this.mChargingViewHeight;
        this.mChargingView.setLayoutParams(lp2);
        this.mChargingHintView.setVisibility(4);
        this.mChargingListAndBackArrow.setVisibility(4);
        this.mBgView.setVisibility(4);
        if (this.mLastBatteryStatus != null) {
            boolean z = true;
            boolean isChargingOrFull = this.mLastBatteryStatus.status == 2 || this.mLastBatteryStatus.status == 5;
            if (!this.mLastBatteryStatus.isPluggedIn() || !isChargingOrFull) {
                z = false;
            }
            boolean pluggedIn = z;
            this.mChargingView.setChargingLevel(this.mLastBatteryStatus.level);
            if (!pluggedIn || !MiuiKeyguardUtils.canShowChargeCircle(getContext())) {
                this.mChargingView.setVisibility(4);
            }
        }
    }

    private void logPositionInfo(String reason) {
        if (!(!isAttachedToWindow() || getParent() == null || this.mChargingView == null)) {
            int containerHeight = getHeight();
            int chargeCircleHeight = this.mChargingView.getHeight();
            int chargeCircleWidth = this.mChargingView.getWidth();
            int chargeCircleTopMargin = ((RelativeLayout.LayoutParams) this.mChargingView.getLayoutParams()).topMargin;
            int chargeCircleBottomMargin = ((RelativeLayout.LayoutParams) this.mChargingView.getLayoutParams()).bottomMargin;
            int chargeCircleTop = this.mChargingView.getTop();
            float chargeCircleY = this.mChargingView.getY();
            Slog.i("MiuiKeyguardChargingContainer", "logPositionInfo: reason: " + reason + "  screenHeight: " + this.mScreenHeight + " containerHeight: " + containerHeight + " chargeCircleHeight: " + chargeCircleHeight + " chargeCircleWidth: " + chargeCircleWidth + " chargeCircleTopMargin: " + chargeCircleTopMargin + " chargeCircleBottomMargin: " + chargeCircleBottomMargin + " chargeCircleTop: " + chargeCircleTop + " chargeCircleY: " + chargeCircleY);
        }
    }
}
