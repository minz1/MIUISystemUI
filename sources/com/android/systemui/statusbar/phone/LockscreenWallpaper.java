package com.android.systemui.statusbar.phone;

import android.app.IWallpaperManagerCallback;
import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.os.UserHandle;
import android.util.Log;
import com.android.keyguard.KeyguardUpdateMonitor;
import libcore.io.IoUtils;

public class LockscreenWallpaper extends IWallpaperManagerCallback.Stub implements Runnable {
    /* access modifiers changed from: private */
    public final StatusBar mBar;
    /* access modifiers changed from: private */
    public Bitmap mCache;
    /* access modifiers changed from: private */
    public boolean mCached;
    private int mCurrentUserId;
    private final Handler mH;
    /* access modifiers changed from: private */
    public AsyncTask<Void, Void, LoaderResult> mLoader;
    private UserHandle mSelectedUser;
    /* access modifiers changed from: private */
    public final KeyguardUpdateMonitor mUpdateMonitor;
    private final WallpaperManager mWallpaperManager;

    private static class LoaderResult {
        public final Bitmap bitmap;
        public final boolean success;

        LoaderResult(boolean success2, Bitmap bitmap2) {
            this.success = success2;
            this.bitmap = bitmap2;
        }

        static LoaderResult success(Bitmap b) {
            return new LoaderResult(true, b);
        }

        static LoaderResult fail() {
            return new LoaderResult(false, null);
        }
    }

    public LoaderResult loadBitmap(int currentUserId, UserHandle selectedUser) {
        ParcelFileDescriptor fd = this.mWallpaperManager.getWallpaperFile(2, selectedUser != null ? selectedUser.getIdentifier() : currentUserId);
        if (fd != null) {
            try {
                return LoaderResult.success(BitmapFactory.decodeFileDescriptor(fd.getFileDescriptor(), null, new BitmapFactory.Options()));
            } catch (OutOfMemoryError e) {
                Log.w("LockscreenWallpaper", "Can't decode file", e);
                return LoaderResult.fail();
            } finally {
                IoUtils.closeQuietly(fd);
            }
        } else if (selectedUser != null) {
            return LoaderResult.success(null);
        } else {
            return LoaderResult.success(null);
        }
    }

    public void setCurrentUser(int user) {
        if (user != this.mCurrentUserId) {
            if (this.mSelectedUser == null || user != this.mSelectedUser.getIdentifier()) {
                this.mCached = false;
            }
            this.mCurrentUserId = user;
        }
    }

    public void onWallpaperChanged() {
        postUpdateWallpaper();
    }

    public void onWallpaperColorsChanged(WallpaperColors colors, int which, int userId) {
    }

    private void postUpdateWallpaper() {
        this.mH.removeCallbacks(this);
        this.mH.post(this);
    }

    public void run() {
        if (this.mLoader != null) {
            this.mLoader.cancel(false);
        }
        final int currentUser = this.mCurrentUserId;
        final UserHandle selectedUser = this.mSelectedUser;
        this.mLoader = new AsyncTask<Void, Void, LoaderResult>() {
            /* access modifiers changed from: protected */
            public LoaderResult doInBackground(Void... params) {
                return LockscreenWallpaper.this.loadBitmap(currentUser, selectedUser);
            }

            /* access modifiers changed from: protected */
            public void onPostExecute(LoaderResult result) {
                super.onPostExecute(result);
                if (!isCancelled()) {
                    if (result.success) {
                        boolean unused = LockscreenWallpaper.this.mCached = true;
                        Bitmap unused2 = LockscreenWallpaper.this.mCache = result.bitmap;
                        LockscreenWallpaper.this.mUpdateMonitor.setHasLockscreenWallpaper(result.bitmap != null);
                        LockscreenWallpaper.this.mBar.updateMediaMetaData(true, true);
                    }
                    AsyncTask unused3 = LockscreenWallpaper.this.mLoader = null;
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
    }
}
