package com.android.keyguard.doze;

import com.android.keyguard.doze.DozeMachine;
import com.android.keyguard.phone.DozeParameters;

public class DozeSuspendScreenStatePreventingAdapter extends DozeMachine.Service.Delegate {
    DozeSuspendScreenStatePreventingAdapter(DozeMachine.Service inner) {
        super(inner);
    }

    public void setDozeScreenState(int state) {
        if (state == 4) {
            state = 3;
        }
        super.setDozeScreenState(state);
    }

    public static DozeMachine.Service wrapIfNeeded(DozeMachine.Service inner, DozeParameters params) {
        return isNeeded(params) ? new DozeSuspendScreenStatePreventingAdapter(inner) : inner;
    }

    private static boolean isNeeded(DozeParameters params) {
        return !params.getDozeSuspendDisplayStateSupported();
    }
}
