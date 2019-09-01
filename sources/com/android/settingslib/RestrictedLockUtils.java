package com.android.settingslib;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;
import java.util.List;
import java.util.Objects;

public class RestrictedLockUtils {
    static Proxy sProxy = new Proxy();

    public static class EnforcedAdmin {
        public static final EnforcedAdmin MULTIPLE_ENFORCED_ADMIN = new EnforcedAdmin();
        public ComponentName component = null;
        public String enforcedRestriction = null;
        public int userId = -10000;

        public static EnforcedAdmin createDefaultEnforcedAdminWithRestriction(String enforcedRestriction2) {
            EnforcedAdmin enforcedAdmin = new EnforcedAdmin();
            enforcedAdmin.enforcedRestriction = enforcedRestriction2;
            return enforcedAdmin;
        }

        public EnforcedAdmin(ComponentName component2, String enforcedRestriction2, int userId2) {
            this.component = component2;
            this.enforcedRestriction = enforcedRestriction2;
            this.userId = userId2;
        }

        public EnforcedAdmin() {
        }

        public boolean equals(Object o) {
            boolean z = true;
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            EnforcedAdmin that = (EnforcedAdmin) o;
            if (this.userId != that.userId || !Objects.equals(this.component, that.component) || !Objects.equals(this.enforcedRestriction, that.enforcedRestriction)) {
                z = false;
            }
            return z;
        }

        public int hashCode() {
            return Objects.hash(new Object[]{this.component, this.enforcedRestriction, Integer.valueOf(this.userId)});
        }

        public String toString() {
            return "EnforcedAdmin{component=" + this.component + ", enforcedRestriction='" + this.enforcedRestriction + ", userId=" + this.userId + '}';
        }
    }

    static class Proxy {
        Proxy() {
        }
    }

    public static EnforcedAdmin checkIfRestrictionEnforced(Context context, String userRestriction, int userId) {
        EnforcedAdmin enforcedAdmin;
        EnforcedAdmin enforcedAdmin2;
        if (((DevicePolicyManager) context.getSystemService("device_policy")) == null) {
            return null;
        }
        UserManager um = UserManager.get(context);
        List<UserManager.EnforcingUser> enforcingUsers = um.getUserRestrictionSources(userRestriction, UserHandle.of(userId));
        if (enforcingUsers.isEmpty()) {
            return null;
        }
        if (enforcingUsers.size() > 1) {
            return EnforcedAdmin.createDefaultEnforcedAdminWithRestriction(userRestriction);
        }
        int restrictionSource = enforcingUsers.get(0).getUserRestrictionSource();
        int adminUserId = enforcingUsers.get(0).getUserHandle().getIdentifier();
        if (restrictionSource == 4) {
            if (adminUserId == userId) {
                return getProfileOwner(context, userRestriction, adminUserId);
            }
            UserInfo parentUser = um.getProfileParent(adminUserId);
            if (parentUser == null || parentUser.id != userId) {
                enforcedAdmin2 = EnforcedAdmin.createDefaultEnforcedAdminWithRestriction(userRestriction);
            } else {
                enforcedAdmin2 = getProfileOwner(context, userRestriction, adminUserId);
            }
            return enforcedAdmin2;
        } else if (restrictionSource != 2) {
            return null;
        } else {
            if (adminUserId == userId) {
                enforcedAdmin = getDeviceOwner(context, userRestriction);
            } else {
                enforcedAdmin = EnforcedAdmin.createDefaultEnforcedAdminWithRestriction(userRestriction);
            }
            return enforcedAdmin;
        }
    }

    public static boolean hasBaseUserRestriction(Context context, String userRestriction, int userId) {
        return ((UserManager) context.getSystemService("user")).hasBaseUserRestriction(userRestriction, UserHandle.of(userId));
    }

    private static EnforcedAdmin getDeviceOwner(Context context, String enforcedRestriction) {
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService("device_policy");
        if (dpm == null) {
            return null;
        }
        ComponentName adminComponent = dpm.getDeviceOwnerComponentOnAnyUser();
        if (adminComponent != null) {
            return new EnforcedAdmin(adminComponent, enforcedRestriction, dpm.getDeviceOwnerUserId());
        }
        return null;
    }

    private static EnforcedAdmin getProfileOwner(Context context, String enforcedRestriction, int userId) {
        if (userId == -10000) {
            return null;
        }
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService("device_policy");
        if (dpm == null) {
            return null;
        }
        ComponentName adminComponent = dpm.getProfileOwnerAsUser(userId);
        if (adminComponent != null) {
            return new EnforcedAdmin(adminComponent, enforcedRestriction, userId);
        }
        return null;
    }

    public static void sendShowAdminSupportDetailsIntent(Context context, EnforcedAdmin admin) {
        Intent intent = getShowAdminSupportDetailsIntent(context, admin);
        int targetUserId = UserHandle.myUserId();
        if (!(admin == null || admin.userId == -10000 || !isCurrentUserOrProfile(context, admin.userId))) {
            targetUserId = admin.userId;
        }
        intent.putExtra("android.app.extra.RESTRICTION", admin.enforcedRestriction);
        context.startActivityAsUser(intent, new UserHandle(targetUserId));
    }

    public static Intent getShowAdminSupportDetailsIntent(Context context, EnforcedAdmin admin) {
        Intent intent = new Intent("android.settings.SHOW_ADMIN_SUPPORT_DETAILS");
        if (admin != null) {
            if (admin.component != null) {
                intent.putExtra("android.app.extra.DEVICE_ADMIN", admin.component);
            }
            int adminUserId = UserHandle.myUserId();
            if (admin.userId != -10000) {
                adminUserId = admin.userId;
            }
            intent.putExtra("android.intent.extra.USER_ID", adminUserId);
        }
        return intent;
    }

    public static boolean isCurrentUserOrProfile(Context context, int userId) {
        for (UserInfo userInfo : UserManager.get(context).getProfiles(UserHandle.myUserId())) {
            if (userInfo.id == userId) {
                return true;
            }
        }
        return false;
    }
}
