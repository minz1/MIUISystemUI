package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Icon;
import android.graphics.drawable.IconCompat;
import android.util.ArrayMap;
import android.util.AttributeSet;
import android.view.View;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.statusbar.AlphaOptimizedFrameLayout;
import com.android.systemui.statusbar.StatusBarIconView;
import com.android.systemui.statusbar.stack.AnimationFilter;
import com.android.systemui.statusbar.stack.AnimationProperties;
import com.android.systemui.statusbar.stack.ViewState;
import java.util.ArrayList;
import java.util.HashMap;

public class NotificationIconContainer extends AlphaOptimizedFrameLayout {
    /* access modifiers changed from: private */
    public static final AnimationProperties ADD_ICON_PROPERTIES = new AnimationProperties() {
        private AnimationFilter mAnimationFilter = new AnimationFilter().animateAlpha();

        public AnimationFilter getAnimationFilter() {
            return this.mAnimationFilter;
        }
    }.setDuration(200).setDelay(50);
    /* access modifiers changed from: private */
    public static final AnimationProperties DOT_ANIMATION_PROPERTIES = new AnimationProperties() {
        private AnimationFilter mAnimationFilter = new AnimationFilter().animateX();

        public AnimationFilter getAnimationFilter() {
            return this.mAnimationFilter;
        }
    }.setDuration(200);
    /* access modifiers changed from: private */
    public static final AnimationProperties ICON_ANIMATION_PROPERTIES = new AnimationProperties() {
        private AnimationFilter mAnimationFilter = new AnimationFilter().animateY().animateAlpha().animateScale();

        public AnimationFilter getAnimationFilter() {
            return this.mAnimationFilter;
        }
    }.setDuration(100).setCustomInterpolator(View.TRANSLATION_Y, Interpolators.ICON_OVERSHOT);
    /* access modifiers changed from: private */
    public static final AnimationProperties UNDARK_PROPERTIES = new AnimationProperties() {
        private AnimationFilter mAnimationFilter = new AnimationFilter().animateX();

        public AnimationFilter getAnimationFilter() {
            return this.mAnimationFilter;
        }
    }.setDuration(200);
    /* access modifiers changed from: private */
    public static final AnimationProperties mTempProperties = new AnimationProperties() {
        private AnimationFilter mAnimationFilter = new AnimationFilter();

        public AnimationFilter getAnimationFilter() {
            return this.mAnimationFilter;
        }
    };
    private int mActualLayoutWidth = Integer.MIN_VALUE;
    private float mActualPaddingEnd = -2.14748365E9f;
    private float mActualPaddingStart = -2.14748365E9f;
    /* access modifiers changed from: private */
    public int mAddAnimationStartIndex = -1;
    /* access modifiers changed from: private */
    public boolean mAnimationsEnabled = true;
    /* access modifiers changed from: private */
    public int mCannedAnimationStartIndex = -1;
    private boolean mChangingViewPositions;
    private boolean mDark;
    /* access modifiers changed from: private */
    public boolean mDisallowNextAnimation;
    private int mDotPadding;
    private int mIconSize;
    private final HashMap<View, IconState> mIconStates = new HashMap<>();
    private float mOpenedAmount = 0.0f;
    private Paint mPaint = new Paint();
    private ArrayMap<String, ArrayList<StatusBarIcon>> mReplacingIcons;
    private boolean mShowAllIcons = true;
    private int mSpeedBumpIndex = -1;
    private int mStaticDotRadius;
    private float mVisualOverflowAdaption;

    public class IconState extends ViewState {
        public float clampedAppearAmount = 1.0f;
        public float iconAppearAmount = 1.0f;
        public int iconColor = 0;
        public boolean justAdded = true;
        /* access modifiers changed from: private */
        public boolean justReplaced;
        public boolean justUndarkened;
        public boolean needsCannedAnimation;
        public boolean noAnimations;
        public boolean translateContent;
        public boolean useFullTransitionAmount;
        public boolean useLinearTransitionAmount;
        public int visibleState;

        public IconState() {
        }

        public void applyToView(View view) {
            if (view instanceof StatusBarIconView) {
                StatusBarIconView icon = (StatusBarIconView) view;
                boolean animate = false;
                AnimationProperties animationProperties = null;
                boolean z = true;
                boolean animationsAllowed = (NotificationIconContainer.this.mAnimationsEnabled || this.justUndarkened) && !NotificationIconContainer.this.mDisallowNextAnimation && !this.noAnimations;
                if (animationsAllowed) {
                    if (this.justAdded || this.justReplaced) {
                        super.applyToView(icon);
                        if (this.justAdded && this.iconAppearAmount != 0.0f) {
                            icon.setAlpha(0.0f);
                            icon.setVisibleState(2, false);
                            animationProperties = NotificationIconContainer.ADD_ICON_PROPERTIES;
                            animate = true;
                        }
                    } else if (this.justUndarkened) {
                        animationProperties = NotificationIconContainer.UNDARK_PROPERTIES;
                        animate = true;
                    } else if (this.visibleState != icon.getVisibleState()) {
                        animationProperties = NotificationIconContainer.DOT_ANIMATION_PROPERTIES;
                        animate = true;
                    }
                    if (!animate && NotificationIconContainer.this.mAddAnimationStartIndex >= 0 && NotificationIconContainer.this.indexOfChild(view) >= NotificationIconContainer.this.mAddAnimationStartIndex && !(icon.getVisibleState() == 2 && this.visibleState == 2)) {
                        animationProperties = NotificationIconContainer.DOT_ANIMATION_PROPERTIES;
                        animate = true;
                    }
                    if (this.needsCannedAnimation) {
                        AnimationFilter animationFilter = NotificationIconContainer.mTempProperties.getAnimationFilter();
                        animationFilter.reset();
                        animationFilter.combineFilter(NotificationIconContainer.ICON_ANIMATION_PROPERTIES.getAnimationFilter());
                        NotificationIconContainer.mTempProperties.resetCustomInterpolators();
                        NotificationIconContainer.mTempProperties.combineCustomInterpolators(NotificationIconContainer.ICON_ANIMATION_PROPERTIES);
                        if (animationProperties != null) {
                            animationFilter.combineFilter(animationProperties.getAnimationFilter());
                            NotificationIconContainer.mTempProperties.combineCustomInterpolators(animationProperties);
                        }
                        animationProperties = NotificationIconContainer.mTempProperties;
                        animationProperties.setDuration(100);
                        animate = true;
                        int unused = NotificationIconContainer.this.mCannedAnimationStartIndex = NotificationIconContainer.this.indexOfChild(view);
                    }
                    if (!animate && NotificationIconContainer.this.mCannedAnimationStartIndex >= 0 && NotificationIconContainer.this.indexOfChild(view) > NotificationIconContainer.this.mCannedAnimationStartIndex && !(icon.getVisibleState() == 2 && this.visibleState == 2)) {
                        AnimationFilter animationFilter2 = NotificationIconContainer.mTempProperties.getAnimationFilter();
                        animationFilter2.reset();
                        animationFilter2.animateX();
                        NotificationIconContainer.mTempProperties.resetCustomInterpolators();
                        animationProperties = NotificationIconContainer.mTempProperties;
                        animationProperties.setDuration(100);
                        animate = true;
                    }
                }
                icon.setVisibleState(this.visibleState, animationsAllowed);
                int i = this.iconColor;
                if (!this.needsCannedAnimation || !animationsAllowed) {
                    z = false;
                }
                icon.setIconColor(i, z);
                if (animate) {
                    animateTo(icon, animationProperties);
                } else {
                    super.applyToView(view);
                }
            }
            this.justAdded = false;
            this.justReplaced = false;
            this.needsCannedAnimation = false;
            this.justUndarkened = false;
        }

        public void initFrom(View view) {
            super.initFrom(view);
            if (view instanceof StatusBarIconView) {
                this.iconColor = ((StatusBarIconView) view).getStaticDrawableColor();
            }
        }
    }

    public NotificationIconContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
        initDimens();
        setWillNotDraw(true);
    }

    private void initDimens() {
        this.mDotPadding = getResources().getDimensionPixelSize(R.dimen.overflow_icon_dot_padding);
        this.mStaticDotRadius = getResources().getDimensionPixelSize(R.dimen.overflow_dot_radius);
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Paint paint = this.mPaint;
        paint.setColor(-65536);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawRect(getActualPaddingStart(), 0.0f, getLayoutEnd(), (float) getHeight(), paint);
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        initDimens();
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        float centerY = ((float) getHeight()) / 2.0f;
        this.mIconSize = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            int width = child.getMeasuredWidth();
            int height = child.getMeasuredHeight();
            int top = (int) (centerY - (((float) height) / 2.0f));
            child.layout(0, top, width, top + height);
            if (i == 0) {
                this.mIconSize = child.getWidth();
            }
        }
        if (this.mShowAllIcons) {
            resetViewStates();
            calculateIconTranslations();
            applyIconStates();
        }
    }

    public void applyIconStates() {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            ViewState childState = this.mIconStates.get(child);
            if (childState != null) {
                childState.applyToView(child);
            }
        }
        this.mAddAnimationStartIndex = -1;
        this.mCannedAnimationStartIndex = -1;
        this.mDisallowNextAnimation = false;
    }

    public void onViewAdded(View child) {
        super.onViewAdded(child);
        boolean isReplacingIcon = isReplacingIcon(child);
        if (!this.mChangingViewPositions) {
            IconState v = new IconState();
            if (isReplacingIcon) {
                v.justAdded = false;
                boolean unused = v.justReplaced = true;
            }
            this.mIconStates.put(child, v);
        }
        int childIndex = indexOfChild(child);
        if (childIndex < getChildCount() - 1 && !isReplacingIcon && this.mIconStates.get(getChildAt(childIndex + 1)).iconAppearAmount > 0.0f) {
            if (this.mAddAnimationStartIndex < 0) {
                this.mAddAnimationStartIndex = childIndex;
            } else {
                this.mAddAnimationStartIndex = Math.min(this.mAddAnimationStartIndex, childIndex);
            }
        }
        if (this.mDark && (child instanceof StatusBarIconView)) {
            ((StatusBarIconView) child).setDark(this.mDark, false, 0);
        }
    }

    private boolean isReplacingIcon(View child) {
        if (this.mReplacingIcons == null || !(child instanceof StatusBarIconView)) {
            return false;
        }
        StatusBarIconView iconView = (StatusBarIconView) child;
        Icon sourceIcon = iconView.getSourceIcon();
        ArrayList<StatusBarIcon> statusBarIcons = this.mReplacingIcons.get(iconView.getNotification().getGroupKey());
        if (statusBarIcons == null || !IconCompat.sameAs(sourceIcon, statusBarIcons.get(0).icon)) {
            return false;
        }
        return true;
    }

    public void onViewRemoved(View child) {
        super.onViewRemoved(child);
        if (child instanceof StatusBarIconView) {
            boolean isReplacingIcon = isReplacingIcon(child);
            final StatusBarIconView icon = (StatusBarIconView) child;
            if (icon.getVisibleState() != 2 && child.getVisibility() == 0 && isReplacingIcon) {
                int animationStartIndex = findFirstViewIndexAfter(icon.getTranslationX());
                if (this.mAddAnimationStartIndex < 0) {
                    this.mAddAnimationStartIndex = animationStartIndex;
                } else {
                    this.mAddAnimationStartIndex = Math.min(this.mAddAnimationStartIndex, animationStartIndex);
                }
            }
            if (this.mChangingViewPositions == 0) {
                this.mIconStates.remove(child);
                if (!isReplacingIcon) {
                    addTransientView(icon, 0);
                    icon.setVisibleState(2, true, new Runnable() {
                        public void run() {
                            NotificationIconContainer.this.removeTransientView(icon);
                        }
                    });
                }
            }
        }
    }

    private int findFirstViewIndexAfter(float translationX) {
        for (int i = 0; i < getChildCount(); i++) {
            if (getChildAt(i).getTranslationX() > translationX) {
                return i;
            }
        }
        return getChildCount();
    }

    public void resetViewStates() {
        for (int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            ViewState iconState = this.mIconStates.get(view);
            iconState.initFrom(view);
            iconState.alpha = 1.0f;
            iconState.hidden = false;
        }
    }

    public void calculateIconTranslations() {
        int i;
        float drawingScale;
        float layoutEnd;
        float overflowStart;
        boolean hasAmbient;
        float visualOverflowStart;
        int firstOverflowIndex;
        float translationX = getActualPaddingStart();
        int childCount = getChildCount();
        int maxVisibleIcons = this.mDark ? 5 : childCount;
        float layoutEnd2 = getLayoutEnd();
        float overflowStart2 = layoutEnd2 - (((float) this.mIconSize) * 2.0f);
        int i2 = -1;
        boolean hasAmbient2 = this.mSpeedBumpIndex != -1 && this.mSpeedBumpIndex < getChildCount();
        float visualOverflowStart2 = 0.0f;
        int firstOverflowIndex2 = -1;
        float translationX2 = translationX;
        int i3 = 0;
        while (i3 < childCount) {
            View view = getChildAt(i3);
            IconState iconState = this.mIconStates.get(view);
            iconState.xTranslation = translationX2;
            boolean forceOverflow = (this.mSpeedBumpIndex != i2 && i3 >= this.mSpeedBumpIndex && iconState.iconAppearAmount > 0.0f) || i3 >= maxVisibleIcons;
            boolean noOverflowAfter = i3 == childCount + -1;
            if (!this.mDark || !(view instanceof StatusBarIconView)) {
                drawingScale = 1.0f;
            } else {
                drawingScale = ((StatusBarIconView) view).getIconScaleFullyDark();
            }
            int maxVisibleIcons2 = maxVisibleIcons;
            if (this.mOpenedAmount != 0.0f) {
                noOverflowAfter = noOverflowAfter && !hasAmbient2 && !forceOverflow;
            }
            iconState.visibleState = 0;
            if (firstOverflowIndex2 == -1) {
                if (!forceOverflow) {
                    if (translationX2 < (noOverflowAfter ? layoutEnd2 - ((float) this.mIconSize) : overflowStart2)) {
                        layoutEnd = layoutEnd2;
                        overflowStart = overflowStart2;
                        hasAmbient = hasAmbient2;
                    }
                }
                int firstOverflowIndex3 = (!noOverflowAfter || forceOverflow) ? i3 : i3 - 1;
                int totalDotLength = (this.mStaticDotRadius * 6) + (this.mDotPadding * 2);
                hasAmbient = hasAmbient2;
                float visualOverflowStart3 = ((((((float) this.mIconSize) * 1.0f) + overflowStart2) - ((float) (totalDotLength / 2))) - (((float) this.mIconSize) * 0.5f)) + ((float) this.mStaticDotRadius);
                if (forceOverflow) {
                    visualOverflowStart = Math.min(translationX2, ((float) (this.mStaticDotRadius * 2)) + visualOverflowStart3 + ((float) this.mDotPadding));
                    overflowStart = overflowStart2;
                } else {
                    overflowStart = overflowStart2;
                    visualOverflowStart = (((translationX2 - overflowStart2) / ((float) this.mIconSize)) * ((float) ((this.mStaticDotRadius * 2) + this.mDotPadding))) + visualOverflowStart3;
                }
                if (this.mShowAllIcons) {
                    this.mVisualOverflowAdaption = 0.0f;
                    if (firstOverflowIndex3 != -1) {
                        IconState overflowState = this.mIconStates.get(getChildAt(i3));
                        firstOverflowIndex = firstOverflowIndex3;
                        layoutEnd = layoutEnd2;
                        float visualOverflowStart4 = (((overflowState.xTranslation + ((layoutEnd2 - overflowState.xTranslation) / 2.0f)) - ((float) (totalDotLength / 2))) - (((float) this.mIconSize) * 0.5f)) + ((float) this.mStaticDotRadius);
                        this.mVisualOverflowAdaption = visualOverflowStart4 - visualOverflowStart;
                        visualOverflowStart2 = visualOverflowStart4;
                    } else {
                        layoutEnd = layoutEnd2;
                        visualOverflowStart2 = visualOverflowStart;
                        firstOverflowIndex2 = firstOverflowIndex3;
                    }
                } else {
                    firstOverflowIndex = firstOverflowIndex3;
                    layoutEnd = layoutEnd2;
                    visualOverflowStart2 = visualOverflowStart + (this.mVisualOverflowAdaption * (1.0f - this.mOpenedAmount));
                }
                firstOverflowIndex2 = firstOverflowIndex;
            } else {
                layoutEnd = layoutEnd2;
                overflowStart = overflowStart2;
                hasAmbient = hasAmbient2;
            }
            translationX2 += iconState.iconAppearAmount * ((float) view.getWidth()) * drawingScale;
            i3++;
            maxVisibleIcons = maxVisibleIcons2;
            hasAmbient2 = hasAmbient;
            overflowStart2 = overflowStart;
            layoutEnd2 = layoutEnd;
            i2 = -1;
        }
        float f = layoutEnd2;
        float f2 = overflowStart2;
        boolean z = hasAmbient2;
        if (firstOverflowIndex2 != -1) {
            float translationX3 = visualOverflowStart2;
            int numDots = 1;
            for (int i4 = firstOverflowIndex2; i4 < childCount; i4++) {
                IconState iconState2 = this.mIconStates.get(getChildAt(i4));
                int dotWidth = (this.mStaticDotRadius * 2) + this.mDotPadding;
                iconState2.xTranslation = translationX3;
                if (numDots <= 3) {
                    if (numDots != 1 || iconState2.iconAppearAmount >= 0.8f) {
                        iconState2.visibleState = 1;
                    } else {
                        iconState2.visibleState = 0;
                        numDots--;
                    }
                    translationX3 += ((float) (numDots == 3 ? 3 * dotWidth : dotWidth)) * iconState2.iconAppearAmount;
                } else {
                    iconState2.visibleState = 2;
                }
                numDots++;
            }
            i = 0;
            translationX2 = translationX3;
        } else {
            i = 0;
        }
        if (this.mDark && translationX2 < getLayoutEnd()) {
            float delta = (getLayoutEnd() - translationX2) / 2.0f;
            if (firstOverflowIndex2 != -1) {
                delta = (((getLayoutEnd() - visualOverflowStart2) / 2.0f) + delta) / 2.0f;
            }
            for (int i5 = i; i5 < childCount; i5++) {
                this.mIconStates.get(getChildAt(i5)).xTranslation += delta;
            }
        }
        if (isLayoutRtl()) {
            while (true) {
                int i6 = i;
                if (i6 < childCount) {
                    View view2 = getChildAt(i6);
                    IconState iconState3 = this.mIconStates.get(view2);
                    iconState3.xTranslation = (((float) getWidth()) - iconState3.xTranslation) - ((float) view2.getWidth());
                    i = i6 + 1;
                } else {
                    return;
                }
            }
        }
    }

    private float getLayoutEnd() {
        return ((float) getActualWidth()) - getActualPaddingEnd();
    }

    private float getActualPaddingEnd() {
        if (this.mActualPaddingEnd == -2.14748365E9f) {
            return (float) getPaddingEnd();
        }
        return this.mActualPaddingEnd;
    }

    private float getActualPaddingStart() {
        if (this.mActualPaddingStart == -2.14748365E9f) {
            return (float) getPaddingStart();
        }
        return this.mActualPaddingStart;
    }

    public void setShowAllIcons(boolean showAllIcons) {
        this.mShowAllIcons = showAllIcons;
    }

    public void setActualLayoutWidth(int actualLayoutWidth) {
        this.mActualLayoutWidth = actualLayoutWidth;
    }

    public void setActualPaddingEnd(float paddingEnd) {
        this.mActualPaddingEnd = paddingEnd;
    }

    public void setActualPaddingStart(float paddingStart) {
        this.mActualPaddingStart = paddingStart;
    }

    public int getActualWidth() {
        if (this.mActualLayoutWidth == Integer.MIN_VALUE) {
            return getWidth();
        }
        return this.mActualLayoutWidth;
    }

    public void setChangingViewPositions(boolean changingViewPositions) {
        this.mChangingViewPositions = changingViewPositions;
    }

    public void setDark(boolean dark, boolean fade, long delay) {
        this.mDark = dark;
        this.mDisallowNextAnimation |= !fade;
        for (int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            if (view instanceof StatusBarIconView) {
                ((StatusBarIconView) view).setDark(dark, fade, delay);
                if (!dark && fade) {
                    getIconState((StatusBarIconView) view).justUndarkened = true;
                }
            }
        }
    }

    public IconState getIconState(StatusBarIconView icon) {
        return this.mIconStates.get(icon);
    }

    public void setSpeedBumpIndex(int speedBumpIndex) {
        this.mSpeedBumpIndex = speedBumpIndex;
    }

    public void setOpenedAmount(float expandAmount) {
        this.mOpenedAmount = expandAmount;
    }

    public float getVisualOverflowAdaption() {
        return this.mVisualOverflowAdaption;
    }

    public void setVisualOverflowAdaption(float visualOverflowAdaption) {
        this.mVisualOverflowAdaption = visualOverflowAdaption;
    }

    public boolean hasOverflow() {
        return ((((float) getChildCount()) + 0.0f) * ((float) this.mIconSize)) - ((((float) getWidth()) - getActualPaddingStart()) - getActualPaddingEnd()) > 0.0f;
    }

    public int getIconSize() {
        return this.mIconSize;
    }

    public void setAnimationsEnabled(boolean enabled) {
        if (!enabled && this.mAnimationsEnabled) {
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                ViewState childState = this.mIconStates.get(child);
                if (childState != null) {
                    childState.cancelAnimations(child);
                    childState.applyToView(child);
                }
            }
        }
        this.mAnimationsEnabled = enabled;
    }

    public void setReplacingIcons(ArrayMap<String, ArrayList<StatusBarIcon>> replacingIcons) {
        this.mReplacingIcons = replacingIcons;
    }
}
