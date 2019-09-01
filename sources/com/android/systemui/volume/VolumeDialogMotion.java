package com.android.systemui.volume;

public class VolumeDialogMotion {
    private static final String TAG = Util.logTag(VolumeDialogMotion.class);

    public interface Callback {
        void onAnimatingChanged(boolean z);
    }
}
