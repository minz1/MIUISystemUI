package com.android.systemui;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import java.util.ArrayList;
import java.util.List;

public class AutoReinflateContainer extends FrameLayout {
    private int mDensity;
    private final List<InflateListener> mInflateListeners = new ArrayList();
    private final int mLayout;
    private Object mLocaleList;

    public interface InflateListener {
        void onInflated(View view);
    }

    public AutoReinflateContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mDensity = context.getResources().getConfiguration().densityDpi;
        this.mLocaleList = SystemUICompat.getLocales(context.getResources().getConfiguration());
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AutoReinflateContainer);
        if (a.hasValue(0)) {
            this.mLayout = a.getResourceId(0, 0);
            inflateLayout();
            return;
        }
        throw new IllegalArgumentException("AutoReinflateContainer must contain a layout");
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        boolean shouldInflateLayout = false;
        int density = newConfig.densityDpi;
        if (density != this.mDensity) {
            this.mDensity = density;
            shouldInflateLayout = true;
        }
        Object localeList = SystemUICompat.getLocales(newConfig);
        if (localeList != this.mLocaleList) {
            this.mLocaleList = localeList;
            shouldInflateLayout = true;
        }
        if (shouldInflateLayout) {
            inflateLayout();
        }
    }

    /* access modifiers changed from: protected */
    public void inflateLayoutImpl() {
        LayoutInflater.from(getContext()).inflate(this.mLayout, this);
    }

    /* access modifiers changed from: protected */
    public void inflateLayout() {
        removeAllViews();
        inflateLayoutImpl();
        int N = this.mInflateListeners.size();
        for (int i = 0; i < N; i++) {
            this.mInflateListeners.get(i).onInflated(getChildAt(0));
        }
    }
}
