package com.android.systemui.pip.phone;

import android.app.IActivityManager;
import android.app.PendingIntent;
import android.app.RemoteAction;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Icon;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import com.android.systemui.R;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class PipMediaController {
    private final IActivityManager mActivityManager;
    private final Context mContext;
    private ArrayList<ActionListener> mListeners = new ArrayList<>();
    /* access modifiers changed from: private */
    public MediaController mMediaController;
    private final MediaSessionManager mMediaSessionManager;
    private RemoteAction mNextAction;
    private RemoteAction mPauseAction;
    private RemoteAction mPlayAction;
    private BroadcastReceiver mPlayPauseActionReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("com.android.systemui.pip.phone.PLAY")) {
                PipMediaController.this.mMediaController.getTransportControls().play();
            } else if (action.equals("com.android.systemui.pip.phone.PAUSE")) {
                PipMediaController.this.mMediaController.getTransportControls().pause();
            } else if (action.equals("com.android.systemui.pip.phone.NEXT")) {
                PipMediaController.this.mMediaController.getTransportControls().skipToNext();
            } else if (action.equals("com.android.systemui.pip.phone.PREV")) {
                PipMediaController.this.mMediaController.getTransportControls().skipToPrevious();
            }
        }
    };
    private MediaController.Callback mPlaybackChangedListener = new MediaController.Callback() {
        public void onPlaybackStateChanged(PlaybackState state) {
            PipMediaController.this.notifyActionsChanged();
        }
    };
    private RemoteAction mPrevAction;

    public interface ActionListener {
        void onMediaActionsChanged(List<RemoteAction> list);
    }

    public PipMediaController(Context context, IActivityManager activityManager) {
        this.mContext = context;
        this.mActivityManager = activityManager;
        IntentFilter mediaControlFilter = new IntentFilter();
        mediaControlFilter.addAction("com.android.systemui.pip.phone.PLAY");
        mediaControlFilter.addAction("com.android.systemui.pip.phone.PAUSE");
        mediaControlFilter.addAction("com.android.systemui.pip.phone.NEXT");
        mediaControlFilter.addAction("com.android.systemui.pip.phone.PREV");
        this.mContext.registerReceiver(this.mPlayPauseActionReceiver, mediaControlFilter);
        createMediaActions();
        this.mMediaSessionManager = (MediaSessionManager) context.getSystemService("media_session");
        this.mMediaSessionManager.addOnActiveSessionsChangedListener(new MediaSessionManager.OnActiveSessionsChangedListener() {
            public final void onActiveSessionsChanged(List list) {
                PipMediaController.this.resolveActiveMediaController(list);
            }
        }, null);
    }

    public void onActivityPinned() {
        resolveActiveMediaController(this.mMediaSessionManager.getActiveSessions(null));
    }

    public void addListener(ActionListener listener) {
        if (!this.mListeners.contains(listener)) {
            this.mListeners.add(listener);
            listener.onMediaActionsChanged(getMediaActions());
        }
    }

    public void removeListener(ActionListener listener) {
        listener.onMediaActionsChanged(Collections.EMPTY_LIST);
        this.mListeners.remove(listener);
    }

    private List<RemoteAction> getMediaActions() {
        if (this.mMediaController == null || this.mMediaController.getPlaybackState() == null) {
            return Collections.EMPTY_LIST;
        }
        ArrayList<RemoteAction> mediaActions = new ArrayList<>();
        boolean isPlaying = MediaSession.isActiveState(this.mMediaController.getPlaybackState().getState());
        long actions = this.mMediaController.getPlaybackState().getActions();
        boolean z = false;
        this.mPrevAction.setEnabled((16 & actions) != 0);
        mediaActions.add(this.mPrevAction);
        if (!isPlaying && (4 & actions) != 0) {
            mediaActions.add(this.mPlayAction);
        } else if (isPlaying && (2 & actions) != 0) {
            mediaActions.add(this.mPauseAction);
        }
        RemoteAction remoteAction = this.mNextAction;
        if ((32 & actions) != 0) {
            z = true;
        }
        remoteAction.setEnabled(z);
        mediaActions.add(this.mNextAction);
        return mediaActions;
    }

    private void createMediaActions() {
        String pauseDescription = this.mContext.getString(R.string.pip_pause);
        this.mPauseAction = new RemoteAction(Icon.createWithResource(this.mContext, R.drawable.ic_pause_white), pauseDescription, pauseDescription, PendingIntent.getBroadcast(this.mContext, 0, new Intent("com.android.systemui.pip.phone.PAUSE"), 134217728));
        String playDescription = this.mContext.getString(R.string.pip_play);
        this.mPlayAction = new RemoteAction(Icon.createWithResource(this.mContext, R.drawable.ic_play_arrow_white), playDescription, playDescription, PendingIntent.getBroadcast(this.mContext, 0, new Intent("com.android.systemui.pip.phone.PLAY"), 134217728));
        String nextDescription = this.mContext.getString(R.string.pip_skip_to_next);
        this.mNextAction = new RemoteAction(Icon.createWithResource(this.mContext, R.drawable.ic_skip_next_white), nextDescription, nextDescription, PendingIntent.getBroadcast(this.mContext, 0, new Intent("com.android.systemui.pip.phone.NEXT"), 134217728));
        String prevDescription = this.mContext.getString(R.string.pip_skip_to_prev);
        this.mPrevAction = new RemoteAction(Icon.createWithResource(this.mContext, R.drawable.ic_skip_previous_white), prevDescription, prevDescription, PendingIntent.getBroadcast(this.mContext, 0, new Intent("com.android.systemui.pip.phone.PREV"), 134217728));
    }

    /* access modifiers changed from: private */
    public void resolveActiveMediaController(List<MediaController> controllers) {
        if (controllers != null) {
            ComponentName topActivity = PipUtils.getTopPinnedActivity(this.mContext, this.mActivityManager);
            if (topActivity != null) {
                for (int i = 0; i < controllers.size(); i++) {
                    MediaController controller = controllers.get(i);
                    if (controller.getPackageName().equals(topActivity.getPackageName())) {
                        setActiveMediaController(controller);
                        return;
                    }
                }
            }
        }
        setActiveMediaController(null);
    }

    private void setActiveMediaController(MediaController controller) {
        if (controller != this.mMediaController) {
            if (this.mMediaController != null) {
                this.mMediaController.unregisterCallback(this.mPlaybackChangedListener);
            }
            this.mMediaController = controller;
            if (controller != null) {
                controller.registerCallback(this.mPlaybackChangedListener);
            }
            notifyActionsChanged();
        }
    }

    /* access modifiers changed from: private */
    public void notifyActionsChanged() {
        if (!this.mListeners.isEmpty()) {
            List<RemoteAction> actions = getMediaActions();
            Iterator<ActionListener> it = this.mListeners.iterator();
            while (it.hasNext()) {
                it.next().onMediaActionsChanged(actions);
            }
        }
    }
}
