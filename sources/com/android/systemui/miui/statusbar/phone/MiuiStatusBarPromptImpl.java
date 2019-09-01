package com.android.systemui.miui.statusbar.phone;

import android.app.ActivityManager;
import android.app.ActivityManagerCompat;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.UserHandle;
import android.view.View;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.internal.telephony.Call;
import com.android.systemui.Constants;
import com.android.systemui.R;
import com.android.systemui.miui.statusbar.InCallUtils;
import com.android.systemui.recents.misc.RecentsPushEventHelper;
import com.android.systemui.statusbar.Icons;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.policy.DarkIconDispatcherHelper;
import com.miui.voiptalk.service.MiuiVoipManager;
import miui.telephony.TelephonyManager;

class MiuiStatusBarPromptImpl implements IMiuiStatusBarPrompt {
    private StatusBar mBar;
    private ImageView mCallStateIcon;
    private Chronometer mCallTimer;
    private int mClickActionType = -1;
    private Context mContext;
    private int mDisableFlags = 0;
    private View mDriveModeBg;
    private boolean mDriveModeMask;
    private TextView mDriveModeTextView;
    private boolean mIsSosTypeImage;
    private ImageView mNotchRecorderImage;
    private View mParentView;
    private boolean mPromptCenter;
    private Chronometer mRecordTimer;
    private View mReturnToDriveModeView;
    private View mReturnToInCallScreenButton;
    private TextView mReturnToMultiModeView;
    private View mReturnToRecorderView;
    private View mSafepayStatusBar;
    private TextView mSafepayStatusBarText;
    private View mSosStatusBar;

    public MiuiStatusBarPromptImpl(StatusBar statusBar, View parent, int disableFlags) {
        this.mBar = statusBar;
        this.mDisableFlags = disableFlags;
        this.mParentView = parent;
        this.mContext = this.mParentView.getContext();
        this.mPromptCenter = this.mContext.getResources().getBoolean(R.bool.config_notch_prompt_center);
        if (Constants.IS_NOTCH) {
            this.mReturnToInCallScreenButton = findViewById(R.id.notch_call);
            this.mCallStateIcon = (ImageView) this.mReturnToInCallScreenButton.findViewById(R.id.image);
            this.mCallTimer = (Chronometer) this.mReturnToInCallScreenButton.findViewById(R.id.timer);
        } else {
            this.mReturnToInCallScreenButton = findViewById(R.id.return_to_in_call_screen);
            this.mCallStateIcon = (ImageView) this.mReturnToInCallScreenButton.findViewById(R.id.call_state_icon);
            this.mCallTimer = (Chronometer) this.mReturnToInCallScreenButton.findViewById(R.id.call_timer);
        }
        if (Constants.IS_NOTCH) {
            this.mReturnToDriveModeView = findViewById(R.id.notch_drivemode);
            this.mDriveModeTextView = (TextView) findViewById(R.id.driveModeTipText_notch);
        } else {
            this.mReturnToDriveModeView = findViewById(R.id.returnToDriveModeScreen);
            this.mDriveModeTextView = (TextView) findViewById(R.id.driveModeTipText);
        }
        if (Constants.IS_NOTCH) {
            this.mReturnToMultiModeView = (TextView) findViewById(R.id.notch_multi);
        } else {
            this.mReturnToMultiModeView = (TextView) findViewById(R.id.returnToMultiMode);
        }
        this.mDriveModeBg = findViewById(R.id.drivemodebg);
        if (Constants.IS_NOTCH) {
            this.mReturnToRecorderView = findViewById(R.id.notch_recorder);
            this.mNotchRecorderImage = (ImageView) this.mReturnToRecorderView.findViewById(R.id.image);
            this.mRecordTimer = (Chronometer) this.mReturnToRecorderView.findViewById(R.id.timer);
        } else {
            this.mReturnToRecorderView = findViewById(R.id.returnToRecorderScreen);
            this.mRecordTimer = (Chronometer) findViewById(R.id.recorderTimer);
        }
        if (!isSafePayDisabled()) {
            if (Constants.IS_NOTCH) {
                this.mSafepayStatusBar = findViewById(R.id.notch_safe);
                this.mSafepayStatusBarText = (TextView) this.mSafepayStatusBar.findViewById(R.id.title);
            } else {
                this.mSafepayStatusBar = findViewById(R.id.safepayStatusBar);
                this.mSafepayStatusBarText = (TextView) findViewById(R.id.safepayStatusBarText);
            }
        }
        if (Constants.IS_NOTCH) {
            this.mSosStatusBar = findViewById(R.id.notch_sos);
        } else {
            this.mSosStatusBar = findViewById(R.id.sosStatusBar);
        }
    }

    public void showReturnToInCallScreenButton(String state, long baseTime) {
        Drawable drawable = this.mReturnToInCallScreenButton.getBackground();
        if (Call.State.HOLDING.toString().equals(state)) {
            if (Constants.IS_NOTCH) {
                drawable.setColorFilter(this.mContext.getResources().getColor(R.color.notch_call_color_yellow), PorterDuff.Mode.SRC_IN);
                this.mReturnToInCallScreenButton.setBackground(drawable);
            } else {
                this.mReturnToInCallScreenButton.setBackgroundResource(R.drawable.status_bar_yellow_bar_bg);
            }
            this.mCallStateIcon.setImageResource(R.drawable.status_bar_ic_return_to_incall_screen_pause);
        } else {
            if (Constants.IS_NOTCH) {
                drawable.setColorFilter(this.mContext.getResources().getColor(R.color.notch_call_color_green), PorterDuff.Mode.SRC_IN);
                this.mReturnToInCallScreenButton.setBackground(drawable);
            } else {
                this.mReturnToInCallScreenButton.setBackgroundResource(R.drawable.status_bar_green_bar_bg);
            }
            this.mCallStateIcon.setImageResource(R.drawable.status_bar_ic_return_to_incall_screen_normal);
        }
        if (!Constants.IS_NOTCH) {
            if (Call.State.ACTIVE.toString().equals(state)) {
                this.mCallTimer.setFormat(null);
            } else if (Call.State.HOLDING.toString().equals(state)) {
                this.mCallTimer.setFormat(this.mContext.getString(R.string.call_status_holding));
            } else if (Call.State.INCOMING.toString().equals(state)) {
                this.mCallTimer.setFormat(this.mContext.getString(R.string.call_status_ringing));
            } else {
                this.mCallTimer.setFormat(this.mContext.getString(R.string.call_status_dialing));
            }
            this.mCallTimer.setBase(baseTime);
            this.mCallTimer.start();
        } else if (!Call.State.ACTIVE.toString().equals(state)) {
            this.mCallTimer.stop();
            if (Call.State.HOLDING.toString().equals(state)) {
                this.mCallTimer.setBase(baseTime);
            } else {
                this.mCallTimer.setBase(SystemClock.elapsedRealtime());
            }
        } else {
            this.mCallTimer.setBase(baseTime);
            this.mCallTimer.start();
        }
        if (this.mReturnToInCallScreenButton.getVisibility() == 8) {
            initReturnToInCallScreenButtonIcons();
        }
        this.mReturnToInCallScreenButton.setVisibility(0);
        if (Constants.IS_NOTCH) {
            updateNotchPromptViewLayout(this.mReturnToInCallScreenButton);
        }
    }

    public void hideReturnToInCallScreenButton() {
        this.mCallTimer.stop();
        this.mReturnToInCallScreenButton.setVisibility(8);
        clearReturnToInCallScreenButtonIcons();
    }

    public void makeReturnToInCallScreenButtonVisible() {
        this.mReturnToInCallScreenButton.setVisibility(0);
    }

    public void makeReturnToInCallScreenButtonGone() {
        this.mReturnToInCallScreenButton.setVisibility(8);
    }

    public void initReturnToInCallScreenButtonIcons() {
    }

    private void clearReturnToInCallScreenButtonIcons() {
    }

    public void showReturnToMulti(boolean isShowReturnToMulti) {
        if (!isMultiWindowDisabled()) {
            if (isShowReturnToMulti) {
                if (Constants.IS_NOTCH) {
                    Drawable drawable = this.mReturnToMultiModeView.getBackground();
                    drawable.setColorFilter(this.mContext.getResources().getColor(R.color.notch_call_color_pink), PorterDuff.Mode.SRC_IN);
                    this.mReturnToMultiModeView.setBackground(drawable);
                    updateNotchPromptViewLayout(this.mReturnToMultiModeView);
                }
                this.mReturnToMultiModeView.setVisibility(0);
            } else {
                this.mReturnToMultiModeView.setVisibility(8);
            }
        }
    }

    public void showReturnToInCall(boolean isShowReturnToIncall) {
        if (isShowReturnToIncall) {
            this.mReturnToInCallScreenButton.setVisibility(0);
        } else {
            this.mReturnToInCallScreenButton.setVisibility(8);
        }
    }

    public void showReturnToDriveMode(boolean isShowReturnToDriveMode) {
        if (!isDriveModeDisabled()) {
            if (isShowReturnToDriveMode) {
                this.mReturnToDriveModeView.setVisibility(0);
                this.mDriveModeBg.setVisibility(0);
            } else {
                this.mReturnToDriveModeView.setVisibility(8);
                this.mDriveModeBg.setVisibility(8);
            }
        }
    }

    public void showReturnToDriveModeView(boolean show, boolean mask_mode) {
        if (!isDriveModeDisabled()) {
            this.mDriveModeMask = mask_mode;
            if (mask_mode) {
                if (Constants.IS_NOTCH) {
                    this.mDriveModeTextView.setText(R.string.drive_mode_tip_idle_notch);
                } else {
                    this.mDriveModeTextView.setText(R.string.drive_mode_tip_idle);
                }
            } else if (Constants.IS_NOTCH) {
                this.mDriveModeTextView.setText(R.string.drive_mode_tip_notch);
            } else {
                this.mDriveModeTextView.setText(R.string.drive_mode_tip);
            }
            int i = 8;
            this.mReturnToDriveModeView.setVisibility(show ? 0 : 8);
            if (Constants.IS_NOTCH && show) {
                updateNotchPromptViewLayout(this.mReturnToDriveModeView);
                View view = this.mDriveModeBg;
                if (show && mask_mode) {
                    i = 0;
                }
                view.setVisibility(i);
            }
        }
    }

    private void updateNotchPromptViewLayout(View viewGroup) {
        if (viewGroup != null) {
            boolean center = false;
            if (this.mPromptCenter) {
                center = true;
            }
            FrameLayout.LayoutParams mlp = (FrameLayout.LayoutParams) viewGroup.getLayoutParams();
            if ((mlp.gravity == 17) != center) {
                if (center) {
                    mlp.gravity = 17;
                } else {
                    mlp.gravity = 8388627;
                }
                viewGroup.setLayoutParams(mlp);
            }
        }
    }

    public void showReturnToRecorderView(boolean show) {
        this.mReturnToRecorderView.setVisibility(show ? 0 : 8);
    }

    public void hideReturnToRecorderView() {
        if (Constants.IS_NOTCH) {
            this.mNotchRecorderImage.setImageDrawable(null);
        } else {
            this.mReturnToRecorderView.setVisibility(8);
        }
        this.mRecordTimer.stop();
    }

    public void showReturnToRecorderView(String title, boolean enable, long duration) {
        if (Constants.IS_NOTCH) {
            Drawable drawable = this.mReturnToRecorderView.getBackground();
            drawable.setColorFilter(this.mContext.getColor(R.color.notch_recorder_color), PorterDuff.Mode.SRC_IN);
            this.mReturnToRecorderView.setBackground(drawable);
            this.mNotchRecorderImage.setImageResource(R.drawable.status_bar_recorder_icon);
            updateNotchPromptViewLayout(this.mReturnToRecorderView);
        } else {
            this.mReturnToRecorderView.setVisibility(0);
            ((TextView) this.mReturnToRecorderView.findViewById(R.id.recorderTitle)).setText(title);
        }
        this.mRecordTimer.stop();
        this.mRecordTimer.setBase(SystemClock.elapsedRealtime() - duration);
        if (enable) {
            this.mRecordTimer.start();
        }
    }

    public void showReturnToSafeBar(boolean show) {
        if (!isSafePayDisabled()) {
            this.mSafepayStatusBar.setVisibility(show ? 0 : 8);
        }
    }

    public void showSafePayStatusBar(int state, Bundle ext) {
        if (!isSafePayDisabled()) {
            if (!Constants.IS_NOTCH) {
                this.mSafepayStatusBar.setVisibility(0);
                switch (state) {
                    case 2:
                        this.mSafepayStatusBar.setBackgroundResource(R.drawable.safepay_status_bar_green_bg);
                        this.mSafepayStatusBarText.setText(R.string.status_bar_safepay_safe);
                        break;
                    case 3:
                        this.mSafepayStatusBar.setBackgroundResource(R.drawable.safepay_status_bar_orange_bg);
                        this.mSafepayStatusBarText.setText(R.string.status_bar_safepay_risk);
                        break;
                    case 4:
                        this.mSafepayStatusBar.setBackgroundResource(R.drawable.safepay_status_bar_yellow_bg);
                        this.mSafepayStatusBarText.setText(R.string.status_bar_safepay_unknown);
                        break;
                }
            } else {
                Drawable drawable = this.mSafepayStatusBar.getBackground();
                int colorId = -1;
                switch (state) {
                    case 2:
                        colorId = R.color.notch_safe_color;
                        this.mSafepayStatusBarText.setText(R.string.prompt_safe);
                        break;
                    case 3:
                        colorId = R.color.notch_danger_color;
                        this.mSafepayStatusBarText.setText(R.string.prompt_danger);
                        break;
                    case 4:
                        colorId = R.color.notch_failure;
                        this.mSafepayStatusBarText.setText(R.string.prompt_unknown);
                        break;
                }
                if (colorId != -1) {
                    drawable.setColorFilter(this.mContext.getResources().getColor(colorId), PorterDuff.Mode.SRC_IN);
                    this.mSafepayStatusBar.setBackground(drawable);
                }
                updateNotchPromptViewLayout(this.mSafepayStatusBar);
            }
        }
    }

    public void hideSafePayStatusBar() {
        if (!isSafePayDisabled()) {
            this.mSafepayStatusBar.setVisibility(8);
        }
    }

    public void showSosStatusBar() {
        if (!isSosDisabled()) {
            if (!Constants.IS_NOTCH || this.mIsSosTypeImage) {
                this.mSosStatusBar.setVisibility(0);
            } else {
                Drawable drawable = this.mSosStatusBar.getBackground();
                drawable.setColorFilter(this.mContext.getResources().getColor(R.color.notch_sos_status_bar_bg), PorterDuff.Mode.SRC_IN);
                this.mSosStatusBar.setBackground(drawable);
                updateNotchPromptViewLayout(this.mSosStatusBar);
            }
        }
    }

    public void hideSosStatusBar() {
        if (!isSosDisabled()) {
            this.mSosStatusBar.setVisibility(8);
        }
    }

    public void showReturnToSosBar(boolean show) {
        if (!isSosDisabled()) {
            this.mSosStatusBar.setVisibility(show ? 0 : 8);
        }
    }

    public void setSosTypeImage() {
        this.mIsSosTypeImage = true;
        this.mSosStatusBar = findViewById(R.id.notch_sos_image);
    }

    public void updateSosImageDark(boolean isDark, Rect area, float darkIntensity) {
        if (this.mIsSosTypeImage) {
            ImageView imageSosView = (ImageView) this.mSosStatusBar;
            imageSosView.setImageResource(Icons.get(Integer.valueOf(R.drawable.stat_sys_sos), DarkIconDispatcherHelper.inDarkMode(area, imageSosView, darkIntensity)));
        }
    }

    public boolean blockClickAction() {
        if (!this.mParentView.isShown()) {
            return false;
        }
        if (!isMultiWindowDisabled() && this.mReturnToMultiModeView != null && this.mReturnToMultiModeView.getVisibility() == 0) {
            this.mClickActionType = 0;
            return true;
        } else if (this.mReturnToInCallScreenButton != null && this.mReturnToInCallScreenButton.getVisibility() == 0) {
            this.mClickActionType = 1;
            return true;
        } else if (!isDriveModeDisabled() && this.mReturnToDriveModeView != null && this.mReturnToDriveModeView.getVisibility() == 0) {
            this.mClickActionType = 2;
            return true;
        } else if (this.mReturnToRecorderView != null && this.mReturnToRecorderView.getVisibility() == 0) {
            this.mClickActionType = 3;
            return true;
        } else if (this.mSosStatusBar == null || this.mSosStatusBar.getVisibility() != 0) {
            this.mClickActionType = -1;
            return false;
        } else {
            this.mClickActionType = 4;
            return true;
        }
    }

    public void handleClickAction() {
        if (this.mClickActionType == 0) {
            try {
                ActivityManager.StackInfo dockStackInfo = ActivityManagerCompat.getStackInfo(3, 3, 0);
                if (!(dockStackInfo == null || this.mBar == null)) {
                    this.mBar.showRecentApps(false, false);
                    RecentsPushEventHelper.sendMultiWindowEvent("clickStatusBarToReturnMultiWindow", dockStackInfo.topActivity + "");
                }
            } catch (Exception e) {
            }
        } else if (this.mClickActionType == 1) {
            TelephonyManager telephonyManager = TelephonyManager.getDefault();
            MiuiVoipManager miuiVoipManager = MiuiVoipManager.getInstance(this.mContext);
            if (telephonyManager.getCallState() == 0 && miuiVoipManager.getCallState() == 0) {
                hideReturnToInCallScreenButton();
            } else {
                InCallUtils.goInCallScreen(this.mContext);
            }
        } else {
            if (this.mClickActionType == 3) {
                Intent intent = new Intent("android.intent.action.MAIN", null);
                intent.setFlags(335806464);
                intent.setClassName("com.android.soundrecorder", "com.android.soundrecorder.SoundRecorder");
                this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
            }
            if (this.mClickActionType == 4) {
                Intent dialogIntent = new Intent("miui.intent.action.EXIT_SOS");
                dialogIntent.setPackage("com.android.settings");
                dialogIntent.addFlags(268435456);
                this.mContext.startActivity(dialogIntent);
            }
        }
    }

    public Context getContext() {
        return this.mContext;
    }

    private View findViewById(int id) {
        return this.mParentView.findViewById(id);
    }

    private boolean isDriveModeDisabled() {
        return (this.mDisableFlags & 1) != 0;
    }

    private boolean isSafePayDisabled() {
        return (this.mDisableFlags & 2) != 0;
    }

    private boolean isSosDisabled() {
        return (this.mDisableFlags & 8) != 0;
    }

    private boolean isMultiWindowDisabled() {
        return (this.mDisableFlags & 4) != 0;
    }
}
