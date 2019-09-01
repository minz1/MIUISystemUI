package com.android.systemui.statusbar.policy;

import android.app.ActivityManager;
import android.app.ActivityManagerCompat;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationCompat;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.content.pm.UserInfoCompat;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserHandleCompat;
import android.os.UserManager;
import android.os.UserManagerCompat;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.UserIcons;
import com.android.internal.util.UserIconsCompat;
import com.android.settingslib.RestrictedLockUtils;
import com.android.systemui.Dependency;
import com.android.systemui.GuestResumeSessionReceiver;
import com.android.systemui.R;
import com.android.systemui.SystemUI;
import com.android.systemui.SystemUISecondaryUserService;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.qs.DetailAdapter;
import com.android.systemui.qs.tiles.UserDetailView;
import com.android.systemui.statusbar.phone.SystemUIDialog;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.android.systemui.util.NotificationChannels;
import com.android.systemui.util.Utils;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class UserSwitcherController {
    private final ActivityStarter mActivityStarter;
    private final ArrayList<WeakReference<BaseUserAdapter>> mAdapters = new ArrayList<>();
    private Dialog mAddUserDialog;
    /* access modifiers changed from: private */
    public boolean mAddUsersWhenLocked;
    private final KeyguardMonitor.Callback mCallback = new KeyguardMonitor.Callback() {
        public void onKeyguardShowingChanged() {
            if (!UserSwitcherController.this.mKeyguardMonitor.isShowing()) {
                UserSwitcherController.this.mHandler.post(new Runnable() {
                    public void run() {
                        UserSwitcherController.this.notifyAdapters();
                    }
                });
            } else {
                UserSwitcherController.this.notifyAdapters();
            }
        }
    };
    protected final Context mContext;
    /* access modifiers changed from: private */
    public Dialog mExitGuestDialog;
    private SparseBooleanArray mForcePictureLoadForUserId = new SparseBooleanArray(2);
    private final GuestResumeSessionReceiver mGuestResumeSessionReceiver = new GuestResumeSessionReceiver();
    protected final Handler mHandler;
    /* access modifiers changed from: private */
    public final KeyguardMonitor mKeyguardMonitor;
    /* access modifiers changed from: private */
    public int mLastNonGuestUser = 0;
    /* access modifiers changed from: private */
    public boolean mPauseRefreshUsers;
    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        private int mCallState;

        public void onCallStateChanged(int state, String incomingNumber) {
            if (this.mCallState != state) {
                this.mCallState = state;
                int currentUserId = ActivityManager.getCurrentUser();
                UserInfo userInfo = UserSwitcherController.this.mUserManager.getUserInfo(currentUserId);
                if (userInfo != null && userInfo.isGuest()) {
                    UserSwitcherController.this.showGuestNotification(currentUserId);
                }
                UserSwitcherController.this.refreshUsers(-10000);
            }
        }
    };
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            boolean unpauseRefreshUsers = false;
            int forcePictureLoadForId = -10000;
            if ("com.android.systemui.REMOVE_GUEST".equals(intent.getAction())) {
                int currentUser = ActivityManager.getCurrentUser();
                UserInfo userInfo = UserSwitcherController.this.mUserManager.getUserInfo(currentUser);
                if (userInfo != null && userInfo.isGuest()) {
                    UserSwitcherController.this.showExitGuestDialog(currentUser);
                }
                return;
            }
            if ("com.android.systemui.LOGOUT_USER".equals(intent.getAction())) {
                UserSwitcherController.this.logoutCurrentUser();
            } else if ("android.intent.action.USER_SWITCHED".equals(intent.getAction())) {
                if (UserSwitcherController.this.mExitGuestDialog != null && UserSwitcherController.this.mExitGuestDialog.isShowing()) {
                    UserSwitcherController.this.mExitGuestDialog.cancel();
                    Dialog unused = UserSwitcherController.this.mExitGuestDialog = null;
                }
                int currentId = intent.getIntExtra("android.intent.extra.user_handle", -1);
                UserInfo userInfo2 = UserSwitcherController.this.mUserManager.getUserInfo(currentId);
                int N = UserSwitcherController.this.mUsers.size();
                int i = 0;
                while (i < N) {
                    UserRecord record = (UserRecord) UserSwitcherController.this.mUsers.get(i);
                    if (record.info != null) {
                        boolean shouldBeCurrent = record.info.id == currentId;
                        if (record.isCurrent != shouldBeCurrent) {
                            UserSwitcherController.this.mUsers.set(i, record.copyWithIsCurrent(shouldBeCurrent));
                        }
                        if (shouldBeCurrent && !record.isGuest) {
                            int unused2 = UserSwitcherController.this.mLastNonGuestUser = record.info.id;
                        }
                        if ((userInfo2 == null || !userInfo2.isAdmin()) && record.isRestricted) {
                            UserSwitcherController.this.mUsers.remove(i);
                            i--;
                        }
                    }
                    i++;
                }
                UserSwitcherController.this.notifyAdapters();
                if (UserSwitcherController.this.mSecondaryUser != -10000) {
                    context.stopServiceAsUser(UserSwitcherController.this.mSecondaryUserServiceIntent, UserHandleCompat.of(UserSwitcherController.this.mSecondaryUser));
                    int unused3 = UserSwitcherController.this.mSecondaryUser = -10000;
                }
                if (!(userInfo2 == null || userInfo2.id == 0)) {
                    context.startServiceAsUser(UserSwitcherController.this.mSecondaryUserServiceIntent, UserHandleCompat.of(userInfo2.id));
                    int unused4 = UserSwitcherController.this.mSecondaryUser = userInfo2.id;
                }
                if (UserManagerCompat.isSplitSystemUser() && userInfo2 != null && !userInfo2.isGuest() && userInfo2.id != 0) {
                    showLogoutNotification(currentId);
                }
                if (userInfo2 != null && userInfo2.isGuest()) {
                    UserSwitcherController.this.showGuestNotification(currentId);
                }
                unpauseRefreshUsers = true;
            } else if ("android.intent.action.USER_INFO_CHANGED".equals(intent.getAction())) {
                forcePictureLoadForId = intent.getIntExtra("android.intent.extra.user_handle", -10000);
            } else if ("android.intent.action.USER_UNLOCKED".equals(intent.getAction()) && intent.getIntExtra("android.intent.extra.user_handle", -10000) != 0) {
                return;
            }
            UserSwitcherController.this.refreshUsers(forcePictureLoadForId);
            if (unpauseRefreshUsers) {
                UserSwitcherController.this.mUnpauseRefreshUsers.run();
            }
        }

        private void showLogoutNotification(int userId) {
            PendingIntent logoutPI = PendingIntent.getBroadcastAsUser(UserSwitcherController.this.mContext, 0, new Intent("com.android.systemui.LOGOUT_USER"), 0, UserHandleCompat.SYSTEM);
            Notification.Builder builder = NotificationCompat.newBuilder(UserSwitcherController.this.mContext, NotificationChannels.GENERAL).setVisibility(-1).setSmallIcon(R.drawable.ic_person).setContentTitle(UserSwitcherController.this.mContext.getString(R.string.user_logout_notification_title)).setContentText(UserSwitcherController.this.mContext.getString(R.string.user_logout_notification_text)).setContentIntent(logoutPI).setOngoing(true).setShowWhen(false).addAction(R.drawable.ic_delete, UserSwitcherController.this.mContext.getString(R.string.user_logout_notification_action), logoutPI);
            SystemUI.overrideNotificationAppName(UserSwitcherController.this.mContext, builder);
            NotificationManager.from(UserSwitcherController.this.mContext).notifyAsUser("logout_user", 1011, builder.build(), new UserHandle(userId));
        }
    };
    private boolean mResumeUserOnGuestLogout = true;
    /* access modifiers changed from: private */
    public int mSecondaryUser = -10000;
    /* access modifiers changed from: private */
    public Intent mSecondaryUserServiceIntent;
    private final ContentObserver mSettingsObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            boolean z = true;
            boolean unused = UserSwitcherController.this.mSimpleUserSwitcher = Settings.Global.getInt(UserSwitcherController.this.mContext.getContentResolver(), "lockscreenSimpleUserSwitcher", 0) != 0;
            UserSwitcherController userSwitcherController = UserSwitcherController.this;
            if (Settings.Global.getInt(UserSwitcherController.this.mContext.getContentResolver(), "add_users_when_locked", 0) == 0) {
                z = false;
            }
            boolean unused2 = userSwitcherController.mAddUsersWhenLocked = z;
            UserSwitcherController.this.refreshUsers(-10000);
        }
    };
    /* access modifiers changed from: private */
    public boolean mSimpleUserSwitcher;
    /* access modifiers changed from: private */
    public final Runnable mUnpauseRefreshUsers = new Runnable() {
        public void run() {
            UserSwitcherController.this.mHandler.removeCallbacks(this);
            boolean unused = UserSwitcherController.this.mPauseRefreshUsers = false;
            UserSwitcherController.this.refreshUsers(-10000);
        }
    };
    protected final UserManager mUserManager;
    /* access modifiers changed from: private */
    public ArrayList<UserRecord> mUsers = new ArrayList<>();
    public final DetailAdapter userDetailAdapter = new DetailAdapter() {
        private final Intent USER_SETTINGS_INTENT = new Intent("android.settings.USER_SETTINGS");

        public CharSequence getTitle() {
            return UserSwitcherController.this.mContext.getString(R.string.quick_settings_user_title);
        }

        public View createDetailView(Context context, View convertView, ViewGroup parent) {
            UserDetailView v;
            if (!(convertView instanceof UserDetailView)) {
                v = UserDetailView.inflate(context, parent, false);
                v.createAndSetAdapter(UserSwitcherController.this);
            } else {
                v = (UserDetailView) convertView;
            }
            v.refreshAdapter();
            return v;
        }

        public Intent getSettingsIntent() {
            return this.USER_SETTINGS_INTENT;
        }

        public Boolean getToggleState() {
            return null;
        }

        public void setToggleState(boolean state) {
        }

        public int getMetricsCategory() {
            return 125;
        }

        public boolean getToggleEnabled() {
            return true;
        }

        public boolean hasHeader() {
            return true;
        }
    };

    private final class AddUserDialog extends SystemUIDialog implements DialogInterface.OnClickListener {
        public AddUserDialog(Context context) {
            super(context);
            setTitle(R.string.user_add_user_title);
            setMessage(context.getString(R.string.user_add_user_message_short));
            setButton(-2, context.getString(17039360), this);
            setButton(-1, context.getString(17039370), this);
        }

        public void onClick(DialogInterface dialog, int which) {
            if (which == -2) {
                cancel();
            } else {
                dismiss();
                if (!ActivityManager.isUserAMonkey()) {
                    UserInfo user = UserSwitcherController.this.mUserManager.createUser(UserSwitcherController.this.mContext.getString(R.string.user_new_user_name), 0);
                    if (user != null) {
                        int id = user.id;
                        UserSwitcherController.this.mUserManager.setUserIcon(id, UserIcons.convertToBitmap(UserIconsCompat.getDefaultUserIcon(UserSwitcherController.this.mContext.getResources(), id, false)));
                        UserSwitcherController.this.switchToUserId(id);
                    }
                }
            }
        }
    }

    public static abstract class BaseUserAdapter extends BaseAdapter {
        final UserSwitcherController mController;
        private final KeyguardMonitor mKeyguardMonitor = ((KeyguardMonitor) Dependency.get(KeyguardMonitor.class));

        protected BaseUserAdapter(UserSwitcherController controller) {
            this.mController = controller;
            controller.addAdapter(new WeakReference(this));
        }

        public int getUserCount() {
            if (!(this.mKeyguardMonitor.isShowing() && this.mKeyguardMonitor.isSecure() && !this.mKeyguardMonitor.canSkipBouncer())) {
                return this.mController.getUsers().size();
            }
            int N = this.mController.getUsers().size();
            int count = 0;
            for (int i = 0; i < N; i++) {
                if (!this.mController.getUsers().get(i).isGuest) {
                    if (this.mController.getUsers().get(i).isRestricted) {
                        break;
                    }
                    count++;
                }
            }
            return count;
        }

        public int getCount() {
            int i = 0;
            if (!(this.mKeyguardMonitor.isShowing() && this.mKeyguardMonitor.isSecure() && !this.mKeyguardMonitor.canSkipBouncer())) {
                return this.mController.getUsers().size();
            }
            int N = this.mController.getUsers().size();
            int count = 0;
            while (i < N && !this.mController.getUsers().get(i).isRestricted) {
                count++;
                i++;
            }
            return count;
        }

        public UserRecord getItem(int position) {
            return this.mController.getUsers().get(position);
        }

        public long getItemId(int position) {
            return (long) position;
        }

        public void switchTo(UserRecord record) {
            this.mController.switchTo(record);
        }

        public String getName(Context context, UserRecord item) {
            if (item.isGuest) {
                if (item.isCurrent) {
                    return context.getString(R.string.guest_exit_guest);
                }
                return context.getString(item.info == null ? R.string.guest_new_guest : R.string.guest_nickname);
            } else if (item.isAddUser) {
                return context.getString(R.string.user_add_user);
            } else {
                return item.info.name;
            }
        }

        public Drawable getDrawable(Context context, UserRecord item) {
            if (item.isAddUser) {
                return context.getDrawable(R.drawable.ic_add_circle_qs);
            }
            Drawable icon = UserIconsCompat.getDefaultUserIcon(context.getResources(), item.resolveId(), false);
            if (item.isGuest) {
                icon.setColorFilter(Utils.getColorAttr(context, 16842800), PorterDuff.Mode.SRC_IN);
            }
            return icon;
        }

        public void refresh() {
            this.mController.refreshUsers(-10000);
        }
    }

    private final class ExitGuestDialog extends SystemUIDialog implements DialogInterface.OnClickListener {
        private final int mGuestId;
        private final int mTargetId;

        public ExitGuestDialog(Context context, int guestId, int targetId) {
            super(context);
            setTitle(R.string.guest_exit_guest_dialog_title);
            setMessage(context.getString(R.string.guest_exit_guest_dialog_message));
            setButton(-2, context.getString(17039360), this);
            setButton(-1, context.getString(R.string.guest_exit_guest_dialog_remove), this);
            setCanceledOnTouchOutside(false);
            this.mGuestId = guestId;
            this.mTargetId = targetId;
        }

        public void onClick(DialogInterface dialog, int which) {
            if (which == -2) {
                cancel();
                return;
            }
            dismiss();
            UserSwitcherController.this.exitGuest(this.mGuestId, this.mTargetId);
        }
    }

    public static final class UserRecord {
        public RestrictedLockUtils.EnforcedAdmin enforcedAdmin;
        public final UserInfo info;
        public final boolean isAddUser;
        public final boolean isCurrent;
        public boolean isDisabledByAdmin;
        public final boolean isGuest;
        public final boolean isRestricted;
        public boolean isSwitchToEnabled;
        public final Bitmap picture;

        public UserRecord(UserInfo info2, Bitmap picture2, boolean isGuest2, boolean isCurrent2, boolean isAddUser2, boolean isRestricted2, boolean isSwitchToEnabled2) {
            this.info = info2;
            this.picture = picture2;
            this.isGuest = isGuest2;
            this.isCurrent = isCurrent2;
            this.isAddUser = isAddUser2;
            this.isRestricted = isRestricted2;
            this.isSwitchToEnabled = isSwitchToEnabled2;
        }

        public UserRecord copyWithIsCurrent(boolean _isCurrent) {
            UserRecord userRecord = new UserRecord(this.info, this.picture, this.isGuest, _isCurrent, this.isAddUser, this.isRestricted, this.isSwitchToEnabled);
            return userRecord;
        }

        public int resolveId() {
            if (this.isGuest || this.info == null) {
                return -10000;
            }
            return this.info.id;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("UserRecord(");
            if (this.info != null) {
                sb.append("name=\"");
                sb.append(this.info.name);
                sb.append("\" id=");
                sb.append(this.info.id);
            } else if (this.isGuest) {
                sb.append("<add guest placeholder>");
            } else if (this.isAddUser) {
                sb.append("<add user placeholder>");
            }
            if (this.isGuest) {
                sb.append(" <isGuest>");
            }
            if (this.isAddUser) {
                sb.append(" <isAddUser>");
            }
            if (this.isCurrent) {
                sb.append(" <isCurrent>");
            }
            if (this.picture != null) {
                sb.append(" <hasPicture>");
            }
            if (this.isRestricted) {
                sb.append(" <isRestricted>");
            }
            if (this.isDisabledByAdmin) {
                sb.append(" <isDisabledByAdmin>");
                sb.append(" enforcedAdmin=");
                sb.append(this.enforcedAdmin);
            }
            if (this.isSwitchToEnabled) {
                sb.append(" <isSwitchToEnabled>");
            }
            sb.append(')');
            return sb.toString();
        }
    }

    public UserSwitcherController(Context context, KeyguardMonitor keyguardMonitor, Handler handler, ActivityStarter activityStarter) {
        this.mContext = context;
        this.mGuestResumeSessionReceiver.register(context);
        this.mKeyguardMonitor = keyguardMonitor;
        this.mHandler = handler;
        this.mActivityStarter = activityStarter;
        this.mUserManager = UserManager.get(context);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.USER_ADDED");
        filter.addAction("android.intent.action.USER_REMOVED");
        filter.addAction("android.intent.action.USER_INFO_CHANGED");
        filter.addAction("android.intent.action.USER_SWITCHED");
        filter.addAction("android.intent.action.USER_STOPPED");
        filter.addAction("android.intent.action.USER_UNLOCKED");
        this.mContext.registerReceiverAsUser(this.mReceiver, UserHandleCompat.SYSTEM, filter, null, null);
        this.mSecondaryUserServiceIntent = new Intent(context, SystemUISecondaryUserService.class);
        IntentFilter filter2 = new IntentFilter();
        filter2.addAction("com.android.systemui.REMOVE_GUEST");
        filter2.addAction("com.android.systemui.LOGOUT_USER");
        this.mContext.registerReceiverAsUser(this.mReceiver, UserHandleCompat.SYSTEM, filter2, "com.android.systemui.permission.SELF", null);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("lockscreenSimpleUserSwitcher"), true, this.mSettingsObserver);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("add_users_when_locked"), true, this.mSettingsObserver);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("allow_user_switching_when_system_user_locked"), true, this.mSettingsObserver);
        this.mSettingsObserver.onChange(false);
        keyguardMonitor.addCallback(this.mCallback);
        listenForCallState();
        refreshUsers(-10000);
    }

    /* access modifiers changed from: private */
    public void refreshUsers(int forcePictureLoadForId) {
        if (forcePictureLoadForId != -10000) {
            this.mForcePictureLoadForUserId.put(forcePictureLoadForId, true);
        }
        if (!this.mPauseRefreshUsers) {
            boolean forceAllUsers = this.mForcePictureLoadForUserId.get(-1);
            SparseArray<Bitmap> bitmaps = new SparseArray<>(this.mUsers.size());
            int N = this.mUsers.size();
            for (int i = 0; i < N; i++) {
                UserRecord r = this.mUsers.get(i);
                if (!(r == null || r.picture == null || r.info == null || forceAllUsers || this.mForcePictureLoadForUserId.get(r.info.id))) {
                    bitmaps.put(r.info.id, r.picture);
                }
            }
            this.mForcePictureLoadForUserId.clear();
            final boolean addUsersWhenLocked = this.mAddUsersWhenLocked;
            new AsyncTask<SparseArray<Bitmap>, Void, ArrayList<UserRecord>>() {
                /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r8v24, resolved type: java.lang.Object} */
                /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r11v1, resolved type: android.content.pm.UserInfo} */
                /* access modifiers changed from: protected */
                /* JADX WARNING: Multi-variable type inference failed */
                /* JADX WARNING: Removed duplicated region for block: B:70:0x013c  */
                /* JADX WARNING: Removed duplicated region for block: B:79:0x0173  */
                /* Code decompiled incorrectly, please refer to instructions dump. */
                public java.util.ArrayList<com.android.systemui.statusbar.policy.UserSwitcherController.UserRecord> doInBackground(android.util.SparseArray<android.graphics.Bitmap>... r30) {
                    /*
                        r29 = this;
                        r0 = r29
                        r1 = 0
                        r2 = r30[r1]
                        com.android.systemui.statusbar.policy.UserSwitcherController r3 = com.android.systemui.statusbar.policy.UserSwitcherController.this
                        android.os.UserManager r3 = r3.mUserManager
                        r4 = 1
                        java.util.List r3 = r3.getUsers(r4)
                        if (r3 != 0) goto L_0x0012
                        r1 = 0
                        return r1
                    L_0x0012:
                        java.util.ArrayList r5 = new java.util.ArrayList
                        int r6 = r3.size()
                        r5.<init>(r6)
                        int r6 = android.app.ActivityManager.getCurrentUser()
                        com.android.systemui.statusbar.policy.UserSwitcherController r7 = com.android.systemui.statusbar.policy.UserSwitcherController.this
                        android.os.UserManager r7 = r7.mUserManager
                        boolean r7 = android.os.UserManagerCompat.canSwitchUsers(r7)
                        r8 = 0
                        r9 = 0
                        java.util.Iterator r14 = r3.iterator()
                        r13 = r8
                        r12 = r9
                    L_0x002f:
                        boolean r8 = r14.hasNext()
                        if (r8 == 0) goto L_0x00df
                        java.lang.Object r8 = r14.next()
                        r11 = r8
                        android.content.pm.UserInfo r11 = (android.content.pm.UserInfo) r11
                        int r8 = r11.id
                        if (r6 != r8) goto L_0x0042
                        r8 = r4
                        goto L_0x0043
                    L_0x0042:
                        r8 = r1
                    L_0x0043:
                        r24 = r8
                        if (r24 == 0) goto L_0x004b
                        r8 = r11
                        r25 = r8
                        goto L_0x004d
                    L_0x004b:
                        r25 = r13
                    L_0x004d:
                        if (r7 != 0) goto L_0x0055
                        if (r24 == 0) goto L_0x0052
                        goto L_0x0055
                    L_0x0052:
                        r23 = r1
                        goto L_0x0057
                    L_0x0055:
                        r23 = r4
                    L_0x0057:
                        boolean r8 = r11.isEnabled()
                        if (r8 == 0) goto L_0x00d2
                        boolean r8 = r11.isGuest()
                        if (r8 == 0) goto L_0x007f
                        com.android.systemui.statusbar.policy.UserSwitcherController$UserRecord r16 = new com.android.systemui.statusbar.policy.UserSwitcherController$UserRecord
                        r10 = 0
                        r13 = 1
                        r15 = 0
                        r17 = 0
                        r8 = r16
                        r9 = r11
                        r1 = r11
                        r11 = r13
                        r13 = r12
                        r12 = r24
                        r26 = r13
                        r13 = r15
                        r27 = r14
                        r14 = r17
                        r15 = r7
                        r8.<init>(r9, r10, r11, r12, r13, r14, r15)
                        r12 = r8
                        goto L_0x00d8
                    L_0x007f:
                        r1 = r11
                        r26 = r12
                        r27 = r14
                        boolean r8 = android.content.pm.UserInfoCompat.supportsSwitchToByUser(r1)
                        if (r8 == 0) goto L_0x00d6
                        int r8 = r1.id
                        java.lang.Object r8 = r2.get(r8)
                        android.graphics.Bitmap r8 = (android.graphics.Bitmap) r8
                        if (r8 != 0) goto L_0x00b3
                        com.android.systemui.statusbar.policy.UserSwitcherController r9 = com.android.systemui.statusbar.policy.UserSwitcherController.this
                        android.os.UserManager r9 = r9.mUserManager
                        int r10 = r1.id
                        android.graphics.Bitmap r8 = r9.getUserIcon(r10)
                        if (r8 == 0) goto L_0x00b3
                        com.android.systemui.statusbar.policy.UserSwitcherController r9 = com.android.systemui.statusbar.policy.UserSwitcherController.this
                        android.content.Context r9 = r9.mContext
                        android.content.res.Resources r9 = r9.getResources()
                        r10 = 2131165767(0x7f070247, float:1.794576E38)
                        int r9 = r9.getDimensionPixelSize(r10)
                        android.graphics.Bitmap r8 = android.graphics.Bitmap.createScaledBitmap(r8, r9, r9, r4)
                    L_0x00b3:
                        if (r24 == 0) goto L_0x00b7
                        r9 = 0
                        goto L_0x00bb
                    L_0x00b7:
                        int r9 = r5.size()
                    L_0x00bb:
                        com.android.systemui.statusbar.policy.UserSwitcherController$UserRecord r10 = new com.android.systemui.statusbar.policy.UserSwitcherController$UserRecord
                        r19 = 0
                        r21 = 0
                        r22 = 0
                        r16 = r10
                        r17 = r1
                        r18 = r8
                        r20 = r24
                        r16.<init>(r17, r18, r19, r20, r21, r22, r23)
                        r5.add(r9, r10)
                        goto L_0x00d6
                    L_0x00d2:
                        r26 = r12
                        r27 = r14
                    L_0x00d6:
                        r12 = r26
                    L_0x00d8:
                        r13 = r25
                        r14 = r27
                        r1 = 0
                        goto L_0x002f
                    L_0x00df:
                        r26 = r12
                        com.android.systemui.statusbar.policy.UserSwitcherController r1 = com.android.systemui.statusbar.policy.UserSwitcherController.this
                        android.os.UserManager r1 = r1.mUserManager
                        java.lang.String r8 = "no_add_user"
                        android.os.UserHandle r9 = android.os.UserHandleCompat.SYSTEM
                        boolean r1 = android.os.UserManagerCompat.hasBaseUserRestriction(r1, r8, r9)
                        r1 = r1 ^ r4
                        if (r13 == 0) goto L_0x00fe
                        boolean r8 = r13.isAdmin()
                        if (r8 != 0) goto L_0x00fa
                        int r8 = r13.id
                        if (r8 != 0) goto L_0x00fe
                    L_0x00fa:
                        if (r1 == 0) goto L_0x00fe
                        r8 = r4
                        goto L_0x00ff
                    L_0x00fe:
                        r8 = 0
                    L_0x00ff:
                        r23 = r8
                        if (r1 == 0) goto L_0x0109
                        boolean r8 = r5
                        if (r8 == 0) goto L_0x0109
                        r8 = r4
                        goto L_0x010a
                    L_0x0109:
                        r8 = 0
                    L_0x010a:
                        r24 = r8
                        if (r23 != 0) goto L_0x0114
                        if (r24 == 0) goto L_0x0111
                        goto L_0x0114
                    L_0x0111:
                        r12 = r26
                        goto L_0x011a
                    L_0x0114:
                        r12 = r26
                        if (r12 != 0) goto L_0x011a
                        r8 = r4
                        goto L_0x011b
                    L_0x011a:
                        r8 = 0
                    L_0x011b:
                        r25 = r8
                        if (r23 != 0) goto L_0x0121
                        if (r24 == 0) goto L_0x012d
                    L_0x0121:
                        com.android.systemui.statusbar.policy.UserSwitcherController r8 = com.android.systemui.statusbar.policy.UserSwitcherController.this
                        android.os.UserManager r8 = r8.mUserManager
                        boolean r8 = r8.canAddMoreUsers()
                        if (r8 == 0) goto L_0x012d
                        r8 = r4
                        goto L_0x012e
                    L_0x012d:
                        r8 = 0
                    L_0x012e:
                        r26 = r8
                        boolean r8 = r5
                        r14 = r8 ^ 1
                        com.android.systemui.statusbar.policy.UserSwitcherController r4 = com.android.systemui.statusbar.policy.UserSwitcherController.this
                        boolean r4 = r4.mSimpleUserSwitcher
                        if (r4 != 0) goto L_0x0173
                        if (r12 != 0) goto L_0x0161
                        if (r25 == 0) goto L_0x015e
                        com.android.systemui.statusbar.policy.UserSwitcherController$UserRecord r4 = new com.android.systemui.statusbar.policy.UserSwitcherController$UserRecord
                        r9 = 0
                        r10 = 0
                        r11 = 1
                        r15 = 0
                        r16 = 0
                        r8 = r4
                        r28 = r12
                        r12 = r15
                        r27 = r13
                        r13 = r16
                        r15 = r7
                        r8.<init>(r9, r10, r11, r12, r13, r14, r15)
                        r12 = r4
                        com.android.systemui.statusbar.policy.UserSwitcherController r4 = com.android.systemui.statusbar.policy.UserSwitcherController.this
                        r4.checkIfAddUserDisallowedByAdminOnly(r12)
                        r5.add(r12)
                        goto L_0x0175
                    L_0x015e:
                        r27 = r13
                        goto L_0x0175
                    L_0x0161:
                        r28 = r12
                        r27 = r13
                        boolean r4 = r12.isCurrent
                        if (r4 == 0) goto L_0x016b
                        r4 = 0
                        goto L_0x016f
                    L_0x016b:
                        int r4 = r5.size()
                    L_0x016f:
                        r5.add(r4, r12)
                        goto L_0x0175
                    L_0x0173:
                        r27 = r13
                    L_0x0175:
                        com.android.systemui.statusbar.policy.UserSwitcherController r4 = com.android.systemui.statusbar.policy.UserSwitcherController.this
                        boolean r4 = r4.mSimpleUserSwitcher
                        if (r4 != 0) goto L_0x019b
                        if (r26 == 0) goto L_0x019b
                        com.android.systemui.statusbar.policy.UserSwitcherController$UserRecord r4 = new com.android.systemui.statusbar.policy.UserSwitcherController$UserRecord
                        r16 = 0
                        r17 = 0
                        r18 = 0
                        r19 = 0
                        r20 = 1
                        r15 = r4
                        r21 = r14
                        r22 = r7
                        r15.<init>(r16, r17, r18, r19, r20, r21, r22)
                        com.android.systemui.statusbar.policy.UserSwitcherController r8 = com.android.systemui.statusbar.policy.UserSwitcherController.this
                        r8.checkIfAddUserDisallowedByAdminOnly(r4)
                        r5.add(r4)
                    L_0x019b:
                        return r5
                    */
                    throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.policy.UserSwitcherController.AnonymousClass1.doInBackground(android.util.SparseArray[]):java.util.ArrayList");
                }

                /* access modifiers changed from: protected */
                public void onPostExecute(ArrayList<UserRecord> userRecords) {
                    if (userRecords != null) {
                        ArrayList unused = UserSwitcherController.this.mUsers = userRecords;
                        UserSwitcherController.this.notifyAdapters();
                    }
                }
            }.execute(new SparseArray[]{bitmaps});
        }
    }

    private void pauseRefreshUsers() {
        if (!this.mPauseRefreshUsers) {
            this.mHandler.postDelayed(this.mUnpauseRefreshUsers, 3000);
            this.mPauseRefreshUsers = true;
        }
    }

    /* access modifiers changed from: private */
    public void notifyAdapters() {
        for (int i = this.mAdapters.size() - 1; i >= 0; i--) {
            BaseUserAdapter adapter = (BaseUserAdapter) this.mAdapters.get(i).get();
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            } else {
                this.mAdapters.remove(i);
            }
        }
    }

    public boolean isSimpleUserSwitcher() {
        return this.mSimpleUserSwitcher;
    }

    public boolean useFullscreenUserSwitcher() {
        int overrideUseFullscreenUserSwitcher = Settings.System.getInt(this.mContext.getContentResolver(), "enable_fullscreen_user_switcher", -1);
        if (overrideUseFullscreenUserSwitcher == -1) {
            return this.mContext.getResources().getBoolean(R.bool.config_enableFullscreenUserSwitcher);
        }
        return overrideUseFullscreenUserSwitcher != 0;
    }

    public void logoutCurrentUser() {
        if (ActivityManager.getCurrentUser() != 0) {
            pauseRefreshUsers();
            ActivityManagerCompat.logoutCurrentUser();
        }
    }

    public void switchTo(UserRecord record) {
        int id;
        if (record.isGuest && record.info == null) {
            UserInfo guest = this.mUserManager.createGuest(this.mContext, this.mContext.getString(R.string.guest_nickname));
            if (guest != null) {
                id = guest.id;
            } else {
                return;
            }
        } else if (record.isAddUser != 0) {
            showAddUserDialog();
            return;
        } else {
            id = record.info.id;
        }
        if (ActivityManager.getCurrentUser() == id) {
            if (record.isGuest) {
                showExitGuestDialog(id);
            }
            return;
        }
        switchToUserId(id);
    }

    /* access modifiers changed from: protected */
    public void switchToUserId(int id) {
        try {
            pauseRefreshUsers();
            ActivityManagerCompat.getService().switchUser(id);
        } catch (RemoteException e) {
            Log.e("UserSwitcherController", "Couldn't switch user.", e);
        }
    }

    /* access modifiers changed from: private */
    public void showExitGuestDialog(int id) {
        int newId = 0;
        if (this.mResumeUserOnGuestLogout && this.mLastNonGuestUser != 0) {
            UserInfo info = this.mUserManager.getUserInfo(this.mLastNonGuestUser);
            if (info != null && info.isEnabled() && UserInfoCompat.supportsSwitchToByUser(info)) {
                newId = info.id;
            }
        }
        showExitGuestDialog(id, newId);
    }

    /* JADX WARNING: type inference failed for: r0v1, types: [com.android.systemui.statusbar.policy.UserSwitcherController$ExitGuestDialog, android.app.Dialog] */
    /* access modifiers changed from: protected */
    public void showExitGuestDialog(int id, int targetId) {
        if (this.mExitGuestDialog != null && this.mExitGuestDialog.isShowing()) {
            this.mExitGuestDialog.cancel();
        }
        this.mExitGuestDialog = new ExitGuestDialog(this.mContext, id, targetId);
        this.mExitGuestDialog.show();
    }

    /* JADX WARNING: type inference failed for: r0v1, types: [com.android.systemui.statusbar.policy.UserSwitcherController$AddUserDialog, android.app.Dialog] */
    public void showAddUserDialog() {
        if (this.mAddUserDialog != null && this.mAddUserDialog.isShowing()) {
            this.mAddUserDialog.cancel();
        }
        this.mAddUserDialog = new AddUserDialog(this.mContext);
        this.mAddUserDialog.show();
    }

    /* access modifiers changed from: protected */
    public void exitGuest(int id, int targetId) {
        switchToUserId(targetId);
        this.mUserManager.removeUser(id);
    }

    private void listenForCallState() {
        TelephonyManager.from(this.mContext).listen(this.mPhoneStateListener, 32);
    }

    /* access modifiers changed from: private */
    public void showGuestNotification(int guestUserId) {
        PendingIntent removeGuestPI;
        if (UserManagerCompat.canSwitchUsers(this.mUserManager)) {
            removeGuestPI = PendingIntent.getBroadcastAsUser(this.mContext, 0, new Intent("com.android.systemui.REMOVE_GUEST"), 0, UserHandleCompat.SYSTEM);
        } else {
            removeGuestPI = null;
        }
        Notification.Builder builder = NotificationCompat.newBuilder(this.mContext, NotificationChannels.GENERAL).setVisibility(-1).setSmallIcon(R.drawable.ic_person).setContentTitle(this.mContext.getString(R.string.guest_notification_title)).setContentText(this.mContext.getString(R.string.guest_notification_text)).setContentIntent(removeGuestPI).setShowWhen(false).addAction(R.drawable.ic_delete, this.mContext.getString(R.string.guest_notification_remove_action), removeGuestPI);
        SystemUI.overrideNotificationAppName(this.mContext, builder);
        NotificationManager.from(this.mContext).notifyAsUser("remove_guest", 1010, builder.build(), new UserHandle(guestUserId));
    }

    public String getCurrentUserName(Context context) {
        if (this.mUsers.isEmpty()) {
            return null;
        }
        UserRecord item = this.mUsers.get(0);
        if (item == null || item.info == null) {
            return null;
        }
        if (item.isGuest) {
            return context.getString(R.string.guest_nickname);
        }
        return item.info.name;
    }

    public void onDensityOrFontScaleChanged() {
        refreshUsers(-1);
    }

    @VisibleForTesting
    public void addAdapter(WeakReference<BaseUserAdapter> adapter) {
        this.mAdapters.add(adapter);
    }

    @VisibleForTesting
    public ArrayList<UserRecord> getUsers() {
        return this.mUsers;
    }

    /* access modifiers changed from: private */
    public void checkIfAddUserDisallowedByAdminOnly(UserRecord record) {
        RestrictedLockUtils.EnforcedAdmin admin = RestrictedLockUtils.checkIfRestrictionEnforced(this.mContext, "no_add_user", ActivityManager.getCurrentUser());
        if (admin == null || RestrictedLockUtils.hasBaseUserRestriction(this.mContext, "no_add_user", ActivityManager.getCurrentUser())) {
            record.isDisabledByAdmin = false;
            record.enforcedAdmin = null;
            return;
        }
        record.isDisabledByAdmin = true;
        record.enforcedAdmin = admin;
    }

    public void startActivity(Intent intent) {
        this.mActivityStarter.startActivity(intent, true);
    }
}
