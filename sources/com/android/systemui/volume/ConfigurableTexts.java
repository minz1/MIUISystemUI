package com.android.systemui.volume;

import android.content.Context;
import android.content.res.Resources;
import android.util.ArrayMap;
import android.view.View;
import android.widget.TextView;

public class ConfigurableTexts {
    private final Context mContext;
    /* access modifiers changed from: private */
    public final ArrayMap<TextView, Integer> mTextLabels = new ArrayMap<>();
    /* access modifiers changed from: private */
    public final ArrayMap<TextView, Integer> mTexts = new ArrayMap<>();
    private final Runnable mUpdateAll = new Runnable() {
        public void run() {
            for (int i = 0; i < ConfigurableTexts.this.mTexts.size(); i++) {
                ConfigurableTexts.this.setTextSizeH((TextView) ConfigurableTexts.this.mTexts.keyAt(i), ((Integer) ConfigurableTexts.this.mTexts.valueAt(i)).intValue());
            }
            for (int i2 = 0; i2 < ConfigurableTexts.this.mTextLabels.size(); i2++) {
                ConfigurableTexts.this.setTextLabelH((TextView) ConfigurableTexts.this.mTextLabels.keyAt(i2), ((Integer) ConfigurableTexts.this.mTextLabels.valueAt(i2)).intValue());
            }
        }
    };

    public ConfigurableTexts(Context context) {
        this.mContext = context;
    }

    public int add(TextView text) {
        return add(text, -1);
    }

    public int add(final TextView text, int labelResId) {
        if (text == null) {
            return 0;
        }
        Resources res = this.mContext.getResources();
        float fontScale = res.getConfiguration().fontScale;
        final int sp = (int) ((text.getTextSize() / fontScale) / res.getDisplayMetrics().density);
        this.mTexts.put(text, Integer.valueOf(sp));
        text.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            public void onViewDetachedFromWindow(View v) {
            }

            public void onViewAttachedToWindow(View v) {
                ConfigurableTexts.this.setTextSizeH(text, sp);
            }
        });
        this.mTextLabels.put(text, Integer.valueOf(labelResId));
        return sp;
    }

    public void update() {
        if (!this.mTexts.isEmpty()) {
            this.mTexts.keyAt(0).post(this.mUpdateAll);
        }
    }

    /* access modifiers changed from: private */
    public void setTextSizeH(TextView text, int sp) {
        text.setTextSize(2, (float) sp);
    }

    /* access modifiers changed from: private */
    public void setTextLabelH(TextView text, int labelResId) {
        if (labelResId >= 0) {
            try {
                Util.setText(text, this.mContext.getString(labelResId));
            } catch (Resources.NotFoundException e) {
            }
        }
    }
}
