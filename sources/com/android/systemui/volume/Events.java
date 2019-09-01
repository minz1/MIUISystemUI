package com.android.systemui.volume;

import com.android.systemui.plugins.VolumeDialogController;

public class Events {
    public static final String[] DISMISS_REASONS = {"unknown", "touch_outside", "volume_controller", "timeout", "screen_off", "settings_clicked", "done_clicked", "back_clicked", "config_changed"};
    private static final String[] EVENT_TAGS = {"show_dialog", "dismiss_dialog", "active_stream_changed", "expand", "key", "collection_started", "collection_stopped", "icon_click", "settings_click", "touch_level_changed", "level_changed", "internal_ringer_mode_changed", "external_ringer_mode_changed", "zen_mode_changed", "suppressor_changed", "mute_changed", "touch_level_done"};
    public static final String[] SHOW_REASONS = {"unknown", "volume_changed", "remote_volume_changed"};
    private static final String TAG = Util.logTag(Events.class);
    public static Callback sCallback;

    public interface Callback {
        void writeEvent(long j, int i, Object[] objArr);

        void writeState(long j, VolumeDialogController.State state);
    }

    /* JADX WARNING: Code restructure failed: missing block: B:11:0x006a, code lost:
        r2.append(ringerModeToString(r10[0].intValue()));
     */
    /* JADX WARNING: Code restructure failed: missing block: B:12:0x007b, code lost:
        r2.append(android.media.AudioSystem.streamToString(r10[0].intValue()));
        r2.append(' ');
        r2.append(r10[1]);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static void writeEvent(android.content.Context r8, int r9, java.lang.Object... r10) {
        /*
            long r0 = java.lang.System.currentTimeMillis()
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            java.lang.String r3 = "writeEvent "
            r2.<init>(r3)
            java.lang.String[] r3 = EVENT_TAGS
            r3 = r3[r9]
            java.lang.StringBuilder r2 = r2.append(r3)
            if (r10 == 0) goto L_0x0156
            int r3 = r10.length
            if (r3 <= 0) goto L_0x0156
            java.lang.String r3 = " "
            r2.append(r3)
            r3 = 207(0xcf, float:2.9E-43)
            r4 = 32
            r5 = 1
            r6 = 0
            switch(r9) {
                case 0: goto L_0x012c;
                case 1: goto L_0x0119;
                case 2: goto L_0x00fc;
                case 3: goto L_0x00e9;
                case 4: goto L_0x00c4;
                case 5: goto L_0x0026;
                case 6: goto L_0x0026;
                case 7: goto L_0x0094;
                case 8: goto L_0x0026;
                case 9: goto L_0x007b;
                case 10: goto L_0x007b;
                case 11: goto L_0x006a;
                case 12: goto L_0x005d;
                case 13: goto L_0x004c;
                case 14: goto L_0x003d;
                case 15: goto L_0x007b;
                case 16: goto L_0x002f;
                default: goto L_0x0026;
            }
        L_0x0026:
            java.util.List r3 = java.util.Arrays.asList(r10)
            r2.append(r3)
            goto L_0x0156
        L_0x002f:
            r3 = 209(0xd1, float:2.93E-43)
            r7 = r10[r5]
            java.lang.Integer r7 = (java.lang.Integer) r7
            int r7 = r7.intValue()
            com.android.internal.logging.MetricsLogger.action(r8, r3, r7)
            goto L_0x007b
        L_0x003d:
            r3 = r10[r6]
            r2.append(r3)
            r2.append(r4)
            r3 = r10[r5]
            r2.append(r3)
            goto L_0x0156
        L_0x004c:
            r3 = r10[r6]
            java.lang.Integer r3 = (java.lang.Integer) r3
            int r3 = r3.intValue()
            java.lang.String r3 = zenModeToString(r3)
            r2.append(r3)
            goto L_0x0156
        L_0x005d:
            r3 = 213(0xd5, float:2.98E-43)
            r4 = r10[r6]
            java.lang.Integer r4 = (java.lang.Integer) r4
            int r4 = r4.intValue()
            com.android.internal.logging.MetricsLogger.action(r8, r3, r4)
        L_0x006a:
            r3 = r10[r6]
            java.lang.Integer r3 = (java.lang.Integer) r3
            int r3 = r3.intValue()
            java.lang.String r3 = ringerModeToString(r3)
            r2.append(r3)
            goto L_0x0156
        L_0x007b:
            r3 = r10[r6]
            java.lang.Integer r3 = (java.lang.Integer) r3
            int r3 = r3.intValue()
            java.lang.String r3 = android.media.AudioSystem.streamToString(r3)
            r2.append(r3)
            r2.append(r4)
            r3 = r10[r5]
            r2.append(r3)
            goto L_0x0156
        L_0x0094:
            r3 = 212(0xd4, float:2.97E-43)
            r7 = r10[r5]
            java.lang.Integer r7 = (java.lang.Integer) r7
            int r7 = r7.intValue()
            com.android.internal.logging.MetricsLogger.action(r8, r3, r7)
            r3 = r10[r6]
            java.lang.Integer r3 = (java.lang.Integer) r3
            int r3 = r3.intValue()
            java.lang.String r3 = android.media.AudioSystem.streamToString(r3)
            r2.append(r3)
            r2.append(r4)
            r3 = r10[r5]
            java.lang.Integer r3 = (java.lang.Integer) r3
            int r3 = r3.intValue()
            java.lang.String r3 = iconStateToString(r3)
            r2.append(r3)
            goto L_0x0156
        L_0x00c4:
            r3 = 211(0xd3, float:2.96E-43)
            r7 = r10[r5]
            java.lang.Integer r7 = (java.lang.Integer) r7
            int r7 = r7.intValue()
            com.android.internal.logging.MetricsLogger.action(r8, r3, r7)
            r3 = r10[r6]
            java.lang.Integer r3 = (java.lang.Integer) r3
            int r3 = r3.intValue()
            java.lang.String r3 = android.media.AudioSystem.streamToString(r3)
            r2.append(r3)
            r2.append(r4)
            r3 = r10[r5]
            r2.append(r3)
            goto L_0x0156
        L_0x00e9:
            r3 = 208(0xd0, float:2.91E-43)
            r4 = r10[r6]
            java.lang.Boolean r4 = (java.lang.Boolean) r4
            boolean r4 = r4.booleanValue()
            com.android.internal.logging.MetricsLogger.visibility(r8, r3, r4)
            r3 = r10[r6]
            r2.append(r3)
            goto L_0x0156
        L_0x00fc:
            r3 = 210(0xd2, float:2.94E-43)
            r4 = r10[r6]
            java.lang.Integer r4 = (java.lang.Integer) r4
            int r4 = r4.intValue()
            com.android.internal.logging.MetricsLogger.action(r8, r3, r4)
            r3 = r10[r6]
            java.lang.Integer r3 = (java.lang.Integer) r3
            int r3 = r3.intValue()
            java.lang.String r3 = android.media.AudioSystem.streamToString(r3)
            r2.append(r3)
            goto L_0x0156
        L_0x0119:
            com.android.internal.logging.MetricsLogger.hidden(r8, r3)
            java.lang.String[] r3 = DISMISS_REASONS
            r4 = r10[r6]
            java.lang.Integer r4 = (java.lang.Integer) r4
            int r4 = r4.intValue()
            r3 = r3[r4]
            r2.append(r3)
            goto L_0x0156
        L_0x012c:
            com.android.internal.logging.MetricsLogger.visible(r8, r3)
            java.lang.String r3 = "volume_from_keyguard"
            r4 = r10[r5]
            java.lang.Boolean r4 = (java.lang.Boolean) r4
            boolean r4 = r4.booleanValue()
            com.android.internal.logging.MetricsLogger.histogram(r8, r3, r4)
            java.lang.String[] r3 = SHOW_REASONS
            r4 = r10[r6]
            java.lang.Integer r4 = (java.lang.Integer) r4
            int r4 = r4.intValue()
            r3 = r3[r4]
            r2.append(r3)
            java.lang.String r3 = " keyguard="
            r2.append(r3)
            r3 = r10[r5]
            r2.append(r3)
        L_0x0156:
            java.lang.String r3 = TAG
            java.lang.String r4 = r2.toString()
            android.util.Log.i(r3, r4)
            com.android.systemui.volume.Events$Callback r3 = sCallback
            if (r3 == 0) goto L_0x0168
            com.android.systemui.volume.Events$Callback r3 = sCallback
            r3.writeEvent(r0, r9, r10)
        L_0x0168:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.volume.Events.writeEvent(android.content.Context, int, java.lang.Object[]):void");
    }

    public static void writeState(long time, VolumeDialogController.State state) {
        if (sCallback != null) {
            sCallback.writeState(time, state);
        }
    }

    private static String iconStateToString(int iconState) {
        switch (iconState) {
            case 1:
                return "unmute";
            case 2:
                return "mute";
            case 3:
                return "vibrate";
            default:
                return "unknown_state_" + iconState;
        }
    }

    private static String ringerModeToString(int ringerMode) {
        switch (ringerMode) {
            case 0:
                return "silent";
            case 1:
                return "vibrate";
            case 2:
                return "normal";
            default:
                return "unknown";
        }
    }

    private static String zenModeToString(int zenMode) {
        switch (zenMode) {
            case 0:
                return "off";
            case 1:
                return "important_interruptions";
            case 2:
                return "no_interruptions";
            case 3:
                return "alarms";
            default:
                return "unknown";
        }
    }
}
