package android.content.pm;

public class ShortcutManagerCompat {
    public static void onApplicationActive(ShortcutManager shortcutManager, String packageName, int userId) {
        shortcutManager.onApplicationActive(packageName, userId);
    }
}
