package android.media;

public class MediaPlayerCompat {
    public static void seekTo(MediaPlayer mediaPlayer, int msec, int mode) {
        mediaPlayer.seekTo((long) msec, mode);
    }
}
