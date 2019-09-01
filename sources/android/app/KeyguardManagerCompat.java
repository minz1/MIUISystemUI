package android.app;

import android.content.Intent;

public class KeyguardManagerCompat {
    public static Intent createConfirmDeviceCredentialIntent(KeyguardManager km, CharSequence title, CharSequence description, int userId) {
        return km.createConfirmDeviceCredentialIntent(title, description, userId);
    }
}
