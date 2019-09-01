package com.android.keyguard;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.ServiceManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManagerCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.IccCardConstants;
import com.android.keyguard.utils.PhoneUtils;
import com.android.systemui.R;
import miui.telephony.SubscriptionManager;

public class KeyguardSimPinView extends KeyguardPinBasedInputView {
    /* access modifiers changed from: private */
    public CheckSimPin mCheckSimPinThread;
    private ViewGroup mContainer;
    /* access modifiers changed from: private */
    public int mRemainingAttempts;
    /* access modifiers changed from: private */
    public boolean mShowDefaultMessage;
    /* access modifiers changed from: private */
    public ImageView mSimImageView;
    /* access modifiers changed from: private */
    public ProgressDialog mSimUnlockProgressDialog;
    /* access modifiers changed from: private */
    public int mSubId;
    KeyguardUpdateMonitorCallback mUpdateMonitorCallback;

    private abstract class CheckSimPin extends Thread {
        private final String mPin;
        private int mSubId;

        /* access modifiers changed from: package-private */
        public abstract void onSimCheckResponse(int i, int i2);

        protected CheckSimPin(String pin, int subId) {
            this.mPin = pin;
            this.mSubId = subId;
        }

        public void run() {
            try {
                Log.v("KeyguardSimPinView", "call supplyPinReportResultForSubscriber(subid=" + this.mSubId + ")");
                final int[] result = ITelephony.Stub.asInterface(ServiceManager.checkService("phone")).supplyPinReportResultForSubscriber(this.mSubId, this.mPin);
                Log.v("KeyguardSimPinView", "supplyPinReportResult returned: " + result[0] + " " + result[1]);
                KeyguardSimPinView.this.post(new Runnable() {
                    public void run() {
                        CheckSimPin.this.onSimCheckResponse(result[0], result[1]);
                    }
                });
            } catch (Exception e) {
                Log.e("KeyguardSimPinView", "Exception for supplyPinReportResult:", e);
                KeyguardSimPinView.this.post(new Runnable() {
                    public void run() {
                        CheckSimPin.this.onSimCheckResponse(2, -1);
                    }
                });
            }
        }
    }

    public KeyguardSimPinView(Context context) {
        this(context, null);
    }

    public KeyguardSimPinView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mSimUnlockProgressDialog = null;
        this.mShowDefaultMessage = true;
        this.mRemainingAttempts = -1;
        this.mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        this.mUpdateMonitorCallback = new KeyguardUpdateMonitorCallback() {
            public void onSimStateChanged(int subId, int slotId, IccCardConstants.State simState) {
                Log.v("KeyguardSimPinView", "onSimStateChanged(subId=" + subId + ",state=" + simState + ")");
                KeyguardSimPinView.this.resetState();
                if (PhoneUtils.getPhoneCount() == 2) {
                    KeyguardSimPinView.this.mSimImageView.setVisibility(0);
                    int pinRequiredSlotid = SubscriptionManagerCompat.getSlotIndex(KeyguardSimPinView.this.mSubId);
                    if (pinRequiredSlotid == 0) {
                        KeyguardSimPinView.this.mSimImageView.setImageResource(R.drawable.miui_keyguard_unlock_sim_1);
                    } else if (pinRequiredSlotid == 1) {
                        KeyguardSimPinView.this.mSimImageView.setImageResource(R.drawable.miui_keyguard_unlock_sim_2);
                    }
                } else {
                    KeyguardSimPinView.this.mSimImageView.setVisibility(8);
                }
            }
        };
    }

    public void resetState() {
        super.resetState();
        Log.v("KeyguardSimPinView", "Resetting state");
        handleSubInfoChangeIfNeeded();
        if (this.mShowDefaultMessage) {
            showDefaultMessage();
        }
    }

    private void handleSubInfoChangeIfNeeded() {
        int subId = KeyguardUpdateMonitor.getInstance(this.mContext).getNextSubIdForState(IccCardConstants.State.PIN_REQUIRED);
        if (subId != this.mSubId && SubscriptionManager.isValidSubscriptionId(subId)) {
            this.mSubId = subId;
            this.mShowDefaultMessage = true;
            this.mRemainingAttempts = -1;
        }
    }

    /* access modifiers changed from: protected */
    public void handleConfigurationFontScaleChanged() {
        int textSize = getResources().getDimensionPixelSize(R.dimen.miui_keyguard_view_eca_text_size);
        this.mEmergencyButton.setTextSize(0, (float) textSize);
        this.mDeleteButton.setTextSize(0, (float) textSize);
    }

    /* access modifiers changed from: protected */
    public void handleConfigurationOrientationChanged() {
        Resources resources = getResources();
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) this.mContainer.getLayoutParams();
        layoutParams.width = resources.getDimensionPixelOffset(R.dimen.miui_keyguard_sim_pin_view_layout_width);
        layoutParams.height = resources.getDimensionPixelOffset(R.dimen.miui_keyguard_sim_pin_view_layout_height);
        this.mContainer.setLayoutParams(layoutParams);
    }

    /* access modifiers changed from: private */
    public String getPinPasswordErrorMessage(int attemptsRemaining, boolean isDefault) {
        String displayMessage;
        int msgId;
        if (attemptsRemaining == 0) {
            displayMessage = getContext().getString(R.string.kg_password_wrong_pin_code_pukked);
        } else if (attemptsRemaining > 0) {
            if (isDefault) {
                msgId = R.plurals.kg_password_default_pin_message;
            } else {
                msgId = R.plurals.kg_password_wrong_pin_code;
            }
            displayMessage = getContext().getResources().getQuantityString(msgId, attemptsRemaining, new Object[]{Integer.valueOf(attemptsRemaining)});
        } else {
            displayMessage = getContext().getString(isDefault ? R.string.kg_sim_pin_instructions : R.string.kg_password_pin_failed);
        }
        Log.d("KeyguardSimPinView", "getPinPasswordErrorMessage: attemptsRemaining=" + attemptsRemaining + " displayMessage=" + displayMessage);
        return displayMessage;
    }

    /* access modifiers changed from: protected */
    public boolean shouldLockout(long deadline) {
        return false;
    }

    /* access modifiers changed from: protected */
    public int getPasswordTextViewId() {
        return R.id.simPinEntry;
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mSimImageView = (ImageView) findViewById(R.id.keyguard_sim);
        this.mContainer = (ViewGroup) findViewById(R.id.container);
        this.mDeleteButton.setVisibility(0);
        this.mBackButton.setVisibility(8);
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (this.mShowDefaultMessage) {
            showDefaultMessage();
        }
        KeyguardUpdateMonitor.getInstance(this.mContext).registerCallback(this.mUpdateMonitorCallback);
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        KeyguardUpdateMonitor.getInstance(this.mContext).removeCallback(this.mUpdateMonitorCallback);
    }

    public void onPause() {
        if (this.mSimUnlockProgressDialog != null) {
            this.mSimUnlockProgressDialog.dismiss();
            this.mSimUnlockProgressDialog = null;
        }
    }

    /* access modifiers changed from: protected */
    public void verifyPasswordAndUnlock() {
        if (this.mPasswordEntry.getText().length() < 4) {
            this.mSecurityMessageDisplay.setMessage((int) R.string.kg_invalid_sim_pin_hint);
            resetPasswordText(true, true);
            this.mCallback.userActivity();
            return;
        }
        if (this.mCheckSimPinThread == null) {
            this.mCheckSimPinThread = new CheckSimPin(this.mPasswordEntry.getText(), this.mSubId) {
                /* access modifiers changed from: package-private */
                public void onSimCheckResponse(final int result, final int attemptsRemaining) {
                    KeyguardSimPinView.this.post(new Runnable() {
                        public void run() {
                            int unused = KeyguardSimPinView.this.mRemainingAttempts = attemptsRemaining;
                            if (KeyguardSimPinView.this.mSimUnlockProgressDialog != null) {
                                KeyguardSimPinView.this.mSimUnlockProgressDialog.hide();
                            }
                            KeyguardSimPinView.this.resetPasswordText(true, result != 0);
                            if (result == 0) {
                                KeyguardUpdateMonitor.getInstance(KeyguardSimPinView.this.getContext()).reportSimUnlocked(KeyguardSimPinView.this.mSubId);
                                int unused2 = KeyguardSimPinView.this.mRemainingAttempts = -1;
                                boolean unused3 = KeyguardSimPinView.this.mShowDefaultMessage = true;
                                if (KeyguardSimPinView.this.mCallback != null) {
                                    KeyguardSimPinView.this.mCallback.dismiss(true, KeyguardUpdateMonitor.getCurrentUser());
                                }
                            } else {
                                boolean unused4 = KeyguardSimPinView.this.mShowDefaultMessage = false;
                                if (result == 1) {
                                    KeyguardSimPinView.this.mSecurityMessageDisplay.setMessage((CharSequence) KeyguardSimPinView.this.getPinPasswordErrorMessage(attemptsRemaining, false));
                                } else {
                                    KeyguardSimPinView.this.mSecurityMessageDisplay.setMessage((CharSequence) KeyguardSimPinView.this.getContext().getString(R.string.kg_password_pin_failed));
                                }
                                Log.d("KeyguardSimPinView", "verifyPasswordAndUnlock  CheckSimPin.onSimCheckResponse: " + result + " attemptsRemaining=" + attemptsRemaining);
                            }
                            KeyguardSimPinView.this.mCallback.userActivity();
                            CheckSimPin unused5 = KeyguardSimPinView.this.mCheckSimPinThread = null;
                        }
                    });
                }
            };
            this.mCheckSimPinThread.start();
        }
    }

    public void startAppearAnimation() {
    }

    public boolean startDisappearAnimation(Runnable finishRunnable) {
        return false;
    }

    private void showDefaultMessage() {
        String msg;
        if (this.mRemainingAttempts >= 0) {
            this.mSecurityMessageDisplay.setMessage((CharSequence) getPinPasswordErrorMessage(this.mRemainingAttempts, true));
            return;
        }
        int count = PhoneUtils.getPhoneCount();
        Resources rez = getResources();
        if (count < 2) {
            msg = rez.getString(R.string.kg_sim_pin_instructions);
        } else {
            SubscriptionInfo info = KeyguardUpdateMonitor.getInstance(this.mContext).getSubscriptionInfoForSubId(this.mSubId);
            msg = rez.getString(R.string.kg_sim_pin_instructions_multi, new Object[]{info != null ? info.getDisplayName() : ""});
            if (info != null) {
                int color = info.getIconTint();
            }
        }
        this.mSecurityMessageDisplay.setMessage((CharSequence) msg);
        new CheckSimPin("", this.mSubId) {
            /* access modifiers changed from: package-private */
            public void onSimCheckResponse(int result, int attemptsRemaining) {
                Log.d("KeyguardSimPinView", "onSimCheckResponse  dummy One result" + result + " attemptsRemaining=" + attemptsRemaining);
                if (attemptsRemaining >= 0) {
                    int unused = KeyguardSimPinView.this.mRemainingAttempts = attemptsRemaining;
                    KeyguardSimPinView.this.mSecurityMessageDisplay.setMessage((CharSequence) KeyguardSimPinView.this.getPinPasswordErrorMessage(attemptsRemaining, true));
                }
            }
        }.start();
    }
}
