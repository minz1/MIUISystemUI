package com.android.systemui.volume;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Objects;

public class SegmentedButtons extends LinearLayout {
    private static final Typeface MEDIUM = Typeface.create("sans-serif-medium", 0);
    private static final Typeface REGULAR = Typeface.create("sans-serif", 0);
    private Callback mCallback;
    private final View.OnClickListener mClick = new View.OnClickListener() {
        public void onClick(View v) {
            SegmentedButtons.this.setSelectedValue(v.getTag(), true);
        }
    };
    private final ConfigurableTexts mConfigurableTexts;
    private final Context mContext;
    protected final LayoutInflater mInflater;
    protected Object mSelectedValue;

    public interface Callback {
        void onSelected(Object obj, boolean z);
    }

    public SegmentedButtons(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        this.mInflater = LayoutInflater.from(this.mContext);
        setOrientation(0);
        this.mConfigurableTexts = new ConfigurableTexts(this.mContext);
    }

    public void setSelectedValue(Object value, boolean fromClick) {
        if (!Objects.equals(value, this.mSelectedValue)) {
            this.mSelectedValue = value;
            for (int i = 0; i < getChildCount(); i++) {
                TextView c = (TextView) getChildAt(i);
                boolean selected = Objects.equals(this.mSelectedValue, c.getTag());
                c.setSelected(selected);
                setSelectedStyle(c, selected);
            }
            fireOnSelected(fromClick);
        }
    }

    /* access modifiers changed from: protected */
    public void setSelectedStyle(TextView textView, boolean selected) {
        textView.setTypeface(selected ? MEDIUM : REGULAR);
    }

    private void fireOnSelected(boolean fromClick) {
        if (this.mCallback != null) {
            this.mCallback.onSelected(this.mSelectedValue, fromClick);
        }
    }
}
