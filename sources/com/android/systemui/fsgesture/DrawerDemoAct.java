package com.android.systemui.fsgesture;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import com.android.systemui.Constants;
import com.android.systemui.R;
import com.android.systemui.Util;
import com.android.systemui.recents.events.RecentsEventBus;
import com.android.systemui.recents.events.activity.FsGestureShowStateEvent;
import miui.os.Build;

public class DrawerDemoAct extends Activity {
    /* access modifiers changed from: private */
    public static final boolean IS_DEBUG = Build.IS_ALPHA_BUILD;
    public static final String TAG = DrawerDemoAct.class.getSimpleName();
    /* access modifiers changed from: private */
    public ImageView drawerImg;
    /* access modifiers changed from: private */
    public FsGestureDemoSwipeView fsGestureDemoSwipeView;
    /* access modifiers changed from: private */
    public FsGestureDemoTitleView fsGestureDemoTitleView;
    Handler handler = new Handler();
    /* access modifiers changed from: private */
    public int initTranslateWidht;
    /* access modifiers changed from: private */
    public int maxTranslateWidth;
    /* access modifiers changed from: private */
    public View shelterView;

    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(1024);
        setContentView(R.layout.drawer_demo_layout);
        Util.hideSystemBars(getWindow().getDecorView());
        this.drawerImg = (ImageView) findViewById(R.id.drawer_img);
        this.shelterView = findViewById(R.id.shelter_view);
        this.shelterView.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                float f;
                int action = event.getAction();
                float rawX = event.getRawX();
                switch (action) {
                    case 0:
                        DrawerDemoAct.this.fsGestureDemoSwipeView.cancelAnimation();
                        ViewGroup.LayoutParams layoutParams = DrawerDemoAct.this.shelterView.getLayoutParams();
                        layoutParams.width = -1;
                        DrawerDemoAct.this.shelterView.setLayoutParams(layoutParams);
                        break;
                    case 1:
                    case 3:
                        ViewGroup.LayoutParams layoutParams2 = DrawerDemoAct.this.shelterView.getLayoutParams();
                        layoutParams2.width = DrawerDemoAct.this.getResources().getDimensionPixelSize(R.dimen.fsgesture_shelter_width);
                        DrawerDemoAct.this.shelterView.setLayoutParams(layoutParams2);
                        if (rawX < ((float) (DrawerDemoAct.this.drawerImg.getWidth() / 2))) {
                            DrawerDemoAct.this.startSwipeViewAnimation(3);
                            DrawerDemoAct.this.drawerImg.animate().translationX((float) DrawerDemoAct.this.initTranslateWidht).setDuration(200).start();
                            break;
                        } else {
                            DrawerDemoAct.this.drawerImg.animate().translationX((float) DrawerDemoAct.this.maxTranslateWidth).setDuration(200).start();
                            DrawerDemoAct.this.fsGestureDemoTitleView.notifyFinish();
                            DrawerDemoAct.this.handler.postDelayed(new Runnable() {
                                public void run() {
                                    DrawerDemoAct.this.finish();
                                }
                            }, 1000);
                            break;
                        }
                    case 2:
                        ImageView access$200 = DrawerDemoAct.this.drawerImg;
                        if (rawX >= ((float) DrawerDemoAct.this.drawerImg.getWidth())) {
                            f = (float) DrawerDemoAct.this.maxTranslateWidth;
                        } else {
                            f = rawX - ((float) DrawerDemoAct.this.drawerImg.getWidth());
                        }
                        access$200.setTranslationX(f);
                        break;
                }
                return true;
            }
        });
        this.drawerImg.post(new Runnable() {
            public void run() {
                int width = DrawerDemoAct.this.drawerImg.getWidth();
                if (DrawerDemoAct.IS_DEBUG) {
                    String str = DrawerDemoAct.TAG;
                    Log.d(str, "====>>>> width:" + width);
                }
                int unused = DrawerDemoAct.this.initTranslateWidht = -width;
                int unused2 = DrawerDemoAct.this.maxTranslateWidth = 0;
                DrawerDemoAct.this.drawerImg.setTranslationX((float) DrawerDemoAct.this.initTranslateWidht);
            }
        });
        this.fsGestureDemoTitleView = (FsGestureDemoTitleView) findViewById(R.id.fsgesture_title_view);
        this.fsGestureDemoTitleView.prepareTitleView(4);
        this.fsGestureDemoTitleView.registerSkipEvent(new View.OnClickListener() {
            public void onClick(View v) {
                DrawerDemoAct.this.finish();
            }
        });
        if (Constants.IS_NOTCH) {
            int naturalBarHeight = getResources().getDimensionPixelSize(R.dimen.status_bar_height);
            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) this.fsGestureDemoTitleView.getLayoutParams();
            lp.setMargins(0, naturalBarHeight, 0, 0);
            this.fsGestureDemoTitleView.setLayoutParams(lp);
        }
        this.fsGestureDemoSwipeView = (FsGestureDemoSwipeView) findViewById(R.id.fsgesture_swipe_view);
        startSwipeViewAnimation(3);
    }

    /* access modifiers changed from: private */
    public void startSwipeViewAnimation(final int status) {
        this.handler.postDelayed(new Runnable() {
            public void run() {
                DrawerDemoAct.this.fsGestureDemoSwipeView.prepare(status);
                DrawerDemoAct.this.fsGestureDemoSwipeView.startAnimation(status);
            }
        }, 500);
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
