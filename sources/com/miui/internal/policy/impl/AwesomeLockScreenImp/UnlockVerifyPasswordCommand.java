package com.miui.internal.policy.impl.AwesomeLockScreenImp;

import android.os.AsyncTask;
import android.util.Log;
import miui.maml.ActionCommand;
import miui.maml.CommandTriggers;
import miui.maml.data.Expression;
import miui.maml.elements.ScreenElement;
import miui.maml.util.Utils;
import org.w3c.dom.Element;

public class UnlockVerifyPasswordCommand extends ActionCommand {
    private Expression mDelayExp;
    private boolean mEnable;
    private Expression mEnableExp;
    /* access modifiers changed from: private */
    public Expression mPasswordExp;
    /* access modifiers changed from: private */
    public CommandTriggers mTriggers;

    public UnlockVerifyPasswordCommand(ScreenElement screenElement, Element ele) {
        super(screenElement);
        this.mPasswordExp = Expression.build(getVariables(), ele.getAttribute("password"));
        if (this.mPasswordExp == null) {
            Log.e("UnlockVerifyPasswordCommand", "no password");
        }
        this.mDelayExp = Expression.build(getVariables(), ele.getAttribute("unlockDelay"));
        this.mEnableExp = Expression.build(getVariables(), ele.getAttribute("enable"));
        Element triggers = Utils.getChild(ele, "Triggers");
        if (triggers != null) {
            this.mTriggers = new CommandTriggers(triggers, screenElement);
        }
    }

    /* access modifiers changed from: protected */
    public void doPerform() {
        if (this.mEnable) {
            final int delay = (int) (this.mDelayExp == null ? 0.0d : this.mDelayExp.evaluate());
            new AsyncTask<Void, Void, Boolean>() {
                /* access modifiers changed from: protected */
                public Boolean doInBackground(Void... args) {
                    try {
                        return Boolean.valueOf(UnlockVerifyPasswordCommand.this.getRoot().unlockVerify(UnlockVerifyPasswordCommand.this.mPasswordExp.evaluateStr(), delay));
                    } catch (Exception e) {
                        return false;
                    }
                }

                /* access modifiers changed from: protected */
                public void onPostExecute(Boolean result) {
                    if (UnlockVerifyPasswordCommand.this.mTriggers != null) {
                        UnlockVerifyPasswordCommand.this.mTriggers.onAction(result.booleanValue() ? "success" : "fail");
                    }
                }
            }.execute(new Void[0]);
            return;
        }
        getRoot().unlocked(null, 0);
    }

    public void init() {
        UnlockVerifyPasswordCommand.super.init();
        if (this.mTriggers != null) {
            this.mTriggers.init();
        }
        boolean z = true;
        if (this.mPasswordExp == null || ((this.mEnableExp != null && this.mEnableExp.evaluate() <= 0.0d) || getRoot().getPasswordMode() != 1)) {
            z = false;
        }
        this.mEnable = z;
        if (this.mEnable) {
            getRoot().setCapability(7, false);
        }
    }

    public void finish() {
        UnlockVerifyPasswordCommand.super.finish();
        if (this.mTriggers != null) {
            this.mTriggers.finish();
        }
    }

    public void pause() {
        UnlockVerifyPasswordCommand.super.pause();
        if (this.mTriggers != null) {
            this.mTriggers.pause();
        }
    }

    public void resume() {
        UnlockVerifyPasswordCommand.super.resume();
        if (this.mTriggers != null) {
            this.mTriggers.resume();
        }
    }
}
