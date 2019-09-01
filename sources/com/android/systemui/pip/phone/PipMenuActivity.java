package com.android.systemui.pip.phone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.PendingIntent;
import android.app.RemoteAction;
import android.content.Intent;
import android.content.pm.ParceledListSlice;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.recents.events.RecentsEventBus;
import com.android.systemui.recents.events.component.HidePipMenuEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PipMenuActivity extends Activity {
    private final List<RemoteAction> mActions = new ArrayList();
    private LinearLayout mActionsGroup;
    private boolean mAllowMenuTimeout = true;
    /* access modifiers changed from: private */
    public boolean mAllowTouches = true;
    /* access modifiers changed from: private */
    public Drawable mBackgroundDrawable;
    private int mBetweenActionPaddingLand;
    private View mDismissButton;
    private PointF mDownDelta = new PointF();
    private PointF mDownPosition = new PointF();
    private ImageView mExpandButton;
    private final Runnable mFinishRunnable = new Runnable() {
        public void run() {
            PipMenuActivity.this.hideMenu();
        }
    };
    private Handler mHandler = new Handler();
    private ValueAnimator.AnimatorUpdateListener mMenuBgUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
        public void onAnimationUpdate(ValueAnimator animation) {
            PipMenuActivity.this.mBackgroundDrawable.setAlpha((int) (0.3f * ((Float) animation.getAnimatedValue()).floatValue() * 255.0f));
        }
    };
    private View mMenuContainer;
    private AnimatorSet mMenuContainerAnimator;
    private int mMenuState;
    private Messenger mMessenger = new Messenger(new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    Bundle data = (Bundle) msg.obj;
                    PipMenuActivity.this.showMenu(data.getInt("menu_state"), (Rect) data.getParcelable("stack_bounds"), (Rect) data.getParcelable("movement_bounds"), data.getBoolean("allow_timeout"));
                    return;
                case 2:
                    PipMenuActivity.this.cancelDelayedFinish();
                    return;
                case 3:
                    PipMenuActivity.this.hideMenu();
                    return;
                case 4:
                    Bundle data2 = (Bundle) msg.obj;
                    ParceledListSlice actions = data2.getParcelable("actions");
                    PipMenuActivity.this.setActions((Rect) data2.getParcelable("stack_bounds"), actions != null ? actions.getList() : Collections.EMPTY_LIST);
                    return;
                case 5:
                    PipMenuActivity.this.updateDismissFraction(((Bundle) msg.obj).getFloat("dismiss_fraction"));
                    return;
                case 6:
                    boolean unused = PipMenuActivity.this.mAllowTouches = true;
                    return;
                default:
                    return;
            }
        }
    });
    private Messenger mToControllerMessenger;
    private ViewConfiguration mViewConfig;
    private View mViewRoot;

    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        this.mViewConfig = ViewConfiguration.get(this);
        getWindow().addFlags(537133056);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pip_menu_activity);
        this.mBackgroundDrawable = new ColorDrawable(-16777216);
        this.mBackgroundDrawable.setAlpha(0);
        this.mViewRoot = findViewById(R.id.background);
        this.mViewRoot.setBackground(this.mBackgroundDrawable);
        this.mMenuContainer = findViewById(R.id.menu_container);
        this.mMenuContainer.setAlpha(0.0f);
        this.mMenuContainer.setOnClickListener(new View.OnClickListener() {
            public final void onClick(View view) {
                PipMenuActivity.lambda$onCreate$0(PipMenuActivity.this, view);
            }
        });
        this.mDismissButton = findViewById(R.id.dismiss);
        this.mDismissButton.setAlpha(0.0f);
        this.mDismissButton.setOnClickListener(new View.OnClickListener() {
            public final void onClick(View view) {
                PipMenuActivity.this.dismissPip();
            }
        });
        this.mActionsGroup = (LinearLayout) findViewById(R.id.actions_group);
        this.mBetweenActionPaddingLand = getResources().getDimensionPixelSize(R.dimen.pip_between_action_padding_land);
        this.mExpandButton = (ImageView) findViewById(R.id.expand_button);
        this.mExpandButton.setOnClickListener(new View.OnClickListener() {
            public final void onClick(View view) {
                PipMenuActivity.lambda$onCreate$2(PipMenuActivity.this, view);
            }
        });
        updateFromIntent(getIntent());
        setTitle(R.string.pip_menu_title);
        setDisablePreviewScreenshots(true);
    }

    public static /* synthetic */ void lambda$onCreate$0(PipMenuActivity pipMenuActivity, View v) {
        if (pipMenuActivity.mMenuState == 1) {
            pipMenuActivity.showPipMenu();
        } else {
            pipMenuActivity.hideMenu();
        }
    }

    public static /* synthetic */ void lambda$onCreate$2(PipMenuActivity pipMenuActivity, View v) {
        if (pipMenuActivity.mMenuState == 1) {
            pipMenuActivity.showPipMenu();
        } else {
            pipMenuActivity.expandPip();
        }
    }

    /* access modifiers changed from: protected */
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        updateFromIntent(intent);
    }

    public void onUserInteraction() {
        if (this.mAllowMenuTimeout) {
            repostDelayedFinish(2000);
        }
    }

    /* access modifiers changed from: protected */
    public void onUserLeaveHint() {
        super.onUserLeaveHint();
        hideMenu();
    }

    /* access modifiers changed from: protected */
    public void onStop() {
        super.onStop();
        cancelDelayedFinish();
        RecentsEventBus.getDefault().unregister(this);
    }

    /* access modifiers changed from: protected */
    public void onDestroy() {
        super.onDestroy();
        notifyActivityCallback(null);
    }

    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        if (!isInPictureInPictureMode) {
            finish();
        }
    }

    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (!this.mAllowTouches) {
            return super.dispatchTouchEvent(ev);
        }
        int action = ev.getAction();
        if (action == 0) {
            this.mDownPosition.set(ev.getX(), ev.getY());
            this.mDownDelta.set(0.0f, 0.0f);
        } else if (action == 2) {
            this.mDownDelta.set(ev.getX() - this.mDownPosition.x, ev.getY() - this.mDownPosition.y);
            if (this.mDownDelta.length() > ((float) this.mViewConfig.getScaledTouchSlop()) && this.mMenuState != 0) {
                notifyRegisterInputConsumer();
                cancelDelayedFinish();
            }
        } else if (action == 4) {
            hideMenu();
        }
        return super.dispatchTouchEvent(ev);
    }

    public void finish() {
        notifyActivityCallback(null);
        super.finish();
        overridePendingTransition(0, 0);
    }

    public void setTaskDescription(ActivityManager.TaskDescription taskDescription) {
    }

    public final void onBusEvent(HidePipMenuEvent event) {
        if (this.mMenuState != 0) {
            event.getAnimationTrigger().increment();
            hideMenu(new Runnable(event) {
                private final /* synthetic */ HidePipMenuEvent f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    PipMenuActivity.this.mHandler.post(new Runnable() {
                        public final void run() {
                            HidePipMenuEvent.this.getAnimationTrigger().decrement();
                        }
                    });
                }
            }, true);
        }
    }

    /* access modifiers changed from: private */
    public void showMenu(int menuState, Rect stackBounds, Rect movementBounds, boolean allowMenuTimeout) {
        this.mAllowMenuTimeout = allowMenuTimeout;
        if (this.mMenuState != menuState) {
            this.mAllowTouches = !(this.mMenuState == 2 || menuState == 2);
            cancelDelayedFinish();
            updateActionViews(stackBounds);
            if (this.mMenuContainerAnimator != null) {
                this.mMenuContainerAnimator.cancel();
            }
            notifyMenuStateChange(menuState);
            this.mMenuContainerAnimator = new AnimatorSet();
            ObjectAnimator menuAnim = ObjectAnimator.ofFloat(this.mMenuContainer, View.ALPHA, new float[]{this.mMenuContainer.getAlpha(), 1.0f});
            menuAnim.addUpdateListener(this.mMenuBgUpdateListener);
            ObjectAnimator dismissAnim = ObjectAnimator.ofFloat(this.mDismissButton, View.ALPHA, new float[]{this.mDismissButton.getAlpha(), 1.0f});
            if (menuState == 2) {
                this.mMenuContainerAnimator.playTogether(new Animator[]{menuAnim, dismissAnim});
            } else {
                this.mMenuContainerAnimator.play(dismissAnim);
            }
            this.mMenuContainerAnimator.setInterpolator(Interpolators.ALPHA_IN);
            this.mMenuContainerAnimator.setDuration(125);
            if (allowMenuTimeout) {
                this.mMenuContainerAnimator.addListener(new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator animation) {
                        PipMenuActivity.this.repostDelayedFinish(3500);
                    }
                });
            }
            this.mMenuContainerAnimator.start();
            return;
        }
        if (allowMenuTimeout) {
            repostDelayedFinish(2000);
        }
        notifyUnregisterInputConsumer();
    }

    /* access modifiers changed from: private */
    public void hideMenu() {
        hideMenu(null, true);
    }

    private void hideMenu(final Runnable animationFinishedRunnable, boolean notifyMenuVisibility) {
        if (this.mMenuState != 0) {
            cancelDelayedFinish();
            if (notifyMenuVisibility) {
                notifyMenuStateChange(0);
            }
            this.mMenuContainerAnimator = new AnimatorSet();
            ObjectAnimator menuAnim = ObjectAnimator.ofFloat(this.mMenuContainer, View.ALPHA, new float[]{this.mMenuContainer.getAlpha(), 0.0f});
            menuAnim.addUpdateListener(this.mMenuBgUpdateListener);
            ObjectAnimator dismissAnim = ObjectAnimator.ofFloat(this.mDismissButton, View.ALPHA, new float[]{this.mDismissButton.getAlpha(), 0.0f});
            this.mMenuContainerAnimator.playTogether(new Animator[]{menuAnim, dismissAnim});
            this.mMenuContainerAnimator.setInterpolator(Interpolators.ALPHA_OUT);
            this.mMenuContainerAnimator.setDuration(125);
            this.mMenuContainerAnimator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    if (animationFinishedRunnable != null) {
                        animationFinishedRunnable.run();
                    }
                    PipMenuActivity.this.finish();
                }
            });
            this.mMenuContainerAnimator.start();
            return;
        }
        finish();
    }

    private void updateFromIntent(Intent intent) {
        this.mToControllerMessenger = (Messenger) intent.getParcelableExtra("messenger");
        notifyActivityCallback(this.mMessenger);
        RecentsEventBus.getDefault().register(this);
        ParceledListSlice actions = intent.getParcelableExtra("actions");
        if (actions != null) {
            this.mActions.clear();
            this.mActions.addAll(actions.getList());
        }
        int menuState = intent.getIntExtra("menu_state", 0);
        if (menuState != 0) {
            showMenu(menuState, (Rect) intent.getParcelableExtra("stack_bounds"), (Rect) intent.getParcelableExtra("movement_bounds"), intent.getBooleanExtra("allow_timeout", true));
        }
    }

    /* access modifiers changed from: private */
    public void setActions(Rect stackBounds, List<RemoteAction> actions) {
        this.mActions.clear();
        this.mActions.addAll(actions);
        updateActionViews(stackBounds);
    }

    private void updateActionViews(Rect stackBounds) {
        int i;
        ViewGroup expandContainer = (ViewGroup) findViewById(R.id.expand_container);
        ViewGroup actionsContainer = (ViewGroup) findViewById(R.id.actions_container);
        actionsContainer.setOnTouchListener($$Lambda$PipMenuActivity$6uC3xpV7xV21Vuu2JnbvTzh8Rok.INSTANCE);
        if (!this.mActions.isEmpty()) {
            boolean isLandscapePip = true;
            if (this.mMenuState != 1) {
                actionsContainer.setVisibility(0);
                if (this.mActionsGroup != null) {
                    LayoutInflater inflater = LayoutInflater.from(this);
                    while (this.mActionsGroup.getChildCount() < this.mActions.size()) {
                        this.mActionsGroup.addView((ImageView) inflater.inflate(R.layout.pip_menu_action, this.mActionsGroup, false));
                    }
                    for (int i2 = 0; i2 < this.mActionsGroup.getChildCount(); i2++) {
                        View childAt = this.mActionsGroup.getChildAt(i2);
                        if (i2 < this.mActions.size()) {
                            i = 0;
                        } else {
                            i = 8;
                        }
                        childAt.setVisibility(i);
                    }
                    if (stackBounds == null || stackBounds.width() <= stackBounds.height()) {
                        isLandscapePip = false;
                    }
                    int i3 = 0;
                    while (i3 < this.mActions.size()) {
                        RemoteAction action = this.mActions.get(i3);
                        ImageView actionView = (ImageView) this.mActionsGroup.getChildAt(i3);
                        action.getIcon().loadDrawableAsync(this, new Icon.OnDrawableLoadedListener(actionView) {
                            private final /* synthetic */ ImageView f$0;

                            {
                                this.f$0 = r1;
                            }

                            public final void onDrawableLoaded(Drawable drawable) {
                                PipMenuActivity.lambda$updateActionViews$6(this.f$0, drawable);
                            }
                        }, this.mHandler);
                        actionView.setContentDescription(action.getContentDescription());
                        if (action.isEnabled()) {
                            actionView.setOnClickListener(new View.OnClickListener(action) {
                                private final /* synthetic */ RemoteAction f$0;

                                {
                                    this.f$0 = r1;
                                }

                                public final void onClick(View view) {
                                    PipMenuActivity.lambda$updateActionViews$7(this.f$0, view);
                                }
                            });
                        }
                        actionView.setEnabled(action.isEnabled());
                        actionView.setAlpha(action.isEnabled() ? 1.0f : 0.54f);
                        ((LinearLayout.LayoutParams) actionView.getLayoutParams()).leftMargin = (!isLandscapePip || i3 <= 0) ? 0 : this.mBetweenActionPaddingLand;
                        i3++;
                    }
                }
                FrameLayout.LayoutParams expandedLp = (FrameLayout.LayoutParams) expandContainer.getLayoutParams();
                expandedLp.topMargin = getResources().getDimensionPixelSize(R.dimen.pip_action_padding);
                expandedLp.bottomMargin = getResources().getDimensionPixelSize(R.dimen.pip_expand_container_edge_margin);
                expandContainer.requestLayout();
                return;
            }
        }
        actionsContainer.setVisibility(4);
    }

    static /* synthetic */ boolean lambda$updateActionViews$5(View v, MotionEvent ev) {
        return true;
    }

    static /* synthetic */ void lambda$updateActionViews$6(ImageView actionView, Drawable d) {
        d.setTint(-1);
        actionView.setImageDrawable(d);
    }

    static /* synthetic */ void lambda$updateActionViews$7(RemoteAction action, View v) {
        try {
            action.getActionIntent().send();
        } catch (PendingIntent.CanceledException e) {
            Log.w("PipMenuActivity", "Failed to send action", e);
        }
    }

    /* access modifiers changed from: private */
    public void updateDismissFraction(float fraction) {
        int alpha;
        float menuAlpha = 1.0f - fraction;
        if (this.mMenuState == 2) {
            this.mMenuContainer.setAlpha(menuAlpha);
            this.mDismissButton.setAlpha(menuAlpha);
            alpha = (int) (255.0f * ((0.3f * menuAlpha) + (0.6f * fraction)));
        } else {
            if (this.mMenuState == 1) {
                this.mDismissButton.setAlpha(menuAlpha);
            }
            alpha = (int) (0.6f * fraction * 255.0f);
        }
        this.mBackgroundDrawable.setAlpha(alpha);
    }

    private void notifyRegisterInputConsumer() {
        Message m = Message.obtain();
        m.what = com.android.systemui.plugins.R.styleable.AppCompatTheme_textColorSearchUrl;
        sendMessage(m, "Could not notify controller to register input consumer");
    }

    private void notifyUnregisterInputConsumer() {
        Message m = Message.obtain();
        m.what = com.android.systemui.plugins.R.styleable.AppCompatTheme_toolbarNavigationButtonStyle;
        sendMessage(m, "Could not notify controller to unregister input consumer");
    }

    private void notifyMenuStateChange(int menuState) {
        this.mMenuState = menuState;
        Message m = Message.obtain();
        m.what = 100;
        m.arg1 = menuState;
        sendMessage(m, "Could not notify controller of PIP menu visibility");
    }

    private void expandPip() {
        hideMenu(new Runnable() {
            public final void run() {
                PipMenuActivity.this.sendEmptyMessage(com.android.systemui.plugins.R.styleable.AppCompatTheme_textAppearanceSearchResultSubtitle, "Could not notify controller to expand PIP");
            }
        }, false);
    }

    /* access modifiers changed from: private */
    public void dismissPip() {
        hideMenu(new Runnable() {
            public final void run() {
                PipMenuActivity.this.sendEmptyMessage(com.android.systemui.plugins.R.styleable.AppCompatTheme_textAppearanceSmallPopupMenu, "Could not notify controller to dismiss PIP");
            }
        }, false);
    }

    private void showPipMenu() {
        Message m = Message.obtain();
        m.what = com.android.systemui.plugins.R.styleable.AppCompatTheme_toolbarStyle;
        sendMessage(m, "Could not notify controller to show PIP menu");
    }

    private void notifyActivityCallback(Messenger callback) {
        Message m = Message.obtain();
        m.what = com.android.systemui.plugins.R.styleable.AppCompatTheme_textColorAlertDialogListItem;
        m.replyTo = callback;
        sendMessage(m, "Could not notify controller of activity finished");
    }

    /* access modifiers changed from: private */
    public void sendEmptyMessage(int what, String errorMsg) {
        Message m = Message.obtain();
        m.what = what;
        sendMessage(m, errorMsg);
    }

    private void sendMessage(Message m, String errorMsg) {
        try {
            this.mToControllerMessenger.send(m);
        } catch (RemoteException e) {
            Log.e("PipMenuActivity", errorMsg, e);
        }
    }

    /* access modifiers changed from: private */
    public void cancelDelayedFinish() {
        this.mHandler.removeCallbacks(this.mFinishRunnable);
    }

    /* access modifiers changed from: private */
    public void repostDelayedFinish(long delay) {
        this.mHandler.removeCallbacks(this.mFinishRunnable);
        this.mHandler.postDelayed(this.mFinishRunnable, delay);
    }
}
