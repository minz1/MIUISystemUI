package com.android.systemui.miui;

import android.app.ActivityThread;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.util.Log;
import android.util.SparseArray;
import com.android.systemui.Constants;
import com.android.systemui.Dependency;
import com.android.systemui.Dumpable;
import com.android.systemui.statusbar.policy.ConfigurationController;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import miui.maml.FancyDrawable;
import miui.maml.util.AppIconsHelper;

public class AppIconsManager implements Dumpable, PackageEventReceiver, ConfigurationController.ConfigurationListener {
    private final SparseArray<WeakHashMap<Bitmap, WeakReference<Bitmap>>> mIconStyledCache = new SparseArray<>();
    private final SparseArray<ConcurrentHashMap<String, WeakReference<Bitmap>>> mIconsCache = new SparseArray<>();
    private final SparseArray<ConcurrentHashMap<String, WeakReference<Bitmap>>> mQuietFancyIconsCache = new SparseArray<>();

    public AppIconsManager() {
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).addCallback(this);
    }

    public Drawable getAppIcon(Context context, ApplicationInfo info, PackageManager pm, int userId) {
        return getAppIconInner(context, info.packageName, userId, info, pm);
    }

    public Drawable getAppIcon(Context context, String packageName, int userId) {
        return getAppIconInner(context, packageName, userId, null, null);
    }

    private Drawable getAppIconInner(Context context, String packageName, int userId, ApplicationInfo info, PackageManager pm) {
        Bitmap bitmap = getAppIconBitmapCache(packageName, userId, false);
        if (bitmap != null) {
            return new BitmapDrawable(Resources.getSystem(), bitmap);
        }
        Drawable icon = loadAppIcon(context, packageName, userId, info, pm);
        if (icon instanceof BitmapDrawable) {
            this.mIconsCache.get(userId).put(packageName, new WeakReference(((BitmapDrawable) icon).getBitmap()));
            log("icon cache missed for " + packageName + ", load and put bitmap cache, userId: " + userId);
        } else {
            log("don't store cache for non-BitmapDrawable: " + packageName + ", " + icon);
        }
        return icon;
    }

    public Bitmap getAppIconBitmap(Context context, String packageName) {
        return getAppIconBitmap(context, packageName, UserHandle.myUserId());
    }

    public Bitmap getAppIconBitmap(Context context, String packageName, int userId) {
        Bitmap bitmap = getAppIconBitmapCache(packageName, userId, true);
        if (bitmap == null) {
            boolean isQuietDrawable = false;
            FancyDrawable icon = loadAppIcon(context, packageName, userId);
            if (icon instanceof FancyDrawable) {
                Drawable quiet = icon.getQuietDrawable();
                if (quiet != null) {
                    icon = quiet;
                    isQuietDrawable = true;
                }
            }
            bitmap = DrawableUtils.drawable2Bitmap(icon);
            if (isQuietDrawable) {
                this.mQuietFancyIconsCache.get(userId).put(packageName, new WeakReference(bitmap));
            } else {
                this.mIconsCache.get(userId).put(packageName, new WeakReference(bitmap));
            }
            log("bitmap cache missed for " + packageName + ", load and put cache " + bitmap + ", userId: " + userId);
        }
        return bitmap;
    }

    private Bitmap getAppIconBitmapCache(String packageName, int userId, boolean considerQuiet) {
        synchronized (this.mIconsCache) {
            if (this.mIconsCache.get(userId) == null) {
                this.mIconsCache.put(userId, new ConcurrentHashMap());
            }
            if (this.mQuietFancyIconsCache.get(userId) == null) {
                this.mQuietFancyIconsCache.put(userId, new ConcurrentHashMap());
            }
        }
        Bitmap bitmap = getAppIconBitmapCacheForUser(packageName, this.mIconsCache.get(userId));
        if (bitmap == null && considerQuiet) {
            log("query quiet drawable cache for " + packageName);
            bitmap = getAppIconBitmapCacheForUser(packageName, this.mQuietFancyIconsCache.get(userId));
        }
        if (bitmap != null) {
            log("bitmap cache found for " + packageName + ", userId: " + userId);
        }
        return bitmap;
    }

    private Bitmap getAppIconBitmapCacheForUser(String packageName, Map<String, WeakReference<Bitmap>> userCaches) {
        if (!userCaches.containsKey(packageName) || !isWeakBitmapValid(userCaches.get(packageName))) {
            return null;
        }
        return (Bitmap) userCaches.get(packageName).get();
    }

    private Drawable loadAppIcon(Context context, String packageName, int userId) {
        return loadAppIcon(context, packageName, userId, null, null);
    }

    private Drawable loadAppIcon(Context context, String packageName, int userId, ApplicationInfo info, PackageManager pm) {
        Drawable drawable = null;
        if (info == null) {
            try {
                info = ActivityThread.getPackageManager().getApplicationInfo(packageName, 0, userId);
            } catch (Exception e) {
                return null;
            }
        }
        if (pm == null) {
            pm = context.getPackageManager();
        }
        if (info != null) {
            drawable = AppIconsHelper.getIconDrawable(context, info, pm);
        }
        return drawable;
    }

    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r4v8, resolved type: java.lang.Object} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r3v5, resolved type: android.graphics.Bitmap} */
    /* JADX WARNING: Multi-variable type inference failed */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public android.graphics.drawable.Drawable getIconStyleDrawable(android.graphics.drawable.Drawable r8, boolean r9) {
        /*
            r7 = this;
            r0 = r9 ^ 1
            android.util.SparseArray<java.util.WeakHashMap<android.graphics.Bitmap, java.lang.ref.WeakReference<android.graphics.Bitmap>>> r1 = r7.mIconStyledCache
            monitor-enter(r1)
            android.util.SparseArray<java.util.WeakHashMap<android.graphics.Bitmap, java.lang.ref.WeakReference<android.graphics.Bitmap>>> r2 = r7.mIconStyledCache     // Catch:{ all -> 0x00a3 }
            java.lang.Object r2 = r2.get(r0)     // Catch:{ all -> 0x00a3 }
            if (r2 != 0) goto L_0x0017
            android.util.SparseArray<java.util.WeakHashMap<android.graphics.Bitmap, java.lang.ref.WeakReference<android.graphics.Bitmap>>> r2 = r7.mIconStyledCache     // Catch:{ all -> 0x00a3 }
            java.util.WeakHashMap r3 = new java.util.WeakHashMap     // Catch:{ all -> 0x00a3 }
            r3.<init>()     // Catch:{ all -> 0x00a3 }
            r2.put(r0, r3)     // Catch:{ all -> 0x00a3 }
        L_0x0017:
            monitor-exit(r1)     // Catch:{ all -> 0x00a3 }
            android.util.SparseArray<java.util.WeakHashMap<android.graphics.Bitmap, java.lang.ref.WeakReference<android.graphics.Bitmap>>> r1 = r7.mIconStyledCache
            java.lang.Object r1 = r1.get(r0)
            java.util.WeakHashMap r1 = (java.util.WeakHashMap) r1
            boolean r2 = r8 instanceof android.graphics.drawable.BitmapDrawable
            if (r2 == 0) goto L_0x008a
            r2 = r8
            android.graphics.drawable.BitmapDrawable r2 = (android.graphics.drawable.BitmapDrawable) r2
            android.graphics.Bitmap r2 = r2.getBitmap()
            r3 = 0
            boolean r4 = r1.containsKey(r2)
            if (r4 == 0) goto L_0x003f
            java.lang.Object r4 = r1.get(r2)
            java.lang.ref.WeakReference r4 = (java.lang.ref.WeakReference) r4
            java.lang.Object r4 = r4.get()
            r3 = r4
            android.graphics.Bitmap r3 = (android.graphics.Bitmap) r3
        L_0x003f:
            if (r3 == 0) goto L_0x005c
            boolean r4 = r3.isRecycled()
            if (r4 != 0) goto L_0x005c
            java.lang.StringBuilder r4 = new java.lang.StringBuilder
            r4.<init>()
            java.lang.String r5 = "icon style cache found for request: "
            r4.append(r5)
            r4.append(r8)
            java.lang.String r4 = r4.toString()
            log(r4)
            goto L_0x0080
        L_0x005c:
            android.graphics.drawable.BitmapDrawable r4 = miui.content.res.IconCustomizer.generateIconStyleDrawable(r8, r9)
            android.graphics.Bitmap r3 = com.android.systemui.miui.DrawableUtils.drawable2Bitmap(r4)
            java.lang.ref.WeakReference r5 = new java.lang.ref.WeakReference
            r5.<init>(r3)
            r1.put(r2, r5)
            java.lang.StringBuilder r5 = new java.lang.StringBuilder
            r5.<init>()
            java.lang.String r6 = "icon style cache missing for request: "
            r5.append(r6)
            r5.append(r8)
            java.lang.String r5 = r5.toString()
            log(r5)
        L_0x0080:
            android.graphics.drawable.BitmapDrawable r4 = new android.graphics.drawable.BitmapDrawable
            android.content.res.Resources r5 = android.content.res.Resources.getSystem()
            r4.<init>(r5, r3)
            return r4
        L_0x008a:
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            java.lang.String r3 = "don't store cache for non-BitmapDrawable in getIconStyleDrawable "
            r2.append(r3)
            r2.append(r8)
            java.lang.String r2 = r2.toString()
            log(r2)
            android.graphics.drawable.BitmapDrawable r2 = miui.content.res.IconCustomizer.generateIconStyleDrawable(r8, r9)
            return r2
        L_0x00a3:
            r2 = move-exception
            monitor-exit(r1)     // Catch:{ all -> 0x00a3 }
            throw r2
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.miui.AppIconsManager.getIconStyleDrawable(android.graphics.drawable.Drawable, boolean):android.graphics.drawable.Drawable");
    }

    /* JADX WARNING: Code restructure failed: missing block: B:20:0x00ac, code lost:
        r2 = r12.mIconStyledCache;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:21:0x00ae, code lost:
        monitor-enter(r2);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:22:0x00af, code lost:
        r0 = 0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:25:0x00b6, code lost:
        if (r0 >= r12.mIconStyledCache.size()) goto L_0x00ff;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:26:0x00b8, code lost:
        r3 = r12.mIconStyledCache.get(r12.mIconStyledCache.keyAt(r0));
     */
    /* JADX WARNING: Code restructure failed: missing block: B:27:0x00c6, code lost:
        if (r3 != null) goto L_0x00ca;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:28:0x00c8, code lost:
        monitor-exit(r2);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:29:0x00c9, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:30:0x00ca, code lost:
        r7 = validCount(r3);
        r8 = java.util.Locale.getDefault();
        r10 = new java.lang.Object[3];
     */
    /* JADX WARNING: Code restructure failed: missing block: B:31:0x00dc, code lost:
        if (r12.mIconStyledCache.keyAt(r0) != 0) goto L_0x00e1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:32:0x00de, code lost:
        r11 = "crop";
     */
    /* JADX WARNING: Code restructure failed: missing block: B:33:0x00e1, code lost:
        r11 = "non-crop";
     */
    /* JADX WARNING: Code restructure failed: missing block: B:34:0x00e3, code lost:
        r10[0] = r11;
        r10[1] = java.lang.Integer.valueOf(r3.size());
        r10[2] = java.lang.Integer.valueOf(r7);
        r14.println(java.lang.String.format(r8, "icon-styled cache for %s, count: %d, valid: %d", r10));
        r0 = r0 + 1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:35:0x00ff, code lost:
        monitor-exit(r2);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:36:0x0100, code lost:
        r14.println();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:37:0x0103, code lost:
        return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void dump(java.io.FileDescriptor r13, java.io.PrintWriter r14, java.lang.String[] r15) {
        /*
            r12 = this;
            java.lang.String r0 = "AppIconsManager:"
            r14.println(r0)
            java.lang.String r0 = "AppIcons:"
            r14.println(r0)
            android.util.SparseArray<java.util.concurrent.ConcurrentHashMap<java.lang.String, java.lang.ref.WeakReference<android.graphics.Bitmap>>> r0 = r12.mIconsCache
            monitor-enter(r0)
            r1 = 0
            r2 = r1
        L_0x000f:
            android.util.SparseArray<java.util.concurrent.ConcurrentHashMap<java.lang.String, java.lang.ref.WeakReference<android.graphics.Bitmap>>> r3 = r12.mIconsCache     // Catch:{ all -> 0x0107 }
            int r3 = r3.size()     // Catch:{ all -> 0x0107 }
            r4 = 2
            r5 = 3
            r6 = 1
            if (r2 >= r3) goto L_0x005e
            android.util.SparseArray<java.util.concurrent.ConcurrentHashMap<java.lang.String, java.lang.ref.WeakReference<android.graphics.Bitmap>>> r3 = r12.mIconsCache     // Catch:{ all -> 0x0107 }
            android.util.SparseArray<java.util.concurrent.ConcurrentHashMap<java.lang.String, java.lang.ref.WeakReference<android.graphics.Bitmap>>> r7 = r12.mIconsCache     // Catch:{ all -> 0x0107 }
            int r7 = r7.keyAt(r2)     // Catch:{ all -> 0x0107 }
            java.lang.Object r3 = r3.get(r7)     // Catch:{ all -> 0x0107 }
            java.util.Map r3 = (java.util.Map) r3     // Catch:{ all -> 0x0107 }
            if (r3 != 0) goto L_0x002c
            monitor-exit(r0)     // Catch:{ all -> 0x0107 }
            return
        L_0x002c:
            int r7 = validCount(r3)     // Catch:{ all -> 0x0107 }
            java.util.Locale r8 = java.util.Locale.getDefault()     // Catch:{ all -> 0x0107 }
            java.lang.String r9 = "userId: %d, cache size: %d, valid bitmaps: %d"
            java.lang.Object[] r5 = new java.lang.Object[r5]     // Catch:{ all -> 0x0107 }
            android.util.SparseArray<java.util.concurrent.ConcurrentHashMap<java.lang.String, java.lang.ref.WeakReference<android.graphics.Bitmap>>> r10 = r12.mIconsCache     // Catch:{ all -> 0x0107 }
            int r10 = r10.keyAt(r2)     // Catch:{ all -> 0x0107 }
            java.lang.Integer r10 = java.lang.Integer.valueOf(r10)     // Catch:{ all -> 0x0107 }
            r5[r1] = r10     // Catch:{ all -> 0x0107 }
            int r10 = r3.size()     // Catch:{ all -> 0x0107 }
            java.lang.Integer r10 = java.lang.Integer.valueOf(r10)     // Catch:{ all -> 0x0107 }
            r5[r6] = r10     // Catch:{ all -> 0x0107 }
            java.lang.Integer r6 = java.lang.Integer.valueOf(r7)     // Catch:{ all -> 0x0107 }
            r5[r4] = r6     // Catch:{ all -> 0x0107 }
            java.lang.String r4 = java.lang.String.format(r8, r9, r5)     // Catch:{ all -> 0x0107 }
            r14.println(r4)     // Catch:{ all -> 0x0107 }
            int r2 = r2 + 1
            goto L_0x000f
        L_0x005e:
            r2 = r1
        L_0x005f:
            android.util.SparseArray<java.util.concurrent.ConcurrentHashMap<java.lang.String, java.lang.ref.WeakReference<android.graphics.Bitmap>>> r3 = r12.mQuietFancyIconsCache     // Catch:{ all -> 0x0107 }
            int r3 = r3.size()     // Catch:{ all -> 0x0107 }
            if (r2 >= r3) goto L_0x00ab
            android.util.SparseArray<java.util.concurrent.ConcurrentHashMap<java.lang.String, java.lang.ref.WeakReference<android.graphics.Bitmap>>> r3 = r12.mQuietFancyIconsCache     // Catch:{ all -> 0x0107 }
            android.util.SparseArray<java.util.concurrent.ConcurrentHashMap<java.lang.String, java.lang.ref.WeakReference<android.graphics.Bitmap>>> r7 = r12.mQuietFancyIconsCache     // Catch:{ all -> 0x0107 }
            int r7 = r7.keyAt(r2)     // Catch:{ all -> 0x0107 }
            java.lang.Object r3 = r3.get(r7)     // Catch:{ all -> 0x0107 }
            java.util.Map r3 = (java.util.Map) r3     // Catch:{ all -> 0x0107 }
            if (r3 != 0) goto L_0x0079
            monitor-exit(r0)     // Catch:{ all -> 0x0107 }
            return
        L_0x0079:
            int r7 = validCount(r3)     // Catch:{ all -> 0x0107 }
            java.util.Locale r8 = java.util.Locale.getDefault()     // Catch:{ all -> 0x0107 }
            java.lang.String r9 = "userId: %d, quiet drawable cache size: %d, valid bitmaps: %d"
            java.lang.Object[] r10 = new java.lang.Object[r5]     // Catch:{ all -> 0x0107 }
            android.util.SparseArray<java.util.concurrent.ConcurrentHashMap<java.lang.String, java.lang.ref.WeakReference<android.graphics.Bitmap>>> r11 = r12.mQuietFancyIconsCache     // Catch:{ all -> 0x0107 }
            int r11 = r11.keyAt(r2)     // Catch:{ all -> 0x0107 }
            java.lang.Integer r11 = java.lang.Integer.valueOf(r11)     // Catch:{ all -> 0x0107 }
            r10[r1] = r11     // Catch:{ all -> 0x0107 }
            int r11 = r3.size()     // Catch:{ all -> 0x0107 }
            java.lang.Integer r11 = java.lang.Integer.valueOf(r11)     // Catch:{ all -> 0x0107 }
            r10[r6] = r11     // Catch:{ all -> 0x0107 }
            java.lang.Integer r11 = java.lang.Integer.valueOf(r7)     // Catch:{ all -> 0x0107 }
            r10[r4] = r11     // Catch:{ all -> 0x0107 }
            java.lang.String r8 = java.lang.String.format(r8, r9, r10)     // Catch:{ all -> 0x0107 }
            r14.println(r8)     // Catch:{ all -> 0x0107 }
            int r2 = r2 + 1
            goto L_0x005f
        L_0x00ab:
            monitor-exit(r0)     // Catch:{ all -> 0x0107 }
            android.util.SparseArray<java.util.WeakHashMap<android.graphics.Bitmap, java.lang.ref.WeakReference<android.graphics.Bitmap>>> r2 = r12.mIconStyledCache
            monitor-enter(r2)
            r0 = r1
        L_0x00b0:
            android.util.SparseArray<java.util.WeakHashMap<android.graphics.Bitmap, java.lang.ref.WeakReference<android.graphics.Bitmap>>> r3 = r12.mIconStyledCache     // Catch:{ all -> 0x0104 }
            int r3 = r3.size()     // Catch:{ all -> 0x0104 }
            if (r0 >= r3) goto L_0x00ff
            android.util.SparseArray<java.util.WeakHashMap<android.graphics.Bitmap, java.lang.ref.WeakReference<android.graphics.Bitmap>>> r3 = r12.mIconStyledCache     // Catch:{ all -> 0x0104 }
            android.util.SparseArray<java.util.WeakHashMap<android.graphics.Bitmap, java.lang.ref.WeakReference<android.graphics.Bitmap>>> r7 = r12.mIconStyledCache     // Catch:{ all -> 0x0104 }
            int r7 = r7.keyAt(r0)     // Catch:{ all -> 0x0104 }
            java.lang.Object r3 = r3.get(r7)     // Catch:{ all -> 0x0104 }
            java.util.Map r3 = (java.util.Map) r3     // Catch:{ all -> 0x0104 }
            if (r3 != 0) goto L_0x00ca
            monitor-exit(r2)     // Catch:{ all -> 0x0104 }
            return
        L_0x00ca:
            int r7 = validCount(r3)     // Catch:{ all -> 0x0104 }
            java.util.Locale r8 = java.util.Locale.getDefault()     // Catch:{ all -> 0x0104 }
            java.lang.String r9 = "icon-styled cache for %s, count: %d, valid: %d"
            java.lang.Object[] r10 = new java.lang.Object[r5]     // Catch:{ all -> 0x0104 }
            android.util.SparseArray<java.util.WeakHashMap<android.graphics.Bitmap, java.lang.ref.WeakReference<android.graphics.Bitmap>>> r11 = r12.mIconStyledCache     // Catch:{ all -> 0x0104 }
            int r11 = r11.keyAt(r0)     // Catch:{ all -> 0x0104 }
            if (r11 != 0) goto L_0x00e1
            java.lang.String r11 = "crop"
            goto L_0x00e3
        L_0x00e1:
            java.lang.String r11 = "non-crop"
        L_0x00e3:
            r10[r1] = r11     // Catch:{ all -> 0x0104 }
            int r11 = r3.size()     // Catch:{ all -> 0x0104 }
            java.lang.Integer r11 = java.lang.Integer.valueOf(r11)     // Catch:{ all -> 0x0104 }
            r10[r6] = r11     // Catch:{ all -> 0x0104 }
            java.lang.Integer r11 = java.lang.Integer.valueOf(r7)     // Catch:{ all -> 0x0104 }
            r10[r4] = r11     // Catch:{ all -> 0x0104 }
            java.lang.String r8 = java.lang.String.format(r8, r9, r10)     // Catch:{ all -> 0x0104 }
            r14.println(r8)     // Catch:{ all -> 0x0104 }
            int r0 = r0 + 1
            goto L_0x00b0
        L_0x00ff:
            monitor-exit(r2)     // Catch:{ all -> 0x0104 }
            r14.println()
            return
        L_0x0104:
            r0 = move-exception
            monitor-exit(r2)     // Catch:{ all -> 0x0104 }
            throw r0
        L_0x0107:
            r1 = move-exception
            monitor-exit(r0)     // Catch:{ all -> 0x0107 }
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.miui.AppIconsManager.dump(java.io.FileDescriptor, java.io.PrintWriter, java.lang.String[]):void");
    }

    public void onConfigChanged(Configuration newConfig) {
    }

    public void onDensityOrFontScaleChanged() {
        synchronized (this.mIconsCache) {
            this.mIconsCache.clear();
            this.mQuietFancyIconsCache.clear();
        }
        synchronized (this.mIconStyledCache) {
            this.mIconStyledCache.clear();
        }
        log("clear all caches");
    }

    public void onPackageChanged(int uid, String packageName) {
    }

    public void onPackageAdded(int uid, String packageName, boolean replacing) {
        if (!replacing) {
            removeCachesForPackage(uid, packageName);
        }
    }

    public void onPackageRemoved(int uid, String packageName, boolean dataRemoved, boolean replacing) {
        if (replacing) {
            removeCachesForPackage(uid, packageName);
        }
    }

    private void removeCachesForPackage(int uid, String packageName) {
        int userId = UserHandle.getUserId(uid);
        synchronized (this.mIconsCache) {
            if (!(this.mIconsCache.get(userId) == null || this.mIconsCache.get(userId).remove(packageName) == null)) {
                log("user " + userId + ", cache for " + packageName + " removed");
            }
            if (!(this.mQuietFancyIconsCache.get(userId) == null || this.mQuietFancyIconsCache.get(userId).remove(packageName) == null)) {
                log("user " + userId + ", quiet drawable cache for " + packageName + " removed");
            }
        }
    }

    private static void log(String message) {
        if (Constants.DEBUG) {
            Log.i("AppIconsManager", message);
        }
    }

    private static int validCount(Map<?, WeakReference<Bitmap>> map) {
        int valid = 0;
        for (Map.Entry<?, WeakReference<Bitmap>> entry : map.entrySet()) {
            if (isWeakBitmapValid(entry.getValue())) {
                valid++;
            }
        }
        return valid;
    }

    private static boolean isWeakBitmapValid(WeakReference<Bitmap> reference) {
        return reference.get() != null && !((Bitmap) reference.get()).isRecycled();
    }
}
