package com.android.systemui.recents.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.CountDownTimer;
import android.support.v4.graphics.ColorUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewDebug;
import android.view.ViewPropertyAnimator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.events.RecentsEventBus;
import com.android.systemui.recents.events.ui.ShowApplicationInfoEvent;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.recents.model.Task;

public class TaskViewHeader extends FrameLayout implements View.OnClickListener, View.OnLongClickListener {
    ImageView mAppIconView;
    ImageView mAppInfoView;
    FrameLayout mAppOverlayView;
    TextView mAppTitleView;
    private HighlightColorDrawable mBackground;
    int mCornerRadius;
    Drawable mDarkDismissDrawable;
    Drawable mDarkFreeformIcon;
    Drawable mDarkFullscreenIcon;
    Drawable mDarkInfoIcon;
    @ViewDebug.ExportedProperty(category = "recents")
    float mDimAlpha;
    private Paint mDimLayerPaint;
    int mDisabledTaskBarBackgroundColor;
    private CountDownTimer mFocusTimerCountDown;
    ProgressBar mFocusTimerIndicator;
    int mHeaderBarHeight;
    int mHeaderButtonPadding;
    int mHighlightHeight;
    ImageView mIconView;
    Drawable mLightDismissDrawable;
    Drawable mLightFreeformIcon;
    Drawable mLightFullscreenIcon;
    Drawable mLightInfoIcon;
    ImageView mLockedView;
    int mMoveTaskTargetStackId;
    private HighlightColorDrawable mOverlayBackground;
    Task mTask;
    int mTaskBarViewDarkTextColor;
    int mTaskBarViewLightTextColor;
    @ViewDebug.ExportedProperty(category = "recents")
    Rect mTaskViewRect;
    TextView mTitleView;
    /* access modifiers changed from: private */
    public float[] mTmpHSL;

    private class HighlightColorDrawable extends Drawable {
        private Paint mBackgroundPaint = new Paint();
        private int mColor;
        private float mDimAlpha;
        private Paint mHighlightPaint = new Paint();

        public HighlightColorDrawable() {
            this.mBackgroundPaint.setColor(Color.argb(255, 0, 0, 0));
            this.mBackgroundPaint.setAntiAlias(true);
            this.mHighlightPaint.setColor(Color.argb(255, 255, 255, 255));
            this.mHighlightPaint.setAntiAlias(true);
        }

        public void setColorAndDim(int color, float dimAlpha) {
            if (this.mColor != color || Float.compare(this.mDimAlpha, dimAlpha) != 0) {
                this.mColor = color;
                this.mDimAlpha = dimAlpha;
                this.mBackgroundPaint.setColor(color);
                ColorUtils.colorToHSL(color, TaskViewHeader.this.mTmpHSL);
                TaskViewHeader.this.mTmpHSL[2] = Math.min(1.0f, TaskViewHeader.this.mTmpHSL[2] + (0.075f * (1.0f - dimAlpha)));
                this.mHighlightPaint.setColor(ColorUtils.HSLToColor(TaskViewHeader.this.mTmpHSL));
                invalidateSelf();
            }
        }

        public void setColorFilter(ColorFilter colorFilter) {
        }

        public void setAlpha(int alpha) {
        }

        public void draw(Canvas canvas) {
            canvas.drawRoundRect(0.0f, 0.0f, (float) TaskViewHeader.this.mTaskViewRect.width(), (float) (2 * Math.max(TaskViewHeader.this.mHighlightHeight, TaskViewHeader.this.mCornerRadius)), (float) TaskViewHeader.this.mCornerRadius, (float) TaskViewHeader.this.mCornerRadius, this.mHighlightPaint);
            canvas.drawRoundRect(0.0f, (float) TaskViewHeader.this.mHighlightHeight, (float) TaskViewHeader.this.mTaskViewRect.width(), (float) (TaskViewHeader.this.getHeight() + TaskViewHeader.this.mCornerRadius), (float) TaskViewHeader.this.mCornerRadius, (float) TaskViewHeader.this.mCornerRadius, this.mBackgroundPaint);
        }

        public int getOpacity() {
            return -1;
        }

        public int getColor() {
            return this.mColor;
        }
    }

    public TaskViewHeader(Context context) {
        this(context, null);
    }

    public TaskViewHeader(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TaskViewHeader(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public TaskViewHeader(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mTaskViewRect = new Rect();
        this.mMoveTaskTargetStackId = -1;
        this.mTmpHSL = new float[3];
        this.mDimLayerPaint = new Paint();
        setWillNotDraw(false);
        Resources res = context.getResources();
        this.mLightDismissDrawable = context.getDrawable(R.drawable.recents_dismiss_light);
        this.mDarkDismissDrawable = context.getDrawable(R.drawable.recents_dismiss_dark);
        this.mCornerRadius = res.getDimensionPixelSize(R.dimen.recents_task_view_rounded_corners_radius);
        this.mHighlightHeight = res.getDimensionPixelSize(R.dimen.recents_task_view_highlight);
        this.mTaskBarViewLightTextColor = context.getColor(R.color.recents_task_bar_light_text_color);
        this.mTaskBarViewDarkTextColor = context.getColor(R.color.recents_task_bar_dark_text_color);
        this.mLightFreeformIcon = context.getDrawable(R.drawable.recents_move_task_freeform_light);
        this.mDarkFreeformIcon = context.getDrawable(R.drawable.recents_move_task_freeform_dark);
        this.mLightFullscreenIcon = context.getDrawable(R.drawable.recents_move_task_fullscreen_light);
        this.mDarkFullscreenIcon = context.getDrawable(R.drawable.recents_move_task_fullscreen_dark);
        this.mLightInfoIcon = context.getDrawable(R.drawable.recents_info_light);
        this.mDarkInfoIcon = context.getDrawable(R.drawable.recents_info_dark);
        this.mDisabledTaskBarBackgroundColor = context.getColor(R.color.recents_task_bar_disabled_background_color);
        this.mBackground = new HighlightColorDrawable();
        this.mBackground.setColorAndDim(Color.argb(255, 0, 0, 0), 0.0f);
        this.mOverlayBackground = new HighlightColorDrawable();
        this.mDimLayerPaint.setColor(Color.argb(255, 0, 0, 0));
        this.mDimLayerPaint.setAntiAlias(true);
    }

    public void reset() {
        setAlpha(1.0f);
        hideAppOverlay(true);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        SystemServicesProxy systemServices = Recents.getSystemServices();
        this.mIconView = (ImageView) findViewById(R.id.icon);
        this.mIconView.setOnLongClickListener(this);
        this.mTitleView = (TextView) findViewById(R.id.title);
        this.mLockedView = (ImageView) findViewById(R.id.locked_flag);
        onConfigurationChanged();
    }

    private void updateLayoutParams(View icon, View title, View secondaryButton, View button) {
        setLayoutParams(new FrameLayout.LayoutParams(-1, this.mHeaderBarHeight, 48));
        icon.setLayoutParams(new FrameLayout.LayoutParams(this.mHeaderBarHeight, this.mHeaderBarHeight, 8388611));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-2, -2, 8388627);
        lp.setMarginStart(this.mHeaderBarHeight);
        lp.setMarginEnd(this.mHeaderBarHeight / 2);
        title.setLayoutParams(lp);
        if (secondaryButton != null) {
            FrameLayout.LayoutParams lp2 = new FrameLayout.LayoutParams(this.mHeaderBarHeight, this.mHeaderBarHeight, 8388613);
            lp2.setMarginEnd(this.mHeaderBarHeight);
            secondaryButton.setLayoutParams(lp2);
            secondaryButton.setPadding(this.mHeaderButtonPadding, this.mHeaderButtonPadding, this.mHeaderButtonPadding, this.mHeaderButtonPadding);
        }
        if (button != null) {
            button.setLayoutParams(new FrameLayout.LayoutParams(this.mHeaderBarHeight, this.mHeaderBarHeight, 8388613));
            button.setPadding(this.mHeaderButtonPadding, this.mHeaderButtonPadding, this.mHeaderButtonPadding, this.mHeaderButtonPadding);
        }
    }

    public void onConfigurationChanged() {
        Resources resources = getResources();
        int headerBarHeight = TaskStackLayoutAlgorithm.getDimensionForDevice(getContext(), R.dimen.recents_task_view_header_height, R.dimen.recents_task_view_header_height, R.dimen.recents_task_view_header_height, R.dimen.recents_task_view_header_height_tablet_land, R.dimen.recents_task_view_header_height, R.dimen.recents_task_view_header_height_tablet_land);
        int headerButtonPadding = TaskStackLayoutAlgorithm.getDimensionForDevice(getContext(), R.dimen.recents_task_view_header_button_padding, R.dimen.recents_task_view_header_button_padding, R.dimen.recents_task_view_header_button_padding, R.dimen.recents_task_view_header_button_padding_tablet_land, R.dimen.recents_task_view_header_button_padding, R.dimen.recents_task_view_header_button_padding_tablet_land);
        if (headerBarHeight != this.mHeaderBarHeight || headerButtonPadding != this.mHeaderButtonPadding) {
            this.mHeaderBarHeight = headerBarHeight;
            this.mHeaderButtonPadding = headerButtonPadding;
            updateLayoutParams(this.mIconView, this.mTitleView, null, null);
            if (this.mAppOverlayView != null) {
                updateLayoutParams(this.mAppIconView, this.mAppTitleView, null, this.mAppInfoView);
            }
        }
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        onTaskViewSizeChanged(this.mTaskViewRect.width(), this.mTaskViewRect.height());
        int x = this.mHeaderBarHeight + this.mTitleView.getWidth() + 10;
        if (isLayoutRtl()) {
            x = (getMeasuredWidth() - x) - this.mLockedView.getMeasuredWidth();
        }
        this.mLockedView.layout(x, this.mLockedView.getTop(), this.mLockedView.getMeasuredWidth() + x, this.mLockedView.getBottom());
    }

    public void onTaskViewSizeChanged(int width, int height) {
        this.mTaskViewRect.set(0, 0, width, height);
        setLeftTopRightBottom(0, 0, width, getMeasuredHeight());
    }

    public void startFocusTimerIndicator(int duration) {
        if (this.mFocusTimerIndicator != null) {
            this.mFocusTimerIndicator.setVisibility(0);
            this.mFocusTimerIndicator.setMax(duration);
            this.mFocusTimerIndicator.setProgress(duration);
            if (this.mFocusTimerCountDown != null) {
                this.mFocusTimerCountDown.cancel();
            }
            AnonymousClass1 r1 = new CountDownTimer((long) duration, 30) {
                public void onTick(long millisUntilFinished) {
                    TaskViewHeader.this.mFocusTimerIndicator.setProgress((int) millisUntilFinished);
                }

                public void onFinish() {
                }
            };
            this.mFocusTimerCountDown = r1.start();
        }
    }

    public void cancelFocusTimerIndicator() {
        if (!(this.mFocusTimerIndicator == null || this.mFocusTimerCountDown == null)) {
            this.mFocusTimerCountDown.cancel();
            this.mFocusTimerIndicator.setProgress(0);
            this.mFocusTimerIndicator.setVisibility(4);
        }
    }

    /* access modifiers changed from: package-private */
    public int getSecondaryColor(int primaryColor, boolean useLightOverlayColor) {
        return Utilities.getColorWithOverlay(primaryColor, useLightOverlayColor ? -1 : -16777216, 0.8f);
    }

    public void setDimAlpha(float dimAlpha) {
        if (Float.compare(this.mDimAlpha, dimAlpha) != 0) {
            this.mDimAlpha = dimAlpha;
            this.mTitleView.setAlpha(1.0f - dimAlpha);
            updateBackgroundColor(this.mBackground.getColor(), dimAlpha);
        }
    }

    private void updateBackgroundColor(int color, float dimAlpha) {
        if (this.mTask != null) {
            this.mBackground.setColorAndDim(color, dimAlpha);
            ColorUtils.colorToHSL(color, this.mTmpHSL);
            this.mTmpHSL[2] = Math.min(1.0f, this.mTmpHSL[2] + (-0.0625f * (1.0f - dimAlpha)));
            this.mOverlayBackground.setColorAndDim(ColorUtils.HSLToColor(this.mTmpHSL), dimAlpha);
            this.mDimLayerPaint.setAlpha((int) (255.0f * dimAlpha));
            invalidate();
        }
    }

    public void bindToTask(Task t, boolean touchExplorationEnabled, boolean disabledInSafeMode) {
        int primaryColor;
        this.mTask = t;
        if (disabledInSafeMode) {
            primaryColor = this.mDisabledTaskBarBackgroundColor;
        } else {
            primaryColor = t.colorPrimary;
        }
        if (this.mBackground.getColor() != primaryColor) {
            updateBackgroundColor(primaryColor, this.mDimAlpha);
        }
        if (!this.mTitleView.getText().toString().equals(t.title)) {
            this.mTitleView.setText(t.title);
        }
        if (Recents.getDebugFlags().isFastToggleRecentsEnabled()) {
            if (this.mFocusTimerIndicator == null) {
                this.mFocusTimerIndicator = (ProgressBar) Utilities.findViewStubById((View) this, (int) R.id.focus_timer_indicator_stub).inflate();
            }
            this.mFocusTimerIndicator.getProgressDrawable().setColorFilter(getSecondaryColor(t.colorPrimary, t.useLightOnPrimaryColor), PorterDuff.Mode.SRC_IN);
        }
        if (touchExplorationEnabled) {
            this.mIconView.setContentDescription(t.appInfoDescription);
            this.mIconView.setOnClickListener(this);
            this.mIconView.setClickable(true);
        }
    }

    public void onTaskDataLoaded() {
        if (this.mTask.icon != null) {
            this.mIconView.setImageDrawable(this.mTask.icon);
        }
    }

    /* access modifiers changed from: package-private */
    public void unbindFromTask(boolean touchExplorationEnabled) {
        this.mTask = null;
        this.mIconView.setImageDrawable(null);
        if (touchExplorationEnabled) {
            this.mIconView.setClickable(false);
        }
    }

    /* access modifiers changed from: package-private */
    public void startNoUserInteractionAnimation() {
    }

    /* access modifiers changed from: package-private */
    public void setNoUserInteractionState() {
    }

    /* access modifiers changed from: package-private */
    public void resetNoUserInteractionState() {
    }

    /* access modifiers changed from: protected */
    public int[] onCreateDrawableState(int extraSpace) {
        return new int[0];
    }

    public void onClick(View v) {
        if (v == this.mIconView) {
            RecentsEventBus.getDefault().send(new ShowApplicationInfoEvent(this.mTask));
        } else if (v == this.mAppInfoView) {
            RecentsEventBus.getDefault().send(new ShowApplicationInfoEvent(this.mTask));
        } else if (v == this.mAppIconView) {
            hideAppOverlay(false);
        }
    }

    public boolean onLongClick(View v) {
        if (v == this.mIconView || v != this.mAppIconView) {
            return false;
        }
        hideAppOverlay(false);
        return true;
    }

    private void hideAppOverlay(boolean immediate) {
        if (this.mAppOverlayView != null) {
            if (immediate) {
                this.mAppOverlayView.setVisibility(8);
            } else {
                Animator revealAnim = ViewAnimationUtils.createCircularReveal(this.mAppOverlayView, this.mIconView.getLeft() + (this.mIconView.getWidth() / 2), this.mIconView.getTop() + (this.mIconView.getHeight() / 2), (float) getWidth(), 0.0f);
                revealAnim.setDuration(250);
                revealAnim.setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN);
                revealAnim.addListener(new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator animation) {
                        TaskViewHeader.this.mAppOverlayView.setVisibility(8);
                    }
                });
                revealAnim.start();
            }
        }
    }

    public void updateLockedFlagVisible(final boolean visible, boolean withAnim, long startDelay) {
        float f = 1.0f;
        if (withAnim) {
            ViewPropertyAnimator animate = this.mLockedView.animate();
            if (!visible) {
                f = 0.0f;
            }
            animate.alpha(f).setStartDelay(startDelay).setDuration(250).setListener(new AnimatorListenerAdapter() {
                public void onAnimationStart(Animator animation) {
                    if (visible) {
                        TaskViewHeader.this.mLockedView.setVisibility(0);
                    }
                }

                public void onAnimationEnd(Animator animation) {
                    if (!visible) {
                        TaskViewHeader.this.mLockedView.setVisibility(4);
                    }
                }
            }).start();
            return;
        }
        if (visible) {
            this.mLockedView.setAlpha(1.0f);
        }
        this.mLockedView.setVisibility(visible ? 0 : 4);
    }
}
