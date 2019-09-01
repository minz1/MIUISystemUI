package com.android.keyguard;

import android.content.Context;
import android.graphics.Canvas;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.widget.TextView;

public class JustifyTextView extends TextView {
    private int mLineY;
    private int mViewWidth;

    public JustifyTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        TextPaint paint = getPaint();
        paint.setColor(getCurrentTextColor());
        paint.drawableState = getDrawableState();
        this.mViewWidth = getMeasuredWidth();
        String text = getText().toString();
        this.mLineY = (int) getTextSize();
        Layout layout = getLayout();
        if (layout != null) {
            int lineCount = layout.getLineCount();
            for (int i = 0; i < lineCount; i++) {
                int lineStart = layout.getLineStart(i);
                int lineEnd = layout.getLineEnd(i);
                float width = StaticLayout.getDesiredWidth(text, lineStart, lineEnd, getPaint());
                String line = text.substring(lineStart, lineEnd);
                if (i >= lineCount - 1 || !needScale(line)) {
                    canvas.drawText(line, 0.0f, (float) this.mLineY, paint);
                } else {
                    drawScaledText(canvas, line, width);
                }
                this.mLineY += getLineHeight();
            }
        }
    }

    private void drawScaledText(Canvas canvas, String line, float lineWidth) {
        float x = 0.0f;
        if (isFirstLineOfParagraph(line)) {
            canvas.drawText("  ", 0.0f, (float) this.mLineY, getPaint());
            x = 0.0f + StaticLayout.getDesiredWidth("  ", getPaint());
            line = line.substring(3);
        }
        int i = 0;
        if (line.length() > 2 && line.charAt(0) == 12288 && line.charAt(1) == 12288) {
            String substring = line.substring(0, 2);
            float cw = StaticLayout.getDesiredWidth(substring, getPaint());
            canvas.drawText(substring, x, (float) this.mLineY, getPaint());
            x += cw;
            i = 0 + 2;
        }
        String[] words = line.trim().split(" ");
        if (words == null || words.length < 2) {
            float d = (((float) this.mViewWidth) - lineWidth) / ((float) (line.length() - 1));
            while (i < line.length()) {
                String c = String.valueOf(line.charAt(i));
                float cw2 = StaticLayout.getDesiredWidth(c, getPaint());
                canvas.drawText(c, x, (float) this.mLineY, getPaint());
                x += cw2 + d;
                i++;
            }
            return;
        }
        float d2 = (((float) this.mViewWidth) - lineWidth) / ((float) (words.length - 1));
        while (i < words.length) {
            String word = words[i];
            if (i != words.length - 1) {
                word = word + " ";
            }
            float cw3 = StaticLayout.getDesiredWidth(word, getPaint());
            canvas.drawText(word, x, (float) this.mLineY, getPaint());
            x += cw3 + d2;
            i++;
        }
    }

    private boolean isFirstLineOfParagraph(String line) {
        return line.length() > 3 && line.charAt(0) == ' ' && line.charAt(1) == ' ';
    }

    private boolean needScale(String line) {
        boolean z = false;
        if (line == null || line.length() == 0) {
            return false;
        }
        if (line.charAt(line.length() - 1) != 10) {
            z = true;
        }
        return z;
    }
}
