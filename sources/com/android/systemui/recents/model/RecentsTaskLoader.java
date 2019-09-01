package com.android.systemui.recents.model;

import android.app.ActivityManager;
import android.app.ActivityManagerCompat;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.LruCache;
import com.android.systemui.R;
import com.android.systemui.proxy.ActivityManager;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.RecentsConfiguration;
import com.android.systemui.recents.events.activity.PackagesChangedEvent;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.model.RecentsTaskLoadPlan;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.model.TaskKeyLruCache;
import java.io.PrintWriter;

public class RecentsTaskLoader {
    /* access modifiers changed from: private */
    public final LruCache<ComponentName, ActivityInfo> mActivityInfoCache;
    private final TaskKeyLruCache<String> mActivityLabelCache;
    private TaskKeyLruCache.EvictionCallback mClearActivityInfoOnEviction = new TaskKeyLruCache.EvictionCallback() {
        public void onEntryEvicted(Task.TaskKey key) {
            if (key != null) {
                RecentsTaskLoader.this.mActivityInfoCache.remove(key.getComponent());
            }
        }
    };
    private final TaskKeyLruCache<String> mContentDescriptionCache;
    BitmapDrawable mDefaultIcon;
    int mDefaultTaskBarBackgroundColor;
    int mDefaultTaskViewBackgroundColor;
    Bitmap mDefaultThumbnail;
    private final TaskKeyLruCache<Drawable> mIconCache;
    private final TaskResourceLoadQueue mLoadQueue;
    private final BackgroundTaskLoader mLoader;
    private final int mMaxIconCacheSize;
    private final int mMaxThumbnailCacheSize;
    private int mNumVisibleTasksLoaded;
    private int mNumVisibleThumbnailsLoaded;
    private final TaskKeyLruCache<ThumbnailData> mThumbnailCache;

    public RecentsTaskLoader(Context context) {
        Resources res = context.getResources();
        this.mDefaultTaskBarBackgroundColor = context.getColor(R.color.recents_task_bar_default_background_color);
        this.mDefaultTaskViewBackgroundColor = context.getColor(R.color.recents_task_view_default_background_color);
        this.mMaxThumbnailCacheSize = res.getInteger(R.integer.config_recents_max_thumbnail_count);
        this.mMaxIconCacheSize = res.getInteger(R.integer.config_recents_max_icon_count);
        int iconCacheSize = this.mMaxIconCacheSize;
        int thumbnailCacheSize = this.mMaxThumbnailCacheSize;
        Bitmap icon = Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8);
        icon.eraseColor(0);
        this.mDefaultThumbnail = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        this.mDefaultThumbnail.setHasAlpha(false);
        this.mDefaultThumbnail.eraseColor(-1);
        this.mDefaultIcon = new BitmapDrawable(context.getResources(), icon);
        int numRecentTasks = ActivityManager.getMaxRecentTasksStatic();
        this.mLoadQueue = new TaskResourceLoadQueue();
        this.mIconCache = new TaskKeyLruCache<>(iconCacheSize, this.mClearActivityInfoOnEviction);
        this.mThumbnailCache = new TaskKeyLruCache<>(thumbnailCacheSize);
        this.mActivityLabelCache = new TaskKeyLruCache<>(numRecentTasks, this.mClearActivityInfoOnEviction);
        this.mContentDescriptionCache = new TaskKeyLruCache<>(numRecentTasks, this.mClearActivityInfoOnEviction);
        this.mActivityInfoCache = new LruCache<>(numRecentTasks);
        BackgroundTaskLoader backgroundTaskLoader = new BackgroundTaskLoader(this.mLoadQueue, this.mIconCache, this.mThumbnailCache, this.mDefaultThumbnail, this.mDefaultIcon);
        this.mLoader = backgroundTaskLoader;
    }

    public int getIconCacheSize() {
        return this.mMaxIconCacheSize;
    }

    public int getThumbnailCacheSize() {
        return this.mMaxThumbnailCacheSize;
    }

    public RecentsTaskLoadPlan createLoadPlan(Context context) {
        return new RecentsTaskLoadPlan(context);
    }

    public void preloadTasks(RecentsTaskLoadPlan plan, int runningTaskId, boolean includeFrontMostExcludedTask) {
        plan.preloadPlan(this, runningTaskId, includeFrontMostExcludedTask);
    }

    public void loadTasks(Context context, RecentsTaskLoadPlan plan, RecentsTaskLoadPlan.Options opts) {
        if (opts != null) {
            plan.executePlan(opts, this, this.mLoadQueue);
            if (!opts.onlyLoadForCache) {
                this.mNumVisibleTasksLoaded = opts.numVisibleTasks;
                this.mNumVisibleThumbnailsLoaded = opts.numVisibleTaskThumbnails;
                this.mLoader.start(context);
                return;
            }
            return;
        }
        throw new RuntimeException("Requires load options");
    }

    public void loadTaskData(Task t) {
        Drawable icon = this.mIconCache.getAndInvalidateIfModified(t.key);
        Bitmap thumbnail = null;
        ActivityManager.TaskThumbnailInfo thumbnailInfo = null;
        ThumbnailData thumbnailData = this.mThumbnailCache.getAndInvalidateIfModified(t.key);
        if (thumbnailData != null) {
            thumbnail = thumbnailData.thumbnail;
            thumbnailInfo = thumbnailData.thumbnailInfo;
        }
        boolean requiresLoad = icon == null || thumbnail == null;
        Drawable icon2 = icon != null ? icon : this.mDefaultIcon;
        if (requiresLoad) {
            this.mLoadQueue.addTask(t);
        }
        t.notifyTaskDataLoaded(thumbnail == this.mDefaultThumbnail ? null : thumbnail, icon2, thumbnailInfo);
    }

    public void unloadTaskData(Task t) {
        this.mLoadQueue.removeTask(t);
        t.notifyTaskDataUnloaded(null, this.mDefaultIcon);
    }

    public void deleteTaskData(Task t, boolean notifyTaskDataUnloaded) {
        this.mLoadQueue.removeTask(t);
        this.mThumbnailCache.remove(t.key);
        this.mIconCache.remove(t.key);
        this.mActivityLabelCache.remove(t.key);
        this.mContentDescriptionCache.remove(t.key);
        if (notifyTaskDataUnloaded) {
            t.notifyTaskDataUnloaded(null, this.mDefaultIcon);
        }
    }

    public void onTrimMemory(int level) {
        RecentsConfiguration config = Recents.getConfiguration();
        if (level != 5) {
            if (level != 10) {
                if (level != 15) {
                    if (level == 20) {
                        stopLoader();
                        if (config.svelteLevel == 0) {
                            this.mThumbnailCache.trimToSize(Math.max(this.mNumVisibleTasksLoaded, this.mMaxThumbnailCacheSize / 2));
                        } else if (config.svelteLevel == 1) {
                            this.mThumbnailCache.trimToSize(this.mNumVisibleThumbnailsLoaded);
                        } else if (config.svelteLevel >= 2) {
                            this.mThumbnailCache.evictAll();
                        }
                        this.mIconCache.trimToSize(Math.max(this.mNumVisibleTasksLoaded, this.mMaxIconCacheSize / 2));
                        return;
                    } else if (level != 40) {
                        if (level != 60) {
                            if (level != 80) {
                                return;
                            }
                        }
                    }
                }
                this.mThumbnailCache.evictAll();
                this.mIconCache.evictAll();
                this.mActivityInfoCache.evictAll();
                this.mActivityLabelCache.evictAll();
                this.mContentDescriptionCache.evictAll();
                return;
            }
            this.mThumbnailCache.trimToSize(Math.max(1, this.mMaxThumbnailCacheSize / 4));
            this.mIconCache.trimToSize(Math.max(1, this.mMaxIconCacheSize / 4));
            this.mActivityInfoCache.trimToSize(Math.max(1, android.app.ActivityManager.getMaxRecentTasksStatic() / 4));
            return;
        }
        this.mThumbnailCache.trimToSize(Math.max(1, this.mMaxThumbnailCacheSize / 2));
        this.mIconCache.trimToSize(Math.max(1, this.mMaxIconCacheSize / 2));
        this.mActivityInfoCache.trimToSize(Math.max(1, android.app.ActivityManager.getMaxRecentTasksStatic() / 2));
    }

    /* access modifiers changed from: package-private */
    public String getAndUpdateActivityTitle(Task.TaskKey taskKey, ActivityManager.TaskDescription td) {
        SystemServicesProxy ssp = Recents.getSystemServices();
        if (td != null && td.getLabel() != null) {
            return td.getLabel();
        }
        String label = this.mActivityLabelCache.getAndInvalidateIfModified(taskKey);
        if (label != null) {
            return label;
        }
        ActivityInfo activityInfo = getAndUpdateActivityInfo(taskKey);
        if (activityInfo == null) {
            return "";
        }
        String label2 = ssp.getBadgedActivityLabel(activityInfo, taskKey.userId);
        this.mActivityLabelCache.put(taskKey, label2);
        return label2;
    }

    /* access modifiers changed from: package-private */
    public String getAndUpdateContentDescription(Task.TaskKey taskKey, Resources res) {
        SystemServicesProxy ssp = Recents.getSystemServices();
        String label = this.mContentDescriptionCache.getAndInvalidateIfModified(taskKey);
        if (label != null) {
            return label;
        }
        ActivityInfo activityInfo = getAndUpdateActivityInfo(taskKey);
        if (activityInfo == null) {
            return "";
        }
        String label2 = ssp.getBadgedContentDescription(activityInfo, taskKey.userId, res);
        this.mContentDescriptionCache.put(taskKey, label2);
        return label2;
    }

    public Drawable getAndUpdateActivityIcon(Task.TaskKey taskKey, ActivityManager.TaskDescription td, Resources res, boolean loadIfNotCached) {
        SystemServicesProxy ssp = Recents.getSystemServices();
        Drawable icon = this.mIconCache.getAndInvalidateIfModified(taskKey);
        if (icon != null) {
            return icon;
        }
        if (loadIfNotCached) {
            try {
                Drawable icon2 = ssp.getBadgedTaskDescriptionIcon(td, taskKey.userId, res);
                if (icon2 != null) {
                    this.mIconCache.put(taskKey, icon2);
                    return icon2;
                }
            } catch (Exception e) {
                Log.e("RecentsTaskLoader", "getBadgedTaskDescriptionIcon error", e);
            }
            ActivityInfo activityInfo = getAndUpdateActivityInfo(taskKey);
            if (activityInfo != null) {
                Drawable icon3 = ssp.getBadgedActivityIcon(activityInfo, taskKey.userId);
                if (icon3 != null) {
                    this.mIconCache.put(taskKey, icon3);
                    return icon3;
                }
            }
        }
        return null;
    }

    /* access modifiers changed from: package-private */
    public Bitmap getAndUpdateThumbnail(Task.TaskKey taskKey, boolean loadIfNotCached, boolean isAccessLocked) {
        SystemServicesProxy ssp = Recents.getSystemServices();
        ThumbnailData thumbnailData = this.mThumbnailCache.getAndInvalidateIfModified(taskKey);
        if (thumbnailData != null) {
            if (thumbnailData.isAccessLocked == isAccessLocked) {
                return thumbnailData.thumbnail;
            }
            this.mThumbnailCache.remove(taskKey);
        }
        if (loadIfNotCached && Recents.getConfiguration().svelteLevel < 3) {
            ThumbnailData thumbnailData2 = ssp.getTaskThumbnail(taskKey);
            if (thumbnailData2.thumbnail != null) {
                this.mThumbnailCache.put(taskKey, thumbnailData2);
                return thumbnailData2.thumbnail;
            }
        }
        return null;
    }

    /* access modifiers changed from: package-private */
    public int getActivityPrimaryColor(ActivityManager.TaskDescription td) {
        if (td == null || td.getPrimaryColor() == 0) {
            return this.mDefaultTaskBarBackgroundColor;
        }
        return td.getPrimaryColor();
    }

    /* access modifiers changed from: package-private */
    public int getActivityBackgroundColor(ActivityManager.TaskDescription td) {
        if (td == null || ActivityManagerCompat.getTaskDescriptionBackgroundColor(td) == 0) {
            return this.mDefaultTaskViewBackgroundColor;
        }
        return ActivityManagerCompat.getTaskDescriptionBackgroundColor(td);
    }

    /* access modifiers changed from: package-private */
    public ActivityInfo getAndUpdateActivityInfo(Task.TaskKey taskKey) {
        SystemServicesProxy ssp = Recents.getSystemServices();
        ComponentName cn = taskKey.getComponent();
        ActivityInfo activityInfo = this.mActivityInfoCache.get(cn);
        if (activityInfo == null) {
            activityInfo = ssp.getActivityInfo(cn, taskKey.userId);
            if (cn == null || activityInfo == null) {
                Log.e("RecentsTaskLoader", "Unexpected null component name or activity info: " + cn + ", " + activityInfo);
                return null;
            }
            this.mActivityInfoCache.put(cn, activityInfo);
        }
        return activityInfo;
    }

    private void stopLoader() {
        this.mLoader.stop();
        this.mLoadQueue.clearTasks();
    }

    public void onThemeChanged() {
        this.mIconCache.evictAll();
    }

    public void onLanguageChange() {
        this.mActivityLabelCache.evictAll();
    }

    public final void onBusEvent(PackagesChangedEvent event) {
        for (ComponentName cn : this.mActivityInfoCache.snapshot().keySet()) {
            if (cn.getPackageName().equals(event.packageName)) {
                this.mActivityInfoCache.remove(cn);
            }
        }
    }

    public void dump(String prefix, PrintWriter writer) {
        String innerPrefix = prefix + "  ";
        writer.print(prefix);
        writer.println("RecentsTaskLoader");
        writer.print(prefix);
        writer.println("Icon Cache");
        this.mIconCache.dump(innerPrefix, writer);
        writer.print(prefix);
        writer.println("Thumbnail Cache");
        this.mThumbnailCache.dump(innerPrefix, writer);
    }
}
