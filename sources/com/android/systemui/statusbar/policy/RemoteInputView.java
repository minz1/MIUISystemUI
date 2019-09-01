package com.android.systemui.statusbar.policy;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutManager;
import android.content.pm.ShortcutManagerCompat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewCompat;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.statusbar.ExpandableView;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.RemoteInputController;
import com.android.systemui.statusbar.notification.NotificationViewWrapper;
import com.android.systemui.statusbar.stack.ScrollContainer;

public class RemoteInputView extends LinearLayout implements TextWatcher, View.OnClickListener {
    public static final Object VIEW_TAG = new Object();
    private RemoteInputController mController;
    /* access modifiers changed from: private */
    public RemoteEditText mEditText;
    /* access modifiers changed from: private */
    public NotificationData.Entry mEntry;
    private PendingIntent mPendingIntent;
    private ProgressBar mProgressBar;
    private RemoteInput mRemoteInput;
    private RemoteInput[] mRemoteInputs;
    /* access modifiers changed from: private */
    public boolean mRemoved;
    private boolean mResetting;
    private int mRevealCx;
    private int mRevealCy;
    private int mRevealR;
    private ScrollContainer mScrollContainer;
    private View mScrollContainerChild;
    private ImageButton mSendButton;
    public final Object mToken = new Object();
    /* access modifiers changed from: private */
    public NotificationViewWrapper mWrapper;

    public static class RemoteEditText extends EditText {
        private final Drawable mBackground = getBackground();
        /* access modifiers changed from: private */
        public RemoteInputView mRemoteInputView;
        boolean mShowImeOnInputConnection;

        public RemoteEditText(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        /* access modifiers changed from: private */
        public void defocusIfNeeded(boolean animate) {
            if ((this.mRemoteInputView == null || !this.mRemoteInputView.mEntry.row.isChangingPosition()) && !ViewCompat.isTemporarilyDetached(this)) {
                if (isFocusable() && isEnabled()) {
                    setInnerFocusable(false);
                    if (this.mRemoteInputView != null) {
                        this.mRemoteInputView.onDefocus(animate);
                    }
                    this.mShowImeOnInputConnection = false;
                }
                return;
            }
            if (ViewCompat.isTemporarilyDetached(this) && this.mRemoteInputView != null) {
                this.mRemoteInputView.mEntry.remoteInputText = getText();
            }
        }

        /* access modifiers changed from: protected */
        public void onVisibilityChanged(View changedView, int visibility) {
            super.onVisibilityChanged(changedView, visibility);
            if (!isShown()) {
                defocusIfNeeded(false);
            }
        }

        /* access modifiers changed from: protected */
        public void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
            super.onFocusChanged(focused, direction, previouslyFocusedRect);
            if (!focused) {
                defocusIfNeeded(true);
            }
        }

        public void getFocusedRect(Rect r) {
            super.getFocusedRect(r);
            r.top = this.mScrollY;
            r.bottom = this.mScrollY + (this.mBottom - this.mTop);
        }

        public boolean requestRectangleOnScreen(Rect rectangle) {
            return this.mRemoteInputView.requestScrollTo();
        }

        public boolean onKeyDown(int keyCode, KeyEvent event) {
            if (keyCode == 4) {
                return true;
            }
            return super.onKeyDown(keyCode, event);
        }

        public boolean onKeyUp(int keyCode, KeyEvent event) {
            if (keyCode != 4) {
                return super.onKeyUp(keyCode, event);
            }
            defocusIfNeeded(true);
            return true;
        }

        public boolean onCheckIsTextEditor() {
            if ((this.mRemoteInputView != null && this.mRemoteInputView.mRemoved) || !super.onCheckIsTextEditor()) {
                return false;
            }
            return true;
        }

        public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
            InputConnection inputConnection = super.onCreateInputConnection(outAttrs);
            if (this.mShowImeOnInputConnection && inputConnection != null) {
                final InputMethodManager imm = InputMethodManager.getInstance();
                if (imm != null) {
                    post(new Runnable() {
                        public void run() {
                            imm.viewClicked(RemoteEditText.this);
                            imm.showSoftInput(RemoteEditText.this, 0);
                        }
                    });
                }
            }
            return inputConnection;
        }

        public void onCommitCompletion(CompletionInfo text) {
            clearComposingText();
            setText(text.getText());
            setSelection(getText().length());
        }

        /* access modifiers changed from: package-private */
        public void setInnerFocusable(boolean focusable) {
            setFocusableInTouchMode(focusable);
            setFocusable(focusable);
            setCursorVisible(focusable);
            if (focusable) {
                requestFocus();
                setBackground(this.mBackground);
                return;
            }
            setBackground(null);
        }
    }

    public RemoteInputView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mProgressBar = (ProgressBar) findViewById(R.id.remote_input_progress);
        this.mSendButton = (ImageButton) findViewById(R.id.remote_input_send);
        this.mSendButton.setOnClickListener(this);
        this.mEditText = (RemoteEditText) getChildAt(0);
        this.mEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean isSoftImeEvent = event == null && (actionId == 6 || actionId == 5 || actionId == 4);
                boolean isKeyboardEnterKey = event != null && KeyEvent.isConfirmKey(event.getKeyCode()) && event.getAction() == 0;
                if (!isSoftImeEvent && !isKeyboardEnterKey) {
                    return false;
                }
                if (RemoteInputView.this.mEditText.length() > 0) {
                    RemoteInputView.this.sendRemoteInput();
                }
                return true;
            }
        });
        this.mEditText.addTextChangedListener(this);
        this.mEditText.setInnerFocusable(false);
        RemoteInputView unused = this.mEditText.mRemoteInputView = this;
    }

    /* access modifiers changed from: private */
    public void sendRemoteInput() {
        Bundle results = new Bundle();
        results.putString(this.mRemoteInput.getResultKey(), this.mEditText.getText().toString());
        Intent fillInIntent = new Intent().addFlags(268435456);
        RemoteInput.addResultsToIntent(this.mRemoteInputs, fillInIntent, results);
        this.mEditText.setEnabled(false);
        this.mSendButton.setVisibility(4);
        this.mProgressBar.setVisibility(0);
        this.mEntry.remoteInputText = this.mEditText.getText();
        this.mController.addSpinning(this.mEntry.key, this.mToken);
        this.mController.removeRemoteInput(this.mEntry, this.mToken);
        this.mEditText.mShowImeOnInputConnection = false;
        this.mController.remoteInputSent(this.mEntry);
        ShortcutManagerCompat.onApplicationActive((ShortcutManager) getContext().getSystemService(ShortcutManager.class), this.mEntry.notification.getPackageName(), this.mEntry.notification.getUser().getIdentifier());
        MetricsLogger.action(this.mContext, 398, this.mEntry.notification.getPackageName());
        try {
            this.mPendingIntent.send(this.mContext, 0, fillInIntent);
        } catch (PendingIntent.CanceledException e) {
            Log.i("RemoteInput", "Unable to send remote input result", e);
            MetricsLogger.action(this.mContext, 399, this.mEntry.notification.getPackageName());
        }
    }

    public static RemoteInputView inflate(Context context, ViewGroup root, NotificationData.Entry entry, RemoteInputController controller) {
        RemoteInputView v = (RemoteInputView) LayoutInflater.from(context).inflate(R.layout.remote_input, root, false);
        v.mController = controller;
        v.mEntry = entry;
        v.setTag(VIEW_TAG);
        return v;
    }

    public void onClick(View v) {
        if (v == this.mSendButton) {
            sendRemoteInput();
        }
    }

    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        return true;
    }

    /* access modifiers changed from: private */
    public void onDefocus(boolean animate) {
        this.mController.removeRemoteInput(this.mEntry, this.mToken);
        this.mEntry.remoteInputText = this.mEditText.getText();
        if (!this.mRemoved) {
            if (!animate || this.mRevealR <= 0) {
                setVisibility(4);
                if (this.mWrapper != null) {
                    this.mWrapper.setRemoteInputVisible(false);
                }
            } else {
                Animator reveal = ViewAnimationUtils.createCircularReveal(this, this.mRevealCx, this.mRevealCy, (float) this.mRevealR, 0.0f);
                reveal.setInterpolator(Interpolators.FAST_OUT_LINEAR_IN);
                reveal.setDuration(150);
                reveal.addListener(new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator animation) {
                        RemoteInputView.this.setVisibility(4);
                        if (RemoteInputView.this.mWrapper != null) {
                            RemoteInputView.this.mWrapper.setRemoteInputVisible(false);
                        }
                    }
                });
                reveal.start();
            }
        }
        MetricsLogger.action(this.mContext, 400, this.mEntry.notification.getPackageName());
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (this.mEntry.row.isChangingPosition() && getVisibility() == 0 && this.mEditText.isFocusable()) {
            this.mEditText.requestFocus();
        }
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (!this.mEntry.row.isChangingPosition() && !ViewCompat.isTemporarilyDetached(this)) {
            this.mController.removeRemoteInput(this.mEntry, this.mToken);
            this.mController.removeSpinning(this.mEntry.key, this.mToken);
        }
    }

    public void setPendingIntent(PendingIntent pendingIntent) {
        this.mPendingIntent = pendingIntent;
    }

    public void setRemoteInput(RemoteInput[] remoteInputs, RemoteInput remoteInput) {
        this.mRemoteInputs = remoteInputs;
        this.mRemoteInput = remoteInput;
        this.mEditText.setHint(this.mRemoteInput.getLabel());
    }

    public void focusAnimated() {
        if (getVisibility() != 0) {
            Animator animator = ViewAnimationUtils.createCircularReveal(this, this.mRevealCx, this.mRevealCy, 0.0f, (float) this.mRevealR);
            animator.setDuration(360);
            animator.setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN);
            animator.start();
        }
        focus();
    }

    public void focus() {
        MetricsLogger.action(this.mContext, 397, this.mEntry.notification.getPackageName());
        setVisibility(0);
        if (this.mWrapper != null) {
            this.mWrapper.setRemoteInputVisible(true);
        }
        this.mController.addRemoteInput(this.mEntry, this.mToken);
        this.mEditText.setInnerFocusable(true);
        this.mEditText.mShowImeOnInputConnection = true;
        this.mEditText.setText(this.mEntry.remoteInputText);
        this.mEditText.setSelection(this.mEditText.getText().length());
        this.mEditText.requestFocus();
        updateSendButton();
    }

    public void onNotificationUpdateOrReset() {
        if (this.mProgressBar.getVisibility() == 0) {
            reset();
        }
        if (isActive() && this.mWrapper != null) {
            this.mWrapper.setRemoteInputVisible(true);
        }
    }

    private void reset() {
        this.mResetting = true;
        this.mEditText.getText().clear();
        this.mEditText.setEnabled(true);
        this.mSendButton.setVisibility(0);
        this.mProgressBar.setVisibility(4);
        this.mController.removeSpinning(this.mEntry.key, this.mToken);
        updateSendButton();
        onDefocus(false);
        this.mResetting = false;
    }

    public boolean onRequestSendAccessibilityEvent(View child, AccessibilityEvent event) {
        if (!this.mResetting || child != this.mEditText) {
            return super.onRequestSendAccessibilityEvent(child, event);
        }
        return false;
    }

    private void updateSendButton() {
        this.mSendButton.setEnabled(this.mEditText.getText().length() != 0);
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    public void afterTextChanged(Editable s) {
        updateSendButton();
    }

    public void close() {
        this.mEditText.defocusIfNeeded(false);
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == 0) {
            findScrollContainer();
            if (this.mScrollContainer != null) {
                this.mScrollContainer.requestDisallowLongPress();
                this.mScrollContainer.requestDisallowDismiss();
            }
        }
        return super.onInterceptTouchEvent(ev);
    }

    public boolean requestScrollTo() {
        findScrollContainer();
        this.mScrollContainer.lockScrollTo(this.mScrollContainerChild);
        return true;
    }

    private void findScrollContainer() {
        if (this.mScrollContainer == null) {
            this.mScrollContainerChild = null;
            for (ViewParent p = this; p != null; p = p.getParent()) {
                if (this.mScrollContainerChild == null && (p instanceof ExpandableView)) {
                    this.mScrollContainerChild = (View) p;
                }
                if (p.getParent() instanceof ScrollContainer) {
                    this.mScrollContainer = (ScrollContainer) p.getParent();
                    if (this.mScrollContainerChild == null) {
                        this.mScrollContainerChild = (View) p;
                        return;
                    }
                    return;
                }
            }
        }
    }

    public boolean isActive() {
        return this.mEditText.isFocused() && this.mEditText.isEnabled();
    }

    public void stealFocusFrom(RemoteInputView other) {
        other.close();
        setPendingIntent(other.mPendingIntent);
        setRemoteInput(other.mRemoteInputs, other.mRemoteInput);
        setRevealParameters(other.mRevealCx, other.mRevealCy, other.mRevealR);
        focus();
    }

    public boolean updatePendingIntentFromActions(Notification.Action[] actions) {
        if (this.mPendingIntent == null || actions == null) {
            return false;
        }
        Intent current = this.mPendingIntent.getIntent();
        if (current == null) {
            return false;
        }
        for (Notification.Action a : actions) {
            RemoteInput[] inputs = a.getRemoteInputs();
            if (!(a.actionIntent == null || inputs == null || !current.filterEquals(a.actionIntent.getIntent()))) {
                RemoteInput input = null;
                for (RemoteInput i : inputs) {
                    if (i.getAllowFreeFormInput()) {
                        input = i;
                    }
                }
                if (input != null) {
                    setPendingIntent(a.actionIntent);
                    setRemoteInput(inputs, input);
                    return true;
                }
            }
        }
        return false;
    }

    public PendingIntent getPendingIntent() {
        return this.mPendingIntent;
    }

    public void setRemoved() {
        this.mRemoved = true;
    }

    public void setRevealParameters(int cx, int cy, int r) {
        this.mRevealCx = cx;
        this.mRevealCy = cy;
        this.mRevealR = r;
    }

    public void dispatchStartTemporaryDetach() {
        super.dispatchStartTemporaryDetach();
        if (this.mEditText == findFocus()) {
            clearChildFocus(this.mEditText);
        }
        detachViewFromParent(this.mEditText);
    }

    public void dispatchFinishTemporaryDetach() {
        if (isAttachedToWindow()) {
            attachViewToParent(this.mEditText, 0, this.mEditText.getLayoutParams());
        } else {
            removeDetachedView(this.mEditText, false);
        }
        super.dispatchFinishTemporaryDetach();
    }

    public void setWrapper(NotificationViewWrapper wrapper) {
        this.mWrapper = wrapper;
    }
}
