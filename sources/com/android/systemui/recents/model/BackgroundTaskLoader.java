package com.android.systemui.recents.model;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.RecentsConfiguration;
import com.android.systemui.recents.misc.SystemServicesProxy;

/* compiled from: RecentsTaskLoader */
class BackgroundTaskLoader implements Runnable {
    static boolean DEBUG = false;
    static String TAG = "TaskResourceLoader";
    boolean mCancelled;
    Context mContext;
    BitmapDrawable mDefaultIcon;
    Bitmap mDefaultThumbnail;
    TaskKeyLruCache<Drawable> mIconCache;
    TaskResourceLoadQueue mLoadQueue;
    HandlerThread mLoadThread = new HandlerThread("Recents-TaskResourceLoader", 10);
    Handler mLoadThreadHandler;
    Handler mMainThreadHandler = new Handler();
    TaskKeyLruCache<ThumbnailData> mThumbnailCache;
    boolean mWaitingOnLoadQueue;

    public BackgroundTaskLoader(TaskResourceLoadQueue loadQueue, TaskKeyLruCache<Drawable> iconCache, TaskKeyLruCache<ThumbnailData> thumbnailCache, Bitmap defaultThumbnail, BitmapDrawable defaultIcon) {
        this.mLoadQueue = loadQueue;
        this.mIconCache = iconCache;
        this.mThumbnailCache = thumbnailCache;
        this.mDefaultThumbnail = defaultThumbnail;
        this.mDefaultIcon = defaultIcon;
        this.mLoadThread.start();
        this.mLoadThreadHandler = new Handler(this.mLoadThread.getLooper());
        this.mLoadThreadHandler.post(this);
    }

    /* access modifiers changed from: package-private */
    public void start(Context context) {
        this.mContext = context.getApplicationContext();
        this.mCancelled = false;
        synchronized (this.mLoadThread) {
            this.mLoadThread.notifyAll();
        }
    }

    /* access modifiers changed from: package-private */
    public void stop() {
        this.mCancelled = true;
        if (this.mWaitingOnLoadQueue) {
            this.mContext = null;
        }
    }

    public void run() {
        while (true) {
            synchronized (this.mLoadThread) {
                while (this.mCancelled) {
                    this.mContext = null;
                    try {
                        this.mLoadThread.wait();
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                }
            }
            if (!this.mCancelled) {
                RecentsConfiguration config = Recents.getConfiguration();
                SystemServicesProxy ssp = Recents.getSystemServices();
                if (ssp != null) {
                    final Task t = this.mLoadQueue.nextTask();
                    if (t != null) {
                        Drawable cachedIcon = this.mIconCache.get(t.key);
                        ThumbnailData cachedThumbnailData = this.mThumbnailCache.get(t.key);
                        if (cachedIcon == null) {
                            cachedIcon = ssp.getBadgedTaskDescriptionIcon(t.taskDescription, t.key.userId, this.mContext.getResources());
                            if (cachedIcon == null) {
                                ActivityInfo info = ssp.getActivityInfo(t.key.getComponent(), t.key.userId);
                                if (info != null) {
                                    if (DEBUG) {
                                        String str = TAG;
                                        Log.d(str, "Loading icon: " + t.key);
                                    }
                                    cachedIcon = ssp.getBadgedActivityIcon(info, t.key.userId);
                                }
                            }
                            if (cachedIcon == null) {
                                cachedIcon = this.mDefaultIcon;
                            }
                            this.mIconCache.put(t.key, cachedIcon);
                        }
                        if (cachedThumbnailData == null) {
                            if (config.svelteLevel < 3) {
                                if (DEBUG) {
                                    String str2 = TAG;
                                    Log.d(str2, "Loading thumbnail: " + t.key);
                                }
                                cachedThumbnailData = ssp.getTaskThumbnail(t.key);
                            }
                            if (cachedThumbnailData.thumbnail == null) {
                                cachedThumbnailData.thumbnail = this.mDefaultThumbnail;
                            }
                            if (config.svelteLevel < 1) {
                                this.mThumbnailCache.put(t.key, cachedThumbnailData);
                            }
                        }
                        if (!this.mCancelled) {
                            final Drawable newIcon = cachedIcon;
                            final ThumbnailData newThumbnailData = cachedThumbnailData;
                            this.mMainThreadHandler.post(new Runnable() {
                                public void run() {
                                    t.notifyTaskDataLoaded(newThumbnailData.thumbnail, newIcon, newThumbnailData.thumbnailInfo);
                                }
                            });
                        }
                    }
                }
                synchronized (this.mLoadQueue) {
                    while (!this.mCancelled && this.mLoadQueue.isEmpty()) {
                        try {
                            this.mWaitingOnLoadQueue = true;
                            this.mLoadQueue.wait();
                            this.mWaitingOnLoadQueue = false;
                        } catch (InterruptedException ie2) {
                            ie2.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}
