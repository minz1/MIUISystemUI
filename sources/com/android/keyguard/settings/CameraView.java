package com.android.keyguard.settings;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Region;
import android.util.AttributeSet;
import android.view.View;
import com.android.systemui.R;

public class CameraView extends View {
    private Path mPath = new Path();
    private boolean mRefreshDifferenceView = false;

    public CameraView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public CameraView(Context context) {
        super(context);
        initView();
    }

    private void initView() {
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    public void draw(Canvas canvas) {
        this.mPath.addOval(new RectF(0.0f, 0.0f, (float) getMeasuredWidth(), (float) getResources().getDimensionPixelSize(R.dimen.keyguard_face_input_camera_height)), Path.Direction.CCW);
        if (this.mRefreshDifferenceView) {
            canvas.clipPath(this.mPath, Region.Op.DIFFERENCE);
            this.mRefreshDifferenceView = false;
        } else {
            canvas.clipPath(this.mPath, Region.Op.INTERSECT);
        }
        super.draw(canvas);
    }

    public void refreshCameraView(boolean difference) {
        this.mRefreshDifferenceView = difference;
        invalidate();
    }
}
