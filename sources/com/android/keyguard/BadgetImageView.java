package com.android.keyguard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.ImageView;
import com.android.systemui.R;
import java.util.Calendar;

public class BadgetImageView extends ImageView {
    private int mBadget;
    private Paint mBadgetPaint = new Paint();
    private int mIconStyle = 0;
    private int mTextMargin;
    private int mTextSize;

    public BadgetImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setBadget(int badget, int style) {
        this.mBadget = badget;
        this.mIconStyle = style;
        this.mBadgetPaint.reset();
        this.mBadgetPaint.setTextAlign(Paint.Align.CENTER);
        this.mBadgetPaint.setAntiAlias(true);
        if (this.mIconStyle == 1) {
            this.mBadget = Calendar.getInstance().get(5);
            this.mBadgetPaint.setColor(0);
            this.mBadgetPaint.setTypeface(Typeface.createFromAsset(getContext().getAssets(), "fonts/Mitype2018-90.otf"));
            this.mTextSize = getContext().getResources().getDimensionPixelSize(R.dimen.badget_text_size_calendar);
            this.mTextMargin = getContext().getResources().getDimensionPixelSize(R.dimen.badget_text_margin_calendar);
        } else {
            this.mBadgetPaint.setColor(getContext().getColor(R.color.badget_text_color));
            this.mTextSize = getContext().getResources().getDimensionPixelSize(R.dimen.badget_text_size);
        }
        this.mBadgetPaint.setTextSize((float) this.mTextSize);
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.mBadget != 0 && this.mIconStyle == 0) {
            canvas.drawText(String.valueOf(this.mBadget), (float) (getWidth() - 50), 60.0f, this.mBadgetPaint);
        } else if (this.mIconStyle == 1) {
            String text = String.valueOf(this.mBadget);
            this.mBadgetPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OUT));
            canvas.drawText(text, (float) (getWidth() / 2), (float) (((getHeight() + this.mTextSize) / 2) - this.mTextMargin), this.mBadgetPaint);
        }
    }
}
