package com.android.systemui.statusbar.policy;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.FrameLayout;
import com.android.settingslib.animation.AppearAnimationUtils;
import com.android.systemui.Dependency;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.qs.tiles.UserDetailItemView;
import com.android.systemui.statusbar.phone.KeyguardStatusBarView;
import com.android.systemui.statusbar.phone.NotificationPanelView;
import com.android.systemui.statusbar.policy.UserSwitcherController;

public class KeyguardUserSwitcher {
    private final Adapter mAdapter;
    /* access modifiers changed from: private */
    public boolean mAnimating;
    private final AppearAnimationUtils mAppearAnimationUtils;
    private final KeyguardUserSwitcherScrim mBackground;
    /* access modifiers changed from: private */
    public ObjectAnimator mBgAnimator;
    public final DataSetObserver mDataSetObserver = new DataSetObserver() {
        public void onChanged() {
            KeyguardUserSwitcher.this.refresh();
        }
    };
    private final KeyguardStatusBarView mStatusBarView;
    /* access modifiers changed from: private */
    public ViewGroup mUserSwitcher;
    /* access modifiers changed from: private */
    public final Container mUserSwitcherContainer;
    private UserSwitcherController mUserSwitcherController;

    public static class Adapter extends UserSwitcherController.BaseUserAdapter implements View.OnClickListener {
        private Context mContext;
        private KeyguardUserSwitcher mKeyguardUserSwitcher;

        public Adapter(Context context, UserSwitcherController controller, KeyguardUserSwitcher kgu) {
            super(controller);
            this.mContext = context;
            this.mKeyguardUserSwitcher = kgu;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            UserSwitcherController.UserRecord item = getItem(position);
            if (!(convertView instanceof UserDetailItemView) || !(convertView.getTag() instanceof UserSwitcherController.UserRecord)) {
                convertView = LayoutInflater.from(this.mContext).inflate(R.layout.keyguard_user_switcher_item, parent, false);
                convertView.setOnClickListener(this);
            }
            UserDetailItemView v = (UserDetailItemView) convertView;
            String name = getName(this.mContext, item);
            if (item.picture == null) {
                v.bind(name, getDrawable(this.mContext, item).mutate(), item.resolveId());
            } else {
                v.bind(name, item.picture, item.info.id);
            }
            v.setAvatarEnabled(item.isSwitchToEnabled);
            convertView.setActivated(item.isCurrent);
            convertView.setTag(item);
            return convertView;
        }

        public void onClick(View v) {
            UserSwitcherController.UserRecord user = (UserSwitcherController.UserRecord) v.getTag();
            if (user.isCurrent && !user.isGuest) {
                this.mKeyguardUserSwitcher.hideIfNotSimple(true);
            } else if (user.isSwitchToEnabled) {
                switchTo(user);
            }
        }
    }

    public static class Container extends FrameLayout {
        private KeyguardUserSwitcher mKeyguardUserSwitcher;

        public Container(Context context, AttributeSet attrs) {
            super(context, attrs);
            setClipChildren(false);
        }

        public void setKeyguardUserSwitcher(KeyguardUserSwitcher keyguardUserSwitcher) {
            this.mKeyguardUserSwitcher = keyguardUserSwitcher;
        }

        public boolean onTouchEvent(MotionEvent ev) {
            if (this.mKeyguardUserSwitcher != null && !this.mKeyguardUserSwitcher.isAnimating()) {
                this.mKeyguardUserSwitcher.hideIfNotSimple(true);
            }
            return false;
        }
    }

    public KeyguardUserSwitcher(Context context, ViewStub userSwitcher, KeyguardStatusBarView statusBarView, NotificationPanelView panelView) {
        boolean keyguardUserSwitcherEnabled = context.getResources().getBoolean(R.bool.config_keyguardUserSwitcher);
        UserSwitcherController userSwitcherController = (UserSwitcherController) Dependency.get(UserSwitcherController.class);
        if (userSwitcherController == null || !keyguardUserSwitcherEnabled) {
            this.mUserSwitcherContainer = null;
            this.mStatusBarView = null;
            this.mAdapter = null;
            this.mAppearAnimationUtils = null;
            this.mBackground = null;
            return;
        }
        this.mUserSwitcherContainer = (Container) userSwitcher.inflate();
        this.mBackground = new KeyguardUserSwitcherScrim(context);
        reinflateViews();
        this.mStatusBarView = statusBarView;
        panelView.setKeyguardUserSwitcher(this);
        this.mAdapter = new Adapter(context, userSwitcherController, this);
        this.mAdapter.registerDataSetObserver(this.mDataSetObserver);
        this.mUserSwitcherController = userSwitcherController;
        AppearAnimationUtils appearAnimationUtils = new AppearAnimationUtils(context, 400, -0.5f, 0.5f, Interpolators.FAST_OUT_SLOW_IN);
        this.mAppearAnimationUtils = appearAnimationUtils;
        this.mUserSwitcherContainer.setKeyguardUserSwitcher(this);
    }

    private void reinflateViews() {
        if (this.mUserSwitcher != null) {
            this.mUserSwitcher.setBackground(null);
            this.mUserSwitcher.removeOnLayoutChangeListener(this.mBackground);
        }
        this.mUserSwitcherContainer.removeAllViews();
        LayoutInflater.from(this.mUserSwitcherContainer.getContext()).inflate(R.layout.keyguard_user_switcher_inner, this.mUserSwitcherContainer);
        this.mUserSwitcher = (ViewGroup) this.mUserSwitcherContainer.findViewById(R.id.keyguard_user_switcher_inner);
        this.mUserSwitcher.addOnLayoutChangeListener(this.mBackground);
        this.mUserSwitcher.setBackground(this.mBackground);
    }

    public void setKeyguard(boolean keyguard, boolean animate) {
        if (this.mUserSwitcher == null) {
            return;
        }
        if (!keyguard || !shouldExpandByDefault()) {
            hide(animate);
        } else {
            show(animate);
        }
    }

    private boolean shouldExpandByDefault() {
        return this.mUserSwitcherController != null && this.mUserSwitcherController.isSimpleUserSwitcher();
    }

    public void show(boolean animate) {
        if (this.mUserSwitcher != null && this.mUserSwitcherContainer.getVisibility() != 0) {
            cancelAnimations();
            this.mAdapter.refresh();
            this.mUserSwitcherContainer.setVisibility(0);
            if (animate) {
                startAppearAnimation();
            }
        }
    }

    private boolean hide(boolean animate) {
        if (this.mUserSwitcher == null || this.mUserSwitcherContainer.getVisibility() != 0) {
            return false;
        }
        cancelAnimations();
        if (animate) {
            startDisappearAnimation();
        } else {
            this.mUserSwitcherContainer.setVisibility(8);
        }
        return true;
    }

    private void cancelAnimations() {
        int count = this.mUserSwitcher.getChildCount();
        for (int i = 0; i < count; i++) {
            this.mUserSwitcher.getChildAt(i).animate().cancel();
        }
        if (this.mBgAnimator != null) {
            this.mBgAnimator.cancel();
        }
        this.mUserSwitcher.animate().cancel();
        this.mAnimating = false;
    }

    private void startAppearAnimation() {
        int count = this.mUserSwitcher.getChildCount();
        View[] objects = new View[count];
        for (int i = 0; i < count; i++) {
            objects[i] = this.mUserSwitcher.getChildAt(i);
        }
        this.mUserSwitcher.setClipChildren(false);
        this.mUserSwitcher.setClipToPadding(false);
        this.mAppearAnimationUtils.startAnimation(objects, new Runnable() {
            public void run() {
                KeyguardUserSwitcher.this.mUserSwitcher.setClipChildren(true);
                KeyguardUserSwitcher.this.mUserSwitcher.setClipToPadding(true);
            }
        });
        this.mAnimating = true;
        this.mBgAnimator = ObjectAnimator.ofInt(this.mBackground, "alpha", new int[]{0, 255});
        this.mBgAnimator.setDuration(400);
        this.mBgAnimator.setInterpolator(Interpolators.ALPHA_IN);
        this.mBgAnimator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                ObjectAnimator unused = KeyguardUserSwitcher.this.mBgAnimator = null;
                boolean unused2 = KeyguardUserSwitcher.this.mAnimating = false;
            }
        });
        this.mBgAnimator.start();
    }

    private void startDisappearAnimation() {
        this.mAnimating = true;
        this.mUserSwitcher.animate().alpha(0.0f).setDuration(300).setInterpolator(Interpolators.ALPHA_OUT).withEndAction(new Runnable() {
            public void run() {
                KeyguardUserSwitcher.this.mUserSwitcherContainer.setVisibility(8);
                KeyguardUserSwitcher.this.mUserSwitcher.setAlpha(1.0f);
                boolean unused = KeyguardUserSwitcher.this.mAnimating = false;
            }
        });
    }

    /* access modifiers changed from: private */
    public void refresh() {
        int childCount = this.mUserSwitcher.getChildCount();
        int adapterCount = this.mAdapter.getCount();
        int N = Math.max(childCount, adapterCount);
        for (int i = 0; i < N; i++) {
            if (i < adapterCount) {
                View oldView = null;
                if (i < childCount) {
                    oldView = this.mUserSwitcher.getChildAt(i);
                }
                View newView = this.mAdapter.getView(i, oldView, this.mUserSwitcher);
                if (oldView == null) {
                    this.mUserSwitcher.addView(newView);
                } else if (oldView != newView) {
                    this.mUserSwitcher.removeViewAt(i);
                    this.mUserSwitcher.addView(newView, i);
                }
            } else {
                this.mUserSwitcher.removeViewAt(this.mUserSwitcher.getChildCount() - 1);
            }
        }
    }

    public boolean hideIfNotSimple(boolean animate) {
        if (this.mUserSwitcherContainer == null || this.mUserSwitcherController.isSimpleUserSwitcher()) {
            return false;
        }
        return hide(animate);
    }

    /* access modifiers changed from: package-private */
    public boolean isAnimating() {
        return this.mAnimating;
    }

    public void onDensityOrFontScaleChanged() {
        if (this.mUserSwitcherContainer != null) {
            reinflateViews();
            refresh();
        }
    }
}
