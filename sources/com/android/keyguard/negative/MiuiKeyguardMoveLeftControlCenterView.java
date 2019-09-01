package com.android.keyguard.negative;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.hardware.ConsumerIrManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.MiuiKeyguardUtils;
import com.android.keyguard.analytics.AnalyticsHelper;
import com.android.keyguard.magazine.LockScreenMagazineUtils;
import com.android.keyguard.utils.ContentProviderUtils;
import com.android.keyguard.utils.PackageUtils;
import com.android.keyguard.wallpaper.WallpaperAuthorityUtils;
import com.android.systemui.R;
import com.android.systemui.SystemUICompat;

public class MiuiKeyguardMoveLeftControlCenterView extends MiuiKeyguardMoveLeftBaseView {
    /* access modifiers changed from: private */
    public static final Uri KEYGUARD_CONTROLLER_AUTHORITY = Uri.parse("content://com.xiaomi.mitv.phone.remotecontroller.provider.LockScreenProvider");
    /* access modifiers changed from: private */
    public static final Uri KEYGUARD_MIPAY_AND_BUSCARD = Uri.parse("content://com.miui.tsmclient.provider.public");
    public static final Uri KEYGUARD_SMART_HOME = Uri.parse("content://com.xiaomi.smarthome.ext_cp");
    /* access modifiers changed from: private */
    public LinearLayout mAllFourLinearLayout;
    /* access modifiers changed from: private */
    public ConsumerIrManager mConsumerIrManager = null;
    /* access modifiers changed from: private */
    public ContentObserver mContentObserver;
    /* access modifiers changed from: private */
    public Context mContext;
    private float mFontScale;
    /* access modifiers changed from: private */
    public int mFourOrThreeItemTopMargin;
    /* access modifiers changed from: private */
    public boolean mHasIrEmitter;
    /* access modifiers changed from: private */
    public int mItemNums = 0;
    View.OnClickListener mListener = new View.OnClickListener() {
        public void onClick(View v) {
            boolean z = true;
            switch (v.getId()) {
                case R.id.keyguard_electric_torch /*2131362189*/:
                    AnalyticsHelper.recordLeftViewItem("keyguard_left_view_electric_torch", "click");
                    boolean enable = Settings.Global.getInt(MiuiKeyguardMoveLeftControlCenterView.this.mContext.getContentResolver(), "torch_state", 0) != 0;
                    MiuiKeyguardMoveLeftControlCenterView.this.mContext.sendBroadcast(PackageUtils.getToggleTorchIntent(!enable));
                    ImageView access$400 = MiuiKeyguardMoveLeftControlCenterView.this.mTorchLightImageView;
                    if (enable) {
                        z = false;
                    }
                    access$400.setSelected(z);
                    return;
                case R.id.keyguard_lock_screen_magazine_info /*2131362201*/:
                    if (PackageUtils.isAppInstalledForUser(MiuiKeyguardMoveLeftControlCenterView.this.mContext, LockScreenMagazineUtils.LOCK_SCREEN_MAGAZINE_PACKAGE_NAME, KeyguardUpdateMonitor.getCurrentUser())) {
                        Log.d("miui_keyguard", "left view goto lock screen wall paper");
                        MiuiKeyguardMoveLeftControlCenterView.this.setPreviewButtonClicked();
                        AnalyticsHelper.recordLeftLockscreenMagazineButton(WallpaperAuthorityUtils.isLockScreenMagazineOpenedWallpaper());
                        LockScreenMagazineUtils.gotoLockScreenMagazine(MiuiKeyguardMoveLeftControlCenterView.this.mContext, "leftLockScreen");
                        return;
                    }
                    AnalyticsHelper.recordDownloadLockScreenMagazine("leftLockScreen");
                    MiuiKeyguardMoveLeftControlCenterView.this.startAppStoreToDownload(R.id.keyguard_lock_screen_magazine_info);
                    return;
                case R.id.keyguard_mi_wallet_info /*2131362203*/:
                    AnalyticsHelper.recordLeftViewItem("keyguard_left_view_mi_wallet", "click");
                    MiuiKeyguardMoveLeftControlCenterView.this.startToTSMClientActivity();
                    return;
                case R.id.keyguard_remote_controller_info /*2131362208*/:
                    if (PackageUtils.isAppInstalledForUser(MiuiKeyguardMoveLeftControlCenterView.this.mContext, "com.duokan.phone.remotecontroller", KeyguardUpdateMonitor.getCurrentUser())) {
                        try {
                            AnalyticsHelper.recordLeftViewItem("keyguard_left_view_remote_controller", "click");
                            Intent intent = MiuiKeyguardMoveLeftControlCenterView.this.mContext.getPackageManager().getLaunchIntentForPackage("com.duokan.phone.remotecontroller");
                            intent.addFlags(268435456);
                            MiuiKeyguardMoveLeftControlCenterView.this.mStatusBar.startActivity(intent, true);
                            return;
                        } catch (Exception e) {
                            return;
                        }
                    } else {
                        AnalyticsHelper.recordLeftViewItem("keyguard_left_view_remote_controller", "download");
                        MiuiKeyguardMoveLeftControlCenterView.this.startAppStoreToDownload(R.id.keyguard_remote_controller_info);
                        return;
                    }
                case R.id.keyguard_smarthome_info /*2131362216*/:
                    if (PackageUtils.isAppInstalledForUser(MiuiKeyguardMoveLeftControlCenterView.this.mContext, "com.xiaomi.smarthome", KeyguardUpdateMonitor.getCurrentUser())) {
                        try {
                            AnalyticsHelper.recordLeftViewItem("keyguard_left_view_smarthome", "click");
                            MiuiKeyguardMoveLeftControlCenterView.this.mStatusBar.startActivity(PackageUtils.getSmartHomeMainIntent(), true);
                            return;
                        } catch (Exception e2) {
                            return;
                        }
                    } else {
                        AnalyticsHelper.recordLeftViewItem("keyguard_left_view_smarthome", "download");
                        MiuiKeyguardMoveLeftControlCenterView.this.startAppStoreToDownload(R.id.keyguard_smarthome_info);
                        return;
                    }
                default:
                    return;
            }
        }
    };
    private Object mLocaleList;
    /* access modifiers changed from: private */
    public LinearLayout mLockScreenMagazineLinearLayout;
    /* access modifiers changed from: private */
    public boolean mMiWalletCardItemUpdate = false;
    /* access modifiers changed from: private */
    public TextView mMiWalletCardNum;
    /* access modifiers changed from: private */
    public String mMiWalletCardNumInfo;
    /* access modifiers changed from: private */
    public LinearLayout mMiWalletLinearLayout;
    /* access modifiers changed from: private */
    public LinearLayout mRemoteCenterLinearLayout;
    /* access modifiers changed from: private */
    public boolean mRemoteControllerItemUpdate = false;
    /* access modifiers changed from: private */
    public TextView mRemoteControllerNum;
    /* access modifiers changed from: private */
    public String mRemoteControllerNumInfo;
    /* access modifiers changed from: private */
    public boolean mSmartHomeItemUpdate = false;
    /* access modifiers changed from: private */
    public LinearLayout mSmartHomeLinearLayout;
    /* access modifiers changed from: private */
    public TextView mSmartHomeNum;
    /* access modifiers changed from: private */
    public String mSmartHomeNumnInfo;
    /* access modifiers changed from: private */
    public boolean mSupportTSMClient;
    /* access modifiers changed from: private */
    public ImageView mTorchLightImageView;
    private ContentObserver mTorchStateReceiver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            boolean enabled = false;
            if (Settings.Global.getInt(MiuiKeyguardMoveLeftControlCenterView.this.mContext.getContentResolver(), "torch_state", 0) != 0) {
                enabled = true;
            }
            MiuiKeyguardMoveLeftControlCenterView.this.mTorchLightImageView.setSelected(enabled);
        }
    };
    /* access modifiers changed from: private */
    public int mTwoOrOneItemLeftMargin;
    /* access modifiers changed from: private */
    public int mTwoOrOneItemRightMargin;
    /* access modifiers changed from: private */
    public int mTwoOrOneItemTopMargin;

    public MiuiKeyguardMoveLeftControlCenterView(Context context) {
        super(context);
        this.mContext = context;
    }

    public MiuiKeyguardMoveLeftControlCenterView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mSmartHomeLinearLayout = (LinearLayout) findViewById(R.id.keyguard_smarthome_info);
        this.mRemoteCenterLinearLayout = (LinearLayout) findViewById(R.id.keyguard_remote_controller_info);
        this.mMiWalletLinearLayout = (LinearLayout) findViewById(R.id.keyguard_mi_wallet_info);
        this.mLockScreenMagazineLinearLayout = (LinearLayout) findViewById(R.id.keyguard_lock_screen_magazine_info);
        this.mTorchLightImageView = (ImageView) findViewById(R.id.keyguard_electric_torch);
        this.mAllFourLinearLayout = (LinearLayout) findViewById(R.id.keyguard_move_left);
        this.mSmartHomeLinearLayout.setOnClickListener(this.mListener);
        this.mRemoteCenterLinearLayout.setOnClickListener(this.mListener);
        this.mMiWalletLinearLayout.setOnClickListener(this.mListener);
        this.mLockScreenMagazineLinearLayout.setOnClickListener(this.mListener);
        this.mTorchLightImageView.setOnClickListener(this.mListener);
        this.mTwoOrOneItemTopMargin = getResources().getDimensionPixelSize(R.dimen.keyguard_move_left_layout_top_margint_twoorone);
        this.mTwoOrOneItemLeftMargin = getResources().getDimensionPixelSize(R.dimen.keyguard_move_left_layout_left_margint_twoorone);
        this.mTwoOrOneItemRightMargin = getResources().getDimensionPixelSize(R.dimen.keyguard_move_left_layout_right_margint_twoorone);
        this.mFourOrThreeItemTopMargin = getResources().getDimensionPixelOffset(R.dimen.keyguard_move_left_layout_top_margint_fourorthree);
        initKeyguardLeftItemInfos();
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("torch_state"), false, this.mTorchStateReceiver);
        this.mTorchStateReceiver.onChange(false);
        this.mContentObserver = new ContentObserver(new Handler()) {
            public void onChange(boolean selfChange, Uri uri) {
                super.onChange(selfChange, uri);
                if (uri != null) {
                    if (uri.toString().contains(MiuiKeyguardMoveLeftControlCenterView.KEYGUARD_SMART_HOME.toString())) {
                        MiuiKeyguardMoveLeftControlCenterView.this.updateItemNumString(R.id.keyguard_smarthome_info);
                    } else if (uri.toString().contains(MiuiKeyguardMoveLeftControlCenterView.KEYGUARD_CONTROLLER_AUTHORITY.toString())) {
                        MiuiKeyguardMoveLeftControlCenterView.this.updateItemNumString(R.id.keyguard_remote_controller_info);
                    } else if (uri.toString().contains(MiuiKeyguardMoveLeftControlCenterView.KEYGUARD_MIPAY_AND_BUSCARD.toString())) {
                        MiuiKeyguardMoveLeftControlCenterView.this.updateItemNumString(R.id.keyguard_mi_wallet_info);
                    }
                }
            }
        };
        initLeftView();
        uploadData();
    }

    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (event.getAction() == 0 && this.mStatusBar != null) {
            this.mStatusBar.userActivity();
        }
        return super.onInterceptTouchEvent(event);
    }

    private void initKeyguardLeftItemInfos() {
        initKeyguardLeftItemInfo(R.id.keyguard_smarthome_info, R.drawable.keyguard_left_view_smarthome, R.string.keyguard_left_smarthome);
        initKeyguardLeftItemInfo(R.id.keyguard_remote_controller_info, R.drawable.keyguard_left_view_remotecontroller, R.string.keyguard_left_remotecentral);
        initKeyguardLeftItemInfo(R.id.keyguard_mi_wallet_info, R.drawable.keyguard_left_view_bankcard, R.string.keyguard_left_mi_wallet);
        initKeyguardLeftItemInfo(R.id.keyguard_lock_screen_magazine_info, R.drawable.keyguard_left_view_magazine, R.string.keyguard_left_view_lock_wallpaper);
    }

    public void initLeftView() {
        new AsyncTask<Void, Void, Boolean>() {
            /* access modifiers changed from: protected */
            public Boolean doInBackground(Void... params) {
                if (MiuiKeyguardMoveLeftControlCenterView.this.mConsumerIrManager == null) {
                    ConsumerIrManager unused = MiuiKeyguardMoveLeftControlCenterView.this.mConsumerIrManager = (ConsumerIrManager) MiuiKeyguardMoveLeftControlCenterView.this.mContext.getSystemService("consumer_ir");
                }
                if (MiuiKeyguardMoveLeftControlCenterView.this.mConsumerIrManager != null) {
                    boolean unused2 = MiuiKeyguardMoveLeftControlCenterView.this.mHasIrEmitter = MiuiKeyguardMoveLeftControlCenterView.this.mConsumerIrManager.hasIrEmitter();
                }
                try {
                    MiuiKeyguardMoveLeftControlCenterView miuiKeyguardMoveLeftControlCenterView = MiuiKeyguardMoveLeftControlCenterView.this;
                    boolean z = false;
                    if (PackageUtils.isAppInstalledForUser(MiuiKeyguardMoveLeftControlCenterView.this.mContext, "com.miui.tsmclient", KeyguardUpdateMonitor.getCurrentUser()) && MiuiKeyguardMoveLeftControlCenterView.this.mContext.getPackageManager().getPackageInfo("com.miui.tsmclient", 0).versionCode >= 18) {
                        z = true;
                    }
                    boolean unused3 = miuiKeyguardMoveLeftControlCenterView.mSupportTSMClient = z;
                } catch (Exception e) {
                    Log.e("MiuiKeyguardMoveLeftBaseView", "cannot find TSMClient Package");
                }
                return true;
            }

            /* access modifiers changed from: protected */
            public void onPostExecute(Boolean result) {
                int unused = MiuiKeyguardMoveLeftControlCenterView.this.mItemNums = 0;
                MiuiKeyguardMoveLeftControlCenterView.this.updateItemVisibility(MiuiKeyguardMoveLeftControlCenterView.this.mHasIrEmitter, MiuiKeyguardMoveLeftControlCenterView.this.mRemoteCenterLinearLayout);
                MiuiKeyguardMoveLeftControlCenterView.this.updateItemVisibility(MiuiKeyguardMoveLeftControlCenterView.this.mSupportTSMClient, MiuiKeyguardMoveLeftControlCenterView.this.mMiWalletLinearLayout);
                MiuiKeyguardMoveLeftControlCenterView.this.updateItemVisibility(MiuiKeyguardUtils.isRegionSupportMiHome(MiuiKeyguardMoveLeftControlCenterView.this.mContext), MiuiKeyguardMoveLeftControlCenterView.this.mSmartHomeLinearLayout);
                MiuiKeyguardMoveLeftControlCenterView.this.updateItemVisibility(MiuiKeyguardMoveLeftControlCenterView.this.supportLockScreenMagazine(), MiuiKeyguardMoveLeftControlCenterView.this.mLockScreenMagazineLinearLayout);
                if (MiuiKeyguardMoveLeftControlCenterView.this.mAllFourLinearLayout != null) {
                    RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) MiuiKeyguardMoveLeftControlCenterView.this.mAllFourLinearLayout.getLayoutParams();
                    layoutParams.setMargins(MiuiKeyguardMoveLeftControlCenterView.this.mTwoOrOneItemLeftMargin, MiuiKeyguardMoveLeftControlCenterView.this.mItemNums <= 2 ? MiuiKeyguardMoveLeftControlCenterView.this.mTwoOrOneItemTopMargin : MiuiKeyguardMoveLeftControlCenterView.this.mFourOrThreeItemTopMargin, MiuiKeyguardMoveLeftControlCenterView.this.mTwoOrOneItemRightMargin, 0);
                    MiuiKeyguardMoveLeftControlCenterView.this.mAllFourLinearLayout.setLayoutParams(layoutParams);
                }
                try {
                    MiuiKeyguardMoveLeftControlCenterView.this.mContext.getContentResolver().unregisterContentObserver(MiuiKeyguardMoveLeftControlCenterView.this.mContentObserver);
                    if (MiuiKeyguardMoveLeftControlCenterView.this.mHasIrEmitter) {
                        MiuiKeyguardMoveLeftControlCenterView.this.mContext.getContentResolver().registerContentObserver(MiuiKeyguardMoveLeftControlCenterView.KEYGUARD_CONTROLLER_AUTHORITY, true, MiuiKeyguardMoveLeftControlCenterView.this.mContentObserver);
                    }
                    if (MiuiKeyguardMoveLeftControlCenterView.this.mSupportTSMClient) {
                        MiuiKeyguardMoveLeftControlCenterView.this.mContext.getContentResolver().registerContentObserver(MiuiKeyguardMoveLeftControlCenterView.KEYGUARD_MIPAY_AND_BUSCARD, true, MiuiKeyguardMoveLeftControlCenterView.this.mContentObserver);
                    }
                    if (MiuiKeyguardUtils.isRegionSupportMiHome(MiuiKeyguardMoveLeftControlCenterView.this.mContext)) {
                        MiuiKeyguardMoveLeftControlCenterView.this.mContext.getContentResolver().registerContentObserver(MiuiKeyguardMoveLeftControlCenterView.KEYGUARD_SMART_HOME, true, MiuiKeyguardMoveLeftControlCenterView.this.mContentObserver);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
    }

    /* access modifiers changed from: private */
    public void updateItemVisibility(boolean show, View item) {
        if (item == null) {
            return;
        }
        if (show) {
            item.setVisibility(0);
            this.mItemNums++;
            return;
        }
        item.setVisibility(8);
    }

    public void uploadData() {
        updateItemNumString(0);
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (this.mContentObserver != null) {
            this.mContext.getContentResolver().unregisterContentObserver(this.mContentObserver);
            this.mContentObserver = null;
        }
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Object localeList = SystemUICompat.getLocales(newConfig);
        float fontScale = newConfig.fontScale;
        if (this.mLocaleList != localeList) {
            initKeyguardLeftItemInfos();
            this.mLocaleList = localeList;
        }
        if (this.mFontScale != fontScale) {
            initKeyguardLeftItemInfos();
            this.mFontScale = fontScale;
        }
    }

    /* access modifiers changed from: private */
    public void updateItemNumString(int itemid) {
        if (itemid == 0) {
            this.mSmartHomeItemUpdate = true;
            this.mRemoteControllerItemUpdate = true;
            this.mMiWalletCardItemUpdate = true;
        }
        if (itemid == R.id.keyguard_smarthome_info) {
            this.mSmartHomeItemUpdate = true;
        }
        if (itemid == R.id.keyguard_remote_controller_info) {
            this.mRemoteControllerItemUpdate = true;
        }
        if (itemid == R.id.keyguard_mi_wallet_info) {
            this.mMiWalletCardItemUpdate = true;
        }
        new AsyncTask<Void, Void, Boolean>() {
            /* access modifiers changed from: protected */
            public Boolean doInBackground(Void... params) {
                if (MiuiKeyguardMoveLeftControlCenterView.this.mSmartHomeItemUpdate && MiuiKeyguardUtils.isRegionSupportMiHome(MiuiKeyguardMoveLeftControlCenterView.this.mContext)) {
                    Bundle smbundle = ContentProviderUtils.getResultFromProvider(MiuiKeyguardMoveLeftControlCenterView.this.mContext, MiuiKeyguardUtils.maybeAddUserId(MiuiKeyguardMoveLeftControlCenterView.KEYGUARD_SMART_HOME, KeyguardUpdateMonitor.getCurrentUser()), "online_devices_count", (String) null, (Bundle) null);
                    String unused = MiuiKeyguardMoveLeftControlCenterView.this.mSmartHomeNumnInfo = smbundle == null ? "" : smbundle.getString("count", "");
                }
                if (MiuiKeyguardMoveLeftControlCenterView.this.mRemoteControllerItemUpdate && MiuiKeyguardMoveLeftControlCenterView.this.mHasIrEmitter) {
                    Bundle bundle = ContentProviderUtils.getResultFromProvider(MiuiKeyguardMoveLeftControlCenterView.this.mContext, MiuiKeyguardMoveLeftControlCenterView.KEYGUARD_CONTROLLER_AUTHORITY, "device_sum", (String) null, (Bundle) null);
                    String unused2 = MiuiKeyguardMoveLeftControlCenterView.this.mRemoteControllerNumInfo = bundle == null ? "" : bundle.getString("ir_device_sum", "");
                }
                if (MiuiKeyguardMoveLeftControlCenterView.this.mMiWalletCardItemUpdate && MiuiKeyguardMoveLeftControlCenterView.this.mSupportTSMClient) {
                    Bundle bundle2 = ContentProviderUtils.getResultFromProvider(MiuiKeyguardMoveLeftControlCenterView.this.mContext, MiuiKeyguardMoveLeftControlCenterView.KEYGUARD_MIPAY_AND_BUSCARD, "cards_info", (String) null, (Bundle) null);
                    String unused3 = MiuiKeyguardMoveLeftControlCenterView.this.mMiWalletCardNumInfo = bundle2 == null ? "" : bundle2.getString("all_cards_count", "");
                }
                return true;
            }

            /* access modifiers changed from: protected */
            public void onPostExecute(Boolean result) {
                if (MiuiKeyguardMoveLeftControlCenterView.this.getWindowToken() != null) {
                    MiuiKeyguardMoveLeftControlCenterView.this.mSmartHomeNum.setText(MiuiKeyguardMoveLeftControlCenterView.this.mSmartHomeNumnInfo);
                    MiuiKeyguardMoveLeftControlCenterView.this.mRemoteControllerNum.setText(MiuiKeyguardMoveLeftControlCenterView.this.mRemoteControllerNumInfo);
                    MiuiKeyguardMoveLeftControlCenterView.this.mMiWalletCardNum.setText(MiuiKeyguardMoveLeftControlCenterView.this.mMiWalletCardNumInfo);
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
    }

    private void initKeyguardLeftItemInfo(int itemId, int imageId, int desId) {
        View item = findViewById(itemId);
        ((ImageView) item.findViewById(R.id.keyguard_left_list_item_img)).setBackgroundResource(imageId);
        TextView des = (TextView) item.findViewById(R.id.keyguard_left_list_item_name);
        des.setText(desId);
        TextView view = (TextView) item.findViewById(R.id.keyguard_left_list_item_number);
        updateItemInfoTextSize(des, view);
        if (itemId == R.id.keyguard_mi_wallet_info) {
            this.mMiWalletCardNum = view;
        } else if (itemId == R.id.keyguard_remote_controller_info) {
            this.mRemoteControllerNum = view;
        } else if (itemId == R.id.keyguard_smarthome_info) {
            this.mSmartHomeNum = view;
        }
    }

    private void updateItemInfoTextSize(TextView des, TextView view) {
        Resources resources = getResources();
        int desTextSize = resources.getDimensionPixelSize(R.dimen.keyguard_move_left_litem_textview_name_size);
        int numberTextSize = resources.getDimensionPixelSize(R.dimen.keyguard_move_left_litem_textview_num_size);
        des.setTextSize(0, (float) desTextSize);
        view.setTextSize(0, (float) numberTextSize);
    }

    /* access modifiers changed from: private */
    public void startAppStoreToDownload(int keyguardInfo) {
        String packageName = "";
        if (keyguardInfo == R.id.keyguard_smarthome_info) {
            packageName = "com.xiaomi.smarthome";
        } else if (keyguardInfo == R.id.keyguard_remote_controller_info) {
            packageName = "com.duokan.phone.remotecontroller";
        } else if (keyguardInfo == R.id.keyguard_lock_screen_magazine_info) {
            try {
                packageName = LockScreenMagazineUtils.LOCK_SCREEN_MAGAZINE_PACKAGE_NAME;
            } catch (Exception e) {
                Log.e("MiuiKeyguardMoveLeftBaseView", "startAppStoreToDownload", e);
                return;
            }
        }
        this.mStatusBar.startActivity(PackageUtils.getMarketDownloadIntent(packageName), true);
    }

    /* access modifiers changed from: private */
    public void startToTSMClientActivity() {
        try {
            this.mContext.startActivityAsUser(PackageUtils.getTSMClientIntent(), UserHandle.CURRENT);
        } catch (Exception e) {
        }
    }

    public boolean isSupportRightMove() {
        return MiuiKeyguardUtils.isRegionSupportMiHome(this.mContext) || this.mHasIrEmitter || this.mSupportTSMClient || supportLockScreenMagazine();
    }

    /* access modifiers changed from: private */
    public void setPreviewButtonClicked() {
        PreferenceManager.getDefaultSharedPreferences(this.mContext).edit().putBoolean("prfe_key_preview_button_clicked", true).commit();
    }

    /* access modifiers changed from: private */
    public boolean supportLockScreenMagazine() {
        return LockScreenMagazineUtils.supportLockScreenMagazineRegion(this.mContext);
    }

    public boolean hasOverlappingRendering() {
        return false;
    }
}
