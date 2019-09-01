package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.session.MediaSessionLegacyHelperCompat;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.InputQueue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import android.widget.FrameLayout;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.view.FloatingActionMode;
import com.android.internal.view.FloatingActionModeCompat;
import com.android.internal.widget.FloatingToolbar;
import com.android.internal.widget.FloatingToolbarCompat;
import com.android.systemui.R;
import com.android.systemui.classifier.FalsingManager;
import com.android.systemui.statusbar.DisableDragDownHelper;
import com.android.systemui.statusbar.DragDownHelper;
import com.android.systemui.statusbar.phone.DoubleTapHelper;
import com.android.systemui.statusbar.stack.NotificationStackScrollLayout;

public class StatusBarWindowView extends FrameLayout {
    public static final boolean DEBUG = StatusBar.DEBUG;
    private View mBrightnessMirror;
    private DoubleTapHelper mDoubleTapHelper;
    private DragDownHelper mDragDownHelper;
    private boolean mExpandingBelowNotch;
    private Window mFakeWindow = new Window(this.mContext) {
        public void takeSurface(SurfaceHolder.Callback2 callback) {
        }

        public void takeInputQueue(InputQueue.Callback callback) {
        }

        public boolean isFloating() {
            return false;
        }

        public void alwaysReadCloseOnTouchAttr() {
        }

        public void setContentView(int layoutResID) {
        }

        public void setContentView(View view) {
        }

        public void setContentView(View view, ViewGroup.LayoutParams params) {
        }

        public void addContentView(View view, ViewGroup.LayoutParams params) {
        }

        public void clearContentView() {
        }

        public View getCurrentFocus() {
            return null;
        }

        public LayoutInflater getLayoutInflater() {
            return null;
        }

        public void setTitle(CharSequence title) {
        }

        public void setTitleColor(int textColor) {
        }

        public void openPanel(int featureId, KeyEvent event) {
        }

        public void closePanel(int featureId) {
        }

        public void togglePanel(int featureId, KeyEvent event) {
        }

        public void invalidatePanelMenu(int featureId) {
        }

        public boolean performPanelShortcut(int featureId, int keyCode, KeyEvent event, int flags) {
            return false;
        }

        public boolean performPanelIdentifierAction(int featureId, int id, int flags) {
            return false;
        }

        public void closeAllPanels() {
        }

        public boolean performContextMenuIdentifierAction(int id, int flags) {
            return false;
        }

        public void onConfigurationChanged(Configuration newConfig) {
        }

        public void setBackgroundDrawable(Drawable drawable) {
        }

        public void setFeatureDrawableResource(int featureId, int resId) {
        }

        public void setFeatureDrawableUri(int featureId, Uri uri) {
        }

        public void setFeatureDrawable(int featureId, Drawable drawable) {
        }

        public void setFeatureDrawableAlpha(int featureId, int alpha) {
        }

        public void setFeatureInt(int featureId, int value) {
        }

        public void takeKeyEvents(boolean get) {
        }

        public boolean superDispatchKeyEvent(KeyEvent event) {
            return false;
        }

        public boolean superDispatchKeyShortcutEvent(KeyEvent event) {
            return false;
        }

        public boolean superDispatchTouchEvent(MotionEvent event) {
            return false;
        }

        public boolean superDispatchTrackballEvent(MotionEvent event) {
            return false;
        }

        public boolean superDispatchGenericMotionEvent(MotionEvent event) {
            return false;
        }

        public View getDecorView() {
            return StatusBarWindowView.this;
        }

        public View peekDecorView() {
            return null;
        }

        public Bundle saveHierarchyState() {
            return null;
        }

        public void restoreHierarchyState(Bundle savedInstanceState) {
        }

        /* access modifiers changed from: protected */
        public void onActive() {
        }

        public void setChildDrawable(int featureId, Drawable drawable) {
        }

        public void setChildInt(int featureId, int value) {
        }

        public boolean isShortcutKey(int keyCode, KeyEvent event) {
            return false;
        }

        public void setVolumeControlStream(int streamType) {
        }

        public int getVolumeControlStream() {
            return 0;
        }

        public int getStatusBarColor() {
            return 0;
        }

        public void setStatusBarColor(int color) {
        }

        public int getNavigationBarColor() {
            return 0;
        }

        public void setNavigationBarColor(int color) {
        }

        public void setDecorCaptionShade(int decorCaptionShade) {
        }

        public void setResizingCaptionDrawable(Drawable drawable) {
        }

        public void onMultiWindowModeChanged() {
        }

        public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        }

        public void reportActivityRelaunched() {
        }
    };
    private FalsingManager mFalsingManager;
    /* access modifiers changed from: private */
    public ActionMode mFloatingActionMode;
    private View mFloatingActionModeOriginatingView;
    private FloatingToolbar mFloatingToolbar;
    private ViewTreeObserver.OnPreDrawListener mFloatingToolbarPreDrawListener;
    private int mLeftInset = 0;
    private NotificationPanelView mNotificationPanel;
    private int mRightInset = 0;
    /* access modifiers changed from: private */
    public StatusBar mService;
    private NotificationStackScrollLayout mStackScrollLayout;
    private PhoneStatusBarView mStatusBarView;
    private boolean mTouchActive;
    private boolean mTouchCancelled;
    private final Paint mTransparentSrcPaint = new Paint();

    private class ActionModeCallback2Wrapper extends ActionMode.Callback2 {
        private final ActionMode.Callback mWrapped;

        public ActionModeCallback2Wrapper(ActionMode.Callback wrapped) {
            this.mWrapped = wrapped;
        }

        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            return this.mWrapped.onCreateActionMode(mode, menu);
        }

        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            StatusBarWindowView.this.requestFitSystemWindows();
            return this.mWrapped.onPrepareActionMode(mode, menu);
        }

        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return this.mWrapped.onActionItemClicked(mode, item);
        }

        public void onDestroyActionMode(ActionMode mode) {
            this.mWrapped.onDestroyActionMode(mode);
            if (mode == StatusBarWindowView.this.mFloatingActionMode) {
                StatusBarWindowView.this.cleanupFloatingActionModeViews();
                ActionMode unused = StatusBarWindowView.this.mFloatingActionMode = null;
            }
            StatusBarWindowView.this.requestFitSystemWindows();
        }

        public void onGetContentRect(ActionMode mode, View view, Rect outRect) {
            if (this.mWrapped instanceof ActionMode.Callback2) {
                ((ActionMode.Callback2) this.mWrapped).onGetContentRect(mode, view, outRect);
            } else {
                super.onGetContentRect(mode, view, outRect);
            }
        }
    }

    public class LayoutParams extends FrameLayout.LayoutParams {
        public boolean ignoreRightInset;

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.StatusBarWindowView_Layout);
            this.ignoreRightInset = a.getBoolean(0, false);
            a.recycle();
        }
    }

    public StatusBarWindowView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setMotionEventSplittingEnabled(false);
        this.mTransparentSrcPaint.setColor(0);
        this.mTransparentSrcPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
        this.mFalsingManager = FalsingManager.getInstance(context);
        DoubleTapHelper doubleTapHelper = new DoubleTapHelper(this, new DoubleTapHelper.ActivationListener() {
            public void onActiveChanged(boolean active) {
            }
        }, new DoubleTapHelper.DoubleTapListener() {
            public boolean onDoubleTap() {
                StatusBarWindowView.this.mService.wakeUpIfDozing(SystemClock.uptimeMillis(), StatusBarWindowView.this);
                return true;
            }
        }, null, null);
        this.mDoubleTapHelper = doubleTapHelper;
    }

    /* access modifiers changed from: protected */
    public boolean fitSystemWindows(Rect insets) {
        boolean changed = true;
        if (getFitsSystemWindows()) {
            if (insets.top == getPaddingTop() && insets.bottom == getPaddingBottom()) {
                changed = false;
            }
            boolean paddingChanged = changed;
            if (!(insets.right == this.mRightInset && insets.left == this.mLeftInset)) {
                this.mRightInset = insets.right;
                this.mLeftInset = insets.left;
                applyMargins();
            }
            if (paddingChanged) {
                setPadding(0, 0, 0, 0);
            }
            insets.left = 0;
            insets.top = 0;
            insets.right = 0;
        } else {
            if (!(this.mRightInset == 0 && this.mLeftInset == 0)) {
                this.mRightInset = 0;
                this.mLeftInset = 0;
                applyMargins();
            }
            if (getPaddingLeft() == 0 && getPaddingRight() == 0 && getPaddingTop() == 0 && getPaddingBottom() == 0) {
                changed = false;
            }
            if (changed) {
                setPadding(0, 0, 0, 0);
            }
            insets.top = 0;
        }
        return false;
    }

    private void applyMargins() {
        int N = getChildCount();
        for (int i = 0; i < N; i++) {
            View child = getChildAt(i);
            if ((child.getLayoutParams() instanceof LayoutParams) && child.getId() != R.id.brightness_mirror) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (!lp.ignoreRightInset && !(lp.rightMargin == this.mRightInset && lp.leftMargin == this.mLeftInset)) {
                    lp.rightMargin = this.mRightInset;
                    lp.leftMargin = this.mLeftInset;
                    child.requestLayout();
                }
            }
        }
    }

    public FrameLayout.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    /* access modifiers changed from: protected */
    public FrameLayout.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(-1, -1);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mStackScrollLayout = (NotificationStackScrollLayout) findViewById(R.id.notification_stack_scroller);
        this.mNotificationPanel = (NotificationPanelView) findViewById(R.id.notification_panel);
        this.mBrightnessMirror = findViewById(R.id.brightness_mirror);
    }

    public void onViewAdded(View child) {
        super.onViewAdded(child);
        if (child.getId() == R.id.brightness_mirror) {
            this.mBrightnessMirror = child;
        }
    }

    public void setStatusBarView(PhoneStatusBarView statusBarView) {
        this.mStatusBarView = statusBarView;
    }

    public void setService(StatusBar service) {
        this.mService = service;
        setDragDownHelper(new DisableDragDownHelper(getContext(), this, this.mStackScrollLayout, this.mService));
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public void setDragDownHelper(DragDownHelper dragDownHelper) {
        this.mDragDownHelper = dragDownHelper;
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (this.mService.isScrimSrcModeEnabled()) {
            IBinder windowToken = getWindowToken();
            WindowManager.LayoutParams lp = (WindowManager.LayoutParams) getLayoutParams();
            lp.token = windowToken;
            setLayoutParams(lp);
            WindowManagerGlobal.getInstance().changeCanvasOpacity(windowToken, true);
            setWillNotDraw(false);
            return;
        }
        setWillNotDraw(!DEBUG);
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        if (this.mService.interceptMediaKey(event) || super.dispatchKeyEvent(event)) {
            return true;
        }
        boolean down = event.getAction() == 0;
        int keyCode = event.getKeyCode();
        if (keyCode != 4) {
            if (keyCode != 62) {
                if (keyCode != 82) {
                    switch (keyCode) {
                        case 24:
                        case 25:
                            if (this.mService.isDozing()) {
                                MediaSessionLegacyHelperCompat.sendVolumeKeyEvent(getContext(), event, Integer.MIN_VALUE, true);
                                return true;
                            }
                            break;
                    }
                    return false;
                } else if (!down) {
                    return this.mService.onMenuPressed();
                }
            }
            if (!down) {
                return this.mService.onSpacePressed();
            }
            return false;
        }
        if (!down) {
            this.mService.onBackPressed();
        }
        return true;
    }

    public boolean dispatchTouchEvent(MotionEvent ev) {
        boolean isDown = ev.getActionMasked() == 0;
        boolean isUp = ev.getActionMasked() == 1;
        boolean isCancel = ev.getActionMasked() == 3;
        boolean expandingBelowNotch = this.mExpandingBelowNotch;
        if (isUp || isCancel) {
            this.mExpandingBelowNotch = false;
        }
        if (isDown && this.mNotificationPanel.isFullyCollapsed()) {
            this.mNotificationPanel.startExpandLatencyTracking();
        }
        if (isDown) {
            this.mTouchActive = true;
            this.mTouchCancelled = false;
        } else if (ev.getActionMasked() == 1 || ev.getActionMasked() == 3) {
            this.mTouchActive = false;
        } else if (ev.getActionMasked() == 5 && this.mService.getBarState() == 1 && this.mNotificationPanel.mIsDefaultTheme) {
            cancelCurrentTouch();
        }
        if (this.mTouchCancelled) {
            return false;
        }
        this.mFalsingManager.onTouchEvent(ev, getWidth(), getHeight());
        if (this.mBrightnessMirror != null && this.mBrightnessMirror.getVisibility() == 0 && ev.getActionMasked() == 5) {
            return false;
        }
        if (isDown) {
            this.mStackScrollLayout.closeControlsIfOutsideTouch(ev);
        }
        if (this.mService.isDozing()) {
            this.mService.mDozeScrimController.extendPulse();
        }
        if (isDown && ev.getY() >= ((float) this.mBottom)) {
            this.mExpandingBelowNotch = true;
            expandingBelowNotch = true;
        }
        if (expandingBelowNotch) {
            return this.mStatusBarView.dispatchTouchEvent(ev);
        }
        return super.dispatchTouchEvent(ev);
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (this.mService.isDozing() && !this.mStackScrollLayout.hasPulsingNotifications()) {
            return true;
        }
        boolean intercept = false;
        if (this.mNotificationPanel.isFullyExpanded() && this.mNotificationPanel.isInCenterScreen() && this.mStackScrollLayout.getVisibility() == 0 && this.mService.getBarState() == 1 && !this.mService.isBouncerShowing() && !this.mService.isDozing()) {
            intercept = this.mDragDownHelper.onInterceptTouchEvent(ev);
        }
        if (!intercept) {
            super.onInterceptTouchEvent(ev);
        }
        if (intercept) {
            MotionEvent cancellation = MotionEvent.obtain(ev);
            cancellation.setAction(3);
            this.mStackScrollLayout.onInterceptTouchEvent(cancellation);
            this.mNotificationPanel.onInterceptTouchEvent(cancellation);
            cancellation.recycle();
        }
        return intercept;
    }

    public boolean onTouchEvent(MotionEvent ev) {
        boolean handled = false;
        if (this.mService.isDozing()) {
            this.mDoubleTapHelper.onTouchEvent(ev);
            handled = true;
        }
        if ((this.mService.getBarState() == 1 && !handled) || this.mDragDownHelper.isDraggingDown()) {
            handled = this.mDragDownHelper.onTouchEvent(ev);
        }
        if (!handled) {
            handled = super.onTouchEvent(ev);
        }
        int action = ev.getAction();
        if (!handled && (action == 1 || action == 3)) {
            this.mService.setInteracting(1, false);
        }
        return handled;
    }

    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.mService.isScrimSrcModeEnabled()) {
            int paddedBottom = getHeight() - getPaddingBottom();
            int paddedRight = getWidth() - getPaddingRight();
            if (getPaddingTop() != 0) {
                canvas.drawRect(0.0f, 0.0f, (float) getWidth(), (float) getPaddingTop(), this.mTransparentSrcPaint);
            }
            if (getPaddingBottom() != 0) {
                canvas.drawRect(0.0f, (float) paddedBottom, (float) getWidth(), (float) getHeight(), this.mTransparentSrcPaint);
            }
            if (getPaddingLeft() != 0) {
                canvas.drawRect(0.0f, (float) getPaddingTop(), (float) getPaddingLeft(), (float) paddedBottom, this.mTransparentSrcPaint);
            }
            if (getPaddingRight() != 0) {
                canvas.drawRect((float) paddedRight, (float) getPaddingTop(), (float) getWidth(), (float) paddedBottom, this.mTransparentSrcPaint);
            }
        }
        if (DEBUG != 0) {
            Paint pt = new Paint();
            pt.setColor(-2130706688);
            pt.setStrokeWidth(12.0f);
            pt.setStyle(Paint.Style.STROKE);
            canvas.drawRect(0.0f, 0.0f, (float) canvas.getWidth(), (float) canvas.getHeight(), pt);
        }
    }

    public void cancelExpandHelper() {
        if (this.mStackScrollLayout != null) {
            this.mStackScrollLayout.cancelExpandHelper();
        }
    }

    public void cancelCurrentTouch() {
        if (this.mTouchActive) {
            long now = SystemClock.uptimeMillis();
            MotionEvent event = MotionEvent.obtain(now, now, 3, 0.0f, 0.0f, 0);
            event.setSource(4098);
            dispatchTouchEvent(event);
            event.recycle();
            this.mTouchCancelled = true;
        }
    }

    public ActionMode startActionModeForChild(View originalView, ActionMode.Callback callback, int type) {
        if (type == 1) {
            return startActionMode(originalView, callback, type);
        }
        return super.startActionModeForChild(originalView, callback, type);
    }

    private ActionMode createFloatingActionMode(View originatingView, ActionMode.Callback2 callback) {
        if (this.mFloatingActionMode != null) {
            this.mFloatingActionMode.finish();
        }
        cleanupFloatingActionModeViews();
        this.mFloatingToolbar = FloatingToolbarCompat.newFloatingToolbar(this.mContext, this.mFakeWindow);
        final FloatingActionMode mode = FloatingActionModeCompat.newFloatingActionMode(this.mContext, callback, originatingView, this.mFloatingToolbar);
        this.mFloatingActionModeOriginatingView = originatingView;
        this.mFloatingToolbarPreDrawListener = new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                mode.updateViewLocationInWindow();
                return true;
            }
        };
        return mode;
    }

    private void setHandledFloatingActionMode(ActionMode mode) {
        this.mFloatingActionMode = mode;
        this.mFloatingActionMode.invalidate();
        this.mFloatingActionModeOriginatingView.getViewTreeObserver().addOnPreDrawListener(this.mFloatingToolbarPreDrawListener);
    }

    /* access modifiers changed from: private */
    public void cleanupFloatingActionModeViews() {
        if (this.mFloatingToolbar != null) {
            this.mFloatingToolbar.dismiss();
            this.mFloatingToolbar = null;
        }
        if (this.mFloatingActionModeOriginatingView != null) {
            if (this.mFloatingToolbarPreDrawListener != null) {
                this.mFloatingActionModeOriginatingView.getViewTreeObserver().removeOnPreDrawListener(this.mFloatingToolbarPreDrawListener);
                this.mFloatingToolbarPreDrawListener = null;
            }
            this.mFloatingActionModeOriginatingView = null;
        }
    }

    private ActionMode startActionMode(View originatingView, ActionMode.Callback callback, int type) {
        ActionMode.Callback2 wrappedCallback = new ActionModeCallback2Wrapper(callback);
        ActionMode mode = createFloatingActionMode(originatingView, wrappedCallback);
        if (mode == null || !wrappedCallback.onCreateActionMode(mode, mode.getMenu())) {
            return null;
        }
        setHandledFloatingActionMode(mode);
        return mode;
    }
}
