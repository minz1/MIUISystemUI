package com.android.systemui.recents.misc;

import android.os.Handler;
import com.android.internal.os.BackgroundThread;
import com.android.systemui.AnalyticsHelper;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.model.TaskStack;
import java.util.ArrayList;
import java.util.HashMap;
import org.json.JSONException;
import org.json.JSONObject;

public class RecentsPushEventHelper {
    /* access modifiers changed from: private */
    public static String mLastBottomStackPkg;
    /* access modifiers changed from: private */
    public static String mLastTopStackPkg;
    /* access modifiers changed from: private */
    public static String sMultiWindowPushEventId;
    /* access modifiers changed from: private */
    public static String sRecentsPushEventId;

    public static void sendRecentsEvent(final String name, final String data) {
        BackgroundThread.getHandler().post(new Runnable() {
            public void run() {
                if ("enterRecents".equals(name)) {
                    String unused = RecentsPushEventHelper.sRecentsPushEventId = System.currentTimeMillis() + "";
                }
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("id", RecentsPushEventHelper.sRecentsPushEventId);
                    jsonObject.put("content", data);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                RecentsPushEventHelper.sendEvent("recents", name, jsonObject.toString());
            }
        });
    }

    public static void sendMultiWindowEvent(final String name, final String data) {
        BackgroundThread.getHandler().post(new Runnable() {
            public void run() {
                if ("enterMultiWindow".equals(name)) {
                    String unused = RecentsPushEventHelper.sMultiWindowPushEventId = System.currentTimeMillis() + "";
                }
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("id", RecentsPushEventHelper.sMultiWindowPushEventId);
                    jsonObject.put("content", data);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                RecentsPushEventHelper.sendEvent("multi_window", name, jsonObject.toString());
            }
        });
    }

    public static void sendEvent(String category, String name, String data) {
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("data", data);
        AnalyticsHelper.track(category, name, parameters);
    }

    public static void sendTaskStackChangedEvent() {
        BackgroundThread.getHandler().post(new Runnable() {
            /* JADX WARNING: Removed duplicated region for block: B:35:0x007c  */
            /* JADX WARNING: Removed duplicated region for block: B:38:0x008e  */
            /* JADX WARNING: Removed duplicated region for block: B:46:? A[RETURN, SYNTHETIC] */
            /* Code decompiled incorrectly, please refer to instructions dump. */
            public void run() {
                /*
                    r11 = this;
                    com.android.systemui.recents.misc.SystemServicesProxy r0 = com.android.systemui.recents.Recents.getSystemServices()
                    boolean r0 = r0.hasDockedTask()
                    if (r0 == 0) goto L_0x00b2
                    r0 = 0
                    r1 = 0
                    r2 = -1
                    r3 = 3
                    r4 = 0
                    android.app.ActivityManager$StackInfo r3 = android.app.ActivityManagerCompat.getStackInfo(r3, r3, r4)     // Catch:{ Exception -> 0x006a }
                    r5 = 0
                    if (r3 == 0) goto L_0x0019
                    android.content.ComponentName r6 = r3.topActivity     // Catch:{ Exception -> 0x006a }
                    goto L_0x001a
                L_0x0019:
                    r6 = r5
                L_0x001a:
                    if (r6 == 0) goto L_0x0025
                    boolean r7 = r3.visible     // Catch:{ Exception -> 0x006a }
                    if (r7 == 0) goto L_0x0025
                    java.lang.String r7 = r6.getPackageName()     // Catch:{ Exception -> 0x006a }
                    r0 = r7
                L_0x0025:
                    r7 = 1
                    android.app.ActivityManager$StackInfo r7 = android.app.ActivityManagerCompat.getStackInfo(r7, r7, r4)     // Catch:{ Exception -> 0x006a }
                    if (r7 == 0) goto L_0x002f
                    android.content.ComponentName r8 = r7.topActivity     // Catch:{ Exception -> 0x006a }
                    goto L_0x0030
                L_0x002f:
                    r8 = r5
                L_0x0030:
                    if (r8 == 0) goto L_0x0049
                    boolean r9 = r7.visible     // Catch:{ Exception -> 0x006a }
                    if (r9 == 0) goto L_0x0049
                    java.lang.String r4 = r8.getPackageName()     // Catch:{ Exception -> 0x006a }
                    r1 = r4
                    com.android.systemui.recents.misc.SystemServicesProxy r4 = com.android.systemui.recents.Recents.getSystemServices()     // Catch:{ Exception -> 0x006a }
                    android.content.pm.ActivityInfo r4 = r4.getActivityInfo(r8)     // Catch:{ Exception -> 0x006a }
                    if (r4 == 0) goto L_0x0048
                    int r5 = r4.resizeMode     // Catch:{ Exception -> 0x006a }
                    r2 = r5
                L_0x0048:
                    goto L_0x0069
                L_0x0049:
                    r9 = 2
                    android.app.ActivityManager$StackInfo r4 = android.app.ActivityManagerCompat.getStackInfo(r4, r4, r9)     // Catch:{ Exception -> 0x006a }
                    if (r4 == 0) goto L_0x0053
                    android.content.ComponentName r5 = r4.topActivity     // Catch:{ Exception -> 0x006a }
                L_0x0053:
                    if (r5 == 0) goto L_0x0069
                    boolean r9 = r4.visible     // Catch:{ Exception -> 0x006a }
                    if (r9 == 0) goto L_0x0069
                    java.lang.String r9 = r5.getPackageName()     // Catch:{ Exception -> 0x006a }
                    r1 = r9
                    com.android.systemui.recents.misc.SystemServicesProxy r9 = com.android.systemui.recents.Recents.getSystemServices()     // Catch:{ Exception -> 0x006a }
                    android.content.pm.ActivityInfo r9 = r9.getActivityInfo(r5)     // Catch:{ Exception -> 0x006a }
                    int r10 = r9.resizeMode     // Catch:{ Exception -> 0x006a }
                    r2 = r10
                L_0x0069:
                    goto L_0x0072
                L_0x006a:
                    r3 = move-exception
                    java.lang.String r4 = "RecentsPushEventHelper"
                    java.lang.String r5 = "sendTaskStackChangedEvent error"
                    android.util.Log.e(r4, r5, r3)
                L_0x0072:
                    java.lang.String r3 = com.android.systemui.recents.misc.RecentsPushEventHelper.mLastTopStackPkg
                    boolean r3 = android.text.TextUtils.equals(r3, r0)
                    if (r3 != 0) goto L_0x0084
                    java.lang.String unused = com.android.systemui.recents.misc.RecentsPushEventHelper.mLastTopStackPkg = r0
                    java.lang.String r3 = "topTaskChanged"
                    com.android.systemui.recents.misc.RecentsPushEventHelper.sendMultiWindowEvent(r3, r0)
                L_0x0084:
                    java.lang.String r3 = com.android.systemui.recents.misc.RecentsPushEventHelper.mLastBottomStackPkg
                    boolean r3 = android.text.TextUtils.equals(r3, r1)
                    if (r3 != 0) goto L_0x00b2
                    java.lang.String unused = com.android.systemui.recents.misc.RecentsPushEventHelper.mLastBottomStackPkg = r1
                    org.json.JSONObject r3 = new org.json.JSONObject
                    r3.<init>()
                    java.lang.String r4 = "pkg"
                    r3.put(r4, r1)     // Catch:{ JSONException -> 0x00a5 }
                    java.lang.String r4 = "resizeMode"
                    java.lang.String r5 = android.content.pm.ActivityInfo.resizeModeToString(r2)     // Catch:{ JSONException -> 0x00a5 }
                    r3.put(r4, r5)     // Catch:{ JSONException -> 0x00a5 }
                    goto L_0x00a9
                L_0x00a5:
                    r4 = move-exception
                    r4.printStackTrace()
                L_0x00a9:
                    java.lang.String r4 = "bottomTaskChanged"
                    java.lang.String r5 = r3.toString()
                    com.android.systemui.recents.misc.RecentsPushEventHelper.sendMultiWindowEvent(r4, r5)
                L_0x00b2:
                    return
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.recents.misc.RecentsPushEventHelper.AnonymousClass3.run():void");
            }
        });
    }

    public static void sendEnterRecentsEvent(final TaskStack stack) {
        BackgroundThread.getHandler().post(new Runnable() {
            public void run() {
                ArrayList<String> taskLockedList = new ArrayList<>();
                ArrayList<Task> stackTasks = TaskStack.this.getStackTasks();
                for (int i = 0; i < stackTasks.size(); i++) {
                    Task task = stackTasks.get(i);
                    if (task.isLocked) {
                        taskLockedList.add(task.key.getComponent().getPackageName());
                    }
                }
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("taskCount", TaskStack.this.getTaskCount());
                    jsonObject.put("taskLockedCount", taskLockedList.size());
                    jsonObject.put("taskLockedList", taskLockedList);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                RecentsPushEventHelper.sendRecentsEvent("enterRecents", jsonObject.toString());
            }
        });
    }

    public static void sendSwitchAppEvent(String switchType, String taskPosition) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("switchType", switchType);
            jsonObject.put("taskPosition", taskPosition);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        sendRecentsEvent("switchApp", jsonObject.toString());
    }

    public static void sendOneKeyCleanEvent(long freeAtFirst, long freeAtLast, long total) {
        Handler handler = BackgroundThread.getHandler();
        final long j = freeAtFirst;
        final long j2 = freeAtLast;
        final long j3 = total;
        AnonymousClass5 r1 = new Runnable() {
            public void run() {
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("freeMemoryBeforeClean", j / 1024);
                    jsonObject.put("freeMemoryAfterClean", j2 / 1024);
                    jsonObject.put("totalMemory", j3 / 1024);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                RecentsPushEventHelper.sendRecentsEvent("oneKeyCleanStart", jsonObject.toString());
            }
        };
        handler.post(r1);
    }

    public static void sendShowRecommendCardEvent(boolean isShow) {
        sendRecentsEvent("cardShow", isShow ? "show" : "hide");
    }

    public static void sendClickRecommendCardEvent(String clickType) {
        sendRecentsEvent("cardClick", clickType);
    }
}
