package com.android.keyguard.fod;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Region;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import com.android.keyguard.MiuiKeyguardUtils;
import com.android.keyguard.fod.item.AddEventItem;
import com.android.keyguard.fod.item.AlipayPayItem;
import com.android.keyguard.fod.item.AlipayScanItem;
import com.android.keyguard.fod.item.IQuickOpenItem;
import com.android.keyguard.fod.item.QrCodeItem;
import com.android.keyguard.fod.item.SearchItem;
import com.android.keyguard.fod.item.WechatPayItem;
import com.android.keyguard.fod.item.WechatScanItem;
import com.android.keyguard.fod.item.XiaoaiItem;
import com.android.systemui.R;
import java.util.ArrayList;
import java.util.List;
import miui.os.Build;

public class MiuiGxzwQuickOpenUtil {
    private static final int[] DEFAULT_ITEM_ID_LIST;
    private static final boolean SUPPORT_QUICK_OPEN = MiuiKeyguardUtils.isGxzwSensor();
    /* access modifiers changed from: private */
    public static int sShowQuickOpenPressCount = -1;
    /* access modifiers changed from: private */
    public static long sShowQuickOpenSlideTime = -1;
    /* access modifiers changed from: private */
    public static int sShowQuickOpenTeachValue = -1;

    static {
        int[] iArr;
        if (Build.IS_INTERNATIONAL_BUILD) {
            iArr = new int[]{1006, 1007, 1008};
        } else {
            iArr = new int[]{1001, 1002, 1003, 1005, 1004};
        }
        DEFAULT_ITEM_ID_LIST = iArr;
    }

    public static boolean isQuickOpenEnable(Context context) {
        return SUPPORT_QUICK_OPEN && Settings.Secure.getIntForUser(context.getContentResolver(), "fod_quick_open", 1, 0) == 1;
    }

    public static void setFodAuthFingerprint(Context context, int fingerId, int userId) {
        if (SUPPORT_QUICK_OPEN) {
            Settings.Secure.putIntForUser(context.getContentResolver(), "fod_auth_fingerprint", fingerId, userId);
        }
    }

    public static void loadSharedPreferencesValue(final Context context) {
        if (SUPPORT_QUICK_OPEN) {
            final Handler handler = new Handler(Looper.getMainLooper());
            AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
                public void run() {
                    SharedPreferences sp = context.getSharedPreferences("quick_open", 0);
                    int showQuickOpenPressCount = sp.getInt("sp_fod_show_quick_open_press_count", 0);
                    long showQuickOpenSlideTime = sp.getLong("sp_fod_show_quick_open_slide_time", 0);
                    int showQuickOpenTeachValue = sp.getInt("sp_fod_show_quick_open_teach", 1);
                    Handler handler = handler;
                    final int i = showQuickOpenPressCount;
                    final long j = showQuickOpenSlideTime;
                    final int i2 = showQuickOpenTeachValue;
                    AnonymousClass1 r3 = new Runnable() {
                        public void run() {
                            int unused = MiuiGxzwQuickOpenUtil.sShowQuickOpenPressCount = i;
                            long unused2 = MiuiGxzwQuickOpenUtil.sShowQuickOpenSlideTime = j;
                            int unused3 = MiuiGxzwQuickOpenUtil.sShowQuickOpenTeachValue = i2;
                        }
                    };
                    handler.post(r3);
                }
            });
        }
    }

    public static boolean isShowQuickOpenPress(Context context) {
        boolean z = false;
        if (!SUPPORT_QUICK_OPEN) {
            return false;
        }
        if (sShowQuickOpenPressCount == -1) {
            sShowQuickOpenPressCount = context.getSharedPreferences("quick_open", 0).getInt("sp_fod_show_quick_open_press_count", 0);
        }
        if (sShowQuickOpenPressCount < 5 && isShowQuickOpenSlide(context)) {
            z = true;
        }
        return z;
    }

    public static void increaseShowQuickOpenPressCount(final Context context) {
        if (SUPPORT_QUICK_OPEN) {
            if (sShowQuickOpenPressCount == -1) {
                sShowQuickOpenPressCount = context.getSharedPreferences("quick_open", 0).getInt("sp_fod_show_quick_open_press_count", 0);
            }
            sShowQuickOpenPressCount++;
            if (sShowQuickOpenPressCount > 5) {
                sShowQuickOpenPressCount = 5;
            }
            final int count = sShowQuickOpenPressCount;
            AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
                public void run() {
                    SharedPreferences.Editor editor = context.getSharedPreferences("quick_open", 0).edit();
                    editor.putInt("sp_fod_show_quick_open_press_count", count);
                    editor.commit();
                }
            });
        }
    }

    public static boolean isShowQuickOpenSlide(Context context) {
        boolean z = false;
        if (!SUPPORT_QUICK_OPEN) {
            return false;
        }
        if (sShowQuickOpenSlideTime == -1) {
            sShowQuickOpenSlideTime = context.getSharedPreferences("quick_open", 0).getLong("sp_fod_show_quick_open_slide_time", 0);
        }
        if (sShowQuickOpenSlideTime <= 0) {
            z = true;
        }
        return z;
    }

    public static void disableShowQuickOpenSlide(final Context context) {
        if (SUPPORT_QUICK_OPEN) {
            if (sShowQuickOpenSlideTime == -1) {
                sShowQuickOpenSlideTime = context.getSharedPreferences("quick_open", 0).getLong("sp_fod_show_quick_open_slide_time", 0);
            }
            if (sShowQuickOpenSlideTime <= 0) {
                final long time = System.currentTimeMillis();
                sShowQuickOpenSlideTime = time;
                AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
                    public void run() {
                        SharedPreferences.Editor editor = context.getSharedPreferences("quick_open", 0).edit();
                        editor.putLong("sp_fod_show_quick_open_slide_time", time);
                        editor.commit();
                    }
                });
            }
        }
    }

    public static boolean isShowQuickOpenTeach(Context context) {
        boolean z = false;
        if (!SUPPORT_QUICK_OPEN) {
            return false;
        }
        if (sShowQuickOpenTeachValue == -1) {
            sShowQuickOpenTeachValue = context.getSharedPreferences("quick_open", 0).getInt("sp_fod_show_quick_open_teach", 1);
        }
        if (sShowQuickOpenTeachValue == 1) {
            z = true;
        }
        return z;
    }

    public static void disableShowQuickOpenTeach(final Context context) {
        if (SUPPORT_QUICK_OPEN && sShowQuickOpenTeachValue != 0) {
            sShowQuickOpenTeachValue = 0;
            AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
                public void run() {
                    SharedPreferences.Editor editor = context.getSharedPreferences("quick_open", 0).edit();
                    editor.putInt("sp_fod_show_quick_open_teach", 0);
                    editor.commit();
                }
            });
        }
    }

    public static float getLargeItemDetal(Context context) {
        switch (DEFAULT_ITEM_ID_LIST.length) {
            case 3:
                return 10.0f;
            case 4:
                return 5.0f;
            default:
                return 0.0f;
        }
    }

    public static List<IQuickOpenItem> generateQuickOpenItemList(Context context, float itemRadius, float circleRadius, boolean isRTL) {
        float f = circleRadius;
        List<IQuickOpenItem> list = new ArrayList<>();
        if (!SUPPORT_QUICK_OPEN) {
            return list;
        }
        MiuiGxzwUtils.caculateGxzwIconSize(context);
        int centerX = MiuiGxzwUtils.GXZW_ICON_X + (MiuiGxzwUtils.GXZW_ICON_WIDTH / 2);
        int centerY = MiuiGxzwUtils.GXZW_ICON_Y + (MiuiGxzwUtils.GXZW_ICON_HEIGHT / 2);
        float bigRadius = context.getResources().getDimension(R.dimen.gxzw_quick_open_region_big);
        float smallRadius = context.getResources().getDimension(R.dimen.gxzw_quick_open_region_samll);
        RectF bigRect = new RectF(((float) centerX) - bigRadius, ((float) centerY) - bigRadius, ((float) centerX) + bigRadius, ((float) centerY) + bigRadius);
        RectF smallRect = new RectF(((float) centerX) - smallRadius, ((float) centerY) - smallRadius, ((float) centerX) + smallRadius, ((float) centerY) + smallRadius);
        Region bigRegion = new Region((int) bigRect.left, (int) bigRect.top, (int) bigRect.right, (int) bigRect.bottom);
        int num = DEFAULT_ITEM_ID_LIST.length;
        float detal = getAngleDetal(num);
        float startAngle = ((180.0f - ((((float) num) - 1.0f) * detal)) / 2.0f) - 0.024902344f;
        float startRegionAngle = -180.0f + ((180.0f - (((float) num) * detal)) / 2.0f);
        int i = 0;
        while (i < num) {
            float angle = (((float) i) * detal) + startAngle;
            float bigRadius2 = bigRadius;
            float regionAngle = (((float) i) * detal) + startRegionAngle;
            float x = getCircleCoordinateX(centerX, f, angle);
            float y = getCircleCoordinateY(centerY, f, angle);
            float smallRadius2 = smallRadius;
            float startAngle2 = startAngle;
            float f2 = angle;
            float startRegionAngle2 = startRegionAngle;
            RectF rectF = new RectF(x - itemRadius, y - itemRadius, x + itemRadius, y + itemRadius);
            Path bigPath = new Path();
            Path smallPath = new Path();
            bigPath.moveTo((float) centerX, (float) centerY);
            smallPath.moveTo((float) centerX, (float) centerY);
            bigPath.arcTo(bigRect, regionAngle, detal, false);
            smallPath.arcTo(smallRect, regionAngle, detal, false);
            bigPath.close();
            smallPath.close();
            bigPath.op(smallPath, Path.Op.DIFFERENCE);
            Region region = new Region();
            region.setPath(bigPath, bigRegion);
            int index = i;
            if (isRTL) {
                index = (num - i) - 1;
            }
            list.add(generateQuickOpenItem(rectF, region, context, DEFAULT_ITEM_ID_LIST[index]));
            i++;
            bigRadius = bigRadius2;
            smallRadius = smallRadius2;
            startAngle = startAngle2;
            startRegionAngle = startRegionAngle2;
            f = circleRadius;
        }
        Context context2 = context;
        float f3 = bigRadius;
        float f4 = smallRadius;
        float f5 = startAngle;
        float f6 = startRegionAngle;
        return list;
    }

    private static float getAngleDetal(int num) {
        if (num == 5) {
            return 45.0f;
        }
        return 60.0f;
    }

    public static float getTeachViewRotation(int total) {
        if (total % 2 != 0) {
            return 0.0f;
        }
        float detal = getAngleDetal(total);
        return 90.0f + (((float) ((total / 2) - 1)) * detal) + -180.0f + ((180.0f - ((((float) total) - 1.0f) * detal)) / 2.0f);
    }

    private static float getCircleCoordinateX(int x, float r, float a) {
        return ((float) x) + ((float) (((double) r) * Math.cos((((double) a) * 3.14d) / 180.0d)));
    }

    private static float getCircleCoordinateY(int y, float r, float a) {
        return ((float) y) + ((float) (((double) r) * Math.sin((((double) a) * 3.14d) / 180.0d)));
    }

    private static IQuickOpenItem generateQuickOpenItem(RectF rectF, Region region, Context context, int id) {
        switch (id) {
            case 1001:
                return new WechatPayItem(rectF, region, context);
            case 1002:
                return new WechatScanItem(rectF, region, context);
            case 1003:
                return new XiaoaiItem(rectF, region, context);
            case 1004:
                return new AlipayPayItem(rectF, region, context);
            case 1005:
                return new AlipayScanItem(rectF, region, context);
            case 1006:
                return new QrCodeItem(rectF, region, context);
            case 1007:
                return new SearchItem(rectF, region, context);
            case 1008:
                return new AddEventItem(rectF, region, context);
            default:
                throw new UnsupportedOperationException();
        }
    }
}
