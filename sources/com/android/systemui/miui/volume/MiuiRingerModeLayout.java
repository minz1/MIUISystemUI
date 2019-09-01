package com.android.systemui.miui.volume;

import android.app.ExtraNotificationManager;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.provider.MiuiSettings;
import android.transition.AutoTransition;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.transition.TransitionManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import com.android.systemui.miui.DrawableAnimators;
import com.android.systemui.miui.analytics.AnalyticsWrapper;
import com.android.systemui.miui.widget.TimerSeekBar;

public class MiuiRingerModeLayout extends LinearLayout implements SeekBar.OnSeekBarChangeListener, TimerSeekBar.OnTimeUpdateListener {
    /* access modifiers changed from: private */
    public Context mContext;
    private ProgressBar mCountDownProgress;
    /* access modifiers changed from: private */
    public boolean mExpanded;
    private View mRingerBtnLayout;
    private RingerButtonHelper mRingerHelper;
    /* access modifiers changed from: private */
    public int mRingerMode;
    private View mTickingTimePortrait;
    private MiuiVolumeTimerSeekBar mTimer;
    private boolean mTimerDragging;
    private View mTimerLayout;
    /* access modifiers changed from: private */
    public boolean mTransitionRunning;
    private Runnable mUpdateTimerRunnable;

    private static class RingerButtonHelper {
        private View mDndView;
        private boolean mExpanded;
        private int mRingerMode;
        private View mStandardView;

        RingerButtonHelper(View standardView, View dndView) {
            this.mStandardView = standardView;
            this.mDndView = dndView;
        }

        /* access modifiers changed from: package-private */
        public void setRingerMode(int mode) {
            this.mRingerMode = mode;
        }

        /* access modifiers changed from: package-private */
        public void onExpanded(boolean expand) {
            this.mExpanded = expand;
        }

        /* access modifiers changed from: private */
        public void updateState() {
            int i;
            int i2;
            int lastMode = this.mRingerMode;
            if (lastMode == 0) {
                lastMode = MiuiSettings.SilenceMode.getLastestQuietMode(this.mStandardView.getContext());
            }
            boolean z = false;
            this.mStandardView.setSelected(this.mRingerMode == 4);
            this.mStandardView.setActivated(!this.mExpanded);
            this.mDndView.setSelected(this.mRingerMode == 1);
            this.mDndView.setActivated(!this.mExpanded);
            Util.setVisOrGone(this.mStandardView, this.mExpanded || lastMode != 1);
            View view = this.mDndView;
            if (this.mExpanded || lastMode == 1) {
                z = true;
            }
            Util.setVisOrGone(view, z);
            Util.setVisOrGone(this.mStandardView.findViewById(16908310), this.mExpanded);
            Util.setVisOrGone(this.mDndView.findViewById(16908310), this.mExpanded);
            Context context = this.mDndView.getContext();
            if (this.mDndView.getBackground() instanceof GradientDrawable) {
                Drawable background = this.mDndView.getBackground();
                if (this.mExpanded) {
                    i2 = R.array.miui_volume_ringer_btn_dnd_corners;
                } else {
                    i2 = R.array.miui_volume_ringer_btn_corners_collapsed;
                }
                DrawableAnimators.updateCornerRadii(context, background, i2);
            }
            if (this.mStandardView.getBackground() instanceof GradientDrawable) {
                Drawable background2 = this.mStandardView.getBackground();
                if (this.mExpanded) {
                    i = R.array.miui_volume_ringer_btn_standard_corners;
                } else {
                    i = R.array.miui_volume_ringer_btn_corners_collapsed;
                }
                DrawableAnimators.updateCornerRadii(context, background2, i);
            }
        }
    }

    public MiuiRingerModeLayout(Context context) {
        this(context, null);
    }

    public MiuiRingerModeLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MiuiRingerModeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mRingerMode = 0;
        this.mUpdateTimerRunnable = new Runnable() {
            public void run() {
                MiuiRingerModeLayout.this.updateRemainTimeH();
            }
        };
        this.mContext = context;
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        initialize();
    }

    private void initialize() {
        this.mRingerBtnLayout = findViewById(R.id.miui_ringer_btn_layout);
        this.mTimerLayout = findViewById(R.id.miui_volume_timer_layout);
        this.mTimer = (MiuiVolumeTimerSeekBar) findViewById(R.id.miui_volume_timer);
        this.mTimer.setOnTimeUpdateListener(this);
        this.mCountDownProgress = (ProgressBar) findViewById(R.id.miui_volume_count_down_progress);
        View ringerStandardButton = findViewById(R.id.miui_ringer_standard_btn);
        View ringerDndButton = findViewById(R.id.miui_ringer_dnd_btn);
        this.mRingerHelper = new RingerButtonHelper(ringerStandardButton, ringerDndButton);
        ringerStandardButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String str;
                if (!MiuiRingerModeLayout.this.mExpanded || !MiuiRingerModeLayout.this.mTransitionRunning) {
                    if (MiuiRingerModeLayout.this.mExpanded) {
                        str = "volume_click_silent_at_secondary_page";
                    } else {
                        str = "volume_click_silent";
                    }
                    AnalyticsWrapper.recordCountEventAnonymous("systemui_volume_dialog", str);
                    MiuiRingerModeLayout miuiRingerModeLayout = MiuiRingerModeLayout.this;
                    int i = 4;
                    if (MiuiRingerModeLayout.this.mRingerMode == 4) {
                        i = 0;
                    }
                    miuiRingerModeLayout.setRingerModeByUser(i);
                    return;
                }
                Log.i("RingerModeLayout", "setSilenceMode mTransitionRunning is true.");
            }
        });
        ringerDndButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!MiuiRingerModeLayout.this.mExpanded || !MiuiRingerModeLayout.this.mTransitionRunning) {
                    if (MiuiRingerModeLayout.this.mExpanded) {
                        AnalyticsWrapper.recordCountEventAnonymous("systemui_volume_dialog", "volume_click_dnd_at_secondary_page");
                    }
                    MiuiRingerModeLayout miuiRingerModeLayout = MiuiRingerModeLayout.this;
                    int i = 1;
                    if (MiuiRingerModeLayout.this.mRingerMode == 1) {
                        i = 0;
                    }
                    miuiRingerModeLayout.setRingerModeByUser(i);
                    return;
                }
                Log.i("RingerModeLayout", "setSilenceMode mTransitionRunning is true.");
            }
        });
        addTickingTimeReceivers();
        this.mTimer.setOnSeekBarChangeListener(this);
        setMotionEventSplittingEnabled(false);
        setRingerModeInternal(MiuiSettings.SilenceMode.getZenMode(this.mContext));
    }

    private void addTickingTimeReceivers() {
        boolean landscape = this.mContext.getResources().getConfiguration().orientation == 2;
        TextView tickingTime = (TextView) findViewById(R.id.miui_volume_timer_ticking);
        TextView tickingTimePortrait = (TextView) findViewById(R.id.miui_volume_timer_ticking_port);
        if (tickingTime != null) {
            this.mTimer.addTickingTimeReceiver(tickingTime);
            this.mTimer.addCountDownStateReceiver(tickingTime);
        }
        if (tickingTimePortrait != null && !landscape) {
            this.mTimer.addCountDownStateReceiver(tickingTimePortrait);
            this.mTickingTimePortrait = tickingTimePortrait;
        }
    }

    private void setRingerModeInternal(int mode) {
        this.mRingerMode = mode;
        this.mRingerHelper.setRingerMode(mode);
    }

    /* access modifiers changed from: private */
    public void setRingerModeByUser(final int mode) {
        setRingerModeInternal(mode);
        AsyncTask.execute(new Runnable() {
            public void run() {
                VolumeUtil.setSilenceMode(MiuiRingerModeLayout.this.mContext, mode, MiuiRingerModeLayout.this.isSilenceModeOn() ? ExtraNotificationManager.getConditionId(MiuiRingerModeLayout.this.mContext) : null);
            }
        });
    }

    public void setSilenceMode(int mode, boolean doAnimation) {
        Log.i("RingerModeLayout", "Zenmode changed " + this.mRingerMode + " -> " + mode + " doAnimation:" + doAnimation);
        setRingerModeInternal(mode);
        if (doAnimation) {
            if (!this.mExpanded || !this.mTransitionRunning) {
                post(new Runnable() {
                    public void run() {
                        TransitionManager.endTransitions(MiuiRingerModeLayout.this);
                        TransitionManager.beginDelayedTransition(MiuiRingerModeLayout.this, MiuiRingerModeLayout.this.getTimerLayoutTransition());
                        MiuiRingerModeLayout.this.updateExpandedStateH();
                        MiuiRingerModeLayout.this.updateRemainTimeH();
                    }
                });
            } else {
                Log.i("RingerModeLayout", "setSilenceMode mTransitionRunning is true.");
            }
        }
    }

    private void updateCountProgressH() {
        Util.setVisOrGone(this.mCountDownProgress, !this.mExpanded && isSilenceModeOn() && this.mTimer.getRemainTime() > 0);
    }

    /* access modifiers changed from: private */
    public void updateExpandedStateH() {
        this.mRingerHelper.updateState();
        updateCountProgressH();
        Util.setVisOrGone(this.mTimerLayout, isSilenceModeOn() && this.mExpanded);
    }

    private void updateDraggingStateH() {
        boolean z = true;
        boolean showPortraitTicking = this.mTickingTimePortrait != null && this.mTimerDragging;
        TransitionManager.beginDelayedTransition(this, new AutoTransition().setOrdering(0).setDuration((long) this.mContext.getResources().getInteger(R.integer.miui_volume_transition_duration_short)));
        View view = this.mRingerBtnLayout;
        if (showPortraitTicking) {
            z = false;
        }
        Util.setVisOrGone(view, z);
        Util.setVisOrGone(this.mTickingTimePortrait, showPortraitTicking);
    }

    /* access modifiers changed from: private */
    public void updateRemainTimeH() {
        updateRemainTimeH(false);
    }

    private void updateRemainTimeH(boolean forceSchedule) {
        long remain = ExtraNotificationManager.getRemainTime(this.mContext);
        this.mTimer.updateRemainTime((int) (remain / 1000));
        scheduleTimerUpdateH(remain > 0 || forceSchedule);
    }

    private void scheduleTimerUpdateH(boolean ongoing) {
        if (!ongoing || !this.mExpanded) {
            removeCallbacks(this.mUpdateTimerRunnable);
        } else {
            postDelayed(this.mUpdateTimerRunnable, 1000);
        }
    }

    public void updateExpandedH(boolean expanded) {
        TransitionManager.endTransitions(this);
        this.mExpanded = expanded;
        if (expanded) {
            updateRemainTimeH();
        } else {
            scheduleTimerUpdateH(false);
            if (this.mTimerDragging) {
                this.mTimerDragging = false;
                updateDraggingStateH();
            }
        }
        this.mRingerHelper.onExpanded(expanded);
        updateExpandedStateH();
    }

    private void setupCountDownProgress() {
        this.mCountDownProgress.setMax(Util.getLastTotalCountDownTime(this.mContext));
        this.mCountDownProgress.setProgress(this.mTimer.getRemainTime());
    }

    public void onTimeSet(int time) {
        Util.setLastTotalCountDownTime(this.mContext, time);
        setupCountDownProgress();
        ExtraNotificationManager.startCountDownSilenceMode(this.mContext, this.mRingerMode, time / 60);
        updateRemainTimeH(true);
    }

    public void onSegmentChange(int currentSegment, int determinedSegment) {
    }

    public void onTimeUpdate(int timeRemain) {
        this.mCountDownProgress.setProgress(timeRemain);
    }

    /* access modifiers changed from: private */
    public boolean isSilenceModeOn() {
        return this.mRingerMode > 0;
    }

    public void init() {
        setRingerModeInternal(MiuiSettings.SilenceMode.getZenMode(this.mContext));
        updateRemainTimeH();
        updateExpandedStateH();
        setupCountDownProgress();
    }

    public void cleanUp() {
    }

    public int getRingerMode() {
        return this.mRingerMode;
    }

    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
        this.mTimerDragging = true;
        updateDraggingStateH();
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
        this.mTimerDragging = false;
        updateDraggingStateH();
        if (this.mRingerMode == 4) {
            AnalyticsWrapper.recordCountEventAnonymous("systemui_volume_dialog", "volume_set_timer_at_silent_mode");
        } else if (this.mRingerMode == 1) {
            AnalyticsWrapper.recordCountEventAnonymous("systemui_volume_dialog", "volume_set_timer_at_dnd_mode");
        }
    }

    /* access modifiers changed from: private */
    public Transition getTimerLayoutTransition() {
        int i;
        TransitionInflater from = TransitionInflater.from(this.mContext);
        if (this.mExpanded) {
            i = R.transition.miui_volume_dialog;
        } else {
            i = R.transition.miui_volume_ringer_collapse;
        }
        return from.inflateTransition(i).addListener(new Transition.TransitionListener() {
            public void onTransitionStart(Transition transition) {
                boolean unused = MiuiRingerModeLayout.this.mTransitionRunning = true;
            }

            public void onTransitionEnd(Transition transition) {
                boolean unused = MiuiRingerModeLayout.this.mTransitionRunning = false;
            }

            public void onTransitionCancel(Transition transition) {
                boolean unused = MiuiRingerModeLayout.this.mTransitionRunning = false;
            }

            public void onTransitionPause(Transition transition) {
            }

            public void onTransitionResume(Transition transition) {
            }
        });
    }

    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);
    }
}
