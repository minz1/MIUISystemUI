package com.android.systemui.pip.phone;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.recents.misc.SystemServicesProxy;

public class PipDismissViewController {
    private Context mContext;
    /* access modifiers changed from: private */
    public View mDismissView;
    /* access modifiers changed from: private */
    public WindowManager mWindowManager;

    public PipDismissViewController(Context context) {
        this.mContext = context;
        this.mWindowManager = (WindowManager) context.getSystemService("window");
    }

    public void createDismissTarget() {
        if (this.mDismissView == null) {
            Rect stableInsets = new Rect();
            SystemServicesProxy.getInstance(this.mContext).getStableInsets(stableInsets);
            Point windowSize = new Point();
            this.mWindowManager.getDefaultDisplay().getRealSize(windowSize);
            int gradientHeight = this.mContext.getResources().getDimensionPixelSize(R.dimen.pip_dismiss_gradient_height);
            int bottomMargin = this.mContext.getResources().getDimensionPixelSize(R.dimen.pip_dismiss_text_bottom_margin);
            this.mDismissView = LayoutInflater.from(this.mContext).inflate(R.layout.pip_dismiss_view, null);
            this.mDismissView.setSystemUiVisibility(256);
            this.mDismissView.forceHasOverlappingRendering(false);
            Drawable gradient = this.mContext.getResources().getDrawable(R.drawable.pip_dismiss_scrim);
            gradient.setAlpha(216);
            this.mDismissView.setBackground(gradient);
            FrameLayout.LayoutParams tlp = (FrameLayout.LayoutParams) this.mDismissView.findViewById(R.id.pip_dismiss_text).getLayoutParams();
            tlp.bottomMargin = stableInsets.bottom + bottomMargin;
            FrameLayout.LayoutParams layoutParams = tlp;
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams(-1, gradientHeight, 0, windowSize.y - gradientHeight, 2024, 16777496, -3);
            lp.setTitle("pip-dismiss-overlay");
            lp.privateFlags |= 16;
            lp.gravity = 49;
            this.mWindowManager.addView(this.mDismissView, lp);
        }
        this.mDismissView.animate().cancel();
    }

    public void showDismissTarget() {
        this.mDismissView.animate().alpha(1.0f).setInterpolator(Interpolators.LINEAR).setStartDelay(100).setDuration(350).start();
    }

    public void destroyDismissTarget() {
        if (this.mDismissView != null) {
            this.mDismissView.animate().alpha(0.0f).setInterpolator(Interpolators.LINEAR).setStartDelay(0).setDuration(225).withEndAction(new Runnable() {
                public void run() {
                    PipDismissViewController.this.mWindowManager.removeViewImmediate(PipDismissViewController.this.mDismissView);
                    View unused = PipDismissViewController.this.mDismissView = null;
                }
            }).start();
        }
    }
}
