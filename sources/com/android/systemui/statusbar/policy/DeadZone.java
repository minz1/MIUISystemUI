package com.android.systemui.statusbar.policy;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Slog;
import android.view.MotionEvent;
import android.view.View;
import com.android.systemui.R;

public class DeadZone extends View {
    private final Runnable mDebugFlash;
    private int mDecay;
    private int mDisplayRotation;
    private float mFlashFrac;
    private int mHold;
    private long mLastPokeTime;
    private boolean mShouldFlash;
    private int mSizeMax;
    private int mSizeMin;
    private boolean mVertical;

    public DeadZone(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DeadZone(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);
        this.mFlashFrac = 0.0f;
        this.mDebugFlash = new Runnable() {
            public void run() {
                ObjectAnimator.ofFloat(DeadZone.this, "flash", new float[]{1.0f, 0.0f}).setDuration(150).start();
            }
        };
        boolean z = false;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DeadZone, defStyle, 0);
        this.mHold = a.getInteger(1, 0);
        this.mDecay = a.getInteger(0, 0);
        this.mSizeMin = a.getDimensionPixelSize(3, 0);
        this.mSizeMax = a.getDimensionPixelSize(2, 0);
        this.mVertical = a.getInt(4, -1) == 1 ? true : z;
        setFlashOnTouchCapture(context.getResources().getBoolean(R.bool.config_dead_zone_flash));
    }

    static float lerp(float a, float b, float f) {
        return ((b - a) * f) + a;
    }

    private float getSize(long now) {
        if (this.mSizeMax == 0) {
            return 0.0f;
        }
        long dt = now - this.mLastPokeTime;
        if (dt > ((long) (this.mHold + this.mDecay))) {
            return (float) this.mSizeMin;
        }
        if (dt < ((long) this.mHold)) {
            return (float) this.mSizeMax;
        }
        return (float) ((int) lerp((float) this.mSizeMax, (float) this.mSizeMin, ((float) (dt - ((long) this.mHold))) / ((float) this.mDecay)));
    }

    public void setFlashOnTouchCapture(boolean dbg) {
        this.mShouldFlash = dbg;
        this.mFlashFrac = 0.0f;
        postInvalidate();
    }

    public boolean onTouchEvent(MotionEvent event) {
        boolean consumeEvent;
        if (event.getToolType(0) == 3) {
            return false;
        }
        int action = event.getAction();
        if (action == 4) {
            poke(event);
            return true;
        }
        if (action == 0) {
            int size = (int) getSize(event.getEventTime());
            if (this.mVertical) {
                consumeEvent = this.mDisplayRotation == 3 ? event.getX() > ((float) (getWidth() - size)) : event.getX() < ((float) size);
            } else {
                consumeEvent = event.getY() < ((float) size);
            }
            if (consumeEvent) {
                Slog.v("DeadZone", "consuming errant click: (" + event.getX() + "," + event.getY() + ")");
                if (this.mShouldFlash) {
                    post(this.mDebugFlash);
                    postInvalidate();
                }
                return true;
            }
        }
        return false;
    }

    public void poke(MotionEvent event) {
        this.mLastPokeTime = event.getEventTime();
        if (this.mShouldFlash) {
            postInvalidate();
        }
    }

    public void onDraw(Canvas can) {
        if (this.mShouldFlash && this.mFlashFrac > 0.0f) {
            int size = (int) getSize(SystemClock.uptimeMillis());
            if (!this.mVertical) {
                can.clipRect(0, 0, can.getWidth(), size);
            } else if (this.mDisplayRotation == 3) {
                can.clipRect(can.getWidth() - size, 0, can.getWidth(), can.getHeight());
            } else {
                can.clipRect(0, 0, size, can.getHeight());
            }
            can.drawARGB((int) (255.0f * this.mFlashFrac), 221, 238, 170);
        }
    }
}
