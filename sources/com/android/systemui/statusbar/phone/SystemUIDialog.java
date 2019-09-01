package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.DialogInterface;
import android.view.WindowManager;
import miui.app.AlertDialog;

public class SystemUIDialog extends AlertDialog {
    private final Context mContext;

    public SystemUIDialog(Context context) {
        this(context, 3);
    }

    public SystemUIDialog(Context context, int theme) {
        super(context, theme);
        this.mContext = context;
        applyFlags(this);
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.setTitle(getClass().getSimpleName());
        getWindow().setAttributes(attrs);
    }

    public void setShowForAllUsers(boolean show) {
        setShowForAllUsers(this, show);
    }

    public void setMessage(int resId) {
        setMessage(this.mContext.getString(resId));
    }

    public void setPositiveButton(int resId, DialogInterface.OnClickListener onClick) {
        setButton(-1, this.mContext.getString(resId), onClick);
    }

    public void setNegativeButton(int resId, DialogInterface.OnClickListener onClick) {
        setButton(-2, this.mContext.getString(resId), onClick);
    }

    public static void setShowForAllUsers(AlertDialog dialog, boolean show) {
        if (show) {
            dialog.getWindow().getAttributes().privateFlags |= 16;
            return;
        }
        dialog.getWindow().getAttributes().privateFlags &= -17;
    }

    public static void applyFlags(AlertDialog dialog) {
        dialog.getWindow().setType(2014);
        dialog.getWindow().addFlags(655360);
    }
}
