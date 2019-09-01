package com.android.systemui.settings;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.SeekBar;
import com.android.settingslib.RestrictedLockUtils;
import com.android.systemui.Dependency;
import com.android.systemui.miui.widget.RelativeSeekBarInjector;
import com.android.systemui.plugins.ActivityStarter;

public class ToggleSeekBar extends SeekBar {
    private String mAccessibilityLabel;
    private RestrictedLockUtils.EnforcedAdmin mEnforcedAdmin;
    private RelativeSeekBarInjector mInjector;

    public ToggleSeekBar(Context context) {
        this(context, null);
    }

    public ToggleSeekBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ToggleSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mEnforcedAdmin = null;
        initInjector();
    }

    private void initInjector() {
        this.mInjector = new RelativeSeekBarInjector(this, false);
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (this.mEnforcedAdmin != null) {
            ((ActivityStarter) Dependency.get(ActivityStarter.class)).postStartActivityDismissingKeyguard(RestrictedLockUtils.getShowAdminSupportDetailsIntent(this.mContext, this.mEnforcedAdmin), 0);
            return true;
        }
        if (!isEnabled()) {
            setEnabled(true);
        }
        if (this.mInjector != null) {
            this.mInjector.transformTouchEvent(event);
        }
        return super.onTouchEvent(event);
    }

    public void setAccessibilityLabel(String label) {
        this.mAccessibilityLabel = label;
    }

    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        if (this.mAccessibilityLabel != null) {
            info.setText(this.mAccessibilityLabel);
        }
    }
}
