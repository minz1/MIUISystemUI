package android.support.v7.widget;

import android.content.Context;
import android.content.res.Configuration;
import android.support.v7.view.menu.ActionMenuItemView;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.view.menu.MenuItemImpl;
import android.support.v7.view.menu.MenuPresenter;
import android.support.v7.view.menu.MenuView;
import android.support.v7.widget.LinearLayoutCompat;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;

public class ActionMenuView extends LinearLayoutCompat implements MenuBuilder.ItemInvoker, MenuView {
    private MenuPresenter.Callback mActionMenuPresenterCallback;
    private boolean mFormatItems;
    private int mFormatItemsWidth;
    private int mGeneratedItemPadding;
    private MenuBuilder mMenu;
    MenuBuilder.Callback mMenuBuilderCallback;
    private int mMinCellSize;
    OnMenuItemClickListener mOnMenuItemClickListener;
    private Context mPopupContext;
    private int mPopupTheme;
    private ActionMenuPresenter mPresenter;
    private boolean mReserveOverflow;

    public interface ActionMenuChildView {
        boolean needsDividerAfter();

        boolean needsDividerBefore();
    }

    private static class ActionMenuPresenterCallback implements MenuPresenter.Callback {
        ActionMenuPresenterCallback() {
        }

        public void onCloseMenu(MenuBuilder menu, boolean allMenusAreClosing) {
        }

        public boolean onOpenSubMenu(MenuBuilder subMenu) {
            return false;
        }
    }

    public static class LayoutParams extends LinearLayoutCompat.LayoutParams {
        @ViewDebug.ExportedProperty
        public int cellsUsed;
        @ViewDebug.ExportedProperty
        public boolean expandable;
        boolean expanded;
        @ViewDebug.ExportedProperty
        public int extraPixels;
        @ViewDebug.ExportedProperty
        public boolean isOverflowButton;
        @ViewDebug.ExportedProperty
        public boolean preventEdgeOffset;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(ViewGroup.LayoutParams other) {
            super(other);
        }

        public LayoutParams(LayoutParams other) {
            super(other);
            this.isOverflowButton = other.isOverflowButton;
        }

        public LayoutParams(int width, int height) {
            super(width, height);
            this.isOverflowButton = false;
        }
    }

    private class MenuBuilderCallback implements MenuBuilder.Callback {
        MenuBuilderCallback() {
        }

        public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {
            return ActionMenuView.this.mOnMenuItemClickListener != null && ActionMenuView.this.mOnMenuItemClickListener.onMenuItemClick(item);
        }

        public void onMenuModeChange(MenuBuilder menu) {
            if (ActionMenuView.this.mMenuBuilderCallback != null) {
                ActionMenuView.this.mMenuBuilderCallback.onMenuModeChange(menu);
            }
        }
    }

    public interface OnMenuItemClickListener {
        boolean onMenuItemClick(MenuItem menuItem);
    }

    public ActionMenuView(Context context) {
        this(context, null);
    }

    public ActionMenuView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setBaselineAligned(false);
        float density = context.getResources().getDisplayMetrics().density;
        this.mMinCellSize = (int) (56.0f * density);
        this.mGeneratedItemPadding = (int) (4.0f * density);
        this.mPopupContext = context;
        this.mPopupTheme = 0;
    }

    public void setPopupTheme(int resId) {
        if (this.mPopupTheme != resId) {
            this.mPopupTheme = resId;
            if (resId == 0) {
                this.mPopupContext = getContext();
            } else {
                this.mPopupContext = new ContextThemeWrapper(getContext(), resId);
            }
        }
    }

    public void setPresenter(ActionMenuPresenter presenter) {
        this.mPresenter = presenter;
        this.mPresenter.setMenuView(this);
    }

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (this.mPresenter != null) {
            this.mPresenter.updateMenuView(false);
            if (this.mPresenter.isOverflowMenuShowing()) {
                this.mPresenter.hideOverflowMenu();
                this.mPresenter.showOverflowMenu();
            }
        }
    }

    public void setOnMenuItemClickListener(OnMenuItemClickListener listener) {
        this.mOnMenuItemClickListener = listener;
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        boolean wasFormatted = this.mFormatItems;
        this.mFormatItems = View.MeasureSpec.getMode(widthMeasureSpec) == 1073741824;
        if (wasFormatted != this.mFormatItems) {
            this.mFormatItemsWidth = 0;
        }
        int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
        if (!(!this.mFormatItems || this.mMenu == null || widthSize == this.mFormatItemsWidth)) {
            this.mFormatItemsWidth = widthSize;
            this.mMenu.onItemsChanged(true);
        }
        int childCount = getChildCount();
        if (!this.mFormatItems || childCount <= 0) {
            for (int i = 0; i < childCount; i++) {
                LayoutParams lp = (LayoutParams) getChildAt(i).getLayoutParams();
                lp.rightMargin = 0;
                lp.leftMargin = 0;
            }
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        onMeasureExactFormat(widthMeasureSpec, heightMeasureSpec);
    }

    private void onMeasureExactFormat(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize;
        boolean needsExpansion;
        int heightSize;
        int childCount;
        int heightPadding;
        int cellSizeRemaining;
        int visibleItemCount;
        boolean z;
        int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);
        int widthSize2 = View.MeasureSpec.getSize(widthMeasureSpec);
        int heightSize2 = View.MeasureSpec.getSize(heightMeasureSpec);
        int widthPadding = getPaddingLeft() + getPaddingRight();
        int heightPadding2 = getPaddingTop() + getPaddingBottom();
        int itemHeightSpec = getChildMeasureSpec(heightMeasureSpec, heightPadding2, -2);
        int widthSize3 = widthSize2 - widthPadding;
        int cellCount = widthSize3 / this.mMinCellSize;
        int cellSizeRemaining2 = widthSize3 % this.mMinCellSize;
        if (cellCount == 0) {
            setMeasuredDimension(widthSize3, 0);
            return;
        }
        int cellSize = this.mMinCellSize + (cellSizeRemaining2 / cellCount);
        boolean hasOverflow = false;
        long smallestItemsAt = 0;
        int childCount2 = getChildCount();
        int heightSize3 = heightSize2;
        int maxChildHeight = 0;
        int visibleItemCount2 = 0;
        int expandableItemCount = 0;
        int maxCellsUsed = 0;
        int cellsRemaining = cellCount;
        int i = 0;
        while (true) {
            int widthPadding2 = widthPadding;
            if (i >= childCount2) {
                break;
            }
            View child = getChildAt(i);
            int cellCount2 = cellCount;
            if (child.getVisibility() == 8) {
                heightPadding = heightPadding2;
                cellSizeRemaining = cellSizeRemaining2;
            } else {
                boolean isGeneratedItem = child instanceof ActionMenuItemView;
                int visibleItemCount3 = visibleItemCount2 + 1;
                if (isGeneratedItem) {
                    cellSizeRemaining = cellSizeRemaining2;
                    visibleItemCount = visibleItemCount3;
                    z = false;
                    child.setPadding(this.mGeneratedItemPadding, 0, this.mGeneratedItemPadding, 0);
                } else {
                    cellSizeRemaining = cellSizeRemaining2;
                    visibleItemCount = visibleItemCount3;
                    z = false;
                }
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                lp.expanded = z;
                lp.extraPixels = z ? 1 : 0;
                lp.cellsUsed = z;
                lp.expandable = z;
                lp.leftMargin = z;
                lp.rightMargin = z;
                lp.preventEdgeOffset = isGeneratedItem && ((ActionMenuItemView) child).hasText();
                int cellsUsed = measureChildForCells(child, cellSize, lp.isOverflowButton ? 1 : cellsRemaining, itemHeightSpec, heightPadding2);
                maxCellsUsed = Math.max(maxCellsUsed, cellsUsed);
                heightPadding = heightPadding2;
                if (lp.expandable != 0) {
                    expandableItemCount++;
                }
                if (lp.isOverflowButton) {
                    hasOverflow = true;
                }
                cellsRemaining -= cellsUsed;
                maxChildHeight = Math.max(maxChildHeight, child.getMeasuredHeight());
                if (cellsUsed == 1) {
                    View view = child;
                    smallestItemsAt |= (long) (1 << i);
                    visibleItemCount2 = visibleItemCount;
                    maxChildHeight = maxChildHeight;
                } else {
                    visibleItemCount2 = visibleItemCount;
                }
            }
            i++;
            widthPadding = widthPadding2;
            cellCount = cellCount2;
            cellSizeRemaining2 = cellSizeRemaining;
            heightPadding2 = heightPadding;
            int i2 = heightMeasureSpec;
        }
        int i3 = cellCount;
        int i4 = cellSizeRemaining2;
        boolean centerSingleExpandedItem = hasOverflow && visibleItemCount2 == 2;
        boolean needsExpansion2 = false;
        while (true) {
            if (expandableItemCount <= 0 || cellsRemaining <= 0) {
                widthSize = widthSize3;
                boolean z2 = centerSingleExpandedItem;
                needsExpansion = needsExpansion2;
            } else {
                long minCellsAt = 0;
                int minCells = Integer.MAX_VALUE;
                int minCellsItemCount = 0;
                int minCells2 = 0;
                while (true) {
                    int i5 = minCells2;
                    if (i5 >= childCount2) {
                        break;
                    }
                    View child2 = getChildAt(i5);
                    boolean needsExpansion3 = needsExpansion2;
                    LayoutParams lp2 = (LayoutParams) child2.getLayoutParams();
                    View view2 = child2;
                    if (lp2.expandable) {
                        if (lp2.cellsUsed < minCells) {
                            minCells = lp2.cellsUsed;
                            minCellsAt = 1 << i5;
                            minCellsItemCount = 1;
                        } else if (lp2.cellsUsed == minCells) {
                            minCellsAt |= 1 << i5;
                            minCellsItemCount++;
                        }
                    }
                    minCells2 = i5 + 1;
                    needsExpansion2 = needsExpansion3;
                }
                needsExpansion = needsExpansion2;
                smallestItemsAt |= minCellsAt;
                if (minCellsItemCount > cellsRemaining) {
                    widthSize = widthSize3;
                    boolean z3 = centerSingleExpandedItem;
                    break;
                }
                int minCells3 = minCells + 1;
                int i6 = 0;
                while (i6 < childCount2) {
                    View child3 = getChildAt(i6);
                    LayoutParams lp3 = (LayoutParams) child3.getLayoutParams();
                    int widthSize4 = widthSize3;
                    int minCellsItemCount2 = minCellsItemCount;
                    boolean centerSingleExpandedItem2 = centerSingleExpandedItem;
                    if ((minCellsAt & ((long) (1 << i6))) != 0) {
                        if (centerSingleExpandedItem2 && lp3.preventEdgeOffset && cellsRemaining == 1) {
                            child3.setPadding(this.mGeneratedItemPadding + cellSize, 0, this.mGeneratedItemPadding, 0);
                        }
                        lp3.cellsUsed++;
                        lp3.expanded = true;
                        cellsRemaining--;
                    } else if (lp3.cellsUsed == minCells3) {
                        smallestItemsAt |= (long) (1 << i6);
                    }
                    i6++;
                    minCellsItemCount = minCellsItemCount2;
                    widthSize3 = widthSize4;
                    centerSingleExpandedItem = centerSingleExpandedItem2;
                }
                int i7 = minCellsItemCount;
                boolean z4 = centerSingleExpandedItem;
                needsExpansion2 = true;
            }
        }
        long smallestItemsAt2 = smallestItemsAt;
        boolean singleItem = !hasOverflow && visibleItemCount2 == 1;
        if (cellsRemaining <= 0 || smallestItemsAt2 == 0) {
            int i8 = visibleItemCount2;
        } else if (cellsRemaining < visibleItemCount2 - 1 || singleItem || maxCellsUsed > 1) {
            float expandCount = (float) Long.bitCount(smallestItemsAt2);
            if (!singleItem) {
                if ((1 & smallestItemsAt2) != 0 && !((LayoutParams) getChildAt(0).getLayoutParams()).preventEdgeOffset) {
                    expandCount -= 0.5f;
                }
                childCount = childCount2;
                if ((smallestItemsAt2 & ((long) (1 << (childCount2 - 1)))) != 0 && !((LayoutParams) getChildAt(childCount - 1).getLayoutParams()).preventEdgeOffset) {
                    expandCount -= 0.5f;
                }
            } else {
                childCount = childCount2;
            }
            int extraPixels = expandCount > 0.0f ? (int) (((float) (cellsRemaining * cellSize)) / expandCount) : 0;
            int i9 = 0;
            while (true) {
                childCount2 = childCount;
                if (i9 >= childCount2) {
                    break;
                }
                boolean singleItem2 = singleItem;
                int visibleItemCount4 = visibleItemCount2;
                if ((((long) (1 << i9)) & smallestItemsAt2) != 0) {
                    View child4 = getChildAt(i9);
                    LayoutParams lp4 = (LayoutParams) child4.getLayoutParams();
                    if (child4 instanceof ActionMenuItemView) {
                        lp4.extraPixels = extraPixels;
                        lp4.expanded = true;
                        if (i9 == 0 && !lp4.preventEdgeOffset) {
                            lp4.leftMargin = (-extraPixels) / 2;
                        }
                        needsExpansion = true;
                    } else {
                        if (lp4.isOverflowButton) {
                            lp4.extraPixels = extraPixels;
                            lp4.expanded = true;
                            lp4.rightMargin = (-extraPixels) / 2;
                            needsExpansion = true;
                        } else {
                            if (i9 != 0) {
                                lp4.leftMargin = extraPixels / 2;
                            }
                            if (i9 != childCount2 - 1) {
                                lp4.rightMargin = extraPixels / 2;
                            }
                        }
                        i9++;
                        childCount = childCount2;
                        singleItem = singleItem2;
                        visibleItemCount2 = visibleItemCount4;
                    }
                }
                i9++;
                childCount = childCount2;
                singleItem = singleItem2;
                visibleItemCount2 = visibleItemCount4;
            }
            int i10 = visibleItemCount2;
        } else {
            boolean z5 = singleItem;
            int i11 = visibleItemCount2;
        }
        if (needsExpansion) {
            int i12 = 0;
            while (true) {
                int i13 = i12;
                if (i13 >= childCount2) {
                    break;
                }
                View child5 = getChildAt(i13);
                LayoutParams lp5 = (LayoutParams) child5.getLayoutParams();
                if (lp5.expanded) {
                    child5.measure(View.MeasureSpec.makeMeasureSpec((lp5.cellsUsed * cellSize) + lp5.extraPixels, 1073741824), itemHeightSpec);
                }
                i12 = i13 + 1;
            }
        }
        if (heightMode != 1073741824) {
            heightSize = maxChildHeight;
        } else {
            heightSize = heightSize3;
        }
        setMeasuredDimension(widthSize, heightSize);
    }

    static int measureChildForCells(View child, int cellSize, int cellsRemaining, int parentHeightMeasureSpec, int parentHeightPadding) {
        View view = child;
        int i = cellsRemaining;
        LayoutParams lp = (LayoutParams) view.getLayoutParams();
        int childHeightSpec = View.MeasureSpec.makeMeasureSpec(View.MeasureSpec.getSize(parentHeightMeasureSpec) - parentHeightPadding, View.MeasureSpec.getMode(parentHeightMeasureSpec));
        ActionMenuItemView itemView = view instanceof ActionMenuItemView ? (ActionMenuItemView) view : null;
        boolean expandable = false;
        boolean hasText = itemView != null && itemView.hasText();
        int cellsUsed = 0;
        if (i > 0 && (!hasText || i >= 2)) {
            view.measure(View.MeasureSpec.makeMeasureSpec(cellSize * i, Integer.MIN_VALUE), childHeightSpec);
            int measuredWidth = view.getMeasuredWidth();
            cellsUsed = measuredWidth / cellSize;
            if (measuredWidth % cellSize != 0) {
                cellsUsed++;
            }
            if (hasText && cellsUsed < 2) {
                cellsUsed = 2;
            }
        }
        if (!lp.isOverflowButton && hasText) {
            expandable = true;
        }
        lp.expandable = expandable;
        lp.cellsUsed = cellsUsed;
        view.measure(View.MeasureSpec.makeMeasureSpec(cellsUsed * cellSize, 1073741824), childHeightSpec);
        return cellsUsed;
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int overflowWidth;
        int dividerWidth;
        int midVertical;
        boolean isLayoutRtl;
        int l;
        int r;
        if (!this.mFormatItems) {
            super.onLayout(changed, left, top, right, bottom);
            return;
        }
        int childCount = getChildCount();
        int midVertical2 = (bottom - top) / 2;
        int dividerWidth2 = getDividerWidth();
        int nonOverflowCount = 0;
        int widthRemaining = ((right - left) - getPaddingRight()) - getPaddingLeft();
        boolean hasOverflow = false;
        boolean isLayoutRtl2 = ViewUtils.isLayoutRtl(this);
        int widthRemaining2 = widthRemaining;
        int nonOverflowWidth = 0;
        int overflowWidth2 = 0;
        int i = 0;
        while (i < childCount) {
            View v = getChildAt(i);
            if (v.getVisibility() == 8) {
                midVertical = midVertical2;
                isLayoutRtl = isLayoutRtl2;
            } else {
                LayoutParams p = (LayoutParams) v.getLayoutParams();
                if (p.isOverflowButton) {
                    int overflowWidth3 = v.getMeasuredWidth();
                    if (hasSupportDividerBeforeChildAt(i) != 0) {
                        overflowWidth3 += dividerWidth2;
                    }
                    int height = v.getMeasuredHeight();
                    if (isLayoutRtl2) {
                        isLayoutRtl = isLayoutRtl2;
                        l = getPaddingLeft() + p.leftMargin;
                        r = l + overflowWidth3;
                    } else {
                        isLayoutRtl = isLayoutRtl2;
                        r = (getWidth() - getPaddingRight()) - p.rightMargin;
                        l = r - overflowWidth3;
                    }
                    int t = midVertical2 - (height / 2);
                    midVertical = midVertical2;
                    v.layout(l, t, r, t + height);
                    widthRemaining2 -= overflowWidth3;
                    hasOverflow = true;
                    overflowWidth2 = overflowWidth3;
                } else {
                    midVertical = midVertical2;
                    isLayoutRtl = isLayoutRtl2;
                    int size = v.getMeasuredWidth() + p.leftMargin + p.rightMargin;
                    nonOverflowWidth += size;
                    widthRemaining2 -= size;
                    if (hasSupportDividerBeforeChildAt(i)) {
                        nonOverflowWidth += dividerWidth2;
                    }
                    nonOverflowCount++;
                }
            }
            i++;
            isLayoutRtl2 = isLayoutRtl;
            midVertical2 = midVertical;
        }
        int midVertical3 = midVertical2;
        boolean isLayoutRtl3 = isLayoutRtl2;
        int i2 = 1;
        if (childCount != 1 || hasOverflow) {
            if (hasOverflow) {
                i2 = 0;
            }
            int spacerCount = nonOverflowCount - i2;
            int i3 = 0;
            int spacerSize = Math.max(0, spacerCount > 0 ? widthRemaining2 / spacerCount : 0);
            if (isLayoutRtl3) {
                int startRight = getWidth() - getPaddingRight();
                while (i3 < childCount) {
                    View v2 = getChildAt(i3);
                    LayoutParams lp = (LayoutParams) v2.getLayoutParams();
                    int spacerCount2 = spacerCount;
                    if (v2.getVisibility() == 8) {
                        dividerWidth = dividerWidth2;
                        overflowWidth = overflowWidth2;
                    } else if (lp.isOverflowButton != 0) {
                        dividerWidth = dividerWidth2;
                        overflowWidth = overflowWidth2;
                    } else {
                        int startRight2 = startRight - lp.rightMargin;
                        int width = v2.getMeasuredWidth();
                        int height2 = v2.getMeasuredHeight();
                        int t2 = midVertical3 - (height2 / 2);
                        dividerWidth = dividerWidth2;
                        overflowWidth = overflowWidth2;
                        v2.layout(startRight2 - width, t2, startRight2, t2 + height2);
                        startRight = startRight2 - ((lp.leftMargin + width) + spacerSize);
                    }
                    i3++;
                    spacerCount = spacerCount2;
                    dividerWidth2 = dividerWidth;
                    overflowWidth2 = overflowWidth;
                }
                int i4 = dividerWidth2;
                int i5 = overflowWidth2;
            } else {
                int i6 = dividerWidth2;
                int i7 = overflowWidth2;
                int startLeft = getPaddingLeft();
                while (i3 < childCount) {
                    View v3 = getChildAt(i3);
                    LayoutParams lp2 = (LayoutParams) v3.getLayoutParams();
                    if (v3.getVisibility() != 8 && !lp2.isOverflowButton) {
                        int startLeft2 = startLeft + lp2.leftMargin;
                        int width2 = v3.getMeasuredWidth();
                        int height3 = v3.getMeasuredHeight();
                        int t3 = midVertical3 - (height3 / 2);
                        v3.layout(startLeft2, t3, startLeft2 + width2, t3 + height3);
                        startLeft = startLeft2 + lp2.rightMargin + width2 + spacerSize;
                    }
                    i3++;
                }
            }
            return;
        }
        View v4 = getChildAt(0);
        int width3 = v4.getMeasuredWidth();
        int height4 = v4.getMeasuredHeight();
        int l2 = ((right - left) / 2) - (width3 / 2);
        int t4 = midVertical3 - (height4 / 2);
        int i8 = width3;
        v4.layout(l2, t4, l2 + width3, t4 + height4);
    }

    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        dismissPopupMenus();
    }

    public boolean isOverflowReserved() {
        return this.mReserveOverflow;
    }

    public void setOverflowReserved(boolean reserveOverflow) {
        this.mReserveOverflow = reserveOverflow;
    }

    /* access modifiers changed from: protected */
    public LayoutParams generateDefaultLayoutParams() {
        LayoutParams params = new LayoutParams(-2, -2);
        params.gravity = 16;
        return params;
    }

    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    /* access modifiers changed from: protected */
    public LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        if (p == null) {
            return generateDefaultLayoutParams();
        }
        LayoutParams result = p instanceof LayoutParams ? new LayoutParams((LayoutParams) p) : new LayoutParams(p);
        if (result.gravity <= 0) {
            result.gravity = 16;
        }
        return result;
    }

    /* access modifiers changed from: protected */
    public boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p != null && (p instanceof LayoutParams);
    }

    public LayoutParams generateOverflowButtonLayoutParams() {
        LayoutParams result = generateDefaultLayoutParams();
        result.isOverflowButton = true;
        return result;
    }

    public boolean invokeItem(MenuItemImpl item) {
        return this.mMenu.performItemAction(item, 0);
    }

    public void initialize(MenuBuilder menu) {
        this.mMenu = menu;
    }

    public Menu getMenu() {
        if (this.mMenu == null) {
            Context context = getContext();
            this.mMenu = new MenuBuilder(context);
            this.mMenu.setCallback(new MenuBuilderCallback());
            this.mPresenter = new ActionMenuPresenter(context);
            this.mPresenter.setReserveOverflow(true);
            this.mPresenter.setCallback(this.mActionMenuPresenterCallback != null ? this.mActionMenuPresenterCallback : new ActionMenuPresenterCallback());
            this.mMenu.addMenuPresenter(this.mPresenter, this.mPopupContext);
            this.mPresenter.setMenuView(this);
        }
        return this.mMenu;
    }

    public void setMenuCallbacks(MenuPresenter.Callback pcb, MenuBuilder.Callback mcb) {
        this.mActionMenuPresenterCallback = pcb;
        this.mMenuBuilderCallback = mcb;
    }

    public MenuBuilder peekMenu() {
        return this.mMenu;
    }

    public boolean showOverflowMenu() {
        return this.mPresenter != null && this.mPresenter.showOverflowMenu();
    }

    public boolean hideOverflowMenu() {
        return this.mPresenter != null && this.mPresenter.hideOverflowMenu();
    }

    public boolean isOverflowMenuShowing() {
        return this.mPresenter != null && this.mPresenter.isOverflowMenuShowing();
    }

    public boolean isOverflowMenuShowPending() {
        return this.mPresenter != null && this.mPresenter.isOverflowMenuShowPending();
    }

    public void dismissPopupMenus() {
        if (this.mPresenter != null) {
            this.mPresenter.dismissPopupMenus();
        }
    }

    /* access modifiers changed from: protected */
    public boolean hasSupportDividerBeforeChildAt(int childIndex) {
        if (childIndex == 0) {
            return false;
        }
        View childBefore = getChildAt(childIndex - 1);
        View child = getChildAt(childIndex);
        boolean result = false;
        if (childIndex < getChildCount() && (childBefore instanceof ActionMenuChildView)) {
            result = false | ((ActionMenuChildView) childBefore).needsDividerAfter();
        }
        if (childIndex > 0 && (child instanceof ActionMenuChildView)) {
            result |= ((ActionMenuChildView) child).needsDividerBefore();
        }
        return result;
    }

    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        return false;
    }

    public void setExpandedActionViewsExclusive(boolean exclusive) {
        this.mPresenter.setExpandedActionViewsExclusive(exclusive);
    }
}
