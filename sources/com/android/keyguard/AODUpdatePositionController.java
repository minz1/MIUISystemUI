package com.android.keyguard;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import com.android.keyguard.widget.AODSettings;
import com.android.systemui.R;

public class AODUpdatePositionController {
    private static final String TAG = AODUpdatePositionController.class.getSimpleName();
    private int mAodMoveCurrent;
    private int mAodMovePeriod;
    private boolean mIsDisplayUpdateModeOn;
    private int mSunTranlationY;
    private int mTranslationX;
    private int mTranslationY;

    public AODUpdatePositionController(Context context) {
        this.mIsDisplayUpdateModeOn = Settings.Secure.getInt(context.getContentResolver(), "aod_display_update_mode", 1) != 1 ? false : true;
        this.mAodMovePeriod = Settings.Secure.getInt(context.getContentResolver(), "aod_move_period", 42);
        this.mTranslationX = context.getResources().getDimensionPixelSize(R.dimen.clock_container_translation_x);
        this.mTranslationY = context.getResources().getDimensionPixelSize(R.dimen.clock_container_translation_y);
        this.mAodMoveCurrent = Settings.Secure.getInt(context.getContentResolver(), "aod_move_current", 20);
        this.mSunTranlationY = getTranslationY(context);
    }

    public void updatePosition(View container) {
        if (!this.mIsDisplayUpdateModeOn) {
            Log.e(TAG, "updatePosition() blocking on setting value");
            return;
        }
        this.mAodMoveCurrent %= this.mAodMovePeriod;
        int position = this.mAodMoveCurrent / 2;
        container.setTranslationX((float) (this.mTranslationX * ((position % 3) - 1)));
        container.setTranslationY((float) ((this.mTranslationY * ((position / 3) - 3)) + this.mSunTranlationY));
        this.mAodMoveCurrent++;
    }

    private int getTranslationY(Context context) {
        if (!AODSettings.isHighPerformace() || Settings.System.getIntForUser(context.getContentResolver(), "auto_dual_clock", 0, -2) == 1 || Settings.Secure.getIntForUser(context.getContentResolver(), "aod_style_index", 0, -2) != 0) {
            return 0;
        }
        return context.getResources().getDimensionPixelOffset(R.dimen.sun_translation_y);
    }
}
