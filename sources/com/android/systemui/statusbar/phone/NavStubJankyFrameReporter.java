package com.android.systemui.statusbar.phone;

import android.os.Build;
import com.android.internal.os.BackgroundThread;
import java.util.ArrayList;
import java.util.List;
import miui.mqsas.sdk.MQSEventManagerDelegate;
import org.json.JSONException;
import org.json.JSONObject;

public class NavStubJankyFrameReporter {
    private static long sCurrentTime = -1;
    private static int sJankyFrameCount = 0;
    private static final List<String> sJankyFramesInfoList = new ArrayList();

    static void resetAnimationFrameIntervalParams(String action) {
        if ("whyred".equals(Build.DEVICE)) {
            sCurrentTime = -1;
            sJankyFrameCount = 0;
        }
    }

    static void caculateAnimationFrameInterval(String action) {
        if ("whyred".equals(Build.DEVICE)) {
            long previousFrameTime = sCurrentTime;
            sCurrentTime = System.currentTimeMillis();
            if (previousFrameTime > 0 && sCurrentTime - previousFrameTime > 100) {
                sJankyFrameCount++;
            }
        }
    }

    static void recordJankyFrames(String action) {
        if ("whyred".equals(Build.DEVICE)) {
            try {
                sJankyFramesInfoList.add(frameInfoToJson(action).toString());
            } catch (JSONException e) {
                e.printStackTrace();
                sJankyFramesInfoList.add(String.format("{\"fullScreenVersion\":\"\",\"action\":\"\",\"jankyFramesCount\":\"\",\"extraKey1\":\"\",\"extraKey2\":\" %s\"}", new Object[]{e.toString()}));
            }
            if (sJankyFramesInfoList.size() == 10) {
                final List<String> uploadList = new ArrayList<>(sJankyFramesInfoList);
                sJankyFramesInfoList.clear();
                BackgroundThread.getHandler().post(new Runnable() {
                    public void run() {
                        MQSEventManagerDelegate.getInstance().reportEvents("fsJankyFrames", uploadList, true);
                    }
                });
            }
        }
    }

    private static JSONObject frameInfoToJson(String action) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("fullScreenVersion", "1");
        jsonObject.put("action", action);
        jsonObject.put("jankyFramesCount", String.valueOf(sJankyFrameCount));
        jsonObject.put("extraKey1", String.valueOf(System.currentTimeMillis()));
        jsonObject.put("extraKey2", "");
        return jsonObject;
    }
}
