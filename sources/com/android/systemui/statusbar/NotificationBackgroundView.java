package com.android.systemui.statusbar;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.util.AttributeSet;
import android.view.NotificationHeaderView;
import android.view.View;
import com.android.systemui.R;
import com.android.systemui.SwipeHelper;
import com.android.systemui.Util;
import com.android.systemui.miui.statusbar.notification.NotificationUtil;
import com.android.systemui.statusbar.stack.StackScrollAlgorithm;
import java.util.ArrayList;
import java.util.List;

public class NotificationBackgroundView extends View {
    private int mActualHeight;
    private Drawable mBackground;
    private final Paint mBackgroundPaint = new Paint();
    private int mClipBottomAmount;
    private int mClipTopAmount;
    private ExpandableNotificationRow mContainingRow;
    private final Paint mCoverPaint = new Paint();
    private boolean mDrawBottomDivider;
    private boolean mDrawTopDivider;
    private final Paint mExpandedDividerPaint = new Paint();
    private final Paint mTransparentPaint = new Paint();
    private List<ExpandableNotificationRow> mVisibleRows = new ArrayList();

    public NotificationBackgroundView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mTransparentPaint.setColor(0);
        this.mTransparentPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        this.mTransparentPaint.setAntiAlias(true);
        updateResource();
    }

    public void updateResource() {
        this.mCoverPaint.setColor(getResources().getColor(R.color.notification_panel_transparent_cover));
        this.mBackgroundPaint.setColor(getResources().getColor(R.color.notification_expanded_bg_color_without_base));
        this.mExpandedDividerPaint.setColor(getResources().getColor(R.color.notification_expanded_bg_divider_color));
    }

    public void setContainingRow(ExpandableNotificationRow row) {
        this.mContainingRow = row;
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        int backgroundTop = this.mClipTopAmount;
        int backgroundBottom = this.mActualHeight - this.mClipBottomAmount;
        if (this.mBackground != null && backgroundBottom > this.mClipTopAmount) {
            this.mBackground.setBounds(0, backgroundTop, getWidth(), backgroundBottom);
            this.mBackground.draw(canvas);
        }
        drawDividerLine(canvas, backgroundTop, backgroundBottom);
        drawTransparentArea(canvas, backgroundTop, backgroundBottom);
    }

    private void drawDividerLine(Canvas canvas, int backgroundTop, int backgroundBottom) {
        int dividerHeight = getResources().getDimensionPixelOffset(R.dimen.notification_expanded_bg_divider_height);
        if (this.mDrawTopDivider) {
            canvas.drawRect(0.0f, (float) backgroundTop, (float) getWidth(), (float) (backgroundTop + dividerHeight), this.mExpandedDividerPaint);
        }
        if (this.mDrawBottomDivider) {
            canvas.drawRect(0.0f, (float) (backgroundBottom - dividerHeight), (float) getWidth(), (float) backgroundBottom, this.mExpandedDividerPaint);
        }
    }

    private void drawTransparentArea(Canvas canvas, int backgroundTop, int backgroundBottom) {
        int backgroundTop2;
        NotificationHeaderView headerView;
        int i;
        ExpandableView nextRow;
        if (Util.isDefaultTheme() && !this.mContainingRow.isDismissAllInProgress() && !NotificationUtil.isFoldAnimating()) {
            if (this.mContainingRow == null || !this.mContainingRow.isSummaryWithChildren() || !this.mContainingRow.isGroupExpanded()) {
                int i2 = backgroundBottom;
            } else {
                boolean isGroupExpansionChanging = this.mContainingRow.isGroupExpansionChanging();
                int backgroundBottom2 = ((backgroundBottom - this.mContainingRow.getChildrenContainer().getCollapsedButtonHeight()) - this.mContainingRow.getChildrenContainer().getExpandedBottomMargin()) - (this.mContainingRow.hasExtraBottomPadding() ? this.mContainingRow.getPaddingBottom() : 0);
                int parentTopPadding = this.mContainingRow.hasExtraTopPadding() ? this.mContainingRow.getPaddingTop() : 0;
                NotificationHeaderView headerView2 = this.mContainingRow.getChildrenContainer().getHeaderView();
                if (!NotificationUtil.showGoogleStyle() || headerView2 == null) {
                    backgroundTop2 = backgroundTop;
                } else {
                    backgroundTop2 = backgroundTop + headerView2.getMeasuredHeight();
                }
                this.mVisibleRows.clear();
                for (ExpandableNotificationRow row : this.mContainingRow.getNotificationChildren()) {
                    if (row.getVisibility() != 8 && row.getTranslationY() < ((float) backgroundBottom2)) {
                        this.mVisibleRows.add(row);
                    }
                }
                for (ExpandableNotificationRow row2 : this.mVisibleRows) {
                    int i3 = this.mVisibleRows.indexOf(row2);
                    ExpandableView nextRow2 = i3 + 1 < this.mVisibleRows.size() ? this.mVisibleRows.get(i3 + 1) : null;
                    boolean isGutsAnimating = row2.getGuts() != null && row2.getGuts().isAnimating();
                    boolean isExpansionChange = row2.isExpansionChanging();
                    if (row2.getTranslation() != 0.0f) {
                        float top = ((float) ((!this.mContainingRow.hasExtraTopPadding() || i3 != 0) ? parentTopPadding : 0)) + row2.getTranslationY();
                        float bottom = ((float) ((!this.mContainingRow.hasExtraTopPadding() || i3 != 0) ? 0 : parentTopPadding)) + top + ((float) (isGutsAnimating ? row2.getActualHeight() : row2.getIntrinsicHeight()));
                        if (row2.getTranslation() <= 0.0f) {
                            nextRow = nextRow2;
                            headerView = headerView2;
                            i = i3;
                            drawCoverAndBackground(canvas, row2, 0.0f, top, ((float) getWidth()) + row2.getTranslation(), bottom);
                        } else if (StackScrollAlgorithm.canChildBeDismissed(row2)) {
                            nextRow = nextRow2;
                            headerView = headerView2;
                            i = i3;
                            drawCoverAndBackground(canvas, row2, row2.getTranslation(), top, (float) getWidth(), bottom);
                        } else {
                            nextRow = nextRow2;
                            headerView = headerView2;
                            i = i3;
                            drawCover(canvas, 0.0f, top, row2.getTranslation(), bottom);
                        }
                    } else {
                        nextRow = nextRow2;
                        headerView = headerView2;
                        i = i3;
                    }
                    if (nextRow != null) {
                        float top2 = ((float) parentTopPadding) + row2.getTranslationY() + ((float) row2.getIntrinsicHeight());
                        float bottom2 = nextRow.getTranslationY();
                        if (!isGroupExpansionChanging && !isGutsAnimating && !isExpansionChange && top2 < bottom2) {
                            drawCover(canvas, 0.0f, top2, (float) getWidth(), bottom2);
                        }
                    }
                    if (i == 0) {
                        float top3 = (float) backgroundTop2;
                        float bottom3 = row2.getTranslationY();
                        if (!isGroupExpansionChanging && top3 < bottom3) {
                            drawCover(canvas, 0.0f, top3, (float) getWidth(), bottom3);
                        }
                    }
                    if (i == this.mVisibleRows.size() - 1) {
                        float top4 = ((float) parentTopPadding) + row2.getTranslationY() + ((float) row2.getIntrinsicHeight());
                        float bottom4 = (float) backgroundBottom2;
                        if (!isGroupExpansionChanging && !isGutsAnimating && !isExpansionChange && top4 < bottom4) {
                            float f = bottom4;
                            drawCover(canvas, 0.0f, top4, (float) getWidth(), bottom4);
                        }
                    }
                    headerView2 = headerView;
                }
            }
        }
    }

    private void drawCoverAndBackground(Canvas canvas, ExpandableNotificationRow row, float left, float top, float right, float bottom) {
        drawCover(canvas, 0.0f, top, (float) getWidth(), bottom);
        int alpha = 255;
        if (getResources().getBoolean(R.bool.notification_swipe_background_alpha_enabled)) {
            alpha = (int) (((float) Color.alpha(getResources().getColor(R.color.notification_expanded_bg_color_without_base))) * SwipeHelper.getAlphaForOffset(row.getTranslation()));
        }
        this.mBackgroundPaint.setAlpha(alpha);
        canvas.drawRect(left, top, right, bottom, this.mBackgroundPaint);
    }

    private void drawCover(Canvas canvas, float left, float top, float right, float bottom) {
        canvas.drawRect(left, top, right, bottom, this.mTransparentPaint);
        canvas.drawRect(left, top, right, bottom, this.mCoverPaint);
    }

    /* access modifiers changed from: protected */
    public boolean verifyDrawable(Drawable who) {
        return super.verifyDrawable(who) || who == this.mBackground;
    }

    /* access modifiers changed from: protected */
    public void drawableStateChanged() {
        drawableStateChanged(this.mBackground);
    }

    private void drawableStateChanged(Drawable d) {
        if (d != null && d.isStateful()) {
            d.setState(getDrawableState());
        }
    }

    public void drawableHotspotChanged(float x, float y) {
        if (this.mBackground != null) {
            this.mBackground.setHotspot(x, y);
        }
    }

    public void setCustomBackground(Drawable background) {
        if (this.mBackground != background) {
            if (this.mBackground != null) {
                this.mBackground.setCallback(null);
                unscheduleDrawable(this.mBackground);
            }
            this.mBackground = background;
            if (this.mBackground != null) {
                this.mBackground.setCallback(this);
            }
            if (this.mBackground instanceof RippleDrawable) {
                ((RippleDrawable) this.mBackground).setForceSoftware(true);
            }
            invalidate();
        }
    }

    public void setCustomBackground(int drawableResId) {
        setCustomBackground(drawableResId, false, false);
    }

    public void setCustomBackground(int drawableResId, boolean drawTopDivider, boolean drawBottomDivider) {
        this.mDrawTopDivider = drawTopDivider;
        this.mDrawBottomDivider = drawBottomDivider;
        setCustomBackground(this.mContext.getDrawable(drawableResId));
    }

    public void setTint(int tintColor) {
    }

    public void setActualHeight(int actualHeight) {
        this.mActualHeight = actualHeight;
        invalidate();
    }

    public void setClipTopAmount(int clipTopAmount) {
        this.mClipTopAmount = clipTopAmount;
        invalidate();
    }

    public void setClipBottomAmount(int clipBottomAmount) {
        this.mClipBottomAmount = clipBottomAmount;
        invalidate();
    }

    public boolean hasOverlappingRendering() {
        return false;
    }

    public void setState(int[] drawableState) {
        this.mBackground.setState(drawableState);
    }

    public void setRippleColor(int color) {
        if (this.mBackground instanceof RippleDrawable) {
            ((RippleDrawable) this.mBackground).setColor(ColorStateList.valueOf(color));
        }
    }

    public void setDrawableAlpha(int drawableAlpha) {
        this.mBackground.setAlpha(drawableAlpha);
    }
}
