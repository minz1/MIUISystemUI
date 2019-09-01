package com.android.systemui;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class SystemUISecondaryUserService extends Service {
    public void onCreate() {
        super.onCreate();
        ((Application) getApplication()).getSystemUIApplication().startSecondaryUserServicesIfNeeded();
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    /* access modifiers changed from: protected */
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        SystemUI[] services = ((Application) getApplication()).getSystemUIApplication().getServices();
        int i = 0;
        if (args == null || args.length == 0) {
            int length = services.length;
            while (i < length) {
                SystemUI ui = services[i];
                if (ui != null) {
                    pw.println("dumping service: " + ui.getClass().getName());
                    ui.dump(fd, pw, args);
                }
                i++;
            }
            return;
        }
        String svc = args[0];
        int length2 = services.length;
        while (i < length2) {
            SystemUI ui2 = services[i];
            if (ui2 != null && ui2.getClass().getName().endsWith(svc)) {
                ui2.dump(fd, pw, args);
            }
            i++;
        }
    }
}
