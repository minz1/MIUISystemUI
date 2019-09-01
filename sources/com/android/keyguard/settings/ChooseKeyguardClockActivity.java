package com.android.keyguard.settings;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.MiuiKeyguardLeftTopClock;
import com.android.keyguard.MiuiKeyguardUtils;
import com.android.keyguard.MiuiKeyguardVerticalClock;
import com.android.keyguard.wallpaper.KeyguardWallpaperUtils;
import com.android.keyguard.wallpaper.WallpaperAuthorityUtils;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import java.io.File;
import miui.date.Calendar;

public class ChooseKeyguardClockActivity extends Activity implements View.OnClickListener {
    protected boolean m24HourFormat;
    /* access modifiers changed from: private */
    public ImageView mBackImage;
    /* access modifiers changed from: private */
    public boolean mBackImageLight = false;
    protected Calendar mCalendar;
    /* access modifiers changed from: private */
    public LinearLayout mChooseClockLayout;
    /* access modifiers changed from: private */
    public LinearLayout mClockLayout1;
    private LinearLayout mClockLayout2;
    private LinearLayout mClockLayout3;
    private TextView mDate1;
    private TextView mDate2;
    private TextView mDate3;
    /* access modifiers changed from: private */
    public final Handler mHandler = new Handler();
    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            ChooseKeyguardClockActivity.this.mHandler.post(ChooseKeyguardClockActivity.this.mUpdateTimeRunnable);
        }
    };
    private boolean mIsMiWallpaper = false;
    private boolean mIsThemeLiveWallpaper = false;
    private boolean mIsVideo24Wallpaper = false;
    /* access modifiers changed from: private */
    public MiuiKeyguardLeftTopClock mKeyguardClock;
    /* access modifiers changed from: private */
    public MiuiKeyguardVerticalClock mKeyguardVerticalClock;
    /* access modifiers changed from: private */
    public MediaPlayer mLiveLockWallpaperPlayer;
    private TextureView mLiveLockWallpaperView;
    private int mSelectedClockPosition = 0;
    private TextView mTime1;
    private TextView mTime2;
    private TextView mTime3;
    /* access modifiers changed from: private */
    public Runnable mUpdateClockHeightRunnable = new Runnable() {
        public void run() {
            WindowManager wm = ChooseKeyguardClockActivity.this.getWindowManager();
            float clockLayoutHeight = (float) ((ChooseKeyguardClockActivity.this.mClockLayout1.getWidth() * wm.getDefaultDisplay().getHeight()) / wm.getDefaultDisplay().getWidth());
            if (clockLayoutHeight > 0.0f) {
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) ChooseKeyguardClockActivity.this.mChooseClockLayout.getLayoutParams();
                lp.height = ((int) clockLayoutHeight) + ChooseKeyguardClockActivity.this.getResources().getDimensionPixelSize(R.dimen.choose_clock_layout_padding) + ChooseKeyguardClockActivity.this.getResources().getDimensionPixelSize(R.dimen.choose_clock_layout_padding_bottom);
                ChooseKeyguardClockActivity.this.mChooseClockLayout.setLayoutParams(lp);
            }
        }
    };
    private KeyguardUpdateMonitor mUpdateMonitor;
    /* access modifiers changed from: private */
    public Runnable mUpdateTimeRunnable = new Runnable() {
        public void run() {
            if (ChooseKeyguardClockActivity.this.mKeyguardClock != null) {
                ChooseKeyguardClockActivity.this.updateTime();
                ChooseKeyguardClockActivity.this.mKeyguardClock.updateTime();
                ChooseKeyguardClockActivity.this.mKeyguardVerticalClock.updateTime();
            }
        }
    };
    /* access modifiers changed from: private */
    public ImageView mWallPaper;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.choose_keyguard_clock);
        this.mUpdateMonitor = KeyguardUpdateMonitor.getInstance(this);
        this.mIsThemeLiveWallpaper = WallpaperAuthorityUtils.isThemeLockLiveWallpaper(this);
        this.mIsVideo24Wallpaper = WallpaperAuthorityUtils.isVideo24Wallpaper(this);
        this.mIsMiWallpaper = WallpaperAuthorityUtils.isMiWallpaper(this);
        this.m24HourFormat = DateFormat.is24HourFormat(this, UserHandle.myUserId());
        this.mWallPaper = (ImageView) findViewById(R.id.wallpaper);
        this.mKeyguardClock = (MiuiKeyguardLeftTopClock) findViewById(R.id.miui_keyguard_clock);
        this.mKeyguardVerticalClock = (MiuiKeyguardVerticalClock) findViewById(R.id.miui_keyguard_vertical_clock);
        this.mKeyguardVerticalClock.setSkipAnimation(true);
        this.mKeyguardClock.setDarkMode(this.mUpdateMonitor.isLightClock());
        this.mKeyguardVerticalClock.setDarkMode(this.mUpdateMonitor.isLightClock());
        this.mTime1 = (TextView) findViewById(R.id.time1);
        this.mDate1 = (TextView) findViewById(R.id.date1);
        this.mTime2 = (TextView) findViewById(R.id.time2);
        this.mDate2 = (TextView) findViewById(R.id.date2);
        this.mTime3 = (TextView) findViewById(R.id.time3);
        this.mDate3 = (TextView) findViewById(R.id.date3);
        this.mChooseClockLayout = (LinearLayout) findViewById(R.id.choose_clock_layout);
        this.mClockLayout1 = (LinearLayout) findViewById(R.id.clock_layout1);
        this.mClockLayout2 = (LinearLayout) findViewById(R.id.clock_layout2);
        this.mClockLayout3 = (LinearLayout) findViewById(R.id.clock_layout3);
        this.mBackImage = (ImageView) findViewById(R.id.back_image);
        this.mBackImage.setOnClickListener(this);
        this.mClockLayout1.setOnClickListener(this);
        this.mClockLayout2.setOnClickListener(this);
        this.mClockLayout3.setOnClickListener(this);
        this.mLiveLockWallpaperView = (TextureView) findViewById(R.id.wallpaper_textureView);
        File videoFile = getLockVideo();
        if (videoFile == null || !videoFile.getPath().endsWith(".mp4")) {
            this.mLiveLockWallpaperView.setVisibility(8);
        } else {
            showMiLiveLockWallpaper(getLockVideo());
        }
        processWallpaperView();
        this.mCalendar = new Calendar();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.TIME_TICK");
        filter.addAction("android.intent.action.TIME_SET");
        filter.addAction("android.intent.action.TIMEZONE_CHANGED");
        registerReceiverAsUser(this.mIntentReceiver, UserHandle.ALL, filter, null, (Handler) Dependency.get(Dependency.TIME_TICK_HANDLER));
        ((GradientDrawable) this.mChooseClockLayout.getBackground()).setCornerRadius((float) getMiuiDialogCornerRadius(this));
        this.mClockLayout1.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                ChooseKeyguardClockActivity.this.mHandler.post(ChooseKeyguardClockActivity.this.mUpdateClockHeightRunnable);
                ChooseKeyguardClockActivity.this.mClockLayout1.removeOnLayoutChangeListener(this);
            }
        });
    }

    /* access modifiers changed from: protected */
    public void onResume() {
        super.onResume();
        this.mSelectedClockPosition = Settings.System.getIntForUser(getContentResolver(), "selected_keyguard_clock_position", 0, UserHandle.myUserId());
        updateKeyguardClockView();
        updateTime();
    }

    /* access modifiers changed from: protected */
    public void onStop() {
        super.onStop();
        finish();
    }

    /* access modifiers changed from: protected */
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(this.mIntentReceiver);
        this.mHandler.removeCallbacks(this.mUpdateClockHeightRunnable);
        releaseLiveWallpaper();
    }

    private void updateKeyguardClockView() {
        if (this.mSelectedClockPosition == 2) {
            this.mKeyguardClock.setVisibility(0);
            this.mKeyguardVerticalClock.setVisibility(8);
            this.mClockLayout2.setSelected(true);
            return;
        }
        this.mKeyguardClock.setVisibility(8);
        this.mKeyguardVerticalClock.setVisibility(0);
        boolean supportVerticalClock = MiuiKeyguardUtils.isSupportVerticalClock(this.mSelectedClockPosition, this);
        this.mKeyguardVerticalClock.setSelectedClockPosition(this.mSelectedClockPosition);
        if (supportVerticalClock) {
            this.mClockLayout3.setSelected(true);
        } else {
            this.mClockLayout1.setSelected(true);
        }
    }

    private void processWallpaperView() {
        new AsyncTask<Void, Void, Drawable>() {
            /* access modifiers changed from: protected */
            public Drawable doInBackground(Void... params) {
                Drawable wallpaperDrawable = KeyguardWallpaperUtils.getLockWallpaperPreview(ChooseKeyguardClockActivity.this);
                try {
                    Bitmap arrowBitmap = Bitmap.createBitmap(((BitmapDrawable) wallpaperDrawable).getBitmap(), 60, 125, 110, 110);
                    if (arrowBitmap != null) {
                        boolean unused = ChooseKeyguardClockActivity.this.mBackImageLight = MiuiKeyguardUtils.getBitmapColorMode(arrowBitmap, 3) != 0;
                        arrowBitmap.recycle();
                    }
                } catch (Exception e) {
                    Log.e("ChooseKeyguardClockActivity", "create bitmap exception: ", e);
                }
                return wallpaperDrawable;
            }

            /* access modifiers changed from: protected */
            public void onPostExecute(Drawable wallpaperDrawable) {
                int i;
                ChooseKeyguardClockActivity.this.mWallPaper.setImageDrawable(wallpaperDrawable);
                ImageView access$1000 = ChooseKeyguardClockActivity.this.mBackImage;
                if (ChooseKeyguardClockActivity.this.mBackImageLight) {
                    i = miui.R.drawable.action_bar_back_light;
                } else {
                    i = miui.R.drawable.action_bar_back_dark;
                }
                access$1000.setImageResource(i);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
    }

    /* access modifiers changed from: private */
    public void updateTime() {
        this.mCalendar.setTimeInMillis(System.currentTimeMillis());
        int hour = this.mCalendar.get(18);
        int i = 12;
        int hour2 = (this.m24HourFormat || hour <= 12) ? hour : hour - 12;
        if (this.m24HourFormat || hour2 != 0) {
            i = hour2;
        }
        int hour3 = i;
        int minute = this.mCalendar.get(20);
        TextView textView = this.mTime1;
        textView.setText(String.valueOf(hour3) + ":" + MiuiKeyguardUtils.formatTime(minute));
        TextView textView2 = this.mTime2;
        textView2.setText(String.valueOf(hour3) + ":" + MiuiKeyguardUtils.formatTime(minute));
        TextView textView3 = this.mTime3;
        textView3.setText(MiuiKeyguardUtils.formatTime(hour3) + "\n" + MiuiKeyguardUtils.formatTime(minute));
        int dateResId = this.m24HourFormat ? R.string.lock_screen_date : R.string.lock_screen_date_12;
        this.mDate1.setText(this.mCalendar.format(getString(dateResId)));
        this.mDate2.setText(this.mCalendar.format(getString(dateResId)));
        this.mDate3.setText(this.mCalendar.format(getString(dateResId)));
    }

    public void onClick(View v) {
        int id = v.getId();
        if (id != R.id.back_image) {
            switch (id) {
                case R.id.clock_layout1 /*2131361958*/:
                    clearSelection();
                    this.mSelectedClockPosition = 1;
                    break;
                case R.id.clock_layout2 /*2131361959*/:
                    clearSelection();
                    this.mSelectedClockPosition = 2;
                    break;
                case R.id.clock_layout3 /*2131361960*/:
                    clearSelection();
                    this.mSelectedClockPosition = 3;
                    break;
            }
        } else {
            finish();
        }
        Settings.System.putIntForUser(getContentResolver(), "selected_keyguard_clock_position", this.mSelectedClockPosition, UserHandle.myUserId());
        updateKeyguardClockView();
    }

    private void clearSelection() {
        this.mClockLayout1.setSelected(false);
        this.mClockLayout2.setSelected(false);
        this.mClockLayout3.setSelected(false);
    }

    private int getMiuiDialogCornerRadius(Context context) {
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("dialog_bg_corner_radius", "dimen", "miui");
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        }
        return -1;
    }

    private File getLockVideo() {
        if (WallpaperAuthorityUtils.isHomeDefaultWallpaper()) {
            return new File("/system/media/lockscreen/video/video_wallpaper.mp4");
        }
        if (WallpaperAuthorityUtils.isThemeLockVideoWallpaper() || (this.mIsThemeLiveWallpaper && !this.mIsVideo24Wallpaper && !this.mIsMiWallpaper)) {
            return new File("/data/system/theme_magic/video/video_wallpaper.mp4");
        }
        return null;
    }

    private void releaseLiveWallpaper() {
        if (this.mLiveLockWallpaperPlayer != null) {
            final MediaPlayer wallPaperPlayer = this.mLiveLockWallpaperPlayer;
            this.mLiveLockWallpaperPlayer = null;
            AsyncTask.execute(new Runnable() {
                public void run() {
                    wallPaperPlayer.release();
                }
            });
        }
    }

    private void showMiLiveLockWallpaper(File wallpaperFile) {
        this.mLiveLockWallpaperPlayer = MediaPlayer.create(this, Uri.fromFile(wallpaperFile));
        this.mLiveLockWallpaperView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                try {
                    ChooseKeyguardClockActivity.this.mLiveLockWallpaperPlayer.setSurface(new Surface(surface));
                    ChooseKeyguardClockActivity.this.startLiveLockWallpaper();
                } catch (Exception e) {
                    Log.e("ChooseKeyguardClockActivity", "show live wallpaper fail:", e);
                }
            }

            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            }

            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        });
    }

    /* access modifiers changed from: private */
    public void startLiveLockWallpaper() {
        if (this.mLiveLockWallpaperPlayer != null) {
            try {
                this.mLiveLockWallpaperPlayer.start();
            } catch (Exception e) {
                Log.e("ChooseKeyguardClockActivity", e.getMessage(), e);
            }
        }
    }
}
