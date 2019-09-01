package com.android.systemui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import com.android.systemui.plugins.PluginListener;
import com.android.systemui.plugins.PluginManager;
import com.android.systemui.plugins.ViewProvider;

public class PluginInflateContainer extends AutoReinflateContainer implements PluginListener<ViewProvider> {
    private Class<?> mClass;
    private View mPluginView;

    public PluginInflateContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
        String viewType = context.obtainStyledAttributes(attrs, R.styleable.PluginInflateContainer).getString(0);
        try {
            this.mClass = Class.forName(viewType);
        } catch (Exception e) {
            Log.d("PluginInflateContainer", "Problem getting class info " + viewType, e);
            this.mClass = null;
        }
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (this.mClass != null) {
            ((PluginManager) Dependency.get(PluginManager.class)).addPluginListener(this, this.mClass);
        }
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (this.mClass != null) {
            ((PluginManager) Dependency.get(PluginManager.class)).removePluginListener(this);
        }
    }

    /* access modifiers changed from: protected */
    public void inflateLayoutImpl() {
        if (this.mPluginView != null) {
            addView(this.mPluginView);
        } else {
            super.inflateLayoutImpl();
        }
    }

    public void onPluginConnected(ViewProvider plugin, Context context) {
        this.mPluginView = plugin.getView();
        inflateLayout();
    }

    public void onPluginDisconnected(ViewProvider plugin) {
        this.mPluginView = null;
        inflateLayout();
    }
}
