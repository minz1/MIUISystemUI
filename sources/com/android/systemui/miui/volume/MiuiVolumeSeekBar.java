package com.android.systemui.miui.volume;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import com.android.systemui.miui.widget.RelativeSeekBarInjector;
import miui.widget.VerticalSeekBar;

public class MiuiVolumeSeekBar extends VerticalSeekBar {
    private RelativeSeekBarInjector mInjector = new RelativeSeekBarInjector(this, true);

    /* JADX WARNING: type inference failed for: r2v0, types: [android.widget.SeekBar, com.android.systemui.miui.volume.MiuiVolumeSeekBar] */
    public MiuiVolumeSeekBar(Context context) {
        super(context);
    }

    /* JADX WARNING: type inference failed for: r2v0, types: [android.widget.SeekBar, com.android.systemui.miui.volume.MiuiVolumeSeekBar] */
    public MiuiVolumeSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /* JADX WARNING: type inference failed for: r2v0, types: [android.widget.SeekBar, com.android.systemui.miui.volume.MiuiVolumeSeekBar] */
    public MiuiVolumeSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public boolean onTouchEvent(MotionEvent event) {
        this.mInjector.transformTouchEvent(event);
        return MiuiVolumeSeekBar.super.onTouchEvent(event);
    }

    public boolean hasOverlappingRendering() {
        return false;
    }
}
