package com.android.systemui;

import java.util.Calendar;
import java.util.Date;

public class DateUtils {
    public static int getDigitalFormatDateToday() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        return getDigitalFormatDate(calendar);
    }

    public static int getDigitalPreviousMonthDate() {
        int month;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        int year = calendar.get(1);
        int month2 = calendar.get(2) + 1;
        if (month2 == 1) {
            year--;
            month = 12;
        } else {
            month = month2 - 1;
        }
        return (year * 10000) + (month * 100) + calendar.get(5);
    }

    public static int getDigitalFormatDate(Calendar calendar) {
        return (calendar.get(1) * 10000) + ((calendar.get(2) + 1) * 100) + calendar.get(5);
    }
}
