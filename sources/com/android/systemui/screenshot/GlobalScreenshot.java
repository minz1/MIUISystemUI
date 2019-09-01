package com.android.systemui.screenshot;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.UserManagerCompat;
import android.provider.MediaStore;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManagerCompat;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;
import com.android.systemui.Constants;
import com.android.systemui.R;
import com.android.systemui.content.pm.PackageManagerCompat;
import com.android.systemui.recents.misc.ForegroundThread;
import java.io.File;
import java.util.List;
import java.util.Locale;
import miui.os.Build;
import miui.util.ScreenshotUtils;

class GlobalScreenshot {
    private ImageView mBackgroundView;
    private BroadcastReceiver mBeforeScreenshotReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (!intent.getBooleanExtra("IsFinished", false)) {
                GlobalScreenshot.this.quitThumbnailWindow(false, true);
                StatHelper.recordCountEvent(GlobalScreenshot.this.mContext, "quit_thumbnail", "continue_screenshot");
            }
        }
    };
    private float mBgPadding;
    private float mBgPaddingScale;
    private BroadcastReceiver mConfigurationReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            GlobalScreenshot.this.quitThumbnailWindow(false, true);
            StatHelper.recordCountEvent(GlobalScreenshot.this.mContext, "quit_thumbnail", "configuration_change");
        }
    };
    /* access modifiers changed from: private */
    public Context mContext;
    private Display mDisplay;
    private DisplayMetrics mDisplayMetrics;
    private Handler mHandler = new Handler();
    /* access modifiers changed from: private */
    public boolean mIsInOutAnimating;
    private boolean mIsQuited;
    /* access modifiers changed from: private */
    public boolean mIsSaved;
    private boolean mIsThumbnailMoving;
    private int mNotificationIconSize;
    private NotificationManager mNotificationManager;
    /* access modifiers changed from: private */
    public NotifyMediaStoreData mNotifyMediaStoreData;
    private Runnable mQuitThumbnailRunnable = new Runnable() {
        public void run() {
            GlobalScreenshot.this.quitThumbnailWindow(true, true);
            StatHelper.recordCountEvent(GlobalScreenshot.this.mContext, "quit_thumbnail", "timeout");
        }
    };
    /* access modifiers changed from: private */
    public Ringtone mRingtone;
    private Bitmap mScreenBitmap;
    private Uri mScreenShotUri;
    /* access modifiers changed from: private */
    public AnimatorSet mScreenshotAnimation;
    /* access modifiers changed from: private */
    public GlobalScreenshotDisplay mScreenshotDisplay;
    private ImageView mScreenshotFlash;
    /* access modifiers changed from: private */
    public View mScreenshotLayout;
    /* access modifiers changed from: private */
    public View mScreenshotMaskDown;
    /* access modifiers changed from: private */
    public View mScreenshotMaskPenal;
    /* access modifiers changed from: private */
    public View mScreenshotMaskUp;
    /* access modifiers changed from: private */
    public ImageView mScreenshotView;
    private WindowManager.LayoutParams mThumbnailLayoutParams;
    private int mThumbnailMarginRight;
    /* access modifiers changed from: private */
    public int mThumbnailMarginTop;
    /* access modifiers changed from: private */
    public ViewGroup mThumbnailRootView;
    /* access modifiers changed from: private */
    public ValueAnimator mThumbnailShakeAnimator;
    private ImageView mThumbnailView;
    private float mTouchDownY;
    private VelocityTracker mVTracker = VelocityTracker.obtain();
    private WindowManager.LayoutParams mWindowLayoutParams;
    /* access modifiers changed from: private */
    public WindowManager mWindowManager;

    public interface ScreenshotFinishCallback {
        void onFinish();
    }

    /* access modifiers changed from: private */
    public void updateRingtone() {
        Log.d("GlobalScreenshot", "updateRingtone() Build.getRegion()=" + Build.getRegion());
        if (Build.checkRegion(Locale.KOREA.getCountry())) {
            this.mScreenShotUri = Uri.fromFile(Constants.SOUND_SCREENSHOT_KR);
        } else {
            this.mScreenShotUri = Uri.fromFile(Constants.SOUND_SCREENSHOT);
        }
        this.mRingtone = RingtoneManager.getRingtone(this.mContext, this.mScreenShotUri);
        if (this.mRingtone == null) {
            return;
        }
        if (Build.checkRegion(Locale.KOREA.getCountry())) {
            this.mRingtone.setStreamType(7);
        } else {
            this.mRingtone.setStreamType(1);
        }
    }

    public GlobalScreenshot(Context context) {
        Resources r = context.getResources();
        this.mContext = context;
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService("layout_inflater");
        this.mScreenshotDisplay = new GlobalScreenshotDisplay(context);
        this.mScreenshotLayout = layoutInflater.inflate(R.layout.global_screenshot, null);
        this.mBackgroundView = (ImageView) this.mScreenshotLayout.findViewById(R.id.global_screenshot_background);
        this.mScreenshotView = (ImageView) this.mScreenshotLayout.findViewById(R.id.global_screenshot);
        this.mScreenshotFlash = (ImageView) this.mScreenshotLayout.findViewById(R.id.global_screenshot_flash);
        this.mScreenshotMaskPenal = this.mScreenshotLayout.findViewById(R.id.global_screenshot_mask);
        this.mScreenshotMaskUp = this.mScreenshotLayout.findViewById(R.id.global_screenshot_mask_up);
        this.mScreenshotMaskDown = this.mScreenshotLayout.findViewById(R.id.global_screenshot_mask_down);
        this.mScreenshotLayout.setFocusable(true);
        this.mScreenshotLayout.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(-1, -1, 0, 0, 2015, 17302784, -3);
        this.mWindowLayoutParams = layoutParams;
        WindowManagerCompat.setLayoutInDisplayCutoutMode(this.mWindowLayoutParams, 1);
        this.mWindowLayoutParams.setTitle("ScreenshotAnimation");
        this.mWindowManager = (WindowManager) context.getSystemService("window");
        this.mNotificationManager = (NotificationManager) context.getSystemService("notification");
        this.mDisplay = this.mWindowManager.getDefaultDisplay();
        this.mDisplayMetrics = new DisplayMetrics();
        this.mDisplay.getRealMetrics(this.mDisplayMetrics);
        this.mThumbnailMarginTop = this.mContext.getResources().getDimensionPixelSize(R.dimen.screenshot_thumbnail_padding_top);
        this.mThumbnailMarginRight = this.mContext.getResources().getDimensionPixelSize(R.dimen.screenshot_thumbnail_padding_right);
        this.mThumbnailRootView = (FrameLayout) layoutInflater.inflate(R.layout.global_screenshot_thumbnail, null);
        this.mThumbnailRootView.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == 4) {
                    GlobalScreenshot.this.quitThumbnailWindow(true, true);
                    StatHelper.recordCountEvent(GlobalScreenshot.this.mContext, "quit_thumbnail", "touch_outside");
                }
                return false;
            }
        });
        this.mThumbnailView = (ImageView) this.mThumbnailRootView.findViewById(R.id.imageView);
        this.mThumbnailView.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                GlobalScreenshot.this.onThumbnailViewTouch(event);
                return true;
            }
        });
        WindowManager.LayoutParams layoutParams2 = new WindowManager.LayoutParams(0, 0, this.mThumbnailMarginRight, this.mThumbnailMarginTop, 2014, 17565480, -3);
        this.mThumbnailLayoutParams = layoutParams2;
        WindowManagerCompat.setLayoutInDisplayCutoutMode(this.mThumbnailLayoutParams, 1);
        this.mThumbnailLayoutParams.gravity = 53;
        this.mThumbnailLayoutParams.setTitle("ScreenshotThumbnail");
        this.mThumbnailShakeAnimator = ValueAnimator.ofInt(new int[]{0, (int) ((context.getResources().getDisplayMetrics().density * 3.0f) + 0.5f)});
        this.mThumbnailShakeAnimator.setDuration(600);
        this.mThumbnailShakeAnimator.setInterpolator(new AccelerateInterpolator());
        this.mThumbnailShakeAnimator.setRepeatCount(-1);
        this.mThumbnailShakeAnimator.setRepeatMode(2);
        this.mThumbnailShakeAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                GlobalScreenshot.this.moveThumbnailWindowY(GlobalScreenshot.this.mThumbnailMarginTop + ((Integer) animation.getAnimatedValue()).intValue());
            }
        });
        this.mNotificationIconSize = r.getDimensionPixelSize(R.dimen.notification_large_icon_height);
        this.mBgPadding = (float) r.getDimensionPixelSize(R.dimen.global_screenshot_bg_padding);
        this.mBgPaddingScale = this.mBgPadding / ((float) this.mDisplayMetrics.widthPixels);
    }

    public static void notifyMediaAndFinish(Context context, NotifyMediaStoreData data) {
        notifyMediaAndFinish(context, data, null);
    }

    public static void notifyMediaAndFinish(final Context context, final NotifyMediaStoreData data, final ScreenshotFinishCallback callback) {
        if (data != null && !data.isRunned) {
            if (!data.saveFinished) {
                data.isPending = true;
            } else {
                new AsyncTask<Void, Void, Void>() {
                    /* access modifiers changed from: protected */
                    public Void doInBackground(Void[] params) {
                        new File(NotifyMediaStoreData.this.tempImageFilePath).renameTo(new File(NotifyMediaStoreData.this.imageFilePath));
                        Log.e("GlobalScreenshot", "File rename, old name = " + NotifyMediaStoreData.this.tempImageFilePath + ", new name = " + NotifyMediaStoreData.this.imageFilePath);
                        long dateSeconds = NotifyMediaStoreData.this.takenTime / 1000;
                        String title = NotifyMediaStoreData.this.imageFileName;
                        int pos = title.lastIndexOf(46);
                        if (pos >= 0) {
                            title = title.substring(0, pos);
                        }
                        ContentValues values = new ContentValues();
                        ContentResolver resolver = context.getContentResolver();
                        values.put("_data", NotifyMediaStoreData.this.imageFilePath);
                        values.put("title", title);
                        values.put("_display_name", NotifyMediaStoreData.this.imageFileName);
                        values.put("width", Integer.valueOf(NotifyMediaStoreData.this.width));
                        values.put("height", Integer.valueOf(NotifyMediaStoreData.this.height));
                        values.put("datetaken", Long.valueOf(NotifyMediaStoreData.this.takenTime));
                        values.put("date_added", Long.valueOf(dateSeconds));
                        values.put("date_modified", Long.valueOf(dateSeconds));
                        values.put("mime_type", "image/png");
                        values.put("_size", Long.valueOf(new File(NotifyMediaStoreData.this.imageFilePath).length()));
                        try {
                            NotifyMediaStoreData.this.outUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                        } catch (Exception e) {
                            NotifyMediaStoreData.this.outUri = null;
                        }
                        Intent intent = new Intent("com.miui.gallery.SAVE_TO_CLOUD");
                        intent.setPackage("com.miui.gallery");
                        List<ResolveInfo> resolveInfos = PackageManagerCompat.queryBroadcastReceiversAsUser(context.getPackageManager(), intent, 0, -2);
                        if (resolveInfos != null && resolveInfos.size() > 0) {
                            intent.setComponent(new ComponentName("com.miui.gallery", resolveInfos.get(0).activityInfo.name));
                        }
                        intent.putExtra("extra_file_path", NotifyMediaStoreData.this.imageFilePath);
                        context.sendBroadcastAsUser(intent, UserHandle.CURRENT);
                        return null;
                    }

                    /* access modifiers changed from: protected */
                    public void onPostExecute(Void aVoid) {
                        super.onPostExecute(aVoid);
                        if (NotifyMediaStoreData.this.finisher != null) {
                            NotifyMediaStoreData.this.finisher.run();
                        }
                        NotifyMediaStoreData.this.isRunned = true;
                        if (callback != null) {
                            callback.onFinish();
                        }
                    }
                }.execute(new Void[0]);
            }
        }
    }

    /* access modifiers changed from: package-private */
    public boolean onThumbnailViewClick() {
        this.mScreenshotDisplay.show(this.mScreenBitmap, this.mNotifyMediaStoreData, ((this.mScreenBitmap.getWidth() - this.mThumbnailMarginRight) - this.mThumbnailLayoutParams.width) + this.mThumbnailView.getLeft(), this.mThumbnailMarginTop + this.mThumbnailView.getTop(), this.mThumbnailView.getWidth(), this.mThumbnailView.getHeight());
        if (this.mIsSaved) {
            this.mScreenshotDisplay.setIsScreenshotSaved();
        }
        this.mThumbnailView.setEnabled(false);
        this.mHandler.postDelayed(new Runnable() {
            public void run() {
                GlobalScreenshot.this.quitThumbnailWindow(false, false);
            }
        }, 70);
        StatHelper.recordCountEvent(this.mContext, "quit_thumbnail", "click");
        return true;
    }

    /* access modifiers changed from: package-private */
    public void onThumbnailViewTouch(MotionEvent event) {
        if (!this.mIsInOutAnimating) {
            event.setLocation(event.getRawX(), event.getRawY());
            this.mVTracker.addMovement(event);
            switch (event.getAction()) {
                case 0:
                    this.mTouchDownY = event.getRawY();
                    this.mHandler.removeCallbacks(this.mQuitThumbnailRunnable);
                    this.mThumbnailShakeAnimator.cancel();
                    break;
                case 1:
                case 3:
                    if (this.mIsThumbnailMoving != 0) {
                        this.mVTracker.computeCurrentVelocity(1000);
                        if (this.mVTracker.getYVelocity() < ((float) ((int) (this.mContext.getResources().getDisplayMetrics().density * -170.0f)))) {
                            quitThumbnailWindow(true, true);
                            StatHelper.recordCountEvent(this.mContext, "quit_thumbnail", "slide_up");
                        } else {
                            ValueAnimator animator = ValueAnimator.ofInt(new int[]{this.mThumbnailLayoutParams.y, this.mThumbnailMarginTop});
                            animator.setDuration(100);
                            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                public void onAnimationUpdate(ValueAnimator animation) {
                                    GlobalScreenshot.this.moveThumbnailWindowY(((Integer) animation.getAnimatedValue()).intValue());
                                }
                            });
                            animator.addListener(new AnimatorListenerAdapter() {
                                public void onAnimationEnd(Animator animation) {
                                    if (GlobalScreenshot.this.mThumbnailRootView.getWindowToken() != null) {
                                        GlobalScreenshot.this.mThumbnailShakeAnimator.start();
                                    }
                                }
                            });
                            animator.setInterpolator(new AccelerateInterpolator());
                            animator.start();
                            this.mHandler.postDelayed(this.mQuitThumbnailRunnable, 3600);
                        }
                    } else if (!onThumbnailViewClick()) {
                        this.mHandler.postDelayed(this.mQuitThumbnailRunnable, 3600);
                        this.mThumbnailShakeAnimator.start();
                    }
                    this.mIsThumbnailMoving = false;
                    this.mVTracker.clear();
                    break;
                case 2:
                    int offsetY = (int) (event.getRawY() - this.mTouchDownY);
                    if (!this.mIsThumbnailMoving && Math.abs(offsetY) > ViewConfiguration.get(this.mContext).getScaledTouchSlop()) {
                        this.mIsThumbnailMoving = true;
                    }
                    int realUseOffsetY = offsetY;
                    if (offsetY > 0) {
                        realUseOffsetY = (int) Math.pow((double) realUseOffsetY, 0.7d);
                    }
                    moveThumbnailWindowY(this.mThumbnailMarginTop + realUseOffsetY + ((Integer) this.mThumbnailShakeAnimator.getAnimatedValue()).intValue());
                    break;
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void moveThumbnailWindowY(int y) {
        if (this.mThumbnailRootView.getWindowToken() != null) {
            this.mThumbnailLayoutParams.y = y;
            this.mWindowManager.updateViewLayout(this.mThumbnailRootView, this.mThumbnailLayoutParams);
        }
    }

    private void saveScreenshotInWorkerThread(Runnable saveFinisher, Runnable totalFinisher) {
        SaveImageInBackgroundData data = new SaveImageInBackgroundData();
        data.context = this.mContext;
        data.image = this.mScreenBitmap;
        data.iconSize = this.mNotificationIconSize;
        data.finisher = saveFinisher;
        SaveImageInBackgroundTask task = new SaveImageInBackgroundTask(this.mContext, data, this.mNotificationManager);
        task.execute(new SaveImageInBackgroundData[]{data});
        this.mNotifyMediaStoreData = task.mNotifyMediaStoreData;
        this.mNotifyMediaStoreData.finisher = totalFinisher;
    }

    /* access modifiers changed from: package-private */
    public void takeScreenshot(Runnable animationFinisher, Runnable totalFinisher, boolean statusBarVisible, boolean navBarVisible) {
        this.mIsSaved = false;
        if ("trigger_restart_min_framework".equals(SystemProperties.get("vold.decrypt")) || !UserManagerCompat.isUserUnlocked((UserManager) this.mContext.getSystemService(UserManager.class))) {
            Log.w("GlobalScreenshot", "Can not screenshot when decrypt state.");
            if (animationFinisher != null) {
                animationFinisher.run();
            }
            if (totalFinisher != null) {
                totalFinisher.run();
            }
            return;
        }
        this.mScreenBitmap = ScreenshotUtils.getScreenshot(this.mContext);
        afterTakeScreenshot(this.mContext);
        if (this.mScreenBitmap == null) {
            notifyScreenshotError(this.mContext, this.mNotificationManager);
            if (animationFinisher != null) {
                animationFinisher.run();
            }
            if (totalFinisher != null) {
                totalFinisher.run();
            }
            return;
        }
        Bitmap original = this.mScreenBitmap;
        this.mScreenBitmap = original.copy(Bitmap.Config.ARGB_8888, false);
        original.recycle();
        this.mScreenBitmap.setHasAlpha(false);
        this.mScreenBitmap.prepareToDraw();
        saveScreenshotInWorkerThread(new Runnable() {
            public void run() {
                boolean unused = GlobalScreenshot.this.mIsSaved = true;
                GlobalScreenshot.this.mScreenshotDisplay.setIsScreenshotSaved();
            }
        }, totalFinisher);
        this.mDisplay.getRealMetrics(this.mDisplayMetrics);
        startAnimation(animationFinisher, this.mDisplayMetrics.widthPixels, this.mDisplayMetrics.heightPixels, statusBarVisible, navBarVisible);
    }

    private void startAnimation(final Runnable animationFinisher, int w, int h, boolean statusBarVisible, boolean navBarVisible) {
        this.mScreenshotView.setImageBitmap(this.mScreenBitmap);
        this.mScreenshotLayout.requestFocus();
        this.mScreenshotView.setScaleX(1.0f);
        this.mScreenshotView.setScaleY(1.0f);
        if (this.mScreenshotAnimation != null) {
            this.mScreenshotAnimation.end();
        }
        this.mWindowManager.addView(this.mScreenshotLayout, this.mWindowLayoutParams);
        this.mScreenshotAnimation = new AnimatorSet();
        this.mScreenshotAnimation.playSequentially(new Animator[]{createScreenshotMaskAnimation(), createFinishAnimation()});
        this.mScreenshotAnimation.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                if (GlobalScreenshot.this.isShowThumbnail()) {
                    GlobalScreenshot.this.startGotoThumbnailAnimation(animationFinisher);
                    return;
                }
                GlobalScreenshot.this.mWindowManager.removeView(GlobalScreenshot.this.mScreenshotLayout);
                animationFinisher.run();
                GlobalScreenshot.notifyMediaAndFinish(GlobalScreenshot.this.mContext, GlobalScreenshot.this.mNotifyMediaStoreData);
            }
        });
        this.mScreenshotLayout.post(new Runnable() {
            public void run() {
                if (GlobalScreenshot.this.hasScreenshotSoundEnabled(GlobalScreenshot.this.mContext)) {
                    ForegroundThread.getHandler().post(new Runnable() {
                        public void run() {
                            GlobalScreenshot.this.updateRingtone();
                            if (GlobalScreenshot.this.mRingtone != null) {
                                GlobalScreenshot.this.mRingtone.play();
                            }
                        }
                    });
                }
                GlobalScreenshot.this.mScreenshotView.setLayerType(2, null);
                GlobalScreenshot.this.mScreenshotView.buildLayer();
                GlobalScreenshot.this.mScreenshotAnimation.start();
            }
        });
    }

    /* access modifiers changed from: private */
    public boolean hasScreenshotSoundEnabled(Context context) {
        return MiuiSettings.System.getBooleanForUser(context.getContentResolver(), "has_screenshot_sound", true, 0);
    }

    /* access modifiers changed from: private */
    public boolean isShowThumbnail() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "device_provisioned", 0) != 0;
    }

    public static float calcPivot(float bigStart, float bigSize, float shortStart, float shortSize) {
        return (((shortStart - bigStart) * shortSize) / (bigSize - shortSize)) + shortStart;
    }

    /* access modifiers changed from: private */
    public void startGotoThumbnailAnimation(final Runnable animationFinisher) {
        showThumbnailWindow();
        float screenW = (float) this.mScreenBitmap.getWidth();
        float pivotX = calcPivot(0.0f, screenW, ((screenW - ((float) this.mThumbnailMarginRight)) - ((float) this.mThumbnailLayoutParams.width)) + ((float) this.mThumbnailView.getLeft()), (float) this.mThumbnailView.getWidth());
        float pivotY = calcPivot(0.0f, (float) this.mScreenBitmap.getHeight(), (float) (this.mThumbnailMarginTop + this.mThumbnailView.getTop()), (float) this.mThumbnailView.getHeight());
        this.mScreenshotView.setPivotX(pivotX);
        this.mScreenshotView.setPivotY(pivotY);
        this.mIsInOutAnimating = true;
        this.mScreenshotView.animate().setDuration(300).scaleX(0.185f).scaleY(0.185f).withEndAction(new Runnable() {
            public void run() {
                GlobalScreenshot.this.mWindowManager.removeView(GlobalScreenshot.this.mScreenshotLayout);
                GlobalScreenshot.this.mThumbnailShakeAnimator.start();
                animationFinisher.run();
                boolean unused = GlobalScreenshot.this.mIsInOutAnimating = false;
            }
        }).start();
    }

    private void showThumbnailWindow() {
        if (this.mThumbnailRootView.getWindowToken() != null) {
            quitThumbnailWindow(false, true);
        }
        this.mThumbnailView.getLayoutParams().width = (int) ((((float) this.mScreenBitmap.getWidth()) * 0.185f) + 0.5f);
        this.mThumbnailView.getLayoutParams().height = (int) ((((float) this.mScreenBitmap.getHeight()) * 0.185f) + 0.5f);
        this.mThumbnailRootView.measure(View.MeasureSpec.makeMeasureSpec(0, 0), View.MeasureSpec.makeMeasureSpec(0, 0));
        this.mThumbnailRootView.layout(0, 0, this.mThumbnailRootView.getMeasuredWidth(), this.mThumbnailRootView.getMeasuredHeight());
        this.mThumbnailLayoutParams.y = this.mThumbnailMarginTop;
        this.mThumbnailLayoutParams.width = this.mThumbnailRootView.getWidth();
        this.mThumbnailLayoutParams.height = this.mThumbnailRootView.getHeight();
        this.mWindowManager.addView(this.mThumbnailRootView, this.mThumbnailLayoutParams);
        this.mHandler.postDelayed(this.mQuitThumbnailRunnable, 3600);
        this.mThumbnailView.setImageBitmap(this.mScreenBitmap);
        this.mThumbnailView.setEnabled(true);
        this.mIsQuited = false;
        this.mContext.getApplicationContext().registerReceiver(this.mBeforeScreenshotReceiver, new IntentFilter("miui.intent.TAKE_SCREENSHOT"));
        this.mContext.getApplicationContext().registerReceiver(this.mConfigurationReceiver, new IntentFilter("android.intent.action.CONFIGURATION_CHANGED"));
    }

    /* access modifiers changed from: private */
    public void quitThumbnailWindow(boolean isAnimating, boolean needNotifyMediaStore) {
        if (this.mThumbnailView.getWindowToken() != null && !this.mIsQuited) {
            this.mIsQuited = true;
            this.mHandler.removeCallbacks(this.mQuitThumbnailRunnable);
            this.mThumbnailShakeAnimator.cancel();
            this.mContext.getApplicationContext().unregisterReceiver(this.mBeforeScreenshotReceiver);
            this.mContext.getApplicationContext().unregisterReceiver(this.mConfigurationReceiver);
            if (!isAnimating) {
                this.mWindowManager.removeView(this.mThumbnailRootView);
            } else {
                this.mIsInOutAnimating = true;
                final int initWindowY = this.mThumbnailLayoutParams.y;
                ValueAnimator animator = ValueAnimator.ofInt(new int[]{0, this.mThumbnailView.getHeight() + this.mThumbnailMarginTop});
                animator.setDuration(150);
                animator.setInterpolator(new LinearInterpolator());
                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    public void onAnimationUpdate(ValueAnimator animation) {
                        GlobalScreenshot.this.moveThumbnailWindowY(initWindowY - ((Integer) animation.getAnimatedValue()).intValue());
                    }
                });
                animator.addListener(new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator animation) {
                        if (GlobalScreenshot.this.mThumbnailRootView.getWindowToken() != null) {
                            GlobalScreenshot.this.mWindowManager.removeView(GlobalScreenshot.this.mThumbnailRootView);
                            boolean unused = GlobalScreenshot.this.mIsInOutAnimating = false;
                        }
                    }
                });
                animator.start();
            }
            if (needNotifyMediaStore) {
                notifyMediaAndFinish(this.mContext, this.mNotifyMediaStoreData);
            }
        }
    }

    private ValueAnimator createScreenshotMaskAnimation() {
        ValueAnimator anim = ValueAnimator.ofFloat(new float[]{1.0f, 0.0f});
        anim.setInterpolator(new AccelerateInterpolator());
        anim.setDuration(200);
        anim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animation) {
                GlobalScreenshot.this.mScreenshotMaskPenal.setVisibility(0);
            }
        });
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                float t = ((Float) animation.getAnimatedValue()).floatValue();
                GlobalScreenshot.this.mScreenshotMaskUp.setTranslationY(((float) (-GlobalScreenshot.this.mScreenshotMaskUp.getHeight())) * t);
                GlobalScreenshot.this.mScreenshotMaskDown.setTranslationY(((float) GlobalScreenshot.this.mScreenshotMaskDown.getHeight()) * t);
            }
        });
        return anim;
    }

    private Animator createFinishAnimation() {
        Animator anim = ValueAnimator.ofFloat(new float[]{0.0f, 0.0f});
        anim.setDuration(0);
        anim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                GlobalScreenshot.this.mScreenshotMaskPenal.setVisibility(8);
            }
        });
        return anim;
    }

    static void notifyScreenshotError(Context context, NotificationManager nManager) {
        Resources r = context.getResources();
        nManager.cancel(789);
        Toast.makeText(context, r.getString(R.string.screenshot_failed_title), 0).show();
    }

    public static void beforeTakeScreenshot(Context context) {
        Intent intent = new Intent("miui.intent.TAKE_SCREENSHOT");
        intent.putExtra("IsFinished", false);
        context.sendBroadcast(intent);
    }

    public static void afterTakeScreenshot(Context context) {
        Intent intent = new Intent("miui.intent.TAKE_SCREENSHOT");
        intent.putExtra("IsFinished", true);
        context.sendBroadcast(intent);
    }
}
