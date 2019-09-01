package com.android.systemui.miui.statusbar.phone;

import android.content.Context;
import android.graphics.Rect;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.ViewGroup;
import com.android.systemui.R;
import com.android.systemui.statusbar.phone.StatusBar;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public class MiuiStatusBarPromptController implements IMiuiStatusBarPrompt {
    private WeakReference<Handler> mHandler = new WeakReference<>(null);
    private IMiuiStatusBarPrompt mLastClickablePrompt;
    private Map<String, OnPromptStateChangedListener> mPromptStateChangedListeners = new HashMap();
    private int mRecorderState;
    private long mRecordingPausedTime;
    private long mRecordingStartTime;
    private int mSilentModeDefault = -1;
    private int mStatusBarModeState = 0;
    private Map<String, MiuiStatusBarPromptImpl> mStatusBarPrompts = new HashMap();
    private int mTopStatusBarModeState = 0;

    public interface OnPromptStateChangedListener {
        void onPromptStateChanged(boolean z, int i);
    }

    public void setHandler(Handler handler) {
        this.mHandler = new WeakReference<>(handler);
    }

    public void addStatusBarPrompt(String tag, StatusBar statusBar, ViewGroup parent, int disableFlags, OnPromptStateChangedListener listener) {
        this.mStatusBarPrompts.put(tag, new MiuiStatusBarPromptImpl(statusBar, parent, disableFlags));
        addPromptStateChangedListener(tag, listener);
    }

    public void addPromptStateChangedListener(String tag, OnPromptStateChangedListener listener) {
        this.mPromptStateChangedListeners.put(tag, listener);
    }

    public void setPromptSosTypeImage(String tag) {
        this.mStatusBarPrompts.get(tag).setSosTypeImage();
    }

    public void showReturnToRecorderView(boolean show) {
        for (MiuiStatusBarPromptImpl statusBarPrompt : this.mStatusBarPrompts.values()) {
            statusBarPrompt.showReturnToRecorderView(show);
        }
    }

    public void hideReturnToRecorderView() {
        clearState(4);
        for (MiuiStatusBarPromptImpl statusBarPrompt : this.mStatusBarPrompts.values()) {
            statusBarPrompt.hideReturnToRecorderView();
        }
    }

    public void showReturnToRecorderView(String title, boolean enable, long duration) {
        setState(4);
        for (MiuiStatusBarPromptImpl statusBarPrompt : this.mStatusBarPrompts.values()) {
            statusBarPrompt.showReturnToRecorderView(title, enable, duration);
        }
    }

    public void showReturnToSafeBar(boolean show) {
        for (MiuiStatusBarPromptImpl statusBarPrompt : this.mStatusBarPrompts.values()) {
            statusBarPrompt.showReturnToSafeBar(show);
        }
    }

    public void showSafePayStatusBar(int state, Bundle ext) {
        setState(8);
        for (MiuiStatusBarPromptImpl statusBarPrompt : this.mStatusBarPrompts.values()) {
            statusBarPrompt.showSafePayStatusBar(state, ext);
        }
    }

    public void hideSafePayStatusBar() {
        clearState(8);
        for (MiuiStatusBarPromptImpl statusBarPrompt : this.mStatusBarPrompts.values()) {
            statusBarPrompt.hideSafePayStatusBar();
        }
    }

    public void showSosStatusBar() {
        setState(16);
        for (MiuiStatusBarPromptImpl statusBarPrompt : this.mStatusBarPrompts.values()) {
            statusBarPrompt.showSosStatusBar();
        }
    }

    public void hideSosStatusBar() {
        clearState(16);
        for (MiuiStatusBarPromptImpl statusBarPrompt : this.mStatusBarPrompts.values()) {
            statusBarPrompt.hideSosStatusBar();
        }
    }

    public void showReturnToSosBar(boolean show) {
        for (MiuiStatusBarPromptImpl statusBarPrompt : this.mStatusBarPrompts.values()) {
            statusBarPrompt.showReturnToSosBar(show);
        }
    }

    public void updateSosImageDark(boolean isDark, Rect area, float darkIntensity) {
        for (MiuiStatusBarPromptImpl statusBarPrompt : this.mStatusBarPrompts.values()) {
            statusBarPrompt.updateSosImageDark(isDark, area, darkIntensity);
        }
    }

    public void showReturnToDriveMode(boolean show) {
        for (MiuiStatusBarPromptImpl statusBarPrompt : this.mStatusBarPrompts.values()) {
            statusBarPrompt.showReturnToDriveMode(show);
        }
    }

    public void showReturnToDriveModeView(boolean show, boolean mask_mode) {
        if (show) {
            setState(2);
        } else {
            clearState(2);
        }
        for (MiuiStatusBarPromptImpl statusBarPrompt : this.mStatusBarPrompts.values()) {
            statusBarPrompt.showReturnToDriveModeView(show, mask_mode);
        }
    }

    public void showReturnToInCall(boolean show) {
        for (MiuiStatusBarPromptImpl statusBarPrompt : this.mStatusBarPrompts.values()) {
            statusBarPrompt.showReturnToInCall(show);
        }
    }

    public void showReturnToInCallScreenButton(String state, long baseTime) {
        setState(32);
        for (MiuiStatusBarPromptImpl statusBarPrompt : this.mStatusBarPrompts.values()) {
            statusBarPrompt.showReturnToInCallScreenButton(state, baseTime);
        }
    }

    public void hideReturnToInCallScreenButton() {
        clearState(32);
        for (MiuiStatusBarPromptImpl statusBarPrompt : this.mStatusBarPrompts.values()) {
            statusBarPrompt.hideReturnToInCallScreenButton();
        }
    }

    public void makeReturnToInCallScreenButtonVisible() {
        for (MiuiStatusBarPromptImpl statusBarPrompt : this.mStatusBarPrompts.values()) {
            statusBarPrompt.makeReturnToInCallScreenButtonVisible();
        }
    }

    public void makeReturnToInCallScreenButtonGone() {
        for (MiuiStatusBarPromptImpl statusBarPrompt : this.mStatusBarPrompts.values()) {
            statusBarPrompt.makeReturnToInCallScreenButtonGone();
        }
    }

    public void showReturnToMulti(boolean show) {
        for (MiuiStatusBarPromptImpl statusBarPrompt : this.mStatusBarPrompts.values()) {
            statusBarPrompt.showReturnToMulti(show);
        }
    }

    public boolean blockClickAction() {
        for (IMiuiStatusBarPrompt statusBarPrompt : this.mStatusBarPrompts.values()) {
            if (statusBarPrompt.blockClickAction()) {
                this.mLastClickablePrompt = statusBarPrompt;
                return true;
            }
        }
        return false;
    }

    public void handleClickAction() {
        if (this.mLastClickablePrompt != null) {
            this.mLastClickablePrompt.handleClickAction();
            this.mLastClickablePrompt = null;
        }
    }

    public Context getContext() {
        for (MiuiStatusBarPromptImpl statusBarPrompt : this.mStatusBarPrompts.values()) {
            Context context = statusBarPrompt.getContext();
            if (context != null) {
                return context;
            }
        }
        return null;
    }

    public void setStatus(int what, String action, Bundle ext) {
        if ("com.miui.app.ExtraStatusBarManager.action_status_recorder".equals(action)) {
            switch (what) {
                case 0:
                    this.mRecorderState = 0;
                    hideReturnToRecorderView();
                    return;
                case 1:
                    long d = ext.getLong("com.miui.app.ExtraStatusBarManager.extra_recorder_duration", 0);
                    this.mRecordingPausedTime = SystemClock.elapsedRealtime();
                    this.mRecordingStartTime = this.mRecordingPausedTime - d;
                    String title = ext.getString("com.miui.app.ExtraStatusBarManager.extra_recorder_title");
                    boolean enable = ext.getBoolean("com.miui.app.ExtraStatusBarManager.extra_recorder_timer_on_off", false);
                    this.mRecorderState = enable ? 1 : 2;
                    showReturnToRecorderView(title, enable, d);
                    return;
                case 2:
                    boolean changedByUser = ext.getBoolean("com.miui.app.ExtraStatusBarManager.extra_recorder_silent_mode_changed_by_user");
                    boolean enterSilent = ext.getBoolean("com.miui.app.ExtraStatusBarManager.extra_recorder_enter_silent_mode");
                    if (changedByUser) {
                        this.mSilentModeDefault = -1;
                        return;
                    } else {
                        setSilenceWhenRecording(enterSilent);
                        return;
                    }
                case 3:
                    setSilenceWhenRecording(false);
                    this.mRecorderState = 0;
                    hideReturnToRecorderView();
                    return;
                default:
                    return;
            }
        } else if ("com.miui.app.ExtraStatusBarManager.action_status_safepay".equals(action)) {
            if (what != 0) {
                switch (what) {
                    case 2:
                    case 3:
                    case 4:
                        showSafePayStatusBar(what, ext);
                        return;
                    default:
                        return;
                }
            } else {
                hideSafePayStatusBar();
            }
        } else if ("com.miui.app.ExtraStatusBarManager.action_status_sos".equals(action)) {
            switch (what) {
                case 0:
                    hideSosStatusBar();
                    return;
                case 1:
                    showSosStatusBar();
                    return;
                default:
                    return;
            }
        }
    }

    private void setSilenceWhenRecording(boolean recordingSilent) {
        if (getContext() != null) {
            AudioManager am = (AudioManager) getContext().getSystemService("audio");
            int silentMode = am.getRingerMode();
            if (recordingSilent) {
                boolean isSilentDefault = silentMode != 2;
                this.mSilentModeDefault = silentMode;
                if (recordingSilent != isSilentDefault) {
                    am.setRingerMode(0);
                }
            } else if (this.mSilentModeDefault != -1) {
                if (this.mSilentModeDefault != silentMode) {
                    am.setRingerMode(this.mSilentModeDefault);
                }
                this.mSilentModeDefault = -1;
            }
        }
    }

    public void dealWithRecordState() {
        if (getContext() != null && this.mRecorderState != 0) {
            if (this.mRecorderState == 1) {
                showReturnToRecorderView(getContext().getString(R.string.status_bar_recording_back), true, SystemClock.elapsedRealtime() - this.mRecordingStartTime);
            } else if (this.mRecorderState == 2) {
                showReturnToRecorderView(getContext().getString(R.string.status_bar_recording_pause), false, this.mRecordingPausedTime - this.mRecordingStartTime);
            }
        }
    }

    /* access modifiers changed from: private */
    public void showPrompt() {
        int topState = getTopPriorityState();
        boolean z = false;
        showReturnToRecorderView(topState == 4);
        showReturnToSafeBar(topState == 8);
        showReturnToDriveMode(topState == 2);
        showReturnToInCall(topState == 32);
        showReturnToMulti(topState == 1);
        showReturnToSosBar(topState == 16);
        boolean isNormalMode = topState == 0;
        if (this.mTopStatusBarModeState == 0) {
            z = true;
        }
        if (z ^ isNormalMode) {
            for (OnPromptStateChangedListener listener : this.mPromptStateChangedListeners.values()) {
                listener.onPromptStateChanged(isNormalMode, topState);
            }
        }
        this.mTopStatusBarModeState = topState;
    }

    private void dispatchShowPrompt() {
        if (this.mHandler.get() != null) {
            ((Handler) this.mHandler.get()).post(new Runnable() {
                public void run() {
                    MiuiStatusBarPromptController.this.showPrompt();
                }
            });
        } else if (Looper.myLooper() == Looper.getMainLooper()) {
            showPrompt();
        } else {
            Log.e("MiuiStatusBarPrompt", "dispatchShowPrompt abandoned, not in main thread");
        }
    }

    public void setState(int state) {
        if (!isShowingState(state)) {
            this.mStatusBarModeState += state;
        }
        dispatchShowPrompt();
    }

    public void clearState(int state) {
        if (isShowingState(state)) {
            this.mStatusBarModeState -= state;
        }
        dispatchShowPrompt();
    }

    public boolean isShowingState(int state) {
        return (this.mStatusBarModeState & state) == state;
    }

    public int getStatusBarModeState() {
        return this.mStatusBarModeState;
    }

    public void forceRefreshRecorder() {
        if (this.mHandler.get() != null) {
            ((Handler) this.mHandler.get()).post(new Runnable() {
                public void run() {
                    MiuiStatusBarPromptController.this.dealWithRecordState();
                }
            });
        }
    }

    private int getTopPriorityState() {
        for (int i = 32; i > 0; i >>= 1) {
            if (isShowingState(i)) {
                return i;
            }
        }
        return 0;
    }
}
