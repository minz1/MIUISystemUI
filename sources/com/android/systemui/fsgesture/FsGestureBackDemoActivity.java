package com.android.systemui.fsgesture;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;
import com.android.systemui.Constants;
import com.android.systemui.R;
import com.android.systemui.Util;
import com.android.systemui.fsgesture.GestureBackArrowView;
import com.android.systemui.recents.events.RecentsEventBus;
import com.android.systemui.recents.events.activity.FsGestureShowStateEvent;
import java.util.Locale;

public class FsGestureBackDemoActivity extends Activity {
    private static Handler sHandler = new Handler();
    /* access modifiers changed from: private */
    public String demoType;
    /* access modifiers changed from: private */
    public boolean isFromPro;
    /* access modifiers changed from: private */
    public GestureBackArrowView mBackArrowView;
    private View mBgView;
    private View.OnTouchListener mDemoActivityTouchListener = new View.OnTouchListener() {
        public boolean onTouch(View v, MotionEvent event) {
            int action = event.getActionMasked();
            float x = event.getX();
            float y = event.getY();
            switch (action) {
                case 0:
                    if (FsGestureBackDemoActivity.this.mGestureStatus != 2) {
                        float unused = FsGestureBackDemoActivity.this.mDownX = x;
                        float unused2 = FsGestureBackDemoActivity.this.mDownY = y;
                        if (((FsGestureBackDemoActivity.this.mStatus == 1 && x < 70.0f) || (FsGestureBackDemoActivity.this.mStatus == 2 && x > ((float) (FsGestureBackDemoActivity.this.mDisplayWidth - 70)))) && y > ((float) ((FsGestureBackDemoActivity.this.mDisplayHeight / 5) * 2))) {
                            int unused3 = FsGestureBackDemoActivity.this.mGestureStatus = 1;
                            FsGestureBackDemoActivity.this.mBackArrowView.onActionDown(FsGestureBackDemoActivity.this.mDownY, 0.0f, -1.0f);
                            FsGestureBackDemoActivity.this.mFsGestureDemoSwipeView.cancelAnimation();
                            break;
                        } else {
                            int unused4 = FsGestureBackDemoActivity.this.mGestureStatus = 0;
                            break;
                        }
                    } else {
                        return false;
                    }
                    break;
                case 1:
                case 3:
                    if (FsGestureBackDemoActivity.this.mGestureStatus != 0) {
                        FsGestureBackDemoActivity.this.mBackArrowView.onActionUp(GesturesBackController.convertOffset(FsGestureBackDemoActivity.this.mStatus == 1 ? x - FsGestureBackDemoActivity.this.mDownX : FsGestureBackDemoActivity.this.mDownX - x), null);
                        FsGestureBackDemoActivity.this.finishGesture(GesturesBackController.isFinished(FsGestureBackDemoActivity.this.mOffsetX, (int) (FsGestureBackDemoActivity.this.mOffsetX / ((float) ((int) (event.getEventTime() - event.getDownTime()))))));
                        break;
                    }
                    break;
                case 2:
                    if (FsGestureBackDemoActivity.this.mGestureStatus != 0) {
                        float rawOffsetX = FsGestureBackDemoActivity.this.mOffsetX = FsGestureBackDemoActivity.this.mStatus == 1 ? x - FsGestureBackDemoActivity.this.mDownX : FsGestureBackDemoActivity.this.mDownX - x;
                        float abs = Math.abs(y - FsGestureBackDemoActivity.this.mDownY);
                        if (rawOffsetX >= 20.0f) {
                            if (FsGestureBackDemoActivity.this.mGestureStatus == 1) {
                                int unused5 = FsGestureBackDemoActivity.this.mGestureStatus = 2;
                            }
                            FsGestureBackDemoActivity.this.mBackArrowView.onActionMove(rawOffsetX);
                            FsGestureBackDemoActivity.this.mBackArrowView.setReadyFinish(GesturesBackController.isFinished(rawOffsetX, (int) (rawOffsetX / ((float) ((int) (event.getEventTime() - event.getDownTime()))))) ? GestureBackArrowView.ReadyState.READY_STATE_BACK : GestureBackArrowView.ReadyState.READY_STATE_NONE);
                            break;
                        }
                    }
                    break;
            }
            return true;
        }
    };
    /* access modifiers changed from: private */
    public View mDemoActivityView;
    /* access modifiers changed from: private */
    public Matrix mDemoActivityViewMatrix = new Matrix();
    /* access modifiers changed from: private */
    public int mDisplayHeight;
    /* access modifiers changed from: private */
    public int mDisplayWidth;
    /* access modifiers changed from: private */
    public float mDownX;
    /* access modifiers changed from: private */
    public float mDownY;
    /* access modifiers changed from: private */
    public FsGestureDemoSwipeView mFsGestureDemoSwipeView;
    private FsGestureDemoTitleView mFsGestureDemoTitleView;
    /* access modifiers changed from: private */
    public int mGestureStatus;
    /* access modifiers changed from: private */
    public float mOffsetX;
    /* access modifiers changed from: private */
    public int mStatus = 0;

    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fs_gesture_back_demo);
        getWindow().addFlags(1024);
        Util.hideSystemBars(getWindow().getDecorView());
        DisplayMetrics dm = new DisplayMetrics();
        ((WindowManager) getSystemService("window")).getDefaultDisplay().getRealMetrics(dm);
        this.mDisplayWidth = dm.widthPixels;
        this.mDisplayHeight = dm.heightPixels;
        Intent intent = getIntent();
        this.demoType = intent.getStringExtra("DEMO_TYPE");
        this.demoType = this.demoType == null ? "DEMO_FULLY_SHOW" : this.demoType;
        this.mStatus = intent.getIntExtra("DEMO_STEP", 1);
        this.isFromPro = intent.getBooleanExtra("IS_FROM_PROVISION", false);
        initView();
    }

    private void initView() {
        RelativeLayout rootView = (RelativeLayout) findViewById(R.id.root_view);
        this.mBgView = findViewById(R.id.bg_view);
        this.mDemoActivityView = findViewById(R.id.demo_activity);
        this.mDemoActivityView.setOnTouchListener(this.mDemoActivityTouchListener);
        this.mFsGestureDemoTitleView = (FsGestureDemoTitleView) findViewById(R.id.fsgesture_title_view);
        if (TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) == 1) {
            this.mFsGestureDemoTitleView.setRTLParams();
        }
        int i = 0;
        if (this.mStatus == 1) {
            this.mFsGestureDemoTitleView.prepareTitleView(0);
        } else {
            this.mFsGestureDemoTitleView.prepareTitleView(1);
        }
        this.mFsGestureDemoTitleView.registerSkipEvent(new View.OnClickListener() {
            public void onClick(View v) {
                FsGestureBackDemoActivity.this.finish();
            }
        });
        if (Constants.IS_NOTCH) {
            int naturalBarHeight = getResources().getDimensionPixelSize(R.dimen.status_bar_height);
            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) this.mFsGestureDemoTitleView.getLayoutParams();
            lp.setMargins(0, naturalBarHeight, 0, 0);
            this.mFsGestureDemoTitleView.setLayoutParams(lp);
        }
        this.mFsGestureDemoSwipeView = (FsGestureDemoSwipeView) findViewById(R.id.fsgesture_swipe_view);
        if (this.mStatus == 1) {
            startSwipeViewAnimation(0);
        } else {
            startSwipeViewAnimation(1);
        }
        if (this.mStatus != 1) {
            i = 1;
        }
        this.mBackArrowView = new GestureBackArrowView(this, i);
        this.mBackArrowView.setDisplayWidth(this.mDisplayWidth);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(this.mDisplayWidth, this.mDisplayHeight);
        layoutParams.addRule(12);
        layoutParams.addRule(this.mStatus == 1 ? 9 : 11);
        rootView.addView(this.mBackArrowView, layoutParams);
        rootView.bringChildToFront(this.mBackArrowView);
    }

    /* access modifiers changed from: private */
    public void startSwipeViewAnimation(final int status) {
        sHandler.postDelayed(new Runnable() {
            public void run() {
                FsGestureBackDemoActivity.this.mFsGestureDemoSwipeView.prepare(status);
                FsGestureBackDemoActivity.this.mFsGestureDemoSwipeView.startAnimation(status);
            }
        }, 500);
    }

    /* access modifiers changed from: private */
    public void finishGesture(final boolean back) {
        if (this.mDemoActivityView != null) {
            ValueAnimator animator = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
            animator.setDuration(200);
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    float fraction = animation.getAnimatedFraction();
                    float[] tmp = new float[9];
                    FsGestureBackDemoActivity.this.mDemoActivityViewMatrix.getValues(tmp);
                    float curScaleX = tmp[0];
                    float curScaleY = tmp[4];
                    FsGestureBackDemoActivity.this.mDemoActivityViewMatrix.reset();
                    FsGestureBackDemoActivity.this.mDemoActivityViewMatrix.setScale(((1.0f - curScaleX) * fraction) + curScaleX, ((1.0f - curScaleY) * fraction) + curScaleY);
                    Matrix access$1000 = FsGestureBackDemoActivity.this.mDemoActivityViewMatrix;
                    access$1000.postTranslate((1.0f - fraction) * tmp[2], (1.0f - fraction) * tmp[5]);
                    FsGestureBackDemoActivity.this.mDemoActivityView.setAnimationMatrix(FsGestureBackDemoActivity.this.mDemoActivityViewMatrix);
                }
            });
            animator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    if (back) {
                        FsGestureBackDemoActivity.this.showBackAnimation();
                        return;
                    }
                    if (FsGestureBackDemoActivity.this.mStatus == 1) {
                        FsGestureBackDemoActivity.this.startSwipeViewAnimation(0);
                    } else if (FsGestureBackDemoActivity.this.mStatus == 2) {
                        FsGestureBackDemoActivity.this.startSwipeViewAnimation(1);
                    }
                    int unused = FsGestureBackDemoActivity.this.mGestureStatus = 3;
                }
            });
            animator.start();
        }
    }

    /* access modifiers changed from: private */
    public void showBackAnimation() {
        this.mBgView.setVisibility(0);
        Animation enterAnimation = AnimationUtils.loadAnimation(this, R.anim.activity_close_enter);
        Animation exitAnimation = AnimationUtils.loadAnimation(this, R.anim.activity_close_exit);
        enterAnimation.setAnimationListener(new Animation.AnimationListener() {
            public void onAnimationStart(Animation animation) {
            }

            public void onAnimationEnd(Animation animation) {
                FsGestureBackDemoActivity.this.onGestureFinish();
            }

            public void onAnimationRepeat(Animation animation) {
            }
        });
        this.mBgView.startAnimation(enterAnimation);
        this.mDemoActivityView.startAnimation(exitAnimation);
    }

    /* access modifiers changed from: private */
    public void onGestureFinish() {
        this.mFsGestureDemoTitleView.notifyFinish();
        this.mDemoActivityView.setVisibility(8);
        finishGestureBack();
    }

    private void finishGestureBack() {
        sHandler.postDelayed(new Runnable() {
            public void run() {
                if (FsGestureBackDemoActivity.this.mStatus == 1) {
                    Intent intent = new Intent(FsGestureBackDemoActivity.this, FsGestureBackDemoActivity.class);
                    intent.putExtra("DEMO_TYPE", FsGestureBackDemoActivity.this.demoType);
                    intent.putExtra("DEMO_STEP", 2);
                    intent.putExtra("IS_FROM_PROVISION", FsGestureBackDemoActivity.this.isFromPro);
                    FsGestureBackDemoActivity.this.startActivity(intent);
                    FsGestureBackDemoActivity.this.overridePendingTransition(R.anim.activity_start_enter, R.anim.activity_start_exit);
                } else if ("DEMO_FULLY_SHOW".equals(FsGestureBackDemoActivity.this.demoType)) {
                    Intent intent2 = new Intent(FsGestureBackDemoActivity.this, DemoFinishAct.class);
                    intent2.putExtra("DEMO_TYPE", FsGestureBackDemoActivity.this.demoType);
                    intent2.putExtra("IS_FROM_PROVISION", FsGestureBackDemoActivity.this.isFromPro);
                    FsGestureBackDemoActivity.this.startActivity(intent2);
                    FsGestureBackDemoActivity.this.overridePendingTransition(R.anim.activity_start_enter, R.anim.activity_start_exit);
                }
                FsGestureBackDemoActivity.this.finish();
            }
        }, 500);
    }

    /* access modifiers changed from: protected */
    public void onResume() {
        super.onResume();
        RecentsEventBus.getDefault().send(new FsGestureShowStateEvent(true));
    }

    /* access modifiers changed from: protected */
    public void onPause() {
        super.onPause();
    }
}
