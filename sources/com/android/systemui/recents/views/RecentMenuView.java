package com.android.systemui.recents.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapCompat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.RecentsConfiguration;
import com.android.systemui.recents.events.RecentsEventBus;
import com.android.systemui.recents.events.activity.HideMemoryAndDockEvent;
import com.android.systemui.recents.events.activity.ShowMemoryAndDockEvent;
import com.android.systemui.recents.events.activity.ShowTaskMenuEvent;
import com.android.systemui.recents.events.component.ChangeTaskLockStateEvent;
import com.android.systemui.recents.events.component.RecentsVisibilityChangedEvent;
import com.android.systemui.recents.events.ui.ShowApplicationInfoEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragDropTargetChangedEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragEndEvent;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.model.TaskStack;
import com.android.systemui.stackdivider.events.StartedDragingEvent;
import miui.graphics.BitmapFactory;
import miui.util.ScreenshotUtils;
import miui.view.animation.BackEaseOutInterpolator;

public class RecentMenuView extends FrameLayout implements View.OnClickListener {
    /* access modifiers changed from: private */
    public Bitmap mBlurBackground;
    private final int mFastBlurMaxRadius;
    /* access modifiers changed from: private */
    public boolean mIsShowing;
    private boolean mIsTaskViewLeft;
    Drawable mLockDrawable;
    /* access modifiers changed from: private */
    public ColorDrawable mMaskBackground;
    private ImageView mMenuItemInfo;
    private ImageView mMenuItemLock;
    private ImageView mMenuItemMultiWindow;
    private TimeInterpolator mShowMenuItemAnimInterpolator;
    ValueAnimator mShowOrHideAnim;
    /* access modifiers changed from: private */
    public Task mTask;
    /* access modifiers changed from: private */
    public TaskStackView mTaskStackView;
    /* access modifiers changed from: private */
    public TaskView mTaskView;
    Drawable mUnlockDrawable;
    private int mVerticalMargin;

    public RecentMenuView(Context context) {
        this(context, null);
    }

    public RecentMenuView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecentMenuView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public RecentMenuView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mIsShowing = false;
        this.mBlurBackground = null;
        this.mShowOrHideAnim = new ValueAnimator();
        this.mShowMenuItemAnimInterpolator = new BackEaseOutInterpolator();
        this.mFastBlurMaxRadius = 36;
        this.mLockDrawable = context.getResources().getDrawable(R.drawable.ic_task_lock);
        this.mUnlockDrawable = context.getResources().getDrawable(R.drawable.ic_task_unlock);
        this.mMaskBackground = new ColorDrawable(getResources().getColor(R.color.recent_menu_mask_color));
        this.mVerticalMargin = context.getResources().getDimensionPixelSize(R.dimen.recents_task_menu_vertical_margin);
        setTranslationZ(10.0f);
        setVisibility(8);
        this.mShowOrHideAnim = new ValueAnimator();
        this.mShowOrHideAnim.setDuration(180);
        this.mShowOrHideAnim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animation) {
                if (RecentMenuView.this.mIsShowing) {
                    RecentMenuView.this.setMaskBackground();
                    RecentMenuView.this.mTaskView.setTranslationZ(10.0f);
                    RecentMenuView.this.mTaskStackView.getMask().setAlpha(1.0f);
                    RecentMenuView.this.mTaskStackView.getMask().setTranslationZ(5.0f);
                    RecentsEventBus.getDefault().send(new HideMemoryAndDockEvent());
                }
            }

            public void onAnimationEnd(Animator animation) {
                if (!RecentMenuView.this.mIsShowing) {
                    RecentMenuView.this.mTaskView.setTranslationZ(0.0f);
                    RecentMenuView.this.mTaskStackView.getMask().setAlpha(0.0f);
                    RecentMenuView.this.mTaskStackView.getMask().setTranslationZ(0.0f);
                    Bitmap unused = RecentMenuView.this.mBlurBackground = null;
                    RecentMenuView.this.mTaskView.getHeaderView().setAlpha(1.0f);
                    RecentMenuView.this.setVisibility(8);
                    RecentsEventBus.getDefault().send(new ShowMemoryAndDockEvent());
                }
            }
        });
        this.mShowOrHideAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = ((Float) animation.getAnimatedValue()).floatValue();
                int radius = (int) ((36.0f * value) + 1.0f);
                RecentMenuView.this.mMaskBackground.setAlpha((int) (255.0f * value));
                if (RecentMenuView.this.mBlurBackground != null) {
                    Drawable[] drawableArr = new Drawable[2];
                    drawableArr[0] = new BitmapDrawable(BitmapCompat.isConfigHardware(RecentMenuView.this.mBlurBackground) ? RecentMenuView.this.mBlurBackground : BitmapFactory.fastBlur(RecentMenuView.this.mBlurBackground, radius));
                    drawableArr[1] = RecentMenuView.this.mMaskBackground;
                    RecentMenuView.this.mTaskStackView.getMask().setBackground(new LayerDrawable(drawableArr));
                } else {
                    RecentMenuView.this.mTaskStackView.getMask().setBackground(RecentMenuView.this.mMaskBackground);
                }
                RecentMenuView.this.mTaskView.getHeaderView().setAlpha(1.0f - value);
            }
        });
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        this.mMenuItemInfo = (ImageView) findViewById(R.id.menu_item_info);
        this.mMenuItemLock = (ImageView) findViewById(R.id.menu_item_lock);
        this.mMenuItemMultiWindow = (ImageView) findViewById(R.id.menu_item_multi_window);
        this.mMenuItemInfo.setImageResource(R.drawable.ic_task_setting);
        this.mMenuItemInfo.setContentDescription(this.mContext.getString(R.string.recent_menu_item_info));
        this.mMenuItemMultiWindow.setImageResource(R.drawable.ic_task_multi);
        this.mMenuItemInfo.setOnClickListener(this);
        this.mMenuItemLock.setOnClickListener(this);
        this.mMenuItemMultiWindow.setOnClickListener(this);
        setOnClickListener(this);
    }

    public void onClick(View v) {
        String str;
        switch (v.getId()) {
            case R.id.menu_item_info:
                if (this.mTask != null) {
                    RecentsEventBus.getDefault().send(new ShowApplicationInfoEvent(this.mTask));
                    break;
                }
                break;
            case R.id.menu_item_lock:
                if (this.mTask != null) {
                    this.mTask.isLocked = !this.mTask.isLocked;
                    this.mTaskView.updateLockedFlagVisible(this.mTask.isLocked, true, 200);
                    RecentsEventBus.getDefault().send(new ChangeTaskLockStateEvent(this.mTask, this.mTask.isLocked));
                    if (this.mTask.isLocked) {
                        str = this.mContext.getString(R.string.accessibility_recent_task_locked_state);
                    } else {
                        str = this.mContext.getString(R.string.accessibility_recent_task_unlocked);
                    }
                    announceForAccessibility(str);
                    break;
                }
                break;
            case R.id.menu_item_multi_window:
                final TaskStack.DockState[] dockStates = getDockStatesForCurrentOrientation();
                if (dockStates[0] != null) {
                    this.mTaskStackView.postDelayed(new Runnable() {
                        public void run() {
                            if (!Recents.getSystemServices().hasDockedTask()) {
                                RecentMenuView.this.mTaskStackView.addIgnoreTask(RecentMenuView.this.mTask);
                                RecentsEventBus.getDefault().send(new DragDropTargetChangedEvent(RecentMenuView.this.mTask, dockStates[0]));
                                RecentsEventBus.getDefault().send(new DragEndEvent(RecentMenuView.this.mTask, RecentMenuView.this.mTaskView, dockStates[0]));
                                RecentMenuView.this.announceForAccessibility(RecentMenuView.this.mContext.getString(R.string.accessibility_splite_screen_primary));
                                return;
                            }
                            RecentMenuView.this.mTaskView.onClick(RecentMenuView.this.mTaskView);
                            RecentMenuView.this.announceForAccessibility(RecentMenuView.this.mContext.getString(R.string.accessibility_splite_screen_secondary));
                        }
                    }, 250);
                    break;
                }
                break;
        }
        removeMenu(true);
    }

    /* access modifiers changed from: private */
    public void setMaskBackground() {
        this.mBlurBackground = ScreenshotUtils.getScreenshot(getContext(), 0.25f, 0, 30000, true);
        if (Recents.getSystemServices().hasDockedTask() && this.mBlurBackground != null) {
            Rect rect = new Rect();
            this.mTaskStackView.getBoundsOnScreen(rect);
            rect.scale(0.25f);
            try {
                this.mBlurBackground = Bitmap.createBitmap(this.mBlurBackground, rect.left, rect.top, rect.width(), rect.height());
            } catch (IllegalArgumentException e) {
                Log.e("RecentMenuView", "Get blur menu background error: rect=" + rect + "   ScreenshotWidth=" + this.mBlurBackground.getWidth() + "   ScreenshotHeight=" + this.mBlurBackground.getHeight(), e);
                this.mBlurBackground = null;
            }
        }
    }

    public TaskStack.DockState[] getDockStatesForCurrentOrientation() {
        boolean isLandscape = getResources().getConfiguration().orientation == 2;
        RecentsConfiguration config = Recents.getConfiguration();
        return isLandscape ? config.isLargeScreen ? DockRegion.TABLET_LANDSCAPE : DockRegion.PHONE_LANDSCAPE : config.isLargeScreen ? DockRegion.TABLET_PORTRAIT : DockRegion.PHONE_PORTRAIT;
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        RecentsEventBus.getDefault().register(this, 3);
        super.onAttachedToWindow();
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        RecentsEventBus.getDefault().unregister(this);
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int pivotY;
        int pivotX;
        int pivotY2;
        int pivotX2;
        int pivotX3;
        int pivotY3;
        int pivotX4;
        int pivotY4;
        int i = bottom;
        int size = this.mMenuItemLock.getMeasuredWidth();
        Rect content = new Rect();
        this.mTaskView.getHitRect(content);
        content.top += this.mTaskView.getHeaderView().getHeight();
        content.intersect(left, top, right, i);
        int[] posX = new int[3];
        int[] posY = new int[3];
        if (this.mIsTaskViewLeft) {
            int i2 = (int) (((float) content.right) + (((float) size) * 0.4f));
            posX[2] = i2;
            posX[0] = i2;
            posX[1] = (int) (((float) content.right) + (0.9f * ((float) size)));
            pivotX = content.right - size;
            pivotY = content.centerY();
        } else {
            int i3 = (int) (((float) content.left) - (((float) size) * 1.4f));
            posX[2] = i3;
            posX[0] = i3;
            posX[1] = (int) (((float) content.left) - (1.9f * ((float) size)));
            pivotX = content.left + size;
            pivotY = content.centerY();
        }
        posY[1] = (int) (((float) content.centerY()) - (0.5f * ((float) size)));
        posY[0] = (int) (((double) posY[1]) - (((double) size) * 1.2d));
        posY[2] = (int) (((double) posY[1]) + (1.2d * ((double) size)));
        if (posY[0] < this.mVerticalMargin) {
            if (this.mIsTaskViewLeft) {
                posX[0] = (int) (((float) content.right) + (0.6f * ((float) size)));
                posX[1] = (int) (((float) content.right) + (((float) size) * 0.4f));
                posX[2] = content.right - size;
                pivotX4 = content.right - (2 * size);
                pivotY4 = content.bottom - (2 * size);
            } else {
                posX[0] = (int) (((float) content.left) - (1.6f * ((float) size)));
                posX[1] = (int) (((float) content.left) - (((float) size) * 1.4f));
                posX[2] = content.left;
                pivotX4 = content.left + (2 * size);
                pivotY4 = content.bottom - (2 * size);
            }
            posY[0] = content.bottom - size;
            posY[1] = (int) (((float) content.bottom) + (((float) size) * 0.4f));
            pivotY2 = pivotY4;
            pivotX2 = pivotX4;
            posY[2] = (int) (((double) content.bottom) + (0.6d * ((double) size)));
        } else if (posY[2] + size > i - this.mVerticalMargin) {
            if (this.mIsTaskViewLeft) {
                posX[0] = content.right - size;
                posX[1] = (int) (((float) content.right) + (((float) size) * 0.4f));
                posX[2] = (int) (((float) content.right) + (0.6f * ((float) size)));
                pivotX3 = content.right - (2 * size);
                pivotY3 = content.top + (2 * size);
            } else {
                posX[0] = content.left;
                posX[1] = (int) (((float) content.left) - (((float) size) * 1.4f));
                posX[2] = (int) (((float) content.left) - (((float) size) * 1.6f));
                pivotX3 = content.left + (2 * size);
                pivotY3 = content.top + (2 * size);
            }
            posY[0] = (int) (((float) content.top) - (1.6f * ((float) size)));
            posY[1] = (int) (((float) content.top) - (((float) size) * 1.4f));
            posY[2] = content.top;
            pivotY2 = pivotY3;
            pivotX2 = pivotX3;
        } else {
            pivotX2 = pivotX;
            pivotY2 = pivotY;
        }
        posX[0] = Math.max(10, Math.min(posX[0], r8 - 10));
        posX[1] = Math.max(10, Math.min(posX[1], r8 - 10));
        posX[2] = Math.max(10, Math.min(posX[2], r8 - 10));
        int i4 = pivotX2;
        int i5 = pivotY2;
        int i6 = size;
        layoutMenuItem(this.mMenuItemLock, posX[0], posY[0], i4, i5, i6);
        layoutMenuItem(this.mMenuItemMultiWindow, posX[1], posY[1], i4, i5, i6);
        layoutMenuItem(this.mMenuItemInfo, posX[2], posY[2], i4, i5, i6);
    }

    private void layoutMenuItem(View view, int x, int y, int pivotX, int pivotY, int size) {
        view.setPivotX((float) (pivotX - x));
        view.setPivotY((float) (pivotY - y));
        view.layout(x, y, x + size, y + size);
    }

    public final void onBusEvent(ShowTaskMenuEvent event) {
        String str;
        String str2;
        if (!this.mIsShowing) {
            this.mIsShowing = true;
            this.mTaskStackView.setIsShowingMenu(true);
            this.mTaskView = event.taskView;
            this.mTask = this.mTaskView.getTask();
            this.mMenuItemMultiWindow.setEnabled(this.mTask.isDockable && Utilities.supportsMultiWindow());
            this.mMenuItemLock.setImageDrawable(this.mTask.isLocked ? this.mUnlockDrawable : this.mLockDrawable);
            ImageView imageView = this.mMenuItemLock;
            if (this.mTask.isLocked) {
                str = this.mContext.getString(R.string.recent_menu_item_unlock);
            } else {
                str = this.mContext.getString(R.string.recent_menu_item_lock);
            }
            imageView.setContentDescription(str);
            this.mMenuItemMultiWindow.setImageAlpha(this.mMenuItemMultiWindow.isEnabled() ? 255 : 80);
            ImageView imageView2 = this.mMenuItemMultiWindow;
            if (this.mMenuItemMultiWindow.isEnabled()) {
                str2 = this.mContext.getString(R.string.accessibility_menu_item_split_enable);
            } else {
                str2 = this.mContext.getString(R.string.accessibility_menu_item_split_disable);
            }
            imageView2.setContentDescription(str2);
            this.mIsTaskViewLeft = this.mTaskStackView.getTaskViews().size() > 1 && this.mTaskView.getLeft() < this.mTaskStackView.getWidth() - this.mTaskView.getRight();
            setVisibility(0);
            setFocusable(true);
            startShowItemAnim(this.mMenuItemLock, 1.0f, 0);
            startShowItemAnim(this.mMenuItemMultiWindow, 1.0f, 50);
            startShowItemAnim(this.mMenuItemInfo, 1.0f, 100);
            this.mShowOrHideAnim.setFloatValues(new float[]{0.0f, 1.0f});
            this.mShowOrHideAnim.start();
            this.mTaskView.animate().setListener(null).setDuration(240).setInterpolator(Interpolators.BACK_EASE_OUT_5).scaleX(1.06f).scaleY(1.06f).start();
            for (TaskView tv : this.mTaskStackView.getTaskViews()) {
                tv.setImportantForAccessibility(4);
            }
        }
    }

    private void startShowItemAnim(View view, float alpha, long delay) {
        view.setAlpha(0.0f);
        view.setScaleX(0.6f);
        view.setScaleY(0.6f);
        view.animate().alpha(alpha).scaleX(1.0f).scaleY(1.0f).setDuration(240).setStartDelay(delay).setInterpolator(this.mShowMenuItemAnimInterpolator).start();
    }

    public final void onBusEvent(RecentsVisibilityChangedEvent event) {
        if (!event.visible) {
            removeMenu(false);
        }
    }

    public final void onBusEvent(StartedDragingEvent event) {
        removeMenu(false);
    }

    public boolean removeMenu(boolean withAnim) {
        if (!this.mIsShowing) {
            return false;
        }
        this.mIsShowing = false;
        this.mTaskStackView.setIsShowingMenu(false);
        if (withAnim) {
            this.mMenuItemLock.animate().alpha(0.0f).scaleX(0.6f).scaleY(0.6f).setStartDelay(0).setDuration(200).start();
            this.mMenuItemMultiWindow.animate().alpha(0.0f).scaleX(0.6f).scaleY(0.6f).setStartDelay(0).setDuration(200).start();
            this.mMenuItemInfo.animate().alpha(0.0f).scaleX(0.6f).scaleY(0.6f).setStartDelay(0).setDuration(200).start();
        }
        this.mShowOrHideAnim.setFloatValues(new float[]{1.0f, 0.0f});
        this.mShowOrHideAnim.start();
        this.mTaskView.animate().setListener(null).setDuration(240).scaleX(1.0f).scaleY(1.0f).setInterpolator(Interpolators.EASE_OUT).start();
        for (TaskView tv : this.mTaskStackView.getTaskViews()) {
            tv.setImportantForAccessibility(0);
        }
        this.mTaskView.sendAccessibilityEvent(8);
        return true;
    }

    public void setTaskStackView(TaskStackView taskStackView) {
        this.mTaskStackView = taskStackView;
    }

    public boolean isShowing() {
        return this.mIsShowing;
    }

    public boolean isShowOrHideAnimRunning() {
        return this.mShowOrHideAnim.isRunning();
    }
}
