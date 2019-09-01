package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import com.android.systemui.plugins.statusbar.phone.NavBarButtonProvider;

public class HomeButtonView extends ImageView implements NavBarButtonProvider.ButtonInterface {
    public HomeButtonView(Context context) {
        super(context);
    }

    public HomeButtonView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HomeButtonView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public HomeButtonView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void abortCurrentGesture() {
    }

    public void setVertical(boolean vertical) {
    }

    public void setCarMode(boolean carMode) {
    }

    public void setDarkIntensity(float darkIntensity) {
        if (getDrawable() != null) {
            ((KeyButtonDrawable) getDrawable()).setDarkIntensity(darkIntensity);
            invalidate();
        }
    }
}
