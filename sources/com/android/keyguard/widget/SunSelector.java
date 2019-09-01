package com.android.keyguard.widget;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Settings;
import java.util.Calendar;
import java.util.TimeZone;

public class SunSelector {
    private static int[] sChangePoint = {330, 340, 350, 360, 370, 380, 475, 570, 665, 760, 855, 950, 1045, 1060, 1070, 1080, 1090, 1100, 1110, 1205, 1300, 45, 140, 235};
    private static int[] sSunImage = {R.drawable.aod_sun_rise_30b, R.drawable.aod_sun_rise_20b, R.drawable.aod_sun_rise_10b, R.drawable.aod_sun_rise, R.drawable.aod_sun_rise_10a, R.drawable.aod_sun_rise_20a, R.drawable.aod_sun7, R.drawable.aod_sun8, R.drawable.aod_sun9, R.drawable.aod_sun10, R.drawable.aod_sun11, R.drawable.aod_sun12, R.drawable.aod_sun13, R.drawable.aod_sun_set_20b, R.drawable.aod_sun_set_10b, R.drawable.aod_sun_set, R.drawable.aod_sun_set_10a, R.drawable.aod_sun_set_20a, R.drawable.aod_sun_set_30a, R.drawable.aod_sun20, R.drawable.aod_sun21, R.drawable.aod_sun22, R.drawable.aod_sun23, R.drawable.aod_sun24};

    public static int getDrawableIndex(int time) {
        int index = sChangePoint.length - 1;
        for (int i = 0; i < sChangePoint.length - 1; i++) {
            if (time >= sChangePoint[i] && time < sChangePoint[i + 1]) {
                index = i;
            }
            if (sChangePoint[i + 1] < sChangePoint[i] && (time >= sChangePoint[i] || time < sChangePoint[i + 1])) {
                index = i;
            }
        }
        return index;
    }

    public static boolean shouldUpdateSunriseTime(Context context, int dayOfYear) {
        return Settings.System.getInt(context.getContentResolver(), "sunrise_update", 0) != dayOfYear;
    }

    public static int getDrawableLength() {
        return sSunImage.length;
    }

    public static int getChangePointLength() {
        return sChangePoint.length;
    }

    public static void updateSunRiseTime(Context context) {
        Calendar calendar = Calendar.getInstance();
        int dayOfYear = calendar.get(6);
        if (shouldUpdateSunriseTime(context, dayOfYear)) {
            Cursor cursor = context.getContentResolver().query(Uri.parse("content://weather/weather"), null, null, null, null);
            if (cursor != null) {
                try {
                    cursor.moveToFirst();
                    long sunrise = cursor.getLong(cursor.getColumnIndex("sunrise"));
                    long sunset = cursor.getLong(cursor.getColumnIndex("sunset"));
                    TimeZone timeZone = TimeZone.getDefault();
                    long sunrise2 = getTimeWithOnlyHourAndMinute(calendar, sunrise, timeZone);
                    long sunset2 = getTimeWithOnlyHourAndMinute(calendar, sunset, timeZone);
                    Settings.System.putLong(context.getContentResolver(), "sunrise", sunrise2);
                    Settings.System.putLong(context.getContentResolver(), "sunset", sunset2);
                    updateChangepoint(sunrise2, sunset2);
                    Settings.System.putInt(context.getContentResolver(), "sunrise_update", dayOfYear);
                } catch (Exception e) {
                    e.printStackTrace();
                } catch (Throwable th) {
                    cursor.close();
                    throw th;
                }
                cursor.close();
                return;
            }
            return;
        }
        updateChangepoint(Settings.System.getLong(context.getContentResolver(), "sunrise", 360), Settings.System.getLong(context.getContentResolver(), "sunset", 1080));
    }

    private static void updateChangepoint(long sunrise, long sunset) {
        sChangePoint[3] = (int) (sunrise / 60000);
        sChangePoint[15] = (int) (sunset / 60000);
        int dayGap = ((sChangePoint[15] - sChangePoint[3]) - 40) / 8;
        int nightGap = (((sChangePoint[3] + 1440) - sChangePoint[15]) - 60) / 6;
        int i = 0;
        sChangePoint[0] = sChangePoint[3] - 30;
        sChangePoint[1] = sChangePoint[3] - 20;
        sChangePoint[2] = sChangePoint[3] - 10;
        while (true) {
            int i2 = i;
            if (i2 > 5) {
                break;
            }
            sChangePoint[i2] = sChangePoint[3] + ((i2 - 3) * 10);
            i = i2 + 1;
        }
        for (int i3 = 6; i3 <= 12; i3++) {
            sChangePoint[i3] = sChangePoint[3] + 20 + ((i3 - 5) * dayGap);
        }
        for (int i4 = 13; i4 <= 18; i4++) {
            sChangePoint[i4] = sChangePoint[15] + ((i4 - 15) * 10);
        }
        for (int i5 = 19; i5 <= 23; i5++) {
            sChangePoint[i5] = sChangePoint[15] + 30 + ((i5 - 18) * nightGap);
            if (sChangePoint[i5] > 1440) {
                sChangePoint[i5] = sChangePoint[i5] - 1440;
            }
        }
    }

    private static long getTimeWithOnlyHourAndMinute(Calendar calendar, long time, TimeZone timeZone) {
        calendar.setTimeZone(timeZone);
        calendar.setTimeInMillis(time);
        int hour = calendar.get(11);
        int minute = calendar.get(12);
        calendar.clear();
        return ((long) ((hour * 60) + minute)) * 60000;
    }

    public static int getSunImage(int index) {
        return sSunImage[index];
    }

    public static int getChangePoint(int index) {
        return sChangePoint[index];
    }
}
