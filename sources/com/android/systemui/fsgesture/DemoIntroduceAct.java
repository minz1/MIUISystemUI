package com.android.systemui.fsgesture;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import com.android.systemui.R;
import com.android.systemui.Util;
import com.android.systemui.recents.events.RecentsEventBus;
import com.android.systemui.recents.events.activity.FsGestureShowStateEvent;

public class DemoIntroduceAct extends Activity {
    TextView backBtn;
    TextView nextBtn;

    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(1024);
        setContentView(R.layout.demo_intro_layout);
        Util.hideSystemBars(getWindow().getDecorView());
        final boolean isFromPro = getIntent().getBooleanExtra("IS_FROM_PROVISION", false);
        this.backBtn = (TextView) findViewById(R.id.btn_back);
        this.backBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                DemoIntroduceAct.this.finish();
            }
        });
        this.nextBtn = (TextView) findViewById(R.id.btn_next);
        this.nextBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setClass(DemoIntroduceAct.this, HomeDemoAct.class);
                intent.putExtra("DEMO_TYPE", "DEMO_FULLY_SHOW");
                intent.putExtra("FULLY_SHOW_STEP", 1);
                intent.putExtra("IS_FROM_PROVISION", isFromPro);
                DemoIntroduceAct.this.startActivity(intent);
                DemoIntroduceAct.this.finish();
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
