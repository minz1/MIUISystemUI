package com.android.systemui.globalactions;

import android.app.ActivityManager;
import android.app.ActivityManagerCompat;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.Vibrator;
import android.provider.Settings;
import android.service.dreams.IDreamManager;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import android.view.accessibility.AccessibilityEvent;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.android.internal.app.AlertController;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.util.EmergencyAffordanceManager;
import com.android.internal.widget.LockPatternUtils;
import com.android.systemui.plugins.GlobalActions;
import java.util.ArrayList;
import java.util.List;

class GlobalActionsDialog implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener {
    /* access modifiers changed from: private */
    public MyAdapter mAdapter;
    private ContentObserver mAirplaneModeObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            GlobalActionsDialog.this.onAirplaneModeChanged();
        }
    };
    /* access modifiers changed from: private */
    public ToggleAction mAirplaneModeOn;
    /* access modifiers changed from: private */
    public ToggleAction.State mAirplaneState = ToggleAction.State.Off;
    /* access modifiers changed from: private */
    public final AudioManager mAudioManager;
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(action) || "android.intent.action.SCREEN_OFF".equals(action)) {
                if (!"globalactions".equals(intent.getStringExtra("reason"))) {
                    GlobalActionsDialog.this.mHandler.sendEmptyMessage(0);
                }
            } else if ("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED".equals(action) && !intent.getBooleanExtra("PHONE_IN_ECM_STATE", false) && GlobalActionsDialog.this.mIsWaitingForEcmExit) {
                boolean unused = GlobalActionsDialog.this.mIsWaitingForEcmExit = false;
                GlobalActionsDialog.this.changeAirplaneModeSystemSetting(true);
            }
        }
    };
    /* access modifiers changed from: private */
    public final Context mContext;
    /* access modifiers changed from: private */
    public boolean mDeviceProvisioned = false;
    /* access modifiers changed from: private */
    public ActionsDialog mDialog;
    private final IDreamManager mDreamManager;
    /* access modifiers changed from: private */
    public final EmergencyAffordanceManager mEmergencyAffordanceManager;
    /* access modifiers changed from: private */
    public Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    if (GlobalActionsDialog.this.mDialog != null) {
                        GlobalActionsDialog.this.mDialog.dismiss();
                        ActionsDialog unused = GlobalActionsDialog.this.mDialog = null;
                        return;
                    }
                    return;
                case 1:
                    GlobalActionsDialog.this.refreshSilentMode();
                    GlobalActionsDialog.this.mAdapter.notifyDataSetChanged();
                    return;
                case 2:
                    GlobalActionsDialog.this.handleShow();
                    return;
                default:
                    return;
            }
        }
    };
    /* access modifiers changed from: private */
    public boolean mHasTelephony;
    private boolean mHasVibrator;
    /* access modifiers changed from: private */
    public boolean mIsWaitingForEcmExit = false;
    /* access modifiers changed from: private */
    public ArrayList<Action> mItems;
    /* access modifiers changed from: private */
    public boolean mKeyguardShowing = false;
    PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        public void onServiceStateChanged(ServiceState serviceState) {
            if (GlobalActionsDialog.this.mHasTelephony) {
                ToggleAction.State unused = GlobalActionsDialog.this.mAirplaneState = serviceState.getState() == 3 ? ToggleAction.State.On : ToggleAction.State.Off;
                GlobalActionsDialog.this.mAirplaneModeOn.updateState(GlobalActionsDialog.this.mAirplaneState);
                GlobalActionsDialog.this.mAdapter.notifyDataSetChanged();
            }
        }
    };
    private BroadcastReceiver mRingerModeReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.media.RINGER_MODE_CHANGED")) {
                GlobalActionsDialog.this.mHandler.sendEmptyMessage(1);
            }
        }
    };
    private final boolean mShowSilentToggle;
    private Action mSilentModeAction;
    /* access modifiers changed from: private */
    public final GlobalActions.GlobalActionsManager mWindowManagerFuncs;

    private interface Action {
        View create(Context context, View view, ViewGroup viewGroup, LayoutInflater layoutInflater);

        CharSequence getLabelForAccessibility(Context context);

        boolean isEnabled();

        void onPress();

        boolean showBeforeProvisioning();

        boolean showDuringKeyguard();
    }

    private static final class ActionsDialog extends Dialog implements DialogInterface {
        private final MyAdapter mAdapter;
        private final AlertController mAlert = AlertController.create(this.mContext, this, getWindow());
        private final Context mContext = getContext();

        public ActionsDialog(Context context, AlertController.AlertParams params) {
            super(context, getDialogTheme(context));
            this.mAdapter = (MyAdapter) params.mAdapter;
            params.apply(this.mAlert);
        }

        private static int getDialogTheme(Context context) {
            TypedValue outValue = new TypedValue();
            context.getTheme().resolveAttribute(16843529, outValue, true);
            return outValue.resourceId;
        }

        /* access modifiers changed from: protected */
        public void onStart() {
            super.setCanceledOnTouchOutside(true);
            super.onStart();
        }

        public ListView getListView() {
            return this.mAlert.getListView();
        }

        /* access modifiers changed from: protected */
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            this.mAlert.installContent();
        }

        public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
            if (event.getEventType() == 32) {
                for (int i = 0; i < this.mAdapter.getCount(); i++) {
                    CharSequence label = this.mAdapter.getItem(i).getLabelForAccessibility(getContext());
                    if (label != null) {
                        event.getText().add(label);
                    }
                }
            }
            return super.dispatchPopulateAccessibilityEvent(event);
        }

        public boolean onKeyDown(int keyCode, KeyEvent event) {
            if (this.mAlert.onKeyDown(keyCode, event)) {
                return true;
            }
            return super.onKeyDown(keyCode, event);
        }

        public boolean onKeyUp(int keyCode, KeyEvent event) {
            if (this.mAlert.onKeyUp(keyCode, event)) {
                return true;
            }
            return super.onKeyUp(keyCode, event);
        }
    }

    private class BugReportAction extends SinglePressAction implements LongPressAction {
        public BugReportAction() {
            super(17302418, 17039607);
        }

        public void onPress() {
            if (!ActivityManager.isUserAMonkey()) {
                GlobalActionsDialog.this.mHandler.postDelayed(new Runnable() {
                    public void run() {
                        try {
                            MetricsLogger.action(GlobalActionsDialog.this.mContext, 292);
                            ActivityManagerCompat.getService().requestBugReport(1);
                        } catch (RemoteException e) {
                        }
                    }
                }, 500);
            }
        }

        public boolean onLongPress() {
            if (ActivityManager.isUserAMonkey()) {
                return false;
            }
            try {
                MetricsLogger.action(GlobalActionsDialog.this.mContext, 293);
                ActivityManagerCompat.getService().requestBugReport(0);
            } catch (RemoteException e) {
            }
            return false;
        }

        public boolean showDuringKeyguard() {
            return true;
        }

        public boolean showBeforeProvisioning() {
            return false;
        }

        public String getStatus() {
            return GlobalActionsDialog.this.mContext.getString(17039606, new Object[]{Build.VERSION.RELEASE, Build.ID});
        }
    }

    private interface LongPressAction extends Action {
        boolean onLongPress();
    }

    private class MyAdapter extends BaseAdapter {
        private MyAdapter() {
        }

        public int getCount() {
            int count = 0;
            for (int i = 0; i < GlobalActionsDialog.this.mItems.size(); i++) {
                Action action = (Action) GlobalActionsDialog.this.mItems.get(i);
                if ((!GlobalActionsDialog.this.mKeyguardShowing || action.showDuringKeyguard()) && (GlobalActionsDialog.this.mDeviceProvisioned || action.showBeforeProvisioning())) {
                    count++;
                }
            }
            return count;
        }

        public boolean isEnabled(int position) {
            return getItem(position).isEnabled();
        }

        public boolean areAllItemsEnabled() {
            return false;
        }

        public Action getItem(int position) {
            int filteredPos = 0;
            for (int i = 0; i < GlobalActionsDialog.this.mItems.size(); i++) {
                Action action = (Action) GlobalActionsDialog.this.mItems.get(i);
                if ((!GlobalActionsDialog.this.mKeyguardShowing || action.showDuringKeyguard()) && (GlobalActionsDialog.this.mDeviceProvisioned || action.showBeforeProvisioning())) {
                    if (filteredPos == position) {
                        return action;
                    }
                    filteredPos++;
                }
            }
            throw new IllegalArgumentException("position " + position + " out of range of showable actions, filtered count=" + getCount() + ", keyguardshowing=" + GlobalActionsDialog.this.mKeyguardShowing + ", provisioned=" + GlobalActionsDialog.this.mDeviceProvisioned);
        }

        public long getItemId(int position) {
            return (long) position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            return getItem(position).create(GlobalActionsDialog.this.mContext, convertView, parent, LayoutInflater.from(GlobalActionsDialog.this.mContext));
        }
    }

    private final class PowerAction extends SinglePressAction implements LongPressAction {
        private PowerAction() {
            super(17301552, 17040003);
        }

        public boolean onLongPress() {
            if (((UserManager) GlobalActionsDialog.this.mContext.getSystemService("user")).hasUserRestriction("no_safe_boot")) {
                return false;
            }
            GlobalActionsDialog.this.mWindowManagerFuncs.reboot(true);
            return true;
        }

        public boolean showDuringKeyguard() {
            return true;
        }

        public boolean showBeforeProvisioning() {
            return true;
        }

        public void onPress() {
            GlobalActionsDialog.this.mWindowManagerFuncs.shutdown();
        }
    }

    private final class RestartAction extends SinglePressAction implements LongPressAction {
        private RestartAction() {
            super(17302743, 17040005);
        }

        public boolean onLongPress() {
            if (((UserManager) GlobalActionsDialog.this.mContext.getSystemService("user")).hasUserRestriction("no_safe_boot")) {
                return false;
            }
            GlobalActionsDialog.this.mWindowManagerFuncs.reboot(true);
            return true;
        }

        public boolean showDuringKeyguard() {
            return true;
        }

        public boolean showBeforeProvisioning() {
            return true;
        }

        public void onPress() {
            GlobalActionsDialog.this.mWindowManagerFuncs.reboot(false);
        }
    }

    private class SilentModeToggleAction extends ToggleAction {
        public SilentModeToggleAction() {
            super(17302283, 17302282, 17040011, 17040009, 17040008);
        }

        /* access modifiers changed from: package-private */
        public void onToggle(boolean on) {
            if (on) {
                GlobalActionsDialog.this.mAudioManager.setRingerMode(0);
            } else {
                GlobalActionsDialog.this.mAudioManager.setRingerMode(2);
            }
        }

        public boolean showDuringKeyguard() {
            return true;
        }

        public boolean showBeforeProvisioning() {
            return false;
        }
    }

    private static class SilentModeTriStateAction implements View.OnClickListener, Action {
        private final int[] ITEM_IDS = {16909167, 16909168, 16909169};
        private final AudioManager mAudioManager;
        private final Context mContext;
        private final Handler mHandler;

        SilentModeTriStateAction(Context context, AudioManager audioManager, Handler handler) {
            this.mAudioManager = audioManager;
            this.mHandler = handler;
            this.mContext = context;
        }

        private int ringerModeToIndex(int ringerMode) {
            return ringerMode;
        }

        private int indexToRingerMode(int index) {
            return index;
        }

        public CharSequence getLabelForAccessibility(Context context) {
            return null;
        }

        public View create(Context context, View convertView, ViewGroup parent, LayoutInflater inflater) {
            View v = inflater.inflate(17367148, parent, false);
            int selectedIndex = ringerModeToIndex(this.mAudioManager.getRingerMode());
            int i = 0;
            while (i < 3) {
                View itemView = v.findViewById(this.ITEM_IDS[i]);
                itemView.setSelected(selectedIndex == i);
                itemView.setTag(Integer.valueOf(i));
                itemView.setOnClickListener(this);
                i++;
            }
            return v;
        }

        public void onPress() {
        }

        public boolean showDuringKeyguard() {
            return true;
        }

        public boolean showBeforeProvisioning() {
            return false;
        }

        public boolean isEnabled() {
            return true;
        }

        public void onClick(View v) {
            if (v.getTag() instanceof Integer) {
                this.mAudioManager.setRingerMode(indexToRingerMode(((Integer) v.getTag()).intValue()));
                this.mHandler.sendEmptyMessageDelayed(0, 300);
            }
        }
    }

    private static abstract class SinglePressAction implements Action {
        private final Drawable mIcon;
        private final int mIconResId;
        private final CharSequence mMessage;
        private final int mMessageResId;

        public abstract void onPress();

        protected SinglePressAction(int iconResId, int messageResId) {
            this.mIconResId = iconResId;
            this.mMessageResId = messageResId;
            this.mMessage = null;
            this.mIcon = null;
        }

        protected SinglePressAction(int iconResId, Drawable icon, CharSequence message) {
            this.mIconResId = iconResId;
            this.mMessageResId = 0;
            this.mMessage = message;
            this.mIcon = icon;
        }

        public boolean isEnabled() {
            return true;
        }

        public String getStatus() {
            return null;
        }

        public CharSequence getLabelForAccessibility(Context context) {
            if (this.mMessage != null) {
                return this.mMessage;
            }
            return context.getString(this.mMessageResId);
        }

        public View create(Context context, View convertView, ViewGroup parent, LayoutInflater inflater) {
            View v = inflater.inflate(17367147, parent, false);
            ImageView icon = (ImageView) v.findViewById(16908294);
            TextView messageView = (TextView) v.findViewById(16908299);
            TextView statusView = (TextView) v.findViewById(16909383);
            String status = getStatus();
            if (!TextUtils.isEmpty(status)) {
                statusView.setText(status);
            } else {
                statusView.setVisibility(8);
            }
            if (this.mIcon != null) {
                icon.setImageDrawable(this.mIcon);
                icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
            } else if (this.mIconResId != 0) {
                icon.setImageDrawable(context.getDrawable(this.mIconResId));
            }
            if (this.mMessage != null) {
                messageView.setText(this.mMessage);
            } else {
                messageView.setText(this.mMessageResId);
            }
            return v;
        }
    }

    private static abstract class ToggleAction implements Action {
        protected int mDisabledIconResid;
        protected int mDisabledStatusMessageResId;
        protected int mEnabledIconResId;
        protected int mEnabledStatusMessageResId;
        protected int mMessageResId;
        protected State mState = State.Off;

        enum State {
            Off(false),
            TurningOn(true),
            TurningOff(true),
            On(false);
            
            private final boolean inTransition;

            private State(boolean intermediate) {
                this.inTransition = intermediate;
            }

            public boolean inTransition() {
                return this.inTransition;
            }
        }

        /* access modifiers changed from: package-private */
        public abstract void onToggle(boolean z);

        public ToggleAction(int enabledIconResId, int disabledIconResid, int message, int enabledStatusMessageResId, int disabledStatusMessageResId) {
            this.mEnabledIconResId = enabledIconResId;
            this.mDisabledIconResid = disabledIconResid;
            this.mMessageResId = message;
            this.mEnabledStatusMessageResId = enabledStatusMessageResId;
            this.mDisabledStatusMessageResId = disabledStatusMessageResId;
        }

        /* access modifiers changed from: package-private */
        public void willCreate() {
        }

        public CharSequence getLabelForAccessibility(Context context) {
            return context.getString(this.mMessageResId);
        }

        public View create(Context context, View convertView, ViewGroup parent, LayoutInflater inflater) {
            willCreate();
            View v = inflater.inflate(17367147, parent, false);
            ImageView icon = (ImageView) v.findViewById(16908294);
            TextView messageView = (TextView) v.findViewById(16908299);
            TextView statusView = (TextView) v.findViewById(16909383);
            boolean enabled = isEnabled();
            if (messageView != null) {
                messageView.setText(this.mMessageResId);
                messageView.setEnabled(enabled);
            }
            boolean on = this.mState == State.On || this.mState == State.TurningOn;
            if (icon != null) {
                icon.setImageDrawable(context.getDrawable(on ? this.mEnabledIconResId : this.mDisabledIconResid));
                icon.setEnabled(enabled);
            }
            if (statusView != null) {
                statusView.setText(on ? this.mEnabledStatusMessageResId : this.mDisabledStatusMessageResId);
                statusView.setVisibility(0);
                statusView.setEnabled(enabled);
            }
            v.setEnabled(enabled);
            return v;
        }

        public final void onPress() {
            if (this.mState.inTransition()) {
                Log.w("GlobalActionsDialog", "shouldn't be able to toggle when in transition");
                return;
            }
            boolean nowOn = this.mState != State.On;
            onToggle(nowOn);
            changeStateFromPress(nowOn);
        }

        public boolean isEnabled() {
            return !this.mState.inTransition();
        }

        /* access modifiers changed from: protected */
        public void changeStateFromPress(boolean buttonOn) {
            this.mState = buttonOn ? State.On : State.Off;
        }

        public void updateState(State state) {
            this.mState = state;
        }
    }

    public GlobalActionsDialog(Context context, GlobalActions.GlobalActionsManager windowManagerFuncs) {
        boolean z = false;
        this.mContext = context;
        this.mWindowManagerFuncs = windowManagerFuncs;
        this.mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
        this.mDreamManager = IDreamManager.Stub.asInterface(ServiceManager.getService("dreams"));
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.CLOSE_SYSTEM_DIALOGS");
        filter.addAction("android.intent.action.SCREEN_OFF");
        filter.addAction("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED");
        context.registerReceiver(this.mBroadcastReceiver, filter);
        this.mHasTelephony = ((ConnectivityManager) context.getSystemService("connectivity")).isNetworkSupported(0);
        ((TelephonyManager) context.getSystemService("phone")).listen(this.mPhoneStateListener, 1);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("airplane_mode_on"), true, this.mAirplaneModeObserver);
        Vibrator vibrator = (Vibrator) this.mContext.getSystemService("vibrator");
        if (vibrator != null && vibrator.hasVibrator()) {
            z = true;
        }
        this.mHasVibrator = z;
        this.mShowSilentToggle = !this.mContext.getResources().getBoolean(17957068);
        this.mEmergencyAffordanceManager = new EmergencyAffordanceManager(context);
    }

    public void showDialog(boolean keyguardShowing, boolean isDeviceProvisioned) {
        this.mKeyguardShowing = keyguardShowing;
        this.mDeviceProvisioned = isDeviceProvisioned;
        if (this.mDialog != null) {
            this.mDialog.dismiss();
            this.mDialog = null;
            this.mHandler.sendEmptyMessage(2);
            return;
        }
        handleShow();
    }

    public void dismissDialog() {
        this.mHandler.removeMessages(0);
        this.mHandler.sendEmptyMessage(0);
    }

    private void awakenIfNecessary() {
        if (this.mDreamManager != null) {
            try {
                if (this.mDreamManager.isDreaming()) {
                    this.mDreamManager.awaken();
                }
            } catch (RemoteException e) {
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleShow() {
        awakenIfNecessary();
        this.mDialog = createDialog();
        prepareDialog();
        if (this.mAdapter.getCount() != 1 || !(this.mAdapter.getItem(0) instanceof SinglePressAction) || (this.mAdapter.getItem(0) instanceof LongPressAction)) {
            WindowManager.LayoutParams attrs = this.mDialog.getWindow().getAttributes();
            attrs.setTitle("ActionsDialog");
            this.mDialog.getWindow().setAttributes(attrs);
            this.mDialog.show();
            this.mWindowManagerFuncs.onGlobalActionsShown();
            this.mDialog.getWindow().getDecorView().setSystemUiVisibility(65536);
            return;
        }
        ((SinglePressAction) this.mAdapter.getItem(0)).onPress();
    }

    private ActionsDialog createDialog() {
        if (!this.mHasVibrator) {
            this.mSilentModeAction = new SilentModeToggleAction();
        } else {
            this.mSilentModeAction = new SilentModeTriStateAction(this.mContext, this.mAudioManager, this.mHandler);
        }
        AnonymousClass1 r4 = new ToggleAction(17302414, 17302416, 17040016, 17040015, 17040014) {
            /* access modifiers changed from: package-private */
            public void onToggle(boolean on) {
                if (!GlobalActionsDialog.this.mHasTelephony || !Boolean.parseBoolean(SystemProperties.get("ril.cdma.inecmmode"))) {
                    GlobalActionsDialog.this.changeAirplaneModeSystemSetting(on);
                    return;
                }
                boolean unused = GlobalActionsDialog.this.mIsWaitingForEcmExit = true;
                Intent ecmDialogIntent = new Intent("com.android.internal.intent.action.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS", null);
                ecmDialogIntent.addFlags(268435456);
                GlobalActionsDialog.this.mContext.startActivity(ecmDialogIntent);
            }

            /* access modifiers changed from: protected */
            public void changeStateFromPress(boolean buttonOn) {
                if (GlobalActionsDialog.this.mHasTelephony && !Boolean.parseBoolean(SystemProperties.get("ril.cdma.inecmmode"))) {
                    this.mState = buttonOn ? ToggleAction.State.TurningOn : ToggleAction.State.TurningOff;
                    ToggleAction.State unused = GlobalActionsDialog.this.mAirplaneState = this.mState;
                }
            }

            public boolean showDuringKeyguard() {
                return true;
            }

            public boolean showBeforeProvisioning() {
                return false;
            }
        };
        this.mAirplaneModeOn = r4;
        onAirplaneModeChanged();
        this.mItems = new ArrayList<>();
        String[] defaultActions = this.mContext.getResources().getStringArray(17236013);
        ArraySet<String> addedKeys = new ArraySet<>();
        for (String actionKey : defaultActions) {
            if (!addedKeys.contains(actionKey)) {
                if ("power".equals(actionKey)) {
                    this.mItems.add(new PowerAction());
                } else if ("airplane".equals(actionKey)) {
                    this.mItems.add(this.mAirplaneModeOn);
                } else if ("bugreport".equals(actionKey)) {
                    if (Settings.Global.getInt(this.mContext.getContentResolver(), "bugreport_in_power_menu", 0) != 0 && isCurrentUserOwner()) {
                        this.mItems.add(new BugReportAction());
                    }
                } else if ("silent".equals(actionKey)) {
                    if (this.mShowSilentToggle) {
                        this.mItems.add(this.mSilentModeAction);
                    }
                } else if ("users".equals(actionKey)) {
                    if (SystemProperties.getBoolean("fw.power_user_switcher", false)) {
                        addUsersToMenu(this.mItems);
                    }
                } else if ("settings".equals(actionKey)) {
                    this.mItems.add(getSettingsAction());
                } else if ("lockdown".equals(actionKey)) {
                    this.mItems.add(getLockdownAction());
                } else if ("voiceassist".equals(actionKey)) {
                    this.mItems.add(getVoiceAssistAction());
                } else if ("assist".equals(actionKey)) {
                    this.mItems.add(getAssistAction());
                } else if ("restart".equals(actionKey)) {
                    this.mItems.add(new RestartAction());
                } else {
                    Log.e("GlobalActionsDialog", "Invalid global action key " + actionKey);
                }
                addedKeys.add(actionKey);
            }
        }
        if (this.mEmergencyAffordanceManager.needsEmergencyAffordance()) {
            this.mItems.add(getEmergencyAction());
        }
        this.mAdapter = new MyAdapter();
        AlertController.AlertParams params = new AlertController.AlertParams(this.mContext);
        params.mAdapter = this.mAdapter;
        params.mOnClickListener = this;
        params.mForceInverseBackground = true;
        ActionsDialog dialog = new ActionsDialog(this.mContext, params);
        dialog.setCanceledOnTouchOutside(false);
        dialog.getListView().setItemsCanFocus(true);
        dialog.getListView().setLongClickable(true);
        dialog.getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
                Action action = GlobalActionsDialog.this.mAdapter.getItem(position);
                if (action instanceof LongPressAction) {
                    return ((LongPressAction) action).onLongPress();
                }
                return false;
            }
        });
        dialog.getWindow().setType(2009);
        dialog.setOnDismissListener(this);
        return dialog;
    }

    private Action getSettingsAction() {
        return new SinglePressAction(17302751, 17040007) {
            public void onPress() {
                Intent intent = new Intent("android.settings.SETTINGS");
                intent.addFlags(335544320);
                GlobalActionsDialog.this.mContext.startActivity(intent);
            }

            public boolean showDuringKeyguard() {
                return true;
            }

            public boolean showBeforeProvisioning() {
                return true;
            }
        };
    }

    private Action getEmergencyAction() {
        return new SinglePressAction(17302182, 17039999) {
            public void onPress() {
                GlobalActionsDialog.this.mEmergencyAffordanceManager.performEmergencyCall();
            }

            public boolean showDuringKeyguard() {
                return true;
            }

            public boolean showBeforeProvisioning() {
                return true;
            }
        };
    }

    private Action getAssistAction() {
        return new SinglePressAction(17302263, 17039995) {
            public void onPress() {
                Intent intent = new Intent("android.intent.action.ASSIST");
                intent.addFlags(335544320);
                GlobalActionsDialog.this.mContext.startActivity(intent);
            }

            public boolean showDuringKeyguard() {
                return true;
            }

            public boolean showBeforeProvisioning() {
                return true;
            }
        };
    }

    private Action getVoiceAssistAction() {
        return new SinglePressAction(17302783, 17040012) {
            public void onPress() {
                Intent intent = new Intent("android.intent.action.VOICE_ASSIST");
                intent.addFlags(335544320);
                GlobalActionsDialog.this.mContext.startActivity(intent);
            }

            public boolean showDuringKeyguard() {
                return true;
            }

            public boolean showBeforeProvisioning() {
                return true;
            }
        };
    }

    private Action getLockdownAction() {
        return new SinglePressAction(17301551, 17040001) {
            public void onPress() {
                new LockPatternUtils(GlobalActionsDialog.this.mContext).requireCredentialEntry(-1);
                try {
                    WindowManagerGlobal.getWindowManagerService().lockNow(null);
                } catch (RemoteException e) {
                    Log.e("GlobalActionsDialog", "Error while trying to lock device.", e);
                }
            }

            public boolean showDuringKeyguard() {
                return true;
            }

            public boolean showBeforeProvisioning() {
                return false;
            }
        };
    }

    private UserInfo getCurrentUser() {
        try {
            return ActivityManagerCompat.getService().getCurrentUser();
        } catch (RemoteException e) {
            return null;
        }
    }

    private boolean isCurrentUserOwner() {
        UserInfo currentUser = getCurrentUser();
        return currentUser == null || currentUser.isPrimary();
    }

    private void addUsersToMenu(ArrayList<Action> items) {
        Drawable drawable;
        UserManager um = (UserManager) this.mContext.getSystemService("user");
        if (um.isUserSwitcherEnabled()) {
            List<UserInfo> users = um.getUsers();
            UserInfo currentUser = getCurrentUser();
            for (UserInfo user : users) {
                if (user.supportsSwitchToByUser()) {
                    boolean z = false;
                    if (currentUser != null ? currentUser.id == user.id : user.id == 0) {
                        z = true;
                    }
                    boolean isCurrentUser = z;
                    if (user.iconPath != null) {
                        drawable = Drawable.createFromPath(user.iconPath);
                    } else {
                        drawable = null;
                    }
                    Drawable icon = drawable;
                    StringBuilder sb = new StringBuilder();
                    sb.append(user.name != null ? user.name : "Primary");
                    sb.append(isCurrentUser ? " ✔" : "");
                    final UserInfo userInfo = user;
                    AnonymousClass8 r5 = new SinglePressAction(17302636, icon, sb.toString()) {
                        public void onPress() {
                            try {
                                ActivityManagerCompat.getService().switchUser(userInfo.id);
                            } catch (RemoteException re) {
                                Log.e("GlobalActionsDialog", "Couldn't switch user " + re);
                            }
                        }

                        public boolean showDuringKeyguard() {
                            return true;
                        }

                        public boolean showBeforeProvisioning() {
                            return false;
                        }
                    };
                    items.add(r5);
                }
            }
        }
    }

    private void prepareDialog() {
        refreshSilentMode();
        this.mAirplaneModeOn.updateState(this.mAirplaneState);
        this.mAdapter.notifyDataSetChanged();
        this.mDialog.getWindow().setType(2009);
        if (this.mShowSilentToggle) {
            this.mContext.registerReceiver(this.mRingerModeReceiver, new IntentFilter("android.media.RINGER_MODE_CHANGED"));
        }
    }

    /* access modifiers changed from: private */
    public void refreshSilentMode() {
        if (!this.mHasVibrator) {
            ((ToggleAction) this.mSilentModeAction).updateState(this.mAudioManager.getRingerMode() != 2 ? ToggleAction.State.On : ToggleAction.State.Off);
        }
    }

    public void onDismiss(DialogInterface dialog) {
        this.mWindowManagerFuncs.onGlobalActionsHidden();
        if (this.mShowSilentToggle) {
            try {
                this.mContext.unregisterReceiver(this.mRingerModeReceiver);
            } catch (IllegalArgumentException ie) {
                Log.w("GlobalActionsDialog", ie);
            }
        }
    }

    public void onClick(DialogInterface dialog, int which) {
        if (!(this.mAdapter.getItem(which) instanceof SilentModeTriStateAction)) {
            dialog.dismiss();
        }
        this.mAdapter.getItem(which).onPress();
    }

    /* access modifiers changed from: private */
    public void onAirplaneModeChanged() {
        if (!this.mHasTelephony) {
            boolean airplaneModeOn = true;
            if (Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) != 1) {
                airplaneModeOn = false;
            }
            this.mAirplaneState = airplaneModeOn ? ToggleAction.State.On : ToggleAction.State.Off;
            this.mAirplaneModeOn.updateState(this.mAirplaneState);
        }
    }

    /* access modifiers changed from: private */
    public void changeAirplaneModeSystemSetting(boolean on) {
        Settings.Global.putInt(this.mContext.getContentResolver(), "airplane_mode_on", on);
        Intent intent = new Intent("android.intent.action.AIRPLANE_MODE");
        intent.addFlags(536870912);
        intent.putExtra("state", on);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        if (!this.mHasTelephony) {
            this.mAirplaneState = on ? ToggleAction.State.On : ToggleAction.State.Off;
        }
    }
}
