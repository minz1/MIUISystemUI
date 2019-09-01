package android.content.pm;

public class UserInfoCompat {
    public static boolean isQuietModeEnabled(UserInfo ui) {
        return ui.isQuietModeEnabled();
    }

    public static boolean supportsSwitchToByUser(UserInfo ui) {
        return ui.supportsSwitchToByUser();
    }
}
