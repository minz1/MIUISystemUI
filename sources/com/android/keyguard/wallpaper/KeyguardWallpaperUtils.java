package com.android.keyguard.wallpaper;

import android.app.WallpaperInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.miui.Shell;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.Display;
import android.view.WindowManager;
import com.android.keyguard.DeviceConfig;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.WallpaperManagerCompat;
import com.android.keyguard.common.Utilities;
import com.android.keyguard.utils.MiuiSettingsUtils;
import com.android.keyguard.utils.PreferenceUtils;
import com.android.keyguard.utils.ThemeUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import miui.content.res.ThemeResources;
import miui.graphics.BitmapFactory;
import miui.theme.ThemeFileUtils;
import miui.util.CustomizeUtil;
import miui.util.IOUtils;
import miui.util.InputStreamLoader;

public class KeyguardWallpaperUtils {
    private static Point mTmpPoint = new Point();
    private static Pair<File, Drawable> sLockWallpaperCache;
    private static boolean sLockWallpaperChangedForSleep;
    private static long sLockWallpaperModifiedTime;
    private static Object sWallpaperLock = new Object();

    public static Drawable getLockWallpaperPreview(Context context) {
        if (WallpaperAuthorityUtils.isThemeLockLiveWallpaper(context)) {
            WallpaperInfo paperInfo = WallpaperManagerCompat.getWallpaperInfo(context);
            if (paperInfo != null) {
                if ("com.miui.miwallpaper.MiWallpaper".equals(paperInfo.getServiceName()) && isMiwallpaperPreviewExist()) {
                    Drawable preview = loadDrawable(context, "/data/system/theme/miwallpaper_preview");
                    if (preview != null) {
                        return preview;
                    }
                }
                String fileName = KeyguardUpdateMonitor.getVideo24WallpaperThumnailName();
                if ("com.android.systemui.wallpaper.Video24WallpaperService".equals(paperInfo.getServiceName()) && !TextUtils.isEmpty(fileName)) {
                    Drawable preview2 = loadAssetsDrawable(context, fileName);
                    if (preview2 != null) {
                        return preview2;
                    }
                }
                if ("com.android.thememanager.service.VideoWallpaperService".equals(paperInfo.getServiceName()) && isVideoWallpaperPreviewExist()) {
                    Drawable preview3 = loadDrawable(context, "/data/system/theme_magic/video/video_wallpaper_thumbnail.jpg");
                    if (preview3 != null) {
                        return preview3;
                    }
                }
                return paperInfo.loadThumbnail(context.getPackageManager());
            }
        }
        Pair<File, Drawable> result = getLockWallpaper(context);
        return result == null ? null : (Drawable) result.second;
    }

    public static final Pair<File, Drawable> getLockWallpaper(Context context) {
        File file = null;
        File previewFile = null;
        if (WallpaperAuthorityUtils.isHomeDefaultWallpaper()) {
            file = new File("/system/media/lockscreen/video/video_wallpaper.mp4");
            previewFile = new File("/system/media/lockscreen/video/video_wallpaper_thumbnail.jpg");
        } else if (WallpaperAuthorityUtils.isThemeLockVideoWallpaper()) {
            file = new File("/data/system/theme_magic/video/video_wallpaper.mp4");
            previewFile = new File("/data/system/theme_magic/video/video_wallpaper_thumbnail.jpg");
        }
        if (file == null || !file.exists() || previewFile == null || !previewFile.exists()) {
            file = ThemeResources.getSystem().getLockscreenWallpaper();
            previewFile = file;
        }
        if (file != null && file.exists() && previewFile != null && previewFile.exists()) {
            return getLockWallpaperCache(context, file, previewFile);
        }
        String filePath = file != null ? file.getAbsolutePath() : "null";
        String previewPath = previewFile != null ? previewFile.getAbsolutePath() : "null";
        Log.d("KeyguardWallpaperUtils", "getLockWallpaper return null; filePath = " + filePath + " previewPath = " + previewPath);
        return null;
    }

    private static final Pair<File, Drawable> getLockWallpaperCache(Context context, File file, File previewFile) {
        if (sLockWallpaperModifiedTime == file.lastModified() && !sLockWallpaperChangedForSleep && sLockWallpaperCache != null && sLockWallpaperCache.first != null && ((File) sLockWallpaperCache.first).exists() && file.equals(sLockWallpaperCache.first)) {
            return sLockWallpaperCache;
        }
        sLockWallpaperCache = null;
        try {
            Display display = ((WindowManager) context.getSystemService("window")).getDefaultDisplay();
            Point size = new Point();
            getRealSize(display, size);
            int width = size.x;
            int height = size.y;
            if (width > height) {
                Log.e("LockWallpaper", "Wrong display metrics for width = " + width + " and height = " + height);
                int tmp = width;
                width = height;
                height = tmp;
            }
            Bitmap bitmap = BitmapFactory.decodeBitmap(previewFile.getAbsolutePath(), width, height, false);
            if (bitmap != null) {
                sLockWallpaperCache = new Pair<>(file, new BitmapDrawable(context.getResources(), bitmap));
                sLockWallpaperModifiedTime = file.lastModified();
                sLockWallpaperChangedForSleep = false;
            }
        } catch (Exception e) {
            Log.e("KeyguardWallpaperUtils", "getLockWallpaperCache", e);
        } catch (OutOfMemoryError error) {
            Log.e("KeyguardWallpaperUtils", "getLockWallpaperCache", error);
        }
        return sLockWallpaperCache;
    }

    private static boolean isMiwallpaperPreviewExist() {
        return new File("/data/system/theme/miwallpaper_preview").exists();
    }

    private static boolean isVideoWallpaperPreviewExist() {
        return new File("/data/system/theme_magic/video/video_wallpaper_thumbnail.jpg").exists();
    }

    private static Drawable loadAssetsDrawable(Context context, String name) {
        Bitmap bitmap = null;
        InputStream is = null;
        try {
            is = context.getAssets().open(name);
            if (is != null) {
                bitmap = BitmapFactory.decodeStream(is);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Throwable th) {
            IOUtils.closeQuietly(null);
            throw th;
        }
        IOUtils.closeQuietly(is);
        if (bitmap != null) {
            return new BitmapDrawable(context.getResources(), bitmap);
        }
        return null;
    }

    private static Drawable loadDrawable(Context context, String filePath) {
        try {
            Bitmap b = BitmapFactory.decodeBitmap(filePath, false);
            if (b != null) {
                return new BitmapDrawable(context.getResources(), b);
            }
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    public static boolean setLockWallpaper(Context context, Uri wallpaperUri, boolean autoChange) {
        synchronized (sWallpaperLock) {
            if (!Utilities.isUriFileExists(context, wallpaperUri)) {
                return false;
            }
            Point p = getScreenSize(context);
            Bitmap wallpaper = getRotatedBitmap(context, wallpaperUri);
            if (wallpaper == null) {
                return false;
            }
            if (((float) wallpaper.getWidth()) / ((float) wallpaper.getHeight()) == ((float) p.x) / ((float) p.y)) {
                boolean lockWallpaperWithoutCrop = setLockWallpaperWithoutCrop(context, wallpaperUri, autoChange);
                return lockWallpaperWithoutCrop;
            }
            boolean lockWallpaper = setLockWallpaper(context, autoCropWallpaper(context, wallpaper, p), autoChange, wallpaperUri.toString());
            return lockWallpaper;
        }
    }

    private static boolean setLockWallpaperWithoutCrop(Context context, String src, String srcUri, boolean autoChange) {
        setWallpaperSourceUri(context, "pref_key_lock_wallpaper_path", srcUri);
        return setLockWallpaperWithoutCrop(context, src, autoChange);
    }

    private static boolean setLockWallpaperWithoutCrop(Context context, Uri wallpaperUri, boolean autoChange) {
        if (wallpaperUri == null || !Utilities.isUriFileExists(context, wallpaperUri)) {
            return false;
        }
        InputStream is = null;
        FileOutputStream os = null;
        try {
            is = context.getContentResolver().openInputStream(wallpaperUri);
            File tmpLockScreen = getTmpLockScreenFile();
            os = new FileOutputStream(tmpLockScreen);
            byte[] bytes = new byte[1024];
            while (true) {
                int read = is.read(bytes);
                int read2 = read;
                if (read == -1) {
                    return setLockWallpaperWithoutCrop(context, tmpLockScreen.getPath(), wallpaperUri.toString(), autoChange);
                }
                os.write(bytes, 0, read2);
            }
        } catch (Exception e) {
            Log.e("KeyguardWallpaperUtils", "setLockWallpaperWithoutCrop", e);
            return false;
        } finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(os);
        }
    }

    private static File getTmpLockScreenFile() throws IOException {
        File systemuiPath = new File("/sdcard/systemui/");
        if (!systemuiPath.exists()) {
            systemuiPath.mkdirs();
        }
        File lockWallpaper = new File("/sdcard/systemui/lock_wallpaper");
        if (!lockWallpaper.exists()) {
            lockWallpaper.createNewFile();
        }
        return lockWallpaper;
    }

    private static boolean setLockWallpaperWithoutCrop(Context context, String src, boolean autoChange) {
        synchronized (sWallpaperLock) {
            new File("/data/system/theme/").mkdirs();
            new File("/data/system/theme/lock_wallpaper").delete();
            if (Build.VERSION.SDK_INT >= 28) {
                ThemeFileUtils.copy(src, "/data/system/theme/lock_wallpaper");
                ThemeFileUtils.remove(src);
            } else {
                Shell.copy(src, "/data/system/theme/lock_wallpaper");
                Shell.remove(src);
            }
            sLockWallpaperChangedForSleep = true;
            Log.d("KeyguardWallpaperUtils", "setLockWallpaperWithoutCrop copy src = " + src);
            ThemeUtils.updateFilePermissionWithThemeContext("/data/system/theme/lock_wallpaper");
            onLockWallpaperChanged(context, autoChange);
        }
        return true;
    }

    private static boolean onLockWallpaperChanged(Context context, boolean autoChange) {
        if (!autoChange) {
            PreferenceUtils.removeKey(context, "currentWallpaperInfo");
            MiuiSettingsUtils.putStringToSystem(context.getContentResolver(), "lock_wallpaper_provider_authority", "com.miui.home.none_provider");
        } else {
            setLockScreenShowLiveWallpaper(context, false);
        }
        return true;
    }

    private static void setLockScreenShowLiveWallpaper(Context context, boolean show) {
        PreferenceUtils.putBoolean(context, "keyguard_show_livewallpaper", show);
        if (show) {
            MiuiSettingsUtils.putStringToSystem(context.getContentResolver(), "lock_wallpaper_provider_authority", "com.miui.home.none_provider");
        }
    }

    private static void setWallpaperSourceUri(Context context, String key, String uri) {
        PreferenceUtils.putString(context, key, uri);
    }

    private static boolean setLockWallpaper(Context context, Bitmap b, boolean autoChange, String srcUri) {
        synchronized (sWallpaperLock) {
            boolean result = false;
            try {
                File tmpLockScreen = getTmpLockScreenFile();
                if (b != null) {
                    if (!saveToJPG(b, tmpLockScreen.getAbsolutePath())) {
                        return false;
                    }
                    result = setLockWallpaperWithoutCrop(context, tmpLockScreen.getAbsolutePath(), srcUri, autoChange);
                    tmpLockScreen.delete();
                }
                return result;
            } catch (Exception e) {
                Log.e("KeyguardWallpaperUtils", "setLockWallpaper ", e);
                return false;
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    private static boolean saveToJPG(Bitmap b, String path) {
        FileOutputStream out = null;
        boolean success = false;
        try {
            out = new FileOutputStream(path);
            b.compress(Bitmap.CompressFormat.JPEG, 100, out);
            success = true;
        } catch (Exception e) {
            Log.e("KeyguardWallpaperUtils", "saveToJPG ", e);
        } catch (Throwable th) {
            IOUtils.closeQuietly(out);
            throw th;
        }
        IOUtils.closeQuietly(out);
        return success;
    }

    private static Bitmap autoCropWallpaper(Context context, Bitmap originalBitmap, Point size) {
        Bitmap cropBitmap = null;
        if (originalBitmap == null) {
            return null;
        }
        try {
            float ratio = Math.min((((float) originalBitmap.getWidth()) * 1.0f) / ((float) size.x), (1.0f * ((float) originalBitmap.getHeight())) / ((float) size.y));
            int orginHorPadding = (int) ((((float) originalBitmap.getWidth()) - (((float) size.x) * ratio)) / 2.0f);
            int orginVerPadding = (int) ((((float) originalBitmap.getHeight()) - (((float) size.y) * ratio)) / 2.0f);
            BitmapFactory.CropOption cOpt = new BitmapFactory.CropOption();
            cOpt.srcBmpDrawingArea = new Rect(orginHorPadding, orginVerPadding, originalBitmap.getWidth() - orginHorPadding, originalBitmap.getHeight() - orginVerPadding);
            cropBitmap = Utilities.createBitmapSafely(cOpt.srcBmpDrawingArea.width(), cOpt.srcBmpDrawingArea.height(), originalBitmap.getConfig());
            BitmapFactory.cropBitmap(originalBitmap, cropBitmap, cOpt);
        } catch (OutOfMemoryError e) {
            Log.e("KeyguardWallpaperUtils", "autoCropWallpaper", e);
        } catch (Throwable th) {
            originalBitmap.recycle();
            throw th;
        }
        originalBitmap.recycle();
        return cropBitmap;
    }

    private static Point getScreenSize(Context context) {
        Display display = ((WindowManager) context.getSystemService("window")).getDefaultDisplay();
        Point p = new Point();
        int rotation = display.getRotation();
        boolean isPortrait = rotation == 0 || rotation == 2;
        getRealSize(display, mTmpPoint);
        p.x = isPortrait ? mTmpPoint.x : mTmpPoint.y;
        p.y = isPortrait ? mTmpPoint.y : mTmpPoint.x;
        return p;
    }

    private static Bitmap getRotatedBitmap(Context context, Uri wallpaperUri) {
        if (!Utilities.isUriFileExists(context, wallpaperUri)) {
            return null;
        }
        try {
            BitmapFactory.Options op = miui.graphics.BitmapFactory.getBitmapSize(context, wallpaperUri);
            Rect cropRect = new Rect(0, 0, op.outWidth, op.outHeight);
            InputStreamLoader is = new InputStreamLoader(context, wallpaperUri);
            int rotation = Utilities.getImageRotation(is.get());
            is.close();
            return decodeRegion(context, wallpaperUri, cropRect, (rotation == 90 || rotation == 270) ? op.outHeight : op.outWidth, (rotation == 90 || rotation == 270) ? op.outWidth : op.outHeight, rotation);
        } catch (IOException e) {
            Log.e("KeyguardWallpaperUtils", "getRotatedBitmap", e);
            return null;
        }
    }

    private static int computeSampleSizeLarger(float scale) {
        int i;
        int initialSize = (int) Math.floor((double) (1.0f / scale));
        if (initialSize <= 1) {
            return 1;
        }
        if (initialSize <= 8) {
            i = Integer.highestOneBit(initialSize);
        } else {
            i = 8 * (initialSize / 8);
        }
        return i;
    }

    /* JADX INFO: finally extract failed */
    /* JADX WARNING: Code restructure failed: missing block: B:37:0x0101, code lost:
        r0 = th;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static void drawInTiles(android.graphics.Canvas r30, int r31, android.graphics.BitmapRegionDecoder r32, android.graphics.Rect r33, int r34, int r35, int r36) {
        /*
            r1 = r30
            r8 = r31
            r9 = r32
            r10 = r34
            r11 = r35
            r12 = r36
            r0 = 512(0x200, float:7.175E-43)
            int r13 = r0 * r12
            android.graphics.Rect r0 = new android.graphics.Rect
            r0.<init>()
            r14 = r0
            android.graphics.BitmapFactory$Options r0 = new android.graphics.BitmapFactory$Options
            r0.<init>()
            r15 = r0
            android.graphics.Bitmap$Config r0 = android.graphics.Bitmap.Config.ARGB_8888
            r15.inPreferredConfig = r0
            r15.inSampleSize = r12
            r0 = 270(0x10e, float:3.78E-43)
            r2 = 90
            if (r8 == r2) goto L_0x0041
            if (r8 != r0) goto L_0x002b
            goto L_0x0041
        L_0x002b:
            float r3 = (float) r12
            float r4 = (float) r10
            float r3 = r3 * r4
            int r4 = r33.width()
            float r4 = (float) r4
            float r3 = r3 / r4
            float r4 = (float) r12
            float r5 = (float) r11
            float r4 = r4 * r5
            int r5 = r33.height()
            float r5 = (float) r5
            float r4 = r4 / r5
            r1.scale(r3, r4)
            goto L_0x0056
        L_0x0041:
            float r3 = (float) r12
            float r4 = (float) r10
            float r3 = r3 * r4
            int r4 = r33.height()
            float r4 = (float) r4
            float r3 = r3 / r4
            float r4 = (float) r12
            float r5 = (float) r11
            float r4 = r4 * r5
            int r5 = r33.width()
            float r5 = (float) r5
            float r4 = r4 / r5
            r1.scale(r3, r4)
        L_0x0056:
            android.graphics.Paint r3 = new android.graphics.Paint
            r7 = 2
            r3.<init>(r7)
            r6 = r3
            if (r8 == r2) goto L_0x0067
            if (r8 != r0) goto L_0x0062
            goto L_0x0067
        L_0x0062:
            int r3 = r33.width()
            goto L_0x006b
        L_0x0067:
            int r3 = r33.height()
        L_0x006b:
            r16 = r3
            if (r8 == r2) goto L_0x0077
            if (r8 != r0) goto L_0x0072
            goto L_0x0077
        L_0x0072:
            int r0 = r33.height()
            goto L_0x007b
        L_0x0077:
            int r0 = r33.width()
        L_0x007b:
            r17 = r0
            int r5 = r16 / r13
            int r4 = r17 / r13
            r0 = 0
            r2 = r0
        L_0x0083:
            r3 = r2
            if (r3 > r5) goto L_0x0129
            r2 = r0
        L_0x0087:
            if (r2 > r4) goto L_0x0116
            r18 = r2
            r2 = r14
            r19 = r3
            r3 = r33
            r20 = r4
            r4 = r8
            r21 = r5
            r5 = r19
            r10 = r6
            r6 = r18
            r22 = r7
            r7 = r13
            calcTileRect(r2, r3, r4, r5, r6, r7)
            r2 = r33
            boolean r3 = r14.intersect(r2)
            if (r3 == 0) goto L_0x0103
            monitor-enter(r32)
            android.graphics.Bitmap r3 = r9.decodeRegion(r14, r15)     // Catch:{ all -> 0x00fa }
            monitor-exit(r32)     // Catch:{ all -> 0x00fa }
            if (r3 == 0) goto L_0x0103
            boolean r4 = r14.isEmpty()
            if (r4 != 0) goto L_0x0103
            if (r8 == 0) goto L_0x00e9
            android.graphics.Matrix r4 = new android.graphics.Matrix
            r4.<init>()
            float r5 = (float) r8
            int r6 = r3.getWidth()
            int r6 = r6 / 2
            float r6 = (float) r6
            int r7 = r3.getHeight()
            int r7 = r7 / 2
            float r7 = (float) r7
            r4.setRotate(r5, r6, r7)
            r5 = r3
            r24 = 0
            r25 = 0
            int r26 = r3.getWidth()
            int r27 = r3.getHeight()
            r29 = 0
            r23 = r3
            r28 = r4
            android.graphics.Bitmap r3 = android.graphics.Bitmap.createBitmap(r23, r24, r25, r26, r27, r28, r29)
            r5.recycle()
        L_0x00e9:
            r4 = r19
            int r5 = r4 * 512
            float r5 = (float) r5
            r6 = r18
            int r7 = r6 * 512
            float r7 = (float) r7
            r1.drawBitmap(r3, r5, r7, r10)
            r3.recycle()
            goto L_0x0107
        L_0x00fa:
            r0 = move-exception
            r6 = r18
            r4 = r19
        L_0x00ff:
            monitor-exit(r32)     // Catch:{ all -> 0x0101 }
            throw r0
        L_0x0101:
            r0 = move-exception
            goto L_0x00ff
        L_0x0103:
            r6 = r18
            r4 = r19
        L_0x0107:
            int r3 = r6 + 1
            r2 = r3
            r3 = r4
            r6 = r10
            r4 = r20
            r5 = r21
            r7 = r22
            r10 = r34
            goto L_0x0087
        L_0x0116:
            r2 = r33
            r20 = r4
            r21 = r5
            r10 = r6
            r22 = r7
            r4 = r3
            int r3 = r4 + 1
            r2 = r3
            r4 = r20
            r10 = r34
            goto L_0x0083
        L_0x0129:
            r2 = r33
            r20 = r4
            r21 = r5
            r10 = r6
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.keyguard.wallpaper.KeyguardWallpaperUtils.drawInTiles(android.graphics.Canvas, int, android.graphics.BitmapRegionDecoder, android.graphics.Rect, int, int, int):void");
    }

    private static void calcTileRect(Rect tileRect, Rect cropRect, int rotation, int cellX, int cellY, int tileSize) {
        if (rotation == 90) {
            tileRect.left = cropRect.left + (cellY * tileSize);
            tileRect.top = cropRect.bottom - ((cellX + 1) * tileSize);
        } else if (rotation == 180) {
            tileRect.left = cropRect.right - ((cellX + 1) * tileSize);
            tileRect.top = cropRect.bottom - ((cellY + 1) * tileSize);
        } else if (rotation == 270) {
            tileRect.left = cropRect.right - ((cellY + 1) * tileSize);
            tileRect.top = cropRect.top + (cellX * tileSize);
        } else {
            tileRect.left = cropRect.left + (cellX * tileSize);
            tileRect.top = cropRect.top + (cellY * tileSize);
        }
        tileRect.right = tileRect.left + tileSize;
        tileRect.bottom = tileRect.top + tileSize;
    }

    public static Bitmap decodeRegion(Context context, Uri wallpaperUri, Rect cropRect, int width, int height, int rotation) {
        int i = width;
        int i2 = height;
        int i3 = rotation;
        InputStreamLoader streamLoader = new InputStreamLoader(context, wallpaperUri);
        BitmapFactory.Options options = new BitmapFactory.Options();
        if (i3 == 90 || i3 == 270) {
            options.inSampleSize = computeSampleSizeLarger(Math.max(((float) i) / ((float) cropRect.height()), ((float) i2) / ((float) cropRect.width())));
        } else {
            options.inSampleSize = computeSampleSizeLarger(Math.max(((float) i) / ((float) cropRect.width()), ((float) i2) / ((float) cropRect.height())));
        }
        try {
            if (streamLoader.get() == null) {
                return null;
            }
            BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(streamLoader.get(), true);
            streamLoader.close();
            Bitmap result = Utilities.createBitmapSafely(i, i2, Bitmap.Config.ARGB_8888);
            if (result != null) {
                drawInTiles(new Canvas(result), i3, decoder, cropRect, i, i2, options.inSampleSize);
            }
            return result;
        } catch (IOException e) {
            Log.e("KeyguardWallpaperUtils", "decodeRegion", e);
            return null;
        } finally {
            streamLoader.close();
        }
    }

    public static void getRealSize(Display display, Point outPoint) {
        CustomizeUtil.getRealSize(display, outPoint);
    }

    public static boolean isDefaultLockStyle(Context context) {
        if (new File("/data/system/theme//lockscreen").exists() || isKeyguardShowLiveWallpaper(context)) {
            return false;
        }
        return true;
    }

    private static boolean isKeyguardShowLiveWallpaper(Context context) {
        return getWorldReadableSharedPreference(context).getBoolean("keyguard_show_livewallpaper", false);
    }

    private static SharedPreferences getWorldReadableSharedPreference(Context context) {
        return context.getSharedPreferences(context.getPackageName() + "_world_readable_preferences", DeviceConfig.TEMP_SHARE_MODE_FOR_WORLD_READABLE);
    }
}
