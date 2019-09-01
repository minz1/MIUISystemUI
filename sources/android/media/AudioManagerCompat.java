package android.media;

public class AudioManagerCompat {
    public static int getStreamMinVolume(AudioManager audioManager, int stream) {
        return audioManager.getStreamMinVolumeInt(stream);
    }
}
