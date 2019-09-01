package com.android.keyguard.fod;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import com.android.keyguard.KeyguardCompatibilityHelperForO;
import com.android.keyguard.MiuiKeyguardUtils;
import com.android.keyguard.fod.MiuiGxzwIconView;
import com.android.systemui.R;
import com.android.systemui.util.FixedFileObserver;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MiuiGxzwOverlayView extends FrameLayout implements MiuiGxzwIconView.CollectGxzwListener {
    private static final double[] CEPHEUS_LOW_BRIGHTNESS_ALPHA = {0.9271d, 0.9235d, 0.9201d, 0.92d, 0.92005d, 0.9169d};
    private boolean mAdded = false;
    private BrightnessFileObserver mBrightnessFileObserver;
    private String mBrightnessFilePath = getBrightnessFile();
    private boolean mCollecting = false;
    /* access modifiers changed from: private */
    public volatile boolean mDozing = false;
    private boolean mEnrolling;
    /* access modifiers changed from: private */
    public final Executor mExecutor = Executors.newSingleThreadExecutor();
    private View mHbmOverlay;
    private volatile boolean mInvertColors = false;
    /* access modifiers changed from: private */
    public boolean mKeyguardAuthen;
    private WindowManager.LayoutParams mLayoutParams;
    private int mMaxBrightness = -1;
    /* access modifiers changed from: private */
    public float mOverlayAlpha = 0.5f;
    private float mPreAlpha = 0.5f;
    private boolean mScreenEffectNone = false;
    private volatile boolean mShowed = false;
    private WindowManager mWindowManager;

    private class BrightnessFileObserver extends FixedFileObserver {
        public BrightnessFileObserver(String filePath) {
            super(filePath, 2);
        }

        public void onEvent(int event, String path) {
            Log.i("MiuiGxzwOverlayView", "onEvent: event = " + event);
            if (event == 2) {
                new AsyncTask<Void, Void, Float>() {
                    /* access modifiers changed from: protected */
                    public Float doInBackground(Void... voids) {
                        return Float.valueOf(MiuiGxzwOverlayView.this.caculateOverlayAlpha());
                    }

                    /* access modifiers changed from: protected */
                    public void onPostExecute(Float alpha) {
                        float unused = MiuiGxzwOverlayView.this.mOverlayAlpha = alpha.floatValue();
                        MiuiGxzwOverlayView.this.updateAlpha(alpha.floatValue());
                    }
                }.executeOnExecutor(MiuiGxzwOverlayView.this.mExecutor, new Void[0]);
            }
        }
    }

    public MiuiGxzwOverlayView(Context context) {
        super(context);
        initView();
    }

    private void initView() {
        LayoutInflater.from(getContext()).inflate(R.layout.miui_keyguard_gxzw_overlay, this);
        this.mHbmOverlay = findViewById(R.id.hbm_overlay);
        setSystemUiVisibility(4864);
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(-1, -1, 2021, 83957016, -2);
        this.mLayoutParams = layoutParams;
        if (Build.VERSION.SDK_INT >= 28) {
            this.mLayoutParams.extraFlags |= 8388608;
        } else {
            KeyguardCompatibilityHelperForO.setRoundedCornersOverlayFlag(this.mLayoutParams);
        }
        this.mLayoutParams.screenOrientation = 5;
        this.mLayoutParams.privateFlags |= MiuiGxzwUtils.PRIVATE_FLAG_IS_HBM_OVERLAY;
        this.mLayoutParams.alpha = 0.0f;
        this.mLayoutParams.setTitle("hbm_overlay");
        this.mWindowManager = (WindowManager) getContext().getSystemService("window");
        this.mBrightnessFileObserver = new BrightnessFileObserver(this.mBrightnessFilePath);
    }

    public void show() {
        if (!this.mShowed) {
            Log.d("MiuiGxzwOverlayView", "show");
            this.mInvertColors = MiuiKeyguardUtils.isInvertColorsEnable(getContext());
            this.mHbmOverlay.setBackgroundColor(this.mInvertColors ? -1 : -16777216);
            this.mShowed = true;
            addViewAndUpdateAlpha();
            if (this.mKeyguardAuthen && !this.mScreenEffectNone) {
                KeyguardCompatibilityHelperForO.setScreenEffect(17, 1);
                this.mScreenEffectNone = true;
            }
        }
    }

    public void dismiss() {
        if (this.mShowed) {
            Log.d("MiuiGxzwOverlayView", "dismiss");
            this.mShowed = false;
            if (this.mScreenEffectNone) {
                KeyguardCompatibilityHelperForO.setScreenEffect(17, isAttachedToWindow() ? 0 : 2);
                this.mScreenEffectNone = false;
            }
            removeOverlayView();
            updateBrightnessFileWatchState();
        }
    }

    public void startDozing() {
        Log.d("MiuiGxzwOverlayView", "startDozing");
        this.mDozing = true;
        this.mOverlayAlpha = 0.657f;
        updateAlpha(this.mOverlayAlpha);
        if (((DisplayManager) getContext().getSystemService("display")).getDisplay(0).getState() != 2) {
            addOverlayView();
        }
    }

    public void stopDozing() {
        Log.d("MiuiGxzwOverlayView", "stopDozing");
        this.mDozing = false;
        removeOverlayView();
    }

    public void onScreenTurnedOn() {
        Log.d("MiuiGxzwOverlayView", "onScreenTurnedOn");
        updateBrightnessFileWatchState();
    }

    public void onStartedGoingToSleep() {
        Log.d("MiuiGxzwOverlayView", "onStartedGoingToSleep");
        removeOverlayView();
    }

    public void onKeyguardAuthen(boolean keyguardAuthen) {
        this.mKeyguardAuthen = keyguardAuthen;
    }

    public void setEnrolling(boolean enrolling) {
        this.mEnrolling = enrolling;
    }

    public void onCollectStateChange(boolean collecting) {
        this.mCollecting = collecting;
        if (collecting) {
            addOverlayView();
        }
        if (this.mDozing) {
            if (collecting) {
                updateAlpha(this.mOverlayAlpha);
            } else {
                updateAlpha(0.0f);
            }
        }
    }

    public void onIconStateChange(boolean transparent) {
    }

    /* access modifiers changed from: private */
    public void addOverlayView() {
        if (this.mShowed && !isAttachedToWindow() && getParent() == null) {
            this.mLayoutParams.alpha = this.mOverlayAlpha;
            if (this.mDozing) {
                MiuiGxzwManager.getInstance().requestDrawWackLock(500);
            }
            if (this.mDozing && !this.mCollecting) {
                this.mLayoutParams.alpha = 0.0f;
            }
            if (MiuiGxzwUtils.supportLowBrightnessFod()) {
                this.mLayoutParams.setTitle(this.mEnrolling ? "enroll_overlay" : "hbm_overlay");
            }
            Log.i("MiuiGxzwOverlayView", "add overlay view: mLayoutParams.alpha = " + this.mLayoutParams.alpha);
            this.mWindowManager.addView(this, this.mLayoutParams);
        }
        if (this.mShowed) {
            this.mAdded = true;
        }
    }

    private void removeOverlayView() {
        if (isAttachedToWindow()) {
            Log.i("MiuiGxzwOverlayView", "remove overlay view");
            if (this.mDozing) {
                MiuiGxzwManager.getInstance().requestDrawWackLock(50);
            }
            this.mWindowManager.removeViewImmediate(this);
        }
        this.mAdded = false;
    }

    private void addViewAndUpdateAlpha() {
        new AsyncTask<Void, Void, Float>() {
            /* access modifiers changed from: protected */
            public Float doInBackground(Void... voids) {
                return Float.valueOf(MiuiGxzwOverlayView.this.caculateOverlayAlpha());
            }

            /* access modifiers changed from: protected */
            public void onPostExecute(Float alpha) {
                float unused = MiuiGxzwOverlayView.this.mOverlayAlpha = alpha.floatValue();
                if (!MiuiGxzwOverlayView.this.mKeyguardAuthen || MiuiGxzwOverlayView.this.mDozing) {
                    MiuiGxzwOverlayView.this.addOverlayView();
                }
                MiuiGxzwOverlayView.this.updateBrightnessFileWatchState();
            }
        }.executeOnExecutor(this.mExecutor, new Void[0]);
    }

    /* access modifiers changed from: private */
    public void updateAlpha(float alpha) {
        if (this.mShowed && isAttachedToWindow()) {
            if (this.mDozing && !this.mCollecting) {
                alpha = 0.0f;
            }
            this.mLayoutParams.alpha = alpha;
            Log.i("MiuiGxzwOverlayView", "upldate overlay view alpha: " + alpha);
            this.mWindowManager.updateViewLayout(this, this.mLayoutParams);
        }
    }

    private String getBrightnessFile() {
        String[] filesArray = getResources().getStringArray(285605914);
        for (int i = 0; i < filesArray.length; i++) {
            if (new File(filesArray[i]).exists()) {
                return filesArray[i];
            }
        }
        return "/sys/class/leds/lcd-backlight/brightness";
    }

    /* access modifiers changed from: private */
    public float caculateOverlayAlpha() {
        float alpha;
        float alpha2;
        if (this.mMaxBrightness <= 0) {
            this.mMaxBrightness = readMaxBrightnessFromFile();
        }
        if (this.mInvertColors) {
            return brighnessToAlpha(converBrighnessFrom1024(8));
        }
        int fileBrightness = readBrightnessFromFile();
        if (fileBrightness > 0 && this.mMaxBrightness > 0) {
            Log.i("MiuiGxzwOverlayView", "read brightness from file: " + fileBrightness + ", mMaxBrightness = " + this.mMaxBrightness);
            int fileBrightness2 = Math.min(fileBrightness, this.mMaxBrightness);
            if (this.mDozing) {
                if (fileBrightness2 > converBrighnessFrom1024(5)) {
                    alpha2 = 0.657f;
                } else if (fileBrightness2 > 0) {
                    alpha2 = 0.89f;
                } else {
                    alpha2 = brighnessToAlpha(fileBrightness2);
                }
                alpha = alpha2;
            } else {
                alpha = brighnessToAlpha(fileBrightness2);
            }
            this.mPreAlpha = alpha;
        } else if (this.mDozing) {
            alpha = 0.657f;
            this.mPreAlpha = 0.657f;
        } else {
            alpha = this.mPreAlpha;
        }
        Log.i("MiuiGxzwOverlayView", "caculate overlay alpha: " + alpha);
        return alpha;
    }

    private float brighnessToAlpha(int brighness) {
        double alpha;
        if (brighness == 0) {
            alpha = 0.9619584887d;
        } else if (brighness >= 2 && brighness <= 8 && ("equuleus".equals(Build.DEVICE) || "ursa".equals(Build.DEVICE))) {
            alpha = 1.0d - ((((double) brighness) * 0.0032d) + 0.0739d);
        } else if (brighness >= 5 && brighness <= 10 && "cepheus".equals(Build.DEVICE)) {
            alpha = CEPHEUS_LOW_BRIGHTNESS_ALPHA[brighness - 5];
        } else if (!MiuiGxzwUtils.supportLowBrightnessFod()) {
            alpha = 1.0d - Math.pow((((((double) brighness) * 1.0d) / ((double) this.mMaxBrightness)) * 430.0d) / 600.0d, 0.45d);
        } else if (brighness > 500) {
            alpha = 1.0d - Math.pow((((((double) brighness) * 1.0d) / 2047.0d) * 430.0d) / 600.0d, 0.455d);
        } else {
            alpha = 1.0d - Math.pow((((double) brighness) * 1.0d) / 1680.0d, 0.455d);
        }
        return (float) alpha;
    }

    private int converBrighnessFrom1024(int brighness) {
        return (int) (((((float) this.mMaxBrightness) + 1.0f) / 1024.0f) * ((float) brighness));
    }

    private int readBrightnessFromFile() {
        return readIntFromFile(this.mBrightnessFilePath);
    }

    private int readMaxBrightnessFromFile() {
        new File(this.mBrightnessFilePath);
        return readIntFromFile(brightnessFile.getParentFile().getAbsolutePath() + "/max_brightness");
    }

    private int readIntFromFile(String path) {
        BufferedReader in = null;
        try {
            BufferedReader in2 = new BufferedReader(new FileReader(path));
            String str = in2.readLine();
            if (str != null) {
                int parseInt = Integer.parseInt(str);
                try {
                    in2.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return parseInt;
            }
            try {
                in2.close();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
            return -1;
        } catch (Exception e3) {
            e3.printStackTrace();
            if (in != null) {
                in.close();
            }
        } catch (Throwable th) {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e4) {
                    e4.printStackTrace();
                }
            }
            throw th;
        }
    }

    /* access modifiers changed from: private */
    public void updateBrightnessFileWatchState() {
        if (!MiuiGxzwUtils.supportLowBrightnessFod()) {
            if (this.mShowed) {
                this.mBrightnessFileObserver.stopWatching();
                this.mBrightnessFileObserver.startWatching();
                this.mBrightnessFileObserver.onEvent(2, this.mBrightnessFilePath);
            } else {
                this.mBrightnessFileObserver.stopWatching();
            }
        }
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateViewAddState();
        if (this.mShowed && this.mAdded && Float.compare(this.mOverlayAlpha, this.mLayoutParams.alpha) != 0) {
            updateAlpha(this.mOverlayAlpha);
        }
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        updateViewAddState();
    }

    private void updateViewAddState() {
        if (this.mAdded && getParent() == null) {
            addOverlayView();
        } else if (!this.mAdded && getParent() != null) {
            removeOverlayView();
        }
    }
}
