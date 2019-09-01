package com.android.systemui.util;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.text.SpannableStringBuilder;
import android.text.style.TextAppearanceSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.systemui.Constants;
import com.android.systemui.R;
import com.android.systemui.util.function.Consumer;
import java.util.Arrays;
import java.util.List;

public class Utils {
    public static <T> void safeForeach(List<T> list, Consumer<T> c) {
        for (int i = list.size() - 1; i >= 0; i--) {
            c.accept(list.get(i));
        }
    }

    public static void makeSenderSpanBold(TextView textView) {
        SpannableStringBuilder builder = new SpannableStringBuilder(textView.getText());
        Object[] spans = builder.getSpans(0, builder.length(), Object.class);
        int length = spans.length;
        int i = 0;
        while (i < length) {
            Object span = spans[i];
            int start = builder.getSpanStart(span);
            int end = builder.getSpanEnd(span);
            if (!(span instanceof TextAppearanceSpan) || start != 0) {
                i++;
            } else {
                TextAppearanceSpan textAppearanceSpan = (TextAppearanceSpan) span;
                TextAppearanceSpan textAppearanceSpan2 = new TextAppearanceSpan(Constants.IS_INTERNATIONAL ? null : "miui", 1, textAppearanceSpan.getTextSize(), textAppearanceSpan.getTextColor(), textAppearanceSpan.getLinkTextColor());
                builder.setSpan(textAppearanceSpan2, start, end, 0);
                textView.setText(builder);
                return;
            }
        }
    }

    public static void makeSenderSpanBold(ViewGroup container) {
        int childCount = container.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = container.getChildAt(i);
            if (child instanceof TextView) {
                makeSenderSpanBold((TextView) child);
            }
        }
    }

    public static void updateFsgState(Context context, String typeFrom, boolean disable) {
        Intent intent = new Intent();
        intent.setPackage("com.android.systemui");
        intent.setAction("com.android.systemui.fsgesture");
        intent.putExtra("typeFrom", typeFrom);
        intent.putExtra("isEnter", disable);
        intent.addFlags(67108864);
        context.sendBroadcast(intent);
    }

    public static int getColorAttr(Context context, int attr) {
        TypedArray ta = context.obtainStyledAttributes(new int[]{attr});
        int colorAccent = ta.getColor(0, 0);
        ta.recycle();
        return colorAccent;
    }

    public static int getColorAccent(Context context) {
        return getColorAttr(context, 16843829);
    }

    public static int getColorError(Context context) {
        return context.getColor(R.color.color_error);
    }

    public static int getDefaultColor(Context context, int resId) {
        return context.getResources().getColorStateList(resId, context.getTheme()).getDefaultColor();
    }

    public static <T> T[] arrayConcat(T[] first, T[] second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
}
