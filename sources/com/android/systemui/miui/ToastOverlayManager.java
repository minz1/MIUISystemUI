package com.android.systemui.miui;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;
import java.lang.ref.WeakReference;

public class ToastOverlayManager implements ViewTreeObserver.OnGlobalLayoutListener {
    private static final boolean ENABLED = (Build.VERSION.SDK_INT >= 28);
    private Context mContext;
    private Runnable mDispatchClearToastRunnable = new Runnable() {
        public void run() {
            ToastOverlayManager.this.mOverlayLayout.setToast(null);
            ToastOverlayManager.this.mOverlayLayout.invalidate();
        }
    };
    private Runnable mDispatchHideToastRunnable = new Runnable() {
        public void run() {
            ToastOverlayManager.this.handleHideToast();
        }
    };
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private Toast mLastToast = null;
    /* access modifiers changed from: private */
    public ToastOverlayLayout mOverlayLayout;
    private ViewGroup mRoot;

    private static class ToastOverlayLayout extends View {
        private WeakReference<Toast> mLastToastRef = new WeakReference<>(null);
        private int mToastX;
        private int mToastY;

        public ToastOverlayLayout(Context context) {
            super(context);
        }

        public void setToast(Toast toast) {
            this.mLastToastRef = new WeakReference<>(toast);
        }

        public void setLocation(int x, int y) {
            this.mToastX = x;
            this.mToastY = y;
        }

        /* access modifiers changed from: protected */
        public void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (this.mToastX <= getWidth() && this.mToastY <= getHeight() && this.mLastToastRef.get() != null && ((Toast) this.mLastToastRef.get()).getView() != null) {
                canvas.save();
                canvas.translate((float) this.mToastX, (float) this.mToastY);
                ((Toast) this.mLastToastRef.get()).getView().draw(canvas);
                canvas.restore();
            }
        }
    }

    public void setup(Context context, ViewGroup root) {
        if (ENABLED) {
            this.mContext = context;
            this.mOverlayLayout = new ToastOverlayLayout(context);
            this.mRoot = root;
            this.mRoot.addView(this.mOverlayLayout, -1, -1);
        }
    }

    public void dispatchShowToast(final Toast toast) {
        if (ENABLED && this.mRoot != null) {
            this.mHandler.post(new Runnable() {
                public void run() {
                    ToastOverlayManager.this.handleShowToast(toast);
                }
            });
        }
    }

    /* access modifiers changed from: private */
    public void handleShowToast(Toast toast) {
        this.mHandler.removeCallbacks(this.mDispatchHideToastRunnable);
        this.mHandler.removeCallbacks(this.mDispatchClearToastRunnable);
        if (this.mLastToast != null) {
            this.mLastToast.cancel();
        }
        this.mOverlayLayout.setToast(toast);
        this.mLastToast = toast;
        if (toast.getView() != null) {
            toast.getView().getViewTreeObserver().addOnGlobalLayoutListener(this);
        }
        startAnimIfExists("toast_enter");
        this.mHandler.postDelayed(this.mDispatchHideToastRunnable, toast.getDuration() == 1 ? 3500 : 2000);
    }

    /* access modifiers changed from: private */
    public void handleHideToast() {
        Animation animation = startAnimIfExists("toast_exit");
        this.mHandler.postDelayed(this.mDispatchClearToastRunnable, animation != null ? animation.getDuration() : 300);
        this.mLastToast = null;
    }

    private Animation startAnimIfExists(String animRes) {
        int resId = this.mContext.getResources().getIdentifier(animRes, "anim", "android");
        if (resId <= 0) {
            return null;
        }
        if (this.mOverlayLayout.getAnimation() != null) {
            this.mOverlayLayout.getAnimation().cancel();
        }
        Animation animation = AnimationUtils.loadAnimation(this.mContext, resId);
        this.mOverlayLayout.startAnimation(animation);
        this.mOverlayLayout.invalidate();
        return animation;
    }

    public void onGlobalLayout() {
        if (this.mLastToast != null) {
            int[] toastLocation = new int[2];
            int[] layoutLocation = new int[2];
            this.mLastToast.getView().getLocationOnScreen(toastLocation);
            this.mOverlayLayout.getLocationOnScreen(layoutLocation);
            this.mOverlayLayout.setLocation(toastLocation[0] - layoutLocation[0], toastLocation[1] - layoutLocation[1]);
            this.mOverlayLayout.invalidate();
        }
    }
}
