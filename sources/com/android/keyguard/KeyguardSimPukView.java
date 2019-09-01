package com.android.keyguard;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.os.ServiceManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.IccCardConstants;
import com.android.systemui.R;

public class KeyguardSimPukView extends KeyguardPinBasedInputView {
    /* access modifiers changed from: private */
    public CheckSimPuk mCheckSimPukThread;
    private ViewGroup mContainer;
    /* access modifiers changed from: private */
    public String mPinText;
    /* access modifiers changed from: private */
    public String mPukText;
    /* access modifiers changed from: private */
    public int mRemainingAttempts;
    private AlertDialog mRemainingAttemptsDialog;
    /* access modifiers changed from: private */
    public boolean mShowDefaultMessage;
    private ImageView mSimImageView;
    /* access modifiers changed from: private */
    public ProgressDialog mSimUnlockProgressDialog;
    /* access modifiers changed from: private */
    public StateMachine mStateMachine;
    /* access modifiers changed from: private */
    public int mSubId;
    KeyguardUpdateMonitorCallback mUpdateMonitorCallback;

    private abstract class CheckSimPuk extends Thread {
        private final String mPin;
        private final String mPuk;
        private final int mSubId;

        /* access modifiers changed from: package-private */
        public abstract void onSimLockChangedResponse(int i, int i2);

        protected CheckSimPuk(String puk, String pin, int subId) {
            this.mPuk = puk;
            this.mPin = pin;
            this.mSubId = subId;
        }

        public void run() {
            try {
                Log.v("KeyguardSimPukView", "call supplyPukReportResult()");
                final int[] result = ITelephony.Stub.asInterface(ServiceManager.checkService("phone")).supplyPukReportResultForSubscriber(this.mSubId, this.mPuk, this.mPin);
                Log.v("KeyguardSimPukView", "supplyPukReportResult returned: " + result[0] + " " + result[1]);
                KeyguardSimPukView.this.post(new Runnable() {
                    public void run() {
                        CheckSimPuk.this.onSimLockChangedResponse(result[0], result[1]);
                    }
                });
            } catch (Exception e) {
                Log.e("KeyguardSimPukView", "Exception for supplyPukReportResult:", e);
                KeyguardSimPukView.this.post(new Runnable() {
                    public void run() {
                        CheckSimPuk.this.onSimLockChangedResponse(2, -1);
                    }
                });
            }
        }
    }

    private class StateMachine {
        final int CONFIRM_PIN;
        final int DONE;
        final int ENTER_PIN;
        final int ENTER_PUK;
        private int state;

        private StateMachine() {
            this.ENTER_PUK = 0;
            this.ENTER_PIN = 1;
            this.CONFIRM_PIN = 2;
            this.DONE = 3;
            this.state = 0;
        }

        public void next() {
            int msg = 0;
            if (this.state == 0) {
                if (KeyguardSimPukView.this.checkPuk()) {
                    this.state = 1;
                    msg = R.string.kg_puk_enter_pin_hint;
                } else {
                    msg = R.string.kg_invalid_sim_puk_hint;
                }
            } else if (this.state == 1) {
                if (KeyguardSimPukView.this.checkPin()) {
                    this.state = 2;
                    msg = R.string.kg_enter_confirm_pin_hint;
                } else {
                    msg = R.string.kg_invalid_sim_pin_hint;
                }
            } else if (this.state == 2) {
                if (KeyguardSimPukView.this.confirmPin()) {
                    this.state = 3;
                    msg = R.string.keyguard_sim_unlock_progress_dialog_message;
                    KeyguardSimPukView.this.updateSim();
                } else {
                    this.state = 1;
                    msg = R.string.kg_invalid_confirm_pin_hint;
                }
            }
            KeyguardSimPukView.this.resetPasswordText(true, true);
            if (msg != 0) {
                KeyguardSimPukView.this.mSecurityMessageDisplay.setMessage(msg);
            }
        }

        /* access modifiers changed from: package-private */
        public void reset() {
            String unused = KeyguardSimPukView.this.mPinText = "";
            String unused2 = KeyguardSimPukView.this.mPukText = "";
            this.state = 0;
            KeyguardSimPukView.this.handleSubInfoChangeIfNeeded();
            if (KeyguardSimPukView.this.mShowDefaultMessage) {
                KeyguardSimPukView.this.showDefaultMessage();
            }
            KeyguardSimPukView.this.mPasswordEntry.requestFocus();
        }
    }

    public KeyguardSimPukView(Context context) {
        this(context, null);
    }

    public KeyguardSimPukView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mSimUnlockProgressDialog = null;
        this.mShowDefaultMessage = true;
        this.mRemainingAttempts = -1;
        this.mStateMachine = new StateMachine();
        this.mSubId = -1;
        this.mUpdateMonitorCallback = new KeyguardUpdateMonitorCallback() {
            public void onSimStateChanged(int subId, int slotId, IccCardConstants.State simState) {
                Log.v("KeyguardSimPukView", "onSimStateChanged(subId=" + subId + ",state=" + simState + ")");
                KeyguardSimPukView.this.resetState();
            }
        };
    }

    /* access modifiers changed from: private */
    public void handleSubInfoChangeIfNeeded() {
        int subId = KeyguardUpdateMonitor.getInstance(this.mContext).getNextSubIdForState(IccCardConstants.State.PUK_REQUIRED);
        if (subId != this.mSubId && SubscriptionManager.isValidSubscriptionId(subId)) {
            this.mSubId = subId;
            this.mShowDefaultMessage = true;
            this.mRemainingAttempts = -1;
        }
    }

    /* access modifiers changed from: private */
    public String getPukPasswordErrorMessage(int attemptsRemaining, boolean isDefault) {
        String displayMessage;
        int msgId;
        int msgId2;
        if (attemptsRemaining == 0) {
            displayMessage = getContext().getString(R.string.kg_password_wrong_puk_code_dead);
        } else if (attemptsRemaining > 0) {
            if (isDefault) {
                msgId2 = R.plurals.kg_password_default_puk_message;
            } else {
                msgId2 = R.plurals.kg_password_wrong_puk_code;
            }
            displayMessage = getContext().getResources().getQuantityString(msgId2, attemptsRemaining, new Object[]{Integer.valueOf(attemptsRemaining)});
        } else {
            if (isDefault) {
                msgId = R.string.kg_puk_enter_puk_hint;
            } else {
                msgId = R.string.kg_password_puk_failed;
            }
            displayMessage = getContext().getString(msgId);
        }
        Log.d("KeyguardSimPukView", "getPukPasswordErrorMessage: attemptsRemaining=" + attemptsRemaining + " displayMessage=" + displayMessage);
        return displayMessage;
    }

    public void resetState() {
        super.resetState();
        this.mStateMachine.reset();
    }

    /* access modifiers changed from: protected */
    public boolean shouldLockout(long deadline) {
        return false;
    }

    /* access modifiers changed from: protected */
    public int getPasswordTextViewId() {
        return R.id.pukEntry;
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

    private Dialog getSimUnlockProgressDialog() {
        if (this.mSimUnlockProgressDialog == null) {
            this.mSimUnlockProgressDialog = new ProgressDialog(this.mContext);
            this.mSimUnlockProgressDialog.setMessage(this.mContext.getString(R.string.kg_sim_unlock_progress_dialog_message));
            this.mSimUnlockProgressDialog.setIndeterminate(true);
            this.mSimUnlockProgressDialog.setCancelable(false);
            if (!(this.mContext instanceof Activity)) {
                this.mSimUnlockProgressDialog.getWindow().setType(2009);
            }
        }
        return this.mSimUnlockProgressDialog;
    }

    /* access modifiers changed from: private */
    public Dialog getPukRemainingAttemptsDialog(int remaining) {
        String msg = getPukPasswordErrorMessage(remaining, false);
        if (this.mRemainingAttemptsDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this.mContext);
            builder.setMessage(msg);
            builder.setCancelable(false);
            builder.setNeutralButton(R.string.ok, null);
            this.mRemainingAttemptsDialog = builder.create();
            this.mRemainingAttemptsDialog.getWindow().setType(2009);
        } else {
            this.mRemainingAttemptsDialog.setMessage(msg);
        }
        return this.mRemainingAttemptsDialog;
    }

    /* access modifiers changed from: private */
    public boolean checkPuk() {
        if (this.mPasswordEntry.getText().length() != 8) {
            return false;
        }
        this.mPukText = this.mPasswordEntry.getText();
        return true;
    }

    /* access modifiers changed from: private */
    public boolean checkPin() {
        int length = this.mPasswordEntry.getText().length();
        if (length < 4 || length > 8) {
            return false;
        }
        this.mPinText = this.mPasswordEntry.getText();
        return true;
    }

    public boolean confirmPin() {
        return this.mPinText.equals(this.mPasswordEntry.getText());
    }

    /* access modifiers changed from: private */
    public void updateSim() {
        getSimUnlockProgressDialog().show();
        if (this.mCheckSimPukThread == null) {
            this.mCheckSimPukThread = new CheckSimPuk(this.mPukText, this.mPinText, this.mSubId) {
                /* access modifiers changed from: package-private */
                public void onSimLockChangedResponse(final int result, final int attemptsRemaining) {
                    KeyguardSimPukView.this.post(new Runnable() {
                        public void run() {
                            if (KeyguardSimPukView.this.mSimUnlockProgressDialog != null) {
                                KeyguardSimPukView.this.mSimUnlockProgressDialog.hide();
                            }
                            KeyguardSimPukView.this.resetPasswordText(true, result != 0);
                            if (result == 0) {
                                KeyguardUpdateMonitor.getInstance(KeyguardSimPukView.this.getContext()).reportSimUnlocked(KeyguardSimPukView.this.mSubId);
                                int unused = KeyguardSimPukView.this.mRemainingAttempts = -1;
                                boolean unused2 = KeyguardSimPukView.this.mShowDefaultMessage = true;
                                if (KeyguardSimPukView.this.mCallback != null) {
                                    KeyguardSimPukView.this.mCallback.dismiss(true, KeyguardUpdateMonitor.getCurrentUser());
                                }
                            } else {
                                boolean unused3 = KeyguardSimPukView.this.mShowDefaultMessage = false;
                                if (result == 1) {
                                    KeyguardSimPukView.this.mSecurityMessageDisplay.setMessage((CharSequence) KeyguardSimPukView.this.getPukPasswordErrorMessage(attemptsRemaining, false));
                                    if (attemptsRemaining <= 2) {
                                        KeyguardSimPukView.this.getPukRemainingAttemptsDialog(attemptsRemaining).show();
                                    } else {
                                        KeyguardSimPukView.this.mSecurityMessageDisplay.setMessage((CharSequence) KeyguardSimPukView.this.getPukPasswordErrorMessage(attemptsRemaining, false));
                                    }
                                } else {
                                    KeyguardSimPukView.this.mSecurityMessageDisplay.setMessage((CharSequence) KeyguardSimPukView.this.getContext().getString(R.string.kg_password_puk_failed));
                                }
                                Log.d("KeyguardSimPukView", "verifyPasswordAndUnlock  UpdateSim.onSimCheckResponse:  attemptsRemaining=" + attemptsRemaining);
                                KeyguardSimPukView.this.mStateMachine.reset();
                            }
                            CheckSimPuk unused4 = KeyguardSimPukView.this.mCheckSimPukThread = null;
                        }
                    });
                }
            };
            this.mCheckSimPukThread.start();
        }
    }

    /* access modifiers changed from: protected */
    public void verifyPasswordAndUnlock() {
        this.mStateMachine.next();
    }

    public void startAppearAnimation() {
    }

    public boolean startDisappearAnimation(Runnable finishRunnable) {
        return false;
    }

    /* access modifiers changed from: private */
    public void showDefaultMessage() {
        String msg;
        if (this.mRemainingAttempts >= 0) {
            this.mSecurityMessageDisplay.setMessage((CharSequence) getPukPasswordErrorMessage(this.mRemainingAttempts, true));
            return;
        }
        int count = TelephonyManager.getDefault().getSimCount();
        Resources rez = getResources();
        int color = -1;
        if (count < 2) {
            msg = rez.getString(R.string.kg_puk_enter_puk_hint);
        } else {
            SubscriptionInfo info = KeyguardUpdateMonitor.getInstance(this.mContext).getSubscriptionInfoForSubId(this.mSubId);
            msg = rez.getString(R.string.kg_puk_enter_puk_hint_multi, new Object[]{info != null ? info.getDisplayName() : ""});
            if (info != null) {
                color = info.getIconTint();
            }
        }
        this.mSecurityMessageDisplay.setMessage((CharSequence) msg);
        this.mSimImageView.setImageTintList(ColorStateList.valueOf(color));
        new CheckSimPuk("", "", this.mSubId) {
            /* access modifiers changed from: package-private */
            public void onSimLockChangedResponse(int result, int attemptsRemaining) {
                Log.d("KeyguardSimPukView", "onSimCheckResponse  dummy One result" + result + " attemptsRemaining=" + attemptsRemaining);
                if (attemptsRemaining >= 0) {
                    int unused = KeyguardSimPukView.this.mRemainingAttempts = attemptsRemaining;
                    KeyguardSimPukView.this.mSecurityMessageDisplay.setMessage((CharSequence) KeyguardSimPukView.this.getPukPasswordErrorMessage(attemptsRemaining, true));
                }
            }
        }.start();
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
}
