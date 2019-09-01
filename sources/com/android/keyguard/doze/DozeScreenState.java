package com.android.keyguard.doze;

import android.os.Handler;
import com.android.keyguard.doze.DozeMachine;

public class DozeScreenState implements DozeMachine.Part {
    private final Runnable mApplyPendingScreenState = new Runnable() {
        public final void run() {
            DozeScreenState.this.applyPendingScreenState();
        }
    };
    private final DozeMachine.Service mDozeService;
    private final Handler mHandler;
    private int mPendingScreenState = 0;

    public DozeScreenState(DozeMachine.Service service, Handler handler) {
        this.mDozeService = service;
        this.mHandler = handler;
    }

    public void transitionTo(DozeMachine.State oldState, DozeMachine.State newState) {
        int screenState = newState.screenState();
        if (newState == DozeMachine.State.FINISH) {
            this.mPendingScreenState = 0;
            this.mHandler.removeCallbacks(this.mApplyPendingScreenState);
            applyScreenState(screenState);
        } else if (screenState != 0) {
            boolean messagePending = this.mHandler.hasCallbacks(this.mApplyPendingScreenState);
            if (messagePending || oldState == DozeMachine.State.INITIALIZED) {
                this.mPendingScreenState = screenState;
                if (!messagePending) {
                    this.mHandler.post(this.mApplyPendingScreenState);
                }
                return;
            }
            applyScreenState(screenState);
        }
    }

    /* access modifiers changed from: private */
    public void applyPendingScreenState() {
        applyScreenState(this.mPendingScreenState);
        this.mPendingScreenState = 0;
    }

    private void applyScreenState(int screenState) {
        if (screenState != 0) {
            this.mDozeService.setDozeScreenState(screenState);
            this.mPendingScreenState = 0;
        }
    }
}
