package com.android.keyguard.fod.item;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.RectF;
import android.graphics.Region;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import com.android.systemui.R;

public class AlipayScanItem extends IQuickOpenItem {
    private final ImageView mView;

    public AlipayScanItem(RectF rectF, Region region, Context context) {
        super(rectF, region, context);
        this.mView = new ImageView(context);
        this.mView.setImageResource(R.drawable.gxzw_quick_open_alipay_scan);
        this.mView.setScaleType(ImageView.ScaleType.FIT_XY);
    }

    public View getView() {
        return this.mView;
    }

    public Intent getIntent() {
        Intent intent = new Intent();
        intent.setAction("android.intent.action.VIEW");
        intent.setComponent(new ComponentName("com.eg.android.AlipayGphone", "com.alipay.mobile.scan.as.main.MainCaptureActivity"));
        intent.setFlags(343932928);
        Bundle bundle = new Bundle();
        bundle.putString("app_id", "10000007");
        Bundle bundleTemp = new Bundle();
        bundleTemp.putString("source", "shortcut");
        bundleTemp.putString("appId", "10000007");
        bundleTemp.putBoolean("REALLY_STARTAPP", true);
        bundleTemp.putString("showOthers", "YES");
        bundleTemp.putBoolean("startFromExternal", true);
        bundleTemp.putBoolean("REALLY_DOSTARTAPP", true);
        bundleTemp.putString("sourceId", "shortcut");
        bundleTemp.putString("ap_framework_sceneId", "20000001");
        bundle.putBundle("mExtras", bundleTemp);
        intent.putExtras(bundle);
        return intent;
    }

    public String getTitle() {
        return this.mContext.getString(R.string.gxzw_quick_open_alipay_scan);
    }

    public String getSubTitle() {
        return this.mContext.getString(R.string.gxzw_quick_open_alipay_scan_sub);
    }
}
