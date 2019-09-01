package com.android.systemui.statusbar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Notification;
import android.app.NotificationCompat;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.FloatProperty;
import android.util.Log;
import android.util.Property;
import android.util.TypedValue;
import android.view.ViewDebug;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.SystemUICompat;
import com.android.systemui.miui.statusbar.ExpandedNotification;
import com.android.systemui.statusbar.notification.NotificationIconDozeHelper;
import com.android.systemui.statusbar.notification.NotificationUtils;
import com.android.systemui.util.function.Consumer;
import java.text.NumberFormat;

public class StatusBarIconView extends AnimatedImageView {
    private static final Property<StatusBarIconView, Float> DOT_APPEAR_AMOUNT = new FloatProperty<StatusBarIconView>("dot_appear_amount") {
        public void setValue(StatusBarIconView object, float value) {
            object.setDotAppearAmount(value);
        }

        public Float get(StatusBarIconView object) {
            return Float.valueOf(object.getDotAppearAmount());
        }
    };
    private static final Property<StatusBarIconView, Float> ICON_APPEAR_AMOUNT = new FloatProperty<StatusBarIconView>("iconAppearAmount") {
        public void setValue(StatusBarIconView object, float value) {
            object.setIconAppearAmount(value);
        }

        public Float get(StatusBarIconView object) {
            return Float.valueOf(object.getIconAppearAmount());
        }
    };
    private final int ANIMATION_DURATION_FAST;
    private boolean mAlwaysScaleIcon;
    /* access modifiers changed from: private */
    public int mAnimationStartColor;
    private final boolean mBlocked;
    /* access modifiers changed from: private */
    public ValueAnimator mColorAnimator;
    private final ValueAnimator.AnimatorUpdateListener mColorUpdater;
    private int mCurrentSetColor;
    /* access modifiers changed from: private */
    public float mDarkAmount;
    private int mDecorColor;
    private int mDensity;
    /* access modifiers changed from: private */
    public ObjectAnimator mDotAnimator;
    private float mDotAppearAmount;
    private final Paint mDotPaint;
    private float mDotRadius;
    private final NotificationIconDozeHelper mDozer;
    private int mDrawableColor;
    private StatusBarIcon mIcon;
    private float mIconAppearAmount;
    /* access modifiers changed from: private */
    public ObjectAnimator mIconAppearAnimator;
    /* access modifiers changed from: private */
    public int mIconColor;
    private float mIconScale;
    private ExpandedNotification mNotification;
    private Drawable mNumberBackground;
    private Paint mNumberPain;
    private String mNumberText;
    private int mNumberX;
    private int mNumberY;
    private OnVisibilityChangedListener mOnVisibilityChangedListener;
    @ViewDebug.ExportedProperty
    private String mSlot;
    private int mStaticDotRadius;
    private int mStatusBarIconDrawingSize;
    private int mStatusBarIconDrawingSizeDark;
    private int mStatusBarIconSize;
    private int mVisibleState;

    public interface OnVisibilityChangedListener {
        void onVisibilityChanged(int i);
    }

    public StatusBarIconView(Context context, String slot, ExpandedNotification sbn) {
        this(context, slot, sbn, false);
    }

    public StatusBarIconView(Context context, String slot, ExpandedNotification sbn, boolean blocked) {
        super(context);
        this.ANIMATION_DURATION_FAST = 100;
        this.mStatusBarIconDrawingSizeDark = 1;
        this.mStatusBarIconDrawingSize = 1;
        this.mStatusBarIconSize = 1;
        this.mIconScale = 1.0f;
        this.mDotPaint = new Paint();
        this.mVisibleState = 0;
        this.mIconAppearAmount = 1.0f;
        this.mCurrentSetColor = 0;
        this.mAnimationStartColor = 0;
        this.mColorUpdater = new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                StatusBarIconView.this.setColorInternal(NotificationUtils.interpolateColors(StatusBarIconView.this.mAnimationStartColor, StatusBarIconView.this.mIconColor, animation.getAnimatedFraction()));
            }
        };
        this.mDozer = new NotificationIconDozeHelper(context);
        this.mBlocked = blocked;
        this.mSlot = slot;
        this.mNumberPain = new Paint();
        this.mNumberPain.setTextAlign(Paint.Align.CENTER);
        this.mNumberPain.setColor(context.getColor(R.drawable.notification_number_text_color));
        this.mNumberPain.setAntiAlias(true);
        setNotification(sbn);
        maybeUpdateIconScaleDimens();
        setScaleType(ImageView.ScaleType.CENTER);
        this.mDensity = context.getResources().getDisplayMetrics().densityDpi;
        if (this.mNotification != null) {
            setDecorColor(getContext().getColor(SystemUICompat.getNotificationDefaultColor()));
        }
        reloadDimens();
    }

    private void maybeUpdateIconScaleDimens() {
        if (this.mNotification != null || this.mAlwaysScaleIcon) {
            updateIconScaleDimens();
        }
    }

    private void updateIconScaleDimens() {
        Resources res = this.mContext.getResources();
        this.mStatusBarIconSize = res.getDimensionPixelSize(R.dimen.status_bar_icon_size);
        this.mStatusBarIconDrawingSizeDark = res.getDimensionPixelSize(R.dimen.status_bar_icon_drawing_size_dark);
        this.mStatusBarIconDrawingSize = res.getDimensionPixelSize(R.dimen.status_bar_icon_drawing_size);
        updateIconScale();
    }

    /* access modifiers changed from: private */
    public void updateIconScale() {
        this.mIconScale = NotificationUtils.interpolate((float) this.mStatusBarIconDrawingSize, (float) this.mStatusBarIconDrawingSizeDark, this.mDarkAmount) / ((float) this.mStatusBarIconSize);
    }

    public float getIconScaleFullyDark() {
        return ((float) this.mStatusBarIconDrawingSizeDark) / ((float) this.mStatusBarIconDrawingSize);
    }

    public float getIconScale() {
        return this.mIconScale;
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        int density = newConfig.densityDpi;
        if (density != this.mDensity) {
            this.mDensity = density;
            maybeUpdateIconScaleDimens();
            updateDrawable();
            reloadDimens();
        }
    }

    private void reloadDimens() {
        boolean applyRadius = this.mDotRadius == ((float) this.mStaticDotRadius);
        this.mStaticDotRadius = getResources().getDimensionPixelSize(R.dimen.overflow_dot_radius);
        if (applyRadius) {
            this.mDotRadius = (float) this.mStaticDotRadius;
        }
    }

    public void setNotification(ExpandedNotification notification) {
        this.mNotification = notification;
        if (notification != null) {
            setContentDescription(notification.getNotification());
        }
    }

    public StatusBarIconView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.ANIMATION_DURATION_FAST = 100;
        this.mStatusBarIconDrawingSizeDark = 1;
        this.mStatusBarIconDrawingSize = 1;
        this.mStatusBarIconSize = 1;
        this.mIconScale = 1.0f;
        this.mDotPaint = new Paint();
        this.mVisibleState = 0;
        this.mIconAppearAmount = 1.0f;
        this.mCurrentSetColor = 0;
        this.mAnimationStartColor = 0;
        this.mColorUpdater = new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                StatusBarIconView.this.setColorInternal(NotificationUtils.interpolateColors(StatusBarIconView.this.mAnimationStartColor, StatusBarIconView.this.mIconColor, animation.getAnimatedFraction()));
            }
        };
        this.mDozer = new NotificationIconDozeHelper(context);
        this.mBlocked = false;
        this.mAlwaysScaleIcon = true;
        updateIconScaleDimens();
        this.mDensity = context.getResources().getDisplayMetrics().densityDpi;
    }

    public boolean equalIcons(Icon a, Icon b) {
        boolean z = true;
        if (a == b) {
            return true;
        }
        if (a.getType() != b.getType()) {
            return false;
        }
        int type = a.getType();
        if (type == 2) {
            if (!a.getResPackage().equals(b.getResPackage()) || a.getResId() != b.getResId()) {
                z = false;
            }
            return z;
        } else if (type != 4) {
            return false;
        } else {
            return a.getUriString().equals(b.getUriString());
        }
    }

    public boolean set(StatusBarIcon icon) {
        int i = 0;
        boolean iconEquals = this.mIcon != null && equalIcons(this.mIcon.icon, icon.icon);
        boolean levelEquals = iconEquals && this.mIcon.iconLevel == icon.iconLevel;
        boolean visibilityEquals = this.mIcon != null && this.mIcon.visible == icon.visible;
        boolean numberEquals = this.mIcon != null && this.mIcon.number == icon.number;
        this.mIcon = icon.clone();
        setContentDescription(icon.contentDescription);
        if (!iconEquals) {
            if (!updateDrawable(false)) {
                return false;
            }
            setTag(R.id.icon_is_grayscale, null);
        }
        if (!levelEquals) {
            setImageLevel(icon.iconLevel);
        }
        if (!numberEquals) {
            if (icon.number <= 0 || !getContext().getResources().getBoolean(R.bool.config_statusBarShowNumber)) {
                this.mNumberBackground = null;
                this.mNumberText = null;
            } else {
                if (this.mNumberBackground == null) {
                    this.mNumberBackground = getContext().getResources().getDrawable(R.drawable.ic_notification_overlay);
                }
                placeNumber();
            }
            invalidate();
        }
        if (!visibilityEquals) {
            if (!icon.visible || this.mBlocked) {
                i = 8;
            }
            setVisibility(i);
        }
        return true;
    }

    public void updateDrawable() {
        if (this.mIcon == null || this.mIcon.icon == null || this.mIcon.icon.getType() != 2 || !TextUtils.equals("com.android.systemui", this.mIcon.icon.getResPackage()) || this.mIcon.icon.getResId() == 0) {
            updateDrawable(true);
        }
    }

    private boolean updateDrawable(boolean withClear) {
        if (this.mIcon == null) {
            return false;
        }
        try {
            Drawable drawable = getIcon(this.mIcon);
            if (drawable == null) {
                Log.w("StatusBarIconView", "No icon for slot " + this.mSlot + "; " + this.mIcon.icon);
                return false;
            }
            if (withClear) {
                setImageDrawable(null);
            }
            setImageDrawable(drawable);
            return true;
        } catch (OutOfMemoryError e) {
            Log.w("StatusBarIconView", "OOM while inflating " + this.mIcon.icon + " for slot " + this.mSlot);
            return false;
        }
    }

    public Icon getSourceIcon() {
        return this.mIcon.icon;
    }

    private Drawable getIcon(StatusBarIcon icon) {
        return getIcon(getContext(), icon);
    }

    public static Drawable getIcon(Context context, StatusBarIcon statusBarIcon) {
        int userId = statusBarIcon.user.getIdentifier();
        if (userId == -1) {
            userId = 0;
        }
        Drawable icon = statusBarIcon.icon.loadDrawableAsUser(context, userId);
        TypedValue typedValue = new TypedValue();
        context.getResources().getValue(R.dimen.status_bar_icon_scale_factor, typedValue, true);
        float scaleFactor = typedValue.getFloat();
        if (scaleFactor == 1.0f) {
            return icon;
        }
        return new ScalingDrawableWrapper(icon, scaleFactor);
    }

    public StatusBarIcon getStatusBarIcon() {
        return this.mIcon;
    }

    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        if (this.mNotification != null) {
            event.setParcelableData(this.mNotification.getNotification());
        }
    }

    /* access modifiers changed from: protected */
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (this.mNumberBackground != null) {
            placeNumber();
        }
    }

    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);
        updateDrawable();
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        float alpha;
        float fadeOutAmount;
        if (this.mIconAppearAmount > 0.0f) {
            canvas.save();
            canvas.scale(this.mIconScale * this.mIconAppearAmount, this.mIconScale * this.mIconAppearAmount, (float) (getWidth() / 2), (float) (getHeight() / 2));
            super.onDraw(canvas);
            canvas.restore();
        }
        if (this.mNumberBackground != null) {
            this.mNumberBackground.draw(canvas);
            canvas.drawText(this.mNumberText, (float) this.mNumberX, (float) this.mNumberY, this.mNumberPain);
        }
        if (this.mDotAppearAmount != 0.0f) {
            if (this.mDotAppearAmount <= 1.0f) {
                fadeOutAmount = this.mDotRadius * this.mDotAppearAmount;
                alpha = 1.0f;
            } else {
                float fadeOutAmount2 = this.mDotAppearAmount - 1.0f;
                alpha = 1.0f - fadeOutAmount2;
                fadeOutAmount = NotificationUtils.interpolate(this.mDotRadius, (float) (getWidth() / 4), fadeOutAmount2);
            }
            this.mDotPaint.setAlpha((int) (255.0f * alpha));
            canvas.drawCircle((float) (getWidth() / 2), (float) (getHeight() / 2), fadeOutAmount, this.mDotPaint);
        }
    }

    /* access modifiers changed from: protected */
    public void debug(int depth) {
        super.debug(depth);
        Log.d("View", debugIndent(depth) + "slot=" + this.mSlot);
        Log.d("View", debugIndent(depth) + "icon=" + this.mIcon);
    }

    /* access modifiers changed from: package-private */
    public void placeNumber() {
        String str;
        if (this.mIcon.number > getContext().getResources().getInteger(17694723)) {
            str = getContext().getResources().getString(17039383);
        } else {
            str = NumberFormat.getIntegerInstance().format((long) this.mIcon.number);
        }
        this.mNumberText = str;
        int w = getWidth();
        int h = getHeight();
        Rect r = new Rect();
        this.mNumberPain.getTextBounds(str, 0, str.length(), r);
        int tw = r.right - r.left;
        int th = r.bottom - r.top;
        this.mNumberBackground.getPadding(r);
        int dw = r.left + tw + r.right;
        if (dw < this.mNumberBackground.getMinimumWidth()) {
            dw = this.mNumberBackground.getMinimumWidth();
        }
        this.mNumberX = (w - r.right) - (((dw - r.right) - r.left) / 2);
        int dh = r.top + th + r.bottom;
        if (dh < this.mNumberBackground.getMinimumWidth()) {
            dh = this.mNumberBackground.getMinimumWidth();
        }
        this.mNumberY = (h - r.bottom) - ((((dh - r.top) - th) - r.bottom) / 2);
        this.mNumberBackground.setBounds(w - dw, h - dh, w, h);
    }

    private void setContentDescription(Notification notification) {
        if (notification != null) {
            CharSequence d = contentDescForNotification(this.mContext, notification);
            if (!TextUtils.isEmpty(d)) {
                setContentDescription(d);
            }
        }
    }

    public String toString() {
        return "StatusBarIconView(slot=" + this.mSlot + " icon=" + this.mIcon + " notification=" + this.mNotification + ")";
    }

    public ExpandedNotification getNotification() {
        return this.mNotification;
    }

    public String getSlot() {
        return this.mSlot;
    }

    public static CharSequence contentDescForNotification(Context c, Notification n) {
        if (Build.VERSION.SDK_INT == 23 && n != null) {
            return n.tickerText;
        }
        String appName = "";
        try {
            appName = NotificationCompat.loadHeaderAppName(NotificationCompat.recoverBuilder(c, n));
        } catch (RuntimeException e) {
            Log.e("StatusBarIconView", "Unable to recover builder", e);
            Parcelable appInfo = n.extras.getParcelable("android.appInfo");
            if (appInfo instanceof ApplicationInfo) {
                appName = String.valueOf(((ApplicationInfo) appInfo).loadLabel(c.getPackageManager()));
            }
        }
        CharSequence title = n.extras.getCharSequence("android.title");
        CharSequence text = n.extras.getCharSequence("android.text");
        CharSequence ticker = n.tickerText;
        CharSequence titleOrText = TextUtils.equals(title, appName) ? text : title;
        return c.getString(R.string.accessibility_desc_notification_icon, new Object[]{appName, !TextUtils.isEmpty(titleOrText) ? titleOrText : !TextUtils.isEmpty(ticker) ? ticker : ""});
    }

    public void setDecorColor(int iconTint) {
        this.mDecorColor = iconTint;
        updateDecorColor();
    }

    /* access modifiers changed from: private */
    public void updateDecorColor() {
        int color = NotificationUtils.interpolateColors(this.mDecorColor, -1, this.mDarkAmount);
        if (this.mDotPaint.getColor() != color) {
            this.mDotPaint.setColor(color);
            if (this.mDotAppearAmount != 0.0f) {
                invalidate();
            }
        }
    }

    public void setStaticDrawableColor(int color) {
        this.mDrawableColor = color;
        setColorInternal(color);
        this.mIconColor = color;
        this.mDozer.setColor(color);
    }

    /* access modifiers changed from: private */
    public void setColorInternal(int color) {
        this.mCurrentSetColor = color;
        updateIconColor();
    }

    /* access modifiers changed from: private */
    public void updateIconColor() {
        if (this.mCurrentSetColor != 0) {
            setImageTintList(ColorStateList.valueOf(NotificationUtils.interpolateColors(this.mCurrentSetColor, -1, this.mDarkAmount)));
            return;
        }
        setImageTintList(null);
        this.mDozer.updateGrayscale((ImageView) this, this.mDarkAmount);
    }

    public void setIconColor(int iconColor, boolean animate) {
        if (this.mIconColor != iconColor) {
            this.mIconColor = iconColor;
            if (this.mColorAnimator != null) {
                this.mColorAnimator.cancel();
            }
            if (this.mCurrentSetColor != iconColor) {
                if (!animate || this.mCurrentSetColor == 0) {
                    setColorInternal(iconColor);
                } else {
                    this.mAnimationStartColor = this.mCurrentSetColor;
                    this.mColorAnimator = ValueAnimator.ofFloat(new float[]{0.0f, 1.0f});
                    this.mColorAnimator.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
                    this.mColorAnimator.setDuration(100);
                    this.mColorAnimator.addUpdateListener(this.mColorUpdater);
                    this.mColorAnimator.addListener(new AnimatorListenerAdapter() {
                        public void onAnimationEnd(Animator animation) {
                            ValueAnimator unused = StatusBarIconView.this.mColorAnimator = null;
                            int unused2 = StatusBarIconView.this.mAnimationStartColor = 0;
                        }
                    });
                    this.mColorAnimator.start();
                }
            }
        }
    }

    public int getStaticDrawableColor() {
        return this.mDrawableColor;
    }

    public void setVisibleState(int state, boolean animate) {
        setVisibleState(state, animate, null);
    }

    public boolean hasOverlappingRendering() {
        return false;
    }

    public void setVisibleState(int visibleState, boolean animate, Runnable endRunnable) {
        int i = visibleState;
        final Runnable runnable = endRunnable;
        boolean runnableAdded = false;
        if (i != this.mVisibleState) {
            this.mVisibleState = i;
            if (this.mIconAppearAnimator != null) {
                this.mIconAppearAnimator.cancel();
            }
            if (this.mDotAnimator != null) {
                this.mDotAnimator.cancel();
            }
            if (animate) {
                float targetAmount = 0.0f;
                Interpolator interpolator = Interpolators.FAST_OUT_LINEAR_IN;
                if (i == 0) {
                    targetAmount = 1.0f;
                    interpolator = Interpolators.LINEAR_OUT_SLOW_IN;
                }
                float currentAmount = getIconAppearAmount();
                boolean z = false;
                if (targetAmount != currentAmount) {
                    this.mIconAppearAnimator = ObjectAnimator.ofFloat(this, ICON_APPEAR_AMOUNT, new float[]{currentAmount, targetAmount});
                    this.mIconAppearAnimator.setInterpolator(interpolator);
                    this.mIconAppearAnimator.setDuration(100);
                    this.mIconAppearAnimator.addListener(new AnimatorListenerAdapter() {
                        public void onAnimationEnd(Animator animation) {
                            ObjectAnimator unused = StatusBarIconView.this.mIconAppearAnimator = null;
                            StatusBarIconView.this.runRunnable(runnable);
                        }
                    });
                    this.mIconAppearAnimator.start();
                    runnableAdded = true;
                }
                float targetAmount2 = i == 0 ? 2.0f : 0.0f;
                Interpolator interpolator2 = Interpolators.FAST_OUT_LINEAR_IN;
                if (i == 1) {
                    targetAmount2 = 1.0f;
                    interpolator2 = Interpolators.LINEAR_OUT_SLOW_IN;
                }
                float currentAmount2 = getDotAppearAmount();
                if (targetAmount2 != currentAmount2) {
                    this.mDotAnimator = ObjectAnimator.ofFloat(this, DOT_APPEAR_AMOUNT, new float[]{currentAmount2, targetAmount2});
                    this.mDotAnimator.setInterpolator(interpolator2);
                    this.mDotAnimator.setDuration(100);
                    if (!runnableAdded) {
                        z = true;
                    }
                    final boolean runRunnable = z;
                    this.mDotAnimator.addListener(new AnimatorListenerAdapter() {
                        public void onAnimationEnd(Animator animation) {
                            ObjectAnimator unused = StatusBarIconView.this.mDotAnimator = null;
                            if (runRunnable) {
                                StatusBarIconView.this.runRunnable(runnable);
                            }
                        }
                    });
                    this.mDotAnimator.start();
                    runnableAdded = true;
                }
            } else {
                float f = 1.0f;
                setIconAppearAmount(i == 0 ? 1.0f : 0.0f);
                if (i != 1) {
                    f = i == 0 ? 2.0f : 0.0f;
                }
                setDotAppearAmount(f);
            }
        }
        if (!runnableAdded) {
            runRunnable(runnable);
        }
    }

    /* access modifiers changed from: private */
    public void runRunnable(Runnable runnable) {
        if (runnable != null) {
            runnable.run();
        }
    }

    public void setIconAppearAmount(float iconAppearAmount) {
        if (this.mIconAppearAmount != iconAppearAmount) {
            this.mIconAppearAmount = iconAppearAmount;
            invalidate();
        }
    }

    public float getIconAppearAmount() {
        return this.mIconAppearAmount;
    }

    public int getVisibleState() {
        return this.mVisibleState;
    }

    public void setDotAppearAmount(float dotAppearAmount) {
        if (this.mDotAppearAmount != dotAppearAmount) {
            this.mDotAppearAmount = dotAppearAmount;
            invalidate();
        }
    }

    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (this.mOnVisibilityChangedListener != null) {
            this.mOnVisibilityChangedListener.onVisibilityChanged(visibility);
        }
    }

    public float getDotAppearAmount() {
        return this.mDotAppearAmount;
    }

    public void setOnVisibilityChangedListener(OnVisibilityChangedListener listener) {
        this.mOnVisibilityChangedListener = listener;
    }

    public void setDark(boolean dark, boolean fade, long delay) {
        this.mDozer.setIntensityDark(new Consumer<Float>() {
            public void accept(Float f) {
                float unused = StatusBarIconView.this.mDarkAmount = f.floatValue();
                StatusBarIconView.this.updateIconScale();
                StatusBarIconView.this.updateDecorColor();
                StatusBarIconView.this.updateIconColor();
            }
        }, dark, fade, delay);
    }
}
