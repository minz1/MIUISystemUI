package com.android.systemui.miui.volume;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import com.android.systemui.Constants;
import com.android.systemui.Interpolators;
import com.android.systemui.Logger;
import com.android.systemui.miui.ViewStateGroup;
import com.android.systemui.statistic.ScenarioConstants;
import com.android.systemui.statistic.ScenarioTrackUtil;
import com.android.systemui.volume.VolumeDialogMotion;
import java.util.ArrayList;
import java.util.List;

public class MiuiVolumeDialogMotion {
    private static final String TAG = MiuiVolumeDialogMotion.class.getSimpleName();
    private boolean mAnimating;
    /* access modifiers changed from: private */
    public Callback mCallback;
    /* access modifiers changed from: private */
    public Animator mCollapseAnimator;
    private ViewStateGroup mCollapsedStates;
    private Context mContext;
    private List<View> mCornerBgViews = new ArrayList();
    private float mCornerRadiusCollapsed;
    private float mCornerRadiusExpanded;
    private View mDialogContentView;
    /* access modifiers changed from: private */
    public View mDialogView;
    private boolean mDismissing;
    private Display mDisplay;
    /* access modifiers changed from: private */
    public float mElevationCollapsed;
    /* access modifiers changed from: private */
    public Animator mExpandAnimator;
    private View mExpandButton;
    private boolean mExpanded;
    private ViewStateGroup mExpandedStates;
    private View mRingerModeLayout;
    private boolean mShowing;
    private FrameLayout mTempColumnContainer;

    public interface Callback extends VolumeDialogMotion.Callback {
        void onDismiss();

        void onShow();
    }

    public MiuiVolumeDialogMotion(View dialogView, ViewGroup contents, FrameLayout tempColumnContainer, View chevron, View footer) {
        this.mContext = dialogView.getContext();
        this.mDialogView = dialogView;
        this.mDialogContentView = contents;
        this.mTempColumnContainer = tempColumnContainer;
        this.mExpandButton = chevron;
        this.mRingerModeLayout = footer;
        this.mCornerRadiusExpanded = (float) this.mContext.getResources().getDimensionPixelSize(R.dimen.miui_volume_bg_radius_expanded);
        this.mCornerRadiusCollapsed = (float) this.mContext.getResources().getDimensionPixelSize(R.dimen.miui_volume_bg_radius);
        this.mElevationCollapsed = this.mContext.getResources().getDimension(R.dimen.miui_volume_elevation_collapsed);
        setupAnimationInfo();
        setupStates();
    }

    public void setCallback(Callback callback) {
        this.mCallback = callback;
    }

    public void setDisplay(Display display) {
        this.mDisplay = display;
        if (display != null) {
            updateStates();
        }
    }

    public void updateStates() {
        setupStates();
        if (this.mExpanded) {
            this.mExpandedStates.apply((ViewGroup) this.mDialogView);
        } else {
            this.mCollapsedStates.apply((ViewGroup) this.mDialogView);
        }
    }

    private void setupAnimationInfo() {
        this.mCornerBgViews.add(this.mDialogContentView);
        this.mCornerBgViews.add(this.mRingerModeLayout);
        this.mCornerBgViews.add(this.mTempColumnContainer);
        if (this.mExpandAnimator == null) {
            this.mExpandAnimator = AnimatorInflater.loadAnimator(this.mContext, R.animator.miui_volume_bg_expand);
        }
        if (this.mCollapseAnimator == null) {
            this.mCollapseAnimator = AnimatorInflater.loadAnimator(this.mContext, R.animator.miui_volume_bg_collapse);
        }
    }

    private void setupStates() {
        int dialogMarginTopExpanded;
        int dialogMarginLeftExpanded;
        Resources resources = this.mContext.getResources();
        boolean landscape = resources.getConfiguration().orientation == 2;
        int offsetEndCollapsed = resources.getDimensionPixelSize(R.dimen.miui_volume_offset_end);
        if (landscape && Constants.IS_NOTCH && this.mDisplay != null) {
            if (this.mDisplay.getRotation() == 3) {
                offsetEndCollapsed += resources.getDimensionPixelSize(R.dimen.miui_volume_status_bar_height);
            }
        }
        this.mCollapsedStates = new ViewStateGroup.Builder(this.mContext).addStateWithIntRes(this.mDialogView.getId(), 1, R.integer.miui_volume_dialog_gravity_collapsed).addState(this.mDialogView.getId(), 11, 1).addStateWithIntDimen(this.mDialogView.getId(), 6, R.dimen.miui_volume_offset_top_collapsed).addState(this.mDialogView.getId(), 5, 0).addState(this.mDialogView.getId(), 7, offsetEndCollapsed).addState(this.mDialogView.getId(), 12, 8388613).addState(this.mDialogContentView.getId(), 2, -2).addState(this.mDialogContentView.getId(), 3, -2).addState(this.mRingerModeLayout.getId(), 2, -2).addState(this.mRingerModeLayout.getId(), 3, -2).addState(this.mRingerModeLayout.getId(), 11, 1).addStateWithIntDimen(this.mRingerModeLayout.getId(), 6, R.dimen.miui_volume_footer_margin_top).addStateWithIntDimen(this.mRingerModeLayout.getId(), 5, R.dimen.miui_volume_footer_margin_left).addState(this.mExpandButton.getId(), 10, 0).addStateWithIntDimen(R.id.miui_ringer_state_layout, 2, R.dimen.miui_volume_silence_button_height).addStateWithIntDimen(R.id.miui_ringer_state_layout, 3, R.dimen.miui_volume_silence_button_height).addState(R.id.miui_volume_ringer_divider, 10, 8).addStateWithIntDimen(this.mRingerModeLayout.getId(), 9, R.dimen.miui_volume_bg_padding).build();
        if (this.mContext.getResources().getBoolean(R.bool.miui_volume_expand_freeland)) {
            dialogMarginTopExpanded = (int) resources.getDimension(R.dimen.miui_volume_offset_top_collapsed);
            dialogMarginLeftExpanded = 0;
        } else if (landscape) {
            dialogMarginLeftExpanded = ((int) (((((float) resources.getDisplayMetrics().widthPixels) - resources.getDimension(R.dimen.miui_volume_content_width_expanded)) - resources.getDimension(R.dimen.miui_volume_ringer_btn_layout_width)) - resources.getDimension(R.dimen.miui_volume_footer_margin_left_expanded))) / 2;
            dialogMarginTopExpanded = 0;
        } else {
            dialogMarginTopExpanded = ((int) (((((float) resources.getDisplayMetrics().heightPixels) - resources.getDimension(R.dimen.miui_volume_content_height_expanded)) - resources.getDimension(R.dimen.miui_volume_ringer_btn_layout_height)) - resources.getDimension(R.dimen.miui_volume_footer_margin_top_expanded))) / 2;
            dialogMarginLeftExpanded = 0;
        }
        this.mExpandedStates = new ViewStateGroup.Builder(this.mContext).addStateWithIntRes(this.mDialogView.getId(), 1, R.integer.miui_volume_dialog_gravity_expanded).addStateWithIntRes(this.mDialogView.getId(), 11, R.integer.miui_volume_layout_orientation_expanded).addState(this.mDialogView.getId(), 6, dialogMarginTopExpanded).addState(this.mDialogView.getId(), 5, dialogMarginLeftExpanded).addStateWithIntDimen(this.mDialogView.getId(), 7, R.dimen.miui_volume_offset_end_expanded).addState(this.mDialogView.getId(), 12, 1).addStateWithIntDimen(this.mDialogContentView.getId(), 2, R.dimen.miui_volume_content_width_expanded).addStateWithIntDimen(this.mDialogContentView.getId(), 3, R.dimen.miui_volume_content_height_expanded).addStateWithIntDimen(this.mRingerModeLayout.getId(), 2, R.dimen.miui_volume_ringer_layout_width_expanded).addStateWithIntDimen(this.mRingerModeLayout.getId(), 3, R.dimen.miui_volume_ringer_layout_height_expanded).addStateWithIntRes(this.mRingerModeLayout.getId(), 11, R.integer.miui_volume_layout_orientation_expanded).addStateWithIntDimen(this.mRingerModeLayout.getId(), 6, R.dimen.miui_volume_footer_margin_top_expanded).addStateWithIntDimen(this.mRingerModeLayout.getId(), 5, R.dimen.miui_volume_footer_margin_left_expanded).addState(this.mExpandButton.getId(), 10, 8).addState(this.mTempColumnContainer.getId(), 10, 8).addStateWithIntDimen(R.id.miui_ringer_state_layout, 2, R.dimen.miui_volume_ringer_btn_layout_width).addStateWithIntDimen(R.id.miui_ringer_state_layout, 3, R.dimen.miui_volume_ringer_btn_layout_height).addState(R.id.miui_volume_ringer_divider, 10, 0).addState(this.mRingerModeLayout.getId(), 9, 0).build();
    }

    public void startExpandH(boolean expand) {
        if (this.mExpandAnimator.isRunning()) {
            this.mExpandAnimator.cancel();
        }
        if (this.mCollapseAnimator.isRunning()) {
            this.mCollapseAnimator.cancel();
        }
        if (expand) {
            this.mExpandAnimator.setTarget(this);
            this.mExpandAnimator.start();
            this.mExpandAnimator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    MiuiVolumeDialogMotion.this.mExpandAnimator.setTarget(null);
                }
            });
            this.mExpandedStates.apply((ViewGroup) this.mDialogView);
            ScenarioTrackUtil.finishScenario(ScenarioConstants.SCENARIO_EXPAND_VOLUME_DIALOG);
        } else {
            this.mCollapseAnimator.setTarget(this);
            this.mCollapseAnimator.start();
            this.mCollapseAnimator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    MiuiVolumeDialogMotion.this.mCollapseAnimator.setTarget(null);
                }
            });
            this.mCollapsedStates.apply((ViewGroup) this.mDialogView);
        }
        this.mExpanded = expand;
    }

    public void setExpandFraction(float fraction) {
        float radius = this.mCornerRadiusCollapsed + ((this.mCornerRadiusExpanded - this.mCornerRadiusCollapsed) * fraction);
        for (View v : this.mCornerBgViews) {
            applyCornerRadius(v, radius);
        }
        setViewsElevation(this.mElevationCollapsed * (1.0f - fraction));
    }

    /* access modifiers changed from: private */
    public void setViewsElevation(float elevation) {
        this.mDialogContentView.setElevation(elevation);
        this.mRingerModeLayout.setElevation(elevation);
        this.mTempColumnContainer.setElevation(elevation);
    }

    /* access modifiers changed from: private */
    public void setViewsAlpha(float alpha) {
        this.mDialogContentView.setAlpha(alpha);
        this.mRingerModeLayout.setAlpha(alpha);
        this.mTempColumnContainer.setAlpha(alpha);
    }

    private void applyCornerRadius(View view, float radius) {
        Drawable bg = view.getBackground();
        if (bg != null && (bg instanceof GradientDrawable)) {
            ((GradientDrawable) bg).setCornerRadius(radius);
        }
    }

    public boolean isAnimating() {
        return this.mAnimating;
    }

    private void setShowing(boolean showing) {
        if (showing != this.mShowing) {
            this.mShowing = showing;
            updateAnimating();
        }
    }

    /* access modifiers changed from: private */
    public void setDismissing(boolean dismissing) {
        if (dismissing != this.mDismissing) {
            this.mDismissing = dismissing;
            updateAnimating();
        }
    }

    private void updateAnimating() {
        boolean animating = this.mShowing || this.mDismissing;
        if (animating != this.mAnimating) {
            this.mAnimating = animating;
            if (this.mCallback != null) {
                this.mCallback.onAnimatingChanged(this.mAnimating);
            }
        }
    }

    public void startShow() {
        String str = TAG;
        Logger.i(str, "startShow mShowing:" + this.mShowing + " mDismissing:" + this.mDismissing);
        if (!this.mShowing) {
            setShowing(true);
            if (this.mDismissing) {
                this.mDialogView.animate().cancel();
                setDismissing(false);
                startShowAnimation();
            } else {
                this.mCallback.onShow();
                this.mDialogView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    public void onGlobalLayout() {
                        MiuiVolumeDialogMotion.this.mDialogView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        MiuiVolumeDialogMotion.this.mDialogView.setTranslationX((float) MiuiVolumeDialogMotion.this.mDialogView.getMeasuredWidth());
                        MiuiVolumeDialogMotion.this.mDialogView.requestLayout();
                        MiuiVolumeDialogMotion.this.startShowAnimation();
                    }
                });
            }
        }
    }

    /* access modifiers changed from: private */
    public void onShowAnimationEnd() {
        setShowing(false);
    }

    /* access modifiers changed from: private */
    public void startShowAnimation() {
        this.mDialogView.animate().translationX(0.0f).setDuration(300).setInterpolator(Interpolators.DECELERATE_QUART).setUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                MiuiVolumeDialogMotion.this.setViewsAlpha(animation.getAnimatedFraction());
            }
        }).withEndAction(new Runnable() {
            public void run() {
                MiuiVolumeDialogMotion.this.onShowAnimationEnd();
                MiuiVolumeDialogMotion.this.setViewsElevation(MiuiVolumeDialogMotion.this.mElevationCollapsed);
                ScenarioTrackUtil.finishScenario(ScenarioConstants.SCENARIO_VOLUME_DIALOG_SHOW);
            }
        }).start();
    }

    private void startDismissAnimation(final Runnable onComplete) {
        this.mDialogView.animate().translationX((float) this.mDialogView.getWidth()).setInterpolator(Interpolators.ACCELERATE_DECELERATE).setDuration(300).setUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                MiuiVolumeDialogMotion.this.setViewsAlpha(1.0f - animation.getAnimatedFraction());
            }
        }).withEndAction(new Runnable() {
            public void run() {
                MiuiVolumeDialogMotion.this.mDialogView.postDelayed(new Runnable() {
                    public void run() {
                        MiuiVolumeDialogMotion.this.mCallback.onDismiss();
                        onComplete.run();
                        MiuiVolumeDialogMotion.this.setDismissing(false);
                        ScenarioTrackUtil.finishScenario(ScenarioConstants.SCENARIO_VOLUME_DIALOG_HIDE);
                    }
                }, 5);
            }
        }).start();
    }

    public void startDismiss(Runnable onComplete) {
        String str = TAG;
        Logger.i(str, "startDismiss mDismissing:" + this.mDismissing + " mShowing:" + this.mShowing + " isShown:" + this.mDialogView.isShown());
        if (!this.mDismissing) {
            setDismissing(true);
            if (this.mShowing) {
                this.mDialogView.animate().cancel();
                setShowing(false);
            }
            if (this.mDialogView.isShown()) {
                startDismissAnimation(onComplete);
            } else {
                setDismissing(false);
                setShowing(false);
            }
        }
    }
}
