package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.ArraySet;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.Util;
import com.android.systemui.statusbar.Icons;
import com.android.systemui.statusbar.StatusBarIconView;
import com.android.systemui.statusbar.policy.DarkIconDispatcher;
import com.android.systemui.statusbar.policy.DarkIconDispatcherHelper;
import com.android.systemui.util.DisableStateTracker;

public interface StatusBarIconController {

    public static class DarkIconManager extends IconManager {
        /* access modifiers changed from: private */
        public static int sFilterColor = 0;
        private final DarkIconDispatcher mDarkIconDispatcher = ((DarkIconDispatcher) Dependency.get(DarkIconDispatcher.class));
        /* access modifiers changed from: private */
        public float mDarkIntensity;
        private DarkIconDispatcher.DarkReceiver mDarkReceiver = new DarkIconDispatcher.DarkReceiver() {
            public void onDarkChanged(Rect area, float darkIntensity, int tint) {
                if (!DarkIconManager.this.mShieldDarkReceiver) {
                    float unused = DarkIconManager.this.mDarkIntensity = darkIntensity;
                    DarkIconManager.this.mTintArea.set(area);
                    if (DarkIconManager.this.mGroup != null && DarkIconManager.this.mGroup.getChildCount() != 0) {
                        for (int i = 0; i < DarkIconManager.this.mGroup.getChildCount(); i++) {
                            if (DarkIconManager.this.mGroup.getChildAt(i) instanceof StatusBarIconView) {
                                StatusBarIconView iconView = (StatusBarIconView) DarkIconManager.this.mGroup.getChildAt(i);
                                boolean isDarkMode = DarkIconDispatcherHelper.inDarkMode(DarkIconManager.this.mTintArea, iconView, DarkIconManager.this.mDarkIntensity);
                                iconView.setImageResource(Icons.get(Integer.valueOf(iconView.getStatusBarIcon().icon.getResId()), isDarkMode));
                                Drawable drawable = iconView.getDrawable();
                                if (drawable != null) {
                                    if (!isDarkMode || !Util.showCtsSpecifiedColor()) {
                                        drawable.setColorFilter(null);
                                    } else {
                                        if (DarkIconManager.sFilterColor == 0) {
                                            int unused2 = DarkIconManager.sFilterColor = DarkIconManager.this.mContext.getResources().getColor(R.color.status_bar_icon_text_color_dark_mode_cts);
                                        }
                                        drawable.setColorFilter(DarkIconManager.sFilterColor, PorterDuff.Mode.SRC_IN);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        };
        private int mIconHPadding = this.mContext.getResources().getDimensionPixelSize(R.dimen.status_bar_icon_padding);
        /* access modifiers changed from: private */
        public boolean mShieldDarkReceiver;
        /* access modifiers changed from: private */
        public Rect mTintArea = new Rect();

        public DarkIconManager(LinearLayout linearLayout) {
            super(linearLayout);
            this.mDarkIconDispatcher.addDarkReceiver(this.mDarkReceiver);
        }

        /* access modifiers changed from: protected */
        public void onIconAdded(int index, String slot, boolean blocked, StatusBarIcon icon) {
            addIcon(index, slot, blocked, icon);
            applyDark(index);
        }

        /* access modifiers changed from: protected */
        public LinearLayout.LayoutParams onCreateLayoutParams() {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, this.mIconSize);
            lp.setMargins(this.mIconHPadding, 0, this.mIconHPadding, 0);
            return lp;
        }

        public void destroy() {
            this.mGroup.removeAllViews();
            this.mDarkIconDispatcher.removeDarkReceiver(this.mDarkReceiver);
        }

        public void onSetIcon(int viewIndex, String slot, StatusBarIcon icon) {
            super.onSetIcon(viewIndex, slot, icon);
            applyDark(viewIndex);
        }

        public void setShieldDarkReceiver(boolean isShieldDarkReceiver) {
            this.mShieldDarkReceiver = isShieldDarkReceiver;
        }

        public void setDarkIntensity(Rect area, float darkIntensity, int tint) {
            this.mDarkIntensity = darkIntensity;
            this.mTintArea.set(area);
        }

        private void applyDark(int index) {
            if (index < this.mGroup.getChildCount() && (this.mGroup.getChildAt(index) instanceof StatusBarIconView)) {
                StatusBarIconView iconView = (StatusBarIconView) this.mGroup.getChildAt(index);
                iconView.setImageResource(Icons.get(Integer.valueOf(iconView.getStatusBarIcon().icon.getResId()), DarkIconDispatcherHelper.inDarkMode(this.mTintArea, iconView, this.mDarkIntensity)));
            }
        }
    }

    public static class IconManager {
        protected final Context mContext;
        protected final ViewGroup mGroup;
        protected final int mIconSize = this.mContext.getResources().getDimensionPixelSize(17105354);
        public ArraySet<String> mWhiteList;

        public IconManager(ViewGroup group) {
            this.mGroup = group;
            this.mContext = group.getContext();
            DisableStateTracker tracker = new DisableStateTracker(0, 2);
            this.mGroup.addOnAttachStateChangeListener(tracker);
            if (this.mGroup.isAttachedToWindow()) {
                tracker.onViewAttachedToWindow(this.mGroup);
            }
        }

        /* access modifiers changed from: protected */
        public void onIconAdded(int index, String slot, boolean blocked, StatusBarIcon icon) {
            addIcon(index, slot, blocked, icon);
        }

        /* access modifiers changed from: protected */
        public StatusBarIconView addIcon(int index, String slot, boolean blocked, StatusBarIcon icon) {
            StatusBarIconView view = onCreateStatusBarIconView(slot, this.mWhiteList != null ? !this.mWhiteList.contains(slot) : blocked);
            view.set(icon);
            this.mGroup.addView(view, index, onCreateLayoutParams());
            return view;
        }

        /* access modifiers changed from: protected */
        public StatusBarIconView onCreateStatusBarIconView(String slot, boolean blocked) {
            return new StatusBarIconView(this.mContext, slot, null, blocked);
        }

        /* access modifiers changed from: protected */
        public LinearLayout.LayoutParams onCreateLayoutParams() {
            return new LinearLayout.LayoutParams(-2, this.mIconSize);
        }

        /* access modifiers changed from: protected */
        public void destroy() {
            this.mGroup.removeAllViews();
        }

        /* access modifiers changed from: protected */
        public void onIconExternal(int viewIndex, int height) {
            ImageView imageView = (ImageView) this.mGroup.getChildAt(viewIndex);
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imageView.setAdjustViewBounds(true);
            setHeightAndCenter(imageView, height);
        }

        private void setHeightAndCenter(ImageView imageView, int height) {
            ViewGroup.LayoutParams params = imageView.getLayoutParams();
            params.height = height;
            if (params instanceof LinearLayout.LayoutParams) {
                ((LinearLayout.LayoutParams) params).gravity = 16;
            }
            imageView.setLayoutParams(params);
        }

        /* access modifiers changed from: protected */
        public void onRemoveIcon(int viewIndex, String slot) {
            this.mGroup.removeViewAt(viewIndex);
        }

        public void onSetIcon(int viewIndex, String slot, StatusBarIcon icon) {
            ((StatusBarIconView) this.mGroup.getChildAt(viewIndex)).set(icon);
        }

        public boolean hasView(String slot) {
            for (int i = 0; i < this.mGroup.getChildCount(); i++) {
                if (((StatusBarIconView) this.mGroup.getChildAt(i)).getSlot().equals(slot)) {
                    return true;
                }
            }
            return false;
        }
    }

    void addIconGroup(IconManager iconManager);

    void dispatchDemoCommand(String str, Bundle bundle);

    void removeIcon(String str);

    void removeIconGroup(IconManager iconManager);

    void setExternalIcon(String str);

    void setIcon(String str, int i, CharSequence charSequence);

    void setIcon(String str, StatusBarIcon statusBarIcon);

    void setIconVisibility(String str, boolean z);
}
