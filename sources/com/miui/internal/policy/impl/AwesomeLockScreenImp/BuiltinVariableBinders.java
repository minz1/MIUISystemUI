package com.miui.internal.policy.impl.AwesomeLockScreenImp;

import miui.maml.data.VariableBinderManager;

public class BuiltinVariableBinders {
    public static void fill(VariableBinderManager m) {
        fillMissedCall(m);
        fillUnreadSms(m);
    }

    private static void fillMissedCall(VariableBinderManager m) {
        m.addContentProviderBinder("content://call_log/calls").setColumns(new String[]{"_id", "number"}).setWhere("type=3 AND new=1").setCountName("call_missed_count");
    }

    private static void fillUnreadSms(VariableBinderManager m) {
        m.addContentProviderBinder("content://sms/inbox").setColumns(new String[]{"_id"}).setWhere("seen=0 AND read=0").setCountName("sms_unread_count");
    }
}
