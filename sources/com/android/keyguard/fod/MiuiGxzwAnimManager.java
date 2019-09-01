package com.android.keyguard.fod;

import android.content.Context;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Build;
import android.os.Handler;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import com.android.keyguard.fod.MiuiGxzwFrameAnimation;
import com.android.systemui.R;

public class MiuiGxzwAnimManager {
    private static final String HW_VERSION = SystemProperties.get("ro.boot.hwversion", "null");
    /* access modifiers changed from: private */
    public static final boolean IS_SPECIAL_CEPHEUS;
    private static final String[] SPECIAL_CEPHEUS_VERSIONS = {"1.12.2", "1.2.2", "1.9.2", "1.19.2"};
    private static final int[] ZERO_RES_ARRAY = new int[0];
    private ContentObserver mContentObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            int animType = 0;
            if (MiuiGxzwAnimManager.IS_SPECIAL_CEPHEUS) {
                animType = 3;
            }
            int unused = MiuiGxzwAnimManager.this.mGxzwAnimType = Settings.System.getIntForUser(MiuiGxzwAnimManager.this.mContext.getContentResolver(), "fod_animation_type", animType, 0);
        }
    };
    /* access modifiers changed from: private */
    public Context mContext;
    private boolean mDozing = false;
    private boolean mEnrolling;
    /* access modifiers changed from: private */
    public int mGxzwAnimType = 1;
    private boolean mKeyguardAuthen;
    private boolean mLightIcon = false;
    private boolean mLightWallpaperGxzw;
    private MiuiGxzwFrameAnimation mMiuiGxzwFrameAnimation;

    private static class AlphaDismissDraw implements MiuiGxzwFrameAnimation.CustomerDrawBitmap {
        private int count;
        private Interpolator interpolator;
        private final Paint paint;

        private AlphaDismissDraw() {
            this.paint = new Paint();
            this.interpolator = new LinearInterpolator();
            this.count = 0;
        }

        public void drawBitmap(Canvas canvas, Bitmap bitmap, Matrix matrix) {
            this.count++;
            float radio = this.interpolator.getInterpolation((((float) this.count) * 1.0f) / 10.0f);
            if (radio < 0.0f) {
                radio = 0.0f;
            }
            if (radio > 1.0f) {
                radio = 1.0f;
            }
            this.paint.setAlpha((int) (255.0f * (1.0f - radio)));
            canvas.drawBitmap(bitmap, matrix, this.paint);
        }
    }

    public static class MiuiGxzwAnimArgs {
        final int backgroundFrame;
        final int backgroundRes;
        final MiuiGxzwFrameAnimation.CustomerDrawBitmap customerDrawBitmap;
        final int frameInterval;
        final boolean repeat;
        final int[] res;
        final int startPosition;

        private static class Builder {
            private int backgroundFrame;
            private int backgroundRes;
            private MiuiGxzwFrameAnimation.CustomerDrawBitmap customerDrawBitmap;
            private int frameInterval;
            private boolean repeat;
            private int[] res;
            private int startPosition;

            private Builder(int[] res2) {
                this.repeat = false;
                this.frameInterval = 30;
                this.backgroundRes = 0;
                this.backgroundFrame = 0;
                this.res = res2;
            }

            /* access modifiers changed from: private */
            public Builder setRepeat(boolean repeat2) {
                this.repeat = repeat2;
                return this;
            }

            /* access modifiers changed from: private */
            public Builder setBackgroundRes(int backgroundRes2) {
                this.backgroundRes = backgroundRes2;
                return this;
            }

            /* access modifiers changed from: private */
            public Builder setBackgroundFrame(int backgroundFrame2) {
                this.backgroundFrame = backgroundFrame2;
                return this;
            }

            /* access modifiers changed from: private */
            public Builder setCustomerDrawBitmap(MiuiGxzwFrameAnimation.CustomerDrawBitmap customerDrawBitmap2) {
                this.customerDrawBitmap = customerDrawBitmap2;
                return this;
            }

            /* access modifiers changed from: private */
            public MiuiGxzwAnimArgs build() {
                MiuiGxzwAnimArgs miuiGxzwAnimArgs = new MiuiGxzwAnimArgs(this.res, this.startPosition, this.repeat, this.frameInterval, this.backgroundRes, this.backgroundFrame, this.customerDrawBitmap);
                return miuiGxzwAnimArgs;
            }
        }

        private MiuiGxzwAnimArgs(int[] res2, int startPosition2, boolean repeat2, int frameInterval2, int backgroundRes2, int backgroundFrame2, MiuiGxzwFrameAnimation.CustomerDrawBitmap customerDrawBitmap2) {
            this.res = res2;
            this.startPosition = startPosition2;
            this.repeat = repeat2;
            this.frameInterval = frameInterval2;
            this.backgroundRes = backgroundRes2;
            this.backgroundFrame = backgroundFrame2;
            this.customerDrawBitmap = customerDrawBitmap2;
        }
    }

    static {
        boolean result = false;
        if ("cepheus".equals(Build.DEVICE) && SPECIAL_CEPHEUS_VERSIONS != null) {
            String[] strArr = SPECIAL_CEPHEUS_VERSIONS;
            int length = strArr.length;
            int i = 0;
            while (true) {
                if (i >= length) {
                    break;
                }
                if (HW_VERSION.equals(strArr[i])) {
                    result = true;
                    break;
                }
                i++;
            }
        }
        IS_SPECIAL_CEPHEUS = result;
    }

    public MiuiGxzwAnimManager(Context context, MiuiGxzwFrameAnimation miuiGxzwFrameAnimation) {
        this.mContext = context;
        this.mMiuiGxzwFrameAnimation = miuiGxzwFrameAnimation;
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("fod_animation_type"), false, this.mContentObserver, 0);
        this.mContentObserver.onChange(false);
    }

    public MiuiGxzwAnimArgs getIconAnimArgs(boolean aod) {
        return new MiuiGxzwAnimArgs.Builder(getIconAnimResources(aod)).build();
    }

    public MiuiGxzwAnimArgs getRecognizingAnimArgs(boolean aod) {
        MiuiGxzwAnimArgs.Builder builder = new MiuiGxzwAnimArgs.Builder(getRecognizingAnimResources(aod));
        if (this.mGxzwAnimType != 1) {
            MiuiGxzwAnimArgs.Builder unused = builder.setRepeat(true);
        }
        MiuiGxzwAnimArgs.Builder unused2 = builder.setBackgroundRes(getFingerIconResource(this.mDozing));
        MiuiGxzwAnimArgs.Builder unused3 = builder.setBackgroundFrame(6);
        return builder.build();
    }

    public MiuiGxzwAnimArgs getFalseAnimArgs(boolean aod) {
        return new MiuiGxzwAnimArgs.Builder(getFalseAnimResources(aod)).build();
    }

    public MiuiGxzwAnimArgs getBackAnimArgs(boolean aod) {
        MiuiGxzwAnimArgs.Builder builder = new MiuiGxzwAnimArgs.Builder(getBackAnimResources(aod));
        if (this.mGxzwAnimType != 1) {
            MiuiGxzwAnimArgs.Builder unused = builder.setBackgroundRes(getFingerIconResource(this.mDozing));
            MiuiGxzwAnimArgs.Builder unused2 = builder.setCustomerDrawBitmap(new AlphaDismissDraw());
        }
        return builder.build();
    }

    public int getFingerIconResource(boolean aod) {
        Log.i("MiuiGxzwAnimManager", "getFingerIconResource: mKeyguardAuthen = " + this.mKeyguardAuthen + ", mLightWallpaperGxzw = " + this.mLightWallpaperGxzw + ", mEnrolling = " + this.mEnrolling + ", mLightIcon = " + this.mLightIcon);
        boolean z = this.mKeyguardAuthen;
        int i = R.drawable.finger_image_normal;
        if (z) {
            if (aod) {
                return R.drawable.finger_image_aod;
            }
            if (isLightResource()) {
                return R.drawable.finger_image_light;
            }
            return R.drawable.finger_image_normal;
        } else if (this.mEnrolling) {
            return R.drawable.finger_image_normal;
        } else {
            if (!this.mLightIcon) {
                i = R.drawable.finger_image_grey;
            }
            return i;
        }
    }

    private boolean isLightResource() {
        return this.mLightWallpaperGxzw && !MiuiGxzwManager.getInstance().isBouncer();
    }

    public int getFalseTipTranslationY() {
        switch (this.mGxzwAnimType) {
            case 0:
                return 100;
            case 1:
                return 80;
            case 2:
                return 100;
            case 3:
                return 20;
            default:
                return 0;
        }
    }

    public void startDozing() {
        this.mDozing = true;
    }

    public void stopDozing() {
        this.mDozing = false;
    }

    public void onKeyguardAuthen(boolean keyguardAuthen) {
        this.mKeyguardAuthen = keyguardAuthen;
    }

    public void setEnrolling(boolean enrolling) {
        this.mEnrolling = enrolling;
    }

    public void setLightWallpaperGxzw(boolean lightWallpaperGxzw) {
        this.mLightWallpaperGxzw = lightWallpaperGxzw;
    }

    public void setLightIcon(boolean lightIcon) {
        this.mLightIcon = lightIcon;
    }

    private int[] getIconAnimResources(boolean aod) {
        if (aod) {
            return MiuiGxzwUtils.AOD_ICON_ANIM_RES;
        }
        if (!isLightResource() || !this.mKeyguardAuthen) {
            return MiuiGxzwUtils.NORMAL_ICON_ANIM_RES;
        }
        return MiuiGxzwUtils.LIGHT_ICON_ANIM_RES;
    }

    private int[] getRecognizingAnimResources(boolean aod) {
        switch (this.mGxzwAnimType) {
            case 0:
                return MiuiGxzwUtils.POP_RECOGNIZING_ANIM_RES;
            case 1:
                if (aod) {
                    return MiuiGxzwUtils.AOD_RECOGNIZING_ANIM_RES;
                }
                if (!isLightResource() || !this.mKeyguardAuthen) {
                    return MiuiGxzwUtils.NORMAL_RECOGNIZING_ANIM_RES;
                }
                return MiuiGxzwUtils.LIGHT_RECOGNIZING_ANIM_RES;
            case 2:
                return MiuiGxzwUtils.RHYTHM_RECOGNIZING_ANIM_RES;
            case 3:
                if (!IS_SPECIAL_CEPHEUS) {
                    return MiuiGxzwUtils.PULSE_RECOGNIZING_ANIM_RES;
                }
                if (aod) {
                    return MiuiGxzwUtils.PULSE_WHITE_RECOGNIZING_ANIM_RES;
                }
                if (!isLightResource() || !this.mKeyguardAuthen) {
                    return MiuiGxzwUtils.PULSE_WHITE_RECOGNIZING_ANIM_RES;
                }
                return MiuiGxzwUtils.PULSE_RECOGNIZING_ANIM_RES;
            default:
                return ZERO_RES_ARRAY;
        }
    }

    private int[] getFalseAnimResources(boolean aod) {
        if (this.mGxzwAnimType != 1) {
            return ZERO_RES_ARRAY;
        }
        if (aod) {
            return MiuiGxzwUtils.AOD_FALSE_ANIM_RES;
        }
        if (!isLightResource() || !this.mKeyguardAuthen) {
            return MiuiGxzwUtils.NORMAL_FALSE_ANIM_RES;
        }
        return MiuiGxzwUtils.LIGHT_FALSE_ANIM_RES;
    }

    private int[] getBackAnimResources(boolean aod) {
        int[] src;
        switch (this.mGxzwAnimType) {
            case 0:
                src = MiuiGxzwUtils.POP_RECOGNIZING_ANIM_RES;
                break;
            case 1:
                if (aod) {
                    return MiuiGxzwUtils.AOD_BACK_ANIM_RES;
                }
                if (!isLightResource() || !this.mKeyguardAuthen) {
                    return MiuiGxzwUtils.NORMAL_BACK_ANIM_RES;
                }
                return MiuiGxzwUtils.LIGHT_BACK_ANIM_RES;
            case 2:
                src = MiuiGxzwUtils.RHYTHM_RECOGNIZING_ANIM_RES;
                break;
            case 3:
                if (!IS_SPECIAL_CEPHEUS) {
                    src = MiuiGxzwUtils.PULSE_RECOGNIZING_ANIM_RES;
                    break;
                } else if (aod) {
                    return MiuiGxzwUtils.PULSE_WHITE_RECOGNIZING_ANIM_RES;
                } else {
                    if (!isLightResource() || !this.mKeyguardAuthen) {
                        return MiuiGxzwUtils.PULSE_WHITE_RECOGNIZING_ANIM_RES;
                    }
                    return MiuiGxzwUtils.PULSE_RECOGNIZING_ANIM_RES;
                }
            default:
                if (aod) {
                    return MiuiGxzwUtils.DEFAULT_AOD_BACK_ANIM_RES;
                }
                if (!isLightResource() || !this.mKeyguardAuthen) {
                    return MiuiGxzwUtils.DEFAULT_NORMAL_BACK_ANIM_RES;
                }
                return MiuiGxzwUtils.DEFAULT_LIGHT_BACK_ANIM_RES;
        }
        int position = (this.mMiuiGxzwFrameAnimation.getCurrentPosition() + 1) % src.length;
        int[] dst = new int[10];
        for (int i = 0; i < 10; i++) {
            dst[i] = src[(position + i) % src.length];
        }
        return dst;
    }
}
