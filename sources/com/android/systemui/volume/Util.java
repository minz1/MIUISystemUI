package com.android.systemui.volume;

import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.view.View;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;

class Util {
    private static int[] AUDIO_MANAGER_FLAGS = {1, 16, 4, 2, 8, 2048, 128, 4096, 1024};
    private static String[] AUDIO_MANAGER_FLAG_NAMES = {"SHOW_UI", "VIBRATE", "PLAY_SOUND", "ALLOW_RINGER_MODES", "REMOVE_SOUND_AND_VIBRATE", "SHOW_VIBRATE_HINT", "SHOW_SILENT_HINT", "FROM_KEY", "SHOW_UI_WARNINGS"};
    private static final SimpleDateFormat HMMAA = new SimpleDateFormat("h:mm aa", Locale.US);

    public static String logTag(Class<?> c) {
        String tag = "vol." + c.getSimpleName();
        return tag.length() < 23 ? tag : tag.substring(0, 23);
    }

    public static String ringerModeToString(int ringerMode) {
        switch (ringerMode) {
            case 0:
                return "RINGER_MODE_SILENT";
            case 1:
                return "RINGER_MODE_VIBRATE";
            case 2:
                return "RINGER_MODE_NORMAL";
            default:
                return "RINGER_MODE_UNKNOWN_" + ringerMode;
        }
    }

    public static String mediaMetadataToString(MediaMetadata metadata) {
        return metadata.getDescription().toString();
    }

    public static String playbackInfoToString(MediaController.PlaybackInfo info) {
        if (info == null) {
            return null;
        }
        return String.format("PlaybackInfo[vol=%s,max=%s,type=%s,vc=%s],atts=%s", new Object[]{Integer.valueOf(info.getCurrentVolume()), Integer.valueOf(info.getMaxVolume()), playbackInfoTypeToString(info.getPlaybackType()), volumeProviderControlToString(info.getVolumeControl()), info.getAudioAttributes()});
    }

    public static String playbackInfoTypeToString(int type) {
        switch (type) {
            case 1:
                return "LOCAL";
            case 2:
                return "REMOTE";
            default:
                return "UNKNOWN_" + type;
        }
    }

    public static String playbackStateStateToString(int state) {
        switch (state) {
            case 0:
                return "STATE_NONE";
            case 1:
                return "STATE_STOPPED";
            case 2:
                return "STATE_PAUSED";
            case 3:
                return "STATE_PLAYING";
            default:
                return "UNKNOWN_" + state;
        }
    }

    public static String volumeProviderControlToString(int control) {
        switch (control) {
            case 0:
                return "VOLUME_CONTROL_FIXED";
            case 1:
                return "VOLUME_CONTROL_RELATIVE";
            case 2:
                return "VOLUME_CONTROL_ABSOLUTE";
            default:
                return "VOLUME_CONTROL_UNKNOWN_" + control;
        }
    }

    public static String playbackStateToString(PlaybackState playbackState) {
        if (playbackState == null) {
            return null;
        }
        return playbackStateStateToString(playbackState.getState()) + " " + playbackState;
    }

    public static String audioManagerFlagsToString(int value) {
        return bitFieldToString(value, AUDIO_MANAGER_FLAGS, AUDIO_MANAGER_FLAG_NAMES);
    }

    private static String bitFieldToString(int value, int[] values, String[] names) {
        if (value == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if ((values[i] & value) != 0) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(names[i]);
            }
            value &= ~values[i];
        }
        if (value != 0) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append("UNKNOWN_");
            sb.append(value);
        }
        return sb.toString();
    }

    private static CharSequence emptyToNull(CharSequence str) {
        if (str == null || str.length() == 0) {
            return null;
        }
        return str;
    }

    public static boolean setText(TextView tv, CharSequence text) {
        if (Objects.equals(emptyToNull(tv.getText()), emptyToNull(text))) {
            return false;
        }
        tv.setText(text);
        return true;
    }

    public static final void setVisOrGone(View v, boolean vis) {
        if (v != null) {
            int i = 0;
            if ((v.getVisibility() == 0) != vis) {
                if (!vis) {
                    i = 8;
                }
                v.setVisibility(i);
            }
        }
    }
}
