package com.android.systemui.fsgesture;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Xfermode;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import com.android.systemui.R;
import miui.util.CustomizeUtil;

public class NavStubDemoView extends View {
    public static final String TAG = NavStubDemoView.class.getSimpleName();
    /* access modifiers changed from: private */
    public Activity curActivity;
    /* access modifiers changed from: private */
    public FsGestureDemoTitleView demoTitleView;
    /* access modifiers changed from: private */
    public String demoType;
    private int fullyShowStep;
    /* access modifiers changed from: private */
    public boolean isFromPro;
    /* access modifiers changed from: private */
    public View mAppBgView;
    private Bitmap mAppIcon;
    /* access modifiers changed from: private */
    public View mAppNoteImg;
    /* access modifiers changed from: private */
    public View mBgView;
    /* access modifiers changed from: private */
    public int mBottomDec;
    /* access modifiers changed from: private */
    public float mCurAlpha;
    /* access modifiers changed from: private */
    public float mCurScale;
    /* access modifiers changed from: private */
    public float mCurrentY;
    private float mDelta;
    private int mDestPivotX;
    private int mDestPivotY;
    private int mDownNo;
    private float mDownX;
    private Bitmap mDragBitmap;
    private Bitmap mDrawBmp;
    private Bitmap mFakeBitmap;
    /* access modifiers changed from: private */
    public float mFollowTailX;
    /* access modifiers changed from: private */
    public float mFollowTailY;
    /* access modifiers changed from: private */
    public Handler mFrameHandler;
    /* access modifiers changed from: private */
    public LinearLayout mHomeIconImg;
    private boolean mIsAppToHome;
    private boolean mIsAppToRecents;
    private boolean mIsInFsgAnim;
    private int mLastDownNo;
    private Xfermode mModeSrcIn;
    private Paint mPaint;
    /* access modifiers changed from: private */
    public int mPivotLocX;
    /* access modifiers changed from: private */
    public int mPivotLocY;
    /* access modifiers changed from: private */
    public ValueAnimator mRecentsAnimator;
    /* access modifiers changed from: private */
    public View mRecentsBgView;
    /* access modifiers changed from: private */
    public LinearLayout mRecentsCardContainer;
    private Rect mRecentsFirstCardBound;
    /* access modifiers changed from: private */
    public View mRecentsFirstCardIconView;
    /* access modifiers changed from: private */
    public int mShowHeight;
    Rect mShowRect;
    /* access modifiers changed from: private */
    public int mShowWidth;
    /* access modifiers changed from: private */
    public int mStateMode;
    private Runnable mTailCatcherTask;
    /* access modifiers changed from: private */
    public float mXScale;
    /* access modifiers changed from: private */
    public float mYScale;
    /* access modifiers changed from: private */
    public FsGestureDemoSwipeView swipeView;

    static /* synthetic */ float access$016(NavStubDemoView x0, float x1) {
        float f = x0.mFollowTailX + x1;
        x0.mFollowTailX = f;
        return f;
    }

    static /* synthetic */ float access$216(NavStubDemoView x0, float x1) {
        float f = x0.mFollowTailY + x1;
        x0.mFollowTailY = f;
        return f;
    }

    public void setHomeIconImg(LinearLayout homeIconImg) {
        this.mHomeIconImg = homeIconImg;
    }

    public void setRecentsBgView(View recentsBgView) {
        this.mRecentsBgView = recentsBgView;
    }

    public void setRecentsCardContainer(LinearLayout recentsCardContainer) {
        this.mRecentsCardContainer = recentsCardContainer;
    }

    public void setAppBgView(View appBgView) {
        this.mAppBgView = appBgView;
    }

    public void setAppNoteImg(View appNoteImg) {
        this.mAppNoteImg = appNoteImg;
    }

    public void setBgView(View bgView) {
        this.mBgView = bgView;
    }

    public void setDemoType(String demoType2) {
        this.demoType = demoType2;
    }

    public void setFullyShowStep(int fullyShowStep2) {
        this.fullyShowStep = fullyShowStep2;
    }

    public void setDemoTitleView(FsGestureDemoTitleView demoTitleView2) {
        this.demoTitleView = demoTitleView2;
    }

    public void setSwipeView(FsGestureDemoSwipeView swipeView2) {
        this.swipeView = swipeView2;
    }

    public void setCurActivity(Activity curActivity2) {
        this.curActivity = curActivity2;
    }

    public void setIsFromPro(boolean isFromPro2) {
        this.isFromPro = isFromPro2;
    }

    public void setRecentsFirstCardIconView(View recentsFirstCardIconView) {
        this.mRecentsFirstCardIconView = recentsFirstCardIconView;
    }

    public NavStubDemoView(Context context) {
        this(context, null);
    }

    public NavStubDemoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NavStubDemoView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public NavStubDemoView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mFrameHandler = new Handler();
        this.mRecentsFirstCardBound = new Rect();
        this.mTailCatcherTask = new Runnable() {
            public void run() {
                NavStubDemoView.access$016(NavStubDemoView.this, (((float) NavStubDemoView.this.mPivotLocX) - NavStubDemoView.this.mFollowTailX) / 4.0f);
                NavStubDemoView.access$216(NavStubDemoView.this, (((float) NavStubDemoView.this.mPivotLocY) - NavStubDemoView.this.mFollowTailY) / 4.0f);
                float xDelta = Math.abs(((float) NavStubDemoView.this.mPivotLocX) - NavStubDemoView.this.mFollowTailX);
                float yDelta = Math.abs(((float) NavStubDemoView.this.mPivotLocY) - NavStubDemoView.this.mFollowTailY);
                double distance = Math.sqrt((double) ((xDelta * xDelta) + (yDelta * yDelta)));
                if (NavStubDemoView.this.mStateMode == 65538) {
                    if (NavStubDemoView.this.mCurrentY < ((float) (NavStubDemoView.this.mShowHeight - 320)) && distance < 20.0d) {
                        int unused = NavStubDemoView.this.mStateMode = 65539;
                        Log.d(NavStubDemoView.TAG, "current state mode: StateMode.STATE_TASK_HOLD");
                        NavStubDemoView.this.performHapticFeedback(1);
                        NavStubDemoView.this.mRecentsCardContainer.setVisibility(0);
                        if (NavStubDemoView.this.mRecentsAnimator.isRunning() || NavStubDemoView.this.mRecentsAnimator.isStarted()) {
                            NavStubDemoView.this.mRecentsAnimator.cancel();
                        }
                        NavStubDemoView.this.mRecentsAnimator.start();
                    }
                } else if (NavStubDemoView.this.mStateMode == 65539 && NavStubDemoView.this.mCurrentY > ((float) (NavStubDemoView.this.mShowHeight - 240))) {
                    int unused2 = NavStubDemoView.this.mStateMode = 65538;
                    if (NavStubDemoView.this.mRecentsAnimator.isRunning() || NavStubDemoView.this.mRecentsAnimator.isStarted()) {
                        NavStubDemoView.this.mRecentsAnimator.cancel();
                    }
                    NavStubDemoView.this.mRecentsAnimator.reverse();
                }
                NavStubDemoView.this.mFrameHandler.postDelayed(this, 16);
            }
        };
        this.mModeSrcIn = new PorterDuffXfermode(PorterDuff.Mode.SRC_IN);
        this.mShowRect = new Rect();
        initInternal();
    }

    private void initInternal() {
        DisplayMetrics dm = new DisplayMetrics();
        ((WindowManager) getContext().getSystemService("window")).getDefaultDisplay().getRealMetrics(dm);
        this.mShowWidth = dm.widthPixels;
        this.mShowHeight = dm.heightPixels;
        this.mStateMode = 65537;
        this.mPaint = new Paint();
        this.mPaint.setAntiAlias(true);
        this.mRecentsAnimator = ValueAnimator.ofPropertyValuesHolder(new PropertyValuesHolder[]{PropertyValuesHolder.ofFloat("scale", new float[]{1.1f, 1.05f}), PropertyValuesHolder.ofInt("alpha", new int[]{0, 255})});
        this.mRecentsAnimator.setInterpolator(new DecelerateInterpolator(1.5f));
        this.mRecentsAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                float curScale = ((Float) animation.getAnimatedValue("scale")).floatValue();
                int curAlpha = ((Integer) animation.getAnimatedValue("alpha")).intValue();
                NavStubDemoView.this.mRecentsCardContainer.setScaleX(curScale);
                NavStubDemoView.this.mRecentsCardContainer.setScaleY(curScale);
                NavStubDemoView.this.mRecentsCardContainer.setAlpha((float) curAlpha);
            }
        });
        this.mRecentsAnimator.setDuration(300);
        this.mFakeBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.app_note);
        this.mFakeBitmap = Bitmap.createBitmap(this.mFakeBitmap, 0, 0, this.mFakeBitmap.getWidth(), (int) (((float) this.mFakeBitmap.getWidth()) * ((((float) this.mShowHeight) * 1.0f) / ((float) this.mShowWidth))));
        this.mFakeBitmap.setHasAlpha(false);
        this.mFakeBitmap.prepareToDraw();
        this.mDragBitmap = createRoundCornerBmp(this.mFakeBitmap);
        this.mDragBitmap.setHasAlpha(false);
        this.mDragBitmap.prepareToDraw();
        this.mAppIcon = BitmapFactory.decodeResource(getResources(), R.drawable.note_icon);
    }

    private Bitmap createRoundCornerBmp(Bitmap srcBmp) {
        Bitmap target = Bitmap.createBitmap(srcBmp.getWidth(), srcBmp.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(target);
        canvas.drawRoundRect(new RectF(0.0f, 0.0f, (float) srcBmp.getWidth(), (float) srcBmp.getHeight()), 50.0f, 50.0f, this.mPaint);
        this.mPaint.setXfermode(this.mModeSrcIn);
        canvas.drawBitmap(srcBmp, 0.0f, 0.0f, this.mPaint);
        this.mPaint.setXfermode(null);
        return target;
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.mDrawBmp == null) {
            return;
        }
        if (!this.mIsAppToHome) {
            canvas.save();
            canvas.translate((float) this.mPivotLocX, (float) this.mPivotLocY);
            canvas.scale(this.mCurScale, this.mCurScale);
            canvas.translate((float) (-this.mPivotLocX), (float) (-this.mPivotLocY));
            this.mShowRect.left = this.mPivotLocX - (this.mShowWidth / 2);
            this.mShowRect.top = this.mPivotLocY - this.mShowHeight;
            this.mShowRect.right = this.mShowRect.left + this.mShowWidth;
            this.mShowRect.bottom = this.mShowRect.top + this.mShowHeight;
            int bottom = (int) (((float) this.mShowRect.top) + (((float) this.mShowHeight) * this.mCurScale));
            if (this.mIsAppToRecents) {
                bottom = this.mShowRect.top + this.mBottomDec;
            }
            this.mPaint.setAlpha(255);
            this.mPaint.setXfermode(null);
            this.mPaint.setStyle(Paint.Style.FILL);
            int saveLayer = canvas.saveLayer((float) this.mShowRect.left, (float) this.mShowRect.top, (float) this.mShowRect.right, (float) bottom, null);
            canvas.drawRoundRect((float) this.mShowRect.left, (float) this.mShowRect.top, (float) this.mShowRect.right, (float) bottom, 50.0f, 50.0f, this.mPaint);
            this.mPaint.setXfermode(this.mModeSrcIn);
            canvas.drawBitmap(this.mDrawBmp, null, this.mShowRect, this.mPaint);
            this.mPaint.setXfermode(null);
            canvas.restoreToCount(saveLayer);
            canvas.restore();
            return;
        }
        canvas.save();
        canvas.translate((float) this.mPivotLocX, (float) this.mPivotLocY);
        canvas.scale(this.mXScale, this.mYScale);
        canvas.translate((float) (-this.mPivotLocX), (float) (-this.mPivotLocY));
        this.mShowRect.left = this.mPivotLocX - (this.mShowWidth / 2);
        this.mShowRect.top = this.mPivotLocY - (this.mShowHeight / 2);
        this.mShowRect.right = this.mShowRect.left + this.mShowWidth;
        this.mShowRect.bottom = this.mShowRect.top + this.mShowHeight;
        this.mPaint.setAlpha((int) ((1.0f - this.mCurAlpha) * 255.0f));
        canvas.drawBitmap(this.mAppIcon, null, this.mShowRect, this.mPaint);
        this.mPaint.setAlpha((int) (255.0f * this.mCurAlpha));
        canvas.drawBitmap(this.mDrawBmp, null, this.mShowRect, this.mPaint);
        canvas.restore();
    }

    public boolean onTouchEvent(MotionEvent event) {
        boolean isTaskHold = false;
        if (this.mIsInFsgAnim) {
            return false;
        }
        if (event.getAction() == 0) {
            this.mDownNo++;
        }
        if (this.mDownNo == this.mLastDownNo) {
            return false;
        }
        if (1 == event.getAction()) {
            this.mLastDownNo = this.mDownNo;
        }
        this.mCurrentY = event.getRawY();
        switch (event.getAction()) {
            case 0:
                this.swipeView.cancelAnimation();
                this.mDownX = event.getRawX();
                this.mDelta = ((float) (this.mShowWidth / 2)) - this.mDownX;
                int i = this.mShowWidth / 2;
                this.mPivotLocX = i;
                this.mFollowTailX = (float) i;
                int i2 = this.mShowHeight;
                this.mPivotLocY = i2;
                this.mFollowTailY = (float) i2;
                this.mDrawBmp = this.mDragBitmap;
                this.mIsAppToHome = false;
                this.mStateMode = 65537;
                break;
            case 1:
                this.mIsInFsgAnim = true;
                setClickable(false);
                this.mFrameHandler.removeCallbacksAndMessages(null);
                boolean isOnDrag = this.mStateMode == 65538;
                if (this.mStateMode == 65539) {
                    isTaskHold = true;
                }
                if (isOnDrag || isTaskHold) {
                    if (((float) this.mPivotLocY) - this.mFollowTailY <= 20.0f) {
                        if (((float) this.mPivotLocY) - this.mFollowTailY >= -20.0f) {
                            if (!isOnDrag) {
                                if (!"DEMO_FULLY_SHOW".equals(this.demoType) || this.fullyShowStep != 2) {
                                    if (!"DEMO_TO_RECENTTASK".equals(this.demoType)) {
                                        performHapticFeedback(1);
                                        startCancelAnim();
                                        break;
                                    } else {
                                        startRecentTaskAnim();
                                        break;
                                    }
                                } else {
                                    startRecentTaskAnim();
                                    break;
                                }
                            } else {
                                performHapticFeedback(1);
                                startCancelAnim();
                                break;
                            }
                        } else if (!"DEMO_FULLY_SHOW".equals(this.demoType) || this.fullyShowStep != 1) {
                            if (!"DEMO_TO_HOME".equals(this.demoType)) {
                                performHapticFeedback(1);
                                startCancelAnim();
                                break;
                            } else {
                                startToHomeAnim();
                                break;
                            }
                        } else {
                            startToHomeAnim();
                            break;
                        }
                    } else {
                        performHapticFeedback(1);
                        startCancelAnim();
                        break;
                    }
                } else {
                    finalization();
                    break;
                }
                break;
            case 2:
                this.mPivotLocX = (int) (((event.getRawX() + this.mDownX) / 2.0f) + this.mDelta);
                this.mPivotLocY = (int) (((float) this.mShowHeight) - (linearToCubic(this.mCurrentY, (float) this.mShowHeight, 0.0f, 3.0f) * 444.0f));
                if (this.mStateMode == 65537) {
                    this.mStateMode = 65538;
                    this.mFrameHandler.post(this.mTailCatcherTask);
                    setLayoutParams(new RelativeLayout.LayoutParams(-1, -1));
                    this.mRecentsCardContainer.setVisibility(8);
                    this.mAppNoteImg.setVisibility(8);
                    this.mHomeIconImg.setVisibility(8);
                    this.mRecentsFirstCardIconView.setVisibility(4);
                }
                this.mCurScale = 1.0f - (linearToCubic(this.mCurrentY, (float) this.mShowHeight, 0.0f, 3.0f) * 0.385f);
                invalidate();
                break;
            case 3:
                finalization();
                break;
        }
        return super.onTouchEvent(event);
    }

    public void setDestPivot(int destPivotX, int destPivotY) {
        this.mDestPivotX = destPivotX;
        this.mDestPivotY = destPivotY;
    }

    private void startToHomeAnim() {
        this.mIsAppToHome = true;
        float destYScale = (((float) this.mAppIcon.getHeight()) * 1.0f) / ((float) this.mShowHeight);
        float destXScale = (((float) this.mAppIcon.getWidth()) * 1.0f) / ((float) this.mShowWidth);
        float curScale = this.mCurScale;
        int curPivotX = this.mPivotLocX;
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.addListener(new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animation) {
                NavStubDemoView.this.mHomeIconImg.setVisibility(0);
                NavStubDemoView.this.mRecentsCardContainer.setVisibility(8);
                NavStubDemoView.this.mRecentsBgView.setVisibility(8);
                NavStubDemoView.this.mAppNoteImg.setVisibility(8);
                NavStubDemoView.this.mAppBgView.setVisibility(8);
            }

            public void onAnimationEnd(Animator animation) {
                NavStubDemoView.this.demoTitleView.notifyFinish();
                if ("DEMO_FULLY_SHOW".equals(NavStubDemoView.this.demoType)) {
                    NavStubDemoView.this.getHandler().postDelayed(new Runnable() {
                        public void run() {
                            Intent intent = new Intent();
                            intent.setClass(NavStubDemoView.this.getContext(), HomeDemoAct.class);
                            intent.putExtra("DEMO_TYPE", "DEMO_FULLY_SHOW");
                            intent.putExtra("FULLY_SHOW_STEP", 2);
                            intent.putExtra("IS_FROM_PROVISION", NavStubDemoView.this.isFromPro);
                            NavStubDemoView.this.getContext().startActivity(intent);
                            NavStubDemoView.this.curActivity.overridePendingTransition(R.anim.activity_start_enter, R.anim.activity_start_exit);
                            NavStubDemoView.this.curActivity.finish();
                        }
                    }, 1000);
                } else if ("DEMO_TO_HOME".equals(NavStubDemoView.this.demoType)) {
                    NavStubDemoView.this.getHandler().postDelayed(new Runnable() {
                        public void run() {
                            NavStubDemoView.this.curActivity.finish();
                        }
                    }, 1000);
                }
            }
        });
        PropertyValuesHolder xScaleHolder = PropertyValuesHolder.ofFloat("xScale", new float[]{this.mCurScale, destXScale});
        PropertyValuesHolder xPivotHolder = PropertyValuesHolder.ofInt("xPivot", new int[]{curPivotX, this.mDestPivotX});
        ValueAnimator cubicAnimator = ValueAnimator.ofPropertyValuesHolder(new PropertyValuesHolder[]{xScaleHolder, xPivotHolder});
        cubicAnimator.setInterpolator(new DecelerateInterpolator(1.5f));
        cubicAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                float unused = NavStubDemoView.this.mXScale = ((Float) animation.getAnimatedValue("xScale")).floatValue();
                int unused2 = NavStubDemoView.this.mPivotLocX = ((Integer) animation.getAnimatedValue("xPivot")).intValue();
            }
        });
        PropertyValuesHolder propertyValuesHolder = xScaleHolder;
        cubicAnimator.setDuration(300);
        ValueAnimator quarterAnimator = ValueAnimator.ofPropertyValuesHolder(new PropertyValuesHolder[]{PropertyValuesHolder.ofFloat("yScale", new float[]{this.mCurScale, destYScale}), PropertyValuesHolder.ofInt("yPivot", new int[]{(int) (((float) this.mPivotLocY) - ((((float) this.mShowHeight) * curScale) / 2.0f)), this.mDestPivotY})});
        quarterAnimator.setInterpolator(new DecelerateInterpolator(2.0f));
        quarterAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                float unused = NavStubDemoView.this.mYScale = ((Float) animation.getAnimatedValue("yScale")).floatValue();
                int unused2 = NavStubDemoView.this.mPivotLocY = ((Integer) animation.getAnimatedValue("yPivot")).intValue();
            }
        });
        quarterAnimator.setDuration(300);
        ValueAnimator alphaAnimator = ValueAnimator.ofFloat(new float[]{1.0f, 0.0f});
        alphaAnimator.setInterpolator(new DecelerateInterpolator(1.0f));
        alphaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                float unused = NavStubDemoView.this.mCurAlpha = ((Float) animation.getAnimatedValue()).floatValue();
            }
        });
        alphaAnimator.setDuration(210);
        alphaAnimator.setStartDelay(40);
        ValueAnimator animator = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                NavStubDemoView.this.mBgView.setBackgroundColor(Color.argb((int) (187.0f * (1.0f - animation.getAnimatedFraction())), 0, 0, 0));
                NavStubDemoView.this.invalidate();
            }
        });
        PropertyValuesHolder propertyValuesHolder2 = xPivotHolder;
        animator.setDuration(300);
        animatorSet.playTogether(new Animator[]{animator, quarterAnimator, cubicAnimator, alphaAnimator});
        animatorSet.start();
    }

    private void startCancelAnim() {
        ValueAnimator animator = ValueAnimator.ofFloat(new float[]{this.mCurScale, 1.0f});
        animator.setInterpolator(new DecelerateInterpolator());
        final int initX = this.mPivotLocX;
        final int initY = this.mPivotLocY;
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                float unused = NavStubDemoView.this.mCurScale = ((Float) animation.getAnimatedValue()).floatValue();
                float fraction = animation.getAnimatedFraction();
                int unused2 = NavStubDemoView.this.mPivotLocX = (int) (((float) initX) + (((float) ((NavStubDemoView.this.mShowWidth / 2) - initX)) * fraction));
                int unused3 = NavStubDemoView.this.mPivotLocY = (int) (((float) initY) + (((float) (NavStubDemoView.this.mShowHeight - initY)) * fraction));
                NavStubDemoView.this.invalidate();
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                NavStubDemoView.this.getHandler().postDelayed(new Runnable() {
                    public void run() {
                        NavStubDemoView.this.swipeView.prepare(2);
                        NavStubDemoView.this.swipeView.startAnimation(2);
                    }
                }, 300);
                NavStubDemoView.this.finalization();
            }
        });
        animator.setDuration(300).start();
    }

    public void setRecentsFirstCardBound(Rect bound) {
        this.mRecentsFirstCardBound = bound;
    }

    private void startRecentTaskAnim() {
        this.mShowHeight -= CustomizeUtil.HAS_NOTCH ? getContext().getResources().getDimensionPixelSize(R.dimen.status_bar_height) : 0;
        this.mIsAppToRecents = true;
        float finalScale = (((float) this.mRecentsFirstCardBound.width()) * 1.0f) / ((float) this.mShowWidth);
        ValueAnimator appAnimator = ValueAnimator.ofPropertyValuesHolder(new PropertyValuesHolder[]{PropertyValuesHolder.ofFloat("scale", new float[]{this.mCurScale, finalScale}), PropertyValuesHolder.ofInt("bottomDec", new int[]{(int) (this.mCurScale * ((float) this.mShowHeight)), (int) (((float) this.mRecentsFirstCardBound.height()) / finalScale)})});
        appAnimator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                NavStubDemoView.this.mRecentsFirstCardIconView.setVisibility(0);
                NavStubDemoView.this.demoTitleView.notifyFinish();
                if ("DEMO_FULLY_SHOW".equals(NavStubDemoView.this.demoType)) {
                    NavStubDemoView.this.getHandler().postDelayed(new Runnable() {
                        public void run() {
                            Intent intent = new Intent();
                            intent.setClass(NavStubDemoView.this.getContext(), FsGestureBackDemoActivity.class);
                            intent.putExtra("DEMO_TYPE", "DEMO_FULLY_SHOW");
                            intent.putExtra("IS_FROM_PROVISION", NavStubDemoView.this.isFromPro);
                            NavStubDemoView.this.getContext().startActivity(intent);
                            NavStubDemoView.this.curActivity.overridePendingTransition(R.anim.activity_start_enter, R.anim.activity_start_exit);
                            NavStubDemoView.this.curActivity.finish();
                        }
                    }, 1000);
                } else if ("DEMO_TO_RECENTTASK".equals(NavStubDemoView.this.demoType)) {
                    NavStubDemoView.this.getHandler().postDelayed(new Runnable() {
                        public void run() {
                            NavStubDemoView.this.curActivity.finish();
                        }
                    }, 1000);
                }
            }
        });
        appAnimator.setInterpolator(new DecelerateInterpolator());
        int initX = this.mPivotLocX;
        int initY = this.mPivotLocY;
        float destX = (float) ((this.mRecentsFirstCardBound.width() / 2) + this.mRecentsFirstCardBound.left);
        final int i = initX;
        AnonymousClass11 r8 = r0;
        final float f = destX;
        final int i2 = initY;
        float f2 = destX;
        final float destX2 = (float) (((this.mRecentsFirstCardBound.width() * this.mShowHeight) / this.mShowWidth) + this.mRecentsFirstCardBound.top);
        AnonymousClass11 r0 = new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                float unused = NavStubDemoView.this.mCurScale = ((Float) animation.getAnimatedValue("scale")).floatValue();
                int unused2 = NavStubDemoView.this.mBottomDec = ((Integer) animation.getAnimatedValue("bottomDec")).intValue();
                float fraction = animation.getAnimatedFraction();
                int unused3 = NavStubDemoView.this.mPivotLocX = (int) (((float) i) + ((f - ((float) i)) * fraction));
                int unused4 = NavStubDemoView.this.mPivotLocY = (int) (((float) i2) + ((destX2 - ((float) i2)) * fraction));
                NavStubDemoView.this.invalidate();
            }
        };
        appAnimator.addUpdateListener(r8);
        appAnimator.setDuration(300);
        ValueAnimator recentsAnimator = ValueAnimator.ofFloat(new float[]{1.05f, 1.0f});
        recentsAnimator.setInterpolator(new DecelerateInterpolator());
        recentsAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                float curScale = ((Float) animation.getAnimatedValue()).floatValue();
                NavStubDemoView.this.mRecentsCardContainer.setScaleX(curScale);
                NavStubDemoView.this.mRecentsCardContainer.setScaleY(curScale);
            }
        });
        recentsAnimator.setDuration(300);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(new Animator[]{appAnimator, recentsAnimator});
        animatorSet.start();
    }

    /* access modifiers changed from: private */
    public void finalization() {
        this.mIsAppToRecents = false;
        this.mIsInFsgAnim = false;
        setClickable(true);
        this.mIsAppToHome = false;
        this.mPivotLocY = 0;
        this.mPivotLocX = 0;
        this.mCurAlpha = 1.0f;
        this.mCurScale = 0.0f;
        this.mPaint.setAlpha(255);
        this.mStateMode = 65537;
        this.mDrawBmp = this.mFakeBitmap;
        this.mHomeIconImg.setVisibility(0);
        this.mRecentsBgView.setVisibility(0);
        this.mAppBgView.setVisibility(0);
        this.mAppNoteImg.setVisibility(0);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(-1, (int) (20.0f * getContext().getResources().getDisplayMetrics().density));
        lp.addRule(12);
        setLayoutParams(lp);
        invalidate();
        if (this.mFrameHandler != null) {
            this.mFrameHandler.removeCallbacksAndMessages(null);
        }
    }

    private float linearToCubic(float now, float orignal, float end, float pow) {
        if (pow == orignal) {
            return now;
        }
        float ease = 0.0f;
        float percent = (now - orignal) / (pow - orignal);
        if (pow != 0.0f) {
            ease = (float) (1.0d - Math.pow((double) (1.0f - percent), (double) pow));
        }
        return ease;
    }
}
