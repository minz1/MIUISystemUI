package com.android.systemui.volume;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.media.AudioManagerCompat;
import android.media.AudioServiceInjector;
import android.media.AudioSystem;
import android.media.IVolumeController;
import android.media.VolumePolicy;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.Vibrator;
import android.provider.Settings;
import android.service.notification.Condition;
import android.util.ArrayMap;
import android.util.Log;
import com.android.internal.annotations.GuardedBy;
import com.android.systemui.Dumpable;
import com.android.systemui.Logger;
import com.android.systemui.R;
import com.android.systemui.plugins.VolumeDialogController;
import com.android.systemui.volume.MediaSessions;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class VolumeDialogControllerImpl implements Dumpable, VolumeDialogController {
    private static final ArrayMap<Integer, Integer> STREAMS = new ArrayMap<>();
    /* access modifiers changed from: private */
    public static final String TAG = Util.logTag(VolumeDialogControllerImpl.class);
    private AudioManager mAudio;
    /* access modifiers changed from: private */
    public final C mCallbacks = new C();
    /* access modifiers changed from: private */
    public final Context mContext;
    /* access modifiers changed from: private */
    public boolean mDestroyed;
    private final boolean mHasVibrator;
    /* access modifiers changed from: private */
    public final MediaSessions mMediaSessions;
    private final MediaSessionsCallbacks mMediaSessionsCallbacksW = new MediaSessionsCallbacks();
    /* access modifiers changed from: private */
    public final NotificationManager mNoMan;
    private final SettingObserver mObserver;
    private final Receiver mReceiver = new Receiver();
    /* access modifiers changed from: private */
    public boolean mShowA11yStream;
    private boolean mShowDndTile;
    /* access modifiers changed from: private */
    public final VolumeDialogController.State mState = new VolumeDialogController.State();
    @GuardedBy("this")
    private UserActivityListener mUserActivityListener;
    private final Vibrator mVibrator;
    private int mVoiceAssistStreamType;
    protected final VC mVolumeController;
    private VolumePolicy mVolumePolicy;
    /* access modifiers changed from: private */
    public final W mWorker;
    private final HandlerThread mWorkerThread;

    private final class C implements VolumeDialogController.Callbacks {
        private final ConcurrentHashMap<VolumeDialogController.Callbacks, Handler> mCallbackMap;

        private C() {
            this.mCallbackMap = new ConcurrentHashMap<>();
        }

        public void add(VolumeDialogController.Callbacks callback, Handler handler) {
            if (callback == null || handler == null) {
                throw new IllegalArgumentException();
            }
            this.mCallbackMap.put(callback, handler);
        }

        public void remove(VolumeDialogController.Callbacks callback) {
            this.mCallbackMap.remove(callback);
        }

        public void onShowRequested(final int reason) {
            for (final Map.Entry<VolumeDialogController.Callbacks, Handler> entry : this.mCallbackMap.entrySet()) {
                entry.getValue().post(new Runnable() {
                    public void run() {
                        ((VolumeDialogController.Callbacks) entry.getKey()).onShowRequested(reason);
                    }
                });
            }
        }

        public void onDismissRequested(final int reason) {
            for (final Map.Entry<VolumeDialogController.Callbacks, Handler> entry : this.mCallbackMap.entrySet()) {
                entry.getValue().post(new Runnable() {
                    public void run() {
                        ((VolumeDialogController.Callbacks) entry.getKey()).onDismissRequested(reason);
                    }
                });
            }
        }

        public void onStateChanged(VolumeDialogController.State state) {
            long time = System.currentTimeMillis();
            final VolumeDialogController.State copy = state.copy();
            for (final Map.Entry<VolumeDialogController.Callbacks, Handler> entry : this.mCallbackMap.entrySet()) {
                entry.getValue().post(new Runnable() {
                    public void run() {
                        ((VolumeDialogController.Callbacks) entry.getKey()).onStateChanged(copy);
                    }
                });
            }
            Events.writeState(time, copy);
        }

        public void onLayoutDirectionChanged(final int layoutDirection) {
            for (final Map.Entry<VolumeDialogController.Callbacks, Handler> entry : this.mCallbackMap.entrySet()) {
                entry.getValue().post(new Runnable() {
                    public void run() {
                        ((VolumeDialogController.Callbacks) entry.getKey()).onLayoutDirectionChanged(layoutDirection);
                    }
                });
            }
        }

        public void onConfigurationChanged() {
            VolumeDialogControllerImpl.this.mState.activeStream = -1;
            for (final Map.Entry<VolumeDialogController.Callbacks, Handler> entry : this.mCallbackMap.entrySet()) {
                entry.getValue().post(new Runnable() {
                    public void run() {
                        ((VolumeDialogController.Callbacks) entry.getKey()).onConfigurationChanged();
                    }
                });
            }
        }

        public void onShowVibrateHint() {
            for (final Map.Entry<VolumeDialogController.Callbacks, Handler> entry : this.mCallbackMap.entrySet()) {
                entry.getValue().post(new Runnable() {
                    public void run() {
                        ((VolumeDialogController.Callbacks) entry.getKey()).onShowVibrateHint();
                    }
                });
            }
        }

        public void onShowSilentHint() {
            for (final Map.Entry<VolumeDialogController.Callbacks, Handler> entry : this.mCallbackMap.entrySet()) {
                entry.getValue().post(new Runnable() {
                    public void run() {
                        ((VolumeDialogController.Callbacks) entry.getKey()).onShowSilentHint();
                    }
                });
            }
        }

        public void onScreenOff() {
            for (final Map.Entry<VolumeDialogController.Callbacks, Handler> entry : this.mCallbackMap.entrySet()) {
                entry.getValue().post(new Runnable() {
                    public void run() {
                        ((VolumeDialogController.Callbacks) entry.getKey()).onScreenOff();
                    }
                });
            }
        }

        public void onShowSafetyWarning(final int flags) {
            for (final Map.Entry<VolumeDialogController.Callbacks, Handler> entry : this.mCallbackMap.entrySet()) {
                entry.getValue().post(new Runnable() {
                    public void run() {
                        ((VolumeDialogController.Callbacks) entry.getKey()).onShowSafetyWarning(flags);
                    }
                });
            }
        }

        public void onAccessibilityModeChanged(Boolean showA11yStream) {
            final boolean show = showA11yStream == null ? false : showA11yStream.booleanValue();
            for (final Map.Entry<VolumeDialogController.Callbacks, Handler> entry : this.mCallbackMap.entrySet()) {
                entry.getValue().post(new Runnable() {
                    public void run() {
                        ((VolumeDialogController.Callbacks) entry.getKey()).onAccessibilityModeChanged(Boolean.valueOf(show));
                    }
                });
            }
        }

        public void onVolumeChanged(final int stream, final boolean fromKey) {
            for (final Map.Entry<VolumeDialogController.Callbacks, Handler> entry : this.mCallbackMap.entrySet()) {
                entry.getValue().post(new Runnable() {
                    public void run() {
                        ((VolumeDialogController.Callbacks) entry.getKey()).onVolumeChanged(stream, fromKey);
                    }
                });
            }
        }
    }

    private final class MediaSessionsCallbacks implements MediaSessions.Callbacks {
        private int mNextStream;
        /* access modifiers changed from: private */
        public final HashMap<MediaSession.Token, Integer> mRemoteStreams;

        private MediaSessionsCallbacks() {
            this.mRemoteStreams = new HashMap<>();
            this.mNextStream = 100;
        }

        public void onRemoteUpdate(MediaSession.Token token, String name, MediaController.PlaybackInfo pi) {
            if (!this.mRemoteStreams.containsKey(token)) {
                this.mRemoteStreams.put(token, Integer.valueOf(this.mNextStream));
                if (D.BUG) {
                    Log.d(VolumeDialogControllerImpl.TAG, "onRemoteUpdate: " + name + " is stream " + this.mNextStream);
                }
                this.mNextStream++;
            }
            int stream = this.mRemoteStreams.get(token).intValue();
            boolean changed = VolumeDialogControllerImpl.this.mState.states.indexOfKey(stream) < 0;
            VolumeDialogController.StreamState ss = VolumeDialogControllerImpl.this.streamStateW(stream);
            ss.dynamic = true;
            ss.levelMin = 0;
            ss.levelMax = pi.getMaxVolume();
            if (ss.level != pi.getCurrentVolume()) {
                ss.level = pi.getCurrentVolume();
                changed = true;
            }
            if (!Objects.equals(ss.remoteLabel, name)) {
                ss.nameRes = -1;
                ss.remoteLabel = name;
                changed = true;
            }
            if (D.BUG) {
                Log.d(VolumeDialogControllerImpl.TAG, "onRemoteUpdate: " + name + " level: " + ss.level + " of levelMax:" + ss.levelMax);
            }
            if (changed) {
                if (D.BUG) {
                    Log.d(VolumeDialogControllerImpl.TAG, "onRemoteUpdate: " + name + ": " + ss.level + " of " + ss.levelMax);
                }
                VolumeDialogControllerImpl.this.mCallbacks.onStateChanged(VolumeDialogControllerImpl.this.mState);
            }
        }

        public void onRemoteVolumeChanged(MediaSession.Token token, int flags) {
            if (this.mRemoteStreams.get(token) != null) {
                int stream = this.mRemoteStreams.get(token).intValue();
                boolean showUI = (flags & 1) != 0;
                boolean changed = VolumeDialogControllerImpl.this.updateActiveStreamW(stream);
                if (showUI) {
                    changed |= VolumeDialogControllerImpl.this.checkRoutedToBluetoothW(3);
                }
                if (changed) {
                    VolumeDialogControllerImpl.this.mCallbacks.onStateChanged(VolumeDialogControllerImpl.this.mState);
                }
                if (showUI) {
                    VolumeDialogControllerImpl.this.mCallbacks.onShowRequested(2);
                }
            }
        }

        public void onRemoteRemoved(MediaSession.Token token) {
            int stream = this.mRemoteStreams.get(token).intValue();
            VolumeDialogControllerImpl.this.mState.states.remove(stream);
            if (VolumeDialogControllerImpl.this.mState.activeStream == stream) {
                boolean unused = VolumeDialogControllerImpl.this.updateActiveStreamW(-1);
            }
            VolumeDialogControllerImpl.this.mCallbacks.onStateChanged(VolumeDialogControllerImpl.this.mState);
        }

        public void setStreamVolume(int stream, int level) {
            MediaSession.Token t = findToken(stream);
            if (t == null) {
                String access$500 = VolumeDialogControllerImpl.TAG;
                Log.w(access$500, "setStreamVolume: No token found for stream: " + stream);
                return;
            }
            VolumeDialogControllerImpl.this.mMediaSessions.setVolume(t, level);
        }

        private MediaSession.Token findToken(int stream) {
            for (Map.Entry<MediaSession.Token, Integer> entry : this.mRemoteStreams.entrySet()) {
                if (entry.getValue().equals(Integer.valueOf(stream))) {
                    return entry.getKey();
                }
            }
            return null;
        }
    }

    private final class Receiver extends BroadcastReceiver {
        private Receiver() {
        }

        public void init() {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.media.VOLUME_CHANGED_ACTION");
            filter.addAction("android.media.STREAM_DEVICES_CHANGED_ACTION");
            filter.addAction("android.media.RINGER_MODE_CHANGED");
            filter.addAction("android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION");
            filter.addAction("android.media.STREAM_MUTE_CHANGED_ACTION");
            filter.addAction("android.os.action.ACTION_EFFECTS_SUPPRESSOR_CHANGED");
            filter.addAction("android.intent.action.CONFIGURATION_CHANGED");
            filter.addAction("android.intent.action.SCREEN_OFF");
            filter.addAction("android.intent.action.CLOSE_SYSTEM_DIALOGS");
            VolumeDialogControllerImpl.this.mContext.registerReceiver(this, filter, null, VolumeDialogControllerImpl.this.mWorker);
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            boolean changed = false;
            if (action.equals("android.media.VOLUME_CHANGED_ACTION")) {
                int stream = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1);
                int level = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", -1);
                int oldLevel = intent.getIntExtra("android.media.EXTRA_PREV_VOLUME_STREAM_VALUE", -1);
                if (D.BUG) {
                    String access$500 = VolumeDialogControllerImpl.TAG;
                    Log.d(access$500, "onReceive VOLUME_CHANGED_ACTION stream=" + stream + " level=" + level + " oldLevel=" + oldLevel);
                }
                changed = VolumeDialogControllerImpl.this.updateStreamLevelW(stream, level);
            } else if (action.equals("android.media.STREAM_DEVICES_CHANGED_ACTION")) {
                int stream2 = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1);
                int devices = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_DEVICES", -1);
                int oldDevices = intent.getIntExtra("android.media.EXTRA_PREV_VOLUME_STREAM_DEVICES", -1);
                if (D.BUG) {
                    String access$5002 = VolumeDialogControllerImpl.TAG;
                    Log.d(access$5002, "onReceive STREAM_DEVICES_CHANGED_ACTION stream=" + stream2 + " devices=" + devices + " oldDevices=" + oldDevices);
                }
                Logger.i(VolumeDialogControllerImpl.TAG, "onReceive STREAM_DEVICES_CHANGED_ACTION");
                changed = VolumeDialogControllerImpl.this.checkRoutedToBluetoothW(stream2) | VolumeDialogControllerImpl.this.onVolumeChangedW(stream2, 0);
            } else if (action.equals("android.media.RINGER_MODE_CHANGED")) {
                int rm = intent.getIntExtra("android.media.EXTRA_RINGER_MODE", -1);
                if (D.BUG) {
                    String access$5003 = VolumeDialogControllerImpl.TAG;
                    Log.d(access$5003, "onReceive RINGER_MODE_CHANGED_ACTION rm=" + Util.ringerModeToString(rm));
                }
                changed = VolumeDialogControllerImpl.this.updateRingerModeExternalW(rm);
            } else if (action.equals("android.media.INTERNAL_RINGER_MODE_CHANGED_ACTION")) {
                int rm2 = intent.getIntExtra("android.media.EXTRA_RINGER_MODE", -1);
                if (D.BUG) {
                    String access$5004 = VolumeDialogControllerImpl.TAG;
                    Log.d(access$5004, "onReceive INTERNAL_RINGER_MODE_CHANGED_ACTION rm=" + Util.ringerModeToString(rm2));
                }
                changed = VolumeDialogControllerImpl.this.updateRingerModeInternalW(rm2);
            } else if (action.equals("android.media.STREAM_MUTE_CHANGED_ACTION")) {
                int stream3 = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1);
                boolean muted = intent.getBooleanExtra("android.media.EXTRA_STREAM_VOLUME_MUTED", false);
                if (D.BUG) {
                    String access$5005 = VolumeDialogControllerImpl.TAG;
                    Log.d(access$5005, "onReceive STREAM_MUTE_CHANGED_ACTION stream=" + stream3 + " muted=" + muted);
                }
                changed = VolumeDialogControllerImpl.this.updateStreamMuteW(stream3, muted);
            } else if (action.equals("android.os.action.ACTION_EFFECTS_SUPPRESSOR_CHANGED")) {
                if (D.BUG) {
                    Log.d(VolumeDialogControllerImpl.TAG, "onReceive ACTION_EFFECTS_SUPPRESSOR_CHANGED");
                }
                changed = VolumeDialogControllerImpl.this.updateEffectsSuppressorW(VolumeDialogControllerImpl.this.mNoMan.getEffectsSuppressor());
            } else if (action.equals("android.intent.action.CONFIGURATION_CHANGED")) {
                if (D.BUG) {
                    Log.d(VolumeDialogControllerImpl.TAG, "onReceive ACTION_CONFIGURATION_CHANGED");
                }
                VolumeDialogControllerImpl.this.mCallbacks.onConfigurationChanged();
            } else if (action.equals("android.intent.action.SCREEN_OFF")) {
                if (D.BUG) {
                    Log.d(VolumeDialogControllerImpl.TAG, "onReceive ACTION_SCREEN_OFF");
                }
                VolumeDialogControllerImpl.this.mCallbacks.onScreenOff();
            } else if (action.equals("android.intent.action.CLOSE_SYSTEM_DIALOGS")) {
                if (D.BUG) {
                    Log.d(VolumeDialogControllerImpl.TAG, "onReceive ACTION_CLOSE_SYSTEM_DIALOGS");
                }
                VolumeDialogControllerImpl.this.dismiss();
            }
            if (changed) {
                VolumeDialogControllerImpl.this.mCallbacks.onStateChanged(VolumeDialogControllerImpl.this.mState);
            }
        }
    }

    private final class SettingObserver extends ContentObserver {
        private final Uri ZEN_MODE_CONFIG_URI = Settings.Global.getUriFor("zen_mode_config_etag");
        private final Uri ZEN_MODE_URI = Settings.Global.getUriFor("zen_mode");

        public SettingObserver(Handler handler) {
            super(handler);
        }

        public void init() {
            VolumeDialogControllerImpl.this.mContext.getContentResolver().registerContentObserver(this.ZEN_MODE_URI, false, this);
            VolumeDialogControllerImpl.this.mContext.getContentResolver().registerContentObserver(this.ZEN_MODE_CONFIG_URI, false, this);
        }

        public void onChange(boolean selfChange, Uri uri) {
            boolean changed = false;
            if (this.ZEN_MODE_URI.equals(uri)) {
                changed = VolumeDialogControllerImpl.this.updateZenModeW();
            }
            if (changed) {
                VolumeDialogControllerImpl.this.mCallbacks.onStateChanged(VolumeDialogControllerImpl.this.mState);
            }
        }
    }

    public interface UserActivityListener {
        void onUserActivity();
    }

    private final class VC extends IVolumeController.Stub {
        private final String TAG;

        private VC() {
            this.TAG = VolumeDialogControllerImpl.TAG + ".VC";
        }

        public void displaySafeVolumeWarning(int flags) throws RemoteException {
            if (D.BUG) {
                String str = this.TAG;
                Log.d(str, "displaySafeVolumeWarning " + Util.audioManagerFlagsToString(flags));
            }
            if (!VolumeDialogControllerImpl.this.mDestroyed) {
                VolumeDialogControllerImpl.this.mWorker.obtainMessage(14, flags, 0).sendToTarget();
            }
        }

        public void volumeChanged(int streamType, int flags) throws RemoteException {
            if (D.BUG) {
                String str = this.TAG;
                Log.d(str, "volumeChanged " + AudioSystem.streamToString(streamType) + " " + Util.audioManagerFlagsToString(flags));
            }
            if (!VolumeDialogControllerImpl.this.mDestroyed) {
                VolumeDialogControllerImpl.this.mWorker.obtainMessage(1, streamType, flags).sendToTarget();
            }
        }

        public void masterMuteChanged(int flags) throws RemoteException {
            if (D.BUG) {
                Log.d(this.TAG, "masterMuteChanged");
            }
        }

        public void setLayoutDirection(int layoutDirection) throws RemoteException {
            if (D.BUG) {
                Log.d(this.TAG, "setLayoutDirection");
            }
            if (!VolumeDialogControllerImpl.this.mDestroyed) {
                VolumeDialogControllerImpl.this.mWorker.obtainMessage(8, layoutDirection, 0).sendToTarget();
            }
        }

        public void dismiss() throws RemoteException {
            if (D.BUG) {
                Log.d(this.TAG, "dismiss requested");
            }
            if (!VolumeDialogControllerImpl.this.mDestroyed) {
                VolumeDialogControllerImpl.this.mWorker.obtainMessage(2, 2, 0).sendToTarget();
                VolumeDialogControllerImpl.this.mWorker.sendEmptyMessage(2);
            }
        }

        public void setA11yMode(int mode) {
            if (D.BUG) {
                String str = this.TAG;
                Log.d(str, "setA11yMode to " + mode);
            }
            if (!VolumeDialogControllerImpl.this.mDestroyed) {
                switch (mode) {
                    case 0:
                        boolean unused = VolumeDialogControllerImpl.this.mShowA11yStream = false;
                        break;
                    case 1:
                        boolean unused2 = VolumeDialogControllerImpl.this.mShowA11yStream = true;
                        break;
                    default:
                        String str2 = this.TAG;
                        Log.e(str2, "Invalid accessibility mode " + mode);
                        break;
                }
                VolumeDialogControllerImpl.this.mWorker.obtainMessage(15, Boolean.valueOf(VolumeDialogControllerImpl.this.mShowA11yStream)).sendToTarget();
            }
        }
    }

    private final class W extends Handler {
        W(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            boolean z = false;
            switch (msg.what) {
                case 1:
                    boolean unused = VolumeDialogControllerImpl.this.onVolumeChangedW(msg.arg1, msg.arg2);
                    return;
                case 2:
                    VolumeDialogControllerImpl.this.onDismissRequestedW(msg.arg1);
                    return;
                case 3:
                    VolumeDialogControllerImpl.this.onGetStateW();
                    return;
                case 4:
                    VolumeDialogControllerImpl volumeDialogControllerImpl = VolumeDialogControllerImpl.this;
                    int i = msg.arg1;
                    if (msg.arg2 != 0) {
                        z = true;
                    }
                    volumeDialogControllerImpl.onSetRingerModeW(i, z);
                    return;
                case 5:
                    VolumeDialogControllerImpl.this.onSetZenModeW(msg.arg1);
                    return;
                case 6:
                    VolumeDialogControllerImpl.this.onSetExitConditionW((Condition) msg.obj);
                    return;
                case 7:
                    VolumeDialogControllerImpl volumeDialogControllerImpl2 = VolumeDialogControllerImpl.this;
                    int i2 = msg.arg1;
                    if (msg.arg2 != 0) {
                        z = true;
                    }
                    volumeDialogControllerImpl2.onSetStreamMuteW(i2, z);
                    return;
                case 8:
                    VolumeDialogControllerImpl.this.mCallbacks.onLayoutDirectionChanged(msg.arg1);
                    return;
                case 9:
                    VolumeDialogControllerImpl.this.mCallbacks.onConfigurationChanged();
                    return;
                case 10:
                    VolumeDialogControllerImpl.this.onSetStreamVolumeW(msg.arg1, msg.arg2);
                    return;
                case 11:
                    VolumeDialogControllerImpl.this.onSetActiveStreamW(msg.arg1);
                    return;
                case 12:
                    VolumeDialogControllerImpl volumeDialogControllerImpl3 = VolumeDialogControllerImpl.this;
                    if (msg.arg1 != 0) {
                        z = true;
                    }
                    volumeDialogControllerImpl3.onNotifyVisibleW(z);
                    return;
                case 13:
                    VolumeDialogControllerImpl.this.onUserActivityW();
                    return;
                case 14:
                    VolumeDialogControllerImpl.this.onShowSafetyWarningW(msg.arg1);
                    return;
                case 15:
                    VolumeDialogControllerImpl.this.onAccessibilityModeChanged((Boolean) msg.obj);
                    return;
                default:
                    return;
            }
        }
    }

    static {
        STREAMS.put(4, Integer.valueOf(R.string.stream_alarm));
        STREAMS.put(6, Integer.valueOf(R.string.stream_bluetooth_sco));
        STREAMS.put(8, Integer.valueOf(R.string.stream_dtmf));
        STREAMS.put(3, Integer.valueOf(R.string.stream_music));
        if (Build.VERSION.SDK_INT >= 26) {
            STREAMS.put(10, Integer.valueOf(R.string.stream_accessibility));
        }
        STREAMS.put(5, Integer.valueOf(R.string.stream_notification));
        STREAMS.put(2, Integer.valueOf(R.string.stream_ring));
        STREAMS.put(1, Integer.valueOf(R.string.stream_system));
        STREAMS.put(7, Integer.valueOf(R.string.stream_system_enforced));
        STREAMS.put(9, Integer.valueOf(R.string.stream_tts));
        STREAMS.put(0, Integer.valueOf(R.string.stream_voice_call));
    }

    public VolumeDialogControllerImpl(Context context) {
        boolean z = true;
        this.mShowDndTile = true;
        this.mVoiceAssistStreamType = -2;
        this.mVolumeController = new VC();
        this.mContext = context.getApplicationContext();
        Events.writeEvent(this.mContext, 5, new Object[0]);
        this.mWorkerThread = new HandlerThread(VolumeDialogControllerImpl.class.getSimpleName());
        this.mWorkerThread.start();
        this.mWorker = new W(this.mWorkerThread.getLooper());
        this.mMediaSessions = createMediaSessions(this.mContext, this.mWorkerThread.getLooper(), this.mMediaSessionsCallbacksW);
        this.mAudio = (AudioManager) this.mContext.getSystemService("audio");
        this.mNoMan = (NotificationManager) this.mContext.getSystemService("notification");
        this.mObserver = new SettingObserver(this.mWorker);
        this.mObserver.init();
        this.mReceiver.init();
        this.mVibrator = (Vibrator) this.mContext.getSystemService("vibrator");
        this.mHasVibrator = (this.mVibrator == null || !this.mVibrator.hasVibrator()) ? false : z;
        if (getVoiceAssistStreamType() > 0) {
            STREAMS.put(Integer.valueOf(getVoiceAssistStreamType()), Integer.valueOf(R.string.stream_voice_assist));
        }
    }

    public int getVoiceAssistStreamType() {
        if (this.mVoiceAssistStreamType == -2) {
            this.mVoiceAssistStreamType = AudioServiceInjector.getVoiceAssistNum();
        }
        return this.mVoiceAssistStreamType;
    }

    public AudioManager getAudioManager() {
        return this.mAudio;
    }

    public void dismiss() {
        this.mCallbacks.onDismissRequested(2);
    }

    /* access modifiers changed from: protected */
    public void setVolumeController() {
        try {
            this.mAudio.setVolumeController(this.mVolumeController);
        } catch (SecurityException e) {
            Log.w(TAG, "Unable to set the volume controller", e);
        }
    }

    /* access modifiers changed from: protected */
    public void setAudioManagerStreamVolume(int stream, int level, int flag) {
        this.mAudio.setStreamVolume(stream, level, flag);
    }

    /* access modifiers changed from: protected */
    public int getAudioManagerStreamVolume(int stream) {
        return this.mAudio.getLastAudibleStreamVolume(stream);
    }

    /* access modifiers changed from: protected */
    public int getAudioManagerStreamMaxVolume(int stream) {
        return this.mAudio.getStreamMaxVolume(stream);
    }

    /* access modifiers changed from: protected */
    public int getAudioManagerStreamMinVolume(int stream) {
        return AudioManagerCompat.getStreamMinVolume(this.mAudio, stream);
    }

    public void register() {
        setVolumeController();
        setVolumePolicy(this.mVolumePolicy);
        showDndTile(this.mShowDndTile);
        try {
            this.mMediaSessions.init();
        } catch (SecurityException e) {
            Log.w(TAG, "No access to media sessions", e);
        }
    }

    public void setVolumePolicy(VolumePolicy policy) {
        this.mVolumePolicy = policy;
        if (this.mVolumePolicy != null) {
            try {
                this.mAudio.setVolumePolicy(this.mVolumePolicy);
            } catch (NoSuchMethodError e) {
                Log.w(TAG, "No volume policy api");
            }
        }
    }

    /* access modifiers changed from: protected */
    public MediaSessions createMediaSessions(Context context, Looper looper, MediaSessions.Callbacks callbacks) {
        return new MediaSessions(context, looper, callbacks);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println(VolumeDialogControllerImpl.class.getSimpleName() + " state:");
        pw.print("  mDestroyed: ");
        pw.println(this.mDestroyed);
        pw.print("  mVolumePolicy: ");
        pw.println(this.mVolumePolicy);
        pw.print("  mState: ");
        pw.println(this.mState.toString(4));
        pw.print("  mShowDndTile: ");
        pw.println(this.mShowDndTile);
        pw.print("  mHasVibrator: ");
        pw.println(this.mHasVibrator);
        pw.print("  mRemoteStreams: ");
        pw.println(this.mMediaSessionsCallbacksW.mRemoteStreams.values());
        pw.print("  mShowA11yStream: ");
        pw.println(this.mShowA11yStream);
        pw.println();
        this.mMediaSessions.dump(pw);
    }

    public void addCallback(VolumeDialogController.Callbacks callback, Handler handler) {
        this.mCallbacks.add(callback, handler);
    }

    public void setUserActivityListener(UserActivityListener listener) {
        if (!this.mDestroyed) {
            synchronized (this) {
                this.mUserActivityListener = listener;
            }
        }
    }

    public void removeCallback(VolumeDialogController.Callbacks callback) {
        this.mCallbacks.remove(callback);
    }

    public void getState() {
        if (!this.mDestroyed) {
            this.mWorker.sendEmptyMessage(3);
        }
    }

    public void notifyVisible(boolean visible) {
        if (!this.mDestroyed) {
            this.mWorker.obtainMessage(12, visible, 0).sendToTarget();
        }
    }

    public void userActivity() {
        if (!this.mDestroyed) {
            this.mWorker.removeMessages(13);
            this.mWorker.sendEmptyMessage(13);
        }
    }

    public void setRingerMode(int value, boolean external) {
        if (!this.mDestroyed) {
            this.mWorker.obtainMessage(4, value, external).sendToTarget();
        }
    }

    public void setStreamVolume(int stream, int level) {
        if (!this.mDestroyed) {
            this.mWorker.obtainMessage(10, stream, level).sendToTarget();
        }
    }

    public void setActiveStream(int stream) {
        if (!this.mDestroyed) {
            this.mWorker.obtainMessage(11, stream, 0).sendToTarget();
        }
    }

    public void vibrate() {
        if (this.mHasVibrator) {
            this.mVibrator.vibrate(50);
        }
    }

    public boolean hasVibrator() {
        return this.mHasVibrator;
    }

    /* access modifiers changed from: private */
    public void onNotifyVisibleW(boolean visible) {
        if (!this.mDestroyed) {
            this.mAudio.notifyVolumeControllerVisible(this.mVolumeController, visible);
            if (!visible && updateActiveStreamW(-1)) {
                this.mCallbacks.onStateChanged(this.mState);
            }
        }
    }

    /* access modifiers changed from: private */
    public void onUserActivityW() {
        synchronized (this) {
            if (this.mUserActivityListener != null) {
                this.mUserActivityListener.onUserActivity();
            }
        }
    }

    /* access modifiers changed from: private */
    public void onShowSafetyWarningW(int flags) {
        this.mCallbacks.onShowSafetyWarning(flags);
    }

    /* access modifiers changed from: private */
    public void onAccessibilityModeChanged(Boolean showA11yStream) {
        this.mCallbacks.onAccessibilityModeChanged(showA11yStream);
    }

    /* access modifiers changed from: private */
    public boolean checkRoutedToBluetoothW(int stream) {
        if (stream != 3) {
            return false;
        }
        return false | updateStreamRoutedToBluetoothW(stream, (this.mAudio.getDevicesForStream(3) & 896) != 0);
    }

    /* access modifiers changed from: private */
    public boolean onVolumeChangedW(int stream, int flags) {
        boolean showUI = (flags & 1) != 0;
        boolean fromKey = (flags & 4096) != 0;
        boolean showVibrateHint = (flags & 2048) != 0;
        boolean showSilentHint = (flags & 128) != 0;
        boolean changed = false;
        if (showUI) {
            changed = false | updateActiveStreamW(stream);
        }
        int lastAudibleStreamVolume = getAudioManagerStreamVolume(stream);
        if (D.BUG) {
            String str = TAG;
            Log.d(str, "onVolumeChangedW stream:" + stream + " lastAudibleStreamVolume:" + lastAudibleStreamVolume);
        }
        boolean changed2 = changed | updateStreamLevelW(stream, lastAudibleStreamVolume) | checkRoutedToBluetoothW(showUI ? 3 : stream);
        if (D.BUG) {
            String str2 = TAG;
            Log.d(str2, "onVolumeChangedW showUI:" + showUI + " fromKey:" + fromKey + " showVibrateHint:" + showVibrateHint + " showSilentHint:" + showSilentHint + " stream:" + stream);
        }
        if (changed2) {
            this.mCallbacks.onStateChanged(this.mState);
        }
        if (showUI) {
            String str3 = TAG;
            Logger.i(str3, "onVolumeChangedW showUI activeStream:" + this.mState.activeStream + " fromKey:" + fromKey);
            this.mCallbacks.onShowRequested(1);
        }
        if (showVibrateHint) {
            this.mCallbacks.onShowVibrateHint();
        }
        if (showSilentHint) {
            this.mCallbacks.onShowSilentHint();
        }
        if (changed2 && fromKey) {
            Events.writeEvent(this.mContext, 4, Integer.valueOf(stream), Integer.valueOf(lastAudibleStreamVolume));
        }
        this.mCallbacks.onVolumeChanged(stream, fromKey);
        return changed2;
    }

    /* access modifiers changed from: private */
    public boolean updateActiveStreamW(int activeStream) {
        if (activeStream == this.mState.activeStream) {
            return false;
        }
        this.mState.activeStream = activeStream;
        Events.writeEvent(this.mContext, 2, Integer.valueOf(activeStream));
        if (D.BUG) {
            String str = TAG;
            Log.d(str, "updateActiveStreamW " + activeStream);
        }
        int s = activeStream < 100 ? activeStream : -1;
        if (D.BUG) {
            String str2 = TAG;
            Log.d(str2, "forceVolumeControlStream " + s);
        }
        this.mAudio.forceVolumeControlStream(s);
        return true;
    }

    /* access modifiers changed from: private */
    public VolumeDialogController.StreamState streamStateW(int stream) {
        VolumeDialogController.StreamState ss = this.mState.states.get(stream);
        if (ss != null) {
            return ss;
        }
        VolumeDialogController.StreamState ss2 = new VolumeDialogController.StreamState();
        this.mState.states.put(stream, ss2);
        return ss2;
    }

    /* access modifiers changed from: private */
    public void onGetStateW() {
        if (D.BUG) {
            String str = TAG;
            Log.d(str, "onGetStateW STREAMS" + STREAMS.size());
        }
        for (Integer intValue : STREAMS.keySet()) {
            int stream = intValue.intValue();
            updateStreamLevelW(stream, getAudioManagerStreamVolume(stream));
            streamStateW(stream).levelMin = getAudioManagerStreamMinVolume(stream);
            streamStateW(stream).levelMax = getAudioManagerStreamMaxVolume(stream);
            updateStreamMuteW(stream, this.mAudio.isStreamMute(stream));
            VolumeDialogController.StreamState ss = streamStateW(stream);
            ss.muteSupported = this.mAudio.isStreamAffectedByMute(stream);
            ss.nameRes = STREAMS.get(Integer.valueOf(stream)).intValue();
            if (D.BUG) {
                String str2 = TAG;
                Log.d(str2, "onGetStateW stream:" + stream + " levelMax:" + streamStateW(stream).levelMax + " name:" + this.mContext.getResources().getString(STREAMS.get(Integer.valueOf(stream)).intValue()) + " mute:" + ss.muted);
            }
            checkRoutedToBluetoothW(stream);
        }
        updateRingerModeExternalW(this.mAudio.getRingerMode());
        updateZenModeW();
        updateEffectsSuppressorW(this.mNoMan.getEffectsSuppressor());
        this.mCallbacks.onStateChanged(this.mState);
    }

    private boolean updateStreamRoutedToBluetoothW(int stream, boolean routedToBluetooth) {
        VolumeDialogController.StreamState ss = streamStateW(stream);
        if (ss.routedToBluetooth == routedToBluetooth) {
            return false;
        }
        ss.routedToBluetooth = routedToBluetooth;
        if (D.BUG) {
            String str = TAG;
            Log.d(str, "updateStreamRoutedToBluetoothW stream=" + stream + " routedToBluetooth=" + routedToBluetooth);
        }
        return true;
    }

    /* access modifiers changed from: private */
    public boolean updateStreamLevelW(int stream, int level) {
        VolumeDialogController.StreamState ss = streamStateW(stream);
        if (ss.level == level) {
            return false;
        }
        String str = TAG;
        Logger.i(str, "updateStreamLevelW stream:" + stream + " ss.level:" + ss.level + " level:" + level + " ss.levelMax:" + ss.levelMax + " mute:" + ss.muted);
        ss.level = level;
        if (isLogWorthy(stream)) {
            Events.writeEvent(this.mContext, 10, Integer.valueOf(stream), Integer.valueOf(level));
        }
        return true;
    }

    private static boolean isLogWorthy(int stream) {
        if (stream != 6) {
            switch (stream) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                    break;
                default:
                    return false;
            }
        }
        return true;
    }

    /* access modifiers changed from: private */
    public boolean updateStreamMuteW(int stream, boolean muted) {
        VolumeDialogController.StreamState ss = streamStateW(stream);
        if (ss.muted == muted) {
            return false;
        }
        ss.muted = muted;
        if (isLogWorthy(stream)) {
            Events.writeEvent(this.mContext, 15, Integer.valueOf(stream), Boolean.valueOf(muted));
        }
        if (muted && isRinger(stream)) {
            updateRingerModeInternalW(this.mAudio.getRingerModeInternal());
        }
        return true;
    }

    private static boolean isRinger(int stream) {
        return stream == 2 || stream == 5;
    }

    /* access modifiers changed from: private */
    public boolean updateEffectsSuppressorW(ComponentName effectsSuppressor) {
        if (Objects.equals(this.mState.effectsSuppressor, effectsSuppressor)) {
            return false;
        }
        this.mState.effectsSuppressor = effectsSuppressor;
        this.mState.effectsSuppressorName = getApplicationName(this.mContext, this.mState.effectsSuppressor);
        Events.writeEvent(this.mContext, 14, this.mState.effectsSuppressor, this.mState.effectsSuppressorName);
        return true;
    }

    private static String getApplicationName(Context context, ComponentName component) {
        if (component == null) {
            return null;
        }
        PackageManager pm = context.getPackageManager();
        String pkg = component.getPackageName();
        try {
            String rt = Objects.toString(pm.getApplicationInfo(pkg, 0).loadLabel(pm), "").trim();
            if (rt.length() > 0) {
                return rt;
            }
            return pkg;
        } catch (PackageManager.NameNotFoundException e) {
        }
    }

    /* access modifiers changed from: private */
    public boolean updateZenModeW() {
        int zen = Settings.Global.getInt(this.mContext.getContentResolver(), "zen_mode", 0);
        if (this.mState.zenMode == zen) {
            return false;
        }
        this.mState.zenMode = zen;
        Events.writeEvent(this.mContext, 13, Integer.valueOf(zen));
        return true;
    }

    /* access modifiers changed from: private */
    public boolean updateRingerModeExternalW(int rm) {
        if (rm == this.mState.ringerModeExternal) {
            return false;
        }
        this.mState.ringerModeExternal = rm;
        Events.writeEvent(this.mContext, 12, Integer.valueOf(rm));
        return true;
    }

    /* access modifiers changed from: private */
    public boolean updateRingerModeInternalW(int rm) {
        if (rm == this.mState.ringerModeInternal) {
            return false;
        }
        this.mState.ringerModeInternal = rm;
        Events.writeEvent(this.mContext, 11, Integer.valueOf(rm));
        return true;
    }

    /* access modifiers changed from: private */
    public void onSetRingerModeW(int mode, boolean external) {
        if (external) {
            this.mAudio.setRingerMode(mode);
        } else {
            this.mAudio.setRingerModeInternal(mode);
        }
    }

    /* access modifiers changed from: private */
    public void onSetStreamMuteW(int stream, boolean mute) {
        int i;
        AudioManager audioManager = this.mAudio;
        if (mute) {
            i = -100;
        } else {
            i = 100;
        }
        audioManager.adjustStreamVolume(stream, i, 0);
    }

    /* access modifiers changed from: private */
    public void onSetStreamVolumeW(int stream, int level) {
        if (D.BUG) {
            String str = TAG;
            Log.d(str, "onSetStreamVolume " + stream + " level=" + level);
        }
        if (stream >= 100) {
            this.mMediaSessionsCallbacksW.setStreamVolume(stream, level);
        } else {
            setAudioManagerStreamVolume(stream, level, 0);
        }
    }

    /* access modifiers changed from: private */
    public void onSetActiveStreamW(int stream) {
        if (updateActiveStreamW(stream)) {
            this.mCallbacks.onStateChanged(this.mState);
        }
    }

    /* access modifiers changed from: private */
    public void onSetExitConditionW(Condition condition) {
        this.mNoMan.setZenMode(this.mState.zenMode, condition != null ? condition.id : null, TAG);
    }

    /* access modifiers changed from: private */
    public void onSetZenModeW(int mode) {
        if (D.BUG) {
            String str = TAG;
            Log.d(str, "onSetZenModeW " + mode);
        }
        this.mNoMan.setZenMode(mode, null, TAG);
    }

    /* access modifiers changed from: private */
    public void onDismissRequestedW(int reason) {
        this.mCallbacks.onDismissRequested(reason);
    }

    public void showDndTile(boolean visible) {
        if (D.BUG) {
            Log.d(TAG, "showDndTile");
        }
    }
}
