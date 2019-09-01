package com.android.systemui.miui.volume;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.media.AudioSystemCompat;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.util.Log;
import android.util.Slog;
import android.util.SparseBooleanArray;
import android.view.AccessibilityManagerCompat;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBarCompat;
import android.widget.SeekBar;
import android.widget.Toast;
import com.android.systemui.Dependency;
import com.android.systemui.Dumpable;
import com.android.systemui.Logger;
import com.android.systemui.Util;
import com.android.systemui.miui.analytics.AnalyticsWrapper;
import com.android.systemui.miui.volume.MiuiVolumeDialogMotion;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.VolumeDialog;
import com.android.systemui.plugins.VolumeDialogController;
import com.android.systemui.statistic.ScenarioConstants;
import com.android.systemui.statistic.ScenarioTrackUtil;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.volume.ConfigurableTexts;
import com.android.systemui.volume.Events;
import com.android.systemui.volume.SafetyWarningDialog;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import miui.app.AlertDialog;

public class MiuiVolumeDialogImpl implements Dumpable, VolumeDialog, ConfigurationController.ConfigurationListener, TunerService.Tunable {
    /* access modifiers changed from: private */
    public static final String TAG = Util.logTag(MiuiVolumeDialogImpl.class);
    private final Accessibility mAccessibility = new Accessibility();
    /* access modifiers changed from: private */
    public final AccessibilityManager mAccessibilityMgr;
    private int mActiveStream;
    private final AudioManager mAudioManager;
    private boolean mAutomute = true;
    private final View.OnClickListener mClickExpand = new View.OnClickListener() {
        public void onClick(View v) {
            ScenarioTrackUtil.beginScenario(ScenarioConstants.SCENARIO_EXPAND_VOLUME_DIALOG);
            AnalyticsWrapper.recordCountEventAnonymous("systemui_volume_dialog", "volume_click_more");
            if (!MiuiVolumeDialogImpl.this.mDialogView.isAnimating()) {
                boolean newExpand = !MiuiVolumeDialogImpl.this.mExpanded;
                Events.writeEvent(MiuiVolumeDialogImpl.this.mContext, 3, Boolean.valueOf(newExpand));
                MiuiVolumeDialogImpl.this.updateExpandedH(newExpand, false);
            }
        }
    };
    private final List<VolumeColumn> mColumns = new ArrayList();
    private ConfigurableTexts mConfigurableTexts;
    /* access modifiers changed from: private */
    public final Context mContext;
    /* access modifiers changed from: private */
    public VolumeDialogController mController;
    private final VolumeDialogController.Callbacks mControllerCallbackH = new VolumeDialogController.Callbacks() {
        public void onVolumeChanged(int stream, boolean fromKey) {
            MiuiVolumeDialogImpl.this.recordVolumeChanged(stream, fromKey);
        }

        public void onShowRequested(int reason) {
            MiuiVolumeDialogImpl.this.showH(reason);
        }

        public void onDismissRequested(int reason) {
            MiuiVolumeDialogImpl.this.dismissH(reason);
        }

        public void onScreenOff() {
            MiuiVolumeDialogImpl.this.dismissH(4);
        }

        public void onStateChanged(VolumeDialogController.State state) {
            MiuiVolumeDialogImpl.this.onStateChangedH(state);
        }

        public void onLayoutDirectionChanged(int layoutDirection) {
            MiuiVolumeDialogImpl.this.mDialogView.setLayoutDirection(layoutDirection);
        }

        public void onConfigurationChanged() {
        }

        public void onShowVibrateHint() {
            if (MiuiVolumeDialogImpl.this.mSilentMode) {
                MiuiVolumeDialogImpl.this.mController.setRingerMode(0, false);
            }
        }

        public void onShowSilentHint() {
            if (MiuiVolumeDialogImpl.this.mSilentMode) {
                MiuiVolumeDialogImpl.this.mController.setRingerMode(2, false);
            }
        }

        public void onShowSafetyWarning(int flags) {
            MiuiVolumeDialogImpl.this.showSafetyWarningH(flags);
        }

        public void onAccessibilityModeChanged(Boolean showA11yStream) {
            boolean unused = MiuiVolumeDialogImpl.this.mShowA11yStream = showA11yStream == null ? false : showA11yStream.booleanValue();
            MiuiVolumeDialogImpl.this.updateColumnH(MiuiVolumeDialogImpl.this.getActiveColumn());
        }
    };
    /* access modifiers changed from: private */
    public CustomDialog mDialog;
    private VolumeColumns mDialogColumns;
    private ViewGroup mDialogContentView;
    /* access modifiers changed from: private */
    public MiuiVolumeDialogView mDialogView;
    private final SparseBooleanArray mDynamic = new SparseBooleanArray();
    private ImageView mExpandButton;
    /* access modifiers changed from: private */
    public boolean mExpanded;
    /* access modifiers changed from: private */
    public final H mHandler = new H();
    /* access modifiers changed from: private */
    public boolean mHovering = false;
    /* access modifiers changed from: private */
    public ColorStateList mIconTintDark;
    private final KeyguardManager mKeyguard;
    private final KeyguardMonitor mKeyguardMonitor;
    private final Configuration mLastConfiguration = new Configuration();
    private int mLastDensity;
    private int mLockRecordTypes = 0;
    private ColorStateList mMutedColorList;
    private boolean mNeedReInit;
    /* access modifiers changed from: private */
    public boolean mPendingRecheckAll;
    /* access modifiers changed from: private */
    public boolean mPendingStateChanged;
    private BroadcastReceiver mRingerModeChangedReceiver = new BroadcastReceiver() {
        private int mRingerMode = -1;

        public void onReceive(Context context, Intent intent) {
            if ("android.media.RINGER_MODE_CHANGED".equals(intent.getAction())) {
                int ringerMode = intent.getIntExtra("android.media.EXTRA_RINGER_MODE", -1);
                if (this.mRingerMode != ringerMode) {
                    if (this.mRingerMode != -1 && ringerMode == 1) {
                        MiuiVolumeDialogImpl.this.mHandler.sendMessageDelayed(MiuiVolumeDialogImpl.this.mHandler.obtainMessage(8), 300);
                    }
                    this.mRingerMode = ringerMode;
                }
            }
        }
    };
    /* access modifiers changed from: private */
    public SafetyWarningDialog mSafetyWarning;
    /* access modifiers changed from: private */
    public final Object mSafetyWarningLock = new Object();
    /* access modifiers changed from: private */
    public boolean mShowA11yStream;
    /* access modifiers changed from: private */
    public boolean mShowing;
    private SilenceModeObserver mSilenceModeObserver = new SilenceModeObserver();
    /* access modifiers changed from: private */
    public boolean mSilentMode = true;
    /* access modifiers changed from: private */
    public VolumeDialogController.State mState;
    private VolumeColumn mTempColumn;
    private FrameLayout mTempColumnContainer;
    private Window mWindow;
    private int mWindowType;

    private final class Accessibility extends View.AccessibilityDelegate implements AccessibilityManager.AccessibilityStateChangeListener {
        private final View.OnAttachStateChangeListener mAttachListener;
        /* access modifiers changed from: private */
        public boolean mFeedbackEnabled;

        private Accessibility() {
            this.mAttachListener = new View.OnAttachStateChangeListener() {
                public void onViewDetachedFromWindow(View v) {
                    if (Util.DEBUG) {
                        Log.d(MiuiVolumeDialogImpl.TAG, "onViewDetachedFromWindow");
                    }
                }

                public void onViewAttachedToWindow(View v) {
                    if (Util.DEBUG) {
                        Log.d(MiuiVolumeDialogImpl.TAG, "onViewAttachedToWindow");
                    }
                    Accessibility.this.updateFeedbackEnabled();
                }
            };
        }

        public void init() {
            MiuiVolumeDialogImpl.this.mDialogView.addOnAttachStateChangeListener(this.mAttachListener);
            MiuiVolumeDialogImpl.this.mDialogView.setAccessibilityDelegate(this);
            MiuiVolumeDialogImpl.this.mAccessibilityMgr.addAccessibilityStateChangeListener(this);
            updateFeedbackEnabled();
            boolean unused = MiuiVolumeDialogImpl.this.mShowA11yStream = AccessibilityManagerCompat.isAccessibilityVolumeStreamActive(MiuiVolumeDialogImpl.this.mAccessibilityMgr);
        }

        public void destory() {
            MiuiVolumeDialogImpl.this.mDialogView.removeOnAttachStateChangeListener(this.mAttachListener);
            MiuiVolumeDialogImpl.this.mDialogView.setAccessibilityDelegate(null);
            MiuiVolumeDialogImpl.this.mAccessibilityMgr.removeAccessibilityStateChangeListener(this);
        }

        public boolean onRequestSendAccessibilityEvent(ViewGroup host, View child, AccessibilityEvent event) {
            MiuiVolumeDialogImpl.this.rescheduleTimeoutH();
            return super.onRequestSendAccessibilityEvent(host, child, event);
        }

        /* access modifiers changed from: private */
        public void updateFeedbackEnabled() {
            this.mFeedbackEnabled = computeFeedbackEnabled();
        }

        private boolean computeFeedbackEnabled() {
            for (AccessibilityServiceInfo asi : MiuiVolumeDialogImpl.this.mAccessibilityMgr.getEnabledAccessibilityServiceList(-1)) {
                if (asi.feedbackType != 0 && asi.feedbackType != 16) {
                    return true;
                }
            }
            return false;
        }

        public void onAccessibilityStateChanged(boolean enabled) {
            updateFeedbackEnabled();
        }
    }

    private final class CustomDialog extends Dialog {
        public CustomDialog(Context context) {
            super(context, R.style.Theme_MiuiVolumeDialog);
        }

        public boolean dispatchTouchEvent(MotionEvent ev) {
            MiuiVolumeDialogImpl.this.rescheduleTimeoutH();
            return super.dispatchTouchEvent(ev);
        }

        /* access modifiers changed from: protected */
        public void onStop() {
            super.onStop();
            boolean animating = MiuiVolumeDialogImpl.this.mDialogView.isAnimating();
            if (Util.DEBUG) {
                String access$500 = MiuiVolumeDialogImpl.TAG;
                Log.d(access$500, "onStop animating=" + animating);
            }
            if (animating) {
                boolean unused = MiuiVolumeDialogImpl.this.mPendingRecheckAll = true;
            } else {
                MiuiVolumeDialogImpl.this.mHandler.sendEmptyMessage(4);
            }
        }

        public boolean onTouchEvent(MotionEvent event) {
            if (!isShowing() || (event.getAction() != 4 && event.getAction() != 0)) {
                return false;
            }
            MiuiVolumeDialogImpl.this.dismissH(1);
            return true;
        }

        public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
            event.setClassName(getClass().getSuperclass().getName());
            event.setPackageName(MiuiVolumeDialogImpl.this.mContext.getPackageName());
            ViewGroup.LayoutParams params = getWindow().getAttributes();
            event.setFullScreen(params.width == -1 && params.height == -1);
            if (event.getEventType() != 32 || !MiuiVolumeDialogImpl.this.mShowing) {
                return false;
            }
            event.getText().add(MiuiVolumeDialogImpl.this.mContext.getString(R.string.volume_dialog_accessibility_shown_message, new Object[]{MiuiVolumeDialogImpl.this.getStreamLabelH(MiuiVolumeDialogImpl.this.getActiveColumn().ss)}));
            return true;
        }

        public boolean dispatchKeyEvent(KeyEvent event) {
            if (event.getKeyCode() != 4) {
                return super.dispatchKeyEvent(event);
            }
            if (event.getAction() == 1) {
                if (MiuiVolumeDialogImpl.this.mExpanded) {
                    MiuiVolumeDialogImpl.this.updateExpandedH(false, false);
                } else {
                    MiuiVolumeDialogImpl.this.dismissH(7);
                }
            }
            return true;
        }
    }

    private final class H extends Handler {
        public H() {
            super(Looper.getMainLooper());
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    MiuiVolumeDialogImpl.this.showH(msg.arg1);
                    return;
                case 2:
                    MiuiVolumeDialogImpl.this.dismissH(msg.arg1);
                    return;
                case 3:
                    MiuiVolumeDialogImpl.this.recheckH((VolumeColumn) msg.obj);
                    return;
                case 4:
                    MiuiVolumeDialogImpl.this.reCheckAllH();
                    return;
                case 5:
                    MiuiVolumeDialogImpl.this.setStreamImportantH(msg.arg1, msg.arg2 != 0);
                    return;
                case 6:
                    MiuiVolumeDialogImpl.this.rescheduleTimeoutH();
                    return;
                case 7:
                    MiuiVolumeDialogImpl.this.onStateChangedH(MiuiVolumeDialogImpl.this.mState);
                    return;
                case 8:
                    MiuiVolumeDialogImpl.this.vibrateH();
                    return;
                case 9:
                    MiuiVolumeDialogImpl.this.unlockRecordType(msg.arg1);
                    return;
                case 10:
                    MiuiVolumeDialogImpl.this.showVolumeDialogH(msg.arg1);
                    return;
                default:
                    return;
            }
        }
    }

    private final class SilenceModeObserver extends ContentObserver {
        private final Uri SILENCE_MODE = Settings.Global.getUriFor("zen_mode");
        private WeakReference<Toast> mLastToast = new WeakReference<>(null);
        private SharedPreferences mSharedPreferences;
        private int mSilenceMode;

        SilenceModeObserver() {
            super(new Handler(Looper.getMainLooper()));
        }

        public void init() {
            this.mSilenceMode = MiuiSettings.SilenceMode.getZenMode(MiuiVolumeDialogImpl.this.mContext);
            this.mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(MiuiVolumeDialogImpl.this.mContext);
        }

        public void register() {
            MiuiVolumeDialogImpl.this.mContext.getContentResolver().registerContentObserver(this.SILENCE_MODE, false, this, -1);
        }

        public void unregister() {
            MiuiVolumeDialogImpl.this.mContext.getContentResolver().unregisterContentObserver(this);
        }

        public void onChange(boolean selfChange, Uri uri) {
            int toastRes;
            int silentRes;
            super.onChange(selfChange, uri);
            int mode = MiuiSettings.SilenceMode.getZenMode(MiuiVolumeDialogImpl.this.mContext);
            int oldMode = this.mSilenceMode;
            this.mSilenceMode = mode;
            MiuiVolumeDialogImpl.this.mDialogView.setSilenceMode(mode, MiuiVolumeDialogImpl.this.mShowing);
            if (oldMode != mode) {
                if (!((DeviceProvisionedController) Dependency.get(DeviceProvisionedController.class)).isDeviceProvisioned() || mode != 4 || this.mSharedPreferences.getBoolean("volume_guide_dialog_already_show", false)) {
                    boolean muteMediaAtSilent = Settings.System.getIntForUser(MiuiVolumeDialogImpl.this.mContext.getContentResolver(), "mute_music_at_silent", 0, -3) == 0;
                    if (mode == 0) {
                        if (muteMediaAtSilent) {
                            silentRes = R.string.miui_toast_zen_standard_to_off;
                        } else {
                            silentRes = R.string.miui_toast_zen_standard_to_off_when_shield_media;
                        }
                        if (oldMode == 1) {
                            toastRes = R.string.miui_toast_zen_dnd_to_off;
                        } else {
                            toastRes = silentRes;
                        }
                    } else if (mode == 1) {
                        toastRes = R.string.miui_toast_zen_to_dnd;
                    } else if (muteMediaAtSilent) {
                        toastRes = R.string.miui_toast_zen_to_standard;
                    } else {
                        toastRes = R.string.miui_toast_zen_to_standard_when_shield_media;
                    }
                    if (this.mLastToast.get() != null) {
                        ((Toast) this.mLastToast.get()).cancel();
                    }
                    this.mLastToast = new WeakReference<>(Util.showSystemOverlayToast(MiuiVolumeDialogImpl.this.mContext, toastRes, 0));
                } else {
                    this.mSharedPreferences.edit().putBoolean("volume_guide_dialog_already_show", true).apply();
                    showGuideDialog();
                }
            }
        }

        private void showGuideDialog() {
            AlertDialog alertDialog = new AlertDialog.Builder(MiuiVolumeDialogImpl.this.mContext, R.style.Theme_MiuiVolumeDialog_Alert).setMessage(MiuiVolumeDialogImpl.this.mContext.getResources().getString(R.string.miui_guide_dialog_message)).setPositiveButton(MiuiVolumeDialogImpl.this.mContext.getResources().getString(R.string.miui_guide_dialog_button_positive_text), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            }).setNegativeButton(MiuiVolumeDialogImpl.this.mContext.getResources().getString(R.string.miui_guide_dialog_button_negative_text), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    Log.d(MiuiVolumeDialogImpl.TAG, "showGuideDialog go to set.");
                    ComponentName component = ComponentName.unflattenFromString("com.android.settings/com.android.settings.Settings$MiuiSilentModeAcivity");
                    if (component != null) {
                        Intent intent = new Intent("android.intent.action.MAIN");
                        intent.setComponent(component);
                        intent.setFlags(335544320);
                        ((ActivityStarter) Dependency.get(ActivityStarter.class)).postStartActivityDismissingKeyguard(intent, 0);
                        dialog.dismiss();
                    }
                }
            }).create();
            Window window = alertDialog.getWindow();
            window.addFlags(787456);
            window.clearFlags(8388608);
            window.setType(2020);
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.show();
        }
    }

    private static class VolumeColumn {
        /* access modifiers changed from: private */
        public ObjectAnimator anim;
        /* access modifiers changed from: private */
        public int animTargetProgress;
        /* access modifiers changed from: private */
        public int cachedIconRes;
        /* access modifiers changed from: private */
        public ColorStateList cachedIconTint;
        /* access modifiers changed from: private */
        public ColorStateList cachedSliderTint;
        /* access modifiers changed from: private */
        public ImageView icon;
        /* access modifiers changed from: private */
        public int iconMuteRes;
        /* access modifiers changed from: private */
        public int iconRes;
        /* access modifiers changed from: private */
        public int iconState;
        /* access modifiers changed from: private */
        public boolean important;
        /* access modifiers changed from: private */
        public int initIconMuteRes;
        /* access modifiers changed from: private */
        public int initIconRes;
        /* access modifiers changed from: private */
        public int lastAudibleLevel;
        /* access modifiers changed from: private */
        public int requestedLevel;
        /* access modifiers changed from: private */
        public SeekBar slider;
        /* access modifiers changed from: private */
        public VolumeDialogController.StreamState ss;
        /* access modifiers changed from: private */
        public int stream;
        /* access modifiers changed from: private */
        public boolean tracking;
        /* access modifiers changed from: private */
        public long userAttempt;
        /* access modifiers changed from: private */
        public View view;

        private VolumeColumn() {
            this.requestedLevel = -1;
            this.lastAudibleLevel = 1;
        }
    }

    private static class VolumeColumns {
        private ViewGroup mColumnsCollapsed;
        private ViewGroup mColumnsExpanded;
        private boolean mExpanded;

        VolumeColumns(ViewGroup columnsCollapsed, ViewGroup columnsExpanded) {
            this.mColumnsCollapsed = columnsCollapsed;
            this.mColumnsExpanded = columnsExpanded;
        }

        public void updateExpandedH(boolean expanded) {
            this.mExpanded = expanded;
            Util.reparentChildren(this.mExpanded ? this.mColumnsCollapsed : this.mColumnsExpanded, this.mExpanded ? this.mColumnsExpanded : this.mColumnsCollapsed);
        }

        public ViewGroup getCurrentParent() {
            return this.mExpanded ? this.mColumnsExpanded : this.mColumnsCollapsed;
        }

        public void addView(View child) {
            getCurrentParent().addView(child);
        }

        public void addView(View child, int index) {
            getCurrentParent().addView(child, index);
        }

        public void removeView(View child) {
            getCurrentParent().removeView(child);
        }
    }

    private final class VolumeSeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {
        private final VolumeColumn mColumn;

        private VolumeSeekBarChangeListener(VolumeColumn column) {
            this.mColumn = column;
        }

        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (this.mColumn.ss != null) {
                if (Util.DEBUG) {
                    String access$500 = MiuiVolumeDialogImpl.TAG;
                    Log.d(access$500, AudioSystem.streamToString(this.mColumn.stream) + " onProgressChanged " + progress + " fromUser=" + fromUser);
                }
                ColorStateList iconTint = ((double) (((float) progress) / ((float) seekBar.getMax()))) < 0.1d ? MiuiVolumeDialogImpl.this.mIconTintDark : null;
                if (this.mColumn.cachedIconTint != iconTint) {
                    ColorStateList unused = this.mColumn.cachedIconTint = iconTint;
                    this.mColumn.icon.setImageTintList(iconTint);
                }
                if (fromUser) {
                    if (this.mColumn.ss.levelMin > 0) {
                        int minProgress = this.mColumn.ss.levelMin * 100;
                        if (progress < minProgress) {
                            seekBar.setProgress(minProgress);
                            progress = minProgress;
                        }
                    }
                    int userLevel = MiuiVolumeDialogImpl.getImpliedLevel(seekBar, progress);
                    if (this.mColumn.ss.level != userLevel || (this.mColumn.ss.muted && userLevel > 0)) {
                        long unused2 = this.mColumn.userAttempt = SystemClock.uptimeMillis();
                        if (this.mColumn.requestedLevel != userLevel) {
                            MiuiVolumeDialogImpl.this.mController.setStreamVolume(this.mColumn.stream, userLevel);
                            int unused3 = this.mColumn.requestedLevel = userLevel;
                            Events.writeEvent(MiuiVolumeDialogImpl.this.mContext, 9, Integer.valueOf(this.mColumn.stream), Integer.valueOf(userLevel));
                        }
                    }
                }
            }
        }

        public void onStartTrackingTouch(SeekBar seekBar) {
            if (Util.DEBUG) {
                String access$500 = MiuiVolumeDialogImpl.TAG;
                Log.d(access$500, "onStartTrackingTouch " + this.mColumn.stream);
            }
            boolean unused = this.mColumn.tracking = true;
        }

        public void onStopTrackingTouch(SeekBar seekBar) {
            if (Util.DEBUG) {
                String access$500 = MiuiVolumeDialogImpl.TAG;
                Log.d(access$500, "onStopTrackingTouch " + this.mColumn.stream);
            }
            boolean unused = this.mColumn.tracking = false;
            long unused2 = this.mColumn.userAttempt = SystemClock.uptimeMillis();
            int userLevel = MiuiVolumeDialogImpl.getImpliedLevel(seekBar, seekBar.getProgress());
            Events.writeEvent(MiuiVolumeDialogImpl.this.mContext, 16, Integer.valueOf(this.mColumn.stream), Integer.valueOf(userLevel));
            if (this.mColumn.ss.level != userLevel) {
                MiuiVolumeDialogImpl.this.mHandler.sendMessageDelayed(MiuiVolumeDialogImpl.this.mHandler.obtainMessage(3, this.mColumn), 1000);
            }
        }
    }

    public MiuiVolumeDialogImpl(Context context) {
        this.mContext = context;
        this.mController = (VolumeDialogController) Dependency.get(VolumeDialogController.class);
        this.mKeyguard = (KeyguardManager) this.mContext.getSystemService("keyguard");
        this.mKeyguardMonitor = (KeyguardMonitor) Dependency.get(KeyguardMonitor.class);
        this.mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
        this.mAccessibilityMgr = (AccessibilityManager) this.mContext.getSystemService("accessibility");
    }

    public void init(int windowType, VolumeDialog.Callback callback) {
        this.mWindowType = windowType;
        initDialog();
        this.mAccessibility.init();
        this.mController.addCallback(this.mControllerCallbackH, this.mHandler);
        this.mController.getState();
        ((TunerService) Dependency.get(TunerService.class)).addTunable(this, "sysui_show_full_zen");
        this.mSilenceModeObserver.init();
        this.mSilenceModeObserver.register();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.media.RINGER_MODE_CHANGED");
        this.mContext.registerReceiverAsUser(this.mRingerModeChangedReceiver, UserHandle.ALL, filter, null, null);
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).addCallback(this);
    }

    public void destroy() {
        this.mAccessibility.destory();
        this.mController.removeCallback(this.mControllerCallbackH);
        ((TunerService) Dependency.get(TunerService.class)).removeTunable(this);
        this.mSilenceModeObserver.unregister();
        this.mContext.unregisterReceiver(this.mRingerModeChangedReceiver);
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).removeCallback(this);
    }

    private void initDialog() {
        Log.d(TAG, "initDialog");
        this.mDialog = new CustomDialog(this.mContext);
        this.mWindow = this.mDialog.getWindow();
        this.mWindow.requestFeature(1);
        this.mConfigurableTexts = new ConfigurableTexts(this.mContext);
        this.mHovering = false;
        this.mShowing = false;
        this.mPendingStateChanged = false;
        this.mDialog.setCanceledOnTouchOutside(true);
        this.mDialog.setContentView(R.layout.miui_volume_dialog);
        setupWindowAttributes();
        ((ViewGroup) this.mDialog.findViewById(16908290)).setClipChildren(false);
        this.mDialogView = (MiuiVolumeDialogView) this.mDialog.findViewById(R.id.volume_dialog);
        this.mDialogView.setOnHoverListener(new View.OnHoverListener() {
            public boolean onHover(View v, MotionEvent event) {
                int action = event.getActionMasked();
                boolean unused = MiuiVolumeDialogImpl.this.mHovering = action == 9 || action == 7;
                MiuiVolumeDialogImpl.this.rescheduleTimeoutH();
                return true;
            }
        });
        this.mDialogContentView = (ViewGroup) this.mDialogView.findViewById(R.id.volume_dialog_content);
        this.mDialogColumns = new VolumeColumns((ViewGroup) this.mDialogContentView.findViewById(R.id.volume_dialog_column_collapsed), (ViewGroup) this.mDialogContentView.findViewById(R.id.volume_dialog_columns));
        this.mTempColumnContainer = (FrameLayout) this.mDialogView.findViewById(R.id.volume_dialog_column_temp);
        this.mExpanded = false;
        this.mExpandButton = (ImageView) this.mDialogView.findViewById(R.id.volume_expand_button);
        this.mExpandButton.setOnClickListener(this.mClickExpand);
        this.mDialogView.setMotionCallback(new MiuiVolumeDialogMotion.Callback() {
            public void onAnimatingChanged(boolean animating) {
                if (!animating) {
                    if (MiuiVolumeDialogImpl.this.mPendingStateChanged) {
                        MiuiVolumeDialogImpl.this.mHandler.sendEmptyMessage(7);
                        boolean unused = MiuiVolumeDialogImpl.this.mPendingStateChanged = false;
                    }
                    if (MiuiVolumeDialogImpl.this.mPendingRecheckAll) {
                        MiuiVolumeDialogImpl.this.mHandler.sendEmptyMessage(4);
                        boolean unused2 = MiuiVolumeDialogImpl.this.mPendingRecheckAll = false;
                    }
                }
            }

            public void onShow() {
                String access$500 = MiuiVolumeDialogImpl.TAG;
                Logger.i(access$500, "onShow isShowing:" + MiuiVolumeDialogImpl.this.mDialog.isShowing());
                if (!MiuiVolumeDialogImpl.this.mDialog.isShowing()) {
                    MiuiVolumeDialogImpl.this.mDialog.show();
                }
            }

            public void onDismiss() {
                String access$500 = MiuiVolumeDialogImpl.TAG;
                Logger.i(access$500, "onDismiss isShowing:" + MiuiVolumeDialogImpl.this.mDialog.isShowing());
                if (MiuiVolumeDialogImpl.this.mDialog.isShowing()) {
                    MiuiVolumeDialogImpl.this.mDialog.dismiss();
                }
            }
        });
        if (this.mColumns.isEmpty()) {
            addColumn(3, R.drawable.ic_miui_volume_media, R.drawable.ic_miui_volume_media_mute, true);
            if (!AudioSystemCompat.isSingleVolume(this.mContext)) {
                addColumn(10, R.drawable.ic_miui_volume_accessibility, R.drawable.ic_miui_volume_accessibility_mute, true);
                addColumn(2, R.drawable.ic_miui_volume_ringer, R.drawable.ic_miui_volume_ringer_mute, true);
                addColumn(4, R.drawable.ic_miui_volume_alarm, R.drawable.ic_miui_volume_alarm_mute, false);
                addColumn(0, R.drawable.ic_miui_volume_voice, R.drawable.ic_miui_volume_voice_mute, false);
                addColumn(6, R.drawable.ic_miui_volume_voice, R.drawable.ic_miui_volume_voice_mute, false);
                if (this.mController.getVoiceAssistStreamType() > 0) {
                    addColumn(this.mController.getVoiceAssistStreamType(), R.drawable.ic_miui_volume_assist, R.drawable.ic_miui_volume_assist_mute, false);
                }
            }
        } else {
            addExistingColumns();
        }
        addTempColumn(3, R.drawable.ic_miui_volume_media, R.drawable.ic_miui_volume_media_mute, true);
        updateExpandedH(false, false, true);
        this.mMutedColorList = this.mContext.getResources().getColorStateList(R.color.miui_volume_disabled_color);
        this.mIconTintDark = this.mContext.getResources().getColorStateList(R.color.miui_volume_tint_dark);
    }

    private void setupWindowAttributes() {
        this.mWindow.setBackgroundDrawable(new ColorDrawable(0));
        this.mWindow.addFlags(787496);
        this.mWindow.clearFlags(8388608);
        this.mWindow.addPrivateFlags(64);
        Resources resources = this.mContext.getResources();
        WindowManager.LayoutParams lp = this.mWindow.getAttributes();
        lp.type = this.mWindowType;
        lp.format = -3;
        lp.setTitle(MiuiVolumeDialogImpl.class.getSimpleName());
        lp.windowAnimations = -1;
        lp.gravity = 48;
        lp.width = -1;
        lp.height = -1;
        updateDialogWindowH(false);
        this.mWindow.setAttributes(lp);
        this.mWindow.setSoftInputMode(48);
    }

    public void onTuningChanged(String key, String newValue) {
    }

    public void setStreamImportant(int stream, boolean important) {
        this.mHandler.obtainMessage(5, stream, important).sendToTarget();
    }

    public void setAutomute(boolean automute) {
        if (this.mAutomute != automute) {
            this.mAutomute = automute;
            this.mHandler.sendEmptyMessage(4);
        }
    }

    public void setSilentMode(boolean silentMode) {
        if (this.mSilentMode != silentMode) {
            this.mSilentMode = silentMode;
            this.mHandler.sendEmptyMessage(4);
        }
    }

    private void addColumn(int stream, int iconRes, int iconMuteRes, boolean important) {
        addColumn(stream, iconRes, iconMuteRes, important, false);
    }

    private void addColumn(int stream, int iconRes, int iconMuteRes, boolean important, boolean dynamic) {
        VolumeColumn column = new VolumeColumn();
        initColumn(column, stream, iconRes, iconMuteRes, important);
        if (this.mShowA11yStream && dynamic) {
            int size = this.mColumns.size();
            int columnSize = size;
            if (size > 1) {
                int childCount = this.mDialogColumns.getCurrentParent().getChildCount();
                int viewSize = childCount;
                if (childCount > 1) {
                    this.mDialogColumns.addView(column.view, viewSize - 2);
                    this.mColumns.add(columnSize - 2, column);
                    return;
                }
            }
        }
        this.mDialogColumns.addView(column.view);
        this.mColumns.add(column);
    }

    private void addTempColumn(int stream, int iconRes, int iconMuteRes, boolean important) {
        VolumeColumn column = new VolumeColumn();
        initColumn(column, stream, iconRes, iconMuteRes, important);
        if (this.mTempColumnContainer.getChildCount() != 0) {
            this.mTempColumnContainer.removeAllViews();
        }
        this.mTempColumnContainer.addView(column.view);
        this.mTempColumn = column;
    }

    private void addExistingColumns() {
        int N = this.mColumns.size();
        for (int i = 0; i < N; i++) {
            VolumeColumn column = this.mColumns.get(i);
            initColumn(column, column.stream, column.initIconRes, column.initIconMuteRes, column.important, true);
            this.mDialogColumns.addView(column.view);
        }
    }

    /* access modifiers changed from: private */
    public VolumeColumn getActiveColumn() {
        for (VolumeColumn column : this.mColumns) {
            if (column.stream == this.mActiveStream) {
                return column;
            }
        }
        return this.mColumns.get(0);
    }

    private VolumeColumn findColumn(int stream) {
        for (VolumeColumn column : this.mColumns) {
            if (column.stream == stream) {
                return column;
            }
        }
        return null;
    }

    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        writer.println(MiuiVolumeDialogImpl.class.getSimpleName() + " state:");
        writer.print("  mShowing: ");
        writer.println(this.mShowing);
        writer.print("  mExpanded: ");
        writer.println(this.mExpanded);
        writer.print("  mActiveStream: ");
        writer.println(this.mActiveStream);
        writer.print("  mDynamic: ");
        writer.println(this.mDynamic);
        writer.print("  mAutomute: ");
        writer.println(this.mAutomute);
        writer.print("  mSilentMode: ");
        writer.println(this.mSilentMode);
        writer.print("  mAccessibility.mFeedbackEnabled: ");
        writer.println(this.mAccessibility.mFeedbackEnabled);
    }

    /* access modifiers changed from: private */
    public static int getImpliedLevel(SeekBar seekBar, int progress) {
        int m = seekBar.getMax();
        int n = (m / 100) - 1;
        if (progress == 0) {
            return 0;
        }
        return progress == m ? m / 100 : 1 + ((int) ((((float) progress) / ((float) m)) * ((float) n)));
    }

    @SuppressLint({"InflateParams"})
    private void initColumn(VolumeColumn column, int stream, int iconRes, int iconMuteRes, boolean important) {
        initColumn(column, stream, iconRes, iconMuteRes, important, false);
    }

    @SuppressLint({"InflateParams"})
    private void initColumn(final VolumeColumn column, int stream, int iconRes, int iconMuteRes, boolean important, boolean resetCacheIconRes) {
        int unused = column.stream = stream;
        int unused2 = column.initIconRes = iconRes;
        int unused3 = column.initIconMuteRes = iconMuteRes;
        boolean unused4 = column.important = important;
        View unused5 = column.view = LayoutInflater.from(this.mContext).inflate(R.layout.miui_volume_dialog_column, this.mDialogColumns.getCurrentParent(), false);
        column.view.setId(column.stream);
        column.view.setTag(column);
        SeekBar unused6 = column.slider = (SeekBar) column.view.findViewById(R.id.volume_column_slider);
        column.slider.setOnSeekBarChangeListener(new VolumeSeekBarChangeListener(column));
        ObjectAnimator unused7 = column.anim = null;
        column.view.setOnTouchListener(new View.OnTouchListener() {
            private boolean mDragging;
            private final Rect mSliderHitRect = new Rect();

            @SuppressLint({"ClickableViewAccessibility"})
            public boolean onTouch(View v, MotionEvent event) {
                column.slider.getHitRect(this.mSliderHitRect);
                if (!this.mDragging && event.getActionMasked() == 0 && event.getY() < ((float) this.mSliderHitRect.top)) {
                    this.mDragging = true;
                }
                if (!this.mDragging) {
                    return false;
                }
                event.offsetLocation((float) (-this.mSliderHitRect.left), (float) (-this.mSliderHitRect.top));
                column.slider.dispatchTouchEvent(event);
                if (event.getActionMasked() == 1 || event.getActionMasked() == 3) {
                    this.mDragging = false;
                }
                return true;
            }
        });
        ImageView unused8 = column.icon = (ImageView) column.view.findViewById(R.id.volume_column_icon);
        column.icon.setImageResource(iconRes);
        if (column.stream == 10) {
            column.icon.setImportantForAccessibility(2);
        }
        column.slider.setProgressTintList(column.cachedSliderTint);
        if (resetCacheIconRes) {
            int unused9 = column.cachedIconRes = 0;
            ColorStateList unused10 = column.cachedIconTint = null;
            ColorStateList unused11 = column.cachedSliderTint = null;
        }
    }

    /* access modifiers changed from: private */
    public void showH(int reason) {
        if (!this.mKeyguard.isKeyguardLocked() || !this.mKeyguardMonitor.needSkipVolumeDialog()) {
            ScenarioTrackUtil.beginScenario(ScenarioConstants.SCENARIO_VOLUME_DIALOG_SHOW);
            if (Util.DEBUG) {
                String str = TAG;
                Log.d(str, "showH r=" + Events.DISMISS_REASONS[reason] + " mShowing:" + this.mShowing + " mNeedReInit:" + this.mNeedReInit);
            }
            this.mHandler.removeMessages(1);
            this.mHandler.removeMessages(2);
            rescheduleTimeoutH();
            if (!this.mShowing && !this.mDialogView.isAnimating()) {
                this.mHandler.removeMessages(10);
                if (this.mNeedReInit) {
                    this.mNeedReInit = false;
                    reInit();
                    this.mHandler.obtainMessage(10, reason, 0).sendToTarget();
                } else {
                    showVolumeDialogH(reason);
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void showVolumeDialogH(int reason) {
        this.mShowing = true;
        String str = TAG;
        Logger.i(str, "showVolumeDialogH reason:" + reason + " mActiveStream:" + this.mActiveStream);
        Events.writeEvent(this.mContext, 0, Integer.valueOf(reason), Boolean.valueOf(this.mKeyguard.isKeyguardLocked()));
        this.mController.notifyVisible(true);
        this.mDialogView.showH();
    }

    private void reInit() {
        String str = TAG;
        Logger.i(str, "reInit mActiveStream:" + this.mActiveStream);
        this.mAccessibility.destory();
        initDialog();
        this.mHandler.sendEmptyMessage(7);
        this.mAccessibility.init();
        reCheckAllH();
        this.mDialogView.updateFooterVisibility((this.mActiveStream == 0 || this.mActiveStream == 6) ? false : true);
    }

    /* access modifiers changed from: protected */
    public void rescheduleTimeoutH() {
        this.mHandler.removeMessages(2);
        int timeout = computeTimeoutH();
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(2, 3, 0), (long) timeout);
        String str = TAG;
        Logger.i(str, "rescheduleTimeout " + timeout + " mActiveStream:" + this.mActiveStream);
        this.mController.userActivity();
    }

    private int computeTimeoutH() {
        if (this.mAccessibility.mFeedbackEnabled) {
            return 20000;
        }
        if (this.mHovering) {
            return 16000;
        }
        if (this.mSafetyWarning != null || this.mExpanded) {
            return 5000;
        }
        if (this.mActiveStream == 3) {
            return 1500;
        }
        return 3000;
    }

    /* access modifiers changed from: private */
    public void vibrateH() {
        ((Vibrator) this.mContext.getSystemService("vibrator")).vibrate(300);
    }

    /* access modifiers changed from: protected */
    public void dismissH(int reason) {
        if (!this.mDialogView.isAnimating() || reason == 8) {
            if (Util.DEBUG) {
                String str = TAG;
                Log.i(str, "dismissH mShowing:" + this.mShowing);
            }
            this.mHandler.removeMessages(2);
            this.mHandler.removeMessages(1);
            if (this.mShowing) {
                ScenarioTrackUtil.beginScenario(ScenarioConstants.SCENARIO_VOLUME_DIALOG_HIDE);
                this.mShowing = false;
                String str2 = TAG;
                Logger.i(str2, "dismissH reason:" + reason + " mActiveStream:" + this.mActiveStream);
                if (this.mAccessibilityMgr.isEnabled()) {
                    AccessibilityEvent event = AccessibilityEvent.obtain(32);
                    event.setPackageName(this.mContext.getPackageName());
                    event.setClassName(CustomDialog.class.getSuperclass().getName());
                    event.getText().add(this.mContext.getString(R.string.volume_dialog_accessibility_dismissed_message));
                    this.mAccessibilityMgr.sendAccessibilityEvent(event);
                }
                Events.writeEvent(this.mContext, 1, Integer.valueOf(reason));
                this.mController.notifyVisible(false);
                this.mDialogView.dismissH(new Runnable() {
                    public void run() {
                        MiuiVolumeDialogImpl.this.updateExpandedH(false, true);
                    }
                });
                synchronized (this.mSafetyWarningLock) {
                    if (this.mSafetyWarning != null) {
                        if (Util.DEBUG) {
                            Log.d(TAG, "SafetyWarning dismissed");
                        }
                        this.mSafetyWarning.dismiss();
                    }
                }
                return;
            }
            return;
        }
        if (Util.DEBUG) {
            String str3 = TAG;
            Log.i(str3, "dismissH reason:" + reason + " mShowing:" + this.mShowing);
        }
    }

    /* access modifiers changed from: private */
    public void updateExpandedH(boolean expanded, boolean dismissing) {
        updateExpandedH(expanded, dismissing, false);
    }

    private void updateExpandedH(boolean expanded, boolean dismissing, boolean forceUpdate) {
        if (this.mExpanded != expanded || forceUpdate) {
            this.mExpanded = expanded;
            if (Util.DEBUG) {
                String str = TAG;
                Log.d(str, "updateExpandedH " + expanded);
            }
            this.mDialogView.updateExpanded(expanded, !dismissing);
            this.mDialogColumns.updateExpandedH(this.mExpanded);
            updateColumnH(getActiveColumn());
            updateDialogWindowH(dismissing);
            rescheduleTimeoutH();
        }
    }

    private void updateDialogWindowH(boolean dismissing) {
        float dimCollapsed = this.mContext.getResources().getFraction(R.fraction.miui_volume_dim_behind_collapsed, 1, 1);
        float dimExpanded = this.mContext.getResources().getFraction(R.fraction.miui_volume_dim_behind_expanded, 1, 1);
        if (this.mExpanded || dimCollapsed > 0.0f) {
            this.mWindow.addFlags(2);
        } else {
            this.mWindow.clearFlags(2);
        }
        if (this.mExpanded) {
            this.mWindow.clearFlags(8);
        } else {
            this.mWindow.addFlags(8);
        }
        this.mWindow.setDimAmount((!this.mExpanded || dismissing) ? dimCollapsed : dimExpanded);
    }

    private boolean shouldBeVisibleH(VolumeColumn column, boolean isActive) {
        boolean z = true;
        if (column.stream == 10) {
            if (this.mExpanded || !this.mShowA11yStream) {
                z = false;
            }
            return z;
        } else if (((this.mController.getVoiceAssistStreamType() > 0 && column.stream == this.mController.getVoiceAssistStreamType()) || this.mDynamic.get(column.stream)) && this.mExpanded) {
            return false;
        } else {
            if ((!this.mExpanded || column.view.getVisibility() != 0) && ((!this.mExpanded || (!column.important && !isActive)) && (this.mExpanded || !isActive))) {
                z = false;
            }
            return z;
        }
    }

    /* access modifiers changed from: private */
    public void updateColumnH(VolumeColumn activeColumn) {
        if (Util.DEBUG) {
            Log.d(TAG, "updateColumnH");
        }
        if (!this.mShowing) {
            trimObsoleteH();
        }
        updateTempColumn();
        Iterator<VolumeColumn> it = this.mColumns.iterator();
        while (it.hasNext()) {
            VolumeColumn column = it.next();
            boolean shouldBeVisible = shouldBeVisibleH(column, column == activeColumn);
            if (this.mExpanded || !column.important) {
                Util.setVisOrGone(column.view, shouldBeVisible);
            } else {
                Util.setVisOrInvis(column.view, shouldBeVisible);
            }
            updateColumnsSizeH(column.slider);
        }
    }

    private void updateTempColumn() {
        boolean isMusicActive = this.mAudioManager.isMusicActive();
        int tempStream = 3;
        int tempIconRes = R.drawable.ic_miui_volume_media;
        int tempIconMuteRes = R.drawable.ic_miui_volume_media_mute;
        boolean tempImportant = true;
        boolean streamVisible = false;
        boolean z = false;
        if (this.mActiveStream == 0 && isMusicActive) {
            streamVisible = true;
        } else if (this.mActiveStream == 3 && AudioSystem.isStreamActive(0, 0)) {
            Log.d(TAG, "voice_call is active too");
            streamVisible = true;
            tempStream = 0;
            tempIconRes = R.drawable.ic_miui_volume_voice;
            tempIconMuteRes = R.drawable.ic_miui_volume_voice_mute;
            tempImportant = false;
        } else if (this.mActiveStream == this.mController.getVoiceAssistStreamType() && this.mActiveStream > 0) {
            streamVisible = true;
        }
        int unused = this.mTempColumn.stream = tempStream;
        int unused2 = this.mTempColumn.initIconRes = tempIconRes;
        int unused3 = this.mTempColumn.initIconMuteRes = tempIconMuteRes;
        boolean unused4 = this.mTempColumn.important = tempImportant;
        if (!this.mExpanded && (streamVisible || this.mShowA11yStream)) {
            z = true;
        }
        boolean shouldTempBeVisible = z;
        if (Util.DEBUG) {
            String str = TAG;
            Log.d(str, "shouldTempBeVisible mExpanded:" + this.mExpanded + " mActiveStream:" + this.mActiveStream + " mShowA11yStream:" + this.mShowA11yStream + " isMusicActive:" + isMusicActive + " shouldTempBeVisible:" + shouldTempBeVisible + " streamVisible:" + streamVisible);
        }
        Util.setVisOrGone(this.mTempColumnContainer, shouldTempBeVisible);
    }

    private void updateColumnsSizeH(View slider) {
        int i;
        int i2;
        int i3;
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) slider.getLayoutParams();
        Resources resources = this.mContext.getResources();
        if (this.mExpanded) {
            i = R.dimen.miui_volume_column_width_expanded;
        } else {
            i = R.dimen.miui_volume_column_width;
        }
        params.width = resources.getDimensionPixelSize(i);
        Resources resources2 = this.mContext.getResources();
        if (this.mExpanded) {
            i2 = R.dimen.miui_volume_column_height_expanded;
        } else {
            i2 = R.dimen.miui_volume_column_height;
        }
        params.height = resources2.getDimensionPixelSize(i2);
        Resources resources3 = this.mContext.getResources();
        if (!this.mExpanded) {
            i3 = R.dimen.miui_volume_column_margin_horizontal;
        } else if (this.mActiveStream == 0) {
            i3 = R.dimen.miui_volume_column_margin_horizontal_expanded_voice;
        } else {
            i3 = R.dimen.miui_volume_column_margin_horizontal_expanded;
        }
        int dimensionPixelSize = resources3.getDimensionPixelSize(i3);
        params.rightMargin = dimensionPixelSize;
        params.leftMargin = dimensionPixelSize;
    }

    private void trimObsoleteH() {
        if (Util.DEBUG) {
            Log.d(TAG, "trimObsoleteH");
        }
        for (int i = this.mColumns.size() - 1; i >= 0; i--) {
            VolumeColumn column = this.mColumns.get(i);
            if (column.ss != null && column.ss.dynamic && !this.mDynamic.get(column.stream)) {
                this.mColumns.remove(i);
                this.mDialogColumns.removeView(column.view);
            }
        }
    }

    /* access modifiers changed from: private */
    public void onStateChangedH(VolumeDialogController.State state) {
        boolean animating = this.mDialogView.isAnimating();
        if (Util.DEBUG) {
            Log.d(TAG, "onStateChangedH animating=" + animating + " activeStream:" + state.activeStream);
        }
        this.mState = state;
        boolean z = true;
        if (animating) {
            this.mPendingStateChanged = true;
            return;
        }
        this.mDynamic.clear();
        for (int i = 0; i < state.states.size(); i++) {
            int stream = state.states.keyAt(i);
            if (state.states.valueAt(i).dynamic) {
                this.mDynamic.put(stream, true);
                if (findColumn(stream) == null) {
                    addColumn(stream, R.drawable.ic_miui_volume_media, R.drawable.ic_miui_volume_media_mute, true, true);
                }
            }
        }
        if (Util.DEBUG != 0) {
            Log.d(TAG, "onStateChangedH mActiveStream:" + this.mActiveStream + " state.activeStream:" + state.activeStream);
        }
        if (this.mActiveStream != state.activeStream) {
            this.mActiveStream = state.activeStream;
            updateColumnH(getActiveColumn());
            rescheduleTimeoutH();
            MiuiVolumeDialogView miuiVolumeDialogView = this.mDialogView;
            if (this.mActiveStream == 0 || this.mActiveStream == 6) {
                z = false;
            }
            miuiVolumeDialogView.updateFooterVisibility(z);
        }
        for (VolumeColumn column : this.mColumns) {
            updateVolumeColumnH(column);
        }
        updateVolumeColumnH(this.mTempColumn);
    }

    private void updateVolumeColumnH(VolumeColumn column) {
        int iconRes;
        int vlevel;
        if (Util.DEBUG) {
            String str = TAG;
            Log.d(str, "updateVolumeColumnH s=" + column.stream);
        }
        if (this.mState != null) {
            VolumeDialogController.StreamState ss = this.mState.states.get(column.stream);
            if (ss != null) {
                VolumeDialogController.StreamState unused = column.ss = ss;
                if (ss.level > 0) {
                    int unused2 = column.lastAudibleLevel = ss.level;
                }
                if (ss.level == column.requestedLevel) {
                    int unused3 = column.requestedLevel = -1;
                }
                boolean streamMuted = ss.muted;
                int i = 2;
                if (column.stream == 2 && !this.mDialogView.isOffMode()) {
                    streamMuted = true;
                }
                int max = ss.levelMax * 100;
                if (max != column.slider.getMax()) {
                    column.slider.setMax(max);
                }
                if (Util.DEBUG) {
                    String str2 = TAG;
                    Log.d(str2, "updateVolumeColumnH level:" + ss.level + " levelMax:" + ss.levelMax + " mAutomute:" + this.mAutomute + " streamMuted:" + streamMuted + " column.stream:" + column.stream);
                }
                updateColumnIconH(column);
                if (!this.mAutomute || ss.level != 0) {
                    iconRes = streamMuted ? column.iconMuteRes : column.iconRes;
                } else {
                    iconRes = column.iconMuteRes;
                }
                if (iconRes != column.cachedIconRes) {
                    int unused4 = column.cachedIconRes = iconRes;
                    column.icon.setImageResource(iconRes);
                }
                if (iconRes != column.iconMuteRes) {
                    if (iconRes == column.iconRes) {
                        i = 1;
                    } else {
                        i = 0;
                    }
                }
                int unused5 = column.iconState = i;
                if (streamMuted) {
                    vlevel = this.mAudioManager.getLastAudibleStreamVolume(column.stream);
                } else {
                    vlevel = column.ss.level;
                }
                updateVolumeColumnSliderH(column, streamMuted, vlevel);
            }
        }
    }

    private void updateColumnIconH(VolumeColumn column) {
        int unused = column.iconRes = column.initIconRes;
        int unused2 = column.iconMuteRes = column.initIconMuteRes;
        if (column.stream == this.mActiveStream) {
            int device = this.mAudioManager.getDevicesForStream(column.stream);
            if (column.stream == 0 && this.mAudioManager.isSpeakerphoneOn()) {
                int unused3 = column.iconRes = R.drawable.ic_miui_volume_speaker;
                int unused4 = column.iconMuteRes = R.drawable.ic_miui_volume_speaker_mute;
            }
            if ((device & 4) != 0 || (device & 8) != 0) {
                int unused5 = column.iconRes = R.drawable.ic_miui_volume_headset;
                int unused6 = column.iconMuteRes = R.drawable.ic_miui_volume_headset_mute;
            }
        }
    }

    private void updateVolumeColumnSliderH(VolumeColumn column, boolean streamMute, int vlevel) {
        ColorStateList stateList = streamMute ? this.mMutedColorList : null;
        if (Util.DEBUG) {
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("updateVolumeColumnSliderH column.stream:");
            sb.append(column.stream);
            sb.append(" activeStream:");
            sb.append(this.mActiveStream);
            sb.append(" streamMute:");
            sb.append(streamMute);
            sb.append(" vlevel:");
            sb.append(vlevel);
            sb.append(" column.cachedSliderTint != stateList?:");
            sb.append(column.cachedSliderTint != stateList);
            Log.d(str, sb.toString());
        }
        if (column.cachedSliderTint != stateList) {
            ColorStateList unused = column.cachedSliderTint = stateList;
            column.slider.setProgressTintList(stateList);
        }
        column.slider.setContentDescription(getStreamLabelH(column.ss));
        if (!column.tracking) {
            int progress = column.slider.getProgress();
            int level = getImpliedLevel(column.slider, progress);
            boolean columnVisible = column.view.getVisibility() == 0;
            boolean inGracePeriod = SystemClock.uptimeMillis() - column.userAttempt < 1000;
            if (Util.DEBUG) {
                String str2 = TAG;
                Log.d(str2, "updateVolumeColumnSliderH column.stream:" + column.stream + " activeStream:" + this.mActiveStream + " progress:" + progress + " level:" + level + " columnVisible:" + columnVisible + " inGracePeriod:" + inGracePeriod);
            }
            this.mHandler.removeMessages(3, column);
            if (this.mShowing && columnVisible && inGracePeriod) {
                if (Util.DEBUG) {
                    Log.d(TAG, "inGracePeriod");
                }
                this.mHandler.sendMessageAtTime(this.mHandler.obtainMessage(3, column), column.userAttempt + 1000);
            } else if (vlevel != level || !this.mShowing || !columnVisible) {
                int newProgress = vlevel * 100;
                if (progress != newProgress) {
                    if (!this.mShowing || !columnVisible) {
                        if (column.anim != null) {
                            column.anim.cancel();
                        }
                        ProgressBarCompat.setProgress(column.slider, newProgress, true);
                    } else if (column.anim == null || !column.anim.isRunning() || column.animTargetProgress != newProgress) {
                        if (column.anim == null) {
                            ObjectAnimator unused2 = column.anim = ObjectAnimator.ofInt(column.slider, "progress", new int[]{progress, newProgress});
                            column.anim.setInterpolator(new DecelerateInterpolator());
                        } else {
                            column.anim.cancel();
                            column.anim.setIntValues(new int[]{progress, newProgress});
                        }
                        int unused3 = column.animTargetProgress = newProgress;
                        column.anim.setDuration(80);
                        column.anim.start();
                    }
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void reCheckAllH() {
        if (Util.DEBUG) {
            Log.d(TAG, "recheckH ALL");
        }
        trimObsoleteH();
        for (VolumeColumn r : this.mColumns) {
            updateVolumeColumnH(r);
        }
    }

    /* access modifiers changed from: private */
    public void recheckH(VolumeColumn column) {
        if (Util.DEBUG) {
            String str = TAG;
            Log.d(str, "recheckH " + column.stream);
        }
        updateVolumeColumnH(column);
    }

    /* access modifiers changed from: private */
    public void setStreamImportantH(int stream, boolean important) {
        for (VolumeColumn column : this.mColumns) {
            if (column.stream == stream) {
                boolean unused = column.important = important;
                return;
            }
        }
    }

    /* access modifiers changed from: private */
    /* JADX WARNING: Code restructure failed: missing block: B:13:0x0026, code lost:
        reCheckAllH();
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void showSafetyWarningH(int r5) {
        /*
            r4 = this;
            r0 = r5 & 1025(0x401, float:1.436E-42)
            if (r0 != 0) goto L_0x0008
            boolean r0 = r4.mShowing
            if (r0 == 0) goto L_0x0029
        L_0x0008:
            java.lang.Object r0 = r4.mSafetyWarningLock
            monitor-enter(r0)
            com.android.systemui.volume.SafetyWarningDialog r1 = r4.mSafetyWarning     // Catch:{ all -> 0x002d }
            if (r1 == 0) goto L_0x0011
            monitor-exit(r0)     // Catch:{ all -> 0x002d }
            return
        L_0x0011:
            com.android.systemui.miui.volume.MiuiVolumeDialogImpl$6 r1 = new com.android.systemui.miui.volume.MiuiVolumeDialogImpl$6     // Catch:{ all -> 0x002d }
            android.content.Context r2 = r4.mContext     // Catch:{ all -> 0x002d }
            com.android.systemui.plugins.VolumeDialogController r3 = r4.mController     // Catch:{ all -> 0x002d }
            android.media.AudioManager r3 = r3.getAudioManager()     // Catch:{ all -> 0x002d }
            r1.<init>(r2, r3)     // Catch:{ all -> 0x002d }
            r4.mSafetyWarning = r1     // Catch:{ all -> 0x002d }
            com.android.systemui.volume.SafetyWarningDialog r1 = r4.mSafetyWarning     // Catch:{ all -> 0x002d }
            r1.show()     // Catch:{ all -> 0x002d }
            monitor-exit(r0)     // Catch:{ all -> 0x002d }
            r4.reCheckAllH()
        L_0x0029:
            r4.rescheduleTimeoutH()
            return
        L_0x002d:
            r1 = move-exception
            monitor-exit(r0)     // Catch:{ all -> 0x002d }
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.miui.volume.MiuiVolumeDialogImpl.showSafetyWarningH(int):void");
    }

    /* access modifiers changed from: private */
    public String getStreamLabelH(VolumeDialogController.StreamState ss) {
        if (ss.remoteLabel != null) {
            return ss.remoteLabel;
        }
        try {
            return this.mContext.getString(ss.nameRes);
        } catch (Resources.NotFoundException e) {
            String str = TAG;
            Slog.e(str, "Can't find translation for stream " + ss);
            return "";
        }
    }

    private boolean applyNewConfig(Resources res) {
        int configChanges = this.mLastConfiguration.updateFrom(res.getConfiguration());
        if (!(this.mLastDensity != res.getDisplayMetrics().densityDpi) && (-1073741180 & configChanges) == 0) {
            return false;
        }
        this.mLastDensity = res.getDisplayMetrics().densityDpi;
        return true;
    }

    public void onConfigChanged(Configuration newConfig) {
        if (applyNewConfig(this.mContext.getResources())) {
            Log.i(TAG, "onConfigChanged sensitive config changed");
            dismissH(8);
            this.mNeedReInit = true;
            this.mActiveStream = -1;
            this.mConfigurableTexts.update();
            return;
        }
        Log.i(TAG, "onConfigChanged not sensitive.");
    }

    public void onDensityOrFontScaleChanged() {
    }

    /* access modifiers changed from: private */
    public void recordVolumeChanged(int stream, boolean fromKey) {
        if (fromKey) {
            recordCountIfNeed(1);
            return;
        }
        Iterator<VolumeColumn> it = this.mColumns.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            VolumeColumn vc = it.next();
            if (vc.stream == stream && vc.tracking) {
                if (this.mDynamic.get(stream)) {
                    recordCountIfNeed(16);
                    return;
                }
                switch (stream) {
                    case 2:
                        recordCountIfNeed(4);
                        break;
                    case 3:
                        recordCountIfNeed(2);
                        break;
                    case 4:
                        recordCountIfNeed(8);
                        break;
                    default:
                        return;
                }
            }
        }
    }

    private void recordCountIfNeed(int type) {
        String str;
        if ((this.mLockRecordTypes & type) == 0) {
            this.mLockRecordTypes |= type;
            Message msg = Message.obtain(this.mHandler, 9);
            msg.arg1 = type;
            this.mHandler.sendMessageDelayed(msg, 2000);
            String key = null;
            if (type == 4) {
                key = "volume_ring_volume_adjust_by_slide";
            } else if (type == 8) {
                key = "volume_alarm_volume_adjust_by_slide";
            } else if (type != 16) {
                switch (type) {
                    case 1:
                        key = "volume_adjust_by_key";
                        break;
                    case 2:
                        if (this.mExpanded) {
                            str = "volume_media_volume_adjust_by_slide";
                        } else {
                            str = "volume_adjust_by_slide";
                        }
                        key = str;
                        break;
                }
            } else {
                key = "volume_remote_volume_adjust_by_slide";
            }
            if (key != null) {
                AnalyticsWrapper.recordCountEventAnonymous("systemui_volume_dialog", key);
            }
        }
    }

    /* access modifiers changed from: private */
    public void unlockRecordType(int unlockType) {
        this.mLockRecordTypes &= ~unlockType;
    }

    public int getVersion() {
        return 0;
    }

    public void onCreate(Context sysuiContext, Context pluginContext) {
    }

    public void onDestroy() {
    }
}
