package com.android.settingslib.drawable;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class UserIconDrawable extends Drawable implements Drawable.Callback {
    private Drawable mBadge;
    private float mBadgeMargin;
    private float mBadgeRadius;
    private Bitmap mBitmap;
    private Paint mClearPaint;
    private float mDisplayRadius;
    private ColorStateList mFrameColor;
    private float mFramePadding;
    private Paint mFramePaint;
    private float mFrameWidth;
    private final Matrix mIconMatrix;
    private final Paint mIconPaint;
    private float mIntrinsicRadius;
    private boolean mInvalidated;
    private float mPadding;
    private final Paint mPaint;
    private int mSize;
    private ColorStateList mTintColor;
    private PorterDuff.Mode mTintMode;
    private Drawable mUserDrawable;
    private Bitmap mUserIcon;

    private static Drawable getDrawableForDisplayDensity(Context context, int drawable) {
        return context.getResources().getDrawableForDensity(drawable, context.getResources().getDisplayMetrics().densityDpi, context.getTheme());
    }

    public UserIconDrawable() {
        this(0);
    }

    public UserIconDrawable(int intrinsicSize) {
        this.mIconPaint = new Paint();
        this.mPaint = new Paint();
        this.mIconMatrix = new Matrix();
        this.mPadding = 0.0f;
        this.mSize = 0;
        this.mInvalidated = true;
        this.mTintColor = null;
        this.mTintMode = PorterDuff.Mode.SRC_ATOP;
        this.mFrameColor = null;
        this.mIconPaint.setAntiAlias(true);
        this.mIconPaint.setFilterBitmap(true);
        this.mPaint.setFilterBitmap(true);
        this.mPaint.setAntiAlias(true);
        if (intrinsicSize > 0) {
            setBounds(0, 0, intrinsicSize, intrinsicSize);
            setIntrinsicSize(intrinsicSize);
        }
        setIcon(null);
    }

    public UserIconDrawable setIcon(Bitmap icon) {
        if (this.mUserDrawable != null) {
            this.mUserDrawable.setCallback(null);
            this.mUserDrawable = null;
        }
        this.mUserIcon = icon;
        if (this.mUserIcon == null) {
            this.mIconPaint.setShader(null);
            this.mBitmap = null;
        } else {
            this.mIconPaint.setShader(new BitmapShader(icon, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        }
        onBoundsChange(getBounds());
        return this;
    }

    public UserIconDrawable setIconDrawable(Drawable icon) {
        if (this.mUserDrawable != null) {
            this.mUserDrawable.setCallback(null);
        }
        this.mUserIcon = null;
        this.mUserDrawable = icon;
        if (this.mUserDrawable == null) {
            this.mBitmap = null;
        } else {
            this.mUserDrawable.setCallback(this);
        }
        onBoundsChange(getBounds());
        return this;
    }

    public UserIconDrawable setBadge(Drawable badge) {
        this.mBadge = badge;
        if (this.mBadge != null) {
            if (this.mClearPaint == null) {
                this.mClearPaint = new Paint();
                this.mClearPaint.setAntiAlias(true);
                this.mClearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
                this.mClearPaint.setStyle(Paint.Style.FILL);
            }
            onBoundsChange(getBounds());
        } else {
            invalidateSelf();
        }
        return this;
    }

    public UserIconDrawable setBadgeIfManagedUser(Context context, int userId) {
        Drawable badge = null;
        if (((DevicePolicyManager) context.getSystemService(DevicePolicyManager.class)).getProfileOwnerAsUser(userId) != null) {
            badge = getDrawableForDisplayDensity(context, 17302335);
        }
        return setBadge(badge);
    }

    public void setBadgeRadius(float radius) {
        this.mBadgeRadius = radius;
        onBoundsChange(getBounds());
    }

    public void setBadgeMargin(float margin) {
        this.mBadgeMargin = margin;
        onBoundsChange(getBounds());
    }

    public void setPadding(float padding) {
        this.mPadding = padding;
        onBoundsChange(getBounds());
    }

    private void initFramePaint() {
        if (this.mFramePaint == null) {
            this.mFramePaint = new Paint();
            this.mFramePaint.setStyle(Paint.Style.STROKE);
            this.mFramePaint.setAntiAlias(true);
        }
    }

    public void setFrameWidth(float width) {
        initFramePaint();
        this.mFrameWidth = width;
        this.mFramePaint.setStrokeWidth(width);
        onBoundsChange(getBounds());
    }

    public void setFramePadding(float padding) {
        initFramePaint();
        this.mFramePadding = padding;
        onBoundsChange(getBounds());
    }

    public void setFrameColor(ColorStateList colorList) {
        initFramePaint();
        this.mFrameColor = colorList;
        invalidateSelf();
    }

    public void setIntrinsicSize(int size) {
        this.mSize = size;
    }

    public void draw(Canvas canvas) {
        if (this.mInvalidated) {
            rebake();
        }
        if (this.mBitmap != null) {
            if (this.mTintColor == null) {
                this.mPaint.setColorFilter(null);
            } else {
                int color = this.mTintColor.getColorForState(getState(), this.mTintColor.getDefaultColor());
                if (this.mPaint.getColorFilter() == null) {
                    this.mPaint.setColorFilter(new PorterDuffColorFilter(color, this.mTintMode));
                } else {
                    ((PorterDuffColorFilter) this.mPaint.getColorFilter()).setMode(this.mTintMode);
                    ((PorterDuffColorFilter) this.mPaint.getColorFilter()).setColor(color);
                }
            }
            canvas.drawBitmap(this.mBitmap, 0.0f, 0.0f, this.mPaint);
        }
    }

    public void setAlpha(int alpha) {
        this.mPaint.setAlpha(alpha);
        super.invalidateSelf();
    }

    public void setColorFilter(ColorFilter colorFilter) {
    }

    public void setTintList(ColorStateList tintList) {
        this.mTintColor = tintList;
        super.invalidateSelf();
    }

    public void setTintMode(PorterDuff.Mode mode) {
        this.mTintMode = mode;
        super.invalidateSelf();
    }

    public Drawable.ConstantState getConstantState() {
        return new BitmapDrawable(this.mBitmap).getConstantState();
    }

    public UserIconDrawable bake() {
        if (this.mSize > 0) {
            onBoundsChange(new Rect(0, 0, this.mSize, this.mSize));
            rebake();
            this.mFrameColor = null;
            this.mFramePaint = null;
            this.mClearPaint = null;
            if (this.mUserDrawable != null) {
                this.mUserDrawable.setCallback(null);
                this.mUserDrawable = null;
            } else if (this.mUserIcon != null) {
                this.mUserIcon.recycle();
                this.mUserIcon = null;
            }
            return this;
        }
        throw new IllegalStateException("Baking requires an explicit intrinsic size");
    }

    private void rebake() {
        this.mInvalidated = false;
        if (this.mBitmap != null && (this.mUserDrawable != null || this.mUserIcon != null)) {
            Canvas canvas = new Canvas(this.mBitmap);
            canvas.drawColor(0, PorterDuff.Mode.CLEAR);
            if (this.mUserDrawable != null) {
                this.mUserDrawable.draw(canvas);
            } else if (this.mUserIcon != null) {
                int saveId = canvas.save();
                canvas.concat(this.mIconMatrix);
                canvas.drawCircle(((float) this.mUserIcon.getWidth()) * 0.5f, ((float) this.mUserIcon.getHeight()) * 0.5f, this.mIntrinsicRadius, this.mIconPaint);
                canvas.restoreToCount(saveId);
            }
            if (this.mFrameColor != null) {
                this.mFramePaint.setColor(this.mFrameColor.getColorForState(getState(), 0));
            }
            if (this.mFrameWidth + this.mFramePadding > 0.001f) {
                canvas.drawCircle(getBounds().exactCenterX(), getBounds().exactCenterY(), (this.mDisplayRadius - this.mPadding) - (this.mFrameWidth * 0.5f), this.mFramePaint);
            }
            if (this.mBadge != null && this.mBadgeRadius > 0.001f) {
                float badgeDiameter = this.mBadgeRadius * 2.0f;
                float badgeTop = ((float) this.mBitmap.getHeight()) - badgeDiameter;
                float badgeLeft = ((float) this.mBitmap.getWidth()) - badgeDiameter;
                this.mBadge.setBounds((int) badgeLeft, (int) badgeTop, (int) (badgeLeft + badgeDiameter), (int) (badgeTop + badgeDiameter));
                canvas.drawCircle(this.mBadgeRadius + badgeLeft, this.mBadgeRadius + badgeTop, (((float) this.mBadge.getBounds().width()) * 0.5f) + this.mBadgeMargin, this.mClearPaint);
                this.mBadge.draw(canvas);
            }
        }
    }

    /* access modifiers changed from: protected */
    public void onBoundsChange(Rect bounds) {
        if (!bounds.isEmpty() && (this.mUserIcon != null || this.mUserDrawable != null)) {
            float newDisplayRadius = ((float) Math.min(bounds.width(), bounds.height())) * 0.5f;
            int size = (int) (newDisplayRadius * 2.0f);
            if (this.mBitmap == null || size != ((int) (this.mDisplayRadius * 2.0f))) {
                this.mDisplayRadius = newDisplayRadius;
                if (this.mBitmap != null) {
                    this.mBitmap.recycle();
                }
                this.mBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            }
            this.mDisplayRadius = ((float) Math.min(bounds.width(), bounds.height())) * 0.5f;
            float iconRadius = ((this.mDisplayRadius - this.mFrameWidth) - this.mFramePadding) - this.mPadding;
            RectF dstRect = new RectF(bounds.exactCenterX() - iconRadius, bounds.exactCenterY() - iconRadius, bounds.exactCenterX() + iconRadius, bounds.exactCenterY() + iconRadius);
            if (this.mUserDrawable != null) {
                Rect rounded = new Rect();
                dstRect.round(rounded);
                this.mIntrinsicRadius = ((float) Math.min(this.mUserDrawable.getIntrinsicWidth(), this.mUserDrawable.getIntrinsicHeight())) * 0.5f;
                this.mUserDrawable.setBounds(rounded);
            } else if (this.mUserIcon != null) {
                float iconCX = ((float) this.mUserIcon.getWidth()) * 0.5f;
                float iconCY = ((float) this.mUserIcon.getHeight()) * 0.5f;
                this.mIntrinsicRadius = Math.min(iconCX, iconCY);
                this.mIconMatrix.setRectToRect(new RectF(iconCX - this.mIntrinsicRadius, iconCY - this.mIntrinsicRadius, this.mIntrinsicRadius + iconCX, this.mIntrinsicRadius + iconCY), dstRect, Matrix.ScaleToFit.FILL);
            }
            invalidateSelf();
        }
    }

    public void invalidateSelf() {
        super.invalidateSelf();
        this.mInvalidated = true;
    }

    public boolean isStateful() {
        return this.mFrameColor != null && this.mFrameColor.isStateful();
    }

    public int getOpacity() {
        return -3;
    }

    public int getIntrinsicWidth() {
        return this.mSize <= 0 ? ((int) this.mIntrinsicRadius) * 2 : this.mSize;
    }

    public int getIntrinsicHeight() {
        return getIntrinsicWidth();
    }

    public void invalidateDrawable(Drawable who) {
        invalidateSelf();
    }

    public void scheduleDrawable(Drawable who, Runnable what, long when) {
        scheduleSelf(what, when);
    }

    public void unscheduleDrawable(Drawable who, Runnable what) {
        unscheduleSelf(what);
    }
}
