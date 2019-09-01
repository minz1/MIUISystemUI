package com.android.keyguard;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import com.android.keyguard.PasswordTextView;
import com.android.systemui.R;

public abstract class KeyguardPinBasedInputView extends KeyguardAbsKeyInputView implements View.OnKeyListener, View.OnTouchListener {
    private View mButton0;
    private View mButton1;
    private View mButton2;
    private View mButton3;
    private View mButton4;
    private View mButton5;
    private View mButton6;
    private View mButton7;
    private View mButton8;
    private View mButton9;
    private View mOkButton;
    protected PasswordTextView mPasswordEntry;

    public KeyguardPinBasedInputView(Context context) {
        this(context, null);
    }

    public KeyguardPinBasedInputView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void reset() {
        this.mPasswordEntry.requestFocus();
        super.reset();
    }

    /* access modifiers changed from: protected */
    public boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        return this.mPasswordEntry.requestFocus(direction, previouslyFocusedRect);
    }

    /* access modifiers changed from: protected */
    public void resetState() {
        setPasswordEntryEnabled(true);
    }

    /* access modifiers changed from: protected */
    public void setPasswordEntryEnabled(boolean enabled) {
        this.mPasswordEntry.setEnabled(enabled);
        if (this.mOkButton != null) {
            this.mOkButton.setEnabled(enabled);
        }
    }

    /* access modifiers changed from: protected */
    public void setPasswordEntryInputEnabled(boolean enabled) {
        this.mPasswordEntry.setEnabled(enabled);
        if (this.mOkButton != null) {
            this.mOkButton.setEnabled(enabled);
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (KeyEvent.isConfirmKey(keyCode)) {
            performClick(this.mOkButton);
            return true;
        } else if (keyCode == 67) {
            performClick(this.mDeleteButton);
            return true;
        } else if (keyCode >= 7 && keyCode <= 16) {
            performNumberClick(keyCode - 7);
            return true;
        } else if (keyCode < 144 || keyCode > 153) {
            return super.onKeyDown(keyCode, event);
        } else {
            performNumberClick(keyCode - 144);
            return true;
        }
    }

    private void performClick(View view) {
        if (view != null) {
            view.performClick();
        }
    }

    private void performNumberClick(int number) {
        switch (number) {
            case 0:
                performClick(this.mButton0);
                return;
            case 1:
                performClick(this.mButton1);
                return;
            case 2:
                performClick(this.mButton2);
                return;
            case 3:
                performClick(this.mButton3);
                return;
            case 4:
                performClick(this.mButton4);
                return;
            case 5:
                performClick(this.mButton5);
                return;
            case 6:
                performClick(this.mButton6);
                return;
            case 7:
                performClick(this.mButton7);
                return;
            case 8:
                performClick(this.mButton8);
                return;
            case 9:
                performClick(this.mButton9);
                return;
            default:
                return;
        }
    }

    /* access modifiers changed from: protected */
    public void resetPasswordText(boolean animate, boolean announce) {
        this.mPasswordEntry.reset(animate, announce);
    }

    /* access modifiers changed from: protected */
    public String getPasswordText() {
        return this.mPasswordEntry.getText();
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mPasswordEntry = (PasswordTextView) findViewById(getPasswordTextViewId());
        this.mPasswordEntry.setOnKeyListener(this);
        this.mPasswordEntry.setSelected(true);
        this.mPasswordEntry.setUserActivityListener(new PasswordTextView.UserActivityListener() {
            public void onUserActivity() {
                KeyguardPinBasedInputView.this.onUserInput();
            }
        });
        this.mOkButton = findViewById(R.id.key_enter);
        if (this.mOkButton != null) {
            this.mOkButton.setOnTouchListener(this);
            this.mOkButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (KeyguardPinBasedInputView.this.mPasswordEntry.isEnabled()) {
                        KeyguardPinBasedInputView.this.verifyPasswordAndUnlock();
                    }
                }
            });
            this.mOkButton.setOnHoverListener(new LiftToActivateListener(getContext()));
        }
        this.mDeleteButton.setOnTouchListener(this);
        this.mDeleteButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (KeyguardPinBasedInputView.this.mPasswordEntry.isEnabled()) {
                    KeyguardPinBasedInputView.this.mPasswordEntry.deleteLastChar();
                }
            }
        });
        this.mDeleteButton.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                if (KeyguardPinBasedInputView.this.mPasswordEntry.isEnabled()) {
                    KeyguardPinBasedInputView.this.resetPasswordText(true, true);
                }
                KeyguardPinBasedInputView.this.doHapticKeyClick();
                return true;
            }
        });
        this.mButton0 = findViewById(R.id.key0);
        this.mButton1 = findViewById(R.id.key1);
        this.mButton2 = findViewById(R.id.key2);
        this.mButton3 = findViewById(R.id.key3);
        this.mButton4 = findViewById(R.id.key4);
        this.mButton5 = findViewById(R.id.key5);
        this.mButton6 = findViewById(R.id.key6);
        this.mButton7 = findViewById(R.id.key7);
        this.mButton8 = findViewById(R.id.key8);
        this.mButton9 = findViewById(R.id.key9);
        this.mPasswordEntry.requestFocus();
    }

    public boolean onTouch(View v, MotionEvent event) {
        if (event.getActionMasked() == 0) {
            doHapticKeyClick();
        }
        return false;
    }

    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getAction() == 0) {
            return onKeyDown(keyCode, event);
        }
        return false;
    }
}
