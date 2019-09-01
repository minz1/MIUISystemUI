package android.support.v4.media;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.os.ResultReceiver;
import android.support.v4.app.BundleCompat;
import android.support.v4.media.MediaController2;
import android.support.v4.media.MediaSession2;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import java.util.List;
import java.util.concurrent.Executor;

@TargetApi(16)
class MediaController2ImplLegacy implements MediaController2.SupportLibraryImpl {
    private static final boolean DEBUG = Log.isLoggable("MC2ImplLegacy", 3);
    static final Bundle sDefaultRootExtras = new Bundle();
    /* access modifiers changed from: private */
    public SessionCommandGroup2 mAllowedCommands;
    private MediaBrowserCompat mBrowserCompat;
    /* access modifiers changed from: private */
    public int mBufferingState;
    /* access modifiers changed from: private */
    public final MediaController2.ControllerCallback mCallback;
    /* access modifiers changed from: private */
    public final Executor mCallbackExecutor;
    private volatile boolean mConnected;
    private final Context mContext;
    private MediaControllerCompat mControllerCompat;
    private ControllerCompatCallback mControllerCompatCallback;
    /* access modifiers changed from: private */
    public MediaItem2 mCurrentMediaItem;
    /* access modifiers changed from: private */
    public final Handler mHandler;
    /* access modifiers changed from: private */
    public final HandlerThread mHandlerThread;
    /* access modifiers changed from: private */
    public MediaController2 mInstance;
    private boolean mIsReleased;
    final Object mLock;
    /* access modifiers changed from: private */
    public MediaMetadataCompat mMediaMetadataCompat;
    /* access modifiers changed from: private */
    public MediaController2.PlaybackInfo mPlaybackInfo;
    /* access modifiers changed from: private */
    public PlaybackStateCompat mPlaybackStateCompat;
    /* access modifiers changed from: private */
    public int mPlayerState;
    /* access modifiers changed from: private */
    public List<MediaItem2> mPlaylist;
    /* access modifiers changed from: private */
    public MediaMetadata2 mPlaylistMetadata;
    /* access modifiers changed from: private */
    public int mRepeatMode;
    /* access modifiers changed from: private */
    public int mShuffleMode;
    private final SessionToken2 mToken;

    /* renamed from: android.support.v4.media.MediaController2ImplLegacy$3  reason: invalid class name */
    class AnonymousClass3 extends ResultReceiver {
        final /* synthetic */ MediaController2ImplLegacy this$0;

        /* access modifiers changed from: protected */
        public void onReceiveResult(int resultCode, Bundle resultData) {
            if (this.this$0.mHandlerThread.isAlive()) {
                switch (resultCode) {
                    case -1:
                        this.this$0.mCallbackExecutor.execute(new Runnable() {
                            public void run() {
                                AnonymousClass3.this.this$0.mCallback.onDisconnected(AnonymousClass3.this.this$0.mInstance);
                            }
                        });
                        this.this$0.close();
                        break;
                    case 0:
                        this.this$0.onConnectedNotLocked(resultData);
                        break;
                }
            }
        }
    }

    private final class ControllerCompatCallback extends MediaControllerCompat.Callback {
        final /* synthetic */ MediaController2ImplLegacy this$0;

        public void onSessionReady() {
            this.this$0.sendCommand("android.support.v4.media.controller.command.CONNECT", new ResultReceiver(this.this$0.mHandler) {
                /* access modifiers changed from: protected */
                public void onReceiveResult(int resultCode, Bundle resultData) {
                    if (ControllerCompatCallback.this.this$0.mHandlerThread.isAlive()) {
                        switch (resultCode) {
                            case -1:
                                ControllerCompatCallback.this.this$0.mCallbackExecutor.execute(new Runnable() {
                                    public void run() {
                                        ControllerCompatCallback.this.this$0.mCallback.onDisconnected(ControllerCompatCallback.this.this$0.mInstance);
                                    }
                                });
                                ControllerCompatCallback.this.this$0.close();
                                break;
                            case 0:
                                ControllerCompatCallback.this.this$0.onConnectedNotLocked(resultData);
                                break;
                        }
                    }
                }
            });
        }

        public void onSessionDestroyed() {
            this.this$0.close();
        }

        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            synchronized (this.this$0.mLock) {
                PlaybackStateCompat unused = this.this$0.mPlaybackStateCompat = state;
            }
        }

        public void onMetadataChanged(MediaMetadataCompat metadata) {
            synchronized (this.this$0.mLock) {
                MediaMetadataCompat unused = this.this$0.mMediaMetadataCompat = metadata;
            }
        }

        public void onSessionEvent(String event, Bundle extras) {
            if (extras != null) {
                extras.setClassLoader(MediaSession2.class.getClassLoader());
            }
            char c = 65535;
            switch (event.hashCode()) {
                case -2076894204:
                    if (event.equals("android.support.v4.media.session.event.ON_BUFFERING_STATE_CHANGED")) {
                        c = 13;
                        break;
                    }
                    break;
                case -2060536131:
                    if (event.equals("android.support.v4.media.session.event.ON_PLAYBACK_SPEED_CHANGED")) {
                        c = 12;
                        break;
                    }
                    break;
                case -1588811870:
                    if (event.equals("android.support.v4.media.session.event.ON_PLAYBACK_INFO_CHANGED")) {
                        c = 11;
                        break;
                    }
                    break;
                case -1471144819:
                    if (event.equals("android.support.v4.media.session.event.ON_PLAYER_STATE_CHANGED")) {
                        c = 1;
                        break;
                    }
                    break;
                case -1021916189:
                    if (event.equals("android.support.v4.media.session.event.ON_ERROR")) {
                        c = 3;
                        break;
                    }
                    break;
                case -617184370:
                    if (event.equals("android.support.v4.media.session.event.ON_CURRENT_MEDIA_ITEM_CHANGED")) {
                        c = 2;
                        break;
                    }
                    break;
                case -92092013:
                    if (event.equals("android.support.v4.media.session.event.ON_ROUTES_INFO_CHANGED")) {
                        c = 4;
                        break;
                    }
                    break;
                case -53555497:
                    if (event.equals("android.support.v4.media.session.event.ON_REPEAT_MODE_CHANGED")) {
                        c = 7;
                        break;
                    }
                    break;
                case 229988025:
                    if (event.equals("android.support.v4.media.session.event.SEND_CUSTOM_COMMAND")) {
                        c = 9;
                        break;
                    }
                    break;
                case 306321100:
                    if (event.equals("android.support.v4.media.session.event.ON_PLAYLIST_METADATA_CHANGED")) {
                        c = 6;
                        break;
                    }
                    break;
                case 408969344:
                    if (event.equals("android.support.v4.media.session.event.SET_CUSTOM_LAYOUT")) {
                        c = 10;
                        break;
                    }
                    break;
                case 806201420:
                    if (event.equals("android.support.v4.media.session.event.ON_PLAYLIST_CHANGED")) {
                        c = 5;
                        break;
                    }
                    break;
                case 896576579:
                    if (event.equals("android.support.v4.media.session.event.ON_SHUFFLE_MODE_CHANGED")) {
                        c = 8;
                        break;
                    }
                    break;
                case 1696119769:
                    if (event.equals("android.support.v4.media.session.event.ON_ALLOWED_COMMANDS_CHANGED")) {
                        c = 0;
                        break;
                    }
                    break;
                case 1871849865:
                    if (event.equals("android.support.v4.media.session.event.ON_SEEK_COMPLETED")) {
                        c = 14;
                        break;
                    }
                    break;
            }
            switch (c) {
                case 0:
                    final SessionCommandGroup2 allowedCommands = SessionCommandGroup2.fromBundle(extras.getBundle("android.support.v4.media.argument.ALLOWED_COMMANDS"));
                    synchronized (this.this$0.mLock) {
                        SessionCommandGroup2 unused = this.this$0.mAllowedCommands = allowedCommands;
                    }
                    this.this$0.mCallbackExecutor.execute(new Runnable() {
                        public void run() {
                            ControllerCompatCallback.this.this$0.mCallback.onAllowedCommandsChanged(ControllerCompatCallback.this.this$0.mInstance, allowedCommands);
                        }
                    });
                    break;
                case 1:
                    final int playerState = extras.getInt("android.support.v4.media.argument.PLAYER_STATE");
                    PlaybackStateCompat state = (PlaybackStateCompat) extras.getParcelable("android.support.v4.media.argument.PLAYBACK_STATE_COMPAT");
                    if (state != null) {
                        synchronized (this.this$0.mLock) {
                            int unused2 = this.this$0.mPlayerState = playerState;
                            PlaybackStateCompat unused3 = this.this$0.mPlaybackStateCompat = state;
                        }
                        this.this$0.mCallbackExecutor.execute(new Runnable() {
                            public void run() {
                                ControllerCompatCallback.this.this$0.mCallback.onPlayerStateChanged(ControllerCompatCallback.this.this$0.mInstance, playerState);
                            }
                        });
                        break;
                    } else {
                        return;
                    }
                case 2:
                    final MediaItem2 item = MediaItem2.fromBundle(extras.getBundle("android.support.v4.media.argument.MEDIA_ITEM"));
                    synchronized (this.this$0.mLock) {
                        MediaItem2 unused4 = this.this$0.mCurrentMediaItem = item;
                    }
                    this.this$0.mCallbackExecutor.execute(new Runnable() {
                        public void run() {
                            ControllerCompatCallback.this.this$0.mCallback.onCurrentMediaItemChanged(ControllerCompatCallback.this.this$0.mInstance, item);
                        }
                    });
                    break;
                case 3:
                    final int errorCode = extras.getInt("android.support.v4.media.argument.ERROR_CODE");
                    final Bundle errorExtras = extras.getBundle("android.support.v4.media.argument.EXTRAS");
                    this.this$0.mCallbackExecutor.execute(new Runnable() {
                        public void run() {
                            ControllerCompatCallback.this.this$0.mCallback.onError(ControllerCompatCallback.this.this$0.mInstance, errorCode, errorExtras);
                        }
                    });
                    break;
                case 4:
                    final List<Bundle> routes = MediaUtils2.convertToBundleList(extras.getParcelableArray("android.support.v4.media.argument.ROUTE_BUNDLE"));
                    this.this$0.mCallbackExecutor.execute(new Runnable() {
                        public void run() {
                            ControllerCompatCallback.this.this$0.mCallback.onRoutesInfoChanged(ControllerCompatCallback.this.this$0.mInstance, routes);
                        }
                    });
                    break;
                case 5:
                    final MediaMetadata2 playlistMetadata = MediaMetadata2.fromBundle(extras.getBundle("android.support.v4.media.argument.PLAYLIST_METADATA"));
                    final List<MediaItem2> playlist = MediaUtils2.convertToMediaItem2List(extras.getParcelableArray("android.support.v4.media.argument.PLAYLIST"));
                    synchronized (this.this$0.mLock) {
                        List unused5 = this.this$0.mPlaylist = playlist;
                        MediaMetadata2 unused6 = this.this$0.mPlaylistMetadata = playlistMetadata;
                    }
                    this.this$0.mCallbackExecutor.execute(new Runnable() {
                        public void run() {
                            ControllerCompatCallback.this.this$0.mCallback.onPlaylistChanged(ControllerCompatCallback.this.this$0.mInstance, playlist, playlistMetadata);
                        }
                    });
                    break;
                case 6:
                    final MediaMetadata2 playlistMetadata2 = MediaMetadata2.fromBundle(extras.getBundle("android.support.v4.media.argument.PLAYLIST_METADATA"));
                    synchronized (this.this$0.mLock) {
                        MediaMetadata2 unused7 = this.this$0.mPlaylistMetadata = playlistMetadata2;
                    }
                    this.this$0.mCallbackExecutor.execute(new Runnable() {
                        public void run() {
                            ControllerCompatCallback.this.this$0.mCallback.onPlaylistMetadataChanged(ControllerCompatCallback.this.this$0.mInstance, playlistMetadata2);
                        }
                    });
                    break;
                case 7:
                    final int repeatMode = extras.getInt("android.support.v4.media.argument.REPEAT_MODE");
                    synchronized (this.this$0.mLock) {
                        int unused8 = this.this$0.mRepeatMode = repeatMode;
                    }
                    this.this$0.mCallbackExecutor.execute(new Runnable() {
                        public void run() {
                            ControllerCompatCallback.this.this$0.mCallback.onRepeatModeChanged(ControllerCompatCallback.this.this$0.mInstance, repeatMode);
                        }
                    });
                    break;
                case 8:
                    final int shuffleMode = extras.getInt("android.support.v4.media.argument.SHUFFLE_MODE");
                    synchronized (this.this$0.mLock) {
                        int unused9 = this.this$0.mShuffleMode = shuffleMode;
                    }
                    this.this$0.mCallbackExecutor.execute(new Runnable() {
                        public void run() {
                            ControllerCompatCallback.this.this$0.mCallback.onShuffleModeChanged(ControllerCompatCallback.this.this$0.mInstance, shuffleMode);
                        }
                    });
                    break;
                case 9:
                    Bundle commandBundle = extras.getBundle("android.support.v4.media.argument.CUSTOM_COMMAND");
                    if (commandBundle != null) {
                        final SessionCommand2 command = SessionCommand2.fromBundle(commandBundle);
                        final Bundle args = extras.getBundle("android.support.v4.media.argument.ARGUMENTS");
                        final ResultReceiver receiver = (ResultReceiver) extras.getParcelable("android.support.v4.media.argument.RESULT_RECEIVER");
                        this.this$0.mCallbackExecutor.execute(new Runnable() {
                            public void run() {
                                ControllerCompatCallback.this.this$0.mCallback.onCustomCommand(ControllerCompatCallback.this.this$0.mInstance, command, args, receiver);
                            }
                        });
                        break;
                    } else {
                        return;
                    }
                case 10:
                    final List<MediaSession2.CommandButton> layout = MediaUtils2.convertToCommandButtonList(extras.getParcelableArray("android.support.v4.media.argument.COMMAND_BUTTONS"));
                    if (layout != null) {
                        this.this$0.mCallbackExecutor.execute(new Runnable() {
                            public void run() {
                                ControllerCompatCallback.this.this$0.mCallback.onCustomLayoutChanged(ControllerCompatCallback.this.this$0.mInstance, layout);
                            }
                        });
                        break;
                    } else {
                        return;
                    }
                case 11:
                    final MediaController2.PlaybackInfo info = MediaController2.PlaybackInfo.fromBundle(extras.getBundle("android.support.v4.media.argument.PLAYBACK_INFO"));
                    if (info != null) {
                        synchronized (this.this$0.mLock) {
                            MediaController2.PlaybackInfo unused10 = this.this$0.mPlaybackInfo = info;
                        }
                        this.this$0.mCallbackExecutor.execute(new Runnable() {
                            public void run() {
                                ControllerCompatCallback.this.this$0.mCallback.onPlaybackInfoChanged(ControllerCompatCallback.this.this$0.mInstance, info);
                            }
                        });
                        break;
                    } else {
                        return;
                    }
                case 12:
                    final PlaybackStateCompat state2 = (PlaybackStateCompat) extras.getParcelable("android.support.v4.media.argument.PLAYBACK_STATE_COMPAT");
                    if (state2 != null) {
                        synchronized (this.this$0.mLock) {
                            PlaybackStateCompat unused11 = this.this$0.mPlaybackStateCompat = state2;
                        }
                        this.this$0.mCallbackExecutor.execute(new Runnable() {
                            public void run() {
                                ControllerCompatCallback.this.this$0.mCallback.onPlaybackSpeedChanged(ControllerCompatCallback.this.this$0.mInstance, state2.getPlaybackSpeed());
                            }
                        });
                        break;
                    } else {
                        return;
                    }
                case 13:
                    final MediaItem2 item2 = MediaItem2.fromBundle(extras.getBundle("android.support.v4.media.argument.MEDIA_ITEM"));
                    final int bufferingState = extras.getInt("android.support.v4.media.argument.BUFFERING_STATE");
                    PlaybackStateCompat state3 = (PlaybackStateCompat) extras.getParcelable("android.support.v4.media.argument.PLAYBACK_STATE_COMPAT");
                    if (item2 != null && state3 != null) {
                        synchronized (this.this$0.mLock) {
                            int unused12 = this.this$0.mBufferingState = bufferingState;
                            PlaybackStateCompat unused13 = this.this$0.mPlaybackStateCompat = state3;
                        }
                        this.this$0.mCallbackExecutor.execute(new Runnable() {
                            public void run() {
                                ControllerCompatCallback.this.this$0.mCallback.onBufferingStateChanged(ControllerCompatCallback.this.this$0.mInstance, item2, bufferingState);
                            }
                        });
                        break;
                    } else {
                        return;
                    }
                case 14:
                    final long position = extras.getLong("android.support.v4.media.argument.SEEK_POSITION");
                    PlaybackStateCompat state4 = (PlaybackStateCompat) extras.getParcelable("android.support.v4.media.argument.PLAYBACK_STATE_COMPAT");
                    if (state4 != null) {
                        synchronized (this.this$0.mLock) {
                            PlaybackStateCompat unused14 = this.this$0.mPlaybackStateCompat = state4;
                        }
                        this.this$0.mCallbackExecutor.execute(new Runnable() {
                            public void run() {
                                ControllerCompatCallback.this.this$0.mCallback.onSeekCompleted(ControllerCompatCallback.this.this$0.mInstance, position);
                            }
                        });
                        break;
                    } else {
                        return;
                    }
            }
        }
    }

    static {
        sDefaultRootExtras.putBoolean("android.support.v4.media.root_default_root", true);
    }

    public void close() {
        if (DEBUG) {
            Log.d("MC2ImplLegacy", "release from " + this.mToken);
        }
        synchronized (this.mLock) {
            if (!this.mIsReleased) {
                this.mHandler.removeCallbacksAndMessages(null);
                if (Build.VERSION.SDK_INT >= 18) {
                    this.mHandlerThread.quitSafely();
                } else {
                    this.mHandlerThread.quit();
                }
                this.mIsReleased = true;
                sendCommand("android.support.v4.media.controller.command.DISCONNECT");
                if (this.mControllerCompat != null) {
                    this.mControllerCompat.unregisterCallback(this.mControllerCompatCallback);
                }
                if (this.mBrowserCompat != null) {
                    this.mBrowserCompat.disconnect();
                    this.mBrowserCompat = null;
                }
                if (this.mControllerCompat != null) {
                    this.mControllerCompat.unregisterCallback(this.mControllerCompatCallback);
                    this.mControllerCompat = null;
                }
                this.mConnected = false;
                this.mCallbackExecutor.execute(new Runnable() {
                    public void run() {
                        MediaController2ImplLegacy.this.mCallback.onDisconnected(MediaController2ImplLegacy.this.mInstance);
                    }
                });
            }
        }
    }

    /* access modifiers changed from: package-private */
    /* JADX WARNING: Code restructure failed: missing block: B:11:0x0088, code lost:
        if (0 == 0) goto L_0x008d;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:12:0x008a, code lost:
        close();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:13:0x008d, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:19:0x009b, code lost:
        if (1 == 0) goto L_0x00a0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:20:0x009d, code lost:
        close();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:21:0x00a0, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:26:?, code lost:
        r14.mCallbackExecutor.execute(new android.support.v4.media.MediaController2ImplLegacy.AnonymousClass2(r14));
     */
    /* JADX WARNING: Code restructure failed: missing block: B:27:0x00c3, code lost:
        if (0 == 0) goto L_0x00c8;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:28:0x00c5, code lost:
        close();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:29:0x00c8, code lost:
        return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void onConnectedNotLocked(android.os.Bundle r15) {
        /*
            r14 = this;
            java.lang.Class<android.support.v4.media.MediaSession2> r0 = android.support.v4.media.MediaSession2.class
            java.lang.ClassLoader r0 = r0.getClassLoader()
            r15.setClassLoader(r0)
            java.lang.String r0 = "android.support.v4.media.argument.ALLOWED_COMMANDS"
            android.os.Bundle r0 = r15.getBundle(r0)
            android.support.v4.media.SessionCommandGroup2 r0 = android.support.v4.media.SessionCommandGroup2.fromBundle(r0)
            java.lang.String r1 = "android.support.v4.media.argument.PLAYER_STATE"
            int r1 = r15.getInt(r1)
            java.lang.String r2 = "android.support.v4.media.argument.MEDIA_ITEM"
            android.os.Bundle r2 = r15.getBundle(r2)
            android.support.v4.media.MediaItem2 r2 = android.support.v4.media.MediaItem2.fromBundle(r2)
            java.lang.String r3 = "android.support.v4.media.argument.BUFFERING_STATE"
            int r3 = r15.getInt(r3)
            java.lang.String r4 = "android.support.v4.media.argument.PLAYBACK_STATE_COMPAT"
            android.os.Parcelable r4 = r15.getParcelable(r4)
            android.support.v4.media.session.PlaybackStateCompat r4 = (android.support.v4.media.session.PlaybackStateCompat) r4
            java.lang.String r5 = "android.support.v4.media.argument.REPEAT_MODE"
            int r5 = r15.getInt(r5)
            java.lang.String r6 = "android.support.v4.media.argument.SHUFFLE_MODE"
            int r6 = r15.getInt(r6)
            java.lang.String r7 = "android.support.v4.media.argument.PLAYLIST"
            android.os.Parcelable[] r7 = r15.getParcelableArray(r7)
            java.util.List r7 = android.support.v4.media.MediaUtils2.convertToMediaItem2List(r7)
            java.lang.String r8 = "android.support.v4.media.argument.PLAYBACK_INFO"
            android.os.Bundle r8 = r15.getBundle(r8)
            android.support.v4.media.MediaController2$PlaybackInfo r8 = android.support.v4.media.MediaController2.PlaybackInfo.fromBundle(r8)
            java.lang.String r9 = "android.support.v4.media.argument.PLAYLIST_METADATA"
            android.os.Bundle r9 = r15.getBundle(r9)
            android.support.v4.media.MediaMetadata2 r9 = android.support.v4.media.MediaMetadata2.fromBundle(r9)
            boolean r10 = DEBUG
            if (r10 == 0) goto L_0x007f
            java.lang.String r10 = "MC2ImplLegacy"
            java.lang.StringBuilder r11 = new java.lang.StringBuilder
            r11.<init>()
            java.lang.String r12 = "onConnectedNotLocked token="
            r11.append(r12)
            android.support.v4.media.SessionToken2 r12 = r14.mToken
            r11.append(r12)
            java.lang.String r12 = ", allowedCommands="
            r11.append(r12)
            r11.append(r0)
            java.lang.String r11 = r11.toString()
            android.util.Log.d(r10, r11)
        L_0x007f:
            r10 = 0
            java.lang.Object r11 = r14.mLock     // Catch:{ all -> 0x00cc }
            monitor-enter(r11)     // Catch:{ all -> 0x00cc }
            boolean r12 = r14.mIsReleased     // Catch:{ all -> 0x00c9 }
            if (r12 == 0) goto L_0x008e
            monitor-exit(r11)     // Catch:{ all -> 0x00c9 }
            if (r10 == 0) goto L_0x008d
            r14.close()
        L_0x008d:
            return
        L_0x008e:
            boolean r12 = r14.mConnected     // Catch:{ all -> 0x00c9 }
            if (r12 == 0) goto L_0x00a1
            java.lang.String r12 = "MC2ImplLegacy"
            java.lang.String r13 = "Cannot be notified about the connection result many times. Probably a bug or malicious app."
            android.util.Log.e(r12, r13)     // Catch:{ all -> 0x00c9 }
            r10 = 1
            monitor-exit(r11)     // Catch:{ all -> 0x00c9 }
            if (r10 == 0) goto L_0x00a0
            r14.close()
        L_0x00a0:
            return
        L_0x00a1:
            r14.mAllowedCommands = r0     // Catch:{ all -> 0x00c9 }
            r14.mPlayerState = r1     // Catch:{ all -> 0x00c9 }
            r14.mCurrentMediaItem = r2     // Catch:{ all -> 0x00c9 }
            r14.mBufferingState = r3     // Catch:{ all -> 0x00c9 }
            r14.mPlaybackStateCompat = r4     // Catch:{ all -> 0x00c9 }
            r14.mRepeatMode = r5     // Catch:{ all -> 0x00c9 }
            r14.mShuffleMode = r6     // Catch:{ all -> 0x00c9 }
            r14.mPlaylist = r7     // Catch:{ all -> 0x00c9 }
            r14.mPlaylistMetadata = r9     // Catch:{ all -> 0x00c9 }
            r12 = 1
            r14.mConnected = r12     // Catch:{ all -> 0x00c9 }
            r14.mPlaybackInfo = r8     // Catch:{ all -> 0x00c9 }
            monitor-exit(r11)     // Catch:{ all -> 0x00c9 }
            java.util.concurrent.Executor r11 = r14.mCallbackExecutor     // Catch:{ all -> 0x00cc }
            android.support.v4.media.MediaController2ImplLegacy$2 r12 = new android.support.v4.media.MediaController2ImplLegacy$2     // Catch:{ all -> 0x00cc }
            r12.<init>(r0)     // Catch:{ all -> 0x00cc }
            r11.execute(r12)     // Catch:{ all -> 0x00cc }
            if (r10 == 0) goto L_0x00c8
            r14.close()
        L_0x00c8:
            return
        L_0x00c9:
            r12 = move-exception
            monitor-exit(r11)     // Catch:{ all -> 0x00c9 }
            throw r12     // Catch:{ all -> 0x00cc }
        L_0x00cc:
            r11 = move-exception
            if (r10 == 0) goto L_0x00d2
            r14.close()
        L_0x00d2:
            throw r11
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.media.MediaController2ImplLegacy.onConnectedNotLocked(android.os.Bundle):void");
    }

    private void sendCommand(String command) {
        sendCommand(command, null, null);
    }

    /* access modifiers changed from: private */
    public void sendCommand(String command, ResultReceiver receiver) {
        sendCommand(command, null, receiver);
    }

    private void sendCommand(String command, Bundle args, ResultReceiver receiver) {
        if (args == null) {
            args = new Bundle();
        }
        synchronized (this.mLock) {
            try {
                MediaControllerCompat controller = this.mControllerCompat;
                try {
                    ControllerCompatCallback callback = this.mControllerCompatCallback;
                    BundleCompat.putBinder(args, "android.support.v4.media.argument.ICONTROLLER_CALLBACK", callback.getIControllerCallback().asBinder());
                    args.putString("android.support.v4.media.argument.PACKAGE_NAME", this.mContext.getPackageName());
                    args.putInt("android.support.v4.media.argument.UID", Process.myUid());
                    args.putInt("android.support.v4.media.argument.PID", Process.myPid());
                    controller.sendCommand(command, args, receiver);
                } catch (Throwable th) {
                    th = th;
                    MediaControllerCompat mediaControllerCompat = controller;
                    while (true) {
                        try {
                            break;
                        } catch (Throwable th2) {
                            th = th2;
                        }
                    }
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                while (true) {
                    break;
                }
                throw th;
            }
        }
    }
}
