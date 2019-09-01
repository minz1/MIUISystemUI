package com.android.systemui.miui.volume;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.util.AttributeSet;
import android.view.Display;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.android.systemui.miui.volume.MiuiVolumeDialogMotion;
import com.android.systemui.miui.volume.widget.ExpandCollapseLinearLayout;

public class MiuiVolumeDialogView extends ExpandCollapseLinearLayout implements DisplayManager.DisplayListener, ViewTreeObserver.OnComputeInternalInsetsListener {
    private boolean mAttached;
    private ViewGroup mDialogContentView;
    private Display mDisplay;
    private int[] mDisplayLocation;
    private ImageView mExpandButton;
    private int mLastRotation;
    private MiuiVolumeDialogMotion mMotion;
    private boolean mObservingInternalInsets;
    private MiuiRingerModeLayout mRingerModeLayout;
    private FrameLayout mTempColumnContainer;

    public MiuiVolumeDialogView(Context context) {
        this(context, null);
    }

    public MiuiVolumeDialogView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MiuiVolumeDialogView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mDisplayLocation = new int[2];
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mExpandButton = (ImageView) findViewById(R.id.volume_expand_button);
        this.mDialogContentView = (ViewGroup) findViewById(R.id.volume_dialog_content);
        this.mRingerModeLayout = (MiuiRingerModeLayout) findViewById(R.id.miui_volume_ringer_layout);
        this.mTempColumnContainer = (FrameLayout) findViewById(R.id.volume_dialog_column_temp);
        MiuiVolumeDialogMotion miuiVolumeDialogMotion = new MiuiVolumeDialogMotion(this, this.mDialogContentView, this.mTempColumnContainer, this.mExpandButton, this.mRingerModeLayout);
        this.mMotion = miuiVolumeDialogMotion;
    }

    public void onExpandStateUpdated(boolean expand) {
        super.onExpandStateUpdated(expand);
        this.mMotion.startExpandH(expand);
        this.mRingerModeLayout.updateExpandedH(expand);
        updateExpandButtonH(expand);
        setInternalInsetsListener();
    }

    private void setInternalInsetsListener() {
        boolean observeInternalInsets = this.mAttached && !isExpanded();
        if (observeInternalInsets != this.mObservingInternalInsets) {
            this.mObservingInternalInsets = observeInternalInsets;
            if (observeInternalInsets) {
                getViewTreeObserver().addOnComputeInternalInsetsListener(this);
                requestLayout();
                return;
            }
            getViewTreeObserver().removeOnComputeInternalInsetsListener(this);
        }
    }

    public void setMotionCallback(MiuiVolumeDialogMotion.Callback callback) {
        this.mMotion.setCallback(callback);
    }

    public void showH() {
        this.mMotion.startShow();
        this.mRingerModeLayout.init();
    }

    public void dismissH(Runnable motionCallback) {
        this.mMotion.startDismiss(motionCallback);
        this.mRingerModeLayout.cleanUp();
    }

    public boolean isAnimating() {
        return this.mMotion.isAnimating();
    }

    public boolean isOffMode() {
        return this.mRingerModeLayout.getRingerMode() == 0;
    }

    public void updateFooterVisibility(boolean visible) {
        Util.setVisOrGone(this.mRingerModeLayout, visible);
    }

    private void updateExpandButtonH(boolean expand) {
        this.mExpandButton.setContentDescription(getContext().getString(expand ? R.string.accessibility_volume_collapse : R.string.accessibility_volume_expand));
    }

    public void setSilenceMode(int mode, boolean doAnimation) {
        this.mRingerModeLayout.setSilenceMode(mode, doAnimation);
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mDisplay = getDisplay();
        this.mMotion.setDisplay(this.mDisplay);
        this.mLastRotation = this.mDisplay.getRotation();
        ((DisplayManager) getContext().getSystemService("display")).registerDisplayListener(this, null);
        this.mAttached = true;
        setInternalInsetsListener();
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mDisplay = null;
        this.mMotion.setDisplay(null);
        ((DisplayManager) getContext().getSystemService("display")).unregisterDisplayListener(this);
        this.mAttached = false;
        setInternalInsetsListener();
    }

    public void onDisplayAdded(int displayId) {
    }

    public void onDisplayRemoved(int displayId) {
    }

    public void onDisplayChanged(int displayId) {
        if (this.mDisplay != null) {
            int rotation = this.mDisplay.getRotation();
            if (this.mLastRotation != rotation) {
                this.mMotion.updateStates();
            }
            this.mLastRotation = rotation;
        }
    }

    public void onComputeInternalInsets(ViewTreeObserver.InternalInsetsInfo info) {
        if (!isExpanded()) {
            info.setTouchableInsets(3);
            if (this.mDisplay.getRotation() == 3) {
                getLocationOnScreen(this.mDisplayLocation);
            } else {
                this.mDisplayLocation[0] = getLeft();
                this.mDisplayLocation[1] = getTop();
            }
            info.touchableRegion.set(this.mDisplayLocation[0], this.mDisplayLocation[1], this.mDisplayLocation[0] + getWidth(), this.mDisplayLocation[1] + getHeight());
        }
    }
}
