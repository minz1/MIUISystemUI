package com.android.systemui.statusbar;

import android.content.ComponentName;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.Pair;
import com.android.internal.os.SomeArgs;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.systemui.SystemUI;
import java.util.ArrayList;

public class CommandQueue extends CompatibilityCommandQueue {
    /* access modifiers changed from: private */
    public ArrayList<Callbacks> mCallbacks = new ArrayList<>();
    private int mDisable1;
    private int mDisable2;
    /* access modifiers changed from: private */
    public Handler mHandler = new H(Looper.getMainLooper());
    private final Object mLock = new Object();

    public interface Callbacks {
        void addQsTile(ComponentName componentName);

        void animateCollapsePanels(int i);

        void animateExpandNotificationsPanel();

        void animateExpandSettingsPanel(String str);

        void appTransitionCancelled();

        void appTransitionFinished();

        void appTransitionPending(boolean z);

        void appTransitionStarting(long j, long j2, boolean z);

        void cancelPreloadRecentApps();

        void clickTile(ComponentName componentName);

        void disable(int i, int i2, boolean z);

        void dismissKeyboardShortcutsMenu();

        void handleShowGlobalActionsMenu();

        void handleSystemNavigationKey(int i);

        void hideFingerprintDialog();

        void hideRecentApps(boolean z, boolean z2);

        void onFingerprintAuthenticated();

        void onFingerprintError(String str);

        void onFingerprintHelp(String str);

        void preloadRecentApps();

        void remQsTile(ComponentName componentName);

        void removeIcon(String str);

        void setIcon(String str, StatusBarIcon statusBarIcon);

        void setImeWindowStatus(IBinder iBinder, int i, int i2, boolean z);

        void setStatus(int i, String str, Bundle bundle);

        void setSystemUiVisibility(int i, int i2, int i3, int i4, Rect rect, Rect rect2);

        void setWindowState(int i, int i2);

        void showAssistDisclosure();

        void showFingerprintDialog(SomeArgs someArgs);

        void showPictureInPictureMenu();

        void showRecentApps(boolean z, boolean z2);

        void showScreenPinningRequest(int i);

        void startAssist(Bundle bundle);

        void toggleKeyboardShortcutsMenu(int i);

        void toggleRecentApps();

        void toggleSplitScreen();

        void topAppWindowChanged(boolean z);
    }

    public static class CommandQueueStart extends SystemUI {
        public void start() {
            putComponent(CommandQueue.class, new CommandQueue());
        }
    }

    private final class H extends Handler {
        private H(Looper l) {
            super(l);
        }

        /* JADX WARNING: Code restructure failed: missing block: B:107:0x03f5, code lost:
            r1 = r2;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:108:0x0400, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r12.this$0).size()) goto L_0x0606;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:109:0x0402, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r12.this$0).get(r1)).setWindowState(r13.arg1, r13.arg2);
            r2 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:10:0x007b, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r12.this$0).size()) goto L_0x0606;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:110:0x041b, code lost:
            r1 = r2;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:111:0x0426, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r12.this$0).size()) goto L_0x0606;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:112:0x0428, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r12.this$0).get(r1)).cancelPreloadRecentApps();
            r2 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:113:0x043d, code lost:
            r1 = r2;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:114:0x0448, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r12.this$0).size()) goto L_0x0606;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:115:0x044a, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r12.this$0).get(r1)).preloadRecentApps();
            r2 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:116:0x045f, code lost:
            r1 = r2;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:117:0x046a, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r12.this$0).size()) goto L_0x0606;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:118:0x046c, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r12.this$0).get(r1)).toggleRecentApps();
            r2 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:11:0x007d, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r12.this$0).get(r1)).hideFingerprintDialog();
            r2 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:12:0x0092, code lost:
            r1 = r2;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:136:0x0517, code lost:
            r1 = r2;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:137:0x0522, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r12.this$0).size()) goto L_0x0606;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:138:0x0524, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r12.this$0).get(r1)).animateExpandSettingsPanel((java.lang.String) r13.obj);
            r2 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:139:0x053d, code lost:
            r1 = r2;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:13:0x009d, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r12.this$0).size()) goto L_0x0606;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:140:0x0548, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r12.this$0).size()) goto L_0x0606;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:141:0x054a, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r12.this$0).get(r1)).animateCollapsePanels(r13.arg1);
            r2 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:142:0x0561, code lost:
            r1 = r2;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:143:0x056c, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r12.this$0).size()) goto L_0x0606;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:144:0x056e, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r12.this$0).get(r1)).animateExpandNotificationsPanel();
            r2 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:145:0x0583, code lost:
            r1 = r2;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:146:0x058e, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r12.this$0).size()) goto L_0x0606;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:147:0x0590, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r12.this$0).get(r1)).disable(r13.arg1, r13.arg2, ((java.lang.Boolean) r13.obj).booleanValue());
            r2 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:14:0x009f, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r12.this$0).get(r1)).onFingerprintError((java.lang.String) r13.obj);
            r2 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:15:0x00b8, code lost:
            r1 = r2;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:16:0x00c3, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r12.this$0).size()) goto L_0x0606;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:17:0x00c5, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r12.this$0).get(r1)).onFingerprintHelp((java.lang.String) r13.obj);
            r2 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:18:0x00de, code lost:
            r1 = r2;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:19:0x00e9, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r12.this$0).size()) goto L_0x0606;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:209:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:20:0x00eb, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r12.this$0).get(r1)).onFingerprintAuthenticated();
            r2 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:210:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:211:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:212:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:213:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:215:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:216:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:217:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:218:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:219:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:220:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:221:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:222:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:223:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:224:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:225:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:226:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:228:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:230:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:233:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:234:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:235:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:236:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:239:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:240:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:241:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:242:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:25:0x014f, code lost:
            r1 = r2;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:26:0x015a, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r12.this$0).size()) goto L_0x0606;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:27:0x015c, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r12.this$0).get(r1)).handleShowGlobalActionsMenu();
            r2 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:28:0x0173, code lost:
            r1 = r2;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:29:0x017e, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r12.this$0).size()) goto L_0x0606;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:30:0x0180, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r12.this$0).get(r1)).dismissKeyboardShortcutsMenu();
            r2 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:31:0x0195, code lost:
            r1 = r2;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:32:0x01a0, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r12.this$0).size()) goto L_0x0606;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:33:0x01a2, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r12.this$0).get(r1)).appTransitionFinished();
            r2 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:34:0x01b7, code lost:
            r1 = r2;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:35:0x01c2, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r12.this$0).size()) goto L_0x0606;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:36:0x01c4, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r12.this$0).get(r1)).toggleSplitScreen();
            r2 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:37:0x01d9, code lost:
            r1 = r2;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:38:0x01e4, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r12.this$0).size()) goto L_0x0606;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:39:0x01e6, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r12.this$0).get(r1)).clickTile((android.content.ComponentName) r13.obj);
            r2 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:40:0x01ff, code lost:
            r1 = r2;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:41:0x020a, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r12.this$0).size()) goto L_0x0606;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:42:0x020c, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r12.this$0).get(r1)).remQsTile((android.content.ComponentName) r13.obj);
            r2 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:43:0x0225, code lost:
            r1 = r2;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:44:0x0230, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r12.this$0).size()) goto L_0x0606;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:45:0x0232, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r12.this$0).get(r1)).addQsTile((android.content.ComponentName) r13.obj);
            r2 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:46:0x024b, code lost:
            r1 = r2;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:47:0x0256, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r12.this$0).size()) goto L_0x0606;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:48:0x0258, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r12.this$0).get(r1)).showPictureInPictureMenu();
            r2 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:49:0x026d, code lost:
            r1 = r2;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:50:0x0278, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r12.this$0).size()) goto L_0x0606;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:51:0x027a, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r12.this$0).get(r1)).toggleKeyboardShortcutsMenu(r13.arg1);
            r2 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:52:0x0291, code lost:
            r1 = r2;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:53:0x029c, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r12.this$0).size()) goto L_0x0606;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:54:0x029e, code lost:
            r2 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:55:0x02a4, code lost:
            r1 = r2;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:56:0x02af, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r12.this$0).size()) goto L_0x0606;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:57:0x02b1, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r12.this$0).get(r1)).startAssist((android.os.Bundle) r13.obj);
            r2 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:58:0x02ca, code lost:
            r1 = r2;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:59:0x02d5, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r12.this$0).size()) goto L_0x0606;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:60:0x02d7, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r12.this$0).get(r1)).showAssistDisclosure();
            r2 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:69:0x0329, code lost:
            r1 = r2;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:6:0x0046, code lost:
            r1 = r2;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:70:0x0334, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r12.this$0).size()) goto L_0x0606;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:71:0x0336, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r12.this$0).get(r1)).appTransitionCancelled();
            r2 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:7:0x0051, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r12.this$0).size()) goto L_0x0606;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:80:0x0373, code lost:
            r1 = r2;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:81:0x037e, code lost:
            if (r1 >= com.android.systemui.statusbar.CommandQueue.access$100(r12.this$0).size()) goto L_0x0606;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:82:0x0380, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r12.this$0).get(r1)).showScreenPinningRequest(r13.arg1);
            r2 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:8:0x0053, code lost:
            ((com.android.systemui.statusbar.CommandQueue.Callbacks) com.android.systemui.statusbar.CommandQueue.access$100(r12.this$0).get(r1)).handleSystemNavigationKey(r13.arg1);
            r2 = r1 + 1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:9:0x0070, code lost:
            r1 = r2;
         */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void handleMessage(android.os.Message r13) {
            /*
                r12 = this;
                int r0 = r13.what
                r1 = -65536(0xffffffffffff0000, float:NaN)
                r0 = r0 & r1
                r1 = 1
                r2 = 0
                switch(r0) {
                    case 65536: goto L_0x05af;
                    case 131072: goto L_0x0582;
                    case 196608: goto L_0x0560;
                    case 262144: goto L_0x053c;
                    case 327680: goto L_0x0516;
                    case 393216: goto L_0x04db;
                    case 458752: goto L_0x04b3;
                    case 524288: goto L_0x0480;
                    case 589824: goto L_0x045e;
                    case 655360: goto L_0x043c;
                    case 720896: goto L_0x041a;
                    case 786432: goto L_0x03f4;
                    case 851968: goto L_0x03c5;
                    case 917504: goto L_0x0396;
                    case 1179648: goto L_0x0372;
                    case 1245184: goto L_0x034a;
                    case 1310720: goto L_0x0328;
                    case 1376256: goto L_0x02eb;
                    case 1441792: goto L_0x02c9;
                    case 1507328: goto L_0x02a3;
                    case 1572864: goto L_0x0290;
                    case 1638400: goto L_0x026c;
                    case 1703936: goto L_0x024a;
                    case 1769472: goto L_0x0224;
                    case 1835008: goto L_0x01fe;
                    case 1900544: goto L_0x01d8;
                    case 1966080: goto L_0x01b6;
                    case 2031616: goto L_0x0194;
                    case 2097152: goto L_0x0172;
                    case 2162688: goto L_0x0170;
                    case 2228224: goto L_0x014e;
                    case 2293760: goto L_0x014c;
                    case 2359296: goto L_0x014a;
                    case 2424832: goto L_0x0148;
                    case 2490368: goto L_0x0146;
                    case 2555904: goto L_0x00ff;
                    case 2621440: goto L_0x00dd;
                    case 2686976: goto L_0x00b7;
                    case 2752512: goto L_0x0091;
                    case 2818048: goto L_0x006f;
                    case 2883584: goto L_0x006d;
                    case 2949120: goto L_0x006b;
                    case 3014656: goto L_0x0069;
                    case 6488064: goto L_0x0045;
                    case 6553600: goto L_0x000c;
                    default: goto L_0x000a;
                }
            L_0x000a:
                goto L_0x0606
            L_0x000c:
                java.lang.Object r1 = r13.obj
                android.os.Bundle r1 = (android.os.Bundle) r1
                java.lang.String r3 = "what"
                int r3 = r1.getInt(r3)
                java.lang.String r4 = "action"
                java.lang.String r4 = r1.getString(r4)
                java.lang.String r5 = "ext"
                android.os.Parcelable r5 = r1.getParcelable(r5)
                android.os.Bundle r5 = (android.os.Bundle) r5
            L_0x0025:
                com.android.systemui.statusbar.CommandQueue r6 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r6 = r6.mCallbacks
                int r6 = r6.size()
                if (r2 >= r6) goto L_0x0043
                com.android.systemui.statusbar.CommandQueue r6 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r6 = r6.mCallbacks
                java.lang.Object r6 = r6.get(r2)
                com.android.systemui.statusbar.CommandQueue$Callbacks r6 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r6
                r6.setStatus(r3, r4, r5)
                int r2 = r2 + 1
                goto L_0x0025
            L_0x0043:
                goto L_0x0606
            L_0x0045:
            L_0x0046:
                r1 = r2
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                int r2 = r2.size()
                if (r1 >= r2) goto L_0x0067
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                java.lang.Object r2 = r2.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r2 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r2
                int r3 = r13.arg1
                r2.handleSystemNavigationKey(r3)
                int r2 = r1 + 1
                goto L_0x0046
            L_0x0067:
                goto L_0x0606
            L_0x0069:
                goto L_0x0606
            L_0x006b:
                goto L_0x0606
            L_0x006d:
                goto L_0x0606
            L_0x006f:
            L_0x0070:
                r1 = r2
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                int r2 = r2.size()
                if (r1 >= r2) goto L_0x008f
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                java.lang.Object r2 = r2.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r2 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r2
                r2.hideFingerprintDialog()
                int r2 = r1 + 1
                goto L_0x0070
            L_0x008f:
                goto L_0x0606
            L_0x0091:
            L_0x0092:
                r1 = r2
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                int r2 = r2.size()
                if (r1 >= r2) goto L_0x00b5
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                java.lang.Object r2 = r2.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r2 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r2
                java.lang.Object r3 = r13.obj
                java.lang.String r3 = (java.lang.String) r3
                r2.onFingerprintError(r3)
                int r2 = r1 + 1
                goto L_0x0092
            L_0x00b5:
                goto L_0x0606
            L_0x00b7:
            L_0x00b8:
                r1 = r2
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                int r2 = r2.size()
                if (r1 >= r2) goto L_0x00db
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                java.lang.Object r2 = r2.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r2 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r2
                java.lang.Object r3 = r13.obj
                java.lang.String r3 = (java.lang.String) r3
                r2.onFingerprintHelp(r3)
                int r2 = r1 + 1
                goto L_0x00b8
            L_0x00db:
                goto L_0x0606
            L_0x00dd:
            L_0x00de:
                r1 = r2
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                int r2 = r2.size()
                if (r1 >= r2) goto L_0x00fd
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                java.lang.Object r2 = r2.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r2 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r2
                r2.onFingerprintAuthenticated()
                int r2 = r1 + 1
                goto L_0x00de
            L_0x00fd:
                goto L_0x0606
            L_0x00ff:
                com.android.systemui.statusbar.CommandQueue r1 = com.android.systemui.statusbar.CommandQueue.this
                android.os.Handler r1 = r1.mHandler
                r3 = 2752512(0x2a0000, float:3.857091E-39)
                r1.removeMessages(r3)
                com.android.systemui.statusbar.CommandQueue r1 = com.android.systemui.statusbar.CommandQueue.this
                android.os.Handler r1 = r1.mHandler
                r3 = 2686976(0x290000, float:3.765255E-39)
                r1.removeMessages(r3)
                com.android.systemui.statusbar.CommandQueue r1 = com.android.systemui.statusbar.CommandQueue.this
                android.os.Handler r1 = r1.mHandler
                r3 = 2621440(0x280000, float:3.67342E-39)
                r1.removeMessages(r3)
            L_0x0121:
                r1 = r2
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                int r2 = r2.size()
                if (r1 >= r2) goto L_0x0144
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                java.lang.Object r2 = r2.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r2 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r2
                java.lang.Object r3 = r13.obj
                com.android.internal.os.SomeArgs r3 = (com.android.internal.os.SomeArgs) r3
                r2.showFingerprintDialog(r3)
                int r2 = r1 + 1
                goto L_0x0121
            L_0x0144:
                goto L_0x0606
            L_0x0146:
                goto L_0x0606
            L_0x0148:
                goto L_0x0606
            L_0x014a:
                goto L_0x0606
            L_0x014c:
                goto L_0x0606
            L_0x014e:
            L_0x014f:
                r1 = r2
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                int r2 = r2.size()
                if (r1 >= r2) goto L_0x016e
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                java.lang.Object r2 = r2.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r2 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r2
                r2.handleShowGlobalActionsMenu()
                int r2 = r1 + 1
                goto L_0x014f
            L_0x016e:
                goto L_0x0606
            L_0x0170:
                goto L_0x0606
            L_0x0172:
            L_0x0173:
                r1 = r2
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                int r2 = r2.size()
                if (r1 >= r2) goto L_0x0192
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                java.lang.Object r2 = r2.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r2 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r2
                r2.dismissKeyboardShortcutsMenu()
                int r2 = r1 + 1
                goto L_0x0173
            L_0x0192:
                goto L_0x0606
            L_0x0194:
            L_0x0195:
                r1 = r2
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                int r2 = r2.size()
                if (r1 >= r2) goto L_0x01b4
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                java.lang.Object r2 = r2.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r2 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r2
                r2.appTransitionFinished()
                int r2 = r1 + 1
                goto L_0x0195
            L_0x01b4:
                goto L_0x0606
            L_0x01b6:
            L_0x01b7:
                r1 = r2
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                int r2 = r2.size()
                if (r1 >= r2) goto L_0x01d6
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                java.lang.Object r2 = r2.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r2 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r2
                r2.toggleSplitScreen()
                int r2 = r1 + 1
                goto L_0x01b7
            L_0x01d6:
                goto L_0x0606
            L_0x01d8:
            L_0x01d9:
                r1 = r2
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                int r2 = r2.size()
                if (r1 >= r2) goto L_0x01fc
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                java.lang.Object r2 = r2.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r2 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r2
                java.lang.Object r3 = r13.obj
                android.content.ComponentName r3 = (android.content.ComponentName) r3
                r2.clickTile(r3)
                int r2 = r1 + 1
                goto L_0x01d9
            L_0x01fc:
                goto L_0x0606
            L_0x01fe:
            L_0x01ff:
                r1 = r2
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                int r2 = r2.size()
                if (r1 >= r2) goto L_0x0222
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                java.lang.Object r2 = r2.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r2 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r2
                java.lang.Object r3 = r13.obj
                android.content.ComponentName r3 = (android.content.ComponentName) r3
                r2.remQsTile(r3)
                int r2 = r1 + 1
                goto L_0x01ff
            L_0x0222:
                goto L_0x0606
            L_0x0224:
            L_0x0225:
                r1 = r2
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                int r2 = r2.size()
                if (r1 >= r2) goto L_0x0248
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                java.lang.Object r2 = r2.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r2 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r2
                java.lang.Object r3 = r13.obj
                android.content.ComponentName r3 = (android.content.ComponentName) r3
                r2.addQsTile(r3)
                int r2 = r1 + 1
                goto L_0x0225
            L_0x0248:
                goto L_0x0606
            L_0x024a:
            L_0x024b:
                r1 = r2
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                int r2 = r2.size()
                if (r1 >= r2) goto L_0x026a
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                java.lang.Object r2 = r2.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r2 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r2
                r2.showPictureInPictureMenu()
                int r2 = r1 + 1
                goto L_0x024b
            L_0x026a:
                goto L_0x0606
            L_0x026c:
            L_0x026d:
                r1 = r2
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                int r2 = r2.size()
                if (r1 >= r2) goto L_0x028e
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                java.lang.Object r2 = r2.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r2 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r2
                int r3 = r13.arg1
                r2.toggleKeyboardShortcutsMenu(r3)
                int r2 = r1 + 1
                goto L_0x026d
            L_0x028e:
                goto L_0x0606
            L_0x0290:
            L_0x0291:
                r1 = r2
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                int r2 = r2.size()
                if (r1 >= r2) goto L_0x02a1
                int r2 = r1 + 1
                goto L_0x0291
            L_0x02a1:
                goto L_0x0606
            L_0x02a3:
            L_0x02a4:
                r1 = r2
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                int r2 = r2.size()
                if (r1 >= r2) goto L_0x02c7
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                java.lang.Object r2 = r2.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r2 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r2
                java.lang.Object r3 = r13.obj
                android.os.Bundle r3 = (android.os.Bundle) r3
                r2.startAssist(r3)
                int r2 = r1 + 1
                goto L_0x02a4
            L_0x02c7:
                goto L_0x0606
            L_0x02c9:
            L_0x02ca:
                r1 = r2
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                int r2 = r2.size()
                if (r1 >= r2) goto L_0x02e9
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                java.lang.Object r2 = r2.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r2 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r2
                r2.showAssistDisclosure()
                int r2 = r1 + 1
                goto L_0x02ca
            L_0x02e9:
                goto L_0x0606
            L_0x02eb:
                r3 = r2
            L_0x02ec:
                com.android.systemui.statusbar.CommandQueue r4 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r4 = r4.mCallbacks
                int r4 = r4.size()
                if (r3 >= r4) goto L_0x0326
                java.lang.Object r4 = r13.obj
                android.util.Pair r4 = (android.util.Pair) r4
                com.android.systemui.statusbar.CommandQueue r5 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r5 = r5.mCallbacks
                java.lang.Object r5 = r5.get(r3)
                r6 = r5
                com.android.systemui.statusbar.CommandQueue$Callbacks r6 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r6
                java.lang.Object r5 = r4.first
                java.lang.Long r5 = (java.lang.Long) r5
                long r7 = r5.longValue()
                java.lang.Object r5 = r4.second
                java.lang.Long r5 = (java.lang.Long) r5
                long r9 = r5.longValue()
                int r5 = r13.arg1
                if (r5 == 0) goto L_0x031f
                r11 = r1
                goto L_0x0320
            L_0x031f:
                r11 = r2
            L_0x0320:
                r6.appTransitionStarting(r7, r9, r11)
                int r3 = r3 + 1
                goto L_0x02ec
            L_0x0326:
                goto L_0x0606
            L_0x0328:
            L_0x0329:
                r1 = r2
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                int r2 = r2.size()
                if (r1 >= r2) goto L_0x0348
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                java.lang.Object r2 = r2.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r2 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r2
                r2.appTransitionCancelled()
                int r2 = r1 + 1
                goto L_0x0329
            L_0x0348:
                goto L_0x0606
            L_0x034a:
                r3 = r2
            L_0x034b:
                com.android.systemui.statusbar.CommandQueue r4 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r4 = r4.mCallbacks
                int r4 = r4.size()
                if (r3 >= r4) goto L_0x0370
                com.android.systemui.statusbar.CommandQueue r4 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r4 = r4.mCallbacks
                java.lang.Object r4 = r4.get(r3)
                com.android.systemui.statusbar.CommandQueue$Callbacks r4 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r4
                int r5 = r13.arg1
                if (r5 == 0) goto L_0x0369
                r5 = r1
                goto L_0x036a
            L_0x0369:
                r5 = r2
            L_0x036a:
                r4.appTransitionPending(r5)
                int r3 = r3 + 1
                goto L_0x034b
            L_0x0370:
                goto L_0x0606
            L_0x0372:
            L_0x0373:
                r1 = r2
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                int r2 = r2.size()
                if (r1 >= r2) goto L_0x0394
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                java.lang.Object r2 = r2.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r2 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r2
                int r3 = r13.arg1
                r2.showScreenPinningRequest(r3)
                int r2 = r1 + 1
                goto L_0x0373
            L_0x0394:
                goto L_0x0606
            L_0x0396:
                r3 = r2
            L_0x0397:
                com.android.systemui.statusbar.CommandQueue r4 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r4 = r4.mCallbacks
                int r4 = r4.size()
                if (r3 >= r4) goto L_0x03c3
                com.android.systemui.statusbar.CommandQueue r4 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r4 = r4.mCallbacks
                java.lang.Object r4 = r4.get(r3)
                com.android.systemui.statusbar.CommandQueue$Callbacks r4 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r4
                int r5 = r13.arg1
                if (r5 == 0) goto L_0x03b5
                r5 = r1
                goto L_0x03b6
            L_0x03b5:
                r5 = r2
            L_0x03b6:
                int r6 = r13.arg2
                if (r6 == 0) goto L_0x03bc
                r6 = r1
                goto L_0x03bd
            L_0x03bc:
                r6 = r2
            L_0x03bd:
                r4.hideRecentApps(r5, r6)
                int r3 = r3 + 1
                goto L_0x0397
            L_0x03c3:
                goto L_0x0606
            L_0x03c5:
                r3 = r2
            L_0x03c6:
                com.android.systemui.statusbar.CommandQueue r4 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r4 = r4.mCallbacks
                int r4 = r4.size()
                if (r3 >= r4) goto L_0x03f2
                com.android.systemui.statusbar.CommandQueue r4 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r4 = r4.mCallbacks
                java.lang.Object r4 = r4.get(r3)
                com.android.systemui.statusbar.CommandQueue$Callbacks r4 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r4
                int r5 = r13.arg1
                if (r5 == 0) goto L_0x03e4
                r5 = r1
                goto L_0x03e5
            L_0x03e4:
                r5 = r2
            L_0x03e5:
                int r6 = r13.arg2
                if (r6 == 0) goto L_0x03eb
                r6 = r1
                goto L_0x03ec
            L_0x03eb:
                r6 = r2
            L_0x03ec:
                r4.showRecentApps(r5, r6)
                int r3 = r3 + 1
                goto L_0x03c6
            L_0x03f2:
                goto L_0x0606
            L_0x03f4:
            L_0x03f5:
                r1 = r2
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                int r2 = r2.size()
                if (r1 >= r2) goto L_0x0418
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                java.lang.Object r2 = r2.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r2 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r2
                int r3 = r13.arg1
                int r4 = r13.arg2
                r2.setWindowState(r3, r4)
                int r2 = r1 + 1
                goto L_0x03f5
            L_0x0418:
                goto L_0x0606
            L_0x041a:
            L_0x041b:
                r1 = r2
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                int r2 = r2.size()
                if (r1 >= r2) goto L_0x043a
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                java.lang.Object r2 = r2.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r2 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r2
                r2.cancelPreloadRecentApps()
                int r2 = r1 + 1
                goto L_0x041b
            L_0x043a:
                goto L_0x0606
            L_0x043c:
            L_0x043d:
                r1 = r2
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                int r2 = r2.size()
                if (r1 >= r2) goto L_0x045c
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                java.lang.Object r2 = r2.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r2 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r2
                r2.preloadRecentApps()
                int r2 = r1 + 1
                goto L_0x043d
            L_0x045c:
                goto L_0x0606
            L_0x045e:
            L_0x045f:
                r1 = r2
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                int r2 = r2.size()
                if (r1 >= r2) goto L_0x047e
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                java.lang.Object r2 = r2.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r2 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r2
                r2.toggleRecentApps()
                int r2 = r1 + 1
                goto L_0x045f
            L_0x047e:
                goto L_0x0606
            L_0x0480:
                r1 = r2
            L_0x0481:
                com.android.systemui.statusbar.CommandQueue r3 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r3 = r3.mCallbacks
                int r3 = r3.size()
                if (r1 >= r3) goto L_0x04b1
                com.android.systemui.statusbar.CommandQueue r3 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r3 = r3.mCallbacks
                java.lang.Object r3 = r3.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r3 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r3
                java.lang.Object r4 = r13.obj
                android.os.IBinder r4 = (android.os.IBinder) r4
                int r5 = r13.arg1
                int r6 = r13.arg2
                android.os.Bundle r7 = r13.getData()
                java.lang.String r8 = "showImeSwitcherKey"
                boolean r7 = r7.getBoolean(r8, r2)
                r3.setImeWindowStatus(r4, r5, r6, r7)
                int r1 = r1 + 1
                goto L_0x0481
            L_0x04b1:
                goto L_0x0606
            L_0x04b3:
                r3 = r2
            L_0x04b4:
                com.android.systemui.statusbar.CommandQueue r4 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r4 = r4.mCallbacks
                int r4 = r4.size()
                if (r3 >= r4) goto L_0x04d9
                com.android.systemui.statusbar.CommandQueue r4 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r4 = r4.mCallbacks
                java.lang.Object r4 = r4.get(r3)
                com.android.systemui.statusbar.CommandQueue$Callbacks r4 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r4
                int r5 = r13.arg1
                if (r5 == 0) goto L_0x04d2
                r5 = r1
                goto L_0x04d3
            L_0x04d2:
                r5 = r2
            L_0x04d3:
                r4.topAppWindowChanged(r5)
                int r3 = r3 + 1
                goto L_0x04b4
            L_0x04d9:
                goto L_0x0606
            L_0x04db:
                java.lang.Object r1 = r13.obj
                com.android.internal.os.SomeArgs r1 = (com.android.internal.os.SomeArgs) r1
            L_0x04e0:
                com.android.systemui.statusbar.CommandQueue r3 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r3 = r3.mCallbacks
                int r3 = r3.size()
                if (r2 >= r3) goto L_0x0511
                com.android.systemui.statusbar.CommandQueue r3 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r3 = r3.mCallbacks
                java.lang.Object r3 = r3.get(r2)
                r4 = r3
                com.android.systemui.statusbar.CommandQueue$Callbacks r4 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r4
                int r5 = r1.argi1
                int r6 = r1.argi2
                int r7 = r1.argi3
                int r8 = r1.argi4
                java.lang.Object r3 = r1.arg1
                r9 = r3
                android.graphics.Rect r9 = (android.graphics.Rect) r9
                java.lang.Object r3 = r1.arg2
                r10 = r3
                android.graphics.Rect r10 = (android.graphics.Rect) r10
                r4.setSystemUiVisibility(r5, r6, r7, r8, r9, r10)
                int r2 = r2 + 1
                goto L_0x04e0
            L_0x0511:
                r1.recycle()
                goto L_0x0606
            L_0x0516:
            L_0x0517:
                r1 = r2
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                int r2 = r2.size()
                if (r1 >= r2) goto L_0x053a
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                java.lang.Object r2 = r2.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r2 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r2
                java.lang.Object r3 = r13.obj
                java.lang.String r3 = (java.lang.String) r3
                r2.animateExpandSettingsPanel(r3)
                int r2 = r1 + 1
                goto L_0x0517
            L_0x053a:
                goto L_0x0606
            L_0x053c:
            L_0x053d:
                r1 = r2
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                int r2 = r2.size()
                if (r1 >= r2) goto L_0x055e
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                java.lang.Object r2 = r2.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r2 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r2
                int r3 = r13.arg1
                r2.animateCollapsePanels(r3)
                int r2 = r1 + 1
                goto L_0x053d
            L_0x055e:
                goto L_0x0606
            L_0x0560:
            L_0x0561:
                r1 = r2
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                int r2 = r2.size()
                if (r1 >= r2) goto L_0x0580
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                java.lang.Object r2 = r2.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r2 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r2
                r2.animateExpandNotificationsPanel()
                int r2 = r1 + 1
                goto L_0x0561
            L_0x0580:
                goto L_0x0606
            L_0x0582:
            L_0x0583:
                r1 = r2
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                int r2 = r2.size()
                if (r1 >= r2) goto L_0x05ae
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                java.lang.Object r2 = r2.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r2 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r2
                int r3 = r13.arg1
                int r4 = r13.arg2
                java.lang.Object r5 = r13.obj
                java.lang.Boolean r5 = (java.lang.Boolean) r5
                boolean r5 = r5.booleanValue()
                r2.disable(r3, r4, r5)
                int r2 = r1 + 1
                goto L_0x0583
            L_0x05ae:
                goto L_0x0606
            L_0x05af:
                int r1 = r13.arg1
                switch(r1) {
                    case 1: goto L_0x05d9;
                    case 2: goto L_0x05b5;
                    default: goto L_0x05b4;
                }
            L_0x05b4:
                goto L_0x0605
            L_0x05b5:
            L_0x05b6:
                r1 = r2
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                int r2 = r2.size()
                if (r1 >= r2) goto L_0x0605
                com.android.systemui.statusbar.CommandQueue r2 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r2 = r2.mCallbacks
                java.lang.Object r2 = r2.get(r1)
                com.android.systemui.statusbar.CommandQueue$Callbacks r2 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r2
                java.lang.Object r3 = r13.obj
                java.lang.String r3 = (java.lang.String) r3
                r2.removeIcon(r3)
                int r2 = r1 + 1
                goto L_0x05b6
            L_0x05d9:
                java.lang.Object r1 = r13.obj
                android.util.Pair r1 = (android.util.Pair) r1
            L_0x05de:
                com.android.systemui.statusbar.CommandQueue r3 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r3 = r3.mCallbacks
                int r3 = r3.size()
                if (r2 >= r3) goto L_0x0604
                com.android.systemui.statusbar.CommandQueue r3 = com.android.systemui.statusbar.CommandQueue.this
                java.util.ArrayList r3 = r3.mCallbacks
                java.lang.Object r3 = r3.get(r2)
                com.android.systemui.statusbar.CommandQueue$Callbacks r3 = (com.android.systemui.statusbar.CommandQueue.Callbacks) r3
                java.lang.Object r4 = r1.first
                java.lang.String r4 = (java.lang.String) r4
                java.lang.Object r5 = r1.second
                com.android.internal.statusbar.StatusBarIcon r5 = (com.android.internal.statusbar.StatusBarIcon) r5
                r3.setIcon(r4, r5)
                int r2 = r2 + 1
                goto L_0x05de
            L_0x0604:
            L_0x0605:
            L_0x0606:
                return
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.CommandQueue.H.handleMessage(android.os.Message):void");
        }
    }

    protected CommandQueue() {
    }

    public void addCallbacks(Callbacks callbacks) {
        this.mCallbacks.add(callbacks);
        callbacks.disable(this.mDisable1, this.mDisable2, false);
    }

    public void removeCallbacks(Callbacks callbacks) {
        this.mCallbacks.remove(callbacks);
    }

    public void setIcon(String slot, StatusBarIcon icon) {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(65536, 1, 0, new Pair(slot, icon)).sendToTarget();
        }
    }

    public void removeIcon(String slot) {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(65536, 2, 0, slot).sendToTarget();
        }
    }

    public void disable(int state1, int state2) {
        disable(state1, state2, true);
    }

    public void disable(int state1, int state2, boolean animate) {
        synchronized (this.mLock) {
            this.mDisable1 = state1;
            this.mDisable2 = state2;
            this.mHandler.removeMessages(131072);
            Message msg = this.mHandler.obtainMessage(131072, state1, state2, Boolean.valueOf(animate));
            if (Looper.myLooper() == this.mHandler.getLooper()) {
                this.mHandler.handleMessage(msg);
                msg.recycle();
            } else {
                msg.sendToTarget();
            }
        }
    }

    public void recomputeDisableFlags(boolean animate) {
        if (!this.mHandler.hasMessages(131072)) {
            synchronized (this.mLock) {
                disable(this.mDisable1, this.mDisable2, animate);
            }
            return;
        }
        Log.d("StatusBar", "give up recomputeDisableFlags");
    }

    public void animateExpandNotificationsPanel() {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(196608);
            this.mHandler.sendEmptyMessage(196608);
        }
    }

    public void animateCollapsePanels() {
        animateCollapsePanels(0);
    }

    public void animateCollapsePanels(int flags) {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(262144);
            this.mHandler.obtainMessage(262144, flags, 0).sendToTarget();
        }
    }

    public void togglePanel() {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(2293760);
            this.mHandler.obtainMessage(2293760, 0, 0).sendToTarget();
        }
    }

    public void animateExpandSettingsPanel(String subPanel) {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(327680);
            this.mHandler.obtainMessage(327680, subPanel).sendToTarget();
        }
    }

    public void setSystemUiVisibility(int vis, int fullscreenStackVis, int dockedStackVis, int mask, Rect fullscreenStackBounds, Rect dockedStackBounds) {
        synchronized (this.mLock) {
            SomeArgs args = SomeArgs.obtain();
            args.argi1 = vis;
            args.argi2 = fullscreenStackVis;
            args.argi3 = dockedStackVis;
            args.argi4 = mask;
            args.arg1 = fullscreenStackBounds;
            args.arg2 = dockedStackBounds;
            this.mHandler.obtainMessage(393216, args).sendToTarget();
        }
    }

    public void topAppWindowChanged(boolean menuVisible) {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(458752);
            this.mHandler.obtainMessage(458752, menuVisible, 0, null).sendToTarget();
        }
    }

    public void setImeWindowStatus(IBinder token, int vis, int backDisposition, boolean showImeSwitcher) {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(524288);
            Message m = this.mHandler.obtainMessage(524288, vis, backDisposition, token);
            m.getData().putBoolean("showImeSwitcherKey", showImeSwitcher);
            m.sendToTarget();
        }
    }

    public void showRecentApps(boolean triggeredFromAltTab) {
        showRecentApps(triggeredFromAltTab, false);
    }

    public void showRecentApps(boolean triggeredFromAltTab, boolean fromHome) {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(851968);
            this.mHandler.obtainMessage(851968, triggeredFromAltTab, fromHome, null).sendToTarget();
        }
    }

    public void hideRecentApps(boolean triggeredFromAltTab, boolean triggeredFromHomeKey) {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(917504);
            this.mHandler.obtainMessage(917504, triggeredFromAltTab, triggeredFromHomeKey, null).sendToTarget();
        }
    }

    public void toggleSplitScreen() {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(1966080);
            this.mHandler.obtainMessage(1966080, 0, 0, null).sendToTarget();
        }
    }

    public void toggleRecentApps() {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(589824);
            Message msg = this.mHandler.obtainMessage(589824, 0, 0, null);
            msg.setAsynchronous(true);
            msg.sendToTarget();
        }
    }

    public void preloadRecentApps() {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(655360);
            this.mHandler.obtainMessage(655360, 0, 0, null).sendToTarget();
        }
    }

    public void cancelPreloadRecentApps() {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(720896);
            this.mHandler.obtainMessage(720896, 0, 0, null).sendToTarget();
        }
    }

    public void dismissKeyboardShortcutsMenu() {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(2097152);
            this.mHandler.obtainMessage(2097152).sendToTarget();
        }
    }

    public void toggleKeyboardShortcutsMenu(int deviceId) {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(1638400);
            this.mHandler.obtainMessage(1638400, deviceId, 0).sendToTarget();
        }
    }

    public void showPictureInPictureMenu() {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(1703936);
            this.mHandler.obtainMessage(1703936).sendToTarget();
        }
    }

    public void setStatus(int what, String action, Bundle ext) {
        synchronized (this.mLock) {
            Bundle b = new Bundle();
            b.putInt("what", what);
            b.putString("action", action);
            b.putParcelable("ext", ext);
            this.mHandler.obtainMessage(6553600, 0, 0, b).sendToTarget();
        }
    }

    public void setWindowState(int window, int state) {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(786432, window, state, null).sendToTarget();
        }
    }

    public void showScreenPinningRequest(int taskId) {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(1179648, taskId, 0, null).sendToTarget();
        }
    }

    public void appTransitionPending() {
        appTransitionPending(false);
    }

    public void appTransitionPending(boolean forced) {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(1245184, forced, 0).sendToTarget();
        }
    }

    public void appTransitionCancelled() {
        synchronized (this.mLock) {
            this.mHandler.sendEmptyMessage(1310720);
        }
    }

    public void appTransitionStarting(long startTime, long duration) {
        appTransitionStarting(startTime, duration, false);
    }

    public void appTransitionStarting(long startTime, long duration, boolean forced) {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(1376256, forced, 0, Pair.create(Long.valueOf(startTime), Long.valueOf(duration))).sendToTarget();
        }
    }

    public void appTransitionFinished() {
        synchronized (this.mLock) {
            this.mHandler.sendEmptyMessage(2031616);
        }
    }

    public void showAssistDisclosure() {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(1441792);
            this.mHandler.obtainMessage(1441792).sendToTarget();
        }
    }

    public void startAssist(Bundle args) {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(1507328);
            this.mHandler.obtainMessage(1507328, args).sendToTarget();
        }
    }

    public void onCameraLaunchGestureDetected(int source) {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(1572864);
            this.mHandler.obtainMessage(1572864, source, 0).sendToTarget();
        }
    }

    public void addQsTile(ComponentName tile) {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(1769472, tile).sendToTarget();
        }
    }

    public void remQsTile(ComponentName tile) {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(1835008, tile).sendToTarget();
        }
    }

    public void clickQsTile(ComponentName tile) {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(1900544, tile).sendToTarget();
        }
    }

    public void handleSystemKey(int key) {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(2162688, key, 0).sendToTarget();
        }
    }

    public void showPinningEnterExitToast(boolean entering) {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(2949120, Boolean.valueOf(entering)).sendToTarget();
        }
    }

    public void showPinningEscapeToast() {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(3014656).sendToTarget();
        }
    }

    public void showGlobalActionsMenu() {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(2228224);
            this.mHandler.obtainMessage(2228224).sendToTarget();
        }
    }

    public void setTopAppHidesStatusBar(boolean hidesStatusBar) {
        this.mHandler.removeMessages(2424832);
        this.mHandler.obtainMessage(2424832, hidesStatusBar, 0).sendToTarget();
    }

    public void showShutdownUi(boolean isReboot, String reason) {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(2359296);
            this.mHandler.obtainMessage(2359296, isReboot, 0, reason).sendToTarget();
        }
    }

    public void showWirelessChargingAnimation(int batteryLevel) {
        this.mHandler.removeMessages(2883584);
        this.mHandler.obtainMessage(2883584, batteryLevel, 0).sendToTarget();
    }

    public void onProposedRotationChanged(int rotation, boolean isValid) {
        synchronized (this.mLock) {
            this.mHandler.removeMessages(2490368);
            this.mHandler.obtainMessage(2490368, rotation, isValid, null).sendToTarget();
        }
    }

    public void showFingerprintDialog(SomeArgs args) {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(2555904, args).sendToTarget();
        }
    }

    public void onFingerprintAuthenticated() {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(2621440).sendToTarget();
        }
    }

    public void onFingerprintHelp(String message) {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(2686976, message).sendToTarget();
        }
    }

    public void onFingerprintError(String error) {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(2752512, error).sendToTarget();
        }
    }

    public void hideFingerprintDialog() {
        synchronized (this.mLock) {
            this.mHandler.obtainMessage(2818048).sendToTarget();
        }
    }
}
