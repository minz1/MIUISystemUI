package android.widget;

import android.content.Context;
import android.util.AttributeSet;

public abstract class AbstractFrameLayout extends FrameLayout {
    public AbstractFrameLayout(Context context) {
        super(context);
    }

    public AbstractFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AbstractFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public AbstractFrameLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void onVisibilityAggregated(boolean isVisible) {
        super.onVisibilityAggregated(isVisible);
    }

    public boolean hasOverlappingRendering() {
        return false;
    }
}
