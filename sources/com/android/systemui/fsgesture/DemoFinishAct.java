package com.android.systemui.fsgesture;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import com.android.systemui.R;
import com.android.systemui.Util;
import com.android.systemui.recents.events.RecentsEventBus;
import com.android.systemui.recents.events.activity.FsGestureShowStateEvent;

public class DemoFinishAct extends Activity {
    TextView finishView;
    /* access modifiers changed from: private */
    public boolean isFromPro;
    TextView replayView;

    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(1024);
        setContentView(R.layout.fs_gesture_demo_final_view);
        Util.hideSystemBars(getWindow().getDecorView());
        Intent intent = getIntent();
        final String demoType = intent.getStringExtra("DEMO_TYPE");
        this.isFromPro = intent.getBooleanExtra("IS_FROM_PROVISION", false);
        this.replayView = (TextView) findViewById(R.id.fs_gesture_final_restart);
        this.replayView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent replayIntent = new Intent();
                if ("DEMO_TO_HOME".equals(demoType) || "DEMO_TO_RECENTTASK".equals(demoType)) {
                    replayIntent.setClass(DemoFinishAct.this, HomeDemoAct.class);
                    replayIntent.putExtra("DEMO_TYPE", demoType);
                } else if ("DEMO_FULLY_SHOW".equals(demoType)) {
                    replayIntent.setClass(DemoFinishAct.this, HomeDemoAct.class);
                    replayIntent.putExtra("DEMO_TYPE", demoType);
                    replayIntent.putExtra("FULLY_SHOW_STEP", 1);
                    replayIntent.putExtra("IS_FROM_PROVISION", DemoFinishAct.this.isFromPro);
                } else if ("FSG_BACK_GESTURE".equals(demoType)) {
                    replayIntent.setClass(DemoFinishAct.this, FsGestureBackDemoActivity.class);
                    replayIntent.putExtra("DEMO_TYPE", "FSG_BACK_GESTURE");
                }
                DemoFinishAct.this.startActivity(replayIntent);
                DemoFinishAct.this.overridePendingTransition(R.anim.activity_start_enter, R.anim.activity_start_exit);
                DemoFinishAct.this.finish();
            }
        });
        this.finishView = (TextView) findViewById(R.id.fs_gesture_final_over);
        this.finishView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (DemoFinishAct.this.isFromPro) {
                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName("com.android.provision", "com.android.provision.activities.NavigationModePickerActivity"));
                    intent.putExtra("IS_COMPLETE", true);
                    DemoFinishAct.this.startActivity(intent);
                    DemoFinishAct.this.overridePendingTransition(R.anim.activity_start_enter, R.anim.activity_start_exit);
                }
                DemoFinishAct.this.finish();
            }
        });
    }

    /* access modifiers changed from: protected */
    public void onResume() {
        super.onResume();
        RecentsEventBus.getDefault().send(new FsGestureShowStateEvent(true));
    }

    /* access modifiers changed from: protected */
    public void onPause() {
        super.onPause();
    }
}
