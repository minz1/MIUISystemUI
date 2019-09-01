package android.support.v4.media;

import android.media.AudioAttributes;
import android.os.Build;
import android.support.v4.media.AudioAttributesCompatApi21;
import android.util.SparseIntArray;
import java.util.Arrays;

public class AudioAttributesCompat {
    private static final int[] SDK_USAGES = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 16};
    private static final SparseIntArray SUPPRESSIBLE_USAGES = new SparseIntArray();
    private static boolean sForceLegacyBehavior;
    private AudioAttributesCompatApi21.Wrapper mAudioAttributesWrapper;
    int mContentType = 0;
    int mFlags = 0;
    Integer mLegacyStream;
    int mUsage = 0;

    static {
        SUPPRESSIBLE_USAGES.put(5, 1);
        SUPPRESSIBLE_USAGES.put(6, 2);
        SUPPRESSIBLE_USAGES.put(7, 2);
        SUPPRESSIBLE_USAGES.put(8, 1);
        SUPPRESSIBLE_USAGES.put(9, 1);
        SUPPRESSIBLE_USAGES.put(10, 1);
    }

    private AudioAttributesCompat() {
    }

    public Object unwrap() {
        if (this.mAudioAttributesWrapper != null) {
            return this.mAudioAttributesWrapper.unwrap();
        }
        return null;
    }

    public int getLegacyStreamType() {
        if (this.mLegacyStream != null) {
            return this.mLegacyStream.intValue();
        }
        if (Build.VERSION.SDK_INT < 21 || sForceLegacyBehavior) {
            return toVolumeStreamType(false, this.mFlags, this.mUsage);
        }
        return AudioAttributesCompatApi21.toLegacyStreamType(this.mAudioAttributesWrapper);
    }

    public static AudioAttributesCompat wrap(Object aa) {
        if (Build.VERSION.SDK_INT < 21 || sForceLegacyBehavior) {
            return null;
        }
        AudioAttributesCompat aac = new AudioAttributesCompat();
        aac.mAudioAttributesWrapper = AudioAttributesCompatApi21.Wrapper.wrap((AudioAttributes) aa);
        return aac;
    }

    public int getContentType() {
        if (Build.VERSION.SDK_INT < 21 || sForceLegacyBehavior || this.mAudioAttributesWrapper == null) {
            return this.mContentType;
        }
        return this.mAudioAttributesWrapper.unwrap().getContentType();
    }

    public int getUsage() {
        if (Build.VERSION.SDK_INT < 21 || sForceLegacyBehavior || this.mAudioAttributesWrapper == null) {
            return this.mUsage;
        }
        return this.mAudioAttributesWrapper.unwrap().getUsage();
    }

    public int getFlags() {
        if (Build.VERSION.SDK_INT >= 21 && !sForceLegacyBehavior && this.mAudioAttributesWrapper != null) {
            return this.mAudioAttributesWrapper.unwrap().getFlags();
        }
        int flags = this.mFlags;
        int legacyStream = getLegacyStreamType();
        if (legacyStream == 6) {
            flags |= 4;
        } else if (legacyStream == 7) {
            flags |= 1;
        }
        return flags & 273;
    }

    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r0v1, resolved type: java.lang.Integer} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r0v5, resolved type: android.support.v4.media.AudioAttributesCompat} */
    /* JADX WARNING: type inference failed for: r0v0 */
    /* JADX WARNING: type inference failed for: r0v7 */
    /* JADX WARNING: type inference failed for: r0v8 */
    /* JADX WARNING: Multi-variable type inference failed */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static android.support.v4.media.AudioAttributesCompat fromBundle(android.os.Bundle r6) {
        /*
            r0 = 0
            if (r6 != 0) goto L_0x0004
            return r0
        L_0x0004:
            int r1 = android.os.Build.VERSION.SDK_INT
            r2 = 21
            if (r1 < r2) goto L_0x001a
            java.lang.String r1 = "android.support.v4.media.audio_attrs.FRAMEWORKS"
            android.os.Parcelable r1 = r6.getParcelable(r1)
            android.media.AudioAttributes r1 = (android.media.AudioAttributes) r1
            if (r1 != 0) goto L_0x0015
            goto L_0x0019
        L_0x0015:
            android.support.v4.media.AudioAttributesCompat r0 = wrap(r1)
        L_0x0019:
            return r0
        L_0x001a:
            java.lang.String r1 = "android.support.v4.media.audio_attrs.USAGE"
            r2 = 0
            int r1 = r6.getInt(r1, r2)
            java.lang.String r3 = "android.support.v4.media.audio_attrs.CONTENT_TYPE"
            int r3 = r6.getInt(r3, r2)
            java.lang.String r4 = "android.support.v4.media.audio_attrs.FLAGS"
            int r2 = r6.getInt(r4, r2)
            android.support.v4.media.AudioAttributesCompat r4 = new android.support.v4.media.AudioAttributesCompat
            r4.<init>()
            r4.mUsage = r1
            r4.mContentType = r3
            r4.mFlags = r2
            java.lang.String r5 = "android.support.v4.media.audio_attrs.LEGACY_STREAM_TYPE"
            boolean r5 = r6.containsKey(r5)
            if (r5 == 0) goto L_0x004b
            java.lang.String r0 = "android.support.v4.media.audio_attrs.LEGACY_STREAM_TYPE"
            int r0 = r6.getInt(r0)
            java.lang.Integer r0 = java.lang.Integer.valueOf(r0)
        L_0x004b:
            r4.mLegacyStream = r0
            return r4
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.media.AudioAttributesCompat.fromBundle(android.os.Bundle):android.support.v4.media.AudioAttributesCompat");
    }

    public int hashCode() {
        if (Build.VERSION.SDK_INT >= 21 && !sForceLegacyBehavior && this.mAudioAttributesWrapper != null) {
            return this.mAudioAttributesWrapper.unwrap().hashCode();
        }
        return Arrays.hashCode(new Object[]{Integer.valueOf(this.mContentType), Integer.valueOf(this.mFlags), Integer.valueOf(this.mUsage), this.mLegacyStream});
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("AudioAttributesCompat:");
        if (unwrap() != null) {
            sb.append(" audioattributes=");
            sb.append(unwrap());
        } else {
            if (this.mLegacyStream != null) {
                sb.append(" stream=");
                sb.append(this.mLegacyStream);
                sb.append(" derived");
            }
            sb.append(" usage=");
            sb.append(usageToString());
            sb.append(" content=");
            sb.append(this.mContentType);
            sb.append(" flags=0x");
            sb.append(Integer.toHexString(this.mFlags).toUpperCase());
        }
        return sb.toString();
    }

    /* access modifiers changed from: package-private */
    public String usageToString() {
        return usageToString(this.mUsage);
    }

    static String usageToString(int usage) {
        switch (usage) {
            case 0:
                return new String("USAGE_UNKNOWN");
            case 1:
                return new String("USAGE_MEDIA");
            case 2:
                return new String("USAGE_VOICE_COMMUNICATION");
            case 3:
                return new String("USAGE_VOICE_COMMUNICATION_SIGNALLING");
            case 4:
                return new String("USAGE_ALARM");
            case 5:
                return new String("USAGE_NOTIFICATION");
            case 6:
                return new String("USAGE_NOTIFICATION_RINGTONE");
            case 7:
                return new String("USAGE_NOTIFICATION_COMMUNICATION_REQUEST");
            case 8:
                return new String("USAGE_NOTIFICATION_COMMUNICATION_INSTANT");
            case 9:
                return new String("USAGE_NOTIFICATION_COMMUNICATION_DELAYED");
            case 10:
                return new String("USAGE_NOTIFICATION_EVENT");
            case 11:
                return new String("USAGE_ASSISTANCE_ACCESSIBILITY");
            case 12:
                return new String("USAGE_ASSISTANCE_NAVIGATION_GUIDANCE");
            case 13:
                return new String("USAGE_ASSISTANCE_SONIFICATION");
            case 14:
                return new String("USAGE_GAME");
            case 16:
                return new String("USAGE_ASSISTANT");
            default:
                return new String("unknown usage " + usage);
        }
    }

    static int toVolumeStreamType(boolean fromGetVolumeControlStream, int flags, int usage) {
        int i = 1;
        if ((flags & 1) == 1) {
            if (!fromGetVolumeControlStream) {
                i = 7;
            }
            return i;
        }
        int i2 = 0;
        if ((flags & 4) == 4) {
            if (!fromGetVolumeControlStream) {
                i2 = 6;
            }
            return i2;
        }
        int i3 = 3;
        switch (usage) {
            case 0:
                if (fromGetVolumeControlStream) {
                    i3 = Integer.MIN_VALUE;
                }
                return i3;
            case 1:
            case 12:
            case 14:
            case 16:
                return 3;
            case 2:
                return 0;
            case 3:
                if (!fromGetVolumeControlStream) {
                    i2 = 8;
                }
                return i2;
            case 4:
                return 4;
            case 5:
            case 7:
            case 8:
            case 9:
            case 10:
                return 5;
            case 6:
                return 2;
            case 11:
                return 10;
            case 13:
                return 1;
            default:
                if (!fromGetVolumeControlStream) {
                    return 3;
                }
                throw new IllegalArgumentException("Unknown usage value " + usage + " in audio attributes");
        }
    }

    public boolean equals(Object o) {
        boolean z = true;
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AudioAttributesCompat that = (AudioAttributesCompat) o;
        if (Build.VERSION.SDK_INT >= 21 && !sForceLegacyBehavior && this.mAudioAttributesWrapper != null) {
            return this.mAudioAttributesWrapper.unwrap().equals(that.unwrap());
        }
        if (!(this.mContentType == that.getContentType() && this.mFlags == that.getFlags() && this.mUsage == that.getUsage() && (this.mLegacyStream == null ? that.mLegacyStream == null : this.mLegacyStream.equals(that.mLegacyStream)))) {
            z = false;
        }
        return z;
    }
}
