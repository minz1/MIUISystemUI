package com.android.systemui.fsgesture;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.android.systemui.R;

public class FsGestureDemoTitleView extends FrameLayout {
    private TextView mSkipView;
    private TextView mSummaryView;
    private TextView mTitleView;

    public FsGestureDemoTitleView(Context context) {
        this(context, null);
    }

    public FsGestureDemoTitleView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FsGestureDemoTitleView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public FsGestureDemoTitleView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.fs_gesture_title_view, this);
        this.mTitleView = (TextView) view.findViewById(R.id.fsgesture_ready_title);
        this.mSummaryView = (TextView) view.findViewById(R.id.fsgesture_ready_summary);
        this.mSkipView = (TextView) view.findViewById(R.id.fsgesture_skip);
    }

    /* access modifiers changed from: package-private */
    public void setRTLParams() {
        ViewGroup.LayoutParams params = this.mSkipView.getLayoutParams();
        if (params instanceof RelativeLayout.LayoutParams) {
            ((RelativeLayout.LayoutParams) params).removeRule(20);
            ((RelativeLayout.LayoutParams) params).addRule(11);
            int paddingRight = getResources().getDimensionPixelSize(R.dimen.fsgesture_skip_margin_right);
            this.mSkipView.setPadding(0, getResources().getDimensionPixelSize(R.dimen.fsgesture_skip_margin_top), paddingRight, 0);
        }
    }

    /* access modifiers changed from: package-private */
    public void prepareTitleView(int status) {
        setBackground(getResources().getDrawable(R.drawable.fs_gesture_bg_ready, null));
        int titleRes = 0;
        int summaryRes = 0;
        switch (status) {
            case 0:
                titleRes = R.string.fs_gesture_back_ready_title;
                summaryRes = R.string.fs_gesture_left_back_ready_summary;
                break;
            case 1:
                titleRes = R.string.fs_gesture_back_ready_title;
                summaryRes = R.string.fs_gesture_right_back_ready_summary;
                break;
            case 2:
                titleRes = R.string.how_to_back_home;
                summaryRes = R.string.fs_gesture_back_home_summary;
                break;
            case 3:
                titleRes = R.string.how_to_switch_recents;
                summaryRes = R.string.fs_gesture_switch_recents_summary;
                break;
            case 4:
                titleRes = R.string.how_to_use_drawer;
                summaryRes = R.string.how_to_use_drawer_summary;
                break;
        }
        if (this.mTitleView != null && this.mSummaryView != null) {
            this.mTitleView.setText(titleRes);
            this.mSummaryView.setText(summaryRes);
            this.mTitleView.setVisibility(0);
        }
    }

    /* access modifiers changed from: package-private */
    public void notifyFinish() {
        setBackground(getResources().getDrawable(R.drawable.fs_gesture_bg_finish, null));
        this.mTitleView.setVisibility(4);
        this.mSummaryView.setTranslationY(this.mSummaryView.getTranslationX() - 15.0f);
        this.mSummaryView.setText(R.string.fs_gesture_finish);
        this.mSkipView.setVisibility(8);
    }

    /* access modifiers changed from: package-private */
    public void registerSkipEvent(View.OnClickListener onClickListener) {
        if (this.mSkipView != null) {
            this.mSkipView.setOnClickListener(onClickListener);
        }
    }
}
