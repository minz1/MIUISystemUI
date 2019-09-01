package com.android.keyguard.doze;

import android.content.Context;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.util.KeyValueListParser;
import android.util.Log;
import com.android.systemui.R;
import java.util.Arrays;

public class AlwaysOnDisplayPolicy {
    public int[] dimmingScrimArray;
    /* access modifiers changed from: private */
    public final Context mContext;
    /* access modifiers changed from: private */
    public final KeyValueListParser mParser = new KeyValueListParser(',');
    private SettingsObserver mSettingsObserver;
    public long proxCooldownPeriodMs;
    public long proxCooldownTriggerMs;
    public long proxScreenOffDelayMs;
    public int[] screenBrightnessArray;

    private final class SettingsObserver extends ContentObserver {
        private final Uri ALWAYS_ON_DISPLAY_CONSTANTS_URI = Settings.Global.getUriFor("always_on_display_constants");

        SettingsObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean selfChange, Uri uri) {
            update(uri);
        }

        public void update(Uri uri) {
            if (uri == null || this.ALWAYS_ON_DISPLAY_CONSTANTS_URI.equals(uri)) {
                Resources resources = AlwaysOnDisplayPolicy.this.mContext.getResources();
                try {
                    AlwaysOnDisplayPolicy.this.mParser.setString(Settings.Global.getString(AlwaysOnDisplayPolicy.this.mContext.getContentResolver(), "always_on_display_constants"));
                } catch (IllegalArgumentException e) {
                    Log.e("AlwaysOnDisplayPolicy", "Bad AOD constants");
                }
                AlwaysOnDisplayPolicy.this.proxScreenOffDelayMs = AlwaysOnDisplayPolicy.this.mParser.getLong("prox_screen_off_delay", 10000);
                AlwaysOnDisplayPolicy.this.proxCooldownTriggerMs = AlwaysOnDisplayPolicy.this.mParser.getLong("prox_cooldown_trigger", 2000);
                AlwaysOnDisplayPolicy.this.proxCooldownPeriodMs = AlwaysOnDisplayPolicy.this.mParser.getLong("prox_cooldown_period", 5000);
                AlwaysOnDisplayPolicy.this.screenBrightnessArray = AlwaysOnDisplayPolicy.this.parseIntArray("screen_brightness_array", resources.getIntArray(R.array.config_doze_brightness_sensor_to_brightness));
                AlwaysOnDisplayPolicy.this.dimmingScrimArray = AlwaysOnDisplayPolicy.this.parseIntArray("dimming_scrim_array", resources.getIntArray(R.array.config_doze_brightness_sensor_to_scrim_opacity));
            }
        }
    }

    public AlwaysOnDisplayPolicy(Context context) {
        this.mContext = context;
        this.mSettingsObserver = new SettingsObserver(context.getMainThreadHandler());
    }

    /* access modifiers changed from: private */
    public int[] parseIntArray(String key, int[] defaultArray) {
        String value = this.mParser.getString(key, null);
        if (value == null) {
            return defaultArray;
        }
        try {
            return Arrays.stream(value.split(":")).map($$Lambda$AlwaysOnDisplayPolicy$MGZTkxm_LWhWFo0u65o5bz97bA.INSTANCE).mapToInt($$Lambda$AlwaysOnDisplayPolicy$wddj3hVVrg0MkscpMtYt3BzY8Y.INSTANCE).toArray();
        } catch (NumberFormatException e) {
            return defaultArray;
        }
    }
}
