package com.android.systemui.statusbar.policy;

import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.hardware.input.InputManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ImageView;
import com.android.systemui.R;

public class KeyButtonView extends ImageView {
    private AudioManager mAudioManager;
    private final Runnable mCheckLongPress;
    private int mCode;
    private int mContentDescriptionRes;
    private long mDownTime;
    private boolean mGestureAborted;
    /* access modifiers changed from: private */
    public boolean mSupportsLongpress;
    private int mTouchSlop;

    public KeyButtonView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KeyButtonView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);
        this.mSupportsLongpress = true;
        this.mCheckLongPress = new Runnable() {
            public void run() {
                if (!KeyButtonView.this.isPressed()) {
                    return;
                }
                if (KeyButtonView.this.isLongClickable()) {
                    KeyButtonView.this.performLongClick();
                } else if (KeyButtonView.this.mSupportsLongpress) {
                    KeyButtonView.this.sendEvent(0, 128);
                    KeyButtonView.this.sendAccessibilityEvent(2);
                }
            }
        };
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.KeyButtonView, defStyle, 0);
        this.mCode = a.getInteger(2, 0);
        this.mSupportsLongpress = a.getBoolean(3, true);
        TypedValue value = new TypedValue();
        if (a.getValue(0, value)) {
            this.mContentDescriptionRes = value.resourceId;
        }
        a.recycle();
        setClickable(true);
        this.mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        this.mAudioManager = (AudioManager) context.getSystemService("audio");
        setBackground(new KeyButtonRipple(context, this));
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (this.mContentDescriptionRes != 0) {
            setContentDescription(this.mContext.getString(this.mContentDescriptionRes));
        }
    }

    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        if (this.mCode != 0) {
            info.addAction(new AccessibilityNodeInfo.AccessibilityAction(16, null));
            if (this.mSupportsLongpress || isLongClickable()) {
                info.addAction(new AccessibilityNodeInfo.AccessibilityAction(32, null));
            }
        }
    }

    /* access modifiers changed from: protected */
    public void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility != 0) {
            jumpDrawablesToCurrentState();
        }
    }

    public boolean performAccessibilityActionInternal(int action, Bundle arguments) {
        if (action == 16 && this.mCode != 0) {
            sendEvent(0, 0, SystemClock.uptimeMillis());
            sendEvent(1, 0);
            sendAccessibilityEvent(1);
            playSoundEffect(0);
            return true;
        } else if (action != 32 || this.mCode == 0) {
            return super.performAccessibilityActionInternal(action, arguments);
        } else {
            sendEvent(0, 128);
            sendEvent(1, 0);
            sendAccessibilityEvent(2);
            return true;
        }
    }

    public boolean onTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        boolean z = false;
        if (action == 0) {
            this.mGestureAborted = false;
        }
        if (this.mGestureAborted) {
            return false;
        }
        switch (action) {
            case 0:
                this.mDownTime = SystemClock.uptimeMillis();
                setPressed(true);
                if (this.mCode != 0) {
                    sendEvent(0, 0, this.mDownTime);
                } else {
                    performHapticFeedback(1);
                }
                removeCallbacks(this.mCheckLongPress);
                postDelayed(this.mCheckLongPress, (long) ViewConfiguration.getLongPressTimeout());
                break;
            case 1:
                boolean doIt = isPressed();
                setPressed(false);
                if (this.mCode != 0) {
                    if (doIt) {
                        sendEvent(1, 0);
                        sendAccessibilityEvent(1);
                        playSoundEffect(0);
                    } else {
                        sendEvent(1, 32);
                    }
                } else if (doIt) {
                    performClick();
                }
                removeCallbacks(this.mCheckLongPress);
                break;
            case 2:
                int x = (int) ev.getX();
                int y = (int) ev.getY();
                if (x >= (-this.mTouchSlop) && x < getWidth() + this.mTouchSlop && y >= (-this.mTouchSlop) && y < getHeight() + this.mTouchSlop) {
                    z = true;
                }
                setPressed(z);
                break;
            case 3:
                setPressed(false);
                if (this.mCode != 0) {
                    sendEvent(1, 32);
                }
                removeCallbacks(this.mCheckLongPress);
                break;
        }
        return true;
    }

    public void playSoundEffect(int soundConstant) {
        this.mAudioManager.playSoundEffect(soundConstant, ActivityManager.getCurrentUser());
    }

    public void sendEvent(int action, int flags) {
        sendEvent(action, flags, SystemClock.uptimeMillis());
    }

    /* access modifiers changed from: package-private */
    public void sendEvent(int action, int flags, long when) {
        int i = flags;
        KeyEvent keyEvent = new KeyEvent(this.mDownTime, when, action, this.mCode, (i & 128) != 0 ? 1 : 0, 0, -1, 0, i | 8 | 64, 257);
        InputManager.getInstance().injectInputEvent(keyEvent, 0);
    }

    public void abortCurrentGesture() {
        setPressed(false);
        this.mGestureAborted = true;
    }

    public void setAlpha(float alpha) {
        super.setAlpha(alpha);
        if (getId() == R.id.back) {
            Log.d("KeyButtonView", "set back alpha:" + alpha);
        }
    }

    public void setPressed(boolean pressed) {
        super.setPressed(pressed);
        if (!isEnabled()) {
            getBackground().jumpToCurrentState();
        }
    }
}
