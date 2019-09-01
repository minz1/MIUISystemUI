package com.android.systemui.statusbar.notification;

import android.content.Context;
import android.os.Bundle;
import android.telephony.SubscriptionManagerCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.systemui.Constants;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.statusbar.CallStateController;
import com.miui.voiptalk.service.MiuiVoipManager;
import miui.telephony.TelephonyManager;

public class InCallNotificationView extends LinearLayout {
    private static final boolean DEBUG = Constants.DEBUG;
    private static final int[] SIM_ICONS = {R.drawable.stat_sys_sim_card_1, R.drawable.stat_sys_sim_card_2};
    private ImageView mAnswerIcon;
    private TextView mCallerInfo;
    private TextView mCallerName;
    private ImageView mCallerSim;
    private Context mContext;
    private ImageView mEndCallIcon;
    /* access modifiers changed from: private */
    public InCallCallback mInCallCallback;
    private boolean mIsVideoCall;
    private MiuiVoipManager mMiuiVoipManager = MiuiVoipManager.getInstance(this.mContext);
    private String mPhoneNumber;
    private int mSubId;
    private TelephonyManager mTelephonyManager = TelephonyManager.getDefault();

    public interface InCallCallback {
        void onAnswerCall();

        void onEndCall();

        void onExitCall();

        void onInCallNotificationHide();

        void onInCallNotificationShow();
    }

    public InCallNotificationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setClipChildren(false);
        setClipToPadding(false);
        this.mContext = context;
    }

    public void updateInfo(View expandedView, Bundle extras) {
        if (expandedView != null) {
            TextView title = (TextView) expandedView.findViewById(16908310);
            TextView content = (TextView) expandedView.findViewById(16909408);
            this.mCallerName.setText(title == null ? "" : title.getText().toString());
            this.mCallerInfo.setText(content == null ? "" : content.getText().toString());
        }
        if (extras != null) {
            this.mPhoneNumber = extras.getString("phoneNumber");
            this.mSubId = extras.getInt("subId", -1);
            this.mIsVideoCall = extras.getBoolean("isVideoCall", false);
        }
        CallStateController callStateController = (CallStateController) Dependency.get(CallStateController.class);
        int slotIndex = -1;
        if (!Constants.IS_CUST_SINGLE_SIM && callStateController.isMsim()) {
            slotIndex = SubscriptionManagerCompat.getSlotIndex(this.mSubId);
        }
        if (slotIndex >= 0) {
            this.mCallerSim.setVisibility(0);
            this.mCallerSim.setImageResource(SIM_ICONS[slotIndex]);
            this.mCallerSim.setContentDescription(getResources().getString(R.string.description_image_icon_sim_card, new Object[]{Integer.valueOf(slotIndex + 1)}));
        } else {
            this.mCallerSim.setVisibility(8);
        }
        updateAnswerIcon();
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mCallerName = (TextView) findViewById(R.id.caller_name);
        this.mCallerInfo = (TextView) findViewById(R.id.caller_Info);
        this.mCallerSim = (ImageView) findViewById(R.id.caller_sim);
        this.mEndCallIcon = (ImageView) findViewById(R.id.end_call_icon);
        this.mAnswerIcon = (ImageView) findViewById(R.id.answer_icon);
        this.mEndCallIcon.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (InCallNotificationView.this.mInCallCallback != null) {
                    InCallNotificationView.this.mInCallCallback.onEndCall();
                }
            }
        });
        this.mAnswerIcon.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (InCallNotificationView.this.mInCallCallback != null) {
                    InCallNotificationView.this.mInCallCallback.onAnswerCall();
                }
            }
        });
        setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (InCallNotificationView.this.mInCallCallback != null) {
                    InCallNotificationView.this.mInCallCallback.onExitCall();
                }
            }
        });
    }

    public void show() {
        if (DEBUG) {
            Log.d("InCallNotificationView", "show()");
        }
        setVisibility(0);
        if (this.mInCallCallback != null) {
            this.mInCallCallback.onInCallNotificationShow();
        }
    }

    public void hide() {
        if (DEBUG) {
            Log.d("InCallNotificationView", "hide()");
        }
        setVisibility(8);
        if (this.mInCallCallback != null) {
            this.mInCallCallback.onInCallNotificationHide();
        }
    }

    private void updateAnswerIcon() {
        int i;
        if (this.mTelephonyManager.getCallState() == 0) {
            ImageView imageView = this.mAnswerIcon;
            if (this.mMiuiVoipManager.isVideoCall()) {
                i = R.drawable.voip_video_answer_icon;
            } else {
                i = R.drawable.voip_audio_answer_icon;
            }
            imageView.setImageResource(i);
        } else if (this.mIsVideoCall) {
            this.mAnswerIcon.setImageResource(R.drawable.video_answer_icon);
        } else {
            this.mAnswerIcon.setImageResource(R.drawable.answer_icon);
        }
    }

    public void setInCallCallback(InCallCallback inCallCallback) {
        this.mInCallCallback = inCallCallback;
    }
}
