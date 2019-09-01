package android.os;

public class UserManagerCompat {
    public static String getUserAccount(UserManager um, int userHandle) {
        return um.getUserAccount(userHandle);
    }

    public static int[] getProfileIdsWithDisabled(UserManager um, int userId) {
        return um.getProfileIdsWithDisabled(userId);
    }

    public static int[] getEnabledProfileIds(UserManager um, int userId) {
        return um.getEnabledProfileIds(userId);
    }

    public static boolean isUserUnlocked(UserManager um) {
        return um.isUserUnlocked();
    }

    public static boolean isUserUnlocked(UserManager um, int userId) {
        return um.isUserUnlocked(userId);
    }

    public static boolean isSplitSystemUser() {
        return SystemProperties.getBoolean("ro.fw.system_user_split", false);
    }

    public static boolean isManagedProfile(UserManager um, int userId) {
        return um.isManagedProfile(userId);
    }

    public static boolean canSwitchUsers(UserManager um) {
        return um.canSwitchUsers();
    }

    public static boolean hasBaseUserRestriction(UserManager um, String restrictionKey, UserHandle userHandle) {
        return um.hasBaseUserRestriction(restrictionKey, userHandle);
    }
}
