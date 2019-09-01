package com.android.keyguard.doze;

import com.android.keyguard.doze.DozeMachine;
import com.android.keyguard.phone.DozeParameters;

public class DozeScreenStatePreventingAdapter extends DozeMachine.Service.Delegate {
    DozeScreenStatePreventingAdapter(DozeMachine.Service inner) {
        super(inner);
    }

    public void setDozeScreenState(int state) {
        if (state == 3 || state == 4) {
            state = 2;
        }
        super.setDozeScreenState(state);
    }

    public static DozeMachine.Service wrapIfNeeded(DozeMachine.Service inner, DozeParameters params) {
        return isNeeded(params) ? new DozeScreenStatePreventingAdapter(inner) : inner;
    }

    private static boolean isNeeded(DozeParameters params) {
        return !params.getDisplayStateSupported();
    }
}
