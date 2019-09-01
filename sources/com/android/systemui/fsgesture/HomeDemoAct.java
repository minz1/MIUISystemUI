package com.android.systemui.fsgesture;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import com.android.systemui.Constants;
import com.android.systemui.R;
import com.android.systemui.Util;
import com.android.systemui.recents.events.RecentsEventBus;
import com.android.systemui.recents.events.activity.FsGestureShowStateEvent;

public class HomeDemoAct extends Activity {
    public static final String TAG = HomeDemoAct.class.getSimpleName();
    private View appBgView;
    private View appNoteImg;
    /* access modifiers changed from: private */
    public FsGestureDemoSwipeView fsGestureDemoSwipeView;
    private FsGestureDemoTitleView fsGestureDemoTitleView;
    /* access modifiers changed from: private */
    public NavStubDemoView fsgNavView;
    Handler handler = new Handler();
    private LinearLayout homeIconImg;
    /* access modifiers changed from: private */
    public View mAnimIcon;
    /* access modifiers changed from: private */
    public LinearLayout mRecentsCardContainer;
    private View mRecentsFirstCardIconView;
    private View navSubViewBgView;
    private View recentsBgView;
    private ImageView wallPaperImg;

    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        int showType;
        super.onCreate(savedInstanceState);
        getWindow().addFlags(1024);
        setContentView(R.layout.home_demo_layout);
        Util.hideSystemBars(getWindow().getDecorView());
        Intent intent = getIntent();
        String demoType = intent.getStringExtra("DEMO_TYPE");
        int step = intent.getIntExtra("FULLY_SHOW_STEP", 1);
        boolean isFromPro = intent.getBooleanExtra("IS_FROM_PROVISION", false);
        this.wallPaperImg = (ImageView) findViewById(R.id.wallpaper_img);
        this.homeIconImg = (LinearLayout) findViewById(R.id.home_icon_img);
        this.mAnimIcon = (ImageView) findViewById(R.id.anim_icon);
        this.mAnimIcon.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            public void onGlobalLayout() {
                HomeDemoAct.this.mAnimIcon.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                int[] loc = HomeDemoAct.this.mAnimIcon.getLocationOnScreen();
                loc[0] = loc[0] + (HomeDemoAct.this.mAnimIcon.getWidth() / 2);
                loc[1] = loc[1] + (HomeDemoAct.this.mAnimIcon.getHeight() / 2);
                if (HomeDemoAct.this.fsgNavView != null) {
                    HomeDemoAct.this.fsgNavView.setDestPivot(loc[0], loc[1]);
                }
            }
        });
        this.recentsBgView = findViewById(R.id.recents_bg_view);
        this.mRecentsCardContainer = (LinearLayout) findViewById(R.id.recents_card_container);
        this.mRecentsFirstCardIconView = findViewById(R.id.recents_first_card_icon);
        this.mRecentsCardContainer.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            public void onGlobalLayout() {
                HomeDemoAct.this.mRecentsCardContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                Rect bound = new Rect();
                ((ImageView) HomeDemoAct.this.findViewById(R.id.recents_first_card)).getBoundsOnScreen(bound);
                if (HomeDemoAct.this.fsgNavView != null) {
                    HomeDemoAct.this.fsgNavView.setRecentsFirstCardBound(bound);
                }
            }
        });
        this.mRecentsFirstCardIconView = findViewById(R.id.recents_first_card_icon);
        this.appBgView = findViewById(R.id.app_bg_view);
        this.appNoteImg = findViewById(R.id.app_note_img);
        this.navSubViewBgView = findViewById(R.id.navstubview_bg_view);
        this.fsGestureDemoTitleView = (FsGestureDemoTitleView) findViewById(R.id.fsgesture_title_view);
        if ("DEMO_FULLY_SHOW".equals(demoType)) {
            if (step == 1) {
                showType = 2;
            } else {
                showType = 3;
            }
        } else if ("DEMO_TO_HOME".equals(demoType)) {
            showType = 2;
        } else {
            showType = 3;
        }
        this.fsGestureDemoTitleView.prepareTitleView(showType);
        this.fsGestureDemoTitleView.registerSkipEvent(new View.OnClickListener() {
            public void onClick(View v) {
                HomeDemoAct.this.finish();
            }
        });
        if (Constants.IS_NOTCH) {
            int naturalBarHeight = getResources().getDimensionPixelSize(R.dimen.status_bar_height);
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) this.fsGestureDemoTitleView.getLayoutParams();
            layoutParams.setMargins(0, naturalBarHeight, 0, 0);
            this.fsGestureDemoTitleView.setLayoutParams(layoutParams);
        }
        this.fsGestureDemoSwipeView = (FsGestureDemoSwipeView) findViewById(R.id.fsgesture_swipe_view);
        if (showType == 3) {
            startSwipeViewAnimation(4);
        } else {
            startSwipeViewAnimation(2);
        }
        this.fsgNavView = (NavStubDemoView) findViewById(R.id.fsg_nav_view);
        this.fsgNavView.setCurActivity(this);
        this.fsgNavView.setDemoType(demoType);
        this.fsgNavView.setFullyShowStep(step);
        this.fsgNavView.setIsFromPro(isFromPro);
        this.fsgNavView.setHomeIconImg(this.homeIconImg);
        this.fsgNavView.setRecentsBgView(this.recentsBgView);
        this.fsgNavView.setRecentsCardContainer(this.mRecentsCardContainer);
        this.fsgNavView.setRecentsFirstCardIconView(this.mRecentsFirstCardIconView);
        this.fsgNavView.setAppBgView(this.appBgView);
        this.fsgNavView.setAppNoteImg(this.appNoteImg);
        this.fsgNavView.setDemoTitleView(this.fsGestureDemoTitleView);
        this.fsgNavView.setSwipeView(this.fsGestureDemoSwipeView);
        this.fsgNavView.setBgView(this.navSubViewBgView);
    }

    private void startSwipeViewAnimation(final int status) {
        this.handler.postDelayed(new Runnable() {
            public void run() {
                HomeDemoAct.this.fsGestureDemoSwipeView.prepare(status);
                HomeDemoAct.this.fsGestureDemoSwipeView.startAnimation(status);
            }
        }, 500);
    }

    /* access modifiers changed from: protected */
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
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
