package com.android.systemui;

import android.app.ActivityManagerCompat;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.os.RemoteException;
import android.os.UserHandleCompat;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Log;
import android.view.WindowManagerGlobal;
import com.android.systemui.statusbar.phone.SystemUIDialog;

public class GuestResumeSessionReceiver extends BroadcastReceiver {
    private Dialog mNewSessionDialog;

    private static class ResetSessionDialog extends SystemUIDialog implements DialogInterface.OnClickListener {
        private final int mUserId;

        public ResetSessionDialog(Context context, int userId) {
            super(context);
            setTitle(context.getString(R.string.guest_wipe_session_title));
            setMessage(context.getString(R.string.guest_wipe_session_message));
            setCanceledOnTouchOutside(false);
            setButton(-2, context.getString(R.string.guest_wipe_session_wipe), this);
            setButton(-1, context.getString(R.string.guest_wipe_session_dontwipe), this);
            this.mUserId = userId;
        }

        public void onClick(DialogInterface dialog, int which) {
            if (which == -2) {
                GuestResumeSessionReceiver.wipeGuestSession(getContext(), this.mUserId);
                dismiss();
            } else if (which == -1) {
                cancel();
            }
        }
    }

    public void register(Context context) {
        Context context2 = context;
        context2.registerReceiverAsUser(this, UserHandleCompat.SYSTEM, new IntentFilter("android.intent.action.USER_SWITCHED"), null, null);
    }

    /* JADX WARNING: type inference failed for: r5v2, types: [com.android.systemui.GuestResumeSessionReceiver$ResetSessionDialog, android.app.Dialog] */
    public void onReceive(Context context, Intent intent) {
        if ("android.intent.action.USER_SWITCHED".equals(intent.getAction())) {
            cancelDialog();
            int userId = intent.getIntExtra("android.intent.extra.user_handle", -10000);
            if (userId == -10000) {
                Log.e("GuestResumeSessionReceiver", intent + " sent to " + "GuestResumeSessionReceiver" + " without EXTRA_USER_HANDLE");
                return;
            }
            try {
                if (ActivityManagerCompat.getService().getCurrentUser().isGuest()) {
                    ContentResolver cr = context.getContentResolver();
                    if (Settings.System.getIntForUser(cr, "systemui.guest_has_logged_in", 0, userId) != 0) {
                        this.mNewSessionDialog = new ResetSessionDialog(context, userId);
                        this.mNewSessionDialog.show();
                    } else {
                        Settings.System.putIntForUser(cr, "systemui.guest_has_logged_in", 1, userId);
                    }
                }
            } catch (RemoteException e) {
            }
        }
    }

    /* access modifiers changed from: private */
    public static void wipeGuestSession(Context context, int userId) {
        UserManager userManager = (UserManager) context.getSystemService("user");
        try {
            UserInfo currentUser = ActivityManagerCompat.getService().getCurrentUser();
            if (currentUser.id != userId) {
                Log.w("GuestResumeSessionReceiver", "User requesting to start a new session (" + userId + ") is not current user (" + currentUser.id + ")");
            } else if (!currentUser.isGuest()) {
                Log.w("GuestResumeSessionReceiver", "User requesting to start a new session (" + userId + ") is not a guest");
            } else if (!userManager.markGuestForDeletion(currentUser.id)) {
                Log.w("GuestResumeSessionReceiver", "Couldn't mark the guest for deletion for user " + userId);
            } else {
                UserInfo newGuest = userManager.createGuest(context, currentUser.name);
                if (newGuest == null) {
                    try {
                        Log.e("GuestResumeSessionReceiver", "Could not create new guest, switching back to system user");
                        ActivityManagerCompat.getService().switchUser(0);
                        userManager.removeUser(currentUser.id);
                        WindowManagerGlobal.getWindowManagerService().lockNow(null);
                    } catch (RemoteException e) {
                        Log.e("GuestResumeSessionReceiver", "Couldn't wipe session because ActivityManager or WindowManager is dead");
                    }
                } else {
                    ActivityManagerCompat.getService().switchUser(newGuest.id);
                    userManager.removeUser(currentUser.id);
                }
            }
        } catch (RemoteException e2) {
            Log.e("GuestResumeSessionReceiver", "Couldn't wipe session because ActivityManager is dead");
        }
    }

    private void cancelDialog() {
        if (this.mNewSessionDialog != null && this.mNewSessionDialog.isShowing()) {
            this.mNewSessionDialog.cancel();
            this.mNewSessionDialog = null;
        }
    }
}
