package com.android.keyguard.settings;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import com.android.keyguard.analytics.AnalyticsHelper;
import com.android.systemui.R;
import miui.preference.PreferenceActivity;

public class MiuiFaceDataSuggestion extends PreferenceActivity {
    private CountDownTimer mCountdownTimer;

    public void onCreate(Bundle bundle) {
        MiuiFaceDataSuggestion.super.onCreate(bundle);
        setContentView(R.layout.miui_add_face_data_suggesstion);
        Button next = (Button) findViewById(R.id.miui_face_recoginition_suggestion_next);
        next.setEnabled(false);
        next.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                AnalyticsHelper.record("face_unlock_click_suggestion");
                MiuiFaceDataSuggestion.this.setResult(-1);
                MiuiFaceDataSuggestion.this.finish();
            }
        });
        final Button button = next;
        AnonymousClass2 r1 = new CountDownTimer(5000, 1000) {
            public void onTick(long millisUntilFinished) {
                button.setText(MiuiFaceDataSuggestion.this.getResources().getString(R.string.face_data_siggesstion_next_time, new Object[]{Long.valueOf(millisUntilFinished / 1000)}));
            }

            public void onFinish() {
                button.setEnabled(true);
                button.setText(MiuiFaceDataSuggestion.this.getResources().getString(R.string.face_data_siggesstion_next));
                button.setTextColor(MiuiFaceDataSuggestion.this.getResources().getColor(R.color.miui_face_data_regular_text_color));
            }
        };
        this.mCountdownTimer = r1.start();
    }

    public void onDestroy() {
        MiuiFaceDataSuggestion.super.onDestroy();
        if (this.mCountdownTimer != null) {
            this.mCountdownTimer.cancel();
            this.mCountdownTimer = null;
        }
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == 0 && event.getKeyCode() == 4) {
            setResult(0);
            finish();
        }
        return true;
    }
}
