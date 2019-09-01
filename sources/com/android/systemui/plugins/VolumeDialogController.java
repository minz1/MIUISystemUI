package com.android.systemui.plugins;

import android.content.ComponentName;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.os.Handler;
import android.util.SparseArray;
import com.android.systemui.plugins.annotations.ProvidesInterface;

@ProvidesInterface(version = 1)
public interface VolumeDialogController {
    public static final int VERSION = 1;

    @ProvidesInterface(version = 1)
    public interface Callbacks {
        public static final int VERSION = 1;

        void onAccessibilityModeChanged(Boolean bool);

        void onConfigurationChanged();

        void onDismissRequested(int i);

        void onLayoutDirectionChanged(int i);

        void onScreenOff();

        void onShowRequested(int i);

        void onShowSafetyWarning(int i);

        void onShowSilentHint();

        void onShowVibrateHint();

        void onStateChanged(State state);

        void onVolumeChanged(int i, boolean z);
    }

    @ProvidesInterface(version = 1)
    public static final class State {
        public static int NO_ACTIVE_STREAM = -1;
        public static final int VERSION = 1;
        public int activeStream = NO_ACTIVE_STREAM;
        public ComponentName effectsSuppressor;
        public String effectsSuppressorName;
        public int ringerModeExternal;
        public int ringerModeInternal;
        public final SparseArray<StreamState> states = new SparseArray<>();
        public int zenMode;

        public State copy() {
            State rt = new State();
            for (int i = 0; i < this.states.size(); i++) {
                rt.states.put(this.states.keyAt(i), this.states.valueAt(i).copy());
            }
            rt.ringerModeExternal = this.ringerModeExternal;
            rt.ringerModeInternal = this.ringerModeInternal;
            rt.zenMode = this.zenMode;
            if (this.effectsSuppressor != null) {
                rt.effectsSuppressor = this.effectsSuppressor.clone();
            }
            rt.effectsSuppressorName = this.effectsSuppressorName;
            rt.activeStream = this.activeStream;
            return rt;
        }

        public String toString() {
            return toString(0);
        }

        public String toString(int indent) {
            StringBuilder sb = new StringBuilder("{");
            if (indent > 0) {
                sep(sb, indent);
            }
            for (int i = 0; i < this.states.size(); i++) {
                if (i > 0) {
                    sep(sb, indent);
                }
                int stream = this.states.keyAt(i);
                StreamState ss = this.states.valueAt(i);
                sb.append(AudioSystem.streamToString(stream));
                sb.append(":");
                sb.append(ss.level);
                sb.append('[');
                sb.append(ss.levelMin);
                sb.append("..");
                sb.append(ss.levelMax);
                sb.append(']');
                if (ss.muted) {
                    sb.append(" [MUTED]");
                }
                if (ss.dynamic) {
                    sb.append(" [DYNAMIC]");
                }
            }
            sep(sb, indent);
            sb.append("ringerModeExternal:");
            sb.append(this.ringerModeExternal);
            sep(sb, indent);
            sb.append("ringerModeInternal:");
            sb.append(this.ringerModeInternal);
            sep(sb, indent);
            sb.append("zenMode:");
            sb.append(this.zenMode);
            sep(sb, indent);
            sb.append("effectsSuppressor:");
            sb.append(this.effectsSuppressor);
            sep(sb, indent);
            sb.append("effectsSuppressorName:");
            sb.append(this.effectsSuppressorName);
            sep(sb, indent);
            sb.append("activeStream:");
            sb.append(this.activeStream);
            if (indent > 0) {
                sep(sb, indent);
            }
            sb.append('}');
            return sb.toString();
        }

        private static void sep(StringBuilder sb, int indent) {
            if (indent > 0) {
                sb.append(10);
                for (int i = 0; i < indent; i++) {
                    sb.append(' ');
                }
                return;
            }
            sb.append(',');
        }
    }

    @ProvidesInterface(version = 1)
    public static final class StreamState {
        public static final int VERSION = 1;
        public boolean dynamic;
        public int level;
        public int levelMax;
        public int levelMin;
        public boolean muteSupported;
        public boolean muted;
        public int nameRes;
        public String remoteLabel;
        public boolean routedToBluetooth;

        public StreamState copy() {
            StreamState rt = new StreamState();
            rt.dynamic = this.dynamic;
            rt.level = this.level;
            rt.levelMin = this.levelMin;
            rt.levelMax = this.levelMax;
            rt.muted = this.muted;
            rt.muteSupported = this.muteSupported;
            rt.nameRes = this.nameRes;
            rt.remoteLabel = this.remoteLabel;
            rt.routedToBluetooth = this.routedToBluetooth;
            return rt;
        }
    }

    void addCallback(Callbacks callbacks, Handler handler);

    AudioManager getAudioManager();

    void getState();

    int getVoiceAssistStreamType();

    boolean hasVibrator();

    void notifyVisible(boolean z);

    void removeCallback(Callbacks callbacks);

    void setActiveStream(int i);

    void setRingerMode(int i, boolean z);

    void setStreamVolume(int i, int i2);

    void userActivity();

    void vibrate();
}
