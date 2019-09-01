package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.KeyButtonRipple;

public class NavigationBarViewTaskSwitchHelper extends GestureDetector.SimpleOnGestureListener {
    private StatusBar mBar;
    private Context mContext;
    private boolean mIsRTL;
    private boolean mIsVertical;
    private final int mMinFlingVelocity;
    private final int mScrollTouchSlop;
    private final GestureDetector mTaskSwitcherDetector;
    private int mTouchDownX;
    private int mTouchDownY;

    public NavigationBarViewTaskSwitchHelper(Context context) {
        this.mContext = context;
        ViewConfiguration configuration = ViewConfiguration.get(context);
        this.mScrollTouchSlop = context.getResources().getDimensionPixelSize(R.dimen.navigation_bar_size);
        this.mMinFlingVelocity = configuration.getScaledMinimumFlingVelocity();
        this.mTaskSwitcherDetector = new GestureDetector(context, this);
    }

    public void setBar(StatusBar phoneStatusBar) {
        this.mBar = phoneStatusBar;
    }

    public void setBarState(boolean isVertical, boolean isRTL) {
        this.mIsVertical = isVertical;
        this.mIsRTL = isRTL;
    }

    public boolean onInterceptTouchEvent(MotionEvent event) {
        this.mTaskSwitcherDetector.onTouchEvent(event);
        int action = event.getAction() & 255;
        if (action == 0) {
            this.mTouchDownX = (int) event.getX();
            this.mTouchDownY = (int) event.getY();
        } else if (action == 2) {
            int xDiff = Math.abs(((int) event.getX()) - this.mTouchDownX);
            int yDiff = Math.abs(((int) event.getY()) - this.mTouchDownY);
            boolean exceededTouchSlop = false;
            if (this.mIsVertical ? !(yDiff <= this.mScrollTouchSlop || yDiff <= xDiff) : !(xDiff <= this.mScrollTouchSlop || xDiff <= yDiff)) {
                exceededTouchSlop = true;
            }
            if (exceededTouchSlop) {
                return true;
            }
        }
        return false;
    }

    public boolean onTouchEvent(MotionEvent event) {
        return this.mTaskSwitcherDetector.onTouchEvent(event);
    }

    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        View rightButton;
        View leftButton;
        float absVelX = Math.abs(velocityX);
        float absVelY = Math.abs(velocityY);
        int xDiff = Math.abs((int) (e2.getX() - e1.getX()));
        if (!this.mIsVertical && ((float) xDiff) > ((float) Math.abs((int) (e2.getY() - e1.getY()))) * 2.0f && xDiff > this.mScrollTouchSlop && absVelX > ((float) this.mMinFlingVelocity) && e2.getY() > 0.0f) {
            NavigationBarView navigationBarView = this.mBar.getNavigationBarView();
            KeyButtonRipple background = (KeyButtonRipple) navigationBarView.getHomeButton().getBackground();
            int[] location = navigationBarView.getHomeButton().getLocationOnScreen();
            Rect homeRect = new Rect(location[0], location[1], location[0] + navigationBarView.getHomeButton().getWidth(), location[1] + navigationBarView.getHomeButton().getHeight());
            if (NavigationBarView.getScreenKeyOrder(this.mContext).get(0).intValue() == 3) {
                leftButton = navigationBarView.getBackButton();
                rightButton = navigationBarView.getRecentsButton();
            } else {
                leftButton = navigationBarView.getRecentsButton();
                rightButton = navigationBarView.getBackButton();
            }
            int[] location2 = leftButton.getLocationOnScreen();
            float f = absVelX;
            float f2 = absVelY;
            Rect leftRect = new Rect(location2[0], location2[1], location2[0] + leftButton.getWidth(), location2[1] + leftButton.getHeight());
            int[] location3 = rightButton.getLocationOnScreen();
            int[] iArr = location3;
            Rect rightRect = new Rect(location3[0], location3[1], location3[0] + rightButton.getWidth(), location3[1] + rightButton.getHeight());
            if (e1.getX() < ((float) homeRect.right) && e2.getX() > ((float) rightRect.left)) {
                sendToHandyMode(2);
                background.gestureSlideEffect(homeRect, rightRect);
            } else if (e1.getX() > ((float) homeRect.left) && e2.getX() < ((float) leftRect.right)) {
                sendToHandyMode(1);
                background.gestureSlideEffect(homeRect, leftRect);
            }
        } else {
            float f3 = absVelY;
        }
        return true;
    }

    private void sendToHandyMode(int mode) {
        Intent intent = new Intent("miui.action.handymode.changemode");
        intent.putExtra("mode", mode);
        this.mContext.sendBroadcast(intent);
    }
}
