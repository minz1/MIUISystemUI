package com.android.keyguard.fod.item;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.RectF;
import android.graphics.Region;
import android.view.View;
import android.widget.ImageView;
import com.android.systemui.R;

public class AddEventItem extends IQuickOpenItem {
    private final ImageView mView;

    public AddEventItem(RectF rectF, Region region, Context context) {
        super(rectF, region, context);
        this.mView = new ImageView(context);
        this.mView.setImageResource(R.drawable.gxzw_quick_open_add_event);
        this.mView.setScaleType(ImageView.ScaleType.FIT_XY);
    }

    public View getView() {
        return this.mView;
    }

    public Intent getIntent() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.android.calendar", "com.android.calendar.event.EditEventActivity"));
        intent.putExtra("from", "fingerprint");
        intent.setFlags(343932928);
        return intent;
    }

    public String getTitle() {
        return this.mContext.getString(R.string.gxzw_quick_open_add_event);
    }

    public String getSubTitle() {
        return this.mContext.getString(R.string.gxzw_quick_open_add_event_sub);
    }
}
