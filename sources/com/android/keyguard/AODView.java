package com.android.keyguard;

import android.content.Context;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.provider.CallLog;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.android.keyguard.util.ContentProviderBinder;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.Util;
import com.android.systemui.statusbar.policy.KeyguardNotificationController;
import com.android.systemui.statusbar.policy.NotificationChangeListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class AODView extends FrameLayout implements ContentProviderBinder.QueryCompleteListener {
    private static final String TAG = AODView.class.getSimpleName();
    private boolean m24HourFormat;
    Runnable mAnimateInvisible = new Runnable() {
        public void run() {
            Dependency.getHost().setNotificationAnimate(false);
            Dependency.getHost().fireAnimateState();
        }
    };
    private AODStyleController mAodStyleController;
    private ArrayList<ContentProviderBinder> mBinders = new ArrayList<>();
    private Context mContext;
    /* access modifiers changed from: private */
    public boolean mEnableAnimate;
    private ContentObserver mEnableAnimateObserver;
    private BadgetImageView mFirstIcon;
    /* access modifiers changed from: private */
    public List<String> mIconList;
    private HashMap<String, Integer> mIconMap = new HashMap<>();
    Interpolator mInterpolator = new Interpolator() {
        public float getInterpolation(float input) {
            float f = (input / 1.0f) - 1.0f;
            float input2 = f;
            return 1.0f * ((f * input2 * input2) + 1.0f);
        }
    };
    private ImageView mLeftImage;
    protected int mMissCallNum;
    private List<String> mNotificationArray = new ArrayList();
    private AODUpdatePositionController mPosictionController;
    private boolean mRegisteredCallLog = false;
    private ImageView mRightImage;
    private BadgetImageView mSecondIcon;
    protected boolean mShowMissCall;
    private View mTableModeContainer;
    private BadgetImageView mThirdIcon;

    public AODView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        this.mPosictionController = new AODUpdatePositionController(context);
        this.mIconMap.put("com.tencent.mm", Integer.valueOf(R.drawable.wechat));
        this.mIconMap.put("com.tencent.mobileqq", Integer.valueOf(R.drawable.qq));
        this.mIconMap.put("com.whatsapp", Integer.valueOf(R.drawable.whatsapp));
        this.mIconMap.put("com.facebook.orca", Integer.valueOf(R.drawable.facebookmsg));
        this.mIconMap.put("jp.naver.line.android", Integer.valueOf(R.drawable.line));
        this.mIconMap.put("com.google.android.gm", Integer.valueOf(R.drawable.gmail));
        this.mIconMap.put("com.android.email", Integer.valueOf(R.drawable.mail));
        this.mIconMap.put("com.google.android.calendar", Integer.valueOf(R.drawable.gcalendar));
        this.mIconMap.put("com.android.calendar", Integer.valueOf(R.drawable.calendarbg));
        this.mIconMap.put("com.android.server.telecom", Integer.valueOf(R.drawable.phone));
        this.mIconMap.put("com.android.mms", Integer.valueOf(R.drawable.sms));
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        makeNormalPanel();
        handleUpdateView();
    }

    private void makeNormalPanel() {
        this.mAodStyleController = new AODStyleController();
        this.m24HourFormat = DateFormat.is24HourFormat(this.mContext, KeyguardUpdateMonitor.getCurrentUser());
        this.mTableModeContainer = findViewById(R.id.clock_container);
        this.mAodStyleController.inflateClockView(this);
        this.mFirstIcon = (BadgetImageView) findViewById(R.id.first);
        this.mSecondIcon = (BadgetImageView) findViewById(R.id.second);
        this.mThirdIcon = (BadgetImageView) findViewById(R.id.third);
        this.mLeftImage = (ImageView) findViewById(R.id.aod_left_image);
        this.mRightImage = (ImageView) findViewById(R.id.aod_right_image);
    }

    public void handleUpdateView() {
        this.mPosictionController.updatePosition(this.mTableModeContainer);
        handleUpdateTime();
        handleUpdateIcons();
    }

    public void setSunImage(int index) {
        this.mAodStyleController.setSunImage(index, this);
    }

    public void onStartDoze() {
        if (!this.mRegisteredCallLog && Utils.isBootCompleted()) {
            fillMissedCall();
            this.mRegisteredCallLog = true;
            Iterator<ContentProviderBinder> it = this.mBinders.iterator();
            while (it.hasNext()) {
                it.next().init();
            }
        }
    }

    public void onStopDoze() {
        if (this.mRegisteredCallLog) {
            Iterator<ContentProviderBinder> it = this.mBinders.iterator();
            while (it.hasNext()) {
                it.next().finish();
            }
            this.mBinders.clear();
            this.mRegisteredCallLog = false;
        }
    }

    private String getPkg(int i) {
        if (i < 0 || i >= this.mNotificationArray.size()) {
            return null;
        }
        return this.mNotificationArray.get(i);
    }

    private void bindView(BadgetImageView view, int index) {
        String pkg = getPkg(index);
        Integer icon = this.mIconMap.get(pkg);
        if (icon != null) {
            int i = 0;
            view.setVisibility(0);
            if ("com.android.server.telecom".equals(pkg)) {
                i = this.mMissCallNum;
            }
            view.setBadget(i, "com.android.calendar".equals(pkg) ? 1 : 0);
            Drawable drawable = getResources().getDrawable(icon.intValue());
            drawable.setAlpha(178);
            view.setBackground(drawable);
            return;
        }
        view.setVisibility(8);
    }

    /* access modifiers changed from: private */
    public void handleUpdateIcons() {
        bindView(this.mFirstIcon, 0);
        bindView(this.mSecondIcon, 1);
        bindView(this.mThirdIcon, 2);
    }

    private void handleUpdateTime() {
        this.mAodStyleController.handleUpdateTime(this.m24HourFormat);
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mEnableAnimateObserver = new ContentObserver(getHandler()) {
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                boolean unused = AODView.this.mEnableAnimate = MiuiKeyguardUtils.getKeyguardNotificationStatus(AODView.this.getContext().getContentResolver()) == 2;
            }
        };
        this.mEnableAnimateObserver.onChange(false);
        getContext().getContentResolver().registerContentObserver(Settings.System.getUriFor("wakeup_for_keyguard_notification"), false, this.mEnableAnimateObserver, -1);
        this.mIconList = new ArrayList();
        updateNotificationList();
        ((KeyguardNotificationController) Dependency.get(KeyguardNotificationController.class)).setListener(new NotificationChangeListener() {
            public void onAdd(final String pkg) {
                AODView.this.mIconList.add(0, pkg);
                AODView.this.post(new Runnable() {
                    public void run() {
                        AODView.this.showAnimate(pkg);
                    }
                });
                AODView.this.updateNotificationList();
            }

            public void onUpdate(final String pkg) {
                AODView.this.post(new Runnable() {
                    public void run() {
                        AODView.this.showAnimate(pkg);
                    }
                });
                int i = 0;
                while (true) {
                    if (i >= AODView.this.mIconList.size()) {
                        break;
                    } else if (AODView.this.mIconList.get(i) == pkg) {
                        AODView.this.mIconList.remove(i);
                        break;
                    } else {
                        i++;
                    }
                }
                AODView.this.mIconList.add(0, pkg);
                AODView.this.updateNotificationList();
            }

            public void onDelete(String pkg) {
                int i = 0;
                while (true) {
                    if (i >= AODView.this.mIconList.size()) {
                        break;
                    } else if (AODView.this.mIconList.get(i) == pkg) {
                        AODView.this.mIconList.remove(i);
                        break;
                    } else {
                        i++;
                    }
                }
                AODView.this.updateNotificationList();
            }

            public void onClearAll() {
                AODView.this.mIconList.clear();
                AODView.this.updateNotificationList();
            }
        });
    }

    public boolean isAnimateEnable() {
        return this.mEnableAnimate;
    }

    /* access modifiers changed from: private */
    public void showAnimate(String pkg) {
        if (this.mEnableAnimate) {
            String str = TAG;
            Log.v(str, "showAnimate pkg:" + pkg);
            Dependency.getHost().setNotificationAnimate(true);
            Dependency.getHost().fireAnimateState();
            this.mLeftImage.setImageResource(AODNotificationColor.getColorItem(pkg).left);
            this.mRightImage.setImageResource(AODNotificationColor.getColorItem(pkg).right);
            AlphaAnimation alphaAnimation = new AlphaAnimation(1.0f, 0.0f);
            alphaAnimation.setFillAfter(true);
            alphaAnimation.setInterpolator(this.mInterpolator);
            alphaAnimation.setDuration(2400);
            this.mRightImage.startAnimation(alphaAnimation);
            this.mLeftImage.startAnimation(alphaAnimation);
            setAnimateInvisible();
        }
    }

    private void setAnimateInvisible() {
        Handler handler = getHandler();
        if (handler != null) {
            handler.removeCallbacks(this.mAnimateInvisible);
            handler.postDelayed(this.mAnimateInvisible, 2400);
        }
    }

    /* access modifiers changed from: private */
    public void updateNotificationList() {
        if (this.mIconList != null) {
            this.mIconList.sort(new Comparator<String>() {
                public int compare(String o1, String o2) {
                    if ("com.android.server.telecom".equals(o1)) {
                        return -1;
                    }
                    if ("com.android.server.telecom".equals(o2)) {
                        return 1;
                    }
                    return 0;
                }
            });
            this.mNotificationArray.clear();
            for (String item : this.mIconList) {
                if (this.mIconMap.containsKey(item) && !this.mNotificationArray.contains(item)) {
                    this.mNotificationArray.add(item);
                    if (this.mNotificationArray.size() == 3) {
                        break;
                    }
                }
            }
        }
        post(new Runnable() {
            public void run() {
                AODView.this.handleUpdateIcons();
            }
        });
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getContext().getContentResolver().unregisterContentObserver(this.mEnableAnimateObserver);
    }

    public void onQueryCompleted(Uri uri, int num) {
        if (CallLog.Calls.CONTENT_URI.equals(uri)) {
            this.mShowMissCall = num > 0;
            this.mMissCallNum = num;
        }
        handleUpdateIcons();
    }

    private void fillMissedCall() {
        String[] columns = {"number"};
        if (Util.isProviderAccess(CallLog.Calls.CONTENT_URI.getAuthority(), -2)) {
            addContentProviderBinder(CallLog.Calls.CONTENT_URI).setColumns(columns).setWhere("type=3 AND new=1");
        }
    }

    public ContentProviderBinder.Builder addContentProviderBinder(Uri uri) {
        ContentProviderBinder binder = new ContentProviderBinder(this.mContext);
        binder.setQueryCompleteListener(this);
        binder.setUri(uri);
        this.mBinders.add(binder);
        return new ContentProviderBinder.Builder(binder);
    }

    /* access modifiers changed from: protected */
    public void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == 0) {
            this.m24HourFormat = DateFormat.is24HourFormat(this.mContext, KeyguardUpdateMonitor.getCurrentUser());
        }
    }
}
