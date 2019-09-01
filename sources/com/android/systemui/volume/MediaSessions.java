package com.android.systemui.volume;

import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.IRemoteVolumeController;
import android.media.MediaMetadata;
import android.media.session.ISessionController;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MediaSessions {
    /* access modifiers changed from: private */
    public static final String TAG = Util.logTag(MediaSessions.class);
    /* access modifiers changed from: private */
    public final Callbacks mCallbacks;
    private final Context mContext;
    /* access modifiers changed from: private */
    public final H mHandler;
    private boolean mInit;
    /* access modifiers changed from: private */
    public final MediaSessionManager mMgr;
    private final Map<MediaSession.Token, MediaControllerRecord> mRecords = new ConcurrentHashMap();
    private final IRemoteVolumeController mRvc = new IRemoteVolumeController.Stub() {
        public void remoteVolumeChanged(ISessionController session, int flags) throws RemoteException {
            MediaSessions.this.mHandler.obtainMessage(2, flags, 0, session).sendToTarget();
        }

        public void updateRemoteController(ISessionController session) throws RemoteException {
            MediaSessions.this.mHandler.obtainMessage(3, session).sendToTarget();
        }
    };
    private final MediaSessionManager.OnActiveSessionsChangedListener mSessionsListener = new MediaSessionManager.OnActiveSessionsChangedListener() {
        public void onActiveSessionsChanged(List<MediaController> controllers) {
            MediaSessions.this.onActiveSessionsUpdatedH(controllers);
        }
    };

    public interface Callbacks {
        void onRemoteRemoved(MediaSession.Token token);

        void onRemoteUpdate(MediaSession.Token token, String str, MediaController.PlaybackInfo playbackInfo);

        void onRemoteVolumeChanged(MediaSession.Token token, int i);
    }

    private final class H extends Handler {
        private H(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    MediaSessions.this.onActiveSessionsUpdatedH(MediaSessions.this.mMgr.getActiveSessions(null));
                    return;
                case 2:
                    MediaSessions.this.onRemoteVolumeChangedH((ISessionController) msg.obj, msg.arg1);
                    return;
                case 3:
                    MediaSessions.this.onUpdateRemoteControllerH((ISessionController) msg.obj);
                    return;
                default:
                    return;
            }
        }
    }

    private final class MediaControllerRecord extends MediaController.Callback {
        /* access modifiers changed from: private */
        public final MediaController controller;
        /* access modifiers changed from: private */
        public String name;
        /* access modifiers changed from: private */
        public boolean sentRemote;

        private MediaControllerRecord(MediaController controller2) {
            this.controller = controller2;
        }

        private String cb(String method) {
            return method + " " + this.controller.getPackageName() + " ";
        }

        public void onAudioInfoChanged(MediaController.PlaybackInfo info) {
            if (D.BUG) {
                String access$500 = MediaSessions.TAG;
                Log.d(access$500, cb("onAudioInfoChanged") + Util.playbackInfoToString(info) + " sentRemote=" + this.sentRemote);
            }
            boolean remote = MediaSessions.isRemote(info);
            if (!remote && this.sentRemote) {
                MediaSessions.this.mCallbacks.onRemoteRemoved(this.controller.getSessionToken());
                this.sentRemote = false;
            } else if (remote) {
                MediaSessions.this.updateRemoteH(this.controller.getSessionToken(), this.name, info);
                this.sentRemote = true;
            }
        }

        public void onExtrasChanged(Bundle extras) {
            if (D.BUG) {
                String access$500 = MediaSessions.TAG;
                Log.d(access$500, cb("onExtrasChanged") + extras);
            }
        }

        public void onMetadataChanged(MediaMetadata metadata) {
            if (D.BUG) {
                String access$500 = MediaSessions.TAG;
                Log.d(access$500, cb("onMetadataChanged") + Util.mediaMetadataToString(metadata));
            }
        }

        public void onPlaybackStateChanged(PlaybackState state) {
            if (D.BUG) {
                String access$500 = MediaSessions.TAG;
                Log.d(access$500, cb("onPlaybackStateChanged") + Util.playbackStateToString(state));
            }
        }

        public void onQueueChanged(List<MediaSession.QueueItem> queue) {
            if (D.BUG) {
                String access$500 = MediaSessions.TAG;
                Log.d(access$500, cb("onQueueChanged") + queue);
            }
        }

        public void onQueueTitleChanged(CharSequence title) {
            if (D.BUG) {
                String access$500 = MediaSessions.TAG;
                Log.d(access$500, cb("onQueueTitleChanged") + title);
            }
        }

        public void onSessionDestroyed() {
            if (D.BUG) {
                Log.d(MediaSessions.TAG, cb("onSessionDestroyed"));
            }
        }

        public void onSessionEvent(String event, Bundle extras) {
            if (D.BUG) {
                String access$500 = MediaSessions.TAG;
                Log.d(access$500, cb("onSessionEvent") + "event=" + event + " extras=" + extras);
            }
        }
    }

    public MediaSessions(Context context, Looper looper, Callbacks callbacks) {
        this.mContext = context;
        this.mHandler = new H(looper);
        this.mMgr = (MediaSessionManager) context.getSystemService("media_session");
        this.mCallbacks = callbacks;
    }

    public void dump(PrintWriter writer) {
        writer.println(getClass().getSimpleName() + " state:");
        writer.print("  mInit: ");
        writer.println(this.mInit);
        writer.print("  mRecords.size: ");
        writer.println(this.mRecords.size());
        int i = 0;
        for (MediaControllerRecord r : this.mRecords.values()) {
            i++;
            dump(i, writer, r.controller);
        }
    }

    public void init() {
        if (D.BUG) {
            Log.d(TAG, "init");
        }
        this.mMgr.addOnActiveSessionsChangedListener(this.mSessionsListener, null, this.mHandler);
        this.mInit = true;
        postUpdateSessions();
        this.mMgr.setRemoteVolumeController(this.mRvc);
    }

    /* access modifiers changed from: protected */
    public void postUpdateSessions() {
        if (this.mInit) {
            this.mHandler.sendEmptyMessage(1);
        }
    }

    public void setVolume(MediaSession.Token token, int level) {
        MediaControllerRecord r = this.mRecords.get(token);
        if (r == null) {
            String str = TAG;
            Log.w(str, "setVolume: No record found for token " + token);
            return;
        }
        if (D.BUG) {
            String str2 = TAG;
            Log.d(str2, "Setting level to " + level);
        }
        r.controller.setVolumeTo(level, 0);
    }

    /* access modifiers changed from: private */
    public void onRemoteVolumeChangedH(ISessionController session, int flags) {
        MediaController controller = new MediaController(this.mContext, session);
        if (D.BUG) {
            String str = TAG;
            Log.d(str, "remoteVolumeChangedH " + controller.getPackageName() + " " + Util.audioManagerFlagsToString(flags));
        }
        this.mCallbacks.onRemoteVolumeChanged(controller.getSessionToken(), flags);
    }

    /* access modifiers changed from: private */
    public void onUpdateRemoteControllerH(ISessionController session) {
        MediaController controller;
        String pkg = null;
        if (session != null) {
            controller = new MediaController(this.mContext, session);
        } else {
            controller = null;
        }
        if (controller != null) {
            pkg = controller.getPackageName();
        }
        if (D.BUG) {
            Log.d(TAG, "updateRemoteControllerH " + pkg);
        }
        postUpdateSessions();
    }

    /* access modifiers changed from: protected */
    public void onActiveSessionsUpdatedH(List<MediaController> controllers) {
        if (D.BUG) {
            String str = TAG;
            Log.d(str, "onActiveSessionsUpdatedH n=" + controllers.size());
        }
        Set<MediaSession.Token> toRemove = new HashSet<>(this.mRecords.keySet());
        for (MediaController controller : controllers) {
            MediaSession.Token token = controller.getSessionToken();
            MediaController.PlaybackInfo pi = controller.getPlaybackInfo();
            toRemove.remove(token);
            if (!this.mRecords.containsKey(token)) {
                MediaControllerRecord r = new MediaControllerRecord(controller);
                String unused = r.name = getControllerName(controller);
                this.mRecords.put(token, r);
                controller.registerCallback(r, this.mHandler);
            }
            MediaControllerRecord r2 = this.mRecords.get(token);
            if (isRemote(pi)) {
                updateRemoteH(token, r2.name, pi);
                boolean unused2 = r2.sentRemote = true;
            }
        }
        for (MediaSession.Token t : toRemove) {
            MediaControllerRecord r3 = this.mRecords.get(t);
            r3.controller.unregisterCallback(r3);
            this.mRecords.remove(t);
            if (D.BUG) {
                String str2 = TAG;
                Log.d(str2, "Removing " + r3.name + " sentRemote=" + r3.sentRemote);
            }
            if (r3.sentRemote) {
                this.mCallbacks.onRemoteRemoved(t);
                boolean unused3 = r3.sentRemote = false;
            }
        }
    }

    /* access modifiers changed from: private */
    public static boolean isRemote(MediaController.PlaybackInfo pi) {
        return pi != null && pi.getPlaybackType() == 2;
    }

    /* access modifiers changed from: protected */
    public String getControllerName(MediaController controller) {
        PackageManager pm = this.mContext.getPackageManager();
        String pkg = controller.getPackageName();
        try {
            String appLabel = Objects.toString(pm.getApplicationInfo(pkg, 0).loadLabel(pm), "").trim();
            if (appLabel.length() > 0) {
                return appLabel;
            }
            return pkg;
        } catch (PackageManager.NameNotFoundException e) {
        }
    }

    /* access modifiers changed from: private */
    public void updateRemoteH(MediaSession.Token token, String name, MediaController.PlaybackInfo pi) {
        if (this.mCallbacks != null) {
            this.mCallbacks.onRemoteUpdate(token, name, pi);
        }
    }

    private static void dump(int n, PrintWriter writer, MediaController c) {
        PrintWriter printWriter = writer;
        printWriter.println("  Controller " + n + ": " + c.getPackageName());
        Bundle extras = c.getExtras();
        long flags = c.getFlags();
        MediaMetadata mm = c.getMetadata();
        MediaController.PlaybackInfo pi = c.getPlaybackInfo();
        PlaybackState playbackState = c.getPlaybackState();
        List<MediaSession.QueueItem> queue = c.getQueue();
        CharSequence queueTitle = c.getQueueTitle();
        int ratingType = c.getRatingType();
        PendingIntent sessionActivity = c.getSessionActivity();
        printWriter.println("    PlaybackState: " + Util.playbackStateToString(playbackState));
        printWriter.println("    PlaybackInfo: " + Util.playbackInfoToString(pi));
        if (mm != null) {
            printWriter.println("  MediaMetadata.desc=" + mm.getDescription());
        }
        printWriter.println("    RatingType: " + ratingType);
        printWriter.println("    Flags: " + flags);
        if (extras != null) {
            printWriter.println("    Extras:");
            for (String key : extras.keySet()) {
                printWriter.println("      " + key + "=" + extras.get(key));
            }
        }
        if (queueTitle != null) {
            printWriter.println("    QueueTitle: " + queueTitle);
        }
        if (queue != null && !queue.isEmpty()) {
            printWriter.println("    Queue:");
            Iterator<MediaSession.QueueItem> it = queue.iterator();
            while (it.hasNext()) {
                printWriter.println("      " + it.next());
            }
        }
        if (pi != null) {
            printWriter.println("    sessionActivity: " + sessionActivity);
        }
    }
}
