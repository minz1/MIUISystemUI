package com.android.keyguard.widget;

import android.view.View;
import java.util.TimeZone;

public interface IAodClock {
    void bindView(View view);

    int getLayoutResource();

    void setPaint(int i);

    void setTimeZone(TimeZone timeZone);

    void setTimeZone2(TimeZone timeZone);

    void updateTime(boolean z);
}
