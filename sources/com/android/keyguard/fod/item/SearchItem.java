package com.android.keyguard.fod.item;

import android.content.Context;
import android.content.Intent;
import android.graphics.RectF;
import android.graphics.Region;
import android.view.View;
import android.widget.ImageView;
import com.android.systemui.R;

public class SearchItem extends IQuickOpenItem {
    private final ImageView mView;

    public SearchItem(RectF rectF, Region region, Context context) {
        super(rectF, region, context);
        this.mView = new ImageView(context);
        this.mView.setImageResource(R.drawable.gxzw_quick_open_search);
        this.mView.setScaleType(ImageView.ScaleType.FIT_XY);
    }

    public View getView() {
        return this.mView;
    }

    public Intent getIntent() {
        Intent intent = new Intent("com.android.browser.browser_search");
        intent.setFlags(343932928);
        intent.setPackage("com.android.browser");
        intent.putExtra("from", "fingerprint");
        return intent;
    }

    public String getTitle() {
        return this.mContext.getString(R.string.gxzw_quick_open_search);
    }

    public String getSubTitle() {
        return this.mContext.getString(R.string.gxzw_quick_open_search_sub);
    }
}
