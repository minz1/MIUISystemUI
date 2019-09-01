package com.android.systemui.miui.volume;

import android.content.Context;
import android.net.Uri;
import android.provider.MiuiSettings;
import com.android.systemui.Util;
import java.io.File;

public class VolumeUtil {
    private static final File RELIEVE_SOUND = new File("/system/media/audio/ui/relieve.ogg");

    public static void setSilenceMode(Context context, int mode, Uri id) {
        MiuiSettings.SilenceMode.setSilenceMode(context, mode, id);
        if (context.getResources().getBoolean(R.bool.miui_config_enableRingerRelieveSound) && mode == 0 && RELIEVE_SOUND.exists()) {
            Util.playRingtoneAsync(context, Uri.fromFile(RELIEVE_SOUND), 5);
        }
    }
}
