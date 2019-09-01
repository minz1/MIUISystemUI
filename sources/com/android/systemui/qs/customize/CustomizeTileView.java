package com.android.systemui.qs.customize;

import android.content.Context;
import android.widget.TextView;
import com.android.systemui.plugins.qs.QSIconView;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.tileimpl.QSTileView;

public class CustomizeTileView extends QSTileView {
    private boolean mShowAppLabel;

    public CustomizeTileView(Context context, QSIconView icon) {
        super(context, icon);
        setGravity(49);
    }

    public void setShowAppLabel(boolean showAppLabel) {
        this.mShowAppLabel = showAppLabel;
        this.mSecondLine.setVisibility(showAppLabel ? 0 : 8);
    }

    /* access modifiers changed from: protected */
    public void handleStateChanged(QSTile.State state) {
        super.handleStateChanged(state);
        this.mSecondLine.setVisibility(this.mShowAppLabel ? 0 : 8);
    }

    public TextView getAppLabel() {
        return this.mSecondLine;
    }
}
