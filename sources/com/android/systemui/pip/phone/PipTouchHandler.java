package com.android.systemui.pip.phone;

import android.animation.ValueAnimator;
import android.app.IActivityManager;
import android.content.Context;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import android.util.Size;
import android.view.IPinnedStackController;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.policy.PipSnapAlgorithm;
import com.android.systemui.R;
import com.android.systemui.pip.phone.InputConsumerController;
import com.android.systemui.pip.phone.PipAccessibilityInteractionConnection;
import com.android.systemui.pip.phone.PipMenuActivityController;
import com.android.systemui.statusbar.FlingAnimationUtils;
import java.io.PrintWriter;

public class PipTouchHandler {
    private final AccessibilityManager mAccessibilityManager;
    private final IActivityManager mActivityManager;
    /* access modifiers changed from: private */
    public final Context mContext;
    private PipTouchGesture mDefaultMovementGesture = new PipTouchGesture() {
        private boolean mStartedOnLeft;

        public void onDown(PipTouchState touchState) {
            if (touchState.isUserInteracting()) {
                boolean z = false;
                this.mStartedOnLeft = PipTouchHandler.this.mMotionHelper.getBounds().left < PipTouchHandler.this.mMovementBounds.centerX();
                boolean unused = PipTouchHandler.this.mMovementWithinMinimize = true;
                PipTouchHandler pipTouchHandler = PipTouchHandler.this;
                if (touchState.getDownTouchPosition().y >= ((float) PipTouchHandler.this.mMovementBounds.bottom)) {
                    z = true;
                }
                boolean unused2 = pipTouchHandler.mMovementWithinDismiss = z;
                if (PipTouchHandler.this.mMenuState != 0 && !PipTouchHandler.this.mIsMinimized) {
                    PipTouchHandler.this.mMenuController.pokeMenu();
                }
                PipTouchHandler.this.mDismissViewController.createDismissTarget();
                PipTouchHandler.this.mHandler.postDelayed(PipTouchHandler.this.mShowDismissAffordance, 225);
            }
        }

        /* access modifiers changed from: package-private */
        public boolean onMove(PipTouchState touchState) {
            boolean z = false;
            if (!touchState.isUserInteracting()) {
                return false;
            }
            if (touchState.startedDragging()) {
                float unused = PipTouchHandler.this.mSavedSnapFraction = -1.0f;
                PipTouchHandler.this.mHandler.removeCallbacks(PipTouchHandler.this.mShowDismissAffordance);
                PipTouchHandler.this.mDismissViewController.showDismissTarget();
            }
            if (!touchState.isDragging()) {
                return false;
            }
            PipTouchHandler.this.mTmpBounds.set(PipTouchHandler.this.mMotionHelper.getBounds());
            PointF lastDelta = touchState.getLastTouchDelta();
            float left = ((float) PipTouchHandler.this.mTmpBounds.left) + lastDelta.x;
            float top = ((float) PipTouchHandler.this.mTmpBounds.top) + lastDelta.y;
            touchState.allowDraggingOffscreen();
            PipTouchHandler.this.mTmpBounds.offsetTo((int) Math.max((float) PipTouchHandler.this.mMovementBounds.left, Math.min((float) PipTouchHandler.this.mMovementBounds.right, left)), (int) Math.max((float) PipTouchHandler.this.mMovementBounds.top, top));
            PipTouchHandler.this.mMotionHelper.movePip(PipTouchHandler.this.mTmpBounds);
            PipTouchHandler.this.updateDismissFraction();
            PointF curPos = touchState.getLastTouchPosition();
            if (PipTouchHandler.this.mMovementWithinMinimize) {
                boolean unused2 = PipTouchHandler.this.mMovementWithinMinimize = !this.mStartedOnLeft ? curPos.x >= ((float) PipTouchHandler.this.mMovementBounds.right) : curPos.x <= ((float) (PipTouchHandler.this.mMovementBounds.left + PipTouchHandler.this.mTmpBounds.width()));
            }
            if (PipTouchHandler.this.mMovementWithinDismiss) {
                PipTouchHandler pipTouchHandler = PipTouchHandler.this;
                if (curPos.y >= ((float) PipTouchHandler.this.mMovementBounds.bottom)) {
                    z = true;
                }
                boolean unused3 = pipTouchHandler.mMovementWithinDismiss = z;
            }
            return true;
        }

        /* JADX WARNING: Removed duplicated region for block: B:40:0x009a  */
        /* JADX WARNING: Removed duplicated region for block: B:43:0x00a8  */
        /* JADX WARNING: Removed duplicated region for block: B:44:0x00c8  */
        /* JADX WARNING: Removed duplicated region for block: B:46:0x00d0  */
        /* JADX WARNING: Removed duplicated region for block: B:47:0x00ec  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean onUp(com.android.systemui.pip.phone.PipTouchState r18) {
            /*
                r17 = this;
                r0 = r17
                com.android.systemui.pip.phone.PipTouchHandler r1 = com.android.systemui.pip.phone.PipTouchHandler.this
                r1.cleanUpDismissTarget()
                boolean r1 = r18.isUserInteracting()
                r2 = 0
                if (r1 != 0) goto L_0x000f
                return r2
            L_0x000f:
                android.graphics.PointF r1 = r18.getVelocity()
                float r3 = r1.x
                float r3 = java.lang.Math.abs(r3)
                float r4 = r1.y
                float r4 = java.lang.Math.abs(r4)
                int r3 = (r3 > r4 ? 1 : (r3 == r4 ? 0 : -1))
                r4 = 1
                if (r3 <= 0) goto L_0x0026
                r3 = r4
                goto L_0x0027
            L_0x0026:
                r3 = r2
            L_0x0027:
                float r5 = r1.x
                float r6 = r1.y
                float r5 = android.graphics.PointF.length(r5, r6)
                com.android.systemui.pip.phone.PipTouchHandler r6 = com.android.systemui.pip.phone.PipTouchHandler.this
                com.android.systemui.statusbar.FlingAnimationUtils r6 = r6.mFlingAnimationUtils
                float r6 = r6.getMinVelocityPxPerSecond()
                int r6 = (r5 > r6 ? 1 : (r5 == r6 ? 0 : -1))
                if (r6 <= 0) goto L_0x003f
                r6 = r4
                goto L_0x0040
            L_0x003f:
                r6 = r2
            L_0x0040:
                r14 = 0
                r7 = 0
                if (r6 == 0) goto L_0x0058
                float r8 = r1.y
                int r8 = (r8 > r7 ? 1 : (r8 == r7 ? 0 : -1))
                if (r8 <= 0) goto L_0x0058
                if (r3 != 0) goto L_0x0058
                com.android.systemui.pip.phone.PipTouchHandler r8 = com.android.systemui.pip.phone.PipTouchHandler.this
                boolean r8 = r8.mMovementWithinDismiss
                if (r8 != 0) goto L_0x0056
                if (r14 == 0) goto L_0x0058
            L_0x0056:
                r8 = r4
                goto L_0x0059
            L_0x0058:
                r8 = r2
            L_0x0059:
                r15 = r8
                com.android.systemui.pip.phone.PipTouchHandler r8 = com.android.systemui.pip.phone.PipTouchHandler.this
                com.android.systemui.pip.phone.PipMotionHelper r8 = r8.mMotionHelper
                boolean r8 = r8.shouldDismissPip()
                if (r8 != 0) goto L_0x0156
                if (r15 == 0) goto L_0x006a
                goto L_0x0156
            L_0x006a:
                boolean r8 = r18.isDragging()
                if (r8 == 0) goto L_0x0102
                if (r6 == 0) goto L_0x008f
                if (r3 == 0) goto L_0x008f
                com.android.systemui.pip.phone.PipTouchHandler r8 = com.android.systemui.pip.phone.PipTouchHandler.this
                boolean r8 = r8.mMovementWithinMinimize
                if (r8 == 0) goto L_0x008f
                boolean r8 = r0.mStartedOnLeft
                if (r8 == 0) goto L_0x0087
                float r8 = r1.x
                int r7 = (r8 > r7 ? 1 : (r8 == r7 ? 0 : -1))
                if (r7 >= 0) goto L_0x008f
                goto L_0x008d
            L_0x0087:
                float r8 = r1.x
                int r7 = (r8 > r7 ? 1 : (r8 == r7 ? 0 : -1))
                if (r7 <= 0) goto L_0x008f
            L_0x008d:
                r7 = r4
                goto L_0x0090
            L_0x008f:
                r7 = r2
            L_0x0090:
                r16 = r7
                com.android.systemui.pip.phone.PipTouchHandler r7 = com.android.systemui.pip.phone.PipTouchHandler.this
                boolean r7 = r7.mIsMinimized
                if (r7 == 0) goto L_0x009f
                com.android.systemui.pip.phone.PipTouchHandler r7 = com.android.systemui.pip.phone.PipTouchHandler.this
                r7.setMinimizedStateInternal(r2)
            L_0x009f:
                r2 = 0
                com.android.systemui.pip.phone.PipTouchHandler r7 = com.android.systemui.pip.phone.PipTouchHandler.this
                int r7 = r7.mMenuState
                if (r7 == 0) goto L_0x00c8
                com.android.systemui.pip.phone.PipTouchHandler r7 = com.android.systemui.pip.phone.PipTouchHandler.this
                com.android.systemui.pip.phone.PipMenuActivityController r7 = r7.mMenuController
                com.android.systemui.pip.phone.PipTouchHandler r8 = com.android.systemui.pip.phone.PipTouchHandler.this
                int r8 = r8.mMenuState
                com.android.systemui.pip.phone.PipTouchHandler r9 = com.android.systemui.pip.phone.PipTouchHandler.this
                com.android.systemui.pip.phone.PipMotionHelper r9 = r9.mMotionHelper
                android.graphics.Rect r9 = r9.getBounds()
                com.android.systemui.pip.phone.PipTouchHandler r10 = com.android.systemui.pip.phone.PipTouchHandler.this
                android.graphics.Rect r10 = r10.mMovementBounds
                r7.showMenu(r8, r9, r10, r4)
                goto L_0x00ce
            L_0x00c8:
                com.android.systemui.pip.phone.PipTouchHandler$3$1 r7 = new com.android.systemui.pip.phone.PipTouchHandler$3$1
                r7.<init>()
                r2 = r7
            L_0x00ce:
                if (r6 == 0) goto L_0x00ec
                com.android.systemui.pip.phone.PipTouchHandler r7 = com.android.systemui.pip.phone.PipTouchHandler.this
                com.android.systemui.pip.phone.PipMotionHelper r7 = r7.mMotionHelper
                float r9 = r1.x
                float r10 = r1.y
                com.android.systemui.pip.phone.PipTouchHandler r8 = com.android.systemui.pip.phone.PipTouchHandler.this
                android.graphics.Rect r11 = r8.mMovementBounds
                com.android.systemui.pip.phone.PipTouchHandler r8 = com.android.systemui.pip.phone.PipTouchHandler.this
                android.animation.ValueAnimator$AnimatorUpdateListener r12 = r8.mUpdateScrimListener
                r8 = r5
                r13 = r2
                r7.flingToSnapTarget(r8, r9, r10, r11, r12, r13)
                goto L_0x0101
            L_0x00ec:
                com.android.systemui.pip.phone.PipTouchHandler r7 = com.android.systemui.pip.phone.PipTouchHandler.this
                com.android.systemui.pip.phone.PipMotionHelper r7 = r7.mMotionHelper
                com.android.systemui.pip.phone.PipTouchHandler r8 = com.android.systemui.pip.phone.PipTouchHandler.this
                android.graphics.Rect r8 = r8.mMovementBounds
                com.android.systemui.pip.phone.PipTouchHandler r9 = com.android.systemui.pip.phone.PipTouchHandler.this
                android.animation.ValueAnimator$AnimatorUpdateListener r9 = r9.mUpdateScrimListener
                r7.animateToClosestSnapTarget(r8, r9, r2)
            L_0x0101:
                goto L_0x0155
            L_0x0102:
                com.android.systemui.pip.phone.PipTouchHandler r7 = com.android.systemui.pip.phone.PipTouchHandler.this
                boolean r7 = r7.mIsMinimized
                if (r7 == 0) goto L_0x0120
                com.android.systemui.pip.phone.PipTouchHandler r7 = com.android.systemui.pip.phone.PipTouchHandler.this
                com.android.systemui.pip.phone.PipMotionHelper r7 = r7.mMotionHelper
                com.android.systemui.pip.phone.PipTouchHandler r8 = com.android.systemui.pip.phone.PipTouchHandler.this
                android.graphics.Rect r8 = r8.mMovementBounds
                r9 = 0
                r7.animateToClosestSnapTarget(r8, r9, r9)
                com.android.systemui.pip.phone.PipTouchHandler r7 = com.android.systemui.pip.phone.PipTouchHandler.this
                r7.setMinimizedStateInternal(r2)
                goto L_0x0155
            L_0x0120:
                com.android.systemui.pip.phone.PipTouchHandler r2 = com.android.systemui.pip.phone.PipTouchHandler.this
                int r2 = r2.mMenuState
                r7 = 2
                if (r2 == r7) goto L_0x0143
                com.android.systemui.pip.phone.PipTouchHandler r2 = com.android.systemui.pip.phone.PipTouchHandler.this
                com.android.systemui.pip.phone.PipMenuActivityController r2 = r2.mMenuController
                com.android.systemui.pip.phone.PipTouchHandler r8 = com.android.systemui.pip.phone.PipTouchHandler.this
                com.android.systemui.pip.phone.PipMotionHelper r8 = r8.mMotionHelper
                android.graphics.Rect r8 = r8.getBounds()
                com.android.systemui.pip.phone.PipTouchHandler r9 = com.android.systemui.pip.phone.PipTouchHandler.this
                android.graphics.Rect r9 = r9.mMovementBounds
                r2.showMenu(r7, r8, r9, r4)
                goto L_0x0155
            L_0x0143:
                com.android.systemui.pip.phone.PipTouchHandler r2 = com.android.systemui.pip.phone.PipTouchHandler.this
                com.android.systemui.pip.phone.PipMenuActivityController r2 = r2.mMenuController
                r2.hideMenu()
                com.android.systemui.pip.phone.PipTouchHandler r2 = com.android.systemui.pip.phone.PipTouchHandler.this
                com.android.systemui.pip.phone.PipMotionHelper r2 = r2.mMotionHelper
                r2.expandPip()
            L_0x0155:
                return r4
            L_0x0156:
                com.android.systemui.pip.phone.PipTouchHandler r2 = com.android.systemui.pip.phone.PipTouchHandler.this
                com.android.systemui.pip.phone.PipMotionHelper r2 = r2.mMotionHelper
                com.android.systemui.pip.phone.PipTouchHandler r7 = com.android.systemui.pip.phone.PipTouchHandler.this
                com.android.systemui.pip.phone.PipMotionHelper r7 = r7.mMotionHelper
                android.graphics.Rect r7 = r7.getBounds()
                float r8 = r1.x
                float r9 = r1.y
                com.android.systemui.pip.phone.PipTouchHandler r10 = com.android.systemui.pip.phone.PipTouchHandler.this
                android.animation.ValueAnimator$AnimatorUpdateListener r10 = r10.mUpdateScrimListener
                r2.animateDismiss(r7, r8, r9, r10)
                com.android.systemui.pip.phone.PipTouchHandler r2 = com.android.systemui.pip.phone.PipTouchHandler.this
                android.content.Context r2 = r2.mContext
                r7 = 822(0x336, float:1.152E-42)
                com.android.internal.logging.MetricsLogger.action(r2, r7, r4)
                return r4
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.pip.phone.PipTouchHandler.AnonymousClass3.onUp(com.android.systemui.pip.phone.PipTouchState):boolean");
        }
    };
    private int mDeferResizeToNormalBoundsUntilRotation = -1;
    /* access modifiers changed from: private */
    public final PipDismissViewController mDismissViewController;
    private int mDisplayRotation;
    private Rect mExpandedBounds = new Rect();
    private Rect mExpandedMovementBounds = new Rect();
    private int mExpandedShortestEdgeSize;
    /* access modifiers changed from: private */
    public final FlingAnimationUtils mFlingAnimationUtils;
    private final PipTouchGesture[] mGestures;
    /* access modifiers changed from: private */
    public Handler mHandler = new Handler();
    private int mImeHeight;
    private boolean mIsImeShowing;
    /* access modifiers changed from: private */
    public boolean mIsMinimized;
    /* access modifiers changed from: private */
    public final PipMenuActivityController mMenuController;
    private final PipMenuListener mMenuListener = new PipMenuListener();
    /* access modifiers changed from: private */
    public int mMenuState;
    /* access modifiers changed from: private */
    public final PipMotionHelper mMotionHelper;
    /* access modifiers changed from: private */
    public Rect mMovementBounds = new Rect();
    /* access modifiers changed from: private */
    public boolean mMovementWithinDismiss;
    /* access modifiers changed from: private */
    public boolean mMovementWithinMinimize;
    private Rect mNormalBounds = new Rect();
    private Rect mNormalMovementBounds = new Rect();
    private IPinnedStackController mPinnedStackController;
    /* access modifiers changed from: private */
    public float mSavedSnapFraction = -1.0f;
    private boolean mSendingHoverAccessibilityEvents;
    /* access modifiers changed from: private */
    public Runnable mShowDismissAffordance = new Runnable() {
        public void run() {
            PipTouchHandler.this.mDismissViewController.showDismissTarget();
        }
    };
    private boolean mShowPipMenuOnAnimationEnd = false;
    private final PipSnapAlgorithm mSnapAlgorithm;
    /* access modifiers changed from: private */
    public final Rect mTmpBounds = new Rect();
    private final PipTouchState mTouchState;
    /* access modifiers changed from: private */
    public ValueAnimator.AnimatorUpdateListener mUpdateScrimListener = new ValueAnimator.AnimatorUpdateListener() {
        public void onAnimationUpdate(ValueAnimator animation) {
            PipTouchHandler.this.updateDismissFraction();
        }
    };
    private final ViewConfiguration mViewConfig;

    private class PipMenuListener implements PipMenuActivityController.Listener {
        private PipMenuListener() {
        }

        public void onPipMenuStateChanged(int menuState, boolean resize) {
            PipTouchHandler.this.setMenuState(menuState, resize);
        }

        public void onPipExpand() {
            if (!PipTouchHandler.this.mIsMinimized) {
                PipTouchHandler.this.mMotionHelper.expandPip();
            }
        }

        public void onPipMinimize() {
            PipTouchHandler.this.setMinimizedStateInternal(true);
            PipTouchHandler.this.mMotionHelper.animateToClosestMinimizedState(PipTouchHandler.this.mMovementBounds, null);
        }

        public void onPipDismiss() {
            PipTouchHandler.this.mMotionHelper.dismissPip();
            MetricsLogger.action(PipTouchHandler.this.mContext, 822, 0);
        }

        public void onPipShowMenu() {
            PipTouchHandler.this.mMenuController.showMenu(2, PipTouchHandler.this.mMotionHelper.getBounds(), PipTouchHandler.this.mMovementBounds, true);
        }
    }

    public PipTouchHandler(Context context, IActivityManager activityManager, PipMenuActivityController menuController, InputConsumerController inputConsumerController) {
        this.mContext = context;
        this.mActivityManager = activityManager;
        this.mAccessibilityManager = (AccessibilityManager) context.getSystemService(AccessibilityManager.class);
        this.mViewConfig = ViewConfiguration.get(context);
        this.mMenuController = menuController;
        this.mMenuController.addListener(this.mMenuListener);
        this.mDismissViewController = new PipDismissViewController(context);
        this.mSnapAlgorithm = new PipSnapAlgorithm(this.mContext);
        this.mTouchState = new PipTouchState(this.mViewConfig);
        this.mFlingAnimationUtils = new FlingAnimationUtils(context, 2.0f);
        this.mGestures = new PipTouchGesture[]{this.mDefaultMovementGesture};
        PipMotionHelper pipMotionHelper = new PipMotionHelper(this.mContext, this.mActivityManager, this.mMenuController, this.mSnapAlgorithm, this.mFlingAnimationUtils);
        this.mMotionHelper = pipMotionHelper;
        this.mExpandedShortestEdgeSize = context.getResources().getDimensionPixelSize(R.dimen.pip_expanded_shortest_edge_size);
        inputConsumerController.setTouchListener(new InputConsumerController.TouchListener() {
            public final boolean onTouchEvent(MotionEvent motionEvent) {
                return PipTouchHandler.this.handleTouchEvent(motionEvent);
            }
        });
        inputConsumerController.setRegistrationListener(new InputConsumerController.RegistrationListener() {
            public final void onRegistrationChanged(boolean z) {
                PipTouchHandler.this.onRegistrationChanged(z);
            }
        });
        onRegistrationChanged(inputConsumerController.isRegistered());
    }

    public void setTouchEnabled(boolean enabled) {
        this.mTouchState.setAllowTouches(enabled);
    }

    public void showPictureInPictureMenu() {
        if (!this.mTouchState.isUserInteracting()) {
            this.mMenuController.showMenu(2, this.mMotionHelper.getBounds(), this.mMovementBounds, false);
        }
    }

    public void onActivityPinned() {
        this.mMenuState = 0;
        if (this.mIsMinimized) {
            setMinimizedStateInternal(false);
        }
        cleanUpDismissTarget();
        this.mShowPipMenuOnAnimationEnd = true;
    }

    public void onPinnedStackAnimationEnded() {
        this.mMotionHelper.synchronizePinnedStackBounds();
        if (this.mShowPipMenuOnAnimationEnd) {
            this.mMenuController.showMenu(1, this.mMotionHelper.getBounds(), this.mMovementBounds, true);
            this.mShowPipMenuOnAnimationEnd = false;
        }
    }

    public void onConfigurationChanged() {
        this.mMotionHelper.onConfigurationChanged();
        this.mMotionHelper.synchronizePinnedStackBounds();
    }

    public void onImeVisibilityChanged(boolean imeVisible, int imeHeight) {
        this.mIsImeShowing = imeVisible;
        this.mImeHeight = imeHeight;
    }

    public void onMovementBoundsChanged(Rect insetBounds, Rect normalBounds, Rect animatingBounds, boolean fromImeAdjustement, int displayRotation) {
        int i;
        int i2;
        Rect rect = insetBounds;
        int i3 = displayRotation;
        Rect rect2 = normalBounds;
        this.mNormalBounds = rect2;
        Rect normalMovementBounds = new Rect();
        PipSnapAlgorithm pipSnapAlgorithm = this.mSnapAlgorithm;
        Rect rect3 = this.mNormalBounds;
        if (this.mIsImeShowing) {
            i = this.mImeHeight;
        } else {
            i = 0;
        }
        pipSnapAlgorithm.getMovementBounds(rect3, rect, normalMovementBounds, i);
        float aspectRatio = ((float) normalBounds.width()) / ((float) normalBounds.height());
        Point displaySize = new Point();
        this.mContext.getDisplay().getRealSize(displaySize);
        Size expandedSize = this.mSnapAlgorithm.getSizeForAspectRatio(aspectRatio, (float) this.mExpandedShortestEdgeSize, displaySize.x, displaySize.y);
        this.mExpandedBounds.set(0, 0, expandedSize.getWidth(), expandedSize.getHeight());
        Rect expandedMovementBounds = new Rect();
        PipSnapAlgorithm pipSnapAlgorithm2 = this.mSnapAlgorithm;
        Rect rect4 = this.mExpandedBounds;
        if (this.mIsImeShowing) {
            i2 = this.mImeHeight;
        } else {
            i2 = 0;
        }
        pipSnapAlgorithm2.getMovementBounds(rect4, rect, expandedMovementBounds, i2);
        if (!fromImeAdjustement || this.mTouchState.isUserInteracting()) {
            Rect rect5 = animatingBounds;
        } else {
            Rect bounds = new Rect(animatingBounds);
            Rect toMovementBounds = this.mMenuState == 2 ? expandedMovementBounds : normalMovementBounds;
            if (this.mIsImeShowing) {
                if (bounds.top == this.mMovementBounds.bottom) {
                    bounds.offsetTo(bounds.left, toMovementBounds.bottom);
                } else {
                    bounds.offset(0, Math.min(0, toMovementBounds.bottom - bounds.top));
                }
            } else if (bounds.top == this.mMovementBounds.bottom) {
                bounds.offsetTo(bounds.left, toMovementBounds.bottom);
            }
            this.mMotionHelper.animateToIMEOffset(bounds);
        }
        this.mNormalMovementBounds = normalMovementBounds;
        this.mExpandedMovementBounds = expandedMovementBounds;
        this.mDisplayRotation = i3;
        updateMovementBounds(this.mMenuState);
        if (this.mDeferResizeToNormalBoundsUntilRotation == i3) {
            this.mMotionHelper.animateToUnexpandedState(rect2, this.mSavedSnapFraction, this.mNormalMovementBounds, this.mMovementBounds, this.mIsMinimized, true);
            this.mSavedSnapFraction = -1.0f;
            this.mDeferResizeToNormalBoundsUntilRotation = -1;
        }
    }

    /* access modifiers changed from: private */
    public void onRegistrationChanged(boolean isRegistered) {
        PipAccessibilityInteractionConnection pipAccessibilityInteractionConnection;
        AccessibilityManager accessibilityManager = this.mAccessibilityManager;
        if (isRegistered) {
            pipAccessibilityInteractionConnection = new PipAccessibilityInteractionConnection(this.mMotionHelper, new PipAccessibilityInteractionConnection.AccessibilityCallbacks() {
                public final void onAccessibilityShowMenu() {
                    PipTouchHandler.this.onAccessibilityShowMenu();
                }
            }, this.mHandler);
        } else {
            pipAccessibilityInteractionConnection = null;
        }
        accessibilityManager.setPictureInPictureActionReplacingConnection(pipAccessibilityInteractionConnection);
        if (!isRegistered && this.mTouchState.isUserInteracting()) {
            cleanUpDismissTarget();
        }
    }

    /* access modifiers changed from: private */
    public void onAccessibilityShowMenu() {
        this.mMenuController.showMenu(2, this.mMotionHelper.getBounds(), this.mMovementBounds, false);
    }

    /* access modifiers changed from: private */
    public boolean handleTouchEvent(MotionEvent ev) {
        boolean z = true;
        if (this.mPinnedStackController == null) {
            return true;
        }
        this.mTouchState.onTouchEvent(ev);
        switch (ev.getAction()) {
            case 0:
                this.mMotionHelper.synchronizePinnedStackBounds();
                for (PipTouchGesture gesture : this.mGestures) {
                    gesture.onDown(this.mTouchState);
                }
                break;
            case 1:
                updateMovementBounds(this.mMenuState);
                PipTouchGesture[] pipTouchGestureArr = this.mGestures;
                int length = pipTouchGestureArr.length;
                int i = 0;
                while (i < length && !pipTouchGestureArr[i].onUp(this.mTouchState)) {
                    i++;
                }
                break;
            case 2:
                PipTouchGesture[] pipTouchGestureArr2 = this.mGestures;
                int length2 = pipTouchGestureArr2.length;
                int i2 = 0;
                while (i2 < length2 && !pipTouchGestureArr2[i2].onMove(this.mTouchState)) {
                    i2++;
                }
                break;
            case 3:
                break;
            case 7:
            case 9:
                if (this.mAccessibilityManager.isEnabled() && !this.mSendingHoverAccessibilityEvents) {
                    AccessibilityEvent event = AccessibilityEvent.obtain(128);
                    PipAccessibilityInteractionConnection.obtainRootAccessibilityNodeInfo().recycle();
                    this.mAccessibilityManager.sendAccessibilityEvent(event);
                    this.mSendingHoverAccessibilityEvents = true;
                    break;
                }
            case 10:
                if (this.mAccessibilityManager.isEnabled() && this.mSendingHoverAccessibilityEvents) {
                    AccessibilityEvent event2 = AccessibilityEvent.obtain(256);
                    PipAccessibilityInteractionConnection.obtainRootAccessibilityNodeInfo().recycle();
                    this.mAccessibilityManager.sendAccessibilityEvent(event2);
                    this.mSendingHoverAccessibilityEvents = false;
                    break;
                }
        }
        this.mTouchState.reset();
        cleanUpDismissTarget();
        if (this.mMenuState != 0) {
            z = false;
        }
        return z;
    }

    /* access modifiers changed from: package-private */
    public void updateDismissFraction() {
        if (this.mMenuController != null) {
            Rect bounds = this.mMotionHelper.getBounds();
            float target = (float) (this.mMovementBounds.bottom + bounds.height());
            float fraction = 0.0f;
            if (((float) bounds.bottom) > target) {
                fraction = Math.min((((float) bounds.bottom) - target) / ((float) bounds.height()), 1.0f);
            }
            if (Float.compare(fraction, 0.0f) != 0 || this.mMenuState != 0) {
                this.mMenuController.setDismissFraction(fraction);
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void setPinnedStackController(IPinnedStackController controller) {
        this.mPinnedStackController = controller;
    }

    /* access modifiers changed from: package-private */
    public void setMinimizedStateInternal(boolean isMinimized) {
    }

    /* access modifiers changed from: package-private */
    public void setMinimizedState(boolean isMinimized, boolean fromController) {
    }

    /* access modifiers changed from: package-private */
    public void setMenuState(int menuState, boolean resize) {
        boolean z = false;
        if (menuState == 2) {
            Rect expandedBounds = new Rect(this.mExpandedBounds);
            if (resize) {
                this.mSavedSnapFraction = this.mMotionHelper.animateToExpandedState(expandedBounds, this.mMovementBounds, this.mExpandedMovementBounds);
            }
        } else if (menuState == 0) {
            if (resize) {
                if (this.mDeferResizeToNormalBoundsUntilRotation == -1) {
                    try {
                        int displayRotation = this.mPinnedStackController.getDisplayRotation();
                        if (this.mDisplayRotation != displayRotation) {
                            this.mDeferResizeToNormalBoundsUntilRotation = displayRotation;
                        }
                    } catch (RemoteException e) {
                        Log.e("PipTouchHandler", "Could not get display rotation from controller");
                    }
                }
                if (this.mDeferResizeToNormalBoundsUntilRotation == -1) {
                    this.mMotionHelper.animateToUnexpandedState(new Rect(this.mNormalBounds), this.mSavedSnapFraction, this.mNormalMovementBounds, this.mMovementBounds, this.mIsMinimized, false);
                    this.mSavedSnapFraction = -1.0f;
                }
            } else {
                setTouchEnabled(false);
                this.mSavedSnapFraction = -1.0f;
            }
        }
        this.mMenuState = menuState;
        updateMovementBounds(menuState);
        if (menuState != 1) {
            Context context = this.mContext;
            if (menuState == 2) {
                z = true;
            }
            MetricsLogger.visibility(context, 823, z);
        }
    }

    public PipMotionHelper getMotionHelper() {
        return this.mMotionHelper;
    }

    private void updateMovementBounds(int menuState) {
        Rect rect;
        int i = 0;
        boolean isMenuExpanded = menuState == 2;
        if (isMenuExpanded) {
            rect = this.mExpandedMovementBounds;
        } else {
            rect = this.mNormalMovementBounds;
        }
        this.mMovementBounds = rect;
        try {
            IPinnedStackController iPinnedStackController = this.mPinnedStackController;
            if (isMenuExpanded) {
                i = this.mExpandedShortestEdgeSize;
            }
            iPinnedStackController.setMinEdgeSize(i);
        } catch (RemoteException e) {
            Log.e("PipTouchHandler", "Could not set minimized state", e);
        }
    }

    /* access modifiers changed from: private */
    public void cleanUpDismissTarget() {
        this.mHandler.removeCallbacks(this.mShowDismissAffordance);
        this.mDismissViewController.destroyDismissTarget();
    }

    public void dump(PrintWriter pw, String prefix) {
        String innerPrefix = prefix + "  ";
        pw.println(prefix + "PipTouchHandler");
        pw.println(innerPrefix + "mMovementBounds=" + this.mMovementBounds);
        pw.println(innerPrefix + "mNormalBounds=" + this.mNormalBounds);
        pw.println(innerPrefix + "mNormalMovementBounds=" + this.mNormalMovementBounds);
        pw.println(innerPrefix + "mExpandedBounds=" + this.mExpandedBounds);
        pw.println(innerPrefix + "mExpandedMovementBounds=" + this.mExpandedMovementBounds);
        pw.println(innerPrefix + "mMenuState=" + this.mMenuState);
        pw.println(innerPrefix + "mIsMinimized=" + this.mIsMinimized);
        pw.println(innerPrefix + "mIsImeShowing=" + this.mIsImeShowing);
        pw.println(innerPrefix + "mImeHeight=" + this.mImeHeight);
        pw.println(innerPrefix + "mSavedSnapFraction=" + this.mSavedSnapFraction);
        pw.println(innerPrefix + "mEnableDragToEdgeDismiss=" + true);
        pw.println(innerPrefix + "mEnableMinimize=" + false);
        this.mSnapAlgorithm.dump(pw, innerPrefix);
        this.mTouchState.dump(pw, innerPrefix);
        this.mMotionHelper.dump(pw, innerPrefix);
    }
}
