package com.android.systemui.qs.tiles;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.settingslib.net.DataUsageController;
import com.android.systemui.FontUtils;
import com.android.systemui.R;
import com.android.systemui.qs.DataUsageGraph;
import com.android.systemui.util.Utils;
import java.text.DecimalFormat;

public class DataUsageDetailView extends LinearLayout {
    private final DecimalFormat FORMAT = new DecimalFormat("#.##");

    public DataUsageDetailView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        FontUtils.updateFontSize(this, 16908310, R.dimen.qs_data_usage_text_size);
        FontUtils.updateFontSize(this, R.id.usage_text, R.dimen.qs_data_usage_usage_text_size);
        FontUtils.updateFontSize(this, R.id.usage_carrier_text, R.dimen.qs_data_usage_text_size);
        FontUtils.updateFontSize(this, R.id.usage_info_top_text, R.dimen.qs_data_usage_text_size);
        FontUtils.updateFontSize(this, R.id.usage_period_text, R.dimen.qs_data_usage_text_size);
        FontUtils.updateFontSize(this, R.id.usage_info_bottom_text, R.dimen.qs_data_usage_text_size);
    }

    public void bind(DataUsageController.DataUsageInfo info) {
        long bytes;
        String top;
        int titleId;
        DataUsageController.DataUsageInfo dataUsageInfo = info;
        Resources res = this.mContext.getResources();
        int usageColor = 0;
        String bottom = null;
        if (dataUsageInfo.usageLevel < dataUsageInfo.warningLevel || dataUsageInfo.limitLevel <= 0) {
            titleId = R.string.quick_settings_cellular_detail_data_usage;
            bytes = dataUsageInfo.usageLevel;
            top = res.getString(R.string.quick_settings_cellular_detail_data_warning, new Object[]{formatBytes(dataUsageInfo.warningLevel)});
        } else if (dataUsageInfo.usageLevel <= dataUsageInfo.limitLevel) {
            titleId = R.string.quick_settings_cellular_detail_remaining_data;
            bytes = dataUsageInfo.limitLevel - dataUsageInfo.usageLevel;
            top = res.getString(R.string.quick_settings_cellular_detail_data_used, new Object[]{formatBytes(dataUsageInfo.usageLevel)});
            bottom = res.getString(R.string.quick_settings_cellular_detail_data_limit, new Object[]{formatBytes(dataUsageInfo.limitLevel)});
        } else {
            titleId = R.string.quick_settings_cellular_detail_over_limit;
            bytes = dataUsageInfo.usageLevel - dataUsageInfo.limitLevel;
            top = res.getString(R.string.quick_settings_cellular_detail_data_used, new Object[]{formatBytes(dataUsageInfo.usageLevel)});
            bottom = res.getString(R.string.quick_settings_cellular_detail_data_limit, new Object[]{formatBytes(dataUsageInfo.limitLevel)});
            usageColor = Utils.getDefaultColor(this.mContext, R.color.color_error);
        }
        if (usageColor == 0) {
            usageColor = Utils.getColorAccent(this.mContext);
        }
        ((TextView) findViewById(16908310)).setText(titleId);
        TextView usage = (TextView) findViewById(R.id.usage_text);
        usage.setText(formatBytes(bytes));
        usage.setTextColor(usageColor);
        DataUsageGraph graph = (DataUsageGraph) findViewById(R.id.usage_graph);
        Resources resources = res;
        int i = usageColor;
        graph.setLevels(dataUsageInfo.limitLevel, dataUsageInfo.warningLevel, dataUsageInfo.usageLevel);
        ((TextView) findViewById(R.id.usage_carrier_text)).setText(dataUsageInfo.carrier);
        ((TextView) findViewById(R.id.usage_period_text)).setText(dataUsageInfo.period);
        TextView infoTop = (TextView) findViewById(R.id.usage_info_top_text);
        infoTop.setVisibility(top != null ? 0 : 8);
        infoTop.setText(top);
        TextView infoBottom = (TextView) findViewById(R.id.usage_info_bottom_text);
        infoBottom.setVisibility(bottom != null ? 0 : 8);
        infoBottom.setText(bottom);
        TextView textView = usage;
        boolean showLevel = dataUsageInfo.warningLevel > 0 || dataUsageInfo.limitLevel > 0;
        graph.setVisibility(showLevel ? 0 : 8);
        if (!showLevel) {
            infoTop.setVisibility(8);
        }
    }

    private String formatBytes(long bytes) {
        String suffix;
        double val;
        long b = Math.abs(bytes);
        if (((double) b) > 1.048576E8d) {
            val = ((double) b) / 1.073741824E9d;
            suffix = "GB";
        } else if (((double) b) > 102400.0d) {
            val = ((double) b) / 1048576.0d;
            suffix = "MB";
        } else {
            val = ((double) b) / 1024.0d;
            suffix = "KB";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(this.FORMAT.format(((double) (bytes < 0 ? -1 : 1)) * val));
        sb.append(" ");
        sb.append(suffix);
        return sb.toString();
    }
}
