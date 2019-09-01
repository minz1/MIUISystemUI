package com.android.keyguard.settings;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.security.MiuiLockPatternUtils;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.LockPatternUtilsWrapper;
import com.android.keyguard.MiuiKeyguardUtils;
import com.android.keyguard.faceunlock.FaceUnlockManager;
import com.android.systemui.R;
import miui.preference.PreferenceActivity;

public class MiuiFaceDataManage extends PreferenceActivity {
    private boolean mConfirmLockLaunched = false;
    private Preference mDeleteFaceData;
    private CheckBoxPreference mFaceDataApplyUnlockScreen;
    private PreferenceCategory mFaceDataCategory;
    private CheckBoxPreference mFaceUnlockByNotificationPreference;
    /* access modifiers changed from: private */
    public FaceUnlockManager mFaceUnlockManager;
    private CheckBoxPreference mFaceUnlockSucessStayScreen;
    private boolean mIsKeyguardPasswordSecured;
    private MiuiLockPatternUtils mLockPatternUtils;
    private LockPatternUtilsWrapper mLockPatternUtilsWrapper;
    private boolean mNeedSkipConfirmPassword = false;

    /* JADX WARNING: type inference failed for: r6v0, types: [android.content.Context, com.android.keyguard.settings.MiuiFaceDataManage, miui.preference.PreferenceActivity] */
    public void onCreate(Bundle savedInstanceState) {
        MiuiFaceDataManage.super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.face_data_manage);
        this.mLockPatternUtils = new MiuiLockPatternUtils(this);
        this.mLockPatternUtilsWrapper = new LockPatternUtilsWrapper(this.mLockPatternUtils);
        this.mFaceUnlockManager = FaceUnlockManager.getInstance(getApplicationContext());
        this.mIsKeyguardPasswordSecured = this.mLockPatternUtilsWrapper.getActivePasswordQuality() != 0;
        this.mFaceDataCategory = (PreferenceCategory) findPreference("lock_screen_face_data");
        this.mDeleteFaceData = findPreference("delete_face_data_recoginition");
        this.mFaceDataApplyUnlockScreen = (CheckBoxPreference) findPreference("apply_face_data_lock");
        this.mFaceDataApplyUnlockScreen.setChecked(Settings.Secure.getIntForUser(getApplicationContext().getContentResolver(), "face_unlcok_apply_for_lock", 1, KeyguardUpdateMonitor.getCurrentUser()) == 1);
        if ("perseus".equals(Build.DEVICE)) {
            this.mFaceDataApplyUnlockScreen.setSummary(R.string.face_data_manage_unlock_slide_msg);
        }
        this.mFaceUnlockSucessStayScreen = (CheckBoxPreference) findPreference("face_unlock_success_stay_screen");
        this.mFaceUnlockSucessStayScreen.setChecked(Settings.Secure.getIntForUser(getApplicationContext().getContentResolver(), "face_unlock_success_stay_screen", 0, KeyguardUpdateMonitor.getCurrentUser()) == 1);
        this.mFaceUnlockByNotificationPreference = (CheckBoxPreference) findPreference("face_unlock_by_notification_screen_on");
        this.mFaceUnlockByNotificationPreference.setChecked(Settings.Secure.getIntForUser(getApplicationContext().getContentResolver(), "face_unlock_by_notification_screen_on", 0, KeyguardUpdateMonitor.getCurrentUser()) == 1);
        if (MiuiKeyguardUtils.isSupportLiftingCamera(getApplicationContext())) {
            this.mFaceDataCategory.removePreference(this.mFaceUnlockSucessStayScreen);
            this.mFaceDataCategory.removePreference(this.mFaceUnlockByNotificationPreference);
            this.mFaceDataApplyUnlockScreen.setSummary(R.string.face_data_manage_unlock_liftcamera_msg);
        }
        this.mNeedSkipConfirmPassword = getIntent().getBooleanExtra("input_facedata_need_skip_password", false);
        if (savedInstanceState != null) {
            this.mConfirmLockLaunched = savedInstanceState.getBoolean("key_confirm_lock_launched");
        }
        if (!this.mIsKeyguardPasswordSecured || !isAvailableFaceData()) {
            finish();
        } else if (!this.mNeedSkipConfirmPassword && !this.mConfirmLockLaunched) {
            Intent intent = new Intent();
            intent.setClassName("com.android.settings", "com.android.settings.MiuiConfirmCommonPassword");
            startActivityForResult(intent, 1002);
            this.mConfirmLockLaunched = true;
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        MiuiFaceDataManage.super.onSaveInstanceState(outState);
        outState.putBoolean("key_confirm_lock_launched", this.mConfirmLockLaunched);
    }

    private boolean isAvailableFaceData() {
        return this.mFaceUnlockManager.hasEnrolledFaces() && this.mFaceUnlockManager.isValidFeature();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        MiuiFaceDataManage.super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1002 && resultCode != -1) {
            finish();
        }
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        String key = preference.getKey();
        if ("delete_face_data_recoginition".equals(key)) {
            handleSecurityLockToggle();
            return true;
        } else if ("apply_face_data_lock".equals(key)) {
            ContentResolver contentResolver = getApplicationContext().getContentResolver();
            boolean isChecked = this.mFaceDataApplyUnlockScreen.isChecked();
            Settings.Secure.putIntForUser(contentResolver, "face_unlcok_apply_for_lock", isChecked ? 1 : 0, KeyguardUpdateMonitor.getCurrentUser());
            return true;
        } else if ("face_unlock_success_stay_screen".equals(key)) {
            ContentResolver contentResolver2 = getApplicationContext().getContentResolver();
            boolean isChecked2 = this.mFaceUnlockSucessStayScreen.isChecked();
            Settings.Secure.putIntForUser(contentResolver2, "face_unlock_success_stay_screen", isChecked2 ? 1 : 0, KeyguardUpdateMonitor.getCurrentUser());
            return true;
        } else if (!"face_unlock_by_notification_screen_on".equals(key)) {
            return MiuiFaceDataManage.super.onPreferenceTreeClick(preferenceScreen, preference);
        } else {
            ContentResolver contentResolver3 = getApplicationContext().getContentResolver();
            boolean isChecked3 = this.mFaceUnlockByNotificationPreference.isChecked();
            Settings.Secure.putIntForUser(contentResolver3, "face_unlock_by_notification_screen_on", isChecked3 ? 1 : 0, KeyguardUpdateMonitor.getCurrentUser());
            return true;
        }
    }

    /* JADX WARNING: type inference failed for: r3v0, types: [android.content.Context, com.android.keyguard.settings.MiuiFaceDataManage] */
    private void handleSecurityLockToggle() {
        DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (which == -1) {
                    MiuiFaceDataManage.this.mFaceUnlockManager.deleteFeature();
                    MiuiFaceDataManage.this.mFaceUnlockManager.release(false);
                    MiuiFaceDataManage.this.finish();
                }
            }
        };
        new AlertDialog.Builder(this).setCancelable(false).setIconAttribute(16843605).setTitle(R.string.face_data_manage_delete).setMessage(R.string.face_data_manage_delete_sure).setPositiveButton(17039370, onClickListener).setNegativeButton(17039360, onClickListener).create().show();
    }
}
