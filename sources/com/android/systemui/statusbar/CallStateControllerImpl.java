package com.android.systemui.statusbar;

import android.util.SparseArray;

public class CallStateControllerImpl implements CallStateController {
    private SparseArray<Integer> mCallStateArray = new SparseArray<>();
    private int mSimCount;

    public int getCallState(int slot) {
        return this.mCallStateArray.get(slot, 0).intValue();
    }

    public void setCallState(int slot, int state) {
        this.mCallStateArray.put(slot, Integer.valueOf(state));
    }

    public void setSimCount(int count) {
        this.mSimCount = count;
    }

    public boolean isMsim() {
        return this.mSimCount == 2;
    }
}
