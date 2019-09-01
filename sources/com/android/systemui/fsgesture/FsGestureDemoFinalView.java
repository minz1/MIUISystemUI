package com.android.systemui.fsgesture;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import com.android.systemui.R;

public class FsGestureDemoFinalView extends FrameLayout {
    public FsGestureDemoFinalView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FsGestureDemoFinalView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public FsGestureDemoFinalView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        View inflate = LayoutInflater.from(getContext()).inflate(R.layout.fs_gesture_demo_final_view, this);
    }
}
