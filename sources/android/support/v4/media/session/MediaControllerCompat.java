package android.support.v4.media.session;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.support.v4.app.BundleCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.SessionToken2;
import android.support.v4.media.session.IMediaControllerCallback;
import android.support.v4.media.session.IMediaSession;
import android.support.v4.media.session.MediaControllerCompatApi21;
import android.support.v4.media.session.MediaSessionCompat;
import android.text.TextUtils;
import android.util.Log;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public final class MediaControllerCompat {
    private final MediaControllerImpl mImpl;
    private final HashSet<Callback> mRegisteredCallbacks;

    public static abstract class Callback implements IBinder.DeathRecipient {
        /* access modifiers changed from: private */
        public final Object mCallbackObj;
        MessageHandler mHandler;
        IMediaControllerCallback mIControllerCallback;

        private class MessageHandler extends Handler {
            boolean mRegistered = false;

            MessageHandler(Looper looper) {
                super(looper);
            }

            public void handleMessage(Message msg) {
                if (this.mRegistered) {
                    switch (msg.what) {
                        case 1:
                            Callback.this.onSessionEvent((String) msg.obj, msg.getData());
                            break;
                        case 2:
                            Callback.this.onPlaybackStateChanged((PlaybackStateCompat) msg.obj);
                            break;
                        case 3:
                            Callback.this.onMetadataChanged((MediaMetadataCompat) msg.obj);
                            break;
                        case 4:
                            Callback.this.onAudioInfoChanged((PlaybackInfo) msg.obj);
                            break;
                        case 5:
                            Callback.this.onQueueChanged((List) msg.obj);
                            break;
                        case 6:
                            Callback.this.onQueueTitleChanged((CharSequence) msg.obj);
                            break;
                        case 7:
                            Callback.this.onExtrasChanged((Bundle) msg.obj);
                            break;
                        case 8:
                            Callback.this.onSessionDestroyed();
                            break;
                        case 9:
                            Callback.this.onRepeatModeChanged(((Integer) msg.obj).intValue());
                            break;
                        case 11:
                            Callback.this.onCaptioningEnabledChanged(((Boolean) msg.obj).booleanValue());
                            break;
                        case 12:
                            Callback.this.onShuffleModeChanged(((Integer) msg.obj).intValue());
                            break;
                        case 13:
                            Callback.this.onSessionReady();
                            break;
                    }
                }
            }
        }

        private static class StubApi21 implements MediaControllerCompatApi21.Callback {
            private final WeakReference<Callback> mCallback;

            StubApi21(Callback callback) {
                this.mCallback = new WeakReference<>(callback);
            }

            public void onSessionDestroyed() {
                Callback callback = (Callback) this.mCallback.get();
                if (callback != null) {
                    callback.onSessionDestroyed();
                }
            }

            public void onSessionEvent(String event, Bundle extras) {
                Callback callback = (Callback) this.mCallback.get();
                if (callback == null) {
                    return;
                }
                if (callback.mIControllerCallback == null || Build.VERSION.SDK_INT >= 23) {
                    callback.onSessionEvent(event, extras);
                }
            }

            public void onPlaybackStateChanged(Object stateObj) {
                Callback callback = (Callback) this.mCallback.get();
                if (callback != null && callback.mIControllerCallback == null) {
                    callback.onPlaybackStateChanged(PlaybackStateCompat.fromPlaybackState(stateObj));
                }
            }

            public void onMetadataChanged(Object metadataObj) {
                Callback callback = (Callback) this.mCallback.get();
                if (callback != null) {
                    callback.onMetadataChanged(MediaMetadataCompat.fromMediaMetadata(metadataObj));
                }
            }

            public void onQueueChanged(List<?> queue) {
                Callback callback = (Callback) this.mCallback.get();
                if (callback != null) {
                    callback.onQueueChanged(MediaSessionCompat.QueueItem.fromQueueItemList(queue));
                }
            }

            public void onQueueTitleChanged(CharSequence title) {
                Callback callback = (Callback) this.mCallback.get();
                if (callback != null) {
                    callback.onQueueTitleChanged(title);
                }
            }

            public void onExtrasChanged(Bundle extras) {
                Callback callback = (Callback) this.mCallback.get();
                if (callback != null) {
                    callback.onExtrasChanged(extras);
                }
            }

            public void onAudioInfoChanged(int type, int stream, int control, int max, int current) {
                Callback callback = (Callback) this.mCallback.get();
                if (callback != null) {
                    PlaybackInfo playbackInfo = new PlaybackInfo(type, stream, control, max, current);
                    callback.onAudioInfoChanged(playbackInfo);
                }
            }
        }

        private static class StubCompat extends IMediaControllerCallback.Stub {
            private final WeakReference<Callback> mCallback;

            StubCompat(Callback callback) {
                this.mCallback = new WeakReference<>(callback);
            }

            public void onEvent(String event, Bundle extras) throws RemoteException {
                Callback callback = (Callback) this.mCallback.get();
                if (callback != null) {
                    callback.postToHandler(1, event, extras);
                }
            }

            public void onSessionDestroyed() throws RemoteException {
                Callback callback = (Callback) this.mCallback.get();
                if (callback != null) {
                    callback.postToHandler(8, null, null);
                }
            }

            public void onPlaybackStateChanged(PlaybackStateCompat state) throws RemoteException {
                Callback callback = (Callback) this.mCallback.get();
                if (callback != null) {
                    callback.postToHandler(2, state, null);
                }
            }

            public void onMetadataChanged(MediaMetadataCompat metadata) throws RemoteException {
                Callback callback = (Callback) this.mCallback.get();
                if (callback != null) {
                    callback.postToHandler(3, metadata, null);
                }
            }

            public void onQueueChanged(List<MediaSessionCompat.QueueItem> queue) throws RemoteException {
                Callback callback = (Callback) this.mCallback.get();
                if (callback != null) {
                    callback.postToHandler(5, queue, null);
                }
            }

            public void onQueueTitleChanged(CharSequence title) throws RemoteException {
                Callback callback = (Callback) this.mCallback.get();
                if (callback != null) {
                    callback.postToHandler(6, title, null);
                }
            }

            public void onCaptioningEnabledChanged(boolean enabled) throws RemoteException {
                Callback callback = (Callback) this.mCallback.get();
                if (callback != null) {
                    callback.postToHandler(11, Boolean.valueOf(enabled), null);
                }
            }

            public void onRepeatModeChanged(int repeatMode) throws RemoteException {
                Callback callback = (Callback) this.mCallback.get();
                if (callback != null) {
                    callback.postToHandler(9, Integer.valueOf(repeatMode), null);
                }
            }

            public void onShuffleModeChangedRemoved(boolean enabled) throws RemoteException {
            }

            public void onShuffleModeChanged(int shuffleMode) throws RemoteException {
                Callback callback = (Callback) this.mCallback.get();
                if (callback != null) {
                    callback.postToHandler(12, Integer.valueOf(shuffleMode), null);
                }
            }

            public void onExtrasChanged(Bundle extras) throws RemoteException {
                Callback callback = (Callback) this.mCallback.get();
                if (callback != null) {
                    callback.postToHandler(7, extras, null);
                }
            }

            public void onVolumeInfoChanged(ParcelableVolumeInfo info) throws RemoteException {
                Callback callback = (Callback) this.mCallback.get();
                if (callback != null) {
                    PlaybackInfo pi = null;
                    if (info != null) {
                        PlaybackInfo playbackInfo = new PlaybackInfo(info.volumeType, info.audioStream, info.controlType, info.maxVolume, info.currentVolume);
                        pi = playbackInfo;
                    }
                    callback.postToHandler(4, pi, null);
                }
            }

            public void onSessionReady() throws RemoteException {
                Callback callback = (Callback) this.mCallback.get();
                if (callback != null) {
                    callback.postToHandler(13, null, null);
                }
            }
        }

        public Callback() {
            if (Build.VERSION.SDK_INT >= 21) {
                this.mCallbackObj = MediaControllerCompatApi21.createCallback(new StubApi21(this));
                return;
            }
            StubCompat stubCompat = new StubCompat(this);
            this.mIControllerCallback = stubCompat;
            this.mCallbackObj = stubCompat;
        }

        public void onSessionReady() {
        }

        public void onSessionDestroyed() {
        }

        public void onSessionEvent(String event, Bundle extras) {
        }

        public void onPlaybackStateChanged(PlaybackStateCompat state) {
        }

        public void onMetadataChanged(MediaMetadataCompat metadata) {
        }

        public void onQueueChanged(List<MediaSessionCompat.QueueItem> list) {
        }

        public void onQueueTitleChanged(CharSequence title) {
        }

        public void onExtrasChanged(Bundle extras) {
        }

        public void onAudioInfoChanged(PlaybackInfo info) {
        }

        public void onCaptioningEnabledChanged(boolean enabled) {
        }

        public void onRepeatModeChanged(int repeatMode) {
        }

        public void onShuffleModeChanged(int shuffleMode) {
        }

        public IMediaControllerCallback getIControllerCallback() {
            return this.mIControllerCallback;
        }

        public void binderDied() {
            onSessionDestroyed();
        }

        /* access modifiers changed from: package-private */
        public void setHandler(Handler handler) {
            if (handler != null) {
                this.mHandler = new MessageHandler(handler.getLooper());
                this.mHandler.mRegistered = true;
            } else if (this.mHandler != null) {
                this.mHandler.mRegistered = false;
                this.mHandler.removeCallbacksAndMessages(null);
                this.mHandler = null;
            }
        }

        /* access modifiers changed from: package-private */
        public void postToHandler(int what, Object obj, Bundle data) {
            if (this.mHandler != null) {
                Message msg = this.mHandler.obtainMessage(what, obj);
                msg.setData(data);
                msg.sendToTarget();
            }
        }
    }

    interface MediaControllerImpl {
        void sendCommand(String str, Bundle bundle, ResultReceiver resultReceiver);

        void unregisterCallback(Callback callback);
    }

    static class MediaControllerImplApi21 implements MediaControllerImpl {
        private HashMap<Callback, ExtraCallback> mCallbackMap;
        protected final Object mControllerObj;
        private final List<Callback> mPendingCallbacks;
        /* access modifiers changed from: private */
        public final MediaSessionCompat.Token mSessionToken;

        private static class ExtraBinderRequestResultReceiver extends ResultReceiver {
            private WeakReference<MediaControllerImplApi21> mMediaControllerImpl;

            /* access modifiers changed from: protected */
            public void onReceiveResult(int resultCode, Bundle resultData) {
                MediaControllerImplApi21 mediaControllerImpl = (MediaControllerImplApi21) this.mMediaControllerImpl.get();
                if (mediaControllerImpl != null && resultData != null) {
                    mediaControllerImpl.mSessionToken.setExtraBinder(IMediaSession.Stub.asInterface(BundleCompat.getBinder(resultData, "android.support.v4.media.session.EXTRA_BINDER")));
                    mediaControllerImpl.mSessionToken.setSessionToken2(SessionToken2.fromBundle(resultData.getBundle("android.support.v4.media.session.SESSION_TOKEN2")));
                    mediaControllerImpl.processPendingCallbacks();
                }
            }
        }

        private static class ExtraCallback extends Callback.StubCompat {
            ExtraCallback(Callback callback) {
                super(callback);
            }

            public void onSessionDestroyed() throws RemoteException {
                throw new AssertionError();
            }

            public void onMetadataChanged(MediaMetadataCompat metadata) throws RemoteException {
                throw new AssertionError();
            }

            public void onQueueChanged(List<MediaSessionCompat.QueueItem> list) throws RemoteException {
                throw new AssertionError();
            }

            public void onQueueTitleChanged(CharSequence title) throws RemoteException {
                throw new AssertionError();
            }

            public void onExtrasChanged(Bundle extras) throws RemoteException {
                throw new AssertionError();
            }

            public void onVolumeInfoChanged(ParcelableVolumeInfo info) throws RemoteException {
                throw new AssertionError();
            }
        }

        /* JADX WARNING: No exception handlers in catch block: Catch:{  } */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public final void unregisterCallback(android.support.v4.media.session.MediaControllerCompat.Callback r4) {
            /*
                r3 = this;
                java.lang.Object r0 = r3.mControllerObj
                java.lang.Object r1 = r4.mCallbackObj
                android.support.v4.media.session.MediaControllerCompatApi21.unregisterCallback(r0, r1)
                android.support.v4.media.session.MediaSessionCompat$Token r0 = r3.mSessionToken
                android.support.v4.media.session.IMediaSession r0 = r0.getExtraBinder()
                if (r0 == 0) goto L_0x0031
                java.util.HashMap<android.support.v4.media.session.MediaControllerCompat$Callback, android.support.v4.media.session.MediaControllerCompat$MediaControllerImplApi21$ExtraCallback> r0 = r3.mCallbackMap     // Catch:{ RemoteException -> 0x0028 }
                java.lang.Object r0 = r0.remove(r4)     // Catch:{ RemoteException -> 0x0028 }
                android.support.v4.media.session.MediaControllerCompat$MediaControllerImplApi21$ExtraCallback r0 = (android.support.v4.media.session.MediaControllerCompat.MediaControllerImplApi21.ExtraCallback) r0     // Catch:{ RemoteException -> 0x0028 }
                if (r0 == 0) goto L_0x0030
                r1 = 0
                r4.mIControllerCallback = r1     // Catch:{ RemoteException -> 0x0028 }
                android.support.v4.media.session.MediaSessionCompat$Token r1 = r3.mSessionToken     // Catch:{ RemoteException -> 0x0028 }
                android.support.v4.media.session.IMediaSession r1 = r1.getExtraBinder()     // Catch:{ RemoteException -> 0x0028 }
                r1.unregisterCallbackListener(r0)     // Catch:{ RemoteException -> 0x0028 }
                goto L_0x0030
            L_0x0028:
                r0 = move-exception
                java.lang.String r1 = "MediaControllerCompat"
                java.lang.String r2 = "Dead object in unregisterCallback."
                android.util.Log.e(r1, r2, r0)
            L_0x0030:
                goto L_0x003a
            L_0x0031:
                java.util.List<android.support.v4.media.session.MediaControllerCompat$Callback> r0 = r3.mPendingCallbacks
                monitor-enter(r0)
                java.util.List<android.support.v4.media.session.MediaControllerCompat$Callback> r1 = r3.mPendingCallbacks     // Catch:{ all -> 0x003b }
                r1.remove(r4)     // Catch:{ all -> 0x003b }
                monitor-exit(r0)     // Catch:{ all -> 0x003b }
            L_0x003a:
                return
            L_0x003b:
                r1 = move-exception
                monitor-exit(r0)     // Catch:{ all -> 0x003b }
                throw r1
            */
            throw new UnsupportedOperationException("Method not decompiled: android.support.v4.media.session.MediaControllerCompat.MediaControllerImplApi21.unregisterCallback(android.support.v4.media.session.MediaControllerCompat$Callback):void");
        }

        public void sendCommand(String command, Bundle params, ResultReceiver cb) {
            MediaControllerCompatApi21.sendCommand(this.mControllerObj, command, params, cb);
        }

        /* access modifiers changed from: private */
        public void processPendingCallbacks() {
            if (this.mSessionToken.getExtraBinder() != null) {
                synchronized (this.mPendingCallbacks) {
                    for (Callback callback : this.mPendingCallbacks) {
                        ExtraCallback extraCallback = new ExtraCallback(callback);
                        this.mCallbackMap.put(callback, extraCallback);
                        callback.mIControllerCallback = extraCallback;
                        try {
                            this.mSessionToken.getExtraBinder().registerCallbackListener(extraCallback);
                            callback.onSessionReady();
                        } catch (RemoteException e) {
                            Log.e("MediaControllerCompat", "Dead object in registerCallback.", e);
                        }
                    }
                    this.mPendingCallbacks.clear();
                }
            }
        }
    }

    public static final class PlaybackInfo {
        private final int mAudioStream;
        private final int mCurrentVolume;
        private final int mMaxVolume;
        private final int mPlaybackType;
        private final int mVolumeControl;

        PlaybackInfo(int type, int stream, int control, int max, int current) {
            this.mPlaybackType = type;
            this.mAudioStream = stream;
            this.mVolumeControl = control;
            this.mMaxVolume = max;
            this.mCurrentVolume = current;
        }
    }

    public void unregisterCallback(Callback callback) {
        if (callback != null) {
            try {
                this.mRegisteredCallbacks.remove(callback);
                this.mImpl.unregisterCallback(callback);
            } finally {
                callback.setHandler(null);
            }
        } else {
            throw new IllegalArgumentException("callback must not be null");
        }
    }

    public void sendCommand(String command, Bundle params, ResultReceiver cb) {
        if (!TextUtils.isEmpty(command)) {
            this.mImpl.sendCommand(command, params, cb);
            return;
        }
        throw new IllegalArgumentException("command must neither be null nor empty");
    }
}
