package com.android.systemui.statusbar;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

public final class KeyboardShortcutKeysLayout extends ViewGroup {
    private final Context mContext;
    private int mLineHeight;

    public static class LayoutParams extends ViewGroup.LayoutParams {
        public final int mHorizontalSpacing;
        public final int mVerticalSpacing;

        public LayoutParams(int horizontalSpacing, int verticalSpacing, ViewGroup.LayoutParams viewGroupLayout) {
            super(viewGroupLayout);
            this.mHorizontalSpacing = horizontalSpacing;
            this.mVerticalSpacing = verticalSpacing;
        }

        public LayoutParams(int mHorizontalSpacing2, int verticalSpacing) {
            super(0, 0);
            this.mHorizontalSpacing = mHorizontalSpacing2;
            this.mVerticalSpacing = verticalSpacing;
        }
    }

    public KeyboardShortcutKeysLayout(Context context) {
        super(context);
        this.mContext = context;
    }

    public KeyboardShortcutKeysLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int childHeightMeasureSpec;
        int width = (View.MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft()) - getPaddingRight();
        int childCount = getChildCount();
        int height = (View.MeasureSpec.getSize(heightMeasureSpec) - getPaddingTop()) - getPaddingBottom();
        int lineHeight = 0;
        int xPos = getPaddingLeft();
        int yPos = getPaddingTop();
        if (View.MeasureSpec.getMode(heightMeasureSpec) == Integer.MIN_VALUE) {
            childHeightMeasureSpec = View.MeasureSpec.makeMeasureSpec(height, Integer.MIN_VALUE);
        } else {
            childHeightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, 0);
        }
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != 8) {
                LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();
                child.measure(View.MeasureSpec.makeMeasureSpec(width, Integer.MIN_VALUE), childHeightMeasureSpec);
                int childWidth = child.getMeasuredWidth();
                lineHeight = Math.max(lineHeight, child.getMeasuredHeight() + layoutParams.mVerticalSpacing);
                if (xPos + childWidth > width) {
                    xPos = getPaddingLeft();
                    yPos += lineHeight;
                }
                xPos += layoutParams.mHorizontalSpacing + childWidth;
            }
        }
        this.mLineHeight = lineHeight;
        if (View.MeasureSpec.getMode(heightMeasureSpec) == 0) {
            height = yPos + lineHeight;
        } else if (View.MeasureSpec.getMode(heightMeasureSpec) == Integer.MIN_VALUE && yPos + lineHeight < height) {
            height = yPos + lineHeight;
        }
        setMeasuredDimension(width, height);
    }

    /* access modifiers changed from: protected */
    public LayoutParams generateDefaultLayoutParams() {
        int spacing = getHorizontalVerticalSpacing();
        return new LayoutParams(spacing, spacing);
    }

    /* access modifiers changed from: protected */
    public LayoutParams generateLayoutParams(ViewGroup.LayoutParams layoutParams) {
        int spacing = getHorizontalVerticalSpacing();
        return new LayoutParams(spacing, spacing, layoutParams);
    }

    /* access modifiers changed from: protected */
    public boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        int xPos;
        int i;
        LayoutParams lp;
        int i2;
        int i3;
        int childCount = getChildCount();
        int fullRowWidth = r - l;
        if (isRTL()) {
            xPos = fullRowWidth - getPaddingRight();
        } else {
            xPos = getPaddingLeft();
        }
        int xPos2 = xPos;
        int yPos = getPaddingTop();
        int lastHorizontalSpacing = 0;
        int rowStartIdx = 0;
        int xPos3 = 0;
        while (true) {
            int i4 = xPos3;
            if (i4 >= childCount) {
                break;
            }
            View currentChild = getChildAt(i4);
            if (currentChild.getVisibility() != 8) {
                int currentChildWidth = currentChild.getMeasuredWidth();
                LayoutParams lp2 = (LayoutParams) currentChild.getLayoutParams();
                boolean childDoesNotFitOnRow = true;
                if (!isRTL() ? xPos2 + currentChildWidth <= fullRowWidth : (xPos2 - getPaddingLeft()) - currentChildWidth >= 0) {
                    childDoesNotFitOnRow = false;
                }
                if (childDoesNotFitOnRow) {
                    lp = lp2;
                    View view = currentChild;
                    i = i4;
                    layoutChildrenOnRow(rowStartIdx, i4, fullRowWidth, xPos2, yPos, lastHorizontalSpacing);
                    if (isRTL()) {
                        i3 = fullRowWidth - getPaddingRight();
                    } else {
                        i3 = getPaddingLeft();
                    }
                    xPos2 = i3;
                    yPos += this.mLineHeight;
                    rowStartIdx = i;
                } else {
                    lp = lp2;
                    View view2 = currentChild;
                    i = i4;
                }
                if (isRTL()) {
                    i2 = (xPos2 - currentChildWidth) - lp.mHorizontalSpacing;
                } else {
                    i2 = xPos2 + currentChildWidth + lp.mHorizontalSpacing;
                }
                xPos2 = i2;
                lastHorizontalSpacing = lp.mHorizontalSpacing;
            } else {
                i = i4;
            }
            xPos3 = i + 1;
        }
        if (rowStartIdx < childCount) {
            layoutChildrenOnRow(rowStartIdx, childCount, fullRowWidth, xPos2, yPos, lastHorizontalSpacing);
        }
    }

    private int getHorizontalVerticalSpacing() {
        return (int) TypedValue.applyDimension(1, 4.0f, getResources().getDisplayMetrics());
    }

    private void layoutChildrenOnRow(int startIndex, int endIndex, int fullRowWidth, int xPos, int yPos, int lastHorizontalSpacing) {
        int nextChildWidth;
        if (!isRTL()) {
            xPos = ((getPaddingLeft() + fullRowWidth) - xPos) + lastHorizontalSpacing;
        }
        int xPos2 = xPos;
        for (int j = startIndex; j < endIndex; j++) {
            View currentChild = getChildAt(j);
            int currentChildWidth = currentChild.getMeasuredWidth();
            LayoutParams lp = (LayoutParams) currentChild.getLayoutParams();
            if (isRTL() && j == startIndex) {
                xPos2 = (((fullRowWidth - xPos2) - getPaddingRight()) - currentChildWidth) - lp.mHorizontalSpacing;
            }
            currentChild.layout(xPos2, yPos, xPos2 + currentChildWidth, currentChild.getMeasuredHeight() + yPos);
            if (isRTL()) {
                if (j < endIndex - 1) {
                    nextChildWidth = getChildAt(j + 1).getMeasuredWidth();
                } else {
                    nextChildWidth = 0;
                }
                xPos2 -= lp.mHorizontalSpacing + nextChildWidth;
            } else {
                xPos2 += lp.mHorizontalSpacing + currentChildWidth;
            }
        }
    }

    private boolean isRTL() {
        return this.mContext.getResources().getConfiguration().getLayoutDirection() == 1;
    }
}
