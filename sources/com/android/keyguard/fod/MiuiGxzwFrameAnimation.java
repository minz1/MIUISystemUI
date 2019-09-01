package com.android.keyguard.fod;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public class MiuiGxzwFrameAnimation {
    /* access modifiers changed from: private */
    public volatile float alpha = 1.0f;
    private final SurfaceHolder.Callback mCallBack;
    private final Context mContext;
    private final Handler mDrawHandler;
    private DrawRunnable mDrawRunnable;
    /* access modifiers changed from: private */
    public final HandlerThread mDrawThread;
    /* access modifiers changed from: private */
    public volatile int mFrameInterval = 32;
    /* access modifiers changed from: private */
    public final Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean mLastDrawAnim = false;
    /* access modifiers changed from: private */
    public volatile int mMode = 1;
    /* access modifiers changed from: private */
    public final Queue<Bitmap> mRecycleBitmapQueue = new ArrayBlockingQueue(2, true);
    private volatile boolean mSupportInBitmap = true;
    private final SurfaceHolder mSurfaceHolder;
    private final SurfaceView mSurfaceView;

    public interface CustomerDrawBitmap {
        void drawBitmap(Canvas canvas, Bitmap bitmap, Matrix matrix);
    }

    private class DrawRunnable implements Runnable {
        private final int[] mAnimRes;
        private final int mBackgroundFrame;
        private final int mBackgroundRes;
        private volatile int mCurrentPosition;
        private final CustomerDrawBitmap mCustomerDrawBitmap;
        private boolean mDrawing;
        /* access modifiers changed from: private */
        public final FrameAnimationListener mFrameAnimationListener;

        private DrawRunnable(int[] animRes, int startPosition, int backgroundRes, int backgroundFrame, FrameAnimationListener l, CustomerDrawBitmap customerDrawBitmap) {
            this.mDrawing = false;
            this.mAnimRes = animRes;
            this.mCurrentPosition = startPosition % animRes.length;
            this.mBackgroundRes = backgroundRes;
            this.mBackgroundFrame = backgroundFrame;
            this.mFrameAnimationListener = l;
            this.mCustomerDrawBitmap = customerDrawBitmap;
        }

        public synchronized void setDrawing(boolean d) {
            this.mDrawing = d;
        }

        public synchronized boolean getDrawing() {
            return this.mDrawing;
        }

        public int getCurrentPosition() {
            return this.mCurrentPosition;
        }

        public void stopDraw() {
            setDrawing(false);
            MiuiGxzwFrameAnimation.this.mDrawThread.interrupt();
            MiuiGxzwFrameAnimation.this.mRecycleBitmapQueue.clear();
        }

        public void run() {
            setDrawing(true);
            notifyStart();
            if (this.mAnimRes == null || this.mAnimRes.length == 0) {
                notifyFinish();
                setDrawing(false);
                return;
            }
            boolean interrupt = true;
            MiuiGxzwFrameAnimation.this.mRecycleBitmapQueue.clear();
            Bitmap background = this.mBackgroundRes == 0 ? null : MiuiGxzwFrameAnimation.this.decodeBitmap(this.mBackgroundRes);
            int count = 0;
            while (true) {
                if (!getDrawing()) {
                    break;
                }
                long now = System.currentTimeMillis();
                int res = this.mAnimRes[this.mCurrentPosition];
                if (res == 0 || MiuiGxzwFrameAnimation.this.alpha < 0.01f) {
                    MiuiGxzwFrameAnimation.this.clearSurface();
                } else {
                    Bitmap bitmap = MiuiGxzwFrameAnimation.this.decodeBitmap(res);
                    if (bitmap == null) {
                        stopDraw();
                        break;
                    } else {
                        MiuiGxzwFrameAnimation.this.drawBitmap(bitmap, (count < this.mBackgroundFrame || this.mBackgroundFrame <= 0) ? background : null, 1.0f, this.mCustomerDrawBitmap);
                        MiuiGxzwFrameAnimation.this.mRecycleBitmapQueue.offer(bitmap);
                    }
                }
                this.mCurrentPosition++;
                count++;
                if (this.mCurrentPosition == this.mAnimRes.length) {
                    if (MiuiGxzwFrameAnimation.this.mMode == 1) {
                        interrupt = false;
                        MiuiGxzwFrameAnimation.this.mRecycleBitmapQueue.clear();
                        break;
                    } else if (MiuiGxzwFrameAnimation.this.mMode == 2) {
                        this.mCurrentPosition = 0;
                    }
                }
                try {
                    long spend = System.currentTimeMillis() - now;
                    if (((long) MiuiGxzwFrameAnimation.this.mFrameInterval) - spend > 0) {
                        Thread.sleep(((long) MiuiGxzwFrameAnimation.this.mFrameInterval) - spend);
                    }
                } catch (InterruptedException e) {
                }
            }
            setDrawing(false);
            if (interrupt) {
                notifyInterrupt();
            } else {
                notifyFinish();
            }
        }

        private void notifyStart() {
            if (this.mFrameAnimationListener != null) {
                MiuiGxzwFrameAnimation.this.mHandler.post(new Runnable() {
                    public void run() {
                        DrawRunnable.this.mFrameAnimationListener.onStart();
                    }
                });
            }
        }

        private void notifyInterrupt() {
            if (this.mFrameAnimationListener != null) {
                MiuiGxzwFrameAnimation.this.mHandler.post(new Runnable() {
                    public void run() {
                        DrawRunnable.this.mFrameAnimationListener.onInterrupt();
                    }
                });
            }
        }

        private void notifyFinish() {
            if (this.mFrameAnimationListener != null) {
                MiuiGxzwFrameAnimation.this.mHandler.post(new Runnable() {
                    public void run() {
                        DrawRunnable.this.mFrameAnimationListener.onFinish();
                    }
                });
            }
        }
    }

    public interface FrameAnimationListener {
        void onFinish();

        void onInterrupt();

        void onStart();
    }

    public MiuiGxzwFrameAnimation(SurfaceView surfaceView, SurfaceHolder.Callback callback) {
        this.mSurfaceView = surfaceView;
        this.mSurfaceHolder = surfaceView.getHolder();
        this.mCallBack = callback;
        this.mSurfaceHolder.setFormat(-3);
        this.mSurfaceView.setZOrderOnTop(true);
        this.mSurfaceHolder.addCallback(this.mCallBack);
        this.mContext = surfaceView.getContext();
        this.mDrawThread = new HandlerThread("FrameAnimation Draw Thread");
        this.mDrawThread.start();
        this.mDrawHandler = new Handler(this.mDrawThread.getLooper());
    }

    public void setFrameInterval(int frameInterval) {
        if (frameInterval >= 0) {
            this.mFrameInterval = frameInterval;
            return;
        }
        throw new UnsupportedOperationException("frameInterval < 0");
    }

    public void setMode(int mode) {
        if (mode == 1 || mode == 2) {
            this.mMode = mode;
            return;
        }
        throw new UnsupportedOperationException("wrong mode: " + mode);
    }

    public void startAnimation(int[] res, int startPosition, int backgroundRes, int backgroundFrame, FrameAnimationListener l, CustomerDrawBitmap customerDrawBitmap) {
        this.mLastDrawAnim = false;
        stopAnimation();
        DrawRunnable drawRunnable = new DrawRunnable(res, startPosition, backgroundRes, backgroundFrame, l, customerDrawBitmap);
        this.mDrawRunnable = drawRunnable;
        this.mDrawHandler.post(this.mDrawRunnable);
    }

    public void stopAnimation() {
        if (this.mDrawRunnable != null) {
            this.mDrawHandler.removeCallbacks(this.mDrawRunnable);
            if (this.mDrawRunnable.getDrawing()) {
                this.mDrawRunnable.stopDraw();
            }
        }
        this.mDrawRunnable = null;
    }

    public boolean isAniming() {
        if (this.mDrawRunnable == null || !this.mDrawRunnable.getDrawing()) {
            return false;
        }
        return true;
    }

    public int getCurrentPosition() {
        if (isAniming()) {
            return this.mDrawRunnable.getCurrentPosition();
        }
        return 0;
    }

    public void draw(final int res, boolean anim, final float scale) {
        Log.i("MiuiGxzwFrameAnimation", "draw: res = " + res + ", anim = " + anim + ", scale = " + scale);
        stopAnimation();
        boolean oldLastDrawAnim = this.mLastDrawAnim;
        final boolean newLastDrawAnim = anim;
        this.mLastDrawAnim = anim;
        if ((!oldLastDrawAnim && newLastDrawAnim) || !newLastDrawAnim) {
            this.mRecycleBitmapQueue.clear();
        }
        this.mDrawHandler.post(new Runnable() {
            public void run() {
                Bitmap bitmap = MiuiGxzwFrameAnimation.this.decodeBitmap(res);
                if (bitmap != null) {
                    MiuiGxzwFrameAnimation.this.drawBitmap(bitmap, scale);
                    if (newLastDrawAnim) {
                        MiuiGxzwFrameAnimation.this.mRecycleBitmapQueue.offer(bitmap);
                    }
                }
            }
        });
    }

    public void clean() {
        Log.i("MiuiGxzwFrameAnimation", "clean");
        stopAnimation();
        this.mDrawHandler.post(new Runnable() {
            public void run() {
                MiuiGxzwFrameAnimation.this.clearSurface();
            }
        });
    }

    private Matrix configureDrawMatrix(Bitmap bitmap, float scale) {
        Matrix matrix = new Matrix();
        int srcWidth = bitmap.getWidth();
        int dstWidth = this.mSurfaceView.getWidth();
        int srcHeight = bitmap.getHeight();
        int dstHeight = this.mSurfaceView.getHeight();
        matrix.setScale(scale, scale);
        matrix.postTranslate((float) Math.round((((float) dstWidth) - (((float) srcWidth) * scale)) * 0.5f), (float) Math.round((((float) dstHeight) - (((float) srcHeight) * scale)) * 0.5f));
        return matrix;
    }

    /* access modifiers changed from: private */
    public void drawBitmap(Bitmap bitmap, float scale) {
        drawBitmap(bitmap, null, scale, null);
    }

    /* access modifiers changed from: private */
    public void drawBitmap(Bitmap bitmap, Bitmap background, float scale, CustomerDrawBitmap customer) {
        Canvas canvas = this.mSurfaceHolder.lockCanvas();
        if (canvas == null || bitmap == null) {
            Log.i("MiuiGxzwFrameAnimation", "drawBitmap: bitmap or canvas is null");
            return;
        }
        try {
            Matrix matrix = configureDrawMatrix(bitmap, scale);
            canvas.drawColor(0, PorterDuff.Mode.CLEAR);
            if (background != null) {
                canvas.drawBitmap(background, configureDrawMatrix(background, scale), null);
            }
            if (customer == null) {
                canvas.drawBitmap(bitmap, matrix, null);
            } else {
                customer.drawBitmap(canvas, bitmap, matrix);
            }
        } finally {
            unlockCanvasAndPostSafely(canvas);
        }
    }

    /* access modifiers changed from: private */
    public void clearSurface() {
        Canvas canvas = this.mSurfaceHolder.lockCanvas();
        if (canvas != null) {
            try {
                canvas.drawColor(0, PorterDuff.Mode.CLEAR);
            } finally {
                unlockCanvasAndPostSafely(canvas);
            }
        }
    }

    private void unlockCanvasAndPostSafely(Canvas canvas) {
        Surface surface = this.mSurfaceHolder.getSurface();
        if (surface != null && surface.isValid()) {
            this.mSurfaceHolder.unlockCanvasAndPost(canvas);
        }
    }

    /* access modifiers changed from: private */
    public Bitmap decodeBitmap(int res) {
        Bitmap inBitmap = null;
        if (this.mSupportInBitmap && this.mRecycleBitmapQueue.size() >= 2) {
            inBitmap = this.mRecycleBitmapQueue.poll();
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        options.inSampleSize = 1;
        options.inBitmap = inBitmap;
        try {
            return BitmapFactory.decodeResource(this.mContext.getResources(), res, options);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
