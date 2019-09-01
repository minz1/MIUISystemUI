package com.android.keyguard.smartcover;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.ImageView;
import com.android.systemui.R;

public class BatteryLevelImageView extends ImageView {
    private int mLevel;
    private int mPadding;

    public BatteryLevelImageView(Context context) {
        this(context, null);
    }

    public BatteryLevelImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BatteryLevelImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mPadding = getContext().getResources().getDimensionPixelSize(R.dimen.smart_cover_battery_padding);
    }

    public void setBatteryLevel(int level) {
        this.mLevel = level;
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        int saveCount = canvas.getSaveCount();
        canvas.save();
        canvas.clipRect(this.mPaddingLeft, this.mPadding + ((int) ((((double) (((this.mBottom - this.mTop) - (2 * this.mPadding)) * (100 - this.mLevel))) * 1.0d) / 100.0d)), (this.mRight - this.mLeft) - this.mPaddingRight, (this.mBottom - this.mTop) - this.mPaddingBottom);
        canvas.translate((float) this.mPaddingLeft, (float) this.mPaddingTop);
        getDrawable().draw(canvas);
        canvas.restoreToCount(saveCount);
    }
}
