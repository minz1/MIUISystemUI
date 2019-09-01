package com.android.keyguard.doze;

import android.content.Context;
import com.android.keyguard.MiuiKeyguardUtils;
import com.android.keyguard.doze.DozeMachine;
import com.android.systemui.Dependency;

public class MiuiAnimDozeStatePreventingAdapter extends DozeMachine.Service.Delegate {
    private Context mContext;

    private MiuiAnimDozeStatePreventingAdapter(DozeMachine.Service wrappedService, Context context) {
        super(wrappedService);
        this.mContext = context;
    }

    public static DozeMachine.Service wrapIfNeeded(DozeMachine.Service inner, Context context) {
        if (MiuiKeyguardUtils.isAodAnimateEnable(context)) {
            return new MiuiAnimDozeStatePreventingAdapter(inner, context);
        }
        return inner;
    }

    public void setDozeScreenState(int state) {
        if ((state != 2 && state != 3 && state != 4) || Dependency.getHost().isAnimateShowing() || !MiuiKeyguardUtils.isAodClockDisable(this.mContext)) {
            super.setDozeScreenState(state);
        }
    }
}
