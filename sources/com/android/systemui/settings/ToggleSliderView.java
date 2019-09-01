package com.android.systemui.settings;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import com.android.systemui.R;
import com.android.systemui.settings.ToggleSlider;
import com.android.systemui.statusbar.policy.BrightnessMirrorController;

public class ToggleSliderView extends RelativeLayout implements ToggleSlider {
    /* access modifiers changed from: private */
    public boolean mIgnoreTrackingEvent;
    /* access modifiers changed from: private */
    public int mLastTouchAction;
    /* access modifiers changed from: private */
    public ToggleSlider.Listener mListener;
    private ToggleSliderView mMirror;
    /* access modifiers changed from: private */
    public BrightnessMirrorController mMirrorController;
    private final SeekBar.OnSeekBarChangeListener mSeekListener;
    /* access modifiers changed from: private */
    public ToggleSeekBar mSlider;
    /* access modifiers changed from: private */
    public boolean mTracking;

    public ToggleSliderView(Context context) {
        this(context, null);
    }

    public ToggleSliderView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ToggleSliderView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mSeekListener = new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (ToggleSliderView.this.mListener != null) {
                    ToggleSliderView.this.mListener.onChanged(ToggleSliderView.this, ToggleSliderView.this.mTracking, progress, false);
                }
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                if (ToggleSliderView.this.mLastTouchAction == 1) {
                    boolean unused = ToggleSliderView.this.mIgnoreTrackingEvent = true;
                    Log.w("ToggleSliderView", "ignoring onStartTrackingTouch, maybe tap event");
                    return;
                }
                boolean unused2 = ToggleSliderView.this.mTracking = true;
                if (ToggleSliderView.this.mListener != null) {
                    ToggleSliderView.this.mListener.onStart(seekBar.getProgress());
                    ToggleSliderView.this.mListener.onChanged(ToggleSliderView.this, ToggleSliderView.this.mTracking, ToggleSliderView.this.mSlider.getProgress(), false);
                }
                if (ToggleSliderView.this.mMirrorController != null) {
                    ToggleSliderView.this.mMirrorController.showMirror();
                    ToggleSliderView.this.mMirrorController.setLocation(ToggleSliderView.this);
                }
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                if (ToggleSliderView.this.mIgnoreTrackingEvent) {
                    boolean unused = ToggleSliderView.this.mIgnoreTrackingEvent = false;
                    Log.w("ToggleSliderView", "ignoring onStopTrackingTouch, maybe tap event");
                    return;
                }
                boolean unused2 = ToggleSliderView.this.mTracking = false;
                if (ToggleSliderView.this.mListener != null) {
                    ToggleSliderView.this.mListener.onChanged(ToggleSliderView.this, ToggleSliderView.this.mTracking, ToggleSliderView.this.mSlider.getProgress(), true);
                    ToggleSliderView.this.mListener.onStop(seekBar.getProgress());
                }
                if (ToggleSliderView.this.mMirrorController != null) {
                    ToggleSliderView.this.mMirrorController.hideMirror();
                }
            }
        };
        View.inflate(context, R.layout.status_bar_toggle_slider, this);
        this.mSlider = (ToggleSeekBar) findViewById(R.id.slider);
        this.mSlider.setOnSeekBarChangeListener(this.mSeekListener);
        this.mSlider.setAccessibilityLabel(getContentDescription().toString());
    }

    public void setMirror(ToggleSliderView toggleSlider) {
        this.mMirror = toggleSlider;
        if (this.mMirror != null) {
            this.mMirror.setMax(this.mSlider.getMax());
            this.mMirror.setValue(this.mSlider.getProgress());
        }
    }

    public void setMirrorController(BrightnessMirrorController c) {
        this.mMirrorController = c;
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (this.mListener != null) {
            this.mListener.onInit(this);
        }
    }

    public void setOnChangedListener(ToggleSlider.Listener l) {
        this.mListener = l;
    }

    public void setMax(int max) {
        if (max != this.mSlider.getMax()) {
            this.mSlider.setMax(max);
            if (this.mMirror != null) {
                this.mMirror.setMax(max);
            }
        }
    }

    public void setValue(int value) {
        this.mSlider.setProgress(value);
        if (this.mMirror != null) {
            this.mMirror.setValue(value);
        }
    }

    public int getValue() {
        return this.mSlider.getProgress();
    }

    public boolean dispatchTouchEvent(MotionEvent ev) {
        this.mLastTouchAction = ev.getActionMasked();
        if (ev.getActionMasked() == 0) {
            this.mIgnoreTrackingEvent = false;
            if (this.mMirror != null) {
                this.mMirror.setValue(this.mSlider.getProgress());
            }
        }
        if (this.mMirror != null) {
            MotionEvent copy = ev.copy();
            this.mMirror.dispatchTouchEvent(copy);
            copy.recycle();
        }
        return super.dispatchTouchEvent(ev);
    }
}
