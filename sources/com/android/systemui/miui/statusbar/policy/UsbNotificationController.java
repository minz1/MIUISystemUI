package com.android.systemui.miui.statusbar.policy;

import android.app.AlertDialog;
import android.app.MiuiThemeHelper;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbManagerCompat;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import com.android.systemui.Constants;
import com.android.systemui.miui.statusbar.ExpandedNotification;
import com.android.systemui.plugins.R;
import com.miui.systemui.annotation.Inject;
import miui.util.ResourceMapper;

public class UsbNotificationController {
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context content, Intent intent) {
            String action = intent.getAction();
            boolean oldUsbPlugged = false;
            if ("android.intent.action.BATTERY_CHANGED".equals(action)) {
                int oldPlugType = UsbNotificationController.this.mPlugType;
                int unused = UsbNotificationController.this.mPlugType = intent.getIntExtra("plugged", 0);
                boolean usbPlugged = UsbNotificationController.this.mPlugType == 2;
                if (oldPlugType == 2) {
                    oldUsbPlugged = true;
                }
                if (usbPlugged != oldUsbPlugged) {
                    UsbNotificationController.this.refreshWhenUsbConnectChanged(usbPlugged);
                }
            } else if ("android.hardware.usb.action.USB_STATE".equals(action)) {
                UsbNotificationController.this.refreshWhenUsbConnectChanged(intent.getBooleanExtra("connected", false));
            }
        }
    };
    private int mCdInstallNotificationId;
    private int mChargingNotificationId;
    /* access modifiers changed from: private */
    public Context mContext;
    /* access modifiers changed from: private */
    public boolean mDisableUsbBySim;
    private final ContentObserver mDisableUsbObserver = new ContentObserver(this.mHandler) {
        public void onChange(boolean selfChange) {
            boolean z = true;
            boolean unused = UsbNotificationController.this.mDisableUsbBySim = Settings.System.getInt(UsbNotificationController.this.mContext.getContentResolver(), "disable_usb_by_sim", (int) Constants.SUPPORT_DISABLE_USB_BY_SIM) != 0;
            if (!Constants.SUPPORT_DISABLE_USB_BY_SIM && UsbNotificationController.this.mDisableUsbBySim) {
                Log.d("UsbNotificationController", "not support disable usb by sim!");
                boolean unused2 = UsbNotificationController.this.mDisableUsbBySim = false;
                Settings.System.putInt(UsbNotificationController.this.mContext.getContentResolver(), "disable_usb_by_sim", 0);
            }
            if (!UsbNotificationController.this.mDisableUsbBySim) {
                if (UsbNotificationController.this.mIsDialogShowing && UsbNotificationController.this.mUsbAlert != null) {
                    UsbNotificationController.this.mUsbAlert.dismiss();
                }
                UsbNotificationController usbNotificationController = UsbNotificationController.this;
                if (UsbNotificationController.this.mPlugType != 2) {
                    z = false;
                }
                usbNotificationController.refreshWhenUsbConnectChanged(z);
            }
        }
    };
    private boolean mEnableUsbModeSelection;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    /* access modifiers changed from: private */
    public boolean mIsDialogShowing;
    private boolean mIsScreenshotMode;
    private int mMtpNotificationId;
    /* access modifiers changed from: private */
    public int mPlugType = 0;
    private int mPtpNotificationId;
    /* access modifiers changed from: private */
    public AlertDialog mUsbAlert;
    private UsbManager mUsbManager;

    public UsbNotificationController(@Inject Context context) {
        this.mContext = context;
        this.mIsScreenshotMode = MiuiThemeHelper.isScreenshotMode();
        this.mUsbManager = (UsbManager) this.mContext.getSystemService("usb");
        this.mPtpNotificationId = ResourceMapper.resolveReference(this.mContext.getResources(), 285802562);
        this.mMtpNotificationId = ResourceMapper.resolveReference(this.mContext.getResources(), 285802563);
        this.mCdInstallNotificationId = ResourceMapper.resolveReference(this.mContext.getResources(), 285802564);
        this.mEnableUsbModeSelection = this.mContext.getResources().getBoolean(285868048);
        this.mChargingNotificationId = this.mContext.getResources().getIdentifier("usb_charging_notification_title", "string", "com.mediatek");
        if (this.mChargingNotificationId == 0) {
            this.mChargingNotificationId = this.mContext.getResources().getIdentifier("usb_charging_notification_title", "string", "android");
        }
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("disable_usb_by_sim"), false, this.mDisableUsbObserver);
        this.mDisableUsbObserver.onChange(false);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.BATTERY_CHANGED");
        filter.addAction("android.hardware.usb.action.USB_STATE");
        this.mContext.registerReceiver(this.mBroadcastReceiver, filter);
    }

    /* access modifiers changed from: private */
    public void refreshWhenUsbConnectChanged(boolean connected) {
        if (Constants.SUPPORT_DISABLE_USB_BY_SIM && connected && this.mDisableUsbBySim && !this.mIsDialogShowing) {
            this.mIsDialogShowing = true;
            AlertDialog.Builder b = new AlertDialog.Builder(this.mContext, R.style.Theme_Dialog_Alert);
            b.setCancelable(true);
            b.setTitle(com.android.systemui.R.string.activate_usb_title);
            b.setMessage(com.android.systemui.R.string.activate_usb_message);
            b.setIconAttribute(16843605);
            b.setPositiveButton(17039370, null);
            this.mUsbAlert = b.create();
            this.mUsbAlert.getWindow().setType(2003);
            this.mUsbAlert.setOnDismissListener(new DialogInterface.OnDismissListener() {
                public void onDismiss(DialogInterface dialog) {
                    boolean unused = UsbNotificationController.this.mIsDialogShowing = false;
                }
            });
            this.mUsbAlert.show();
            UsbManagerCompat.setCurrentFunction(this.mUsbManager, "none", false);
            Settings.Global.putInt(this.mContext.getContentResolver(), "adb_enabled", 0);
        }
    }

    public boolean needDisableUsbNotification(ExpandedNotification n) {
        return (this.mDisableUsbBySim || this.mIsScreenshotMode) && isUsbNotification(n);
    }

    public boolean isUsbNotification(ExpandedNotification n) {
        return isMtpSwitcherNotification(n) || isUsbModeNotification(n) || isChargingNotification(n);
    }

    private boolean isChargingNotification(ExpandedNotification n) {
        int id = n.getId();
        return "android".equals(n.getPackageName()) && (id == this.mChargingNotificationId || id == 32);
    }

    private boolean isMtpSwitcherNotification(ExpandedNotification n) {
        int id = n.getId();
        return "android".equals(n.getPackageName()) && (id == this.mPtpNotificationId || id == this.mMtpNotificationId || id == this.mCdInstallNotificationId);
    }

    private boolean isUsbModeNotification(ExpandedNotification n) {
        int id = n.getId();
        return this.mEnableUsbModeSelection && "com.android.systemui".equals(n.getPackageName()) && (id == 1397773634 || id == 1397772886 || id == 1396986699 || id == 1397575510);
    }
}
