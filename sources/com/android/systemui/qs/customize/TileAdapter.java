package com.android.systemui.qs.customize;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.widget.FrameLayout;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.Util;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSTileHost;
import com.android.systemui.qs.customize.TileQueryHelper;
import com.android.systemui.qs.external.CustomTile;
import com.android.systemui.qs.tileimpl.QSIconViewImpl;
import com.android.systemui.statusbar.phone.SystemUIDialog;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import miui.app.AlertDialog;

public class TileAdapter extends RecyclerView.Adapter<Holder> implements TileQueryHelper.TileStateListener {
    private int mAccessibilityFromIndex;
    private final AccessibilityManager mAccessibilityManager;
    /* access modifiers changed from: private */
    public boolean mAccessibilityMoving;
    private List<TileQueryHelper.TileInfo> mAllTiles;
    private int mBottomDividerPadding;
    private final ItemTouchHelper.Callback mCallback = new ItemTouchHelper.Callback() {
        public boolean isLongPressDragEnabled() {
            return true;
        }

        public boolean isItemViewSwipeEnabled() {
            return false;
        }

        public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
            super.onSelectedChanged(viewHolder, actionState);
            if (actionState != 2) {
                viewHolder = null;
            }
            if (viewHolder != TileAdapter.this.mCurrentDrag) {
                if (TileAdapter.this.mCurrentDrag != null) {
                    int position = TileAdapter.this.mCurrentDrag.getAdapterPosition();
                    TileAdapter.this.mCurrentDrag.mTileView.setShowAppLabel(position > TileAdapter.this.mEditIndex && !((TileQueryHelper.TileInfo) TileAdapter.this.mTiles.get(position)).isSystem);
                    Holder unused = TileAdapter.this.mCurrentDrag = null;
                }
                if (viewHolder != null) {
                    Holder unused2 = TileAdapter.this.mCurrentDrag = (Holder) viewHolder;
                    TileAdapter.this.mCurrentDrag.startDrag();
                }
            }
        }

        public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);
            if (viewHolder != null) {
                ((Holder) viewHolder).stopDrag();
            }
            TileAdapter.this.saveSpecs(TileAdapter.this.mHost);
        }

        public boolean canDropOver(RecyclerView recyclerView, RecyclerView.ViewHolder current, RecyclerView.ViewHolder target) {
            boolean z = false;
            if (TileAdapter.this.canRemoveTiles() || current.getAdapterPosition() >= TileAdapter.this.mEditIndex) {
                if (target.getAdapterPosition() <= TileAdapter.this.mEditIndex + 1) {
                    z = true;
                }
                return z;
            }
            if (target.getAdapterPosition() < TileAdapter.this.mEditIndex) {
                z = true;
            }
            return z;
        }

        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            if (viewHolder.getItemViewType() == 1 || viewHolder.getItemViewType() == 4) {
                return makeMovementFlags(0, 0);
            }
            return makeMovementFlags(15, 0);
        }

        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            return TileAdapter.this.move(viewHolder.getAdapterPosition(), target.getAdapterPosition(), target.itemView);
        }

        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        }
    };
    private final Context mContext;
    /* access modifiers changed from: private */
    public Holder mCurrentDrag;
    private List<String> mCurrentSpecs;
    private RecyclerView.ItemDecoration mDecoration;
    /* access modifiers changed from: private */
    public int mEditIndex;
    private final Handler mHandler = new Handler();
    /* access modifiers changed from: private */
    public QSTileHost mHost;
    private final ItemTouchHelper mItemTouchHelper;
    /* access modifiers changed from: private */
    public Map<String, QSTile> mLiveTiles;
    private boolean mNeedsFocus;
    private List<TileQueryHelper.TileInfo> mOtherTiles;
    private RecyclerView mParent;
    private final GridLayoutManager.SpanSizeLookup mSizeLookup = new GridLayoutManager.SpanSizeLookup() {
        public int getSpanSize(int position) {
            int type = TileAdapter.this.getItemViewType(position);
            if (type == 1 || type == 4) {
                return TileAdapter.this.mSpanCount;
            }
            return 1;
        }
    };
    /* access modifiers changed from: private */
    public int mSpanCount;
    /* access modifiers changed from: private */
    public int mTileDividerIndex;
    /* access modifiers changed from: private */
    public final List<TileQueryHelper.TileInfo> mTiles = new ArrayList();
    private int mTopDividerPadding;

    public class Holder extends RecyclerView.ViewHolder {
        /* access modifiers changed from: private */
        public CustomizeTileView mTileView;

        public Holder(View itemView) {
            super(itemView);
            if (itemView instanceof FrameLayout) {
                this.mTileView = (CustomizeTileView) ((FrameLayout) itemView).getChildAt(0);
                this.mTileView.setBackground(null);
                this.mTileView.getIcon().setAnimationEnabled(false);
            }
        }

        public void clearDrag() {
            this.itemView.clearAnimation();
            this.mTileView.findViewById(R.id.tile_label).clearAnimation();
            this.mTileView.findViewById(R.id.tile_label).setAlpha(1.0f);
            this.mTileView.getAppLabel().clearAnimation();
            this.mTileView.getAppLabel().setAlpha(0.6f);
        }

        public void startDrag() {
            this.itemView.animate().setDuration(100).scaleX(1.2f).scaleY(1.2f);
            this.mTileView.findViewById(R.id.tile_label).animate().setDuration(100).alpha(0.0f);
            this.mTileView.getAppLabel().animate().setDuration(100).alpha(0.0f);
        }

        public void stopDrag() {
            this.itemView.animate().setDuration(100).scaleX(1.0f).scaleY(1.0f);
            this.mTileView.findViewById(R.id.tile_label).animate().setDuration(100).alpha(1.0f);
            this.mTileView.getAppLabel().animate().setDuration(100).alpha(0.6f);
        }
    }

    private class TileItemDecoration extends RecyclerView.ItemDecoration {
        private final ColorDrawable mDrawable;

        private TileItemDecoration(Context context) {
            this.mDrawable = new ColorDrawable(context.getColor(R.color.qs_customize_content_background_color));
        }

        public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
            super.onDraw(c, parent, state);
            int index = findEditViewIndex(parent);
            if (index >= 0) {
                drawBackgroundAfter(parent, index, c);
            }
        }

        private int findEditViewIndex(RecyclerView parent) {
            if ((parent.getLayoutManager() instanceof LinearLayoutManager) && ((LinearLayoutManager) parent.getLayoutManager()).findFirstVisibleItemPosition() > TileAdapter.this.mEditIndex) {
                return 0;
            }
            int childCount = parent.getChildCount();
            for (int i = 0; i < childCount; i++) {
                if (parent.getChildViewHolder(parent.getChildAt(i)).getAdapterPosition() == TileAdapter.this.mEditIndex) {
                    return i;
                }
            }
            return -1;
        }

        private void drawBackgroundAfter(RecyclerView parent, int index, Canvas c) {
            View child = parent.getChildAt(index);
            int width = parent.getWidth();
            int bottom = parent.getBottom();
            this.mDrawable.setBounds(0, child.getTop() + ((RecyclerView.LayoutParams) child.getLayoutParams()).topMargin + Math.round(ViewCompat.getTranslationY(child)), width, bottom);
            this.mDrawable.draw(c);
        }
    }

    public TileAdapter(Context context, int count, RecyclerView parent) {
        this.mContext = context;
        this.mSpanCount = count;
        this.mAccessibilityManager = (AccessibilityManager) context.getSystemService(AccessibilityManager.class);
        this.mItemTouchHelper = new ItemTouchHelper(this.mCallback);
        this.mDecoration = new TileItemDecoration(context);
        Resources res = context.getResources();
        this.mTopDividerPadding = -res.getDimensionPixelSize(R.dimen.qs_customize_content_padding_horizontal);
        this.mBottomDividerPadding = res.getDimensionPixelSize(R.dimen.qs_customize_divider_padding_horizontal);
        this.mParent = parent;
    }

    public void setHost(QSTileHost host) {
        this.mHost = host;
    }

    public ItemTouchHelper getItemTouchHelper() {
        return this.mItemTouchHelper;
    }

    public RecyclerView.ItemDecoration getItemDecoration() {
        return this.mDecoration;
    }

    public void saveSpecs(QSTileHost host) {
        List<String> newSpecs = new ArrayList<>();
        int i = 0;
        while (i < this.mTiles.size() && this.mTiles.get(i) != null) {
            newSpecs.add(this.mTiles.get(i).spec);
            i++;
        }
        newSpecs.add("edit");
        host.changeTiles(this.mCurrentSpecs, newSpecs);
        this.mCurrentSpecs = newSpecs;
    }

    public void resetTileSpecs(QSTileHost host, List<String> specs) {
        host.changeTiles(this.mCurrentSpecs, specs);
        setTileSpecs(specs);
    }

    public void setTileSpecs(List<String> currentSpecs) {
        if (!currentSpecs.equals(this.mCurrentSpecs)) {
            this.mCurrentSpecs = currentSpecs;
            recalcSpecs();
        }
    }

    public void onTilesChanged(List<TileQueryHelper.TileInfo> tiles, Map<String, QSTile> liveTiles) {
        this.mLiveTiles = liveTiles;
        this.mAllTiles = tiles;
        recalcSpecs();
    }

    public void onTileChanged(TileQueryHelper.TileInfo tileInfoNew) {
        int i = 0;
        while (i < this.mTiles.size()) {
            TileQueryHelper.TileInfo tileInfo = this.mTiles.get(i);
            if (tileInfo == null || !TextUtils.equals(tileInfoNew.spec, tileInfo.spec)) {
                i++;
            } else {
                handleUpdateStateForPosition(i, tileInfoNew.state);
                return;
            }
        }
    }

    private void handleUpdateStateForPosition(int position, QSTile.State newState) {
        this.mTiles.get(position).state = newState;
        Holder holder = (Holder) this.mParent.findViewHolderForAdapterPosition(position);
        if (holder != null) {
            holder.mTileView.getIcon().setAnimationEnabled(true);
            holder.mTileView.handleStateChanged(newState);
            holder.mTileView.getIcon().setAnimationEnabled(false);
        }
    }

    private void recalcSpecs() {
        if (this.mCurrentSpecs != null && this.mAllTiles != null) {
            this.mOtherTiles = new ArrayList(this.mAllTiles);
            this.mTiles.clear();
            int i = 0;
            for (int i2 = 0; i2 < this.mCurrentSpecs.size(); i2++) {
                TileQueryHelper.TileInfo tile = getAndRemoveOther(this.mCurrentSpecs.get(i2));
                if (tile != null) {
                    this.mTiles.add(tile);
                }
            }
            this.mTiles.add(null);
            while (i < this.mOtherTiles.size()) {
                TileQueryHelper.TileInfo tile2 = this.mOtherTiles.get(i);
                if (tile2.isSystem) {
                    this.mOtherTiles.remove(i);
                    this.mTiles.add(tile2);
                    i--;
                }
                i++;
            }
            this.mTileDividerIndex = this.mTiles.size();
            this.mTiles.add(null);
            this.mTiles.addAll(this.mOtherTiles);
            updateDividerLocations();
            notifyDataSetChanged();
        }
    }

    private TileQueryHelper.TileInfo getAndRemoveOther(String s) {
        for (int i = 0; i < this.mOtherTiles.size(); i++) {
            if (this.mOtherTiles.get(i).spec.equals(s)) {
                return this.mOtherTiles.remove(i);
            }
        }
        return null;
    }

    public int getItemViewType(int position) {
        if (this.mAccessibilityMoving && position == this.mEditIndex - 1) {
            return 2;
        }
        if (position == this.mTileDividerIndex) {
            return 4;
        }
        if (position == this.mEditIndex) {
            return 1;
        }
        return 0;
    }

    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        if (viewType == 4 || viewType == 1) {
            View view = inflater.inflate(R.layout.qs_customize_tile_divider, parent, false);
            if (viewType == 4) {
                view.setPadding(this.mBottomDividerPadding, 0, this.mBottomDividerPadding, 0);
            } else {
                view.setPadding(this.mTopDividerPadding, 0, this.mTopDividerPadding, 0);
            }
            return new Holder(view);
        }
        FrameLayout frame = (FrameLayout) inflater.inflate(R.layout.qs_customize_tile_frame, parent, false);
        frame.addView(new CustomizeTileView(context, new QSIconViewImpl(context)));
        return new Holder(frame);
    }

    public int getItemCount() {
        return this.mTiles.size();
    }

    public boolean onFailedToRecycleView(Holder holder) {
        holder.clearDrag();
        return true;
    }

    public void onBindViewHolder(final Holder holder, int position) {
        int i = 4;
        boolean selectable = false;
        if (holder.getItemViewType() == 4) {
            View view = holder.itemView;
            if (this.mTileDividerIndex < this.mTiles.size() - 1) {
                i = 0;
            }
            view.setVisibility(i);
        } else if (holder.getItemViewType() != 1) {
            if (holder.getItemViewType() == 2) {
                holder.mTileView.setClickable(true);
                holder.mTileView.setFocusable(true);
                holder.mTileView.setFocusableInTouchMode(true);
                holder.mTileView.setVisibility(0);
                holder.mTileView.setImportantForAccessibility(1);
                holder.mTileView.setContentDescription(this.mContext.getString(R.string.accessibility_qs_edit_position_label, new Object[]{Integer.valueOf(position + 1)}));
                holder.mTileView.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        TileAdapter.this.selectPosition(holder.getAdapterPosition(), v);
                    }
                });
                if (this.mNeedsFocus) {
                    holder.mTileView.requestLayout();
                    holder.mTileView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                        public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                            holder.mTileView.removeOnLayoutChangeListener(this);
                            holder.mTileView.requestFocus();
                        }
                    });
                    this.mNeedsFocus = false;
                }
                return;
            }
            TileQueryHelper.TileInfo info = this.mTiles.get(position);
            if (info != null) {
                if (position > this.mEditIndex) {
                    info.state.contentDescription = this.mContext.getString(R.string.accessibility_qs_edit_add_tile_label, new Object[]{info.state.label});
                } else if (this.mAccessibilityMoving) {
                    info.state.contentDescription = this.mContext.getString(R.string.accessibility_qs_edit_position_label, new Object[]{Integer.valueOf(position + 1)});
                } else {
                    info.state.contentDescription = this.mContext.getString(R.string.accessibility_qs_edit_tile_label, new Object[]{Integer.valueOf(position + 1), info.state.label});
                }
                holder.mTileView.onStateChanged(info.state);
                holder.mTileView.setShowAppLabel(position > this.mEditIndex && !info.isSystem);
                bindOnClickListeners(info, holder);
                if (this.mAccessibilityManager.isTouchExplorationEnabled()) {
                    if (!this.mAccessibilityMoving || position < this.mEditIndex) {
                        selectable = true;
                    }
                    holder.mTileView.setClickable(selectable);
                    holder.mTileView.setFocusable(selectable);
                    CustomizeTileView access$100 = holder.mTileView;
                    if (selectable) {
                        i = 1;
                    }
                    access$100.setImportantForAccessibility(i);
                    if (selectable) {
                        holder.mTileView.setOnClickListener(new View.OnClickListener() {
                            public void onClick(View v) {
                                int position = holder.getAdapterPosition();
                                if (TileAdapter.this.mAccessibilityMoving) {
                                    TileAdapter.this.selectPosition(position, v);
                                } else if (position >= TileAdapter.this.mEditIndex || !TileAdapter.this.canRemoveTiles()) {
                                    TileAdapter.this.startAccessibleDrag(position);
                                } else {
                                    TileAdapter.this.showAccessibilityDialog(position, v);
                                }
                            }
                        });
                    }
                }
            }
        }
    }

    private void bindOnClickListeners(final TileQueryHelper.TileInfo info, Holder holder) {
        holder.mTileView.init(new View.OnClickListener() {
            public void onClick(View v) {
                QSTile tile;
                if (TileAdapter.this.mLiveTiles == null || !TileAdapter.this.mLiveTiles.containsKey(info.spec)) {
                    tile = TileAdapter.this.mHost.getTile(info.spec);
                } else {
                    tile = (QSTile) TileAdapter.this.mLiveTiles.get(info.spec);
                }
                if (tile != null) {
                    tile.click(true);
                } else if (TileAdapter.isCustomTile(info)) {
                    Util.showSystemOverlayToast(v.getContext(), (int) R.string.quick_settings_toast_drag_to_enable_custom_tile, 0);
                }
            }
        }, null, null);
    }

    /* access modifiers changed from: private */
    public boolean canRemoveTiles() {
        return this.mCurrentSpecs.size() > 12;
    }

    public List<String> getCurrentSpecs() {
        return this.mCurrentSpecs;
    }

    /* access modifiers changed from: private */
    public void selectPosition(int position, View v) {
        int i;
        this.mAccessibilityMoving = false;
        List<TileQueryHelper.TileInfo> list = this.mTiles;
        this.mEditIndex = this.mEditIndex - 1;
        list.remove(i);
        notifyItemRemoved(this.mEditIndex - 1);
        if (position == this.mEditIndex) {
            position--;
        }
        move(this.mAccessibilityFromIndex, position, v);
        notifyDataSetChanged();
    }

    /* access modifiers changed from: private */
    public void showAccessibilityDialog(final int position, final View v) {
        final TileQueryHelper.TileInfo info = this.mTiles.get(position);
        AlertDialog dialog = new AlertDialog.Builder(this.mContext).setItems(new CharSequence[]{this.mContext.getString(R.string.accessibility_qs_edit_move_tile, new Object[]{info.state.label}), this.mContext.getString(R.string.accessibility_qs_edit_remove_tile, new Object[]{info.state.label})}, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    TileAdapter.this.startAccessibleDrag(position);
                    return;
                }
                boolean unused = TileAdapter.this.move(position, info.isSystem ? TileAdapter.this.mEditIndex : TileAdapter.this.mTileDividerIndex, v);
                TileAdapter.this.notifyItemChanged(TileAdapter.this.mTileDividerIndex);
                TileAdapter.this.notifyDataSetChanged();
            }
        }).setNegativeButton(17039360, null).create();
        SystemUIDialog.setShowForAllUsers(dialog, true);
        SystemUIDialog.applyFlags(dialog);
        dialog.show();
    }

    /* access modifiers changed from: private */
    public void startAccessibleDrag(int position) {
        this.mAccessibilityMoving = true;
        this.mNeedsFocus = true;
        this.mAccessibilityFromIndex = position;
        List<TileQueryHelper.TileInfo> list = this.mTiles;
        int i = this.mEditIndex;
        this.mEditIndex = i + 1;
        list.add(i, null);
        notifyDataSetChanged();
    }

    public GridLayoutManager.SpanSizeLookup getSizeLookup() {
        return this.mSizeLookup;
    }

    /* access modifiers changed from: private */
    public boolean move(int from, int to, View v) {
        CharSequence announcement;
        if (to == from) {
            return true;
        }
        if (from > this.mEditIndex && to > this.mEditIndex) {
            return false;
        }
        CharSequence fromLabel = this.mTiles.get(from).state.label;
        move(from, to, this.mTiles);
        updateDividerLocations();
        if (to >= this.mEditIndex) {
            MetricsLogger.action(this.mContext, 360, strip(this.mTiles.get(to)));
            MetricsLogger.action(this.mContext, 361, from);
            announcement = this.mContext.getString(R.string.accessibility_qs_edit_tile_removed, new Object[]{fromLabel});
        } else if (from >= this.mEditIndex) {
            MetricsLogger.action(this.mContext, 362, strip(this.mTiles.get(to)));
            MetricsLogger.action(this.mContext, 363, to);
            announcement = this.mContext.getString(R.string.accessibility_qs_edit_tile_added, new Object[]{fromLabel, Integer.valueOf(to + 1)});
        } else {
            MetricsLogger.action(this.mContext, 364, strip(this.mTiles.get(to)));
            MetricsLogger.action(this.mContext, 365, to);
            announcement = this.mContext.getString(R.string.accessibility_qs_edit_tile_moved, new Object[]{fromLabel, Integer.valueOf(to + 1)});
        }
        v.announceForAccessibility(announcement);
        saveSpecs(this.mHost);
        return true;
    }

    private void updateDividerLocations() {
        this.mEditIndex = -1;
        this.mTileDividerIndex = this.mTiles.size();
        for (int i = 0; i < this.mTiles.size(); i++) {
            if (this.mTiles.get(i) == null) {
                if (this.mEditIndex == -1) {
                    this.mEditIndex = i;
                } else {
                    this.mTileDividerIndex = i;
                }
            }
        }
        if (this.mTiles.size() - 1 == this.mTileDividerIndex) {
            notifyItemChanged(this.mTileDividerIndex);
        }
    }

    private static String strip(TileQueryHelper.TileInfo tileInfo) {
        String spec = tileInfo.spec;
        if (isCustomTile(tileInfo)) {
            return CustomTile.getComponentFromSpec(spec).getPackageName();
        }
        return spec;
    }

    /* access modifiers changed from: private */
    public static boolean isCustomTile(TileQueryHelper.TileInfo tileInfo) {
        return tileInfo.spec.startsWith("custom(");
    }

    private <T> void move(int from, int to, List<T> list) {
        list.add(to, list.remove(from));
        notifyItemMoved(from, to);
    }

    public void setSpanCount(int spanCount) {
        this.mSpanCount = spanCount;
    }
}
