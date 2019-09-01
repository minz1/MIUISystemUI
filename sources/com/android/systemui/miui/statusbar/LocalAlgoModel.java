package com.android.systemui.miui.statusbar;

import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.systemui.DateUtils;
import com.android.systemui.Dependency;
import com.android.systemui.miui.statusbar.analytics.SystemUIStat;
import com.android.systemui.miui.statusbar.notification.FoldBucketHelper;
import com.android.systemui.miui.statusbar.notification.NotificationUtil;
import com.android.systemui.miui.statusbar.notification.PushEvents;
import com.android.systemui.miui.statusbar.notification.ScoreInfo;
import com.android.systemui.miui.statusbar.phone.rank.PackageScoreCache;
import com.android.systemui.miui.statusbar.phone.rank.RankUtil;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class LocalAlgoModel {
    static double b = 0.0d;
    static double[] cg = null;
    static double[] cgw = null;
    static double[] cl = null;
    static double[] clw = null;
    static double[] eg = null;
    static double[] egw = null;
    static double[] el = null;
    static double[] elw = null;
    private static long mLastUpdateTimeStamp = 0;
    private static long mOldestUpdateTimeStamp = 0;
    private static HashSet<String> sPkgs = new HashSet<>();
    private static HashMap<String, List<LocalScoreRule>> sRules = new HashMap<>();
    static double th = 0.0d;
    static double v = 0.0d;
    static double[] w = new double[6];

    static {
        sPkgs.add("com.tencent.mm");
        sPkgs.add("com.tencent.mobileqq");
        sPkgs.add("com.android.mms");
        sPkgs.add("com.android.calendar");
        sPkgs.add("com.whatsapp");
        sPkgs.add("com.facebook.orca");
        sPkgs.add("com.google.android.gm");
        sPkgs.add("com.google.android.calendar");
        sPkgs.add("com.android.deskclock");
        sPkgs.add("com.android.phone");
        sPkgs.add("com.android.stk");
        sPkgs.add("com.android.cellbroadcastreceiver");
        sPkgs.add("com.android.incallui");
        sPkgs.add("com.android.server.telecom");
        sPkgs.add("com.android.email");
        sPkgs.add("com.xiaomi.channel");
        sPkgs.add("com.android.updater");
        sPkgs.add("com.android.settings");
        sPkgs.add("com.miui.player");
        sPkgs.add("com.miui.bugreport");
        sPkgs.add("com.eg.android.AlipayGphone");
        sPkgs.add("com.xiaomi.market");
        sPkgs.add("com.android.providers.downloads");
    }

    public static void updateLocalModelIfNeed(final Context context, ExpandedNotification notification, Handler bgHandler) {
        if (needUpdateLocalModel()) {
            final ScoreInfo scoreInfo = PushEvents.getScoreInfo(notification.getNotification());
            if (scoreInfo != null) {
                bgHandler.post(new Runnable() {
                    public void run() {
                        LocalAlgoModel.updateLocalModel(context, scoreInfo);
                        LocalAlgoModel.recordUpdateTime(context);
                    }
                });
            }
        }
    }

    public static boolean needUpdateLocalModel() {
        return System.currentTimeMillis() - mLastUpdateTimeStamp > 3600000;
    }

    public static void updateLocalModel(Context context, ScoreInfo scoreInfo) {
        String extraInfo = scoreInfo.getExtraInfo();
        if (!TextUtils.isEmpty(extraInfo)) {
            try {
                cacheLocalModel(new JSONObject(extraInfo.substring(1, extraInfo.length() - 1)));
                if (scoreInfo.getSortDelay() > 0) {
                    RankUtil.sNewNotification = scoreInfo.getSortDelay();
                }
                if (scoreInfo.getGroupInterval() > 0) {
                    RankUtil.sGap = scoreInfo.getGroupInterval();
                }
                if (scoreInfo.getCount() > 0) {
                    RankUtil.UNFLOD_LIMIT = scoreInfo.getCount();
                }
                PushEvents.persistLocalModel(context, scoreInfo);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public static void recordUpdateTime(Context context) {
        mLastUpdateTimeStamp = System.currentTimeMillis();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putLong("update_time", mLastUpdateTimeStamp).apply();
        if (mOldestUpdateTimeStamp == 0) {
            mOldestUpdateTimeStamp = mLastUpdateTimeStamp;
            sp.edit().putLong("oldest_update_time", mOldestUpdateTimeStamp).apply();
        }
    }

    public static void restoreUpdateTime(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        if (sp.contains("update_time")) {
            mLastUpdateTimeStamp = sp.getLong("update_time", 0);
        }
        if (sp.contains("oldest_update_time")) {
            mOldestUpdateTimeStamp = sp.getLong("oldest_update_time", 0);
        }
    }

    private static double diff() {
        if (mLastUpdateTimeStamp == 0 || mOldestUpdateTimeStamp == 0) {
            return 0.0d;
        }
        return (1.0d * ((double) (mLastUpdateTimeStamp - mOldestUpdateTimeStamp))) / 8.64E7d;
    }

    private static double getIncreasePercent() {
        double diff = diff();
        if (diff >= 14.0d || diff < 0.0d) {
            return 1.0d;
        }
        return diff / 14.0d;
    }

    public static boolean isLocalModelAvailable() {
        if (!FoldBucketHelper.allowFold()) {
            return false;
        }
        for (double d : w) {
            if (d != 0.0d) {
                return true;
            }
        }
        return false;
    }

    public static void cacheLocalModel(JSONObject jsonObject) {
        try {
            v = jsonObject.optDouble("v", 0.0d);
            b = jsonObject.optDouble("b", 0.0d);
            th = jsonObject.optDouble("th", 0.0d);
            JSONArray wArray = jsonObject.optJSONArray("w");
            if (wArray != null) {
                int i = 0;
                while (i < wArray.length() && i < 6) {
                    w[i] = wArray.optDouble(i, 0.0d);
                    i++;
                }
            }
            JSONArray elArray = jsonObject.optJSONArray("el");
            if (elArray != null) {
                el = new double[elArray.length()];
                elw = new double[elArray.length()];
                for (int i2 = 0; i2 < elArray.length(); i2++) {
                    JSONArray elElem = elArray.optJSONArray(i2);
                    if (elElem != null) {
                        el[i2] = elElem.optDouble(0, 0.0d);
                        elw[i2] = elElem.optDouble(1, 0.0d);
                    }
                }
            }
            JSONArray egArray = jsonObject.optJSONArray("eg");
            if (egArray != null) {
                eg = new double[egArray.length()];
                egw = new double[egArray.length()];
                for (int i3 = 0; i3 < egArray.length(); i3++) {
                    JSONArray egElem = egArray.optJSONArray(i3);
                    if (egElem != null) {
                        eg[i3] = egElem.optDouble(0, 0.0d);
                        egw[i3] = egElem.optDouble(1, 0.0d);
                    }
                }
            }
            JSONArray clArray = jsonObject.getJSONArray("cl");
            if (clArray != null) {
                cl = new double[clArray.length()];
                clw = new double[clArray.length()];
                for (int i4 = 0; i4 < clArray.length(); i4++) {
                    JSONArray clElem = clArray.optJSONArray(i4);
                    if (clElem != null) {
                        cl[i4] = clElem.optDouble(0, 0.0d);
                        clw[i4] = clElem.optDouble(1, 0.0d);
                    }
                }
            }
            JSONArray cgArray = jsonObject.getJSONArray("cg");
            if (cgArray != null) {
                cg = new double[cgArray.length()];
                cgw = new double[cgArray.length()];
                for (int i5 = 0; i5 < cgArray.length(); i5++) {
                    JSONArray cgAElem = cgArray.optJSONArray(i5);
                    if (cgAElem != null) {
                        cg[i5] = cgAElem.optDouble(0, 0.0d);
                        cgw[i5] = cgAElem.optDouble(1, 0.0d);
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static boolean hasLocalRules() {
        return !sRules.isEmpty();
    }

    public static void updateRules(HashMap<String, List<LocalScoreRule>> rules) {
        sRules.clear();
        sRules.putAll(rules);
    }

    private static int matchRules(String key, Notification notification) {
        if (!sRules.containsKey(key)) {
            return Integer.MIN_VALUE;
        }
        CharSequence title = NotificationUtil.resolveTitle(notification);
        CharSequence text = NotificationUtil.resolveText(notification);
        for (LocalScoreRule rule : sRules.get(key)) {
            if (TextUtils.isEmpty(rule.title) && TextUtils.isEmpty(rule.desc)) {
                return rule.score;
            }
            if (!TextUtils.isEmpty(title) && !TextUtils.isEmpty(text)) {
                try {
                    if (Pattern.matches(rule.title, title) && Pattern.matches(rule.desc, text)) {
                        return rule.score;
                    }
                } catch (Exception e) {
                    Log.d("LocalAlgoModel", String.format("match exception title=%s rule=%s text=%s rule=%s", new Object[]{title, rule.title, text, rule.desc}));
                }
            }
        }
        return Integer.MIN_VALUE;
    }

    public static double getScore(ExpandedNotification sbn) {
        String packageName = sbn.getFoldPackageName();
        if (!isLocalModelAvailable()) {
            return 0.0d;
        }
        int matchScore = matchRules(packageName, sbn.getNotification());
        if (matchScore != Integer.MIN_VALUE) {
            return (double) matchScore;
        }
        PackageScoreCache packageScoreCache = (PackageScoreCache) Dependency.get(PackageScoreCache.class);
        int pkgTotalClick = packageScoreCache.getTotalClickCount(packageName);
        int pkgTotalShow = packageScoreCache.getTotalShowCount(packageName);
        double pkgClickRate = pkgTotalShow != 0 ? (((double) pkgTotalClick) * 1.0d) / ((double) pkgTotalShow) : 0.0d;
        int totalClickCount = packageScoreCache.getTotalClickCount();
        int totalShowCount = packageScoreCache.getTotalShowCount();
        double averageClickRate = totalShowCount != 0 ? (((double) totalClickCount) * 1.0d) / ((double) totalShowCount) : 0.0d;
        double sum = 0.0d;
        if (el != null) {
            double sum2 = 0.0d;
            for (int i = 0; i < el.length; i++) {
                sum2 += ((double) pkgTotalShow) < el[i] ? elw[i] : 0.0d;
            }
            sum = sum2;
        }
        if (eg != null) {
            int i2 = 0;
            while (i2 < eg.length) {
                int totalClickCount2 = totalClickCount;
                int totalShowCount2 = totalShowCount;
                sum += ((double) pkgTotalShow) > eg[i2] ? egw[i2] : 0.0d;
                i2++;
                totalClickCount = totalClickCount2;
                totalShowCount = totalShowCount2;
            }
        }
        int totalClickCount3 = totalClickCount;
        int i3 = totalShowCount;
        if (cl != null) {
            for (int i4 = 0; i4 < cl.length; i4++) {
                sum += ((double) pkgTotalClick) < cl[i4] ? clw[i4] : 0.0d;
            }
        }
        if (cg != null) {
            for (int i5 = 0; i5 < cg.length; i5++) {
                sum += ((double) pkgTotalClick) > cg[i5] ? cgw[i5] : 0.0d;
            }
        }
        if (sPkgs.contains(packageName)) {
            sum += 9.0d;
        }
        String str = packageName;
        int i6 = matchScore;
        double d = averageClickRate;
        return 1.0d / (Math.exp(-(((((((b + (w[0] * pkgClickRate)) + (w[1] * averageClickRate)) + (w[2] * ((double) pkgTotalShow))) + (w[3] * ((double) pkgTotalClick))) + (w[4] * ((((double) pkgTotalClick) * 0.1d) / ((0.1d * ((double) pkgTotalClick)) + 1.0d)))) + ((w[5] * ((double) (pkgTotalClick + totalClickCount3))) / ((double) (pkgTotalShow + 1)))) + sum)) + 1.0d);
    }

    public static double getScoreForRank(double score) {
        double d = 0.0d;
        if (th == 0.0d) {
            return score;
        }
        if (score > getThreshold()) {
            d = 1.0d;
        }
        return d;
    }

    public static double getThreshold() {
        return th * getIncreasePercent();
    }

    public static void uploadLocalAlgoModelIfNeed(Context context, Handler bgHandler) {
        if (needUploadLocalAlgoModel(context)) {
            Log.d("LocalAlgoModel", "upload local algo model");
            saveUploadDate(context);
            bgHandler.post(new Runnable() {
                public void run() {
                    ((SystemUIStat) Dependency.get(SystemUIStat.class)).uploadLocalAlgoModel();
                }
            });
        }
    }

    private static boolean needUploadLocalAlgoModel(Context context) {
        if (PreferenceManager.getDefaultSharedPreferences(context).getInt("last_upload_algo_date", 0) != DateUtils.getDigitalFormatDateToday()) {
            return true;
        }
        return false;
    }

    private static void saveUploadDate(Context context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putInt("last_upload_algo_date", DateUtils.getDigitalFormatDateToday()).apply();
    }
}
