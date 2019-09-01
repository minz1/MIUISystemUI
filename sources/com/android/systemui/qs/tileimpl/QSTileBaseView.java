package com.android.systemui.qs.tileimpl;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Switch;
import com.android.systemui.R;
import com.android.systemui.plugins.qs.QSIconView;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.plugins.qs.QSTileView;

public class QSTileBaseView extends QSTileView {
    private String mAccessibilityClass;
    private boolean mClicked;
    private boolean mCollapsedView;
    private final H mHandler = new H();
    protected QSIconView mIcon;
    private final FrameLayout mIconFrame;
    private boolean mTileState;

    private class H extends Handler {
        public H() {
            super(Looper.getMainLooper());
        }

        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                QSTileBaseView.this.handleStateChanged((QSTile.State) msg.obj);
            }
        }
    }

    public QSTileBaseView(Context context, QSIconView icon, boolean collapsedView) {
        super(context);
        this.mIconFrame = new FrameLayout(context);
        this.mIconFrame.setForegroundGravity(17);
        int size = context.getResources().getDimensionPixelSize(R.dimen.qs_tile_icon_bg_size);
        addView(this.mIconFrame, new LinearLayout.LayoutParams(size, size));
        this.mIcon = icon;
        this.mIconFrame.addView(this.mIcon);
        setImportantForAccessibility(2);
        setPadding(0, 0, 0, 0);
        setClipChildren(false);
        setClipToPadding(false);
        this.mCollapsedView = collapsedView;
        setFocusable(true);
    }

    public void init(final QSTile tile) {
        init(new View.OnClickListener() {
            public void onClick(View view) {
                tile.click();
            }
        }, new View.OnClickListener() {
            public void onClick(View view) {
                tile.secondaryClick();
            }
        }, new View.OnLongClickListener() {
            public boolean onLongClick(View view) {
                tile.longClick();
                return true;
            }
        });
    }

    public void init(View.OnClickListener click, View.OnClickListener secondaryClick, View.OnLongClickListener longClick) {
        this.mIconFrame.setOnClickListener(click);
        this.mIconFrame.setOnLongClickListener(longClick);
    }

    public boolean hasOverlappingRendering() {
        return false;
    }

    public View updateAccessibilityOrder(View previousView) {
        setAccessibilityTraversalAfter(previousView.getId());
        return this;
    }

    public void onStateChanged(QSTile.State state) {
        this.mHandler.obtainMessage(1, state).sendToTarget();
    }

    /* access modifiers changed from: protected */
    public void handleStateChanged(QSTile.State state) {
        setClickable(state.state != 0);
        this.mIcon.setIcon(state);
        this.mIcon.setContentDescription(state.contentDescription);
        this.mAccessibilityClass = state.expandedAccessibilityClassName;
        if (state instanceof QSTile.BooleanState) {
            boolean newState = ((QSTile.BooleanState) state).value;
            if (this.mTileState != newState) {
                this.mClicked = false;
                this.mTileState = newState;
            }
        }
    }

    public int getDetailY() {
        return getTop() + (getHeight() / 2);
    }

    public QSIconView getIcon() {
        return this.mIcon;
    }

    public View getIconWithBackground() {
        return this.mIconFrame;
    }

    public boolean performClick() {
        this.mClicked = true;
        return super.performClick();
    }

    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        if (!TextUtils.isEmpty(this.mAccessibilityClass)) {
            event.setClassName(this.mAccessibilityClass);
            if (Switch.class.getName().equals(this.mAccessibilityClass)) {
                boolean b = this.mClicked ? !this.mTileState : this.mTileState;
                event.setContentDescription(getResources().getString(b ? R.string.switch_bar_on : R.string.switch_bar_off));
                event.setChecked(b);
            }
        }
    }

    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        if (!TextUtils.isEmpty(this.mAccessibilityClass)) {
            if (!this.mAccessibilityClass.equals(Button.class.getName())) {
                info.setClassName(this.mAccessibilityClass);
            }
            if (Switch.class.getName().equals(this.mAccessibilityClass)) {
                boolean b = this.mClicked ? !this.mTileState : this.mTileState;
                info.setText(getResources().getString(b ? R.string.switch_bar_on : R.string.switch_bar_off));
                info.setChecked(b);
                info.setCheckable(true);
            }
        }
    }
}
