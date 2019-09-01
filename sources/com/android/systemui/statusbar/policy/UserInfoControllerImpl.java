package com.android.systemui.statusbar.policy;

import android.app.ActivityManagerCompat;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.UserManagerCompat;
import android.util.Log;
import com.android.internal.util.UserIconsCompat;
import com.android.settingslib.drawable.UserIconDrawable;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.UserInfoController;
import java.util.ArrayList;
import java.util.Iterator;

public class UserInfoControllerImpl implements UserInfoController {
    private final ArrayList<UserInfoController.OnUserInfoChangedListener> mCallbacks = new ArrayList<>();
    /* access modifiers changed from: private */
    public final Context mContext;
    private final BroadcastReceiver mProfileReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.provider.Contacts.PROFILE_CHANGED".equals(action) || "android.intent.action.USER_INFO_CHANGED".equals(action)) {
                try {
                    if (intent.getIntExtra("android.intent.extra.user_handle", getSendingUserId()) == ActivityManagerCompat.getService().getCurrentUser().id) {
                        UserInfoControllerImpl.this.reloadUserInfo();
                    }
                } catch (RemoteException e) {
                    Log.e("UserInfoController", "Couldn't get current user id for profile change", e);
                }
            }
        }
    };
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.USER_SWITCHED".equals(intent.getAction())) {
                UserInfoControllerImpl.this.reloadUserInfo();
            }
        }
    };
    /* access modifiers changed from: private */
    public String mUserAccount;
    /* access modifiers changed from: private */
    public Drawable mUserDrawable;
    /* access modifiers changed from: private */
    public AsyncTask<Void, Void, UserInfoQueryResult> mUserInfoTask;
    /* access modifiers changed from: private */
    public String mUserName;

    private static class UserInfoQueryResult {
        private Drawable mAvatar;
        private String mName;
        private String mUserAccount;

        public UserInfoQueryResult(String name, Drawable avatar, String userAccount) {
            this.mName = name;
            this.mAvatar = avatar;
            this.mUserAccount = userAccount;
        }

        public String getName() {
            return this.mName;
        }

        public Drawable getAvatar() {
            return this.mAvatar;
        }

        public String getUserAccount() {
            return this.mUserAccount;
        }
    }

    public UserInfoControllerImpl(Context context) {
        this.mContext = context;
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.USER_SWITCHED");
        this.mContext.registerReceiver(this.mReceiver, filter);
        IntentFilter profileFilter = new IntentFilter();
        profileFilter.addAction("android.provider.Contacts.PROFILE_CHANGED");
        profileFilter.addAction("android.intent.action.USER_INFO_CHANGED");
        this.mContext.registerReceiverAsUser(this.mProfileReceiver, UserHandle.ALL, profileFilter, null, null);
    }

    public void addCallback(UserInfoController.OnUserInfoChangedListener callback) {
        this.mCallbacks.add(callback);
        callback.onUserInfoChanged(this.mUserName, this.mUserDrawable, this.mUserAccount);
    }

    public void removeCallback(UserInfoController.OnUserInfoChangedListener callback) {
        this.mCallbacks.remove(callback);
    }

    public void reloadUserInfo() {
        if (this.mUserInfoTask != null) {
            this.mUserInfoTask.cancel(false);
            this.mUserInfoTask = null;
        }
        try {
            queryForUserInformation();
        } catch (Exception e) {
            Log.e("UserInfoController", "Couldn't query user info", e);
        }
    }

    private void queryForUserInformation() {
        try {
            UserInfo userInfo = ActivityManagerCompat.getService().getCurrentUser();
            Context currentUserContext = this.mContext.createPackageContextAsUser("android", 0, new UserHandle(userInfo.id));
            int userId = userInfo.id;
            boolean isGuest = userInfo.isGuest();
            String userName = userInfo.name;
            Resources res = this.mContext.getResources();
            Context context = currentUserContext;
            final String str = userName;
            final int i = userId;
            final int max = Math.max(res.getDimensionPixelSize(R.dimen.multi_user_avatar_expanded_size), res.getDimensionPixelSize(R.dimen.multi_user_avatar_keyguard_size));
            AnonymousClass3 r8 = r1;
            final boolean z = isGuest;
            AnonymousClass3 r1 = new AsyncTask<Void, Void, UserInfoQueryResult>() {
                /* access modifiers changed from: protected */
                public UserInfoQueryResult doInBackground(Void... params) {
                    Drawable avatar;
                    UserManager um = UserManager.get(UserInfoControllerImpl.this.mContext);
                    String name = str;
                    Bitmap rawAvatar = um.getUserIcon(i);
                    if (rawAvatar != null) {
                        avatar = new UserIconDrawable(max).setIcon(rawAvatar).setBadgeIfManagedUser(UserInfoControllerImpl.this.mContext, i).bake();
                    } else {
                        avatar = UserIconsCompat.getDefaultUserIcon(UserInfoControllerImpl.this.mContext.getResources(), z ? -10000 : i, true);
                    }
                    if (um.getUsers().size() <= 1) {
                        Cursor cursor = null;
                        if (cursor != null) {
                            try {
                                if (cursor.moveToFirst()) {
                                    name = cursor.getString(cursor.getColumnIndex("display_name"));
                                }
                            } finally {
                                cursor.close();
                            }
                        }
                    }
                    return new UserInfoQueryResult(name, avatar, UserManagerCompat.getUserAccount(um, i));
                }

                /* access modifiers changed from: protected */
                public void onPostExecute(UserInfoQueryResult result) {
                    String unused = UserInfoControllerImpl.this.mUserName = result.getName();
                    Drawable unused2 = UserInfoControllerImpl.this.mUserDrawable = result.getAvatar();
                    String unused3 = UserInfoControllerImpl.this.mUserAccount = result.getUserAccount();
                    AsyncTask unused4 = UserInfoControllerImpl.this.mUserInfoTask = null;
                    UserInfoControllerImpl.this.notifyChanged();
                }
            };
            this.mUserInfoTask = r8;
            this.mUserInfoTask.execute(new Void[0]);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("UserInfoController", "Couldn't create user context", e);
            throw new RuntimeException(e);
        } catch (RemoteException e2) {
            Log.e("UserInfoController", "Couldn't get user info", e2);
            throw new RuntimeException(e2);
        }
    }

    /* access modifiers changed from: private */
    public void notifyChanged() {
        Iterator<UserInfoController.OnUserInfoChangedListener> it = this.mCallbacks.iterator();
        while (it.hasNext()) {
            it.next().onUserInfoChanged(this.mUserName, this.mUserDrawable, this.mUserAccount);
        }
    }

    public void onDensityOrFontScaleChanged() {
        reloadUserInfo();
    }
}
