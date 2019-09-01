package com.android.keyguard.charge.rapid;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

public class NumberDrawView extends View {
    private String mContent;
    private Paint.FontMetrics mFontMetrics;
    private TextPaint mLinePaint;

    public NumberDrawView(Context context) {
        this(context, null);
    }

    public NumberDrawView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NumberDrawView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mContent = "1234567890";
        init(context);
    }

    private void init(Context context) {
        this.mLinePaint = new TextPaint(1);
        this.mLinePaint.setColor(-1);
        Typeface typeface = Typeface.createFromAsset(context.getAssets(), "fonts/Mitype2018-35.otf");
        if (typeface != null) {
            this.mLinePaint.setTypeface(typeface);
        }
        this.mFontMetrics = this.mLinePaint.getFontMetrics();
    }

    public void setTextSize(int unit, float size) {
        Resources r;
        Context c = getContext();
        if (c == null) {
            r = Resources.getSystem();
        } else {
            r = c.getResources();
        }
        float textSize = TypedValue.applyDimension(unit, size, r.getDisplayMetrics());
        if (textSize != this.mLinePaint.getTextSize()) {
            this.mLinePaint.setTextSize(textSize);
            this.mFontMetrics = this.mLinePaint.getFontMetrics();
            requestLayout();
        }
    }

    public void setTypeface(Typeface typeFace) {
        if (typeFace != null) {
            this.mLinePaint.setTypeface(typeFace);
            requestLayout();
        }
    }

    public void setTextColor(int color) {
        this.mLinePaint.setColor(color);
        invalidate();
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawText(this.mContent, 0.0f, Math.abs(this.mFontMetrics.top), this.mLinePaint);
    }

    public void setText(String text) {
        if (TextUtils.isEmpty(text)) {
            this.mContent = "";
        } else if (!text.equals(this.mContent)) {
            this.mContent = text;
            invalidate();
            requestLayout();
        }
    }

    private int getStringLength() {
        int length;
        if (TextUtils.isEmpty(this.mContent)) {
            return 0;
        }
        if (this.mContent.equals("%")) {
            length = (int) this.mLinePaint.measureText("%");
        } else if (this.mContent.length() == 1) {
            length = (int) this.mLinePaint.measureText("8");
        } else if (this.mContent.length() == 2) {
            length = (int) this.mLinePaint.measureText("88");
        } else if (this.mContent.length() == 3) {
            length = (int) this.mLinePaint.measureText("100");
        } else {
            length = (int) this.mLinePaint.measureText(this.mContent);
        }
        return length;
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(getStringLength(), (int) (Math.abs(this.mFontMetrics.top) + 3.0f));
    }
}
