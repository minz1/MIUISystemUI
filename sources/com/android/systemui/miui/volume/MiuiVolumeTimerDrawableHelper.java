package com.android.systemui.miui.volume;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ScaleDrawable;
import android.util.Log;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.SeekBar;
import android.widget.TextView;
import com.android.systemui.miui.DrawableAnimators;
import com.android.systemui.miui.DrawableUtils;
import com.android.systemui.miui.volume.MiuiVolumeTimerSeekBar;
import com.android.systemui.miui.widget.CenterTextDrawable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class MiuiVolumeTimerDrawableHelper implements MiuiVolumeTimerSeekBar.TimerSeekBarMotions {
    private Drawable mBackground;
    private Drawable mBackgroundSegments;
    private Context mContext;
    private List<Object> mCountDownStates = new ArrayList();
    private int mCurrentSegment;
    private int mDeterminedSegment;
    /* access modifiers changed from: private */
    public boolean mDragging;
    private Drawable mDrawable;
    private Drawable mProgress;
    private Drawable mProgressDraggingIcon;
    /* access modifiers changed from: private */
    public Drawable mProgressDraggingRect;
    private Drawable mProgressDraggingRectIdle;
    /* access modifiers changed from: private */
    public Drawable mProgressNormalRect;
    /* access modifiers changed from: private */
    public boolean mTicking;
    private List<Object> mTickingTimes = new ArrayList();
    private CenterTextDrawable mTimeDrawableBg;
    private CenterTextDrawable mTimeDrawableFg;
    private String mTimeDrawableHint;
    private int mTimeRemain;
    private String[] mTimeSegmentTitle;

    MiuiVolumeTimerDrawableHelper(SeekBar timer, boolean drawTickingTime) {
        this.mContext = timer.getContext();
        this.mTimeDrawableHint = timer.getResources().getString(R.string.miui_ringer_count_down);
        this.mTimeSegmentTitle = timer.getResources().getStringArray(R.array.miui_volume_timer_segments_title);
        this.mDrawable = timer.getProgressDrawable();
        if (this.mDrawable != null) {
            setupDrawables(timer.getContext(), drawTickingTime);
            setOutlineProvider(timer);
            timer.setProgressDrawable(this.mDrawable);
            updateDrawables();
        }
    }

    private void setupDrawables(Context context, boolean drawTickingTime) {
        this.mBackground = DrawableUtils.findDrawableById(this.mDrawable, 16908288);
        this.mBackgroundSegments = DrawableUtils.findDrawableById(this.mBackground, R.id.miui_volume_timer_background_segments);
        this.mProgress = DrawableUtils.findDrawableById(this.mDrawable, 16908301);
        this.mProgressNormalRect = DrawableUtils.findDrawableById(this.mProgress, R.id.miui_volume_timer_progress_normal);
        this.mProgressDraggingRect = DrawableUtils.findDrawableById(this.mProgress, R.id.miui_volume_timer_progress_dragging_rect);
        this.mProgressDraggingRectIdle = DrawableUtils.findDrawableById(this.mProgress, R.id.miui_volume_timer_progress_dragging_rect_idle);
        this.mProgressDraggingIcon = DrawableUtils.findDrawableById(this.mProgress, R.id.miui_volume_timer_progress_dragging_icon);
        if (drawTickingTime) {
            addTextDrawables(context);
        }
    }

    private void addTextDrawables(Context context) {
        if (!(this.mDrawable instanceof LayerDrawable)) {
            Log.e("VolumeTimerDrawables", "progress drawable is not a LayerDrawable");
            return;
        }
        LayerDrawable layer = (LayerDrawable) this.mDrawable;
        float textSize = context.getResources().getDimension(R.dimen.miui_volume_timer_time_text_size);
        this.mTimeDrawableBg = new CenterTextDrawable();
        this.mTimeDrawableBg.setTextSize(textSize);
        this.mTimeDrawableBg.setTextColor(context.getResources().getColor(R.color.miui_volume_tint_dark));
        layer.setDrawableByLayerId(16908288, new LayerDrawable(new Drawable[]{this.mBackground, this.mTimeDrawableBg}));
        this.mTimeDrawableFg = new CenterTextDrawable();
        this.mTimeDrawableFg.setTextSize(textSize);
        this.mTimeDrawableFg.setTextColor(context.getResources().getColor(R.color.miui_volume_tint_light));
        layer.setDrawableByLayerId(16908301, new LayerDrawable(new Drawable[]{this.mProgress, new ScaleDrawable(this.mTimeDrawableFg, 8388611, 1.0f, 0.0f)}));
        this.mTickingTimes.add(this.mTimeDrawableFg);
        this.mTickingTimes.add(this.mTimeDrawableBg);
    }

    private void setOutlineProvider(View seekBar) {
        seekBar.setOutlineProvider(new ViewOutlineProvider() {
            public void getOutline(View view, Outline outline) {
                Outline outline2 = outline;
                outline2.setRoundRect(0, 0, view.getWidth(), view.getHeight(), (float) (Math.min(view.getWidth(), view.getHeight()) / 2));
            }
        });
        seekBar.setClipToOutline(true);
    }

    private void updateDrawables() {
        boolean normalRectVisible = true;
        DrawableAnimators.fade(this.mProgressDraggingIcon, this.mDragging || !this.mTicking);
        DrawableAnimators.fade(this.mBackgroundSegments, this.mDragging);
        DrawableAnimators.fade(this.mProgressDraggingRectIdle, !this.mDragging && !this.mTicking);
        boolean ongoing = this.mTicking && !this.mDragging;
        boolean idle = !this.mTicking && !this.mDragging;
        if (this.mTimeDrawableFg != null) {
            DrawableAnimators.fade(this.mTimeDrawableFg, ongoing && this.mCurrentSegment > 1);
        }
        if (this.mTimeDrawableBg != null) {
            DrawableAnimators.fade(this.mTimeDrawableBg, (ongoing && this.mCurrentSegment <= 1) || idle);
        }
        if (idle) {
            updateTickingTimeText(0);
        }
        if (!ongoing) {
            if (this.mProgressNormalRect.getAlpha() != 255) {
                normalRectVisible = false;
            }
            Util.setVisOrInvis(this.mProgressNormalRect, false);
            if (normalRectVisible) {
                Util.setVisOrInvis(this.mProgressDraggingRect, this.mDragging);
            } else {
                DrawableAnimators.fade(this.mProgressDraggingRect, this.mDragging);
            }
            DrawableAnimators.updateCornerRadii(this.mContext, this.mProgressDraggingRect, R.array.miui_volume_progress_dragging_corners);
            DrawableAnimators.updateCornerRadii(this.mContext, this.mProgressDraggingRectIdle, R.array.miui_volume_progress_dragging_corners);
            return;
        }
        DrawableAnimators.updateCornerRadii(this.mContext, this.mProgressDraggingRectIdle, R.array.miui_volume_progress_released_corners);
        DrawableAnimators.updateCornerRadii(this.mContext, this.mProgressDraggingRect, R.array.miui_volume_progress_released_corners).addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                boolean z = false;
                boolean ongoing = MiuiVolumeTimerDrawableHelper.this.mTicking && !MiuiVolumeTimerDrawableHelper.this.mDragging;
                Util.setVisOrInvis(MiuiVolumeTimerDrawableHelper.this.mProgressNormalRect, ongoing);
                Drawable access$300 = MiuiVolumeTimerDrawableHelper.this.mProgressDraggingRect;
                if (!ongoing) {
                    z = true;
                }
                Util.setVisOrInvis(access$300, z);
            }
        });
    }

    public void onTouchDown() {
        this.mDragging = true;
        updateDrawables();
        updateCountDownStateText();
    }

    public void onTouchRelease() {
        this.mDragging = false;
        updateDrawables();
        updateTickingTimeText(this.mTimeRemain);
    }

    public void onSegmentChange(int currentSegment, int determinedSegment) {
        if (!(this.mCurrentSegment == currentSegment && this.mDeterminedSegment == determinedSegment)) {
            this.mCurrentSegment = currentSegment;
            this.mDeterminedSegment = determinedSegment;
            updateDrawables();
        }
        if (this.mDragging) {
            updateCountDownStateText();
        }
    }

    public void onTimeUpdate(int remain) {
        this.mTimeRemain = remain;
        updateTickingTimeText(remain);
        boolean ticking = remain > 0;
        if (this.mTicking != ticking) {
            this.mTicking = ticking;
            updateDrawables();
        }
    }

    public void addTickingTimeReceiver(TextView view) {
        this.mTickingTimes.add(view);
    }

    public void addCountDownStateReceiver(TextView view) {
        this.mCountDownStates.add(view);
    }

    private void updateTickingTimeText(int remain) {
        String text;
        if (!this.mTicking && !this.mDragging) {
            text = this.mTimeDrawableHint;
        } else {
            text = formatRemainTime(remain);
        }
        for (Object o : this.mTickingTimes) {
            if (o instanceof CenterTextDrawable) {
                ((CenterTextDrawable) o).setText(text);
            } else if (o instanceof TextView) {
                ((TextView) o).setText(text);
            }
        }
    }

    private void updateCountDownStateText() {
        String text = "";
        if (this.mDragging) {
            String segmentTitle = this.mTimeSegmentTitle[Util.constrain(this.mDeterminedSegment - 1, 0, this.mTimeSegmentTitle.length - 1)];
            text = this.mContext.getResources().getString(R.string.miui_ringer_count_down_time, new Object[]{segmentTitle});
        }
        for (Object o : this.mCountDownStates) {
            if (o instanceof CenterTextDrawable) {
                ((CenterTextDrawable) o).setText(text);
            } else if (o instanceof TextView) {
                ((TextView) o).setText(text);
            }
        }
    }

    private String formatRemainTime(int remain) {
        return String.format(Locale.getDefault(), "%d:%02d:%02d", new Object[]{Integer.valueOf((remain / 60) / 60), Integer.valueOf((remain / 60) % 60), Integer.valueOf(remain % 60)});
    }
}
