package com.android.systemui.pip.tv;

import android.app.PendingIntent;
import android.app.RemoteAction;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import com.android.systemui.R;
import com.android.systemui.pip.tv.PipManager;
import java.util.ArrayList;
import java.util.List;

public class PipControlsView extends LinearLayout {
    private static final String TAG = PipControlsView.class.getSimpleName();
    private PipControlButtonView mCloseButtonView;
    private List<RemoteAction> mCustomActions;
    private ArrayList<PipControlButtonView> mCustomButtonViews;
    private final View.OnFocusChangeListener mFocusChangeListener;
    /* access modifiers changed from: private */
    public PipControlButtonView mFocusedChild;
    private PipControlButtonView mFullButtonView;
    private final Handler mHandler;
    private final LayoutInflater mLayoutInflater;
    /* access modifiers changed from: private */
    public Listener mListener;
    /* access modifiers changed from: private */
    public MediaController mMediaController;
    private MediaController.Callback mMediaControllerCallback;
    /* access modifiers changed from: private */
    public final PipManager mPipManager;
    private final PipManager.MediaListener mPipMediaListener;
    private PipControlButtonView mPlayPauseButtonView;

    public interface Listener {
        void onClosed();
    }

    public PipControlsView(Context context) {
        this(context, null, 0, 0);
    }

    public PipControlsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }

    public PipControlsView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public PipControlsView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mPipManager = PipManager.getInstance();
        this.mCustomButtonViews = new ArrayList<>();
        this.mCustomActions = new ArrayList();
        this.mMediaControllerCallback = new MediaController.Callback() {
            public void onPlaybackStateChanged(PlaybackState state) {
                PipControlsView.this.updateUserActions();
            }
        };
        this.mPipMediaListener = new PipManager.MediaListener() {
            public void onMediaControllerChanged() {
                PipControlsView.this.updateMediaController();
            }
        };
        this.mFocusChangeListener = new View.OnFocusChangeListener() {
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    PipControlButtonView unused = PipControlsView.this.mFocusedChild = (PipControlButtonView) view;
                } else if (PipControlsView.this.mFocusedChild == view) {
                    PipControlButtonView unused2 = PipControlsView.this.mFocusedChild = null;
                }
            }
        };
        this.mLayoutInflater = (LayoutInflater) getContext().getSystemService("layout_inflater");
        this.mLayoutInflater.inflate(R.layout.tv_pip_controls, this);
        this.mHandler = new Handler();
        setOrientation(0);
        setGravity(49);
    }

    public void onFinishInflate() {
        super.onFinishInflate();
        this.mFullButtonView = (PipControlButtonView) findViewById(R.id.full_button);
        this.mFullButtonView.setOnFocusChangeListener(this.mFocusChangeListener);
        this.mFullButtonView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                PipControlsView.this.mPipManager.movePipToFullscreen();
            }
        });
        this.mCloseButtonView = (PipControlButtonView) findViewById(R.id.close_button);
        this.mCloseButtonView.setOnFocusChangeListener(this.mFocusChangeListener);
        this.mCloseButtonView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                PipControlsView.this.mPipManager.closePip();
                if (PipControlsView.this.mListener != null) {
                    PipControlsView.this.mListener.onClosed();
                }
            }
        });
        this.mPlayPauseButtonView = (PipControlButtonView) findViewById(R.id.play_pause_button);
        this.mPlayPauseButtonView.setOnFocusChangeListener(this.mFocusChangeListener);
        this.mPlayPauseButtonView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (PipControlsView.this.mMediaController != null && PipControlsView.this.mMediaController.getPlaybackState() != null) {
                    long actions = PipControlsView.this.mMediaController.getPlaybackState().getActions();
                    int state = PipControlsView.this.mMediaController.getPlaybackState().getState();
                    if (PipControlsView.this.mPipManager.getPlaybackState() == 1) {
                        PipControlsView.this.mMediaController.getTransportControls().play();
                    } else if (PipControlsView.this.mPipManager.getPlaybackState() == 0) {
                        PipControlsView.this.mMediaController.getTransportControls().pause();
                    }
                }
            }
        });
    }

    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateMediaController();
        this.mPipManager.addMediaListener(this.mPipMediaListener);
    }

    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mPipManager.removeMediaListener(this.mPipMediaListener);
        if (this.mMediaController != null) {
            this.mMediaController.unregisterCallback(this.mMediaControllerCallback);
        }
    }

    /* access modifiers changed from: private */
    public void updateMediaController() {
        MediaController newController = this.mPipManager.getMediaController();
        if (this.mMediaController != newController) {
            if (this.mMediaController != null) {
                this.mMediaController.unregisterCallback(this.mMediaControllerCallback);
            }
            this.mMediaController = newController;
            if (this.mMediaController != null) {
                this.mMediaController.registerCallback(this.mMediaControllerCallback);
            }
            updateUserActions();
        }
    }

    /* access modifiers changed from: private */
    public void updateUserActions() {
        int i = 0;
        if (!this.mCustomActions.isEmpty()) {
            while (this.mCustomButtonViews.size() < this.mCustomActions.size()) {
                PipControlButtonView buttonView = (PipControlButtonView) this.mLayoutInflater.inflate(R.layout.tv_pip_custom_control, this, false);
                addView(buttonView);
                this.mCustomButtonViews.add(buttonView);
            }
            int i2 = 0;
            while (i2 < this.mCustomButtonViews.size()) {
                this.mCustomButtonViews.get(i2).setVisibility(i2 < this.mCustomActions.size() ? 0 : 8);
                i2++;
            }
            while (true) {
                int i3 = i;
                if (i3 < this.mCustomActions.size()) {
                    RemoteAction action = this.mCustomActions.get(i3);
                    PipControlButtonView actionView = this.mCustomButtonViews.get(i3);
                    action.getIcon().loadDrawableAsync(getContext(), new Icon.OnDrawableLoadedListener() {
                        public final void onDrawableLoaded(Drawable drawable) {
                            PipControlsView.lambda$updateUserActions$0(PipControlButtonView.this, drawable);
                        }
                    }, this.mHandler);
                    actionView.setText(action.getContentDescription());
                    if (action.isEnabled()) {
                        actionView.setOnClickListener(new View.OnClickListener(action) {
                            private final /* synthetic */ RemoteAction f$0;

                            {
                                this.f$0 = r1;
                            }

                            public final void onClick(View view) {
                                PipControlsView.lambda$updateUserActions$1(this.f$0, view);
                            }
                        });
                    }
                    actionView.setEnabled(action.isEnabled());
                    actionView.setAlpha(action.isEnabled() ? 1.0f : 0.54f);
                    i = i3 + 1;
                } else {
                    this.mPlayPauseButtonView.setVisibility(8);
                    return;
                }
            }
        } else {
            int state = this.mPipManager.getPlaybackState();
            if (state == 2) {
                this.mPlayPauseButtonView.setVisibility(8);
            } else {
                this.mPlayPauseButtonView.setVisibility(0);
                if (state == 0) {
                    this.mPlayPauseButtonView.setImageResource(R.drawable.ic_pause_white);
                    this.mPlayPauseButtonView.setText((int) R.string.pip_pause);
                } else {
                    this.mPlayPauseButtonView.setImageResource(R.drawable.ic_play_arrow_white);
                    this.mPlayPauseButtonView.setText((int) R.string.pip_play);
                }
            }
            while (i < this.mCustomButtonViews.size()) {
                this.mCustomButtonViews.get(i).setVisibility(8);
                i++;
            }
        }
    }

    static /* synthetic */ void lambda$updateUserActions$0(PipControlButtonView actionView, Drawable d) {
        d.setTint(-1);
        actionView.setImageDrawable(d);
    }

    static /* synthetic */ void lambda$updateUserActions$1(RemoteAction action, View v) {
        try {
            action.getActionIntent().send();
        } catch (PendingIntent.CanceledException e) {
            Log.w(TAG, "Failed to send action", e);
        }
    }

    public void setActions(List<RemoteAction> actions) {
        this.mCustomActions.clear();
        this.mCustomActions.addAll(actions);
        updateUserActions();
    }
}
