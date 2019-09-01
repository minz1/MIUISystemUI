package com.android.systemui.qs;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.Dependency;
import com.android.systemui.FontUtils;
import com.android.systemui.R;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.proxy.UserManager;
import com.android.systemui.qs.TouchAnimator;
import com.android.systemui.statusbar.phone.ExpandableIndicator;
import com.android.systemui.statusbar.phone.MultiUserSwitch;
import com.android.systemui.statusbar.phone.SettingsButton;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.NextAlarmController;
import com.android.systemui.statusbar.policy.UserInfoController;
import java.util.List;
import miui.telephony.SubscriptionInfo;

public class QSFooter extends FrameLayout implements View.OnClickListener, NetworkController.EmergencyListener, NetworkController.SignalCallback, UserInfoController.OnUserInfoChangedListener {
    private ActivityStarter mActivityStarter;
    private TouchAnimator mAlarmAnimator;
    private boolean mAlarmShowing;
    /* access modifiers changed from: private */
    public TextView mAlarmStatus;
    private View mAlarmStatusCollapsed;
    private boolean mAlwaysShowMultiUserSwitch;
    private TouchAnimator mAnimator;
    /* access modifiers changed from: private */
    public View mDate;
    private View mDateTimeGroup;
    protected View mEdit;
    protected ExpandableIndicator mExpandIndicator;
    private boolean mExpanded;
    private float mExpansionAmount;
    private boolean mKeyguardShowing;
    private boolean mListening;
    private ImageView mMultiUserAvatar;
    protected MultiUserSwitch mMultiUserSwitch;
    private AlarmManager.AlarmClockInfo mNextAlarm;
    private NextAlarmController mNextAlarmController;
    /* access modifiers changed from: private */
    public QSPanel mQsPanel;
    protected TouchAnimator mSettingsAlpha;
    private SettingsButton mSettingsButton;
    protected View mSettingsContainer;
    private boolean mShowEditIcon;
    private boolean mShowEmergencyCallsOnly;
    private UserInfoController mUserInfoController;

    public QSFooter(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        Resources res = getResources();
        this.mShowEditIcon = res.getBoolean(R.bool.config_showQuickSettingsEditingIcon);
        this.mEdit = findViewById(16908291);
        int i = 8;
        this.mEdit.setVisibility(this.mShowEditIcon ? 0 : 8);
        if (this.mShowEditIcon) {
            findViewById(16908291).setOnClickListener(new View.OnClickListener() {
                public void onClick(final View v) {
                    ((ActivityStarter) Dependency.get(ActivityStarter.class)).postQSRunnableDismissingKeyguard(new Runnable() {
                        public void run() {
                            QSFooter.this.mQsPanel.showEdit(v);
                        }
                    });
                }
            });
        }
        this.mDateTimeGroup = findViewById(R.id.date_time_alarm_group);
        this.mDate = findViewById(R.id.date);
        this.mExpandIndicator = (ExpandableIndicator) findViewById(R.id.expand_indicator);
        ExpandableIndicator expandableIndicator = this.mExpandIndicator;
        if (res.getBoolean(R.bool.config_showQuickSettingsExpandIndicator)) {
            i = 0;
        }
        expandableIndicator.setVisibility(i);
        this.mSettingsButton = (SettingsButton) findViewById(R.id.settings_button);
        this.mSettingsContainer = findViewById(R.id.settings_button_container);
        this.mSettingsButton.setOnClickListener(this);
        this.mAlarmStatusCollapsed = findViewById(R.id.alarm_status_collapsed);
        this.mAlarmStatus = (TextView) findViewById(R.id.alarm_status);
        this.mDateTimeGroup.setOnClickListener(this);
        this.mMultiUserSwitch = (MultiUserSwitch) findViewById(R.id.multi_user_switch);
        this.mMultiUserAvatar = (ImageView) this.mMultiUserSwitch.findViewById(R.id.multi_user_avatar);
        this.mAlwaysShowMultiUserSwitch = res.getBoolean(R.bool.config_alwaysShowMultiUserSwitcher);
        ((RippleDrawable) this.mSettingsButton.getBackground()).setForceSoftware(true);
        ((RippleDrawable) this.mExpandIndicator.getBackground()).setForceSoftware(true);
        updateResources();
        this.mNextAlarmController = (NextAlarmController) Dependency.get(NextAlarmController.class);
        this.mUserInfoController = (UserInfoController) Dependency.get(UserInfoController.class);
        this.mActivityStarter = (ActivityStarter) Dependency.get(ActivityStarter.class);
        addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                QSFooter.this.updateAnimator(right - left);
            }
        });
    }

    /* access modifiers changed from: private */
    public void updateAnimator(int width) {
        int numTiles = QuickQSPanel.getNumQuickTiles(this.mContext);
        int remaining = (width - (numTiles * (this.mContext.getResources().getDimensionPixelSize(R.dimen.qs_quick_tile_icon_bg_size) - this.mContext.getResources().getDimensionPixelSize(R.dimen.qs_quick_tile_padding)))) / (numTiles - 1);
        int defSpace = this.mContext.getResources().getDimensionPixelOffset(R.dimen.default_gear_space);
        this.mAnimator = new TouchAnimator.Builder().addFloat(this.mSettingsContainer, "translationX", (float) (-(remaining - defSpace)), 0.0f).addFloat(this.mSettingsButton, "rotation", -120.0f, 0.0f).build();
        if (this.mAlarmShowing) {
            this.mAlarmAnimator = new TouchAnimator.Builder().addFloat(this.mDate, "alpha", 1.0f, 0.0f).addFloat(this.mDateTimeGroup, "translationX", 0.0f, (float) (-this.mDate.getWidth())).addFloat(this.mAlarmStatus, "alpha", 0.0f, 1.0f).setListener(new TouchAnimator.ListenerAdapter() {
                public void onAnimationAtStart() {
                    QSFooter.this.mAlarmStatus.setVisibility(8);
                }

                public void onAnimationStarted() {
                    QSFooter.this.mAlarmStatus.setVisibility(0);
                }
            }).build();
        } else {
            this.mAlarmAnimator = null;
            this.mAlarmStatus.setVisibility(8);
            this.mDate.setAlpha(1.0f);
            this.mDateTimeGroup.setTranslationX(0.0f);
        }
        setExpansion(this.mExpansionAmount);
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateResources();
    }

    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);
        updateResources();
    }

    private void updateResources() {
        FontUtils.updateFontSize(this.mAlarmStatus, R.dimen.qs_date_collapsed_size);
        updateSettingsAnimator();
    }

    private void updateSettingsAnimator() {
        this.mSettingsAlpha = createSettingsAlphaAnimator();
        boolean isRtl = isLayoutRtl();
        if (!isRtl || this.mDate.getWidth() != 0) {
            this.mDate.setPivotX(isRtl ? (float) this.mDate.getWidth() : 0.0f);
        } else {
            this.mDate.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    QSFooter.this.mDate.setPivotX((float) QSFooter.this.getWidth());
                    QSFooter.this.mDate.removeOnLayoutChangeListener(this);
                }
            });
        }
    }

    private TouchAnimator createSettingsAlphaAnimator() {
        if (!this.mShowEditIcon && this.mAlwaysShowMultiUserSwitch) {
            return null;
        }
        TouchAnimator.Builder animatorBuilder = new TouchAnimator.Builder();
        animatorBuilder.setStartDelay(0.5f);
        if (this.mShowEditIcon) {
            animatorBuilder.addFloat(this.mEdit, "alpha", 0.0f, 1.0f);
        }
        if (!this.mAlwaysShowMultiUserSwitch) {
            animatorBuilder.addFloat(this.mMultiUserSwitch, "alpha", 0.0f, 1.0f);
        }
        return animatorBuilder.build();
    }

    public void setKeyguardShowing(boolean keyguardShowing) {
        this.mKeyguardShowing = keyguardShowing;
        setExpansion(this.mExpansionAmount);
    }

    public void setExpanded(boolean expanded) {
        if (this.mExpanded != expanded) {
            this.mExpanded = expanded;
            updateEverything();
        }
    }

    public void setExpansion(float headerExpansionFraction) {
        float f;
        this.mExpansionAmount = headerExpansionFraction;
        if (this.mAnimator != null) {
            this.mAnimator.setPosition(headerExpansionFraction);
        }
        if (this.mAlarmAnimator != null) {
            TouchAnimator touchAnimator = this.mAlarmAnimator;
            if (this.mKeyguardShowing) {
                f = 0.0f;
            } else {
                f = headerExpansionFraction;
            }
            touchAnimator.setPosition(f);
        }
        if (this.mSettingsAlpha != null) {
            this.mSettingsAlpha.setPosition(headerExpansionFraction);
        }
        updateAlarmVisibilities();
        this.mExpandIndicator.setExpanded(headerExpansionFraction > 0.93f);
    }

    public void onDetachedFromWindow() {
        setListening(false);
        super.onDetachedFromWindow();
    }

    private void updateAlarmVisibilities() {
        this.mAlarmStatusCollapsed.setVisibility(this.mAlarmShowing ? 0 : 8);
    }

    public void setListening(boolean listening) {
        if (listening != this.mListening) {
            this.mListening = listening;
            updateListeners();
        }
    }

    public View getExpandView() {
        return findViewById(R.id.expand_indicator);
    }

    public void updateEverything() {
        post(new Runnable() {
            public void run() {
                QSFooter.this.updateVisibilities();
                QSFooter.this.setClickable(false);
            }
        });
    }

    /* access modifiers changed from: private */
    public void updateVisibilities() {
        updateAlarmVisibilities();
        int i = 4;
        this.mSettingsContainer.findViewById(R.id.tuner_icon).setVisibility(4);
        boolean isDemo = UserManager.isDeviceInDemoMode(this.mContext);
        this.mMultiUserSwitch.setVisibility(((this.mExpanded || this.mAlwaysShowMultiUserSwitch) && this.mMultiUserSwitch.hasMultipleUsers() && !isDemo) ? 0 : 4);
        if (this.mShowEditIcon) {
            View view = this.mEdit;
            if (!isDemo && this.mExpanded) {
                i = 0;
            }
            view.setVisibility(i);
        }
    }

    private void updateListeners() {
        if (this.mListening) {
            this.mUserInfoController.addCallback(this);
            if (((NetworkController) Dependency.get(NetworkController.class)).hasVoiceCallingFeature()) {
                ((NetworkController) Dependency.get(NetworkController.class)).addEmergencyListener(this);
                ((NetworkController) Dependency.get(NetworkController.class)).addCallback(this);
                return;
            }
            return;
        }
        this.mUserInfoController.removeCallback(this);
        ((NetworkController) Dependency.get(NetworkController.class)).removeEmergencyListener(this);
        ((NetworkController) Dependency.get(NetworkController.class)).removeCallback(this);
    }

    public void setQSPanel(QSPanel qsPanel) {
        this.mQsPanel = qsPanel;
        if (this.mQsPanel != null) {
            this.mMultiUserSwitch.setQsPanel(qsPanel);
        }
    }

    public void onClick(View v) {
        int i;
        if (v == this.mSettingsButton) {
            if (!((DeviceProvisionedController) Dependency.get(DeviceProvisionedController.class)).isCurrentUserSetup()) {
                this.mActivityStarter.postQSRunnableDismissingKeyguard(new Runnable() {
                    public void run() {
                    }
                });
                return;
            }
            Context context = this.mContext;
            if (this.mExpanded) {
                i = 406;
            } else {
                i = 490;
            }
            MetricsLogger.action(context, i);
            if (!this.mSettingsButton.isTunerClick()) {
                startSettingsActivity();
            }
        } else if (v == this.mDateTimeGroup) {
            MetricsLogger.action(this.mContext, 930, this.mNextAlarm != null);
            if (this.mNextAlarm != null) {
                this.mActivityStarter.startPendingIntentDismissingKeyguard(this.mNextAlarm.getShowIntent());
            } else {
                this.mActivityStarter.postStartActivityDismissingKeyguard(new Intent("android.intent.action.SHOW_ALARMS"), 0);
            }
        }
    }

    private void startSettingsActivity() {
        this.mActivityStarter.startActivity(new Intent("android.settings.SETTINGS"), true);
    }

    public void setEmergencyCallsOnly(boolean show) {
        if (show != this.mShowEmergencyCallsOnly) {
            this.mShowEmergencyCallsOnly = show;
            if (this.mExpanded) {
                updateEverything();
            }
        }
    }

    public void onUserInfoChanged(String name, Drawable picture, String userAccount) {
    }

    public void setWifiIndicators(boolean enabled, NetworkController.IconState statusIcon, NetworkController.IconState qsIcon, boolean activityIn, boolean activityOut, String description, boolean isTransient) {
    }

    public void setMobileDataIndicators(NetworkController.IconState statusIcon, NetworkController.IconState qsIcon, int statusType, int qsType, boolean activityIn, boolean activityOut, int dataActivityId, int stackedDataIcon, int stackedVoiceIcon, String typeContentDescription, String description, boolean isWide, int slot, boolean roaming) {
    }

    public void setSubs(List<SubscriptionInfo> list) {
    }

    public void setNoSims(boolean show) {
    }

    public void setEthernetIndicators(NetworkController.IconState icon) {
    }

    public void setIsAirplaneMode(NetworkController.IconState icon) {
    }

    public void setMobileDataEnabled(boolean enabled) {
    }

    public void setIsImsRegisted(int slot, boolean imsRegisted) {
    }

    public void setVowifi(int slot, boolean vowifi) {
    }

    public void setVolteNoService(int slot, boolean show) {
    }

    public void setSpeechHd(int slot, boolean hd) {
    }

    public void setNetworkNameVoice(int slot, String networkNameVoice) {
    }

    public void setIsDefaultDataSim(int slot, boolean isDefaultDataSim) {
    }
}
