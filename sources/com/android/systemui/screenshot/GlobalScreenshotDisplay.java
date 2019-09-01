package com.android.systemui.screenshot;

import android.accounts.Account;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.MiuiSettings;
import android.text.TextUtils;
import android.util.Log;
import android.view.IWindowManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MiuiWindowManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.WindowManagerCompat;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.android.systemui.Constants;
import com.android.systemui.SystemUICompat;
import com.android.systemui.miui.ToastOverlayManager;
import com.android.systemui.screenshot.GlobalScreenshot;
import com.android.systemui.screenshot.ScreenshotScrollView;
import java.io.File;
import java.io.IOException;
import java.lang.Thread;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import miui.R;
import miui.accounts.ExtraAccountManager;
import miui.graphics.BitmapFactory;
import miui.process.IMiuiApplicationThread;
import miui.process.ProcessManager;
import miui.util.ScreenshotUtils;

public class GlobalScreenshotDisplay implements ScreenshotScrollView.AnimatingCallback, Thread.UncaughtExceptionHandler {
    static SoftReference<int[]> sPixelsCache;
    private TextView mActionBarBack;
    /* access modifiers changed from: private */
    public Button mActionBarFeedback;
    /* access modifiers changed from: private */
    public View mActionBarLayout;
    /* access modifiers changed from: private */
    public View mBackgroundView;
    /* access modifiers changed from: private */
    public ViewGroup mButtonContainer;
    /* access modifiers changed from: private */
    public TextView mButtonDelete;
    /* access modifiers changed from: private */
    public TextView mButtonEdit;
    /* access modifiers changed from: private */
    public TextView mButtonLongScreenshot;
    /* access modifiers changed from: private */
    public TextView mButtonSend;
    /* access modifiers changed from: private */
    public Button mButtonStopLongScreenshot;
    /* access modifiers changed from: private */
    public Context mContext;
    private IMiuiApplicationThread mForeAppThread;
    private Handler mHandler = new Handler();
    private boolean mHasNavigationBar;
    /* access modifiers changed from: private */
    public boolean mIsScreenshotSaved;
    private boolean mIsShow;
    /* access modifiers changed from: private */
    public boolean mIsShowingLongScreenshot;
    /* access modifiers changed from: private */
    public boolean mIsTakingLongScreenshot;
    /* access modifiers changed from: private */
    public Bitmap mLongScreenshotFirstPart;
    private BroadcastReceiver mLongScreenshotReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            GlobalScreenshotDisplay.this.onCallbackReceive(intent);
        }
    };
    /* access modifiers changed from: private */
    public int mNavigationBarHeight;
    /* access modifiers changed from: private */
    public NotifyMediaStoreData mNotifyMediaStoreData;
    /* access modifiers changed from: private */
    public boolean mPendingContinueSnap;
    /* access modifiers changed from: private */
    public Runnable mPendingSavedRunnable;
    private BroadcastReceiver mQuitReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            GlobalScreenshotDisplay.this.backAll();
            Context access$200 = GlobalScreenshotDisplay.this.mContext;
            StatHelper.recordCountEvent(access$200, "quit_display", "receiver_" + intent.getAction());
        }
    };
    /* access modifiers changed from: private */
    public View mRootView;
    /* access modifiers changed from: private */
    public Bitmap mScreenshot;
    ArrayList<Bitmap> mScreenshotParts = new ArrayList<>();
    /* access modifiers changed from: private */
    public ScreenshotScrollView mScreenshotView;
    /* access modifiers changed from: private */
    public boolean mTakedTotalParts;
    /* access modifiers changed from: private */
    public ToastOverlayManager mToastOverlayManager;
    /* access modifiers changed from: private */
    public View mTopMsgDivider;
    /* access modifiers changed from: private */
    public TextView mTxtTopMsg;
    private WindowManager.LayoutParams mWindowLayoutParams;
    /* access modifiers changed from: private */
    public WindowManager mWindowManager;

    /* access modifiers changed from: private */
    public void backAll() {
        if (!back()) {
            this.mHandler.post(new Runnable() {
                public void run() {
                    GlobalScreenshotDisplay.this.backAll();
                }
            });
        }
    }

    /* access modifiers changed from: private */
    public boolean back() {
        if (this.mIsTakingLongScreenshot || this.mButtonStopLongScreenshot.getVisibility() == 0) {
            if (this.mButtonStopLongScreenshot.isEnabled()) {
                stopLongScreenshot(true);
                StatHelper.recordCountEvent(this.mContext, "finish_longscreenshot", "cancel");
            }
            return false;
        }
        quit(true, false);
        return true;
    }

    public GlobalScreenshotDisplay(Context context) {
        this.mContext = context;
        this.mContext.setTheme(R.style.Theme_DayNight);
        this.mWindowManager = (WindowManager) context.getSystemService("window");
        try {
            this.mHasNavigationBar = IWindowManager.Stub.asInterface(ServiceManager.getService("window")).hasNavigationBar();
            if (this.mHasNavigationBar && MiuiSettings.Global.getBoolean(this.mContext.getContentResolver(), "force_fsg_nav_bar")) {
                this.mHasNavigationBar = false;
            }
            if (this.mHasNavigationBar) {
                this.mNavigationBarHeight = this.mContext.getResources().getDimensionPixelSize(com.android.systemui.R.dimen.navigation_bar_size);
            }
        } catch (RemoteException e) {
        }
        this.mRootView = ((LayoutInflater) this.mContext.getSystemService("layout_inflater")).inflate(com.android.systemui.R.layout.global_screenshot_display, null);
        this.mRootView.setSystemUiVisibility(512);
        this.mScreenshotView = (ScreenshotScrollView) this.mRootView.findViewById(com.android.systemui.R.id.global_screenshot);
        this.mButtonContainer = (ViewGroup) this.mRootView.findViewById(com.android.systemui.R.id.button_container);
        this.mButtonLongScreenshot = (TextView) this.mRootView.findViewById(com.android.systemui.R.id.button_long_screenshot);
        this.mButtonEdit = (TextView) this.mRootView.findViewById(com.android.systemui.R.id.button_edit);
        this.mButtonSend = (TextView) this.mRootView.findViewById(com.android.systemui.R.id.button_send);
        this.mButtonDelete = (TextView) this.mRootView.findViewById(com.android.systemui.R.id.button_delete);
        this.mBackgroundView = this.mRootView.findViewById(com.android.systemui.R.id.background);
        if ((this.mContext.getResources().getConfiguration().uiMode & 48) == 32) {
            this.mButtonSend.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.action_button_send_dark, 0, 0);
            this.mButtonDelete.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.action_button_delete_dark, 0, 0);
            this.mButtonEdit.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.action_button_edit_dark, 0, 0);
        } else {
            this.mButtonSend.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.action_button_send_light, 0, 0);
            this.mButtonDelete.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.action_button_delete_light, 0, 0);
            this.mButtonEdit.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.action_button_edit_light, 0, 0);
        }
        this.mActionBarLayout = this.mRootView.findViewById(com.android.systemui.R.id.screenshot_actionbar_layout);
        this.mActionBarFeedback = (Button) this.mRootView.findViewById(com.android.systemui.R.id.screenshot_feedback);
        if (isShowFeedback()) {
            this.mActionBarFeedback.setVisibility(0);
        }
        this.mActionBarFeedback.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                GlobalScreenshotDisplay.this.clickActionBtn("feedback");
            }
        });
        this.mActionBarBack = (TextView) this.mRootView.findViewById(com.android.systemui.R.id.screenshot_toalbum);
        this.mActionBarBack.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                boolean unused = GlobalScreenshotDisplay.this.back();
            }
        });
        this.mTxtTopMsg = (TextView) this.mRootView.findViewById(com.android.systemui.R.id.txt_top_msg);
        this.mTopMsgDivider = this.mRootView.findViewById(com.android.systemui.R.id.top_msg_divider);
        if (this.mHasNavigationBar) {
            if (this.mContext.getResources().getConfiguration().orientation == 1) {
                this.mButtonContainer.setPadding(this.mButtonContainer.getPaddingLeft(), this.mButtonContainer.getPaddingTop(), this.mButtonContainer.getPaddingRight(), this.mNavigationBarHeight);
            } else {
                int ablLeftPadding = this.mActionBarLayout.getPaddingLeft();
                int ablRightPadding = this.mActionBarLayout.getPaddingRight();
                if (this.mWindowManager.getDefaultDisplay().getRotation() == 3) {
                    ablLeftPadding = this.mNavigationBarHeight + this.mActionBarLayout.getPaddingLeft();
                } else if (this.mWindowManager.getDefaultDisplay().getRotation() == 1) {
                    ablRightPadding = this.mActionBarLayout.getPaddingRight() + this.mNavigationBarHeight;
                }
                this.mActionBarLayout.setPadding(ablLeftPadding, this.mActionBarLayout.getPaddingTop(), ablRightPadding, this.mActionBarLayout.getPaddingBottom());
                this.mScreenshotView.setPadding(this.mNavigationBarHeight + this.mScreenshotView.getPaddingLeft(), this.mScreenshotView.getPaddingTop(), this.mScreenshotView.getPaddingRight() + this.mNavigationBarHeight, this.mScreenshotView.getPaddingBottom());
            }
        }
        Configuration config = this.mContext.getResources().getConfiguration();
        if (Constants.IS_NOTCH && config.orientation == 1) {
            int naturalBarHeight = this.mContext.getResources().getDimensionPixelSize(com.android.systemui.R.dimen.status_bar_height);
            ViewGroup.LayoutParams layoutParams = this.mActionBarLayout.getLayoutParams();
            layoutParams.height = context.getResources().getDimensionPixelSize(com.android.systemui.R.dimen.screenshot_actionbar_back_height) + naturalBarHeight;
            this.mActionBarLayout.setLayoutParams(layoutParams);
            int paddingSize = context.getResources().getDimensionPixelSize(com.android.systemui.R.dimen.screenshot_topmsg_padding);
            this.mTxtTopMsg.setPadding(paddingSize, paddingSize + naturalBarHeight, paddingSize, paddingSize);
        }
        this.mButtonStopLongScreenshot = (Button) this.mRootView.findViewById(com.android.systemui.R.id.button_stop_long_screenshot);
        this.mScreenshotView.setAnimatingCallback(this);
        this.mScreenshotView.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (GlobalScreenshotDisplay.this.mIsTakingLongScreenshot) {
                    GlobalScreenshotDisplay.this.mTxtTopMsg.setText(com.android.systemui.R.string.long_screenshot_top_msg_manual);
                }
                return false;
            }
        });
        this.mRootView.setFocusableInTouchMode(true);
        this.mRootView.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (GlobalScreenshotDisplay.this.isPendingAction()) {
                    return true;
                }
                if (event.getAction() != 0 || keyCode != 4) {
                    return false;
                }
                boolean unused = GlobalScreenshotDisplay.this.back();
                StatHelper.recordCountEvent(GlobalScreenshotDisplay.this.mContext, "quit_display", "key_back");
                return true;
            }
        });
        this.mButtonLongScreenshot.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!GlobalScreenshotDisplay.this.isPendingAction()) {
                    if (GlobalScreenshotDisplay.this.canLongScreenshot()) {
                        GlobalScreenshotDisplay.this.enterTakingLongScreenshot();
                        StatHelper.recordCountEvent(GlobalScreenshotDisplay.this.mContext, "longscreenshot_button_click", "enable");
                    } else {
                        Toast toast = Toast.makeText(GlobalScreenshotDisplay.this.mContext, com.android.systemui.R.string.long_screenshot_not_support_msg, 0);
                        toast.setType(2006);
                        toast.show();
                        GlobalScreenshotDisplay.this.mToastOverlayManager.dispatchShowToast(toast);
                        StatHelper.recordCountEvent(GlobalScreenshotDisplay.this.mContext, "longscreenshot_button_click", "disable");
                    }
                }
            }
        });
        this.mButtonEdit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                GlobalScreenshotDisplay.this.clickActionBtn("edit");
            }
        });
        this.mButtonSend.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                GlobalScreenshotDisplay.this.clickActionBtn("send");
            }
        });
        this.mButtonStopLongScreenshot.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                GlobalScreenshotDisplay.this.stopLongScreenshot(false);
                StatHelper.recordCountEvent(GlobalScreenshotDisplay.this.mContext, "finish_longscreenshot", "button_click");
            }
        });
        this.mButtonDelete.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!GlobalScreenshotDisplay.this.isPendingAction()) {
                    GlobalScreenshotDisplay.this.showDeleteDialog();
                    StatHelper.recordCountEvent(GlobalScreenshotDisplay.this.mContext, "delete_button_click", GlobalScreenshotDisplay.this.mIsShowingLongScreenshot ? "longscreenshot" : "normal");
                }
            }
        });
        WindowManager.LayoutParams layoutParams2 = new WindowManager.LayoutParams(-1, -1, 0, 0, 2014, 17368320, -3);
        this.mWindowLayoutParams = layoutParams2;
        this.mWindowLayoutParams.screenOrientation = 14;
        if (Build.VERSION.SDK_INT >= 28) {
            this.mWindowLayoutParams.extraFlags |= 8388608;
        }
        WindowManagerCompat.setLayoutInDisplayCutoutMode(this.mWindowLayoutParams, 1);
        this.mWindowLayoutParams.setTitle("GlobalScreenshotShow");
        this.mToastOverlayManager = new ToastOverlayManager();
        this.mToastOverlayManager.setup(this.mContext, (ViewGroup) this.mRootView);
    }

    private boolean isShowFeedback() {
        if (!miui.os.Build.IS_ALPHA_BUILD && !miui.os.Build.IS_DEVELOPMENT_VERSION) {
            return false;
        }
        Account xiaomiAccount = ExtraAccountManager.getXiaomiAccount(this.mContext);
        return true;
    }

    public void show(Bitmap screenshot, NotifyMediaStoreData data, int thumbnailX, int thumbnailY, int thumbnailW, int thumbnailH) {
        Thread.currentThread().setUncaughtExceptionHandler(this);
        this.mNotifyMediaStoreData = data;
        this.mIsShow = true;
        Bitmap bitmap = screenshot;
        this.mScreenshot = bitmap;
        this.mIsShowingLongScreenshot = false;
        this.mIsScreenshotSaved = false;
        this.mPendingSavedRunnable = null;
        this.mScreenshotView.setSingleBitmap(this.mScreenshot);
        this.mWindowManager.addView(this.mRootView, this.mWindowLayoutParams);
        this.mContext.sendBroadcast(new Intent("android.intent.action.CLOSE_SYSTEM_DIALOGS"));
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.CLOSE_SYSTEM_DIALOGS");
        filter.addAction("android.intent.action.PHONE_STATE");
        filter.addAction("android.intent.action.SCREEN_OFF");
        this.mContext.getApplicationContext().registerReceiver(this.mQuitReceiver, filter);
        sendNavigationBarVisibilityChangeIfNeed(true);
        this.mRootView.measure(View.MeasureSpec.makeMeasureSpec(bitmap.getWidth(), 1073741824), View.MeasureSpec.makeMeasureSpec(bitmap.getHeight(), 1073741824));
        this.mRootView.layout(0, 0, this.mRootView.getMeasuredWidth(), this.mRootView.getMeasuredHeight());
        this.mRootView.requestFocus();
        Configuration config = this.mContext.getResources().getConfiguration();
        this.mActionBarFeedback.getLayoutParams();
        if (config.orientation == 2) {
            this.mTopMsgDivider.setVisibility(8);
            this.mActionBarLayout.setBackgroundColor(0);
        } else {
            this.mTopMsgDivider.setVisibility(0);
        }
        this.mActionBarLayout.setVisibility(0);
        this.mButtonLongScreenshot.setVisibility(0);
        this.mButtonLongScreenshot.setAlpha(canLongScreenshot() ? 1.0f : 0.5f);
        if (!isShowFeedback() || !this.mHasNavigationBar || this.mContext.getResources().getConfiguration().orientation != 2) {
            calScreenshotViewPaddingAndAnim(0, thumbnailX, thumbnailY, thumbnailW, thumbnailH);
            return;
        }
        ViewTreeObserver viewTreeObserver = this.mActionBarFeedback.getViewTreeObserver();
        final int i = thumbnailX;
        final int i2 = thumbnailY;
        final int i3 = thumbnailW;
        final int i4 = thumbnailH;
        AnonymousClass13 r0 = new ViewTreeObserver.OnGlobalLayoutListener() {
            public void onGlobalLayout() {
                int[] location = new int[2];
                GlobalScreenshotDisplay.this.mActionBarFeedback.getLocationOnScreen(location);
                GlobalScreenshotDisplay.this.calScreenshotViewPaddingAndAnim(((int) GlobalScreenshotDisplay.this.mContext.getResources().getDimension(com.android.systemui.R.dimen.screenshot_feedback_margin_right)) + ((GlobalScreenshotDisplay.this.mContext.getResources().getDisplayMetrics().widthPixels + GlobalScreenshotDisplay.this.mNavigationBarHeight) - location[0]), i, i2, i3, i4);
                GlobalScreenshotDisplay.this.mActionBarFeedback.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        };
        viewTreeObserver.addOnGlobalLayoutListener(r0);
    }

    /* access modifiers changed from: private */
    public void calScreenshotViewPaddingAndAnim(int paddingRight, int thumbnailX, int thumbnailY, int thumbnailW, int thumbnailH) {
        this.mScreenshotView.autoCalcPadding();
        if (paddingRight > 0) {
            this.mScreenshotView.setPadding(paddingRight, this.mScreenshotView.getPaddingTop(), paddingRight, this.mScreenshotView.getPaddingBottom());
        }
        float pivotX = GlobalScreenshot.calcPivot((float) this.mScreenshotView.getPaddingLeft(), (float) this.mScreenshotView.getWidthInner(), (float) thumbnailX, (float) thumbnailW) - ((float) this.mScreenshotView.getLeft());
        float pivotY = GlobalScreenshot.calcPivot((float) this.mScreenshotView.getPaddingTop(), (float) this.mScreenshotView.getHeightInner(), (float) thumbnailY, (float) thumbnailH) - ((float) this.mScreenshotView.getTop());
        float scale = ((float) thumbnailH) / ((float) this.mScreenshotView.getHeightInner());
        this.mScreenshotView.setPivotX(pivotX);
        this.mScreenshotView.setPivotY(pivotY);
        this.mScreenshotView.setScaleX(scale);
        this.mScreenshotView.setScaleY(scale);
        this.mScreenshotView.setAlpha(1.0f);
        this.mScreenshotView.setTranslationY(0.0f);
        this.mBackgroundView.setVisibility(0);
        this.mBackgroundView.setAlpha(0.0f);
        this.mButtonContainer.setTranslationY((float) this.mButtonContainer.getHeight());
        this.mRootView.postDelayed(new Runnable() {
            public void run() {
                GlobalScreenshotDisplay.this.mScreenshotView.animate().setDuration(300).scaleX(1.0f).scaleY(1.0f).start();
                GlobalScreenshotDisplay.this.mButtonContainer.animate().setDuration(300).translationY(0.0f).start();
                GlobalScreenshotDisplay.this.mBackgroundView.animate().setDuration(300).alpha(1.0f).start();
            }
        }, 20);
    }

    public void setIsScreenshotSaved() {
        this.mIsScreenshotSaved = true;
        if (this.mPendingSavedRunnable != null) {
            this.mPendingSavedRunnable.run();
            this.mPendingSavedRunnable = null;
        }
    }

    public boolean canLongScreenshot() {
        if (((KeyguardManager) this.mContext.getSystemService("keyguard")).isKeyguardLocked()) {
            return false;
        }
        IMiuiApplicationThread appThread = getForegroundApplicationThread();
        if (appThread == null) {
            Log.w("GlobalScreenshotDisplay", "getForegroundApplicationThread failed.");
            return false;
        }
        try {
            return appThread.longScreenshot(1, this.mNavigationBarHeight);
        } catch (RemoteException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private IMiuiApplicationThread getForegroundApplicationThread() {
        return ProcessManager.getForegroundApplicationThread();
    }

    /* access modifiers changed from: private */
    public void quit(boolean isAnimting, boolean isDelete) {
        if (this.mIsShow) {
            this.mIsShow = false;
            this.mScreenshotParts.clear();
            this.mContext.getApplicationContext().unregisterReceiver(this.mQuitReceiver);
            sendNavigationBarVisibilityChangeIfNeed(false);
            if (!isAnimting) {
                this.mWindowManager.removeView(this.mRootView);
                Thread.currentThread().setUncaughtExceptionHandler(null);
            } else {
                if (!isDelete) {
                    this.mScreenshotView.animate().translationY((float) (-this.mRootView.getHeight()));
                } else {
                    this.mScreenshotView.setPivotX((float) (this.mScreenshotView.getWidth() / 2));
                    this.mScreenshotView.setPivotY((float) (this.mScreenshotView.getHeight() / 2));
                    this.mScreenshotView.animate().scaleX(0.7f).scaleY(0.7f).alpha(0.0f);
                }
                this.mScreenshotView.animate().setDuration(300).withEndAction(new Runnable() {
                    public void run() {
                        GlobalScreenshotDisplay.this.mWindowManager.removeView(GlobalScreenshotDisplay.this.mRootView);
                        Thread.currentThread().setUncaughtExceptionHandler(null);
                    }
                }).start();
                this.mButtonContainer.animate().setDuration(300).translationY((float) this.mButtonContainer.getHeight()).start();
                this.mBackgroundView.animate().setDuration(300).alpha(0.0f).start();
            }
            GlobalScreenshot.notifyMediaAndFinish(this.mContext, this.mNotifyMediaStoreData);
        }
    }

    private void sendNavigationBarVisibilityChangeIfNeed(boolean isShow) {
        if (((KeyguardManager) this.mContext.getSystemService("keyguard")).isKeyguardLocked()) {
            Intent intent = new Intent("com.miui.lockscreen.navigation_bar_visibility");
            intent.putExtra("is_show", isShow);
            this.mContext.sendBroadcast(intent);
        }
    }

    /* access modifiers changed from: private */
    public boolean isPendingAction() {
        return this.mPendingSavedRunnable != null;
    }

    private void dismissKeyguardIfNeed() {
        if (((KeyguardManager) this.mContext.getSystemService("keyguard")).isKeyguardLocked()) {
            SystemUICompat.dismissKeyguardOnNextActivity();
        }
    }

    /* access modifiers changed from: private */
    public void showDeleteDialog() {
        AlertDialog dlg = new AlertDialog.Builder(this.mContext).setTitle(com.android.systemui.R.string.screenshot_delete_dlg_title).setMessage(com.android.systemui.R.string.screenshot_delete_dlg_msg).setNegativeButton(com.android.systemui.R.string.screenshot_dlg_cancel, null).setPositiveButton(com.android.systemui.R.string.screenshot_dlg_delete_button, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (GlobalScreenshotDisplay.this.mIsScreenshotSaved) {
                    GlobalScreenshotDisplay.this.delete();
                } else {
                    Runnable unused = GlobalScreenshotDisplay.this.mPendingSavedRunnable = new Runnable() {
                        public void run() {
                            GlobalScreenshotDisplay.this.delete();
                        }
                    };
                }
                StatHelper.recordCountEvent(GlobalScreenshotDisplay.this.mContext, "indeed_deleted", GlobalScreenshotDisplay.this.mIsShowingLongScreenshot ? "longscreenshot" : "normal");
            }
        }).create();
        dlg.getWindow().setType(this.mWindowLayoutParams.type);
        dlg.show();
    }

    /* access modifiers changed from: private */
    public void delete() {
        new File(this.mNotifyMediaStoreData.tempImageFilePath).delete();
        Log.e("GlobalScreenshotDisplay", "File delete, path = " + this.mNotifyMediaStoreData.tempImageFilePath);
        if (this.mNotifyMediaStoreData.finisher != null) {
            this.mNotifyMediaStoreData.finisher.run();
        }
        this.mNotifyMediaStoreData.isRunned = true;
        quit(true, true);
    }

    /* access modifiers changed from: private */
    public Bundle createQuitAnimationBundle() {
        return ActivityOptions.makeCustomAnimation(this.mContext, 0, 0, this.mHandler, new ActivityOptions.OnAnimationStartedListener() {
            public void onAnimationStarted() {
                GlobalScreenshotDisplay.this.mRootView.postDelayed(new Runnable() {
                    public void run() {
                        GlobalScreenshotDisplay.this.quit(false, false);
                    }
                }, 300);
            }
        }).toBundle();
    }

    /* access modifiers changed from: private */
    public void clickActionBtn(final String btnType) {
        if (!isPendingAction()) {
            if (this.mIsScreenshotSaved) {
                startPicActivity(btnType);
            } else {
                this.mPendingSavedRunnable = new Runnable() {
                    public void run() {
                        GlobalScreenshotDisplay.this.startPicActivity(btnType);
                    }
                };
            }
            this.mButtonContainer.animate().setDuration(200).translationY((float) this.mButtonContainer.getHeight()).start();
            if (TextUtils.equals(btnType, "send")) {
                StatHelper.recordCountEvent(this.mContext, "send_button_click", this.mIsShowingLongScreenshot ? "longscreenshot" : "normal");
            } else if (TextUtils.equals(btnType, "edit")) {
                StatHelper.recordCountEvent(this.mContext, "edit_button_click", this.mIsShowingLongScreenshot ? "longscreenshot" : "normal");
            }
        }
    }

    /* access modifiers changed from: private */
    public void startPicActivity(final String btnType) {
        dismissKeyguardIfNeed();
        GlobalScreenshot.notifyMediaAndFinish(this.mContext, this.mNotifyMediaStoreData, new GlobalScreenshot.ScreenshotFinishCallback() {
            public void onFinish() {
                if (TextUtils.equals(btnType, "feedback")) {
                    if (GlobalScreenshotDisplay.this.mNotifyMediaStoreData.outUri != null) {
                        Intent intent = new Intent("android.intent.action.SEND");
                        intent.setClassName("com.miui.bugreport", "com.miui.bugreport.ui.FeedbackActivity");
                        intent.putExtra("android.intent.extra.STREAM", GlobalScreenshotDisplay.this.mNotifyMediaStoreData.outUri);
                        intent.setType("image/*");
                        intent.addFlags(268468224);
                        GlobalScreenshotDisplay.this.mContext.startActivity(intent, GlobalScreenshotDisplay.this.createQuitAnimationBundle());
                    }
                    return;
                }
                if (TextUtils.equals(btnType, "edit")) {
                    GlobalScreenshotDisplay.this.mScreenshotView.resetToShortMode(true);
                }
                if (GlobalScreenshotDisplay.this.mNotifyMediaStoreData.outUri != null) {
                    Intent intent2 = new Intent();
                    intent2.setPackage("com.miui.gallery");
                    intent2.setData(GlobalScreenshotDisplay.this.mNotifyMediaStoreData.outUri);
                    if (TextUtils.equals(btnType, "send")) {
                        intent2.addFlags(268468224);
                        intent2.setAction("android.intent.action.VIEW");
                        intent2.putExtra("com.miui.gallery.extra.photo_enter_choice_mode", true);
                        intent2.putExtra("com.miui.gallery.extra.sync_load_intent_data", true);
                        intent2.putExtra("com.miui.gallery.extra.show_menu_after_choice_mode", true);
                        intent2.putExtra("from_MiuiScreenShot", true);
                        GlobalScreenshotDisplay.this.mContext.startActivity(intent2, GlobalScreenshotDisplay.this.createQuitAnimationBundle());
                    } else if (TextUtils.equals(btnType, "edit")) {
                        intent2.setAction("android.intent.action.EDIT");
                        intent2.putExtra("IsScreenshot", true);
                        intent2.putExtra("IsLongScreenshot", GlobalScreenshotDisplay.this.mIsShowingLongScreenshot);
                        Intent intermediateIntent = new Intent();
                        intermediateIntent.addFlags(268468224);
                        intermediateIntent.setClass(GlobalScreenshotDisplay.this.mContext, IntermediateActivity.class);
                        intermediateIntent.putExtra("Intent", intent2);
                        GlobalScreenshotDisplay.this.mContext.startActivity(intermediateIntent, GlobalScreenshotDisplay.this.createQuitAnimationBundle());
                    }
                } else {
                    GlobalScreenshotDisplay.this.quit(false, false);
                    Toast.makeText(GlobalScreenshotDisplay.this.mContext, GlobalScreenshotDisplay.this.mContext.getResources().getString(com.android.systemui.R.string.screenshot_insert_failed), 0).show();
                }
            }
        });
    }

    /* access modifiers changed from: private */
    public boolean startLongScreenshot() {
        this.mForeAppThread = getForegroundApplicationThread();
        Log.d("GlobalScreenshotDisplay", "startLongScreenshot:" + this.mForeAppThread);
        if (this.mForeAppThread == null) {
            return false;
        }
        this.mScreenshotView.resetToShortMode(true);
        Bitmap bmp = getScreenshotForLong(this.mContext, true);
        try {
            if (!this.mForeAppThread.longScreenshot(2, this.mNavigationBarHeight)) {
                return false;
            }
            this.mLongScreenshotFirstPart = bmp;
            this.mContext.getApplicationContext().registerReceiverAsUser(this.mLongScreenshotReceiver, UserHandle.ALL, new IntentFilter("com.miui.util.LongScreenshotUtils.LongScreenshot"), null, null);
            return true;
        } catch (RemoteException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    /* access modifiers changed from: private */
    public void stopLongScreenshot(boolean isCancel) {
        if (this.mForeAppThread != null) {
            try {
                this.mForeAppThread.longScreenshot(4, this.mNavigationBarHeight);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        finishTakingLongScreenshot(isCancel);
    }

    private Bitmap getScreenshotForLong(Context context, boolean isFirst) {
        int maxLayout;
        if (isFirst) {
            maxLayout = MiuiWindowManager.getLayer(context, 2000);
        } else {
            maxLayout = MiuiWindowManager.getLayer(context, 2001) - 1;
        }
        return ScreenshotUtils.getScreenshot(context, 1.0f, 0, maxLayout, MiuiSettings.Global.getBoolean(context.getContentResolver(), "force_fsg_nav_bar"));
    }

    static Bitmap cropBitmap(Bitmap bmp, int top, int height) {
        int[] pixels = null;
        if (top < 0 || height <= 0 || top + height > bmp.getHeight()) {
            return null;
        }
        int width = bmp.getWidth();
        if (sPixelsCache != null) {
            pixels = sPixelsCache.get();
        }
        if (pixels == null || pixels.length != height * width) {
            pixels = new int[(height * width)];
            sPixelsCache = new SoftReference<>(pixels);
        }
        bmp.getPixels(pixels, 0, width, 0, top, width, height);
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
    }

    /* access modifiers changed from: private */
    public void onCallbackReceive(final Intent intent) {
        final boolean isEnd = intent.getBooleanExtra("IsEnd", false);
        new AsyncTask<Intent, Void, Bitmap>() {
            /* access modifiers changed from: protected */
            public Bitmap doInBackground(Intent... params) {
                return GlobalScreenshotDisplay.this.snapForLongScreenshot(params[0]);
            }

            /* access modifiers changed from: protected */
            public void onPostExecute(Bitmap screenshot) {
                if (GlobalScreenshotDisplay.this.mIsTakingLongScreenshot) {
                    boolean isFirstTime = GlobalScreenshotDisplay.this.mScreenshotParts.size() == 0;
                    if (isFirstTime) {
                        int bottomLoc = intent.getIntExtra("BottomLoc", 0);
                        int viewBottom = intent.getIntExtra("ViewBottom", 0);
                        GlobalScreenshotDisplay.this.mScreenshotParts.add(GlobalScreenshotDisplay.cropBitmap(GlobalScreenshotDisplay.this.mLongScreenshotFirstPart, 0, bottomLoc));
                        if (viewBottom < GlobalScreenshotDisplay.this.mLongScreenshotFirstPart.getHeight() - 1) {
                            GlobalScreenshotDisplay.this.mScreenshotView.setBottomPart(GlobalScreenshotDisplay.cropBitmap(GlobalScreenshotDisplay.this.mLongScreenshotFirstPart, viewBottom, GlobalScreenshotDisplay.this.mLongScreenshotFirstPart.getHeight() - viewBottom));
                        } else {
                            GlobalScreenshotDisplay.this.mScreenshotView.setBottomPart(null);
                        }
                    }
                    if (screenshot != null) {
                        GlobalScreenshotDisplay.this.mScreenshotParts.add(screenshot);
                    }
                    if (isFirstTime) {
                        GlobalScreenshotDisplay.this.mScreenshotView.setSingleBitmap(null);
                        GlobalScreenshotDisplay.this.mScreenshotView.setBitmaps(GlobalScreenshotDisplay.this.mScreenshotParts, true);
                        GlobalScreenshotDisplay.this.mScreenshotView.setIsTakingLongScreenshot(true);
                        GlobalScreenshotDisplay.this.mScreenshotView.startAnimating();
                    } else {
                        GlobalScreenshotDisplay.this.mScreenshotView.setBitmaps(GlobalScreenshotDisplay.this.mScreenshotParts, false);
                    }
                    if (!isEnd) {
                        boolean unused = GlobalScreenshotDisplay.this.mPendingContinueSnap = true;
                        GlobalScreenshotDisplay.this.tryToContinueOrFinish();
                    } else {
                        boolean unused2 = GlobalScreenshotDisplay.this.mTakedTotalParts = true;
                    }
                }
            }
        }.execute(new Intent[]{intent});
    }

    /* access modifiers changed from: private */
    public Bitmap snapForLongScreenshot(Intent intent) {
        Bitmap screenshot = getScreenshotForLong(this.mContext, false);
        boolean isEnd = intent.getBooleanExtra("IsEnd", false);
        int topLoc = intent.getIntExtra("TopLoc", 0);
        int height = intent.getIntExtra("BottomLoc", 0) - topLoc;
        if (isEnd) {
            height = screenshot.getHeight() - topLoc;
        }
        Bitmap cropedBmp = null;
        if (height > 0) {
            cropedBmp = cropBitmap(screenshot, topLoc, height);
        }
        screenshot.recycle();
        return cropedBmp;
    }

    /* access modifiers changed from: private */
    public void tryToContinueOrFinish() {
        if (this.mScreenshotView.getShowedPageCount() < this.mScreenshotParts.size() - 1) {
            return;
        }
        if (this.mPendingContinueSnap) {
            try {
                this.mForeAppThread.longScreenshot(3, this.mNavigationBarHeight);
                this.mPendingContinueSnap = false;
            } catch (RemoteException ex) {
                ex.printStackTrace();
            }
        } else if (this.mTakedTotalParts && this.mScreenshotView.getShowedPageCount() == this.mScreenshotParts.size() && !this.mScreenshotView.getIsManuTaking()) {
            finishTakingLongScreenshot(false);
            this.mScreenshotView.setBottomPart(null);
            StatHelper.recordCountEvent(this.mContext, "finish_longscreenshot", "auto_finish");
        }
    }

    /* access modifiers changed from: private */
    public void finishTakingLongScreenshot(final boolean isCancel) {
        if (isCancel || this.mIsScreenshotSaved) {
            Log.d("GlobalScreenshotDisplay", "finishTakingLongScreenshot:" + this.mForeAppThread);
            if (this.mForeAppThread != null) {
                this.mScreenshotView.stopAnimating();
                this.mContext.getApplicationContext().unregisterReceiver(this.mLongScreenshotReceiver);
            }
            this.mButtonStopLongScreenshot.setEnabled(false);
            if (isCancel) {
                exitTakingLongScreenshot(true);
            } else {
                this.mButtonStopLongScreenshot.setText(com.android.systemui.R.string.long_screenshot_processing);
                new AsyncTask<Void, Void, Bitmap>() {
                    /* access modifiers changed from: protected */
                    public Bitmap doInBackground(Void[] params) {
                        Bitmap resultBmp = null;
                        try {
                            Bitmap longScreenshot = GlobalScreenshotDisplay.this.mScreenshotView.buildLongScreenshot();
                            if (longScreenshot != null) {
                                try {
                                    BitmapFactory.saveToFile(longScreenshot, GlobalScreenshotDisplay.this.mNotifyMediaStoreData.tempImageFilePath, true);
                                } catch (IOException ex) {
                                    ex.printStackTrace();
                                }
                                GlobalScreenshotDisplay.this.mNotifyMediaStoreData.width = longScreenshot.getWidth();
                                GlobalScreenshotDisplay.this.mNotifyMediaStoreData.height = longScreenshot.getHeight();
                                int minHeight = GlobalScreenshotDisplay.this.mLongScreenshotFirstPart.getHeight();
                                resultBmp = Bitmap.createScaledBitmap(longScreenshot, (longScreenshot.getWidth() * minHeight) / longScreenshot.getHeight(), minHeight, true);
                                StatHelper.recordNumericPropertyEvent(GlobalScreenshotDisplay.this.mContext, "longscreenshot_height", (long) longScreenshot.getHeight());
                            } else {
                                Toast toast = Toast.makeText(GlobalScreenshotDisplay.this.mContext, com.android.systemui.R.string.long_screenshot_out_of_memory_error, 0);
                                toast.setType(2006);
                                toast.show();
                                StatHelper.recordCountEvent(GlobalScreenshotDisplay.this.mContext, "longscreenshot_fail_height");
                            }
                            return resultBmp;
                        } catch (Exception ex2) {
                            Log.w("GlobalScreenshotDisplay", "", ex2);
                            return null;
                        }
                    }

                    /* access modifiers changed from: protected */
                    public void onPostExecute(Bitmap result) {
                        if (result != null) {
                            Bitmap unused = GlobalScreenshotDisplay.this.mScreenshot = result;
                            GlobalScreenshotDisplay.this.exitTakingLongScreenshot(false);
                            return;
                        }
                        GlobalScreenshotDisplay.this.exitTakingLongScreenshot(true);
                    }
                }.execute(new Void[0]);
            }
            return;
        }
        this.mPendingSavedRunnable = new Runnable() {
            public void run() {
                GlobalScreenshotDisplay.this.finishTakingLongScreenshot(isCancel);
            }
        };
    }

    /* access modifiers changed from: private */
    public void enterTakingLongScreenshot() {
        this.mIsTakingLongScreenshot = true;
        this.mPendingContinueSnap = false;
        this.mTakedTotalParts = false;
        this.mScreenshotParts.clear();
        GlobalScreenshot.beforeTakeScreenshot(this.mContext);
        this.mWindowLayoutParams.flags |= 128;
        this.mWindowManager.updateViewLayout(this.mRootView, this.mWindowLayoutParams);
        this.mButtonContainer.animate().translationY((float) this.mButtonLongScreenshot.getHeight()).withEndAction(new Runnable() {
            public void run() {
                GlobalScreenshotDisplay.this.mButtonLongScreenshot.setVisibility(8);
                GlobalScreenshotDisplay.this.mButtonEdit.setVisibility(8);
                GlobalScreenshotDisplay.this.mButtonSend.setVisibility(8);
                GlobalScreenshotDisplay.this.mButtonDelete.setVisibility(8);
                GlobalScreenshotDisplay.this.mActionBarLayout.setVisibility(8);
                GlobalScreenshotDisplay.this.mTopMsgDivider.setVisibility(0);
                GlobalScreenshotDisplay.this.mTxtTopMsg.setText(com.android.systemui.R.string.long_screenshot_top_msg);
                GlobalScreenshotDisplay.this.mTxtTopMsg.setVisibility(0);
                GlobalScreenshotDisplay.this.mTxtTopMsg.setTranslationY((float) (-GlobalScreenshotDisplay.this.mTxtTopMsg.getHeight()));
                GlobalScreenshotDisplay.this.mTxtTopMsg.animate().translationY(0.0f).start();
                GlobalScreenshotDisplay.this.mButtonStopLongScreenshot.setText(com.android.systemui.R.string.long_screenshot_stop);
                GlobalScreenshotDisplay.this.mButtonStopLongScreenshot.setEnabled(true);
                GlobalScreenshotDisplay.this.mButtonStopLongScreenshot.setVisibility(0);
                GlobalScreenshotDisplay.this.mButtonContainer.animate().translationY(0.0f).withEndAction(new Runnable() {
                    public void run() {
                        if (!GlobalScreenshotDisplay.this.startLongScreenshot()) {
                            GlobalScreenshotDisplay.this.exitTakingLongScreenshot(true);
                        }
                    }
                }).start();
            }
        }).start();
    }

    /* access modifiers changed from: private */
    public void exitTakingLongScreenshot(final boolean isCancel) {
        Log.d("GlobalScreenshotDisplay", "exitTakingLongScreenshot:" + isCancel);
        this.mIsTakingLongScreenshot = false;
        this.mForeAppThread = null;
        if (this.mLongScreenshotFirstPart != null) {
            this.mLongScreenshotFirstPart.recycle();
            this.mLongScreenshotFirstPart = null;
        }
        GlobalScreenshot.afterTakeScreenshot(this.mContext);
        this.mWindowLayoutParams.flags &= -129;
        if (this.mRootView.getWindowToken() != null) {
            this.mWindowManager.updateViewLayout(this.mRootView, this.mWindowLayoutParams);
        }
        if (isCancel) {
            this.mTxtTopMsg.animate().translationY((float) (-this.mTxtTopMsg.getHeight())).start();
        }
        this.mButtonContainer.animate().translationY((float) this.mButtonContainer.getHeight()).withEndAction(new Runnable() {
            public void run() {
                GlobalScreenshotDisplay.this.mButtonEdit.setVisibility(0);
                GlobalScreenshotDisplay.this.mButtonSend.setVisibility(0);
                GlobalScreenshotDisplay.this.mButtonDelete.setVisibility(0);
                if (!isCancel) {
                    boolean unused = GlobalScreenshotDisplay.this.mIsShowingLongScreenshot = true;
                    GlobalScreenshotDisplay.this.mButtonLongScreenshot.setVisibility(8);
                } else {
                    GlobalScreenshotDisplay.this.mButtonLongScreenshot.setVisibility(0);
                }
                GlobalScreenshotDisplay.this.mButtonStopLongScreenshot.setVisibility(8);
                GlobalScreenshotDisplay.this.mTxtTopMsg.animate().alpha(0.0f).setDuration(200).withEndAction(new Runnable() {
                    public void run() {
                        GlobalScreenshotDisplay.this.mTxtTopMsg.setVisibility(8);
                        GlobalScreenshotDisplay.this.mTxtTopMsg.setAlpha(1.0f);
                        GlobalScreenshotDisplay.this.mActionBarLayout.setVisibility(0);
                        if (GlobalScreenshotDisplay.this.mContext.getResources().getConfiguration().orientation == 2) {
                            GlobalScreenshotDisplay.this.mTopMsgDivider.setVisibility(8);
                        }
                    }
                }).start();
                GlobalScreenshotDisplay.this.mButtonContainer.animate().translationY(0.0f).start();
            }
        }).start();
        this.mScreenshotView.setSingleBitmap(this.mScreenshot);
        this.mScreenshotView.setIsTakingLongScreenshot(false);
        if (isCancel) {
            this.mScreenshotView.setBitmaps(null, true);
            this.mScreenshotView.setBottomPart(null);
        } else {
            this.mScreenshotView.gotoSingleBitmap();
        }
        this.mScreenshotView.setAlpha(1.0f);
        this.mScreenshotView.setScaleX(1.0f);
        this.mScreenshotView.setScaleY(1.0f);
        this.mBackgroundView.setVisibility(0);
        this.mRootView.setVisibility(0);
    }

    public void onShowedPageCountChanged(int showedPageCount) {
        tryToContinueOrFinish();
    }

    public void doubleClickEventReaction(boolean isShowBig) {
        if (this.mContext.getResources().getConfiguration().orientation != 2) {
            return;
        }
        if (isShowBig) {
            this.mActionBarLayout.animate().alpha(0.0f).setDuration(200).start();
        } else {
            this.mActionBarLayout.animate().alpha(1.0f).setDuration(200).start();
        }
    }

    public void uncaughtException(Thread t, Throwable e) {
        if (!(this.mRootView == null || this.mRootView.getWindowToken() == null)) {
            this.mWindowManager.removeViewImmediate(this.mRootView);
        }
        Thread.UncaughtExceptionHandler defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        if (defaultExceptionHandler != null) {
            defaultExceptionHandler.uncaughtException(t, e);
        }
    }
}
