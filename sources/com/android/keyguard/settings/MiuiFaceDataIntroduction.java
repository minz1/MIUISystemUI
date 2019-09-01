package com.android.keyguard.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import com.android.keyguard.analytics.AnalyticsHelper;
import com.android.systemui.R;
import miui.preference.PreferenceActivity;

public class MiuiFaceDataIntroduction extends PreferenceActivity {
    private Button mNextButton;

    public void onCreate(Bundle bundle) {
        MiuiFaceDataIntroduction.super.onCreate(bundle);
        setContentView(R.layout.miui_add_face_data_introduction);
        this.mNextButton = (Button) findViewById(R.id.miui_face_recoginition_intorduction_next);
        this.mNextButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                AnalyticsHelper.record("face_unlock_click_introduction");
                Intent intent = new Intent();
                intent.setClassName("com.android.settings", "com.android.settings.MiuiSecurityChooseUnlock");
                intent.putExtra("add_keyguard_password_then_add_face_recoginition", true);
                MiuiFaceDataIntroduction.this.startActivityForResult(intent, 1);
            }
        });
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        MiuiFaceDataIntroduction.super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == -1) {
                setResult(-1);
            } else {
                setResult(0);
            }
            finish();
        }
    }
}
