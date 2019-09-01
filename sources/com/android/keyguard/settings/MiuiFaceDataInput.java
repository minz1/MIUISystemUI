package com.android.keyguard.settings;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.miuiface.IMiuiFaceManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.Settings;
import android.security.MiuiLockPatternUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.LockPatternUtilsWrapper;
import com.android.keyguard.MiuiKeyguardUtils;
import com.android.keyguard.analytics.AnalyticsHelper;
import com.android.keyguard.faceunlock.FaceUnlockManager;
import com.android.systemui.R;
import miui.graphics.BitmapFactory;
import miui.preference.PreferenceActivity;

public class MiuiFaceDataInput extends PreferenceActivity implements TextureView.SurfaceTextureListener {
    /* access modifiers changed from: private */
    public Button mCancelOrOkButton;
    private boolean mConfirmLockLaunched = false;
    /* access modifiers changed from: private */
    public Context mContext;
    /* access modifiers changed from: private */
    public final Runnable mDelayedHide = new Runnable() {
        public void run() {
            Toast.makeText(MiuiFaceDataInput.this.getApplicationContext(), R.string.face_data_input_cancel_msg, 0).show();
            MiuiFaceDataInput.this.finish();
        }
    };
    IMiuiFaceManager.EnrollmentCallback mEnrollCallback = new IMiuiFaceManager.EnrollmentCallback() {
        public void onEnrollmentError(int errMsgId, CharSequence errString) {
            Log.i("face_unlock", "enrollCallback, onEnrollmentError errMsgId:" + errMsgId + " errString:" + errString);
        }

        public void onEnrollmentHelp(int helpMsgId, CharSequence helpString) {
            int string;
            Log.i("face_unlock", "enrollCallback, onEnrollmentHelp helpMsgId:" + helpMsgId + " helpString:" + helpString);
            if (helpMsgId == 1001) {
                MiuiFaceDataInput.this.runOnUiThread(new Runnable() {
                    public void run() {
                        MiuiFaceDataInput.this.mSurface.setBackgroundColor(MiuiFaceDataInput.this.getApplicationContext().getResources().getColor(R.color.face_unlock_input_cameraview_color));
                        MiuiFaceDataInput.this.mSurface.refreshCameraView(true);
                        MiuiFaceDataInput.this.mCancelOrOkButton.setEnabled(true);
                        MiuiFaceDataInput.this.mFaceInputMsg.setText(null);
                        MiuiFaceDataInput.this.mWorkerHandler.postDelayed(new Runnable() {
                            public void run() {
                                MiuiFaceDataInput.this.textureView.setAlpha(1.0f);
                            }
                        }, 200);
                    }
                });
                return;
            }
            if (helpMsgId == 33) {
                string = R.string.face_unlock_not_roi;
            } else if (helpMsgId != 1000) {
                switch (helpMsgId) {
                    case 4:
                        string = R.string.face_unlock_quality;
                        break;
                    case 5:
                        string = R.string.face_unlock_not_found;
                        break;
                    case 6:
                        string = R.string.face_unlock_close_screen;
                        break;
                    case 7:
                        string = R.string.face_unlock_stay_away_screen;
                        break;
                    case 8:
                        string = R.string.face_unlock_offset_left;
                        break;
                    case 9:
                        string = R.string.face_unlock_offset_top;
                        break;
                    case 10:
                        string = R.string.face_unlock_offset_right;
                        break;
                    case 11:
                        string = R.string.face_unlock_offset_bottom;
                        break;
                    default:
                        switch (helpMsgId) {
                            case 15:
                                string = R.string.face_unlock_rotated_left;
                                break;
                            case 16:
                                string = R.string.face_unlock_rise;
                                break;
                            case 17:
                                string = R.string.face_unlock_rotated_right;
                                break;
                            case 18:
                                string = R.string.face_unlock_down;
                                break;
                            default:
                                string = R.string.unlock_camera_steady;
                                break;
                        }
                }
            } else {
                string = R.string.face_data_input_camera_fail;
            }
            final int finalString = string;
            MiuiFaceDataInput.this.runOnUiThread(new Runnable() {
                public void run() {
                    MiuiFaceDataInput.this.mFaceInputTitle.setVisibility(4);
                    if (!MiuiFaceDataInput.this.mFaceInputMsg.getText().equals(Integer.valueOf(finalString))) {
                        MiuiFaceDataInput.this.mFaceInputMsg.setText(MiuiFaceDataInput.this.mSCStatus == 1 ? R.string.face_unlock_sc_status : finalString);
                    }
                    if (System.currentTimeMillis() - MiuiFaceDataInput.this.mLastAnnounceTime > 1300) {
                        MiuiFaceDataInput.this.setContentDescription(MiuiFaceDataInput.this.getResources().getString(finalString));
                        long unused = MiuiFaceDataInput.this.mLastAnnounceTime = System.currentTimeMillis();
                    }
                }
            });
        }

        public void onEnrollmentProgress(int remaining, int faceId) {
            Log.i("face_unlock", "enrollCallback, onEnrollmentProgress :" + remaining + "  faceId:" + faceId);
            if (remaining == 0 && faceId != 0) {
                boolean unused = MiuiFaceDataInput.this.mSucceed = true;
                boolean unused2 = MiuiFaceDataInput.this.mHasStart = false;
                MiuiFaceDataInput.this.mWorkerHandler.removeCallbacks(MiuiFaceDataInput.this.mDelayedHide);
                AnalyticsHelper.record("face_unlock_input_success");
                MiuiFaceDataInput.this.mFaceUnlockManager.stopEnrollFace();
                Settings.Secure.putIntForUser(MiuiFaceDataInput.this.getApplicationContext().getContentResolver(), "face_unlcok_apply_for_lock", 1, KeyguardUpdateMonitor.getCurrentUser());
                ContentResolver contentResolver = MiuiFaceDataInput.this.getApplicationContext().getContentResolver();
                boolean isSupportLiftingCamera = true ^ MiuiKeyguardUtils.isSupportLiftingCamera(MiuiFaceDataInput.this.mContext);
                Settings.Secure.putIntForUser(contentResolver, "face_unlock_success_stay_screen", isSupportLiftingCamera ? 1 : 0, KeyguardUpdateMonitor.getCurrentUser());
                ContentResolver contentResolver2 = MiuiFaceDataInput.this.getApplicationContext().getContentResolver();
                boolean isSupportLiftingCamera2 = MiuiKeyguardUtils.isSupportLiftingCamera(MiuiFaceDataInput.this.mContext);
                Settings.Secure.putIntForUser(contentResolver2, "face_unlock_by_notification_screen_on", isSupportLiftingCamera2 ? 1 : 0, KeyguardUpdateMonitor.getCurrentUser());
                MiuiFaceDataInput.this.runOnUiThread(new Runnable() {
                    public void run() {
                        MiuiFaceDataInput.this.mSurface.refreshCameraView(false);
                        MiuiFaceDataInput.this.textureView.setAlpha(0.0f);
                        MiuiFaceDataInput.this.mSurface.setBackground(new BitmapDrawable(BitmapFactory.fastBlur(MiuiFaceDataInput.this.textureView.getBitmap(), 75)));
                        MiuiFaceDataInput.this.mFaceInputTitle.setVisibility(0);
                        MiuiFaceDataInput.this.mFaceInputTitle.setText(R.string.face_data_input_ok_title);
                        MiuiFaceDataInput.this.mFaceInputMsg.setText(MiuiKeyguardUtils.isSupportLiftingCamera(MiuiFaceDataInput.this.mContext) ? R.string.face_data_manage_unlock_liftcamera_msg : R.string.face_data_input_ok_msg);
                        MiuiFaceDataInput.this.mCancelOrOkButton.setText(R.string.face_data_input_ok);
                        MiuiFaceDataInput.this.setContentDescription(MiuiFaceDataInput.this.getResources().getString(R.string.structure_face_data_input_success));
                    }
                });
            }
        }
    };
    /* access modifiers changed from: private */
    public TextView mFaceInputMsg;
    /* access modifiers changed from: private */
    public TextView mFaceInputTitle;
    /* access modifiers changed from: private */
    public FaceUnlockManager mFaceUnlockManager;
    private boolean mFaceUnlockTextureAvailable = true;
    private HandlerThread mHandlerThread = new HandlerThread("detect");
    /* access modifiers changed from: private */
    public boolean mHasConfirmPassword;
    /* access modifiers changed from: private */
    public boolean mHasStart;
    private boolean mIsKeyguardPasswordSecured;
    /* access modifiers changed from: private */
    public long mLastAnnounceTime = 0;
    private MiuiLockPatternUtils mLockPatternUtils;
    private LockPatternUtilsWrapper mLockPatternUtilsWrapper;
    /* access modifiers changed from: private */
    public int mSCStatus;
    private ContentObserver mSCStatusProviderObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            int unused = MiuiFaceDataInput.this.mSCStatus = Settings.System.getIntForUser(MiuiFaceDataInput.this.mContext.getContentResolver(), "sc_status", 0, -2);
            if (MiuiFaceDataInput.this.mSCStatus == 0 && !MiuiFaceDataInput.this.mHasStart && MiuiFaceDataInput.this.mHasConfirmPassword && !MiuiFaceDataInput.this.mSucceed) {
                MiuiFaceDataInput.this.startEnroll();
            }
            if (MiuiFaceDataInput.this.mSCStatus == 1 && !MiuiFaceDataInput.this.mSucceed) {
                MiuiFaceDataInput.this.mFaceInputMsg.setText(R.string.face_unlock_sc_status);
            }
        }
    };
    /* access modifiers changed from: private */
    public boolean mSucceed;
    /* access modifiers changed from: private */
    public CameraView mSurface;
    private SurfaceTexture mSurfaceTexture;
    /* access modifiers changed from: private */
    public Handler mWorkerHandler;
    /* access modifiers changed from: private */
    public TextureView textureView;

    /* JADX WARNING: type inference failed for: r6v0, types: [android.content.Context, miui.preference.PreferenceActivity, com.android.keyguard.settings.MiuiFaceDataInput, android.view.TextureView$SurfaceTextureListener] */
    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        MiuiFaceDataInput.super.onCreate(savedInstanceState);
        setContentView(R.layout.miui_add_face_data_input);
        this.mContext = getApplicationContext();
        this.mLockPatternUtils = new MiuiLockPatternUtils(this);
        this.mLockPatternUtilsWrapper = new LockPatternUtilsWrapper(this.mLockPatternUtils);
        this.mIsKeyguardPasswordSecured = this.mLockPatternUtilsWrapper.getActivePasswordQuality() != 0;
        this.mHandlerThread.start();
        this.mWorkerHandler = new Handler(this.mHandlerThread.getLooper());
        this.mFaceUnlockManager = FaceUnlockManager.getInstance(getApplicationContext());
        if (this.mIsKeyguardPasswordSecured && !isAvailableFaceData()) {
            Intent intent = new Intent();
            intent.setClassName("com.android.settings", "com.android.settings.MiuiConfirmCommonPassword");
            startActivityForResult(intent, 2);
            this.mConfirmLockLaunched = true;
        } else if (!this.mIsKeyguardPasswordSecured && !isAvailableFaceData()) {
            Intent intent2 = new Intent();
            intent2.setClassName("com.android.systemui", "com.android.keyguard.settings.MiuiFaceDataIntroduction");
            startActivityForResult(intent2, 1);
            this.mConfirmLockLaunched = true;
        }
        this.mFaceInputTitle = (TextView) findViewById(R.id.miui_face_input_title);
        this.mFaceInputMsg = (TextView) findViewById(R.id.miui_face_input_msg);
        this.mSurface = (CameraView) findViewById(R.id.camera_view_second);
        this.textureView = (TextureView) findViewById(R.id.camera_view);
        this.textureView.setAlpha(0.0f);
        this.textureView.setSurfaceTextureListener(this);
        this.mCancelOrOkButton = (Button) findViewById(R.id.miui_face_recoginition_input_ok);
        this.mCancelOrOkButton.setEnabled(false);
        this.mCancelOrOkButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (MiuiFaceDataInput.this.mSucceed || MiuiFaceDataInput.this.isAvailableFaceData()) {
                    Intent intent = new Intent();
                    intent.setClassName("com.android.systemui", "com.android.keyguard.settings.MiuiFaceDataManage");
                    intent.putExtra("input_facedata_need_skip_password", true);
                    MiuiFaceDataInput.this.startActivity(intent);
                } else {
                    MiuiFaceDataInput.this.mWorkerHandler.removeCallbacks(MiuiFaceDataInput.this.mDelayedHide);
                    Toast.makeText(MiuiFaceDataInput.this.getApplicationContext(), R.string.face_data_input_cancel_msg, 0).show();
                }
                MiuiFaceDataInput.this.finish();
            }
        });
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("sc_status"), false, this.mSCStatusProviderObserver, -1);
        this.mSCStatusProviderObserver.onChange(false);
        this.mFaceInputMsg.setText(this.mSCStatus == 1 ? R.string.face_unlock_sc_status : R.string.face_data_input_camera_ok);
    }

    public void onSaveInstanceState(Bundle outState) {
        MiuiFaceDataInput.super.onSaveInstanceState(outState);
        outState.putBoolean("key_confirm_lock_launched", this.mConfirmLockLaunched);
    }

    /* access modifiers changed from: private */
    public boolean isAvailableFaceData() {
        return this.mFaceUnlockManager.hasEnrolledFaces() && this.mFaceUnlockManager.isValidFeature();
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == 0 && event.getKeyCode() == 4) {
            if (!this.mSucceed) {
                Toast.makeText(getApplicationContext(), R.string.face_data_input_cancel_msg, 0).show();
            }
            finish();
        }
        return true;
    }

    /* access modifiers changed from: protected */
    public void onResume() {
        MiuiFaceDataInput.super.onResume();
    }

    /* access modifiers changed from: protected */
    public void onPause() {
        MiuiFaceDataInput.super.onPause();
        if (this.mHasStart || this.mSucceed) {
            this.mFaceUnlockManager.stopEnrollFace();
            this.mWorkerHandler.removeCallbacks(this.mDelayedHide);
            this.mSucceed = false;
            this.mHasStart = false;
            finish();
        }
    }

    /* access modifiers changed from: protected */
    public void onDestroy() {
        MiuiFaceDataInput.super.onDestroy();
        this.mContext.getContentResolver().unregisterContentObserver(this.mSCStatusProviderObserver);
        this.mHasConfirmPassword = false;
    }

    /* access modifiers changed from: private */
    public void setContentDescription(String string) {
        this.mFaceInputMsg.setContentDescription(string);
        this.mFaceInputMsg.announceForAccessibility(string);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        MiuiFaceDataInput.super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 2 || requestCode == 1) {
            if (resultCode == -1) {
                Intent intent = new Intent();
                intent.setClassName("com.android.systemui", "com.android.keyguard.settings.MiuiFaceDataSuggestion");
                startActivityForResult(intent, 3);
                return;
            }
            finish();
        } else if (requestCode != 3) {
        } else {
            if (resultCode == -1) {
                this.mHasConfirmPassword = true;
                if (this.mSCStatus == 0) {
                    startEnroll();
                } else {
                    this.mHasStart = false;
                }
            } else {
                finish();
            }
        }
    }

    /* access modifiers changed from: private */
    public void startEnroll() {
        if (this.textureView.getSurfaceTexture() != null) {
            this.mSurfaceTexture = this.textureView.getSurfaceTexture();
            this.mFaceUnlockManager.enrollFace(this.mSurfaceTexture, this.mEnrollCallback);
            this.mWorkerHandler.postDelayed(this.mDelayedHide, 30000);
            this.mHasStart = true;
            return;
        }
        this.mFaceUnlockTextureAvailable = false;
        this.mHasStart = false;
    }

    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.d("face_unlock", "onSurfaceTextureAvailable mHasStart=" + this.mHasStart + ";mFaceUnlockTextureAvailable=" + this.mFaceUnlockTextureAvailable);
        if (!this.mHasStart && !this.mFaceUnlockTextureAvailable) {
            startEnroll();
            this.mFaceUnlockTextureAvailable = true;
        }
    }

    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }

    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        this.mFaceUnlockManager.stopEnrollFace();
        return false;
    }

    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }
}
