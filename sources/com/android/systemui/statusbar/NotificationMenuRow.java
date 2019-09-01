package com.android.systemui.statusbar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.android.systemui.Constants;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.miui.statusbar.notification.NotificationUtil;
import com.android.systemui.plugins.statusbar.NotificationMenuRowPlugin;
import com.android.systemui.plugins.statusbar.NotificationSwipeActionHelper;
import com.android.systemui.statistic.ScenarioConstants;
import com.android.systemui.statistic.ScenarioTrackUtil;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.NotificationGuts;
import com.android.systemui.statusbar.stack.NotificationStackScrollLayout;
import java.util.ArrayList;

public class NotificationMenuRow implements View.OnClickListener, NotificationMenuRowPlugin, ExpandableNotificationRow.LayoutListener {
    /* access modifiers changed from: private */
    public float mAlpha = 0.0f;
    /* access modifiers changed from: private */
    public boolean mAnimating;
    private CheckForDrag mCheckForDrag;
    private Context mContext;
    private boolean mDismissing;
    private ValueAnimator mFadeAnimator;
    private NotificationMenuRowPlugin.MenuItem mFilterItem;
    private Handler mHandler;
    private float mHorizSpaceForIcon = -1.0f;
    private int[] mIconLocation = new int[2];
    private NotificationMenuRowPlugin.MenuItem mInfoItem;
    private boolean mIsFoldInFilter;
    private boolean mIsUserFold = true;
    private FrameLayout mMenuContainer;
    /* access modifiers changed from: private */
    public boolean mMenuFadedIn;
    private ArrayList<NotificationMenuRowPlugin.MenuItem> mMenuItems;
    private NotificationMenuRowPlugin.OnMenuEventListener mMenuListener;
    private boolean mMenuSnappedTo;
    /* access modifiers changed from: private */
    public ExpandableNotificationRow mParent;
    private int[] mParentLocation = new int[2];
    private float mPrevX;
    private boolean mShouldShowMenu;
    private boolean mSnapping;
    private NotificationSwipeActionHelper mSwipeHelper;
    /* access modifiers changed from: private */
    public float mTranslation;
    private int mVertSpaceForIcons = -1;

    private final class CheckForDrag implements Runnable {
        private CheckForDrag() {
        }

        public void run() {
            float absTransX = Math.abs(NotificationMenuRow.this.mTranslation);
            float bounceBackToMenuWidth = NotificationMenuRow.this.getSpaceForMenu();
            float notiThreshold = ((float) NotificationMenuRow.this.mParent.getWidth()) * 0.4f;
            if (!NotificationMenuRow.this.isMenuVisible() && ((double) absTransX) >= ((double) bounceBackToMenuWidth) * 0.4d && absTransX < notiThreshold) {
                NotificationMenuRow.this.fadeInMenu(notiThreshold);
            }
        }
    }

    public static class NotificationMenuItem implements NotificationMenuRowPlugin.MenuItem {
        String mContentDescription;
        Context mContext;
        NotificationGuts.GutsContent mGutsContent;
        int mGutsResource;
        AlphaOptimizedImageView mMenuView;

        public NotificationMenuItem(Context context, String s, int gutsResource, int iconResId) {
            setIcon(context, iconResId);
            this.mContext = context;
            this.mContentDescription = s;
            this.mGutsResource = gutsResource;
        }

        public View getMenuView() {
            return this.mMenuView;
        }

        public View getGutsView() {
            if (this.mGutsContent == null) {
                this.mGutsContent = (NotificationGuts.GutsContent) LayoutInflater.from(this.mContext).inflate(this.mGutsResource, null, false);
            }
            return this.mGutsContent.getContentView();
        }

        public String getContentDescription() {
            return this.mContentDescription;
        }

        public void setIcon(Context context, int iconResId) {
            Resources res = context.getResources();
            if (this.mMenuView == null) {
                this.mMenuView = new AlphaOptimizedImageView(context);
                int padding = res.getDimensionPixelSize(R.dimen.notification_menu_icon_padding);
                this.mMenuView.setPadding(padding, padding, padding, padding);
                this.mMenuView.setAlpha(1.0f);
            }
            this.mMenuView.setImageDrawable(context.getResources().getDrawable(iconResId));
            this.mMenuView.setTag(Integer.valueOf(iconResId));
        }
    }

    public NotificationMenuRow(Context context) {
        this.mContext = context;
        this.mShouldShowMenu = context.getResources().getBoolean(R.bool.config_showNotificationGear);
        this.mHandler = new Handler(Looper.getMainLooper());
        this.mMenuItems = new ArrayList<>();
    }

    public ArrayList<NotificationMenuRowPlugin.MenuItem> getMenuItems(Context context) {
        return this.mMenuItems;
    }

    public NotificationMenuRowPlugin.MenuItem getLongpressMenuItem(Context context) {
        return this.mInfoItem;
    }

    public void setSwipeActionHelper(NotificationSwipeActionHelper helper) {
        this.mSwipeHelper = helper;
    }

    public void setMenuClickListener(NotificationMenuRowPlugin.OnMenuEventListener listener) {
        this.mMenuListener = listener;
    }

    public void createMenu(ViewGroup parent) {
        this.mParent = (ExpandableNotificationRow) parent;
        createMenuViews(true);
    }

    public boolean isMenuVisible() {
        return this.mAlpha > 0.0f;
    }

    public View getMenuView() {
        return this.mMenuContainer;
    }

    public void resetMenu() {
        resetState(true);
    }

    public void onNotificationUpdated() {
        if (this.mMenuContainer != null) {
            createMenuViews(!isMenuVisible());
        }
    }

    public void onConfigurationChanged() {
        this.mParent.setLayoutListener(this);
    }

    public boolean useDefaultMenuItems() {
        return false;
    }

    public int getVersion() {
        return -1;
    }

    public void onCreate(Context sysuiContext, Context pluginContext) {
    }

    public void onDestroy() {
    }

    public void onLayout() {
        setMenuLocation();
        this.mParent.removeListener();
    }

    private void createMenuViews(boolean resetState) {
        Resources res = this.mContext.getResources();
        this.mHorizSpaceForIcon = (float) res.getDimensionPixelSize(R.dimen.notification_menu_icon_size);
        this.mVertSpaceForIcons = res.getDimensionPixelSize(R.dimen.notification_min_height);
        this.mMenuItems.clear();
        if (!Constants.IS_INTERNATIONAL) {
            if (!(this.mParent == null || this.mParent.getStatusBarNotification() == null)) {
                this.mIsFoldInFilter = this.mParent.getStatusBarNotification().isFold();
            }
            this.mFilterItem = createFilterItem(this.mContext, this.mIsFoldInFilter);
            if (this.mIsUserFold) {
                this.mMenuItems.add(this.mFilterItem);
            }
        } else if (!(this.mParent == null || this.mParent.getStatusBarNotification() == null)) {
            if (!((this.mParent.getStatusBarNotification().getNotification().flags & 64) != 0)) {
                this.mMenuItems.add(createSnoozeItem(this.mContext));
            }
        }
        this.mInfoItem = createInfoItem(this.mContext);
        this.mMenuItems.add(this.mInfoItem);
        if (this.mMenuContainer != null) {
            this.mMenuContainer.removeAllViews();
        } else {
            this.mMenuContainer = new FrameLayout(this.mContext);
        }
        for (int i = 0; i < this.mMenuItems.size(); i++) {
            addMenuView(this.mMenuItems.get(i), this.mMenuContainer);
        }
        if (resetState) {
            resetState(false);
            return;
        }
        setMenuLocation();
        showMenu(this.mParent, -getSpaceForMenu(), 0.0f);
    }

    private void resetState(boolean notify) {
        setMenuAlpha(0.0f);
        this.mMenuFadedIn = false;
        this.mAnimating = false;
        this.mSnapping = false;
        this.mDismissing = false;
        this.mMenuSnappedTo = false;
        setMenuLocation();
        if (this.mMenuListener != null && notify) {
            this.mMenuListener.onMenuReset(this.mParent);
        }
    }

    public boolean onTouchEvent(View view, MotionEvent ev, float velocity) {
        switch (ev.getActionMasked()) {
            case 0:
                this.mSnapping = false;
                if (this.mFadeAnimator != null) {
                    this.mFadeAnimator.cancel();
                }
                this.mHandler.removeCallbacks(this.mCheckForDrag);
                this.mCheckForDrag = null;
                this.mPrevX = ev.getRawX();
                break;
            case 1:
                return handleUpEvent(ev, view, velocity);
            case 2:
                this.mSnapping = false;
                float rawX = ev.getRawX() - this.mPrevX;
                this.mPrevX = ev.getRawX();
                if (this.mShouldShowMenu && this.mTranslation < 0.0f && !NotificationStackScrollLayout.isPinnedHeadsUp(view) && !this.mParent.isOnKeyguard() && !this.mParent.areGutsExposed() && !this.mParent.isDark() && (this.mCheckForDrag == null || !this.mHandler.hasCallbacks(this.mCheckForDrag))) {
                    this.mCheckForDrag = new CheckForDrag();
                    this.mHandler.postDelayed(this.mCheckForDrag, 60);
                    break;
                }
        }
        return false;
    }

    private boolean handleUpEvent(MotionEvent ev, View animView, float velocity) {
        MotionEvent motionEvent = ev;
        View view = animView;
        float f = velocity;
        if (!this.mShouldShowMenu) {
            if (this.mTranslation <= 0.0f || !this.mSwipeHelper.isDismissGesture(motionEvent)) {
                snapBack(view, f);
            } else {
                dismiss(view, f);
            }
            return true;
        }
        boolean gestureTowardsMenu = isTowardsMenu(f);
        boolean gestureFastEnough = this.mSwipeHelper.getMinDismissVelocity() <= Math.abs(velocity);
        boolean swipedFarEnough = this.mSwipeHelper.swipedFarEnough(this.mTranslation, (float) this.mParent.getWidth());
        boolean showMenuForSlowOnGoing = !this.mParent.canViewBeDismissed() && ((double) (ev.getEventTime() - ev.getDownTime())) >= 200.0d;
        updateFilterItem();
        float menuSnapTarget = -getSpaceForMenu();
        if (this.mTranslation > 0.0f || NotificationStackScrollLayout.isPinnedHeadsUp(animView) || this.mParent.isOnKeyguard()) {
            if (this.mSwipeHelper.isDismissGesture(motionEvent)) {
                ScenarioTrackUtil.beginScenario(ScenarioConstants.SCENARIO_CLEAR_NOTI);
                dismiss(view, f);
            } else {
                snapBack(view, f);
            }
            return true;
        }
        boolean withinSnapMenuThreshold = this.mTranslation < (-(getSpaceForMenu() - (this.mHorizSpaceForIcon * 0.2f)));
        if (!this.mMenuSnappedTo || !isMenuVisible()) {
            if ((!this.mSwipeHelper.isFalseGesture(motionEvent) && swipedEnoughToShowMenu() && (!gestureFastEnough || showMenuForSlowOnGoing)) || gestureTowardsMenu) {
                showMenu(view, menuSnapTarget, f);
            } else if (!gestureTowardsMenu) {
                fadeInMenu((float) this.mParent.getWidth());
                showMenu(view, menuSnapTarget, f);
            } else {
                snapBack(view, f);
            }
        } else if (withinSnapMenuThreshold) {
            showMenu(view, menuSnapTarget, f);
        } else {
            snapBack(view, f);
        }
        return true;
    }

    private void showMenu(View animView, float targetLeft, float velocity) {
        this.mMenuSnappedTo = true;
        this.mMenuListener.onMenuShown(animView);
        this.mSwipeHelper.snap(animView, targetLeft, velocity);
    }

    private void snapBack(View animView, float velocity) {
        if (this.mFadeAnimator != null) {
            this.mFadeAnimator.cancel();
        }
        this.mHandler.removeCallbacks(this.mCheckForDrag);
        this.mMenuSnappedTo = false;
        this.mSnapping = true;
        this.mSwipeHelper.snap(animView, 0.0f, velocity);
    }

    private void dismiss(View animView, float velocity) {
        if (this.mFadeAnimator != null) {
            this.mFadeAnimator.cancel();
        }
        this.mHandler.removeCallbacks(this.mCheckForDrag);
        this.mMenuSnappedTo = false;
        this.mDismissing = true;
        this.mSwipeHelper.dismiss(animView, velocity);
    }

    private boolean swipedEnoughToShowMenu() {
        float multiplier;
        if (this.mParent.canViewBeDismissed()) {
            multiplier = 0.25f;
        } else {
            multiplier = 0.15f;
        }
        return !this.mSwipeHelper.swipedFarEnough(0.0f, 0.0f) && isMenuVisible() && this.mTranslation < (-(this.mHorizSpaceForIcon * multiplier));
    }

    private boolean isTowardsMenu(float movement) {
        return isMenuVisible() && movement >= 0.0f;
    }

    public void setAppName(String appName) {
        if (appName != null) {
            Resources res = this.mContext.getResources();
            int count = this.mMenuItems.size();
            for (int i = 0; i < count; i++) {
                NotificationMenuRowPlugin.MenuItem item = this.mMenuItems.get(i);
                String description = String.format(res.getString(R.string.notification_menu_accessibility), new Object[]{appName, item.getContentDescription()});
                View menuView = item.getMenuView();
                if (menuView != null) {
                    menuView.setContentDescription(description);
                }
            }
        }
    }

    public void onHeightUpdate() {
        if (this.mParent != null && this.mMenuItems.size() != 0 && this.mMenuContainer != null) {
            this.mMenuContainer.setTranslationY(((((float) this.mParent.getIntrinsicHeight()) - this.mHorizSpaceForIcon) / 2.0f) + ((float) (this.mParent.hasExtraTopPadding() ? -this.mParent.getPaddingTop() : 0)));
        }
    }

    public void onTranslationUpdate(float translation) {
        float desiredAlpha;
        this.mTranslation = translation;
        this.mMenuContainer.setClipBounds(new Rect(translation < 0.0f ? (int) (((float) this.mMenuContainer.getWidth()) + translation) : this.mMenuContainer.getWidth(), 0, this.mMenuContainer.getWidth(), this.mMenuContainer.getHeight()));
        if (!this.mAnimating && this.mMenuFadedIn) {
            float fadeThreshold = ((float) this.mParent.getWidth()) * 0.3f;
            float absTrans = Math.abs(translation);
            if (absTrans == 0.0f) {
                desiredAlpha = 0.0f;
            } else if (absTrans <= fadeThreshold) {
                desiredAlpha = 1.0f;
            } else {
                desiredAlpha = 1.0f - ((absTrans - fadeThreshold) / (((float) this.mParent.getWidth()) - fadeThreshold));
            }
            setMenuAlpha(desiredAlpha);
        }
    }

    public void onExpansionChanged() {
        setMenuLocation();
    }

    public void onClick(View v) {
        if (this.mMenuListener != null) {
            v.getLocationOnScreen(this.mIconLocation);
            this.mParent.getLocationOnScreen(this.mParentLocation);
            int i = this.mIconLocation[0] - this.mParentLocation[0];
            int i2 = this.mIconLocation[1] - this.mParentLocation[1];
            int index = this.mMenuContainer.indexOfChild(v);
            this.mMenuListener.onMenuClicked(this.mParent, i + ((int) (this.mHorizSpaceForIcon / 2.0f)), i2 + (v.getHeight() / 2), this.mMenuItems.get(index));
        }
    }

    private void setMenuLocation() {
        if (!this.mSnapping && this.mMenuContainer != null && this.mMenuContainer.isAttachedToWindow()) {
            int count = this.mMenuContainer.getChildCount();
            for (int i = 0; i < count; i++) {
                View v = this.mMenuContainer.getChildAt(i);
                float right = ((float) this.mParent.getWidth()) - (this.mHorizSpaceForIcon * ((float) (i + 1)));
                int intrinsicHeight = (this.mParent.getIntrinsicHeight() / 2) - (v.getHeight() / 2);
                v.setX(right);
            }
        }
    }

    /* access modifiers changed from: private */
    public void setMenuAlpha(float alpha) {
        this.mAlpha = alpha;
        if (this.mMenuContainer != null) {
            if (alpha == 0.0f) {
                this.mMenuFadedIn = false;
                this.mMenuContainer.setVisibility(4);
            } else {
                this.mMenuContainer.setVisibility(0);
                updateFilterItem();
            }
            int count = this.mMenuContainer.getChildCount();
            for (int i = 0; i < count; i++) {
                this.mMenuContainer.getChildAt(i).setAlpha(this.mAlpha);
            }
        }
    }

    /* access modifiers changed from: private */
    public float getSpaceForMenu() {
        return this.mHorizSpaceForIcon * ((float) this.mMenuContainer.getChildCount());
    }

    /* access modifiers changed from: private */
    public void fadeInMenu(final float notiThreshold) {
        if (!this.mDismissing && !this.mAnimating) {
            final float transX = this.mTranslation;
            final boolean fromLeft = this.mTranslation > 0.0f;
            setMenuLocation();
            this.mFadeAnimator = ValueAnimator.ofFloat(new float[]{this.mAlpha, 1.0f});
            this.mFadeAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    if (((fromLeft && transX <= notiThreshold) || (!fromLeft && Math.abs(transX) <= notiThreshold)) && !NotificationMenuRow.this.mMenuFadedIn) {
                        NotificationMenuRow.this.setMenuAlpha(((Float) animation.getAnimatedValue()).floatValue());
                    }
                }
            });
            this.mFadeAnimator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationStart(Animator animation) {
                    boolean unused = NotificationMenuRow.this.mAnimating = true;
                }

                public void onAnimationCancel(Animator animation) {
                    NotificationMenuRow.this.setMenuAlpha(0.0f);
                }

                public void onAnimationEnd(Animator animation) {
                    boolean z = false;
                    boolean unused = NotificationMenuRow.this.mAnimating = false;
                    NotificationMenuRow notificationMenuRow = NotificationMenuRow.this;
                    if (NotificationMenuRow.this.mAlpha == 1.0f) {
                        z = true;
                    }
                    boolean unused2 = notificationMenuRow.mMenuFadedIn = z;
                }
            });
            this.mFadeAnimator.setInterpolator(Interpolators.ALPHA_IN);
            this.mFadeAnimator.setDuration(200);
            this.mFadeAnimator.start();
        }
    }

    public void setMenuItems(ArrayList<NotificationMenuRowPlugin.MenuItem> arrayList) {
    }

    public static NotificationMenuRowPlugin.MenuItem createSnoozeItem(Context context) {
        return new NotificationMenuItem(context, context.getResources().getString(R.string.notification_menu_snooze_description), R.layout.notification_snooze, R.drawable.ic_snooze);
    }

    public static NotificationMenuRowPlugin.MenuItem createInfoItem(Context context) {
        return new NotificationMenuItem(context, context.getResources().getString(R.string.notification_menu_gear_description), R.layout.notification_info, R.drawable.ic_settings);
    }

    public static NotificationMenuRowPlugin.MenuItem createFilterItem(Context context, boolean isFoldInFilter) {
        return new NotificationMenuItem(context, context.getResources().getString(R.string.notification_menu_filter_description), R.layout.notification_filter, isFoldInFilter ? R.drawable.ic_remove_from_filter : R.drawable.ic_filter);
    }

    private void addMenuView(NotificationMenuRowPlugin.MenuItem item, ViewGroup parent) {
        View menuView = item.getMenuView();
        if (menuView != null) {
            parent.addView(menuView);
            menuView.setOnClickListener(this);
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) menuView.getLayoutParams();
            lp.width = (int) this.mHorizSpaceForIcon;
            lp.height = (int) this.mHorizSpaceForIcon;
            menuView.setLayoutParams(lp);
        }
    }

    private void updateFilterItem() {
        if (this.mFilterItem != null && this.mParent.getStatusBarNotification() != null) {
            boolean isUserFold = NotificationUtil.isUserFold();
            if (isUserFold != this.mIsUserFold) {
                this.mIsUserFold = isUserFold;
                if (isUserFold) {
                    this.mMenuContainer.removeAllViews();
                    this.mMenuItems.clear();
                    this.mMenuItems.add(this.mFilterItem);
                    this.mMenuItems.add(this.mInfoItem);
                    for (int i = 0; i < this.mMenuItems.size(); i++) {
                        addMenuView(this.mMenuItems.get(i), this.mMenuContainer);
                    }
                } else {
                    this.mMenuItems.clear();
                    this.mMenuItems.add(this.mInfoItem);
                    this.mMenuContainer.removeView(this.mFilterItem.getMenuView());
                }
                setMenuLocation();
            }
            if (isUserFold) {
                boolean isFold = this.mParent.getStatusBarNotification().isFold();
                if (this.mIsFoldInFilter != isFold) {
                    this.mIsFoldInFilter = isFold;
                    if (isFold) {
                        this.mFilterItem.setIcon(this.mContext, R.drawable.ic_remove_from_filter);
                    } else {
                        this.mFilterItem.setIcon(this.mContext, R.drawable.ic_filter);
                    }
                }
            }
        }
    }
}
