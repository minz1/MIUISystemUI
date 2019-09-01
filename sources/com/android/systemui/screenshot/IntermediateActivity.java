package com.android.systemui.screenshot;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class IntermediateActivity extends Activity {
    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            Intent newIntent = (Intent) getIntent().getParcelableExtra("Intent");
            if (newIntent != null) {
                startActivityForResult(newIntent, 1);
            }
        }
    }

    /* access modifiers changed from: protected */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Uri uri = ((Intent) getIntent().getParcelableExtra("Intent")).getData();
        if (!(resultCode != -1 || data == null || data.getData() == null)) {
            uri = data.getData();
        }
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setPackage("com.miui.gallery");
        intent.setDataAndType(uri, "image/*");
        intent.putExtra("com.miui.gallery.extra.show_bars_when_enter", true);
        startActivity(intent);
        finish();
    }
}
