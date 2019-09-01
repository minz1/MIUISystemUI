package com.android.systemui.qs;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.systemui.FontUtils;
import com.android.systemui.R;

public class QSDetailItems extends FrameLayout {
    private static final boolean DEBUG = Log.isLoggable("QSDetailItems", 3);
    private final Adapter mAdapter = new Adapter();
    /* access modifiers changed from: private */
    public Callback mCallback;
    /* access modifiers changed from: private */
    public final Context mContext;
    private View mEmpty;
    /* access modifiers changed from: private */
    public ImageView mEmptyIcon;
    protected Runnable mEmptyStateRunnable = new Runnable() {
        public void run() {
            QSDetailItems.this.mEmptyIcon.setImageResource(QSDetailItems.this.mIconId);
            QSDetailItems.this.mEmptyText.setText(QSDetailItems.this.mTextId);
        }
    };
    /* access modifiers changed from: private */
    public TextView mEmptyText;
    private final H mHandler = new H();
    /* access modifiers changed from: private */
    public int mIconId;
    private AutoSizingList mItemList;
    /* access modifiers changed from: private */
    public Item[] mItems;
    /* access modifiers changed from: private */
    public boolean mItemsVisible = true;
    /* access modifiers changed from: private */
    public final int mQsDetailIconOverlaySize;
    private String mTag;
    /* access modifiers changed from: private */
    public int mTextId;

    private class Adapter extends BaseAdapter {
        private Adapter() {
        }

        public int getCount() {
            if (QSDetailItems.this.mItems != null) {
                return QSDetailItems.this.mItems.length;
            }
            return 0;
        }

        public Object getItem(int position) {
            return QSDetailItems.this.mItems[position];
        }

        public long getItemId(int position) {
            return 0;
        }

        public View getView(int position, View view, ViewGroup parent) {
            final Item item = QSDetailItems.this.mItems[position];
            if (view == null) {
                view = LayoutInflater.from(QSDetailItems.this.mContext).inflate(R.layout.qs_detail_item, parent, false);
            }
            view.setVisibility(QSDetailItems.this.mItemsVisible ? 0 : 4);
            ImageView iv = (ImageView) view.findViewById(16908294);
            iv.setImageResource(item.icon);
            iv.getOverlay().clear();
            if (item.overlay != null) {
                item.overlay.setBounds(0, 0, QSDetailItems.this.mQsDetailIconOverlaySize, QSDetailItems.this.mQsDetailIconOverlaySize);
                iv.getOverlay().add(item.overlay);
            }
            TextView title = (TextView) view.findViewById(16908310);
            title.setText(item.line1);
            TextView summary = (TextView) view.findViewById(16908304);
            boolean twoLines = !TextUtils.isEmpty(item.line2);
            title.setMaxLines(twoLines ? 1 : 2);
            summary.setVisibility(twoLines ? 0 : 8);
            summary.setText(twoLines ? item.line2 : null);
            view.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (QSDetailItems.this.mCallback != null) {
                        QSDetailItems.this.mCallback.onDetailItemClick(item);
                    }
                }
            });
            ImageView icon2 = (ImageView) view.findViewById(16908296);
            if (item.canDisconnect) {
                icon2.setImageResource(R.drawable.ic_qs_cancel);
                icon2.setVisibility(0);
                icon2.setClickable(true);
                icon2.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (QSDetailItems.this.mCallback != null) {
                            QSDetailItems.this.mCallback.onDetailItemDisconnect(item);
                        }
                    }
                });
            } else if (item.icon2 != -1) {
                icon2.setVisibility(0);
                icon2.setImageResource(item.icon2);
                icon2.setClickable(false);
            } else {
                icon2.setVisibility(8);
            }
            return view;
        }
    }

    public interface Callback {
        void onDetailItemClick(Item item);

        void onDetailItemDisconnect(Item item);
    }

    private class H extends Handler {
        public H() {
            super(Looper.getMainLooper());
        }

        public void handleMessage(Message msg) {
            boolean z = true;
            if (msg.what == 1) {
                QSDetailItems.this.handleSetItems((Item[]) msg.obj);
            } else if (msg.what == 2) {
                QSDetailItems.this.handleSetCallback((Callback) msg.obj);
            } else if (msg.what == 3) {
                QSDetailItems qSDetailItems = QSDetailItems.this;
                if (msg.arg1 == 0) {
                    z = false;
                }
                qSDetailItems.handleSetItemsVisible(z);
            }
        }
    }

    public static class Item {
        public boolean canDisconnect;
        public int icon;
        public int icon2 = -1;
        public CharSequence line1;
        public CharSequence line2;
        public Drawable overlay;
        public Object tag;
    }

    public QSDetailItems(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        this.mTag = "QSDetailItems";
        this.mQsDetailIconOverlaySize = (int) getResources().getDimension(R.dimen.qs_detail_icon_overlay_size);
    }

    public static QSDetailItems convertOrInflate(Context context, View convert, ViewGroup parent) {
        if (convert instanceof QSDetailItems) {
            return (QSDetailItems) convert;
        }
        return (QSDetailItems) LayoutInflater.from(context).inflate(R.layout.qs_detail_items, parent, false);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mItemList = (AutoSizingList) findViewById(16908298);
        this.mItemList.setVisibility(8);
        this.mItemList.setAdapter(this.mAdapter);
        this.mEmpty = findViewById(16908292);
        this.mEmpty.setVisibility(8);
        this.mEmptyText = (TextView) this.mEmpty.findViewById(16908310);
        this.mEmptyIcon = (ImageView) this.mEmpty.findViewById(16908294);
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        FontUtils.updateFontSize(this.mEmptyText, R.dimen.qs_detail_empty_text_size);
        FontUtils.updateFontColor(this.mEmptyText, R.color.qs_detail_empty_text_color);
        int count = this.mItemList.getChildCount();
        for (int i = 0; i < count; i++) {
            View item = this.mItemList.getChildAt(i);
            FontUtils.updateFontSize(item, 16908310, R.dimen.qs_detail_item_primary_text_size);
            FontUtils.updateFontColor(item, 16908310, R.color.qs_detail_item_primary_text_color);
            FontUtils.updateFontSize(item, 16908304, R.dimen.qs_detail_item_secondary_text_size);
            FontUtils.updateFontColor(item, 16908304, R.color.qs_detail_item_secondary_text_color);
        }
    }

    public void setTagSuffix(String suffix) {
        this.mTag = "QSDetailItems." + suffix;
    }

    public void setEmptyState(int icon, int text) {
        this.mIconId = icon;
        this.mTextId = text;
        this.mEmptyIcon.removeCallbacks(this.mEmptyStateRunnable);
        this.mEmptyIcon.post(this.mEmptyStateRunnable);
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (DEBUG) {
            Log.d(this.mTag, "onAttachedToWindow");
        }
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (DEBUG) {
            Log.d(this.mTag, "onDetachedFromWindow");
        }
        this.mCallback = null;
    }

    public void setCallback(Callback callback) {
        this.mHandler.removeMessages(2);
        this.mHandler.obtainMessage(2, callback).sendToTarget();
    }

    public void setItems(Item[] items) {
        this.mHandler.removeMessages(1);
        this.mHandler.obtainMessage(1, items).sendToTarget();
    }

    public void setItemsVisible(boolean visible) {
        this.mHandler.removeMessages(3);
        this.mHandler.obtainMessage(3, visible, 0).sendToTarget();
    }

    /* access modifiers changed from: private */
    public void handleSetCallback(Callback callback) {
        this.mCallback = callback;
    }

    /* access modifiers changed from: private */
    public void handleSetItems(Item[] items) {
        int i = 0;
        int itemCount = items != null ? items.length : 0;
        this.mEmpty.setVisibility(itemCount == 0 ? 0 : 8);
        AutoSizingList autoSizingList = this.mItemList;
        if (itemCount == 0) {
            i = 8;
        }
        autoSizingList.setVisibility(i);
        this.mItems = items;
        this.mAdapter.notifyDataSetChanged();
    }

    /* access modifiers changed from: private */
    public void handleSetItemsVisible(boolean visible) {
        if (this.mItemsVisible != visible) {
            this.mItemsVisible = visible;
            for (int i = 0; i < this.mItemList.getChildCount(); i++) {
                this.mItemList.getChildAt(i).setVisibility(this.mItemsVisible ? 0 : 4);
            }
        }
    }
}
