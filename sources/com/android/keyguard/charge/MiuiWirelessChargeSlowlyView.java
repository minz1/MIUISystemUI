package com.android.keyguard.charge;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import com.android.systemui.R;
import java.io.IOException;

public class MiuiWirelessChargeSlowlyView {
    /* access modifiers changed from: private */
    public Context mContext;
    private AlertDialog mDialog;
    /* access modifiers changed from: private */
    public Handler mHandler = new Handler();
    /* access modifiers changed from: private */
    public ImageView mImageView;
    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        /* access modifiers changed from: private */
        public MediaPlayer mMediaPlayer;

        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            MiuiWirelessChargeSlowlyView.this.mImageView.setVisibility(0);
            this.mMediaPlayer = new MediaPlayer();
            this.mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                public void onPrepared(MediaPlayer mp) {
                    MiuiWirelessChargeSlowlyView.this.mHandler.postDelayed(new Runnable() {
                        public void run() {
                            if (AnonymousClass2.this.mMediaPlayer != null && !AnonymousClass2.this.mMediaPlayer.isPlaying()) {
                                AnonymousClass2.this.mMediaPlayer.start();
                            }
                        }
                    }, 1000);
                }
            });
            this.mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                public void onCompletion(MediaPlayer mp) {
                    MiuiWirelessChargeSlowlyView.this.mHandler.postDelayed(new Runnable() {
                        public void run() {
                            MiuiWirelessChargeSlowlyView.this.mImageView.setVisibility(0);
                        }
                    }, 1000);
                    MiuiWirelessChargeSlowlyView.this.mHandler.postDelayed(new Runnable() {
                        public void run() {
                            if (AnonymousClass2.this.mMediaPlayer != null && !AnonymousClass2.this.mMediaPlayer.isPlaying()) {
                                AnonymousClass2.this.mMediaPlayer.start();
                            }
                        }
                    }, 2000);
                }
            });
            this.mMediaPlayer.setSurface(new Surface(surface));
            try {
                this.mMediaPlayer.setDataSource(MiuiWirelessChargeSlowlyView.this.mContext, MiuiWirelessChargeSlowlyView.this.getVideoUri());
                this.mMediaPlayer.prepareAsync();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        }

        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            MiuiWirelessChargeSlowlyView.this.mImageView.setVisibility(0);
            if (this.mMediaPlayer != null) {
                this.mMediaPlayer.pause();
                this.mMediaPlayer.stop();
                this.mMediaPlayer.release();
                this.mMediaPlayer = null;
            }
            return false;
        }

        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            if (MiuiWirelessChargeSlowlyView.this.mImageView.getVisibility() != 8) {
                MiuiWirelessChargeSlowlyView.this.mHandler.postDelayed(new Runnable() {
                    public void run() {
                        if (AnonymousClass2.this.mMediaPlayer != null && AnonymousClass2.this.mMediaPlayer.isPlaying()) {
                            MiuiWirelessChargeSlowlyView.this.mImageView.setVisibility(8);
                        }
                    }
                }, 100);
            }
        }
    };
    private TextureView mTextureView;
    private boolean mTipOnlyOnce;

    public MiuiWirelessChargeSlowlyView(Context context, boolean tipOnlyOnce) {
        this.mContext = context;
        this.mTipOnlyOnce = tipOnlyOnce;
    }

    public void show() {
        Log.i("MiuiWirelessChargeSlowlyView", "show: ");
        if (this.mDialog == null) {
            initView();
        }
        this.mDialog.show();
        Button button = this.mDialog.getButton(-2);
        if (button != null) {
            ViewGroup.LayoutParams lp = button.getLayoutParams();
            lp.height = (int) this.mContext.getResources().getDimension(R.dimen.wireless_chagre_slowly_dialog_button_height);
            button.setLayoutParams(lp);
        }
    }

    public void dismiss() {
        Log.i("MiuiWirelessChargeSlowlyView", "dismiss: ");
        if (this.mDialog != null) {
            this.mDialog.dismiss();
        }
        this.mDialog = null;
    }

    private void initView() {
        View view = View.inflate(this.mContext, R.layout.miui_keyguard_wireless_charge_slowly, null);
        this.mImageView = (ImageView) view.findViewById(R.id.wireless_charge_picture);
        this.mTextureView = (TextureView) view.findViewById(R.id.wireless_charge_video);
        this.mTextureView.setSurfaceTextureListener(this.mSurfaceTextureListener);
        AlertDialog.Builder builder = new AlertDialog.Builder(this.mContext, R.style.wireless_charge_slowly_dialog);
        builder.setCancelable(false);
        builder.setView(view);
        DialogInterface.OnClickListener listener = null;
        if (this.mTipOnlyOnce) {
            listener = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    SharedPreferences.Editor editor = MiuiWirelessChargeSlowlyView.this.mContext.getSharedPreferences("wireless_charge", 0).edit();
                    editor.putBoolean("show_dialog", false);
                    editor.apply();
                }
            };
        }
        builder.setNegativeButton(R.string.wireless_charge_dialog_cancel, listener);
        this.mDialog = builder.create();
        this.mDialog.getWindow().setType(2010);
        this.mDialog.getWindow().requestFeature(1);
        this.mDialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_bg_light);
    }

    /* access modifiers changed from: private */
    public Uri getVideoUri() {
        return Uri.parse("android.resource://" + this.mContext.getPackageName() + "/" + R.raw.wireless_charge_video);
    }
}
