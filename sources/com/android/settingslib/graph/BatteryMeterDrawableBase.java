package com.android.settingslib.graph;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import com.android.settingslib.R;
import com.android.settingslib.Utils;

public class BatteryMeterDrawableBase extends Drawable {
    public static final String TAG = BatteryMeterDrawableBase.class.getSimpleName();
    protected final Paint mBatteryPaint;
    private final RectF mBoltFrame = new RectF();
    protected final Paint mBoltPaint;
    private final Path mBoltPath = new Path();
    private final float[] mBoltPoints;
    private final RectF mButtonFrame = new RectF();
    protected float mButtonHeightFraction;
    private int mChargeColor;
    private boolean mCharging;
    private final int[] mColors;
    protected final Context mContext;
    private final int mCriticalLevel;
    private final RectF mFrame = new RectF();
    protected final Paint mFramePaint;
    private int mHeight;
    private int mIconTint = -1;
    private final int mIntrinsicHeight;
    private final int mIntrinsicWidth;
    private int mLevel = -1;
    private float mOldDarkIntensity = -1.0f;
    private final Path mOutlinePath = new Path();
    private final Rect mPadding = new Rect();
    private final RectF mPlusFrame = new RectF();
    protected final Paint mPlusPaint;
    private final Path mPlusPath = new Path();
    private final float[] mPlusPoints;
    protected boolean mPowerSaveAsColorError = true;
    private boolean mPowerSaveEnabled;
    protected final Paint mPowersavePaint;
    private final Path mShapePath = new Path();
    private boolean mShowPercent;
    private float mSubpixelSmoothingLeft;
    private float mSubpixelSmoothingRight;
    private float mTextHeight;
    protected final Paint mTextPaint;
    private final Path mTextPath = new Path();
    private String mWarningString;
    private float mWarningTextHeight;
    protected final Paint mWarningTextPaint;
    private int mWidth;

    public BatteryMeterDrawableBase(Context context, int frameColor) {
        this.mContext = context;
        Resources res = context.getResources();
        TypedArray levels = res.obtainTypedArray(R.array.batterymeter_color_levels);
        TypedArray colors = res.obtainTypedArray(R.array.batterymeter_color_values);
        int N = levels.length();
        this.mColors = new int[(2 * N)];
        for (int i = 0; i < N; i++) {
            this.mColors[2 * i] = levels.getInt(i, 0);
            if (colors.getType(i) == 2) {
                this.mColors[(2 * i) + 1] = Utils.getColorAttr(context, colors.getThemeAttributeId(i, 0));
            } else {
                this.mColors[(2 * i) + 1] = colors.getColor(i, 0);
            }
        }
        levels.recycle();
        colors.recycle();
        this.mWarningString = context.getString(R.string.battery_meter_very_low_overlay_symbol);
        this.mCriticalLevel = this.mContext.getResources().getInteger(17694757);
        this.mButtonHeightFraction = context.getResources().getFraction(R.fraction.battery_button_height_fraction, 1, 1);
        this.mSubpixelSmoothingLeft = context.getResources().getFraction(R.fraction.battery_subpixel_smoothing_left, 1, 1);
        this.mSubpixelSmoothingRight = context.getResources().getFraction(R.fraction.battery_subpixel_smoothing_right, 1, 1);
        this.mFramePaint = new Paint(1);
        this.mFramePaint.setColor(frameColor);
        this.mFramePaint.setDither(true);
        this.mFramePaint.setStrokeWidth(0.0f);
        this.mFramePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        this.mBatteryPaint = new Paint(1);
        this.mBatteryPaint.setDither(true);
        this.mBatteryPaint.setStrokeWidth(0.0f);
        this.mBatteryPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        this.mTextPaint = new Paint(1);
        this.mTextPaint.setTypeface(Typeface.create("sans-serif-condensed", 1));
        this.mTextPaint.setTextAlign(Paint.Align.CENTER);
        this.mWarningTextPaint = new Paint(1);
        this.mWarningTextPaint.setTypeface(Typeface.create("sans-serif", 1));
        this.mWarningTextPaint.setTextAlign(Paint.Align.CENTER);
        if (this.mColors.length > 1) {
            this.mWarningTextPaint.setColor(this.mColors[1]);
        }
        this.mChargeColor = Utils.getDefaultColor(this.mContext, R.color.meter_consumed_color);
        this.mBoltPaint = new Paint(1);
        this.mBoltPaint.setColor(Utils.getDefaultColor(this.mContext, R.color.batterymeter_bolt_color));
        this.mBoltPoints = loadPoints(res, R.array.batterymeter_bolt_points);
        this.mPlusPaint = new Paint(1);
        this.mPlusPaint.setColor(Utils.getDefaultColor(this.mContext, R.color.batterymeter_plus_color));
        this.mPlusPoints = loadPoints(res, R.array.batterymeter_plus_points);
        this.mPowersavePaint = new Paint(1);
        this.mPowersavePaint.setColor(this.mPlusPaint.getColor());
        this.mPowersavePaint.setStyle(Paint.Style.STROKE);
        this.mPowersavePaint.setStrokeWidth((float) context.getResources().getDimensionPixelSize(R.dimen.battery_powersave_outline_thickness));
        this.mIntrinsicWidth = context.getResources().getDimensionPixelSize(R.dimen.battery_width);
        this.mIntrinsicHeight = context.getResources().getDimensionPixelSize(R.dimen.battery_height);
    }

    public int getIntrinsicHeight() {
        return this.mIntrinsicHeight;
    }

    public int getIntrinsicWidth() {
        return this.mIntrinsicWidth;
    }

    public void setBatteryLevel(int val) {
        this.mLevel = val;
        postInvalidate();
    }

    /* access modifiers changed from: protected */
    public void postInvalidate() {
        unscheduleSelf(new Runnable() {
            public final void run() {
                BatteryMeterDrawableBase.this.invalidateSelf();
            }
        });
        scheduleSelf(new Runnable() {
            public final void run() {
                BatteryMeterDrawableBase.this.invalidateSelf();
            }
        }, 0);
    }

    private static float[] loadPoints(Resources res, int pointArrayRes) {
        int[] pts = res.getIntArray(pointArrayRes);
        int maxY = 0;
        int maxX = 0;
        for (int i = 0; i < pts.length; i += 2) {
            maxX = Math.max(maxX, pts[i]);
            maxY = Math.max(maxY, pts[i + 1]);
        }
        float[] ptsF = new float[pts.length];
        for (int i2 = 0; i2 < pts.length; i2 += 2) {
            ptsF[i2] = ((float) pts[i2]) / ((float) maxX);
            ptsF[i2 + 1] = ((float) pts[i2 + 1]) / ((float) maxY);
        }
        return ptsF;
    }

    public void setBounds(int left, int top, int right, int bottom) {
        super.setBounds(left, top, right, bottom);
        updateSize();
    }

    private void updateSize() {
        Rect bounds = getBounds();
        this.mHeight = (bounds.bottom - this.mPadding.bottom) - (bounds.top + this.mPadding.top);
        this.mWidth = (bounds.right - this.mPadding.right) - (bounds.left + this.mPadding.left);
        this.mWarningTextPaint.setTextSize(((float) this.mHeight) * 0.75f);
        this.mWarningTextHeight = -this.mWarningTextPaint.getFontMetrics().ascent;
    }

    public boolean getPadding(Rect padding) {
        if (this.mPadding.left == 0 && this.mPadding.top == 0 && this.mPadding.right == 0 && this.mPadding.bottom == 0) {
            return super.getPadding(padding);
        }
        padding.set(this.mPadding);
        return true;
    }

    public void setPadding(int left, int top, int right, int bottom) {
        this.mPadding.left = left;
        this.mPadding.top = top;
        this.mPadding.right = right;
        this.mPadding.bottom = bottom;
        updateSize();
    }

    private int getColorForLevel(int percent) {
        int color = 0;
        int i = 0;
        while (i < this.mColors.length) {
            int thresh = this.mColors[i];
            color = this.mColors[i + 1];
            if (percent > thresh) {
                i += 2;
            } else if (i == this.mColors.length - 2) {
                return this.mIconTint;
            } else {
                return color;
            }
        }
        return color;
    }

    /* access modifiers changed from: protected */
    public int batteryColorForLevel(int level) {
        if (this.mCharging || (this.mPowerSaveEnabled && this.mPowerSaveAsColorError)) {
            return this.mChargeColor;
        }
        return getColorForLevel(level);
    }

    /* JADX WARNING: Removed duplicated region for block: B:63:0x0387  */
    /* JADX WARNING: Removed duplicated region for block: B:64:0x038b  */
    /* JADX WARNING: Removed duplicated region for block: B:67:0x03bb  */
    /* JADX WARNING: Removed duplicated region for block: B:70:0x03c1  */
    /* JADX WARNING: Removed duplicated region for block: B:77:0x040f  */
    /* JADX WARNING: Removed duplicated region for block: B:78:0x042b  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void draw(android.graphics.Canvas r41) {
        /*
            r40 = this;
            r0 = r40
            r1 = r41
            int r2 = r0.mLevel
            android.graphics.Rect r3 = r40.getBounds()
            r4 = -1
            if (r2 != r4) goto L_0x000e
            return
        L_0x000e:
            float r4 = (float) r2
            r5 = 1120403456(0x42c80000, float:100.0)
            float r4 = r4 / r5
            int r5 = r0.mHeight
            float r6 = r40.getAspectRatio()
            int r7 = r0.mHeight
            float r7 = (float) r7
            float r6 = r6 * r7
            int r6 = (int) r6
            int r7 = r0.mWidth
            int r7 = r7 - r6
            r8 = 2
            int r7 = r7 / r8
            float r9 = (float) r5
            float r10 = r0.mButtonHeightFraction
            float r9 = r9 * r10
            int r9 = java.lang.Math.round(r9)
            android.graphics.Rect r10 = r0.mPadding
            int r10 = r10.left
            int r11 = r3.left
            int r10 = r10 + r11
            int r11 = r3.bottom
            android.graphics.Rect r12 = r0.mPadding
            int r12 = r12.bottom
            int r11 = r11 - r12
            int r11 = r11 - r5
            android.graphics.RectF r12 = r0.mFrame
            float r13 = (float) r10
            float r14 = (float) r11
            int r15 = r6 + r10
            float r15 = (float) r15
            int r8 = r5 + r11
            float r8 = (float) r8
            r12.set(r13, r14, r15, r8)
            android.graphics.RectF r8 = r0.mFrame
            float r12 = (float) r7
            r13 = 0
            r8.offset(r12, r13)
            android.graphics.RectF r8 = r0.mButtonFrame
            android.graphics.RectF r12 = r0.mFrame
            float r12 = r12.left
            float r14 = (float) r6
            r15 = 1049582633(0x3e8f5c29, float:0.28)
            float r14 = r14 * r15
            int r14 = java.lang.Math.round(r14)
            float r14 = (float) r14
            float r12 = r12 + r14
            android.graphics.RectF r14 = r0.mFrame
            float r14 = r14.top
            android.graphics.RectF r13 = r0.mFrame
            float r13 = r13.right
            r18 = r3
            float r3 = (float) r6
            float r3 = r3 * r15
            int r3 = java.lang.Math.round(r3)
            float r3 = (float) r3
            float r13 = r13 - r3
            android.graphics.RectF r3 = r0.mFrame
            float r3 = r3.top
            float r15 = (float) r9
            float r3 = r3 + r15
            r8.set(r12, r14, r13, r3)
            android.graphics.RectF r3 = r0.mFrame
            float r8 = r3.top
            float r12 = (float) r9
            float r8 = r8 + r12
            r3.top = r8
            android.graphics.Paint r3 = r0.mBatteryPaint
            int r8 = r0.batteryColorForLevel(r2)
            r3.setColor(r8)
            r3 = 96
            if (r2 < r3) goto L_0x0091
            r4 = 1065353216(0x3f800000, float:1.0)
            goto L_0x0096
        L_0x0091:
            int r3 = r0.mCriticalLevel
            if (r2 > r3) goto L_0x0096
            r4 = 0
        L_0x0096:
            r3 = 1065353216(0x3f800000, float:1.0)
            int r8 = (r4 > r3 ? 1 : (r4 == r3 ? 0 : -1))
            if (r8 != 0) goto L_0x00a1
            android.graphics.RectF r8 = r0.mButtonFrame
            float r8 = r8.top
            goto L_0x00af
        L_0x00a1:
            android.graphics.RectF r8 = r0.mFrame
            float r8 = r8.top
            android.graphics.RectF r12 = r0.mFrame
            float r12 = r12.height()
            float r13 = r3 - r4
            float r12 = r12 * r13
            float r8 = r8 + r12
        L_0x00af:
            android.graphics.Path r12 = r0.mShapePath
            r12.reset()
            android.graphics.Path r12 = r0.mOutlinePath
            r12.reset()
            float r12 = r40.getRadiusRatio()
            android.graphics.RectF r13 = r0.mFrame
            float r13 = r13.height()
            float r14 = (float) r9
            float r13 = r13 + r14
            float r12 = r12 * r13
            android.graphics.Path r13 = r0.mShapePath
            android.graphics.Path$FillType r14 = android.graphics.Path.FillType.WINDING
            r13.setFillType(r14)
            android.graphics.Path r13 = r0.mShapePath
            android.graphics.RectF r14 = r0.mFrame
            android.graphics.Path$Direction r15 = android.graphics.Path.Direction.CW
            r13.addRoundRect(r14, r12, r12, r15)
            android.graphics.Path r13 = r0.mShapePath
            android.graphics.RectF r14 = r0.mButtonFrame
            android.graphics.Path$Direction r15 = android.graphics.Path.Direction.CW
            r13.addRect(r14, r15)
            android.graphics.Path r13 = r0.mOutlinePath
            android.graphics.RectF r14 = r0.mFrame
            android.graphics.Path$Direction r15 = android.graphics.Path.Direction.CW
            r13.addRoundRect(r14, r12, r12, r15)
            android.graphics.Path r13 = new android.graphics.Path
            r13.<init>()
            android.graphics.RectF r14 = r0.mButtonFrame
            android.graphics.Path$Direction r15 = android.graphics.Path.Direction.CW
            r13.addRect(r14, r15)
            android.graphics.Path r14 = r0.mOutlinePath
            android.graphics.Path$Op r15 = android.graphics.Path.Op.XOR
            r14.op(r13, r15)
            boolean r14 = r0.mCharging
            r19 = 1
            if (r14 == 0) goto L_0x023c
            android.graphics.RectF r14 = r0.mFrame
            float r14 = r14.left
            android.graphics.RectF r15 = r0.mFrame
            float r15 = r15.width()
            r21 = 1082130432(0x40800000, float:4.0)
            float r15 = r15 / r21
            float r14 = r14 + r15
            float r14 = r14 + r3
            android.graphics.RectF r15 = r0.mFrame
            float r15 = r15.top
            android.graphics.RectF r3 = r0.mFrame
            float r3 = r3.height()
            r22 = 1086324736(0x40c00000, float:6.0)
            float r3 = r3 / r22
            float r15 = r15 + r3
            android.graphics.RectF r3 = r0.mFrame
            float r3 = r3.right
            r23 = r4
            android.graphics.RectF r4 = r0.mFrame
            float r4 = r4.width()
            float r4 = r4 / r21
            float r3 = r3 - r4
            r4 = 1065353216(0x3f800000, float:1.0)
            float r3 = r3 + r4
            android.graphics.RectF r4 = r0.mFrame
            float r4 = r4.bottom
            r24 = r6
            android.graphics.RectF r6 = r0.mFrame
            float r6 = r6.height()
            r21 = 1092616192(0x41200000, float:10.0)
            float r6 = r6 / r21
            float r4 = r4 - r6
            android.graphics.RectF r6 = r0.mBoltFrame
            float r6 = r6.left
            int r6 = (r6 > r14 ? 1 : (r6 == r14 ? 0 : -1))
            if (r6 != 0) goto L_0x0170
            android.graphics.RectF r6 = r0.mBoltFrame
            float r6 = r6.top
            int r6 = (r6 > r15 ? 1 : (r6 == r15 ? 0 : -1))
            if (r6 != 0) goto L_0x0170
            android.graphics.RectF r6 = r0.mBoltFrame
            float r6 = r6.right
            int r6 = (r6 > r3 ? 1 : (r6 == r3 ? 0 : -1))
            if (r6 != 0) goto L_0x0170
            android.graphics.RectF r6 = r0.mBoltFrame
            float r6 = r6.bottom
            int r6 = (r6 > r4 ? 1 : (r6 == r4 ? 0 : -1))
            if (r6 == 0) goto L_0x0164
            goto L_0x0170
        L_0x0164:
            r25 = r3
            r26 = r4
            r27 = r7
            r28 = r9
            r29 = r12
            goto L_0x0208
        L_0x0170:
            android.graphics.RectF r6 = r0.mBoltFrame
            r6.set(r14, r15, r3, r4)
            android.graphics.Path r6 = r0.mBoltPath
            r6.reset()
            android.graphics.Path r6 = r0.mBoltPath
            r25 = r3
            android.graphics.RectF r3 = r0.mBoltFrame
            float r3 = r3.left
            r26 = r4
            float[] r4 = r0.mBoltPoints
            r20 = 0
            r4 = r4[r20]
            r27 = r7
            android.graphics.RectF r7 = r0.mBoltFrame
            float r7 = r7.width()
            float r4 = r4 * r7
            float r3 = r3 + r4
            android.graphics.RectF r4 = r0.mBoltFrame
            float r4 = r4.top
            float[] r7 = r0.mBoltPoints
            r7 = r7[r19]
            r28 = r9
            android.graphics.RectF r9 = r0.mBoltFrame
            float r9 = r9.height()
            float r7 = r7 * r9
            float r4 = r4 + r7
            r6.moveTo(r3, r4)
            r16 = 2
        L_0x01ab:
            r3 = r16
            float[] r4 = r0.mBoltPoints
            int r4 = r4.length
            if (r3 >= r4) goto L_0x01e0
            android.graphics.Path r4 = r0.mBoltPath
            android.graphics.RectF r6 = r0.mBoltFrame
            float r6 = r6.left
            float[] r7 = r0.mBoltPoints
            r7 = r7[r3]
            android.graphics.RectF r9 = r0.mBoltFrame
            float r9 = r9.width()
            float r7 = r7 * r9
            float r6 = r6 + r7
            android.graphics.RectF r7 = r0.mBoltFrame
            float r7 = r7.top
            float[] r9 = r0.mBoltPoints
            int r16 = r3 + 1
            r9 = r9[r16]
            r29 = r12
            android.graphics.RectF r12 = r0.mBoltFrame
            float r12 = r12.height()
            float r9 = r9 * r12
            float r7 = r7 + r9
            r4.lineTo(r6, r7)
            int r16 = r3 + 2
            r12 = r29
            goto L_0x01ab
        L_0x01e0:
            r29 = r12
            android.graphics.Path r3 = r0.mBoltPath
            android.graphics.RectF r4 = r0.mBoltFrame
            float r4 = r4.left
            float[] r6 = r0.mBoltPoints
            r7 = 0
            r6 = r6[r7]
            android.graphics.RectF r7 = r0.mBoltFrame
            float r7 = r7.width()
            float r6 = r6 * r7
            float r4 = r4 + r6
            android.graphics.RectF r6 = r0.mBoltFrame
            float r6 = r6.top
            float[] r7 = r0.mBoltPoints
            r7 = r7[r19]
            android.graphics.RectF r9 = r0.mBoltFrame
            float r9 = r9.height()
            float r7 = r7 * r9
            float r6 = r6 + r7
            r3.lineTo(r4, r6)
        L_0x0208:
            android.graphics.RectF r3 = r0.mBoltFrame
            float r3 = r3.bottom
            float r3 = r3 - r8
            android.graphics.RectF r4 = r0.mBoltFrame
            float r4 = r4.bottom
            android.graphics.RectF r6 = r0.mBoltFrame
            float r6 = r6.top
            float r4 = r4 - r6
            float r3 = r3 / r4
            r4 = 0
            float r4 = java.lang.Math.max(r3, r4)
            r6 = 1065353216(0x3f800000, float:1.0)
            float r3 = java.lang.Math.min(r4, r6)
            r4 = 1050253722(0x3e99999a, float:0.3)
            int r4 = (r3 > r4 ? 1 : (r3 == r4 ? 0 : -1))
            if (r4 > 0) goto L_0x0231
            android.graphics.Path r4 = r0.mBoltPath
            android.graphics.Paint r6 = r0.mBoltPaint
            r1.drawPath(r4, r6)
            goto L_0x023a
        L_0x0231:
            android.graphics.Path r4 = r0.mShapePath
            android.graphics.Path r6 = r0.mBoltPath
            android.graphics.Path$Op r7 = android.graphics.Path.Op.DIFFERENCE
            r4.op(r6, r7)
        L_0x023a:
            goto L_0x035f
        L_0x023c:
            r23 = r4
            r24 = r6
            r27 = r7
            r28 = r9
            r29 = r12
            boolean r3 = r0.mPowerSaveEnabled
            if (r3 == 0) goto L_0x035f
            android.graphics.RectF r3 = r0.mFrame
            float r3 = r3.width()
            r4 = 1073741824(0x40000000, float:2.0)
            float r3 = r3 * r4
            r6 = 1077936128(0x40400000, float:3.0)
            float r3 = r3 / r6
            android.graphics.RectF r6 = r0.mFrame
            float r6 = r6.left
            android.graphics.RectF r7 = r0.mFrame
            float r7 = r7.width()
            float r7 = r7 - r3
            float r7 = r7 / r4
            float r6 = r6 + r7
            android.graphics.RectF r7 = r0.mFrame
            float r7 = r7.top
            android.graphics.RectF r9 = r0.mFrame
            float r9 = r9.height()
            float r9 = r9 - r3
            float r9 = r9 / r4
            float r7 = r7 + r9
            android.graphics.RectF r9 = r0.mFrame
            float r9 = r9.right
            android.graphics.RectF r12 = r0.mFrame
            float r12 = r12.width()
            float r12 = r12 - r3
            float r12 = r12 / r4
            float r9 = r9 - r12
            android.graphics.RectF r12 = r0.mFrame
            float r12 = r12.bottom
            android.graphics.RectF r14 = r0.mFrame
            float r14 = r14.height()
            float r14 = r14 - r3
            float r14 = r14 / r4
            float r12 = r12 - r14
            android.graphics.RectF r4 = r0.mPlusFrame
            float r4 = r4.left
            int r4 = (r4 > r6 ? 1 : (r4 == r6 ? 0 : -1))
            if (r4 != 0) goto L_0x02b5
            android.graphics.RectF r4 = r0.mPlusFrame
            float r4 = r4.top
            int r4 = (r4 > r7 ? 1 : (r4 == r7 ? 0 : -1))
            if (r4 != 0) goto L_0x02b5
            android.graphics.RectF r4 = r0.mPlusFrame
            float r4 = r4.right
            int r4 = (r4 > r9 ? 1 : (r4 == r9 ? 0 : -1))
            if (r4 != 0) goto L_0x02b5
            android.graphics.RectF r4 = r0.mPlusFrame
            float r4 = r4.bottom
            int r4 = (r4 > r12 ? 1 : (r4 == r12 ? 0 : -1))
            if (r4 == 0) goto L_0x02ab
            goto L_0x02b5
        L_0x02ab:
            r30 = r3
            r31 = r6
            r32 = r7
            r20 = 0
            goto L_0x034a
        L_0x02b5:
            android.graphics.RectF r4 = r0.mPlusFrame
            r4.set(r6, r7, r9, r12)
            android.graphics.Path r4 = r0.mPlusPath
            r4.reset()
            android.graphics.Path r4 = r0.mPlusPath
            android.graphics.RectF r14 = r0.mPlusFrame
            float r14 = r14.left
            float[] r15 = r0.mPlusPoints
            r17 = 0
            r15 = r15[r17]
            r30 = r3
            android.graphics.RectF r3 = r0.mPlusFrame
            float r3 = r3.width()
            float r15 = r15 * r3
            float r14 = r14 + r15
            android.graphics.RectF r3 = r0.mPlusFrame
            float r3 = r3.top
            float[] r15 = r0.mPlusPoints
            r15 = r15[r19]
            r31 = r6
            android.graphics.RectF r6 = r0.mPlusFrame
            float r6 = r6.height()
            float r15 = r15 * r6
            float r3 = r3 + r15
            r4.moveTo(r14, r3)
            r16 = 2
        L_0x02ec:
            r3 = r16
            float[] r4 = r0.mPlusPoints
            int r4 = r4.length
            if (r3 >= r4) goto L_0x0321
            android.graphics.Path r4 = r0.mPlusPath
            android.graphics.RectF r6 = r0.mPlusFrame
            float r6 = r6.left
            float[] r14 = r0.mPlusPoints
            r14 = r14[r3]
            android.graphics.RectF r15 = r0.mPlusFrame
            float r15 = r15.width()
            float r14 = r14 * r15
            float r6 = r6 + r14
            android.graphics.RectF r14 = r0.mPlusFrame
            float r14 = r14.top
            float[] r15 = r0.mPlusPoints
            int r16 = r3 + 1
            r15 = r15[r16]
            r32 = r7
            android.graphics.RectF r7 = r0.mPlusFrame
            float r7 = r7.height()
            float r15 = r15 * r7
            float r14 = r14 + r15
            r4.lineTo(r6, r14)
            int r16 = r3 + 2
            r7 = r32
            goto L_0x02ec
        L_0x0321:
            r32 = r7
            android.graphics.Path r3 = r0.mPlusPath
            android.graphics.RectF r4 = r0.mPlusFrame
            float r4 = r4.left
            float[] r6 = r0.mPlusPoints
            r20 = 0
            r6 = r6[r20]
            android.graphics.RectF r7 = r0.mPlusFrame
            float r7 = r7.width()
            float r6 = r6 * r7
            float r4 = r4 + r6
            android.graphics.RectF r6 = r0.mPlusFrame
            float r6 = r6.top
            float[] r7 = r0.mPlusPoints
            r7 = r7[r19]
            android.graphics.RectF r14 = r0.mPlusFrame
            float r14 = r14.height()
            float r7 = r7 * r14
            float r6 = r6 + r7
            r3.lineTo(r4, r6)
        L_0x034a:
            android.graphics.Path r3 = r0.mShapePath
            android.graphics.Path r4 = r0.mPlusPath
            android.graphics.Path$Op r6 = android.graphics.Path.Op.DIFFERENCE
            r3.op(r4, r6)
            boolean r3 = r0.mPowerSaveAsColorError
            if (r3 == 0) goto L_0x0361
            android.graphics.Path r3 = r0.mPlusPath
            android.graphics.Paint r4 = r0.mPlusPaint
            r1.drawPath(r3, r4)
            goto L_0x0361
        L_0x035f:
            r20 = 0
        L_0x0361:
            r3 = 0
            r4 = 0
            r6 = 0
            r7 = 0
            boolean r9 = r0.mCharging
            if (r9 != 0) goto L_0x03e6
            boolean r9 = r0.mPowerSaveEnabled
            if (r9 != 0) goto L_0x03e6
            int r9 = r0.mCriticalLevel
            if (r2 <= r9) goto L_0x03e6
            boolean r9 = r0.mShowPercent
            if (r9 == 0) goto L_0x03e6
            android.graphics.Paint r9 = r0.mTextPaint
            int r14 = r0.getColorForLevel(r2)
            r9.setColor(r14)
            android.graphics.Paint r9 = r0.mTextPaint
            float r14 = (float) r5
            int r15 = r0.mLevel
            r12 = 100
            if (r15 != r12) goto L_0x038b
            r12 = 1052938076(0x3ec28f5c, float:0.38)
            goto L_0x038d
        L_0x038b:
            r12 = 1056964608(0x3f000000, float:0.5)
        L_0x038d:
            float r14 = r14 * r12
            r9.setTextSize(r14)
            android.graphics.Paint r9 = r0.mTextPaint
            android.graphics.Paint$FontMetrics r9 = r9.getFontMetrics()
            float r9 = r9.ascent
            float r9 = -r9
            r0.mTextHeight = r9
            java.lang.String r7 = java.lang.String.valueOf(r2)
            int r9 = r0.mWidth
            float r9 = (float) r9
            r12 = 1056964608(0x3f000000, float:0.5)
            float r9 = r9 * r12
            float r12 = (float) r10
            float r4 = r9 + r12
            int r9 = r0.mHeight
            float r9 = (float) r9
            float r12 = r0.mTextHeight
            float r9 = r9 + r12
            r12 = 1055957975(0x3ef0a3d7, float:0.47)
            float r9 = r9 * r12
            float r12 = (float) r11
            float r6 = r9 + r12
            int r9 = (r8 > r6 ? 1 : (r8 == r6 ? 0 : -1))
            if (r9 <= 0) goto L_0x03bb
            goto L_0x03bd
        L_0x03bb:
            r19 = r20
        L_0x03bd:
            r3 = r19
            if (r3 != 0) goto L_0x03e6
            android.graphics.Path r9 = r0.mTextPath
            r9.reset()
            android.graphics.Paint r9 = r0.mTextPaint
            r35 = 0
            int r36 = r7.length()
            android.graphics.Path r12 = r0.mTextPath
            r33 = r9
            r34 = r7
            r37 = r4
            r38 = r6
            r39 = r12
            r33.getTextPath(r34, r35, r36, r37, r38, r39)
            android.graphics.Path r9 = r0.mShapePath
            android.graphics.Path r12 = r0.mTextPath
            android.graphics.Path$Op r14 = android.graphics.Path.Op.DIFFERENCE
            r9.op(r12, r14)
        L_0x03e6:
            android.graphics.Path r9 = r0.mShapePath
            android.graphics.Paint r12 = r0.mFramePaint
            r1.drawPath(r9, r12)
            android.graphics.RectF r9 = r0.mFrame
            r9.top = r8
            r41.save()
            android.graphics.RectF r9 = r0.mFrame
            r1.clipRect(r9)
            android.graphics.Path r9 = r0.mShapePath
            android.graphics.Paint r12 = r0.mBatteryPaint
            r1.drawPath(r9, r12)
            r41.restore()
            boolean r9 = r0.mCharging
            if (r9 != 0) goto L_0x0432
            boolean r9 = r0.mPowerSaveEnabled
            if (r9 != 0) goto L_0x0432
            int r9 = r0.mCriticalLevel
            if (r2 > r9) goto L_0x042b
            int r9 = r0.mWidth
            float r9 = (float) r9
            r12 = 1056964608(0x3f000000, float:0.5)
            float r9 = r9 * r12
            float r12 = (float) r10
            float r9 = r9 + r12
            int r12 = r0.mHeight
            float r12 = (float) r12
            float r14 = r0.mWarningTextHeight
            float r12 = r12 + r14
            r14 = 1056293519(0x3ef5c28f, float:0.48)
            float r12 = r12 * r14
            float r14 = (float) r11
            float r12 = r12 + r14
            java.lang.String r14 = r0.mWarningString
            android.graphics.Paint r15 = r0.mWarningTextPaint
            r1.drawText(r14, r9, r12, r15)
            goto L_0x0432
        L_0x042b:
            if (r3 == 0) goto L_0x0432
            android.graphics.Paint r9 = r0.mTextPaint
            r1.drawText(r7, r4, r6, r9)
        L_0x0432:
            boolean r9 = r0.mCharging
            if (r9 != 0) goto L_0x0445
            boolean r9 = r0.mPowerSaveEnabled
            if (r9 == 0) goto L_0x0445
            boolean r9 = r0.mPowerSaveAsColorError
            if (r9 == 0) goto L_0x0445
            android.graphics.Path r9 = r0.mOutlinePath
            android.graphics.Paint r12 = r0.mPowersavePaint
            r1.drawPath(r9, r12)
        L_0x0445:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.settingslib.graph.BatteryMeterDrawableBase.draw(android.graphics.Canvas):void");
    }

    public void setAlpha(int alpha) {
    }

    public void setColorFilter(ColorFilter colorFilter) {
        this.mFramePaint.setColorFilter(colorFilter);
        this.mBatteryPaint.setColorFilter(colorFilter);
        this.mWarningTextPaint.setColorFilter(colorFilter);
        this.mBoltPaint.setColorFilter(colorFilter);
        this.mPlusPaint.setColorFilter(colorFilter);
    }

    public int getOpacity() {
        return 0;
    }

    /* access modifiers changed from: protected */
    public float getAspectRatio() {
        return 0.58f;
    }

    /* access modifiers changed from: protected */
    public float getRadiusRatio() {
        return 0.05882353f;
    }
}
