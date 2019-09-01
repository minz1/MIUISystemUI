package com.android.systemui.pip.phone;

import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import android.view.BatchedInputEventReceiver;
import android.view.Choreographer;
import android.view.IWindowManager;
import android.view.InputChannel;
import android.view.InputEvent;
import android.view.MotionEvent;
import java.io.PrintWriter;

public class InputConsumerController {
    private static final String TAG = InputConsumerController.class.getSimpleName();
    private PipInputEventReceiver mInputEventReceiver;
    /* access modifiers changed from: private */
    public TouchListener mListener;
    private RegistrationListener mRegistrationListener;
    private IWindowManager mWindowManager;

    private final class PipInputEventReceiver extends BatchedInputEventReceiver {
        public PipInputEventReceiver(InputChannel inputChannel, Looper looper) {
            super(inputChannel, looper, Choreographer.getSfInstance());
        }

        public void onInputEvent(InputEvent event, int displayId) {
            boolean handled = true;
            try {
                if (InputConsumerController.this.mListener != null && (event instanceof MotionEvent)) {
                    handled = InputConsumerController.this.mListener.onTouchEvent((MotionEvent) event);
                }
            } finally {
                finishInputEvent(event, handled);
            }
        }
    }

    public interface RegistrationListener {
        void onRegistrationChanged(boolean z);
    }

    public interface TouchListener {
        boolean onTouchEvent(MotionEvent motionEvent);
    }

    public InputConsumerController(IWindowManager windowManager) {
        this.mWindowManager = windowManager;
        registerInputConsumer();
    }

    public void setTouchListener(TouchListener listener) {
        this.mListener = listener;
    }

    public void setRegistrationListener(RegistrationListener listener) {
        this.mRegistrationListener = listener;
        if (this.mRegistrationListener != null) {
            this.mRegistrationListener.onRegistrationChanged(this.mInputEventReceiver != null);
        }
    }

    public boolean isRegistered() {
        return this.mInputEventReceiver != null;
    }

    public void registerInputConsumer() {
        if (this.mInputEventReceiver == null) {
            InputChannel inputChannel = new InputChannel();
            try {
                this.mWindowManager.destroyInputConsumer("pip_input_consumer");
                InputConsumerControllerHelper.createInputConsumer(this.mWindowManager, null, "pip_input_consumer", inputChannel);
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to create PIP input consumer", e);
            }
            this.mInputEventReceiver = new PipInputEventReceiver(inputChannel, Looper.myLooper());
            if (this.mRegistrationListener != null) {
                this.mRegistrationListener.onRegistrationChanged(true);
            }
        }
    }

    public void unregisterInputConsumer() {
        if (this.mInputEventReceiver != null) {
            try {
                this.mWindowManager.destroyInputConsumer("pip_input_consumer");
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to destroy PIP input consumer", e);
            }
            this.mInputEventReceiver.dispose();
            this.mInputEventReceiver = null;
            if (this.mRegistrationListener != null) {
                this.mRegistrationListener.onRegistrationChanged(false);
            }
        }
    }

    public void dump(PrintWriter pw, String prefix) {
        pw.println(prefix + TAG);
        StringBuilder sb = new StringBuilder();
        sb.append(prefix + "  ");
        sb.append("registered=");
        sb.append(this.mInputEventReceiver != null);
        pw.println(sb.toString());
    }
}
