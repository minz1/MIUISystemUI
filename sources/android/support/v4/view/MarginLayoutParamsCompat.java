package android.support.v4.view;

import android.os.Build;
import android.view.ViewGroup;

public final class MarginLayoutParamsCompat {
    public static int getMarginStart(ViewGroup.MarginLayoutParams lp) {
        if (Build.VERSION.SDK_INT >= 17) {
            return lp.getMarginStart();
        }
        return lp.leftMargin;
    }

    public static int getMarginEnd(ViewGroup.MarginLayoutParams lp) {
        if (Build.VERSION.SDK_INT >= 17) {
            return lp.getMarginEnd();
        }
        return lp.rightMargin;
    }
}
