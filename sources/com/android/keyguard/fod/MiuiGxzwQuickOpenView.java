package com.android.keyguard.fod;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.os.UserHandle;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;
import android.util.Property;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.fod.item.IQuickOpenItem;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.UiOffloadThread;
import com.android.systemui.miui.statusbar.phone.applock.AppLockHelper;
import java.util.ArrayList;
import java.util.List;
import miui.security.SecurityManager;

public class MiuiGxzwQuickOpenView extends FrameLayout {
    private float mCicleRadius;
    private IQuickOpenItem mCurrentSelectItem;
    private DismissListener mDismissListener;
    private RectF mFastRect;
    private int mFingerID = 0;
    private RectF mFingerRect;
    private float mItemRadius;
    private float mItemScaleRadius;
    private WindowManager.LayoutParams mLayoutParams;
    /* access modifiers changed from: private */
    public boolean mLoading = false;
    /* access modifiers changed from: private */
    public ObjectAnimator mLoadingAnimator;
    private MiuiGxzwQuickTeachView mMiuiGxzwQuickTeachView;
    private Paint mPaint;
    private boolean mPendingUpdateLp;
    /* access modifiers changed from: private */
    public MiuiGxzwQuickLoadingView mQuickLoadingView;
    private List<IQuickOpenItem> mQuickOpenItemList = new ArrayList();
    private int mScreenHeight;
    private SecurityManager mSecurityManager;
    private float mSelectBackgroundRadius;
    /* access modifiers changed from: private */
    public boolean mShowed = false;
    private TextView mSkipTeach;
    private TextView mSubTitleView;
    /* access modifiers changed from: private */
    public boolean mTeachMode;
    private boolean mTeachTouchDown;
    private FrameLayout.LayoutParams mTipLayoutParams;
    private int mTipPressMargin;
    private int mTipSlideMargin;
    private TextView mTipView;
    private LinearLayout mTitleContainer;
    private FrameLayout.LayoutParams mTitleLayoutParams;
    private int mTitleMargin;
    private TextView mTitleView;
    private final UiOffloadThread mUiOffloadThread = ((UiOffloadThread) Dependency.get(UiOffloadThread.class));
    private Vibrator mVibrator;
    private WindowManager mWindowManager;

    public interface DismissListener {
        void onDismiss();
    }

    public MiuiGxzwQuickOpenView(Context context) {
        super(context);
        initView();
    }

    /* access modifiers changed from: protected */
    public boolean drawChild(Canvas canvas, View child, long drawingTime) {
        if (this.mCurrentSelectItem != null && this.mCurrentSelectItem.getView() == child) {
            canvas.drawCircle(this.mCurrentSelectItem.getRect().centerX(), this.mCurrentSelectItem.getRect().centerY(), this.mSelectBackgroundRadius, this.mPaint);
        }
        return super.drawChild(canvas, child, drawingTime);
    }

    public void show(int fingerID) {
        this.mFingerID = fingerID;
        if (!this.mShowed) {
            this.mShowed = true;
            addView();
            this.mCurrentSelectItem = null;
            updateTextSize();
            updatePixelSize();
            startLoadingAnimation();
        }
    }

    public void dismiss() {
        if (this.mShowed) {
            this.mFingerID = 0;
            this.mShowed = false;
            if (this.mLoading || this.mCurrentSelectItem != null) {
                removeView();
            } else {
                startDismissAnimation();
            }
            if (this.mDismissListener != null) {
                this.mDismissListener.onDismiss();
            }
        }
    }

    public boolean isShow() {
        return this.mShowed;
    }

    public void onTouchDown(float touchX, float touchY) {
        if (this.mTeachMode && this.mMiuiGxzwQuickTeachView != null) {
            this.mMiuiGxzwQuickTeachView.stopTeachAnim();
        }
    }

    public void onTouchUp(float touchX, float touchY) {
        IQuickOpenItem item = caculateSelectQucikOpenItem(touchX, touchY);
        if (item != null) {
            handleQucikOpenItemTouchUp(item);
            dismiss();
        } else if (this.mQuickOpenItemList.size() <= 0 || (!MiuiGxzwQuickOpenUtil.isShowQuickOpenTeach(getContext()) && !this.mTeachMode)) {
            dismiss();
        } else {
            enterTeachMode();
        }
    }

    public void onTouchMove(float touchX, float touchY) {
        if (this.mLoadingAnimator != null && this.mLoading && this.mShowed && !this.mFastRect.contains(touchX, touchY)) {
            this.mLoadingAnimator.cancel();
            startShowQuickOpenItemAnimation();
        }
        IQuickOpenItem item = caculateSelectQucikOpenItem(touchX, touchY);
        if (!(this.mCurrentSelectItem == null || this.mCurrentSelectItem == item)) {
            handleQucikOpenItemExit(this.mCurrentSelectItem);
        }
        if (!(item == null || this.mCurrentSelectItem == item)) {
            handleQucikOpenItemEnter(item);
        }
        this.mCurrentSelectItem = item;
        invalidate();
    }

    public void setDismissListener(DismissListener l) {
        this.mDismissListener = l;
    }

    public void resetFingerID() {
        if (this.mFingerID != 0 && MiuiGxzwQuickOpenUtil.isQuickOpenEnable(getContext())) {
            this.mFingerID = 0;
            this.mUiOffloadThread.submit(new Runnable() {
                public void run() {
                    MiuiGxzwQuickOpenUtil.setFodAuthFingerprint(MiuiGxzwQuickOpenView.this.getContext(), 0, KeyguardUpdateMonitor.getCurrentUser());
                }
            });
        }
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (this.mPendingUpdateLp && isAttachedToWindow()) {
            this.mWindowManager.updateViewLayout(this, this.mLayoutParams);
        }
        this.mPendingUpdateLp = false;
        updateViewAddState();
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mPendingUpdateLp = false;
        updateViewAddState();
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() != 4) {
            return super.dispatchKeyEvent(event);
        }
        if (event.getAction() == 0) {
            dismiss();
        }
        return true;
    }

    private void initView() {
        setSystemUiVisibility(4864);
        updatePixelSize();
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(-1, -1, 2009, 84083968, -2);
        this.mLayoutParams = layoutParams;
        this.mLayoutParams.setTitle("gxzw_quick_open");
        this.mLayoutParams.screenOrientation = 1;
        this.mWindowManager = (WindowManager) getContext().getSystemService("window");
        this.mSecurityManager = (SecurityManager) getContext().getSystemService("security");
        MiuiGxzwUtils.caculateGxzwIconSize(getContext());
        int centerX = MiuiGxzwUtils.GXZW_ICON_X + (MiuiGxzwUtils.GXZW_ICON_WIDTH / 2);
        int centerY = MiuiGxzwUtils.GXZW_ICON_Y + (MiuiGxzwUtils.GXZW_ICON_HEIGHT / 2);
        this.mFingerRect = new RectF(((float) centerX) - this.mItemRadius, ((float) centerY) - this.mItemRadius, ((float) centerX) + this.mItemRadius, ((float) centerY) + this.mItemRadius);
        float small = getResources().getDimension(R.dimen.gxzw_quick_open_region_samll);
        this.mFastRect = new RectF(((float) centerX) - small, ((float) centerY) - small, ((float) centerX) + small, ((float) centerY) + small);
        this.mVibrator = (Vibrator) getContext().getSystemService("vibrator");
        this.mPaint = new Paint();
        this.mPaint.setAntiAlias(true);
        this.mPaint.setStyle(Paint.Style.FILL);
        this.mPaint.setColor(872415231);
        Display display = ((DisplayManager) getContext().getSystemService("display")).getDisplay(0);
        Point point = new Point();
        display.getRealSize(point);
        this.mScreenHeight = point.y;
        setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!MiuiGxzwQuickOpenView.this.mTeachMode) {
                    MiuiGxzwQuickOpenView.this.dismiss();
                }
            }
        });
        this.mTitleContainer = new LinearLayout(getContext());
        this.mTitleContainer.setOrientation(1);
        this.mTitleView = new TextView(getContext());
        this.mTitleView.setTextColor(-1);
        this.mTitleView.setGravity(17);
        this.mTitleContainer.addView(this.mTitleView, new LinearLayout.LayoutParams(-1, -2));
        this.mSubTitleView = new TextView(getContext());
        this.mSubTitleView.setTextColor(-1694498817);
        this.mSubTitleView.setGravity(17);
        this.mTitleContainer.addView(this.mSubTitleView, new LinearLayout.LayoutParams(-1, -2));
        this.mTitleLayoutParams = new FrameLayout.LayoutParams(-1, -2);
        this.mTitleLayoutParams.gravity = 80;
        this.mTitleLayoutParams.bottomMargin = (this.mScreenHeight - centerY) + this.mTitleMargin;
        addView(this.mTitleContainer, this.mTitleLayoutParams);
        this.mTipView = new TextView(getContext());
        this.mTipView.setTextColor(-16777216);
        this.mTipView.setGravity(17);
        this.mTipView.setVisibility(4);
        this.mTipView.setBackgroundResource(R.drawable.gxzw_quick_tip_background);
        this.mTipLayoutParams = new FrameLayout.LayoutParams(-2, -2);
        this.mTipLayoutParams.gravity = 81;
        this.mTipLayoutParams.bottomMargin = (this.mScreenHeight - centerY) + this.mTipPressMargin;
        addView(this.mTipView, this.mTipLayoutParams);
        updateTextSize();
        MiuiGxzwQuickOpenUtil.loadSharedPreferencesValue(getContext());
    }

    private void updateTextSize() {
        this.mTitleView.setTextSize(0, (float) getResources().getDimensionPixelSize(R.dimen.gxzw_quick_open_title_size));
        this.mSubTitleView.setTextSize(0, (float) getResources().getDimensionPixelSize(R.dimen.gxzw_quick_open_subtitle_size));
        this.mTipView.setTextSize(0, (float) getResources().getDimensionPixelSize(R.dimen.gxzw_quick_open_tip_size));
    }

    private void updatePixelSize() {
        this.mItemRadius = getContext().getResources().getDimension(R.dimen.gxzw_quick_open_item_radius);
        this.mItemRadius += MiuiGxzwQuickOpenUtil.getLargeItemDetal(getContext());
        this.mItemScaleRadius = getContext().getResources().getDimension(R.dimen.gxzw_quick_open_item_scale_radius);
        this.mSelectBackgroundRadius = getContext().getResources().getDimension(R.dimen.gxzw_quick_open_item_background_radius);
        this.mCicleRadius = getContext().getResources().getDimension(R.dimen.gxzw_quick_open_circle_radius);
        this.mTitleMargin = (int) getContext().getResources().getDimension(R.dimen.gxzw_quick_open_title_margin);
        this.mTipPressMargin = (int) getContext().getResources().getDimension(R.dimen.gxzw_quick_open_tip_press_margin);
        this.mTipSlideMargin = (int) getContext().getResources().getDimension(R.dimen.gxzw_quick_open_tip_slide_margin);
        int centerX = MiuiGxzwUtils.GXZW_ICON_X + (MiuiGxzwUtils.GXZW_ICON_WIDTH / 2);
        int centerY = MiuiGxzwUtils.GXZW_ICON_Y + (MiuiGxzwUtils.GXZW_ICON_HEIGHT / 2);
        this.mFingerRect = new RectF(((float) centerX) - this.mItemRadius, ((float) centerY) - this.mItemRadius, ((float) centerX) + this.mItemRadius, ((float) centerY) + this.mItemRadius);
        float small = getResources().getDimension(R.dimen.gxzw_quick_open_region_samll);
        this.mFastRect = new RectF(((float) centerX) - small, ((float) centerY) - small, ((float) centerX) + small, ((float) centerY) + small);
    }

    private void initQuickOpenItemList() {
        cleanQuickOpenItemList();
        this.mQuickOpenItemList.addAll(MiuiGxzwQuickOpenUtil.generateQuickOpenItemList(getContext(), this.mItemRadius, this.mCicleRadius, isRTL()));
        for (IQuickOpenItem item : this.mQuickOpenItemList) {
            RectF rectF = item.getRect();
            View view = item.getView();
            view.setVisibility(4);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams((int) rectF.width(), (int) rectF.height());
            lp.gravity = 51;
            lp.leftMargin = (int) rectF.left;
            lp.topMargin = (int) rectF.top;
            addView(view, lp);
        }
    }

    private void cleanQuickOpenItemList() {
        for (IQuickOpenItem item : this.mQuickOpenItemList) {
            item.getView().setVisibility(4);
            if (item.getView().isAttachedToWindow()) {
                removeView(item.getView());
            }
        }
        this.mQuickOpenItemList.clear();
    }

    private void updateViewAddState() {
        if (this.mShowed && getParent() == null) {
            addView();
        } else if (!this.mShowed && getParent() != null) {
            removeView();
        }
    }

    private void addView() {
        if (!isAttachedToWindow()) {
            this.mWindowManager.addView(this, this.mLayoutParams);
        }
    }

    /* access modifiers changed from: private */
    public void removeView() {
        this.mLayoutParams.blurRatio = 0.0f;
        this.mLayoutParams.flags &= -5;
        if (isAttachedToWindow()) {
            this.mWindowManager.removeViewImmediate(this);
        }
        cleanQuickOpenItemList();
        showTitle("", "");
        this.mTipView.setVisibility(4);
        this.mCurrentSelectItem = null;
        if (this.mSkipTeach != null) {
            removeView(this.mSkipTeach);
            this.mSkipTeach = null;
        }
        if (this.mMiuiGxzwQuickTeachView != null) {
            this.mMiuiGxzwQuickTeachView.stopTeachAnim();
            removeView(this.mMiuiGxzwQuickTeachView);
            this.mMiuiGxzwQuickTeachView = null;
        }
        this.mTeachMode = false;
        this.mTeachTouchDown = false;
        if (this.mLoadingAnimator != null) {
            this.mLoadingAnimator.cancel();
        }
    }

    private IQuickOpenItem caculateSelectQucikOpenItem(float x, float y) {
        for (IQuickOpenItem item : this.mQuickOpenItemList) {
            if (isInItemArea(item, x, y)) {
                return item;
            }
        }
        return null;
    }

    private void handleQucikOpenItemEnter(IQuickOpenItem item) {
        new ObjectAnimator();
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(item.getView(), View.SCALE_X, new float[]{item.getView().getScaleX(), this.mItemScaleRadius / this.mItemRadius});
        new ObjectAnimator();
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(item.getView(), View.SCALE_Y, new float[]{item.getView().getScaleY(), this.mItemScaleRadius / this.mItemRadius});
        AnimatorSet set = new AnimatorSet();
        set.playTogether(new Animator[]{scaleX, scaleY});
        set.setDuration(100);
        set.start();
        this.mVibrator.vibrate(5);
        showTitle(item.getTitle(), item.getSubTitle());
        this.mTipView.setVisibility(4);
    }

    private void handleQucikOpenItemExit(IQuickOpenItem item) {
        new ObjectAnimator();
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(item.getView(), View.SCALE_X, new float[]{item.getView().getScaleX(), 1.0f});
        new ObjectAnimator();
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(item.getView(), View.SCALE_Y, new float[]{item.getView().getScaleY(), 1.0f});
        AnimatorSet set = new AnimatorSet();
        set.playTogether(new Animator[]{scaleX, scaleY});
        set.setDuration(100);
        set.start();
        showTitle("", "");
    }

    private void handleQucikOpenItemTouchUp(IQuickOpenItem item) {
        List<ResolveInfo> list;
        Intent intent = item.getIntent();
        if (intent != null) {
            String packageName = null;
            if (!TextUtils.isEmpty(intent.getPackage())) {
                packageName = intent.getPackage();
            } else if (intent.getComponent() != null && !TextUtils.isEmpty(intent.getComponent().getPackageName())) {
                packageName = intent.getComponent().getPackageName();
            }
            if (packageName != null) {
                boolean needStartProcess = item.needStartProcess();
                boolean startActionByService = item.startActionByService();
                if (startActionByService) {
                    list = getContext().getPackageManager().queryIntentServicesAsUser(intent, 65536, KeyguardUpdateMonitor.getCurrentUser());
                } else {
                    list = getContext().getPackageManager().queryIntentActivitiesAsUser(intent, 65536, KeyguardUpdateMonitor.getCurrentUser());
                }
                if (list == null || list.size() <= 0) {
                    Intent marketIntent = new Intent("android.intent.action.VIEW", Uri.parse("market://details?id=" + packageName));
                    marketIntent.addFlags(268435456);
                    startActivitySafely(marketIntent);
                } else {
                    boolean appLock = AppLockHelper.isAppLocked(getContext(), this.mSecurityManager, packageName, KeyguardUpdateMonitor.getCurrentUser());
                    if (this.mFingerID != 0 && appLock && (needStartProcess || !startActionByService)) {
                        intent.putExtra("fod_quick_open", true);
                        MiuiGxzwQuickOpenUtil.setFodAuthFingerprint(getContext(), this.mFingerID, KeyguardUpdateMonitor.getCurrentUser());
                    }
                    if (needStartProcess && !appLock) {
                        startActivitySafely(getContext().getPackageManager().getLaunchIntentForPackage(packageName));
                    } else if (needStartProcess) {
                        intent.putExtra("quick_open_start_process", true);
                    }
                    if (startActionByService) {
                        startServiceSafely(intent);
                    } else {
                        startActivitySafely(intent);
                    }
                }
                MiuiGxzwQuickOpenUtil.disableShowQuickOpenTeach(getContext());
            }
        }
    }

    private void startActivitySafely(Intent intent) {
        try {
            getContext().startActivityAsUser(intent, new UserHandle(KeyguardUpdateMonitor.getCurrentUser()));
        } catch (Exception e) {
            Log.w("MiuiGxzwQuickOpenView", "start activity filed " + e);
        }
    }

    private void startServiceSafely(Intent intent) {
        try {
            getContext().startForegroundServiceAsUser(intent, new UserHandle(KeyguardUpdateMonitor.getCurrentUser()));
        } catch (Exception e) {
            Log.w("MiuiGxzwQuickOpenView", "start service filed " + e);
        }
    }

    private void startLoadingAnimation() {
        if (this.mQuickLoadingView != null) {
            removeView(this.mQuickLoadingView);
            this.mQuickLoadingView = null;
        }
        if (this.mLoadingAnimator != null) {
            this.mLoadingAnimator.cancel();
        }
        this.mQuickLoadingView = new MiuiGxzwQuickLoadingView(getContext(), this.mItemRadius);
        float max = this.mQuickLoadingView.getLoadingMaxRadius();
        MiuiGxzwUtils.caculateGxzwIconSize(getContext());
        int centerX = MiuiGxzwUtils.GXZW_ICON_X + (MiuiGxzwUtils.GXZW_ICON_WIDTH / 2);
        int centerY = MiuiGxzwUtils.GXZW_ICON_Y + (MiuiGxzwUtils.GXZW_ICON_HEIGHT / 2);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams((int) (max * 2.0f), (int) (2.0f * max));
        lp.gravity = 51;
        lp.leftMargin = centerX - ((int) max);
        lp.topMargin = centerY - ((int) max);
        addView(this.mQuickLoadingView, lp);
        this.mQuickLoadingView.setLoading(true);
        new ObjectAnimator();
        this.mLoadingAnimator = ObjectAnimator.ofFloat(this.mQuickLoadingView, "currentLoadingRadius", new float[]{this.mQuickLoadingView.getLoadingOriginalRadius(), this.mQuickLoadingView.getLoadingMaxRadius()});
        this.mLoadingAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                MiuiGxzwQuickOpenView.this.mQuickLoadingView.setCurrentLoadingRadius(((Float) animation.getAnimatedValue()).floatValue());
            }
        });
        this.mLoadingAnimator.addListener(new Animator.AnimatorListener() {
            private boolean canceled = false;

            public void onAnimationStart(Animator animation) {
                MiuiGxzwQuickOpenView.this.showPressTipIfNeed();
            }

            public void onAnimationEnd(Animator animation) {
                ObjectAnimator unused = MiuiGxzwQuickOpenView.this.mLoadingAnimator = null;
                boolean unused2 = MiuiGxzwQuickOpenView.this.mLoading = false;
                MiuiGxzwQuickOpenView.this.mQuickLoadingView.setLoading(false);
                if (!this.canceled && MiuiGxzwQuickOpenView.this.mShowed) {
                    MiuiGxzwQuickOpenView.this.startShowQuickOpenItemAnimation();
                }
            }

            public void onAnimationCancel(Animator animation) {
                this.canceled = true;
            }

            public void onAnimationRepeat(Animator animation) {
            }
        });
        this.mLoadingAnimator.setStartDelay(500);
        this.mLoadingAnimator.setDuration(600);
        this.mLoadingAnimator.start();
        this.mLoading = true;
    }

    /* access modifiers changed from: private */
    public void startShowQuickOpenItemAnimation() {
        this.mLayoutParams.blurRatio = 1.0f;
        this.mLayoutParams.flags |= 4;
        if (isAttachedToWindow()) {
            this.mWindowManager.updateViewLayout(this, this.mLayoutParams);
        } else {
            this.mPendingUpdateLp = true;
        }
        initQuickOpenItemList();
        MiuiGxzwUtils.caculateGxzwIconSize(getContext());
        int centerX = MiuiGxzwUtils.GXZW_ICON_X + (MiuiGxzwUtils.GXZW_ICON_WIDTH / 2);
        int centerY = MiuiGxzwUtils.GXZW_ICON_Y + (MiuiGxzwUtils.GXZW_ICON_HEIGHT / 2);
        for (IQuickOpenItem item : this.mQuickOpenItemList) {
            item.getView().setVisibility(0);
            new ObjectAnimator();
            ObjectAnimator translationX = ObjectAnimator.ofFloat(item.getView(), View.TRANSLATION_X, new float[]{((float) centerX) - item.getRect().centerX(), 0.0f});
            new ObjectAnimator();
            ObjectAnimator translationY = ObjectAnimator.ofFloat(item.getView(), View.TRANSLATION_Y, new float[]{((float) centerY) - item.getRect().centerY(), 0.0f});
            AnimatorSet set = new AnimatorSet();
            set.playTogether(new Animator[]{translationX, translationY});
            set.setDuration(150);
            set.start();
        }
        this.mVibrator.vibrate(12);
        showSlideTipIfNeed();
    }

    private void startDismissAnimation() {
        MiuiGxzwUtils.caculateGxzwIconSize(getContext());
        int i = 2;
        int centerX = MiuiGxzwUtils.GXZW_ICON_X + (MiuiGxzwUtils.GXZW_ICON_WIDTH / 2);
        int centerY = MiuiGxzwUtils.GXZW_ICON_Y + (MiuiGxzwUtils.GXZW_ICON_HEIGHT / 2);
        for (IQuickOpenItem item : this.mQuickOpenItemList) {
            new ObjectAnimator();
            View view = item.getView();
            Property property = View.TRANSLATION_X;
            float[] fArr = new float[i];
            fArr[0] = item.getView().getTranslationX();
            fArr[1] = ((float) centerX) - item.getRect().centerX();
            ObjectAnimator translationX = ObjectAnimator.ofFloat(view, property, fArr);
            new ObjectAnimator();
            View view2 = item.getView();
            Property property2 = View.TRANSLATION_Y;
            float[] fArr2 = new float[i];
            fArr2[0] = item.getView().getTranslationY();
            fArr2[1] = ((float) centerY) - item.getRect().centerY();
            ObjectAnimator translationY = ObjectAnimator.ofFloat(view2, property2, fArr2);
            new ObjectAnimator();
            float[] fArr3 = new float[i];
            // fill-array-data instruction
            fArr3[0] = 1065353216;
            fArr3[1] = 0;
            ObjectAnimator alpha = ObjectAnimator.ofFloat(item.getView(), View.ALPHA, fArr3);
            new ObjectAnimator();
            View view3 = item.getView();
            Property property3 = View.SCALE_X;
            float[] fArr4 = new float[i];
            fArr4[0] = item.getView().getScaleX();
            fArr4[1] = 0.0f;
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(view3, property3, fArr4);
            new ObjectAnimator();
            View view4 = item.getView();
            Property property4 = View.SCALE_Y;
            float[] fArr5 = new float[i];
            fArr5[0] = item.getView().getScaleY();
            fArr5[1] = 0.0f;
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(view4, property4, fArr5);
            AnimatorSet set = new AnimatorSet();
            set.playTogether(new Animator[]{translationX, translationY, alpha, scaleX, scaleY});
            set.setDuration(150);
            set.start();
            i = 2;
        }
        if (this.mQuickLoadingView != null) {
            new ObjectAnimator();
            ObjectAnimator alpha2 = ObjectAnimator.ofFloat(this.mQuickLoadingView, View.ALPHA, new float[]{1.0f, 0.0f});
            alpha2.setDuration(150);
            alpha2.addListener(new Animator.AnimatorListener() {
                public void onAnimationStart(Animator animation) {
                }

                public void onAnimationEnd(Animator animation) {
                    MiuiGxzwQuickOpenView.this.removeView();
                }

                public void onAnimationCancel(Animator animation) {
                }

                public void onAnimationRepeat(Animator animation) {
                }
            });
            alpha2.start();
        } else {
            removeView();
        }
        if (this.mMiuiGxzwQuickTeachView != null) {
            this.mMiuiGxzwQuickTeachView.stopTeachAnim();
            removeView(this.mMiuiGxzwQuickTeachView);
            this.mMiuiGxzwQuickTeachView = null;
        }
        this.mTipView.setVisibility(4);
    }

    private boolean isInItemArea(IQuickOpenItem item, float x, float y) {
        return item.getRegion().contains((int) x, (int) y);
    }

    private void showTitle(String title, String subTitle) {
        int centerY = MiuiGxzwUtils.GXZW_ICON_Y + (MiuiGxzwUtils.GXZW_ICON_HEIGHT / 2);
        this.mTitleLayoutParams.bottomMargin = (this.mScreenHeight - centerY) + this.mTitleMargin;
        updateViewLayout(this.mTitleContainer, this.mTitleLayoutParams);
        this.mTitleView.setText(title);
        this.mSubTitleView.setText(subTitle);
    }

    /* access modifiers changed from: private */
    public void showPressTipIfNeed() {
        if (MiuiGxzwQuickOpenUtil.isShowQuickOpenPress(getContext())) {
            int centerY = MiuiGxzwUtils.GXZW_ICON_Y + (MiuiGxzwUtils.GXZW_ICON_HEIGHT / 2);
            this.mTipLayoutParams.bottomMargin = (this.mScreenHeight - centerY) + this.mTipPressMargin;
            updateViewLayout(this.mTipView, this.mTipLayoutParams);
            this.mTipView.setTranslationY(0.0f);
            this.mTipView.setVisibility(0);
            this.mTipView.setText(R.string.gxzw_quick_tip_press);
            MiuiGxzwQuickOpenUtil.increaseShowQuickOpenPressCount(getContext());
        }
    }

    private void showSlideTipIfNeed() {
        if (MiuiGxzwQuickOpenUtil.isShowQuickOpenSlide(getContext())) {
            int centerY = MiuiGxzwUtils.GXZW_ICON_Y + (MiuiGxzwUtils.GXZW_ICON_HEIGHT / 2);
            this.mTipLayoutParams.bottomMargin = (this.mScreenHeight - centerY) + this.mTipPressMargin;
            updateViewLayout(this.mTipView, this.mTipLayoutParams);
            this.mTipView.setTranslationY(0.0f);
            new ObjectAnimator();
            ObjectAnimator translationY = ObjectAnimator.ofFloat(this.mTipView, View.TRANSLATION_Y, new float[]{this.mTipView.getTranslationY(), (float) (this.mTipPressMargin - this.mTipSlideMargin)});
            translationY.setDuration(100);
            translationY.start();
            this.mTipView.setVisibility(0);
            this.mTipView.setText(R.string.gxzw_quick_tip_slide);
            MiuiGxzwQuickOpenUtil.disableShowQuickOpenSlide(getContext());
        }
    }

    private void enterTeachMode() {
        this.mTeachMode = true;
        if (this.mSkipTeach == null) {
            this.mSkipTeach = new TextView(getContext());
            this.mSkipTeach.setTextColor(-1694498817);
            this.mSkipTeach.setTextSize(0, (float) getResources().getDimensionPixelSize(R.dimen.gxzw_quick_open_skip_teach));
            this.mSkipTeach.setText(R.string.gxzw_quick_open_skip_teach);
            this.mSkipTeach.setBackgroundResource(R.drawable.gxzw_quick_open_skip_teach_b);
            int horizontal = (int) getResources().getDimension(R.dimen.gxzw_quick_open_skip_teach_padding_horizontal);
            int vertical = (int) getResources().getDimension(R.dimen.gxzw_quick_open_skip_teach_padding_vertical);
            this.mSkipTeach.setPadding(horizontal, vertical, horizontal, vertical);
            this.mSkipTeach.setGravity(17);
            this.mSkipTeach.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    MiuiGxzwQuickOpenView.this.dismiss();
                }
            });
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-2, -2);
            lp.gravity = 8388661;
            lp.setMarginEnd((int) getResources().getDimension(R.dimen.gxzw_quick_open_skip_teach_margin_end));
            lp.topMargin = (int) getResources().getDimension(R.dimen.gxzw_quick_open_skip_teach_margin_top);
            addView(this.mSkipTeach, lp);
        }
        if (this.mMiuiGxzwQuickTeachView == null) {
            this.mMiuiGxzwQuickTeachView = new MiuiGxzwQuickTeachView(getContext(), this.mItemRadius);
            RectF rectF = new RectF(this.mFingerRect.left, this.mFingerRect.top - this.mCicleRadius, this.mFingerRect.right, this.mFingerRect.bottom);
            FrameLayout.LayoutParams lp2 = new FrameLayout.LayoutParams((int) rectF.width(), (int) rectF.height());
            lp2.gravity = 51;
            lp2.leftMargin = (int) rectF.left;
            lp2.topMargin = (int) rectF.top;
            addView(this.mMiuiGxzwQuickTeachView, lp2);
            if (this.mQuickOpenItemList.size() > 0) {
                this.mMiuiGxzwQuickTeachView.setPivotX(rectF.width() / 2.0f);
                this.mMiuiGxzwQuickTeachView.setPivotY(rectF.height() - this.mItemRadius);
                this.mMiuiGxzwQuickTeachView.setRotation(MiuiGxzwQuickOpenUtil.getTeachViewRotation(this.mQuickOpenItemList.size()));
            }
        }
        this.mMiuiGxzwQuickTeachView.startTeachAnim();
        MiuiGxzwQuickOpenUtil.disableShowQuickOpenTeach(getContext());
    }

    private boolean isRTL() {
        return (getResources().getConfiguration().screenLayout & 192) == 128;
    }
}
