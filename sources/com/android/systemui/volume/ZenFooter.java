package com.android.systemui.volume;

import android.animation.LayoutTransition;
import android.animation.ValueAnimator;
import android.content.Context;
import android.service.notification.Condition;
import android.service.notification.ZenModeConfig;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.systemui.Prefs;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.ZenModeController;
import java.util.Objects;

public class ZenFooter extends LinearLayout {
    private static final String TAG = Util.logTag(ZenFooter.class);
    private ZenModeConfig mConfig;
    private final ConfigurableTexts mConfigurableTexts;
    private final Context mContext;
    private ZenModeController mController;
    private TextView mEndNowButton;
    private ImageView mIcon;
    private TextView mSummaryLine1;
    private TextView mSummaryLine2;
    private int mZen = -1;
    private final ZenModeController.Callback mZenCallback = new ZenModeController.Callback() {
        public void onZenChanged(int zen) {
            ZenFooter.this.setZen(zen);
        }

        public void onConditionsChanged(Condition[] conditions) {
        }

        public void onNextAlarmChanged() {
        }

        public void onZenAvailableChanged(boolean available) {
        }

        public void onEffectsSupressorChanged() {
        }

        public void onManualRuleChanged(ZenModeConfig.ZenRule rule) {
        }

        public void onConfigChanged(ZenModeConfig config) {
            ZenFooter.this.setConfig(config);
        }
    };
    private View mZenIntroduction;
    private View mZenIntroductionConfirm;
    private TextView mZenIntroductionMessage;

    public ZenFooter(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        this.mConfigurableTexts = new ConfigurableTexts(this.mContext);
        LayoutTransition layoutTransition = new LayoutTransition();
        layoutTransition.setDuration(new ValueAnimator().getDuration() / 2);
        setLayoutTransition(layoutTransition);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mIcon = (ImageView) findViewById(R.id.volume_zen_icon);
        this.mSummaryLine1 = (TextView) findViewById(R.id.volume_zen_summary_line_1);
        this.mSummaryLine2 = (TextView) findViewById(R.id.volume_zen_summary_line_2);
        this.mEndNowButton = (TextView) findViewById(R.id.volume_zen_end_now);
        this.mZenIntroduction = findViewById(R.id.zen_introduction);
        this.mZenIntroductionMessage = (TextView) findViewById(R.id.zen_introduction_message);
        this.mConfigurableTexts.add(this.mZenIntroductionMessage, R.string.zen_alarms_introduction);
        this.mZenIntroductionConfirm = findViewById(R.id.zen_introduction_confirm);
        this.mZenIntroductionConfirm.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ZenFooter.this.confirmZenIntroduction();
            }
        });
        Util.setVisOrGone(this.mZenIntroduction, shouldShowIntroduction());
        this.mConfigurableTexts.add(this.mSummaryLine1);
        this.mConfigurableTexts.add(this.mSummaryLine2);
        this.mConfigurableTexts.add(this.mEndNowButton, R.string.volume_zen_end_now);
    }

    /* access modifiers changed from: private */
    public void setZen(int zen) {
        if (this.mZen != zen) {
            this.mZen = zen;
            update();
            updateIntroduction();
        }
    }

    /* access modifiers changed from: private */
    public void setConfig(ZenModeConfig config) {
        if (!Objects.equals(this.mConfig, config)) {
            this.mConfig = config;
            update();
        }
    }

    /* access modifiers changed from: private */
    public void confirmZenIntroduction() {
        Prefs.putBoolean(this.mContext, "DndConfirmedAlarmIntroduction", true);
        updateIntroduction();
    }

    private boolean isZenPriority() {
        return this.mZen == 1;
    }

    private boolean isZenAlarms() {
        return this.mZen == 3;
    }

    private boolean isZenNone() {
        return this.mZen == 2;
    }

    public void update() {
        String line1;
        this.mIcon.setImageResource(isZenNone() ? R.drawable.ic_dnd_total_silence : R.drawable.ic_dnd);
        if (isZenPriority()) {
            line1 = this.mContext.getString(R.string.interruption_level_priority);
        } else if (isZenAlarms()) {
            line1 = this.mContext.getString(R.string.interruption_level_alarms);
        } else if (isZenNone()) {
            line1 = this.mContext.getString(R.string.interruption_level_none);
        } else {
            line1 = null;
        }
        Util.setText(this.mSummaryLine1, line1);
        Util.setText(this.mSummaryLine2, ZenModeConfig.getConditionSummary(this.mContext, this.mConfig, this.mController.getCurrentUser(), true));
    }

    public boolean shouldShowIntroduction() {
        if (Prefs.getBoolean(this.mContext, "DndConfirmedAlarmIntroduction", false) || !isZenAlarms()) {
            return false;
        }
        return true;
    }

    public void updateIntroduction() {
        Util.setVisOrGone(this.mZenIntroduction, shouldShowIntroduction());
    }
}
