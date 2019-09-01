package com.android.systemui.statusbar;

import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.android.systemui.statusbar.stack.ExpandableViewState;
import com.android.systemui.statusbar.stack.StackScrollState;
import java.util.ArrayList;
import java.util.Iterator;

public abstract class ExpandableView extends FrameLayout {
    private static Rect mClipRect = new Rect();
    private int mActualHeight;
    private boolean mChangingPosition = false;
    protected int mClipBottomAmount;
    private boolean mClipToActualHeight = true;
    protected int mClipTopAmount;
    private boolean mDark;
    private boolean mInShelf;
    private ArrayList<View> mMatchParentViews = new ArrayList<>();
    private int mMinClipTopAmount = 0;
    protected OnHeightChangedListener mOnHeightChangedListener;
    private boolean mTransformingInShelf;
    private ViewGroup mTransientContainer;
    protected int mViewType;
    private boolean mWillBeGone;

    public interface OnHeightChangedListener {
        void onHeightChanged(ExpandableView expandableView, boolean z);

        void onReset(ExpandableView expandableView);
    }

    public abstract void performAddAnimation(long j, long j2, AnimatorListenerAdapter animatorListenerAdapter);

    public abstract void performRemoveAnimation(long j, float f, AnimatorListenerAdapter animatorListenerAdapter, Runnable runnable);

    public ExpandableView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setViewType(int viewType) {
        this.mViewType = viewType;
    }

    public int getViewType() {
        return this.mViewType;
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int i;
        int i2 = widthMeasureSpec;
        int givenSize = View.MeasureSpec.getSize(heightMeasureSpec);
        int ownMaxHeight = Integer.MAX_VALUE;
        int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);
        if (!(heightMode == 0 || givenSize == 0)) {
            ownMaxHeight = Math.min(givenSize, Integer.MAX_VALUE);
        }
        int newHeightSpec = View.MeasureSpec.makeMeasureSpec(ownMaxHeight, Integer.MIN_VALUE);
        int childCount = getChildCount();
        int maxChildHeight = 0;
        for (int i3 = 0; i3 < childCount; i3++) {
            View child = getChildAt(i3);
            if (child.getVisibility() != 8) {
                int childHeightSpec = newHeightSpec;
                ViewGroup.LayoutParams layoutParams = child.getLayoutParams();
                if (layoutParams.height != -1) {
                    if (layoutParams.height >= 0) {
                        if (layoutParams.height > ownMaxHeight) {
                            i = View.MeasureSpec.makeMeasureSpec(ownMaxHeight, 1073741824);
                        } else {
                            i = View.MeasureSpec.makeMeasureSpec(layoutParams.height, 1073741824);
                        }
                        childHeightSpec = i;
                    }
                    child.measure(getChildMeasureSpec(i2, 0, layoutParams.width), childHeightSpec);
                    maxChildHeight = Math.max(maxChildHeight, child.getMeasuredHeight());
                } else {
                    this.mMatchParentViews.add(child);
                }
            }
        }
        int ownHeight = heightMode == 1073741824 ? givenSize : Math.min(ownMaxHeight, maxChildHeight);
        int newHeightSpec2 = View.MeasureSpec.makeMeasureSpec(ownHeight, 1073741824);
        Iterator<View> it = this.mMatchParentViews.iterator();
        while (it.hasNext()) {
            View child2 = it.next();
            child2.measure(getChildMeasureSpec(i2, 0, child2.getLayoutParams().width), newHeightSpec2);
        }
        this.mMatchParentViews.clear();
        setMeasuredDimension(View.MeasureSpec.getSize(widthMeasureSpec), ownHeight);
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        updateClipping();
    }

    public boolean pointInView(float localX, float localY, float slop) {
        return localX >= (-slop) && localY >= ((float) this.mClipTopAmount) - slop && localX < ((float) (this.mRight - this.mLeft)) + slop && localY < ((float) this.mActualHeight) + slop;
    }

    public void setActualHeight(int actualHeight, boolean notifyListeners) {
        this.mActualHeight = actualHeight;
        updateClipping();
        if (notifyListeners) {
            notifyHeightChanged(false);
        }
    }

    public void setActualHeight(int actualHeight) {
        setActualHeight(actualHeight, true);
    }

    public int getActualHeight() {
        return this.mActualHeight;
    }

    public int getMaxContentHeight() {
        return getHeight();
    }

    public int getMinHeight() {
        return getHeight();
    }

    public int getCollapsedHeight() {
        return getHeight();
    }

    public void setDimmed(boolean dimmed, boolean fade) {
    }

    public void setDark(boolean dark, boolean fade, long delay) {
        this.mDark = dark;
    }

    public boolean isDark() {
        return this.mDark;
    }

    public void setHideSensitiveForIntrinsicHeight(boolean hideSensitive) {
    }

    public void setHideSensitive(boolean hideSensitive, boolean animated, long delay, long duration) {
    }

    public int getIntrinsicHeight() {
        return getHeight();
    }

    public void setClipTopAmount(int clipTopAmount) {
        this.mClipTopAmount = clipTopAmount;
        updateClipping();
    }

    public void setClipBottomAmount(int clipBottomAmount) {
        this.mClipBottomAmount = clipBottomAmount;
        updateClipping();
    }

    public int getClipTopAmount() {
        return this.mClipTopAmount;
    }

    public int getClipBottomAmount() {
        return this.mClipBottomAmount;
    }

    public void setOnHeightChangedListener(OnHeightChangedListener listener) {
        this.mOnHeightChangedListener = listener;
    }

    public boolean isContentExpandable() {
        return false;
    }

    public void notifyHeightChanged(boolean needsAnimation) {
        if (this.mOnHeightChangedListener != null) {
            this.mOnHeightChangedListener.onHeightChanged(this, needsAnimation);
        }
    }

    public boolean isTransparent() {
        return false;
    }

    public void setBelowSpeedBump(boolean below) {
    }

    public int getPinnedHeadsUpHeight() {
        return getIntrinsicHeight();
    }

    public void setTranslation(float translation) {
        setTranslationX(translation);
    }

    public float getTranslation() {
        return getTranslationX();
    }

    public void onHeightReset() {
        if (this.mOnHeightChangedListener != null) {
            this.mOnHeightChangedListener.onReset(this);
        }
    }

    public void getDrawingRect(Rect outRect) {
        super.getDrawingRect(outRect);
        outRect.left = (int) (((float) outRect.left) + getTranslationX());
        outRect.right = (int) (((float) outRect.right) + getTranslationX());
        outRect.bottom = (int) (((float) outRect.top) + getTranslationY() + ((float) getActualHeight()));
        outRect.top = (int) (((float) outRect.top) + getTranslationY() + ((float) getClipTopAmount()));
    }

    public void getBoundsOnScreen(Rect outRect, boolean clipToParent) {
        super.getBoundsOnScreen(outRect, clipToParent);
        if (((float) getTop()) + getTranslationY() < 0.0f) {
            outRect.top = (int) (((float) outRect.top) + ((float) getTop()) + getTranslationY());
        }
        outRect.bottom = outRect.top + getActualHeight();
        outRect.top += getClipTopAmount();
    }

    public boolean isSummaryWithChildren() {
        return false;
    }

    public boolean areChildrenExpanded() {
        return false;
    }

    /* access modifiers changed from: protected */
    public void updateClipping() {
        if (this.mClipToActualHeight) {
            int left = (int) getTranslation();
            int right = getWidth() + left + getExtraClipRightAmount();
            int top = getClipTopAmount();
            mClipRect.set(left, top, right, Math.max((getActualHeight() + getExtraBottomPadding()) - this.mClipBottomAmount, top));
            setClipBounds(mClipRect);
            return;
        }
        setClipBounds(null);
    }

    /* access modifiers changed from: protected */
    public int getExtraClipRightAmount() {
        return 0;
    }

    public void setClipToActualHeight(boolean clipToActualHeight) {
        this.mClipToActualHeight = clipToActualHeight;
        updateClipping();
    }

    public boolean willBeGone() {
        return this.mWillBeGone;
    }

    public void setWillBeGone(boolean willBeGone) {
        this.mWillBeGone = willBeGone;
    }

    public void setMinClipTopAmount(int minClipTopAmount) {
        this.mMinClipTopAmount = minClipTopAmount;
    }

    public void setLayerType(int layerType, Paint paint) {
        if (hasOverlappingRendering()) {
            super.setLayerType(layerType, paint);
        }
    }

    public boolean hasOverlappingRendering() {
        return super.hasOverlappingRendering() && getActualHeight() <= getHeight();
    }

    public float getShadowAlpha() {
        return 0.0f;
    }

    public void setShadowAlpha(float shadowAlpha) {
    }

    public float getIncreasedPaddingAmount() {
        return 0.0f;
    }

    public boolean mustStayOnScreen() {
        return false;
    }

    public void setFakeShadowIntensity(float shadowIntensity, float outlineAlpha, int shadowYEnd, int outlineTranslation) {
    }

    public float getOutlineAlpha() {
        return 0.0f;
    }

    public int getOutlineTranslation() {
        return 0;
    }

    public void setChangingPosition(boolean changingPosition) {
        this.mChangingPosition = changingPosition;
    }

    public boolean isChangingPosition() {
        return this.mChangingPosition;
    }

    public void setTransientContainer(ViewGroup transientContainer) {
        this.mTransientContainer = transientContainer;
    }

    public ViewGroup getTransientContainer() {
        return this.mTransientContainer;
    }

    public int getExtraBottomPadding() {
        return 0;
    }

    public boolean isGroupExpansionChanging() {
        return false;
    }

    public boolean isGroupExpanded() {
        return false;
    }

    public boolean isChildInGroup() {
        return false;
    }

    public void setActualHeightAnimating(boolean animating) {
    }

    public ExpandableViewState createNewViewState(StackScrollState stackScrollState) {
        return new ExpandableViewState();
    }

    public boolean hasNoContentHeight() {
        return false;
    }

    public void setInShelf(boolean inShelf) {
        this.mInShelf = inShelf;
    }

    public boolean isInShelf() {
        return this.mInShelf;
    }

    public void setTransformingInShelf(boolean transformingInShelf) {
        this.mTransformingInShelf = transformingInShelf;
    }

    public boolean isTransformingIntoShelf() {
        return this.mTransformingInShelf;
    }

    public boolean isAboveShelf() {
        return false;
    }
}
