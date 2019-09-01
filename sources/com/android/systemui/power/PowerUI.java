package com.android.systemui.power;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.Slog;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.keyguard.charge.ChargeUtils;
import com.android.systemui.Constants;
import com.android.systemui.R;
import com.android.systemui.SystemUI;
import com.android.systemui.Util;
import com.android.systemui.events.ScreenOffEvent;
import com.android.systemui.events.ScreenOnEvent;
import com.android.systemui.recents.events.RecentsEventBus;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import miui.securityspace.CrossUserUtils;
import miui.util.FeatureParser;
import miui.util.HapticFeedbackUtil;

public class PowerUI extends SystemUI {
    private int mBatteryLevel = 100;
    /* access modifiers changed from: private */
    public ImageView mBatteryLevelImageView;
    /* access modifiers changed from: private */
    public TextView mBatteryLevelTextView;
    private int mBatteryStatus = 1;
    private Handler mBgHandler;
    private HandlerThread mBgThread;
    /* access modifiers changed from: private */
    public AlertDialog mExtremePowerSaveDialog;
    private final Handler mHandler = new Handler();
    private HapticFeedbackUtil mHapticFeedBack;
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                char c = 65535;
                if (action.hashCode() == -1538406691 && action.equals("android.intent.action.BATTERY_CHANGED")) {
                    c = 0;
                }
                if (c != 0) {
                    Slog.w("PowerUI", "unknown intent: " + intent);
                } else {
                    PowerUI.this.handleBatteryChanged(intent);
                }
            }
        }
    };
    private int mInvalidCharger = 0;
    /* access modifiers changed from: private */
    public AlertDialog mInvalidChargerDialog;
    private int mLowBatteryAlertCloseLevel = 30;
    /* access modifiers changed from: private */
    public AlertDialog mLowBatteryDialog;
    private int[] mLowBatteryReminderLevels = {20, 10, -1};
    /* access modifiers changed from: private */
    public AlertDialog mLowTemperatureDialog;
    private boolean mNeedShowLowTemperatureDialog = true;
    /* access modifiers changed from: private */
    public int mPlugType = 0;
    private boolean mScreenOn = true;

    private final class ShowPowerSaveDialogTask extends AsyncTask<Void, Void, Bundle> {
        private ShowPowerSaveDialogTask() {
        }

        /* access modifiers changed from: protected */
        public Bundle doInBackground(Void... voids) {
            Bundle bundle = null;
            try {
                bundle = PowerUI.this.mContext.getContentResolver().call(Uri.parse("content://com.miui.powercenter.provider"), "getBatteryInfo", null, null);
            } catch (Exception e) {
                Log.e("PowerUI", "getBatteryInfo error", e);
            }
            if (bundle == null) {
                return null;
            }
            long lastChargedTime = bundle.getLong("last_charged_time");
            long drainedTime = bundle.getLong("last_drained_time");
            int drainedPercent = bundle.getInt("last_drained_percent");
            Bundle data = new Bundle();
            data.putLong("lastChargedTime", lastChargedTime);
            data.putLong("drainedTime", drainedTime);
            data.putInt("drainedPercent", drainedPercent);
            return data;
        }

        /* access modifiers changed from: protected */
        public void onPostExecute(Bundle bundle) {
            View v = View.inflate(PowerUI.this.mContext, R.layout.extreme_power_save_view, null);
            String expectedTimeStr = PowerUI.this.getExpectedTimeString(PowerUI.this.mContext, bundle);
            ((TextView) v.findViewById(R.id.expected_time)).setText(PowerUI.this.getSpanStr(PowerUI.this.mContext.getString(R.string.battery_can_use_time, new Object[]{expectedTimeStr}), expectedTimeStr));
            AlertDialog.Builder b = new AlertDialog.Builder(PowerUI.this.mContext, com.android.systemui.plugins.R.style.Theme_Dialog_Alert);
            b.setCancelable(true);
            b.setTitle(R.string.open_extreme_power_save_mode_title);
            b.setView(v);
            b.setNegativeButton(R.string.dlg_cancel, null);
            b.setPositiveButton(R.string.dlg_confirm, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    PowerUtils.enableExtremeSaveMode(PowerUI.this.mContext, "extreme_dialog");
                }
            });
            PowerUI.this.dismissExtremePowerSaveDialog();
            AlertDialog d = b.create();
            d.getWindow().setType(2003);
            d.show();
            AlertDialog unused = PowerUI.this.mExtremePowerSaveDialog = d;
        }
    }

    public void start() {
        this.mLowBatteryAlertCloseLevel = 30;
        this.mLowBatteryReminderLevels[0] = 20;
        this.mLowBatteryReminderLevels[1] = 10;
        if (isSupportExtremeMode()) {
            this.mLowBatteryReminderLevels[2] = 5;
        }
        this.mHapticFeedBack = new HapticFeedbackUtil(this.mContext, false);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.BATTERY_CHANGED");
        RecentsEventBus.getDefault().register(this);
        this.mContext.registerReceiver(this.mIntentReceiver, filter, null, this.mHandler);
        this.mBgThread = new HandlerThread("PowerUI", 10);
        this.mBgThread.start();
        this.mBgHandler = new Handler(this.mBgThread.getLooper()) {
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 1:
                        PowerUI.this.playLowBatterySound();
                        return;
                    case 2:
                        PowerUI.this.playBatterySound(Uri.fromFile(Constants.SOUND_DISCONNECT));
                        return;
                    case 3:
                        PowerUI.this.playBatterySound(Uri.fromFile(PowerUI.this.mPlugType == 4 ? Constants.SOUND_CHARGE_WIRELESS : Constants.SOUND_CHARGING));
                        return;
                    default:
                        return;
                }
            }
        };
    }

    private int findBatteryLevelBucket(int level) {
        if (level >= this.mLowBatteryAlertCloseLevel) {
            return 1;
        }
        if (level >= this.mLowBatteryReminderLevels[0]) {
            return 0;
        }
        for (int i = this.mLowBatteryReminderLevels.length - 1; i >= 0; i--) {
            if (level <= this.mLowBatteryReminderLevels[i] && this.mLowBatteryReminderLevels[i] > 0) {
                return -1 - i;
            }
        }
        throw new RuntimeException("not possible!");
    }

    public final void onBusEvent(ScreenOffEvent event) {
        Log.d("PowerUI", "onBusEvent ScreenOffEvent");
        this.mScreenOn = false;
    }

    public final void onBusEvent(ScreenOnEvent event) {
        Log.d("PowerUI", "onBusEvent ScreenOnEvent");
        this.mScreenOn = true;
    }

    /* access modifiers changed from: private */
    public void handleBatteryChanged(Intent intent) {
        int oldBatteryLevel = this.mBatteryLevel;
        this.mBatteryLevel = intent.getIntExtra("level", 100);
        this.mBatteryStatus = intent.getIntExtra("status", 1);
        int oldPlugType = this.mPlugType;
        this.mPlugType = intent.getIntExtra("plugged", 1);
        int oldInvalidCharger = this.mInvalidCharger;
        this.mInvalidCharger = intent.getIntExtra("invalid_charger", 0);
        boolean plugged = this.mPlugType != 0;
        boolean oldPlugged = oldPlugType != 0;
        int oldBucket = findBatteryLevelBucket(oldBatteryLevel);
        int bucket = findBatteryLevelBucket(this.mBatteryLevel);
        int temperature = intent.getIntExtra("temperature", 0);
        if (!this.mNeedShowLowTemperatureDialog || this.mLowTemperatureDialog != null || temperature > -80 || this.mBatteryLevel > 50) {
            if (temperature >= 0) {
                this.mNeedShowLowTemperatureDialog = true;
                dismissLowTemperatureDialog();
            } else if (this.mLowTemperatureDialog != null) {
                return;
            }
            if (oldInvalidCharger != 0 || this.mInvalidCharger == 0) {
                if (oldInvalidCharger != 0 && this.mInvalidCharger == 0) {
                    dismissInvalidChargerDialog();
                } else if (this.mInvalidChargerDialog != null) {
                    return;
                }
                boolean needPlayDisconnctSound = true;
                if (!plugged && ((bucket < oldBucket || oldPlugged) && this.mBatteryStatus != 1 && bucket < 0)) {
                    showLowBatteryWarning();
                    showLowBatteryNotification();
                    this.mBgHandler.removeMessages(1);
                    this.mBgHandler.obtainMessage(1).sendToTarget();
                    needPlayDisconnctSound = false;
                } else if (plugged || (bucket > oldBucket && bucket > 0)) {
                    dismissLowBatteryWarning();
                    dismissExtremePowerSaveDialog();
                    hideLowBatteryNotification();
                } else if (this.mBatteryLevelTextView != null) {
                    showLowBatteryWarning();
                }
                if (this.mPlugType != oldPlugType) {
                    this.mHapticFeedBack.performHapticFeedback(0, false);
                }
                if (plugged && !oldPlugged) {
                    this.mBgHandler.removeMessages(3);
                    this.mBgHandler.obtainMessage(3).sendToTarget();
                } else if (needPlayDisconnctSound && !plugged && oldPlugged) {
                    this.mBgHandler.removeMessages(2);
                    this.mBgHandler.obtainMessage(2).sendToTarget();
                }
                return;
            }
            Slog.d("PowerUI", "showing invalid charger warning");
            showInvalidChargerDialog();
            return;
        }
        showLowTemperatureDialog();
        this.mNeedShowLowTemperatureDialog = false;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println(PowerUI.class.getSimpleName() + " state:");
        pw.println("  mBatteryLevel:" + this.mBatteryLevel);
        pw.println("  mBatteryStatus:" + this.mBatteryStatus);
        pw.println("  mPlugType:" + this.mPlugType);
        pw.println("  mInvalidCharger:" + this.mInvalidCharger);
        pw.println("  mScreenOn:" + this.mScreenOn);
    }

    /* access modifiers changed from: package-private */
    public void dismissLowBatteryWarning() {
        if (this.mLowBatteryDialog != null) {
            Slog.i("PowerUI", "closing low battery warning: level=" + this.mBatteryLevel);
            this.mLowBatteryDialog.dismiss();
        }
    }

    /* access modifiers changed from: package-private */
    public void showLowBatteryWarning() {
        int resId;
        StringBuilder sb = new StringBuilder();
        sb.append(this.mBatteryLevelTextView == null ? "showing" : "updating");
        sb.append(" low battery warning: level=");
        sb.append(this.mBatteryLevel);
        sb.append(" [");
        sb.append(findBatteryLevelBucket(this.mBatteryLevel));
        sb.append("]");
        Slog.i("PowerUI", sb.toString());
        if (!isLowBatteryDialogAvailable()) {
            Slog.i("PowerUI", "low battery dialog not shown");
        } else if (Settings.System.getIntForUser(this.mContext.getContentResolver(), "low_battery_dialog_disabled", 0, -2) != 1 && 1 != Settings.System.getInt(this.mContext.getContentResolver(), "vr_mode", 0) && ((TelephonyManager) this.mContext.getSystemService("phone")).getCallState() != 1 && deviceIsProvisioned()) {
            CharSequence levelText = this.mContext.getString(R.string.battery_low_percent_format, new Object[]{Integer.valueOf(this.mBatteryLevel)});
            if (this.mBatteryLevel <= 4) {
                resId = R.drawable.battery_low_4_percent;
            } else if (this.mBatteryLevel <= 9) {
                resId = R.drawable.battery_low_9_percent;
            } else if (this.mBatteryLevel <= 14) {
                resId = R.drawable.battery_low_14_percent;
            } else {
                resId = R.drawable.battery_low_19_percent;
            }
            if (this.mBatteryLevelTextView != null) {
                this.mBatteryLevelTextView.setText(levelText);
                this.mBatteryLevelImageView.setImageResource(resId);
            } else {
                View v = View.inflate(this.mContext, R.layout.battery_low, null);
                this.mBatteryLevelTextView = (TextView) v.findViewById(R.id.level_percent);
                this.mBatteryLevelImageView = (ImageView) v.findViewById(R.id.image);
                this.mBatteryLevelTextView.setText(levelText);
                this.mBatteryLevelImageView.setImageResource(resId);
                AlertDialog.Builder b = new AlertDialog.Builder(this.mContext, com.android.systemui.plugins.R.style.Theme_Dialog_Alert);
                b.setCancelable(true);
                b.setTitle(R.string.battery_low_title);
                b.setView(v);
                b.setIconAttribute(16843605);
                b.setNegativeButton(R.string.save_mode_btn_ok, null);
                if (!Constants.IS_TABLET) {
                    if (isSupportExtremeMode() && this.mBatteryLevel <= 5 && !PowerUtils.isExtremeSaveModeEnabable(this.mContext)) {
                        b.setPositiveButton(R.string.enable_extreme_power_save_mode, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                if (PowerUI.this.isFirstOpenExtremePowerSave()) {
                                    new ShowPowerSaveDialogTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
                                    PowerUI.this.setFirstOpenExtremePowerSave(false);
                                    return;
                                }
                                PowerUtils.enableExtremeSaveMode(PowerUI.this.mContext, "5percent_dialog");
                            }
                        });
                    } else if (!PowerUtils.isExtremeSaveModeEnabable(this.mContext) && !PowerUtils.isSaveModeEnabled(this.mContext)) {
                        b.setPositiveButton(R.string.enable_power_save_mode, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                PowerUtils.enableSaveMode(PowerUI.this.mContext);
                                PowerUI.this.dismissLowBatteryWarning();
                            }
                        });
                    }
                }
                AlertDialog d = b.create();
                d.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    public void onDismiss(DialogInterface dialog) {
                        AlertDialog unused = PowerUI.this.mLowBatteryDialog = null;
                        TextView unused2 = PowerUI.this.mBatteryLevelTextView = null;
                        ImageView unused3 = PowerUI.this.mBatteryLevelImageView = null;
                        PowerUtils.hideLowBatteryNotification(PowerUI.this.mContext);
                    }
                });
                d.getWindow().setType(2003);
                d.show();
                this.mLowBatteryDialog = d;
                PowerUtils.trackLowBatteryDialog(this.mContext);
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void dismissInvalidChargerDialog() {
        if (this.mInvalidChargerDialog != null) {
            this.mInvalidChargerDialog.dismiss();
        }
    }

    /* access modifiers changed from: package-private */
    public void showInvalidChargerDialog() {
        Slog.d("PowerUI", "showing invalid charger dialog");
        dismissLowBatteryWarning();
        dismissExtremePowerSaveDialog();
        AlertDialog.Builder b = new AlertDialog.Builder(this.mContext, com.android.systemui.plugins.R.style.Theme_Dialog_Alert);
        b.setCancelable(true);
        b.setMessage(R.string.invalid_charger);
        b.setIconAttribute(16843605);
        b.setPositiveButton(17039370, null);
        AlertDialog d = b.create();
        d.setOnDismissListener(new DialogInterface.OnDismissListener() {
            public void onDismiss(DialogInterface dialog) {
                AlertDialog unused = PowerUI.this.mInvalidChargerDialog = null;
                TextView unused2 = PowerUI.this.mBatteryLevelTextView = null;
                ImageView unused3 = PowerUI.this.mBatteryLevelImageView = null;
            }
        });
        d.getWindow().setType(2003);
        d.show();
        this.mInvalidChargerDialog = d;
    }

    /* access modifiers changed from: package-private */
    public void dismissLowTemperatureDialog() {
        if (this.mLowTemperatureDialog != null) {
            this.mLowTemperatureDialog.dismiss();
        }
    }

    /* access modifiers changed from: package-private */
    public void showLowTemperatureDialog() {
        Slog.d("PowerUI", "showing low temperature dialog");
        dismissLowBatteryWarning();
        dismissInvalidChargerDialog();
        dismissExtremePowerSaveDialog();
        AlertDialog.Builder b = new AlertDialog.Builder(this.mContext, com.android.systemui.plugins.R.style.Theme_Dialog_Alert);
        b.setCancelable(true);
        b.setTitle(R.string.low_temperature_warning_title);
        b.setMessage(R.string.low_temperature_warning_message);
        b.setIconAttribute(16843605);
        b.setPositiveButton(R.string.low_temperature_button_ok, null);
        AlertDialog d = b.create();
        d.setOnDismissListener(new DialogInterface.OnDismissListener() {
            public void onDismiss(DialogInterface dialog) {
                AlertDialog unused = PowerUI.this.mLowTemperatureDialog = null;
            }
        });
        d.getWindow().setType(2010);
        d.show();
        this.mLowTemperatureDialog = d;
    }

    /* access modifiers changed from: private */
    public void playLowBatterySound() {
        String soundPath = Settings.System.getString(this.mContext.getContentResolver(), "low_battery_sound");
        if (soundPath != null) {
            Context context = this.mContext;
            Util.playRingtoneAsync(context, Uri.parse("file://" + soundPath), 1);
        }
    }

    /* access modifiers changed from: private */
    public void playBatterySound(Uri soundUri) {
        if (Settings.System.getInt(this.mContext.getContentResolver(), "power_sounds_enabled", 1) == 1 && soundUri != null) {
            Util.playRingtoneAsync(this.mContext, soundUri, 1);
        }
    }

    private boolean deviceIsProvisioned() {
        return Settings.Secure.getInt(this.mContext.getContentResolver(), "device_provisioned", 0) != 0;
    }

    private void showLowBatteryNotification() {
        if (!this.mScreenOn || !isLowBatteryDialogAvailable()) {
            PowerUtils.showLowBatteryNotification(this.mContext, this.mBatteryLevel);
        }
    }

    private boolean isLowBatteryDialogAvailable() {
        return this.mContext.getResources().getConfiguration().orientation == 1;
    }

    private void hideLowBatteryNotification() {
        PowerUtils.hideLowBatteryNotification(this.mContext);
    }

    /* access modifiers changed from: private */
    public boolean isFirstOpenExtremePowerSave() {
        return Settings.Secure.getInt(this.mContext.getContentResolver(), "is_first_open_extreme_power_save", 1) == 1;
    }

    public void setFirstOpenExtremePowerSave(boolean isFirst) {
        Settings.Secure.putInt(this.mContext.getContentResolver(), "is_first_open_extreme_power_save", isFirst);
    }

    /* access modifiers changed from: private */
    public void dismissExtremePowerSaveDialog() {
        if (this.mExtremePowerSaveDialog != null) {
            this.mExtremePowerSaveDialog.dismiss();
        }
    }

    /* access modifiers changed from: private */
    public String getExpectedTimeString(Context context, Bundle bundle) {
        long drainedTime = bundle.getLong("drainedTime");
        if (drainedTime <= 0) {
            return "-";
        }
        long minutes = ChargeUtils.getMins(drainedTime);
        long hours = ChargeUtils.getHours(drainedTime);
        if (hours > 0 && minutes > 0) {
            return context.getResources().getQuantityString(R.plurals.keyguard_charging_info_drained_time_format, (int) hours, new Object[]{Long.valueOf(hours), Long.valueOf(minutes)});
        } else if (hours > 0) {
            return context.getResources().getQuantityString(R.plurals.keyguard_charging_info_drained_hour_time_format, (int) hours, new Object[]{Long.valueOf(hours)});
        } else if (minutes <= 0) {
            return "-";
        } else {
            return context.getResources().getQuantityString(R.plurals.keyguard_charging_info_drained_min_time_format, (int) minutes, new Object[]{Long.valueOf(minutes)});
        }
    }

    /* access modifiers changed from: private */
    public SpannableString getSpanStr(String str, String childStr) {
        int index = str.indexOf(childStr, 0);
        SpannableString ss = new SpannableString(str);
        if (index > 0) {
            ss.setSpan(new ForegroundColorSpan(this.mContext.getColor(R.color.extreme_drained_time_color)), index, childStr.length() + index, 33);
        }
        return ss;
    }

    private boolean isSupportExtremeMode() {
        if (!FeatureParser.getBoolean("support_extreme_battery_saver", false) || CrossUserUtils.getCurrentUserId() != 0) {
            return false;
        }
        return true;
    }
}
